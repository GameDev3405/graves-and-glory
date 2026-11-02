package gg.gravesandglory;

import java.nio.file.*;

/**
 * The server rules Kort approved on 2026-11-01 (Q20): a stopped or crashed match is void, a dropped commander's army waits
 * disconnectGraceSeconds (60 s) before a bot takes command and is handed back on rejoin, and /warp is ops only. Never shipped.
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
        check(!MatchRules.settles(MatchRules.Ending.STOPPED), "a stopped match is void");
        check(!MatchRules.settles(MatchRules.Ending.CRASHED), "a crashed match is void");

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
        check(!voided.contains("save(") && !voided.contains("gold") && !voided.contains("finish("), "voidMatch pays nothing, costs nothing and saves no profile");
        String finish = body(campaign, "void finish(int winner)");
        check(finish.contains("if(!b.running)return;"), "finish settles only a running match (a restarted server has none)");

        // Wiring: leave starts the wait, the bot brain skips a waiting army, and the owner's rejoin hands command back.
        String leave = body(game, "void leave(UUID owner)");
        check(leave.contains("s.takeoverAt=MatchRules.takeoverAt(time,Spec.CAMPAIGN[0].disconnectGraceSeconds())"), "a drop mid-battle starts the grace period from the sheet");
        check(!leave.contains("s.order=\"attack\""), "the army keeps its last orders while it waits");
        check(campaign.contains("if(MatchRules.waiting(b.time,s.takeoverAt))continue;if(s.takeoverAt>0){s.takeoverAt=0;s.order=\"attack\";}"), "the bot brain waits out the grace period, then takes command");
        check(body(game, "void join(ServerPlayerEntity p)").contains("s.bot=false;s.seated=true;s.takeoverAt=0;"), "rejoining hands command back and ends the wait");

        // /warp is ops only.
        check(MatchRules.OPS == 2, "ops-only is permission level 2");
        check(build.contains("CommandManager.literal(\"warp\").requires(s->s.hasPermissionLevel(MatchRules.OPS))"), "/warp requires ops");

        System.out.println("MatchRulesCheck: " + checks + " checks passed");
    }
}
