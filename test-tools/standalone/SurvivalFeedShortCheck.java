package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/** Draft copy check: the proposed shorter Survival feed lines (docs/drafts/survival-feed-short-2026-11-02.md) each fit the Compact
 *  feed row, measured with FeedFitCheck.width like ModeHudFitCheck. Every site name is kept. Prints the draft table with -Dshort.table=true.
 *  Copy needs Kort's approval; ModeMath is unchanged. Checked by javac alone, never shipped. */
public final class SurvivalFeedShortCheck {
    static int checks, failed;
    static final int FEED_PX = UiMath.feedTextW(UiMath.HUD_COMPACT);

    static final String SHRINE = "The dead take the shrine";
    static final String STANDS = "The shrine stands till dawn";
    static final String HOLD = "The dead hold the shrine";
    /** One feed line per site, the name kept as the sheet has it. */
    static List<String> rise(List<String> sites) {
        if (sites.isEmpty()) return List.of("The dead rise");
        return sites.stream().map(x -> "Dead rise: " + x).toList();
    }
    static String rest(int wave, int gold) { return gold > 0 ? "Wave " + wave + " at rest, +" + gold + " gold" : "Wave " + wave + " laid to rest"; }

    static void row(List<String[]> rows, String cur, String... prop) { rows.add(new String[]{cur, String.join(" / ", prop), String.valueOf(prop.length)}); }

    public static void main(String[] args) {
        List<String> sites = new ArrayList<>();
        for (var s : Spec.MODESITES) if (s.mode().equals("survival") && s.kind().equals("rise")) sites.add(s.name());
        List<String[]> rows = new ArrayList<>();
        row(rows, ModeMath.SHRINE_LINE, SHRINE);
        row(rows, "The shrine stands until dawn", STANDS);
        row(rows, "The dead hold the shrine", HOLD);
        row(rows, ModeMath.riseLine(List.of()), rise(List.of()).toArray(new String[0]));
        for (int wave = 1; wave <= Spec.WAVES.length; wave++) {
            row(rows, ModeMath.restLine(wave, Spec.WAVES[wave - 1].gold()), rest(wave, Spec.WAVES[wave - 1].gold()));
            if (Spec.WAVES[wave - 1].gold() > 0) row(rows, ModeMath.restLine(wave, 0), rest(wave, 0));
        }
        for (String a : sites) {
            row(rows, ModeMath.riseLine(List.of(a)), rise(List.of(a)).toArray(new String[0]));
            for (String b : sites) if (!a.equals(b)) {
                row(rows, ModeMath.riseLine(List.of(a, b)), rise(List.of(a, b)).toArray(new String[0]));
                for (String c : sites) if (!c.equals(a) && !c.equals(b)) row(rows, ModeMath.riseLine(List.of(a, b, c)), rise(List.of(a, b, c)).toArray(new String[0]));
            }
        }
        boolean table = Boolean.getBoolean("short.table");
        int widest = 0;
        for (String[] r : rows) {
            int px = 0;
            for (String line : r[1].split(" / ")) {
                px = Math.max(px, FeedFitCheck.width(line));
                checks++;
                if (FeedFitCheck.width(line) > FEED_PX || line.length() > FeedLines.MAX_TEXT) { failed++; System.out.println("FAILED: \"" + line + "\" is " + FeedFitCheck.width(line) + " px, the Compact feed row holds " + FEED_PX); }
            }
            widest = Math.max(widest, px);
            for (String s : sites) if (r[0].contains(s) && !r[1].contains(s)) { failed++; System.out.println("FAILED: site lost in \"" + r[1] + "\""); }
            if (table) System.out.println("| " + r[0] + " | " + FeedFitCheck.width(r[0]) + " | " + r[1] + " | " + px + (r[2].equals("1") ? "" : " (widest of " + r[2] + ")") + " |");
        }
        System.out.println("survival-feed-short: " + checks + " lines checked" + (failed == 0 ? ", all fit" : ", " + failed + " failed") + "; widest " + widest + " of " + FEED_PX + " px");
        if (failed > 0) System.exit(1);
    }
}
