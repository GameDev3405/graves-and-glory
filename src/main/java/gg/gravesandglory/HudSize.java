package gg.gravesandglory;

import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;

/** The battle HUD's size (UI Task 8c): Compact, the default, Standard or Large (UiMath.HUD_COMPACT, HUD_STANDARD and HUD_LARGE;
 *  UiMath.hudScale turns one into the HUD's scale on a window). The choice is kept on the player's PC in gravesandglory/hud.json,
 *  beside presentation.json, and read the first time the HUD asks for it, so it lasts from one launch to the next. The Visuals and
 *  sound screen's Battle HUD button cycles it. Client thread only. */
public final class HudSize {
    private HudSize() {}

    /** The sizes in order, as the Battle HUD button names them and hud.json keeps them. */
    static final String[] NAMES = {"Compact", "Standard", "Large"};
    private static int size = UiMath.HUD_COMPACT;
    private static boolean loaded;

    static Path file() { return MinecraftClient.getInstance().runDirectory.toPath().resolve("gravesandglory/hud.json"); }

    /** The size the HUD draws at: the saved choice, Compact when none is saved or the file cannot be read. */
    public static int get() {
        if (!loaded) reload();
        return size;
    }

    /** Reads the saved choice again, as a launch does: Compact for a missing file, an unreadable one or a name it does not know. */
    static void reload() {
        loaded = true;
        size = UiMath.HUD_COMPACT;
        try {
            String name = JsonParser.parseString(Files.readString(file())).getAsJsonObject().get("size").getAsString();
            for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) size = i;
        } catch (Exception ignored) {}
    }

    /** Sets the size (Compact for one it does not know) and saves it. The screen's notice says whether it was saved; a size that
     *  could not be saved still applies until the game closes. */
    static void set(int s) {
        size = s >= 0 && s < NAMES.length ? s : UiMath.HUD_COMPACT;
        loaded = true;
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), "{\"size\":\"" + NAMES[size] + "\"}");
            Presentation.notice = "Battle HUD: " + NAMES[size] + ". Your choice is saved.";
        } catch (Exception e) { Presentation.notice = "Your HUD size could not be saved."; }
    }

    /** The next size, Compact after Large: what the Battle HUD button does. */
    static void cycle() { set((get() + 1) % NAMES.length); }
}
