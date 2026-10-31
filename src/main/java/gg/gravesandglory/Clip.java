package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A hand-keyed clip (animation rework design section 6.3), converted from Blockbench by tools/clips.py. Channels are rig
 * space: degrees added to a bone's rest rotation (ZYX), pixels added to its pivot, y down. Sampling replicates Blockbench
 * 5.2.2's BoneAnimator.interpolate and Keyframe.getBezierLerp / getCatmullromLerp, so a clip plays as it does in the editor;
 * tools/clips.py samples the same way. Pure.
 */
public final class Clip {
    public static final int TICKS = 20;
    static final double EPSILON = 1 / 1200.0;
    static final int LINEAR = 0, CATMULLROM = 1, BEZIER = 2, STEP = 3;
    static final List<String> INTERPOLATIONS = List.of("linear", "catmullrom", "bezier", "step");

    record Key(double t, int i, double[] v, double[] post, boolean split, double[] lt, double[] lv, double[] rt, double[] rv) {}

    private final String name;
    private final double length;
    private final boolean loop;
    private final int impact;
    private final Map<String, Double> markers;
    private final String[] bones;
    private final Set<String> boneSet;
    private final Key[][] rotation, position;
    private final double[] reach;
    /** Each keyed bone's own envelope (envelope), in bones' order. */
    private final double[][] boneReach;

    private Clip(String name, double length, boolean loop, Map<String, Double> markers, String[] bones, Key[][] rotation, Key[][] position) {
        this.name = name; this.length = length; this.loop = loop; this.markers = Collections.unmodifiableMap(markers);
        this.bones = bones; this.rotation = rotation; this.position = position; this.boneSet = Set.of(bones);
        Double at = markers.get("impact");
        if (at == null) impact = -1;
        else {
            double ticks = at * TICKS;
            if (Math.abs(ticks - Math.rint(ticks)) > 1e-3) throw new IllegalArgumentException(name + ": impact marker at " + at + " s is not on a tick");
            impact = (int) Math.rint(ticks);
        }
        boneReach = new double[bones.length][];
        reach = new double[6];
        for (int b = 0; b < bones.length; b++) {
            boneReach[b] = envelope(rotation[b], position[b]);
            for (int c = 0; c < 6; c++) reach[c] = Math.max(reach[c], boneReach[b][c]);
        }
    }

    public static Clip parse(String json) { return parse(JsonParser.parseString(json).getAsJsonObject()); }

    /** A clip from tools/clips.py's JSON (format 1); throws IllegalArgumentException when it is not one. */
    public static Clip parse(JsonObject json) {
        String name = json.has("clip") ? json.get("clip").getAsString() : "?";
        if (!json.has("format") || json.get("format").getAsInt() != 1) throw new IllegalArgumentException(name + ": clip format " + json.get("format") + ", expected 1");
        double length = json.get("length").getAsDouble();
        if (!(length > 0) || Double.isInfinite(length)) throw new IllegalArgumentException(name + ": length " + length);
        Map<String, Double> markers = new LinkedHashMap<>();
        if (json.has("markers")) for (var e : json.getAsJsonObject("markers").entrySet()) markers.put(e.getKey(), e.getValue().getAsDouble());
        JsonObject bonesJson = json.has("bones") ? json.getAsJsonObject("bones") : new JsonObject();
        List<String> names = new ArrayList<>(bonesJson.keySet());
        Key[][] rot = new Key[names.size()][], pos = new Key[names.size()][];
        for (int b = 0; b < names.size(); b++) {
            JsonObject chans = bonesJson.getAsJsonObject(names.get(b));
            for (String ch : chans.keySet()) if (!ch.equals("rotation") && !ch.equals("position")) throw new IllegalArgumentException(name + ": " + names.get(b) + " channel " + ch);
            rot[b] = keys(chans.getAsJsonArray("rotation"), name);
            pos[b] = keys(chans.getAsJsonArray("position"), name);
        }
        return new Clip(name, length, json.has("loop") && json.get("loop").getAsBoolean(), markers, names.toArray(new String[0]), rot, pos);
    }

    private static Key[] keys(JsonArray array, String clip) {
        if (array == null) return new Key[0];
        Key[] keys = new Key[array.size()];
        for (int n = 0; n < keys.length; n++) {
            JsonObject k = array.get(n).getAsJsonObject();
            int i = INTERPOLATIONS.indexOf(k.get("i").getAsString());
            if (i < 0) throw new IllegalArgumentException(clip + ": interpolation " + k.get("i"));
            double[] v = triple(k.get("v"), null);
            keys[n] = new Key(k.get("t").getAsDouble(), i, v, triple(k.get("post"), v), k.has("post"),
                triple(k.get("lt"), new double[]{-.1, -.1, -.1}), triple(k.get("lv"), new double[3]), triple(k.get("rt"), new double[]{.1, .1, .1}), triple(k.get("rv"), new double[3]));
        }
        Arrays.sort(keys, Comparator.comparingDouble(Key::t));   // stable, as Blockbench's sort is
        return keys;
    }

    private static double[] triple(JsonElement e, double[] fallback) {
        if (e == null) { if (fallback == null) throw new IllegalArgumentException("a key without a value"); return fallback; }
        JsonArray a = e.getAsJsonArray();
        if (a.size() != 3) throw new IllegalArgumentException("expected [x, y, z], got " + a);
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    public String name() { return name; }
    public double length() { return length; }
    public boolean loop() { return loop; }
    /** The impact marker in ticks from the clip's start, or -1 when the clip has none. */
    public int impact() { return impact; }
    public Map<String, Double> markers() { return markers; }
    public Set<String> bones() { return boneSet; }

    /** Writes [rx, ry, rz, tx, ty, tz] for every bone the clip keys (others are left alone). A looping clip wraps the time; any other holds its ends. */
    public void sample(double seconds, Map<String, double[]> out) {
        double t = Double.isFinite(seconds) ? seconds : 0;
        if (loop) { t %= length; if (t < 0) t += length; }
        else t = Math.min(Math.max(t, 0), length);
        for (int b = 0; b < bones.length; b++) {
            double[] o = out.computeIfAbsent(bones[b], k -> new double[6]);
            channel(rotation[b], loop, t, o, 0);
            channel(position[b], loop, t, o, 3);
        }
    }

    /**
     * Adds {@code w} times this clip at {@code seconds} into {@code out}, bone by bone, using {@code tmp} (at least six
     * doubles) as scratch. Times as {@link #sample}; allocates only the first time {@code out} meets a bone.
     */
    public void add(double seconds, Pose out, double w, double[] tmp) {
        if (w == 0) return;
        double t = Double.isFinite(seconds) ? seconds : 0;
        if (loop) { t %= length; if (t < 0) t += length; }
        else t = Math.min(Math.max(t, 0), length);
        for (int b = 0; b < bones.length; b++) {
            channel(rotation[b], loop, t, tmp, 0);
            channel(position[b], loop, t, tmp, 3);
            double[] o = out.get(bones[b]);
            for (int c = 0; c < 6; c++) o[c] += w * tmp[c];
        }
    }

    /**
     * The largest |value| each of the six channels can reach on any bone at any time: the clip's envelope for culling.
     * Computed once when the clip is built (final, so safely published) and exact rather than sampled, so a short bezier or
     * catmullrom overshoot between samples is never missed.
     */
    public double[] reach() { return reach.clone(); }

    /** As {@link #reach()}, for one bone: its own six channels' envelope, all zero for a bone the clip does not key. */
    public double[] reach(String bone) {
        for (int b = 0; b < bones.length; b++) if (bones[b].equals(bone)) return boneReach[b].clone();
        return new double[6];
    }

    /**
     * Every sampled value is a key's v or post (held, stepped or past either end) or lies on one segment between two
     * consecutive key times, chosen exactly as {@link #channel} chooses it. A linear segment stays between its ends; a bezier
     * segment is a lerp between two of its 201 curve points, so its bound is the largest point; a catmullrom segment is one
     * Hermite cubic in alpha, bounded by its ends and the real roots of its derivative in [0, 1].
     */
    private double[] envelope(Key[] rot, Key[] pos) {
        double[] r = new double[6];
        for (int at : new int[]{0, 3}) { Key[] keys = at == 0 ? rot : pos;
            for (Key k : keys) for (int a = 0; a < 3; a++) r[at + a] = Math.max(r[at + a], Math.max(Math.abs(k.v()[a]), Math.abs(k.post()[a])));
            for (int bi = 0; bi < keys.length; bi++) {
                if (bi > 0 && keys[bi - 1].t() == keys[bi].t()) continue;   // channel() takes the first key of an equal-time group
                int ai = bi + 1;
                while (ai < keys.length && keys[ai].t() == keys[bi].t()) ai++;
                if (ai >= keys.length) break;
                Key before = keys[bi], after = keys[ai];
                if (before.i() == STEP || (before.i() == LINEAR && (after.i() == LINEAR || after.i() == STEP))) continue;
                for (int a = 0; a < 3; a++) {
                    double m;
                    if (before.i() == CATMULLROM || after.i() == CATMULLROM) {
                        Key[] around = around(keys, loop, bi);
                        m = hermiteReach(hermite(around[0], before, after, around[1], a, 0.5));
                    } else m = bezierReach(before, after, a);
                    r[at + a] = Math.max(r[at + a], m);
                }
            }
        }
        return r;
    }

    private static double hermiteReach(double[] h) {
        double a = h[0], b = h[1], c = h[2], d = h[3];
        double m = Math.max(Math.abs(d), Math.abs(a + b + c + d));
        double qa = 3 * a, qb = 2 * b;
        if (Math.abs(qa) < 1e-12) {
            if (Math.abs(qb) > 1e-12) m = Math.max(m, cubicAt(h, -c / qb));
        } else {
            double disc = qb * qb - 4 * qa * c;
            if (disc >= 0) {
                double root = Math.sqrt(disc);
                m = Math.max(m, Math.max(cubicAt(h, (-qb + root) / (2 * qa)), cubicAt(h, (-qb - root) / (2 * qa))));
            }
        }
        return m;
    }

    /** |a w^3 + b w^2 + c w + d| at w, or 0 outside [0, 1]. */
    private static double cubicAt(double[] h, double w) {
        if (!(w >= 0 && w <= 1)) return 0;
        return Math.abs(((h[0] * w + h[1]) * w + h[2]) * w + h[3]);
    }

    private static double bezierReach(Key before, Key after, int axis) {
        double vb = before.post()[axis], va = after.v()[axis];
        double y0 = vb, y1 = vb + before.rv()[axis], y2 = va + after.lv()[axis], y3 = va, m = 0;
        for (int d = 0; d <= 200; d++) m = Math.max(m, Math.abs(cubic(d / 200.0, y0, y1, y2, y3)));
        return m;
    }

    /** BoneAnimator.interpolate for one channel. */
    static void channel(Key[] keys, boolean loop, double time, double[] o, int at) {
        if (keys.length == 0) { o[at] = o[at + 1] = o[at + 2] = 0; return; }
        int bi = -1, ai = -1;
        for (int n = 0; n < keys.length; n++) {
            double kt = keys[n].t();
            if (kt < time) { if (bi < 0 || kt > keys[bi].t()) bi = n; }
            else if (ai < 0 || kt < keys[ai].t()) ai = n;
        }
        Key before = bi < 0 ? null : keys[bi], after = ai < 0 ? null : keys[ai];
        double[] held = null;
        if (before != null && Math.abs(time - before.t()) < EPSILON) held = before.v();
        else if (after != null && Math.abs(after.t() - time) < EPSILON) held = after.v();
        else if (before != null && (before.i() == STEP || after == null)) held = before.post();
        else if (before == null) held = after.v();
        if (held != null) { System.arraycopy(held, 0, o, at, 3); return; }
        double alpha = (time - before.t()) / (after.t() - before.t());
        if (before.i() == LINEAR && (after.i() == LINEAR || after.i() == STEP)) {
            for (int a = 0; a < 3; a++) o[at + a] = before.post()[a] + (after.v()[a] - before.post()[a]) * alpha;
        } else if (before.i() == CATMULLROM || after.i() == CATMULLROM) {
            Key bp = bi >= 1 ? keys[bi - 1] : null, ap = bi + 2 < keys.length ? keys[bi + 2] : null;   // as around(), without the array
            if (loop && keys.length >= 3) {
                if (bp == null) bp = keys[keys.length - 2];
                if (ap == null) ap = keys[1];
            }
            for (int a = 0; a < 3; a++) o[at + a] = catmullrom(bp, before, after, ap, a, alpha);
        } else {
            for (int a = 0; a < 3; a++) o[at + a] = bezier(before, after, a, alpha);
        }
    }

    /** The catmullrom neighbours Blockbench takes for the segment that starts at keys[bi]: the keys either side, wrapping when the clip loops. */
    static Key[] around(Key[] keys, boolean loop, int bi) {
        Key bp = bi >= 1 ? keys[bi - 1] : null, ap = bi + 2 < keys.length ? keys[bi + 2] : null;
        if (loop && keys.length >= 3) {
            if (bp == null) bp = keys[keys.length - 2];
            if (ap == null) ap = keys[1];
        }
        return new Key[]{bp, ap};
    }

    /** Keyframe.getCatmullromLerp on three.js SplineCurve.getPoint, term for term. */
    static double catmullrom(Key bp, Key before, Key after, Key ap, int axis, double alpha) {
        double s0 = 0, s1 = 0, s2 = 0, s3 = 0;   // hermite()'s points without its arrays; the same arithmetic
        int n = 0;
        if (bp != null && !before.split()) { s0 = bp.post()[axis]; n++; }
        double v = before.post()[axis];
        if (n == 0) s0 = v; else s1 = v;
        n++;
        v = after.v()[axis];
        if (n == 1) s1 = v; else s2 = v;
        n++;
        if (ap != null && !after.split()) { v = ap.v()[axis]; if (n == 2) s2 = v; else s3 = v; n++; }
        double t = (alpha + (bp != null ? 1 : 0)) / (n - 1), p = (n - 1) * t;
        int i = (int) Math.floor(p);
        double w = p - i;
        double p0 = pick(i == 0 ? i : i - 1, s0, s1, s2, s3), p1 = pick(i, s0, s1, s2, s3);
        double p2 = pick(i > n - 2 ? n - 1 : i + 1, s0, s1, s2, s3), p3 = pick(i > n - 3 ? n - 1 : i + 2, s0, s1, s2, s3);
        double v0 = (p2 - p0) * 0.5, v1 = (p3 - p1) * 0.5;
        double a = 2 * p1 - 2 * p2 + v0 + v1, b = -3 * p1 + 3 * p2 - 2 * v0 - v1;
        double t2 = w * w, t3 = w * t2;
        return a * t3 + b * t2 + v0 * w + p1;
    }

    private static double pick(int i, double s0, double s1, double s2, double s3) {
        return i <= 0 ? s0 : i == 1 ? s1 : i == 2 ? s2 : s3;
    }

    /**
     * The segment's Hermite cubic as {a, b, c, d, w}: the value is a w^3 + b w^2 + c w + d. Inside one segment the spline index
     * i is fixed (1 with a leading neighbour, else 0), so w equals alpha and the cubic covers the whole segment.
     */
    static double[] hermite(Key bp, Key before, Key after, Key ap, int axis, double alpha) {
        double[] pts = new double[4];
        int n = 0;
        if (bp != null && !before.split()) pts[n++] = bp.post()[axis];
        pts[n++] = before.post()[axis];
        pts[n++] = after.v()[axis];
        if (ap != null && !after.split()) pts[n++] = ap.v()[axis];
        double t = (alpha + (bp != null ? 1 : 0)) / (n - 1), p = (n - 1) * t;
        int i = (int) Math.floor(p);
        double w = p - i;
        double p0 = pts[i == 0 ? i : i - 1], p1 = pts[i], p2 = pts[i > n - 2 ? n - 1 : i + 1], p3 = pts[i > n - 3 ? n - 1 : i + 2];
        double v0 = (p2 - p0) * 0.5, v1 = (p3 - p1) * 0.5;
        return new double[]{2 * p1 - 2 * p2 + v0 + v1, -3 * p1 + 3 * p2 - 2 * v0 - v1, v0, p1, w};
    }

    /**
     * Keyframe.getBezierLerp: Blockbench lays the curve out as 201 points (three.js getPoints(200)) and lerps between the two
     * nearest the time. The clamped handle times keep the points' x rising with their index, so a binary search finds where
     * the time falls and only the points around it are compared, in index order, with the same strict tests.
     */
    static double bezier(Key before, Key after, int axis, double alpha) {
        double vb = before.post()[axis], va = after.v()[axis], gap = after.t() - before.t();
        double hb = clamp(before.rt()[axis], 0, gap), ha = clamp(after.lt()[axis], -gap, 0);
        double x0 = before.t(), x1 = before.t() + hb, x2 = after.t() + ha, x3 = after.t();
        double y0 = vb, y1 = vb + before.rv()[axis], y2 = va + after.lv()[axis], y3 = va;
        double time = before.t() + (after.t() - before.t()) * alpha;
        int lo = 0, hi = 200;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cubic(mid / 200.0, x0, x1, x2, x3) < time) lo = mid + 1; else hi = mid;
        }
        int from = Math.max(0, lo - 4), to = Math.min(200, lo + 3), c = -1, s = -1;
        double cd = Double.POSITIVE_INFINITY, sd = Double.POSITIVE_INFINITY;
        for (int d = from; d <= to; d++) {
            double diff = Math.abs(cubic(d / 200.0, x0, x1, x2, x3) - time);
            if (diff < cd) { cd = diff; c = d; }
        }
        for (int d = from; d <= to; d++) {
            if (d == c) continue;
            double diff = Math.abs(cubic(d / 200.0, x0, x1, x2, x3) - time);
            if (diff < sd) { sd = diff; s = d; }
        }
        double cx = cubic(c / 200.0, x0, x1, x2, x3), cy = cubic(c / 200.0, y0, y1, y2, y3);
        double sx = cubic(s / 200.0, x0, x1, x2, x3), sy = cubic(s / 200.0, y0, y1, y2, y3);
        return cy + (sy - cy) * clamp((time - cx) / (sx - cx), 0, 1);
    }

    /** three.js CubicBezier. */
    static double cubic(double t, double p0, double p1, double p2, double p3) {
        double k = 1 - t;
        return k * k * k * p0 + 3 * k * k * t * p1 + 3 * (1 - t) * t * t * p2 + t * t * t * p3;
    }

    /** Math.clamp: NaN goes to the minimum. */
    static double clamp(double v, double min, double max) {
        if (v > max) v = max;
        if (v < min || Double.isNaN(v)) v = min;
        return v;
    }
}
