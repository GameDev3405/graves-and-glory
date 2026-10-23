package gg.gravesandglory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure match statistics shared by the server and the bot simulation. No Minecraft types, so the standalone check compiles it. */
public final class MatchStats {
    private MatchStats() {}
    /** One commander's tally for the current round; Battle.startRound gives every seated squad a fresh one. */
    public static final class SquadStats {
        /** Gold earned this round from income and kills; starting gold and refunds do not count. */
        public int goldEarned;
        /** Enemy troops killed by this army's troops, its commander, or a shot of theirs that landed after the shooter fell. */
        public int kills;
        /** This army's own troops that died, whatever killed them. */
        public int lost;
        /** Blows this army's troops landed on a flank or rear (the counters row C16) and shots that did (C16s), this round: the flank asymmetry measured, not inferred (Combat.resolve counts; BotSimulation writes them per side). */
        public int flankHits, flankShots;
        /** Seconds with a troop or the commander inside any objective's radius; one per second however many points. */
        public int objectiveSeconds;
        /** Seconds each House's trait was at tier I or higher for this army this round, by House id (Houses.tick adds one a second). */
        public final Map<String, Integer> traitSeconds = new LinkedHashMap<>();
        /** This round's kills by troop id, for the mastery ledger: a troop's own blow, or a death in the credit window after it. A commander's kill and a temporary troop's (a Grave Bloom Risen) count in kills only. */
        public final Map<String, Integer> troopKills = new LinkedHashMap<>();
    }
    /** One House row of the battle report: the banner's trait tier in that House (0 when its slots reach none), the seconds its trait held this round, and the shards and Renown the match paid it. */
    public record HouseRow(String house, int tier, int traitSeconds, int shards, int renown) {}
    /** The report's House rows: one for every House the banner has slots in, in houseOrder. Tiers, trait seconds, shards and Renown are read by House id, 0 when absent. */
    public static List<HouseRow> houseRows(List<String> houseOrder, Map<String, Integer> bannerSlots, Map<String, Integer> tiers, Map<String, Integer> traitSeconds, Map<String, Integer> shards, Map<String, Integer> renown) {
        List<HouseRow> rows = new ArrayList<>();
        for (String h : houseOrder)
            if (bannerSlots.getOrDefault(h, 0) > 0) rows.add(new HouseRow(h, tiers.getOrDefault(h, 0), traitSeconds.getOrDefault(h, 0), shards.getOrDefault(h, 0), renown.getOrDefault(h, 0)));
        return rows;
    }
    /** One troop type's row of the report's levels block (in-match progression design 7): the highest level a soldier of it reached this round (1 to 3), how many reached level 3, and the weapon and armour tier the army bought for it (1: none). */
    public record LevelRow(String troop, int highest, int level3s, int weapon, int armour) {}
    /**
     * The results page's line for a LevelRow: "reached Knight-Captain / 2 at level 3 / Weapon III, Armour II". reachedName is the troop's name at its highest level;
     * null, or the troop's own name (an Epic or Legendary keeps its name), reads "reached level 3". "no level ups" and "no gear bought" when nothing happened.
     */
    public static String levelText(LevelRow r, String troopName, String reachedName) {
        String reached = r.highest() < 2 ? "no level ups" : "reached " + (reachedName == null || reachedName.isEmpty() || reachedName.equals(troopName) ? "level " + r.highest() : reachedName);
        String threes = r.level3s() > 0 ? " / " + r.level3s() + " at level 3" : "";
        List<String> gear = new ArrayList<>();
        if (r.weapon() > 1) gear.add("Weapon " + roman(r.weapon()));
        if (r.armour() > 1) gear.add("Armour " + roman(r.armour()));
        return reached + threes + " / " + (gear.isEmpty() ? "no gear bought" : String.join(", ", gear));
    }
    static String roman(int n) { return switch (n) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> Integer.toString(n); }; }
    /** Share of matches won, 0 when no match was played. */
    public static double winRate(int wins, int matches) {
        return matches == 0 ? 0 : (double) wins / matches;
    }
    /** The report's MVP: the row with the most kills, ties broken by the most objective seconds (a full tie goes to the earlier row); -1 when there are no rows, or when the best row has no kills and no objective seconds, so a dead tie at nothing crowns nobody. */
    public static int mvp(List<SquadStats> rows) {
        int best = -1;
        for (int i = 0; i < rows.size(); i++) {
            SquadStats row = rows.get(i), top = best < 0 ? null : rows.get(best);
            if (top == null || row.kills > top.kills || row.kills == top.kills && row.objectiveSeconds > top.objectiveSeconds) best = i;
        }
        return best >= 0 && rows.get(best).kills == 0 && rows.get(best).objectiveSeconds == 0 ? -1 : best;
    }
    /** One second of holding: each owned point adds a second to its owner's row. seconds is [team][point], sized from the map's points; owners[i] is point i's team, -1 when nobody holds it. */
    public static void teamPointSeconds(int[][] seconds, int[] owners) {
        for (int i = 0; i < owners.length; i++)
            if (owners[i] >= 0 && owners[i] < seconds.length && i < seconds[owners[i]].length) seconds[owners[i]][i]++;
    }
}
