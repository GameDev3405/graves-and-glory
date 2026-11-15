package gg.gravesandglory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.MapColor;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Section F map blocks (props lead, 2026-11-13; docs/props/MAP-BLOCKS.md). Two real blocks for the map builders:
 * <ul>
 * <li>gravesandglory:troop_fence is an invisible full cube that stops living entities and lets projectiles, targeting and sight rays through.</li>
 * <li>gravesandglory:still_water is a water-looking surface block (14/16 high, tinted still water) that sits on real sealed water, is not a fluid,
 * is a full cube to living entities and nothing to projectiles, and cannot be stood on: the server puts back an entity that stays on it.</li>
 * </ul>
 * Neither needs client code to behave (the client shapes only match the server shapes); a plain client sees the vanilla stand-ins of STAND_INS once the
 * Polymer layer maps them (not added here).
 */
public final class MapBlocks {
    public static final String MOD = "gravesandglory";
    /** The one place that names each block's vanilla stand-in for a plain client (the later Polymer layer reads this). */
    public static final Map<Identifier, Identifier> STAND_INS = Map.of(
        Identifier.of(MOD, "troop_fence"), Identifier.of("minecraft", "barrier"),
        Identifier.of(MOD, "still_water"), Identifier.of("minecraft", "barrier"));

    /** The still-water top, in blocks above the block floor (14/16, as vanilla water draws). */
    public static final double SURFACE = 14 / 16.0;
    /** How far (blocks) a recorded standing place may be and still be used to put an entity back; farther, it is moved to the nearest bank instead. */
    static final double RETURN_RANGE = 8, BANK_RANGE = 12;

    public static final Block TROOP_FENCE = Registry.register(Registries.BLOCK, Identifier.of(MOD, "troop_fence"), new Fence(settings()));
    public static final Block STILL_WATER = Registry.register(Registries.BLOCK, Identifier.of(MOD, "still_water"), new StillWater(settings()));
    public static final Item TROOP_FENCE_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD, "troop_fence"), new BlockItem(TROOP_FENCE, new Item.Settings()));
    public static final Item STILL_WATER_ITEM = Registry.register(Registries.ITEM, Identifier.of(MOD, "still_water"), new BlockItem(STILL_WATER, new Item.Settings()));

    /** What the still-water rule remembers of one living entity: where it last stood on anything else (and in which world), when it may next search for a bank, and how it was put back. */
    static final class Last {
        boolean valid; double x, y, z; RegistryKey<World> world; long nextSearch; int putBacks, viaLast, viaBank;
    }
    private static final Map<Entity, Last> LAST = new WeakHashMap<>();
    /** How often (ticks) one entity may run the bank search, which scans a 25 x 25 area. */
    static final int SEARCH_EVERY = 20;
    /** Times the rule put this entity back since it was loaded (read by the map-blocks check). */
    public static int putBacks(Entity e) {Last l = LAST.get(e); return l == null ? 0 : l.putBacks;}
    /** Of those, how many went to the entity's last standing place rather than a searched bank. */
    public static int putBacksToLast(Entity e) {Last l = LAST.get(e); return l == null ? 0 : l.viaLast;}

    static AbstractBlock.Settings settings() {
        return AbstractBlock.Settings.create().mapColor(MapColor.CLEAR).strength(-1f, 3600000f).dropsNothing().nonOpaque().noBlockBreakParticles()
            .allowsSpawning((s, w, p, t) -> false).solidBlock((s, w, p) -> false).suffocates((s, w, p) -> false).blockVision((s, w, p) -> false)
            .pistonBehavior(PistonBehavior.BLOCK)
            .solid(); // the no-entity collision shape is empty, so without this water, lava and buckets would replace the blocks (FlowableFluid.canFill reads isSolid and blocksMovement)
    }

    /** Loads the class (which registers the blocks and items), adds them to the Building Blocks tab and starts the still-water rule. */
    static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(e -> {e.add(TROOP_FENCE_ITEM); e.add(STILL_WATER_ITEM);});
        ServerTickEvents.END_WORLD_TICK.register(MapBlocks::keepOffStillWater);
    }

    /**
     * Whether this context is one both blocks let through: a projectile's, or one with no entity at all. Vanilla asks an arrow whether it is stuck in the ground with
     * a context-free query (ShapeContext.absent(), no entity), so a full cube there would catch every arrow that flies through the fence cell; the arrow's own
     * collision ray is a projectile context. Every living mover (players, mobs, troops) always comes with its entity and gets the full cube. The one cost: vanilla
     * mob pathfinding reads the block through that context-free query and sees it as open, so troop pathing has to classify the ids (the AI lead does).
     */
    static boolean passes(ShapeContext context) {
        return !(context instanceof EntityShapeContext c) || c.getEntity() == null || c.getEntity() instanceof ProjectileEntity;
    }
    static VoxelShape collision(ShapeContext context) {return passes(context) ? VoxelShapes.empty() : VoxelShapes.fullCube();}

    /** A builder holding the item can still aim at the block to break it or build against it; nobody else can target it. */
    static VoxelShape outline(ShapeContext context, Item item, VoxelShape shape) {return context.isHolding(item) ? shape : VoxelShapes.empty();}

    /** Shows the vanilla barrier-style marker to a creative builder holding the item (client only; the server never calls it). */
    @Environment(EnvType.CLIENT)
    static void marker(BlockState state, World world, BlockPos pos, Item item) {
        var player = net.minecraft.client.MinecraftClient.getInstance().player;
        if (player != null && player.isCreative() && player.isHolding(item))
            world.addParticle(new net.minecraft.particle.BlockStateParticleEffect(net.minecraft.particle.ParticleTypes.BLOCK_MARKER, state), pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 0, 0, 0);
    }

    /** troop_fence: invisible, full cube for everything but projectiles, no outline, no ray block. */
    static final class Fence extends Block {
        Fence(Settings s) {super(s);}
        @Override public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {return collision(context);}
        @Override public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {return outline(context, TROOP_FENCE_ITEM, VoxelShapes.fullCube());}
        @Override public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {return VoxelShapes.empty();}
        @Override public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {return VoxelShapes.empty();}
        @Override public BlockRenderType getRenderType(BlockState state) {return BlockRenderType.INVISIBLE;}
        @Override public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {return 1f;}
        @Environment(EnvType.CLIENT)
        @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {marker(state, world, pos, TROOP_FENCE_ITEM);}
    }

    /** still_water: a 14/16 water look on sealed real water; solid to living entities, nothing to projectiles. Not a fluid. */
    static final class StillWater extends Block {
        StillWater(Settings s) {super(s);}
        @Override public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {return collision(context);}
        @Override public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
            return outline(context, STILL_WATER_ITEM, VoxelShapes.cuboid(0, 0, 0, 1, SURFACE, 1));}
        @Override public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {return VoxelShapes.empty();}
        @Override public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {return VoxelShapes.empty();}
        /** Neighbouring still water draws one surface, like glass. */
        @Override public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {return stateFrom.isOf(this);}
        @Override public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {return 1f;}
        @Environment(EnvType.CLIENT)
        @Override public void randomDisplayTick(BlockState state, World world, BlockPos pos, net.minecraft.util.math.random.Random random) {marker(state, world, pos, STILL_WATER_ITEM);}
    }

    // ---- the still-water rule: nobody stays standing on it ----

    /** Whether the entity is standing on still_water only: on the ground, and every cell under its feet that holds a collision shape is still_water (one foot on the bank is on the bank). */
    public static boolean standingOnStillWater(World world, Entity e) {
        if (!e.isOnGround()) return false;
        Box box = e.getBoundingBox();
        int y = MathHelper.floor(box.minY - 1.0E-3);
        boolean water = false;
        for (int x = MathHelper.floor(box.minX); x <= MathHelper.floor(box.maxX - 1.0E-7); x++)
            for (int z = MathHelper.floor(box.minZ); z <= MathHelper.floor(box.maxZ - 1.0E-7); z++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = world.getBlockState(p);
                if (s.isOf(STILL_WATER)) water = true;
                else if (!s.getCollisionShape(world, p, ShapeContext.of(e)).isEmpty()) return false;
            }
        return water;
    }

    /**
     * Each tick, every living entity that stands on still_water is put back: at the last place it stood on anything else in this world when that is within
     * RETURN_RANGE, otherwise on the nearest bank (searched at most once per SEARCH_EVERY ticks per entity), and when no bank is found at the last place however far.
     * Creative and spectator players (builders) are left alone. Riders and vehicles are not handled (no mounts exist today).
     */
    static void keepOffStillWater(ServerWorld world) {
        long now = world.getTime();
        for (Entity e : world.iterateEntities()) {
            if (!(e instanceof LivingEntity le) || !le.isAlive() || le.isSpectator() || (le instanceof PlayerEntity p && p.isCreative())) continue;
            if (!le.isOnGround()) continue;
            Last l = LAST.computeIfAbsent(le, k -> new Last());
            if (!standingOnStillWater(world, le)) {
                l.valid = true; l.x = le.getX(); l.y = le.getY(); l.z = le.getZ(); l.world = world.getRegistryKey();
                continue;
            }
            boolean last = l.valid && l.world == world.getRegistryKey(); // a place from another world is no place
            Vec3d to = null; boolean viaLast = false;
            if (last && le.squaredDistanceTo(l.x, l.y, l.z) <= RETURN_RANGE * RETURN_RANGE && fits(world, le, l.x, l.y, l.z)) {to = new Vec3d(l.x, l.y, l.z); viaLast = true;}
            if (to == null && now >= l.nextSearch) {
                l.nextSearch = now + SEARCH_EVERY;
                to = nearestBank(world, le);
                if (to == null && last) {to = new Vec3d(l.x, l.y, l.z); viaLast = true;}
            }
            if (to != null) {putBack(le, to); l.putBacks++; if (viaLast) l.viaLast++; else l.viaBank++;}
        }
    }

    static boolean fits(World world, Entity e, double x, double y, double z) {
        Box box = e.getDimensions(e.getPose()).getBoxAt(x, y, z);
        if (!world.isSpaceEmpty(e, box)) return false;
        BlockPos below = BlockPos.ofFloored(x, y - 1.0E-3, z);
        BlockState floor = world.getBlockState(below);
        // dry ground only: not the bed of the real water under still_water, whose cell above is open to the entity too
        return !floor.isOf(STILL_WATER) && !floor.isOf(TROOP_FENCE) && !floor.getCollisionShape(world, below, ShapeContext.of(e)).isEmpty()
            && world.getFluidState(BlockPos.ofFloored(x, y + 0.1, z)).isEmpty() && world.getFluidState(BlockPos.ofFloored(x, y + e.getHeight() - 0.1, z)).isEmpty();
    }

    /** The nearest cell centre within BANK_RANGE where the entity fits standing on a solid floor that is not still_water, scanning a few levels above and below it. */
    static Vec3d nearestBank(World world, Entity e) {
        BlockPos o = e.getBlockPos(); Vec3d best = null; double bestD = Double.MAX_VALUE;
        int r = (int) BANK_RANGE;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            double d = dx * dx + dz * dz;
            if (d >= bestD || d > BANK_RANGE * BANK_RANGE) continue;
            for (int dy = -2; dy <= 3; dy++) {
                BlockPos floor = o.add(dx, dy - 1, dz);
                BlockState fs = world.getBlockState(floor);
                if (fs.isOf(STILL_WATER) || fs.isOf(TROOP_FENCE)) continue;
                VoxelShape shape = fs.getCollisionShape(world, floor, ShapeContext.of(e));
                if (shape.isEmpty()) continue;
                double top = floor.getY() + shape.getMax(Direction.Axis.Y);
                if (fits(world, e, o.getX() + dx + .5, top, o.getZ() + dz + .5)) {bestD = d; best = new Vec3d(o.getX() + dx + .5, top, o.getZ() + dz + .5); break;}
            }
        }
        return best;
    }

    static void putBack(LivingEntity e, Vec3d to) {
        e.setVelocity(Vec3d.ZERO); e.fallDistance = 0;
        if (e instanceof ServerPlayerEntity sp && sp.networkHandler != null) sp.networkHandler.requestTeleport(to.x, to.y, to.z, sp.getYaw(), sp.getPitch());
        else e.refreshPositionAndAngles(to.x, to.y, to.z, e.getYaw(), e.getPitch());
        if (e instanceof MobEntity m) m.getNavigation().stop();
    }
}
