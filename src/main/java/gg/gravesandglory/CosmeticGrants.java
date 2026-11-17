package gg.gravesandglory;

import java.util.*;

/**
 * Cosmetic grants (plan 2026-11-16-cosmetics-render, section 3): idempotent and retroactive. apply reads a profile's peaks (its highest
 * Renown level in any House, its highest troop Mastery, its stored Founders claims), adds every earned id to Profile.cosmetics and returns
 * the ids that are new, for a notice. Profile.sanitize runs it (so every load, restore and migration grants what a profile already earned);
 * Campaign.finish and the Mastery ledger run it again before their save, to name the new ones. When skin_proven is first granted it is
 * equipped, which keeps the old behaviour (every Mastery II troop wore the trim). Pure, so CosmeticsCheck runs it.
 */
public final class CosmeticGrants {
    private CosmeticGrants() {}

    /** The Renown level thresholds, in level order. */
    static int[] renownThresholds() {
        return Arrays.stream(Spec.RENOWN).sorted(Comparator.comparingInt(Spec.RenownRow::level)).mapToInt(Spec.RenownRow::points).toArray();
    }

    /** The highest Renown level any House of the profile has reached. */
    static int maxRenown(Profile p) {
        int best = 0; int[] t = renownThresholds();
        if (p.renown != null) for (Integer points : p.renown.values()) if (points != null) best = Math.max(best, RosterMath.renownLevel(points, t));
        return best;
    }

    /** The highest Mastery level of any owned troop. */
    static int maxMastery(Profile p) {
        int best = 0;
        if (p.owned != null) for (var o : p.owned.values()) if (o != null) best = Math.max(best, o.mastery);
        return best;
    }

    /** Grants every earned id the profile lacks; returns those new ids in catalogue order (empty when nothing changed). */
    public static List<String> apply(Profile p) {
        if (p == null) return List.of();
        if (p.cosmetics == null) p.cosmetics = new LinkedHashSet<>();
        if (p.cosmeticSlots == null) p.cosmeticSlots = new LinkedHashMap<>();
        var claims = p.progression == null ? null : p.progression.claims;
        List<String> fresh = new ArrayList<>();
        for (String id : Cosmetics.earned(maxRenown(p), maxMastery(p), claims)) if (p.cosmetics.add(id)) fresh.add(id);
        // Proven goes on when first granted, and whenever it is owned with the skin slot never set (a rollback or an old save); an explicit none stays
        if (fresh.contains("skin_proven") || p.cosmetics.contains("skin_proven") && !p.cosmeticSlots.containsKey(Cosmetics.SKIN)) p.cosmeticSlots.put(Cosmetics.SKIN, "skin_proven");
        return fresh;
    }

    /** "New cosmetic: Renown Pennant" for each new id, for the notice and the report's reward rows. */
    public static List<String> lines(List<String> fresh) {
        List<String> out = new ArrayList<>();
        for (String id : fresh) out.add("New cosmetic: " + Cosmetics.name(id));
        return out;
    }
}
