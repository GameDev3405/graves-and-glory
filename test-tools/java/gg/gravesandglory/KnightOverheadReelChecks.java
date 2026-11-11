package gg.gravesandglory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Capture of the knight's overhead swing (GRAVES_PHASE1_CHECKS=knight-overhead-reel; the knight is a GeoTroopRenderer.APPROVED troop, no flag
 * needed). In a practice battle cleared of every troop, one tier III knight (own side, AI off, invulnerable) faces east at a tier III foe
 * knight {@link #GAP} blocks ahead (frozen, healed every tick), in clear daylight. The server makes the knight swing the game's own way
 * (Tactics.shoot, cooldown taken, arm swung as the duel in GeoKnightChecks does) every {@link #EVERY} ticks, with the battle's swing roll
 * seeded so BattleMath.pickVariant picks the family's overhead row: the real swing path (attack_v1 event, pending blow, landing) plays.
 * For each camera (a near side-on view from the knight's sword side and a near front three-quarter view) the client saves one frame per
 * clip tick, p1-knight-overhead-&lt;cam&gt;-tNN.png, named by the tick the client's AttackController showed; the swing repeats until every
 * tick 0..{@link #FRAMES}-1 is shot (client ticks stall on screenshots, so one swing does not catch them all) or {@link #MAX_SWINGS}.
 * Checks: p1-knight-overhead-clip (every swing the client showed played attack_overhead, its impact marker the variant's impactTicks),
 * p1-knight-overhead-impact (every swing's blow was planned, left the pending list and hurt the foe exactly impactTicks after the swing
 * began), p1-knight-overhead-&lt;cam&gt;-frames (all clip ticks shot). Never shipped.
 */
final class KnightOverheadReelChecks implements Phase1Checks.Suite {
    static final String TROOP = "knight";
    /** The knight faces east (+x); its right, sword side is south (+z). */
    static final float YAW = -90f, FOE_YAW = 90f;
    /** The foe stands ahead and a little to the knight's left (north), within reach (2.8): off the line of both cameras, so it never hides the knight. */
    static final double GAP = 1.8, FOE_DZ = -0.8;
    /** Cameras: name, offset from the knight (dx, dz), downward pitch. The eye looks at the knight. */
    static final String[] CAMS = {"side", "threequarter"};
    static final double[][] CAM_AT = {{0, 4.5}, {3.2, 3.2}};
    static final float PITCH = 4f;
    static final int FOV = 48;
    /** The overhead clip's length in ticks (knight.animation.json attack_overhead: 22). */
    static final int FRAMES = 22;
    /** World ticks between swings (the clip, its blend out and a rest), and the most swings per camera. */
    static final int EVERY = 40, MAX_SWINGS = 14;

    static volatile KnightOverheadReelChecks live;
    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            KnightOverheadReelChecks r = live;
            if (r != null) try { r.serverHold(server); } catch (Exception e) { Phase1Checks.report("p1-knight-overhead-exception", false, "server hold: " + e); e.printStackTrace(); }
        });
    }

    // ---- state ----
    volatile UUID player;
    volatile boolean battleReady, placed;
    volatile String why = "";
    volatile double cx, cz;
    Spec.TroopsRow spec;
    volatile GravesAndGlory.Unit knight, foe;
    volatile int knightId = -1, ovh = -1, impactTicks = -1;
    volatile String ovhId = "";
    /** Server: the camera shown, whether the knight swings, the world tick of its last swing. */
    volatile int cam;
    volatile boolean swinging;
    long lastSwing = -100000;
    /** Server swing log: {swing world tick, planned delay (landsAt - swungAt), tick the blow left the pending list (-1), tick the foe was hurt (-1), variant}. */
    final List<long[]> swings = Collections.synchronizedList(new ArrayList<>());
    GravesAndGlory.Battle.PendingStrike open; long[] openRow;
    int stage, at, fov0 = 70;
    // client
    final Set<Integer> taken = new TreeSet<>();
    final Set<String> clips = new LinkedHashSet<>(), impacts = new LinkedHashSet<>();
    int swingsAtStart;
    double lastT = -1;

    void next(int tick, int s) { stage = s; at = tick; }

    @Override public void cleanup(MinecraftClient c) {
        live = null;
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

    GravesAndGlory.Unit spawn(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, double x, double z, float yaw) {
        var u = b.spawn(s, spec, slot, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
        u.level = 3; u.weaponTier = 3; u.armourTier = 3;
        u.mob.setAiDisabled(true);
        RankSync.changed(b, u);
        stand(u, x, z, yaw);
        u.mob.prevYaw = yaw; u.mob.prevBodyYaw = yaw; u.mob.prevHeadYaw = yaw;
        Phase1Checks.freeze(u);
        return u;
    }

    void place(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) { why = "no player"; return; }
        spec = Campaign.troop(TROOP);
        if (spec == null) { why = "no " + TROOP + " troop"; return; }
        var rows = Tactics.variants(spec.family());
        for (int v = 0; v < rows.size(); v++) if (rows.get(v).id().endsWith("_overhead")) { ovh = v; ovhId = rows.get(v).id(); impactTicks = rows.get(v).impactTicks(); }
        if (ovh < 0) { why = "no overhead variant in family " + spec.family(); return; }
        GravesAndGlory.Squad mine = null, other = null;
        for (var u : b.units) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); }
        b.units.clear(); b.clearCorpses();
        var own = b.squads.get(player);
        if (own != null) { mine = own; for (var s : b.squads.values()) if (s.team != own.team) { other = s; break; } }
        if (mine == null || other == null) { why = "squads " + b.squads.size(); return; }
        b.world.setTimeOfDay(6000);
        b.world.setWeather(24000, 0, false, false);
        cx = Math.floor(p.getX()) + .5; cz = Math.floor(p.getZ()) + 7.5;
        knight = spawn(b, mine, 20, cx, cz, YAW);
        knight.mob.setInvulnerable(true);
        foe = spawn(b, other, 21, cx + GAP, cz + FOE_DZ, FOE_YAW);
        foe.mob.setInvulnerable(false);
        knightId = knight.mob.getId();
        live = this;
        placed = true;
    }

    /** A seed for the battle's rolls whose next draw makes BattleMath.pickVariant pick the overhead after the knight's last swing. */
    long seedFor(int count, int last) {
        for (long s = 1; s < 10000; s++) if (BattleMath.pickVariant(count, last, new Random(s).nextLong()) == ovh) return s;
        throw new IllegalStateException("no seed picks variant " + ovh);
    }

    void serverHold(MinecraftServer server) {
        var b = GravesAndGlory.battle;
        var u = knight; var f = foe;
        if (b == null || !placed || u == null || f == null) return;
        long now = b.world.getTime();
        stand(u, cx, cz, YAW); stand(f, cx + GAP, cz + FOE_DZ, FOE_YAW);
        Phase1Checks.freeze(f);
        // the foe: hurt only by the knight's blows (it never swings), healed back each tick so it never falls
        if (f.mob.isAlive() && f.mob.getHealth() < f.mob.getMaxHealth()) {
            if (openRow != null && openRow[3] < 0) openRow[3] = now;
            f.mob.setHealth(f.mob.getMaxHealth());
        }
        if (open != null && !b.strikes.contains(open)) { openRow[2] = now; open = null; }
        if (openRow != null && openRow[2] >= 0 && openRow[3] >= 0) openRow = null;
        if (swinging && open == null && now - lastSwing >= EVERY && u.mob.isAlive() && f.mob.isAlive() && !Tactics.pending(b, u)) {
            var rows = Tactics.variants(u.spec.family());
            // pickVariant never repeats the last swing: the knight's memory of it is set to another row, so the overhead can be picked again
            if (u.lastVariant == ovh) u.lastVariant = (ovh + 1) % rows.size();
            b.rolls.setSeed(seedFor(rows.size(), u.lastVariant));
            u.nextAttack = b.time + u.spec.cooldown(); u.mob.swingHand(Hand.MAIN_HAND);
            Tactics.shoot(b, u, f.mob, b.time, false);
            lastSwing = now;
            GravesAndGlory.Battle.PendingStrike ps = null;
            for (var st : b.strikes) if (st.attacker() == u && st.swungAt() == u.swungAt) ps = st;
            long[] row = {u.swingTick, ps == null ? 0 : ps.landsAt() - ps.swungAt(), ps == null ? now : -1, -1, u.lastVariant};
            swings.add(row); open = ps; openRow = row;
        } else Phase1Checks.freeze(u);
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(player);
        if (p != null) {
            double[] o = CAM_AT[cam];
            float yaw = (float) Math.toDegrees(Math.atan2(o[0], -o[1]));
            p.setNoGravity(true);
            p.networkHandler.requestTeleport(cx + o[0], u.mob.getY() - .45, cz + o[1], yaw, PITCH);
        }
    }

    // ---- client side ----

    public void step(MinecraftClient c, int tick) {
        if (tick > 6000 && stage != 9 && stage < 99) { Phase1Checks.report("p1-knight-overhead-timeout", false, "stuck at stage " + stage); next(tick, 9); }
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
                    if (k > 600) { Phase1Checks.report("p1-knight-overhead", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 80) Phase1Checks.server(c, this::place);
                if (k > 100) {
                    if (!placed) { Phase1Checks.report("p1-knight-overhead-placed", false, "could not place the knights: " + why); next(tick, 9); return; }
                    Phase1Checks.report("p1-knight-overhead-placed", true, "tier III " + TROOP + " vs foe at " + GAP + " blocks, variant " + ovh + " " + ovhId + " impactTicks " + impactTicks + ", fov " + FOV);
                    cam = 0; next(tick, 2);
                }
            }
            case 2 -> {
                if (k == 0) { taken.clear(); swingsAtStart = swings.size(); lastT = -1; c.setScreen(null); c.options.hudHidden = true; }
                if (k == 10) swinging = true;
                if (k > 10 && c.world != null) {
                    Entity e = c.world.getEntityById(knightId);
                    AttackController ac = e == null ? null : AttackController.LIVE.get(e);
                    if (ac != null && ac.shownT >= 0 && !ac.shownClip.isEmpty() && (ac.shownWeight > 0 || ac.shownT < .5)) {
                        lastT = ac.shownT;
                        clips.add(ac.shownClip); impacts.add(ac.shownClip + "@" + ac.shownImpact);
                        int tt = (int) Math.round(ac.shownT);
                        if (ac.shownClip.startsWith("attack_overhead") && Math.abs(ac.shownT - tt) <= .3 && tt >= 0 && tt < FRAMES && taken.add(tt))
                            Phase1Checks.shot(c, String.format("knight-overhead-%s-t%02d", CAMS[cam], tt));
                    }
                }
                boolean all = taken.size() >= FRAMES;
                if (all || swings.size() >= (cam + 1) * MAX_SWINGS || k > 2400) {
                    swinging = false;
                    List<Integer> miss = new ArrayList<>();
                    for (int t = 0; t < FRAMES; t++) if (!taken.contains(t)) miss.add(t);
                    Phase1Checks.report("p1-knight-overhead-" + CAMS[cam] + "-frames", all,
                        taken.size() + "/" + FRAMES + " clip ticks shot over " + (swings.size() - swingsAtStart) + " swings" + (miss.isEmpty() ? "" : ", missing ticks " + miss));
                    next(tick, 3);
                }
            }
            case 3 -> {
                // let the last swing's blow land and its clip end before the camera moves
                if (k > EVERY) {
                    if (cam + 1 < CAMS.length) { cam++; next(tick, 2); } else next(tick, 4);
                }
            }
            case 4 -> {
                List<String> bad = new ArrayList<>(); int landed = 0;
                synchronized (swings) {
                    for (long[] r : swings) {
                        long land = r[2] < 0 ? -1 : r[2] - r[0], hurt = r[3] < 0 ? -1 : r[3] - r[0];
                        if (r[4] != ovh || r[1] != impactTicks || land != impactTicks || hurt != impactTicks)
                            bad.add("swing@" + r[0] + " variant " + r[4] + " planned " + r[1] + " left pending +" + land + " hurt +" + hurt);
                        else landed++;
                    }
                }
                Phase1Checks.report("p1-knight-overhead-impact", bad.isEmpty() && landed > 0,
                    landed + "/" + swings.size() + " swings were " + ovhId + ", planned, left the pending list and hurt the foe exactly " + impactTicks + " ticks after the swing began" + (bad.isEmpty() ? "" : "; bad " + bad));
                boolean clipOk = clips.stream().allMatch(cl -> cl.startsWith("attack_overhead")) && clips.contains("attack_overhead")
                    && impacts.stream().filter(s -> s.startsWith("attack_overhead@")).allMatch(s -> Math.round(Double.parseDouble(s.substring(s.indexOf('@') + 1))) == impactTicks);
                Phase1Checks.report("p1-knight-overhead-clip", clipOk, "client showed clips " + clips + ", impact markers " + impacts + " (want attack_overhead only, marker " + impactTicks + ")");
                next(tick, 9);
            }
            case 9 -> {
                swinging = false;
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { live = null; if (b.running) b.campaign.returnLobby(); });
                if (k > 40) { Phase1Checks.finish(c, "knight-overhead-reel"); stage = 99; }
            }
            default -> {}
        }
    }
}
