package gg.gravesandglory;

/**
 * Autosave policy Kort approved on 2026-11-02. The overworld is the arena, and vanilla's 6000-tick autosave of it stalls one
 * battle tick for 480 to 750 ms (docs/reports/perf-baseline-2026-11-02.md). While a battle runs the periodic autosave is skipped
 * and owed; the round's return to the lobby pays it with one save. Only the periodic autosave is gated: shutdown saves,
 * BuildMode's saveAll, /save-all, chunk unloads and the mod's own profile and marker files are untouched.
 * Kept pure so ArenaAutosaveCheck runs it without Minecraft; AutosaveMixin and Campaign.returnLobby are the wiring.
 */
public final class ArenaAutosave {
    private ArenaAutosave() {}

    private static boolean owed;

    /** Called at vanilla's periodic autosave: true to save now, false to skip it (a battle is running) and owe a save. */
    public static synchronized boolean allow(boolean battleRunning) {
        if (battleRunning) { owed = true; return false; }
        owed = false; // the autosave about to run covers anything owed
        return true;
    }

    /** Called when the round returns to the lobby: true once if an autosave was skipped since the last save, and clears the debt. */
    public static synchronized boolean settle() {
        boolean was = owed;
        owed = false;
        return was;
    }

    /** True while a skipped autosave has not yet been paid. */
    public static synchronized boolean owed() { return owed; }

    /** Test aid: forget any debt. */
    static synchronized void reset() { owed = false; }
}
