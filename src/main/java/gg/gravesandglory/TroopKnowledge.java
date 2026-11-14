package gg.gravesandglory;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.*;

/** What the commander knows of a troop (Kort, 2026-11-12): a troop he owns shows its name, portrait and numbers; one he has not
 *  recruited shows only a silhouette and "Unknown troop" everywhere (the roll, the troop page's faces, the Odds note, the tips). The
 *  stats come from the troops sheet (Spec.TROOPS) with the commander's star bonus on top as the server applies it
 *  (UpgradeStats.starMult). Client only; read-only. */
final class TroopKnowledge {
    private TroopKnowledge() {}

    /** Kort, 2026-11-12: hide the name of a troop not yet recruited. */
    static final boolean SHOW_UNOWNED_NAMES = false;
    static final String UNKNOWN = "Unknown troop";
    /** What every face of an unrecruited troop's page says in place of its contents. */
    static final String HIDDEN = "Recruit this troop to learn about it.";
    /** The letter's kind line for an unrecruited troop (the House is already the roll's). */
    static final String NOT_RECRUITED = "Not yet recruited";

    static boolean owned(String troopId) { return troopId != null && CampaignClient.profile.owned.containsKey(troopId); }

    /** The name the commander may see: the real one once owned, else UNKNOWN. */
    static String name(String troopId, String real) { return SHOW_UNOWNED_NAMES || owned(troopId) ? real : UNKNOWN; }

    static String name(Spec.TroopsRow t) { return t == null ? UNKNOWN : name(t.id(), t.name()); }

    /** One row of the Stats face: its label, its value, and an optional note after it (sepia), "" for none. */
    record Stat(String key, String label, String value, String note) {}

    /** The Stats face of an owned troop, in order: role, health, damage per hit, attack interval, damage per second, range, move
     *  speed, deploy cost, training time, army slots, weapon and armour. Every number is a sheet cell or a product of them. */
    static List<Stat> stats(Spec.TroopsRow t) {
        List<Stat> out = new ArrayList<>();
        double mult = UpgradeStats.starMult(t);
        String bonus = mult > 1.0001 ? "base %s, +" + Math.round((mult - 1) * 100) + "% from stars" : "";
        String role = RecruitView.group(t.group()) + ", " + (t.ranged() ? "ranged" : "melee") + (t.magic() ? ", magic" : "");
        out.add(new Stat("role", "Role", role, ""));
        out.add(new Stat("health", "Health", UpgradeStats.num(t.health() * mult), bonus.isEmpty() ? "" : String.format(Locale.ROOT, bonus, t.health())));
        double hit = t.damage() * mult;
        out.add(new Stat("damage", "Damage per hit", UpgradeStats.num(hit), bonus.isEmpty() ? "" : String.format(Locale.ROOT, bonus, t.damage())));
        double every = t.cooldown() / 20.0;
        out.add(new Stat("interval", "Attacks every", String.format(java.util.Locale.ROOT, "%.2f", every) + " s", "")); // two decimals, so it agrees with the damage per second
        if (every > 0) out.add(new Stat("dps", "Damage per second", UpgradeStats.num(hit / every), ""));
        out.add(new Stat("range", "Range", UpgradeStats.num(t.range()) + " blocks", t.ranged() ? "" : "melee reach"));
        out.add(new Stat("speed", "Move speed", speedLabel(t.speed()), "")); // the word only: the sheet's raw speed is no unit a player knows
        out.add(new Stat("cost", "Deploy cost", t.cost() + " gold", ""));
        out.add(new Stat("training", "Training time", UpgradeStats.num(t.trainingSeconds()) + " s", ""));
        List<String> armour = new ArrayList<>();
        for (String id : new String[]{t.helmet(), t.chest()}) { String n = item(id); if (!n.isEmpty()) armour.add(n); }
        String weapon = item(t.weapon());
        if (!weapon.isEmpty()) out.add(new Stat("weapon", "Weapon", weapon, ""));
        if (!armour.isEmpty()) out.add(new Stat("armour", "Armour", String.join(", ", armour), ""));
        return out;
    }

    /** speedWord with a capital: "Slow", "Average" or "Fast". */
    static String speedLabel(double speed) { String w = speedWord(speed); return w.isEmpty() ? "" : Character.toUpperCase(w.charAt(0)) + w.substring(1); }

    /** "slow", "average" or "fast": where the speed falls among every troop of the sheet, by thirds. */
    static String speedWord(double speed) {
        double[] all = Arrays.stream(Spec.TROOPS).mapToDouble(Spec.TroopsRow::speed).sorted().toArray();
        if (all.length < 3) return "";
        double lo = all[all.length / 3], hi = all[2 * all.length / 3];
        return speed < lo ? "slow" : speed >= hi ? "fast" : "average";
    }

    /** An item id's English name ("minecraft:iron_sword" reads "Iron Sword"), or "" for none. */
    static String item(String id) {
        if (id == null || id.isBlank() || id.equals("-")) return "";
        try {
            var it = Registries.ITEM.get(new Identifier(id));
            if (it == net.minecraft.item.Items.AIR) return "";
            return it.getName().getString();
        } catch (Exception e) {
            return "";
        }
    }
}
