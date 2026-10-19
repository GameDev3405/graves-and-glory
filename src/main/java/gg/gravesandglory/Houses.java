package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import java.util.*;

/**
 * House traits (spec 1.3). A squad's trait tier in a House comes from the slot weights of its living troops of that House against the
 * houseTraits sheet's slot thresholds; the tiers feed the HUD strip, the trait seconds of the battle report and, in each House's task, the
 * trait multipliers. The pure maths is RosterMath and CounterMath.
 */
final class Houses {
    private Houses() {}

    private static Spec.HouseTraitsRow[] cachedRows;
    private static Map<String, int[]> cachedThresholds = Map.of();
    /** The slot thresholds of a House's tiers I to III (houseTraits.slots), in tier order; empty for a House the sheet gives no trait. */
    static synchronized int[] thresholds(String house) {
        if (cachedRows != Spec.HOUSETRAITS) {
            Map<String, int[]> map = new HashMap<>();
            for (var h : Spec.HOUSES) map.put(h.id(), Arrays.stream(Spec.HOUSETRAITS).filter(t -> t.house().equals(h.id())).sorted(Comparator.comparingInt(Spec.HouseTraitsRow::tier)).mapToInt(Spec.HouseTraitsRow::slots).toArray());
            cachedThresholds = map;
            cachedRows = Spec.HOUSETRAITS;
        }
        return cachedThresholds.getOrDefault(house, new int[0]);
    }

    /** Sets the squad's House tiers from its living troops' slot weights, in House order, keeping only the Houses at tier I or higher. Temporary troops do not count. */
    static void recompute(GravesAndGlory.Squad s) {
        var battle = GravesAndGlory.battle;
        if (battle == null) return;
        Map<String, Integer> slots = new HashMap<>();
        for (var u : battle.units) if (u.squad == s && !u.temporary && u.mob.isAlive()) slots.merge(u.spec.house(), u.spec.slotWeight(), Integer::sum);
        s.traits.clear();
        for (var h : Spec.HOUSES) { int tier = RosterMath.traitTier(slots.getOrDefault(h.id(), 0), thresholds(h.id())); if (tier > 0) s.traits.put(h.id(), tier); }
    }

    /** The tiers a squad's banner reaches, House order, tier I or higher only; battle rewards read them. */
    static Map<String, Integer> bannerTiers(GravesAndGlory.Squad s) {
        Map<String, Integer> slots = RosterMath.houseSlots(s.banner, RosterMath.Catalog.fromSpec()), out = new LinkedHashMap<>();
        for (var h : Spec.HOUSES) { int tier = RosterMath.traitTier(slots.getOrDefault(h.id(), 0), thresholds(h.id())); if (tier > 0) out.put(h.id(), tier); }
        return out;
    }

    /** The trait share of the factor in Combat.resolve: the attacker's House traits and the target's. 1 until each House's task fills in its trait. */
    static double traitFactor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target, String kind) { return 1; }

    /** Once a second while a round runs: refreshes every seated squad's tiers and adds a second to the trait seconds of each House at tier I or higher. */
    static void tick(GravesAndGlory.Battle b) {
        for (var s : b.squads.values()) {
            if (!s.seated) continue;
            recompute(s);
            for (String house : s.traits.keySet()) s.stats.traitSeconds.merge(house, 1, Integer::sum);
        }
    }
}
