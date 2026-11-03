package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/** Copy check: the approved shorter Survival feed lines (docs/drafts/survival-feed-short-2026-11-02.md) each fit the Compact
 *  feed row, measured with FeedFitCheck.width like ModeHudFitCheck. Every site name is kept. Prints the draft table with -Dshort.table=true.
 *  Approved by Kort 2026-11-02. Checked by javac alone, never shipped. */
public final class SurvivalFeedShortCheck {
    static int checks, failed;
    static final int FEED_PX = UiMath.feedTextW(UiMath.HUD_COMPACT);

    /** The lines before the 2026-11-02 shortening, kept only as the "current" column of the printed table. */
    static String oldRise(List<String> sites) {
        if (sites.isEmpty()) return "The dead rise";
        if (sites.size() == 1) return "The dead rise at " + sites.get(0);
        return "The dead rise at " + String.join(", ", sites.subList(0, sites.size() - 1)) + " and " + sites.get(sites.size() - 1);
    }
    static String oldRest(int wave, int gold) { return "Wave " + wave + " laid to rest" + (gold > 0 ? ", +" + gold + " gold" : ""); }
    static List<String> rise(List<String> sites) { return ModeMath.riseLines(sites); }
    static String rest(int wave, int gold) { return ModeMath.restLine(wave, gold); }

    static void row(List<String[]> rows, String cur, String... prop) { rows.add(new String[]{cur, String.join(" / ", prop), String.valueOf(prop.length)}); }

    public static void main(String[] args) {
        List<String> sites = new ArrayList<>();
        for (var s : Spec.MODESITES) if (s.mode().equals("survival") && s.kind().equals("rise")) sites.add(s.name());
        List<String[]> rows = new ArrayList<>();
        row(rows, "The dead are taking the shrine", ModeMath.shrineLine());
        row(rows, "The shrine stands until dawn", ModeMath.closingLine(true));
        row(rows, "The dead hold the shrine", ModeMath.closingLine(false));
        row(rows, oldRise(List.of()), rise(List.of()).toArray(new String[0]));
        for (int wave = 1; wave <= Spec.WAVES.length; wave++) {
            row(rows, oldRest(wave, Spec.WAVES[wave - 1].gold()), rest(wave, Spec.WAVES[wave - 1].gold()));
            if (Spec.WAVES[wave - 1].gold() > 0) row(rows, oldRest(wave, 0), rest(wave, 0));
        }
        for (String a : sites) {
            row(rows, oldRise(List.of(a)), rise(List.of(a)).toArray(new String[0]));
            for (String b : sites) if (!a.equals(b)) {
                row(rows, oldRise(List.of(a, b)), rise(List.of(a, b)).toArray(new String[0]));
                for (String c : sites) if (!c.equals(a) && !c.equals(b)) row(rows, oldRise(List.of(a, b, c)), rise(List.of(a, b, c)).toArray(new String[0]));
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
