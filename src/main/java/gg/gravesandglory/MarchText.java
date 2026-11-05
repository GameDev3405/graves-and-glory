package gg.gravesandglory;

/**
 * The Long March's display words as plain functions (plan 2C.5): what a battle's bonus pays and how the campaign card names its battle.
 * The War Table card, the briefing and the report read these, so the three say a bonus the same way. Display only; nothing here decides
 * a reward (MarchMath does).
 */
final class MarchText {
    private MarchText() {}

    /** What a bonus pays, with the House named: "+5 Gilded shards, +40 gold". */
    static String reward(Spec.BonusObjectivesRow bonus, Spec.CampaignBattlesRow row) {
        return "+" + bonus.shards() + " " + RosterMath.houseName(row.house()) + " shards, +" + bonus.gold() + " gold";
    }

    /** What a bonus pays in the card's narrow line: "+5 shards, +40 gold". */
    static String rewardShort(Spec.BonusObjectivesRow bonus) {
        return "+" + bonus.shards() + " shards, +" + bonus.gold() + " gold";
    }

    /** The card's second line: "Battle 2 of 6, Normal", with the sides ("3 v 3") once a battle seats more than one a side. */
    static String battleLine(Spec.CampaignBattlesRow row, int total, String tierName) {
        return "Battle " + row.order() + " of " + total + ", " + (row.size() > 1 ? row.size() + " v " + row.size() + ", " : "") + tierName;
    }
}
