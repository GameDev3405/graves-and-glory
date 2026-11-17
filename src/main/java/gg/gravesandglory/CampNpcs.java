package gg.gravesandglory;

import java.util.HashMap;
import java.util.Map;

/**
 * Which camp NPC a villager is (lobby NPC spec 3.1, Q3): the server spawns a plain villager per campStations row with an npc, named by
 * the row's label before " / " and with its AI off; a modded client draws the campNpcs row of the row's look instead, matched by that
 * name, since the name is the one thing vanilla syncs (command tags never reach a client). Every other villager (a village's, a
 * campaign map's, one a player named with a tag) stays vanilla: it has its AI on, or a name no look row carries. Pure (Spec only), so
 * the standalone CampNpcsCheck runs it. A plain client never reads this and sees the villager.
 */
public final class CampNpcs {
    private CampNpcs() {}

    /** Villager name to the campNpcs row drawn for it: one entry per campStations row with an npc and a look (preflight keeps the names unique). */
    private static final Map<String, Spec.CampNpcsRow> BY_NAME = new HashMap<>();
    static {
        for (var s : Spec.CAMPSTATIONS) if (!s.npc().equals("-") && !s.look().equals("-")) BY_NAME.put(name(s), look(s.look()));
    }

    /** The name a station's villager carries (CampWorld.npc sets it): the row's label before " / ". */
    public static String name(Spec.CampStationsRow s) { return s.label().split(" / ")[0]; }

    /** The campNpcs row of that id, or null. */
    public static Spec.CampNpcsRow look(String id) {
        for (var r : Spec.CAMPNPCS) if (r.id().equals(id)) return r;
        return null;
    }

    /** The look of a station row (null for a row with no npc or look -). */
    public static Spec.CampNpcsRow look(Spec.CampStationsRow s) { return s.npc().equals("-") || s.look().equals("-") ? null : look(s.look()); }

    /**
     * The look a villager with that custom name (null for none) draws as, or null for vanilla. Only a villager with its AI off is a camp
     * NPC (CampWorld.npc sets NoAI, and the flag is synced), so a village villager renamed to a camp name keeps the vanilla look.
     */
    public static Spec.CampNpcsRow resolve(String customName, boolean aiDisabled) {
        if (customName == null || !aiDisabled) return null;
        return BY_NAME.get(customName);
    }

    /** The bark lines of a look (its lines column, split on " | "). */
    public static String[] lines(Spec.CampNpcsRow r) { return r.lines().split(" \\| "); }

    /**
     * The chat line a plain player gets for a click on a staff NPC whose station has no plain menu (lobby NPC spec 3.4): the character's name
     * and that the station needs the modpack (VersionGate.refusal appends the download link). The page's villager row names the character
     * through its look; a page with no look falls back to the villager's name, and a page with no villager to a bare line.
     */
    public static String plainLine(String page) {
        for (var s : Spec.CAMPSTATIONS) {
            if (!s.page().equals(page) || s.npc().equals("-")) continue;
            var l = look(s);
            String who = l != null ? l.name() : name(s);
            return Character.toUpperCase(who.charAt(0)) + who.substring(1) + ": this station needs the Graves and Glory modpack.";
        }
        return "This station needs the Graves and Glory modpack.";
    }

    /** Whether a campStations row of that page has a villager (a staff NPC's station, which gets plainLine). */
    public static boolean hasNpc(String page) {
        for (var s : Spec.CAMPSTATIONS) if (s.page().equals(page) && !s.npc().equals("-")) return true;
        return false;
    }

    /** Madame Fleece's page (Kort 2026-11-16): no screen yet, so a modded click gets wardrobeSoon() and a plain one plainLine plus storeLine. */
    public static final String WARDROBE = "wardrobe";
    /** The modded click on Madame Fleece until the Wardrobe screen is built (it waits for cosmetics to render). */
    public static String wardrobeSoon() { return plainLine(WARDROBE).replaceFirst(":.*$", "") + ": the Wardrobe opens soon."; }
    /** Madame Fleece's second plain-client line: the store, where cosmetics are bought without the mod. No URL yet (VersionGate.STORE_URL, a TODO): then it says the link is coming. */
    public static String storeLine(String url) { return "Cosmetics are sold in the Graves and Glory store and show with the modpack. Store: " + (url == null || url.isBlank() ? "link coming soon." : url); }

    /** Whether a look gets a vanilla interaction entity as its click box (hitWidth and hitHeight above 0). */
    public static boolean hitBox(Spec.CampNpcsRow r) { return r.hitWidth() > 0 && r.hitHeight() > 0; }
}
