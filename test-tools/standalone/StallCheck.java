package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Stall (engine-map pathing plan, step 2): the ladder's clock and rungs, driven by fake walkers that stand still, jitter, queue behind a mover, hold, arrive and get a new order; the watched/unwatched gate of the jump;
 *  a wedged troop on a NavField grid that escalates, jumps to a floor with a way on and then closes on its goal; and a source check that the ladder sits in Tactics.walk (every squad's walking) with no squad test. */
public final class StallCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }

    /** A fake walker: the walk-side of Tactics (step, then JUMP asks whether anyone watches). */
    static final class Walker {
        final Stall.State s = new Stall.State();
        final List<String> log = new ArrayList<>();
        int jumps, refused;
        double x, z;
        /** One beat: place (x, z), goal (gx, gz), route distance d (NaN: none), watched or not. Returns the rung done. */
        int beat(long t, double gx, double gz, double d, boolean watched) {
            int rung = Stall.step(s, t, x, z, gx, gz, d);
            if (rung == Stall.JUMP) {
                if (!watched) { jumps++; s.live = false; log.add(t + ":jump"); return rung; }
                refused++;
                rung = Stall.refused(s);
            }
            if (rung != Stall.NONE) log.add(t + ":" + Stall.NAMES[rung]);
            return rung;
        }
    }

    public static void main(String[] args) throws Exception {
        // 1. a troop that stands still against a wall: every rung once, in order, at its time; then the jump
        var w = new Walker();
        w.x = 100; w.z = 0;
        for (long t = 0; t <= 650; t += 10) w.beat(t, 160, 0, 60, false);
        check(w.log.equals(List.of("60:repath", "120:sidestep", "200:detour", "300:regroup", "600:jump")), "the ladder's rungs at 3, 6, 10, 15 and 30 s: " + w.log);
        check(w.jumps == 1, "an unwatched stalled troop jumps once");

        // 2. a troop that jitters in place (a sidestep every beat, half a block either way, its route distance flickering) is still stalled
        var j = new Walker();
        j.x = 100; j.z = 0;
        for (long t = 0; t <= 650; t += 10) { j.x = 100 + (t / 10 % 2 == 0 ? .5 : -.5); j.z = (t / 10 % 3) * .3; j.beat(t, 160, 0, 60 + (t / 10 % 2 == 0 ? .4 : -.4), false); }
        check(j.jumps == 1 && j.log.size() == 5, "jitter does not hide a stall: " + j.log);

        // 3. a troop queueing behind a mover, a third of a block of route distance a beat, is progressing: no rung in two minutes
        var q = new Walker();
        q.x = 100;
        for (long t = 0; t <= 2400; t += 10) { q.x = 100 + t / 30.0; q.beat(t, 400, 0, 300 - t / 30.0, false); }
        check(q.log.isEmpty(), "a crawl in a file is not a stall: " + q.log);
        // a troop that takes a long way round (its route distance falls, its straight distance rises) is not stalled
        var round = new Walker();
        for (long t = 0; t <= 1200; t += 10) { round.x = 100 + 40 * Math.sin(t / 200.0); round.z = t / 20.0; round.beat(t, 160, 0, 120 - t / 20.0, false); }
        check(round.log.isEmpty(), "a way round a wall is not a stall: " + round.log);

        // 4. a holding troop is not flagged: Tactics.walk is not called for it (it stands on its slot, or fights); the clock starts over when it walks on
        var h = new Walker();
        h.x = 100;
        for (long t = 0; t <= 100; t += 10) h.beat(t, 160, 0, 60, false); // walks, stuck for 10 s of the 30...
        h.log.clear();
        for (long t = 100; t < 5000; t += 10) { /* holds: no calls */ }
        h.beat(5000, 160, 0, 60, false);
        check(h.log.isEmpty() && h.s.age(5000) == 0, "after a hold the clock starts over: " + h.log + " age " + h.s.age(5000));
        for (long t = 5010; t <= 5500; t += 10) h.beat(t, 160, 0, 60, false);
        check(h.log.equals(List.of("5060:repath", "5120:sidestep", "5200:detour", "5300:regroup")), "and runs from the new start: " + h.log);
        // arrived: within 2.5 blocks of its goal it is never flagged, however long it stands there (a troop pressed against its slot)
        var a = new Walker();
        a.x = 100;
        for (long t = 0; t <= 6000; t += 10) a.beat(t, 101.5, 1, 1.8, false);
        check(a.log.isEmpty() && a.jumps == 0, "a troop at its goal is not flagged: " + a.log);
        // a new order (the goal moves 10 blocks) restarts the clock; a chase (the goal creeping a block a beat) does too, once it has moved four
        var o = new Walker();
        o.x = 100;
        for (long t = 0; t <= 100; t += 10) o.beat(t, 160, 0, 60, false);
        o.log.clear();
        for (long t = 110; t <= 150; t += 10) o.beat(t, 170, 0, 70, false);
        check(o.log.isEmpty() && o.s.age(150) == 40, "a new order starts the clock: age " + o.s.age(150));
        var chase = new Walker();
        chase.x = 100;
        for (long t = 0; t <= 1000; t += 10) chase.beat(t, 130 + t / 10 * 1.0, 0, 30 + t / 10 * 1.0, false);
        check(chase.log.isEmpty(), "a chase after a mover is not a stall: " + chase.log);

        // 5. no route field (an old map, a goal the field does not know): the test is the place; leaving a 5-block patch restarts the clock
        var p = new Walker();
        p.x = 100;
        for (long t = 0; t <= 650; t += 10) p.beat(t, 160, 0, Double.NaN, false);
        check(p.log.equals(List.of("60:repath", "120:sidestep", "200:detour", "300:regroup", "600:jump")), "without a field the place decides: " + p.log);
        var pm = new Walker();
        for (long t = 0; t <= 2000; t += 10) { pm.x = 100 + t / 10 * 2; pm.beat(t, 1000, 0, Double.NaN, false); }
        check(pm.log.isEmpty(), "walking on without a field is not a stall: " + pm.log);

        // 6. a watched troop never jumps: the jump is asked every 40 ticks, each refusal sets it on another rung of the cycle, and it jumps the beat nobody watches
        var wt = new Walker();
        wt.x = 100;
        for (long t = 0; t <= 3000; t += 10) wt.beat(t, 160, 0, 60, true);
        check(wt.jumps == 0 && wt.refused >= 50, "a watched troop does not jump (refused " + wt.refused + " times)");
        long cycled = wt.log.stream().filter(e -> e.endsWith("regroup") || e.endsWith("sidestep") || e.endsWith("detour")).count();
        check(cycled > 50, "and keeps walking the ladder meanwhile: " + cycled + " rungs");
        check(wt.log.stream().noneMatch(e -> e.endsWith(":jump")), "no jump in the log of a watched troop");
        for (long t2 = 3010; t2 <= 3040; t2 += 10) wt.beat(t2, 160, 0, 60, false);
        check(wt.jumps == 1, "it jumps when the viewers look away: " + wt.jumps + " " + wt.s.nextJump + " " + wt.s.live + " " + wt.s.rung);
        check(Stall.watched(2, i -> false, i -> i == 1) && Stall.watched(2, i -> i == 0, i -> false) && !Stall.watched(2, i -> false, i -> false) && !Stall.watched(0, i -> true, i -> true),
            "watched: any viewer that sees the troop or the spot; nobody online: not watched");

        // 7. a wedged troop on a floor grid (NavField): the ladder's clock reads the field, the jump is the field's rescue, and the troop then closes on its goal
        int w0 = 60, d0 = 40;
        int[][] cols = new int[w0 * d0][];
        for (int z = 0; z < d0; z++) for (int x = 0; x < w0; x++) {
            boolean wall = x >= 30 && x <= 32 && !(z >= 5 && z <= 7);
            if (!wall) cols[z * w0 + x] = new int[]{64};
        }
        NavField yard = NavField.fromColumns(0, 0, w0, d0, cols, new int[][]{{10, 20, 64}});
        double gx = 50.5, gz = 20.5;
        yard.route(10.5, 65, 20.5, gx, 65, gz, 0); // a troop's first walk builds the goal's field
        var f = new Walker();
        f.x = 29.5; f.z = 20.5;
        double d = yard.cachedDistance(f.x, 65, f.z, gx, 65, gz);
        check(d > 40, "the field gives the wedged troop's distance round the wall: " + d);
        long jumpedAt = -1;
        for (long t = 0; t <= 700 && jumpedAt < 0; t += 10) {
            int rung = f.beat(t, gx, gz, yard.cachedDistance(f.x, 65, f.z, gx, 65, gz), false);
            if (rung == Stall.JUMP) {
                var wp = yard.rescue(f.x, 65, f.z, gx, 65, gz, t);
                check(wp != null, "the field has a rescue spot");
                check(Math.hypot(wp.x() - f.x, wp.z() - f.z) >= NavField.RESCUE_MIN, "the jump is a real move, not a step in the wedge: " + Math.hypot(wp.x() - f.x, wp.z() - f.z));
                double before = yard.cachedDistance(f.x, 65, f.z, gx, 65, gz);
                f.x = wp.x(); f.z = wp.z();
                double after = yard.cachedDistance(f.x, wp.y(), f.z, gx, 65, gz);
                check(yard.onField(wp.x(), wp.y(), wp.z()) && after >= 0 && after < before - 2, "the jump puts the troop on the field, nearer its goal by the route: " + before + " -> " + after);
                jumpedAt = t;
            }
        }
        check(jumpedAt == 600, "it jumps at 30 s: " + jumpedAt);
        // the same wedged troop with a player watching does not jump; a troop off the field (water, a wall top) gets a spot on a floor
        var offField = new Walker();
        offField.x = 31.5; offField.z = 10.5;
        check(!yard.onField(31.5, 65, 10.5) && yard.rescue(31.5, 65, 10.5, gx, 65, gz, 0) != null, "a troop in the wall is off the field and has a rescue spot");

        // 8. every squad: the ladder is one call in Tactics.walk (toSlot, the enemy chase and backOff all end there, a player's and a bot's alike) and the block has no squad test
        Path src = Path.of("src/main/java/gg/gravesandglory/Tactics.java");
        if (!Files.exists(src)) src = Path.of(System.getProperty("user.dir")).resolve(src);
        String t = Files.readString(src);
        int walk = t.indexOf("static void walk(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d destination,long time,double speed,boolean climb)");
        int at = t.indexOf("int rung=stallRung(u,destination,time);", walk);
        int end = t.indexOf("boolean pinned=pinned(u,time);", at);
        check(walk > 0 && at > walk && end > at, "the ladder is inside Tactics.walk");
        String block = t.substring(at, end);
        check(!block.contains(".bot") && !block.contains("owner") && !block.contains("PlayerEntity"), "the ladder block makes no bot or player test: it covers every squad");
        check(t.indexOf("int rung=stallRung(") == t.lastIndexOf("int rung=stallRung("), "walk is the only place the ladder runs from");
        System.out.println("StallCheck: " + count + " checks passed");
    }
}
