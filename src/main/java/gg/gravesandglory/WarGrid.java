package gg.gravesandglory;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * The war table grid (army rework design 4.1): the cells a banner lays its troops on. Row 0 is the front; cell index is
 * row * cols + col. A banner is kept as a sparse table (cell to troop id), saved as a compact string and, in a battle, as a dense
 * list with RosterMath.EMPTY where nothing stands. Pure, no Minecraft types, so the standalone WarGridCheck compiles it. The grid
 * size comes from the rules sheet. (The client's War Table chrome is the separate class WarTable.)
 */
public final class WarGrid {
    private WarGrid() {}

    /** Columns of the war table (rules.bannerCols). */
    public static int cols() { return Spec.RULES[0].bannerCols(); }
    /** Rows of the war table (rules.bannerRows). */
    public static int rows() { return Spec.RULES[0].bannerRows(); }
    /** Cells of the war table: columns times rows. */
    public static int cells() { return cols() * rows(); }
    /** The cell index of a column and row. */
    public static int index(int col, int row) { return row * cols() + col; }
    /** The column of a cell index. */
    public static int col(int i) { return i % cols(); }
    /** The row of a cell index (0 is the front). */
    public static int row(int i) { return i / cols(); }
    /** True when the index is a cell of the table. */
    public static boolean inRange(int i) { return i >= 0 && i < cells(); }

    /**
     * Reads the compact cell string "index:id,index:id" into a table sorted by index. A malformed pair is skipped and a repeated
     * index keeps its first troop. A null or blank string is an empty table. Indices are not range-checked here.
     */
    public static SortedMap<Integer, String> parse(String cells) {
        SortedMap<Integer, String> out = new TreeMap<>();
        if (cells == null || cells.isBlank()) return out;
        for (String pair : cells.split(",")) {
            int colon = pair.indexOf(':');
            if (colon <= 0) continue;
            String id = pair.substring(colon + 1).trim();
            if (id.isEmpty()) continue;
            try { out.putIfAbsent(Integer.parseInt(pair.substring(0, colon).trim()), id); } catch (NumberFormatException e) { /* skipped */ }
        }
        return out;
    }

    /** The compact cell string of a table, sorted by index; "" for an empty table. The inverse of parse. */
    public static String format(Map<Integer, String> table) {
        StringBuilder sb = new StringBuilder();
        new TreeMap<>(table).forEach((i, id) -> sb.append(sb.length() == 0 ? "" : ",").append(i).append(':').append(id));
        return sb.toString();
    }

    /** The table as a list of cells() entries, RosterMath.EMPTY where nothing stands; an out-of-range cell is left out. */
    public static List<String> dense(Map<Integer, String> table) {
        List<String> out = new ArrayList<>(Collections.nCopies(cells(), RosterMath.EMPTY));
        table.forEach((i, id) -> { if (inRange(i)) out.set(i, id); });
        return out;
    }

    /** The table of a dense list: every entry except RosterMath.EMPTY, RosterMath.MARKER and null, by index. The inverse of dense. */
    public static SortedMap<Integer, String> sparse(List<String> dense) {
        SortedMap<Integer, String> out = new TreeMap<>();
        if (dense != null) for (int i = 0; i < dense.size(); i++) {
            String id = dense.get(i);
            if (id != null && !id.equals(RosterMath.EMPTY) && !id.equals(RosterMath.MARKER)) out.put(i, id);
        }
        return out;
    }

    /** The muster cost of a table: the troops.cost of each cell, an unknown id counting 0. */
    public static int cost(Map<Integer, String> table, RosterMath.Catalog c) {
        int sum = 0;
        for (String id : table.values()) sum += c.cost(id);
        return sum;
    }

    /** The troops in a table: cells holding an id the catalog knows. */
    public static int troops(Map<Integer, String> table, RosterMath.Catalog c) {
        int n = 0;
        for (String id : table.values()) if (c.known(id)) n++;
        return n;
    }

    /** The columns of a row in fill order, centre first and then outwards, left before right (4,3,5,2,6,1,7,0,8 at 9 columns). */
    static List<Integer> centreOut() {
        int cols = cols(), mid = (cols - 1) / 2;
        List<Integer> out = new ArrayList<>();
        out.add(mid);
        for (int d = 1; out.size() < cols; d++) {
            if (mid - d >= 0) out.add(mid - d);
            if (mid + d < cols) out.add(mid + d);
        }
        return out;
    }

    /**
     * Places troops on the table by role. rank gives 0 for a shield troop, 1 for other melee and 2 for ranged or magic. The list is
     * sorted stably by rank; rows fill from row 0, each row centre-out; a new rank starts a new row. Troops past the last cell are
     * dropped.
     */
    public static SortedMap<Integer, String> place(List<String> ordered, ToIntFunction<String> rank) {
        List<String> sorted = new ArrayList<>(ordered);
        sorted.sort(Comparator.comparingInt(rank::applyAsInt));
        SortedMap<Integer, String> out = new TreeMap<>();
        List<Integer> order = centreOut();
        int row = 0, used = 0, last = Integer.MIN_VALUE;
        for (String id : sorted) {
            int r = rank.applyAsInt(id);
            if (last != Integer.MIN_VALUE && r != last && used > 0) { row++; used = 0; }
            if (used == order.size()) { row++; used = 0; }
            last = r;
            if (row >= rows()) break;
            out.put(index(order.get(used++), row), id);
        }
        return out;
    }

    /**
     * Removes the highest-index troop (the last row, from its right end backwards) until ok accepts the table or it is empty. The
     * input is not changed.
     */
    public static SortedMap<Integer, String> trim(SortedMap<Integer, String> table, Predicate<SortedMap<Integer, String>> ok) {
        SortedMap<Integer, String> out = new TreeMap<>(table);
        while (!out.isEmpty() && !ok.test(out)) out.remove(out.lastKey());
        return out;
    }

    /** A table moved from the old banner, and the troops that had to be trimmed off it. */
    public record Moved(SortedMap<Integer, String> table, int trimmed) {}

    /**
     * Moves an old 20-entry banner onto the war table: entry i goes to cell (2 + i % 5, i / 5) (the old 5 by 4 block, centred).
     * Markers and empty entries are skipped, so an Epic or Legendary lands once, on its first entry. The result is trimmed from the
     * back until ok accepts it; trimmed counts what was taken off.
     */
    public static Moved fromLine(List<String> line20, RosterMath.Catalog c, Predicate<SortedMap<Integer, String>> ok) {
        SortedMap<Integer, String> moved = new TreeMap<>();
        if (line20 != null) for (int i = 0; i < line20.size(); i++) {
            String id = line20.get(i);
            if (id == null || id.equals(RosterMath.MARKER) || id.equals(RosterMath.EMPTY)) continue;
            moved.put(index(2 + i % 5, i / 5), id);
        }
        SortedMap<Integer, String> kept = trim(moved, ok);
        return new Moved(kept, moved.size() - kept.size());
    }
}
