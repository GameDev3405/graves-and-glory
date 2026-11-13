package gg.gravesandglory;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/** The server half of the battle map (spec sections 4, 5 and 7): the field grid scan and its packet, the once-a-second battlemap_v1 state,
 *  the battleplan_v1 receiver, and the bots' ack lines. The plan table itself is the pure BattlePlan on the Battle; this class only moves
 *  data in and out and checks who may ask for what. Server thread only (the receiver hands its work to it). */
final class BattleMapNet {
    private BattleMapNet() {}
    static final Logger LOG = LoggerFactory.getLogger("gravesandglory");

    /** A custom payload may not pass 1 MB; a grid over this is not sent (no map of the shipped sheet comes near it: Kingsfield is tens of KB deflated). */
    static final int MAX_GRID_BYTES = 900_000;
    /** The state goes out every STATE_TICKS; a player's plan packets are spaced PLAN_COOLDOWN_TICKS (0.5 s). */
    static final int STATE_TICKS = 20, PLAN_COOLDOWN_TICKS = 10;
    /** A bot says at most one line per ACK_GAP_TICKS; a new line waits ACK_MIN..ACK_MAX ticks, so several bots do not answer on one tick. */
    static final int ACK_GAP_TICKS = 100, ACK_MIN = 20, ACK_MAX = 60;

    // ---------------------------------------------------------------- the grid

    /** Scans the built field (once per build: the cache on the battle is keyed by map id and build key). */
    static void scan(GravesAndGlory.Battle b) {
        int map = b.campaign.map;
        String id = Spec.MAPS[map].id();
        String build = EngineMaps.active() ? EngineMaps.current().sha : "old-" + map + "-" + Integer.toHexString(Spec.MAPS[map].hashCode());
        String key = id + "/" + build;
        if (key.equals(b.mapGridKey) && b.mapGrid != null) return;
        try {
            b.mapGrid = BattleMapScan.scan(b.world, id, build, Field.halfX(map), Field.halfZ(map));
            b.mapGridKey = key;
            b.mapGridBytes = null;
            b.mapGridSent.clear();
        } catch (RuntimeException e) {
            LOG.warn("battle map scan failed: {}", e.toString());
            b.mapGrid = null; b.mapGridKey = null; b.mapGridBytes = null;
        }
    }

    /** Sends the grid to one player unless he already holds this key this session. */
    static void sendGrid(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        if (b.mapGrid == null || !ServerPlayNetworking.canSend(p, GravesAndGlory.MAP_GRID)) return;
        if (b.mapGridKey.equals(b.mapGridSent.get(p.getUuid()))) return;
        if (b.mapGridBytes == null) b.mapGridBytes = b.mapGrid.encode();
        if (b.mapGridBytes.length > MAX_GRID_BYTES) { LOG.warn("battle map grid is {} bytes, over the packet limit; not sent", b.mapGridBytes.length); return; }
        var buf = PacketByteBufs.create();
        buf.writeInt(b.mapGridBytes.length);
        buf.writeBytes(b.mapGridBytes);
        ServerPlayNetworking.send(p, GravesAndGlory.MAP_GRID, buf);
        b.mapGridSent.put(p.getUuid(), b.mapGridKey);
    }

    static void sendGridAll(GravesAndGlory.Battle b) { for (var p : b.server.getPlayerManager().getPlayerList()) sendGrid(b, p); }

    // ---------------------------------------------------------------- armies, for assign and for the state

    /** The point index an army's target sits on, or -1 (the sticky half of BattlePlan.assign). */
    static int pointOf(GravesAndGlory.Battle b, Vec3d target) {
        if (target == null) return -1;
        for (int i = 0; i < b.points.size(); i++) if (Campaign.on(target, b.points.get(i))) return i;
        return -1;
    }

    /** Every seated squad with living troops as the pure plan sees it: where its troops stand, whether it is a bot, whether it is falling back. */
    static List<BattlePlan.Army> armies(GravesAndGlory.Battle b) {
        Map<GravesAndGlory.Squad, double[]> sums = new IdentityHashMap<>();
        for (var u : b.units) {
            if (!u.mob.isAlive() || u.musterSince >= 0) continue;
            double[] a = sums.computeIfAbsent(u.squad, k -> new double[3]);
            a[0] += u.mob.getX(); a[1] += u.mob.getZ(); a[2]++;
        }
        List<BattlePlan.Army> out = new ArrayList<>();
        for (var s : b.squads.values()) {
            double[] a = sums.get(s);
            if (a == null || !s.seated) continue;
            out.add(new BattlePlan.Army(s.owner.toString(), s.team, a[0] / a[2], a[1] / a[2], s.bot, b.campaign.botRetreating.contains(s.owner), pointOf(b, s.target)));
        }
        return out;
    }

    static double[][] pointXZ(GravesAndGlory.Battle b) {
        double[] px = new double[b.points.size()], pz = new double[b.points.size()];
        for (int i = 0; i < px.length; i++) { px[i] = b.points.get(i).spec.x(); pz[i] = b.points.get(i).spec.z(); }
        return new double[][] {px, pz};
    }

    /** What the plan asks of one army now, or null (a pure query: BattlePlan.jobFor). */
    static BattlePlan.Assignment jobFor(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (b.plan.points(s.team).isEmpty() && b.plan.orders(s.team).isEmpty()) return null;
        var xz = pointXZ(b);
        return b.plan.jobFor(s.owner.toString(), s.team, armies(b), xz[0], xz[1]);
    }

    /** The player an army was told to follow, or null (not following, or he is gone). */
    static ServerPlayerEntity followed(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        for (var o : b.plan.orders(s.team)) {
            if (o.job() != BattlePlan.Job.FOLLOW || !o.army().equals(s.owner.toString())) continue;
            try { return b.server.getPlayerManager().getPlayer(UUID.fromString(o.setter())); } catch (IllegalArgumentException e) { return null; }
        }
        return null;
    }

    static String botName(GravesAndGlory.Squad s) { return s.tier + " bot"; }

    static String nameOf(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return s.bot ? botName(s) : b.campaign.name(s.owner); }

    static String[] pointIds(GravesAndGlory.Battle b) {
        String[] ids = new String[b.points.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = b.points.get(i).spec.name();
        return ids;
    }

    // ---------------------------------------------------------------- ack lines

    record Ack(BattlePlan.Assignment job, long due) {}
    private static final Random SPREAD = new Random(0x62616e6e657231L);

    /** A bot's decision applied this assignment (null: none). When it differs from the last one noted, the bot says so after a short wait, at most
     *  once per ACK_GAP_TICKS; a newer change replaces a line still waiting, and a change back to the old job cancels it. Armies that fall back are
     *  not passed here, so a retreat is silent. */
    static void noted(GravesAndGlory.Battle b, GravesAndGlory.Squad s, BattlePlan.Assignment now) {
        if (!s.bot) return;
        var before = b.lastAck.get(s.owner);
        if (Objects.equals(before, now)) return;
        b.lastAck.put(s.owner, now);
        if (Objects.equals(b.saidAck.get(s.owner), now)) { b.pendingAck.remove(s.owner); return; }
        long due = b.time + ACK_MIN + SPREAD.nextInt(ACK_MAX - ACK_MIN + 1);
        due = Math.max(due, b.lastSaid.getOrDefault(s.owner, -(long) ACK_GAP_TICKS) + ACK_GAP_TICKS);
        b.pendingAck.put(s.owner, new Ack(now, due));
    }

    private static void sayDue(GravesAndGlory.Battle b) {
        if (b.pendingAck.isEmpty()) return;
        for (var it = b.pendingAck.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            Ack ack = e.getValue();
            if (b.time < ack.due()) continue;
            it.remove();
            var s = b.squads.get(e.getKey());
            if (s == null || !s.bot || !s.seated) continue;
            b.lastSaid.put(s.owner, b.time);
            b.saidAck.put(s.owner, ack.job());
            String text = botName(s) + ": " + BattlePlan.ackLine(ack.job(), pointIds(b));
            for (var p : b.server.getPlayerManager().getPlayerList()) {
                var ps = b.squads.get(p.getUuid());
                if (ps != null && ps.seated && ps.team == s.team) FeedEvents.send(p, "chat", s.team, text);
            }
        }
    }

    // ---------------------------------------------------------------- the state

    /** Every server tick of a running round: due ack lines go out; once a second Attack plans on held points turn to Defend and each seated player gets the state. */
    static void tick(GravesAndGlory.Battle b) {
        if (!b.running) return;
        sayDue(b);
        if (b.time % STATE_TICKS != 0) return;
        if (!b.points.isEmpty() && b.mode.capturesPoints(b)) {
            int[] owners = new int[b.points.size()]; boolean[] contested = new boolean[owners.length];
            for (int i = 0; i < owners.length; i++) { owners[i] = b.points.get(i).owner; contested[i] = b.points.get(i).contested; }
            b.plan.update(owners, contested);
        }
        sendState(b);
    }

    static void sendState(GravesAndGlory.Battle b) {
        List<ServerPlayerEntity> to = new ArrayList<>();
        for (var p : b.server.getPlayerManager().getPlayerList()) {
            var s = b.squads.get(p.getUuid());
            if (s != null && s.seated && ServerPlayNetworking.canSend(p, GravesAndGlory.MAP_STATE)) to.add(p);
        }
        if (to.isEmpty()) return;
        var armies = armies(b);
        var xz = pointXZ(b);
        Map<Integer, PacketByteBuf> perTeam = new HashMap<>();
        for (var p : to) {
            int team = b.squads.get(p.getUuid()).team;
            var buf = perTeam.computeIfAbsent(team, t -> writeState(b, t, armies, xz[0], xz[1]));
            ServerPlayNetworking.send(p, GravesAndGlory.MAP_STATE, PacketByteBufs.copy(buf));
        }
    }

    private static PacketByteBuf writeState(GravesAndGlory.Battle b, int team, List<BattlePlan.Army> armies, double[] px, double[] pz) {
        var buf = PacketByteBufs.create();
        buf.writeLong(b.time);
        Map<String, BattlePlan.Assignment> jobs = b.plan.assign(team, armies, px, pz);
        Map<GravesAndGlory.Squad, Map<String, List<GravesAndGlory.Unit>>> byArmy = new LinkedHashMap<>();
        for (var u : b.units) {
            if (u.squad.team != team || !u.mob.isAlive() || !u.squad.seated) continue;
            byArmy.computeIfAbsent(u.squad, k -> new LinkedHashMap<>()).computeIfAbsent(u.spec.group(), k -> new ArrayList<>()).add(u);
        }
        buf.writeInt(byArmy.size());
        for (var e : byArmy.entrySet()) {
            var s = e.getKey();
            String id = s.owner.toString();
            buf.writeString(id, 36);
            buf.writeString(nameOf(b, s), 32);
            buf.writeBoolean(s.bot);
            buf.writeBoolean(b.campaign.botRetreating.contains(s.owner));
            buf.writeInt(e.getValue().size());
            for (var g : e.getValue().entrySet()) {
                var members = g.getValue();
                var a = b.groupOrder(s, g.getKey());
                buf.writeString(g.getKey(), 16);
                buf.writeFloat((float) members.stream().mapToDouble(u -> u.mob.getX()).average().orElse(0));
                buf.writeFloat((float) members.stream().mapToDouble(u -> u.mob.getZ()).average().orElse(0));
                buf.writeInt(members.size());
                buf.writeFloat((float) members.stream().mapToDouble(u -> u.mob.getHealth() / u.mob.getMaxHealth()).average().orElse(0));
                buf.writeString(a.order, 16);
                buf.writeFloat((float) a.target.x);
                buf.writeFloat((float) a.target.z);
                buf.writeBoolean(members.stream().anyMatch(u -> u.mob.getTarget() != null));
            }
            var as = s.bot ? jobs.get(id) : null;
            buf.writeByte(as == null ? -1 : as.job().ordinal());
            buf.writeInt(as == null ? -1 : as.point());
            buf.writeFloat((float) (as == null ? 0 : as.x()));
            buf.writeFloat((float) (as == null ? 0 : as.z()));
            buf.writeBoolean(as != null && as.direct());
        }
        var points = b.plan.points(team);
        buf.writeInt(points.size());
        for (var p : points) { buf.writeInt(p.point()); buf.writeByte(p.job().ordinal()); buf.writeString(setterName(b, p.setter()), 32); }
        var orders = b.plan.orders(team);
        buf.writeInt(orders.size());
        for (var o : orders) {
            buf.writeString(o.army(), 36); buf.writeByte(o.job().ordinal()); buf.writeInt(o.point());
            buf.writeFloat((float) o.x()); buf.writeFloat((float) o.z()); buf.writeString(setterName(b, o.setter()), 32);
        }
        return buf;
    }

    private static String setterName(GravesAndGlory.Battle b, String setter) {
        try { return b.campaign.name(UUID.fromString(setter)); } catch (IllegalArgumentException e) { return setter; }
    }

    // ---------------------------------------------------------------- the plan packet

    /** Called on the server thread with what battleplan_v1 said. A refusal is one feed line to the player alone. */
    static void apply(GravesAndGlory.Battle b, ServerPlayerEntity p, int kind, int point, String army, int job, double x, double z) {
        var me = b.squads.get(p.getUuid());
        if (me == null || !me.seated || !b.running) return;
        if (b.time - b.lastPlan.getOrDefault(p.getUuid(), -100L) < PLAN_COOLDOWN_TICKS) return;
        b.lastPlan.put(p.getUuid(), b.time);
        String why = check(b, me, kind, point, army, job, x, z);
        if (why != null) { FeedEvents.send(p, "notice", me.team, why); return; }
        BattlePlan.Job j = job < 0 ? null : BattlePlan.Job.values()[job];
        String setter = p.getUuid().toString();
        if (kind == 0) b.plan.setPoint(me.team, point, j, setter, b.time);
        else b.plan.setOrder(me.team, army, j, point, x, z, setter, b.time);
    }

    /** The reason a request is refused, or null when it is fine. */
    private static String check(GravesAndGlory.Battle b, GravesAndGlory.Squad me, int kind, int point, String army, int job, double x, double z) {
        if (job < -1 || job >= BattlePlan.Job.values().length) return "That order is not known.";
        var j = job < 0 ? null : BattlePlan.Job.values()[job];
        if (kind == 0) {
            if (!b.mode.capturesPoints(b)) return "This battle has no points to plan.";
            if (point < 0 || point >= b.points.size()) return "There is no such point.";
            if (j == BattlePlan.Job.FOLLOW) return "Only one army can be told to follow you.";
            return null;
        }
        if (kind != 1) return "That order is not known.";
        UUID id;
        try { id = UUID.fromString(army); } catch (IllegalArgumentException e) { return "No such army."; }
        var target = b.squads.get(id);
        if (target == null || !target.bot || !target.seated || target.team != me.team) return "You can only order your allied bots.";
        if (j == null || j == BattlePlan.Job.FOLLOW) return null;
        if (point >= 0) return point < b.points.size() && b.mode.capturesPoints(b) ? null : "There is no such point.";
        if (!Double.isFinite(x) || !Double.isFinite(z) || !GravesAndGlory.Battle.inArena(x, z)) return "That place is outside the field.";
        return null;
    }
}
