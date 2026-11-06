package gg.gravesandglory;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Unit animation rebuild, step 8 (docs/anim-rebuild/PLAN.md): what the knight rig costs to draw. GRAVES_PHASE1_CHECKS=geo-perf, run once
 * with GRAVES_GEO_TROOPS=true (GeckoLib rig) and once with false (the old rig) and compared. In a practice battle every troop is cleared
 * and 40 tier-III knights are placed, 20 a side in two 5 x 4 blocks four blocks apart, all in view of a camera 16 blocks off and raised.
 * Phase "march": the server walks every knight back and forth at the knight's walk speed, 3 blocks each way (gait clips play, AI off).
 * Phase "melee": AI on, invulnerable, so the two blocks close and trade blows (attack, hit, stance layers play) without dying.
 * Each phase samples the client's frame-to-frame time for 20 s after a 3 s settle; p1-geo-perf-<phase> reports median, p95, p99
 * and mean fps, the rig and the client's vsync/maxFps. The check only fails when a phase draws no frames or loses knights. Never shipped.
 */
final class GeoPerfChecks implements Phase1Checks.Suite {
    static final int N_SIDE = 20, SETTLE = 60, SAMPLE = 400;
    static final double WALK = .169, SWING = 3;
    static final String[] PHASES = {"march", "melee"};

    static volatile GeoPerfChecks marching;
    static volatile List<Double> frames;
    static long lastFrame;
    static {
        WorldRenderEvents.START.register(ctx -> {
            long now = System.nanoTime();
            List<Double> f = frames;
            if (f != null && lastFrame != 0) f.add((now - lastFrame) / 1e6);
            lastFrame = now;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> { GeoPerfChecks m = marching; if (m != null) m.serverMarch(server.getTicks()); });
    }

    final List<GravesAndGlory.Unit> knights = new ArrayList<>();
    final List<double[]> home = new ArrayList<>();
    volatile UUID player;
    volatile boolean battleReady, placed;
    volatile String why = "";
    volatile double cx, cy, cz;
    volatile int alive = -1;
    List<Double> taken;
    int stage, at, phase;

    void next(int tick, int s) { stage = s; at = tick; }

    @Override public void cleanup(MinecraftClient c) {
        marching = null; frames = null;
        c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    /** Clears the field and places 20 knights a side, 12 blocks south of the player. */
    void place(GravesAndGlory.Battle b) {
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) { why = "no player"; return; }
        var spec = Campaign.troop("knight");
        if (spec == null) { why = "no knight troop"; return; }
        for (var u : b.units) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); }
        b.units.clear(); b.clearCorpses();
        GravesAndGlory.Squad mine = null, foe = null;
        var own = b.squads.get(player);
        if (own != null) { mine = own; for (var s : b.squads.values()) if (s.team != own.team) { foe = s; break; } }
        else for (var s : b.squads.values()) { if (s.team == 0 && mine == null) mine = s; else if (s.team == 1 && foe == null) foe = s; }
        if (mine == null || foe == null) { why = "squads " + b.squads.size(); return; }
        p.getServerWorld().setTimeOfDay(6000);
        cx = Math.floor(p.getX()) + .5; cz = Math.floor(p.getZ()) + 12.5;
        cy = Arena.ground((int) cx, (int) cz) + 1;
        int slot = 0;
        for (int side = 0; side < 2; side++) for (int j = 0; j < N_SIDE; j++) {
            int col = j % 5, row = j / 5;
            double x = cx - 6 + col * 3, z = side == 0 ? cz - 2 - row * 1.6 : cz + 2 + row * 1.6;
            var u = b.spawn(side == 0 ? mine : foe, spec, 20 + slot++, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
            u.level = 3; u.weaponTier = 3; u.armourTier = 3;
            u.mob.setAiDisabled(true); u.mob.setInvulnerable(true);
            float yaw = side == 0 ? 0 : 180;
            u.mob.setYaw(yaw); u.mob.setBodyYaw(yaw); u.mob.setHeadYaw(yaw);
            RankSync.changed(b, u);
            knights.add(u); home.add(new double[]{x, z});
        }
        placed = true;
    }

    /** Server thread, every tick of the march phase: each knight walks 3 blocks east and back at walk speed. */
    void serverMarch(int tick) {
        double period = 4 * SWING / WALK, ph = (tick % period) / period;
        double off = ph < .5 ? -SWING + 4 * SWING * ph : 3 * SWING - 4 * SWING * ph;
        float yaw = ph < .5 ? -90 : 90;
        for (int j = 0; j < knights.size(); j++) {
            var m = knights.get(j).mob;
            if (!m.isAlive()) continue;
            double x = home.get(j)[0] + off, z = home.get(j)[1];
            m.setPosition(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z);
            m.setYaw(yaw); m.setBodyYaw(yaw); m.setHeadYaw(yaw);
        }
    }

    void camera(GravesAndGlory.Battle b) {
        var p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        p.setNoGravity(true);
        p.networkHandler.requestTeleport(cx, cy + 5, cz - 16, 0, 18);
    }

    static double pct(List<Double> s, double q) { return s.get(Math.min(s.size() - 1, (int) Math.ceil(s.size() * q) - 1)); }

    void report(MinecraftClient c, String name, List<Double> f) {
        List<Double> s = new ArrayList<>(f); Collections.sort(s);
        double sum = 0; for (double v : s) sum += v;
        int drawn = 0; synchronized (Gait.LIVE) { for (var e : Gait.LIVE.keySet()) if (e != null && e.isAlive()) drawn++; }
        String rig = GeoTroopRenderer.ENABLED ? "geckolib" : "old";
        boolean ok = s.size() > 100 && alive == 2 * N_SIDE;
        String msg = s.isEmpty() ? "no frames" : String.format(Locale.ROOT,
            "rig %s, %d knights alive, %d on the geo gait clock; %d frames over %.1f s: median %.2f ms, p95 %.2f, p99 %.2f, max %.2f, mean fps %.1f; vsync %b, maxFps %d",
            rig, alive, drawn, s.size(), sum / 1000, pct(s, .5), pct(s, .95), pct(s, .99), s.get(s.size() - 1), s.size() * 1000 / sum,
            c.options.getEnableVsync().getValue(), c.options.getMaxFps().getValue());
        Phase1Checks.report("p1-geo-perf-" + name, ok, msg);
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 12000 && stage != 9 && stage < 99) { Phase1Checks.report("p1-geo-perf-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-geo-perf", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 80) Phase1Checks.server(c, this::place);
                if (k > 100) {
                    if (!placed) { Phase1Checks.report("p1-geo-perf-placed", false, "could not place knights: " + why); next(tick, 9); return; }
                    phase = 0; next(tick, 2);
                }
            }
            case 2 -> {
                String name = PHASES[phase];
                if (k == 1) Phase1Checks.server(c, b -> {
                    camera(b);
                    if (name.equals("march")) marching = this;
                    else { marching = null; for (var u : knights) u.mob.setAiDisabled(false); }
                });
                if (k == 3) { c.setScreen(null); c.options.hudHidden = true; c.options.setPerspective(Perspective.FIRST_PERSON); }
                if (k == SETTLE) { lastFrame = 0; frames = Collections.synchronizedList(new ArrayList<>()); }
                if (k == SETTLE + SAMPLE / 2) Phase1Checks.shot(c, "geo-perf-" + name);
                if (k == SETTLE + SAMPLE) {
                    taken = frames; frames = null;
                    Phase1Checks.server(c, b -> { int n = 0; for (var u : knights) if (u.mob.isAlive()) n++; alive = n; });
                }
                if (k == SETTLE + SAMPLE + 2) {
                    report(c, name, taken == null ? List.of() : taken);
                }
                if (k == SETTLE + SAMPLE + 3) { alive = -1; if (++phase < PHASES.length) next(tick, 2); else next(tick, 9); }
            }
            case 9 -> {
                marching = null;
                c.options.setPerspective(Perspective.FIRST_PERSON); c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                if (k > 40) { Phase1Checks.finish(c, "geo-perf"); stage = 99; }
            }
            default -> {}
        }
    }
}
