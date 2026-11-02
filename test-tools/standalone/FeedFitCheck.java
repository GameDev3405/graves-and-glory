package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/** p4-feed-fit (plan 2C.2): the draft copy of the battle modes fits where it is drawn. Every modeLines row, with the longest team,
 *  point and stop name of the sheets in its fields, fits one row of the event feed leaf at Compact (UiMath.feedTextW, the narrowest)
 *  in the game's own font widths, and is under FeedLines.MAX_TEXT; every mode has a 2 to 4 number ledger with short labels; each
 *  briefing how-to line is short; and all of it is still marked draft until Kort approves it. Checked by javac alone, never shipped. */
public final class FeedFitCheck {
    static int checks, failed;
    static void check(boolean pass, String message) {
        if (!pass && !ALL) throw new AssertionError(message);
        if (!pass) { failed++; System.out.println("FAILED: " + message); return; }
        checks++;
    }
    /** -Dfeedfit.all=true lists every failing check instead of stopping at the first (for the red run of a deliberate break). */
    static final boolean ALL = Boolean.getBoolean("feedfit.all");

    /** A string's width in GUI pixels in the game's default font: 6 for most letters (5 wide and a pixel between), narrower for the
     *  thin ones, 7 for @ and ~. Taken from the font's glyph widths; a wider real width would show as a "..." in the feed. */
    static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) w += switch (c) {
            case 'i', '!', '.', ',', ':', ';', '|' -> 2;
            case 'l', '\'', '`' -> 3;
            case 'I', 't', '[', ']', ' ' -> 4;
            case 'f', 'k', '(', ')', '<', '>', '{', '}', '*', '"' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
        return w;
    }

    /** Briefing lines and the objective panel's start line are not feed rows; these are their draft ceilings in characters. */
    static final int HOWTO_CHARS = 70, START_CHARS = 60, LABEL_CHARS = 16, FEED_PX = UiMath.feedTextW(UiMath.HUD_COMPACT);

    public static void main(String[] args) {
        check(width("i") == 2 && width("l") == 3 && width("W") == 6 && width(" ") == 4 && width("it") == 6, "font widths: thin letters narrower, space 4");
        List<String> teams = new ArrayList<>(), points = new ArrayList<>(), stops = new ArrayList<>();
        for (int t = 0; t < 2; t++) teams.add(FeedLines.teamShort(Spec.FACTIONS[t].name())); // a battle has two sides, FACTIONS[0] and [1]
        for (var p : Spec.POINTS) points.add(p.name());
        for (var s : Spec.MODESITES) if (s.kind().equals("road") && !s.name().equals("-")) stops.add(s.name());
        check(!teams.isEmpty() && !points.isEmpty() && !stops.isEmpty(), "the sheets name teams, points and stops to fill the fields with");
        int tightest = FEED_PX, rows = 0;
        String tightestId = "";
        for (var r : Spec.MODELINES) {
            rows++;
            List<String> fills = new ArrayList<>(List.of(r.text()));
            for (String field : r.args().equals("-") ? new String[0] : r.args().split(",")) {
                List<String> next = new ArrayList<>();
                List<String> values = switch (field) { case "team" -> teams; case "point" -> points; case "stop" -> stops; default -> List.of("10", "99"); };
                for (String text : fills) for (String v : values) next.add(text.replace("{" + field + "}", v));
                fills = next;
            }
            for (String text : fills) {
                check(text.indexOf('{') < 0 && text.indexOf('}') < 0, r.id() + ": a field is left unfilled in \"" + text + "\"");
                check(text.length() <= FeedLines.MAX_TEXT, r.id() + ": \"" + text + "\" is over " + FeedLines.MAX_TEXT + " characters");
                if (r.event().equals("start")) { check(text.length() <= START_CHARS, r.id() + ": the start line \"" + text + "\" is over " + START_CHARS + " characters"); continue; }
                int w = width(text);
                check(w <= FEED_PX, r.id() + ": \"" + text + "\" is " + w + " px, the Compact feed row holds " + FEED_PX);
                check(UiMath.trimWords(text, FEED_PX, FeedFitCheck::width).equals(text), r.id() + ": \"" + text + "\" would be trimmed on the feed");
                if (FEED_PX - w < tightest) { tightest = FEED_PX - w; tightestId = r.id(); }
            }
            check(List.of("point", "notice").contains(r.kind()), r.id() + ": kind " + r.kind() + " is not one the mode feed draws (point or notice)");
            check(FeedLines.style(r.kind(), 0, 1).glyph().startsWith(r.kind().equals("point") ? "hud/fgv-seal-x-" : "hud/fgv-"), r.id() + ": kind " + r.kind() + " has the wrong glyph");
            check(r.status().equals("draft") || r.status().equals("approved"), r.id() + ": status " + r.status());
        }
        check(rows > 0, "there are mode lines");

        for (String mode : new String[]{"domination", "siege", "convoy"}) {
            check(!FeedLines.modeRows(mode).isEmpty(), mode + " has feed lines");
            var ledger = ModeLines.rows(mode);
            check(ledger.size() >= 2 && ledger.size() <= 4, mode + " has a 2 to 4 number ledger, not " + ledger.size());
            for (int i = 0; i < ledger.size(); i++) {
                var l = ledger.get(i);
                check(l.slot() == i && !l.label().isBlank() && l.label().length() <= LABEL_CHARS, l.id() + ": slot " + l.slot() + " (want " + i + "), label \"" + l.label() + "\" of at most " + LABEL_CHARS);
                check(l.status().equals("draft") || l.status().equals("approved"), l.id() + ": status " + l.status());
            }
            for (String side : new String[]{"attack", "defend"}) {
                var how = ModeLines.briefing(mode, side);
                check(how.size() >= 1 && how.size() <= 3, mode + "/" + side + " has " + how.size() + " how-to lines");
                for (String line : how) check(line.length() <= HOWTO_CHARS, mode + "/" + side + ": \"" + line + "\" is " + line.length() + " characters, the ceiling is " + HOWTO_CHARS);
                check(ModeLines.briefing(mode, side, id -> "K").equals(how) || how.stream().anyMatch(line -> line.contains("{key:")), mode + "/" + side + ": the keyed form reads as the raw one when no key is named");
            }
        }

        // The ledger as data: a value in its row's format, the team it counts for, and a time as minutes and seconds.
        var stage = ModeLines.entry("siege_stage", 1, 3);
        check(stage.text().equals("1/3") && stage.label().equals("Stages taken") && stage.team() == -1 && stage.id().equals("siege_stage"), "a fraction entry");
        check(ModeLines.entry("siege_strength", 64, 100).text().equals("64%") && ModeLines.entry("siege_strength", 64, 100).team() == 1, "a percent entry for the defence");
        check(ModeLines.entry("siege_time", 65, 600).text().equals("1:05") && ModeLines.entry("convoy_time", 600, 0).text().equals("10:00") && ModeLines.entry("convoy_time", 9, 0).text().equals("0:09") && ModeLines.entry("convoy_time", -4, 0).text().equals("0:00"), "a time entry in minutes and seconds");
        check(ModeLines.entry("convoy_stops", 2, 3).text().equals("2"), "a plain entry");
        check(ModeLines.entry("domination_score_1", 120, 300).text().equals("120/300") && ModeLines.entry("domination_score_1", 120, 300).team() == 1, "Domination's score entry");

        // The extension point Survival uses: its own rows, no change here. Until it has them it has none, and a call for one fails loudly.
        check(FeedLines.modeRows("survival").isEmpty() && ModeLines.rows("survival").isEmpty(), "a mode without rows has no lines and no ledger");
        boolean threw = false;
        try { FeedLines.modeLine("survival", "wave"); } catch (IllegalArgumentException e) { threw = e.getMessage().contains("survival/wave"); }
        check(threw, "an event with no row is a programming error that names it");
        threw = false;
        try { FeedLines.modeLine("domination", "point_taken", "team", "Gold"); } catch (IllegalArgumentException e) { threw = e.getMessage().contains("unfilled"); }
        check(threw, "a field the call leaves out is a programming error that names the line");
        check(FeedLines.modeLine("convoy", "checkpoint", "team", "Gold", "stop", "Pass Crossing").equals("Gold at Pass Crossing") && FeedLines.pointTaken("Dawn", "Mill Hamlet").equals("Dawn took Mill Hamlet") && FeedLines.modeKind("siege", "gate_breached").equals("point"), "lines fill their fields");

        System.out.println("p4-feed-fit: " + checks + " checks passed" + (failed == 0 ? "" : ", " + failed + " failed") + "; " + rows + " mode lines, the tightest " + tightestId + " with " + tightest + " of " + FEED_PX + " px to spare");
        if (failed > 0) System.exit(1);
    }
}
