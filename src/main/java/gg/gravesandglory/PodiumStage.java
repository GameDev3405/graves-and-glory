package gg.gravesandglory;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** The shared podium stage of the match ending (PodiumLayout says where): a dark stone dais, three oak plinths with stripped-oak tops, a deepslate backdrop with two black banners, lanterns on posts. Built once at server start
 *  (after the camp), checked by a marker block, and force-loaded only while an ending runs (ensure / release). */
final class PodiumStage {
    private PodiumStage() {}
    /** Buried under the dais centre: its presence means the stage stands. */
    static final Block MARKER = Blocks.CRYING_OBSIDIAN;
    /** Backdrop: 15 wide, 8 high, standing this many blocks behind (north of) the centre column. */
    static final int BACKDROP_DZ = -7, BACKDROP_WIDTH = 15, BACKDROP_HEIGHT = 8;

    static BlockPos markerPos() {
        int[] o = PodiumLayout.origin();
        return new BlockPos(o[0], o[1] - 3, o[2]);
    }

    /** The chunk box of the stage and its view spot, as chunk coordinates {minX, minZ, maxX, maxZ}. */
    static int[] chunks() {
        int[] o = PodiumLayout.origin();
        return new int[]{(o[0] - 16) >> 4, (o[2] - 16) >> 4, (o[0] + 16) >> 4, (o[2] + 24) >> 4};
    }

    /** Force-loads the stage's chunks (during an ending, and by build while it writes). */
    static void ensure(ServerWorld w) {
        int[] c = chunks();
        for (int x = c[0]; x <= c[2]; x++) for (int z = c[1]; z <= c[3]; z++) w.setChunkForced(x, z, true);
    }

    /** Lets the chunks go again (the ending's teardown calls it). */
    static void release(ServerWorld w) {
        int[] c = chunks();
        for (int x = c[0]; x <= c[2]; x++) for (int z = c[1]; z <= c[3]; z++) w.setChunkForced(x, z, false);
    }

    static boolean built(ServerWorld w) { return w.getBlockState(markerPos()).isOf(MARKER); }

    /** Places the stage unless the marker stands; the marker is written last, so an interrupted build is redone. The chunks are held only while it runs (an ending holds them with ensure). */
    static void build(ServerWorld w) {
        ensure(w);
        try {
            if (built(w)) { System.out.println("[Graves and Glory] podium stage present"); return; }
            long started = System.nanoTime();
            int[] o = PodiumLayout.origin();
            int x = o[0], y = o[1], z = o[2], hx = PodiumLayout.HALF_X, hz = PodiumLayout.HALF_Z;
            Block tiles = Blocks.DEEPSLATE_TILES, bricks = Blocks.DEEPSLATE_BRICKS, polished = Blocks.POLISHED_BLACKSTONE;
            // Clear the volume first, then the foundation (three deep so the stage never floats), the dais top and its tiled rim.
            fill(w, x - hx, y + 1, z - hz - 2, x + hx, y + 14, z + hz + 14, Blocks.AIR);
            fill(w, x - hx, y - 3, z - hz, x + hx, y - 1, z + hz, tiles);
            fill(w, x - hx, y, z - hz, x + hx, y, z + hz, polished);
            for (int dx = -hx; dx <= hx; dx++) for (int dz = -hz; dz <= hz; dz++)
                if (Math.abs(dx) == hx || Math.abs(dz) == hz) set(w, x + dx, y, z + dz, tiles);
            // Plinths: oak logs standing on end, a stripped-oak log on top.
            double[][] pl = PodiumLayout.plinths();
            for (int i = 0; i < 3; i++) {
                int px = (int) Math.floor(pl[i][0]), h = PodiumLayout.HEIGHTS[i];
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    for (int k = 1; k < h; k++) set(w, px + dx, y + k, z + dz, Blocks.OAK_LOG);
                    set(w, px + dx, y + h, z + dz, Blocks.STRIPPED_OAK_LOG);
                }
            }
            // Backdrop: deepslate bricks, a tile course at the foot, a blackstone coping.
            int bz = z + BACKDROP_DZ, half = BACKDROP_WIDTH / 2;
            fill(w, x - half, y + 1, bz, x + half, y + BACKDROP_HEIGHT, bz, bricks);
            fill(w, x - half, y + 1, bz, x + half, y + 1, bz, tiles);
            fill(w, x - half - 1, y + BACKDROP_HEIGHT + 1, bz, x + half + 1, y + BACKDROP_HEIGHT + 1, bz, polished);
            // Two black banners on the front face, either side of the middle plinth.
            for (int dx : new int[]{-5, 5})
                w.setBlockState(new BlockPos(x + dx, y + 6, bz + 1), Blocks.BLACK_WALL_BANNER.getDefaultState().with(WallBannerBlock.FACING, Direction.SOUTH), 2);
            // Lanterns on dark-oak posts: the two front corners and the two ends of the backdrop.
            int[][] posts = {{-hx + 1, hz - 1}, {hx - 1, hz - 1}, {-half, BACKDROP_DZ + 1}, {half, BACKDROP_DZ + 1}};
            for (int[] p : posts) {
                set(w, x + p[0], y + 1, z + p[1], Blocks.DARK_OAK_FENCE);
                set(w, x + p[0], y + 2, z + p[1], Blocks.DARK_OAK_FENCE);
                set(w, x + p[0], y + 3, z + p[1], Blocks.LANTERN);
            }
            w.setBlockState(markerPos(), MARKER.getDefaultState(), 2);
            System.out.println("[Graves and Glory] podium stage built at " + x + "," + y + "," + z + " in " + (System.nanoTime() - started) / 1000000 + " ms");
        } finally {
            release(w);
        }
    }

    private static void set(ServerWorld w, int x, int y, int z, Block b) { w.setBlockState(new BlockPos(x, y, z), b.getDefaultState(), 2); }

    private static void fill(ServerWorld w, int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        var pos = new BlockPos.Mutable();
        var s = b.getDefaultState();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) w.setBlockState(pos.set(x, y, z), s, 2);
    }
}
