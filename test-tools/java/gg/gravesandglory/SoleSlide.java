package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * R83: the planted slide of a foot, measured at the point it stands on. A box foot that rocks from heel to toe moves its sole's centroid
 * while the edge it pivots on stays put; this tracks the corners in contact with the ground (within {@link #CONTACT} of the frame's
 * lowest corner) and takes, per step, the smallest horizontal move of a corner in contact in both frames, so a rocking foot reads as no
 * slide and a sliding foot as its true travel. Pure geometry, no game classes; {@link #selfCheck()} pins both cases.
 */
public final class SoleSlide {
    private SoleSlide() {}
    /** Blocks above a frame's lowest corner that still count as touching (about a third of a texel). */
    public static final double CONTACT = 0.02;

    /** The corners of a frame (x, height, z) within {@link #CONTACT} of its lowest one. */
    static boolean[] contact(double[][] c) {
        double lo = Double.POSITIVE_INFINITY; for (double[] p : c) lo = Math.min(lo, p[1]);
        boolean[] in = new boolean[c.length]; for (int i = 0; i < c.length; i++) in[i] = c[i][1] <= lo + CONTACT; return in;
    }

    /**
     * The horizontal slide (blocks) of a foot between two frames, given its corners in each (same order): the smallest move of a corner
     * touching in both, else of a corner touching in either, else (corner sets differ) the move of the touching corners' centroid.
     */
    public static double step(double[][] a, double[][] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) return Double.NaN;
        boolean[] ca = contact(a), cb = contact(b);
        if (a.length != b.length) { double[] p = centroid(a, ca), q = centroid(b, cb); return Math.hypot(q[0] - p[0], q[2] - p[2]); }
        double both = Double.POSITIVE_INFINITY, either = Double.POSITIVE_INFINITY;
        for (int i = 0; i < a.length; i++) {
            double d = Math.hypot(b[i][0] - a[i][0], b[i][2] - a[i][2]);
            if (ca[i] && cb[i]) both = Math.min(both, d);
            if (ca[i] || cb[i]) either = Math.min(either, d);
        }
        return both < Double.POSITIVE_INFINITY ? both : either;
    }
    static double[] centroid(double[][] c, boolean[] in) {
        double x = 0, y = 0, z = 0; int n = 0; for (int i = 0; i < c.length; i++) if (in[i]) { x += c[i][0]; y += c[i][1]; z += c[i][2]; n++; }
        return new double[]{x / n, y / n, z / n};
    }

    /**
     * One plant: a run of consecutive planted frames of one foot. {@code slide} is the summed step slide inside it, {@code slideAfterIdle}
     * the part of it on steps whose later frame is past the idle lead-in.
     */
    public record Plant(int foot, int startFrame, int frames, double slide, double slideAfterIdle) {}

    /**
     * The plants of one foot: {@code planted[k]} says frame k is planted, {@code corners[k]} its corners (null when not drawn, which
     * ends a plant), {@code afterIdle[k]} that frame k is past the idle lead-in.
     */
    public static List<Plant> plants(int foot, boolean[] planted, double[][][] corners, boolean[] afterIdle) {
        List<Plant> out = new ArrayList<>(); int n = planted.length, k = 0;
        while (k < n) {
            if (!planted[k] || corners[k] == null) { k++; continue; }
            int s = k; double sl = 0, mv = 0; k++;
            while (k < n && planted[k] && corners[k] != null) {
                double d = step(corners[k - 1], corners[k]); if (!Double.isNaN(d)) { sl += d; if (afterIdle[k]) mv += d; } k++;
            }
            out.add(new Plant(foot, s, k - s, sl, mv));
        }
        return out;
    }

    // ---- synthetic check ----
    /** The eight corners of a box foot (length along x, width along z, height up) turned by {@code pitch} radians about a pivot on its bottom, then moved by {@code dx}. */
    static double[][] foot(double len, double wid, double hi, double pivotX, double pitch, double dx) {
        double[][] c = new double[8][]; double cs = Math.cos(pitch), sn = Math.sin(pitch);
        for (int k = 0; k < 8; k++) {
            double x = ((k & 1) == 0 ? 0 : len) - pivotX, y = (k & 2) == 0 ? 0 : hi, z = (k & 4) == 0 ? 0 : wid;
            c[k] = new double[]{x * cs - y * sn + pivotX + dx, x * sn + y * cs, z};
        }
        return c;
    }
    /** A rocking plant of {@code frames} frames: heel pivot lifting the toe, back flat, toe pivot lifting the heel, back flat; sliding {@code total} blocks evenly. */
    static double[][][] rock(int frames, double maxDeg, double total, boolean rocking) {
        double len = 0.3, wid = 0.25, hi = 0.15; double[][][] out = new double[frames][][];
        for (int k = 0; k < frames; k++) {
            double t = k / (double) (frames - 1), dx = total * t, ph = (t * 2) % 1, ang = rocking ? Math.toRadians(maxDeg) * Math.sin(Math.PI * ph) : 0;
            out[k] = t * 2 < 1 ? foot(len, wid, hi, 0, ang, dx) : foot(len, wid, hi, len, -ang, dx);
        }
        return out;
    }
    static double sum(double[][][] f) { boolean[] p = new boolean[f.length], a = new boolean[f.length]; java.util.Arrays.fill(p, true); java.util.Arrays.fill(a, true); return plants(0, p, f, a).get(0).slide(); }
    static double centroidSum(double[][][] f) {
        double s = 0; for (int k = 1; k < f.length; k++) { double[] p = centroid(f[k - 1], contact(f[k - 1])), q = centroid(f[k], contact(f[k])); s += Math.hypot(q[0] - p[0], q[2] - p[2]); } return s;
    }
    /**
     * The pinned cases: a rocking foot (heel and toe pivots up to 25 degrees, no translation) slides under 0.01 block; a foot sliding 0.3
     * block, flat and rocking, reads within 10% of 0.3; a plant broken by a lifted frame is two plants. Throws when one fails; returns a line.
     */
    public static String selfCheck() {
        double rock = sum(rock(24, 25, 0, true)), cen = centroidSum(rock(24, 25, 0, true));
        if (!(rock < 0.01)) throw new AssertionError("rocking foot with no translation reads " + rock + " blocks of slide");
        double flat = sum(rock(24, 25, 0.3, false)), both = sum(rock(24, 25, 0.3, true));
        if (Math.abs(flat - 0.3) > 0.03) throw new AssertionError("flat foot sliding 0.3 reads " + flat);
        if (Math.abs(both - 0.3) > 0.03) throw new AssertionError("rocking foot sliding 0.3 reads " + both);
        double[][][] f = rock(12, 10, 0.12, false); boolean[] p = new boolean[12], a = new boolean[12];
        for (int k = 0; k < 12; k++) { p[k] = k != 5; a[k] = k >= 3; }
        var pl = plants(1, p, f, a);
        if (pl.size() != 2 || pl.get(0).startFrame() != 0 || pl.get(0).frames() != 5 || pl.get(1).startFrame() != 6 || pl.get(1).frames() != 6) throw new AssertionError("plant split wrong: " + pl);
        double step = 0.12 / 11;
        if (Math.abs(pl.get(0).slide() - 4 * step) > 1e-9 || Math.abs(pl.get(0).slideAfterIdle() - 2 * step) > 1e-9 || Math.abs(pl.get(1).slide() - 5 * step) > 1e-9) throw new AssertionError("plant slide wrong: " + pl);
        return String.format(Locale.ROOT, "rocking foot, no translation: %.4f blocks (the old sole centroid read %.4f); sliding 0.3 block: flat %.4f, rocking %.4f; a lifted frame splits a plant", rock, cen, flat, both);
    }
}
