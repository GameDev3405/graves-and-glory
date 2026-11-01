package gg.gravesandglory;

import java.util.*;

/** Pure Long March bookkeeping (plan 2C.3): the six-battle sheet, the order lock, settling a battle and the bonus trackers. Never shipped. */
public final class MarchMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Spec.CampaignBattlesRow[] ROWS = Spec.CAMPAIGNBATTLES;
    static final List<String> HOUSES = Profile.houseOrder();
    static Set<String> won(String... ids) { return new LinkedHashSet<>(List.of(ids)); }
    static Spec.CampaignBattlesRow row(int order) { return MarchMath.byOrder(ROWS).get(order - 1); }

    public static void main(String[] args) {
        // sheet: the plan's six battles, in order, on the plan's maps and modes
        String[][] plan = {{"The Training Field", "rivergate", "domination", "gilded"}, {"The Iron Gate", "ironhold", "domination", "ironclad"}, {"The Long Night", "hollowwood", "survival", "graveborn"},
            {"The Frozen Standard", "frostmarch", "convoy", "wildmoot"}, {"The Five Crossings", "kingsfield", "domination", "tidecallers"}, {"The Last Watch", "ironhold", "siege", "graveborn"}};
        check(ROWS.length == 6, "sheet: six campaign battles");
        for (int i = 0; i < 6; i++) {
            var r = row(i + 1);
            check(r.id().equals("march-" + (i + 1)) && r.name().equals(plan[i][0]) && Spec.MAPS[r.map()].id().equals(plan[i][1]) && r.mode().equals(plan[i][2]) && r.house().equals(plan[i][3]), "sheet: battle " + (i + 1) + " is " + plan[i][0] + " on " + plan[i][1]);
            check(MarchMath.bonus(r, Spec.BONUSOBJECTIVES) != null && !r.placeholder().isBlank(), "sheet: battle " + (i + 1) + " names a bonus objective and a placeholder note");
        }
        // Kort 2026-10-31: The Iron Gate runs as Domination until Siege joins the lobby; its placeholder note says it goes back to siege
        check(row(2).placeholder().contains("siege"), "sheet: battle 2's Domination stand-in is a marked placeholder");
        check(row(1).placeholder().equals("none") && row(1).lessons() && !row(6).placeholder().contains("house"), "sheet: battle 1 is final; battle 6's house is settled");
        var b1 = MarchMath.bonus(row(1), Spec.BONUSOBJECTIVES);
        check(b1.id().equals("b1_hold_all") && b1.kind().equals("hold_all") && b1.shards() == 5, "sheet: battle 1's bonus holds all three crossings for 5 shards");
        // cleared, lockedBy, nextUnwon, chapterDone
        check(MarchMath.cleared(won(), ROWS) == 0 && MarchMath.cleared(won("march-1", "march-2"), ROWS) == 2 && MarchMath.cleared(won("march-1", "march-3"), ROWS) == 1 && MarchMath.cleared(won("march-2"), ROWS) == 0, "cleared: the unbroken run from march-1");
        check(MarchMath.lockedBy(row(1), won(), ROWS) == null && MarchMath.lockedBy(row(2), won(), ROWS) == row(1) && MarchMath.lockedBy(row(2), won("march-1"), ROWS) == null, "lockedBy: battle 1 is open; battle 2 waits for battle 1");
        check(MarchMath.lockedBy(row(4), won("march-1", "march-3"), ROWS) == row(2) && MarchMath.lockedBy(row(1), won("march-2"), ROWS) == null, "lockedBy: the lowest unwon earlier battle; a replay of a won battle is open");
        check(MarchMath.nextUnwon(won(), ROWS) == row(1) && MarchMath.nextUnwon(won("march-1", "march-2"), ROWS) == row(3) && MarchMath.nextUnwon(won("march-1", "march-2", "march-3", "march-4", "march-5", "march-6"), ROWS) == null, "nextUnwon: the next battle, null when all six are won");
        int chapter = row(1).chapter();
        Set<String> all = won();
        for (var r : ROWS) if (r.chapter() == chapter) all.add(r.id());
        check(!MarchMath.chapterDone(chapter, won(), ROWS) && MarchMath.chapterDone(chapter, all, ROWS) && !MarchMath.chapterDone(99, all, ROWS), "chapterDone: every battle of the chapter; false for a chapter with none");
        // settle
        Set<String> battles = won(), claimed = won();
        var first = MarchMath.settle(row(1), b1, true, true, battles, claimed, HOUSES, ROWS);
        check(first.firstWin() && first.cleared() == 1 && first.bonusPaid() && first.bonusShards() == 5 && first.bonusGold() == b1.gold() && first.bonusHouse().equals("gilded") && battles.equals(won("march-1")) && claimed.equals(won("b1_hold_all")), "settle: a first win with the bonus kept records march-1 and pays 5 Gilded shards and the bonus gold once");
        var replay = MarchMath.settle(row(1), b1, true, true, battles, claimed, HOUSES, ROWS);
        check(!replay.firstWin() && !replay.bonusPaid() && replay.bonusShards() == 0 && replay.bonusGold() == 0 && replay.bonusHouse() == null && replay.cleared() == 1, "settle: a replay pays no bonus again");
        Set<String> lostBattles = won(), lostClaimed = won();
        var loss = MarchMath.settle(row(1), b1, false, true, lostBattles, lostClaimed, HOUSES, ROWS);
        check(!loss.firstWin() && !loss.bonusPaid() && lostBattles.isEmpty() && lostClaimed.isEmpty() && loss.cleared() == 0, "settle: a loss records nothing");
        Set<String> missed = won();
        var plain = MarchMath.settle(row(1), b1, true, false, won(), missed, HOUSES, ROWS);
        check(plain.firstWin() && !plain.bonusPaid() && missed.isEmpty(), "settle: a win with the bonus missed records the battle but pays no bonus, which stays open");
        Set<String> sixClaimed = won();
        var finale = MarchMath.settle(row(6), MarchMath.bonus(row(6), Spec.BONUSOBJECTIVES), true, true, won(), sixClaimed, HOUSES, ROWS);
        check(finale.bonusPaid() && finale.bonusShards() == 5 && finale.bonusGold() == 60 && finale.bonusHouse().equals("graveborn") && sixClaimed.equals(won("b6_kill_hollowking")), "settle: battle 6 pays 5 Graveborn shards and 60 gold once the bonus is kept");
        var b4 = new MarchMath.Tracker(MarchMath.bonus(row(4), Spec.BONUSOBJECTIVES));
        check(b4.kept(true, 1), "tracker: battle 4's never_rolled bonus is kept on a win with no roll back");
        b4.rolled();
        check(!b4.kept(true, 1), "tracker: battle 4's never_rolled bonus is lost once the wagon rolls back");
        // trackers
        var hold = new MarchMath.Tracker("hold_all", 0, "none");
        hold.held(2, 3);
        check(!hold.kept(true, 60), "tracker hold_all: two of three is not all");
        hold.held(3, 3);
        check(hold.kept(true, 60) && !hold.kept(false, 60), "tracker hold_all: all three at one moment, and only with the win");
        var lose = new MarchMath.Tracker("lose_at_most", 3, "none");
        lose.lost(2); lose.lost(-5); lose.lost(1);
        check(lose.kept(true, 60), "tracker lose_at_most: 3 lost of at most 3 (a negative count adds nothing)");
        lose.lost(1);
        check(!lose.kept(true, 60), "tracker lose_at_most: 4 lost is too many");
        var stage = new MarchMath.Tracker("stage_by", 120, "gate");
        stage.staged("wall", 10);
        check(!stage.kept(true, 300), "tracker stage_by: another stage does not count");
        stage.staged("gate", 100); stage.staged("gate", 200);
        check(stage.kept(true, 300), "tracker stage_by: the first time the stage is reached counts");
        var late = new MarchMath.Tracker("stage_by", 120, "gate");
        late.staged("gate", 121);
        check(!late.kept(true, 300), "tracker stage_by: reached after the limit");
        var rolled = new MarchMath.Tracker("never_rolled", 0, "none");
        check(rolled.kept(true, 60), "tracker never_rolled: a wagon that never rolled back keeps it");
        check(!rolled.kept(false, 60), "tracker never_rolled: needs the win");
        rolled.rolled();
        check(!rolled.kept(true, 60), "tracker never_rolled: one roll back loses it");
        var fast = new MarchMath.Tracker("win_within", 300, "none");
        check(fast.kept(true, 300) && !fast.kept(true, 301) && !fast.kept(false, 10), "tracker win_within: a win by the limit");
        var kill = new MarchMath.Tracker("kill_unit", 0, "revenant");
        kill.killed("revenant", false); kill.killed("knight", true);
        check(!kill.kept(true, 60), "tracker kill_unit: another squad's kill or another troop does not count");
        kill.killed("revenant", true);
        check(kill.kept(true, 60), "tracker kill_unit: the target troop by the player's own squad");
        System.out.println("MarchMathCheck: " + checks + " checks passed");
    }
}
