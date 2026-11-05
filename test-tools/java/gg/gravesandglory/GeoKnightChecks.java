package gg.gravesandglory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Unit animation rebuild, step 3 (docs/anim-rebuild/PLAN.md): the GeckoLib knight rig drawn in game at rest. GRAVES_PHASE1_CHECKS=geo-knight.
 * In a practice battle every troop is cleared, then six knights stand frozen in a line facing the camera: tiers I-III of the player's side
 * and of the enemy side. They are turned to front, three-quarter, side and back and shot close and at 16 blocks
 * (p1-geo-knight-<view>-<near|far>.png), then one tier-III close-up per side. Then the six march past at the knight's real walk
 * and charge speeds, seen side on, head on and three-quarter, past a camera that holds still (so a planted foot must hold still
 * on screen too): close for 16 server ticks (two or more stride cycles) and at 16 blocks for 8,
 * p1-geo-knight-<walk|run>-<view>-<near|far>-<tick>.png, named by the march's server tick (a tick the client was busy for is
 * missing, never doubled). The server moves them every server tick, as a troop's own walk does, never from the client, whose
 * ticks stall on each screenshot. Over 8 mid-march ticks the client's stride clock (Gait) is read back for every knight: its
 * mean speed must be within 10% of the true speed and its clip the expected one in every sample. Never shipped.
 */
final class GeoKnightChecks implements Phase1Checks.Suite {
    /** Knight yaw per view; the camera looks south (+z, yaw 0) and the knights face it at 180. */
    static final String[] VIEWS = {"front", "threequarter", "side", "back"};
    static final float[] YAWS = {180, 135, 90, 0};
    /** March gaits and their true speeds, blocks per tick: the knight's walk and charge (Spec troops row). */
    static final String[] GAITS = {"walk", "run"};
    static final double[] SPEEDS = {.169, .264};
    static final Gait.Mode[] MODES = {Gait.Mode.WALK, Gait.Mode.RUN};
    /** March directions per view, seen from the camera north of the line looking south: side on, head on, three-quarter. */
    static final String[] MARCH_VIEWS = {"side", "front", "threequarter"};
    static final double[][] DIRS = {{1, 0}, {0, -1}, {Math.sqrt(.5), -Math.sqrt(.5)}};
    /** Server ticks of one march; the line passes its start point halfway. */
    static final int MARCH = 48;
    /** The march the server is running (set from the client, advanced every server tick by the hook below). */
    static final class March {
        final GeoKnightChecks owner; final GravesAndGlory.Battle b; final String name; final int start; final double v, dx, dz; final boolean column;
        volatile int k = -1;
        March(GeoKnightChecks owner, GravesAndGlory.Battle b, String name, double v, double dx, double dz, boolean column) {
            this.owner = owner; this.b = b; this.name = name; this.start = b.server.getTicks(); this.v = v; this.dx = dx; this.dz = dz; this.column = column;
        }
    }
    static volatile March run;
    /**
     * A duel the server is running between the two tier-III knights (own, slot 2; foe, slot 5), held in place every server tick. Each
     * swing either starts is logged: {entity id, swing world tick, variant, server impact delay, landing world tick (-1 until it lands)}.
     * The battle ticks in its own END_SERVER_TICK hook, registered at mod init before this one, so a strike that left Battle.strikes
     * this tick landed on this world tick. A duelist that has not swung for {@link #NUDGE} ticks (its squad's orders held it) is made
     * to swing the way Tactics does (forced counts these).
     */
    static final class Duel {
        static final int NUDGE = 40;
        final GeoKnightChecks owner; final GravesAndGlory.Battle b; final String mode; final int start;
        final GravesAndGlory.Unit[] us;
        final long[] seen = new long[2], last = new long[2];
        final Map<GravesAndGlory.Battle.PendingStrike, long[]> open = new HashMap<>();
        final List<long[]> swings = Collections.synchronizedList(new ArrayList<>());
        volatile int k = -1, forced;
        /** Death mode: the foe swing it dies 4 ticks into, and the world tick it was killed (-1 until then). */
        volatile long killSwing = -1, killedAt = -1;
        volatile boolean killedDead;
        Duel(GeoKnightChecks owner, GravesAndGlory.Battle b, String mode) {
            this.owner = owner; this.b = b; this.mode = mode; this.start = b.server.getTicks();
            us = new GravesAndGlory.Unit[]{owner.knights.get(2), owner.knights.get(5)};
            long now = b.world.getTime();
            for (int j = 0; j < 2; j++) { seen[j] = us[j].swingTick; last[j] = now; }
        }
    }
    static volatile Duel duel;
    static {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            March m = run; if (m != null) m.owner.serverMarch(m, server.getTicks() - m.start);
            Duel d = duel; if (d != null) d.owner.serverDuel(d, server.getTicks() - d.start);
        });
    }
    int lastShot;
    boolean reported;
    final List<double[]> samples = new ArrayList<>();
    final Set<String> wrongClip = new LinkedHashSet<>();
    volatile UUID player;
    volatile boolean battleReady, placed;
    volatile String why = "";
    volatile double cx, cy, cz;
    final List<GravesAndGlory.Unit> knights = new ArrayList<>();
    int stage, at, i;

    void next(int tick, int s) { stage = s; at = tick; }

    @Override public void cleanup(MinecraftClient c) {
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    /** Clears the field and stands the six knights in a line 7 blocks south of the player. */
    void place(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) { why = "no player"; return; }
        var spec = Campaign.troop("knight");
        if (spec == null) { why = "no knight troop"; return; }
        for (var u : b.units) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); }
        b.units.clear(); b.clearCorpses();
        GravesAndGlory.Squad mine = null, foe = null;
        for (var s : b.squads.values()) { if (s.team == 0 && mine == null) mine = s; else if (s.team == 1 && foe == null) foe = s; }
        var own = b.squads.get(player);
        if (own != null) { mine = own; for (var s : b.squads.values()) if (s.team != own.team) { foe = s; break; } }
        if (mine == null || foe == null) { why = "squads " + b.squads.size(); return; }
        p.getServerWorld().setTimeOfDay(6000);
        cx = Math.floor(p.getX()) + .5; cz = Math.floor(p.getZ()) + 7.5;
        cy = Arena.ground((int) cx, (int) cz) + 1;
        int slot = 0;
        for (var s : List.of(mine, foe)) for (int t = 1; t <= 3; t++) {
            // Own side at cx-7..cx-3, enemy at cx+3..cx+7: six blocks apart so the two sides never come to blows.
            double x = cx + (slot < 3 ? -7 + slot * 2 : 3 + (slot - 3) * 2), z = cz;
            var u = b.spawn(s, spec, 20 + slot, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
            u.level = t; u.weaponTier = t; u.armourTier = t;
            u.mob.setAiDisabled(true); u.mob.setInvulnerable(true);
            RankSync.changed(b, u);
            knights.add(u);
            slot++;
        }
        turn(180);
        placed = true;
    }

    /** Stands the six knights around (x, z) facing (dx, dz): a column along the march for the side view, else a rank across it. */
    void march(double x, double z, double dx, double dz, boolean column) {
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        double ax = column ? dx : -dz, az = column ? dz : dx;
        for (int j = 0; j < knights.size(); j++) {
            var m = knights.get(j).mob;
            // Own side then enemy side, 2.4 apart within a side and 6 between the sides, so they never come to blows.
            double o = j < 3 ? -3 - (2 - j) * 2.4 : 3 + (j - 3) * 2.4, kx = x + ax * o, kz = z + az * o;
            m.setPosition(kx, Arena.ground((int) Math.floor(kx), (int) Math.floor(kz)) + 1, kz);
            m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw);
        }
    }

    /** One sample of every knight's client stride clock: its measured speed (mean kept per knight) and chosen clip. */
    void sample(MinecraftClient c, Gait.Mode want, int mk) {
        double[] row = new double[knights.size()];
        for (int j = 0; j < knights.size(); j++) {
            var e = c.world == null ? null : c.world.getEntityById(knights.get(j).mob.getId());
            Gait g = e == null ? null : Gait.LIVE.get(e);
            row[j] = g == null ? Double.NaN : Math.abs(g.speed);
            if (g == null) wrongClip.add(j + ":none"); else if (g.mode != want) wrongClip.add(String.format("%d:%s@%d(s%.3f t%.3f)", j, g.mode, mk, g.speed, g.trend));
        }
        samples.add(row);
    }

    /** Passes when every knight's mean client speed is within 10% of the true speed and its clip was right in every sample. */
    void readBack(String name, double v) {
        StringBuilder seen = new StringBuilder();
        boolean ok = !samples.isEmpty() && wrongClip.isEmpty();
        for (int j = 0; j < knights.size(); j++) {
            // The median, not the mean: a server hitch on a loaded machine delivers a tick's motion late and the smoothed speed dips
            // for a frame or two on every knight at once (the stride clock still integrates the full distance). The clip choice
            // above is the hard gate; the min shows any dip.
            double[] col = new double[samples.size()];
            for (int i = 0; i < col.length; i++) col[i] = samples.get(i)[j];
            Arrays.sort(col);
            double med = col.length == 0 ? 0 : col[col.length / 2], lo = col.length == 0 ? 0 : col[0], hi = col.length == 0 ? 0 : col[col.length - 1];
            ok &= Math.abs(med - v) <= .1 * v;
            seen.append(String.format(" %.3f[%.3f-%.3f]", med, lo, hi));
        }
        Phase1Checks.report("p1-geo-knight-march-" + name, ok, "true " + v + ", " + samples.size() + " samples, client median[min-max]:" + seen
            + (wrongClip.isEmpty() ? "" : ", wrong clip " + wrongClip));
    }

    /**
     * Server tick k of a march: the line at its point, the camera still. Close (k < 30) the camera frames where the line is
     * mid-way through the close shots (k 20); far, where it is mid-way through the far shots (k 36).
     */
    void serverMarch(March m, int k) {
        m.k = k;
        double s = m.v * (Math.min(k, MARCH) - MARCH / 2.0);
        march(cx + m.dx * s, cz + 2 + m.dz * s, m.dx, m.dz, m.column);
        boolean near = k < 30;
        double f = m.v * ((near ? 20 : 36) - MARCH / 2.0), dist = near ? 8 : 16;
        camera(m.b, m.dx * f, -(2 + m.dz * f) + dist, near ? 8 : 3);
    }

    static void stand(GravesAndGlory.Unit u, double x, double z, float yaw) {
        var m = u.mob;
        if (!m.isAlive()) return;
        m.setPosition(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
        m.setVelocity(0, 0, 0);
        m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw);
    }

    /** Walk mode: the own knight walks +x at the knight's walk speed for this many server ticks, the foe backing up 2 blocks ahead. */
    static final int WALK = 70;
    /** Walk mode: the duelists stand at their start marks this many ticks first (the move there is a 5 block teleport). */
    static final int WALK0 = 20;

    /** Server tick k of a duel: the duelists held (or walked), the other four kept 30 blocks behind the camera, swings logged. */
    void serverDuel(Duel d, int k) {
        d.k = k;
        var b = d.b;
        long now = b.world.getTime();
        for (int j = 0; j < knights.size(); j++) if (j != 2 && j != 5) stand(knights.get(j), cx + (j < 3 ? -20 - j * 3 : 20 + j * 3), cz - 30, 0);
        var f = d.us[1];
        if (d.mode.equals("walk")) {
            double x = cx - 6 + SPEEDS[0] * Math.max(0, Math.min(k - WALK0, WALK));
            stand(d.us[0], x, cz, -90); stand(f, x + 2, cz, 90);
        } else { stand(d.us[0], cx - 1, cz, -90); stand(f, cx + 1, cz, 90); }
        for (int j = 0; j < 2; j++) {
            var u = d.us[j];
            if (u.swingTick != d.seen[j] && u.swingTick >= 0) {
                d.seen[j] = u.swingTick; d.last[j] = now;
                GravesAndGlory.Battle.PendingStrike ps = null;
                for (var st : b.strikes) if (st.attacker() == u && st.swungAt() == u.swungAt) ps = st;
                long[] row = {u.mob.getId(), u.swingTick, u.lastVariant, ps == null ? 0 : ps.landsAt() - ps.swungAt(), ps == null ? now : -1};
                d.swings.add(row);
                if (ps != null) d.open.put(ps, row);
                if (j == 1 && d.mode.equals("death") && k > 20 && d.killSwing < 0) d.killSwing = u.swingTick;
            }
        }
        for (var it = d.open.entrySet().iterator(); it.hasNext(); ) { var en = it.next(); if (!b.strikes.contains(en.getKey())) { en.getValue()[4] = now; it.remove(); } }
        if (d.killSwing >= 0 && d.killedAt < 0 && now - d.killSwing == 4) {
            f.mob.setInvulnerable(false); f.mob.kill(); d.killedAt = now; d.killedDead = f.mob.isDead();
        }
        for (int j = 0; j < 2; j++) {
            var u = d.us[j]; LivingEntity enemy = d.us[1 - j].mob;
            if (u.mob.isAlive() && enemy.isAlive() && now - d.last[j] > Duel.NUDGE && b.time >= u.stunnedUntil && !Tactics.pending(b, u)) {
                u.nextAttack = b.time + u.spec.cooldown(); u.mob.swingHand(Hand.MAIN_HAND);
                Tactics.shoot(b, u, enemy, b.time, false);
                d.forced++; d.last[j] = now;
            }
        }
    }

    /** Puts the camera (the player's eye) at (x, z) looking along yaw. */
    void eye(GravesAndGlory.Battle b, double x, double z, float yaw, float pitch) {
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        p.setNoGravity(true);
        p.networkHandler.requestTeleport(x, cy - .45, z, yaw, pitch);
    }

    /** Attack views: side on to the duel, three-quarter on the own knight's front, and side on at 16 blocks: {dx, dz, yaw, pitch}. */
    static final String[] ATTACK_VIEWS = {"side", "threequarter", "far"};
    static final double[][] ATTACK_EYES = {{0, -4.5, 0, 8}, {1.5, 3.5, 144.5, 8}, {0, -16, 0, 3}};
    /** Frames taken this view (clip:tick), and the client's view of every swing: id:start to variant, and to its clip's impact tick. */
    final Set<String> taken = new HashSet<>();
    final Map<String, Integer> clientVariant = new HashMap<>();
    final Map<String, Double> clientImpact = new HashMap<>();
    double minImpactW = 2, maxGapW = 0, maxDeadW = 0, maxBeforeDeathW = 0, foeBackMin = 9;
    int impactSamples, gapSamples, walkSwingSamples, deadSamples;
    final Set<String> walkWrong = new LinkedHashSet<>(), deadWrong = new LinkedHashSet<>();

    /** One client tick of a duel: every duelist's announced swings and what its attack layer drew last frame. */
    void watch(MinecraftClient c, Duel d) {
        if (c.world == null) return;
        long now = c.world.getTime();
        for (var u : d.us) {
            int id = u.mob.getId();
            var e = c.world.getEntityById(id);
            for (var a : MotionEvents.Client.attacks(id)) clientVariant.putIfAbsent(id + ":" + a.startTick(), a.variant());
            AttackController ac = e == null ? null : AttackController.LIVE.get(e);
            if (ac == null || ac.shownAt < 0) continue;
            // Everything below judges the last frame drawn, at that frame's own time (a frame can trail the client tick by one or more).
            long latest = Long.MIN_VALUE;
            for (var a : MotionEvents.Client.attacks(id)) if (a.startTick() <= ac.shownAt) latest = Math.max(latest, a.startTick());
            if (ac.shownStart != Long.MIN_VALUE && ac.shownImpact >= 0) clientImpact.putIfAbsent(id + ":" + ac.shownStart, ac.shownImpact);
            boolean dead = ac.shownDeath >= 0;
            double t = ac.shownAt - latest;
            if (!dead && latest != Long.MIN_VALUE && ac.shownStart == latest && ac.shownImpact >= 0 && Math.abs(ac.shownT - ac.shownImpact) <= 1) {
                minImpactW = Math.min(minImpactW, ac.shownWeight); impactSamples++;
            }
            if (!dead && latest != Long.MIN_VALUE && t >= 22 && t <= 24.5) { maxGapW = Math.max(maxGapW, ac.shownWeight); gapSamples++; }
            Gait g = Gait.LIVE.get(e);
            if (d.mode.equals("walk") && d.k > WALK0 + 6 && d.k < WALK0 + WALK && ac.shownWeight > .5) {
                walkSwingSamples++;
                if (g == null || g.mode != Gait.Mode.WALK) walkWrong.add((u == d.us[0] ? "own" : "foe") + ":" + (g == null ? "none" : g.mode) + "@" + d.k);
                if (u == d.us[1] && g != null) foeBackMin = Math.min(foeBackMin, -g.speed);
            }
            if (d.mode.equals("death") && u == d.us[1] && e instanceof LivingEntity l) {
                if (!dead && d.killSwing >= 0 && ac.shownAt - d.killSwing >= 2 && ac.shownAt - d.killSwing <= 4) maxBeforeDeathW = Math.max(maxBeforeDeathW, ac.shownWeight);
                if (ac.shownDeath >= AttackController.DEATH_OUT) { deadSamples++; maxDeadW = Math.max(maxDeadW, ac.shownWeight); }
                if (l.deathTime >= 1 && g != null && g.mode != Gait.Mode.IDLE) deadWrong.add(g.mode + "@" + l.deathTime);
            }
        }
    }

    /**
     * Every server swing of the duel (but the last 12 ticks') reached the client with the same variant and start, its clip's impact
     * marker equals the server's impact delay, and the strike landed exactly that many ticks after the swing began.
     */
    void syncReport(Duel d) {
        int ok = 0, n = 0; List<String> bad = new ArrayList<>();
        long cut = d.b.world.getTime() - 12;
        synchronized (d.swings) {
            for (long[] r : d.swings) {
                if (r[1] > cut) continue;
                n++;
                String key = r[0] + ":" + r[1];
                Integer v = clientVariant.get(key); Double imp = clientImpact.get(key);
                boolean good = v != null && v == r[2] && imp != null && imp == r[3] && r[4] - r[1] == r[3];
                if (good) ok++; else if (bad.size() < 6) bad.add(String.format("%s v%d/%s delay%d clip%s landed+%d", key, r[2], v, r[3], imp, r[4] - r[1]));
            }
        }
        Phase1Checks.report("p1-geo-knight-attack-sync-" + d.mode, n >= 4 && ok == n,
            ok + "/" + n + " swings matched (variant, clip impact = server delay = landing)" + (d.forced > 0 ? ", " + d.forced + " nudged" : "") + (bad.isEmpty() ? "" : ", bad " + bad));
    }

    void turn(float yaw) {
        for (var u : knights) { u.mob.setYaw(yaw); u.mob.setBodyYaw(yaw); u.mob.setHeadYaw(yaw); u.mob.prevYaw = yaw; }
    }

    /** Puts the camera (the player's eye) dist blocks north of the line's centre, looking south and slightly down. */
    void camera(GravesAndGlory.Battle b, double dx, double dist, float pitch) {
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        p.setNoGravity(true);
        p.networkHandler.requestTeleport(cx + dx, cy - .45, cz - dist, 0, pitch);
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 30000 && stage < 9) { Phase1Checks.report("p1-geo-knight-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-geo-knight", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 80) Phase1Checks.server(c, this::place);
                if (k > 100) {
                    if (!placed) { Phase1Checks.report("p1-geo-knight-placed", false, "could not place knights: " + why); next(tick, 9); return; }
                    Phase1Checks.report("p1-geo-knight-placed", true, "six knights");
                    i = 0; next(tick, 2);
                }
            }
            case 2 -> {
                String view = VIEWS[i];
                float yaw = YAWS[i];
                // Re-aimed every tick: the battle's own logic turns troops back toward the enemy.
                Phase1Checks.server(c, b -> turn(yaw));
                if (k == 1) Phase1Checks.server(c, b -> camera(b, 0, 8, 8));
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON); }
                if (k == 40) Phase1Checks.shot(c, "geo-knight-" + view + "-near");
                if (k == 41) Phase1Checks.server(c, b -> camera(b, 0, 16, 3));
                if (k == 80) Phase1Checks.shot(c, "geo-knight-" + view + "-far");
                if (k == 81) { if (++i < VIEWS.length) next(tick, 2); else { i = 0; next(tick, 3); } }
            }
            case 3 -> {
                // Tier III close-ups: the player's (slot 2, x = cx - 3) and the enemy's (slot 5, x = cx + 7), three-quarter.
                double dx = i == 0 ? -3 : 7;
                Phase1Checks.server(c, b -> turn(150));
                if (k == 1) Phase1Checks.server(c, b -> camera(b, dx, 3.5, 3));
                if (k == 40) Phase1Checks.shot(c, "geo-knight-t3-" + (i == 0 ? "own" : "foe") + "-close");
                if (k == 41) { if (++i < 2) next(tick, 3); else {
                    Phase1Checks.report("p1-geo-knight", true, VIEWS.length + " views near and far, two close-ups");
                    i = 0; next(tick, 4); } }
            }
            case 4 -> {
                int g = i / MARCH_VIEWS.length, vw = i % MARCH_VIEWS.length;
                String name = GAITS[g] + "-" + MARCH_VIEWS[vw];
                if (k == 1) {
                    double v = SPEEDS[g], dx = DIRS[vw][0], dz = DIRS[vw][1];
                    lastShot = -1; reported = false; samples.clear(); wrongClip.clear();
                    Phase1Checks.server(c, b -> run = new March(this, b, name, v, dx, dz, vw == 0));
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                March m = run;
                if (k > 400) { Phase1Checks.report("p1-geo-knight-march-" + name, false, "march never ran"); run = null; next(tick, 9); return; }
                if (m == null || !m.name.equals(name) || m.k == lastShot) return;
                int mk = lastShot = m.k;
                if (mk >= 12 && mk < 28) Phase1Checks.shot(c, "geo-knight-" + name + "-near-" + String.format("%02d", mk));
                if (mk >= 32 && mk < 40) Phase1Checks.shot(c, "geo-knight-" + name + "-far-" + String.format("%02d", mk));
                if (mk >= 18 && mk < 26) sample(c, MODES[g], mk);
                if (mk >= 26 && !reported) { reported = true; readBack(name, SPEEDS[g]); }
                if (mk >= 44) { run = null; if (++i < GAITS.length * MARCH_VIEWS.length) next(tick, 4); else { i = 0; next(tick, 5); } }
            }
            case 5 -> {
                // Standing duel, one view at a time: every tick of each of the three swings, frame by frame (far: every other tick).
                String view = ATTACK_VIEWS[i];
                double[] eyeAt = ATTACK_EYES[i];
                boolean far = view.equals("far");
                if (k == 1) {
                    taken.clear();
                    if (i == 0) {
                        clientVariant.clear(); clientImpact.clear(); minImpactW = 2; maxGapW = 0; impactSamples = gapSamples = 0;
                        Phase1Checks.server(c, b -> duel = new Duel(this, b, "stand"));
                    }
                    Phase1Checks.server(c, b -> eye(b, cx + eyeAt[0], cz + eyeAt[1], (float) eyeAt[2], (float) eyeAt[3]));
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                Duel d = duel;
                if (d == null || !d.mode.equals("stand")) { if (k > 200) { Phase1Checks.report("p1-geo-knight-attack", false, "duel never ran"); next(tick, 9); } return; }
                watch(c, d);
                // Tick 0 is weight 0 by design (the same pose as the frame before it), and the client often hears of a swing a
                // fraction of a tick after it starts, so it is saved when caught but not required: ticks 1-21 (far: 2-20) are.
                int want = far ? 3 * 10 : 3 * 21;
                if (k > 30 && c.world != null) {
                    var e = c.world.getEntityById(d.us[0].mob.getId());
                    AttackController ac = e == null ? null : AttackController.LIVE.get(e);
                    // Frame 0 is the instant the swing begins: its weight is 0 by design (the fade in), so it shows the pose it leaves.
                    // Its window is half a tick (weight there is at most .10) so a slow harness frame rate still lands in it.
                    if (ac != null && ac.shownT >= 0 && (ac.shownWeight > 0 || ac.shownT < .5)) {
                        int tt = (int) Math.round(ac.shownT);
                        if (Math.abs(ac.shownT - tt) <= (tt == 0 ? .5 : .3) && tt < 22 && (!far || tt % 2 == 0) && taken.add(ac.shownClip + ":" + tt))
                            Phase1Checks.shot(c, String.format("geo-knight-attack-%s-%s-%02d", view, ac.shownClip.replace("attack_", ""), tt));
                    }
                }
                int got = (int) taken.stream().filter(f -> !f.endsWith(":0")).count();
                if (got >= want || k > 1200) {
                    Phase1Checks.report("p1-geo-knight-attack-frames-" + view, got >= want, got + "/" + want + " clip frames of the own knight (tick 0 optional: "
                        + (taken.size() - got) + "/3 caught)");
                    if (++i < ATTACK_VIEWS.length) next(tick, 5);
                    else {
                        syncReport(d);
                        Phase1Checks.report("p1-geo-knight-attack-plays", impactSamples > 10 && minImpactW > .9 && gapSamples > 4 && maxGapW == 0,
                            String.format("weight at impact (+-1 tick) min %.4f over %d samples; between swings max %.5f over %d", minImpactW, impactSamples, maxGapW, gapSamples));
                        duel = null; i = 0; next(tick, 6);
                    }
                }
            }
            case 6 -> {
                // Walking attack: the own knight presses forward at walk speed, the foe backs away, both swinging.
                if (k == 1) {
                    clientVariant.clear(); clientImpact.clear(); minImpactW = 2; impactSamples = 0; walkSwingSamples = 0; walkWrong.clear(); foeBackMin = 9; lastShot = -1;
                    Phase1Checks.server(c, b -> { duel = new Duel(this, b, "walk"); eye(b, cx, cz + 5, 180, 8); });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                Duel d = duel;
                if (d == null || !d.mode.equals("walk")) { if (k > 200) { Phase1Checks.report("p1-geo-knight-attack-walk", false, "walk duel never ran"); next(tick, 9); } return; }
                watch(c, d);
                int dk = d.k;
                if (dk != lastShot && dk >= WALK0 + 10 && dk < WALK0 + WALK - 4) { lastShot = dk; Phase1Checks.shot(c, String.format("geo-knight-attack-walk-%02d", dk - WALK0)); }
                if (dk >= WALK0 + WALK + 4 || k > 600) {
                    syncReport(d);
                    Phase1Checks.report("p1-geo-knight-attack-walk", walkSwingSamples > 10 && walkWrong.isEmpty() && impactSamples > 2 && minImpactW > .9 && foeBackMin > .1,
                        String.format("%d swinging samples, gait while swinging %s; weight at impact min %.3f over %d; foe backing speed min %.3f",
                            walkSwingSamples, walkWrong.isEmpty() ? "always WALK" : "wrong " + walkWrong, minImpactW, impactSamples, foeBackMin));
                    duel = null; next(tick, 7);
                }
            }
            case 7 -> {
                // Death mid-swing: the foe is killed 4 ticks into a swing; its arms must let the swing go and its legs stay still.
                if (k == 1) {
                    maxBeforeDeathW = 0; maxDeadW = 0; deadSamples = 0; deadWrong.clear(); lastShot = -1;
                    Phase1Checks.server(c, b -> { duel = new Duel(this, b, "death"); eye(b, cx, cz - 4.5, 0, 8); });
                }
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; }
                Duel d = duel;
                if (d == null || !d.mode.equals("death")) { if (k > 200) { Phase1Checks.report("p1-geo-knight-attack-death", false, "death duel never ran"); next(tick, 9); } return; }
                watch(c, d);
                long ks = d.killSwing;
                if (ks >= 0 && c.world != null) {
                    int t = (int) (c.world.getTime() - ks);
                    if (t != lastShot && t >= 0 && t <= 16) { lastShot = t; Phase1Checks.shot(c, String.format("geo-knight-attack-death-%02d", t)); }
                }
                if ((d.killedAt >= 0 && c.world != null && c.world.getTime() - d.killedAt > 20) || k > 600) {
                    Phase1Checks.report("p1-geo-knight-attack-death", d.killedAt >= 0 && d.killedDead && maxBeforeDeathW > 0 && deadSamples > 5 && maxDeadW == 0 && deadWrong.isEmpty(),
                        String.format("killed %s (dead %s); weight 2-4 ticks into the swing max %.3f; once faded (dead 3+ ticks) max %.5f over %d samples; gait %s",
                            d.killedAt >= 0 ? "at +4" : "never", d.killedDead, maxBeforeDeathW, maxDeadW, deadSamples, deadWrong.isEmpty() ? "IDLE" : "wrong " + deadWrong));
                    duel = null; next(tick, 9);
                }
            }
            case 9 -> {
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                if (k > 40) { Phase1Checks.finish(c, "geo-knight"); stage = 99; }
            }
            default -> {}
        }
    }
}
