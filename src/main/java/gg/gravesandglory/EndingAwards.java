package gg.gravesandglory;

import java.util.*;

/**
 * The match ending's awards (ending polish spec 2026-11-13, section 2): pure rules, no Minecraft types. Computed once at finish from each
 * seated army's stats. The awards are tried in order; each goes to the army with the highest value that meets the award's floor, a tie on
 * the value going to the higher match score; a tie on both gives the award to nobody (it is won outright or not at all). Each army keeps only
 * the first award it wins, so a later award it would also win is simply not given. An army whose commander left the battle wins nothing.
 */
public final class EndingAwards {
    private EndingAwards() {}

    public enum Award {
        KINGSLAYER("Kingslayer"), DUELIST("Duelist"), BANNER_BREAKER("Banner Breaker"), HOLDFAST("Holdfast"), BUTCHER("Butcher"), IRON_WALL("Iron Wall");
        public final String title;
        Award(String title) { this.title = title; }
    }

    /** One army's numbers: enemy commanders downed by the army, kills by the commander's own hand, captures, objective seconds,
     *  army kills (troops' kills, the commander's excluded), troops kept and lost, the match score, and whether its commander left. */
    public record Line(String id, int commanderDowns, int commanderKills, int captures, int objectiveSeconds, int armyKills, int kept, int lost, int score, boolean left) {
        int fielded() { return kept + lost; }
    }

    /** The floor each award needs (Iron Wall's is troops fielded). */
    static boolean meets(Award a, Line l) {
        return switch (a) {
            case KINGSLAYER -> l.commanderDowns() >= 1;
            case DUELIST -> l.commanderKills() >= 1;
            case BANNER_BREAKER -> l.captures() >= 1;
            case HOLDFAST -> l.objectiveSeconds() >= 30;
            case BUTCHER -> l.armyKills() >= 5;
            case IRON_WALL -> l.fielded() >= 10;
        };
    }

    /** Compares two lines on an award's value: positive when a is better. Iron Wall compares kept / fielded exactly. */
    static int compare(Award aw, Line a, Line b) {
        return switch (aw) {
            case KINGSLAYER -> Integer.compare(a.commanderDowns(), b.commanderDowns());
            case DUELIST -> Integer.compare(a.commanderKills(), b.commanderKills());
            case BANNER_BREAKER -> Integer.compare(a.captures(), b.captures());
            case HOLDFAST -> Integer.compare(a.objectiveSeconds(), b.objectiveSeconds());
            case BUTCHER -> Integer.compare(a.armyKills(), b.armyKills());
            case IRON_WALL -> Long.compare((long) a.kept() * b.fielded(), (long) b.kept() * a.fielded());
        };
    }

    /** The winner of one award among the lines (null for none): the best eligible value, ties to the higher score, a full tie to nobody. */
    public static Line winner(Award aw, List<Line> lines) {
        Line best = null; boolean tied = false;
        for (Line l : lines) {
            if (l.left() || !meets(aw, l)) continue;
            if (best == null) { best = l; tied = false; continue; }
            int c = compare(aw, l, best);
            if (c == 0) c = Integer.compare(l.score(), best.score());
            if (c > 0) { best = l; tied = false; } else if (c == 0) tied = true;
        }
        return tied ? null : best;
    }

    /** Each line's id to its award, for the lines that won one (at most one each, the first in the award order). */
    public static Map<String, Award> awards(List<Line> lines) {
        Map<String, Award> out = new LinkedHashMap<>();
        for (Award aw : Award.values()) {
            Line w = winner(aw, lines);
            if (w != null && !out.containsKey(w.id())) out.put(w.id(), aw);
        }
        return out;
    }

    /** The award's detail for one line: "2 commanders downed", "3 kills by hand", "2 captures", "2:05 on points", "41 army kills", "18 of 20 kept". */
    public static String detail(Award aw, Line l) {
        return switch (aw) {
            case KINGSLAYER -> l.commanderDowns() + (l.commanderDowns() == 1 ? " commander downed" : " commanders downed");
            case DUELIST -> l.commanderKills() + (l.commanderKills() == 1 ? " kill by hand" : " kills by hand");
            case BANNER_BREAKER -> l.captures() + (l.captures() == 1 ? " capture" : " captures");
            case HOLDFAST -> (l.objectiveSeconds() / 60) + ":" + String.format(Locale.ROOT, "%02d", l.objectiveSeconds() % 60) + " on points";
            case BUTCHER -> l.armyKills() + " army kills";
            case IRON_WALL -> l.kept() + " of " + l.fielded() + " kept";
        };
    }

    /** The award with this title (the report carries titles), or null. */
    public static Award byTitle(String title) {
        for (Award a : Award.values()) if (a.title.equals(title)) return a;
        return null;
    }
}
