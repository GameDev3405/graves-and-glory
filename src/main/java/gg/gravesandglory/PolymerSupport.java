package gg.gravesandglory;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Whether Polymer runs in this JVM (plain clients, port plan P3). Polymer is a server-only, optional dependency: the modpack never
 * carries it. Every Polymer type lives in gg.gravesandglory.polymer, which loads only when this is true (PolymerBridge, and the
 * mixins of gravesandglory.polymer.mixins.json, whose plugin applies them only then). Code outside that package names no Polymer
 * type, so a modpack client, its integrated server and a server without Polymer load the mod exactly as before.
 */
public final class PolymerSupport {
    private PolymerSupport() {}
    public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("polymer-core");
}
