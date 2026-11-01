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
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import static gg.gravesandglory.MotionEngineCheck.*;

/**
 * The MotionEngine's hit and death rulings (design section 6.1) through its frame API: death directions, static corpses and
 * per-side knee buckles. Fixtures come from MotionEngineCheck. Never shipped.
 */
public final class MotionReactCheck {
    public static void main(String[] args) {
        MotionEngine.Family fam = family(true), bare = family(false);
        MotionMath.Seed[] seeds = seeds(20, 6100);
        run("death directions", () -> deaths(fam, seeds));
        run("static corpse", () -> corpse(fam, seeds));
        run("knee buckle by side", () -> kneeSide(bare, seeds));
        if (failed > 0) {
            System.out.println("MotionReactCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionReactCheck: " + checks + " checks passed");
    }

    /**
     * A lethal blow from 0, 90, 180 and 270 degrees plays front, right, back and left. 45 and -135 degrees start the fall as an
     * even blend of the two nearest clips and come to rest in one authored end pose (the tie goes to the second: right, back),
     * since a blend of two lying poses props the body up and sinks it (Task 16 item 5).
     */
    static String deaths(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        float[] bearings = {0, 90, 180, 270, 45, -135};
        String[] top = {"death_front", "death_right", "death_back", "death_left", "death_right", "death_back"};
        String[] early = {null, null, null, null, "death_front", "death_left"};
        double[] rz = {0, -80, 0, 80, -80, 0};
        StringBuilder report = new StringBuilder();
        for (int i = 0; i < bearings.length; i++) {
            Drive d = new Drive(seeds[1]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            MotionEngine.pose(d.in(), st, fam, out);
            d.step(1 / 60.0);
            d.die(bearings[i]);
            String fall = "";
            for (int f = 0; f < 120; f++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (f == 6 && early[i] != null) {
                    Map<String, Double> share = new HashMap<>();
                    for (var p : MotionEngine.playing(st, d.time)) share.merge(p.clip().name(), p.gain(), Double::sum);
                    double a = share.getOrDefault(early[i], 0.0), b = share.getOrDefault(top[i], 0.0), half = a + b > 0 ? b / (a + b) : 0;
                    check(Math.abs(half - 0.5) <= 0.05, "a blow from " + bearings[i] + " should start the fall as an even blend of " + early[i] + " and " + top[i] + ": " + share);
                    fall = String.format(Locale.ROOT, " (falling %.2f %s)", half, top[i]);
                }
                d.step(1 / 60.0);
            }
            var playing = MotionEngine.playing(st, d.time);
            Map<String, Double> share = new HashMap<>();
            for (var p : playing) share.merge(p.clip().name(), p.gain(), Double::sum);
            check(share.getOrDefault(top[i], 0.0) >= 0.99, "a blow from " + bearings[i] + " should rest in " + top[i] + ": " + share);
            check(near(out.get("body", 2), rz[i], 2), "a blow from " + bearings[i] + " should roll the body to " + rz[i] + ", got " + out.get("body", 2));
            report.append(bearings[i]).append(' ').append(share.keySet()).append(fall).append("; ");
        }
        return report.toString().trim();
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

}
