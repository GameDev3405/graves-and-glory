package gg.gravesandglory;

import java.util.*;

/**
 * A commander's worn gear as one Loadout (gear-combat plan Task 2): the raw stat totals (Kort 2026-11-03: raw totals), the armour set counts,
 * the set bonuses those counts switch on, the fixed perks of worn Epic and Legendary pieces, the role, the weapon families and the three relic
 * slots, plus the stat conversions. Pure, so standalone checks compile it; every number is a sheet cell (gearCombat, gearSetBonuses, gearPerks,
 * gearFamilies). The code knows only the effect ids. GearCombat applies a Loadout to the player; GearEffects reads its bonuses and perks.
 */
final class GearStats {
    private GearStats() {}

    /** totals in Gear.STATS order. mainFamily and offFamily are gearItems.family ids, or - for an empty hand. relics always has the sheet's relic slot count. */
    record Loadout(double[] totals, Map<String, Integer> setCounts, List<Spec.GearSetBonusesRow> bonuses, List<Spec.GearPerksRow> perks,
                   String role, String mainFamily, String offFamily, List<String> relics) {}

    /** The rarity from which a piece carries its item's perk (Kort 2026-11-03: Epic and above only). The id is fixed by preflight's rarity list. */
    static final String PERK_RARITY = "epic";

    static Spec.GearCombatRow combat() { return Spec.GEARCOMBAT[0]; }

    /** The gearFamilies row for a weapon family or an armour slot, or null. */
    static Spec.GearFamiliesRow family(String id) {
        for (var f : Spec.GEARFAMILIES) if (f.id().equals(id)) return f;
        return null;
    }

    /** The Loadout of a profile's worn pieces. A profile wearing nothing has zero totals, no bonuses and the role its old class maps to. */
    static Loadout of(Profile p) {
        double[] totals = new double[Gear.STATS.size()];
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<Spec.GearPerksRow> perks = new ArrayList<>();
        String main = "-", off = "-";
        for (String slot : Gear.SLOTS) {
            Integer uid = p.equipped.get(slot);
            Gear.Piece piece = uid == null ? null : Gear.piece(p, uid);
            var it = piece == null ? null : Gear.item(piece.item);
            if (it == null) continue;
            double[] s = Gear.stats(piece);
            for (int i = 0; i < totals.length; i++) totals[i] += s[i];
            if (Gear.ARMOUR.contains(it.slot())) counts.merge(it.set(), 1, Integer::sum);
            else if (it.slot().equals("main")) main = it.family();
            else off = it.family();
            var perk = perkFor(it.id(), piece.rarity);
            if (perk != null) perks.add(perk);
        }
        List<Spec.GearSetBonusesRow> bonuses = new ArrayList<>();
        for (var b : Spec.GEARSETBONUSES) if (b.pieces() <= counts.getOrDefault(b.set(), 0)) bonuses.add(b);
        String role = null; int best = 0;
        for (var s : Spec.GEARSETS) { int n = counts.getOrDefault(s.id(), 0); if (n > best) { best = n; role = s.id(); } } // sheet order breaks ties
        if (role == null) role = Gear.roleFor(p.commander);
        return new Loadout(totals, counts, bonuses, perks, role, main, off, relics(p.relicSlots));
    }

    /** No gear at all: zero totals, no bonuses or perks, empty hands and empty relic slots. Every factor is 1 and every bonus 0. */
    static Loadout empty() {
        return new Loadout(new double[Gear.STATS.size()], Map.of(), List.of(), List.of(), Spec.GEARSETS[0].id(), "-", "-", relics(List.of()));
    }

    static List<String> relics(List<String> slots) {
        List<String> out = new ArrayList<>();
        int n = Gear.forge().relicSlots();
        for (int i = 0; i < n; i++) out.add(slots != null && i < slots.size() && slots.get(i) != null ? slots.get(i) : "");
        return List.copyOf(out);
    }

    /** The item's fixed perk row when the piece is Epic or better, else null. */
    static Spec.GearPerksRow perkFor(String item, String rarity) {
        if (Gear.rarityIndex(rarity) < Gear.rarityIndex(PERK_RARITY)) return null;
        for (var k : Spec.GEARPERKS) if (k.item().equals(item)) return k;
        return null;
    }

    static double total(Loadout l, String stat) { return l.totals()[Gear.STATS.indexOf(stat)]; }

    /** Max health added to the player. */
    static double healthBonus(Loadout l) { return total(l, "health") * combat().healthPerPoint(); }
    /** Attack damage from the damage stat, before the main weapon's damageMult. */
    static double attackBonus(Loadout l) { return total(l, "damage") * combat().damagePerPoint(); }
    /** The attack damage modifier the player gets: attackBonus times the main family's damageMult (1 with an empty hand). */
    static double meleeBonus(Loadout l) {
        var f = family(l.mainFamily());
        return attackBonus(l) * (f == null ? 1 : f.damageMult());
    }
    static double armourBonus(Loadout l) { return total(l, "armour") * combat().armourPerPoint(); }
    /** Added to the movement speed attribute (a player's base is 0.1). */
    static double speedBonus(Loadout l) { return total(l, "speed") * combat().speedPerPoint(); }
    /** Multiplies the commander's projectile damage. */
    static double rangedFactor(Loadout l) { return 1 + total(l, "ranged") * combat().rangedPerPoint(); }
    /** Multiplies every heal the commander gives. */
    static double healingFactor(Loadout l) { return 1 + total(l, "healing") * combat().healingPerPoint(); }
    /** The extra share of damage the commander's troops inside his command aura deal. */
    static double commandBonus(Loadout l) { return total(l, "command") * combat().commandPerPoint(); }

    /** Whether any active set bonus or perk has this effect. */
    static boolean has(Loadout l, String effect) {
        for (var b : l.bonuses()) if (b.effect().equals(effect)) return true;
        for (var k : l.perks()) if (k.effect().equals(effect)) return true;
        return false;
    }
    /** The value1 of every active bonus and perk with this effect, summed (stacked perks add up). */
    static double value1(Loadout l, String effect) {
        double v = 0;
        for (var b : l.bonuses()) if (b.effect().equals(effect)) v += b.value1();
        for (var k : l.perks()) if (k.effect().equals(effect)) v += k.value1();
        return v;
    }
    /** The largest value2 among the active bonuses and perks with this effect (a threshold or a count, which does not add up). */
    static double value2(Loadout l, String effect) {
        double v = 0;
        for (var b : l.bonuses()) if (b.effect().equals(effect)) v = Math.max(v, b.value2());
        for (var k : l.perks()) if (k.effect().equals(effect)) v = Math.max(v, k.value2());
        return v;
    }
    /** The largest radius among the active set bonuses with this effect; 0 when none. */
    static double radius(Loadout l, String effect) {
        double r = 0;
        for (var b : l.bonuses()) if (b.effect().equals(effect)) r = Math.max(r, b.radius());
        return r;
    }
}
