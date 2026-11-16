package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.ScreenshotRecorder;

import java.util.List;
import java.util.function.Supplier;

/** UI editor phase 1, the no-edit identity check: enters a private practice camp and screenshots the mod's screens one by one
 *  (uishot-NAME.png in the run's screenshots folder), so the same class run on main and on the branch gives pictures to compare.
 *  Uses only classes main already has. Then hands over to UiEditorChecks when that suite is on (the branch only). Only loaded by
 *  the isolated dev harness (GRAVES_UI_SHOTS=true). Never shipped. */
public final class UiScreenShots implements ClientModInitializer {
    record Shot(String name, Supplier<Screen> make) {}
    static final List<Shot> SHOTS = List.of(
        new Shot("menu-home", () -> new FrontMenuScreen()),
        new Shot("menu-friends", () -> new FrontMenuScreen("friends")),
        new Shot("menu-credits", () -> new FrontMenuScreen("credits")),
        new Shot("table-home", () -> new CampScreen("home")),
        new Shot("table-recruit", () -> new CampScreen("recruit")),
        new Shot("table-banner", () -> new CampScreen("banner")),
        new Shot("table-contracts", () -> new CampScreen("contracts")),
        new Shot("table-chapter", () -> new CampScreen("chapter")),
        new Shot("table-friends", () -> new CampScreen("friends")),
        new Shot("table-field", () -> new CampScreen("field")),
        new Shot("table-lobby", () -> new CampScreen("lobby")),
        new Shot("table-gear", () -> new CampScreen("gear")),
        new Shot("table-train", () -> new CampScreen("train")),
        new Shot("barracks", () -> new RecruitmentScreen(0)),
        new Shot("armoury", () -> new RecruitmentScreen(1)),
        new Shot("presentation", () -> new PresentationScreen(null)));
    /** Set when the editor checks should run after the shots (UiEditorChecks reads it). */
    static volatile boolean shotsDone;
    int ticks, at = -1, wait;
    boolean entered;

    static void shot(MinecraftClient c, String name) { ScreenshotRecorder.saveScreenshot(c.runDirectory, "uishot-" + name + ".png", c.getFramebuffer(), t -> {}); }

    @Override public void onInitializeClient() {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("GRAVES_UI_SHOTS", "false"))) return;
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (c.getOverlay() != null || shotsDone) return;
            try {
                if (!entered) {
                    if (!(c.currentScreen instanceof FrontMenuScreen)) return;
                    if (++ticks == 60) { org.lwjgl.glfw.GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080); c.options.getGuiScale().setValue(0); c.onResolutionChanged(); }
                    if (ticks == 80) { GravesAndGloryClient.startCamp(c, false); entered = true; }
                    return;
                }
                if (c.player == null || !GravesAndGloryClient.ready) return;
                if (at < 0) { if (++wait < 100) return; at = 0; wait = 0; c.setScreen(SHOTS.get(0).make().get()); return; }
                if (++wait == 40) shot(c, SHOTS.get(at).name());
                if (wait == 45) {
                    MeltyTest.report("uishot-" + SHOTS.get(at).name(), c.currentScreen != null, String.valueOf(c.currentScreen));
                    wait = 0;
                    if (++at >= SHOTS.size()) {
                        c.setScreen(null); shotsDone = true;
                        if (!Boolean.parseBoolean(System.getenv().getOrDefault("GRAVES_UI_EDITOR_CHECKS", "false"))) c.scheduleStop();
                    } else c.setScreen(SHOTS.get(at).make().get());
                }
            } catch (Exception e) { MeltyTest.report("uishot-suite", false, e.toString()); e.printStackTrace(); c.scheduleStop(); }
        });
    }
}
