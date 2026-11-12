package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.*;

/** The server-side gear and forge request parser (gear foundation Task 8): bad requests refuse and change nothing, good ones apply. Never shipped. */
public final class GearCommandsCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        Profile p = new Profile();
        Gear.grantStarter(p);
        p.forgeMarks = 75;
        p.gold = 500;
        var rng = new SplittableRandom(5);

        String[] bad = {"gear:equip:999999", "forge:breakdown:abc", "gear:relic:7:x", "gear:relic:0", "forge:roll:", "forge:roll:cape", "gear:", "forge:frob", "gear:equip:-1",
            "gear", "", "forge:repair:0", "gear:unequip:cape", "gear:relic:0:", "gear:equip:1:2", "forge:expand:1", "forge:roll", "gear:equip:99999999999", "forge:breakdown-duplicates:x"};
        for (String cmd : bad) {
            String before = JSON.toJson(p);
            var o = GearCommands.apply(p, cmd, rng);
            check(o.refusal().equals(GearCommands.INVALID) && o.notice().equals(GearCommands.INVALID) && !o.ok() && o.rolled() == null, "'" + cmd + "' is refused as not valid, got '" + o.refusal() + "'");
            check(before.equals(JSON.toJson(p)), "'" + cmd + "' changes nothing");
        }
        check(!GearCommands.arsenal("gear:knight:weapon:2") && GearCommands.arsenal("gear:equip:5") && GearCommands.arsenal("gear:unequip:helm") && GearCommands.arsenal("gear:relic:0:x") && GearCommands.arsenal("forge:roll:helm"), "arsenal(): the Arsenal verbs are the Arsenal's, the Armoury's gear:<troop>:<track>:<tier> is not");
        check(GearCommands.apply(null, "gear:equip:1", rng).refusal().equals(GearCommands.INVALID) && GearCommands.apply(p, null, rng).refusal().equals(GearCommands.INVALID), "null profile and null command are refused");
        check(GearCommands.apply(p, "gear:relic:0:nonesuch", rng).refusal().equals("You have not unlocked that relic.") && JSON.toJson(p).equals(JSON.toJson(p)), "an unknown relic id in a real slot is refused with Gear's reason");

        // roll then equip
        var roll = GearCommands.apply(p, "forge:roll:helm", rng);
        check(roll.ok() && roll.notice().startsWith("Rolled: ") && roll.rolled() != null && roll.rolled().piece() != null, "a helm roll with 75 marks succeeds: " + roll);
        check(p.forgeMarks == 0, "the roll cost 75 marks");
        int uid = roll.rolled().piece().uid;
        var eq = GearCommands.apply(p, "gear:equip:" + uid, rng);
        check(eq.ok() && eq.notice().startsWith("Equipped: ") && p.equipped.get("helm") == uid, "equipping the rolled helm works: " + eq);
        check(GearCommands.apply(p, "forge:roll:helm", rng).refusal().equals("A roll costs 75 Forge marks."), "a roll with no marks says the price");

        // breakdown refusal of a worn piece
        int boots = p.equipped.get("boots");
        String before = JSON.toJson(p);
        var worn = GearCommands.apply(p, "forge:breakdown:" + boots, rng);
        check(worn.refusal().equals("Unequip it first.") && before.equals(JSON.toJson(p)), "a worn piece cannot be broken down: " + worn.refusal());
        var spare = GearCommands.apply(p, "gear:unequip:helm", rng);
        check(spare.ok() && !p.equipped.containsKey("helm"), "unequip empties the slot");
        int marks = p.forgeMarks;
        var gone = GearCommands.apply(p, "forge:breakdown:" + uid, rng);
        check(gone.ok() && gone.notice().startsWith("+") && gone.notice().endsWith(" Forge marks") && p.forgeMarks > marks && Gear.piece(p, uid) == null, "the unworn helm breaks down for marks: " + gone.notice());

        // relic, repair, expand
        p.relics.add(Spec.RELICS[0].id());
        var relic = GearCommands.apply(p, "gear:relic:1:" + Spec.RELICS[0].id(), rng);
        check(relic.ok() && p.relicSlots.get(1).equals(Spec.RELICS[0].id()), "an owned relic goes into a slot: " + relic);
        check(GearCommands.apply(p, "gear:relic:1:-", rng).ok() && p.relicSlots.get(1).isEmpty(), "- empties a relic slot");
        p.gear.get(0).durability = 40;
        var fix = GearCommands.apply(p, "forge:repair:" + p.gear.get(0).uid, rng);
        check(fix.ok() && fix.notice().startsWith("Repaired for ") && p.gear.get(0).durability == 100, "repair restores durability: " + fix.notice());
        check(GearCommands.apply(p, "forge:repair-all", rng).refusal().equals("It needs no repair."), "repair-all with nothing damaged is refused");
        p.gold = 500;
        var big = GearCommands.apply(p, "forge:expand", rng);
        check(big.ok() && p.inventorySize == 70 && big.notice().equals("Inventory: 70 slots"), "expand with 500 gold gives 70 slots: " + big);
        check(GearCommands.apply(p, "forge:breakdown-duplicates", rng).refusal().equals("You have no duplicates to break down."), "no duplicates is refused");

        System.out.println("GearCommandsCheck: " + checks + " checks passed");
    }
}
