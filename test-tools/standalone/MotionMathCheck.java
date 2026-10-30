package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Pure motion maths for the troop motion engine (animation rework design section 6): easing, the gait and triangle waves, frame-rate-independent approach, the sub-stepped spring, per-unit seeds and Clip sampling against Blockbench's own curves. MotionEngineCheck and MotionLayersCheck cover the engine. Never shipped. */
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
}
