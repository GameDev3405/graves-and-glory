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
 * the places beside it, the shield troops brace, every wall troop faces its Hold yaw within 10 degrees (yaw and body yaw: ruling R21, Tactics.stretch),
 * and nothing still shuffles in the last 40 ticks (no jam, no stutter). The screenshot is hl-wall.png.</li>
 * <li>hl-deploy: before the round's fixture, a mixed Ironclad banner (Hearthguard with shields, Brassjaw golems without) is deployed at the
 * barracks (Battle.deploy, forward reinforcement off): no two deploy points stand closer than the two troops' half widths and 0.1.</li>
 * <li>hl-reinforce: the round-start rows (Battle.reinforce) of two Hearthguard rows and a mixed row: the wall rows one deep wall 0.7 apart, the
 * mixed row 3 behind, no two troops closer than their half widths and 0.1.</li>
 * <li>hl-wall-loss: then a front-rank man of the wall with a man behind him in his file is killed. 240 ticks later the survivors who took
 * another place are exactly the movers BattleMath.keepPlaces gives for the places they held (the man behind steps up, the rank behind slides one
 * place), the server's held places are those, nobody passed anybody, the front rank (its columns from the Line's formation row) is whole, every
 * wall man stands within 0.1 of his place, none moved 0.05 in the last 40 ticks, and the rank neighbours stand 0.7 apart again. The screenshot
 * is hl-wall-loss.png.</li>
 * <li>hl-wall-loss2: then two neighbouring front-rank Squires (the Line's second and third columns) fall at once; 240 ticks later the same, the
 * holes closed one at a time (no man more than two places, one per loss). The screenshot is hl-wall-loss2.png.</li>
 * <li>hl-mirror: the command preview's places (CommandMode.places, from the client's own markers and group counts) are the server's slots
 * (Battle.formation) of the commander's Line on Hold after the losses, as a set of {side, back} from the order's point.</li>
 * <li>hl-raise (Task 3): then, with no enemy anywhere, no shield troop of either wall holds its shield up; a frozen Dusk Order Archer out of
 * every wall troop's sight shoots one Squire through Projectiles.fire, and 4 ticks later every shield troop of his squad's group holds its shield
 * up on Hold and none of the other squad's does; a frozen Dusk Revenant pinned 12
 * blocks in front of the Line's point (in sight, outside Hold's ten-block leash) raises every shield of both walls within 20 ticks, every wall
 * troop facing its Hold yaw within 10 degrees (the screenshot is hl-raise.png); pinned 1.8 in front of a front-rank Squire, 12 ticks later every shield troop inside its reach (Abilities.range,
 * less 0.3) has lowered its shield to strike and every one beyond it (plus 0.3) still holds it up; then both walls Charge, their groups
 * stamped shot at every tick, and no shield is up on any of the next 40 ticks.</li>
 * <li>hl-crawl (Task 3): six tough Order Squires of the commander's army with full knockback resistance on the same level patch. Unfired,
 * they Move 30 blocks east: their mean speed over ticks 30-70 is v0. Back at the start, six Dusk Order Archers (AI on, pinned 11 blocks to the
 * side, on Attack) shoot them while they Move again: their mean speed over ticks 30-130 is within 15% of crawlShare times v0, with their
 * shields up and the crawl modifier (Tactics.CRAWL_ID) on. The archers removed, the 10-tick mean speed is back to 0.85 v0 with no crawl
 * modifier within fireMemoryTicks + 20. With the archers back, on Hold, the knights walking 27 blocks back to their slots under fire with
 * their shields up keep 0.85 v0 over ticks 10-30, and none carries the crawl modifier.</li>
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
        /** The same units' held places in cells ({file, rank} of BattleMath.holdGrid, from the server's Squad.places) just before the loss. */
        final Map<GravesAndGlory.Unit, double[]> gridBefore = new HashMap<>();
        final List<GravesAndGlory.Unit> fell = new ArrayList<>();
        /** The server's places of the commander's Line, {side, back} from its point, as "side,back" to three places; set with the last stage. */
        volatile Set<String> serverPlaces;
        volatile boolean mirrorDone;
        int mirrorWait;
        /** The level patch flat found (its west end x, standing y, z). */
        double[] patch;
        // hl-raise
        GravesAndGlory.Unit foe, front, archer, shotKnight;
        final List<String> raiseWhy = new ArrayList<>();
        boolean raisePass = true;
        int chargeRaised, chargeTicks;
        // hl-crawl
        final List<GravesAndGlory.Unit> crawlers = new ArrayList<>(), archers = new ArrayList<>();
        final List<String> crawlWhy = new ArrayList<>();
        boolean crawlPass = true;
        Vec3d crawlFrom, crawlTo;
        final Map<GravesAndGlory.Unit, Vec3d> snap = new HashMap<>();
        final Map<Long, Map<GravesAndGlory.Unit, Vec3d>> track = new HashMap<>();
        double v0, v1;
        long recoveredAt;
        int crawlingAtRemoval, holdCrawled, holdUp, holdFire;

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
            if (tick > 9000 && !finished) {finished = true; report("hl-timeout", false, "stuck in stage " + stage); Phase2Checks.finish(c, "holdline");}
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
            server(c, b -> next(b, 40));
        }

        @Override public void serverTick(GravesAndGlory.Battle b) {
            if (player == null || stage >= 99) return;
            hold(); // the pinned enemies of hl-raise and hl-crawl stand where they were put
            try {stages(b); if (stage >= 40) shieldStages(b);} catch (RuntimeException e) {report("hl-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, 99);}
        }

        void stages(GravesAndGlory.Battle b) {
            switch (stage) {
                case 1 -> {var p = sp(b); if (p == null) return; if (b.running) {b.campaign.returnLobby(); return;} next(b, 2);}
                case 2 -> {var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return; next(b, 3);}
                case 3 -> {
                    if (since(b) < 10) return;
                    stageFixture(b);
                    deploy(b);
                    double[] g = flat(b); patch = g;
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
                    // hl-wall-loss: a front-rank man with a man directly behind him falls (the first such Squire, beside the rear rank's flank: its rank slides)
                    List<GravesAndGlory.Unit> line = wallLine();
                    record(b, line);
                    GravesAndGlory.Unit fallen = null;
                    for (var u : line) {
                        double[] p = gridBefore.get(u); if (p == null || p[1] != 0 || u == marshal) continue;
                        boolean behind = false;
                        for (var o : line) {double[] q = gridBefore.get(o); behind |= o != u && q != null && q[0] == p[0] && q[1] > 0;}
                        if (behind) {fallen = u; break;}
                    }
                    if (fallen == null) {report("hl-wall-loss", false, "no front-rank Squire with a man behind him in his file (held places " + gridBefore.size() + ")"); serverPlaces = places(b); next(b, 30); return;}
                    fall(b, List.of(fallen));
                    next(b, 20);
                }
                case 20 -> {
                    if (since(b) == 200) {settledAt.clear(); for (var u : wallLine()) settledAt.put(u, u.mob.getPos());}
                    if (since(b) == 210) requestShot("hl-wall-loss", 4);
                    if (since(b) < 240) return;
                    wallLoss(b, "hl-wall-loss");
                    // hl-wall-loss2: the Squires on the Line's second and third columns of the front rank fall at once
                    List<GravesAndGlory.Unit> line = wallLine(); record(b, line);
                    double left = -(columns() - 1) / 2.0; GravesAndGlory.Unit second = null, third = null;
                    for (var u : line) {double[] p = gridBefore.get(u); if (p == null || p[1] != 0 || u == marshal) continue; if (p[0] == left + 1) second = u; if (p[0] == left + 2) third = u;}
                    if (second == null || third == null) {report("hl-wall-loss2", false, "no two Squires on the second and third columns of the front rank"); serverPlaces = places(b); next(b, 30); return;}
                    fall(b, List.of(second, third));
                    next(b, 25);
                }
                case 25 -> {
                    if (since(b) == 200) {settledAt.clear(); for (var u : wallLine()) settledAt.put(u, u.mob.getPos());}
                    if (since(b) == 210) requestShot("hl-wall-loss2", 4);
                    if (since(b) < 240) return;
                    wallLoss(b, "hl-wall-loss2");
                    serverPlaces = places(b);
                    next(b, 30);
                }
                default -> {}
            }
        }

        /** The Line's formation row (Spec.FORMATIONS), the commander's wall's shape. */
        static Spec.FormationsRow lineRow() { return Arrays.stream(Spec.FORMATIONS).filter(f -> f.id().equals("line")).findFirst().orElseThrow(); }
        /** How many abreast the commander's infantry stands in Line, from the formation row (as Battle.formation counts them). */
        static int columns() { return BattleMath.groupColumns("infantry", lineRow().columns()); }
        /** Each wall man's place now, in blocks from the order's point and in cells from the server's held places (Squad.places). */
        void record(GravesAndGlory.Battle b, List<GravesAndGlory.Unit> line) {
            placeBefore.clear(); gridBefore.clear();
            for (var u : line) placeBefore.put(u, sideBack(b, u)); // Battle.formation, which refreshes the held places
            var held = own.places.get("infantry");
            for (var u : line) {double[] g = held == null ? null : held.grid.get(u); if (g != null) gridBefore.put(u, g.clone());}
        }
        /** These wall men fall at once. */
        void fall(GravesAndGlory.Battle b, List<GravesAndGlory.Unit> dead) {
            fell.clear(); fell.addAll(dead);
            for (var u : dead) u.mob.kill();
            b.units.removeIf(u -> !u.mob.isAlive() || u.mob.isRemoved()); knights.removeAll(dead);
        }
        /** The survivors' places in cells as BattleMath.keepPlaces gives them from the places they held before the loss (the wall in slot order, as Battle.formation lists it). */
        Map<GravesAndGlory.Unit, double[]> expected(List<GravesAndGlory.Unit> line) {
            List<GravesAndGlory.Unit> members = new ArrayList<>(line);
            members.sort(Comparator.<GravesAndGlory.Unit>comparingInt(m -> m.slot).thenComparingInt(m -> m.mob.getId()));
            boolean[] shield = new boolean[members.size()]; double[][] prev = new double[members.size()][];
            for (int i = 0; i < shield.length; i++) {shield[i] = GravesAndGlory.Battle.walls(members.get(i), "hold"); prev[i] = gridBefore.get(members.get(i));}
            int[] at = BattleMath.keepPlaces(prev, shield, columns(), lineRow().wedge());
            double[][] grid = BattleMath.holdGrid(shield, columns(), lineRow().wedge());
            Map<GravesAndGlory.Unit, double[]> out = new HashMap<>();
            for (int i = 0; i < at.length; i++) out.put(members.get(i), grid[at[i]]);
            return out;
        }
        static String name(GravesAndGlory.Unit u) { return u.spec.id() + " " + u.slot; }

        /**
         * hl-deploy: a mixed Ironclad banner, Hearthguard (shield) and Brassjaw (no shield) in every row, deployed at Dusk's barracks
         * (Battle.deploy with forward reinforcement off, each slot as its own troop trains): no two deploy points closer than the two troops'
         * half widths and 0.1. Dusk's banner and the battle's flag are restored; nothing spawns.
         */
        void deploy(GravesAndGlory.Battle b) {
            String mix = "HHBHH" + "HHHHH" + "BH.BH";
            List<String> banner = new ArrayList<>(), was = dusk.banner; boolean forward = b.forwardReinforcement;
            for (char k : mix.toCharArray()) banner.add(k == 'H' ? "ic_hearthguard" : k == 'B' ? "ic_colossus" : "");
            dusk.banner = banner; b.forwardReinforcement = false;
            try {
                List<Vec3d> at = new ArrayList<>(); List<Double> width = new ArrayList<>(); int troops = 0, shields = 0;
                for (int i = 0; i < banner.size(); i++) {
                    var spec = Campaign.troop(banner.get(i));
                    if (spec == null) {at.add(null); width.add(0.0); continue;}
                    troops++; if (ArmyMobs.tags(spec).contains("shield")) shields++;
                    at.add(b.deploy(dusk, i, spec).at()); width.add(ArmyMobs.width(spec));
                }
                double worst = 9, wallGap = 9; String pair = "none";
                for (int i = 0; i < at.size(); i++) for (int j = i + 1; j < at.size(); j++) {
                    if (at.get(i) == null || at.get(j) == null) continue;
                    double d = distance(at.get(i), at.get(j)), slack = d - (width.get(i) / 2 + width.get(j) / 2 + 0.1);
                    if (slack < worst) {worst = slack; pair = banner.get(i) + " " + i + " and " + banner.get(j) + " " + j + " " + fmt(d) + " apart";}
                    if (j == i + 1 && i / 5 == j / 5 && banner.get(i).equals("ic_hearthguard") && banner.get(j).equals("ic_hearthguard")) wallGap = Math.min(wallGap, d);
                }
                boolean pass = troops == mix.replace(".", "").length() && shields == mix.replace("B", "").replace(".", "").length() && worst >= -1e-6 && Math.abs(wallGap - WALL) < 1e-6;
                report("hl-deploy", pass, troops + " troops (" + shields + " Hearthguard, the rest Brassjaw " + fmt(ArmyMobs.width(Campaign.troop("ic_colossus"))) + " wide) of a mixed banner " + mix + " deploy at the barracks; the least slack of two deploy points over their half widths and 0.1 is " + fmt(worst) + " (" + pair + "; want at least 0), two Hearthguard side by side in a row stand " + fmt(wallGap) + " apart (want the wall cell " + fmt(WALL) + ")=" + pass);
            } finally {dusk.banner = was; b.forwardReinforcement = forward;}
            reinforce(b);
        }
        /**
         * hl-reinforce: the round-start rows (Battle.reinforce) of a banner of two rows of Hearthguard and a mixed row: the two wall rows stand the
         * wall cell apart (one deep wall, BattleMath.deployDepths), the mixed row the row gap 3 behind, Hearthguard side by side in a row stand
         * the wall cell apart, and no two troops closer than their half widths and 0.1. The troops are discarded at once.
         */
        void reinforce(GravesAndGlory.Battle b) {
            String mix = "HHHHH" + "HHHHH" + "BH.BH";
            List<String> banner = new ArrayList<>(), was = dusk.banner;
            for (char k : mix.toCharArray()) banner.add(k == 'H' ? "ic_hearthguard" : k == 'B' ? "ic_colossus" : "");
            dusk.banner = banner;
            List<GravesAndGlory.Unit> rows = new ArrayList<>();
            try {
                b.reinforce(dusk);
                for (var u : b.units) if (u.squad == dusk) rows.add(u);
                Map<Integer, GravesAndGlory.Unit> bySlot = new HashMap<>(); for (var u : rows) bySlot.put(u.slot, u);
                double worst = 9, wallGap = 9; String pair = "none";
                for (int i = 0; i < rows.size(); i++) for (int j = i + 1; j < rows.size(); j++) {
                    var p = rows.get(i); var q = rows.get(j);
                    double d = distance(p.mob.getPos(), q.mob.getPos()), slack = d - (p.mob.getWidth() / 2 + q.mob.getWidth() / 2 + 0.1);
                    if (slack < worst) {worst = slack; pair = name(p) + " and " + name(q) + " " + fmt(d) + " apart";}
                    if (Math.abs(p.slot - q.slot) == 1 && p.slot / 5 == q.slot / 5 && p.spec.id().equals("ic_hearthguard") && q.spec.id().equals("ic_hearthguard")) wallGap = Math.min(wallGap, d);
                }
                var a = bySlot.get(0); var c = bySlot.get(5); var e = bySlot.get(11);
                double walls = a == null || c == null ? -1 : Math.abs(a.mob.getX() - c.mob.getX()), back = c == null || e == null ? -1 : Math.abs(c.mob.getX() - e.mob.getX());
                boolean pass = rows.size() == mix.replace(".", "").length() && worst >= -1e-6 && Math.abs(wallGap - WALL) < 1e-6 && Math.abs(walls - WALL) < 1e-6 && Math.abs(back - 3) < 1e-6;
                report("hl-reinforce", pass, rows.size() + " troops of the banner " + mix + " take their round-start rows: the two Hearthguard rows stand " + fmt(walls) + " apart (want the wall cell " + fmt(WALL) + "), the mixed row " + fmt(back) + " behind them (want 3), Hearthguard side by side " + fmt(wallGap) + " apart (want " + fmt(WALL) + "), the least slack of two troops over their half widths and 0.1 " + fmt(worst) + " (" + pair + "; want at least 0)=" + pass);
            } finally {
                for (var u : b.units) if (u.squad == dusk) u.mob.discard();
                b.units.removeIf(u -> u.squad == dusk); dusk.banner = was;
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
            String[] face = facing(b); boolean faces = face[0].equals("true"); pass &= faces; why.add(face[1] + "=" + faces);
            report("hl-wall", pass, String.join("; ", why));
        }
        /** Every living wall troop of the fixture (Battle.walls) faces its Hold yaw within 10 degrees, yaw and body yaw (ruling R21): whether, and the line. */
        String[] facing(GravesAndGlory.Battle b) {
            int n = 0; double worst = 0; String who = "none"; List<String> askew = new ArrayList<>();
            for (var u : fixture) {
                if (!u.mob.isAlive() || !GravesAndGlory.Battle.walls(u, b.assignment(u).order)) continue;
                n++; float want = b.assignment(u).yaw;
                double d = Math.max(Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(u.mob.getYaw() - want)), Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(u.mob.bodyYaw - want)));
                if (d > worst) {worst = d; who = name(u);}
                if (d > 10) askew.add(name(u) + " " + fmt(d));
            }
            boolean ok = n > 0 && askew.isEmpty();
            return new String[]{String.valueOf(ok), n + " wall troops face their Hold yaw, yaw and body: the worst " + fmt(worst) + " degrees off (" + who + "; want within 10)" + (askew.isEmpty() ? "" : ", askew " + askew)};
        }

        /**
         * hl-wall-loss, on the server 240 ticks after the front-rank man fell: the gap closes by short moves (the man behind steps up, the rank
         * slides one place toward the gap), nobody passes anybody, the men beyond the sliding stretch stand, and everyone is on his place.
         */
        void wallLoss(GravesAndGlory.Battle b, String check) {
            List<String> why = new ArrayList<>(); boolean pass = true;
            List<GravesAndGlory.Unit> line = wallLine(); int losses = fell.size();
            // who took another place, and how far: at most one cell sideways or one rank per loss
            List<String> movers = new ArrayList<>(); Set<String> took = new TreeSet<>(); Map<GravesAndGlory.Unit, double[]> now = new HashMap<>(); double far = 0; String farthest = "none";
            for (var u : line) {
                double[] was = placeBefore.get(u), p = sideBack(b, u); now.put(u, p);
                if (was == null) {movers.add(name(u) + " (no place before)"); took.add(name(u)); far = 9; farthest = name(u); continue;}
                if (Math.hypot(p[0] - was[0], p[1] - was[1]) <= 0.05) continue;
                movers.add(name(u) + " " + fmt(was[0]) + "," + fmt(was[1]) + "->" + fmt(p[0]) + "," + fmt(p[1])); took.add(name(u));
                double d = Math.max(Math.abs(p[0] - was[0]), Math.abs(p[1] - was[1])); if (d > far) {far = d; farthest = name(u);}
            }
            boolean shortMoves = far <= losses * WALL + 0.01; pass &= shortMoves;
            List<String> dead = new ArrayList<>(); for (var u : fell) dead.add(name(u));
            why.add(dead + " fell from the front rank; " + movers.size() + " of the " + line.size() + " survivors took another place " + movers + ", the longest step " + fmt(far) + " sideways or back (" + farthest + "; want at most one cell per loss, " + fmt(losses * WALL) + ")=" + shortMoves);
            // exactly the movers keepPlaces gives (one loss at a time), and the server's held places are those
            Map<GravesAndGlory.Unit, double[]> want = expected(line); var held = own.places.get("infantry");
            Set<String> wantMoved = new TreeSet<>(); boolean heldSame = held != null; String heldOff = "none";
            for (var u : line) {
                double[] e = want.get(u), g = gridBefore.get(u), h = held == null ? null : held.grid.get(u);
                if (e == null || g == null || !Arrays.equals(e, g)) wantMoved.add(name(u));
                if (e == null || h == null || !Arrays.equals(e, h)) {heldSame = false; heldOff = name(u) + " holds " + (h == null ? "nothing" : Arrays.toString(h)) + ", want " + (e == null ? "nothing" : Arrays.toString(e));}
            }
            boolean exact = took.equals(wantMoved) && !wantMoved.isEmpty() && heldSame; pass &= exact;
            why.add("the movers are " + took + " (" + took.size() + "), keepPlaces gives " + wantMoved + " (" + wantMoved.size() + "); the server's held places are keepPlaces' (" + heldOff + ")=" + exact);
            // nobody passed anybody: along each rank (as it stands now) the men keep the order they stood in before (two from different ranks may share a file)
            Map<Long, List<GravesAndGlory.Unit>> newRanks = new HashMap<>(), oldRanks = new HashMap<>();
            for (var u : line) if (placeBefore.get(u) != null) {
                newRanks.computeIfAbsent(Math.round(now.get(u)[1] * 1000), k -> new ArrayList<>()).add(u);
                oldRanks.computeIfAbsent(Math.round(placeBefore.get(u)[1] * 1000), k -> new ArrayList<>()).add(u);
            }
            boolean order = true; String crossed = "none";
            for (var rank : newRanks.values()) {
                rank.sort(Comparator.comparingDouble(u -> now.get(u)[0]));
                for (int i = 1; i < rank.size(); i++) if (placeBefore.get(rank.get(i))[0] < placeBefore.get(rank.get(i - 1))[0] - 1e-6) {order = false; crossed = rank.get(i - 1).spec.id() + " " + rank.get(i - 1).slot + " and " + rank.get(i).spec.id() + " " + rank.get(i).slot;}
            }
            pass &= order;
            why.add("along every rank the men keep their order (passed: " + crossed + ")=" + order);
            // the men beyond the sliding stretch stand: in each rank as it stood, the movers are side by side
            boolean segment = true; String beyond = "none";
            for (var rank : oldRanks.values()) {
                rank.sort(Comparator.comparingDouble(u -> placeBefore.get(u)[0]));
                int first = -1, last = -1;
                for (int i = 0; i < rank.size(); i++) {double[] was = placeBefore.get(rank.get(i)), p = now.get(rank.get(i)); if (Math.hypot(p[0] - was[0], p[1] - was[1]) > 0.05) {if (first < 0) first = i; last = i;}}
                for (int i = Math.max(first, 0); i <= last; i++) {double[] was = placeBefore.get(rank.get(i)), p = now.get(rank.get(i)); if (Math.hypot(p[0] - was[0], p[1] - was[1]) <= 0.05) {segment = false; beyond = rank.get(i).spec.id() + " " + rank.get(i).slot;}}
            }
            pass &= segment;
            why.add("in every rank the movers are one stretch, the men beyond it stand (a stander inside a stretch: " + beyond + ")=" + segment);
            // the gap is closed: the front rank has its places, each held
            double front = 99; int columns = columns();
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
            report(check, pass, String.join("; ", why));
        }

        /** The fixture's living shield troops of the commander's side (both walls, team 0). */
        List<GravesAndGlory.Unit> shieldTroops() {
            List<GravesAndGlory.Unit> out = new ArrayList<>();
            for (var u : fixture) if (u.mob.isAlive() && u.squad.team == 0 && u.tags.contains("shield")) out.add(u);
            return out;
        }
        /** Whether the troop carries the crawl's movement-speed modifier. */
        static boolean crawling(GravesAndGlory.Unit u) {
            var speed = u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
            return speed != null && speed.getModifier(Tactics.CRAWL_ID) != null;
        }
        /** A Dusk troop at 4000 health pinned at this point (re-placed each tick by hold()), facing west. */
        GravesAndGlory.Unit pinned(GravesAndGlory.Battle b, String troop, int slot, Vec3d at, boolean freeze) {
            var u = tough(b.spawn(dusk, Campaign.troop(troop), slot, at)); if (freeze) Phase1Checks.freeze(u);
            fixture.add(u); pins.add(new Pin(u, at, 90f)); u.deployed = at; Phase2Checks.Engine.apply(u, at, 90f); return u;
        }
        /** These pinned troops leave the battle. */
        void unpin(GravesAndGlory.Battle b, List<GravesAndGlory.Unit> gone) {
            for (var u : gone) u.mob.discard();
            b.units.removeAll(gone); fixture.removeAll(gone); pins.removeIf(p -> gone.contains(p.u()));
        }
        /** The six knights put down at this x on the knights' lane, three abreast, facing east. */
        void lineUp(GravesAndGlory.Battle b, double x) {
            for (int i = 0; i < crawlers.size(); i++) {
                var u = crawlers.get(i); Vec3d at = ground(b, x - i / 3, crawlFrom.z + (i % 3 - 1));
                u.mob.getNavigation().stop(); Phase2Checks.Engine.apply(u, at, -90f); u.deployed = at; u.lastPosition = at; u.nextPath = 0; u.stuckTicks = 0;
            }
        }
        /** Six Dusk Order Archers on Attack, AI on, pinned in a row 11 blocks south of the knights' lane. */
        void archersUp(GravesAndGlory.Battle b) {
            dusk.order = "attack"; dusk.assignments.clear();
            for (int i = 0; i < 6; i++) archers.add(pinned(b, "archer", i, ground(b, patch[0] + 6.5 + 2 * i, crawlFrom.z + 11), false));
        }
        void snap(Map<GravesAndGlory.Unit, Vec3d> into) {into.clear(); for (var u : crawlers) into.put(u, u.mob.getPos());}
        /** The knights' mean horizontal speed (blocks a tick) from the positions then to now, over this many ticks. */
        double speed(Map<GravesAndGlory.Unit, Vec3d> then, double ticks) {
            double sum = 0; int n = 0;
            for (var u : crawlers) {Vec3d p = then.get(u); if (p != null) {sum += distance(p, u.mob.getPos()); n++;}}
            return n == 0 ? 0 : sum / n / ticks;
        }

        /** hl-raise (stages 40-43) and hl-crawl (stages 50-56), on the server after the Task 2 stages. */
        void shieldStages(GravesAndGlory.Battle b) {
            switch (stage) {
                case 40 -> {
                    if (since(b) < 20) return;
                    var shields = shieldTroops(); long up = shields.stream().filter(u -> u.mob.isUsingItem()).count();
                    boolean none = up == 0 && !shields.isEmpty(); raisePass &= none;
                    raiseWhy.add("with no enemy anywhere " + up + " of " + shields.size() + " shield troops hold their shield up (want none)=" + none);
                    // a frozen Dusk Order Archer out of every wall troop's sight (beyond scouting range) shoots one Squire through Projectiles.fire
                    Vec3d far = null;
                    for (double[] o : new double[][]{{-45, 0}, {0, -45}, {0, 45}, {45, 0}, {-40, -20}, {-40, 20}}) {
                        double x = lineAt.x + o[0], z = lineAt.z + o[1];
                        if (!GravesAndGlory.Battle.inArena(x, z) || Arena.ground((int) Math.floor(x), (int) Math.floor(z)) < 64) continue;
                        Vec3d at = ground(b, x, z); double range = Spec.SCOUTING[0].range();
                        if (shields.stream().allMatch(u -> u.mob.getPos().distanceTo(at) > range + 2)) {far = at; break;}
                    }
                    if (far == null) {raisePass = false; raiseWhy.add("no spot in the arena out of the walls' sight for the unseen archer=false"); foe = pinned(b, "revenant", 0, ground(b, lineAt.x + 12, lineAt.z), true); next(b, 41); return;}
                    archer = pinned(b, "archer", 1, far, true); shotKnight = null;
                    for (var k : knights) if (k.mob.isAlive()) {shotKnight = k; break;}
                    Projectiles.fire(b, archer, shotKnight.mob, "arrow", 0.5f);
                    next(b, 44);
                }
                case 44 -> {
                    if (since(b) < 4) return;
                    int group = 0, groupUp = 0, other = 0, otherUp = 0;
                    for (var u : shieldTroops()) {
                        boolean mine = u.squad == shotKnight.squad && u.spec.group().equals(shotKnight.spec.group());
                        if (mine) {group++; if (u.mob.isUsingItem()) groupUp++;} else {other++; if (u.mob.isUsingItem()) otherUp++;}
                    }
                    double nearest = 1e9; for (var u : shieldTroops()) nearest = Math.min(nearest, u.mob.getPos().distanceTo(archer.mob.getPos()));
                    boolean unseen = group > 0 && groupUp == group && otherUp == 0; raisePass &= unseen;
                    raiseWhy.add("a frozen Dusk Order Archer " + fmt(nearest) + " from the nearest wall troop (beyond sight, " + fmt(Spec.SCOUTING[0].range()) + ") shoots " + name(shotKnight) + " (Projectiles.fire): 4 ticks later " + groupUp + " of " + group + " shield troops of his squad's group hold their shields up on Hold (want all), " + otherUp + " of " + other + " of the other squad (want none: not shot at)=" + unseen);
                    var shooter = archer; b.shots.removeIf(q -> {if (q.shooter() != shooter) return false; if (q.visual() != null) q.visual().discard(); return true;}); // the arrow never lands: its knockback would put the shot knight out of the wall
                    unpin(b, List.of(archer)); archer = null; own.shotAt.clear(); dawn.shotAt.clear();
                    foe = pinned(b, "revenant", 0, ground(b, lineAt.x + 12, lineAt.z), true);
                    next(b, 41);
                }
                case 41 -> {
                    if (since(b) < 20) return;
                    var shields = shieldTroops(); long up = shields.stream().filter(u -> u.mob.isUsingItem()).count();
                    boolean all = up == shields.size() && !shields.isEmpty(); raisePass &= all;
                    raiseWhy.add("a frozen Dusk Revenant " + fmt(distance(foe.mob.getPos(), lineAt)) + " in front of the Line's point (outside Hold's leash, in sight): " + up + " of " + shields.size() + " shield troops raise their shields within 20 ticks (want all)=" + all);
                    String[] face = facing(b); boolean faces = face[0].equals("true"); raisePass &= faces; raiseWhy.add("with their shields up " + face[1] + "=" + faces);
                    requestShot("hl-raise", 4);
                    for (var k : knights) if (k.mob.isAlive() && (front == null || backNow(b, k) + 0.01 * Math.abs(k.mob.getZ() - lineAt.z) < backNow(b, front) + 0.01 * Math.abs(front.mob.getZ() - lineAt.z))) front = k;
                    Vec3d at = new Vec3d(front.mob.getX() + 1.8, front.mob.getY(), front.mob.getZ());
                    pins.removeIf(p -> p.u() == foe); pins.add(new Pin(foe, at, 90f)); Phase2Checks.Engine.apply(foe, at, 90f);
                    next(b, 42);
                }
                case 42 -> {
                    if (since(b) < 12) return;
                    int inReach = 0, beyond = 0, wrong = 0; List<String> bad = new ArrayList<>();
                    for (var u : shieldTroops()) {
                        double d = Math.sqrt(u.mob.squaredDistanceTo(foe.mob)), r = Abilities.range(b, u); boolean up = u.mob.isUsingItem();
                        if (d < r - 0.3) {inReach++; if (up) {wrong++; bad.add(name(u) + " up at " + fmt(d));}}
                        else if (d > r + 0.3) {beyond++; if (!up) {wrong++; bad.add(name(u) + " down at " + fmt(d));}}
                    }
                    boolean lowered = !front.mob.isUsingItem(), rule = wrong == 0 && inReach > 0 && beyond > 0 && lowered; raisePass &= rule;
                    raiseWhy.add("the Revenant pinned 1.8 in front of " + name(front) + ": 12 ticks later " + inReach + " shield troops stand inside their reach (less 0.3) and " + beyond + " beyond it (plus 0.3); " + wrong + " hold the wrong way " + bad + "; " + name(front) + " lowered=" + lowered + " (want every one in reach down to strike, every one beyond up)=" + rule);
                    b.issue(own, "infantry", "charge", "line", foe.mob.getPos(), -90, foe.mob.getId());
                    b.issue(dawn, "all", "charge", "block", foe.mob.getPos(), -90, foe.mob.getId());
                    for (var u : shieldTroops()) u.squad.shotAt.put(u.spec.group(), b.time);
                    chargeRaised = 0; chargeTicks = 0;
                    next(b, 43);
                }
                case 43 -> {
                    boolean fire = true;
                    for (var u : shieldTroops()) {u.squad.shotAt.put(u.spec.group(), b.time); fire &= Tactics.underFire(b, u); if (u.mob.isUsingItem()) chargeRaised++;}
                    chargeTicks++;
                    if (since(b) < 40) return;
                    boolean never = chargeRaised == 0 && fire; raisePass &= never;
                    raiseWhy.add("both walls ordered to Charge, their groups shot at every tick (under fire " + fire + "): " + chargeRaised + " shield-troop ticks raised over " + chargeTicks + " ticks (want 0)=" + never);
                    report("hl-raise", raisePass, String.join("; ", raiseWhy));
                    next(b, 50);
                }
                case 50 -> {
                    clearFixture(b, "hl-crawl"); knights.clear(); hearthguard.clear(); breakers.clear(); foe = null; front = null;
                    own.assignments.clear(); own.arrived.clear(); own.shotAt.clear();
                    crawlFrom = ground(b, patch[0] + 1.5, patch[2] - 2.5); crawlTo = ground(b, crawlFrom.x + 30, crawlFrom.z);
                    for (int i = 0; i < 6; i++) {
                        var u = tough(b.spawn(own, Campaign.troop("knight"), i, crawlFrom)); fixture.add(u); crawlers.add(u);
                        var kb = u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE); if (kb != null) kb.setBaseValue(1);
                    }
                    lineUp(b, crawlFrom.x);
                    b.issue(own, "infantry", "hold", "line", crawlFrom, -90, -1);
                    var p = sp(b); b.relocate(p, crawlFrom.x + 15, Phase1Checks.top(b, crawlFrom.x + 15, crawlFrom.z - 14) + 2.2, crawlFrom.z - 14, 0f, 20f);
                    next(b, 51);
                }
                case 51 -> {
                    if (since(b) < 60) return;
                    b.issue(own, "infantry", "move", "line", crawlTo, -90, -1);
                    next(b, 52);
                }
                case 52 -> { // unfired: v0 over ticks 30-70 of the Move
                    if (since(b) == 30) snap(snap);
                    if (since(b) < 70) return;
                    v0 = speed(snap, 40);
                    long crawled = crawlers.stream().filter(Wall::crawling).count();
                    boolean ok = v0 > 0.05 && crawled == 0; crawlPass &= ok;
                    crawlWhy.add("unfired, the six Squires Move at " + fmt(v0 * 20) + " blocks a second (ticks 30-70; " + crawled + " with the crawl modifier, want 0)=" + ok);
                    lineUp(b, crawlFrom.x); b.issue(own, "infantry", "hold", "line", crawlFrom, -90, -1);
                    archersUp(b);
                    next(b, 53);
                }
                case 53 -> {
                    if (since(b) < 40) return;
                    b.issue(own, "infantry", "move", "line", crawlTo, -90, -1);
                    next(b, 54);
                }
                case 54 -> { // under fire: v1 over ticks 30-130 of the Move
                    if (since(b) == 30) snap(snap);
                    if (since(b) == 80) {
                        long up = crawlers.stream().filter(u -> u.mob.isUsingItem()).count(), crawled = crawlers.stream().filter(Wall::crawling).count();
                        boolean fire = Tactics.underFire(b, crawlers.get(0)), on = up == crawlers.size() && crawled == crawlers.size() && fire; crawlPass &= on;
                        crawlWhy.add("80 ticks into the Move under the archers' fire " + up + " of 6 hold their shields up and " + crawled + " carry the crawl modifier (group under fire " + fire + "; want all)=" + on);
                    }
                    if (since(b) < 130) return;
                    v1 = speed(snap, 100);
                    double share = Spec.COMBATDISCIPLINE[0].crawlShare(), want = share * v0; boolean slow = Math.abs(v1 - want) <= 0.15 * want; crawlPass &= slow;
                    crawlWhy.add("under fire they Move at " + fmt(v1 * 20) + " blocks a second (ticks 30-130), " + fmt(v1 / Math.max(v0, 1e-9)) + " of v0 (want crawlShare " + fmt(share) + " x v0 = " + fmt(want * 20) + " +/- 15%)=" + slow);
                    crawlingAtRemoval = (int) crawlers.stream().filter(Wall::crawling).count();
                    unpin(b, archers); archers.clear(); track.clear(); recoveredAt = -1;
                    next(b, 55);
                }
                case 55 -> { // the archers gone: the 10-tick mean speed back to 0.85 v0 with no crawl modifier within fireMemoryTicks + 20
                    long t = since(b); Map<GravesAndGlory.Unit, Vec3d> now = new HashMap<>(); snap(now); track.put(t, now);
                    int limit = Spec.COMBATDISCIPLINE[0].fireMemoryTicks() + 20;
                    if (recoveredAt < 0 && track.containsKey(t - 10) && speed(track.get(t - 10), 10) >= 0.85 * v0 && crawlers.stream().noneMatch(Wall::crawling)) recoveredAt = t;
                    if (t < limit) return;
                    boolean back = recoveredAt >= 0 && recoveredAt <= limit; crawlPass &= back;
                    crawlWhy.add("the archers removed (" + crawlingAtRemoval + " crawling then): the 10-tick mean speed is back to 0.85 v0 with no crawl modifier " + (recoveredAt < 0 ? "never" : recoveredAt + " ticks") + " later (want within fireMemoryTicks + 20 = " + limit + ")=" + back);
                    // Hold: the knights walk back to their slots 27 blocks west, under fire, shields up, never crawling
                    lineUp(b, crawlFrom.x + 27); archersUp(b);
                    b.issue(own, "infantry", "hold", "line", crawlFrom, -90, -1); own.shotAt.put("infantry", b.time);
                    holdCrawled = 0; holdUp = 0; holdFire = 0;
                    next(b, 56);
                }
                case 56 -> {
                    long t = since(b);
                    if (t >= 10) for (var u : crawlers) {if (crawling(u)) holdCrawled++; if (u.mob.isUsingItem()) holdUp++; if (Tactics.underFire(b, u)) holdFire++;}
                    if (t == 10) snap(snap);
                    if (t < 30) return;
                    double vh = speed(snap, 20); int samples = 21 * crawlers.size();
                    boolean free = vh >= 0.85 * v0 && holdCrawled == 0 && holdUp == samples && holdFire == samples; crawlPass &= free;
                    crawlWhy.add("on Hold, walking back to their slots under fire (" + holdFire + " of " + samples + " knight-ticks under fire, " + holdUp + " with the shield up), they keep " + fmt(vh * 20) + " blocks a second over ticks 10-30, " + fmt(vh / Math.max(v0, 1e-9)) + " of v0 (want at least 0.85), " + holdCrawled + " knight-ticks crawling (want 0)=" + free);
                    report("hl-crawl", crawlPass, String.join("; ", crawlWhy));
                    unpin(b, archers); archers.clear();
                    next(b, 99);
                }
                default -> {}
            }
        }
    }
}
