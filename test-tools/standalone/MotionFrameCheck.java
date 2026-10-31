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
 * The MotionEngine as a per-frame function (design section 6.1): frame-rate independence, no steady-state allocation,
 * the limb-angle wrap and stability under random input. Fixtures come from MotionEngineCheck. Never shipped.
 */
public final class MotionFrameCheck {
    public static void main(String[] args) {
        MotionEngine.Family fam = family(true), bare = family(false);
        MotionMath.Seed[] seeds = seeds(20, 6100);
        run("frame-rate independence", () -> fps(fam, seeds));
        run("allocation", () -> allocation(fam, seeds));
        run("limb angle wrap", () -> limbWrap(fam, seeds));
        run("stable", () -> stable(fam, seeds));
        if (failed > 0) {
            System.out.println("MotionFrameCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionFrameCheck: " + checks + " checks passed");
    }

    /** The bones only the secondary layer moves (MotionBody.secondary): springs whose error under a coarse frame shrinks with h squared. */
    static final Set<String> SECONDARY = Set.of("cape", "tabard", "crest", "right_strap", "left_strap");
    /** The frame rates checked against a 240 fps reference, and each one's limit for the secondary bones in degrees (h-squared scaled). */
    static final int[] FPS = {20, 30, 60};
    static final double[] SECONDARY_LIMIT = {1.5, 0.75, 0.2};
    /** Every primary channel (all but the secondary bones) must match the reference this closely: degrees, and pixels. */
    static final double PRIMARY_ROT = 0.01, PRIMARY_PX = 0.01;
    /** The timeline's length in seconds and how often it is sampled: every 1/20 s (every 1/10 s at 30 fps, where its frames land). */
    static final double FPS_SPAN = 6.0, FPS_SAMPLE = 0.05;

    /**
     * Frame-rate independence (Task 12 cap ruling): one input timeline (walk, enemy, look, attack, hit, stance change, turn,
     * second attack, death) played at 20, 30 and 60 fps against 240 fps, eight seeds, compared at every 1/20 s sample point
     * each rate's frames land on. Primary channels hold within 0.01 degrees and 0.01 px; the secondary springs within 1.5,
     * 0.75 and 0.2 degrees. The limits are the ruling's; the engine is not retuned to meet them.
     */
    static String fps(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double[] rot = new double[FPS.length], px = new double[FPS.length], sec = new double[FPS.length];
        String[] where = new String[FPS.length], secWhere = new String[FPS.length];
        int samples = 0;
        for (int s = 0; s < 8; s++) {
            Map<Integer, Map<String, double[]>> ref = fpsRun(fam, seeds[s], 240);
            for (int i = 0; i < FPS.length; i++) {
                Map<Integer, Map<String, double[]>> got = fpsRun(fam, seeds[s], FPS[i]);
                check(got.size() >= (FPS[i] == 30 ? 60 : 120), FPS[i] + " fps should land on every sample point: " + got.size());
                for (var k : got.entrySet()) {
                    Map<String, double[]> r = ref.get(k.getKey()), g = k.getValue();
                    Set<String> bones = new HashSet<>(r.keySet());
                    bones.addAll(g.keySet());
                    for (String b : bones) for (int c = 0; c < 6; c++) {
                        double e = Math.abs(r.getOrDefault(b, new double[6])[c] - g.getOrDefault(b, new double[6])[c]);
                        String at = "seed " + s + " t " + String.format("%.2f", k.getKey() * FPS_SAMPLE) + " " + b + " ch " + c;
                        if (SECONDARY.contains(b)) { if (e > sec[i]) { sec[i] = e; secWhere[i] = at; } }
                        else if (c < 3) { if (e > rot[i]) { rot[i] = e; where[i] = at; } }
                        else px[i] = Math.max(px[i], e);
                        samples++;
                    }
                }
            }
        }
        StringBuilder report = new StringBuilder();
        for (int i = 0; i < FPS.length; i++)
            report.append(String.format("%d fps primary %.5f deg (%s) %.5f px, secondary %.3f / %.2f deg (%s); ", FPS[i], rot[i], where[i], px[i], sec[i], SECONDARY_LIMIT[i], secWhere[i]));
        report.append(samples).append(" channel samples");
        for (int i = 0; i < FPS.length; i++) {
            check(rot[i] <= PRIMARY_ROT && px[i] <= PRIMARY_PX, FPS[i] + " fps: a primary channel is off (limit " + PRIMARY_ROT + " deg, " + PRIMARY_PX + " px): " + report);
            check(sec[i] <= SECONDARY_LIMIT[i], FPS[i] + " fps: a secondary spring is off (limit " + SECONDARY_LIMIT[i] + " deg): " + report);
        }
        return report.toString();
    }

    /** The integral from minus infinity to t of a ramp from 0 at a to 1 at b (and 1 after): the limb angle a speed ramp walks. */
    static double integral(double t, double a, double b) {
        if (t <= a) return 0;
        if (t <= b) return (t - a) * (t - a) / (2 * (b - a));
        return (b - a) / 2 + (t - b);
    }

    /** One run of the timeline at this frame rate: the pose at every sample point (by index, FPS_SAMPLE apart) a frame lands on. */
    static Map<Integer, Map<String, double[]>> fpsRun(MotionEngine.Family fam, MotionMath.Seed seed, int fps) {
        var st = new MotionEngine.State();
        Pose out = new Pose();
        Map<Integer, Map<String, double[]>> shots = new HashMap<>();
        int frames = (int) Math.round(FPS_SPAN * fps);
        for (int k = 0; k <= frames; k++) {
            double tau = k / (double) fps;
            boolean dead = tau >= 4.6 - 1e-9;
            // the walk eases in over 1.0-1.3 s and out over 2.4-2.8 s, as Minecraft's limb distance does: the engine's input is
            // continuous, so any frame-rate error is the engine's own, not a step the frames happen to straddle
            double speed = 0.6 * Math.max(0, Math.min(1, Math.min((tau - 1.0) / 0.3, (2.8 - tau) / 0.4)));
            double limb = 0.6 * (integral(tau, 1.0, 1.3) - integral(tau, 2.4, 2.8)) * 20;
            MotionInput.Attack attack = tau >= 3.5 - 1e-9 ? new MotionInput.Attack(1, 20070) : tau >= 1.2 - 1e-9 ? new MotionInput.Attack(0, 20024) : null;
            MotionInput.Hit hit = dead ? new MotionInput.Hit("slash", "body", 60, 2, false, true, 20092)
                : tau >= 2.0 - 1e-9 ? new MotionInput.Hit("slash", "body", 30, 1, false, false, 20040) : null;
            String stance = tau >= 2.6 - 1e-9 ? "hold" : "";
            double yawRate = tau >= 3.0 - 1e-9 && tau < 3.8 - 1e-9 ? 120 : 0;
            MotionEngine.pose(new MotionInput(1000 + tau, limb, speed, tau >= 0.8 - 1e-9 ? 40 : 0, tau >= 3.2 - 1e-9 ? 15 : 0, yawRate,
                stance, 0, false, tau >= 0.5 - 1e-9, dead, dead ? tau - 4.6 : 0, attack, hit, seed), st, fam, out);
            double index = tau / FPS_SAMPLE;
            if (k > 0 && Math.abs(index - Math.rint(index)) < 1e-6) shots.put((int) Math.rint(index), out.snapshot());
        }
        return shots;
    }

    /** Steady-state frames allocate nothing: ThreadMXBean.getThreadAllocatedBytes over 10,000 frames after a 20,000-frame warm-up. */
    static String allocation(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        var mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        check(mx.isThreadAllocatedMemorySupported(), "this JVM cannot measure thread allocation, so the check would pass trivially");
        if (!mx.isThreadAllocatedMemoryEnabled()) mx.setThreadAllocatedMemoryEnabled(true);
        check(mx.isThreadAllocatedMemoryEnabled(), "thread allocation measurement is disabled, so the check would pass trivially");
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
            check(b0 >= 0 && b1 >= b0, "thread allocation reads should be real counts: " + b0 + ", " + b1);
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

}
