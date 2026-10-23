package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.nio.file.Files;
import java.nio.file.Path;

/** The battle HUD's size (UI Task 8c): Compact, the default, Standard or Large (UiMath.HUD_COMPACT, HUD_STANDARD and HUD_LARGE;
 *  UiMath.hudScale turns one into the HUD's scale on a window). The choice is kept on the player's PC in gravesandglory/hud.json,
 *  beside presentation.json, and read the first time the HUD asks for it, so it lasts from one launch to the next. The Visuals and
 *  sound screen's Battle HUD button cycles it, and it and the notice say where the window draws the choice at a smaller size's
 *  scale (UI Task 8c fix round 1: UiMath.hudShownAs). Client thread only. */
public final class HudSize {
    private HudSize() {}

    /** The sizes in order, as the Battle HUD button names them and hud.json keeps them (UiMath.HUD_SIZE_NAMES). */
    static final String[] NAMES = UiMath.HUD_SIZE_NAMES;
    private static int size = UiMath.HUD_COMPACT;
    private static boolean loaded;

    static Path file() { return MinecraftClient.getInstance().runDirectory.toPath().resolve("gravesandglory/hud.json"); }

    /** The size the HUD draws at: the saved choice, Compact when none is saved or the file cannot be read. */
    public static int get() {
        if (!loaded) reload();
        return size;
    }

    /** Reads the saved choice again, as a launch does: Compact for a missing file, an unreadable one, text that is not the one
     *  {"size":...} object or a name it does not know (UiMath.hudSizeOf, which the standalone check covers). */
    static void reload() {
        loaded = true;
        String text = null;
        try { text = Files.readString(file()); } catch (Exception ignored) {}
        size = UiMath.hudSizeOf(text);
    }

    /** The size this window shows the choice at (UiMath.hudShownAs): the choice itself, or a smaller size where the window has no
     *  room to draw it a step larger. */
    static int shownAs() {
        var w = MinecraftClient.getInstance().getWindow();
        return UiMath.hudShownAs(w.getFramebufferWidth(), w.getFramebufferHeight(), (int) w.getScaleFactor(), get());
    }

    /** The Battle HUD button's words, true to this window: "Battle HUD: Large (Standard size here)" (UiMath.hudSizeLabel). */
    static String label() { return UiMath.hudSizeLabel(get(), shownAs()); }

    /** Sets the size (Compact for one it does not know) and saves it. The screen's notice says whether it was saved, and the size
     *  this window draws it at where that is smaller (UiMath.hudSizeNotice); a size that could not be saved still applies until the
     *  game closes. */
    static void set(int s) {
        size = s >= 0 && s < NAMES.length ? s : UiMath.HUD_COMPACT;
        loaded = true;
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), "{\"size\":\"" + NAMES[size] + "\"}");
            Presentation.notice = UiMath.hudSizeNotice(size, shownAs());
        } catch (Exception e) { Presentation.notice = "Your HUD size could not be saved."; }
    }

    /** The next size, Compact after Large: what the Battle HUD button does. */
    static void cycle() { set((get() + 1) % NAMES.length); }
}
