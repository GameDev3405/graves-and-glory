package gg.gravesandglory;

import java.util.List;

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
    static boolean slides(double[][] prev, double[][] grid, int[] at) {
        for (int i = 0; i < at.length; i++) {
            if (prev[i] == null) continue;
            if (Math.abs(grid[at[i]][0] - prev[i][0]) > 1 + 1e-9 || Math.abs(grid[at[i]][1] - prev[i][1]) > 1 + 1e-9) return false;
            for (int j = 0; j < at.length; j++)
                if (prev[j] != null && Math.abs(grid[at[i]][1] - grid[at[j]][1]) < 1e-9 && (grid[at[i]][0] - grid[at[j]][0]) * (prev[i][0] - prev[j][0]) < -1e-12) return false;
        }
        return true;
    }
    static boolean permutation(int[] at) {
        boolean[] seen = new boolean[at.length];
        for (int a : at) { if (a < 0 || a >= at.length || seen[a]) return false; seen[a] = true; }
        return true;
    }
    public static void main(String[] args) {
        check(BattleMath.travelTicks(22, 2.2, 2, 14) == 10, "arrow at max range lands in 10 ticks");
        check(BattleMath.travelTicks(1, 2.2, 2, 14) == 2, "point-blank shots still travel the minimum");
        check(BattleMath.travelTicks(400, 1.2, 2, 14) == 14, "travel is capped");
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
        int[] after = BattleMath.keepPlaces(lost, s12, g12);
        check(permutation(after) && moved(lost, g12, after) == 2 && slides(lost, g12, after) && Math.abs(g12[after[9]][0] - g13[5][0]) < 1e-9 && Math.abs(g12[after[9]][1]) < 1e-9, "a front man falls before a rear man: the man behind him steps up, the next of the rank closes behind; nobody else moves");
        double[][] lost4 = new double[12][];
        for (int i = 0, j = 0; i < 13; i++) if (i != 4) lost4[j++] = g13[i];
        int[] after4 = BattleMath.keepPlaces(lost4, s12, g12);
        check(permutation(after4) && moved(lost4, g12, after4) == 3 && slides(lost4, g12, after4) && Math.abs(g12[after4[11]][0] - g13[5][0]) < 1e-9 && Math.abs(g12[after4[9]][0] - g13[4][0]) < 1e-9 && Math.abs(g12[after4[10]][1]) < 1e-9,
            "a front man falls before the flank of the rear rank: the man behind steps up, the rank slides one place toward the gap (three short moves, nobody passes anybody)");
        double[][] g5 = BattleMath.holdGrid(s5, 5, false), g4 = BattleMath.holdGrid(s4, 5, false);
        double[][] lostOne = {g5[0], g5[1], g5[3], g5[4]};
        int[] one4 = BattleMath.keepPlaces(lostOne, s4, g4);
        check(permutation(one4) && moved(lostOne, g4, one4) == 2 && slides(lostOne, g4, one4) && Math.abs(g4[one4[1]][0] - g5[2][0]) < 1e-9 && Math.abs(g4[one4[0]][0] - g5[1][0]) < 1e-9, "one rank, its middle man falls: the rank slides one place in from the side whose flank place is gone, the rest stand");
        double[][] grown = java.util.Arrays.copyOf(g12, 13);
        int[] arrival = BattleMath.keepPlaces(grown, s13, g13);
        check(permutation(arrival) && moved(grown, g13, arrival) == 0, "a man joins: everyone keeps his place, he takes the new one");
        double[][] fresh = new double[12][];
        for (int i = 0; i < 12; i++) fresh[i] = new double[]{100 + i, 100};
        int[] reformed = BattleMath.keepPlaces(fresh, s12, g12);
        boolean identity = true;
        for (int i = 0; i < 12; i++) identity &= reformed[i] == i;
        check(identity, "a new order or shape (no place kept): the units take the places in slot order");
        // the widest wall man falls and the wall's cell narrows (1.0 to 0.7): every man keeps his file and rank, only the spacing changes
        double[] wide = w13.clone(); wide[12] = 0.9;
        double[][] wideLayout = BattleMath.holdLayout(s13, wide, 1.8, 0.7, 10, false);
        int[] narrowed = BattleMath.keepPlaces(java.util.Arrays.copyOf(g13, 12), s12, g12);
        boolean kept = permutation(narrowed) && Math.abs(wideLayout[0][1] - 1.0) < 1e-9;
        for (int i = 0; i < 12; i++) kept &= Math.abs(l12[narrowed[i]][0] - wideLayout[i][0] * 0.7) < 1e-9 && Math.abs(l12[narrowed[i]][1] - wideLayout[i][1] * 0.7) < 1e-9;
        check(kept, "the widest wall man (at the rear) falls and the cell narrows: nobody changes file or rank, the wall closes in place");
        double[] wideFront = w13.clone(); wideFront[4] = 0.9;
        double[] narrow = new double[12]; java.util.Arrays.fill(narrow, .6);
        double[][] narrowLayout = BattleMath.holdLayout(s12, narrow, 1.8, 0.7, 10, false);
        check(Math.abs(BattleMath.holdLayout(s13, wideFront, 1.8, 0.7, 10, false)[0][1] - 1.0) < 1e-9 && java.util.Arrays.deepEquals(narrowLayout, l12) && moved(lost4, g12, after4) == 3,
            "the widest wall man (in the front) falls and the cell narrows: the same three short moves as at one cell, by file and rank");
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
        check(BattleMath.laneTarget(0,0,5) == 0 && BattleMath.laneTarget(1,0,5) == 1 && BattleMath.laneTarget(2,0,5) == 2
            && BattleMath.laneTarget(0,1,5) == 4 && BattleMath.laneTarget(1,1,5) == 3 && BattleMath.laneTarget(2,1,5) == 2
            && BattleMath.laneTarget(0,1,3) == 2 && BattleMath.laneTarget(1,1,3) == 1 && BattleMath.laneTarget(2,1,3) == 0, "Dusk lanes mirror Dawn's: A,B,C against E,D,C on five points, C,B,A on three");
        check(BattleMath.laneTarget(3,0,3) == 0 && BattleMath.laneTarget(4,0,3) == 1 && BattleMath.laneTarget(3,1,3) == 2
            && BattleMath.laneTarget(5,1,5) == 4 && BattleMath.laneTarget(7,1,5) == 2, "a lane past the last point wraps round (lane 3 on three points is lane 0), mirrored for Dusk");
        check(!BattleMath.timedCharge(60,20,8,1.3,18) && BattleMath.timedCharge(15,20,8,1.3,18) && BattleMath.timedCharge(60,20,8,1.5,0), "hard waits for range; normal ignores distance");
        check(!BattleMath.timedCharge(15,8,20,1.3,18), "an outnumbered army does not charge even with the enemy in range");
        check(java.util.Arrays.stream(Spec.FORMATIONS).noneMatch(f -> f.id().equals("shieldwall")) && Spec.FORMATIONS.length == 4, "the Shieldwall formation is gone: the cycle is Compact, Line, Wedge and Loose, and Hold builds the wall");
        double reach=2.8*2.8; // a knight's sword reach, squared
        check(!BattleMath.raiseShield("block","attack",2*2,reach,true) && !BattleMath.raiseShield("block","attack",reach,reach,true), "block: the shield comes down with the enemy in sword reach, so the knight can swing");
        check(BattleMath.raiseShield("block","attack",4*4,reach,true) && !BattleMath.raiseShield("block","attack",7*7,reach,true), "block: the shield is up on the approach (enemy at 4 blocks), not yet at 7");
        check(BattleMath.raiseShield("block","attack",5.9*5.9,reach,true) && !BattleMath.raiseShield("block","attack",6*6,reach,true), "block: the shield rises inside 6 blocks, not at 6");
        check(!BattleMath.raiseShield("shieldwall","attack",4*4,reach,true) && !BattleMath.raiseShield("shieldwall","hold",20*20,reach,true), "a stale shieldwall id raises nothing of its own: the formation is gone");
        check(!BattleMath.raiseShield("block","charge",4*4,reach,true) && !BattleMath.raiseShield("block","charge",5*5,reach,true) && !BattleMath.raiseShield("block","retreat",4*4,reach,true), "no shield on a charge or a retreat");
        check(!BattleMath.raiseShield("block","attack",4*4,reach,false) && !BattleMath.raiseShield("block","hold",4*4,reach,false) && !BattleMath.raiseShield("loose","attack",4*4,reach,true), "no shield with no enemy, or outside block");
        check(MatchStats.winRate(7,10) == .7 && MatchStats.winRate(0,0) == 0, "win rate");
        var st=new MatchStats.SquadStats(); st.goldEarned+=5; check(st.goldEarned == 5, "stats accumulate");
        var a=new MatchStats.SquadStats(); a.kills=4; a.objectiveSeconds=10; var b2=new MatchStats.SquadStats(); b2.kills=4; b2.objectiveSeconds=30;
        var idle=new MatchStats.SquadStats(); var holder=new MatchStats.SquadStats(); holder.objectiveSeconds=1; var killer=new MatchStats.SquadStats(); killer.kills=1;
        check(MatchStats.mvp(List.of(a,b2)) == 1 && MatchStats.mvp(List.of()) == -1 && MatchStats.mvp(List.of(idle,new MatchStats.SquadStats())) == -1
            && MatchStats.mvp(List.of(idle,holder)) == 1 && MatchStats.mvp(List.of(idle,killer)) == 1, "mvp by kills then objective time; a kill alone or a point alone crowns; a dead tie at nothing crowns nobody");
        // the report's levels line (in-match progression design 7): the level reached, how many reached level 3, the gear bought
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",2,0,1,1),"Order Squire","Knight").equals("reached Knight / no gear bought"), "level 2 with no level 3s: no count, no gear");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",3,2,1,2),"Order Squire","Knight-Captain").equals("reached Knight-Captain / 2 at level 3 / Armour II"), "armour only");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",3,1,2,1),"Order Squire","Knight-Captain").equals("reached Knight-Captain / 1 at level 3 / Weapon II") && MatchStats.levelText(new MatchStats.LevelRow("knight",3,1,3,3),"Order Squire","Knight-Captain").endsWith("Weapon III, Armour III"), "weapon only; both, weapon first");
        check(MatchStats.levelText(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver",null).equals("reached level 3 / 1 at level 3 / Weapon III") && MatchStats.levelText(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver","").equals("reached level 3 / 1 at level 3 / Weapon III") && MatchStats.levelText(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver","Sunweaver").equals("reached level 3 / 1 at level 3 / Weapon III"), "a troop with no level names (null, empty or its own name) reads level 3");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",1,0,1,1),"Order Squire","Order Squire").equals("no level ups / no gear bought") && MatchStats.levelText(new MatchStats.LevelRow("knight",0,0,0,0),"Order Squire",null).equals("no level ups / no gear bought"), "no level ups reads the same under any reached name; tier 0 is no gear");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",4,1,4,1),"Order Squire","Order Squire").startsWith("reached level 4 / 1 at level 3 / Weapon "), "a level above 3 (no sheet has one) still reads, without throwing");
        // one Roman numeral helper for every tier and level the UI prints (the barracks pages, the refusal copy, the results page): I to III as always, nothing for 0 or less, true numerals above 3
        check(MatchStats.roman(1).equals("I") && MatchStats.roman(2).equals("II") && MatchStats.roman(3).equals("III"), "roman: I, II, III");
        check(MatchStats.roman(0).isEmpty() && MatchStats.roman(-2).isEmpty(), "roman: nothing for a tier of 0 or less (a House with no trait tier draws no numeral)");
        check(MatchStats.roman(4).equals("IV") && MatchStats.roman(5).equals("V") && MatchStats.roman(9).equals("IX") && MatchStats.roman(14).equals("XIV") && MatchStats.roman(40).equals("XL") && MatchStats.roman(1994).equals("MCMXCIV"), "roman: a tier above 3 (a longer gearTiers sheet) reads as a numeral, never empty and never a digit");
        check(MatchStats.levelText(new MatchStats.LevelRow("knight",3,1,4,4),"Order Squire","Knight-Captain").endsWith("Weapon IV, Armour IV") && MatchStats.levelShort(new MatchStats.LevelRow("knight",3,1,4,4),"Order Squire","Knight-Captain").endsWith("W IV A IV"), "roman: the results page prints tier 4 gear as IV in both forms");
        // the same line in the report's two-column form (more than three troop types): "Knight-Captain x2 / W III A III"
        check(MatchStats.levelShort(new MatchStats.LevelRow("knight",3,2,3,3),"Order Squire","Knight-Captain").equals("Knight-Captain x2 / W III A III") && MatchStats.levelShort(new MatchStats.LevelRow("knight",2,0,1,1),"Order Squire","Knight").equals("Knight / no gear"), "short form: level name, x level-3 count, W and A tiers; no count without level 3s");
        check(MatchStats.levelShort(new MatchStats.LevelRow("archer",3,12,2,1),"Order Archer","Sergeant-Archer").equals("Sergeant-Archer x12 / W II") && MatchStats.levelShort(new MatchStats.LevelRow("archer",3,1,1,3),"Order Archer","Sergeant-Archer").equals("Sergeant-Archer x1 / A III"), "short form: one track bought");
        check(MatchStats.levelShort(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver","Sunweaver").equals("level 3 x1 / W III") && MatchStats.levelShort(new MatchStats.LevelRow("mage",3,1,3,1),"Sunweaver",null).equals("level 3 x1 / W III") && MatchStats.levelShort(new MatchStats.LevelRow("knight",1,0,1,1),"Order Squire","Order Squire").equals("no level ups / no gear"), "short form: a troop with no level names reads level 3; no level ups");
        int[][] ps=new int[2][2]; MatchStats.teamPointSeconds(ps,new int[]{-1,1,0,2}); MatchStats.teamPointSeconds(new int[2][0],new int[]{0,1});
        check(ps[0][0] == 0 && ps[0][1] == 0 && ps[1][0] == 0 && ps[1][1] == 1, "point seconds skip an unheld point, an unknown team and a point past the rows");
        for (int points = 0; points <= 5; points++) check(BattleMath.pointIncome(points, 3, 0, 9) == Math.min(9, points * 3), "point income with no class bonus is the Phase 1 rule min(cap, held x pointGold), " + points + " held");
        check(BattleMath.pointIncome(1, 3, 2, 9) == 5 && BattleMath.pointIncome(2, 3, 2, 9) == 10 && BattleMath.pointIncome(3, 3, 2, 9) == 15 && BattleMath.pointIncome(5, 3, 2, 9) == 15, "the Quartermaster's +2: one held point pays 5, and the cap still stands for three points (15)");
        check(BattleMath.pointIncome(0, 3, 2, 9) == 0 && BattleMath.pointIncome(4, 0, 2, 9) == 8 && BattleMath.pointIncome(9, 0, 2, 9) == 9, "nothing held pays nothing; a sheet with no point gold keeps its plain cap");
        check(BattleMath.classCost(70, 1 - 0.2) == 56 && BattleMath.classCost(22, 1) == 22 && BattleMath.classCost(45, 0.8) == 36 && BattleMath.classCost(25, 0.9) == 23, "the Warlock's 20% off: a 70-gold Sunweaver costs 56; costs round half up");
        check(BattleMath.trainingTicks(4.0, 1 - 0.2) == 64 && BattleMath.trainingTicks(4.0, 1) == 80 && BattleMath.trainingTicks(3.5, 0.8) == 56 && BattleMath.trainingTicks(9.0, 0.8) == 144, "the Marshal's drill: a knight's 80 ticks become 64");
        System.out.println("BattleMathCheck: " + checks + " checks passed");
    }
}
