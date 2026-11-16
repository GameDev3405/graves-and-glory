package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.file.Files;
import java.nio.file.Path;

/** UI editor phase 1 (words), in a real client with its built-in server, after UiScreenShots: the owner gate (admin.json owners),
 *  /gg ui edit opening the editor, lines located on a War Table page, a draft edit drawn for the owner only while previewing, a bad
 *  edit refused by the server, Publish reaching the client's published layer, a non-owner request ignored, and /gg ui reset. Shots:
 *  uiedit-*.png. Only loaded by the isolated dev harness (GRAVES_UI_SHOTS=true and GRAVES_UI_EDITOR_CHECKS=true). Never shipped. */
public final class UiEditorChecks implements ClientModInitializer {
    int t;
    String id, before;
    static final String EDIT = "EDITED IN GAME {{ok}}";

    static void report(String name, boolean pass, String why) { MeltyTest.report("uiedit-" + name, pass, why); }
    static void shot(MinecraftClient c, String name) { net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(c.runDirectory, "uiedit-" + name + ".png", c.getFramebuffer(), x -> {}); }
    static Path admin() { return FabricLoader.getInstance().getConfigDir().resolve("gravesandglory").resolve("admin.json"); }
    static void owners(MinecraftClient c, boolean owner) throws Exception {
        Files.createDirectories(admin().getParent());
        Files.writeString(admin(), "{\"owners\":[" + (owner ? "\"" + c.player.getUuid() + "\"" : "") + "]}");
        Files.setLastModifiedTime(admin(), java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + (owner ? 0 : 5000)));
    }

    @Override public void onInitializeClient() {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("GRAVES_UI_SHOTS", "false"))) return;
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("GRAVES_UI_EDITOR_CHECKS", "false"))) return;
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (!UiScreenShots.shotsDone || c.player == null) return;
            try {
                t++;
                if (t == 10) { owners(c, false); c.player.networkHandler.sendChatCommand("gg ui edit"); }
                if (t == 40) report("non-owner-no-editor", !UiEditorClient.active && !(c.currentScreen instanceof UiEditorScreen), "active " + UiEditorClient.active);
                if (t == 50) { owners(c, true); c.player.networkHandler.sendChatCommand("gg ui edit"); }
                if (t == 90) { report("owner-opens-editor", UiEditorClient.active && c.currentScreen instanceof UiEditorScreen, String.valueOf(c.currentScreen)); shot(c, "hub"); }
                if (t == 100) c.setScreen(new CampScreen("home"));
                if (t == 140) {
                    shot(c, "overlay");
                    report("lines-located", !UiEditorClient.LAST.isEmpty(), UiEditorClient.LAST.size() + " boxes");
                    // the first located editable line on the page
                    for (var b : UiEditorClient.LAST) { var s = UiText.seen(b.id()); if (s != null && !s.locked && s.slots.isEmpty()) { id = b.id(); before = s.shown; break; } }
                    report("editable-line-found", id != null, String.valueOf(id));
                    if (id != null) c.setScreen(new UiEditScreen(c.currentScreen, UiText.seen(id)));
                }
                if (t == 160) shot(c, "edit-box");
                if (t == 170 && id != null) { c.setScreen(new CampScreen("home")); UiEditorClient.set(id, EDIT, true); }
                if (t == 210 && id != null) {
                    report("draft-saved", EDIT.equals(UiText.draft().get(id)), UiEditorClient.message);
                    report("draft-drawn-for-owner", UiText.seen(id) != null && UiText.seen(id).shown.equals("EDITED IN GAME {ok}"), UiText.seen(id) == null ? "unseen" : UiText.seen(id).shown);
                    shot(c, "draft");
                    UiEditorClient.set(id, "{nope}", true);
                }
                if (t == 240 && id != null) {
                    report("bad-edit-refused", UiEditorClient.message.startsWith("Not saved") && EDIT.equals(UiText.draft().get(id)), UiEditorClient.message);
                    UiEditorClient.op("publish");
                }
                if (t == 270 && id != null) {
                    report("published-live", EDIT.equals(UiText.server().get(id)), UiEditorClient.message);
                    UiEditorClient.on(false);
                    c.setScreen(new CampScreen("home"));
                }
                if (t == 310 && id != null) {
                    shot(c, "published");
                    report("published-drawn-with-editor-off", UiText.t(id, before).equals("EDITED IN GAME {ok}"), UiText.t(id, before));
                    owners(c, false);
                    UiEditorClient.set(id, "SHOULD NOT SAVE", false);
                }
                if (t == 350 && id != null) {
                    report("non-owner-request-ignored", !"SHOULD NOT SAVE".equals(UiText.draft().get(id)) && EDIT.equals(UiText.server().get(id)), String.valueOf(UiText.draft().get(id)));
                    owners(c, true);
                    c.player.networkHandler.sendChatCommand("gg ui reset");
                }
                if (t == 390 && id != null) {
                    report("reset-to-built-in", UiText.server().isEmpty() && UiText.t(id, before).equals(before), UiText.t(id, before));
                    shot(c, "reset");
                    report("history-kept", Files.readString(FabricLoader.getInstance().getConfigDir().resolve("gravesandglory/ui-edits.json")).contains("EDITED IN GAME"), "ui-edits.json");
                }
                if (t == 400) { Files.deleteIfExists(admin()); c.setScreen(null); c.scheduleStop(); }
            } catch (Exception e) { report("suite", false, e.toString()); e.printStackTrace(); c.scheduleStop(); }
        });
    }
}
