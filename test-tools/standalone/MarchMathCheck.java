package gg.gravesandglory;

import java.util.*;

/** Pure campaign bookkeeping (plan 2C.3, chapter 1 "The Oath"): the six-battle sheet, the order lock, settling a battle and the bonus trackers. Never shipped. */
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
    static Spec.CampaignBattlesRow synthetic(int size) { return battle("x", 1, 1, size, "none", "none"); }
    /** A test row: the fields the bookkeeping reads, the rest blank. */
    static Spec.CampaignBattlesRow battle(String id, int chapter, int order, int size, String allies, String enemies) {
        return new Spec.CampaignBattlesRow(id, chapter, order, id, 0, size, "easy", false, "", "", "domination", "gilded", "none", allies, enemies, "", false, "none");
    }

    public static void main(String[] args) {
        // sheet: chapter 1 "The Oath" (chapter-1-battles.md section 5, Kort 2026-11-09): six battles in order, on their maps and modes, all Gilded
        String[][] plan = {{"The Drowned Crossings", "rivergate", "domination", "easy"}, {"The Moon Shrine Watch", "hollowwood", "survival", "easy"}, {"Smoke at Ironhold", "ironhold", "siege", "normal"},
            {"The Hunters' Road", "frostmarch", "convoy", "normal"}, {"The Grazing Grounds", "hollowwood", "domination", "normal"}, {"The Antlered Alpha", "kingsfield", "domination", "hard"}};
        int[] sizes = {1, 2, 2, 2, 3, 3};
        check(ROWS.length == 6, "sheet: six campaign battles");
        for (int i = 0; i < 6; i++) {
            var r = row(i + 1);
            check(r.id().equals("oath-" + (i + 1)) && r.name().equals(plan[i][0]) && Spec.MAPS[r.map()].id().equals(plan[i][1]) && r.mode().equals(plan[i][2]) && r.difficulty().equals(plan[i][3]) && r.size() == sizes[i], "sheet: battle " + (i + 1) + " is " + plan[i][0] + " on " + plan[i][1]);
            check(r.house().equals("gilded") && r.chapter() == 1 && r.placeholder().equals("none") && r.lessons() == (i == 0), "sheet: battle " + (i + 1) + " is Gilded, chapter 1, settled, lessons on battle 1 only");
            var bonus = MarchMath.bonus(r, Spec.BONUSOBJECTIVES);
            check(bonus != null && bonus.id().startsWith("o" + (i + 1) + "_") && !bonus.kind().equals("kill_unit"), "sheet: battle " + (i + 1) + " names its own oath bonus, never kill_unit");
            check(!r.after().isBlank() && r.ebb() == (i >= 3), "sheet: battle " + (i + 1) + " has an after line; the ebb is on battles 4 to 6 only");
        }
        String[][] kinds = {{"hold_all", "0"}, {"lose_at_most", "8"}, {"stage_by", "360"}, {"never_rolled", "0"}, {"lose_at_most", "12"}, {"hold_all", "0"}};
        for (int i = 0; i < 6; i++) { var bonus = MarchMath.bonus(row(i + 1), Spec.BONUSOBJECTIVES); check(bonus.kind().equals(kinds[i][0]) && bonus.value() == Integer.parseInt(kinds[i][1]), "sheet: battle " + (i + 1) + "'s bonus is " + kinds[i][0] + " " + kinds[i][1]); }
        check(MarchMath.bonus(row(3), Spec.BONUSOBJECTIVES).target().equals("gate"), "sheet: battle 3 breaks the gate by 360 seconds");
        check(Arrays.stream(Spec.CAMPAIGNBATTLES).noneMatch(r -> r.id().startsWith("march-")) && Arrays.stream(Spec.BONUSOBJECTIVES).noneMatch(b -> b.id().matches("b[0-9]_.*")), "sheet: no march-* row or bonus id remains");
        // allies (brief A2): dedicated ally rows, one per allied seat, each reserved from every tier's pool
        String[][] allyPlan = {{}, {"ally_gilded_easy"}, {"ally_ironclad_normal"}, {"ally_gilded_normal"}, {"ally_gilded_normal", "ally_gilded_normal_b"}, {"ally_gilded_normal", "ally_gilded_normal_b"}};
        for (int i = 0; i < 6; i++) {
            check(MarchMath.allyIds(row(i + 1)).equals(List.of(allyPlan[i])), "sheet: battle " + (i + 1) + "'s allies are " + Arrays.toString(allyPlan[i]));
            for (String a : allyPlan[i]) check(MarchMath.reservedAlly(a, ROWS) && Arrays.stream(Spec.BOTBANNERS).anyMatch(x -> x.id().equals(a)), "sheet: ally " + a + " exists and is reserved");
        }
        check(!MarchMath.reservedAlly("gilded_normal", ROWS) && !MarchMath.reservedAlly("tidecallers_easy", ROWS), "allies: the House rows the ally rows copy stay in their tier's pool");
        check(Spec.BOTCOMMAND[Spec.BOTCOMMAND.length - 1].id().equals(row(6).difficulty()), "sheet: battle 6 plays the last (hardest) botCommand tier");
        var b1 = MarchMath.bonus(row(1), Spec.BONUSOBJECTIVES);
        check(b1.id().equals("o1_hold_all") && b1.kind().equals("hold_all") && b1.shards() == 5, "sheet: battle 1's bonus holds all three crossings for 5 shards");
        // cleared, lockedBy, nextUnwon, chapterDone
        check(MarchMath.cleared(won(), ROWS) == 0 && MarchMath.cleared(won("oath-1", "oath-2"), ROWS) == 2 && MarchMath.cleared(won("oath-1", "oath-3"), ROWS) == 1 && MarchMath.cleared(won("oath-2"), ROWS) == 0, "cleared: the unbroken run from oath-1");
        check(MarchMath.lockedBy(row(1), won(), ROWS) == null && MarchMath.lockedBy(row(2), won(), ROWS) == row(1) && MarchMath.lockedBy(row(2), won("oath-1"), ROWS) == null, "lockedBy: battle 1 is open; battle 2 waits for battle 1");
        check(MarchMath.lockedBy(row(4), won("oath-1", "oath-3"), ROWS) == row(2) && MarchMath.lockedBy(row(1), won("oath-2"), ROWS) == null, "lockedBy: the lowest unwon earlier battle; a replay of a won battle is open");
        check(MarchMath.nextUnwon(won(), ROWS) == row(1) && MarchMath.nextUnwon(won("oath-1", "oath-2"), ROWS) == row(3) && MarchMath.nextUnwon(won("oath-1", "oath-2", "oath-3", "oath-4", "oath-5", "oath-6"), ROWS) == null, "nextUnwon: the next battle, null when all six are won");
        int chapter = row(1).chapter();
        Set<String> all = won();
        for (var r : ROWS) if (r.chapter() == chapter) all.add(r.id());
        check(!MarchMath.chapterDone(chapter, won(), ROWS) && MarchMath.chapterDone(chapter, all, ROWS) && !MarchMath.chapterDone(99, all, ROWS), "chapterDone: every battle of the chapter; false for a chapter with none");
        // settle
        Set<String> battles = won(), claimed = won();
        var first = MarchMath.settle(row(1), b1, true, true, battles, claimed, HOUSES, ROWS);
        check(first.firstWin() && first.cleared() == 1 && first.bonusPaid() && first.bonusShards() == 5 && first.bonusGold() == b1.gold() && first.bonusHouse().equals("gilded") && battles.equals(won("oath-1")) && claimed.equals(won("o1_hold_all")), "settle: a first win with the bonus kept records oath-1 and pays 5 Gilded shards and the bonus gold once");
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
        check(finale.bonusPaid() && finale.bonusShards() == 5 && finale.bonusGold() == 60 && finale.bonusHouse().equals("gilded") && sixClaimed.equals(won("o6_hold_all")), "settle: battle 6 pays 5 Gilded shards and 60 gold once the bonus is kept");
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
        // seat size (plan 2C.6): a March battle seats its row's size per team, practice seats one, and nothing leaves the six-seat lobby
        check(MarchMath.seatSize(null) == 1, "seatSize: practice (no row) is one a side");
        for (var r : ROWS) check(MarchMath.seatSize(r) == Math.max(1, Math.min(3, r.size())), "seatSize: " + r.id() + " follows its row's size within 1 to 3");
        var big = synthetic(9);
        var zero = synthetic(0);
        var two = synthetic(2);
        check(MarchMath.seatSize(big) == 3 && MarchMath.seatSize(zero) == 1 && MarchMath.seatSize(two) == 2, "seatSize: held to 1..3 (9 -> 3, 0 -> 1, 2 -> 2)");
        System.out.println("MarchMathCheck: " + checks + " checks passed");
    }
}
