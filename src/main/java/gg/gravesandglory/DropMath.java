package gg.gravesandglory;

import java.util.random.RandomGenerator;

/**
 * The drop board's odds (design 2026-11-14, section 2), pure and exact in whole numbers. A roll lands in one of the dropSlots (chance in
 * thousandths, multiplier m); the tiers of the boosted set have their chance multiplied by m / mean, where mean is the table's average
 * multiplier, and the tiers below share what is left in their old proportions. Averaged over the slots every tier's chance is the one
 * its weight gives with no board at all (DropMathCheck proves it in fractions), so the board moves no average.
 */
public final class DropMath {
    private DropMath() {}

    /** The number of slots, left to right. */
    public static int slots() { return Spec.DROPSLOTS.length; }
    public static int multiplier(int slot) { return Spec.DROPSLOTS[slot].multiplier(); }
    /** The sum of the slot chances (1000) and the sum of chance x multiplier: mean = meanK / chanceSum. */
    public static long chanceSum() { long s = 0; for (var r : Spec.DROPSLOTS) s += r.chance(); return s; }
    public static long meanK() { long s = 0; for (var r : Spec.DROPSLOTS) s += (long) r.chance() * r.multiplier(); return s; }
    public static double mean() { return (double) meanK() / chanceSum(); }
    /** What a roll on this multiplier gives against a roll with no board: m / mean (0.43 for x1, 4.35 for x10 on the bell table). */
    public static double factor(int multiplier) { return multiplier / mean(); }

    /** Whether a rarity is in the boosted set (dropBoard.boosted). */
    public static boolean boosted(String rarity) {
        for (String b : Spec.DROPBOARD[0].boosted().split(",")) if (b.equals(rarity)) return true;
        return false;
    }

    /** Draws a slot by the table's chances. */
    public static int drawSlot(RandomGenerator rng) {
        int pick = rng.nextInt((int) chanceSum());
        for (int i = 0; i < slots(); i++) {
            if (pick < Spec.DROPSLOTS[i].chance()) return i;
            pick -= Spec.DROPSLOTS[i].chance();
        }
        return slots() - 1;
    }

    /**
     * The weights of a pool after a landing on multiplier m, as whole numbers on one common scale: a boosted tier's chance is its old
     * chance x m / mean, the others share the rest in their old proportions. With nothing boosted, or nothing but boosted tiers, in the
     * pool the weights come back unchanged (there is nothing to move chance from or to). Never negative: preflight keeps the top
     * multiplier's boosted share at or under the whole.
     */
    public static long[] boost(long[] weights, boolean[] boosted, int m) {
        long total = 0, high = 0;
        for (int i = 0; i < weights.length; i++) { total += weights[i]; if (boosted[i]) high += weights[i]; }
        long low = total - high;
        if (high == 0 || low == 0) return weights.clone();
        long s = chanceSum(), t = meanK();
        // chance of a boosted tier: (w / total) x (m s / t); of a low tier: (1 - (high / total) (m s / t)) x w / low. Common denominator total x t x low.
        long rest = total * t - high * m * s;
        if (rest < 0) throw new IllegalStateException("drop board: multiplier " + m + " boosts a pool past 100%");
        long[] out = new long[weights.length];
        for (int i = 0; i < weights.length; i++) out[i] = boosted[i] ? weights[i] * m * s * low : rest * weights[i];
        return out;
    }

    /** Picks an index by weights. */
    public static int pick(long[] weights, RandomGenerator rng) {
        long total = 0;
        for (long w : weights) total += w;
        long pick = rng.nextLong(total);
        for (int i = 0; i < weights.length; i++) {
            if (pick < weights[i]) return i;
            pick -= weights[i];
        }
        return weights.length - 1;
    }

    /** A whole amount scaled by m / mean and rounded down with the remainder paid by chance, so its average is exactly the amount (a relic's duplicate refund). */
    public static int scale(int amount, int m, RandomGenerator rng) {
        long num = (long) amount * m * chanceSum(), den = meanK();
        long whole = num / den, part = num % den;
        return (int) (whole + (part > 0 && rng.nextLong(den) < part ? 1 : 0));
    }
}
