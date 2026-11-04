package gg.gravesandglory;

/**
 * The pure rules of the set bonuses and perks a Loadout carries (gear-combat plan Task 2). Every number is a sheet cell; with no matching bonus or
 * perk every factor is 1 and every share 0, so GearStats.empty() changes nothing. GearCombat and Battle.allowDamage call these with what they measured.
 */
final class GearEffects {
    private GearEffects() {}

    /** surrounded_guard: damage taken x(1 - value1) while at least gearCombat.surroundedCount foes stand within gearCombat.surroundedRadius. */
    static double surroundedFactor(GearStats.Loadout l, int enemiesNear) {
        if (!GearStats.has(l, "surrounded_guard") || enemiesNear < GearStats.combat().surroundedCount()) return 1;
        return Math.max(0, 1 - GearStats.value1(l, "surrounded_guard"));
    }

    /** taunt_aura: the share of nearby foes that pick the commander as their target (gearCombat.tauntShare); 0 without it. */
    static double tauntShare(GearStats.Loadout l) { return GearStats.has(l, "taunt_aura") ? GearStats.combat().tauntShare() : 0; }

    /** A commander aura's radius: base plus command_radius's value1, times the main weapon family's auraMult (a war banner's, say). */
    static double auraRadius(GearStats.Loadout l, double base) {
        var f = GearStats.family(l.mainFamily());
        return (base + GearStats.value1(l, "command_radius")) * (f == null ? 1 : f.auraMult());
    }

    /** The command aura's radius: gearCombat.commandRadius through auraRadius. */
    static double commandRadius(GearStats.Loadout l) { return auraRadius(l, GearStats.combat().commandRadius()); }

    /** A troop's blow inside its commander's command aura: x(1 + commandBonus), and formation_charge's x(1 + value2) on the Attack order. */
    static double troopFactor(GearStats.Loadout l, boolean inAura, boolean attacking) {
        if (!inAura) return 1;
        double f = 1 + GearStats.commandBonus(l);
        if (attacking && GearStats.has(l, "formation_charge")) f *= 1 + GearStats.value2(l, "formation_charge");
        return f;
    }

    /** oathkeeper: a troop inside its commander's command aura takes x(1 - value1). */
    static double oathFactor(GearStats.Loadout l, boolean inAura) {
        return inAura && GearStats.has(l, "oathkeeper") ? Math.max(0, 1 - GearStats.value1(l, "oathkeeper")) : 1;
    }

    /** second_chance: whether the blow that would down the commander leaves him on value1 health instead (once a match). */
    static boolean secondChance(GearStats.Loadout l, boolean usedThisMatch) { return !usedThisMatch && GearStats.has(l, "second_chance"); }

    /** quickened_relic: relic cooldowns x(1 - value1), never below a tenth. */
    static double relicCooldownFactor(GearStats.Loadout l) { return Math.max(0.1, 1 - GearStats.value1(l, "quickened_relic")); }

    /** grudge: x(1 + value1) against whoever last hurt a troop of the commander's. */
    static double grudgeFactor(GearStats.Loadout l, boolean attackerHitMeLast) {
        return attackerHitMeLast && GearStats.has(l, "grudge") ? 1 + GearStats.value1(l, "grudge") : 1;
    }

    /** last_light: x(1 + value1) while the commander's health is below value2 of his maximum. */
    static double lastLightFactor(GearStats.Loadout l, float healthFraction) {
        return GearStats.has(l, "last_light") && healthFraction < GearStats.value2(l, "last_light") ? 1 + GearStats.value1(l, "last_light") : 1;
    }

    /** hawkeye: x(1 + value1) on a shot from value2 blocks or further. */
    static double hawkeyeFactor(GearStats.Loadout l, double distance) {
        return GearStats.has(l, "hawkeye") && distance >= GearStats.value2(l, "hawkeye") ? 1 + GearStats.value1(l, "hawkeye") : 1;
    }

    /** draw_speed: a bow's draw progress x the main family's drawMult x(1 + value1). */
    static double drawFactor(GearStats.Loadout l) {
        var f = GearStats.family(l.mainFamily());
        return (f == null ? 1 : f.drawMult()) * (1 + GearStats.value1(l, "draw_speed"));
    }

    /** heavy_hand: every value2-th greatweapon swing sweeps x(1 + value1) wider; swing counts from 1. */
    static double sweepRadiusFactor(GearStats.Loadout l, int swing) {
        if (!GearStats.has(l, "heavy_hand")) return 1;
        int every = (int) Math.round(GearStats.value2(l, "heavy_hand"));
        return every > 0 && swing % every == 0 ? 1 + GearStats.value1(l, "heavy_hand") : 1;
    }

    /** trailblazer: movement speed x(1 + value1) with no foe within value2 blocks. */
    static double trailFactor(GearStats.Loadout l, boolean foeNear) {
        return !foeNear && GearStats.has(l, "trailblazer") ? 1 + GearStats.value1(l, "trailblazer") : 1;
    }

    /** pierce: the pierce level of the commander's arrows. */
    static int pierceLevel(GearStats.Loadout l) { return (int) Math.round(GearStats.value1(l, "pierce")); }
}
