package gg.gravesandglory;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.block.Block;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.registry.tag.FluidTags;
import java.util.UUID;

/**
 * Section F map blocks (docs/props/MAP-BLOCKS.md). GRAVES_PHASE1_CHECKS=map-blocks. On a platform high above the camp:
 * troop_fence stops a walking player and a walking zombie, a 2-high stack stops a player who keeps jumping (a 1-high one does not, the control), an arrow
 * flies through it where it stops at a stone block, its outline and ray shapes are empty and both cubes of the stack are full; the client world sees the same shapes.
 * still_water: an arrow flies down through it into the real water below; a zombie spawned on it and the real survival player teleported onto it are put back on the bank. Never shipped.
 */
final class MapBlocksChecks implements Phase1Checks.Suite {
    volatile UUID player;
    volatile boolean built, simmed, launched, checked;
    volatile GameMode before;
    volatile boolean cycleBefore; volatile long timeBefore;
    volatile boolean drive;
    int putsBefore, lastBefore;
    volatile int ox, oy, oz;
    int stage, at;
    PigEntity pig;
    ArrowEntity fenceArrow, stoneArrow, waterArrow;
    static final int FENCE = 8, WALL = 20, POOL_X = 2, POOL_Z = 16, POOL = 7;

    void next(int tick, int s) { stage = s; at = tick; }
    static void report(String id, boolean pass, String detail) { Phase1Checks.report("p1-mapblocks-" + id, pass, detail); }
    static void cmd(GravesAndGlory.Battle b, ServerPlayerEntity p, String line) {
        var src = b.server.getCommandSource().withWorld(p.getServerWorld()).withSilent().withLevel(4);
        b.server.getCommandManager().executeWithPrefix(src, line);
    }
    ServerPlayerEntity me(GravesAndGlory.Battle b) { return b.server.getPlayerManager().getPlayer(player); }
    static String put(String block, int x1, int y1, int z1, int x2, int y2, int z2) { return String.format("fill %d %d %d %d %d %d %s", x1, y1, z1, x2, y2, z2, block); }
    void fill(GravesAndGlory.Battle b, ServerPlayerEntity p, String block, int x1, int y1, int z1, int x2, int y2, int z2) { cmd(b, p, put(block, x1, y1, z1, x2, y2, z2)); }

    void build(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        before = p.interactionManager.getGameMode();
        cycleBefore = b.server.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE); timeBefore = p.getServerWorld().getTimeOfDay();
        ox = (int) Math.floor(p.getX()) - 4; oz = (int) Math.floor(p.getZ()); oy = Math.min(290, (int) Math.floor(p.getY()) + 45);
        cmd(b, p, "gamerule doDaylightCycle false"); cmd(b, p, "time set 6000");
        fill(b, p, "minecraft:air", ox - 3, oy, oz - 3, ox + 30, oy + 10, oz + 30);
        fill(b, p, "minecraft:stone", ox - 3, oy - 3, oz - 3, ox + 30, oy - 1, oz + 30);
        String fence = MapBlocks.TROOP_FENCE.getRegistryEntry().registryKey().getValue().toString();
        // lanes (z offsets): 0 player walks, 2 zombie walks, 4 player hops at a 2-high stack, 6 player hops at a 1-high fence, 8 no fence, 10 arrow at a 2-high stack, 12 arrow at stone
        for (int z : new int[] {0, 2, 4, 10}) fill(b, p, fence, ox + FENCE, oy, oz + z, ox + FENCE, oy + 1, oz + z);
        fill(b, p, fence, ox + FENCE, oy, oz + 6, ox + FENCE, oy, oz + 6);
        fill(b, p, "minecraft:stone", ox + WALL, oy, oz + 10, ox + WALL, oy + 3, oz + 10);
        fill(b, p, "minecraft:stone", ox + FENCE, oy, oz + 12, ox + FENCE, oy + 1, oz + 12);
        fill(b, p, "minecraft:stone", ox + WALL, oy, oz + 12, ox + WALL, oy + 3, oz + 12);
        // the pool: real sealed water one cell under the floor top, still_water on it (its cube top is level with the bank top plus one)
        fill(b, p, "minecraft:water", ox + POOL_X, oy - 1, oz + POOL_Z, ox + POOL_X + POOL - 1, oy - 1, oz + POOL_Z + POOL - 1);
        fill(b, p, MapBlocks.STILL_WATER.getRegistryEntry().registryKey().getValue().toString(), ox + POOL_X, oy, oz + POOL_Z, ox + POOL_X + POOL - 1, oy, oz + POOL_Z + POOL - 1);
        // fluids next to both blocks, in two lanes along x so that the water never reaches the lava: a water source at x 25 and a lava source at x 28 (z 26), still_water on the west side and a fence on the east
        String still = MapBlocks.STILL_WATER.getRegistryEntry().registryKey().getValue().toString();
        for (int x : new int[] {24, 27}) {
            fill(b, p, still, ox + x, oy, oz + 26, ox + x, oy, oz + 26);
            fill(b, p, fence, ox + x + 2, oy, oz + 26, ox + x + 2, oy, oz + 26);
            fill(b, p, x == 24 ? "minecraft:water" : "minecraft:lava", ox + x + 1, oy, oz + 26, ox + x + 1, oy, oz + 26);
        }
        p.teleport(p.getServerWorld(), ox + 0.5, oy, oz - 2.5, 0f, 0f);
        built = true;
    }

    /** Walks (and, with hop, keeps jumping) an entity east from x = ox+2.5 along lane z, one move() per tick, the same collision code as the game. Returns {final x, highest y}. */
    double[] run(Entity e, int lane, boolean hop) {
        double x = ox + 2.5, z = oz + lane + .5;
        e.refreshPositionAndAngles(x, oy, z, 0, 0);
        double vy = 0, top = oy; boolean grounded = false;
        for (int t = 0; t < 90; t++) {
            if (hop && grounded) vy = 0.42;
            double y0 = e.getY();
            e.move(MovementType.SELF, new Vec3d(0.22, vy, 0));
            grounded = vy <= 0 && e.getY() - y0 > vy + 1e-7;
            vy = grounded ? -0.0784 : (vy - 0.08) * 0.98;
            top = Math.max(top, e.getY());
        }
        return new double[] {e.getX(), top};
    }

    void sims(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        var fake = FakePlayer.get(w);
        double wall = ox + FENCE; // the fence cell is [wall, wall+1)
        double[] walk = run(fake, 0, false), mob, hop2, hop1, free;
        report("player-walk", walk[0] < wall, String.format("a player walking east stops at x %.2f, short of the fence cell at %d", walk[0], (int) wall));
        var z = EntityType.ZOMBIE.create(w);
        mob = run(z, 2, false);
        report("mob-walk", mob[0] < wall, String.format("a zombie walking east stops at x %.2f, short of %d", mob[0], (int) wall));
        hop2 = run(fake, 4, true);
        report("stack-blocks-jump", hop2[0] < wall && hop2[1] > oy + 1.0, String.format("a player jumping east all the way: highest y %.2f above the floor (a jump clears 1.25, the stack is 2), stopped at x %.2f", hop2[1] - oy, hop2[0]));
        hop1 = run(fake, 6, true);
        report("jump-control", hop1[0] > wall + 1.5, String.format("control: the same hops over a 1-high fence reach x %.2f, past it", hop1[0]));
        free = run(fake, 8, false);
        report("walk-control", free[0] > wall + 1.5, String.format("control: the same walk with no fence reaches x %.2f", free[0]));
        // shapes
        BlockPos lo = new BlockPos(ox + FENCE, oy, oz + 4), hi = lo.up();
        var arrow = new ArrowEntity(w, ox, oy, oz);
        var fenceState = w.getBlockState(lo);
        boolean cubes = Block.isShapeFullCube(fenceState.getCollisionShape(w, lo, ShapeContext.of(fake))) && Block.isShapeFullCube(w.getBlockState(hi).getCollisionShape(w, hi, ShapeContext.of(z)))
            && fenceState.isOf(MapBlocks.TROOP_FENCE) && w.getBlockState(hi).isOf(MapBlocks.TROOP_FENCE);
        report("stack-cubes", cubes, "both cells of a 2-high stack are full cubes for a player and a mob, with no gap");
        report("projectile-empty", fenceState.getCollisionShape(w, lo, ShapeContext.of(arrow)).isEmpty() && fenceState.getCollisionShape(w, lo, ShapeContext.absent()).isEmpty() && !MapBlocks.passes(ShapeContext.of(fake)) && !MapBlocks.passes(ShapeContext.of(z)),
            "an arrow's context gets an empty collision shape, and so does the context-free query vanilla uses for an arrow stuck in a block; players and mobs never do");
        report("no-outline", fenceState.getOutlineShape(w, lo, ShapeContext.of(fake)).isEmpty() && fenceState.getOutlineShape(w, lo, ShapeContext.absent()).isEmpty()
            && fenceState.getRaycastShape(w, lo).isEmpty() && fenceState.getCameraCollisionShape(w, lo, ShapeContext.of(fake)).isEmpty() && fenceState.getOutlineShape(w, lo, ShapeContext.of(fake)).isEmpty(),
            "no outline for a player, no raycast shape, no camera collision");
        var sight = w.raycast(new net.minecraft.world.RaycastContext(new Vec3d(ox + FENCE - 2, oy + 1, oz + 4.5), new Vec3d(ox + FENCE + 3, oy + 1, oz + 4.5), net.minecraft.world.RaycastContext.ShapeType.OUTLINE, net.minecraft.world.RaycastContext.FluidHandling.NONE, fake));
        report("ray-passes", sight.getType() == net.minecraft.util.hit.HitResult.Type.MISS, "an outline ray (aim, targeting) from a player through the stack: " + sight.getType());
        report("opacity", !fenceState.isOpaque() && fenceState.getLuminance() == 0 && !fenceState.isSolidBlock(w, lo) && fenceState.getOpacity(w, lo) == 0, "not opaque, no light, the light passes (opacity " + fenceState.getOpacity(w, lo) + ")");
        var sw = w.getBlockState(new BlockPos(ox + POOL_X, oy, oz + POOL_Z)); var swp = new BlockPos(ox + POOL_X, oy, oz + POOL_Z);
        report("water-shapes", sw.isOf(MapBlocks.STILL_WATER) && sw.getFluidState().isEmpty() && Block.isShapeFullCube(sw.getCollisionShape(w, swp, ShapeContext.of(z))) && sw.getCollisionShape(w, swp, ShapeContext.of(arrow)).isEmpty()
            && sw.getOutlineShape(w, swp, ShapeContext.of(fake)).isEmpty(), "still_water is no fluid, a full cube to a mob, empty to an arrow, and not targetable");
        simmed = true;
    }

    void launch(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        fenceArrow = shoot(w, ox + 2.5, oy + 1.0, oz + 10.5, 1.5, 0);
        stoneArrow = shoot(w, ox + 2.5, oy + 1.0, oz + 12.5, 1.5, 0);
        // into the pool, falling at 45 degrees from above, from x = POOL_X - 1.5 toward the middle
        waterArrow = shoot(w, ox + POOL_X + 0.5, oy + 3.0, oz + POOL_Z + 5.5, 0.6, -0.6); // two blocks clear of the pig and the player
        // A pig with its AI on: a no-AI mob never falls or settles, so it is never on the ground. It may wander, which is fine: it is judged on not standing on the water.
        pig = EntityType.PIG.create(w);
        pig.refreshPositionAndAngles(ox + POOL_X + 3.5, oy + 1.0, oz + POOL_Z + 3.5, 0, 0);
        pig.setPersistent(); pig.setSilent(true);
        w.spawnEntity(pig);
        p.changeGameMode(GameMode.SURVIVAL);
        p.teleport(w, ox + POOL_X + 4.5, oy + 1.0, oz + POOL_Z + 1.5, 0f, 0f);
        launched = true;
    }
    static ArrowEntity shoot(ServerWorld w, double x, double y, double z, double vx, double vy) {
        var a = new ArrowEntity(w, x, y, z);
        a.setNoGravity(true); a.setVelocity(vx, vy, 0); a.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.DISALLOWED;
        w.spawnEntity(a);
        return a;
    }

    void verdict(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        double wall = ox + FENCE;
        report("arrow-passes", fenceArrow.getX() > wall + 1.5, String.format("an arrow shot east through a 2-high stack ends at x %.2f, past it (the fence is at %d)", fenceArrow.getX(), (int) wall));
        report("arrow-control", stoneArrow.getX() < wall + .6, String.format("control: the same arrow at a stone block ends at x %.2f, in front of %d", stoneArrow.getX(), (int) wall));
        report("arrow-into-water", waterArrow.getY() < oy - 0.3 && waterArrow.getY() > oy - 3, String.format("an arrow falling into still_water goes through it to the real water below: y %.2f against the water block bottom at %d", waterArrow.getY(), oy));
        boolean off = !MapBlocks.standingOnStillWater(w, pig) && !inPool(pig.getX(), pig.getZ()) && Math.abs(pig.getY() - oy) < .05;
        report("mob-put-back", off, String.format("a pig put on still_water (alive %b, ground %b, on water %b, burning %b) ends at %.2f %.2f %.2f, on the bank (pool x %d-%d, z %d-%d, floor %d)", pig.isAlive(), pig.isOnGround(), MapBlocks.standingOnStillWater(w, pig), pig.isOnFire(), pig.getX(), pig.getY(), pig.getZ(), ox + POOL_X, ox + POOL_X + POOL, oz + POOL_Z, oz + POOL_Z + POOL, oy));
        report("player-put-back", !MapBlocks.standingOnStillWater(w, p) && !inPool(p.getX(), p.getZ()) && Math.abs(p.getY() - oy) < .1,
            String.format("the real player put on still_water ends at %.2f %.2f %.2f, on the bank", p.getX(), p.getY(), p.getZ()));
        report("put-back-count", MapBlocks.putBacks(pig) >= 1 && MapBlocks.putBacks(p) >= 1, "put-backs by the server rule: the pig " + MapBlocks.putBacks(pig) + ", the player " + MapBlocks.putBacks(p));
        boolean held = true; StringBuilder why = new StringBuilder();
        for (int x : new int[] {24, 27}) {
            var water = w.getBlockState(new BlockPos(ox + x, oy, oz + 26)); var fence = w.getBlockState(new BlockPos(ox + x + 2, oy, oz + 26));
            boolean ok = water.isOf(MapBlocks.STILL_WATER) && fence.isOf(MapBlocks.TROOP_FENCE) && water.isSolid() && fence.isSolid() && water.blocksMovement() && fence.blocksMovement();
            held &= ok; why.append(x == 24 ? "water: " : "; lava: ").append(ok ? "both blocks stand" : "replaced (" + water + ", " + fence + ")");
        }
        // the fluids flow along z (the blocks are on the x sides)
        boolean flowed = (w.getFluidState(new BlockPos(ox + 25, oy, oz + 27)).isIn(FluidTags.WATER) || w.getFluidState(new BlockPos(ox + 25, oy, oz + 25)).isIn(FluidTags.WATER))
            && (w.getFluidState(new BlockPos(ox + 28, oy, oz + 27)).isIn(FluidTags.LAVA) || w.getFluidState(new BlockPos(ox + 28, oy, oz + 25)).isIn(FluidTags.LAVA));
        report("fluids-leave-blocks", held && flowed, why + "; the fluids really flowed: " + flowed);
        checked = true;
    }
    /** The real player, survival, on the bank two blocks west of the pool facing east. */
    void bankStart(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        p.changeGameMode(GameMode.SURVIVAL);
        p.teleport(p.getServerWorld(), ox + POOL_X - 1.5, oy, oz + POOL_Z + 3.5, -90f, 0f);
    }
    void walkVerdict(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        ServerWorld w = p.getServerWorld();
        int puts = MapBlocks.putBacks(p) - putsBefore, viaLast = MapBlocks.putBacksToLast(p) - lastBefore;
        report("walk-on-put-back", puts >= 1 && viaLast >= 1 && !MapBlocks.standingOnStillWater(w, p) && !inPool(p.getX(), p.getZ()) && Math.abs(p.getY() - oy) < .1,
            String.format("the player hopped from the bank onto still_water: put back %d times, %d of them to the last place he stood; ends at %.2f %.2f %.2f on the bank", puts, viaLast, p.getX(), p.getY(), p.getZ()));
    }
    void creativeStart(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        putsBefore = MapBlocks.putBacks(p);
        p.changeGameMode(GameMode.CREATIVE); p.getAbilities().flying = false;
        p.teleport(p.getServerWorld(), ox + POOL_X + 3.5, oy + 1.0, oz + POOL_Z + 3.5, 0f, 0f);
    }
    void creativeVerdict(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        boolean on = MapBlocks.standingOnStillWater(p.getServerWorld(), p);
        report("creative-exempt", on && MapBlocks.putBacks(p) == putsBefore, String.format("a creative player stands on still_water untouched: on it %b, put back %d times, at %.2f %.2f %.2f", on, MapBlocks.putBacks(p) - putsBefore, p.getX(), p.getY(), p.getZ()));
    }
    boolean inPool(double x, double z) { return x > ox + POOL_X && x < ox + POOL_X + POOL && z > oz + POOL_Z && z < oz + POOL_Z + POOL; }

    void clientView(MinecraftClient c) {
        var w = c.world; BlockPos lo = new BlockPos(ox + FENCE, oy, oz + 4);
        var arrow = new ArrowEntity(w, ox, oy, oz);
        var s = w.getBlockState(lo);
        report("client-agrees", s.isOf(MapBlocks.TROOP_FENCE) && Block.isShapeFullCube(s.getCollisionShape(w, lo, ShapeContext.of(c.player))) && s.getCollisionShape(w, lo, ShapeContext.of(arrow)).isEmpty()
            && s.getOutlineShape(w, lo, ShapeContext.of(c.player)).isEmpty() && w.getBlockState(new BlockPos(ox + POOL_X, oy, oz + POOL_Z)).isOf(MapBlocks.STILL_WATER),
            "the client world holds the same blocks with the same collision, outline and projectile shapes");
    }

    @Override public void cleanup(MinecraftClient c) {
        if (c.getServer() == null) return;
        Phase1Checks.server(c, b -> {
            var p = me(b); if (p == null || !built) return;
            for (Entity e : new Entity[] {pig, fenceArrow, stoneArrow, waterArrow}) if (e != null) e.discard();
            p.teleport(p.getServerWorld(), ox + 0.5, oy, oz - 2.5, 0f, 0f);
            cmd(b, p, put("minecraft:air", ox - 3, oy - 3, oz - 3, ox + 30, oy + 10, oz + 30));
            if (before != null) p.changeGameMode(before);
            cmd(b, p, "gamerule doDaylightCycle " + cycleBefore); cmd(b, p, "time set " + timeBefore);
        });
    }

    public void step(MinecraftClient c, int tick) {
        if (tick > 3000 && stage < 9) { report("timeout", false, "stuck at stage " + stage + ": built " + built + ", simmed " + simmed + ", launched " + launched + ", checked " + checked); next(tick, 9); }
        int k = tick - at;
        switch (stage) {
            case 0 -> { if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); next(tick, 1); } }
            case 1 -> {
                if (k == 5) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) build(b, p); });
                if (k == 40 && built) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) sims(b, p); });
                if (k == 50 && simmed) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) launch(b, p); });
                if (k == 140 && launched) { Phase1Checks.server(c, b -> { var p = me(b); if (p != null) verdict(b, p); }); clientView(c); }
                if (k == 150) next(tick, 2);
            }
            case 2 -> { // a real client hops from the bank onto still_water
                if (k == 1) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) bankStart(b, p); });
                if (k == 40) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) {putsBefore = MapBlocks.putBacks(p); lastBefore = MapBlocks.putBacksToLast(p);} });
                if (k == 41) drive = true;
                if (drive && c.player != null) {
                    var v = c.player.getVelocity();
                    c.player.setVelocity(0.2, v.y, 0);
                    if (c.player.isOnGround()) c.player.jump();
                }
                if (k == 90) {drive = false; c.player.setVelocity(0, c.player.getVelocity().y, 0);}
                if (k == 100) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) walkVerdict(b, p); });
                if (k == 110) next(tick, 3);
            }
            case 3 -> { // creative builders are left alone
                if (k == 1) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) creativeStart(b, p); });
                if (k == 60) Phase1Checks.server(c, b -> { var p = me(b); if (p != null) creativeVerdict(b, p); });
                if (k == 70) next(tick, 9);
            }
            case 9 -> { if (k > 5) { Phase1Checks.finish(c, "map-blocks"); stage = 99; } }
            default -> {}
        }
    }
}
