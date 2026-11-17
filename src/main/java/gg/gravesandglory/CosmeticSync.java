package gg.gravesandglory;

import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A player's equipped cosmetics and title for modded clients (cosmetics render plan 5), RankSync's pattern: a channel sent only to a client
 * that registered it (VersionGate.canSend), so a plain client never gets it.
 *
 * cosmetic_v1, server to client, one packet per entity: VarInt entity id, VarInt n, n strings "slot=id" (Cosmetics.wire), then the title
 * ("" for none; Cosmetics.wireTitle). The entities are players (their own profile) and the stand of a planted Forward Supply banner (its
 * army's commander's set; a bot's banner sends nothing). It goes on start-tracking, to a client that registers the channel late (every
 * entity it tracks, and the player's own), and to every tracker and the player himself when he equips or changes title (changed). A client
 * keeps the entries by entity id, drops one when its entity unloads and empties the store on disconnect.
 */
public final class CosmeticSync {
    private CosmeticSync() {}
    public static final Identifier CHANNEL = Identifier.of("gravesandglory", "cosmetic_v1");
    /** The most slot pairs a packet carries, and the longest string; the reader refuses more. */
    static final int MAX_PAIRS = 16, MAX_STRING = 64;

    /** One entity's set as the client holds it: the slot map and the title ("" for none). */
    public record Entry(Map<String, String> slots, String title) {
        static final Entry NONE = new Entry(Map.of(), "");
        boolean empty() { return slots.isEmpty() && title.isEmpty(); }
    }

    /** Packets sent, by trigger, and the times a send was skipped because the client had not registered the channel (the harness reads them). */
    static volatile int trackingSends, registerSends, changeSends, skippedPlain;

    // ---- server ----

    /** The set an entity carries on the server: a player's own, a supply banner stand's commander's; null for anything else or a bot's banner. */
    static Entry of(Entity e) {
        var b = GravesAndGlory.battle;
        if (b == null || e == null) return null;
        UUID owner = null;
        if (e instanceof ServerPlayerEntity p) owner = p.getUuid();
        else if (e instanceof GravesAndGlory.ArmySupplyBanner banner && banner.squad() != null && !banner.squad().bot) owner = banner.squad().owner;
        if (owner == null) return null;
        Profile pr = b.campaign.profile(owner);
        return new Entry(Cosmetics.normalize(pr.cosmetics, pr.cosmeticSlots), Cosmetics.wireTitle(pr.progression == null ? null : pr.progression.title));
    }

    static PacketByteBuf packet(int entityId, Entry e) {
        var buf = PacketByteBufs.create();
        buf.writeVarInt(entityId);
        List<String> pairs = Cosmetics.wire(e.slots());
        buf.writeVarInt(pairs.size());
        for (String s : pairs) buf.writeString(s, MAX_STRING);
        buf.writeString(e.title(), MAX_STRING);
        return buf;
    }

    /** Sends an entity's set to each of these players that takes cosmetic_v1. Returns how many it sent. */
    static int send(Entity e, Collection<ServerPlayerEntity> players) {
        Entry entry = of(e);
        if (entry == null) return 0;
        int sent = 0;
        for (ServerPlayerEntity p : players) {
            if (!VersionGate.canSend(p, CHANNEL)) { skippedPlain++; continue; }
            ServerPlayNetworking.send(p, gg.gravesandglory.net.Payloads.CosmeticPayload.of(packet(e.getId(), entry)));
            sent++;
        }
        return sent;
    }

    /** A player's set changed (equip, title, a grant that equipped Proven): every tracker and the player himself learn it, and so does each of his planted banners. */
    public static void changed(ServerPlayerEntity p) {
        if (p == null) return;
        List<ServerPlayerEntity> to = new ArrayList<>(PlayerLookup.tracking(p));
        if (!to.contains(p)) to.add(p);
        changeSends += send(p, to);
        var b = GravesAndGlory.battle;
        if (b != null) for (var s : List.copyOf(b.structures)) if (s instanceof GravesAndGlory.ArmySupplyBanner banner && banner.squad() != null && p.getUuid().equals(banner.squad().owner)) changeSends += send(banner, PlayerLookup.tracking(banner));
    }

    /** The player's own set and that of every player and planted banner he tracks: for a client that registered cosmetic_v1 after its trackers started. */
    static int sendTracked(ServerPlayerEntity p) {
        int sent = send(p, List.of(p));
        var world = p.getServerWorld();
        for (ServerPlayerEntity other : world.getPlayers()) if (other != p && PlayerLookup.tracking(other).contains(p)) sent += send(other, List.of(p));
        var b = GravesAndGlory.battle;
        if (b != null) for (var s : List.copyOf(b.structures)) if (s instanceof GravesAndGlory.ArmySupplyBanner banner && PlayerLookup.tracking(banner).contains(p)) sent += send(banner, List.of(p));
        return sent;
    }

    /** START_TRACKING (a player or a supply banner stand coming into view: the stand's squad is set before spawnEntity) and the late registration. */
    public static void registerServer() {
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            if (entity instanceof ServerPlayerEntity || entity instanceof GravesAndGlory.ArmySupplyBanner) trackingSends += send(entity, List.of(player));
        });
        // a respawn or a world change makes a new client entity, whose unload dropped the player's own entry; the server never tracks him to himself
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((old, player, alive) -> changed(player));
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> changed(player));
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (!channels.contains(CHANNEL)) return;
            ServerPlayerEntity joined = handler.player;
            server.execute(() -> registerSends += sendTracked(joined));
        });
    }

    // ---- client ----

    /** The client's store, by entity id. */
    static final Map<Integer, Entry> ENTRIES = new ConcurrentHashMap<>();
    /** cosmetic_v1 packets this client read (the harness reads it). */
    static volatile int received;

    /** The set an entity draws in: for the local player the Wardrobe preview while one runs (Cosmetics.previewOverride), else what the server sent; NONE when unknown. */
    public static Entry client(Entity e) { return Client.entry(e); }

    static void accept(int entityId, List<String> pairs, String title) {
        received++;
        Entry e = new Entry(Cosmetics.unwire(pairs), title == null ? "" : title);
        if (e.empty()) ENTRIES.remove(entityId); else ENTRIES.put(entityId, e);
    }

    /** Forgets every entry (disconnect). */
    public static void clear() { ENTRIES.clear(); }

    /** Registers the client receiver and the unload hook (GravesAndGloryClient). */
    public static void registerClient() { Client.register(); }

    /** The client-only half, a nested class a dedicated server never loads. */
    static final class Client {
        private Client() {}
        static Entry entry(Entity e) {
            if (e == null) return Entry.NONE;
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            var preview = Cosmetics.previewOverride;
            Entry sent = ENTRIES.get(e.getId());
            if (preview != null && mc != null && e == mc.player) return new Entry(preview, sent == null ? "" : sent.title());
            var world = e.getWorld();
            return sent != null && world != null && world.getEntityById(e.getId()) == e ? sent : Entry.NONE;
        }
        static void register() {
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(gg.gravesandglory.net.Payloads.CosmeticPayload.ID, (payload, context) -> {
                var buf = payload.buf();
                int id = buf.readVarInt(), n = buf.readVarInt();
                if (n < 0 || n > MAX_PAIRS) return;
                List<String> pairs = new ArrayList<>();
                for (int i = 0; i < n; i++) pairs.add(buf.readString(MAX_STRING));
                String title = buf.readString(MAX_STRING);
                context.client().execute(() -> accept(id, pairs, title));
            });
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_UNLOAD.register((entity, world) -> ENTRIES.remove(entity.getId()));
        }
    }
}
