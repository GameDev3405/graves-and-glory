package gg.gravesandglory;

/** Pure maths for the troop motion engine (animation rework design section 6): easing, gait and triangle waves, frame-rate-independent smoothing, the spring and per-unit seeds. No Minecraft types. */
public final class MotionMath {
    private MotionMath() {}

    static final double TAU = 2 * Math.PI, MAX_DT = 0.1;

    static double clamp01(double x) { return x < 0 ? 0 : x > 1 ? 1 : x; }

    /** Degrees wrapped into [-180, 180). */
    public static double wrapDegrees(double d) { d %= 360; if (d >= 180) d -= 360; else if (d < -180) d += 360; return d; }

    /**
     * P1 (Task 16): one bone's step between two six-channel rows [rx, ry, rz (degrees), tx, ty, tz (px)]: the rotation step
     * is the sum over the three angles of |wrapDegrees(b - a)|, the translation step the largest |b - a|. MotionGrade's
     * per-bone step (maxBoneDegPerTick, authoredPeakBoneDegPerTick) is the same metric on the model's parts. Writes both into o.
     */
    public static void boneStep(double[] a, double[] b, double[] o) {
        double r = 0, p = 0;
        for (int c = 0; c < 3; c++) r += Math.abs(wrapDegrees(b[c] - a[c]));
        for (int c = 3; c < 6; c++) p = Math.max(p, Math.abs(b[c] - a[c]));
        o[0] = r; o[1] = p;
    }
    static double clampDt(double dt) { return dt > 0 ? Math.min(dt, MAX_DT) : 0; }

    /** Cosine ease, 0 to 1 over 0 to 1. */
    public static double ease(double x) { return 0.5 - 0.5 * Math.cos(Math.PI * clamp01(x)); }

    /** Distorted sine for footfalls: in [-1, 1]. It crests early (0.95 by about 0.93 rad, where sine needs 1.25) and still peaks at exactly 1 at pi/2. */
    public static double gait(double phase) { return Math.sin(phase + Math.sin(2 * phase) / 3); }

    /** Triangle wave in [-1, 1], period 2pi, in phase with sine. */
    public static double tri(double phase) {
        double t = phase / TAU;
        t -= Math.floor(t);
        return t < 0.25 ? 4 * t : t < 0.75 ? 2 - 4 * t : 4 * t - 4;
    }

    /** Exponential approach toward target; the same after one second at any frame rate. A negative or NaN rate counts as 0 (no movement). */
    public static double approach(double current, double target, double ratePerSecond, double dt) {
        if (!(ratePerSecond > 0)) return current;
        return target + (current - target) * Math.exp(-ratePerSecond * clampDt(dt));
    }

    public record Spring(double pos, double vel) {}

    /**
     * Damped spring x'' = stiffness * (target - x) - damping * x', solved exactly over dt (under-, critically and over-damped),
     * so it is unconditionally stable and gives the same motion at any frame rate. dt is clamped to 0.1 s. A negative or NaN
     * stiffness or damping counts as 0; with stiffness 0 the target is ignored and the velocity only decays.
     */
    public static Spring spring(Spring s, double target, double stiffness, double damping, double dt) {
        double[] pos = {s.pos()}, vel = {s.vel()};
        step(pos, vel, 0, target, target, stiffness, damping, dt);
        return new Spring(pos[0], vel[0]);
    }

    /** {@link #spring} in place on pos[i] and vel[i] with a constant target, without allocating. */
    static void step(double[] pos, double[] vel, int i, double target, double stiffness, double damping, double dt) {
        step(pos, vel, i, target, target, stiffness, damping, dt);
    }

    /**
     * {@link #spring} in place on pos[i] and vel[i], with the target moving linearly from {@code from} (last frame's target)
     * to {@code to} across the frame. Treating the target as continuous, not held for the whole frame, is what keeps a
     * spring chasing a moving target the same at 20 and 240 frames per second. Does not allocate.
     */
    static void step(double[] pos, double[] vel, int i, double from, double to, double stiffness, double damping, double dt) {
        double k = stiffness > 1e-9 ? stiffness : 0, d = damping > 0 ? damping : 0;
        dt = clampDt(dt);
        if (dt == 0) return;
        double x = pos[i], v = vel[i], slope = 0, base = 0, end = 0;
        if (k > 0) {
            slope = Double.isFinite(from) ? (to - from) / dt : 0;
            base = (Double.isFinite(from) ? from : to) - d * slope / k;
            end = to - d * slope / k;
        }
        double e = x - base, ev = v - slope, a = -d / 2, q = k - d * d / 4, c, sn;
        if (q > 0) {
            double w = Math.sqrt(q);
            c = Math.cos(w * dt);
            sn = Math.sin(w * dt) / w;
        } else if (q < 0) {
            double b = Math.sqrt(-q);
            c = Math.cosh(b * dt);
            sn = Math.sinh(b * dt) / b;
        } else {
            c = 1;
            sn = dt;
        }
        double decay = Math.exp(a * dt), bb = ev - a * e;
        double e1 = decay * (e * c + bb * sn), ev1 = decay * (ev * c + (a * bb - q * e) * sn);
        pos[i] = (k > 0 ? end : 0) + e1;
        vel[i] = slope + ev1;
    }

    /**
     * Per-unit identity from the entity UUID.
     * phase: cyclic-layer offset in [0, 2pi). tempo, amp: scales in [0.9, 1.1].
     * breath: breathing-rate multiplier in [0.85, 1.15]. idleLeg: preferred weight leg, 0 or 1.
     * noveltyOffset: seconds offset for novelty idle timing, in [0, 12). variantBias: raw bits for attack-variant picks.
     */
    public record Seed(double phase, double tempo, double amp, double breath, int idleLeg, double noveltyOffset, long variantBias) {}

    public static Seed seed(long uuidMost, long uuidLeast) {
        long h = mix(uuidMost ^ mix(uuidLeast + 0x9E3779B97F4A7C15L));
        long a = mix(h + 0x9E3779B97F4A7C15L), b = mix(h + 2 * 0x9E3779B97F4A7C15L), c = mix(h + 3 * 0x9E3779B97F4A7C15L);
        long d = mix(h + 4 * 0x9E3779B97F4A7C15L), e = mix(h + 5 * 0x9E3779B97F4A7C15L), f = mix(h + 6 * 0x9E3779B97F4A7C15L);
        return new Seed(Math.min(unit(a) * TAU, Math.nextDown(TAU)), 0.9 + 0.2 * unit(b), 0.9 + 0.2 * unit(c),
                0.85 + 0.3 * unit(d), (int) (e >>> 63), 12 * unit(f), mix(h ^ 0xD1B54A32D192ED03L));
    }

    /** splitmix64 finaliser. */
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Top 53 bits as a double in [0, 1). */
    static double unit(long bits) { return (bits >>> 11) * 0x1.0p-53; }
}
