package gg.gravesandglory;

import java.util.*;

/**
 * The compact gear sync (gear_v1, server to client): one string with the Forge marks, inventory size, pity, gear power, the six worn uids,
 * the unlocked relics, the three relic slots and every piece. The camp snapshot stays under its client limit because the gear list is
 * stripped from it (Campaign.forClient). Format: g1|marks|size|pity|power|e0,e1,e2,e3,e4,e5|relic,relic|s0,s1,s2|uid.item.r.q.d;uid.item.r.q.d
 * with the worn uids in Gear.SLOTS order (0 is empty), r the rarity's sheet position, and an empty list an empty field. Pure, so the standalone
 * checks compile it.
 */
final class GearWire {
    private GearWire() {}
    /** The most characters a gear_v1 string may hold. 130 pieces use about a third of it. */
    static final int LIMIT = 12000;
    private static final String TAG = "g1";

    /** What a client holds of its commander's gear: a decoded gear_v1 string. */
    record View(int forgeMarks, int inventorySize, int pity, int power, Map<String, Integer> equipped, List<String> relics, List<String> relicSlots, List<Gear.Piece> pieces) {}

    static String encode(Profile p) {
        StringBuilder sb = new StringBuilder(TAG).append('|').append(p.forgeMarks).append('|').append(p.inventorySize).append('|').append(p.gearPity).append('|').append(p.gearPower).append('|');
        for (int i = 0; i < Gear.SLOTS.size(); i++) sb.append(i == 0 ? "" : ",").append(p.equipped.getOrDefault(Gear.SLOTS.get(i), 0));
        sb.append('|').append(String.join(",", p.relics)).append('|').append(String.join(",", p.relicSlots)).append('|');
        boolean first = true;
        for (Gear.Piece g : p.gear) {
            if (!first) sb.append(';');
            first = false;
            sb.append(g.uid).append('.').append(g.item).append('.').append(Gear.rarityIndex(g.rarity)).append('.').append(g.quality).append('.').append(g.durability);
        }
        return sb.toString();
    }

    /** The decoded string, or null for anything malformed (wrong tag or field count, a bad number, an unknown rarity position). Never throws. */
    static View decode(String s) {
        try {
            if (s == null || s.length() > LIMIT) return null;
            String[] f = s.split("\\|", -1);
            if (f.length != 9 || !f[0].equals(TAG)) return null;
            String[] worn = f[5].split(",", -1);
            if (worn.length != Gear.SLOTS.size()) return null;
            Map<String, Integer> equipped = new LinkedHashMap<>();
            for (int i = 0; i < worn.length; i++) {
                int uid = Integer.parseInt(worn[i]);
                if (uid < 0) return null;
                if (uid > 0) equipped.put(Gear.SLOTS.get(i), uid);
            }
            List<String> relics = new ArrayList<>();
            for (String r : f[6].split(",", -1)) if (!r.isEmpty()) relics.add(r);
            List<String> slots = new ArrayList<>(Arrays.asList(f[7].split(",", -1)));
            if (slots.size() != Gear.forge().relicSlots()) return null;
            List<Gear.Piece> pieces = new ArrayList<>();
            if (!f[8].isEmpty()) {
                for (String row : f[8].split(";", -1)) {
                    String[] c = row.split("\\.", -1);
                    if (c.length != 5 || c[1].isEmpty()) return null;
                    int rarity = Integer.parseInt(c[2]);
                    if (rarity < 0 || rarity >= Spec.GEARRARITIES.length) return null;
                    pieces.add(new Gear.Piece(Integer.parseInt(c[0]), c[1], Spec.GEARRARITIES[rarity].id(), Integer.parseInt(c[3]), Integer.parseInt(c[4])));
                }
            }
            return new View(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]), equipped, relics, slots, pieces);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
