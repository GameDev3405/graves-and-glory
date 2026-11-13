package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/** FileThrough (engine-map pathing plan, step 3): a 3-wide gate takes a column of troops front rank first, a lane at a time, and then lets the formation re-form; a troop with no cell joins the end; a file nobody passes opens;
 *  and the queue test of Stall (a troop behind a mover is queueing: its clock is held and no rung, no jump, fires; a ring of queueing troops is not held for ever). */
public final class FileThroughCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }

    public static void main(String[] args) {
        // 1. a 3-wide door, 12 troops of a formation (rows 0..3, three to a row) all 8 blocks from it: two lanes, front rank first, a column behind
        check(FileThrough.lanes(3) == 2 && FileThrough.lanes(1) == 1 && FileThrough.lanes(6) == 4 && FileThrough.lanes(0) == 1, "lanes of a door: " + FileThrough.lanes(3));
        var g = new FileThrough.Gate();
        int n = 12;
        int[] rank = new int[n];
        double[] dist = new double[n];
        for (int i = 0; i < n; i++) { rank[i] = i / 3; dist[i] = 8 + (i % 3) * .3; }
        List<Integer> order = new ArrayList<>();
        int maxInflight = 0;
        boolean[] done = new boolean[n];
        long t = 0;
        FileThrough.Verdict v0 = null;
        for (; t < 1500 && order.size() < n; t += 10) {
            int inflight = 0, waiting = 0;
            for (int i = 0; i < n; i++) {
                if (done[i]) continue;
                var v = FileThrough.beat(g, t, i, rank[i], 3, dist[i]);
                if (t == 0 && i == 11) v0 = v;
                if (v.go) { dist[i] -= 1.5; if (dist[i] > FileThrough.REACHED) inflight++; else { FileThrough.beat(g, t, i, rank[i], 3, dist[i]); done[i] = true; order.add(i); } } // the beat at the door is a pass
                else { waiting++; check(v.place < n, "a place in the column"); }
            }
            maxInflight = Math.max(maxInflight, inflight);
            if (t == 0) check(inflight == 2 && waiting == 10, "two lanes open, ten wait: " + inflight + "/" + waiting);
        }
        check(order.size() == n, "all twelve pass: " + order);
        check(maxInflight <= 2, "never more than the door's lanes on the way in: " + maxInflight);
        boolean frontFirst = true;
        for (int k = 1; k < n; k++) if (rank[order.get(k)] < rank[order.get(k - 1)]) frontFirst = false;
        check(frontFirst, "front rank first: " + order);
        check(v0 != null && !v0.go && v0.place == 9, "the last of the rear rank is the tenth to wait: " + (v0 == null ? "?" : v0.place));
        // the column: places go back from the door, a lane to the side each
        check(FileThrough.back(0, 2) == FileThrough.QUEUE0 && FileThrough.back(2, 2) == FileThrough.QUEUE0 + FileThrough.GAP && FileThrough.side(0, 2) < 0 && FileThrough.side(1, 2) > 0, "column places");
        // re-forms: once all have passed, a troop asking again is simply let go
        long after = t + 20;
        var again = FileThrough.beat(g, after, 3, 1, 3, 9);
        check(again.go || FileThrough.waiting(g) <= 1, "after the file, nobody waits for it");
        // a squad no bigger than the lanes never waits: the open ground and small squads are untouched
        var small = new FileThrough.Gate();
        check(FileThrough.beat(small, 0, 1, 0, 3, 8).go && FileThrough.beat(small, 0, 2, 0, 3, 8).go, "two troops, two lanes: nobody waits");
        check(FileThrough.beat(new FileThrough.Gate(), 0, 1, 0, 1, 8).go, "a lone troop at a 1-wide door goes");

        // 2. a troop with no cell joins the end, whatever it is nearest to; the front rank is admitted before the rank behind it
        var h = new FileThrough.Gate();
        check(FileThrough.beat(h, 0, 1, 0, 1, 10).go, "the first goes");
        FileThrough.beat(h, 0, 99, -1, 1, 3);
        FileThrough.beat(h, 0, 2, 0, 1, 11);
        FileThrough.beat(h, 0, 3, 2, 1, 5);
        var loose = FileThrough.beat(h, 10, 99, -1, 1, 3);
        var front2 = FileThrough.beat(h, 10, 2, 0, 1, 11);
        var rear = FileThrough.beat(h, 10, 3, 2, 1, 5);
        check(!front2.go && front2.place == 0 && rear.place == 1 && loose.place == 2, "a no-cell troop is last: " + front2.place + rear.place + loose.place);
        FileThrough.beat(h, 20, 1, 0, 1, 1.0); // the first passes
        loose = FileThrough.beat(h, 30, 99, -1, 1, 3);
        front2 = FileThrough.beat(h, 30, 2, 0, 1, 11);
        rear = FileThrough.beat(h, 30, 3, 2, 1, 5);
        check(front2.go && !rear.go && !loose.go && loose.place == 1, "the front rank is next, the no-cell troop after the rank behind it");

        // 3. an admitted troop that never reaches the door loses its turn after TURN ticks and goes to the back
        var s = new FileThrough.Gate();
        FileThrough.beat(s, 0, 1, 0, 1, 10);
        boolean second = FileThrough.beat(s, 0, 2, 1, 1, 10).go;
        check(!second, "second waits");
        long lost = -1;
        for (long tt = 10; tt <= 400 && lost < 0; tt += 10) { FileThrough.beat(s, tt, 1, 0, 1, 10); if (FileThrough.beat(s, tt, 2, 1, 1, 10).go) lost = tt; }
        check(lost > 0 && lost <= FileThrough.TURN + 20, "the troop that does not move loses its turn: " + lost);
        check(s.turnsLost >= 1, "and goes to the back");

        // 4. a file nobody passes opens: everyone walks on, and the stall ladder takes it from there
        var d = new FileThrough.Gate();
        long opened = -1;
        for (long tt = 0; tt <= 1200 && opened < 0; tt += 10) {
            for (int i = 0; i < 6; i++) FileThrough.beat(d, tt, i, 0, 1, 10); // nobody gets nearer
            boolean allGo = true;
            for (int i = 0; i < 6; i++) allGo &= FileThrough.beat(d, tt, i, 0, 1, 10).go;
            if (allGo) opened = tt;
        }
        check(opened > 0 && d.opened >= 1, "a stuck file opens: " + opened);

        // 5. the queue test: a friend ahead, near, going the same way; not one behind, far off, still, or going the other way
        check(Stall.queueing(0, 0, 1, 0, 2, 0.3, 1.2, 0), "a friend 2 blocks ahead walking on: queueing");
        check(!Stall.queueing(0, 0, 1, 0, -2, 0, 1.2, 0), "one behind is not");
        check(!Stall.queueing(0, 0, 1, 0, 6, 0, 1.2, 0), "one far ahead is not");
        check(!Stall.queueing(0, 0, 1, 0, 2, 0, 0.1, 0), "one standing still is not");
        check(!Stall.queueing(0, 0, 1, 0, 2, 0, -1.2, 0), "one coming the other way is not");
        check(!Stall.queueing(0, 0, 1, 0, 0.5, 3, 1.2, 0), "one off to the side is not ahead");
        // measured moving: the friend's own beat gives its speed; a stale measure is not moving
        var fr = new Stall.State();
        Stall.track(fr, 100, 10, 0);
        check(!Stall.moving(fr, 100), "no speed from one measure");
        Stall.track(fr, 110, 11.2, 0);
        check(Stall.moving(fr, 110) && Stall.moving(fr, 130) && !Stall.moving(fr, 140), "moving after a beat, not once the measure is stale");
        Stall.track(fr, 120, 11.3, 0);
        check(!Stall.moving(fr, 120), "a tenth of a block a beat is standing");

        // 6. a troop pressed behind a mover: its clock is held (no rung, no jump), for QUEUE_MAX ticks; then it runs
        var q = new Stall.State();
        int rungs = 0;
        for (long tt = 0; tt <= Stall.QUEUE_MAX; tt += 10) if (Stall.step(q, tt, 100, 0, 160, 0, 0, 60, false, true) != Stall.NONE) rungs++;
        check(rungs == 0 && q.held, "a moving queue holds the clock for its whole hold: " + rungs);
        int jumped = 0, fired = 0;
        for (long tt = Stall.QUEUE_MAX + 10; tt <= Stall.QUEUE_MAX + 700; tt += 10) { int r = Stall.step(q, tt, 100, 0, 160, 0, 0, 60, false, true); if (r == Stall.JUMP) jumped++; if (r != Stall.NONE) fired++; }
        check(!q.held && fired >= 4 && jumped >= 1, "a ring that never gets anywhere stops being a queue: " + fired + " rungs");
        // the same troop, the friend stops: the clock runs from that beat, the ladder as ever
        var q2 = new Stall.State();
        for (long tt = 0; tt <= 300; tt += 10) Stall.step(q2, tt, 100, 0, 160, 0, 0, 60, false, true);
        List<Integer> got = new ArrayList<>();
        for (long tt = 310; tt <= 1000; tt += 10) { int r = Stall.step(q2, tt, 100, 0, 160, 0, 0, 60, false, false); if (r != Stall.NONE) got.add(r); }
        check(got.size() >= 5 && got.subList(0, 5).equals(List.of(Stall.REPATH, Stall.SIDESTEP, Stall.DETOUR, Stall.REGROUP, Stall.JUMP)), "the ladder runs from when the queue stops: " + got);
        // a queued troop is never flagged on a map with no field either
        var q3 = new Stall.State();
        int f3 = 0;
        for (long tt = 0; tt <= 500; tt += 10) if (Stall.step(q3, tt, 100, 0, 160, 0, 0, Double.NaN, false, true) != Stall.NONE) f3++;
        check(f3 == 0, "no field: a queued troop is not flagged");
        System.out.println("FileThroughCheck: " + count + " checks passed");
    }
}
