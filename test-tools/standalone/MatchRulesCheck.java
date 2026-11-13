package gg.gravesandglory;

import java.nio.file.*;

/**
 * The server rules Kort approved on 2026-11-01 (Q20) and 2026-11-02: a stopped or crashed match pays a partial reward (the participation
 * gold pro-rated by the share played, no win bonus), a dropped commander's army waits disconnectGraceSeconds (60 s) before a bot takes
 * command, his slot is held 120 s and then released, and /warp is ops only. Never shipped.
 * The pure rules run as they are; the Minecraft wiring is checked in the source, where it cannot be run without a game.
 */
public final class MatchRulesCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static Path source() {
        for (Path at = Paths.get("").toAbsolutePath(); at != null; at = at.getParent()) {
            Path s = at.resolve("src/main/java/gg/gravesandglory");
            if (Files.isDirectory(s)) return s;
        }
        throw new AssertionError("run from the repository: src/main/java/gg/gravesandglory not found");
    }
    /** The body of the method whose declaration starts with head, up to the next blank-indented declaration (good enough for these files). */
    static String body(String text, String head) {
        int i = text.indexOf(head);
        check(i >= 0, "found " + head);
        int depth = 0, j = text.indexOf('{', i);
        for (int k = j; k < text.length(); k++) {
            char c = text.charAt(k);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return text.substring(i, k + 1);
        }
        throw new AssertionError("unbalanced " + head);
    }

    public static void main(String[] args) throws Exception {
        // Void matches.
        check(MatchRules.settles(MatchRules.Ending.COMPLETED), "a match that ends on the field settles");
        check(!MatchRules.settles(MatchRules.Ending.STOPPED), "a stopped match does not settle in full");
        check(!MatchRules.settles(MatchRules.Ending.CRASHED), "a crashed match does not settle in full");

        // Partial rewards (I7, I9): participation gold times the share played, rounded down, never the win bonus.
        int base = Spec.CAMPAIGN[0].completionGold();
        check(MatchRules.partialGold(base, 450, 900) == base / 2, "half the match pays half the participation gold");
        check(MatchRules.partialGold(base, 900, 900) == base && MatchRules.partialGold(base, 5000, 900) == base, "the share never passes the whole");
        check(MatchRules.partialGold(base, 0, 900) == 0 && MatchRules.partialGold(base, 100, 0) == 0, "no time played or no match length pays nothing");
        check(MatchRules.partialGold(base, 100, 900) == base * 100 / 900, "the share rounds down");
        check(MatchRules.partialGold(base, 899, 900) < base + Spec.CAMPAIGN[0].victoryGold(), "an interrupted match never reaches the win bonus");

        // Pro-rated pay for a match that finishes (Kort 2026-11-02): paid by the share of the match a player was seated.
        int full = base + Spec.CAMPAIGN[0].victoryGold();
        long total = 400, round0 = 1_000, end = round0 + total * 20;
        long wholeTicks = MatchRules.seatedTicks(end, round0, round0, 0, -1);
        check(wholeTicks / 20 == total, "a player seated from the start is seated the whole match");
        check(MatchRules.prorate(full, wholeTicks / 20, total) == full && MatchRules.sharePercent(wholeTicks / 20, total) == 100, "a full-time player is paid in full");
        check(MatchRules.prorate(full, total + 50, total) == full, "the share never passes the whole");
        long droppedSeated = MatchRules.seatedTicks(end, round0, round0, 0, round0 + total * 5) / 20; // dropped a quarter in, never returned
        check(droppedSeated == total / 4 && MatchRules.sharePercent(droppedSeated, total) == 25, "a player who dropped at 25% was seated 25% of the match");
        check(MatchRules.prorate(full, droppedSeated, total) == full / 4, "a player who dropped at 25% is paid 25%, got " + MatchRules.prorate(full, droppedSeated, total));
        check(MatchRules.prorate(2, droppedSeated, total) == 0 && MatchRules.prorate(100, droppedSeated, total) == 25, "pro-rated counts round down");
        check(MatchRules.prorate(0, droppedSeated, total) == 0 && MatchRules.prorate(full, 0, total) == 0 && MatchRules.prorate(full, 50, 0) == 0, "nothing seated, nothing owed or no match length pays nothing");
        check(MatchRules.forgeMarks(100, true, false, 0.5, 0.5) == 100 && MatchRules.forgeMarks(100, false, false, 0.5, 0.5) == 50, "a win pays full gear shards (old Forge marks) and a loss pays the loss share");
        check(MatchRules.forgeMarks(100, true, true, 0.5, 0.5) == 50 && MatchRules.forgeMarks(100, false, true, 0.5, 0.5) == 25, "a bot-only match pays half, a lost one a quarter");
        check(MatchRules.forgeMarks(0, true, false, 0.5, 0.5) == 0 && MatchRules.forgeMarks(75, false, true, 0.5, 0.5) == 18, "no base pays nothing and 75 x 0.25 rounds down to 18");
        check(MatchRules.prorate(MatchRules.forgeMarks(100, true, false, 0.5, 0.5), 90, 180) == 50, "gear shards are pro-rated by the time seated");
        long lateSeated = MatchRules.seatedTicks(end, round0, round0 + total * 10, 0, -1) / 20; // seated for the last half
        check(lateSeated == total / 2 && MatchRules.prorate(full, lateSeated, total) == full / 2, "a late joiner is paid for the time he was seated");
        long backSeated = MatchRules.seatedTicks(end, round0, round0, total * 5, -1) / 20; // away for a quarter of the match, back for the rest
        check(backSeated == total * 3 / 4 && MatchRules.prorate(full, backSeated, total) == full * 3 / 4, "a rejoiner is paid for the time he was seated, not for the time he was away");
        long outAndBack = MatchRules.seatedTicks(end, round0, round0, total * 5, round0 + total * 15) / 20; // away a quarter, back, away again for the last quarter
        check(outAndBack == total / 2, "two absences both come off the time seated");
        check(MatchRules.seatedTicks(end, round0, round0, 0, round0) == 0 && MatchRules.seatedTicks(end, round0, round0 + total * 40, 0, -1) == 0, "time seated is never negative");
        check(MatchRules.seatedTicks(end, round0 + 100, round0, 0, -1) == end - round0 - 100, "time seated starts at the round start, not before");
        check(MatchRules.majority(total, total) && MatchRules.majority(total / 2, total) && !MatchRules.majority(total / 2 - 1, total) && !MatchRules.majority(0, 0), "a win counts for a player seated at least half the match");
        // Campaign battle pay (Kort 2026-11-02): practice gold plus the normal win's victory gold, only for a win seated at least half, pro-rated.
        int comp = Spec.CAMPAIGN[0].completionGold(), prac = Spec.CAMPAIGN[0].practiceGold(), vic = Spec.CAMPAIGN[0].victoryGold();
        check(vic > 0 && MatchRules.grossGold(true, true, true, true, comp, prac, vic) == prac + vic, "a won campaign battle pays practice gold plus the normal victory gold");
        check(MatchRules.grossGold(true, true, false, false, comp, prac, vic) == prac, "a lost campaign battle pays practice gold only");
        check(MatchRules.grossGold(true, true, true, false, comp, prac, vic) == prac, "a campaign win seated under half pays no victory gold");
        check(MatchRules.grossGold(true, false, true, true, comp, prac, vic) == prac, "plain practice never pays victory gold");
        check(MatchRules.grossGold(false, false, true, true, comp, prac, vic) == comp + vic && MatchRules.grossGold(false, false, false, false, comp, prac, vic) == comp, "a normal match is unchanged: completion, plus victory gold for a win");
        int campaignWin = MatchRules.grossGold(true, true, true, MatchRules.majority(droppedSeated * 3, total), comp, prac, vic);
        check(MatchRules.prorate(campaignWin, total / 2, total) == (prac + vic) / 2, "a campaign win seated half the match pays half of practice plus victory gold");
        check(MatchRules.prorate(MatchRules.grossGold(true, true, true, MatchRules.majority(droppedSeated, total), comp, prac, vic), droppedSeated, total) == prac / 4, "a campaign win seated 25% pays only the pro-rated practice gold");
        var shards = new java.util.LinkedHashMap<String, Integer>();
        shards.put("gilded", 2);shards.put("ironclad", 1);
        var scaled = RosterMath.prorate(new RosterMath.Rewards(shards, shards), 300, 400);
        check(scaled.shards().equals(java.util.Map.of("gilded", 1)) && scaled.renown().equals(scaled.shards()), "shards and Renown are pro-rated, a count that falls to 0 is dropped");
        check(RosterMath.prorate(new RosterMath.Rewards(shards, shards), 400, 400).shards().equals(shards), "a full-time player keeps every shard");
        var xp = new Progression.State();
        Progression.match(xp, "m1", true, true, 25);
        check(xp.xp == 37 && xp.totalBattles == 1, "XP is pro-rated by the share and the battle still counts");
        var xpFull = new Progression.State();
        Progression.match(xpFull, "m1", true, true);
        check(xpFull.xp == 150, "a full-time winner keeps 150 XP");

        // Slot hold (H8): 120 s from the drop, then released.
        check(MatchRules.HOLD_SECONDS == 120, "a dropped player's slot is held for 2 minutes");
        long dropped = 777, until = MatchRules.releaseAt(dropped);
        check(until == dropped + 2400, "the hold is 2400 ticks");
        check(!MatchRules.holdExpired(dropped, until) && !MatchRules.holdExpired(until - 1, until), "the slot is held the whole 2 minutes");
        check(MatchRules.holdExpired(until, until) && MatchRules.holdExpired(until + 1, until), "the slot is released at 2 minutes");

        // Disconnect grace, from the sheet.
        int grace = Spec.CAMPAIGN[0].disconnectGraceSeconds();
        check(grace == 60, "the sheet's disconnect grace is 60 s, got " + grace);
        long left = 12_345, at = MatchRules.takeoverAt(left, grace);
        check(at == left + 1200, "the bot takes command 1200 ticks after the drop");
        check(MatchRules.waiting(left, at) && MatchRules.waiting(at - 1, at), "the army waits for the whole grace period");
        check(!MatchRules.waiting(at, at) && !MatchRules.waiting(at + 1, at), "the bot commands from the grace period's end");
        check(!MatchRules.waiting(left, 0), "a bot seated from the start never waits");
        check(MatchRules.takeoverAt(left, -5) == left, "a negative grace is no wait");

        Path src = source();
        String game = Files.readString(src.resolve("GravesAndGlory.java"));
        String campaign = Files.readString(src.resolve("Campaign.java"));
        String build = Files.readString(src.resolve("BuildMode.java"));

        // Wiring: the server stop voids a running match before anything else; a void match saves nothing.
        check(game.contains("SERVER_STOPPING.register(server -> {if(battle!=null){battle.campaign.voidMatch(MatchRules.Ending.STOPPED);"), "SERVER_STOPPING voids the running match");
        String voided = body(campaign, "void voidMatch(MatchRules.Ending ending)");
        check(voided.contains("MatchRules.settles(ending)") && voided.contains("returnLobby()"), "voidMatch returns the armies to camp");
        check(voided.contains("payPartial(playedNow(),b.roundSeconds,practice)") && !voided.contains("victoryGold") && !voided.contains("finish("), "voidMatch pays the pro-rated share and never the win bonus");
        String partial = body(campaign, "void payPartial(");
        check(partial.contains("MatchRules.partialGold(") && !partial.contains("victoryGold") && !partial.contains("progression") && !partial.contains("shards") && !partial.contains("wins"), "a partial reward is participation gold only");
        check(body(campaign, "void checkpoint()").contains("playedNow()") && campaign.contains("if(b.time%600==0)checkpoint();"), "a running match checkpoints each player's time every 30 s for a crash");
        check(game.contains("Modes.recover(this);campaign.payInterrupted();"), "the next start pays the crashed match's checkpoint");
        check(body(campaign, "void returnLobby()").contains("deleteProgress()"), "a clean end or stop deletes the checkpoint");
        String finish = body(campaign, "void finish(int winner)");
        check(finish.contains("if(!b.running)return;"), "finish settles only a running match (a restarted server has none)");
        check(finish.contains("MatchRules.forgeMarks(Spec.CAMPAIGN[0].forgeMarks(),credited,botOnly,Spec.CAMPAIGN[0].lossMarksPct(),Spec.CAMPAIGN[0].botOnlyMarksPct())") && finish.contains("p.gearShards=Math.min(1_000_000,p.gearShards+marks)") && finish.contains("if(eligible)Forge.wear(p)") && finish.indexOf("Forge.wear(p)") < finish.indexOf("boolean saved=save(id)"), "finish pays pro-rated gear shards (the old Forge marks) and wears the gear before the save, so a failed save undoes both");
        check(finish.contains("report.addProperty(\"writs\",saved?p.troopWrits-writs0:0)") && finish.contains("report.addProperty(\"gearShards\",saved?p.gearShards-shards0:0)"), "the match report carries the writs and gear shards earned as plain numbers");

        // starsCount (war table army design): every mode counts stars today; a mode that switched them off would make Campaign.progression give 1.
        check(MatchRules.starsCount("domination") && MatchRules.starsCount("siege") && MatchRules.starsCount("survival"), "starsCount is true by default");
        check(MatchRules.starsMultiplier(false, 1.12) == 1.0 && MatchRules.starsMultiplier(true, 1.12) == 1.12, "starsOffGivesOne: stars off gives 1, stars on keeps the bonus");
        String progression = body(campaign, "double progression(GravesAndGlory.Squad s,Spec.TroopsRow t)");
        check(progression.contains("MatchRules.starsMultiplier(MatchRules.starsCount(battleMode),"), "starsOffGivesOne: Campaign.progression reads the starsCount rule");

        // Wiring: leave starts the wait, the bot brain skips a waiting army, and the owner's rejoin hands command back.
        String leave = body(game, "void leave(UUID owner)");
        check(leave.contains("s.takeoverAt=MatchRules.takeoverAt(time,Spec.CAMPAIGN[0].disconnectGraceSeconds())"), "a drop mid-battle starts the grace period from the sheet");
        check(!leave.contains("s.order=\"attack\""), "the army keeps its last orders while it waits");
        check(campaign.contains("if(MatchRules.waiting(b.time,s.takeoverAt))continue;if(s.takeoverAt>0){s.takeoverAt=0;s.order=\"attack\";}"), "the bot brain waits out the grace period, then takes command");
        check(body(game, "void join(ServerPlayerEntity p)").contains("s.bot=false;s.seated=true;s.takeoverAt=0;"), "rejoining hands command back and ends the wait");

        // Wiring: a drop records the absence, a return closes it, and finish pays a commander who is away from his saved seat, pro-rated.
        check(leave.contains("campaign.dropped(s)"), "a drop mid-battle records the absence");
        check(body(campaign, "void joined(ServerPlayerEntity p)").contains("returned(p.getUuid())"), "a seated return closes the absence");
        check(finish.contains("absentSeat.get(id)") && finish.contains("MatchRules.prorate(") && finish.contains("RosterMath.prorate(") && finish.contains("Progression.match(p.progression,matchId,credited,eligible,percent)"), "finish pays a commander who dropped, pro-rated by the time seated");
        check(body(campaign, "Map<UUID,Long> playedNow()").contains("seatedTicks(id)"), "an interrupted match pays a player's time seated, less his absences");

        // The hold: leave stamps the release tick, rejoin clears it, join and the tick release an expired one.
        check(leave.contains("campaign.vacatedUntil.put(owner,MatchRules.releaseAt(time))"), "a drop mid-battle starts the 2-minute hold");
        check(body(game, "void join(ServerPlayerEntity p)").contains("campaign.releaseExpiredHolds();") && body(game, "void join(ServerPlayerEntity p)").contains("campaign.vacatedUntil.remove(p.getUuid())"), "join releases expired holds and a returning owner ends his own");
        check(body(campaign, "void releaseExpiredHolds()").contains("vacated.remove(e.getKey())") && campaign.contains("releaseExpiredHolds();if(b.time%600"), "an expired hold drops the reservation on the tick");

        // /warp is ops only.
        check(MatchRules.OPS == 2, "ops-only is permission level 2");
        check(build.contains("CommandManager.literal(\"warp\").requires(s->s.hasPermissionLevel(MatchRules.OPS))"), "/warp requires ops");

        System.out.println("MatchRulesCheck: " + checks + " checks passed");
    }
}
