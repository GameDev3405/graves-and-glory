package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Battle map looks (Task 5): the pure painter keeps the preview's hash, sizes and determinism, paints a pond as water and copes with
 *  columns that have no block (an old map's void). Checked by javac alone, never shipped. */
public final class BattleMapPaintCheck {
    static int checks;
    static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); checks++; }

    static BattleMapGrid field(boolean holes) {
        int w = 60, d = 40;
        String[] pal = {"minecraft:grass_block", "minecraft:water", "minecraft:oak_leaves", "minecraft:oak_planks"};
        short[] look = new short[w * d], height = new short[w * d];
        for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
            int i = z * w + x;
            look[i] = 0; height[i] = (short) (64 + x / 12);
            if (Math.hypot(x - 15, z - 20) < 7) { look[i] = 1; height[i] = 63; }
            if (x >= 40 && x < 50 && z >= 5 && z < 15) { look[i] = 3; height[i] = (short) (height[i] + 6); }
            if (x >= 40 && z >= 25) { look[i] = 2; height[i] = (short) (height[i] + 5); }
            if (holes && x < 3) { look[i] = -1; height[i] = -64; }
        }
        return new BattleMapGrid("test", "b", -30, -20, w, d, pal, look, height);
    }

    public static void main(String[] args) throws Exception {
        var kinds = MapKinds.parse(Files.readString(Path.of("src/main/resources/assets/gravesandglory/battlemap/kinds.json")));
        { // hashSameAsPreview: battlemap_preview.hash2 values
            check(BattleMapPaint.hash2(0, 0, 0) == 0.0, "hash2(0,0,0)");
            check(BattleMapPaint.hash2(5, -7, 11) == 0.61883544921875, "hash2(5,-7,11)");
            check(BattleMapPaint.hash2(-300, 200, 23) == 0.2280426025390625, "hash2(-300,200,23)");
            check(BattleMapPaint.hash2(123456, -98765, 3) == 0.728973388671875, "hash2(123456,-98765,3)");
        }
        var g = field(false);
        int[] rgb = {0x536d33, 0x2b519e, 0x43601a, 0xa2834f};
        { // sizes and determinism
            int[] r = BattleMapPaint.real(g, rgb), p = BattleMapPaint.parchment(g, kinds);
            check(r.length == g.width * g.depth, "real is one texel per block");
            check(p.length == g.width * g.depth * 9, "parchment is 3 texels per block");
            check(Arrays.equals(p, BattleMapPaint.parchment(g, kinds)) && Arrays.equals(r, BattleMapPaint.real(g, rgb)), "the same grid paints the same picture");
        }
        { // pondIsWater: the pond's centre is the water wash, a column of the field is not
            var f = BattleMapPaint.resolve(g, kinds);
            int water = Arrays.asList(f.names()).indexOf("water"), building = Arrays.asList(f.names()).indexOf("building");
            check(f.kind()[20 * g.width + 15] == water, "pond centre resolves to water");
            check(f.kind()[10 * g.width + 45] == building, "raised planks resolve to a building");
            int[] p = BattleMapPaint.parchment(g, kinds);
            int W = g.width * 3, c = p[(20 * 3 + 1) * W + 15 * 3 + 1];
            check((c & 255) > (c >> 16 & 255), "pond centre is blue-grey, not paper: " + Integer.toHexString(c));
        }
        { // holesAreUnknown: no-block columns paint the empty colour in the real look and leave the terrain sane
            var h = field(true);
            int[] r = BattleMapPaint.real(h, rgb);
            check((r[5 * h.width] & 0xffffff) == BattleMapPaint.EMPTY, "a column with no block is the empty colour");
            var f = BattleMapPaint.resolve(h, kinds);
            check(f.t()[5 * h.width] > 60, "unknown columns take the neighbouring terrain, not the world bottom: " + f.t()[5 * h.width]);
            int[] p = BattleMapPaint.parchment(h, kinds);
            check(p.length == h.width * h.depth * 9, "parchment of a field with holes");
        }
        System.out.println("BattleMapPaintCheck: " + checks + " checks passed");
    }
}
