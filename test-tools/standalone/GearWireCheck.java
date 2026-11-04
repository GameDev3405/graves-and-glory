package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.*;

/** The gear_v1 string (gear foundation Task 7): the round trip, the size at 120 and 130 pieces, the untouched camp snapshot and malformed input. Never shipped. */
public final class GearWireCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create(); // same settings as Campaign.JSON

    static Profile full(int pieces) {
        Profile p = new Profile();
        String longest = "";
        for (var it : Spec.GEARITEMS) if (it.id().length() > longest.length()) longest = it.id();
        for (int i = 0; i < pieces; i++) p.gear.add(new Gear.Piece(100000 + i, longest, "legendary", 100, 100));
        for (var r : Spec.RELICS) p.relics.add(r.id());
        p.relicSlots = new ArrayList<>(List.of(Spec.RELICS[0].id(), Spec.RELICS[1].id(), Spec.RELICS[2].id()));
        p.forgeMarks = 1_000_000;
        p.inventorySize = 120;
        p.gearPity = 10;
        p.gearPower = 99;
        return p;
    }

    public static void main(String[] args) {
        // round trip: a starter profile plus three rolled pieces
        Profile p = new Profile();
        Gear.grantStarter(p);
        p.gear.add(new Gear.Piece(p.nextGearUid++, "greatsword", "epic", 77, 40));
        p.gear.add(new Gear.Piece(p.nextGearUid++, "crossbow", "legendary", 0, 0));
        p.gear.add(new Gear.Piece(p.nextGearUid++, "kite_shield", "uncommon", 100, 100));
        p.forgeMarks = 1234;
        p.gearPity = 4;
        p.inventorySize = 70;
        Forge.wear(p);
        String text = GearWire.encode(p);
        check(text.startsWith("g1|1234|70|4|"), "the header carries marks, size and pity: " + text);
        var v = GearWire.decode(text);
        check(v != null, "the string decodes");
        check(v.forgeMarks() == 1234 && v.inventorySize() == 70 && v.pity() == 4 && v.power() == p.gearPower, "marks, size, pity and power round trip");
        check(v.equipped().equals(p.equipped) && v.relics().equals(new ArrayList<>(p.relics)) && v.relicSlots().equals(p.relicSlots), "equipped, relics and relic slots round trip: " + v.relicSlots());
        check(v.pieces().size() == p.gear.size(), "every piece comes back");
        for (int i = 0; i < p.gear.size(); i++) {
            var a = p.gear.get(i);
            var b = v.pieces().get(i);
            check(a.uid == b.uid && a.item.equals(b.item) && a.rarity.equals(b.rarity) && a.quality == b.quality && a.durability == b.durability, "piece " + i + " round trips");
        }
        Profile bare = new Profile();
        var empty = GearWire.decode(GearWire.encode(bare));
        check(empty != null && empty.pieces().isEmpty() && empty.relics().isEmpty() && empty.equipped().isEmpty() && empty.relicSlots().equals(List.of("", "", "")), "an empty profile round trips with empty lists");

        // size
        int at120 = GearWire.encode(full(120)).length(), at130 = GearWire.encode(full(130)).length();
        check(at120 <= GearWire.LIMIT / 2, "120 full pieces fit half the limit: " + at120);
        check(at130 <= GearWire.LIMIT, "130 pieces fit the limit: " + at130);
        check(GearWire.decode(GearWire.encode(full(130))).pieces().size() == 130, "130 pieces decode");

        // snapshot untouched
        JsonObject none = JSON.toJsonTree(full(0)).getAsJsonObject(), many = JSON.toJsonTree(full(120)).getAsJsonObject();
        check(none.has(Profile.GEAR) && many.has(Profile.GEAR), "the gear list is saved under Profile.GEAR");
        none.remove(Profile.GEAR);
        many.remove(Profile.GEAR);
        check(JSON.toJson(none).length() == JSON.toJson(many).length(), "with the gear key removed the profile JSON does not grow with the pieces");

        // malformed
        check(GearWire.decode("") == null && GearWire.decode(null) == null, "empty and null are malformed");
        check(GearWire.decode("g1|x") == null, "a short string is malformed");
        check(GearWire.decode(text.replaceFirst("^g1", "g2")) == null, "another tag is malformed");
        check(GearWire.decode(GearWire.encode(bare) + "1.helm.9.50.100") == null, "a piece with rarity position 9 is malformed");
        check(GearWire.decode("g1|a|60|0|0|0,0,0,0,0,0|||") == null, "a non-number is malformed");
        check(GearWire.decode("g1|0|60|0|0|0,0,0|||") == null, "the wrong number of worn slots is malformed");
        check(GearWire.decode("g1|0|60|0|0|0,0,0,0,0,0||,|") == null, "the wrong number of relic slots is malformed");
        check(GearWire.decode("g1|0|60|0|0|0,0,0,0,0,0|||1.helm.0.50") == null, "a short piece is malformed");
        check(GearWire.decode("x".repeat(GearWire.LIMIT + 1)) == null, "an over-limit string is malformed");

        System.out.println("GearWireCheck: " + checks + " checks passed");
    }
}
