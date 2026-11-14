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
        check(BannerDrop.why("legendary_limit").equals("1 Legendary per banner"), "The Legendary limit reads short: " + BannerDrop.why("legendary_limit"));
        check(BannerDrop.why("house").equals("This House is not playable yet.") && BannerDrop.why("count").equals(RosterMath.maxTroops() + " troops max"), "The House and count refusals read short");
        check(BannerDrop.costWhy(22, 10).equals("Need 22 gold, 10 left"), "The cost reason names the price and the gold left");
        for (String f : List.of("range", "unknown", "not_owned", "house", "count", "cost", "epic_limit", "rare_limit", "common_limit")) check(!BannerDrop.why(f).isEmpty() && (f.equals("house") || !BannerDrop.why(f).endsWith(".")) && !BannerDrop.why(f).toLowerCase().contains("war table"), f + " is a short reason: " + BannerDrop.why(f));
        fastTools(common);
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
        check(p.ok() && p.result().equals(Map.of(6, common)) && p.command().endsWith(":-"), "The same brush on its own troop removes it, and sends the erase id so it is not the place command twice");
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
        check(last.why().startsWith("Need ") && last.why().contains(" gold, ") && last.why().endsWith(" left") || last.why().endsWith(" troops max"), "The refusal says why: " + last.why());
        check(last.result().equals(t) && RosterMath.validTable(t, C, ALL.owned(), ALL.houses()) == null, "A refusal leaves the table as it was, still valid");
    }

    static void refusesSecondLegendary() {
        String leg = of("legendary");
        String second = C.rarity().keySet().stream().filter(id -> C.rarityOf(id).equals("legendary") && !id.equals(leg)).sorted().findFirst().orElseThrow();
        SortedMap<Integer, String> t = new TreeMap<>(Map.of(0, leg));
        var p = BannerDrop.paint(t, 1, second, ALL);
        check(!p.ok() && p.why().equals("1 Legendary per banner") && p.result().equals(t), "A second Legendary is refused in words");
        check(BannerDrop.paint(t, 0, second, ALL).ok(), "A Legendary may replace the Legendary on its own cell");
    }

    static void refusesOutOfRange(String common) {
        SortedMap<Integer, String> t = new TreeMap<>();
        for (int cell : new int[]{-1, WarGrid.cells(), 99}) {
            var p = BannerDrop.paint(t, cell, common, ALL);
            check(!p.ok() && p.why().equals("That cell is not on the table") && p.result().isEmpty(), "Cell " + cell + " is refused");
        }
        check(!BannerDrop.paint(t, 0, "no_such_troop", ALL).ok(), "An unknown troop is refused");
        var unowned = new BannerDrop.Rules(C, Set.of(), ALL.houses());
        check(BannerDrop.paint(t, 0, common, unowned).why().equals("You do not own that troop yet"), "An unowned troop is refused in words");
    }

    /** Auto-arrange, Clear and Move (items 8 of the War Table review): pure plans, the table given is never changed. */
    static void fastTools(String common) {
        String shield = C.rarity().keySet().stream().filter(id -> C.rarityOf(id).equals("common") && WarGrid.role(id) == 0).sorted().findFirst().orElseThrow();
        String melee = C.rarity().keySet().stream().filter(id -> C.rarityOf(id).equals("common") && WarGrid.role(id) == 1).sorted().findFirst().orElseThrow();
        String ranged = C.rarity().keySet().stream().filter(id -> C.rarityOf(id).equals("common") && WarGrid.role(id) == 2).sorted().findFirst().orElseThrow();
        // arrange: scattered back to front, set out shields, melee, ranged, each row centred
        SortedMap<Integer, String> mess = new TreeMap<>(Map.of(0, ranged, 8, melee, 30, shield, 53, shield));
        String before = mess.toString();
        var a = BannerDrop.arrange(mess, ALL);
        check(a.ok() && a.command().equals("banner:arrange:0") && mess.toString().equals(before), "Auto-arrange is a plan and leaves the table given alone");
        int mid = WarGrid.centreOut().get(0), second = WarGrid.centreOut().get(1);
        check(a.result().equals(new TreeMap<>(Map.of(WarGrid.index(mid, 0), shield, WarGrid.index(second, 0), shield, WarGrid.index(mid, 1), melee, WarGrid.index(mid, 2), ranged))), "Shields in front, then melee, then ranged, centred: " + a.result());
        check(!BannerDrop.arrange(a.result(), ALL).ok() && BannerDrop.arrange(a.result(), ALL).why().equals("Already in order"), "An arranged banner is refused as already in order");
        check(!BannerDrop.arrange(new TreeMap<>(), ALL).ok(), "An empty banner has nothing to arrange");
        check(a.withBanner(2).command().equals("banner:arrange:2"), "withBanner addresses arrange: " + a.withBanner(2).command());
        // arrange never loses a troop, even when every rank would want its own rows
        SortedMap<Integer, String> full = new TreeMap<>();
        List<String> kinds = List.of(shield, melee, ranged);
        for (int i = 0; i < RosterMath.maxTroops(); i++) full.put(i, kinds.get(i % 3));
        var f = BannerDrop.arrange(full, ALL);
        check(f.result().size() == full.size() && new TreeMap<>(f.result()).values().stream().sorted().toList().equals(full.values().stream().sorted().toList()), "Auto-arrange keeps every troop of a full banner");
        int lastShield = -1, firstRanged = Integer.MAX_VALUE;
        for (var e : f.result().entrySet()) { if (WarGrid.role(e.getValue()) == 0) lastShield = Math.max(lastShield, WarGrid.row(e.getKey())); if (WarGrid.role(e.getValue()) == 2) firstRanged = Math.min(firstRanged, WarGrid.row(e.getKey())); }
        check(lastShield <= firstRanged, "Shields stand no further back than ranged troops");
        // clear
        var c = BannerDrop.clear(mess, ALL);
        check(c.ok() && c.result().isEmpty() && c.command().equals("banner:clear:0") && mess.toString().equals(before), "Clear empties the banner and leaves the table given alone");
        check(!BannerDrop.clear(new TreeMap<>(), ALL).ok() && c.withBanner(4).command().equals("banner:clear:4"), "Clear on an empty banner is refused; withBanner addresses it");
        // move
        SortedMap<Integer, String> t = new TreeMap<>(Map.of(10, shield, 11, melee));
        var m = BannerDrop.move(t, 10, 20, ALL);
        check(m.ok() && m.result().equals(Map.of(20, shield, 11, melee)) && m.command().equals("banner:move:0:10:20") && t.size() == 2 && t.get(10).equals(shield), "Move takes the troop to an empty cell");
        var sw = BannerDrop.move(t, 10, 11, ALL);
        check(sw.ok() && sw.result().equals(Map.of(10, melee, 11, shield)), "Moving onto a troop swaps the two");
        check(!BannerDrop.move(t, 30, 31, ALL).ok() && !BannerDrop.move(t, 10, 10, ALL).ok() && !BannerDrop.move(t, 10, WarGrid.cells(), ALL).ok() && !BannerDrop.move(t, -1, 3, ALL).ok(), "Empty source, same cell and off-table cells are refused");
        check(m.withBanner(1).command().equals("banner:move:1:10:20"), "withBanner addresses move: " + m.withBanner(1).command());
        check(BannerDrop.paint(t, 12, common, ALL).withBanner(3).command().startsWith("cell:3:"), "withBanner still addresses cell strokes");
    }
}
