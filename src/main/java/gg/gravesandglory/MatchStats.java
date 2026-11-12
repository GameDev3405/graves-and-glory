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
        /** Enemy troops killed by the commander's own blow or shot (also counted in kills). */
        public int commanderKills;
        /** Enemy commanders downed and credited to this army. */
        public int commanderDowns;
        /** Times this army's commander was downed. */
        public int deaths;
        /** Damage the commander's own blows and shots dealt to enemy troops and commanders, after mitigation, rounded from damageExact. */
        public int damageDealt;
        /** The unrounded running total behind damageDealt. */
        public double damageExact;
        /** Capture points this army took: every seated squad of the capturing team with a troop or commander on the point at the capture. */
        public int captures;
        /** Gold paid for training and gear this round, refunds taken off. */
        public int goldSpent;
        /** Troops that finished training. */
        public int trained;
        /** Gear upgrades bought. */
        public int upgrades;
        /** Orders the commander gave. */
        public int orders;
        /** This army's living non-temporary troops at the end of the fight (set when the match ends). */
        public int kept;
        /** Every spawn or refill of a troop type this round, by troop id. */
        public final Map<String, Integer> fielded = new LinkedHashMap<>();
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
     * The results page's line for a LevelRow: "reached Knight-Captain / 2 at Rank 3 / Weapon III, Armour II". reachedName is the troop's name at its highest level;
     * null, or the troop's own name (an Epic or Legendary keeps its name), reads "reached level 3". "no Rank gains" and "no gear bought" when nothing happened.
     */
    public static String levelText(LevelRow r, String troopName, String reachedName) {
        String reached = r.highest() < 2 ? "no Rank gains" : "reached " + reachedPart(r, troopName, reachedName);
        String threes = r.level3s() > 0 ? " / " + r.level3s() + " at Rank 3" : "";
        List<String> gear = new ArrayList<>();
        if (r.weapon() > 1) gear.add("Weapon " + roman(r.weapon()));
        if (r.armour() > 1) gear.add("Armour " + roman(r.armour()));
        return reached + threes + " / " + (gear.isEmpty() ? "no gear bought" : String.join(", ", gear));
    }
    /** The level a row reached: the troop's name at its highest level, or "Rank N" when it has none (null, empty, or the troop's own name: an Epic or Legendary). */
    static String reachedPart(LevelRow r, String troopName, String reachedName) {
        return reachedName == null || reachedName.isEmpty() || reachedName.equals(troopName) ? "Rank " + r.highest() : reachedName;
    }
    /**
     * The same line for the results page's two-column form (more than three troop types, half a page each): "Knight-Captain x2 / W III A III". The level name
     * (or "Rank 3"), x and how many reached level 3, then W and A for the weapon and armour tiers bought; "no Rank gains" and "no gear" when nothing happened.
     */
    public static String levelShort(LevelRow r, String troopName, String reachedName) {
        String reached = r.highest() < 2 ? "no Rank gains" : reachedPart(r, troopName, reachedName);
        String threes = r.level3s() > 0 ? " x" + r.level3s() : "";
        List<String> gear = new ArrayList<>();
        if (r.weapon() > 1) gear.add("W " + roman(r.weapon()));
        if (r.armour() > 1) gear.add("A " + roman(r.armour()));
        return reached + threes + " / " + (gear.isEmpty() ? "no gear" : String.join(" ", gear));
    }
    private static final int[] ROMAN_VALUE = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_TEXT = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    /** The one Roman numeral every tier and level on screen uses (gear tiers, trait tiers, mastery levels, the refusal copy, the results page): I, II, III, IV and on; nothing for 0 or less, so a House with no trait tier draws no numeral. Above 3999 the digits. */
    public static String roman(int n) {
        if (n <= 0) return "";
        if (n >= 4000) return Integer.toString(n);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < ROMAN_VALUE.length; i++) while (n >= ROMAN_VALUE[i]) { out.append(ROMAN_TEXT[i]); n -= ROMAN_VALUE[i]; }
        return out.toString();
    }
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
