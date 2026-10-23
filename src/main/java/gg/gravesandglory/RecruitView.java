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

    /** The Houses in rod order. Phase 1: Dawn in the gilded cloth, Dusk in the graveborn. */
    static List<House> houses() {
        return List.of(new House(Spec.FACTIONS[0].id(), Spec.FACTIONS[0].name(), "gilded"),
                new House(Spec.FACTIONS[1].id(), Spec.FACTIONS[1].name(), "graveborn"));
    }

    /** The House's troops in Spec.TROOPS order. */
    static List<Row> roll(String houseId) {
        List<Row> out = new ArrayList<>();
        for (var t : Spec.TROOPS)
            if (t.faction().equals(houseId))
                out.add(new Row(t.id(), t.name(), group(t.group()), CampaignClient.profile.unlocked.contains(t.id()), 0, "", false));
        return out;
    }

    /** A group id's label from Spec.GROUPS ("infantry" reads "Infantry"). */
    static String group(String id) {
        for (var g : Spec.GROUPS) if (g.id().equals(id)) return g.label();
        return CommandMode.label(id);
    }

    /** Letters until the next Epic is sure to come, or -1 where there is no such count (Phase 1). */
    static int pityLeft(String houseId) { return -1; }

    /** The House's troops a letter can still bring: not recruited and not locked. */
    static int newLeft(String houseId) {
        int n = 0;
        for (var r : roll(houseId)) if (!r.owned() && !r.locked()) n++;
        return n;
    }

    /** The gold a letter costs (Enlist). */
    static int cost() { return Spec.CAMPAIGN[0].rollCost(); }

    /** The gold the letter's Choose costs, for a troop picked by name. */
    static int chooseCost() { return Spec.CAMPAIGN[0].rollCost(); }
}
