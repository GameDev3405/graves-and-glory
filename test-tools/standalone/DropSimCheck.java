package gg.gravesandglory;

import java.util.*;

/** The drop board's coin simulation (design 2026-11-14): determinism, every start reaches every slot, drop length, no tunnelling, smooth playback, the wire form. Never shipped.
 *  With the argument "probe" it prints the plain (unsearched) landing spread and timings instead, for tuning. */
public final class DropSimCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("probe")) { probe(); return; }
        double min = Spec.DROPBOARD[0].dropMinTenths() / 10.0, max = Spec.DROPBOARD[0].dropMaxTenths() / 10.0;

        // deterministic: the same arguments give the same path
        var a = DropSim.find(2.3, 6, 99L);
        var b = DropSim.find(2.3, 6, 99L);
        check(a.slot() == 6 && Arrays.equals(a.xs(), b.xs()) && Arrays.equals(a.ys(), b.ys()) && a.hits().equals(b.hits()), "the same start, slot and seed give the same path");
        check(!Arrays.equals(a.xs(), DropSim.find(2.3, 6, 100L).xs()), "another seed gives another path");

        // every start reaches every slot, in the drop's length, without leaving the board or passing through a peg, and plays back smoothly
        double[] starts = {0.5, 1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5, 0.0, 9.0, 4.01};
        int steered = 0, runs = 0, pegHits = 0;
        double longest = 0, shortest = 99;
        for (double start : starts) for (int slot = 0; slot < 9; slot++) for (long seed = 1; seed <= 3; seed++) {
            var p = DropSim.find(start, slot, seed * 7919 + slot);
            runs++;
            if (p.steered()) steered++;
            check(p.slot() == slot, "from " + start + " to slot " + slot + ": landed in " + p.slot());
            check(p.seconds() >= min && p.seconds() <= max, "from " + start + " to slot " + slot + ": " + p.seconds() + " s, outside " + min + " to " + max);
            longest = Math.max(longest, p.seconds()); shortest = Math.min(shortest, p.seconds());
            check(Math.abs(p.xs()[0] - DropSim.clampStart(start)) < 1e-4 && Math.abs(p.ys()[0] - DropSim.START_Y) < 1e-4, "the path starts where the coin was placed");
            int last = p.xs().length - 1;
            check((int) Math.floor(p.xs()[last]) == slot && Math.abs(p.ys()[last] - (DropSim.FLOOR - DropSim.COIN)) < 1e-3, "the path ends at rest on the floor of slot " + slot);
            for (int i = 0; i <= last; i++) {
                double x = p.xs()[i], y = p.ys()[i];
                check(x >= DropSim.COIN - 1e-3 && x <= DropSim.WIDTH - DropSim.COIN + 1e-3 && y >= DropSim.START_Y - 1e-3 && y <= DropSim.FLOOR - DropSim.COIN + 1e-3, "sample " + i + " inside the board");
                if (i > 0) {
                    double step = Math.hypot(x - p.xs()[i - 1], y - p.ys()[i - 1]);
                    check(step < 0.35, "no jump over 0.35 block between 60 Hz samples, got " + step + " at " + i);
                    check(y > p.ys()[i - 1] - 0.2, "the coin never leaps upward more than a bounce allows at " + i);
                }
                // never inside a peg (a little give for the sample between two steps)
                for (int r = 0; r < DropSim.ROWS; r++) {
                    double py = DropSim.FIRST_ROW + r, off = r % 2 == 0 ? 0 : 0.5;
                    double px = Math.round(x - off) + off;
                    if (!DropSim.peg(r, (int) Math.round(x - off))) continue;
                    check(Math.hypot(x - px, y - py) > DropSim.COIN + DropSim.PEG - 0.03, "sample " + i + " is inside the peg at " + px + "," + py);
                }
            }
            for (var h : p.hits()) { check(h.sample() >= 0 && h.sample() <= last && h.speed() > 0, "a hit lies on the path"); if (h.what() >= 0) pegHits++; }
            check(p.hits().stream().anyMatch(h -> h.what() == DropSim.FLOOR_HIT), "the coin lands on the slot's floor");
            // playback at 144 fps: at() moves no further between frames than between samples
            double px0 = p.at(0)[0], py0 = p.at(0)[1];
            for (double t = 1 / 144.0; t < p.seconds(); t += 1 / 144.0) {
                double[] q = p.at(t);
                check(Math.hypot(q[0] - px0, q[1] - py0) < 0.2, "144 fps playback is smooth at " + t);
                px0 = q[0]; py0 = q[1];
            }
            check(Arrays.equals(p.at(-1), p.at(0)) && Arrays.equals(p.at(99), p.at(p.seconds())), "playback holds at both ends");
        }
        check(pegHits / (double) runs >= 6, "a drop strikes at least 6 pegs on average, got " + pegHits / (double) runs);
        System.out.println("  " + runs + " searched drops: " + steered + " leaned, " + String.format("%.2f", shortest) + " to " + String.format("%.2f", longest) + " s, " + String.format("%.1f", pegHits / (double) runs) + " peg hits a drop");

        // the wire form round-trips within a thousandth of a block and refuses nonsense
        var back = DropSim.decode(DropSim.encode(a));
        check(back.slot() == a.slot() && back.xs().length == a.xs().length && back.hits().equals(a.hits()) && back.steered() == a.steered(), "the wire form keeps the slot, the length and the hits");
        for (int i = 0; i < a.xs().length; i++) check(Math.abs(back.xs()[i] - a.xs()[i]) < 1e-3 && Math.abs(back.ys()[i] - a.ys()[i]) < 1e-3, "the wire form keeps sample " + i);
        check(DropSim.encode(a).length < 4096, "a path fits a small packet: " + DropSim.encode(a).length + " bytes");
        boolean refused = false;
        try { DropSim.decode(new byte[]{1, 2, 3}); } catch (IllegalArgumentException e) { refused = true; }
        check(refused, "cut bytes are refused");

        // a bad start is clamped to the rail
        check(DropSim.clampStart(-5) == DropSim.MIN_X && DropSim.clampStart(50) == DropSim.MAX_X && DropSim.clampStart(Double.NaN) == 4.5, "the start is clamped to the rail");

        System.out.println("DropSimCheck: " + checks + " checks passed");
    }

    static void probe() {
        for (double start : new double[]{0.5, 2.5, 4.5}) {
            int[] seen = new int[9];
            int fail = 0, n = 20000;
            double sum = 0, lo = 99, hi = 0, hitSum = 0;
            for (int i = 0; i < n; i++) {
                var p = DropSim.run(start, i * 31L + 5, 0, 0);
                if (p.slot() < 0) { fail++; continue; }
                seen[p.slot()]++; sum += p.seconds(); lo = Math.min(lo, p.seconds()); hi = Math.max(hi, p.seconds());
                hitSum += p.hits().stream().filter(h -> h.what() >= 0).count();
            }
            System.out.println("start " + start + ": slots " + Arrays.toString(seen) + " unfinished " + fail + " mean " + String.format("%.2f", sum / (n - fail)) + " s (" + String.format("%.2f", lo) + " to " + String.format("%.2f", hi) + "), peg hits " + String.format("%.1f", hitSum / (n - fail)));
        }
        long t = System.nanoTime();
        for (int s = 0; s < 9; s++) DropSim.find(0.5, 8 - s, s);
        System.out.println("nine far finds: " + (System.nanoTime() - t) / 1000000 + " ms");
    }
}
