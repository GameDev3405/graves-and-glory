package gg.gravesandglory;

import java.util.*;

/** Pure gear core (gear foundation Task 2): stats from base, rarity and quality, role and relic lookups, the starter grant and gear power. Never shipped. */
public final class GearCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-9; }
    static Gear.Piece piece(int uid, String item, String rarity, int quality, int durability) {
        return new Gear.Piece(uid, item, rarity, quality, durability);
    }
    static Profile of(String commander) {
        Profile p = new Profile();
        p.commander = commander;
        Gear.grantStarter(p);
        return p;
    }

    public static void main(String[] args) {
        double base = Gear.item("bruiser_helm").health();
        check(base > 0, "the bruiser helm adds health");
        check(near(Gear.stats(piece(1, "bruiser_helm", "legendary", 100, 100))[0], base * 2.2), "legendary at quality 100 is base x 2.2");
        check(near(Gear.stats(piece(1, "bruiser_helm", "legendary", 0, 100))[0], base * 1.9), "legendary at quality 0 is base x 1.9");
        check(near(Gear.stats(piece(1, "bruiser_helm", "legendary", 100, 0))[0], base * 2.2 * 0.5), "a broken piece works at half stats");
        double[] all = Gear.stats(piece(1, "bruiser_helm", "common", 50, 100));
        check(all.length == Gear.STATS.size(), "stats come in STATS order, one per stat");
        check(near(all[2], Gear.item("bruiser_helm").armour() * 1.05), "common at quality 50 sits mid range");

        check(Gear.roleFor("reaver").equals("bruiser"), "reaver is a bruiser");
        check(Gear.roleFor("warlock").equals("healer"), "warlock is a healer");
        check(Gear.roleFor("marshal").equals("commander"), "marshal is a commander");
        check(Gear.roleFor("nope").equals("bruiser"), "an unknown class falls back to bruiser");
        check(Gear.starterRelic("warden").equals("mark_prey"), "the warden's relic is mark_prey");
        check(Gear.starterRelic("nope").equals(Spec.RELICS[0].id()), "an unknown class takes the first relic");

        check(Gear.item("nope") == null && Gear.rarity("nope") == null && Gear.relic("nope") == null && Gear.set("nope") == null, "unknown lookups are null");
        check(Gear.rarityIndex("epic") == 3 && Gear.rarityIndex("x") == -1, "rarity index follows sheet order");
        check(Gear.forge() != null && Gear.forge().relicSlots() == 3, "the forge row is found");

        Profile w = of("warden");
        check(w.gear.size() == 6, "warden starts with 6 pieces, got " + w.gear.size());
        for (int i = 0; i < 4; i++) check(Gear.item(w.gear.get(i).item).set().equals("ranged") && Gear.item(w.gear.get(i).item).slot().equals(Gear.ARMOUR.get(i)), "ranged armour piece " + i);
        check(w.gear.get(4).item.equals("hunting_bow") && w.gear.get(5).item.equals("quiver"), "ranged starts with a bow and a quiver");
        for (var g : w.gear) check(g.rarity.equals("common") && g.quality == 50 && g.durability == 100, "starter pieces are common, quality 50, durability 100");
        check(w.equipped.size() == 6, "all 6 slots are equipped");
        for (var g : w.gear) check(w.equipped.get(Gear.item(g.item).slot()) == g.uid, "slot points at the matching uid for " + g.item);
        check(w.relics.equals(Set.of("mark_prey")) && w.relicSlots.equals(List.of("mark_prey", "", "")), "warden starts with mark_prey in slot 1");
        for (int i = 0; i < 6; i++) check(w.gear.get(i).uid == i + 1, "uids run from 1");
        check(w.nextGearUid == 7 && w.gearPower == 6, "next uid 7 and gear power 6");
        check(Gear.piece(w, 3) == w.gear.get(2) && Gear.piece(w, 99) == null, "piece() finds an owned uid only");

        Profile l = of("warlock");
        check(l.gear.size() == 5 && !l.equipped.containsKey("off"), "warlock gets 5 pieces and no off-hand");
        check(l.gear.get(4).item.equals("mending_staff"), "healer starts with a staff");

        Profile a = new Profile(), b = new Profile();
        a.gearPower = 6; b.gearPower = 10;
        check(near(Gear.averagePower(List.of(a, b)), 8.0), "average of 6 and 10 is 8");
        check(near(Gear.averagePower(List.of()), 0.0), "average of none is 0");
        check(Gear.power(w) == 6, "power sums the equipped rarity power");
        w.gear.get(0).rarity = "epic";
        check(Gear.power(w) == 6 - 1 + Gear.rarity("epic").power(), "an epic piece counts its rarity power");

        System.out.println("GearCheck: " + checks + " checks passed");
    }
}
