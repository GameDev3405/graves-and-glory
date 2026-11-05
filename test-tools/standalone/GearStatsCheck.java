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

        List<Spec.GearSetsRow> starters = new ArrayList<>(), houses = new ArrayList<>();
        for (var s : Spec.GEARSETS) (s.classes().equals("-") ? houses : starters).add(s);
        check(starters.size() == 4 && houses.size() == 5, "four class starter sets and five House sets");
        for (var s : starters) {
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
        var a = starters.get(starters.size() - 1);
        var b = starters.get(0);
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

        houseSets(houses, classOf(b));
        houseExpected();

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

    /** The five House sets (Kort 2026-11-04): each piece worn adds its count, 2 switch on only the 2-piece bonus, 4 switch on both; the most-worn set is the role; no one starts in them. */
    static void houseSets(List<Spec.GearSetsRow> houses, String commander) {
        for (var s : houses) {
            check(s.starterMain().equals("-") && s.starterOff().equals("-"), s.id() + " starts no one");
            check(!Gear.roleFor(commander).equals(s.id()) && Gear.roleFor("-").equals("bruiser"), "no class maps to " + s.id());
            Profile p = new Profile();
            p.commander = commander;
            for (int n = 0; n < Gear.ARMOUR.size(); n++) {
                wear(p, armourOf(s.id(), Gear.ARMOUR.get(n)).id(), "common");
                var l = GearStats.of(p);
                int worn = n + 1;
                check(l.setCounts().get(s.id()) == worn, s.id() + " counts " + worn);
                check(l.role().equals(s.id()), worn + " " + s.id() + " pieces set the role");
                for (var r : Spec.GEARSETBONUSES) check(l.bonuses().contains(r) == (r.set().equals(s.id()) && r.pieces() <= worn), worn + " " + s.id() + " pieces: " + r.id());
                check(l.perks().isEmpty(), s.id() + " Commons carry no perk");
            }
        }
    }
    /** The approved draft's numbers read back through GearStats (docs/drafts/house-gear-sets-2026-11-04.md). */
    static void houseExpected() {
        // set, then 2-piece and 4-piece: effect, value1, value2, radius
        Object[][] bonus = {
            {"oathbound", "command_radius", 3.0, 0.0, 0.0, "heal_over_time", 0.5, 0.0, 8.0},
            {"barrow_warden", "cleanse", 8.0, 0.0, 6.0, "taunt_aura", 0.0, 0.0, 6.0},
            {"thornhide", "surrounded_guard", 0.2, 0.0, 0.0, "formation_charge", 0.1, 0.15, 0.0},
            {"forgewall", "taunt_aura", 0.0, 0.0, 5.0, "pierce", 1.0, 0.0, 0.0},
            {"tidewrought", "draw_speed", 0.15, 0.0, 0.0, "formation_charge", 0.15, 0.0, 0.0},
        };
        for (var row : bonus) {
            String set = (String) row[0];
            Profile p = new Profile();
            for (String slot : Gear.ARMOUR) wear(p, armourOf(set, slot).id(), "common");
            var l = GearStats.of(p);
            check(l.bonuses().size() == 2, set + " full set switches on two bonuses");
            for (int k = 0; k < 2; k++) {
                String eff = (String) row[1 + 4 * k];
                check(GearStats.has(l, eff), set + " " + eff + " is on");
                check(near(GearStats.value1(l, eff), (double) row[2 + 4 * k]), set + " " + eff + " value1 " + GearStats.value1(l, eff));
                check(near(GearStats.value2(l, eff), (double) row[3 + 4 * k]), set + " " + eff + " value2 " + GearStats.value2(l, eff));
                check(near(GearStats.radius(l, eff), (double) row[4 + 4 * k]), set + " " + eff + " radius " + GearStats.radius(l, eff));
            }
        }
        // set, then the helm, chest, legs and boots perk effects
        String[][] perks = {
            {"oathbound", "oathkeeper", "second_chance", "grudge", "quickened_relic"},
            {"barrow_warden", "quickened_relic", "second_chance", "oathkeeper", "last_light"},
            {"thornhide", "grudge", "last_light", "heavy_hand", "trailblazer"},
            {"forgewall", "hawkeye", "second_chance", "heavy_hand", "quickened_relic"},
            {"tidewrought", "hawkeye", "last_light", "trailblazer", "quickened_relic"},
        };
        String top = Spec.GEARRARITIES[Spec.GEARRARITIES.length - 1].id();
        for (var row : perks) for (int i = 0; i < 4; i++) {
            String item = armourOf(row[0], Gear.ARMOUR.get(i)).id();
            var k = GearStats.perkFor(item, top);
            check(k != null && k.effect().equals(row[1 + i]), item + " carries " + row[1 + i]);
            Profile p = new Profile();
            wear(p, item, top);
            check(GearStats.has(GearStats.of(p), row[1 + i]), "a worn top-rarity " + item + " switches on " + row[1 + i]);
        }
    }
}
