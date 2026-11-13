package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The battle map's plans (pure, no Minecraft): what each team's players asked their allied bots to do. A point plan names a capture
 *  point and a job; an order sends one army to a job of its own (at a point, at a place, or following its setter). Both are kept per
 *  team, and {@link #assign} turns them into one job per bot. Everything that can go stale (a point index a map switch made invalid,
 *  an army that left) is validated at assign time rather than trusted, so the server never has to chase plans as armies come and go.
 *  {@link #jobFor} and {@link #assign} are pure queries: the bot hook may call them every tick. */
public final class BattlePlan {
    public enum Job { ATTACK, DEFEND, HOLD, FOLLOW }
    public record PointPlan(int point, Job job, String setter, long tick) {}
    /** point -1 means a place at (x, z). */
    public record Order(String army, Job job, int point, double x, double z, String setter, long tick) {}
    /** current = the point index the army's present target sits on (-1 none). Assignment treats a plan on that point as half as far, so bots do not swap plans as they move. */
    public record Army(String id, int team, double x, double z, boolean bot, boolean retreating, int current) {}
    public record Assignment(Job job, int point, double x, double z, boolean direct) {}

    /** team -> point -> plan. LinkedHashMaps, and a re-set moves the entry to the end, so insertion order is recency (the later call wins an equal tick). */
    private final Map<Integer, LinkedHashMap<Integer, PointPlan>> plans = new TreeMap<>();
    private final Map<Integer, LinkedHashMap<String, Order>> orders = new TreeMap<>();

    /** The higher tick wins; on an equal tick the later call wins; a null job clears the point. */
    public void setPoint(int team, int point, Job job, String setter, long tick) {
        var m = plans.computeIfAbsent(team, t -> new LinkedHashMap<>());
        var old = m.get(point);
        if (job == null) { m.remove(point); return; }
        if (old != null && old.tick() > tick) return;
        m.remove(point);
        m.put(point, new PointPlan(point, job, setter, tick));
    }

    /** Same rule as {@link #setPoint}, keyed by army; point -1 sends the army to (x, z). */
    public void setOrder(int team, String army, Job job, int point, double x, double z, String setter, long tick) {
        var m = orders.computeIfAbsent(team, t -> new LinkedHashMap<>());
        var old = m.get(army);
        if (job == null) { m.remove(army); return; }
        if (old != null && old.tick() > tick) return;
        m.remove(army);
        m.put(army, new Order(army, job, point, x, z, setter, tick));
    }

    /** An army left or a player took its seat: its direct order goes, the point plans stay. */
    public void dropArmy(String army) { for (var m : orders.values()) m.remove(army); }

    public void clear() { plans.clear(); orders.clear(); }

    /** An Attack plan on a point the team now owns and nobody contests has done its job, so it becomes Defend (same setter and tick). */
    public List<Integer> update(int[] owners, boolean[] contested) {
        List<Integer> changed = new ArrayList<>();
        for (var e : plans.entrySet()) {
            for (var it : new ArrayList<>(e.getValue().values())) {
                int p = it.point();
                if (it.job() != Job.ATTACK || p < 0 || p >= owners.length || owners[p] != e.getKey() || p < contested.length && contested[p]) continue;
                e.getValue().replace(p, new PointPlan(p, Job.DEFEND, it.setter(), it.tick()));
                if (!changed.contains(p)) changed.add(p);
            }
        }
        return changed;
    }

    /** Newest first: by tick, and among equal ticks by who was set last. */
    public List<PointPlan> points(int team) {
        var m = plans.get(team);
        List<PointPlan> l = new ArrayList<>(m == null ? List.of() : m.values());
        java.util.Collections.reverse(l);
        l.sort(Comparator.comparingLong(PointPlan::tick).reversed()); // stable, so the reversal keeps the tie order
        return l;
    }

    public List<Order> orders(int team) {
        var m = orders.get(team);
        return new ArrayList<>(m == null ? List.of() : m.values());
    }

    /** One job per bot of the team. Direct orders first (those bots leave the point split); then the point plans newest first, each taking
     *  the nearest free bot; bots left over join the nearest Attack plan, else Defend, else Hold. Retreating bots and humans get nothing. */
    public Map<String, Assignment> assign(int team, List<Army> armies, double[] px, double[] pz) {
        Map<String, Assignment> out = new HashMap<>();
        List<Army> free = new ArrayList<>();
        var direct = orders.get(team);
        int n = Math.min(px.length, pz.length);
        for (Army a : armies) {
            if (a.team() != team || !a.bot() || a.retreating()) continue;
            Order o = direct == null ? null : direct.get(a.id());
            if (o != null && (o.point() < 0 || o.point() < n)) {
                out.put(a.id(), o.point() < 0 ? new Assignment(o.job(), -1, o.x(), o.z(), true) : new Assignment(o.job(), o.point(), px[o.point()], pz[o.point()], true));
            } else free.add(a);
        }
        List<PointPlan> valid = new ArrayList<>();
        for (PointPlan p : points(team)) if (p.point() >= 0 && p.point() < n) valid.add(p);
        if (valid.isEmpty()) return out;
        for (PointPlan p : valid) {
            Army best = nearest(free, p.point(), px[p.point()], pz[p.point()]);
            if (best == null) break;
            free.remove(best);
            out.put(best.id(), new Assignment(p.job(), p.point(), px[p.point()], pz[p.point()], false));
        }
        for (Army a : free) {
            PointPlan pick = null;
            for (Job j : new Job[] {Job.ATTACK, Job.DEFEND, Job.HOLD}) {
                double bd = Double.MAX_VALUE;
                for (PointPlan p : valid) {
                    if (p.job() != j) continue;
                    double d = dist(a, p.point(), px[p.point()], pz[p.point()]);
                    if (d < bd) { bd = d; pick = p; }
                }
                if (pick != null) break;
            }
            out.put(a.id(), new Assignment(pick.job(), pick.point(), px[pick.point()], pz[pick.point()], false));
        }
        return out;
    }

    /** The army's job, or null when no plan reaches it (so it keeps its own plan). */
    public Assignment jobFor(String army, int team, List<Army> armies, double[] px, double[] pz) { return assign(team, armies, px, pz).get(army); }

    private static Army nearest(List<Army> l, int point, double x, double z) {
        Army best = null;
        double bd = Double.MAX_VALUE;
        for (Army a : l) { double d = dist(a, point, x, z); if (d < bd) { bd = d; best = a; } }
        return best;
    }

    /** Distance to a plan's point, halved for an army already heading there (the stickiness between decisions). */
    private static double dist(Army a, int point, double x, double z) { return Math.hypot(a.x() - x, a.z() - z) * (a.current() == point ? 0.5 : 1); }

    /** What the bot says when it takes the job; a null assignment means its plan was cleared. */
    public static String ackLine(Assignment a, String[] pointIds) {
        if (a == null) return "back to my own plan";
        if (a.job() == Job.FOLLOW) return "following you";
        if (a.point() < 0) return switch (a.job()) { case ATTACK -> "pushing there"; case DEFEND -> "guarding there"; default -> "holding there"; };
        String id = pointIds != null && a.point() < pointIds.length ? pointIds[a.point()] : "?";
        return switch (a.job()) { case ATTACK -> "on it, taking " + id; case DEFEND -> "defending " + id; default -> "holding " + id; };
    }
}
