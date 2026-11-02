package gg.gravesandglory;

import static gg.gravesandglory.MotionEngine.RX;

import java.util.ArrayList;
import java.util.List;

/**
 * The clip layers of {@link MotionEngine}: the stance stack and the action tracks (animation rework design section 6.1).
 * Both are stacks where only the top weight moves. A change pushes a new entry on top; it ramps in with a cosine ease and
 * the entries below drop once it covers them. The fold is acc = acc (1 - w) + clip w from the bottom up, so entry i's
 * effective share is w_i times (1 - w_j) for every j above it. A stack that grows past its bound merges its two bottom
 * entries into one with the same fold, so no weighted entry is ever dropped. Pure.
 */
final class MotionStack {
    private MotionStack() {}

    /** The most distinct clips a merged stance layer can hold: one per stance name. */
    static final int LAYER_CLIPS = 8;
    /** How many 60 fps frames ahead a fade checks: the longest fade, plus one. */
    static final int FADE_FRAMES = (int) Math.ceil(60 * MotionEngine.FADES[MotionEngine.FADES.length - 1]) + 1;

    // ---- stance ----

    /** A stance layer: a weighted mix of clips (one, or several once merged), its share of the aim stance and its ramp. */
    static final class Layer {
        final Clip[] clips = new Clip[LAYER_CLIPS];
        final double[] coef = new double[LAYER_CLIPS];
        int n;
        double aim, raw;
        /** P1: how long this layer takes to ramp in (s): STANCE_BLEND, or longer where the velocity bound (stanceFade) needs it. */
        double fade = MotionEngine.STANCE_BLEND;
        boolean fresh;

        Clip clip() { return n == 0 ? null : clips[0]; }

        void put(Clip c, double w) {
            if (c == null || w == 0) return;
            for (int i = 0; i < n; i++) if (clips[i] == c) { coef[i] += w; return; }
            if (n < LAYER_CLIPS) { clips[n] = c; coef[n++] = w; }
        }
    }

    /**
     * Stance: a stack of layers. A change of clip, or of whether the arms aim, pushes a new layer that ramps in over
     * STANCE_BLEND from the next frame. Looping stances play at the unit's tempo and phase; held stances at their last key.
     * The aim share raises both arms by the aim pitch, whichever clip the aim stance fell back to.
     */
    static void stance(MotionEngine.State st, MotionEngine.Family fam, MotionEngine.Frame f, String key, Pose out) {
        Clip clip = null;
        for (String k : MotionEngine.stanceChain(key)) if ((clip = fam.stances().get(k)) != null) break;
        double aim = key.equals("aim") ? 1 : 0;
        List<Layer> layers = st.stances;
        Layer top = layers.isEmpty() ? null : layers.get(layers.size() - 1);
        if (top == null || top.n > 1 || top.clip() != clip || top.aim != aim) {
            double fade = f.first ? MotionEngine.STANCE_BLEND : Math.max(MotionEngine.STANCE_BLEND, stanceFade(st, fam, f, clip, aim));
            top = new Layer();
            top.fade = fade;
            top.put(clip, 1);
            top.aim = aim;
            top.raw = f.first ? 1 : 0;
            top.fresh = !f.first;
            layers.add(top);
            if (layers.size() > MotionEngine.MAX_LAYERS) mergeLayers(layers);
        }
        if (!top.fresh) top.raw = Math.min(1, top.raw + f.dt / top.fade);
        top.fresh = false;
        if (top.raw >= 1) while (layers.size() > 1) layers.remove(0);

        Clip[] mix = st.mixClips;
        double[] coef = st.mixCoef;
        int n = 0;
        double carry = 1, aimShare = 0;
        for (int i = layers.size() - 1; i >= 0 && carry > 0; i--) {
            Layer l = layers.get(i);
            double w = MotionMath.ease(l.raw), e = w * carry;
            carry *= 1 - w;
            aimShare += e * l.aim;
            for (int c = 0; c < l.n; c++) {
                int at = 0;
                while (at < n && mix[at] != l.clips[c]) at++;
                if (at == n) { if (n == mix.length) continue; mix[n] = l.clips[c]; coef[n++] = 0; }
                coef[at] += e * l.coef[c];
            }
        }
        for (int i = 0; i < n; i++) {
            Clip c = mix[i];
            c.add(stanceTime(c, f), out, coef[i], st.tmp);
            mix[i] = null;
        }
        f.aim = aimShare;
        out.add("right_arm", RX, f.aimPitch * aimShare);
        out.add("left_arm", RX, f.aimPitch * aimShare);
    }

    /** A stance clip's time at {@code now}: looping stances at the unit's tempo and phase, held ones at their last key. */
    static double stanceTime(Clip c, MotionEngine.Frame f) {
        return c.loop() ? f.now * f.seed.tempo() + f.seed.phase() / MotionMath.TAU * c.length() : c.length();
    }

    /**
     * P1: the cross-fade time of a new stance layer (clip, aim share): the velocity bound (velocityFade) from the stance fold as
     * it stands (aim raise included) to the new layer, at least STANCE_MIN and at most INTERRUPT_MAX seconds.
     */
    static double stanceFade(MotionEngine.State st, MotionEngine.Family fam, MotionEngine.Frame f, Clip clip, double aim) {
        Pose old = st.stanceOld, neu = st.stanceNew;
        old.clear(); neu.clear();
        double carry = 1, aimShare = 0;
        for (int i = st.stances.size() - 1; i >= 0 && carry > 0; i--) {
            Layer l = st.stances.get(i);
            double w = MotionMath.ease(l.raw), e = w * carry;
            carry *= 1 - w;
            aimShare += e * l.aim;
            for (int c = 0; c < l.n; c++) l.clips[c].add(stanceTime(l.clips[c], f), old, e * l.coef[c], st.tmp);
        }
        old.add("right_arm", RX, f.aimPitch * aimShare);
        old.add("left_arm", RX, f.aimPitch * aimShare);
        if (clip != null) clip.add(stanceTime(clip, f), neu, 1, st.tmp);
        neu.add("right_arm", RX, f.aimPitch * aim);
        neu.add("left_arm", RX, f.aimPitch * aim);
        double t = velocityFade(fam.peak(), old, neu, clip, 0, null, clip != null && clip.loop() ? f.seed.tempo() : 1, MotionEngine.STANCE_MIN);
        return Math.max(t, accelFade(old, neu));
    }

    /**
     * Fix 2 (Task 16): the fade that keeps a stance cross-fade's second difference within STANCE_ACC. The cosine ease's largest
     * curvature is pi^2 / 2, so a fade of T seconds over gaps G (rotation, summed over every bone's angles) bends the pose by
     * G (pi^2 / 2) (0.05 / T)^2 a tick squared: T = 0.05 pi sqrt(G / (2 STANCE_ACC)), at most STANCE_LONG.
     */
    static double accelFade(Pose from, Pose to) {
        double g = 0;
        // each pose lists its bones (a cleared pose keeps its rows at zero), so the larger pass's sum covers both
        for (int pass = 0; pass < 2; pass++) {
            Pose p = pass == 0 ? to : from, q = pass == 0 ? from : to;
            double sum = 0;
            for (int i = 0, n = p.size(); i < n; i++) {
                String bone = p.name(i);
                double[] v = p.row(i);
                for (int c = 0; c < 3; c++) sum += Math.abs(v[c] - q.get(bone, c));
            }
            g = Math.max(g, sum);
        }
        return Math.min(MotionEngine.STANCE_LONG, 0.05 * Math.PI * Math.sqrt(g / (2 * MotionEngine.STANCE_ACC)));
    }

    /**
     * P1 (Task 16, R101): the fade from pose {@code from} to {@code to} that keeps every bone's step within INTERRUPT_HEADROOM
     * times the family's peak. The cosine ease's steepest slope is pi / 2, so a fade of T seconds over a gap G adds at most
     * G (pi / 2) (0.05 / T) a tick on top of the incoming clip's own step Vn (its peak over its first PEAK_WINDOW, times
     * scale): T = pi G 0.05 / (2 (1.2 peak - Vn)) for each bone, rotation (summed over its angles, as MotionMath.boneStep) and
     * translation (largest channel) apart; the largest over bones, clamped to [min, INTERRUPT_MAX]. Where 1.2 peak - Vn is at
     * most 0.1 peak there is no headroom, so INTERRUPT_MAX. The incoming pose plays a (and b by mix, a death's blend).
     */
    static double velocityFade(double[] peak, Pose from, Pose to, Clip a, double mix, Clip b, double scale, double min) {
        double t = min;
        for (int pass = 0; pass < 2; pass++) {
            Pose p = pass == 0 ? to : from, q = pass == 0 ? from : to;
            for (int i = 0, n = p.size(); i < n; i++) {
                String bone = p.name(i);
                double[] v = p.row(i);
                double g = 0, gp = 0;
                for (int c = 0; c < 3; c++) g += Math.abs(v[c] - q.get(bone, c));
                for (int c = 3; c < 6; c++) gp = Math.max(gp, Math.abs(v[c] - q.get(bone, c)));
                double[] vn = {0, 0};
                if (a != null) { double[] s = a.peakStep(bone, true); vn[0] = s[0] * (1 - mix); vn[1] = s[1] * (1 - mix); }
                if (b != null) { double[] s = b.peakStep(bone, true); vn[0] = Math.max(vn[0], s[0] * mix); vn[1] = Math.max(vn[1], s[1] * mix); }
                for (int k = 0; k < 2; k++) {
                    double gap = k == 0 ? g : gp;
                    if (gap <= 1e-9) continue;
                    double head = MotionEngine.INTERRUPT_HEADROOM * peak[k] - vn[k] * scale;
                    double tb = head <= 0.1 * peak[k] ? MotionEngine.INTERRUPT_MAX : Math.PI * gap * 0.05 / (2 * head);
                    t = Math.max(t, Math.min(MotionEngine.INTERRUPT_MAX, tb));
                }
            }
        }
        return t;
    }

    /** Folds the bottom two layers into one with the same contribution: L0 w0 (1 - w1) + L1 w1, fully weighted. */
    static void mergeLayers(List<Layer> layers) {
        Layer l0 = layers.get(0), l1 = layers.get(1), m = new Layer();
        double w0 = MotionMath.ease(l0.raw), w1 = MotionMath.ease(l1.raw), k0 = w0 * (1 - w1);
        for (int i = 0; i < l0.n; i++) m.put(l0.clips[i], l0.coef[i] * k0);
        for (int i = 0; i < l1.n; i++) m.put(l1.clips[i], l1.coef[i] * w1);
        m.aim = l0.aim * k0 + l1.aim * w1;
        m.raw = 1;
        layers.set(0, m);
        layers.remove(1);
    }

    // ---- action tracks ----

    /** An action clip playing as an offset on the stance. For a death, a and b blend by mix. A merged track holds a frozen pose. */
    static final class Track {
        final int id, kind;
        final Clip a, b;
        final double mix, gain, start, impact, tempo;
        /** How much of this track keys the legs (1 if its clip does, a blend once merged). */
        final double legShare;
        double x, fadeIn = MotionEngine.FADES[0], fadeOut = MotionEngine.FADES[0];
        /** When a leaving track's weight reaches 0 (exact: its weight falls linearly from where it started to leave). */
        double gone = Double.POSITIVE_INFINITY;
        boolean leaving, fresh;
        /** P1: a FROM track, the merged pose plus State.fromVel coasting to a stop over FROM_TD from start. */
        final boolean inertial;

        Track(int id, int kind, Clip a, Clip b, double mix, double gain, double start, double tempo) {
            this(id, kind, a, b, mix, gain, start, tempo, a.impact() < 0 ? -1 : a.impact() / (double) Clip.TICKS);
        }

        /** As above with the time (seconds) the tempo starts to apply: a hit's snap plays in real time and only its recovery takes the weight's tempo. */
        Track(int id, int kind, Clip a, Clip b, double mix, double gain, double start, double tempo, double impact) {
            this.id = id; this.kind = kind; this.a = a; this.b = b; this.mix = b == null ? 0 : mix; this.gain = gain;
            this.start = start; this.tempo = tempo; this.impact = impact;
            this.legShare = keysLegs(a) || (b != null && keysLegs(b)) ? 1 : 0;
            this.inertial = false;
        }

        /** A merged track: the frozen fold in State.merged, fully weighted. */
        Track(int id, double legShare) {
            this(id, legShare, 0, false);
        }

        /** P1: a FROM track from {@code start}: State.merged (the action fold at the interrupt) coasting on State.fromVel. */
        Track(int id, double legShare, double start, boolean inertial) {
            this.id = id; this.kind = MotionEngine.MERGED; this.a = null; this.b = null; this.mix = 0; this.gain = 1;
            this.start = start; this.impact = -1; this.tempo = 1; this.legShare = legShare; this.x = 1; this.inertial = inertial;
        }

        /** Clip time: real time up to the impact marker, so the impact lands on the server's tick, then the unit's tempo. */
        double time(double now) {
            double e = Math.max(0, now - start);
            if (impact < 0) return e * tempo;
            return e <= impact ? e : impact + (e - impact) * tempo;
        }

        double length() { return a == null ? 0 : b == null ? a.length() : Math.max(a.length(), b.length()); }

        /** When the clip time reaches the clip's length: time() inverted. */
        double end() {
            double l = length(), e = impact < 0 ? l / tempo : l <= impact ? l : impact + (l - impact) / tempo;
            return start + e;
        }

        /**
         * b's share at clip time t. A death's blend starts at the blow's mix and eases (smoothstep over the clip) to the nearer clip
         * (a below 0.5, else b), so the corpse lies in one authored end pose: each end pose lies on the ground, a blend of two lying
         * poses props the body up and sinks it (Task 16 item 5: up to 11 px under the ground at 45 degrees). Other tracks keep mix.
         */
        double mix(double t) {
            if (b == null || kind != MotionEngine.DEATH) return mix;
            double l = length(), u = l <= 0 ? 1 : Math.min(1, Math.max(0, t / l)), rest = mix >= 0.5 ? 1 : 0;
            return mix + (rest - mix) * u * u * (3 - 2 * u);
        }
    }

    static boolean keysLegs(Clip c) {
        for (String b : c.bones()) if (b.endsWith("_leg") || b.endsWith("_shin")) return true;
        return false;
    }

    /** Adds w times track tr's pose at {@code now} (its gain included) into out. */
    static void add(MotionEngine.State st, Track tr, double now, double w, Pose out) {
        if (w == 0) return;
        if (tr.kind == MotionEngine.MERGED) {
            out.add(st.merged, w);
            // before its start (begin's slope sample of a fold that holds it) the coast runs back smoothly along the same curve
            if (tr.inertial) out.add(st.fromVel, w * MotionEngine.FROM_TD * (1 - Math.exp(-(now - tr.start) / MotionEngine.FROM_TD)));
            return;
        }
        double t = tr.time(now), m = tr.mix(t);
        tr.a.add(t, out, w * tr.gain * (1 - m), st.tmp);
        if (tr.b != null) tr.b.add(t, out, w * tr.gain * m, st.tmp);
        // K1b: a death's gain pivots on its last frame, w [g clip(t) + (1 - g) clip(end)], so the corpse lies exactly as authored
        if (tr.kind == MotionEngine.DEATH && tr.gain != 1) {
            tr.a.endPose(out, w * (1 - tr.gain) * (1 - m));
            if (tr.b != null) tr.b.endPose(out, w * (1 - tr.gain) * m);
        }
    }

    /** Effective shares of tracks[0, count) into shares: w_i times (1 - w_j) for each j above i. */
    static void shares(List<Track> tracks, int count, double[] shares) {
        double carry = 1;
        for (int i = count - 1; i >= 0; i--) {
            double w = MotionMath.ease(tracks.get(i).x);
            shares[i] = w * carry;
            carry *= 1 - w;
        }
    }

    /** The fold of tracks[0, count) at time t, added into out. Returns how much of it keys the legs. */
    static double fold(MotionEngine.State st, int count, double t, Pose out) {
        List<Track> tracks = st.tracks;
        double[] e = st.shares;
        shares(tracks, count, e);
        double legs = 0;
        for (int i = 0; i < count; i++) {
            Track tr = tracks.get(i);
            add(st, tr, t, e[i], out);
            legs += e[i] * tr.legShare;
        }
        return legs;
    }

    /**
     * Pushes a track on top. A late join (first frame) shows it at once. Otherwise (P1, Task 16) the tracks playing are replaced
     * by one FROM track: the action fold as it stands at {@code now} (the weights advanced to now as action() would, the clips
     * evaluated at now, so it is the same at any frame rate), each angle wrapped to within 180 degrees of the new clip's start
     * (an Euler angle 360 off is the same rotation), coasting on its velocity (the fold's slope over FROM_H) to a stop over
     * FROM_TD, so the blend starts from the evaluated pose with no break in speed. The new track fades in over the longer of
     * fadeTime (the pop budget) and velocityFade (R101). Past MAX_TRACKS the bottom two merge, so a weighted track is never dropped.
     */
    static void begin(MotionEngine.State st, MotionEngine.Family fam, Track tr, MotionEngine.Frame f) {
        if (f.first) tr.x = 1;
        else {
            advance(st, f);
            Pose mine = st.stanceNew;
            if (!st.tracks.isEmpty()) {
                Pose p0 = st.mergeTmp, v = st.fromVel;
                p0.clear(); v.clear();
                int n = st.tracks.size();
                double legs = fold(st, n, f.now, p0);
                fold(st, n, f.now - MotionEngine.FROM_H, v);
                for (int i = 0, m = p0.size(); i < m; i++) v.get(p0.name(i));
                for (int i = 0, m = v.size(); i < m; i++) {
                    double[] r = v.row(i);
                    for (int c = 0; c < 6; c++) r[c] = (p0.get(v.name(i), c) - r[c]) / MotionEngine.FROM_H;
                }
                mine.clear();
                add(st, tr, f.now, 1, mine);
                for (int i = 0, m = p0.size(); i < m; i++) {
                    double[] r = p0.row(i);
                    for (int c = 0; c < 3; c++) r[c] += 360 * Math.rint((mine.get(p0.name(i), c) - r[c]) / 360);
                }
                st.merged.clear();
                st.merged.add(p0, 1);
                st.tracks.clear();
                st.tracks.add(new Track(st.ids++, legs, f.now, true));
            }
            double pops = fadeTime(st, tr, f.now, st.tracks.size(), true);
            Pose from = st.stanceOld;
            from.clear(); mine.clear();
            fold(st, st.tracks.size(), f.now, from);
            add(st, tr, f.now, 1, mine);
            double scale = tr.gain * Math.max(1, tr.tempo);
            tr.fadeIn = Math.max(pops, velocityFade(fam.peak(), from, mine, tr.a, tr.mix(tr.time(f.now)), tr.b, scale, MotionEngine.INTERRUPT_MIN));
            tr.fresh = true;
        }
        st.tracks.add(tr);
        if (st.tracks.size() > MotionEngine.MAX_TRACKS) mergeTracks(st, f.now);
    }

    /** Folds the bottom two tracks into one merged track holding their fold at {@code now}, frozen. */
    static void mergeTracks(MotionEngine.State st, double now) {
        List<Track> tracks = st.tracks;
        Track t0 = tracks.get(0), t1 = tracks.get(1);
        double w0 = MotionMath.ease(t0.x), w1 = MotionMath.ease(t1.x), k0 = w0 * (1 - w1);
        Pose tmp = st.mergeTmp;
        tmp.clear();
        add(st, t0, now, k0, tmp);
        add(st, t1, now, w1, tmp);
        st.merged.clear();
        st.merged.add(tmp, 1);
        tracks.set(0, new Track(st.ids++, t0.legShare * k0 + t1.legShare * w1));
        tracks.remove(1);
    }

    /**
     * The shortest fade in FADES whose per-frame weight change, times the gap between the track and the fold of the tracks
     * below it at each coming 60 fps frame, stays within FADE_ROT and FADE_PX. A track that changes how much the clips own
     * the legs also moves the planted-feet correction by that change times what the solve corrected last frame
     * (State.plantRot and plantPx), so its gap counts that too. The longest fade is the fallback.
     */
    static double fadeTime(MotionEngine.State st, Track tr, double t0, int below, boolean in) {
        int frames = FADE_FRAMES;
        double[] rot = st.fadeRot, px = st.fadePx;
        Pose mine = st.fadeMine, under = st.fadeUnder;
        double legs = 0;
        for (int k = 0; k <= frames; k++) {
            double t = t0 + k / 60.0, r = 0, p = 0;
            mine.clear();
            under.clear();
            add(st, tr, t, 1, mine);
            legs = fold(st, below, t, under);
            r = Math.max(r, gap(mine, under, true));
            p = Math.max(p, gap(mine, under, false));
            r = Math.max(r, gap(under, mine, true));
            p = Math.max(p, gap(under, mine, false));
            double hand = Math.abs(tr.legShare - legs);
            rot[k] = r + hand * st.plantRot;
            px[k] = p + hand * st.plantPx;
        }
        double x0 = in ? 0 : tr.x;
        for (double fade : MotionEngine.FADES) {
            boolean ok = true;
            double prev = MotionMath.ease(x0);
            for (int k = 1; k <= frames && ok; k++) {
                double x = in ? Math.min(1, k / (60 * fade)) : Math.max(0, x0 - k / (60 * fade)), w = MotionMath.ease(x), dw = Math.abs(w - prev);
                prev = w;
                ok = dw * rot[k] <= MotionEngine.FADE_ROT && dw * px[k] <= MotionEngine.FADE_PX;
            }
            if (ok) return fade;
        }
        return MotionEngine.FADES[MotionEngine.FADES.length - 1];
    }

    /** The largest |a - b| over a's bones, on the rotation or the translation channels. */
    static double gap(Pose a, Pose b, boolean rotation) {
        double g = 0;
        int from = rotation ? 0 : 3;
        for (int i = 0, n = a.size(); i < n; i++) {
            double[] v = a.row(i);
            String name = a.name(i);
            for (int c = from; c < from + 3; c++) g = Math.max(g, Math.abs(v[c] - b.get(name, c)));
        }
        return g;
    }

    /**
     * Action: the tracks' fold, added to the stance as an offset. Only the top track changes weight. It rises until it covers
     * the ones below, which then drop. Once its clip ends (for a death, once the unit lives again; for a merged track, once
     * it is the top) it leaves. The end is back-dated to when the clip actually ended, so a fade-out runs the same at any
     * frame rate.
     */
    static void action(MotionEngine.State st, MotionEngine.Frame f, Pose out) {
        advance(st, f);
        f.bodyAction = fold(st, st.tracks.size(), f.now, out);
        overlays(st, f, out);
    }

    /** Whether an attack is playing on top of the stack and not yet leaving (a hit now becomes an overlay on it). */
    static boolean attacking(MotionEngine.State st) {
        if (st.tracks.isEmpty()) return false;
        Track top = st.tracks.get(st.tracks.size() - 1);
        return top.kind == MotionEngine.ATTACK && !top.leaving;
    }

    /** Fix r3 (item 3): starts a hit as an additive overlay (MotionEngine.HIT_OVER_*); the tracks below keep playing untouched. */
    static void overlay(MotionEngine.State st, Track tr) {
        tr.x = 0;
        tr.fadeIn = MotionEngine.HIT_OVER_IN;
        tr.fadeOut = MotionEngine.HIT_OVER_OUT;
        st.overlays.add(tr);
        if (st.overlays.size() > MotionEngine.MAX_OVERLAYS) st.overlays.remove(0);
    }

    /**
     * Steps each overlay's weight (rising over fadeIn from its start, falling over fadeOut from its clip's end; a dead unit's
     * fall from now) and adds ease(weight) times its pose into out. Time based, so the same at any frame rate.
     */
    static void overlays(MotionEngine.State st, MotionEngine.Frame f, Pose out) {
        for (int i = 0; i < st.overlays.size(); i++) {
            Track tr = st.overlays.get(i);
            if (f.dead && !tr.leaving) { tr.leaving = true; tr.gone = f.now; }
            double up = MotionEngine.clamp((f.now - tr.start) / tr.fadeIn, 0, 1);
            double down = tr.leaving ? 1 - (f.now - tr.gone) / tr.fadeOut : 1 - (f.now - tr.end()) / tr.fadeOut;
            tr.x = Math.min(up, MotionEngine.clamp(down, 0, 1));
            if (down <= 0) { st.overlays.remove(i--); continue; }
            add(st, tr, f.now, MotionMath.ease(tr.x), out);
        }
    }

    /**
     * Steps the top track's weight to {@code now} (fading in, or out once its clip ends) and drops what it covers or what has
     * faded out. A track begun this frame (fresh) holds its weight. action() runs it every frame; begin() runs it first, so the
     * FROM track starts from the fold as it stands at now, and action() then finds the new track on top, fresh.
     */
    static void advance(MotionEngine.State st, MotionEngine.Frame f) {
        List<Track> tracks = st.tracks;
        if (tracks.isEmpty()) return;
        Track top = tracks.get(tracks.size() - 1);
        double last = f.now - f.dt;
        if (!top.leaving) {
            double end = top.kind == MotionEngine.DEATH ? (f.dead ? Double.POSITIVE_INFINITY : f.now)
                    : top.kind == MotionEngine.MERGED ? f.now : top.end();
            if (f.now >= end) {
                double tEnd = Math.max(last, Math.min(end, f.now));
                if (!top.fresh) top.x = Math.min(1, top.x + (tEnd - last) / top.fadeIn);
                top.leaving = true;
                top.fadeOut = fadeTime(st, top, tEnd, tracks.size() - 1, false);
                top.gone = tEnd + top.x * top.fadeOut;
                top.x = Math.max(0, top.x - (f.now - tEnd) / top.fadeOut);
            } else {
                if (!top.fresh) top.x = Math.min(1, top.x + f.dt / top.fadeIn);
                if (top.x >= 1) while (tracks.size() > 1) tracks.remove(0);
            }
        } else top.x = Math.max(0, top.x - f.dt / top.fadeOut);
        top.fresh = false;
        if (top.leaving && top.x <= 0) {
            if (top.kind == MotionEngine.DEATH) st.deathGone = top.gone;
            tracks.remove(tracks.size() - 1);
        }
    }

    /**
     * When the newest death clip's weight reaches 0 once a revived unit's fade-out has begun (infinity while it still holds
     * the corpse), or, once it has left, when it did (minus infinity if no death clip ever left).
     */
    static double deathGone(MotionEngine.State st) {
        for (int i = st.tracks.size() - 1; i >= 0; i--) {
            Track tr = st.tracks.get(i);
            if (tr.kind == MotionEngine.DEATH) return tr.gone;
        }
        return st.deathGone;
    }

    /**
     * When the newest death clip is 0.55 s from its end (render time), so the corpse's secondary springs start to settle;
     * minus infinity if no death clip is playing. It is exact rather than the first frame after, so the settle is the same at any frame rate.
     */
    static double deathSettles(MotionEngine.State st) {
        for (int i = st.tracks.size() - 1; i >= 0; i--) {
            Track tr = st.tracks.get(i);
            if (tr.kind != MotionEngine.DEATH) continue;
            double x = Math.max(0, tr.length() - 0.55);
            return tr.start + (tr.impact < 0 ? x / tr.tempo : x <= tr.impact ? x : tr.impact + (x - tr.impact) / tr.tempo);
        }
        return Double.NEGATIVE_INFINITY;
    }

    /** The clips the tracks are playing at {@code now}, bottom first, each with its effective share of the fold times its gain. */
    static List<MotionEngine.Playing> playing(MotionEngine.State st, double now) {
        List<MotionEngine.Playing> list = new ArrayList<>();
        double[] e = new double[st.tracks.size()];
        shares(st.tracks, e.length, e);
        for (int i = 0; i < e.length; i++) {
            Track tr = st.tracks.get(i);
            if (tr.kind == MotionEngine.MERGED) continue;
            double t = tr.time(now), m = tr.mix(t);
            list.add(new MotionEngine.Playing(tr.id * 2, tr.a, t, tr.gain * (1 - m) * e[i]));
            if (tr.b != null) list.add(new MotionEngine.Playing(tr.id * 2 + 1, tr.b, t, tr.gain * m * e[i]));
        }
        for (Track tr : st.overlays) list.add(new MotionEngine.Playing(tr.id * 2, tr.a, tr.time(now), tr.gain * MotionMath.ease(tr.x)));
        return list;
    }
}
