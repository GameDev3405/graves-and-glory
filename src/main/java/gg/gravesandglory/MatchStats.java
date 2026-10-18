package gg.gravesandglory;

import java.util.List;

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
        /** Seconds with a troop or the commander inside any objective's radius; one per second however many points. */
        public int objectiveSeconds;
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
