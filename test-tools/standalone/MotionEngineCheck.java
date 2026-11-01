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

/**
 * The layered MotionEngine (animation rework design section 6.1) through its frame API: pops at rest, planted feet, hit
 * zones, stance easing, the gait, life, look and secondary, and actions; plus the shared fixtures (rig, clips, family, seeds,
 * the test drive and the pop tracker). MotionStanceCheck, MotionReactCheck, MotionFrameCheck, MotionLifeCheck and MotionClothCheck cover the review's rulings through the same API and
 * MotionLayersCheck the layer functions directly. Never shipped.
 */
public final class MotionEngineCheck {
    static int checks, failed;

    static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    static boolean near(double a, double b, double eps) { return Math.abs(a - b) <= eps; }

    static void run(String name, Supplier<String> body) {
        try {
            String detail = body.get();
            System.out.println("PASS " + name + (detail == null || detail.isEmpty() ? "" : " (" + detail + ")"));
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL " + name + ": " + t);
        }
    }

    public static void main(String[] args) {
        Map<String, RigBone> rig = rig();
        MotionEngine.Family fam = family(true), bare = family(false);
        MotionMath.Seed[] seeds = seeds(20, 6100);
        run("pops at rest", () -> popsAtRest(fam, seeds));
        run("feet planted", () -> feetPlanted(rig, fam, seeds));
        run("hit zones", () -> hitZones(bare, seeds));
        run("stance eases", () -> stanceEases(fam, seeds));
        run("gait", () -> gait(fam, seeds));
        run("life", () -> life(rig, fam, seeds));
        run("look and secondary", () -> lookAndSecondary(fam, seeds));
        run("actions", () -> actions(fam));
        run("death keys", MotionEngineCheck::deathKeys);
        run("death spread", () -> deathSpread(rig));
        run("hit variants", MotionEngineCheck::hitVariants);
        run("hit direction and weight", () -> hitDirectionWeight(fam));
        if (failed > 0) {
            System.out.println("MotionEngineCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionEngineCheck: " + checks + " checks passed");
    }

    static MotionMath.Seed[] seeds(int n, long at) {
        Random random = new Random(at);
        MotionMath.Seed[] seeds = new MotionMath.Seed[n];
        for (int i = 0; i < n; i++) seeds[i] = MotionMath.seed(random.nextLong(), random.nextLong());
        return seeds;
    }

    // ---- rig and fixtures ----

    record RigBone(String parent, double[] pivot, double[] rest) {}

    /** The jointed biped from fixtures/clip_rig.json, read independently of the engine. */
    static Map<String, RigBone> rig() {
        JsonObject json;
        try { json = JsonParser.parseString(Files.readString(Path.of("test-tools/standalone/fixtures/clip_rig.json"))).getAsJsonObject(); }
        catch (java.io.IOException e) { throw new AssertionError("clip_rig.json: " + e); }
        Map<String, RigBone> rig = new HashMap<>();
        for (var e : json.getAsJsonArray("bones")) {
            JsonObject b = e.getAsJsonObject();
            double[] pivot = new double[3], rest = new double[3];
            for (int i = 0; i < 3; i++) pivot[i] = b.getAsJsonArray("pivot").get(i).getAsDouble();
            if (b.has("rotation")) for (int i = 0; i < 3; i++) rest[i] = b.getAsJsonArray("rotation").get(i).getAsDouble();
            rig.put(b.get("name").getAsString(), new RigBone(b.get("parent").getAsString(), pivot, rest));
        }
        return rig;
    }

    /** Carries a point in a bone's frame out to entity space: each bone maps p to pivot + offset + R(rest + rotation) p, R turning about x, then y, then z (ModelPart). */
    static double[] world(Map<String, RigBone> rig, Map<String, double[]> pose, String bone, double[] p) {
        double[] q = p.clone();
        for (String b = bone; b != null && !b.isEmpty(); b = rig.get(b).parent()) {
            RigBone rb = rig.get(b);
            double[] ch = pose.getOrDefault(b, new double[6]);
            q = turn(q, 1, 2, rb.rest()[0] + ch[0]);
            q = turn(q, 2, 0, rb.rest()[1] + ch[1]);
            q = turn(q, 0, 1, rb.rest()[2] + ch[2]);
            for (int i = 0; i < 3; i++) q[i] += rb.pivot()[i] + ch[3 + i];
        }
        return q;
    }

    /** Right-handed rotation by deg degrees in the (a, b) plane: a toward b. */
    static double[] turn(double[] q, int a, int b, double deg) {
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r);
        double[] o = q.clone();
        o[a] = q[a] * c - q[b] * s;
        o[b] = q[a] * s + q[b] * c;
        return o;
    }

    /** Mutable test input on the engine's clock: time in seconds, ticks = time * 20. */
    static final class Drive {
        double time = 1000, limbAngle, limbSpeed, target, headYaw, headPitch, yawRate, deathSeconds;
        String stance = "";
        float aim;
        boolean shield, enemy, dead;
        MotionInput.Attack attack;
        MotionInput.Hit hit;
        final MotionMath.Seed seed;
        Drive(MotionMath.Seed seed) { this.seed = seed; }
        MotionInput in() { return new MotionInput(time, limbAngle, limbSpeed, headYaw, headPitch, yawRate, stance, aim, shield, enemy, dead, deathSeconds, attack, hit, seed); }
        long tick() { return (long) Math.floor(time * 20); }
        void hit(String zone, float bearing, int weight, boolean blocked, boolean lethal) { hit = new MotionInput.Hit("slash", zone, bearing, weight, blocked, lethal, tick()); }
        void die(float bearing) { hit("body", bearing, 2, false, true); dead = true; deathSeconds = 0; }
        /** Advances like Minecraft: limb distance eases toward its target speed and drives limb swing. */
        void step(double dt) {
            limbSpeed = MotionMath.approach(limbSpeed, target, 8, dt);
            if (limbSpeed < 1e-4 && target == 0) limbSpeed = 0;
            limbAngle += limbSpeed * 20 * dt;
            time += dt;
            if (dead) deathSeconds += dt;
        }
    }

    static String key(double t, String i, double x, double y, double z) { return "{\"t\":" + t + ",\"i\":\"" + i + "\",\"v\":[" + x + "," + y + "," + z + "]}"; }

    /** "bone:rotation|position" channels written as t,x,y,z;t,x,y,z with one interpolation for the clip. */
    static Clip clip(String name, double length, boolean loop, String markers, String interp, String... channels) {
        StringBuilder bones = new StringBuilder();
        Map<String, List<String>> perBone = new LinkedHashMap<>();
        for (String c : channels) {
            String[] head = c.split("=", 2);
            String[] bc = head[0].split(":");
            StringBuilder keys = new StringBuilder();
            for (String k : head[1].split(";")) {
                String[] v = k.split(",");
                if (keys.length() > 0) keys.append(',');
                keys.append(key(Double.parseDouble(v[0]), interp, Double.parseDouble(v[1]), Double.parseDouble(v[2]), Double.parseDouble(v[3])));
            }
            perBone.computeIfAbsent(bc[0], b -> new ArrayList<>()).add("\"" + bc[1] + "\":[" + keys + "]");
        }
        for (var e : perBone.entrySet()) {
            if (bones.length() > 0) bones.append(',');
            bones.append('"').append(e.getKey()).append("\":{").append(String.join(",", e.getValue())).append('}');
        }
        return Clip.parse("{\"format\":1,\"clip\":\"" + name + "\",\"length\":" + length + ",\"loop\":" + loop + (markers.isEmpty() ? "" : ",\"markers\":{" + markers + "}") + ",\"bones\":{" + bones + "}}");
    }

    static Map<String, Clip> stances() {
        Map<String, Clip> stances = new HashMap<>();
        stances.put("idle", clip("idle", 4, true, "", "catmullrom", "right_arm:rotation=0,-2,0,1;2,2,0,2;4,-2,0,1", "body:rotation=0,0.5,0,0;2,-0.5,0,0;4,0.5,0,0"));
        stances.put("carry", clip("carry", 0.5, false, "", "linear", "right_arm:rotation=0,-18,0,4", "right_forearm:rotation=0,-35,0,0", "left_arm:rotation=0,3,0,-2"));
        stances.put("guard", clip("guard", 3, true, "", "catmullrom", "right_arm:rotation=0,-75,12,8;1.5,-79,12,8;3,-75,12,8", "right_forearm:rotation=0,-40,0,0",
            "left_arm:rotation=0,-45,-10,-10", "left_forearm:rotation=0,-50,0,0", "body:rotation=0,0,-12,0", "root:position=0,0,1.5,0"));
        stances.put("wall", clip("wall", 0.5, false, "", "linear", "left_arm:rotation=0,-85,20,-15", "left_forearm:rotation=0,-30,0,0", "right_arm:rotation=0,-60,10,10",
            "right_forearm:rotation=0,-70,0,0", "body:rotation=0,6,0,0", "root:position=0,0,2,0"));
        stances.put("aim", clip("aim", 0.5, false, "", "linear", "right_arm:rotation=0,-90,0,0", "left_arm:rotation=0,-88,30,0", "right_forearm:rotation=0,-10,0,0"));
        stances.put("rest", clip("rest", 0.5, false, "", "linear", "right_arm:rotation=0,-30,0,0"));
        return stances;
    }

    static Clip lunge() {
        return clip("lunge", 0.8, false, "\"impact\":0.35", "bezier", "right_leg:rotation=0,0,0,0;0.35,-30,0,0;0.8,0,0,0", "right_shin:rotation=0,0,0,0;0.35,25,0,0;0.8,0,0,0",
            "root:position=0,0,0,0;0.35,0,0.5,-2;0.8,0,0,0", "body:rotation=0,0,0,0;0.35,10,0,0;0.8,0,0,0");
    }

    /**
     * A sword-and-shield style family: looping idle and guard, held poses, three attacks with impact markers (the third, a
     * lunge, keys the legs), a body hit and deaths to the front, back, left and right. Death clips are absolute relative to rest.
     */
    static MotionEngine.Family family(boolean withHits) {
        List<Clip> attacks = List.of(
            clip("slash", 0.9, false, "\"impact\":0.4", "bezier", "right_arm:rotation=0,0,0,0;0.25,-150,10,20;0.4,40,-10,-10;0.62,15,0,0;0.9,0,0,0",
                "right_forearm:rotation=0,0,0,0;0.25,-40,0,0;0.4,-5,0,0;0.9,0,0,0", "body:rotation=0,0,0,0;0.25,4,-25,0;0.4,10,30,0;0.9,0,0,0",
                "root:position=0,0,0,0;0.4,0,0.6,-1.5;0.9,0,0,0"),
            clip("thrust", 0.7, false, "\"impact\":0.3", "bezier", "right_arm:rotation=0,0,0,0;0.15,25,0,0;0.3,-95,0,0;0.45,-80,0,0;0.7,0,0,0",
                "body:rotation=0,0,0,0;0.15,-5,0,0;0.3,12,0,0;0.7,0,0,0"),
            lunge());
        Map<String, List<Clip>> hits = new HashMap<>();
        if (withHits) hits.put("body", List.of(clip("hit_body", 0.5, false, "", "bezier", "body:rotation=0,0,0,0;0.12,14,0,0;0.5,0,0,0")));
        Map<String, Clip> deaths = new HashMap<>();
        String legs = "right_leg:rotation=0,0,0,0;0.3,-5,0,0;0.9,-40,0,0;1.2,-40,0,0";
        deaths.put("any/front", clip("death_front", 1.2, false, "", "bezier", "body:rotation=0,0,0,0;0.25,-12,0,0;0.8,-85,0,0;1.2,-88,0,0",
            "root:position=0,0,0,0;0.3,0,0.5,0;0.9,0,10,2;1.2,0,10.5,2", legs, "right_shin:rotation=0,0,0,0;0.4,20,0,0;0.9,5,0,0;1.2,5,0,0"));
        deaths.put("any/back", clip("death_back", 1.2, false, "", "bezier", "body:rotation=0,0,0,0;0.25,10,0,0;0.8,80,0,0;1.2,84,0,0",
            "root:position=0,0,0,0;0.3,0,0.5,0;0.9,0,10,-2;1.2,0,10.5,-2", legs));
        deaths.put("any/left", clip("death_left", 1.2, false, "", "bezier", "body:rotation=0,0,0,0;0.8,0,0,76;1.2,0,0,80",
            "root:position=0,0,0,0;0.9,0,10,0;1.2,0,10.5,0", legs));
        deaths.put("any/right", clip("death_right", 1.2, false, "", "bezier", "body:rotation=0,0,0,0;0.8,0,0,-76;1.2,0,0,-80",
            "root:position=0,0,0,0;0.9,0,10,0;1.2,0,10.5,0", legs));
        Map<String, List<Clip>> dl = new HashMap<>();
        deaths.forEach((k, c) -> dl.put(k, List.of(c)));
        return new MotionEngine.Family("test", stances(), attacks, hits, dl);
    }

    /** K1a (R105): death_<kind>[_<dir>]_<n> variants join their key's list, sorted by name; other names keep today's keys. */
    static String deathKeys() {
        String[][] cases = {{"death", "any"}, {"death_back", "any/back"}, {"death_stab", "stab"}, {"death_stab_3", "stab"},
            {"death_slash_front", "slash/front"}, {"death_slash_front_2", "slash/front"}, {"death_left_2", "any/left"}, {"death_2", "any"},
            {"death_foo", null}, {"hit_body", null}, {"death_slash_up", null}};
        for (String[] c : cases) check(java.util.Objects.equals(MotionEngine.deathKey(c[0]), c[1]), c[0] + " -> " + MotionEngine.deathKey(c[0]));
        Clip one = clip("death_slash_front", 1, false, "", "linear", "body:rotation=0,0,0,0;1,-80,0,0");
        Clip two = clip("death_slash_front_2", 1, false, "", "linear", "body:rotation=0,0,0,0;1,-70,0,0");
        Map<String, List<Clip>> d = new HashMap<>();
        for (Clip c : List.of(two, one)) d.computeIfAbsent(MotionEngine.deathKey(c.name()), k -> new ArrayList<>()).add(c);
        var f = new MotionEngine.Family("k", Map.of(), List.of(), Map.of(), d);
        check(f.deaths().get("slash/front").size() == 2, "both variants kept");
        check(MotionEngine.deathClip(f, "slash", "front", 0) == one && MotionEngine.deathClip(f, "slash", "front", 1) == two
            && MotionEngine.deathClip(f, "slash", "front", 5) == two, "variants sorted by name, picked modulo");
        return cases.length + " names";
    }

    /** The body hit clip a state is playing last, or null. */
    static Clip lastHit(MotionEngine.State st) {
        Clip c = null;
        for (MotionStack.Track t : st.tracks) if (t.kind == MotionEngine.HIT) c = t.a;
        return c;
    }

    /** H1: the hit variant is seeded (neighbours struck on one tick differ) and never repeats back to back in a zone. */
    static String hitVariants() {
        List<Clip> body = List.of(clip("hit_body", 0.5, false, "", "linear", "body:rotation=0,0,0,0;0.12,14,0,0;0.5,0,0,0"),
            clip("hit_body_2", 0.5, false, "", "linear", "body:rotation=0,0,0,0;0.12,10,0,6;0.5,0,0,0"),
            clip("hit_body_3", 0.5, false, "", "linear", "body:rotation=0,0,0,0;0.12,12,4,0;0.5,0,0,0"));
        var fam = new MotionEngine.Family("hv", stances(), List.of(), Map.of("body", body), Map.of());
        Set<String> same = new HashSet<>();
        for (MotionMath.Seed seed : seeds(16, 8800)) {
            Drive d = new Drive(seed);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            MotionEngine.pose(d.in(), st, fam, out);
            d.step(1 / 60.0);
            d.time = 1000.5;
            d.hit("body", 0, 1, false, false);
            MotionEngine.pose(d.in(), st, fam, out);
            Clip c = lastHit(st);
            check(c != null, "a hit track");
            same.add(c.name());
        }
        check(same.size() >= 2, "16 seeds on one tick all played " + same);
        Drive d = new Drive(seeds(1, 8900)[0]);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        List<String> run = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            d.hit("body", 0, 1, false, false);
            MotionEngine.pose(d.in(), st, fam, out);
            run.add(lastHit(st).name());
            for (int k = 0; k < 40; k++) { d.step(1 / 60.0); MotionEngine.pose(d.in(), st, fam, out); }
        }
        for (int i = 1; i < run.size(); i++) check(!run.get(i).equals(run.get(i - 1)), "back-to-back repeat in " + run);
        check(new HashSet<>(run).size() == 3, "all three variants in " + run);
        Clip m = clip("hit_x", 0.5, false, "", "bezier", "right_arm:rotation=0,0,0,0;0.2,-30,8,12", "root:position=0,0,0,0;0.2,1,0.5,-1").mirror();
        Map<String, double[]> mp = new HashMap<>();
        m.sample(0.2, mp);
        check(m.name().equals("hit_x~m") && mp.containsKey("left_arm") && !mp.containsKey("right_arm"), "mirror swaps sides: " + mp.keySet());
        check(near(mp.get("left_arm")[0], -30, 1e-9) && near(mp.get("left_arm")[1], -8, 1e-9) && near(mp.get("left_arm")[2], -12, 1e-9)
            && near(mp.get("root")[3], -1, 1e-9) && near(mp.get("root")[4], 0.5, 1e-9) && near(mp.get("root")[5], -1, 1e-9), "mirror negates ry, rz and tx");
        return same.size() + " clips across 16 seeds on one tick; run " + run;
    }

    /** One seed's pose with and without a body hit, 1.5 s from the blow: the peak and recovery of the difference, and the peaks of body rx and rz. */
    static double[] hitResponse(MotionEngine.Family fam, MotionMath.Seed seed, float bearing, int weight) {
        Drive a = new Drive(seed), b = new Drive(seed);
        var sa = new MotionEngine.State();
        var sb = new MotionEngine.State();
        Pose pa = new Pose(), pb = new Pose();
        for (int i = 0; i < 60; i++) {
            MotionEngine.pose(a.in(), sa, fam, pa); MotionEngine.pose(b.in(), sb, fam, pb);
            a.step(1 / 60.0); b.step(1 / 60.0);
        }
        a.hit("body", bearing, weight, false, false);
        double peak = 0, back = 0, rx = 0, rz = 0, t0 = a.time;
        while (a.time < t0 + 1.5) {
            MotionEngine.pose(a.in(), sa, fam, pa); MotionEngine.pose(b.in(), sb, fam, pb);
            Map<String, double[]> x = pa.snapshot(), y = pb.snapshot();
            double dev = 0;
            for (var e : x.entrySet()) {
                double[] o = y.getOrDefault(e.getKey(), new double[6]);
                for (int c = 0; c < 3; c++) dev = Math.max(dev, Math.abs(e.getValue()[c] - o[c]));
            }
            double drx = x.get("body")[0] - y.get("body")[0], drz = x.get("body")[2] - y.get("body")[2];
            if (Math.abs(drx) > Math.abs(rx)) rx = drx;
            if (Math.abs(drz) > Math.abs(rz)) rz = drz;
            peak = Math.max(peak, dev);
            if (dev > 2) back = a.time - t0;
            a.step(1 / 60.0); b.step(1 / 60.0);
        }
        return new double[]{peak, back, rx, rz};
    }

    /** R106 and the weight shapes: the side a blow comes from rolls the torso away from it, front and back pitch opposite ways, and a heavy blow is bigger and slower to recover than a light one. */
    static String hitDirectionWeight(MotionEngine.Family fam) {
        MotionMath.Seed seed = seeds(1, 9100)[0];
        double[] right = hitResponse(fam, seed, 90, 1), left = hitResponse(fam, seed, -90, 1);
        double[] front = hitResponse(fam, seed, 0, 1), back = hitResponse(fam, seed, 180, 1);
        double[] light = hitResponse(fam, seed, 0, 0), heavy = hitResponse(fam, seed, 0, 2);
        check(Math.signum(right[3]) == -Math.signum(left[3]) && Math.abs(right[3] - left[3]) >= 16,
            "body rz +90 " + right[3] + " vs -90 " + left[3]);
        check(Math.signum(front[2]) == -Math.signum(back[2]), "body rx front " + front[2] + " vs back " + back[2]);
        check(heavy[0] >= 1.8 * light[0], "heavy peak " + heavy[0] + " vs light " + light[0]);
        check(heavy[1] >= 1.3 * light[1], "heavy recovery " + heavy[1] + " s vs light " + light[1] + " s");
        return String.format("body rz +90 %.1f / -90 %.1f; body rx front %.1f / back %.1f; peak heavy %.1f / light %.1f (x%.2f); recovery %.2f / %.2f s (x%.2f)",
            right[3], left[3], front[2], back[2], heavy[0], light[0], heavy[0] / light[0], heavy[1], light[1], heavy[1] / light[1]);
    }

    /** Every box corner of the rig, posed: how far the lowest one sits above the ground at y = 24 (px; negative is under it). */
    static double lowest(Map<String, RigBone> rig, Map<String, double[]> pose) {
        JsonObject json;
        try { json = JsonParser.parseString(Files.readString(Path.of("test-tools/standalone/fixtures/clip_rig.json"))).getAsJsonObject(); }
        catch (java.io.IOException e) { throw new AssertionError("clip_rig.json: " + e); }
        double low = Double.POSITIVE_INFINITY;
        for (var e : json.getAsJsonArray("bones")) {
            JsonObject b = e.getAsJsonObject();
            for (var x : b.getAsJsonArray("boxes")) {
                JsonObject box = x.getAsJsonObject();
                double[] o = new double[3], z = new double[3];
                for (int i = 0; i < 3; i++) { o[i] = box.getAsJsonArray("origin").get(i).getAsDouble(); z[i] = box.getAsJsonArray("size").get(i).getAsDouble(); }
                for (int c = 0; c < 8; c++) {
                    double[] q = {o[0] + ((c & 1) != 0 ? z[0] : 0), o[1] + ((c & 2) != 0 ? z[1] : 0), o[2] + ((c & 4) != 0 ? z[2] : 0)};
                    low = Math.min(low, 24 - world(rig, pose, b.get("name").getAsString(), q)[1]);
                }
            }
        }
        return low;
    }

    /**
     * K1b: twelve seeds dying to the same slash from the front, with the real death_slash_front and a renamed variant
     * death_slash_front_2 parsed through deathKey. Both variants are picked; the seed tempo spreads the end times (each within
     * [L / 1.1, L / 0.9]); half a second past the end every pose is the authored end pose and lies on the ground; and the seed
     * amp changes the fall's peak root pitch rate.
     */
    static String deathSpread(Map<String, RigBone> rig) {
        String json;
        try { json = Files.readString(Path.of("src/main/resources/assets/gravesandglory/clips/biped/common/death_slash_front.json")); }
        catch (java.io.IOException e) { throw new AssertionError("death_slash_front.json: " + e); }
        Clip one = Clip.parse(json), two = Clip.parse(json.replace("\"death_slash_front\"", "\"death_slash_front_2\""));
        check(two.name().equals("death_slash_front_2"), "renamed variant");
        Map<String, List<Clip>> d = new HashMap<>();
        for (Clip c : List.of(one, two)) {
            String k = MotionEngine.deathKey(c.name());
            check(k != null, c.name() + " has a death key");
            d.computeIfAbsent(k, x -> new ArrayList<>()).add(c);
        }
        var fam = new MotionEngine.Family("spread", stances(), List.of(), Map.of(), d);
        MotionMath.Seed[] seeds = seeds(12, 7700);
        Set<String> picked = new HashSet<>();
        double minL = 1e9, maxL = 0, sumL = 0, worstEnd = 0, low = 1e9, minAmp = 9, maxAmp = 0, vMinAmp = 0, vMaxAmp = 0;
        double L = one.length();
        for (MotionMath.Seed seed : seeds) {
            Drive dr = new Drive(seed);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            for (int i = 0; i < 60; i++) { MotionEngine.pose(dr.in(), st, fam, out); dr.step(1 / 60.0); }
            dr.die(0);
            MotionEngine.pose(dr.in(), st, fam, out);
            MotionStack.Track tr = null;
            for (MotionStack.Track t : st.tracks) if (t.kind == MotionEngine.DEATH) tr = t;
            check(tr != null, "a death track");
            picked.add(tr.a.name());
            double len = tr.end() - tr.start;
            minL = Math.min(minL, len); maxL = Math.max(maxL, len); sumL += len;
            check(len >= L / 1.1 - 1e-9 && len <= L / 0.9 + 1e-9, "end time " + len + " outside [L/1.1, L/0.9] for L " + L);
            double end = tr.end(), prev = out.get("root", 0), peak = 0;
            while (dr.time < end + 0.5) {
                dr.step(1 / 60.0);
                MotionEngine.pose(dr.in(), st, fam, out);
                double rx = out.get("root", 0);
                if (dr.time <= end + 1e-9) peak = Math.max(peak, Math.abs(rx - prev) * 60);
                prev = rx;
            }
            Map<String, double[]> want = new HashMap<>();
            tr.a.sample(tr.a.length(), want);
            Map<String, double[]> got = out.snapshot();
            for (var e : want.entrySet()) {
                double[] g = got.getOrDefault(e.getKey(), new double[6]);
                for (int c = 0; c < 6; c++) worstEnd = Math.max(worstEnd, Math.abs(g[c] - e.getValue()[c]));
            }
            low = Math.min(low, lowest(rig, got));
            if (seed.amp() < minAmp) { minAmp = seed.amp(); vMinAmp = peak; }
            if (seed.amp() > maxAmp) { maxAmp = seed.amp(); vMaxAmp = peak; }
        }
        double spread = (maxL - minL) / (sumL / seeds.length);
        check(picked.size() == 2, "variants picked " + picked);
        check(spread >= 0.12, "end-time spread " + spread);
        check(worstEnd <= 1e-6, "end pose off the authored end by " + worstEnd);
        check(low >= -0.16, "lowest point " + low + " px");
        double diff = Math.abs(vMaxAmp - vMinAmp) / Math.min(vMaxAmp, vMinAmp);
        check(diff >= 0.08, "peak root rx rate differs " + diff + " (amp " + minAmp + ": " + vMinAmp + ", amp " + maxAmp + ": " + vMaxAmp + ")");
        return String.format("variants %s, spread %.3f, end off %.1e, lowest %.3f px, root rate amp %.3f %.1f vs amp %.3f %.1f (%.1f%%)",
            picked, spread, worstEnd, low, minAmp, vMinAmp, maxAmp, vMaxAmp, 100 * diff);
    }

    static boolean at(double elapsed, double when) { return Math.abs(elapsed - when) < 0.5 / 60; }

    // ---- pops ----

    /** Tracks the worst per-frame move against the 12 degree / 1.5 px bar, less what each playing clip's own keys move. */
    static class Pops {
        Map<String, double[]> prev;
        Map<Integer, Double> prevTimes = new HashMap<>();
        double worst;
        String where = "none";
        final Map<String, double[]> a = new HashMap<>(), b = new HashMap<>();

        double extra(String bone, int c) { return 0; }

        void frame(String tag, Pose out, MotionEngine.State st, double now, double dt) {
            Map<String, double[]> cur = out.snapshot();
            List<MotionEngine.Playing> playing = MotionEngine.playing(st, now);
            Map<Integer, Double> times = new HashMap<>();
            for (var p : playing) times.put(p.id(), p.time());
            if (prev != null) {
                Set<String> bones = new HashSet<>(cur.keySet());
                bones.addAll(prev.keySet());
                for (String bone : bones) {
                    double[] n = cur.getOrDefault(bone, new double[6]), o = prev.getOrDefault(bone, new double[6]);
                    for (int c = 0; c < 6; c++) {
                        double allow = (c < 3 ? 12 : 1.5) * Math.max(1, dt * 60) + extra(bone, c);
                        for (var p : playing) {
                            Double before = prevTimes.get(p.id());
                            if (before == null) continue;
                            a.clear(); b.clear();
                            p.clip().sample(before, a);
                            p.clip().sample(p.time(), b);
                            if (a.containsKey(bone)) allow += Math.abs(p.gain()) * Math.abs(b.get(bone)[c] - a.get(bone)[c]);
                        }
                        double ratio = Math.abs(n[c] - o[c]) / allow;
                        if (!(ratio <= worst)) { worst = ratio; where = tag + " " + bone + " ch " + c + ": " + String.format("%.3f", n[c] - o[c]) + " allowed " + String.format("%.3f", allow); }
                    }
                }
            }
            prev = cur;
            prevTimes = times;
        }
    }

    /** The largest per-frame move of any channel against the 12 degree / 1.5 px bar, with no clip exemption. */
    static double ratio(Map<String, double[]> prev, Map<String, double[]> cur) {
        double worst = 0;
        Set<String> bones = new HashSet<>(cur.keySet());
        bones.addAll(prev.keySet());
        for (String bone : bones) {
            double[] n = cur.getOrDefault(bone, new double[6]), o = prev.getOrDefault(bone, new double[6]);
            for (int c = 0; c < 6; c++) worst = Math.max(worst, Math.abs(n[c] - o[c]) / (c < 3 ? 12 : 1.5));
        }
        return worst;
    }

    /** The pop schedule at speed 0 through the frame API (MotionLayersCheck runs it at every speed with the gait exempted). */
    static String popsAtRest(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worst = 0;
        String where = "none";
        for (int s = 0; s < 3; s++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            Pops pops = new Pops();
            double e = 0;
            while (e < 32) {
                double dt = schedule(d, e, 0);
                MotionEngine.pose(d.in(), st, fam, out);
                pops.frame("seed " + s + " t " + String.format("%.3f", e), out, st, d.time, dt);
                double next = at(e, 23.5) ? 0.1 : 1 / 60.0;
                d.step(next);
                e += next;
            }
            if (pops.worst > worst) { worst = pops.worst; where = pops.where; }
        }
        check(worst <= 1, "no pops at rest: worst ratio " + worst + " at " + where);
        return String.format("worst ratio %.3f at %s", worst, where);
    }

    /**
     * The shared pop schedule: every stance and order, attacks (one late, one leg-keying), hits in every zone, a turn in
     * place, look swings, enemy flicker, a 0.1 s frame hitch, shield toggles, a lethal blow and a revive. Returns the dt
     * that led into this frame.
     */
    static double schedule(Drive d, double e, double speed) {
        if (at(e, 0)) d.target = d.limbSpeed = speed;
        if (at(e, 2.0)) d.enemy = true;
        if (at(e, 3.5)) d.attack = new MotionInput.Attack(0, d.tick());
        if (at(e, 4.1)) d.hit("body", 20, 1, false, false);
        if (at(e, 4.6)) d.attack = new MotionInput.Attack(1, d.tick() - 5);
        if (at(e, 5.0)) d.attack = new MotionInput.Attack(0, d.tick());
        if (at(e, 5.3)) d.hit("head", -150, 2, false, false);
        if (at(e, 6.0)) d.stance = "wall";
        if (at(e, 7.0)) d.shield = true;
        if (at(e, 7.8)) d.hit("body", 0, 1, true, false);
        if (at(e, 8.5)) { d.shield = false; d.stance = "ranged_aim"; }
        if (d.stance.equals("ranged_aim")) d.aim = (float) (25 * Math.sin(e));
        if (at(e, 9.0)) d.target = 0.6;
        if (at(e, 10.0)) { d.stance = "ranged_rest"; d.target = speed; }
        if (at(e, 11.0)) d.stance = "";
        if (at(e, 11.5)) d.attack = new MotionInput.Attack(2, d.tick());
        if (at(e, 12.0)) d.stance = "fall_back";
        if (at(e, 13.0)) d.stance = "run";
        if (at(e, 14.5)) { d.shield = true; d.stance = "shield_move"; }
        if (at(e, 15.5)) d.attack = new MotionInput.Attack(1, d.tick());
        if (at(e, 16.0)) { d.shield = false; d.stance = "hold"; }
        if (at(e, 16.5)) d.yawRate = 140;
        if (at(e, 17.5)) d.yawRate = 0;
        if (at(e, 18.0)) d.headYaw = 80;
        if (at(e, 18.6)) d.headYaw = -80;
        if (at(e, 19.2)) { d.headYaw = 0; d.enemy = false; d.stance = ""; }
        if (at(e, 20.0)) d.hit("legs", 0, 2, false, false);
        if (at(e, 21.0)) { d.hit("head", 90, 0, false, false); d.attack = new MotionInput.Attack(0, d.tick()); }
        if (e > 21.5 && e < 23 && Math.round(e * 60) % 6 == 0) d.enemy = !d.enemy;
        if (at(e, 23.0)) d.enemy = true;
        if (e > 24 && e < 25.2 && Math.round(e * 60) % 18 == 0) d.shield = !d.shield;
        if (at(e, 25.3)) d.shield = false;
        if (at(e, 26.0)) d.die(10);
        if (at(e, 30.0)) { d.dead = false; d.hit = null; }
        return e > 23.5 + 0.5 / 60 && e < 23.6 + 0.5 / 60 ? 0.1 : 1 / 60.0;
    }

    // ---- the section 6.1 behaviours carried over from Task 12 ----

    /** Feet planted: standing with speed 0, through stance changes, hits, a hip-moving attack and look changes, the feet move under 0.05 px. */
    static String feetPlanted(Map<String, RigBone> rig, MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worst = 0;
        String where = "none";
        for (int s = 0; s < 8; s++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double[][] first = null;
            for (int f = 0; f < 60 * 25; f++) {
                double e = f / 60.0;
                if (at(e, 6)) d.enemy = true;
                if (at(e, 9)) d.hit("head", 0, 2, false, false);
                if (at(e, 11)) d.hit("legs", 160, 2, false, false);
                if (at(e, 12)) d.attack = new MotionInput.Attack(0, d.tick());
                if (at(e, 14)) d.enemy = false;
                if (at(e, 17)) d.hit("body", 30, 1, false, false);
                if (at(e, 20)) d.headYaw = 70;
                if (at(e, 22)) d.stance = "wall";
                MotionEngine.pose(d.in(), st, fam, out);
                Map<String, double[]> pose = out.snapshot();
                double[][] feet = {world(rig, pose, "right_shin", new double[]{0, 6, 0}), world(rig, pose, "left_shin", new double[]{0, 6, 0})};
                if (first == null) first = feet;
                for (int k = 0; k < 2; k++) {
                    double dist = Math.sqrt(Math.pow(feet[k][0] - first[k][0], 2) + Math.pow(feet[k][1] - first[k][1], 2) + Math.pow(feet[k][2] - first[k][2], 2));
                    if (!(dist <= worst)) { worst = dist; where = "seed " + s + " t " + String.format("%.3f", e) + (k == 0 ? " right" : " left") + " foot"; }
                }
                d.step(1 / 60.0);
            }
        }
        check(worst < 0.05, "feet planted: a standing foot moved " + worst + " px, worst at " + where);
        return String.format("worst %.5f px", worst);
    }

    /** Hit zones: a head hit's peak rotation is largest on the head, a legs hit's on the shins (procedural reactions, no hit clips). */
    static String hitZones(MotionEngine.Family bare, MotionMath.Seed[] seeds) {
        String[] bones = {"root", "body", "head", "right_arm", "right_forearm", "left_arm", "left_forearm", "right_leg", "right_shin", "left_leg", "left_shin"};
        for (int s = 0; s < 4; s++) for (String zone : new String[]{"head", "legs"}) for (float bearing : new float[]{0, 180, -60}) for (int weight : new int[]{0, 2}) {
            double[][] base = hitTrace(bare, seeds[s], null, 0, 0, bones), hit = hitTrace(bare, seeds[s], zone, bearing, weight, bones);
            double[] peak = new double[bones.length];
            for (int f = 0; f < base.length; f++) for (int i = 0; i < bones.length * 3; i++) peak[i / 3] = Math.max(peak[i / 3], Math.abs(hit[f][i] - base[f][i]));
            int top = 0;
            for (int i = 1; i < bones.length; i++) if (peak[i] > peak[top]) top = i;
            boolean ok = zone.equals("head") ? bones[top].equals("head") : bones[top].endsWith("_shin");
            check(ok && peak[top] > 8, "hit zones: a " + zone + " hit (bearing " + bearing + ", weight " + weight + ", seed " + s + ") peaked on " + bones[top] + " at " + peak[top] + "; all " + Arrays.toString(peak));
        }
        return "";
    }

    static double[][] hitTrace(MotionEngine.Family fam, MotionMath.Seed seed, String zone, float bearing, int weight, String[] bones) {
        Drive d = new Drive(seed);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        double[][] trace = new double[90][bones.length * 3];
        for (int f = 0; f < 90; f++) {
            if (f == 18 && zone != null) d.hit(zone, bearing, weight, false, false);
            MotionEngine.pose(d.in(), st, fam, out);
            for (int b = 0; b < bones.length; b++) for (int c = 0; c < 3; c++) trace[f][b * 3 + c] = out.get(bones[b], c);
            d.step(1 / 60.0);
        }
        return trace;
    }

    /** Stances blend with cosine-eased, time-smoothed weights: no instant switch, a slow start and a settled end. */
    static String stanceEases(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        for (int s = 0; s < 4; s++) {
            double[] calm = armTrace(fam, seeds[s], 0), swap = armTrace(fam, seeds[s], 1), guard = armTrace(fam, seeds[s], 2);
            int at = 120;
            check(Math.abs(swap[at - 1] - calm[at - 1]) < 1e-9, "nothing should change before the swap");
            check(Math.abs(guard[at] - calm[at]) > 40, "the guard should lift the sword arm, moved " + (guard[at] - calm[at]));
            double early = (swap[at + 3] - calm[at + 3]) / (guard[at + 3] - calm[at + 3]), mid = (swap[at + 14] - calm[at + 14]) / (guard[at + 14] - calm[at + 14]);
            double late = (swap[at + 40] - calm[at + 40]) / (guard[at + 40] - calm[at + 40]);
            check(early >= 0 && early < 0.08, "a stance swap should start slowly (cosine ease), 0.05 s in: " + early);
            check(mid > 0.3 && mid < 0.75, "a stance swap should be about halfway by 0.25 s: " + mid);
            check(late > 0.97 && late < 1.03, "a stance swap should settle within about 0.5 s: " + late);
        }
        return "";
    }

    /** The sword arm's pitch: calm throughout (mode 0), an enemy appearing at 2 s (1), or an enemy from the start (2). */
    static double[] armTrace(MotionEngine.Family fam, MotionMath.Seed seed, int mode) {
        Drive d = new Drive(seed);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        double[] trace = new double[300];
        d.enemy = mode == 2;
        for (int f = 0; f < trace.length; f++) {
            if (f == 120 && mode == 1) d.enemy = true;
            MotionEngine.pose(d.in(), st, fam, out);
            trace[f] = out.get("right_arm", 0);
            d.step(1 / 60.0);
        }
        return trace;
    }

    /** Locomotion: bob at twice the gait frequency, knees bend on the passing leg, and walk and run cross-fade on speed. */
    static String gait(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        for (int s = 0; s < 4; s++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            d.target = d.limbSpeed = 0.5;
            double h1r = 0, h1i = 0, h2r = 0, h2i = 0, fwd = 0, back = 0, prevLeg = Double.NaN;
            int nf = 0, nb = 0;
            for (int f = 0; f < 60 * 8; f++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (f > 120) {
                    double p = d.limbAngle * 0.6662 + seeds[s].phase(), ty = out.get("root", 4);
                    h1r += ty * Math.cos(p); h1i += ty * Math.sin(p); h2r += ty * Math.cos(2 * p); h2i += ty * Math.sin(2 * p);
                    double leg = out.get("right_leg", 0), shin = out.get("right_shin", 0);
                    if (!Double.isNaN(prevLeg)) { if (leg < prevLeg) { fwd += shin; nf++; } else { back += shin; nb++; } }
                    prevLeg = leg;
                }
                d.step(1 / 60.0);
            }
            double h1 = Math.hypot(h1r, h1i), h2 = Math.hypot(h2r, h2i);
            check(h2 > 3 * h1 && h2 > 0, "the body should bob at twice the gait frequency: 1x " + h1 + ", 2x " + h2);
            check(fwd / nf > 3 * Math.max(back / nb, 0) + 5, "the knee should bend on the passing (forward-swinging) leg: forward " + fwd / nf + ", back " + back / nb);
        }
        double prevAmp = 0, prevElbow = 0;
        for (int i = 3; i <= 10; i++) {
            double speed = i / 10.0;
            Drive d = new Drive(seeds[0]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            d.target = d.limbSpeed = speed;
            double amp = 0, elbow = 0;
            for (int f = 0; f < 60 * 5; f++) {
                MotionEngine.pose(d.in(), st, fam, out);
                if (f > 120) { amp = Math.max(amp, Math.abs(out.get("left_leg", 0))); elbow += out.get("left_forearm", 0) / (60 * 5 - 121); }
                d.step(1 / 60.0);
            }
            check(amp >= prevAmp - 1, "stride should not shrink as speed rises: " + prevAmp + " then " + amp + " at " + speed);
            check(elbow <= prevElbow + 1, "elbows should bend more toward a run: " + prevElbow + " then " + elbow + " at " + speed);
            if (i > 3) check(Math.abs(elbow - prevElbow) < 15 && amp - prevAmp < 15, "walk and run should cross-fade, not switch, at speed " + speed + ": elbow " + prevElbow + " -> " + elbow + ", stride " + prevAmp + " -> " + amp);
            prevAmp = amp; prevElbow = elbow;
        }
        return "";
    }

    /** Life: novelty idles play on calm units only, and the hips shift toward the seed's idle leg. */
    static String life(Map<String, RigBone> rig, MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        for (int s = 0; s < 6; s++) {
            int[] counts = new int[3];
            double hip = 0;
            for (int mode = 0; mode < 3; mode++) {
                Drive d = new Drive(seeds[s]);
                var st = new MotionEngine.State();
                Pose out = new Pose();
                d.enemy = mode == 1;
                if (mode == 2) d.target = d.limbSpeed = 0.5;
                for (int f = 0; f < 60 * 90; f++) {
                    MotionEngine.pose(d.in(), st, fam, out);
                    if (mode == 0) hip += world(rig, out.snapshot(), "body", new double[3])[0];
                    d.step(1 / 60.0);
                }
                counts[mode] = st.novelties;
            }
            check(counts[0] >= 4 && counts[0] <= 16, "a calm unit should play a novelty idle every 6 to 14 s: " + counts[0] + " in 90 s");
            check(counts[1] == 0 && counts[2] == 0, "no novelty idles in combat or on the move: " + counts[1] + ", " + counts[2]);
            double side = seeds[s].idleLeg() == 0 ? -1 : 1, mean = hip / (60 * 90);
            check(mean * side > 0.2, "the hips should shift toward the idle leg " + seeds[s].idleLeg() + ": mean hip x " + mean);
        }
        return "";
    }

    /** Look: smoothed head yaw, part of a large yaw passes into body twist; secondary bones spring-lag and settle. */
    static String lookAndSecondary(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        for (int s = 0; s < 3; s++) {
            double[][] base = lookTrace(fam, seeds[s], 0, 0), look = lookTrace(fam, seeds[s], 80, 0), spin = lookTrace(fam, seeds[s], 0, 240);
            int end = base.length - 1;
            double head = look[end][0] - base[end][0], body = look[end][1] - base[end][1], crest = 0, cape = 0;
            for (int f = 0; f <= end; f++) { crest = Math.max(crest, Math.abs(look[f][2] - base[f][2])); cape = Math.max(cape, Math.abs(spin[f][3] - base[f][3])); }
            check(Math.abs(head + body - 80) < 1, "head plus body twist should reach the look yaw: " + head + " + " + body);
            check(body > 10 && body < 25, "a large yaw should pass part into body twist: " + body);
            check(crest > 3, "a crest should lag the head while it turns: " + crest);
            check(Math.abs(look[end][2] - base[end][2]) < 0.3, "the crest should settle once the head stops: " + (look[end][2] - base[end][2]));
            check(cape > 2, "a cape should swing while the body turns: " + cape);
            check(Math.abs(spin[end][3] - base[end][3]) < 0.3, "the cape should settle after the turn: " + (spin[end][3] - base[end][3]));
        }
        return "";
    }

    /** Per frame {head ry, body plus root ry, crest rz, cape rz}, with a look of {@code yaw} from 1 s and a body turn at {@code rate} from 1 s to 1.5 s. */
    static double[][] lookTrace(MotionEngine.Family fam, MotionMath.Seed seed, double yaw, double rate) {
        Drive d = new Drive(seed);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        double[][] trace = new double[60 * 5][];
        for (int f = 0; f < trace.length; f++) {
            if (f == 60) { d.headYaw = yaw; d.yawRate = rate; }
            if (f == 90) d.yawRate = 0;
            MotionEngine.pose(d.in(), st, fam, out);
            trace[f] = new double[]{out.get("head", 1), out.get("body", 1) + out.get("root", 1), out.get("crest", 2), out.get("cape", 2)};
            d.step(1 / 60.0);
        }
        return trace;
    }

    /** Action: the impact lands on the server's tick at any tempo, attacks scale per unit, hits override attacks. */
    static String actions(MotionEngine.Family fam) {
        Random r = new Random(5);
        MotionMath.Seed slow = null, fast = null;
        while (slow == null || fast == null) {
            var s = MotionMath.seed(r.nextLong(), r.nextLong());
            if (s.tempo() < 0.93 && slow == null) slow = s;
            if (s.tempo() > 1.07 && fast == null) fast = s;
        }
        for (MotionMath.Seed seed : new MotionMath.Seed[]{slow, fast}) {
            Drive d = new Drive(seed);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            MotionEngine.pose(d.in(), st, fam, out);
            long start = d.tick() + 1;
            d.attack = new MotionInput.Attack(0, start);
            MotionEngine.Playing seen = null;
            double impactTime = 0, after = 0, gain = 0;
            for (int f = 0; f < 60; f++) {
                d.step(1 / 60.0);
                MotionEngine.pose(d.in(), st, fam, out);
                var playing = MotionEngine.playing(st, d.time);
                if (playing.isEmpty()) continue;
                seen = playing.get(playing.size() - 1);
                gain = Math.max(gain, seen.gain());
                double elapsed = d.time - start / 20.0, impact = seen.clip().impact() / 20.0;
                if (elapsed <= impact) check(near(seen.time(), Math.max(0, elapsed), 1e-9), "before impact the attack plays in real time so the impact lands on the server tick: " + seen.time() + " vs " + elapsed);
                else { impactTime = impact; after = elapsed; check(near(seen.time(), impact + (elapsed - impact) * seed.tempo(), 1e-9), "after impact the follow-through plays at the unit's tempo " + seed.tempo()); }
            }
            check(seen != null && after > impactTime, "the attack should play through its impact");
            check(near(gain, seed.amp(), 1e-12), "attacks scale by the unit's amplitude: " + gain + " vs " + seed.amp());
            check(seen.clip().name().equals("slash"), "variant 0 plays the first attack clip: " + seen.clip().name());
        }
        Drive d = new Drive(slow);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        d.attack = new MotionInput.Attack(1, d.tick());
        MotionEngine.pose(d.in(), st, fam, out);
        d.step(1 / 60.0);
        d.hit("body", 0, 1, false, false);
        MotionEngine.pose(d.in(), st, fam, out);
        var playing = MotionEngine.playing(st, d.time);
        check(playing.get(playing.size() - 1).clip().name().equals("hit_body"), "a hit overrides the attack: " + playing);
        return "";
    }
}
