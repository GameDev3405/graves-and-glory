package gg.gravesandglory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;

/** Pack order for the 1.0 look (Kort, 2026-11-05): Excalibur styles the environment, our armory and weapons packs sit above it so weapons never
 *  take Excalibur's art. Excalibur is switched on once, on a first launch where its zip is installed; the order is repaired on every launch. */
final class LookPacks {
    static final String EXCALIBUR = "file/Excalibur_V1.20.zip", ARMORY = "gravesandglory:armory", WEAPONS = "gravesandglory:weapons";
    private LookPacks() {}

    /** Enabled packs, lowest priority first (the options.txt order). Ours go on top of Excalibur whenever it is enabled. */
    static List<String> arrange(List<String> enabled, Collection<String> available, boolean firstLaunch) {
        List<String> out = new ArrayList<>(enabled);
        boolean excalibur = out.contains(EXCALIBUR) || (firstLaunch && available.contains(EXCALIBUR));
        if (!excalibur) return out;
        out.removeIf(n -> n.equals(EXCALIBUR) || n.equals(ARMORY) || n.equals(WEAPONS));
        out.add(EXCALIBUR);
        if (available.contains(ARMORY)) out.add(ARMORY);
        if (available.contains(WEAPONS)) out.add(WEAPONS);
        return out;
    }

    static void apply(MinecraftClient c) {
        Path marker = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("gravesandglory-look.txt");
        boolean first = !Files.exists(marker);
        var manager = c.getResourcePackManager();
        List<String> now = new ArrayList<>(manager.getEnabledNames());
        List<String> next = arrange(now, manager.getNames(), first);
        if (!next.equals(now)) {
            manager.setEnabledProfiles(next);
            c.options.resourcePacks.clear();
            c.options.resourcePacks.addAll(manager.getEnabledNames());
            c.options.write();
            c.reloadResources();
        }
        if (first) try { Files.writeString(marker, "Excalibur is enabled once on a first launch; delete this file to repeat.\n"); } catch (IOException ignored) {}
    }
}
