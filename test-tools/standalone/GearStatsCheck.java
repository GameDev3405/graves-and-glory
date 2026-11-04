package gg.gravesandglory;

import java.util.*;

/** Worn gear as a Loadout (gear-combat plan Task 2): raw totals, set counts and bonuses, Epic+ perks, role, families, relic slots and conversions. Rows come from the sheet, never by name. Never shipped. */
public final class GearStatsCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-9; }
    static String classOf(Spec.GearSetsRow s) { return s.classes().split(",")[0]; }
    static Profile starter(Spec.GearSetsRow s) {
        Profile p = new Profile();
        p.commander = classOf(s);
        Gear.grantStarter(p);
        return p;
    }
    static Gear.Piece wear(Profile p, String item, String rarity) {
        Gear.Piece piece = new Gear.Piece(p.nextGearUid++, item, rarity, 70, 100);
        p.gear.add(piece);
        p.equipped.put(Gear.item(item).slot(), piece.uid);
        return piece;
    }
    static Spec.GearItemsRow armourOf(String set, String slot) {
        for (var it : Spec.GEARITEMS) if (it.set().equals(set) && it.slot().equals(slot)) return it;
        throw new AssertionError("no " + set + " " + slot);
    }

    public static void main(String[] args) {
        int slots = Gear.forge().relicSlots();
        var none = GearStats.of(new Profile());
        for (double t : none.totals()) check(t == 0, "a profile wearing nothing has zero totals");
        check(none.bonuses().isEmpty() && none.perks().isEmpty(), "nothing worn switches on nothing");
        check(none.mainFamily().equals("-") && none.offFamily().equals("-"), "empty hands are -");
        check(none.relics().size() == slots, "relics always fill the sheet's slot count");

        var e = GearStats.empty();
        check(GearStats.healthBonus(e) == 0 && GearStats.meleeBonus(e) == 0 && GearStats.armourBonus(e) == 0 && GearStats.speedBonus(e) == 0, "empty adds nothing");
        check(GearStats.rangedFactor(e) == 1 && GearStats.healingFactor(e) == 1 && GearStats.commandBonus(e) == 0, "empty factors are neutral");
        check(e.relics().size() == slots && e.relics().stream().allMatch(String::isEmpty), "empty relic slots are blank");

        for (var s : Spec.GEARSETS) {
            Profile p = starter(s);
            var l = GearStats.of(p);
            check(l.role().equals(s.id()), "a full " + s.id() + " set gives the " + s.id() + " role");
            check(l.setCounts().getOrDefault(s.id(), 0) == Gear.ARMOUR.size(), s.id() + " counts every armour piece");
            int rows = 0;
            for (var b : Spec.GEARSETBONUSES) if (b.set().equals(s.id())) { rows++; check(l.bonuses().contains(b), s.id() + " full set switches on " + b.id()); }
            check(rows == l.bonuses().size(), s.id() + " switches on only its own bonuses");
            check(l.perks().isEmpty(), "starter Commons carry no perk");
            double[] sum = new double[Gear.STATS.size()];
            for (var g : p.gear) { double[] st = Gear.stats(g); for (int i = 0; i < sum.length; i++) sum[i] += st[i]; }
            for (int i = 0; i < sum.length; i++) check(near(l.totals()[i], sum[i]), s.id() + " total " + Gear.STATS.get(i) + " is the raw sum");
            check(l.mainFamily().equals(s.starterMain().equals("-") ? "-" : Gear.item(s.starterMain()).family()), s.id() + " main family follows the starter weapon");
            check(l.offFamily().equals(s.starterOff().equals("-") ? "-" : Gear.item(s.starterOff()).family()), s.id() + " off family follows the starter off-hand");
            check(l.relics().get(0).equals(p.relicSlots.get(0)) && l.relics().size() == slots, s.id() + " relic slot 1 carries the starter relic");

            var f = GearStats.family(l.mainFamily());
            check(near(GearStats.healthBonus(l), GearStats.total(l, "health") * GearStats.combat().healthPerPoint()), "health converts by healthPerPoint");
            check(near(GearStats.meleeBonus(l), GearStats.attackBonus(l) * (f == null ? 1 : f.damageMult())), "melee is damage x the main family's damageMult");
            check(near(GearStats.rangedFactor(l), 1 + GearStats.total(l, "ranged") * GearStats.combat().rangedPerPoint()), "ranged converts by rangedPerPoint");
            check(near(GearStats.healingFactor(l), 1 + GearStats.total(l, "healing") * GearStats.combat().healingPerPoint()), "healing converts by healingPerPoint");
            check(near(GearStats.commandBonus(l), GearStats.total(l, "command") * GearStats.combat().commandPerPoint()), "command converts by commandPerPoint");
        }

        // Two pieces of the last set: only its 2-piece rows. Then pieces of the first set: the larger count wins, a tie goes to sheet order.
        var a = Spec.GEARSETS[Spec.GEARSETS.length - 1];
        var b = Spec.GEARSETS[0];
        Profile two = new Profile();
        two.commander = classOf(b);
        wear(two, armourOf(a.id(), Gear.ARMOUR.get(0)).id(), "common");
        wear(two, armourOf(a.id(), Gear.ARMOUR.get(1)).id(), "common");
        var lt = GearStats.of(two);
        for (var r : Spec.GEARSETBONUSES) check(lt.bonuses().contains(r) == (r.set().equals(a.id()) && r.pieces() <= 2), "two " + a.id() + " pieces: " + r.id());
        check(lt.role().equals(a.id()), "two " + a.id() + " pieces beat the class's own role");
        wear(two, armourOf(b.id(), Gear.ARMOUR.get(2)).id(), "common");
        check(GearStats.of(two).role().equals(a.id()), "two pieces beat one");
        wear(two, armourOf(b.id(), Gear.ARMOUR.get(3)).id(), "common");
        check(GearStats.of(two).role().equals(b.id()), "a tie goes to sheet order");
        Profile bare = new Profile();
        bare.commander = classOf(a);
        check(GearStats.of(bare).role().equals(a.id()), "no armour falls back to the class's role");

        // Perks: Epic and above only, one per item.
        int epic = Gear.rarityIndex(GearStats.PERK_RARITY);
        check(epic > 0, "the perk rarity is in the sheet");
        for (var k : Spec.GEARPERKS) {
            for (var r : Spec.GEARRARITIES) {
                var got = GearStats.perkFor(k.item(), r.id());
                check((Gear.rarityIndex(r.id()) >= epic) == (got == k), k.item() + " at " + r.id() + " carries its perk only from Epic");
            }
            Profile p = new Profile();
            wear(p, k.item(), Spec.GEARRARITIES[Spec.GEARRARITIES.length - 1].id());
            var l = GearStats.of(p);
            check(l.perks().equals(List.of(k)), "a worn top-rarity " + k.item() + " carries " + k.id());
            check(GearStats.has(l, k.effect()) && near(GearStats.value1(l, k.effect()), k.value1()), k.id() + " effect and value1 read back");
        }
        System.out.println("GearStatsCheck: " + checks + " checks passed");
    }
}
