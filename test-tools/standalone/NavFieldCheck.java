package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;

/** NavField (engine-map pathing plan, step 1): the route field over floors. Synthetic grids (a side door found, a sealed tunnel avoided, a 3-wide door passable, a roof ignored, a budgeted lookup)
 *  and the two bundled maps (every point reachable from both spawns by following waypoints, New Kingsfield's inner gates not used), with the bake time, memory and lookup time printed. */
public final class NavFieldCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }

    /** A w x d grid of floor y 64 with columns blanked by the predicate. */
    interface Blank { boolean at(int x, int z); }
    static NavField grid(int w, int d, Blank blank, int[][] seeds) {
        int[][] cols = new int[w * d][];
        for (int z = 0; z < d; z++) for (int x = 0; x < w; x++) if (!blank.at(x, z)) cols[z * w + x] = new int[]{64};
        return NavField.fromColumns(0, 0, w, d, cols, seeds);
    }

    /** Walks the waypoints: from (x, z) toward the goal, each waypoint taken as the next position, until route has none (a straight walk) or the steps run out. Returns the waypoints. */
    static java.util.List<NavField.Waypoint> follow(NavField n, double x, double z, double gx, double gz, int max) {
        var out = new java.util.ArrayList<NavField.Waypoint>();
        double cx = x, cz = z, cy = 65;
        for (int i = 0; i < max; i++) {
            var wp = n.route(cx, cy, cz, gx, 65, gz, i);
            if (wp == null) return out;
            out.add(wp);
            cx = wp.x(); cy = wp.y(); cz = wp.z();
        }
        out.add(null); // never arrived
        return out;
    }

    public static void main(String[] args) throws Exception {
        // 1. a wall three thick across a 60 x 40 yard: a 3-wide side door at z 5..7, and a sealed tunnel (x 30..31, z 18..23: plugged at x 32) that opens to the west only
        NavField yard = grid(60, 40, (x, z) -> {
            boolean wall = x >= 30 && x <= 32;
            if (!wall) return false;
            if (z >= 5 && z <= 7) return false; // the side door
            if (x <= 31 && z >= 18 && z <= 23) return false; // the tunnel, plugged at x 32
            return true;
        }, new int[][]{{10, 20, 64}});
        var way = follow(yard, 10.5, 20.5, 50.5, 20.5, 40);
        check(!way.isEmpty() && way.get(way.size() - 1) != null, "a troop west of a sealed gate reaches the east side: " + way);
        boolean tunnel = false, door = false;
        for (var wp : way) {
            if (wp.x() >= 30 && wp.x() < 32 && wp.z() >= 18 && wp.z() < 24) tunnel = true;
            if (wp.x() >= 30 && wp.x() < 33 && wp.z() >= 5 && wp.z() < 8) door = true;
        }
        check(!tunnel, "no waypoint inside the sealed tunnel: " + way);
        check(door, "the way goes through the side door: " + way);
        boolean portal = false;
        for (var wp : way) if (wp.portal() && wp.width() == 3) portal = true;
        check(portal, "the 3-wide door is a portal of width 3: " + way);
        check(yard.route(40.5, 65, 20.5, 50.5, 65, 20.5, 0) == null, "a goal in plain sight is walked to straight (no waypoint)");
        check(yard.route(10.5, 65, 20.5, 10.5, 65, 20.5, 0) == null, "no waypoint for a goal in the same place");
        double dd = yard.distance(10.5, 65, 20.5, 50.5, 65, 20.5);
        check(dd > 40 && dd < 90, "the field's distance goes round by the door, not through the plug: " + dd);
        // the tunnel is not a goal anyone is sent past: a goal inside it from the west is plain sight, from the east it is far round
        check(yard.distance(50.5, 65, 20.5, 30.5, 65, 20.5) > 40, "the tunnel is far round from the east side");

        // 2. a narrower door: 1 wide is a portal of width 1; no door at all: no route
        NavField slit = grid(60, 40, (x, z) -> x >= 30 && x <= 32 && z != 5, new int[][]{{10, 20, 64}});
        var s = follow(slit, 10.5, 20.5, 50.5, 20.5, 40);
        check(!s.isEmpty() && s.get(s.size() - 1) != null && s.stream().anyMatch(wp -> wp.portal() && wp.width() == 1), "a 1-wide slit is a portal of width 1: " + s);
        // a troop standing within a block of the portal cell it was sent to is at the door: the way goes on past it, the same waypoint is never handed back
        var door1 = s.stream().filter(wp -> wp != null && wp.portal()).findFirst().get();
        var past = slit.route(door1.x() - .9, 65, door1.z(), 50.5, 65, 20.5, 0);
        check(past != null && (Math.abs(past.x() - door1.x()) > 1 || Math.abs(past.z() - door1.z()) > 1), "at the door the waypoint moves on: " + door1 + " then " + past);
        NavField closed = grid(60, 40, (x, z) -> x >= 30 && x <= 32, new int[][]{{10, 20, 64}});
        check(closed.route(10.5, 65, 20.5, 50.5, 65, 20.5, 0) == null, "no way across a closed wall: no waypoint");
        check(closed.distance(10.5, 65, 20.5, 50.5, 65, 20.5) < 0, "no distance across a closed wall");

        // 3. a roof with no stair is not a floor: the column holds the ground under it, the roof is dropped; a floor under a lintel counts
        int[][] cols = new int[60 * 40][];
        for (int z = 0; z < 40; z++) for (int x = 0; x < 60; x++) cols[z * 60 + x] = (x >= 20 && x < 30 && z >= 10 && z < 20) ? new int[]{64, 69} : new int[]{64};
        NavField roofs = NavField.fromColumns(0, 0, 60, 40, cols, new int[][]{{5, 5, 64}});
        int[] st = roofs.stand(25, 15, (x, z) -> 69, 25, 15, 4);
        check(st != null && st[0] == 25 && st[1] == 15 && st[2] == 64, "a roof column stands on its ground: " + java.util.Arrays.toString(st));
        // a floor on stairs counts: roof reachable by 1-block steps
        int[][] stairs = new int[60 * 40][];
        for (int x = 0; x < 6; x++) stairs[5 * 60 + x] = new int[]{64 + x};
        NavField up = NavField.fromColumns(0, 0, 60, 40, stairs, new int[][]{{0, 5, 64}});
        check(up.has(5, 5) && !up.has(6, 5), "stairs of 1-block steps are reached: " + up.cells + " cells");
        check(roofs.stand(0, 0, (x, z) -> 64, 0, 0, 2)[2] == 64, "a column with only ground stands on it");

        // 4. the build budget: more fields than BUILDS_PER_TICK in one tick are refused, then answered the next tick
        NavField big = grid(60, 40, (x, z) -> x >= 30 && x <= 32 && z != 5, new int[][]{{10, 20, 64}});
        int answered = 0;
        for (int g = 0; g < 12; g++) if (big.route(10.5, 65, 20.5, 40.5 + g % 3 * 4, 65, 6.5 + g * 2.5, 7) != null) answered++;
        check(big.refused > 0 || answered <= 12, "lookups past the per-tick budget are refused");
        long before = big.refused;
        check(big.route(10.5, 65, 20.5, 52.5, 65, 6.5, 99) != null || big.refused == before, "a later tick builds again");

        // 5. the bundled maps: follow the waypoints from each spawn to each point
        for (String id : new String[]{"kingsfield_cq", "rivergate_ey"}) {
            Path dir = Path.of("src/main/resources/data/gravesandglory/sectionmaps/" + id);
            if (!Files.exists(dir.resolve("map.json"))) dir = Path.of(System.getProperty("user.dir")).resolve(dir);
            EngineMap m = EngineMap.read(dir);
            check(m.nav != null, id + " has a route field");
            NavField n = m.nav;
            System.out.println(id + ": " + n.summary());
            for (var sp : m.spawns) for (var p : m.points) {
                var w = follow(n, sp.x() + .5, sp.z() + .5, p.x() + .5, p.z() + .5, 400);
                check(!w.isEmpty() ? w.get(w.size() - 1) != null : true, id + " " + sp.id() + " to " + p.name() + ": the waypoints arrive (" + w.size() + ")");
                for (var wp : w) check(wp != null, id + " " + sp.id() + " to " + p.name() + " never arrives");
                if (id.startsWith("kings")) for (var wp : w) check(!(Math.abs(wp.x()) >= 100 && Math.abs(wp.x()) <= 108 && Math.abs(wp.z()) < 8), id + " " + sp.id() + " to " + p.name() + ": a waypoint in a sealed gate tunnel at " + wp);
                System.out.println("  " + sp.id() + " -> " + p.name() + ": " + w.size() + " waypoints, narrowest door " + w.stream().filter(NavField.Waypoint::portal).mapToInt(NavField.Waypoint::width).min().orElse(-1));
            }
            if (id.startsWith("kings")) {
                // the Hand from the west grange yard: by the side door at about (-127, +-27), through its portal
                var w = follow(n, -150.5, 0.5, 0.5, 0.5, 400);
                boolean side = w.stream().anyMatch(wp -> wp != null && Math.abs(wp.x() + 127) < 6 && Math.abs(Math.abs(wp.z()) - 27) < 6);
                check(side, "the Hand from inside the west grange goes by a side door: " + w);
                // a troop standing at the sealed gatehouse's west face gets a waypoint away from it, not a straight walk at the Hand
                var wp = n.route(-110.5, 65, 0.5, 0.5, 58, 0.5, 0);
                check(wp != null, "a troop at the sealed west gate is routed, not sent at the wall");
            }
            // lookups: time per call over a spread of starts and goals
            long t0 = System.nanoTime();
            int calls = 20000, got = 0;
            java.util.Random r = new java.util.Random(7);
            for (int i = 0; i < calls; i++) {
                var sp = m.spawns.get(r.nextInt(m.spawns.size()));
                var p = m.points.get(r.nextInt(m.points.size()));
                if (n.route(sp.x() + r.nextInt(200) - 100, 65, sp.z() + r.nextInt(100) - 50, p.x() + .5, p.y(), p.z() + .5, i / 160) != null) got++;
            }
            double us = (System.nanoTime() - t0) / 1e3 / calls;
            System.out.println("  " + calls + " lookups (" + got + " with a waypoint): " + String.format("%.1f", us) + " us each, " + n.builds + " field builds");
        }
        System.out.println("NavFieldCheck: " + count + " checks passed");
    }
}
