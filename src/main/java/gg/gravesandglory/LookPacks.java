package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;

/** Pack order for the look (Kort, 2026-11-15: Excalibur is dropped on 1.21.1). Our armory and weapons packs go on top of whatever else the
 *  player has enabled, weapons last; a pack the player switched off stays off. A player's own old Excalibur entry is left alone: it is never switched on and never removed. */
final class LookPacks {
    static final String ARMORY = "gravesandglory:armory", WEAPONS = "gravesandglory:weapons";
    private LookPacks() {}

    /** Enabled packs, lowest priority first (the options.txt order). Armory then weapons, when enabled, end up at the top. */
    static List<String> arrange(List<String> enabled, Collection<String> available) {
        List<String> out = new ArrayList<>(enabled);
        boolean armory = out.remove(ARMORY), weapons = out.remove(WEAPONS);
        if (armory && available.contains(ARMORY)) out.add(ARMORY);
        if (weapons && available.contains(WEAPONS)) out.add(WEAPONS);
        return out;
    }

    static void apply(MinecraftClient c) {
        var manager = c.getResourcePackManager();
        List<String> now = new ArrayList<>(manager.getEnabledIds());
        List<String> next = arrange(now, manager.getIds());
        if (!next.equals(now)) {
            manager.setEnabledProfiles(next);
            c.options.resourcePacks.clear();
            c.options.resourcePacks.addAll(manager.getEnabledIds());
            c.options.write();
            c.reloadResources();
        }
    }
}
