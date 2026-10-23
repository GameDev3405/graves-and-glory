package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Suites statuses and levels-gilded to levels-tidecallers of the in-match progression plan (Tasks 6 and 7, lane B). Never included in releases.
 * In-match progression Task 3 lays the skeleton: each suite reports fail p2-<suite>-not-built until the task that builds it replaces its entry.
 * Phase2Checks.SUITES takes these entries through register (GRAVES_PHASE2_CHECKS=<suite>), and the checks print through Phase2Checks.report.
 */
final class AbilityLevelChecks {
    private AbilityLevelChecks() {}

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("statuses", () -> new NotBuilt("statuses", "Task 6 (lane B): statuses and the new mechanics"));
        suites.put("levels-gilded", () -> new NotBuilt("levels-gilded", "Task 7 (lane B): the Gilded level rows"));
        suites.put("levels-graveborn", () -> new NotBuilt("levels-graveborn", "Task 7 (lane B): the Graveborn level rows"));
        suites.put("levels-wildmoot", () -> new NotBuilt("levels-wildmoot", "Task 7 (lane B): the Wildmoot level rows"));
        suites.put("levels-ironclad", () -> new NotBuilt("levels-ironclad", "Task 7 (lane B): the Ironclad level rows"));
        suites.put("levels-tidecallers", () -> new NotBuilt("levels-tidecallers", "Task 7 (lane B): the Tidecallers level rows"));
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
