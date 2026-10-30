package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.function.Supplier;

import static gg.gravesandglory.Phase2Checks.report;
import static gg.gravesandglory.Phase2Checks.server;

/**
 * The shield wall on Hold (animation rework Task 2), in a real round on real ground: GRAVES_PHASE2_CHECKS=holdline.
 * <ul>
 * <li>hl-wall: twelve Order Squires and Lord Marshal Aldric of the commander's army on Hold in Line, and an Ironclad army (six Hearthguard,
 * four Shieldbreakers) on Hold in Compact, all with their AI on, start a few blocks off their slots and walk in. After 200 ticks the
 * wall's neighbours in each rank (by the places of its layout) stand 0.7 apart (the wall cell, give or take 0.05), no two bodies' boxes overlap
 * by more than 0.02, every Shieldbreaker stands behind the Hearthguard's ranks, the Marshal's place has at least his width and 0.1 of room to
 * the places beside it, the shield troops brace, and nothing still shuffles in the last 40 ticks (no jam, no stutter). The screenshot is hl-wall.png.</li>
 * <li>hl-wall-loss: then a front-rank man of the wall with a man behind him in his file is killed. 240 ticks later at most 2 of the survivors
 * have another place than they had (the man behind steps up, one closes the rank behind), the front rank is whole, every wall man stands within
 * 0.1 of his place, none moved 0.05 in the last 40 ticks, and the rank neighbours stand 0.7 apart again. The screenshot is hl-wall-loss.png.</li>
 * <li>hl-mirror: the command preview's places (CommandMode.places, from the client's own markers and group counts) are the server's slots
 * (Battle.formation) of the commander's Line on Hold after the loss, as a set of {side, back} from the order's point.</li>
 * </ul>
 */
final class HoldLineChecks {
    private HoldLineChecks() {}

    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) { suites.put("holdline", Wall::new); }

    static final class Wall extends Phase2Checks.Staged {
        final List<GravesAndGlory.Unit> knights = new ArrayList<>(), hearthguard = new ArrayList<>(), breakers = new ArrayList<>();
        GravesAndGlory.Unit marshal;
        Vec3d lineAt, blockAt, cameraAt;
        final Map<GravesAndGlory.Unit, Vec3d> settledAt = new HashMap<>();
        /** The commander's units' places ({side, back}) just before the loss, and the man killed. */
        final Map<GravesAndGlory.Unit, double[]> placeBefore = new HashMap<>();
        GravesAndGlory.Unit fallen;
        /** The server's places of the commander's Line, {side, back} from its point, as "side,back" to three places; set with the last stage. */
        volatile Set<String> serverPlaces;
        volatile boolean mirrorDone;
        int mirrorWait;

        static final double WALL = 0.7;

        static Vec3d ground(GravesAndGlory.Battle b, double x, double z) { return new Vec3d(x, Phase1Checks.top(b, x, z), z); }

        /** A level, open 36 x 17 patch of real ground west of the river (no step over a block, no water): its west end (x, standing y, z), or null (Ironclad.flat). */
        double[] flat(GravesAndGlory.Battle b) {
            for (int x = -240; x <= -40; x += 6) for (int z = -100; z <= 100; z += 6) {
                int y0 = (int) Phase1Checks.top(b, x, z); boolean open = true;
                for (int dx = -4; dx <= 32 && open; dx += 2) for (int dz = -8; dz <= 8 && open; dz += 2) {
                    int y = (int) Phase1Checks.top(b, x + dx, z + dz);
                    var below = new net.minecraft.util.math.BlockPos(x + dx, y - 1, z + dz);
                    open = !Arena.river(x + dx, z + dz) && Math.abs(y - y0) <= 1 && b.world.getBlockState(below).isSolidBlock(b.world, below);
                }
                if (open) return new double[]{x, y0, z};
            }
            return null;
        }

        /** The unit's slot as {side, back} from its order's point (the inverse of Battle.formation's turn by the order's facing). */
        static double[] sideBack(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
            var a = b.assignment(u); Vec3d p = b.formation(u);
            double angle = Math.toRadians(a.yaw), c = Math.cos(angle), s = Math.sin(angle), dx = p.x - a.target.x, dz = p.z - a.target.z;
            return new double[]{c * dx + s * dz, s * dx - c * dz};
        }
        /** How far behind its order's point the unit stands now, along the order's facing. */
        static double backNow(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
            var a = b.assignment(u); double angle = Math.toRadians(a.yaw);
            return Math.sin(angle) * (u.mob.getX() - a.target.x) - Math.cos(angle) * (u.mob.getZ() - a.target.z);
        }
        static String key(double side, double back) { return String.format(Locale.ROOT, "%.3f,%.3f", side + 0.0, back + 0.0); }

        GravesAndGlory.Unit spawn(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troop, int slot) {
            var u = b.spawn(s, Campaign.troop(troop), slot, ground(b, lineAt.x + 30, lineAt.z)); fixture.add(u); return u;
        }
        /** Every unit a few blocks off its slot: its slot's offset from the order's point spread to 1/0.7 of it, then 3 blocks out in front, on the ground, facing the order's way. */
        void scatter(GravesAndGlory.Battle b) {
            for (var u : fixture) {
                var a = b.assignment(u); Vec3d slot = b.formation(u), off = slot.subtract(a.target).multiply(1 / WALL);
                Vec3d at = ground(b, a.target.x + off.x + 3, a.target.z + off.z);
                u.mob.getNavigation().stop(); u.mob.refreshPositionAndAngles(at.x, at.y, at.z, -90, 0); u.mob.setVelocity(Vec3d.ZERO);
                u.deployed = at; u.lastPosition = at; u.nextPath = 0; u.stuckTicks = 0;
            }
        }

        @Override public void cleanup(MinecraftClient c) {
            c.options.hudHidden = false;
            server(c, b -> {clearFixture(b, "cleanup"); if (site != null) platform(b, site, 12, Blocks.AIR.getDefaultState()); unforce(b); if (b.running) b.campaign.returnLobby();});
        }

        @Override public void step(MinecraftClient c, int tick) {
            if (tick == 20) {c.setScreen(null); UUID id = c.player.getUuid(); server(c, b -> {player = id; next(b, 1);});}
            if (wantShot != null && !shotTaken) {c.options.hudHidden = true; if (++shotDelay >= shotAfter) {ScreenshotRecorder.saveScreenshot(c.runDirectory, wantShot + ".png", c.getFramebuffer(), t -> {}); shotTaken = true; c.options.hudHidden = false;}}
            if (stage == 30 && serverPlaces != null && !mirrorDone) mirror(c);
            if (stage == 99 && !finished) {finished = true; c.setScreen(null); Phase2Checks.finish(c, "holdline");}
            if (tick > 6000 && !finished) {finished = true; report("hl-timeout", false, "stuck in stage " + stage); Phase2Checks.finish(c, "holdline");}
        }

        /** hl-mirror, on the client: the preview's places from the markers and the group card, against the server's. Waits (up to 200 client ticks) for the state packet to show the whole group. */
        void mirror(MinecraftClient c) {
            var group = CommandMode.groups.stream().filter(g -> g.id().equals("infantry")).findFirst().orElse(null);
            var troops = group == null ? List.<Spec.TroopsRow>of() : CommandMode.troops(group);
            int want = serverPlaces.size();
            if ((group == null || group.count() != want || troops.size() != want) && ++mirrorWait < 200) return;
            var line = Arrays.stream(Spec.FORMATIONS).filter(f -> f.id().equals("line")).findFirst().orElseThrow();
            Set<String> client = new TreeSet<>();
            if (group != null) for (double[] p : CommandMode.places(troops, group.count(), group.id(), "hold", line)) client.add(key(p[0], p[1]));
            boolean pass = client.equals(serverPlaces) && client.size() == want;
            report("hl-mirror", pass, "the command preview of the commander's Line on Hold (" + (group == null ? "no infantry card" : group.count() + " on the card") + ", " + troops.size() + " own markers named) places " + client + "; the server's slots are " + serverPlaces + " (want the same set)");
            mirrorDone = true;
            server(c, b -> next(b, 99));
        }

        @Override public void serverTick(GravesAndGlory.Battle b) {
            if (player == null || stage >= 99) return;
            try {stages(b);} catch (RuntimeException e) {report("hl-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, 99);}
        }

        void stages(GravesAndGlory.Battle b) {
            switch (stage) {
                case 1 -> {var p = sp(b); if (p == null) return; if (b.running) {b.campaign.returnLobby(); return;} next(b, 2);}
                case 2 -> {var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return; next(b, 3);}
                case 3 -> {
                    if (since(b) < 10) return;
                    stageFixture(b);
                    double[] g = flat(b);
                    if (g == null) {report("hl-wall", false, "no level, open patch of real ground west of the river on map 0"); next(b, 99); return;}
                    lineAt = ground(b, g[0] + 20.5, g[2] - 3.5); blockAt = ground(b, g[0] + 20.5, g[2] + 5.5);
                    cameraAt = new Vec3d(g[0] + 28, lineAt.y + 2.2, g[2] + 1);
                    var p = sp(b); p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE); b.relocate(p, cameraAt.x, cameraAt.y, cameraAt.z, 90f, 16f);
                    next(b, 4);
                }
                case 4 -> {
                    if (since(b) < 40) return; // the client has the chunks round the camera
                    // the commander's army: twelve Squires (slots 0-11) and the Marshal (slot 12), on Hold in Line, facing east
                    for (int i = 0; i < 12; i++) knights.add(spawn(b, own, "knight", i));
                    marshal = spawn(b, own, "gl_marshal", 12);
                    b.issue(own, "infantry", "hold", "line", lineAt, -90, -1);
                    // an Ironclad army on Hold in Compact: six Hearthguard (slots 0-5) and four Shieldbreakers (slots 6-9)
                    dawn.order = "hold"; dawn.formation = "block"; dawn.target = blockAt; dawn.yaw = -90;
                    for (int i = 0; i < 6; i++) hearthguard.add(spawn(b, dawn, "ic_hearthguard", i));
                    for (int i = 0; i < 4; i++) breakers.add(spawn(b, dawn, "ic_breaker", 6 + i));
                    scatter(b);
                    next(b, 10);
                }
                case 10 -> {
                    if (since(b) == 160) for (var u : fixture) settledAt.put(u, u.mob.getPos());
                    if (since(b) == 190) requestShot("hl-wall", 4);
                    if (since(b) < 200) return;
                    wall(b);
                    // hl-wall-loss: a front-rank man with a man directly behind him falls
                    List<GravesAndGlory.Unit> line = wallLine();
                    for (var u : line) placeBefore.put(u, sideBack(b, u));
                    double front = 99;
                    for (var u : line) front = Math.min(front, placeBefore.get(u)[1]);
                    for (var u : line) {
                        double[] p = placeBefore.get(u); if (Math.abs(p[1] - front) > 1e-6 || u == marshal) continue;
                        boolean behind = false;
                        for (var o : line) {double[] q = placeBefore.get(o); behind |= o != u && Math.abs(q[0] - p[0]) < 1e-6 && q[1] > p[1] + 1e-6;}
                        if (behind) {fallen = u; break;}
                    }
                    if (fallen == null) {report("hl-wall-loss", false, "no front-rank Squire with a man behind him in his file"); serverPlaces = places(b); next(b, 30); return;}
                    fallen.mob.kill(); b.units.removeIf(u -> !u.mob.isAlive() || u.mob.isRemoved()); knights.remove(fallen);
                    next(b, 20);
                }
                case 20 -> {
                    if (since(b) == 200) {settledAt.clear(); for (var u : wallLine()) settledAt.put(u, u.mob.getPos());}
                    if (since(b) == 210) requestShot("hl-wall-loss", 4);
                    if (since(b) < 240) return;
                    wallLoss(b);
                    serverPlaces = places(b);
                    next(b, 30);
                }
                default -> {}
            }
        }

        /** The commander's living wall: the Squires and the Marshal. */
        List<GravesAndGlory.Unit> wallLine() {
            List<GravesAndGlory.Unit> line = new ArrayList<>();
            for (var k : knights) if (k.mob.isAlive()) line.add(k);
            if (marshal.mob.isAlive()) line.add(marshal);
            return line;
        }
        /** The server's places of the commander's living units, {side, back} from the order's point, as keys. */
        Set<String> places(GravesAndGlory.Battle b) {
            Set<String> places = new TreeSet<>();
            for (var u : fixture) if (u.squad == own && u.mob.isAlive()) {double[] sb = sideBack(b, u); places.add(key(sb[0], sb[1]));}
            return places;
        }
        /**
         * How far apart the wall's rank neighbours stand: the units grouped into ranks by the back of their places, each rank in order of side,
         * and the distance between the bodies of each two that neighbour in it: {least, most, pairs}.
         */
        static double[] rankGaps(GravesAndGlory.Battle b, List<GravesAndGlory.Unit> line) {
            Map<Long, List<GravesAndGlory.Unit>> ranks = new TreeMap<>(); Map<GravesAndGlory.Unit, double[]> at = new HashMap<>();
            for (var u : line) {double[] p = sideBack(b, u); at.put(u, p); ranks.computeIfAbsent(Math.round(p[1] * 1000), k -> new ArrayList<>()).add(u);}
            double least = 9, most = 0, pairs = 0;
            for (var rank : ranks.values()) {
                rank.sort(Comparator.comparingDouble(u -> at.get(u)[0]));
                for (int i = 1; i < rank.size(); i++) {double d = distance(rank.get(i - 1).mob.getPos(), rank.get(i).mob.getPos()); least = Math.min(least, d); most = Math.max(most, d); pairs++;}
            }
            return new double[]{least, most, pairs};
        }

        /** hl-wall, on the server after 200 ticks. */
        void wall(GravesAndGlory.Battle b) {
            List<String> why = new ArrayList<>(); boolean pass = true;
            // the wall's rank neighbours, by the places of its layout
            List<GravesAndGlory.Unit> line = wallLine();
            double[] g = rankGaps(b, line);
            boolean gaps = g[2] > 0 && g[0] >= WALL - 0.05 && g[1] <= WALL + 0.05; pass &= gaps;
            why.add("the wall's " + (int) g[2] + " pairs of rank neighbours stand " + fmt(g[0]) + " to " + fmt(g[1]) + " apart (want 0.70 +/- 0.05)=" + gaps);
            // no two bodies' boxes overlap by more than 0.02
            double overlap = 0; String worst = "none";
            for (int i = 0; i < fixture.size(); i++) for (int j = i + 1; j < fixture.size(); j++) {
                var x = fixture.get(i).mob.getBoundingBox(); var y = fixture.get(j).mob.getBoundingBox();
                double ox = Math.min(x.maxX, y.maxX) - Math.max(x.minX, y.minX), oy = Math.min(x.maxY, y.maxY) - Math.max(x.minY, y.minY), oz = Math.min(x.maxZ, y.maxZ) - Math.max(x.minZ, y.minZ);
                double o = ox > 0 && oy > 0 && oz > 0 ? Math.min(ox, oz) : 0;
                if (o > overlap) {overlap = o; worst = fixture.get(i).spec.id() + " " + fixture.get(i).slot + " and " + fixture.get(j).spec.id() + " " + fixture.get(j).slot;}
            }
            boolean apart = overlap <= 0.02; pass &= apart;
            why.add("the deepest overlap of two bodies' boxes is " + fmt(overlap) + " (" + worst + "; want at most 0.02)=" + apart);
            // every Shieldbreaker behind the Hearthguard's ranks
            double guardBack = -99, breakerFront = 99;
            for (var h : hearthguard) guardBack = Math.max(guardBack, backNow(b, h));
            for (var k : breakers) breakerFront = Math.min(breakerFront, backNow(b, k));
            boolean behind = breakerFront > guardBack + 0.3; pass &= behind;
            why.add("the Hearthguard's rearmost stands " + fmt(guardBack) + " behind the Ironclad point, the Shieldbreakers' foremost " + fmt(breakerFront) + " (want behind them)=" + behind);
            // the Marshal's room: his place to the nearest place beside it, with no slack; where he stands is reported, the box overlap above bounds it
            double marshalRoom = 9, marshalGap = 9; Vec3d marshalPlace = b.formation(marshal);
            for (var o : line) if (o != marshal) {marshalRoom = Math.min(marshalRoom, distance(marshalPlace, b.formation(o))); marshalGap = Math.min(marshalGap, distance(marshal.mob.getPos(), o.mob.getPos()));}
            double need = marshal.mob.getWidth() + 0.1;
            boolean room = marshalRoom >= need - 1e-9; pass &= room;
            why.add("Lord Marshal Aldric's place has " + fmt(marshalRoom) + " of room to the nearest place beside it (want at least his width + 0.1 = " + fmt(need) + "; he stands " + fmt(marshalGap) + " from his nearest neighbour)=" + room);
            // braced, and still: nothing jams or shuffles at 0.7
            long braced = fixture.stream().filter(u -> u.tags.contains("shield") && u.stance.equals("brace")).count(), shields = fixture.stream().filter(u -> u.tags.contains("shield")).count();
            boolean brace = braced == shields; pass &= brace;
            why.add(braced + " of " + shields + " shield troops brace (want all)=" + brace);
            // a wall troop is settled onto its slot (Tactics.settle); any other stops where its walk ends, inside the arrival tolerance, as it always has
            double moved = 0, wallOff = 0, restOff = 0, tolerance = Spec.NAVIGATION[0].arrivalTolerance(); String mover = "none", wallWorst = "none", restWorst = "none";
            for (var u : fixture) {
                Vec3d then = settledAt.get(u); double m = then == null ? 9 : distance(then, u.mob.getPos());
                if (m > moved) {moved = m; mover = u.spec.id() + " " + u.slot;}
                double o = distance(u.mob.getPos(), b.formation(u));
                if (GravesAndGlory.Battle.walls(u, b.assignment(u).order)) {if (o > wallOff) {wallOff = o; wallWorst = u.spec.id() + " " + u.slot;}}
                else if (o > restOff) {restOff = o; restWorst = u.spec.id() + " " + u.slot;}
            }
            boolean still = moved < 0.05 && wallOff < 0.1 && restOff <= tolerance + 0.05; pass &= still;
            why.add("in the last 40 ticks the most any troop moved is " + fmt(moved) + " (" + mover + "; want under 0.05: no jam, no stutter); the farthest wall troop from its slot stands " + fmt(wallOff) + " off it (" + wallWorst + "; want under 0.1), the farthest of the rest " + fmt(restOff) + " (" + restWorst + "; want within the arrival tolerance " + fmt(tolerance) + ")=" + still);
            report("hl-wall", pass, String.join("; ", why));
        }

        /** hl-wall-loss, on the server 240 ticks after the front-rank man fell. */
        void wallLoss(GravesAndGlory.Battle b) {
            List<String> why = new ArrayList<>(); boolean pass = true;
            List<GravesAndGlory.Unit> line = wallLine();
            // at most 2 survivors took another place
            List<String> movers = new ArrayList<>();
            for (var u : line) {double[] was = placeBefore.get(u), now = sideBack(b, u); if (was == null || Math.hypot(now[0] - was[0], now[1] - was[1]) > 0.05) movers.add(u.spec.id() + " " + u.slot);}
            boolean few = movers.size() <= 2; pass &= few;
            why.add(fallen.spec.id() + " " + fallen.slot + " fell from the front rank; " + movers.size() + " of the " + line.size() + " survivors took another place " + movers + " (want at most 2)=" + few);
            // the gap is closed: the front rank has its places, each held
            double front = 99; int columns = BattleMath.groupColumns("infantry", 10);
            for (var u : line) front = Math.min(front, sideBack(b, u)[1]);
            int inFront = 0; double off = 0; String worst = "none";
            for (var u : line) {
                if (Math.abs(sideBack(b, u)[1] - front) > 1e-6) continue;
                inFront++; double o = distance(u.mob.getPos(), b.formation(u));
                if (o > off) {off = o; worst = u.spec.id() + " " + u.slot;}
            }
            boolean whole = inFront == columns && off < 0.1; pass &= whole;
            why.add(inFront + " stand in the front rank (want " + columns + "), the farthest " + fmt(off) + " off his place (" + worst + "; want under 0.1)=" + whole);
            // every wall man (the ranks behind too) on his place, and still
            double wallOff = 0, moved = 0; String wallWorst = "none", mover = "none";
            for (var u : line) {
                double o = distance(u.mob.getPos(), b.formation(u)); if (o > wallOff) {wallOff = o; wallWorst = u.spec.id() + " " + u.slot;}
                Vec3d then = settledAt.get(u); double m = then == null ? 9 : distance(then, u.mob.getPos()); if (m > moved) {moved = m; mover = u.spec.id() + " " + u.slot;}
            }
            boolean settled = wallOff < 0.1 && moved < 0.05; pass &= settled;
            why.add("the farthest wall man from his place stands " + fmt(wallOff) + " off it (" + wallWorst + "; want under 0.1), the most one moved in the last 40 ticks " + fmt(moved) + " (" + mover + "; want under 0.05)=" + settled);
            double[] g = rankGaps(b, line);
            boolean gaps = g[2] > 0 && g[0] >= WALL - 0.05 && g[1] <= WALL + 0.05; pass &= gaps;
            why.add("the wall's " + (int) g[2] + " pairs of rank neighbours stand " + fmt(g[0]) + " to " + fmt(g[1]) + " apart (want 0.70 +/- 0.05)=" + gaps);
            report("hl-wall-loss", pass, String.join("; ", why));
        }
    }
}
