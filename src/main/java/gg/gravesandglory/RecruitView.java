package gg.gravesandglory;

import java.util.*;

/** What the War Table recruit page shows, read from the camp state (UI plan Task 6): the Houses on the rod, each House's muster
 *  roll, how many letters are left and what a letter costs. Phase 1 has two Houses (the factions), no rarity, stars, locks or
 *  pity; Phase 2 replaces these bodies, and nothing else reads rarity, stars or pity. */
final class RecruitView {
    private RecruitView() {}

    /** A House on the rod: its id (a Spec.FACTIONS id in Phase 1), its name and the cloth its banner is woven in
     *  (recruit/banner-<cloth>). */
    record House(String id, String name, String cloth) {}
    /** A troop on a House's roll: its id and name, its group's label ("Infantry"), whether the commander has recruited it, its stars
     *  (0 to 3; 0 draws none), its rarity ("" draws none) and whether it is locked (not to be drawn yet). */
    record Row(String troopId, String name, String group, boolean owned, int stars, String rarity, boolean locked) {}

    /** The Houses in rod order: the five Houses of the sheet (Spec.HOUSES), each woven in the cloth of its own id. */
    static List<House> houses() {
        List<House> out = new ArrayList<>();
        for (var h : Spec.HOUSES) out.add(new House(h.id(), h.name(), h.id()));
        return out;
    }

    /** The House's troops in Spec.TROOPS order, each with the commander's stars in it (Phase 2 merge: read from the v2 collection).
     *  A Legendary is locked until owned: it comes from a Pact, not from a letter. */
    static List<Row> roll(String houseId) {
        List<Row> out = new ArrayList<>();
        for (var t : Spec.TROOPS)
            if (t.house().equals(houseId)) {
                var o = CampaignClient.profile.owned.get(t.id());
                out.add(new Row(t.id(), t.name(), group(t.group()), o != null, o == null ? 0 : o.stars, t.rarity(), o == null && t.rarity().equals("legendary")));
            }
        return out;
    }

    /** A group id's label from Spec.GROUPS ("infantry" reads "Infantry"). */
    static String group(String id) {
        for (var g : Spec.GROUPS) if (g.id().equals(id)) return g.label();
        return CommandMode.label(id);
    }

    /** Letters until the next Epic is sure to come in this House (Phase 2 pity). */
    static int pityLeft(String houseId) { return Math.max(1, Spec.RECRUITMENT[0].epicPity() - CampaignClient.profile.pity.getOrDefault(houseId, 0)); }

    /** The House's troops a letter can bring: every one but the Legendary, a duplicate raising stars (Phase 2). */
    static int newLeft(String houseId) {
        int n = 0;
        for (var r : roll(houseId)) if (!r.locked()) n++;
        return n;
    }

    /** The gold a letter costs (Enlist). */
    static int cost() { return Spec.CAMPAIGN[0].rollCost(); }

    /** The gold the letter's Choose costs, for a troop picked by name. */
    static int chooseCost() { return Spec.RECRUITMENT[0].chosenCost(); }
}
