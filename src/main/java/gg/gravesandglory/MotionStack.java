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
            top = new Layer();
            top.put(clip, 1);
            top.aim = aim;
            top.raw = f.first ? 1 : 0;
            top.fresh = !f.first;
            layers.add(top);
            if (layers.size() > MotionEngine.MAX_LAYERS) mergeLayers(layers);
        }
        if (!top.fresh) top.raw = Math.min(1, top.raw + f.dt / MotionEngine.STANCE_BLEND);
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
            c.add(c.loop() ? f.now * f.seed.tempo() + f.seed.phase() / MotionMath.TAU * c.length() : c.length(), out, coef[i], st.tmp);
            mix[i] = null;
        }
        f.aim = aimShare;
        out.add("right_arm", RX, f.aimPitch * aimShare);
        out.add("left_arm", RX, f.aimPitch * aimShare);
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
        boolean leaving, fresh;

        Track(int id, int kind, Clip a, Clip b, double mix, double gain, double start, double tempo) {
            this.id = id; this.kind = kind; this.a = a; this.b = b; this.mix = b == null ? 0 : mix; this.gain = gain;
            this.start = start; this.tempo = tempo; this.impact = a.impact() < 0 ? -1 : a.impact() / (double) Clip.TICKS;
            this.legShare = keysLegs(a) || (b != null && keysLegs(b)) ? 1 : 0;
        }

        /** A merged track: the frozen fold in State.merged, fully weighted. */
        Track(int id, double legShare) {
            this.id = id; this.kind = MotionEngine.MERGED; this.a = null; this.b = null; this.mix = 0; this.gain = 1;
            this.start = 0; this.impact = -1; this.tempo = 1; this.legShare = legShare; this.x = 1;
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
    }

    static boolean keysLegs(Clip c) {
        for (String b : c.bones()) if (b.endsWith("_leg") || b.endsWith("_shin")) return true;
        return false;
    }

    /** Adds w times track tr's pose at {@code now} (its gain included) into out. */
    static void add(MotionEngine.State st, Track tr, double now, double w, Pose out) {
        if (w == 0) return;
        if (tr.kind == MotionEngine.MERGED) { out.add(st.merged, w); return; }
        double t = tr.time(now);
        tr.a.add(t, out, w * tr.gain * (1 - tr.mix), st.tmp);
        if (tr.b != null) tr.b.add(t, out, w * tr.gain * tr.mix, st.tmp);
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
     * Pushes a track on top. The tracks below hold their weights; the new one fades in from the next frame as fast as the
     * pop budget allows. Past MAX_TRACKS the bottom two merge, so a weighted track is never dropped.
     */
    static void begin(MotionEngine.State st, Track tr, MotionEngine.Frame f) {
        if (f.first) tr.x = 1;
        else {
            tr.fadeIn = fadeTime(st, tr, f.now, st.tracks.size(), true);
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
        List<Track> tracks = st.tracks;
        if (!tracks.isEmpty()) {
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
                    top.x = Math.max(0, top.x - (f.now - tEnd) / top.fadeOut);
                } else {
                    if (!top.fresh) top.x = Math.min(1, top.x + f.dt / top.fadeIn);
                    if (top.x >= 1) while (tracks.size() > 1) tracks.remove(0);
                }
            } else top.x = Math.max(0, top.x - f.dt / top.fadeOut);
            top.fresh = false;
            if (top.leaving && top.x <= 0) tracks.remove(tracks.size() - 1);
        }
        f.bodyAction = fold(st, tracks.size(), f.now, out);
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
            double t = tr.time(now);
            list.add(new MotionEngine.Playing(tr.id * 2, tr.a, t, tr.gain * (1 - tr.mix) * e[i]));
            if (tr.b != null) list.add(new MotionEngine.Playing(tr.id * 2 + 1, tr.b, t, tr.gain * tr.mix * e[i]));
        }
        return list;
    }
}
