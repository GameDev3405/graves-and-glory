package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** p4-map-view and p4-map-plan (battle map Task 6; a Phase4Checks suite, run as GRAVES_PHASE4_CHECKS=map-screen), on map 0 (Rivergate, an
 *  old map) and then New Kingsfield (an engine map), each a skirmish with bot fill.
 *  - view: BattleMapScreen opens at the fit scale; a scroll of +3 at the map's centre zooms and keeps the centre's world point; a 50 px
 *    drag moves the view; T switches the look and the texture drawn; Home fits again; with no grid the screen still draws (fallback).
 *  - plan: a click on point B opens its popup; Attack puts ATTACK on B in battlemap_v1 and a bot says "on it, taking"; a click on an ally
 *    bot's plaque opens its popup; Hold here puts a direct HOLD for that army in battlemap_v1.
 *  Screenshots (screenshots/p4-ui-map-*-(map)[-1080|-small].png): fit, zoom, real, plan at 960x540 GUI 2, fit and plan at 1920x1080 GUI 3,
 *  fit at 854x480 GUI 2. */
public final class MapScreenChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    static final int[] MAPS = {0, 5};
    volatile UUID player;
    volatile int at;
    volatile boolean started, finished;
    int stage, stageTick, startTick = -1;
    String id = "";
    BattleMapScreen screen;
    // the view check's findings
    boolean fitOpen, zoomUp, zoomKept, dragMoved, toggled, homeFit, noGridOk;
    String viewWhy = "";
    // the plan check's
    boolean pointPopup, planInState, ackHeard, armyPopup, orderInState;
    String planWhy = "", ally = "", setter = "";
    final List<String> heard = new ArrayList<>();
    int winW, winH, winGui;

    public int limit() { return 20000; } // New Kingsfield is built from scratch after the switch

    void next(int s, int tick) { stage = s; stageTick = tick; }

    public void step(MinecraftClient c, int tick) {
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); winW = c.getWindow().getWidth(); winH = c.getWindow().getHeight(); winGui = c.options.getGuiScale().getValue(); }
        if (finished) return;
        if (at >= MAPS.length) { finished = true; Phase4Checks.finish(c, "map-screen"); return; }
        if (!started) { startTick = -1; return; }
        if (startTick < 0) { startTick = tick; id = Spec.MAPS[MAPS[at]].id(); next(1, tick); resetFindings(); }
        for (var l : EventFeed.lines(net.minecraft.util.Util.getMeasuringTimeMs())) if (!heard.contains(l.text())) heard.add(l.text());
        int waited = tick - stageTick;
        switch (stage) {
            case 1 -> { // the grid, both looks and a state packet, or 400 ticks
                boolean ready = BattleMapClient.grid != null && id.equals(BattleMapArt.builtMap()) && BattleMapClient.state != null && !CommandMode.groups.isEmpty();
                if (!ready && waited < 400) return;
                if (!ready) viewWhy += "not ready after 400 ticks (grid " + (BattleMapClient.grid != null) + ", built " + BattleMapArt.builtMap() + ", state " + (BattleMapClient.state != null) + "); ";
                Phase1Checks.window(c, 960, 540, 2);
                BattleMapScreen.setLook(true);
                next(2, tick);
            }
            case 2 -> { if (waited >= 6) { screen = new BattleMapScreen(null); c.setScreen(screen); next(3, tick); } }
            case 3 -> {
                if (waited < 5) return;
                var v = screen.view;
                fitOpen = c.currentScreen == screen && Math.abs(v.scale() - v.fitScale()) < 1e-9;
                viewWhy += "opened at scale " + r(v.scale()) + " (fit " + r(v.fitScale()) + "); ";
                Phase4Checks.shot(c, "ui-map-fit-" + id);
                double cx = screen.ax + screen.aw / 2.0, cy = screen.ay + screen.ah / 2.0, wx = v.worldX(cx), wz = v.worldZ(cy), before = v.scale();
                screen.mouseScrolled(cx, cy, 3);
                zoomUp = v.scale() > before * 1.9;
                zoomKept = Math.abs(v.worldX(cx) - wx) < 1e-6 && Math.abs(v.worldZ(cy) - wz) < 1e-6;
                viewWhy += "scroll +3 at the centre: scale " + r(before) + " -> " + r(v.scale()) + ", centre world point moved " + r(Math.hypot(v.worldX(cx) - wx, v.worldZ(cy) - wz)) + " blocks; ";
                next(4, tick);
            }
            case 4 -> {
                if (waited < 4) return;
                Phase4Checks.shot(c, "ui-map-zoom-" + id);
                var v = screen.view;
                double cx = screen.ax + screen.aw / 2.0, cy = screen.ay + screen.ah / 2.0, wx = v.worldX(cx);
                screen.mouseClicked(cx, cy, 0);
                screen.mouseDragged(cx + 20, cy, 0, 20, 0);
                screen.mouseDragged(cx + 50, cy, 0, 30, 0);
                screen.mouseReleased(cx + 50, cy, 0);
                dragMoved = Math.abs(v.worldX(cx) - wx) > 1 && screen.popup == null;
                viewWhy += "a 50 px drag moved the centre " + r(wx - v.worldX(cx)) + " blocks (no popup: " + (screen.popup == null) + "); ";
                screen.keyPressed(GLFW.GLFW_KEY_T, 0, 0);
                next(5, tick);
            }
            case 5 -> {
                if (waited < 4) return;
                toggled = !BattleMapScreen.lookParchment() && "real".equals(screen.drawnTexture) && screen.drawnId != null && screen.drawnId.equals(BattleMapArt.real());
                viewWhy += "T: look parchment=" + BattleMapScreen.lookParchment() + ", drawn " + screen.drawnTexture + "; ";
                screen.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
                Phase4Checks.shot(c, "ui-map-real-" + id);
                next(6, tick);
            }
            case 6 -> {
                if (waited < 3) return;
                var v = screen.view;
                homeFit = Math.abs(v.scale() - v.fitScale()) < 1e-9 && !v.moved();
                screen.keyPressed(GLFW.GLFW_KEY_T, 0, 0); // back to parchment
                saved = BattleMapClient.grid;
                BattleMapClient.grid = null; // a late joiner: the screen draws the fallback
                next(7, tick);
            }
            case 7 -> {
                if (waited < 3) return;
                noGridOk = c.currentScreen == screen && ("fallback".equals(screen.drawnTexture) || "plain".equals(screen.drawnTexture)) && screen.pointAt(0) != null;
                viewWhy += "Home: scale " + r(screen.view.scale()) + " (fit " + r(screen.view.fitScale()) + "); with no grid drew " + screen.drawnTexture + " and the points; ";
                BattleMapClient.grid = saved;
                Phase4Checks.report("p4-map-view-" + id, fitOpen && zoomUp && zoomKept && dragMoved && toggled && homeFit && noGridOk, viewWhy);
                next(10, tick);
            }
            // ---- plan
            case 10 -> {
                if (waited < 3) return;
                int[] p = screen.pointAt(1);
                if (p == null) { planWhy += "point B not drawn; "; next(19, tick); return; }
                screen.clickAt(p[0], p[1]);
                pointPopup = screen.popup != null && screen.popup.kind() == 0 && screen.popup.point() == 1;
                planWhy += "click on B at " + p[0] + "," + p[1] + ": popup " + (screen.popup == null ? "none" : screen.popup.title() + " " + screen.popup.rows()) + "; ";
                if (!CampaignClient.showsPoints()) { planWhy += "this mode plans no points (" + CampaignClient.modeId() + "); "; pointPopup = pointPopup && screen.popup.rows().equals(List.of("Ping here")); screen.popup = null; next(13, tick); return; }
                int[] row = screen.popupRow("Attack");
                if (row != null) screen.clickAt(row[0], row[1]);
                next(11, tick);
            }
            case 11 -> { // the plan in battlemap_v1
                var s = BattleMapClient.state;
                if (s != null) for (var q : s.points()) if (q.point() == 1 && q.job() == BattlePlan.Job.ATTACK.ordinal()) { planInState = true; setter = q.setter(); }
                if (planInState || waited > 60) { planWhy += "ATTACK on B in the state after " + waited + " ticks: " + planInState + " (set by " + setter + "); "; next(12, tick); }
            }
            case 12 -> { // a bot's line
                ackHeard = heard.stream().anyMatch(h -> h.contains("on it, taking"));
                if (ackHeard || waited > 240) { planWhy += "bot line 'on it, taking' heard: " + ackHeard + " (" + heard.stream().filter(h -> h.contains("bot")).limit(4).toList() + "); "; next(13, tick); }
            }
            case 13 -> { // an ally bot's plaque, zoomed onto it so no point disc covers it
                var s = BattleMapClient.state;
                BattleMapClient.Army pick = null;
                if (s != null) for (var a : s.armies()) if (a.bot() && !a.aimed() && !a.groups().isEmpty() && !a.retreating()) { pick = a; break; }
                if (pick == null) { if (waited > 100) { planWhy += "no free ally bot in the state; "; next(19, tick); } return; }
                ally = pick.id();
                double[] cxz = BattleMapScreen.centre(pick);
                screen.view.fit();
                screen.view.zoomAt(screen.view.screenX(cxz[0]), screen.view.screenY(cxz[1]), 4);
                next(14, tick);
            }
            case 14 -> {
                if (waited < 3) return;
                int[] a = screen.armyAt(ally);
                if (a == null) { planWhy += "the ally plaque was not drawn; "; next(19, tick); return; }
                screen.clickAt(a[0], a[1]);
                armyPopup = screen.popup != null && screen.popup.kind() == 1 && ally.equals(screen.popup.army());
                planWhy += "click on the plaque: popup " + (screen.popup == null ? "none" : screen.popup.title() + " " + screen.popup.rows()) + "; ";
                int[] row = screen.popupRow("Hold here");
                if (row != null) screen.clickAt(row[0], row[1]);
                next(15, tick);
            }
            case 15 -> {
                var s = BattleMapClient.state;
                if (s != null) for (var o : s.orders()) if (o.army().equals(ally) && o.job() == BattlePlan.Job.HOLD.ordinal()) orderInState = true;
                if (orderInState || waited > 60) {
                    planWhy += "direct HOLD in the state after " + waited + " ticks: " + orderInState + "; ";
                    screen.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
                    next(16, tick);
                }
            }
            case 16 -> { // the plan shot: fit, with B's popup open over its flag
                if (waited < 3) return;
                int[] p = screen.pointAt(1);
                if (p != null) screen.clickAt(p[0], p[1]);
                next(17, tick);
            }
            case 17 -> { if (waited >= 3) { Phase4Checks.shot(c, "ui-map-plan-" + id); next(19, tick); } }
            case 19 -> {
                boolean pointsPlanned = CampaignClient.showsPoints();
                Phase4Checks.report("p4-map-plan-" + id, pointPopup && (!pointsPlanned || planInState && ackHeard) && armyPopup && orderInState, planWhy);
                screen.popup = null;
                Phase1Checks.window(c, 1920, 1080, 3, true);
                next(20, tick);
            }
            // ---- the big window and the smallest one
            case 20 -> { if (waited >= 8) { Phase4Checks.shot(c, "ui-map-fit-" + id + "-1080"); int[] p = screen.pointAt(1); if (p != null) screen.clickAt(p[0], p[1]); next(21, tick); } }
            case 21 -> { if (waited >= 3) { Phase4Checks.shot(c, "ui-map-plan-" + id + "-1080"); screen.popup = null; Phase1Checks.window(c, 854, 480, 2); next(22, tick); } }
            case 22 -> {
                if (waited < 8) return;
                Phase4Checks.shot(c, "ui-map-fit-" + id + "-small");
                boolean inside = true;
                for (var w : screen.children()) if (w instanceof net.minecraft.client.gui.widget.ClickableWidget b && (b.getX() < 0 || b.getX() + b.getWidth() > screen.width || b.getY() + b.getHeight() > screen.height)) inside = false;
                Phase4Checks.report("p4-map-small-" + id, inside && screen.ah >= 120, "at 854x480 GUI 2 the screen is " + screen.width + "x" + screen.height + ", the map area " + screen.aw + "x" + screen.ah + ", the bar buttons inside: " + inside);
                c.setScreen(null);
                Phase1Checks.window(c, 960, 540, 2);
                at++;
                started = false;
            }
            default -> {}
        }
    }

    BattleMapGrid saved;

    void resetFindings() {
        fitOpen = zoomUp = zoomKept = dragMoved = toggled = homeFit = noGridOk = false;
        pointPopup = planInState = ackHeard = armyPopup = orderInState = false;
        viewWhy = ""; planWhy = ""; ally = ""; setter = ""; heard.clear();
    }

    static String r(double d) { return String.format(java.util.Locale.ROOT, "%.3f", d); }

    public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || started || finished || at >= MAPS.length) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        if (Phase1Checks.skirmish(b, p, MAPS[at], 3, "normal")) {
            System.out.println("[p4] map-screen: round started on map " + MAPS[at] + " (" + Spec.MAPS[MAPS[at]].id() + ", mode " + b.mode.id() + ")");
            started = true;
        }
    }

    public void cleanup(MinecraftClient c) {
        if (saved != null && BattleMapClient.grid == null) BattleMapClient.grid = saved;
        if (c.currentScreen instanceof BattleMapScreen) c.setScreen(null);
        if (!finished) Phase4Checks.report("p4-map-screen-incomplete", false, "the suite ended on map " + id + " in stage " + stage + "; view: " + viewWhy + " plan: " + planWhy);
    }
}
