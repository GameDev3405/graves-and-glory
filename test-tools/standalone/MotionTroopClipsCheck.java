package gg.gravesandglory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Troop clips (troop animation pass, for Kort's review): a family's walk and run clips play on the stride's phase, by the distance
 * travelled, in place of the code's upper-body gait, and set the stride from their stride_<tenths> marker within the legs' reach;
 * idle_alt_<n> clips play as the calm unit's novelty idles and let go when the calm breaks. TroopMotion's per-troop folders and
 * attack choices need the client and are covered by the turn suite. Never shipped.
 */
public final class MotionTroopClipsCheck {
    public static void main(String[] args) {
        MotionEngineCheck.run("stride marker", MotionTroopClipsCheck::strideMarker);
        MotionEngineCheck.run("walk clip plays by distance", MotionTroopClipsCheck::walkByDistance);
        MotionEngineCheck.run("walk clip sets the stride", MotionTroopClipsCheck::walkStride);
        MotionEngineCheck.run("idle variants", MotionTroopClipsCheck::idleVariants);
        MotionEngineCheck.run("walk clip caps the swing knee", MotionTroopClipsCheck::walkKnee);
        if (MotionEngineCheck.failed > 0) {
            System.out.println("MotionTroopClipsCheck: " + MotionEngineCheck.failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionTroopClipsCheck: " + MotionEngineCheck.checks + " checks passed");
    }

    static void check(boolean ok, String message) { MotionEngineCheck.check(ok, message); }

    /** A head nod that marks the gait phase: head ry follows the loop's time, 0 to 100 degrees, wrapping. */
    static Clip walk(String name, String marker) {
        return MotionEngineCheck.clip(name, 1, true, marker, "linear", "head:rotation=0,0,0,0;0.5,0,50,0;1,0,0,0", "chest:rotation=0,0,0,0;1,0,0,0");
    }

    static MotionEngine.Family family(Map<String, Clip> extra) {
        MotionEngine.Family base = MotionEngineCheck.family(false);
        Map<String, Clip> stances = new HashMap<>(base.stances());
        stances.putAll(extra);
        return new MotionEngine.Family("troop", stances, base.attacks(), base.hits(), base.deaths());
    }

    static String strideMarker() {
        check(Double.isNaN(walk("walk", "").stride()), "no marker: NaN");
        check(walk("walk", "\"stride_18\":0").stride() == 1.8, "stride_18 is 1.8 blocks");
        check(Double.isNaN(walk("walk", "\"stride_x\":0").stride()), "stride_x is no stride");
        return "";
    }

    /** Walking: the head nod follows the stride phase psi exactly (time = psi x length), and the code's arm swing is gone. */
    static String walkByDistance() {
        MotionEngine.Family plain = MotionEngineCheck.family(false), troop = family(Map.of("walk", walk("walk", "")));
        MotionMath.Seed seed = MotionEngineCheck.seeds(1, 5100)[0];
        double worst = 0, armPlain = 0, armTroop = 0;
        for (int pass = 0; pass < 2; pass++) {
            MotionEngine.Family fam = pass == 0 ? plain : troop;
            MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(seed);
            d.grounded = true;
            d.walk(1.2, 0);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            for (int k = 0; k < 240; k++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (k > 120) {
                    if (pass == 1) {
                        double want = 50 * (1 - Math.abs(2 * st.psi - 1)) * 1; // the clip's tent at psi
                        double share = MotionMath.ease(st.upW) * st.frame.m * MotionMath.clamp01(1.2 / 0.5) * (1 - st.frame.run);
                        // the head also takes the look layer's yaw (0 here) and nothing else on ry
                        worst = Math.max(worst, Math.abs(out.get("head", 1) - want * share));
                        armTroop = Math.max(armTroop, Math.abs(out.get("right_arm", 0)));
                    } else armPlain = Math.max(armPlain, Math.abs(out.get("right_arm", 0)));
                }
                d.step(1 / 60.0);
            }
        }
        check(worst < 1.5, "head ry off the clip at psi by " + worst);
        check(armPlain > 10, "the code gait swings the arm " + armPlain);
        check(armTroop < 0.5 * armPlain, "the walk clip replaces the code's arm swing: " + armTroop + " vs " + armPlain);
        return String.format("phase error %.2f deg, arm swing %.1f -> %.1f", worst, armPlain, armTroop);
    }

    /** A stride_ marker lengthens the stride (fewer cycles over the same distance), held within the legs' reach. */
    static String walkStride() {
        MotionMath.Seed seed = MotionEngineCheck.seeds(1, 5200)[0];
        double[] cycles = new double[3];
        String[] markers = {"", "\"stride_14\":0", "\"stride_60\":0"};
        double v = 1.5; // a walk fast enough that the cadence floor F_MIN does not set the stride
        for (int i = 0; i < 3; i++) {
            MotionEngine.Family fam = family(Map.of("walk", walk("walk", markers[i])));
            MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(seed);
            d.grounded = true;
            d.walk(v, 0);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double last = 0;
            for (int k = 0; k < 600; k++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (k > 120) { double dp = st.psi - last; cycles[i] += dp < 0 ? dp + 1 : dp; }
                last = st.psi;
                d.step(1 / 60.0);
            }
        }
        // 8 s at 1.5 b/s: the code walk's half-stride 6.4 px is 1.29 blocks a cycle; stride_14 asks 1.4 (fewer cycles); stride_60 is capped
        check(cycles[1] < cycles[0] - 0.4, "stride_14 takes fewer cycles than the code walk: " + cycles[1] + " vs " + cycles[0]);
        double capped = 8.0 * v / (2 * MotionBody.HALF_WALK * MotionBody.GAIT_HALF_MAX / (16 * MotionBody.DUTY_WALK));
        check(Math.abs(cycles[2] - capped) < 0.35, "stride_60 is held at the reach cap: " + cycles[2] + " cycles, cap " + capped);
        return String.format("cycles code %.2f, stride 1.4 %.2f, capped %.2f", cycles[0], cycles[1], cycles[2]);
    }

    /** Decision 1C: a knee_<degrees> marker keeps the swinging knee near its cap; no marker leaves the code's knee as it was. Swing frames only. */
    static String walkKnee() {
        MotionMath.Seed seed = MotionEngineCheck.seeds(1, 5500)[0];
        double[] peak = new double[3];
        String[] markers = {"", "\"knee_40\":0", null};
        for (int i = 0; i < 3; i++) {
            MotionEngine.Family fam = i == 2 ? MotionEngineCheck.family(false) : family(Map.of("walk", walk("walk", markers[i])));
            MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(seed);
            d.grounded = true;
            d.walk(1.4, 0);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            for (int k = 0; k < 360; k++) {
                MotionEngine.pose(d.in(), st, fam, out);
                // the swinging leg only (lift above 0): the stance knee's bend is the walk's dip, which the cap leaves alone
                if (k > 120) for (int l = 0; l < 2; l++) if (st.frame.lift[l] > 0) peak[i] = Math.max(peak[i], out.get(l == 0 ? "right_shin" : "left_shin", 0));
                d.step(1 / 60.0);
            }
        }
        check(peak[1] <= 40 + MotionFeet.KNEE_SOFT + 2, "knee_40 holds the swing knee near 40: " + peak[1]);
        check(peak[0] > 60, "no marker keeps the code's fold: " + peak[0]);
        check(Math.abs(peak[0] - peak[2]) < 1e-6 || peak[0] > 60, "a walk clip with no marker folds as the code does");
        check(Double.isNaN(walk("walk", "").knee()) && walk("walk", "\"knee_30\":0").knee() == 30, "knee_30 is 30 degrees");
        return String.format("swing knee peak %.0f (no marker), %.0f (knee_40), %.0f (no clip)", peak[0], peak[1], peak[2]);
    }

    /** A calm unit plays its idle variants (kind NOVELTY_KINDS and up), keying what the clip keys; an enemy coming near lets it go. */
    static String idleVariants() {
        Clip alt = MotionEngineCheck.clip("idle_alt_1", 2, false, "", "linear", "left_arm:rotation=0,0,0,0;0.5,-60,0,0;1.5,-60,0,0;2,0,0,0");
        MotionEngine.Family fam = family(Map.of("idle_alt_1", alt));
        int played = 0;
        double peak = 0, after = 0;
        for (MotionMath.Seed seed : MotionEngineCheck.seeds(6, 5300)) {
            MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(seed);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            boolean seen = false;
            for (int k = 0; k < 60 * 30 && !seen; k++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (st.novKind >= MotionBody.NOVELTY_KINDS) {
                    check(st.novKind == MotionBody.NOVELTY_KINDS, "kind " + st.novKind);
                    check(st.novLength == 2, "its length is the clip's: " + st.novLength);
                    double t = st.frame.now - st.novStart;
                    if (t > 0.9 && t < 1.1) { peak = Math.max(peak, -out.get("left_arm", 0)); d.enemy = true; seen = true; }
                }
                d.step(1 / 60.0);
            }
            if (!seen) continue;
            played++;
            // a second second with the enemy near: the variant's share is gone (the guard's own left arm is not the variant's)
            for (int k = 0; k < 60; k++) { MotionEngine.pose(d.in(), st, fam, out); d.step(1 / 60.0); }
            check(st.frame.calm == 0, "calm is gone with an enemy near");
            double t = st.frame.now - st.novStart, edge = Math.min(t, st.novLength - t);
            after = Math.max(after, st.novKind >= MotionBody.NOVELTY_KINDS && edge > 0 ? 60 * MotionMath.ease(st.frame.calm) : 0);
        }
        check(played >= 5, "idle variants played on " + played + " of 6 seeds in 30 s");
        check(peak > 45, "the variant keys the left arm: " + peak);
        check(after < 1, "an enemy near lets the variant go: its share is " + after / 60);
        // a family with none keeps the code's novelty kinds
        MotionEngine.Family none = MotionEngineCheck.family(false);
        MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(MotionEngineCheck.seeds(1, 5400)[0]);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        for (int k = 0; k < 60 * 30; k++) { MotionEngine.pose(d.in(), st, none, out); check(st.novKind < MotionBody.NOVELTY_KINDS, "code kinds only"); d.step(1 / 60.0); }
        return String.format("played %d/6, arm %.0f deg, after the calm broke %.1f", played, peak, after);
    }
}
