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

    /** As ranked, with every commander who left the battle (ids in left) after everyone still present, each part in score order (ending polish pass 2):
     *  a commander who left never takes a podium place or a rank above one who stayed. */
    public static List<Entry> ranked(List<Entry> entries, int winner, Spec.EndingRow w, Set<String> left) {
        List<Entry> all = ranked(entries, winner, w), out = new ArrayList<>();
        for (Entry e : all) if (!left.contains(e.id())) out.add(e);
        for (Entry e : all) if (left.contains(e.id())) out.add(e);
        return out;
    }

    /** One REWARDS bar (ending polish pass 2): label, before and after the match, the bar's floor (the threshold already reached), the next threshold
     *  and the tier that threshold is (1-based); kind is xp, renown or contract. */
    public record Progress(String label, String kind, int before, int after, int base, int next, int tier) {
        public boolean crossed() { return after >= next; }
    }
    /** The bar from before toward the first of the ascending thresholds above before, or null when before is at or past the last (nothing left to fill). */
    public static Progress progress(String label, String kind, int before, int after, int[] thresholds) {
        for (int i = 0; i < thresholds.length; i++)
            if (before < thresholds[i]) return new Progress(label, kind, before, Math.max(before, after), i == 0 ? 0 : thresholds[i - 1], thresholds[i], i + 1);
        return null;
    }

    public static int mvpIndex(List<Entry> entries, int winner, Spec.EndingRow w) {
        if (entries.isEmpty()) return -1;
        return entries.indexOf(ranked(entries, winner, w).get(0));
    }

    /** The MVP among commanders still in the battle: the first ranked entry whose id is not in left (ids of commanders who left the battle);
     *  -1 when every one left. Indexes the original list, as mvpIndex. */
    public static int mvpIndex(List<Entry> entries, int winner, Spec.EndingRow w, Set<String> left) {
        for (Entry e : ranked(entries, winner, w)) if (!left.contains(e.id())) return entries.indexOf(e);
        return -1;
    }

    /** An AudioCues cue by its enum name, or null when no such cue exists (yet): never throws, unlike Cue.valueOf. */
    public static AudioCues.Cue cue(String name) {
        for (AudioCues.Cue c : AudioCues.Cue.values()) if (c.name().equals(name)) return c;
        return null;
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

    // ---- ending polish pass 3: the score timeline ----

    /**
     * The score graph's samples (spec 5): each team's mode score at every interval seconds of battle time, at most MAX samples. A sample that
     * falls due while nothing samples (the round clock moved, a long tick) takes the value at hand, so the count always follows the battle's
     * length: floor(seconds / interval) while it runs, ceil(seconds / interval) at the end, whose last sample is the final score. When a sample
     * is due past MAX, every second sample is dropped and the interval doubles.
     */
    public static final class Timeline {
        public static final int MAX = 240, START = 5;
        public int interval = START;
        public final List<int[]> samples = new ArrayList<>();
        public void clear() { samples.clear(); interval = START; }
        /** Samples now at seconds of battle time; last (the finish) takes the partial interval too and puts now in the last sample. */
        public void sample(long seconds, int[] now, boolean last) {
            for (;;) {
                long due = last ? (seconds + interval - 1) / interval : seconds / interval;
                if (samples.size() >= due) break;
                if (samples.size() >= MAX) { halve(); continue; }
                samples.add(now.clone());
            }
            if (last && !samples.isEmpty()) samples.set(samples.size() - 1, now.clone());
        }
        void halve() {
            List<int[]> kept = new ArrayList<>();
            for (int i = 1; i < samples.size(); i += 2) kept.add(samples.get(i));
            samples.clear(); samples.addAll(kept); interval *= 2;
        }
        /** One team's row of samples. */
        public int[] row(int team) { int[] out = new int[samples.size()]; for (int i = 0; i < out.length; i++) out[i] = team < samples.get(i).length ? samples.get(i)[team] : 0; return out; }
    }

    /** The compact form of a timeline row for the report (it travels in the camp snapshot, which has a size limit): each value as its change from
     *  the one before (from 0) in one character of DELTAS when it is -30 to 31, else "~" and the value in base 36 closed by ".". */
    static final String DELTAS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static String encodeRow(int[] values) {
        StringBuilder out = new StringBuilder();
        int prev = 0;
        for (int v : values) {
            int d = v - prev;
            if (d >= -30 && d <= 31) out.append(DELTAS.charAt(d + 30));
            else out.append('~').append(Integer.toString(v, 36)).append('.');
            prev = v;
        }
        return out.toString();
    }
    /** encodeRow read back; null for text that is not one. */
    public static int[] decodeRow(String text) {
        if (text == null) return null;
        List<Integer> out = new ArrayList<>();
        int prev = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '~') {
                int end = text.indexOf('.', i);
                if (end < 0) return null;
                try { prev = Integer.parseInt(text.substring(i + 1, end), 36); } catch (NumberFormatException e) { return null; }
                i = end;
            } else {
                int d = DELTAS.indexOf(c);
                if (d < 0) return null;
                prev += d - 30;
            }
            out.add(prev);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    // ---- ending polish pass 3: personal bests ----

    /** The profile's bests keys in the order the report lists them, and their labels. */
    public static final List<String> BEST_KEYS = List.of("kills", "armyKills", "score", "keptPct", "captures", "commanderDowns");
    static final List<String> BEST_LABELS = List.of("Kills", "Army kills", "Score", "Kept alive", "Captures", "Commanders downed");
    public record Best(String key, String label, int value, int previous) {}
    /**
     * This match's values against the records (both by BEST_KEYS; a value absent from values is not played): a value above an earlier record is
     * a best, listed with that record; a key with no record yet takes the value quietly. records is updated in place (a higher value, or the
     * first). Returns the bests in BEST_KEYS order.
     */
    public static List<Best> bests(Map<String, Integer> records, Map<String, Integer> values) {
        List<Best> out = new ArrayList<>();
        for (int i = 0; i < BEST_KEYS.size(); i++) {
            String k = BEST_KEYS.get(i);
            Integer v = values.get(k);
            if (v == null) continue;
            Integer was = records.get(k);
            if (was != null && v > was) out.add(new Best(k, BEST_LABELS.get(i), v, was));
            if (was == null || v > was) records.put(k, v);
        }
        return out;
    }
    /** The words of one best on REWARDS: "New best: 14 kills (was 11)", "New best: a score of 812 (was 640)", "New best: 90% kept alive (was 75%)". */
    public static String bestText(String key, int value, int previous) {
        String v = String.valueOf(value), p = String.valueOf(previous);
        String what = switch (key) {
            case "kills" -> v + (value == 1 ? " kill" : " kills");
            case "armyKills" -> v + (value == 1 ? " army kill" : " army kills");
            case "score" -> "a score of " + v;
            case "keptPct" -> v + "% kept alive";
            case "captures" -> v + (value == 1 ? " capture" : " captures");
            case "commanderDowns" -> v + (value == 1 ? " commander downed" : " commanders downed");
            default -> v + " " + key;
        };
        return "New best: " + what + " (was " + p + (key.equals("keptPct") ? "%" : "") + ")";
    }
}
