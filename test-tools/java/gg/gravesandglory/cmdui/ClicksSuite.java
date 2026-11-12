package gg.gravesandglory.cmdui;

import gg.gravesandglory.BattleHud;
import gg.gravesandglory.CommandPanel;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.UiCanvas;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Every formation choice by mouse (Kort, 2026-11-11: "why cant i click which hold line i want?"). Through the GLFW callbacks a real mouse drives:
 * a tray cell clicked (no number key) starts the Formation aim in that shape; a click on a group card's formation opens the tray for that group;
 * the wheel during the aim steps the shape and never turns the hotbar; the aim strip shows under the seals, clear of every HUD box and the crosshair.
 * Shots of the tray hover, the aim strip and the card hover at 1920x1080, 854x480 and 2560x1440 go to <run>/command-ui/shots.
 */
public final class ClicksSuite implements CommandUiChecks.Suite {
    static final int[][] SIZES = {{1920, 1080}, {854, 480}, {2560, 1440}};
    static final int STEP = 110, BASE = 50;
    UUID id;
    Path shots;
    int[] initialSize;
    final Waits waits = new Waits();
    final List<String> shotLog = new ArrayList<>(), stripFails = new ArrayList<>(), log = new ArrayList<>();
    final Map<String, String> trayHover = new LinkedHashMap<>(), cardHover = new LinkedHashMap<>();
    int stripChecked;
    // per-size results
    boolean[] cellOk = new boolean[3], cardOk = new boolean[3], staleOk = new boolean[3], wheelOk = new boolean[3];
    int slotBefore, sentBefore;

    @Override public int limit() { return BASE + STEP * 3 + 200; }

    @Override public void tick(MinecraftClient c, int tick) {
        waits.pump(tick);
        if (tick == 1) {
            id = c.player.getUuid();
            shots = FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("shots");
            try { Files.createDirectories(shots); } catch (Exception ignored) { }
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-clicks-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> { OrderIntakeProbe.respawning(b, id, false); OrderIntakeProbe.resetCharge(b, id); });
            CommandSelection.reset();
            initialSize = new int[]{c.getWindow().getWidth(), c.getWindow().getHeight()};
        }
        for (int k = 0; k < 3; k++) {
            int t = tick - (BASE + STEP * k);
            if (t >= 0 && t < STEP) sizeStep(c, tick, t, k);
        }
        int end = BASE + STEP * 3;
        if (tick == end) {
            CommandUiChecks.report("cu-clicks-tray-cell", allOf(cellOk), "a tray cell clicked by mouse (no number key) at 1920, 854 and 2560: " + Arrays.toString(cellOk) + "; " + String.join("; ", filter("cell")));
            CommandUiChecks.report("cu-clicks-card-form", allOf(cardOk), "a click on a group card's formation opened the tray for that group: " + Arrays.toString(cardOk) + "; " + String.join("; ", filter("card")));
            CommandUiChecks.report("cu-clicks-stale-form", allOf(staleOk), "after the selection changed, a click on the old group's formation box opened no tray: " + Arrays.toString(staleOk) + "; " + String.join("; ", filter("stale")));
            CommandUiChecks.report("cu-clicks-wheel", allOf(wheelOk), "the wheel during the Formation aim stepped the shape and left the hotbar: " + Arrays.toString(wheelOk) + "; " + String.join("; ", filter("wheel")));
            boolean hovers = trayHover.size() == 3 && trayHover.values().stream().allMatch(v -> v.startsWith("hovered")) && cardHover.size() == 3 && cardHover.values().stream().allMatch(v -> v.startsWith("hovered"));
            CommandUiChecks.report("cu-clicks-hover", hovers, "tray cell hover " + trayHover + "; card formation hover " + cardHover);
            CommandUiChecks.report("cu-clicks-strip-clear", stripFails.isEmpty() && stripChecked == 3, stripChecked + " aim strips checked: " + (stripFails.isEmpty() ? "inside the screen, off every HUD box and the crosshair" : String.join("; ", stripFails)) + "; shots: " + String.join(", ", shotLog));
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), initialSize[0], initialSize[1]);
        }
        if (tick == end + 10) CommandUiChecks.finish(c);
    }

    static boolean allOf(boolean[] a) { for (boolean b : a) if (!b) return false; return true; }
    List<String> filter(String p) { List<String> o = new ArrayList<>(); for (String s : log) if (s.startsWith(p)) o.add(s); return o; }

    /** One window size: the tray hover and a cell click, the aim strip and the wheel, then the card hover and a formation click. */
    void sizeStep(MinecraftClient c, int tick, int t, int k) {
        int w = SIZES[k][0], h = SIZES[k][1]; String size = String.valueOf(w);
        if (t == 0) { cancelAim(c); CommandSelection.setPanelOpen(false); GLFW.glfwSetWindowSize(c.getWindow().getHandle(), w, h); }
        // the tray: opened, a cell hovered (a shot), then clicked
        if (t == 14) { CommandSelection.select("infantry", false); CommandSelection.setPanelOpen(true); CommandSelection.setTrayOpen(true); setFormation("line"); PanelSuite.cursorCentre(c); }
        if (t == 18) waits.when(tick, 12, () -> CommandPanel.hovered == CommandPanel.CELL + 1, () -> PanelSuite.cursorTo(c, CommandPanel.CELL + 1), ok -> {
            trayHover.put(size, (ok ? "hovered" : "NOT hovered") + " (id " + CommandPanel.hovered + ")");
            shot(c, "clicks-tray-hover-" + size + ".png");
        });
        if (t == 34) { sentBefore = OrderIntakeProbe.ordersSent(); PanelSuite.click(c, CommandPanel.CELL + 1, 0); }
        if (t == 37) {
            boolean ok = "hold".equals(CommandSelection.aiming()) && "block".equals(PanelSuite.formation()) && !CommandSelection.trayOpen() && OrderIntakeProbe.ordersSent() == sentBefore;
            cellOk[k] = ok;
            log.add("cell " + size + ": aiming '" + CommandSelection.aiming() + "' shape " + PanelSuite.formation() + " tray open " + CommandSelection.trayOpen() + " orders sent " + (OrderIntakeProbe.ordersSent() - sentBefore));
        }
        // the aim strip, then the wheel: down steps to the next shape, up back; the hotbar slot stays
        if (t == 40) waits.when(tick, 10, () -> BattleHud.aimStrip != null, null, ok -> { stripClear(c, size); shot(c, "clicks-aim-strip-" + size + ".png"); });
        if (t == 52) { slotBefore = c.player.getInventory().selectedSlot; scroll(c, -1); }
        if (t == 54) { wheelDown = PanelSuite.formation(); scroll(c, 1); scroll(c, 1); }
        if (t == 56) {
            String back = PanelSuite.formation(); int slot = c.player.getInventory().selectedSlot;
            wheelOk[k] = "wedge".equals(wheelDown) && "line".equals(back) && slot == slotBefore && "hold".equals(CommandSelection.aiming());
            log.add("wheel " + size + ": block, down -> " + wheelDown + ", up twice -> " + back + "; hotbar slot " + slotBefore + " -> " + slot + "; aiming '" + CommandSelection.aiming() + "'");
            waits.when(tick, 6, () -> BattleHud.aimStrip != null, null, ok -> shot(c, "clicks-aim-strip-wheel-" + size + ".png"));
        }
        if (t == 66) cancelAim(c);
        // the group card: the formation box hovered (a shot), then clicked: the tray opens for that group
        if (t == 70) { CommandSelection.select("ranged", false); CommandSelection.setPanelOpen(true); CommandSelection.setTrayOpen(false); PanelSuite.cursorCentre(c); }
        if (t == 74) waits.when(tick, 12, () -> ("form-ranged").equals(BattleHud.hoveredCard), () -> cursorToForm(c, "ranged"), ok -> {
            cardHover.put(size, (ok ? "hovered" : "NOT hovered") + " (" + BattleHud.hoveredCard + ", box " + Arrays.toString(BattleHud.toGui(BattleHud.formBox("ranged"))) + ")");
            shot(c, "clicks-card-hover-" + size + ".png");
        });
        if (t == 90) { CommandSelection.select("infantry", false); }
        if (t == 93) {
            int[] b = BattleHud.toGui(BattleHud.formBox("infantry"));
            if (b == null) { log.add("card " + size + ": no formation box drawn for infantry"); return; }
            PanelSuite.clickGui(c, b[0] + b[2] / 2.0, b[1] + b[3] / 2.0, 0);
        }
        if (t == 96) {
            boolean ok = CommandSelection.trayOpen() && CommandSelection.panelOpen() && CommandSelection.selected().equals(List.of("infantry")) && CommandSelection.aiming().isEmpty();
            cardOk[k] = ok;
            log.add("card " + size + ": tray open " + CommandSelection.trayOpen() + ", selection " + CommandSelection.selected() + ", aiming '" + CommandSelection.aiming() + "'");
            shot(c, "clicks-card-tray-" + size + ".png");
        }
        // a changed selection: a click where infantry's formation box was (before any frame redraws the column) must not open a tray
        if (t == 100) {
            int[] old = BattleHud.toGui(BattleHud.formBox("infantry"));
            CommandSelection.setTrayOpen(false); CommandSelection.select("ranged", false);
            if (old == null) { log.add("stale " + size + ": no infantry formation box to test"); return; }
            PanelSuite.clickGui(c, old[0] + old[2] / 2.0, old[1] + old[3] / 2.0, 0);
            staleOk[k] = !CommandSelection.trayOpen();
            log.add("stale " + size + ": after selecting ranged, a click on infantry's old formation box left the tray " + (CommandSelection.trayOpen() ? "OPEN" : "shut") + ", selection " + CommandSelection.selected());
        }
        if (t == 104) { CommandSelection.setTrayOpen(false); CommandSelection.setPanelOpen(false); }
    }
    String wheelDown = "";

    static void setFormation(String shape) {
        try { var f = Class.forName("gg.gravesandglory.CommandMode").getDeclaredField("formation"); f.setAccessible(true); f.set(null, shape); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    void cancelAim(MinecraftClient c) { if (!CommandSelection.aiming().isEmpty()) CommandUiChecks.press(c, PanelSuite.find(c, "key.gravesandglory.cancel")); }

    static void cursorToForm(MinecraftClient c, String group) {
        int[] b = BattleHud.toGui(BattleHud.formBox(group));
        if (b != null) PanelSuite.cursorGui(c, b[0] + b[2] / 2.0, b[1] + b[3] / 2.0);
    }

    static void scroll(MinecraftClient c, double v) {
        PanelSuite.invoke(c, "onMouseScroll", new Class<?>[]{long.class, double.class, double.class}, c.getWindow().getHandle(), 0.0, v);
    }

    /** The strip lies inside the HUD canvas, meets no HUD box drawn the same frame, and stays off the crosshair (16 GUI px around the centre). */
    void stripClear(MinecraftClient c, String size) {
        stripChecked++;
        int[] s = BattleHud.aimStrip;
        if (s == null) { stripFails.add(size + ": no strip"); return; }
        int cw = UiCanvas.hudWidth();
        if (s[0] < 0 || s[0] + s[2] > cw || s[1] < 0) stripFails.add(size + ": strip " + Arrays.toString(s) + " leaves the canvas " + cw + " wide");
        for (var e : BattleHud.rects().entrySet()) {
            int[] b = e.getValue();
            if (b == null || b.length < 4 || b[2] <= 0 || b[3] <= 0) continue;
            if (s[0] < b[0] + b[2] && b[0] < s[0] + s[2] && s[1] < b[1] + b[3] && b[1] < s[1] + s[3]) stripFails.add(size + ": strip " + Arrays.toString(s) + " meets " + e.getKey() + " " + Arrays.toString(b));
        }
        int[] g = BattleHud.toGui(s);
        double cx = c.getWindow().getScaledWidth() / 2.0, cy = c.getWindow().getScaledHeight() / 2.0;
        if (g[1] + g[3] > cy - 16) stripFails.add(size + ": strip bottom " + (g[1] + g[3]) + " GUI px reaches the crosshair at " + cy);
        int[] v = CommandPanel.vanillaHud(c.getWindow().getScaledWidth(), c.getWindow().getScaledHeight());
        if (g[0] < v[0] + v[2] && v[0] < g[0] + g[2] && g[1] < v[1] + v[3] && v[1] < g[1] + g[3]) stripFails.add(size + ": strip meets the vanilla hotbar");
    }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) {
        waits.clear();
        CommandSelection.reset();
    }
}
