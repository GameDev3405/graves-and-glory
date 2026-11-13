package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** p4-map-view and p4-map-plan (battle map Task 6; a Phase4Checks suite, run as GRAVES_PHASE4_CHECKS=map-screen), on map 0 (Rivergate, an
 *  old map) and then New Kingsfield (an engine map), each a skirmish with bot fill.
 *  - view: BattleMapScreen opens at the fit scale and draws the real texture (the only look since Revision 2); a scroll of +3 at the map's
 *    centre zooms and keeps the centre's world point; a 50 px drag moves the view; Home fits again; with no grid the screen still draws
 *    (fallback). A point's hit target is its disc (the centre hits, the box corner does not).
 *  - plan: a click on point B's disc opens its popup; Attack puts ATTACK on B in battlemap_v1 and a bot says "on it, taking"; a click on an
 *    ally bot's square icon (13 px, not a plaque) opens its popup; Hold here puts a direct HOLD for that army in battlemap_v1. With the screen closed and the player
 *    moved 100 blocks from point B and facing it, PlanMarkers drew point B's own world flag (Task 7), and the popup methods
 *    refuse what the server would: no order popup for an aimed bot, only "Ping here" on a point in a mode with no capture points. A plan
 *    only counts when it is in a state sent after the click.
 *  Screenshots (screenshots/p4-ui-map-*-(map)[-1080|-small].png): fit, zoom, plan at 960x540 GUI 2 and at 1920x1080 GUI 3,
 *  fit at 854x480 GUI 2; the world flag from the player's view toward B (p4-ui-plan-flag-(map)). */
public final class MapScreenChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    static final int[] MAPS = {0, 5};
    volatile UUID player;
    volatile int at;
    volatile boolean started, finished;
    int stage, stageTick, startTick = -1;
    String id = "";
    BattleMapScreen screen;
    // the view check's findings
    boolean fitOpen, realDrawn, discHit, zoomUp, zoomKept, dragMoved, homeFit, noGridOk;
    String viewWhy = "";
    // the plan check's
    boolean pointPopup, planInState, ackHeard, armyPopup, orderInState, flagDrawn, aimedNoPopup, noPointsOk;
    long planClickTime = -1, orderClickTime = -1; // the state's time when each click went out: only a later state can hold its effect
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
                boolean ready = BattleMapClient.grid != null && id.equals(BattleMapArt.builtMap()) && BattleMapClient.current() != null && !CommandMode.groups.isEmpty();
                if (!ready && waited < 400) return;
                if (!ready) viewWhy += "not ready after 400 ticks (grid " + (BattleMapClient.grid != null) + ", built " + BattleMapArt.builtMap() + ", state " + (BattleMapClient.state != null) + "); ";
                Phase1Checks.window(c, 960, 540, 2);
                next(2, tick);
            }
            case 2 -> { if (waited >= 6) { screen = new BattleMapScreen(null); c.setScreen(screen); next(3, tick); } }
            case 3 -> {
                if (waited < 5) return;
                var v = screen.view;
                fitOpen = c.currentScreen == screen && Math.abs(v.scale() - v.fitScale()) < 1e-9;
                viewWhy += "opened at scale " + r(v.scale()) + " (fit " + r(v.fitScale()) + "); ";
                realDrawn = "real".equals(screen.drawnTexture) && screen.drawnId != null && screen.drawnId.equals(BattleMapArt.real());
                viewWhy += "drew " + screen.drawnTexture + " (the real texture: " + realDrawn + "); ";
                var b = screen.pointHit(1);
                discHit = b != null && b.x1() - b.x0() == 2 * BattleMapScreen.DISC + 3 && b.has(b.cx(), b.cy()) && !b.has(b.x0(), b.y0()) && !b.has(b.x1() - 1, b.y1() - 1);
                viewWhy += "B's hit target " + (b == null ? "none" : (b.x1() - b.x0()) + " px, a disc: " + discHit) + "; ";
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
                next(5, tick);
            }
            case 5 -> {
                if (waited < 4) return;
                realDrawn &= "real".equals(screen.drawnTexture); // still the real look after a zoom and a pan
                screen.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0);
                next(6, tick);
            }
            case 6 -> {
                if (waited < 3) return;
                var v = screen.view;
                homeFit = Math.abs(v.scale() - v.fitScale()) < 1e-9 && !v.moved();
                saved = BattleMapClient.grid;
                BattleMapClient.grid = null; // a late joiner: the screen draws the fallback
                next(7, tick);
            }
            case 7 -> {
                if (waited < 3) return;
                noGridOk = c.currentScreen == screen && ("fallback".equals(screen.drawnTexture) || "plain".equals(screen.drawnTexture)) && screen.pointAt(0) != null;
                viewWhy += "Home: scale " + r(screen.view.scale()) + " (fit " + r(screen.view.fitScale()) + "); with no grid drew " + screen.drawnTexture + " and the points; ";
                BattleMapClient.grid = saved;
                Phase4Checks.report("p4-map-view-" + id, fitOpen && realDrawn && discHit && zoomUp && zoomKept && dragMoved && homeFit && noGridOk, viewWhy);
                next(10, tick);
            }
            // ---- plan
            case 10 -> {
                if (waited < 3) return;
                int[] p = screen.pointAt(1);
                if (p == null) { planWhy += "point B not drawn; "; next(19, tick); return; }
                planClickTime = stateTime();
                screen.clickAt(p[0], p[1]);
                pointPopup = screen.popup != null && screen.popup.kind() == 0 && screen.popup.point() == 1;
                planWhy += "click on B at " + p[0] + "," + p[1] + ": popup " + (screen.popup == null ? "none" : screen.popup.title() + " " + screen.popup.rows()) + "; ";
                if (!CampaignClient.showsPoints()) { planWhy += "this mode plans no points (" + CampaignClient.modeId() + "); "; pointPopup = pointPopup && screen.popup.rows().equals(List.of("Ping here")); screen.popup = null; next(13, tick); return; }
                int[] row = screen.popupRow("Attack");
                if (row != null) screen.clickAt(row[0], row[1]);
                next(11, tick);
            }
            case 11 -> { // the plan in battlemap_v1
                var s = BattleMapClient.current();
                if (s != null && s.time() > planClickTime) for (var q : s.points()) if (q.point() == 1 && q.job() == BattlePlan.Job.ATTACK.ordinal()) { planInState = true; setter = q.setter(); } // a state sent after the click
                if (planInState || waited > 60) { planWhy += "ATTACK on B in the state after " + waited + " ticks: " + planInState + " (set by " + setter + "); "; next(12, tick); }
            }
            case 12 -> { // a bot's line
                ackHeard = heard.stream().anyMatch(h -> h.contains("on it, taking"));
                if (ackHeard || waited > 240) { planWhy += "bot line 'on it, taking' heard: " + ackHeard + " (" + heard.stream().filter(h -> h.contains("bot")).limit(4).toList() + "); "; next(13, tick); }
            }
            case 13 -> { // an ally bot's icon, zoomed onto it so no point disc covers it
                var s = BattleMapClient.current();
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
                var ah = screen.armyHit(ally);
                if (ah == null) { planWhy += "the ally icon was not drawn; "; next(19, tick); return; }
                boolean square = ah.x1() - ah.x0() == 2 * BattleMapScreen.ICON + 1 && ah.y1() - ah.y0() == 2 * BattleMapScreen.ICON + 1;
                screen.clickAt(ah.cx(), ah.cy());
                armyPopup = square && screen.popup != null && screen.popup.kind() == 1 && ally.equals(screen.popup.army());
                planWhy += "click on the " + (ah.x1() - ah.x0()) + "x" + (ah.y1() - ah.y0()) + " ally icon (square: " + square + "): popup " + (screen.popup == null ? "none" : screen.popup.title() + " " + screen.popup.rows()) + "; ";
                int[] row = screen.popupRow("Hold here");
                orderClickTime = stateTime();
                if (row != null) screen.clickAt(row[0], row[1]);
                next(15, tick);
            }
            case 15 -> {
                var s = BattleMapClient.current();
                if (s != null && s.time() > orderClickTime) for (var o : s.orders()) if (o.army().equals(ally) && o.job() == BattlePlan.Job.HOLD.ordinal()) orderInState = true;
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
            case 17 -> { if (waited >= 3) { Phase4Checks.shot(c, "ui-map-plan-" + id); screen.popup = null; c.setScreen(null); next(18, tick); } }
            case 18 -> { // the world flag: the screen closed, the player 100 blocks from B and facing its flag
                if (waited < 2) return;
                var b = Spec.POINTS[1];
                double fx = b.x() + .5, fz = b.z() + .5, fy = PlanMarkers.ground(b, fx, fz) + PlanMarkers.POINT_LIFT + 2;
                if (waited == 2) {
                    double ox = c.player.getX() - fx, oz = c.player.getZ() - fz, ol = Math.max(1e-3, Math.hypot(ox, oz));
                    double tx = fx + ox / ol * 100, tz = fz + oz / ol * 100;
                    UUID who = player;
                    Phase4Checks.server(c, battle -> { var p = battle.server.getPlayerManager().getPlayer(who); if (p != null) p.teleport(p.getServerWorld(), tx, Arena.ground((int) Math.floor(tx), (int) Math.floor(tz)) + 1, tz, p.getYaw(), 0); });
                    return;
                }
                var eye = c.player.getEyePos();
                double dx = fx - eye.x, dy = fy - eye.y, dz = fz - eye.z, h = Math.hypot(dx, dz);
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
                c.player.setYaw(yaw); c.player.setPitch(pitch); c.player.prevYaw = yaw; c.player.prevPitch = pitch; c.player.setHeadYaw(yaw); c.player.prevHeadYaw = yaw;
                if (waited < 22) return;
                flagDrawn = PlanMarkers.pointDrawn(1); // B's own plan flag, not just any flag
                planWhy += "screen closed, facing B from " + Math.round(Math.hypot(h, dy)) + " blocks: PlanMarkers drew " + PlanMarkers.pointsDrawn() + " point flags (B's: " + flagDrawn + ") of " + PlanMarkers.drawn() + " flags; ";
                Phase4Checks.shot(c, "ui-plan-flag-" + id);
                c.setScreen(screen);
                next(19, tick);
            }
            case 19 -> {
                boolean pointsPlanned = CampaignClient.showsPoints();
                refusals();
                Phase4Checks.report("p4-map-plan-" + id, pointPopup && (!pointsPlanned || planInState && ackHeard && flagDrawn) && armyPopup && orderInState && aimedNoPopup && noPointsOk, planWhy);
                screen.popup = null;
                if (c.currentScreen != screen) c.setScreen(screen);
                Phase1Checks.window(c, 1920, 1080, 3, true);
                next(20, tick);
            }
            // ---- the big window and the smallest one
            case 20 -> { if (waited >= 8) { Phase4Checks.shot(c, "ui-map-fit-" + id + "-1080"); screen.mouseScrolled(screen.ax + screen.aw / 2.0, screen.ay + screen.ah / 2.0, 3); next(23, tick); } }
            case 23 -> { if (waited >= 4) { Phase4Checks.shot(c, "ui-map-zoom-" + id + "-1080"); screen.keyPressed(GLFW.GLFW_KEY_HOME, 0, 0); next(24, tick); } }
            case 24 -> { if (waited >= 3) { int[] p = screen.pointAt(1); if (p != null) screen.clickAt(p[0], p[1]); next(21, tick); } }
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

    /** The state's server time, or -1 when none. */
    static long stateTime() { var s = BattleMapClient.current(); return s == null ? -1 : s.time(); }

    /** What the server would refuse is never offered: an aimed bot (Siege, Convoy, Survival) gets no order popup, and a mode with no capture
     *  points gets only "Ping here" on a point. Both through the popup-building methods, with a fake state and a fake mode block. */
    void refusals() {
        var s = BattleMapClient.current();
        BattleMapClient.Army real = null;
        if (s != null) for (var a : s.armies()) if (a.bot() && !a.groups().isEmpty()) { real = a; break; }
        screen.popup = null;
        if (real == null) planWhy += "no bot army for the aimed check; ";
        else {
            var aimed = new BattleMapClient.Army(real.id(), real.name(), true, false, true, real.groups(), -1, -1, 0, 0, false);
            screen.openArmyPopup(aimed, 100, 100);
            aimedNoPopup = screen.popup == null;
            screen.popup = null;
            var free = new BattleMapClient.Army(real.id(), real.name(), true, false, false, real.groups(), -1, -1, 0, 0, false);
            screen.openArmyPopup(free, 100, 100);
            aimedNoPopup &= screen.popup != null; // and a free bot still gets one
            screen.popup = null;
        }
        var mode = CampaignClient.battleMode;
        var none = new com.google.gson.JsonObject();
        none.addProperty("points", false);
        CampaignClient.battleMode = none;
        try {
            screen.openPointPopup(1, 100, 100);
            noPointsOk = screen.popup != null && screen.popup.rows().equals(List.of("Ping here"));
            planWhy += "no capture points: point popup " + (screen.popup == null ? "none" : screen.popup.rows()) + "; aimed bot popup refused: " + aimedNoPopup + "; ";
        } finally { CampaignClient.battleMode = mode; screen.popup = null; }
    }

    void resetFindings() {
        fitOpen = realDrawn = discHit = zoomUp = zoomKept = dragMoved = homeFit = noGridOk = false;
        pointPopup = planInState = ackHeard = armyPopup = orderInState = flagDrawn = aimedNoPopup = noPointsOk = false;
        planClickTime = orderClickTime = -1;
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
