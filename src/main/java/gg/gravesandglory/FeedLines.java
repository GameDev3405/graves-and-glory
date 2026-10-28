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

    public static String pointTaken(String team, String point) { return team + " took " + point; }

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
