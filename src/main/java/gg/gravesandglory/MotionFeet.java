package gg.gravesandglory;

import static gg.gravesandglory.MotionEngine.RX;
import static gg.gravesandglory.MotionEngine.RY;
import static gg.gravesandglory.MotionEngine.RZ;
import static gg.gravesandglory.MotionEngine.TX;
import static gg.gravesandglory.MotionEngine.TY;
import static gg.gravesandglory.MotionEngine.TZ;

/**
 * Planted feet for {@link MotionEngine} (animation rework design section 6.1, G3, G4): each foot is a rigid flat sole that
 * rocks on its heel or toe edge, and each leg is solved so that edge, not the shin's tip, stays where the foot was put. Rig
 * space is y down and -z forward; the root pivot is at the origin, the hips 12 px below it. Uses only the scratch in
 * {@link MotionEngine.State}, so it does not allocate. Pure.
 *
 * <p>The model has no ankle, so a foot is flat exactly when its shin is upright. Standing, the hips sit back and dip so both
 * shins stand upright over soft knees; a knee buckle rolls a foot onto its toe. Striding, the hips' height follows the stance
 * leg like a pendulum (its knee bend aimed, heel strike to toe-off), and each swing lands where that pendulum can reach.
 */
final class MotionFeet {
    private MotionFeet() {}

    /** Hip-to-foot length of a straight leg and the shin's length (px), and the least knee bend a planted leg keeps (degrees). */
    static final double LEG = 12, SHIN = 6, THIGH = LEG - SHIN, SOFT_KNEE = 12;
    /** Hip spacing (px) and the largest roll (degrees) the hips take to let one knee buckle more than the other. */
    static final double HIP_WIDTH = 3.8, MAX_ROLL = 30;
    static final double[][] HIPS = {{-1.9, 12, 0}, {1.9, 12, 0}};
    static final String[] LEGS = {"right_leg", "left_leg"}, SHINS = {"right_shin", "left_shin"};
    /**
     * The knight's sole in shin space (px): {xMin, xMax, y, zToe, zHeel}. Families whose model is not posed yet, and the
     * standalone checks, use it.
     */
    static final double[] KNIGHT_SOLE = {-2.5, 2.5, 6, -3, 2};
    /**
     * Standing: the hips' sit-back (px, either way), rise (px) and roll (deg) limits, how far a foot may rock off its pitch
     * (deg), and how far its heel lifts per degree of knee buckle.
     */
    static final double SIT_MAX = 3, DIP_UP = 4, STAND_ROLL = 12, ROCK_MAX = 15, HEEL_LIFT = 0.5;
    /**
     * The standing solve's weights: per px of a leg's reach miss, per radian of a knee bent less than its least (KNEE_MIN deg
     * or its authored bend, whichever is more, plus its buckle) and per radian it bends off that least either way, per px of
     * hip drop off its want and of sit-back, per degree of extra roll and of a foot's rock; and its steps.
     */
    static final double W_REACH = 30, W_KNEE = 30, W_BEND = 3, KNEE_MIN = 12, W_DIP = 1, W_SIT = 0.5, W_ROLL = 0.12, W_ROCK = 0.1;
    static final int STAND_STEPS = 12;
    /** Standing: an authored pitch past this (deg) rests the foot on that edge whenever its solve allows. */
    static final double REST_PICK = 2;
    /** solveAt: how much nearer (deg) one branch must be than the other to be taken in full. */
    static final double BRANCH_TIE = 40;
    /** Rocker: a pitch this far (deg) past flat still rests on that edge; the knee's largest sideways offset from the foot (px). */
    static final double FLAT_SLACK = 0.5, LAT_MAX = 5.5;
    /**
     * Hip pendulum (G3): the stance knee aim's ease (s); the aim in stance (deg) and its crouch extra, and backing; the aim at
     * lift-off, forward and backing; the least bend another stance leg keeps (deg); the hips' height range (px, y down).
     */
    static final double KAP_TAU = 0.06, KAP_STANCE = 12, KAP_CROUCH = 14, KAP_BACK = 40, KAP_LIFT = 45, KAP_LIFT_BACK = 30,
            KAP_MIN = 3, HIP_HI = 12, HIP_LO = 18;
    /** Swing lift slow and its rise to fast (px), a standing step's target foot centre limit off the centre line (px). */
    static final double SWING_H = 2.4, SWING_H_FAST = 1.6, FOOT_X = 2.85;

    /** One edge solve's result: the foot's pitch and knee bend (deg), the knee off the edge (px, down and back), the edge, and how far it misses (px). */
    static final class Rock {
        double phi, kap, kw, ku, ex, ey, ez, miss;
    }

    /**
     * Plants the feet. Standing (weight pw), the hips roll toward a deeper knee buckle, sit back so each shin stands upright
     * over a soft knee, and dip just enough for both legs to reach. Striding on a ground (weight wm), the hips' height follows
     * the stance leg's knee aim. Each leg is then solved under the root the frame gets so its foot rests on the heel or toe edge
     * at its contact (targets()). Moving off the ground, turning in place or a clip that keys the legs hands the feet back,
     * eased by the same weights; a knee buckle then bends the knees directly. Death hands them back too.
     *
     * <p>Records in State how far the corrected channels sit from the uncorrected ones (plantRot, plantPx), so a fade that
     * changes how much the clips own the legs can count the hand-off in its pop budget.
     */
    static void plant(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double act = 1 - Math.min(1, f.bodyAction);
        double base = (1 - f.m) * (1 - f.tw) * f.plant, pw = base * act;
        // G3: the moving plant's weight, eased out in the air and back in on landing
        boolean grounded = f.ground && !f.air && !f.dead;
        st.groundW = f.first ? (grounded ? 1 : 0) : MotionMath.clamp01(st.groundW + (grounded ? 1 : -1) * f.dt / GROUND_BLEND);
        double wm = f.ground && !f.dead ? f.m * (1 - f.tw) * act * st.groundW * f.plant : 0;
        double[] k = st.knee, sole = f.sole;
        k[0] = Math.max(0, st.react[MotionBody.KNEE_R]) * f.lw;
        k[1] = Math.max(0, st.react[MotionBody.KNEE_L]) * f.lw;
        double[] root = out.get("root"), m = st.orient;
        for (int i = 0; i < 2; i++) {
            double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), p = st.feet[i];
            chain(m, null, leg, shin);
            double ps = heading(m), ph = Math.toDegrees(Math.asin(MotionEngine.clamp(-m[5], -1, 1)));
            st.chPsi[i] = ps;
            st.rpiv[i] = ph > FLAT_SLACK ? 1 : 0;
            // the footprint: the flat sole's centre-line point under the knee, set back from the edge the authored foot rests on
            double ze = Math.abs(ph) <= FLAT_SLACK ? 0 : sole[st.rpiv[i] == 1 ? 3 : 4], pr = Math.toRadians(ps);
            foot(i, leg, shin, sole[2], ze, p);
            p[0] -= ze * Math.sin(pr);
            p[2] -= ze * Math.cos(pr);
            st.tphi[i] = ph + HEEL_LIFT * k[i];
        }
        if (f.ground) targets(f, st, root);
        else loose(st);
        st.plantW[0] = st.plantW[1] = pw + wm;
        st.plantRot = st.plantPx = 0;

        if (base > 0 || wm > 0) {
            double soft = Math.cos(Math.toRadians(SOFT_KNEE / 2));
            double eR = LEG * (soft - Math.cos(Math.toRadians(Math.min(SOFT_KNEE + k[0], 150) / 2)));
            double eL = LEG * (soft - Math.cos(Math.toRadians(Math.min(SOFT_KNEE + k[1], 150) / 2)));
            double roll = MotionEngine.clamp(-Math.toDegrees(Math.asin(MotionEngine.clamp((eR - eL) / HIP_WIDTH, -1, 1))), -MAX_ROLL, MAX_ROLL);
            double rr = Math.toRadians(roll), rollX = LEG * Math.sin(rr), rollY = LEG * (1 - Math.cos(rr));
            double[] full = st.rootFull, h = st.aim;
            System.arraycopy(root, 0, full, 0, 6);
            full[RZ] += roll;
            full[TX] += rollX;
            full[TY] += rollY;
            // Standing (there is no ankle): each foot keeps its authored pitch on its authored edge, its heel lifting with its
            // knee's buckle, and that fixes where its knee is; each thigh must then reach its knee from the hips. Two feet at
            // fixed pitches leave the hips almost no freedom (a staggered stance has a single placement, which an authored crouch
            // rarely matches), so the hips' drop, sit-back and roll and each foot's rock off its pitch are traded in one small
            // least-squares solve: the legs must reach with their knees forward, the rest is spent by weight. It starts from the
            // same point every frame and takes a fixed number of steps, so it moves smoothly with its inputs.
            for (int i = 0; i < 2; i++) st.standSide[i] = st.tphi[i] >= 0 ? 1 : -1;
            // a buckle bends each knee by its amount past its least bend; the solve finds the hips' drop for it
            double dipW = 0;
            for (int i = 0; i < 2; i++) {
                dipW = Math.max(dipW, standDip(i, st, sole, full, out.get(LEGS[i]), h));
                st.standK[i] = Math.toRadians(Math.max(KNEE_MIN, out.get(SHINS[i])[RX]) + k[i]);
            }
            dipW = MotionEngine.clamp(dipW, 0, 9);
            double[] x = st.standX;
            standSolve(st, sole, root, full, roll, dipW, out, x);
            double dip = x[0], sit = x[1], rollT = roll + x[2];
            st.tphi[0] += x[3];
            st.tphi[1] += x[4];
            seat(full, root, rollT, dip, sit);

            // Standing alone (no stride weight), the legs are solved under the full standing root and eased in by the plant
            // weight like the root is, never re-solved under the part-blended root: hips part way down from the full seat
            // can leave no forward-kneed crossing on the current edge, and the far one is a pop.
            boolean same = !(wm > 0);
            if (base > 0) {
                double rr2 = Math.toRadians(rollT), rot = Math.abs(rollT);
                double px = Math.max(Math.max(Math.abs(dip), Math.abs(sit)), Math.max(Math.abs(LEG * Math.sin(rr2)), LEG * (1 - Math.cos(rr2))));
                for (int i = 0; i < 2; i++) {
                    double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
                    rock(i, f, st, full, leg, shin, sol, same);
                    rot = Math.max(rot, Math.max(Math.abs(sol[0] - leg[RX]), Math.max(Math.abs(sol[1] - leg[RZ]), Math.abs(sol[5] - leg[RY]))));
                    rot = Math.max(rot, Math.max(Math.abs(sol[2] - shin[RX]), Math.max(Math.abs(sol[3] - shin[RY]), Math.abs(sol[4] - shin[RZ]))));
                    for (int c = TX; c <= TZ; c++) px = Math.max(px, Math.abs(shin[c]));
                }
                st.plantRot = base * rot;
                st.plantPx = base * px;
            }

            double dm = 0;
            if (wm > 0 && f.gaitW > 0) dm = pendulum(f, st, root, out);
            else {
                st.driver = -1;
                st.hipY = Double.NaN;
                dm = rollY + dip;
            }
            if (pw > 0) {
                double pr = Math.toRadians(pw * rollT);
                root[RZ] += pw * rollT;
                root[TX] += LEG * Math.sin(pr);
                root[TY] += LEG * (1 - Math.cos(pr)) + pw * dip;
                root[TZ] += pw * sit;
            }
            root[TY] += wm * dm;
            for (int i = 0; i < 2; i++) {
                double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
                if (!same) rock(i, f, st, root, leg, shin, sol, true);
                double w = st.plantW[i];
                if (!(w > 0)) continue;
                leg[RX] += (sol[0] - leg[RX]) * w;
                leg[RZ] += (sol[1] - leg[RZ]) * w;
                leg[RY] += (sol[5] - leg[RY]) * w;
                shin[RX] += (sol[2] - shin[RX]) * w;
                shin[RY] += (sol[3] - shin[RY]) * w;
                shin[RZ] += (sol[4] - shin[RZ]) * w;
                for (int c = TX; c <= TZ; c++) shin[c] -= shin[c] * w;
            }
        }
        double freeR = (1 - st.plantW[0]) * k[0], freeL = (1 - st.plantW[1]) * k[1];
        if (freeR > 0 || freeL > 0) {
            out.add("right_shin", RX, freeR);
            out.add("right_leg", RX, -freeR / 2);
            out.add("left_shin", RX, freeL);
            out.add("left_leg", RX, -freeL / 2);
            double rootFree = (1 - pw) * (k[0] + k[1]);
            out.add("root", TY, LEG * (1 - Math.cos(Math.toRadians(rootFree / 4))));
        }
    }

    /**
     * Leg i standing at pitch phi (deg, heel up positive) on its standing side's edge (the toe heel up, the heel toe up), its
     * hip under root: writes {knee w, knee u, hip w, hip u, thigh reach, pitch (rad)} into st.stand, w down and u heel-ward
     * along the foot (px). h is scratch.
     */
    static double[] stance(int i, MotionEngine.State st, double[] sole, double[] root, double[] leg, double[] h, double phi) {
        double[] c = st.con[i], g = st.stand;
        hip(i, root, leg, h);
        double ph = Math.toRadians(phi), ps = Math.toRadians(st.conPsi[i]), bx = Math.sin(ps), bz = Math.cos(ps);
        // one edge for the whole solve: the knee's path bends where the foot passes flat onto the other edge, and either way
        // from flat the knee rises, which would give the solve two answers (rock heel up or toe up) to jump between
        double ze = sole[st.standSide[i] > 0 ? 3 : 4], re = Math.hypot(ze, sole[2]), ga = Math.atan2(ze, sole[2]);
        double ex = c[0] + ze * bx, ez = c[2] + ze * bz;
        double del = MotionEngine.clamp((h[0] - ex) * bz - (h[2] - ez) * bx, -LAT_MAX, LAT_MAX);
        g[0] = c[1] - st.lift[i] - re * Math.cos(ph + ga);
        g[1] = ex * bx + ez * bz - re * Math.sin(ph + ga);
        g[2] = h[1];
        g[3] = h[0] * bx + h[2] * bz;
        g[4] = Math.sqrt(THIGH * THIGH - del * del);
        g[5] = ph;
        return g;
    }

    /** How far (px) the hips must drop for leg i, standing at its pitch (stance()), to bend its knee by at least SOFT_KNEE (deg). */
    static double standDip(int i, MotionEngine.State st, double[] sole, double[] root, double[] leg, double[] h) {
        double[] g = stance(i, st, sole, root, leg, h, st.tphi[i]);
        double ph = g[5], rp = g[4], kc = ph + Math.acos(MotionEngine.clamp((g[0] - g[2]) / rp, -1, 1));
        double kw = Math.min(Math.max(kc, Math.toRadians(SOFT_KNEE)), ph + Math.toRadians(80));
        return g[0] - rp * Math.cos(ph - kw) - g[2];
    }

    /** Sets full to root rolled by roll (deg) about the hips' centre, dropped by dy and sat back by dz (px). */
    static void seat(double[] full, double[] root, double roll, double dy, double dz) {
        double rr = Math.toRadians(roll);
        System.arraycopy(root, 0, full, 0, 6);
        full[RZ] += roll;
        full[TX] += LEG * Math.sin(rr);
        full[TY] += LEG * (1 - Math.cos(rr)) + dy;
        full[TZ] += dz;
    }

    /** The standing solve's limits and largest step per unknown: hip drop, sit-back (px), extra roll, right and left rock (deg). */
    static final double[] STAND_LO = {-DIP_UP, -SIT_MAX, -STAND_ROLL, -ROCK_MAX, -ROCK_MAX}, STAND_HI = {9, SIT_MAX, STAND_ROLL, ROCK_MAX, ROCK_MAX},
            STAND_STEP = {1.5, 1.5, 6, 6, 6};

    /**
     * Standing: the hips' drop, sit-back, extra roll (on top of the buckle's roll) and each foot's rock that best let both
     * standing legs reach their knees (Levenberg-Marquardt on standRes). It starts from the want (no sit-back, roll or rock)
     * every frame and takes a fixed number of steps, so its answer depends only on this frame's pose, not on the frame rate.
     * Writes them into x.
     */
    static void standSolve(MotionEngine.State st, double[] sole, double[] root, double[] full, double roll, double dipW, Pose out, double[] x) {
        int n = 5, m = 11, jo = 3 * m, ao = jo + m * n;
        double[] s = st.standS, t = st.standT, lo = st.standLo, hi = st.standHi;
        // each foot's rock keeps it on its side of flat
        for (int j = 0; j < n; j++) {
            lo[j] = STAND_LO[j];
            hi[j] = STAND_HI[j];
        }
        for (int i = 0; i < 2; i++) {
            if (st.standSide[i] > 0) lo[3 + i] = Math.min(ROCK_MAX, Math.max(lo[3 + i], -st.tphi[i]));
            else hi[3 + i] = Math.max(-ROCK_MAX, Math.min(hi[3 + i], -st.tphi[i]));
        }
        x[0] = dipW;
        x[1] = x[2] = x[3] = x[4] = 0;
        for (int j = 0; j < n; j++) x[j] = MotionEngine.clamp(x[j], lo[j], hi[j]);
        double lam = 1e-2, cost = standRes(st, sole, root, full, roll, dipW, out, x, s, 0);
        for (int it = 0; it < STAND_STEPS; it++) {
            for (int j = 0; j < n; j++) {
                double keep = x[j], d = j < 2 ? 1e-3 : 1e-2;
                x[j] = keep + d;
                standRes(st, sole, root, full, roll, dipW, out, x, s, m);
                for (int q = 0; q < m; q++) s[jo + q * n + j] = (s[m + q] - s[q]) / d;
                x[j] = keep;
            }
            for (int a = 0; a < n; a++) {
                for (int b = 0; b < n; b++) {
                    double v = 0;
                    for (int q = 0; q < m; q++) v += s[jo + q * n + a] * s[jo + q * n + b];
                    s[ao + a * (n + 1) + b] = v;
                }
                s[ao + a * (n + 1) + a] *= 1 + lam;
                s[ao + a * (n + 1) + a] += 1e-6;
                double v = 0;
                for (int q = 0; q < m; q++) v -= s[jo + q * n + a] * s[q];
                s[ao + a * (n + 1) + n] = v;
            }
            // the damped normal equations are symmetric positive definite, so no pivoting
            for (int a = 0; a < n; a++) {
                double piv = s[ao + a * (n + 1) + a];
                for (int b = a + 1; b < n; b++) {
                    double fa = s[ao + b * (n + 1) + a] / piv;
                    for (int c = a; c <= n; c++) s[ao + b * (n + 1) + c] -= fa * s[ao + a * (n + 1) + c];
                }
            }
            for (int a = n - 1; a >= 0; a--) {
                double v = s[ao + a * (n + 1) + n];
                for (int c = a + 1; c < n; c++) v -= s[ao + a * (n + 1) + c] * s[ao + c * (n + 1) + n];
                s[ao + a * (n + 1) + n] = v / s[ao + a * (n + 1) + a];
            }
            for (int j = 0; j < n; j++) t[j] = MotionEngine.clamp(x[j] + MotionEngine.clamp(s[ao + j * (n + 1) + n], -STAND_STEP[j], STAND_STEP[j]), lo[j], hi[j]);
            double next = standRes(st, sole, root, full, roll, dipW, out, t, s, 2 * m);
            if (next < cost) {
                System.arraycopy(t, 0, x, 0, n);
                System.arraycopy(s, 2 * m, s, 0, m);
                cost = next;
                lam = Math.max(lam / 4, 1e-4);
            } else lam *= 8;
        }
    }

    /**
     * The standing solve's residuals at x into r from at (11): each standing leg's reach miss and knee short of its floor, then
     * the hip drop off its want, the sit-back, the extra roll, each foot's rock and each standing knee's bend off its floor, by
     * weight; returns their sum of squares. The last keeps the answer from wandering: a near-straight knee bends tens of degrees
     * per px the hips move, so answers a few hundredths apart in cost can sit ten degrees of knee apart, and the cheaper one
     * changes as the pose moves. full is scratch.
     */
    static double standRes(MotionEngine.State st, double[] sole, double[] root, double[] full, double roll, double dipW, Pose out, double[] x, double[] r, int at) {
        seat(full, root, roll + x[2], x[0], x[1]);
        for (int i = 0; i < 2; i++) {
            r[at + i] = r[at + 2 + i] = r[at + 9 + i] = 0;
            if (st.lift[i] > 0) continue;
            double[] g = stance(i, st, sole, full, out.get(LEGS[i]), st.aim, st.tphi[i] + x[3 + i]);
            double kw = g[0] - g[2], ku = g[1] - g[3], bend = g[5] - Math.atan2(ku, kw) - st.standK[i];
            r[at + i] = W_REACH * (Math.hypot(kw, ku) - g[4]);
            r[at + 2 + i] = W_KNEE * Math.max(0, -bend);
            r[at + 9 + i] = W_BEND * bend;
        }
        r[at + 4] = W_DIP * (x[0] - dipW);
        r[at + 5] = W_SIT * x[1];
        r[at + 6] = W_ROLL * x[2];
        r[at + 7] = W_ROCK * x[3];
        r[at + 8] = W_ROCK * x[4];
        double c = 0;
        for (int q = 0; q < 11; q++) c += r[at + q] * r[at + q];
        return c;
    }

    /**
     * Solves leg i under root so its foot rests on the edge (heel or toe) that keeps it at or above its contact plane: both
     * edges are solved, the current one kept while valid. Writes {thigh rx, thigh rz, shin rx, shin ry, shin rz, thigh ry}
     * into sol; with record, keeps the edge, bend and pitch for the next frame.
     */
    static void rock(int i, MotionEngine.Frame f, MotionEngine.State st, double[] root, double[] leg, double[] shin, double[] sol, boolean record) {
        double[] h = st.aim, c = st.con[i];
        hip(i, root, leg, h);
        boolean stand = !(f.gaitW > 0) && !st.swinging[i];
        int mode = st.swinging[i] ? 1 : i == st.driver ? 2 : stand ? 3 : 0;
        double ref = mode == 2 ? st.kapD : mode == 3 ? st.tphi[i] : st.kap[i];
        Rock hr = st.rocks[0], tr = st.rocks[1];
        edge(0, f.sole, c, st.conPsi[i], st.lift[i], h, mode, ref, hr);
        edge(1, f.sole, c, st.conPsi[i], st.lift[i], h, mode, ref, tr);
        int p = !stand ? pick(st.pivot[i], hr, tr, 0)
                : st.standSide[i] > 0 ? (tr.phi >= -FLAT_SLACK ? 1 : pick(st.pivot[i], hr, tr, st.tphi[i]))
                : hr.phi <= FLAT_SLACK ? 0 : pick(st.pivot[i], hr, tr, st.tphi[i]);
        Rock r = p == 0 ? hr : tr;
        chain(st.orient, root, leg, shin);
        angles(r, st.conPsi[i], heading(st.orient), root, leg, shin, h, sol, st.vec, st.orient);
        if (record) {
            st.pivot[i] = p;
            st.kap[i] = r.kap;
            st.phi[i] = r.phi;
        }
    }

    /**
     * The edge a foot rests on: the one its authored pitch rest (deg) rests on while valid, if that is clearly pitched; else
     * the current one while its pitch is valid (back to the heel once flat), else the valid one, else the flatter.
     */
    static int pick(int cur, Rock heel, Rock toe, double rest) {
        boolean hv = heel.phi <= FLAT_SLACK, tv = toe.phi >= -FLAT_SLACK;
        if (Math.abs(rest) > REST_PICK && (rest > 0 ? tv : hv)) return rest > 0 ? 1 : 0;
        if (cur == 1 && Math.abs(heel.phi) <= FLAT_SLACK && Math.abs(toe.phi) <= FLAT_SLACK) return 0;
        if (cur == 0 ? hv : tv) return cur;
        if (cur == 0 ? tv : hv) return 1 - cur;
        return Math.abs(heel.phi) <= Math.abs(toe.phi) ? 0 : 1;
    }

    /**
     * The planar knee for a foot at contact c (heading psi, lifted by lift) resting on edge p (0 heel, 1 toe) under the hip h:
     * the knee lies on the edge's circle (the shin is rigid to the sole) and on the thigh's circle about the hip, in the foot's
     * vertical plane. Of the two crossings, a bend under -1 degree (backward) is refused; of two forward ones a swinging leg
     * (mode 1) takes the more bent, others the one nearer ref (deg). Writes the result into r.
     */
    static void edge(int p, double[] sole, double[] c, double psi, double lift, double[] h, int mode, double ref, Rock r) {
        double sy = sole[2], ze = p == 0 ? sole[4] : sole[3];
        double ps = Math.toRadians(psi), bx = Math.sin(ps), bz = Math.cos(ps);
        r.ex = c[0] + ze * bx;
        r.ey = c[1] - lift;
        r.ez = c[2] + ze * bz;
        double dx = h[0] - r.ex, dy = h[1] - r.ey, dz = h[2] - r.ez;
        double del = MotionEngine.clamp(dx * bz - dz * bx, -LAT_MAX, LAT_MAX), rp = Math.sqrt(THIGH * THIGH - del * del);
        double w = dy, u = dx * bx + dz * bz, d = Math.hypot(w, u), re = Math.hypot(ze, sy), g = Math.atan2(ze, sy);
        r.miss = Math.max(d - rp - re, Math.abs(rp - re) - d);
        if (!(d > 1e-6)) {
            r.kw = -re * Math.cos(g);
            r.ku = -re * Math.sin(g);
            r.phi = 0;
            r.kap = 0;
            return;
        }
        double a = MotionEngine.clamp((re * re - rp * rp + d * d) / (2 * d), -re, re), hh = Math.sqrt(Math.max(0, re * re - a * a));
        double nw = w / d, nu = u / d;
        double k1w = a * nw - hh * nu, k1u = a * nu + hh * nw, k2w = a * nw + hh * nu, k2u = a * nu - hh * nw;
        double p1 = wrapRad(Math.atan2(-k1u, -k1w) - g), p2 = wrapRad(Math.atan2(-k2u, -k2w) - g);
        double c1 = wrapRad(p1 - Math.atan2(k1u - u, k1w - w)), c2 = wrapRad(p2 - Math.atan2(k2u - u, k2w - w));
        double lim = Math.toRadians(-1);
        boolean ok1 = c1 >= lim, ok2 = c2 >= lim, one;
        if (ok1 != ok2) one = ok1;
        else if (!ok1 || mode == 1) one = c1 >= c2;
        else if (mode == 3) {
            double rf = Math.toRadians(ref);
            one = Math.abs(p1 - rf) <= Math.abs(p2 - rf);
        } else {
            double rf = Math.toRadians(ref);
            one = Math.abs(c1 - rf) <= Math.abs(c2 - rf);
        }
        r.kw = one ? k1w : k2w;
        r.ku = one ? k1u : k2u;
        r.phi = Math.toDegrees(one ? p1 : p2);
        r.kap = Math.toDegrees(one ? c1 : c2);
    }

    /**
     * The leg's channels for an edge solve r: the thigh points at the knee (its channels' rotation, yaw turned by however far
     * the foot's heading is from where the channels would put it, swung the least way), and the shin takes the world rotation
     * yaw psi then pitch r.phi. Writes
     * {thigh rx, thigh rz, shin rx, shin ry, shin rz, thigh ry} into sol, each near its channel value. v and m are scratch.
     */
    static void angles(Rock r, double psi, double psiFull, double[] root, double[] leg, double[] shin, double[] h, double[] sol, double[] v, double[] m) {
        double ps = Math.toRadians(psi), bx = Math.sin(ps), bz = Math.cos(ps);
        v[0] = r.ex + r.ku * bx - h[0];
        v[1] = r.ey + r.kw - h[1];
        v[2] = r.ez + r.ku * bz - h[2];
        unrotate(v, root);
        double n = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
        double cDeg = leg[RY] + MotionMath.wrapDegrees(psi - psiFull), rxD = leg[RX], rzD = leg[RZ], ryD = cDeg;
        if (n > 1e-9) {
            // The thigh is the channels' rotation (yaw turned to the foot's heading) swung the least way to point at the knee,
            // so its twist about its own length follows the channels. Solving rx and rz for the direction at a fixed yaw
            // instead is singular with the thigh level and pointing straight ahead (a deep squat): there rz only twists the
            // thigh, and a degree of aim swings rz and the shin's ry by tens of degrees a frame.
            m[0] = 1; m[1] = 0; m[2] = 0; m[3] = 0; m[4] = 1; m[5] = 0; m[6] = 0; m[7] = 0; m[8] = 1;
            rows(m, 1, 2, leg[RX]);
            rows(m, 2, 0, cDeg);
            rows(m, 0, 1, leg[RZ]);
            double tx = v[0] / n, ty = v[1] / n, tz = v[2] / n, dx = m[1], dy = m[4], dz = m[7];
            double kx = dy * tz - dz * ty, ky = dz * tx - dx * tz, kz = dx * ty - dy * tx, co = dx * tx + dy * ty + dz * tz;
            if (co > -0.999) {
                double q = 1 / (1 + co);
                for (int j = 0; j < 3; j++) {
                    double px = m[j], py = m[3 + j], pz = m[6 + j], kp = (kx * px + ky * py + kz * pz) * q;
                    m[j] = co * px + ky * pz - kz * py + kx * kp;
                    m[3 + j] = co * py + kz * px - kx * pz + ky * kp;
                    m[6 + j] = co * pz + kx * py - ky * px + kz * kp;
                }
                // its two Euler triples, (x, y, z) and (x + 180, 180 - y, z + 180): the one nearer the channels
                double eY = Math.toDegrees(Math.asin(MotionEngine.clamp(-m[6], -1, 1)));
                double eX = Math.toDegrees(Math.atan2(m[7], m[8])), eZ = Math.toDegrees(Math.atan2(m[3], m[0])), best = Double.MAX_VALUE;
                for (int b = 0; b < 2; b++) {
                    double x = eX + 180 * b, y = b == 0 ? eY : 180 - eY, z = eZ + 180 * b;
                    x += 360 * Math.rint((leg[RX] - x) / 360);
                    y += 360 * Math.rint((cDeg - y) / 360);
                    z += 360 * Math.rint((leg[RZ] - z) / 360);
                    double dist = Math.abs(x - leg[RX]) + Math.abs(y - cDeg) + Math.abs(z - leg[RZ]);
                    if (dist < best) {
                        best = dist;
                        rxD = x;
                        ryD = y;
                        rzD = z;
                    }
                }
            }
        }
        m[0] = 1; m[1] = 0; m[2] = 0; m[3] = 0; m[4] = 1; m[5] = 0; m[6] = 0; m[7] = 0; m[8] = 1;
        rows(m, 1, 2, r.phi);
        rows(m, 2, 0, psi);
        rows(m, 0, 1, -root[RZ]);
        rows(m, 2, 0, -root[RY]);
        rows(m, 1, 2, -root[RX]);
        rows(m, 0, 1, -rzD);
        rows(m, 2, 0, -ryD);
        rows(m, 1, 2, -rxD);
        double sRy = Math.toDegrees(Math.asin(MotionEngine.clamp(-m[6], -1, 1)));
        double sRx = Math.toDegrees(Math.atan2(m[7], m[8])), sRz = Math.toDegrees(Math.atan2(m[3], m[0]));
        sol[0] = rxD;
        sol[1] = rzD;
        sol[5] = ryD;
        // the shin's rotation has two Euler triples, (x, y, z) and (x + 180, 180 - y, z + 180): the one nearer its channels
        double best = Double.MAX_VALUE;
        for (int b = 0; b < 2; b++) {
            double x = sRx + 180 * b, y = b == 0 ? sRy : 180 - sRy, z = sRz + 180 * b;
            x += 360 * Math.rint((shin[RX] - x) / 360);
            y += 360 * Math.rint((shin[RY] - y) / 360);
            z += 360 * Math.rint((shin[RZ] - z) / 360);
            double dist = Math.abs(x - shin[RX]) + Math.abs(y - shin[RY]) + Math.abs(z - shin[RZ]);
            if (dist < best) {
                best = dist;
                sol[2] = x;
                sol[3] = y;
                sol[4] = z;
            }
        }
    }

    /**
     * G3 hip pendulum: the root drop (px) that gives the stance leg driving the hips its knee aim. The driver is the one
     * anchored leg, or of two the one further through its stance; its aim eases from its own bend to the stance aim and on
     * to the lift-off aim over the stance's second half. Another anchored leg keeps at least KAP_MIN of bend. With no leg
     * anchored (a run's flight), the hips keep their height.
     */
    static double pendulum(MotionEngine.Frame f, MotionEngine.State st, double[] root, Pose out) {
        int d = st.anchored[0] && st.anchored[1] ? (f.q[0] >= f.q[1] ? 0 : 1) : st.anchored[0] ? 0 : st.anchored[1] ? 1 : -1;
        if (d != st.driver) {
            st.driver = d;
            if (d >= 0) st.kapD = st.kap[d];
        }
        double[] h = st.aim, sole = f.sole;
        hip(0, root, out.get(LEGS[0]), h);
        double mid = h[1];
        hip(1, root, out.get(LEGS[1]), h);
        mid = (mid + h[1]) / 2;
        if (d < 0) return Double.isNaN(st.hipY) ? 0 : st.hipY - mid;
        double s = MotionMath.clamp01(f.q[d] / Math.max(f.duty, 1e-6));
        double ks = kapStance(f), kl = kapLift(f);
        double aimK = s <= 0.5 ? ks : ks + (kl - ks) * MotionMath.ease((s - 0.5) / 0.5);
        st.kapD += (aimK - st.kapD) * (f.dt > 0 ? 1 - Math.exp(-f.dt / KAP_TAU) : 1);
        hip(d, root, out.get(LEGS[d]), h);
        double want = reach(d, st, sole, h, st.kapD) - h[1];
        want = MotionEngine.clamp(want, HIP_HI - h[1], HIP_LO - h[1]);
        int o = 1 - d;
        if (st.anchored[o]) {
            hip(o, root, out.get(LEGS[o]), h);
            want = Math.max(want, reach(o, st, sole, h, KAP_MIN) - h[1]);
        }
        st.hipY = mid + want;
        return want;
    }

    /** The stance knee aim (deg): more crouched under the shield, more bent backing. */
    static double kapStance(MotionEngine.Frame f) {
        double bk = MotionMath.clamp01(-f.vF / 0.5);
        return KAP_STANCE + KAP_CROUCH * f.sh * f.m + (KAP_BACK - KAP_STANCE - KAP_CROUCH * f.sh * f.m) * bk;
    }

    /** The lift-off knee aim (deg). */
    static double kapLift(MotionEngine.Frame f) {
        double bk = MotionMath.clamp01(-f.vF / 0.5);
        return KAP_LIFT + (KAP_LIFT_BACK - KAP_LIFT) * bk;
    }

    /** The hip height (px, y down) at which leg i, its hip at h's height and place, bends its knee by kap on its higher edge. */
    static double reach(int i, MotionEngine.State st, double[] sole, double[] h, double kap) {
        double[] c = st.con[i];
        double ps = Math.toRadians(st.conPsi[i]), bx = Math.sin(ps), bz = Math.cos(ps), best = Double.MAX_VALUE;
        for (int p = 0; p < 2; p++) {
            double ze = p == 0 ? sole[4] : sole[3];
            double ex = c[0] + ze * bx, ey = c[1] - st.lift[i], ez = c[2] + ze * bz;
            double del = MotionEngine.clamp((h[0] - ex) * bz - (h[2] - ez) * bx, -LAT_MAX, LAT_MAX), rp = Math.sqrt(THIGH * THIGH - del * del);
            double u = (h[0] - ex) * bx + (h[2] - ez) * bz, re = Math.hypot(ze, sole[2]), g = Math.atan2(ze, sole[2]);
            double dd = rp * rp + re * re + 2 * rp * re * Math.cos(Math.toRadians(kap) + g);
            best = Math.min(best, ey - Math.sqrt(Math.max(0, dd - u * u)));
        }
        return best;
    }

    /** G3: the moving plant's air blend (s), and a released foot's ease back to its channel (s). */
    static final double GROUND_BLEND = 0.06, T_OUT = 0.08;
    /** G3: a standing foot's settle step (s, px high), and how far its anchor may drift fore/aft, sideways (px) and in heading (deg) before one. */
    static final double SETTLE_T = 0.25, SETTLE_H = 1.8, SETTLE_FA = 3.0, SETTLE_LAT = 1.2, SETTLE_YAW = 20;
    /** G3: an anchor further than this from where it was planted against its channel lets go (px), and the ground's height in rig space (px). */
    static final double SLIP_PX = 6, FLOOR = 24;

    /** Without a ground input: each foot rests where its channels put it, never below the floor, and nothing is held. */
    static void loose(MotionEngine.State st) {
        st.plantInit = false;
        st.driver = -1;
        st.hipY = Double.NaN;
        for (int i = 0; i < 2; i++) {
            double[] ch = st.feet[i], c = st.con[i];
            st.anchored[i] = st.settling[i] = st.swinging[i] = false;
            c[0] = ch[0];
            c[1] = Math.min(ch[1], FLOOR);
            c[2] = ch[2];
            st.conPsi[i] = st.chPsi[i];
            st.lift[i] = 0;
        }
    }

    /**
     * G3: each foot's contact this frame (st.con, conPsi, lift), in rig space, given a ground input. Anchors and the points a
     * step leaves from or lands on are world-fixed: they move against the body's velocity and turn against its yaw. Striding,
     * a foot anchors where its swing lands, holds that ground point through stance and lifts off at the stance's end; the swing
     * lands it where the hip pendulum reaches at touch-down. Standing, the feet keep their anchors; one that drifts too far
     * from its channel (led by the body's motion) takes a short settle step, one foot at a time unless it is about to stray
     * too wide. A slip past SLIP_PX steps; a teleport, the air or death lets go, and the foot eases back to its channel.
     */
    static void targets(MotionEngine.Frame f, MotionEngine.State st, double[] root) {
        double now = f.now;
        if (!st.plantInit) {
            st.plantInit = true;
            st.driver = -1;
            st.hipY = Double.NaN;
            for (int i = 0; i < 2; i++) {
                double[] ch = st.feet[i];
                st.con[i][0] = ch[0];
                st.con[i][1] = FLOOR;
                st.con[i][2] = ch[2];
                st.conPsi[i] = st.chPsi[i];
                st.lift[i] = 0;
                st.anchored[i] = st.settling[i] = st.swinging[i] = false;
                st.prevQ[i] = -1;
                st.liftT[i] = -1e9;
                st.kap[i] = SOFT_KNEE;
                st.pivot[i] = 0;
                st.stepFrozen[i] = false;
                System.arraycopy(st.con[i], 0, st.stepFrom[i], 0, 3);
                st.stepPsi0[i] = st.conPsi[i];
                st.stepH0[i] = 0;
            }
        }
        double dz = f.dF * 16, dx = -f.dL * 16;
        for (int i = 0; i < 2; i++) {
            if (st.anchored[i]) {
                carry(st.con[i], dx, dz, f.dyaw);
                st.conPsi[i] -= f.dyaw;
            } else if (st.settling[i] || st.swinging[i]) {
                carry(st.stepFrom[i], dx, dz, f.dyaw);
                st.stepPsi0[i] -= f.dyaw;
                if (st.stepFrozen[i]) {
                    carry(st.stepTo[i], dx, dz, f.dyaw);
                    st.stepPsi1[i] -= f.dyaw;
                }
            }
        }
        boolean release = st.teleport || f.air || f.dead, gait = f.gaitW > 0;
        double vx = f.vL * 16, vz = -f.vF * 16, yawRate = f.dt > 0 ? f.dyaw / f.dt : 0;
        for (int i = 0; i < 2; i++) {
            if (release) {
                if (st.anchored[i] || st.settling[i] || st.swinging[i]) let(st, i, now);
                st.prevQ[i] = -1;
                st.released = true;
                continue;
            }
            if (st.released && !st.anchored[i] && !st.settling[i] && !st.swinging[i]) let(st, i, now);
            double[] ch = st.feet[i];
            boolean free = !st.anchored[i] && !st.settling[i] && !st.swinging[i], done = now - st.liftT[i] >= T_OUT;
            if (st.anchored[i] && Math.hypot(st.con[i][0] - ch[0] - st.slipRef[i][0], st.con[i][2] - ch[2] - st.slipRef[i][1]) > SLIP_PX) {
                step(st, i, now, false);
            }
            if (gait) {
                double q = f.q[i], pq = st.prevQ[i];
                boolean wrap = pq >= 0 && q < pq;
                st.prevQ[i] = q;
                if (st.swinging[i] && wrap) {
                    System.arraycopy(st.stepTo[i], 0, st.con[i], 0, 3);
                    st.con[i][1] = FLOOR;
                    st.conPsi[i] = st.stepPsi1[i];
                    anchor(st, i);
                } else if (st.settling[i] && q >= f.duty) {
                    swing(f, st, i, now);
                } else if (st.anchored[i] && q >= f.duty && !wrap) {
                    swing(f, st, i, now);
                } else if (free && done) {
                    if (q < f.duty) {
                        st.con[i][1] = FLOOR;
                        anchor(st, i);
                    } else swing(f, st, i, now);
                }
            } else {
                st.prevQ[i] = -1;
                if (st.swinging[i]) step(st, i, now, false);
                if (st.settling[i]) {
                    if (now - st.liftT[i] >= SETTLE_T) {
                        if (!st.stepFrozen[i]) settleTarget(f, st, i, vx, vz, yawRate);
                        System.arraycopy(st.stepTo[i], 0, st.con[i], 0, 3);
                        st.con[i][1] = FLOOR;
                        st.conPsi[i] = st.stepPsi1[i];
                        anchor(st, i);
                    }
                } else if (st.anchored[i]) {
                    settleTarget(f, st, i, vx, vz, yawRate);
                    double[] c = st.con[i], t = st.stepTo[i];
                    double lim = Math.max(FOOT_X, Math.abs(ch[0]) + 0.3), ahead = c[0] - vx * 0.1;
                    boolean drift = Math.abs(c[2] - t[2]) > SETTLE_FA || Math.abs(c[0] - t[0]) > SETTLE_LAT
                            || Math.abs(MotionMath.wrapDegrees(st.conPsi[i] - st.stepPsi1[i])) > SETTLE_YAW;
                    int o = 1 - i;
                    if (drift && (st.anchored[o] && !st.settling[o] || Math.abs(ahead) > lim)) step(st, i, now, true);
                } else if (free && done) {
                    st.con[i][1] = FLOOR;
                    anchor(st, i);
                }
            }
        }
        if (!release) st.released = false;

        for (int i = 0; i < 2; i++) {
            double[] ch = st.feet[i], c = st.con[i], a = st.stepFrom[i], b = st.stepTo[i];
            if (st.anchored[i]) {
                c[1] = FLOOR;
                st.lift[i] = 0;
            } else if (st.swinging[i]) {
                double sg = MotionMath.clamp01((f.q[i] - f.duty) / Math.max(1 - f.duty, 1e-6));
                double s0 = st.stepS0[i], sp = s0 < 1 ? MotionMath.clamp01((sg - s0) / (1 - s0)) : 1;
                if (!st.stepFrozen[i]) {
                    place(f, st, i, root, vx, vz, yawRate);
                    if (sp >= 0.6) st.stepFrozen[i] = true;
                }
                double e = MotionMath.ease(sp), hs = SWING_H + SWING_H_FAST * MotionMath.clamp01((f.gaitV - 1) / 5);
                for (int j = 0; j < 3; j++) c[j] = a[j] + (b[j] - a[j]) * e;
                st.conPsi[i] = st.stepPsi0[i] + MotionMath.wrapDegrees(st.stepPsi1[i] - st.stepPsi0[i]) * e;
                st.lift[i] = st.stepH0[i] * (1 - e) + hs * Math.sin(Math.PI * sp);
            } else if (st.settling[i]) {
                double s = MotionMath.clamp01((now - st.liftT[i]) / SETTLE_T);
                if (!st.stepFrozen[i]) {
                    settleTarget(f, st, i, vx, vz, yawRate);
                    if (s >= 0.8) st.stepFrozen[i] = true;
                }
                double e = MotionMath.ease((s - 0.2) / 0.6);
                for (int j = 0; j < 3; j++) c[j] = a[j] + (b[j] - a[j]) * e;
                st.conPsi[i] = st.stepPsi0[i] + MotionMath.wrapDegrees(st.stepPsi1[i] - st.stepPsi0[i]) * e;
                st.lift[i] = s < 0.2 ? st.stepH0[i] + (SETTLE_H - st.stepH0[i]) * MotionMath.ease(s / 0.2)
                        : s > 0.8 ? SETTLE_H * (1 - MotionMath.ease((s - 0.8) / 0.2)) : SETTLE_H;
            } else {
                double e = MotionMath.ease((now - st.liftT[i]) / T_OUT), ty = f.air ? ch[1] : FLOOR;
                c[0] = a[0] + (ch[0] - a[0]) * e;
                c[1] = a[1] + (ty - a[1]) * e;
                c[2] = a[2] + (ch[2] - a[2]) * e;
                st.conPsi[i] = st.stepPsi0[i] + MotionMath.wrapDegrees(st.chPsi[i] - st.stepPsi0[i]) * e;
                st.lift[i] = st.stepH0[i] * (1 - e);
            }
        }
    }

    /** Anchors leg i at its contact, and remembers where that sits against its channel (for the slip test). */
    static void anchor(MotionEngine.State st, int i) {
        st.anchored[i] = true;
        st.settling[i] = st.swinging[i] = false;
        st.lift[i] = 0;
        st.slipRef[i][0] = st.con[i][0] - st.feet[i][0];
        st.slipRef[i][1] = st.con[i][2] - st.feet[i][2];
    }

    /** Lets leg i go: it eases back to its channel from where it is. */
    static void let(MotionEngine.State st, int i, double now) {
        st.anchored[i] = st.settling[i] = st.swinging[i] = false;
        begin(st, i, now);
    }

    /** Starts a step of leg i from its contact now. */
    static void begin(MotionEngine.State st, int i, double now) {
        System.arraycopy(st.con[i], 0, st.stepFrom[i], 0, 3);
        st.stepPsi0[i] = st.conPsi[i];
        st.stepH0[i] = st.lift[i];
        st.liftT[i] = now;
        st.stepFrozen[i] = false;
    }

    /** Starts a settle step of leg i (standing, or a slip). */
    static void step(MotionEngine.State st, int i, double now, boolean unused) {
        begin(st, i, now);
        st.anchored[i] = st.swinging[i] = false;
        st.settling[i] = true;
    }

    /** Starts a gait swing of leg i from where it is, through what is left of its swing phase. */
    static void swing(MotionEngine.Frame f, MotionEngine.State st, int i, double now) {
        begin(st, i, now);
        st.anchored[i] = st.settling[i] = false;
        st.swinging[i] = true;
        st.stepS0[i] = MotionMath.clamp01((f.q[i] - f.duty) / Math.max(1 - f.duty, 1e-6));
    }

    /**
     * Where leg i's swing lands (st.stepTo, stepPsi1), in this frame's rig space: its touch-down edge (the heel going forward,
     * the toe going back) placed ahead of where the hip will be at touch-down, so the hip pendulum's height is the same at
     * touch-down (that edge, the stance aim) and lift-off (the other edge, the lift-off aim) over the stance's travel.
     */
    static void place(MotionEngine.Frame f, MotionEngine.State st, int i, double[] root, double vx, double vz, double yawRate) {
        double[] sole = f.sole, ch = st.feet[i], t = st.stepTo[i];
        double tau = (1 - f.q[i]) / Math.max(f.cad, 1e-6), v = Math.hypot(vx, vz);
        double ux = v > 1e-6 ? vx / v : 0, uz = v > 1e-6 ? vz / v : -1;
        double psi = st.chPsi[i] + yawRate * tau, ps = Math.toRadians(psi), bx = Math.sin(ps), bz = Math.cos(ps);
        boolean fwd = ux * bx + uz * bz <= 0;
        double zTd = fwd ? sole[4] : sole[3], zLo = fwd ? sole[3] : sole[4];
        double lat = MotionEngine.clamp(ch[0] - HIPS[i][0], -LAT_MAX, LAT_MAX), rp = Math.sqrt(THIGH * THIGH - lat * lat);
        double reT = Math.hypot(zTd, sole[2]), gT = Math.atan2(zTd, sole[2]), reL = Math.hypot(zLo, sole[2]), gL = Math.atan2(zLo, sole[2]);
        double dT = rp * rp + reT * reT + 2 * rp * reT * Math.cos(Math.toRadians(kapStance(f)) + gT);
        double dL = rp * rp + reL * reL + 2 * rp * reL * Math.cos(Math.toRadians(kapLift(f)) + gL);
        double s = f.travel - (sole[4] - sole[3]), a = s / 2 - (dL - dT) / (2 * Math.max(s, 8));
        double ex = ch[0] + vx * tau + ux * a, ez = HIPS[i][2] + root[TZ] + vz * tau + uz * a;
        t[0] = ex - zTd * bx;
        t[1] = FLOOR;
        t[2] = ez - zTd * bz;
        st.stepPsi1[i] = psi;
    }

    /** Where a standing settle step of leg i lands: its channel, led by the body's motion and turn over a settle (st.stepTo, stepPsi1). */
    static void settleTarget(MotionEngine.Frame f, MotionEngine.State st, int i, double vx, double vz, double yawRate) {
        double[] ch = st.feet[i], t = st.stepTo[i];
        double lim = Math.max(FOOT_X, Math.abs(ch[0]) + 0.3), y = yawRate * SETTLE_T;
        t[0] = ch[0] + vx * SETTLE_T;
        t[1] = FLOOR;
        t[2] = ch[2] + vz * SETTLE_T;
        turn(t, 0, 2, -y);
        t[0] = MotionEngine.clamp(t[0], -lim, lim);
        st.stepPsi1[i] = st.chPsi[i] + y;
    }

    /** Carries a world-fixed rig-space point through one frame of the body's motion: dx, dz px of ground travel, then a turn of dyaw degrees. */
    static void carry(double[] p, double dx, double dz, double dyaw) {
        p[0] += dx;
        p[2] += dz;
        turn(p, 0, 2, dyaw);
    }

    /** Leg i's sole point under its knee (shin point (0, y, 0)) in root space from its own channels, written into p. */
    static void foot(int i, double[] leg, double[] shin, double y, double[] p) {
        foot(i, leg, shin, y, 0, p);
    }

    /** Leg i's shin point (0, y, z) in root space from its own channels, written into p. */
    static void foot(int i, double[] leg, double[] shin, double y, double z, double[] p) {
        p[0] = 0; p[1] = y; p[2] = z;
        rotate(p, shin);
        p[0] += shin[TX]; p[1] += SHIN + shin[TY]; p[2] += shin[TZ];
        rotate(p, leg);
        p[0] += HIPS[i][0] + leg[TX]; p[1] += HIPS[i][1] + leg[TY]; p[2] += HIPS[i][2] + leg[TZ];
    }

    /** Leg i's foot (the shin's tip) in root space from its own channels, written into p. */
    static void foot(int i, double[] leg, double[] shin, double[] p) {
        foot(i, leg, shin, SHIN, p);
    }

    /** Leg i's hip in rig space under root, written into h. */
    static void hip(int i, double[] root, double[] leg, double[] h) {
        h[0] = HIPS[i][0] + leg[TX];
        h[1] = HIPS[i][1] + leg[TY];
        h[2] = HIPS[i][2] + leg[TZ];
        rotate(h, root);
        h[0] += root[TX];
        h[1] += root[TY];
        h[2] += root[TZ];
    }

    /** The shin's rotation (root, if given, after the leg after the shin's own) as a row-major 3x3 in m. */
    static void chain(double[] m, double[] root, double[] leg, double[] shin) {
        m[0] = 1; m[1] = 0; m[2] = 0; m[3] = 0; m[4] = 1; m[5] = 0; m[6] = 0; m[7] = 0; m[8] = 1;
        rows(m, 1, 2, shin[RX]);
        rows(m, 2, 0, shin[RY]);
        rows(m, 0, 1, shin[RZ]);
        rows(m, 1, 2, leg[RX]);
        rows(m, 2, 0, leg[RY]);
        rows(m, 0, 1, leg[RZ]);
        if (root == null) return;
        rows(m, 1, 2, root[RX]);
        rows(m, 2, 0, root[RY]);
        rows(m, 0, 1, root[RZ]);
    }

    /** The heading (deg, as a bone's yaw) a rotation turns the x axis to. */
    static double heading(double[] m) {
        return Math.toDegrees(Math.atan2(-m[6], m[0]));
    }

    static double wrapRad(double a) {
        return a - 2 * Math.PI * Math.rint(a / (2 * Math.PI));
    }

    /**
     * Solves leg i so its foot reaches target (entity space) under the given root, writing {thigh rx, thigh rz, knee rx}
     * into sol. The thigh's yaw is kept. A target on the hip leaves the leg as it is.
     */
    static void solve(int i, double[] target, double[] root, double[] leg, double[] shin, double[] sol, double[] v) {
        sol[0] = leg[RX]; sol[1] = leg[RZ]; sol[2] = shin[RX];
        v[0] = target[0] - root[TX]; v[1] = target[1] - root[TY]; v[2] = target[2] - root[TZ];
        unrotate(v, root);
        double vx = v[0] - (HIPS[i][0] + leg[TX]), vy = v[1] - (HIPS[i][1] + leg[TY]), vz = v[2] - (HIPS[i][2] + leg[TZ]);
        double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (!(d > 1e-6)) return;
        double knee = 2 * Math.acos(Math.min(d, LEG) / LEG);
        double c = Math.toRadians(leg[RY]), cc = Math.cos(c);
        double beta = Math.asin(MotionEngine.clamp(vz / d / (Math.abs(cc) < 1e-6 ? 1e-6 : cc), -1, 1));
        double a = Math.atan2(vy / d, vx / d) - Math.atan2(Math.cos(beta), Math.sin(beta) * Math.sin(c));
        double rz = Math.toDegrees(a);
        rz += 360 * Math.rint((leg[RZ] - rz) / 360);
        sol[0] = Math.toDegrees(beta - knee / 2);
        sol[1] = rz;
        sol[2] = Math.toDegrees(knee);
    }

    /** Left-multiplies the row-major 3x3 m by the turn in the (a, b) plane (as {@link #turn}). */
    static void rows(double[] m, int a, int b, double deg) {
        if (deg == 0) return;
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r);
        for (int j = 0; j < 3; j++) {
            double qa = m[3 * a + j], qb = m[3 * b + j];
            m[3 * a + j] = qa * c - qb * s;
            m[3 * b + j] = qa * s + qb * c;
        }
    }

    /**
     * As {@link #solve}, but for the shin point (0, y, z0) instead of the tip: a two-bone solve with a thigh of LEG - SHIN
     * and an effective shin of |(y, z0)|, then the shin's own pitch backed out of the effective segment's angle. With z0 = 0
     * and y = SHIN it gives what solve gives. sol holds the pose solved before (the branch nearer its knee is kept) and gets
     * the result.
     */
    static void solveAt(int i, double[] target, double z0, double y, double[] root, double[] leg, double[] sol, double[] v) {
        v[0] = target[0] - root[TX]; v[1] = target[1] - root[TY]; v[2] = target[2] - root[TZ];
        unrotate(v, root);
        double vx = v[0] - (HIPS[i][0] + leg[TX]), vy = v[1] - (HIPS[i][1] + leg[TY]), vz = v[2] - (HIPS[i][2] + leg[TZ]);
        double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (!(d > 1e-6)) return;
        double a = LEG - SHIN, b = Math.sqrt(y * y + z0 * z0);
        double dc = MotionEngine.clamp(d, Math.abs(a - b) + 1e-6, a + b - 1e-6);
        double th1 = Math.acos(MotionEngine.clamp((a * a + dc * dc - b * b) / (2 * a * dc), -1, 1));
        double th2 = Math.acos(MotionEngine.clamp((b * b + dc * dc - a * a) / (2 * b * dc), -1, 1));
        double c = Math.toRadians(leg[RY]), cc = Math.cos(c);
        double beta = Math.asin(MotionEngine.clamp(vz / d / (Math.abs(cc) < 1e-6 ? 1e-6 : cc), -1, 1));
        double ang = Math.atan2(vy / d, vx / d) - Math.atan2(Math.cos(beta), Math.sin(beta) * Math.sin(c));
        double rz = Math.toDegrees(ang);
        rz += 360 * Math.rint((leg[RZ] - rz) / 360);
        double g = Math.atan2(z0, y), k0 = Math.toRadians(sol[2]);
        double kp = th1 + th2 - g, km = -th1 - th2 - g, dp = Math.abs(kp - k0), dm = Math.abs(km - k0);
        double e = g < 0 && dm < dp ? -1 : 1, w = g < 0 ? MotionMath.clamp01(Math.abs(dp - dm) / Math.toRadians(BRANCH_TIE)) : 1;
        double thigh = beta - e * th1;
        sol[0] += (Math.toDegrees(thigh) - sol[0]) * w;
        sol[1] += (rz - sol[1]) * w;
        sol[2] += (Math.toDegrees(e * (th1 + th2) - g) - sol[2]) * w;
    }

    /** Rotates p in place by a bone's rotation channels: about x, then y, then z (ModelPart order), degrees. */
    static void rotate(double[] p, double[] ch) {
        turn(p, 1, 2, ch[RX]);
        turn(p, 2, 0, ch[RY]);
        turn(p, 0, 1, ch[RZ]);
    }

    /** Undoes {@link #rotate} in place. */
    static void unrotate(double[] p, double[] ch) {
        turn(p, 0, 1, -ch[RZ]);
        turn(p, 2, 0, -ch[RY]);
        turn(p, 1, 2, -ch[RX]);
    }

    static void turn(double[] q, int a, int b, double deg) {
        if (deg == 0) return;
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r), qa = q[a], qb = q[b];
        q[a] = qa * c - qb * s;
        q[b] = qa * s + qb * c;
    }
}
