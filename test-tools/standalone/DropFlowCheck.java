package gg.gravesandglory;

import java.util.*;

/** The drop board's server bookkeeping (design 2026-11-14): a drop holds no value, double sends, auto-drop, skip, disconnect at each step, two rollers, a match starting; and the slot odds do not depend on the start. Never shipped. */
public final class DropFlowCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) {
        UUID a = new UUID(0, 1), b = new UUID(0, 2);
        var flow = new DropFlow();
        long now = 1000;

        // the plain course: begin, aim, release, path, reveal over
        var began = flow.begin(a, "Kort", "unit", 6, "{card}", "Joined", now);
        check(began.size() == 1 && began.get(0).stage == DropFlow.Stage.AIM && flow.active().size() == 1, "a roll begins one drop on the rail");
        var d = began.get(0);
        check(flow.aim(a, 2.25) && d.x == 2.25 && flow.aim(a, -40) && d.x == DropSim.MIN_X && !flow.aim(a, Double.NaN) && !flow.aim(b, 3), "the coin slides, clamped to the rail, for its roller only");
        check(flow.overdue(now + DropFlow.aimTicks() - 1).isEmpty(), "the aim timer has not run out yet");
        check(flow.release(a, 3.5) == d && d.stage == DropFlow.Stage.SEARCH && d.x == 3.5, "the release takes the place");
        check(flow.release(a, 7) == null && d.x == 3.5 && !flow.aim(a, 7), "a second release, or an aim after it, changes nothing");
        var path = DropSim.find(d.x, d.slot, 42);
        check(!flow.falling(a, d.id + 1, path, now) && !flow.falling(b, d.id, path, now), "a path for another drop is refused");
        check(!flow.falling(a, d.id, DropSim.find(d.x, 2, 42), now), "a path to another slot than the one drawn is refused");
        check(flow.falling(a, d.id, path, now) && d.stage == DropFlow.Stage.DROP && !flow.falling(a, d.id, path, now), "the path is taken once");
        long length = (long) Math.ceil(path.seconds() * 20) + DropFlow.ticks(Spec.DROPBOARD[0].flareTenths()) + DropFlow.ticks(Spec.DROPBOARD[0].revealTenths());
        check(flow.finished(now + length - 1).isEmpty(), "the drop is not over before its reveal");
        var done = flow.finished(now + length);
        check(done.size() == 1 && done.get(0) == d && d.card.equals("{card}") && flow.active().isEmpty() && flow.finished(now + length + 5).isEmpty(), "the drop ends once, with its card");

        // the aim timer: the coin drops where it stands
        d = flow.begin(a, "Kort", "gear", 0, "c2", "", now).get(0);
        flow.aim(a, 8.1);
        var late = flow.overdue(now + DropFlow.aimTicks());
        check(late.size() == 1 && late.get(0) == d && d.stage == DropFlow.Stage.SEARCH && d.x == 8.1 && flow.overdue(now + DropFlow.aimTicks() + 1).isEmpty(), "the timer drops the coin where it stands, once");
        check(flow.release(a, 1) == null, "a release after the timer changes nothing");

        // a skip or a disconnect at each step hands the card back exactly once
        check(flow.end(a) == d && flow.end(a) == null && flow.of(a) == null, "ending while the path is sought gives the card once");
        d = flow.begin(a, "Kort", "unit", 4, "c3", "", now).get(0);
        check(flow.end(a) == d && flow.release(a, 2) == null, "ending on the rail gives the card; nothing is left to release");
        d = flow.begin(a, "Kort", "unit", 4, "c4", "", now).get(0);
        flow.release(a, 4.5); flow.falling(a, d.id, DropSim.find(4.5, 4, 1), now);
        check(flow.end(a) == d && flow.finished(now + 100000).isEmpty(), "ending in the pegs gives the card; it does not finish a second time");
        // the path that arrives after the drop was ended is dropped
        d = flow.begin(a, "Kort", "unit", 4, "c5", "", now).get(0);
        flow.release(a, 4.5); flow.end(a);
        check(!flow.falling(a, d.id, DropSim.find(4.5, 4, 1), now), "a path arriving after the end is dropped");

        // a second roll while the first drops: the first is ended and its card handed back first
        var first = flow.begin(a, "Kort", "unit", 1, "one", "", now).get(0);
        var both = flow.begin(a, "Kort", "unit", 2, "two", "", now);
        check(both.size() == 2 && both.get(0) == first && both.get(1).card.equals("two") && both.get(1).id > first.id && flow.active().size() == 1, "a new roll ends the old drop and hands its card back");

        // two rollers at once: no queue, each his own drop
        var other = flow.begin(b, "Maff", "relic", 8, "three", "", now).get(0);
        check(flow.active().size() == 2 && flow.of(a) != other && flow.of(b) == other, "two commanders drop at once");
        flow.release(b, 6);
        check(flow.of(a).stage == DropFlow.Stage.AIM && other.stage == DropFlow.Stage.SEARCH, "one release does not move the other's coin");
        // a match starting ends them all
        var all = flow.endAll();
        check(all.size() == 2 && flow.active().isEmpty() && flow.endAll().isEmpty(), "a match starting ends every drop once");

        // every start has the table's odds: the slot is drawn before the place is known, and the coin lands in the drawn slot from any start
        var rng = new SplittableRandom(3);
        int[] seen = new int[9];
        int n = 100000;
        for (int i = 0; i < n; i++) seen[DropMath.drawSlot(rng)]++;
        for (int k = 0; k < 9; k++) {
            double p = Spec.DROPSLOTS[k].chance() / 1000.0, sigma = Math.sqrt(n * p * (1 - p));
            check(Math.abs(seen[k] - n * p) < 4.5 * sigma, "slot " + k + " comes " + seen[k] + " times in " + n + ", expected " + n * p);
        }
        double[] starts = {0.5, 4.5, 8.5};
        for (int i = 0; i < 300; i++) { int slot = DropMath.drawSlot(rng); double start = starts[i % 3]; check(DropSim.find(start, slot, rng.nextLong()).slot() == slot, "the coin lands in the drawn slot from " + start); }

        System.out.println("DropFlowCheck: " + checks + " checks passed");
    }
}
