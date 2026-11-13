package gg.gravesandglory;

import java.util.*;

/** The War Table page's paint rules (BannerDrop): a stroke places, removes, or is refused in plain words, and never changes the table it was given. */
public final class BannerDropCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }

    static final RosterMath.Catalog C = RosterMath.Catalog.fromSpec();
    static final BannerDrop.Rules ALL = new BannerDrop.Rules(C, new HashSet<>(C.house().keySet()), new HashSet<>(C.house().values()));

    /** The first troop id of a rarity, in id order. */
    static String of(String rarity) { return C.rarity().entrySet().stream().filter(e -> e.getValue().equals(rarity)).map(Map.Entry::getKey).sorted().findFirst().orElseThrow(); }

    public static void main(String[] args) {
        String common = of("common");
        paintPlaces(common);
        samebrushRemoves(common);
        refusesOverCost();
        refusesSecondLegendary();
        refusesOutOfRange(common);
        check(BannerDrop.why("legendary_limit").equals("Only one Legendary troop can stand in a banner."), "The Legendary limit reads as a sentence");
        for (String f : List.of("range", "unknown", "not_owned", "house", "count", "cost", "epic_limit", "rare_limit", "common_limit")) check(!BannerDrop.why(f).isEmpty() && BannerDrop.why(f).endsWith("."), f + " has a sentence");
        System.out.println("BannerDropCheck passed: " + count + " checks");
    }

    static void paintPlaces(String common) {
        SortedMap<Integer, String> t = new TreeMap<>();
        var p = BannerDrop.paint(t, 12, common, ALL);
        check(p.ok() && p.result().equals(Map.of(12, common)) && t.isEmpty(), "A troop is placed on its cell and the table given is left alone");
        check(p.command().equals("cell:0:" + WarGrid.col(12) + ":" + WarGrid.row(12) + ":" + common), "The command names banner, column, row and troop: " + p.command());
        check(p.withBanner(3).command().startsWith("cell:3:"), "withBanner addresses another banner");
        String other = C.rarity().keySet().stream().filter(id -> !id.equals(common) && C.rarityOf(id).equals("common")).sorted().findFirst().orElseThrow();
        var q = BannerDrop.paint(p.result(), 12, other, ALL);
        check(q.ok() && q.result().get(12).equals(other) && q.result().size() == 1, "A different brush replaces the troop on the cell");
    }

    static void samebrushRemoves(String common) {
        SortedMap<Integer, String> t = new TreeMap<>(Map.of(5, common, 6, common));
        var p = BannerDrop.paint(t, 5, common, ALL);
        check(p.ok() && p.result().equals(Map.of(6, common)) && p.command().endsWith(":" + common), "The same brush on its own troop removes it");
        var e = BannerDrop.paint(t, 6, "-", ALL);
        check(e.ok() && e.result().equals(Map.of(5, common)) && e.command().endsWith(":-"), "The brush - removes whatever stands there");
        check(BannerDrop.paint(t, 20, "-", ALL).ok(), "Removing from an empty cell is allowed and changes nothing");
    }

    static void refusesOverCost() {
        String dear = C.rarity().keySet().stream().filter(id -> C.rarityOf(id).equals("common")).max(Comparator.comparingInt(C::cost)).orElseThrow();
        SortedMap<Integer, String> t = new TreeMap<>();
        BannerDrop.Plan last = null;
        for (int i = 0; i < WarGrid.cells(); i++) {
            last = BannerDrop.paint(t, i, dear, ALL);
            if (!last.ok()) break;
            t = last.result();
        }
        check(last != null && !last.ok(), "Painting the dearest Common everywhere is refused before the table is full");
        check(last.why().startsWith("That is more than the banner's") || last.why().startsWith("A banner holds at most"), "The refusal says why: " + last.why());
        check(last.result().equals(t) && RosterMath.validTable(t, C, ALL.owned(), ALL.houses()) == null, "A refusal leaves the table as it was, still valid");
    }

    static void refusesSecondLegendary() {
        String leg = of("legendary");
        String second = C.rarity().keySet().stream().filter(id -> C.rarityOf(id).equals("legendary") && !id.equals(leg)).sorted().findFirst().orElseThrow();
        SortedMap<Integer, String> t = new TreeMap<>(Map.of(0, leg));
        var p = BannerDrop.paint(t, 1, second, ALL);
        check(!p.ok() && p.why().equals("Only one Legendary troop can stand in a banner.") && p.result().equals(t), "A second Legendary is refused in words");
        check(BannerDrop.paint(t, 0, second, ALL).ok(), "A Legendary may replace the Legendary on its own cell");
    }

    static void refusesOutOfRange(String common) {
        SortedMap<Integer, String> t = new TreeMap<>();
        for (int cell : new int[]{-1, WarGrid.cells(), 99}) {
            var p = BannerDrop.paint(t, cell, common, ALL);
            check(!p.ok() && p.why().equals("That cell is not on the war table.") && p.result().isEmpty(), "Cell " + cell + " is refused");
        }
        check(!BannerDrop.paint(t, 0, "no_such_troop", ALL).ok(), "An unknown troop is refused");
        var unowned = new BannerDrop.Rules(C, Set.of(), ALL.houses());
        check(BannerDrop.paint(t, 0, common, unowned).why().equals("You do not own that troop yet."), "An unowned troop is refused in words");
    }
}
