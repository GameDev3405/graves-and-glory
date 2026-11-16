package gg.gravesandglory;

import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;

/**
 * The raw ids of the entries a Polymer server keeps out of Fabric's registry sync (plain clients, port plan P3): the supply wagon's
 * entity type and every state of the two map blocks. Sounds are not here: Polymer sends them by name to every client.
 *
 * <p>With Polymer on the server these entries are server-only, so Fabric never remaps them on a modpack client; the client keeps its
 * own raw ids. A modpack client may be shown the real wagon and blocks only when its ids equal the server's: the client sends its map
 * in the client_hello_v1 reply, and {@link #matches} compares it. Any difference (a client mod that registers into these registries,
 * say) leaves that player on the plain stand-ins, which are always safe to decode.
 */
public final class RegistryFingerprint {
    private RegistryFingerprint() {}

    /** This side's raw ids, by key ("entity_type/(id)", "block_state/(id)/(index)"). */
    public static Map<String, Integer> local() {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("entity_type/" + Registries.ENTITY_TYPE.getId(WagonEntity.TYPE), Registries.ENTITY_TYPE.getRawId(WagonEntity.TYPE));
        for (Block b : new Block[]{MapBlocks.TROOP_FENCE, MapBlocks.STILL_WATER}) {
            var states = b.getStateManager().getStates();
            for (int i = 0; i < states.size(); i++) out.put("block_state/" + Registries.BLOCK.getId(b) + "/" + i, Block.getRawIdFromState(states.get(i)));
        }
        return out;
    }

    /** The reply body: varInt n, then n times string key (128) and varInt raw id. */
    public static PacketByteBuf write() {
        var buf = PacketByteBufs.create();
        var map = local();
        buf.writeVarInt(map.size());
        for (var e : map.entrySet()) { buf.writeString(e.getKey(), 128); buf.writeVarInt(e.getValue()); }
        return buf;
    }

    /** Reads a reply body; null when malformed or over 64 entries. */
    public static Map<String, Integer> read(PacketByteBuf buf) {
        try {
            int n = buf.readVarInt();
            if (n < 0 || n > 64) return null;
            Map<String, Integer> out = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) out.put(buf.readString(128), buf.readVarInt());
            return buf.isReadable() ? null : out;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    /** Whether a client's map equals this server's, key for key. */
    public static boolean matches(Map<String, Integer> client) {
        return client != null && client.equals(local());
    }
}
