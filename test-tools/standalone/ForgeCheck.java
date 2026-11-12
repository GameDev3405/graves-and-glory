package gg.gravesandglory;

import java.util.*;
import java.util.random.RandomGenerator;

/** The blacksmith's rules (gear foundation Task 5): rolls with pity and duplicates, breakdown, repair, expansion and wear. Never shipped. */
public final class ForgeCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    /** A generator whose nextInt(bound) is always 0: the first rarity, the first item, quality 0. */
    static final class Zero implements RandomGenerator {
        public long nextLong() { return 0; }
        @Override public int nextInt(int bound) { return 0; }
    }
    static Gear.Piece piece(int uid, String item, String rarity, int quality, int durability) {
        return new Gear.Piece(uid, item, rarity, quality, durability);
    }
    static Profile rich(int marks) {
        Profile p = new Profile();
        p.forgeMarks = marks;
        p.gearShards = 5000;
        return p;
    }

    public static void main(String[] args) {
        // distribution: 20000 helm rolls, pity held at 0
        var rng = new SplittableRandom(42);
        Map<String, Integer> byRarity = new LinkedHashMap<>(), byItem = new LinkedHashMap<>();
        Profile d = rich(0);
        int n = 20000, totalWeight = Arrays.stream(Spec.GEARRARITIES).mapToInt(Spec.GearRaritiesRow::rollWeight).sum();
        for (int i = 0; i < n; i++) {
            d.gear.clear();
            d.gearPity = 0;
            d.forgeMarks = 75;
            var r = Forge.roll(d, "helm", rng);
            check(r.refusal().isEmpty() && r.piece() != null && Gear.item(r.piece().item).slot().equals("helm"), "a helm roll gives a helm");
            byRarity.merge(r.piece().rarity, 1, Integer::sum);
            byItem.merge(r.piece().item, 1, Integer::sum);
        }
        for (var row : Spec.GEARRARITIES) {
            double share = 100.0 * byRarity.getOrDefault(row.id(), 0) / n, want = 100.0 * row.rollWeight() / totalWeight;
            check(Math.abs(share - want) <= 1.5, row.id() + " share " + share + " vs " + want);
        }
        int helms = 0;
        for (var it : Spec.GEARITEMS) if (it.slot().equals("helm")) helms++;
        check(helms == 9 && byItem.size() == helms, "every helm in the sheet comes up (four starter, five House): " + byItem);
        double even = 100.0 / helms;
        for (var e : byItem.entrySet()) check(Math.abs(100.0 * e.getValue() / n - even) <= 2.0, e.getKey() + " is about " + even + "%: " + e.getValue());

        // pity: the always-zero stub draws common until the pity count
        Profile y = rich(75 * 12);
        var zero = new Zero();
        for (int i = 1; i <= 10; i++) {
            var r = Forge.roll(y, "helm", zero);
            check(r.piece().rarity.equals("common") && y.gearPity == i, "roll " + i + " is common and pity is " + i + ", got " + r.piece().rarity + "/" + y.gearPity);
        }
        var eleventh = Forge.roll(y, "helm", zero);
        check(eleventh.piece().rarity.equals("epic") || eleventh.piece().rarity.equals("legendary"), "roll 11 is epic or better: " + eleventh.piece().rarity);
        check(y.gearPity == 0, "pity resets after an epic");
        Profile rp = rich(1000);
        rp.gearPity = 7;
        Forge.roll(rp, "relic", new SplittableRandom(1));
        check(rp.gearPity == 7, "a relic roll never touches pity");

        // cost
        check(Forge.rollCost("helm") == 75 && Forge.rollCost("weapon") == 75 && Forge.rollCost("relic") == 113, "a helm costs 75, a relic 113");
        Profile cost = rich(113);
        cost.relics.add(Spec.RELICS[0].id());
        var cr = Forge.roll(cost, "relic", new Zero()); // the first relic, already owned: a duplicate
        check(cr.refusal().isEmpty() && cr.duplicate() && cr.refund() == 56 && cost.forgeMarks == 56, "a duplicate relic refunds 56 of 113: " + cr);

        // double request
        Profile two = rich(100);
        int before = two.gear.size();
        var first = Forge.roll(two, "helm", new SplittableRandom(3));
        var second = Forge.roll(two, "helm", new SplittableRandom(3));
        check(first.refusal().isEmpty() && !second.refusal().isEmpty() && two.forgeMarks == 25 && two.gear.size() == before + 1, "the second roll is refused: " + second.refusal() + " marks " + two.forgeMarks);
        check(second.refusal().equals("A roll costs 75 Forge marks.") && second.piece() == null && second.refund() == 0 && !second.duplicate(), "a refusal carries nothing else");
        check(Forge.roll(two, "cape", rng).refusal().equals("No such forge slot.") && Forge.roll(two, null, rng).refusal().equals("No such forge slot."), "an unknown slot is refused");

        // full
        Profile full = rich(1000);
        for (int i = 1; i <= 130; i++) full.gear.add(piece(i, "bruiser_helm", "common", 50, 100));
        full.nextGearUid = 131;
        full.inventorySize = 120;
        var fr = Forge.roll(full, "helm", rng);
        check(fr.refusal().equals("Your inventory is full. Break down or expand first.") && full.forgeMarks == 1000 && full.gear.size() == 130, "a full inventory refuses a helm roll and keeps the marks");
        check(Forge.roll(full, "relic", rng).refusal().isEmpty(), "a relic roll is allowed when full");

        // relic duplicate until dash comes up
        Profile rel = rich(100000);
        for (var r : Spec.RELICS) if (!r.id().equals("dash")) rel.relics.add(r.id());
        var rr = new SplittableRandom(7);
        boolean sawDupe = false, gotDash = false;
        for (int i = 0; i < 400 && !gotDash; i++) {
            int marks = rel.forgeMarks;
            var r = Forge.roll(rel, "relic", rr);
            if (r.duplicate()) {
                sawDupe = true;
                check(r.refund() == 56 && rel.forgeMarks == marks - 113 + 56, "a duplicate relic costs 57 net");
            } else {
                check(r.relic().equals("dash") && rel.relics.contains("dash") && rel.forgeMarks == marks - 113, "the first new relic is dash and costs 113");
                gotDash = true;
            }
        }
        check(sawDupe && gotDash, "draws repeat owned relics until dash");
        int left = rel.forgeMarks;
        var none = Forge.roll(rel, "relic", rr);
        check(none.refusal().equals("You own every relic.") && rel.forgeMarks == left, "all relics owned is refused and keeps the marks");

        // breakdown
        Profile b = rich(0);
        b.gear.add(piece(1, "bruiser_helm", "rare", 50, 100));
        b.gear.add(piece(2, "bruiser_chest", "common", 50, 100));
        b.equipped.put("chest", 2);
        check(Forge.breakdownValue(b.gear.get(0)) == 37 && Forge.breakdownValue(b.gear.get(1)) == 15, "rare gives 37, common 15");
        check(Forge.breakdown(b, 2).equals("Unequip it first.") && b.gear.size() == 2 && b.forgeMarks == 0, "an equipped piece is refused");
        check(Forge.breakdown(b, 99).equals("You do not own that piece.") && b.forgeMarks == 0, "an unowned uid is refused");
        check(Forge.breakdown(b, 1).isEmpty() && b.forgeMarks == 37 && b.gear.size() == 1 && Gear.piece(b, 1) == null, "a rare piece returns 37");

        // dupes
        Profile u = rich(0);
        u.gear.add(piece(1, "bruiser_helm", "epic", 10, 100));
        u.gear.add(piece(2, "bruiser_helm", "epic", 90, 100));
        u.gear.add(piece(3, "bruiser_helm", "epic", 60, 100));
        u.gear.add(piece(4, "bruiser_chest", "common", 50, 100));
        u.gear.add(piece(5, "bruiser_legs", "common", 50, 100));
        u.gear.add(piece(6, "bruiser_boots", "common", 50, 100));
        u.gear.add(piece(7, "falchion", "common", 50, 100));
        u.gear.add(piece(8, "greatsword", "common", 40, 100));
        u.gear.add(piece(9, "greatsword", "common", 40, 100));
        u.equipped.put("helm", 1);
        check(Forge.duplicate(u, u.gear.get(0)) && !Forge.duplicate(u, u.gear.get(3)), "duplicate() finds another piece of the same item and rarity");
        int gained = Forge.breakdownDuplicates(u);
        check(gained == 2 * 52 + 15 && u.forgeMarks == gained, "dupes pay 2 x 52 + 15: " + gained);
        check(Gear.piece(u, 1) != null && Gear.piece(u, 8) != null && Gear.piece(u, 2) == null && Gear.piece(u, 3) == null && Gear.piece(u, 9) == null && u.gear.size() == 6, "the worn helm and the oldest greatsword stay");
        check(Forge.breakdownDuplicates(u) == 0, "a second pass finds nothing");
        Profile q = rich(0);
        q.gear.add(piece(1, "bruiser_helm", "rare", 30, 100));
        q.gear.add(piece(2, "bruiser_helm", "rare", 80, 20));
        q.gear.add(piece(3, "bruiser_helm", "rare", 80, 90));
        Forge.breakdownDuplicates(q);
        check(q.gear.size() == 1 && q.gear.get(0).uid == 3, "with none worn the best quality then durability stays");

        // repair
        Profile rep = rich(0);
        rep.gearShards = 46;
        rep.gear.add(piece(1, "bruiser_helm", "epic", 50, 30));
        rep.gear.add(piece(2, "bruiser_chest", "epic", 50, 100));
        check(Forge.repairCost(rep.gear.get(0)) == 46 && Forge.repairCost(rep.gear.get(1)) == 0, "an epic at 30 costs ceil(65 x 70 / 100) = 46");
        rep.gearShards = 45;
        check(Forge.repairAll(rep).equals("Repairing everything costs 46 gear shards.") && rep.gearShards == 45 && rep.gear.get(0).durability == 30, "repairAll one shard short changes nothing");
        check(Forge.repair(rep, 1).equals("Repair costs 46 gear shards.") && rep.gear.get(0).durability == 30, "repair one shard short is refused");
        check(Forge.repair(rep, 2).equals("It needs no repair.") && Forge.repair(rep, 9).equals("You do not own that piece."), "a full or unowned piece is refused");
        rep.gearShards = 46;
        check(Forge.repairAll(rep).isEmpty() && rep.gearShards == 0 && rep.gear.get(0).durability == 100, "repairAll with enough gear shards repairs and charges the total");
        check(Forge.repairAll(rep).equals("It needs no repair."), "repairAll with nothing damaged is refused");
        rep.gear.get(0).durability = 30;
        rep.gearShards = 100;
        check(Forge.repair(rep, 1).isEmpty() && rep.gearShards == 54 && rep.gear.get(0).durability == 100, "repair charges and restores");

        // expand
        Profile ex = rich(0);
        ex.gearShards = 1250;
        check(Forge.expandCost(ex) == 500, "the first row costs 500");
        check(Forge.expand(ex).isEmpty() && ex.inventorySize == 70 && ex.gearShards == 750 && Forge.expandCost(ex) == 750, "then 750");
        check(Forge.expand(ex).isEmpty() && ex.inventorySize == 80 && ex.gearShards == 0, "a row adds 10 slots");
        check(Forge.expand(ex).equals("Another row costs 1000 gear shards.") && ex.inventorySize == 80, "short gear shards is refused");
        ex.inventorySize = 120;
        ex.gearShards = 100000;
        check(Forge.expand(ex).equals("Your inventory is as large as it gets.") && ex.inventorySize == 120 && ex.gearShards == 100000, "at 120 it is refused");

        // wear
        Profile w = rich(0);
        w.gear.add(piece(1, "bruiser_helm", "common", 50, 100));
        w.gear.add(piece(2, "bruiser_chest", "common", 50, 100));
        w.gear.add(piece(3, "bruiser_legs", "common", 50, 5));
        w.equipped.put("helm", 1);
        w.equipped.put("legs", 3);
        Forge.wear(w);
        check(w.gear.get(0).durability == 90 && w.gear.get(1).durability == 100 && w.gear.get(2).durability == 0, "worn pieces lose 10, others keep 100, a piece at 5 goes to 0");

        System.out.println("ForgeCheck: " + checks + " checks passed");
    }
}
