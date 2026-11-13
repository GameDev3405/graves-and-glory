package gg.gravesandglory;

import java.util.*;

/** The gear screen's pure view rules (gear screen plan Task 1). Expectations come from the sheet rows, never literal catalogue names. Never shipped. */
public final class GearViewCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-9; }
    static GearWire.View view(Profile p) {
        return new GearWire.View(p.gearShards, p.inventorySize, p.gearPity, p.gearPower, new LinkedHashMap<>(p.equipped), new ArrayList<>(p.relics), new ArrayList<>(p.relicSlots), p.gear);
    }
    static Profile starter() {
        Profile p = new Profile();
        Gear.grantStarter(p);
        return p;
    }
    static Spec.GearItemsRow firstIn(String slot, String notSet) {
        for (var it : Spec.GEARITEMS) if (it.slot().equals(slot) && !it.set().equals(notSet)) return it;
        throw new AssertionError("no " + slot + " item outside " + notSet);
    }
    static Gear.Piece add(Profile p, String item, String rarity, int quality, int durability) {
        Gear.Piece g = new Gear.Piece(p.nextGearUid++, item, rarity, quality, durability);
        p.gear.add(g);
        return g;
    }
    static String rarity(int i) { return Spec.GEARRARITIES[i].id(); }

    public static void main(String[] args) {
        Profile p = starter();
        String helmId = Gear.item(Gear.piece(p, p.equipped.get("helm")).item).id();
        String starterSet = Gear.item(helmId).set();
        Gear.Piece better = add(p, helmId, rarity(4), 100, 100);
        Gear.Piece worse = add(p, helmId, rarity(0), 0, 100);
        Gear.Piece epic = add(p, helmId, rarity(3), 50, 100);
        add(p, "not_in_the_sheet", rarity(0), 50, 100);
        GearWire.View v = view(p);

        for (var g : GearView.visible(v, "helm", "rarity")) check(Gear.item(g.item).slot().equals("helm"), "helm filter shows helms only");
        check(GearView.visible(v, "helm", "rarity").size() == 4, "four helms visible");
        for (var g : GearView.visible(v, "weapon", "rarity")) check(Set.of("main", "off").contains(Gear.item(g.item).slot()), "weapon filter is main and off");
        check(!GearView.visible(v, "weapon", "rarity").isEmpty(), "the starter weapons show under weapon");
        check(GearView.visible(v, "all", "newest").stream().noneMatch(g -> g.item.equals("not_in_the_sheet")), "an unknown item is skipped");
        check(GearView.visible(v, "all", "newest").size() == p.gear.size() - 1, "every known piece is visible");
        check(GearView.visible(v, "relic", "rarity").isEmpty(), "relic filter shows no pieces");

        List<Gear.Piece> byRarity = GearView.visible(v, "helm", "rarity");
        check(byRarity.get(0) == better && byRarity.get(1) == epic, "legendary sorts before epic");
        List<Gear.Piece> commons = byRarity.subList(2, 4);
        check(commons.get(0).uid < commons.get(1).uid, "equal rarity and name sorts by uid");
        Profile n = new Profile();
        var it0 = Spec.GEARITEMS[0];
        add(n, it0.id(), rarity(0), 50, 100);
        n.nextGearUid = 9;
        add(n, it0.id(), rarity(0), 50, 100);
        n.gear.get(0).uid = 3;
        List<Gear.Piece> newest = GearView.visible(view(n), "all", "newest");
        check(newest.get(0).uid == 9 && newest.get(1).uid == 3, "newest puts uid 9 before uid 3");
        List<Gear.Piece> bySlot = GearView.visible(v, "all", "slot");
        for (int i = 1; i < bySlot.size(); i++) check(Gear.SLOTS.indexOf(Gear.item(bySlot.get(i - 1).item).slot()) <= Gear.SLOTS.indexOf(Gear.item(bySlot.get(i).item).slot()), "slot sort follows Gear.SLOTS");
        List<Gear.Piece> bySet = GearView.visible(v, "all", "set");
        check(Set.of("main", "off").contains(Gear.item(bySet.get(bySet.size() - 1).item).slot()), "set sort puts weapons last");

        Profile s = starter();
        GearWire.View sv = view(s);
        double[] sum = new double[Gear.STATS.size()];
        for (int uid : s.equipped.values()) { double[] st = Gear.stats(Gear.piece(s, uid)); for (int i = 0; i < sum.length; i++) sum[i] += st[i]; }
        double[] totals = GearView.totals(sv);
        check(s.equipped.size() >= 4 && s.equipped.size() <= 6, "the starter wears 4 to 6 pieces");
        for (int i = 0; i < sum.length; i++) check(near(totals[i], sum[i]), "totals match the worn stats for " + Gear.STATS.get(i));
        Gear.Piece helm = Gear.piece(s, s.equipped.get("helm"));
        double[] whole = Gear.stats(helm);
        helm.durability = 0;
        double[] broken = GearView.totals(view(s));
        for (int i = 0; i < sum.length; i++) check(near(totals[i] - broken[i], whole[i] * (1 - Gear.forge().brokenStatPct())), "a broken worn piece counts half for " + Gear.STATS.get(i));
        helm.durability = 100;

        check(GearView.statText(0).equals("—"), "zero is a dash");
        check(GearView.statText(12.0).equals("12"), "12.0 drops the .0");
        check(GearView.statText(9.54).equals("9.5"), "one decimal");

        check(GearView.setProgress(sv).equals("Set: " + Gear.set(starterSet).name() + " 4 of 4"), "starter set progress, got " + GearView.setProgress(sv));
        Profile mix = starter();
        String other = firstIn("helm", starterSet).set();
        for (String slot : List.of("helm", "chest", "legs")) {
            Spec.GearItemsRow row = null;
            for (var it : Spec.GEARITEMS) if (it.slot().equals(slot) && it.set().equals(other)) row = it;
            Gear.equip(mix, add(mix, row.id(), rarity(0), 50, 100).uid);
        }
        check(GearView.setProgress(view(mix)).equals("Set: " + Gear.set(other).name() + " 3 of 4"), "the set with more worn pieces is named, got " + GearView.setProgress(view(mix)));
        check(GearView.setProgress(view(new Profile())).isEmpty(), "nothing worn, no set line");
        check(GearView.counter(v).equals(p.gear.size() + " of " + p.inventorySize), "counter");

        List<String> up = GearView.compare(v, better);
        check(!up.isEmpty() && up.stream().allMatch(l -> l.startsWith("▲ +") && l.endsWith(" vs worn")), "a better helm shows only up arrows: " + up);
        List<String> down = GearView.compare(v, worse);
        check(!down.isEmpty() && down.stream().allMatch(l -> l.startsWith("▼ -")), "a worse helm shows only down arrows: " + down);
        check(GearView.compare(v, Gear.piece(p, p.equipped.get("helm"))).isEmpty(), "a worn piece compares to nothing");
        Profile bare = starter();
        Gear.unequip(bare, "helm");
        check(GearView.compare(view(bare), Gear.piece(bare, bare.gear.get(0).uid)).equals(List.of("Nothing worn there")), "empty slot");
        check(GearView.worn(v, p.equipped.get("helm")) && !GearView.worn(v, better.uid), "worn");

        Gear.Piece snapped = add(p, helmId, rarity(2), 50, 0);
        GearWire.View v2 = view(p);
        check(GearView.tooltip(v2, snapped).contains("Broken: half stats until repaired"), "broken tooltip");
        check(GearView.tooltip(v2, worse).contains("Duplicate"), "a second common helm is a duplicate");
        check(!GearView.tooltip(v2, better).contains("Duplicate"), "the only legendary helm is no duplicate");
        check(GearView.tooltip(v2, better).get(0).equals(Gear.item(helmId).name()), "tooltip starts with the name");
        check(GearView.tooltip(v2, better).get(GearView.tooltip(v2, better).size() - 1).startsWith("Click to equip"), "unworn footer");
        check(GearView.tooltip(v2, Gear.piece(p, p.equipped.get("helm"))).contains("Worn"), "worn footer");
        check(GearView.tooltip(v2, new Gear.Piece(99, "not_in_the_sheet", rarity(0), 1, 1)).isEmpty(), "an unknown piece has no tooltip");

        for (int i = 0; i < Spec.GEARRARITIES.length; i++) check(GearView.confirmBreakdown(new Gear.Piece(1, helmId, rarity(i), 50, 100)) == (i >= 3), "confirm for rarity " + i);

        GearWire.View rv = new GearWire.View(0, 60, 0, 0, Map.of(), List.of("x"), List.of("x", "", ""), List.of());
        check(GearView.relicTarget(rv, -1) == 1, "first empty relic slot");
        check(GearView.relicTarget(new GearWire.View(0, 60, 0, 0, Map.of(), List.of(), List.of("x", "y", "z"), List.of()), -1) == 0, "all full gives 0");
        check(GearView.relicTarget(rv, 2) == 2, "selected slot wins");

        check(GearView.rows(30) == 4 && GearView.rows(0) == 0 && GearView.rows(8) == 1, "rows");
        check(GearView.scrollClamp(10, 30, 4) == 0, "30 pieces fit 4 rows");
        check(GearView.scrollClamp(3, 120, 6) == 3, "in range stays");
        check(GearView.scrollClamp(20, 120, 6) == 9, "clamped to the last page");

        Profile poor = starter();
        poor.gearShards = Forge.rollCost("helm") - 1;
        check(!GearView.rollRefusal(view(poor), "helm").isEmpty(), "short marks refused");
        Profile full = starter();
        full.gearShards = 10000;
        while (full.gear.size() < 130) add(full, helmId, rarity(0), 50, 100);
        full.inventorySize = 120;
        check(!GearView.rollRefusal(view(full), "helm").isEmpty(), "a full inventory refuses helm");
        check(GearView.rollRefusal(view(full), "relic").isEmpty(), "a full inventory still rolls a relic");
        check(!GearView.rollRefusal(null, "helm").isEmpty(), "no view, no roll");

        int expected = Forge.breakdownDuplicates(GearView.asProfile(view(full)));
        Profile copy = starter();
        copy.gear.clear();
        for (var g : full.gear) copy.gear.add(new Gear.Piece(g.uid, g.item, g.rarity, g.quality, g.durability));
        copy.equipped = new LinkedHashMap<>(full.equipped);
        check(GearView.duplicatesValue(view(full)) == Forge.breakdownDuplicates(copy) && expected == Forge.breakdownDuplicates(starterCopy(full)), "duplicatesValue matches Forge");
        check(full.gear.size() == 130, "the preview leaves the view's pieces alone");

        Profile worn = starter();
        int cost = 0;
        for (var g : worn.gear) { g.durability = 40; cost += Forge.repairCost(g); }
        check(cost > 0 && GearView.repairAllCost(view(worn)) == cost, "repair all sums the damaged pieces");

        // Polish: the greyed perk hint below the perk rarity, and the notices under the Worn row.
        var perk = Spec.GEARPERKS[0];
        Profile pk = starter();
        String hint = GearView.perkHintPrefix() + perk.text();
        check(GearView.perkHintPrefix().equals("From " + Gear.rarity(GearStats.PERK_RARITY).name() + ": "), "the hint names the perk rarity from the sheet");
        for (int i = 0; i < Spec.GEARRARITIES.length; i++) {
            var tip = GearView.tooltip(view(pk), new Gear.Piece(500 + i, perk.item(), rarity(i), 50, 100));
            boolean live = i >= Gear.rarityIndex(GearStats.PERK_RARITY);
            check(tip.contains(hint) == !live, "the From hint shows only below the perk rarity (rarity " + i + ")");
            check(tip.contains(perk.name() + ": " + perk.text()) == live, "the perk line shows only from the perk rarity (rarity " + i + ")");
            check(tip.get(tip.size() - 1).startsWith("Click to equip"), "the action line stays last (rarity " + i + ")");
        }
        Spec.GearItemsRow two = null, offHand = null;
        for (var it : Spec.GEARITEMS) { if (it.slot().equals("main") && it.hands() == 2 && two == null) two = it; if (it.slot().equals("off") && offHand == null) offHand = it; }
        check(two != null && offHand != null, "the sheet has a two-hander and an off hand");
        Profile sw = starter();
        Gear.Piece big = add(sw, two.id(), rarity(0), 50, 100), shield = add(sw, offHand.id(), rarity(0), 50, 100);
        check(Gear.equip(sw, big.uid).isEmpty(), "the two-hander goes on");
        GearWire.View before = view(sw);
        check(Gear.equip(sw, shield.uid).isEmpty() && sw.equipped.get("main") == null, "the off hand takes the two-hander off");
        check(GearView.removedByEquip(before, view(sw)).equals(List.of(two.name())), "the swap names the removed two-hander");
        GearWire.View after = view(sw);
        Gear.unequip(sw, "helm");
        check(GearView.removedByEquip(after, view(sw)).isEmpty(), "taking one piece off names nothing");
        check(GearView.removedByEquip(null, after).isEmpty() && GearView.removedByEquip(after, null).isEmpty(), "no view, no notice");
        Profile none = starter();
        for (String slot : Gear.SLOTS) Gear.unequip(none, slot);
        check(GearView.nothingWorn(view(none)) && !GearView.nothingWorn(view(starter())) && !GearView.nothingWorn(null), "nothingWorn reads the worn row");
        check(GearView.STARTER_NOTICE.equals("Nothing worn. You will fight in the starter kit.") && GearView.swapNotice("Axe").equals("Axe was removed: it needs both hands."), "notice texts");

        System.out.println("GearViewCheck: " + checks + " checks passed");
    }
    static Profile starterCopy(Profile from) {
        Profile c = new Profile();
        for (var g : from.gear) c.gear.add(new Gear.Piece(g.uid, g.item, g.rarity, g.quality, g.durability));
        c.equipped = new LinkedHashMap<>(from.equipped);
        return c;
    }
}
