package gg.gravesandglory;

import java.util.*;

/** Server loadout rules (gear foundation Task 3): equip with two-hander interplay, unequip and relic slots. Never shipped. */
public final class GearLoadoutCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static Gear.Piece add(Profile p, String item) {
        Gear.Piece piece = new Gear.Piece(p.nextGearUid++, item, "rare", 80, 100);
        p.gear.add(piece);
        return piece;
    }
    static Profile starter(String commander) {
        Profile p = new Profile();
        p.commander = commander;
        Gear.grantStarter(p);
        return p;
    }

    public static void main(String[] args) {
        // Review Focus 4: a greatsword pushes the shield off, a shield pushes the greatsword off, and no piece is lost
        Profile p = starter("champion");
        int pieces = p.gear.size();
        int sword = p.equipped.get("main"), shield = p.equipped.get("off");
        Gear.Piece great = add(p, "greatsword");
        check(Gear.equip(p, great.uid).isEmpty(), "the greatsword equips");
        check(p.equipped.get("main") == great.uid && !p.equipped.containsKey("off"), "main is the greatsword and off is empty");
        check(Gear.piece(p, sword) != null && Gear.piece(p, shield) != null && p.gear.size() == pieces + 1, "the sword and the shield stay in the inventory");
        check(Gear.equip(p, shield).isEmpty(), "the shield equips");
        check(!p.equipped.containsKey("main") && p.equipped.get("off") == shield, "main is empty and off is the shield");
        check(Gear.piece(p, great.uid) != null, "the greatsword stays in the inventory");
        check(Gear.equip(p, sword).isEmpty() && p.equipped.get("main") == sword && p.equipped.get("off") == shield, "a one-hander and a shield sit together");

        // a bad uid is refused and changes nothing
        Map<String, Integer> before = new LinkedHashMap<>(p.equipped);
        int powerBefore = p.gearPower;
        check(Gear.equip(p, 999).equals("You do not own that piece."), "an unowned uid is refused");
        check(p.equipped.equals(before) && p.gearPower == powerBefore, "a refusal changes nothing");

        // only the matching slot changes
        Profile h = starter("champion");
        Gear.Piece healerChest = add(h, "healer_chest");
        check(Gear.equip(h, healerChest.uid).isEmpty(), "a healer chest equips");
        Map<String, Integer> worn = new LinkedHashMap<>(h.equipped);
        Gear.Piece bruiserChest = add(h, "bruiser_chest");
        check(Gear.equip(h, bruiserChest.uid).isEmpty(), "a bruiser chest equips over it");
        worn.put("chest", bruiserChest.uid);
        check(h.equipped.equals(worn), "only the chest changed");
        check(h.gearPower == Gear.power(h), "gear power is recomputed");

        // a broken piece may still be worn
        Gear.Piece broken = add(h, "bruiser_helm");
        broken.durability = 0;
        check(Gear.equip(h, broken.uid).isEmpty() && h.equipped.get("helm") == broken.uid, "durability 0 is allowed");

        // relic slots
        Profile r = starter("champion");
        r.relics.add("dash");
        check(r.relicSlots.equals(List.of("stand_fast", "", "")), "the champion starts with stand_fast in slot 0");
        check(Gear.setRelic(r, 1, "stand_fast").isEmpty() && r.relicSlots.equals(List.of("", "stand_fast", "")), "a relic moves to the new slot");
        check(Gear.setRelic(r, 3, "dash").equals("No such relic slot.") && Gear.setRelic(r, -1, "dash").equals("No such relic slot."), "slots outside 0 to 2 are refused");
        Profile fresh = starter("champion");
        check(Gear.setRelic(fresh, 0, "dash").equals("You have not unlocked that relic."), "an unowned relic is refused");
        check(fresh.relicSlots.equals(List.of("stand_fast", "", "")), "the refusal changes nothing");
        check(Gear.setRelic(fresh, 0, "nope").equals("You have not unlocked that relic."), "an unknown relic is refused");
        check(Gear.setRelic(fresh, 0, "-").isEmpty() && fresh.relicSlots.equals(List.of("", "", "")), "- empties the slot");
        check(Gear.setRelic(r, 2, "dash").isEmpty() && r.relicSlots.equals(List.of("", "stand_fast", "dash")), "an owned relic fills a free slot");

        // unequip
        Profile u = starter("champion");
        int power = u.gearPower;
        check(Gear.unequip(u, "cape").equals("No such slot."), "an unknown slot is refused");
        check(Gear.unequip(u, "helm").isEmpty() && !u.equipped.containsKey("helm"), "unequip clears the slot");
        check(u.gearPower == power - 1, "gear power drops by the common power of 1");
        check(Gear.unequip(u, "helm").isEmpty(), "clearing an empty slot succeeds");

        System.out.println("GearLoadoutCheck: " + checks + " checks passed");
    }
}
