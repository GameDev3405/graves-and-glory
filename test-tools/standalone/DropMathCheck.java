package gg.gravesandglory;

import java.math.BigInteger;
import java.util.*;

/** The drop board's odds (design 2026-11-14, section 2): the table, the exact average, the cap, the draw and the scaled refund. Never shipped. */
public final class DropMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    /** Sum over slots of chance x boosted weight share, as an exact fraction {num, den}, for one tier of a pool. */
    static BigInteger[] average(long[] weights, boolean[] boosted, int tier) {
        BigInteger num = BigInteger.ZERO, den = BigInteger.ONE;
        for (var s : Spec.DROPSLOTS) {
            long[] b = DropMath.boost(weights, boosted, s.multiplier());
            long total = 0;
            for (long w : b) total += w;
            // num/den += chance * b[tier] / (chanceSum * total)
            BigInteger n = BigInteger.valueOf(s.chance()).multiply(BigInteger.valueOf(b[tier])), d = BigInteger.valueOf(DropMath.chanceSum()).multiply(BigInteger.valueOf(total));
            num = num.multiply(d).add(n.multiply(den));
            den = den.multiply(d);
            BigInteger g = num.gcd(den);
            num = num.divide(g); den = den.divide(g);
        }
        return new BigInteger[]{num, den};
    }

    static void exact(String name, long[] weights, boolean[] boosted) {
        long total = 0;
        for (long w : weights) total += w;
        for (int i = 0; i < weights.length; i++) {
            BigInteger[] a = average(weights, boosted, i);
            // a = weights[i] / total exactly
            check(a[0].multiply(BigInteger.valueOf(total)).equals(a[1].multiply(BigInteger.valueOf(weights[i]))), name + ": tier " + i + " averages " + a[0] + "/" + a[1] + ", not " + weights[i] + "/" + total);
        }
        for (var s : Spec.DROPSLOTS) for (long w : DropMath.boost(weights, boosted, s.multiplier())) check(w >= 0, name + ": a negative weight at x" + s.multiplier());
    }

    public static void main(String[] args) {
        // the table: nine slots, mirrored, edges highest, chances sum to 1000
        check(DropMath.slots() == 9 && DropMath.chanceSum() == 1000, "nine slots summing to 1000");
        for (int i = 0; i < 4; i++) {
            check(DropMath.multiplier(i) == DropMath.multiplier(8 - i) && Spec.DROPSLOTS[i].chance() == Spec.DROPSLOTS[8 - i].chance(), "mirrored at " + i);
            check(DropMath.multiplier(i) > DropMath.multiplier(i + 1) && Spec.DROPSLOTS[i].chance() < Spec.DROPSLOTS[i + 1].chance(), "higher and rarer outward at " + i);
        }
        check(DropMath.multiplier(0) == 10 && DropMath.multiplier(4) == 1, "x10 at the edge, x1 in the middle");
        check(Math.abs(DropMath.mean() - 2.3) < 1e-12, "the bell table's mean multiplier is 2.30, got " + DropMath.mean());

        // gear: every rarity's average chance is today's, exactly; the same on a pity roll (epic and legendary only, legendary boosted)
        var gr = Spec.GEARRARITIES;
        long[] gear = new long[gr.length];
        boolean[] gearBoost = new boolean[gr.length];
        for (int i = 0; i < gr.length; i++) { gear[i] = gr[i].rollWeight(); gearBoost[i] = DropMath.boosted(gr[i].id()); }
        check(!gearBoost[0] && !gearBoost[1] && gearBoost[2] && gearBoost[3] && gearBoost[4], "rare, epic and legendary are boosted; common and uncommon are not");
        exact("gear", gear, gearBoost);
        exact("gear pity", new long[]{6, 1}, new boolean[]{false, true});

        // units: every House's pool (commons, rares, the Epic)
        for (var h : Spec.HOUSES) {
            int commons = 0, rares = 0, epics = 0;
            for (var t : Spec.TROOPS) if (t.house().equals(h.id())) switch (t.rarity()) { case "common" -> commons++; case "rare" -> rares++; case "epic" -> epics++; default -> {} }
            if (commons + rares + epics == 0) continue;
            long[] w = {(long) commons * RosterMath.rarityRow("common").rollWeight(), (long) rares * RosterMath.rarityRow("rare").rollWeight(), (long) epics * RosterMath.rarityRow("epic").rollWeight()};
            exact("units " + h.id(), w, new boolean[]{false, true, true});
        }

        // pools with nothing to move: unchanged
        check(Arrays.equals(DropMath.boost(new long[]{5, 7}, new boolean[]{true, true}, 10), new long[]{5, 7}), "an all-boosted pool is unchanged");
        check(Arrays.equals(DropMath.boost(new long[]{5, 7}, new boolean[]{false, false}, 10), new long[]{5, 7}), "a pool with nothing boosted is unchanged");

        // x10 on gear: rare or better 95.65%, x1: 9.57%
        long[] x10 = DropMath.boost(gear, gearBoost, 10), x1 = DropMath.boost(gear, gearBoost, 1);
        double hi10 = (double) (x10[2] + x10[3] + x10[4]) / Arrays.stream(x10).sum(), hi1 = (double) (x1[2] + x1[3] + x1[4]) / Arrays.stream(x1).sum();
        check(Math.abs(hi10 - 0.22 * 10 / 2.3) < 1e-12 && Math.abs(hi1 - 0.22 / 2.3) < 1e-12, "x10 gives 95.65% rare or better and x1 9.57%, got " + hi10 + " and " + hi1);

        // the slot draw follows the table (400k draws, each slot within 4 sigma)
        var rng = new SplittableRandom(7);
        int n = 400000;
        int[] seen = new int[9];
        for (int i = 0; i < n; i++) seen[DropMath.drawSlot(rng)]++;
        for (int i = 0; i < 9; i++) {
            double p = Spec.DROPSLOTS[i].chance() / 1000.0, sigma = Math.sqrt(n * p * (1 - p));
            check(Math.abs(seen[i] - n * p) < 4 * sigma, "slot " + i + " drawn " + seen[i] + " of " + n + ", expected " + n * p);
        }

        // the scaled refund: whole numbers whose average over the slots is the amount
        double sum = 0;
        int rounds = 200000;
        for (int i = 0; i < rounds; i++) sum += DropMath.scale(56, DropMath.multiplier(DropMath.drawSlot(rng)), rng);
        check(Math.abs(sum / rounds - 56) < 0.6, "a refund of 56 averages 56 over the slots, got " + sum / rounds);
        check(DropMath.scale(0, 10, rng) == 0, "nothing scales to nothing");

        System.out.println("DropMathCheck: " + checks + " checks passed");
    }
}
