package gg.gravesandglory;

/** Pure match statistics shared by the server and the bot simulation. No Minecraft types, so the standalone check compiles it. */
public final class MatchStats {
    private MatchStats() {}
    /** One commander's tally for the current round; Battle.startRound gives every seated squad a fresh one. */
    public static final class SquadStats {
        /** Gold earned this round from income and kills; starting gold and refunds do not count. */
        public int goldEarned;
    }
    /** Share of matches won, 0 when no match was played. */
    public static double winRate(int wins, int matches) {
        return matches == 0 ? 0 : (double) wins / matches;
    }
}
