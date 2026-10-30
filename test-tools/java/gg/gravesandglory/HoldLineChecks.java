package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.BlockPos;
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
 * <li>hl-arc (Task 4, fix rounds 1 and 2): on the same patch, a frozen dummy of the commander's army; east of it, in turn (ARC_CASES), a frozen Dusk
 * troop fires one shot of its own kind (Abilities.visual) at it through Projectiles.fire, and the arrow's place and attitude are read after each
 * tick's move. At 22 blocks: the Order Archer's peak over the straight line from bow to target is 5.5 +/- 0.15 (volleyPeakPerBlock x 22, under
 * volleyPeakMax); the Arbalist's crossbow bolt is crossbowArcScale (0.4) of the bow's and the Harpooner's trident thrownArcScale (0.6) of it,
 * +/- 0.02; the Ballista crew's bolt arcs as a crossbow (R24), crossbowArcScale of the bow's +/- 0.05; the Mortar crew's shot stays within 0.05 of
 * the line and a Mist Caller's bolt is a particle shot with no arc. Then an Order Archer 5 blocks off (two ticks of flight) peaks within 10%
 * (or 0.15) of its sheet peak; one 22 blocks off under a stone roof 3 blocks over the ground at the middle of the flight still arcs, peaking at
 * the roof's cap (its underside over the line's middle less COVER_MARGIN) +/- 0.02, and never stands in a block (M2, N2); one shoots the dummy as
 * it walks sideways (ARC_WALK a tick) and, leading it by a tick (N6), lands within 0.05 of it (M4). Every shot leaves the queue and hurts the dummy
 * exactly travelTicks (times arcTravelScale, 1) after it was fired, its arrow within 0.3 of the dummy's body then (where the battle aimed on that
 * tick); with the dummy still, every tick's place is within 0.05 of the curve (R26). Every tick, halfway through it (as the renderer eases the
 * attitude over the tick) the arrow points along that tick's move within 1 degree (R25, N1): on the server, where on a long arc one tick's end
 * attitude is also within 2 degrees of the next one's start, and on the client against the move it drew, nose up while it climbs and down while
 * it falls. The last place the client draws the arrow is within 0.3 of where it landed (N7). An untagged no-clip, no-gravity arrow that no battle
 * fired keeps vanilla's attitude (N8). Then six Dusk Order Archers (AI on, pinned, on Attack) shoot a line of six frozen dummies 20 blocks
 * off: their damage a second over ticks 40-440 is within 3% of ARC_DPS_BASE, which this stage measured at BASE (straight shots) on the same
 * world seed. The screenshot, hl-arc.png, is the bow's arrow near its peak, seen from the side.</li>
 * <li>hl-impact (Task 5): on the same patch, a Dusk Order Squire (AI on, pinned, on Attack) 1.8 east of a frozen dummy of the commander's army.
 * Each of three swings hurts the dummy exactly its swing's impactTicks (attackVariants) after the swing tick, never on the swing tick, and no
 * swing repeats the one before. The dummy carried 6 blocks off on a swing's tick takes nothing (the blow misses), the swing's cooldown stands,
 * and the Squire's next swing comes at its first 10-tick beat after the cooldown. A Squire killed on its swing's tick deals nothing. A dummy on
 * 1 health killed by a fresh Squire's blow on its impact frame credits that Squire the bounty once, and his army one kill and the dummy's
 * army one loss; a dummy killed by a generic kill on the Squire's swing tick is nobody's kill (a miss, no credit). A round ended on a swing's
 * tick leaves no blow pending, and none lands in the next round. Task 5 fix round 1: a swing on tick 59 of a fresh Charge's 60-tick window,
 * its order turned to Hold during the wind-up, still crashes in when its blow lands past the window (counters row C5, Ruling R29). A second
 * swing tried while a blow is pending queues nothing. A Squire stunned a tick after its swing deals nothing (a miss). A blow landing inside
 * the dummy's vanilla hurt cooldown (a hit 3 ticks before the impact frame, the cooldown left intact) is judged by vanilla's rule then, as
 * an instant blow on that tick was at BASE: nothing after a larger hit, only its excess (through the dummy's armour) after a smaller one.
 * Abilities.attack on a melee troop deals nothing and queues nothing (its blow is Tactics.swing's). After the next round has begun, a map
 * switch with a blow pending clears it and none lands once the battle runs again; the round then restarts on map 0.</li>
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
        /** hl-raise's per-tick shield sample under fire on Hold: the knight set by the Revenant, the samples, how many down, and who. */
        GravesAndGlory.Unit nearKnight;
        int fireSamples, fireDown; final List<String> fireDownAt = new ArrayList<>();
        // hl-wall's per-tick facing sample (fix round 2, R2): each wall troop's position a tick ago, and the samples
        final Map<GravesAndGlory.Unit, Vec3d> lastAt = new HashMap<>();
        int faceSamples, faceBack, faceSide, faceAir, faceRouted; final List<String> faceRoutedWho = new ArrayList<>(); double faceWorst; String faceWho = "none"; final List<String> faceBad = new ArrayList<>();
        // hl-house (fix round 2, R1)
        GravesAndGlory.Unit houseKnight; Vec3d houseSlot, houseStart; String houseText; long houseAt; double houseBest = 99; int houseStuckBefore;
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
        // hl-arc (Task 4)
        /** One of hl-arc's single shots: who fires it, how far east of the dummy it stands, and what the case adds ("" none, "short", "roof", "moving"). */
        record ArcCase(String id, double east, String kind) {}
        /**
         * hl-arc's single shots, in turn: at 22 blocks a bow, a crossbow, a thrown trident, the Ballista crew's crossbow bolt (R24), the Mortar crew's
         * straight shot and a Mist Caller's bolt (particles, no arrow); then a bow 5 blocks off (two ticks of flight), one under a roof, and one at a
         * dummy walking sideways.
         */
        static final ArcCase[] ARC_CASES = {new ArcCase("archer", 22, ""), new ArcCase("ic_arbalist", 22, ""), new ArcCase("tc_harpooner", 22, ""), new ArcCase("ic_ballista", 22, ""),
            new ArcCase("ic_mortar", 22, ""), new ArcCase("tc_mist", 22, ""), new ArcCase("archer", 5, "short"), new ArcCase("archer", 22, "roof"), new ArcCase("archer", 22, "moving")};
        /** How far the moving case's dummy walks south each tick of the flight: 4 blocks a second, a troop's walk. */
        static final double ARC_WALK = 0.2;
        /**
         * The damage a second six Dusk Order Archers deal to the six-dummy line of hl-arc's volley stage at BASE 2cc6d9a (Projectiles before Task 4:
         * straight shots), measured by this check on the same world seed before the arc went in; the arc must leave it within 3%.
         */
        static final double ARC_DPS_BASE = 10.560; // 211.20 over the 400 ticks in both BASE runs, test-tools/work/t4/base-1 and base-2
        final List<String> arcWhy = new ArrayList<>();
        boolean arcPass = true;
        GravesAndGlory.Unit arcTarget, arcShooter;
        Projectiles.Shot arcShot;
        net.minecraft.entity.Entity arcArrow;
        /** The shot's arrow where it stands after each tick's move (age 1 on), the last its place on the landing tick. */
        final List<Vec3d> arcPath = new ArrayList<>();
        /** The arrow's attitude on the server with each place in arcPath: {yaw, pitch, prevYaw, prevPitch}, the renderer easing from the prev pair to the other over the tick. */
        final List<float[]> arcTurn = new ArrayList<>();
        /**
         * The arrow's entity id while the client should watch it (-1: none), and what the client saw after each of its ticks: {yaw, pitch, prevYaw,
         * prevPitch, x, y, z, prevX, prevY, prevZ}; the client draws the tick's move from the prev place to the other, its nose eased from the prev attitude.
         */
        volatile int arcClientId = -1;
        final List<double[]> arcClient = java.util.Collections.synchronizedList(new ArrayList<>());
        /** The dummy's pinned place; where the battle aimed at it when the shot was fired, and on the tick it last ticked (read before this tick's pins). */
        Vec3d arcHome, arcFireAim, arcAim;
        /** The roof case's blocks, with what stood there before. */
        final Map<BlockPos, net.minecraft.block.BlockState> arcRoof = new HashMap<>();
        /** The roof's block layer (its underside), and where the landed shot's arrow stood on its landing tick on the server. */
        int arcRoofY; Vec3d arcEnd;
        /** hl-arc's decoy (N8): an untagged no-clip, no-gravity arrow no battle fired. */
        net.minecraft.entity.Entity arcDecoy;
        /** The landed shot's result, finished once the client has seen the whole flight. */
        String arcPendingWhy; boolean arcPendingPass, arcPendingArrow, arcPendingArced; int arcPendingTicks;
        float arcHealth;
        int arcIndex;
        double bowPeak = Double.NaN, arcLostAt;
        final List<GravesAndGlory.Unit> arcDummies = new ArrayList<>();
        // hl-impact (Task 5)
        /** hl-impact's Dusk Order Squire (AI on, pinned) and the frozen dummy it strikes; where the dummy stands. */
        GravesAndGlory.Unit impKnight, impTarget;
        Vec3d impHome;
        /** The swing hl-impact watches (its tick and its pending blow), the last swing tick seen, the tick the dummy was hurt or died (-1 not yet), and a saved tick. */
        long impSwingAt = -1, impSeen = -1, impHurtAt = -1, impNext;
        GravesAndGlory.Battle.PendingStrike impStrike;
        float impHealth;
        int impSwings, impHurts, impLastVariant = -1, impMissed, impLanded, impCredits, impKills, impLost, impBounty;
        /** Fix round 1: the Charge stamp the watched swing fell under, the hit put on the dummy before an impact frame and its hurt cooldown then, the hit log's last entry before a step. */
        long impCharged; float impHit; int impRegen; Combat.HitLog impLog;
        boolean impPass = true;
        final List<String> impWhy = new ArrayList<>();

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
            // the ends of each squad's ranks (the troops farthest to a side) come in from 1.7 blocks out to that side instead, so the wall is seen stepping in sideways as well as backwards
            Map<GravesAndGlory.Squad, Double> widest = new HashMap<>();
            for (var u : fixture) widest.merge(u.squad, Math.abs(side(b, u)), Math::max);
            for (var u : fixture) {
                var a = b.assignment(u); Vec3d slot = b.formation(u), off = slot.subtract(a.target).multiply(1 / WALL);
                double side = side(b, u), angle = Math.toRadians(a.yaw);
                Vec3d at = Math.abs(side) >= widest.get(u.squad) - 0.01 ? ground(b, slot.x + Math.cos(angle) * Math.signum(side) * 1.7, slot.z + Math.sin(angle) * Math.signum(side) * 1.7)
                    : ground(b, a.target.x + off.x + 3, a.target.z + off.z);
                u.mob.getNavigation().stop(); u.mob.refreshPositionAndAngles(at.x, at.y, at.z, -90, 0); u.mob.setVelocity(Vec3d.ZERO);
                u.deployed = at; u.lastPosition = at; u.nextPath = 0; u.stuckTicks = 0;
            }
        }

        /** The troop's slot to the side of its order's point (along the rank: Battle.formation's side axis). */
        static double side(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
            var a = b.assignment(u); Vec3d off = b.formation(u).subtract(a.target); double angle = Math.toRadians(a.yaw);
            return off.x * Math.cos(angle) + off.z * Math.sin(angle);
        }
        /**
         * hl-wall's per-tick facing sample (R2): every living wall troop within 2 blocks of its slot that moved more than 0.02 in the last tick faces its Hold yaw within
         * 15 degrees, yaw and body yaw, on every such tick. Each sample is sorted by the way its step went against its facing: back (more than 135 degrees off), side (45-135)
         * or ahead; in the air counts apart. With the stretch off (Tactics.stretch returning false) the walk turns them to their steps and this goes red (fix round 2).
         */
        void sampleFacing(GravesAndGlory.Battle b) {
            for (var u : fixture) {
                if (!u.mob.isAlive() || !GravesAndGlory.Battle.walls(u, b.assignment(u).order)) continue;
                Vec3d now = u.mob.getPos(), then = lastAt.put(u, now); if (then == null) continue;
                double dx = now.x - then.x, dz = now.z - then.z, moved = Math.sqrt(dx * dx + dz * dz);
                if (moved <= 0.02 || distance(now, b.formation(u)) > 2) continue;
                // R1: where walk routes him (a detour round a body, or the straight line to his slot not clear) the stretch yields and he walks without the facing; counted apart, not held to the yaw
                if (u.detour != null || !Tactics.clear(b, u, b.formation(u))) {faceRouted++; if (faceRoutedWho.size() < 6) faceRoutedWho.add(name(u) + (u.detour != null ? " on a detour" : " with the line blocked") + " at " + fmt(distance(now, b.formation(u)))); continue;}
                float want = b.assignment(u).yaw; faceSamples++;
                double off = Math.max(Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(u.mob.getYaw() - want)), Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(u.mob.bodyYaw - want)));
                double step = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - want));
                if (step > 135) faceBack++; else if (step >= 45) faceSide++;
                if (!u.mob.isOnGround()) faceAir++;
                if (off > faceWorst) {faceWorst = off; faceWho = name(u);}
                if (off > 15 && faceBad.size() < 6) faceBad.add(name(u) + " " + fmt(off) + " at " + fmt(distance(now, b.formation(u))) + (b.time < u.stretchOff ? " (let go as stuck)" : "") + (u.hopTo != null ? " (hopping)" : "") + (u.lockedTarget != null && u.lockedTarget.isAlive() ? " (fighting)" : ""));
                else if (off > 15) faceBad.add("");
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
            int arcId = arcClientId; // hl-arc: the shot's arrow as the client draws it, after the client's own tick
            if (arcId >= 0 && c.world != null) {var e = c.world.getEntityById(arcId); if (e != null) arcClient.add(new double[]{e.getYaw(), e.getPitch(), e.prevYaw, e.prevPitch, e.getX(), e.getY(), e.getZ(), e.prevX, e.prevY, e.prevZ});}
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
            if (stage == 73 && arcTarget != null) arcAim = Projectiles.aim(arcTarget.mob); // where the battle aimed this tick, before the pins move the dummy on
            hold(); // the pinned enemies of hl-raise and hl-crawl stand where they were put
            try {stages(b); if (stage >= 40) shieldStages(b); if (stage >= 70) arcStages(b); if (stage >= 80) impactStages(b);} catch (RuntimeException e) {report("hl-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, 99);}
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
                    sampleFacing(b);
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
            long bad = faceBad.size(); boolean walkIn = faceSamples >= 20 && bad == 0 && faceBack > 0 && faceSide > 0 && faceRouted * 4 <= faceSamples; pass &= walkIn;
            why.add("walking in, every tick: " + faceSamples + " samples of a wall troop within 2 blocks of its slot moving over 0.02 a tick (" + faceBack + " stepping back, " + faceSide + " sideways, " + (faceSamples - faceBack - faceSide) + " ahead, " + faceAir + " in the air), " + bad + " more than 15 degrees off its Hold yaw" + (bad == 0 ? "" : " " + faceBad.stream().filter(x -> !x.isEmpty()).toList()) + ", the worst " + fmt(faceWorst) + " (" + faceWho + "); besides, " + faceRouted + " ticks walk routed him (the stretch yields, no facing)" + (faceRouted == 0 ? "" : " " + faceRoutedWho) + " (want at least 20 samples, some back and some sideways, none off, and routed ticks at most a quarter of the samples)=" + walkIn);
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
                    // m1: a Squire of the shot group set 1.5 from the Revenant (in sword reach, outside Hold's leash of 10 from the Line's point, so never picked), the group shot at every tick
                    for (var k : knights) if (k.mob.isAlive() && k != shotKnight) {nearKnight = k; break;}
                    Vec3d by = ground(b, foe.mob.getX() - 1.5, foe.mob.getZ());
                    pins.add(new Pin(nearKnight, by, -90f)); Phase2Checks.Engine.apply(nearKnight, by, -90f);
                    fireSamples = 0; fireDown = 0; fireDownAt.clear();
                    next(b, 46);
                }
                case 46 -> {
                    own.shotAt.put("infantry", b.time); dawn.shotAt.put("infantry", b.time);
                    long t = since(b);
                    if (t >= 6) for (var u : shieldTroops()) {fireSamples++; if (!u.mob.isUsingItem()) {fireDown++; if (fireDownAt.size() < 6) fireDownAt.add(name(u) + " at tick " + t);}}
                    if (t < 46) return;
                    double d = Math.sqrt(nearKnight.mob.squaredDistanceTo(foe.mob)), leash = distance(foe.mob.getPos(), lineAt);
                    boolean steady = fireSamples > 0 && fireDown == 0 && d < Abilities.range(b, nearKnight) && leash > 10;
                    raisePass &= steady;
                    raiseWhy.add("on Hold under fire (their groups shot at every tick), with " + name(nearKnight) + " set " + fmt(d) + " from the Revenant (in his reach " + fmt(Abilities.range(b, nearKnight)) + ", the Revenant " + fmt(leash) + " from the Line's point: outside Hold's leash of 10, never picked): over ticks 6-46 " + fireDown + " of " + fireSamples + " shield-troop ticks had the shield down (want 0: up every tick, no drop on the beat)" + (fireDownAt.isEmpty() ? "" : " " + fireDownAt) + "=" + steady);
                    pins.removeIf(p -> p.u() == nearKnight);
                    var a = b.assignment(nearKnight); Vec3d back = b.formation(nearKnight);
                    nearKnight.mob.getNavigation().stop(); Phase2Checks.Engine.apply(nearKnight, back, a.yaw); nearKnight.mob.setVelocity(Vec3d.ZERO); nearKnight.lastPosition = back; nearKnight.stuckTicks = 0;
                    own.shotAt.clear(); dawn.shotAt.clear();
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
                    next(b, 70);
                }
                case 60 -> { // hl-house (R1): the fixture leaves; the camera goes to the supply houses west of the river so their chunks are loaded
                    clearFixture(b, "hl-house"); knights.clear(); hearthguard.clear(); breakers.clear(); crawlers.clear(); marshal = null;
                    for (var r : Tactics.housesOfMap()) if (r.x() < 0) force(b, new BlockPos(r.x() - 24, 0, r.z() - 24), new BlockPos(r.x() + 24, 0, r.z() + 24));
                    var r = Arrays.stream(Tactics.housesOfMap()).filter(h -> h.x() < 0).findFirst().orElse(null);
                    var p = sp(b); if (r != null && p != null) b.relocate(p, r.x() - 26, Phase1Checks.top(b, r.x() - 26, r.z()) + 8, r.z(), -90f, 20f);
                    next(b, 61);
                }
                case 61 -> {
                    if (since(b) < 60) return;
                    double[] w = houseWall(b);
                    if (w == null) {report("hl-house", false, "no stretch of the outer wall of a supply house west of the river with open level ground 3 deep inside it and 1 outside, away from the gates"); next(b, 99); return;}
                    var r = Tactics.housesOfMap()[(int) w[0]]; double dx = w[3], dz = w[4];
                    // the wall block (w[1], w[2]); its inner face is half a block in from its centre; the slot 0.35 inside it, the troop 0.4 outside its outer face
                    houseSlot = new Vec3d(w[1] + .5 - dx * (.5 + .35), w[5], w[2] + .5 - dz * (.5 + .35));
                    houseStart = new Vec3d(w[1] + .5 + dx * (.5 + .4), w[6], w[2] + .5 + dz * (.5 + .4));
                    float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)); // facing out over the wall
                    houseKnight = b.spawn(own, Campaign.troop("knight"), 0, houseStart); fixture.add(houseKnight);
                    houseKnight.mob.refreshPositionAndAngles(houseStart.x, houseStart.y, houseStart.z, yaw, 0);
                    b.issue(own, "infantry", "hold", "line", houseSlot, yaw, -1);
                    Vec3d f = b.formation(houseKnight);
                    b.issue(own, "infantry", "hold", "line", houseSlot.add(houseSlot.x - f.x, 0, houseSlot.z - f.z), yaw, -1);
                    f = b.formation(houseKnight);
                    boolean blocked = !Tactics.clear(b, houseKnight, f);
                    double[] gate = HouseRoute.next(r.x(), r.z(), r.rotation(), houseStart.x, houseStart.z, f.x, f.z, 0);
                    houseText = "a Squire " + fmt(distance(houseStart, f)) + " from his Hold slot, outside the wall of " + r.id() + " at (" + (int) w[1] + "," + (int) w[2] + ") with his slot 0.35 inside it (slot off by " + fmt(distance(f, houseSlot)) + "); the straight line blocked (the stretch must yield)=" + blocked + ", routed by a gate (HouseRoute.next " + (gate == null ? "none" : "(" + fmt(gate[0]) + "," + fmt(gate[1]) + ")") + ")";
                    houseAt = b.time; houseBest = 99; houseStuckBefore = Tactics.stretchStuck;
                    if (!blocked || gate == null || distance(f, houseSlot) > 0.05 || distance(houseStart, f) > 2) {report("hl-house", false, houseText + " (want blocked, a gate, the slot where it was put and the Squire within 2 of it)"); next(b, 99); return;}
                    next(b, 62);
                }
                case 62 -> {
                    double d = distance(houseKnight.mob.getPos(), b.formation(houseKnight)); houseBest = Math.min(houseBest, d);
                    long t = since(b); boolean there = d <= Spec.NAVIGATION[0].arrivalTolerance() && houseKnight.mob.isAlive();
                    if (!there && t < 1800) return;
                    report("hl-house", there, houseText + ": " + (there ? "he reached it in " + t + " ticks (" + fmt(t / 20.0) + " s)" : "he did not reach it in " + t + " ticks; the nearest he came " + fmt(houseBest) + ", standing at (" + fmt(houseKnight.mob.getX()) + "," + fmt(houseKnight.mob.getY()) + "," + fmt(houseKnight.mob.getZ()) + ")") + ", " + fmt(d) + " from it (want within the arrival tolerance " + fmt(Spec.NAVIGATION[0].arrivalTolerance()) + " within 1800 ticks); stretch let-gos while he walked " + (Tactics.stretchStuck - houseStuckBefore) + "=" + there);
                    next(b, 80);
                }
                default -> {}
            }
        }

        /** A frozen Order Archer of the commander's army at 4000 health pinned at this point, facing east: a target of hl-arc. */
        GravesAndGlory.Unit dummy(GravesAndGlory.Battle b, int slot, Vec3d at) {
            var u = tough(b.spawn(own, Campaign.troop("archer"), slot, at)); Phase1Checks.freeze(u);
            fixture.add(u); pins.add(new Pin(u, at, -90f)); u.deployed = at; Phase2Checks.Engine.apply(u, at, -90f); return u;
        }
        /** How far p stands over the straight line from a to c, at p's place along that line seen from above. */
        static double overLine(Vec3d a, Vec3d c, Vec3d p) {
            double hx = c.x - a.x, hz = c.z - a.z, hh = hx * hx + hz * hz;
            double along = hh == 0 ? 0 : ((p.x - a.x) * hx + (p.z - a.z) * hz) / hh;
            return p.y - (a.y + along * (c.y - a.y));
        }
        /** The health the volley's dummies have lost so far. */
        double arcLost() {double sum = 0; for (var u : arcDummies) sum += u.mob.getMaxHealth() - u.mob.getHealth(); return sum;}

        /** hl-arc (stages 70-77, Task 4): one shot from each of ARC_CASES at a pinned dummy, an untagged decoy arrow, then a six-archer volley's damage a second. */
        void arcStages(GravesAndGlory.Battle b) {
            var cd = Spec.COMBATDISCIPLINE[0];
            switch (stage) {
                case 70 -> {
                    clearFixture(b, "hl-arc"); crawlers.clear(); archers.clear(); Projectiles.clear(b);
                    own.assignments.clear(); own.arrived.clear(); own.shotAt.clear(); dusk.assignments.clear(); dusk.shotAt.clear();
                    arcHome = ground(b, patch[0] + 2, patch[2]); arcTarget = dummy(b, 0, arcHome);
                    // the camera 8 blocks north of the 22-block flights' middle, looking south (yaw 0) and up at them, so an arrow is seen side on (the single shooters are frozen: none shoots the commander)
                    var p = sp(b); b.relocate(p, arcHome.x + 11, Phase1Checks.top(b, arcHome.x + 11, arcHome.z - 8) + 1, arcHome.z - 8, 0f, -25f);
                    arcIndex = 0;
                    next(b, 71);
                }
                case 71 -> { // the next shooter, frozen (it never shoots of its own accord) and pinned east of the dummy, facing it
                    if (since(b) < 20) return;
                    var ac = ARC_CASES[arcIndex];
                    arcShooter = pinned(b, ac.id(), 1, ground(b, arcHome.x + ac.east(), arcHome.z), true);
                    if (ac.kind().equals("roof")) { // a stone roof 3 x 3, 3 blocks over the ground, over the middle of the flight
                        double mid = arcHome.x + ac.east() / 2; int y = (int) Math.floor(ground(b, mid, arcHome.z).y) + 3; arcRoofY = y;
                        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                            var at = new BlockPos((int) Math.floor(mid) + dx, y, (int) Math.floor(arcHome.z) + dz);
                            arcRoof.put(at, b.world.getBlockState(at)); b.world.setBlockState(at, Blocks.STONE.getDefaultState());
                        }
                    }
                    next(b, 72);
                }
                case 72 -> { // its shot, of the kind its own attack fires (Abilities.visual), through Projectiles.fire
                    if (since(b) < 2) return;
                    arcTarget.mob.timeUntilRegen = 0; arcTarget.mob.hurtTime = 0;
                    arcFireAim = Projectiles.aim(arcTarget.mob); arcAim = arcFireAim;
                    Projectiles.fire(b, arcShooter, arcTarget.mob, Abilities.visual(arcShooter), 2f);
                    arcShot = b.shots.get(b.shots.size() - 1); arcArrow = arcShot.visual(); arcPath.clear(); arcTurn.clear(); arcHealth = arcTarget.mob.getHealth();
                    arcClient.clear(); arcClientId = arcArrow == null ? -1 : arcArrow.getId();
                    if (arcIndex == 0) requestShot("hl-arc", 7); // the bow's arrow near its peak (tick 5 of 10; the client draws a tick behind the server)
                    next(b, 73);
                }
                case 73 -> { // after each tick's move (the world ticks the arrow before the battle steers or lands it) its place and attitude; the landing tick's are its last
                    if (arcArrow != null) {arcPath.add(arcArrow.getPos()); arcTurn.add(new float[]{arcArrow.getYaw(), arcArrow.getPitch(), arcArrow.prevYaw, arcArrow.prevPitch});}
                    var ac = ARC_CASES[arcIndex];
                    if (b.shots.contains(arcShot)) {
                        if (ac.kind().equals("moving")) walk(arcHome.add(0, 0, ARC_WALK * since(b)));
                        if (since(b) <= 60) return;
                        arcPass = false; arcWhy.add(ac.id() + " " + ac.kind() + "'s shot still in flight 60 ticks after it was fired=false");
                        Projectiles.clear(b); arcPendingWhy = null;
                    } else arcLanded(b, cd, ac);
                    for (var r : arcRoof.entrySet()) b.world.setBlockState(r.getKey(), r.getValue());
                    arcRoof.clear(); walk(arcHome);
                    unpin(b, List.of(arcShooter)); arcShooter = null; arcShot = null; arcArrow = null;
                    next(b, 74);
                }
                case 74 -> { // five ticks on, the client has drawn the whole flight: where its arrow ended, and its attitude every tick against the move it drew
                    if (since(b) < 5) return;
                    arcClientId = -1;
                    List<double[]> seen; synchronized (arcClient) {seen = new ArrayList<>(arcClient); arcClient.clear();}
                    if (arcPendingWhy != null) {
                        boolean pass = arcPendingPass; String why = arcPendingWhy;
                        if (arcPendingArrow) {
                            // R25 and N1: the renderer eases the nose from the prev attitude to the current one while it draws the tick's move from the prev place to
                            // the current one; halfway through the tick the nose points along the move (no lag), and its ends and the step from one tick's end to the
                            // next one's start are measured too (N1's apex number)
                            double lag = 0, ends = 0, jump = 0, firstPitch = Double.NaN, lastPitch = Double.NaN; int n = 0; double[] moved = null;
                            for (var t : seen) {
                                double dx = t[4] - t[7], dy = t[5] - t[8], dz = t[6] - t[9];
                                if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= 0.01) continue;
                                n++; double midYaw = (t[2] + t[0]) / 2, midPitch = (t[3] + t[1]) / 2;
                                lag = Math.max(lag, noseOff(midYaw, midPitch, dx, dy, dz));
                                ends = Math.max(ends, Math.max(noseOff(t[2], t[3], dx, dy, dz), noseOff(t[0], t[1], dx, dy, dz)));
                                if (moved != null) jump = Math.max(jump, attitudeOff(moved[0], moved[1], t[2], t[3]));
                                if (n == 1) firstPitch = midPitch; lastPitch = midPitch; moved = t;
                            }
                            // N7: the last place the client drew the arrow (it is gone the tick after) is where it landed on the server
                            double[] last = seen.isEmpty() ? null : seen.get(seen.size() - 1);
                            double arrive = last == null || arcEnd == null ? 1e9 : arcEnd.distanceTo(new Vec3d(last[4], last[5], last[6]));
                            double lastMove = moved == null || arcEnd == null ? Double.NaN : arcEnd.distanceTo(new Vec3d(moved[4], moved[5], moved[6]));
                            int want = Math.max(1, arcPendingTicks - 1); boolean upDown = arcPendingArced && arcPendingTicks >= 2;
                            boolean client = n >= want && lag <= 1 && arrive <= 0.3 && (!upDown || firstPitch > 0 && lastPitch < 0); pass &= client;
                            why += "; on the client " + n + " ticks of flight seen (want " + want + " or more), its last drawn place " + fmt(arrive) + " from where it landed (want 0.3 at most; "
                                + fmt(lastMove) + " at the end of its last move), the nose halfway through each tick at most " + fmt(lag) + " degrees off that tick's move (want 1 at most; at the tick's ends "
                                + fmt(ends) + ", " + fmt(jump) + " from one tick's end to the next one's start), its pitch " + fmt(firstPitch) + " first and " + fmt(lastPitch) + " last" + (upDown ? " (want nose up, then down)" : "");
                        }
                        arcPass &= pass; arcWhy.add(why + "=" + pass);
                    }
                    arcPendingWhy = null; arcEnd = null;
                    next(b, ++arcIndex < ARC_CASES.length ? 71 : 75);
                }
                case 75 -> { // N8: a no-clip, no-gravity arrow flying east that no battle fired keeps vanilla's own attitude, on the server and the client
                    // Vanilla, for this arrow: the server holds the -90 it was spawned with (a no-clip arrow's yaw is atan2(-vx, -vz), backward). The client
                    // first turns a new arrow to its velocity the flying way (setVelocityClient: +90), then eases a fifth of the way back to -90 each tick,
                    // so after 15 ticks it stands within 180 x 0.8^15 = 6.3 degrees of it. A battle shot's steering would hold it nose first at +90.
                    if (arcDecoy == null) {
                        var a = new net.minecraft.entity.projectile.ArrowEntity(b.world, arcHome.x + 6, arcHome.y + 4, arcHome.z);
                        a.setNoGravity(true); a.setNoClip(true); a.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.DISALLOWED;
                        a.setVelocity(0.05, 0, 0); a.setYaw(-90f); a.setPitch(0f); a.prevYaw = -90f; a.prevPitch = 0f;
                        synchronized (arcClient) {arcClient.clear();}
                        b.world.spawnEntity(a); arcDecoy = a; arcClientId = a.getId();
                        return;
                    }
                    if (since(b) < 20) return;
                    arcClientId = -1;
                    List<double[]> seen; synchronized (arcClient) {seen = new ArrayList<>(arcClient); arcClient.clear();}
                    double server = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(arcDecoy.getYaw() + 90)); int moving = 0;
                    for (var t : seen) if (Math.abs(t[4] - t[7]) > 0.01) moving++;
                    double client = seen.isEmpty() ? 1e9 : Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(seen.get(seen.size() - 1)[0] + 90));
                    boolean vanilla = moving >= 15 && server <= 5 && client <= 10; arcPass &= vanilla;
                    arcWhy.add("an untagged no-clip, no-gravity arrow flying east that no battle fired (N8): its yaw on the server " + fmt(server) + " degrees off vanilla's -90 for a no-clip arrow (want 5 at most), on the client "
                        + fmt(client) + " off it on the last of " + moving + " ticks of its flight seen (want 10 at most, after 15 ticks or more of vanilla easing it round from +90; a battle shot's steering would hold it nose first, at +90)=" + vanilla);
                    arcDecoy.discard(); arcDecoy = null;
                    next(b, 76);
                }
                case 76 -> { // the volley; it starts on a 20-tick boundary of the battle clock, so the archers' timers and the 10-tick sight refresh fall alike in every run
                    if (b.time % 20 != 0) return;
                    unpin(b, List.of(arcTarget)); arcTarget = null; Projectiles.clear(b);
                    // the camera 34 blocks north of the patch, out of every archer's reach (an enemy player is a target)
                    var p = sp(b); b.relocate(p, patch[0] + 13, Phase1Checks.top(b, patch[0] + 13, patch[2] - 34) + 6, patch[2] - 34, 0f, 15f);
                    dusk.order = "attack"; dusk.assignments.clear(); dusk.shotAt.clear(); own.shotAt.clear();
                    for (int i = 0; i < 6; i++) arcDummies.add(dummy(b, 10 + i, ground(b, patch[0] + 4, patch[2] - 5 + 2 * i)));
                    for (int i = 0; i < 6; i++) archers.add(pinned(b, "archer", i, ground(b, patch[0] + 24, patch[2] - 5 + 2 * i), false));
                    arcLostAt = 0;
                    next(b, 77);
                }
                case 77 -> { // damage a second over ticks 40-440 of the volley, against the baseline measured at BASE
                    long t = since(b);
                    if (t == 40) arcLostAt = arcLost();
                    if (t < 440) return;
                    double dps = (arcLost() - arcLostAt) / 20.0;
                    long alive = arcDummies.stream().filter(u -> u.mob.isAlive()).count();
                    boolean steady = dps > 0 && alive == arcDummies.size() && Math.abs(dps - ARC_DPS_BASE) <= 0.03 * ARC_DPS_BASE; arcPass &= steady;
                    arcWhy.add(String.format(Locale.ROOT, "six Dusk Order Archers (AI on, pinned, on Attack) shooting six frozen dummies 20 blocks off (%d of 6 alive) deal %.3f damage a second over ticks 40-440 (%.2f in all; want the BASE baseline %.3f +/- 3%%, from %.3f to %.3f)", alive, dps, dps * 20, ARC_DPS_BASE, ARC_DPS_BASE * 0.97, ARC_DPS_BASE * 1.03) + "=" + steady);
                    report("hl-arc", arcPass, String.join("; ", arcWhy));
                    unpin(b, archers); archers.clear(); unpin(b, arcDummies); arcDummies.clear(); Projectiles.clear(b);
                    next(b, 60);
                }
                default -> {}
            }
        }
        /** hl-impact's dummy stands here from now on (its pin moves with it). */
        void impMove(Vec3d at) {pins.removeIf(p -> p.u() == impTarget); pins.add(new Pin(impTarget, at, -90f)); Phase2Checks.Engine.apply(impTarget, at, -90f);}
        /** A new swing of hl-impact's Squire on this tick (Tactics.swing stamps swungAt): its pending blow; null when it did not swing this tick. */
        GravesAndGlory.Battle.PendingStrike impSwung(GravesAndGlory.Battle b) {
            if (impKnight == null || impKnight.swungAt != b.time || impKnight.swungAt == impSeen) return null;
            impSeen = impKnight.swungAt;
            for (var s : b.strikes) if (s.attacker() == impKnight && s.landsAt() > b.time) return s;
            impPass = false; impWhy.add("the Squire swung on tick " + b.time + " with no blow pending (want one, landing on a later tick)=false");
            return null;
        }
        /** The impact frame of a blow's swing, from the sheet: its family's attackVariants row's impactTicks. */
        static int impactTicks(GravesAndGlory.Battle.PendingStrike s) {return Tactics.variants(s.attacker().spec.family()).get(s.variant()).impactTicks();}
        /** The hit log's newest entry (Combat.LAST) of hl-impact's Squire on its dummy logged after the entry `after`; null if none. */
        Combat.HitLog impHitLog(Combat.HitLog after) {
            for (var it = Combat.LAST.descendingIterator(); it.hasNext(); ) {
                var h = it.next(); if (h == after) return null;
                if (h.attacker() == impKnight.mob.getId() && h.target() == impTarget.mob.getId()) return h;
            }
            return null;
        }
        /** The blows of hl-impact's Squire still pending. */
        long impPending(GravesAndGlory.Battle b) {return b.strikes.stream().filter(x -> x.attacker() == impKnight).count();}
        /**
         * hl-impact's hurt-cooldown case (Task 5 fix round 1, M5): a generic hit of `pre` on the dummy 3 ticks before a blow's impact frame, the dummy's
         * vanilla hurt cooldown (timeUntilRegen) left as that hit set it. The blow is judged by vanilla's rule on its landing tick, as an instant blow on
         * that tick was at BASE: nothing if it is no larger than the hit, else only its excess (through the dummy's armour); the cooldown is not reset.
         */
        void impIframes(GravesAndGlory.Battle b, float pre, int then) {
            var m = impTarget.mob;
            if (impStrike == null) {
                var s = impSwung(b);
                if (s == null) {if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung for the " + fmt(pre) + " hurt-cooldown hit=false"); next(b, then);} return;}
                impStrike = s; impSwingAt = b.time; impLanded = Tactics.strikesLanded; impRegen = -1; impHit = pre; m.timeUntilRegen = 0;
            }
            if (impRegen < 0) {
                if (b.time < impStrike.landsAt() - 3) return;
                if (b.time >= impStrike.landsAt()) {impPass = false; impWhy.add("the blow landed before the hurt-cooldown hit could go in=false"); impStrike = null; next(b, then); return;}
                impLog = Combat.LAST.peekLast(); m.timeUntilRegen = 0; m.setHealth(m.getMaxHealth()); // a full dummy (its 4000 is clamped to 1024, vanilla's cap), so neither hit can kill it
                m.damage(b.world.getDamageSources().generic(), pre);
                impRegen = m.timeUntilRegen; impHealth = m.getHealth(); impNext = b.time;
                return;
            }
            if (b.time < impStrike.landsAt()) return;
            var hit = impHitLog(impLog); float h = m.getHealth(); int regen = m.timeUntilRegen; long gap = impStrike.landsAt() - impNext;
            float blow = hit == null ? 0 : (float) (hit.base() * hit.veteran() * hit.total());
            float armour = m.getArmor(), toughness = (float) m.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS);
            float full = net.minecraft.entity.DamageUtil.getDamageLeft(blow, armour, toughness);
            float want = blow <= pre ? 0 : net.minecraft.entity.DamageUtil.getDamageLeft(blow - pre, armour, toughness);
            boolean vanilla = hit != null && Math.abs((impHealth - h) - want) < 0.01f && Tactics.strikesLanded == impLanded + 1 && impRegen == 20 && regen == 20 - gap && regen > 10;
            impPass &= vanilla;
            impWhy.add("a " + fmt(pre) + " hit " + gap + " ticks before the Squire's blow of " + fmt(blow) + " (" + fmt(full) + " through armour " + fmt(armour) + " when fresh) landed on " + impStrike.landsAt() + ": the dummy lost " + fmt(impHealth - h) + " (want " + fmt(want) + ", vanilla's hurt-cooldown rule), its hurt cooldown " + impRegen + " after the hit and " + regen + " after the blow (want 20 and " + (20 - gap) + ", not reset), blows landed " + (Tactics.strikesLanded - impLanded) + " (want 1)=" + vanilla);
            impStrike = null; next(b, then);
        }
        /** The next hl-impact stage, and how the check stands so far once the last one is done. */
        void impFinish(GravesAndGlory.Battle b) {report("hl-impact", impPass, String.join("; ", impWhy)); next(b, 99);}

        /**
         * hl-impact (stages 80-97, Task 5 and its fix round 1): see the class comment. Every wait is bounded (200 ticks for a swing that never comes, 400 for the first three).
         */
        void impactStages(GravesAndGlory.Battle b) {
            var row = Campaign.troop("knight"); int cd = row.cooldown(), beat = (cd + 9) / 10 * 10; // Tactics acts on a 10-tick beat
            switch (stage) {
                case 80 -> {
                    clearFixture(b, "hl-impact"); crawlers.clear(); archers.clear(); Projectiles.clear(b); b.strikes.clear();
                    own.assignments.clear(); own.arrived.clear(); own.shotAt.clear(); dusk.assignments.clear(); dusk.shotAt.clear(); dusk.order = "attack";
                    impHome = ground(b, patch[0] + 4, patch[2]);
                    // the camera 24 blocks north of the pair, looking south at them: their chunks stay loaded, and the dummy stays the Squire's nearest enemy
                    var p = sp(b); b.relocate(p, impHome.x + 1, Phase1Checks.top(b, impHome.x + 1, impHome.z - 24) + 6, impHome.z - 24, 0f, 15f);
                    impTarget = dummy(b, 20, impHome);
                    impKnight = pinned(b, "knight", 21, ground(b, impHome.x + 1.8, impHome.z), false);
                    impHealth = impTarget.mob.getHealth(); impSeen = -1; impSwings = 0; impHurts = 0; impSwingAt = -1; impHurtAt = -1; impStrike = null; impLastVariant = -1;
                    next(b, 81);
                }
                case 81 -> { // three swings: each blow hurts the dummy exactly its swing's impactTicks after the swing tick, and no sooner
                    float h = impTarget.mob.getHealth();
                    if (h < impHealth) {
                        impHurts++;
                        boolean on = impStrike != null && impHurtAt < 0 && b.time - impSwingAt == impactTicks(impStrike); impPass &= on;
                        impWhy.add("swing " + impSwings + " (" + (impStrike == null ? "none" : Tactics.variants(row.family()).get(impStrike.variant()).id() + ", impactTicks " + impactTicks(impStrike)) + ") on tick " + impSwingAt + " hurt the dummy " + fmt(impHealth - h) + " on tick " + b.time + ", " + (b.time - impSwingAt) + " ticks after it=" + on);
                        impHurtAt = b.time; impHealth = h;
                    }
                    var s = impSwung(b);
                    if (s != null) {
                        if (impStrike != null && impHurtAt < 0) {impPass = false; impWhy.add("swing " + impSwings + " on tick " + impSwingAt + " never hurt the dummy before the next swing=false");}
                        boolean fresh = s.variant() != impLastVariant; impPass &= fresh;
                        if (!fresh) impWhy.add("swing " + (impSwings + 1) + " repeats swing " + impSwings + "'s variant " + s.variant() + "=false");
                        impLastVariant = s.variant(); impSwings++; impSwingAt = b.time; impStrike = s; impHurtAt = -1; impTarget.mob.timeUntilRegen = 0;
                    }
                    if (impSwings >= 3 && impHurtAt >= 0 || since(b) > 400) {
                        boolean all = impSwings >= 3 && impHurts == impSwings; impPass &= all;
                        impWhy.add(impSwings + " swings, " + impHurts + " blows on the dummy (want 3, one a swing)=" + all);
                        // the dummy goes 6 blocks off for 60 ticks: the Squire's cooldown goes stale, so its next swing starts it afresh (Tactics: time + cooldown)
                        impMove(ground(b, impHome.x - 6, impHome.z));
                        next(b, 82);
                    }
                }
                case 82 -> {
                    if (since(b) < 60) return;
                    impMove(impHome); impHealth = impTarget.mob.getHealth(); impMissed = Tactics.strikesMissed; impStrike = null;
                    next(b, 83);
                }
                case 83 -> { // the swing: on its tick the dummy is carried 6 blocks off (7.8 from the Squire, past its reach plus meleeReachGrace)
                    var s = impSwung(b);
                    if (s != null) {impSwingAt = b.time; impStrike = s; impNext = impKnight.nextAttack; impMove(ground(b, impHome.x - 6, impHome.z)); next(b, 84); return;}
                    if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung at the returned dummy=false"); next(b, 85);}
                }
                case 84 -> { // past the impact frame: nothing hurt the dummy, the blow missed and the cooldown stands; then, the dummy back in reach, the next swing
                    if (impSeen == impSwingAt) {
                        if (b.time <= impStrike.landsAt()) return;
                        float h = impTarget.mob.getHealth(); boolean none = h == impHealth && Tactics.strikesMissed == impMissed + 1 && impKnight.nextAttack == impNext; impPass &= none;
                        impWhy.add("the dummy carried 6 blocks off on the swing's tick (" + impSwingAt + "; its blow due on " + impStrike.landsAt() + ") lost " + fmt(impHealth - h) + " health, blows missed " + (Tactics.strikesMissed - impMissed) + " (want 0 and 1), the Squire's next attack at " + impKnight.nextAttack + " (set on the swing to " + impNext + ", want unchanged)=" + none);
                        impMove(impHome); impSeen = -2; // back in reach: watch for the next swing
                        return;
                    }
                    if (impKnight.swungAt > impSwingAt) {
                        long gap = impKnight.swungAt - impSwingAt; boolean after = gap == beat; impPass &= after;
                        impWhy.add("its next swing came on tick " + impKnight.swungAt + ", " + gap + " ticks after the missed one (want the cooldown " + cd + " rounded up to the 10-tick beat, " + beat + ")=" + after);
                        impSeen = impKnight.swungAt; impHealth = impTarget.mob.getHealth(); next(b, 85); return;
                    }
                    if (since(b) > 200) {impPass = false; impWhy.add("no swing after the missed one=false"); next(b, 85);}
                }
                case 85 -> { // a Squire killed on its swing's tick deals nothing
                    float h = impTarget.mob.getHealth();
                    if (impSeen == -3) {
                        if (b.time <= impStrike.landsAt() + 2) return;
                        boolean none = h == impHealth && !impKnight.mob.isAlive() && Tactics.strikesMissed == impMissed + 1 && b.strikes.stream().noneMatch(x -> x.attacker() == impKnight); impPass &= none;
                        impWhy.add("the Squire killed on its swing's tick (" + impSwingAt + "; its blow due on " + impStrike.landsAt() + "; alive after: " + impKnight.mob.isAlive() + ") took " + fmt(impHealth - h) + " health off the dummy, blows missed " + (Tactics.strikesMissed - impMissed) + " (want 0 health and 1 miss)=" + none);
                        unpin(b, List.of(impKnight, impTarget)); next(b, 86); return;
                    }
                    if (h < impHealth) impHealth = h; // the previous swing's blow, landing while this stage waits
                    var s = impSwung(b);
                    if (s != null) {impSwingAt = b.time; impStrike = s; impHealth = h; impMissed = Tactics.strikesMissed; impSeen = -3; impKnight.mob.kill(); return;}
                    if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung again=false"); unpin(b, List.of(impKnight, impTarget)); next(b, 86);}
                }
                case 86 -> { // kill credit: a fresh Squire and a dummy on 1 health
                    impTarget = dummy(b, 22, impHome); impTarget.mob.setHealth(1f);
                    impKnight = pinned(b, "knight", 23, ground(b, impHome.x + 1.8, impHome.z), false);
                    impBounty = LevelMath.bounty(impTarget.level, false, impTarget.temporary);
                    impCredits = impKnight.credits; impKills = dusk.stats.kills; impLost = own.stats.lost; impLanded = Tactics.strikesLanded; impSeen = -1; impStrike = null; impHurtAt = -1;
                    next(b, 87);
                }
                case 87 -> { // the blow that kills it on the impact frame credits the Squire the bounty once, and his army one kill
                    if (impStrike == null) {
                        if (!impTarget.mob.isAlive() && impHurtAt < 0) impHurtAt = b.time;
                        var s = impSwung(b);
                        if (s != null) {impStrike = s; impSwingAt = b.time;}
                        else if (since(b) > 200) {impPass = false; impWhy.add("the fresh Squire never swung at the 1-health dummy=false"); unpin(b, List.of(impTarget)); impTarget = dummy(b, 24, impHome); next(b, 88);}
                        return;
                    }
                    if (!impTarget.mob.isAlive() && impHurtAt < 0) impHurtAt = b.time;
                    if (b.time < impStrike.landsAt() + 10) return;
                    boolean on = Veterans.enabled();
                    boolean once = impHurtAt == impStrike.landsAt() && Tactics.strikesLanded - impLanded == 1 && impKnight.credits - impCredits == impBounty && dusk.stats.kills - impKills == 1 && own.stats.lost - impLost == 1;
                    impPass &= once && on;
                    impWhy.add("a 1-health dummy died on tick " + impHurtAt + " (the blow swung on " + impSwingAt + ", due on " + impStrike.landsAt() + "): the Squire's kill credits went up " + (impKnight.credits - impCredits) + " (want the bounty " + impBounty + " once), his army's kills " + (dusk.stats.kills - impKills) + " and the dummy's army's losses " + (own.stats.lost - impLost) + " (want 1 each), blows landed " + (Tactics.strikesLanded - impLanded) + " (want 1); progression on: " + on + "=" + (once && on));
                    // a victim killed by something else (a generic kill, no attacker) on the swing's tick: the blow misses and the kill is nobody's
                    unpin(b, List.of(impTarget));
                    impTarget = dummy(b, 24, impHome);
                    impCredits = impKnight.credits; impKills = dusk.stats.kills; impLost = own.stats.lost; impMissed = Tactics.strikesMissed; impLanded = Tactics.strikesLanded; impStrike = null;
                    next(b, 88);
                }
                case 88 -> { // a victim that dies to something else between swing and impact is nobody's kill
                    if (impStrike == null) {
                        var s = impSwung(b);
                        if (s != null) {impStrike = s; impSwingAt = b.time; impTarget.mob.kill();}
                        else if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung at the second victim=false"); next(b, 89);}
                        return;
                    }
                    if (b.time < impStrike.landsAt() + 10) return;
                    boolean nobody = !impTarget.mob.isAlive() && Tactics.strikesMissed - impMissed == 1 && Tactics.strikesLanded == impLanded && impKnight.credits == impCredits && dusk.stats.kills == impKills && own.stats.lost - impLost == 1;
                    impPass &= nobody;
                    impWhy.add("a dummy killed by a generic kill on the Squire's swing tick (" + impSwingAt + "; its blow due on " + impStrike.landsAt() + "): blows missed " + (Tactics.strikesMissed - impMissed) + " and landed " + (Tactics.strikesLanded - impLanded) + " (want 1 and 0), the Squire's credits up " + (impKnight.credits - impCredits) + " and his army's kills up " + (dusk.stats.kills - impKills) + " (want 0 each), the dummy's army's losses up " + (own.stats.lost - impLost) + " (want 1)=" + nobody);
                    unpin(b, List.of(impTarget));
                    impTarget = dummy(b, 25, impHome); impStrike = null;
                    next(b, 90);
                }
                case 89 -> { // the round ends on a swing's tick with its blow pending: the lobby clears it, and none lands in the next round
                    if (impStrike == null) {
                        var s = impSwung(b);
                        if (s != null) {
                            impStrike = s; impSwingAt = b.time;
                            int before = b.strikes.size(); b.campaign.returnLobby(); int left = b.strikes.size();
                            impLanded = Tactics.strikesLanded; impMissed = Tactics.strikesMissed;
                            boolean cleared = before >= 1 && left == 0; impPass &= cleared;
                            impWhy.add("the round ended on the Squire's swing tick (" + impSwingAt + ") with " + before + " blow(s) pending (its own due on " + impStrike.landsAt() + "): " + left + " left (want 0)=" + cleared);
                            fixture.clear(); pins.clear(); impKnight = null; impTarget = null; impNext = -1; // returnLobby took every troop
                        } else if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung at the round-end dummy=false"); impFinish(b);}
                        return;
                    }
                    if (!b.running) {var p = sp(b); if (p != null) Phase1Checks.skirmish(b, p, 0, 1, "normal"); return;}
                    if (impNext < 0) impNext = b.time; // the next round's first tick of play
                    if (b.time < Math.max(b.campaign.introUntil, impStrike.landsAt()) + 10) return;
                    boolean none = Tactics.strikesLanded == impLanded && Tactics.strikesMissed == impMissed;
                    impPass &= none;
                    impWhy.add("the next round ran from tick " + impNext + " (play from " + b.campaign.introUntil + ") to " + b.time + ", past the old blow's frame " + impStrike.landsAt() + ": blows landed " + (Tactics.strikesLanded - impLanded) + " and missed " + (Tactics.strikesMissed - impMissed) + " (want 0 each)=" + none);
                    impStrike = null; next(b, 96);
                }
                case 90 -> { // Ruling R29: a swing on tick 59 of a fresh Charge's 60-tick window, the order turned to Hold in its wind-up, still crashes in (C5) when its blow lands past the window
                    String g = impKnight.spec.group(); int window = Spec.COMBATDISCIPLINE[0].chargeImpactTicks();
                    if (impStrike == null) {
                        var s = impSwung(b);
                        if (s != null) {
                            impStrike = s; impSwingAt = b.time; Long at = dusk.chargedAt.get(g); impCharged = at == null ? Long.MIN_VALUE / 2 : at;
                            dusk.order = "hold"; // the order changes during the wind-up: the blow keeps the Charge its swing was judged under
                            impLanded = Tactics.strikesLanded; impHealth = impTarget.mob.getHealth(); impLog = Combat.LAST.peekLast();
                            return;
                        }
                        if (since(b) > 200) {impPass = false; impWhy.add("the charging Squire never swung=false"); dusk.order = "attack"; dusk.chargedAt.clear(); next(b, 91); return;}
                        // a fresh Charge stamped so that a swing on the next tick falls on its window's tick 59
                        dusk.assignments.clear(); dusk.order = "charge"; dusk.chargedAt.put(g, b.time + 1 - (window - 1)); impTarget.mob.timeUntilRegen = 0;
                        return;
                    }
                    if (b.time < impStrike.landsAt()) return;
                    var hit = impHitLog(impLog); float h = impTarget.mob.getHealth();
                    boolean c5 = hit != null && hit.rows().contains("C5");
                    boolean on = impSwingAt - impCharged == window - 1 && impStrike.landsAt() - impCharged > window && c5 && impKnight.chargeHitAt == impStrike.landsAt() && Tactics.strikesLanded == impLanded + 1 && h < impHealth;
                    impPass &= on;
                    impWhy.add("a swing on tick " + (impSwingAt - impCharged) + " of a fresh Charge's " + window + "-tick window (the order turned to Hold in its wind-up) landed on tick " + (impStrike.landsAt() - impCharged) + " of it, past the window: its hit's rows " + (hit == null ? "none (no hit logged)" : hit.rows()) + " (want C5, the impact), the Squire's last impact on " + impKnight.chargeHitAt + " (want the landing tick " + impStrike.landsAt() + "), blows landed " + (Tactics.strikesLanded - impLanded) + " (want 1), the dummy lost " + fmt(impHealth - h) + "=" + on);
                    dusk.order = "attack"; dusk.chargedAt.clear(); impStrike = null;
                    next(b, 91);
                }
                case 91 -> { // M1: a second swing tried while a blow is pending queues nothing
                    var s = impSwung(b);
                    if (s != null) {
                        long mine = impPending(b); int swings = Tactics.swings, variant = impKnight.lastVariant; long swung = impKnight.swungAt;
                        Tactics.swing(b, impKnight, impTarget.mob, b.time);
                        long after = impPending(b);
                        boolean refused = mine == 1 && after == 1 && Tactics.swings == swings && impKnight.lastVariant == variant && impKnight.swungAt == swung && b.strikes.contains(s);
                        impPass &= refused;
                        impWhy.add("a second Tactics.swing on the Squire's swing tick (" + b.time + ", " + mine + " blow pending, due on " + s.landsAt() + ") left " + after + " pending (want 1, the first still queued: " + b.strikes.contains(s) + "), swings begun up " + (Tactics.swings - swings) + " (want 0), his variant " + variant + " now " + impKnight.lastVariant + "=" + refused);
                        impStrike = null; next(b, 92); return;
                    }
                    if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung for the second-swing check=false"); next(b, 92);}
                }
                case 92 -> { // M6: a Squire stunned a tick after his swing deals nothing: the blow misses on its frame
                    if (impStrike == null) {
                        var s = impSwung(b);
                        if (s != null) {impStrike = s; impSwingAt = b.time; impHealth = impTarget.mob.getHealth(); impMissed = Tactics.strikesMissed; impLanded = Tactics.strikesLanded; impTarget.mob.timeUntilRegen = 0; return;}
                        if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung for the stun check=false"); next(b, 93);}
                        return;
                    }
                    if (b.time == impSwingAt + 1) {impKnight.stunnedUntil = b.time + 20; return;}
                    if (b.time <= impStrike.landsAt()) return;
                    float h = impTarget.mob.getHealth();
                    boolean none = h == impHealth && Tactics.strikesMissed == impMissed + 1 && Tactics.strikesLanded == impLanded;
                    impPass &= none;
                    impWhy.add("the Squire stunned on tick " + (impSwingAt + 1) + ", a tick after his swing (its blow due on " + impStrike.landsAt() + "): the dummy lost " + fmt(impHealth - h) + ", blows missed " + (Tactics.strikesMissed - impMissed) + " and landed " + (Tactics.strikesLanded - impLanded) + " (want 0, 1 and 0)=" + none);
                    impStrike = null; next(b, 93);
                }
                case 93 -> impIframes(b, 20f, 94); // M5: a larger hit 3 ticks before the impact frame: the blow does nothing, as vanilla rules
                case 94 -> impIframes(b, 1f, 95); // M5: a smaller hit then: the blow deals only its excess, through the dummy's armour
                case 95 -> { // M8: Abilities.attack on a melee troop deals nothing and queues nothing (Tactics.swing owns its blow)
                    if (impPending(b) > 0) {if (since(b) > 200) {impPass = false; impWhy.add("the Squire's blow never cleared for the misroute check=false"); next(b, 89);} return;}
                    var m = impTarget.mob; m.timeUntilRegen = 0; m.hurtTime = 0; m.setHealth(m.getMaxHealth()); boolean alive = m.isAlive(); // a live, full dummy: on a dead one any call would deal nothing
                    float h = m.getHealth(); int queued = b.strikes.size(), swings = Tactics.swings; var log = Combat.LAST.peekLast();
                    Abilities.attack(b, impKnight, m, b.time);
                    float h2 = m.getHealth(); var hit = impHitLog(log);
                    boolean nothing = alive && m.isAlive() && h2 == h && b.strikes.size() == queued && Tactics.swings == swings && hit == null;
                    impPass &= nothing;
                    impWhy.add("Abilities.attack called for the Squire (a melee troop) on tick " + b.time + ": the dummy lost " + fmt(h - h2) + ", blows queued " + (b.strikes.size() - queued) + ", swings begun " + (Tactics.swings - swings) + ", hits logged " + (hit == null ? 0 : 1) + " (want 0 each; the dummy alive before: " + alive + ")=" + nothing);
                    impStrike = null; next(b, 89);
                }
                case 96 -> { // M6: after the next round has begun, a map switch with a blow pending clears it, and none lands once the battle runs again
                    if (impKnight == null) {
                        if (!b.ready) return;
                        var p = sp(b); if (p != null) b.relocate(p, impHome.x + 1, Phase1Checks.top(b, impHome.x + 1, impHome.z - 24) + 6, impHome.z - 24, 0f, 15f);
                        impTarget = dummy(b, 26, impHome);
                        impKnight = pinned(b, "knight", 27, ground(b, impHome.x + 1.8, impHome.z), false);
                        impSeen = -1; impStrike = null; impNext = -1;
                        return;
                    }
                    if (impStrike == null) {
                        var s = impSwung(b);
                        if (s != null) {
                            impStrike = s; impSwingAt = b.time;
                            int from = b.campaign.map, to = from == 1 ? 2 : 1;
                            int before = b.strikes.size(); b.campaign.switchMap(to); int left = b.strikes.size();
                            impLanded = Tactics.strikesLanded; impMissed = Tactics.strikesMissed;
                            boolean cleared = before >= 1 && left == 0; impPass &= cleared;
                            impWhy.add("the map switched (" + from + " to " + to + ") on the Squire's swing tick (" + impSwingAt + ") with " + before + " blow(s) pending (his due on " + impStrike.landsAt() + "): " + left + " left (want 0)=" + cleared);
                            unpin(b, List.of(impKnight, impTarget)); // were a blow left behind, it would now count as a miss (its Squire gone)
                            return;
                        }
                        if (since(b) > 200) {impPass = false; impWhy.add("the Squire never swung for the map-switch check=false"); unpin(b, List.of(impKnight, impTarget)); impKnight = null; impTarget = null; next(b, 97);}
                        return;
                    }
                    if (!b.ready) {if (since(b) > 6000) {impPass = false; impWhy.add("the switched-to map never finished building=false"); impKnight = null; impTarget = null; next(b, 97);} return;}
                    if (impNext < 0) impNext = b.time; // the first tick the battle runs again
                    if (b.time < Math.max(impNext, impStrike.landsAt()) + 10) return;
                    boolean none = Tactics.strikesLanded == impLanded && Tactics.strikesMissed == impMissed && !b.strikes.contains(impStrike);
                    impPass &= none;
                    impWhy.add("the battle ran again from tick " + impNext + " to " + b.time + ", past the old blow's frame " + impStrike.landsAt() + ": blows landed " + (Tactics.strikesLanded - impLanded) + " and missed " + (Tactics.strikesMissed - impMissed) + " (want 0 each), the old blow queued: " + b.strikes.contains(impStrike) + "=" + none);
                    fixture.clear(); pins.clear(); impKnight = null; impTarget = null; impStrike = null;
                    next(b, 97);
                }
                case 97 -> { // back to a round on map 0
                    var p = sp(b);
                    if (p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) {impFinish(b); return;}
                    if (since(b) > 8000) {impPass = false; impWhy.add("the round never restarted on map 0=false"); impFinish(b);}
                }
                default -> {}
            }
        }
        /** The hl-arc dummy stands here from now on (its pin moves with it). */
        void walk(Vec3d at) {
            if (arcTarget == null) return;
            pins.removeIf(p -> p.u() == arcTarget); pins.add(new Pin(arcTarget, at, -90f)); Phase2Checks.Engine.apply(arcTarget, at, -90f);
        }
        /** The angle, in degrees, between an arrow's nose (yaw and pitch as BattleMath.arrowYaw and arrowPitch give them) and the direction (x, y, z). */
        static double noseOff(double yaw, double pitch, double x, double y, double z) {
            double len = Math.sqrt(x * x + y * y + z * z); if (len == 0) return 0;
            double ry = Math.toRadians(yaw), rp = Math.toRadians(pitch);
            double dot = (Math.sin(ry) * Math.cos(rp) * x + Math.sin(rp) * y + Math.cos(ry) * Math.cos(rp) * z) / len;
            return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
        }
        /** The angle, in degrees, between two arrow attitudes. */
        static double attitudeOff(double yaw, double pitch, double yaw2, double pitch2) {
            double ry = Math.toRadians(yaw2), rp = Math.toRadians(pitch2);
            return noseOff(yaw, pitch, Math.sin(ry) * Math.cos(rp), Math.sin(rp), Math.cos(ry) * Math.cos(rp));
        }
        /**
         * One hl-arc shot has left the queue: its peak over the straight line, its landing place and tick, its place against the curve each tick and
         * its attitude on the server, against the sheet's arc. The client's view of its attitude is added in stage 74.
         */
        void arcLanded(GravesAndGlory.Battle b, Spec.CombatDisciplineRow cd, ArcCase ac) {
            String id = ac.id(), kind = ac.kind(); boolean bolt = arcShot.kind().equals("bolt"), moving = kind.equals("moving");
            Vec3d from = arcShot.from(), to = arcAim;
            double dist = from.distanceTo(arcFireAim);
            int travel = BattleMath.travelTicks(dist, bolt ? cd.boltSpeed() : cd.arrowSpeed(), cd.projectileMinTicks(), cd.projectileMaxTicks());
            // the arc each weapon should take (animation rework section 5, R24): a bow the whole of it, a crossbow (the Arbalist's and the Ballista's) and a thrown trident their sheet scales, the Mortar's and a caster's bolt none
            double scale = bolt ? 0 : switch (id) {case "archer" -> 1; case "ic_arbalist", "ic_ballista" -> cd.crossbowArcScale(); case "tc_harpooner" -> cd.thrownArcScale(); default -> 0;};
            double wantPeak = BattleMath.arcPeak(dist, cd.volleyPeakPerBlock(), cd.volleyPeakMax(), scale);
            int wantTicks = BattleMath.arcTravelTicks(travel, wantPeak, cd.arcTravelScale());
            long age = b.time - arcShot.fired(), queued = arcShot.lands() - arcShot.fired();
            double peak = arcPath.isEmpty() ? 0 : -1e9; int peakAt = 0; List<String> heights = new ArrayList<>();
            for (int i = 0; i < arcPath.size(); i++) {double h = overLine(from, to, arcPath.get(i)); heights.add(fmt(h)); if (h > peak) {peak = h; peakAt = i + 1;}}
            Vec3d end = arcPath.isEmpty() ? null : arcPath.get(arcPath.size() - 1); arcEnd = end;
            double miss = end == null ? 1e9 : end.distanceTo(to);
            float hurt = arcHealth - arcTarget.mob.getHealth();
            boolean timed = age == wantTicks && queued == wantTicks && (cd.arcTravelScale() != 1 || wantTicks == travel);
            boolean landed = hurt > 0 && timed && (bolt ? arcShot.visual() == null && arcPath.isEmpty() : miss <= 0.3 && arcPath.size() == age);
            // R26: with the dummy still, every tick's place is on the curve from + (to - from) k/T, lifted by arcHeight(k/T, the shot's peak)
            double offCurve = 0, n = Math.max(1, arcShot.ticks());
            if (!moving) for (int k = 1; k <= arcPath.size(); k++) offCurve = Math.max(offCurve, arcPath.get(k - 1).distanceTo(from.lerp(to, k / n).add(0, BattleMath.arcHeight(k / n, arcShot.peak()), 0)));
            boolean onCurve = moving || offCurve <= 0.05;
            // R25 and N1: every tick, the arrow on the server points along its move halfway through the tick (the renderer eases from the prev attitude to
            // the current one over it), and on a long arc it turns smoothly: one tick's end attitude is the next one's start
            double turnOff = 0, turnJump = 0;
            for (int k = 0; k < arcPath.size(); k++) {
                Vec3d d = arcPath.get(k).subtract(k == 0 ? from : arcPath.get(k - 1)); float[] a = arcTurn.get(k);
                if (d.length() > 0.01) turnOff = Math.max(turnOff, noseOff((a[2] + a[0]) / 2, (a[3] + a[1]) / 2, d.x, d.y, d.z));
                if (k > 0) turnJump = Math.max(turnJump, attitudeOff(arcTurn.get(k - 1)[0], arcTurn.get(k - 1)[1], a[2], a[3]));
            }
            boolean longArc = arcShot.peak() > 0.5 && wantTicks >= 6;
            boolean nosed = bolt || turnOff <= 1 && (!longArc || turnJump <= 2);
            int inBlock = 0; for (var q : arcPath) if (solid(b, (int) Math.floor(q.x), (int) Math.floor(q.y), (int) Math.floor(q.z))) inBlock++;
            String shape; boolean shaped;
            if (bolt) {shaped = arcShot.peak() == 0 && arcShot.visual() == null; shape = "a particle bolt, no arrow, peak " + fmt(arcShot.peak()) + " (want none)";}
            else if (kind.equals("short")) {double tol = Math.max(0.1 * wantPeak, 0.15); shaped = wantTicks <= 3 && Math.abs(peak - wantPeak) <= tol; shape = "peak " + fmt(peak) + " at tick " + peakAt + " of " + wantTicks + " (want the sheet's " + fmt(wantPeak) + " +/- " + fmt(tol) + ", in 3 ticks at most)";}
            else if (kind.equals("roof")) { // N2: an arc, capped by the roof: the roof's underside over the line's middle, less the margin (the shape there is 1)
                double cap = arcRoofY - from.lerp(arcFireAim, .5).y - Projectiles.COVER_MARGIN;
                shaped = Math.abs(arcShot.peak() - cap) <= 0.02 && peak > 0 && peak <= cap + 0.02 && peak <= 2.5 && inBlock == 0;
                shape = "under the roof, peak " + fmt(peak) + " at tick " + peakAt + ", the shot's own " + fmt(arcShot.peak()) + " of the open sky's " + fmt(wantPeak) + " (want the roof's cap " + fmt(cap) + " +/- 0.02, over 0 and 2.5 at most), " + inBlock + " ticks inside a block (want none)";
            }
            else if (moving) {double walked = Math.sqrt(GravesAndGlory.Battle.horizontalDistance(arcFireAim, to)); shaped = walked >= ARC_WALK * (wantTicks - 2) && miss <= 0.05; shape = "at a dummy that walked " + fmt(walked) + " sideways in its flight (want " + fmt(ARC_WALK * (wantTicks - 2)) + " or more), peak " + fmt(peak) + ", the arrow led it onto its landing place (N6: want 0.05 at most of the miss)";}
            else if (id.equals("archer")) {bowPeak = peak; shaped = Math.abs(peak - 5.5) <= 0.15; shape = "peak " + fmt(peak) + " over the straight line at tick " + peakAt + " (want 5.5 +/- 0.15; the sheet's " + fmt(wantPeak) + ")";}
            else if (scale > 0) {double ratio = peak / bowPeak, tol = id.equals("ic_ballista") ? 0.05 : 0.02; shaped = bowPeak > 0.5 && Math.abs(ratio - scale) <= tol; shape = "peak " + fmt(peak) + " at tick " + peakAt + ", " + String.format(Locale.ROOT, "%.3f", ratio) + " of the bow's (want " + fmt(scale) + " +/- " + fmt(tol) + ")";}
            else {shaped = Math.abs(peak) <= 0.05 && arcShot.peak() == 0; shape = "peak " + fmt(peak) + " (want a straight flight, within 0.05 of the line)";}
            if (!bolt) shape += ", heights by tick " + heights;
            arcPendingPass = shaped && landed && onCurve && nosed; arcPendingArrow = !bolt; arcPendingArced = !bolt && arcShot.peak() > 0.5; arcPendingTicks = wantTicks;
            arcPendingWhy = id + (kind.isEmpty() ? "" : " " + kind) + " (" + arcShot.kind() + ", " + fmt(dist) + " blocks): " + shape + "; it left the queue " + age + " ticks after it was fired, queued for " + queued
                + " (want travelTicks " + travel + " x arcTravelScale " + fmt(cd.arcTravelScale()) + " = " + wantTicks + ")" + (bolt ? "" : ", its arrow " + fmt(miss) + " from the dummy's body then (want 0.3 at most)") + ", the dummy hurt " + fmt(hurt)
                + (bolt ? "" : (moving ? "" : "; every tick at most " + fmt(offCurve) + " off the curve (want 0.05)") + "; on the server the arrow's nose halfway through each tick at most " + fmt(turnOff) + " degrees off its move (want 1)"
                + ", " + fmt(turnJump) + " from one tick's end to the next one's start" + (longArc ? " (want 2 at most)" : ""));
        }
        /**
         * hl-house's wall: a block of the outer wall of a supply house west of the river (the first found from outside, along each side of the footprint, away from the gates),
         * exactly 2 high over the level ground outside it (the yard wall, not a building's), with the cell outside and three cells inside open (air at feet and head, and nothing
         * overhead inside) on that same level, so the slot inside is in the open yard.
         * Returns {house index, wall x, wall z, outward dx, outward dz, standing y inside, standing y outside}, or null.
         */
        double[] houseWall(GravesAndGlory.Battle b) {
            var houses = Tactics.housesOfMap();
            int[][] dirs = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
            for (int h = 0; h < houses.length; h++) {
                var r = houses[h]; if (r.x() >= 0) continue;
                for (int[] d : dirs) for (int lateral = -8; lateral <= 8; lateral++) {
                    int reach = d[0] != 0 ? 20 : 22;
                    for (int k = reach; k >= 10; k--) {
                        int x = r.x() + d[0] * k + (d[0] == 0 ? lateral : 0), z = r.z() + d[1] * k + (d[1] == 0 ? lateral : 0);
                        int floor = Phase1Checks.top(b, x + d[0] + .5, z + d[1] + .5) == 0 ? -1 : (int) Phase1Checks.top(b, x + d[0] + .5, z + d[1] + .5);
                        if (!solid(b, x, floor, z) || !solid(b, x, floor + 1, z)) continue; // not the wall (2 high over the ground outside)
                        if (solid(b, x, floor + 2, z)) break; // a building's wall (a barn or a hall), not the yard wall: the slot would be under a roof
                        boolean ok = openAt(b, x + d[0], floor, z + d[1]);
                        for (int i = 1; i <= 3 && ok; i++) ok = openAt(b, x - d[0] * i, floor, z - d[1] * i) && !solid(b, x - d[0] * i, floor + 2, z - d[1] * i); // the yard, open overhead (no lean-to roof)
                        if (ok) {
                            double sx = x + .5 - d[0] * .85, sz = z + .5 - d[1] * .85, ox = x + .5 + d[0] * .9, oz = z + .5 + d[1] * .9;
                            if (HouseRoute.inside(r.x(), r.z(), sx, sz, 0.5) && HouseRoute.next(r.x(), r.z(), r.rotation(), ox, oz, sx, sz, 0) != null) return new double[]{h, x, z, d[0], d[1], floor, floor};
                        }
                        break; // the first wall from outside on this line decides it
                    }
                }
            }
            return null;
        }
        /** The block at (x, y, z) has a collision shape. */
        static boolean solid(GravesAndGlory.Battle b, int x, int y, int z) {var at = new BlockPos(x, y, z); return !b.world.getBlockState(at).getCollisionShape(b.world, at).isEmpty();}
        /** A troop could stand in the cell (x, z) at y: solid below, empty at its feet and head. */
        static boolean openAt(GravesAndGlory.Battle b, int x, int y, int z) {return solid(b, x, y - 1, z) && !solid(b, x, y, z) && !solid(b, x, y + 1, z);}
    }
}
