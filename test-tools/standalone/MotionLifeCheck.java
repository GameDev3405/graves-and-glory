package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import static gg.gravesandglory.MotionEngineCheck.*;

/**
 * The MotionEngine's life rulings (design section 6.1): novelty idles that spare the sword arm, no clones, the per-unit
 * breathing rate and the turn-in-place step. Fixtures come from MotionEngineCheck. Never shipped.
 */
public final class MotionLifeCheck {
    public static void main(String[] args) {
        MotionEngine.Family fam = family(true), bare = family(false);
        MotionMath.Seed[] seeds = seeds(20, 6100);
        run("novelty spares the sword arm", () -> novelty(fam, seeds));
        run("no clones", () -> noClones(fam));
        run("breathing rate", () -> breathRate(fam));
        run("turn-in-place step", () -> turnStep(fam, seeds));
        if (failed > 0) {
            System.out.println("MotionLifeCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionLifeCheck: " + checks + " checks passed");
    }

    /** Novelty idles never touch the sword arm's pitch: over 90 s of calm idle the sword forearm's rx never moves. */
    static String novelty(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        int total = 0;
        for (int s = 0; s < 10; s++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
            for (int f = 0; f < 60 * 90; f++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (f > 60) { lo = Math.min(lo, out.get("right_forearm", 0)); hi = Math.max(hi, out.get("right_forearm", 0)); }
                d.step(1 / 60.0);
            }
            total += st.novelties;
            check(hi - lo < 1e-9, "seed " + s + ": a novelty idle moved the sword forearm's pitch by " + (hi - lo));
        }
        check(total >= 40, "novelty idles should play: " + total);
        return total + " novelty idles over 10 units";
    }

    static double[][] idleTrace(MotionEngine.Family fam, MotionMath.Seed seed, double seconds, String... bones) {
        Drive d = new Drive(seed);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        int frames = (int) Math.round(seconds * 60);
        double[][] trace = new double[frames][bones.length * 3];
        for (int f = 0; f < frames; f++) {
            MotionEngine.pose(d.in(), st, fam, out);
            for (int b = 0; b < bones.length; b++) for (int c = 0; c < 3; c++) trace[f][b * 3 + c] = out.get(bones[b], c);
            d.step(1 / 60.0);
        }
        return trace;
    }

    /**
     * No clones: 100 units that differ only in seed, over a 4 s idle of the body and arms. The 5th percentile of the 4,950
     * pairwise RMS distances is at least 2.5 degrees and the closest pair at least 1 degree. Distance alone misses units that
     * move in step at a small offset, so over an 8 s idle each unit's channels are detrended (least-squares line removed)
     * and the 95th percentile of the pairwise motion correlations stays below 0.4.
     */
    static String noClones(MotionEngine.Family fam) {
        MotionMath.Seed[] many = seeds(100, 9100);
        double[][][] traces = new double[many.length][][];
        for (int i = 0; i < many.length; i++) traces[i] = idleTrace(fam, many[i], 4, "body", "right_arm", "left_arm");
        List<Double> dist = new ArrayList<>();
        for (int i = 0; i < many.length; i++) for (int j = i + 1; j < many.length; j++) {
            double sum = 0;
            int n = 0;
            for (int f = 0; f < traces[i].length; f++) for (int c = 0; c < traces[i][f].length; c++) { double x = traces[i][f][c] - traces[j][f][c]; sum += x * x; n++; }
            dist.add(Math.sqrt(sum / n));
        }
        dist.sort(null);
        double least = dist.get(0), p5 = dist.get((int) (0.05 * dist.size()));
        check(p5 >= 2.5 && least >= 1, "no clones: 5th percentile " + p5 + ", closest " + least + " degrees RMS");
        double[][] motion = new double[many.length][];
        for (int i = 0; i < many.length; i++) motion[i] = detrended(idleTrace(fam, many[i], 8, "body", "right_arm", "left_arm"));
        List<Double> corr = new ArrayList<>();
        for (int i = 0; i < many.length; i++) for (int j = i + 1; j < many.length; j++) {
            double ab = 0, aa = 0, bb = 0;
            for (int k = 0; k < motion[i].length; k++) { ab += motion[i][k] * motion[j][k]; aa += motion[i][k] * motion[i][k]; bb += motion[j][k] * motion[j][k]; }
            corr.add(ab / Math.sqrt(Math.max(1e-12, aa * bb)));
        }
        corr.sort(null);
        double p95 = corr.get((int) (0.95 * corr.size())), most = corr.get(corr.size() - 1);
        check(p95 < 0.4, "no clones: units should not move in step, 95th percentile motion correlation " + p95 + " (max " + most + ")");
        return String.format("closest %.2f, 5th percentile %.2f, median %.2f deg RMS; motion correlation p95 %.3f, max %.3f",
            least, p5, dist.get(dist.size() / 2), p95, most);
    }

    /** Every channel of a trace with its least-squares line removed, laid end to end: the motion alone, mean zero per channel. */
    static double[] detrended(double[][] trace) {
        int n = trace.length, channels = trace[0].length;
        double[] motion = new double[n * channels];
        double tMean = (n - 1) / 2.0, tVar = 0;
        for (int f = 0; f < n; f++) tVar += (f - tMean) * (f - tMean);
        for (int c = 0; c < channels; c++) {
            double mean = 0, cov = 0;
            for (int f = 0; f < n; f++) mean += trace[f][c] / n;
            for (int f = 0; f < n; f++) cov += (f - tMean) * (trace[f][c] - mean);
            double slope = cov / tVar;
            for (int f = 0; f < n; f++) motion[c * n + f] = trace[f][c] - mean - slope * (f - tMean);
        }
        return motion;
    }

    /** Breathing runs at a per-unit rate: the phase advances in proportion to the seed's breath. */
    static String breathRate(MotionEngine.Family fam) {
        MotionMath.Seed[] many = seeds(8, 31);
        double lo = Double.POSITIVE_INFINITY, hi = 0, err = 0;
        for (MotionMath.Seed seed : many) {
            Drive d = new Drive(seed);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double turns = 0, prev = Double.NaN;
            for (int f = 0; f < 60 * 20; f++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (!Double.isNaN(prev)) { double step = st.breathPhase - prev; if (step < 0) step += MotionMath.TAU; turns += step; }
                prev = st.breathPhase;
                d.step(1 / 60.0);
            }
            double rate = turns / MotionMath.TAU / ((60 * 20 - 1) / 60.0);
            err = Math.max(err, Math.abs(rate / (0.23 * seed.breath()) - 1));
            lo = Math.min(lo, rate); hi = Math.max(hi, rate);
        }
        check(err < 0.01 && hi / lo > 1.1, "breathing should run at 0.23 Hz times the seed's breath: error " + err + ", spread " + hi / lo);
        return String.format("calm rates %.3f to %.3f Hz", lo, hi);
    }

    /**
     * Turning in place steps: each knee lifts in turn and the thighs yaw with the turn. The step stands each foot on its spot
     * in the world, so it runs on the ground input (as in game: TroopMotion always supplies it).
     */
    static String turnStep(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        Drive d = new Drive(seeds[4]);
        d.grounded = true;
        var st = new MotionEngine.State();
        Pose out = new Pose();
        double right = 0, left = 0, ry = 0;
        int alternations = 0;
        double lastLead = 0;
        for (int f = 0; f < 60 * 4; f++) {
            if (f == 60) d.yawRate = d.turn = 140;
            MotionEngine.pose(d.in(), st, fam, out);
            if (f > 90) {
                double r = out.get("right_shin", 0), l = out.get("left_shin", 0);
                right = Math.max(right, r); left = Math.max(left, l);
                ry = Math.max(ry, Math.abs(out.get("right_leg", 1)));
                double lead = Math.signum(r - l);
                if (lead != 0 && lead != lastLead) { if (lastLead != 0) alternations++; lastLead = lead; }
            }
            d.step(1 / 60.0);
        }
        check(right > 12 && left > 12 && ry > 3 && alternations >= 4, "turning in place should step: knees " + right + ", " + left + ", thigh yaw " + ry + ", alternations " + alternations);
        return String.format("knee lift %.1f / %.1f, thigh yaw %.1f, %d alternations", right, left, ry, alternations);
    }

}
