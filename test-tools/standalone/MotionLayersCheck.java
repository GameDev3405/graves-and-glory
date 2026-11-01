package gg.gravesandglory;

import java.util.List;

/**
 * The MotionEngine's layers one at a time (animation rework design section 6.1): no pops through every state change at
 * every speed, with the stride's own motion exempted; the stride (counter-twist, arm lag and outward roll, lean into speed,
 * the braced shuffle and the backpedal); breathing's phase offsets; novelty idles; the actions' effective shares; and
 * stance and track overflow. MotionEngineCheck covers the engine through its frame API. Never shipped.
 */
public final class MotionLayersCheck {
    static void check(boolean ok, String message) {
        MotionEngineCheck.checks++;
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        MotionEngine.Family fam = MotionEngineCheck.family(true);
        MotionMath.Seed[] seeds = MotionEngineCheck.seeds(8, 7300);
        for (double speed : new double[]{0, 0.35, 0.7, 1.0, 1.5}) MotionEngineCheck.run("no pops at speed " + speed, () -> noPops(fam, seeds, speed));
        MotionEngineCheck.run("counter-twist", () -> twist(seeds));
        MotionEngineCheck.run("arm lag and outward roll", () -> arms(seeds));
        MotionEngineCheck.run("lean into speed", () -> lean(seeds));
        MotionEngineCheck.run("braced shuffle", () -> shuffle(seeds));
        MotionEngineCheck.run("backpedal", () -> backpedal(seeds));
        MotionEngineCheck.run("breathing phase offsets", MotionLayersCheck::breathing);
        MotionEngineCheck.run("novelty kinds spare the sword arm", MotionLayersCheck::novelties);
        MotionEngineCheck.run("effective shares", () -> shares(fam, seeds));
        MotionEngineCheck.run("track overflow", () -> trackOverflow(fam, seeds));
        if (MotionEngineCheck.failed > 0) {
            System.out.println("MotionLayersCheck: " + MotionEngineCheck.failed + " checks FAILED");
            System.exit(1);
        }
        System.out.println("MotionLayersCheck: " + MotionEngineCheck.checks + " checks passed");
    }

    /** Pops that also exempt the stride's own per-frame motion: the gait at this frame's weights, at the last and this limb angle. */
    static final class GaitPops extends MotionEngineCheck.Pops {
        final Pose now = new Pose(), before = new Pose();
        double lastAngle = Double.NaN;

        @Override double extra(String bone, int c) { return Math.abs(now.get(bone, c) - before.get(bone, c)); }

        void gait(MotionEngine.State st, double limbAngle) {
            now.clear();
            before.clear();
            MotionBody.gaitPose(st.frame, st.frame.limbAngle, now);
            MotionBody.gaitPose(st.frame, Double.isNaN(lastAngle) ? st.frame.limbAngle : lastAngle, before);
            lastAngle = st.frame.limbAngle;
        }
    }

    /** The schedule's worst pop as a share of the 12 degree / 1.5 px bar: kept clear of 1 so a retune cannot creep up to it. */
    static final double POP_MARGIN = 0.85;

    /**
     * The shared schedule at this speed, three seeds: no channel moves more than 12 degrees or 1.5 px in a 60 fps frame
     * (scaled up for the 0.1 s hitch) beyond what the playing clips' own keys move at their effective shares and what the
     * stride itself moves.
     */
    static String noPops(MotionEngine.Family fam, MotionMath.Seed[] seeds, double speed) {
        double worst = 0;
        String where = "none";
        for (int s = 0; s < 3; s++) {
            MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            GaitPops pops = new GaitPops();
            double e = 0;
            while (e < 32) {
                double dt = MotionEngineCheck.schedule(d, e, speed);
                MotionEngine.pose(d.in(), st, fam, out);
                pops.gait(st, d.limbAngle);
                pops.frame("seed " + s + " t " + String.format("%.3f", e), out, st, d.time, dt);
                double next = MotionEngineCheck.at(e, 23.5) ? 0.1 : 1 / 60.0;
                d.step(next);
                e += next;
            }
            if (pops.worst > worst) { worst = pops.worst; where = pops.where; }
        }
        check(worst <= POP_MARGIN, "no pops at speed " + speed + ": worst ratio " + worst + " at " + where + " (bar " + POP_MARGIN + ")");
        return String.format("worst ratio %.3f at %s", worst, where);
    }

    static MotionEngine.Frame frame(MotionMath.Seed seed, double speed, double run, double sh, double bp) {
        MotionEngine.Frame f = new MotionEngine.Frame();
        f.seed = seed;
        f.m = 1;
        f.speed = speed;
        f.run = run;
        f.sh = sh;
        f.bp = bp;
        return f;
    }

    /** One stride cycle of the gait, per sample {channel values for the asked bones}. */
    static double[][] cycle(MotionEngine.Frame f, int samples, String[] bones, int[] channels) {
        double[][] out = new double[samples][bones.length];
        Pose pose = new Pose();
        for (int k = 0; k < samples; k++) {
            pose.clear();
            MotionBody.gaitPose(f, (MotionMath.TAU * k / samples - f.seed.phase()) / 0.6662, pose);
            for (int b = 0; b < bones.length; b++) out[k][b] = pose.get(bones[b], channels[b]);
        }
        return out;
    }

    /** The fundamental's phase of column col over one cycle, radians: v ~ A sin(p - phase) with A > 0. */
    static double phase(double[][] v, int col) {
        double s = 0, c = 0;
        for (int k = 0; k < v.length; k++) { double p = MotionMath.TAU * k / v.length; s += v[k][col] * Math.sin(p); c += v[k][col] * Math.cos(p); }
        return Math.atan2(-c, s);
    }

    static double mean(double[][] v, int col) {
        double m = 0;
        for (double[] row : v) m += row[col] / v.length;
        return m;
    }

    static double range(double[][] v, int col) {
        double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
        for (double[] row : v) { lo = Math.min(lo, row[col]); hi = Math.max(hi, row[col]); }
        return hi - lo;
    }

    static double wrap(double a) { return Math.IEEEremainder(a, MotionMath.TAU); }

    /** The hips counter-twist the shoulders: the root's yaw and the shoulders' (root plus body) yaw swing in opposite phase. */
    static String twist(MotionMath.Seed[] seeds) {
        StringBuilder r = new StringBuilder();
        for (double run : new double[]{0, 1}) {
            double[][] v = cycle(frame(seeds[0], run > 0 ? 1.2 : 0.5, run, 0, 0), 240, new String[]{"root", "body"}, new int[]{1, 1});
            for (double[] row : v) row[1] += row[0];
            double gap = Math.abs(wrap(phase(v, 0) - phase(v, 1)));
            check(Math.abs(gap - Math.PI) < 0.2 && range(v, 0) > 4 && range(v, 1) > 4, "hips and shoulders should twist against each other: phase gap " + gap + ", hips " + range(v, 0) + ", shoulders " + range(v, 1));
            r.append(String.format("run %.0f: gap %.2f rad, hips %.1f, shoulders %.1f; ", run, gap, range(v, 0), range(v, 1)));
        }
        return r.toString().trim();
    }

    /** The arms counter-swing late (the right arm trails the left leg by about 0.35 rad) and roll outward while moving. */
    static String arms(MotionMath.Seed[] seeds) {
        double[][] v = cycle(frame(seeds[1], 0.6, 0.4, 0, 0), 360, new String[]{"right_arm", "left_leg", "right_arm", "left_arm"}, new int[]{0, 0, 2, 2});
        double lag = wrap(phase(v, 0) - phase(v, 1));
        check(lag > 0.2 && lag < 0.6, "the sword arm should swing with the opposite leg and trail it by about 0.35 rad: " + lag);
        check(mean(v, 2) > 2 && mean(v, 3) < -2, "the arms should roll outward while moving: right rz " + mean(v, 2) + ", left rz " + mean(v, 3));
        return String.format("arm trails leg by %.2f rad, roll %.1f / %.1f", lag, mean(v, 2), mean(v, 3));
    }

    /** The body leans further forward as speed rises. */
    static String lean(MotionMath.Seed[] seeds) {
        double[] speeds = {0.2, 0.45, 0.8, 1.5}, runs = {0, 0.17, 0.75, 1};
        double prev = 0;
        StringBuilder r = new StringBuilder();
        for (int i = 0; i < speeds.length; i++) {
            double l = mean(cycle(frame(seeds[2], speeds[i], runs[i], 0, 0), 120, new String[]{"body"}, new int[]{0}), 0);
            check(l > prev + 0.3, "the lean should grow with speed: " + prev + " then " + l + " at " + speeds[i]);
            prev = l;
            r.append(String.format("%.2f: %.1f; ", speeds[i], l));
        }
        return r.toString().trim();
    }

    /** The braced shuffle is its own gait: a shorter stride, a lower body, bent knees, a tight arm swing and little twist. */
    static String shuffle(MotionMath.Seed[] seeds) {
        String[] bones = {"right_leg", "root", "right_shin", "right_arm", "root"};
        int[] ch = {0, 4, 0, 0, 1};
        double[][] walk = cycle(frame(seeds[3], 0.5, 0, 0, 0), 240, bones, ch), sh = cycle(frame(seeds[3], 0.5, 0, 1, 0), 240, bones, ch);
        check(range(sh, 0) < 0.6 * range(walk, 0), "the shuffle should shorten the stride: " + range(sh, 0) + " vs " + range(walk, 0));
        check(mean(sh, 1) > mean(walk, 1) + 0.5, "the shuffle should lower the body: " + mean(sh, 1) + " vs " + mean(walk, 1));
        check(mean(sh, 2) > mean(walk, 2) + 5, "the shuffle should keep the knees bent: " + mean(sh, 2) + " vs " + mean(walk, 2));
        check(range(sh, 3) < 0.4 * range(walk, 3) && range(sh, 4) < 0.2 * range(walk, 4) + 1e-9, "the shuffle should hold the arms and hips: arm " + range(sh, 3) + " vs " + range(walk, 3) + ", twist " + range(sh, 4));
        return String.format("stride %.1f vs %.1f, drop %.2f, knee %.1f vs %.1f", range(sh, 0), range(walk, 0), mean(sh, 1) - mean(walk, 1), mean(sh, 2), mean(walk, 2));
    }

    /** The backpedal reverses the knee timing (the knee bends as the leg swings back) and leans the body back. */
    static String backpedal(MotionMath.Seed[] seeds) {
        double[] fb = new double[4];
        for (int k = 0; k < 2; k++) {
            double[][] v = cycle(frame(seeds[4], 0.5, 0, 0, k), 360, new String[]{"right_leg", "right_shin", "body"}, new int[]{0, 0, 0});
            double fwd = 0, back = 0;
            for (int i = 0; i < v.length; i++) {
                double dl = v[(i + 1) % v.length][0] - v[(i + v.length - 1) % v.length][0];
                if (dl < 0) fwd += v[i][1]; else back += v[i][1];
            }
            fb[2 * k] = fwd / v.length;
            fb[2 * k + 1] = back / v.length;
            if (k == 1) check(mean(v, 2) < -1, "the backpedal should lean the body back: " + mean(v, 2));
        }
        check(fb[0] > 2 * fb[1], "walking forward bends the knee on the forward swing: " + fb[0] + " vs " + fb[1]);
        check(fb[3] > 2 * fb[2], "the backpedal bends the knee on the backward swing: " + fb[3] + " vs " + fb[2]);
        return String.format("walk fwd/back %.1f/%.1f, backpedal %.1f/%.1f", fb[0], fb[1], fb[2], fb[3]);
    }

    /** Breathing: the chest leads, the shoulders follow 0.45 rad later and the arms 0.9 rad later. */
    static String breathing() {
        int n = 360;
        double[][] v = new double[n][3];
        Pose pose = new Pose();
        for (int k = 0; k < n; k++) {
            pose.clear();
            MotionBody.breathe(1, MotionMath.TAU * k / n, pose);
            v[k][0] = -pose.get("body", 0);
            v[k][1] = -pose.get("right_arm", 4);
            v[k][2] = pose.get("right_arm", 2);
        }
        double chest = phase(v, 0), shoulder = wrap(phase(v, 1) - chest), arm = wrap(phase(v, 2) - chest);
        check(Math.abs(shoulder - 0.45) < 0.05 && Math.abs(arm - 0.9) < 0.05, "breathing phase offsets should be 0.45 and 0.9 rad: " + shoulder + ", " + arm);
        return String.format("shoulders +%.2f, arms +%.2f rad", shoulder, arm);
    }

    /** Every novelty kind, at full strength either way, leaves the sword arm's and forearm's pitch alone and moves something. */
    static String novelties() {
        Pose pose = new Pose();
        for (int kind = 0; kind < MotionBody.NOVELTY_KINDS; kind++) for (double wave : new double[]{-1, 1}) for (double side : new double[]{-1, 1}) {
            pose.clear();
            MotionBody.novelty(kind, 1, wave, side, pose);
            check(pose.get("right_arm", 0) == 0 && pose.get("right_forearm", 0) == 0, "novelty kind " + kind + " moved the sword arm's pitch");
            double moved = 0;
            for (String b : pose.bones()) for (int c = 0; c < 6; c++) moved += Math.abs(pose.get(b, c));
            check(moved > 1, "novelty kind " + kind + " should move something");
        }
        return MotionBody.NOVELTY_KINDS + " kinds";
    }

    /** playing() reports each clip's effective share of the fold: a covered track's gain shrinks by the weights above it. */
    static String shares(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(seeds[5]);
        var st = new MotionEngine.State();
        Pose out = new Pose();
        double worst = 0;
        int checked = 0;
        for (int f = 0; f < 60; f++) {
            if (f == 5) d.attack = new MotionInput.Attack(0, d.tick());
            if (f == 25) d.attack = new MotionInput.Attack(1, d.tick());
            MotionEngine.pose(d.in(), st, fam, out);
            List<MotionEngine.Playing> playing = MotionEngine.playing(st, d.time);
            if (st.tracks.size() == 2) {
                // P1: the first attack is now the FROM track below (its fold at the interrupt, coasting; MERGED, so not in
                // playing). Expected shares from each track's fade progress alone, with the cosine ease written out here.
                check(st.tracks.get(0).kind == MotionEngine.MERGED && st.tracks.get(0).inertial && playing.size() == 1,
                    "the interrupted attack should be one FROM track under the new one: " + st.tracks.size() + " tracks, " + playing.size() + " playing");
                double wBottom = cosineEase(st.tracks.get(0).x), wTop = cosineEase(st.tracks.get(1).x);
                double wantTop = wTop * seeds[5].amp(), wantBottom = wBottom * (1 - wTop);
                double[] e = new double[MotionEngine.MAX_TRACKS + 1];
                MotionStack.shares(st.tracks, 2, e);
                double top = playing.get(0).gain(), bottom = e[0];
                check(Math.abs(top - wantTop) < 1e-9 && Math.abs(bottom - wantBottom) < 1e-9, "effective shares should be w_top and w_bottom (1 - w_top): got "
                    + top + " / " + bottom + ", want " + wantTop + " / " + wantBottom);
                worst = Math.max(worst, Math.max(Math.abs(top - wantTop), Math.abs(bottom - wantBottom)));
                checked++;
            }
            d.step(1 / 60.0);
        }
        check(checked > 3, "the second attack should fade in over several frames: " + checked);
        return String.format("%d overlapped frames, worst error %.1e", checked, worst);
    }

    static double cosineEase(double x) { return (1 - Math.cos(Math.PI * Math.max(0, Math.min(1, x)))) / 2; }

    /** An attack every other frame for a second keeps the tracks bounded without dropping a weighted one. */
    static String trackOverflow(MotionEngine.Family fam, MotionMath.Seed[] seeds) {
        double worst = 0;
        int peak = 0;
        for (int s = 0; s < 3; s++) {
            MotionEngineCheck.Drive d = new MotionEngineCheck.Drive(seeds[s]);
            var st = new MotionEngine.State();
            Pose out = new Pose();
            MotionEngineCheck.Pops pops = new MotionEngineCheck.Pops();
            for (int f = 0; f < 180; f++) {
                if (f >= 30 && f < 90 && f % 2 == 0) d.attack = new MotionInput.Attack(f / 2, d.tick());
                if (f >= 31 && f < 90 && f % 6 == 1) d.hit("body", 30, 1, false, false);
                MotionEngine.pose(d.in(), st, fam, out);
                pops.frame("seed " + s + " frame " + f, out, st, d.time, 1 / 60.0);
                peak = Math.max(peak, st.tracks.size());
                d.step(1 / 60.0);
            }
            worst = Math.max(worst, pops.worst);
        }
        check(peak <= MotionEngine.MAX_TRACKS && MotionEngine.MAX_TRACKS <= 8, "tracks should stay bounded: " + peak + " of " + MotionEngine.MAX_TRACKS);
        check(worst <= 1, "an overflowing track stack should never pop: worst ratio " + worst);
        return String.format("peak %d tracks, worst ratio %.3f", peak, worst);
    }
}
