package gg.gravesandglory;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * command_v1, server to client (command UI plan Task 3): a byte kind, then its fields.
 * <ul>
 *   <li>0 COOLDOWN: int chargeReadyInTicks, to the commander whose Charge was used or refused.</li>
 *   <li>1 OUTLINE: int ownerTeamSlot, String formation, double x, z, float yaw, int count, then count x (String group, int members); to the
 *       issuer's teammates (the other non-bot squads of his team), never to him and never to an enemy. ownerTeamSlot is the issuer's place
 *       among his team's squads in Battle.squads order, stable through a round, so a client can tell two teammates' outlines apart.</li>
 *   <li>2 ACK: String order, int n then n x int unitEntityIds (the troops that raise their weapons), double x, z (the pulse centre); to the
 *       issuer and his teammates.</li>
 * </ul>
 * The server owns every value: OrderRules.playerOrder calls these after it stores an order. Only data travels here; sounds are Task 9's.
 */
public final class CommandNet {
    private CommandNet() {}
    public static final Identifier COMMAND = Identifier.of("gravesandglory", "command_v1");
    public static final int COOLDOWN = 0, OUTLINE = 1, ACK = 2;
    /**
     * For the harness: per recipient owner and kind (COOLDOWN, OUTLINE, ACK), how many command_v1 records the server addressed to him, counted
     * before the online and channel checks, so a synthetic owner with no connection (Battle.testOwners) counts as a recipient too.
     */
    public static final Map<UUID, int[]> sent = new ConcurrentHashMap<>();

    /** The server side: command_v1 is server to client only, so there is no receiver; the per-recipient tally starts empty. Called from GravesAndGlory.onInitialize. */
    static void registerServer() { sent.clear(); }

    /** The client side (Task 4 calls it from GravesAndGloryClient; idempotent). Kept in Client so a dedicated server never loads client classes. */
    public static void registerClient() { Client.register(); }

    /** A seated commander's charge cooldown, in ticks until ready (0: ready). */
    static void cooldown(ServerPlayerEntity p, int ticks) {
        if (p == null) return;
        tally(p.getUuid(), COOLDOWN);
        PacketByteBuf b = PacketByteBufs.create(); b.writeByte(COOLDOWN); b.writeInt(Math.max(0, ticks));
        send(p, b);
    }

    /** The issuer's Formation or Charge outline, to his teammates only. groups: each group the order named, with its living members. */
    static void outline(GravesAndGlory.Battle battle, GravesAndGlory.Squad issuer, String formation, double x, double z, float yaw, List<Map.Entry<String, Integer>> groups) {
        int slot = teamSlot(battle, issuer);
        for (UUID to : recipients(battle, issuer, false)) {
            tally(to, OUTLINE);
            var p = battle.server.getPlayerManager().getPlayer(to);
            if (p == null) continue;
            PacketByteBuf b = PacketByteBufs.create(); b.writeByte(OUTLINE); b.writeInt(slot); b.writeString(formation); b.writeDouble(x); b.writeDouble(z); b.writeFloat(yaw);
            b.writeInt(groups.size()); for (var g : groups) { b.writeString(g.getKey()); b.writeInt(g.getValue()); }
            send(p, b);
        }
    }

    /** A stored order's acknowledgement, to the issuer and his teammates: the order, the troops that raise their weapons and the pulse centre. */
    static void ack(GravesAndGlory.Battle battle, GravesAndGlory.Squad issuer, String order, int[] units, double x, double z) {
        for (UUID to : recipients(battle, issuer, true)) {
            tally(to, ACK);
            var p = battle.server.getPlayerManager().getPlayer(to);
            if (p == null) continue;
            PacketByteBuf b = PacketByteBufs.create(); b.writeByte(ACK); b.writeString(order);
            b.writeInt(units.length); for (int id : units) b.writeInt(id); b.writeDouble(x); b.writeDouble(z);
            send(p, b);
        }
    }

    /** The owners a team record goes to: every non-bot squad of the issuer's team, the issuer himself only when withIssuer. Never another team's. */
    static List<UUID> recipients(GravesAndGlory.Battle battle, GravesAndGlory.Squad issuer, boolean withIssuer) {
        List<UUID> out = new ArrayList<>();
        for (var s : battle.squads.values()) if (!s.bot && s.team == issuer.team && (withIssuer || !s.owner.equals(issuer.owner))) out.add(s.owner);
        return out;
    }

    /** The issuer's place among his team's squads, in Battle.squads order (-1 if he is not in it). */
    static int teamSlot(GravesAndGlory.Battle battle, GravesAndGlory.Squad issuer) {
        int i = 0;
        for (var s : battle.squads.values()) if (s.team == issuer.team) { if (s == issuer) return i; i++; }
        return -1;
    }

    static void tally(UUID to, int kind) { sent.computeIfAbsent(to, k -> new int[3])[kind]++; }
    /** How many records of kind the server addressed to this owner since the tally was last cleared. */
    public static int sentTo(UUID to, int kind) { var n = sent.get(to); return n == null ? 0 : n[kind]; }

    static void send(ServerPlayerEntity p, PacketByteBuf b) { if (ServerPlayNetworking.canSend(p, COMMAND)) ServerPlayNetworking.send(p, COMMAND, b); }

    /** What the client last heard on command_v1. Task 4 registers it; the panel (Task 7) and outlines and feedback (Task 9) read it. */
    public static final class Client {
        private Client() {}
        /** A teammate's Formation or Charge outline: shown until untilMs (outlineShareTicks after it arrived) or until his troops arrive. own is set for an outline of this client's own order (a client-side addition; the server never sends a commander his own). owner is the sender's team slot and atMs when it arrived: one order's several outlines arrive together, and an owner's later order replaces his earlier ones (OrderOutlines). */
        public record Outline(String formation, double x, double z, float yaw, List<Map.Entry<String, Integer>> groups, long untilMs, boolean own, int owner, long atMs) {}
        /** An order acknowledgement: the order, the troops that raise their weapons, the pulse centre and when it arrived. */
        public record Ack(String order, int[] units, double x, double z, long atMs) {}
        /** Ticks until this commander's Charge is ready, as last sent (0: ready); chargeStampMs is when it arrived, so a reader may count it down. */
        public static volatile int chargeReadyIn;
        public static volatile long chargeStampMs;
        public static final List<Outline> outlines = new CopyOnWriteArrayList<>();
        public static final List<Ack> acks = new CopyOnWriteArrayList<>();
        static boolean registered;

        /** Ticks until Charge is ready now: chargeReadyIn counted down by the time since it arrived. */
        public static int chargeTicksLeft() {
            long gone = (net.minecraft.util.Util.getMeasuringTimeMs() - chargeStampMs) / 50;
            return (int) Math.max(0, chargeReadyIn - gone);
        }
        /** A disconnect: nothing of the last server's orders stays (Task 4 calls it from GravesAndGloryClient.resetHud). */
        public static void reset() { chargeReadyIn = 0; chargeStampMs = 0; outlines.clear(); acks.clear(); }

        static synchronized void register() {
            if (registered) return;
            registered = true;
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(COMMAND, (client, handler, buf, responder) -> {
                int kind = buf.readByte();
                long now = net.minecraft.util.Util.getMeasuringTimeMs();
                if (kind == COOLDOWN) {
                    int ticks = buf.readInt();
                    client.execute(() -> { chargeReadyIn = ticks; chargeStampMs = now; });
                } else if (kind == OUTLINE) {
                    int owner = buf.readInt(); // ownerTeamSlot: which teammate
                    String formation = buf.readString(); double x = buf.readDouble(), z = buf.readDouble(); float yaw = buf.readFloat();
                    int n = buf.readInt(); if (n < 0 || n > 16) return;
                    List<Map.Entry<String, Integer>> groups = new ArrayList<>();
                    for (int i = 0; i < n; i++) groups.add(Map.entry(buf.readString(), buf.readInt()));
                    var outline = new Outline(formation, x, z, yaw, List.copyOf(groups), now + Spec.COMBATDISCIPLINE[0].outlineShareTicks() * 50L, false, owner, now);
                    // one order's outlines arrive within a few ms of each other (several for a multi-group Charge); an earlier order of this owner is replaced
                    client.execute(() -> { outlines.removeIf(o -> o.untilMs() < now || (o.owner() == owner && Math.abs(o.atMs() - now) > 150)); outlines.add(outline); });
                } else if (kind == ACK) {
                    String order = buf.readString();
                    int n = buf.readInt(); if (n < 0 || n > 512) return;
                    int[] units = new int[n]; for (int i = 0; i < n; i++) units[i] = buf.readInt();
                    double x = buf.readDouble(), z = buf.readDouble();
                    var ack = new Ack(order, units, x, z, now);
                    client.execute(() -> { acks.add(ack); while (acks.size() > 32) acks.remove(0); });
                }
            });
        }
    }
}
