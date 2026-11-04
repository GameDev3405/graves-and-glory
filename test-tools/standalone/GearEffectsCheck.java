package gg.gravesandglory;

import java.util.*;

/** The pure set-bonus and perk rules (gear-combat plan Task 2). Each effect's row is found by its effect id and the numbers read from it. Never shipped. */
public final class GearEffectsCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-9; }
    static Spec.GearSetBonusesRow bonus(String effect) {
        for (var b : Spec.GEARSETBONUSES) if (b.effect().equals(effect)) return b;
        throw new AssertionError("no set bonus with effect " + effect);
    }
    static Spec.GearPerksRow perk(String effect) {
        for (var k : Spec.GEARPERKS) if (k.effect().equals(effect)) return k;
        throw new AssertionError("no perk with effect " + effect);
    }
    static GearStats.Loadout with(double[] totals, List<Spec.GearSetBonusesRow> bonuses, List<Spec.GearPerksRow> perks, String main) {
        return new GearStats.Loadout(totals, Map.of(), bonuses, perks, Spec.GEARSETS[0].id(), main, "-", GearStats.relics(List.of()));
    }
    static GearStats.Loadout with(List<Spec.GearSetBonusesRow> bonuses, List<Spec.GearPerksRow> perks, String main) {
        return with(new double[Gear.STATS.size()], bonuses, perks, main);
    }
    static GearStats.Loadout bonusOnly(String effect) { return with(List.of(bonus(effect)), List.of(), "-"); }
    static GearStats.Loadout perkOnly(String effect) { return with(List.of(), List.of(perk(effect)), "-"); }
    static Spec.GearFamiliesRow familyWhere(java.util.function.Predicate<Spec.GearFamiliesRow> p, String what) {
        for (var f : Spec.GEARFAMILIES) if (p.test(f)) return f;
        throw new AssertionError("no family " + what);
    }

    public static void main(String[] args) {
        var c = GearStats.combat();
        var e = GearStats.empty();
        check(GearEffects.surroundedFactor(e, 99) == 1 && GearEffects.tauntShare(e) == 0 && GearEffects.relicCooldownFactor(e) == 1, "empty: guard, taunt and cooldown neutral");
        check(GearEffects.grudgeFactor(e, true) == 1 && GearEffects.lastLightFactor(e, 0f) == 1 && GearEffects.hawkeyeFactor(e, 999) == 1, "empty: perks neutral");
        check(GearEffects.drawFactor(e) == 1 && GearEffects.sweepRadiusFactor(e, 6) == 1 && GearEffects.trailFactor(e, false) == 1 && GearEffects.pierceLevel(e) == 0, "empty: weapon rules neutral");
        check(!GearEffects.secondChance(e, false) && GearEffects.oathFactor(e, true) == 1 && near(GearEffects.auraRadius(e, 7), 7), "empty: no second chance, oath or aura change");
        check(GearEffects.troopFactor(e, true, true) == 1 && GearEffects.troopFactor(e, false, true) == 1, "empty: troops unchanged");

        var g = bonus("surrounded_guard");
        var lg = bonusOnly("surrounded_guard");
        check(near(GearEffects.surroundedFactor(lg, c.surroundedCount()), 1 - g.value1()), "surrounded at the sheet count cuts damage by value1");
        check(GearEffects.surroundedFactor(lg, c.surroundedCount() - 1) == 1, "one foe short of surrounded changes nothing");

        check(near(GearEffects.tauntShare(bonusOnly("taunt_aura")), c.tauntShare()), "taunt share is gearCombat.tauntShare");

        var cr = bonus("command_radius");
        check(near(GearEffects.auraRadius(bonusOnly("command_radius"), 5), 5 + cr.value1()), "command_radius adds value1 to an aura");
        var wide = familyWhere(f -> f.auraMult() != 1, "that widens auras");
        check(near(GearEffects.auraRadius(with(List.of(cr), List.of(), wide.id()), 5), (5 + cr.value1()) * wide.auraMult()), "the main family's auraMult scales the aura");
        check(near(GearEffects.commandRadius(e), c.commandRadius()), "the command aura's base is gearCombat.commandRadius");

        var fc = bonus("formation_charge");
        var lc = bonusOnly("formation_charge");
        check(near(GearEffects.troopFactor(lc, true, true), 1 + fc.value2()), "formation_charge adds value2 damage on the Attack order");
        check(near(GearEffects.troopFactor(lc, true, false), 1), "and nothing on other orders");
        double[] t = new double[Gear.STATS.size()];
        t[Gear.STATS.indexOf("command")] = 10;
        var cmd = with(t, List.of(), List.of(), "-");
        check(near(GearEffects.troopFactor(cmd, true, false), 1 + 10 * c.commandPerPoint()), "the command stat lifts troops inside the aura");
        check(GearEffects.troopFactor(cmd, false, false) == 1, "and not outside it");

        var ok = perk("oathkeeper");
        check(near(GearEffects.oathFactor(perkOnly("oathkeeper"), true), 1 - ok.value1()) && GearEffects.oathFactor(perkOnly("oathkeeper"), false) == 1, "oathkeeper guards troops in the aura only");

        check(GearEffects.secondChance(perkOnly("second_chance"), false) && !GearEffects.secondChance(perkOnly("second_chance"), true), "second chance works once a match");

        var q = perk("quickened_relic");
        check(near(GearEffects.relicCooldownFactor(perkOnly("quickened_relic")), Math.max(0.1, 1 - q.value1())), "quickened_relic cuts cooldowns by value1");
        var many = new ArrayList<Spec.GearPerksRow>();
        for (int i = 0; i < 40; i++) many.add(q);
        check(near(GearEffects.relicCooldownFactor(with(List.of(), many, "-")), 0.1), "cooldowns never drop below a tenth");

        var gr = perk("grudge");
        check(near(GearEffects.grudgeFactor(perkOnly("grudge"), true), 1 + gr.value1()) && GearEffects.grudgeFactor(perkOnly("grudge"), false) == 1, "grudge only against the last attacker");

        var ll = perk("last_light");
        var lll = perkOnly("last_light");
        check(near(GearEffects.lastLightFactor(lll, (float) (ll.value2() - 0.01)), 1 + ll.value1()), "last_light below the threshold");
        check(GearEffects.lastLightFactor(lll, (float) (ll.value2() + 0.01)) == 1, "no last_light above it");

        var hk = perk("hawkeye");
        var lh = perkOnly("hawkeye");
        check(near(GearEffects.hawkeyeFactor(lh, hk.value2()), 1 + hk.value1()) && GearEffects.hawkeyeFactor(lh, hk.value2() - 0.5) == 1, "hawkeye from value2 blocks only");

        var ds = bonus("draw_speed");
        var bow = familyWhere(f -> f.drawMult() != 1, "that draws faster");
        check(near(GearEffects.drawFactor(with(List.of(ds), List.of(), bow.id())), bow.drawMult() * (1 + ds.value1())), "draw is the family's drawMult x(1 + draw_speed)");

        var hh = perk("heavy_hand");
        int every = (int) Math.round(hh.value2());
        var lhh = perkOnly("heavy_hand");
        check(every > 1, "heavy_hand counts swings");
        check(near(GearEffects.sweepRadiusFactor(lhh, every), 1 + hh.value1()) && near(GearEffects.sweepRadiusFactor(lhh, every * 2), 1 + hh.value1()), "heavy_hand on every value2-th swing");
        check(GearEffects.sweepRadiusFactor(lhh, every - 1) == 1, "and not between");

        var tb = perk("trailblazer");
        check(near(GearEffects.trailFactor(perkOnly("trailblazer"), false), 1 + tb.value1()) && GearEffects.trailFactor(perkOnly("trailblazer"), true) == 1, "trailblazer only with no foe near");

        check(GearEffects.pierceLevel(bonusOnly("pierce")) == (int) Math.round(bonus("pierce").value1()), "the pierce level is the row's value1");
        System.out.println("GearEffectsCheck: " + checks + " checks passed");
    }
}
