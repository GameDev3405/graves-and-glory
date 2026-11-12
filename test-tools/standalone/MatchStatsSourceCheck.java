package gg.gravesandglory;

import java.nio.file.*;

/** The match ending's new per-army statistics: the fields exist on SquadStats, and each is incremented where the plan says (source checks: the wiring needs a game to run). */
public final class MatchStatsSourceCheck {
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
    static String read(String file) throws Exception {
        return Files.readString(source().resolve(file));
    }

    public static void main(String[] args) throws Exception {
        for (String f : new String[]{"commanderKills", "commanderDowns", "deaths", "damageDealt", "captures", "goldSpent", "trained", "upgrades", "orders", "kept"}) {
            try {
                check(MatchStats.SquadStats.class.getField(f).getType() == int.class, f + " is a public int");
            } catch (NoSuchFieldException e) {
                throw new AssertionError("SquadStats has no field " + f);
            }
        }
        try {
            var fielded = MatchStats.SquadStats.class.getField("fielded");
            check(java.util.Map.class.isAssignableFrom(fielded.getType()) && java.lang.reflect.Modifier.isFinal(fielded.getModifiers()), "fielded is a final Map");
            check(new MatchStats.SquadStats().fielded.isEmpty(), "fielded starts empty");
        } catch (NoSuchFieldException e) {
            throw new AssertionError("SquadStats has no field fielded");
        }
        String game = read("GravesAndGlory.java");
        for (String s : new String[]{"stats.commanderKills++", "stats.commanderDowns++", "stats.deaths++", "stats.damageDealt", "stats.captures++", "stats.trained++", "stats.goldSpent", "stats.fielded.merge("})
            check(game.contains(s), "GravesAndGlory counts " + s);
        check(read("Veterans.java").contains("stats.upgrades++"), "Veterans.purchase counts upgrades");
        check(read("Veterans.java").contains("stats.goldSpent"), "Veterans.purchase counts the gold it spent");
        check(read("OrderRules.java").contains("stats.orders++"), "OrderRules counts orders");
        check(read("Campaign.java").contains("stats.goldSpent"), "the bot's training buys count gold spent");
        check(read("BattleMode.java").contains("boolean onObjective("), "BattleMode declares onObjective");
        for (String m : new String[]{"SiegeMode", "ConvoyMode", "SurvivalMode"})
            check(read(m + ".java").contains("boolean onObjective("), m + " overrides onObjective");
        check(game.contains("onObjective("), "the objective count asks the mode");
        System.out.println("MatchStatsSourceCheck passed " + checks + " checks");
    }
}
