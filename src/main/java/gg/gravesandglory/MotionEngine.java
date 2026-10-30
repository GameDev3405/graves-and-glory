package gg.gravesandglory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The layered troop motion engine (animation rework design section 6.1). Each frame, every bone's six channels are the sum
 * of six layers, applied in order: locomotion, stance, life, action, look and secondary. The feet are planted after look,
 * from the summed pose. Stances and actions are clips; the other layers are procedural ({@link MotionBody}). Every state
 * weight ramps toward its target at its own rate on render time and is shaped by {@link MotionMath#ease}. Pure.
 */
public final class MotionEngine {
    private MotionEngine() {}

    static final int RX = 0, RY = 1, RZ = 2, TX = 3, TY = 4, TZ = 5;
    /** Seconds each state weight takes to ramp fully on or off. */
    static final double STANCE_BLEND = 0.45, MOVE_BLEND = 0.3, RUN_BLEND = 0.5, COMBAT_BLEND = 0.6, GAIT_BLEND = 0.4,
            TURN_BLEND = 0.3, LIFE_BLEND = 0.5, LOOK_BLEND = 0.4, CALM_BLEND = 0.35;
    /**
     * An engine-made fade may move a channel at most this far in one 60 fps frame (degrees, pixels). This leaves room under
     * the 12 degree / 1.5 px pop limit for the procedural layers moving in the same frame.
     */
    static final double FADE_ROT = 6, FADE_PX = 0.8;
    static final double[] FADES = {0.08, 0.12, 0.18, 0.25, 0.35, 0.45};
    /** The largest gains an attack (seed amp) and a hit clip (weight scale times seed amp) can play at. */
    static final double ATTACK_GAIN = 1.1, HIT_GAIN = 1.35 * 1.1;
    /** Bounds on everything the procedural layers can add to one channel (degrees, pixels). */
    static final double PROC_ROT = 360, PROC_PX = 16;
    static final int ATTACK = 0, HIT = 1, DEATH = 2, MAX_LAYERS = 12, MAX_TRACKS = 12;

    /**
     * One weapon family's clips (design section 6.2), resolved from ClipLibrary.
     * <ul>
     * <li>stances by name: idle, carry, guard, brace, wall, shield, aim and rest. A missing stance falls back along
     * {@link #stanceChain}.</li>
     * <li>attacks by variant.</li>
     * <li>hits by zone: head, body, legs and block. A missing zone falls back to body.</li>
     * <li>deaths by "kind/direction", then "any/direction". The direction is front, back, left or right.</li>
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

    /** Per-entity engine state: weights, springs, the stance stack, the active actions, hit reactions and the novelty timer. */
    public static final class State {
        boolean started, moving, dead;
        double last;
        /** Linear ramps in [0, 1]; the engine uses their eased values. */
        double move, run, combat, shuffle, back, turn, life, look, calm;
        double turnPhase, breathPhase, flutter, yawRate;
        final List<Layer> stances = new ArrayList<>();
        final List<Track> tracks = new ArrayList<>();
        int ids;
        MotionInput.Attack attack;
        MotionInput.Hit hit, lethal;
        final List<MotionBody.Pulse> pulses = new ArrayList<>();
        final double[] react = new double[MotionBody.REACT], reactVel = new double[MotionBody.REACT];
        double lookYaw, lookYawVel, lookPitch, lookPitchVel;
        double capeX, capeXVel, capeY, capeYVel, crestX, crestXVel, crestY, crestYVel;
        double noveltyNext;
        long noveltyRng;
        /** Novelty idles started so far. */
        int novelties;
        MotionBody.Novelty novelty;
        final Frame frame = new Frame();
        final Map<String, double[]> buf = new HashMap<>(), acc = new HashMap<>(), one = new HashMap<>();
    }

    /** What one frame's layers share: the sanitised input and the eased weights. */
    static final class Frame {
        double now, dt, speed, limbAngle, headYaw, headPitch, yawRate, aimPitch;
        boolean first, dead, enemy;
        MotionMath.Seed seed;
        /** Eased weights: moving, running, combat, shield shuffle, backpedal, turning in place, life, look; calm is raw. */
        double m, run, cw, sh, bp, tw, lw, lookW, calm;
        /** How much of the pose leg-keying actions own, and how much the aim stance does. */
        double bodyAction, aim;
    }

    record Layer(String key, Clip clip, double[] raw) {}

    /** An action clip playing as an offset on the stance. For a death, a and b blend by mix. */
    static final class Track {
        final int id, kind;
        final Clip a, b;
        final double mix, gain, start, impact, tempo;
        final boolean legs;
        double x, fadeIn = FADES[0], fadeOut = FADES[0];
        boolean leaving;

        Track(int id, int kind, Clip a, Clip b, double mix, double gain, double start, double tempo) {
            this.id = id; this.kind = kind; this.a = a; this.b = b; this.mix = b == null ? 0 : mix; this.gain = gain;
            this.start = start; this.tempo = tempo; this.impact = a.impact() < 0 ? -1 : a.impact() / (double) Clip.TICKS;
            this.legs = keysLegs(a) || (b != null && keysLegs(b));
        }

        /** Clip time: real time up to the impact marker, so the impact lands on the server's tick, then the unit's tempo. */
        double time(double now) {
            double e = Math.max(0, now - start);
            if (impact < 0) return e * tempo;
            return e <= impact ? e : impact + (e - impact) * tempo;
        }

        double length() { return b == null ? a.length() : Math.max(a.length(), b.length()); }

        /** Clears out, then writes gain times the clip (or the blend) at {@code now} into it. */
        void sample(double now, Map<String, double[]> out, Map<String, double[]> tmp) {
            out.clear();
            double t = time(now);
            add(a, t, gain * (1 - mix), out, tmp);
            if (b != null && mix > 0) add(b, t, gain * mix, out, tmp);
        }

        private static void add(Clip clip, double t, double w, Map<String, double[]> out, Map<String, double[]> tmp) {
            tmp.clear();
            clip.sample(t, tmp);
            for (var e : tmp.entrySet()) {
                double[] o = out.computeIfAbsent(e.getKey(), k -> new double[6]), v = e.getValue();
                for (int c = 0; c < 6; c++) o[c] += w * v[c];
            }
        }
    }

    /** One clip a track is playing, for tests and culling: its clip time and its share of the track's gain. */
    record Playing(int id, Clip clip, double time, double gain) {}

    static boolean keysLegs(Clip c) {
        for (String b : c.bones()) if (b.endsWith("_leg") || b.endsWith("_shin")) return true;
        return false;
    }

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
        f.limbAngle = finite(in.limbAngle()) % 1.0e6;
        f.headYaw = clamp(finite(in.headYaw()), -85, 85);
        f.headPitch = clamp(finite(in.headPitch()), -60, 60);
        f.yawRate = clamp(finite(in.bodyYawRate()), -720, 720);
        f.aimPitch = clamp(finite(in.aimPitch()), -60, 60);
        f.dead = in.dead();
        f.enemy = in.enemyNear() && !f.dead;
        String stance = in.stance();
        if (st.dead && !f.dead) st.lethal = null;

        if (f.speed > 0.1) st.moving = true;
        else if (f.speed < 0.04) st.moving = false;
        boolean moving = st.moving && !f.dead;
        st.move = ramp(st.move, moving ? 1 : 0, MOVE_BLEND, f);
        st.run = ramp(st.run, moving ? MotionMath.clamp01((f.speed - 0.35) / 0.6) : 0, RUN_BLEND, f);
        st.combat = ramp(st.combat, f.enemy ? 1 : 0, COMBAT_BLEND, f);
        st.shuffle = ramp(st.shuffle, !f.dead && (in.shieldUp() || stance.equals("shield_move")) ? 1 : 0, GAIT_BLEND, f);
        st.back = ramp(st.back, !f.dead && stance.equals("fall_back") ? 1 : 0, GAIT_BLEND, f);
        st.turn = ramp(st.turn, !moving && !f.dead ? MotionMath.clamp01((Math.abs(f.yawRate) - 30) / 60) : 0, TURN_BLEND, f);
        st.life = ramp(st.life, f.dead ? 0 : 1, LIFE_BLEND, f);
        st.look = ramp(st.look, f.dead ? 0 : 1, LOOK_BLEND, f);
        f.m = MotionMath.ease(st.move); f.run = MotionMath.ease(st.run); f.cw = MotionMath.ease(st.combat);
        f.sh = MotionMath.ease(st.shuffle); f.bp = MotionMath.ease(st.back); f.tw = MotionMath.ease(st.turn);
        f.lw = MotionMath.ease(st.life); f.lookW = MotionMath.ease(st.look);

        events(in, st, fam, f);
        boolean acting = !st.tracks.isEmpty() || !st.pulses.isEmpty();
        st.calm = ramp(st.calm, !moving && !f.enemy && !acting && !f.dead && st.turn == 0 ? 1 : 0, CALM_BLEND, f);
        f.calm = st.calm;

        MotionBody.locomotion(f, st, out);
        stance(st, fam, f, stance, in.shieldUp(), out);
        MotionBody.life(f, st, out);
        action(st, f, out);
        MotionBody.react(f, st, out);
        MotionBody.look(f, st, out);
        MotionBody.plant(f, st, out);
        MotionBody.secondary(f, st, out);
        st.dead = f.dead;
    }

    /** The stance key for this frame's state (design section 6.2). */
    static String stanceKey(String stance, boolean shieldUp, boolean enemy, boolean moving, boolean dead) {
        if (dead) return "idle";
        switch (stance) {
            case "wall": return "wall";
            case "brace": return shieldUp ? "shield" : "brace";
            case "ranged_aim": return "aim";
            case "ranged_rest": return "rest";
            case "run": return "carry";
            default:
        }
        if (stance.equals("shield_move") || shieldUp) return "shield";
        return enemy ? "guard" : moving ? "carry" : "idle";
    }

    /** Where a family without a stance clip falls back to, nearest first. */
    static List<String> stanceChain(String key) {
        switch (key) {
            case "carry": return List.of("carry", "idle");
            case "guard": return List.of("guard", "carry", "idle");
            case "brace": return List.of("brace", "guard", "carry", "idle");
            case "wall": return List.of("wall", "shield", "brace", "guard", "carry", "idle");
            case "shield": return List.of("shield", "wall", "guard", "carry", "idle");
            case "aim": return List.of("aim", "guard", "carry", "idle");
            case "rest": return List.of("rest", "carry", "idle");
            default: return List.of("idle", "carry");
        }
    }

    /**
     * Stance: a stack of layers. A change pushes a new layer on top that ramps in over STANCE_BLEND with a cosine ease. The
     * layers below hold their weights, so only one weight moves at a time, and they drop out once the top is fully in. The
     * pose is the fold acc = acc (1 - w) + clip w from the bottom up. Looping stances play at the unit's tempo and phase.
     */
    static void stance(State st, Family fam, Frame f, String stance, boolean shieldUp, Pose out) {
        String key = stanceKey(stance, shieldUp, f.enemy, st.moving && !f.dead, f.dead);
        Clip clip = null;
        for (String k : stanceChain(key)) if ((clip = fam.stances().get(k)) != null) break;
        Layer top = st.stances.isEmpty() ? null : st.stances.get(st.stances.size() - 1);
        if (top == null || top.clip() != clip) {
            st.stances.add(top = new Layer(key, clip, new double[]{f.first ? 1 : 0}));
            if (st.stances.size() > MAX_LAYERS) st.stances.remove(0);
        }
        top.raw()[0] = Math.min(1, top.raw()[0] + f.dt / STANCE_BLEND);
        if (top.raw()[0] >= 1) while (st.stances.size() > 1) st.stances.remove(0);

        Map<String, double[]> acc = st.acc;
        acc.clear();
        double aim = 0;
        for (Layer l : st.stances) {
            double w = MotionMath.ease(l.raw()[0]);
            aim = aim * (1 - w) + (l.key().equals("aim") ? w : 0);
            for (double[] v : acc.values()) for (int c = 0; c < 6; c++) v[c] *= 1 - w;
            if (l.clip() == null || w == 0) continue;
            st.buf.clear();
            Clip c = l.clip();
            c.sample(c.loop() ? f.now * f.seed.tempo() + f.seed.phase() / MotionMath.TAU * c.length() : c.length(), st.buf);
            for (var e : st.buf.entrySet()) {
                double[] o = acc.computeIfAbsent(e.getKey(), k -> new double[6]), v = e.getValue();
                for (int ch = 0; ch < 6; ch++) o[ch] += w * v[ch];
            }
        }
        out.add(acc, 1);
        f.aim = aim;
        out.add("right_arm", RX, f.aimPitch * aim);
        out.add("left_arm", RX, f.aimPitch * aim);
    }

    /** Starts attacks, hit reactions and deaths from this frame's input. A new event is one not equal to the last seen. */
    static void events(MotionInput in, State st, Family fam, Frame f) {
        MotionInput.Attack attack = in.attack();
        if (attack != null && !attack.equals(st.attack)) {
            st.attack = attack;
            if (!f.dead && !fam.attacks().isEmpty()) {
                int n = fam.attacks().size();
                Clip clip = fam.attacks().get(Math.floorMod(attack.variant() + (int) (f.seed.variantBias() >>> 40), n));
                Track tr = new Track(st.ids++, ATTACK, clip, null, 0, f.seed.amp(), attack.startTick() / (double) Clip.TICKS, f.seed.tempo());
                if (tr.time(f.now) < clip.length() - 0.05) begin(st, tr, f);
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
                    Clip clip = set.get(Math.floorMod((int) (hit.tick() ^ (f.seed.variantBias() >>> 32)), set.size()));
                    Track tr = new Track(st.ids++, HIT, clip, null, 0, scale, at, f.seed.tempo());
                    if (tr.time(f.now) < clip.length() - 0.05) begin(st, tr, f);
                }
            }
        }
        if (f.dead && !st.dead) {
            Track tr = death(st, fam, f, finite(in.deathSeconds()));
            if (tr != null) begin(st, tr, f);
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
    static Track death(State st, Family fam, Frame f, double seconds) {
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
        return new Track(st.ids++, DEATH, a, b, mix, 1, f.now - Math.max(0, seconds), 1);
    }

    static Clip deathClip(Family fam, String kind, String dir) {
        Clip c = fam.deaths().get(kind + "/" + dir);
        if (c == null) c = fam.deaths().get("any/" + dir);
        if (c == null && dir.equals("front")) c = fam.deaths().getOrDefault(kind, fam.deaths().get("any"));
        return c;
    }

    /** Pushes a track on top. The tracks below hold their weights; the new one fades in as fast as the pop budget allows. */
    static void begin(State st, Track tr, Frame f) {
        if (f.first) tr.x = 1;
        else tr.fadeIn = fadeTime(st, tr, f.now, st.tracks.size(), true);
        st.tracks.add(tr);
        if (st.tracks.size() > MAX_TRACKS) st.tracks.remove(0);
    }

    /**
     * The shortest fade in FADES whose per-frame weight change, times the gap between the track and the fold of the tracks
     * below it at each coming 60 fps frame, stays within FADE_ROT and FADE_PX. A clip that keys the legs also hands the feet
     * from the planted solve to the clip, so its gap counts that too. The longest fade is the fallback.
     */
    static double fadeTime(State st, Track tr, double now, int below, boolean in) {
        int frames = (int) Math.ceil(60 * FADES[FADES.length - 1]) + 1;
        double[] rot = new double[frames + 1], px = new double[frames + 1];
        Map<String, double[]> mine = new HashMap<>(), under = new HashMap<>(), tmp = new HashMap<>(), one = new HashMap<>();
        for (int k = 0; k <= frames; k++) {
            double t = now + k / 60.0, r = 0, p = 0;
            tr.sample(t, mine, tmp);
            fold(st.tracks, below, t, under, one, tmp);
            for (var e : mine.entrySet()) {
                double[] u = under.get(e.getKey()), v = e.getValue();
                for (int c = 0; c < 6; c++) {
                    double gap = Math.abs(v[c] - (u == null ? 0 : u[c]));
                    if (c < 3) r = Math.max(r, gap); else p = Math.max(p, gap);
                }
            }
            for (var e : under.entrySet()) if (!mine.containsKey(e.getKey())) {
                double[] u = e.getValue();
                for (int c = 0; c < 6; c++) if (c < 3) r = Math.max(r, Math.abs(u[c])); else p = Math.max(p, Math.abs(u[c]));
            }
            if (tr.legs) { r += 20; p += 1; }
            rot[k] = r;
            px[k] = p;
        }
        double x0 = in ? 0 : tr.x;
        for (double fade : FADES) {
            boolean ok = true;
            double prev = MotionMath.ease(x0);
            for (int k = 1; k <= frames && ok; k++) {
                double x = in ? Math.min(1, k / (60 * fade)) : Math.max(0, x0 - k / (60 * fade)), w = MotionMath.ease(x), dw = Math.abs(w - prev);
                prev = w;
                ok = dw * rot[k] <= FADE_ROT && dw * px[k] <= FADE_PX;
            }
            if (ok) return fade;
        }
        return FADES[FADES.length - 1];
    }

    /** The fold of tracks[0, count) at time t into out. */
    static void fold(List<Track> tracks, int count, double t, Map<String, double[]> out, Map<String, double[]> one, Map<String, double[]> tmp) {
        out.clear();
        for (int i = 0; i < count; i++) {
            Track tr = tracks.get(i);
            double w = MotionMath.ease(tr.x);
            for (double[] v : out.values()) for (int c = 0; c < 6; c++) v[c] *= 1 - w;
            if (w == 0) continue;
            tr.sample(t, one, tmp);
            for (var e : one.entrySet()) {
                double[] o = out.computeIfAbsent(e.getKey(), k -> new double[6]), v = e.getValue();
                for (int c = 0; c < 6; c++) o[c] += w * v[c];
            }
        }
    }

    /**
     * Action: the tracks' fold, added to the stance as an offset. Only the top track changes weight. It rises until it covers
     * the ones below, which then drop. Once its clip ends (for a death, once the unit lives again) it leaves.
     */
    static void action(State st, Frame f, Pose out) {
        List<Track> tracks = st.tracks;
        if (!tracks.isEmpty()) {
            Track top = tracks.get(tracks.size() - 1);
            if (!top.leaving && (top.kind == DEATH ? !f.dead : top.time(f.now) >= top.length())) {
                top.leaving = true;
                top.fadeOut = fadeTime(st, top, f.now, tracks.size() - 1, false);
            }
            if (top.leaving) {
                top.x = Math.max(0, top.x - f.dt / top.fadeOut);
                if (top.x <= 0) tracks.remove(tracks.size() - 1);
            } else {
                top.x = Math.min(1, top.x + f.dt / top.fadeIn);
                if (top.x >= 1) while (tracks.size() > 1) tracks.remove(0);
            }
        }
        double legs = 0;
        for (Track tr : tracks) {
            double w = MotionMath.ease(tr.x);
            legs = legs * (1 - w) + (tr.legs ? w : 0);
        }
        f.bodyAction = legs;
        fold(tracks, tracks.size(), f.now, st.acc, st.one, st.buf);
        out.add(st.acc, 1);
    }

    /** The clips the tracks are playing at {@code now}, bottom first, each with its share of the track's gain. */
    static List<Playing> playing(State st, double now) {
        List<Playing> list = new ArrayList<>();
        for (Track tr : st.tracks) {
            double t = tr.time(now);
            list.add(new Playing(tr.id * 2, tr.a, t, tr.gain * (1 - tr.mix)));
            if (tr.b != null) list.add(new Playing(tr.id * 2 + 1, tr.b, t, tr.gain * tr.mix));
        }
        return list;
    }

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

    /** Moves a linear weight toward its target by dt / seconds. The first frame snaps to it. */
    static double ramp(double raw, double target, double seconds, Frame f) {
        if (f.first) return target;
        double step = f.dt / seconds;
        return raw < target ? Math.min(target, raw + step) : Math.max(target, raw - step);
    }

    static double finite(double v) { return Double.isFinite(v) ? v : 0; }

    static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }
}
