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
 * The MotionEngine's secondary motion (design section 6.1): the tabard follows the forward leg and rests straight, loose
 * straps swing and settle, and their whip is counter-motion, not a pop. Fixtures come from MotionEngineCheck. Never shipped.
 */
public final class MotionClothCheck {
    public static void main(String[] args) {
        MotionEngine.Family fam = family(true), bare = family(false);
        MotionMath.Seed[] seeds = seeds(20, 6100);
        run("smooth min", MotionClothCheck::smoothMin);
        run("tabard follow", () -> tabard(fam, seeds));
        run("tabard rest", () -> tabardRest(fam, seeds));
        run("loose straps", () -> straps(fam, seeds));
        run("strap whip", () -> strapWhip(fam, seeds));
        if (failed > 0) {
            System.out.println("MotionClothCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionClothCheck: " + checks + " checks passed");
    }

    /**
     * The tabard's smooth min has no bias: it is exactly min(a, b) where a == b and where they are SOFT_MIN or more apart,
     * never below it and at most 27/512 of SOFT_MIN above it between; its first and second derivatives are continuous
     * (sampled across both seams, |a - b| = 0 and SOFT_MIN).
     */
    static String smoothMin() {
        double k = MotionBody.SOFT_MIN, cap = 27.0 / 512 * k, worst = 0, d1 = 0, d2 = 0;
        Random r = new Random(13);
        for (int i = 0; i < 20000; i++) {
            double a = (r.nextDouble() - 0.5) * 80, b = i % 4 == 0 ? a : i % 4 == 1 ? a + (r.nextBoolean() ? k : -k) * (1 + r.nextDouble()) : (r.nextDouble() - 0.5) * 80;
            double m = Math.min(a, b), v = MotionBody.softMin(a, b), over = v - m;
            if (i % 4 < 2) check(Math.abs(over) < 1e-9, "the smooth min should be exact at a == b and " + k + " apart: " + a + ", " + b + " gives " + v);
            check(over >= -1e-9 && over <= cap + 1e-9, "the smooth min should lie in [min, min + " + cap + "]: " + a + ", " + b + " gives " + over + " over");
            check(MotionBody.softMin(a, b) == MotionBody.softMin(b, a), "the smooth min should be symmetric");
            worst = Math.max(worst, over);
        }
        double e = 1e-4;
        for (double seam : new double[]{0, k, -k}) {
            double[] g = new double[2], c = new double[2];
            for (int side = 0; side < 2; side++) {
                double x = seam + (side == 0 ? -e : e);
                g[side] = (MotionBody.softMin(x + e, 0) - MotionBody.softMin(x - e, 0)) / (2 * e);
                c[side] = (MotionBody.softMin(x + e, 0) - 2 * MotionBody.softMin(x, 0) + MotionBody.softMin(x - e, 0)) / (e * e);
            }
            d1 = Math.max(d1, Math.abs(g[1] - g[0]));
            d2 = Math.max(d2, Math.abs(c[1] - c[0]));
        }
        check(d1 < 1e-3, "the smooth min's slope should be continuous at its seams: jump " + d1);
        check(d2 < 0.05, "the smooth min's curvature should be continuous at its seams: jump " + d2);
        return String.format("worst %.4f deg over the min (cap %.4f), slope jump %.1e, curvature jump %.1e at the seams", worst, cap, d1, d2);
    }

    /**
     * A standing unit's tabard has no bias (Task 12's soft min pushed it 6.8 degrees forward of the forward thigh's
     * TABARD_FOLLOW share even with straight legs): with straight legs it rests exactly straight, and over 4 s idle, 8
     * seeds, it stays within the smooth min's rounding (TABARD_FOLLOW x 27/512 x SOFT_MIN, 0.44 deg, where the thighs are
     * 3 degrees apart) plus 0.5 degrees of spring lag of that share of the forward thigh, min(right_leg, left_leg, 0). The
     * standing thighs themselves lean 6 to 11 degrees forward (the planted feet's soft knee plus the idle weight shift),
     * so the tabard rests that share forward, over the thighs.
     */
    static String tabardRest(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worst = 0, lo = 1e9, hi = -1e9, oldLo = 1e9, oldHi = -1e9;
        String where = "none";
        for (int s = 0; s < 8; s++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            for (int f = 0; f < 60 * 6; f++) {
                MotionEngine.pose(d.in(), st, fam, out);
                double r = out.get("right_leg", 0), l = out.get("left_leg", 0), tab = out.get("tabard", 0);
                double share = MotionBody.TABARD_FOLLOW * Math.min(0, Math.min(r, l)), off = Math.abs(tab - share);
                double old = MotionBody.TABARD_FOLLOW * biasedSoftMin(biasedSoftMin(r, l), 0);
                if (f >= 120) {
                    if (off > worst) { worst = off; where = String.format("seed %d frame %d (tabard %.2f, thighs %.2f / %.2f)", s, f, tab, r, l); }
                    lo = Math.min(lo, tab); hi = Math.max(hi, tab); oldLo = Math.min(oldLo, old); oldHi = Math.max(oldHi, old);
                }
                d.step(1 / 60.0);
            }
        }
        double bar = MotionBody.TABARD_FOLLOW * 27.0 / 512 * MotionBody.SOFT_MIN + 0.5;
        check(worst <= bar, "a standing unit's tabard should follow its forward thigh without bias: off by " + worst + " deg at " + where + " (bar " + bar + ")");
        check(Math.abs(MotionBody.softMin(MotionBody.softMin(0, 0), 0)) < 1e-12, "straight legs should rest the tabard straight");
        return String.format("worst %.3f deg (bar %.2f) off the thigh's share at %s; resting %.2f to %.2f deg (Task 12's soft min: %.2f to %.2f)", worst, bar, where, lo, hi, oldLo, oldHi);
    }

    /** Task 12's soft min, which sat SOFT_MIN / 2 below min(a, b) where they met: kept here only to report the change. */
    static double biasedSoftMin(double a, double b) {
        double h = a - b;
        return 0.5 * (a + b - Math.sqrt(h * h + MotionBody.SOFT_MIN * MotionBody.SOFT_MIN));
    }

    /**
     * The straps' whip is counter-motion, not a pop (Task 12 carry A5): MotionLayersCheck's speed 1.5 schedule sets its
     * pop margin on a strap (0.832 of the 12 degree bar), because a strap's local channel is STRAP_GAIN times its lag
     * behind the hips, so when an attack or hit swings the hips 7 degrees in a frame the strap's local channel steps
     * about -1.2 times that. Hanging off the hips, the strap's world angle (local plus its parent body's) steps far less:
     * it must stay within half the bar (6 degrees a frame) on the schedule at speeds 1.0 and 1.5, and every local step
     * over 6 degrees must be a counter-step (against the hips).
     */
    static String strapWhip(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worstLocal = 0, worstWorld = 0;
        String whereLocal = "none", whereWorld = "none";
        for (double speed : new double[]{1.0, 1.5}) {
            for (int s = 0; s < 3; s++) {
                Drive d = new Drive(seeds[s]);
                var st = new MotionEngine.State();
                Pose out = new Pose();
                double e = 0, prevParent = Double.NaN;
                double[] prev = {Double.NaN, Double.NaN};
                String[] straps = {"right_strap", "left_strap"};
                while (e < 32) {
                    double dt = schedule(d, e, speed);
                    MotionEngine.pose(d.in(), st, fam, out);
                    double parent = out.get("body", 0) + out.get("root", 0), scale = dt > 1 / 59.0 ? dt * 60 : 1;
                    for (int k = 0; k < 2; k++) {
                        double local = out.get(straps[k], 0);
                        if (!Double.isNaN(prev[k])) {
                            double dl = (local - prev[k]) / scale, dp = (parent - prevParent) / scale, dw = dl + dp;
                            String at = String.format("%s speed %.1f seed %d t %.3f (local %.2f, body %.2f)", straps[k], speed, s, e, dl, dp);
                            if (Math.abs(dl) > worstLocal) { worstLocal = Math.abs(dl); whereLocal = at; }
                            if (Math.abs(dw) > worstWorld) { worstWorld = Math.abs(dw); whereWorld = at; }
                            check(Math.abs(dl) <= 6 || dl * dp < 0, "a strap's big local step should be counter-motion against the hips: " + at);
                        }
                        prev[k] = local;
                    }
                    prevParent = parent;
                    double next = at(e, 23.5) ? 0.1 : 1 / 60.0;
                    d.step(next);
                    e += next;
                }
            }
        }
        check(worstWorld <= 6, "a strap's world angle should step at most 6 degrees a frame: " + worstWorld + " at " + whereWorld);
        return String.format("worst local step %.2f deg/frame at %s; worst world step %.2f at %s", worstLocal, whereLocal, worstWorld, whereWorld);
    }

    /** The tabard follows the forward leg while walking: it correlates with the leading thigh's swing. */
    static String tabard(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        Drive d = new Drive(seeds[5]);
        d.target = d.limbSpeed = 0.5;
        var st = new MotionEngine.State();
        Pose out = new Pose();
        int n = 60 * 6;
        double[] tab = new double[n], leg = new double[n];
        for (int f = 0; f < 60 * 2 + n; f++) {
            MotionEngine.pose(d.in(), st, fam, out);
            if (f >= 120) { tab[f - 120] = out.get("tabard", 0); leg[f - 120] = Math.min(0, Math.min(out.get("right_leg", 0), out.get("left_leg", 0))); }
            d.step(1 / 60.0);
        }
        double best = -1;
        int bestLag = 0;
        for (int lag = 0; lag <= 10; lag++) {
            double c = corr(Arrays.copyOfRange(leg, 0, n - lag), Arrays.copyOfRange(tab, lag, n));
            if (c > best) { best = c; bestLag = lag; }
        }
        check(best > 0.8, "the tabard should follow the forward leg: correlation " + best + " at lag " + bestLag);
        double[] walk = tabardRun(fam, seeds[5], 0.5), run = tabardRun(fam, seeds[5], 1.5);
        check(walk[3] >= 0.4, "walking, the tabard should swing with the forward leg about as much as the old rigid follow (0.46): swing share " + walk[3]);
        check(run[1] <= run[2] * 0.5, "at a sprint the tabard's per-frame step should change smoothly, with no cusp where the legs cross: worst change "
            + run[1] + " against the thigh's own " + run[2]);
        return String.format("correlation %.3f at a %d-frame lag, walking swing share %.2f; at speed 1.5 worst step %.2f deg/frame, step change %.2f (thigh %.2f), swing share %.2f",
            best, bestLag, walk[3], run[0], run[1], run[2], run[3]);
    }

    /**
     * A steady gait at this speed: {the tabard's worst per-frame step, the worst change between consecutive steps, the
     * thigh's own worst step change, the tabard's swing range over the forward leg's}.
     */
    static double[] tabardRun(MotionEngine.Family fam, MotionMath.Seed seed, double speed) {
        Drive d = new Drive(seed);
        d.target = d.limbSpeed = speed;
        var st = new MotionEngine.State();
        Pose out = new Pose();
        double prevTab = Double.NaN, prevStep = Double.NaN, prevLeg = Double.NaN, prevLegStep = Double.NaN;
        double step = 0, change = 0, legChange = 0, tabLo = 1e9, tabHi = -1e9, legLo = 1e9, legHi = -1e9;
        for (int f = 0; f < 60 * 8; f++) {
            MotionEngine.pose(d.in(), st, fam, out);
            double tab = out.get("tabard", 0), leg = out.get("right_leg", 0), fwd = Math.min(0, Math.min(leg, out.get("left_leg", 0)));
            if (f >= 180) {
                double s = tab - prevTab, ls = leg - prevLeg;
                step = Math.max(step, Math.abs(s));
                if (!Double.isNaN(prevStep)) change = Math.max(change, Math.abs(s - prevStep));
                if (!Double.isNaN(prevLegStep)) legChange = Math.max(legChange, Math.abs(ls - prevLegStep));
                prevStep = s;
                prevLegStep = ls;
                tabLo = Math.min(tabLo, tab); tabHi = Math.max(tabHi, tab); legLo = Math.min(legLo, fwd); legHi = Math.max(legHi, fwd);
            }
            prevTab = tab;
            prevLeg = leg;
            d.step(1 / 60.0);
        }
        return new double[]{step, change, legChange, (tabHi - tabLo) / Math.max(1e-9, legHi - legLo)};
    }

    static double corr(double[] a, double[] b) {
        double ma = 0, mb = 0;
        for (int i = 0; i < a.length; i++) { ma += a[i] / a.length; mb += b[i] / b.length; }
        double sab = 0, saa = 0, sbb = 0;
        for (int i = 0; i < a.length; i++) { sab += (a[i] - ma) * (b[i] - mb); saa += (a[i] - ma) * (a[i] - ma); sbb += (b[i] - mb) * (b[i] - mb); }
        return sab / Math.sqrt(saa * sbb + 1e-30);
    }

    /** Loose straps spring-lag the hips and torso: they swing at least 8 degrees while walking, trail the body, and settle when it stops. */
    static String straps(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        StringBuilder report = new StringBuilder();
        for (String strap : new String[]{"right_strap", "left_strap"}) {
            Drive d = new Drive(seeds[6]);
            d.target = d.limbSpeed = 0.6;
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY, still = 0;
            for (int f = 0; f < 60 * 8; f++) {
                if (f == 60 * 4) { d.target = 0; d.limbSpeed = 0; }
                MotionEngine.pose(d.in(), st, fam, out);
                double v = out.get(strap, 2);
                if (f > 60 && f < 60 * 4) { lo = Math.min(lo, v); hi = Math.max(hi, v); }
                if (f > 60 * 7) still = Math.max(still, Math.abs(out.get(strap, 2)) + Math.abs(out.get(strap, 0)));
                d.step(1 / 60.0);
            }
            check(hi - lo >= 8, strap + " should swing at least 8 degrees while walking (design section 6.1: alive): range " + (hi - lo));
            check(still < 3, strap + " should settle once the unit stands: " + still);
            report.append(String.format("%s swing %.1f, standing %.2f; ", strap, hi - lo, still));
        }
        return report.toString().trim();
    }

}
