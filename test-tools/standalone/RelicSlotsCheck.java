package gg.gravesandglory;

/** The relic slots' clocks (gear-combat plan Task 5): a slot starts once, refuses while it cools or runs, slots are independent, and readyIn counts down. Never shipped. */
public final class RelicSlotsCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) {
        var r = new RelicSlots(3);
        check(r.size() == 3 && r.readyIn(0, 0) == 0 && !r.active(0, 0) && r.ready(0, 0), "a fresh slot is ready and idle");
        check(r.tryStart(0, 100, 600, 40), "slot 0 starts");
        check(r.active(0, 100) && r.active(0, 139) && !r.active(0, 140), "it runs for its duration, to the tick");
        check(r.activeFor(0, 120) == 20 && r.activeFor(0, 200) == 0, "activeFor counts the run down to 0");
        check(!r.tryStart(0, 120, 600, 40) && r.readyAt[0] == 700, "it refuses while running and the cooldown stands");
        check(!r.tryStart(0, 650, 600, 40) && r.readyAt[0] == 700, "it refuses while cooling");
        check(r.readyIn(0, 100) == 600 && r.readyIn(0, 400) == 300 && r.readyIn(0, 699) == 1 && r.readyIn(0, 700) == 0 && r.readyIn(0, 900) == 0, "readyIn counts down to 0");
        check(r.tryStart(1, 120, 300, 0) && r.readyIn(1, 120) == 300 && !r.active(1, 120), "slot 1 starts while slot 0 cools; a zero-length relic does not run");
        check(r.tryStart(2, 130, 0, 10) && r.readyIn(0, 130) == 570, "slot 2 is independent of both");
        check(r.tryStart(0, 700, 600, 40), "slot 0 starts again once its cooldown is over");
        r.data[0] = "mark";
        check(r.started(0), "a running slot has a run to end");
        r.stop(0);
        check(!r.active(0, 710) && !r.started(0) && r.data[0] == null && r.readyAt[0] == 1300, "stop ends the run and clears the data; the cooldown stands");
        r.clear();
        check(r.ready(0, 0) && r.ready(1, 0) && r.ready(2, 0) && !r.started(2), "clear readies every slot");
        check(!r.ready(3, 0) && !r.tryStart(-1, 0, 1, 1) && r.readyIn(5, 0) == 0 && !r.active(3, 0), "a slot past the end never starts");
        System.out.println("RelicSlotsCheck: " + checks + " checks passed");
    }
}
