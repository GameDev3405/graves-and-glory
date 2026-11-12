package gg.gravesandglory;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.world.Heightmap;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The barracks page by mouse (Kort, 2026-11-11: the mid-match training screen rebuilt as a War Table page). The commander stands at his own
 * barracks with gold to spend; the lectern's screen opens on its barracks page, and every press goes through Mouse.onMouseButton at the centre of
 * the widget, as a player's click does: a troop row picks the troop, Disband removes one, Recruit queues one, Cancel training empties the queue.
 * The page's boxes are checked to lie inside the window and apart at 1920x1080, 854x480 and 2560x1440, with shots of the overview, a troop
 * picked and a training queue at each size in <run>/command-ui/shots. The suite is cmdui.BarracksSuite (GRAVES_COMMAND_UI_CHECKS=Barracks).
 */
public class BarracksUiChecks implements CommandUiChecks.Suite {
    static final int[][] SIZES = {{1920, 1080}, {854, 480}, {2560, 1440}};
    static final int BASE = 70, STEP = 130;
    UUID id;
    Path shots;
    int[] initialSize;
    final List<String> shotLog = new ArrayList<>(), fitFails = new ArrayList<>();
    final Map<String, String> results = new LinkedHashMap<>();
    boolean openOk, pickOk, disbandOk; int recruitOk, cancelOk, fitsChecked;
    int pickWant = -1, rosterBefore, queuedBefore, goldBefore;
    String openInfo = "";

    @Override public int limit() { return BASE + STEP * 3 + 120; }

    static RecruitmentScreen screen(MinecraftClient c) { return c.currentScreen instanceof RecruitmentScreen r && r.tab == 0 ? r : null; }

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) {
            id = c.player.getUuid();
            shots = FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("shots");
            try { Files.createDirectories(shots); } catch (Exception ignored) { }
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-barracks-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            initialSize = new int[]{c.getWindow().getWidth(), c.getWindow().getHeight()};
            CommandUiChecks.server(c, b -> toBarracks(c, b));
        }
        if (tick > 40 && tick < BASE && tick % 10 == 0) CommandUiChecks.server(c, b -> toBarracks(c, b));
        for (int k = 0; k < 3; k++) {
            int t = tick - (BASE + STEP * k);
            if (t >= 0 && t < STEP) sizeStep(c, t, k);
        }
        int end = BASE + STEP * 3;
        if (tick == end) {
            CommandUiChecks.report("cu-barracks-open", openOk, openInfo);
            CommandUiChecks.report("cu-barracks-row-click", pickOk, results.getOrDefault("pick", "not reached"));
            CommandUiChecks.report("cu-barracks-disband-click", disbandOk, results.getOrDefault("disband", "not reached"));
            CommandUiChecks.report("cu-barracks-recruit-click", recruitOk == 3, recruitOk + " of 3 sizes queued by clicking Recruit: " + results.getOrDefault("recruit", "not reached"));
            CommandUiChecks.report("cu-barracks-cancel-click", cancelOk == 3, cancelOk + " of 3 sizes emptied the queue by clicking Cancel training: " + results.getOrDefault("cancel", "not reached"));
            CommandUiChecks.report("cu-barracks-fits", fitFails.isEmpty() && fitsChecked == 3, fitsChecked + " sizes checked: " + (fitFails.isEmpty() ? "every row, plate and rail plaque inside the window, the plates apart and off the letter and the roll" : String.join("; ", fitFails)) + "; shots: " + String.join(", ", shotLog));
            c.setScreen(null);
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), initialSize[0], initialSize[1]);
        }
        if (tick == end + 10) CommandUiChecks.finish(c);
    }

    /** Puts the commander beside his own barracks with 5000 gold, so every plate can be pressed. */
    void toBarracks(MinecraftClient c, GravesAndGlory.Battle b) {
        var s = b.squads.get(id); var p = c.getServer().getPlayerManager().getPlayer(id);
        if (s == null || p == null) return;
        s.gold = 5000;
        var at = b.fieldBarracks(s.team);
        if (GravesAndGloryClient.nearBarracks) return;
        double x = at.x + 2.5, z = at.z + .5;
        int y = p.getServerWorld().getTopY(Heightmap.Type.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        p.teleport(p.getServerWorld(), x, y, z, p.getYaw(), p.getPitch());
    }

    void sizeStep(MinecraftClient c, int t, int k) {
        int w = SIZES[k][0], h = SIZES[k][1]; String size = String.valueOf(w);
        if (t == 0) { c.setScreen(null); GLFW.glfwSetWindowSize(c.getWindow().getHandle(), w, h); }
        if (t == 12) c.setScreen(new GravesAndGloryClient.BarracksScreen()); // as the lectern opens it (OPEN_BARRACKS)
        if (t == 24) {
            var s = screen(c);
            park(c);
            if (k == 0) {
                openOk = s != null && UiCanvas.fillsWindow() && !s.barracks.rows.isEmpty() && s.recruit instanceof UiButton && s.disband instanceof UiButton && s.cancel instanceof UiButton && GravesAndGloryClient.nearBarracks
                    && s.controls.size() == s.barracks.rows.size() + Math.max(0, s.barracks.listed.size() - s.barracks.rows.size());
                openInfo = s == null ? "no barracks page: " + c.currentScreen : "page 0 open at the barracks=" + GravesAndGloryClient.nearBarracks + ", War Table scale=" + UiCanvas.fillsWindow() + ", rows " + s.barracks.rows.size() + " of " + s.barracks.listed.size() + " troop types, controls " + s.controls.size() + ", plates " + (s.recruit instanceof UiButton) + "/" + (s.disband instanceof UiButton) + "/" + (s.cancel instanceof UiButton) + ", gold " + GravesAndGloryClient.gold;
            }
            fits(c, s, size);
        }
        if (t == 28) shot(c, "barracks-overview-" + size + ".png");
        // a troop row clicked
        if (t == 32) {
            var s = screen(c); if (s == null) return;
            int row = s.barracks.rows.size() > 1 ? 1 : 0;
            pickWant = s.barracks.listed.get(s.barracks.first + row);
            if (s.selected == pickWant) pickWant = s.barracks.listed.get(s.barracks.first);
            click(c, s.barracks.rows.get(s.barracks.listed.indexOf(pickWant) - s.barracks.first));
        }
        if (t == 35) {
            var s = screen(c); boolean ok = s != null && s.selected == pickWant;
            if (k == 0) { pickOk = ok; results.put("pick", "clicked the row of " + (pickWant < 0 ? "?" : Spec.TROOPS[pickWant].name()) + ": picked " + (s == null || s.selected < 0 ? "none" : Spec.TROOPS[s.selected].name())); }
            if (s != null) hover(c, s.disband);
        }
        if (t == 38) { var s = screen(c); if (s != null) { var r = s.barracks.rows.get(0); park(c); hover(c, r.getX() + r.getWidth() / 2.0, r.getY() + r.getHeight() / 2.0); } }
        if (t == 40) shot(c, "barracks-selected-" + size + ".png");
        // Disband twice (the first size only), which frees the banner's slots so Recruit can queue
        if (k == 0 && t == 44) { var s = screen(c); if (s != null) { rosterBefore = GravesAndGloryClient.roster[s.selected]; click(c, s.disband); } }
        if (k == 0 && t == 54) { var s = screen(c); if (s != null) click(c, s.disband); }
        if (k == 0 && t == 66) {
            var s = screen(c); int after = s == null ? -1 : GravesAndGloryClient.roster[s.selected];
            disbandOk = s != null && after >= 0 && after < rosterBefore;
            results.put("disband", "Disband one clicked twice: " + (s == null ? "?" : Spec.TROOPS[s.selected].name()) + " fielded " + rosterBefore + " -> " + after);
        }
        // Recruit twice, the queue shown, then Cancel training
        if (t == 70) { var s = screen(c); if (s != null) { queuedBefore = GravesAndGloryClient.queued; click(c, s.recruit); } }
        if (t == 80) { var s = screen(c); if (s != null && s.recruit.active) click(c, s.recruit); }
        if (t == 92) {
            var s = screen(c); boolean ok = GravesAndGloryClient.queued > queuedBefore;
            if (ok) recruitOk++;
            results.merge("recruit", size + ": queued " + queuedBefore + " -> " + GravesAndGloryClient.queued + " (" + (s == null ? "?" : s.barracks.reasonDrawn) + ")", (a, b) -> a + "; " + b);
            if (s != null) { park(c); hover(c, s.recruit); }
        }
        if (t == 95) shot(c, "barracks-queue-" + size + ".png");
        if (t == 100) { var s = screen(c); if (s != null) { goldBefore = GravesAndGloryClient.gold; click(c, s.cancel); } }
        if (t == 114) {
            boolean ok = GravesAndGloryClient.queued == 0 && GravesAndGloryClient.gold > goldBefore;
            if (ok) cancelOk++;
            results.merge("cancel", size + ": queued " + GravesAndGloryClient.queued + ", gold " + goldBefore + " -> " + GravesAndGloryClient.gold, (a, b) -> a + "; " + b);
        }
        if (t == 120) c.setScreen(null);
    }

    /** Every widget inside the window; the three plates apart and off the letter; every row inside the roll. */
    void fits(MinecraftClient c, RecruitmentScreen s, String size) {
        fitsChecked++;
        if (s == null) { fitFails.add(size + ": no page"); return; }
        int cw = UiCanvas.width(), ch = UiCanvas.height();
        List<int[]> plates = new ArrayList<>();
        for (var e : s.children()) if (e instanceof UiButton u) {
            int[] b = u.canvasBox();
            if (b[0] < 0 || b[1] < 0 || b[0] + b[2] > cw || b[1] + b[3] > ch) fitFails.add(size + ": " + u.getMessage().getString() + " " + Arrays.toString(b) + " leaves the " + cw + "x" + ch + " canvas");
        }
        for (var b : new UiButton[]{(UiButton) s.recruit, (UiButton) s.disband, (UiButton) s.cancel}) plates.add(b.canvasBox());
        for (int i = 0; i < plates.size(); i++) for (int j = i + 1; j < plates.size(); j++) if (meet(plates.get(i), plates.get(j))) fitFails.add(size + ": plates " + i + " and " + j + " meet");
        int[] lo = WarTableRecruit.letterAt(), ro = WarTableRecruit.rollAt();
        int[] letter = {lo[0] + WarTableRecruit.LETTER_X, lo[1] + WarTableRecruit.LETTER_Y, UiTex.entry("recruit/letter-hi").w() - 6, UiTex.entry("recruit/letter-hi").h()};
        int[] roll = {ro[0] + WarTableRecruit.ROLL_X, ro[1] + WarTableRecruit.ROLL_Y, UiTex.entry("recruit/muster-roll").w(), UiTex.entry("recruit/muster-roll").h()};
        for (int[] p : plates) if (meet(p, letter)) fitFails.add(size + ": plate " + Arrays.toString(p) + " meets the letter " + Arrays.toString(letter));
        for (var r : s.barracks.rows) { int[] b = r.canvasBox(); if (b[0] < roll[0] - 16 || b[1] < roll[1] || b[0] + b[2] > roll[0] + roll[2] || b[1] + b[3] > roll[1] + roll[3]) fitFails.add(size + ": row " + Arrays.toString(b) + " outside the roll " + Arrays.toString(roll)); }
        if (s.barracks.queueBox != null && s.barracks.purseBox != null) {
            for (int[] p : plates) if (meet(p, s.barracks.queueBox) || meet(p, s.barracks.purseBox)) fitFails.add(size + ": plate " + Arrays.toString(p) + " meets the queue note or the purse");
            if (s.barracks.queueBox[1] + s.barracks.queueBox[3] > s.barracks.purseBox[1]) fitFails.add(size + ": queue note meets the purse");
        }
    }
    static boolean meet(int[] a, int[] b) { return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3]; }

    // --- the mouse, through the GLFW callbacks a real mouse drives ---
    static void hover(MinecraftClient c, net.minecraft.client.gui.widget.ButtonWidget b) { if (b != null) hover(c, b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0); }
    static void hover(MinecraftClient c, double gx, double gy) {
        var w = c.getWindow();
        invoke(c, "onCursorPos", new Class<?>[]{long.class, double.class, double.class}, w.getHandle(), gx * w.getWidth() / w.getScaledWidth(), gy * w.getHeight() / w.getScaledHeight());
    }
    static void click(MinecraftClient c, net.minecraft.client.gui.widget.ButtonWidget b) {
        if (b == null) return;
        int[] box = b instanceof UiButton u ? UiCanvas.guiBox(u.canvasBox()[0], u.canvasBox()[1], u.canvasBox()[2], u.canvasBox()[3]) : new int[]{b.getX(), b.getY(), b.getWidth(), b.getHeight()};
        hover(c, box[0] + box[2] / 2.0, box[1] + box[3] / 2.0);
        long h = c.getWindow().getHandle();
        invoke(c, "onMouseButton", new Class<?>[]{long.class, int.class, int.class, int.class}, h, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0);
        invoke(c, "onMouseButton", new Class<?>[]{long.class, int.class, int.class, int.class}, h, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, 0);
    }
    /** The pointer at the window's bottom-left corner, off every widget. */
    static void park(MinecraftClient c) { hover(c, 2, c.getWindow().getScaledHeight() - 2); }
    static void invoke(MinecraftClient c, String name, Class<?>[] types, Object... args) {
        try { Method m = Mouse.class.getDeclaredMethod(name, types); m.setAccessible(true); m.invoke(c.mouse, args); }
        catch (Exception e) { throw new IllegalStateException("Mouse." + name + ": " + e, e); }
    }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) { if (c.currentScreen instanceof RecruitmentScreen) c.setScreen(null); }
}
