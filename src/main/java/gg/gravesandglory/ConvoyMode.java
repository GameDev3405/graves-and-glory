package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.List;

/**
 * Convoy, the supply-wagon escort (spec docs/superpowers/specs/2026-10-31-convoy-escort-design.md, approved by Kort
 * 2026-10-31). One one-sided round: Dawn escorts a wagon along a fixed road through two checkpoints to a destination,
 * Dusk stops it. The wagon has no health; any single enemy within push range stops it; left alone for 10 s it rolls back,
 * never past the last checkpoint. The flags are down all round.
 *
 * The wagon is kinematic: an invisible vanilla armour stand (no new entity type, never saved) slid along the road
 * polyline each tick, so its progress is exact. The round's own state lives on this object, which startRound makes fresh
 * every round and teardown clears, so nothing outlives the round. Numbers marked "(draft value)" are the spec's.
 */
public final class ConvoyMode extends BattleMode {
    /** The team that escorts. Dawn, as Dawn attacks in Siege (spec 3.1); the skirmish player picks a side by team. */
    public static final int ESCORT = 0;
    /** The tag the wagon carries, so recover can find one a crash left in a loaded chunk. */
    public static final String TAG = "gg-convoy-wagon";
    /** Push and contest range, horizontal and vertical (draft values). */
    static final double PUSH_RADIUS = 6, PUSH_HEIGHT = 3;
    /** Blocks a second with one, two, three or more pushers, and rolling back (draft values). */
    static final double[] SPEEDS = {1.2, 1.5, 1.8};
    static final double ROLLBACK_SPEED = 0.6;
    /** Ticks alone before the wagon rolls back, 10 s (draft value). */
    static final int IDLE_TICKS = 200;
    /** Seconds each checkpoint adds to the clock, and gold it pays each escort squad (draft values). */
    static final int CHECKPOINT_SECONDS = 180, CHECKPOINT_GOLD = 50;
    /** Grade over which the wagon slows uphill and speeds downhill (draft values). */
    static final double MAX_GRADE = 0.25, UPHILL = 0.75, DOWNHILL = 1.25;
    /** Overtime: within this many blocks of the next stop, for this many ticks after it stops being near (draft values). */
    static final double OVERTIME_RADIUS = 6;
    static final int OVERTIME_GRACE = 60;
    /** "The wagon is stopped" waits this long into a contest after a move, and is sent at most this often (draft values). */
    static final int STOPPED_AFTER = 100, STOPPED_EVERY = 400;

    /** A road node on the map: x, z and the checkpoint's name, or null for a plain node. The last node is the destination. */
    record Node(double x, double z, String name) {}
    /**
     * Frostmarch's draft road (spec 5.2): the caravan lodge yard on the Dawn side, Pass Crossing past the gorge bridge, Signal
     * Ridge, the refuge short of the Dusk gate. Placeholders until the Frostmarch Map Forge layout is signed; then they move to
     * modeSites road rows (spec 5.1).
     */
    static final List<Node> FROSTMARCH = List.of(
            new Node(-230, 0, null), new Node(-28, 0, null), new Node(32, 0, "Pass Crossing"), new Node(100, 30, null),
            new Node(150, 60, "Signal Ridge"), new Node(220, 30, null), new Node(280, 0, "The Refuge"));

    final double[][] nodes;
    final double[] stations;
    /** Road distance of each checkpoint, the destination last. */
    final double[] stops;
    final String[] stopNames;
    double distance;
    String state = ModeMath.IDLE;
    int idleTicks, pushers, defenders;
    long movingSince = -1, lastStopped = -100000, lastNear = -100000;
    boolean everMoved, rolledBack, wasRolling, finished;
    Wagon wagon;
    long forcedChunk = Long.MIN_VALUE;

    ConvoyMode(Spec.ModesRow row) {
        super(row);
        nodes = new double[FROSTMARCH.size()][];
        List<Double> at = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) nodes[i] = new double[]{FROSTMARCH.get(i).x(), FROSTMARCH.get(i).z()};
        stations = ModeMath.stations(nodes);
        for (int i = 1; i < nodes.length; i++) if (FROSTMARCH.get(i).name() != null) {at.add(stations[i]); names.add(FROSTMARCH.get(i).name());}
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
        for (var e : b.world.getEntitiesByType(EntityType.ARMOR_STAND, e -> e.getCommandTags().contains(TAG))) e.discard();
    }

    void spawn(GravesAndGlory.Battle b) {
        Vec3d at = position(distance);
        Vec3d ahead = road(distance + 2);
        wagon = new Wagon(b.world);
        wagon.addCommandTag(TAG);
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
        b.world.setChunkForced(c.x, c.z, false);
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
        idleTicks = state.equals(ModeMath.IDLE) || state.equals(ModeMath.ROLLBACK) ? idleTicks + 1 : 0;
        int seg = ModeMath.segment(stations, distance);
        Vec3d a = position(stations[seg]), z = position(stations[seg + 1]);
        double grade = ModeMath.gradeFactor(z.y - a.y, stations[seg + 1] - stations[seg], MAX_GRADE, UPHILL, DOWNHILL);
        int passedBefore = checkpoints();
        distance = ModeMath.advance(distance, state, ModeMath.pushSpeed(pushers, SPEEDS) * grade, ROLLBACK_SPEED, 0.05, floor(), total());
        Vec3d next = position(distance);
        boolean moved = next.squaredDistanceTo(at) > 1e-8;
        wagon.refreshPositionAndAngles(next.x, next.y, next.z, moved ? yaw(at, next) + (state.equals(ModeMath.ROLLBACK) ? 180 : 0) : wagon.getYaw(), 0);
        wagon.setHeadYaw(wagon.getYaw());
        if (b.time % 20 == 0 || moved) force(b, next);
        if (b.time % 10 == 0) wagon.setCustomName(Text.literal(label()));
        feed(b, was);
        if (ModeMath.overtime(state, distance, stops[nextStop()], OVERTIME_RADIUS, Long.MAX_VALUE, 0)) lastNear = b.time;
        int passed = checkpoints();
        for (int i = passedBefore; i < passed; i++) checkpoint(b, i);
        if (through()) {
            finished = true;
            say(b, "The convoy is through");
            b.campaign.finish(ESCORT);
            b.world.playSound(null, BlockPos.ORIGIN, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 2, 1);
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
            if (!everMoved) {everMoved = true; say(b, "The wagon rolls");}
        }
        if (state.equals(ModeMath.CONTESTED) && was.equals(ModeMath.MOVING) && movingSince >= 0 && b.time - movingSince >= STOPPED_AFTER && b.time - lastStopped >= STOPPED_EVERY) {
            lastStopped = b.time;
            say(b, "The wagon is stopped");
        }
        boolean rolling = state.equals(ModeMath.ROLLBACK) && distance > floor() + 1e-6;
        if (rolling && !wasRolling) say(b, "The wagon rolls back");
        if (rolling) rolledBack = true;
        wasRolling = rolling;
    }

    /** A checkpoint reached: clock +3:00, 50 gold to each escort squad, a feed line; the floor moves up with it (checkpoints lock). */
    void checkpoint(GravesAndGlory.Battle b, int index) {
        if (index >= stops.length - 1) return; // the destination ends the round instead
        b.roundSeconds += CHECKPOINT_SECONDS;
        for (var s : b.squads.values()) if (s.seated && s.team == ESCORT) s.earn(CHECKPOINT_GOLD);
        say(b, FeedLines.teamShort(Spec.FACTIONS[ESCORT].name()) + " reaches " + stopNames[index]);
        b.world.playSound(null, BlockPos.ORIGIN, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1, 1);
    }

    static void say(GravesAndGlory.Battle b, String text) {
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) FeedEvents.send(p, "point", ESCORT, text);
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
        if (team == ESCORT) {at = passed == 0 ? road(0) : road(stops[passed - 1]); name = passed == 0 ? "the convoy's start" : stopNames[passed - 1];}
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
    @Override public int[] max(GravesAndGlory.Battle b) {int[] m = new int[2]; m[ESCORT] = 100; m[1 - ESCORT] = stops.length; return m;}
    @Override public String[] formats() {String[] f = new String[2]; f[ESCORT] = "percent"; f[1 - ESCORT] = "plain"; return f;}

    /** The share of a team's banners on the wagon itself (raiders for the defence; the escort pushes with the rest), by bot tier (draft values, spec 3.6). */
    static double raidShare(String tier) {return switch (tier) {case "easy" -> 0.34; default -> 0.5;};}
    /** How far a screen or blocker may stray from the wagon before it regroups on it, by bot tier (draft values). */
    static double chaseRange(String tier) {return switch (tier) {case "easy" -> 40; case "hard" -> 120; default -> 80;};}

    /**
     * Bot goals (spec 3.6). Each team's seated banners split by ModeMath.convoyRoles, humans counted first as primary.
     * Escort: pushers walk just ahead of the wagon, screens 20 blocks further up the road. Defence: raiders go to the wagon,
     * blockers hold the road up to 30 blocks ahead, no further than the next stop. A screen or blocker farther than chaseRange from the wagon regroups on it.
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
        boolean far = Math.hypot(centre.x - at.x, centre.z - at.z) > chaseRange(tier.id());
        return switch (role) {
            case "pusher" -> road(distance + 3);
            case "raider" -> at;
            case "screen" -> far ? at : road(distance + 20);
            default -> far ? at : road(Math.min(distance + 30, stops[nextStop()]));
        };
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
    public static final class Wagon extends ArmorStandEntity {
        public Wagon(World world) {
            super(EntityType.ARMOR_STAND, world);
            setInvisible(true);
            setNoGravity(true);
            setInvulnerable(true);
            setCustomName(Text.literal("Supply Wagon"));
            setCustomNameVisible(true);
            equipStack(EquipmentSlot.HEAD, new ItemStack(Items.BARREL));
        }
        @Override public boolean shouldSave() {return false;}
        /** No health (spec 3.3): arrows, blows, bolts and blasts change nothing. */
        @Override public boolean damage(DamageSource source, float amount) {return false;}
        @Override public boolean isImmuneToExplosion() {return true;}
        @Override public ActionResult interactAt(PlayerEntity player, Vec3d hitPos, Hand hand) {return ActionResult.FAIL;}
        @Override public boolean isPushable() {return false;}
        @Override public void pushAwayFrom(net.minecraft.entity.Entity entity) {}
    }
}
