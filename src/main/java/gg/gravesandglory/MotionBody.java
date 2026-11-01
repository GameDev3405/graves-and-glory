package gg.gravesandglory;

import static gg.gravesandglory.MotionEngine.RX;
import static gg.gravesandglory.MotionEngine.RY;
import static gg.gravesandglory.MotionEngine.RZ;
import static gg.gravesandglory.MotionEngine.TX;
import static gg.gravesandglory.MotionEngine.TY;
import static gg.gravesandglory.MotionEngine.TZ;

import java.util.Arrays;

/**
 * The procedural layers of {@link MotionEngine} (animation rework design section 6.1): locomotion, life, hit reactions,
 * look and secondary motion. Rig space is y down and -z forward. A negative arm or leg rx swings forward, a positive shin
 * rx bends the knee, a positive body rx leans forward and a positive head rx looks down. Steady-state frames do not
 * allocate. Pure.
 */
final class MotionBody {
    private MotionBody() {}

    static final double TAU = MotionMath.TAU;
    static final String[] LEGS = MotionFeet.LEGS, SHINS = MotionFeet.SHINS;

    // ---- locomotion ----

    /**
     * Walk and run cycles cross-faded on speed, then the step of turning in place. The turn step's phase integrates last
     * frame's yaw rate, so a rate change at a sample time steps the same at any frame rate.
     */
    static void locomotion(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        if (f.ground) stride(f, st, out);
        else {
            f.gaitW = 0;
            gaitPose(f, f.limbAngle, out);
        }
        double rate = f.first ? f.yawRate : st.turnRate;
        st.turnPhase = (st.turnPhase + Math.abs(rate) / 45 * Math.PI * f.dt) % TAU;
        st.turnRate = f.yawRate;
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

    /**
     * The stride at limb angle {@code limbAngle}, from the frame's seed and its moving, running, shuffle and backpedal
     * weights only. The legs swing on a gait curve that crests early and the knee bends on the forward-swinging leg. The
     * body bobs at twice the stride frequency: lowest at double support when walking, at mid-stance when running. The arms
     * counter-swing with a lag and roll outward, the hips counter-twist the shoulders, the body leans into speed and the
     * elbows bend more toward a run. A shield shuffle shortens and crouches the stride; a backpedal reverses the knee timing
     * and leans back.
     */
    static void gaitPose(MotionEngine.Frame f, double limbAngle, Pose out) {
        double a = f.m * Math.min(f.speed / 0.6, 1), run = f.run, sh = f.sh, bp = f.bp;
        double p = limbAngle * 0.6662 + f.seed.phase();
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
        }
        double crouch = sh * f.m;
        if (crouch > 0) {
            for (int i = 0; i < 2; i++) { out.add(SHINS[i], RX, 14 * crouch); out.add(LEGS[i], RX, -7 * crouch); }
            out.add("root", TY, crouch);
        }
    }

    /** G1: duty factor walk to run, the stride's half-length cap forward walk to run and sideways (px), at the soft knee's reach. */
    static final double DUTY_WALK = 0.62, DUTY_RUN = 0.38, HALF_WALK = 6.4, HALF_RUN = 7.0, HALF_SIDE = 3.2;
    /** G1: the least and most cadence (cycles per second), the swing knee walk to run (degrees), and the swing foot's lift walk to run (px). */
    static final double F_MIN = 0.9, F_MAX = 3.6, KNEE_WALK = 35, KNEE_RUN = 70, LIFT_MIN = 1.5, LIFT_RUN = 3.5;
    /** Fix 1 (R109): the speed (b/s) under which the phase stops dividing by the speed, and the most a frame's travel may step the phase, in multiples of the cadence's own step. */
    static final double V_FLOOR = 0.15, PSI_STEP = 4;
    /**
     * Fix 2: the move weight under which a start counts as from standing, the upper body's ease (s), and the first step's lean:
     * the speed's rise smoothed at ACC_HZ, LEAN_GAIN degrees per b/s^2 up to LEAN_MAX, with DIP_PER_DEG px of hip dip a degree.
     */
    static final double START_W = 0.15, UP_BLEND = 0.45, ACC_HZ = 8, LEAN_GAIN = 0.2, LEAN_MAX = 5, DIP_PER_DEG = 0.12;

    /**
     * G1 (R109): the stride matched to the ground speed. The gait keeps its own phase psi, advanced by the distance travelled
     * each frame over the stride length (the smoothed speed over the cadence), at a cadence chosen so the stride is
     * as long as the half-length cap allows along the direction of travel (an ellipse: long forward and back, short sideways,
     * so a strafe steps quicker) and rises past F_MIN. Each leg's foot sweeps along the body-frame velocity: in stance it
     * travels back at exactly the body's speed (half a stride either side of the hip), in swing it returns on a cubic whose
     * end speeds match the stance's, so a foot leaving or meeting the ground does not jerk. A backpedal sweeps the other way.
     * The thigh angles put the straight leg's tip on that sweep; MotionFeet.stride then stands the stance foot on the ground
     * and lifts the swing foot LIFT_MIN to LIFT_RUN. The knee bends in swing; arms, sway, lean and twist follow the phase. Writes
     * each leg's phase, the duty, the weight and each foot's sweep and lift into the frame.
     */
    static void stride(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double v = Math.hypot(f.vF, f.vL), run = f.run, sh = f.sh, bp = f.bp;
        double dirF = v > 1e-9 ? f.vF / v : 1, dirL = v > 1e-9 ? f.vL / v : 0;
        double duty = DUTY_WALK + (DUTY_RUN - DUTY_WALK) * run, hF = HALF_WALK + (HALF_RUN - HALF_WALK) * run;
        double half = 1 / Math.sqrt(dirF * dirF / (hF * hF) + dirL * dirL / (HALF_SIDE * HALF_SIDE));
        double cad = MotionEngine.clamp(16 * v * duty / (2 * half), F_MIN, F_MAX) * f.seed.tempo();
        double h = 16 * v * duty / (2 * cad);
        if (!st.psiInit) {
            st.psiInit = true;
            st.psi = frac(f.seed.phase() / TAU);
        }
        // Fix 2: a start from standing takes its first step from the stance: one foot (the seed's) stands under its hip at the
        // middle of its stance (sweep 0) while the other is mid-swing (sweep 0 too), so the sweep can start at full weight and
        // the planted foot moves with the ground from the first frame; only the lift and the knee ease in
        boolean go = st.wTarget[MotionEngine.W_MOVE] > 0.5;
        if (!go) st.sweepFull = false;
        else if (!st.sweepFull && st.w[MotionEngine.W_MOVE] < START_W) {
            st.sweepFull = true;
            st.psi = frac(duty / 2 + ((f.seed.variantBias() & 1) == 0 ? 0 : 0.5));
        }
        // the phase follows the distance actually travelled this frame along the gait's direction, so a stance foot's sweep
        // (16 px per block of travel) moves with the ground however unevenly the entity's position arrives
        double travel = Math.max(0, f.dF * dirF + f.dL * dirL);
        st.psi = frac(st.psi + Math.min(travel * cad / Math.max(v, V_FLOOR), PSI_STEP * cad * f.dt));
        // the upper body eases in over UP_BLEND (the arms and lean), slower than the legs, so the sword arm does not pop into the run
        st.upW = f.first ? (go ? 1 : 0) : MotionEngine.toward(st.upW, go ? 1 : 0, f.dt / UP_BLEND);
        double a = f.m, sw = st.sweepFull ? 1 : a, b = MotionMath.ease(st.upW) * f.m * MotionMath.clamp01(v / 0.5), mSw = -2 * h * (1 - duty) / duty;
        f.gaitW = a;
        f.duty = duty;
        f.gaitV = v;
        double knee = KNEE_WALK + (KNEE_RUN - KNEE_WALK) * run, g = 0;
        for (int i = 0; i < 2; i++) {
            double q = i == 0 ? st.psi : frac(st.psi + 0.5), d, lift = 0;
            f.q[i] = q;
            if (q < duty) d = h * (1 - 2 * q / duty);
            else {
                double u = (q - duty) / (1 - duty), u2 = u * u, u3 = u2 * u;
                d = h * (-1 + 6 * u2 - 4 * u3) + mSw * (2 * u3 - 3 * u2 + u);
                lift = Math.sin(Math.PI * u);
            }
            f.sweep[i] = d;
            f.lift[i] = lift;
            if (i == 0) g = h > 1e-9 ? dirF * d / h : 0;
            if (sw <= 0) continue;
            double rx = Math.asin(MotionEngine.clamp(-dirF * d / MotionFeet.LEG, -1, 1));
            double rz = -Math.asin(MotionEngine.clamp(dirL * d / (MotionFeet.LEG * Math.cos(rx)), -1, 1));
            out.add(LEGS[i], RX, sw * Math.toDegrees(rx));
            out.add(LEGS[i], RZ, sw * Math.toDegrees(rz));
            if (lift > 0) out.add(SHINS[i], RX, a * knee * lift);
        }
        double crouch = sh * f.m;
        if (crouch > 0) {
            for (int i = 0; i < 2; i++) { out.add(SHINS[i], RX, 14 * crouch); out.add(LEGS[i], RX, -7 * crouch); }
            out.add("root", TY, crouch);
        }
        // Fix 2: the first step's lean into the travel, and a dip under it, from the smoothed speed's rise
        double acc = f.first || f.dt <= 0 ? 0 : (v - st.vPrev) / f.dt;
        st.vPrev = v;
        st.acc += (Math.max(0, acc) - st.acc) * (f.dt > 0 ? 1 - Math.exp(-ACC_HZ * f.dt) : 0);
        double first = MotionEngine.clamp(LEAN_GAIN * st.acc, 0, LEAN_MAX) * f.plant;
        if (first > 1e-6) {
            out.add("body", RX, first * dirF);
            out.add("head", RX, -0.5 * first * dirF);
            out.add("root", TY, DIP_PER_DEG * first);
        }
        if (b <= 0) return;
        double p = TAU * st.psi, s = Math.sin(p), cr = Math.cos(TAU * (st.psi - duty / 2));
        out.add("root", TY, 1.3 * b * run * cr * cr);
        out.add("root", TX, 0.35 * b * (1 - run) * s);
        out.add("body", RZ, 1.5 * b * (1 - run) * s);
        double lean = b * (3 + 7 * run) * (1 - bp) - 4 * bp * b;
        out.add("body", RX, lean);
        out.add("head", RX, -0.6 * lean);
        double twist = b * (5 + 3 * run) * (1 - sh);
        out.add("root", RY, -twist * g);
        out.add("body", RY, 1.8 * twist * g);
        double arm = b * (26 + 14 * run) * (1 - 0.75 * sh) * (1 - 0.3 * bp);
        out.add("right_arm", RX, arm * g);
        out.add("left_arm", RX, -arm * g);
        out.add("right_arm", RZ, b * (4 + 5 * run));
        out.add("left_arm", RZ, -b * (4 + 5 * run));
        double elbow = b * (10 + 45 * run), flick = MotionMath.tri(p - 0.85);
        out.add("right_forearm", RX, -elbow - 12 * b * Math.max(0, -flick));
        out.add("left_forearm", RX, -elbow - 12 * b * Math.max(0, flick));
    }

    static double frac(double x) { return x - Math.floor(x); }

    // ---- life ----

    static final int NOVELTY_KINDS = 6;
    private static final String[] SWAY = {"body", "head", "right_arm", "left_arm"};

    /**
     * Life: breathing, a per-unit posture, slow micro-sway, a weight shift onto the seed's idle leg with a hip drop, and novelty
     * idles. Breathing is a phase integrated with the trapezoid rule, so its rate can change without a jump and the phase
     * runs the same at any frame rate. Posture and sway are drawn from the seed, so two units never move alike. Novelty
     * idles play only on calm units, every 6 to 14 s, and never touch the sword arm's pitch.
     */
    static void life(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        MotionMath.Seed seed = f.seed;
        long bias = seed.variantBias();
        double lw = f.lw, m = f.m, cw = f.cw;
        if (f.first) {
            st.noveltyNext = f.now + 2 + seed.noveltyOffset();
            st.noveltyRng = MotionMath.mix(bias ^ 0x6A09E667F3BCC908L);
        }
        if (st.novKind >= 0 && (f.now - st.novStart >= st.novLength || f.now < st.novStart)) st.novKind = -1;
        if (st.novKind < 0 && f.now >= st.noveltyNext && f.calm >= 1) {
            st.novKind = Math.floorMod(next(st), NOVELTY_KINDS);
            st.novLength = 1.8 + 1.4 * MotionMath.unit(next(st));
            st.novSide = (next(st) & 1) == 0 ? -1 : 1;
            st.novStart = f.now;
            st.noveltyNext = f.now + 6 + 8 * MotionMath.unit(next(st));
            st.novelties++;
        }
        int kind = st.novKind;
        double ne = 0, wave = 0;
        if (kind >= 0) {
            double u = (f.now - st.novStart) / st.novLength, s = Math.sin(Math.PI * u), calm = MotionMath.ease(f.calm) * lw;
            ne = s * s * calm;
            wave = Math.sin(TAU * u) * s * calm;
        }

        // breathing
        double rate = 0.23 * seed.breath() * (1 + 0.7 * Math.max(cw, m * f.run));
        if (f.first) st.breathPhase = seed.phase();
        else st.breathPhase = (st.breathPhase + TAU * 0.5 * (st.breathRate + rate) * f.dt) % TAU;
        st.breathRate = rate;
        breathe(lw * (1 + 0.5 * cw) * (1 + (kind == 4 ? 1.6 * ne : 0)), st.breathPhase, out);

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
        for (int i = 0; i < 12; i++) {
            int k = 20 + 3 * i;
            double hz = 0.12 + 0.25 * draw(bias, k), ph = TAU * draw(bias, k + 1), amp = 1.2 + 1.3 * draw(bias, k + 2);
            double t = TAU * hz * f.now + ph;
            out.add(SWAY[i / 3], i % 3, sw * amp * (Math.sin(t) + 0.35 * Math.sin(2.3 * t + 1.7 * ph)) / 1.35);
        }

        // weight shift onto the idle leg, with a hip drop the root rotation carries and the torso counters
        double idle = lw * (1 - m) * (1 - 0.6 * cw), legSide = seed.idleLeg() == 0 ? -1 : 1;
        double commit = 0.65 + 0.35 * Math.sin(0.19 * f.now + 3.1 * seed.phase());
        out.add("root", TX, 0.55 * legSide * commit * idle);
        double drop = -legSide * 2.2 * commit * idle, r = Math.toRadians(drop);
        out.add("root", RZ, drop);
        out.add("root", TX, MotionFeet.LEG * Math.sin(r));
        out.add("root", TY, MotionFeet.LEG * (1 - Math.cos(r)));
        out.add("body", RZ, -1.3 * drop);

        if (kind >= 0) novelty(kind, ne, wave, st.novSide, out);
    }

    /**
     * Breathing at amplitude ba and phase b: the chest leads, the shoulders rise 0.45 rad later and the arms roll out 0.9 rad
     * later.
     */
    static void breathe(double ba, double b, Pose out) {
        double chest = Math.sin(b), shoulders = Math.sin(b - 0.45), arms = Math.sin(b - 0.9);
        out.add("body", RX, -1.1 * ba * chest);
        out.add("head", RX, 0.7 * ba * chest);
        out.add("right_arm", TY, -0.22 * ba * shoulders);
        out.add("left_arm", TY, -0.22 * ba * shoulders);
        out.add("right_arm", RZ, 1.1 * ba * arms);
        out.add("left_arm", RZ, -1.1 * ba * arms);
    }

    /**
     * Novelty idle {@code kind} at strength ne (a 0..1 bump), wave (a -1..1 swing) and side (-1 or 1): look around, shrug,
     * turn the weapon hand, shift the feet, sigh, or tilt the head. None touches the sword arm's or forearm's pitch.
     */
    static void novelty(int kind, double ne, double wave, double side, Pose out) {
        switch (kind) {
            case 0 -> { out.add("head", RY, 40 * side * wave); out.add("head", RX, 4 * ne); }
            case 1 -> {
                out.add("right_arm", TY, -0.8 * ne); out.add("left_arm", TY, -0.8 * ne);
                out.add("right_arm", RZ, 3 * ne); out.add("left_arm", RZ, -3 * ne); out.add("body", RX, -2 * ne);
            }
            case 2 -> { out.add("right_hand", RZ, 15 * wave); out.add("left_arm", RX, -8 * ne); out.add("head", RX, 6 * ne); }
            case 3 -> { out.add("root", TX, 0.7 * side * ne); out.add("body", RZ, -2 * side * ne); }
            case 4 -> { out.add("body", RX, -2 * ne); out.add("head", RX, -3 * ne); }
            default -> { out.add("head", RZ, 13 * side * ne); out.add("head", RX, -4 * ne); }
        }
    }

    static long next(MotionEngine.State st) { return st.noveltyRng = MotionMath.mix(st.noveltyRng + 0x9E3779B97F4A7C15L); }

    /** A seed draw in [0, 1) for slot i. */
    static double draw(long bias, int i) { return MotionMath.unit(MotionMath.mix(bias + 0x9E3779B97F4A7C15L * (i + 1))); }

    static double sym(long bias, int i, double amp) { return amp * (2 * draw(bias, i) - 1); }

    // ---- hit reactions ----

    static final int MAX_PULSES = 8;

    /** One blow's procedural reaction: its zone (0 head, 1 body, 2 legs, 3 block), direction, side, start, scale and weight (0 light, 1, 2 heavy). */
    record Pulse(int zone, double dir, double side, double start, double scale, int weight) {
        double envelope(double now) {
            double rise = zone == 2 ? 0.15 : 0.08, fall = zone == 2 ? 0.5 : 0.38, age = now - start;
            if (age <= 0) return 0;
            if (age < rise) return MotionMath.ease(age / rise);
            return 1 - MotionMath.ease((age - rise) / fall);
        }

        boolean over(double now) { return now - start > (zone == 2 ? 0.75 : 0.56); }
    }

    /** The springed reaction channels. KNEE_R and KNEE_L go to the planted-feet solve as extra knee bend. */
    static final int HEAD_RX = 0, HEAD_RZ = 1, BODY_RX = 2, BODY_RZ = 3, RARM_RX = 4, LARM_RX = 5, ARMS_RZ = 6, ROOT_TZ = 7,
            KNEE_R = 8, KNEE_L = 9, ROOT_TX = 10, REACT = 11;
    /** The reaction springs integrate in steps of at most 1 / REACT_HZ s, with the blows' envelopes evaluated at each. */
    static final double REACT_HZ = 120;

    static void pulse(MotionEngine.State st, MotionInput.Hit hit, double at, double scale) { pulse(st, hit, at, scale, 1); }

    static void pulse(MotionEngine.State st, MotionInput.Hit hit, double at, double scale, int weight) {
        int zone = hit.blocked() ? 3 : switch (MotionEngine.zone(hit.zone())) { case "head" -> 0; case "legs" -> 2; default -> 1; };
        double b = Math.toRadians(MotionEngine.finite(hit.bearing()));
        st.pulses.add(new Pulse(zone, MotionEngine.clamp(2 * Math.cos(b), -1, 1), MotionEngine.clamp(1.5 * Math.sin(b), -1, 1), at, scale, weight));
        if (st.pulses.size() > MAX_PULSES) st.pulses.remove(0);
    }

    /**
     * Hit reactions, part of the action layer: each blow pushes its zone the way the blow travelled. A head blow snaps the
     * head, a body blow doubles the torso, a legs blow buckles the knees (the side it came from more; a blow from straight
     * ahead or behind, the idle leg's side) and a blocked blow jolts the shield arm. Each target sum is springed, so
     * blows overlap smoothly. The springs step at most 1 / REACT_HZ s at a time against the envelopes evaluated at each
     * step, so a blow lands the same at any frame rate. The reactions fade with life,
     * so a corpse does not twitch.
     */
    static void react(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        for (int i = st.pulses.size() - 1; i >= 0; i--) {
            Pulse p = st.pulses.get(i);
            if (p.over(f.now) || p.start() - f.now > 1) st.pulses.remove(i);
        }
        double[] target = st.reactTarget;
        if (f.first) {
            targets(f, st, f.now, target);
            System.arraycopy(target, 0, st.reactPrev, 0, REACT);
        }
        int n = Math.max(1, (int) Math.ceil(f.dt * REACT_HZ - 1e-9));
        double h = f.dt / n;
        for (int j = 1; j <= n; j++) {
            targets(f, st, f.now - f.dt + j * h, target);
            for (int i = 0; i < REACT; i++) {
                boolean knee = i == KNEE_R || i == KNEE_L;
                MotionMath.step(st.react, st.reactVel, i, st.reactPrev[i], target[i], knee ? 150 : 300, knee ? 18 : 22, h);
                st.reactPrev[i] = target[i];
                st.react[i] = MotionEngine.clamp(st.react[i], -90, 90);
                st.reactVel[i] = MotionEngine.clamp(st.reactVel[i], -3000, 3000);
            }
        }
        double[] r = st.react;
        double lw = f.lw;
        out.add("head", RX, lw * r[HEAD_RX]);
        out.add("head", RZ, lw * r[HEAD_RZ]);
        out.add("body", RX, lw * r[BODY_RX]);
        out.add("body", RZ, lw * r[BODY_RZ]);
        out.add("right_arm", RX, lw * r[RARM_RX]);
        out.add("left_arm", RX, lw * r[LARM_RX]);
        out.add("right_arm", RZ, lw * r[ARMS_RZ]);
        out.add("left_arm", RZ, -lw * r[ARMS_RZ]);
        out.add("root", TZ, lw * r[ROOT_TZ]);
        out.add("root", TX, lw * r[ROOT_TX]);
    }

    /** The reaction channels' targets at time t: every live blow's push, summed and clamped. */
    static void targets(MotionEngine.Frame f, MotionEngine.State st, double t, double[] target) {
        Arrays.fill(target, 0);
        double ownSide = f.seed.idleLeg() == 0 ? 0.5 : -0.5;
        for (int i = 0, n = st.pulses.size(); i < n; i++) {
            Pulse p = st.pulses.get(i);
            double e = p.envelope(t) * p.scale(), d = p.dir(), s = p.side();
            if (e == 0) continue;
            switch (p.zone()) {
                case 0 -> { target[HEAD_RX] -= 24 * d * e; target[HEAD_RZ] += 16 * s * e; target[BODY_RX] -= 5 * d * e; target[ARMS_RZ] += 4 * e;
                    if (p.weight() >= 2) { target[KNEE_R] += 18 * e; target[KNEE_L] += 18 * e; } }
                case 1 -> {
                    target[BODY_RX] += 16 * d * e; target[HEAD_RX] += 7 * d * e; target[RARM_RX] -= 9 * e; target[LARM_RX] -= 9 * e;
                    target[BODY_RZ] += 12 * s * e; target[ROOT_TZ] += 0.6 * d * e; target[ROOT_TX] += 1.2 * s * e;
                    if (p.weight() >= 2) { target[KNEE_R] += 18 * e; target[KNEE_L] += 18 * e; }
                }
                case 2 -> {
                    double k = Math.abs(s) >= 0.1 ? s : ownSide, ek = Math.min(e, 1.35 * p.envelope(t));
                    target[KNEE_R] += 44 * ek * (0.55 + 0.45 * k);
                    target[KNEE_L] += 44 * ek * (0.55 - 0.45 * k);
                    target[BODY_RX] += 7 * e; target[ARMS_RZ] += 10 * e; target[BODY_RZ] += 4 * s * e; target[HEAD_RX] += 4 * e;
                }
                default -> { target[LARM_RX] += 14 * e; target[BODY_RX] -= 4 * d * e; target[ROOT_TZ] += 0.5 * d * e; target[HEAD_RX] -= 3 * e; }
            }
        }
        for (int i = 0; i < REACT; i++) {
            boolean knee = i == KNEE_R || i == KNEE_L;
            target[i] = knee ? MotionEngine.clamp(target[i], 0, 80) : i == ROOT_TZ || i == ROOT_TX ? MotionEngine.clamp(target[i], -2, 2) : MotionEngine.clamp(target[i], -60, 60);
        }
    }

    // ---- look ----

    /**
     * Look: the head follows the look yaw and pitch through a critically damped spring with a speed limit, so a snap turn
     * still sweeps. The spring chases last frame's look (the look holds across a frame), and the result fades with the look
     * weight. Past 30 degrees, 35 percent of the extra yaw passes into the body as twist.
     */
    static void look(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double[] pos = st.look, vel = st.lookVel, prev = st.lookPrev;
        if (f.first) { pos[0] = prev[0] = f.headYaw; pos[1] = prev[1] = f.headPitch; }
        for (int i = 0; i < 2; i++) {
            double x0 = pos[i], lim = 400 * f.dt, bound = i == 0 ? 90 : 65;
            MotionMath.step(pos, vel, i, prev[i], 160, 25.3, f.dt);
            vel[i] = MotionEngine.clamp(vel[i], -400, 400);
            pos[i] = MotionEngine.clamp(x0 + MotionEngine.clamp(pos[i] - x0, -lim, lim), -bound, bound);
        }
        prev[0] = f.headYaw;
        prev[1] = f.headPitch;
        double yaw = pos[0] * f.lookW, share = Math.signum(yaw) * 0.35 * Math.max(0, Math.abs(yaw) - 30);
        out.add("head", RY, yaw - share);
        out.add("body", RY, share);
        out.add("head", RX, pos[1] * f.lookW);
    }

    // ---- secondary ----

    /** The secondary springs' channels in State.lag. */
    static final int CAPE_X = 0, CAPE_Y = 1, CREST_X = 2, CREST_Y = 3, RSTRAP_Z = 4, RSTRAP_X = 5, LSTRAP_Z = 6, LSTRAP_X = 7, TABARD = 8, LAGS = 9;
    private static final double[] LAG_K = {70, 70, 160, 160, 80, 80, 100, 100, 600}, LAG_D = {8, 8, 9, 9, 7, 7, 7, 7, 35};
    /** How far loose straps swing per degree of hip lag, how much of the forward leg the tabard follows, and the soft min's rounding (degrees). */
    static final double STRAP_GAIN = 1.2, TABARD_FOLLOW = 0.7, SOFT_MIN = 12;

    /**
     * Secondary motion: cape, tabard, crest and strap bones lag their parents on underdamped springs and settle. The cape
     * streams back with speed and flutters on a run, and swings out on a body turn. The tabard spring-lags the forward leg,
     * taken through a soft min, so it does not jerk where the legs cross or when a clip swings them. The
     * crest trails the head. Loose straps swing with the hips. Each spring chases a target moving linearly across the frame,
     * and the flutter integrates its rate with the trapezoid rule. Once a corpse's death clip has played, the lag terms fade
     * out with the settle weight (the straps' own, MotionEngine.pose), so a corpse lies still. Bones a model lacks are ignored by the renderer.
     */
    static void secondary(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double[] t = st.lagTarget, lag = st.lag, prev = st.lagPrev;
        double bodyX = out.get("body", RX) + out.get("root", RX), bodyY = out.get("body", RY) + out.get("root", RY);
        double headX = out.get("head", RX), headY = out.get("head", RY);
        double hipZ = out.get("root", RZ) + out.get("body", RZ) + 8 * out.get("root", TX), hipX = bodyX + 5 * out.get("root", TY);
        t[CAPE_X] = bodyX; t[CAPE_Y] = bodyY; t[CREST_X] = headX; t[CREST_Y] = headY;
        t[RSTRAP_Z] = t[LSTRAP_Z] = hipZ;
        t[RSTRAP_X] = t[LSTRAP_X] = hipX;
        double rLeg = out.get("right_leg", RX), lLeg = out.get("left_leg", RX), legs = softMin(softMin(rLeg, lLeg), 0);
        t[TABARD] = legs;
        double rate = 7 + 5 * f.run;
        if (f.first) {
            System.arraycopy(t, 0, lag, 0, LAGS);
            System.arraycopy(t, 0, prev, 0, LAGS);
            st.legsPrev[0] = rLeg;
            st.legsPrev[1] = lLeg;
            st.yawRate = st.yawRateTarget = f.yawRate;
            st.flutterRate = rate;
        }
        for (int i = 0; i < LAGS; i++) {
            if (i == TABARD) tabardSteps(f, st, rLeg, lLeg);
            else MotionMath.step(lag, st.lagVel, i, prev[i], t[i], LAG_K[i], LAG_D[i], f.dt);
            lag[i] = MotionEngine.clamp(lag[i], -720, 720);
            st.lagVel[i] = MotionEngine.clamp(st.lagVel[i], -5000, 5000);
            prev[i] = t[i];
        }
        st.yawRate = MotionMath.approach(st.yawRate, st.yawRateTarget, 5, f.dt);
        st.yawRateTarget = f.yawRate;
        st.flutter = (st.flutter + 0.5 * (st.flutterRate + rate) * f.dt) % TAU;
        st.flutterRate = rate;

        double a = f.m * Math.min(f.speed / 0.6, 1), settle = f.settle;
        double lagX = (lag[CAPE_X] - bodyX) * settle, lagY = (lag[CAPE_Y] - bodyY) * settle;
        out.add("cape", RX, MotionEngine.clamp(a * (14 + 22 * f.run) + 2.5 * a * Math.sin(st.flutter) + 0.9 * lagX, -15, 75));
        out.add("cape", RZ, MotionEngine.clamp(0.6 * lagY - 0.03 * st.yawRate * settle, -20, 20));
        double follow = legs + (lag[TABARD] - legs) * settle;
        out.add("tabard", RX, MotionEngine.clamp(a * (6 + 10 * f.run) + 0.6 * lagX + TABARD_FOLLOW * follow, -60, 60));
        out.add("crest", RX, MotionEngine.clamp(1.1 * (lag[CREST_X] - headX) * settle + 6 * a, -35, 35));
        out.add("crest", RZ, MotionEngine.clamp(0.5 * (lag[CREST_Y] - headY) * settle, -25, 25));
        out.add("right_strap", RZ, MotionEngine.clamp(STRAP_GAIN * (lag[RSTRAP_Z] - hipZ), -35, 35) * f.straps);
        out.add("right_strap", RX, MotionEngine.clamp(STRAP_GAIN * (lag[RSTRAP_X] - hipX), -35, 35) * f.straps);
        out.add("left_strap", RZ, MotionEngine.clamp(STRAP_GAIN * (lag[LSTRAP_Z] - hipZ), -35, 35) * f.straps);
        out.add("left_strap", RX, MotionEngine.clamp(STRAP_GAIN * (lag[LSTRAP_X] - hipX), -35, 35) * f.straps);
    }

    /**
     * Steps the tabard's spring across the frame in steps of at most 1 / MotionBody.REACT_HZ s. The thighs are taken as
     * moving linearly across the frame and the smooth min is taken of them at each step, so the target follows the smooth
     * min's rounding where the legs cross instead of a chord across it, and the tabard is the same at any frame rate.
     */
    static void tabardSteps(MotionEngine.Frame f, MotionEngine.State st, double rLeg, double lLeg) {
        int n = Math.max(1, (int) Math.ceil(f.dt * REACT_HZ - 1e-9));
        double h = f.dt / n, r0 = st.legsPrev[0], l0 = st.legsPrev[1], from = st.lagPrev[TABARD];
        for (int j = 1; j <= n; j++) {
            double u = j / (double) n, to = j == n ? st.lagTarget[TABARD] : softMin(softMin(lerp(r0, rLeg, u), lerp(l0, lLeg, u)), 0);
            MotionMath.step(st.lag, st.lagVel, TABARD, from, to, LAG_K[TABARD], LAG_D[TABARD], h);
            from = to;
        }
        st.legsPrev[0] = rLeg;
        st.legsPrev[1] = lLeg;
    }

    /**
     * The smaller of a and b, rounded where they are within SOFT_MIN degrees: its slope changes smoothly (C2) from one to
     * the other, so a bone following the forward leg has no velocity cusp where the legs pass. It is exactly min(a, b) where
     * a == b and where they are SOFT_MIN or more apart, so it has no bias at rest; in between it is never below min(a, b)
     * and at most 0.633 deg above it (27/512 of SOFT_MIN, where they are SOFT_MIN / 4 apart). Being exact both at a crossing
     * and far from it, it cannot be monotone in each argument: it dips back by that 0.633 deg as they part.
     */
    static double softMin(double a, double b) {
        double h = Math.abs(a - b);
        if (h >= SOFT_MIN) return Math.min(a, b);
        double c = 1 - h / SOFT_MIN;
        return 0.5 * (a + b - h * (1 - c * c * c));
    }

    static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
