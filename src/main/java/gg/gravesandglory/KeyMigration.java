package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** The command keys' one-time move to Z X C V B (commit 71a8b539, 2026-11-04). Before it, Attack was G and Follow me V; a player who
 *  launched then keeps those in options.txt, so Follow me V met the new Retreat V. On the first launch with this class, Attack still on
 *  G goes to its default Z and Follow me still on V to its default C, and options.txt is written. The marker file then says it ran, so a
 *  later deliberate G or V is never touched. Client only; no other binding is changed. */
public final class KeyMigration {
    private KeyMigration() {}

    /** The bindings moved: translation key and the old default it had before the move. */
    static final String[][] OLD = {{"key.gravesandglory.attack", "key.keyboard.g"}, {"key.gravesandglory.follow", "key.keyboard.v"}};

    /** What the last run rebound, for the checks: "attack" and/or "follow". */
    public static final List<String> moved = new ArrayList<>();
    public static int runs;

    public static Path marker() { return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("gravesandglory").resolve("keys-zxcvb.done"); }

    /** Once per install: rebinds the old defaults and writes the marker. Returns whether a binding was changed. */
    public static boolean run(MinecraftClient client) { return run(client, marker()); }

    public static boolean run(MinecraftClient client, Path flag) {
        if (client.options == null || Files.exists(flag)) return false;
        // the marker first: if it cannot be written, nothing moves, so a later launch can never move a key the player has since chosen
        try { Files.createDirectories(flag.getParent()); Files.writeString(flag, "commandKeys=zxcvb\n"); }
        catch (Exception e) { System.err.println("[Graves and Glory] Could not record the command key move, so the keys stay as they are: " + e.getMessage()); return false; }
        runs++;
        moved.clear();
        for (String[] o : OLD) {
            KeyBinding k = find(client, o[0]);
            if (k == null || !k.getBoundKeyTranslationKey().equals(o[1])) continue;
            client.options.setKeyCode(k, k.getDefaultKey());
            moved.add(o[0].substring(o[0].lastIndexOf('.') + 1));
        }
        if (!moved.isEmpty()) { KeyBinding.updateKeysByCode(); client.options.write(); }
        try { Files.writeString(flag, "commandKeys=zxcvb\nmoved=" + String.join(",", moved) + "\n"); } catch (Exception ignored) { } // what moved, for the record; the marker is already there
        return !moved.isEmpty();
    }

    static KeyBinding find(MinecraftClient c, String translationKey) {
        for (KeyBinding k : c.options.allKeys) if (k.getTranslationKey().equals(translationKey)) return k;
        return null;
    }
}
