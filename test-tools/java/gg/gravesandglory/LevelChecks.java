package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Suites levels, gear and armoury of the in-match progression plan (Tasks 4, 5 and 8, lane A). Never included in releases.
 * In-match progression Task 3 lays the skeleton: each suite reports fail p2-<suite>-not-built until the task that builds it replaces its entry.
 * Phase2Checks.SUITES takes these entries through register (GRAVES_PHASE2_CHECKS=<suite>), and the checks print through Phase2Checks.report.
 */
final class LevelChecks {
    private LevelChecks() {}

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("levels", () -> new NotBuilt("levels", "Task 4 (lane A): kill credit and levels"));
        suites.put("gear", () -> new NotBuilt("gear", "Task 5 (lane A): gear, the gear command, the Armoury state, bot buys"));
        suites.put("armoury", () -> new NotBuilt("armoury", "Task 8: the Armoury tab"));
    }

    /** A suite whose checks are not written yet: one fail line naming the task that writes them, then the suite ends. */
    static final class NotBuilt implements Phase2Checks.Suite {
        final String suite, task;
        NotBuilt(String suite, String task) { this.suite = suite; this.task = task; }
        public void step(MinecraftClient c, int tick) {
            if (tick != 1) return;
            Phase2Checks.report("p2-" + suite + "-not-built", false, "suite " + suite + " is a skeleton until " + task);
            Phase2Checks.finish(c, suite);
        }
    }
}
