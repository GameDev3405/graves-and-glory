package gg.gravesandglory;

import gg.gravesandglory.BattlePlan.*;
import java.util.List;

/** Battle map plan table (Task 1): nearest-bot assignment per plan, newest plan for a lone bot, direct orders, retreat and human exclusion,
 *  stale point indices and unknown armies, attack-to-defend, team separation and purity of the queries. Checked by javac alone, never shipped. */
public final class BattlePlanCheck {
    static int checks;
    static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); checks++; }
    static final double[] PX = {0, 100, 200}, PZ = {0, 0, 0};
    static Army bot(String id, double x) { return new Army(id, 0, x, 0, true, false, -1); }
    static Army botOn(String id, double x, int current) { return new Army(id, 0, x, 0, true, false, current); }

    public static void main(String[] args) {
        { // dropFollowsBy: a leaver's Follow orders go, his other orders stay
            var p = new BattlePlan();
            p.setOrder(0, "a", Job.FOLLOW, -1, 0, 0, "p1", 1); p.setOrder(0, "b", Job.HOLD, -1, 5, 5, "p1", 2); p.setOrder(0, "c", Job.FOLLOW, -1, 0, 0, "p2", 3);
            p.dropFollowsBy("p1");
            check(p.orders(0).size() == 2 && p.orders(0).stream().noneMatch(o -> o.army().equals("a")), "p1's follow gone, p1's hold and p2's follow stay");
        }
        { // assignNearestPerPlan
            var p = new BattlePlan();
            p.setPoint(0, 2, Job.ATTACK, "p1", 5); p.setPoint(0, 0, Job.DEFEND, "p1", 3);
            var m = p.assign(0, List.of(bot("a", 190), bot("b", 10)), PX, PZ);
            check(m.get("a").job() == Job.ATTACK && m.get("a").point() == 2, "a attacks 2");
            check(m.get("b").job() == Job.DEFEND && m.get("b").point() == 0, "b defends 0");
        }
        { // extrasGoToAttackFirst
            var p = new BattlePlan();
            p.setPoint(0, 2, Job.ATTACK, "p1", 5); p.setPoint(0, 0, Job.HOLD, "p1", 3);
            var m = p.assign(0, List.of(bot("a", 190), bot("b", 10), bot("c", 20)), PX, PZ);
            check(m.get("a").point() == 2 && m.get("b").point() == 0, "plans take nearest bots");
            check(m.get("c").job() == Job.ATTACK && m.get("c").point() == 2, "extra bot joins the attack");
        }
        { // oneBotNewestPlan
            var p = new BattlePlan();
            p.setPoint(0, 0, Job.HOLD, "p1", 1); p.setPoint(0, 1, Job.HOLD, "p1", 2); p.setPoint(0, 2, Job.DEFEND, "p1", 3); p.setPoint(0, 3, Job.ATTACK, "p1", 4);
            var m = p.assign(0, List.of(bot("a", 0)), new double[] {0, 100, 200, 300}, new double[4]);
            check(m.size() == 1 && m.get("a").point() == 3, "lone bot takes the tick-4 plan");
        }
        { // latestWins
            var p = new BattlePlan();
            p.setPoint(0, 1, Job.ATTACK, "p1", 10); p.setPoint(0, 1, Job.HOLD, "p2", 10);
            check(p.points(0).size() == 1 && p.points(0).get(0).job() == Job.HOLD && p.points(0).get(0).setter().equals("p2"), "equal tick: later call wins");
            p.setPoint(0, 1, Job.ATTACK, "p3", 9);
            check(p.points(0).get(0).setter().equals("p2"), "lower tick loses");
            p.setPoint(0, 1, Job.ATTACK, "p3", 11);
            check(p.points(0).get(0).setter().equals("p3"), "higher tick wins");
            p.setPoint(0, 1, null, "p3", 12);
            check(p.points(0).isEmpty(), "null clears");
        }
        { // directBeatsTeam
            var p = new BattlePlan();
            p.setPoint(0, 0, Job.ATTACK, "p1", 1);
            p.setOrder(0, "a", Job.HOLD, -1, 50, 60, "p1", 2);
            var a = p.jobFor("a", 0, List.of(bot("a", 0), bot("b", 5)), PX, PZ);
            check(a.equals(new Assignment(Job.HOLD, -1, 50, 60, true)), "direct order wins");
            var m = p.assign(0, List.of(bot("a", 0), bot("b", 5)), PX, PZ);
            check(m.get("b").point() == 0 && !m.get("b").direct(), "b takes the point plan alone");
        }
        { // retreatingExcluded
            var p = new BattlePlan();
            p.setPoint(0, 0, Job.ATTACK, "p1", 1); p.setPoint(0, 2, Job.ATTACK, "p1", 2);
            var m = p.assign(0, List.of(new Army("r", 0, 0, 0, true, true, -1), bot("a", 5), bot("b", 190)), PX, PZ);
            check(!m.containsKey("r") && m.size() == 2, "retreating bot gets nothing");
            check(m.get("a").point() == 0 && m.get("b").point() == 2, "others cover the plans");
        }
        { // stickyAssignment
            var p = new BattlePlan();
            p.setPoint(0, 2, Job.ATTACK, "p1", 5); p.setPoint(0, 0, Job.DEFEND, "p1", 3);
            var m = p.assign(0, List.of(botOn("a", 150, 2), botOn("b", 160, 0)), PX, PZ);
            check(m.get("a").point() == 2 && m.get("b").point() == 0, "a keeps 2 though b is a little closer");
            m = p.assign(0, List.of(botOn("a", 150, 2), botOn("b", 180, 0)), PX, PZ);
            check(m.get("b").point() == 2 && m.get("a").point() == 0, "b under half a's distance takes 2");
            m = p.assign(0, List.of(bot("a", 150), bot("b", 160)), PX, PZ);
            check(m.get("b").point() == 2, "without current the nearer bot wins");
            // the extras pass: two Attack plans, both staffed; c sits between them and keeps the one it is on
            var q = new BattlePlan();
            q.setPoint(0, 2, Job.ATTACK, "p1", 5); q.setPoint(0, 0, Job.ATTACK, "p1", 4);
            m = q.assign(0, List.of(botOn("a", 190, 2), botOn("b", 10, 0), botOn("c", 80, 2)), PX, PZ);
            check(m.get("c").point() == 2, "extra bot keeps the Attack plan it is on");
            m = q.assign(0, List.of(botOn("a", 190, 2), botOn("b", 10, 0), botOn("c", 80, -1)), PX, PZ);
            check(m.get("c").point() == 0, "without current the extra bot takes the nearer plan");
        }
        { // humansIgnored
            var p = new BattlePlan();
            p.setPoint(0, 0, Job.ATTACK, "p1", 1);
            check(p.assign(0, List.of(new Army("h", 0, 0, 0, false, false, -1)), PX, PZ).isEmpty(), "humans never assigned");
        }
        { // noBots
            var p = new BattlePlan();
            p.setPoint(0, 0, Job.ATTACK, "p1", 1);
            check(p.assign(0, List.of(), PX, PZ).isEmpty(), "no bots, no assignments");
            check(p.jobFor("x", 0, List.of(), PX, PZ) == null, "jobFor null with no bots");
        }
        { // attackBecomesDefend
            var p = new BattlePlan();
            p.setPoint(0, 1, Job.ATTACK, "p1", 7);
            check(p.update(new int[] {-1, 0}, new boolean[] {false, true}).isEmpty() && p.points(0).get(0).job() == Job.ATTACK, "contested stays attack");
            check(p.update(new int[] {-1, 1}, new boolean[] {false, false}).isEmpty(), "other team's point does not count");
            var ch = p.update(new int[] {-1, 0}, new boolean[] {false, false});
            check(ch.equals(List.of(1)) && p.points(0).get(0).job() == Job.DEFEND && p.points(0).get(0).tick() == 7 && p.points(0).get(0).setter().equals("p1"), "uncontested own point becomes defend");
        }
        { // staleIndexIgnored
            var p = new BattlePlan();
            p.setPoint(0, 7, Job.ATTACK, "p1", 9); p.setPoint(0, 1, Job.HOLD, "p1", 2);
            p.setOrder(0, "a", Job.DEFEND, 9, 0, 0, "p1", 3);
            var m = p.assign(0, List.of(bot("a", 0)), PX, PZ);
            check(m.size() == 1 && m.get("a").point() == 1, "invalid point plan and order skipped");
        }
        { // unknownArmyIgnored
            var p = new BattlePlan();
            p.setOrder(0, "zz", Job.HOLD, -1, 1, 1, "p1", 1);
            check(p.assign(0, List.of(bot("a", 0)), PX, PZ).isEmpty(), "unknown army order assigns nothing");
        }
        { // dropArmy
            var p = new BattlePlan();
            p.setOrder(0, "a", Job.HOLD, -1, 1, 1, "p1", 1); p.setOrder(0, "b", Job.HOLD, -1, 1, 1, "p1", 1); p.setPoint(0, 0, Job.HOLD, "p1", 1);
            p.dropArmy("a");
            check(p.orders(0).size() == 1 && p.orders(0).get(0).army().equals("b") && p.points(0).size() == 1, "only a's order dropped");
        }
        { // teamsSeparate
            var p = new BattlePlan();
            p.setPoint(1, 0, Job.ATTACK, "p1", 1);
            check(p.assign(0, List.of(bot("a", 0)), PX, PZ).isEmpty() && p.points(0).isEmpty(), "team 1 plan never reaches team 0");
            check(p.assign(1, List.of(new Army("c", 1, 0, 0, true, false, -1)), PX, PZ).containsKey("c"), "team 1 bot takes it");
        }
        { // jobForIsPure
            var p = new BattlePlan();
            p.setPoint(0, 0, Job.ATTACK, "p1", 1); p.setOrder(0, "a", Job.FOLLOW, -1, 3, 4, "p1", 2);
            var l = List.of(bot("a", 0), bot("b", 9));
            var pts = p.points(0); var ords = p.orders(0);
            check(p.jobFor("b", 0, l, PX, PZ).equals(p.jobFor("b", 0, l, PX, PZ)), "same answer twice");
            check(p.points(0).equals(pts) && p.orders(0).equals(ords), "tables unchanged");
        }
        { // ackCopy
            String[] ids = {"A", "B", "C"};
            check(BattlePlan.ackLine(new Assignment(Job.ATTACK, 1, 0, 0, false), ids).equals("on it, taking B"), "attack");
            check(BattlePlan.ackLine(new Assignment(Job.DEFEND, 2, 0, 0, false), ids).equals("defending C"), "defend");
            check(BattlePlan.ackLine(new Assignment(Job.HOLD, 0, 0, 0, false), ids).equals("holding A"), "hold");
            check(BattlePlan.ackLine(new Assignment(Job.FOLLOW, -1, 0, 0, true), ids).equals("following you"), "follow");
            check(BattlePlan.ackLine(new Assignment(Job.ATTACK, -1, 0, 0, true), ids).equals("pushing there"), "push");
            check(BattlePlan.ackLine(new Assignment(Job.DEFEND, -1, 0, 0, true), ids).equals("guarding there"), "guard");
            check(BattlePlan.ackLine(new Assignment(Job.HOLD, -1, 0, 0, true), ids).equals("holding there"), "hold there");
            check(BattlePlan.ackLine(null, ids).equals("back to my own plan"), "cleared");
        }
        System.out.println("BattlePlanCheck: " + checks + " checks passed");
    }
}
