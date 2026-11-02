package gg.gravesandglory;

/** The battle event feed's copy and look (UI spec 4.5, v4.1): what each line says, and its glyph and ink. Pure, so the standalone
 *  check runs it; FeedEvents writes the copy on the server, BattleHud draws the style on the client. */
public final class FeedLines {
    private FeedLines() {}

    /** A line's glyph (an 8x8 atlas name) and the dark ink of its hue. */
    public record Style(String glyph, int argb) {}

    /** The inks on the feed's vellum: the team inks Dawn #1b4a47 and Dusk #52376d, losses red, kills and last stands grey, commanders
     *  brass, deploys and notices sepia. Each holds 5.2:1 or better on the slip, the spec's bar for the feed's small text, on its
     *  darker vellum (#d0bd90) as on its body, and 4.5:1 on the next tone down (#c2ae80), the worst the Compact leaf's text rows
     *  show at the 90th percentile (UiMathCheck). UI Task 8c fix round 1 took Dawn, Dusk, grey and brass one step darker in
     *  their own hues for that (from #1d4f4c, #563a73, #504b44 and #684600: 5.0, 5.05, 4.67 and 4.61:1 on #d0bd90). */
    public static final int DAWN_INK = 0xff1b4a47, DUSK_INK = 0xff52376d, LOSS_INK = 0xff7a2416, GREY_INK = 0xff47433d,
        BRASS_INK = 0xff5c3e00, NOTICE_INK = 0xff3d3124;

    /** A troop name in the plural: +"s", a trailing "man" becomes "men", and a name ending in "s" or the Risen stays as it is. */
    public static String plural(String name) {
        if (name.endsWith("s") || name.equals("Risen")) return name;
        if (name.endsWith("man")) return name.substring(0, name.length() - 3) + "men";
        return name + "s";
    }

    /** The longest text a feed line carries (FeedEvents.send cuts a longer one). */
    public static final int MAX_TEXT = 120;

    /** A battle mode's feed line (plan 2C.2): the modeLines row of this mode and event, each {name} field filled from `fields`
     *  (name, value, name, value...). The copy is draft, in the sheet, not in the mode; Survival adds its rows and calls this the same
     *  way. A row that is missing, or a field the call leaves out, is a programming error and the exception names it. */
    public static String modeLine(String mode, String event, String... fields) {
        String text = modeRow(mode, event).text();
        for (int i = 0; i + 1 < fields.length; i += 2) text = text.replace("{" + fields[i] + "}", fields[i + 1]);
        if (text.indexOf('{') >= 0) throw new IllegalArgumentException("modeLines " + mode + "/" + event + " has an unfilled field: " + text);
        return text;
    }

    /** The kind (point, notice...) the modeLines row of this mode and event sends its line as; it picks the glyph and ink (style). */
    public static String modeKind(String mode, String event) { return modeRow(mode, event).kind(); }

    /** The modeLines rows of a mode, in sheet order: the events its feed can say. */
    public static java.util.List<Spec.ModeLinesRow> modeRows(String mode) {
        var out = new java.util.ArrayList<Spec.ModeLinesRow>();
        for (var r : Spec.MODELINES) if (r.mode().equals(mode)) out.add(r);
        return out;
    }

    static Spec.ModeLinesRow modeRow(String mode, String event) {
        for (var r : Spec.MODELINES) if (r.mode().equals(mode) && r.event().equals(event)) return r;
        throw new IllegalArgumentException("No modeLines row for " + mode + "/" + event);
    }

    /** "Dawn took Mill Hamlet": Domination's point line, the modeLines row domination/point_taken. */
    public static String pointTaken(String team, String point) { return modeLine("domination", "point_taken", "team", team, "point", point); }

    /** "Dawn Knight lost" for one, "3 Dawn Knights lost" for more. */
    public static String lost(int n, String troop) { return n == 1 ? troop + " lost" : n + " " + plural(troop) + " lost"; }

    public static String bannerDown(String name) { return name + "'s banner has fallen"; }

    public static String bannerUp(String name) { return name + " is back in the field"; }

    /** "Infantry make a last stand": a group of the owner's army turned to its last stand (Morale's throttled line). */
    public static String lastStand(String group) { return group + " make a last stand"; }

    /** "Dawn Knight deploying" for one, "Dawn Knights deploying" for more. */
    public static String deploying(int n, String troop) { return (n == 1 ? troop : plural(troop)) + " deploying"; }

    /** A faction's short name, its first word: "Dusk" for the Dusk Legion. */
    public static String teamShort(String factionName) {
        int space = factionName.indexOf(' ');
        return space < 0 ? factionName : factionName.substring(0, space);
    }

    /** The glyph and ink of a line of `kind` about eventTeam, as viewerTeam sees it. A point taken shows the taker's seal, struck
     *  through when the taker is not the viewer's team (ours versus theirs by shape, spec 6.1), in the taker's ink. Deploys,
     *  notices and any kind not listed carry the viewer's own emblem. */
    public static Style style(String kind, int eventTeam, int viewerTeam) {
        return switch (kind) {
            case "point" -> new Style((eventTeam == viewerTeam ? "hud/fgv-seal-" : "hud/fgv-seal-x-") + side(eventTeam), eventTeam == 1 ? DUSK_INK : DAWN_INK);
            case "loss" -> new Style("hud/fgv-skull", LOSS_INK);
            case "kill" -> new Style("hud/fgv-skull-grey", GREY_INK);
            case "down" -> new Style("hud/fgv-banner-down", BRASS_INK);
            case "up" -> new Style("hud/fgv-banner-up", BRASS_INK);
            case "laststand" -> new Style("hud/fgv-laststand", GREY_INK);
            default -> new Style(viewerTeam == 1 ? "hud/fgv-moon" : "hud/fgv-sun", NOTICE_INK);
        };
    }

    static String side(int team) { return team == 1 ? "dusk" : "dawn"; }
}
