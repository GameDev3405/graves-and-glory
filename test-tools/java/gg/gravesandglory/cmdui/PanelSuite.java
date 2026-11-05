package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandNet;
import gg.gravesandglory.CommandPanel;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import gg.gravesandglory.Spec;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Command UI plan Task 7: the pennant panel. Clicks go through Mouse.onCursorPos and Mouse.onMouseButton (the GLFW callbacks a real mouse
 * drives, so PanelInputMixin sees them as it would a player's); keys through CommandUiChecks.press. Screenshots of each state at 1920x1080
 * and 854x480 go to <run>/command-ui/shots for the visual review. Never included in releases.
 */
public final class PanelSuite implements CommandUiChecks.Suite {
    UUID id;
    Path shots;
    final List<String> shotLog = new ArrayList<>();
    final List<String> edgeFails = new ArrayList<>();
    int edgeChecked, overlapChecked;
    final List<String> overlapFails = new ArrayList<>();
    int openSeenAt = -1, openFullAt = -1, closeAt = -1, closeZeroAt = -1;
    Vec3d walkFrom;
    int[] initialSize;
    // troop pick
    List<Entity> candidates = List.of();
    int tryIndex, troopNext;
    String troopWant = "cavalry", troopDetail = "";
    boolean troopDone, troopPass;
    int sentBefore;
    // disconnect: watched from a tick listener of our own while the suite's ticks pause without a world
    static volatile PanelSuite live;
    static boolean listening;
    int disconnectStage, offlineTicks;
    String afterDisconnect = "";

    static KeyBinding find(MinecraftClient c, String translationKey) {
        for (KeyBinding k : c.options.allKeys) if (k.getTranslationKey().equals(translationKey)) return k;
        return null;
    }

    @Override public int limit() { return 2400; }

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) {
            id = c.player.getUuid(); live = this;
            shots = FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("shots");
            try { Files.createDirectories(shots); } catch (Exception ignored) { }
            listen();
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-panel-battle", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandUiChecks.server(c, b -> { OrderIntakeProbe.respawning(b, id, false); OrderIntakeProbe.resetCharge(b, id); });
            CommandSelection.reset();
            initialSize = new int[]{c.getWindow().getWidth(), c.getWindow().getHeight()};
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        }
        if (tick == 56) cursorCentre(c);
        if (tick == 60) shot(c, "panel-closed-1920.png");

        // slide in: R opens the panel; slide() reaches 1 within panelSlideTicks + 2 ticks of the press being seen (the next tick)
        if (tick == 62) CommandUiChecks.press(c, find(c, "key.gravesandglory.menu"));
        if (tick >= 63 && tick <= 90 && openFullAt < 0) { if (openSeenAt < 0 && CommandSelection.panelOpen()) openSeenAt = tick; if (openSeenAt >= 0 && CommandPanel.slide() >= 1) openFullAt = tick; }
        if (tick == 75) cursorCentre(c);
        if (tick == 78) {
            edges(c, "open-1920");
            float want = (float) Spec.COMMANDINTERFACE[0].pennantAlpha();
            StringBuilder d = new StringBuilder();
            boolean ok = Math.abs(want - 0.55f) <= 0.01f;
            int seen = 0;
            for (int i = 0; i < CommandPanel.ORDERS.length; i++) {
                Float a = CommandPanel.lastAlpha.get(CommandPanel.PENNANT + i);
                if (a == null) { ok = false; d.append(CommandPanel.ORDERS[i]).append("=none "); continue; }
                seen++;
                if (Math.abs(a - 0.55f) > 0.01f) ok = false;
                d.append(CommandPanel.ORDERS[i]).append('=').append(String.format(Locale.ROOT, "%.3f", a)).append(' ');
            }
            CommandUiChecks.report("cu-panel-alpha", ok && seen == 5, "Spec pennantAlpha " + want + "; idle pennants drawn at " + d.toString().trim());
            shot(c, "panel-open-1920.png"); shot(c, "panel-open.png");
        }
        if (tick == 80) cursorTo(c, CommandPanel.PENNANT); // hover Attack
        if (tick == 84) { edges(c, "hover-1920"); shot(c, "panel-hover-1920.png"); }

        // walking with the panel open
        if (tick == 86) { cursorCentre(c); walkFrom = c.player.getPos(); c.options.forwardKey.setPressed(true); }
        if (tick == 106) {
            c.options.forwardKey.setPressed(false);
            double moved = Math.sqrt(c.player.getPos().squaredDistanceTo(walkFrom.x, c.player.getY(), walkFrom.z));
            CommandUiChecks.report("cu-panel-walk", moved >= 1 && CommandSelection.panelOpen(), String.format(Locale.ROOT, "W held 20 ticks with the panel open: moved %.2f blocks; panel still open=%s; cursor locked=%s", moved, CommandSelection.panelOpen(), c.mouse.isCursorLocked()));
        }

        // pills
        if (tick == 110) { CommandSelection.setPanelOpen(true); CommandSelection.select("all", false); click(c, CommandPanel.PILL + 1, 0); }
        if (tick == 113) {
            pillFirst = CommandSelection.selected();
            click(c, CommandPanel.PILL, GLFW.GLFW_MOD_SHIFT);
        }
        if (tick == 116) {
            var both = new HashSet<>(CommandSelection.selected());
            CommandUiChecks.report("cu-panel-click-pill", pillFirst.equals(List.of("ranged")) && both.equals(Set.of("ranged", "infantry")),
                "Archers pill click -> " + pillFirst + "; shift-click Infantry -> " + CommandSelection.selected());
        }

        // a click on a troop of the army selects its group, with no swing
        if (tick == 117) spawnRider(c);
        if (tick == 120) troopStart(c);
        if (tick > 120 && tick < 178 && !troopDone && tick == troopNext) troopStep(c, tick);
        if (tick == 178) {
            if (!troopDone) troopDetail += " (ran out of time)";
            CommandUiChecks.report("cu-panel-click-troop", troopPass, troopDetail.trim());
        }

        // the Formation pennant opens the tray; key 2 picks Shield wall (block), which starts the Formation aim and folds the tray
        if (tick == 180) { CommandSelection.select("infantry", false); CommandSelection.setPanelOpen(true); CommandSelection.setTrayOpen(false); cursorCentre(c); }
        if (tick == 183) click(c, CommandPanel.PENNANT + 4, 0);
        if (tick == 187) {
            trayOpened = CommandSelection.trayOpen();
            edges(c, "tray-1920");
            shot(c, "panel-tray-1920.png"); shot(c, "panel-tray.png");
            CommandUiChecks.press(c, c.options.hotbarKeys[1]);
        }
        if (tick == 191) {
            String form = formation();
            CommandUiChecks.report("cu-panel-tray", trayOpened && "hold".equals(CommandSelection.aiming()) && "block".equals(form) && !CommandSelection.trayOpen(),
                "Formation click opened the tray=" + trayOpened + "; key 2: aiming '" + CommandSelection.aiming() + "' formation " + form + ", tray open=" + CommandSelection.trayOpen());
            CommandUiChecks.press(c, find(c, "key.gravesandglory.cancel"));
        }

        // slide: open fully, then an order without Shift slides it shut
        if (tick == 196) { CommandSelection.setPanelOpen(true); CommandSelection.select("infantry", false); }
        if (tick == 206) { slideBeforeClose = CommandPanel.slide(); CommandUiChecks.press(c, find(c, "key.gravesandglory.attack")); }
        if (tick >= 207 && tick <= 225 && closeZeroAt < 0) { if (closeAt < 0 && !CommandSelection.panelOpen()) closeAt = tick; if (closeAt >= 0 && CommandPanel.slide() <= 0) closeZeroAt = tick; }
        if (tick == 226) {
            int limitTicks = Spec.COMMANDINTERFACE[0].panelSlideTicks() + 2;
            boolean open = openSeenAt >= 0 && openFullAt >= 0 && openFullAt - openSeenAt <= limitTicks;
            boolean shut = slideBeforeClose >= 1 && closeAt >= 0 && closeZeroAt >= 0 && closeZeroAt - closeAt <= limitTicks;
            CommandUiChecks.report("cu-panel-slide", open && shut, "opened (seen tick " + openSeenAt + ") reached 1 at tick " + openFullAt + "; Attack without Shift closed it at tick " + closeAt
                + " (slide before " + slideBeforeClose + "), reached 0 at tick " + closeZeroAt + "; allowed " + limitTicks + " ticks each");
        }

        // a Charge: its pennant darkens with the sweep; X while cooling sends nothing
        if (tick == 228) { CommandUiChecks.server(c, b -> OrderIntakeProbe.resetCharge(b, id)); CommandSelection.select("infantry", false); CommandSelection.setPanelOpen(false); }
        if (tick == 231) CommandUiChecks.press(c, find(c, "key.gravesandglory.charge"));
        if (tick == 235) { chargeAimed = CommandSelection.aiming(); CommandUiChecks.press(c, find(c, "key.gravesandglory.confirm")); }
        if (tick == 246) { CommandSelection.setPanelOpen(true); cursorCentre(c); }
        if (tick == 256) {
            cooling = CommandNet.Client.chargeTicksLeft();
            sweepDrawn = drawn(CommandPanel.SWEEP);
            edges(c, "cooldown-1920");
            shot(c, "panel-cooldown-1920.png"); shot(c, "panel-cooldown.png");
            sentBefore = OrderIntakeProbe.ordersSent();
            CommandUiChecks.press(c, find(c, "key.gravesandglory.charge"));
        }
        if (tick == 262) {
            int sent = OrderIntakeProbe.ordersSent() - sentBefore;
            CommandUiChecks.report("cu-panel-cooldown", "charge".equals(chargeAimed) && cooling > 0 && sweepDrawn && sent == 0 && CommandSelection.aiming().isEmpty(),
                "X aimed '" + chargeAimed + "' and Enter charged; cooldown ticks left " + cooling + "; Charge pennant sweep drawn=" + sweepDrawn + "; X while cooling sent " + sent + " orders, aiming '" + CommandSelection.aiming() + "'");
        }

        // the same states at 854x480 (GUI Scale 3 falls to 2 there)
        if (tick == 264) { CommandSelection.setPanelOpen(false); GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 854, 480); }
        if (tick == 278) cursorCentre(c);
        if (tick == 280) shot(c, "panel-closed-854.png");
        if (tick == 282) CommandSelection.setPanelOpen(true);
        if (tick == 292) { edges(c, "open-854"); shot(c, "panel-open-854.png"); }
        if (tick == 294) cursorTo(c, CommandPanel.PENNANT);
        if (tick == 298) { edges(c, "hover-854"); shot(c, "panel-hover-854.png"); cursorCentre(c); }
        if (tick == 300) CommandSelection.order("formation", false);
        if (tick == 304) { edges(c, "tray-854"); shot(c, "panel-tray-854.png"); CommandSelection.setTrayOpen(false); }
        if (tick == 308) {
            boolean sweep854 = drawn(CommandPanel.SWEEP);
            edges(c, "cooldown-854"); shot(c, "panel-cooldown-854.png");
            CommandUiChecks.report("cu-panel-left-edge", edgeFails.isEmpty() && edgeChecked >= 8,
                edgeChecked + " frames checked (1920x1080 and 854x480: open, hover, tray, cooldown); " + (edgeFails.isEmpty() ? "every rect inside 30% (tray inside 45%)" : String.join("; ", edgeFails))
                    + "; 854 cooldown sweep drawn=" + sweep854 + "; shots: " + String.join(", ", shotLog));
            CommandUiChecks.report("cu-panel-overlap", overlapFails.isEmpty() && overlapChecked >= 8,
                overlapChecked + " frames (1920x1080 and 854x480): " + (overlapFails.isEmpty() ? "no panel rect meets the hint slip, a group card or the spine; the hint slip hidden while open" : String.join("; ", overlapFails.subList(0, Math.min(6, overlapFails.size())))));
        }
        if (tick == 310) { GLFW.glfwSetWindowSize(c.getWindow().getHandle(), initialSize[0], initialSize[1]); CommandSelection.setPanelOpen(true); CommandSelection.select("ranged", false); }

        // the cursor comes back: panel open, the pause screen opened and closed, the panel shut -> the cursor is locked again
        if (tick == 314) { relockFreeBefore = !c.mouse.isCursorLocked(); c.setScreen(new net.minecraft.client.gui.screen.GameMenuScreen(true)); }
        if (tick == 318) c.setScreen(null);
        if (tick == 322) { relockFreeAfterResume = !c.mouse.isCursorLocked(); CommandSelection.setPanelOpen(false); }
        if (tick == 326) {
            boolean locked = c.mouse.isCursorLocked();
            CommandUiChecks.report("cu-panel-relock", relockFreeBefore && relockFreeAfterResume && locked,
                "panel open: cursor free=" + relockFreeBefore + "; after Esc and resume: free=" + relockFreeAfterResume + "; panel shut: locked=" + locked + " (window focused=" + c.isWindowFocused() + ")");
            CommandSelection.setPanelOpen(true); CommandSelection.select("ranged", false);
        }

        // disconnect with the panel open; the listener records the state without a world and rejoins
        if (tick == 330) {
            beforeDisconnect = "panel open=" + CommandSelection.panelOpen() + " selection " + CommandSelection.selected();
            disconnectStage = 1;
            c.world.disconnect();
            c.disconnect(new gg.gravesandglory.FrontMenuScreen());
        }
        if (tick >= 331 && disconnectStage == 3 && rejoinTick < 0) rejoinTick = tick;
        if (rejoinTick > 0 && tick == rejoinTick + 20) {
            boolean ok = afterDisconnect.contains("panel open=false") && !CommandSelection.panelOpen() && CommandSelection.selected().equals(List.of("all")) && CommandPanel.slide() <= 0;
            CommandUiChecks.report("cu-panel-disconnect", ok, "before: " + beforeDisconnect + "; without a world: " + afterDisconnect + "; after rejoin: panel open=" + CommandSelection.panelOpen()
                + " selection " + CommandSelection.selected() + " slide " + CommandPanel.slide());
            CommandUiChecks.finish(c);
        }
    }

    List<String> pillFirst = List.of();
    boolean trayOpened, sweepDrawn;
    String chargeAimed = "", beforeDisconnect = "";
    int cooling, rejoinTick = -1;
    boolean relockFreeBefore, relockFreeAfterResume;
    double slideBeforeClose;

    /** A tick listener of the suite's own: the suite's ticks stop while there is no world, so this one watches the disconnect and rejoins. */
    static void listen() {
        if (listening) return;
        listening = true;
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            PanelSuite s = live;
            if (s == null || s.disconnectStage == 0 || s.disconnectStage == 3) return;
            if (s.disconnectStage == 1 && c.world == null && c.player == null) {
                if (++s.offlineTicks < 10) return;
                s.afterDisconnect = "panel open=" + CommandSelection.panelOpen() + " selection " + CommandSelection.selected() + " slide " + CommandPanel.slide();
                s.disconnectStage = 2;
                try {
                    Method m = GravesAndGloryClient.class.getDeclaredMethod("startCamp", MinecraftClient.class, boolean.class);
                    m.setAccessible(true);
                    m.invoke(null, c, false);
                } catch (Exception e) { CommandUiChecks.report("cu-panel-disconnect", false, "rejoin failed: " + e); s.disconnectStage = 3; c.scheduleStop(); }
            } else if (s.disconnectStage == 2 && c.player != null && c.world != null) s.disconnectStage = 3;
        });
    }

    /** Every rect the panel drew this frame lies inside 30% of the screen's width; tray rects inside 45%. */
    void edges(MinecraftClient c, String state) {
        int sw = c.getWindow().getScaledWidth();
        edgeChecked++;
        if (CommandPanel.lastDrawn.isEmpty()) { edgeFails.add(state + ": nothing drawn"); return; }
        for (int[] r : CommandPanel.lastDrawn) {
            boolean tray = r[0] >= CommandPanel.TRAY && r[0] < CommandPanel.SWEEP;
            double limit = (tray ? 0.45 : 0.30) * sw;
            if (r[1] + r[3] > limit + 1e-9) edgeFails.add(state + ": id " + r[0] + " right " + (r[1] + r[3]) + " > " + limit);
        }
        // no panel rect (the cooldown sweep aside) may cover a HUD box drawn the same frame: the hint slip, a group card or chip, the spine
        overlapChecked++;
        double k = gg.gravesandglory.UiCanvas.hudK() / c.getWindow().getScaleFactor();
        var hud = gg.gravesandglory.BattleHud.rects();
        if (hud.containsKey("hints")) overlapFails.add(state + ": hint slip drawn while the panel is open");
        for (var e : hud.entrySet()) {
            String name = e.getKey(); int[] b = e.getValue();
            if (b == null || b.length < 4 || !(name.equals("hints") || name.startsWith("card-") || name.equals("spine"))) continue;
            double bx = b[0] * k, by = b[1] * k, bw = b[2] * k, bh = b[3] * k;
            for (int[] r : CommandPanel.lastDrawn) {
                if (r[0] == CommandPanel.SWEEP) continue;
                if (r[1] < bx + bw && bx < r[1] + r[3] && r[2] < by + bh && by < r[2] + r[4])
                    overlapFails.add(state + ": panel id " + r[0] + " [" + r[1] + "," + r[2] + " " + r[3] + "x" + r[4] + "] meets " + name
                        + String.format(Locale.ROOT, " [%.0f,%.0f %.0fx%.0f]", bx, by, bw, bh));
            }
        }
    }

    static boolean drawn(int id) { for (int[] r : CommandPanel.lastDrawn) if (r[0] == id) return true; return false; }
    static int[] rect(int id) { for (int[] r : CommandPanel.lastDrawn) if (r[0] == id) return r; return null; }

    static String formation() {
        try { var f = Class.forName("gg.gravesandglory.CommandMode").getDeclaredField("formation"); f.setAccessible(true); return (String) f.get(null); }
        catch (Exception e) { return "?" + e; }
    }

    // --- the mouse, through the GLFW callbacks ---
    static void cursorGui(MinecraftClient c, double gx, double gy) {
        var w = c.getWindow();
        invoke(c, "onCursorPos", new Class<?>[]{long.class, double.class, double.class}, w.getHandle(), gx * w.getWidth() / w.getScaledWidth(), gy * w.getHeight() / w.getScaledHeight());
    }
    static void cursorCentre(MinecraftClient c) { var w = c.getWindow(); cursorGui(c, w.getScaledWidth() / 2.0, w.getScaledHeight() / 2.0); }
    static void cursorTo(MinecraftClient c, int id) { int[] r = rect(id); if (r != null) cursorGui(c, r[1] + r[3] / 2.0, r[2] + r[4] / 2.0); }
    static void clickGui(MinecraftClient c, double gx, double gy, int mods) {
        cursorGui(c, gx, gy);
        long h = c.getWindow().getHandle();
        invoke(c, "onMouseButton", new Class<?>[]{long.class, int.class, int.class, int.class}, h, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, mods);
        invoke(c, "onMouseButton", new Class<?>[]{long.class, int.class, int.class, int.class}, h, GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_RELEASE, mods);
    }
    static void click(MinecraftClient c, int id, int mods) {
        int[] r = rect(id);
        if (r == null) { CommandUiChecks.report("cu-panel-rect-" + id, false, "nothing drawn with id " + id); return; }
        clickGui(c, r[1] + r[3] / 2.0, r[2] + r[4] / 2.0, mods);
    }
    static void invoke(MinecraftClient c, String name, Class<?>[] types, Object... args) {
        try { Method m = Mouse.class.getDeclaredMethod(name, types); m.setAccessible(true); m.invoke(c.mouse, args); }
        catch (Exception e) { throw new IllegalStateException("Mouse." + name + ": " + e, e); }
    }

    // --- the troop pick ---
    /** The Gilded House fields no riders, so one Moot Hound (a cavalry row) joins the player's squad 5 blocks ahead, through Battle.spawn as a round spawns its troops. */
    void spawnRider(MinecraftClient c) {
        Vec3d look = Vec3d.fromPolar(0, c.player.getYaw()), at = c.player.getPos().add(look.multiply(5));
        CommandUiChecks.server(c, b -> {
            try {
                Spec.TroopsRow hound = null;
                for (var t : Spec.TROOPS) if (t.id().equals("wm_hound")) hound = t;
                Object squad = b.squads.get(id);
                Method m = null;
                for (Method k : b.getClass().getDeclaredMethods()) if (k.getName().equals("spawn") && k.getParameterCount() == 4) m = k;
                m.setAccessible(true);
                m.invoke(b, squad, hound, 90, at);
            } catch (Exception e) { troopDetail = "could not spawn a rider: " + e + ";"; }
        });
    }
    void troopStart(MinecraftClient c) {
        CommandSelection.setPanelOpen(true);
        List<Entity> own = new ArrayList<>(), want = new ArrayList<>();
        Set<String> groups = new TreeSet<>();
        for (Entity e : c.world.getEntities()) { String g = CommandPanel.troopGroup(e); if (g == null) continue; own.add(e); groups.add(g); if (g.equals("cavalry")) want.add(e); }
        if (want.isEmpty() && !own.isEmpty()) { troopWant = CommandPanel.troopGroup(own.get(0)); want = own.stream().filter(e -> troopWant.equals(CommandPanel.troopGroup(e))).collect(java.util.stream.Collectors.toList()); troopDetail = "no cavalry fielded (own groups " + groups + "), used " + troopWant + ";"; }
        want.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(c.player)));
        candidates = want.subList(0, Math.min(6, want.size()));
        if (candidates.isEmpty()) { troopDone = true; troopDetail = "no own troops visible to the client"; return; }
        tryIndex = 0; look(c, candidates.get(0)); troopNext = 124;
    }
    int troopPhase;
    void troopStep(MinecraftClient c, int tick) {
        Entity e = candidates.get(tryIndex);
        if (troopPhase == 0) {
            CommandSelection.select("all", false); CommandSelection.setPanelOpen(true);
            double[] at = CommandPanel.project(e.getBoundingBox().getCenter());
            if (at == null) { troopDetail += " candidate " + tryIndex + " behind the camera;"; next(c, tick); return; }
            sentBefore = OrderIntakeProbe.ordersSent();
            clickGui(c, at[0], at[1], 0);
            troopDetail += String.format(Locale.ROOT, " candidate %d (%s, %.1f blocks) clicked at gui (%.0f, %.0f);", tryIndex, e.getCustomName() == null ? "?" : e.getCustomName().getString(), Math.sqrt(e.squaredDistanceTo(c.player)), at[0], at[1]);
            troopPhase = 1; troopNext = tick + 2;
        } else {
            boolean swung = c.player.handSwinging || c.options.attackKey.isPressed();
            int sent = OrderIntakeProbe.ordersSent() - sentBefore;
            var sel = CommandSelection.selected();
            troopDetail += " -> selection " + sel + ", swing=" + swung + ", orders sent " + sent + ";";
            if (sel.equals(List.of(troopWant)) && !swung && sent == 0) { troopPass = troopWant.equals("cavalry"); troopDone = true; return; }
            troopPhase = 0; next(c, tick);
        }
    }
    void next(MinecraftClient c, int tick) {
        if (++tryIndex >= candidates.size()) { troopDone = true; return; }
        look(c, candidates.get(tryIndex)); troopNext = tick + 4;
    }
    static void look(MinecraftClient c, Entity e) {
        Vec3d eye = c.player.getEyePos(), to = e.getBoundingBox().getCenter().subtract(eye);
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90), pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        c.player.setYaw(yaw); c.player.setPitch(pitch); c.player.prevYaw = yaw; c.player.prevPitch = pitch;
        c.player.setHeadYaw(yaw); c.player.prevHeadYaw = yaw;
    }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) {
        c.options.forwardKey.setPressed(false);
        CommandSelection.reset();
        live = null;
    }
}
