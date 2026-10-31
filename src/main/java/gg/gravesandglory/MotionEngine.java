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
            TURN_BLEND = 0.3, LIFE_BLEND = 0.5, LOOK_BLEND = 0.4, CALM_BLEND = 0.35, SETTLE_BLEND = 0.5, PLANT_BLEND = 0.6;
    /** The limb speed above which a unit starts moving and below which it stops (hysteresis). */
    static final double MOVE_ON = 0.1, MOVE_OFF = 0.04;
    /** The limb speed a run starts at, and how much faster it is all run. */
    static final double RUN_FROM = 0.35, RUN_SPAN = 0.6;
    /**
     * An engine-made fade may move a channel at most this far in one 60 fps frame (degrees, pixels). This leaves room under
     * the 12 degree / 1.5 px pop limit for the procedural layers moving in the same frame.
     */
    static final double FADE_ROT = 6, FADE_PX = 0.8;
    static final double[] FADES = {0.08, 0.12, 0.18, 0.25, 0.35, 0.45};
    /** The largest gains an attack (seed amp) and a hit clip (weight scale times seed amp) can play at. */
    static final double ATTACK_GAIN = 1.1, HIT_GAIN = 1.35 * 1.1;
    /** Bounds on everything the procedural layers can add to one channel (degrees, pixels). */
    static final double PROC_ROT = 180, PROC_PX = 16;
    static final int ATTACK = 0, HIT = 1, DEATH = 2, MERGED = 3, MAX_LAYERS = 6, MAX_TRACKS = 6;
    /** The limb angle wraps on a whole number of strides, so the gait phase (limb angle times 0.6662) runs on across it. */
    static final double LIMB_WRAP = 100000 * MotionMath.TAU / 0.6662;
    /** Ramp slots in {@link State#w}. */
    static final int W_MOVE = 0, W_RUN = 1, W_COMBAT = 2, W_SHUFFLE = 3, W_BACK = 4, W_TURN = 5, W_LIFE = 6, W_LOOK = 7,
            W_CALM = 8, W_SETTLE = 9, W_PLANT = 10, WEIGHTS = 11;

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
    public record Family(String name, Map<String, Clip> stances, List<Clip> attacks, Map<String, List<Clip>> hits, Map<String, Clip> deaths) {
        public Family {
            Objects.requireNonNull(name, "name");
            stances = Map.copyOf(stances);
            attacks = List.copyOf(attacks);
            Map<String, List<Clip>> h = new HashMap<>();
            for (var e : hits.entrySet()) h.put(e.getKey(), List.copyOf(e.getValue()));
            hits = Map.copyOf(h);
            deaths = Map.copyOf(deaths);
        }
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
        double turnPhase, turnRate, breathPhase, breathRate, flutter, flutterRate, yawRate, yawRateTarget;
        final List<MotionStack.Layer> stances = new ArrayList<>(MAX_LAYERS + 1);
        final List<MotionStack.Track> tracks = new ArrayList<>(MAX_TRACKS + 1);
        int ids;
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
        final double[][] feet = new double[2][3], sol = new double[2][3];
        final double[] vec = new double[3], rootFull = new double[6], knee = new double[2];
    }

    /** What one frame's layers share: the sanitised input and the eased weights. */
    static final class Frame {
        double now, dt, speed, limbAngle, headYaw, headPitch, yawRate, aimPitch;
        boolean first, dead, enemy;
        MotionMath.Seed seed;
        /**
         * Eased weights: moving, running, combat, shield shuffle, backpedal, turning in place, life, look, settle and planted
         * feet; calm is raw. The planted-feet weight drops on death and eases back in on revive, so the feet are not
         * re-planted at full strength while the body rises.
         */
        double m, run, cw, sh, bp, tw, lw, lookW, settle, plant, calm;
        /** How much of the pose leg-keying actions own, and how much the aim stance does. */
        double bodyAction, aim;
    }

    /** One clip a track is playing, for tests and culling: its clip time and its effective share of the fold, times the track's gain. */
    record Playing(int id, Clip clip, double time, double gain) {}

    /** Poses one entity for this frame into {@code out}, clearing it first. */
    public static void pose(MotionInput in, State st, Family fam, Pose out) {
        out.clear();
        Frame f = st.frame;
        f.first = !st.started;
        f.now = Double.isFinite(in.time()) ? in.time() : st.started ? st.last : 0;
        f.dt = f.first ? 0 : MotionMath.clampDt(f.now - st.last);
        st.started = true;
        st.last = f.now;
        f.seed = in.seed();
        f.speed = clamp(finite(in.limbSpeed()), 0, 1.5);
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

        events(in, st, fam, f);
        boolean acting = !st.tracks.isEmpty() || !st.pulses.isEmpty();
        ramp(st, W_CALM, !moving && !f.enemy && !acting && !f.dead && w[W_TURN] == 0 ? 1 : 0, CALM_BLEND, f);
        f.calm = w[W_CALM];
        double settles = MotionStack.deathSettles(st);
        boolean played = f.dead && f.now >= settles;
        ramp(st, W_SETTLE, played ? 0 : 1, SETTLE_BLEND, f, played && !f.first ? MotionMath.clamp01((f.now - settles) / f.dt) : 0);
        f.settle = MotionMath.ease(w[W_SETTLE]);

        MotionBody.locomotion(f, st, out);
        MotionStack.stance(st, fam, f, stanceKey(stance, in.shieldUp(), f.enemy, moving, f.dead), out);
        MotionBody.life(f, st, out);
        MotionStack.action(st, f, out);
        MotionBody.react(f, st, out);
        MotionBody.look(f, st, out);
        MotionFeet.plant(f, st, out);
        MotionBody.secondary(f, st, out);
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

    /** Starts attacks, hit reactions and deaths from this frame's input. A new event is one not equal to the last seen. */
    static void events(MotionInput in, State st, Family fam, Frame f) {
        MotionInput.Attack attack = in.attack();
        if (attack != null && !attack.equals(st.attack)) {
            st.attack = attack;
            if (!f.dead && !fam.attacks().isEmpty()) {
                Clip clip = fam.attacks().get(Math.floorMod(attack.variant(), fam.attacks().size()));
                var tr = new MotionStack.Track(st.ids++, ATTACK, clip, null, 0, f.seed.amp(), attack.startTick() / (double) Clip.TICKS, f.seed.tempo());
                if (tr.time(f.now) < clip.length() - 0.05) MotionStack.begin(st, tr, f);
            }
        }
        MotionInput.Hit hit = in.hit();
        if (hit != null && !hit.equals(st.hit)) {
            st.hit = hit;
            if (hit.lethal()) st.lethal = hit;
            double at = hit.tick() / (double) Clip.TICKS, age = f.now - at;
            if (!f.dead && age >= -0.2 && age <= 0.6) {
                int weight = Math.max(0, Math.min(2, hit.weight()));
                double scale = (weight == 0 ? 0.7 : weight == 1 ? 1.0 : 1.35) * f.seed.amp();
                MotionBody.pulse(st, hit, at, scale);
                List<Clip> set = fam.hits().get(hit.blocked() ? "block" : zone(hit.zone()));
                if (set == null || set.isEmpty()) set = fam.hits().get("body");
                if (set != null && !set.isEmpty()) {
                    Clip clip = set.get(Math.floorMod((int) hit.tick(), set.size()));
                    var tr = new MotionStack.Track(st.ids++, HIT, clip, null, 0, scale, at, f.seed.tempo());
                    if (tr.time(f.now) < clip.length() - 0.05) MotionStack.begin(st, tr, f);
                }
            }
        }
        if (f.dead && !st.dead) {
            MotionStack.Track tr = death(st, fam, f, finite(in.deathSeconds()));
            if (tr != null) MotionStack.begin(st, tr, f);
        }
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
        Clip a = deathClip(fam, kind, from), b = deathClip(fam, kind, to);
        if (a == null && b == null) {
            for (String dir : abs <= 90 ? List.of("front", side, "back") : List.of("back", side, "front"))
                if ((a = deathClip(fam, kind, dir)) != null) break;
            if (a == null) a = fam.deaths().values().stream().min((x, y) -> x.name().compareTo(y.name())).orElse(null);
            if (a == null) return null;
            mix = 0;
        } else if (a == null) { a = b; b = null; mix = 0; }
        else if (b == null || b == a) { b = null; mix = 0; }
        return new MotionStack.Track(st.ids++, DEATH, a, b, mix, 1, f.now - Math.max(0, seconds), 1);
    }

    static Clip deathClip(Family fam, String kind, String dir) {
        Clip c = fam.deaths().get(kind + "/" + dir);
        if (c == null) c = fam.deaths().get("any/" + dir);
        if (c == null && dir.equals("front")) c = fam.deaths().getOrDefault(kind, fam.deaths().get("any"));
        return c;
    }

    /** The clips the tracks are playing at {@code now}, bottom first, each with its effective share of the fold. */
    static List<Playing> playing(State st, double now) { return MotionStack.playing(st, now); }

    /** Active tracks, hit pulses and stance layers: the state that grows with events. */
    static int load(State st) { return st.tracks.size() + st.pulses.size() + st.stances.size(); }

    /**
     * The largest |value| any bone's channel can take for this family, per channel: the procedural layers' bounds, plus the
     * largest stance, plus the largest action at its largest gain. Rotations are in degrees and translations in pixels. Task
     * 13 derives cull bounds from the translations.
     */
    public static double[] reach(Family fam) {
        double[] stance = new double[6], action = new double[6], r = new double[6];
        for (Clip c : fam.stances().values()) max(stance, c.reach(), 1);
        for (Clip c : fam.attacks()) max(action, c.reach(), ATTACK_GAIN);
        for (List<Clip> l : fam.hits().values()) for (Clip c : l) max(action, c.reach(), HIT_GAIN);
        for (Clip c : fam.deaths().values()) max(action, c.reach(), 1);
        for (int c = 0; c < 6; c++) r[c] = (c < 3 ? PROC_ROT : PROC_PX) + stance[c] + action[c];
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

    private static double toward(double raw, double goal, double step) { return raw < goal ? Math.min(goal, raw + step) : Math.max(goal, raw - step); }

    static double finite(double v) { return Double.isFinite(v) ? v : 0; }

    static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }
}
