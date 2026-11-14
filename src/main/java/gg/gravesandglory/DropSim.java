package gg.gravesandglory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * The drop board's coin (design 2026-11-14, section 2.1 rule 3): a deterministic 2D simulation of a coin falling through the pegs into a
 * slot, and the search for a run from a chosen start that ends in a slot drawn beforehand. Pure: no Minecraft, the same result for the
 * same arguments on every machine (plain double arithmetic and Math.sqrt only).
 *
 * <p>Board frame, in blocks: x from 0 (left wall) to WIDTH (right wall), y down from the rail. Peg row r is at y = FIRST_ROW + r; even
 * rows have a peg on every whole x (0 to WIDTH, the outer two half in the walls), odd rows on every half; beside each odd row a buttress ramps from the wall down to the outermost peg. The slot dividers stand on the
 * whole x from MOUTH down to FLOOR; slot i is x in [i, i+1).
 */
public final class DropSim {
    private DropSim() {}

    public static final int WIDTH = 9, ROWS = 8;
    public static final double COIN = 0.3125, PEG = 0.125, DIVIDER = 0.0625;
    public static final double START_Y = 0.45, FIRST_ROW = 1.5, MOUTH = 9.5, FLOOR = 11.0;
    /** The rail: how far the coin's centre may slide. */
    public static final double MIN_X = 0.5, MAX_X = WIDTH - 0.5;
    /** Gravity (blocks a second squared), the bounce kept along the normal, the speed kept along the surface, and the turn (radians) a bounce may take by chance. */
    static final double GRAVITY = 24.0, BOUNCE = 0.45, SLIDE = 0.985, JITTER = 0.20, FLOOR_BOUNCE = 0.25;
    static final double STEP = 1.0 / 240, SAMPLE = 1.0 / 60, MAX_SECONDS = 9.0;
    /** What a hit struck: a peg (its row * 16 + its index in the row), or one of these. */
    public static final int WALL = -1, DIVIDER_HIT = -2, FLOOR_HIT = -3;
    /** Plain runs tried before the search starts to lean the bounces toward the slot, and the tries in all. */
    static final int FREE_TRIES = 250, MAX_TRIES = 4000;

    /** One hit of the coin: the sample it falls in, what it struck and how fast the coin met it (blocks a second along the normal). */
    public record Hit(int sample, int what, float speed) {}

    /**
     * One run: the slot it ended in (-1: it did not come to rest in time), the coin's centre at every 60th of a second from the release
     * to the rest, and its hits. steered tells whether the search had to lean the bounces.
     */
    public record Path(int slot, float[] xs, float[] ys, List<Hit> hits, boolean steered) {
        public double seconds() { return (xs.length - 1) * SAMPLE; }
        /** The coin's centre at a time from the release, between samples in a straight line; the ends hold. */
        public double[] at(double seconds) {
            double f = Math.max(0, Math.min(xs.length - 1, seconds / SAMPLE));
            int i = (int) Math.min(xs.length - 2, Math.floor(f));
            if (xs.length == 1) return new double[]{xs[0], ys[0]};
            double t = f - i;
            return new double[]{xs[i] + (xs[i + 1] - xs[i]) * t, ys[i] + (ys[i + 1] - ys[i]) * t};
        }
    }

    /** Whether row r has a peg at index i: even rows on every whole x (0 to WIDTH), odd rows on every half. */
    public static boolean peg(int row, int i) { return row >= 0 && row < ROWS && i >= 0 && i <= (row % 2 == 0 ? WIDTH : WIDTH - 1); }
    /** The wall's buttress beside an odd row: a ramp from the wall, RAMP above the row, down to the row's outermost peg (without it a coin wedges between that peg and the wall). */
    public static final double RAMP = 0.6;
    public static double pegX(int row, int i) { return row % 2 == 0 ? i : i + 0.5; }

    public static double clampStart(double x) { return Double.isNaN(x) ? WIDTH / 2.0 : Math.max(MIN_X, Math.min(MAX_X, x)); }
    /** The middle of a slot. */
    public static double slotX(int slot) { return slot + 0.5; }

    /** One run from a start with the seed's chance; lean is the sideways push (blocks a second) each bounce gets toward target (0: none). */
    public static Path run(double startX, long seed, double lean, int target) {
        var rng = new SplittableRandom(seed);
        double x = clampStart(startX), y = START_Y, vx = (rng.nextDouble() - 0.5) * 0.2, vy = 0, clock = 0, nextSample = 0;
        var xs = new ArrayList<Float>();
        var ys = new ArrayList<Float>();
        var hits = new ArrayList<Hit>();
        int steps = (int) (MAX_SECONDS / STEP), still = 0;
        for (int s = 0; s <= steps; s++) {
            if (clock >= nextSample - 1e-9) { xs.add((float) x); ys.add((float) y); nextSample += SAMPLE; }
            vy += GRAVITY * STEP;
            x += vx * STEP; y += vy * STEP; clock += STEP;
            // pegs: only the rows the coin can touch
            int r0 = (int) Math.floor(y - FIRST_ROW - 1), r1 = r0 + 3;
            for (int r = Math.max(0, r0); r <= Math.min(ROWS - 1, r1); r++) {
                double py = FIRST_ROW + r, off = r % 2 == 0 ? 0 : 0.5;
                int i0 = (int) Math.floor(x - off - 1), n = r % 2 == 0 ? WIDTH + 1 : WIDTH;
                for (int i = Math.max(0, i0); i <= Math.min(n - 1, i0 + 3); i++) {
                    double[] v = bounce(x, y, vx, vy, i + off, py, COIN + PEG, BOUNCE, rng, lean, target);
                    if (v != null) { x = v[0]; y = v[1]; vx = v[2]; vy = v[3]; if (v[4] > 0.4) hits.add(new Hit(xs.size() - 1, r * 16 + i, (float) v[4])); }
                }
            }
            // buttresses: the nearest point of the ramp beside each odd row in reach
            for (int r = Math.max(1, r0); r <= Math.min(ROWS - 1, r1); r++) {
                if (r % 2 == 0) continue;
                double py = FIRST_ROW + r;
                for (int side = 0; side < 2; side++) {
                    if (side == 0 ? x > 1.2 : x < WIDTH - 1.2) continue;
                    double ax = side == 0 ? 0 : WIDTH, ay = py - RAMP, bx = side == 0 ? 0.5 : WIDTH - 0.5, ex = bx - ax, ey = py - ay;
                    double t = Math.max(0, Math.min(1, ((x - ax) * ex + (y - ay) * ey) / (ex * ex + ey * ey)));
                    double[] v = bounce(x, y, vx, vy, ax + ex * t, ay + ey * t, COIN, BOUNCE, null, 0, target);
                    if (v != null) { x = v[0]; y = v[1]; vx = v[2]; vy = v[3]; if (v[4] > 0.4) hits.add(new Hit(xs.size() - 1, WALL, (float) v[4])); }
                }
            }
            // dividers: the nearest point of the two segments beside the coin
            if (y > MOUTH - COIN - DIVIDER) {
                for (int i = (int) Math.floor(x); i <= (int) Math.floor(x) + 1; i++) {
                    if (i < 1 || i > WIDTH - 1) continue;
                    double cy = Math.max(MOUTH, Math.min(FLOOR, y));
                    double[] v = bounce(x, y, vx, vy, i, cy, COIN + DIVIDER, 0.35, null, 0, target);
                    if (v != null) { x = v[0]; y = v[1]; vx = v[2]; vy = v[3]; if (v[4] > 0.4) hits.add(new Hit(xs.size() - 1, DIVIDER_HIT, (float) v[4])); }
                }
            }
            if (x < COIN) { x = COIN; if (vx < 0) { if (-vx > 0.4) hits.add(new Hit(xs.size() - 1, WALL, (float) -vx)); vx = -vx * BOUNCE; } }
            if (x > WIDTH - COIN) { x = WIDTH - COIN; if (vx > 0) { if (vx > 0.4) hits.add(new Hit(xs.size() - 1, WALL, (float) vx)); vx = -vx * BOUNCE; } }
            if (y > FLOOR - COIN) {
                y = FLOOR - COIN;
                if (vy > 0) { if (vy > 0.4) hits.add(new Hit(xs.size() - 1, FLOOR_HIT, (float) vy)); vy = -vy * FLOOR_BOUNCE; if (-vy < 0.5) vy = 0; }
                vx *= 0.94; // the floor of the slot drags
                still = Math.abs(vx) < 0.15 && vy == 0 ? still + 1 : 0;
                if (still > 36) {
                    xs.add((float) x); ys.add((float) y);
                    return path((int) Math.max(0, Math.min(WIDTH - 1, Math.floor(x))), xs, ys, hits, lean != 0);
                }
            }
        }
        return path(-1, xs, ys, hits, lean != 0);
    }

    private static Path path(int slot, List<Float> xs, List<Float> ys, List<Hit> hits, boolean steered) {
        float[] ax = new float[xs.size()], ay = new float[ys.size()];
        for (int i = 0; i < ax.length; i++) { ax[i] = xs.get(i); ay[i] = ys.get(i); }
        return new Path(slot, ax, ay, List.copyOf(hits), steered);
    }

    /**
     * The coin against a round thing at (px, py) whose reach (both radii) is given: null when they do not touch; else the coin pushed out to
     * the touch and its velocity turned {x, y, vx, vy, the speed it met the thing at (0 when it was already leaving)}. With rng the outgoing
     * velocity takes a small turn by chance and the lean.
     */
    private static double[] bounce(double x, double y, double vx, double vy, double px, double py, double reach, double keep, SplittableRandom rng, double lean, int target) {
        double dx = x - px, dy = y - py, d2 = dx * dx + dy * dy;
        if (d2 >= reach * reach) return null;
        double d = Math.sqrt(d2);
        if (d < 1e-9) { dx = 0; dy = -1; d = 1; }
        double nx = dx / d, ny = dy / d, into = vx * nx + vy * ny;
        x = px + nx * reach; y = py + ny * reach;
        if (into >= 0) return new double[]{x, y, vx, vy, 0};
        double tx = -ny, ty = nx, along = (vx * tx + vy * ty) * SLIDE, out = -into * keep;
        vx = nx * out + tx * along; vy = ny * out + ty * along;
        if (rng != null) {
            double a = (rng.nextDouble() - 0.5) * 2 * JITTER, c = Math.cos(a), s = Math.sin(a), rx = vx * c - vy * s, ry = vx * s + vy * c;
            // never turned back into the peg
            if (rx * nx + ry * ny > 0) { vx = rx; vy = ry; }
            if (lean != 0) vx += lean * Math.signum(slotX(target) - x);
        }
        return new double[]{x, y, vx, vy, -into};
    }

    /**
     * A run from the start that ends in the slot (the slot was drawn first, DropMath.drawSlot; this only finds the bounces). Plain runs
     * with fresh chance first; when none of FREE_TRIES lands there, each bounce is leaned a little toward the slot, more with every further
     * try, so every slot is reached from every start. Deterministic in its arguments.
     */
    public static Path find(double startX, int slot, long seed) {
        var seeds = new SplittableRandom(seed);
        double minSeconds = Spec.DROPBOARD[0].dropMinTenths() / 10.0, maxSeconds = Spec.DROPBOARD[0].dropMaxTenths() / 10.0;
        Path fallback = null;
        for (int k = 0; k < MAX_TRIES; k++) {
            double lean = k < FREE_TRIES ? 0 : Math.min(1.6, 0.1 + 0.002 * (k - FREE_TRIES));
            Path p = run(startX, seeds.nextLong(), lean, slot);
            if (p.slot() != slot) continue;
            if (p.seconds() >= minSeconds && p.seconds() <= maxSeconds) return p;
            if (fallback == null) fallback = p; // the right slot but too quick or too slow: kept in case nothing better comes
            if (k > MAX_TRIES / 2) return fallback;
        }
        if (fallback != null) return fallback;
        throw new IllegalStateException("drop board: no path from " + startX + " to slot " + slot);
    }

    /** The path as bytes for the wire: slot, steered, the samples as unsigned shorts (x / WIDTH, y / FLOOR), the hits. */
    public static byte[] encode(Path p) {
        var b = ByteBuffer.allocate(8 + p.xs().length * 4 + 4 + p.hits().size() * 8);
        b.put((byte) p.slot()).put((byte) (p.steered() ? 1 : 0)).putShort((short) 0).putInt(p.xs().length);
        for (int i = 0; i < p.xs().length; i++) { b.putShort((short) Math.round(p.xs()[i] / WIDTH * 65535)); b.putShort((short) Math.round(p.ys()[i] / FLOOR * 65535)); }
        b.putInt(p.hits().size());
        for (Hit h : p.hits()) { b.putShort((short) h.sample()).putShort((short) h.what()).putFloat(h.speed()); }
        return b.array();
    }

    /** The path of encode's bytes; throws IllegalArgumentException on bytes that are not one. */
    public static Path decode(byte[] bytes) {
        try {
            var b = ByteBuffer.wrap(bytes);
            int slot = b.get(); boolean steered = b.get() != 0; b.getShort();
            int n = b.getInt();
            if (n < 1 || n > 60 * 20) throw new IllegalArgumentException("drop path: " + n + " samples");
            float[] xs = new float[n], ys = new float[n];
            for (int i = 0; i < n; i++) { xs[i] = (b.getShort() & 0xffff) / 65535f * WIDTH; ys[i] = (b.getShort() & 0xffff) / 65535f * (float) FLOOR; }
            int hn = b.getInt();
            if (hn < 0 || hn > 4000) throw new IllegalArgumentException("drop path: " + hn + " hits");
            var hits = new ArrayList<Hit>();
            for (int i = 0; i < hn; i++) hits.add(new Hit(b.getShort() & 0xffff, b.getShort(), b.getFloat()));
            return new Path(slot, xs, ys, List.copyOf(hits), steered);
        } catch (java.nio.BufferUnderflowException e) {
            throw new IllegalArgumentException("drop path: cut short");
        }
    }
}
