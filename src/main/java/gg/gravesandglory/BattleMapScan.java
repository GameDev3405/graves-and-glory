package gg.gravesandglory;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The server's one scan of the playable field, run once per map build (step 1 of tools/battlemap_preview.py). For every block column
 *  of the field (|x| <= halfX, |z| <= halfZ, world coordinates) it keeps the highest block whose model covers the whole cell top and the
 *  height of that top, so the grid says what a player looking down would see. Air and small things (torches, flowers, plants, fences'
 *  thin posts, carpets of grass) are skipped; water is kept, including what a seagrass or kelp stands in. */
final class BattleMapScan {
    static final Logger LOG = LoggerFactory.getLogger("gravesandglory");

    /** A block that fills most of its cell from above. Thin shapes (a torch, a flower, a fence post) do not count. */
    private static final double FULL = 0.9;

    /** @param build the build key the caller caches on (EngineMap.sha, or "old-" + map + "-" + Spec hash); stored on the grid unread */
    static BattleMapGrid scan(ServerWorld w, String map, String build, int halfX, int halfZ) {
        long t0 = System.nanoTime();
        int width = 2 * halfX + 1, depth = 2 * halfZ + 1, bottom = w.getBottomY();
        short[] look = new short[width * depth], height = new short[width * depth];
        Map<String, Short> index = new HashMap<>();
        List<String> palette = new ArrayList<>();
        var pos = new BlockPos.Mutable();
        for (int z = -halfZ; z <= halfZ; z++) {
            for (int x = -halfX; x <= halfX; x++) {
                // load the chunk first: an unloaded chunk answers the world bottom (the rule Arena and EngineMaps follow)
                int top = w.getChunk(x >> 4, z >> 4).sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 15, z & 15);
                int i = (z + halfZ) * width + x + halfX;
                look[i] = -1;
                height[i] = (short) bottom;
                for (int y = top; y >= bottom; y--) {
                    BlockState s = w.getBlockState(pos.set(x, y, z));
                    if (s.isAir()) continue;
                    String id = null;
                    int h = y + 1;
                    if (s.getBlock() instanceof FluidBlock) id = "minecraft:water";
                    else {
                        var shape = s.getOutlineShape(w, pos);
                        boolean full = !shape.isEmpty() && shape.getMax(Direction.Axis.X) - shape.getMin(Direction.Axis.X) >= FULL
                            && shape.getMax(Direction.Axis.Z) - shape.getMin(Direction.Axis.Z) >= FULL;
                        if (full) { id = Registries.BLOCK.getId(s.getBlock()).toString(); h = (int) Math.round(y + shape.getMax(Direction.Axis.Y)); }
                        else if (s.getFluidState().getFluid() == Fluids.WATER || s.getFluidState().getFluid() == Fluids.FLOWING_WATER) id = "minecraft:water"; // seagrass and kelp
                    }
                    if (id == null) continue;
                    Short p = index.get(id);
                    if (p == null) { p = (short) palette.size(); index.put(id, p); palette.add(id); }
                    look[i] = p;
                    height[i] = (short) h;
                    break;
                }
            }
        }
        LOG.info("battle map scan of {} ({}x{}, {} block kinds) took {} ms", map, width, depth, palette.size(), (System.nanoTime() - t0) / 1_000_000);
        return new BattleMapGrid(map, build, -halfX, -halfZ, width, depth, palette.toArray(new String[0]), look, height);
    }
}
