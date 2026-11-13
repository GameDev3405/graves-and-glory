package gg.gravesandglory;

import java.util.*;

/** The match ending's awards (EndingAwards): the rules table, the floors, ties to the higher score, one award each, and none for a commander who left. */
public final class EndingAwardsCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    /** id, commander downs, kills by hand, captures, objective seconds, army kills, kept, lost, score. */
    static EndingAwards.Line l(String id, int downs, int ck, int caps, int obj, int army, int kept, int lost, int score) {
        return new EndingAwards.Line(id, downs, ck, caps, obj, army, kept, lost, score, false);
    }
    static EndingAwards.Line left(EndingAwards.Line x) {
        return new EndingAwards.Line(x.id(), x.commanderDowns(), x.commanderKills(), x.captures(), x.objectiveSeconds(), x.armyKills(), x.kept(), x.lost(), x.score(), true);
    }
    static EndingAwards.Award of(List<EndingAwards.Line> lines, String id) { return EndingAwards.awards(lines).get(id); }

    public static void main(String[] args) {
        var K = EndingAwards.Award.KINGSLAYER; var D = EndingAwards.Award.DUELIST; var B = EndingAwards.Award.BANNER_BREAKER;
        var H = EndingAwards.Award.HOLDFAST; var U = EndingAwards.Award.BUTCHER; var I = EndingAwards.Award.IRON_WALL;
        // A spread: each army leads one stat.
        List<EndingAwards.Line> spread = List.of(
            l("king", 3, 0, 0, 0, 0, 0, 0, 90), l("duel", 0, 4, 0, 0, 0, 0, 0, 40), l("flag", 0, 0, 2, 0, 0, 0, 0, 10),
            l("hold", 0, 0, 0, 120, 0, 0, 0, 120), l("butch", 0, 0, 0, 0, 30, 2, 30, 300), l("wall", 0, 0, 0, 0, 0, 12, 0, 100));
        var got = EndingAwards.awards(spread);
        check(got.equals(Map.of("king", K, "duel", D, "flag", B, "hold", H, "butch", U, "wall", I)), "the spread: one award each " + got);
        // The order matters: an army that leads two keeps the first; the second is not passed down.
        List<EndingAwards.Line> both = List.of(l("a", 2, 0, 0, 0, 20, 0, 0, 200), l("b", 0, 0, 0, 0, 10, 0, 0, 100));
        check(EndingAwards.awards(both).equals(Map.of("a", K)), "one award each, the first in order; Butcher is not handed on");
        // Floors.
        check(of(List.of(l("a", 0, 0, 0, 0, 0, 0, 0, 0)), "a") == null, "nothing earned, nothing won");
        check(of(List.of(l("a", 0, 0, 0, 29, 0, 0, 0, 0)), "a") == null, "Holdfast needs 30 s");
        check(of(List.of(l("a", 0, 0, 0, 30, 0, 0, 0, 0)), "a") == H, "30 s is enough");
        check(of(List.of(l("a", 0, 0, 0, 0, 4, 0, 0, 0)), "a") == null, "Butcher needs 5 army kills");
        check(of(List.of(l("a", 0, 0, 0, 0, 5, 0, 0, 0)), "a") == U, "5 army kills is enough");
        check(of(List.of(l("a", 0, 0, 0, 0, 0, 9, 0, 0)), "a") == null, "Iron Wall needs 10 fielded");
        check(of(List.of(l("a", 0, 0, 0, 0, 0, 6, 4, 0)), "a") == I, "10 fielded is enough");
        // Iron Wall compares the ratio, not the count.
        check(of(List.of(l("many", 0, 0, 0, 0, 0, 18, 12, 0), l("few", 0, 0, 0, 0, 0, 10, 0, 0)), "few") == I, "10 of 10 beats 18 of 30");
        // Ties go to the higher score; a full tie gives nobody the award.
        check(of(List.of(l("lo", 1, 0, 0, 0, 0, 0, 0, 50), l("hi", 1, 0, 0, 0, 0, 0, 0, 60)), "hi") == K, "tie to the higher score");
        check(EndingAwards.winner(K, List.of(l("x", 1, 0, 0, 0, 0, 0, 0, 50), l("y", 1, 0, 0, 0, 0, 0, 0, 50))) == null, "a full tie: nobody");
        check(EndingAwards.winner(K, List.of(l("x", 1, 0, 0, 0, 0, 0, 0, 50), l("y", 1, 0, 0, 0, 0, 0, 0, 50), l("z", 2, 0, 0, 0, 0, 0, 0, 10))).id().equals("z"), "a better value beats a tie");
        // Left the battle: no award, and the next best wins it.
        List<EndingAwards.Line> gone = List.of(left(l("gone", 5, 0, 0, 0, 0, 0, 0, 500)), l("stay", 1, 0, 0, 0, 0, 0, 0, 10));
        check(of(gone, "gone") == null && of(gone, "stay") == K, "a commander who left wins nothing; the next best does");
        check(EndingAwards.awards(List.of()).isEmpty(), "no armies");
        // Details and titles.
        check(EndingAwards.detail(K, l("a", 1, 0, 0, 0, 0, 0, 0, 0)).equals("1 commander downed"), "Kingslayer detail");
        check(EndingAwards.detail(H, l("a", 0, 0, 0, 125, 0, 0, 0, 0)).equals("2:05 on points"), "Holdfast detail");
        check(EndingAwards.detail(I, l("a", 0, 0, 0, 0, 0, 18, 2, 0)).equals("18 of 20 kept"), "Iron Wall detail");
        check(EndingAwards.detail(U, l("a", 0, 0, 0, 0, 41, 0, 0, 0)).equals("41 army kills"), "Butcher detail");
        for (var a : EndingAwards.Award.values()) check(EndingAwards.byTitle(a.title) == a, "title round trip " + a);
        check(EndingAwards.byTitle("Nope") == null, "unknown title");
        System.out.println("EndingAwardsCheck: " + checks + " checks passed");
    }
}
