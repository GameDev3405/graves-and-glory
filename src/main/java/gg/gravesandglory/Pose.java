package gg.gravesandglory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A frame of bone offsets: for each bone [rx, ry, rz, tx, ty, tz], degrees added to the rest rotation (ZYX) and pixels added
 * to the pivot, y down, in the same rig space as {@link Clip}. Pure.
 */
public final class Pose {
    final Map<String, double[]> bones = new LinkedHashMap<>();

    /** The bone's six channels, created at zero when absent. */
    public double[] get(String bone) { return bones.computeIfAbsent(bone, k -> new double[6]); }

    public double get(String bone, int channel) {
        double[] v = bones.get(bone);
        return v == null ? 0 : v[channel];
    }

    public void add(String bone, int channel, double value) { if (value != 0) get(bone)[channel] += value; }

    public Set<String> bones() { return Collections.unmodifiableSet(bones.keySet()); }

    /** Zeroes every channel and keeps the bones, so a pose can be reused each frame without allocating. */
    public void clear() { for (double[] v : bones.values()) Arrays.fill(v, 0); }

    /** Adds {@code w} times every non-zero bone in {@code src}. */
    void add(Map<String, double[]> src, double w) {
        if (w == 0) return;
        for (Map.Entry<String, double[]> e : src.entrySet()) {
            double[] v = e.getValue();
            if (v[0] == 0 && v[1] == 0 && v[2] == 0 && v[3] == 0 && v[4] == 0 && v[5] == 0) continue;
            double[] o = get(e.getKey());
            for (int c = 0; c < 6; c++) o[c] += w * v[c];
        }
    }

    /** A copy of every bone's channels. */
    public Map<String, double[]> snapshot() {
        Map<String, double[]> copy = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> e : bones.entrySet()) copy.put(e.getKey(), e.getValue().clone());
        return copy;
    }
}
