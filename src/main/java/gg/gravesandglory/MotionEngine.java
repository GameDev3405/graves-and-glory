package gg.gravesandglory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The layered troop motion engine (animation rework design section 6.1). Each frame, every bone's six channels are the sum
 * of six layers, applied in order: locomotion, stance, life, action, look and secondary. The feet are planted after look,
 * from the summed pose. Stances and actions are clips ({@link MotionStack}); the other layers are procedural
 * ({@link MotionBody}, {@link MotionFeet}). Every state weight ramps toward its target at its own rate on render time and is
 * shaped by {@link MotionMath#ease}.
 *
 * <p>Frame-rate independence: every integrator treats the frame as the interval (last, now]. Weights ramp toward the
 * target they had last frame, a layer or track pushed this frame starts moving next frame, springs chase a target that
 * moves linearly across the frame, and phases integrate their rate with the trapezoid rule. Input that changes at a shared
 * sample time therefore gives the same pose at 20, 60 and 240 fps. Steady-state frames do not allocate. Pure.
 */
public final class MotionEngine {
    private MotionEngine() {}

    static final int RX = 0, RY = 1, RZ = 2, TX = 3, TY = 4, TZ = 5;
    /** Seconds each state weight takes to ramp fully on or off. */
    static final double STANCE_BLEND = 0.45, MOVE_BLEND = 0.3, RUN_BLEND = 0.5, COMBAT_BLEND = 0.6, GAIT_BLEND = 0.4,
            TURN_BLEND = 0.3, LIFE_BLEND = 0.5, LOOK_BLEND = 0.4, CALM_BLEND = 0.35, SETTLE_BLEND = 0.5, PLANT_BLEND = 0.6,
            /** Fix r3: how long a death takes to hand the ground pass over to its clip. */
            GROUND_FADE = 0.4;
    /** The limb speed above which a unit starts moving and below which it stops (hysteresis). */
    static final double MOVE_ON = 0.1, MOVE_OFF = 0.04;
    /** Ground input: velocity smoothing (Hz), the jump (blocks) and gap (s) that start it afresh, and the time off the ground that counts as airborne (s). */
    static final double VEL_HZ = 12, GROUND_JUMP = 2, GROUND_GAP = 0.25, AIR_T = 0.06;
    /** The limb speed a run starts at, and how much faster it is all run. */
    static final double RUN_FROM = 0.35, RUN_SPAN = 0.6;
    /**
     * An engine-made fade may move a channel at most this far in one 60 fps frame (degrees, pixels). This leaves room under
     * the 12 degree / 1.5 px pop limit for the procedural layers moving in the same frame.
     */
    static final double FADE_ROT = 6, FADE_PX = 0.8;
    static final double[] FADES = {0.08, 0.12, 0.18, 0.25, 0.35, 0.45};
    /** The largest gains an attack (seed amp) and a hit clip (weight scale times seed amp) can play at. */
    static final double ATTACK_GAIN = 1.1, HIT_GAIN = 1.4 * 1.1;
    /** Per hit weight (light, normal, heavy): the clip gain, the tempo multiplier and the reaction pulse's scale. */
    static final double[] HIT_W_GAIN = {0.6, 1.0, 1.4}, HIT_W_TEMPO = {1.25, 1.0, 0.8}, HIT_W_PULSE = {0.55, 1.0, 1.5};
    /** A hit clip without an impact marker plays its first HIT_SNAP seconds (the snap) in real time; the weight's tempo shapes the recovery. */
    static final double HIT_SNAP = 0.1;
    /**
     * Fix r3 (grade r2 item 3): a hit that lands while an attack plays is an additive overlay on the attack, which keeps playing
     * (no FROM track, no restart): its weight eases in over HIT_OVER_IN seconds (4 ticks) and out over HIT_OVER_OUT once its clip
     * ends, at HIT_OVER_GAIN of the hit's gain, and its reaction pulse at HIT_OVER_PULSE of its scale. At most MAX_OVERLAYS play at once; past that the oldest goes.
     */
    static final double HIT_OVER_IN = 0.2, HIT_OVER_OUT = 0.15, HIT_OVER_PULSE = 0.3, HIT_OVER_GAIN = 0.5;
    static final int MAX_OVERLAYS = 3;
    /** A death plays at the seed amp pivoted on its last frame, g c(t) + (1 - g) c(end): at most 1.1 + 0.1 of the clip's reach. */
    static final double DEATH_GAIN = 1.2;
    /** P1: the largest gain a death clip plays at (the seed's amp; K1b pivots it on the last frame, so its steps scale by it). */
    static final double DEATH_STEP_GAIN = 1.1;
    /** P1, R101: no interrupt or stance cross-fade steps a bone past this times the family's peak (Family.peak). */
    static final double INTERRUPT_HEADROOM = 1.2;
    /** P1: the interrupt fade's shortest and longest (s), the stance cross-fade's shortest, and the FROM track's inertia time constant (s). */
    static final double INTERRUPT_MIN = 0.08, INTERRUPT_MAX = 0.6, STANCE_MIN = 0.2, FROM_TD = 0.08;
    /** Round 4: an attack fades in over at least this (s), so its weight is still easing in, not at its steepest, when the windup turns. */
    static final double ATTACK_IN = 0.25;
    /**
     * Fix 2: a stance cross-fade's largest second difference (degrees a tick squared, summed over bones' angles) and the longest
     * fade (s) that bound may ask for.
     */
    static final double STANCE_ACC = 6, STANCE_LONG = 0.85;
    /** P1: the span (s) the FROM track's velocity is measured over, the fold at now against the fold FROM_H before (frame-rate free). */
    static final double FROM_H = 1 / 120.0;
    /** Bounds on everything the procedural layers can add to one channel (degrees, pixels). */
    static final double PROC_ROT = 180, PROC_PX = 16;
    static final int ATTACK = 0, HIT = 1, DEATH = 2, MERGED = 3, MAX_LAYERS = 6, MAX_TRACKS = 6;
    /** The limb angle wraps on a whole number of strides, so the gait phase (limb angle times 0.6662) runs on across it. */
    static final double LIMB_WRAP = 100000 * MotionMath.TAU / 0.6662;
    /** Ramp slots in {@link State#w}. */
    static final int W_MOVE = 0, W_RUN = 1, W_COMBAT = 2, W_SHUFFLE = 3, W_BACK = 4, W_TURN = 5, W_LIFE = 6, W_LOOK = 7,
            W_CALM = 8, W_SETTLE = 9, W_PLANT = 10, W_STRAPS = 11, WEIGHTS = 12;

    /**
     * One weapon family's clips (design section 6.2), resolved from ClipLibrary.
     * <ul>
     * <li>stances by name: idle, carry, guard, brace, wall, shield, aim and rest. A missing stance falls back along
     * {@link #stanceChain}.</li>
     * <li>attacks by variant: the server's variant picks {@code floorMod(variant, attacks.size())}.</li>
     * <li>hits by zone: head, body, legs and block. A missing zone falls back to body.</li>
     * <li>deaths by "kind/direction", then "any/direction". The direction is front, back, left or right. Death clips are
     * absolute relative to rest: on death the stance fades to rest and life, look and hit reactions fade out, so the
     * corpse is the death clip's last key and nothing else.</li>
     * </ul>
     */
    public record Family(String name, Map<String, Clip> stances, List<Clip> attacks, List<Clip> wallAttacks, Map<String, List<Clip>> hits,
                         Map<String, List<Clip>> deaths, Map<String, List<Clip>> hitsMirror, Map<String, double[]> vPeak) {
        /** A family whose mirrored hits (R106) are built from its hits. */
        public Family(String name, Map<String, Clip> stances, List<Clip> attacks, Map<String, List<Clip>> hits, Map<String, List<Clip>> deaths) {
            this(name, stances, attacks, List.of(), hits, deaths, null, null);
        }

        /**
         * A family with wall strikes (Task 16): wallAttacks.get(i) is attacks.get(i) as thrown from the cocked wall or brace
         * hold, over the board; empty when the family has none.
         */
        public Family(String name, Map<String, Clip> stances, List<Clip> attacks, List<Clip> wallAttacks, Map<String, List<Clip>> hits,
                      Map<String, List<Clip>> deaths) {
            this(name, stances, attacks, wallAttacks, hits, deaths, null, null);
        }

        /** A family with its peak velocities (P1) sampled from its clips. */
        public Family(String name, Map<String, Clip> stances, List<Clip> attacks, Map<String, List<Clip>> hits, Map<String, List<Clip>> deaths,
                      Map<String, List<Clip>> hitsMirror) {
            this(name, stances, attacks, List.of(), hits, deaths, hitsMirror, null);
        }

        /**
         * P1, R101: the fastest any bone moves in any of the family's clips, {degrees per tick, px per tick} (MotionMath.boneStep at
         * 20 Hz, each clip at its largest gain): the speed no interrupt or stance cross-fade may step past by more than INTERRUPT_HEADROOM.
         */
        public double[] peak() {
            double[] p = new double[2];
            for (double[] v : vPeak.values()) { p[0] = Math.max(p[0], v[0]); p[1] = Math.max(p[1], v[1]); }
            return p;
        }

        public Family {
            Objects.requireNonNull(name, "name");
            stances = Map.copyOf(stances);
            attacks = List.copyOf(attacks);
            wallAttacks = wallAttacks == null ? List.of() : List.copyOf(wallAttacks);
            if (!wallAttacks.isEmpty() && wallAttacks.size() != attacks.size())
                throw new IllegalArgumentException("wallAttacks must parallel attacks: " + wallAttacks.size() + " vs " + attacks.size());
            Map<String, List<Clip>> h = new HashMap<>();
            for (var e : hits.entrySet()) h.put(e.getKey(), List.copyOf(e.getValue()));
            hits = Map.copyOf(h);
            Map<String, List<Clip>> hm = new HashMap<>();
            for (var e : hits.entrySet()) {
                List<Clip> given = hitsMirror == null ? null : hitsMirror.get(e.getKey());
                List<Clip> l = new ArrayList<>();
                for (int i = 0; i < e.getValue().size(); i++) l.add(given != null && i < given.size() ? given.get(i) : e.getValue().get(i).mirror());
                hm.put(e.getKey(), List.copyOf(l));
            }
            hitsMirror = Map.copyOf(hm);
            Map<String, List<Clip>> d = new HashMap<>();
            for (var e : deaths.entrySet()) {
                List<Clip> l = new ArrayList<>(e.getValue());
                l.sort(java.util.Comparator.comparing(Clip::name));
                d.put(e.getKey(), List.copyOf(l));
            }
            deaths = Map.copyOf(d);
            if (vPeak == null) {
                Map<String, double[]> vp = new HashMap<>();
                for (Clip c : stances.values()) peakInto(vp, c, 1);
                for (Clip c : attacks) peakInto(vp, c, ATTACK_GAIN);
                for (Clip c : wallAttacks) peakInto(vp, c, ATTACK_GAIN);
                for (List<Clip> l : hits.values()) for (Clip c : l) peakInto(vp, c, HIT_GAIN);
                for (List<Clip> l : hitsMirror.values()) for (Clip c : l) peakInto(vp, c, HIT_GAIN);
                for (List<Clip> l : deaths.values()) for (Clip c : l) peakInto(vp, c, DEATH_STEP_GAIN);
                vPeak = vp;
            }
            Map<String, double[]> vc = new HashMap<>();
            for (var e : vPeak.entrySet()) vc.put(e.getKey(), e.getValue().clone());
            vPeak = java.util.Collections.unmodifiableMap(vc);
        }

        private static void peakInto(Map<String, double[]> vp, Clip c, double gain) {
            for (String b : c.bones()) {
                double[] s = c.peakStep(b, false), o = vp.computeIfAbsent(b, k -> new double[2]);
                o[0] = Math.max(o[0], s[0] * gain);
                o[1] = Math.max(o[1], s[1] * gain);
            }
        }
    }

    private static final java.util.regex.Pattern DEATH_NAME =
        java.util.regex.Pattern.compile("death(?:_(slash|stab|blunt|arrow|magic))?(?:_(front|back|left|right))?(?:_([0-9]+))?");

    /**
     * A death clip's key in Family.deaths (R105: {@code death_<kind>[_<dir>]_<n>}, n a variant number): death is "any",
     * death_<dir> "any/<dir>", death_<kind> "<kind>", death_<kind>_<dir> "<kind>/<dir>"; a trailing _<n> appends the clip to
     * the same key's list. Null for any other clip.
     */
    public static String deathKey(String name) {
        java.util.regex.Matcher m = DEATH_NAME.matcher(name);
        if (!m.matches()) return null;
        String kind = m.group(1), dir = m.group(2);
        if (kind == null) return dir == null ? "any" : "any/" + dir;
        return dir == null ? kind : kind + "/" + dir;
    }

    /**
     * Per-entity engine state: weights, phases, springs, the stance stack, the active tracks, hit reactions, the novelty
     * timer and the scratch the layers reuse every frame.
     */
    public static final class State {
        boolean started, moving, dead;
        /** Last frame's render time and limb speed (the speed lets a moving flip find where in the frame it crossed). */
        double last, lastSpeed;
        /** Linear ramps in [0, 1] (the engine uses their eased values) and the target each had last frame. */
        final double[] w = new double[WEIGHTS], wTarget = new double[WEIGHTS];
        /** Turn-in-place step per foot: yaw relative to the body (deg), swing progress (-1 planted), swing start yaw, and the
         * foot target offset (x, lift, z in px) MotionFeet.plant applies. */
        final double[] turnYaw = new double[2], turnU = {-1, -1}, turnFrom = new double[2];
        final double[][] turnOff = new double[2][3];
        /** Seconds since the last start from standing (MotionBody.stride sets 0 and advances it). */
        double startAge = Double.POSITIVE_INFINITY;
        double breathPhase, breathRate, flutter, flutterRate, yawRate, yawRateTarget;
        final List<MotionStack.Layer> stances = new ArrayList<>(MAX_LAYERS + 1);
        final List<MotionStack.Track> tracks = new ArrayList<>(MAX_TRACKS + 1);
        /** Hits playing as additive overlays on an attack (HIT_OVER_*). */
        final List<MotionStack.Track> overlays = new ArrayList<>(MAX_OVERLAYS + 1);
        int ids;
        /** Hits seen so far, and the variant each hit zone (body, head, legs, block) played last (H1). */
        int hitCount;
        final int[] lastHitVariant = {-1, -1, -1, -1};
        /** Death edges seen so far: a revived unit's next death picks its variant afresh (K1b). */
        int deaths;
        /**
         * Whether the unit revived after its corpse's straps began to settle, so they stay settled until the death clip has
         * faded out; and when the last death clip to fade out did (MotionStack.deathGone).
         */
        boolean clothHeld;
        double deathGone = Double.NEGATIVE_INFINITY;
        MotionInput.Attack attack;
        MotionInput.Hit hit, lethal;
        final List<MotionBody.Pulse> pulses = new ArrayList<>(MotionBody.MAX_PULSES + 1);
        final double[] react = new double[MotionBody.REACT], reactVel = new double[MotionBody.REACT],
                reactPrev = new double[MotionBody.REACT], reactTarget = new double[MotionBody.REACT];
        final double[] look = new double[2], lookVel = new double[2], lookPrev = new double[2];
        final double[] lag = new double[MotionBody.LAGS], lagVel = new double[MotionBody.LAGS], lagPrev = new double[MotionBody.LAGS],
                lagTarget = new double[MotionBody.LAGS], legsPrev = new double[2];
        double noveltyNext;
        long noveltyRng;
        /** Novelty idles started so far. */
        int novelties;
        /** The novelty idle playing: its kind (-1 for none), start, length and side. */
        int novKind = -1;
        double novStart, novLength, novSide;
        /**
         * What the planted-feet solve corrected last frame, as the leg channels would jump if it let go at once: the largest
         * rotation (degrees) and translation (pixels), scaled by how much the feet would be planted with no leg-keying clip.
         */
        double plantRot, plantPx;
        final Frame frame = new Frame();

        // scratch, so steady-state frames do not allocate
        final double[] tmp = new double[6];
        final Clip[] mixClips = new Clip[MotionStack.LAYER_CLIPS];
        final double[] mixCoef = new double[MotionStack.LAYER_CLIPS];
        final double[] shares = new double[MAX_TRACKS + 1];
        final double[] fadeRot = new double[MotionStack.FADE_FRAMES + 1], fadePx = new double[MotionStack.FADE_FRAMES + 1];
        final Pose fadeMine = new Pose(), fadeUnder = new Pose(), merged = new Pose(), mergeTmp = new Pose();
        /** P1: the FROM track's velocity (per second; State.merged holds its pose). */
        final Pose fromVel = new Pose();
        /** P1: scratch for the stance and interrupt cross-fade bounds. */
        final Pose stanceOld = new Pose(), stanceNew = new Pose();
        final double[][] feet = new double[2][3], sol = new double[2][3];
        final double[] vec = new double[3], rootFull = new double[6], knee = new double[2];
        /** Fix r3: the stride solve's scratch: each foot's lowest corner under its sole's middle (px), and a solved leg and shin. */
        final double[] drop = new double[2], legT = new double[6], shinT = new double[6];
        /** Round 4: the stride's hip dip as last drawn, so a release eases out (MotionFeet.DIP_RELEASE) instead of snapping. */
        double dipHeld;
        /** Ground input (Task 16): last position (blocks) and yaw (deg), the smoothed body-frame velocity (b/s), time off the ground. */
        double gx, gy, gz, gyaw, vF, vL, vU, airT;
        boolean gInit, teleport;
        /** G1: the gait's own phase (cycles). */
        double psi;
        boolean psiInit;
        /** Fix 2: the sweep runs at full weight since a start from standing; the upper body's own ease; last speed and its smoothed rise. */
        boolean sweepFull;
        double upW, vPrev, acc;
        /**
         * Fix r3 (gate 2): each foot's sweep (px) carried frame to frame, so a stance foot moves back by exactly the travel however
         * the speed and stride change; where its swing set off (u, px, start slope px per u); whether it is in swing; whether it has
         * stopped for good after a stop (locked); the gait's hold through a stop's settling steps.
         */
        final double[] sd = new double[2], su0 = new double[2], sd0 = new double[2], sm0 = new double[2];
        final boolean[] inSwing = new boolean[2], locked = new boolean[2], settleSwing = new boolean[2];
        /** Fix r3: each foot's own stride weight; a stop's last step lands each foot on its idle pose at weight 0. */
        final double[] fw = {1, 1};
        boolean fresh = true;
        double hold;
        /** Fix r3: the ground pass's weight and last frame's target, eased out over GROUND_FADE without the ground input. */
        double groundW = 1;
        double groundT = 1;
        /** Fix r3: how far a settled corpse's ground lift is held under its torso's clearance, and last frame's target. */
        double lieW, lieT;
        /** Fix r3 (gate 2): each stance foot's roll shift (px, x and z), the one it held at liftoff, its corner's offset from the middle and that corner, and pass scratch. */
        final double[][] pc = new double[2][2], pcl = new double[2][2], pp = new double[2][2], pNext = new double[2][2], pivotT = new double[2][2];
        final int[] pIdx = new int[2], pNextIdx = new int[2];
        final boolean[] pStance = new boolean[2], pNow = new boolean[2];
    }

    /**
     * The body-frame ground velocity (Task 16 ground input): blocks per second forward, to the left and up, smoothed at VEL_HZ,
     * from the change in the entity's position since last frame. A first frame, a move over GROUND_JUMP blocks or a gap over
     * GROUND_GAP s starts afresh at rest and marks a teleport. Without a ground the velocity is zero and the
     * engine strides by limbSpeed. Airborne only after AIR_T s off the ground, so a one-frame packet flicker does not count.
     */
    static void ground(MotionInput.Ground g, State st, Frame f, double rawDt) {
        st.teleport = false;
        f.dyaw = f.dF = f.dL = 0;
        f.ground = g != null && Double.isFinite(g.x()) && Double.isFinite(g.y()) && Double.isFinite(g.z()) && Double.isFinite(g.yawDeg());
        if (!f.ground) {
            st.gInit = false;
            st.vF = st.vL = st.vU = st.airT = 0;
            f.vF = f.vL = 0;
            f.air = false;
            return;
        }
        double dx = g.x() - st.gx, dy = g.y() - st.gy, dz = g.z() - st.gz;
        if (!st.gInit || dx * dx + dz * dz > GROUND_JUMP * GROUND_JUMP || rawDt > GROUND_GAP) {
            st.teleport = st.gInit;
            st.gInit = true;
            st.vF = st.vL = st.vU = 0;
        } else if (f.dt > 0) {
            double y = Math.toRadians(g.yawDeg()), sy = Math.sin(y), cy = Math.cos(y);
            double vx = dx / rawDt, vz = dz / rawDt, k = 1 - Math.exp(-VEL_HZ * f.dt);
            st.vF += (vx * -sy + vz * cy - st.vF) * k;
            st.vL += (vx * cy + vz * sy - st.vL) * k;
            st.vU += (dy / rawDt - st.vU) * k;
            f.dyaw = MotionMath.wrapDegrees(g.yawDeg() - st.gyaw);
            // this frame's own travel in last frame's body frame, unsmoothed: what carries the planted feet's ground points
            double oy = Math.toRadians(st.gyaw), so = Math.sin(oy), co = Math.cos(oy);
            f.dF = dx * -so + dz * co;
            f.dL = dx * co + dz * so;
        }
        st.gx = g.x(); st.gy = g.y(); st.gz = g.z(); st.gyaw = g.yawDeg();
        st.airT = g.onGround() ? 0 : st.airT + f.dt;
        f.air = !g.onGround() && st.airT > AIR_T;
        f.vF = st.vF;
        f.vL = st.vL;
    }

    /** What one frame's layers share: the sanitised input and the eased weights. */
    static final class Frame {
        double now, dt, speed, limbAngle, headYaw, headPitch, yawRate, aimPitch;
        boolean first, dead, enemy;
        MotionMath.Seed seed;
        /**
         * Eased weights: moving, running, combat, shield shuffle, backpedal, turning in place, life, look, settle, the straps'
         * settle and planted feet; calm is raw. The planted-feet weight drops on death and eases back in on revive, so the feet are not
         * re-planted at full strength while the body rises.
         */
        double m, run, cw, sh, bp, tw, lw, lookW, settle, straps, plant, calm;
        /** How much of the pose leg-keying actions own, and how much the aim stance does. */
        double bodyAction, aim;
        /** Ground input (Task 16): whether there is one, whether airborne, the body-frame velocity (b/s), this frame's turn (deg) and its travel forward and left (blocks). */
        boolean ground, air;
        double vF, vL, dyaw, dF, dL;
        /** G1 stride (ground only): its weight, duty, horizontal speed (b/s), and per leg (right, left) the phase in [0, 1), the foot's sweep along the travel (px, forward of the hip) and its swing lift (0 in stance, 1 at mid-swing). */
        double gaitW, duty, gaitV;
        final double[] q = new double[2], sweep = new double[2], lift = new double[2];
    }

    /** One clip a track is playing, for tests and culling: its clip time and its effective share of the fold, times the track's gain. */
    record Playing(int id, Clip clip, double time, double gain) {}

    /** Poses one entity for this frame into {@code out}, clearing it first. */
    public static void pose(MotionInput in, State st, Family fam, Pose out) {
        out.clear();
        Frame f = st.frame;
        f.first = !st.started;
        f.now = Double.isFinite(in.time()) ? in.time() : st.started ? st.last : 0;
        double rawDt = f.first ? 0 : f.now - st.last;
        f.dt = MotionMath.clampDt(rawDt);
        st.started = true;
        st.last = f.now;
        f.seed = in.seed();
        ground(in.ground(), st, f, rawDt);
        f.speed = f.ground ? clamp(Math.hypot(f.vF, f.vL) / 5, 0, 1.5) : clamp(finite(in.limbSpeed()), 0, 1.5);
        f.limbAngle = finite(in.limbAngle()) % LIMB_WRAP;
        f.headYaw = clamp(finite(in.headYaw()), -85, 85);
        f.headPitch = clamp(finite(in.headPitch()), -60, 60);
        f.yawRate = clamp(finite(in.bodyYawRate()), -720, 720);
        f.aimPitch = clamp(finite(in.aimPitch()), -60, 60);
        f.dead = in.dead();
        f.enemy = in.enemyNear() && !f.dead;
        String stance = in.stance();
        if (st.dead && !f.dead) st.lethal = null;

        boolean wasMoving = st.moving;
        if (f.speed > MOVE_ON) st.moving = true;
        else if (f.speed < MOVE_OFF) st.moving = false;
        // The limb speed is continuous, so a flip happened inside the frame: the share of the frame after the speed crossed
        // its edge (taking the speed as linear across the frame) is spent moving toward the new target, as at any frame rate.
        double s0 = st.lastSpeed, after = st.moving != wasMoving && !f.first && f.speed != s0
            ? MotionMath.clamp01((f.speed - (st.moving ? MOVE_ON : MOVE_OFF)) / (f.speed - s0)) : 0;
        st.lastSpeed = f.speed;
        boolean moving = st.moving && !f.dead;
        if (f.dead) after = 0;
        ramp(st, W_MOVE, moving ? 1 : 0, MOVE_BLEND, f, after);
        runWeight(st, moving, s0, f);
        ramp(st, W_COMBAT, f.enemy ? 1 : 0, COMBAT_BLEND, f);
        ramp(st, W_SHUFFLE, !f.dead && (in.shieldUp() || stance.equals("shield_move")) ? 1 : 0, GAIT_BLEND, f);
        ramp(st, W_BACK, !f.dead && stance.equals("fall_back") ? 1 : 0, GAIT_BLEND, f);
        ramp(st, W_TURN, !moving && !f.dead ? MotionMath.clamp01((Math.abs(f.yawRate) - 30) / 60) : 0, TURN_BLEND, f, after);
        ramp(st, W_LIFE, f.dead ? 0 : 1, LIFE_BLEND, f);
        ramp(st, W_LOOK, f.dead ? 0 : 1, LOOK_BLEND, f);
        ramp(st, W_PLANT, f.dead ? 0 : 1, PLANT_BLEND, f);
        double[] w = st.w;
        f.m = MotionMath.ease(w[W_MOVE]); f.run = MotionMath.ease(w[W_RUN]); f.cw = MotionMath.ease(w[W_COMBAT]);
        f.sh = MotionMath.ease(w[W_SHUFFLE]); f.bp = MotionMath.ease(w[W_BACK]); f.tw = MotionMath.ease(w[W_TURN]);
        f.lw = MotionMath.ease(w[W_LIFE]); f.lookW = MotionMath.ease(w[W_LOOK]); f.plant = MotionMath.ease(w[W_PLANT]);

        events(in, st, fam, f, stanceKey(stance, in.shieldUp(), f.enemy, moving, f.dead));
        boolean acting = !st.tracks.isEmpty() || !st.pulses.isEmpty();
        ramp(st, W_CALM, !moving && !f.enemy && !acting && !f.dead && w[W_TURN] == 0 ? 1 : 0, CALM_BLEND, f);
        f.calm = w[W_CALM];
        double settles = MotionStack.deathSettles(st);
        boolean played = f.dead && f.now >= settles;
        ramp(st, W_SETTLE, played ? 0 : 1, SETTLE_BLEND, f, played && !f.first ? MotionMath.clamp01((f.now - settles) / f.dt) : 0);
        f.settle = MotionMath.ease(w[W_SETTLE]);
        // A unit revived once its corpse's straps had begun to settle keeps them settled until the death clip has faded out,
        // then wakes them over SETTLE_BLEND, exactly from when it faded out. The fade-out is a cross-fade from lying to
        // standing, not motion: the hips swing some 88 degrees in under half a second, and a waking strap whips on its
        // spring's lag behind them (Task 16). The tabard, cape and crest wake with the settle weight: held, the tabard
        // would follow the legs' cross-fade unsprung.
        if (st.dead && !f.dead) st.clothHeld = f.now >= settles;
        double wakes = !f.dead && st.clothHeld ? MotionStack.deathGone(st) : Double.NEGATIVE_INFINITY;
        boolean held = f.dead ? played : f.now < wakes;
        double wake = f.first ? 0 : f.dead ? (played ? MotionMath.clamp01((f.now - settles) / f.dt) : 0)
                : held || wakes == Double.NEGATIVE_INFINITY ? 0 : MotionMath.clamp01((f.now - wakes) / f.dt);
        ramp(st, W_STRAPS, held ? 0 : 1, SETTLE_BLEND, f, wake);
        f.straps = MotionMath.ease(w[W_STRAPS]);

        MotionBody.locomotion(f, st, out);
        MotionStack.stance(st, fam, f, stanceKey(stance, in.shieldUp(), f.enemy, moving, f.dead), out);
        MotionBody.life(f, st, out);
        MotionStack.action(st, f, out);
        MotionBody.react(f, st, out);
        MotionBody.look(f, st, out);
        MotionFeet.plant(f, st, out);
        // the ground pass sits on the stride's solve: without the ground input the legs are the unsolved limb-angle gait, whose soles
        // swing through the ground at a run, and lifting the hips off them hops the body (MotionLayersCheck pops), so it fades out.
        // A death keeps it on through the fall (which sweeps the shins up to 7 px through the ground); once the clip ends the
        // lift eases under the torso's clearance (MotionFeet.torsoClear), so the corpse lies in its authored end pose rather than
        // propped on a boot toe. Like ramp(): this frame chases last frame's target, and a target that changed inside the frame
        // moves only the frame's last share, so both fades are the same at any frame rate.
        double groundT = f.ground ? 1 : 0;
        st.groundW = f.first ? groundT : toward(st.groundW, st.groundT, f.dt / GROUND_FADE);
        st.groundT = groundT;
        double ends = MotionStack.deathEnds(st);
        double lieT = f.dead && f.now >= ends ? 1 : 0, lAfter = f.first ? 0 : lieT > 0 && st.lieT == 0 ? MotionMath.clamp01((f.now - ends) / f.dt) : 0;
        st.lieW = f.first ? lieT : toward(toward(st.lieW, st.lieT, f.dt * (1 - lAfter) / GROUND_FADE), lieT, f.dt * lAfter / GROUND_FADE);
        st.lieT = lieT;
        // the secondary springs read the hips before the ground pass: its lift is a whole-body raise, not hip motion for the straps to whip on
        MotionBody.secondary(f, st, out);
        MotionFeet.ground(out, st.vec, MotionMath.ease(st.groundW), MotionMath.ease(st.lieW));
        st.dead = f.dead;
    }

    /**
     * The stance key for this frame's state (design section 6.2). Hold, or an enemy near, raises the guard; Run is the march
     * carry unless an enemy is near, when it is a charge with the guard up. A dead unit's stance is rest ("dead" has no clip).
     */
    static String stanceKey(String stance, boolean shieldUp, boolean enemy, boolean moving, boolean dead) {
        if (dead) return "dead";
        switch (stance) {
            case "wall": return "wall";
            case "brace": return shieldUp ? "shield" : "brace";
            case "ranged_aim": return "aim";
            case "ranged_rest": return "rest";
            case "hold": return shieldUp ? "shield" : "guard";
            case "run": return enemy ? "guard" : "carry";
            default:
        }
        if (stance.equals("shield_move") || shieldUp) return "shield";
        return enemy ? "guard" : moving ? "carry" : "idle";
    }

    private static final String[] CARRY = {"carry", "idle"}, GUARD = {"guard", "carry", "idle"}, BRACE = {"brace", "guard", "carry", "idle"},
            WALL = {"wall", "shield", "brace", "guard", "carry", "idle"}, SHIELD = {"shield", "wall", "guard", "carry", "idle"},
            AIM = {"aim", "guard", "carry", "idle"}, REST = {"rest", "carry", "idle"}, IDLE = {"idle", "carry"}, NONE = {};

    /** Where a family without a stance clip falls back to, nearest first. Shared arrays: never modify. */
    static String[] stanceChain(String key) {
        switch (key) {
            case "carry": return CARRY;
            case "guard": return GUARD;
            case "brace": return BRACE;
            case "wall": return WALL;
            case "shield": return SHIELD;
            case "aim": return AIM;
            case "rest": return REST;
            case "dead": return NONE;
            default: return IDLE;
        }
    }

    /**
     * Starts attacks, hit reactions and deaths from this frame's input. A new event is one not equal to the last seen. An attack
     * started in the wall or brace hold (stanceKey, this frame's) plays the family's wall strike, thrown over the board from
     * the cocked sword.
     */
    static void events(MotionInput in, State st, Family fam, Frame f, String stanceKey) {
        MotionInput.Attack attack = in.attack();
        if (attack != null && !attack.equals(st.attack)) {
            st.attack = attack;
            if (!f.dead && !fam.attacks().isEmpty()) {
                List<Clip> set = (stanceKey.equals("wall") || stanceKey.equals("brace")) && !fam.wallAttacks().isEmpty() ? fam.wallAttacks() : fam.attacks();
                Clip clip = set.get(Math.floorMod(attack.variant(), set.size()));
                var tr = new MotionStack.Track(st.ids++, ATTACK, clip, null, 0, f.seed.amp(), attack.startTick() / (double) Clip.TICKS, f.seed.tempo());
                if (tr.time(f.now) < clip.length() - 0.05) MotionStack.begin(st, fam, tr, f);
            }
        }
        MotionInput.Hit hit = in.hit();
        if (hit != null && !hit.equals(st.hit)) {
            st.hit = hit;
            if (hit.lethal()) st.lethal = hit;
            double at = hit.tick() / (double) Clip.TICKS, age = f.now - at;
            if (!f.dead && age >= -0.2 && age <= 0.6) {
                int weight = Math.max(0, Math.min(2, hit.weight()));
                boolean over = !f.first && MotionStack.attacking(st);
                MotionBody.pulse(st, hit, at, HIT_W_PULSE[weight] * f.seed.amp() * (over ? HIT_OVER_PULSE : 1), weight);
                // Task 16: a blocked blow plays its zone's block (block_head, block for the body, block_legs), else the body's
                String zk = zone(hit.zone()), key = hit.blocked() ? (zk.equals("body") ? "block" : "block_" + zk) : zk;
                List<Clip> set = fam.hits().get(key);
                if (hit.blocked() && (set == null || set.isEmpty())) set = fam.hits().get(key = "block");
                if (set == null || set.isEmpty()) set = fam.hits().get(key = "body");
                if (set != null && !set.isEmpty()) {
                    // H1: a seeded variant per hit, never the same one twice running in a zone
                    int n = set.size(), z = key.equals("head") ? 1 : key.equals("legs") ? 2 : key.startsWith("block") ? 3 : 0;
                    int idx = hitVariant(f.seed.variantBias(), ++st.hitCount, st.lastHitVariant[z], n);
                    st.lastHitVariant[z] = idx;
                    // R106: a blow from the unit's left plays the mirrored clip; never a block, whose board is on the left arm
                    // (mirrored, the sword arm would take the blow), so a block's side comes from the pulse alone
                    double side = Math.sin(Math.toRadians(finite(hit.bearing())));
                    List<Clip> mirrored = key.startsWith("block") ? null : fam.hitsMirror().get(key);
                    Clip clip = side < -0.33 && mirrored != null && mirrored.size() == n ? mirrored.get(idx) : set.get(idx);
                    // the hit clips are authored front-on: from behind they fade out (none past ~132 degrees) and the
                    // procedural pulse, which flips with the bearing, carries the blow, so a back hit pitches forward
                    double front = key.startsWith("block") ? 1 : MotionMath.clamp01(1 + 1.5 * Math.cos(Math.toRadians(finite(hit.bearing()))));
                    var tr = new MotionStack.Track(st.ids++, HIT, clip, null, 0, HIT_W_GAIN[weight] * f.seed.amp() * front * (over ? HIT_OVER_GAIN : 1), at,
                        f.seed.tempo() * HIT_W_TEMPO[weight], clip.impact() < 0 ? HIT_SNAP : clip.impact() / (double) Clip.TICKS);
                    if (tr.time(f.now) < clip.length() - 0.05) {
                        if (over) MotionStack.overlay(st, tr);
                        else MotionStack.begin(st, fam, tr, f);
                    }
                }
            }
        }
        if (f.dead && !st.dead) {
            MotionStack.Track tr = death(st, fam, f, finite(in.deathSeconds()));
            if (tr != null) MotionStack.begin(st, fam, tr, f);
        }
    }

    /** H1: the variant of n a unit's count-th hit plays, never {@code last} again when n > 1. */
    static int hitVariant(long variantBias, int count, int last, int n) {
        long h = MotionMath.mix(variantBias + 0x9E3779B97F4A7C15L * count);
        int idx = (int) Math.floorMod(h, (long) n);
        if (n > 1 && idx == last) idx = (idx + 1 + (int) Math.floorMod(h >>> 32, (long) (n - 1))) % n;
        return idx;
    }

    /** Hit zones as the engine knows them: head, legs or body. */
    static String zone(String zone) {
        if (zone == null) return "body";
        switch (zone) {
            case "head": case "neck": return "head";
            case "legs": case "leg": case "feet": return "legs";
            default: return "body";
        }
    }

    /**
     * A death clip chosen from the lethal blow. Up to 90 degrees off the front it mixes front with the side the blow came
     * from; past 90 it mixes that side with back. A missing direction falls back to the nearest one the family has.
     */
    static MotionStack.Track death(State st, Family fam, Frame f, double seconds) {
        String kind = st.lethal == null || st.lethal.kind() == null ? "any" : st.lethal.kind();
        double bearing = st.lethal == null ? 0 : Math.IEEEremainder(finite(st.lethal.bearing()), 360);
        String side = bearing > 0 ? "right" : "left";
        double abs = Math.abs(bearing);
        String from = abs <= 90 ? "front" : side, to = abs <= 90 ? side : "back";
        double mix = abs <= 90 ? abs / 90 : (abs - 90) / 90;
        // K1b: the variant comes from the seed and the death count, so units struck alike fall differently
        long h = MotionMath.mix(f.seed.variantBias() ^ 0xD6E8FEB86659FD93L ^ st.deaths++);
        int idx = (int) Math.floorMod(h, 6L);
        Clip a = deathClip(fam, kind, from, idx), b = deathClip(fam, kind, to, idx);
        if (a == null && b == null) {
            for (String dir : abs <= 90 ? List.of("front", side, "back") : List.of("back", side, "front"))
                if ((a = deathClip(fam, kind, dir, idx)) != null) break;
            if (a == null) a = fam.deaths().values().stream().flatMap(List::stream).min((x, y) -> x.name().compareTo(y.name())).orElse(null);
            if (a == null) return null;
            mix = 0;
        } else if (a == null) { a = b; b = null; mix = 0; }
        else if (b == null || b == a) { b = null; mix = 0; }
        return new MotionStack.Track(st.ids++, DEATH, a, b, mix, f.seed.amp(), f.now - Math.max(0, seconds), f.seed.tempo());
    }

    static Clip deathClip(Family fam, String kind, String dir) { return deathClip(fam, kind, dir, 0); }

    /** The death clip for kind and direction, variant {@code idx} (modulo the variants that key has). */
    static Clip deathClip(Family fam, String kind, String dir, int idx) {
        List<Clip> l = fam.deaths().get(kind + "/" + dir);
        if (l == null) l = fam.deaths().get("any/" + dir);
        if (l == null && dir.equals("front")) l = fam.deaths().getOrDefault(kind, fam.deaths().get("any"));
        return l == null || l.isEmpty() ? null : l.get(Math.floorMod(idx, l.size()));
    }

    /** The clips the tracks are playing at {@code now}, bottom first, each with its effective share of the fold. */
    static List<Playing> playing(State st, double now) { return MotionStack.playing(st, now); }

    /** Active tracks, hit pulses and stance layers: the state that grows with events. */
    static int load(State st) { return st.tracks.size() + st.overlays.size() + st.pulses.size() + st.stances.size(); }

    /**
     * The largest |value| any bone's channel can take for this family, per channel: the procedural layers' bounds, plus the
     * largest stance, plus the largest action at its largest gain. Rotations are in degrees and translations in pixels. Task
     * 13 derives cull bounds from the translations.
     */
    public static double[] reach(Family fam) {
        double[] stance = new double[6], action = new double[6], r = new double[6];
        for (Clip c : fam.stances().values()) max(stance, c.reach(), 1);
        for (Clip c : fam.attacks()) max(action, c.reach(), ATTACK_GAIN);
        for (Clip c : fam.wallAttacks()) max(action, c.reach(), ATTACK_GAIN);
        for (List<Clip> l : fam.hits().values()) for (Clip c : l) max(action, c.reach(), HIT_GAIN);
        for (List<Clip> l : fam.deaths().values()) for (Clip c : l) max(action, c.reach(), DEATH_GAIN);
        double[] over = new double[6];
        for (List<Clip> l : fam.hits().values()) for (Clip c : l) max(over, c.reach(), HIT_GAIN * HIT_OVER_GAIN * MAX_OVERLAYS);
        for (int c = 0; c < 6; c++) r[c] = (c < 3 ? PROC_ROT : PROC_PX) + stance[c] + action[c] + over[c];
        return r;
    }

    /**
     * As {@link #reach(Family)} for one bone's translation channels (pixels: tx, ty, tz): {@code proc}, what the procedural layers
     * may move that bone, plus its largest stance plus its largest action at its largest gain, each clip read at that bone alone
     * (Clip.reach(bone)). Task 13 fix round 1 (I1) builds the cull reach bone by bone from it.
     */
    public static double[] reachPx(Family fam, String bone, double proc) {
        double[] stance = new double[6], action = new double[6], r = new double[3];
        for (Clip c : fam.stances().values()) max(stance, c.reach(bone), 1);
        for (Clip c : fam.attacks()) max(action, c.reach(bone), ATTACK_GAIN);
        for (Clip c : fam.wallAttacks()) max(action, c.reach(bone), ATTACK_GAIN);
        for (List<Clip> l : fam.hits().values()) for (Clip c : l) max(action, c.reach(bone), HIT_GAIN);
        for (List<Clip> l : fam.deaths().values()) for (Clip c : l) max(action, c.reach(bone), DEATH_GAIN);
        double[] over = new double[6];
        for (List<Clip> l : fam.hits().values()) for (Clip c : l) max(over, c.reach(bone), HIT_GAIN * HIT_OVER_GAIN * MAX_OVERLAYS);
        for (int c = 0; c < 3; c++) r[c] = proc + stance[3 + c] + action[3 + c] + over[3 + c];
        return r;
    }

    private static void max(double[] into, double[] reach, double gain) { for (int c = 0; c < 6; c++) into[c] = Math.max(into[c], reach[c] * gain); }

    /**
     * Moves ramp i toward the target it had last frame by dt / seconds, then records this frame's target, so a target that
     * changes at a sample time starts moving from that time at any frame rate. The first frame snaps to the target.
     */
    static void ramp(State st, int i, double target, double seconds, Frame f) { ramp(st, i, target, seconds, f, 0); }

    /**
     * As {@link #ramp(State, int, double, double, Frame)}, for a target that changed inside the frame: the last {@code after}
     * share of the frame already moves toward the new target.
     */
    static void ramp(State st, int i, double target, double seconds, Frame f, double after) {
        if (f.first) { st.w[i] = target; st.wTarget[i] = target; return; }
        st.w[i] = toward(toward(st.w[i], st.wTarget[i], f.dt * (1 - after) / seconds), target, f.dt * after / seconds);
        st.wTarget[i] = target;
    }

    /** The run weight's target at a limb speed: none at a walk (0.35 and under), all of it at 0.95 and over. */
    static double runTarget(double speed) { return MotionMath.clamp01((speed - RUN_FROM) / RUN_SPAN); }

    /**
     * Moves the run weight, at no more than 1 / RUN_BLEND a second, after its target. The limb speed is continuous, so it is
     * taken as linear from last frame's speed {@code s0} across the frame, and the target as piecewise linear between the
     * points where the speed crosses RUN_FROM and RUN_FROM + RUN_SPAN; each piece is chased exactly, so the weight is the same
     * at any frame rate. A unit that is not moving targets none.
     */
    static void runWeight(State st, boolean moving, double s0, Frame f) {
        double s1 = f.speed, rate = 1 / RUN_BLEND;
        if (f.first || !moving) { ramp(st, W_RUN, moving ? runTarget(s1) : 0, RUN_BLEND, f); return; }
        if (st.wTarget[W_RUN] == 0 && s0 > RUN_FROM) s0 = RUN_FROM;   // it was not moving last frame: its target was none
        double w = st.w[W_RUN], u = 0, g = runTarget(s0);
        double a = s1 != s0 ? (RUN_FROM - s0) / (s1 - s0) : -1, b = s1 != s0 ? (RUN_FROM + RUN_SPAN - s0) / (s1 - s0) : -1;
        for (int k = 0; k < 3; k++) {
            double next = k == 0 ? Math.min(a > 0 && a < 1 ? a : 1, b > 0 && b < 1 ? b : 1) : k == 1 ? Math.max(a > 0 && a < 1 ? a : 0, b > 0 && b < 1 ? b : 0) : 1;
            if (next <= u) continue;
            double gn = runTarget(s0 + (s1 - s0) * next);
            w = chase(w, g, gn, rate, f.dt * (next - u));
            u = next;
            g = gn;
        }
        st.w[W_RUN] = w;
        st.wTarget[W_RUN] = runTarget(s1);
    }

    /** Where w ends after dt chasing a target moving linearly from g0 to g1 at no more than rate a second. */
    static double chase(double w, double g0, double g1, double rate, double dt) {
        if (!(dt > 0)) return w;
        double s = (g1 - g0) / dt, gap = g0 - w;
        if (gap == 0 && Math.abs(s) <= rate) return g1;
        double dir = gap > 0 ? 1 : gap < 0 ? -1 : Math.signum(s), close = rate - dir * s;
        if (close <= 0) return w + dir * rate * dt;              // the target runs away at least as fast
        double caught = Math.abs(gap) / close;
        if (caught >= dt) return w + dir * rate * dt;
        if (Math.abs(s) <= rate) return g1;                      // caught, then tracks it
        return w + dir * rate * caught + Math.signum(s) * rate * (dt - caught);
    }

    static double toward(double raw, double goal, double step) { return raw < goal ? Math.min(goal, raw + step) : Math.max(goal, raw - step); }

    static double finite(double v) { return Double.isFinite(v) ? v : 0; }

    static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }
}
