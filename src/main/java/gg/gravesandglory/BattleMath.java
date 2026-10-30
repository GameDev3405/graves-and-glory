package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pure battle arithmetic shared by the server, the HUD and standalone checks. No Minecraft types. */
public final class BattleMath {
    private BattleMath() {}
    /** Ticks a shot spends in the air: distance over speed, rounded, clamped to [minTicks,maxTicks]. */
    public static int travelTicks(double distance, double blocksPerTick, int minTicks, int maxTicks) {
        long ticks = Math.round(distance / blocksPerTick);
        return (int) Math.max(minTicks, Math.min(maxTicks, ticks));
    }
    /** Whether a blow struck at lastHitAt can still credit a death at now: no more than window ticks ago, and not in the future. */
    public static boolean creditWindowOpen(long now, long lastHitAt, int window) {
        long age = now - lastHitAt;
        return age >= 0 && age <= window;
    }
    /** A group took a loss when a member died or its average health fell by at least 0.008 (one 4-damage hit on a full 12-knight group). */
    public static boolean tookLoss(float prevHealth, int prevCount, float health, int count) {
        return count < prevCount || prevHealth - health >= 0.008f - 1e-6f;
    }
    /** A group's fighting strength: living health over the health it started the round with, clamped to [0,1]. */
    public static double strength(double livingHealth, double fullHealth) {
        if (fullHealth <= 0) return 1;
        return Math.max(0, Math.min(1, livingHealth / fullHealth));
    }
    /** Degrees (0..180) between Minecraft's forward vector for a yaw, (-sin yaw, cos yaw), and the offset (dx,dz). */
    public static double relativeBearing(double facingYawDeg, double dx, double dz) {
        double length = Math.hypot(dx, dz);
        if (length < 1e-9) return 0;
        double yaw = Math.toRadians(facingYawDeg);
        double cos = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / length;
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }
    /**
     * The attribute multiplier that makes a troop travel x travel ("+x% speed" on a sheet: 1 + x): the modifier value for a MULTIPLY_TOTAL on
     * the movement speed. A swimmer's stroke is linear in the attribute, so in water it is travel - 1 as it stands; a land mob's walk is
     * quadratic in it (the attribute sets the forward input, and the ground turns that input into an acceleration by the attribute again:
     * measured, a +15% attribute walks 1.32 as far), so on land it is the square root of travel, less 1. Riptide, Muster and the Howl share it.
     */
    public static double speedModifier(double travel, boolean swimming) {
        return (swimming ? travel : Math.sqrt(Math.max(0, travel))) - 1;
    }
    /** Whether a troop is wounded for a sheet rule that reads "at or below share of its max health" (the fearless stance, Run them down): health at or below share x max, with a hair of tolerance for float health. */
    public static boolean wounded(double health, double maxHealth, double share) {
        return maxHealth > 0 && health <= share * maxHealth + 1e-6;
    }
    /** The room a body of this width takes in a formation and on a bridge: its width and 0.3, at least 1.3. A formation's cell is the widest such room in its group, at least the formation's own spacing; the server (Battle.formation) and the command preview (CommandMode) both read it. */
    public static double spacing(double width) { return Math.max(1.3, width + 0.3); }
    /** A shield troop's cell on Hold (animation rework §7.2): the shield-wall cell (combatDiscipline.shieldWallCell), or its body's width and 0.1 when that is more, so neighbouring collision boxes keep a gap and never push each other apart. */
    public static double shieldCell(double width, double wallCell) { return Math.max(wallCell, width + 0.1); }
    /** The cell of each unit of a group on Hold: a shield unit its shieldCell, any other the formation's cell or its body's room (spacing) when that is more. */
    public static double[] holdSlots(boolean[] shield, double[] width, double formationCell, double wallCell) {
        double[] cells = new double[shield.length];
        for (int i = 0; i < cells.length; i++) cells[i] = shield[i] ? shieldCell(width[i], wallCell) : Math.max(formationCell, spacing(width[i]));
        return cells;
    }
    /** How many ranks this many places take in a formation's shape: rows of `columns`, or the wedge's ranks of 1, 3, 5... */
    public static int ranks(int count, int columns, boolean wedge) {
        if (count <= 0) return 0;
        return wedge ? (int) Math.floor(Math.sqrt(count - 1)) + 1 : (count - 1) / columns + 1;
    }
    /**
     * A group's places (Battle.formation, and the command preview in CommandMode, which must agree): {side, back} for each unit, in the order
     * given (the server gives slot order), before the group's role depth. `shield` marks the units that close into the wall (shield troops on Hold;
     * all false on any other order). The shield units come first, in the front ranks of the formation's shape, at the widest of their cells; the
     * others take the ranks behind them at the widest of theirs. With no shield unit this is the old layout: one cell, the formation's spacing or
     * the widest body's room. The places depend only on how many units of each kind there are and their cells, not on their order.
     * With a wall (some shield unit, not the wedge) a partial rank, of the wall or of the rest, stands centred on the formation's columns, filled
     * from the middle out (centreOrder): so a rank one man short is the rank with its outermost place gone, and a wall that loses men stays
     * centred within half a cell. With no shield unit the partial rank stands at the left, as it always has.
     */
    public static double[][] holdLayout(boolean[] shield, double[] width, double formationCell, double wallCell, int columns, boolean wedge) {
        double[] cells = holdSlots(shield, width, formationCell, wallCell);
        int shields = 0; double front = 0, rear = formationCell;
        for (int i = 0; i < cells.length; i++) if (shield[i]) { shields++; front = Math.max(front, cells[i]); } else rear = Math.max(rear, cells[i]);
        double depth = ranks(shields, columns, wedge) * front;
        double[][] grid = holdGrid(shield, columns, wedge), places = new double[cells.length][];
        for (int i = 0; i < cells.length; i++) {
            double cell = shield[i] ? front : rear, back = (grid[i][1] + 1) * cell;
            places[i] = new double[]{grid[i][0] * cell, shield[i] ? back : depth + back};
        }
        return places;
    }
    /**
     * holdLayout's places in cells, not blocks: {side, rank} for each unit, side in cells from the middle of the formation, rank from 0 at the
     * front of its kind's ranks (the wall's, or the rest's behind it). They do not depend on the cells, so a wall whose cell changes (its widest
     * man fell) keeps every man's file and rank (Battle.heldPlace hands keepPlaces these).
     */
    public static double[][] holdGrid(boolean[] shield, int columns, boolean wedge) {
        int shields = 0;
        for (boolean wall : shield) if (wall) shields++;
        boolean centre = shields > 0 && !wedge;
        double[][] walls = kindGrid(shields, columns, wedge, centre), rest = kindGrid(shield.length - shields, columns, wedge, centre), grid = new double[shield.length][];
        for (int i = 0, s = 0, o = 0; i < shield.length; i++) grid[i] = shield[i] ? walls[s++] : rest[o++];
        return grid;
    }
    /** The places of one kind (the wall's, or the rest's) of holdGrid, for this many units of it: rows of `columns` from rank 0, the partial rank centred when `centre` (a wall, not the wedge), or the wedge's ranks. */
    static double[][] kindGrid(int count, int columns, boolean wedge, boolean centre) {
        double[][] grid = new double[Math.max(count, 0)][]; int[] order = centreOrder(columns);
        for (int index = 0; index < grid.length; index++) {
            int inRank = Math.min(columns, count - index / columns * columns);
            int column = centre && inRank < columns ? order[index % columns] : index % columns;
            double side = column - (columns - 1) / 2.0; int rank = index / columns;
            if (wedge) { rank = (int) Math.floor(Math.sqrt(index)); side = index - rank * rank - rank; }
            grid[index] = new double[]{side, rank};
        }
        return grid;
    }
    /** The columns of a rank in the order a partial rank fills them: the middle first, then out to either side alternately (the right of a tie first). */
    public static int[] centreOrder(int columns) {
        Integer[] c = new Integer[Math.max(columns, 0)];
        for (int i = 0; i < c.length; i++) c[i] = i;
        Arrays.sort(c, (a, b) -> { int d = Integer.compare(Math.abs(2 * a - (columns - 1)), Math.abs(2 * b - (columns - 1))); return d != 0 ? d : Integer.compare(b, a); });
        int[] out = new int[c.length];
        for (int i = 0; i < c.length; i++) out[i] = c[i];
        return out;
    }
    /**
     * Which of a held group's places each unit takes (Battle.heldPlace): an index into holdGrid(shield, columns, wedge), one place per unit, place
     * j of the kind of unit j. prev[i] is unit i's holdGrid place the last time (null: none, a new unit); a change of cell keeps every file and rank.
     * A wall that loses a man closes the gap as a wall closes ranks: every unit whose place is still in the layout keeps it; a place left empty,
     * front first, takes the unit directly behind it in its file (of its kind), whose own place is then empty in turn; with none behind it, the
     * rank slides one place toward the man who has to come in (the unit whose place is the one the smaller layout drops; with none, the nearest
     * of the rearmost rank): the neighbour on that side steps into the gap, and so on until that man steps into the last. So every man moves at
     * most one place sideways or one rank up, nobody passes anybody, and the men beyond the sliding stretch stand (ruling R17).
     * Several places left empty at once (an area blow, or losses while the group was on another order and kept its cached places) are filled one
     * at a time, each a single loss against the result of the last: the layout the men stood in is found (the smallest that holds every kept
     * place), and while it is larger than the group, the empty place nearest the place the next smaller layout drops is the one that goes, and
     * the rest close up as for one loss. A man moves at most one place per loss. New units then take the places left, as for one loss.
     * When no unit stands on a place of the new layout (a new shape: Battle.heldPlace hands no last places) the units take the places in order.
     */
    public static int[] keepPlaces(double[][] prev, boolean[] shield, int columns, boolean wedge) {
        int n = shield.length, shields = 0; double[][] places = holdGrid(shield, columns, wedge); int[] at = new int[n]; boolean any = false;
        for (boolean wall : shield) if (wall) shields++;
        for (int i = 0; i < n && !any; i++) if (prev[i] != null) for (int j = 0; j < n; j++) if (shield[j] == shield[i] && same(places[j], prev[i])) { any = true; break; }
        if (!any) { for (int i = 0; i < n; i++) at[i] = i; return at; }
        for (boolean kind : new boolean[]{true, false}) {
            List<Integer> mine = new ArrayList<>();
            for (int i = 0; i < n; i++) if (shield[i] == kind) mine.add(i);
            double[][] last = new double[mine.size()][];
            for (int k = 0; k < last.length; k++) last[k] = prev[mine.get(k)];
            int[] got = closeUp(last, columns, wedge, shields > 0 && !wedge);
            for (int k = 0; k < got.length; k++) at[mine.get(k)] = mine.get(got[k]);
        }
        return at;
    }
    /** keepPlaces for the units of one kind: last[k] is unit k's place the last time (null: new); an index into kindGrid(last.length) for each. */
    private static int[] closeUp(double[][] last, int columns, boolean wedge, boolean centre) {
        int m = last.length, known = 0;
        for (double[] p : last) if (p != null) known++;
        double[][] now = kindGrid(m, columns, wedge, centre);
        int old = -1;
        for (int c = Math.max(known, 1); known > 0 && c <= known + 64 && old < 0; c++) if (holds(kindGrid(c, columns, wedge, centre), last)) old = c;
        if (old <= m) return slide(last, now);
        // the layout they stood in: its places nobody stands on are the places left empty
        double[][] stood = kindGrid(old, columns, wedge, centre); List<double[]> empty = new ArrayList<>();
        for (double[] place : stood) { boolean held = false; for (double[] p : last) held |= p != null && same(p, place); if (!held) empty.add(place); }
        double[][] here = last.clone();
        for (int c = old; c > m; c--) {
            double[][] smaller = kindGrid(c - 1, columns, wedge, centre); double[] dropped = null;
            for (double[] place : kindGrid(c, columns, wedge, centre)) { boolean kept = false; for (double[] q : smaller) kept |= same(q, place); if (!kept) dropped = place; }
            // the empty place nearest the dropped one goes (front first, then the left, on a tie); the rest close up as for one loss
            int gone = 0;
            for (int e = 1; e < empty.size(); e++) {
                double[] a = empty.get(e), b = empty.get(gone); double da = Math.hypot(a[0] - dropped[0], a[1] - dropped[1]), db = Math.hypot(b[0] - dropped[0], b[1] - dropped[1]);
                if (da < db - EPS || Math.abs(da - db) <= EPS && (a[1] < b[1] - EPS || Math.abs(a[1] - b[1]) <= EPS && a[0] < b[0])) gone = e;
            }
            empty.remove(gone);
            // the men and the places still empty, as one loss: the empty places close up with the men, so they stay where the slide leaves a gap
            List<Integer> men = new ArrayList<>();
            for (int k = 0; k < m; k++) if (here[k] != null) men.add(k);
            double[][] before = new double[men.size() + empty.size()][];
            for (int k = 0; k < men.size(); k++) before[k] = here[men.get(k)];
            for (int e = 0; e < empty.size(); e++) before[men.size() + e] = empty.get(e);
            int[] after = slide(before, smaller);
            for (int k = 0; k < men.size(); k++) here[men.get(k)] = smaller[after[k]];
            for (int e = 0; e < empty.size(); e++) empty.set(e, smaller[after[men.size() + e]]);
        }
        return slide(here, now);
    }
    /** Whether these places hold every kept place (null: none), each on its own. */
    private static boolean holds(double[][] places, double[][] last) {
        boolean[] used = new boolean[places.length];
        for (double[] p : last) {
            if (p == null) continue;
            int found = -1;
            for (int j = 0; j < places.length && found < 0; j++) if (!used[j] && same(places[j], p)) found = j;
            if (found < 0) return false;
            used[found] = true;
        }
        return true;
    }
    /** The single-loss slide over the places of one kind (keepPlaces): last[i] is unit i's place before (null: new), an index into places for each. */
    private static int[] slide(double[][] prev, double[][] places) {
        boolean[] shield = new boolean[places.length]; Arrays.fill(shield, true);
        int n = places.length; int[] at = new int[n]; boolean[] taken = new boolean[n]; boolean any = false;
        Arrays.fill(at, -1);
        for (int i = 0; i < n; i++) {
            if (prev[i] == null) continue;
            for (int j = 0; j < n; j++) if (!taken[j] && shield[j] == shield[i] && same(places[j], prev[i])) { at[i] = j; taken[j] = true; any = true; break; }
        }
        if (!any) { for (int i = 0; i < n; i++) at[i] = i; return at; }
        for (int guard = 0; guard < 4 * n * n + n; guard++) {
            int h = -1;
            for (int j = 0; j < n; j++) if (!taken[j] && (h < 0 || places[j][1] < places[h][1] - EPS || Math.abs(places[j][1] - places[h][1]) < EPS && places[j][0] < places[h][0])) h = j;
            if (h < 0) break;
            boolean kind = shield[h]; double[] hole = places[h]; int pick = -1; double best = Double.MAX_VALUE;
            // directly behind it in its file: the nearest of its kind standing (or, without a place now, last standing) further back at its side
            for (int i = 0; i < n; i++) {
                if (shield[i] != kind) continue;
                double[] p = at[i] >= 0 ? places[at[i]] : prev[i];
                if (p != null && Math.abs(p[0] - hole[0]) < EPS && p[1] > hole[1] + EPS && p[1] < best) { best = p[1]; pick = i; }
            }
            if (pick < 0) {
                // the man who has to come in: a unit without a place, the nearest by where it was (a newcomer, with none, after them)
                int donor = -1;
                for (int i = 0; i < n; i++) {
                    if (shield[i] != kind || at[i] >= 0) continue;
                    double d = prev[i] == null ? Double.MAX_VALUE / 2 : Math.hypot(prev[i][0] - hole[0], prev[i][1] - hole[1]);
                    if (d < best) { best = d; donor = i; }
                }
                // with none, the nearest of the rearmost rank of its kind, unless the empty place is in that rank
                if (donor < 0) {
                    double rearmost = -Double.MAX_VALUE;
                    for (int j = 0; j < n; j++) if (shield[j] == kind) rearmost = Math.max(rearmost, places[j][1]);
                    if (hole[1] < rearmost - EPS) for (int i = 0; i < n; i++) {
                        if (shield[i] != kind || at[i] < 0 || Math.abs(places[at[i]][1] - rearmost) > EPS) continue;
                        double d = Math.abs(places[at[i]][0] - hole[0]);
                        if (d < best) { best = d; donor = i; }
                    }
                }
                if (donor < 0) break; // cannot happen: each kind has a place for each of its units
                // the rank slides toward him: its man nearest the gap on his side, between the gap and him, steps in; with none, he does
                pick = donor; double[] from = at[donor] >= 0 ? places[at[donor]] : prev[donor];
                if (from != null) {
                    // a donor in another rank (the rear man who comes up) brings in the man of the gap's rank in his own file too: that man
                    // slides sideways and the donor steps up behind him, rather than the donor stepping across him on the diagonal
                    double reach = Math.abs(from[1] - hole[1]) > EPS ? -EPS : EPS, nearest = Double.MAX_VALUE;
                    double lo = hole[0] < from[0] ? hole[0] + EPS : from[0] + reach, hi = hole[0] < from[0] ? from[0] - reach : hole[0] - EPS;
                    for (int i = 0; i < n; i++) {
                        if (i == donor || shield[i] != kind || at[i] < 0) continue;
                        double[] q = places[at[i]];
                        if (Math.abs(q[1] - hole[1]) < EPS && q[0] > lo && q[0] < hi && Math.abs(q[0] - hole[0]) < nearest) { nearest = Math.abs(q[0] - hole[0]); pick = i; }
                    }
                }
            }
            if (at[pick] >= 0) taken[at[pick]] = false;
            at[pick] = h; taken[h] = true;
        }
        // a unit still without a place (a guard, never expected): the first free place
        for (int i = 0; i < n; i++) if (at[i] < 0) for (int j = 0; j < n; j++) if (!taken[j]) { at[i] = j; taken[j] = true; break; }
        return at;
    }
    private static final double EPS = 1e-6;
    private static boolean same(double[] a, double[] b) { return Math.abs(a[0] - b[0]) < EPS && Math.abs(a[1] - b[1]) < EPS; }
    /**
     * How far behind the first deploy row each row of five stands (Battle.reinforce): rows 3 apart, as always, but two rows of shield troops
     * (wall[r]: the row has troops and every one of them carries a shield) stand their cell apart (cell[r], the row's widest shield cell), so an
     * uncommanded shield group deploys as one deep wall.
     */
    public static double[] deployDepths(boolean[] wall, double[] cell, double rowGap) {
        double[] depth = new double[wall.length];
        for (int r = 1; r < wall.length; r++) depth[r] = depth[r - 1] + (wall[r - 1] && wall[r] ? Math.max(cell[r - 1], cell[r]) : rowGap);
        return depth;
    }
    /**
     * A held group's places (Battle.heldPlace): its members (slot order), the shape (formation, columns, wedge) and layout they stood in, and
     * the place each took, in cells (holdGrid) and in blocks (the layout).
     */
    public static final class Held<T> {
        public final List<T> members; public final String shape; public final double[][] layout;
        public final Map<T, double[]> grid = new HashMap<>(), at = new HashMap<>();
        Held(List<T> members, String shape, double[][] layout) { this.members = members; this.shape = shape; this.layout = layout; }
    }
    /**
     * The places of a group on Hold, kept through losses (Battle.heldPlace): the same members, shape and layout keep `kept`. Any change (a man
     * falls or joins, the wall's cell changes when its widest man fell, the layout changes) rebuilds them: on the same shape each member's last
     * place in cells goes to keepPlaces, so the wall closes as a wall closes ranks and a new cell only changes the spacing; a new shape takes the
     * places in slot order. A Hold given again after another order, with the same members and shape, keeps the cached places; losses during the
     * other order arrive as several empty places at once, which keepPlaces fills one at a time.
     */
    public static <T> Held<T> held(Held<T> kept, List<T> members, String shape, double[][] layout, boolean[] shield, int columns, boolean wedge) {
        if (kept != null && kept.members.equals(members) && kept.shape.equals(shape) && Arrays.deepEquals(kept.layout, layout)) return kept;
        double[][] prev = new double[members.size()][];
        if (kept != null && kept.shape.equals(shape)) for (int i = 0; i < prev.length; i++) prev[i] = kept.grid.get(members.get(i));
        int[] at = keepPlaces(prev, shield, columns, wedge); double[][] grid = holdGrid(shield, columns, wedge);
        Held<T> next = new Held<>(List.copyOf(members), shape, layout);
        for (int i = 0; i < at.length; i++) { next.grid.put(members.get(i), grid[at[i]]); next.at.put(members.get(i), layout[at[i]]); }
        return next;
    }
    /** A troop's cell in the deploy ring (Battle.deploy): a shield troop's wall cell (shieldCell), any other 2, the old ring's, or its width and 0.1 when that is more. */
    public static double deployCell(boolean shield, double width, double wallCell) { return shield ? shieldCell(width, wallCell) : Math.max(2, width + 0.1); }
    /**
     * Where each slot of a squad stands in its deploy ring (Battle.deploy: trained troops at the barracks or a forward anchor), {x, z} from the
     * ring's centre: rows of five, as the reinforce rows (deployRow, deployDepths). cell[i] is slot i's cell (deployCell; 0 or less: an empty
     * slot), shield[i] whether it carries a shield. In a row neighbours stand half of each one's cell apart; two rows of nothing but shield troops
     * stand their wall cell apart, one deep wall (a wall row's empty places take its cell); any other two rows stand `gap` apart, or half of
     * the two rows' widest cells when that is more. So no two troops of any mix stand closer than their cells allow. At least three rows, centred
     * on the middle of the third (slot 12): with every cell `gap` this is the old ring, (slot % 5 - 2, slot / 5 - 2) x gap.
     */
    public static double[][] deployRing(boolean[] shield, double[] cell, double gap) {
        int size = Math.max(15, (shield.length + 4) / 5 * 5), rows = size / 5;
        boolean[] wall = new boolean[rows], troops = new boolean[rows]; double[] rowCell = new double[rows], widest = new double[rows], cells = new double[size], depth = new double[rows];
        Arrays.fill(wall, true);
        for (int i = 0; i < shield.length; i++) {
            if (cell[i] <= 0) continue;
            troops[i / 5] = true;
            if (shield[i]) rowCell[i / 5] = Math.max(rowCell[i / 5], cell[i]); else wall[i / 5] = false;
        }
        for (int r = 0; r < rows; r++) wall[r] &= troops[r];
        for (int i = 0; i < size; i++) { cells[i] = i < cell.length && cell[i] > 0 ? cell[i] : wall[i / 5] ? rowCell[i / 5] : gap; widest[i / 5] = Math.max(widest[i / 5], cells[i]); }
        for (int r = 1; r < rows; r++) depth[r] = depth[r - 1] + (wall[r - 1] && wall[r] ? Math.max(rowCell[r - 1], rowCell[r]) : Math.max(gap, (widest[r - 1] + widest[r]) / 2));
        double[][] ring = new double[size][];
        for (int r = 0; r < rows; r++) {
            double[] row = deployRow(Arrays.copyOfRange(cells, r * 5, r * 5 + 5));
            for (int i = 0; i < 5; i++) ring[r * 5 + i] = new double[]{row[i], depth[r] - depth[2]};
        }
        return ring;
    }
    /** How far behind its order's point a group forms (Battle.formation and CommandMode): support 9, siege 7 (a mortar cannot fire within its minimum range), ranged 5, the rest 0. */
    public static double roleDepth(String group) {
        return switch (group) { case "support" -> 9; case "siege" -> 7; case "ranged" -> 5; default -> 0; };
    }
    /** How many abreast a group forms in a formation of this many columns: support 2, siege and ranged 6, the rest 12. */
    public static int groupColumns(String group, int formationColumns) {
        return Math.min(formationColumns, switch (group) { case "support" -> 2; case "siege", "ranged" -> 6; default -> 12; });
    }
    /**
     * The sideways offsets of one deploy row (Battle.reinforce) from the cells of its places: neighbours stand half of each one's cell apart, so
     * shield troops form their default Hold at the wall cell, and the row is centred on its middle place (index 2 of a row of 5). With every cell
     * the same c this is (i - 2) x c, the old row.
     */
    public static double[] deployRow(double[] cells) {
        double[] at = new double[cells.length];
        for (int i = 1; i < cells.length; i++) at[i] = at[i - 1] + (cells[i - 1] + cells[i]) / 2;
        int middle = (cells.length - 1) / 2; double shift = cells.length == 0 ? 0 : at[middle];
        for (int i = 0; i < at.length; i++) at[i] -= shift;
        return at;
    }
    /** Index of the anchor nearest the target (tx,tz), or -1 when no anchor is strictly nearer to the target than the base (bx,bz). */
    public static int nearestAnchor(double tx, double tz, double bx, double bz, double[] ax, double[] az) {
        int best = -1;
        double bestDistance = Math.hypot(tx - bx, tz - bz);
        for (int i = 0; i < Math.min(ax.length, az.length); i++) {
            double distance = Math.hypot(tx - ax[i], tz - az[i]);
            if (distance < bestDistance) { bestDistance = distance; best = i; }
        }
        return best;
    }
    /** Index of the point to attack: the one minimising distance + defenders*defenderWeight among points not owned by team (all points when the team owns every one). */
    public static int leastDefended(double cx, double cz, double[] px, double[] pz, int[] owner, int[] defenders, int team, double defenderWeight) {
        int n = Math.min(Math.min(px.length, pz.length), Math.min(owner.length, defenders.length));
        boolean anyFree = false;
        for (int i = 0; i < n; i++) if (owner[i] != team) { anyFree = true; break; }
        int best = -1;
        double bestValue = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (anyFree && owner[i] == team) continue;
            double value = Math.hypot(px[i] - cx, pz[i] - cz) + defenders[i] * defenderWeight;
            if (value < bestValue) { bestValue = value; best = i; }
        }
        return best;
    }
    /** Per army index, the point index that army must hold, or -1 (free to flank). Positions are {x,z}; near means within nearRadius.
     *  Guards first: a point owned by team keeps the nearest army standing near it as its guard (ties: the lower army index), and that army
     *  holds it, so a point is not left the moment it is taken. Then every owned point still unguarded takes the nearest army that is neither
     *  guarding nor holding. In both passes the nearest army-point pair is settled first, so an army takes at most one point and goes to the
     *  nearer of two. assignable (one per army) is false for an army the bots cannot order, a player's: it still guards the point it stands
     *  on, so no bot is pulled back to it, but it is never sent to one and always gets -1. */
    public static int[] garrison(int[] owners, int team, double[][] armyPos, boolean[] assignable, double[][] pointPos, double nearRadius) {
        int[] hold = new int[armyPos.length];
        for (int i = 0; i < hold.length; i++) hold[i] = -1;
        int n = Math.min(owners.length, pointPos.length);
        boolean[] open = new boolean[n], taken = new boolean[armyPos.length];
        for (int j = 0; j < n; j++) open[j] = owners[j] == team;
        int[] pair;
        while ((pair = nearestPair(open, taken, null, armyPos, pointPos, nearRadius)) != null) {
            taken[pair[0]] = true;
            open[pair[1]] = false;
            if (assignable[pair[0]]) hold[pair[0]] = pair[1];
        }
        while ((pair = nearestPair(open, taken, assignable, armyPos, pointPos, Double.POSITIVE_INFINITY)) != null) {
            taken[pair[0]] = true;
            open[pair[1]] = false;
            hold[pair[0]] = pair[1];
        }
        return hold;
    }
    /** The nearest {army, point} pair, at most limit apart, of an open point and an army not yet taken (and assignable, unless that is null),
     *  or null. Points, then armies, are scanned in index order and only a strictly nearer pair replaces the best, so ties go to the lower
     *  point index, then the lower army index. */
    private static int[] nearestPair(boolean[] open, boolean[] taken, boolean[] assignable, double[][] armyPos, double[][] pointPos, double limit) {
        int[] best = null;
        double bestDistance = limit;
        for (int j = 0; j < open.length; j++) {
            if (!open[j]) continue;
            for (int i = 0; i < armyPos.length; i++) {
                if (taken[i] || assignable != null && !assignable[i]) continue;
                double distance = Math.hypot(armyPos[i][0] - pointPos[j][0], armyPos[i][1] - pointPos[j][1]);
                if (best == null ? distance <= limit : distance < bestDistance) { best = new int[]{i, j}; bestDistance = distance; }
            }
        }
        return best;
    }
    /** The point index a bot army's lane sends it to. Dawn's lane i is point i; Dusk's is the mirror, points-1-i, so both teams
     *  play the same plan from their own side (Kingsfield: Dawn A, B, C against Dusk E, D, C; three points: Dusk C, B, A). */
    public static int laneTarget(int lane, int team, int points) {
        int i = Math.floorMod(lane, points);
        return team == 0 ? i : points - 1 - i;
    }
    /** A charge goes in when the army outnumbers the enemies near it by chargeRatio and, when chargeDistance is above zero, the nearest enemy is within it. */
    public static boolean timedCharge(double nearestEnemyDistance, long living, long enemiesNear, double chargeRatio, double chargeDistance) {
        return living >= enemiesNear * chargeRatio && (chargeDistance <= 0 || nearestEnemyDistance <= chargeDistance);
    }
    /** Whether a shield troop holds its shield up. Never on a charge or a retreat, and never with an enemy in reach
     *  (nearestSq at or inside reachSq, both squared blocks): the troop lowers it to swing, as a player must. On Hold any
     *  enemy raises it (hasEnemy: one picked or in sight); on any other order only archer fire does (underFire). */
    public static boolean raiseShield(String order, boolean underFire, double nearestSq, double reachSq, boolean hasEnemy) {
        if (order.equals("charge") || order.equals("retreat")) return false;
        if (hasEnemy && nearestSq <= reachSq) return false;
        if (order.equals("hold")) return hasEnemy;
        return underFire;
    }
    /** Whether a squad group is still under fire: it was shot at (shotAt, battle ticks; 0 means never) at most memoryTicks ago. */
    public static boolean underFire(long now, long shotAt, int memoryTicks) { return shotAt > 0 && now - shotAt <= memoryTicks; }
    /**
     * Round gold from held points per income tick: held times (pointGold + bonus), capped at objectiveIncomeCap scaled by the same
     * ratio, so the cap still stands for the same number of points (the Quartermaster's bonus: 3 a point becomes 5, and the cap of 9,
     * three points, becomes 15). With bonus 0 it is the Phase 1 rule min(cap, held x pointGold).
     */
    public static int pointIncome(long held, int pointGold, int bonus, int cap) {
        int per = pointGold + bonus;
        long limit = pointGold > 0 ? (long) cap * per / pointGold : cap;
        return (int) Math.max(0, Math.min(limit, held * per));
    }
    /** A troop's round cost under a class factor (the Warlock's magic discount): the sheet cost times the factor, rounded half up. */
    public static int classCost(int cost, double factor) { return (int) Math.round(cost * factor); }
    /** Ticks a troop trains under a class factor (the Marshal's drill): the sheet seconds times 20 times the factor, rounded. */
    public static long trainingTicks(double seconds, double factor) { return Math.round(seconds * 20 * factor); }
}
