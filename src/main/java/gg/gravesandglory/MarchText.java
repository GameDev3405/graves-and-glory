package gg.gravesandglory;

/**
 * The Long March's display words as plain functions (plan 2C.5): what a battle's bonus pays and how the campaign card names its battle.
 * The War Table card, the briefing and the report read these, so the three say a bonus the same way. Display only; nothing here decides
 * a reward (MarchMath does).
 */
final class MarchText {
    private MarchText() {}

    /** What a bonus pays, with the House named: "+5 Gilded shards, +40 gear shards". */
    static String reward(Spec.BonusObjectivesRow bonus, Spec.CampaignBattlesRow row) {
        return "+" + bonus.shards() + " " + RosterMath.houseName(row.house()) + " shards, +" + bonus.gold() + " gear shards";
    }

    /** What a bonus pays in the card's narrow line: "+5 shards, +40 gear shards". */
    static String rewardShort(Spec.BonusObjectivesRow bonus) {
        return "+" + bonus.shards() + " shards, +" + bonus.gold() + " gear shards";
    }
}
