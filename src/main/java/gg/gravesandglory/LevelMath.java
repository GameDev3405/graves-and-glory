package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure in-match progression rules (in-match progression design sections 3, 4, 5.1 and 9.1): level thresholds, the veteran bounty, the level-and-gear stat factors, worn tiers, Armoury buy states and their copy, gear prices, the bots' buy order and level names. Reads Spec only; no Minecraft types. */
public final class LevelMath {
    private LevelMath() {}

    public static final String WEAPON = "weapon", ARMOUR = "armour";

    /** The level a soldier with this many kill credits holds: the highest levels row whose kills he reaches, never under 1. */
    public static int levelFor(int credits) {
        int level = 1;
        for (var r : Spec.LEVELS) if (credits >= r.kills() && r.level() > level) level = r.level();
        return level;
    }

    /** The kill credits each credited soldier gets for one victim: a commander counts progression.commanderBounty, a temporary troop 1, any other troop its own level (never under 1). */
    public static int bounty(int victimLevel, boolean commander, boolean temporary) {
        if (commander) return Spec.PROGRESSION[0].commanderBounty();
        if (temporary) return 1;
        return Math.max(1, victimLevel);
    }

    /** Max health factor: 1 + the level's healthPct + the worn armour tier's armourPct (stars and every other effect multiply on top). */
    public static double healthFactor(int level, int armourTier) {
        return 1 + levelRow(level).healthPct() + tierRow(armourTier).armourPct();
    }

    /** Main output factor: 1 + the level's damagePct + the worn weapon tier's weaponPct. */
    public static double damageFactor(int level, int weaponTier) {
        return 1 + levelRow(level).damagePct() + tierRow(weaponTier).weaponPct();
    }

    /** The tier a soldier wears on one track: his level caps the army's bought tier, and tier I is always worn. */
    public static int worn(int level, int boughtTier) {
        return Math.max(1, Math.min(level, boughtTier));
    }

    /** An Armoury button's state. */
    public enum Buy { LOCKED, OPEN, BOUGHT, UNAFFORDABLE }

    /** One track's tier for one troop type: BOUGHT once bought; LOCKED until a soldier of the type has been seen at that tier's level this round and the tier below is bought (tier III needs tier II); then OPEN, or UNAFFORDABLE when the gold is short. */
    public static Buy buyState(int tier, int highestLevelSeen, int boughtTier, int gold, int price) {
        if (boughtTier >= tier) return Buy.BOUGHT;
        if (tier > maxTier() || highestLevelSeen < tier || boughtTier < tier - 1) return Buy.LOCKED;
        return gold < price ? Buy.UNAFFORDABLE : Buy.OPEN;
    }

    /** The refusal copy for a weapon buy with no level name ("Needs a level-2 soldier"); "" when the buy is allowed. */
    public static String refusal(int tier, int highestLevelSeen, int boughtTier, int gold, int price, boolean here) {
        return refusal(tier, highestLevelSeen, boughtTier, gold, price, here, WEAPON, null);
    }

    /** The refusal copy for a weapon buy; levelName is the troop's name at the tier's level ("Knight" for tier II of the Order Squire). */
    public static String refusal(int tier, int highestLevelSeen, int boughtTier, int gold, int price, boolean here, String levelName) {
        return refusal(tier, highestLevelSeen, boughtTier, gold, price, here, WEAPON, levelName);
    }

    /** The player copy that refuses a buy, or "" when it is allowed. In order: already bought, the level not yet seen, the tier below not bought, away from the barracks or a held point (here false), not enough gold. */
    public static String refusal(int tier, int highestLevelSeen, int boughtTier, int gold, int price, boolean here, String track, String levelName) {
        if (boughtTier >= tier) return "Already bought";
        if (tier > maxTier() || highestLevelSeen < tier) return "Needs a level-" + tier + " " + (levelName == null || levelName.isEmpty() ? "soldier" : levelName);
        if (boughtTier < tier - 1) return "Buy " + (ARMOUR.equals(track) ? "Armour" : "Weapon") + " " + roman(tier - 1) + " first";
        if (!here) return "Buy at your barracks or a held point";
        if (gold < price) return "Not enough gold";
        return "";
    }

    /** The gear price of a tier for a rarity (rarities.gearPrice2 and gearPrice3), the same on both tracks; tier I is free. */
    public static int price(String rarity, int tier) {
        for (var r : Spec.RARITIES) if (r.id().equals(rarity)) {
            if (tier <= 1) return 0;
            if (tier == 2) return r.gearPrice2();
            if (tier == 3) return r.gearPrice3();
            throw new IllegalArgumentException("no gear tier " + tier);
        }
        throw new IllegalArgumentException("no rarity " + rarity);
    }

    /** A buy: one troop type, one track (WEAPON or ARMOUR), one tier, at its price. */
    public record Option(String troop, String track, int tier, int price) {}

    /** One banner troop type as a bot's Armoury sees it: the highest level seen this round, the bought tier on each track (1 for none), and the living soldiers at level 2 or higher. */
    public record TypeState(String troop, String group, String rarity, int highestLevelSeen, int weaponBought, int armourBought, int livingAtLevel2Plus) {}

    /**
     * The gear a bot buys this economy tick, in order (design 9.1). The types come in banner order. Each pick keeps
     * gearReserveTroops x cheapestTroopPrice gold for rebuys and never goes above gearMaxTier; a buy the gold left cannot
     * cover is skipped and the next one is tried. A tier bought here opens the next tier of its track in the same tick.
     * gearPriority "banner": banner order, weapon then armour. "role": banner order; ranged, siege and support weapon first,
     * infantry and cavalry armour first. "open": every open buy, the type with the most living soldiers at level 2 or higher
     * first (ties in banner order), weapon then armour.
     */
    public static List<Option> botPicks(Spec.BotCommandRow row, List<TypeState> types, int gold, int cheapestTroopPrice) {
        List<TypeState> order = new ArrayList<>(types);
        if ("open".equals(row.gearPriority())) order.sort(Comparator.comparingInt(TypeState::livingAtLevel2Plus).reversed());
        int reserve = row.gearReserveTroops() * cheapestTroopPrice, budget = gold;
        int cap = Math.min(row.gearMaxTier(), maxTier());
        List<Option> out = new ArrayList<>();
        for (var t : order) {
            boolean armourFirst = "role".equals(row.gearPriority()) && ("infantry".equals(t.group()) || "cavalry".equals(t.group()));
            for (String track : armourFirst ? new String[] {ARMOUR, WEAPON} : new String[] {WEAPON, ARMOUR}) {
                int bought = Math.max(1, WEAPON.equals(track) ? t.weaponBought() : t.armourBought());
                for (int tier = bought + 1; tier <= cap; tier++) {
                    int price = price(t.rarity(), tier);
                    if (buyState(tier, t.highestLevelSeen(), bought, budget - reserve, price) != Buy.OPEN) break;
                    out.add(new Option(t.troop(), track, tier, price));
                    budget -= price;
                    bought = tier;
                }
            }
        }
        return out;
    }

    /** A troop's name at a level: its levelNames row at level 2 or 3 (looked up by troops.evolveLine), else the troop's own name (level 1, and Epics and Legendaries at every level). */
    public static String levelName(Spec.TroopsRow t, int level) {
        if (level >= 2) for (var e : Spec.LEVELNAMES) if (e.troop().equals(t.evolveLine()) && e.level() == level) return e.name();
        return t.name();
    }

    /** The highest gear tier the gearTiers sheet has. */
    public static int maxTier() {
        int max = 1;
        for (var r : Spec.GEARTIERS) max = Math.max(max, r.tier());
        return max;
    }

    /** The levels row for a level, clamped to the sheet's range. */
    static Spec.LevelsRow levelRow(int level) {
        Spec.LevelsRow best = null, lowest = null;
        for (var r : Spec.LEVELS) {
            if (lowest == null || r.level() < lowest.level()) lowest = r;
            if (r.level() <= level && (best == null || r.level() > best.level())) best = r;
        }
        return best != null ? best : lowest;
    }

    /** The gearTiers row for a tier, clamped to the sheet's range. */
    static Spec.GearTiersRow tierRow(int tier) {
        Spec.GearTiersRow best = null, lowest = null;
        for (var r : Spec.GEARTIERS) {
            if (lowest == null || r.tier() < lowest.tier()) lowest = r;
            if (r.tier() <= tier && (best == null || r.tier() > best.tier())) best = r;
        }
        return best != null ? best : lowest;
    }

    static String roman(int tier) {
        return switch (tier) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; default -> Integer.toString(tier); };
    }
}
