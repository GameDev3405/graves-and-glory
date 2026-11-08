package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Prop kit art look (plan 2026-11-08-prop-kit, Task A5). GRAVES_PHASE1_CHECKS=prop-look. In a practice battle the server lays a grass
 * platform above the map, summons each prop's part models as item_display entities (paper + CustomModelData from the art preview pack,
 * "fixed" transform, turned 180 so a model's north front faces north, model px 0,0,0 on the anchor cell's min corner) and stands a frozen
 * Knight beside each. Shots p1-prop-<id>-<near|far>-<day|night>.png from the front-left at about 3 m and 15 m, by day and torch-lit.
 * Needs the art preview pack enabled (work/props/art/). Never shipped.
 */
final class PropLookChecks implements Phase1Checks.Suite {
    /** id, size x y z, pivot cell x z, part CMDs, glow mask (bit per part). CMD = 7310001 + 10*propIndex + partIndex (preview pack). */
    record P(String id, int sx, int sy, int sz, int px, int pz, int[] cmd, int glow) {}
    static final List<P> PROPS = List.of(
        new P("forge", 3, 4, 2, 1, 0, new int[]{7310001, 7310002, 7310003}, 0b100),
        new P("anvil", 1, 1, 1, 0, 0, new int[]{7310011}, 0),
        new P("weapon_rack", 2, 2, 1, 0, 0, new int[]{7310021}, 0),
        new P("shield_rack", 2, 2, 1, 0, 0, new int[]{7310031}, 0),
        new P("quench_trough", 2, 1, 1, 0, 0, new int[]{7310041}, 0),
        new P("grindstone_bench", 2, 2, 1, 0, 0, new int[]{7310051}, 0));
    static final int GAP = 9;
    volatile UUID player;
    volatile boolean battleReady, built;
    volatile int ox, oy, oz;
    int stage, at, pi, vi;

    static void say(String line) { System.out.println("[p1] prop-look: " + line); }
    void next(int tick, int s) { stage = s; at = tick; }

    static void cmd(GravesAndGlory.Battle b, ServerPlayerEntity p, String line) {
        var src = b.server.getCommandSource().withWorld(p.getServerWorld()).withSilent().withLevel(4);
        b.server.getCommandManager().executeWithPrefix(src, line);
    }

    /** Anchor cell of prop i (its pivot cell). */
    int[] anchor(int i) { return new int[]{ox + i * GAP, oy, oz}; }

    String summons() {
        var sb = new StringBuilder();
        for (int i = 0; i < PROPS.size(); i++) {
            var d = PROPS.get(i); int[] a = anchor(i);
            for (int k = 0; k < d.cmd().length; k++) {
                boolean glow = (d.glow() >> k & 1) != 0;
                sb.append(String.format(Locale.ROOT,
                    "summon minecraft:item_display %.1f %.1f %.1f {Tags:[\"gg_proplook\"],item_display:\"fixed\",Rotation:[180f,0f],%sitem:{id:\"minecraft:paper\",Count:1b,tag:{CustomModelData:%d}}}\n",
                    a[0] + 0.5, a[1] + 0.5, a[2] + 0.5, glow ? "brightness:{block:15,sky:15}," : "", d.cmd()[k]));
            }
        }
        return sb.toString();
    }

    void build(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        ox = (int) Math.floor(p.getX()) - 20; oz = (int) Math.floor(p.getZ()); oy = Math.min(300, (int) Math.floor(p.getY()) + 45);
        int x1 = ox - 8, x2 = ox + PROPS.size() * GAP + 4, z1 = oz - 24, z2 = oz + 8;
        cmd(b, p, "gamerule doDaylightCycle false"); cmd(b, p, "weather clear 100000"); cmd(b, p, "time set 6000");
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:air", x1, oy, z1, x2, oy + 10, z2));
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x1, oy - 1, z1, x2, oy - 1, z2));
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:dirt", x1, oy - 3, z1, x2, oy - 2, z2));
        cmd(b, p, "kill @e[tag=gg_proplook]");
        String all = summons();
        for (String line : all.split("\n")) cmd(b, p, line);
        try { var dir = java.nio.file.Path.of(System.getProperty("user.dir"), "proplook"); java.nio.file.Files.createDirectories(dir);
              java.nio.file.Files.writeString(dir.resolve("summon.mcfunction"), "# anchor cells: " + ox + " " + oy + " " + oz + " every " + GAP + " x\n" + all); }
        catch (Exception e) { say("summon file: " + e); }
        var own = b.squads.get(p.getUuid());
        for (int i = 0; i < PROPS.size(); i++) {
            var d = PROPS.get(i); int[] a = anchor(i);
            int minX = a[0] - d.px(), minZ = a[2] - d.pz();
            // torch light: lanterns on fence posts beside the front corners
            cmd(b, p, String.format("setblock %d %d %d minecraft:oak_fence", minX - 1, oy, minZ + d.sz()));
            cmd(b, p, String.format("setblock %d %d %d minecraft:lantern", minX - 1, oy + 1, minZ + d.sz()));
            cmd(b, p, String.format("setblock %d %d %d minecraft:oak_fence", minX + d.sx(), oy, minZ + d.sz()));
            cmd(b, p, String.format("setblock %d %d %d minecraft:lantern", minX + d.sx(), oy + 1, minZ + d.sz()));
            if (d.id().equals("forge")) for (int lx : new int[]{0, 2})
                cmd(b, p, String.format("setblock %d %d %d minecraft:light[level=12]", minX + lx, oy + 2, minZ));
            if (own != null) {
                var u = b.spawn(own, Campaign.troop("knight"), 60 + i, new Vec3d(minX - 1.5 + 0.0, oy, minZ + 0.5));
                Phase1Checks.freeze(u); u.mob.setAiDisabled(true); u.mob.setNoGravity(true);
                u.mob.refreshPositionAndAngles(minX - 1.0, oy, minZ + 0.6, 180f, 0f); u.mob.setHeadYaw(180f); u.mob.setBodyYaw(180f);
            }
        }
        built = true; say("platform at " + ox + " " + oy + " " + oz + (own == null ? " (no squad: no knights)" : ""));
    }

    /** Camera at the front-left (north-west) of prop i at distance d, eye height, looking at its middle. */
    void camera(MinecraftClient c, int i, double dist) {
        var d = PROPS.get(i); int[] a = anchor(i);
        double cx = a[0] - d.px() + d.sx() / 2.0, cy = oy + Math.min(d.sy(), 2) / 2.0 + 0.3, cz = a[2] - d.pz() + d.sz() / 2.0;
        if (dist < 8) dist += Math.max(d.sx(), d.sz()) * 0.35 + (d.sy() > 2 ? 1.2 : 0) - 0.6;
        double x = cx + dist * 0.5, z = cz - dist * 0.87, y = oy + 1.62 + (dist > 8 ? 2.5 : 0.2);
        double dx = cx - x, dy = cy - y, dz = cz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        var id = c.player.getUuid();
        c.getServer().execute(() -> { var p = c.getServer().getPlayerManager().getPlayer(id);
            if (p != null) { p.changeGameMode(GameMode.SPECTATOR); p.teleport(p.getServerWorld(), x, y, z, yaw, pitch); } });
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.hudHidden = false;
        if (c.getServer() != null) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
    }

    static final String[] VIEWS = {"near-day", "far-day", "near-night", "far-night"};

    public void step(MinecraftClient c, int tick) {
        if (tick > 30000 && stage < 9) { Phase1Checks.report("p1-prop-look-timeout", false, "stuck at stage " + stage); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (!battleReady) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (!battleReady && p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) battleReady = true; });
                    if (k > 600) { Phase1Checks.report("p1-prop-look", false, "the practice battle never started"); next(tick, 9); }
                    return;
                }
                if (k == 100) Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (p != null) build(b, p); });
                if (k > 140 && built) { say("packs " + c.getResourcePackManager().getEnabledNames()); vi = 0; pi = 0; next(tick, 2); }
            }
            case 2 -> {
                String v = VIEWS[vi];
                if (k == 1) {
                    if (pi == 0) Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player);
                        if (p != null) cmd(b, p, v.endsWith("night") ? "time set 18000" : "time set 6000"); });
                    camera(c, pi, v.startsWith("near") ? 3.2 : 15.0);
                }
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 60) Phase1Checks.shot(c, "prop-" + PROPS.get(pi).id() + "-" + v);
                if (k == 62) {
                    if (++pi < PROPS.size()) next(tick, 2);
                    else if (++vi < VIEWS.length) { pi = 0; next(tick, 2); }
                    else { Phase1Checks.report("p1-prop-look", true, PROPS.size() * VIEWS.length + " shots"); next(tick, 3); }
                }
            }
            case 3 -> { // a lineup of the whole set, day
                if (k == 1) { Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (p != null) cmd(b, p, "time set 6000"); });
                    var id = c.player.getUuid(); double x = ox + 2.5 * GAP, y = oy + 7, z = oz - 17;
                    c.getServer().execute(() -> { var p = c.getServer().getPlayerManager().getPlayer(id); if (p != null) p.teleport(p.getServerWorld(), x, y, z, 0f, 18f); }); }
                if (k == 60) Phase1Checks.shot(c, "prop-lineup");
                if (k == 62) next(tick, 9);
            }
            case 9 -> {
                c.options.hudHidden = false;
                if (k == 1) Phase1Checks.server(c, b -> { if (b.running) b.campaign.returnLobby(); });
                if (k > 40) { Phase1Checks.finish(c, "prop-look"); stage = 99; }
            }
            default -> {}
        }
    }
}
