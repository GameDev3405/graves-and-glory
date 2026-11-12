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
 * The MotionEngine's stance rulings (animation rework design section 6.2) through its frame API: no pops through stance
 * flicker, the planted-feet hand-off into an attack, the stance keys for Hold and Run, the server's attack variant and the aim
 * fallback, the old model's rise timing (riseAt, riseFall) and its cast release against the bolt. Fixtures come from MotionEngineCheck. Never shipped.
 */
public final class MotionStanceCheck {
    public static void main(String[] args) {
        MotionEngine.Family fam = family(true), bare = family(false);
        MotionMath.Seed[] seeds = seeds(20, 6100);
        run("flicker", () -> flicker(seeds));
        run("hand-off", () -> handoff(fam, seeds));
        run("hold", () -> hold(fam, seeds));
        run("run and charge", () -> runCharge());
        run("variant", () -> variant(fam));
        run("aim fallback", () -> aimFallback(seeds));
        run("rise timing", MotionStanceCheck::rise);
        run("cast release", MotionStanceCheck::castRelease);
        if (failed > 0) {
            System.out.println("MotionStanceCheck: " + failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionStanceCheck: " + checks + " checks passed");
    }

    /**
     * Gap 1 (docs/anim-rebuild/OLD-MODEL-GAPS.md): the old model's rise timing. MotionEngine.riseAt is DeathController's (moved unchanged): a long
     * window falls, holds and rises so the rise ends on the window's last tick; a short one quickens both; a window of 0 plays them at their length.
     * riseFall plays fall.json forward, holds its last frame, then plays it backwards to stand, at full weight between the fades.
     */
    static String rise() {
        double fall = 24; // common/fall.json: 1.2 s
        // the Hollow King: 70 ticks, longer than both clips (48): fall 0-24, hold to 46, rise 46-70
        double[] a = MotionEngine.riseAt(10, 70, fall, fall);
        check(a != null && a[0] == 0 && near(a[1], 10, 1e-9) && a[2] == 70, "king, 10 ticks in: falling at 10 of 70: " + Arrays.toString(a));
        a = MotionEngine.riseAt(30, 70, fall, fall);
        check(a != null && a[0] == 0 && near(a[1], 24, 1e-9), "king, 30 ticks in: the fall's last frame held: " + Arrays.toString(a));
        a = MotionEngine.riseAt(50, 70, fall, fall);
        check(a != null && a[0] == 1 && near(a[1], 4, 1e-9), "king, 50 ticks in: 4 ticks into the rise: " + Arrays.toString(a));
        check(MotionEngine.riseAt(70, 70, fall, fall) == null && MotionEngine.riseAt(-.01, 70, fall, fall) == null, "nothing before the rise or from its window's end");
        // Rise Again: a 20-tick stun, shorter than both clips: both quickened by 20/48, the fall to tick 10, the rise to 20
        a = MotionEngine.riseAt(5, 20, fall, fall);
        check(a != null && a[0] == 0 && near(a[1], 12, 1e-9), "rise again, 5 ticks in: half the fall: " + Arrays.toString(a));
        a = MotionEngine.riseAt(15, 20, fall, fall);
        check(a != null && a[0] == 1 && near(a[1], 12, 1e-9), "rise again, 15 ticks in: half the rise: " + Arrays.toString(a));
        a = MotionEngine.riseAt(47.9, 0, fall, fall);
        check(a != null && a[0] == 1 && a[2] == 48, "a window of 0 plays both clips at their own length: " + Arrays.toString(a));
        // the old model's fall clip: forward, held, backwards; at full weight between the fades; continuous at the turn
        StringBuilder out = new StringBuilder();
        for (int window : new int[]{20, 70}) {
            double prevT = Double.NaN, maxStep = 0, lowest = 1, lastT = Double.NaN, lastW = 1;
            for (double e = 0; e < window; e += .25) {
                double[] r = MotionEngine.riseFall(e, window, fall);
                check(r != null, "window " + window + ": a pose at " + e);
                double t = r[0] * Clip.TICKS;
                check(t >= -1e-9 && t <= fall + 1e-9, "window " + window + ": fall time " + t + " inside the clip at " + e);
                if (!Double.isNaN(prevT)) maxStep = Math.max(maxStep, Math.abs(t - prevT));
                if (e >= MotionEngine.RISE_IN && e <= window - MotionEngine.RISE_OUT) lowest = Math.min(lowest, r[1]);
                prevT = t; lastT = t; lastW = r[1];
            }
            double quick = window >= 2 * fall ? 1 : 2 * fall / window;
            check(maxStep <= .25 * quick + 1e-6, "window " + window + ": the fall time never jumps (largest step " + maxStep + " in a quarter tick)");
            check(near(lowest, 1, 1e-9), "window " + window + ": full weight between the fades (" + lowest + ")");
            check(lastT <= .25 * quick + 1e-6 && lastW < .05, "window " + window + ": it ends standing (fall time " + lastT + ", one quarter-tick step from 0) and faded out (" + lastW + ")");
            double[] lying = MotionEngine.riseFall(window >= 2 * fall ? fall + 5 : window / 2.0, window, fall);
            check(lying != null && near(lying[0] * Clip.TICKS, fall, 1e-6), "window " + window + ": down on the fall's last frame at the turn: " + Arrays.toString(lying));
            out.append(window).append(" ticks: largest step ").append(String.format("%.2f", maxStep)).append("; ");
        }
        check(MotionEngine.riseFall(20, 20, fall) == null && MotionEngine.riseFall(-1, 20, fall) == null, "no pose outside the window");
        return out.toString();
    }


    /**
     * Gap 3: on the old model a hexer's or mist caller's cast releases on the tick its bolt leaves. The server announces the cast a beat ahead
     * (Projectiles.drawAhead: start = beat + 10 - impactTicks) and sends the same event when the bolt leaves (Projectiles.loose: start = now -
     * impactTicks); the engine keeps the first copy and plays caster_cast in real time up to its impact marker, whatever the unit's tempo. So the
     * clip's impact (the release) lands on the bolt's tick; heard late, it still does, from mid-clip.
     */
    static String castRelease() {
        Clip cast;
        try { cast = Clip.parse(Files.readString(Path.of("src/main/resources/assets/gravesandglory/clips/biped/caster/caster_cast.json"))); }
        catch (Exception e) { throw new AssertionError("caster_cast.json: " + e); }
        int impactTicks = Arrays.stream(Spec.ATTACKVARIANTS).filter(r -> r.id().equals("caster_cast")).findFirst().orElseThrow().impactTicks();
        check(cast.impact() == impactTicks, "caster_cast's impact marker (" + cast.impact() + " ticks) is the sheet's impactTicks (" + impactTicks + ")");
        var fam = new MotionEngine.Family("caster", stances(), List.of(cast), Map.of(), Map.of());
        double worst = 0;
        for (boolean announced : new boolean[]{true, false}) for (MotionMath.Seed seed : seeds(20, 7300)) {
            Drive d = new Drive(seed);
            var state = new MotionEngine.State();
            Pose out = new Pose();
            long bolt = d.tick() + 40; // the world tick the bolt leaves (Abilities.attack: Projectiles.bolt, then loose)
            double releaseAt = Double.NaN;
            for (int f = 0; f < 60 * 3; f++) {
                long tick = d.tick();
                if (announced && tick >= bolt - 10 && d.attack == null) d.attack = new MotionInput.Attack(0, bolt - 10 + 10 - impactTicks); // drawAhead, a beat (10) ahead
                if (tick >= bolt) d.attack = new MotionInput.Attack(0, bolt - impactTicks); // loose: equal to the announced one
                MotionEngine.pose(d.in(), state, fam, out);
                for (var tr : state.tracks) if (tr.kind == MotionEngine.ATTACK && Double.isNaN(releaseAt) && tr.time(d.time) >= tr.impact) releaseAt = d.time * Clip.TICKS;
                d.step(1 / 60.0);
            }
            check(!Double.isNaN(releaseAt), (announced ? "announced" : "unannounced") + " cast: the clip reached its release");
            worst = Math.max(worst, Math.abs(releaseAt - bolt));
            check(Math.abs(releaseAt - bolt) <= 20 / 60.0 + 1e-9, (announced ? "announced" : "unannounced") + " cast, tempo " + seed.tempo() + ": release at tick " + releaseAt + ", bolt at " + bolt);
        }
        return "release within " + String.format("%.3f", worst) + " ticks of the bolt (60 fps frames), tempo 0.9 to 1.1, announced or not";
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

}
