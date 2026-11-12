package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Convoy, the supply-wagon escort (spec docs/superpowers/specs/2026-10-31-convoy-escort-design.md, approved by Kort
 * 2026-10-31). One one-sided round: Dawn escorts a wagon along a fixed road through two checkpoints to a destination,
 * Dusk stops it. The wagon has no health; any single enemy within push range stops it; left alone for 10 s it rolls back,
 * never past the last checkpoint. The flags are down all round.
 *
 * The wagon is kinematic: a WagonEntity (Kort approved the type 2026-11-01; never saved, no NBT) slid along the road
 * polyline each tick, so its progress is exact. The round's own state lives on this object, which startRound makes fresh
 * every round and teardown clears, so nothing outlives the round. Numbers marked "(draft value)" are the spec's.
 */
public final class ConvoyMode extends BattleMode {
    /** The team that escorts. Dawn, as Dawn attacks in Siege (spec 3.1); the skirmish player picks a side by team. */
    public static final int ESCORT = 0;
    /** The tag the wagon carries, so recover can find one a crash left in a loaded chunk. */
    public static final String TAG = "gg-convoy-wagon";
    /** The mode's numbers, the modeRules convoy row (draft values, spec 3); preflight requires the row. */
    static Spec.ModeRulesRow rules() {
        for (var r : Spec.MODERULES) if (r.id().equals(Modes.CONVOY)) return r;
        throw new IllegalStateException("no modeRules convoy row");
    }
    static final Spec.ModeRulesRow R = rules();
    /** Push and contest range, horizontal and vertical. */
    static final double PUSH_RADIUS = R.pushRadius(), PUSH_HEIGHT = R.pushHeight();
    /** Blocks a second with one, two, three or more pushers, and rolling back. */
    static final double[] SPEEDS = {R.speedOne(), R.speedTwo(), R.speedThree()};
    static final double ROLLBACK_SPEED = R.rollbackSpeed();
    /** Ticks alone before the wagon rolls back. */
    static final int IDLE_TICKS = R.idleSeconds() * 20;
    /** Seconds each checkpoint adds to the clock, and gold it pays each escort squad. */
    static final int CHECKPOINT_SECONDS = R.checkpointSeconds(), CHECKPOINT_GOLD = R.checkpointGold();
    /** Grade over which the wagon slows uphill and speeds downhill. */
    static final double MAX_GRADE = R.maxGrade(), UPHILL = R.uphill(), DOWNHILL = R.downhill();
    /** Overtime: within this many blocks of the next stop, for this many ticks after it stops being near. */
    static final double OVERTIME_RADIUS = R.overtimeRadius();
    static final int OVERTIME_GRACE = R.overtimeGraceSeconds() * 20;
    /** "The wagon is stopped" waits this long into a contest after a move, and is sent at most this often. */
    static final int STOPPED_AFTER = R.stoppedAfterSeconds() * 20, STOPPED_EVERY = R.stoppedEverySeconds() * 20;

    /** A road node on the map: x, z and the checkpoint's name, or null for a plain node. The last node is the destination. */
    record Node(double x, double z, String name) {}
    /**
     * The road of a map, its modeSites road rows in stage order (spec 5.1; Frostmarch's is the draft straight road Kort accepted
     * 2026-11-01). A name of "-" is a plain node.
     */
    static List<Node> roadOf(int map) {
        List<Spec.ModeSitesRow> rows = new ArrayList<>();
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.CONVOY) && s.kind().equals("road") && s.map() == map) rows.add(s);
        rows.sort(java.util.Comparator.comparingInt(Spec.ModeSitesRow::stage));
        List<Node> out = new ArrayList<>();
        for (var s : rows) out.add(new Node(s.x(), s.z(), s.name().equals("-") ? null : s.name()));
        return out;
    }
    /** The map this round's road is on: the map standing, or the convoy row's first map when the standing one has no road. */
    static int roadMap(Spec.ModesRow row) {
        int map = CampaignMaps.selected;
        return roadOf(map).size() >= 2 ? map : Integer.parseInt(row.maps().split(",")[0].trim());
    }
    /** A team's own field base on the map from a modeSites base row (stage is the team), or null for the map's usual base. */
    static Spec.ModeSitesRow base(int map, int team) {
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.CONVOY) && s.kind().equals("base") && s.map() == map && s.stage() == team) return s;
        return null;
    }

    final double[][] nodes;
    final double[] stations;
    /** Road distance of each checkpoint, the destination last. */
    final double[] stops;
    final String[] stopNames;
    double distance;
    String state = ModeMath.IDLE;
    /** The battle tick the wagon was last contested (a defender within reach), for the escort bots' clear-the-wagon task (botGoal). */
    long contestedAt = Long.MIN_VALUE / 2;
    int idleTicks, pushers, defenders;
    long movingSince = -1, lastStopped = -100000, lastNear = -100000;
    boolean everMoved, rolledBack, wasRolling, finished;
    /** Whether the stop cue sounded and the wagon has not set off since (so its next start sounds the roll cue again). */
    boolean stopSounded;
    WagonEntity wagon;
    long forcedChunk = Long.MIN_VALUE;

    final int map;
    final List<Node> road;

    ConvoyMode(Spec.ModesRow row) {
        super(row);
        map = roadMap(row);
        road = roadOf(map);
        nodes = new double[road.size()][];
        List<Double> at = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) nodes[i] = new double[]{road.get(i).x(), road.get(i).z()};
        stations = ModeMath.stations(nodes);
        for (int i = 1; i < nodes.length; i++) if (road.get(i).name() != null) {at.add(stations[i]); names.add(road.get(i).name());}
        stops = at.stream().mapToDouble(Double::doubleValue).toArray();
        stopNames = names.toArray(String[]::new);
    }

    /** The road's length, start to destination. */
    double total() {return stations[stations.length - 1];}
    /** Checkpoints passed, the destination not counted. */
    int checkpoints() {return Math.min(stops.length - 1, ModeMath.passed(stops, distance));}
    /** The road distance the wagon can never roll back behind: the last checkpoint passed, or the start. */
    double floor() {int n = checkpoints(); return n == 0 ? 0 : stops[n - 1];}
    /** The next stop ahead of the wagon, a checkpoint or the destination. */
    int nextStop() {return Math.min(stops.length - 1, ModeMath.passed(stops, distance));}
    /** The wagon's place on the road, at standing height. */
    Vec3d position(double d) {
        double[] p = ModeMath.along(nodes, stations, d);
        return new Vec3d(p[0], Arena.ground((int) Math.floor(p[0]), (int) Math.floor(p[1])) + 1, p[1]);
    }
    /** The road point the given distance along, clamped, at standing height; bots and anchors use it. */
    Vec3d road(double d) {return position(Math.max(0, Math.min(total(), d)));}
    /** Whether the round is past its destination. */
    boolean through() {return distance >= total() - 1e-6;}

    @Override public String rule() {return row.ruleLine().replace("{n}", stopNames[stopNames.length - 1]);}
    @Override public boolean capturesPoints(GravesAndGlory.Battle b) {return false;}
    /** The objective panel's first line: the round has no points to capture. */
    @Override public String startLine(GravesAndGlory.Battle b) {return FeedLines.modeLine(Modes.CONVOY, "start", "stop", stopNames[stopNames.length - 1]);}
    /** A team with a modeSites base row fields there (Frostmarch's Dusk spawn sits at The Refuge, the road's end). */
    @Override public Vec3d fieldBase(GravesAndGlory.Battle b, int team) {
        var s = base(map, team);
        if (s == null) return super.fieldBase(b, team);
        return new Vec3d(s.x(), Arena.height((int) Math.floor(s.x()), (int) Math.floor(s.z())) + 1, s.z());
    }
    @Override public String howToSide(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {return s != null && s.team == ESCORT ? "attack" : "defend";}
    /** No draws: the clock running out with the wagon short of the destination is the defence's. */
    @Override public int timeoutWinner(GravesAndGlory.Battle b) {return 1 - ESCORT;}

    @Override public void setup(GravesAndGlory.Battle b) {
        for (var p : b.points) Arena.unflag(b.world, p.spec);
        removeStray(b);
        spawn(b);
    }

    @Override protected void undo(GravesAndGlory.Battle b) {
        if (wagon != null) wagon.discard();
        wagon = null;
        removeStray(b);
        unforce(b);
        for (var p : b.points) Arena.flag(b.world, p.spec, -1);
    }

    /** A crash left the flags down (Modes.recover raised them) and maybe a wagon in a chunk that loaded again: remove it. */
    @Override public void recover(GravesAndGlory.Battle b, JsonObject marker) {removeStray(b);}

    static void removeStray(GravesAndGlory.Battle b) {
        for (var e : b.world.getEntitiesByType(WagonEntity.TYPE, e -> e.getCommandTags().contains(TAG))) e.discard();
    }

    void spawn(GravesAndGlory.Battle b) {
        Vec3d at = position(distance);
        Vec3d ahead = road(distance + 2);
        wagon = new WagonEntity(b.world);
        wagon.addCommandTag(TAG);
        wagon.setCustomName(Text.literal(label()));
        wagon.setCustomNameVisible(true);
        wagon.refreshPositionAndAngles(at.x, at.y, at.z, yaw(at, ahead), 0);
        force(b, at);
        b.world.spawnEntity(wagon);
    }

    static float yaw(Vec3d from, Vec3d to) {return (float) (Math.toDegrees(Math.atan2(to.z - from.z, to.x - from.x)) - 90);}

    /** Keeps the wagon's chunk loaded: an unsaved entity in an unloading chunk would be lost. Re-asserted, as updateUnitChunks may release it. */
    void force(GravesAndGlory.Battle b, Vec3d at) {
        long key = net.minecraft.util.math.ChunkPos.toLong((int) Math.floor(at.x) >> 4, (int) Math.floor(at.z) >> 4);
        if (key != forcedChunk) unforce(b);
        forcedChunk = key;
        var c = new net.minecraft.util.math.ChunkPos(key);
        b.world.setChunkForced(c.x, c.z, true);
    }

    void unforce(GravesAndGlory.Battle b) {
        if (forcedChunk == Long.MIN_VALUE) return;
        var c = new net.minecraft.util.math.ChunkPos(forcedChunk);
        // A chunk Battle.updateUnitChunks holds for troops stays forced: it re-forces only chunks it did not hold, so releasing it here froze every troop in it (they never tick again and never leave).
        if (!b.unitChunks.contains(forcedChunk)) b.world.setChunkForced(c.x, c.z, false);
        forcedChunk = Long.MIN_VALUE;
    }

    /** Whether a spot is inside push range of the wagon at at: 6 blocks horizontally, 3 vertically. */
    static boolean inRange(Vec3d at, double x, double y, double z) {
        return Math.hypot(x - at.x, z - at.z) <= PUSH_RADIUS && Math.abs(y - at.y) <= PUSH_HEIGHT;
    }

    /** Counts this tick's pushers (living escort troops and on-foot escort commanders) and defenders (any living defence troop or commander) in range. */
    void count(GravesAndGlory.Battle b, Vec3d at) {
        int push = 0, stop = 0;
        for (var u : b.units) {
            if (!u.mob.isAlive() || !inRange(at, u.mob.getX(), u.mob.getY(), u.mob.getZ())) continue;
            if (u.squad.team == ESCORT) push++; else stop++;
        }
        for (var s : b.squads.values()) {
            if (s.bot || !s.seated || b.respawns.containsKey(s.owner)) continue;
            ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(s.owner);
            if (p == null || !p.isAlive() || p.isSpectator() || !inRange(at, p.getX(), p.getY(), p.getZ())) continue;
            if (s.team == ESCORT) {if (!p.hasVehicle()) push++;} else stop++;
        }
        pushers = push;
        defenders = stop;
    }

    @Override public void tick(GravesAndGlory.Battle b) {
        if (finished) return;
        if (wagon == null || wagon.isRemoved()) spawn(b);
        Vec3d at = wagon.getPos();
        count(b, at);
        String was = state;
        state = ModeMath.convoyState(pushers, defenders, idleTicks, IDLE_TICKS);
        if (state.equals(ModeMath.CONTESTED)) contestedAt = b.time;
        idleTicks = state.equals(ModeMath.IDLE) || state.equals(ModeMath.ROLLBACK) ? idleTicks + 1 : 0;
        int seg = ModeMath.segment(stations, distance);
        Vec3d a = position(stations[seg]), z = position(stations[seg + 1]);
        double grade = ModeMath.gradeFactor(z.y - a.y, stations[seg + 1] - stations[seg], MAX_GRADE, UPHILL, DOWNHILL);
        int passedBefore = checkpoints();
        distance = ModeMath.advance(distance, state, ModeMath.pushSpeed(pushers, SPEEDS) * grade, ROLLBACK_SPEED, 0.05, floor(), total());
        Vec3d next = position(distance);
        boolean moved = next.squaredDistanceTo(at) > 1e-8;
        // The wagon faces down the road both ways: rolling back, it runs backwards and its wheels turn back (WagonEntity).
        wagon.refreshPositionAndAngles(next.x, next.y, next.z, moved ? (state.equals(ModeMath.ROLLBACK) ? yaw(next, at) : yaw(at, next)) : wagon.getYaw(), 0);
        if (b.time % 20 == 0 || moved) force(b, next);
        if (b.time % 10 == 0) wagon.setCustomName(Text.literal(label()));
        feed(b, was);
        if (ModeMath.overtime(state, distance, stops[nextStop()], OVERTIME_RADIUS, Long.MAX_VALUE, 0)) lastNear = b.time;
        int passed = checkpoints();
        for (int i = passedBefore; i < passed; i++) checkpoint(b, i);
        if (through()) {
            finished = true;
            say(b, "through");
            b.campaign.finish(ESCORT); // the victory and defeat stings play from Campaign.finish
        }
    }

    String label() {
        return switch (state) {
            case ModeMath.MOVING -> "Supply Wagon - rolling";
            case ModeMath.CONTESTED -> "Supply Wagon - stopped";
            case ModeMath.ROLLBACK -> "Supply Wagon - rolling back";
            default -> "Supply Wagon";
        };
    }

    /** The feed lines (spec 8, draft copy): first move, a stop after 5 s of moving (at most once in 20 s), a roll back. */
    void feed(GravesAndGlory.Battle b, String was) {
        if (state.equals(ModeMath.MOVING)) {
            if (!was.equals(ModeMath.MOVING)) movingSince = b.time;
            if (!everMoved) {everMoved = true; say(b, "wagon_rolls"); Audio.all(b, AudioCues.Cue.CONVOY_ROLL, "wagon");}
            else if (stopSounded && !was.equals(ModeMath.MOVING)) Audio.all(b, AudioCues.Cue.CONVOY_ROLL, "wagon"); // it sounded stopping, so it sounds setting off again
            if (!was.equals(ModeMath.MOVING)) stopSounded = false;
        }
        if (state.equals(ModeMath.CONTESTED) && was.equals(ModeMath.MOVING) && movingSince >= 0 && b.time - movingSince >= STOPPED_AFTER && b.time - lastStopped >= STOPPED_EVERY) {
            lastStopped = b.time;
            say(b, "wagon_stopped");
            if (Audio.all(b, AudioCues.Cue.CONVOY_STOP, "wagon")) stopSounded = true;
        }
        boolean rolling = state.equals(ModeMath.ROLLBACK) && distance > floor() + 1e-6;
        if (rolling && !wasRolling) {say(b, "wagon_rolls_back"); Audio.all(b, AudioCues.Cue.CONVOY_ROLLBACK, "wagon");}
        if (rolling) {rolledBack = true; if (b.campaign.bonusTracker != null) b.campaign.bonusTracker.rolled();}
        wasRolling = rolling;
    }

    /** A checkpoint reached: clock +3:00, 50 gold to each escort squad, a feed line; the floor moves up with it (checkpoints lock). */
    void checkpoint(GravesAndGlory.Battle b, int index) {
        if (index >= stops.length - 1) return; // the destination ends the round instead
        b.roundSeconds += CHECKPOINT_SECONDS;
        for (var s : b.squads.values()) if (s.seated && s.team == ESCORT) s.earn(CHECKPOINT_GOLD);
        say(b, "checkpoint", "team", FeedLines.teamShort(b.side(ESCORT).name()), "stop", stopNames[index]);
        Audio.all(b, AudioCues.Cue.CONVOY_CHECKPOINT, "checkpoint");
    }

    /** A feed line (the modeLines convoy row of this event, draft copy) to every player, in the escort's colour. */
    static void say(GravesAndGlory.Battle b, String event, String... fields) {
        FeedEvents.mode(b, ESCORT, Modes.CONVOY, event, fields);
    }

    /** The clock waits while the wagon is moving or contested by the next stop, and for 3 s after (spec 3.1). */
    @Override public boolean overtime(GravesAndGlory.Battle b) {
        return !finished && ModeMath.overtime(state, distance, stops[nextStop()], OVERTIME_RADIUS, b.time - lastNear, OVERTIME_GRACE);
    }

    /**
     * Forward deploy places (spec 3.4): the escort at the last checkpoint passed, or the start; the defence at the next
     * checkpoint ahead. Each only while no enemy stands within the economy's forward safe radius of it.
     */
    @Override public List<GravesAndGlory.Anchor> anchors(GravesAndGlory.Battle b, int team) {
        int passed = checkpoints();
        Vec3d at;String name;
        if (team == ESCORT) {at = passed == 0 ? road(0.0) : road(stops[passed - 1]); name = passed == 0 ? "the convoy's start" : stopNames[passed - 1];}
        else {if (passed >= stops.length - 1) return List.of(); at = road(stops[passed]); name = stopNames[passed];}
        if (Morale.enemyWithin(b, team, at, Spec.ECONOMY[0].forwardSafeRadius())) return List.of();
        return List.of(new GravesAndGlory.Anchor(at.x, at.z, name, team));
    }

    /** The tracks: the escort's road progress in percent, the defence's stops still held (checkpoints ahead plus the destination). */
    @Override public int[] ledger(GravesAndGlory.Battle b) {
        int[] out = new int[2];
        out[ESCORT] = ModeMath.percent(distance, total());
        out[1 - ESCORT] = stops.length - checkpoints() - (through() ? 1 : 0);
        return out;
    }
    /** The deciding numbers: the road covered in percent, the stops the defence still holds, and the clock (which each checkpoint extends). */
    @Override public List<ModeLines.Entry> ledgerEntries(GravesAndGlory.Battle b) {
        int[] l = ledger(b), m = max(b);
        return List.of(ModeLines.entry("convoy_progress", l[ESCORT], m[ESCORT]), ModeLines.entry("convoy_stops", l[1 - ESCORT], m[1 - ESCORT]), ModeLines.entry("convoy_time", clock(b), b.roundSeconds));
    }
    @Override public int[] max(GravesAndGlory.Battle b) {int[] m = new int[2]; m[ESCORT] = 100; m[1 - ESCORT] = stops.length; return m;}
    @Override public String[] formats() {String[] f = new String[2]; f[ESCORT] = "percent"; f[1 - ESCORT] = "plain"; return f;}

    /** The share of a team's banners on the wagon itself (raiders for the defence; the escort pushes with the rest), by bot tier (draft values, spec 3.6). */
    static double raidShare(String tier) {return switch (tier) {case "easy" -> 0.34; default -> 0.5;};}
    /** How far a screen or blocker may stray from the wagon before it regroups on it, by bot tier (draft values). */
    static double chaseRange(String tier) {return switch (tier) {case "easy" -> 40; case "hard" -> 120; default -> 80;};}

    /** How far up the road a defence blocker holds ahead of the wagon: 30 blocks let the escort's screens hold the blockers off the wagon
     *  (escort 10 of 10, mode gate 2026-11-02); 10 keeps them in the fight at the wagon. */
    static final double BLOCK_AHEAD = 10;
    /** The defence never falls back to The Refuge, the road's far end: retreating there took it off the wagon for about 46% of the round
     *  (mode gate 2026-11-02). It fights on the road and refills from its buys; the escort keeps the tier's fall-back. */
    @Override public boolean botRetreats(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {return s.team == ESCORT;}

    /**
     * Bot goals (spec 3.6). Each team's seated banners split by ModeMath.convoyRoles, humans counted first as primary.
     * Escort: pushers walk just ahead of the wagon, screens 20 blocks further up the road. Defence: raiders go to the wagon,
     * blockers hold the road up to BLOCK_AHEAD blocks ahead, no further than the next stop. A screen or blocker farther than chaseRange from the wagon regroups on it.
     */
    @Override public Vec3d botGoal(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Vec3d centre, Spec.BotCommandRow tier) {
        if (finished) return null;
        boolean escort = s.team == ESCORT;
        int banners = 0, humans = 0, index = -1, bots = 0;
        for (var q : b.squads.values()) {
            if (!q.seated || q.team != s.team) continue;
            banners++;
            if (!q.bot) humans++; else {if (q == s) index = bots; bots++;}
        }
        if (index < 0) return null;
        List<String> roles = ModeMath.convoyRoles(banners, humans, raidShare(tier.id()), escort);
        String role = roles.get(Math.min(roles.size() - 1, humans + index));
        Vec3d at = position(distance);
        if (escort && b.time - contestedAt <= CLEAR_TICKS && clears(b, s, at)) return at; // the wagon is contested: the nearest escorts clear it (audit C1, C2)
        boolean far = Math.hypot(centre.x - at.x, centre.z - at.z) > chaseRange(tier.id());
        return switch (role) {
            case "pusher" -> road(distance + 3);
            case "raider" -> at;
            case "screen" -> far ? at : road(distance + 20);
            default -> far ? at : road(Math.min(distance + BLOCK_AHEAD, stops[nextStop()]));
        };
    }

    /** How long after the wagon was last contested the escorts sent to clear it stay on it (so a defender stepping in and out does not flap their orders), and how many of the escort's bot armies go. */
    static final long CLEAR_TICKS = 100;
    static final int CLEAR_ARMIES = 2;
    /**
     * Whether this escort army is one of the CLEAR_ARMIES escort bot armies nearest the wagon (by the centre of their troops; ties to the earlier army),
     * which go to the wagon itself while it is contested and clear the defenders off it, whatever their role (pusher or screen).
     */
    static boolean clears(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Vec3d wagon) {
        Map<GravesAndGlory.Squad, double[]> sum = new LinkedHashMap<>();
        for (var u : b.units) {
            var q = u.squad;
            if (!u.mob.isAlive() || q.team != ESCORT || !q.bot || !q.seated || u.musterSince >= 0 || b.campaign.botRetreating.contains(q.owner)) continue;
            double[] c = sum.computeIfAbsent(q, k -> new double[3]);
            c[0] += u.mob.getX(); c[1] += u.mob.getZ(); c[2]++;
        }
        double[] mine = sum.get(s);
        if (mine == null) return false;
        double own = Math.hypot(mine[0] / mine[2] - wagon.x, mine[1] / mine[2] - wagon.z);
        int nearer = 0;
        for (var e : sum.entrySet()) {
            if (e.getKey() == s) continue;
            double[] c = e.getValue();
            if (Math.hypot(c[0] / c[2] - wagon.x, c[1] / c[2] - wagon.z) < own) nearer++;
        }
        return nearer < CLEAR_ARMIES;
    }

    /** Whether the round's wagon ever rolled back behind where it stood: the never_rolled bonus is lost. */
    public boolean rolledBack() {return rolledBack;}

    /** The lobby and HUD block gains convoy: wagon entity id, progress, state, checkpoints passed, the next stop, the escort, the road and its stops. */
    @Override public JsonObject snapshot(GravesAndGlory.Battle b, GravesAndGlory.Squad viewer) {
        JsonObject j = super.snapshot(b, viewer);
        JsonObject c = new JsonObject();
        c.addProperty("entity", wagon == null ? -1 : wagon.getId());
        c.addProperty("progress", total() <= 0 ? 0 : Math.round(distance / total() * 1000) / 1000.0);
        c.addProperty("state", state);
        c.addProperty("checkpoint", checkpoints());
        c.addProperty("next", stopNames[nextStop()]);
        c.addProperty("escortTeam", ESCORT);
        JsonArray road = new JsonArray();
        for (double[] n : nodes) {JsonArray p = new JsonArray(); p.add(n[0]); p.add(n[1]); road.add(p);}
        c.add("nodes", road);
        JsonArray at = new JsonArray();
        for (double s : stops) at.add(Math.round(s / total() * 1000) / 1000.0);
        c.add("stops", at);
        j.add("convoy", c);
        return j;
    }

    /** The wagon: an invisible armour stand wearing a barrel, never saved, never hurt, never moved but by the round. */
}
