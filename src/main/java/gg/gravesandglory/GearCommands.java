package gg.gravesandglory;

import java.util.random.RandomGenerator;

/**
 * Parses and applies one gear or forge request from a client (spec 2026-11-03-commander-gear-design). The server never trusts the client:
 * every number is parsed defensively, every uid must be owned and every slot or relic known, and a refusal changes nothing. Pure, so the
 * standalone checks compile it. Requests:
 * gear:equip:uid, gear:unequip:slot, gear:relic:0-2:relicId (or -), forge:roll:slot, forge:breakdown:uid, forge:breakdown-duplicates,
 * forge:repair:uid, forge:repair-all, forge:expand.
 */
final class GearCommands {
    private GearCommands() {}
    static final String INVALID = "That gear request is not valid.";

    /** Whether the request is the commander's Arsenal (gear:equip, gear:unequip, gear:relic, forge:...). Any other gear: is the in-match Armoury's gear:<troop>:<weapon|armour>:<tier> (Veterans.buy), which the battle answers. */
    static boolean arsenal(String cmd) {
        if (cmd.startsWith("forge:")) return true;
        return cmd.startsWith("gear:equip:") || cmd.startsWith("gear:unequip:") || cmd.startsWith("gear:relic:");
    }

    /** refusal is "" on success; notice is the line to show the player; rolled is the roll's result (null for anything but forge:roll). */
    record Outcome(String refusal, String notice, Forge.Rolled rolled) {
        static Outcome refused(String why) { return new Outcome(why, why, null); }
        static Outcome done(String notice) { return new Outcome("", notice, null); }
        boolean ok() { return refusal.isEmpty(); }
    }

    /** Applies the request to the profile. Never throws; a refusal leaves the profile as it was. */
    static Outcome apply(Profile p, String cmd, RandomGenerator rng) {
        try {
            if (p == null || cmd == null) return Outcome.refused(INVALID);
            String[] part = cmd.split(":", -1);
            if (part.length < 2) return Outcome.refused(INVALID);
            if (part[0].equals("gear")) return gear(p, part);
            if (part[0].equals("forge")) return forge(p, part, rng);
            return Outcome.refused(INVALID);
        } catch (RuntimeException e) {
            return Outcome.refused(INVALID);
        }
    }

    /** The uid in part[2], or -1 when it is missing, not a number or below 1. */
    private static int uid(String[] part) {
        if (part.length != 3) return -1;
        try {
            int v = Integer.parseInt(part[2]);
            return v < 1 ? -1 : v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Outcome gear(Profile p, String[] part) {
        switch (part[1]) {
            case "equip": {
                int uid = uid(part);
                Gear.Piece piece = uid < 0 ? null : Gear.piece(p, uid);
                if (piece == null) return Outcome.refused(INVALID);
                String why = Gear.equip(p, uid);
                return why.isEmpty() ? Outcome.done("Equipped: " + Gear.item(piece.item).name()) : Outcome.refused(why);
            }
            case "unequip": {
                if (part.length != 3 || !Gear.SLOTS.contains(part[2])) return Outcome.refused(INVALID);
                String why = Gear.unequip(p, part[2]);
                return why.isEmpty() ? Outcome.done("Unequipped the " + part[2] + " slot") : Outcome.refused(why);
            }
            case "relic": {
                if (part.length != 4) return Outcome.refused(INVALID);
                int slot;
                try {
                    slot = Integer.parseInt(part[2]);
                } catch (NumberFormatException e) {
                    return Outcome.refused(INVALID);
                }
                if (slot < 0 || slot >= Gear.forge().relicSlots() || part[3].isEmpty()) return Outcome.refused(INVALID);
                String why = Gear.setRelic(p, slot, part[3]);
                if (!why.isEmpty()) return Outcome.refused(why);
                return Outcome.done(part[3].equals("-") ? "Relic slot " + (slot + 1) + " emptied" : "Relic set: " + Gear.relic(part[3]).name());
            }
            default:
                return Outcome.refused(INVALID);
        }
    }

    private static Outcome forge(Profile p, String[] part, RandomGenerator rng) {
        switch (part[1]) {
            case "roll": {
                if (part.length != 3 || !Gear.ROLL_SLOTS.contains(part[2])) return Outcome.refused(INVALID);
                Forge.Rolled r = Forge.roll(p, part[2], rng);
                if (!r.refusal().isEmpty()) return Outcome.refused(r.refusal());
                String line;
                if (r.relic() != null) {
                    line = "Rolled: " + Gear.relic(r.relic()).name() + " (Relic)" + (r.duplicate() ? " - already owned, +" + r.refund() + " Forge marks" : "");
                } else {
                    line = "Rolled: " + Gear.item(r.piece().item).name() + " (" + Gear.rarity(r.piece().rarity).name() + ")" + (r.duplicate() ? " - duplicate" : "");
                }
                return new Outcome("", line, r);
            }
            case "breakdown": {
                int uid = uid(part);
                Gear.Piece piece = uid < 0 ? null : Gear.piece(p, uid);
                if (piece == null) return Outcome.refused(INVALID);
                int value = Forge.breakdownValue(piece);
                String why = Forge.breakdown(p, uid);
                return why.isEmpty() ? Outcome.done("+" + value + " Forge marks") : Outcome.refused(why);
            }
            case "breakdown-duplicates": {
                if (part.length != 2) return Outcome.refused(INVALID);
                int before = p.gear.size();
                int gained = Forge.breakdownDuplicates(p);
                return p.gear.size() == before ? Outcome.refused("You have no duplicates to break down.") : Outcome.done("+" + gained + " Forge marks");
            }
            case "repair": {
                int uid = uid(part);
                Gear.Piece piece = uid < 0 ? null : Gear.piece(p, uid);
                if (piece == null) return Outcome.refused(INVALID);
                int cost = Forge.repairCost(piece);
                String why = Forge.repair(p, uid);
                return why.isEmpty() ? Outcome.done("Repaired for " + cost + " gold") : Outcome.refused(why);
            }
            case "repair-all": {
                if (part.length != 2) return Outcome.refused(INVALID);
                int cost = 0;
                for (Gear.Piece g : p.gear) cost += Forge.repairCost(g);
                String why = Forge.repairAll(p);
                return why.isEmpty() ? Outcome.done("Repaired for " + cost + " gold") : Outcome.refused(why);
            }
            case "expand": {
                if (part.length != 2) return Outcome.refused(INVALID);
                String why = Forge.expand(p);
                return why.isEmpty() ? Outcome.done("Inventory: " + p.inventorySize + " slots") : Outcome.refused(why);
            }
            default:
                return Outcome.refused(INVALID);
        }
    }
}
