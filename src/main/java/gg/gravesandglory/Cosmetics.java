package gg.gravesandglory;

import java.util.*;

/**
 * The cosmetics catalogue (plan 2026-11-16-cosmetics-render, section 2): each owned cosmetic's slot, name, the reward that grants it, the
 * vanilla banner layers every client sees and nothing else. Pure, so the standalone checks run it (CosmeticsCheck). It lives in code, not
 * sheets/design.json, so Spec.java does not churn; the reward strings stay in the sheet and map to an id here (byReward).
 *
 * Slots hold one item each: pennant, trim, frame, standard and skin. Titles keep their own slot (Progression.State.title). Ids are global,
 * never per House. Vanilla layers are drawn field first (the side's dye, the banner's own colour), then standard, frame, trim and pennant,
 * at most MAX_LAYERS, and every pattern id is a minecraft: one, so a plain client never meets a mod id.
 */
public final class Cosmetics {
    private Cosmetics() {}

    /** The slots, in the order a profile's slot map is kept. */
    public static final List<String> SLOTS = List.of("pennant", "trim", "frame", "standard", "skin");
    /** The vanilla layer order on a banner: standard first (lowest), pennant last (on top). */
    static final List<String> LAYER_ORDER = List.of("standard", "frame", "trim", "pennant");
    /** What an equip names to empty a slot. */
    public static final String NONE = "none";
    /** Vanilla draws at most six pattern layers on a banner; more are dropped, so the catalogue never asks for more. */
    public static final int MAX_LAYERS = 6;
    /** The default title every profile owns: it is no title on the wire (CosmeticSync sends ""), so nobody wears a "Recruit" line. */
    public static final String DEFAULT_TITLE = "Recruit";

    /** One vanilla pattern layer: a minecraft: pattern id and a dye name ("" is the contrast dye: white, or black on a white field). */
    public record Layer(String pattern, String dye) {}
    /** One catalogue entry: id, slot, shown name, the reward string that grants it, and its vanilla layers (bottom first). */
    public record Item(String id, String slot, String name, String reward, List<Layer> layers) {}

    private static Layer l(String pattern) { return new Layer("minecraft:" + pattern, ""); }
    private static Layer l(String pattern, String dye) { return new Layer("minecraft:" + pattern, dye); }

    /** The catalogue. The pattern and colour choices are placeholders until Kort's art review (plan 9.5). */
    public static final List<Item> CATALOGUE = List.of(
        new Item("pennant", "pennant", "Renown Pennant", "cosmetic:pennant", List.of(l("triangle_top"))),
        new Item("pennant_pattern", "pennant", "Founders' Pennant", "founders:21", List.of(l("stripe_top"), l("triangles_top"))),
        new Item("trim", "trim", "Renown Trim", "cosmetic:trim", List.of(l("border"))),
        new Item("frame", "frame", "Renown Frame", "cosmetic:frame", List.of(l("curly_border"))),
        new Item("banner_frame", "frame", "Founders' Frame", "founders:29", List.of(l("curly_border", "orange"))),
        new Item("frame_brass", "frame", "Brass Frame", "frame:brass", List.of(l("border", "orange"))),
        new Item("standard", "standard", "Renown Standard", "cosmetic:standard", List.of(l("circle"))),
        new Item("skin_proven", "skin", "Proven", "skin:proven", List.of()));

    private static final Map<String, Item> BY_ID = new LinkedHashMap<>();
    static { for (Item i : CATALOGUE) BY_ID.put(i.id(), i); }

    /** The entry for an id, or null. */
    public static Item item(String id) { return id == null ? null : BY_ID.get(id); }
    /** The entry a reward string grants ("cosmetic:pennant", "skin:proven", "frame:brass", "founders:21"), or null. */
    public static Item byReward(String reward) {
        if (reward != null) for (Item i : CATALOGUE) if (i.reward().equals(reward.trim())) return i;
        return null;
    }
    /** The shown name of an id (the id itself when unknown). */
    public static String name(String id) { Item i = item(id); return i == null ? String.valueOf(id) : i.name(); }

    /**
     * The ids earned by a profile's peaks: every Renown level up to maxRenown (the highest Renown level any House reached), every Mastery level
     * up to maxMastery (the highest any owned troop reached) and each Founders cosmetic tier whose claim is stored ("founders:21"). In catalogue
     * order. Reward parts that name no catalogue entry ("legendary", "title:...") are skipped.
     */
    public static List<String> earned(int maxRenown, int maxMastery, Set<String> claims) {
        Set<String> got = new HashSet<>();
        for (var r : Spec.RENOWN) if (r.level() <= maxRenown) for (String part : r.reward().split(";")) { Item i = byReward(part); if (i != null) got.add(i.id()); }
        for (var r : Spec.MASTERY) if (r.level() <= maxMastery) for (String part : r.reward().split(";")) { Item i = byReward(part); if (i != null) got.add(i.id()); }
        if (claims != null) for (var e : Progression.COSMETIC_TIERS.entrySet()) if (claims.contains(Progression.SEASON + ":" + e.getKey())) { Item i = item(e.getValue()); if (i != null) got.add(i.id()); }
        List<String> out = new ArrayList<>();
        for (Item i : CATALOGUE) if (got.contains(i.id())) out.add(i.id());
        return out;
    }

    /**
     * A slot map repaired: only known slots, each holding a known id of that slot that is owned; kept in SLOTS order. Null becomes empty.
     * The owned set itself is not touched (an id a later build names stays owned).
     */
    public static Map<String, String> normalize(Set<String> owned, Map<String, String> slots) {
        Map<String, String> out = new LinkedHashMap<>();
        if (slots == null) return out;
        for (String slot : SLOTS) {
            String id = slots.get(slot);
            Item i = item(id);
            if (i != null && i.slot().equals(slot) && owned != null && owned.contains(id)) out.put(slot, id);
        }
        return out;
    }

    /**
     * Equips id in slot (or empties it for NONE). Returns the player's notice. Refused, with nothing changed: an unknown slot, an unknown id,
     * an id of another slot, or one not owned ("That cosmetic is locked.").
     */
    public static String equip(Set<String> owned, Map<String, String> slots, String slot, String id) {
        if (slot == null || !SLOTS.contains(slot)) return "Unknown cosmetic slot.";
        if (NONE.equals(id)) { slots.remove(slot); return "Cosmetic slot cleared: " + slot + "."; }
        Item i = item(id);
        if (i == null) return "Unknown cosmetic.";
        if (!i.slot().equals(slot)) return i.name() + " does not go in the " + slot + " slot.";
        if (owned == null || !owned.contains(id)) return "That cosmetic is locked.";
        slots.put(slot, id);
        return "Cosmetic equipped: " + i.name() + ".";
    }

    /** The contrast dye on a field: white, or black on a white field. */
    static String contrast(String fieldDye) { return "white".equals(fieldDye) ? "black" : "white"; }

    /**
     * The vanilla pattern layers of a slot map on a banner whose field is fieldDye: standard, frame, trim, then pennant layers, each with its
     * dye resolved (the contrast dye where the catalogue names none), at most MAX_LAYERS. Every pattern is a minecraft: id.
     */
    public static List<Layer> layers(Map<String, String> slots, String fieldDye) {
        List<Layer> out = new ArrayList<>();
        if (slots == null) return out;
        for (String slot : LAYER_ORDER) {
            Item i = item(slots.get(slot));
            if (i == null || !i.slot().equals(slot)) continue;
            for (Layer x : i.layers()) if (out.size() < MAX_LAYERS) out.add(new Layer(x.pattern(), x.dye().isEmpty() ? contrast(fieldDye) : x.dye()));
        }
        return out;
    }

    /** The title as the wire carries it: "" for none (null, blank or the default title). */
    public static String wireTitle(String title) { return title == null || title.isBlank() || DEFAULT_TITLE.equals(title) ? "" : title; }

    /** The slot map as cosmetic_v1 carries it: "slot=id" in SLOTS order. */
    public static List<String> wire(Map<String, String> slots) {
        List<String> out = new ArrayList<>();
        if (slots != null) for (String slot : SLOTS) { String id = slots.get(slot); if (id != null) out.add(slot + "=" + id); }
        return out;
    }
    /** A cosmetic_v1 list read back into a slot map; unknown slots or ids are dropped (an older client reading a newer server). */
    public static Map<String, String> unwire(List<String> pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        if (pairs != null) for (String p : pairs) { int eq = p == null ? -1 : p.indexOf('='); if (eq > 0) m.put(p.substring(0, eq), p.substring(eq + 1)); }
        Map<String, String> out = new LinkedHashMap<>();
        for (String slot : SLOTS) { Item i = item(m.get(slot)); if (i != null && i.slot().equals(slot)) out.put(slot, i.id()); }
        return out;
    }

    /**
     * The future Wardrobe's preview: while set, the local player draws in this slot map instead of the one the server sent (as
     * CommanderGearFeature.preview shows a loadout). Client thread only; null when no preview runs.
     */
    public static volatile Map<String, String> previewOverride;
}
