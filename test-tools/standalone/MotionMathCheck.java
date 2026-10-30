package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Pure motion maths for the troop motion engine (animation rework design section 6): easing, the gait and triangle waves, frame-rate-independent approach, the sub-stepped spring, per-unit seeds, Clip sampling against Blockbench's own curves and the layered MotionEngine (no pops, no clones, planted feet, stability, hit zones and the section 6.1 layer behaviours). Never shipped. */
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
        check(MotionMath.approach(0.4, 1, -3, 0.1) == 0.4 && MotionMath.approach(0.4, 1, Double.NaN, 0.1) == 0.4, "approach should treat a negative or NaN rate as 0");

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
        var from = new MotionMath.Spring(0.3, 0.2);
        MotionMath.Spring slack = MotionMath.spring(from, 1, 0, 20, 0.1), loose = MotionMath.spring(from, 1, 200, 0, 0.1);
        check(MotionMath.spring(from, 1, -200, 20, 0.1).equals(slack) && MotionMath.spring(from, 1, Double.NaN, 20, 0.1).equals(slack), "spring should treat a negative or NaN stiffness as 0");
        check(MotionMath.spring(from, 1, 200, -20, 0.1).equals(loose) && MotionMath.spring(from, 1, 200, Double.NaN, 0.1).equals(loose), "spring should treat a negative or NaN damping as 0");

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

        clips();
        clipReach();
        engine();
        System.out.println("MotionMathCheck: " + checks + " checks passed");
    }

    /** fixtures/clip_bezier.json holds clips sampled by Blockbench 5.2.2's own BoneAnimator.interpolate and Keyframe lerps (its "source" says how). */
    static void clips() {
        JsonObject fixture;
        try { fixture = JsonParser.parseString(Files.readString(Path.of("test-tools/standalone/fixtures/clip_bezier.json"))).getAsJsonObject(); }
        catch (java.io.IOException e) { throw new AssertionError("clip_bezier.json: " + e); }
        Map<String, double[]> out = new HashMap<>();
        Clip clip = null;
        for (var c : fixture.getAsJsonArray("clips")) {
            Clip each = Clip.parse(c.getAsJsonObject().getAsJsonObject("clip"));
            if (each.name().equals("reference")) clip = each;
            var samples = c.getAsJsonObject().getAsJsonArray("samples");
            check(samples.size() > 0, each.name() + " should hold samples");
            for (var e : samples) {
                JsonObject s = e.getAsJsonObject();
                double t = s.get("t").getAsDouble();
                int at = s.get("channel").getAsString().equals("rotation") ? 0 : 3;
                each.sample(t, out);
                double[] got = out.get(s.get("bone").getAsString());
                for (int i = 0; i < 3; i++) {
                    double want = s.getAsJsonArray("value").get(i).getAsDouble();
                    check(near(got[at + i], want, 1e-3), each.name() + " " + s.get("bone").getAsString() + " " + s.get("channel").getAsString() + " axis " + i + " at " + t + ": " + got[at + i] + ", Blockbench " + want);
                }
            }
        }
        check(clip != null, "the fixture should hold the reference clip");
        clip.sample(0.2, out);
        check(near(out.get("right_arm")[0], 10, 1e-12), "on a key, sampling reads its pre value: " + out.get("right_arm")[0]);
        clip.sample(0.2 + 0.0005, out);
        check(near(out.get("right_arm")[0], 10, 1e-12), "within 1/1200 s of a key Blockbench still reads the key: " + out.get("right_arm")[0]);
        clip.sample(0.201, out);
        check(near(out.get("right_arm")[0], 14, 0.5) && near(out.get("right_arm")[2], 0, 0.5), "just past a split key the curve leaves from its post value: " + out.get("right_arm")[0] + ", " + out.get("right_arm")[2]);
        clip.sample(5, out);
        check(near(out.get("right_arm")[0], 80, 1e-12) && near(out.get("body")[5], -2, 1e-12) && out.get("right_arm")[3] == 0, "a clip that does not loop holds its last frame; unkeyed channels are 0");
        double[] reach = clip.reach();
        check(reach.length == 6 && reach[0] >= 80 && reach[2] >= 40 && reach[4] >= 3 && reach[4] < 3.5, "reach should bound every channel: " + java.util.Arrays.toString(reach));
        check(clip.impact() == -1 && !clip.loop() && clip.length() == 1, "the reference clip has no impact, does not loop and lasts 1 s");

        Clip loop = Clip.parse(JsonParser.parseString("{\"format\":1,\"clip\":\"idle\",\"length\":1.2,\"loop\":true,\"markers\":{\"impact\":0.35,\"step\":0.6},"
            + "\"bones\":{\"body\":{\"rotation\":[{\"t\":0,\"i\":\"step\",\"v\":[1,0,0]},{\"t\":0.6,\"i\":\"linear\",\"v\":[3,0,0]},{\"t\":1.2,\"i\":\"linear\",\"v\":[1,0,0]}]}}}").getAsJsonObject());
        check(loop.impact() == 7 && loop.markers().get("step") == 0.6, "impact is the impact marker in ticks: " + loop.impact());
        loop.sample(0.3, out);
        check(out.get("body")[0] == 1, "a step key holds until the next key: " + out.get("body")[0]);
        loop.sample(0.9, out);
        double mid = out.get("body")[0];
        loop.sample(0.9 + 1.2 * 3, out);
        check(near(mid, 2, 1e-12) && near(out.get("body")[0], mid, 1e-9), "a looping clip wraps its time: " + mid + " vs " + out.get("body")[0]);
    }

    /** Clip.reach is a bound, not a sample: it must cover a bezier spike and a catmullrom overshoot that 1/100 s sampling steps over. */
    static void clipReach() {
        Clip spike = Clip.parse("{\"format\":1,\"clip\":\"spike\",\"length\":1,\"bones\":{\"right_arm\":{\"rotation\":["
            + "{\"t\":0,\"i\":\"bezier\",\"v\":[0,0,0]},"
            + "{\"t\":0.501,\"i\":\"bezier\",\"v\":[0,0,0],\"rt\":[0.002,0.002,0.002],\"rv\":[60,0,0]},"
            + "{\"t\":0.506,\"i\":\"bezier\",\"v\":[0,0,0],\"lt\":[-0.002,-0.002,-0.002],\"lv\":[60,0,0]},"
            + "{\"t\":1,\"i\":\"bezier\",\"v\":[0,0,0]}]}}}");
        Clip over = Clip.parse("{\"format\":1,\"clip\":\"over\",\"length\":0.03,\"loop\":false,\"bones\":{\"body\":{\"position\":["
            + "{\"t\":0,\"i\":\"catmullrom\",\"v\":[0,0,0]},{\"t\":0.01,\"i\":\"catmullrom\",\"v\":[0,10,0]},"
            + "{\"t\":0.02,\"i\":\"catmullrom\",\"v\":[0,10,0]},{\"t\":0.03,\"i\":\"catmullrom\",\"v\":[0,0,0]}]}}}");
        Map<String, double[]> out = new HashMap<>();
        for (Clip c : new Clip[]{spike, over}) {
            double[] dense = new double[6];
            for (int i = 0; i <= 200000; i++) {
                c.sample(c.length() * i / 200000.0, out);
                for (double[] o : out.values()) for (int ch = 0; ch < 6; ch++) dense[ch] = Math.max(dense[ch], Math.abs(o[ch]));
            }
            double[] reach = c.reach();
            for (int ch = 0; ch < 6; ch++) {
                check(reach[ch] >= dense[ch] - 1e-9, c.name() + " reach channel " + ch + " " + reach[ch] + " misses the densely sampled " + dense[ch]);
                check(reach[ch] <= dense[ch] + 1e-3 * Math.max(1, dense[ch]), c.name() + " reach channel " + ch + " " + reach[ch] + " should be tight to " + dense[ch]);   // a lerp between curve points just misses the top one
            }
        }
        check(spike.reach()[0] > 20 && over.reach()[4] > 10.5, "the fixtures should overshoot: " + spike.reach()[0] + ", " + over.reach()[4]);
    }

    // ---- MotionEngine (animation rework design section 6.1) ----

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

    /** A sword-and-shield style family: looping idle and guard, held poses, two attacks with impact markers, a body hit and front/back deaths. */
    static MotionEngine.Family family(boolean withHits) {
        Map<String, Clip> stances = new HashMap<>();
        stances.put("idle", clip("idle", 4, true, "", "catmullrom", "right_arm:rotation=0,-2,0,1;2,2,0,2;4,-2,0,1", "body:rotation=0,0.5,0,0;2,-0.5,0,0;4,0.5,0,0"));
        stances.put("carry", clip("carry", 0.5, false, "", "linear", "right_arm:rotation=0,-18,0,4", "right_forearm:rotation=0,-35,0,0", "left_arm:rotation=0,3,0,-2"));
        stances.put("guard", clip("guard", 3, true, "", "catmullrom", "right_arm:rotation=0,-75,12,8;1.5,-79,12,8;3,-75,12,8", "right_forearm:rotation=0,-40,0,0",
            "left_arm:rotation=0,-45,-10,-10", "left_forearm:rotation=0,-50,0,0", "body:rotation=0,0,-12,0", "root:position=0,0,1.5,0"));
        stances.put("wall", clip("wall", 0.5, false, "", "linear", "left_arm:rotation=0,-85,20,-15", "left_forearm:rotation=0,-30,0,0", "right_arm:rotation=0,-60,10,10",
            "right_forearm:rotation=0,-70,0,0", "body:rotation=0,6,0,0", "root:position=0,0,2,0"));
        stances.put("aim", clip("aim", 0.5, false, "", "linear", "right_arm:rotation=0,-90,0,0", "left_arm:rotation=0,-88,30,0", "right_forearm:rotation=0,-10,0,0"));
        stances.put("rest", clip("rest", 0.5, false, "", "linear", "right_arm:rotation=0,-30,0,0"));
        List<Clip> attacks = List.of(
            clip("slash", 0.9, false, "\"impact\":0.4", "bezier", "right_arm:rotation=0,0,0,0;0.25,-150,10,20;0.4,40,-10,-10;0.62,15,0,0;0.9,0,0,0",
                "right_forearm:rotation=0,0,0,0;0.25,-40,0,0;0.4,-5,0,0;0.9,0,0,0", "body:rotation=0,0,0,0;0.25,4,-25,0;0.4,10,30,0;0.9,0,0,0",
                "root:position=0,0,0,0;0.4,0,0.6,-1.5;0.9,0,0,0"),
            clip("thrust", 0.7, false, "\"impact\":0.3", "bezier", "right_arm:rotation=0,0,0,0;0.15,25,0,0;0.3,-95,0,0;0.45,-80,0,0;0.7,0,0,0",
                "body:rotation=0,0,0,0;0.15,-5,0,0;0.3,12,0,0;0.7,0,0,0"));
        Map<String, List<Clip>> hits = new HashMap<>();
        if (withHits) hits.put("body", List.of(clip("hit_body", 0.5, false, "", "bezier", "body:rotation=0,0,0,0;0.12,14,0,0;0.5,0,0,0")));
        Map<String, Clip> deaths = new HashMap<>();
        String legs = "right_leg:rotation=0,0,0,0;0.3,-5,0,0;0.9,-40,0,0;1.2,-40,0,0";
        deaths.put("any/front", clip("death_front", 1.2, false, "", "bezier", "body:rotation=0,0,0,0;0.25,-12,0,0;0.8,-85,0,0;1.2,-88,0,0",
            "root:position=0,0,0,0;0.3,0,0.5,0;0.9,0,10,2;1.2,0,10.5,2", legs, "right_shin:rotation=0,0,0,0;0.4,20,0,0;0.9,5,0,0;1.2,5,0,0"));
        deaths.put("any/back", clip("death_back", 1.2, false, "", "bezier", "body:rotation=0,0,0,0;0.25,10,0,0;0.8,80,0,0;1.2,84,0,0",
            "root:position=0,0,0,0;0.3,0,0.5,0;0.9,0,10,-2;1.2,0,10.5,-2", legs));
        return new MotionEngine.Family("test", stances, attacks, hits, deaths);
    }

    static void engine() {
        Map<String, RigBone> rig = rig();
        MotionEngine.Family fam = family(true), bare = family(false);
        Random random = new Random(6100);
        MotionMath.Seed[] seeds = new MotionMath.Seed[20];
        for (int i = 0; i < seeds.length; i++) seeds[i] = MotionMath.seed(random.nextLong(), random.nextLong());
        noPops(fam, seeds);
        noClones(fam, seeds);
        feetPlanted(rig, fam, seeds);
        stable(fam, seeds);
        hitZones(bare, seeds);
        stanceEases(fam, seeds);
        gait(fam, seeds);
        life(rig, fam, seeds);
        lookAndSecondary(fam, seeds);
        actions(fam);
    }

    /**
     * No pops: at 60 fps no channel moves more than 12 degrees or 1.5 px in one frame across every state change, except by
     * what an authored clip's own keys move that frame (its gain times its own per-frame delta on that channel).
     */
    static void noPops(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worst = 0;
        String where = "none";
        for (int s = 0; s < 8; s++) {
            boolean walkingDeath = s >= 6;
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double dt = 1 / 60.0;
            Map<String, double[]> prev = null;
            Map<Integer, Double> prevTimes = new HashMap<>();
            Map<String, double[]> a = new HashMap<>(), b = new HashMap<>();
            int frames = walkingDeath ? 60 * 5 : 60 * 26;
            for (int f = 0; f <= frames; f++) {
                double e = f * dt;
                if (walkingDeath) {
                    if (at(e, 0.0)) { d.target = 0.55; d.enemy = true; }
                    if (at(e, 2.0)) { d.hit("body", 15, 2, false, true); d.dead = true; d.deathSeconds = 0; }
                } else {
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
                    if (at(e, 10.0)) d.stance = "ranged_rest";
                    if (at(e, 11.0)) { d.stance = ""; d.target = 0.6; }
                    if (at(e, 12.0)) d.stance = "fall_back";
                    if (at(e, 13.0)) { d.stance = "run"; d.target = 0.7; }   // brisk; a full sprint's own stride passes 12 degrees a frame at 60 fps
                    if (at(e, 14.5)) { d.shield = true; d.stance = "shield_move"; d.target = 0.35; }
                    if (at(e, 15.5)) d.attack = new MotionInput.Attack(1, d.tick());
                    if (at(e, 16.0)) { d.target = 0; d.shield = false; d.stance = "hold"; }
                    if (at(e, 16.5)) d.yawRate = 140;
                    if (at(e, 17.5)) d.yawRate = 0;
                    if (at(e, 18.0)) d.headYaw = 80;
                    if (at(e, 18.6)) d.headYaw = -80;
                    if (at(e, 19.2)) { d.headYaw = 0; d.enemy = false; d.stance = ""; }
                    if (at(e, 20.0)) d.hit("legs", 0, 2, false, false);
                    if (at(e, 21.0)) { d.hit("head", 90, 0, false, false); d.attack = new MotionInput.Attack(0, d.tick()); }
                    if (at(e, 22.0)) { d.hit("body", 10, 2, false, true); d.dead = true; d.deathSeconds = 0; }
                }
                MotionEngine.pose(d.in(), st, fam, out);
                Map<String, double[]> now = out.snapshot();
                Map<Integer, Double> times = new HashMap<>();
                List<MotionEngine.Playing> playing = MotionEngine.playing(st, d.time);
                for (var p : playing) times.put(p.id(), p.time());
                if (prev != null) {
                    Set<String> bones = new HashSet<>(now.keySet());
                    bones.addAll(prev.keySet());
                    for (String bone : bones) {
                        double[] n = now.getOrDefault(bone, new double[6]), o = prev.getOrDefault(bone, new double[6]);
                        for (int c = 0; c < 6; c++) {
                            double allow = c < 3 ? 12 : 1.5;
                            for (var p : playing) {
                                Double before = prevTimes.get(p.id());
                                if (before == null) continue;
                                a.clear(); b.clear();
                                p.clip().sample(before, a);
                                p.clip().sample(p.time(), b);
                                if (a.containsKey(bone)) allow += p.gain() * Math.abs(b.get(bone)[c] - a.get(bone)[c]);
                            }
                            double ratio = Math.abs(n[c] - o[c]) / allow;
                            if (!(ratio <= worst)) { worst = ratio; where = "seed " + s + " t " + String.format("%.3f", e) + " " + bone + " ch " + c + ": " + String.format("%.3f", n[c] - o[c]) + " allowed " + String.format("%.3f", allow); }
                        }
                    }
                }
                prev = now;
                prevTimes = times;
                d.step(dt);
            }
        }
        check(worst <= 1, "no pops: a channel moved past its budget in one 60 fps frame, worst " + where);
    }

    static boolean at(double elapsed, double when) { return Math.abs(elapsed - when) < 0.5 / 60; }

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

    /** No clones: two units that differ only in seed differ by at least 2 degrees RMS on the body and arms over a 4 s idle. */
    static void noClones(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double[][][] traces = new double[seeds.length][][];
        for (int i = 0; i < seeds.length; i++) traces[i] = idleTrace(fam, seeds[i], 4, "body", "right_arm", "left_arm");
        double least = Double.POSITIVE_INFINITY;
        for (int i = 0; i < seeds.length; i++) for (int j = i + 1; j < seeds.length; j++) {
            double sum = 0;
            int n = 0;
            for (int f = 0; f < traces[i].length; f++) for (int c = 0; c < traces[i][f].length; c++) { double x = traces[i][f][c] - traces[j][f][c]; sum += x * x; n++; }
            least = Math.min(least, Math.sqrt(sum / n));
        }
        check(least >= 2, "no clones: the closest pair of 20 seeded units differs by " + least + " degrees RMS");
    }

    /** Feet planted: standing with speed 0, through stance changes, hits, a hip-moving attack and look changes, the feet move under 0.05 px. */
    static void feetPlanted(Map<String, RigBone> rig, MotionEngine.Family fam, MotionMath.Seed[] seeds) {
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
    }

    /** Stable: 10 minutes of random input at random dt in [0.004, 0.1] stays finite, inside reach(Family), and keeps its state bounded. */
    static void stable(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        Random r = new Random(77);
        double[] reach = MotionEngine.reach(fam);
        check(reach.length == 6 && reach[3] > 0 && reach[4] > 0 && reach[5] > 0, "reach(Family) should bound every channel: " + java.util.Arrays.toString(reach));
        String[] stances = {"", "hold", "brace", "wall", "shield_move", "fall_back", "run", "ranged_rest", "ranged_aim", "nonsense"};
        String[] zones = {"head", "body", "legs", "arm", ""};
        for (int s = 0; s < 2; s++) {
            Drive d = new Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            double total = 0;
            int frames = 0, maxLoad = 0;
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
                    check(Math.abs(v) <= reach[c] + 1e-9, "stable: " + bone + " channel " + c + " = " + v + " is outside reach " + reach[c] + " after " + total + " s");
                }
                maxLoad = Math.max(maxLoad, MotionEngine.load(st));
                d.step(dt);
                total += dt;
                frames++;
            }
            check(frames > 10000, "stable: should step about 11,500 frames, stepped " + frames);
            check(maxLoad <= 40, "stable: the engine's active tracks, pulses and stance layers should stay bounded, peaked at " + maxLoad);
        }
    }

    /** Hit zones: a head hit's peak rotation is largest on the head, a legs hit's on the shins (procedural reactions, no hit clips). */
    static void hitZones(MotionEngine.Family bare, MotionMath.Seed[] seeds) {
        String[] bones = {"root", "body", "head", "right_arm", "right_forearm", "left_arm", "left_forearm", "right_leg", "right_shin", "left_leg", "left_shin"};
        for (int s = 0; s < 4; s++) for (String zone : new String[]{"head", "legs"}) for (float bearing : new float[]{0, 180, -60}) for (int weight : new int[]{0, 2}) {
            double[][] base = hitTrace(bare, seeds[s], null, 0, 0, bones), hit = hitTrace(bare, seeds[s], zone, bearing, weight, bones);
            double[] peak = new double[bones.length];
            for (int f = 0; f < base.length; f++) for (int i = 0; i < bones.length * 3; i++) peak[i / 3] = Math.max(peak[i / 3], Math.abs(hit[f][i] - base[f][i]));
            int top = 0;
            for (int i = 1; i < bones.length; i++) if (peak[i] > peak[top]) top = i;
            boolean ok = zone.equals("head") ? bones[top].equals("head") : bones[top].endsWith("_shin");
            check(ok && peak[top] > 8, "hit zones: a " + zone + " hit (bearing " + bearing + ", weight " + weight + ", seed " + s + ") peaked on " + bones[top] + " at " + peak[top] + "; all " + java.util.Arrays.toString(peak));
        }
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
    static void stanceEases(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
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
    static void gait(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
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
    }

    /** Life: novelty idles play on calm units only, and the hips shift toward the seed's idle leg. */
    static void life(Map<String, RigBone> rig, MotionEngine.Family fam, MotionMath.Seed[] seeds) {
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
    }

    /** Look: smoothed head yaw, part of a large yaw passes into body twist; secondary bones spring-lag and settle. */
    static void lookAndSecondary(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
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

    /** Action: the impact lands on the server's tick at any tempo, attacks scale per unit, hits override attacks, deaths follow the blow's bearing. */
    static void actions(MotionEngine.Family fam) {
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
            double impactTime = 0, after = 0;
            for (int f = 0; f < 60; f++) {
                d.step(1 / 60.0);
                MotionEngine.pose(d.in(), st, fam, out);
                var playing = MotionEngine.playing(st, d.time);
                if (playing.isEmpty()) continue;
                seen = playing.get(playing.size() - 1);
                double elapsed = d.time - start / 20.0, impact = seen.clip().impact() / 20.0;
                if (elapsed <= impact) check(near(seen.time(), Math.max(0, elapsed), 1e-9), "before impact the attack plays in real time so the impact lands on the server tick: " + seen.time() + " vs " + elapsed);
                else { impactTime = impact; after = elapsed; check(near(seen.time(), impact + (elapsed - impact) * seed.tempo(), 1e-9), "after impact the follow-through plays at the unit's tempo " + seed.tempo()); }
            }
            check(seen != null && after > impactTime, "the attack should play through its impact");
            check(near(seen.gain(), seed.amp(), 1e-12), "attacks scale by the unit's amplitude: " + seen.gain() + " vs " + seed.amp());
            check(seen.clip().name().equals("slash") || seen.clip().name().equals("thrust"), "an attack plays an attack clip");
        }
        for (float bearing : new float[]{0, 180}) {
            Drive d = new Drive(slow);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            d.attack = new MotionInput.Attack(1, d.tick());
            MotionEngine.pose(d.in(), st, fam, out);
            d.step(1 / 60.0);
            d.hit("body", bearing, 1, false, false);
            MotionEngine.pose(d.in(), st, fam, out);
            var playing = MotionEngine.playing(st, d.time);
            check(playing.get(playing.size() - 1).clip().name().equals("hit_body"), "a hit overrides the attack: " + playing);
            d.step(1 / 60.0);
            d.hit("body", bearing, 2, false, true);
            d.dead = true;
            for (int f = 0; f < 90; f++) { MotionEngine.pose(d.in(), st, fam, out); d.step(1 / 60.0); }
            playing = MotionEngine.playing(st, d.time);
            String want = bearing == 0 ? "death_front" : "death_back";
            check(!playing.isEmpty() && playing.get(playing.size() - 1).clip().name().equals(want), "a blow from bearing " + bearing + " should play " + want + ": " + playing);
            check(Math.abs(out.get("body", 0)) > 60, "the death clip should carry the body down: " + out.get("body", 0));
        }
    }
}
