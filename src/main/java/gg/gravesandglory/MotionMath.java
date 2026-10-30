package gg.gravesandglory;

/** Pure maths for the troop motion engine (animation rework design section 6): easing, gait and triangle waves, frame-rate-independent smoothing, the spring and per-unit seeds. No Minecraft types. */
public final class MotionMath {
    private MotionMath() {}

    static final double TAU = 2 * Math.PI, MAX_DT = 0.1, SPRING_STEP = 1 / 120.0;

    static double clamp01(double x) { return x < 0 ? 0 : x > 1 ? 1 : x; }
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
     * Semi-implicit Euler spring, sub-stepped at no more than 1/120 s so it stays stable at low frame rates. At step h it is
     * stable while stiffness*h^2 + 2*damping*h < 4: with h = 1/120, stiffness under about 57,600 and damping under 240.
     * A negative or NaN stiffness or damping counts as 0.
     */
    public static Spring spring(Spring s, double target, double stiffness, double damping, double dt) {
        if (!(stiffness > 0)) stiffness = 0;
        if (!(damping > 0)) damping = 0;
        dt = clampDt(dt);
        int n = Math.max(1, (int) Math.ceil(dt / SPRING_STEP - 1e-9));
        double h = dt / n, pos = s.pos(), vel = s.vel();
        for (int i = 0; i < n; i++) {
            vel += (stiffness * (target - pos) - damping * vel) * h;
            pos += vel * h;
        }
        return new Spring(pos, vel);
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
