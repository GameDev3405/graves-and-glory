package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGlory;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.OrderIntakeProbeBehaviour;
import gg.gravesandglory.OrderIntakeProbeBotOrders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command UI plan Task 11: a bot army (Campaign.botDecide, on its own cadence, in Battle.squads) shares the Charge cooldown and still holds its
 * objective. Scene 0 flips the enemy count every 100 ticks so the bot wants a Charge at every other decision, over 2400 ticks, and requires that
 * it never starts two Charges within the cooldown (400 ticks), never stores a hunting Attack and charges more than once (the gate is exercised, not idle).
 * Scene 1 is Task 5's leash check on a real decision: the bot walks to its chosen point with an enemy 60 blocks off the route. Never included in releases.
 */
public final class BotOrdersSuite implements CommandUiChecks.Suite {
    volatile UUID id;
    volatile boolean done;
    int scene = -1; long t0;
    final List<GravesAndGlory.Unit> junk = new ArrayList<>();
    Object bot, foeSquad; List<GravesAndGlory.Unit> botUnits = List.of(), few = List.of(), many = List.of(), decoy = List.of();
    int side, zs; Vec3d C, tgt; double startDist; boolean wasCharging, hunted, targetedDecoy; final List<Long> starts = new ArrayList<>();

    static final String[] NAMES = {"bot-charge-cooldown", "bot-objective"};
    static final int[] LIMITS = {2460, 520};
    static final int COOLDOWN = 400;

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) id = c.player.getUuid();
        if (done) { done = false; CommandUiChecks.finish(c); }
    }
    @Override public int limit() { return 4200; }
    @Override public void cleanup(MinecraftClient c) { CommandUiChecks.server(c, b -> { discard(b); OrderIntakeProbeBehaviour.restore(b); }); }

    @Override public void serverTick(GravesAndGlory.Battle b) {
        if (id == null || scene >= NAMES.length) return;
        if (scene < 0) { next(b); return; }
        if (!OrderIntakeProbeBehaviour.keep(b)) { fail("round stopped at scene tick " + (now(b) - t0)); next(b); return; }
        long t = now(b) - t0;
        try { if (t > LIMITS[scene]) { fail("timed out"); next(b); } else step(b, t); }
        catch (RuntimeException e) { fail("threw " + e); next(b); }
    }

    void next(GravesAndGlory.Battle b) {
        discard(b); scene++; wasCharging = false; hunted = false; targetedDecoy = false; starts.clear();
        if (scene >= NAMES.length) { OrderIntakeProbeBehaviour.restore(b); done = true; return; }
        OrderIntakeProbeBehaviour.emptyRound(b, id); t0 = now(b);
    }
    void discard(GravesAndGlory.Battle b) {
        for (var u : junk) OrderIntakeProbeBehaviour.mob(u).discard();
        junk.clear(); if (bot != null) OrderIntakeProbeBotOrders.drop(b, bot);
        bot = null; botUnits = few = many = decoy = List.of();
    }
    void pass(boolean ok, String detail) { CommandUiChecks.report("cu-" + NAMES[scene], ok, detail); }
    void fail(String detail) { if (scene >= 0 && scene < NAMES.length) pass(false, detail); }
    static long now(GravesAndGlory.Battle b) { return OrderIntakeProbe.time(b); }
    static double flat(Vec3d a, Vec3d b) { return Math.hypot(a.x - b.x, a.z - b.z); }
    static String xz(Vec3d v) { return v == null ? "null" : String.format("%.1f, %.1f", v.x, v.z); }

    boolean layout(GravesAndGlory.Battle b) {
        side = OrderIntakeProbeBehaviour.team(b, id) == 0 ? 1 : -1;
        C = OrderIntakeProbeBehaviour.centre(OrderIntakeProbeBehaviour.members(b, id, "infantry"));
        if (C == null) { fail("no infantry after the empty round"); return false; }
        zs = C.z > 0 ? -1 : 1; return true;
    }
    /** The bot army on the player's team, 25 blocks off the player's infantry along z. */
    void armBot(GravesAndGlory.Battle b, int n, boolean inert) {
        Vec3d at = C.add(0, 0, zs * 25);
        bot = OrderIntakeProbeBotOrders.addBot(b, OrderIntakeProbeBehaviour.team(b, id), at);
        botUnits = OrderIntakeProbeBehaviour.spawn(b, bot, "knight", at, n);
        if (inert) OrderIntakeProbeBehaviour.inert(botUnits);
        junk.addAll(botUnits);
    }
    /** Inert enemies of the other team (not a bot, not in Battle.squads). */
    List<GravesAndGlory.Unit> foes(GravesAndGlory.Battle b, Vec3d at, int n) {
        if (foeSquad == null) foeSquad = OrderIntakeProbeBehaviour.squad(1 - OrderIntakeProbeBehaviour.team(b, id), false, at, "hold");
        var us = OrderIntakeProbeBehaviour.spawn(b, foeSquad, "revenant", at, n);
        OrderIntakeProbeBehaviour.inert(us); junk.addAll(us); return us;
    }

    void step(GravesAndGlory.Battle b, long t) {
        switch (scene) {
            case 0 -> cooldown(b, t);
            case 1 -> objective(b, t);
            default -> {}
        }
    }

    // Every Charge the bot starts (the order turning to charge) is at least 400 ticks after the one before, over 2400 ticks.
    void cooldown(GravesAndGlory.Battle b, long t) {
        if (t == 20) {
            if (!layout(b)) { next(b); return; }
            armBot(b, 8, true);
            foeSquad = null; few = foes(b, C.add(side * 70, 0, 0), 3);
        }
        if (t <= 20) return;
        long abs = now(b);
        // the enemy count flips: 3 enemies (the 8 bots outnumber them 1.5 to 1: a Charge is wanted) at decisions on 200k, 15 at 200k+100 (not wanted)
        if (abs % 200 == 50 && many.isEmpty()) many = foes(b, C.add(side * 70, 0, 40 * zs), 12);
        if (abs % 200 == 150 && !many.isEmpty()) { for (var u : many) OrderIntakeProbeBehaviour.mob(u).discard(); junk.removeAll(many); many = List.of(); }
        boolean now = OrderIntakeProbeBotOrders.charging(bot);
        if (now && !wasCharging) starts.add(abs - t0);
        wasCharging = now;
        if (OrderIntakeProbeBotOrders.hunting(bot)) hunted = true;
        if (t == LIMITS[0]) {
            long gap = Long.MAX_VALUE; for (int i = 1; i < starts.size(); i++) gap = Math.min(gap, starts.get(i) - starts.get(i - 1));
            boolean ok = starts.size() >= 4 && gap >= COOLDOWN && !hunted;
            pass(ok, "charge starts at ticks " + starts + " (want at least 4, none closer than " + COOLDOWN + "; closest " + (gap == Long.MAX_VALUE ? "n/a" : gap) + "); hunting attack stored " + hunted + "; order now " + OrderIntakeProbeBotOrders.order(bot));
            next(b);
        }
    }

    // A bot army's decision sends it to its chosen point and keeps it walking there with an enemy 60 blocks off the route.
    void objective(GravesAndGlory.Battle b, long t) {
        if (t == 20) { if (!layout(b)) { next(b); return; } armBot(b, 6, false); }
        if (t == 150) {
            tgt = OrderIntakeProbeBotOrders.target(bot);
            Vec3d from = OrderIntakeProbeBehaviour.centre(botUnits);
            if (tgt == null || from == null) { fail("no target or no bot army after the first decision"); next(b); return; }
            startDist = flat(from, tgt);
            double dx = tgt.x - from.x, dz = tgt.z - from.z, len = Math.max(1, Math.hypot(dx, dz));
            Vec3d mid = new Vec3d((from.x + tgt.x) / 2, 0, (from.z + tgt.z) / 2), at = null;
            for (int sign = 1; sign >= -1 && at == null; sign -= 2) {
                Vec3d p = mid.add(-dz / len * 60 * sign, 0, dx / len * 60 * sign);
                if (OrderIntakeProbeBehaviour.inArena(p.x, p.z) && !OrderIntakeProbeBehaviour.wet(p.x, p.z)) at = p;
            }
            if (at == null) { fail("no dry arena ground 60 blocks off the route " + xz(from) + " to " + xz(tgt)); next(b); return; }
            foeSquad = null; decoy = foes(b, at, 2);
        }
        if (t > 150) {
            for (var u : botUnits) { var tg = OrderIntakeProbeBehaviour.target(u); for (var f : decoy) if (tg == OrderIntakeProbeBehaviour.mob(f)) targetedDecoy = true; }
            if (OrderIntakeProbeBotOrders.hunting(bot)) hunted = true;
        }
        if (t == 480) {
            Vec3d c = OrderIntakeProbeBehaviour.centre(botUnits); double d = c == null ? Double.MAX_VALUE : flat(c, tgt);
            pass((startDist - d >= 15 || d <= 8) && !targetedDecoy && !hunted, String.format("bot centre to its point %.1f -> %.1f (point %s); targeted the off-route enemy %s; hunting attack stored %s; order %s", startDist, d, xz(tgt), targetedDecoy, hunted, OrderIntakeProbeBotOrders.order(bot)));
            next(b);
        }
    }
}
