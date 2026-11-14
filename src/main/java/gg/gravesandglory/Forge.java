package gg.gravesandglory;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * The blacksmith's rules (spec 2026-11-03-commander-gear-design): rolls with pity, duplicates, breakdown, repair, inventory expansion and
 * match wear. Pure and server-authoritative: each method checks, then changes the profile, and a refusal changes nothing. Every number comes
 * from the forge and gearRarities sheets. Repairs, rows and rolls cost Profile.gearShards; Gear rolls and breakdowns use the same gear shards (the old Forge marks merged into them).
 */
final class Forge {
    private Forge() {}

    /** One roll's result. On a refusal the piece and relic are null, duplicate false and refund 0. A relic roll has a relic and no piece. */
    record Rolled(String refusal, Gear.Piece piece, String relic, boolean duplicate, int refund) {
        static Rolled refused(String why) { return new Rolled(why, null, null, false, 0); }
    }

    /** Gear shards one roll of this slot costs: rollMarks, or rollMarks x relicRollPct (rounded) for a relic. */
    static int rollCost(String slot) {
        var f = Gear.forge();
        return "relic".equals(slot) ? (int) Math.round(f.rollMarks() * f.relicRollPct()) : f.rollMarks();
    }

    /**
     * Rolls one piece or relic. Refuses an unknown slot, short gear shards, a full inventory (not for a relic) and a relic roll with every relic
     * owned. A relic is drawn uniformly in sheet order; an owned one is a duplicate and refunds floor(cost x relicDuplicatePct) gear shards.
     * Armour and weapons draw an item of the slot (weapon = main or off), a rarity by rollWeight (only epic and legendary once gearPity
     * has reached the pity count), a quality 0 to 100 and full durability. gearPity resets on an epic or better, else rises; a relic roll leaves it.
     */
    static Rolled roll(Profile p, String slot, RandomGenerator rng) { return roll(p, slot, rng, 0); }
    /**
     * The same roll after a drop-board landing on a multiplier (design 2026-11-14; under 1: no board). Rare, epic and legendary chances are
     * multiplied by multiplier / the table's mean and common and uncommon take the rest; on a pity roll (epic or legendary only) legendary is
     * the boosted one. A relic is drawn as ever and the multiplier scales its duplicate refund by multiplier / mean (DropMath.scale), so the
     * average refund is unchanged. Averaged over the slots every chance is the one with no board.
     */
    static Rolled roll(Profile p, String slot, RandomGenerator rng, int multiplier) {
        var f = Gear.forge();
        if (slot == null || !Gear.ROLL_SLOTS.contains(slot)) return Rolled.refused("No such forge slot.");
        int cost = rollCost(slot);
        if (p.gearShards < cost) return Rolled.refused("A roll costs " + cost + " gear shards.");
        if ("relic".equals(slot)) {
            if (Arrays.stream(Spec.RELICS).allMatch(r -> p.relics.contains(r.id()))) return Rolled.refused("You own every relic.");
            p.gearShards -= cost;
            String id = Spec.RELICS[rng.nextInt(Spec.RELICS.length)].id();
            boolean dupe = p.relics.contains(id);
            int refund = 0;
            if (dupe) {
                refund = (int) Math.floor(cost * f.relicDuplicatePct() + 1e-9);
                if (multiplier >= 1) refund = DropMath.scale(refund, multiplier, rng);
                p.gearShards += refund;
            } else {
                p.relics.add(id);
            }
            return new Rolled("", null, id, dupe, refund);
        }
        if (p.gear.size() >= p.inventorySize) return Rolled.refused("Your inventory is full. Break down or expand first.");
        p.gearShards -= cost;
        List<Spec.GearItemsRow> pool = new ArrayList<>();
        for (var it : Spec.GEARITEMS) {
            boolean match = "weapon".equals(slot) ? it.slot().equals("main") || it.slot().equals("off") : it.slot().equals(slot);
            if (match) pool.add(it);
        }
        var item = pool.get(rng.nextInt(pool.size()));
        boolean pity = p.gearPity >= f.pity();
        Spec.GearRaritiesRow rarity = null;
        if (multiplier >= 1) {
            List<Spec.GearRaritiesRow> rows = new ArrayList<>();
            for (var r : Spec.GEARRARITIES) if (!pity || isTop(r.id())) rows.add(r);
            long[] w = new long[rows.size()];
            boolean[] boosted = new boolean[rows.size()];
            for (int i = 0; i < w.length; i++) { w[i] = rows.get(i).rollWeight(); boosted[i] = pity ? rows.get(i).id().equals("legendary") : DropMath.boosted(rows.get(i).id()); }
            rarity = rows.get(DropMath.pick(DropMath.boost(w, boosted, multiplier), rng));
        } else {
            int total = 0;
            for (var r : Spec.GEARRARITIES) if (!pity || isTop(r.id())) total += r.rollWeight();
            int pick = rng.nextInt(total);
            for (var r : Spec.GEARRARITIES) {
                if (pity && !isTop(r.id())) continue;
                rarity = r;
                if (pick < r.rollWeight()) break;
                pick -= r.rollWeight();
            }
        }
        Gear.Piece piece = new Gear.Piece(p.nextGearUid++, item.id(), rarity.id(), rng.nextInt(101), f.maxDurability());
        p.gearPity = isTop(rarity.id()) ? 0 : p.gearPity + 1;
        boolean dupe = duplicate(p, piece);
        p.gear.add(piece);
        return new Rolled("", piece, null, dupe, 0);
    }
    /** Epic or legendary: the rarities that reset the pity count and the only ones a pity roll draws. */
    private static boolean isTop(String rarity) { return rarity.equals("epic") || rarity.equals("legendary"); }

    /** True when another owned piece has the same item and rarity. */
    static boolean duplicate(Profile p, Gear.Piece piece) {
        for (Gear.Piece o : p.gear) if (o != piece && o.uid != piece.uid && o.item.equals(piece.item) && o.rarity.equals(piece.rarity)) return true;
        return false;
    }

    /** Marks a piece breaks down into: floor(rollMarks x its rarity's breakdownPct). */
    static int breakdownValue(Gear.Piece piece) {
        var r = Gear.rarity(piece.rarity);
        return r == null ? 0 : (int) Math.floor(Gear.forge().rollMarks() * r.breakdownPct() + 1e-9);
    }
    private static boolean worn(Profile p, int uid) { return p.equipped.containsValue(uid); }
    /** Breaks one unworn piece down for gear shards. */
    static String breakdown(Profile p, int uid) {
        Gear.Piece piece = Gear.piece(p, uid);
        if (piece == null) return "You do not own that piece.";
        if (worn(p, uid)) return "Unequip it first.";
        p.gearShards += breakdownValue(piece);
        p.gear.remove(piece);
        return "";
    }
    /**
     * Breaks down every duplicate, keeping one piece per (item, rarity): the worn one if any, else the highest quality, then the highest
     * durability, then the oldest uid. Returns the gear shards gained.
     */
    static int breakdownDuplicates(Profile p) {
        Map<String, List<Gear.Piece>> groups = new LinkedHashMap<>();
        for (Gear.Piece g : p.gear) groups.computeIfAbsent(g.item + "/" + g.rarity, k -> new ArrayList<>()).add(g);
        int gained = 0;
        for (List<Gear.Piece> group : groups.values()) {
            if (group.size() < 2) continue;
            Gear.Piece keep = group.get(0);
            for (Gear.Piece g : group) {
                boolean better = worn(p, g.uid) != worn(p, keep.uid) ? worn(p, g.uid)
                    : g.quality != keep.quality ? g.quality > keep.quality
                    : g.durability != keep.durability ? g.durability > keep.durability
                    : g.uid < keep.uid;
                if (better) keep = g;
            }
            for (Gear.Piece g : group) {
                if (g == keep) continue;
                gained += breakdownValue(g);
                p.gear.remove(g);
            }
        }
        p.gearShards += gained;
        return gained;
    }

    /** Gear shards to repair a piece to full: ceil(repairGold x missing durability / max). */
    static int repairCost(Gear.Piece piece) {
        var r = Gear.rarity(piece.rarity);
        int max = Gear.forge().maxDurability();
        int missing = Math.max(0, max - piece.durability);
        return r == null ? 0 : (r.repairGold() * missing + max - 1) / max;
    }
    static String repair(Profile p, int uid) {
        Gear.Piece piece = Gear.piece(p, uid);
        if (piece == null) return "You do not own that piece.";
        if (piece.durability >= Gear.forge().maxDurability()) return "It needs no repair.";
        int cost = repairCost(piece);
        if (p.gearShards < cost) return "Repair costs " + cost + " gear shards.";
        p.gearShards -= cost;
        piece.durability = Gear.forge().maxDurability();
        return "";
    }
    /** Repairs every damaged piece when the gear shards cover the total; otherwise refuses naming the total, changing nothing. */
    static String repairAll(Profile p) {
        int total = 0;
        for (Gear.Piece g : p.gear) total += repairCost(g);
        if (total == 0) return "It needs no repair.";
        if (p.gearShards < total) return "Repairing everything costs " + total + " gear shards.";
        p.gearShards -= total;
        for (Gear.Piece g : p.gear) g.durability = Gear.forge().maxDurability();
        return "";
    }

    /** Gear shards for the next inventory row: rowGold + rowGoldStep per row already added. */
    static int expandCost(Profile p) {
        var f = Gear.forge();
        return f.rowGold() + f.rowGoldStep() * ((p.inventorySize - f.startSlots()) / f.rowSlots());
    }
    static String expand(Profile p) {
        var f = Gear.forge();
        if (p.inventorySize >= f.maxSlots()) return "Your inventory is as large as it gets.";
        int cost = expandCost(p);
        if (p.gearShards < cost) return "Another row costs " + cost + " gear shards.";
        p.gearShards -= cost;
        p.inventorySize = Math.min(f.maxSlots(), p.inventorySize + f.rowSlots());
        return "";
    }

    /** One match of wear: every worn piece loses maxDurability x wearPct / 100, down to 0. */
    static void wear(Profile p) {
        var f = Gear.forge();
        int loss = f.maxDurability() * f.wearPct() / 100;
        for (int uid : p.equipped.values()) {
            Gear.Piece g = Gear.piece(p, uid);
            if (g != null) g.durability = Math.max(0, g.durability - loss);
        }
    }
}
