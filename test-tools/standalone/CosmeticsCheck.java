package gg.gravesandglory;

import java.util.*;

/**
 * Cosmetics core (plan 2026-11-16-cosmetics-render): grants are idempotent and retroactive, normalize drops what is not owned or known,
 * the equip rules refuse locked or misplaced ids, the vanilla layers keep their order and the cap, and only minecraft: pattern ids reach
 * a banner. Never shipped.
 */
public final class CosmeticsCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    static int renownPoints(int level) {
        for (var r : Spec.RENOWN) if (r.level() == level) return r.points();
        throw new AssertionError("no renown level " + level);
    }

    public static void main(String[] args) {
        // the catalogue: every slot known, every id unique, every reward string of the sheet that names a cosmetic maps to an entry
        Set<String> ids = new HashSet<>();
        for (var i : Cosmetics.CATALOGUE) { check(ids.add(i.id()), "id " + i.id() + " is unique"); check(Cosmetics.SLOTS.contains(i.slot()), i.id() + " has a known slot"); }
        for (var r : Spec.RENOWN) if (r.reward().startsWith("cosmetic:")) check(Cosmetics.byReward(r.reward()) != null, "renown reward " + r.reward() + " maps to a cosmetic");
        for (var r : Spec.MASTERY) for (String part : r.reward().split(";")) if (part.startsWith("skin:") || part.startsWith("frame:")) check(Cosmetics.byReward(part) != null, "mastery reward " + part + " maps to a cosmetic");
        for (String id : Progression.COSMETIC_TIERS.values()) check(Cosmetics.item(id) != null, "Founders cosmetic " + id + " is in the catalogue");

        // earned: nothing at zero; Renown I gives the pennant; Mastery II the Proven skin, III the brass frame; claims give the Founders pair
        check(Cosmetics.earned(0, 0, Set.of()).isEmpty(), "nothing is earned at zero");
        check(Cosmetics.earned(1, 0, Set.of()).equals(List.of("pennant")), "Renown I earns the pennant: " + Cosmetics.earned(1, 0, Set.of()));
        check(Cosmetics.earned(5, 0, Set.of()).containsAll(List.of("pennant", "trim", "frame", "standard")), "Renown IV and past earn all four Renown cosmetics");
        check(Cosmetics.earned(0, 2, Set.of()).equals(List.of("skin_proven")), "Mastery II earns Proven");
        check(Cosmetics.earned(0, 3, Set.of()).equals(List.of("frame_brass", "skin_proven")), "Mastery III earns Proven and the brass frame, in catalogue order");
        check(Cosmetics.earned(0, 0, Set.of("founders:21", "founders:29")).equals(List.of("pennant_pattern", "banner_frame")), "the Founders claims earn their two cosmetics");

        // grants: retroactive on sanitize, idempotent, new ids reported once, Proven auto-equipped only when first granted
        Profile p = new Profile();
        p.sanitize();
        check(p.cosmetics.isEmpty() && p.cosmeticSlots.isEmpty(), "a new profile owns and wears nothing");
        p.renown.put(Profile.houseOrder().get(0), renownPoints(2));
        var owned = p.owned.values().iterator().next();
        owned.mastery = 2;
        p.progression.claims.add("founders:21");
        List<String> fresh = CosmeticGrants.apply(p);
        check(fresh.equals(List.of("pennant", "pennant_pattern", "trim", "skin_proven")), "an old profile past the thresholds gets them all at once: " + fresh);
        check("skin_proven".equals(p.cosmeticSlots.get("skin")), "Proven is equipped when first granted");
        check(CosmeticGrants.apply(p).isEmpty(), "a second apply grants nothing");
        check(CosmeticGrants.lines(List.of("pennant")).equals(List.of("New cosmetic: Renown Pennant")), "the notice names the cosmetic");
        p.cosmeticSlots.remove("skin");
        CosmeticGrants.apply(p);
        check("skin_proven".equals(p.cosmeticSlots.get("skin")), "an owned Proven with the skin slot never set goes back on (the rollback case)");
        check(Cosmetics.equip(p.cosmetics, p.cosmeticSlots, "skin", Cosmetics.NONE).startsWith("Cosmetic slot cleared") && Cosmetics.NONE.equals(p.cosmeticSlots.get("skin")), "turning Proven off stores an explicit none");
        check(CosmeticGrants.apply(p).isEmpty() && Cosmetics.NONE.equals(p.cosmeticSlots.get("skin")), "turning Proven off survives another apply");
        p.sanitize();
        check(Cosmetics.NONE.equals(p.cosmeticSlots.get("skin")), "turning Proven off survives a reload (sanitize)");
        var gs = new com.google.gson.Gson(); Profile off = gs.fromJson(gs.toJson(p), Profile.class); off.sanitize();
        check(Cosmetics.NONE.equals(off.cosmeticSlots.get("skin")), "turning Proven off survives a save and load");
        check(Cosmetics.wire(p.cosmeticSlots).stream().noneMatch(w -> w.startsWith("skin=")), "an explicit none is not sent on the wire");
        // sanitize grants retroactively too, from Renown in a second House
        Profile q = new Profile();
        q.renown.put(Profile.houseOrder().get(1), renownPoints(4));
        q.sanitize();
        check(q.cosmetics.containsAll(List.of("pennant", "trim", "frame", "standard")), "sanitize grants Renown IV's cosmetics from any House: " + q.cosmetics);
        var gson = new com.google.gson.Gson();
        Profile back = gson.fromJson(gson.toJson(q), Profile.class);
        back.sanitize();
        check(back.cosmetics.equals(q.cosmetics) && back.cosmeticSlots.equals(q.cosmeticSlots), "the owned and equipped cosmetics survive a save and load");
        Profile old = gson.fromJson("{\"schema\":7,\"cosmetics\":[\"pennant_pattern\"]}", Profile.class);
        old.sanitize();
        check(old.cosmeticSlots != null && old.cosmeticSlots.isEmpty() && old.cosmetics.contains("pennant_pattern"), "an old save without the slot map loads with none equipped");

        // normalize: unknown slots, unknown ids, ids of another slot, ids not owned all go; SLOTS order
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("skin", "skin_proven"); raw.put("hat", "pennant"); raw.put("pennant", "nope"); raw.put("trim", "frame"); raw.put("frame", "frame_brass"); raw.put("standard", "standard");
        var n = Cosmetics.normalize(Set.of("skin_proven", "frame", "standard"), raw);
        check(n.equals(Map.of("skin", "skin_proven", "standard", "standard")), "normalize keeps only owned ids in their own slot: " + n);
        check(new ArrayList<>(Cosmetics.normalize(Set.of("skin_proven", "pennant"), Map.of("skin", "skin_proven", "pennant", "pennant")).keySet()).equals(List.of("pennant", "skin")), "normalize keeps SLOTS order");
        check(Cosmetics.normalize(Set.of(), null).isEmpty(), "a null map normalizes to empty");

        // equip rules
        Set<String> own = Set.of("pennant", "frame_brass");
        Map<String, String> slots = new LinkedHashMap<>();
        check(Cosmetics.equip(own, slots, "pennant", "pennant").startsWith("Cosmetic equipped") && "pennant".equals(slots.get("pennant")), "an owned id equips");
        check(Cosmetics.equip(own, slots, "pennant", "pennant_pattern").equals("That cosmetic is locked.") && "pennant".equals(slots.get("pennant")), "a locked id is refused and nothing changes");
        check(!Cosmetics.equip(own, slots, "trim", "frame_brass").startsWith("Cosmetic equipped") && !slots.containsKey("trim"), "an id of another slot is refused");
        check(Cosmetics.equip(own, slots, "hat", "pennant").equals("Unknown cosmetic slot."), "an unknown slot is refused");
        check(Cosmetics.equip(own, slots, "frame", "nope").equals("Unknown cosmetic."), "an unknown id is refused");
        check(Cosmetics.equip(own, slots, "frame", "frame_brass").startsWith("Cosmetic equipped") && Cosmetics.equip(own, slots, "frame", Cosmetics.NONE).startsWith("Cosmetic slot cleared") && !slots.containsKey("frame"), "none empties a slot");

        // layers: standard, frame, trim, pennant order; contrast dye; cap; minecraft ids only; skin adds none
        Map<String, String> all = new LinkedHashMap<>();
        all.put("pennant", "pennant_pattern"); all.put("trim", "trim"); all.put("frame", "frame_brass"); all.put("standard", "standard"); all.put("skin", "skin_proven");
        var layers = Cosmetics.layers(all, "red");
        List<String> order = new ArrayList<>(); for (var x : layers) order.add(x.pattern());
        check(order.equals(List.of("minecraft:circle", "minecraft:border", "minecraft:border", "minecraft:stripe_top", "minecraft:triangles_top")), "layers go standard, frame, trim, pennant: " + order);
        check(layers.get(0).dye().equals("white") && layers.get(1).dye().equals("orange"), "the contrast dye is white on a coloured field; a fixed dye stays");
        check(Cosmetics.layers(Map.of("standard", "standard"), "white").get(0).dye().equals("black"), "on a white field the contrast dye is black");
        check(layers.size() <= Cosmetics.MAX_LAYERS, "never more than six layers");
        for (var i : Cosmetics.CATALOGUE) for (var x : i.layers()) check(x.pattern().startsWith("minecraft:") && x.pattern().indexOf(':') == x.pattern().lastIndexOf(':'), i.id() + " uses only minecraft: pattern ids");
        // the worst case the slots allow stays under the cap
        int worst = 0; for (String slot : Cosmetics.LAYER_ORDER) { int m = 0; for (var i : Cosmetics.CATALOGUE) if (i.slot().equals(slot)) m = Math.max(m, i.layers().size()); worst += m; }
        check(worst <= Cosmetics.MAX_LAYERS, "one item per slot never passes six vanilla layers: " + worst);
        check(Cosmetics.layers(Map.of("pennant", "frame"), "red").isEmpty(), "an id in the wrong slot draws nothing");

        // the wire: round trip, unknowns dropped, the default title is no title
        check(Cosmetics.unwire(Cosmetics.wire(all)).equals(Cosmetics.normalize(Set.copyOf(all.values()), all)), "the slot map round-trips the wire");
        check(Cosmetics.unwire(List.of("hat=pennant", "pennant=nope", "frame=frame", "bad")).equals(Map.of("frame", "frame")), "unwire drops unknown slots and ids");
        check(Cosmetics.wireTitle("Recruit").isEmpty() && Cosmetics.wireTitle(null).isEmpty() && Cosmetics.wireTitle("Bannerlord").equals("Bannerlord"), "Recruit is no title on the wire");
        System.out.println("CosmeticsCheck passed " + checks + " checks");
    }
}
