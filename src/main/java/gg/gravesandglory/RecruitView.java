package gg.gravesandglory;

import java.util.*;

/** What the War Table recruit page shows, read from the camp state (UI plan Task 6): the Houses on the rod, each House's muster
 *  roll, what a letter and a chosen buy cost, the letter's action (RecruitPages.action) and the Odds note's words. */
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
        return Morale.label(id);
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
    static int cost() { return Spec.RECRUITMENT[0].rollCost(); }

    /** The gold a chosen buy costs (the letter's Buy tag on a Common or a Rare). */
    static int chooseCost() { return Spec.RECRUITMENT[0].chosenCost(); }

    /** The action the opened letter offers for a troop of the roll (a Buy, a Star, a Pact or shards), as RecruitPages.action gives it
     *  for the commander's collection, or null when it offers none (an Epic not yet found: it comes from letters only; a troop at full stars). */
    static RecruitPages.Action action(String troopId) {
        var t = ArmyCards.spec(troopId);
        return t == null ? null : RecruitPages.action(t, CampaignClient.profile);
    }

    /** What the letter says where its tag would stand when it has none: an Epic not yet found, or a troop at full stars; "" otherwise. */
    static String noAction(String troopId) {
        var t = ArmyCards.spec(troopId);
        if (t == null || action(troopId) != null) return "";
        return CampaignClient.profile.owned.containsKey(troopId) ? "Full stars" : "From letters only";
    }

    /** One rarity's chance of a letter: the rarity's id and its share of a roll in the House, in percent of 100. */
    record Odd(String rarity, double percent) {}

    /** The chance of each rarity a letter of the House can bring, from the sheets as Campaign.roll draws it: every Common weighs
     *  rarities.rollWeight of common, every Rare that of rare, the one Epic that of epic, and a Legendary never comes from a
     *  letter. Only the rarities the House has appear, in sheet order. */
    static List<Odd> odds(String houseId) {
        Map<String, Integer> count = new LinkedHashMap<>();
        for (var t : Spec.TROOPS) if (t.house().equals(houseId) && !t.rarity().equals("legendary")) count.merge(t.rarity(), 1, Integer::sum);
        int total = 0;
        for (var e : count.entrySet()) { var r = RosterMath.rarityRow(e.getKey()); total += e.getValue() * (r == null ? 0 : r.rollWeight()); }
        List<Odd> out = new ArrayList<>();
        for (var r : Spec.RARITIES) {
            Integer n = count.get(r.id());
            if (n != null && total > 0) out.add(new Odd(r.id(), 100.0 * n * r.rollWeight() / total));
        }
        return out;
    }

    /** "Common 86%, Rare 12%, Epic 2%": the chance of each rarity as the Odds note writes it. */
    static String oddsLine(String houseId) {
        List<String> parts = new ArrayList<>();
        for (var o : odds(houseId)) parts.add(Character.toUpperCase(o.rarity().charAt(0)) + o.rarity().substring(1) + " " + String.format(Locale.ROOT, "%.0f%%", o.percent()));
        return String.join(", ", parts);
    }

    /** The Odds note's rule for a troop already owned (a letter that brings it counts a copy toward its next star), and for a Legendary. */
    static final String COPY_RULE = "A troop you own gives a copy toward its next star.";
    static final String LEGENDARY_RULE = "A Legendary comes from a Pact, not a letter.";

    /** The Odds note's lines under its heading, before they are wrapped to the note's width: the chance of each rarity, the letters
     *  left until the House's Epic is sure, the copy rule and the Legendary's rule. */
    static List<String> note(String houseId) {
        return List.of(oddsLine(houseId), UiMath.pityLine(pityLeft(houseId)), COPY_RULE, LEGENDARY_RULE);
    }
}
