package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * File-through (engine-map pathing plan, step 3): troops of one side whose way crosses a portal narrower than they are wide go through it as a column, front rank first, and form up again on the far side. Pure (a clock, a
 * door's width and each troop's rank and distance in; go or wait out), so test-tools/standalone/FileThroughCheck.java can run it without the game; Tactics.walk asks it once per beat of a troop on a slot walk whose next route
 * point is a portal (NavField.Waypoint.portal), and carries out the verdict.
 *
 * A gate is one portal of one side. A troop within RANGE blocks of it that can see it takes a ticket (renewed every beat; one not renewed for EXPIRE ticks is gone). The gate lets LANES(width) troops through at a time: the
 * admitted ones walk on to the door, and a troop that gets within REACHED blocks of it has passed, which frees a lane for the next in the file. The file is ordered by rank (the WarGrid row of the troop's cell, the front rank first),
 * then by who is nearest the door; a troop with no cell (a refill, a summon) has the rank after the last row, so it joins the end. A troop's rank is read afresh every beat (a new order may give it another cell). The others wait in a column behind the door, each at its own place in it
 * (place 0 first). A troop admitted that has not reached the door in TURN ticks loses its turn and goes to the back, so that nobody pushes at a door for ever; a gate nobody has passed for FILE_STALL ticks while troops wait opens for
 * OPEN ticks (everyone walks on as before; the stall ladder does the rest). With no more troops than lanes nobody waits at all, so a lone troop, a small squad and the open ground are untouched.
 */
final class FileThrough {
    private FileThrough() {}
    /** Blocks from the door within which a troop that sees it joins the file, and within which it has passed. */
    static final double RANGE = 12, REACHED = 1.6;
    /** The column: the first waiting place is QUEUE0 blocks back from the door, each further row GAP blocks behind the one before. A lane takes LANE blocks of the door's width (a body and its elbows). */
    static final double QUEUE0 = 2.4, GAP = 0.9, LANE = 1.5;
    /** Ticks: a ticket not renewed leaves the file; an admitted troop's turn; a file nobody passes before it opens; how long an opened gate stays open. */
    static final long EXPIRE = 40, TURN = 100, FILE_STALL = 300, OPEN = 400;
    /** The rank of a troop with no cell: after every row of the war table. */
    static final int LOOSE = 1 << 20;

    /** How many troops a door of this many blocks takes at once (at least one). */
    static int lanes(int width) { return Math.max(1, (int) Math.floor(width / LANE)); }

    static final class Ticket {
        final int id; int rank; final long seq;
        long seen, admitted = -1;
        double dist;
        boolean passed, lost;
        Ticket(int id, int rank, long seq) { this.id = id; this.rank = rank; this.seq = seq; }
    }

    /** One portal of one side: its tickets and when it last let someone through. */
    static final class Gate {
        final Map<Integer, Ticket> tickets = new LinkedHashMap<>();
        long seq, lastPass = Long.MIN_VALUE / 2, waitSince = -1, openUntil = -1;
        /** Passes since the gate was made (diagnostic). */
        int passes, turnsLost, opened;
    }

    /** What a troop does this beat: go on to the door (walk its route as it was), or wait in the column at place (0 is the first to wait). */
    static final class Verdict {
        boolean go = true;
        int place;
    }

    /**
     * One beat of the troop id at dist blocks from the door (door width in blocks), rank its war table row (a negative rank: no cell). Gives the troop its ticket, lets the gate admit what its lanes allow and says what the troop does.
     */
    static Verdict beat(Gate g, long time, int id, int rank, int width, double dist) {
        Verdict v = new Verdict();
        for (Iterator<Ticket> it = g.tickets.values().iterator(); it.hasNext(); ) { Ticket t = it.next(); if (t.id != id && time - t.seen > EXPIRE) it.remove(); }
        Ticket me = g.tickets.get(id);
        if (me == null) { me = new Ticket(id, rank < 0 ? LOOSE : rank, g.seq++); g.tickets.put(id, me); }
        me.seen = time; me.dist = dist;
        me.rank = rank < 0 ? LOOSE : rank; // a new order mid-file (a player's) may give the troop another cell, so another rank
        if (me.passed) return v;
        if (dist <= REACHED) { me.passed = true; g.lastPass = time; g.passes++; return v; }
        if (time < g.openUntil) return v;
        int lanes = lanes(width), inflight = 0;
        List<Ticket> waiting = new ArrayList<>();
        for (Ticket t : g.tickets.values()) {
            if (t.passed) continue;
            if (t.admitted >= 0 && time - t.admitted > TURN) { t.admitted = -1; t.lost = true; g.turnsLost++; } // it had its turn: to the back (behind every troop that has not had one, among those that lost it by rank again)
            if (t.admitted >= 0) inflight++; else waiting.add(t);
        }
        waiting.sort(Comparator.<Ticket>comparingInt(t -> t.lost ? 1 : 0).thenComparingInt(t -> t.rank).thenComparingDouble(t -> t.dist).thenComparingInt(t -> t.id));
        int admit = Math.min(Math.max(0, lanes - inflight), waiting.size());
        for (int i = 0; i < admit; i++) waiting.get(i).admitted = time;
        waiting = waiting.subList(admit, waiting.size());
        if (waiting.isEmpty()) g.waitSince = -1; else if (g.waitSince < 0) g.waitSince = time;
        if (me.admitted >= 0) return v;
        if (!waiting.isEmpty() && time - Math.max(g.lastPass, g.waitSince) > FILE_STALL) { g.openUntil = time + OPEN; g.opened++; g.waitSince = -1; for (Ticket t : g.tickets.values()) if (!t.passed) t.admitted = time; return v; } // nobody has passed: the gate opens
        v.go = false; v.place = Math.max(0, waiting.indexOf(me));
        return v;
    }

    /** The troop id has reached the door (the caller saw it: once within REACHED blocks the route goes on past the door, so the troop no longer asks this gate): it has passed, and its lane is free. */
    static void pass(Gate g, int id, long time) {
        Ticket t = g.tickets.get(id);
        if (t == null || t.passed) return;
        t.passed = true; g.lastPass = time; g.passes++;
    }

    /** How far back from the door the troop at this place of the column waits, and how far to the side of the way in (lane l of lanes, in blocks from its middle). */
    static double back(int place, int lanes) { return QUEUE0 + (place / lanes) * GAP; }
    static double side(int place, int lanes) { return (place % lanes - (lanes - 1) / 2.0) * GAP; }

    /** Whether a gate holds anyone (any ticket not passed): for the harness. */
    static int waiting(Gate g) { int n = 0; for (Ticket t : g.tickets.values()) if (!t.passed && t.admitted < 0) n++; return n; }
}
