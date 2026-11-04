package gg.gravesandglory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Unshipped. The balance method's fights: runServer -PduelSim=<plan file> plays small fights with no points on a dedicated server with nobody
 * online, one at a time, in one open cell of the -PduelMap map (default 0) that it picks from the built arena (no river, structure, scenery,
 * point or solid block in it, as flat as the map allows), then stops the server.
 *
 * Plan file, one pairing a line ('#' starts a comment): id|lineupA|lineupB|repetitions|options
 *   lineup: troop*count[+troop*count...] (for example revenant*16) or banner:<botBanners id> (one army of that banner, its slot positions kept)
 *   options, ';' separated: traits=banner (default: every House in the lineup at tier III, the tier a pure normal banner reaches) or traits=live
 *   (the tier the lineup's own slots give, as in battle); charge=0 (default: Attack only) or charge=1 (the Normal bot's timed Charge at
 *   botCommand.normal.chargeRatio, counted inside the fight); cap=<seconds> (default 120); distance=<blocks between the two front rows> (default 28).
 * Repetitions are played round by round (every pairing's first, then every pairing's second...), so a run cut short stays balanced. Repetition r
 * puts lineup A on the west side (team 0) when r is even and on the east side (team 1) when r is odd. -PduelShard=i/n plays only the fights whose
 * place in that order is i modulo n, so two processes can share one plan.
 *
 * Each side is a squad outside Battle.squads (so no bot decision, purchase, income, reinforcement or Houses.tick touches it) with commander class
 * champion (whose aura needs a player, so it does nothing here), bot flag set (troops fight at the sheet numbers, no stars) and an empty banner.
 * Every 100 ticks (botCommand.normal.decisionTicks) each side gets the Normal bot's order without the points: Attack (or Charge, if allowed and
 * it has chargeRatio times the enemy's living troops) toward the enemy's living centre, block formation at 8 or more living troops, loose below.
 * A fight ends when a side has no living troop (wipe) or at the cap. Writes duel/duels.csv (one row per fight), duel/types.csv (TypeLedger rows
 * per side) and duel/meta.txt.
 */
public final class DuelSimulation implements ModInitializer {
    record Lineup(String label, List<String> slots) {
        int gold() { int g = 0; for (String id : slots) { var t = Campaign.troop(id); if (t != null) g += t.cost(); } return g; }
        int count() { int n = 0; for (String id : slots) if (Campaign.troop(id) != null) n++; return n; }
        Set<String> houses() { Set<String> h = new LinkedHashSet<>(); for (String id : slots) { var t = Campaign.troop(id); if (t != null) h.add(t.house()); } return h; }
    }
    record Fight(int order, String id, Lineup a, Lineup b, int rep, boolean aWest, boolean forceTraits, boolean charge, int capTicks, double distance) {}

    static final int CELL_LENGTH = 96, CELL_WIDTH = 36, GAP_TICKS = 40, DECISION_TICKS = 100;
    /** Battle code runs on fixed beats of the server clock (Morale and Houses every 20 ticks, a troop's Tactics on its 10-tick beat, unit
     *  chunks every 40), and a fight's result depends on where on those beats it starts. So each fight starts on a phase of this period drawn
     *  from the run's seed and the fight's place in the plan, never on whatever tick the fight before it happened to end. Two runs with the
     *  same seed and plan then start every fight on the same phase (a paired comparison); runs with different seeds draw the phases apart. */
    static final int PHASE_PERIOD = 200;
    long seed;
    final List<Fight> fights = new ArrayList<>();
    int map, next, waited, notReady, recorded, wipes, caps, aborted;
    Path duels, types, meta;
    enum Phase { WAIT, GAP, FIGHT, DONE }
    Phase phase = Phase.WAIT;
    long gapUntil, fightStart, nextDecision, wallStart, runWallStart;
    Fight current;
    GravesAndGlory.Squad west, east;
    final Map<String, Integer> forcedWest = new HashMap<>(), forcedEast = new HashMap<>();
    int startGoldWest, startGoldEast, startCountWest, startCountEast;
    Vec3d centre;
    final TypeLedger ledger = new TypeLedger();
    final Set<Long> forcedChunks = new HashSet<>();

    @Override public void onInitialize() {
        String planPath = System.getProperty("gravesandglory.duelSim");
        if (planPath == null) return;
        map = number("duelMap", System.getProperty("gravesandglory.duelMap", "0"));
        if (map < 0 || map >= Spec.MAPS.length) throw new IllegalArgumentException("[duel] duelMap must be 0 to " + (Spec.MAPS.length - 1));
        int shard = 0, shards = 1;
        String s = System.getProperty("gravesandglory.duelShard");
        if (s != null) { String[] p = s.split("/"); shard = number("duelShard", p[0]); shards = number("duelShard", p[1]); if (shards < 1 || shard < 0 || shard >= shards) throw new IllegalArgumentException("[duel] duelShard must be i/n with 0 <= i < n"); }
        SheetOverride.applyFromProperty();
        String seedText = System.getProperty("gravesandglory.duelSeed");
        seed = seedText != null ? Long.parseLong(seedText.trim()) : (System.nanoTime() & 0xffffffL);
        Path dir = FabricLoader.getInstance().getGameDir().resolve("duel");
        duels = dir.resolve("duels.csv"); types = dir.resolve("types.csv"); meta = dir.resolve("meta.txt");
        List<String> lines;
        try { lines = Files.readAllLines(Path.of(planPath), StandardCharsets.UTF_8); } catch (IOException e) { throw new UncheckedIOException("[duel] cannot read plan " + planPath, e); }
        record Line(String id, Lineup a, Lineup b, int reps, boolean force, boolean charge, int cap, double distance) {}
        List<Line> plan = new ArrayList<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] f = line.split("\\|", -1);
            if (f.length < 4) throw new IllegalArgumentException("[duel] plan line needs id|lineupA|lineupB|repetitions[|options]: " + line);
            boolean force = true, charge = false; int cap = 120; double distance = 28;
            if (f.length > 4) for (String opt : f[4].split(";")) {
                opt = opt.trim(); if (opt.isEmpty()) continue;
                String[] kv = opt.split("=", 2);
                if (kv.length != 2) throw new IllegalArgumentException("[duel] option needs key=value: " + opt);
                switch (kv[0].trim()) {
                    case "traits" -> { if (!kv[1].equals("banner") && !kv[1].equals("live")) throw new IllegalArgumentException("[duel] traits must be banner or live"); force = kv[1].equals("banner"); }
                    case "charge" -> charge = kv[1].trim().equals("1");
                    case "cap" -> cap = number("cap", kv[1]);
                    case "distance" -> distance = Double.parseDouble(kv[1].trim());
                    default -> throw new IllegalArgumentException("[duel] unknown option " + kv[0]);
                }
            }
            plan.add(new Line(f[0].trim(), lineup(f[1].trim()), lineup(f[2].trim()), number("repetitions", f[3]), force, charge, cap, distance));
        }
        int most = plan.stream().mapToInt(Line::reps).max().orElse(0), order = 0;
        for (int r = 0; r < most; r++) for (Line l : plan) if (r < l.reps()) {
            if (order % shards == shard) fights.add(new Fight(order, l.id(), l.a(), l.b(), r, r % 2 == 0, l.force(), l.charge(), l.cap() * 20, l.distance()));
            order++;
        }
        if (fights.isEmpty()) throw new IllegalArgumentException("[duel] the plan holds no fight for this shard");
        try {
            Files.createDirectories(dir);
            Files.writeString(duels, "fight,id,rep,map,aLineup,bLineup,aSide,winner,end,seconds,aStartGold,bStartGold,aStartCount,bStartCount,aEndCount,bEndCount,aLeft,bLeft,margin,aKills,bKills,aDealt,bDealt,wallSeconds,startPhase\n", StandardCharsets.UTF_8);
            Files.writeString(types, "fight,id,rep,role,side,lineup," + TypeLedger.HEADER + "\n", StandardCharsets.UTF_8);
            Files.writeString(meta, "plan=" + planPath + "\nshard=" + shard + "/" + shards + "\nfights=" + fights.size() + "\nmap=" + map + "\noverride=" + SheetOverride.describe() + "\nfast=" + gg.gravesandglory.testing.SimSpeed.FAST + "\nseed=" + seed + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) { throw new UncheckedIOException(e); }
        System.out.println("[duel] plan " + planPath + ": " + fights.size() + " fights (shard " + shard + "/" + shards + "), map " + map + ", override " + SheetOverride.describe() + ", seed " + seed);
        Identifier late = new Identifier("gravesandglory_local_test_tools", "late");
        ServerLivingEntityEvents.AFTER_DEATH.addPhaseOrdering(Event.DEFAULT_PHASE, late);
        ServerLivingEntityEvents.AFTER_DEATH.register(late, (entity, source) -> { ledger.onDeath(GravesAndGlory.battle, entity, source); if (phase == Phase.FIGHT) reforce(); });
        ScratchFlags.install();
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    static Lineup lineup(String text) {
        List<String> slots = new ArrayList<>();
        if (text.startsWith("banner:")) {
            String id = text.substring(7);
            if (Arrays.stream(Spec.BOTBANNERS).noneMatch(r -> r.id().equals(id))) throw new IllegalArgumentException("[duel] no botBanners row " + id);
            slots.addAll(Campaign.bannerRow(id));
        } else for (String part : text.split("\\+")) {
            String[] p = part.trim().split("\\*");
            String id = p[0].trim(); int n = p.length > 1 ? number("count", p[1]) : 1;
            if (Campaign.troop(id) == null) throw new IllegalArgumentException("[duel] no troop " + id);
            for (int i = 0; i < n; i++) slots.add(id);
        }
        return new Lineup(text, slots);
    }
    static int number(String name, String value) {
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { throw new IllegalArgumentException("[duel] " + name + " must be a whole number, got '" + value + "'"); }
    }

    void tick(MinecraftServer server) {
        var b = GravesAndGlory.battle;
        if (phase == Phase.DONE) return;
        if (b == null) { if (++waited == 200) { System.out.println("[duel] no battle on this world (level-name must be Graves and Glory); stopping"); stop(server); } return; }
        switch (phase) {
            case WAIT -> {
                if (!b.ready) { if (++notReady == 6000) { System.out.println("[duel] arena not ready after 6000 ticks; stopping"); stop(server); } return; }
                notReady = 0;
                if (b.campaign.map != map) { b.campaign.switchMap(map); return; }
                if (!b.running) startEmptyRound(b);
                centre = findCell(b);
                if (centre == null) { System.out.println("[duel] no open cell on map " + map + "; stopping"); stop(server); return; }
                forceChunks(b);
                phase = Phase.GAP; gapUntil = startAt(b.time + 140); runWallStart = System.currentTimeMillis();
            }
            case GAP -> {
                if (!keepRound(b)) return;
                if (b.time < gapUntil) return;
                if (next >= fights.size()) { summary(); stop(server); return; }
                startFight(b, fights.get(next++));
                phase = Phase.FIGHT;
            }
            case FIGHT -> {
                if (!b.running) { System.out.println("[duel] the round stopped during fight " + current.order() + "; restarting it"); endFight(b, "aborted"); startEmptyRound(b); phase = Phase.GAP; gapUntil = startAt(b.time + 140); return; }
                keepRound(b);
                ledger.tick(b);
                ScratchFlags.track(b);
                reforce();
                if (b.time >= nextDecision) { decide(b, west, east); decide(b, east, west); nextDecision += DECISION_TICKS; }
                long w = living(b, west).size(), e = living(b, east).size();
                if (w == 0 || e == 0) { endFight(b, "wipe"); phase = Phase.GAP; gapUntil = startAt(b.time + GAP_TICKS); }
                else if (b.time - fightStart >= current.capTicks()) { endFight(b, "cap"); phase = Phase.GAP; gapUntil = startAt(b.time + GAP_TICKS); }
            }
            default -> {}
        }
    }
    void stop(MinecraftServer server) { phase = Phase.DONE; server.stop(false); }
    /** The first tick at or after earliest on the next fight's phase (see PHASE_PERIOD). */
    long startAt(long earliest) {
        if (next >= fights.size()) return earliest;
        int want = new Random(seed * 1_000_003L + fights.get(next).order()).nextInt(PHASE_PERIOD);
        return earliest + Math.floorMod(want - earliest, (long) PHASE_PERIOD);
    }

    /** A round with no army in it: no bots (fill off), no clock that runs out, so the troops tick and nothing scores. */
    void startEmptyRound(GravesAndGlory.Battle b) {
        var c = b.campaign; c.fill = false; c.practice = false; c.size = 1; c.lastWinner = -2;
        b.startRound();
        b.roundSeconds = Integer.MAX_VALUE / 40; // after startRound, which sets the row's clock
        System.out.println("[duel] empty round started at tick " + b.time + "; units " + b.units.size() + ", squads " + b.squads.size());
    }
    /** Nothing may score in a duel round: the scores stay 0, so the round never ends by points. False when the round is not running. */
    boolean keepRound(GravesAndGlory.Battle b) {
        if (!b.running) { startEmptyRound(b); gapUntil = startAt(b.time + 140); return false; }
        b.scores[0] = 0; b.scores[1] = 0;
        return true;
    }

    /** The open cell: CELL_LENGTH along x by CELL_WIDTH along z, away from the river, every point, structure and tree, with nothing solid at head
     *  height, and the smallest ground-height range the map offers. */
    Vec3d findCell(GravesAndGlory.Battle b) {
        record Candidate(int x, int z, int range) {}
        List<Candidate> found = new ArrayList<>();
        int hl = CELL_LENGTH / 2, hw = CELL_WIDTH / 2;
        for (int cx = -300; cx <= 300; cx += 8) for (int cz = -200; cz <= 200; cz += 8) {
            if (Math.abs(cx) + hl > GravesAndGlory.R.arenaHalfX() - 24 || Math.abs(cz) + hw > GravesAndGlory.R.arenaHalfZ() - 24) continue;
            boolean ok = true;
            for (var p : b.points) { double dx = Math.max(0, Math.abs(p.spec.x() - cx) - hl), dz = Math.max(0, Math.abs(p.spec.z() - cz) - hw); if (Math.hypot(dx, dz) < p.spec.radius() + 30) ok = false; }
            for (var s : Spec.STRUCTURES) if (Math.abs(s.x() - cx) < hl + s.width() / 2 + 8 && Math.abs(s.z() - cz) < hw + s.depth() / 2 + 8) ok = false;
            // Trees are cleared from the chosen cell (clearTrees); rocks stay, so a cell must miss them.
            for (var s : Spec.SCENERY) if (!s.kind().equals("tree") && Math.abs(s.x() - cx) < hl + 4 + s.width() && Math.abs(s.z() - cz) < hw + 4 + s.width()) ok = false;
            if (!ok) continue;
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            for (int dx = -hl; dx <= hl && ok; dx += 3) for (int dz = -hw; dz <= hw; dz += 3) {
                int x = cx + dx, z = cz + dz;
                if (Arena.river(x, z) || Math.abs(x - Arena.riverX(z)) < 18) { ok = false; break; }
                int h = Arena.ground(x, z); lo = Math.min(lo, h); hi = Math.max(hi, h);
            }
            if (ok) found.add(new Candidate(cx, cz, hi - lo));
        }
        found.sort(Comparator.comparingInt(Candidate::range).thenComparingInt(c -> Math.abs(c.z())));
        // The flattest cells lie along roads, where MapDressing puts lamp posts and walls, so every candidate is tried in order.
        int blocked = 0;
        for (var c : found) {
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE; boolean ok = true;
            for (int dx = -hl - 4; dx <= hl + 4 && ok; dx++) for (int dz = -hw - 4; dz <= hw + 4; dz++) {
                int x = c.x() + dx, z = c.z() + dz, h = Arena.ground(x, z);
                lo = Math.min(lo, h); hi = Math.max(hi, h);
                for (int y = 1; y <= 2; y++) { var pos = new BlockPos(x, h + y, z); var st = b.world.getBlockState(pos); if (!tree(st) && !st.getCollisionShape(b.world, pos).isEmpty()) {
                    if (++blocked <= 3) System.out.println("[duel] cell " + c.x() + "," + c.z() + " (range " + c.range() + ") blocked by " + st.getBlock() + " at " + x + "," + (h + y) + "," + z);
                    ok = false; break; } }
                if (!ok) break;
            }
            if (!ok) continue;
            System.out.println("[duel] " + blocked + " flatter candidates had a solid block at head height");
            int cleared = clearTrees(b, c.x(), c.z());
            System.out.println("[duel] cell at x=" + c.x() + " z=" + c.z() + " on map " + map + ", ground range " + (hi - lo) + " blocks (" + found.size() + " candidates), " + cleared + " tree blocks cleared");
            BotSimulation.append(meta, "cell=" + c.x() + "," + c.z() + " groundRange=" + (hi - lo) + " treeBlocksCleared=" + cleared + "\n");
            return new Vec3d(c.x(), Arena.ground(c.x(), c.z()) + 1, c.z());
        }
        System.out.println("[duel] " + found.size() + " candidates clear of points, structures, rocks and the river; all " + blocked + " blocked at head height");
        return null;
    }
    static boolean tree(net.minecraft.block.BlockState st) { return st.isIn(net.minecraft.registry.tag.BlockTags.LOGS) || st.isIn(net.minecraft.registry.tag.BlockTags.LEAVES); }
    /** Logs and leaves from the ground to 24 blocks up over the cell and an 8-block margin become air, so the cell is open ground. This
     *  server's world is thrown away after the run; the sheet's scenery list is untouched. */
    int clearTrees(GravesAndGlory.Battle b, int cx, int cz) {
        int n = 0, hl = CELL_LENGTH / 2 + 8, hw = CELL_WIDTH / 2 + 8;
        for (int x = cx - hl; x <= cx + hl; x++) for (int z = cz - hw; z <= cz + hw; z++) {
            int h = Arena.ground(x, z);
            for (int y = h + 1; y <= h + 24; y++) { var pos = new BlockPos(x, y, z); if (tree(b.world.getBlockState(pos))) { b.world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 2 | 16); n++; } }
        }
        return n;
    }
    void forceChunks(GravesAndGlory.Battle b) {
        int hl = CELL_LENGTH / 2 + 24, hw = CELL_WIDTH / 2 + 24;
        for (int x = ((int) centre.x - hl) >> 4; x <= ((int) centre.x + hl) >> 4; x++) for (int z = ((int) centre.z - hw) >> 4; z <= ((int) centre.z + hw) >> 4; z++) {
            b.world.setChunkForced(x, z, true); forcedChunks.add(net.minecraft.util.math.ChunkPos.toLong(x, z));
        }
    }

    void startFight(GravesAndGlory.Battle b, Fight f) {
        current = f;
        forceChunks(b); // Battle.updateUnitChunks may have released a chunk of the cell between fights
        Lineup w = f.aWest() ? f.a() : f.b(), e = f.aWest() ? f.b() : f.a();
        west = squad(0); east = squad(1);
        startGoldWest = spawn(b, west, w, f.distance()); startGoldEast = spawn(b, east, e, f.distance());
        startCountWest = living(b, west).size(); startCountEast = living(b, east).size();
        forcedWest.clear(); forcedEast.clear();
        if (f.forceTraits()) { for (String h : w.houses()) forcedWest.put(h, 3); for (String h : e.houses()) forcedEast.put(h, 3); }
        reforce();
        ledger.reset(b.time);
        fightStart = b.time; nextDecision = b.time; wallStart = System.currentTimeMillis();
    }
    GravesAndGlory.Squad squad(int team) {
        var s = new GravesAndGlory.Squad(UUID.randomUUID(), team, centre);
        s.bot = true; s.seated = true; s.tier = "normal"; s.banner = new ArrayList<>();
        s.order = "attack"; s.formation = "block"; s.target = centre; s.yaw = team == 0 ? -90 : 90;
        return s;
    }
    /** Spawns a lineup in rows of five like Battle.reinforce, front row distance/2 from the cell centre, each troop at its slot position. Returns its gold. */
    int spawn(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Lineup l, double distance) {
        double side = s.team == 0 ? -1 : 1; int position = 0, gold = 0;
        for (String id : l.slots()) {
            int p = position++;
            var spec = Campaign.troop(id);
            if (spec == null) continue;
            double x = centre.x + side * (distance / 2 + (p / 5) * 3), z = centre.z - side * ((p % 5 - 2) * 2.5);
            b.spawn(s, spec, p, new Vec3d(x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z));
            gold += spec.cost();
        }
        return gold;
    }
    /** traits=banner: each House of the lineup at tier III whatever the fight leaves alive (Houses.recompute runs on every spawn and death). */
    void reforce() {
        if (west != null && !forcedWest.isEmpty()) { west.traits.clear(); west.traits.putAll(forcedWest); }
        if (east != null && !forcedEast.isEmpty()) { east.traits.clear(); east.traits.putAll(forcedEast); }
    }
    static List<GravesAndGlory.Unit> living(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        List<GravesAndGlory.Unit> out = new ArrayList<>();
        for (var u : b.units) if (u.squad == s && !u.temporary && u.mob.isAlive()) out.add(u);
        return out;
    }
    /** The Normal bot's order without the points (Campaign.botDecide): Attack, or a timed Charge when allowed, toward the enemy's living centre. */
    void decide(GravesAndGlory.Battle b, GravesAndGlory.Squad s, GravesAndGlory.Squad enemy) {
        var mine = living(b, s); var foes = living(b, enemy);
        if (mine.isEmpty() || foes.isEmpty()) return;
        Vec3d c = Morale.centre(foes);
        Vec3d target = new Vec3d(c.x, Arena.ground((int) Math.floor(c.x), (int) Math.floor(c.z)) + 1, c.z);
        String was = s.order; Vec3d wasTarget = s.target; boolean couldCharge = b.startsCharge(s, "all");
        var tier = b.campaign.tier(s);
        s.assignments.clear();
        s.order = current.charge() && BattleMath.timedCharge(0, mine.size(), foes.size(), tier.chargeRatio(), 0) ? "charge" : "attack";
        s.formation = mine.size() < 8 ? "loose" : "block";
        s.target = target; s.yaw = s.team == 0 ? -90 : 90;
        if (!s.order.equals(was) || !Objects.equals(s.target, wasTarget)) s.arrived.clear();
        if (s.order.equals("charge") && couldCharge) b.markCharge(s, "all");
    }
    static double left(List<GravesAndGlory.Unit> units) { double g = 0; for (var u : units) g += u.spec.cost() * u.mob.getHealth() / Math.max(1e-6, u.mob.getMaxHealth()); return g; }

    void endFight(GravesAndGlory.Battle b, String end) {
        var f = current;
        ledger.stop();
        var lw = living(b, west); var le = living(b, east);
        double leftW = left(lw) / Math.max(1, startGoldWest), leftE = left(le) / Math.max(1, startGoldEast);
        boolean aW = f.aWest();
        double aLeft = aW ? leftW : leftE, bLeft = aW ? leftE : leftW;
        int aAlive = aW ? lw.size() : le.size(), bAlive = aW ? le.size() : lw.size();
        String winner;
        if (end.equals("aborted")) winner = "aborted";
        else if (aAlive > 0 && bAlive == 0) winner = "a";
        else if (bAlive > 0 && aAlive == 0) winner = "b";
        else if (aAlive == 0) winner = "draw";
        else winner = aLeft > bLeft + 0.005 ? "a" : bLeft > aLeft + 0.005 ? "b" : "draw";
        int aSide = aW ? 0 : 1, bSide = 1 - aSide;
        double dealtA = 0, dealtB = 0; for (var r : ledger.sides.get(aSide).values()) dealtA += r.dealt; for (var r : ledger.sides.get(bSide).values()) dealtB += r.dealt;
        String row = String.join(",", String.valueOf(f.order()), f.id(), String.valueOf(f.rep()), String.valueOf(map), f.a().label(), f.b().label(), aW ? "west" : "east", winner, end,
            TypeLedger.f1((b.time - fightStart) / 20.0), String.valueOf(aW ? startGoldWest : startGoldEast), String.valueOf(aW ? startGoldEast : startGoldWest),
            String.valueOf(aW ? startCountWest : startCountEast), String.valueOf(aW ? startCountEast : startCountWest), String.valueOf(aAlive), String.valueOf(bAlive),
            String.format(Locale.ROOT, "%.4f", aLeft), String.format(Locale.ROOT, "%.4f", bLeft), String.format(Locale.ROOT, "%.4f", aLeft - bLeft),
            String.valueOf(ledger.kills(aSide)), String.valueOf(ledger.kills(bSide)), TypeLedger.f1(dealtA), TypeLedger.f1(dealtB),
            TypeLedger.f1((System.currentTimeMillis() - wallStart) / 1000.0), String.valueOf(Math.floorMod(fightStart, (long) PHASE_PERIOD))) + "\n";
        BotSimulation.append(duels, row);
        String pre = f.order() + "," + f.id() + "," + f.rep() + ",";
        BotSimulation.append(types, ledger.rows(aSide, pre + "a," + (aW ? "west" : "east") + "," + f.a().label()) + ledger.rows(bSide, pre + "b," + (aW ? "east" : "west") + "," + f.b().label()));
        recorded++; if (end.equals("wipe")) wipes++; else if (end.equals("cap")) caps++; else aborted++;
        if (recorded % 25 == 0) System.out.println("[duel] " + recorded + " of " + fights.size() + " fights, " + (System.currentTimeMillis() - runWallStart) / 1000 + " s");
        for (var u : new ArrayList<>(b.units)) if (u.squad == west || u.squad == east) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); b.units.remove(u); }
        ScratchFlags.reset(); Projectiles.clear(b); Abilities.clearBanners(b); b.deaths.clear();
        west = null; east = null; forcedWest.clear(); forcedEast.clear();
    }
    void summary() {
        String line = String.format(Locale.ROOT, "[duel] fights=%d wipes=%d caps=%d aborted=%d wallSeconds=%d fastApplied=%s", recorded, wipes, caps, aborted, (System.currentTimeMillis() - runWallStart) / 1000, gg.gravesandglory.testing.SimSpeed.applied);
        System.out.println(line);
        BotSimulation.append(meta, line.substring(7) + "\n");
        ScratchFlags.write(duels.resolveSibling("drops.csv"));
    }
}
