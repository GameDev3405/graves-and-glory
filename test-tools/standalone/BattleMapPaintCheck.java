package gg.gravesandglory;

import java.util.Arrays;

/** Battle map look (Task 5, Revision 2: the real look only): the pure painter keeps its sizes and determinism, paints a pond as water,
 *  copes with columns that have no block (an old map's void) and tones the field down as the approved mockup. Checked by javac alone,
 *  never shipped. */
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
        var g = field(false);
        int[] rgb = {0x536d33, 0x2b519e, 0x43601a, 0xa2834f};
        { // sizes and determinism
            int[] r = BattleMapPaint.real(g, rgb);
            check(r.length == g.width * g.depth, "real is one texel per block");
            check(Arrays.equals(r, BattleMapPaint.real(g, rgb)), "the same grid paints the same picture");
        }
        { // pondIsWater: the pond's centre takes the water colour, the field the grass colour
            int[] r = BattleMapPaint.real(g, rgb);
            int c = r[20 * g.width + 15];
            check((c & 255) > (c >> 16 & 255), "pond centre is blue: " + Integer.toHexString(c));
        }
        { // holesAreUnknown: no-block columns paint the empty colour
            var h = field(true);
            int[] r = BattleMapPaint.real(h, rgb);
            check((r[5 * h.width] & 0xffffff) == BattleMapPaint.EMPTY, "a column with no block is the empty colour");
        }
        { // toned (Revision 2): PIL's Color(0.5) then Brightness(0.6), alpha kept, grey stays grey, never brighter
            int[] t = BattleMapPaint.tone(new int[] {0xffff0000, 0xff808080, 0x80ffffff, 0xff000000}, BattleMapPaint.SATURATION, BattleMapPaint.BRIGHTNESS);
            // red: luma 76.245; r = (76.245 + 178.755 * .5) * .6 = 99.4, g = b = 76.245 * .5 * .6 = 22.9
            check(t[0] == 0xff631717, "pure red toned: " + Integer.toHexString(t[0]));
            check(t[1] == 0xff4d4d4d, "grey 128 is 77 grey: " + Integer.toHexString(t[1]));
            check(t[2] >>> 24 == 0x80 && (t[2] & 0xffffff) == 0x999999, "alpha kept, white is 153: " + Integer.toHexString(t[2]));
            check(t[3] == 0xff000000, "black stays black");
            int[] r = BattleMapPaint.real(g, rgb), tr = BattleMapPaint.tone(r, BattleMapPaint.SATURATION, BattleMapPaint.BRIGHTNESS);
            boolean darker = true;
            for (int i = 0; i < r.length; i++) {
                int a = r[i], b = tr[i];
                if ((b >> 16 & 255) + (b >> 8 & 255) + (b & 255) > (a >> 16 & 255) + (a >> 8 & 255) + (a & 255)) darker = false;
            }
            check(darker && tr.length == r.length, "the toned field is never brighter than the real one");
        }
        System.out.println("BattleMapPaintCheck: " + checks + " checks passed");
    }
}
