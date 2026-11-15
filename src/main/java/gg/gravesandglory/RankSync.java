package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A soldier's rank for its clients (in-match progression design 7): his level and the weapon and armour tiers he wears. The troop renderer picks
 * the armour skin, the weapon model and the chevrons by it (Task 9); a portrait sets its own (setLocal).
 *
 * rank_v1, server to client, one packet per soldier: VarInt entity id, byte level (1 to 3), byte weaponTier (1 to 3), byte armourTier (1 to 3).
 * An entity id of -1 (CLEAR, the three bytes 0) tells the client to forget every rank: a round boundary (startRound, Campaign.returnLobby,
 * switchMap), sent only when a rank has gone out since the last one (out), so a returnLobby and the startRound after it send one. It is sent as stance_v1 is: to a player who starts tracking a troop (ahead of the entity's spawn packet; the client keeps the rank
 * for an id it does not have yet), to a player whose client registers the channel only after its trackers started (a guest joining a battle in
 * progress gets every tracked soldier's rank), and on each change to every player tracking the soldier (changed: level ups and gear, Task 4 and 5).
 * The client drops an entry when its entity unloads and empties the store on disconnect.
 *
 * The client side lives in Client, a nested class a dedicated server never loads.
 */
public final class RankSync {
    private RankSync() {}
    public static final Identifier CHANNEL = Identifier.of("gravesandglory", "rank_v1");
    /** The entity id that means "forget every rank". */
    public static final int CLEAR = -1;

    /** A soldier's rank: his level and his worn weapon and armour tiers. */
    public record Rank(int level, int weaponTier, int armourTier) {
        /** What every soldier holds when trained or rebought, and what a troop draws at before its first rank_v1. */
        public static final Rank FRESH = new Rank(1, 1, 1);
    }

    /** rank_v1 packets sent: on a change (changed), to a player who starts tracking a troop, to a player whose client registers the channel late, and the round-boundary clears (the harness reads them). */
    static volatile int changeSends, trackingSends, registerSends, clearSends;

    // ---- server ----

    /** The rank a unit holds now. */
    static Rank of(GravesAndGlory.Unit u) { return new Rank(u.level, u.weaponTier, u.armourTier); }

    /** Writes one rank_v1 packet: the entity id and the three bytes. */
    static PacketByteBuf packet(int entityId, Rank r) {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(entityId); buf.writeByte(r.level()); buf.writeByte(r.weaponTier()); buf.writeByte(r.armourTier());
        return buf;
    }

    /**
     * Whether any rank has gone to a client since the last CLEAR went out. Only then does a client hold a rank, so only then is there anything to clear: a
     * boundary right after another (Campaign.returnLobby, then the next round's startRound) sends its clients one CLEAR, not two. A connection starts with no
     * ranks (the client empties its store on disconnect), so none is owed to a client that joins between boundaries. Server thread.
     */
    static volatile boolean out;

    /** Sends a unit's rank to each of these players that takes rank_v1. Returns how many it sent. */
    static int send(GravesAndGlory.Unit u, Collection<ServerPlayerEntity> players) {
        Rank r = of(u);
        int sent = 0;
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, CHANNEL)) continue;
            ServerPlayNetworking.send(p, gg.gravesandglory.net.Payloads.RankPayload.of(packet(u.mob.getId(), r)));
            sent++;
        }
        if (sent > 0) out = true;
        return sent;
    }

    /** The unit's rank changed (a level up, a gear buy worn): every player tracking it learns the new one. */
    static void changed(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        if (b == null || u == null) return;
        changeSends += send(u, PlayerLookup.tracking(u.mob));
    }

    /** Sends a player the rank of every troop it tracks: for a player whose client registered rank_v1 after its trackers started. Returns how many it sent. */
    static int sendTracked(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        int sent = 0;
        for (GravesAndGlory.Unit u : b.units) if (u.mob.isAlive() && PlayerLookup.tracking(u.mob).contains(p)) sent += send(u, List.of(p));
        return sent;
    }

    /** A round boundary: every client that takes rank_v1 forgets every rank (Battle.clearProgression), unless none was sent since the last time (out). */
    static void clear(GravesAndGlory.Battle b) {
        if (b == null || !out) return;
        out = false;
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            if (!ServerPlayNetworking.canSend(p, CHANNEL)) continue;
            ServerPlayNetworking.send(p, gg.gravesandglory.net.Payloads.RankPayload.of(packet(CLEAR, new Rank(0, 0, 0))));
            clearSends++;
        }
    }

    /**
     * The server's two sends that copy stance_v1's: a player who starts tracking a troop learns its rank (START_TRACKING fires inside spawnEntity,
     * so a troop Battle.spawn is placing is found through Battle.spawning), and a player whose client registers rank_v1 only after its trackers
     * started gets every tracked troop's rank.
     */
    static void registerServer() {
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            var b = GravesAndGlory.battle;
            if (b == null || !ArmyMobs.isTroop(entity)) return;
            GravesAndGlory.Unit u = b.spawning != null && b.spawning.mob == entity ? b.spawning : null;
            if (u == null) for (GravesAndGlory.Unit x : b.units) if (x.mob == entity) { u = x; break; }
            if (u != null) trackingSends += send(u, List.of(player));
        });
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (!channels.contains(CHANNEL)) return;
            ServerPlayerEntity joined = handler.player;
            server.execute(() -> { var b = GravesAndGlory.battle; if (b != null) registerSends += sendTracked(b, joined); });
        });
    }

    // ---- client ----

    /** The ranks the server sent, by entity id (a client-world troop). */
    static final Map<Integer, Rank> RANKS = new ConcurrentHashMap<>();
    /** Ranks set on client-only entities (portraits), by the entity object itself (identity, weak, thread-safe): Entity.equals compares ids, and a portrait's client-made id can equal a world troop's network id on a multiplayer guest. */
    static final Map<Entity, Rank> LOCAL = new MapMaker().weakKeys().makeMap();
    /** rank_v1 packets this client read (the harness reads it). */
    static volatile int received;

    /** The rank the client holds for an entity: a portrait's own (setLocal), else what the server sent for this world entity; null when unknown (draw at Rank.FRESH). */
    public static Rank client(Entity e) {
        if (e == null) return null;
        Rank local = LOCAL.get(e);
        if (local != null) return local;
        var world = e.getWorld();
        return world != null && world.getEntityById(e.getId()) == e ? RANKS.get(e.getId()) : null;
    }

    /** Sets the rank of a client-only entity (a portrait mob, ArmyCards.portrait), which the server never sends. */
    public static void setLocal(Entity e, Rank r) {
        if (e == null) return;
        if (r == null) LOCAL.remove(e); else LOCAL.put(e, r);
    }

    /** Forgets every server-sent rank (disconnect, a round boundary). Portrait ranks stay with their portraits. */
    public static void clear() { RANKS.clear(); }

    /** Forgets one entity's rank (its entity unloaded on the client). */
    static void forget(int entityId) { RANKS.remove(entityId); }

    /** Applies one rank_v1 packet's fields on the client thread. */
    static void accept(int entityId, int level, int weaponTier, int armourTier) {
        received++;
        if (entityId == CLEAR) { clear(); return; }
        RANKS.put(entityId, new Rank(level, weaponTier, armourTier));
    }

    /** Registers the client receiver (GravesAndGloryClient). */
    public static void registerClient() { Client.register(); }

    /** The client-only half: the receiver and the unload hook. */
    static final class Client {
        private Client() {}
        static void register() {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(gg.gravesandglory.net.Payloads.RankPayload.ID, (payload, context) -> {
                var client = context.client(); var buf = payload.buf();
                int id = buf.readVarInt(), level = buf.readByte(), weapon = buf.readByte(), armour = buf.readByte();
                client.execute(() -> accept(id, level, weapon, armour));
            });
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> forget(entity.getId()));
        }
    }
}
