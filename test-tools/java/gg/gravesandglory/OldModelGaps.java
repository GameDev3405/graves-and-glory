package gg.gravesandglory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SweepAttackParticle;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * The four old-model gaps (docs/anim-rebuild/OLD-MODEL-GAPS.md), GRAVES_PHASE1_CHECKS=old-gaps, on default settings (no geoTroops=all). Each segment's
 * subject is the first of its candidates still outside GeoTroops.APPROVED (old model, TroopMotion); sweep-knight's is an approved melee troop (GeckoLib).
 * A segment whose candidates are all approved is reported skipped. Named below as they are today. In a practice battle, one subject at a time stands
 * frozen (AI off) at the centre and the real server paths run on it:
 * <ol>
 * <li>rise: a tier III revenant with Rise Again forced (Houses.forceRise) takes a lethal blow (Houses.allowDeath, rise_v1, its 20-tick stun);</li>
 * <li>rise-king: the Hollow King's Undying King (his level-3 rise row, a 70-tick stun);</li>
 * <li>cast: a hexer's blight, announced a beat ahead (Projectiles.drawAhead, as Tactics.tick) and cast 10 server ticks later through Tactics.shoot
 *     (Abilities.attack: the bolt and Projectiles.loose); its target stands 8 blocks ahead;</li>
 * <li>sweep: a revenant's swing (Tactics.swing) at a foe in reach, the blow landing on its impact frame through the battle's tick (BattleEffects.strike);</li>
 * <li>sweep-knight: the same for a knight (GeckoLib, approved): no frames;</li>
 * <li>cast-live: a hexer left to the battle's own Tactics (not frozen, held in place) against a target 8 blocks ahead for 400 ticks: every bolt it
 *     casts (server world tick) against every cast clip the client starts (its release tick); no frames.</li>
 * </ol>
 * Evidence: the client's TroopMotion engine state (the stance clip, the attack track's start and impact), the old-model rise overlay
 * (TroopMotion.riseShown, read by reflection so the suite also runs on code without it), the sweep particles the client spawned (SweepAttackParticle in
 * the particle manager), and the bolt's origin and world tick on the server. Every client tick of segments 1-4 is saved as p1-og-&lt;slug&gt;-&lt;TTT&gt;.png.
 * Never shipped.
 */
final class OldModelGaps implements Phase1Checks.Suite {
    static final float YAW = 225f;
    static final double FX = -Math.sin(Math.toRadians(YAW)), FZ = Math.cos(Math.toRadians(YAW));
    static final float PITCH = 3f;
    static final int FOV = 48, PRE = 6;

    record Seg(int no, String slug, String troop, int lead, int active, double dist, boolean shots) { int total() { return lead + active; } }
    static final List<Seg> SEGS = List.of(
        new Seg(1, "rise", oldModel("revenant", "reaper", "bonebow", "stalker"), 6, 50, 5.0, true),
        new Seg(2, "rise-king", oldModel("gb_hollowking"), 6, 92, 6.5, true),
        new Seg(3, "cast", oldModel("hexer", "tc_mist"), 6, 45, 6.0, true),
        new Seg(4, "sweep", oldModel("revenant", "reaper", "sentinel", "tc_raider"), 6, 30, 4.5, true),
        new Seg(5, "sweep-knight", approvedMelee(), 6, 30, 4.5, false),
        new Seg(6, "cast-live", oldModel("hexer", "tc_mist"), 6, 400, 6.0, false));

    /** The first candidate still drawn on the old model (GeoTroops), or null once a lane has approved them all: that segment is then skipped. */
    static String oldModel(String... candidates) { for (String t : candidates) if (!GeoTroops.drawn(t)) return t; return null; }
    /** An approved (GeckoLib) melee troop: the knight while it is approved, else the first other. */
    static String approvedMelee() {
        if (GeoTroops.APPROVED.contains("knight")) return "knight";
        return Arrays.stream(Spec.TROOPS).filter(r -> GeoTroops.APPROVED.contains(r.id()) && !r.ranged()).map(Spec.TroopsRow::id).sorted().findFirst().orElse(null);
    }

    static volatile OldModelGaps live;
    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            OldModelGaps r = live;
            if (r != null) try { r.serverHold(server); } catch (Exception e) { Phase1Checks.report("p1-old-gaps-exception", false, "server hold: " + e); e.printStackTrace(); }
        });
    }

    volatile UUID player;
    volatile boolean battleReady, placed;
    volatile String why = "";
    volatile double cx, cz, camDist = 5;
    GravesAndGlory.Squad mine, foe;
    volatile GravesAndGlory.Unit subject, target, anchor;
    volatile int subjectId = -1;
    /** Server world ticks of the segment's scripted events (-1: none) and what the server saw. */
    volatile long hitAt = -1, announceAt = -1, castAt = -1, swingAt = -1;
    volatile long riseTick = -1, boltTick = -1;
    volatile int riseWindow = -1, swingImpact = -1;
    volatile String serverWhy = "";
    /** cast-live: from which world tick the hexer is left to Tactics, and the world ticks its bolts left on. */
    volatile long liveFrom = -1;
    int liveBeats;
    final List<Long> liveBolts = Collections.synchronizedList(new ArrayList<>());
    volatile double boltFromEye = Double.NaN, boltFromFeetUp = Double.NaN, boltFromForward = Double.NaN;
    int stage, at, i, fov0 = 70;
    // client evidence for the current segment
    int frames, missing, sweeps;
    final Set<Particle> seenSweeps = Collections.newSetFromMap(new WeakHashMap<>());
    final Set<String> stanceClips = new LinkedHashSet<>();
    final List<String> riseLog = new ArrayList<>();
    /** cast-live: each cast clip the client started, by start tick: {release tick, the client tick it was first seen}. */
    final Map<Double, double[]> liveCasts = new TreeMap<>();
    int riseFrames, riseHeavy;
    double riseMaxW;
    String attackClip = "";
    double attackStartTick = Double.NaN, attackReleaseTick = Double.NaN, attackFirstClipT = Double.NaN, attackFirstSeenTick = Double.NaN;
    boolean failedAny;

    void next(int tick, int s) { stage = s; at = tick; }

    @Override public void cleanup(MinecraftClient c) {
        live = null; Houses.forceRise = null;
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        try { c.options.getFov().setValue(fov0); } catch (Exception e) { /* leave it */ }
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    // ---- server side ----

    static void stand(GravesAndGlory.Unit u, double x, double z, float yaw) {
        var m = u.mob;
        if (!m.isAlive()) return;
        m.setPosition(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
        m.setVelocity(0, 0, 0);
        m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw);
    }

    GravesAndGlory.Unit spawn(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troop, int slot, double x, double z, float yaw) {
        var spec = Campaign.troop(troop);
        if (spec == null) throw new IllegalStateException("no troop " + troop);
        var u = b.spawn(s, spec, slot, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
        u.level = 3; u.weaponTier = 3; u.armourTier = 3;
        u.mob.setAiDisabled(true); u.mob.setInvulnerable(true);
        RankSync.changed(b, u);
        stand(u, x, z, yaw);
        u.mob.prevYaw = yaw; u.mob.prevBodyYaw = yaw; u.mob.prevHeadYaw = yaw;
        Phase1Checks.freeze(u);
        return u;
    }

    void place(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) { why = "no player"; return; }
        for (var u : b.units) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); }
        b.units.clear(); b.clearCorpses();
        var own = b.squads.get(player);
        if (own != null) { mine = own; for (var s : b.squads.values()) if (s.team != own.team) { foe = s; break; } }
        if (mine == null || foe == null) { why = "squads " + b.squads.size(); return; }
        b.world.setTimeOfDay(6000);
        b.world.setWeather(24000, 0, false, false);
        cx = Math.floor(p.getX()) + .5; cz = Math.floor(p.getZ()) + 7.5;
        anchor = spawn(b, foe, "revenant", 21, cx + 20, cz - 30, YAW); // keeps the round alive
        live = this;
        placed = true;
    }

    /** Segment start (server thread): a fresh subject (and its target) at the centre, the scripted ticks set from now. */
    void begin(GravesAndGlory.Battle b, Seg s) {
        long now = b.world.getTime();
        hitAt = announceAt = castAt = swingAt = liveFrom = -1; riseTick = boltTick = -1; riseWindow = -1; swingImpact = -1; serverWhy = "";
        boltFromEye = boltFromFeetUp = boltFromForward = Double.NaN;
        camDist = s.dist();
        subject = spawn(b, mine, s.troop(), 22, cx, cz, YAW); subjectId = subject.mob.getId(); target = null;
        long go = now + s.lead() + PRE;
        switch (s.slug()) {
            case "rise", "rise-king" -> hitAt = go;
            case "cast" -> { target = spawn(b, foe, "revenant", 23, cx + FX * 8, cz + FZ * 8, YAW + 180); announceAt = go; castAt = go + 10; }
            case "cast-live" -> { target = spawn(b, foe, "revenant", 23, cx + FX * 8, cz + FZ * 8, YAW + 180); liveFrom = go; liveBeats = 0; subject.nextAttack = 0; subject.abilityAt = 0; liveBolts.clear(); }
            default -> { target = spawn(b, foe, "revenant", 23, cx + FX * 1.6, cz + FZ * 1.6, YAW + 180); swingAt = go; }
        }
    }

    void end(GravesAndGlory.Battle b) {
        for (var u : new GravesAndGlory.Unit[]{subject, target}) if (u != null) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); b.units.remove(u); }
        b.clearCorpses(); b.shots.removeIf(sh -> sh.shooter() == subject); subject = null; target = null;
    }

    void serverHold(MinecraftServer server) {
        var b = GravesAndGlory.battle;
        if (b == null || !placed) return;
        long now = b.world.getTime();
        stand(anchor, cx + 20, cz - 30, YAW); Phase1Checks.freeze(anchor);
        var u = subject;
        if (u != null && u.mob.isAlive()) {
            stand(u, cx, cz, YAW);
            if (target != null && target.mob.isAlive()) { var t = target; stand(t, t.mob.getX(), t.mob.getZ(), YAW + 180); Phase1Checks.freeze(t); }
            if (now == hitAt) {
                Houses.forceRise = true;
                u.squad.traits.put(u.spec.house(), 1);
                u.mob.setInvulnerable(false);
                u.mob.damage(b.world.getDamageSources().generic(), 10000f);
                u.mob.setInvulnerable(true);
                Houses.forceRise = null;
                riseTick = u.riseTick; riseWindow = u.riseWindow;
                serverWhy = "lethal blow at world tick " + now + ": alive=" + u.mob.isAlive() + " rose=" + u.rose + " stunned for " + (u.stunnedUntil - b.time) + " ticks";
            }
            if (now == announceAt) serverWhy += "announce sent to " + Projectiles.drawAhead(b, u, target.mob, 10) + " player(s) at " + now + "; ";
            if (now == castAt) {
                u.abilityAt = 0; u.nextAttack = 0;
                Tactics.shoot(b, u, target.mob, b.time, true);
                for (var sh : b.shots) if (sh.shooter() == u && sh.kind().equals("bolt") && sh.fired() == b.time) {
                    boltTick = now;
                    Vec3d eye = u.mob.getEyePos(), from = sh.from();
                    boltFromEye = from.distanceTo(eye);
                    boltFromFeetUp = from.y - u.mob.getY();
                    boltFromForward = (from.x - u.mob.getX()) * FX + (from.z - u.mob.getZ()) * FZ;
                }
                serverWhy += "cast at " + now + " (swingTick " + u.swingTick + "), bolt " + (boltTick < 0 ? "missing" : "queued");
            }
            if (now == swingAt) {
                int before = Tactics.swings;
                u.nextAttack = 0;
                Tactics.swing(b, u, target.mob, b.time);
                swingImpact = Tactics.variants(u.spec.family()).get(u.lastVariant).impactTicks();
                serverWhy = "swing " + (Tactics.swings - before) + " at " + now + ", impact in " + swingImpact + " ticks";
            }
            if (liveFrom >= 0 && now >= liveFrom) { // left to the battle's Tactics: only its bolts are logged
                boolean ticked = b.running && b.time >= b.campaign.introUntil;
                if (now == liveFrom) { u.nextAttack = 0; u.abilityAt = 0; } // the hold before go froze it (Phase1Checks.freeze): its first shot is ready
                if (now == liveFrom) serverWhy += "units ticked by the battle " + ticked + " (running " + b.running + ", intro until " + b.campaign.introUntil + ", battle time " + b.time + "); ";
                if (!ticked) { Abilities.unitTick(b, u, b.time); Tactics.tick(b, u, b.time); } // this practice round does not tick its units: the battle's own per-unit step, for the subject alone
                if (b.time % 10 == u.slot % 10 && liveBeats++ < 6) serverWhy += "beat " + b.time + ": target " + (u.mob.getTarget() == null ? "none" : u.mob.getTarget().getType().getUntranslatedName()) + ", next attack " + u.nextAttack + ", ability at " + u.abilityAt + ", stunned until " + u.stunnedUntil + "; ";
                for (var sh : b.shots) if (sh.shooter() == u && sh.kind().equals("bolt") && sh.fired() == b.time && !liveBolts.contains(now)) liveBolts.add(now);
            } else if (hitAt < 0 || now < hitAt) Phase1Checks.freeze(u); // after the blow, a riser keeps its real stun
            else { u.nextAttack = Long.MAX_VALUE; u.abilityAt = Long.MAX_VALUE; }
        }
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p != null) { p.setNoGravity(true); p.networkHandler.requestTeleport(cx, (u == null ? p.getY() + .45 : u.mob.getY()) - .45, cz - camDist, 0f, PITCH); }
    }

    // ---- client side ----

    /** Sweep particles alive in the client's particle manager that this segment has not counted yet. */
    @SuppressWarnings("unchecked")
    int newSweeps(MinecraftClient c) {
        int n = 0;
        try {
            var f = net.minecraft.client.particle.ParticleManager.class.getDeclaredField("particles"); f.setAccessible(true);
            for (Queue<Particle> q : ((Map<?, Queue<Particle>>) f.get(c.particleManager)).values())
                for (Particle p : q) if (p instanceof SweepAttackParticle && seenSweeps.add(p)) n++;
        } catch (ReflectiveOperationException e) { serverWhy += " [particle read failed: " + e + "]"; }
        return n;
    }

    /** The old-model rise overlay {clip seconds, weight} TroopMotion shows now, or null (also null on code without TroopMotion.riseShown). */
    static double[] riseShown(int id) {
        try { var m = TroopMotion.class.getDeclaredMethod("riseShown", int.class); m.setAccessible(true); return (double[]) m.invoke(null, id); }
        catch (ReflectiveOperationException e) { return null; }
    }

    void sample(MinecraftClient c, Seg s) {
        sweeps += newSweeps(c);
        Entity e = c.world == null ? null : c.world.getEntityById(subjectId);
        if (e == null) { missing++; return; }
        double nowTick = c.world.getTime() + c.getTickDelta();
        MotionEngine.State st = TroopMotion.state(subjectId);
        if (st != null && !st.stances.isEmpty()) { var top = st.stances.get(st.stances.size() - 1); for (int k = 0; k < top.n; k++) if (top.clips[k] != null) stanceClips.add(top.clips[k].name()); }
        if (s.slug().startsWith("rise") && riseTick >= 0) {
            double el = c.world.getTime() - riseTick;
            if (el >= 0 && el < riseWindow) {
                riseFrames++;
                double[] r = riseShown(subjectId);
                if (r != null) { riseMaxW = Math.max(riseMaxW, r[1]); if (r[1] > .9) riseHeavy++; }
                if (riseLog.size() < 40) riseLog.add((long) el + ":" + (r == null ? "-" : String.format("%.2f/%.2f", r[0], r[1])));
            }
        }
        if (st != null && s.slug().equals("cast-live")) for (var tr : st.tracks) if (tr.kind == MotionEngine.ATTACK && tr.a != null)
            liveCasts.putIfAbsent(tr.start * Clip.TICKS, new double[]{(tr.start + tr.impact) * Clip.TICKS, nowTick});
        if (st != null && s.slug().equals("cast")) for (var tr : st.tracks) if (tr.kind == MotionEngine.ATTACK && tr.a != null && Double.isNaN(attackStartTick)) {
            attackClip = tr.a.name(); attackStartTick = tr.start * Clip.TICKS; attackReleaseTick = (tr.start + tr.impact) * Clip.TICKS;
            attackFirstSeenTick = nowTick; attackFirstClipT = tr.time(nowTick / Clip.TICKS) * Clip.TICKS;
        }
    }

    void report(Seg s) {
        String base = frames + "/" + s.total() + " frames" + (missing > 0 ? ", entity missing on " + missing : "") + ", stance clips " + stanceClips + "; server: " + serverWhy;
        boolean ok;
        String id = "p1-old-gaps-" + s.slug();
        switch (s.slug()) {
            case "rise", "rise-king" -> {
                ok = riseTick >= 0 && riseFrames > 0 && riseHeavy * 2 >= riseFrames;
                base += "; rise_v1 tick " + riseTick + " window " + riseWindow + ", in-window frames " + riseFrames + ", fall overlay over 0.9 on " + riseHeavy + " (max weight " + String.format("%.2f", riseMaxW) + "); elapsed:clipT/weight " + riseLog;
            }
            case "cast" -> {
                ok = boltTick >= 0 && Math.abs(attackReleaseTick - boltTick) < .51 && attackFirstClipT < 10; // heard before its release (clip tick 10): a loaded client hears it a few ticks late and plays on from mid-clip
                base += "; client attack track " + attackClip + " start tick " + attackStartTick + ", release (impact) tick " + attackReleaseTick + ", first seen at " + String.format("%.2f", attackFirstSeenTick)
                    + " clip tick " + String.format("%.2f", attackFirstClipT) + "; bolt world tick " + boltTick + " (release - bolt = " + (attackReleaseTick - boltTick) + ")";
                boolean origin = boltTick >= 0 && boltFromEye < .01;
                Phase1Checks.report("p1-old-gaps-bolt-origin", origin, "hexer on the old model: bolt starts " + String.format("%.3f", boltFromEye) + " blocks from its eyes, "
                    + String.format("%.2f", boltFromFeetUp) + " up from its feet and " + String.format("%.2f", boltFromForward) + " forward (want the eyes: the old origin)");
                failedAny |= !origin;
            }
            case "cast-live" -> {
                List<Long> bolts; synchronized (liveBolts) { bolts = new ArrayList<>(liveBolts); }
                StringBuilder pairs = new StringBuilder(); int matched = 0; double worst = 0;
                for (long bt : bolts) {
                    double best = Double.NaN;
                    for (double[] v : liveCasts.values()) if (Double.isNaN(best) || Math.abs(v[0] - bt) < Math.abs(best - bt)) best = v[0];
                    if (!Double.isNaN(best) && Math.abs(best - bt) < .51) matched++;
                    if (!Double.isNaN(best)) worst = Math.max(worst, Math.abs(best - bt));
                    pairs.append(bt).append("->").append(best).append(' ');
                }
                StringBuilder casts = new StringBuilder();
                for (var en : liveCasts.entrySet()) casts.append(String.format("start %.0f release %.0f seen %.2f; ", en.getKey(), en.getValue()[0], en.getValue()[1]));
                ok = !bolts.isEmpty() && matched == bolts.size() && liveCasts.size() == bolts.size();
                base += "; bolts (world tick -> nearest client release) " + pairs + "| client cast clips " + casts + "| matched " + matched + "/" + bolts.size() + ", worst " + worst;
            }
            case "sweep" -> { ok = sweeps >= 1; base += "; sweep particles seen " + sweeps + " (want at least 1: old model)"; }
            default -> { ok = sweeps == 0 && GeoTroopRenderer.ENABLED; base += "; sweep particles seen " + sweeps + " (want 0: the knight draws on GeckoLib)"; }
        }
        failedAny |= !ok;
        Phase1Checks.report(id, ok, base);
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 6000 && stage != 9 && stage < 99) { Phase1Checks.report("p1-old-gaps-timeout", false, "stuck at stage " + stage + ", segment " + i); next(tick, 9); }
        int k = tick - at - 1;
        switch (stage) {
            case 0 -> {
                if (tick == 20) {
                    c.setScreen(null); player = c.player.getUuid();
                    c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON);
                    fov0 = c.options.getFov().getValue(); c.options.getFov().setValue(FOV);
                    next(tick, 1);
                }
            }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-old-gaps", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 80) Phase1Checks.server(c, this::place);
                if (k > 100) {
                    if (!placed) { Phase1Checks.report("p1-old-gaps-placed", false, "could not place: " + why); next(tick, 9); return; }
                    Phase1Checks.report("p1-old-gaps-placed", true, "geoTroops=" + System.getProperty("gravesandglory.geoTroops") + ", GeckoLib enabled " + GeoTroopRenderer.ENABLED);
                    i = 0; next(tick, 2);
                }
            }
            case 2 -> {
                Seg s = SEGS.get(i);
                if (k == 0 && s.troop() == null) { // every candidate is approved (drawn through GeckoLib): nothing on the old model to film; re-point the segment
                    Phase1Checks.report("p1-old-gaps-" + s.slug(), true, "skipped: every candidate troop now draws through GeckoLib (GeoTroops.APPROVED)");
                    if (++i >= SEGS.size()) { Phase1Checks.report("p1-old-gaps", !failedAny, SEGS.size() + " segments"); next(tick, 9); } else next(tick, 2);
                    return;
                }
                if (k == 0) {
                    frames = 0; missing = 0; sweeps = 0; stanceClips.clear(); riseLog.clear(); riseFrames = 0; riseHeavy = 0; riseMaxW = 0;
                    liveCasts.clear();
                    attackClip = ""; attackStartTick = attackReleaseTick = attackFirstClipT = attackFirstSeenTick = Double.NaN;
                    newSweeps(c);
                    Phase1Checks.server(c, b -> begin(b, s));
                }
                int f = k - PRE;
                if (f >= 0 && f < s.total()) {
                    sample(c, s);
                    if (s.shots()) Phase1Checks.shot(c, String.format("og-%s-%03d", s.slug(), f));
                    frames++;
                }
                if (f >= s.total()) {
                    report(s);
                    Phase1Checks.server(c, this::end);
                    if (++i >= SEGS.size()) { Phase1Checks.report("p1-old-gaps", !failedAny, SEGS.size() + " segments"); next(tick, 9); }
                    else next(tick, 2);
                }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                if (k > 40) { Phase1Checks.finish(c, "old-gaps"); stage = 99; }
            }
            default -> {}
        }
    }

}
