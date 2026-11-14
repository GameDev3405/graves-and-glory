package gg.gravesandglory;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * What the troop side does with the two map blocks (docs/props/MAP-BLOCKS.md; AI lead, 2026-11-13). Both blocks answer a context-free shape query with an empty shape
 * and every living entity's with a full cube, so anything that reads shapes without an entity (vanilla pathfinding, a free-cell test) sees them as open air and
 * anything that reads them with the looker's context (LivingEntity.canSee, a collider ray) sees a wall. These rules key on the block ids instead:
 * troop_fence is a wall troops see through, still_water is no-entry water, and neither is ever a floor or a place to stand inside.
 */
public final class MapBlockRules {
    private MapBlockRules() {}

    /** Whether the state is troop_fence or still_water. */
    public static boolean isMapBlock(BlockState s) {return s.isOf(MapBlocks.TROOP_FENCE) || s.isOf(MapBlocks.STILL_WATER);}

    /** Whether a troop may be put with its feet in cell (x, y, z): neither of the two map blocks in the cell it stands in, the one above it (head room) or the one under it (a floor). */
    public static boolean landingOk(BlockView w, int x, int y, int z) {
        var p = new BlockPos.Mutable();
        for (int dy = -1; dy <= 1; dy++) if (isMapBlock(w.getBlockState(p.set(x, y + dy, z)))) return false;
        return true;
    }

    /**
     * The node type a troop's pathfinder gives the cell (x, y, z) once the map blocks are known: BLOCKED for a cell holding one of them, and for the cell above one
     * (vanilla reads the block under a cell with the same empty context-free shape, finds no floor there and so lets a troop walk the cell above as open air, or over a
     * lake of still_water). Any other type is kept. Called from the path node makers of troop mobs only (TroopPathMixin).
     */
    public static PathNodeType troopNode(BlockView w, int x, int y, int z, PathNodeType type) {
        if (type == PathNodeType.BLOCKED) return type;
        var p = new BlockPos.Mutable(x, y, z);
        if (isMapBlock(w.getBlockState(p)) || isMapBlock(w.getBlockState(p.set(x, y - 1, z)))) return PathNodeType.BLOCKED;
        return type;
    }

    /**
     * Whether the segment from to is clear of blocks for the given shape type (COLLIDER, as LivingEntity.canSee reads, or VISUAL, as the stall's watched test reads), with the
     * context's entity, and troop_fence and still_water ignored. The same walk as BlockView.raycast over the same shapes, fluids left out (FluidHandling.NONE); only a
     * cell holding one of the two blocks is skipped, so ordinary walls block exactly as before.
     */
    public static boolean clear(World world, Vec3d from, Vec3d to, RaycastContext.ShapeType type, Entity context) {
        var ctx = new RaycastContext(from, to, type, RaycastContext.FluidHandling.NONE, context);
        BlockHitResult hit = BlockView.raycast(from, to, ctx, (c, pos) -> {
            BlockState state = world.getBlockState(pos);
            if (isMapBlock(state)) return null;
            return world.raycastBlock(c.getStart(), c.getEnd(), pos, c.getBlockShape(state, world, pos), state);
        }, c -> {
            Vec3d d = c.getStart().subtract(c.getEnd());
            return BlockHitResult.createMissed(c.getEnd(), Direction.getFacing(d.x, d.y, d.z), BlockPos.ofFloored(c.getEnd()));
        });
        return hit.getType() == HitResult.Type.MISS;
    }

    /** LivingEntity.canSee with the two map blocks transparent: same world, within 128 blocks, eye to eye, a COLLIDER ray from the looker. Use it wherever a troop or an order tests sight. */
    public static boolean canSee(Entity looker, Entity target) {
        if (target.getWorld() != looker.getWorld()) return false;
        Vec3d from = new Vec3d(looker.getX(), looker.getEyeY(), looker.getZ()), to = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        if (to.distanceTo(from) > 128.0) return false;
        return clear(looker.getWorld(), from, to, RaycastContext.ShapeType.COLLIDER, looker);
    }
}
