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
 * (tools/prop_shot.py does it). Compare: GRAVES_PROPCMP=<id>=<group/name>;... places that structure template (data/gravesandglory/structures/
 * group/name.nbt, the maps' plain-block version, through {@link Structures#place}) 2 blocks east of the prop and takes the same four views of
 * the pair, p1-prop-<id>-cmp-<near|far>-<day|night>.png; an id without a template is skipped with a line saying so. GRAVES_PROPFRAMES=1 adds
 * p1-prop-<id>-near-day-t3/t6/t9.png, the near-day view 3, 6 and 9 ticks later (animated flame and lamp textures). GRAVES_PROPTURN=1 replaces the
 * four views with a turntable (no Knight, no fence lanterns, so the prop's own light is all there is at night): p1-prop-<id>-turn-000..315 (8 angles, day,
 * the camera orbits the prop, 000 = from the front/north, 090 = from the east), -close-day, -close-night, -turn-night-000/090/180/270.
 * GRAVES_PROPFOCUS=<id>=H,dist,fx,fy,fz,cdist;... gives per prop the framed height (blocks), the orbit distance, the close-up target (blocks from the
 * prop's min corner) and the close-up distance. Never shipped.
 */
final class PropLookChecks implements Phase1Checks.Suite {
    /** id, size x y z, pivot cell x z. */
    record P(String id, int sx, int sy, int sz, int px, int pz) {}
    static volatile List<P> PROPS = List.of();
    /** A compare pair: prop index, template id, template size; filled by build(). */
    record Cmp(int i, String template, int tx, int ty, int tz) {}
    static volatile List<Cmp> CMPS = List.of();
    static int GAP = 9;
    volatile UUID player;
    volatile boolean battleReady, built;
    volatile int ox, oy, oz;
    int stage, at, pi, vi, ci;

    static void say(String line) { System.out.println("[p1] prop-look: " + line); }
    void next(int tick, int s) { stage = s; at = tick; }

    static int cmd(GravesAndGlory.Battle b, ServerPlayerEntity p, String line) {
        var src = b.server.getCommandSource().withWorld(p.getServerWorld()).withSilent().withLevel(4);
        return b.server.getCommandManager().executeWithPrefix(src, line);
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

    /** GRAVES_PROPCMP: id=template;id=template. Ids not being shot and templates that do not load are skipped, each with a line. */
    static List<Cmp> compares(net.minecraft.server.world.ServerWorld w) {
        List<Cmp> out = new ArrayList<>();
        for (String pair : System.getenv().getOrDefault("GRAVES_PROPCMP", "").split(";")) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length != 2 || kv[0].isBlank()) continue;
            String id = kv[0].trim(), tid = kv[1].trim(); int i = -1;
            for (int k = 0; k < PROPS.size(); k++) if (PROPS.get(k).id().equals(id)) i = k;
            if (i < 0) { say("compare " + id + ": not one of the props shot, skipped"); continue; }
            var tpl = Structures.template(w, tid);
            if (tpl.isEmpty()) { say("compare " + id + ": no structure template " + tid + " (data/gravesandglory/structures/" + tid + ".nbt), skipped");
                Phase1Checks.report("p1-prop-cmp-" + id, true, "skipped: no structure template " + tid); continue; }
            var s = tpl.get().getSize(); out.add(new Cmp(i, tid, s.getX(), s.getY(), s.getZ()));
        }
        return out;
    }
    Cmp cmpOf(int i) { for (Cmp c : CMPS) if (c.i() == i) return c; return null; }

    void build(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        PROPS = select();
        var w = p.getServerWorld();
        CMPS = compares(w);
        GAP = 6 + PROPS.stream().mapToInt(d -> Math.max(d.sx(), d.sz())).max().orElse(3);
        for (Cmp c : CMPS) GAP = Math.max(GAP, 6 + PROPS.get(c.i()).sx() + 2 + c.tx());
        int deep = 0, high = 10;
        for (Cmp c : CMPS) { deep = Math.max(deep, c.tz()); high = Math.max(high, c.ty() + 2); }
        ox = (int) Math.floor(p.getX()) - 20; oz = (int) Math.floor(p.getZ()); oy = Math.min(300, (int) Math.floor(p.getY()) + 45);
        int x1 = ox - 8, x2 = ox + PROPS.size() * GAP + 4, z1 = oz - 26, z2 = oz + Math.max(10, deep + 2);
        cmd(b, p, "gamerule doDaylightCycle false"); cmd(b, p, "weather clear 100000"); cmd(b, p, "time set 6000");
        scanTests(b, p, w);
        // /fill stops at commandModificationBlockLimit (32768 blocks) and then changes nothing: a platform for 30 props is over 40000 blocks of
        // dirt alone, so without this the dirt under the grass is missing, no cell is buried, and every anchor falls back to the floor (it shows).
        cmd(b, p, "gamerule commandModificationBlockLimit 2000000");
        // A long lineup (40+ props with compare fixtures) runs past the loaded chunks, and /fill refuses unloaded ground and changes
        // nothing; force-load the platform and fill it in strips (forceload takes at most 256 chunks per command).
        int grass = 0, dirt = 0;
        for (int sx = x1; sx <= x2; sx += 128) {
            int ex = Math.min(x2, sx + 127);
            cmd(b, p, String.format("forceload add %d %d %d %d", sx, z1, ex, z2));
            cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:air", sx, oy, z1, ex, oy + high, z2));
            grass += cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:grass_block", sx, oy - 1, z1, ex, oy - 1, z2));
            dirt += cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:dirt", sx, oy - 3, z1, ex, oy - 2, z2));
        }
        Phase1Checks.report("p1-prop-stage-platform", grass > 0 && dirt > 0, "fill grass " + grass + " dirt " + dirt + " blocks, platform " + (x2 - x1 + 1) + "x" + (z2 - z1 + 1));
        for (int i = 0; i < PROPS.size(); i++) {
            int[] a = anchor(i); String why = Props.place(w, PROPS.get(i).id(), new net.minecraft.util.math.BlockPos(a[0], a[1], a[2]), 0);
            Phase1Checks.report("p1-prop-place-" + PROPS.get(i).id(), why.isEmpty(), why.isEmpty() ? "placed through Props.place, facing north" : why);
            var cell = new net.minecraft.util.math.BlockPos(a[0], a[1], a[2]); var base = Props.anchorBase(w, cell);
            boolean hidden = why.isEmpty() && base != null && base.getY() < a[1] - 1 && w.getBlockState(cell.down()).isOf(net.minecraft.block.Blocks.GRASS_BLOCK);
            Phase1Checks.report("p1-prop-anchor-hidden-" + PROPS.get(i).id(), hidden, "anchor at " + (base == null ? "none" : base.toShortString()) + ", floor " + w.getBlockState(cell.down()).getBlock());
        }
        for (Cmp c : CMPS) { // the template 2 blocks east of the prop, its north edge on the prop's
            var d = PROPS.get(c.i()); int[] a = anchor(c.i());
            int tx0 = a[0] - d.px() + d.sx() + 2, tz0 = a[2] - d.pz();
            var placed = Structures.place(w, c.template(), tx0 + (c.tx() - 1) / 2, oy, tz0 + (c.tz() - 1) / 2, 0);
            Phase1Checks.report("p1-prop-cmp-place-" + d.id(), !placed.missing(), c.template() + " " + c.tx() + "x" + c.ty() + "x" + c.tz() + " beside " + d.id());
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
            if (TURN) continue;   // turntable: nothing but the prop (no fence lanterns, no Knight)
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

    /** The example template (docs/props/examples/prop_facings.nbt, GRAVES_PROPEXAMPLE or found upwards from the working directory). */
    static java.nio.file.Path exampleFile() {
        String env = System.getenv("GRAVES_PROPEXAMPLE");
        if (env != null && !env.isBlank()) return java.nio.file.Path.of(env);
        for (var d = java.nio.file.Path.of("").toAbsolutePath(); d != null; d = d.getParent()) {
            var f = d.resolve("docs/props/examples/prop_facings.nbt");
            if (java.nio.file.Files.isRegularFile(f)) return f;
        }
        return null;
    }

    /** Anchors written without barriers (a map generator writes only the anchor; docs/props/ANCHOR-NBT.md): the example template, one supply wagon
     *  at each facing with nothing but its anchor, is placed through {@link Structures#place} and every wagon must have its barriers and parts;
     *  then a wagon's barriers and parts are taken away, one barrier cell is replaced with stone, and a scan must put back all but that cell. */
    void scanTests(GravesAndGlory.Battle b, ServerPlayerEntity p, net.minecraft.server.world.ServerWorld w) {
        var def = Props.DEFS.get("supply_wagon");
        var file = exampleFile();
        if (def == null || file == null) { Phase1Checks.report("p1-prop-example-template", false, def == null ? "no supply_wagon def" : "docs/props/examples/prop_facings.nbt not found"); return; }
        try {
            net.minecraft.nbt.NbtCompound nbt;
            try (var in = java.nio.file.Files.newInputStream(file)) { nbt = net.minecraft.nbt.NbtIo.readCompressed(in); }
            var tpl = w.getStructureTemplateManager().createTemplate(nbt);
            String id = "propcheck/prop_facings";
            Structures.CACHE.put(id, new Structures.Loaded(tpl, nbt.getList("blocks", net.minecraft.nbt.NbtElement.COMPOUND_TYPE).size()));
            var size = tpl.getSize(); int floor = 4, x0 = ox - 4, z0 = oz - 24, y0 = oy - floor;
            cmd(b, p, "gamerule commandModificationBlockLimit 2000000");
            var box = new net.minecraft.util.math.BlockBox(x0, y0, z0, x0 + size.getX() - 1, y0 + size.getY() - 1, z0 + size.getZ() - 1);
            cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:air", box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ()));
            var placed = Structures.place(w, id, x0 + (size.getX() - 1) / 2, y0, z0 + (size.getZ() - 1) / 2, 0);
            var cells = new ArrayList<net.minecraft.util.math.BlockPos>(); int[] facings = {0, 90, 180, 270}; var bad = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                var cell = new net.minecraft.util.math.BlockPos(x0 + i * 8 + 4, oy, z0 + 4); cells.add(cell);
                var live = Props.readAnchor(w, cell);
                if (live == null || live.facing() != facings[i]) { bad.append(" wagon ").append(facings[i]).append(": anchor ").append(live == null ? "missing" : "facing " + live.facing()); continue; }
                for (var c : Props.solidCells(def, cell, facings[i])) if (!w.getBlockState(c).isOf(net.minecraft.block.Blocks.BARRIER)) bad.append(" wagon ").append(facings[i]).append(": no barrier at ").append(c.toShortString());
                var parts = Props.parts(w, cell);
                if (parts.size() != def.parts().size()) bad.append(" wagon ").append(facings[i]).append(": ").append(parts.size()).append(" parts");
                else for (var part : def.parts()) {
                    double[] t = PropGeometry.partTransform(def, facings[i], part); boolean found = false;
                    for (var e : parts) if (Props.partCmd(e) == part.cmd() && Math.abs(e.getX() - (cell.getX() + t[0])) < 1e-3 && Math.abs(e.getY() - (cell.getY() + t[1])) < 1e-3
                        && Math.abs(e.getZ() - (cell.getZ() + t[2])) < 1e-3 && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(e.getYaw() - (float) t[3])) < 1) found = true;
                    if (!found) bad.append(" wagon ").append(facings[i]).append(": part ").append(part.model()).append(" not at its transform");
                }
            }
            int barriers = 0;
            for (var c : net.minecraft.util.math.BlockPos.iterate(box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ())) if (w.getBlockState(c).isOf(net.minecraft.block.Blocks.BARRIER)) barriers++;
            if (barriers != 4 * def.solid().size()) bad.append(" ").append(barriers).append(" barriers, wanted ").append(4 * def.solid().size());
            Phase1Checks.report("p1-prop-example-template", !placed.missing() && bad.length() == 0, "4 wagons from anchors only, " + barriers + " barriers" + bad);

            // a scan restores into air only: wagon 0 loses its barriers and parts, one cell becomes stone
            var cell = cells.get(0); var solid = Props.solidCells(def, cell, 0);
            for (var c : solid) w.setBlockState(c, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            var blocked = solid.get(solid.size() / 2); w.setBlockState(blocked, net.minecraft.block.Blocks.STONE.getDefaultState(), 3);
            for (var e : Props.parts(w, cell)) e.discard();
            int found = Props.scanBox(w, box);
            int back = 0; for (var c : solid) if (w.getBlockState(c).isOf(net.minecraft.block.Blocks.BARRIER)) back++;
            boolean ok = found == 4 && back == solid.size() - 1 && w.getBlockState(blocked).isOf(net.minecraft.block.Blocks.STONE) && Props.parts(w, cell).size() == def.parts().size();
            Phase1Checks.report("p1-prop-scan-restore", ok, "found " + found + ", barriers back " + back + "/" + solid.size() + ", stone cell " + w.getBlockState(blocked).getBlock() + ", parts " + Props.parts(w, cell).size());
            for (var c : cells) Props.remove(w, c);
            Structures.CACHE.remove(id);
            cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:air", box.getMinX(), box.getMinY(), box.getMinZ(), box.getMaxX(), box.getMaxY(), box.getMaxZ()));
        } catch (Exception e) {
            Phase1Checks.report("p1-prop-example-template", false, "threw " + e);
        }
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
        aim(c, a[0] - d.px() + d.sx() / 2.0, a[2] - d.pz() + d.sz() / 2.0, Math.max(d.sx(), d.sz()), d.sy(), dist);
    }
    /** The same camera on prop i and its compare template together. */
    void cameraPair(MinecraftClient c, Cmp k, double dist) {
        var d = PROPS.get(k.i()); int[] a = anchor(k.i());
        int w = d.sx() + 2 + k.tx(), deep = Math.max(d.sz(), k.tz());
        aim(c, a[0] - d.px() + w / 2.0, a[2] - d.pz() + deep / 2.0, Math.max(w, deep), Math.max(d.sy(), k.ty()), dist + (dist < 8 ? 0 : w * 0.4));
    }
    void aim(MinecraftClient c, double cx, double cz, int wide, int sy, double dist) {
        // Aim at the prop's middle (up to 2 blocks), from far enough back that its whole height fits the 70-degree view.
        double cy = oy + Math.min(sy, 4) / 2.0;
        if (dist < 8) dist = Math.max(dist + wide * 0.35 + (sy > 2 ? 1.2 : 0) - 0.6, 1.1 * sy + 1.0);
        // teleport sets the feet; the eyes are 1.62 higher: near shots look from eye height 1.82, far ones from 5.74 as before
        double x = cx + dist * 0.5, z = cz - dist * 0.87, y = oy + (dist > 8 ? 1.62 + 2.5 : 0.2);
        double dx = cx - x, dy = cy - (y + 1.62), dz = cz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        var id = c.player.getUuid();
        c.getServer().execute(() -> { var p = c.getServer().getPlayerManager().getPlayer(id);
            if (p != null) { p.changeGameMode(GameMode.SPECTATOR); p.teleport(p.getServerWorld(), x, y, z, yaw, pitch); } });
    }

    /** Turntable camera: orbits prop i's middle at angle deg (0 = north of it, 90 = east) from a little above eye height, or, close, circles the focus point. */
    void orbit(MinecraftClient c, int i, int deg, boolean close) {
        var d = PROPS.get(i); int[] a = anchor(i); double[] f = focus(d);
        double minX = a[0] - d.px(), minZ = a[2] - d.pz(), tx, ty, tz, r, eye;
        if (close) { tx = minX + f[2]; ty = oy + f[3]; tz = minZ + f[4]; r = f[5]; eye = ty + 0.3; }
        else { tx = minX + d.sx() / 2.0; tz = minZ + d.sz() / 2.0; ty = oy + f[0] / 2; r = f[1]; eye = oy + Math.min(2.12, f[0] * 0.5 + 0.7); }
        double rad = Math.toRadians(deg), x = tx + r * Math.sin(rad), z = tz - r * Math.cos(rad), y = eye - 1.62;
        double dx = tx - x, dy = ty - eye, dz = tz - z;
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
    /** GRAVES_PROPFRAMES=1: three extra near-day shots 3, 6 and 9 ticks after the first (p1-prop-<id>-near-day-t<n>.png), to see animated textures move. */
    static final boolean FRAMES = "1".equals(System.getenv("GRAVES_PROPFRAMES"));
    /** GRAVES_PROPTURN=1: the turntable review mode (see the class comment). */
    static final boolean TURN = "1".equals(System.getenv("GRAVES_PROPTURN"));
    /** One turntable shot: file suffix, night or day, orbit angle in degrees (0 = camera north of the prop, 90 = east), close-up of the focus point. */
    record Shot(String name, boolean night, int angle, boolean close) {}
    static final List<Shot> TURNS = turns();
    static List<Shot> turns() {
        List<Shot> l = new ArrayList<>();
        for (int a = 0; a < 360; a += 45) l.add(new Shot(String.format("turn-%03d", a), false, a, false));
        l.add(new Shot("close-day", false, 20, true)); l.add(new Shot("close-night", true, 20, true));
        for (int a = 0; a < 360; a += 90) l.add(new Shot(String.format("turn-night-%03d", a), true, a, false));
        return l;
    }
    /** GRAVES_PROPFOCUS: id=H,dist,fx,fy,fz,cdist;id=... */
    static double[] focus(P d) {
        for (String pair : System.getenv().getOrDefault("GRAVES_PROPFOCUS", "").split(";")) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2 && kv[0].trim().equals(d.id())) { String[] n = kv[1].split(","); double[] v = new double[6];
                for (int k = 0; k < 6 && k < n.length; k++) v[k] = Double.parseDouble(n[k].trim()); return v; }
        }
        double h = d.sy();
        return new double[]{h, Math.max(2.0, 0.9 * h + 0.6), d.sx() / 2.0, h * 0.75, d.sz() / 2.0, 1.3};
    }

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
                if (k > 140 && built) { say("packs " + c.getResourcePackManager().getEnabledNames()); vi = 0; pi = 0; next(tick, TURN ? 5 : 2); }
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
                boolean frames = FRAMES && v.equals("near-day");   // animated-texture check: the same view a few ticks later
                if (frames && (k == 63 || k == 66 || k == 69)) Phase1Checks.shot(c, "prop-" + PROPS.get(pi).id() + "-" + v + "-t" + (k - 60));
                if (k == (frames ? 71 : 62)) {
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
                if (k == 62) { ci = 0; vi = 0; next(tick, CMPS.isEmpty() ? 9 : 4); }
            }
            case 4 -> { // compare pairs: the prop beside its map template, the same four views
                String v = VIEWS[vi]; Cmp pair = CMPS.get(ci);
                if (k == 1) {
                    Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player);
                        if (p != null) cmd(b, p, v.endsWith("night") ? "time set 18000" : "time set 6000"); });
                    cameraPair(c, pair, v.startsWith("near") ? 3.2 : 15.0);
                }
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 60) Phase1Checks.shot(c, "prop-" + PROPS.get(pair.i()).id() + "-cmp-" + v);
                if (k == 62) {
                    if (++vi < VIEWS.length) next(tick, 4);
                    else if (++ci < CMPS.size()) { vi = 0; next(tick, 4); }
                    else { Phase1Checks.report("p1-prop-cmp", true, CMPS.size() * VIEWS.length + " compare shots"); next(tick, 9); }
                }
            }
            case 5 -> { // turntable: shot vi of TURNS for every prop (time of day set once per shot, on the first prop)
                Shot t = TURNS.get(vi);
                if (k == 1) {
                    if (pi == 0) Phase1Checks.server(c, b -> { var p = b.server.getPlayerManager().getPlayer(player); if (p != null) cmd(b, p, t.night() ? "time set 18000" : "time set 6000"); });
                    orbit(c, pi, t.angle(), t.close());
                }
                if (k == 2) { c.setScreen(null); c.options.hudHidden = true; }
                if (k == 60) Phase1Checks.shot(c, "prop-" + PROPS.get(pi).id() + "-" + t.name());
                if (k == 62) {
                    if (++pi < PROPS.size()) next(tick, 5);
                    else if (++vi < TURNS.size()) { pi = 0; next(tick, 5); }
                    else { Phase1Checks.report("p1-prop-look", true, PROPS.size() * TURNS.size() + " turntable shots"); next(tick, 9); }
                }
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
