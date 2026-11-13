package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The battle map's two looks, painted from the field grid (pure: arrays in, ARGB pixels out, no Minecraft). A straight port of
 *  tools/battlemap_preview.py steps 2 and 3 with the same constants: {@link #real} is step 2 at one texel per block, {@link #parchment}
 *  is steps 3a to 3e at {@link #S} texels per block. Deterministic: the only randomness is a cheap integer hash of world cells, so the
 *  pattern is anchored to the world and the same field always paints the same picture. Grid arrays are row-major, index z * w + x.
 *  A column with no block (look -1) is drawn as the preview's empty colour in the real look and is unknown terrain in the parchment. */
public final class BattleMapPaint {
    private BattleMapPaint() {}

    /** Parchment texels per block. */
    public static final int S = 3;
    static final int PAPER = 0xe6d3a6, INK = 0x3e2b1a, INK_MID = 0x6e5232, SEPIA = 0x9a7a52, EMPTY = 0x282b33;

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

    // ---------------------------------------------------------------- kinds (preview 3a to 3c)

    /** The resolved kind of every column and the terrain height T under it. */
    public record Field(int[] kind, float[] t, String[] names) {}

    public static Field resolve(BattleMapGrid g, MapKinds table) {
        int w = g.width, d = g.depth, n = w * d;
        String[] names = table.kinds();
        int ground = index(names, "ground"), water = index(names, "water"), building = index(names, "building");
        int[] palKind = new int[g.palette.length], palFlat = new int[g.palette.length];
        for (int p = 0; p < g.palette.length; p++) {
            palKind[p] = table.kind(g.palette[p], true);
            int f = table.kind(g.palette[p], false);
            palFlat[p] = f != palKind[p] ? f : -1;
        }
        int unknownKind = table.kind("", true);
        int[] kind = new int[n];
        for (int i = 0; i < n; i++) kind[i] = g.look[i] < 0 ? unknownKind : palKind[g.look[i]];
        boolean[] natural = naturalSet(names);
        float[] t = terrain(g, kind, natural, w, d);
        int raise = table.raise();
        for (int i = 0; i < n; i++) {
            if (g.look[i] < 0) continue;
            int f = palFlat[g.look[i]];
            if (f >= 0 && !(g.height[i] - t[i] >= raise)) kind[i] = f;
        }
        // area kinds become smooth regions; cells that leave a region fall to ground, ground cells that join it take the kind
        String[] areaKinds = {"rock", "crops", "road"};
        int[] radii = {3, 2, 1};
        float[] thr = {0.50f, 0.45f, 0.45f};
        for (int a = 0; a < 3; a++) {
            int kk = index(names, areaKinds[a]);
            float[] m = new float[n];
            for (int i = 0; i < n; i++) m[i] = kind[i] == kk ? 1 : 0;
            float[] b = boxBlur(m, w, d, radii[a]);
            boolean[] region = new boolean[n];
            for (int i = 0; i < n; i++) region[i] = b[i] >= thr[a];
            if (a == 0) { // ground holes inside a rock area read as rock
                float[] b6 = boxBlur(m, w, d, 6);
                for (int i = 0; i < n; i++) region[i] |= b6[i] >= 0.5f;
            }
            for (int i = 0; i < n; i++) if (m[i] > 0 && !region[i]) kind[i] = ground;
            for (int i = 0; i < n; i++) if (region[i] && kind[i] == ground) kind[i] = kk;
        }
        String[] partKinds = {"building", "stonework"};
        int[] smallest = {6, 4};
        for (int a = 0; a < 2; a++) {
            int kk = index(names, partKinds[a]);
            boolean[] m = new boolean[n];
            for (int i = 0; i < n; i++) m[i] = kind[i] == kk;
            boolean[] keep = bigParts(m, w, d, smallest[a]);
            for (int i = 0; i < n; i++) if (m[i] && !keep[i]) kind[i] = ground;
        }
        for (int kk : new int[] {building, water}) {
            boolean[] m = new boolean[n];
            for (int i = 0; i < n; i++) m[i] = kind[i] == kk;
            boolean[] closed = erode(dilate(m, w, d, 1), w, d, 1);
            boolean[] set = new boolean[n];
            for (int i = 0; i < n; i++) set[i] = closed[i] && !m[i] && (kk != building || kind[i] != water);
            for (int i = 0; i < n; i++) if (set[i]) kind[i] = kk;
        }
        t = terrain(g, kind, natural, w, d);
        return new Field(kind, t, names);
    }

    static boolean[] naturalSet(String[] names) {
        boolean[] s = new boolean[names.length];
        for (String k : new String[] {"ground", "rock", "crops", "road", "water", "stonework"}) { int i = index(names, k); if (i >= 0) s[i] = true; }
        return s;
    }

    /** Terrain T: heights of natural columns, the rest filled by up to 64 rounds of 4-neighbour averaging from known cells. */
    static float[] terrain(BattleMapGrid g, int[] kind, boolean[] naturalKind, int w, int d) {
        int n = w * d;
        float[] t = new float[n];
        boolean[] known = new boolean[n];
        boolean all = true;
        for (int i = 0; i < n; i++) {
            known[i] = g.look[i] >= 0 && naturalKind[kind[i]];
            t[i] = known[i] ? g.height[i] : 0;
            all &= known[i];
        }
        float[] nt = new float[n];
        boolean[] nk = new boolean[n];
        for (int it = 0; it < 64 && !all; it++) {
            all = true;
            for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
                int i = z * w + x;
                nt[i] = t[i]; nk[i] = known[i];
                if (known[i]) continue;
                float tot = 0; int cnt = 0;
                if (z > 0 && known[i - w]) { tot += t[i - w]; cnt++; }
                if (z < d - 1 && known[i + w]) { tot += t[i + w]; cnt++; }
                if (x > 0 && known[i - 1]) { tot += t[i - 1]; cnt++; }
                if (x < w - 1 && known[i + 1]) { tot += t[i + 1]; cnt++; }
                if (cnt > 0) { nt[i] = tot / cnt; nk[i] = true; } else all = false;
            }
            float[] st = t; t = nt; nt = st;
            boolean[] sk = known; known = nk; nk = sk;
        }
        return t;
    }

    /** The 8-connected parts of mask with at least smallest cells. */
    static boolean[] bigParts(boolean[] mask, int w, int d, int smallest) {
        boolean[] seen = new boolean[mask.length], keep = new boolean[mask.length];
        int[] stack = new int[mask.length];
        List<Integer> part = new ArrayList<>();
        for (int s = 0; s < mask.length; s++) {
            if (!mask[s] || seen[s]) continue;
            part.clear();
            int top = 0;
            stack[top++] = s;
            seen[s] = true;
            while (top > 0) {
                int c = stack[--top];
                part.add(c);
                int cz = c / w, cx = c % w;
                for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                    int nz = cz + dz, nx = cx + dx;
                    if (nz < 0 || nz >= d || nx < 0 || nx >= w) continue;
                    int j = nz * w + nx;
                    if (mask[j] && !seen[j]) { seen[j] = true; stack[top++] = j; }
                }
            }
            if (part.size() >= smallest) for (int c : part) keep[c] = true;
        }
        return keep;
    }

    // ---------------------------------------------------------------- PARCHMENT look (preview 3d, 3e)

    /** S texels per block, (width * S) x (depth * S) ARGB. */
    public static int[] parchment(BattleMapGrid g, MapKinds table) {
        Field f = resolve(g, table);
        int w = g.width, d = g.depth, W = w * S, H = d * S, n = W * H;
        int[] kind = f.kind();
        float[] t = f.t();
        String[] names = f.names();
        int x0 = g.minX, z0 = g.minZ;
        float[] img = paper(W, H);

        // hillshade, very faint
        float[] ts = boxBlur(boxBlur(t, w, d, 2), w, d, 2);
        float[] hs = new float[w * d];
        for (int z = 0; z < d; z++) for (int x = 0; x < w; x++)
            hs[z * w + x] = clamp(((ts[cl(z - 1, d) * w + x] - ts[cl(z + 1, d) * w + x]) + (ts[z * w + cl(x - 1, w)] - ts[z * w + cl(x + 1, w)])) * -0.25f, -1, 1);
        float[] hsp = upLinear(hs, w, d, S);
        for (int i = 0; i < n; i++) { float m = 1 + 0.06f * hsp[i]; img[3 * i] *= m; img[3 * i + 1] *= m; img[3 * i + 2] *= m; }

        // rock: wash and sparse stipple
        boolean[] rock = soft(mask(kind, index(names, "rock")), w, d, S, S);
        paint(img, rock, 0xb8a888, 0.35f);
        boolean[] stip = new boolean[n];
        for (int Z = 0; Z < H; Z++) for (int X = 0; X < W; X++) {
            int i = Z * W + X;
            stip[i] = rock[i] && X % 3 == 1 && Z % 3 == 1 && hash2(X / 3 + x0, Z / 3 + z0, 11) < 0.03;
        }
        paint(img, stip, INK_MID, 0.30f);
        paint(img, outline(rock, W, H, 1), SEPIA, 0.25f);

        // crops: faint hatching along the field's long axis
        boolean[] cropsC = mask(kind, index(names, "crops"));
        int[] runx = new int[w * d], runz = new int[w * d];
        for (int z = 0; z < d; z++) {
            for (int x = 1; x < w; x++) runx[z * w + x] = cropsC[z * w + x] ? runx[z * w + x - 1] + 1 : 0;
            for (int x = w - 2; x >= 0; x--) runx[z * w + x] = cropsC[z * w + x] ? Math.max(runx[z * w + x], runx[z * w + x + 1]) : 0;
        }
        for (int x = 0; x < w; x++) {
            for (int z = 1; z < d; z++) runz[z * w + x] = cropsC[z * w + x] ? runz[(z - 1) * w + x] + 1 : 0;
            for (int z = d - 2; z >= 0; z--) runz[z * w + x] = cropsC[z * w + x] ? Math.max(runz[z * w + x], runz[(z + 1) * w + x]) : 0;
        }
        float[] alongF = new float[w * d];
        for (int i = 0; i < w * d; i++) alongF[i] = runx[i] >= runz[i] ? 1 : 0;
        float[] alongB = boxBlur(alongF, w, d, 3);
        boolean[] along = new boolean[w * d];
        for (int i = 0; i < w * d; i++) along[i] = alongB[i] >= 0.5f;
        boolean[] crops = soft(cropsC, w, d, S, S);
        boolean[] ax = up(along, w, d, S);
        boolean[] hatch = new boolean[n];
        for (int Z = 0; Z < H; Z++) for (int X = 0; X < W; X++) {
            int i = Z * W + X;
            hatch[i] = crops[i] && (ax[i] ? Z % 4 == 0 : X % 4 == 0);
        }
        paint(img, crops, 0xd8c08a, 0.45f);
        paint(img, hatch, SEPIA, 0.45f);
        paint(img, outline(crops, W, H, 1), SEPIA, 0.30f);

        // contours every 4 blocks; every 5th darker
        float[] tsp = upLinear(boxBlur(t, w, d, 1), w, d, S);
        int[] lvl = new int[n];
        for (int i = 0; i < n; i++) lvl[i] = (int) Math.floor(tsp[i] / 4);
        boolean[] minor = new boolean[n], major = new boolean[n];
        for (int Z = 0; Z < H; Z++) for (int X = 0; X < W; X++) {
            int i = Z * W + X, e = lvl[Z * W + cl(X + 1, W)], s = lvl[cl(Z + 1, H) * W + X];
            boolean edge = lvl[i] != e || lvl[i] != s;
            major[i] = edge && Math.floorMod(Math.max(lvl[i], e), 5) == 0;
            minor[i] = edge && !major[i];
        }
        paint(img, minor, SEPIA, 0.30f);
        paint(img, major, SEPIA, 0.50f);

        // cliff hachures: where T drops 3+ to a 4-neighbour, a short tick downhill, every other cell
        int building = index(names, "building"), foliage = index(names, "foliage");
        List<int[]> ticks = new ArrayList<>();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dd : dirs) {
            int dz = dd[0], dx = dd[1];
            for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) {
                int i = z * w + x;
                if (kind[i] == building || kind[i] == foliage || (x + z) % 2 != 0) continue;
                if (t[i] - t[cl(z + dz, d) * w + cl(x + dx, w)] >= 3) ticks.add(new int[] {x, z, dx, dz});
            }
        }
        quantize(img);
        for (int[] tk : ticks) {
            double cx = tk[0] * S + S / 2.0, cz = tk[1] * S + S / 2.0;
            line(img, W, H, (int) (cx + tk[2] * S * 0.3), (int) (cz + tk[3] * S * 0.3), (int) (cx + tk[2] * S * 1.6), (int) (cz + tk[3] * S * 1.6), INK_MID, 170);
        }

        // roads
        boolean[] road = soft(mask(kind, index(names, "road")), w, d, S, S);
        paint(img, road, 0xdcc391, 0.85f);
        paint(img, outline(road, W, H, 1), INK_MID, 0.55f);

        // water: wash, two inner ripple lines, ink shore
        boolean[] waterC = mask(kind, index(names, "water"));
        boolean[] wat = soft(waterC, w, d, S, S);
        float[] wblur = boxBlur(upF(waterC, w, d, S), W, H, 2 * S);
        paint(img, wat, 0xaebcb8, 0.9f);
        for (float lv : new float[] {0.8f, 0.95f}) {
            boolean[] inner = new boolean[n];
            for (int i = 0; i < n; i++) inner[i] = wblur[i] >= lv;
            boolean[] o = outline(inner, W, H, 1);
            for (int i = 0; i < n; i++) o[i] &= wat[i];
            paint(img, o, 0x7f928f, 0.7f);
        }
        paint(img, outline(dilate(wat, W, H, 1), W, H, 1), INK, 0.85f);

        // stonework (statues, reliefs): pale shape, thin ink outline
        boolean[] stw = soft(mask(kind, index(names, "stonework")), w, d, S, 1);
        paint(img, stw, 0xe9e2cf, 0.9f);
        paint(img, outline(stw, W, H, 1), INK_MID, 0.8f);

        // buildings: shadow, fill, roof hatch, outline
        boolean[] bm = up(mask(kind, building), w, d, S);
        boolean[] sh = new boolean[n], roof = new boolean[n];
        boolean[] er = erode(bm, W, H, 2);
        for (int Z = 0; Z < H; Z++) for (int X = 0; X < W; X++) {
            int i = Z * W + X;
            sh[i] = Z >= 2 && X >= 2 && bm[i - 2 * W - 2] && !bm[i];
            roof[i] = er[i] && (X + Z) % 6 == 0;
        }
        paint(img, sh, 0x5a4024, 0.22f);
        paint(img, bm, 0xb98458, 0.92f);
        paint(img, roof, 0x8f6340, 0.25f);
        paint(img, outline(bm, W, H, 2), INK, 0.9f);
        quantize(img);

        // trees: discs on a world-anchored lattice, drawn north to south
        int step = 4;
        boolean[] fol = mask(kind, foliage);
        List<double[]> discs = new ArrayList<>();
        int lx = Math.floorDiv(x0, step), lz = Math.floorDiv(z0, step);
        for (int gz = -1; gz < d / step + 2; gz++) for (int gx = -1; gx < w / step + 2; gx++) {
            int wx = gx + lx, wz = gz + lz;
            double jx = (hash2(wx, wz, 21) - 0.5) * step * 0.8, jz = (hash2(wx, wz, 22) - 0.5) * step * 0.8;
            double cx = gx * step + step / 2.0 + jx, cz = gz * step + step / 2.0 + jz;
            int ix = (int) cx, iz = (int) cz;
            if (ix >= 0 && ix < w && iz >= 0 && iz < d && fol[iz * w + ix]) discs.add(new double[] {cz, cx, (1.7 + hash2(wx, wz, 23) * 0.9) * S});
        }
        discs.sort((a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : a[1] != b[1] ? Double.compare(a[1], b[1]) : Double.compare(a[2], b[2]));
        for (double[] c : discs) {
            double px = c[1] * S, pz = c[0] * S, r = c[2];
            ellipse(img, W, H, (int) (px - r), (int) (pz - r), (int) (px + r), (int) (pz + r), 0xb9b07a, INK_MID, 230);
            chord(img, W, H, (int) (px - r + 1.5), (int) (pz - r + 1.5), (int) (px + r - 1.5), (int) (pz + r - 1.5), 10, 120, 0x8f8a58);
        }

        // double ink border
        rect(img, W, H, 3, 3, W - 4, H - 4, 2, INK);
        rect(img, W, H, 8, 8, W - 9, H - 9, 1, INK);

        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = 0xff000000 | ch(img[3 * i]) << 16 | ch(img[3 * i + 1]) << 8 | ch(img[3 * i + 2]);
        return out;
    }

    /** Parchment base: base colour, +-3 per-pixel hash grain, 2 octaves of hashed lattice blotches, warm edge vignette. */
    static float[] paper(int W, int H) {
        float[] img = new float[3 * W * H];
        float[] blot = new float[W * H];
        int[][] oct = {{96, 7, 1}, {24, 3, 2}};
        for (int[] o : oct) {
            int cell = o[0], lw = W / cell + 2, lh = H / cell + 2;
            float[] lat = new float[lw * lh];
            for (int j = 0; j < lh; j++) for (int i = 0; i < lw; i++) lat[j * lw + i] = (float) hash2(i, j, o[2]);
            float[] u = upLinear(lat, lw, lh, cell); // (lw*cell) x (lh*cell), cropped below
            int uw = lw * cell;
            for (int Z = 0; Z < H; Z++) for (int X = 0; X < W; X++) blot[Z * W + X] += (u[Z * uw + X] - 0.5f) * 2 * o[1];
        }
        int pr = PAPER >> 16 & 255, pg = PAPER >> 8 & 255, pb = PAPER & 255;
        for (int Z = 0; Z < H; Z++) for (int X = 0; X < W; X++) {
            int i = Z * W + X;
            float v0 = blot[i] + (float) ((hash2(X, Z, 3) - 0.5) * 6);
            double ex = Math.min(X, W - 1 - X) / (0.18 * W), ez = Math.min(Z, H - 1 - Z) / (0.18 * H);
            double e = Math.max(0, Math.min(1, Math.min(ex, ez)));
            float v = (float) ((1 - e) * (1 - e));
            img[3 * i] = pr + v0 * 1.0f - v * 38;
            img[3 * i + 1] = pg + v0 * 0.95f - v * 46;
            img[3 * i + 2] = pb + v0 * 0.8f - v * 52;
        }
        return img;
    }

    // ---------------------------------------------------------------- helpers

    static int index(String[] names, String k) { for (int i = 0; i < names.length; i++) if (names[i].equals(k)) return i; return -1; }
    static int cl(int v, int n) { return v < 0 ? 0 : v >= n ? n - 1 : v; }
    static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }
    /** float to a byte channel as numpy does it: clip to 0..255, then truncate. */
    static int ch(float v) { return v <= 0 ? 0 : v >= 255 ? 255 : (int) v; }

    /** The preview's hash2: a cheap integer hash of (x, z, seed) in [0, 1), 32-bit wrap. */
    static double hash2(long x, long z, long seed) {
        long h = (x * 374761393L + z * 668265263L + seed * 2246822519L) & 0xFFFFFFFFL;
        h = ((h ^ (h >>> 13)) * 1274126177L) & 0xFFFFFFFFL;
        h = h ^ (h >>> 16);
        return (h & 0xFFFF) / 65536.0;
    }

    static boolean[] mask(int[] kind, int k) {
        boolean[] m = new boolean[kind.length];
        for (int i = 0; i < m.length; i++) m[i] = kind[i] == k;
        return m;
    }

    /** Mean over a (2r+1)^2 window, edges repeated; rows first, then columns, as the preview's axis order. */
    static float[] boxBlur(float[] a, int w, int h, int r) {
        if (r <= 0) return a.clone();
        int k = 2 * r + 1;
        float[] tmp = new float[w * h], out = new float[w * h];
        double[] sum = new double[w];
        for (int x = 0; x < w; x++) { double s = 0; for (int j = -r; j <= r; j++) s += a[cl(j, h) * w + x]; sum[x] = s; }
        for (int z = 0; z < h; z++) {
            int add = cl(z + r + 1, h) * w, sub = cl(z - r, h) * w;
            for (int x = 0; x < w; x++) { tmp[z * w + x] = (float) (sum[x] / k); sum[x] += a[add + x] - a[sub + x]; }
        }
        for (int z = 0; z < h; z++) {
            int row = z * w;
            double s = 0;
            for (int j = -r; j <= r; j++) s += tmp[row + cl(j, w)];
            for (int x = 0; x < w; x++) { out[row + x] = (float) (s / k); s += tmp[row + cl(x + r + 1, w)] - tmp[row + cl(x - r, w)]; }
        }
        return out;
    }

    static boolean[] erode(boolean[] m, int w, int h, int steps) {
        for (int s = 0; s < steps; s++) {
            boolean[] o = new boolean[m.length];
            for (int z = 0; z < h; z++) for (int x = 0; x < w; x++) {
                int i = z * w + x;
                o[i] = m[i] && m[cl(z - 1, h) * w + x] && m[cl(z + 1, h) * w + x] && m[z * w + cl(x - 1, w)] && m[z * w + cl(x + 1, w)];
            }
            m = o;
        }
        return m;
    }

    static boolean[] dilate(boolean[] m, int w, int h, int steps) {
        for (int s = 0; s < steps; s++) {
            boolean[] o = new boolean[m.length];
            for (int z = 0; z < h; z++) for (int x = 0; x < w; x++) {
                int i = z * w + x;
                o[i] = m[i] || m[cl(z - 1, h) * w + x] || m[cl(z + 1, h) * w + x] || m[z * w + cl(x - 1, w)] || m[z * w + cl(x + 1, w)];
            }
            m = o;
        }
        return m;
    }

    static boolean[] outline(boolean[] m, int w, int h, int steps) {
        boolean[] e = erode(m, w, h, steps), o = new boolean[m.length];
        for (int i = 0; i < m.length; i++) o[i] = m[i] && !e[i];
        return o;
    }

    static boolean[] up(boolean[] m, int w, int d, int s) {
        int W = w * s;
        boolean[] o = new boolean[W * d * s];
        for (int Z = 0; Z < d * s; Z++) for (int X = 0; X < W; X++) o[Z * W + X] = m[(Z / s) * w + X / s];
        return o;
    }

    static float[] upF(boolean[] m, int w, int d, int s) {
        int W = w * s;
        float[] o = new float[W * d * s];
        for (int Z = 0; Z < d * s; Z++) for (int X = 0; X < W; X++) o[Z * W + X] = m[(Z / s) * w + X / s] ? 1 : 0;
        return o;
    }

    /** Cell mask scaled up, box-blurred with radius r and thresholded at 0.5, so its edges are round. */
    static boolean[] soft(boolean[] cells, int w, int d, int s, int r) {
        float[] b = boxBlur(upF(cells, w, d, s), w * s, d * s, r);
        boolean[] o = new boolean[b.length];
        for (int i = 0; i < b.length; i++) o[i] = b[i] >= 0.5f;
        return o;
    }

    /** Bilinear scale-up by s, sample centres aligned to cell centres (the preview's up_linear, its edge clamping included). */
    static float[] upLinear(float[] a, int w, int d, int s) {
        int W = w * s, H = d * s;
        int[] x0 = new int[W], x1 = new int[W], z0 = new int[H], z1 = new int[H];
        double[] fx = new double[W], fz = new double[H];
        for (int X = 0; X < W; X++) { double v = (X + 0.5) / s - 0.5, fl = Math.floor(v); x0[X] = cl((int) fl, w); x1[X] = cl(x0[X] + 1, w); fx[X] = v - fl; }
        for (int Z = 0; Z < H; Z++) { double v = (Z + 0.5) / s - 0.5, fl = Math.floor(v); z0[Z] = cl((int) fl, d); z1[Z] = cl(z0[Z] + 1, d); fz[Z] = v - fl; }
        float[] o = new float[W * H];
        for (int Z = 0; Z < H; Z++) {
            int r0 = z0[Z] * w, r1 = z1[Z] * w;
            double f = fz[Z];
            for (int X = 0; X < W; X++) {
                double top = a[r0 + x0[X]] * (1 - fx[X]) + a[r0 + x1[X]] * fx[X];
                double bot = a[r1 + x0[X]] * (1 - fx[X]) + a[r1 + x1[X]] * fx[X];
                o[Z * W + X] = (float) (top * (1 - f) + bot * f);
            }
        }
        return o;
    }

    /** img = img * (1 - a) + a * colour where mask. */
    static void paint(float[] img, boolean[] mask, int rgb, float alpha) {
        float r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, k = 1 - alpha;
        for (int i = 0; i < mask.length; i++) {
            if (!mask[i]) continue;
            img[3 * i] = img[3 * i] * k + alpha * r;
            img[3 * i + 1] = img[3 * i + 1] * k + alpha * g;
            img[3 * i + 2] = img[3 * i + 2] * k + alpha * b;
        }
    }

    /** The preview hands the picture to PIL as bytes at two points: clip and truncate. */
    static void quantize(float[] img) { for (int i = 0; i < img.length; i++) img[i] = ch(img[i]); }

    /** PIL's blend of an RGBA ink onto an RGB pixel (DIV255 rounding). */
    static void blend(float[] img, int i, int rgb, int a) {
        int[] c = {rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255};
        for (int k = 0; k < 3; k++) {
            int tmp = (int) img[3 * i + k] * (255 - a) + c[k] * a + 128;
            img[3 * i + k] = (tmp + (tmp >> 8)) >> 8;
        }
    }

    static void set(float[] img, int W, int H, int x, int y, int rgb, int a) {
        if (x < 0 || y < 0 || x >= W || y >= H) return;
        blend(img, y * W + x, rgb, a);
    }

    /** A 1 px axis-aligned (or Bresenham) line, both ends included. */
    static void line(float[] img, int W, int H, int xa, int ya, int xb, int yb, int rgb, int a) {
        int dx = Math.abs(xb - xa), dy = -Math.abs(yb - ya), sx = xa < xb ? 1 : -1, sy = ya < yb ? 1 : -1, err = dx + dy;
        while (true) {
            set(img, W, H, xa, ya, rgb, a);
            if (xa == xb && ya == yb) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; xa += sx; }
            if (e2 <= dx) { err += dx; ya += sy; }
        }
    }

    static boolean inEllipse(double px, double py, double cx, double cy, double a, double b) {
        if (a <= 0 || b <= 0) return false;
        double u = (px - cx) / a, v = (py - cy) / b;
        return u * u + v * v <= 1;
    }

    /** PIL ellipse over the inclusive pixel box: opaque fill, then a 1 px outline blended at outlineAlpha. */
    static void ellipse(float[] img, int W, int H, int xa, int ya, int xb, int yb, int fill, int outline, int outlineAlpha) {
        double cx = (xa + xb + 1) / 2.0, cy = (ya + yb + 1) / 2.0, a = (xb - xa + 1) / 2.0, b = (yb - ya + 1) / 2.0;
        for (int y = ya; y <= yb; y++) for (int x = xa; x <= xb; x++) {
            if (!inEllipse(x + 0.5, y + 0.5, cx, cy, a, b)) continue;
            set(img, W, H, x, y, fill, 255);
            if (!inEllipse(x + 0.5, y + 0.5, cx, cy, a - 1, b - 1)) set(img, W, H, x, y, outline, outlineAlpha);
        }
    }

    /** PIL chord: the part of the ellipse cut off by the straight line between the arc's ends (degrees clockwise from east). */
    static void chord(float[] img, int W, int H, int xa, int ya, int xb, int yb, double start, double end, int fill) {
        if (xb < xa || yb < ya) return;
        double cx = (xa + xb + 1) / 2.0, cy = (ya + yb + 1) / 2.0, a = (xb - xa + 1) / 2.0, b = (yb - ya + 1) / 2.0;
        double s = Math.toRadians(start), e = Math.toRadians(end), m = (s + e) / 2;
        double p1x = cx + a * Math.cos(s), p1y = cy + b * Math.sin(s), p2x = cx + a * Math.cos(e), p2y = cy + b * Math.sin(e);
        double side = (p2x - p1x) * (cy + b * Math.sin(m) - p1y) - (p2y - p1y) * (cx + a * Math.cos(m) - p1x);
        for (int y = ya; y <= yb; y++) for (int x = xa; x <= xb; x++) {
            double px = x + 0.5, py = y + 0.5;
            if (!inEllipse(px, py, cx, cy, a, b)) continue;
            double c = (p2x - p1x) * (py - p1y) - (p2y - p1y) * (px - p1x);
            if (c * side >= 0) set(img, W, H, x, y, fill, 255);
        }
    }

    /** PIL rectangle outline of the given width, drawn inward from the inclusive box. */
    static void rect(float[] img, int W, int H, int xa, int ya, int xb, int yb, int width, int rgb) {
        for (int y = ya; y <= yb; y++) for (int x = xa; x <= xb; x++)
            if (x < xa + width || x > xb - width || y < ya + width || y > yb - width) set(img, W, H, x, y, rgb, 255);
    }
}
