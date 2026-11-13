package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGlory;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderIntakeProbe.Snap;
import gg.gravesandglory.OrderIntakeProbeBehaviour;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Command UI plan Task 5: what troops do with the orders the intake stores (Tactics). Each scene starts an empty round (only this player's army),
 * places synthetic enemies (CommandPlaytest's spawnVisualSquad pattern: squads outside Battle.squads), gives a real player order through
 * OrderRules.playerOrder and watches the troops on the server thread every tick. Never included in releases.
 */
public final class OrderBehaviourSuite implements CommandUiChecks.Suite {
    volatile UUID id;
    volatile boolean done;
    int scene = -1; long t0;
    final List<GravesAndGlory.Unit> junk = new ArrayList<>();
    // scene state
    int side, zs; Vec3d C, R, E, T, O, D, D1, spec; double startDist, best, bestShare; long arrivedAt, switchedAt, issuedAt; int swings, shots, strikes0;
    boolean flag, shield; String note = ""; Object squad; List<GravesAndGlory.Unit> foes = List.of(), bots = List.of();
    final Map<GravesAndGlory.Unit, Long> attackMarks = new HashMap<>();

    static final String[] NAMES = {"attack-hunts-far", "bot-attack-leashed", "charge-run", "charge-impact", "charge-archers", "retreat", "retreat-respawning", "formation-hold", "legacy-charge"};
    long chargedAt0; boolean crawled; double shieldPace, offMelee = Double.MAX_VALUE, reachGap;
    static final int[] LIMITS = {520, 420, 760, 560, 560, 760, 480, 420, 900};

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
        discard(); scene++; note = ""; flag = false; shield = false; swings = 0; shots = 0; best = Double.MAX_VALUE; bestShare = 0; arrivedAt = -1; switchedAt = -1; attackMarks.clear();
        if (scene >= NAMES.length) { OrderIntakeProbeBehaviour.restore(b); done = true; return; }
        OrderIntakeProbeBehaviour.emptyRound(b, id); t0 = now(b);
    }
    void discard() { for (var u : junk) OrderIntakeProbeBehaviour.mob(u).discard(); junk.clear(); }
    void pass(boolean ok, String detail) { CommandUiChecks.report("cu-beh-" + NAMES[scene], ok, detail); }
    void fail(String detail) { pass(false, detail); }

    List<GravesAndGlory.Unit> inf(GravesAndGlory.Battle b) { return OrderIntakeProbeBehaviour.members(b, id, "infantry"); }
    List<GravesAndGlory.Unit> rng(GravesAndGlory.Battle b) { return OrderIntakeProbeBehaviour.members(b, id, "ranged"); }
    Snap snap(GravesAndGlory.Battle b, String g) { return OrderIntakeProbe.snap(b, id, g); }
    static long now(GravesAndGlory.Battle b) { return OrderIntakeProbe.time(b); }
    static double flat(Vec3d a, Vec3d b) { return Math.hypot(a.x - b.x, a.z - b.z); }
    static String xz(Vec3d v) { return v == null ? "null" : String.format("%.1f, %.1f", v.x, v.z); }
    float face() { return side > 0 ? -90 : 90; }
    List<GravesAndGlory.Unit> enemies(GravesAndGlory.Battle b, boolean bot, Vec3d at, String order, String troop, int n, boolean inert) {
        squad = OrderIntakeProbeBehaviour.squad(1 - OrderIntakeProbeBehaviour.team(b, id), bot, at, order);
        var us = OrderIntakeProbeBehaviour.spawn(b, squad, troop, at, n);
        if (inert) OrderIntakeProbeBehaviour.inert(us);
        junk.addAll(us); return us;
    }
    /** Shots: a member whose next attack moved on since the last tick has swung or loosed. */
    int countAttacks(List<GravesAndGlory.Unit> us) {
        int n = 0;
        for (var u : us) { long na = OrderIntakeProbeBehaviour.nextAttack(u); Long was = attackMarks.put(u, na); if (was != null && na != was) n++; }
        return n;
    }
    /** The base layout every scene reads at its tick 40: own team's facing, a z sign that keeps offsets inside the arena, the group centres. */
    boolean layout(GravesAndGlory.Battle b) {
        side = OrderIntakeProbeBehaviour.team(b, id) == 0 ? 1 : -1;
        C = OrderIntakeProbeBehaviour.centre(inf(b)); R = OrderIntakeProbeBehaviour.centre(rng(b));
        if (C == null || R == null) { fail("no infantry or ranged after the empty round (infantry " + inf(b).size() + ", ranged " + rng(b).size() + ")"); return false; }
        zs = C.z > 0 ? -1 : 1; return true;
    }

    void step(GravesAndGlory.Battle b, long t) {
        switch (scene) {
            case 0 -> attackHuntsFar(b, t);
            case 1 -> botLeashed(b, t);
            case 2 -> chargeRun(b, t);
            case 3 -> chargeImpact(b, t);
            case 4 -> chargeArchers(b, t);
            case 5 -> retreat(b, t);
            case 6 -> retreatRespawning(b, t);
            case 7 -> formationHold(b, t);
            case 8 -> legacyCharge(b, t);
            default -> {}
        }
    }

    // An enemy 80 blocks off (beyond scouting range): a player's Attack walks the infantry 20+ blocks toward it within 400 ticks.
    void attackHuntsFar(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            E = C.add(side * 80, 0, 0);
            foes = enemies(b, false, E, "hold", "revenant", 3, true);
            startDist = flat(C, E); note = "enemy at " + xz(E) + " wet " + OrderIntakeProbeBehaviour.wet(E.x, E.z);
        }
        if (t == 45) { OrderIntakeProbeBehaviour.order(b, id, "infantry", "attack", "block", 0, 0, face()); issuedAt = now(b); }
        if (t > 45) {
            Vec3d c = OrderIntakeProbeBehaviour.centre(inf(b)); if (c != null) best = Math.min(best, flat(c, E));
            if (t == 445) { Snap a = snap(b, "infantry"); pass(startDist - best >= 20, String.format("start %.1f, closest %.1f within 400 ticks (%.1f closer); stored %s; %s", startDist, best, startDist - best, a, note)); next(b); }
        }
    }

    // A bot squad's attack (Battle.issue, no hunt) toward its objective keeps walking there with an enemy 60 blocks off its route.
    void botLeashed(GravesAndGlory.Battle b, long t) {
        if (t == 40) {
            if (!layout(b)) { next(b); return; }
            Vec3d q = C.add(side * 40, 0, zs * 60); O = q.add(side * 50, 0, 0);
            bots = enemies(b, true, q, "attack", "revenant", 3, false);
            OrderIntakeProbeBehaviour.issue(b, squad, "infantry", "attack", new Vec3d(O.x, OrderIntakeProbeBehaviour.ground(O.x, O.z), O.z), -face());
            // the off-route enemy: two of this player's team, still, 60 blocks across the route from the bots' start toward the objective
            var mine = OrderIntakeProbeBehaviour.squad(OrderIntakeProbeBehaviour.team(b, id), false, q.add(side * 25, 0, -zs * 60), "hold");
            var off = OrderIntakeProbeBehaviour.spawn(b, mine, "knight", q.add(side * 25, 0, -zs * 60), 2); OrderIntakeProbeBehaviour.inert(off); junk.addAll(off); foes = off;
            startDist = flat(OrderIntakeProbeBehaviour.centre(bots), O);
        }
        if (t > 40) {
            for (var u : bots) { var tg = OrderIntakeProbeBehaviour.target(u); for (var f : foes) if (tg == OrderIntakeProbeBehaviour.mob(f)) flag = true; }
            if (t == 360) {
                Vec3d c = OrderIntakeProbeBehaviour.centre(bots); double d = c == null ? Double.MAX_VALUE : flat(c, O);
                pass(startDist - d >= 15 && !flag, String.format("bot centre to objective %.1f -> %.1f; targeted the off-route enemy %s", startDist, d, flag)); next(b);
            }
        }
    }

    // Charge with no enemy: the run at the 1.25 share, the centre within 4 of the end point, then attack (hunt) within 40 ticks of arrival.
    void chargeRun(GravesAndGlory.Battle b, long t) {
        if (t == 40) { if (!layout(b)) { next(b); return; } OrderIntakeProbeBehaviour.order(b, id, "infantry", "charge", "block", C.x, C.z, face()); issuedAt = now(b); Snap a = snap(b, "infantry"); T = a == null ? null : new Vec3d(a.tx(), 0, a.tz()); note = "stored " + a; if (a == null || !a.order().equals("charge")) { fail("charge not stored: " + a); next(b); return; } }
        if (t > 40) {
            Snap a = snap(b, "infantry"); var us = inf(b); Vec3d c = OrderIntakeProbeBehaviour.centre(us);
            if (a.order().equals("charge")) for (var u : us) if (OrderIntakeProbeBehaviour.walking(u)) bestShare = Math.max(bestShare, OrderIntakeProbeBehaviour.speedShare(u));
            if (arrivedAt < 0 && c != null && flat(c, T) <= 4) arrivedAt = now(b);
            if (switchedAt < 0 && a.order().equals("attack") && a.hunt()) switchedAt = now(b);
            if ((arrivedAt >= 0 && switchedAt >= 0 && now(b) >= Math.max(arrivedAt, switchedAt) + 60) || t == LIMITS[scene] - 1) {
                boolean ok = arrivedAt >= 0 && switchedAt >= 0 && Math.abs(switchedAt - arrivedAt) <= 40 && c != null && flat(c, T) <= 4 && bestShare >= 1.2 && bestShare <= 1.3;
                pass(ok, String.format("end %s; arrived at +%d, attack hunt at +%d; final centre %s (%.1f off); max speed share while running %.3f; now %s", xz(T), arrivedAt < 0 ? -1 : arrivedAt - issuedAt, switchedAt < 0 ? -1 : switchedAt - issuedAt, xz(c), c == null ? -1 : flat(c, T), bestShare, a));
                next(b);
            }
        }
    }

    // An enemy 15 blocks ahead: on the first landed blow the infantry's Charge has turned into attack (hunt); chargedAt kept.
    void chargeImpact(GravesAndGlory.Battle b, long t) {
        if (t == 40) { if (!layout(b)) { next(b); return; } E = C.add(side * 15, 0, 0); foes = enemies(b, false, E, "hold", "revenant", 3, true); }
        if (t == 45) { OrderIntakeProbeBehaviour.order(b, id, "infantry", "charge", "block", C.x, C.z, face()); issuedAt = now(b); strikes0 = OrderIntakeProbeBehaviour.strikesLanded(); chargedAt0 = OrderIntakeProbeBehaviour.chargedAt(b, id, "infantry"); note = "charge stored " + snap(b, "infantry") + ", chargedAt " + OrderIntakeProbeBehaviour.chargedAt(b, id, "infantry"); }
        if (t > 45 && OrderIntakeProbeBehaviour.strikesLanded() > strikes0) {
            Snap a = snap(b, "infantry");
            long kept = OrderIntakeProbeBehaviour.chargedAt(b, id, "infantry");
            pass(a != null && a.order().equals("attack") && a.hunt() && kept == chargedAt0 && kept >= 0, String.format("first landed blow at +%d: stored %s; chargedAt %d after the switch (want %d, as stamped by the Charge; issued %d); %s", now(b) - issuedAt, a, kept, chargedAt0, issuedAt, note));
            next(b);
        }
    }

    // A ranged group under Charge with enemies in its path never comes within 3 blocks of one and shoots at least once.
    void chargeArchers(GravesAndGlory.Battle b, long t) {
        if (t == 40) { if (!layout(b)) { next(b); return; } E = R.add(side * 26, 0, 0); foes = enemies(b, false, E, "hold", "revenant", 3, true); countAttacks(rng(b)); }
        if (t == 45) { OrderIntakeProbeBehaviour.order(b, id, "ranged", "charge", "block", R.x, R.z, face()); issuedAt = now(b); note = "stored " + snap(b, "ranged"); }
        if (t > 45) {
            var us = rng(b); shots += countAttacks(us);
            for (var u : us) for (var f : foes) if (OrderIntakeProbeBehaviour.mob(f).isAlive()) best = Math.min(best, OrderIntakeProbeBehaviour.mob(u).distanceTo(OrderIntakeProbeBehaviour.mob(f)));
            if (t == 445) { pass(best >= 3 && shots >= 1, String.format("closest archer to an enemy %.1f; shots %d; now %s; %s", best, shots, snap(b, "ranged"), note)); next(b); }
        }
    }

    // Under attack, Retreat: no infantry member swings until the group reaches the commander and turns to follow. Shield troops under arrow fire
    // keep their shields up on the way (Ruling 2): cu-beh-retreat-shield.
    void retreat(GravesAndGlory.Battle b, long t) {
        if (t == 40) { if (!layout(b)) { next(b); return; } E = C.add(side * 5, 0, 0); foes = enemies(b, false, E, "hold", "revenant", 4, false); }
        if (t == 100) {
            Vec3d back = C.add(-side * 3, 0, zs * 22); OrderIntakeProbeBehaviour.teleport(b, id, back.x, back.z);
            OrderIntakeProbeBehaviour.order(b, id, "infantry", "retreat", "block", 0, 0, face()); issuedAt = now(b); note = "stored " + snap(b, "infantry");
        }
        if (t > 100) {
            Snap a = snap(b, "infantry"); var us = inf(b);
            if (now(b) - issuedAt <= 40) OrderIntakeProbeBehaviour.fire(b, id, "infantry");
            if (!a.order().equals("follow")) {
                for (var u : us) if (OrderIntakeProbeBehaviour.swungAt(u) > issuedAt) { swings++; attackMarks.put(u, OrderIntakeProbeBehaviour.swungAt(u)); }
                if (now(b) - issuedAt > 2 && now(b) - issuedAt <= 40) for (var u : us) if (OrderIntakeProbeBehaviour.shieldUp(u)) {
                    shield = true; if (OrderIntakeProbeBehaviour.crawling(u)) crawled = true;
                    if (OrderIntakeProbeBehaviour.walking(u)) shieldPace = Math.max(shieldPace, OrderIntakeProbeBehaviour.baseShare(u));
                }
            } else if (switchedAt < 0) switchedAt = now(b);
            if (switchedAt >= 0 || t == LIMITS[scene] - 1) {
                int swingers = attackMarks.size();
                pass(switchedAt >= 0 && swingers == 0, String.format("follow at +%d; members that swung on the way %d; now %s; %s", switchedAt < 0 ? -1 : switchedAt - issuedAt, swingers, a, note));
                boolean pace = shieldPace >= 1.10 && !crawled; // Retreat's moveMultiplier 1.15, no shield crawl (fix round 1 ruling)
                CommandUiChecks.report("cu-beh-retreat-shield", shield && pace, String.format("a knight's shield up under arrow fire during the first 40 ticks of the Retreat: %s; shielded knights' pace %.3f of their base speed (want 1.10 or more, Retreat's 1.15), the shield crawl on one of them %s (want false)", shield, shieldPace, crawled));
                next(b);
            }
        }
    }

    // The commander moves during the Retreat (it follows him), then is struck down: the troops stop at his death spot, not at the spectator's.
    void retreatRespawning(GravesAndGlory.Battle b, long t) {
        if (t == 40) { if (!layout(b)) { next(b); return; } D1 = C.add(-side * 6, 0, zs * 8); D = C.add(-side * 6, 0, zs * 24); OrderIntakeProbeBehaviour.teleport(b, id, D1.x, D1.z); }
        if (t == 50) { OrderIntakeProbeBehaviour.order(b, id, "infantry", "retreat", "block", 0, 0, face()); issuedAt = now(b); note = "stored " + snap(b, "infantry"); }
        if (t == 60) OrderIntakeProbeBehaviour.teleport(b, id, D.x, D.z);
        if (t == 110) {
            var p = b.server.getPlayerManager().getPlayer(id); D = p.getPos();
            note += "; killed by " + OrderIntakeProbeBehaviour.kill(b, id); spec = b.server.getPlayerManager().getPlayer(id).getPos(); issuedAt = now(b);
        }
        if (t == 110 + 220) {
            Snap a = snap(b, "infantry"); Vec3d c = OrderIntakeProbeBehaviour.centre(inf(b)); Vec3d tg = new Vec3d(a.tx(), 0, a.tz());
            boolean ok = OrderIntakeProbeBehaviour.respawning(b, id) && flat(tg, D) <= 1.5 && c != null && flat(c, D) <= 6 && flat(c, spec) > 10;
            pass(ok, String.format("death spot %s, spectator %s; 220 ticks on (still respawning %s): target %s (%.1f off), centre %s (%.1f from death spot, %.1f from spectator); now %s; first spot %s; %s",
                xz(D), xz(spec), OrderIntakeProbeBehaviour.respawning(b, id), xz(tg), flat(tg, D), xz(c), c == null ? -1 : flat(c, D), c == null ? -1 : flat(c, spec), a, xz(D1), note));
            next(b);
        }
    }

    // Formation (hold + line): ranged shoot an enemy in reach, melee stay within arriveRadius (4) of their slots while an enemy stands 8 blocks off.
    void formationHold(GravesAndGlory.Battle b, long t) {
        if (t == 40) { if (!layout(b)) { next(b); return; } OrderIntakeProbeBehaviour.order(b, id, "all", "hold", "block", C.x, C.z, face()); note = "stored " + snap(b, "infantry"); }
        if (t == 180) {
            double front = -Double.MAX_VALUE; for (var u : inf(b)) front = Math.max(front, side * OrderIntakeProbeBehaviour.slot(b, u).x);
            E = new Vec3d(side * front + side * 8, C.y, C.z); // 8 blocks ahead of the front melee slot
            for (var u : inf(b)) offMelee = Math.min(offMelee, flat(E, OrderIntakeProbeBehaviour.slot(b, u)));
            reachGap = Double.MAX_VALUE; for (var u : rng(b)) reachGap = Math.min(reachGap, flat(E, OrderIntakeProbeBehaviour.slot(b, u)) - OrderIntakeProbeBehaviour.reach(b, u));
            foes = enemies(b, false, E, "hold", "revenant", 1, true); countAttacks(rng(b)); best = 0; issuedAt = now(b);
        }
        if (t > 180) {
            shots += countAttacks(rng(b));
            for (var u : inf(b)) best = Math.max(best, flat(OrderIntakeProbeBehaviour.mob(u).getPos(), OrderIntakeProbeBehaviour.slot(b, u)));
            if (t == 400) {
                double ranged = Double.MAX_VALUE; for (var u : rng(b)) ranged = Math.min(ranged, OrderIntakeProbeBehaviour.mob(u).distanceTo(OrderIntakeProbeBehaviour.mob(foes.get(0))));
                boolean inReach = reachGap <= -1; // the enemy stands at least a block inside the nearest archer's weapon reach from its slot
                pass(inReach && offMelee >= 7.5 && best <= 4 && shots >= 1, String.format("enemy at %s, %.1f off the nearest melee slot; %.1f inside the nearest archer's reach from its slot (want 1 or more); infantry max off slot %.1f (want arriveRadius 4 at most); ranged shots %d (nearest archer %.1f from it); %s", xz(E), offMelee, -reachGap, best, shots, ranged, note));
                next(b);
            }
        }
    }

    // Final fix wave, review I1 and M4: the legacy bare-order path (Battle.command "charge", what an old or modified client's ORDER packet reaches) holds the
    // per-commander Charge cooldown. A second charge inside it changes nothing: no new stamp, no new cooldown, no shout, and it does not use up lastOrder.
    // After the real cooldown a charge works again. The legacy charge and retreat also set chargeDir and thenFollow as a player's tactical order does.
    long ready1, stamp1; int heard1, cd0, cd1; Snap snap1; boolean refusedSame; String refusedNote = "";
    void legacyCharge(GravesAndGlory.Battle b, long t) {
        int cool = OrderIntakeProbeBehaviour.chargeCooldownTicks();
        if (t == 40) {
            OrderIntakeProbe.resetCharge(b, id); OrderIntakeProbeBehaviour.clearThrottle();
            cd0 = CommandNet.sentTo(id, CommandNet.COOLDOWN);
            OrderIntakeProbeBehaviour.legacy(b, id, "charge");
            snap1 = snap(b, "infantry"); ready1 = OrderIntakeProbe.chargeReadyAt(b, id); stamp1 = OrderIntakeProbeBehaviour.chargedAt(b, id, "infantry");
            heard1 = OrderIntakeProbeBehaviour.chargeHeard(id); cd1 = CommandNet.sentTo(id, CommandNet.COOLDOWN);
            double r = Math.toRadians(OrderIntakeProbeBehaviour.yaw(b, id)), dx = Math.sin(-r), dz = Math.cos(r);
            boolean dir = snap1 != null && snap1.chargeDir() != null && Math.abs(snap1.chargeDir().x - dx) < 0.01 && Math.abs(snap1.chargeDir().z - dz) < 0.01
                && OrderIntakeProbeBehaviour.allAssigned(b, id, "charge", true, false);
            CommandUiChecks.report("cu-legacy-charge-flags-charge", dir && ready1 > now(b) && stamp1 >= 0 && heard1 == 1 && cd1 == cd0 + 1,
                "legacy charge: stored " + OrderIntakeProbeBehaviour.flags(b, id) + "infantry " + snap1 + " vs yaw dir " + String.format("%.2f, %.2f", dx, dz) + "; chargeReadyAt " + ready1 + " at " + now(b) + "; stamp " + stamp1 + "; shouts " + heard1 + "; COOLDOWN sends " + (cd1 - cd0));
        }
        if (t == 55) { // 15 ticks later: past the 4-tick command cooldown, far inside the Charge cooldown
            OrderIntakeProbeBehaviour.clearThrottle();
            long cleared = OrderIntakeProbeBehaviour.legacy(b, id, "charge");
            Snap again = snap(b, "infantry"); int cd2 = CommandNet.sentTo(id, CommandNet.COOLDOWN);
            refusedSame = again != null && again.equals(snap1) && OrderIntakeProbe.chargeReadyAt(b, id) == ready1 && OrderIntakeProbeBehaviour.chargedAt(b, id, "infantry") == stamp1
                && OrderIntakeProbeBehaviour.chargeHeard(id) == heard1 && OrderIntakeProbeBehaviour.lastOrder(b, id) == cleared && cd2 == cd1 + 1;
            refusedNote = "second charge at +" + (now(b) - t0) + ": stored " + (again != null && again.equals(snap1) ? "unchanged" : "now " + again) + ", chargeReadyAt " + OrderIntakeProbe.chargeReadyAt(b, id) + " (was " + ready1
                + "), stamp " + OrderIntakeProbeBehaviour.chargedAt(b, id, "infantry") + " (was " + stamp1 + "), shouts " + OrderIntakeProbeBehaviour.chargeHeard(id) + " (was " + heard1 + "), lastOrder "
                + (OrderIntakeProbeBehaviour.lastOrder(b, id) == cleared ? "kept (not stamped)" : "consumed") + ", COOLDOWN sends " + (cd2 - cd1) + " (the remaining time, as a refused tactical charge)";
        }
        if (t == 60) OrderIntakeProbeBehaviour.legacy(b, id, "follow"); // the charge ends as it would at its end point, so the next one starts a new charge
        if (t == 40 + cool + 5) { // the real cooldown has run: nothing was reset by hand since the first charge
            OrderIntakeProbeBehaviour.clearThrottle();
            OrderIntakeProbeBehaviour.legacy(b, id, "charge");
            long ready3 = OrderIntakeProbe.chargeReadyAt(b, id), stamp3 = OrderIntakeProbeBehaviour.chargedAt(b, id, "infantry"); int heard3 = OrderIntakeProbeBehaviour.chargeHeard(id);
            boolean ok = refusedSame && ready3 > ready1 && ready3 > now(b) && stamp3 > stamp1 && heard3 == heard1 + 1 && OrderIntakeProbeBehaviour.allAssigned(b, id, "charge", true, false);
            CommandUiChecks.report("cu-legacy-charge-cooldown", ok, refusedNote + "; after the cooldown (" + cool + " ticks, +" + (t - 40) + ") a third charge: chargeReadyAt " + ready3 + " (first " + ready1 + "), stamp " + stamp3 + " (first " + stamp1
                + "), shouts " + heard3 + " (want " + (heard1 + 1) + ")");
        }
        if (t == 40 + cool + 15) {
            OrderIntakeProbeBehaviour.legacy(b, id, "retreat");
            CommandUiChecks.report("cu-legacy-charge-flags-retreat", OrderIntakeProbeBehaviour.allAssigned(b, id, "retreat", false, true), "legacy retreat: stored " + OrderIntakeProbeBehaviour.flags(b, id));
            next(b);
        }
    }
}
