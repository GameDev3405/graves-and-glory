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
 * modifier within fireMemoryTicks + 20. On a crawling Squire (tick 80 under fire) Muster's hasten multiplies with the crawl on the attribute
 * (crawlMuster, Task 8), and its end leaves the crawl alone. With the archers back, on Hold, the knights walking 27 blocks back to their slots under fire with
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
 * switch with a blow pending clears it and none lands once the battle runs again; the round then restarts on map 0. Task 5 fix round 2: Hold is
 * judged with the Charge on the swing tick, so an order turned during the wind-up neither grants nor costs a braced blow's row (C18c, C18c2, C18d)
 * on a charging dummy, both ways for each; and the restarted round's variant rolls start afresh from the world's seed and the round's number.</li>
 * <li>hl-events (Task 6): the motion events as the client holds them (MotionEvents.Client), on the same patch in the restarted round. A Dusk Order
 * Squire (AI on, pinned, on Attack) swings at a frozen dummy of the commander's army: each swing reaches the client as attack_v1 with the variant
 * Battle picked (its pending blow's) and the world tick of the swing, and each blow as hit_v1 with its swing's kind, zone and weight, the signed
 * bearing to the Squire, unblocked, not lethal, on its landing tick; a blow that kills a dummy on 1 health arrives lethal. An arrow on a Hearthguard
 * holding its shield up arrives blocked, one on an Ironclad Breaker unblocked, each with the zone of the height it struck at; a fall on the Breaker
 * arrives blunt to the legs and magic damage as magic. Plain-hold rule: on Hold the Hearthguard (shield) and a Tusk (spear) show brace (wall when
 * formed), the Breaker and a Reaper hold, on the server and the client. Late join, emulated twice: each troop leaves the player's tracking and is
 * tracked again (START_TRACKING), then the client's tables are emptied and the channel registration replayed (S2CPlayChannelEvents.REGISTER); both
 * times every tracked troop's stance reaches the client within 2 ticks, the Squire's newest swing with it, and a troop with no stance shows none.</li>
 * <li>hl-corpse (Task 7): on the same patch, a pinned Dusk Order Squire (AI on, on Attack) strikes down a tough frozen Squire of the commander's army
 * (a second one stands 6 blocks west as the Dusk Squire's other foe). The body lies: in the world and dead on its last death tick before its fade
 * (20 + corpseLieTicks - 1) and on the tick before its removal, removed on BattleMath.corpseGoneTick of the corpse rows (20 + lie + fade), and the
 * client holds it lying past the first of those; nothing (no item,
 * no experience orb) lies within 4 blocks of it, AFTER_DEATH fired for it once; from the tick after its death no unit's lockedTarget nor mob target
 * is the body, it cannot be hit (canHit) nor pushed (isPushable), it is out of Battle.units, out of its side team, and the army's fielded count is one
 * less; an own Squire set 0.15 from the body (AI on, out of the battle's orders) moves under 0.02 over 20 ticks, and the body under 0.02 too. Rise
 * Again still works: a Revenant of a Graveborn Dusk army (six fielded, Houses.recompute), the roll forced, rises (alive, rose, death ticks 0, no
 * AFTER_DEATH); struck down again it lies as a body. Last, after hl-corpse-load, a Squire's body 30 ticks dead goes the moment the round ends
 * (Campaign.returnLobby, Battle.clearCorpses) and no dead troop is left in the world.</li>
 * <li>hl-corpse-load (Task 7): on dry ground only (the patch has no water and no fire, so every body settles; bodies in water or burning keep
 * ticking and are not measured). 100 Order Squires of the commander's army and 100 of Dusk's (AI on, 4000 health) spawn in two 10 by 10 blocks 20
 * apart on the patch, both armies on Attack toward the middle; as they meet, one falls each tick, the armies in turn, so all 200 die within 10 s.
 * Over the 340 ticks from the first death the mean server tick (MinecraftServer.lastTickLengths, the tick before each sample) and the client's
 * mean frame time (wall clock over the frames drawn; the frame rate is the run's options.txt: vsync off and maxFps 260 for the Task 8
 * measurement, logged in the report) are measured with the corpse rows at 0 and 0 (A: vanilla's
 * 20-tick fall) and at 60 and 40 (B), in the order warm-up B (not counted), A, B, B, A: B's mean is within +10% of A's on the server and +15% on
 * the client, every troop died, and in B 100 or more bodies lay at once. The world render alone (Phase2Checks.RenderCost) and every run's raw
 * figures are logged.</li>
 * </ul>
 */
final class HoldLineChecks {
    private HoldLineChecks() {}

    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) { suites.put("holdline", () -> new Wall(false)); suites.put("holdline-corpse", () -> new Wall(true)); }

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
        /** Fix round 2: the brace case under way (0-5: each of BRACE_TAGS swung under Hold then turned to Charge, then the reverse), and the orders and target it put aside. */
        int impBrace; String impOwnOrder, impDuskOrder; Vec3d impDuskTarget;
        static final String[] BRACE_TAGS = {"brace", "brace2", "bracewall"}, BRACE_ROWS = {"C18c", "C18c2", "C18d"};
        boolean impPass = true;
        final List<String> impWhy = new ArrayList<>();
        // hl-events (Task 6)
        /** hl-events' Squire, its two dummies (the second on 1 health), the holders on Hold (shield, spear and two others), the frozen Dusk archer, the Dusk shield guard the player strikes. */
        GravesAndGlory.Unit evKnight, evTarget, evLethal, evHg, evTusk, evBreaker, evReaper, evArcher, evGuard;
        Vec3d evHome;
        boolean evPass = true;
        final List<String> evWhy = new ArrayList<>();
        /** What the server sent, by entity id, oldest first (a shot's weight -1: not judged). */
        final Map<Integer, List<MotionInput.Attack>> evAttacks = new HashMap<>();
        final Map<Integer, List<MotionInput.Hit>> evHits = new HashMap<>();
        /** The client's tables for the watched ids, copied after each client tick while evSnap is on (the newest non-empty copy kept). */
        final Set<Integer> evWatch = java.util.concurrent.ConcurrentHashMap.newKeySet();
        // hl-corpse and hl-corpse-load (Task 7)
        /** Only hl-corpse and hl-corpse-load after the round and its patch (GRAVES_PHASE2_CHECKS=holdline-corpse). */
        final boolean corpseOnly;
        Wall(boolean corpseOnly) {this.corpseOnly = corpseOnly;}
        /** The Squire struck down, its foe the Dusk Squire, the other own Squire, the one set against the body, the Revenant that rises. */
        GravesAndGlory.Unit cpVictim, cpHunter, cpBait, cpLeaner, cpRevenant;
        Vec3d cpHome, cpBody0, cpBody10, cpLean0;
        boolean cpPass = true;
        final List<String> cpWhy = new ArrayList<>();
        /** AFTER_DEATH calls by entity id (one listener, registered once). */
        static final Map<Integer, Integer> CP_DEATHS = new java.util.concurrent.ConcurrentHashMap<>();
        static boolean cpHooked;
        /** The body's id the client watches, and the most death ticks the client saw it lying. */
        volatile int cpClientId = -1, cpClientDeath = -1;
        /** hl-corpse (Task 7 fix 1, I4): the overlay V the client's renderer gives the body while it falls and once it lies (-1 unseen; 3 red, 10 none). */
        volatile int cpTintFall = -1, cpTintLie = -1;
        /** hl-corpse (Task 7 fix 2): the client entities of the marked Squire (watched from before its blow) and of the living Squire beside it, and what the client saw of them. */
        volatile int cpGlowId = -1, cpBaitId = -1;
        volatile boolean cpClientGlowBefore, cpClientGlowFall, cpClientHitLying, cpClientLyingSeen, cpClientBaitHit;
        /** hl-corpse (Task 8 m4): the burning Revenant's body as the client holds it, and whether the client drew it on fire as it fell (death ticks 1-15) and once its fire should be out (death ticks 35 on): the synced on-fire flag the renderer draws flames from. */
        volatile int cpFireId = -1;
        volatile boolean cpClientFireFall, cpClientFireLate;
        /** N1: the horizontal speed the killing blow left the body, and the blow a Revenant rose from (Rise Again); -1 unmeasured. */
        double cpKnock = -1, cpRiseKnock = -1;
        /** N7: the body Grave Bloom raises a Risen from, and where it lay. */
        GravesAndGlory.Unit cpBloomBody; Vec3d cpBloomAt;
        /** hl-corpse (Task 8 fix 1, R39): the settled body the client shoots drawn from its recorded buffers, drawn the normal way and not drawn; and what it found. */
        GravesAndGlory.Unit cpCacheBody;
        volatile int cpShotId = -1;
        volatile boolean cpShotDone, cpShotPass;
        volatile String cpShotWhy = "";
        int cpShotPhase, cpShotWait, cpShotAsked = -1, cpShotAt = -1; long cpShotDraws; boolean cpShotEngaged, cpShotNormal, cpShotPaused;
        net.minecraft.client.texture.NativeImage cpShotA, cpShotB, cpShotB2;
        /** R44: the player's place before the camera was nudged (x, y, z, then prev and last render x, y, z). */
        final double[] cpNudged = new double[9];
        /** hl-corpse (Task 8 fix 2, R42): the body the client samples as it fades (at the fade's first tick, its middle and its last), and what it found. */
        volatile int cpFadeId = -1;
        volatile boolean cpFadeDone, cpFadePass;
        volatile String cpFadeWhy = "";
        int cpFadeK, cpFadePhase, cpFadeWait; long cpFadeDraws; boolean cpFadeCached, cpFadePaused;
        net.minecraft.client.texture.NativeImage cpFadeF, cpFadeN, cpFadeO;
        /** Each sample: {alpha as drawn from its buffers, alpha drawn the normal way, the alpha its death ticks and the frame's tick delta give, death ticks, O's difference from H}. */
        final double[][] cpFadeV = new double[3][5];
        boolean cpAt79, cpAt119, cpHitless, cpOutOfUnits, cpNoSide;
        long cpFielded0, cpFielded1, cpGoneAt = -1;
        int cpDeath79 = -1, cpLastDeath = -1, cpItems, cpOrbs;
        final List<String> cpTargeted = new ArrayList<>();
        boolean cpGlowBefore; String cpGlow1 = "never looked";
        /** hl-corpse-load: the original corpse row, the 200 troops in kill order, the run, the kills, the server samples, the most bodies lying, each run's figures. */
        Spec.CombatDisciplineRow cpRow;
        final List<GravesAndGlory.Unit> cpLoad = new ArrayList<>();
        int cpRun, cpKilled, cpSamples, cpLying; boolean cpSwept;
        double cpServerSum, cpPeakSum; long cpWall0, cpPeakWall0, cpPeakFrames0; double cpPeakFrame;
        /**
         * Each run: {lie, fade, server ms mean, client world-render ms mean, frames, client worst ms, most bodies lying, measured 1/0, all dead 1/0,
         * server samples, client frame ms (wall clock / frames), arm (0 warm-up, 1 A, 2 B), peak-window server ms, peak-window frame ms}.
         */
        final List<double[]> cpRuns = new ArrayList<>();
        /** The runs by arm (0 a warm-up with the sheet's corpse rows, 1 A with no lie and no fade, 2 B with the sheet's rows): a warm-up, then A B B A eight times (Task 8 fix 2, R43). */
        static final int[] CP_PLAN = {0, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2, 1, 1, 2, 2, 1};
        /** GRAVES_CORPSE_LOAD=0 leaves hl-corpse-load out (a quick hl-corpse run while developing; never for a reported run). */
        static final boolean CP_LOAD = !"0".equals(System.getenv("GRAVES_CORPSE_LOAD"));
        /** GRAVES_FADE_NOGROUPS=1 runs hl-corpse-load with the fade pass's overlap groups off (BodyCache.noGroups, carry D14's red side): what the groups cost, measured. */
        static final boolean CP_NOGROUPS = "1".equals(System.getenv("GRAVES_FADE_NOGROUPS"));
        /** The peak window (Task 7 fix 1): the 40 ticks of stage 131 after this tick, when the most bodies lie. Reported, not gated. */
        static final int CP_PEAK_FROM = 150, CP_PEAK_TICKS = 40;
        final Map<Integer, List<MotionInput.Attack>> evSeenAttacks = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<Integer, List<MotionInput.Hit>> evSeenHits = new java.util.concurrent.ConcurrentHashMap<>();
        volatile boolean evSnap;
        /** The Squire's pending blows: landing tick to swing variant. */
        final Map<Long, Integer> evLands = new HashMap<>();
        /** The troops taken out of tracking and tracked again; the tick they were tracked again; the tick each one's stance reached the client. */
        final List<GravesAndGlory.Unit> evRetrack = new ArrayList<>();
        final Map<Integer, Long> evArrived = new HashMap<>();
        final List<Projectiles.Shot> evShots = new ArrayList<>();
        long evSeen = -1, evLoadAt;
        float evHealth;
        int evSwings, evStep;
        /** The landing ticks of the Squire's blows dealt inside the dummy's hurt cooldown (fix round 1, M3: want light). */
        final Set<Long> evCooled = new HashSet<>();
        /** The Squire's swings the client should hold after a catch-up (evArrival): its fresh swing on START_TRACKING, none once the swing is stale (fix round 1, M6). */
        List<MotionInput.Attack> evWantAttack = List.of();

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
            BodyCache.noGroups = false; // Task 13 fix round 1, M8: an hl-corpse-load that stops before its report leaves the fade's groups on for what runs next
            server(c, b -> {if (cpRow != null) Spec.COMBATDISCIPLINE[0] = cpRow; Houses.forceRise = null; clearFixture(b, "cleanup"); if (site != null) platform(b, site, 12, Blocks.AIR.getDefaultState()); unforce(b); if (b.running) b.campaign.returnLobby();});
        }

        @Override public void step(MinecraftClient c, int tick) {
            if (tick == 20) {c.setScreen(null); UUID id = c.player.getUuid(); server(c, b -> {player = id; next(b, 1);});}
            if (wantShot != null && !shotTaken) {c.options.hudHidden = true; if (++shotDelay >= shotAfter) {ScreenshotRecorder.saveScreenshot(c.runDirectory, wantShot + ".png", c.getFramebuffer(), t -> {}); shotTaken = true; c.options.hudHidden = false;}}
            if (stage == 30 && serverPlaces != null && !mirrorDone) mirror(c);
            int arcId = arcClientId; // hl-arc: the shot's arrow as the client draws it, after the client's own tick
            if (arcId >= 0 && c.world != null) {var e = c.world.getEntityById(arcId); if (e != null) arcClient.add(new double[]{e.getYaw(), e.getPitch(), e.prevYaw, e.prevPitch, e.getX(), e.getY(), e.getZ(), e.prevX, e.prevY, e.prevZ});}
            if (stage == 99 && !finished) {finished = true; c.setScreen(null); Phase2Checks.finish(c, "holdline");}
            if (evSnap) evSnapshot(); // hl-events: what the client holds, after its own tick (a dead dummy's tables go when its entity unloads)
            int cpId = cpClientId; // hl-corpse: the body as the client holds it
            if (cpId >= 0 && c.world != null && c.world.getEntityById(cpId) instanceof net.minecraft.entity.LivingEntity body && body.isDead()) {
                cpClientDeath = Math.max(cpClientDeath, body.deathTime);
                int v = net.minecraft.client.render.entity.LivingEntityRenderer.getOverlay(body, 0f) >> 16;
                if (body.deathTime >= 2 && body.deathTime <= 15 && cpTintFall < 0) cpTintFall = v;
                if (body.deathTime >= 40 && body.deathTime <= 100) cpTintLie = Math.max(cpTintLie, v == 10 ? Math.max(cpTintLie, 10) : 1000 + v);
            }
            int glowId = cpGlowId; // hl-corpse (Task 7 fix 2): the marked Squire as the client holds it, from before its blow
            if (glowId >= 0 && c.world != null && c.world.getEntityById(glowId) instanceof net.minecraft.entity.LivingEntity g) {
                if (!g.isDead() && g.isGlowing()) cpClientGlowBefore = true; // N5: the synced glow flag (6) the client draws
                if (g.isDead() && g.deathTime >= 5 && g.deathTime <= 15 && g.isGlowing()) cpClientGlowFall = true;
                if (g.isDead() && g.deathTime > BattleMath.DEATH_FALL_TICKS && g.deathTime <= 100) {cpClientLyingSeen = true; if (g.canHit()) cpClientHitLying = true;} // N2: the crosshair (GameRenderer.updateTargetedEntity) takes only canHit entities
            }
            int fireId = cpFireId; // m4: the burning body's fire as the client draws it (Entity.isOnFire: the synced flag 0, set from the server's fire ticks)
            if (fireId >= 0 && c.world != null && c.world.getEntityById(fireId) instanceof net.minecraft.entity.LivingEntity f && f.isDead()) {
                if (f.deathTime >= 1 && f.deathTime <= 15 && f.isOnFire()) cpClientFireFall = true;
                if (f.deathTime >= 35 && f.isOnFire()) cpClientFireLate = true;
            }
            if (cpShotPhase > 0 || cpShotId >= 0 && !cpShotDone) cacheShots(c, cpShotId); // R39: the cached body against the normal draw
            else if (cpFadePhase > 0 || cpFadeId >= 0 && !cpFadeDone) fadeShots(c, cpFadeId); // R42: the body as it fades
            int baitId = cpBaitId; // N2: a living troop stays hittable on the client
            if (baitId >= 0 && c.world != null && c.world.getEntityById(baitId) instanceof net.minecraft.entity.LivingEntity l && l.isAlive() && l.canHit()) cpClientBaitHit = true;
            if (tick > (CP_LOAD ? 34000 : 16000) && !finished) // R43: the corpse load runs 33 waves of about 22 s (ABBA x8), some 16000 ticks alone
                {finished = true; report("hl-timeout", false, "stuck in stage " + stage); Phase2Checks.finish(c, "holdline");}
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
            if (player == null || stage == 99) return;
            if (stage == 73 && arcTarget != null) arcAim = Projectiles.aim(arcTarget.mob); // where the battle aimed this tick, before the pins move the dummy on
            hold(); // the pinned enemies of hl-raise and hl-crawl stand where they were put
            try {stages(b); if (stage >= 40) shieldStages(b); if (stage >= 70) arcStages(b); if (stage >= 80) impactStages(b); if (stage >= 100 && stage < 120) eventStages(b); if (stage >= 120) corpseStages(b);} catch (RuntimeException e) {report("hl-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, 99);}
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
                    next(b, corpseOnly ? 120 : 4);
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
            long braced = fixture.stream().filter(u -> u.tags.contains("shield") && (u.stance.equals("brace") || u.stance.equals("wall"))).count(), shields = fixture.stream().filter(u -> u.tags.contains("shield")).count();
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
        /**
         * hl-crawl (Task 8, crawl x Muster): on a crawling Squire, Muster's hasten (Commander.Muster.hasten, the Marshal's rally_horn row) and the
         * crawl multiply: both being MULTIPLY_TOTAL modifiers, the movement speed with both is the crawl's alone times (1 + Muster's value), not
         * the crawl's plus Muster's share of the base (as a MULTIPLY_BASE or ADDITION one would give), and Muster's end (Commander.Muster.end)
         * puts it back to the crawl's alone. Measured on the attribute, the same tick: the travel it gives is hl-crawl's speed checks' and the
         * Muster checks' own, and a few per cent between a product and a sum is below what a walk's travel can tell apart.
         */
        void crawlMuster(GravesAndGlory.Battle b) {
            var u = crawlers.get(0); var spd = u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);
            var marshal = Arrays.stream(Spec.CLASSES).filter(r -> r.active().equals("rally_horn")).findFirst().orElse(null);
            if (spd == null || marshal == null) {crawlPass = false; crawlWhy.add("crawl x Muster: no movement speed attribute or no rally_horn row (want both)=false"); return;}
            var crawl = spd.getModifier(Tactics.CRAWL_ID); double base = spd.getBaseValue(), alone = spd.getValue();
            Commander.Muster.hasten(b, own, marshal);
            var rally = spd.getModifier(Commander.RALLY); double both = spd.getValue();
            new Commander.Muster().end(b, own);
            double after = spd.getValue(), m = rally == null ? Double.NaN : rally.getValue(), product = alone * (1 + m), sum = alone + base * m;
            boolean ops = crawl != null && rally != null && crawl.getOperation() == net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL && rally.getOperation() == net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL;
            boolean ok = ops && Math.abs(both - product) < 1e-9 && Math.abs(both - sum) > 1e-6 && Math.abs(after - alone) < 1e-12 && spd.getModifier(Commander.RALLY) == null && spd.getModifier(Tactics.CRAWL_ID) != null;
            crawlPass &= ok;
            crawlWhy.add(String.format(Locale.ROOT, "crawl x Muster on a crawling Squire: base %.5f, crawl alone %.5f (modifier %s); Muster's hasten (rally_horn +%.2f: modifier %s) on it gives %.5f (want the product %.5f = crawl alone x (1 + %.5f), not the sum %.5f); Muster's end leaves %.5f (want the crawl alone again, the crawl still on)=%b",
                base, alone, crawl == null ? "none" : String.format(Locale.ROOT, "%.5f %s", crawl.getValue(), crawl.getOperation()), marshal.value1(), rally == null ? "none" : String.format(Locale.ROOT, "%.5f %s", m, rally.getOperation()), both, product, m, sum, after, ok));
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
                        crawlMuster(b); // Task 8: the crawl and Muster's hasten multiply
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
        /**
         * hl-impact's brace case (Task 5 fix round 2): the Squire carries one of BRACE_TAGS alone and the dummy's army charges. Cases 0, 2 and 4 swing under
         * Hold and turn to Charge during the wind-up: the blow still braces (its row, as Hold was on the swing tick). Cases 1, 3 and 5 swing under Charge and
         * turn to Hold: no brace. Judged at the landing instead, each would come out the other way.
         */
        void impBraced(GravesAndGlory.Battle b) {
            int k = impBrace / 2; boolean fromHold = impBrace % 2 == 0; String tag = BRACE_TAGS[k], row = BRACE_ROWS[k];
            String swungUnder = fromHold ? "hold" : "charge", landsUnder = fromHold ? "charge" : "hold";
            if (impStrike == null) {
                var s = impSwung(b);
                if (s != null && dusk.order.equals(swungUnder) && impKnight.tags.contains(tag)) {
                    impStrike = s; impSwingAt = b.time; impLog = Combat.LAST.peekLast();
                    dusk.order = landsUnder; dusk.chargedAt.clear(); impTarget.mob.timeUntilRegen = 0; // the order turns during the wind-up
                    return;
                }
                if (since(b) > 300) {impPass = false; impWhy.add("the Squire never swung under " + swungUnder + " for the " + tag + " case=false"); impBraceNext(b); return;}
                // this case's tag alone, the Squire on the order of his swing (the dummy inside his Hold leash), the dummy's army charging (the rows' target)
                impKnight.tags.removeAll(List.of(BRACE_TAGS)); impKnight.tags.add(tag);
                dusk.assignments.clear(); dusk.order = swungUnder; dusk.target = impHome; dusk.chargedAt.clear();
                own.assignments.clear(); own.order = "charge"; impTarget.mob.timeUntilRegen = 0;
                return;
            }
            if (b.time < impStrike.landsAt()) return;
            var hit = impHitLog(impLog); boolean braced = hit != null && hit.rows().contains(row);
            boolean on = hit != null && braced == fromHold;
            impPass &= on;
            impWhy.add("a " + tag + " Squire swung under " + swungUnder + " on tick " + impSwingAt + " at a charging dummy, his order turned to " + landsUnder + " before the blow landed on " + impStrike.landsAt() + ": its rows " + (hit == null ? "none (no hit logged)" : hit.rows()) + " (want " + (fromHold ? row : "no " + row) + ", as on the swing tick)=" + on);
            impBraceNext(b);
        }
        /** The next brace case, or back to the round-end case (89) with the Squire's tags and the armies' orders as they were. */
        void impBraceNext(GravesAndGlory.Battle b) {
            impStrike = null; impBrace++;
            if (impBrace < 2 * BRACE_TAGS.length) {next(b, 98); return;}
            impKnight.tags.removeAll(List.of(BRACE_TAGS)); impKnight.tags.addAll(ArmyMobs.tags(impKnight.spec));
            dusk.assignments.clear(); dusk.order = impDuskOrder; dusk.target = impDuskTarget; dusk.chargedAt.clear(); own.assignments.clear(); own.order = impOwnOrder;
            next(b, 89);
        }
        /** The next hl-impact stage, and how the check stands so far once the last one is done. */
        void impFinish(GravesAndGlory.Battle b) {report("hl-impact", impPass, String.join("; ", impWhy)); next(b, 100);}

        /**
         * hl-impact (stages 80-98, Task 5 and its fix rounds 1 and 2): see the class comment. Every wait is bounded (200 ticks for a swing that never comes, 400 for the first three).
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
                    impStrike = null; impBrace = 0; impOwnOrder = own.order; impDuskOrder = dusk.order; impDuskTarget = dusk.target; next(b, 98);
                }
                case 98 -> impBraced(b); // fix round 2: Hold judged with the Charge on the swing tick
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
                    if (p != null && Phase1Checks.skirmish(b, p, 0, 1, "normal")) {
                        // fix round 2: startRound reseeded the variant rolls from the world's seed and this round's number (no tick has run since)
                        long want = new java.util.Random(BattleMath.roundSeed(b.world.getSeed(), b.rounds)).nextLong(), got = b.rolls.nextLong();
                        b.rolls.setSeed(BattleMath.roundSeed(b.world.getSeed(), b.rounds)); // the draw taken back
                        boolean fresh = got == want; impPass &= fresh;
                        impWhy.add("round " + b.rounds + " began with its variant rolls drawing " + got + " (want " + want + ", a fresh Random on BattleMath.roundSeed of the world's seed and the round)=" + fresh);
                        impFinish(b); return;
                    }
                    if (since(b) > 8000) {impPass = false; impWhy.add("the round never restarted on map 0=false"); impFinish(b);}
                }
                default -> {}
            }
        }
        /** A frozen troop of the commander's army at 4000 health pinned at this point, facing east, whose motion events the client is watched for (hl-events). */
        GravesAndGlory.Unit holder(GravesAndGlory.Battle b, String troop, int slot, Vec3d at) {
            var u = tough(b.spawn(own, Campaign.troop(troop), slot, at)); Phase1Checks.freeze(u);
            fixture.add(u); pins.add(new Pin(u, at, -90f)); u.deployed = at; Phase2Checks.Engine.apply(u, at, -90f); evWatch.add(u.mob.getId()); return u;
        }
        /** hl-events, on the client after its tick: the watched troops' swings and blows as MotionEvents.Client holds them. */
        void evSnapshot() {
            for (int id : evWatch) {
                var a = MotionEvents.Client.attacks(id); if (!a.isEmpty()) evSeenAttacks.put(id, List.copyOf(a));
                var h = MotionEvents.Client.hits(id); if (!h.isEmpty()) evSeenHits.put(id, List.copyOf(h));
            }
        }
        void evCheck(boolean ok, String why) {evPass &= ok; evWhy.add(why + "=" + ok);}
        void evFinish(GravesAndGlory.Battle b) {evSnap = false; report("hl-events", evPass, String.join("; ", evWhy)); next(b, 120);}
        /** A hit as the server meant it and as the client holds it: the same kind, zone, weight (want -1: not judged), flags and tick, the bearing within 10 degrees. */
        static boolean same(MotionInput.Hit want, MotionInput.Hit got) {
            return got != null && want.kind().equals(got.kind()) && want.zone().equals(got.zone()) && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(want.bearing() - got.bearing())) <= 10
                && (want.weight() < 0 || want.weight() == got.weight()) && want.blocked() == got.blocked() && want.lethal() == got.lethal() && want.tick() == got.tick();
        }
        static String evShow(MotionInput.Hit h) {return h.kind() + "/" + h.zone() + "/" + fmt(h.bearing()) + "/w" + h.weight() + (h.blocked() ? "/blocked" : "") + (h.lethal() ? "/lethal" : "") + "@" + h.tick();}
        void evCompareHits(String who, GravesAndGlory.Unit u) {
            int id = u.mob.getId(); var want = evHits.getOrDefault(id, List.of()); var got = evSeenHits.getOrDefault(id, List.of());
            boolean ok = !want.isEmpty() && want.size() == got.size();
            for (int i = 0; ok && i < want.size(); i++) ok = same(want.get(i), got.get(i));
            evCheck(ok, who + ": the client holds hits " + got.stream().map(Wall::evShow).toList() + " (want " + want.stream().map(Wall::evShow).toList() + ")");
        }
        /** The hit_v1 the server should send for the Squire's blow of this swing variant on the victim now (Combat): its swing's kind, zone and weight. */
        MotionInput.Hit evBlow(GravesAndGlory.Battle b, GravesAndGlory.Unit victim, int variant, boolean lethal) {
            var row = Tactics.variants(evKnight.spec.family()).get(variant);
            float bearing = (float) BattleMath.signedBearing(victim.mob.getYaw(), evKnight.mob.getX() - victim.mob.getX(), evKnight.mob.getZ() - victim.mob.getZ());
            return new MotionInput.Hit(BattleMath.hitKind("melee", evKnight.spec.family(), row.kind()), row.zone(), bearing, BattleMath.weightIndex(row.weight()), false, lethal, b.world.getTime());
        }
        /** A new swing of hl-events' Squire on this tick: its pending blow's variant is the unit's (carry 5), and the client is owed attack_v1 with it and the world tick now. */
        void evSwing(GravesAndGlory.Battle b) {
            var k = evKnight; if (k == null || k.swungAt != b.time || k.swungAt == evSeen) return;
            evSeen = k.swungAt;
            GravesAndGlory.Battle.PendingStrike s = null;
            for (var x : b.strikes) if (x.attacker() == k && x.landsAt() > b.time) s = x;
            if (s == null) {evCheck(false, "the Squire swung on tick " + b.time + " with no blow pending"); return;}
            evCheck(s.variant() == k.lastVariant, "swing " + (evSwings + 1) + ": its pending blow's variant " + s.variant() + ", the unit's lastVariant " + k.lastVariant + " (want the same)");
            evLands.put(s.landsAt(), s.variant());
            evAttacks.computeIfAbsent(k.mob.getId(), x -> new ArrayList<>()).add(new MotionInput.Attack(k.lastVariant, b.world.getTime()));
            boolean dummy = stage == 102 && evTarget != null && evTarget.mob.isAlive();
            if (dummy) { // fix round 1, M11: the dummy faces the Squire for the first blow, turns its left side to it for the second and its back for the third
                float yaw = evSwings == 0 ? -90f : evSwings == 1 ? 0f : 90f;
                pins.removeIf(p -> p.u() == evTarget); pins.add(new Pin(evTarget, evHome, yaw)); Phase2Checks.Engine.apply(evTarget, evHome, yaw);
            }
            evSwings++;
            for (var t : Arrays.asList(evTarget, evLethal)) if (t != null && t.mob.isAlive()) t.mob.timeUntilRegen = 0; // no blow is lost to the hurt cooldown
            if (dummy && evCooled.isEmpty() && BattleMath.weightIndex(Tactics.variants(k.spec.family()).get(s.variant()).weight()) > 0) {
                // fix round 1, M3: the first heavier blow lands inside the dummy's hurt cooldown, which takes all but half a point off it: light, from what it lost
                evTarget.mob.timeUntilRegen = 40; evLastDamage(evTarget.mob, 0.5f); evCooled.add(s.landsAt());
            }
        }
        /** Sets the protected LivingEntity.lastDamageTaken (the hurt cooldown deals only what a blow has over it). */
        static void evLastDamage(net.minecraft.entity.LivingEntity e, float v) {
            try {var f = net.minecraft.entity.LivingEntity.class.getDeclaredField("lastDamageTaken"); f.setAccessible(true); f.setFloat(e, v);}
            catch (ReflectiveOperationException x) {throw new RuntimeException(x);}
        }
        /** A hit_v1 packet straight to the player, as MotionEvents.sendHit builds it but with this weight byte (fix round 1, M4: the client clamps it). */
        static void evRawHit(net.minecraft.server.network.ServerPlayerEntity p, int id, int weight, long tick) {
            var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            buf.writeVarInt(id); buf.writeByte(MotionEvents.KINDS.indexOf("blunt")); buf.writeByte(MotionEvents.ZONES.indexOf("body")); buf.writeFloat(0f); buf.writeByte(weight); buf.writeByte(0); buf.writeVarLong(tick);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, MotionEvents.HIT, buf);
        }
        /** The Dusk guard keeps its shield up: its group under fire (so Tactics leaves it raised off Hold) and Tactics.shield. */
        void evGuardUp(GravesAndGlory.Battle b) {
            if (evGuard == null || !evGuard.mob.isAlive()) return;
            dusk.shotAt.put(evGuard.spec.group(), b.time); Tactics.shield(evGuard.mob, true);
        }
        /** The troop leaves every player's tracking (unloadEntity: its clients unload it) or is tracked again (loadEntity: START_TRACKING, then its spawn packet). */
        static void evTrack(GravesAndGlory.Battle b, GravesAndGlory.Unit u, boolean load) {
            try {
                var m = net.minecraft.server.world.ThreadedAnvilChunkStorage.class.getDeclaredMethod(load ? "loadEntity" : "unloadEntity", net.minecraft.entity.Entity.class);
                m.setAccessible(true); m.invoke(b.world.getChunkManager().threadedAnvilChunkStorage, u.mob);
            } catch (ReflectiveOperationException e) {throw new RuntimeException(e);}
        }
        /**
         * Whether the stances owed since evLoadAt have reached the client (or 20 ticks have passed), judging them once they have: each troop with a
         * stance has it on the client within 2 ticks, a troop with none shows none, and the Squire's swings on the client are evWantAttack.
         */
        boolean evArrival(GravesAndGlory.Battle b, String how) {
            for (var u : evRetrack) {int id = u.mob.getId(); if (!u.stance.isEmpty() && !evArrived.containsKey(id) && MotionEvents.Client.STANCES.containsKey(id)) evArrived.put(id, b.time);}
            long owed = evRetrack.stream().filter(u -> !u.stance.isEmpty()).count();
            List<MotionInput.Attack> want = evWantAttack, got = List.copyOf(MotionEvents.Client.attacks(evKnight.mob.getId()));
            if ((evArrived.size() < owed || !got.equals(want)) && since(b) <= 20) return false;
            List<String> seen = new ArrayList<>(); boolean ok = owed > 0;
            for (var u : evRetrack) {
                int id = u.mob.getId(); var c = MotionEvents.Client.stance(id); Long at = evArrived.get(id);
                boolean on = u.stance.isEmpty() ? c.name().isEmpty() : at != null && at - evLoadAt <= 2 && c.name().equals(u.stance) && Math.abs(c.aimPitch() - u.aimPitch) <= Tactics.AIM_STEP + 0.01;
                ok &= on;
                seen.add(name(u) + " " + (c.name().isEmpty() ? "none" : c.name() + " " + fmt(c.aimPitch())) + (at == null ? "" : " after " + (at - evLoadAt)) + " (server " + (u.stance.isEmpty() ? "none" : u.stance + " " + fmt(u.aimPitch)) + ")");
            }
            ok &= got.equals(want);
            evCheck(ok, how + ": " + seen + " (want each troop's stance within 2 ticks, none for a troop with none); the Squire's swings on the client " + got + " (want " + (want.isEmpty() ? "none, its swing stale" : "its current swing after the spawn, " + want) + ")");
            return true;
        }

        /** hl-events (stages 100-110, Task 6): see the class comment. Every wait is bounded. */
        void eventStages(GravesAndGlory.Battle b) {
            evSwing(b);
            switch (stage) {
                case 100 -> { // a fresh fixture on the same patch, the camera 24 blocks north of it
                    if (!b.running || since(b) < 20) {if (since(b) > 2000) {evCheck(false, "the round never ran for hl-events"); evFinish(b);} return;}
                    if (patch == null) {evCheck(false, "no level patch for hl-events"); evFinish(b); return;}
                    clearFixture(b, "hl-events"); crawlers.clear(); archers.clear(); Projectiles.clear(b); b.strikes.clear();
                    evHome = ground(b, patch[0] + 4, patch[2]);
                    own.order = "hold"; own.target = evHome; own.yaw = -90; own.assignments.clear(); own.arrived.clear(); own.shotAt.clear();
                    dusk.order = "attack"; dusk.assignments.clear(); dusk.shotAt.clear();
                    var p = sp(b); b.relocate(p, patch[0] + 12, Phase1Checks.top(b, patch[0] + 12, patch[2] - 24) + 6, patch[2] - 24, 0f, 15f);
                    next(b, 101);
                }
                case 101 -> { // the troops: the Squire and its dummy west, the holders and the Dusk archer east
                    if (since(b) < 40) return; // the client has the chunks round the camera
                    evTarget = holder(b, "archer", 40, evHome);
                    evKnight = pinned(b, "knight", 41, ground(b, evHome.x + 1.8, evHome.z), false); evWatch.add(evKnight.mob.getId());
                    evHg = holder(b, "ic_hearthguard", 42, ground(b, patch[0] + 20, patch[2] + 4));
                    evTusk = holder(b, "wm_tusk", 43, ground(b, patch[0] + 20, patch[2] + 1));
                    evBreaker = holder(b, "ic_breaker", 44, ground(b, patch[0] + 20, patch[2] - 2));
                    evReaper = holder(b, "reaper", 45, ground(b, patch[0] + 20, patch[2] - 5));
                    evArcher = pinned(b, "archer", 46, ground(b, patch[0] + 28, patch[2] + 4), true); evWatch.add(evArcher.mob.getId());
                    evGuard = pinned(b, "ic_hearthguard", 48, ground(b, patch[0] + 12, patch[2] + 6), true); evWatch.add(evGuard.mob.getId());
                    pins.removeIf(p -> p.u() == evGuard); pins.add(new Pin(evGuard, evGuard.deployed, 180f)); Phase2Checks.Engine.apply(evGuard, evGuard.deployed, 180f);
                    evHealth = evTarget.mob.getHealth(); evSnap = true;
                    next(b, 102);
                }
                case 102 -> { // three swings, each blow on the dummy; then the Squire stops
                    if (since(b) == 2) {
                        var p = sp(b);
                        evCheck(p != null && net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(evKnight.mob).contains(p) && net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(evHg.mob).contains(p), "the player tracks the Squire and the Hearthguard once they stand");
                    }
                    float h = evTarget.mob.getHealth();
                    if (h < evHealth) {
                        Integer v = evLands.remove(b.time);
                        if (v == null) evCheck(false, "the dummy was hurt on tick " + b.time + " with no blow of the Squire due");
                        else {
                            var w = evBlow(b, evTarget, v, false);
                            if (evCooled.contains(b.time)) w = new MotionInput.Hit(w.kind(), w.zone(), w.bearing(), 0, w.blocked(), w.lethal(), w.tick());
                            evHits.computeIfAbsent(evTarget.mob.getId(), x -> new ArrayList<>()).add(w);
                        }
                        evHealth = h;
                    }
                    for (var it = evLands.keySet().iterator(); it.hasNext(); ) {long t = it.next(); if (t < b.time) {it.remove(); evCheck(false, "the blow due on tick " + t + " never hurt the dummy");}}
                    if (evSwings >= 3) evKnight.nextAttack = Long.MAX_VALUE;
                    if (evSwings >= 3 && evLands.isEmpty()) {next(b, 103); return;}
                    if (since(b) > 600) {evCheck(false, "the Squire swung " + evSwings + " times in 600 ticks (want 3, every blow landed)"); evKnight.nextAttack = Long.MAX_VALUE; evLands.clear(); next(b, 103);}
                }
                case 103 -> { // the lethal blow: a dummy on 1 health, the Squire free to swing again
                    evCheck(evCooled.size() == 1, "one of the Squire's heavier blows fell inside the dummy's hurt cooldown");
                    unpin(b, List.of(evTarget)); // its id stays watched until the client has had its last blow (the probes in 106 take it off)
                    evLethal = holder(b, "archer", 47, evHome); evLethal.mob.setHealth(1f);
                    evKnight.nextAttack = b.time;
                    next(b, 104);
                }
                case 104 -> {
                    if (!evLethal.mob.isAlive()) {
                        Integer v = evLands.remove(b.time);
                        if (v == null) evCheck(false, "the 1-health dummy died on tick " + b.time + " with no blow of the Squire due");
                        else evHits.computeIfAbsent(evLethal.mob.getId(), x -> new ArrayList<>()).add(evBlow(b, evLethal, v, true));
                        evKnight.nextAttack = Long.MAX_VALUE; evLands.clear(); next(b, 105); return;
                    }
                    if (since(b) > 300) {evCheck(false, "the Squire never killed the 1-health dummy"); evKnight.nextAttack = Long.MAX_VALUE; evLands.clear(); next(b, 105);}
                }
                case 105 -> { // an arrow at the Hearthguard, its shield up, and one at the Breaker
                    Tactics.shield(evHg.mob, true); evGuardUp(b);
                    if (evShots.isEmpty()) {
                        boolean raised = evHg.mob.isUsingItem() && evHg.mob.getActiveItem().isOf(net.minecraft.item.Items.SHIELD);
                        if (since(b) < 5 || !raised) {if (since(b) > 100) {evCheck(false, "the Hearthguard never raised its shield"); evStep = 0; next(b, 106);} return;}
                        for (var t : List.of(evHg, evBreaker)) {
                            t.mob.timeUntilRegen = 0; t.mob.hurtTime = 0;
                            Projectiles.fire(b, evArcher, t.mob, "arrow", 2f);
                            var s = b.shots.get(b.shots.size() - 1); evShots.add(s);
                            float bearing = (float) BattleMath.signedBearing(t.mob.getYaw(), evArcher.mob.getX() - t.mob.getX(), evArcher.mob.getZ() - t.mob.getZ());
                            String zone = BattleMath.zone(Projectiles.aim(t.mob).y, t.mob.getY(), t.mob.getHeight());
                            evHits.computeIfAbsent(t.mob.getId(), x -> new ArrayList<>()).add(new MotionInput.Hit("arrow", zone, bearing, -1, t == evHg, false, b.world.getTime() + (s.lands() - b.time)));
                        }
                        return;
                    }
                    long last = evShots.stream().mapToLong(Projectiles.Shot::lands).max().orElse(b.time);
                    if (b.time > last) {evCheck(b.shots.stream().noneMatch(evShots::contains), "both arrows left the queue by tick " + last); evStep = 0; next(b, 106);}
                }
                case 106 -> { // a fall on the Breaker, then magic damage, a tick apart; the player's blow on the guard's shield; the per-target guard and the client's drops
                    Tactics.shield(evHg.mob, true); evGuardUp(b);
                    var m = evBreaker.mob; var magic = b.world.getDamageSources().magic(); long now = b.world.getTime();
                    if (evStep < 2) {
                        m.timeUntilRegen = 0; m.hurtTime = 0;
                        boolean fall = evStep == 0;
                        boolean hurt = m.damage(fall ? b.world.getDamageSources().fall() : magic, 1f);
                        evCheck(hurt, "the Breaker took " + (fall ? "a fall" : "magic damage") + " on tick " + b.time);
                        if (hurt) evHits.computeIfAbsent(m.getId(), x -> new ArrayList<>()).add(new MotionInput.Hit(fall ? "blunt" : "magic", fall ? "legs" : "body", 0f, 0, false, false, now));
                        evStep++; return;
                    }
                    var p = sp(b);
                    if (evStep == 2) { // fix round 1, M1: a player's blow the guard's raised shield stops whole is a hit, blocked, of no weight
                        var g = evGuard.mob;
                        if (!g.isBlocking()) {if (since(b) > 100) {evCheck(false, "the Dusk guard never blocked with its shield"); evStep = 3;} return;}
                        g.timeUntilRegen = 0; g.hurtTime = 0; float before = g.getHealth(); int sends = MotionEvents.hitSends;
                        boolean hurt = g.damage(b.world.getDamageSources().playerAttack(p), 4f);
                        evCheck(!hurt && g.getHealth() == before && MotionEvents.hitSends > sends, "the player's blow on the guard's raised shield: damage returned " + hurt + ", health " + fmt(before) + " to " + fmt(g.getHealth()) + ", hit_v1 sent " + (MotionEvents.hitSends - sends) + " (want false, unchanged, one or more)");
                        String kind = BattleMath.weaponKind(net.minecraft.registry.Registries.ITEM.getId(p.getMainHandStack().getItem()).toString());
                        float bearing = (float) BattleMath.signedBearing(g.getYaw(), p.getX() - g.getX(), p.getZ() - g.getZ());
                        evHits.computeIfAbsent(g.getId(), x -> new ArrayList<>()).add(new MotionInput.Hit(kind, "body", bearing, 0, true, false, now));
                        evStep = 3; return;
                    }
                    // fix round 1, M2: Combat's guard covers only its own target, so another troop struck inside it still sends its hit; its own target sends none
                    m.timeUntilRegen = 0; m.hurtTime = 0; int sends = MotionEvents.hitSends;
                    boolean other = MotionEvents.resolving(evHg.mob, () -> m.damage(magic, 1f));
                    evCheck(other && MotionEvents.hitSends > sends, "magic damage on the Breaker while the Hearthguard's blow resolves: damage returned " + other + ", hit_v1 sent " + (MotionEvents.hitSends - sends) + " (want true, one or more)");
                    if (other) evHits.computeIfAbsent(m.getId(), x -> new ArrayList<>()).add(new MotionInput.Hit("magic", "body", 0f, 0, false, false, now));
                    m.timeUntilRegen = 0; m.hurtTime = 0; sends = MotionEvents.hitSends;
                    boolean own = MotionEvents.resolving(m, () -> m.damage(magic, 1f));
                    evCheck(own && MotionEvents.hitSends == sends, "magic damage on the Breaker while its own blow resolves: damage returned " + own + ", hit_v1 sent " + (MotionEvents.hitSends - sends) + " (want true, none)");
                    // fix round 1, M4 and M5: what the client must drop or clamp
                    var to = List.of(p); evWatch.remove(evTarget.mob.getId()); // its hits as the client held them stay in evSeenHits
                    MotionEvents.sendAttack(evTarget, 0, now, to); // the discarded dummy: an id the client world no longer has
                    MotionEvents.sendHit(evTarget.mob, "slash", "body", 0f, 1, false, false, to);
                    MotionEvents.sendAttack(evKnight, Tactics.variants(evKnight.spec.family()).size(), now, to); // past the family's rows
                    MotionEvents.sendAttack(evKnight, -1, now, to);
                    evRawHit(p, m.getId(), 7, now); evRawHit(p, m.getId(), -3, now); // weights out of 0..2
                    evHits.get(m.getId()).add(new MotionInput.Hit("blunt", "body", 0f, 2, false, false, now));
                    evHits.get(m.getId()).add(new MotionInput.Hit("blunt", "body", 0f, 0, false, false, now));
                    next(b, 107);
                }
                case 107 -> { // what the client holds against what the server sent; the plain-hold rule; then every troop out of the player's tracking
                    Tactics.shield(evHg.mob, true); evGuardUp(b);
                    if (since(b) < 8) return;
                    evSnap = false;
                    int kid = evKnight.mob.getId(); var want = evAttacks.getOrDefault(kid, List.of()); var got = evSeenAttacks.getOrDefault(kid, List.of());
                    evCheck(want.size() == 4 && got.equals(want), "the client holds the Squire's swings " + got + " (want the server's four, each with Battle's variant and the world tick of its swing: " + want + ")");
                    evCompareHits("the first dummy (three blows)", evTarget);
                    evCompareHits("the 1-health dummy (a killing blow)", evLethal);
                    evCompareHits("the Hearthguard (an arrow on its raised shield)", evHg);
                    evCompareHits("the Breaker (an arrow, a fall, magic, magic inside another's resolve, two clamped weights)", evBreaker);
                    evCompareHits("the Dusk guard (a player's blow stopped by its shield)", evGuard);
                    // fix round 1, M11: the dummy's three blows from its front, its left side and its back
                    var dh = evSeenHits.getOrDefault(evTarget.mob.getId(), List.of());
                    boolean sides = dh.size() == 3 && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(dh.get(0).bearing())) <= 10
                        && dh.get(1).bearing() < 0 && Math.abs(dh.get(1).bearing() + 90) <= 10 && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(dh.get(2).bearing() - 180)) <= 10;
                    evCheck(sides, "the dummy's blows on the client from bearings " + dh.stream().map(h -> fmt(h.bearing())).toList() + " (want near 0, near -90 from its left, near 180 from behind)");
                    // fix round 1, M4 and M5: nothing kept for an id the client world lacks, no swing outside the family's rows
                    int tid = evTarget.mob.getId(), rows = Tactics.variants(evKnight.spec.family()).size();
                    evCheck(!MotionEvents.Client.ATTACKS.containsKey(tid) && !MotionEvents.Client.HITS.containsKey(tid), "the client kept no swing or hit for the discarded dummy's id");
                    var sw = MotionEvents.Client.attacks(kid);
                    evCheck(sw.stream().allMatch(a -> BattleMath.validVariant(a.variant(), rows)), "the client's Squire swings " + sw + " all index its family's " + rows + " rows");
                    // fix round 1, M7: aimPitch positive down on the wire as in MotionInput, so a lofted shot reads negative on both sides
                    var ac = MotionEvents.Client.stance(evArcher.mob.getId());
                    evCheck(evArcher.stance.equals("ranged_aim") && evArcher.aimPitch < 0 && ac.name().equals("ranged_aim") && Math.abs(ac.aimPitch() - evArcher.aimPitch) <= Tactics.AIM_STEP + 0.01,
                        "the Dusk archer aims " + evArcher.stance + " " + fmt(evArcher.aimPitch) + " on the server, " + ac.name() + " " + fmt(ac.aimPitch()) + " on the client (want ranged_aim, below 0 for its lofted volley, the same on both)");
                    for (var u : List.of(evHg, evTusk, evBreaker, evReaper)) {
                        boolean wall = u == evHg || u == evTusk; String c = MotionEvents.Client.stance(u.mob.getId()).name();
                        boolean on = (wall ? u.stance.equals("brace") || u.stance.equals("wall") : u.stance.equals("hold")) && c.equals(u.stance);
                        evCheck(on, "plain-hold rule: on Hold " + name(u) + " (" + (u == evHg ? "shield" : u == evTusk ? "spear" : "neither shield nor spear") + ") shows " + (u.stance.isEmpty() ? "none" : u.stance) + " on the server, " + (c.isEmpty() ? "none" : c) + " on the client (want " + (wall ? "brace or wall" : "hold") + ", the same on both)");
                    }
                    evRetrack.clear(); for (var u : fixture) if (u.mob.isAlive()) evRetrack.add(u);
                    for (var u : evRetrack) evTrack(b, u, false);
                    next(b, 108);
                }
                case 108 -> { // the client forgot them; then they are tracked again
                    boolean gone = evRetrack.stream().allMatch(u -> {int id = u.mob.getId(); return !MotionEvents.Client.STANCES.containsKey(id) && !MotionEvents.Client.ATTACKS.containsKey(id) && !MotionEvents.Client.HITS.containsKey(id);});
                    if (!gone && since(b) <= 40) return;
                    evCheck(gone, "out of the player's tracking, the client forgot the stances, swings and hits of " + evRetrack.size() + " troops");
                    // fix round 1, M6: the Squire swung a tick ago, so the catch-up owes it, and it must reach the client after the spawn packet (the client drops unknown ids)
                    long now = b.world.getTime(); evKnight.swungAt = b.time - 1; evKnight.swingTick = now - 1;
                    evWantAttack = List.of(new MotionInput.Attack(evKnight.catchVariant, now - 1));
                    for (var u : evRetrack) evTrack(b, u, true);
                    var p = sp(b);
                    evCheck(p != null && evRetrack.stream().allMatch(u -> net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(u.mob).contains(p)), "the player tracks the " + evRetrack.size() + " troops again");
                    evLoadAt = b.time; evArrived.clear(); next(b, 109);
                }
                case 109 -> { // late join through START_TRACKING; then the channel registration replayed, the client's tables emptied first
                    if (!evArrival(b, "tracked again (START_TRACKING)")) return;
                    MotionEvents.Client.clear();
                    // fix round 1, M6: a swing older than its impact and recovery is not resent
                    evKnight.swungAt = b.time - 100; evKnight.swingTick = b.world.getTime() - 100; evWantAttack = List.of();
                    var p = sp(b);
                    net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents.REGISTER.invoker().onChannelRegister(p.networkHandler, net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.getSender(p), b.server, List.of(MotionEvents.STANCE));
                    evLoadAt = b.time; evArrived.clear(); next(b, 110);
                }
                case 110 -> {if (evArrival(b, "the channels registered again (REGISTER)")) evFinish(b);}
                default -> {}
            }
        }
        /** hl-corpse and hl-corpse-load (Task 7): a fallen troop lies, out of the fight, and goes on BattleMath.corpseGoneTick; what lying bodies cost. */
        void corpseStages(GravesAndGlory.Battle b) {
            switch (stage) {
                case 120 -> { // a fresh fixture on the same patch, the camera 24 blocks north of it
                    if (!b.running || since(b) < 20) {if (since(b) > 2000) {cpCheck(false, "the round never ran for hl-corpse"); cpFinish(b);} return;}
                    if (patch == null) {cpCheck(false, "no level patch for hl-corpse"); cpFinish(b); return;}
                    clearFixture(b, "hl-corpse"); crawlers.clear(); archers.clear(); Projectiles.clear(b); b.strikes.clear(); b.clearCorpses();
                    if (!cpHooked) {cpHooked = true; net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((e, src) -> CP_DEATHS.merge(e.getId(), 1, Integer::sum));}
                    Houses.forceRise = Boolean.FALSE;
                    cpHome = ground(b, patch[0] + 6, patch[2]);
                    own.order = "hold"; own.target = cpHome; own.yaw = -90; own.assignments.clear(); own.arrived.clear(); own.shotAt.clear();
                    dusk.order = "attack"; dusk.assignments.clear(); dusk.shotAt.clear();
                    var p = sp(b); b.relocate(p, patch[0] + 12, Phase1Checks.top(b, patch[0] + 12, patch[2] - 24) + 6, patch[2] - 24, 0f, 15f);
                    next(b, 121);
                }
                case 121 -> { // the Squire to fall, the other own Squire 6 west, the Dusk Squire 1.8 east of the first
                    if (since(b) < 40) return; // the client has the chunks round the camera
                    cpVictim = cpOwn(b, 60, cpHome);
                    cpBait = cpOwn(b, 61, ground(b, cpHome.x - 6, cpHome.z));
                    cpHunter = pinned(b, "knight", 62, ground(b, cpHome.x + 1.8, cpHome.z), false);
                    var v = cpVictim.mob; // marked well before the blow, so the client holds its glow (Task 7 fix 2, N5)
                    v.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.GLOWING, 400, 0, false, false)); // as Mark Prey marks it (Commander.MarkPrey)
                    v.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS, 400));
                    cpGlowId = v.getId(); cpBaitId = cpBait.mob.getId();
                    next(b, 122);
                }
                case 122 -> { // the Dusk Squire strikes it down
                    if (since(b) < 30) return;
                    cpFielded0 = b.fielded(own).count();
                    var v = cpVictim.mob; v.setHealth(1f);
                    cpGlowBefore = v.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.GLOWING) && v.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
                    strike(b, cpHunter, cpVictim, 50f);
                    cpCheck(v.isDead(), "the Dusk Squire's blow killed the Squire on 1 health");
                    cpKnock = v.getVelocity().horizontalLength(); // Task 7 fix 2 (N1): the killing blow throws it back (design 5)
                    cpBody0 = v.getPos(); cpClientId = v.getId();
                    next(b, 123);
                }
                case 123 -> { // it lies; then it goes
                    long n = since(b); var v = cpVictim.mob;
                    if (!v.isRemoved()) cpLastDeath = v.deathTime; else if (cpGoneAt < 0) cpGoneAt = n;
                    if (n == cpLieEnd()) {cpAt79 = !v.isRemoved() && v.isDead(); cpDeath79 = v.deathTime;} // R43: from the sheet's rows, which may lower the lie
                    if (n == cpGone() - 1) cpAt119 = !v.isRemoved() && v.isDead();
                    var box = new net.minecraft.util.math.Box(cpBody0, cpBody0).expand(4);
                    cpItems = Math.max(cpItems, b.world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class, box, e -> true).size());
                    cpOrbs = Math.max(cpOrbs, b.world.getEntitiesByClass(net.minecraft.entity.ExperienceOrbEntity.class, box, e -> true).size());
                    if (n >= 1) for (var u : b.units) { // no unit keeps a dead entity as its target (the hunter's own one included)
                        var t = u.mob.getTarget(); var dead = u.lockedTarget != null && !u.lockedTarget.isAlive() ? u.lockedTarget : t != null && !t.isAlive() ? t : null;
                        if (dead != null && cpTargeted.size() < 6) cpTargeted.add(u.spec.id() + " -> " + dead.getId() + " at +" + n);
                    }
                    if (n == 1) cpGlow1 = "glowing " + v.isGlowing() + ", effects " + v.getStatusEffects().size();
                    if (n == Math.min(60, cpLieEnd())) requestShot("hl-corpse-lying", 4); // Task 7 fix 1 (I4): a body up to forty ticks past its fall (before its fade), lying with no red tint
                    if (n == 1) {cpHitless = !v.canHit() && !v.isPushable(); cpOutOfUnits = !b.units.contains(cpVictim); cpNoSide = v.getScoreboardTeam() == null; cpFielded1 = b.fielded(own).count();}
                    if (n == 5) { // an own Squire, AI on but out of the battle's orders and its side (so nothing but a push moves it), 0.15 from the body
                        var l = tough(b.spawn(own, Campaign.troop("knight"), 63, ground(b, v.getX(), v.getZ() + 0.15))); Phase1Checks.freeze(l);
                        b.units.remove(l); GravesAndGlory.Battle.leaveSide(l.mob); fixture.add(l); cpLeaner = l;
                    }
                    if (n == 10) {cpLean0 = cpLeaner.mob.getPos(); cpBody10 = v.getPos();} // from tick 10: the killing blow's knockback has settled
                    if (n == 30) {
                        double lean = distance(cpLeaner.mob.getPos(), cpLean0), body = distance(v.getPos(), cpBody10);
                        cpCheck(lean < 0.02 && body < 0.02, "a Squire 0.15 from the body moved " + fmt3(lean) + " over 20 ticks and the body " + fmt3(body) + " (want both under 0.02: it blocks and pushes nothing)");
                    }
                    if (n < 125) return;
                    cpClientId = -1; cpGlowId = -1; cpBaitId = -1;
                    cpCheck(cpKnock > 0.1, "the killing blow left the body moving " + fmt3(cpKnock) + " blocks a tick across the ground on its death tick (want over 0.1: it is thrown back)");
                    cpCheck(cpClientGlowBefore && !cpClientGlowFall, "the client saw the marked Squire glowing before the blow " + cpClientGlowBefore + " (want true), and glowing from its death tick 5 to 15 " + cpClientGlowFall + " (want false)");
                    cpCheck(cpClientLyingSeen && !cpClientHitLying && cpClientBaitHit, "on the client the lying body (death ticks 21-100, seen " + cpClientLyingSeen + ") could be hit (canHit) " + cpClientHitLying + " (want false: the crosshair passes through it, so no nameplate); the living Squire beside it could " + cpClientBaitHit + " (want true)");
                    int lieEnd = cpLieEnd(), gone = cpGone(); var cr = Spec.COMBATDISCIPLINE[0];
                    cpCheck(cpAt79 && cpDeath79 == lieEnd, "the body is in the world and dead on its " + lieEnd + "th death tick, its last before its fade (death ticks " + cpDeath79 + ")");
                    cpCheck(cpAt119, "it still lies on its " + (gone - 1) + "th");
                    cpCheck(cpGoneAt == gone && cpLastDeath == gone - 1, "removed on tick +" + cpGoneAt + ", last seen lying on death tick " + cpLastDeath + " (want +" + gone + ", 20 + lie " + cr.corpseLieTicks() + " + fade " + cr.corpseFadeTicks() + "; gone by +" + (gone + 1) + ")");
                    cpCheck(cpClientDeath >= lieEnd, "the client held it lying to death tick " + cpClientDeath + " (want " + lieEnd + " or more)");
                    cpCheck(cpTintFall == 3 && cpTintLie == 10, "the client's overlay V for the body (getOverlay: 3 the red hurt tint, 10 none) as it fell " + cpTintFall + " (want 3), on every client tick it lay from death tick 40 to 100 " + (cpTintLie >= 1000 ? "once " + (cpTintLie - 1000) : String.valueOf(cpTintLie)) + " (want 10)");
                    cpCheck(cpItems == 0 && cpOrbs == 0, "within 4 blocks of it lay " + cpItems + " items and " + cpOrbs + " experience orbs (want none)");
                    int deaths = CP_DEATHS.getOrDefault(v.getId(), 0);
                    cpCheck(deaths == 1, "AFTER_DEATH fired " + deaths + " times for it (want 1)");
                    cpCheck(cpTargeted.isEmpty(), "units that targeted a dead entity after the body fell: " + cpTargeted + " (want none)");
                    cpCheck(cpGlowBefore && cpGlow1.equals("glowing false, effects 0"), "marked glowing (and slowed) before the blow " + cpGlowBefore + "; a tick after it fell: " + cpGlow1 + " (want glowing false, effects 0)");
                    cpCheck(cpHitless, "the body cannot be hit (canHit) nor pushed (isPushable)");
                    cpCheck(cpOutOfUnits && cpNoSide && cpFielded1 == cpFielded0 - 1, "a tick after it fell it is out of Battle.units=" + cpOutOfUnits + ", out of its side team=" + cpNoSide + ", the army fields " + cpFielded1 + " (was " + cpFielded0 + ", want one less)");
                    next(b, 124);
                }
                case 124 -> { // Rise Again: a Revenant of a Graveborn army rises with the roll forced; struck down again it lies
                    for (int i = 0; i < 6; i++) {var r = pinned(b, "revenant", 70 + i, ground(b, cpHome.x + 4, cpHome.z - 4 + i * 1.5), true); if (i == 0) cpRevenant = r;}
                    Houses.recompute(dusk);
                    var r = cpRevenant; int tier = dusk.traits.getOrDefault("graveborn", 0);
                    r.mob.setHealth(1f); // struck by the own Squire, so the blow has an attacker and its knockback (Task 7 fix 2, N1)
                    Houses.forceRise = Boolean.TRUE; try {strike(b, cpBait, r, 50f); cpRiseKnock = r.mob.getVelocity().horizontalLength();} finally {Houses.forceRise = Boolean.FALSE;}
                    boolean rose = r.mob.isAlive() && !r.mob.isDead() && r.rose && r.mob.deathTime == 0 && CP_DEATHS.getOrDefault(r.mob.getId(), 0) == 0;
                    cpCheck(tier >= 1 && rose, "a Revenant of a Graveborn army (tier " + tier + ") struck down with the roll forced rose: alive " + r.mob.isAlive() + ", rose " + r.rose + ", death ticks " + r.mob.deathTime);
                    cpCheck(cpRiseKnock > 0.1, "the blow it rose from threw it back " + fmt3(cpRiseKnock) + " blocks a tick (want over 0.1)");
                    cpClientFireFall = false; cpClientFireLate = false; cpFireId = r.mob.getId(); // m4: the client watches its fire too
                    fall(b, r); r.mob.setFireTicks(25); // Task 7 fix 2 (N3): it falls burning, its fire to end 25 ticks on
                    next(b, 125);
                }
                case 125 -> {
                    if (since(b) < 30) return;
                    var r = cpRevenant.mob;
                    cpCheck(!r.isRemoved() && r.isDead() && r.deathTime == 30 && CP_DEATHS.getOrDefault(r.getId(), 0) == 1, "struck down again it lies: in the world " + !r.isRemoved() + ", death ticks " + r.deathTime + " (want 30), AFTER_DEATH " + CP_DEATHS.getOrDefault(r.getId(), 0) + " (want 1)");
                    cpCheck(r.getFireTicks() <= 0 && !r.isOnFire(), "it fell burning with 25 fire ticks; on death tick 30 fire ticks " + r.getFireTicks() + ", on fire " + r.isOnFire() + " (want out: the fire ends as it would, not frozen while it lies)");
                    // Task 7 fix 1 (M3): a body the battle has held past its removal tick (its own clock stalled) is swept by the battle tick
                    cpSwept = b.corpses.containsKey(r);
                    b.corpses.put(r, b.time - BattleMath.corpseGoneTick(Spec.COMBATDISCIPLINE[0].corpseLieTicks(), Spec.COMBATDISCIPLINE[0].corpseFadeTicks()) - 30);
                    next(b, 126);
                }
                case 126 -> {
                    if (since(b) < 21) return;
                    var r = cpRevenant.mob;
                    cpFireId = -1; // m4: the client saw it burning as it fell (the flag reaches the client) and not burning once its fire was out (death tick 35 to its sweep, past 50)
                    cpCheck(cpClientFireFall && !cpClientFireLate, "on the client the burning body was drawn on fire as it fell (death ticks 1-15) " + cpClientFireFall + " (want true), and still on fire from death tick 35 " + cpClientFireLate + " (want false: its fire burned out, the flag cleared, not frozen on as it lies)");
                    cpCheck(cpSwept && r.isRemoved(), "the battle holds the body (Battle.corpses)=" + cpSwept + "; stamped past its removal tick it is swept within 20 ticks: removed " + r.isRemoved());
                    // Task 7 fix 2 (N7, R33): a Squire falls where the commander looks; once it lies, Grave Bloom cast there raises a Risen and takes the body
                    var p = sp(b); Vec3d aim = Commander.GraveBloom.aim(p, Commander.byActive("grave_bloom").range());
                    b.deaths.clear(); cpBloomBody = cpOwn(b, 64, ground(b, aim.x, aim.z)); fall(b, cpBloomBody); cpBloomAt = cpBloomBody.mob.getPos();
                    next(b, 127);
                }
                case 127 -> {
                    if (since(b) < 30) return;
                    var body = cpBloomBody.mob; var p = sp(b);
                    boolean lay = body.isDead() && !body.isRemoved() && body.deathTime > BattleMath.DEATH_FALL_TICKS;
                    long risen0 = b.units.stream().filter(u -> u.temporary && u.squad == own).count();
                    boolean cast = new Commander.GraveBloom().start(b, p, own, Commander.byActive("grave_bloom"));
                    var risen = b.units.stream().filter(u -> u.temporary && u.squad == own).toList();
                    boolean there = risen.stream().anyMatch(u -> u.mob.isAlive() && u.mob.getPos().distanceTo(cpBloomAt) < 1.5);
                    var team = body.getScoreboardTeam();
                    cpCheck(lay && cast && body.isRemoved() && (team == null || !team.getName().startsWith("gg_")) && risen.size() == risen0 + 1 && there,
                        "Grave Bloom on a body lying (death tick " + body.deathTime + ", lying " + lay + "): cast " + cast + ", the body removed " + body.isRemoved() + ", off its side " + (team == null || !team.getName().startsWith("gg_")) + ", Risen " + risen0 + " -> " + risen.size() + " (want one more), one standing where it lay " + there);
                    for (var u : risen) {u.mob.discard(); b.units.remove(u);}
                    Houses.recompute(own);
                    clearFixture(b, "hl-corpse"); b.clearCorpses(); cpRow = Spec.COMBATDISCIPLINE[0]; cpRun = 0;
                    own.order = "hold"; dusk.order = "hold";
                    next(b, 128);
                }
                case 128 -> { // Task 8 fix 1 (R39): a settled body the client draws from the buffers it recorded looks as the normal draw does
                    long n = since(b);
                    if (n == 1) {cpShotDone = false; cpFadeDone = false; cpFadeK = 0; cpFadeId = -1; cpCacheBody = cpOwn(b, 65, cpHome); var at = ground(b, cpHome.x, cpHome.z - 3); b.relocate(sp(b), at.x, at.y, at.z, 0f, 28f); return;}
                    // R43 / Task 13 carry 20: it falls once the client has it. The old animator settled a troop struck down within ~3 s of spawning
                    // only near its 60th tick of age (r2-lie20b), so this stood 60 ticks; TroopMotion.settled has no age in it (mo-settle: 1 and 197 ticks old both settle at death tick 22)
                    if (n == CP_STAND) {fall(b, cpCacheBody); return;}
                    if (n == CP_STAND + Math.min(45, cpLieEnd() - 4)) cpShotId = cpCacheBody.mob.getId(); // lying since death tick 21, before its fade; the camera 3 blocks north of it, looking down at it
                    if (cpShotDone && cpShotId >= 0) {cpShotId = -1; cpFadeId = cpCacheBody.mob.getId();} // R42: then the client samples it as it fades
                    if (!(cpShotDone && cpFadeDone) && n < CP_STAND + 220) return; // removed on its gone tick (120 at most here) (the client pauses the game while it shoots, so the body waits)
                    cpShotId = -1; cpFadeId = -1;
                    cpCheck(cpShotDone && cpShotPass, cpShotDone ? cpShotWhy : "the client never shot the cached body (by +" + (CP_STAND + 220) + "; BodyCache: " + BodyCache.diag() + ")");
                    cpCheck(cpFadeDone && cpFadePass, cpFadeDone ? cpFadeWhy : "the client never sampled the body's fade (by +" + (CP_STAND + 220) + "; BodyCache: " + BodyCache.diag() + ")");
                    var p = sp(b); b.relocate(p, patch[0] + 12, Phase1Checks.top(b, patch[0] + 12, patch[2] - 24) + 6, patch[2] - 24, 0f, 15f); // the camera back where hl-corpse-load measures from
                    clearFixture(b, "hl-corpse"); b.clearCorpses();
                    next(b, CP_LOAD ? 129 : 140);
                }
                case 129 -> {if (since(b) >= 40) {BodyCache.noGroups = CP_NOGROUPS; System.out.println("[p2] hl-corpse-load fade overlap groups " + (CP_NOGROUPS ? "off (GRAVES_FADE_NOGROUPS=1)" : "on")); cpBegin(b);}} // the client has the chunks round the camera again
                case 130 -> { // hl-corpse-load: the corpse rows of this run, then 200 troops, 20 a tick, both armies on Attack toward the middle; the wave begins as they meet
                    long n = since(b); // the first tick of a stage is since 1
                    if (n == 1) {
                        var mid = new Vec3d(patch[0] + 16, patch[1], patch[2]);
                        own.order = "attack"; own.target = mid; dusk.order = "attack"; dusk.target = mid;
                        for (var s : new GravesAndGlory.Squad[]{own, dusk}) {s.assignments.clear(); s.arrived.clear(); s.shotAt.clear(); s.chargedAt.clear();}
                    }
                    if (n >= 1 && n <= 10) {
                        for (int j = (int) (n - 1) * 20; j < n * 20; j++) {
                            int k = j / 2; boolean mine = j % 2 == 0;
                            double x = patch[0] + (mine ? 1 : 21) + (k / 10) * 1.2, z = patch[2] + (k % 10 - 4.5) * 1.2;
                            var u = tough(b.spawn(mine ? own : dusk, Campaign.troop("knight"), k, ground(b, x, z))); // AI on; 4000 health, so the scripted falls set the pace, not the blows
                            fixture.add(u); cpLoad.add(u);
                        }
                        return;
                    }
                    if (n < 71) return;
                    cpKilled = 0; cpSamples = 0; cpServerSum = 0; cpPeakSum = 0; cpPeakFrame = 0; cpLying = 0; Phase2Checks.RenderCost.start(); cpWall0 = System.nanoTime();
                    next(b, 131); corpseStages(b); // the first death on this tick
                }
                case 131 -> { // one death a tick for 200 ticks; measure for 340
                    long n = since(b);
                    if (n > 0) {long[] t = b.server.lastTickLengths; double ms = t[(b.server.getTicks() + 99) % 100] / 1e6; cpServerSum += ms; cpSamples++; if (n > CP_PEAK_FROM && n <= CP_PEAK_FROM + CP_PEAK_TICKS) cpPeakSum += ms;}
                    if (n == CP_PEAK_FROM) {cpPeakWall0 = System.nanoTime(); cpPeakFrames0 = Phase2Checks.RenderCost.frames;}
                    if (n == CP_PEAK_FROM + CP_PEAK_TICKS) cpPeakFrame = (System.nanoTime() - cpPeakWall0) / 1e6 / Math.max(1, Phase2Checks.RenderCost.frames - cpPeakFrames0);
                    if (cpKilled < cpLoad.size()) fall(b, cpLoad.get(cpKilled++));
                    if (n % 10 == 0) {int lying = 0; for (var u : cpLoad) if (u.mob.isDead() && !u.mob.isRemoved()) lying++; cpLying = Math.max(cpLying, lying);}
                    if (n < 340) return;
                    Phase2Checks.RenderCost.on = false;
                    long frames = Phase2Checks.RenderCost.frames; int arm = CP_PLAN[cpRun]; int[] run = cpArm(arm);
                    double server = cpServerSum / Math.max(1, cpSamples), client = Phase2Checks.RenderCost.sumNs / 1e6 / Math.max(1, frames), worst = Phase2Checks.RenderCost.worstNs / 1e6;
                    double frame = (System.nanoTime() - cpWall0) / 1e6 / Math.max(1, frames); // the whole frame, wall clock over the frames drawn (the frame rate is capped by the run's options.txt, logged with the report: vsync off, maxFps 260 when measuring)
                    boolean allDead = cpLoad.stream().noneMatch(u -> u.mob.isAlive());
                    double peakServer = cpPeakSum / CP_PEAK_TICKS;
                    cpRuns.add(new double[]{run[0], run[1], server, client, frames, worst, cpLying, arm == 0 ? 0 : 1, allDead ? 1 : 0, cpSamples, frame, arm, peakServer, cpPeakFrame});
                    System.out.println(String.format(Locale.ROOT, "[p2] hl-corpse-load run %d (%s, lie %d fade %d): %d troops, %d fell, all dead %b; server tick %.3f ms mean over %d ticks; client frame %.3f ms mean (wall clock / frames); client world render %.3f ms mean, %.3f ms worst over %d frames; at most %d bodies lying; peak window (ticks %d-%d) server %.3f ms, frame %.3f ms",
                        cpRun, arm == 0 ? "warm-up" : arm == 1 ? "A" : "B", run[0], run[1], cpLoad.size(), cpKilled, allDead, server, cpSamples, frame, client, worst, frames, cpLying, CP_PEAK_FROM + 1, CP_PEAK_FROM + CP_PEAK_TICKS, peakServer, cpPeakFrame));
                    b.clearCorpses(); clearFixture(b, "hl-corpse-load"); cpLoad.clear();
                    next(b, 132);
                }
                case 132 -> {
                    if (since(b) < 40) return;
                    if (++cpRun < CP_PLAN.length) {cpBegin(b); return;}
                    Spec.COMBATDISCIPLINE[0] = cpRow; own.order = "hold"; dusk.order = "hold";
                    int[] sheet = cpArm(2); int gone = BattleMath.corpseGoneTick(sheet[0], sheet[1]), want = Math.min(100, gone - 10);
                    // each A B B A block's ratio is its two B runs over its two A runs; the gate is the median of the blocks' ratios, the spread their largest less their smallest
                    List<double[]> blocks = new ArrayList<>(); // {server, frame, world render, peak server, peak frame}, each B/A
                    for (int k = 1; k + 3 < cpRuns.size(); k += 4) {
                        double[] r0 = cpRuns.get(k), r1 = cpRuns.get(k + 1), r2 = cpRuns.get(k + 2), r3 = cpRuns.get(k + 3);
                        blocks.add(new double[]{(r1[2] + r2[2]) / (r0[2] + r3[2]), (r1[10] + r2[10]) / (r0[10] + r3[10]), (r1[3] + r2[3]) / (r0[3] + r3[3]), (r1[12] + r2[12]) / (r0[12] + r3[12]), (r1[13] + r2[13]) / (r0[13] + r3[13])});
                    }
                    double rs = cpMedian(blocks, 0), rf = cpMedian(blocks, 1), rc = cpMedian(blocks, 2), ps = cpMedian(blocks, 3), pf = cpMedian(blocks, 4);
                    boolean dead = cpRuns.stream().allMatch(r -> r[8] == 1), lay = cpRuns.stream().filter(r -> r[11] == 2).allMatch(r -> r[6] >= want);
                    StringBuilder raw = new StringBuilder();
                    for (int i = 0; i < cpRuns.size(); i++) {var r = cpRuns.get(i); raw.append(String.format(Locale.ROOT, "%s%d %s(lie %d fade %d): server %.3f ms, frame %.3f ms, world render %.3f ms (worst %.3f) over %d frames, %d lying at most, peak server %.3f frame %.3f", i == 0 ? "" : "; ", i, r[11] == 0 ? "warm-up " : r[11] == 1 ? "A " : "B ", (int) r[0], (int) r[1], r[2], r[10], r[3], r[5], (int) r[4], (int) r[6], r[12], r[13]));}
                    StringBuilder per = new StringBuilder();
                    for (int i = 0; i < blocks.size(); i++) {var k = blocks.get(i); per.append(String.format(Locale.ROOT, "%sblock %d server %.3f frame %.3f world render %.3f peak server %.3f peak frame %.3f", i == 0 ? "" : "; ", i + 1, k[0], k[1], k[2], k[3], k[4]));}
                    boolean measured = blocks.size() >= 3 && blocks.stream().allMatch(k -> k[0] > 0 && k[1] > 0);
                    BodyCache.noGroups = false;
                    report("hl-corpse-load", dead && lay && measured && rs <= 1.10 && rf <= 1.15, String.format(Locale.ROOT,
                        "dry ground only (the patch: no water, no fire, so every body settles; a body in water or burning keeps ticking and is not measured here); the client's vsync %b, maxFps %d (the run's options.txt); "
                        + "200 Squires (100 a side, AI on, both on Attack into each other, 4000 health) on the patch, one falling a tick, all dead within 10 s=%b; the bodies lay (B, lie %d fade %d: %d or more at once)=%b; "
                        + "%d A B B A blocks after a warm-up, System.gc() before every run: median server tick B/A %.3f, spread %.3f (want median 1.10 or less); median client frame B/A %.3f, spread %.3f (want median 1.15 or less); "
                        + "for reference the world render alone median B/A %.3f (spread %.3f), and over the peak window (ticks %d-%d of the wave) server %.3f, frame %.3f; blocks: %s; runs: %s; body cache: %s",
                        MinecraftClient.getInstance().options.getEnableVsync().getValue(), MinecraftClient.getInstance().options.getMaxFps().getValue(), dead, sheet[0], sheet[1], want, lay, blocks.size(), rs, cpSpread(blocks, 0), rf, cpSpread(blocks, 1), rc, cpSpread(blocks, 2), CP_PEAK_FROM + 1, CP_PEAK_FROM + CP_PEAK_TICKS, ps, pf, per, raw, BodyCache.diag()));
                    next(b, 140);
                }
                case 140 -> { // a round's end takes a lying body at once
                    if (since(b) == 1) {cpVictim = cpOwn(b, 60, cpHome); return;}
                    if (since(b) == 6) {fall(b, cpVictim); return;}
                    if (since(b) < 36) return;
                    var v = cpVictim.mob; boolean lying = !v.isRemoved() && v.isDead();
                    b.campaign.returnLobby(); fixture.clear(); pins.clear();
                    int left = 0; for (var e : b.world.iterateEntities()) if (ArmyMobs.isTroop(e) && e instanceof net.minecraft.entity.mob.MobEntity m && m.isDead() && !e.isRemoved()) left++;
                    cpCheck(lying && v.isRemoved() && left == 0, "a Squire's body 30 ticks dead (lying " + lying + ") is gone the moment the round ends (returnLobby): removed " + v.isRemoved() + ", " + left + " dead troops left in the world (want none)");
                    cpFinish(b);
                }
                default -> {}
            }
        }
        /** hl-corpse-load: this run's corpse rows, then its troops (stage 130). */
        void cpBegin(GravesAndGlory.Battle b) {int[] run = cpArm(CP_PLAN[cpRun]); Spec.COMBATDISCIPLINE[0] = corpseRow(cpRow, run[0], run[1]); cpLoad.clear(); System.gc(); next(b, 130);}
        /** An arm's {corpseLieTicks, corpseFadeTicks}: A none, the warm-up and B the sheet's rows. */
        /** R43: the corpse rows' last lying death tick before the fade (20 + lie - 1) and the removal tick (20 + lie + fade). */
        static final int CP_STAND = 5;
        static int cpLieEnd() {return BattleMath.DEATH_FALL_TICKS + Spec.COMBATDISCIPLINE[0].corpseLieTicks() - 1;}
        static int cpGone() {return BattleMath.corpseGoneTick(Spec.COMBATDISCIPLINE[0].corpseLieTicks(), Spec.COMBATDISCIPLINE[0].corpseFadeTicks());}
        int[] cpArm(int arm) {return arm == 1 ? new int[]{0, 0} : new int[]{cpRow.corpseLieTicks(), cpRow.corpseFadeTicks()};}
        /** The median of column c of the blocks (the mean of the middle two for an even count), 0 for none. */
        static double cpMedian(List<double[]> blocks, int c) {
            double[] v = blocks.stream().mapToDouble(k -> k[c]).sorted().toArray(); int n = v.length;
            return n == 0 ? 0 : n % 2 == 1 ? v[n / 2] : (v[n / 2 - 1] + v[n / 2]) / 2;
        }
        /** The largest less the smallest of column c of the blocks. */
        static double cpSpread(List<double[]> blocks, int c) {
            return blocks.isEmpty() ? 0 : blocks.stream().mapToDouble(k -> k[c]).max().getAsDouble() - blocks.stream().mapToDouble(k -> k[c]).min().getAsDouble();
        }
        /** A frozen Squire of the commander's army at 4000 health pinned at this point, facing east. */
        GravesAndGlory.Unit cpOwn(GravesAndGlory.Battle b, int slot, Vec3d at) {
            var u = tough(b.spawn(own, Campaign.troop("knight"), slot, at)); Phase1Checks.freeze(u);
            fixture.add(u); pins.add(new Pin(u, at, -90f)); u.deployed = at; Phase2Checks.Engine.apply(u, at, -90f); return u;
        }
        /**
         * R39, on the client: with the game paused (no menu drawn), the body drawn from its buffers (A), drawn the normal way (B), drawn the normal
         * way with the camera nudged 0.001 blocks along its look (B2) and not drawn (C, its shadow still drawn); the body is every pixel where B or A
         * differs from C. R44: the pixels where B and B2 differ by more than 2 of 255 are z-fight-unstable (coplanar faces whose winner moves with
         * the camera) and are left out, at most 2% of the body; A must match B on all but 0.5% of the rest.
         */
        void cacheShots(MinecraftClient c, int id) {
            if (id < 0) {cacheShotsEnd(c); return;} // the server gave up
            var e = c.world == null ? null : c.world.getEntityById(id);
            switch (cpShotPhase) {
                case 0 -> {
                    if (e instanceof net.minecraft.entity.LivingEntity l0 && cpShotAsked < 0) cpShotAsked = l0.deathTime;
                    if (!(e instanceof net.minecraft.entity.LivingEntity l) || !l.isDead() || !BodyCache.cached(e)) return;
                    cpShotAt = l.deathTime;
                    c.options.hudHidden = true; c.getToastManager().clear(); EventFeed.clear(); c.setScreen(new net.minecraft.client.gui.screen.GameMenuScreen(false)); cpShotPhase = 1; cpShotWait = 0; cpShotDraws = BodyCache.cachedDraws;
                }
                case 1 -> {
                    if (++cpShotWait < 6) return;
                    cpShotPaused = c.isPaused(); cpShotA = ScreenshotRecorder.takeScreenshot(c.getFramebuffer()); cpShotEngaged = BodyCache.cachedDraws > cpShotDraws;
                    BodyCache.off = true; cpShotPhase = 2; cpShotWait = 0;
                }
                case 2 -> {
                    if (cpShotWait++ == 1) cpShotDraws = BodyCache.cachedDraws; // a frame on, every draw since is the normal one
                    if (cpShotWait < 6) return;
                    cpShotB = ScreenshotRecorder.takeScreenshot(c.getFramebuffer()); cpShotNormal = BodyCache.cachedDraws == cpShotDraws;
                    cpNudge(c, true); cpShotPhase = 3; cpShotWait = 0;
                }
                case 3 -> {
                    if (++cpShotWait < 6) return;
                    cpShotB2 = ScreenshotRecorder.takeScreenshot(c.getFramebuffer()); cpShotNormal &= BodyCache.cachedDraws == cpShotDraws;
                    cpNudge(c, false); BodyCache.hide = id; cpShotPhase = 4; cpShotWait = 0;
                }
                default -> {
                    if (++cpShotWait < 6) return;
                    var a = cpShotA; var bb = cpShotB; var b2 = cpShotB2; var cc = ScreenshotRecorder.takeScreenshot(c.getFramebuffer()); cpShotPaused &= c.isPaused();
                    int w = a.getWidth(), h = a.getHeight(), body = 0, unstable = 0, kept = 0, any = 0, diff = 0, loose = 0, worst = 0, diffUnstable = 0;
                    boolean sized = bb.getWidth() == w && cc.getWidth() == w && b2.getWidth() == w && bb.getHeight() == h && cc.getHeight() == h && b2.getHeight() == h;
                    if (sized) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                        int pa = a.getColor(x, y), pb = bb.getColor(x, y), pc = cc.getColor(x, y);
                        if (pa == pc && pb == pc) continue;
                        body++; int d = cpChannel(pa, pb);
                        if (cpChannel(pb, b2.getColor(x, y)) > 2) {unstable++; if (d > 2) diffUnstable++; continue;}
                        kept++;
                        if (d > 0) any++; if (d > 2) diff++; if (d > 8) loose++; worst = Math.max(worst, d);
                    }
                    var dir = new java.io.File(c.runDirectory, "screenshots"); dir.mkdirs();
                    try {a.writeTo(new java.io.File(dir, "hl-corpse-cache-a.png")); bb.writeTo(new java.io.File(dir, "hl-corpse-cache-b.png")); b2.writeTo(new java.io.File(dir, "hl-corpse-cache-b2.png")); cc.writeTo(new java.io.File(dir, "hl-corpse-cache-c.png"));} catch (java.io.IOException ex) {ex.printStackTrace();}
                    a.close(); bb.close(); b2.close(); cc.close(); cpShotA = null; cpShotB = null; cpShotB2 = null;
                    double share = kept == 0 ? 1 : diff / (double) kept, out = body == 0 ? 1 : unstable / (double) body;
                    cpShotPass = sized && cpShotPaused && cpShotEngaged && cpShotNormal && body >= 1000 && out <= 0.02 && share <= 0.005;
                    cpShotWhy = String.format(Locale.ROOT, "a settled Squire's body 3 blocks from the camera (asked for on its death tick %d, drawn from its buffers and shot from %d), the game paused %b: drawn from its recorded buffers %b (BodyCache drew it), then the normal way %b (no cached draw, twice: the second with the camera nudged 0.001 blocks along its look); of its %d pixels (where either draw differs from the frame without it, so its shadow is left out) %d are z-fight-unstable (the two normal draws differ by more than 2 of 255), %.3f%% (want 2%% or less; %d of them differ cached against normal); of the other %d, %d differ by more than 2 of 255, %.3f%% (want 0.5%% or less; the cached body lights its byte-packed normals in world space, not view space, which moves shading by 1 or 2: %d differ at all, %d by more than 8, the worst by %d); screenshots hl-corpse-cache-a/b/b2/c.png",
                        cpShotAsked, cpShotAt, cpShotPaused, cpShotEngaged, cpShotNormal, body, unstable, out * 100, diffUnstable, kept, diff, share * 100, any, loose, worst);
                    System.out.println("[p2] hl-corpse cache: " + cpShotWhy + " (BodyCache: " + BodyCache.diag() + ")");
                    cacheShotsEnd(c); cpShotDone = true;
                }
            }
        }
        void cacheShotsEnd(MinecraftClient c) {
            if (cpShotPhase == 3) cpNudge(c, false);
            if (cpShotPhase > 0) {c.setScreen(null); c.options.hudHidden = false;}
            BodyCache.off = false; BodyCache.hide = -1; cpShotPhase = 0;
            if (cpShotA != null) {cpShotA.close(); cpShotA = null;} if (cpShotB != null) {cpShotB.close(); cpShotB = null;} if (cpShotB2 != null) {cpShotB2.close(); cpShotB2 = null;}
        }
        /** The largest channel difference of two ABGR pixels. */
        static int cpChannel(int p, int q) {int d = 0; for (int k = 0; k < 24; k += 8) d = Math.max(d, Math.abs((p >> k & 255) - (q >> k & 255))); return d;}
        /** The sum of every channel's difference of two frames of one size. */
        static long cpSum(net.minecraft.client.texture.NativeImage p, net.minecraft.client.texture.NativeImage q) {
            long s = 0; int w = Math.min(p.getWidth(), q.getWidth()), h = Math.min(p.getHeight(), q.getHeight());
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {int a = p.getColor(x, y), b = q.getColor(x, y); if (a != b) for (int k = 0; k < 24; k += 8) s += Math.abs((a >> k & 255) - (b >> k & 255));}
            return s;
        }
        /** R44: moves the camera (the paused player, so where the camera lerps it from too) 0.001 blocks along its look, or back where it was. */
        void cpNudge(MinecraftClient c, boolean out) {
            var p = c.player; if (p == null) return; double[] s = cpNudged;
            if (out) {
                s[0] = p.getX(); s[1] = p.getY(); s[2] = p.getZ(); s[3] = p.prevX; s[4] = p.prevY; s[5] = p.prevZ; s[6] = p.lastRenderX; s[7] = p.lastRenderY; s[8] = p.lastRenderZ;
                Vec3d d = p.getRotationVec(1f).multiply(0.001);
                p.setPos(s[0] + d.x, s[1] + d.y, s[2] + d.z); p.prevX = s[3] + d.x; p.prevY = s[4] + d.y; p.prevZ = s[5] + d.z; p.lastRenderX = s[6] + d.x; p.lastRenderY = s[7] + d.y; p.lastRenderZ = s[8] + d.z;
            } else {p.setPos(s[0], s[1], s[2]); p.prevX = s[3]; p.prevY = s[4]; p.prevZ = s[5]; p.lastRenderX = s[6]; p.lastRenderY = s[7]; p.lastRenderZ = s[8];}
        }
        /**
         * R42, on the client: the body as it fades, sampled on its fade's first death tick, its middle and its last (20 + lie, + fade / 2, + fade - 1:
         * 80, 100 and 119 on the sheet), the game paused each time: drawn as it is (F, from its recorded buffers), drawn the normal way (N), drawn
         * opaque (O, BodyCache.opaque) and not drawn (H). Over a fixed background a body drawn at alpha a differs from H by a times what it
         * differs opaque, so a = the summed colour difference of F (or N) from H over O's. They must fall monotonically, about 1, 0.5 and 0
         * (each within 0.08 of what the death ticks and the frame's tick delta give), and F and N within 0.05 of each other (no pop between
         * the cached draw and the normal one).
         */
        void fadeShots(MinecraftClient c, int id) {
            if (id < 0) {fadeShotsEnd(c); return;} // the server gave up
            var e = c.world == null ? null : c.world.getEntityById(id);
            var row = Spec.COMBATDISCIPLINE[0]; int start = BattleMath.DEATH_FALL_TICKS + row.corpseLieTicks(), fade = Math.max(1, row.corpseFadeTicks());
            int[] at = {start, start + fade / 2, start + fade - 1};
            if (!(e instanceof net.minecraft.entity.LivingEntity l) || !l.isDead()) {
                cpFadeWhy = "the client lost the fading body before its sample on death tick " + at[cpFadeK] + " (" + cpFadeK + " of 3 taken)"; cpFadePass = false; fadeShotsEnd(c); cpFadeDone = true; return;
            }
            switch (cpFadePhase) {
                case 0 -> {
                    if (l.deathTime < at[cpFadeK]) return;
                    if (cpFadeK == 0) {cpFadeCached = true; cpFadePaused = true;}
                    cpFadeV[cpFadeK][3] = l.deathTime;
                    c.options.hudHidden = true; c.getToastManager().clear(); EventFeed.clear(); c.setScreen(new net.minecraft.client.gui.screen.GameMenuScreen(false)); cpFadePhase = 1; cpFadeWait = 0; cpFadeDraws = BodyCache.cachedDraws;
                }
                case 1 -> {
                    if (++cpFadeWait < 6) return;
                    cpFadePaused &= c.isPaused(); cpFadeCached &= BodyCache.cachedDraws > cpFadeDraws;
                    cpFadeV[cpFadeK][2] = BattleMath.corpseAlpha(l.deathTime + c.getTickDelta(), row.corpseLieTicks(), row.corpseFadeTicks());
                    cpFadeF = ScreenshotRecorder.takeScreenshot(c.getFramebuffer());
                    BodyCache.off = true; cpFadePhase = 2; cpFadeWait = 0;
                }
                case 2 -> {
                    if (++cpFadeWait < 6) return;
                    cpFadeN = ScreenshotRecorder.takeScreenshot(c.getFramebuffer());
                    BodyCache.off = false; BodyCache.opaque = true; cpFadePhase = 3; cpFadeWait = 0;
                }
                case 3 -> {
                    if (++cpFadeWait < 6) return;
                    cpFadeO = ScreenshotRecorder.takeScreenshot(c.getFramebuffer());
                    BodyCache.opaque = false; BodyCache.hide = id; cpFadePhase = 4; cpFadeWait = 0;
                }
                default -> {
                    if (++cpFadeWait < 6) return;
                    var hh = ScreenshotRecorder.takeScreenshot(c.getFramebuffer()); cpFadePaused &= c.isPaused();
                    double o = cpSum(cpFadeO, hh); var v = cpFadeV[cpFadeK];
                    v[0] = o <= 0 ? -1 : cpSum(cpFadeF, hh) / o; v[1] = o <= 0 ? -1 : cpSum(cpFadeN, hh) / o; v[4] = o;
                    var dir = new java.io.File(c.runDirectory, "screenshots"); dir.mkdirs(); String n = "hl-corpse-fade-" + (int) v[3];
                    try {cpFadeF.writeTo(new java.io.File(dir, n + "-f.png")); cpFadeN.writeTo(new java.io.File(dir, n + "-n.png")); cpFadeO.writeTo(new java.io.File(dir, n + "-o.png")); hh.writeTo(new java.io.File(dir, n + "-h.png"));} catch (java.io.IOException ex) {ex.printStackTrace();}
                    hh.close(); fadeShotsEnd(c);
                    if (++cpFadeK < 3) return;
                    double[] f = {cpFadeV[0][0], cpFadeV[1][0], cpFadeV[2][0]}, nn = {cpFadeV[0][1], cpFadeV[1][1], cpFadeV[2][1]}, x = {cpFadeV[0][2], cpFadeV[1][2], cpFadeV[2][2]};
                    boolean mono = f[0] > f[1] && f[1] > f[2] && nn[0] > nn[1] && nn[1] > nn[2];
                    boolean ends = f[0] >= 0.9 && nn[0] >= 0.9 && f[1] >= 0.35 && f[1] <= 0.65 && nn[1] >= 0.35 && nn[1] <= 0.65 && f[2] <= 0.1 && nn[2] <= 0.1;
                    boolean near = true, seen = true; for (int k = 0; k < 3; k++) {near &= Math.abs(f[k] - x[k]) <= 0.08 && Math.abs(nn[k] - x[k]) <= 0.08 && Math.abs(f[k] - nn[k]) <= 0.05; seen &= cpFadeV[k][4] > 1000;}
                    cpFadePass = cpFadePaused && cpFadeCached && seen && mono && ends && near;
                    StringBuilder s = new StringBuilder();
                    for (int k = 0; k < 3; k++) s.append(String.format(Locale.ROOT, "%sdeath tick %d: from its buffers %.3f, the normal way %.3f, want %.3f (opaque differs by %.0f)", k == 0 ? "" : "; ", (int) cpFadeV[k][3], f[k], nn[k], x[k], cpFadeV[k][4]));
                    cpFadeWhy = String.format(Locale.ROOT, "the settled Squire's body fades over its corpse fade (lie %d, fade %d ticks), the game paused for each sample %b, drawn from its buffers at every sample %b; its alpha as drawn (its summed colour difference from the frame without it over the opaque one's): %s (want falling, at least 0.9, 0.35 to 0.65 and at most 0.1, each within 0.08 of the want and the two draws within 0.05 of each other)=%b/%b/%b; screenshots hl-corpse-fade-<death tick>-f/n/o/h.png",
                        row.corpseLieTicks(), row.corpseFadeTicks(), cpFadePaused, cpFadeCached, s, mono, ends, near);
                    System.out.println("[p2] hl-corpse fade: " + cpFadeWhy + " (BodyCache: " + BodyCache.diag() + ")");
                    cpFadeDone = true;
                }
            }
        }
        void fadeShotsEnd(MinecraftClient c) {
            if (cpFadePhase > 0) {c.setScreen(null); c.options.hudHidden = false;}
            BodyCache.off = false; BodyCache.opaque = false; BodyCache.hide = -1; cpFadePhase = 0;
            if (cpFadeF != null) {cpFadeF.close(); cpFadeF = null;} if (cpFadeN != null) {cpFadeN.close(); cpFadeN = null;} if (cpFadeO != null) {cpFadeO.close(); cpFadeO = null;}
        }
        /** A fall with no attacker, past the hurt cooldown. */
        static void fall(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {u.mob.timeUntilRegen = 0; u.mob.hurtTime = 0; u.mob.damage(b.world.getDamageSources().generic(), 10000f);}
        /** The corpse row with these lie and fade ticks, the rest as it was. */
        static Spec.CombatDisciplineRow corpseRow(Spec.CombatDisciplineRow row, int lie, int fade) {
            try {
                var parts = Spec.CombatDisciplineRow.class.getRecordComponents(); Object[] args = new Object[parts.length]; Class<?>[] types = new Class<?>[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    types[i] = parts[i].getType(); String name = parts[i].getName();
                    args[i] = name.equals("corpseLieTicks") ? Integer.valueOf(lie) : name.equals("corpseFadeTicks") ? Integer.valueOf(fade) : parts[i].getAccessor().invoke(row);
                }
                return Spec.CombatDisciplineRow.class.getDeclaredConstructor(types).newInstance(args);
            } catch (ReflectiveOperationException e) {throw new IllegalStateException(e);}
        }
        static String fmt3(double v) {return String.format(Locale.ROOT, "%.3f", v);}
        void cpCheck(boolean ok, String why) {cpPass &= ok; cpWhy.add(why + "=" + ok);}
        void cpFinish(GravesAndGlory.Battle b) {Houses.forceRise = null; if (cpRow != null) Spec.COMBATDISCIPLINE[0] = cpRow; cpClientId = -1; cpGlowId = -1; cpBaitId = -1; report("hl-corpse", cpPass, String.join("; ", cpWhy)); next(b, 99);}
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
