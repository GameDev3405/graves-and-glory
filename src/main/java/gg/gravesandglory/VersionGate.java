package gg.gravesandglory;

import com.google.gson.JsonParser;
import gg.gravesandglory.net.Payloads;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerConfigurationTask;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Refuses incompatible clients before world admission, and remembers per connection what each client is (port P1.3).
 *
 * <p>Primary gate (1.21.1): a configuration-phase task on {@link #CONFIG_CHANNEL} (release_check_v2). On CONFIGURE the server
 * looks at the channels the client registered. A client that can receive release_check_v2 gets a task: an empty S2C request,
 * answered by a C2S reply whose body is {@link #response()} (varInt protocol, string version (64), string build (64)), the same
 * bytes the 1.20.1 login answer carried. The server checks it with {@link CompatibilityPolicy#failure} and either disconnects with
 * the refusal text or completes the task. A client without the channel is recorded as {@link Kind#PLAIN}; while a battle exists it
 * is refused with {@link CompatibilityPolicy#required} (the 1.20.1 outcome), unless the server admits plain clients
 * ({@link PlainClients#ON}: Polymer loaded and -Dgravesandglory.plainClients=true), when it joins as plain.
 *
 * <p>Plain clients (port plan P3.1): the flag is final before play starts, because Polymer picks each viewer's entity types and
 * blocks when it writes the packets. A client that registered release_check_v2 must answer within {@link #GRACE_MS}, or it is
 * refused. A modpack client that also registered client_hello_v1 gets a second task: its reply carries its raw ids of the
 * server-only entries ({@link RegistryFingerprint}); a mismatch or a missing reply within the grace leaves it on the plain
 * stand-ins for the wagon and the map blocks ({@link #rawIds}).
 *
 * <p>Fallback (kept until the dedicated-server test passes): the 1.20.1 login query on {@link #CHANNEL} (release_check_v1). It is
 * still registered and refuses the same clients; start the server with {@code -Dgravesandglory.loginGate=false} to switch it off
 * (for a proxy that does not forward login queries).
 */
public final class VersionGate {
    public static final Identifier CHANNEL = Identifier.of("gravesandglory", "release_check_v1");
    public static final Identifier CONFIG_CHANNEL = Payloads.RELEASE_CHECK;
    public static final String VERSION = FabricLoader.getInstance().getModContainer("gravesandglory")
            .orElseThrow().getMetadata().getVersion().getFriendlyString();
    public static final String BUILD = readBuild();
    private VersionGate() {}

    /** What the configuration check found for one connection. */
    public enum Kind { MODDED, PLAIN, REFUSED }
    /** The check's result for one connection: the kind, and the version and build a modded client reported ("" otherwise). */
    public record Result(Kind kind, String version, String build) {}

    /** Results by profile id, from CONFIGURE until the connection closes (config or play disconnect). */
    private static final Map<UUID, Result> RESULTS = new ConcurrentHashMap<>();
    /** Connections with a check sent and no reply handled yet; a reply from anyone else is ignored. */
    private static final Set<UUID> PENDING = ConcurrentHashMap.newKeySet();
    /** Connections with a hello sent and no reply handled yet. */
    private static final Set<UUID> HELLO_PENDING = ConcurrentHashMap.newKeySet();
    /** Modpack connections whose raw ids of the server-only entries equal the server's (client_hello_v1). */
    private static final Set<UUID> RAW_IDS = ConcurrentHashMap.newKeySet();
    /** A configuring connection's open task (check or hello) and when its request went out, for the grace. */
    private record Waiting(ServerConfigurationNetworkHandler handler, ServerPlayerConfigurationTask.Key task, long since) {}
    private static final Map<UUID, Waiting> WAITING = new ConcurrentHashMap<>();
    /** How long a client that registered a channel has to answer its request. */
    public static final long GRACE_MS = 5000;
    /** Optional: where to get the modpack, added to every refusal (-Dgravesandglory.modpackUrl). */
    static final String MODPACK_URL = System.getProperty("gravesandglory.modpackUrl", "").trim();

    /** The configuration check's result for a connected (or configuring) player, empty if none ran. For the P3.1 plain flag. */
    public static Optional<Result> result(UUID player) { return Optional.ofNullable(RESULTS.get(player)); }

    /** The per-player flag: true for a client without the modpack (no release_check_v2 channel). Every connection passes the
     *  configuration check before play, so a player with no result (a player object with no real connection) counts as modded. */
    public static boolean plain(ServerPlayerEntity p) {
        if (p == null) return false;
        var r = RESULTS.get(p.getUuid());
        return r != null && r.kind() == Kind.PLAIN;
    }
    public static boolean modded(ServerPlayerEntity p) { return p != null && !plain(p); }
    /** Whether this player may be sent the real wagon and map blocks: a modpack client whose raw ids match the server's. */
    public static boolean rawIds(ServerPlayerEntity p) { return modded(p) && RAW_IDS.contains(p.getUuid()); }
    /** The guard for every custom payload send (P3.3): never to a plain client, and only on a channel the client registered. */
    public static boolean canSend(ServerPlayerEntity p, Identifier channel) { return modded(p) && ServerPlayNetworking.canSend(p, channel); }
    public static boolean canSend(ServerPlayerEntity p, net.minecraft.network.packet.CustomPayload.Id<?> id) { return modded(p) && ServerPlayNetworking.canSend(p, id); }

    /** A refusal's text with the modpack link, when the server names one. */
    public static String refusal(String text) { return MODPACK_URL.isEmpty() ? text : text + " Download: " + MODPACK_URL; }

    private static String readBuild() {
        try (var stream = VersionGate.class.getResourceAsStream("/gravesandglory-build.json")) {
            if (stream == null) throw new IllegalStateException("Release identity is missing; rebuild the mod");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            String hash = json.get("sourceSha256").getAsString();
            if (!hash.matches("[0-9a-f]{64}") || !VERSION.equals(json.get("version").getAsString()))
                throw new IllegalStateException("Release identity is invalid; rebuild the mod");
            return hash;
        } catch (java.io.IOException error) { throw new IllegalStateException("Could not read release identity", error); }
    }

    /** The client's answer body, for both the login query and the configuration reply. */
    public static PacketByteBuf response() {
        var packet = PacketByteBufs.create();
        packet.writeVarInt(CompatibilityPolicy.PROTOCOL);
        packet.writeString(VERSION, 64);
        packet.writeString(BUILD, 64);
        return packet;
    }

    /** Reads an answer body and returns the refusal text ("" accepts); a malformed or over-long body is refused. */
    static String check(PacketByteBuf buffer, String[] reported) {
        try {
            int protocol = buffer.readVarInt();
            String version = buffer.readString(64), build = buffer.readString(64);
            if (buffer.isReadable()) return "Invalid modpack compatibility response. " + CompatibilityPolicy.required(VERSION, BUILD);
            reported[0] = version; reported[1] = build;
            return CompatibilityPolicy.failure(VERSION, BUILD, protocol, version, build);
        } catch (RuntimeException malformed) {
            return "Invalid modpack compatibility response. " + CompatibilityPolicy.required(VERSION, BUILD);
        }
    }

    /** The configuration task: sends the empty request and only then accepts a reply from this player; the receiver completes it. */
    record CheckTask(UUID player, ServerConfigurationNetworkHandler handler) implements ServerPlayerConfigurationTask {
        static final Key KEY = new Key(CONFIG_CHANNEL.toString());
        @Override public void sendPacket(Consumer<Packet<?>> sender) {
            PENDING.add(player);
            WAITING.put(player, new Waiting(handler, KEY, System.currentTimeMillis())); // the grace starts when the request is sent
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new Payloads.ReleaseCheckRequestPayload(new byte[0])));
        }
        @Override public Key getKey() { return KEY; }
    }

    /** The hello task (P3.1): an empty client_hello_v1 request; the reply (RegistryFingerprint) completes it. */
    record HelloTask(UUID player, ServerConfigurationNetworkHandler handler) implements ServerPlayerConfigurationTask {
        static final Key KEY = new Key(Payloads.CLIENT_HELLO.toString());
        @Override public void sendPacket(Consumer<Packet<?>> sender) {
            HELLO_PENDING.add(player);
            WAITING.put(player, new Waiting(handler, KEY, System.currentTimeMillis()));
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new Payloads.ClientHelloRequestPayload(new byte[0])));
        }
        @Override public Key getKey() { return KEY; }
    }

    public static void registerServer() {
        registerConfiguration();
        if (!"false".equals(System.getProperty("gravesandglory.loginGate"))) registerLogin();
    }

    /** The primary gate. CONFIGURE and the reply receiver run on netty threads; the outcome is applied on the server thread. */
    private static void registerConfiguration() {
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            UUID id = handler.getDebugProfile().getId();
            forget(id); // nothing left over from an earlier connection of this UUID
            if (ServerConfigurationNetworking.canSend(handler, Payloads.ReleaseCheckRequestPayload.ID)) {
                handler.addTask(new CheckTask(id, handler));
                if (ServerConfigurationNetworking.canSend(handler, Payloads.ClientHelloRequestPayload.ID)) handler.addTask(new HelloTask(id, handler));
                return;
            }
            RESULTS.put(id, new Result(Kind.PLAIN, "", ""));
            if (GravesAndGlory.battle != null && !PlainClients.ON) {
                RESULTS.put(id, new Result(Kind.REFUSED, "", ""));
                System.out.println("[Graves and Glory] Refused a client without the mod during configuration");
                handler.disconnect(Text.literal(refusal(CompatibilityPolicy.required(VERSION, BUILD))));
            } else if (PlainClients.ON) System.out.println("[Graves and Glory] " + handler.getDebugProfile().getName() + " joins without the modpack (plain)");
        });
        ServerConfigurationNetworking.registerGlobalReceiver(Payloads.ClientHelloReplyPayload.ID, (payload, context) -> {
            var handler = context.networkHandler();
            UUID id = handler.getDebugProfile().getId();
            if (!HELLO_PENDING.remove(id)) return;
            var ids = RegistryFingerprint.read(payload.buf());
            context.server().execute(() -> {
                boolean match = RegistryFingerprint.matches(ids);
                if (match) RAW_IDS.add(id); else RAW_IDS.remove(id);
                System.out.println("[Graves and Glory] " + handler.getDebugProfile().getName() + " joins with the modpack; raw ids of the server-only entries "
                    + (match ? "match" : "differ (shown the stand-ins): client " + ids + ", server " + RegistryFingerprint.local()));
                WAITING.remove(id);
                handler.completeTask(HelloTask.KEY);
            });
        });
        // The grace: a client that registered a channel and leaves its request unanswered is refused (check) or left on the stand-ins (hello).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (WAITING.isEmpty()) return;
            long now = System.currentTimeMillis();
            for (var e : WAITING.entrySet()) {
                var w = e.getValue();
                if (now - w.since() < GRACE_MS) continue;
                UUID id = e.getKey();
                WAITING.remove(id);
                if (w.task() == CheckTask.KEY && PENDING.remove(id)) {
                    RESULTS.put(id, new Result(Kind.REFUSED, "", ""));
                    System.out.println("[Graves and Glory] Refused a client that did not answer the modpack check within " + GRACE_MS + " ms");
                    w.handler().disconnect(Text.literal(refusal("Your client did not answer the modpack check. " + CompatibilityPolicy.required(VERSION, BUILD))));
                } else if (w.task() == HelloTask.KEY && HELLO_PENDING.remove(id)) {
                    RAW_IDS.remove(id);
                    System.out.println("[Graves and Glory] No client_hello_v1 reply within " + GRACE_MS + " ms; the client is shown the stand-ins");
                    w.handler().completeTask(HelloTask.KEY);
                }
            }
        });
        ServerConfigurationNetworking.registerGlobalReceiver(Payloads.ReleaseCheckReplyPayload.ID, (payload, context) -> {
            var handler = context.networkHandler();
            UUID id = handler.getDebugProfile().getId();
            if (!PENDING.remove(id)) return; // early, unsolicited or repeated: completeTask would throw for a task that is not current
            String[] reported = {"", ""};
            String failure = check(payload.buf(), reported);
            // The 1.20.1 gate refused a mismatch only while a battle existed; the same holds here.
            boolean refuse = !failure.isEmpty() && GravesAndGlory.battle != null;
            RESULTS.put(id, new Result(refuse ? Kind.REFUSED : Kind.MODDED, reported[0], reported[1]));
            context.server().execute(() -> {
                if (refuse) {
                    WAITING.remove(id);
                    System.out.println("[Graves and Glory] Refused incompatible client during configuration");
                    handler.disconnect(Text.literal(refusal(failure)));
                } else {
                    WAITING.remove(id); // the hello task, when the client registered it, starts next with its own timer
                    if (failure.isEmpty()) Diagnostics.report("client-compatibility", true, "Matching release accepted before world admission");
                    handler.completeTask(CheckTask.KEY);
                }
            });
        });
        ServerConfigurationConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getDebugProfile().getId()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player.getUuid()));
    }

    private static void forget(UUID id) { RESULTS.remove(id); PENDING.remove(id); HELLO_PENDING.remove(id); RAW_IDS.remove(id); WAITING.remove(id); }

    /** The 1.20.1 login query, unchanged on the wire; kept as a fallback until the configuration gate is proven in game. */
    private static void registerLogin() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) -> {
            if (GravesAndGlory.battle != null) sender.sendPacket(CHANNEL, PacketByteBufs.empty());
        });
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, handler, understood, buffer, sync, sender) -> {
            // With plain clients admitted, a client that does not understand the query is a vanilla client: it passes here and is
            // flagged plain in configuration. A modpack client still answers, and a wrong answer is still refused.
            final String reason = understood ? check(buffer, new String[2]) : PlainClients.ON ? "" : CompatibilityPolicy.required(VERSION, BUILD);
            sync.waitFor(server.submit(() -> {
                if (!reason.isEmpty()) {
                    System.out.println("[Graves and Glory] Refused incompatible client during login");
                    handler.disconnect(Text.literal(refusal(reason)));
                } else Diagnostics.report("client-compatibility", true, "Matching release accepted before world admission");
            }));
        });
    }
}
