package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * Prop look (prop workflow, docs/props/WORKFLOW.md step 4). GRAVES_PHASE1_CHECKS=prop-look, GRAVES_PROPLOOK=<id,id,...> (default: every def).
 * In a practice battle the server lays a grass platform above the map, places each prop through the real path ({@link Props#place}: anchor,
 * barriers, lights, display parts, effects) facing north, and stands a frozen Knight beside each. Shots p1-prop-<id>-<near|far>-<day|night>.png
 * from the front-left at about 3 m and 15 m, by day and lantern-lit night, then a lineup. Needs the prop pack enabled as a client resource pack
 * (tools/prop_shot.py does it). Never shipped.
 */
final class PropLookChecks implements Phase1Checks.Suite {
    /** id, size x y z, pivot cell x z. */
    record P(String id, int sx, int sy, int sz, int px, int pz) {}
    static volatile List<P> PROPS = List.of();
    static int GAP = 9;
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

    static List<P> select() {
        String want = System.getenv().getOrDefault("GRAVES_PROPLOOK", "").trim();
        List<String> ids = want.isEmpty() ? new ArrayList<>(new TreeSet<>(Props.DEFS.keySet())) : Arrays.asList(want.split(","));
        List<P> out = new ArrayList<>();
        for (String id : ids) {
            PropDef d = Props.DEFS.get(id.trim());
            if (d == null) { Phase1Checks.report("p1-prop-def-" + id.trim(), false, "no def loaded for " + id.trim()); continue; }
            out.add(new P(d.id(), d.sx(), d.sy(), d.sz(), (d.sx() - 1) / 2, (d.sz() - 1) / 2));
        }
        return out;
    }

    void build(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        PROPS = select();
        GAP = 6 + PROPS.stream().mapToInt(d -> Math.max(d.sx(), d.sz())).max().orElse(3);
        ox = (int) Math.floor(p.getX()) - 20; oz = (int) Math.floor(p.getZ()); oy = Math.min(300, (int) Math.floor(p.getY()) + 45);
        int x1 = ox - 8, x2 = ox + PROPS.size() * GAP + 4, z1 = oz - 26, z2 = oz + 10;
        cmd(b, p, "gamerule doDaylightCycle false"); cmd(b, p, "weather clear 100000"); cmd(b, p, "time set 6000");
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:air", x1, oy, z1, x2, oy + 10, z2));
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:grass_block", x1, oy - 1, z1, x2, oy - 1, z2));
        cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:dirt", x1, oy - 3, z1, x2, oy - 2, z2));
        var w = p.getServerWorld();
        for (int i = 0; i < PROPS.size(); i++) {
            int[] a = anchor(i); String why = Props.place(w, PROPS.get(i).id(), new net.minecraft.util.math.BlockPos(a[0], a[1], a[2]), 0);
            Phase1Checks.report("p1-prop-place-" + PROPS.get(i).id(), why.isEmpty(), why.isEmpty() ? "placed through Props.place, facing north" : why);
            var cell = new net.minecraft.util.math.BlockPos(a[0], a[1], a[2]); var base = Props.anchorBase(w, cell);
            boolean hidden = why.isEmpty() && base != null && base.getY() < a[1] - 1 && w.getBlockState(cell.down()).isOf(net.minecraft.block.Blocks.GRASS_BLOCK);
            Phase1Checks.report("p1-prop-anchor-hidden-" + PROPS.get(i).id(), hidden, "anchor at " + (base == null ? "none" : base.toShortString()) + ", floor " + w.getBlockState(cell.down()).getBlock());
        }
        if (!PROPS.isEmpty()) oldAnchor(w, PROPS.get(0).id(), new net.minecraft.util.math.BlockPos(ox, oy, oz - 20));
        if (!PROPS.isEmpty()) { // no buried cell (air under the floor): the anchor falls back to the floor under the cell
            var cell = new net.minecraft.util.math.BlockPos(ox + 10, oy, oz - 20);
            w.setBlockState(cell.down(2), net.minecraft.block.Blocks.AIR.getDefaultState(), 3); w.setBlockState(cell.down(3), net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            String why = Props.place(w, PROPS.get(0).id(), cell, 0); var base = Props.anchorBase(w, cell);
            boolean ok = why.isEmpty() && cell.down().equals(base) && Props.remove(w, cell) && w.getBlockState(cell.down()).isOf(net.minecraft.block.Blocks.GRASS_BLOCK);
            Phase1Checks.report("p1-prop-anchor-fallback", ok, why.isEmpty() ? "anchor at " + (base == null ? "none" : base.toShortString()) + ", floor after remove " + w.getBlockState(cell.down()).getBlock() : why);
        }
        var own = b.squads.get(p.getUuid());
        for (int i = 0; i < PROPS.size(); i++) {
            var d = PROPS.get(i); int[] a = anchor(i);
            int minX = a[0] - d.px(), minZ = a[2] - d.pz();
            // torch light: lanterns on fence posts beside the front corners
            cmd(b, p, String.format("setblock %d %d %d minecraft:oak_fence", minX - 1, oy, minZ + d.sz()));
            cmd(b, p, String.format("setblock %d %d %d minecraft:lantern", minX - 1, oy + 1, minZ + d.sz()));
            cmd(b, p, String.format("setblock %d %d %d minecraft:oak_fence", minX + d.sx(), oy, minZ + d.sz()));
            cmd(b, p, String.format("setblock %d %d %d minecraft:lantern", minX + d.sx(), oy + 1, minZ + d.sz()));
            if (own != null) {
                var u = b.spawn(own, Campaign.troop("knight"), 60 + i, new Vec3d(minX - 1.5 + 0.0, oy, minZ + 0.5));
                Phase1Checks.freeze(u); u.mob.setAiDisabled(true); u.mob.setNoGravity(true);
                u.mob.refreshPositionAndAngles(minX - 1.3, oy, minZ + 0.6, 180f, 0f); u.mob.setHeadYaw(180f); u.mob.setBodyYaw(180f);
            }
        }
        built = true; say("platform at " + ox + " " + oy + " " + oz + (own == null ? " (no squad: no knights)" : ""));
    }

    /** An anchor in the old format (one block under its cell, no depth=) is found by the scanner, moved down out of sight, and removed with
     *  its floor and the block under it put back. */
    static void oldAnchor(net.minecraft.server.world.ServerWorld w, String id, net.minecraft.util.math.BlockPos cell) {
        var under = cell.down(); var deep = cell.down(2);
        w.setBlockState(under, net.minecraft.block.Blocks.COMMAND_BLOCK.getDefaultState().with(net.minecraft.block.CommandBlock.FACING, net.minecraft.util.math.Direction.NORTH), 3);
        if (w.getBlockEntity(under) instanceof net.minecraft.block.entity.CommandBlockBlockEntity cb) { cb.getCommandExecutor().setCommand(Props.ANCHOR_PREFIX + id + " floor=minecraft:grass_block"); cb.setAuto(false); cb.markDirty(); }
        int found = Props.scanBox(w, new net.minecraft.util.math.BlockBox(cell.getX() - 2, cell.getY() - 4, cell.getZ() - 2, cell.getX() + 2, cell.getY() + 2, cell.getZ() + 2));
        var l = Props.readAnchor(w, cell);
        boolean moved = found == 1 && l != null && w.getBlockState(under).isOf(net.minecraft.block.Blocks.GRASS_BLOCK) && w.getBlockState(deep).isOf(net.minecraft.block.Blocks.COMMAND_BLOCK) && !Props.parts(w, cell).isEmpty();
        Phase1Checks.report("p1-prop-old-anchor-scan", moved, "found " + found + ", under " + w.getBlockState(under).getBlock() + ", deep " + w.getBlockState(deep).getBlock() + ", parts " + Props.parts(w, cell).size());
        boolean removed = Props.remove(w, cell) && Props.readAnchor(w, cell) == null && Props.parts(w, cell).isEmpty()
            && w.getBlockState(under).isOf(net.minecraft.block.Blocks.GRASS_BLOCK) && w.getBlockState(deep).isOf(net.minecraft.block.Blocks.DIRT);
        Phase1Checks.report("p1-prop-old-anchor-remove", removed, "under " + w.getBlockState(under).getBlock() + ", deep " + w.getBlockState(deep).getBlock());
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
                    var id = c.player.getUuid(); double x = ox + (PROPS.size() - 1) * GAP / 2.0, y = oy + 6, z = oz - Math.min(22, 9 + PROPS.size() * GAP * 0.35);
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
