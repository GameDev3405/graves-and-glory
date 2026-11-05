package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A frame of bone offsets: for each bone [rx, ry, rz, tx, ty, tz], degrees added to the rest rotation (ZYX) and pixels added
 * to the pivot, y down, in the same rig space as {@link Clip}. Rows are kept once made and zeroed by {@link #clear}, so a
 * pose reused every frame stops allocating once it has seen its bones. Pure.
 */
public final class Pose {
    private final Map<String, double[]> index = new HashMap<>();
    private final List<String> names = new ArrayList<>();
    private final List<double[]> rows = new ArrayList<>();

    /** The bone's six channels, created at zero when absent. */
    public double[] get(String bone) {
        double[] v = index.get(bone);
        if (v == null) {
            v = new double[6];
            index.put(bone, v);
            names.add(bone);
            rows.add(v);
        }
        return v;
    }

    /** The bone's six channels, or null when the pose has no row for it (a read that does not make one). */
    double[] find(String bone) { return index.get(bone); }

    public double get(String bone, int channel) {
        double[] v = index.get(bone);
        return v == null ? 0 : v[channel];
    }

    public void add(String bone, int channel, double value) { if (value != 0) get(bone)[channel] += value; }

    /** The bones this pose has rows for, in the order they were first written. */
    public Set<String> bones() { return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(names)); }

    /** Zeroes every channel and keeps the bones, so a pose can be reused each frame without allocating. */
    public void clear() { for (int i = 0, n = rows.size(); i < n; i++) Arrays.fill(rows.get(i), 0); }

    int size() { return rows.size(); }
    String name(int i) { return names.get(i); }
    double[] row(int i) { return rows.get(i); }

    /** Adds {@code w} times every non-zero bone in {@code src}. */
    void add(Pose src, double w) {
        if (w == 0) return;
        for (int i = 0, n = src.rows.size(); i < n; i++) {
            double[] v = src.rows.get(i);
            if (v[0] == 0 && v[1] == 0 && v[2] == 0 && v[3] == 0 && v[4] == 0 && v[5] == 0) continue;
            double[] o = get(src.names.get(i));
            for (int c = 0; c < 6; c++) o[c] += w * v[c];
        }
    }

    /** A copy of every bone's channels. */
    public Map<String, double[]> snapshot() {
        Map<String, double[]> copy = new LinkedHashMap<>();
        for (int i = 0; i < rows.size(); i++) copy.put(names.get(i), rows.get(i).clone());
        return copy;
    }
}
