package gg.gravesandglory;

import java.util.*;

/** Read-only adapter from the v2 collection and recruitment sheets to the War Table. */
final class RecruitView {
    private RecruitView() {}

    /** A House on the rod: its House id, its name and the cloth its banner is woven in
     *  (recruit/banner-<cloth>). */
    record House(String id, String name, String cloth) {}
    /** A troop on a House's roll: its id and name, its group's label ("Infantry"), whether the commander has recruited it, its stars
     *  (0 to 3; 0 draws none), its rarity ("" draws none) and whether it is locked (a Legendary not yet owned). */
    record Row(String troopId, String name, String group, boolean owned, int stars, String rarity, boolean locked) {}

    /** The Houses in rod order: the five Houses of the sheet (Spec.HOUSES), each woven in the cloth of its own id. */
    static List<House> houses() {
        List<House> out = new ArrayList<>();
        for (var h : Spec.HOUSES) out.add(new House(h.id(), h.name(), h.id()));
        return out;
    }

    /** The House's troops in Spec.TROOPS order, each with the commander's stars in it (Phase 2 merge: read from the v2 collection).
     *  A Legendary is locked until owned and never comes from a letter. Pact granting is not implemented in this preview. */
    static List<Row> roll(String houseId) {
        List<Row> out = new ArrayList<>();
        for (var t : Spec.TROOPS)
            if (t.house().equals(houseId)) {
                var o = CampaignClient.profile.owned.get(t.id());
                out.add(new Row(t.id(), name(t.id()), group(t.group()), o != null, o == null ? 0 : o.stars, t.rarity(), o == null && t.rarity().equals("legendary")));
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
        for (var r : roll(houseId)) if (!r.rarity().equals("legendary")) n++;
        return n;
    }

    /** The gold a letter costs (Enlist). */
    static int cost() { return Spec.RECRUITMENT[0].rollCost(); }

    /** A troop's collection display name, matching the stage used by its portrait. */
    static String name(String troop) {
        var t = ArmyCards.spec(troop);
        var o = CampaignClient.profile.owned.get(troop);
        return t == null ? troop : ArmyMobs.stageName(t, o == null ? 1 : o.stage);
    }

    static boolean fieldable(String house) { return RosterMath.fieldableHouses().contains(house); }

    /** Actual weighted pool, including owned troops. The Epic pity overrides these base chances. */
    static List<String> odds(String house) {
        int total = 0;
        for (var r : roll(house)) {
            var rarity = RosterMath.rarityRow(r.rarity());
            if (rarity != null) total += rarity.rollWeight();
        }
        List<String> lines = new ArrayList<>();
        if (pityLeft(house) == 1) lines.add("Next letter: guaranteed Epic");
        for (var rarity : Spec.RARITIES) {
            if (rarity.rollWeight() <= 0) continue;
            int count = 0;
            for (var r : roll(house)) if (r.rarity().equals(rarity.id())) count++;
            if (count > 0 && total > 0) lines.add(CommandMode.label(rarity.id()) + " (each): " + String.format(Locale.ROOT, "%.1f%%", 100.0 * rarity.rollWeight() / total));
        }
        if (pityLeft(house) != 1) lines.add("Epic within " + pityLeft(house) + " letters");
        lines.add("Duplicates raise stars");
        lines.add("Full-star copies give shards");
        lines.add("Legendaries are not rolled");
        return lines;
    }

    /** Existing saved state only: no mutation or unimplemented progression actions. */
    static List<String> details(Row row, String house) {
        var o = CampaignClient.profile.owned.get(row.troopId());
        String state = o == null ? (row.locked() ? "Legendary: not in letters" : "Not yet recruited")
                : "Stage " + o.stage + " / Mastery " + o.mastery + " / Copies " + o.copies;
        String availability = fieldable(house) ? "House ready for battle" : "Collection only: not fieldable yet";
        return List.of(state, availability, "House shards: " + CampaignClient.profile.shards.getOrDefault(house, 0));
    }
}
