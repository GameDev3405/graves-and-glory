package gg.gravesandglory;

import java.util.*;

/** Pure war table rules: cell maths, the compact cell string, role placement, trimming, the v6 move and validTable. Never shipped. */
public final class WarGridCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    /** A small fixture catalog: id, House, rarity, slot weight and cost. */
    static final String[][] TROOPS = {
        {"knight", "gilded", "common", "1", "20"}, {"archer", "gilded", "common", "1", "20"},
        {"mage", "gilded", "epic", "2", "100"}, {"hexer", "graveborn", "epic", "2", "100"}, {"seer", "wildmoot", "epic", "2", "100"},
        {"bigepic", "gilded", "epic", "2", "330"}, {"bigepic2", "gilded", "epic", "2", "330"},
        {"marshal", "gilded", "legendary", "4", "150"}, {"hollow", "graveborn", "legendary", "4", "150"},
        {"revenant", "graveborn", "common", "1", "20"}};
    static final RosterMath.Catalog cat = fixture();
    static RosterMath.Catalog fixture() {
        Map<String, String> house = new LinkedHashMap<>(), rarity = new LinkedHashMap<>();
        Map<String, Integer> weight = new LinkedHashMap<>(), cost = new LinkedHashMap<>();
        for (String[] t : TROOPS) { house.put(t[0], t[1]); rarity.put(t[0], t[2]); weight.put(t[0], Integer.parseInt(t[3])); cost.put(t[0], Integer.parseInt(t[4])); }
        return new RosterMath.Catalog(house, rarity, weight, cost);
    }
    static final Set<String> ALL = cat.house().keySet();
    static final Set<String> HOUSES = Set.of("gilded", "graveborn", "wildmoot");

    static int rank(String id) { return id.equals("knight") || id.equals("mage") ? 0 : id.equals("archer") ? 2 : 1; }
    static SortedMap<Integer, String> table(Object... cells) {
        SortedMap<Integer, String> t = new TreeMap<>();
        for (int i = 0; i < cells.length; i += 2) t.put((Integer) cells[i], (String) cells[i + 1]);
        return t;
    }
    static String valid(Map<Integer, String> t) { return RosterMath.validTable(t, cat, ALL, HOUSES); }

    static void gridFromSheet() {
        check(WarGrid.cols() == 9 && WarGrid.rows() == 6 && WarGrid.cells() == 54, "the table is 9 x 6 = 54 cells");
        check(RosterMath.allowance() == 640 && RosterMath.maxTroops() == 40 && RosterMath.maxBanners() == 5, "allowance 640, 40 troops, 5 banners");
        check(RosterMath.bannerMax("epic") == 40 && RosterMath.bannerMax("legendary") == 1, "no Epic limit; one Legendary");
    }

    static void parseFormatRoundTrip() {
        check(WarGrid.format(WarGrid.parse("0:knight,10:archer")).equals("0:knight,10:archer"), "parse then format keeps a sorted string");
        check(WarGrid.parse("10:archer,0:knight").toString().equals("{0=knight, 10=archer}"), "parse sorts by index");
        check(WarGrid.parse("x:knight,0:a,0:b").equals(Map.of(0, "a")), "a malformed pair is skipped and a repeated index keeps the first");
        check(WarGrid.parse(null).isEmpty() && WarGrid.parse("").isEmpty() && WarGrid.format(Map.of()).isEmpty(), "empty in, empty out");
    }

    static void denseSparse() {
        var t = table(0, "knight", 13, "archer", 53, "mage");
        var d = WarGrid.dense(t);
        check(d.size() == 54 && d.get(0).equals("knight") && d.get(1).equals(RosterMath.EMPTY) && d.get(53).equals("mage"), "dense has 54 entries, empty where nothing stands");
        check(WarGrid.sparse(d).equals(t), "sparse is the inverse of dense");
        check(WarGrid.sparse(Arrays.asList("a", RosterMath.MARKER, null, RosterMath.EMPTY, "b")).equals(Map.of(0, "a", 4, "b")), "sparse skips empty, marker and null");
    }

    static void indexMath() {
        check(WarGrid.index(4, 1) == 13 && WarGrid.col(13) == 4 && WarGrid.row(13) == 1, "cell 13 is column 4, row 1");
        check(!WarGrid.inRange(54) && !WarGrid.inRange(-1) && WarGrid.inRange(0) && WarGrid.inRange(53), "range is 0 to 53");
    }

    static void costAndTroops() {
        var t = table(0, "knight", 1, "mage", 2, "ghost");
        check(WarGrid.cost(t, cat) == 120 && WarGrid.troops(t, cat) == 2, "cost and troops count known ids only");
    }

    static void placeCentresByRole() {
        var t = WarGrid.place(List.of("archer", "knight", "knight", "archer", "knight"), WarGridCheck::rank);
        check(t.equals(table(4, "knight", 3, "knight", 5, "knight", 13, "archer", 12, "archer")), "3 shield in row 0 centre-out, 2 archers in row 1: " + t);
        var other = WarGrid.place(List.of("revenant", "archer", "knight"), WarGridCheck::rank);
        check(other.equals(table(4, "knight", 13, "revenant", 22, "archer")), "each rank takes its own row: " + other);
        var many = new ArrayList<String>();
        for (int i = 0; i < 60; i++) many.add("revenant");
        check(WarGrid.place(many, WarGridCheck::rank).size() == 54, "troops past the last cell are dropped");
        var ten = new ArrayList<String>();
        for (int i = 0; i < 10; i++) ten.add("revenant");
        var wrapped = WarGrid.place(ten, WarGridCheck::rank);
        check(wrapped.size() == 10 && wrapped.containsKey(8) && wrapped.containsKey(0) && wrapped.containsKey(13) && !wrapped.containsKey(9), "a full row of nine continues in the next row from its centre");
    }

    static void trimFromBack() {
        var t = table(9, "a", 52, "b", 53, "c");
        var seen = new ArrayList<Integer>();
        var out = WarGrid.trim(t, x -> { seen.add(x.size()); return x.size() <= 1; });
        check(out.equals(Map.of(9, "a")) && t.size() == 3, "53 goes before 52 before 9, and the input is unchanged");
        check(WarGrid.trim(t, x -> false).isEmpty() && WarGrid.trim(t, x -> true).equals(t), "trim stops when ok holds or the table is empty");
    }

    static void fromLineMovesAndSkipsMarkers() {
        var line = new ArrayList<String>(Collections.nCopies(20, RosterMath.EMPTY));
        line.set(0, "mage"); line.set(1, RosterMath.MARKER); line.set(2, "knight");
        line.set(5, "marshal"); line.set(6, RosterMath.MARKER); line.set(7, RosterMath.MARKER); line.set(8, RosterMath.MARKER);
        var moved = WarGrid.fromLine(line, cat, t -> true);
        check(moved.trimmed() == 0 && moved.table().equals(table(2, "mage", 4, "knight", 11, "marshal")), "entry i goes to cell (2 + i % 5, i / 5); markers vanish: " + moved.table());
        var trimmed = WarGrid.fromLine(line, cat, t -> t.size() <= 2);
        check(trimmed.trimmed() == 1 && trimmed.table().equals(table(2, "mage", 4, "knight")), "the back row is trimmed first");
    }

    static void validTableOrder() {
        check(valid(table(0, "knight", 4, "archer")) == null, "a small table is valid");
        check("range".equals(valid(table(54, "knight"))) && "range".equals(valid(table(-1, "knight"))), "range");
        check("unknown".equals(valid(table(0, "ghost"))), "unknown");
        check("not_owned".equals(RosterMath.validTable(table(0, "knight"), cat, Set.of("archer"), HOUSES)), "not_owned");
        check("house".equals(RosterMath.validTable(table(0, "revenant"), cat, ALL, Set.of("gilded"))), "house");
        var forty = new TreeMap<Integer, String>();
        for (int i = 0; i < 41; i++) forty.put(i, "knight");
        check("count".equals(valid(forty)), "41 troops is count");
        check("cost".equals(valid(table(0, "bigepic", 1, "bigepic2"))), "660 gold is cost (a count of 2 passes first)");
        check("legendary_limit".equals(valid(table(0, "marshal", 1, "hollow"))), "two Legendaries");
        check(valid(table(0, "mage", 1, "hexer", 2, "seer")) == null, "three Epics are valid");
        check("range".equals(valid(table(99, "ghost"))), "range is checked before unknown");
        check("unknown".equals(RosterMath.validTable(table(0, "ghost"), cat, Set.of(), Set.of())), "unknown is checked before not_owned and house");
    }

    static void starterValid() {
        List<String> ordered = new ArrayList<>();
        for (int i = 0; i < 12; i++) ordered.add("knight");
        for (int i = 0; i < 8; i++) ordered.add("archer");
        var starter = WarGrid.place(ordered, WarGridCheck::rank);
        check(starter.size() == 20, "the starter places 20 troops");
        var real = RosterMath.Catalog.fromSpec();
        check(RosterMath.validTable(starter, real, Set.of("knight", "archer"), RosterMath.fieldableHouses()) == null, "the starter table is valid with Commons owned");
    }

    static void botBannersValid() {
        // TODO Task 6: bot banners become plain ordered lists placed by WarGrid.botTable; until then the old 20-entry rows hold markers.
        System.out.println("SKIP botBannersValid (Task 6)");
    }

    public static void main(String[] args) {
        gridFromSheet();
        parseFormatRoundTrip();
        denseSparse();
        indexMath();
        costAndTroops();
        placeCentresByRole();
        trimFromBack();
        fromLineMovesAndSkipsMarkers();
        validTableOrder();
        starterValid();
        botBannersValid();
        System.out.println("WarGridCheck ok: " + checks + " checks");
    }
}
