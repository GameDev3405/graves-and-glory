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

    /** Whether a look gets a vanilla interaction entity as its click box (hitWidth and hitHeight above 0). */
    public static boolean hitBox(Spec.CampNpcsRow r) { return r.hitWidth() > 0 && r.hitHeight() > 0; }
}
