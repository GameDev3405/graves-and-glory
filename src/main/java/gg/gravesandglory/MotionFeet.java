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
    static final String[] LEGS = {"right_leg", "left_leg"}, SHINS = {"right_shin", "left_shin"}, FEET = {"right_foot", "left_foot"};
    /** The ankle's pivot below the knee (px, the shin's frame): the foot bone turns the boot about it. */
    static final double ANKLE = 4;
    /** A bone's channels at rest: a foot or chest the pose has no row for (read only). */
    private static final double[] NONE = new double[6];

    /**
     * Plants the feet while standing. Each foot's target is where the legs' own channels put it with the root at rest. A
     * knee buckle from a legs hit bends each knee by its own amount: the hips roll toward the deeper buckle so both feet
     * still reach, then the root dips just enough for both legs to keep a soft knee. Each leg is solved analytically (two
     * bones, knee hinge on x, thigh yaw kept) so the foot lands on its target. Turning in place moves each target by
     * State.turnOff (MotionBody.turnStep). Moving or a clip that keys the legs hands the feet back, eased by the same weights; the buckle then bends the knees directly. Death hands them
     * back too, and a revive eases them in again over PLANT_BLEND.
     *
     * <p>Records in State how far the corrected channels sit from the uncorrected ones (plantRot, plantPx), so a fade that
     * changes how much the clips own the legs can count the hand-off in its pop budget.
     */
    static void plant(MotionEngine.Frame f, MotionEngine.State st, Pose out) {
        stride(f, st, out);
        double base = (1 - f.m) * f.plant, pw = base * (1 - Math.min(1, f.bodyAction));
        double[] k = st.knee;
        k[0] = Math.max(0, st.react[MotionBody.KNEE_R]) * f.lw;
        k[1] = Math.max(0, st.react[MotionBody.KNEE_L]) * f.lw;
        double[] root = out.get("root");
        for (int i = 0; i < 2; i++) {
            double[] t = st.feet[i], o = st.turnOff[i];
            foot(i, out.get(LEGS[i]), out.get(SHINS[i]), t);
            t[0] += o[0]; t[1] -= o[1]; t[2] += o[2];
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

    /** Round 4: the time constant (s) over which the stride's hip dip lets go. */
    static final double DIP_RELEASE = 0.08;

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
        if (!(gwAll > 0)) { st.dipHeld = 0; st.footLive = false; return; }
        if (st.waist) { strideAnkle(f, st, out, gwAll); return; }
        // round 4: the dip may deepen at once (a foot must reach) but lets go over DIP_RELEASE, so a trailing foot that lifts off
        // does not drop its hip demand in one frame and snap the stance knee straight
        double held = st.dipHeld * Math.exp(-f.dt / DIP_RELEASE);
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
                t[1] = GROUND - lift * f.lift[i] - drop[i] * Math.min(1, DROP_GAIN * f.lift[i]) * f.liftCap;
                t[0] += pv[i][0]; t[2] += pv[i][1];
                v[0] = HIPS[i][0] + leg[TX]; v[1] = HIPS[i][1] + leg[TY]; v[2] = HIPS[i][2] + leg[TZ];
                rotate(v, full);
                double dx = t[0] - v[0] - full[TX], dy = t[1] - v[1] - full[TY], dz = t[2] - v[2] - full[TZ];
                dip = Math.max(dip, st.fw[i] * (dy - Math.sqrt(Math.max(0, soft * soft - dx * dx - dz * dz))));
            }
            dip = Math.max(MotionEngine.clamp(dip, 0, 6), held);
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
                int at = lowestAt(i, full, l, k, footOf(out, i), v);
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
        st.dipHeld = dip;
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

    /** Anim redo gait (2026-11): the heel's and the toe's ground points in the shin's frame (px): the back and front edges of the boot's floor (HULL). */
    static final double[] HEEL = {0, 6, 2}, TOE = {0, 6, -4};
    /**
     * Anim redo gait: the sole's pitch (deg, toe down) at the heel strike, walk to run (negative: toe up), which eases flat by
     * FLAT_AT of the stance; the share of the stance at which the heel rises (walk to run) and the toe-off pitch it rolls to.
     */
    static final double STRIKE_WALK = -14, STRIKE_RUN = -6, FLAT_AT = 0.2, HEEL_OFF_WALK = 0.6, HEEL_OFF_RUN = 0.45, TOE_WALK = 28, TOE_RUN = 35;
    /**
     * Anim redo gait: in the swing the foot hangs HANG (walk to run) deg toe down off its shin by HANG_U of the swing, then from
     * LAND_U turns to the strike's pitch; the shin tip hands its toe-off offset back over KS_U of the swing. A foot turns at most
     * FOOT_MAX deg on its ankle (TroopMotion.limits allows 45) and turns back at most FOOT_YAW deg of the hips' twist.
     */
    static final double HANG_WALK = 8, HANG_RUN = 12, HANG_U = 0.35, LAND_U = 0.5, KS_U = 0.4, FOOT_MAX = 40, FOOT_YAW = 15;
    /**
     * Anim redo gait: the roll follows the travel's forward share (a strafe's feet stay flat); a backpedal runs it backwards (it
     * lands on the toe, rolls flat and rocks onto the heel to lift) at BACK_ROLL of the forward pitch.
     */
    static final double BACK_ROLL = 0.6;
    /**
     * Anim redo gait: the roll's full speed (b/s) and its ease (s): slower, the roll shrinks toward a flat-footed step, so through a
     * stop the standing feet set flat and the stop's own steps land flat instead of holding a strike or a toe-off.
     */
    static final double ROLL_V = 1.2, ROLL_T = 0.12;

    /** Anim redo gait: the thigh (hip to knee) the ankle's leg solve works with; the shin's knee-to-ankle part is ANKLE (px). */
    static final double THIGH = LEG - SHIN;

    /**
     * Anim redo gait (2026-11): stride for a biped with the waist and ankle joints, in the Fresh Animations style. Each foot rolls on
     * its ankle: it lands on its heel toe-up, rolls flat, raises its heel onto its toe and leaves the ground toe down, then hangs off
     * the shin in the swing and turns to the strike's pitch to land; a backpedal runs the roll backwards, a strafe keeps the sole
     * flat. The foot turns back the hips' twist, so it points along the travel. The ground point the foot stands on (its heel,
     * then its toe) stays where it landed: the ankle's target is that point less the point's offset from the ankle under the
     * foot's pitch, so the leg (solved for the ankle, not the shin's tip) carries no offset of its own back into its target and
     * the sole does not slide however the shin turns over it. The foot's pitch on the ankle is what the shin leaves to reach the
     * wanted pitch, within FOOT_MAX; the passes settle the two from last frame's legs. The swing lifts the lower of heel and toe
     * LIFT_MIN to LIFT_RUN. The hips drop as far as the ankles need to reach, as in stride. Replaces stride's corner roll on
     * these rigs.
     */
    static void strideAnkle(MotionEngine.Frame f, MotionEngine.State st, Pose out, double gwAll) {
        double held = st.dipHeld * Math.exp(-f.dt / DIP_RELEASE);
        double[] root = out.get("root"), full = st.rootFull, v = st.vec, ft = st.footT, oh = st.offH, ot = st.offT, l = st.legT, k = st.shinT;
        double run = f.run, dip = 0;
        double kr = Math.toRadians(STRIDE_KNEE), soft = Math.sqrt(THIGH * THIGH + ANKLE * ANKLE + 2 * THIGH * ANKLE * Math.cos(kr));
        double liftPx = MotionBody.LIFT_MIN + (MotionBody.LIFT_RUN - MotionBody.LIFT_MIN) * run;
        double strike = STRIKE_WALK + (STRIKE_RUN - STRIKE_WALK) * run, heelOff = HEEL_OFF_WALK + (HEEL_OFF_RUN - HEEL_OFF_WALK) * run;
        double toeOff = TOE_WALK + (TOE_RUN - TOE_WALK) * run, hang = HANG_WALK + (HANG_RUN - HANG_WALK) * run;
        double yaw = MotionEngine.clamp(-f.hipYaw, -FOOT_YAW, FOOT_YAW);
        boolean live = st.footLive, back = f.travelF < 0;
        double rollGoal = MotionMath.clamp01(f.gaitV / ROLL_V);
        st.rollW = live ? MotionEngine.toward(st.rollW, rollGoal, f.dt / ROLL_T) : rollGoal;
        double amp = Math.min(1, Math.abs(f.travelF)) * (back ? BACK_ROLL : 1) * smooth(st.rollW), land = amp * (back ? toeOff : strike);
        for (int pass = 0; pass <= CORNER_PASSES; pass++) {
            System.arraycopy(root, 0, full, 0, 6);
            dip = 0;
            for (int i = 0; i < 2; i++) {
                double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), t = st.feet[i], clip = footOf(out, i), kn = st.kNow[i];
                foot(i, leg, shin, t);
                // the legs as last solved (this frame's previous pass, or last frame's for the first)
                legsAt(leg, shin, pass > 0 ? st.sol[i] : live ? st.footSol[i] : null, l, k);
                double p0 = pitch(clip, k, l, full, v), q = f.q[i];
                boolean locked = st.locked[i], stand = q < f.duty || locked, was = live && st.footStance[i];
                double x = q / Math.max(1e-6, f.duty), u = stand ? 0 : (q - f.duty) / Math.max(1e-6, 1 - f.duty);
                if (back) x = 1 - x;
                double rx = footPitch(st, i, stand, locked, was, live, x, u, p0, amp, strike, heelOff, toeOff, hang, land, clip);
                System.arraycopy(clip, 0, ft, 0, 6);
                ft[RX] += rx;
                ft[RY] += yaw;
                offset(HEEL, ft, k, l, full, oh);
                offset(TOE, ft, k, l, full, ot);
                double low = Math.max(oh[1], ot[1]);
                // a foot sets down on its lower end; the pivot then passes once per stance (heel to toe; toe to heel on a backpedal)
                // and does not pass back when the stance share runs backwards (a stop's duty grows over a standing foot)
                int raw = x < heelOff ? 0 : 1, landOn = oh[1] >= ot[1] ? 0 : 1;
                int piv = !was ? (locked ? landOn : back ? Math.min(raw, landOn) : Math.max(raw, landOn)) : locked ? st.footPivot[i]
                    : back ? Math.min(raw, st.footPivot[i]) : Math.max(raw, st.footPivot[i]);
                if (stand) {
                    double[] o = piv == 0 ? oh : ot;
                    // a landing: the ankle sets down over the sweep's point, less what is left of the swing's carried offset (none on a
                    // full swing; all of it on a foot that sets down again straight after leaving)
                    if (!was) { kn[0] = o[0] + (live ? st.footKf[i][0] : 0); kn[1] = o[2] + (live ? st.footKf[i][1] : 0); }
                    else if (piv != st.footPivot[i]) { double[] o0 = piv == 0 ? ot : oh; kn[0] = st.footK[i][0] - o0[0] + o[0]; kn[1] = st.footK[i][1] - o0[2] + o[2]; }
                    else { kn[0] = st.footK[i][0]; kn[1] = st.footK[i][1]; }
                    t[0] += kn[0] - o[0]; t[2] += kn[1] - o[2];
                    t[1] = GROUND - low;
                } else {
                    if (was) { double[] o0 = st.footPivot[i] == 0 ? oh : ot; kn[0] = st.footK[i][0] - o0[0]; kn[1] = st.footK[i][1] - o0[2]; }
                    else if (live) { kn[0] = st.footKs[i][0]; kn[1] = st.footKs[i][1]; }
                    else kn[0] = kn[1] = 0;
                    double fade = 1 - smooth(u / KS_U);
                    t[0] += kn[0] * fade; t[2] += kn[1] * fade;
                    t[1] = GROUND - liftPx * f.lift[i] - low;
                }
                st.pivNow[i] = piv;
                v[0] = HIPS[i][0] + leg[TX]; v[1] = HIPS[i][1] + leg[TY]; v[2] = HIPS[i][2] + leg[TZ];
                rotate(v, full);
                double dx = t[0] - v[0] - full[TX], dy = t[1] - v[1] - full[TY], dz = t[2] - v[2] - full[TZ];
                dip = Math.max(dip, st.fw[i] * (dy - Math.sqrt(Math.max(0, soft * soft - dx * dx - dz * dz))));
            }
            dip = Math.max(MotionEngine.clamp(dip, 0, 6), held);
            full[TY] += dip;
            for (int i = 0; i < 2; i++) solveAnkle(i, st.feet[i], full, out.get(LEGS[i]), out.get(SHINS[i]), st.sol[i], v);
        }
        // the ankle's pitch under the legs as finally solved
        for (int i = 0; i < 2; i++) {
            double[] clip = footOf(out, i);
            legsAt(out.get(LEGS[i]), out.get(SHINS[i]), st.sol[i], l, k);
            double p0 = pitch(clip, k, l, full, v), q = f.q[i];
            boolean locked = st.locked[i], stand = q < f.duty || locked, was = live && st.footStance[i];
            double x = q / Math.max(1e-6, f.duty), u = stand ? 0 : (q - f.duty) / Math.max(1e-6, 1 - f.duty);
            if (back) x = 1 - x;
            st.footRxNow[i] = clampFoot(footPitch(st, i, stand, locked, was, live, x, u, p0, amp, strike, heelOff, toeOff, hang, land, clip), clip);
        }
        for (int i = 0; i < 2; i++) {
            boolean stand = f.q[i] < f.duty || st.locked[i], was = live && st.footStance[i];
            if (stand) { st.footK[i][0] = st.kNow[i][0]; st.footK[i][1] = st.kNow[i][1]; }
            else {
                double fade = 1 - smooth((f.q[i] - f.duty) / Math.max(1e-6, 1 - f.duty) / KS_U);
                st.footKs[i][0] = st.kNow[i][0]; st.footKs[i][1] = st.kNow[i][1];
                st.footKf[i][0] = st.kNow[i][0] * fade; st.footKf[i][1] = st.kNow[i][1] * fade;
                if (was) st.footRx0[i] = st.footRx[i]; else if (!live) st.footRx0[i] = 0;
            }
            st.footStance[i] = stand;
            st.footPivot[i] = st.pivNow[i];
            st.footRx[i] = st.footRxNow[i];
            System.arraycopy(st.sol[i], 0, st.footSol[i], 0, 3);
        }
        st.footLive = true;
        st.dipHeld = dip;
        root[TY] += gwAll * dip;
        for (int i = 0; i < 2; i++) {
            double[] leg = out.get(LEGS[i]), shin = out.get(SHINS[i]), sol = st.sol[i], fo = out.get(FEET[i]);
            double gw = gwAll * st.fw[i];
            leg[RX] += (sol[0] - leg[RX]) * gw;
            leg[RZ] += (sol[1] - leg[RZ]) * gw;
            shin[RX] += (sol[2] - shin[RX]) * gw;
            shin[RY] -= shin[RY] * gw;
            shin[RZ] -= shin[RZ] * gw;
            for (int c = TX; c <= TZ; c++) shin[c] -= shin[c] * gw;
            fo[RX] += gw * st.footRx[i];
            fo[RY] += gw * yaw;
        }
    }

    /**
     * Anim redo gait: the foot's pitch on its ankle (deg, toe down, on top of the clip's) for a sole pitch p0 under the legs. In
     * stance the sole takes the roll's world pitch at x (the stance share, run backwards on a backpedal): the strike's toe-up
     * easing flat by FLAT_AT, then from the heel's rise to the toe-off. In the swing it hangs toward HANG off the shin by
     * HANG_U, then from LAND_U turns to the landing's world pitch (flat on a stop's last step). Not clamped: the ankle's target
     * takes the ground points under the wanted pitch, so a leg folded past what FOOT_MAX can turn back is not held folded by
     * the foot it drew (a stop's spread feet); clampFoot bounds the pitch drawn.
     */
    static double footPitch(MotionEngine.State st, int i, boolean stand, boolean locked, boolean was, boolean live, double x, double u,
                            double p0, double amp, double strike, double heelOff, double toeOff, double hang, double land, double[] clip) {
        double rx;
        if (stand) {
            double want = locked ? 0 : amp * (x < FLAT_AT ? strike * sq(1 - x / FLAT_AT) : x < heelOff ? 0 : toeOff * sq((x - heelOff) / (1 - heelOff)));
            rx = want - p0;
        } else {
            double rx0 = was ? st.footRx[i] : live ? st.footRx0[i] : 0;
            double hung = rx0 + (hang - rx0) * smooth(u / HANG_U);
            rx = hung + ((st.settleSwing[i] ? 0 : land) - p0 - hung) * smooth((u - LAND_U) / (1 - LAND_U));
        }
        return rx;
    }

    /** Anim redo gait: a foot pitch on top of clip's, within FOOT_MAX on the ankle. */
    static double clampFoot(double rx, double[] clip) { return MotionEngine.clamp(rx, -FOOT_MAX - clip[RX], FOOT_MAX - clip[RX]); }

    /** Anim redo gait: the leg and shin channels for a solve sol ({thigh rx, thigh rz, knee}; null: the channels as they are) into l and k. */
    static void legsAt(double[] leg, double[] shin, double[] sol, double[] l, double[] k) {
        System.arraycopy(leg, 0, l, 0, 6);
        Arrays.fill(k, 0);
        if (sol != null) { l[RX] = sol[0]; l[RZ] = sol[1]; k[RX] = sol[2]; } else k[RX] = shin[RX];
    }

    /** Anim redo gait: the sole's world pitch (deg, toe down; past 90 when the toe points backwards) under foot, shin, leg and root rotations. */
    static double pitch(double[] foot, double[] shin, double[] leg, double[] root, double[] v) {
        v[0] = 0; v[1] = 0; v[2] = -1;
        rotate(v, foot); rotate(v, shin); rotate(v, leg); rotate(v, root);
        return Math.toDegrees(Math.atan2(v[1], Math.copySign(Math.hypot(v[0], v[2]), -v[2])));
    }

    /** Anim redo gait: point c of the shin's frame (a foot point, turned about the ankle by foot) less the ankle, carried through shin, leg and root rotations into o. */
    static void offset(double[] c, double[] foot, double[] shin, double[] leg, double[] root, double[] o) {
        o[0] = c[0]; o[1] = c[1] - ANKLE; o[2] = c[2];
        rotate(o, foot);
        o[0] += foot[TX]; o[1] += foot[TY]; o[2] += foot[TZ];
        rotate(o, shin); rotate(o, leg); rotate(o, root);
    }

    /**
     * Anim redo gait: as solve, for leg i's ankle (THIGH, then ANKLE down the shin) at target: the knee bends to the distance
     * and the thigh turns back from the target's line by the angle the bent shin opens.
     */
    static void solveAnkle(int i, double[] target, double[] root, double[] leg, double[] shin, double[] sol, double[] v) {
        sol[0] = leg[RX]; sol[1] = leg[RZ]; sol[2] = shin[RX];
        v[0] = target[0] - root[TX]; v[1] = target[1] - root[TY]; v[2] = target[2] - root[TZ];
        unrotate(v, root);
        double vx = v[0] - (HIPS[i][0] + leg[TX]), vy = v[1] - (HIPS[i][1] + leg[TY]), vz = v[2] - (HIPS[i][2] + leg[TZ]);
        double d = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (!(d > 1e-6)) return;
        double r = MotionEngine.clamp(d, THIGH - ANKLE + 1e-6, THIGH + ANKLE);
        double knee = Math.acos(MotionEngine.clamp((r * r - THIGH * THIGH - ANKLE * ANKLE) / (2 * THIGH * ANKLE), -1, 1));
        double c = Math.toRadians(leg[RY]), cc = Math.cos(c);
        double beta = Math.asin(MotionEngine.clamp(vz / d / (Math.abs(cc) < 1e-6 ? 1e-6 : cc), -1, 1));
        double a = Math.atan2(vy / d, vx / d) - Math.atan2(Math.cos(beta), Math.sin(beta) * Math.sin(c));
        double rz = Math.toDegrees(a);
        rz += 360 * Math.rint((leg[RZ] - rz) / 360);
        sol[0] = Math.toDegrees(beta - Math.atan2(ANKLE * Math.sin(knee), THIGH + ANKLE * Math.cos(knee)));
        sol[1] = rz;
        sol[2] = Math.toDegrees(knee);
    }

    static double sq(double x) { return x * x; }

    /** Smoothstep of x clamped to [0, 1]. */
    static double smooth(double x) { x = MotionEngine.clamp(x, 0, 1); return x * x * (3 - 2 * x); }

    /** Fix r3: the extra stride solves that move each foot's target by its lowest corner's drop below the sole's middle. */
    static final int CORNER_PASSES = 2;
    /** Fix r3: how fast a lifting foot takes its corner's full drop, against its lift weight (the clearance is the corner's sooner). */
    static final double DROP_GAIN = 2.5;

    /**
     * Fix r3 (Task 16): the drawn lower leg's corners in the shin's frame (px, knee pivot at the origin, y down): the shin box below
     * the ankle, the boot and the toe cap, which ride the foot (before FOOT_TO), then the knee guard and the shin box above the
     * ankle (the troop rig's knight; the other troops' boots sit inside it). What the ground pass and the stride's corner targets
     * keep on the ground. The waist and ankle rig (2026-11) split the shin at ANKLE; at rest the corners are the whole shin's.
     */
    static final double[][] HULL = hull(new double[][]{
        {-2, 2, 4, 6, -2, 2}, {-2.5, 2.5, 3, 6, -3, 2}, {-1.5, 1.5, 5, 6, -4, -3}, {-2.5, 2.5, -1, 1, -3, -1}, {-2, 2, 0, 4, -2, 2}});
    /** HULL's corners before this index ride the foot: they turn about the ankle by its channels. */
    static final int FOOT_TO = 24;

    static double[][] hull(double[][] boxes) {
        double[][] h = new double[boxes.length * 8][];
        for (int b = 0; b < boxes.length; b++) for (int c = 0; c < 8; c++)
            h[b * 8 + c] = new double[]{boxes[b][(c & 1)], boxes[b][2 + ((c >> 1) & 1)], boxes[b][4 + ((c >> 2) & 1)]};
        return h;
    }

    /** Leg i's lowest drawn corner (rig y, px, larger is lower) under the given root, leg, shin and foot channels; v is scratch. */
    static double lowest(int i, double[] root, double[] leg, double[] shin, double[] foot, double[] v) {
        double lo = Double.NEGATIVE_INFINITY;
        for (int c = 0; c < HULL.length; c++) {
            corner(c, foot, v);
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
    static int lowestAt(int i, double[] root, double[] leg, double[] shin, double[] foot, double[] v) {
        double lo = Double.NEGATIVE_INFINITY, x = 0, y = 0, z = 0; int at = -1;
        for (int c = 0; c < HULL.length; c++) {
            corner(c, foot, v);
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

    /** HULL's corner c in the shin's frame, into v: a foot corner turned about the ankle by the foot's channels (unmoved at rest). */
    static void corner(int c, double[] foot, double[] v) {
        double[] p = HULL[c];
        v[0] = p[0]; v[1] = p[1]; v[2] = p[2];
        if (c >= FOOT_TO) return;
        v[1] -= ANKLE;
        rotate(v, foot);
        v[0] += foot[TX]; v[1] += ANKLE + foot[TY]; v[2] += foot[TZ];
    }

    /** Leg i's foot channels in the pose, or NONE: read without making a row. */
    static double[] footOf(Pose out, int i) {
        double[] f = out.find(FEET[i]);
        return f == null ? NONE : f;
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

    /** ground at weight w (none without the ground input: the legs are then the unsolved limb-angle gait). */
    static void ground(Pose out, double[] v, double w) { ground(out, v, w, 0); }

    /** ground at weight w, its lift held under the torso's clearance at weight lie (a settled corpse: MotionEngine.pose). */
    static void ground(Pose out, double[] v, double w, double lie) {
        if (!(w > 0)) return;
        double[] root = out.get("root");
        double a = lowest(0, root, out.get(LEGS[0]), out.get(SHINS[0]), footOf(out, 0), v) - GROUND;
        double b = lowest(1, root, out.get(LEGS[1]), out.get(SHINS[1]), footOf(out, 1), v) - GROUND;
        double d = a - b, s = (a + b + Math.sqrt(d * d + SINK_EASE * SINK_EASE)) / 2 - SINK_TOL;
        double k = SINK_EASE, lift = s <= -k ? 0 : s >= k ? s : (s + k) * (s + k) / (4 * k);
        if (!(lift > 0)) return;
        // a settled corpse is not lifted off its torso: it lies in its authored pose, its boots wherever the clip put them
        if (lie > 0) lift += lie * (softMin(lift, Math.max(0, torsoClear(out, v) - SINK_TOL), SINK_EASE) - lift);
        if (lift > 0) root[TY] -= w * lift;
    }

    /**
     * The torso's two halves and the head box (the body pivots at the hips, the chest at the waist WAIST above them, the head at
     * the neck NECK above the waist), as {x0, y0, z0, x1, y1, z1}.
     */
    private static final double[] BODY_BOX = {-4, -6, -2, 4, 0, 2}, CHEST_BOX = {-4, -6, -2, 4, 0, 2}, HEAD_BOX = {-4, -8, -4, 4, 0, 4};
    static final double WAIST = 6, NECK = 6;

    /** How high (px) the torso's and head's lowest corner is over the ground (root translation included). */
    static double torsoClear(Pose out, double[] v) {
        double[] root = out.get("root"), body = out.get("body"), head = out.get("head"), chest = out.find("chest");
        if (chest == null) chest = NONE;
        double lo = Double.NEGATIVE_INFINITY;
        for (int h = 0; h < 3; h++) {
            double[] b = h == 0 ? BODY_BOX : h == 1 ? CHEST_BOX : HEAD_BOX;
            for (int c = 0; c < 8; c++) {
                v[0] = b[(c & 1) * 3]; v[1] = b[1 + (c >> 1 & 1) * 3]; v[2] = b[2 + (c >> 2 & 1) * 3];
                if (h == 2) { rotate(v, head); v[0] += head[TX]; v[1] += head[TY] - NECK; v[2] += head[TZ]; }
                if (h >= 1) { rotate(v, chest); v[0] += chest[TX]; v[1] += chest[TY] - WAIST; v[2] += chest[TZ]; }
                rotate(v, body);
                v[0] += body[TX]; v[1] += 12 + body[TY]; v[2] += body[TZ];
                rotate(v, root);
                lo = Math.max(lo, v[1] + root[TY]);
            }
        }
        return GROUND - lo;
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
