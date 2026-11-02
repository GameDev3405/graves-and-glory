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
    public static int partialGold(int base, long playedSeconds, int roundSeconds) {
        if (base <= 0 || playedSeconds <= 0 || roundSeconds <= 0) return 0;
        return (int) ((long) base * Math.min(playedSeconds, roundSeconds) / roundSeconds);
    }

    /** The permission level of ops-only commands such as /warp (vanilla's game-master level, as /backup and /map use). */
    public static final int OPS = 2;
}
