package gg.gravesandglory;

import static gg.gravesandglory.MotionEngine.RX;
import static gg.gravesandglory.MotionEngine.RY;
import static gg.gravesandglory.MotionEngine.RZ;
import static gg.gravesandglory.MotionEngine.TX;
import static gg.gravesandglory.MotionEngine.TY;
import static gg.gravesandglory.MotionEngine.TZ;

import java.util.Iterator;

/**
 * The procedural layers of {@link MotionEngine} (animation rework design section 6.1): locomotion, life, hit reactions,
 * look, planted feet and secondary motion. Rig space is y down and -z forward. A negative arm or leg rx swings forward,
 * a positive shin rx bends the knee, a positive body rx leans forward and a positive head rx looks down. Pure.
 */
final class MotionBody {
    private MotionBody() {}

    static final double TAU = MotionMath.TAU;
    /** Hip-to-foot length of a straight leg and the shin's length (px), and the least knee bend a planted leg keeps (degrees). */
    static final double LEG = 12, SHIN = 6, SOFT_KNEE = 12;
    static final double[][] HIPS = {{-1.9, 12, 0}, {1.9, 12, 0}};
    static final String[] LEGS = {"right_leg", "left_leg"}, SHINS = {"right_shin", "left_shin"};

    // ---- locomotion ----

    /**
     * Walk and run cycles cross-faded on speed. The legs swing on a gait curve that crests early. The knee bends on the
     * forward-swinging leg. The body bobs at twice the stride frequency: lowest at double support when walking, at mid-stance
     * when running. Arms counter-swing with a lag, the hips counter-twist the shoulders, and elbows bend more toward a run.
     * A shield shuffle shortens and crouches the stride, a backpedal reverses the knee timing, and turning in place steps.
     */
    static void locomotion(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double a = f.m * Math.min(f.speed / 0.6, 1), run = f.run, sh = f.sh, bp = f.bp;
        double p = f.limbAngle * 0.6662 + f.seed.phase();
        if (a > 0) {
            double g = MotionMath.gait(p);
            double legAmp = a * (1 - 0.55 * sh) * (1 - 0.3 * bp) * (36 + 10 * run);
            out.add("right_leg", RX, -legAmp * g);
            out.add("left_leg", RX, legAmp * g);
            double knee = a * (1 - 0.4 * sh) * (32 + 38 * run);
            out.add("right_shin", RX, knee * lerp(Math.max(0, Math.cos(p - 0.25)), Math.max(0, -Math.cos(p + 0.25)), bp));
            out.add("left_shin", RX, knee * lerp(Math.max(0, -Math.cos(p - 0.25)), Math.max(0, Math.cos(p + 0.25)), bp));
            double s = Math.sin(p), c = Math.cos(p);
            out.add("root", TY, a * (0.9 * s * s * (1 - run) + 1.3 * c * c * run));
            out.add("root", TX, 0.35 * a * (1 - run) * s);
            out.add("body", RZ, 1.5 * a * (1 - run) * s);
            double lean = a * (3 + 7 * run) * (1 - bp) - 4 * bp * a;
            out.add("body", RX, lean);
            out.add("head", RX, -0.6 * lean);
            double twist = a * (5 + 3 * run) * (1 - sh);
            out.add("root", RY, -twist * g);
            out.add("body", RY, 1.8 * twist * g);
            double arm = a * (26 + 14 * run) * (1 - 0.75 * sh) * (1 - 0.3 * bp);
            double swing = MotionMath.tri(p - 0.35);
            out.add("right_arm", RX, arm * swing);
            out.add("left_arm", RX, -arm * swing);
            out.add("right_arm", RZ, a * (4 + 5 * run));
            out.add("left_arm", RZ, -a * (4 + 5 * run));
            double elbow = a * (10 + 45 * run), flick = MotionMath.tri(p - 0.85);
            out.add("right_forearm", RX, -elbow - 12 * a * Math.max(0, -flick));
            out.add("left_forearm", RX, -elbow - 12 * a * Math.max(0, flick));
            double crouch = sh * f.m;
            for (int i = 0; i < 2; i++) { out.add(SHINS[i], RX, 14 * crouch); out.add(LEGS[i], RX, -7 * crouch); }
            out.add("root", TY, crouch);
        }
        double rate = f.yawRate;
        st.turnPhase = (st.turnPhase + Math.abs(rate) / 45 * Math.PI * f.dt) % TAU;
        if (f.tw > 0) {
            double dir = Math.signum(rate), s = Math.sin(st.turnPhase), tw = f.tw;
            out.add("right_shin", RX, 24 * tw * Math.max(0, s));
            out.add("left_shin", RX, 24 * tw * Math.max(0, -s));
            out.add("right_leg", RX, -10 * tw * Math.max(0, s));
            out.add("left_leg", RX, -10 * tw * Math.max(0, -s));
            out.add("right_leg", RY, 9 * dir * tw * s);
            out.add("left_leg", RY, -9 * dir * tw * s);
            out.add("root", RY, -dir * 4 * tw);
        }
    }

    // ---- life ----

    /** A novelty idle: kind, start, length and side. */
    record Novelty(int kind, double start, double length, double side) {}

    static final int NOVELTY_KINDS = 6;

    /**
     * Life: breathing, a per-unit posture, slow micro-sway, a weight shift onto the seed's idle leg with a hip drop, and novelty
     * idles. Breathing is an integrated phase, so its rate can change without a jump. Posture and sway are drawn from the
     * seed, so two units never move alike. Novelty idles play only on calm units, every 6 to 14 s, and never touch the
     * sword arm's pitch.
     */
    static void life(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        MotionMath.Seed seed = f.seed;
        long bias = seed.variantBias();
        double lw = f.lw, m = f.m, cw = f.cw;
        if (f.first) {
            st.noveltyNext = f.now + 2 + seed.noveltyOffset();
            st.noveltyRng = MotionMath.mix(bias ^ 0x6A09E667F3BCC908L);
        }
        Novelty nov = st.novelty;
        if (nov != null && f.now - nov.start() >= nov.length()) nov = st.novelty = null;
        if (nov != null && f.now < nov.start()) nov = st.novelty = null;   // the render clock stepped back
        if (nov == null && f.now >= st.noveltyNext && f.calm >= 1) {
            int kind = Math.floorMod(next(st), NOVELTY_KINDS);
            double length = 1.8 + 1.4 * MotionMath.unit(next(st)), side = (next(st) & 1) == 0 ? -1 : 1;
            nov = st.novelty = new Novelty(kind, f.now, length, side);
            st.noveltyNext = f.now + 6 + 8 * MotionMath.unit(next(st));
            st.novelties++;
        }
        double ne = 0;
        if (nov != null) {
            double u = (f.now - nov.start()) / nov.length(), s = Math.sin(Math.PI * u);
            ne = s * s * MotionMath.ease(f.calm) * lw;
        }
        int kind = nov == null ? -1 : nov.kind();
        double side = nov == null ? 0 : nov.side(), u = nov == null ? 0 : (f.now - nov.start()) / nov.length();

        // breathing
        double breathRate = 0.23 * seed.breath() * (1 + 0.7 * Math.max(cw, m * f.run));
        st.breathPhase = (st.breathPhase + TAU * breathRate * f.dt) % TAU;
        if (f.first) st.breathPhase = seed.phase();
        double ba = lw * (1 + 0.5 * cw) * (1 + (kind == 4 ? 1.6 * ne : 0)), b = st.breathPhase;
        out.add("body", RX, -1.1 * ba * Math.sin(b));
        out.add("head", RX, 0.7 * ba * Math.sin(b));
        out.add("right_arm", TY, -0.22 * ba * Math.sin(b - 0.45));
        out.add("left_arm", TY, -0.22 * ba * Math.sin(b - 0.45));
        out.add("right_arm", RZ, 1.1 * ba * Math.sin(b - 0.9));
        out.add("left_arm", RZ, -1.1 * ba * Math.sin(b - 0.9));

        // posture
        double pw = lw * (1 - 0.5 * m) * (1 - 0.45 * cw);
        out.add("body", RX, pw * (-2 + 6 * draw(bias, 0)));
        out.add("body", RY, pw * sym(bias, 1, 6));
        out.add("body", RZ, pw * sym(bias, 2, 3));
        out.add("head", RX, pw * sym(bias, 3, 3));
        out.add("head", RZ, pw * sym(bias, 4, 4));
        out.add("right_arm", RX, pw * sym(bias, 5, 7));
        out.add("left_arm", RX, pw * sym(bias, 6, 7));
        out.add("right_arm", RY, pw * sym(bias, 7, 6));
        out.add("left_arm", RY, pw * sym(bias, 8, 6));
        out.add("right_arm", RZ, pw * (2 + 7 * draw(bias, 9)));
        out.add("left_arm", RZ, -pw * (2 + 7 * draw(bias, 10)));
        out.add("right_forearm", RX, -pw * (2 + 10 * draw(bias, 11)));
        out.add("left_forearm", RX, -pw * (2 + 10 * draw(bias, 12)));

        // micro sway: each channel on its own slow frequency, phase and amplitude
        double sw = lw * (1 - 0.5 * m);
        String[] bones = {"body", "head", "right_arm", "left_arm"};
        for (int i = 0; i < 12; i++) {
            int k = 20 + 3 * i;
            double hz = 0.12 + 0.25 * draw(bias, k), ph = TAU * draw(bias, k + 1), amp = 1.2 + 1.3 * draw(bias, k + 2);
            double t = TAU * hz * f.now + ph;
            out.add(bones[i / 3], i % 3, sw * amp * (Math.sin(t) + 0.35 * Math.sin(2.3 * t + 1.7 * ph)) / 1.35);
        }

        // weight shift onto the idle leg, with a hip drop the root rotation carries and the torso counters
        double idle = lw * (1 - m) * (1 - 0.6 * cw), legSide = seed.idleLeg() == 0 ? -1 : 1;
        double commit = 0.65 + 0.35 * Math.sin(0.19 * f.now + 3.1 * seed.phase());
        out.add("root", TX, 0.55 * legSide * commit * idle);
        double drop = -legSide * 2.2 * commit * idle, r = Math.toRadians(drop);
        out.add("root", RZ, drop);
        out.add("root", TX, LEG * Math.sin(r));
        out.add("root", TY, LEG * (1 - Math.cos(r)));
        out.add("body", RZ, -1.3 * drop);

        // novelty idles
        if (ne > 0) {
            double wave = Math.sin(TAU * u) * Math.sin(Math.PI * u) * MotionMath.ease(f.calm) * lw;
            switch (kind) {
                case 0 -> { out.add("head", RY, 40 * side * wave); out.add("head", RX, 4 * ne); }
                case 1 -> {
                    out.add("right_arm", TY, -0.8 * ne); out.add("left_arm", TY, -0.8 * ne);
                    out.add("right_arm", RZ, 3 * ne); out.add("left_arm", RZ, -3 * ne); out.add("body", RX, -2 * ne);
                }
                case 2 -> { out.add("right_forearm", RX, -22 * ne); out.add("right_hand", RZ, 15 * wave); out.add("left_arm", RX, -8 * ne); out.add("head", RX, 6 * ne); }
                case 3 -> { out.add("root", TX, 0.7 * side * ne); out.add("body", RZ, -2 * side * ne); }
                case 4 -> { out.add("body", RX, -2 * ne); out.add("head", RX, -3 * ne); }
                default -> { out.add("head", RZ, 13 * side * ne); out.add("head", RX, -4 * ne); }
            }
        }
    }

    static long next(MotionEngine.State st) { return st.noveltyRng = MotionMath.mix(st.noveltyRng + 0x9E3779B97F4A7C15L); }

    /** A seed draw in [0, 1) for slot i. */
    static double draw(long bias, int i) { return MotionMath.unit(MotionMath.mix(bias + 0x9E3779B97F4A7C15L * (i + 1))); }

    static double sym(long bias, int i, double amp) { return amp * (2 * draw(bias, i) - 1); }

    // ---- hit reactions ----

    /** One blow's procedural reaction: its zone (0 head, 1 body, 2 legs, 3 block), direction, side, start and scale. */
    record Pulse(int zone, double dir, double side, double start, double scale) {
        double envelope(double now) {
            double rise = zone == 2 ? 0.15 : 0.08, fall = zone == 2 ? 0.5 : 0.38, age = now - start;
            if (age <= 0) return 0;
            if (age < rise) return MotionMath.ease(age / rise);
            return 1 - MotionMath.ease((age - rise) / fall);
        }

        boolean over(double now) { return now - start > (zone == 2 ? 0.75 : 0.56); }
    }

    /** The springed reaction channels. KNEE goes to the planted-feet solve as extra knee bend. */
    static final int HEAD_RX = 0, HEAD_RZ = 1, BODY_RX = 2, BODY_RZ = 3, RARM_RX = 4, LARM_RX = 5, ARMS_RZ = 6, ROOT_TZ = 7, KNEE = 8, REACT = 9;

    static void pulse(MotionEngine.State st, MotionInput.Hit hit, double at, double scale) {
        int zone = hit.blocked() ? 3 : switch (MotionEngine.zone(hit.zone())) { case "head" -> 0; case "legs" -> 2; default -> 1; };
        double b = Math.toRadians(MotionEngine.finite(hit.bearing()));
        st.pulses.add(new Pulse(zone, MotionEngine.clamp(2 * Math.cos(b), -1, 1), MotionEngine.clamp(1.5 * Math.sin(b), -1, 1), at, scale));
        if (st.pulses.size() > 8) st.pulses.remove(0);
    }

    /**
     * Hit reactions, part of the action layer: each blow pushes its zone the way the blow travelled. A head blow snaps the
     * head, a body blow doubles the torso, a legs blow buckles the knees and a blocked blow jolts the shield arm. Each target
     * sum is springed, so blows overlap smoothly.
     */
    static void react(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double[] target = new double[REACT];
        for (Iterator<Pulse> it = st.pulses.iterator(); it.hasNext(); ) {
            Pulse p = it.next();
            if (p.over(f.now) || p.start() - f.now > 1) { it.remove(); continue; }
            double e = p.envelope(f.now) * p.scale(), d = p.dir(), s = p.side();
            switch (p.zone()) {
                case 0 -> { target[HEAD_RX] -= 24 * d * e; target[HEAD_RZ] += 9 * s * e; target[BODY_RX] -= 5 * d * e; target[ARMS_RZ] += 4 * e; }
                case 1 -> {
                    target[BODY_RX] += 16 * d * e; target[HEAD_RX] += 7 * d * e; target[RARM_RX] -= 9 * e; target[LARM_RX] -= 9 * e;
                    target[BODY_RZ] += 5 * s * e; target[ROOT_TZ] += 0.6 * d * e;
                }
                case 2 -> { target[KNEE] += 44 * e; target[BODY_RX] += 7 * e; target[ARMS_RZ] += 10 * e; target[BODY_RZ] += 4 * s * e; target[HEAD_RX] += 4 * e; }
                default -> { target[LARM_RX] += 14 * e; target[BODY_RX] -= 4 * d * e; target[ROOT_TZ] += 0.5 * d * e; target[HEAD_RX] -= 3 * e; }
            }
        }
        for (int i = 0; i < REACT; i++) {
            double t = i == KNEE ? MotionEngine.clamp(target[i], 0, 80) : MotionEngine.clamp(target[i], -60, 60);
            if (i == ROOT_TZ) t = MotionEngine.clamp(target[i], -2, 2);
            var s = MotionMath.spring(new MotionMath.Spring(st.react[i], st.reactVel[i]), t, i == KNEE ? 150 : 300, i == KNEE ? 18 : 22, f.dt);
            st.react[i] = MotionEngine.clamp(s.pos(), -90, 90);
            st.reactVel[i] = MotionEngine.clamp(s.vel(), -3000, 3000);
        }
        double[] r = st.react;
        out.add("head", RX, r[HEAD_RX]);
        out.add("head", RZ, r[HEAD_RZ]);
        out.add("body", RX, r[BODY_RX]);
        out.add("body", RZ, r[BODY_RZ]);
        out.add("right_arm", RX, r[RARM_RX]);
        out.add("left_arm", RX, r[LARM_RX]);
        out.add("right_arm", RZ, r[ARMS_RZ]);
        out.add("left_arm", RZ, -r[ARMS_RZ]);
        out.add("root", TZ, r[ROOT_TZ]);
    }

    // ---- look ----

    /**
     * Look: the head follows the look yaw and pitch through a critically damped spring with a speed limit, so a snap turn
     * still sweeps. Past 30 degrees, 35 percent of the extra yaw passes into the body as twist.
     */
    static void look(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double yawTarget = f.headYaw * f.lookW, pitchTarget = f.headPitch * f.lookW;
        if (f.first) { st.lookYaw = yawTarget; st.lookPitch = pitchTarget; }
        var y = MotionMath.spring(new MotionMath.Spring(st.lookYaw, st.lookYawVel), yawTarget, 160, 25.3, f.dt);
        var p = MotionMath.spring(new MotionMath.Spring(st.lookPitch, st.lookPitchVel), pitchTarget, 160, 25.3, f.dt);
        st.lookYawVel = MotionEngine.clamp(y.vel(), -400, 400);
        st.lookPitchVel = MotionEngine.clamp(p.vel(), -400, 400);
        st.lookYaw = MotionEngine.clamp(st.lookYaw + MotionEngine.clamp(y.pos() - st.lookYaw, -400 * f.dt, 400 * f.dt), -90, 90);
        st.lookPitch = MotionEngine.clamp(st.lookPitch + MotionEngine.clamp(p.pos() - st.lookPitch, -400 * f.dt, 400 * f.dt), -65, 65);
        double yaw = st.lookYaw, share = Math.signum(yaw) * 0.35 * Math.max(0, Math.abs(yaw) - 30);
        out.add("head", RY, yaw - share);
        out.add("body", RY, share);
        out.add("head", RX, st.lookPitch);
    }

    // ---- planted feet ----

    /**
     * Plants the feet while standing. Each foot's target is where the legs' own channels put it with the root at rest. When
     * the root moves (weight shift, hip drop, a hip-moving attack, a stance's crouch), the root dips just enough for both
     * legs to reach, keeping a soft knee and adding any knee-buckle from a legs hit. Then each leg is solved analytically
     * (two bones, knee hinge on x, thigh yaw kept) so the foot lands exactly on its target. Moving, turning in place or a
     * clip that keys the legs hands the feet back, eased by the same weights; the knee-buckle then bends the legs directly.
     */
    static void plant(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double pw = (1 - f.m) * (1 - f.tw) * (1 - Math.min(1, f.bodyAction));
        double kneeHit = Math.max(0, st.react[KNEE]);
        double[][] targets = new double[2][];
        for (int i = 0; i < 2; i++) targets[i] = foot(i, out.get(LEGS[i]), out.get(SHINS[i]));
        if (pw > 0) {
            double[] root = out.get("root");
            double dip = 0;
            for (int i = 0; i < 2; i++) {
                double[] leg = out.get(LEGS[i]);
                double[] hip = add(rotate(new double[]{HIPS[i][0] + leg[TX], HIPS[i][1] + leg[TY], HIPS[i][2] + leg[TZ]}, root), root[TX], root[TY], root[TZ]);
                double dx = targets[i][0] - hip[0], dy = targets[i][1] - hip[1], dz = targets[i][2] - hip[2];
                double d0 = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double soft = 2 * Math.toDegrees(Math.acos(Math.min(d0, LEG * Math.cos(Math.toRadians(SOFT_KNEE / 2))) / LEG));
                double bend = Math.min(soft + kneeHit, 150), want = LEG * Math.cos(Math.toRadians(bend / 2));
                double flat = want * want - dx * dx - dz * dz;
                dip = Math.max(dip, dy - Math.sqrt(Math.max(0, flat)));
            }
            out.add("root", TY, MotionEngine.clamp(dip, 0, 9) * pw);
            root = out.get("root");
            for (int i = 0; i < 2; i++) solve(i, targets[i], root, out.get(LEGS[i]), out.get(SHINS[i]), pw);
        }
        double free = (1 - pw) * kneeHit;
        if (free > 0) {
            for (int i = 0; i < 2; i++) { out.add(SHINS[i], RX, free); out.add(LEGS[i], RX, -free / 2); }
            out.add("root", TY, LEG * (1 - Math.cos(Math.toRadians(free / 2))));
        }
    }

    /** Leg i's foot in root space from its own channels. */
    static double[] foot(int i, double[] leg, double[] shin) {
        double[] q = add(rotate(new double[]{0, SHIN, 0}, shin), shin[TX], SHIN + shin[TY], shin[TZ]);
        q = rotate(q, leg);
        return add(q, HIPS[i][0] + leg[TX], HIPS[i][1] + leg[TY], HIPS[i][2] + leg[TZ]);
    }

    /** Solves leg i so its foot reaches target (entity space) under the given root, blending the result in by w. */
    static void solve(int i, double[] target, double[] root, double[] leg, double[] shin, double w) {
        double[] local = unrotate(new double[]{target[0] - root[TX], target[1] - root[TY], target[2] - root[TZ]}, root);
        double vx = local[0] - (HIPS[i][0] + leg[TX]), vy = local[1] - (HIPS[i][1] + leg[TY]), vz = local[2] - (HIPS[i][2] + leg[TZ]);
        double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (!(d > 1e-6)) return;
        double knee = 2 * Math.acos(Math.min(d, LEG) / LEG);
        double c = Math.toRadians(leg[RY]), cc = Math.cos(c);
        double beta = Math.asin(MotionEngine.clamp(vz / d / (Math.abs(cc) < 1e-6 ? 1e-6 : cc), -1, 1));
        double a = Math.atan2(vy / d, vx / d) - Math.atan2(Math.cos(beta), Math.sin(beta) * Math.sin(c));
        double rz = Math.toDegrees(a);
        rz += 360 * Math.rint((leg[RZ] - rz) / 360);
        double rx = Math.toDegrees(beta - knee / 2), kx = Math.toDegrees(knee);
        leg[RX] += (rx - leg[RX]) * w;
        leg[RZ] += (rz - leg[RZ]) * w;
        shin[RX] += (kx - shin[RX]) * w;
        shin[RY] -= shin[RY] * w;
        shin[RZ] -= shin[RZ] * w;
        for (int ch = TX; ch <= TZ; ch++) shin[ch] -= shin[ch] * w;
    }

    /** Rotates p by a bone's rotation channels: about x, then y, then z (ModelPart order), degrees. */
    static double[] rotate(double[] p, double[] ch) {
        double[] q = turn(p, 1, 2, ch[RX]);
        q = turn(q, 2, 0, ch[RY]);
        return turn(q, 0, 1, ch[RZ]);
    }

    static double[] unrotate(double[] p, double[] ch) {
        double[] q = turn(p, 0, 1, -ch[RZ]);
        q = turn(q, 2, 0, -ch[RY]);
        return turn(q, 1, 2, -ch[RX]);
    }

    static double[] turn(double[] q, int a, int b, double deg) {
        if (deg == 0) return q.clone();
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r);
        double[] o = q.clone();
        o[a] = q[a] * c - q[b] * s;
        o[b] = q[a] * s + q[b] * c;
        return o;
    }

    static double[] add(double[] q, double x, double y, double z) { return new double[]{q[0] + x, q[1] + y, q[2] + z}; }

    // ---- secondary ----

    /**
     * Secondary motion: cape, tabard and crest bones lag their parents on underdamped springs and settle. The cape streams
     * back with speed and flutters on a run, and swings out on a body turn. The tabard follows the forward leg. The crest
     * trails the head. Bones a model lacks are ignored by the renderer.
     */
    static void secondary(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double bodyX = out.get("body", RX) + out.get("root", RX), bodyY = out.get("body", RY) + out.get("root", RY);
        double headX = out.get("head", RX), headY = out.get("head", RY);
        if (f.first) { st.capeX = bodyX; st.capeY = bodyY; st.crestX = headX; st.crestY = headY; st.yawRate = f.yawRate; }
        var cx = MotionMath.spring(new MotionMath.Spring(st.capeX, st.capeXVel), bodyX, 70, 8, f.dt);
        var cy = MotionMath.spring(new MotionMath.Spring(st.capeY, st.capeYVel), bodyY, 70, 8, f.dt);
        var hx = MotionMath.spring(new MotionMath.Spring(st.crestX, st.crestXVel), headX, 160, 9, f.dt);
        var hy = MotionMath.spring(new MotionMath.Spring(st.crestY, st.crestYVel), headY, 160, 9, f.dt);
        st.capeX = MotionEngine.clamp(cx.pos(), -720, 720); st.capeXVel = MotionEngine.clamp(cx.vel(), -5000, 5000);
        st.capeY = MotionEngine.clamp(cy.pos(), -720, 720); st.capeYVel = MotionEngine.clamp(cy.vel(), -5000, 5000);
        st.crestX = MotionEngine.clamp(hx.pos(), -720, 720); st.crestXVel = MotionEngine.clamp(hx.vel(), -5000, 5000);
        st.crestY = MotionEngine.clamp(hy.pos(), -720, 720); st.crestYVel = MotionEngine.clamp(hy.vel(), -5000, 5000);
        st.yawRate = MotionMath.approach(st.yawRate, f.yawRate, 5, f.dt);
        double a = f.m * Math.min(f.speed / 0.6, 1);
        st.flutter = (st.flutter + (7 + 5 * f.run) * f.dt) % TAU;
        double lagX = st.capeX - bodyX, lagY = st.capeY - bodyY;
        double cape = MotionEngine.clamp(a * (14 + 22 * f.run) + 2.5 * a * Math.sin(st.flutter) + 0.9 * lagX, -15, 75);
        out.get("cape")[RX] += cape;
        out.get("cape")[RZ] += MotionEngine.clamp(0.6 * lagY - 0.03 * st.yawRate, -20, 20);
        double legs = Math.min(0, Math.min(out.get("right_leg", RX), out.get("left_leg", RX)));
        out.get("tabard")[RX] += MotionEngine.clamp(a * (6 + 10 * f.run) + 0.6 * lagX + 0.45 * legs, -60, 60);
        out.get("crest")[RX] += MotionEngine.clamp(1.1 * (st.crestX - headX) + 6 * a, -35, 35);
        out.get("crest")[RZ] += MotionEngine.clamp(0.5 * (st.crestY - headY), -25, 25);
    }

    static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
