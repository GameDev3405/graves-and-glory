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
    static final int HOWTO_CHARS = 70, START_CHARS = 60, LABEL_CHARS = 16, FEED_PX = UiMath.feedTextW(UiMath.HUD_COMPACT), INTRO_PX = 280;

    public static void main(String[] args) {
        check(width("i") == 2 && width("l") == 3 && width("W") == 6 && width(" ") == 4 && width("it") == 6, "font widths: thin letters narrower, space 4");
        List<String> teams = new ArrayList<>(), points = new ArrayList<>(), stops = new ArrayList<>(), sites = new ArrayList<>();
        for (int t = 0; t < 2; t++) teams.add(FeedLines.teamShort(Spec.FACTIONS[t].name())); // a battle has two sides, FACTIONS[0] and [1]
        for (var p : Spec.POINTS) points.add(p.name());
        for (var s : Spec.MODESITES) if (s.kind().equals("road") && !s.name().equals("-")) stops.add(s.name());
        for (var s : Spec.MODESITES) if (s.kind().equals("rise") && !s.name().equals("-")) sites.add(s.name());
        check(!teams.isEmpty() && !points.isEmpty() && !stops.isEmpty() && !sites.isEmpty(), "the sheets name teams, points, stops and rise sites to fill the fields with");
        // Siege's yard, gate and keep names as each map's feed lines carry them (ModeMath.siegeSiteNames), and the unnamed fallback.
        List<String[]> siegeNames = new ArrayList<>();
        siegeNames.add(new String[]{"the Outer Yard", "The Iron Gate", "the Keep"});
        for (int map = 0; map < Spec.MAPS.length; map++) if (ModeMath.siegeMap(map) == map) siegeNames.add(ModeMath.siegeSiteNames(map));
        check(siegeNames.size() >= 6, "Siege's feed lines have the names of every Siege map to be measured with: " + siegeNames.size());
        int tightest = FEED_PX, rows = 0;
        String tightestId = "";
        for (var r : Spec.MODELINES) {
            rows++;
            List<String> fills = new ArrayList<>(List.of(r.text()));
            // the unnamed gate line is measured with the old gate name only, the named one with every map's own
            List<String[]> rowNames = r.event().equals("gate_breached") ? siegeNames.subList(0, 1) : r.event().equals("gate_breached_named") ? siegeNames.subList(1, siegeNames.size()) : siegeNames;
            if (r.mode().equals("siege") && r.event().startsWith("sides")) { // the sides line: every pair of banner colours (any two of the factions can meet), in the form ModeMath.siegeSidesEvents sends that pair in
                fills = new ArrayList<>();
                for (var a : Spec.FACTIONS) for (var d : Spec.FACTIONS) {
                    String att = FeedLines.teamShort(a.name()), def = FeedLines.teamShort(d.name());
                    if (a == d || !List.of(ModeMath.siegeSidesEvents(att, def)).contains(r.event())) continue;
                    fills.add(r.text().replace("{team}", att).replace("{defenders}", def));
                }
                check(!fills.isEmpty() || !r.event().equals("sides"), r.id() + ": some pair of banners goes out as this row");
            }
            else for (String field : r.args().equals("-") ? new String[0] : r.args().split(",")) {
                List<String> next = new ArrayList<>();
                if (field.equals("yard") || field.equals("gate") || field.equals("keep")) { // a Siege map's own site names: each map fills its three together, so a line is measured with names that really go side by side
                    int slot = field.equals("yard") ? 0 : field.equals("gate") ? 1 : 2;
                    for (String text : fills) for (String[] names : rowNames) next.add(text.replace("{" + field + "}", names[slot]));
                    fills = next;
                    continue;
                }
                List<String> values = switch (field) { case "team" -> teams; case "point" -> points; case "stop" -> stops; case "site" -> sites; case "gold" -> List.of("100"); case "defenders" -> List.of("Dawn", "Dusk"); default -> List.of("10", "99"); };
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
        // The Siege round intro's lines (CampaignClient.hud) are centred, not wrapped: they must fit the smallest GUI the game opens at (320 px), with a margin, for every pair of banner colours and both viewers.
        int introWidest = 0;
        for (int map = 0; map < Spec.MAPS.length; map++) for (var a : Spec.FACTIONS) for (var d : Spec.FACTIONS) {
            if (a == d) continue;
            for (int viewer = -1; viewer < 2; viewer++) for (String line : ModeMath.siegeIntro(map, viewer, FeedLines.teamShort(a.name()), FeedLines.teamShort(d.name()))) introWidest = Math.max(introWidest, width(line));
        }
        check(introWidest > 0 && introWidest <= INTRO_PX, "the Siege round intro's widest line is " + introWidest + " px, the budget is " + INTRO_PX);
        check(ModeMath.siegeSides("Gold", "Violet").equals(List.of("Gold attacks, Violet defends")) && ModeMath.siegeSides("Crimson", "Violet").equals(List.of("Crimson attacks", "Violet defends")), "the sides announcement is one row when the names fit it, two when they do not");

        for (String mode : new String[]{"domination", "siege", "convoy", "survival"}) {
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

        // Survival's own rows are the extension point's first user: its ledger and its lines come from the sheets like every mode's.
        check(FeedLines.modeRows("survival").size() == 8 && ModeLines.rows("survival").size() == 3, "Survival has its feed lines and a three number ledger");
        check(ModeLines.entry("survival_wave", 3, 7).text().equals("3/7") && ModeLines.entry("survival_dead", 12, 40).text().equals("12") && ModeLines.entry("survival_time", 95, 0).text().equals("1:35"), "Survival's ledger entries");
        check(FeedLines.modeLine("survival", "rise", "site", "the ford").equals("Dead rise: the ford") && FeedLines.modeLine("survival", "rest", "n", "5", "gold", "100").equals("Wave 5 at rest, +100 gold") && FeedLines.modeKind("survival", "start").equals("notice"), "Survival's lines fill their fields");
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
