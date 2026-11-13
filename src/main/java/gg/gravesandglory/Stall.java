package gg.gravesandglory;

import java.util.function.IntPredicate;

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
    /** A troop not seen walking for more than this many ticks (three beats) was holding or fighting: the clock starts again when it walks on. */
    static final long GAP = 30;

    /** One troop's clock. since: tick the clock started; seen: the tick of its last beat; best: the least route distance reached (field mode); ax, az: where it started (place mode); gx, gz: the goal it started for. */
    static final class State {
        boolean live, field;
        double best, ax, az, gx, gz;
        long since, seen, nextJump;
        int rung, cycle, sidesteps, jumps;
        /** Ticks the troop has been stalled as of its last beat (0 when it is not). */
        long age(long time) { return live ? time - since : 0; }
    }

    private static void start(State s, long time, double x, double z, double gx, double gz, double dist, boolean field) {
        s.live = true; s.field = field; s.best = dist; s.ax = x; s.az = z; s.gx = gx; s.gz = gz; s.since = time; s.rung = 0; s.nextJump = 0;
    }

    /**
     * One beat of a walking troop at (x, z), whose goal is (gx, gz), and whose distance to it by the route field is dist (blocks; NaN or negative when the field has none).
     * Returns the rung to carry out now, or NONE; at most one rung a beat, in order.
     */
    static int step(State s, long time, double x, double z, double gx, double gz, double dist) {
        boolean field = !Double.isNaN(dist) && dist >= 0;
        if (Math.hypot(gx - x, gz - z) < ARRIVED) { s.live = false; return NONE; }
        if (!s.live || time - s.seen > GAP || field != s.field || Math.hypot(gx - s.gx, gz - s.gz) > MOVED) start(s, time, x, z, gx, gz, dist, field);
        s.seen = time;
        boolean progress = field ? dist < s.best - GAIN : Math.hypot(x - s.ax, z - s.az) > PATCH;
        if (progress) start(s, time, x, z, gx, gz, dist, field);
        long age = time - s.since;
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

    /** Whether a viewer watches: viewers numbered 0 to n - 1, one that sees the troop or sees the landing spot (each test already includes the viewer's range). A jump is made only when this is false. */
    static boolean watched(int viewers, IntPredicate seesTroop, IntPredicate seesSpot) {
        for (int i = 0; i < viewers; i++) if (seesTroop.test(i) || seesSpot.test(i)) return true;
        return false;
    }
}
