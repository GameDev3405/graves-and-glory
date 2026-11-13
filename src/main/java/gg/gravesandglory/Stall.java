package gg.gravesandglory;

import java.util.List;

/**
 * The stall ladder (engine-map pathing plan, step 2): what a troop does when it has been walking and getting nowhere. Pure (the clock, the place, the goal and the distance in, a rung out), so that
 * test-tools/standalone/StallCheck.java can run it without the game; Tactics.walk asks it once per 10-tick beat of every walking troop, a player's squad and a bot's alike, and carries out the rung.
 *
 * A troop is stalled when it has not got nearer its goal: its distance by the route field (NavField.cachedDistance) has not fallen GAIN blocks below the best it reached since the clock started. On a map with no field
 * (or a goal the field does not know) the test is the place: it has not left a PATCH-wide patch round where the clock started. A troop that jitters in place or goes round in circles does not reset the clock; one that
 * closes on its goal, however slowly (a file through a door), does. A troop within ARRIVED of its goal, one that stopped walking for more than GAP ticks (it holds, fights or has arrived: Tactics.walk is not called for it),
 * and one whose goal moved MOVED blocks (a new order, a chase) start again, so they are never flagged.
 *
 * Rungs, by ticks since the clock started: REPATH drops the path it follows (3 s), SIDESTEP the sidestep of walk's stuck handling toward the other side each time (6 s), DETOUR the wide search of Wedge (10 s),
 * REGROUP a walk to the nearest standing formation cell on the field (15 s), JUMP puts it on the nearest standing floor toward its goal, but never while a player sees it or the place (30 s). A jump that is refused is asked
 * for again every RETRY ticks, and each refusal sets the troop on another rung of the cycle meanwhile (REGROUP, SIDESTEP, DETOUR, by cycle), so that it keeps trying until nobody watches.
 *
 * The queue test (plan step 3): a troop with a friend ahead of it on its way who is moving the same way is queueing, not stalled. queueing() is that test (a few numbers in, a boolean out); step() given queued holds the clock
 * (it starts again every beat, no rung fires, and the jump least of all). A queue that has held the clock for QUEUE_MAX ticks without the troop getting anywhere is not a queue any more (a ring of troops each behind the next):
 * the clock runs from then on, until the troop gets somewhere or a gap of GAP ticks ends the stretch.
 */
final class Stall {
    private Stall() {}
    static final int NONE = 0, REPATH = 1, SIDESTEP = 2, DETOUR = 3, REGROUP = 4, JUMP = 5;
    static final String[] NAMES = {"none", "repath", "sidestep", "detour", "regroup", "jump"};
    /** Ticks since the clock started at which each rung fires (index = rung). */
    static final long[] AT = {0, 60, 120, 200, 300, 600};
    /** A jump that was refused (watched, or nowhere to put the troop) is asked for again after this many ticks. */
    static final long RETRY = 40;
    /** Blocks of route distance a troop must gain to restart the clock; the patch it must leave on a map with no field; within this many blocks of its goal it has arrived; a goal that moves this far starts again. */
    static final double GAIN = 1.0, PATCH = 5.0, ARRIVED = 2.5, MOVED = 4.0;
    /** A troop off the field (in water, on a wall top: no floor under it for the route field) drifts about (a swimmer is pushed round), so leaving the patch restarts its clock only at OFF_SPEED blocks a second or more, and its clock runs
     *  OFF_RATE times as fast: it is put back on a floor sooner. */
    static final double OFF_SPEED = 1.0;
    static final int OFF_RATE = 2;
    /** A troop this far (blocks) under or over its goal has not arrived however near it is on the map: a raised slot (a tower top) over its head. */
    static final double RAISED = 2.5;
    /** Blocks within which a player (or the camera a player has out) counts as watching, whatever it can see. */
    static final double NEAR = 24.0;
    /** The height of a troop for the sight test, blocks above its feet. */
    static final double HEAD = 1.8;
    /** A troop not seen walking for more than this many ticks (three beats) was holding or fighting: the clock starts again when it walks on. */
    static final long GAP = 30;
    /** The queue test: a friend within QUEUE_RANGE blocks, QUEUE_CONE (cosine) round the way on, moving at least QUEUE_SPEED blocks a beat (10 ticks) with the troop's way (cosine over 0) and measured within QUEUE_FRESH ticks. A hold of the clock lasts QUEUE_MAX ticks. */
    static final double QUEUE_RANGE = 3.5, QUEUE_CONE = 0.3, QUEUE_SPEED = 0.5;
    static final long QUEUE_FRESH = 25, QUEUE_MAX = 600;

    /** One troop's clock. since: tick the clock started; seen: the tick of its last beat; best: the least route distance reached (field mode); ax, az: where it started (place mode); gx, gz: the goal it started for. */
    static final class State {
        boolean live, field;
        double best, ax, az, gx, gz;
        long since, seen, nextJump;
        int rung, cycle, sidesteps, jumps;
        /** The queue test's memory: where and when the troop was last measured (px, pz, pt), how far it went in that beat (mvx, mvz: blocks), when a queue hold began and last held (qSince, qLast; -1: none), and whether this beat's clock was held. */
        double px, pz, mvx, mvz;
        long pt = -1000, qSince = -1, qLast = -1000;
        boolean held;
        /** Diagnostics: the age (ticks) the clock had when it was last restarted, and why (gap, goal, progress). */
        long lostAge; String why;
        /** Ticks the troop has been stalled as of its last beat (0 when it is not). */
        long age(long time) { return live ? time - since : 0; }
    }

    /** The troop's own movement, measured once a beat while it walks: how far it went since the last measure (at least 8 ticks ago), kept for the friends behind it (queueing). */
    static void track(State s, long time, double x, double z) {
        if (time - s.pt > QUEUE_FRESH) { s.px = x; s.pz = z; s.pt = time; s.mvx = 0; s.mvz = 0; return; } // the last measure is stale: no speed to tell yet
        if (time - s.pt < 8) return;
        s.mvx = x - s.px; s.mvz = z - s.pz; s.px = x; s.pz = z; s.pt = time;
    }

    /** Whether the troop's last measure says it is moving: at least QUEUE_SPEED blocks in its last beat, measured within QUEUE_FRESH ticks. */
    static boolean moving(State s, long time) { return time - s.pt <= QUEUE_FRESH && Math.hypot(s.mvx, s.mvz) >= QUEUE_SPEED; }

    /** The queue test for one friend: at (fx, fz), moving (mvx, mvz) a beat, for the troop at (x, z) whose way on is the unit vector (dx, dz): near, ahead and going the same way. */
    static boolean queueing(double x, double z, double dx, double dz, double fx, double fz, double mvx, double mvz) {
        double ox = fx - x, oz = fz - z, d = Math.hypot(ox, oz);
        if (d > QUEUE_RANGE || d < 1e-6) return false;
        if ((ox * dx + oz * dz) / d < QUEUE_CONE) return false; // not ahead of it
        double v = Math.hypot(mvx, mvz);
        return v >= QUEUE_SPEED && (mvx * dx + mvz * dz) / v > 0; // moving, and the troop's way
    }

    private static void start(State s, long time, double x, double z, double gx, double gz, double dist, boolean field) {
        s.live = true; s.field = field; s.best = field ? dist : Double.NaN; s.ax = x; s.az = z; s.gx = gx; s.gz = gz; s.since = time; s.rung = 0; s.nextJump = 0;
    }

    /**
     * One beat of a walking troop at (x, z), whose goal is (gx, gz), dy blocks above or below it, and whose distance to it by the route field is dist (blocks; NaN or negative when the field has none).
     * off: the troop is off the field or in water (see OFF_SPEED). Returns the rung to carry out now, or NONE; at most one rung a beat, in order.
     */
    static int step(State s, long time, double x, double z, double gx, double gz, double dy, double dist, boolean off) { return step(s, time, x, z, gx, gz, dy, dist, off, false); }

    /** As step, and queued: the troop has a friend ahead of it moving its way (the queue test), so its clock is held (see the class comment). */
    static int step(State s, long time, double x, double z, double gx, double gz, double dy, double dist, boolean off, boolean queued) {
        s.held = false;
        boolean field = !Double.isNaN(dist) && dist >= 0;
        if (Math.hypot(gx - x, gz - z) < ARRIVED && Math.abs(dy) < RAISED) { s.live = false; return NONE; }
        if (!s.live || time - s.seen > GAP || Math.hypot(gx - s.gx, gz - s.gz) > MOVED) { s.lostAge = s.live ? s.seen - s.since : 0; s.why = !s.live ? "dead" : time - s.seen > GAP ? "gap " + (time - s.seen) : "goal"; start(s, time, x, z, gx, gz, dist, field); }
        else if (field && Double.isNaN(s.best)) s.best = dist; // the field knows the way now (the goal was in plain sight before): the clock runs on, it does not start over
        s.field = field;
        s.seen = time;
        if (queued) s.qLast = time;
        else if (time - s.qLast > GAP) s.qSince = -1;
        if (queued && (s.qSince < 0 || time - s.qSince <= QUEUE_MAX)) { // a moving queue: the clock starts again, nothing fires
            if (s.qSince < 0) s.qSince = time;
            s.since = time; s.rung = 0; s.nextJump = 0; s.held = true;
            if (field) s.best = Math.min(s.best, dist);
            return NONE;
        }
        double moved = Math.hypot(x - s.ax, z - s.az);
        boolean progress = field ? dist < s.best - GAIN : moved > PATCH && (!off || moved >= OFF_SPEED * (time - s.since) / 20.0);
        if (progress) { s.qSince = -1; s.lostAge = s.seen - s.since; s.why = "progress " + String.format("%.1f<%.1f", dist, s.best); start(s, time, x, z, gx, gz, dist, field); }
        long age = (time - s.since) * (off && !field ? OFF_RATE : 1);
        if (s.rung < JUMP) {
            if (age < AT[s.rung + 1]) return NONE;
            s.rung++;
            if (s.rung == JUMP) s.nextJump = time + RETRY;
            return s.rung;
        }
        if (time < s.nextJump) return NONE;
        s.nextJump = time + RETRY;
        return JUMP;
    }

    /** The rung a refused jump is answered with: REGROUP, SIDESTEP and DETOUR in turn. */
    static int refused(State s) { return new int[]{REGROUP, SIDESTEP, DETOUR}[s.cycle++ % 3]; }

    /** One player (or one camera a player has out) as the watched test sees it: live (not a spectator, in the troop's world), its distance to a point, and whether it has a clear line of sight (a block's visual shape, no fluid) to a point within its view range. */
    interface Viewer {
        boolean live();
        double dist(double x, double y, double z);
        boolean line(double x, double y, double z);
    }

    /** Whether anyone watches a troop at (tx, ty, tz) put on (sx, sy, sz): a live viewer within NEAR blocks of either, or one with a line to the feet or the head of either. A jump is made only when this is false. Nobody online: not watched. */
    static boolean watched(List<? extends Viewer> viewers, double tx, double ty, double tz, double sx, double sy, double sz) {
        for (Viewer v : viewers) {
            if (!v.live()) continue;
            if (v.dist(tx, ty, tz) <= NEAR || v.dist(sx, sy, sz) <= NEAR) return true;
            if (v.line(tx, ty, tz) || v.line(tx, ty + HEAD, tz) || v.line(sx, sy, sz) || v.line(sx, sy + HEAD, sz)) return true;
        }
        return false;
    }
}
