package gg.gravesandglory.cmdui;

import gg.gravesandglory.Combat;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGlory;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderIntakeProbe.Snap;
import gg.gravesandglory.OrderIntakeProbeBehaviour;
import gg.gravesandglory.OrderIntakeProbeFormation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Command UI plan Task 6: what a formation shape does in the fight (FormationRules) and the charge's first-hit impact, plus the two commander and
 * map-edge checks Ruling 11 adds. Each scene starts an empty round (only this player's army) and builds synthetic squads beside it (squads outside
 * Battle.squads, ordered as bots are, through Battle.issue: Ruling 10 says the rules hold for every unit of the shape). Never included in releases.
 */
public final class FormationRulesSuite implements CommandUiChecks.Suite {
    volatile UUID id;
    volatile boolean done;
    int scene = -1; long t0;
    final List<GravesAndGlory.Unit> junk = new ArrayList<>();

    static final String[] NAMES = {"cu-form-wall-arrows", "cu-form-wall-slow", "cu-form-wall-square", "cu-form-wedge-shove", "cu-form-loose-spacing", "cu-charge-first-hit", "cu-focus-follow-respawning", "cu-focus-charge-edge", "cu-charge-unlanded-blow", "cu-form-wedge-roles"};
    static final int[] LIMITS = {620, 420, 360, 420, 120, 620, 420, 640, 700, 360};

    // scene state
    int side, zs, team; Vec3d C, P, D, D1, spec, T; String note = ""; long issuedAt, arrivedAt, switchedAt;
    List<GravesAndGlory.Unit> wall = List.of(), line = List.of(), foes = List.of(), mine = List.of();
    Object squadA, squadB;
    final Map<GravesAndGlory.Unit, Float> lastHealth = new HashMap<>();
    final Map<GravesAndGlory.Unit, Double> taken = new HashMap<>();
    final Set<GravesAndGlory.Unit> front = new HashSet<>();
    final Map<GravesAndGlory.Unit, Vec3d> start = new HashMap<>();
    final Map<GravesAndGlory.Unit, Double> pushed = new HashMap<>();
    double wallShare, lineShare, wallLow = Double.MAX_VALUE; int samples, shoves0, hits0;
    // charge-first-hit
    Combat.HitLog seen; float hp; Vec3d tPos; final List<double[]> hits = new ArrayList<>(); Vec3d hitFrom; int watch = -1; double run;

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) id = c.player.getUuid();
        if (done) { done = false; CommandUiChecks.finish(c); }
    }
    @Override public int limit() { return 7000; }
    @Override public void cleanup(MinecraftClient c) { CommandUiChecks.server(c, b -> { discard(); OrderIntakeProbeBehaviour.restore(b); }); }

    @Override public void serverTick(GravesAndGlory.Battle b) {
        if (id == null || scene >= NAMES.length) return;
        if (scene < 0) { next(b); return; }
        if (!OrderIntakeProbeBehaviour.keep(b)) { fail("round stopped at scene tick " + (now(b) - t0)); next(b); return; }
        long t = now(b) - t0;
        try { if (t > LIMITS[scene]) { fail("timed out: " + note); next(b); } else step(b, t); }
        catch (RuntimeException e) { fail("threw " + e); next(b); }
    }

    void next(GravesAndGlory.Battle b) {
        discard(); scene++; note = ""; arrivedAt = -1; switchedAt = -1; wall = List.of(); line = List.of(); foes = List.of(); mine = List.of();
        lastHealth.clear(); taken.clear(); front.clear(); start.clear(); pushed.clear(); hits.clear(); wallShare = 0; lineShare = 0; wallLow = Double.MAX_VALUE; samples = 0; watch = -1; run = 0; seen = Combat.LAST.peekLast();
        if (scene >= NAMES.length) { OrderIntakeProbeBehaviour.restore(b); done = true; return; }
        OrderIntakeProbeBehaviour.emptyRound(b, id); t0 = now(b);
    }
    void discard() { for (var u : junk) OrderIntakeProbeBehaviour.mob(u).discard(); junk.clear(); }
    void pass(boolean ok, String detail) { CommandUiChecks.report(NAMES[scene], ok, detail); }
    void fail(String detail) { pass(false, detail); }

    List<GravesAndGlory.Unit> inf(GravesAndGlory.Battle b) { return OrderIntakeProbeBehaviour.members(b, id, "infantry"); }
    Snap snap(GravesAndGlory.Battle b, String g) { return OrderIntakeProbe.snap(b, id, g); }
    static long now(GravesAndGlory.Battle b) { return OrderIntakeProbe.time(b); }
    static double flat(Vec3d a, Vec3d b) { return Math.hypot(a.x - b.x, a.z - b.z); }
    static String xz(Vec3d v) { return v == null ? "null" : String.format("%.1f, %.1f", v.x, v.z); }
    static Vec3d ground(Vec3d v) { return new Vec3d(v.x, OrderIntakeProbeBehaviour.ground(v.x, v.z), v.z); }
    float face() { return side > 0 ? -90 : 90; }
    /** A synthetic squad of this team (mine) or the other, n troops in a column across z at `at`. */
    List<GravesAndGlory.Unit> troops(GravesAndGlory.Battle b, boolean ours, Vec3d at, String troop, int n) {
        Object s = OrderIntakeProbeBehaviour.squad(ours ? team : 1 - team, false, at, "hold");
        if (ours) squadA = s; else squadB = s;
        var us = OrderIntakeProbeBehaviour.spawn(b, s, troop, at, n); junk.addAll(us); return us;
    }
    /** The base layout every scene reads at its tick 40: own team's facing, a z sign toward the arena's middle, the infantry centre, and P, a
     *  clear spot 25 ahead and 20 across from it for the synthetic squads. */
    boolean layout(GravesAndGlory.Battle b) {
        team = OrderIntakeProbeBehaviour.team(b, id); side = team == 0 ? 1 : -1;
        C = OrderIntakeProbeBehaviour.centre(inf(b));
        if (C == null) { fail("no infantry after the empty round"); return false; }
        zs = C.z > 0 ? -1 : 1; P = C.add(side * 25, 0, zs * 20); return true;
    }
    /** Sums each unit's health losses since the last tick and heals it full again, so a long volley never kills it. */
    void bleed(List<GravesAndGlory.Unit> us) {
        for (var u : us) {
            var mob = OrderIntakeProbeBehaviour.mob(u); if (!mob.isAlive()) continue;
            Float was = lastHealth.get(u); float now = mob.getHealth();
            if (was != null && was > now) taken.merge(u, (double) (was - now), Double::sum);
            mob.setHealth(mob.getMaxHealth()); lastHealth.put(u, mob.getMaxHealth());
        }
    }
    static int quadrant(double yaw) { return (int) (((Math.round(yaw / 90.0) % 4) + 4) % 4); }

    void step(GravesAndGlory.Battle b, long t) {
        switch (scene) {
            case 0 -> wallArrows(b, t);
            case 1 -> wallSlow(b, t);
            case 2 -> wallSquare(b, t);
            case 3 -> wedgeShove(b, t);
            case 4 -> looseSpacing(b, t);
            case 5 -> chargeFirstHit(b, t);
            case 6 -> followRespawning(b, t);
            case 7 -> chargeEdge(b, t);
            case 8 -> chargeUnlanded(b, t);
            case 9 -> wedgeRoles(b, t);
            default -> {}
        }
    }

    // Two groups of six knights on Hold, one a Shield wall (block), one a Line, 45 apart; five enemy archers 7 blocks in front of each. Over 400
    // ticks each wall front-rank man takes at most 0.4 of what each Line man takes.
    void wallArrows(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            Vec3d a1 = P, a2 = P.add(0, 0, zs * 45);
            wall = troops(b, true, a1.add(-side * 3, 0, 0), "knight", 6); Object sw = squadA;
            line = troops(b, true, a2.add(-side * 3, 0, 0), "knight", 6); Object sl = squadA;
            OrderIntakeProbeFormation.issue(b, sw, "infantry", "hold", "block", ground(a1), face(), true);
            OrderIntakeProbeFormation.issue(b, sl, "infantry", "hold", "block", ground(a2), face(), true);
            D = a1; D1 = a2;
        }
        if (t == 140) {
            foes = new ArrayList<>(troops(b, false, D.add(side * 7, 0, 0), "archer", 5));
            foes.addAll(troops(b, false, D1.add(side * 7, 0, 0), "archer", 5));
            for (var u : wall) if (OrderIntakeProbeFormation.frontRank(b, u)) front.add(u);
            bleed(wall); bleed(line);
            note = "wall " + OrderIntakeProbeFormation.formation(b, wall.get(0)) + " front rank " + front.size() + "/" + wall.size() + ", line " + OrderIntakeProbeFormation.formation(b, line.get(0));
        }
        if (t > 140) {
            bleed(wall); bleed(line);
            if (t == 540) {
                double w = 0, l = 0;
                for (var u : front) w += taken.getOrDefault(u, 0.0);
                for (var u : line) l += taken.getOrDefault(u, 0.0);
                double wPer = front.isEmpty() ? Double.MAX_VALUE : w / front.size(), lPer = l / line.size();
                double rest = 0; for (var u : wall) if (!front.contains(u)) rest += taken.getOrDefault(u, 0.0);
                pass(!front.isEmpty() && l >= 4 && wPer <= 0.4 * lPer, String.format("400 ticks of arrows: wall front-rank man %.2f (total %.1f over %d), Line man %.2f (total %.1f over %d), ratio %.3f (want 0.4 or less); wall rear %.1f; %s",
                    wPer, w, front.size(), lPer, l, line.size(), lPer <= 0 ? -1 : wPer / lPer, rest, note));
                next(b);
            }
        }
    }

    // Six knights walk 16 blocks as a Shield wall, then back as a Line: the wall's walk is 0.85 of its speed (the Line's 1.0).
    void wallSlow(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            wall = troops(b, true, P, "knight", 6);
            OrderIntakeProbeFormation.issue(b, squadA, "infantry", "hold", "block", ground(P), face(), true);
        }
        if (t == 90) OrderIntakeProbeFormation.issue(b, squadA, "infantry", "hold", "block", ground(P.add(side * 16, 0, 0)), face(), true);
        if (t > 92 && t < 240) for (var u : wall) if (OrderIntakeProbeBehaviour.walking(u)) { double s = OrderIntakeProbeBehaviour.speedShare(u); if (s > 0.05) { wallShare = Math.max(wallShare, s); wallLow = Math.min(wallLow, s); samples++; } }
        if (t == 240) OrderIntakeProbeFormation.issue(b, squadA, "infantry", "hold", "block", ground(P), face(), true);
        if (t > 242 && t < 400) for (var u : wall) if (OrderIntakeProbeBehaviour.walking(u)) lineShare = Math.max(lineShare, OrderIntakeProbeBehaviour.speedShare(u));
        if (t == 400) {
            double ratio = lineShare <= 0 ? -1 : wallShare / lineShare;
            pass(samples > 0 && Math.abs(wallShare - 0.85) <= 0.01 && Math.abs(ratio - 0.85) <= 0.01, String.format("Shield wall walk speed share %.3f (lowest %.3f, %d samples), Line %.3f, ratio %.3f (want 0.85 +- 0.01)", wallShare, wallLow == Double.MAX_VALUE ? -1 : wallLow, samples, lineShare, ratio));
            next(b);
        }
    }

    // Eight knights hold a Shield wall; four enemies stand 14 blocks off on its four sides: the wall closes into a square and its shield troops face
    // at least three distinct quadrants (their FormationRules facing and their bodies).
    void wallSquare(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            wall = troops(b, true, P.add(-side * 3, 0, 0), "knight", 8);
            OrderIntakeProbeFormation.issue(b, squadA, "infantry", "hold", "block", ground(P), face(), true);
        }
        if (t == 100) {
            var out = new ArrayList<GravesAndGlory.Unit>();
            for (Vec3d off : new Vec3d[]{new Vec3d(14, 0, 0), new Vec3d(-14, 0, 0), new Vec3d(0, 0, 14), new Vec3d(0, 0, -14)}) {
                var one = troops(b, false, P.add(off), "revenant", 1); OrderIntakeProbeBehaviour.inert(one); out.addAll(one);
            }
            foes = out;
        }
        if (t == 320) {
            Set<Integer> rule = new HashSet<>(), body = new HashSet<>(); int square = 0;
            for (var u : wall) {
                if (!OrderIntakeProbeFormation.shield(u) || !OrderIntakeProbeBehaviour.mob(u).isAlive()) continue;
                if (OrderIntakeProbeFormation.square(b, u)) square++;
                rule.add(quadrant(OrderIntakeProbeFormation.facing(b, u)));
                body.add(quadrant(OrderIntakeProbeBehaviour.mob(u).getBodyYaw()));
            }
            boolean sur = OrderIntakeProbeFormation.surrounded(b, wall.get(0));
            pass(sur && rule.size() >= 3 && body.size() >= 3, String.format("surrounded %s; %d of %d in the square; facing quadrants %s, body quadrants %s (want 3 or more each)", sur, square, wall.size(), rule, body));
            next(b);
        }
    }

    // Six revenants in a Wedge walk 21 blocks through a line of six enemies (stunned, AI on, holding their slots): at least two enemies are pushed
    // more than a block sideways and the shove counter moves.
    void wedgeShove(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            Vec3d l = P.add(side * 8, 0, 0); D = l;
            foes = troops(b, false, l, "revenant", 6); OrderIntakeProbeFormation.stun(foes);
            OrderIntakeProbeFormation.issue(b, squadB, "infantry", "hold", "block", ground(l), -face(), true);
            mine = troops(b, true, P, "revenant", 6);
            OrderIntakeProbeFormation.issue(b, squadA, "infantry", "hold", "wedge", ground(P), face(), true);
        }
        if (t == 140) {
            for (var u : foes) start.put(u, OrderIntakeProbeBehaviour.mob(u).getPos());
            shoves0 = OrderIntakeProbeFormation.shoves();
            OrderIntakeProbeFormation.issue(b, squadA, "infantry", "hold", "wedge", ground(D.add(side * 13, 0, 0)), face(), true);
        }
        if (t > 140) {
            for (var u : foes) { Vec3d s = start.get(u), p = OrderIntakeProbeBehaviour.mob(u).getPos(); pushed.merge(u, Math.abs(p.z - s.z), Math::max); }
            if (t == 400) {
                int moved = 0; for (double d : pushed.values()) if (d > 1) moved++;
                int shoved = OrderIntakeProbeFormation.shoves() - shoves0;
                Vec3d c = OrderIntakeProbeBehaviour.centre(mine);
                pass(moved >= 2 && shoved >= 2, String.format("enemies pushed more than 1 block sideways %d of %d (peaks %s); shoves %d; wedge centre %s, %.1f past the line", moved, foes.size(), peaks(), shoved, xz(c), c == null ? -1 : side * (c.x - D.x)));
                next(b);
            }
        }
    }
    String peaks() { var sb = new StringBuilder("["); for (double d : pushed.values()) sb.append(String.format("%.1f ", d)); return sb.toString().trim() + "]"; }

    // Six revenants on Hold in Loose: no two slots nearer than the sheet's 3.2 (less 0.05).
    void looseSpacing(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            mine = troops(b, true, P, "revenant", 6);
            OrderIntakeProbeFormation.issue(b, squadA, "infantry", "hold", "loose", ground(P), face(), true);
        }
        if (t == 60) {
            double nearest = Double.MAX_VALUE;
            for (var u : mine) for (var v : mine) if (u != v) nearest = Math.min(nearest, flat(OrderIntakeProbeBehaviour.slot(b, u), OrderIntakeProbeBehaviour.slot(b, v)));
            pass(nearest >= 3.2 - 0.05, String.format("nearest neighbour slots %.2f apart (want 3.15 or more), formation %s", nearest, OrderIntakeProbeFormation.formation(b, mine.get(0))));
            next(b);
        }
    }

    // One knight charges an enemy archer 11 blocks ahead (stunned, bare, a deep pool of health). His first landed blow deals chargeBonusMultiplier
    // (1.3) times the same blow without it (its logged base x veteran x total, the counters C5/C6 in it) and knocks the archer 0.5 or more; his
    // second blow on it is plain (1.0).
    void chargeFirstHit(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            mine = troops(b, true, P, "knight", 1);
            foes = troops(b, false, P.add(side * 11, 0, 0), "archer", 1); OrderIntakeProbeFormation.stun(foes); OrderIntakeProbeFormation.bare(foes.get(0), 400);
        }
        if (t == 60) {
            OrderIntakeProbeFormation.charge(b, squadA, "infantry", ground(P.add(side * 18, 0, 0)), new Vec3d(side, 0, 0), face());
            hits0 = OrderIntakeProbeFormation.firstHits(); issuedAt = now(b);
            var tm = OrderIntakeProbeBehaviour.mob(foes.get(0)); hp = tm.getHealth(); tPos = tm.getPos(); seen = Combat.LAST.peekLast();
            note = "archer armour " + OrderIntakeProbeFormation.armour(foes.get(0));
        }
        if (t > 60) {
            var k = mine.get(0); var tm = OrderIntakeProbeBehaviour.mob(foes.get(0));
            if (OrderIntakeProbeFormation.order(b, k).equals("charge")) run = Math.max(run, OrderIntakeProbeFormation.run(k));
            int ka = OrderIntakeProbeFormation.id(k), ta = OrderIntakeProbeFormation.id(foes.get(0));
            List<Combat.HitLog> fresh = new ArrayList<>(); boolean past = seen == null;
            for (var h : Combat.LAST) { if (past) fresh.add(h); else if (h == seen) past = true; }
            if (!past) fresh.addAll(Combat.LAST); // the log rolled over: all of it is new
            seen = Combat.LAST.peekLast();
            float now = tm.getHealth();
            for (var h : fresh) if (h.attacker() == ka && h.target() == ta && hits.size() < 2) {
                double expected = h.base() * h.veteran() * h.total();
                hits.add(new double[]{(hp - now) / expected, hp - now, expected, 0, OrderIntakeProbeFormation.firstHits() - hits0, run, h.rows().size()});
                hitFrom = tPos; watch = 12; note += "; hit " + hits.size() + " rows " + h.rows();
            }
            if (watch > 0) { watch--; double[] last = hits.get(hits.size() - 1); last[3] = Math.max(last[3], flat(tm.getPos(), hitFrom)); }
            hp = now; tPos = tm.getPos();
            if (hits.size() == 2 && watch == 0 || t == LIMITS[scene] - 1) {
                boolean ok = hits.size() == 2 && Math.abs(hits.get(0)[0] - 1.3) <= 0.02 && Math.abs(hits.get(1)[0] - 1.0) <= 0.02 && hits.get(0)[3] >= 0.5 && hits.get(0)[5] >= 6 && OrderIntakeProbeFormation.firstHits() - hits0 == 1;
                var sb = new StringBuilder();
                for (int i = 0; i < hits.size(); i++) { double[] h = hits.get(i); sb.append(String.format("blow %d: dealt %.2f of plain %.2f = x%.3f, target moved %.2f, run %.1f; ", i + 1, h[1], h[2], h[0], h[3], h[5])); }
                pass(ok, sb + String.format("first hits %d (want 1); %s", OrderIntakeProbeFormation.firstHits() - hits0, note));
                next(b);
            }
        }
    }

    // Task 6 review I1: one knight charges an enemy knight 11 blocks ahead (stunned, bare, a deep pool of health) that has just taken a heavier
    // blow and stays in vanilla's hurt cooldown until the knight's first blow: that blow does not land (damage() answers false, no health lost;
    // a troop blow's damage type bypasses_shield, so a raised shield never stops one, and the hurt cooldown is how a troop blow fails to land)
    // and does not spend the crash; the cooldown then runs out, and his next landed blow deals chargeBonusMultiplier (1.3) and knocks the
    // target 0.5 or more; the first-hit counter moves once, on the landed blow.
    int absorbed = -1; double absorbedHits = -1;
    void chargeUnlanded(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            mine = troops(b, true, P, "knight", 1);
            foes = troops(b, false, P.add(side * 11, 0, 0), "knight", 1); OrderIntakeProbeFormation.stun(foes); OrderIntakeProbeFormation.bare(foes.get(0), 400);
            absorbed = -1; absorbedHits = -1;
        }
        var tgt = foes.isEmpty() ? null : foes.get(0);
        if (t == 60) {
            OrderIntakeProbeFormation.charge(b, squadA, "infantry", ground(P.add(side * 18, 0, 0)), new Vec3d(side, 0, 0), face());
            hits0 = OrderIntakeProbeFormation.firstHits(); issuedAt = now(b);
            var tm = OrderIntakeProbeBehaviour.mob(tgt); tm.damage(tm.getDamageSources().generic(), 60); tm.setVelocity(Vec3d.ZERO); hp = tm.getHealth(); tPos = tm.getPos(); seen = Combat.LAST.peekLast();
        }
        if (t > 60) {
            var k = mine.get(0); var tm = OrderIntakeProbeBehaviour.mob(tgt);
            if (absorbed < 0) tm.timeUntilRegen = 20; // still in the heavier blow's hurt cooldown, so a lighter blow does not land
            if (OrderIntakeProbeFormation.order(b, k).equals("charge")) run = Math.max(run, OrderIntakeProbeFormation.run(k));
            int ka = OrderIntakeProbeFormation.id(k), ta = OrderIntakeProbeFormation.id(tgt);
            List<Combat.HitLog> fresh = new ArrayList<>(); boolean past = seen == null;
            for (var h : Combat.LAST) { if (past) fresh.add(h); else if (h == seen) past = true; }
            if (!past) fresh.addAll(Combat.LAST);
            seen = Combat.LAST.peekLast();
            float now = tm.getHealth();
            for (var h : fresh) if (h.attacker() == ka && h.target() == ta) {
                if (absorbed < 0) {
                    absorbed = (int) (now(b) - issuedAt); absorbedHits = OrderIntakeProbeFormation.firstHits() - hits0;
                    note += String.format("blow 1 at +%d: lost %.2f in the hurt cooldown, first hits %d (want 0); ", absorbed, hp - now, (int) absorbedHits);
                    if (hp - now > 0.01) absorbedHits = 99; // landed: the scene's premise failed
                } else if (hits.isEmpty() && hp - now > 0.01) {
                    double expected = h.base() * h.veteran() * h.total();
                    hits.add(new double[]{(hp - now) / expected, hp - now, expected, 0, OrderIntakeProbeFormation.firstHits() - hits0, run});
                    hitFrom = tPos; watch = 12; note += "landed blow rows " + h.rows() + "; ";
                }
            }
            if (watch > 0) { watch--; double[] last = hits.get(0); last[3] = Math.max(last[3], flat(tm.getPos(), hitFrom)); }
            hp = now; tPos = tm.getPos();
            if (hits.size() == 1 && watch == 0 || t == LIMITS[scene] - 1) {
                double[] h = hits.isEmpty() ? null : hits.get(0);
                boolean ok = absorbedHits == 0 && h != null && Math.abs(h[0] - 1.3) <= 0.02 && h[3] >= 0.5 && h[4] == 1 && h[5] >= 6;
                pass(ok, (h == null ? "no landed blow; " : String.format("landed blow: dealt %.2f of plain %.2f = x%.3f (want 1.3), target moved %.2f (want 0.5 or more), first hits %d (want 1), run %.1f; ", h[1], h[2], h[0], h[3], (int) h[4], h[5])) + note);
                next(b);
            }
        }
    }

    // Task 6 review I2: a Wedge of six knights (shield troops, so on Hold they make the wall kind's wedge) and three revenants: one point and two
    // flankers, all knights, the point the only front-rank man; the point falls, and 60 ticks later there is again one point and two flankers
    // (the held places closed up), the new point the man who stepped into rank 0.
    String rolesBefore = "";
    void wedgeRoles(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            mine = new ArrayList<>(troops(b, true, P, "knight", 6)); Object s = squadA;
            var rev = OrderIntakeProbeBehaviour.spawn(b, s, "revenant", P, 3); junk.addAll(rev); mine.addAll(rev);
            OrderIntakeProbeFormation.issue(b, s, "infantry", "hold", "wedge", ground(P), face(), true);
        }
        if (t == 140) {
            String[] r = roles(b); rolesBefore = r[0];
            GravesAndGlory.Unit point = null; for (var u : mine) if (OrderIntakeProbeBehaviour.mob(u).isAlive() && OrderIntakeProbeFormation.wedgeRole(b, u).equals("point")) point = u;
            boolean ok = r[1].equals("ok");
            if (!ok || point == null) { fail("before the loss: " + r[0]); next(b); return; }
            OrderIntakeProbeFormation.kill(point); note = "point " + OrderIntakeProbeFormation.id(point) + " killed; ";
        }
        if (t == 200) {
            String[] r = roles(b);
            pass(r[1].equals("ok"), "before the loss: " + rolesBefore + "; " + note + "60 ticks on: " + r[0]);
            next(b);
        }
    }
    /** The wedge's roles now: {description, "ok" when there is one point and two flankers, all shield troops, and the point the only front-rank man}. */
    String[] roles(GravesAndGlory.Battle b) {
        int points = 0, flanks = 0, fronts = 0, noShield = 0; List<String> who = new ArrayList<>();
        for (var u : mine) {
            if (!OrderIntakeProbeBehaviour.mob(u).isAlive()) continue;
            String r = OrderIntakeProbeFormation.wedgeRole(b, u); boolean f = OrderIntakeProbeFormation.frontRank(b, u);
            if (r.equals("point")) points++; if (r.equals("flank")) flanks++; if (f) fronts++;
            if (!r.equals("none") && !OrderIntakeProbeFormation.shield(u)) noShield++;
            if (!r.equals("none") || f) who.add(OrderIntakeProbeFormation.id(u) + (OrderIntakeProbeFormation.shield(u) ? " shield " : " ") + r + (f ? " front" : ""));
        }
        boolean ok = points == 1 && flanks == 2 && fronts == 1 && noShield == 0;
        return new String[]{String.format("%d points, %d flankers, %d front-rank, %d non-shield in a role (want 1, 2, 1, 0) %s", points, flanks, fronts, noShield, who), ok ? "ok" : "no"};
    }

    // Ruling 11: Follow me while the commander waits to respawn: the troops stop at his last living spot, not at a stale point nor at the spectator.
    void followRespawning(GravesAndGlory.Battle b, long t) {
        if (t == 40) { if (!layout(b)) { next(b); return; } D1 = C.add(-side * 6, 0, zs * 8); D = C.add(-side * 6, 0, zs * 24); OrderIntakeProbeBehaviour.teleport(b, id, D1.x, D1.z); }
        if (t == 50) { OrderIntakeProbeBehaviour.order(b, id, "infantry", "follow", "block", 0, 0, face()); issuedAt = now(b); note = "stored " + snap(b, "infantry"); }
        if (t == 60) OrderIntakeProbeBehaviour.teleport(b, id, D.x, D.z);
        if (t == 150) {
            var p = b.server.getPlayerManager().getPlayer(id); D = p.getPos();
            note += "; killed by " + OrderIntakeProbeBehaviour.kill(b, id); spec = b.server.getPlayerManager().getPlayer(id).getPos();
        }
        if (t == 150 + 220) {
            Snap a = snap(b, "infantry"); Vec3d c = OrderIntakeProbeBehaviour.centre(inf(b)); Vec3d tg = new Vec3d(a.tx(), 0, a.tz());
            boolean ok = OrderIntakeProbeBehaviour.respawning(b, id) && flat(tg, D) <= 1.5 && c != null && flat(c, D) <= 6 && flat(c, spec) > 10;
            pass(ok, String.format("death spot %s, spectator %s; 220 ticks on (still respawning %s): target %s (%.1f off), centre %s (%.1f from death spot, %.1f from spectator); now %s; first spot %s; %s",
                xz(D), xz(spec), OrderIntakeProbeBehaviour.respawning(b, id), xz(tg), flat(tg, D), xz(c), c == null ? -1 : flat(c, D), c == null ? -1 : flat(c, spec), a, xz(D1), note));
            next(b);
        }
    }

    // Ruling 11: a Charge aimed off the arena's edge: the infantry (moved 14 blocks inside the edge first if it stands farther off) stops on dry
    // ground inside inArena, and its order turns to attack.
    void chargeEdge(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            double limit = OrderIntakeProbeFormation.arenaHalfX() - 10; int out = C.x >= 0 ? 1 : -1; side = out;
            double gx = out * (limit - 14), gz = C.z;
            for (int i = 0; i < 40 && OrderIntakeProbeBehaviour.wet(gx, gz); i++) gz += zs * 3;
            if (Math.abs(C.x) < limit - 30 || Math.abs(gz - C.z) > 0.1) {
                for (var u : inf(b)) { Vec3d p = OrderIntakeProbeBehaviour.mob(u).getPos(); OrderIntakeProbeFormation.place(u, gx + (p.x - C.x), gz + (p.z - C.z)); }
                OrderIntakeProbeBehaviour.teleport(b, id, gx - out * 6, gz);
                note = String.format("moved the infantry to %.1f, %.1f; ", gx, gz);
            }
            D = new Vec3d(gx, 0, gz);
        }
        if (t == 80) {
            Vec3d c = OrderIntakeProbeBehaviour.centre(inf(b));
            OrderIntakeProbeBehaviour.order(b, id, "infantry", "charge", "block", c.x, c.z, face()); issuedAt = now(b);
            Snap a = snap(b, "infantry"); T = a == null ? null : new Vec3d(a.tx(), 0, a.tz()); note += "from " + xz(c) + ", stored " + a;
            if (a == null || !a.order().equals("charge")) { fail("charge not stored: " + note); next(b); return; }
        }
        if (t > 80) {
            Snap a = snap(b, "infantry"); Vec3d c = OrderIntakeProbeBehaviour.centre(inf(b));
            if (arrivedAt < 0 && c != null && flat(c, T) <= 4) arrivedAt = now(b);
            if (switchedAt < 0 && a.order().equals("attack")) switchedAt = now(b);
            if (switchedAt >= 0 && now(b) >= switchedAt + 60 || t == LIMITS[scene] - 1) {
                double limit = OrderIntakeProbeFormation.arenaHalfX() - 10;
                boolean tDry = OrderIntakeProbeBehaviour.inArena(T.x, T.z) && !OrderIntakeProbeBehaviour.wet(T.x, T.z);
                boolean cDry = c != null && OrderIntakeProbeBehaviour.inArena(c.x, c.z) && !OrderIntakeProbeBehaviour.wet(c.x, c.z);
                boolean ok = tDry && cDry && switchedAt >= 0 && a.order().equals("attack") && c != null && flat(c, T) <= 6;
                pass(ok, String.format("end %s (edge at x %.0f, %.1f inside; dry and in the arena %s); centre %s (%.1f off the end; dry and in the arena %s); arrived +%d, attack at +%d; now %s; %s",
                    xz(T), side * limit, limit - Math.abs(T.x), tDry, xz(c), c == null ? -1 : flat(c, T), cDry, arrivedAt < 0 ? -1 : arrivedAt - issuedAt, switchedAt < 0 ? -1 : switchedAt - issuedAt, a, note));
                next(b);
            }
        }
    }
}
