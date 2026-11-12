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
        System.out.println("MatchEndingCheck: " + checks + " checks passed");
    }
}
