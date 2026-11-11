package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandPanel;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.PanelLayout;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The command panel's edit mode (Arrange command panel, H): enter and leave (H, Done, Esc without the pause menu), no orders meanwhile, a
 * drag moves the panel and the file changes, the panel stays on screen in its corner after 1920 -> 854, hide, opacity, reset, and the
 * arrangement read back from the file after a disconnect and rejoin. All input through the GLFW callbacks (PanelSuite's helpers) and
 * KeyBinding presses. Shots panel-edit, panel-moved, panel-hidden and panel-opacity at 1920x1080 and 854x480. Never included in releases.
 */
public final class EditSuite implements CommandUiChecks.Suite {
    UUID id;
    Path shots;
    final Waits waits = new Waits();
    final List<String> shotLog = new ArrayList<>();
    final List<String> movedFails = new ArrayList<>();
    final List<String> notes = new ArrayList<>();
    int movedChecked;
    // disconnect: watched by a tick listener of our own while the suite's ticks pause without a world
    static volatile EditSuite live;
    static boolean listening;
    int disconnectStage, offlineTicks, rejoinTick = -1;
    String expect = "";
    int loadsBefore, savesBefore, sentBefore;
    List<String> selBefore = List.of();
    boolean enterOk, hiddenRest1920, hiddenRest854, hiddenOpens;
    String enterDetail = "", dragDetail = "", resizeDetail = "", hiddenDetail = "", opacityDetail = "";
    boolean dragOk, resizeOk, escOk, opacityOk854, opacityOk1920, resetOk;
    int[] dragFrom;

    @Override public int limit() { return 2400; }

    static void press(MinecraftClient c, String key) { CommandUiChecks.press(c, PanelSuite.find(c, key)); }
    static int[] rect(int id) { return PanelSuite.rect(id); }
    static boolean drawn(int id) { return PanelSuite.drawn(id); }
    static String box(int[] b) { return b == null ? "none" : b.length == 5 ? Arrays.toString(Arrays.copyOfRange(b, 1, 5)) : Arrays.toString(b); }
    static int sw(MinecraftClient c) { return c.getWindow().getScaledWidth(); }
    static int sh(MinecraftClient c) { return c.getWindow().getScaledHeight(); }
    static String layout() { return PanelLayout.anchor + String.format(Locale.ROOT, " %.1f,%.1f", PanelLayout.offX, PanelLayout.offY) + " opacity " + PanelLayout.opacity + " hidden " + PanelLayout.hidden; }
    static String file() { try { return Files.readString(PanelLayout.file(), StandardCharsets.UTF_8).replaceAll("\\s+", " "); } catch (Exception e) { return "(no file: " + e.getClass().getSimpleName() + ")"; } }

    static void mouse(MinecraftClient c, int action) {
        PanelSuite.invoke(c, "onMouseButton", new Class<?>[]{long.class, int.class, int.class, int.class}, c.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT, action, 0);
    }
    static void clickId(MinecraftClient c, int id) { int[] r = rect(id); if (r != null) PanelSuite.clickGui(c, r[1] + r[3] / 2.0, r[2] + r[4] / 2.0, 0); }
    static void escape(MinecraftClient c) {
        try {
            Method m = Keyboard.class.getDeclaredMethod("onKey", long.class, int.class, int.class, int.class, int.class);
            m.setAccessible(true);
            m.invoke(c.keyboard, c.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE, 1, GLFW.GLFW_PRESS, 0);
            m.invoke(c.keyboard, c.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE, 1, GLFW.GLFW_RELEASE, 0);
        } catch (Exception e) { throw new IllegalStateException("Keyboard.onKey: " + e, e); }
    }

    /** A drag of the panel: press at its centre (start), the cursor walks to (tx, ty) over six ticks, release at start + 7. */
    void drag(MinecraftClient c, int tick, int start, double tx, double ty) {
        if (tick == start) {
            int[] b = CommandPanel.bounds;
            if (b == null) { notes.add("drag at " + start + ": no panel drawn"); return; }
            dragFrom = new int[]{b[0] + b[2] / 2, b[1] + b[3] / 2};
            PanelSuite.cursorGui(c, dragFrom[0], dragFrom[1]);
            mouse(c, GLFW.GLFW_PRESS);
        } else if (tick > start && tick <= start + 6 && dragFrom != null) {
            double f = (tick - start) / 6.0;
            PanelSuite.cursorGui(c, dragFrom[0] + (tx - dragFrom[0]) * f, dragFrom[1] + (ty - dragFrom[1]) * f);
        } else if (tick == start + 7) mouse(c, GLFW.GLFW_RELEASE);
    }

    /** The moved panel's rules: every rect it drew (tray included) on screen; the tray clear of the vanilla hotbar rows; with the panel in the right half the tray opens to its left. */
    void movedChecks(MinecraftClient c, String state, boolean trayLeft) {
        movedChecked++;
        int sw = sw(c), sh = sh(c);
        if (CommandPanel.lastDrawn.isEmpty() || CommandPanel.bounds == null) { movedFails.add(state + ": nothing drawn"); return; }
        int[] b = CommandPanel.bounds;
        if (b[0] < 0 || b[1] < 0 || b[0] + b[2] > sw || b[1] + b[3] > sh) movedFails.add(state + ": bounds " + box(b) + " leave the " + sw + "x" + sh + " screen");
        for (int[] r : CommandPanel.lastDrawn) if (r[1] < 0 || r[2] < 0 || r[1] + r[3] > sw || r[2] + r[4] > sh) movedFails.add(state + ": id " + r[0] + " " + box(r) + " off screen");
        int[] t = rect(CommandPanel.TRAY), v = CommandPanel.vanillaHud(sw, sh);
        if (t != null && t[1] < v[0] + v[2] && v[0] < t[1] + t[3] && t[2] < v[1] + v[3] && v[1] < t[2] + t[4]) movedFails.add(state + ": tray " + box(t) + " meets the vanilla hotbar rows " + Arrays.toString(v));
        if (trayLeft && (t == null || t[1] + t[3] > b[0])) movedFails.add(state + ": tray " + box(t) + " not to the left of the panel " + box(b));
        notes.add(state + " bounds " + box(b) + " tray " + box(t));
    }

    /** The edit toolbar: drawn on screen, clear of the panel, and attached to it (within 20 px above or below). */
    String toolbarCheck(MinecraftClient c, String state) {
        int[] t = rect(CommandPanel.TOOLBAR), b = CommandPanel.bounds, cap = rect(CommandPanel.CAPTION);
        if (t == null || b == null || cap == null || !drawn(CommandPanel.EDGE)) return state + ": toolbar " + box(t) + " caption " + box(cap) + " outline " + drawn(CommandPanel.EDGE) + " FAIL";
        boolean on = t[1] >= 0 && t[2] >= 0 && t[1] + t[3] <= sw(c) && t[2] + t[4] <= sh(c);
        boolean below = t[2] >= b[1] + b[3], above = t[2] + t[4] <= b[1];
        int gapPx = below ? t[2] - (b[1] + b[3]) : b[1] - (t[2] + t[4]);
        boolean ok = on && (below || above) && gapPx <= 20;
        return state + ": toolbar " + box(t) + (below ? " below" : above ? " above" : " OVER") + " the panel " + box(b) + " gap " + gapPx + ", caption " + box(cap) + (ok ? "" : " FAIL");
    }

    @Override public void tick(MinecraftClient c, int tick) {
        waits.pump(tick);
        if (tick == 1) {
            id = c.player.getUuid(); live = this;
            shots = FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("shots");
            try { Files.createDirectories(shots); Files.deleteIfExists(PanelLayout.file()); } catch (Exception ignored) { }
            PanelLayout.forget();
            listen();
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-edit-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> { OrderIntakeProbe.respawning(b, id, false); OrderIntakeProbe.resetCharge(b, id); });
            CommandSelection.reset();
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        }
        if (tick == 56) PanelSuite.cursorCentre(c);

        // H enters edit mode: the cursor freed, the toolbar drawn; order and number keys do nothing meanwhile
        if (tick == 60) press(c, "key.gravesandglory.panel_edit");
        if (tick == 63) {
            enterOk = CommandPanel.editing() && CommandPanel.capturing() && !c.mouse.isCursorLocked();
            enterDetail = "H: editing=" + CommandPanel.editing() + " capturing=" + CommandPanel.capturing() + " cursor free=" + !c.mouse.isCursorLocked();
            sentBefore = OrderIntakeProbe.ordersSent(); selBefore = CommandSelection.selected();
            press(c, "key.gravesandglory.attack"); press(c, "key.gravesandglory.retreat"); CommandUiChecks.press(c, c.options.hotbarKeys[1]);
        }
        if (tick == 66) {
            int sent = OrderIntakeProbe.ordersSent() - sentBefore;
            enterOk &= sent == 0 && CommandSelection.selected().equals(selBefore) && CommandPanel.editing() && !CommandSelection.panelOpen();
            enterDetail += "; Z, V and 2 pressed in edit mode: orders sent " + sent + ", selection " + selBefore + " -> " + CommandSelection.selected() + ", still editing=" + CommandPanel.editing();
        }
        if (tick == 67) waits.when(tick, 13, () -> drawn(CommandPanel.TOOLBAR), null, ok -> { enterDetail += "; " + toolbarCheck(c, "1920"); shot(c, "panel-edit-1920.png"); });

        // drag toward the bottom-right corner, past it: the panel follows, clamped on screen; H leaves and saves
        if (tick >= 82 && tick <= 89) {
            if (tick == 82) { savesBefore = PanelLayout.saves; dragDetail = "before " + box(CommandPanel.bounds) + " file " + file(); }
            drag(c, tick, 82, sw(c) + 60, sh(c) + 60);
        }
        if (tick == 91) {
            int[] b = CommandPanel.bounds;
            dragOk = b != null && b[0] + b[2] == sw(c) && b[1] + b[3] == sh(c) && PanelLayout.anchor.equals("bottom-right") && !CommandPanel.dragging();
            dragDetail += "; after the drag " + box(b) + " on " + sw(c) + "x" + sh(c) + ", layout " + layout();
            press(c, "key.gravesandglory.panel_edit");
        }
        if (tick == 95) {
            String f = file();
            dragOk &= !CommandPanel.editing() && PanelLayout.saves == savesBefore + 1 && f.contains("bottom-right") && f.contains(id.toString());
            dragDetail += "; H again: editing=" + CommandPanel.editing() + ", saves +" + (PanelLayout.saves - savesBefore) + ", file " + f;
            CommandUiChecks.report("cu-edit-drag", dragOk, dragDetail);
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 854, 480);
        }
        // 1920 -> 854 with the panel at the bottom-right: still on screen, still in that corner
        if (tick == 110) press(c, "key.gravesandglory.menu");
        if (tick == 112) waits.when(tick, 20, () -> CommandPanel.lastSlide >= 1 && CommandPanel.bounds != null, null, ok -> {
            int[] b = CommandPanel.bounds;
            resizeOk = ok && b[0] >= 0 && b[1] >= 0 && b[0] + b[2] == sw(c) && b[1] + b[3] == sh(c);
            resizeDetail = "854x480 (GUI " + sw(c) + "x" + sh(c) + "), panel opened with R: bounds " + box(b) + ", layout " + layout();
            movedChecks(c, "bottom-right-854", false);
            CommandUiChecks.report("cu-edit-resize", resizeOk, resizeDetail);
            CommandSelection.setPanelOpen(false);
        });

        // edit mode at 854 (the toolbar flips above a panel at the foot), then Esc leaves it without the pause menu
        if (tick == 140) press(c, "key.gravesandglory.panel_edit");
        if (tick == 143) waits.when(tick, 17, () -> drawn(CommandPanel.TOOLBAR), null, ok -> { enterDetail += "; " + toolbarCheck(c, "854"); shot(c, "panel-edit-854.png"); });
        if (tick == 162) { escOk = CommandPanel.editing(); savesBefore = PanelLayout.saves; escape(c); }
        if (tick == 165) {
            boolean ok = escOk && !CommandPanel.editing() && c.currentScreen == null && PanelLayout.saves == savesBefore + 1;
            CommandUiChecks.report("cu-edit-esc", ok, "editing before Esc=" + escOk + "; after: editing=" + CommandPanel.editing() + ", screen=" + (c.currentScreen == null ? "none" : c.currentScreen.getClass().getSimpleName()) + ", saved=" + (PanelLayout.saves - savesBefore));
            enterOk &= !enterDetail.contains("FAIL");
            CommandUiChecks.report("cu-edit-enter", enterOk, enterDetail);
        }

        // to the top-right corner, Done; R opens it there and B the tray, which opens to the panel's left
        if (tick == 168) press(c, "key.gravesandglory.panel_edit");
        if (tick >= 171 && tick <= 178) drag(c, tick, 171, sw(c) + 60, -60);
        if (tick == 180) clickId(c, CommandPanel.DONE);
        if (tick == 183) notes.add("Done clicked: editing=" + CommandPanel.editing() + ", layout " + layout());
        if (tick == 185) press(c, "key.gravesandglory.menu");
        if (tick == 188) press(c, "key.gravesandglory.formation");
        if (tick == 190) waits.when(tick, 20, () -> drawn(CommandPanel.TRAY) && CommandPanel.lastSlide >= 1, null, ok -> { movedChecks(c, "top-right-tray-854", true); shot(c, "panel-moved-854.png"); });
        if (tick == 212) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        if (tick == 228) waits.when(tick, 22, () -> drawn(CommandPanel.TRAY) && sw(c) > 427, null, ok -> {
            movedChecks(c, "top-right-tray-1920", true);
            shot(c, "panel-moved-1920.png");
            int[] b = CommandPanel.bounds;
            boolean corner = b != null && b[1] == 0 && b[0] + b[2] == sw(c) && PanelLayout.anchor.equals("top-right") && !CommandPanel.editing();
            CommandUiChecks.report("cu-edit-moved", corner && movedFails.isEmpty() && movedChecked >= 3,
                "top-right after Done=" + corner + "; " + movedChecked + " moved frames: " + (movedFails.isEmpty() ? "every rect on screen, tray clear of the hotbar rows and to the panel's left" : String.join("; ", movedFails)) + "; " + String.join("; ", notes));
            CommandSelection.setPanelOpen(false);
        });

        // Hide = keyboard-only mode: R draws nothing and keeps the cursor locked, the hint slip names H Panel instead of R Commands,
        // number keys stay with the hotbar (the panel never opens) and the order keys still give orders
        if (tick == 254) press(c, "key.gravesandglory.panel_edit");
        if (tick == 257) clickId(c, CommandPanel.HIDE);
        if (tick == 259) {
            hiddenDetail = "Hide clicked: hidden=" + PanelLayout.hidden + ", edit-mode alpha " + CommandPanel.op() + ", button " + PanelSuite.drawn(CommandPanel.HIDE); // 35% while editing
            shot(c, "panel-edit-hidden-1920.png");
            press(c, "key.gravesandglory.panel_edit");
        }
        if (tick == 264) { hiddenRest1920 = !CommandPanel.editing() && PanelLayout.hidden; sentBefore = OrderIntakeProbe.ordersSent(); selBefore = CommandSelection.selected(); press(c, "key.gravesandglory.menu"); }
        if (tick == 266) { press(c, "key.gravesandglory.attack"); CommandUiChecks.press(c, c.options.hotbarKeys[3]); }
        if (tick == 269) waits.when(tick, 15, () -> CommandPanel.bounds == null && !gg.gravesandglory.BattleHud.hintRows().isEmpty(), null, ok -> {
            hiddenRest1920 &= ok && keyboardOnly(c, "1920");
            shot(c, "panel-hidden-1920.png");
        });
        if (tick == 304) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 854, 480);
        if (tick == 318) { sentBefore = -1; press(c, "key.gravesandglory.menu"); }
        if (tick == 321) waits.when(tick, 15, () -> CommandPanel.bounds == null && sw(c) < 600, null, ok -> {
            hiddenRest854 = ok && keyboardOnly(c, "854");
            shot(c, "panel-hidden-854.png");
            CommandUiChecks.report("cu-edit-hidden", hiddenRest1920 && hiddenRest854, hiddenDetail);
        });

        // opacity 35% through the slider (and Show again): the pennants' cloth at 35% of their alpha, text and icons kept at 60%
        if (tick == 338) press(c, "key.gravesandglory.panel_edit");
        if (tick == 341) clickId(c, CommandPanel.HIDE);
        if (tick == 343) {
            int[] r = rect(CommandPanel.SLIDER);
            if (r != null) { double x0 = r[1] + 3, w = r[3] - 6; PanelSuite.clickGui(c, x0 + (0.35 - 0.20) / 0.80 * w, r[2] + r[4] / 2.0, 0); }
        }
        if (tick == 345) { opacityDetail = "slider clicked at 35%: opacity " + PanelLayout.opacity + ", hidden " + PanelLayout.hidden; shot(c, "panel-edit-opacity-854.png"); press(c, "key.gravesandglory.panel_edit"); }
        if (tick == 349) press(c, "key.gravesandglory.menu");
        if (tick == 352) waits.when(tick, 18, () -> CommandPanel.lastSlide >= 1 && CommandPanel.bounds != null, null, ok -> { opacityOk854 = ok && opacityApplied("854"); shot(c, "panel-opacity-854.png"); });
        if (tick == 372) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        if (tick == 388) waits.when(tick, 17, () -> CommandPanel.lastSlide >= 1 && sw(c) > 427, null, ok -> {
            opacityOk1920 = ok && opacityApplied("1920");
            shot(c, "panel-opacity-1920.png");
            CommandUiChecks.report("cu-edit-opacity", opacityOk854 && opacityOk1920, opacityDetail);
            CommandSelection.setPanelOpen(false);
        });

        // Reset: the left edge, 100%, shown
        if (tick == 409) press(c, "key.gravesandglory.panel_edit");
        if (tick == 412) clickId(c, CommandPanel.RESET);
        if (tick == 414) { resetOk = !PanelLayout.moved() && PanelLayout.opacity == 1f && !PanelLayout.hidden; notes.add("after Reset: " + layout()); clickId(c, CommandPanel.DONE); }
        if (tick == 418) press(c, "key.gravesandglory.menu");
        if (tick == 421) waits.when(tick, 19, () -> CommandPanel.lastSlide >= 1 && CommandPanel.bounds != null, null, ok -> {
            int[] b = CommandPanel.bounds;
            Float a = CommandPanel.lastAlpha.get(CommandPanel.PENNANT);
            boolean left = ok && b[0] == 0 && b[0] + b[2] <= sw(c) * 0.30 + 1e-9 && a != null && Math.abs(a - 0.55f) < 0.01f && !CommandPanel.editing();
            CommandUiChecks.report("cu-edit-reset", resetOk && left, "after Reset: " + layout() + "; opened at " + box(b) + " (left edge, inside 30% of " + sw(c) + "), Attack alpha " + a + "; file " + file());
            CommandSelection.setPanelOpen(false);
        });

        // an arrangement to keep (top-right, 50%), saved by Done, then a disconnect and rejoin reads it back from the file
        if (tick == 444) press(c, "key.gravesandglory.panel_edit");
        if (tick >= 447 && tick <= 454) drag(c, tick, 447, sw(c) + 60, -60);
        if (tick == 456) { int[] r = rect(CommandPanel.SLIDER); if (r != null) { double x0 = r[1] + 3, w = r[3] - 6; PanelSuite.clickGui(c, x0 + (0.50 - 0.20) / 0.80 * w, r[2] + r[4] / 2.0, 0); } }
        if (tick == 458) clickId(c, CommandPanel.DONE);
        if (tick == 461) {
            expect = layout();
            loadsBefore = PanelLayout.loads;
            disconnectStage = 1;
            c.world.disconnect();
            c.disconnect(new gg.gravesandglory.FrontMenuScreen());
        }
        if (tick >= 462 && disconnectStage == 3 && rejoinTick < 0) rejoinTick = tick;
        if (rejoinTick > 0 && tick == rejoinTick + 20) { PanelLayout.ensureLoaded(c); press(c, "key.gravesandglory.panel_edit"); }
        if (rejoinTick > 0 && tick == rejoinTick + 24) {
            String got = layout();
            boolean ok = got.equals(expect) && PanelLayout.loads > loadsBefore && expect.startsWith("top-right") && expect.contains("opacity 0.5");
            CommandUiChecks.report("cu-edit-persist", ok, "saved " + expect + "; after rejoin (read from the file again: loads +" + (PanelLayout.loads - loadsBefore) + ") " + got
                + "; H with no battle HUD: editing=" + CommandPanel.editing() + " (battle " + GravesAndGloryClient.battleActive() + "); file " + file() + "; shots: " + String.join(", ", shotLog));
            CommandUiChecks.finish(c);
        }
    }

    /** After R with the panel hidden: nothing drawn, the panel shut, the cursor locked, the slip shows the arrange key and no Commands; the order key
     *  sent its order and the number key picked no group (checked once, at 1920: sentBefore >= 0). */
    boolean keyboardOnly(MinecraftClient c, String size) {
        var rows = gg.gravesandglory.BattleHud.hintRows();
        List<String> row = rows.isEmpty() ? List.of() : rows.get(0);
        String h = GravesAndGloryClient.hint("panel_edit");
        // at 854x480 the HUD layout leaves the slip out for room (its priority); where it is drawn it must name H Panel, never R Commands
        boolean slip = rows.isEmpty() ? size.equals("854") : row.contains("Panel") && row.contains(h) && !row.contains("Commands");
        boolean ok = CommandPanel.bounds == null && !CommandSelection.panelOpen() && !CommandPanel.capturing() && slip; // the panel never holds the cursor (the lock itself needs a focused window)
        hiddenDetail += "; " + size + " after R: drawn=" + (CommandPanel.bounds != null) + ", panel open=" + CommandSelection.panelOpen() + ", holds the cursor=" + CommandPanel.capturing() + ", cursor locked=" + c.mouse.isCursorLocked() + " (window focused=" + c.isWindowFocused() + "), hint slip row " + row;
        if (sentBefore >= 0) {
            int sent = OrderIntakeProbe.ordersSent() - sentBefore;
            ok &= sent >= 1 && CommandSelection.selected().equals(selBefore);
            hiddenDetail += ", Z sent " + sent + " order(s), key 4 left the selection " + selBefore + " -> " + CommandSelection.selected();
        }
        return ok;
    }

    /** The open panel drawn at the chosen opacity: each idle pennant at Spec pennantAlpha x opacity, text and icons at no less than 60%. */
    boolean opacityApplied(String size) {
        Float a = CommandPanel.lastAlpha.get(CommandPanel.PENNANT);
        float want = 0.55f * PanelLayout.opacity;
        boolean ok = Math.abs(PanelLayout.opacity - 0.35f) < 1e-4 && a != null && Math.abs(a - want) < 0.01f && Math.abs(CommandPanel.op() - 0.35f) < 1e-4 && Math.abs(CommandPanel.ink() - 0.6f) < 1e-4;
        opacityDetail += "; " + size + ": Attack cloth alpha " + a + " (want " + want + "), panel opacity " + CommandPanel.op() + ", text/icon floor " + CommandPanel.ink();
        return ok;
    }

    static void listen() {
        if (listening) return;
        listening = true;
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            EditSuite s = live;
            if (s == null || s.disconnectStage == 0 || s.disconnectStage == 3) return;
            if (s.disconnectStage == 1 && c.world == null && c.player == null) {
                if (++s.offlineTicks < 10) return;
                s.disconnectStage = 2;
                try {
                    Method m = GravesAndGloryClient.class.getDeclaredMethod("startCamp", MinecraftClient.class, boolean.class);
                    m.setAccessible(true);
                    m.invoke(null, c, false);
                } catch (Exception e) { CommandUiChecks.report("cu-edit-persist", false, "rejoin failed: " + e); s.disconnectStage = 3; c.scheduleStop(); }
            } else if (s.disconnectStage == 2 && c.player != null && c.world != null) s.disconnectStage = 3;
        });
    }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) {
        waits.clear();
        if (CommandPanel.editing()) CommandPanel.leaveEdit(c);
        CommandSelection.reset();
        PanelLayout.reset();
        try { Files.deleteIfExists(PanelLayout.file()); } catch (Exception ignored) { }
        PanelLayout.forget();
        live = null;
    }
}
