package gg.gravesandglory.polymer;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.other.PolymerSoundEvent;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import gg.gravesandglory.PlainPack;
import gg.gravesandglory.WagonEntity;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Every call into Polymer (plain clients, port plan P3). Loaded only when PolymerSupport.LOADED: callers check it first, so a JVM
 * without Polymer never resolves this class.
 */
public final class PolymerBridge {
    private PolymerBridge() {}
    private static final Map<String, PolymerModelData> ICONS = new LinkedHashMap<>();

    /** A mod sound: Polymer keeps it out of the registry sync and sends it to every client by its id (the pack or the mod has it). */
    public static SoundEvent sound(Identifier id) {
        return PolymerSoundEvent.of(id, null);
    }

    /** At mod init, before the registries freeze: the wagon type becomes server-only (WagonPolymerMixin gives each viewer its look),
     *  the order icons get their custom model data, and the plain pack's files are added when Polymer builds it. */
    public static void init() {
        PolymerEntityUtils.registerType(WagonEntity.TYPE);
        for (String order : PlainPack.ORDERS)
            ICONS.put(order, PolymerResourcePackUtils.requestModel(Items.PAPER, Identifier.of(PlainPack.MOD, "item/plain_" + order)));
        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> PlainPack.addFiles(builder::addData));
    }

    /** An order's hotbar icon (paper with the order's model), or null before init. */
    public static ItemStack icon(String order) {
        var m = ICONS.get(order);
        return m == null ? null : m.asStack();
    }

    /** Builds the plain pack to the given zip; false on failure. */
    public static boolean buildPack(Path zip) {
        return PolymerResourcePackUtils.buildMain(zip);
    }
}
