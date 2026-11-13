package gg.gravesandglory;

import java.util.*;

/** The pure rules of the match ending: timings, the step clock, the score, the ranking and the podium, the squad picks and the stat lines. */
public final class MatchEndingCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static MatchEnding.Entry e(String id, int team, int seat, int kills, int downs, int obj, int kept, int lost) {
        return new MatchEnding.Entry(id, team, false, seat, kills, downs, obj, kept, lost);
    }
    static List<String> ids(List<MatchEnding.Entry> l) {
        List<String> out = new ArrayList<>();
        for (MatchEnding.Entry x : l) out.add(x.id());
        return out;
    }

    public static void main(String[] args) {
        Spec.EndingRow w = Spec.ENDING[0];
        // Score.
        check(MatchEnding.score(e("a", 0, 0, 38, 1, 185, 17, 9), w) == 660, "score 380+30+185+65");
        check(MatchEnding.score(e("a", 0, 0, 0, 0, 0, 0, 0), w) == 0, "no survival bonus with nobody fielded");
        check(MatchEnding.score(e("a", 0, 0, 0, 0, 0, 5, 0), w) == 100, "all kept is +100");
        // Ranking.
        check(ids(MatchEnding.ranked(List.of(e("a", 0, 0, 1, 0, 0, 0, 0), e("b", 1, 1, 2, 0, 0, 0, 0)), 0, w)).equals(List.of("b", "a")), "higher score first");
        // equal score 20: a = 2 kills, b = 1 kill + 10 objective
        check(ids(MatchEnding.ranked(List.of(e("b", 1, 1, 1, 0, 10, 0, 0), e("a", 0, 0, 2, 0, 0, 0, 0)), 1, w)).equals(List.of("a", "b")), "equal score: more kills first");
        // equal score 20, equal kills 1? use kills 1 + obj 10 versus kills 1 + 2 downs? Use downs: 10+0+10 vs 10 + 10 obj
        check(ids(MatchEnding.ranked(List.of(e("a", 0, 0, 0, 0, 30, 0, 0), e("b", 1, 1, 0, 1, 0, 0, 0)), 0, w)).equals(List.of("a", "b")), "equal score and kills: more objective first");
        check(ids(MatchEnding.ranked(List.of(e("a", 0, 0, 3, 0, 0, 0, 0), e("b", 1, 1, 3, 0, 0, 0, 0)), 1, w)).equals(List.of("b", "a")), "full tie: the winning side first");
        check(ids(MatchEnding.ranked(List.of(e("b", 1, 1, 3, 0, 0, 0, 0), e("a", 0, 0, 3, 0, 0, 0, 0)), -1, w)).equals(List.of("a", "b")), "draw: the earlier seat");
        check(ids(MatchEnding.ranked(List.of(e("b", 1, 2, 3, 0, 0, 0, 0), e("a", 1, 1, 3, 0, 0, 0, 0)), 1, w)).equals(List.of("a", "b")), "same side: the earlier seat");
        List<MatchEnding.Entry> bots = List.of(new MatchEnding.Entry("bot", 0, true, 0, 9, 0, 0, 0, 0), e("h", 1, 1, 1, 0, 0, 0, 0));
        check(ids(MatchEnding.ranked(bots, 1, w)).get(0).equals("bot"), "bots rank like humans");
        check(MatchEnding.mvpIndex(List.of(), 0, w) == -1, "no entries, no mvp");
        check(MatchEnding.mvpIndex(List.of(e("a", 0, 0, 0, 0, 0, 0, 0), e("b", 1, 1, 0, 0, 0, 0, 0)), 0, w) == 0, "a 0-score match still crowns the first ranked");
        check(MatchEnding.mvpIndex(List.of(e("a", 0, 0, 1, 0, 0, 0, 0), e("b", 1, 1, 5, 0, 0, 0, 0)), 0, w) == 1, "mvp indexes the original list");
        // Left the battle: no MVP for a commander who left; the podium still ranks him.
        List<MatchEnding.Entry> withLeft = List.of(e("gone", 0, 0, 50, 0, 0, 0, 0), e("b", 1, 1, 5, 0, 0, 0, 0), e("c", 0, 2, 3, 0, 0, 0, 0));
        check(MatchEnding.mvpIndex(withLeft, 0, w, Set.of("gone")) == 1, "the top scorer left: the next ranked is MVP");
        check(MatchEnding.mvpIndex(withLeft, 0, w, Set.of()) == 0, "nobody left: the top scorer");
        check(MatchEnding.mvpIndex(withLeft, 0, w, Set.of("gone", "b", "c")) == -1, "everyone left: no MVP");
        check(MatchEnding.mvpIndex(withLeft, 0, w, Set.of("c")) == 0, "a lower commander left: the MVP unchanged");
        check(ids(MatchEnding.podium(MatchEnding.ranked(withLeft, 0, w))).get(0).equals("gone"), "the podium still ranks a commander who left");
        // Pass 2: a commander who left ranks after everyone present, on the podium and in rank.
        var leftLast = MatchEnding.ranked(withLeft, 0, w, Set.of("gone"));
        check(ids(leftLast).equals(List.of("b", "c", "gone")), "left ranks after the present: " + ids(leftLast));
        check(ids(MatchEnding.podium(leftLast)).get(0).equals(ids(leftLast).get(0)) && withLeft.get(MatchEnding.mvpIndex(withLeft, 0, w, Set.of("gone"))).id().equals(ids(leftLast).get(0)), "the podium's first is the MVP");
        List<MatchEnding.Entry> eight = new ArrayList<>();
        for (int i = 0; i < 8; i++) eight.add(e("p" + i, i % 2, i, 10 - i, 0, 0, 0, 0));
        var gone8 = MatchEnding.ranked(eight, 0, w, Set.of("p0", "p2"));
        check(ids(gone8).subList(6, 8).equals(List.of("p0", "p2")), "two who left: last, in score order " + ids(gone8));
        check(!ids(MatchEnding.podium(gone8)).contains("p0") && !ids(MatchEnding.podium(gone8)).contains("p2"), "nobody who left on a podium of eight");
        check(ids(MatchEnding.ranked(withLeft, 0, w, Set.of("gone", "b", "c"))).equals(ids(MatchEnding.ranked(withLeft, 0, w))), "all left: plain score order");
        check(ids(MatchEnding.ranked(withLeft, 0, w, Set.of())).equals(ids(MatchEnding.ranked(withLeft, 0, w))), "nobody left: unchanged");
        var two = MatchEnding.ranked(List.of(e("gone", 0, 0, 50, 0, 0, 0, 0), e("b", 1, 1, 5, 0, 0, 0, 0)), 0, w, Set.of("gone"));
        check(ids(two).equals(List.of("b", "gone")), "two commanders, the top one left: he takes 2nd");
        // Pass 2: the REWARDS bars.
        int[] xp = new int[30]; for (int i = 0; i < 30; i++) xp[i] = 250 * (i + 1);
        var bar = MatchEnding.progress("XP", "xp", 730, 880, xp);
        check(bar != null && bar.base() == 500 && bar.next() == 750 && bar.tier() == 3 && bar.crossed(), "xp crossing tier 3: " + bar);
        bar = MatchEnding.progress("XP", "xp", 760, 860, xp);
        check(bar != null && bar.base() == 750 && bar.next() == 1000 && bar.tier() == 4 && !bar.crossed(), "xp short of tier 4: " + bar);
        check(MatchEnding.progress("XP", "xp", 0, 100, xp).base() == 0, "from nothing: floor 0");
        check(MatchEnding.progress("XP", "xp", 7500, 7650, xp) == null, "past the last threshold: no bar");
        var renown = MatchEnding.progress("Ironclad Renown", "renown", 7, 9, new int[]{3, 8, 15, 22, 30});
        check(renown.tier() == 2 && renown.base() == 3 && renown.next() == 8 && renown.crossed(), "renown crossing tier 2: " + renown);
        check(MatchEnding.progress("c", "contract", 2, 3, new int[]{3}).crossed() && !MatchEnding.progress("c", "contract", 1, 2, new int[]{3}).crossed(), "contract 3 battles");
        check(MatchEnding.progress("c", "contract", 5, 3, new int[]{6}).after() == 5, "after never below before");
        check(MatchEnding.cue("VICTORY") == AudioCues.Cue.VICTORY, "a cue by name");
        check(MatchEnding.cue("NO_SUCH_CUE_YET") == null, "a missing cue is null, not a throw");
        // Podium.
        for (int[] c : new int[][]{{0, 0}, {1, 1}, {2, 2}, {3, 3}, {8, 3}}) {
            List<MatchEnding.Entry> l = new ArrayList<>();
            for (int i = 0; i < c[0]; i++) l.add(e("p" + i, i % 2, i, i, 0, 0, 0, 0));
            check(MatchEnding.podium(MatchEnding.ranked(l, 0, w)).size() == c[1], "podium of " + c[0]);
        }
        // Squad troops.
        check(MatchEnding.squadTroops(Map.of("archer", 5, "knight", 5, "pike", 1), List.of("knight", "archer", "pike", "squire"), 3).equals(List.of("knight", "archer", "pike")), "by kills, ties in banner order");
        check(MatchEnding.squadTroops(Map.of(), List.of("knight", "archer"), 3).equals(List.of("knight", "archer")), "filled from the banner");
        check(MatchEnding.squadTroops(Map.of(), List.of(), 3).isEmpty(), "empty banner");
        check(MatchEnding.squadTroops(Map.of("pike", 2), List.of("knight", "archer", "pike", "squire"), 3).equals(List.of("pike", "knight", "archer")), "fill without repeats");
        // Timing and the step clock.
        check(MatchEnding.timing(w, 6).rewardsTicks() == 120, "six rows: 6 s");
        check(MatchEnding.timing(w, 9).rewardsTicks() == 144, "nine rows: +1.2 s");
        check(MatchEnding.timing(w, 40).rewardsTicks() == 200, "capped at 10 s");
        MatchEnding.Timing t = MatchEnding.timing(w, 6);
        check(MatchEnding.step(0, t) == MatchEnding.Step.RESULT && MatchEnding.step(119, t) == MatchEnding.Step.RESULT, "result");
        check(MatchEnding.step(120, t) == MatchEnding.Step.PODIUM && MatchEnding.step(279, t) == MatchEnding.Step.PODIUM, "podium");
        check(MatchEnding.step(280, t) == MatchEnding.Step.STATS, "stats");
        check(MatchEnding.step(480, t) == MatchEnding.Step.REWARDS, "rewards");
        check(MatchEnding.step(600, t) == MatchEnding.Step.DONE, "done");
        check(MatchEnding.ticksLeft(121, t) == 159, "ticks left in podium");
        check(MatchEnding.ticksLeft(600, t) == 0, "none at DONE");
        check(MatchEnding.playerStep(MatchEnding.Step.RESULT, MatchEnding.Step.STATS) == MatchEnding.Step.STATS, "skip ahead");
        check(MatchEnding.playerStep(MatchEnding.Step.REWARDS, MatchEnding.Step.PODIUM) == MatchEnding.Step.REWARDS, "the clock wins when later");
        check(MatchEnding.next(MatchEnding.Step.REWARDS) == MatchEnding.Step.DONE && MatchEnding.next(MatchEnding.Step.DONE) == MatchEnding.Step.DONE, "next");
        // Lines.
        check(MatchEnding.kd(6, 1).equals("6 / 1"), "kd");
        check(MatchEnding.ratio(6, 0).equals("6.0") && MatchEnding.ratio(7, 2).equals("3.5"), "ratio");
        check(MatchEnding.headlineText(e("a", 0, 0, 0, 0, 252, 0, 0), MatchEnding.Headline.OBJECTIVE).equals("4:12 on points"), "objective text");
        check(MatchEnding.headlineText(e("a", 0, 0, 38, 0, 0, 0, 0), MatchEnding.Headline.KILLS).equals("38 kills"), "kills text");
        check(MatchEnding.headlineText(e("a", 0, 0, 0, 0, 0, 17, 3), MatchEnding.Headline.KEPT).equals("17 of 20 kept"), "kept text");
        check(MatchEnding.headline(e("a", 0, 0, 38, 0, 10, 1, 9), w) == MatchEnding.Headline.KILLS, "headline kills");
        check(MatchEnding.headline(e("a", 0, 0, 1, 0, 200, 1, 9), w) == MatchEnding.Headline.OBJECTIVE, "headline objective");
        check(MatchEnding.headline(e("a", 0, 0, 0, 0, 0, 5, 0), w) == MatchEnding.Headline.KEPT, "headline kept");
        check(MatchEnding.headline(e("a", 0, 0, 0, 0, 0, 0, 0), w) == MatchEnding.Headline.KILLS, "headline tie: enum order");
        // Pass 3: the timeline.
        var tl = new MatchEnding.Timeline();
        for (int s = 0; s <= 12; s++) tl.sample(s, new int[]{s, 100 - s}, false);
        check(tl.samples.size() == 2 && tl.row(0)[0] == 5 && tl.row(0)[1] == 10 && tl.row(1)[1] == 90, "timeline: a sample at 5 s and 10 s");
        tl.sample(13, new int[]{13, 87}, true);
        check(tl.samples.size() == 3 && tl.row(0)[2] == 13 && tl.row(1)[2] == 87, "timeline: the finish adds the partial interval with the final score");
        var exact = new MatchEnding.Timeline();
        exact.sample(10, new int[]{1, 1}, false); exact.sample(10, new int[]{2, 3}, true);
        check(exact.samples.size() == 2 && exact.row(0)[1] == 2 && exact.row(1)[1] == 3, "timeline: a finish on an interval puts the final score in the last sample");
        var jump = new MatchEnding.Timeline();
        jump.sample(3, new int[]{0, 0}, false); jump.sample(181, new int[]{4, 2}, false);
        check(jump.samples.size() == 36, "timeline: a clock jump catches up (181 s gives 36 samples)");
        var lng = new MatchEnding.Timeline();
        for (int s = 0; s <= 1500; s++) lng.sample(s, new int[]{s, s / 2}, false);
        check(lng.samples.size() <= MatchEnding.Timeline.MAX && lng.interval == 10 && lng.samples.size() == 150 && lng.row(0)[0] == 10 && lng.row(0)[149] == 1500, "timeline: past 240 samples the interval doubles (1500 s: 150 samples of 10 s)");
        int[] row = {0, 3, 7, 7, 120, 119, 64, 300, 299, 0};
        String enc = MatchEnding.encodeRow(row);
        check(Arrays.equals(MatchEnding.decodeRow(enc), row) && MatchEnding.encodeRow(new int[]{1, 2, 3}).length() == 3, "timeline row: encodes and decodes (" + enc + ")");
        check(MatchEnding.decodeRow("~zz") == null && MatchEnding.decodeRow("!") == null && MatchEnding.decodeRow("").length == 0, "timeline row: bad text is refused");
        // Pass 3: personal bests.
        Map<String, Integer> rec = new LinkedHashMap<>();
        check(MatchEnding.bests(rec, Map.of("kills", 11, "score", 400)).isEmpty() && rec.equals(Map.of("kills", 11, "score", 400)), "bests: the first match sets records quietly");
        var better = MatchEnding.bests(rec, Map.of("kills", 14, "score", 300, "captures", 2));
        check(better.size() == 1 && better.get(0).equals(new MatchEnding.Best("kills", "Kills", 14, 11)) && rec.equals(Map.of("kills", 14, "score", 400, "captures", 2)), "bests: a higher value beats its record; a lower one does not; a new key is quiet");
        check(MatchEnding.bests(rec, Map.of("kills", 14)).isEmpty(), "bests: an equal value is no best");
        check(MatchEnding.bestText("kills", 14, 11).equals("New best: 14 kills (was 11)") && MatchEnding.bestText("keptPct", 90, 75).equals("New best: 90% kept alive (was 75%)")
            && MatchEnding.bestText("score", 812, 640).equals("New best: a score of 812 (was 640)") && MatchEnding.bestText("captures", 1, 0).equals("New best: 1 capture (was 0)"), "bests: the REWARDS words");
        System.out.println("MatchEndingCheck: " + checks + " checks passed");
    }
}
