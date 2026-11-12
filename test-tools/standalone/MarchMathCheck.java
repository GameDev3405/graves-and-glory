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

    static Spec.CampaignChaptersRow oathChapter() { return MarchMath.chapter(1, Spec.CAMPAIGNCHAPTERS); }

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
        // enemies (brief N1): team 1's bot seats field the battle's enemies rows by lane; no off-chapter Legendary on either side
        String[][] foes = {{"tidecallers_easy"}, {}, {"tidecallers_normal", "tidecallers_normal"}, {"wildmoot_normal", "wildmoot_normal"}, {"wildmoot_normal", "wildmoot_normal", "wildmoot_normal"}, {"wildmoot_hard", "wildmoot_normal", "wildmoot_normal"}};
        Set<String> offChapter = Set.of("gb_hollowking", "ic_colossus", "tc_admiral");
        for (int i = 0; i < 6; i++) {
            var r = row(i + 1);
            check(MarchMath.enemyIds(r).equals(List.of(foes[i])), "enemies: battle " + (i + 1) + " names " + Arrays.toString(foes[i]));
            List<String> fielded = new ArrayList<>();
            if (!r.mode().equals("survival")) for (int lane = 0; lane < MarchMath.seatSize(r); lane++) fielded.add(MarchMath.enemyId(r, lane));
            check(fielded.equals(List.of(foes[i])), "enemies: battle " + (i + 1) + "'s enemy seats field exactly " + Arrays.toString(foes[i]) + ", got " + fielded);
            List<String> sides = new ArrayList<>(fielded); sides.addAll(MarchMath.allyIds(r));
            int oldHorn = 0;
            for (String id : sides) {
                var bb = Arrays.stream(Spec.BOTBANNERS).filter(x -> x.id().equals(id)).findFirst().orElseThrow();
                for (String t : bb.slots().split(",")) { check(!offChapter.contains(t), "enemies: no off-chapter Legendary (" + t + ") in battle " + (i + 1)); if (t.equals("wm_oldhorn")) oldHorn++; }
            }
            check(oldHorn == (i == 5 ? 1 : 0), "enemies: Old Horn takes the field once, in battle 6 only (battle " + (i + 1) + ": " + oldHorn + ")");
        }
        check(MarchMath.enemyId(row(2), 0) == null && MarchMath.enemyId(null, 0) == null, "enemies: a battle with none (Survival's director) falls back to tier and lane picking");
        check(MarchMath.enemyId(row(1), 3).equals("tidecallers_easy"), "enemies: a lane past the list wraps");
        // the House banner per chapter (brief G1): a house chapter fields its own botBanners row for the commander, never the collection;
        // an own chapter (The Oath) fields the commander's banner; the house row never joins a tier's pool
        check(MarchMath.houseBannerRow(row(1), Spec.CAMPAIGNCHAPTERS) == null && MarchMath.houseBannerRow(null, Spec.CAMPAIGNCHAPTERS) == null, "banner: The Oath fields the commander's own banner");
        var wild = new Spec.CampaignChaptersRow[]{oathChapter(), new Spec.CampaignChaptersRow("hunt", 2, "The Hunt", "wildmoot", "house", "ally_wildmoot_normal", 2, "End.", "none")};
        var huntBattle = battle("hunt-1", 2, 7, 1, "none", "gilded_normal");
        check("ally_wildmoot_normal".equals(MarchMath.houseBannerRow(huntBattle, wild)) && MarchMath.houseBannerRow(row(1), wild) == null, "banner: a house chapter's battle fields its House row; its other chapters keep their own banner");
        check(MarchMath.reserved("ally_wildmoot_normal", ROWS, wild) && !MarchMath.reserved("wildmoot_normal", ROWS, wild), "banner: a chapter's House row is reserved from the bot pools; a plain tier row is not");
        check(MarchMath.reserved("ally_gilded_easy", Spec.CAMPAIGNBATTLES, Spec.CAMPAIGNCHAPTERS) && !MarchMath.reserved("gilded_easy", Spec.CAMPAIGNBATTLES, Spec.CAMPAIGNCHAPTERS), "banner: ally rows stay reserved");
        // season locks (brief G2): a chapter opens when the server's season reaches it, from the start dates in the server config;
        // season 1 is always open, earlier chapters stay open, the order lock still applies, and a locked chapter names its date
        var dates = MarchMath.Seasons.parse("{\"starts\": [\"2026-11-20\", \"2026-12-20\", \"bad\"]}");
        check(dates.equals(Arrays.asList(java.time.LocalDate.of(2026, 11, 20), java.time.LocalDate.of(2026, 12, 20), null)), "season: start dates parse; a bad entry keeps its place as null: " + dates);
        var gap = Arrays.asList(java.time.LocalDate.of(2026, 11, 20), null, java.time.LocalDate.of(2027, 1, 20));
        check(MarchMath.Seasons.current(gap, java.time.LocalDate.of(2027, 1, 20)) == 3 && MarchMath.Seasons.current(gap, java.time.LocalDate.of(2027, 1, 19)) == 1, "season: a bad date never shifts the seasons after it");
        long clock = 1_763_000_000_000L; // C06-F1: the season file is read on the first ask (never-read sentinel Long.MIN_VALUE), then at most every 30 s
        check(MarchMath.Seasons.reread(Long.MIN_VALUE, clock) && MarchMath.Seasons.reread(clock - 30_000L, clock) && !MarchMath.Seasons.reread(clock - 29_999L, clock), "season: the file is read first, then every 30 s");
        check(MarchMath.Seasons.parse("").isEmpty() && MarchMath.Seasons.parse("not json").isEmpty(), "season: a missing or broken file is no dates");
        check(MarchMath.Seasons.current(List.of(), java.time.LocalDate.of(2030, 1, 1)) == 1, "season: no dates, season 1");
        check(MarchMath.Seasons.current(dates, java.time.LocalDate.of(2026, 11, 19)) == 1 && MarchMath.Seasons.current(dates, java.time.LocalDate.of(2026, 11, 20)) == 1
            && MarchMath.Seasons.current(dates, java.time.LocalDate.of(2026, 12, 19)) == 1 && MarchMath.Seasons.current(dates, java.time.LocalDate.of(2026, 12, 20)) == 2, "season: season 2 starts on its date, not before");
        var seasons = new Spec.CampaignChaptersRow[]{oathChapter(), new Spec.CampaignChaptersRow("hunt", 2, "The Hunt", "wildmoot", "own", "none", 2, "End.", "none")};
        var hunt1 = battle("hunt-1", 2, 7, 1, "none", "gilded_normal");
        check(MarchMath.Seasons.lock(row(1), seasons, 1, dates).isEmpty() && MarchMath.Seasons.lock(row(6), seasons, 1, List.of()).isEmpty(), "season: The Oath (season 1) is always open");
        check(MarchMath.Seasons.lock(hunt1, seasons, 1, dates).equals("The Hunt opens with season 2 on 20 December 2026."), "season: a chapter before its season is locked and names its date: " + MarchMath.Seasons.lock(hunt1, seasons, 1, dates));
        check(MarchMath.Seasons.lock(hunt1, seasons, 1, List.of()).equals("The Hunt opens with season 2."), "season: with no date set it names the season");
        check(MarchMath.Seasons.lock(hunt1, seasons, 2, dates).isEmpty() && MarchMath.Seasons.lock(row(1), seasons, 2, dates).isEmpty(), "season: in season 2 chapter 2 opens and chapter 1 stays open");
        check(MarchMath.Seasons.lock(null, seasons, 1, dates).isEmpty(), "season: no battle, no lock");
        var withHunt = new Spec.CampaignBattlesRow[]{row(1), row(2), row(3), row(4), row(5), row(6), hunt1};
        check(MarchMath.lockedBy(hunt1, Set.of(), withHunt) != null, "season: the order lock still applies, so a late joiner starts at chapter 1");
        // chapter-aware text (brief G3): the card names the chapter and counts within it
        check(MarchMath.battleLine(row(1), Spec.CAMPAIGNBATTLES, Spec.CAMPAIGNCHAPTERS, "Easy").equals("The Oath, battle 1 of 6, Easy"), "line: " + MarchMath.battleLine(row(1), Spec.CAMPAIGNBATTLES, Spec.CAMPAIGNCHAPTERS, "Easy"));
        check(MarchMath.battleLine(row(6), Spec.CAMPAIGNBATTLES, Spec.CAMPAIGNCHAPTERS, "Hard").equals("The Oath, battle 6 of 6, 3 v 3, Hard"), "line: sides once more than one a side: " + MarchMath.battleLine(row(6), Spec.CAMPAIGNBATTLES, Spec.CAMPAIGNCHAPTERS, "Hard"));
        var twoChapters = new Spec.CampaignBattlesRow[]{row(1), row(2), row(3), row(4), row(5), row(6), battle("hunt-1", 2, 7, 1, "none", "gilded_normal"), battle("hunt-2", 2, 8, 1, "none", "gilded_normal")};
        var huntChapters = new Spec.CampaignChaptersRow[]{oathChapter(), new Spec.CampaignChaptersRow("hunt", 2, "The Hunt", "wildmoot", "own", "none", 2, "End.", "none")};
        check(MarchMath.battleLine(twoChapters[7], twoChapters, huntChapters, "Easy").equals("The Hunt, battle 2 of 2, Easy") && MarchMath.number(twoChapters[6], twoChapters) == 1 && MarchMath.inChapter(1, twoChapters).size() == 6, "line: a second chapter counts from 1 within itself");
        // after lines and the chapter end (brief N2): the battle's line on a win only; the chapter's end line with its Pact, once
        var oath = MarchMath.chapter(1, Spec.CAMPAIGNCHAPTERS);
        check(oath != null && oath.name().equals("The Oath") && oath.chapterEnd().startsWith("Aldric, alone with the Charter.") && MarchMath.chapter(9, Spec.CAMPAIGNCHAPTERS) == null, "chapter: chapter 1 is The Oath with its end line; an unknown chapter is null");
        for (var r : ROWS) check(MarchMath.afterLine(r, true).equals(r.after()) && MarchMath.afterLine(r, false).isEmpty(), "after: " + r.id() + "'s line shows on a win and never on a loss");
        check(MarchMath.afterLine(null, true).isEmpty(), "after: no battle, no line");
        check(MarchMath.chapterEndLine(List.of(1), Spec.CAMPAIGNCHAPTERS).equals(oath.chapterEnd()) && MarchMath.chapterEndLine(List.of(), Spec.CAMPAIGNCHAPTERS).isEmpty() && MarchMath.chapterEndLine(List.of(9), Spec.CAMPAIGNCHAPTERS).isEmpty(), "chapterEnd: shown with the chapter's Pact grant, else nothing");
        Set<Integer> paidChapters = new LinkedHashSet<>();
        int shown = 0;
        for (int round = 0; round < 3; round++) { // the chapter's last battle won three times: the Pact, and so the end line, comes once
            var granted = RosterMath.pactChapters(Arrays.asList(ROWS), 6, paidChapters);
            paidChapters.addAll(granted);
            if (!MarchMath.chapterEndLine(granted, Spec.CAMPAIGNCHAPTERS).isEmpty()) shown++;
        }
        check(shown == 1, "chapterEnd: the end line shows once over repeated wins of the last battle (" + shown + ")");
        // the glimpur ebb (brief N3): casters' glow dims now and then in ebb battles; never off, never in sync, never without the flag
        int dimmed = 0, starts = 0, windows = 0; boolean was = false, syncedAll = true;
        for (long t = 0; t < 24000; t++) {
            float g = MarchMath.Ebb.glow(true, 41, t);
            check(g >= MarchMath.Ebb.LOW && g <= 1f, "ebb: glow stays within LOW..1 at tick " + t);
            check(MarchMath.Ebb.glow(false, 41, t) == 1f, "ebb: no flag, full glow");
            check(g == MarchMath.Ebb.glow(true, 41, t), "ebb: deterministic per entity and world time, so server and client agree");
            boolean now = g < 1f;
            if (now) dimmed++;
            if (now && !was) windows++;
            if (MarchMath.Ebb.starts(true, 41, t)) { starts++; check(now && !was, "ebb: starts marks the first dim tick at " + t); }
            check(!MarchMath.Ebb.starts(false, 41, t), "ebb: no flag, no flicker sound");
            if (now != (MarchMath.Ebb.glow(true, 42, t) < 1f)) syncedAll = false;
            was = now;
        }
        check(starts == windows && windows >= 40 && windows <= 80, "ebb: about one flicker per 20 seconds over 20 minutes (" + windows + ")");
        check(dimmed > 24000 * 0.03 && dimmed < 24000 * 0.15, "ebb: dim now and then, not most of the time (" + dimmed + " of 24000 ticks)");
        check(!syncedAll, "ebb: two casters do not flicker in step");
        check(MarchMath.Ebb.LOW > 0f && MarchMath.Ebb.LOW < 0.5f, "ebb: a dimmed glow fades, it never goes fully dark");
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
