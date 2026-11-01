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
     * The knight's sole in shin space (px): {xMin, xMax, y, zToe, zHeel}. Families whose model is not posed yet, and the
     * standalone checks, use it.
     */
    static final double[] KNIGHT_SOLE = {-2.5, 2.5, 6, -3, 2};
    /** The lowest sole corner may sit this far (px) below the tip's ground before the corner pass re-solves on it. */
    static final double CORNER_SLACK = 0.02;
    /** Sit-back (G4, R102): hip shift per radian of solved-over-authored shin tilt (px), and its limit (px). */
    static final double SIT_GAIN = 12, SIT_MAX = 3;
    static final int SIT_PASSES = 3;
    /** G4: how much nearer (deg) one corner branch must be than the other to be taken in full. */
    static final double BRANCH_TIE = 40;

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
        double act = 1 - Math.min(1, f.bodyAction);
        double base = (1 - f.m) * (1 - f.tw) * f.plant, pw = base * act;
        // G3: the moving plant's weight, eased out in the air and back in on landing
        boolean grounded = f.ground && !f.air && !f.dead;
        st.groundW = f.first ? (grounded ? 1 : 0) : MotionMath.clamp01(st.groundW + (grounded ? 1 : -1) * f.dt / GROUND_BLEND);
        double wm = f.ground && !f.dead ? f.m * (1 - f.tw) * act * st.groundW * f.plant : 0;
        double[] k = st.knee;
        k[0] = Math.max(0, st.react[MotionBody.KNEE_R]) * f.lw;
        k[1] = Math.max(0, st.react[MotionBody.KNEE_L]) * f.lw;
        double[] root = out.get("root");
        for (int i = 0; i < 2; i++) foot(i, out.get(LEGS[i]), out.get(SHINS[i]), st.feet[i]);
        if (f.ground) targets(f, st, root);
        else {
            st.plantInit = false;
            for (int i = 0; i < 2; i++) {
                System.arraycopy(st.feet[i], 0, st.tgt[i], 0, 3);
                st.anchored[i] = st.settling[i] = false;
            }
        }

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
        // G4 sit-back: shift the hips fore/aft so the solved shins tilt as the channels authored them (R102). Standing only. It is
        // solved before the dip, so a hit's knee bend and dip tilt the shins as they should and do not move the hips.
        double[] v = st.vec;
        double sit = 0, lastSit = 0, lastErr = 0;
        for (int pass = 0; pass < SIT_PASSES; pass++) {
            double err = 0;
            for (int i = 0; i < 2; i++) {
                double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
                solve(i, st.tgt[i], full, leg, shin, sol, v);
                err += (sol[0] + sol[2]) - (leg[RX] + shin[RX]);
            }
            // A secant step after the first (the shins' tilt is not linear in the shift once a leg straightens).
            double step = pass == 0 || Math.abs(err - lastErr) < 1e-6 ? SIT_GAIN * Math.toRadians(err / 2) : -err * (sit - lastSit) / (err - lastErr);
            double next = MotionEngine.clamp(sit + step, -SIT_MAX, SIT_MAX);
            lastSit = sit; lastErr = err;
            full[TZ] += next - sit;
            sit = next;
        }
        double dip = 0;
        for (int i = 0; i < 2; i++) {
            double[] leg = out.get(LEGS[i]), t = st.tgt[i];
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

        double rot = Math.abs(roll), px = Math.max(Math.max(dip, Math.abs(sit)), Math.max(Math.abs(rollX), rollY));
        for (int i = 0; i < 2; i++) {
            double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
            solve(i, st.tgt[i], full, leg, shin, sol, v);
            corner(i, f.sole, st.tgt[i], full, leg, sol, v, st.orient, st.aim);
            rot = Math.max(rot, Math.max(Math.abs(sol[0] - leg[RX]), Math.max(Math.abs(sol[1] - leg[RZ]), Math.abs(sol[2] - shin[RX]))));
            rot = Math.max(rot, Math.max(Math.abs(shin[RY]), Math.abs(shin[RZ])));
            for (int c = TX; c <= TZ; c++) px = Math.max(px, Math.abs(shin[c]));
        }
        st.plantRot = base * rot;
        st.plantPx = base * px;

        // A moving leg (G3) is solved under the root the frame actually gets: the standing roll, dip and sit-back scaled by how
        // much of the leg's weight is the standing plant's (all of it when standing, none of it when walking).
        double[] lr = st.legRoot;
        for (int i = 0; i < 2; i++) {
            double pwi = Math.max(pw, wm);
            st.plantW[i] = pwi;
            if (!(pwi > pw)) continue;
            double share = pw / pwi;
            for (int c = 0; c < 6; c++) lr[c] = root[c] + (full[c] - root[c]) * share;
            double[] leg = out.get(LEGS[i]), sol = st.sol[i];
            solve(i, st.tgt[i], lr, leg, out.get(SHINS[i]), sol, v);
            corner(i, f.sole, st.tgt[i], lr, leg, sol, v, st.orient, st.aim);
        }

        if (pw > 0) {
            double pr = Math.toRadians(pw * roll);
            root[RZ] += pw * roll;
            root[TX] += LEG * Math.sin(pr);
            root[TY] += LEG * (1 - Math.cos(pr)) + pw * dip;
            root[TZ] += pw * sit;
        }
        for (int i = 0; i < 2; i++) {
            double w = st.plantW[i];
            if (!(w > 0)) continue;
            double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i];
            leg[RX] += (sol[0] - leg[RX]) * w;
            leg[RZ] += (sol[1] - leg[RZ]) * w;
            shin[RX] += (sol[2] - shin[RX]) * w;
            shin[RY] -= shin[RY] * w;
            shin[RZ] -= shin[RZ] * w;
            for (int c = TX; c <= TZ; c++) shin[c] -= shin[c] * w;
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

    /** G3: the moving plant's air blend (s), the swing's blend out of its lift-off point (s), and the swing lift slow to fast (px). */
    static final double GROUND_BLEND = 0.06, T_OUT = 0.08, LIFT_SLOW = 1.6, LIFT_FAST = 4.0;
    /** G3: a standing foot's settle step (s, px high), and how far its anchor may drift fore/aft and sideways before one (px). */
    static final double SETTLE_T = 0.25, SETTLE_H = 1.2, SETTLE_FA = 3.0, SETTLE_LAT = 1.2;
    /** G3: an anchor further than this from its channel target lets go (px), and the ground's height in rig space (px). */
    static final double SLIP_PX = 6, FLOOR = 24;

    /**
     * G3: each foot's target this frame, in entity space (st.tgt), given a ground input. Anchors and the points the feet blend
     * from are world-fixed: they move against the body's velocity and turn against its yaw. While striding, a foot plants where
     * its channel puts it at touch-down, holds that ground point through stance (as low as the leg reaches, never below the
     * floor) and lets go at lift-off; in swing it blends from where it lifted to its channel and clears the floor by a lift that
     * grows with speed. Standing, the feet keep their anchors; one that drifts too far from its channel takes a short settle
     * step, one foot at a time. A slip past SLIP_PX, a teleport, the air or death lets go.
     */
    static void targets(MotionEngine.Frame f, MotionEngine.State st, double[] root) {
        if (!st.plantInit) {
            st.plantInit = true;
            for (int i = 0; i < 2; i++) {
                System.arraycopy(st.feet[i], 0, st.lastTgt[i], 0, 3);
                st.anchored[i] = st.settling[i] = false;
                st.prevQ[i] = -1;
                st.liftT[i] = -1e9;
                System.arraycopy(st.feet[i], 0, st.liftFrom[i], 0, 3);
            }
        }
        {
            double dz = f.dF * 16, dx = -f.dL * 16;
            for (int i = 0; i < 2; i++) {
                carry(st.anchor[i], dx, dz, f.dyaw);
                carry(st.liftFrom[i], dx, dz, f.dyaw);
                carry(st.lastTgt[i], dx, dz, f.dyaw);
            }
        }
        boolean release = st.teleport || f.air || f.dead, gait = f.gaitW > 0;
        double now = f.now;
        for (int i = 0; i < 2; i++) {
            double[] ch = st.feet[i], t = st.tgt[i], an = st.anchor[i], lf = st.liftFrom[i];
            if ((st.anchored[i] || st.settling[i]) && (release || st.anchored[i] && Math.hypot(an[0] - ch[0], an[2] - ch[2]) > SLIP_PX)) {
                st.anchored[i] = st.settling[i] = false;
                st.liftT[i] = now;
                System.arraycopy(st.lastTgt[i], 0, lf, 0, 3);
            }
            double q = f.q[i], pq = st.prevQ[i];
            boolean swing = false;
            if (gait && !release) {
                if (st.settling[i]) {
                    st.settling[i] = false;
                    st.liftT[i] = now;
                    System.arraycopy(st.lastTgt[i], 0, lf, 0, 3);
                }
                if (pq >= 0 && q < pq) {
                    System.arraycopy(ch, 0, an, 0, 3);
                    st.anchored[i] = true;
                } else if (pq >= 0 && pq < f.duty && q >= f.duty && st.anchored[i]) {
                    st.anchored[i] = false;
                    st.liftT[i] = now;
                    System.arraycopy(an, 0, lf, 0, 3);
                }
                st.prevQ[i] = q;
                swing = q >= f.duty;
            } else {
                st.prevQ[i] = -1;
                if (!release) {
                    int o = 1 - i;
                    if (st.settling[i]) {
                        if (now - st.liftT[i] >= SETTLE_T) {
                            st.settling[i] = false;
                            st.anchored[i] = true;
                            System.arraycopy(ch, 0, an, 0, 3);
                        }
                    } else if (st.anchored[i]) {
                        if ((Math.abs(an[2] - ch[2]) > SETTLE_FA || Math.abs(an[0] - ch[0]) > SETTLE_LAT) && st.anchored[o] && !st.settling[o]) settle(st, i, an, now);
                    } else if (Math.hypot(st.lastTgt[i][0] - ch[0], st.lastTgt[i][2] - ch[2]) < 0.05) {
                        st.anchored[i] = true;
                        System.arraycopy(ch, 0, an, 0, 3);
                    } else if (!st.settling[o]) settle(st, i, st.lastTgt[i], now);
                }
            }
            if (st.anchored[i]) {
                t[0] = an[0];
                t[2] = an[2];
                // standing, the height is the channel's; striding, as low as the leg reaches over the floor
                double hx = an[0] - (HIPS[i][0] + root[TX]), hz = an[2] - (HIPS[i][2] + root[TZ]), r = LEG * Math.cos(Math.toRadians(SOFT_KNEE / 2));
                double floor = Math.min(FLOOR, HIPS[i][1] + root[TY] + Math.sqrt(Math.max(0, r * r - hx * hx - hz * hz)));
                t[1] = ch[1] + (floor - ch[1]) * f.m;
            } else if (st.settling[i]) {
                double s = MotionMath.clamp01((now - st.liftT[i]) / SETTLE_T), e = MotionMath.ease(s);
                t[0] = lf[0] + (ch[0] - lf[0]) * e;
                t[2] = lf[2] + (ch[2] - lf[2]) * e;
                t[1] = Math.min(ch[1], FLOOR - SETTLE_H * Math.sin(Math.PI * s));
            } else {
                double e = MotionMath.ease((now - st.liftT[i]) / T_OUT);
                t[0] = lf[0] + (ch[0] - lf[0]) * e;
                t[2] = lf[2] + (ch[2] - lf[2]) * e;
                t[1] = ch[1];
                if (swing) {
                    double h = LIFT_SLOW + (LIFT_FAST - LIFT_SLOW) * MotionMath.clamp01((f.gaitV - 1) / 5);
                    t[1] = Math.min(ch[1], FLOOR - h * Math.sin(Math.PI * (q - f.duty) / (1 - f.duty)));
                }
            }
            System.arraycopy(t, 0, st.lastTgt[i], 0, 3);
        }
    }

    /** Starts leg i's settle step from the point from (G3). */
    static void settle(MotionEngine.State st, int i, double[] from, double now) {
        if (from != st.liftFrom[i]) System.arraycopy(from, 0, st.liftFrom[i], 0, 3);
        st.anchored[i] = false;
        st.settling[i] = true;
        st.liftT[i] = now;
    }

    /** Carries a world-fixed rig-space point through one frame of the body's motion: dx, dz px of ground travel, then a turn of dyaw degrees. */
    static void carry(double[] p, double dx, double dz, double dyaw) {
        p[0] += dx;
        p[2] += dz;
        turn(p, 0, 2, dyaw);
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

    /**
     * G4 corner pass: with leg i solved for its tip, finds the lowest sole corner under the solved shin. If it sits more than
     * CORNER_SLACK below the tip's ground, re-solves the leg once with the effector at that corner's (0, y, z0), its target
     * the tip target plus the effector's offset and its height lowered by the corner's roll, so the corner, not the tip,
     * lands on the ground. A corner that cannot reach leaves the leg straight and the corner above the ground. m and t are
     * scratch (9 and 3 doubles).
     */
    static void corner(int i, double[] sole, double[] target, double[] root, double[] leg, double[] sol, double[] v, double[] m, double[] t) {
        if (sole == null || !CORNER_ON) return;
        orient(m, root, leg, sol);
        double sy = sole[2] - SHIN, best = CORNER_SLACK, x0 = 0, z0 = 0;
        boolean found = false;
        for (int cx = 0; cx < 2; cx++) for (int cz = 3; cz < 5; cz++) {
            double y = m[3] * sole[cx] + m[4] * sy + m[5] * sole[cz];
            if (y > best) { best = y; x0 = sole[cx]; z0 = sole[cz]; found = true; }
        }
        if (!found) return;
        t[0] = target[0] + m[1] * sy + m[2] * z0;
        t[1] = target[1] - m[3] * x0;
        t[2] = target[2] + m[7] * sy + m[8] * z0;
        solveAt(i, t, z0, sole[2], root, leg, sol, v);
    }
    /** G4 corner pass switch. Off: the toe-corner solve is singular when the effective chain is near straight (a hit-bent knee
     * over a toe-down sole), where a 0.4 px lift needs a 28 deg knee change either way; the pops and hit-zone checks catch it.
     * See task-16-fix-r1b-report.md. */
    static final boolean CORNER_ON = false;

    /**
     * The solved shin's rotation in entity space, row-major into m: root (x, y, z) after the leg (its solved pitch plus the
     * knee, its kept yaw, its solved roll). The shin's own yaw and roll are planted to zero. Uses six sin/cos pairs.
     */
    static void orient(double[] m, double[] root, double[] leg, double[] sol) {
        m[0] = 1; m[1] = 0; m[2] = 0; m[3] = 0; m[4] = 1; m[5] = 0; m[6] = 0; m[7] = 0; m[8] = 1;
        rows(m, 1, 2, sol[0] + sol[2]);
        rows(m, 2, 0, leg[RY]);
        rows(m, 0, 1, sol[1]);
        rows(m, 1, 2, root[RX]);
        rows(m, 2, 0, root[RY]);
        rows(m, 0, 1, root[RZ]);
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
     * As {@link #solve}, but for the shin point (0, y, z0) instead of the tip (G4): a two-bone solve with a thigh of
     * LEG - SHIN and an effective shin of |(y, z0)|, then the shin's own pitch backed out of the effective segment's angle.
     * With z0 = 0 and y = SHIN it gives what solve gives. sol holds the pose solved before (the branch nearer its knee is kept) and
     * gets the result.
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
        // The effective chain (thigh, knee-to-point) can bend either way under a toe corner. Take the branch whose knee lies
        // nearer the pose solved before, and only as far as it is clearly nearer: near the tie (the effective chain straight,
        // where a small lift needs a large bend either way) the pose solved before stands, so the leg never flips.
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
