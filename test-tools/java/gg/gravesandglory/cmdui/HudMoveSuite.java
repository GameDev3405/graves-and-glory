package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandPanel;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.HudLayout;
import gg.gravesandglory.PanelLayout;
import gg.gravesandglory.UiCanvas;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Every battle HUD part movable in edit mode (Kort, 2026-11-12): each part outlined with its name; a drag of the score, the gold line, the
 * objectives and the feed moves them (past an edge: clamped on screen); Done saves hud-layout.json under the player; 1920 -> 854 and a GUI
 * Scale change keep every part on screen at the same places; the file read again gives the same places; Reset puts everything back.
 * Real GLFW input (PanelSuite's helpers). Shots hud-edit and hud-moved at 854, 1920 and 2560. Never included in releases.
 */
public final class HudMoveSuite implements CommandUiChecks.Suite {
    UUID id;
    Path shots;
    final List<String> shotLog = new ArrayList<>(), notes = new ArrayList<>(), fails = new ArrayList<>();
    int[] from;
    int savesBefore, loadsBefore, guiBefore;
    Map<String, HudLayout.Place> kept = new LinkedHashMap<>();

    @Override public int limit() { return 1400; }

    static int sw(MinecraftClient c) { return c.getWindow().getScaledWidth(); }
    static int sh(MinecraftClient c) { return c.getWindow().getScaledHeight(); }
    static void press(MinecraftClient c, String key) { CommandUiChecks.press(c, PanelSuite.find(c, key)); }
    static void mouse(MinecraftClient c, int action) {
        PanelSuite.invoke(c, "onMouseButton", new Class<?>[]{long.class, int.class, int.class, int.class}, c.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT, action, 0);
    }
    static void clickId(MinecraftClient c, int id) { int[] r = PanelSuite.rect(id); if (r != null) PanelSuite.clickGui(c, r[1] + r[3] / 2.0, r[2] + r[4] / 2.0, 0); }
    static String file() { try { return Files.readString(HudLayout.file(), StandardCharsets.UTF_8).replaceAll("\\s+", " "); } catch (Exception e) { return "(no file)"; } }
    static String box(int[] b) { return b == null ? "none" : Arrays.toString(b); }

    /** A part dragged by the cursor from its centre to GUI (tx, ty): press at start, six steps, release at start + 7. */
    void drag(MinecraftClient c, int tick, int start, String part, double tx, double ty) {
        if (tick == start) {
            int[] b = HudLayout.guiBox(part);
            if (b == null) { fails.add("no box for " + part); from = null; return; }
            from = new int[]{b[0] + b[2] / 2, b[1] + Math.min(b[3] / 2, 6)}; // near the top, clear of a smaller part inside the column
            PanelSuite.cursorGui(c, from[0], from[1]);
            mouse(c, GLFW.GLFW_PRESS);
            if (!part.equals(HudLayout.dragging())) fails.add("press on " + part + " at " + Arrays.toString(from) + " grabbed " + HudLayout.dragging());
        } else if (tick > start && tick <= start + 6 && from != null) {
            double f = (tick - start) / 6.0;
            PanelSuite.cursorGui(c, from[0] + (tx - from[0]) * f, from[1] + (ty - from[1]) * f);
        } else if (tick == start + 7) mouse(c, GLFW.GLFW_RELEASE);
    }

    /** Every part with a box lies wholly on screen (canvas parts on the HUD canvas, the relics on the GUI). */
    boolean onScreen(MinecraftClient c, String at) {
        boolean ok = true;
        for (String p : HudLayout.PARTS) {
            int[] b = HudLayout.boxes().get(p);
            if (b == null) continue;
            int W = p.equals("relics") ? sw(c) : UiCanvas.hudWidth(), H = p.equals("relics") ? sh(c) : UiCanvas.hudHeight();
            if (b[0] < 0 || b[1] < 0 || b[0] + b[2] > W || b[1] + b[3] > H) { fails.add(at + ": " + p + " " + box(b) + " off the " + W + "x" + H + " screen"); ok = false; }
        }
        return ok;
    }

    Map<String, HudLayout.Place> places() { Map<String, HudLayout.Place> m = new LinkedHashMap<>(); for (String p : HudLayout.PARTS) if (HudLayout.moved(p)) m.put(p, HudLayout.place(p)); return m; }

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) {
            id = c.player.getUuid();
            shots = FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("shots");
            try { Files.createDirectories(shots); Files.deleteIfExists(HudLayout.file()); Files.deleteIfExists(PanelLayout.file()); } catch (Exception ignored) { }
            HudLayout.reset(); HudLayout.forget(); PanelLayout.forget();
            guiBefore = c.options.getGuiScale().getValue();
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-hud-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandSelection.reset();
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        }
        // edit mode: every part outlined, inside the screen
        if (tick == 60) press(c, "key.gravesandglory.panel_edit");
        if (tick == 66) {
            List<String> missing = new ArrayList<>();
            for (String p : HudLayout.PARTS) if (HudLayout.guiBox(p) == null) missing.add(p);
            boolean ok = CommandPanel.editing() && missing.isEmpty() && onScreen(c, "edit-1920");
            CommandUiChecks.report("cu-hud-edit", ok, "editing=" + CommandPanel.editing() + ", parts outlined " + (HudLayout.PARTS.size() - missing.size()) + " of " + HudLayout.PARTS.size() + (missing.isEmpty() ? "" : ", missing " + missing) + "; boxes " + boxes());
            shot(c, "hud-edit-1920.png");
        }
        // four drags: the gold line past the bottom-left corner (clamped), the score toward the foot, the feed past the top-left, the objectives to the left middle
        if (tick >= 70 && tick <= 77) drag(c, tick, 70, "gold", -80, sh(c) + 80);
        if (tick >= 80 && tick <= 87) drag(c, tick, 80, "score", sw(c) / 2.0, sh(c) * 0.72);
        if (tick >= 90 && tick <= 97) drag(c, tick, 90, "feed", -80, -80);
        if (tick >= 100 && tick <= 107) drag(c, tick, 100, "objectives", 60, sh(c) / 2.0);
        if (tick == 110) {
            var gold = HudLayout.boxes().get("gold");
            int H = UiCanvas.hudHeight();
            boolean clamp = gold != null && gold[0] == HudLayout.MARGIN && gold[1] + gold[3] == H - HudLayout.MARGIN && HudLayout.place("gold") != null && HudLayout.place("gold").h().equals("left") && HudLayout.place("gold").v().equals("bottom");
            boolean moved = HudLayout.moved("score") && HudLayout.moved("feed") && HudLayout.moved("objectives") && HudLayout.dragging() == null;
            boolean on = onScreen(c, "dragged-1920");
            CommandUiChecks.report("cu-hud-drag", clamp && moved && on && fails.isEmpty(), "gold past the corner " + box(gold) + " clamped=" + clamp + "; places " + places() + "; " + (fails.isEmpty() ? "every part on screen" : String.join("; ", fails)));
            fails.clear();
            shot(c, "hud-edit-moved-1920.png");
            savesBefore = HudLayout.saves;
            clickId(c, CommandPanel.DONE);
        }
        if (tick == 114) {
            String f = file();
            kept = places();
            boolean ok = !CommandPanel.editing() && HudLayout.saves == savesBefore + 1 && f.contains(id.toString()) && f.contains("\"gold\"") && f.contains("\"feed\"") && kept.size() >= 4;
            CommandUiChecks.report("cu-hud-save", ok, "Done: editing=" + CommandPanel.editing() + ", saves +" + (HudLayout.saves - savesBefore) + ", file " + f);
        }
        if (tick == 120) shot(c, "hud-moved-1920.png");
        // 1920 -> 854: the same places, every part clamped on screen
        if (tick == 124) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 854, 480);
        if (tick == 144) {
            boolean ok = onScreen(c, "854") && places().equals(kept);
            CommandUiChecks.report("cu-hud-resize", ok && fails.isEmpty(), "854x480 (GUI " + sw(c) + "x" + sh(c) + ", canvas " + UiCanvas.hudWidth() + "x" + UiCanvas.hudHeight() + "): places kept=" + places().equals(kept) + ", boxes " + boxes() + (fails.isEmpty() ? "" : "; " + fails));
            fails.clear();
            shot(c, "hud-moved-854.png");
            press(c, "key.gravesandglory.panel_edit");
        }
        if (tick == 150) { shot(c, "hud-edit-854.png"); press(c, "key.gravesandglory.panel_edit"); }
        // a GUI Scale change at 1920: still on screen, still the same places; then the file read again
        if (tick == 156) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        if (tick == 172) { c.options.getGuiScale().setValue(1); c.onResolutionChanged(); }
        if (tick == 184) {
            boolean ok = onScreen(c, "gui1") && places().equals(kept);
            shot(c, "hud-moved-1920-gui1.png");
            c.options.getGuiScale().setValue(4); c.onResolutionChanged();
            notes.add("GUI 1: " + boxes());
            CommandUiChecks.report("cu-hud-guiscale", ok, "GUI Scale 1 at 1920x1080 (GUI " + sw(c) + "x" + sh(c) + "): places kept=" + places().equals(kept) + (fails.isEmpty() ? ", every part on screen" : "; " + fails));
            fails.clear();
        }
        if (tick == 196) {
            boolean ok = onScreen(c, "gui4") && places().equals(kept);
            shot(c, "hud-moved-1920-gui4.png");
            loadsBefore = HudLayout.loads;
            HudLayout.forget(); HudLayout.reset(); HudLayout.ensureLoaded(c);
            boolean read = HudLayout.loads == loadsBefore + 1 && places().equals(kept);
            CommandUiChecks.report("cu-hud-persist", ok && read && fails.isEmpty(), "GUI 4: on screen=" + ok + "; forgotten and read from the file again: loads +" + (HudLayout.loads - loadsBefore) + ", places " + places() + " (want " + kept + ")" + (fails.isEmpty() ? "" : "; " + fails));
            fails.clear();
            c.options.getGuiScale().setValue(guiBefore); c.onResolutionChanged();
        }
        // 2560x1440: edit with the handles, the moved HUD
        if (tick == 204) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 2560, 1440);
        if (tick == 222) { onScreen(c, "2560"); shot(c, "hud-moved-2560.png"); press(c, "key.gravesandglory.panel_edit"); }
        if (tick == 228) { shot(c, "hud-edit-2560.png"); press(c, "key.gravesandglory.panel_edit"); }
        if (tick == 232) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        // Reset: every part and the panel back where the layout puts them, saved
        if (tick == 250) press(c, "key.gravesandglory.panel_edit");
        if (tick == 254) clickId(c, CommandPanel.RESET);
        if (tick == 256) { savesBefore = HudLayout.saves; clickId(c, CommandPanel.DONE); }
        if (tick == 260) {
            boolean none = places().isEmpty() && !PanelLayout.moved() && !CommandPanel.editing();
            String f = file();
            boolean ok = none && HudLayout.saves == savesBefore + 1 && !f.contains("\"gold\"") && fails.isEmpty();
            CommandUiChecks.report("cu-hud-reset", ok, "after Reset and Done: places " + places() + ", panel moved " + PanelLayout.moved() + ", file " + f + "; 2560 " + (fails.isEmpty() ? "on screen" : fails) + "; shots " + String.join(", ", shotLog));
            shot(c, "hud-reset-1920.png");
            CommandUiChecks.finish(c);
        }
    }

    static String boxes() { StringBuilder b = new StringBuilder(); for (var e : HudLayout.boxes().entrySet()) b.append(e.getKey()).append(box(e.getValue())).append(' '); return b.toString().trim(); }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) {
        if (CommandPanel.editing()) CommandPanel.leaveEdit(c);
        CommandSelection.reset();
        HudLayout.reset(); PanelLayout.reset();
        try { Files.deleteIfExists(HudLayout.file()); Files.deleteIfExists(PanelLayout.file()); } catch (Exception ignored) { }
        HudLayout.forget(); PanelLayout.forget();
        c.options.getGuiScale().setValue(guiBefore); c.onResolutionChanged();
    }
}
