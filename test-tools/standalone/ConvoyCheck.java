package gg.gravesandglory;

/** The Convoy's pure rules (ModeMath, spec 2026-10-31 section 3): wagon state, push speed, the road polyline, rollback floor,
 *  checkpoints, progress and overtime. Checked by javac alone, never shipped. */
public final class ConvoyCheck {
    static int checks, failed;
    static final boolean ALL = Boolean.getBoolean("modemath.all");
    static void check(boolean pass, String message) {
        if (!pass && !ALL) throw new AssertionError(message);
        if (!pass) { failed++; System.out.println("FAILED: " + message); return; }
        checks++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }
    public static void main(String[] args) {
        int idle = 200; // 10 s
        check(ModeMath.convoyState(1, 0, 0, idle).equals("moving") && ModeMath.convoyState(3, 0, 500, idle).equals("moving"), "a pusher and no defender moves it");
        check(ModeMath.convoyState(3, 1, 0, idle).equals("contested") && ModeMath.convoyState(0, 1, 900, idle).equals("contested"), "any single defender stops it, whatever the pushers");
        check(ModeMath.convoyState(0, 0, 199, idle).equals("idle") && ModeMath.convoyState(0, 0, 200, idle).equals("rollback"), "alone: idle under 10 s, then rolling back");
        check(ModeMath.convoyState(0, 0, 900, idle, true).equals("idle") && ModeMath.convoyState(0, 0, 900, idle, false).equals("rollback") && ModeMath.convoyState(0, 0, 199, idle, false).equals("idle") && ModeMath.convoyState(1, 0, 900, idle, true).equals("moving") && ModeMath.convoyState(0, 1, 900, idle, true).equals("contested"), "at its floor the wagon cannot roll back, so it shows idle (C05-F4)");
        double[] speeds = {1.2, 1.5, 1.8};
        check(ModeMath.pushSpeed(0, speeds) == 0 && ModeMath.pushSpeed(1, speeds) == 1.2 && ModeMath.pushSpeed(2, speeds) == 1.5 && ModeMath.pushSpeed(3, speeds) == 1.8 && ModeMath.pushSpeed(4, speeds) == 1.8, "push speed by pushers, capped at three");
        double[][] road = {{0, 0}, {30, 0}, {30, 40}, {60, 40}};
        double[] st = ModeMath.stations(road);
        check(near(st[0], 0) && near(st[1], 30) && near(st[2], 70) && near(st[3], 100), "stations are cumulative horizontal lengths");
        check(ModeMath.segment(st, 0) == 0 && ModeMath.segment(st, 29.9) == 0 && ModeMath.segment(st, 30) == 1 && ModeMath.segment(st, 100) == 2 && ModeMath.segment(st, 500) == 2, "segment lookup clamps to the last segment");
        double[] p = ModeMath.along(road, st, 50), end = ModeMath.along(road, st, 999), start = ModeMath.along(road, st, -5);
        check(near(p[0], 30) && near(p[1], 20) && near(end[0], 60) && near(end[1], 40) && near(start[0], 0) && near(start[1], 0), "points along the road, clamped at both ends");
        // the wagon's floor: a straight road 0..10 on x; floor 64, a bridge deck at 70 over x 4..6 (two floors there), a step up to 65 from x 8, no floor at x 3
        ModeMath.FloorNear under = (x, z, y) -> x == 3 ? Integer.MIN_VALUE : x >= 8 ? 65 : x >= 4 && x <= 6 ? (Math.abs(70 - y) < Math.abs(64 - y) ? 70 : 64) : 64;
        double[][] flat = {{0, 0}, {5, 0}, {10, 0}};
        int[] none = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int[] low = ModeMath.roadFloors(flat, ModeMath.stations(flat), none, 0.5, 64, under);
        check(low.length == 21 && ModeMath.roadFloor(low, 0.5, 5) == 64 && ModeMath.roadFloor(low, 0.5, 3.4) == 64 && ModeMath.roadFloor(low, 0.5, 8.2) == 65 && ModeMath.roadFloor(low, 0.5, 99) == 65 && ModeMath.roadFloor(low, 0.5, -1) == 64, "the road keeps its level under a deck, holds it over a column with no floor, and climbs a step");
        int[] deck = ModeMath.roadFloors(flat, ModeMath.stations(flat), new int[]{Integer.MIN_VALUE, 70, Integer.MIN_VALUE}, 0.5, 64, under);
        check(ModeMath.roadFloor(deck, 0.5, 4.5) == 64 && ModeMath.roadFloor(deck, 0.5, 5) == 70 && ModeMath.roadFloor(deck, 0.5, 6.4) == 70, "a node with a height of its own moves the road to that level from the node on");
        check(ModeMath.gradeFactor(1, 3, 0.25, 0.75, 1.25) == 0.75 && ModeMath.gradeFactor(-1, 3, 0.25, 0.75, 1.25) == 1.25 && ModeMath.gradeFactor(1, 5, 0.25, 0.75, 1.25) == 1 && ModeMath.gradeFactor(5, 0, 0.25, 0.75, 1.25) == 1, "grade: slower up a steep segment, faster down, flat otherwise");
        check(near(ModeMath.advance(10, "moving", 1.2, 0.6, 10, 0, 100), 22) && near(ModeMath.advance(99, "moving", 1.8, 0.6, 1, 0, 100), 100), "moving adds speed times seconds, stopping at the destination");
        check(near(ModeMath.advance(40, "rollback", 1.2, 0.6, 10, 30, 100), 34) && near(ModeMath.advance(32, "rollback", 1.2, 0.6, 10, 30, 100), 30), "rolling back at 0.6 never passes the last checkpoint");
        check(ModeMath.advance(40, "idle", 1.2, 0.6, 10, 30, 100) == 40 && ModeMath.advance(40, "contested", 1.8, 0.6, 10, 30, 100) == 40, "idle and contested hold still");
        double[] cps = {30, 70};
        check(ModeMath.passed(cps, 0) == 0 && ModeMath.passed(cps, 29.99) == 0 && ModeMath.passed(cps, 30) == 1 && ModeMath.passed(cps, 100) == 2, "checkpoints passed, a checkpoint counts on arrival");
        check(ModeMath.percent(0, 100) == 0 && ModeMath.percent(50, 100) == 50 && ModeMath.percent(99.99, 100) == 99 && ModeMath.percent(100, 100) == 100 && ModeMath.percent(5, 0) == 0, "progress percent, 100 only at the destination");
        check(ModeMath.overtime("moving", 66, 70, 6, 999, 60) && ModeMath.overtime("contested", 64, 70, 6, 999, 60) && !ModeMath.overtime("moving", 60, 70, 6, 999, 60) && !ModeMath.overtime("idle", 68, 70, 6, 999, 60) && ModeMath.overtime("idle", 68, 70, 6, 59, 60) && !ModeMath.overtime("idle", 68, 70, 6, 60, 60), "overtime: moving or contested within 6 of the next stop, plus 3 s grace");
        // A simulated push on the fixture road: one pusher for 20 s, then left alone for 10 s.
        double d = 0; int idleTicks = 0; String state = "idle";
        for (int t = 0; t < 600; t++) { state = ModeMath.convoyState(t < 400 ? 1 : 0, 0, idleTicks, idle); idleTicks = state.equals("idle") || state.equals("rollback") ? idleTicks + 1 : 0;
            double floor = ModeMath.passed(cps, d) == 0 ? 0 : cps[ModeMath.passed(cps, d) - 1]; d = ModeMath.advance(d, state, ModeMath.pushSpeed(1, speeds), 0.6, 0.05, floor, st[3]); }
        check(near(d, 24.0) && state.equals("idle") && idleTicks == 200, "simulated: 20 s at 1.2 moves 24 blocks, then 10 s idle holds it (roll-back starts on the next tick)");
        System.out.println("ConvoyCheck: " + checks + " checks passed" + (failed == 0 ? "" : ", " + failed + " failed"));
        if (failed > 0) System.exit(1);
    }
}
