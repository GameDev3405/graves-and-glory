package gg.gravesandglory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import java.util.ArrayList;
import java.util.List;

/** The client end of the battle map's three channels (spec section 7). battlemap_grid_v1 stores the scanned field in {@link #grid};
 *  battlemap_v1 (every 20 ticks, own team only) replaces {@link #state}; {@link #sendPlan} writes battleplan_v1. Both fields are
 *  written on the client thread and are null until the first packet of a battle. */
public final class BattleMapClient {
    private BattleMapClient() {}

    /** One group of an army: where it stands, how many, its mean health 0..1, its standing order and goal, and whether it is in a fight. */
    public record Group(String group, double x, double z, int count, double health, String order, double targetX, double targetZ, boolean fighting) {}
    /** One allied army. job is the plan's job ordinal (-1 none), point its capture point (-1 none), direct true for a direct order. */
    public record Army(String id, String name, boolean bot, boolean retreating, List<Group> groups, int job, int point, double planX, double planZ, boolean direct) {}
    public record PointPlan(int point, int job, String setter) {}
    public record Order(String army, int job, int point, double x, double z, String setter) {}
    public record State(long time, List<Army> armies, List<PointPlan> points, List<Order> orders) {}

    /** The field as last received, or null. */
    public static volatile BattleMapGrid grid;
    /** The last battlemap_v1 record, or null. */
    public static volatile State state;

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.MAP_GRID, (client, handler, buf, responder) -> {
            int n = buf.readInt();
            if (n < 0 || n > buf.readableBytes()) return;
            byte[] data = new byte[n];
            buf.readBytes(data);
            client.execute(() -> {
                try { grid = BattleMapGrid.decode(data); }
                catch (RuntimeException bad) { grid = null; }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.MAP_STATE, (client, handler, buf, responder) -> {
            State s;
            try { s = readState(buf); } catch (RuntimeException bad) { return; }
            client.execute(() -> state = s);
        });
    }

    static State readState(PacketByteBuf buf) {
        long time = buf.readLong();
        int na = buf.readInt();
        List<Army> armies = new ArrayList<>();
        for (int i = 0; i < na; i++) {
            String id = buf.readString(36), name = buf.readString(32);
            boolean bot = buf.readBoolean(), retreating = buf.readBoolean();
            int ng = buf.readInt();
            List<Group> groups = new ArrayList<>();
            for (int g = 0; g < ng; g++) {
                String group = buf.readString(16);
                double x = buf.readFloat(), z = buf.readFloat();
                int count = buf.readInt();
                double health = buf.readFloat();
                String order = buf.readString(16);
                double tx = buf.readFloat(), tz = buf.readFloat();
                groups.add(new Group(group, x, z, count, health, order, tx, tz, buf.readBoolean()));
            }
            int job = buf.readByte(), point = buf.readInt();
            double px = buf.readFloat(), pz = buf.readFloat();
            armies.add(new Army(id, name, bot, retreating, groups, job, point, px, pz, buf.readBoolean()));
        }
        int np = buf.readInt();
        List<PointPlan> points = new ArrayList<>();
        for (int i = 0; i < np; i++) points.add(new PointPlan(buf.readInt(), buf.readByte(), buf.readString(32)));
        int no = buf.readInt();
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < no; i++) {
            String army = buf.readString(36);
            int job = buf.readByte(), point = buf.readInt();
            double x = buf.readFloat(), z = buf.readFloat();
            orders.add(new Order(army, job, point, x, z, buf.readString(32)));
        }
        return new State(time, armies, points, orders);
    }

    /** battleplan_v1. kind 0 sets a point's plan (point), kind 1 orders one army (army, and point or x/z). job is a
     *  {@link BattlePlan.Job} ordinal, -1 clears. */
    public static void sendPlan(int kind, int point, String army, int job, double x, double z) {
        if (!ClientPlayNetworking.canSend(GravesAndGlory.PLAN)) return;
        var buf = PacketByteBufs.create();
        buf.writeByte(kind);
        buf.writeInt(point);
        buf.writeString(army == null ? "" : army, 36);
        buf.writeByte(job);
        buf.writeDouble(x);
        buf.writeDouble(z);
        ClientPlayNetworking.send(GravesAndGlory.PLAN, buf);
    }

    /** Forgets the field and state (a new battle, or leaving). */
    public static void clear() { grid = null; state = null; }
}
