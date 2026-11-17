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
 *  uiedit-*.png. Phase 2: undo before the server replies, the window-size preview (4K, 720p), value chips, a size check and the
 *  Warnings tab. Only loaded by the isolated dev harness (GRAVES_UI_SHOTS=true and GRAVES_UI_EDITOR_CHECKS=true). Never shipped. */
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
                    // the War Table home's cards and Change, on parchment and vellum (Kort, 2026-11-16): each located for a click
                    for (String want : new String[] {"home.sheet.practiceName", "home.sheet.practiceSubtitle", "home.sheet.skirmishName", "home.sheet.campaignSubtitle", "home.field.change"})
                        report("located-" + want, UiEditorClient.LAST.stream().anyMatch(b -> b.id().equals(want)), String.valueOf(UiText.seen(want) == null ? "unseen" : UiText.seen(want).shown));
                    report("strip-clear-of-tab-bar", UiEditorClient.barTop(c.currentScreen) >= c.currentScreen.height / 2, "strip top " + UiEditorClient.barTop(c.currentScreen) + " of " + c.currentScreen.height);
                    // the first located editable line on the page
                    for (var b : UiEditorClient.LAST) { var s = UiText.seen(b.id()); if (s != null && !s.locked && s.slots.isEmpty()) { id = b.id(); before = s.shown; break; } }
                    report("editable-line-found", id != null, String.valueOf(id));
                    if (id != null) c.setScreen(new UiEditScreen(c.currentScreen, UiText.seen(id)));
                }
                if (t == 160) shot(c, "edit-box");
                if (t == 170 && id != null) { c.setScreen(new CampScreen("home")); UiEditorClient.set(id, EDIT, true); }
                if (t == 195 && id != null) c.setScreen(new CampScreen("home")); // rebuilt after the draft reply, so init-time labels show it
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
                    UiEditorClient.set(id, "STALE DRAFT", false); // a draft started from the published version, which the reset then replaces
                    c.player.networkHandler.sendChatCommand("gg ui reset");
                }
                if (t == 390 && id != null) {
                    report("reset-to-built-in", UiText.server().isEmpty() && UiText.t(id, before).equals(before), UiText.t(id, before));
                    shot(c, "reset");
                    report("history-kept", Files.readString(FabricLoader.getInstance().getConfigDir().resolve("gravesandglory/ui-edits.json")).contains("EDITED IN GAME"), "ui-edits.json");
                    UiEditorClient.op("publish");
                }
                if (t == 420 && id != null) {
                    report("stale-draft-refused", UiEditorClient.message.startsWith("Not published: your draft was started from version") && UiText.server().isEmpty(), UiEditorClient.message);
                    UiEditorClient.op("discard");
                }
                if (t == 440 && id != null) {
                    UiEditorClient.op("publish");
                }
                if (t == 460 && id != null) report("no-change-publish-refused", UiEditorClient.message.startsWith("Not published: nothing to publish"), UiEditorClient.message);
                // ---- phase 2: undo against replies in flight, the size preview, value chips, the size check, the new hub screens ----
                if (t == 470 && id != null) {
                    UiEditorClient.set(id, "UNDO A", true); UiEditorClient.set(id, "UNDO B", true); UiEditorClient.undo(); // all before any reply
                }
                if (t == 510 && id != null) {
                    report("undo-before-reply", "UNDO A".equals(UiText.draft().get(id)), String.valueOf(UiText.draft().get(id)));
                    UiEditorClient.op("discard");
                }
                if (t == 530) { if (!UiEditorClient.active) c.player.networkHandler.sendChatCommand("gg ui edit"); }
                if (t == 560) { c.setScreen(new CampScreen("home")); UiPreview.res = 3; UiPreview.gui = 0; UiPreview.apply(); }
                if (t == 590) {
                    var size = UiPreview.size();
                    report("preview-4k", UiPreview.previewingShown() && size != null && c.currentScreen.width == size.w() && c.currentScreen.height == size.h(), UiPreview.describe(size) + " screen " + c.currentScreen.width + "x" + c.currentScreen.height);
                    report("preview-keeps-real-window", c.getWindow().getScaledWidth() != size.w() || c.getWindow().getFramebufferWidth() != size.fbW(), "window " + c.getWindow().getFramebufferWidth() + "x" + c.getWindow().getFramebufferHeight());
                    shot(c, "preview-4k");
                    UiPreview.res = 0; UiPreview.gui = 0; UiPreview.apply();
                }
                if (t == 620) {
                    var size = UiPreview.size();
                    report("preview-720p", size != null && c.currentScreen.width == size.w() && size.scale() == 3, UiPreview.describe(size));
                    shot(c, "preview-720p");
                    UiPreview.reset();
                }
                if (t == 640) {
                    report("preview-reset", !UiPreview.on() && c.currentScreen.width == c.getWindow().getScaledWidth(), c.currentScreen.width + " vs " + c.getWindow().getScaledWidth());
                    UiText.Seen wave = new UiText.Seen("hud.wave");
                    wave.def = "Wave {wave} of {total}"; wave.shown = "Wave 3 of 5";
                    wave.slots = new java.util.LinkedHashSet<>(java.util.List.of("wave", "total"));
                    wave.values = java.util.Map.of("wave", "3", "total", "5");
                    UiEditScreen box = new UiEditScreen(c.currentScreen, wave);
                    c.setScreen(box);
                    report("value-chips", box.chips().size() == 2 && box.chips().get(0).label().contains("3"), String.valueOf(box.chips()));
                }
                if (t == 660) { shot(c, "chips"); c.setScreen(new CampScreen("home")); }
                if (t == 680) UiWarnings.start(c.currentScreen, "War Table: home", null);
                if (t > 680 && t < 1400 && UiWarnings.scanning()) return;
                if (t >= 681 && t < 1400) {
                    report("size-check-ran", !UiWarnings.scanning() && !UiPreview.on(), UiWarnings.count() + " warning(s)");
                    c.setScreen(new UiEditorScreen(c.currentScreen, "warnings"));
                    t = 1400;
                }
                if (t == 1420) { shot(c, "warnings"); c.setScreen(UiSamples.battleMap(c.currentScreen)); }
                if (t == 1450) {
                    shot(c, "sample-map");
                    report("sample-map-lines", UiEditorClient.editable(c.currentScreen) && !UiEditorClient.LAST.isEmpty(), UiEditorClient.LAST.size() + " boxes");
                    c.setScreen(UiSamples.battleReport());
                }
                if (t == 1480) {
                    shot(c, "sample-report");
                    report("sample-report-lines", UiEditorClient.editable(c.currentScreen) && !UiEditorClient.LAST.isEmpty(), UiEditorClient.LAST.size() + " boxes");
                    c.setScreen(UiSamples.battleHud(null));
                }
                if (t == 1510) {
                    shot(c, "sample-hud");
                    report("sample-hud-lines", UiEditorClient.editable(c.currentScreen) && !UiEditorClient.LAST.isEmpty(), UiEditorClient.LAST.size() + " boxes");
                    report("sample-state-restored", !CampaignClient.result.toString().contains("Old Mill"), "report state after the samples");
                    c.setScreen(null);
                }
                if (t == 1530) { Files.deleteIfExists(admin()); c.setScreen(null); c.scheduleStop(); }
            } catch (Exception e) { report("suite", false, e.toString()); e.printStackTrace(); c.scheduleStop(); }
        });
    }
}
