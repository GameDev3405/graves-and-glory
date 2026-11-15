package gg.gravesandglory;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Refuse incompatible clients during LOGIN, before any profile or battle packet. */
public final class VersionGate {
    public static final Identifier CHANNEL = Identifier.of("gravesandglory", "release_check_v1");
    public static final String VERSION = FabricLoader.getInstance().getModContainer("gravesandglory")
            .orElseThrow().getMetadata().getVersion().getFriendlyString();
    public static final String BUILD = readBuild();
    private VersionGate() {}

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

    public static PacketByteBuf response() {
        var packet = PacketByteBufs.create();
        packet.writeVarInt(CompatibilityPolicy.PROTOCOL);
        packet.writeString(VERSION, 64);
        packet.writeString(BUILD, 64);
        return packet;
    }

    public static void registerServer() {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, sync) -> {
            if (GravesAndGlory.battle != null) sender.sendPacket(CHANNEL, PacketByteBufs.empty());
        });
        ServerLoginNetworking.registerGlobalReceiver(CHANNEL, (server, handler, understood, buffer, sync, sender) -> {
            String failure;
            if (!understood) failure = CompatibilityPolicy.required(VERSION, BUILD);
            else {
                try {
                    int protocol = buffer.readVarInt();
                    String version = buffer.readString(64), build = buffer.readString(64);
                    failure = CompatibilityPolicy.failure(VERSION, BUILD, protocol, version, build);
                    if (buffer.isReadable()) failure = "Invalid modpack compatibility response. " + CompatibilityPolicy.required(VERSION, BUILD);
                } catch (RuntimeException malformed) {
                    failure = "Invalid modpack compatibility response. " + CompatibilityPolicy.required(VERSION, BUILD);
                }
            }
            final String reason = failure;
            sync.waitFor(server.submit(() -> {
                if (!reason.isEmpty()) {
                    System.out.println("[Graves and Glory] Refused incompatible client during login");
                    handler.disconnect(Text.literal(reason));
                } else Diagnostics.report("client-compatibility", true, "Matching release accepted before world admission");
            }));
        });
    }
}
