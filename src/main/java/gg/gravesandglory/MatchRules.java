package gg.gravesandglory;

/**
 * Server rules Kort approved on 2026-11-01 (Q20), kept pure so MatchRulesCheck runs them without Minecraft.
 * Only a match that ends on the field settles in full (win bonus, shards, Renown, XP, record). A stopped or crashed one pays a partial
 * reward instead (Kort 2026-11-02): the participation gold pro-rated by the share of the match played, and no win bonus.
 * A commander who drops mid-battle keeps his army on its last orders for the grace period (campaign sheet: disconnectGraceSeconds),
 * then a bot takes command; rejoining hands it back, but only for HOLD_SECONDS: after that the slot is released (Kort 2026-11-02).
 */
public final class MatchRules {
    private MatchRules() {}

    /** How a match ended. Only COMPLETED (a winner, a draw or the clock) settles in full; the others pay a partial reward (partialGold). */
    public enum Ending { COMPLETED, STOPPED, CRASHED }

    /** True when the ending pays in full and records the match. A stopped or crashed one pays only partialGold and costs nothing. */
    public static boolean settles(Ending ending) { return ending == Ending.COMPLETED; }

    /** The tick a dropped commander's bot takes command: leftAt plus the grace period. */
    public static long takeoverAt(long leftAt, int graceSeconds) { return leftAt + Math.max(0, graceSeconds) * 20L; }

    /** True while a vacated army still waits for its owner (no bot orders yet). takeoverAt 0 means no wait (a bot from the start). */
    public static boolean waiting(long now, long takeoverAt) { return takeoverAt > 0 && now < takeoverAt; }

    /** How long a dropped commander's slot is held for him, from the drop. After it the army stays with its bot and any joiner may take the seat. */
    public static final int HOLD_SECONDS = 120;

    /** The tick a dropped commander's slot is released: leftAt plus HOLD_SECONDS. */
    public static long releaseAt(long leftAt) { return leftAt + HOLD_SECONDS * 20L; }

    /** True once the hold has run out. */
    public static boolean holdExpired(long now, long releaseAt) { return now >= releaseAt; }

    /**
     * The gold a stopped or crashed match pays one player: the participation gold (base, never the win bonus) times the share of the
     * match he played, rounded down. A match with no length (roundSeconds 0) or no time played pays nothing.
     */
    public static int partialGold(int base, long playedSeconds, int roundSeconds) { return prorate(base, playedSeconds, roundSeconds); }

    /**
     * An amount scaled by the share of a match a player was seated (Kort 2026-11-02): amount times seated over total, rounded down. A player seated
     * the whole match (seated at or past total) gets the amount unchanged. No amount, no time seated or no match length gives nothing.
     */
    public static int prorate(int amount, long seated, long total) {
        if (amount <= 0 || seated <= 0 || total <= 0) return 0;
        return (int) ((long) amount * Math.min(seated, total) / total);
    }

    /**
     * The gold a finished match pays before pro-rating. A normal match pays completionGold plus victoryGold for a win; practice pays practiceGold
     * and no victory gold; a campaign battle (fought as practice against bots) pays practiceGold plus the same victoryGold a normal win pays, but
     * only when the player counts as the winner (won and seated at least half, see majority), so a win he barely played earns nothing extra.
     */
    public static int grossGold(boolean practice, boolean campaignBattle, boolean won, boolean credited, int completionGold, int practiceGold, int victoryGold) {
        if (!practice) return completionGold + (won ? victoryGold : 0);
        return practiceGold + (campaignBattle && credited ? victoryGold : 0);
    }

    /**
     * The Forge marks a finished match pays before pro-rating: base for a win, base times lossPct for a loss, times botOnlyPct when no human
     * played the other side, rounded down. No base pays nothing.
     */
    public static int forgeMarks(int base, boolean won, boolean botOnly, double lossPct, double botOnlyPct) {
        if (base <= 0) return 0;
        return (int) Math.floor(base * (won ? 1.0 : lossPct) * (botOnly ? botOnlyPct : 1.0) + 1e-9);
    }

    /** The share of the match a player was seated, as a whole percent from 0 to 100 (rounded down). */
    public static int sharePercent(long seated, long total) { return total <= 0 || seated <= 0 ? 0 : (int) (100L * Math.min(seated, total) / total); }

    /** True when a player was seated for at least half of the match. Only then does the match count as his win, a cleared battle or a bonus objective. */
    public static boolean majority(long seated, long total) { return total > 0 && seated * 2 >= total; }

    /**
     * The ticks a player was seated, from the later of the round start and his own seating, less the time he was away: awayTicks is the sum of his
     * finished absences and leftAt the tick of the drop he has not returned from (negative when he is present). Never negative.
     */
    public static long seatedTicks(long now, long roundStarted, long joinedAt, long awayTicks, long leftAt) {
        long span = now - Math.max(roundStarted, joinedAt) - Math.max(0, awayTicks) - (leftAt >= 0 ? Math.max(0, now - leftAt) : 0);
        return Math.max(0, span);
    }

    /** The permission level of ops-only commands such as /warp (vanilla's game-master level, as /backup and /map use). */
    public static final int OPS = 2;
}
