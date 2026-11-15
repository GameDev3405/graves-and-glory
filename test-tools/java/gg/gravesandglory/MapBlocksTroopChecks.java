package gg.gravesandglory;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.pathing.AmphibiousPathNodeMaker;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.ChunkCache;
import java.util.UUID;

/**
 * The troop side of the two map blocks (docs/props/MAP-BLOCKS.md). GRAVES_PHASE1_CHECKS=map-blocks-troops. On a platform high above the camp:
 * a troop's path node maker calls troop_fence and still_water (and the cell above each) BLOCKED where a vanilla zombie's does not, a troop's real path to a point behind a full-width
 * fence wall does not enter it where a vanilla zombie's goes through, a troop's path across a still_water pool keeps off the pool, the sight helper sees through a fence and a pool
 * but not a stone wall (vanilla canSee is stopped by the fence), a visual line passes through the fence, and the landing, floor and standable tests refuse a cell in or on either block. Never shipped.
 */
final class MapBlocksTroopChecks implements Phase1Checks.Suite {
    volatile UUID player;
    volatile boolean built;
    volatile GameMode before;
    volatile boolean cycleBefore; volatile long timeBefore;
    volatile int ox, oy, oz;
    int stage, at;
    // layout, x offsets: a full-width fence wall (2 high), a still_water pool after it, a full-width stone wall (3 high)
    static final int FENCE = 8, POOL_X = 12, POOL_W = 5, POOL_Z = 8, POOL_D = 9, WALL = 22, SPAN = 24;

    void next(int tick, int s) { stage = s; at = tick; }
    static void report(String id, boolean pass, String detail) { Phase1Checks.report("p1-mapblocks-troops-" + id, pass, detail); }
    static void cmd(GravesAndGlory.Battle b, ServerPlayerEntity p, String line) {
        var src = b.server.getCommandSource().withWorld(p.getServerWorld()).withSilent().withLevel(4);
        b.server.getCommandManager().executeWithPrefix(src, line);
    }
    ServerPlayerEntity me(GravesAndGlory.Battle b) { return b.server.getPlayerManager().getPlayer(player); }
    void fill(GravesAndGlory.Battle b, ServerPlayerEntity p, String block, int x1, int y1, int z1, int x2, int y2, int z2) { cmd(b, p, String.format("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block)); }

    void build(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        before = p.interactionManager.getGameMode();
        cycleBefore = b.server.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE); timeBefore = p.getServerWorld().getTimeOfDay();
        ox = (int) Math.floor(p.getX()) - 4; oz = (int) Math.floor(p.getZ()); oy = Math.min(290, (int) Math.floor(p.getY()) + 45);
        cmd(b, p, "gamerule doDaylightCycle false"); cmd(b, p, "time set 6000");
        fill(b, p, "minecraft:air", ox - 3, oy, oz - 3, ox + 30, oy + 10, oz + 30);
        fill(b, p, "minecraft:stone", ox - 3, oy - 3, oz - 3, ox + 30, oy - 1, oz + 30);
        String fence = MapBlocks.TROOP_FENCE.getRegistryEntry().registryKey().getValue().toString();
        String still = MapBlocks.STILL_WATER.getRegistryEntry().registryKey().getValue().toString();
        fill(b, p, fence, ox + FENCE, oy, oz - 3, ox + FENCE, oy + 1, oz + SPAN + 3);
        fill(b, p, "minecraft:stone", ox + WALL, oy, oz - 3, ox + WALL, oy + 2, oz + SPAN + 3);
        fill(b, p, fence, ox + 26, oy, oz + 2, ox + 26, oy, oz + 2); // a 1-high fence and a 1-high stone block, for the rise tests
        fill(b, p, "minecraft:stone", ox + 28, oy, oz + 2, ox + 28, oy, oz + 2);
        fill(b, p, "minecraft:water", ox + POOL_X, oy - 1, oz + POOL_Z, ox + POOL_X + POOL_W - 1, oy - 1, oz + POOL_Z + POOL_D - 1);
        fill(b, p, still, ox + POOL_X, oy, oz + POOL_Z, ox + POOL_X + POOL_W - 1, oy, oz + POOL_Z + POOL_D - 1);
        p.teleport(p.getServerWorld(), ox + 0.5, oy, oz - 2.5, 0f, 0f);
        built = true;
    }

    static MobEntity troop(ServerWorld w, double x, double y, double z) {
        var t = new GravesAndGlory.ArmyZombie(w);
        t.refreshPositionAndAngles(x, y, z, 0, 0); t.setOnGround(true); // not spawned, so the navigation's 'at a valid position' test needs the ground flag
        return t;
    }
    static ZombieEntity vanilla(ServerWorld w, double x, double y, double z) {
        var z0 = EntityType.ZOMBIE.create(w);
        z0.refreshPositionAndAngles(x, y, z, 0, 0); z0.setOnGround(true);
        return z0;
    }
    MobEntity open1(ServerWorld w, int py) { return troop(w, ox + 14.5, py, oz + 1.5); }
    MobEntity open2(ServerWorld w, int py) { return troop(w, ox + 19.5, py, oz + 1.5); }
    /** How many nodes of a path are in or on a map block. */
    static int bad(ServerWorld w, Path path) {
        int n = 0;
        if (path != null) for (int i = 0; i < path.getLength(); i++) { var node = path.getNode(i); if (!MapBlockRules.landingOk(w, node.x, node.y, node.z)) n++; }
        return n;
    }
    /** How many nodes of a path stand in the fence wall column or the pool. */
    int through(Path path) {
        int n = 0;
        if (path != null) for (int i = 0; i < path.getLength(); i++) {
            var nd = path.getNode(i);
            if (nd.x == ox + FENCE || (nd.x >= ox + POOL_X && nd.x < ox + POOL_X + POOL_W && nd.z >= oz + POOL_Z && nd.z < oz + POOL_Z + POOL_D)) n++;
        }
        return n;
    }
    static String describe(Path path) { return path == null ? "none" : "length " + path.getLength() + ", reaches target " + path.reachesTarget(); }

    void checks(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        int fx = ox + FENCE, py = oy, sz = oz + 5;
        // ---- node makers: the cell types a troop's maker gives, against a vanilla zombie's
        var cache = new ChunkCache(w, new BlockPos(ox - 3, oy - 3, oz - 3), new BlockPos(ox + 30, oy + 10, oz + 30));
        var army = troop(w, fx - 2.5, py, sz + .5); var plain = vanilla(w, fx - 2.5, py, sz + .5);
        var armyMaker = new LandPathNodeMaker(); armyMaker.init(cache, army);
        var plainMaker = new LandPathNodeMaker(); plainMaker.init(cache, plain);
        var armyCtx = new net.minecraft.entity.ai.pathing.PathContext(cache, army); var plainCtx = new net.minecraft.entity.ai.pathing.PathContext(cache, plain); // 1.21: the node makers take a PathContext (world + mob) instead of the cache
        int px = ox + POOL_X + 2, pz = oz + POOL_Z + 4;
        PathNodeType[] a = {armyMaker.getDefaultNodeType(armyCtx, fx, py, sz), armyMaker.getDefaultNodeType(armyCtx, fx, py + 1, sz), armyMaker.getDefaultNodeType(armyCtx, fx, py + 2, sz),
            armyMaker.getDefaultNodeType(armyCtx, px, py, pz), armyMaker.getDefaultNodeType(armyCtx, px, py + 1, pz), armyMaker.getDefaultNodeType(armyCtx, fx - 2, py, sz)};
        PathNodeType[] v = {plainMaker.getDefaultNodeType(plainCtx, fx, py, sz), plainMaker.getDefaultNodeType(plainCtx, fx, py + 1, sz), plainMaker.getDefaultNodeType(plainCtx, fx, py + 2, sz),
            plainMaker.getDefaultNodeType(plainCtx, px, py, pz), plainMaker.getDefaultNodeType(plainCtx, px, py + 1, pz), plainMaker.getDefaultNodeType(plainCtx, fx - 2, py, sz)};
        report("node-fence", a[0] == PathNodeType.BLOCKED && a[1] == PathNodeType.BLOCKED && a[2] == PathNodeType.BLOCKED, "troop node maker, fence cells 1 and 2 and the cell over the stack: " + a[0] + " " + a[1] + " " + a[2]);
        report("node-still-water", a[3] == PathNodeType.BLOCKED && a[4] == PathNodeType.BLOCKED, "troop node maker, a still_water cell and the cell over it: " + a[3] + " " + a[4]);
        report("node-open-control", a[5] != PathNodeType.BLOCKED && v[5] == a[5], "a plain cell keeps its vanilla type for a troop: " + a[5] + " (vanilla " + v[5] + ")");
        report("node-vanilla-untouched", v[0] != PathNodeType.BLOCKED && v[1] != PathNodeType.BLOCKED && v[3] != PathNodeType.BLOCKED && v[4] != PathNodeType.BLOCKED,
            "a vanilla zombie's node maker is unchanged (both blocks stay open for it, the reason the troop hook exists): fence " + v[0] + " " + v[1] + ", still_water " + v[3] + " " + v[4]);
        var drowned = new GravesAndGlory.ArmyDrowned(w); drowned.refreshPositionAndAngles(fx - 2.5, py, sz + .5, 0, 0);
        var amph = new AmphibiousPathNodeMaker(false); amph.init(cache, drowned); var drownedCtx = new net.minecraft.entity.ai.pathing.PathContext(cache, drowned);
        PathNodeType af = amph.getDefaultNodeType(drownedCtx, fx, py, sz), aw = amph.getDefaultNodeType(drownedCtx, px, py, pz);
        report("node-amphibious", af == PathNodeType.BLOCKED && aw == PathNodeType.BLOCKED, "a drowned troop's amphibious node maker also blocks the fence " + af + " and still_water " + aw);
        // ---- real paths: a troop does not walk through a full-width fence wall, a vanilla zombie's path does
        Path troopPath = army.getNavigation().findPathTo(fx + 4.5, py, sz + .5, 1);
        Path plainPath = vanilla(w, fx - 2.5, py, sz + .5).getNavigation().findPathTo(fx + 4.5, py, sz + .5, 1);
        boolean troopReaches = troopPath != null && troopPath.reachesTarget();
        report("path-fence-wall", !troopReaches && through(troopPath) == 0 && bad(w, troopPath) == 0,
            "a troop's path to a point behind a full-width fence wall: " + describe(troopPath) + ", nodes in the wall " + through(troopPath) + ", in or on a map block " + bad(w, troopPath));
        report("path-fence-control", plainPath != null && plainPath.reachesTarget() && through(plainPath) > 0,
            "control: a vanilla zombie's path goes straight through the wall: " + describe(plainPath) + ", nodes in the wall " + through(plainPath));
        // across the pool: the pool is 5 wide and 9 deep, so a way round it exists
        Path round = troop(w, ox + POOL_X - 2.5, py, oz + POOL_Z + 4.5).getNavigation().findPathTo(ox + POOL_X + POOL_W + 2.5, py, oz + POOL_Z + 4.5, 1);
        report("path-pool", round != null && round.reachesTarget() && bad(w, round) == 0 && through(round) == 0,
            "a troop's path across a still_water pool goes round it: " + describe(round) + ", nodes in the pool " + through(round) + ", in or on a map block " + bad(w, round));
        // ---- sight: the helper sees through the fence and the pool, and still stops at stone
        var fake = FakePlayer.get(w);
        double lz = oz + 14.5;
        var look = troop(w, fx - 3.5, py, lz); var seen = troop(w, fx + 3.5, py, lz);
        boolean vanillaFence = look.canSee(seen), helperFence = MapBlockRules.canSee(look, seen);
        report("sight-fence", !vanillaFence && helperFence, "troop to troop across the 2-high fence wall: vanilla canSee " + vanillaFence + " (stopped), the helper " + helperFence);
        // the split: a melee troop keeps vanilla canSee (a fence stops its target pick), a ranged troop sees past the fence
        report("sight-split", !Tactics.look(false, look, seen) && Tactics.look(true, look, seen) && Tactics.look(false, open1(w, py), open2(w, py)),
            "across the fence wall: melee " + Tactics.look(false, look, seen) + ", ranged " + Tactics.look(true, look, seen) + "; open ground melee " + Tactics.look(false, open1(w, py), open2(w, py)));
        var beyond = troop(w, ox + WALL + 3.5, py, lz); var between = troop(w, ox + WALL - 3.5, py, lz);
        boolean stone = MapBlockRules.canSee(between, beyond), both = MapBlockRules.canSee(look, beyond);
        report("sight-stone", !stone && !both, "a 3-high stone wall still blocks: " + stone + ", and the fence plus the stone: " + both);
        var poolA = troop(w, ox + POOL_X - 1.5, py, oz + POOL_Z + 4.5); var poolB = troop(w, ox + POOL_X + POOL_W + 1.5, py, oz + POOL_Z + 4.5);
        // a pool cube is one block, so an eye-level line passes over it for everyone: judge it on a low line (a crawling body, an arrow's height), the stock collider ray against the helper
        Vec3d lowA = new Vec3d(ox + POOL_X - 1.5, py + .5, oz + POOL_Z + 4.5), lowB = new Vec3d(ox + POOL_X + POOL_W + 1.5, py + .5, oz + POOL_Z + 4.5);
        boolean poolVanilla = w.raycast(new RaycastContext(lowA, lowB, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, poolA)).getType() == HitResult.Type.MISS;
        boolean poolHelper = MapBlockRules.clear(w, lowA, lowB, RaycastContext.ShapeType.COLLIDER, poolA);
        report("sight-still-water", !poolVanilla && poolHelper && MapBlockRules.canSee(poolA, poolB), "a low line across the still_water pool: the stock collider ray " + (poolVanilla ? "passes" : "is stopped") + ", the helper " + poolHelper + "; eye to eye " + MapBlockRules.canSee(poolA, poolB));
        var open1 = troop(w, ox + 14.5, py, oz + 1.5); var open2 = troop(w, ox + 19.5, py, oz + 1.5);
        report("sight-open", MapBlockRules.canSee(open1, open2) && open1.canSee(open2), "open ground: the helper and vanilla agree (" + MapBlockRules.canSee(open1, open2) + " " + open1.canSee(open2) + ")");
        fake.refreshPositionAndAngles(fx - 3.5, py, lz, 0, 0);
        boolean playerVanilla = fake.canSee(seen), playerHelper = MapBlockRules.canSee(fake, seen);
        report("sight-player", !playerVanilla && playerHelper, "a player aiming an order through the fence: vanilla " + playerVanilla + ", the helper " + playerHelper);
        Vec3d eye = new Vec3d(fx - 3.5, py + 1.62, lz), foot = new Vec3d(fx + 3.5, py + .1, lz), wallFoot = new Vec3d(ox + WALL + 3.5, py + .1, lz);
        boolean visualVanilla = w.raycast(new RaycastContext(eye, foot, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, fake)).getType() == HitResult.Type.MISS;
        report("sight-visual", MapBlockRules.clear(w, eye, foot, RaycastContext.ShapeType.VISUAL, fake) && !MapBlockRules.clear(w, eye, wallFoot, RaycastContext.ShapeType.VISUAL, fake),
            "the stall's visual line passes the fence (the stock visual ray: " + (visualVanilla ? "passes too" : "stopped") + ") and stops at the stone wall");
        // ---- landing: in, on or under either block is refused
        report("landing", MapBlockRules.landingOk(w, ox + 3, py, oz + 3) && !MapBlockRules.landingOk(w, fx, py, sz) && !MapBlockRules.landingOk(w, fx, py + 1, sz) && !MapBlockRules.landingOk(w, fx, py + 2, sz)
            && !MapBlockRules.landingOk(w, px, py, pz) && !MapBlockRules.landingOk(w, px, py + 1, pz) && MapBlockRules.landingOk(w, fx, py + 3, sz) && MapBlockRules.landingOk(w, px, py + 2, pz),
            "plain ground lands; feet in the fence, on a 1-high fence, in a 2-high stack, in still_water or on it do not");
        // a one-block rise: a troop does not hop onto a 1-high fence (its top is no floor), and still hops a stone block
        var riser = troop(w, ox + 25.5, py, oz + 2.5); var riser2 = troop(w, ox + 27.5, py, oz + 2.5);
        boolean riseFence = Tactics.onRise(riser, new Vec3d(ox + 26.5, py, oz + 2.5)), riseStone = Tactics.onRise(riser2, new Vec3d(ox + 28.5, py, oz + 2.5));
        report("rise", !riseFence && riseStone, "Tactics.onRise toward a 1-high fence: " + riseFence + ", toward a 1-high stone block (control): " + riseStone);
        // Arena.floor and Tactics.standable on a camp column away from the platform, with the blocks put at its feet cells and then put back
        int cx = (int) Math.floor(p.getX()) + 40, cz = (int) Math.floor(p.getZ()) + 40;
        int g = Arena.ground(cx, cz);
        BlockPos feet = new BlockPos(cx, g + 1, cz);
        BlockState savedGround = w.getBlockState(feet.down()), saved1 = w.getBlockState(feet), saved2 = w.getBlockState(feet.up());
        int[] free = Arena.floor(w, cx, cz, cx + .5, cz + .5);
        w.setBlockState(feet, MapBlocks.TROOP_FENCE.getDefaultState()); w.setBlockState(feet.up(), MapBlocks.TROOP_FENCE.getDefaultState());
        int[] fenced = Arena.floor(w, cx, cz, cx + .5, cz + .5);
        int[] spot = Tactics.standable(b, cx, cz);
        boolean standable = spot != null && !(spot[0] == cx && spot[2] == cz) && MapBlockRules.landingOk(w, spot[0], spot[1], spot[2]);
        w.setBlockState(feet, saved1); w.setBlockState(feet.up(), saved2);
        w.setBlockState(feet.down(), MapBlocks.STILL_WATER.getDefaultState());
        int[] onWater = Arena.floor(w, cx, cz, cx + .5, cz + .5);
        w.setBlockState(feet.down(), savedGround);
        boolean sameFree = free[0] == cx && free[1] == cz && MapBlockRules.landingOk(w, free[0], free[2], free[1]);
        report("arena-floor", sameFree && (fenced[0] != cx || fenced[1] != cz) && MapBlockRules.landingOk(w, fenced[0], fenced[2], fenced[1]) && (onWater[0] != cx || onWater[1] != cz) && MapBlockRules.landingOk(w, onWater[0], onWater[2], onWater[1]),
            String.format("Arena.floor at (%d,%d): free column %d,%d,%d; with a 2-high fence stack on its feet cells %d,%d,%d; with still_water as its ground block %d,%d,%d", cx, cz, free[0], free[1], free[2], fenced[0], fenced[1], fenced[2], onWater[0], onWater[1], onWater[2]));
        report("standable", standable, "Tactics.standable with a fence stack on the column's feet cells: " + (spot == null ? "none" : spot[0] + "," + spot[1] + "," + spot[2]));
    }

    @Override public void cleanup(MinecraftClient c) {
        if (c.getServer() == null) return;
        Phase1Checks.server(c, b -> {
            var p = me(b); if (p == null || !built) return;
            p.teleport(p.getServerWorld(), ox + 0.5, oy, oz - 2.5, 0f, 0f);
            cmd(b, p, String.format("fill %d %d %d %d %d %d minecraft:air", ox - 3, oy - 3, oz - 3, ox + 30, oy + 10, oz + 30));
            if (before != null) p.changeGameMode(before);
            cmd(b, p, "gamerule doDaylightCycle " + cycleBefore); cmd(b, p, "time set " + timeBefore);
        });
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 1500 && stage < 9) { report("timeout", false, "stuck at stage " + stage + ": built " + built); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (k == 5) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) build(b, p); });
                if (k == 40 && built) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) checks(b, p); });
                if (k == 70) next(tick, 9);
            }
            case 9 -> { if (k > 5) { Phase1Checks.finish(c, "map-blocks-troops"); stage = 99; } }
            default -> {}
        }
    }
}
