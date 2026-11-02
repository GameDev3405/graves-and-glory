package gg.gravesandglory;

import java.util.Arrays;

import static gg.gravesandglory.MotionEngine.RX;
import static gg.gravesandglory.MotionEngine.RY;
import static gg.gravesandglory.MotionEngine.RZ;
import static gg.gravesandglory.MotionEngine.TX;
import static gg.gravesandglory.MotionEngine.TY;
import static gg.gravesandglory.MotionEngine.TZ;

/**
 * Planted feet for {@link MotionEngine} (animation rework design section 6.1): two-bone leg solves that keep a standing
 * unit's feet where its leg channels put them while the root moves. Rig space is y down and -z forward; the root pivot is
 * at the origin, the hips 12 px below it. Uses only the scratch in {@link MotionEngine.State}, so it does not allocate. Pure.
 */
final class MotionFeet {
    private MotionFeet() {}

    /** Hip-to-foot length of a straight leg and the shin's length (px), and the least knee bend a planted leg keeps (degrees). */
    static final double LEG = 12, SHIN = 6, SOFT_KNEE = 12;
    /** Hip spacing (px) and the largest roll (degrees) the hips take to let one knee buckle more than the other. */
    static final double HIP_WIDTH = 3.8, MAX_ROLL = 30;
    static final double[][] HIPS = {{-1.9, 12, 0}, {1.9, 12, 0}};
    static final String[] LEGS = {"right_leg", "left_leg"}, SHINS = {"right_shin", "left_shin"};

    /**
     * Plants the feet while standing. Each foot's target is where the legs' own channels put it with the root at rest. A
     * knee buckle from a legs hit bends each knee by its own amount: the hips roll toward the deeper buckle so both feet
     * still reach, then the root dips just enough for both legs to keep a soft knee. Each leg is solved analytically (two
     * bones, knee hinge on x, thigh yaw kept) so the foot lands on its target. Moving, turning in place or a clip that keys
     * the legs hands the feet back, eased by the same weights; the buckle then bends the knees directly. Death hands them
     * back too, and a revive eases them in again over PLANT_BLEND.
     *
     * <p>Records in State how far the corrected channels sit from the uncorrected ones (plantRot, plantPx), so a fade that
     * changes how much the clips own the legs can count the hand-off in its pop budget.
     */
    static void plant(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        stride(f, st, out);
        double base = (1 - f.m) * (1 - f.tw) * f.plant, pw = base * (1 - Math.min(1, f.bodyAction));
        double[] k = st.knee;
        k[0] = Math.max(0, st.react[MotionBody.KNEE_R]) * f.lw;
        k[1] = Math.max(0, st.react[MotionBody.KNEE_L]) * f.lw;
        double[] root = out.get("root");
        for (int i = 0; i < 2; i++) foot(i, out.get(LEGS[i]), out.get(SHINS[i]), st.feet[i]);

        double soft = Math.cos(Math.toRadians(SOFT_KNEE / 2));
        double eR = LEG * (soft - Math.cos(Math.toRadians(Math.min(SOFT_KNEE + k[0], 150) / 2)));
        double eL = LEG * (soft - Math.cos(Math.toRadians(Math.min(SOFT_KNEE + k[1], 150) / 2)));
        double roll = MotionEngine.clamp(-Math.toDegrees(Math.asin(MotionEngine.clamp((eR - eL) / HIP_WIDTH, -1, 1))), -MAX_ROLL, MAX_ROLL);
        double rr = Math.toRadians(roll), rollX = LEG * Math.sin(rr), rollY = LEG * (1 - Math.cos(rr));

        double[] full = st.rootFull;
        System.arraycopy(root, 0, full, 0, 6);
        full[RZ] += roll;
        full[TX] += rollX;
        full[TY] += rollY;
        double dip = 0;
        double[] v = st.vec;
        for (int i = 0; i < 2; i++) {
            double[] leg = out.get(LEGS[i]), t = st.feet[i];
            v[0] = HIPS[i][0] + leg[TX]; v[1] = HIPS[i][1] + leg[TY]; v[2] = HIPS[i][2] + leg[TZ];
            rotate(v, full);
            double dx = t[0] - v[0] - full[TX], dy = t[1] - v[1] - full[TY], dz = t[2] - v[2] - full[TZ];
            double d0 = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double rest = 2 * Math.toDegrees(Math.acos(softMin(d0, LEG * soft, REACH_EASE) / LEG));
            double bend = Math.min(rest + k[i], 150), want = LEG * Math.cos(Math.toRadians(bend / 2));
            dip = Math.max(dip, dy - Math.sqrt(Math.max(0, want * want - dx * dx - dz * dz)));
        }
        dip = MotionEngine.clamp(dip, 0, 9);
        full[TY] += dip;

        double rot = Math.abs(roll), px = Math.max(dip, Math.max(Math.abs(rollX), rollY));
        for (int i = 0; i < 2; i++) {
            double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
            solve(i, st.feet[i], full, leg, shin, sol, v);
            rot = Math.max(rot, Math.max(Math.abs(sol[0] - leg[RX]), Math.max(Math.abs(sol[1] - leg[RZ]), Math.abs(sol[2] - shin[RX]))));
            rot = Math.max(rot, Math.max(Math.abs(shin[RY]), Math.abs(shin[RZ])));
            for (int c = TX; c <= TZ; c++) px = Math.max(px, Math.abs(shin[c]));
        }
        st.plantRot = base * rot;
        st.plantPx = base * px;

        if (pw > 0) {
            double pr = Math.toRadians(pw * roll);
            root[RZ] += pw * roll;
            root[TX] += LEG * Math.sin(pr);
            root[TY] += LEG * (1 - Math.cos(pr)) + pw * dip;
            for (int i = 0; i < 2; i++) {
                double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
                leg[RX] += (sol[0] - leg[RX]) * pw;
                leg[RZ] += (sol[1] - leg[RZ]) * pw;
                shin[RX] += (sol[2] - shin[RX]) * pw;
                shin[RY] -= shin[RY] * pw;
                shin[RZ] -= shin[RZ] * pw;
                for (int c = TX; c <= TZ; c++) shin[c] -= shin[c] * pw;
            }
        }
        double freeR = (1 - pw) * k[0], freeL = (1 - pw) * k[1];
        if (freeR > 0 || freeL > 0) {
            out.add("right_shin", RX, freeR);
            out.add("right_leg", RX, -freeR / 2);
            out.add("left_shin", RX, freeL);
            out.add("left_leg", RX, -freeL / 2);
            out.add("root", TY, LEG * (1 - Math.cos(Math.toRadians((freeR + freeL) / 4))));
        }
    }

    /** The ground under a standing rig (px, y down) and the knee bend a striding stance leg keeps at the least (degrees). */
    static final double GROUND = 24, STRIDE_KNEE = 8;

    /**
     * G1 (R109): stands a striding unit's stance foot on the ground and lifts its swing foot. Each foot's target is where the
     * legs' own channels put it with the root at rest, so the root's bob, sway and twist do not carry the feet; MotionBody's
     * stride makes that sweep travel back at the body's speed in stance. A stance foot's height is the ground; a swing foot
     * rises on a sine from the ground and back, LIFT_MIN high at a walk and LIFT_RUN at a run. The hips drop as far as the feet need to reach
     * them with a soft knee (the walk's vault comes from this), then each leg is solved analytically as in plant. Eased by the
     * gait weight; a death, a turn in place or a clip that keys the legs hands the feet back. No world anchors: a standing unit's
     * feet are plant's.
     */
    static void stride(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        double gwAll = f.gaitW * f.plant * (1 - f.tw) * (1 - Math.min(1, f.bodyAction));
        if (!(gwAll > 0)) return;
        double[] root = out.get("root"), full = st.rootFull, v = st.vec, drop = st.drop;
        double soft = LEG * Math.cos(Math.toRadians(STRIDE_KNEE / 2)), dip = 0, lift = MotionBody.LIFT_MIN + (MotionBody.LIFT_RUN - MotionBody.LIFT_MIN) * f.run;
        drop[0] = drop[1] = 0;
        // fix r3 (gate 2): a stance foot rolls on its lowest corner rather than its sole's middle. Each stance frame the target moves
        // by the corner's shift against the middle (pc), so the corner it stands on stays put while it stays the lowest; when the
        // contact passes to another corner (heel to toe) the middle carries on. A swing hands the shift back over its first half.
        double[][] pv = st.pivotT;
        for (int i = 0; i < 2; i++) {
            double u = f.q[i] < f.duty ? -1 : (f.q[i] - f.duty) / Math.max(1e-6, 1 - f.duty);
            // a stop's last step and the foot it sets down do not roll: that foot stands on its idle pose's sole
            boolean still = st.settleSwing[i] && (st.locked[i] || u > ROLL_U + ROLL_BACK);
            if (still) { st.pc[i][0] = st.pc[i][1] = st.pcl[i][0] = st.pcl[i][1] = 0; }
            boolean roll = !still && (u < ROLL_U || u > 1 - ROLL_LAND);
            if (roll) { pv[i][0] = st.pc[i][0]; pv[i][1] = st.pc[i][1]; }
            else {
                if (st.pStance[i]) { st.pcl[i][0] = st.pc[i][0]; st.pcl[i][1] = st.pc[i][1]; st.pc[i][0] = st.pc[i][1] = 0; }
                double x = MotionEngine.clamp((u - ROLL_U) / ROLL_BACK, 0, 1), w = 1 - x * x * (3 - 2 * x);
                pv[i][0] = st.pcl[i][0] * w; pv[i][1] = st.pcl[i][1] * w;
            }
            st.pNow[i] = roll;
        }
        // fix r3: solved for the sole's middle first, then again (CORNER_PASSES) for its lowest drawn corner, so a stance foot
        // stands on the corner it pitches onto and a swing foot's lift is that corner's clearance, not the middle's
        for (int pass = 0; pass <= CORNER_PASSES; pass++) {
            System.arraycopy(root, 0, full, 0, 6);
            dip = 0;
            for (int i = 0; i < 2; i++) {
                double[] leg = out.get(LEGS[i]), t = st.feet[i];
                foot(i, leg, out.get(SHINS[i]), t);
                t[1] = GROUND - lift * f.lift[i] - drop[i] * Math.min(1, DROP_GAIN * f.lift[i]);
                t[0] += pv[i][0]; t[2] += pv[i][1];
                v[0] = HIPS[i][0] + leg[TX]; v[1] = HIPS[i][1] + leg[TY]; v[2] = HIPS[i][2] + leg[TZ];
                rotate(v, full);
                double dx = t[0] - v[0] - full[TX], dy = t[1] - v[1] - full[TY], dz = t[2] - v[2] - full[TZ];
                dip = Math.max(dip, st.fw[i] * (dy - Math.sqrt(Math.max(0, soft * soft - dx * dx - dz * dz))));
            }
            dip = MotionEngine.clamp(dip, 0, 6);
            full[TY] += dip;
            for (int i = 0; i < 2; i++) {
                double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
                solve(i, st.feet[i], full, leg, shin, sol, v);
                if (pass == CORNER_PASSES) continue;
                double[] l = st.legT, k = st.shinT;
                System.arraycopy(leg, 0, l, 0, 6);
                l[RX] = sol[0]; l[RZ] = sol[1];
                Arrays.fill(k, 0);
                k[RX] = sol[2];
                v[0] = 0; v[1] = SHIN; v[2] = 0;
                rotate(v, k);
                v[1] += SHIN;
                rotate(v, l);
                v[0] += HIPS[i][0] + l[TX]; v[1] += HIPS[i][1] + l[TY]; v[2] += HIPS[i][2] + l[TZ];
                rotate(v, full);
                double mid = v[1] + full[TY];
                double mx = v[0] + full[TX], mz = v[2] + full[TZ];
                int at = lowestAt(i, full, l, k, v);
                drop[i] = f.lift[i] > 0 ? Math.max(0, v[1] - mid) : 0;
                if (!st.pNow[i]) continue;
                double px = v[0] - mx, pz = v[2] - mz;
                if (st.pStance[i] && at == st.pIdx[i]) {
                    pv[i][0] = MotionEngine.clamp(st.pc[i][0] - ROLL_GAIN * (px - st.pp[i][0]), -PIVOT_MAX, PIVOT_MAX);
                    pv[i][1] = MotionEngine.clamp(st.pc[i][1] - ROLL_GAIN * (pz - st.pp[i][1]), -PIVOT_MAX, PIVOT_MAX);
                }
                st.pNext[i][0] = px; st.pNext[i][1] = pz; st.pNextIdx[i] = at;
            }
        }
        for (int i = 0; i < 2; i++) {
            if (st.pNow[i]) {
                st.pc[i][0] = pv[i][0]; st.pc[i][1] = pv[i][1];
                st.pp[i][0] = st.pNext[i][0]; st.pp[i][1] = st.pNext[i][1]; st.pIdx[i] = st.pNextIdx[i];
            }
            st.pStance[i] = st.pNow[i];
        }
        root[TY] += gwAll * dip;
        for (int i = 0; i < 2; i++) {
            double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
            double gw = gwAll * st.fw[i];
            leg[RX] += (sol[0] - leg[RX]) * gw;
            leg[RZ] += (sol[1] - leg[RZ]) * gw;
            shin[RX] += (sol[2] - shin[RX]) * gw;
            shin[RY] -= shin[RY] * gw;
            shin[RZ] -= shin[RZ] * gw;
            for (int c = TX; c <= TZ; c++) shin[c] -= shin[c] * gw;
        }
    }

    /** Fix r3: the extra stride solves that move each foot's target by its lowest corner's drop below the sole's middle. */
    static final int CORNER_PASSES = 2;
    /** Fix r3: how fast a lifting foot takes its corner's full drop, against its lift weight (the clearance is the corner's sooner). */
    static final double DROP_GAIN = 2.5;

    /**
     * Fix r3 (Task 16): the drawn shin's corners in its own frame (px, knee pivot at the origin, y down): the shin box, the boot,
     * the toe cap and the knee guard of the troop rig (TroopModel's knight; the other troops' boots sit inside it). What the ground
     * pass and the stride's corner targets keep on the ground.
     */
    static final double[][] HULL = hull(new double[][]{
        {-2, 2, 0, 6, -2, 2}, {-2.5, 2.5, 3, 6, -3, 2}, {-1.5, 1.5, 5, 6, -4, -3}, {-2.5, 2.5, -1, 1, -3, -1}});

    static double[][] hull(double[][] boxes) {
        double[][] h = new double[boxes.length * 8][];
        for (int b = 0; b < boxes.length; b++) for (int c = 0; c < 8; c++)
            h[b * 8 + c] = new double[]{boxes[b][(c & 1)], boxes[b][2 + ((c >> 1) & 1)], boxes[b][4 + ((c >> 2) & 1)]};
        return h;
    }

    /** Leg i's lowest drawn corner (rig y, px, larger is lower) under the given root, leg and shin channels; v is scratch. */
    static double lowest(int i, double[] root, double[] leg, double[] shin, double[] v) {
        double lo = Double.NEGATIVE_INFINITY;
        for (double[] p : HULL) {
            v[0] = p[0]; v[1] = p[1]; v[2] = p[2];
            rotate(v, shin);
            v[0] += shin[TX]; v[1] += SHIN + shin[TY]; v[2] += shin[TZ];
            rotate(v, leg);
            v[0] += HIPS[i][0] + leg[TX]; v[1] += HIPS[i][1] + leg[TY]; v[2] += HIPS[i][2] + leg[TZ];
            rotate(v, root);
            lo = Math.max(lo, v[1] + root[TY]);
        }
        return lo;
    }

    /** Fix r3: the most (px) a stance foot's roll may shift its target off the stride's. */
    static final double PIVOT_MAX = 6;
    static final double ROLL_GAIN = 0.5;
    /** Fix r3: the swing progress until which a lifting foot still rolls on its corner (toe-off), the span it hands the roll back over, and the last part of the swing in which a landing foot rolls onto its corner. */
    static final double ROLL_U = 0.25, ROLL_BACK = 0.35, ROLL_LAND = 0.2;

    /** Leg i's lowest drawn corner's index in HULL, leaving that corner (rig space, root translation included) in v. */
    static int lowestAt(int i, double[] root, double[] leg, double[] shin, double[] v) {
        double lo = Double.NEGATIVE_INFINITY, x = 0, y = 0, z = 0; int at = -1;
        for (int c = 0; c < HULL.length; c++) {
            double[] p = HULL[c];
            v[0] = p[0]; v[1] = p[1]; v[2] = p[2];
            rotate(v, shin);
            v[0] += shin[TX]; v[1] += SHIN + shin[TY]; v[2] += shin[TZ];
            rotate(v, leg);
            v[0] += HIPS[i][0] + leg[TX]; v[1] += HIPS[i][1] + leg[TY]; v[2] += HIPS[i][2] + leg[TZ];
            rotate(v, root);
            // a near tie (within a hundredth of a pixel) keeps the earlier corner, so two corners of one edge do not flicker
            if (v[1] + root[TY] > lo + 1e-2) { lo = v[1] + root[TY]; x = v[0] + root[TX]; y = lo; z = v[2] + root[TZ]; at = c; }
        }
        v[0] = x; v[1] = y; v[2] = z;
        return at;
    }

    /** Fix r3: how far (px) a drawn sole may sink before the ground pass lifts the hips, and the width (px) its lift eases in over. */
    static final double SINK_TOL = 0.25, SINK_EASE = 0.2;

    /**
     * Fix r3 (Task 16 gate 1): grounds the drawn soles. Where a shin's lowest corner (HULL) reaches more than SINK_TOL under the
     * ground, the hips rise until it does not: a pure lift of the root, so every bone keeps its angles and the knees their bend.
     * The lift eases in over SINK_EASE (a parabola) so the root does not kink, and takes the deeper foot by a smooth maximum
     * (never under the deeper one). Runs last on the legs (MotionEngine.pose after plant; TroopMotion after a scene clip and on
     * a forced clip).
     */
    static void ground(Pose out, double[] v) { ground(out, v, 1); }

    /** ground at weight w (a death hands the body over to its clip's lying pose, which rests on its torso, not its soles). */
    static void ground(Pose out, double[] v, double w) {
        if (!(w > 0)) return;
        double[] root = out.get("root");
        double a = lowest(0, root, out.get(LEGS[0]), out.get(SHINS[0]), v) - GROUND;
        double b = lowest(1, root, out.get(LEGS[1]), out.get(SHINS[1]), v) - GROUND;
        double d = a - b, s = (a + b + Math.sqrt(d * d + SINK_EASE * SINK_EASE)) / 2 - SINK_TOL;
        double k = SINK_EASE, lift = s <= -k ? 0 : s >= k ? s : (s + k) * (s + k) / (4 * k);
        if (lift > 0) root[TY] -= w * lift;
    }

    /** Fix 2 (Task 16): the width (px) over which a standing leg's reach eases onto its soft-knee limit instead of meeting it with a kink. */
    static final double REACH_EASE = 0.6;

    /**
     * min(x, cap) with the corner rounded over cap +- k by a parabola, so a reach running onto the soft-knee limit does not turn
     * the knee and the hips' dip on a single frame (a second-difference spike at a stance change). Equal to min(x, cap) outside.
     */
    static double softMin(double x, double cap, double k) {
        double u = x - cap + k;
        return u <= 0 ? x : u >= 2 * k ? cap : x - u * u / (4 * k);
    }

    /** Leg i's foot (the shin's tip) in root space from its own channels, written into p. */
    static void foot(int i, double[] leg, double[] shin, double[] p) {
        p[0] = 0; p[1] = SHIN; p[2] = 0;
        rotate(p, shin);
        p[0] += shin[TX]; p[1] += SHIN + shin[TY]; p[2] += shin[TZ];
        rotate(p, leg);
        p[0] += HIPS[i][0] + leg[TX]; p[1] += HIPS[i][1] + leg[TY]; p[2] += HIPS[i][2] + leg[TZ];
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
