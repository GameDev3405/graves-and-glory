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
     *  and the tier that threshold is (1-based); kind is xp, renown or contract. base, next and tier are the first climb. When the match crosses
     *  two or more thresholds, crossings counts them and finalBase, finalNext and finalTier are the climb the bar ends on (the last threshold reached,
     *  the one after it and that one's tier; finalNext equals finalBase when no threshold is left); otherwise they repeat the first climb. */
    public record Progress(String label, String kind, int before, int after, int base, int next, int tier, int crossings, int finalBase, int finalNext, int finalTier) {
        public boolean crossed() { return after >= next; }
        /** Whether the match crossed more than one threshold (the bar climbs, empties and climbs again). */
        public boolean multi() { return crossings >= 2; }
        /** The level or tier the bar ends on: the tier of the last threshold crossed (the first climb's own when it crossed one or none). */
        public int reached() { return multi() ? (finalNext > finalBase ? finalTier - 1 : finalTier) : crossed() ? tier : Math.max(0, tier - 1); }
    }
    /** The bar from before toward the first of the ascending thresholds above before, or null when before is at or past the last (nothing left to fill). */
    public static Progress progress(String label, String kind, int before, int after, int[] thresholds) {
        for (int i = 0; i < thresholds.length; i++) {
            if (before >= thresholds[i]) continue;
            int end = Math.max(before, after), k = 0;
            while (i + k < thresholds.length && end >= thresholds[i + k]) k++;
            int base = i == 0 ? 0 : thresholds[i - 1];
            int fb = base, fn = thresholds[i], ft = i + 1;
            if (k >= 2) {
                int last = i + k - 1;
                fb = thresholds[last];
                if (last + 1 < thresholds.length) { fn = thresholds[last + 1]; ft = last + 2; } else { fn = fb; ft = last + 1; }
            }
            return new Progress(label, kind, before, end, base, thresholds[i], i + 1, k, fb, fn, ft);
        }
        return null;
    }

    /** A bar that crosses several thresholds animates in climbs: each climb fills for CLIMB_FILL of its time and holds full (the flash) for the rest, then the bar
     *  empties for the next; the last climb fills to where the match left it. MULTI_SPAN is the most of the REWARDS step all the climbs may take together. */
    public static final float CLIMB_FILL = .7f, MULTI_SPAN = .26f;
    /** The share of the step one climb takes: barFill for a bar that crosses at most one threshold, else barFill or less so the whole bar stays within MULTI_SPAN. */
    public static float climbSpan(int crossings, float barFill) { return crossings < 2 ? barFill : Math.min(barFill, MULTI_SPAN / (crossings + CLIMB_FILL)); }
    /** The progress at which a bar starting at start has finished its last climb and its last flash (flash is the flash's share of the step). */
    public static float barEnd(float start, int crossings, float barFill, float flash) {
        return start + (crossings < 2 ? barFill : climbSpan(crossings, barFill) * (crossings + CLIMB_FILL)) + (crossings < 1 ? 0 : flash);
    }
    /** One frame of a multi-crossing bar: the climb it is on (0 first, crossings last), the share of that climb's bar filled (0..1), the thresholds crossed so far,
     *  and the time since the latest crossing in climbs (a huge number before the first). */
    public record Climb(int index, float share, int crossed, float since) {}
    /**
     * The frame t climbs after the bar started (t in climbs, one climb being one unit): the first climb fills from was (the share the bar began at), each later
     * climb from empty to full, the last from empty to finalShare. Crossing m (1-based) is reached at the end of climb m's fill, t = m - 1 + CLIMB_FILL, and is
     * counted from then on.
     */
    public static Climb climb(int crossings, float t, float was, float finalShare) {
        t = Math.max(0, t);
        int j = (int) Math.min(Math.floor(t), crossings);
        float f = Math.min(1, (t - j) / CLIMB_FILL), ease = 1 - (1 - f) * (1 - f);
        float lo = j == 0 ? was : 0, hi = j < crossings ? 1 : finalShare;
        int crossed = 0;
        for (int m = 1; m <= crossings; m++) if (t >= m - 1 + CLIMB_FILL - 1e-4f) crossed = m;
        return new Climb(j, lo + (hi - lo) * ease, crossed, crossed == 0 ? Float.MAX_VALUE : t - (crossed - 1 + CLIMB_FILL));
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
        public static final int MAX = 120, START = 5; // 120 samples: the report rides the size-limited camp snapshot (see fit)
        public int interval = START;
        public final List<int[]> samples = new ArrayList<>();
        public void clear() { samples.clear(); interval = START; }
        /** Whether a sample is due at seconds of battle time (before the finish), so the caller reads the score only then. */
        public boolean due(long seconds) { return samples.size() < seconds / interval; }
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
            int was = records.getOrDefault(k, 0); // a record of 0 is no earlier record: nothing to beat, nothing to say
            if (was > 0 && v > was) out.add(new Best(k, BEST_LABELS.get(i), v, was));
            if (v > was || !records.containsKey(k)) records.put(k, v);
        }
        return out;
    }
    /** Fewest troops a match must field (kept + lost) before "kept alive" is a personal best: 100% of two troops is no record. */
    public static final int MIN_FIELDED_FOR_KEPT = 10;
    /** Kept alive as a percent of the army fielded, or -1 when fewer than MIN_FIELDED_FOR_KEPT were. */
    public static int keptPct(int kept, int lost) { return kept + lost < MIN_FIELDED_FOR_KEPT ? -1 : Math.round(100f * kept / (kept + lost)); }
    /** One camp snapshot as sent: {text, form, full}. */
    public record Fitted(String text, String form, int full) {}
    /**
     * The snapshot within limit characters (the client drops its connection on a longer string): pretty when it fits, else compact, else compact with
     * the report's timeline stripped, then its bests, then its progress (the extras, in that order; the rewards, bars' totals and the rest stay), then
     * the whole report, last "{}" (the client keeps its last camp state). Never changes j.
     */
    public static Fitted fit(com.google.gson.JsonObject j, int limit, com.google.gson.Gson pretty, com.google.gson.Gson wire) {
        String out = pretty.toJson(j);
        int full = out.length();
        if (full <= limit) return new Fitted(out, "pretty", full);
        out = wire.toJson(j);
        if (out.length() <= limit) return new Fitted(out, "compact", full);
        var copy = j.deepCopy();
        var res = copy.has("result") && copy.get("result").isJsonObject() ? copy.getAsJsonObject("result") : null;
        String form = "compact";
        if (res != null) {
            for (String k : new String[]{"timeline", "bests", "progress"}) {
                if (!res.has(k)) continue;
                res.remove(k);
                form = form.equals("compact") ? "compact without the " + k : form + " and " + k;
                out = wire.toJson(copy);
                if (out.length() <= limit) return new Fitted(out, form, full);
            }
            copy.add("result", new com.google.gson.JsonObject());
            out = wire.toJson(copy);
            if (out.length() <= limit) return new Fitted(out, "compact without the battle report", full);
        }
        return new Fitted("{}", "empty", full);
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
        return "New best: " + what + (previous > 0 ? " (was " + p + (key.equals("keptPct") ? "%" : "") + ")" : "");
    }
}
