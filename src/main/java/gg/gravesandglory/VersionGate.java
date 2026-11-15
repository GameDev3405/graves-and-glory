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
 * is refused with {@link CompatibilityPolicy#required} (the 1.20.1 outcome). P3 turns that branch into "admit as plain".
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

    /** The configuration check's result for a connected (or configuring) player, empty if none ran. For the P3.1 plain flag. */
    public static Optional<Result> result(UUID player) { return Optional.ofNullable(RESULTS.get(player)); }

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

    /** The configuration task: sends the empty request; the reply receiver completes it. */
    record CheckTask() implements ServerPlayerConfigurationTask {
        static final Key KEY = new Key(CONFIG_CHANNEL.toString());
        @Override public void sendPacket(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new Payloads.ReleaseCheckRequestPayload(new byte[0])));
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
            if (ServerConfigurationNetworking.canSend(handler, Payloads.ReleaseCheckRequestPayload.ID)) {
                PENDING.add(id);
                handler.addTask(new CheckTask());
                return;
            }
            RESULTS.put(id, new Result(Kind.PLAIN, "", ""));
            if (GravesAndGlory.battle != null) {
                RESULTS.put(id, new Result(Kind.REFUSED, "", ""));
                System.out.println("[Graves and Glory] Refused a client without the mod during configuration");
                handler.disconnect(Text.literal(CompatibilityPolicy.required(VERSION, BUILD)));
            }
        });
        ServerConfigurationNetworking.registerGlobalReceiver(Payloads.ReleaseCheckReplyPayload.ID, (payload, context) -> {
            var handler = context.networkHandler();
            UUID id = handler.getDebugProfile().getId();
            if (!PENDING.remove(id)) return; // unsolicited or repeated: completeTask would throw for a task that is not current
            String[] reported = {"", ""};
            String failure = check(payload.buf(), reported);
            // The 1.20.1 gate refused a mismatch only while a battle existed; the same holds here.
            boolean refuse = !failure.isEmpty() && GravesAndGlory.battle != null;
            RESULTS.put(id, new Result(refuse ? Kind.REFUSED : Kind.MODDED, reported[0], reported[1]));
            context.server().execute(() -> {
                if (refuse) {
                    System.out.println("[Graves and Glory] Refused incompatible client during configuration");
                    handler.disconnect(Text.literal(failure));
                } else {
                    if (failure.isEmpty()) Diagnostics.report("client-compatibility", true, "Matching release accepted before world admission");
                    handler.completeTask(CheckTask.KEY);
                }
            });
        });
        ServerConfigurationConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.getDebugProfile().getId()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player.getUuid()));
    }

    private static void forget(UUID id) { RESULTS.remove(id); PENDING.remove(id); }

    /** The 1.20.1 login query, unchanged on the wire; kept as a fallback until the configuration gate is proven in game. */
    private static void registerLogin() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) -> {
            if (GravesAndGlory.battle != null) sender.sendPacket(CHANNEL, PacketByteBufs.empty());
        });
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, handler, understood, buffer, sync, sender) -> {
            final String reason = understood ? check(buffer, new String[2]) : CompatibilityPolicy.required(VERSION, BUILD);
            sync.waitFor(server.submit(() -> {
                if (!reason.isEmpty()) {
                    System.out.println("[Graves and Glory] Refused incompatible client during login");
                    handler.disconnect(Text.literal(reason));
                } else Diagnostics.report("client-compatibility", true, "Matching release accepted before world admission");
            }));
        });
    }
}
