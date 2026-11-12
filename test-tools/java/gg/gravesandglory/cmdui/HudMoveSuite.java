package gg.gravesandglory.cmdui;

import gg.gravesandglory.BattleHud;
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
            tagsClear("1920");
            shot(c, "hud-edit-1920.png");
            // the panel's caption and its tab row take a press before any HUD part (or edit-mode ghost) under them
            StringBuilder grab = new StringBuilder();
            int[] cap = null; for (int[] d : CommandPanel.lastDrawn) if (d[0] == CommandPanel.CAPTION) cap = new int[]{d[1], d[2], d[3], d[4]};
            int[] pb = CommandPanel.bounds;
            double[][] presses = {cap == null ? null : new double[]{cap[0] + cap[2] / 2.0, cap[1] + cap[3] / 2.0}, pb == null ? null : new double[]{pb[0] + pb[2] / 2.0, pb[1] + 4}};
            String[] what = {"caption", "tab row"};
            for (int i = 0; i < 2; i++) {
                if (presses[i] == null) { grab.append(what[i] + " not drawn; "); continue; }
                CommandPanel.editPress(presses[i][0], presses[i][1]);
                boolean panel = CommandPanel.dragging() && HudLayout.dragging() == null;
                if (!panel) grab.append(what[i] + " press went to " + (HudLayout.dragging() == null ? "nothing" : HudLayout.dragging()) + "; ");
                CommandPanel.editRelease();
            }
            CommandUiChecks.report("cu-hud-panel-grab", grab.length() == 0, grab.length() == 0 ? "a press on the panel's caption and on its tab row drags the panel, not a HUD part under it" : grab.toString());
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
            tagsClear("moved-1920");
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
        if (tick == 150) { tagsClear("854"); shot(c, "hud-edit-854.png"); press(c, "key.gravesandglory.panel_edit"); }
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
        if (tick == 238) { tagsClear("2560"); shot(c, "hud-edit-2560.png"); press(c, "key.gravesandglory.panel_edit"); } // edit mode has frames to draw at the new size first
        if (tick == 242) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
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
            press(c, "key.gravesandglory.panel_edit");
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 854, 480);
        }
        // the default layout in edit mode at 854 and 2560 (1920 is above): the name chips again
        if (tick == 280) { tagsClear("default-854"); shot(c, "hud-edit-default-854.png"); GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 2560, 1440); }
        if (tick == 300) {
            tagsClear("default-2560"); shot(c, "hud-edit-default-2560.png");
            CommandUiChecks.report("cu-hud-tags", tagBad.length() == 0 && tagsSeen == 6, "edit-mode name chips at 1920, moved 1920, moved 854, moved 2560, default 854 and default 2560 (" + tagsSeen + " looked at): "
                + (tagBad.length() == 0 ? "each outside its own box, none covers another chip, another part, the panel or its caption or toolbar, and the toolbar covers no part" : tagBad));
            press(c, "key.gravesandglory.panel_edit");
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        }
        if (tick >= 320) resizeTick(c, tick);
    }

    // ---- sizes (Kort, 2026-11-12: every piece resized by dragging its corner) ----

    final StringBuilder sizeBad = new StringBuilder();
    final List<String> sizeLog = new ArrayList<>(), labels = new ArrayList<>();
    double[] hfrom, hto;
    Map<String, HudLayout.Place> sized = new LinkedHashMap<>();
    double panelSized;

    static boolean near(double a, double b) { return Math.abs(a - b) < 0.051; }

    /** A corner handle dragged to make its piece want times its own size (the panel when part is null): press on the handle at start,
     *  six steps to where the handle's corner would be at that size, release at start + 7; the size label read on the way. */
    void handleDrag(MinecraftClient c, int tick, int start, String part, double want) {
        String name = part == null ? "panel" : part;
        if (tick == start) {
            int[] h = part == null ? CommandPanel.handle : HudLayout.lastHandles.get(part), b = part == null ? CommandPanel.bounds : HudLayout.guiBox(part);
            if (h == null || b == null) { sizeBad.append("no handle drawn for the " + name + "; "); hfrom = null; return; }
            boolean right = h[0] > b[0] + b[2] / 2.0, down = h[1] > b[1] + b[3] / 2.0;
            double fx = right ? b[0] : b[0] + b[2], fy = down ? b[1] : b[1] + b[3], cx = right ? b[0] + b[2] : b[0], cy = down ? b[1] + b[3] : b[1];
            double s0 = part == null ? CommandPanel.panelScale() : HudLayout.drawnScale(part);
            hfrom = new double[]{h[0] + h[2] / 2.0, h[1] + h[3] / 2.0};
            hto = new double[]{fx + (cx - fx) * want / s0 + (hfrom[0] - cx), fy + (cy - fy) * want / s0 + (hfrom[1] - cy)}; // the handle sits outside the corner: it keeps its offset
            PanelSuite.cursorGui(c, hfrom[0], hfrom[1]);
            mouse(c, GLFW.GLFW_PRESS);
            boolean took = part == null ? CommandPanel.resizing() : part.equals(HudLayout.resizing());
            if (!took) sizeBad.append("a press on the " + name + "'s handle " + box(h) + " did not start a resize (part " + HudLayout.resizing() + ", panel " + CommandPanel.resizing() + ", moving " + HudLayout.dragging() + "); ");
        } else if (tick > start && tick <= start + 6 && hfrom != null) {
            double f = (tick - start) / 6.0;
            PanelSuite.cursorGui(c, hfrom[0] + (hto[0] - hfrom[0]) * f, hfrom[1] + (hto[1] - hfrom[1]) * f);
            if (tick == start + 6) labels.add(name + " " + HudLayout.lastSizeLabel);
        } else if (tick == start + 7) mouse(c, GLFW.GLFW_RELEASE);
    }

    /** Every part and the panel lie wholly on screen at their sizes. */
    boolean allOnScreen(MinecraftClient c, String at) {
        boolean ok = onScreen(c, at);
        int[] b = CommandPanel.bounds;
        if (b != null && (b[0] < 0 || b[1] < 0 || b[0] + b[2] > sw(c) || b[1] + b[3] > sh(c))) { fails.add(at + ": the panel " + box(b) + " off the " + sw(c) + "x" + sh(c) + " GUI"); ok = false; }
        return ok;
    }

    String sizes() {
        StringBuilder b = new StringBuilder();
        for (String p : HudLayout.PARTS) if (HudLayout.scale(p) != 1) b.append(p).append(' ').append(Math.round(HudLayout.scale(p) * 100)).append("% ");
        return b.append("panel ").append(Math.round(PanelLayout.scale * 100)).append('%').toString();
    }

    /** The panel's click targets at its size: a hovered pennant, a pill, the Formation pennant opening the tray, a tray cell, and a
     *  group card's formation box on a resized column. */
    final Map<String, String> clicks = new LinkedHashMap<>();
    boolean clickOk = true, hoverSeen;
    void clickTick(MinecraftClient c, int t, String size) {
        if (t == 0) { CommandSelection.select("all", false); CommandSelection.setPanelOpen(true); CommandSelection.setTrayOpen(false); }
        if (t == 1) hoverSeen = false;
        if (t >= 1 && t <= 4) { hoverSeen |= CommandPanel.hovered == CommandPanel.PENNANT + 2; PanelSuite.cursorTo(c, CommandPanel.PENNANT + 2); } // each tick until the panel reports it
        if (t == 4) {
            boolean hov = hoverSeen || CommandPanel.hovered == CommandPanel.PENNANT + 2;
            clicks.put(size + " hover", hov + " (" + PanelSuite.hoverDiag(c) + ")"); clickOk &= hov;
            PanelSuite.click(c, CommandPanel.PILL + 1, 0);
        }
        if (t == 6) {
            boolean ok = CommandSelection.selected().equals(List.of("ranged"));
            clicks.put(size + " pill", ok + " " + CommandSelection.selected()); clickOk &= ok;
            CommandSelection.select("infantry", false);
            PanelSuite.click(c, CommandPanel.PENNANT + 4, 0);
        }
        if (t == 10) {
            boolean ok = CommandSelection.trayOpen();
            clicks.put(size + " formation pennant", "tray open " + ok); clickOk &= ok;
            if (size.equals("180%")) shot(c, "hud-resized-panel-1920.png");
            PanelSuite.click(c, CommandPanel.CELL + 1, 0);
        }
        if (t == 14) {
            boolean ok = "hold".equals(CommandSelection.aiming()) && "block".equals(PanelSuite.formation());
            clicks.put(size + " tray cell", ok + " (aiming '" + CommandSelection.aiming() + "', formation " + PanelSuite.formation() + ")"); clickOk &= ok;
            CommandUiChecks.press(c, PanelSuite.find(c, "key.gravesandglory.cancel"));
        }
        if (t == 18) { CommandSelection.select("infantry", false); CommandSelection.setPanelOpen(true); CommandSelection.setTrayOpen(false); }
        if (t == 21) {
            int[] b = BattleHud.toGui(BattleHud.formBox("infantry"));
            if (b == null) { clicks.put(size + " card", "no formation box drawn"); clickOk = false; return; }
            PanelSuite.clickGui(c, b[0] + b[2] / 2.0, b[1] + b[3] / 2.0, 0);
        }
        if (t == 24) {
            boolean ok = CommandSelection.trayOpen() && CommandSelection.selected().equals(List.of("infantry"));
            clicks.put(size + " card formation (column " + Math.round(HudLayout.drawnScale("column") * 100) + "%)", "tray open " + ok); clickOk &= ok;
            CommandSelection.setTrayOpen(false); CommandSelection.setPanelOpen(false);
        }
    }

    void resizeTick(MinecraftClient c, int tick) {
        // at 1920: edit mode, the handles drawn on every part and the panel
        if (tick == 320) { HudLayout.reset(); PanelLayout.reset(); press(c, "key.gravesandglory.panel_edit"); }
        if (tick == 326) {
            List<String> missing = new ArrayList<>();
            for (String p : HudLayout.PARTS) if (HudLayout.guiBox(p) != null && !HudLayout.lastHandles.containsKey(p)) missing.add(p);
            if (CommandPanel.handle == null) missing.add("panel");
            for (var e : HudLayout.lastHandles.entrySet()) { int[] b = HudLayout.guiBox(e.getKey()), h = e.getValue(); if (b != null && meets(h, b)) missing.add(e.getKey() + " handle " + box(h) + " covers its part " + box(b)); if (h[2] > 9) missing.add(e.getKey() + " handle " + h[2] + " px"); }
            if (CommandPanel.handle != null && CommandPanel.bounds != null && meets(CommandPanel.handle, CommandPanel.bounds)) missing.add("panel handle covers the panel");
            CommandUiChecks.report("cu-hud-handles", CommandPanel.editing() && missing.isEmpty(), "edit mode: a corner handle outside every part and the panel, covering none" + (missing.isEmpty() ? " " + HudLayout.lastHandles.keySet() + " + panel " + box(CommandPanel.handle) : "; MISSING " + missing));
            shot(c, "hud-handles-1920.png");
        }
        // grow, shrink, the clamps, and a resize at the screen's edge
        if (tick >= 330 && tick <= 337) handleDrag(c, tick, 330, "gold", 1.53); // 153% asked: the 150% magnet holds it
        if (tick >= 340 && tick <= 347) handleDrag(c, tick, 340, "objectives", 0.8);
        if (tick >= 350 && tick <= 357) handleDrag(c, tick, 350, "column", 1.25);
        if (tick >= 360 && tick <= 367) handleDrag(c, tick, 360, "houses", 4);
        if (tick >= 370 && tick <= 377) handleDrag(c, tick, 370, "score", 0.1);
        if (tick >= 380 && tick <= 387) drag(c, tick, 380, "feed", sw(c) + 80, sh(c) / 2.0);
        if (tick >= 390 && tick <= 397) handleDrag(c, tick, 390, "feed", 5);
        if (tick >= 400 && tick <= 407) handleDrag(c, tick, 400, null, 1.8);
        if (tick == 410) {
            double gold = HudLayout.scale("gold"), obj = HudLayout.scale("objectives"), col = HudLayout.scale("column");
            boolean grow = gold == 1.5 && near(col, 1.25), shrink = near(obj, 0.8), label = labels.contains("gold 140%") || labels.stream().anyMatch(l -> l.startsWith("gold ") && l.endsWith("%")); label &= labels.stream().anyMatch(l -> l.startsWith("panel ") && l.endsWith("%"));
            boolean on = allOnScreen(c, "resized-1920");
            CommandUiChecks.report("cu-hud-size", grow && shrink && label && on && sizeBad.length() == 0 && HudLayout.resizing() == null,
                "handles dragged: gold " + gold + " (153% asked, want the 150% magnet), column " + col + " (want 1.25), objectives " + obj + " (want 0.8); the size shown while dragging " + labels + "; boxes " + boxes() + (fails.isEmpty() ? "; all on screen" : "; " + fails) + (sizeBad.length() == 0 ? "" : "; " + sizeBad));
            fails.clear();
            double houses = HudLayout.scale("houses"), score = HudLayout.scale("score");
            CommandUiChecks.report("cu-hud-size-clamp", houses <= PanelLayout.MAX_SCALE && near(houses, PanelLayout.MAX_SCALE) && near(score, PanelLayout.MIN_SCALE),
                "the house traits dragged to 4x: " + houses + " (max " + PanelLayout.MAX_SCALE + "); the score dragged to 0.1x: " + score + " (min " + PanelLayout.MIN_SCALE + ")");
            int[] feed = HudLayout.boxes().get("feed"); int W = UiCanvas.hudWidth(), H = UiCanvas.hudHeight();
            boolean edge = feed != null && feed[0] >= HudLayout.MARGIN && feed[0] + feed[2] <= W - HudLayout.MARGIN && feed[1] >= HudLayout.MARGIN && feed[1] + feed[3] <= H - HudLayout.MARGIN && HudLayout.scale("feed") > 1;
            CommandUiChecks.report("cu-hud-size-edge", edge, "the feed at the right edge, its handle dragged to 5x: size " + HudLayout.scale("feed") + " drawn at " + HudLayout.drawnScale("feed") + ", box " + box(feed) + " on the " + W + "x" + H + " canvas");
            int[] pb = CommandPanel.bounds;
            boolean panel = near(PanelLayout.scale, 1.8) && pb != null && pb[0] >= 0 && pb[1] >= 0 && pb[0] + pb[2] <= sw(c) && pb[1] + pb[3] <= sh(c) && PanelLayout.moved();
            CommandUiChecks.report("cu-hud-size-panel", panel, "the panel's handle dragged to 1.8x: scale " + PanelLayout.scale + " drawn at " + CommandPanel.panelScale() + ", bounds " + box(pb) + " on the " + sw(c) + "x" + sh(c) + " GUI");
            tagsClear("resized-1920");
            shot(c, "hud-resize-edit-1920.png");
            savesBefore = HudLayout.saves;
            clickId(c, CommandPanel.DONE);
        }
        if (tick == 414) {
            String f = file(), pf; try { pf = Files.readString(PanelLayout.file(), StandardCharsets.UTF_8).replaceAll("\\s+", " "); } catch (Exception e) { pf = "(no file)"; }
            sized = places(); panelSized = PanelLayout.scale;
            boolean ok = !CommandPanel.editing() && HudLayout.saves == savesBefore + 1 && f.contains("\"s\": 1.5") && f.contains("\"s\": 0.8") && pf.contains("\"scale\": 1.8");
            CommandUiChecks.report("cu-hud-size-save", ok, "Done: sizes " + sizes() + "; hud-layout.json " + f + "; command-panel.json " + pf);
        }
        if (tick == 418) shot(c, "hud-resized-1920.png");
        // the panel's targets at 180%
        if (tick >= 420 && tick <= 444) clickTick(c, tick - 420, "180%");
        // a GUI Scale change keeps every size and place, on screen; the files read again give them back
        if (tick == 448) { c.options.getGuiScale().setValue(2); c.onResolutionChanged(); }
        if (tick == 460) {
            boolean ok = allOnScreen(c, "gui2") && places().equals(sized) && PanelLayout.scale == panelSized;
            shot(c, "hud-resized-1920-gui2.png");
            c.options.getGuiScale().setValue(guiBefore); c.onResolutionChanged();
            loadsBefore = HudLayout.loads;
            HudLayout.forget(); HudLayout.reset(); PanelLayout.forget(); PanelLayout.reset(); HudLayout.ensureLoaded(c); PanelLayout.ensureLoaded(c);
            boolean read = places().equals(sized) && PanelLayout.scale == panelSized;
            CommandUiChecks.report("cu-hud-size-persist", ok && read, "GUI Scale 2 (GUI " + sw(c) + "x" + sh(c) + "): sizes and places kept=" + ok + (fails.isEmpty() ? "" : " " + fails) + "; read from the files again: " + read + " (" + sizes() + ")");
            fails.clear();
        }
        // the panel's targets at 60%
        if (tick == 470) PanelLayout.scale = PanelLayout.MIN_SCALE;
        if (tick >= 474 && tick <= 498) clickTick(c, tick - 474, "70%");
        if (tick == 500) {
            CommandUiChecks.report("cu-hud-size-clicks", clickOk, "the panel at 180% and its 70% minimum (a resized column): " + clicks);
            PanelLayout.scale = panelSized;
        }
        // the resized layout at 854 and 2560: in play and in edit mode
        int[][] sizes = {{504, 854, 480}, {540, 2560, 1440}};
        for (int[] z : sizes) {
            if (tick == z[0]) GLFW.glfwSetWindowSize(c.getWindow().getHandle(), z[1], z[2]);
            if (tick == z[0] + 18) { allOnScreen(c, "resized-" + z[1]); shot(c, "hud-resized-" + z[1] + ".png"); press(c, "key.gravesandglory.panel_edit"); }
            if (tick == z[0] + 32) { tagsClear("resized-" + z[1]); shot(c, "hud-resize-edit-" + z[1] + ".png"); press(c, "key.gravesandglory.panel_edit"); }
        }
        // Reset puts every size back too
        if (tick == 580) { GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080); }
        if (tick == 596) press(c, "key.gravesandglory.panel_edit");
        if (tick == 602) clickId(c, CommandPanel.RESET);
        if (tick == 604) clickId(c, CommandPanel.DONE);
        if (tick == 608) {
            boolean none = true; for (String p : HudLayout.PARTS) if (HudLayout.scale(p) != 1 || HudLayout.drawnScale(p) != 1) none = false;
            String f = file();
            boolean ok = none && PanelLayout.scale == 1 && CommandPanel.panelScale() == 1 && places().isEmpty() && !f.contains("\"s\"");
            CommandUiChecks.report("cu-hud-size-reset", ok, "after Reset and Done: " + sizes() + ", drawn panel " + CommandPanel.panelScale() + ", file " + f);
            CommandUiChecks.report("cu-hud-size-tags", tagBad.length() == 0, "the name chips on the resized layout at 1920, 854 and 2560: " + (tagBad.length() == 0 ? "clear of each other, every part, the panel and the vanilla HUD" : tagBad));
            CommandUiChecks.report("cu-hud-size-screens", fails.isEmpty(), "the resized layout at 854 and 2560: " + (fails.isEmpty() ? "every part and the panel on screen" : fails));
        }
        if (tick == 612) {
            try {
                String id = c.player.getUuid().toString();
                Files.writeString(HudLayout.file(), "{\"version\":1,\"profiles\":{\"" + id + "\":{\"gold\":{\"h\":\"left\",\"v\":\"top\",\"x\":0.1,\"y\":0.1,\"s\":\"huge\"},"
                    + "\"objectives\":{\"h\":\"right\",\"v\":\"top\",\"x\":0.05,\"y\":0.05,\"s\":{\"bad\":1}},\"column\":{\"h\":\"left\",\"v\":\"bottom\",\"x\":0.02,\"y\":0.02,\"s\":1.5}}}}", StandardCharsets.UTF_8);
            } catch (Exception e) { sizeBad.append("could not write the corrupted file: " + e + "; "); }
            HudLayout.forget(); HudLayout.reset(); HudLayout.ensureLoaded(c);
            var pl = places();
            boolean ok = pl.size() == 3 && pl.get("gold") != null && pl.get("gold").s() == 1 && pl.get("objectives") != null && pl.get("objectives").s() == 1 && pl.get("column") != null && pl.get("column").s() == 1.5 && sizeBad.length() == 0;
            CommandUiChecks.report("cu-hud-size-corrupt", ok, "a file with gold's size \"huge\" and objectives' an object: places " + pl + (sizeBad.length() == 0 ? "" : "; " + sizeBad));
            HudLayout.reset(); HudLayout.save(c);
        }
        if (tick == 616) CommandUiChecks.finish(c);
    }

    static String boxes() { StringBuilder b = new StringBuilder(); for (var e : HudLayout.boxes().entrySet()) b.append(e.getKey()).append(box(e.getValue())).append(' '); return b.toString().trim(); }

    final StringBuilder tagBad = new StringBuilder();
    int tagsSeen;

    /** Edit mode's name tags (HudLayout.lastTags) cover neither one another nor the panel's caption or toolbar. */
    void tagsClear(String size) {
        if (!CommandPanel.editing() || HudLayout.lastTags.isEmpty()) { tagBad.append(size + ": no tags drawn; "); return; }
        tagsSeen++;
        int[] tr = PanelSuite.rect(CommandPanel.TOOLBAR), tb = tr == null ? null : Arrays.copyOfRange(tr, 1, 5), vw = CommandPanel.vanillaHudWide(c().getWindow().getScaledWidth(), c().getWindow().getScaledHeight());
        if (tb != null) {
            if (tb[0] < 0 || tb[1] < 0 || tb[0] + tb[2] > c().getWindow().getScaledWidth() || tb[1] + tb[3] > c().getWindow().getScaledHeight()) tagBad.append(size + ": the toolbar " + box(tb) + " off screen; ");
            if (meets(tb, vw)) tagBad.append(size + ": the toolbar " + box(tb) + " covers the vanilla HUD " + box(vw) + "; ");
            if (CommandPanel.bounds != null && meets(tb, CommandPanel.bounds)) tagBad.append(size + ": the toolbar " + box(tb) + " covers the panel " + box(CommandPanel.bounds) + "; ");
        }
        var tags = new ArrayList<>(HudLayout.lastTags.entrySet());
        for (int i = 0; i < tags.size(); i++) {
            int[] a = tags.get(i).getValue();
            for (int j = i + 1; j < tags.size(); j++) if (meets(a, tags.get(j).getValue())) tagBad.append(size + ": " + tags.get(i).getKey() + " tag covers " + tags.get(j).getKey() + "'s; ");
            for (int[] d : CommandPanel.lastDrawn) if ((d[0] == CommandPanel.CAPTION || d[0] == CommandPanel.TOOLBAR || d[0] == CommandPanel.EDGE) && meets(a, new int[]{d[1], d[2], d[3], d[4]}))
                tagBad.append(size + ": " + tags.get(i).getKey() + " tag covers the panel's " + (d[0] == CommandPanel.CAPTION ? "caption" : d[0] == CommandPanel.TOOLBAR ? "toolbar" : "outline") + "; ");
            for (String p : HudLayout.PARTS) { int[] b = HudLayout.guiBox(p); if (b != null && meets(a, b)) tagBad.append(size + ": " + tags.get(i).getKey() + " tag covers " + (p.equals(tags.get(i).getKey()) ? "its own part" : "the " + p + " part") + " " + box(b) + "; "); }
            int[] van = CommandPanel.vanillaHudWide(c().getWindow().getScaledWidth(), c().getWindow().getScaledHeight());
            if (meets(a, van)) tagBad.append(size + ": " + tags.get(i).getKey() + " tag covers the vanilla HUD (hotbar, offhand, hearts) " + box(van) + "; ");
        }
        // leaders: none across the panel's toolbar, caption or outline or the vanilla HUD
        int[] van = CommandPanel.vanillaHudWide(c().getWindow().getScaledWidth(), c().getWindow().getScaledHeight());
        for (var e : HudLayout.lastLeaders.entrySet()) for (int[] l : e.getValue()) {
            if (meets(l, van)) tagBad.append(size + ": the " + e.getKey() + " leader crosses the vanilla HUD; ");
            for (int[] d : CommandPanel.lastDrawn) if ((d[0] == CommandPanel.CAPTION || d[0] == CommandPanel.TOOLBAR || d[0] == CommandPanel.EDGE) && meets(l, new int[]{d[1], d[2], d[3], d[4]}))
                tagBad.append(size + ": the " + e.getKey() + " leader crosses the panel's " + (d[0] == CommandPanel.CAPTION ? "caption" : d[0] == CommandPanel.TOOLBAR ? "toolbar" : "outline") + "; ");
        }
        for (int[] d : CommandPanel.lastDrawn) if (d[0] == CommandPanel.TOOLBAR || d[0] == CommandPanel.CAPTION) for (String p : HudLayout.PARTS) { // the caption sits on the panel's corner; only a part drawn now counts there (a ghost under it gives the panel the press, cu-hud-panel-grab)
            int[] b = HudLayout.guiBox(p);
            if (b != null && CommandPanel.bounds != null && meets(b, CommandPanel.bounds)) continue; // a part the player put under the panel itself: the toolbar beside it covers nothing more
            if (b != null && (d[0] == CommandPanel.TOOLBAR || HudLayout.drawn(p)) && meets(new int[]{d[1], d[2], d[3], d[4]}, b)) tagBad.append(size + ": the " + (d[0] == CommandPanel.CAPTION ? "caption" : "toolbar") + " covers the " + p + " part; ");
        }
    }
    static MinecraftClient c() { return MinecraftClient.getInstance(); }
    static boolean meets(int[] a, int[] b) { return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3]; }

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
