package gg.gravesandglory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure Phase 1 battle maths: projectile travel, HUD loss detection, the speed modifier and the wounded share. Never shipped. */
public final class BattleMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    /** How many units stand somewhere else than they did: unit i was at prev[i] (null: new), it now takes places[at[i]]. */
    static int moved(double[][] prev, double[][] places, int[] at) {
        int n = 0;
        for (int i = 0; i < at.length; i++) if (prev[i] != null && Math.hypot(places[at[i]][0] - prev[i][0], places[at[i]][1] - prev[i][1]) > 1e-9) n++;
        return n;
    }
    /** Whether every unit that moved went at most one cell sideways and one rank (grid places), and no two of a rank passed each other. */
    static boolean slides(double[][] prev, double[][] grid, int[] at) { return slides(prev, grid, at, 1); }
    /** As slides, for `most` places emptied at once: each man at most `most` cells sideways and `most` ranks (one per loss), and still nobody passes anybody. */
    static boolean slides(double[][] prev, double[][] grid, int[] at, int most) {
        for (int i = 0; i < at.length; i++) {
            if (prev[i] == null) continue;
            if (Math.abs(grid[at[i]][0] - prev[i][0]) > most + 1e-9 || Math.abs(grid[at[i]][1] - prev[i][1]) > most + 1e-9) return false;
            for (int j = 0; j < at.length; j++)
                if (prev[j] != null && Math.abs(grid[at[i]][1] - grid[at[j]][1]) < 1e-9 && (grid[at[i]][0] - grid[at[j]][0]) * (prev[i][0] - prev[j][0]) < -1e-12) return false;
        }
        return true;
    }
    /**
     * `total` shield men in `columns` columns (holdGrid's places, in its order), the men at `dead` fall in one refresh: keepPlaces over the
     * survivors. The original indices of the men who took another place, in order, or a note of what broke (not a permutation, or a man who
     * went further than one place per loss, or passed another).
     */
    static String movers(int total, int columns, int... dead) {
        boolean[] all = new boolean[total]; java.util.Arrays.fill(all, true);
        double[][] g = BattleMath.holdGrid(all, columns, false);
        java.util.Set<Integer> gone = new java.util.HashSet<>(); for (int d : dead) gone.add(d);
        int n = total - gone.size(); double[][] prev = new double[n][]; int[] who = new int[n];
        for (int i = 0, j = 0; i < total; i++) if (!gone.contains(i)) {prev[j] = g[i]; who[j++] = i;}
        boolean[] s = new boolean[n]; java.util.Arrays.fill(s, true);
        double[][] gn = BattleMath.holdGrid(s, columns, false); int[] at = BattleMath.keepPlaces(prev, s, columns, false);
        if (!permutation(at)) return "not a permutation";
        if (!slides(prev, gn, at, gone.size())) return "a long step or a crossing";
        java.util.List<Integer> m = new java.util.ArrayList<>();
        for (int j = 0; j < n; j++) if (Math.hypot(gn[at[j]][0] - prev[j][0], gn[at[j]][1] - prev[j][1]) > 1e-9) m.add(who[j]);
        return m.toString();
    }
    /** The grid place of each man after the losses one refresh apart (keepPlaces with one loss, then the next against its result), by original index; null for the fallen. */
    static double[][] oneByOne(int total, int columns, int... dead) {
        boolean[] all = new boolean[total]; java.util.Arrays.fill(all, true);
        double[][] place = BattleMath.holdGrid(all, columns, false).clone(); java.util.Set<Integer> gone = new java.util.HashSet<>();
        for (int d : dead) {
            gone.add(d); java.util.List<Integer> alive = new java.util.ArrayList<>();
            for (int i = 0; i < total; i++) if (!gone.contains(i)) alive.add(i);
            double[][] prev = new double[alive.size()][]; for (int j = 0; j < prev.length; j++) prev[j] = place[alive.get(j)];
            boolean[] s = new boolean[prev.length]; java.util.Arrays.fill(s, true);
            double[][] gn = BattleMath.holdGrid(s, columns, false); int[] at = BattleMath.keepPlaces(prev, s, columns, false);
            if (!slides(prev, gn, at, 1)) throw new AssertionError("a single loss of " + d + " slides more than one place");
            java.util.Arrays.fill(place, null); for (int j = 0; j < prev.length; j++) place[alive.get(j)] = gn[at[j]];
        }
        return place;
    }
    /** The grid place of each man (by original index; null for the fallen) after the losses in one refresh. */
    static double[][] atOnce(int total, int columns, int... dead) {
        boolean[] all = new boolean[total]; java.util.Arrays.fill(all, true);
        double[][] g = BattleMath.holdGrid(all, columns, false), place = new double[total][];
        java.util.Set<Integer> gone = new java.util.HashSet<>(); for (int d : dead) gone.add(d);
        java.util.List<Integer> alive = new java.util.ArrayList<>(); for (int i = 0; i < total; i++) if (!gone.contains(i)) alive.add(i);
        double[][] prev = new double[alive.size()][]; for (int j = 0; j < prev.length; j++) prev[j] = g[alive.get(j)];
        boolean[] s = new boolean[prev.length]; java.util.Arrays.fill(s, true);
        double[][] gn = BattleMath.holdGrid(s, columns, false); int[] at = BattleMath.keepPlaces(prev, s, columns, false);
        for (int j = 0; j < prev.length; j++) place[alive.get(j)] = gn[at[j]];
        return place;
    }
    static boolean permutation(int[] at) {
        boolean[] seen = new boolean[at.length];
        for (int a : at) { if (a < 0 || a >= at.length || seen[a]) return false; seen[a] = true; }
        return true;
    }
    public static void main(String[] args) {
        check(BattleMath.travelTicks(22, 1.0, 2, 32) == 22, "arrow at 22 blocks lands in 22 ticks");
        check(BattleMath.travelTicks(1, 1.0, 2, 32) == 2, "point-blank shots still travel the minimum");
        check(BattleMath.travelTicks(400, 1.4, 2, 32) == 32, "travel is capped");
        // volley arcs (animation rework Task 4): a range-scaled parabola over the straight line, flight time unchanged
        check(BattleMath.arcHeight(0, 5) == 0 && BattleMath.arcHeight(1, 5) == 0 && BattleMath.arcHeight(.5, 5) == 5, "arc is a parabola peaking mid-flight");
        check(BattleMath.arcPeak(22, .25, 6, 1) == 5.5 && BattleMath.arcPeak(40, .25, 6, 1) == 6 && Math.abs(BattleMath.arcPeak(22, .25, 6, .4) - 2.2) < 1e-9, "peak grows with range, capped, scaled");
        check(BattleMath.launchPitch(20, 0, 0, 10) == 0 && BattleMath.launchPitch(20, 0, 5, 10) > 30, "flat with no arc; lofted with one");
        check(Math.abs(BattleMath.arcHeight(.25, 4) - 3) < 1e-9 && BattleMath.arcHeight(-.5, 4) == 0 && BattleMath.arcHeight(1.5, 4) == 0 && BattleMath.arcHeight(.5, 0) == 0, "a quarter of the way up the arc stands at three quarters of its peak; outside the flight (before launch, after landing) and with no peak it is flat");
        check(Math.abs(BattleMath.launchPitch(20, 0, 5, 10) - 45) < 1e-9 && Math.abs(BattleMath.launchPitch(20, -20, 5, 7)) < 1e-9 && Math.abs(BattleMath.launchPitch(10, 10, 0, 4) - 45) < 1e-9 && BattleMath.launchPitch(20, 0, 5, 0) == BattleMath.launchPitch(20, 0, 5, 10),
            "the launch tangent: up 4 x peak over the range (45 degrees for 20 out and 20 up), a downhill shot's arc can cancel the drop, a straight shot up a slope follows it, and the flight time never tilts it");
        check(Math.abs(BattleMath.launchPitch(0, 0, 5, 10) - 90) < 1e-9 && BattleMath.launchPitch(0, 0, 0, 10) == 0, "a shot at its own feet: straight up with an arc, flat with none (never NaN)");
        check(BattleMath.arcScale("bow", .4, .6) == 1 && BattleMath.arcScale("crossbow", .4, .6) == .4 && BattleMath.arcScale("thrower", .4, .6) == .6, "by weapon: a bow arcs fully, a crossbow (the Arbalist's and the Ballista's, R24) at crossbowArcScale, a thrown trident at thrownArcScale");
        check(BattleMath.arcScale("crew", .4, .6) == 0 && BattleMath.arcScale("caster", .4, .6) == 0 && BattleMath.arcScale("sword", .4, .6) == 0, "the Mortar's crew, a caster's (mist) bolt and anything else fly as before: no arc");
        // R26: overhead cover caps the peak (M2): clear is how far the first solid block over the straight line stands above it at sampled t; the arc keeps 0.5 under it, and under 1 block of headroom (clear - margin) it flies flat
        double open = Double.POSITIVE_INFINITY; double[] quarters = {.25, .5, .75};
        check(BattleMath.arcCoverPeak(5.5, quarters, new double[]{open, open, open}, .5, 1) == 5.5 && BattleMath.arcCoverPeak(.5, quarters, new double[]{open, open, open}, .5, 1) == .5, "open sky keeps the peak, however small");
        check(Math.abs(BattleMath.arcCoverPeak(5.5, quarters, new double[]{open, 1.7, open}, .5, 1) - 1.2) < 1e-9 && Math.abs(BattleMath.arcCoverPeak(5.5, quarters, new double[]{2, open, open}, .5, 1) - 2) < 1e-9, "a roof 1.7 over the middle caps the peak at 1.2; one 2 over the first quarter (where the arc stands at 3/4 of its peak) at 2");
        check(BattleMath.arcCoverPeak(5.5, quarters, new double[]{open, 1.2, open}, .5, 1) == 0 && BattleMath.arcCoverPeak(5.5, quarters, new double[]{open, 0, open}, .5, 1) == 0 && BattleMath.arcCoverPeak(.5, quarters, new double[]{open, 1.2, open}, .5, 1) == .5, "under 1 block of headroom the shot flies flat, and a line through a block too; cover higher than a small arc leaves it be");
        check(BattleMath.arcCoverPeak(5.5, quarters, new double[]{1.3, open, open}, .5, 1) == 0 && Math.abs(BattleMath.arcCoverPeak(5.5, quarters, new double[]{1.5, open, open}, .5, 1) - 4 / 3.0) < 1e-9,
            "the flat rule reads headroom, not the capped peak (R-M2): 0.8 of room over the first quarter flies flat though it would cap the peak at 1.07; a full block of room there caps it at 1.33");
        // N4: sampled on whole ticks, an odd flight never stands on t = 0.5; its highest tick is P (1 - 1/T^2), an even one reaches P
        double top3 = 0, top2 = 0; for (int k = 0; k <= 3; k++) top3 = Math.max(top3, BattleMath.arcHeight(k / 3.0, 5.5)); for (int k = 0; k <= 2; k++) top2 = Math.max(top2, BattleMath.arcHeight(k / 2.0, 5.5));
        check(Math.abs(top3 - 5.5 * (1 - 1 / 9.0)) < 1e-9 && Math.abs(top2 - 5.5) < 1e-9, "a 3-tick arc's highest tick stands at 8/9 of its peak; a 2-tick arc's at the peak");
        // R25: an arrow's attitude, nose first, in the convention vanilla gives a flying (not no-clip) arrow: yaw atan2(vx, vz), pitch atan2(vy, horizontal), up positive
        check(BattleMath.arrowYaw(0, 1) == 0 && Math.abs(BattleMath.arrowYaw(1, 0) - 90) < 1e-9 && Math.abs(Math.abs(BattleMath.arrowYaw(0, -1)) - 180) < 1e-9 && Math.abs(BattleMath.arrowYaw(-1, 0) + 90) < 1e-9, "an arrow's yaw: flying +z 0, +x 90, -z 180, -x -90");
        check(Math.abs(BattleMath.arrowPitch(1, 1, 0) - 45) < 1e-9 && Math.abs(BattleMath.arrowPitch(0, -1, 1) + 45) < 1e-9 && BattleMath.arrowPitch(3, 0, 4) == 0 && Math.abs(BattleMath.arrowPitch(0, 1, 0) - 90) < 1e-9, "an arrow's pitch: nose up while it climbs, down while it falls, level in level flight");
        check(BattleMath.arcTravelTicks(10, 5.5, 1) == 10 && BattleMath.arcTravelTicks(10, 5.5, 1.5) == 15 && BattleMath.arcTravelTicks(7, 2, 1.25) == 9 && BattleMath.arcTravelTicks(10, 0, 2) == 10 && BattleMath.arcTravelTicks(10, 5.5, 2) == 20,
            "an arced shot's flight is travelTicks x arcTravelScale, rounded (1.0 changes nothing); a shot with no arc keeps travelTicks");
        check(BattleMath.creditWindowOpen(100, 100, 34) && BattleMath.creditWindowOpen(134, 100, 34) && !BattleMath.creditWindowOpen(135, 100, 34), "a death with no attacker credits the last blow on the window's last tick (34 on) but not one tick after");
        check(!BattleMath.creditWindowOpen(5000, 100, 34) && !BattleMath.creditWindowOpen(100, 101, 34) && BattleMath.creditWindowOpen(7, 7, 0), "a blow minutes old credits nothing (a fall, lava or /kill long after), nor does one stamped in the future; a zero window still takes the same tick");
        check(BattleMath.tookLoss(.80f, 12, .78f, 12) && BattleMath.tookLoss(.8f, 12, .9f, 11), "health drop or a death flashes");
        check(!BattleMath.tookLoss(.80f, 12, .805f, 12) && !BattleMath.tookLoss(.5f, 6, .7f, 6), "noise and healing do not flash");
        check(!BattleMath.tookLoss(.80f, 12, .795f, 12), "a sub-threshold drop does not flash");
        check(BattleMath.tookLoss(1f, 12, 1f - 4f / 36f / 12f, 12), "one arrow on a full 12-knight group flashes");
        check(Math.abs(BattleMath.strength(60, 360) - 1/6.0) < 1e-9 && BattleMath.strength(400, 360) == 1 && BattleMath.strength(5, 0) == 1, "strength is health-weighted and clamped");
        check(BattleMath.relativeBearing(0, 0, 1) < 1 && Math.abs(BattleMath.relativeBearing(0, 0, -1) - 180) < 1 && Math.abs(BattleMath.relativeBearing(0, -1, 0) - 90) < 1, "yaw 0 faces +z");
        // the no-routing design: one speed helper for Riptide, Muster and Howl (a real xN on land is the square root of the attribute, the swim speed is linear), and the wounded share the fearless stance reads
        check(Math.abs(BattleMath.speedModifier(1.2,false)-(Math.sqrt(1.2)-1))<1e-12 && Math.abs(BattleMath.speedModifier(1.2,true)-0.2)<1e-12 && BattleMath.speedModifier(1,false)==0 && BattleMath.speedModifier(1,true)==0 && Math.abs(BattleMath.speedModifier(1.44,false)-0.2)<1e-12, "speedModifier: x1.2 on land is sqrt(1.2)-1, in water 0.2; x1 is 0");
        check(BattleMath.wounded(6,20,.3) && !BattleMath.wounded(6.2f,20,.3) && BattleMath.wounded(5,20,.3) && !BattleMath.wounded(1,0,.3), "wounded: at or under 30% of max health, never with no max");
        double[] ax={0,-40}, az={0,-140};
        check(BattleMath.nearestAnchor(40, 0, -108, 0, ax, az) == 0, "centre point is nearer the front than base");
        check(BattleMath.nearestAnchor(-100, 0, -108, 0, ax, az) == -1, "base wins when the fight is at home");
        check(BattleMath.nearestAnchor(40, 0, -108, 0, new double[0], new double[0]) == -1, "no anchors means base");
        check(BattleMath.spacing(0.6) == 1.3 && Math.abs(BattleMath.spacing(1.4) - 1.7) < 1e-9 && Math.abs(BattleMath.spacing(1.95) - 2.25) < 1e-9, "spacing: a husk or illager takes 1.3, an iron golem 1.7, a ravager 2.25");
        check(BattleMath.shieldCell(0.6, 0.7) == 0.7, "a husk-wide shield troop takes the 0.7 wall cell");
        check(Math.abs(BattleMath.shieldCell(0.9, 0.7) - 1.0) < 1e-9, "a large shield troop takes its width plus 0.1");
        double[] c = BattleMath.holdSlots(new boolean[]{true,false,true}, new double[]{.6,.6,.6}, 1.8, 0.7);
        check(c[0] == 0.7 && c[2] == 0.7 && c[1] == 1.8, "shield units close up; others keep the formation cell");
        check(BattleMath.holdSlots(new boolean[]{false}, new double[]{1.4}, 1.8, 0.7)[0] == 1.8 && Math.abs(BattleMath.holdSlots(new boolean[]{true,false}, new double[]{1.4,1.95}, 1.8, 0.7)[0] - 1.5) < 1e-9 && Math.abs(BattleMath.holdSlots(new boolean[]{true,false}, new double[]{1.4,1.95}, 1.8, 0.7)[1] - 2.25) < 1e-9, "a golem-wide shield troop takes 1.5, a ravager behind it its 2.25 room");
        // the Hold layout (Battle.formation and CommandMode both call it): shield ranks in front at their cell, the rest behind at theirs
        double[][] old = BattleMath.holdLayout(new boolean[7], new double[]{.6,.6,.6,1.4,.6,.6,.6}, 1.8, 0.7, 5, false);
        boolean same = true;
        for (int i = 0; i < 7; i++) same &= Math.abs(old[i][0] - (i % 5 - 2) * 1.8) < 1e-9 && Math.abs(old[i][1] - (i / 5 + 1) * 1.8) < 1e-9;
        check(same, "with no shield unit the layout is the old one: one cell, the formation's 1.8 (it holds Brassjaw's 1.7)");
        double[][] wedgeOld = BattleMath.holdLayout(new boolean[5], new double[]{.6,.6,.6,.6,.6}, 1.8, 0.7, 5, true);
        check(wedgeOld[0][0] == 0 && wedgeOld[0][1] == 1.8 && wedgeOld[1][0] == -1.8 && wedgeOld[3][0] == 1.8 && Math.abs(wedgeOld[2][1] - 3.6) < 1e-9 && Math.abs(wedgeOld[4][1] - 5.4) < 1e-9 && wedgeOld[4][0] == -3.6, "the old wedge: 1, then 3 a rank, then 5");
        boolean[] mix = {true,false,true,true,false,true,true,true};double[] w8 = {.6,.6,.6,.6,.6,.6,.6,.6};
        double[][] hold = BattleMath.holdLayout(mix, w8, 1.8, 0.7, 5, false);
        check(Math.abs(hold[0][0] + 1.4) < 1e-9 && Math.abs(hold[2][0] + 0.7) < 1e-9 && Math.abs(hold[0][1] - 0.7) < 1e-9 && Math.abs(hold[7][1] - 1.4) < 1e-9 && Math.abs(hold[7][0]) < 1e-9, "six shield units in block: five abreast 0.7 apart, 0.7 back, the sixth a rank behind at the middle");
        check(Math.abs(hold[1][1] - (1.4 + 1.8)) < 1e-9 && Math.abs(hold[4][1] - (1.4 + 1.8)) < 1e-9 && Math.abs(hold[1][0]) < 1e-9 && Math.abs(hold[4][0] - 1.8) < 1e-9, "the two others form behind the two shield ranks at the 1.8 cell, centred in the columns of the block");
        int[] order5 = BattleMath.centreOrder(5), order10 = BattleMath.centreOrder(10);
        check(java.util.Arrays.equals(order5, new int[]{2,3,1,4,0}) && order10[0] == 5 && order10[1] == 4 && order10[2] == 6 && order10[9] == 0, "a partial rank fills from the middle out, the right of a tie first");
        check(BattleMath.ranks(0,5,false) == 0 && BattleMath.ranks(5,5,false) == 1 && BattleMath.ranks(6,5,false) == 2 && BattleMath.ranks(1,5,true) == 1 && BattleMath.ranks(4,5,true) == 2 && BattleMath.ranks(5,5,true) == 3, "ranks in rows and in the wedge");
        double[][] wedgeHold = BattleMath.holdLayout(new boolean[]{true,true,true,true,false}, new double[]{.6,.6,.6,.6,.6}, 1.8, 0.7, 5, true);
        check(Math.abs(wedgeHold[1][0] + 0.7) < 1e-9 && Math.abs(wedgeHold[3][1] - 1.4) < 1e-9 && wedgeHold[4][0] == 0 && Math.abs(wedgeHold[4][1] - (1.4 + 1.8)) < 1e-9, "a wedge on Hold keeps its shape at 0.7, the rest start a wedge of their own behind it");
        // the command preview sees the markers in another order than the slots: the same places come out
        boolean[] shuffled = {false,true,true,true,true,false,true,true};
        double[][] other = BattleMath.holdLayout(shuffled, w8, 1.8, 0.7, 5, false);
        java.util.Set<String> setA = new java.util.TreeSet<>(), setB = new java.util.TreeSet<>();
        for (int i = 0; i < 8; i++) { setA.add(String.format(java.util.Locale.ROOT, "%.4f,%.4f", hold[i][0], hold[i][1])); setB.add(String.format(java.util.Locale.ROOT, "%.4f,%.4f", other[i][0], other[i][1])); }
        check(setA.equals(setB) && setA.size() == 8, "the places depend on the counts and cells, not on the order the units come in");
        // the wall keeps its places through a loss (Battle.heldPlace hands keepPlaces holdGrid's places): the man behind steps up, the rank slides toward the gap
        boolean[] s13 = new boolean[13], s12 = new boolean[12], s4 = new boolean[4], s5 = new boolean[5]; double[] w13 = new double[13];
        java.util.Arrays.fill(s13, true); java.util.Arrays.fill(s12, true); java.util.Arrays.fill(s4, true); java.util.Arrays.fill(s5, true); java.util.Arrays.fill(w13, .6);
        double[][] l13 = BattleMath.holdLayout(s13, w13, 1.8, 0.7, 10, false), l12 = BattleMath.holdLayout(s12, java.util.Arrays.copyOf(w13, 12), 1.8, 0.7, 10, false);
        double[][] g13 = BattleMath.holdGrid(s13, 10, false), g12 = BattleMath.holdGrid(s12, 10, false);
        check(Math.abs(l13[10][0] - l13[5][0]) < 1e-9 && Math.abs(l13[10][1] - 1.4) < 1e-9 && Math.abs(l13[11][0] - l13[4][0]) < 1e-9 && Math.abs(l13[12][0] - l13[6][0]) < 1e-9, "thirteen in ten columns: three behind the middle files");
        boolean scaled = true;
        for (int i = 0; i < 13; i++) scaled &= Math.abs(g13[i][0] * 0.7 - l13[i][0]) < 1e-9 && Math.abs((g13[i][1] + 1) * 0.7 - l13[i][1]) < 1e-9;
        check(scaled, "holdGrid is holdLayout in cells: side in cells, rank from 0");
        double[][] lost = new double[12][];
        for (int i = 0, j = 0; i < 13; i++) if (i != 5) lost[j++] = g13[i];
        int[] after = BattleMath.keepPlaces(lost, s12, 10, false);
        check(permutation(after) && moved(lost, g12, after) == 2 && slides(lost, g12, after) && Math.abs(g12[after[9]][0] - g13[5][0]) < 1e-9 && Math.abs(g12[after[9]][1]) < 1e-9, "a front man falls before a rear man: the man behind him steps up, the next of the rank closes behind; nobody else moves");
        double[][] lost4 = new double[12][];
        for (int i = 0, j = 0; i < 13; i++) if (i != 4) lost4[j++] = g13[i];
        int[] after4 = BattleMath.keepPlaces(lost4, s12, 10, false);
        check(permutation(after4) && moved(lost4, g12, after4) == 3 && slides(lost4, g12, after4) && Math.abs(g12[after4[11]][0] - g13[5][0]) < 1e-9 && Math.abs(g12[after4[9]][0] - g13[4][0]) < 1e-9 && Math.abs(g12[after4[10]][1]) < 1e-9,
            "a front man falls before the flank of the rear rank: the man behind steps up, the rank slides one place toward the gap (three short moves, nobody passes anybody)");
        double[][] g5 = BattleMath.holdGrid(s5, 5, false), g4 = BattleMath.holdGrid(s4, 5, false);
        double[][] lostOne = {g5[0], g5[1], g5[3], g5[4]};
        int[] one4 = BattleMath.keepPlaces(lostOne, s4, 5, false);
        check(permutation(one4) && moved(lostOne, g4, one4) == 2 && slides(lostOne, g4, one4) && Math.abs(g4[one4[1]][0] - g5[2][0]) < 1e-9 && Math.abs(g4[one4[0]][0] - g5[1][0]) < 1e-9, "one rank, its middle man falls: the rank slides one place in from the side whose flank place is gone, the rest stand");
        double[][] grown = java.util.Arrays.copyOf(g12, 13);
        int[] arrival = BattleMath.keepPlaces(grown, s13, 10, false);
        check(permutation(arrival) && moved(grown, g13, arrival) == 0, "a man joins: everyone keeps his place, he takes the new one");
        double[][] fresh = new double[12][];
        for (int i = 0; i < 12; i++) fresh[i] = new double[]{100 + i, 100};
        int[] reformed = BattleMath.keepPlaces(fresh, s12, 10, false);
        boolean identity = true;
        for (int i = 0; i < 12; i++) identity &= reformed[i] == i;
        check(identity, "a new order or shape (no place kept): the units take the places in slot order");
        // several places empty in one refresh (an AoE, or deaths while the group fought): the holes are filled one at a time, each a single-loss slide against the last (R17)
        check(movers(13, 10, 1, 2).equals("[3, 4, 10, 11, 12]"), "two adjacent front men fall, no one behind them: the rank slides two places in from the rear rank's side, the rear men step up and close, nobody passes anybody: " + movers(13, 10, 1, 2));
        check(movers(13, 10, 1, 8).equals("[2, 3, 4, 6, 7, 11, 12]"), "two front men fall at either flank: each side of the rank slides one place out and a rear man steps up behind each slide; the rear middle man stands: " + movers(13, 10, 1, 8));
        check(movers(13, 10, 4, 10).equals("[11, 12]"), "a front man and the rear man beside his file fall: the man behind steps up, the rear flank closes one place; nobody else moves: " + movers(13, 10, 4, 10));
        check(movers(13, 10, 1, 2, 3).equals("[4, 5, 10, 11, 12]"), "three adjacent front men fall: the rank slides three places in, the three rear men step into it, nobody passes anybody: " + movers(13, 10, 1, 2, 3));
        check(movers(13, 10, 5).equals("[10, 12]") && movers(13, 10, 4).equals("[10, 11, 12]"), "one loss through the same call: two and three short moves, as before");
        boolean composed = true;
        for (int[] d : new int[][]{{2, 1}, {8, 1}, {10, 4}, {3, 2, 1}}) {
            double[][] once = atOnce(13, 10, d), step = oneByOne(13, 10, d);
            for (int i = 0; i < 13; i++) composed &= once[i] == null ? step[i] == null : step[i] != null && Math.abs(once[i][0] - step[i][0]) < 1e-9 && Math.abs(once[i][1] - step[i][1]) < 1e-9;
        }
        check(composed, "the losses in one refresh end where the same losses one refresh apart end (the hole nearest the flank place that goes first): successive single-loss slides");
        // heldPlace's rebuild (BattleMath.held, which Battle.heldPlace calls): the cache, a cell change, a death and a new shape
        java.util.List<String> men13 = new java.util.ArrayList<>(); for (int i = 0; i < 13; i++) men13.add("m" + i);
        double[] wideFront = w13.clone(); wideFront[4] = 0.9;
        BattleMath.Held<String> h13 = BattleMath.held(null, men13, "line/10/false", BattleMath.holdLayout(s13, wideFront, 1.8, 0.7, 10, false), s13, 10, false);
        check(h13 == BattleMath.held(h13, new java.util.ArrayList<>(men13), "line/10/false", BattleMath.holdLayout(s13, wideFront, 1.8, 0.7, 10, false), s13, 10, false) && Math.abs(h13.at.get("m0")[0] + 4.5) < 1e-9, "the same members, shape and layout keep the cached places; a 0.9-wide man makes the wall's cell 1.0");
        java.util.List<String> men12 = new java.util.ArrayList<>(men13); men12.remove("m4");
        double[] narrow = new double[12]; java.util.Arrays.fill(narrow, .6);
        BattleMath.Held<String> h12 = BattleMath.held(h13, men12, "line/10/false", BattleMath.holdLayout(s12, narrow, 1.8, 0.7, 10, false), s12, 10, false);
        java.util.List<String> filed = new java.util.ArrayList<>();
        for (String m : men12) if (!java.util.Arrays.equals(h12.grid.get(m), h13.grid.get(m))) filed.add(m);
        check(h12 != h13 && filed.equals(java.util.List.of("m10", "m11", "m12")) && Math.abs(h12.at.get("m0")[0] + 4.5 * 0.7) < 1e-9 && Math.abs(h12.at.get("m11")[0] + 0.35) < 1e-9 && Math.abs(h12.at.get("m11")[1] - 0.7) < 1e-9,
            "the widest wall man (in the front) falls and the cell narrows from 1.0 to 0.7: the places are rebuilt from the kept files and ranks (not the blocks), the same three short moves, the rest keep file and rank at the new spacing: " + filed);
        BattleMath.Held<String> wider = BattleMath.held(h12, men12, "line/10/false", BattleMath.holdLayout(s12, narrow, 1.8, 0.8, 10, false), s12, 10, false);
        boolean sameFiles = wider != h12;
        for (String m : men12) sameFiles &= java.util.Arrays.equals(wider.grid.get(m), h12.grid.get(m)) && Math.abs(wider.at.get(m)[0] - h12.grid.get(m)[0] * 0.8) < 1e-9;
        check(sameFiles, "only the layout changes (the wall cell 0.7 to 0.8): rebuilt, every man keeps his file and rank, the spacing follows");
        BattleMath.Held<String> block = BattleMath.held(h12, men12, "block/5/false", BattleMath.holdLayout(s12, narrow, 1.8, 0.7, 5, false), s12, 5, false);
        double[][] g5x12 = BattleMath.holdGrid(s12, 5, false); boolean slotOrder = true;
        for (int i = 0; i < 12; i++) slotOrder &= java.util.Arrays.equals(block.grid.get(men12.get(i)), g5x12[i]);
        check(slotOrder, "a new shape re-forms in slot order");
        // the deploy ring (Battle.deploy) from every slot's cell, as the reinforce rows: Hearthguard (0.6 wide, shield: 0.7) and Brassjaw (1.4: 2) never overlap
        String ringBanner = "HHBHH" + "HHHHH" + "BH.BH";
        int ringSize = ringBanner.length(); boolean[] ringShield = new boolean[ringSize]; double[] ringCell = new double[ringSize], ringWidth = new double[ringSize];
        for (int i = 0; i < ringSize; i++) {char k = ringBanner.charAt(i); if (k == '.') continue; ringShield[i] = k == 'H'; ringWidth[i] = k == 'H' ? .6 : 1.4; ringCell[i] = BattleMath.deployCell(ringShield[i], ringWidth[i], 0.7);}
        double[][] ring = BattleMath.deployRing(ringShield, ringCell, 2);
        double worstRing = 9; String worstPair = "none";
        for (int i = 0; i < ringSize; i++) for (int j = i + 1; j < ringSize; j++) {
            if (ringCell[i] <= 0 || ringCell[j] <= 0) continue;
            double slack = Math.hypot(ring[i][0] - ring[j][0], ring[i][1] - ring[j][1]) - (ringWidth[i] / 2 + ringWidth[j] / 2 + 0.1);
            if (slack < worstRing) {worstRing = slack; worstPair = i + " and " + j;}
        }
        check(ringCell[0] == 0.7 && ringCell[2] == 2 && worstRing >= -1e-9, "a mixed Hearthguard and Brassjaw banner: every two deploy points stand at least their half widths and 0.1 apart (least slack " + worstRing + ", slots " + worstPair + ")");
        double[] twos = new double[12]; java.util.Arrays.fill(twos, 2);
        double[][] oldRing = BattleMath.deployRing(new boolean[12], twos, 2); boolean asBefore = oldRing.length >= 12;
        for (int i = 0; i < 12; i++) asBefore &= Math.abs(oldRing[i][0] - (i % 5 - 2) * 2) < 1e-9 && Math.abs(oldRing[i][1] - (i / 5 - 2) * 2) < 1e-9;
        check(asBefore, "no shield troop: the old ring, 2 apart, centred on slot 12");
        double[] sevens = new double[10]; java.util.Arrays.fill(sevens, .7); boolean[] tens = new boolean[10]; java.util.Arrays.fill(tens, true);
        double[][] wallRing = BattleMath.deployRing(tens, sevens, 2);
        check(Math.abs(wallRing[1][0] - wallRing[0][0] - 0.7) < 1e-9 && Math.abs(wallRing[5][1] - wallRing[0][1] - 0.7) < 1e-9 && Math.abs(wallRing[5][0] - wallRing[0][0]) < 1e-9, "two rows of shield troops: shoulder to shoulder, their cell apart, one deep wall");
        double[] depths = BattleMath.deployDepths(new boolean[]{true,true,false,true,true}, new double[]{.7,.7,0,.7,1.0}, 3);
        check(depths[0] == 0 && Math.abs(depths[1] - 0.7) < 1e-9 && Math.abs(depths[2] - 3.7) < 1e-9 && Math.abs(depths[3] - 6.7) < 1e-9 && Math.abs(depths[4] - 7.7) < 1e-9, "deploy rows: shield rows a cell apart (the wider of the two), any other row 3 back");
        double[] row = BattleMath.deployRow(new double[]{2.5,2.5,2.5,2.5,2.5}), wall = BattleMath.deployRow(new double[]{.7,.7,.7,2.5,2.5});
        check(row[0] == -5 && row[2] == 0 && row[4] == 5 && Math.abs(wall[1] - wall[0] - 0.7) < 1e-9 && Math.abs(wall[2] - wall[1] - 0.7) < 1e-9 && wall[2] == 0 && Math.abs(wall[3] - wall[2] - 1.6) < 1e-9, "a deploy row: the old 2.5 places, and shield troops shoulder to shoulder at 0.7");
        double[] px={-44,0,44}, pz={-43,0,43}; int[] own={-1,-1,-1};
        check(BattleMath.leastDefended(0,0,px,pz,own,new int[]{0,9,0},1,25) != 1, "nine defenders push hard bots off the centre");
        check(BattleMath.leastDefended(0,0,px,pz,own,new int[]{0,0,0},1,25) == 1, "undefended nearest point wins");
        check(BattleMath.leastDefended(0,0,px,pz,new int[]{1,1,1},new int[]{0,0,0},1,25) == 1, "all owned falls back to all points");
        double[][] at={{-44,-43},{0,0},{44,43}}; boolean[] bots={true,true,true};
        int[] held=BattleMath.garrison(new int[]{0,0,0},0,new double[][]{{-44,-43},{60,80},{0,0}},bots,at,36); // armies on A and B; the middle one stands 40 blocks off C
        check(held.length == 3 && held[0] == 0 && held[1] == 2 && held[2] == 1, "the armies on A and B stay as their guards; the unguarded owned point C gets the nearest free army");
        int[] none=BattleMath.garrison(new int[]{-1,1,-1},0,new double[][]{{-44,-43},{60,80},{0,0}},bots,at,36);
        check(none[0] == -1 && none[1] == -1 && none[2] == -1, "nothing owned means nothing to hold");
        int[] one=BattleMath.garrison(new int[]{0,0,-1},0,new double[][]{{40,20}},new boolean[]{true},at,36); // 45 blocks from B, 105 from A
        check(one.length == 1 && one[0] == 1, "one army, two unguarded owned points: it holds the nearer only");
        int[] tieGuard=BattleMath.garrison(new int[]{0,-1,-1},0,new double[][]{{-34,-43},{-54,-43}},new boolean[]{true,true},at,36); // both 10 blocks from A
        int[] tieSent=BattleMath.garrison(new int[]{-1,0,-1},0,new double[][]{{50,0},{-50,0}},new boolean[]{true,true},at,36); // both 50 blocks from B
        check(tieGuard[0] == 0 && tieGuard[1] == -1 && tieSent[0] == 1 && tieSent[1] == -1, "ties go to the lower army index, for a guard and for a point handed out");
        int[] onPoint=BattleMath.garrison(new int[]{0,-1,-1},0,new double[][]{{-44,-43},{0,0}},new boolean[]{false,true},at,36); // a player's army on A, a bot on B
        check(onPoint[0] == -1 && onPoint[1] == -1, "a player's army on an owned point guards it, so no bot is pulled back to it");
        int[] nearer=BattleMath.garrison(new int[]{0,-1,-1},0,new double[][]{{-44,0},{300,0}},new boolean[]{false,true},at,36); // the player's army 43 blocks from A, the bot 347
        check(nearer[0] == -1 && nearer[1] == 0, "a player's army is never sent to hold a point, even when it is the nearest");
        // Bot-AI plan step 2: a team's armies spread, taken points are left, ranged stand off, facing, guard group, refill muster.
        double[] qx={-130,0,130}, qz={100,0,-100}; boolean[] calm={false,false,false};
        check(BattleMath.pickPoint(-60,50,qx,qz,new int[]{-1,-1,-1},calm,0,0,new int[]{0,0,0},-1) == 0, "a lone army takes its near lane point");
        check(BattleMath.pickPoint(-60,50,qx,qz,new int[]{-1,-1,-1},calm,0,0,new int[]{1,0,0},-1) == 1, "a point another army is sent to is dearer: the next army spreads (audit D1)");
        check(BattleMath.pickPoint(-130,100,qx,qz,new int[]{0,-1,-1},calm,0,0,new int[]{0,0,0},0) == 1, "a taken point is left for the next one, even from on top of it (audit D2)");
        check(BattleMath.pickPoint(-130,100,qx,qz,new int[]{0,-1,-1},new boolean[]{true,false,false},0,0,new int[]{0,0,0},0) == 0, "a taken point under attack is fought for");
        check(BattleMath.pickPoint(0,0,qx,qz,new int[]{0,0,0},calm,0,1,new int[]{0,2,0},-1) != 1, "all owned, the held centre already claimed twice: the army spreads to another");
        check(BattleMath.pickPoint(10,0,new double[]{0,30},new double[]{0,0},new int[]{-1,-1},new boolean[]{false,false},0,-1,new int[]{0,0},1) == 1
            && BattleMath.pickPoint(10,0,new double[]{0,30},new double[]{0,0},new int[]{-1,-1},new boolean[]{false,false},0,-1,new int[]{0,0},-1) == 0, "near-equal points: the army keeps the one it goes for");
        check(BattleMath.rangedStandoff(22,12) == 8 && BattleMath.rangedStandoff(30,12) == 14 && BattleMath.rangedStandoff(16,12) == 5, "ranged stand off by their reach, between 5 and the point's radius plus 2");
        check(Math.abs(BattleMath.yawToward(1,0) + 90) < 1e-4 && Math.abs(BattleMath.yawToward(-1,0) - 90) < 1e-4 && Math.abs(BattleMath.yawToward(0,1)) < 1e-4, "yaw: +x is -90 (Dawn's facing), -x 90, +z 0");
        check(Math.abs(BattleMath.yawGap(170,-170) - 20) < 1e-9 && Math.abs(BattleMath.yawGap(-90,90) - 180) < 1e-9, "yaw gap wraps round");
        check("ranged".equals(BattleMath.guardGroup(8,4,0,2,0)) && "infantry".equals(BattleMath.guardGroup(3,3,0,0,0)) && BattleMath.guardGroup(10,1,0,0,0) == null, "the guard is the army's smallest fighting group; a one-group army has none to spare");
        check(BattleMath.guardGroup(10,0,0,3,0) == null && BattleMath.guardGroup(10,1,0,0,2) == null && BattleMath.guardGroup(10,0,0,3,2) == null, "support and siege are not a guard group: one fighting group plus healers or rams stays one group (C02-F2)");
        check(BattleMath.musterGo(3,3,0,200) && !BattleMath.musterGo(2,3,100,200) && BattleMath.musterGo(1,3,200,200), "refills move up once three gathered or the wait is over");
        check(BattleMath.laneTarget(0,0,5) == 0 && BattleMath.laneTarget(1,0,5) == 1 && BattleMath.laneTarget(2,0,5) == 2
            && BattleMath.laneTarget(0,1,5) == 4 && BattleMath.laneTarget(1,1,5) == 3 && BattleMath.laneTarget(2,1,5) == 2
            && BattleMath.laneTarget(0,1,3) == 2 && BattleMath.laneTarget(1,1,3) == 1 && BattleMath.laneTarget(2,1,3) == 0, "Dusk lanes mirror Dawn's: A,B,C against E,D,C on five points, C,B,A on three");
        check(BattleMath.laneTarget(3,0,3) == 0 && BattleMath.laneTarget(4,0,3) == 1 && BattleMath.laneTarget(3,1,3) == 2
            && BattleMath.laneTarget(5,1,5) == 4 && BattleMath.laneTarget(7,1,5) == 2, "a lane past the last point wraps round (lane 3 on three points is lane 0), mirrored for Dusk");
        check(!BattleMath.timedCharge(60,20,8,1.3,18) && BattleMath.timedCharge(15,20,8,1.3,18) && BattleMath.timedCharge(60,20,8,1.5,0), "hard waits for range; normal ignores distance");
        check(!BattleMath.timedCharge(15,8,20,1.3,18), "an outnumbered army does not charge even with the enemy in range");
        check(java.util.Arrays.stream(Spec.FORMATIONS).noneMatch(f -> f.id().equals("shieldwall")) && Spec.FORMATIONS.length == 4, "the Shieldwall formation is gone: the cycle is Compact, Line, Wedge and Loose, and Hold builds the wall");
        check(!BattleMath.raiseShield("charge", true, 50, 4, true) && !BattleMath.raiseShield("retreat", true, 50, 4, true), "never on charge or retreat");
        check(BattleMath.raiseShield("hold", false, 50, 4, true) && !BattleMath.raiseShield("hold", false, 3, 4, true), "hold: up, down to strike in reach");
        check(BattleMath.raiseShield("move", true, 400, 4, true) && !BattleMath.raiseShield("move", false, 400, 4, true), "moving: up only under fire");
        check(BattleMath.raiseShield("attack", true, 400, 4, false) && !BattleMath.raiseShield("attack", true, 400, 4, true), "attack: up under fire only until it has an enemy to close on, then down to run in (Kort 2026-11-11)");
        // An archer's arrow (Projectiles.scatter, strike, flyOn; Kort 2026-11-11): a fixed arc to its aim point, judged against the target where it comes down.
        double[] eye = {0, 1.6, 0}, body = {0, .9, 16};
        double[] still = {-.45, -.15, 15.55, .45, 1.95, 16.45}; // a 0.6-wide, 1.8-tall troop standing 16 blocks out, HIT_MARGIN 0.15 round it
        double[] aimStill = BattleMath.arrowAim(0, 0, body, 0, 0, 16, false, 0, 0, 0);
        check(BattleMath.arrowStrike(eye, aimStill, 4, 16, still) != null, "arrow: a stationary target in range is hit at zero spread");
        double[] aside = {1.55, -.15, 15.55, 2.45, 1.95, 16.45}; // the same troop 2 blocks off the line
        check(BattleMath.arrowStrike(eye, aimStill, 4, 16, aside) == null, "arrow: a target 2 blocks off the line is missed");
        double[] crossed = {2.75, -.15, 15.55, 3.65, 1.95, 16.45}; // a troop walking across at 0.2 blocks a tick, where it stands 16 ticks on
        double[] led = BattleMath.arrowAim(0, 0, body, .2, 0, 16, true, 0, 0, 0), unled = BattleMath.arrowAim(0, 0, body, .2, 0, 16, false, 0, 0, 0);
        check(Math.abs(led[0] - 3.2) < 1e-9 && BattleMath.arrowStrike(eye, led, 4, 16, crossed) != null, "arrow: a crossing mover on a path is led and hit");
        check(BattleMath.arrowStrike(eye, unled, 4, 16, crossed) == null, "arrow: the same mover shot where it stood (standing to fight, not led) is missed");
        double[] wide = BattleMath.arrowAim(0, 0, body, 0, 0, 16, false, 1, 0, 0);
        check(Math.abs(wide[0] + 1) < 1e-9 && Math.abs(wide[2] - 16) < 1e-9, "arrow: the across error moves the aim square to the line (-x shooting +z: the shooter's right), the given blocks");
        check(Math.abs(BattleMath.arrowSigma(.05, .01, 20) - .25) < 1e-12, "arrow: sigma = base + perBlock x range");
        check(BattleMath.flyOnTicks(.9, 0, -.5, 10) == 2 && BattleMath.flyOnTicks(.9, 0, .1, 10) == 10 && BattleMath.flyOnTicks(80, 0, -1, 10) == 10 && BattleMath.flyOnTicks(0, 0, -1, 10) == 1,
              "arrow: a miss flies on to the ground at its last fall a tick, 1 to FLY_ON_MAX ticks");
        check(BattleMath.raiseShield("hold", true, 0, 4, false), "hold: an archer it cannot see raises it");
        check(BattleMath.shieldOrder("move", true).equals("hold") && BattleMath.shieldOrder("move", false).equals("move") && BattleMath.shieldOrder("retreat", true).equals("retreat")
            && !BattleMath.raiseShield(BattleMath.shieldOrder("retreat", true), true, 1e9, 4, false) && BattleMath.raiseShield(BattleMath.shieldOrder("move", true), false, 100, 4, true),
            "an arrived Move holds its shields as on Hold; a Retreat, arrived and under fire, never raises");
        check(BattleMath.underFire(160, 100, 60) && !BattleMath.underFire(161, 100, 60) && !BattleMath.underFire(5, 0, 60), "fire memory");
        check(!BattleMath.raiseShield("hold", false, 0, 4, false) && !BattleMath.raiseShield("hold", true, 4, 4, true) && BattleMath.raiseShield("attack", true, 0, 4, false), "hold with no enemy stays down; an enemy at exactly reach lowers it; arrows with no enemy picked still raise it");
        check(MatchStats.winRate(7,10) == .7 && MatchStats.winRate(0,0) == 0, "win rate");
        var st=new MatchStats.SquadStats(); st.goldEarned+=5; check(st.goldEarned == 5, "stats accumulate");
        var a=new MatchStats.SquadStats(); a.kills=4; a.objectiveSeconds=10; var b2=new MatchStats.SquadStats(); b2.kills=4; b2.objectiveSeconds=30;
        var idle=new MatchStats.SquadStats(); var holder=new MatchStats.SquadStats(); holder.objectiveSeconds=1; var killer=new MatchStats.SquadStats(); killer.kills=1;
        check(MatchStats.mvp(List.of(a,b2)) == 1 && MatchStats.mvp(List.of()) == -1 && MatchStats.mvp(List.of(idle,new MatchStats.SquadStats())) == -1
            && MatchStats.mvp(List.of(idle,holder)) == 1 && MatchStats.mvp(List.of(idle,killer)) == 1, "mvp by kills then objective time; a kill alone or a point alone crowns; a dead tie at nothing crowns nobody");
        // the report's levels line (in-match progression design 7): the level reached, how many reached Rank 3, the gear bought
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",2,0,1,1),"Order Squire","Knight").equals("reached Knight / no gear bought"), "level 2 with no level 3s: no count, no gear");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",3,2,1,2),"Order Squire","Knight-Captain").equals("reached Knight-Captain / 2 at Rank 3 / Armour II"), "armour only");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",3,1,2,1),"Order Squire","Knight-Captain").equals("reached Knight-Captain / 1 at Rank 3 / Weapon II") && MatchStats.levelText(new MatchStats.LevelRow("knight",3,1,3,3),"Order Squire","Knight-Captain").endsWith("Weapon III, Armour III"), "weapon only; both, weapon first");
        check(MatchStats.levelText(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver",null).equals("reached Rank 3 / 1 at Rank 3 / Weapon III") && MatchStats.levelText(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver","").equals("reached Rank 3 / 1 at Rank 3 / Weapon III") && MatchStats.levelText(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver","Sunweaver").equals("reached Rank 3 / 1 at Rank 3 / Weapon III"), "a troop with no level names (null, empty or its own name) reads level 3");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",1,0,1,1),"Order Squire","Order Squire").equals("no Rank gains / no gear bought") && MatchStats.levelText(new MatchStats.LevelRow("knight",0,0,0,0),"Order Squire",null).equals("no Rank gains / no gear bought"), "no Rank gains reads the same under any reached name; tier 0 is no gear");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",4,1,4,1),"Order Squire","Order Squire").startsWith("reached Rank 4 / 1 at Rank 3 / Weapon "), "a level above 3 (no sheet has one) still reads, without throwing");
        // one Roman numeral helper for every tier and level the UI prints (the barracks pages, the refusal copy, the results page): I to III as always, nothing for 0 or less, true numerals above 3
        check(MatchStats.roman(1).equals("I") && MatchStats.roman(2).equals("II") && MatchStats.roman(3).equals("III"), "roman: I, II, III");
        check(MatchStats.roman(0).isEmpty() && MatchStats.roman(-2).isEmpty(), "roman: nothing for a tier of 0 or less (a House with no trait tier draws no numeral)");
        check(MatchStats.roman(4).equals("IV") && MatchStats.roman(5).equals("V") && MatchStats.roman(9).equals("IX") && MatchStats.roman(14).equals("XIV") && MatchStats.roman(40).equals("XL") && MatchStats.roman(1994).equals("MCMXCIV"), "roman: a tier above 3 (a longer gearTiers sheet) reads as a numeral, never empty and never a digit");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",3,1,4,4),"Order Squire","Knight-Captain").endsWith("Weapon IV, Armour IV") && MatchStats.levelShort(new MatchStats.LevelRow("knight",3,1,4,4),"Order Squire","Knight-Captain").endsWith("W IV A IV"), "roman: the results page prints tier 4 gear as IV in both forms");
        // the same line in the report's two-column form (more than three troop types): "Knight-Captain x2 / W III A III"
        check(MatchStats.levelShort(new MatchStats.LevelRow("knight",3,2,3,3),"Order Squire","Knight-Captain").equals("Knight-Captain x2 / W III A III") && MatchStats.levelShort(new MatchStats.LevelRow("knight",2,0,1,1),"Order Squire","Knight").equals("Knight / no gear"), "short form: level name, x level-3 count, W and A tiers; no count without level 3s");
        check(MatchStats.levelShort(new MatchStats.LevelRow("archer",3,12,2,1),"Order Archer","Sergeant-Archer").equals("Sergeant-Archer x12 / W II") && MatchStats.levelShort(new MatchStats.LevelRow("archer",3,1,1,3),"Order Archer","Sergeant-Archer").equals("Sergeant-Archer x1 / A III"), "short form: one track bought");
        check(MatchStats.levelShort(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver","Sunweaver").equals("Rank 3 x1 / W III") && MatchStats.levelShort(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver",null).equals("Rank 3 x1 / W III") && MatchStats.levelShort(new MatchStats.LevelRow("knight",1,0,1,1),"Order Squire","Order Squire").equals("no Rank gains / no gear"), "short form: a troop with no level names reads level 3; no level ups");
        int[][] ps=new int[2][2]; MatchStats.teamPointSeconds(ps,new int[]{-1,1,0,2}); MatchStats.teamPointSeconds(new int[2][0],new int[]{0,1});
        check(ps[0][0] == 0 && ps[0][1] == 0 && ps[1][0] == 0 && ps[1][1] == 1, "point seconds skip an unheld point, an unknown team and a point past the rows");
        for (int points = 0; points <= 5; points++) check(BattleMath.pointIncome(points, 3, 0, 9) == Math.min(9, points * 3), "point income with no class bonus is the Phase 1 rule min(cap, held x pointGold), " + points + " held");
        check(BattleMath.pointIncome(1, 3, 2, 9) == 5 && BattleMath.pointIncome(2, 3, 2, 9) == 10 && BattleMath.pointIncome(3, 3, 2, 9) == 15 && BattleMath.pointIncome(5, 3, 2, 9) == 15, "the Quartermaster's +2: one held point pays 5, and the cap still stands for three points (15)");
        check(BattleMath.pointIncome(0, 3, 2, 9) == 0 && BattleMath.pointIncome(4, 0, 2, 9) == 8 && BattleMath.pointIncome(9, 0, 2, 9) == 9, "nothing held pays nothing; a sheet with no point gold keeps its plain cap");
        check(BattleMath.classCost(70, 1 - 0.2) == 56 && BattleMath.classCost(22, 1) == 22 && BattleMath.classCost(45, 0.8) == 36 && BattleMath.classCost(25, 0.9) == 23, "the Warlock's 20% off: a 70-gold Sunweaver costs 56; costs round half up");
        check(BattleMath.trainingTicks(4.0, 1 - 0.2) == 64 && BattleMath.trainingTicks(4.0, 1) == 80 && BattleMath.trainingTicks(3.5, 0.8) == 56 && BattleMath.trainingTicks(9.0, 0.8) == 144, "the Marshal's drill: a knight's 80 ticks become 64");
        // Task 5: melee damage lands on the swing's impact frame
        check(BattleMath.strikeLands(true, false, true, 9, 2.8, .5) && !BattleMath.strikeLands(true, false, true, 3.4*3.4, 2.8, .5), "in reach plus grace lands; beyond misses");
        check(!BattleMath.strikeLands(false, false, true, 1, 2.8, .5) && !BattleMath.strikeLands(true, true, true, 1, 2.8, .5) && !BattleMath.strikeLands(true, false, false, 1, 2.8, .5), "dead or stunned attacker, dead target: no blow");
        for (int i = 0; i < 50; i++) check(BattleMath.pickVariant(3, 1, i) != 1, "no immediate repeat");
        check(BattleMath.pickVariant(1, 0, 7) == 0, "a single variant always plays");
        Set<Integer> others = new HashSet<>(); for (long r = -6; r < 6; r++) {int v = BattleMath.pickVariant(3, 1, r); check(v >= 0 && v < 3, "a picked variant is one of the family's"); others.add(v);}
        check(others.equals(Set.of(0, 2)) && BattleMath.pickVariant(2, 0, 99) == 1 && BattleMath.pickVariant(2, 1, -5) == 0, "every other variant can play; with two they alternate");
        Set<Integer> first = new HashSet<>(); for (long r = 0; r < 9; r++) first.add(BattleMath.pickVariant(3, -1, r));
        check(first.equals(Set.of(0, 1, 2)) && BattleMath.pickVariant(0, -1, 4) == 0, "a troop's first swing may be any variant; an empty family plays index 0");
        // M10: the blow lands before the attacker's next swing can start, even under a shortened cooldown
        check(BattleMath.impactDelay(12, 25) == 12 && BattleMath.impactDelay(7, 20) == 7 && BattleMath.impactDelay(12, 12) == 11 && BattleMath.impactDelay(12, 5) == 4 && BattleMath.impactDelay(8, 1) == 0 && BattleMath.impactDelay(8, 0) == 0 && BattleMath.impactDelay(-3, 20) == 0, "impact delay: the sheet's impactTicks, clamped to land a tick before the next swing");
        for (int cd = 1; cd <= 40; cd++) for (int imp = 0; imp <= 14; imp++) check(BattleMath.impactDelay(imp, cd) < Math.max(1, cd) && BattleMath.impactDelay(imp, cd) <= imp, "impact delay " + imp + " under a cooldown of " + cd + " lands before the next swing");
        // Ruling R29 (Task 5 fix round 1): a swing earns its Charge's impact on its swing tick; the wind-up never shortens the 60-tick window
        Long stamp = BattleMath.chargeStamp(true, 0L, 59, 60);
        check(stamp != null && stamp == 0L && BattleMath.chargeImpact(stamp, -1), "a swing on tick 59 of a fresh Charge's 60-tick window earns the impact, and its blow carries it however late it lands");
        check(BattleMath.chargeStamp(true, 0L, 59 + 10, 60) == null, "judged on the landing tick instead (69, a 10-tick wind-up on) the same blow would have lost it: the reason the stamp is taken at the swing");
        check(BattleMath.chargeStamp(true, 100L, 160, 60) == 100L && BattleMath.chargeStamp(true, 100L, 161, 60) == null && BattleMath.chargeStamp(true, 100L, 100, 60) == 100L, "the window holds ticks 0 to 60 from the stamp, as Combat judged it before");
        check(BattleMath.chargeStamp(false, 100L, 110, 60) == null && BattleMath.chargeStamp(true, null, 110, 60) == null, "no Charge order at the swing, or no stamp: no impact window");
        check(!BattleMath.chargeImpact(100L, 100) && !BattleMath.chargeImpact(100L, 130) && BattleMath.chargeImpact(100L, 99) && !BattleMath.chargeImpact(null, -1), "one impact per stamp: an attacker that crashed in under it already does not again");
        // Task 5 fix round 2: each round reseeds its variant rolls from the world's seed and its number alone
        // Golden values (Phase 1 review M2): a change to the mixing changes every round's swing sequence, so it must show here first
        check(BattleMath.roundSeed(4242, 3) == -5561024584431303819L && BattleMath.roundSeed(4242, 0) == -2932002319331483421L,
            "a round's variant seed is fixed by the world's seed and the round: roundSeed(4242, 3) is " + BattleMath.roundSeed(4242, 3) + " (want -5561024584431303819)");
        Set<Long> seeds = new HashSet<>(); for (int r = 0; r < 200; r++) seeds.add(BattleMath.roundSeed(4242, r));
        check(seeds.size() == 200 && BattleMath.roundSeed(4242, 1) != BattleMath.roundSeed(4243, 1), "every round, and every world, rolls its own seed");
        int[] firstPick = new int[3]; for (int r = 1; r <= 300; r++) firstPick[BattleMath.pickVariant(3, -1, new java.util.Random(BattleMath.roundSeed(4242, r)).nextLong())]++;
        check(firstPick[0] > 60 && firstPick[1] > 60 && firstPick[2] > 60, "neighbouring rounds open on unrelated swings: the first variant over 300 rounds " + java.util.Arrays.toString(firstPick) + " (want each of the 3 more than 60 times)");
        // Task 6 (hit_v1): the zone a projectile strikes from its impact height on the victim's box, and the weight of a blow from its share of the victim's health
        check(BattleMath.zone(0, 0, 2).equals("legs") && BattleMath.zone(0.88, 0, 2).equals("legs") && BattleMath.zone(0.9, 0, 2).equals("body")
            && BattleMath.zone(1.58, 0, 2).equals("body") && BattleMath.zone(1.6, 0, 2).equals("head") && BattleMath.zone(64 + 0.5 * 1.8, 64, 1.8).equals("body") && BattleMath.zone(65.75, 64, 1.8).equals("head"),
            "zone: under 0.45 of the height legs, under 0.8 body, else head");
        check(BattleMath.zone(63, 64, 1.8).equals("legs") && BattleMath.zone(70, 64, 1.8).equals("head") && BattleMath.zone(64.5, 64, 0).equals("body"), "zone: below the feet legs, over the head head, a box with no height body");
        check(BattleMath.weight(1, 20, .1, .25) == 0 && BattleMath.weight(1.99, 20, .1, .25) == 0 && BattleMath.weight(2, 20, .1, .25) == 1 && BattleMath.weight(4.99, 20, .1, .25) == 1
            && BattleMath.weight(5, 20, .1, .25) == 2 && BattleMath.weight(40, 20, .1, .25) == 2, "weight: under mediumHitShare of the max health light (0), under heavyHitShare medium (1), else heavy (2)");
        check(BattleMath.weight(5, 0, .1, .25) == 0 && BattleMath.weight(0, 20, .1, .25) == 0, "weight: no max health or no damage is light");
        // carry 1: the signed bearing of a blow's source from the victim's facing (yaw 0 faces +z, so its right hand is -x): + right, - left, 0 front, 180 behind
        check(Math.abs(BattleMath.signedBearing(0, 0, 1)) < 1e-9, "signedBearing: a source straight ahead is 0");
        check(Math.abs(BattleMath.signedBearing(0, -1, 0) - 90) < 1e-9 && Math.abs(BattleMath.signedBearing(0, 1, 0) + 90) < 1e-9, "signedBearing: a source on the right (-x at yaw 0) is +90, on the left -90");
        check(Math.abs(BattleMath.signedBearing(0, 0, -1) - 180) < 1e-9, "signedBearing: a source straight behind is 180 (never -180)");
        check(Math.abs(BattleMath.signedBearing(0, -0.01, -1) - 179.43) < 0.01 && Math.abs(BattleMath.signedBearing(0, 0.01, -1) + 179.43) < 0.01, "signedBearing: just right of behind is near +180, just left of it near -180 (the wrap)");
        check(Math.abs(BattleMath.signedBearing(90, 0, -1) - 90) < 1e-9 && Math.abs(BattleMath.signedBearing(-270, 0, -1) - 90) < 1e-9 && Math.abs(BattleMath.signedBearing(450, 0, -1) - 90) < 1e-9, "signedBearing: facing west (yaw 90) north is on the right; any turn of the yaw reads the same");
        check(Math.abs(BattleMath.signedBearing(30, 0, 0)) < 1e-9, "signedBearing: a source on the victim has no direction (0)");
        for (double yaw = -180; yaw <= 180; yaw += 37) for (double deg = 0; deg < 360; deg += 23) {
            double dx = Math.sin(Math.toRadians(deg)), dz = Math.cos(Math.toRadians(deg));
            check(Math.abs(Math.abs(BattleMath.signedBearing(yaw, dx, dz)) - BattleMath.relativeBearing(yaw, dx, dz)) < 1e-6, "signedBearing's size is relativeBearing at yaw " + yaw + ", source " + deg);
        }
        // carry 2: the plain-hold rule. On Hold shield and spear troops brace (a shield troop formed in its wall shows the wall), every other troop holds
        check(BattleMath.holdStance(true, false, false).equals("brace") && BattleMath.holdStance(true, false, true).equals("wall") && BattleMath.holdStance(false, true, false).equals("brace")
            && BattleMath.holdStance(false, true, true).equals("brace") && BattleMath.holdStance(true, true, true).equals("wall") && BattleMath.holdStance(false, false, false).equals("hold") && BattleMath.holdStance(false, false, true).equals("hold"),
            "holdStance: shield or spear brace, a formed shield wall wall, anyone else hold");
        check(BattleMath.stance("hold", false, false, false, false, false, false).equals("hold") && BattleMath.stance("hold", true, false, false, true, false, true).equals("wall") && BattleMath.stance("hold", false, true, false, false, false, false).equals("brace"),
            "stance on Hold: the plain-hold rule");
        check(BattleMath.stance("fall_back", true, false, false, false, false, true).equals("fall_back") && BattleMath.stance("charge", false, false, false, false, false, false).equals("charge") && BattleMath.stance("retreat", false, false, true, false, true, false).equals("run")
            && BattleMath.stance("attack", false, false, false, false, false, false).isEmpty(), "stance: Fall Back fall_back, Charge charge, Retreat run, other orders none");
        check(BattleMath.stance("attack", false, false, true, false, false, false).equals("ranged_rest") && BattleMath.stance("attack", false, false, true, false, true, false).equals("ranged_aim")
            && BattleMath.stance("hold", false, false, true, false, true, false).equals("ranged_aim") && BattleMath.stance("hold", false, false, true, false, false, false).equals("hold") && BattleMath.stance("fall_back", false, false, true, false, true, false).equals("fall_back"),
            "stance: a ranged troop aims at an enemy in sight and reach on any order but a run or Fall Back; with none it rests, or holds on Hold");
        check(BattleMath.stance("attack", true, false, false, false, false, true).equals("shield_move") && BattleMath.stance("attack", true, false, false, false, false, false).isEmpty() && BattleMath.stance("move", false, false, false, false, false, true).isEmpty(),
            "stance: a shield troop moving with its shield up is shield_move");
        // charge (stance 10) and stunned (11): a melee charge, a ranged troop's charge (aiming stops to shoot on any order but a run), retreat run for both, stunned never from stance()
        check(BattleMath.stance("charge", false, false, false, false, false, false).equals("charge") && BattleMath.stance("charge", true, false, false, false, false, true).equals("charge") && BattleMath.stance("charge", false, true, false, false, false, false).equals("charge"),
            "stance: a melee Charge is charge, shield up or not");
        check(BattleMath.stance("charge", false, false, true, false, false, false).equals("charge") && BattleMath.stance("charge", false, false, true, false, true, false).equals("ranged_aim"),
            "stance: a ranged troop on a Charge charges, and aims (stops to shoot) once an enemy is in sight and reach");
        check(BattleMath.stance("retreat", false, false, false, false, false, false).equals("run") && BattleMath.stance("retreat", false, false, true, false, true, false).equals("run") && BattleMath.stance("retreat", false, false, true, false, false, false).equals("run"),
            "stance: Retreat is run for melee and for ranged, aiming or not");
        check(BattleMath.STANCES.indexOf("charge") == 10 && BattleMath.STANCES.indexOf("stunned") == 11 && BattleMath.STANCES.size() == 12, "charge is stance 10 and stunned 11, appended");
        for (String order : List.of("", "hold", "attack", "follow", "charge", "retreat", "fall_back")) check(!BattleMath.stance(order, true, true, true, true, true, true).equals("stunned") && !BattleMath.stance(order, false, false, false, false, false, false).equals("stunned"),
            "stance(" + order + ") never answers stunned: the server shows it over the order's stance while Unit.stunnedUntil runs (Tactics.stunStance)");
        // stunned overrides every order's stance while battle time is before stunnedUntil, and the order's returns the tick the stun ends
        for (String shownOrder : List.of("", "hold", "brace", "wall", "shield_move", "fall_back", "run", "ranged_rest", "ranged_aim", "rally", "charge")) check(BattleMath.shownStance(shownOrder, 99, 100).equals("stunned") && BattleMath.shownStance(shownOrder, 100, 100).equals(shownOrder) && BattleMath.shownStance(shownOrder, 5, 0).equals(shownOrder),
            "shownStance: stunned over " + (shownOrder.isEmpty() ? "none" : shownOrder) + " until stunnedUntil, then the order's");
        for (String name : List.of("hold", "brace", "wall", "shield_move", "fall_back", "run", "ranged_rest", "ranged_aim", "rally", "charge", "stunned")) check(BattleMath.STANCES.indexOf(name) > 0, "STANCES holds " + name);
        check(BattleMath.STANCES.equals(List.of("", "hold", "brace", "wall", "shield_move", "fall_back", "run", "ranged_rest", "ranged_aim", "rally", "charge", "stunned")), "STANCES in the stance_v2 wire order (the command UI's rally, then charge and stunned, appended)");
        // hit_v1's kind from Combat.resolve's kind, the shooter's family and its swing's attackVariants kind
        check(BattleMath.hitKind("slam", "blunt", "blunt").equals("blunt") && BattleMath.hitKind("slam", "bow", "arrow").equals("blunt"), "hitKind: a slam is blunt");
        check(BattleMath.hitKind("bolt", "caster", "magic").equals("magic") && BattleMath.hitKind("blight", "caster", "magic").equals("magic"), "hitKind: a caster's bolt and blight are magic");
        check(BattleMath.hitKind("blight", "crew", "blunt").equals("blunt"), "hitKind: the mortar's shell lands blunt (M5)");
        check(BattleMath.hitKind("arrow", "bow", "arrow").equals("arrow") && BattleMath.hitKind("arrow", "crossbow", "bolt").equals("bolt") && BattleMath.hitKind("arrow", "thrower", "arrow").equals("arrow"),
            "hitKind: a shot keeps its family's projectile, a crossbow's a bolt");
        check(BattleMath.hitKind("melee", "stab", "stab").equals("stab") && BattleMath.hitKind("melee", "slash", "slash").equals("slash") && BattleMath.hitKind("melee", "blunt", "blunt").equals("blunt")
            && BattleMath.hitKind("melee", "", "").equals("slash"), "hitKind: a melee blow keeps its swing's kind, an unknown swing slashes");
        for (String k : List.of("stab", "slash", "blunt", "arrow", "bolt", "magic")) check(BattleMath.KINDS.contains(k), "KINDS holds " + k);
        check(BattleMath.weaponKind("minecraft:iron_sword").equals("slash") && BattleMath.weaponKind("minecraft:diamond_axe").equals("slash") && BattleMath.weaponKind("minecraft:trident").equals("stab")
            && BattleMath.weaponKind("minecraft:air").equals("blunt") && BattleMath.weaponKind("minecraft:stick").equals("blunt"), "weaponKind: a player's sword or axe slashes, a trident stabs, anything else is blunt");
        // Task 6 fix round 1. M3: a hit's weight is the health it really took (health and absorption before less after, never below 0)
        check(BattleMath.lostHealth(20, 14) == 6 && BattleMath.lostHealth(20, 20) == 0 && BattleMath.lostHealth(5, 9) == 0 && BattleMath.lostHealth(Double.NaN, 3) == 0, "lostHealth: before less after, 0 for none, never below 0 (a troop that rose again), 0 for NaN");
        check(BattleMath.hitWeight(1, false, 0.5, 4000, .1, .25) == 1 && BattleMath.hitWeight(0, false, 10, 20, .1, .25) == 2 && BattleMath.hitWeight(2, false, 0, 20, .1, .25) == 2,
            "hitWeight outside the hurt cooldown: the larger of its swing's weight and the health it took");
        check(BattleMath.hitWeight(2, true, 0.5, 20, .1, .25) == 0 && BattleMath.hitWeight(1, true, 3, 20, .1, .25) == 1 && BattleMath.hitWeight(2, true, 0, 20, .1, .25) == 0,
            "hitWeight inside the hurt cooldown (vanilla deals only the excess over the last blow): the health it took alone, so a heavy swing that took little reads light");
        check(BattleMath.hitWeight(7, false, 0, 20, .1, .25) == 2 && BattleMath.hitWeight(-3, false, 0, 20, .1, .25) == 0, "hitWeight: a swing weight outside 0-2 is clamped");
        // M4: what the client takes off the wire: a weight clamped to the ordinals, a variant only inside its family's rows
        check(BattleMath.weightOrdinal(-3) == 0 && BattleMath.weightOrdinal(0) == 0 && BattleMath.weightOrdinal(1) == 1 && BattleMath.weightOrdinal(2) == 2 && BattleMath.weightOrdinal(7) == 2, "weightOrdinal: clamped to 0 light, 1 medium, 2 heavy");
        check(BattleMath.validVariant(0, 3) && BattleMath.validVariant(2, 3) && !BattleMath.validVariant(3, 3) && !BattleMath.validVariant(-1, 3) && !BattleMath.validVariant(0, 0), "validVariant: an index inside its family's attackVariants rows; none for a family with no rows");
        // M7: stance_v2's aimPitch is MotionInput's (positive aims down): the launch angle's negative
        check(BattleMath.aimPitch(20, 0, 5, 10) < -44.9 && BattleMath.aimPitch(20, 0, 5, 10) > -45.1 && Math.abs(BattleMath.aimPitch(10, -10, 0, 4) - 45) < 1e-9, "aimPitch: a lofted shot aims up (negative), a shot down a slope aims down (positive)");
        check(Double.compare(BattleMath.aimPitch(20, 0, 0, 10), 0.0) == 0, "aimPitch: a flat shot is 0, not -0");
        // M6 and carry B8: a late joiner is sent a troop's swing while its clip may still play: ATTACK_CLIP_MAX_TICKS (no clip lengths on the server;
        // the client skips a swing whose clip has ended, and tools/clips.py holds every attack clip to the cap)
        check(BattleMath.ATTACK_CLIP_MAX_TICKS == 40, "the attack clip cap is 40 ticks");
        check(BattleMath.swingCurrent(1000, 1000) && BattleMath.swingCurrent(1040, 1000) && !BattleMath.swingCurrent(1041, 1000), "swingCurrent: from its tick through ATTACK_CLIP_MAX_TICKS, not a tick after");
        check(!BattleMath.swingCurrent(1000, -1) && !BattleMath.swingCurrent(999, 1000), "swingCurrent: no swing (-1) or one stamped after now is not current");
        // the hit's blocked flag: a counters row of the raised-shield kind (raised_front) applies
        Set<String> arrowFrom = new HashSet<>(Set.of("ranged", "nonsiege")); arrowFrom.addAll(CounterMath.kindTags("arrow"));
        check(CounterMath.blocked(new CounterMath.Hit(arrowFrom, Set.of("shield", "infantry"), 10, true, false, false, true, false, false), Spec.COUNTERS), "blocked: an arrow from the front on a raised shield");
        check(!CounterMath.blocked(new CounterMath.Hit(arrowFrom, Set.of("shield", "infantry"), 10, false, false, false, true, false, false), Spec.COUNTERS), "blocked: not with the shield lowered");
        check(!CounterMath.blocked(new CounterMath.Hit(arrowFrom, Set.of("shield", "infantry"), 150, true, false, false, true, false, false), Spec.COUNTERS), "blocked: not from behind");
        check(!CounterMath.blocked(new CounterMath.Hit(arrowFrom, Set.of("infantry"), 10, true, false, false, true, false, false), Spec.COUNTERS), "blocked: not on a troop with no shield");
        // Task 7: a fallen troop lies whole for 20 + lie ticks, then fades on an eased cosine to nothing at 20 + lie + fade
        check(BattleMath.corpseAlpha(10, 60, 40) == 1 && BattleMath.corpseAlpha(0, 60, 40) == 1, "corpseAlpha: whole while it falls");
        check(BattleMath.corpseAlpha(80, 60, 40) == 1, "corpseAlpha: still whole at 20 + lie");
        check(BattleMath.corpseAlpha(120, 60, 40) == 0 && BattleMath.corpseAlpha(500, 60, 40) == 0, "corpseAlpha: gone at 20 + lie + fade and after");
        { boolean falls = true; float last = BattleMath.corpseAlpha(80, 60, 40); for (int d = 81; d <= 120; d++) { float fa = BattleMath.corpseAlpha(d, 60, 40); falls &= fa < last && fa >= 0; last = fa; }
          check(falls, "corpseAlpha: strictly falling from 80 to 120"); }
        check(Math.abs(BattleMath.corpseAlpha(100, 60, 40) - 0.5f) < 1e-6, "corpseAlpha: half way through the fade is one half (0.5 + 0.5cos)");
        check(BattleMath.corpseAlpha(20, 0, 0) == 1 && BattleMath.corpseAlpha(21, 0, 0) == 0, "corpseAlpha: no lie and no fade is vanilla's 20-tick fall, then gone");
        { boolean onWhole = true; for (int d = 0; d <= 130; d++) onWhole &= BattleMath.corpseAlpha((float) d, 60, 40) == BattleMath.corpseAlpha(d, 60, 40);
          float h = BattleMath.corpseAlpha(100.5f, 60, 40);
          check(onWhole && h < BattleMath.corpseAlpha(100, 60, 40) && h > BattleMath.corpseAlpha(101, 60, 40), "corpseAlpha on a partial tick: equal on whole ticks, between them on a half tick"); }
        // Task 7 fix 1 (I2): a body lying past its fall ranks after every living troop for relief and whole weapons
        { double[] living = new double[60], bodies = new double[30];
          for (int i = 0; i < 60; i++) living[i] = Math.pow(10 + i / 6.0, 2); // 60 living from 10 to ~20 blocks
          for (int i = 0; i < 30; i++) bodies[i] = Math.pow(1 + i / 8.0, 2);  // 30 bodies from 1 to ~5 blocks
          double lc = BattleMath.reliefCutoffSq(living.clone(), 60, 48, 576), bc = BattleMath.bodyCutoffSq(living.clone(), 60, bodies.clone(), 30, 48, 576);
          int livingNear = 0, bodiesNear = 0; for (double d : living) if (d < 576 && d <= lc) livingNear++; for (double d : bodies) if (d < 576 && d <= bc) bodiesNear++;
          check(livingNear == 48 && bodiesNear == 0, "bodyCutoffSq: 60 living at 10-20 blocks keep all 48 relief slots while 30 bodies lie at 1-5 blocks (living " + livingNear + ", bodies " + bodiesNear + ")"); }
        { double[] living = {100, 200, 300}, bodies = {1, 4, 9, 16, 25};
          double bc = BattleMath.bodyCutoffSq(living.clone(), 3, bodies.clone(), 5, 6, 576);
          int bodiesNear = 0; for (double d : bodies) if (d < 576 && d <= bc) bodiesNear++;
          check(bodiesNear == 3, "bodyCutoffSq: bodies take only the slots the living leave (3 of 6), nearest first: " + bodiesNear); }
        check(BattleMath.bodyCutoffSq(new double[]{1, 2}, 2, new double[]{1, 2, 3}, 3, 48, 576) == 576, "bodyCutoffSq: with slots to spare every body in range is near");
        check(BattleMath.bodyCutoffSq(new double[]{1, 2, 700}, 3, new double[]{1}, 1, 2, 576) < 0, "bodyCutoffSq: no slot left means no body is near");
        check(BattleMath.reliefCutoffSq(new double[]{9, 1, 4, 700}, 4, 2, 576) == 4 && BattleMath.reliefCutoffSq(new double[]{9, 1}, 2, 48, 576) == 576, "reliefCutoffSq: the cap-th nearest in range, or nearSq under the cap");
        // Task 7 fix 1 (I4): the hurt tint is vanilla's while hurt or falling, and gone once the body lies
        check(BattleMath.corpseHurtTint(0, 5) && !BattleMath.corpseHurtTint(0, 0), "corpseHurtTint: a living troop tints only while hurt");
        check(BattleMath.corpseHurtTint(1, 10) && BattleMath.corpseHurtTint(20, 0), "corpseHurtTint: red through the 20-tick fall, as vanilla");
        check(!BattleMath.corpseHurtTint(21, 0) && !BattleMath.corpseHurtTint(60, 3) && !BattleMath.corpseHurtTint(119, 0), "corpseHurtTint: no red once the body lies");
        check(BattleMath.nextBleedBeat(100, 100, 20) == 120, "nextBleedBeat: a beat on time schedules the next one interval on");
        check(BattleMath.nextBleedBeat(100, 135, 20) == 155, "nextBleedBeat: a beat held back by the hurt cooldown waits a full interval, no catch-up");
        { long next = 100; int beats = 0; for (long t = 100; t < 200; t++) if (t >= next && t >= 130) { next = BattleMath.nextBleedBeat(next, t, 20); beats++; if (beats > 1) check(next - t == 20, "nextBleedBeat: beats stay 20 ticks apart"); }
          check(beats == 4, "nextBleedBeat: 70 ticks after a 30-tick hold give 4 beats (130, 150, 170, 190), not a burst"); }
        System.out.println("BattleMathCheck: " + checks + " checks passed");
    }
}
