package gg.gravesandglory;

import java.util.*;

/**
 * Commander gear core (spec 2026-11-03-commander-gear-design): the saved Piece, stats from the catalogue sheets, gear power and
 * the starter grant. Pure, so the standalone checks compile it; Profile holds the saved state, the sheets hold every name and number.
 * Equip and relic-slot rules are below; the blacksmith's rolls live in Forge.
 */
final class Gear {
    private Gear() {}
    /** One owned piece. Gson-saved, so the field names are save format. Quality 0 to 100 scales every base stat inside the rarity's range. */
    static final class Piece {
        int uid;
        String item, rarity;
        int quality, durability;
        Piece() {}
        Piece(int uid, String item, String rarity, int quality, int durability) {
            this.uid = uid; this.item = item; this.rarity = rarity; this.quality = quality; this.durability = durability;
        }
    }
    static final List<String> ARMOUR = List.of("helm", "chest", "legs", "boots");
    static final List<String> SLOTS = List.of("helm", "chest", "legs", "boots", "main", "off");
    /** What a player may ask the blacksmith for: an armour slot, any weapon (main or off-hand), or a relic. */
    static final List<String> ROLL_SLOTS = List.of("helm", "chest", "legs", "boots", "weapon", "relic");
    static final List<String> STATS = List.of("health", "damage", "armour", "speed", "ranged", "healing", "command");

    static Spec.GearItemsRow item(String id) {
        for (var r : Spec.GEARITEMS) if (r.id().equals(id)) return r;
        return null;
    }
    static Spec.GearRaritiesRow rarity(String id) {
        for (var r : Spec.GEARRARITIES) if (r.id().equals(id)) return r;
        return null;
    }
    static Spec.RelicsRow relic(String id) {
        for (var r : Spec.RELICS) if (r.id().equals(id)) return r;
        return null;
    }
    static Spec.GearSetsRow set(String id) {
        for (var r : Spec.GEARSETS) if (r.id().equals(id)) return r;
        return null;
    }
    static Spec.ForgeRow forge() { return Spec.FORGE.length == 0 ? null : Spec.FORGE[0]; }
    /** Sheet position of a rarity, 0 (lowest) to 4, or -1 when unknown. */
    static int rarityIndex(String rarity) {
        for (int i = 0; i < Spec.GEARRARITIES.length; i++) if (Spec.GEARRARITIES[i].id().equals(rarity)) return i;
        return -1;
    }

    /** The piece's stats in STATS order: base x (statMin + (statMax - statMin) x quality / 100), times brokenStatPct at durability 0. */
    static double[] stats(Piece p) {
        var it = item(p.item);
        var r = rarity(p.rarity);
        double[] out = new double[STATS.size()];
        if (it == null || r == null) return out;
        double scale = r.statMin() + (r.statMax() - r.statMin()) * p.quality / 100.0;
        if (p.durability <= 0) scale *= forge().brokenStatPct();
        double[] base = {it.health(), it.damage(), it.armour(), it.speed(), it.ranged(), it.healing(), it.command()};
        for (int i = 0; i < out.length; i++) out[i] = base[i] * scale;
        return out;
    }

    /** Gear power: the sum of the rarity power of every equipped piece. The stub matchmaking and bot scaling read. */
    static int power(Profile p) {
        int sum = 0;
        for (int uid : p.equipped.values()) {
            Piece piece = piece(p, uid);
            var r = piece == null ? null : rarity(piece.rarity);
            if (r != null) sum += r.power();
        }
        return sum;
    }
    /** The mean gearPower of the humans in a match, 0 when there are none. */
    static double averagePower(Collection<Profile> humans) {
        if (humans.isEmpty()) return 0.0;
        double sum = 0;
        for (Profile h : humans) sum += h.gearPower;
        return sum / humans.size();
    }

    /** The gearSets id whose classes list names the class, else bruiser. */
    static String roleFor(String commanderClass) {
        for (var s : Spec.GEARSETS) if (Arrays.asList(s.classes().split(",")).contains(commanderClass)) return s.id();
        return "bruiser";
    }
    /** The relic that replaces the class's old active, else the first relic in sheet order. */
    static String starterRelic(String commanderClass) {
        for (var r : Spec.RELICS) if (r.fromClass().equals(commanderClass)) return r.id();
        return Spec.RELICS[0].id();
    }

    /**
     * The one-time starter set: four Common armour pieces of the class's role set plus the set's starter weapons, all equipped, and
     * the class's relic unlocked in slot 0. It ignores the inventory cap. Used by a fresh profile, the 3-to-4 migration and an import.
     */
    static void grantStarter(Profile p) {
        var set = set(roleFor(p.commander));
        var f = forge();
        List<String> ids = new ArrayList<>();
        for (String slot : ARMOUR) for (var it : Spec.GEARITEMS) if (it.set().equals(set.id()) && it.slot().equals(slot)) ids.add(it.id());
        if (!set.starterMain().equals("-")) ids.add(set.starterMain());
        if (!set.starterOff().equals("-")) ids.add(set.starterOff());
        for (String id : ids) {
            Piece piece = new Piece(p.nextGearUid++, id, Spec.GEARRARITIES[0].id(), f.starterQuality(), f.maxDurability());
            p.gear.add(piece);
            p.equipped.put(item(id).slot(), piece.uid);
        }
        String relic = starterRelic(p.commander);
        p.relics.add(relic);
        while (p.relicSlots.size() < f.relicSlots()) p.relicSlots.add("");
        p.relicSlots.set(0, relic);
        p.gearPower = power(p);
    }
    /** The owned piece with this uid, or null. */
    static Piece piece(Profile p, int uid) {
        for (Piece piece : p.gear) if (piece != null && piece.uid == uid) return piece;
        return null;
    }

    /**
     * Wears an owned piece in its item's slot. A two-handed main clears the off-hand and an off-hand clears a two-handed main; the
     * displaced piece stays in the inventory. Returns "" or the refusal, and a refusal changes nothing. Durability 0 may be worn.
     */
    static String equip(Profile p, int uid) {
        Piece piece = piece(p, uid);
        var it = piece == null ? null : item(piece.item);
        if (it == null) return "You do not own that piece.";
        p.equipped.put(it.slot(), uid);
        if (it.slot().equals("main") && it.hands() == 2) p.equipped.remove("off");
        if (it.slot().equals("off")) {
            Piece main = piece(p, p.equipped.getOrDefault("main", 0));
            var mainItem = main == null ? null : item(main.item);
            if (mainItem != null && mainItem.hands() == 2) p.equipped.remove("main");
        }
        p.gearPower = power(p);
        return "";
    }
    /** Empties a worn slot (an already empty slot succeeds). The piece stays in the inventory. */
    static String unequip(Profile p, String slot) {
        if (!SLOTS.contains(slot)) return "No such slot.";
        p.equipped.remove(slot);
        p.gearPower = power(p);
        return "";
    }
    /** Puts an unlocked relic into slot 0 to 2 ("-" empties it). A relic already in another slot moves, leaving that slot empty. */
    static String setRelic(Profile p, int slot, String relicId) {
        int slots = forge().relicSlots();
        if (slot < 0 || slot >= slots) return "No such relic slot.";
        boolean clear = "-".equals(relicId);
        if (!clear && (relicId == null || relic(relicId) == null || !p.relics.contains(relicId))) return "You have not unlocked that relic.";
        while (p.relicSlots.size() < slots) p.relicSlots.add("");
        if (!clear) for (int i = 0; i < p.relicSlots.size(); i++) if (relicId.equals(p.relicSlots.get(i))) p.relicSlots.set(i, "");
        p.relicSlots.set(slot, clear ? "" : relicId);
        return "";
    }
}
