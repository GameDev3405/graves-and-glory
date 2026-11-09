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

    /** The most commanders a team seats in a March battle: the six-seat lobby is 3 v 3. */
    static final int MAX_SEATS = 3;

    /** The commanders per team a March battle seats: its row's size held to 1..MAX_SEATS; 1 for practice (no row). The one human takes a seat on team 0 and bot fill takes the rest. */
    static int seatSize(Spec.CampaignBattlesRow row) {
        return row == null ? 1 : Math.max(1, Math.min(MAX_SEATS, row.size()));
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

    /** The botBanners ids a battle row names as its commander's bot allies (campaignBattles.allies, Kort 2026-11-02, Q76); empty for none or no row. */
    static List<String> allyIds(Spec.CampaignBattlesRow r) { return r == null || r.allies().equals("none") ? List.of() : Arrays.asList(r.allies().split(",")); }

    /** The campaignChapters row of a chapter number, or null. */
    /** The name of the chapter a battle belongs to ("The Oath"), or "The campaign" for a battle without a chapter row. */
    static String chapterName(Spec.CampaignBattlesRow r, Spec.CampaignChaptersRow[] chapters) {
        var c = r == null ? null : chapter(r.chapter(), chapters);
        return c == null ? "The campaign" : c.name();
    }

    static Spec.CampaignChaptersRow chapter(int chapter, Spec.CampaignChaptersRow[] chapters) {
        for (var c : chapters) if (c.chapter() == chapter) return c;
        return null;
    }

    /** The battle's after line (campaignBattles.after, brief N2) for the result screen: shown after a win only; "" otherwise. */
    static String afterLine(Spec.CampaignBattlesRow r, boolean won) { return r == null || !won ? "" : r.after(); }

    /** The chapter end lines (campaignChapters.chapterEnd) of the chapters whose Pact this battle granted, in order; "" when it granted none. A
     *  chapter's Pact is granted once (writChapters), so its end line shows once, the first time its last battle is won. */
    static String chapterEndLine(Collection<Integer> granted, Spec.CampaignChaptersRow[] chapters) {
        List<String> out = new ArrayList<>();
        for (int n : granted) { var c = chapter(n, chapters); if (c != null && !c.chapterEnd().isBlank()) out.add(c.chapterEnd()); }
        return String.join(" ", out);
    }

    /** The botBanners ids a battle names as its enemies by lane (campaignBattles.enemies, chapter 1 brief N1); empty for none or no row. */
    static List<String> enemyIds(Spec.CampaignBattlesRow r) { return r == null || r.enemies().equals("none") ? List.of() : Arrays.asList(r.enemies().split(",")); }

    /** The botBanners id team 1's bot in this lane fields: the battle's enemies row at lane % rows; null when the battle names none, so tier and lane picking choose. */
    static String enemyId(Spec.CampaignBattlesRow r, int lane) { var ids = enemyIds(r); return ids.isEmpty() ? null : ids.get(Math.floorMod(lane, ids.size())); }

    /** The botBanners row the commander fields in this battle when its chapter sets banner house (brief G1), or null to field their own banner. */
    static String houseBannerRow(Spec.CampaignBattlesRow r, Spec.CampaignChaptersRow[] chapters) {
        var c = r == null ? null : chapter(r.chapter(), chapters);
        return c != null && c.banner().equals("house") && !c.bannerRow().equals("none") ? c.bannerRow() : null;
    }

    /** Whether a botBanners row is kept out of every tier's pool: some battle's ally or some chapter's House banner. */
    static boolean reserved(String id, Spec.CampaignBattlesRow[] rows, Spec.CampaignChaptersRow[] chapters) {
        if (reservedAlly(id, rows)) return true;
        for (var c : chapters) if (c.bannerRow().equals(id)) return true;
        return false;
    }

    /** Whether a botBanners row is reserved as some battle's allies, so it never joins a tier's pool (Campaign.fieldableBotBanners). */
    static boolean reservedAlly(String id, Spec.CampaignBattlesRow[] rows) { for (var r : rows) if (allyIds(r).contains(id)) return true; return false; }

    /**
     * The glimpur ebb (chapter 1 brief N3): in a battle whose row sets ebb, every caster troop (troops.magic) on both sides now and then
     * flickers, its own glow and particles fading for a moment with a faint sound. Presentation only; nothing here touches combat. Pure and
     * keyed on the entity's network id and the world time, so the server (particles, sound) and every client (glow) agree without a packet.
     * Once per PERIOD ticks each caster dims for LENGTH ticks at a point of the period its id and the period pick; windows never touch.
     */
    static final class Ebb {
        private Ebb() {}
        /** Ticks between a caster's flickers (20 seconds) and how long one lasts (1.5 seconds). */
        static final int PERIOD = 400, LENGTH = 30;
        /** The glow left at the bottom of a flicker: faded, never dark. */
        static final float LOW = 0.2f;

        /** Where in its period this caster's flicker starts, from 0 to PERIOD - 2 * LENGTH (so a window ends LENGTH before the next can start). */
        static int start(int entity, long period) {
            long h = entity * 0x9E3779B97F4A7C15L ^ period * 0xC2B2AE3D27D4EB4FL;
            h ^= h >>> 31; h *= 0x94D049BB133111EBL; h ^= h >>> 29;
            return (int) Math.floorMod(h, (long) (PERIOD - 2 * LENGTH));
        }

        /** How far into its flicker the caster is at this world tick, or -1 outside one (and always without the battle's ebb). */
        static int into(boolean ebb, int entity, long time) {
            if (!ebb) return -1;
            long period = Math.floorDiv(time, PERIOD);
            long in = time - period * PERIOD - start(entity, period);
            return in >= 0 && in < LENGTH ? (int) in : -1;
        }

        /** The caster's glow brightness, 1 outside a flicker: a sputter between LOW and 0.6 for the first 12 ticks, LOW, then a climb back over the last 6. */
        static float glow(boolean ebb, int entity, long time) {
            int in = into(ebb, entity, time);
            if (in < 0) return 1f;
            if (in < 12) return (in / 2) % 2 == 0 ? LOW : 0.6f;
            if (in < LENGTH - 6) return LOW;
            return LOW + (0.9f - LOW) * (in - (LENGTH - 6)) / 6f;
        }

        /** Whether the caster's flicker begins on this tick: the server plays the faint sound once then. */
        static boolean starts(boolean ebb, int entity, long time) { return into(ebb, entity, time) == 0; }
    }

    /**
     * One round's bonus objective (the closed kinds of Phase 4 spec 11). The round feeds it events; kept reads the result at the finish. The
     * kinds: hold_all, the player's side held every point at one moment; lose_at_most, at most value troops lost; stage_by, the target stage
     * reached by value seconds; never_rolled, the Convoy wagon never rolled back; win_within, won by value seconds;
     * kill_unit, a troop of the target id fell to the player's own squad. Any other kind (tbd, a placeholder) is never kept. Every kind needs
     * the win, which settle also requires.
     */
    static final class Tracker {
        final String kind, target;
        final int value;
        boolean heldAll, rolledBack, killed;
        int lost, stagedAt = -1;

        Tracker(Spec.BonusObjectivesRow row) { this(row.kind(), row.value(), row.target()); }
        Tracker(String kind, int value, String target) { this.kind = kind; this.value = value; this.target = target; }

        /** The points the player's side holds now, of all points. */
        void held(int own, int total) { if (total > 0 && own >= total) heldAll = true; }
        /** Troops of the player's banner lost since the last call. */
        void lost(int troops) { lost += Math.max(0, troops); }
        /** A stage reached, seconds into the round; the first time a stage is reached counts. */
        void staged(String stage, int seconds) { if (stage.equals(target) && stagedAt < 0) stagedAt = seconds; }
        /** The Convoy wagon rolled back this tick (ConvoyMode.feed). */
        void rolled() { rolledBack = true; }
        /** A troop fell; byOwnSquad when the killing blow came from the player's squad. */
        void killed(String troop, boolean byOwnSquad) { if (byOwnSquad && troop.equals(target)) killed = true; }

        boolean kept(boolean won, int seconds) {
            if (!won) return false;
            return switch (kind) {
                case "hold_all" -> heldAll;
                case "lose_at_most" -> lost <= value;
                case "stage_by" -> stagedAt >= 0 && stagedAt <= value;
                case "never_rolled" -> !rolledBack;
                case "win_within" -> seconds <= value;
                case "kill_unit" -> killed;
                default -> false;
            };
        }
    }
}
