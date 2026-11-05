package gg.gravesandglory;

import java.util.Arrays;

/**
 * R72 (Task 16 P1): the event windows of a capture's per-tick pose change and its ratio to an authored peak. Pure, no game classes,
 * so StepWindowsCheck pins them. An event fired after frame E owns frames E-dense to E+dense, cut where the previous event's frames
 * end and the next event's begin. Frame k shows only events fired after frames below k, so frame E still belongs to the event
 * before, and the event's own frames after it start at E+1.
 */
public final class StepWindows {
    private StepWindows() {}

    /** Per event (after-frames, ascending) its {lo, hi} frames, inclusive, within 1 to n-1. */
    public static int[][] windows(int[] ev, int n, int dense) {
        int[][] w = new int[ev.length][];
        for (int q = 0; q < ev.length; q++) {
            int e = ev[q], lo = Math.max(1, Math.max(e - dense, q > 0 ? w[q - 1][1] + 1 : 0)), hi = Math.min(n - 1, Math.min(e + dense, q + 1 < ev.length ? ev[q + 1] : Integer.MAX_VALUE));
            w[q] = new int[]{Math.min(lo, n - 1), Math.max(Math.min(lo, n - 1), hi)};
        }
        return w;
    }

    /** A measured step over an authored peak; -1 when there is no authored peak. */
    public static double ratio(double step, double peak) { return peak > 0 ? step / peak : -1; }

    /** Pins the cut: two events five frames apart, two fifty apart, and the states' three (event, clip end, event). */
    public static String selfCheck() {
        int[][][] want = {{{10, 25}, {26, 35}}, {{10, 30}, {60, 80}}, {{1, 20}, {21, 32}, {60, 80}}};
        int[][] evs = {{20, 25}, {20, 70}, {10, 22, 70}};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < evs.length; i++) {
            int[][] got = windows(evs[i], 120, 10);
            if (!Arrays.deepEquals(got, want[i])) throw new AssertionError("events " + Arrays.toString(evs[i]) + ": windows " + Arrays.deepToString(got) + ", want " + Arrays.deepToString(want[i]));
            sb.append(Arrays.toString(evs[i])).append(" -> ").append(Arrays.deepToString(got)).append("; ");
        }
        if (ratio(30, 0) != -1 || ratio(30, 20) != 1.5) throw new AssertionError("ratio");
        return sb.append("ratio to a missing peak -1").toString();
    }
}
