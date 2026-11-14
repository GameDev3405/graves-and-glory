package gg.gravesandglory;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * The drop board's court in the camp (DropLayout says where; design 2026-11-14, section 4): a flat stone court with a tiled rim, the
 * altar's plinth at its back and the altar's props on it. Built once at server start after the camp, checked by a marker block that
 * is written last, so an interrupted build is redone.
 */
final class DropStage {
    private DropStage() {}
    /** Buried under the court's centre: its presence means the court stands. */
    static final Block MARKER = Blocks.CRYING_OBSIDIAN;

    static BlockPos markerPos() {
        int[] o = DropLayout.origin();
        return new BlockPos(o[0], o[1] - 3, o[2]);
    }

    /** Whether the court stands in the running server's world: set by build at server start. */
    private static volatile ServerWorld standing;
    static boolean built(ServerWorld w) { return standing == w; }

    private static void force(ServerWorld w, boolean on) {
        int[] o = DropLayout.origin();
        for (int x = (o[0] - 12) >> 4; x <= (o[0] + 12) >> 4; x++) for (int z = (o[2] - 12) >> 4; z <= (o[2] + 12) >> 4; z++) w.setChunkForced(x, z, on);
    }

    /** A court block's place: `side` blocks to the viewer's right of the centre, `front` blocks out toward the viewer, `up` over the floor block. */
    static BlockPos at(int side, int front, int up) {
        int[] o = DropLayout.origin(), out = DropLayout.out(), r = DropLayout.right();
        return new BlockPos(o[0] + r[0] * side + out[0] * front, o[1] + up, o[2] + r[1] * side + out[1] * front);
    }

    static void build(ServerWorld w) {
        force(w, true);
        try { raise(w); standing = w; } finally { force(w, false); }
    }

    private static void raise(ServerWorld w) {
        if (w.getBlockState(markerPos()).isOf(MARKER)) { System.out.println("[Graves and Glory] drop board court present"); return; }
        long started = System.nanoTime();
        int half = DropLayout.COURT_WIDTH / 2, back = -DropLayout.COURT_DEPTH / 2, front = DropLayout.COURT_DEPTH / 2 - 1;
        Block tiles = Blocks.DEEPSLATE_TILES, floor = Blocks.POLISHED_DEEPSLATE, bricks = Blocks.DEEPSLATE_BRICKS;
        for (int s = -half; s <= half; s++) for (int f = back; f <= front; f++) {
            for (int up = 1; up <= 18; up++) set(w, at(s, f, up), Blocks.AIR);
            for (int up = -3; up <= -1; up++) set(w, at(s, f, up), bricks);
            boolean rim = Math.abs(s) == half || f == back || f == front;
            set(w, at(s, f, 0), rim ? tiles : floor);
        }
        // the altar's plinth: two courses under the slots, across the altar's width, at the back
        for (int s = -5; s <= 5; s++) for (int f = back; f <= back + 1; f++) for (int up = 1; up <= 2; up++) set(w, at(s, f, up), up == 2 ? tiles : bricks);
        placeProps(w);
        set(w, markerPos(), MARKER);
        System.out.println("[Graves and Glory] drop board court built at " + markerPos().toShortString() + " in " + (System.nanoTime() - started) / 1000000 + " ms");
    }

    /** The altar, its braziers: placed by the prop system (the props arrive with the art). */
    static void placeProps(ServerWorld w) {}

    private static void set(ServerWorld w, BlockPos p, Block b) { w.setBlockState(p, b.getDefaultState(), 2); }
}
