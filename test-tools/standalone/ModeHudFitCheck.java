package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/** p4-mode-hud-fit (plan 4C.5): where FeedFitCheck stops. For every battle mode, the worst-case text of the score bar (the 256 px
 *  card), Convoy's road line, the report note's mode section (title, rule lines, rows; 196 px) and Survival's feed lines (code
 *  strings in ModeMath, not modeLines rows) fits where it is drawn. Widths are the default font's (FeedFitCheck.width). The report
 *  rows repeat ReportSections' labels and formats because that class cannot compile without the game; the real layout is checked in
 *  game by the ui-report and mode suites. A Survival line wider than the Compact feed row is listed as a gap and fails only with
 *  -Dmodehud.strict=true. Checked by javac alone, never shipped. */
public final class ModeHudFitCheck {
    static int checks, failed;
    static final boolean STRICT = Boolean.getBoolean("modehud.strict");
    static final int CARD_W = 256, TRACKS = 2 * 56 + 4 * 6, NOTE_W = 196, VALUE_GAP = 8, RULE_LINES = 2;
    static final int FEED_PX = UiMath.feedTextW(UiMath.HUD_COMPACT);
    static final java.util.TreeMap<String, String> gaps = new java.util.TreeMap<>();
    static int spare = Integer.MAX_VALUE;
    static String spareId = "";

    static void check(boolean pass, String message) {
        if (pass) { checks++; return; }
        failed++;
        System.out.println("FAILED: " + message);
    }
    static int w(String s) { return FeedFitCheck.width(s); }
    static void room(String id, int limit, int used) {
        check(used <= limit, id + " is " + used + " px, room " + limit);
        if (limit - used < spare) { spare = limit - used; spareId = id; }
    }
    /** Lines the text takes when wrapped to px at word breaks. */
    static int lines(String text, int px) {
        int n = 1, x = 0;
        for (String word : text.split(" ")) {
            int ww = w(word);
            if (x > 0 && x + w(" ") + ww > px) { n++; x = ww; } else x += (x > 0 ? w(" ") : 0) + ww;
        }
        return n;
    }
    /** The widest a ledger number can read in its format. */
    static String worst(String format) {
        return switch (format) {
            case "fraction" -> ModeMath.ledgerText(format, 99, 99);
            case "percent" -> ModeMath.ledgerText(format, 100, 100);
            case "time" -> ModeMath.ledgerText(format, 5999, 0);
            default -> ModeMath.ledgerText(format, 999, 0);
        };
    }

    public static void main(String[] args) {
        // ---- the score bar: Dawn's text, the clock, Dusk's text between two 56 px tracks (BattleHud.ledger) ----
        for (var m : Spec.MODES) {
            var ledger = ModeLines.rows(m.id());
            String a, d;
            if (ledger.size() >= 2) { a = worst(ledger.get(0).format()); d = worst(ledger.get(1).format()); }
            else if (m.id().equals("survival")) { a = worst("fraction"); d = worst("plain"); } // SurvivalMode: wave out of waves, the dead standing
            else { check(false, m.id() + " has no two ledger rows and is not Survival"); continue; }
            room("score bar " + m.id(), CARD_W, w(a) + 1 + w("10:00") + 1 + w(d) + 1 + TRACKS);
        }

        // ---- Convoy's road line: the next stop and the wagon's state, under the card ----
        for (var s : Spec.MODESITES) {
            if (!(s.mode().equals("convoy") && s.kind().equals("road")) || s.name().equals("-")) continue;
            for (String state : new String[]{"rolling", "stopped", "rolling back", "waiting"}) room("road line " + s.name() + " - " + state, CARD_W, w(s.name() + " - " + state));
        }

        // ---- the report note: title, rule lines and rows (WarTableReport; ReportSections) ----
        for (var m : Spec.MODES) {
            room("report title " + m.id(), NOTE_W, w(m.name()));
            String rule = ModeMath.ruleLine(m.ruleLine(), Math.max(m.winTarget(), 300));
            check(lines(rule, NOTE_W) <= RULE_LINES, "report rule \"" + rule + "\" takes " + lines(rule, NOTE_W) + " lines, the note has " + RULE_LINES);
        }
        room("report row Dawn score", NOTE_W, w("Dawn score") + VALUE_GAP + w("300 of 300"));
        room("report row Dusk score", NOTE_W, w("Dusk score") + VALUE_GAP + w("300 of 300"));
        for (var p : Spec.POINTS) room("report row " + p.name(), NOTE_W, w(p.name()) + VALUE_GAP + w("999s / 999s"));
        room("report row Stages taken", NOTE_W, w("Stages taken") + VALUE_GAP + w("3/3"));
        room("report row Stage strength", NOTE_W, w("Stage strength") + VALUE_GAP + w("100%"));
        room("report row Escort progress", NOTE_W, w("Escort progress") + VALUE_GAP + w("100%"));
        room("report row Stops held", NOTE_W, w("Stops held") + VALUE_GAP + w("9 of 9"));
        room("report row Dawn (generic)", NOTE_W, w("Dawn") + VALUE_GAP + w("99/99"));
        room("report row Dusk (generic)", NOTE_W, w("Dusk") + VALUE_GAP + w("999"));

        // ---- Survival's feed lines: code strings (SurvivalMode, ModeMath), so FeedFitCheck's modeLines rows do not reach them ----
        List<String> sites = new ArrayList<>();
        for (var s : Spec.MODESITES) if (s.mode().equals("survival") && s.kind().equals("rise")) sites.add(s.name());
        List<String> lines = new ArrayList<>(List.of(ModeMath.SHRINE_LINE, ModeMath.STANDS_LINE, ModeMath.HOLD_LINE));
        lines.addAll(ModeMath.riseLines(List.of()));
        for (int wave = 1; wave <= Spec.WAVES.length; wave++) { lines.add(ModeMath.restLine(wave, Spec.WAVES[wave - 1].gold())); lines.add(ModeMath.restLine(wave, 0)); }
        for (String s1 : sites) {
            lines.addAll(ModeMath.riseLines(List.of(s1)));
            for (String s2 : sites) if (!s1.equals(s2)) {
                lines.addAll(ModeMath.riseLines(List.of(s1, s2)));
                for (String s3 : sites) if (!s3.equals(s1) && !s3.equals(s2)) lines.addAll(ModeMath.riseLines(List.of(s1, s2, s3)));
            }
        }
        int widest = 0, over = 0;
        String widestLine = "";
        for (String line : lines) {
            check(line.length() <= FeedLines.MAX_TEXT, "survival line \"" + line + "\" is over " + FeedLines.MAX_TEXT + " characters");
            int px = w(line);
            if (px > widest) { widest = px; widestLine = line; }
            if (px <= FEED_PX) continue;
            over++;
            if (STRICT) check(false, "survival feed line \"" + line + "\" is " + px + " px, the Compact feed row holds " + FEED_PX);
            String kind = line.startsWith("The dead rise at") ? "rise line of " + (line.split(", ").length + (line.contains(" and ") ? 1 : 0)) + " site(s)" : "fixed or rest line";
            gaps.merge(kind, line + " (" + px + " px, drawn \"" + UiMath.trimWords(line, FEED_PX, FeedFitCheck::width) + "\")", (x, y) -> w(x) >= w(y) ? x : y);
        }
        check(!sites.isEmpty() && lines.size() > 10, "survival rise sites and waves are in the sheets");

        System.out.println("p4-mode-hud-fit: " + checks + " checks passed" + (failed == 0 ? "" : ", " + failed + " failed") + "; tightest fixed text " + spareId + " with " + spare
                + " px to spare; widest survival feed line " + widest + " of " + FEED_PX + " px (\"" + widestLine + "\")");
        gaps.forEach((k, v) -> System.out.println("GAP " + k + ", widest: " + v));
        if (over > 0) System.out.println("gaps: " + over + " of " + lines.size() + " Survival feed lines are wider than the Compact feed row and are drawn cut with \"...\" (not failing; -Dmodehud.strict=true fails them)");
        if (failed > 0) System.exit(1);
    }
}
