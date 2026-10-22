package gg.gravesandglory;

import java.util.List;

/** Pure Phase 1 battle maths: projectile travel, HUD loss detection and rout maths. Never shipped. */
public final class BattleMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
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
        double[] rx={0,.5,-.5}, rz={-5,-5,-5}, fx={0,.5,-.5}, fz={5,5,5};
        check(BattleMath.flanked(0, rx, rz, 110, 3) && !BattleMath.flanked(0, fx, fz, 110, 3) && !BattleMath.flanked(0, new double[]{0,0}, new double[]{-5,-5}, 110, 3), "three rear attackers flank; front or two do not");
        check(BattleMath.shouldRout(.2, false, .25, .6) && BattleMath.shouldRout(.5, true, .25, .6), "weak routs; flanked half-strength routs");
        check(!BattleMath.shouldRout(.5, false, .25, .6) && !BattleMath.shouldRout(.9, true, .25, .6), "half-strength unflanked and flanked fresh hold");
        double[] ax={0,-40}, az={0,-140};
        check(BattleMath.nearestAnchor(40, 0, -108, 0, ax, az) == 0, "centre point is nearer the front than base");
        check(BattleMath.nearestAnchor(-100, 0, -108, 0, ax, az) == -1, "base wins when the fight is at home");
        check(BattleMath.nearestAnchor(40, 0, -108, 0, new double[0], new double[0]) == -1, "no anchors means base");
        check(BattleMath.spacing(0.6) == 1.3 && Math.abs(BattleMath.spacing(1.4) - 1.7) < 1e-9 && Math.abs(BattleMath.spacing(1.95) - 2.25) < 1e-9, "spacing: a husk or illager takes 1.3, an iron golem 1.7, a ravager 2.25");
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
        check(BattleMath.shieldwallVsArchers(4,0,3,true) && !BattleMath.shieldwallVsArchers(4,1,3,true) && !BattleMath.shieldwallVsArchers(4,0,3,false), "shieldwall only vs archers, only with shields");
        double reach=2.8*2.8; // a knight's sword reach, squared
        check(!BattleMath.raiseShield("block","attack",2*2,reach,true) && !BattleMath.raiseShield("block","attack",reach,reach,true), "block: the shield comes down with the enemy in sword reach, so the knight can swing");
        check(BattleMath.raiseShield("block","attack",4*4,reach,true) && !BattleMath.raiseShield("block","attack",7*7,reach,true), "block: the shield is up on the approach (enemy at 4 blocks), not yet at 7");
        check(BattleMath.raiseShield("block","attack",5.9*5.9,reach,true) && !BattleMath.raiseShield("block","attack",6*6,reach,true), "block: the shield rises inside 6 blocks, not at 6");
        check(BattleMath.raiseShield("shieldwall","attack",2*2,reach,true) && BattleMath.raiseShield("shieldwall","hold",20*20,reach,true), "shieldwall braces in reach and at range");
        check(!BattleMath.raiseShield("block","charge",4*4,reach,true) && !BattleMath.raiseShield("shieldwall","charge",2*2,reach,true) && !BattleMath.raiseShield("shieldwall","retreat",4*4,reach,true), "no shield on a charge or a retreat");
        check(!BattleMath.raiseShield("block","attack",4*4,reach,false) && !BattleMath.raiseShield("shieldwall","hold",4*4,reach,false) && !BattleMath.raiseShield("loose","attack",4*4,reach,true), "no shield with no enemy, or outside block and shieldwall");
        check(MatchStats.winRate(7,10) == .7 && MatchStats.winRate(0,0) == 0, "win rate");
        var st=new MatchStats.SquadStats(); st.goldEarned+=5; check(st.goldEarned == 5, "stats accumulate");
        var a=new MatchStats.SquadStats(); a.kills=4; a.objectiveSeconds=10; var b2=new MatchStats.SquadStats(); b2.kills=4; b2.objectiveSeconds=30;
        var idle=new MatchStats.SquadStats(); var holder=new MatchStats.SquadStats(); holder.objectiveSeconds=1; var killer=new MatchStats.SquadStats(); killer.kills=1;
        check(MatchStats.mvp(List.of(a,b2)) == 1 && MatchStats.mvp(List.of()) == -1 && MatchStats.mvp(List.of(idle,new MatchStats.SquadStats())) == -1
            && MatchStats.mvp(List.of(idle,holder)) == 1 && MatchStats.mvp(List.of(idle,killer)) == 1, "mvp by kills then objective time; a kill alone or a point alone crowns; a dead tie at nothing crowns nobody");
        int[][] ps=new int[2][2]; MatchStats.teamPointSeconds(ps,new int[]{-1,1,0,2}); MatchStats.teamPointSeconds(new int[2][0],new int[]{0,1});
        check(ps[0][0] == 0 && ps[0][1] == 0 && ps[1][0] == 0 && ps[1][1] == 1, "point seconds skip an unheld point, an unknown team and a point past the rows");
        for (int points = 0; points <= 5; points++) check(BattleMath.pointIncome(points, 3, 0, 9) == Math.min(9, points * 3), "point income with no class bonus is the Phase 1 rule min(cap, held x pointGold), " + points + " held");
        check(BattleMath.pointIncome(1, 3, 2, 9) == 5 && BattleMath.pointIncome(2, 3, 2, 9) == 10 && BattleMath.pointIncome(3, 3, 2, 9) == 15 && BattleMath.pointIncome(5, 3, 2, 9) == 15, "the Quartermaster's +2: one held point pays 5, and the cap still stands for three points (15)");
        check(BattleMath.pointIncome(0, 3, 2, 9) == 0 && BattleMath.pointIncome(4, 0, 2, 9) == 8 && BattleMath.pointIncome(9, 0, 2, 9) == 9, "nothing held pays nothing; a sheet with no point gold keeps its plain cap");
        check(BattleMath.classCost(70, 1 - 0.2) == 56 && BattleMath.classCost(22, 1) == 22 && BattleMath.classCost(45, 0.8) == 36 && BattleMath.classCost(25, 0.9) == 23, "the Warlock's 20% off: a 70-gold Sunweaver costs 56; costs round half up");
        check(BattleMath.trainingTicks(4.0, 1 - 0.2) == 64 && BattleMath.trainingTicks(4.0, 1) == 80 && BattleMath.trainingTicks(3.5, 0.8) == 56 && BattleMath.trainingTicks(9.0, 0.8) == 144, "the Marshal's drill: a knight's 80 ticks become 64");
        // Graveborn: the share of strength a group breaks at (fearless 12%, dread 40%, the sheet's 25% otherwise; fearless wins over dread)
        check(BattleMath.routShare(.25, 5, 10, .5, .12, false, .4) == .12 && BattleMath.routShare(.25, 10, 10, .5, .12, false, .4) == .12, "a group at least half fearless breaks at the fearless share (5 of 10, 10 of 10)");
        check(BattleMath.routShare(.25, 4, 10, .5, .12, false, .4) == .25 && BattleMath.routShare(.25, 0, 10, .5, .12, false, .4) == .25 && BattleMath.routShare(.25, 1, 3, .5, .12, false, .4) == .25 && BattleMath.routShare(.25, 2, 3, .5, .12, false, .4) == .12, "under half fearless it keeps the sheet's share (4 of 10, none, 1 of 3), over half does not (2 of 3)");
        check(BattleMath.routShare(.25, 0, 10, .5, .12, true, .4) == .4 && BattleMath.routShare(.25, 4, 10, .5, .12, true, .4) == .4, "a dread troop in reach raises the share to the dread share for a group that is not half fearless");
        check(BattleMath.routShare(.25, 10, 10, .5, .12, true, .4) == .12 && BattleMath.routShare(.25, 5, 10, .5, .12, true, .4) == .12, "fearless wins over dread");
        check(BattleMath.routShare(.25, 0, 0, .5, .12, false, .4) == .25 && BattleMath.routShare(.25, 0, 0, .5, .12, true, .4) == .4, "a group with no living member is not fearless");
        check(BattleMath.shouldRout(.15, false, BattleMath.routShare(.25, 10, 10, .5, .12, false, .4), .6) == false && BattleMath.shouldRout(.10, false, BattleMath.routShare(.25, 10, 10, .5, .12, false, .4), .6) && BattleMath.shouldRout(.15, false, BattleMath.routShare(.25, 0, 10, .5, .12, false, .4), .6), "ten Risen at 15% hold and break at 10%; a knight group at 15% breaks");
        check(BattleMath.shouldRout(.35, false, BattleMath.routShare(.25, 0, 10, .5, .12, true, .4), .6) && !BattleMath.shouldRout(.35, false, BattleMath.routShare(.25, 10, 10, .5, .12, true, .4), .6), "a group at 35% breaks beside dread, the fearless one does not");
        System.out.println("BattleMathCheck: " + checks + " checks passed");
    }
}
