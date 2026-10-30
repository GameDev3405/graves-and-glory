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
 * The review's rulings for the MotionEngine (animation rework design section 6.1) through its frame API: no pops through
 * stance flicker; the planted-feet hand-off; stance keys for Hold and Run; the server's attack variant; static corpses; no
 * steady-state allocation; frame-rate independence; death directions; novelty idles that leave the sword arm alone;
 * per-side knee buckles; the limb-angle wrap; the aim fallback; no clones; stability; the turn step, tabard, straps and
 * breathing rate. Fixtures come from MotionEngineCheck. Never shipped.
 */
public final class MotionRulingsCheck {
    public static void main(String[] args) {
        MotionEngine.Family fam = family(true), bare = family(false);
        MotionMath.Seed[] seeds = seeds(20, 6100);
        run("flicker", () -> flicker(seeds));
        run("hand-off", () -> handoff(fam, seeds));
        run("hold", () -> hold(fam, seeds));
        run("run and charge", () -> runCharge());
        run("variant", () -> variant(fam));
        run("static corpse", () -> corpse(fam, seeds));
        run("allocation", () -> allocation(fam, seeds));
        run("frame-rate independence", () -> fps(fam, seeds));
        run("death directions", () -> deaths(fam, seeds));
        run("novelty spares the sword arm", () -> novelty(fam, seeds));
        run("knee buckle by side", () -> kneeSide(bare, seeds));
        run("limb angle wrap", () -> limbWrap(fam, seeds));
        run("aim fallback", () -> aimFallback(seeds));
        run("no clones", () -> noClones(fam));
        run("stable", () -> stable(fam, seeds));
        run("turn-in-place step", () -> turnStep(fam, seeds));
        run("tabard follow", () -> tabard(fam, seeds));
        run("loose straps", () -> straps(fam, seeds));
        run("breathing rate", () -> breathRate(fam));
        if (failed > 0) {
            System.out.println("MotionRulingsCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionRulingsCheck: " + checks + " checks passed");
    }

    /** A guard unit whose enemyNear flips at 20, 10 and 5 Hz for 5 s stays under the pop bar and settles to the guard. */
    static String flicker(MotionMath.Seed[] seeds) {
        Map<String, Clip> st = new HashMap<>();
        st.put("idle", clip("idle", 0.5, false, "", "linear", "right_arm:rotation=0,-5,0,0"));
        st.put("guard", clip("guard", 0.5, false, "", "linear", "right_arm:rotation=0,-75,0,0"));
        var fam = new MotionEngine.Family("flicker", st, List.of(), Map.of(), Map.of());
        StringBuilder report = new StringBuilder();
        for (int period : new int[]{3, 6, 12}) {
            double worst = 0, settled = 0;
            int layers = 0;
            for (int s = 0; s < 4; s++) {
                Drive d = new Drive(seeds[s]), ref = new Drive(seeds[s]);
                var state = new MotionEngine.State();
                var refState = new MotionEngine.State();
                Pose out = new Pose(), refOut = new Pose();
                d.enemy = ref.enemy = true;
                Map<String, double[]> prev = null;
                for (int f = 0; f < 60 * 8; f++) {
                    if (f >= 60 && f < 360 && f % period == 0) d.enemy = !d.enemy;
                    if (f == 360) d.enemy = true;
                    MotionEngine.pose(d.in(), state, fam, out);
                    MotionEngine.pose(ref.in(), refState, fam, refOut);
                    var cur = out.snapshot();
                    if (prev != null) worst = Math.max(worst, ratio(prev, cur));
                    prev = cur;
                    layers = Math.max(layers, state.stances.size());
                    d.step(1 / 60.0);
                    ref.step(1 / 60.0);
                }
                settled = Math.max(settled, Math.abs(out.get("right_arm", 0) - refOut.get("right_arm", 0)));
            }
            check(worst <= 1, "flicker every " + period + " frames: worst step " + String.format("%.2f", worst * 12) + " deg (ratio " + String.format("%.3f", worst) + ")");
            check(settled < 0.01, "flicker every " + period + " frames should settle to the guard: right_arm rx off by " + settled);
            report.append(String.format("%d Hz %.2f deg/frame, %d layers; ", 60 / period, worst * 12, layers));
        }
        return report.toString().trim();
    }

    /** A leg-keying attack from standing at crouch 0, 1.5 and 3 px, and from the fixture's guard, hands the feet to the clip without a pop. */
    static String handoff(MotionEngine.Family fixture, MotionMath.Seed[] seeds) {
        StringBuilder report = new StringBuilder();
        Clip probe = Clip.parse("{\"format\":1,\"clip\":\"lunge\",\"length\":0.8,\"loop\":false,\"markers\":{\"impact\":0.4},\"bones\":{\"right_leg\":{\"rotation\":[{\"t\":0,\"i\":\"linear\",\"v\":[0,0,0]},{\"t\":0.8,\"i\":\"linear\",\"v\":[-10,0,0]}]}}}");
        double overall = 0;
        for (int crouchCase = 0; crouchCase < 4; crouchCase++) {
            double crouch = crouchCase == 3 ? -1 : crouchCase * 1.5;
            MotionEngine.Family fam;
            if (crouch < 0) fam = fixture;
            else {
                Map<String, Clip> st = new HashMap<>();
                st.put("idle", clip("idle", 0.5, false, "", "linear", "root:position=0,0," + crouch + ",0"));
                fam = new MotionEngine.Family("crouch", st, List.of(probe, lunge()), Map.of(), Map.of());
            }
            double worst = 0;
            String where = "";
            for (int s = 0; s < 4; s++) for (int variant = 0; variant < 2; variant++) {
                Drive d = new Drive(seeds[s]);
                d.enemy = crouch < 0;
                var st = new MotionEngine.State();
                Pose out = new Pose();
                Pops pops = new Pops();
                for (int f = 0; f < 180; f++) {
                    if (f == 60) d.attack = new MotionInput.Attack(crouch < 0 ? 2 : variant, d.tick());
                    MotionEngine.pose(d.in(), st, fam, out);
                    pops.frame("seed " + s + " variant " + variant + " frame " + f, out, st, d.time, 1 / 60.0);
                    d.step(1 / 60.0);
                }
                if (pops.worst > worst) { worst = pops.worst; where = pops.where; }
            }
            overall = Math.max(overall, worst);
            String name = crouch < 0 ? "guard" : "crouch " + crouch + " px";
            check(worst <= 1, "hand-off from " + name + ": worst ratio " + String.format("%.3f", worst) + " at " + where);
            report.append(String.format("%s %.3f; ", name, worst));
        }
        return "worst ratio " + report.toString().trim();
    }

    /** Hold with no enemy near takes the low ready guard (design section 6.2: "Hold, or an enemy near"). */
    static String hold(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        check(MotionEngine.stanceKey("hold", false, false, false, false).equals("guard"), "Hold with no enemy should key guard, got " + MotionEngine.stanceKey("hold", false, false, false, false));
        check(MotionEngine.stanceKey("hold", true, false, false, false).equals("shield"), "Hold with the shield up should key shield");
        Drive d = new Drive(seeds[0]);
        d.stance = "hold";
        var st = new MotionEngine.State();
        Pose out = new Pose();
        for (int f = 0; f < 90; f++) { MotionEngine.pose(d.in(), st, fam, out); d.step(1 / 60.0); }
        check(out.get("right_arm", 0) < -60, "a holding unit with no enemy near should raise the guard: right_arm rx " + out.get("right_arm", 0));
        return "right_arm rx " + String.format("%.1f", out.get("right_arm", 0));
    }

    /** Run with an enemy near is a charge: the guard comes up (section 6.2); with none near it is the march carry. */
    static String runCharge() {
        check(MotionEngine.stanceKey("run", false, true, true, false).equals("guard"), "Run with an enemy near should key guard, got " + MotionEngine.stanceKey("run", false, true, true, false));
        check(MotionEngine.stanceKey("run", false, false, true, false).equals("carry"), "Run with no enemy should key carry");
        check(MotionEngine.stanceKey("run", false, false, false, false).equals("carry"), "a standing Run order with no enemy should still carry");
        check(MotionEngine.stanceKey("", false, false, false, false).equals("idle"), "no order, no enemy, standing: idle");
        return "run + enemy -> guard, run alone -> carry";
    }

    /** The server's attack variant picks the clip directly for every unit (section 6.4). */
    static String variant(MotionEngine.Family fam) {
        MotionMath.Seed[] many = seeds(20, 404);
        String[] want = {"slash", "thrust", "lunge"};
        for (int v = 0; v < 5; v++) for (MotionMath.Seed seed : many) {
            Drive d = new Drive(seed);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            MotionEngine.pose(d.in(), st, fam, out);
            d.step(1 / 60.0);
            d.attack = new MotionInput.Attack(v, d.tick());
            MotionEngine.pose(d.in(), st, fam, out);
            var playing = MotionEngine.playing(st, d.time);
            check(!playing.isEmpty() && playing.get(playing.size() - 1).clip().name().equals(want[v % 3]), "variant " + v + " should play " + want[v % 3] + " for every seed: " + playing);
        }
        return "variants 0-4 over 20 seeds";
    }

    /** After the death clip ends, every channel is constant to 1e-6 over 3 s (death clips are absolute relative to rest). */
    static String corpse(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worst = 0;
        for (int s = 0; s < 6; s++) for (int mode = 0; mode < 3; mode++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            d.enemy = true;
            d.headYaw = 50;
            d.headPitch = 20;
            if (mode == 1) d.target = d.limbSpeed = 0.6;
            if (mode == 2) d.stance = "ranged_aim";
            d.aim = 20;
            Map<String, double[]> first = null;
            for (int f = 0; f < 60 * 7; f++) {
                if (f == 30) d.attack = new MotionInput.Attack(s % 3, d.tick());
                if (f == 50) d.hit("head", 60, 2, false, false);
                if (f == 60) { d.yawRate = 90; d.die(30 * s - 60); }
                MotionEngine.pose(d.in(), st, fam, out);
                if (f >= 60 + 72 + 60) {   // the 1.2 s clip has ended plus one second
                    var cur = out.snapshot();
                    if (first == null) first = cur;
                    Set<String> bones = new HashSet<>(cur.keySet());
                    bones.addAll(first.keySet());
                    for (String b : bones) for (int c = 0; c < 6; c++)
                        worst = Math.max(worst, Math.abs(cur.getOrDefault(b, new double[6])[c] - first.getOrDefault(b, new double[6])[c]));
                }
                d.step(1 / 60.0);
            }
        }
        check(worst <= 1e-6, "a settled corpse should be static: a channel moved " + worst);
        return "largest drift " + worst;
    }

    /** Steady-state frames allocate nothing: ThreadMXBean.getThreadAllocatedBytes over 10,000 frames after a 20,000-frame warm-up. */
    static String allocation(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        var mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long id = Thread.currentThread().getId();
        StringBuilder report = new StringBuilder();
        String[] names = {"idle", "walking guard", "corpse", "aiming"};
        for (int mode = 0; mode < names.length; mode++) {
            int warm = 20000, measured = 10000, n = warm + measured;
            MotionInput[] inputs = new MotionInput[n];
            for (int f = 0; f < n; f++) {
                double t = 1000 + f / 60.0;
                boolean moving = mode == 1;
                inputs[f] = new MotionInput(t, moving ? f * 0.2 : 0, moving ? 0.6 : 0, mode == 1 ? 30 * Math.sin(f / 90.0) : 0, 0, mode == 1 ? 20 * Math.sin(f / 200.0) : 0,
                    mode == 3 ? "ranged_aim" : "", (float) (mode == 3 ? 20 * Math.sin(f / 70.0) : 0), false, mode == 1 || mode == 3, mode == 2, mode == 2 ? f / 60.0 : 0, null, null, seeds[mode]);
            }
            var st = new MotionEngine.State();
            Pose out = new Pose();
            for (int f = 0; f < warm; f++) MotionEngine.pose(inputs[f], st, fam, out);
            long b0 = mx.getThreadAllocatedBytes(id), b1 = mx.getThreadAllocatedBytes(id);
            long base = b1 - b0;
            long before = mx.getThreadAllocatedBytes(id);
            for (int f = warm; f < n; f++) MotionEngine.pose(inputs[f], st, fam, out);
            long after = mx.getThreadAllocatedBytes(id);
            double perFrame = (after - before - base) / (double) measured;
            report.append(String.format("%s %.3f B/frame; ", names[mode], perFrame));
            check(perFrame < 1, names[mode] + ": steady-state frames should not allocate (tolerance under 1 byte a frame), measured " + perFrame + " bytes a frame");
        }
        return report.toString().trim();
    }

    /** The same input timeline at 20, 60 and 240 fps gives the same pose at 1.6, 2.4 and 3.2 s. */
    static String fps(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double[] times = {1.6, 2.4, 3.2};
        double rot = 0, px = 0;
        String where = "";
        for (int s = 0; s < 4; s++) {
            List<Map<String, double[]>> ref = fpsRun(fam, seeds[s], 240, times);
            for (int fps : new int[]{20, 60}) {
                List<Map<String, double[]>> got = fpsRun(fam, seeds[s], fps, times);
                for (int k = 0; k < times.length; k++) for (String b : ref.get(k).keySet()) for (int c = 0; c < 6; c++) {
                    double e = Math.abs(ref.get(k).get(b)[c] - got.get(k).getOrDefault(b, new double[6])[c]);
                    if (c < 3 && e > rot) { rot = e; where = fps + " fps seed " + s + " t " + times[k] + " " + b + " ch " + c; }
                    if (c >= 3) px = Math.max(px, e);
                }
            }
        }
        check(rot <= 0.5 && px <= 0.05, "frame-rate independence: worst " + rot + " deg at " + where + ", " + px + " px (tolerance 0.5 deg, 0.05 px)");
        return String.format("worst %.4f deg (%s), %.5f px; tolerance 0.5 deg, 0.05 px", rot, where, px);
    }

    static List<Map<String, double[]>> fpsRun(MotionEngine.Family fam, MotionMath.Seed seed, int fps, double[] times) {
        var st = new MotionEngine.State();
        Pose out = new Pose();
        List<Map<String, double[]>> shots = new ArrayList<>();
        int frames = (int) Math.round(times[times.length - 1] * fps);
        for (int k = 0; k <= frames; k++) {
            double tau = k / (double) fps;
            boolean go = tau >= 1.0 - 1e-9;
            MotionInput.Attack attack = tau >= 1.2 - 1e-9 ? new MotionInput.Attack(0, 20024) : null;
            MotionInput.Hit hit = tau >= 2.0 - 1e-9 ? new MotionInput.Hit("slash", "body", 30, 1, false, false, 20040) : null;
            MotionEngine.pose(new MotionInput(1000 + tau, go ? 12 * (tau - 1) : 0, go ? 0.6 : 0, tau >= 0.8 - 1e-9 ? 40 : 0, 0, 0, "", 0, false, tau >= 0.5 - 1e-9, false, 0, attack, hit, seed), st, fam, out);
            for (double t : times) if (Math.abs(t * fps - k) < 1e-6) shots.add(out.snapshot());
        }
        return shots;
    }

    /** A lethal blow from 0, 90, 180 and 270 degrees plays front, right, back and left; 45 degrees blends front and right evenly. */
    static String deaths(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        float[] bearings = {0, 90, 180, 270, 45, -135};
        String[] top = {"death_front", "death_right", "death_back", "death_left", "death_front", "death_left"};
        double[] rz = {0, -80, 0, 80, -40, 40};
        StringBuilder report = new StringBuilder();
        for (int i = 0; i < bearings.length; i++) {
            Drive d = new Drive(seeds[1]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            MotionEngine.pose(d.in(), st, fam, out);
            d.step(1 / 60.0);
            d.die(bearings[i]);
            for (int f = 0; f < 120; f++) { MotionEngine.pose(d.in(), st, fam, out); d.step(1 / 60.0); }
            var playing = MotionEngine.playing(st, d.time);
            Map<String, Double> share = new HashMap<>();
            for (var p : playing) share.merge(p.clip().name(), p.gain(), Double::sum);
            check(share.getOrDefault(top[i], 0.0) >= (i >= 4 ? 0.49 : 0.99), "a blow from " + bearings[i] + " should play " + top[i] + ": " + share);
            check(near(out.get("body", 2), rz[i], 2), "a blow from " + bearings[i] + " should roll the body to " + rz[i] + ", got " + out.get("body", 2));
            report.append(bearings[i]).append(' ').append(share.keySet()).append("; ");
        }
        return report.toString().trim();
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

    /** A legs blow from the right buckles the right knee more; from the left, the left. */
    static String kneeSide(MotionEngine.Family bare, MotionMath.Seed[] seeds) {
        StringBuilder report = new StringBuilder();
        for (int s = 0; s < 4; s++) for (float bearing : new float[]{90, -90}) {
            String[] bones = {"right_shin", "left_shin"};
            double[][] base = hitTrace(bare, seeds[s], null, 0, 0, bones), hit = hitTrace(bare, seeds[s], "legs", bearing, 2, bones);
            double right = 0, left = 0;
            for (int f = 0; f < base.length; f++) { right = Math.max(right, hit[f][0] - base[f][0]); left = Math.max(left, hit[f][3] - base[f][3]); }
            double near = bearing > 0 ? right : left, far = bearing > 0 ? left : right;
            check(near > 1.3 * far && near > 8, "a legs blow from " + bearing + " should buckle its own side's knee more: near " + near + ", far " + far);
            if (s == 0) report.append(String.format("%+.0f: near %.1f far %.1f; ", bearing, near, far));
        }
        return report.toString().trim();
    }

    /**
     * A walking unit's stride stays continuous where the limb angle passes 1e6 and the engine's own wrap: no channel moves
     * more than half the pop bar beyond what the stride moves at the input's own, unwrapped limb angle.
     */
    static String limbWrap(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worst = 0;
        for (double edge : new double[]{1.0e6, 100000 * MotionMath.TAU / 0.6662, 2 * 100000 * MotionMath.TAU / 0.6662}) {
            Drive d = new Drive(seeds[2]);
            d.target = d.limbSpeed = 0.6;
            d.limbAngle = edge - 30;
            var st = new MotionEngine.State();
            Pose out = new Pose(), gait = new Pose(), gaitPrev = new Pose();
            Map<String, double[]> prev = null;
            for (int f = 0; f < 60 * 5; f++) {
                MotionEngine.pose(d.in(), st, fam, out);
                var cur = out.snapshot();
                gait.clear();
                MotionBody.gaitPose(st.frame, d.limbAngle, gait);
                if (prev != null && f > 60) {
                    Set<String> bones = new HashSet<>(cur.keySet());
                    bones.addAll(prev.keySet());
                    for (String bone : bones) {
                        double[] n = cur.getOrDefault(bone, new double[6]), o = prev.getOrDefault(bone, new double[6]);
                        for (int c = 0; c < 6; c++) {
                            double stride = Math.abs(gait.get(bone, c) - gaitPrev.get(bone, c));
                            worst = Math.max(worst, Math.abs(n[c] - o[c]) / ((c < 3 ? 12 : 1.5) + stride));
                        }
                    }
                }
                Pose swap = gaitPrev; gaitPrev = gait; gait = swap;
                prev = cur;
                d.step(1 / 60.0);
            }
        }
        check(worst <= 0.5, "the stride should stay continuous across the limb-angle wrap: worst ratio " + worst);
        return String.format("worst ratio %.3f", worst);
    }

    /** A family with no aim clip falls back to its guard and still raises the arms by aimPitch, even from a guard already up. */
    static String aimFallback(MotionMath.Seed[] seeds) {
        Map<String, Clip> st = stances();
        st.remove("aim");
        var fam = new MotionEngine.Family("noaim", st, List.of(), Map.of(), Map.of());
        double[] arm = new double[2];
        for (int k = 0; k < 2; k++) {
            Drive d = new Drive(seeds[3]);
            d.enemy = true;
            var state = new MotionEngine.State();
            Pose out = new Pose();
            for (int f = 0; f < 150; f++) {
                if (f == 30) { d.stance = "ranged_aim"; d.aim = k == 0 ? 0 : 30; }
                MotionEngine.pose(d.in(), state, fam, out);
                d.step(1 / 60.0);
            }
            arm[k] = out.get("right_arm", 0);
        }
        check(near(arm[1] - arm[0], 30, 0.5), "aimPitch 30 should still move the guard fallback's arm by 30: " + (arm[1] - arm[0]));
        return String.format("arm moved %.2f for aimPitch 30", arm[1] - arm[0]);
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
     * pairwise RMS distances is at least 2.5 degrees and the closest pair at least 1 degree.
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
        return String.format("closest %.2f, 5th percentile %.2f, median %.2f deg RMS", least, p5, dist.get(dist.size() / 2));
    }

    /**
     * Stable: 10 minutes of random input at random dt in [0.004, 0.1] stays finite, inside reach(Family), and keeps its state
     * bounded. A clip-less family shows the procedural bound itself is meaningful: no wider than 180 degrees.
     */
    static String stable(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        var none = new MotionEngine.Family("none", Map.of(), List.of(), Map.of(), Map.of());
        check(MotionEngine.PROC_ROT <= 180, "the procedural rotation bound should be meaningful, is " + MotionEngine.PROC_ROT);
        StringBuilder report = new StringBuilder();
        for (MotionEngine.Family f : new MotionEngine.Family[]{fam, none}) {
            double[] reach = MotionEngine.reach(f);
            check(reach.length == 6 && reach[3] > 0 && reach[4] > 0 && reach[5] > 0, "reach(Family) should bound every channel: " + Arrays.toString(reach));
            double[] seen = new double[6];
            int maxLoad = stableRun(f, seeds, reach, seen);
            check(maxLoad <= MotionEngine.MAX_LAYERS + MotionEngine.MAX_TRACKS + 8, "stable: active tracks, pulses and stance layers should stay bounded, peaked at " + maxLoad);
            report.append(String.format("%s: peak rot %.1f / %.1f, px %.2f / %.2f, load %d; ", f.name(), Math.max(seen[0], Math.max(seen[1], seen[2])), reach[0],
                Math.max(seen[3], Math.max(seen[4], seen[5])), reach[3], maxLoad));
        }
        return report.toString().trim();
    }

    static int stableRun(MotionEngine.Family fam, MotionMath.Seed[] seeds, double[] reach, double[] seen) {
        Random r = new Random(77);
        String[] stances = {"", "hold", "brace", "wall", "shield_move", "fall_back", "run", "ranged_rest", "ranged_aim", "nonsense"};
        String[] zones = {"head", "body", "legs", "arm", ""};
        int maxLoad = 0;
        for (int s = 0; s < 2; s++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double total = 0;
            int frames = 0;
            while (total < 600) {
                double dt = 0.004 + 0.096 * r.nextDouble();
                if (r.nextDouble() < dt * 0.3) d.stance = stances[r.nextInt(stances.length)];
                if (r.nextDouble() < dt * 0.2) d.enemy = !d.enemy;
                if (r.nextDouble() < dt * 0.2) d.shield = !d.shield;
                if (r.nextDouble() < dt * 0.3) d.target = r.nextDouble() < 0.4 ? 0 : 1.5 * r.nextDouble();
                if (r.nextDouble() < dt * 0.5) d.headYaw = -200 + 400 * r.nextDouble();
                if (r.nextDouble() < dt * 0.5) d.headPitch = -100 + 200 * r.nextDouble();
                if (r.nextDouble() < dt * 0.3) d.yawRate = r.nextDouble() < 0.5 ? 0 : -500 + 1000 * r.nextDouble();
                if (r.nextDouble() < dt * 0.3) d.aim = (float) (-90 + 180 * r.nextDouble());
                if (r.nextDouble() < dt * 1.0) d.attack = new MotionInput.Attack(r.nextInt(7) - 3, d.tick() - r.nextInt(12));
                if (r.nextDouble() < dt * 1.0) d.hit = new MotionInput.Hit("slash", zones[r.nextInt(zones.length)], (float) (-400 + 800 * r.nextDouble()), r.nextInt(5) - 1, r.nextBoolean(), r.nextDouble() < 0.2, d.tick() - r.nextInt(4));
                if (r.nextDouble() < dt * 0.04) { d.dead = !d.dead; d.deathSeconds = 0; }
                if (r.nextDouble() < dt * 0.02) d.headYaw = Double.NaN;
                if (r.nextDouble() < 0.002) d.time -= 0.05;   // a render clock that steps back
                MotionEngine.pose(d.in(), st, fam, out);
                if (Double.isNaN(d.headYaw)) d.headYaw = 0;
                for (String bone : out.bones()) for (int c = 0; c < 6; c++) {
                    double v = out.get(bone, c);
                    check(Double.isFinite(v), "stable: " + bone + " channel " + c + " went non-finite after " + total + " s");
                    check(Math.abs(v) <= reach[c] + 1e-9, "stable (" + fam.name() + "): " + bone + " channel " + c + " = " + v + " is outside reach " + reach[c] + " after " + total + " s");
                    seen[c] = Math.max(seen[c], Math.abs(v));
                }
                maxLoad = Math.max(maxLoad, MotionEngine.load(st));
                d.step(dt);
                total += dt;
                frames++;
            }
            check(frames > 10000, "stable: should step about 11,500 frames, stepped " + frames);
        }
        return maxLoad;
    }

    /** Turning in place steps: each knee lifts in turn and the thighs yaw with the turn. */
    static String turnStep(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        Drive d = new Drive(seeds[4]);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        double right = 0, left = 0, ry = 0;
        int alternations = 0;
        double lastLead = 0;
        for (int f = 0; f < 60 * 4; f++) {
            if (f == 60) d.yawRate = 140;
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
        return String.format("correlation %.3f at a %d-frame lag", best, bestLag);
    }

    static double corr(double[] a, double[] b) {
        double ma = 0, mb = 0;
        for (int i = 0; i < a.length; i++) { ma += a[i] / a.length; mb += b[i] / b.length; }
        double sab = 0, saa = 0, sbb = 0;
        for (int i = 0; i < a.length; i++) { sab += (a[i] - ma) * (b[i] - mb); saa += (a[i] - ma) * (a[i] - ma); sbb += (b[i] - mb) * (b[i] - mb); }
        return sab / Math.sqrt(saa * sbb + 1e-30);
    }

    /** Loose straps spring-lag the hips and torso: they swing while walking, trail the body, and settle when it stops. */
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
            check(hi - lo > 3, strap + " should swing while walking: range " + (hi - lo));
            check(still < 3, strap + " should settle once the unit stands: " + still);
            report.append(String.format("%s swing %.1f, standing %.2f; ", strap, hi - lo, still));
        }
        return report.toString().trim();
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
}
