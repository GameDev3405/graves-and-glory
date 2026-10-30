package gg.gravesandglory;

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
            double rest = 2 * Math.toDegrees(Math.acos(Math.min(d0, LEG * soft) / LEG));
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
