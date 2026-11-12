package gg.gravesandglory;

import java.util.Locale;

/** What one Armoury buy gives a troop type, in the game's own numbers (Kort, 2026-11-12: "place stats here next to the upgrades so
 *  players know"). A weapon tier raises the soldier's damage factor and an armour tier his max health (LevelMath.damageFactor and
 *  healthFactor, applied by Veterans.wearOne), and a soldier wears a tier only from the Rank of the same number (LevelMath.worn: min of his
 *  level and the tier bought). So tier N is shown on a Rank N soldier of the type, from tier N-1 to tier N, with the commander's star bonus
 *  on top as the server puts it (Campaign.progression: RosterMath.progressionMultiplier of the troop's stars). Client only; read-only. */
final class UpgradeStats {
    private UpgradeStats() {}

    /** The star multiplier the commander's troop fights at (1 for a troop he does not own). */
    static double starMult(Spec.TroopsRow t) {
        var o = CampaignClient.profile.owned.get(t.id());
        var r = RosterMath.rarityRow(t.rarity());
        if (o == null || r == null) return 1;
        return RosterMath.progressionMultiplier(RosterMath.starPct(o.stars, r), Spec.RECRUITMENT[0].capPct());
    }

    /** A track's stat on a soldier of this Rank wearing this tier: damage per hit (weapon) or max health (armour). */
    static double value(Spec.TroopsRow t, String track, int rank, int tier) {
        boolean weapon = LevelMath.WEAPON.equals(track);
        double base = weapon ? t.damage() : t.health();
        return base * starMult(t) * (weapon ? LevelMath.damageFactor(rank, tier) : LevelMath.healthFactor(rank, tier));
    }

    /** One decimal, without a trailing .0. */
    static String num(double v) {
        double r = Math.round(v * 10) / 10.0;
        return r == Math.rint(r) ? String.valueOf((long) r) : String.format(Locale.ROOT, "%.1f", r);
    }

    /** The tier's added share of the base, as the sheet gives it ("+10%"). */
    static String pct(String track, int tier) {
        var lo = LevelMath.tierRow(tier - 1); var hi = LevelMath.tierRow(tier);
        double d = LevelMath.WEAPON.equals(track) ? hi.weaponPct() - lo.weaponPct() : hi.armourPct() - lo.armourPct();
        return "+" + Math.round(d * 100) + "%";
    }

    /** A troop that deals no hit damage (the Sunweaver, the Mist Caller): its weapon tier raises its main effect instead (Veterans.outputFactor). */
    static boolean noHit(Spec.TroopsRow t, String track) { return LevelMath.WEAPON.equals(track) && t.damage() <= 0; }

    /** The cell's line: "Dmg 5.3→5.8" or "HP 31→34" (a Rank-tier soldier, tier below to this tier). */
    static String cell(Spec.TroopsRow t, String track, int tier) {
        if (noHit(t, track)) return "Heal/effect " + pct(track, tier);
        String word = LevelMath.WEAPON.equals(track) ? "Dmg " : "HP ";
        return word + num(value(t, track, tier, tier - 1)) + "→" + num(value(t, track, tier, tier));
    }

    /** The cell's line labelled with the Rank it is shown on, so tier III (shown on a Rank 3 soldier, who also has his Rank's own bonus) does not
     *  read as a jump from tier II's numbers. One form for the whole grid (see form): 0 "Rank 3: Dmg 10.8→11.7", 1 "R3 Dmg 10.8→11.7",
     *  2 "R3 10.8→11.7" (the column's head, Weapons or Armour, names the stat). The effect line of a troop with no hit damage is a share of
     *  base, the same at every Rank, so it has no label. */
    static String cell(Spec.TroopsRow t, String track, int tier, int form) {
        String line = cell(t, track, tier);
        if (noHit(t, track)) return line;
        return form <= 0 ? "Rank " + tier + ": " + line : form == 1 ? "R" + tier + " " + line : "R" + tier + " " + line.substring(line.indexOf(' ') + 1);
    }

    /** The widest form every cell of these troops fits in w px (the narrowest cell, one with a glyph): the stat word stays unless some cell
     *  cannot hold it. The whole grid then reads alike. */
    static int form(Iterable<Spec.TroopsRow> troops, int w, java.util.function.ToIntFunction<String> width) {
        for (int form = 0; form < 2; form++) {
            boolean fits = true;
            for (var t : troops) for (String track : new String[] {LevelMath.WEAPON, LevelMath.ARMOUR}) for (int tier = 2; tier <= 3 && fits; tier++)
                if (width.applyAsInt(cell(t, track, tier, form)) > w) fits = false;
            if (fits) return form;
        }
        return 2;
    }

    /** The tooltip's longer form: "Damage per hit 5.3 → 5.8 (+10% of base) on a Rank 2 soldier; only soldiers at Rank 2 or higher wear it." */
    static String tip(Spec.TroopsRow t, String track, int tier) {
        if (noHit(t, track)) return "No hit damage to raise: the weapon raises its main effect (a heal's amount, a cloud's time) by " + pct(track, tier)
            + " of base on a Rank " + tier + " soldier; only soldiers at Rank " + tier + " or higher wear it.";
        String what = LevelMath.WEAPON.equals(track) ? "Damage per hit " : "Max health ";
        return what + num(value(t, track, tier, tier - 1)) + " → " + num(value(t, track, tier, tier)) + " (" + pct(track, tier) + " of base) on a Rank " + tier
            + " soldier; only soldiers at Rank " + tier + " or higher wear it.";
    }
}
