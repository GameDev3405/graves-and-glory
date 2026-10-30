package gg.gravesandglory;

import java.util.Random;

/** Pure motion maths for the troop motion engine (animation rework design section 6): easing, the gait and triangle waves, frame-rate-independent approach, the sub-stepped spring and per-unit seeds. Never shipped. */
public final class MotionMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static boolean near(double a, double b, double eps) { return Math.abs(a - b) <= eps; }

    public static void main(String[] args) {
        check(MotionMath.ease(0) == 0, "ease(0) " + MotionMath.ease(0));
        check(MotionMath.ease(1) == 1, "ease(1) " + MotionMath.ease(1));
        check(near(MotionMath.ease(.5), .5, 1e-12), "ease(.5) " + MotionMath.ease(.5));
        check(MotionMath.ease(-3) == 0 && MotionMath.ease(4) == 1, "ease clamps its input to [0, 1]");

        // sin(x + sin(2x)/3) tops out exactly at pi/2 (its inner phase is monotonic and fixed there); the snap is that it reaches its crest early.
        double crestAt = -1, sineCrestAt = -1, peak = -2;
        for (int i = 0; i <= 20000; i++) {
            double p = i * 2 * Math.PI / 20000, g = MotionMath.gait(p);
            check(g >= -1 && g <= 1, "gait(" + p + ") out of [-1, 1]: " + g);
            if (p <= Math.PI) peak = Math.max(peak, g);
            if (crestAt < 0 && g >= 0.95) crestAt = p;
            if (sineCrestAt < 0 && Math.sin(p) >= 0.95) sineCrestAt = p;
        }
        check(crestAt < Math.PI / 3 && crestAt < sineCrestAt - 0.25, "gait should reach its crest (0.95) well before pi/2 and before sine for a snappy footfall, reached at " + crestAt + " (sine " + sineCrestAt + ")");
        check(near(peak, 1, 1e-6), "gait should still reach 1, peaked at " + peak);

        check(near(MotionMath.tri(0), 0, 1e-12) && near(MotionMath.tri(Math.PI / 2), 1, 1e-12) && near(MotionMath.tri(3 * Math.PI / 2), -1, 1e-12), "tri should follow sine's phase: 0, 1, -1");
        for (int i = -500; i <= 500; i++) {
            double p = i * 0.037, t = MotionMath.tri(p);
            check(t >= -1 && t <= 1, "tri(" + p + ") out of [-1, 1]: " + t);
            check(near(t, MotionMath.tri(p + 2 * Math.PI), 1e-9), "tri should have period 2pi at " + p);
        }

        double slow = 0, fast = 0;
        for (int i = 0; i < 20; i++) slow = MotionMath.approach(slow, 1, 3, 1 / 20.0);
        for (int i = 0; i < 240; i++) fast = MotionMath.approach(fast, 1, 3, 1 / 240.0);
        check(near(slow, fast, 1e-3), "approach should not depend on frame rate: 20 fps " + slow + ", 240 fps " + fast);
        check(slow > 0.9 && slow < 1, "approach at rate 3 should be most of the way after 1 s: " + slow);
        check(MotionMath.approach(0, 1, 3, 5) == MotionMath.approach(0, 1, 3, 0.1), "approach should clamp dt to 0.1");
        check(MotionMath.approach(0.4, 1, 3, -1) == 0.4, "approach should clamp negative dt to 0");

        for (double[] k : new double[][]{{200, 20}, {400, 30}, {80, 6}}) {
            var s = new MotionMath.Spring(0, 0);
            double worst = 0;
            for (int i = 0; i < 20; i++) {
                s = MotionMath.spring(s, 1, k[0], k[1], 0.1);
                check(Double.isFinite(s.pos()) && Double.isFinite(s.vel()), "spring went non-finite at dt 0.1, k " + k[0]);
                worst = Math.max(worst, Math.abs(s.pos()));
            }
            check(worst < 2, "spring blew up at dt 0.1, k " + k[0] + ": " + worst);
            check(near(s.pos(), 1, 1e-2) && Math.abs(s.vel()) < 5e-2, "spring k " + k[0] + " should settle within 2 s: pos " + s.pos() + " vel " + s.vel());
        }
        var a = new MotionMath.Spring(0, 0);
        var b = new MotionMath.Spring(0, 0);
        for (int i = 0; i < 10; i++) a = MotionMath.spring(a, 1, 200, 20, 0.1);
        for (int i = 0; i < 120; i++) b = MotionMath.spring(b, 1, 200, 20, 1 / 120.0);
        check(near(a.pos(), b.pos(), 1e-6), "spring sub-stepping should match 120 Hz stepping: " + a.pos() + " vs " + b.pos());
        check(MotionMath.spring(new MotionMath.Spring(0.3, 0.2), 1, 200, 20, 3).equals(MotionMath.spring(new MotionMath.Spring(0.3, 0.2), 1, 200, 20, 0.1)), "spring should clamp dt to 0.1");

        var s1 = MotionMath.seed(0x123456789abcdefL, -42L);
        check(s1.equals(MotionMath.seed(0x123456789abcdefL, -42L)), "seed should be deterministic");
        check(!s1.equals(MotionMath.seed(0x123456789abcdefL, -41L)), "seed should differ for a different UUID");

        Random random = new Random(1029);
        int n = 1000, bins = (int) Math.ceil(2 * Math.PI / 0.1), legs = 0;
        int[] count = new int[bins];
        double tempoMin = 9, tempoMax = 0;
        for (int i = 0; i < n; i++) {
            var s = MotionMath.seed(random.nextLong(), random.nextLong());
            check(s.phase() >= 0 && s.phase() < 2 * Math.PI, "phase out of [0, 2pi): " + s.phase());
            check(s.tempo() >= 0.9 && s.tempo() <= 1.1, "tempo out of [0.9, 1.1]: " + s.tempo());
            check(s.amp() >= 0.9 && s.amp() <= 1.1, "amp out of [0.9, 1.1]: " + s.amp());
            check(s.breath() > 0.7 && s.breath() < 1.3, "breath should be a rate near 1: " + s.breath());
            check(s.idleLeg() == 0 || s.idleLeg() == 1, "idleLeg should be 0 or 1: " + s.idleLeg());
            check(s.noveltyOffset() >= 0 && Double.isFinite(s.noveltyOffset()), "noveltyOffset should be a non-negative seconds offset: " + s.noveltyOffset());
            count[(int) (s.phase() / 0.1)]++;
            legs += s.idleLeg();
            tempoMin = Math.min(tempoMin, s.tempo()); tempoMax = Math.max(tempoMax, s.tempo());
        }
        for (int i = 0; i < bins; i++) check(count[i] <= n * 3 / 100, "phase bin " + i + " holds " + count[i] + " of " + n);
        check(tempoMin < 0.92 && tempoMax > 1.08, "tempo should use its whole range: " + tempoMin + ".." + tempoMax);
        check(legs > 400 && legs < 600, "idle legs should split roughly evenly: " + legs + " of " + n);

        System.out.println("MotionMathCheck: " + checks + " checks passed");
    }
}
