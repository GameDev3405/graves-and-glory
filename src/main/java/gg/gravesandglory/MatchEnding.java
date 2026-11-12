package gg.gravesandglory;

import java.util.*;

/** The pure rules of the match ending: the step clock, the army score and ranking, the podium and the stat lines. No Minecraft types. */
public final class MatchEnding {
    private MatchEnding() {}

    public enum Step { RESULT, PODIUM, STATS, REWARDS, DONE }

    public record Timing(int resultTicks, int podiumTicks, int statsTicks, int rewardsTicks) {
        int of(Step s) {
            return switch (s) { case RESULT -> resultTicks; case PODIUM -> podiumTicks; case STATS -> statsTicks; case REWARDS -> rewardsTicks; case DONE -> 0; };
        }
    }

    /** The sheet's seconds as ticks; the rewards step grows by rewardRowTenths tenths of a second per reward row past six, up to rewardsMaxSeconds. */
    public static Timing timing(Spec.EndingRow row, int rewardRows) {
        int rewards = row.rewardsSeconds() * 20;
        if (rewardRows > 6) rewards = Math.min(row.rewardsMaxSeconds() * 20, rewards + row.rewardRowTenths() * (rewardRows - 6) * 2);
        return new Timing(row.resultSeconds() * 20, row.podiumSeconds() * 20, row.statsSeconds() * 20, rewards);
    }

    public static Step step(long elapsedTicks, Timing t) {
        long at = Math.max(0, elapsedTicks);
        for (Step s : Step.values()) {
            if (s == Step.DONE) break;
            int len = t.of(s);
            if (at < len) return s;
            at -= len;
        }
        return Step.DONE;
    }

    public static long ticksLeft(long elapsedTicks, Timing t) {
        long at = Math.max(0, elapsedTicks);
        for (Step s : Step.values()) {
            if (s == Step.DONE) break;
            int len = t.of(s);
            if (at < len) return len - at;
            at -= len;
        }
        return 0;
    }

    /** The later of the shared clock's step and the step this player skipped to (null counts as none). */
    public static Step playerStep(Step clock, Step skippedTo) {
        return skippedTo != null && skippedTo.ordinal() > clock.ordinal() ? skippedTo : clock;
    }

    public static Step next(Step s) {
        return s == Step.DONE ? Step.DONE : Step.values()[s.ordinal() + 1];
    }

    public record Entry(String id, int team, boolean bot, int seat, int kills, int commanderDowns, int objectiveSeconds, int kept, int lost) {}

    public static int score(Entry e, Spec.EndingRow w) {
        return w.killWeight() * e.kills() + w.commanderWeight() * e.commanderDowns() + w.objectiveWeight() * e.objectiveSeconds() + survival(e, w);
    }

    private static int survival(Entry e, Spec.EndingRow w) {
        int total = e.kept() + e.lost();
        return total <= 0 ? 0 : (int) Math.round((double) w.survivalMax() * e.kept() / total);
    }

    /** Score descending; ties: more kills, more objective seconds, the winning side, the earlier seat. A winner of -1 (a draw) skips the side rule. */
    public static List<Entry> ranked(List<Entry> entries, int winner, Spec.EndingRow w) {
        List<Entry> out = new ArrayList<>(entries);
        out.sort((a, b) -> {
            int c = Integer.compare(score(b, w), score(a, w));
            if (c == 0) c = Integer.compare(b.kills(), a.kills());
            if (c == 0) c = Integer.compare(b.objectiveSeconds(), a.objectiveSeconds());
            if (c == 0 && winner >= 0) c = Boolean.compare(b.team() == winner, a.team() == winner);
            if (c == 0) c = Integer.compare(a.seat(), b.seat());
            return c;
        });
        return out;
    }

    public static int mvpIndex(List<Entry> entries, int winner, Spec.EndingRow w) {
        if (entries.isEmpty()) return -1;
        return entries.indexOf(ranked(entries, winner, w).get(0));
    }

    public static List<Entry> podium(List<Entry> ranked) {
        return new ArrayList<>(ranked.subList(0, Math.min(3, ranked.size())));
    }

    /** Troop ids by kills descending (ties in banner order), then the rest of the banner in order, without repeats, up to max. */
    public static List<String> squadTroops(Map<String, Integer> troopKills, List<String> bannerOrder, int max) {
        List<String> killers = new ArrayList<>();
        for (String id : bannerOrder) if (troopKills.getOrDefault(id, 0) > 0 && !killers.contains(id)) killers.add(id);
        killers.sort(Comparator.comparingInt((String id) -> troopKills.get(id)).reversed()); // stable: banner order breaks ties
        List<String> out = new ArrayList<>();
        for (String id : killers) if (out.size() < max) out.add(id);
        for (String id : bannerOrder) if (out.size() < max && !out.contains(id)) out.add(id);
        return out;
    }

    public enum Headline { KILLS, OBJECTIVE, KEPT }

    /** The score term that contributes most; ties go to the earlier enum value. */
    public static Headline headline(Entry e, Spec.EndingRow w) {
        int[] v = {w.killWeight() * e.kills(), w.objectiveWeight() * e.objectiveSeconds(), survival(e, w)};
        int best = 0;
        for (int i = 1; i < v.length; i++) if (v[i] > v[best]) best = i;
        return Headline.values()[best];
    }

    public static String headlineText(Entry e, Headline h) {
        return switch (h) {
            case KILLS -> e.kills() + " kills";
            case OBJECTIVE -> (e.objectiveSeconds() / 60) + ":" + String.format(Locale.ROOT, "%02d", e.objectiveSeconds() % 60) + " on points";
            case KEPT -> e.kept() + " of " + (e.kept() + e.lost()) + " kept";
        };
    }

    public static String kd(int kills, int deaths) {
        return kills + " / " + deaths;
    }

    public static String ratio(int kills, int deaths) {
        return String.format(Locale.ROOT, "%.1f", deaths <= 0 ? (double) kills : (double) kills / deaths);
    }
}
