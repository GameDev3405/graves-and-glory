package gg.gravesandglory;

/**
 * Server rules Kort approved on 2026-11-01 (Q20), kept pure so MatchRulesCheck runs them without Minecraft.
 * A match pays only when it ends on the field; a stopped or crashed one is void. A commander who drops mid-battle keeps his army
 * on its last orders for the grace period (campaign sheet: disconnectGraceSeconds), then a bot takes command; rejoining hands it back.
 */
public final class MatchRules {
    private MatchRules() {}

    /** How a match ended. Only COMPLETED (a winner, a draw or the clock) settles rewards; the others are void. */
    public enum Ending { COMPLETED, STOPPED, CRASHED }

    /** True when the ending pays rewards and records the match. A void match pays nothing and costs nothing. */
    public static boolean settles(Ending ending) { return ending == Ending.COMPLETED; }

    /** The tick a dropped commander's bot takes command: leftAt plus the grace period. */
    public static long takeoverAt(long leftAt, int graceSeconds) { return leftAt + Math.max(0, graceSeconds) * 20L; }

    /** True while a vacated army still waits for its owner (no bot orders yet). takeoverAt 0 means no wait (a bot from the start). */
    public static boolean waiting(long now, long takeoverAt) { return takeoverAt > 0 && now < takeoverAt; }

    /** The permission level of ops-only commands such as /warp (vanilla's game-master level, as /backup and /map use). */
    public static final int OPS = 2;
}
