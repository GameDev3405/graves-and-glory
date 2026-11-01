package gg.gravesandglory;

import java.util.*;

/**
 * The Long March bookkeeping (plan 2C.3): battle order, the cleared count, the order lock and the settling of one finished
 * campaign battle, plus the bonus tracker of a round. Pure and all static, so the standalone MarchMathCheck compiles it; rows
 * are Spec.CAMPAIGNBATTLES in the game. Orders run 1 to n across the sheet (preflight), so the lock reads order alone.
 */
public final class MarchMath {
    private MarchMath() {}

    /** The highest order whose every row of that order or lower is won: the unbroken run from order 1. 0 for none. */
    static int cleared(Set<String> won, Spec.CampaignBattlesRow[] rows) {
        int run = 0;
        for (var row : byOrder(rows)) {
            if (!won.contains(row.id())) break;
            run = row.order();
        }
        return run;
    }

    /** The lowest-order unwon row ordered before this one, which must be won first; null when the row is open. */
    static Spec.CampaignBattlesRow lockedBy(Spec.CampaignBattlesRow row, Set<String> won, Spec.CampaignBattlesRow[] rows) {
        for (var r : byOrder(rows)) {
            if (r.order() >= row.order()) return null;
            if (!won.contains(r.id())) return r;
        }
        return null;
    }

    /** Whether every row of the chapter is won (false for a chapter with no rows). */
    static boolean chapterDone(int chapter, Set<String> won, Spec.CampaignBattlesRow[] rows) {
        boolean any = false;
        for (var r : rows) {
            if (r.chapter() != chapter) continue;
            any = true;
            if (!won.contains(r.id())) return false;
        }
        return any;
    }

    /** The lowest-order unwon row; null when every row is won. */
    static Spec.CampaignBattlesRow nextUnwon(Set<String> won, Spec.CampaignBattlesRow[] rows) {
        for (var r : byOrder(rows)) if (!won.contains(r.id())) return r;
        return null;
    }

    /** The row with this id, or null. */
    static Spec.CampaignBattlesRow row(String id, Spec.CampaignBattlesRow[] rows) {
        for (var r : rows) if (r.id().equals(id)) return r;
        return null;
    }

    /** The bonusObjectives row a battle names, or null. */
    static Spec.BonusObjectivesRow bonus(Spec.CampaignBattlesRow row, Spec.BonusObjectivesRow[] bonuses) {
        if (row == null) return null;
        for (var b : bonuses) if (b.id().equals(row.bonus())) return b;
        return null;
    }

    static List<Spec.CampaignBattlesRow> byOrder(Spec.CampaignBattlesRow[] rows) {
        List<Spec.CampaignBattlesRow> sorted = new ArrayList<>(Arrays.asList(rows));
        sorted.sort(Comparator.comparingInt(Spec.CampaignBattlesRow::order));
        return sorted;
    }

    /** What settle recorded: a first win, the cleared count after it, and the bonus it paid (0 shards and gold, house null, when it paid none). */
    record Settled(boolean firstWin, int cleared, boolean bonusPaid, int bonusShards, int bonusGold, String bonusHouse) {}

    /**
     * The save side of one finished campaign battle (Phase 4 spec 12, steps 1 and 2). A win adds the row's id to battlesWon. A bonus pays only
     * on a win with the bonus kept, only when its id is not in bonusClaimed, and only when the row's house is a House id in houses (the
     * shards need one House); a paid bonus adds its id to bonusClaimed. A loss or draw changes nothing. The Pact (step 3) stays with
     * Campaign.pacts, which reads the cleared count. The caller copies the profile first and puts it back when the save fails.
     */
    static Settled settle(Spec.CampaignBattlesRow row, Spec.BonusObjectivesRow bonus, boolean won, boolean bonusKept,
                          Set<String> battlesWon, Set<String> bonusClaimed, Collection<String> houses, Spec.CampaignBattlesRow[] rows) {
        if (!won) return new Settled(false, cleared(battlesWon, rows), false, 0, 0, null);
        boolean first = battlesWon.add(row.id());
        boolean pay = bonus != null && bonusKept && houses.contains(row.house()) && !bonusClaimed.contains(bonus.id());
        if (pay) bonusClaimed.add(bonus.id());
        return new Settled(first, cleared(battlesWon, rows), pay, pay ? bonus.shards() : 0, pay ? bonus.gold() : 0, pay ? row.house() : null);
    }

    /**
     * One round's bonus objective (the closed kinds of Phase 4 spec 11). The round feeds it events; kept reads the result at the finish. The
     * kinds: hold_all, the player's side held every point at one moment; lose_at_most, at most value troops lost; stage_by, the target stage
     * reached by value seconds; carry_self, the commander carried the enemy banner home himself; win_within, won by value seconds;
     * kill_unit, a troop of the target id fell to the player's own squad. Any other kind (tbd, a placeholder) is never kept. Every kind needs
     * the win, which settle also requires.
     */
    static final class Tracker {
        final String kind, target;
        final int value;
        boolean heldAll, carriedSelf, killed;
        int lost, stagedAt = -1;

        Tracker(Spec.BonusObjectivesRow row) { this(row.kind(), row.value(), row.target()); }
        Tracker(String kind, int value, String target) { this.kind = kind; this.value = value; this.target = target; }

        /** The points the player's side holds now, of all points. */
        void held(int own, int total) { if (total > 0 && own >= total) heldAll = true; }
        /** Troops of the player's banner lost since the last call. */
        void lost(int troops) { lost += Math.max(0, troops); }
        /** A stage reached, seconds into the round; the first time a stage is reached counts. */
        void staged(String stage, int seconds) { if (stage.equals(target) && stagedAt < 0) stagedAt = seconds; }
        /** The enemy banner brought home, by the commander himself or by another carrier. */
        void carried(boolean self) { if (self) carriedSelf = true; }
        /** A troop fell; byOwnSquad when the killing blow came from the player's squad. */
        void killed(String troop, boolean byOwnSquad) { if (byOwnSquad && troop.equals(target)) killed = true; }

        boolean kept(boolean won, int seconds) {
            if (!won) return false;
            return switch (kind) {
                case "hold_all" -> heldAll;
                case "lose_at_most" -> lost <= value;
                case "stage_by" -> stagedAt >= 0 && stagedAt <= value;
                case "carry_self" -> carriedSelf;
                case "win_within" -> seconds <= value;
                case "kill_unit" -> killed;
                default -> false;
            };
        }
    }
}
