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

        // Wiring: leave starts the wait, the bot brain skips a waiting army, and the owner's rejoin hands command back.
        String leave = body(game, "void leave(UUID owner)");
        check(leave.contains("s.takeoverAt=MatchRules.takeoverAt(time,Spec.CAMPAIGN[0].disconnectGraceSeconds())"), "a drop mid-battle starts the grace period from the sheet");
        check(!leave.contains("s.order=\"attack\""), "the army keeps its last orders while it waits");
        check(campaign.contains("if(MatchRules.waiting(b.time,s.takeoverAt))continue;if(s.takeoverAt>0){s.takeoverAt=0;s.order=\"attack\";}"), "the bot brain waits out the grace period, then takes command");
        check(body(game, "void join(ServerPlayerEntity p)").contains("s.bot=false;s.seated=true;s.takeoverAt=0;"), "rejoining hands command back and ends the wait");

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
