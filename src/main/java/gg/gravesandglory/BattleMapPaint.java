package gg.gravesandglory;

import java.util.Arrays;

/** The battle map's look, painted from the field grid (pure: arrays in, ARGB pixels out, no Minecraft). {@link #real} is
 *  tools/battlemap_preview.py step 2 at one texel per block; {@link #tone} then desaturates and darkens it so the markers stand out
 *  (spec 2026-11-13 section 0, Revision 2: the real look only, toned down as in work/map-declutter-mockup.py). Grid arrays are
 *  row-major, index z * w + x. A column with no block (look -1) is drawn as the preview's empty colour. */
public final class BattleMapPaint {
    private BattleMapPaint() {}

    static final int EMPTY = 0x282b33;
    /** The tone the map is drawn at: PIL's ImageEnhance.Color(0.5) then Brightness(0.6), as the approved mockup. */
    public static final float SATURATION = 0.5f, BRIGHTNESS = 0.6f;
    // ---------------------------------------------------------------- REAL look (preview step 2)

    /** One texel per block. rgb: the top colour of each palette entry (0xRRGGBB). */
    public static int[] real(BattleMapGrid g, int[] rgb) {
        int w = g.width, d = g.depth, n = w * d;
        float[] h = new float[n];
        float[] known = new float[n];
        int k = 0;
        for (int i = 0; i < n; i++) if (g.look[i] >= 0) known[k++] = g.height[i];
        float fill = k == 0 ? 0 : median(Arrays.copyOf(known, k));
        for (int i = 0; i < n; i++) h[i] = g.look[i] >= 0 ? g.height[i] : fill; // the preview's empty columns stand at the floor
        float ref = median(h.clone());
        int[] out = new int[n];
        for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
            int i = z * w + x;
            if (g.look[i] < 0) { out[i] = 0xff000000 | EMPTY; continue; }
            float hn = h[cl(z - 1, d) * w + x], hw = h[z * w + cl(x - 1, w)], hnw = h[cl(z - 1, d) * w + cl(x - 1, w)];
            float slope = clamp((h[i] - hn) + (h[i] - hw), -3, 3);
            float f = clamp(1 + 0.035f * (h[i] - ref), 0.72f, 1.28f) * (1 + 0.09f * slope);
            if (hnw - h[i] >= 2) f *= 0.84f;
            int c = rgb[g.look[i]];
            out[i] = 0xff000000 | ch((c >> 16 & 255) * f) << 16 | ch((c >> 8 & 255) * f) << 8 | ch((c & 255) * f);
        }
        return out;
    }

    static float median(float[] a) {
        Arrays.sort(a);
        int n = a.length;
        return n == 0 ? 0 : n % 2 == 1 ? a[n / 2] : (a[n / 2 - 1] + a[n / 2]) / 2;
    }


    /** Each pixel blended toward its grey (ITU-R 601 luma, as PIL's "L") by 1 - sat, then scaled by bright; alpha kept. A new array. */
    public static int[] tone(int[] argb, float sat, float bright) {
        int[] out = new int[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int c = argb[i], r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            float l = 0.299f * r + 0.587f * g + 0.114f * b;
            out[i] = (c & 0xff000000) | ch((l + (r - l) * sat) * bright + .5f) << 16 | ch((l + (g - l) * sat) * bright + .5f) << 8 | ch((l + (b - l) * sat) * bright + .5f);
        }
        return out;
    }

    static int cl(int v, int n) { return v < 0 ? 0 : v >= n ? n - 1 : v; }
    static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
    static int ch(float v) { return v <= 0 ? 0 : v >= 255 ? 255 : (int) v; }
}
