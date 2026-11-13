package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** The battle map (spec 2026-11-13 sections 2, 4, 5 and 6; it replaces MapScreen): the whole field under a {@link MapView}, in the
 *  parchment or the real look ({@link BattleMapArt}), between two dark wood bars with brass edges (the War Table direction). The top bar
 *  holds the mode, both scores with the held-point pips, the clock and the look toggle; the bottom bar the ping buttons, Fit and the
 *  legend. Shapes carry the meaning and colour only the state (UI spec 6.1). A left click without a drag picks, in order: a popup row,
 *  an armed ping or attack target, a point (the plan popup), an ally bot's plaque (the order popup), an own group (select). Right click
 *  moves the selected own groups. Scroll zooms, a drag past 4 px pans, T toggles the look, Home fits, M and Esc close. */
public final class BattleMapScreen extends GravesAndGloryClient.ArmyScreen {
    static final int TOP = 20, ROW = 12, BUTTON_H = 14, DRAG = 4;
    // the War Table palette: dark oak, brass, parchment and ink
    static final int WOOD = 0xff2a1c13, WOOD_DARK = 0xff1d130c, WOOD_LINE = 0xff342317, BRASS_LO = 0xff6b5530, TABLE = 0xff170f0a;
    static final int PARCH = 0xffdccba4, PARCH_DIM = 0xffb8ab8c, INK = 0xff241a10, PLATE = 0xe61a120b, TIP = 0xf21c140d, NEUTRAL = 0xffbdb4a0;
    static final String[] JOBS = {"Attack", "Defend", "Hold", "Follow"};

    /** The screen that opened the map; M returns there (the game when the map key opened it). */
    final Screen parent;
    /** The view of the field; built on the first init and kept across resizes. */
    MapView view;
    /** The map area: the screen between the bars. */
    int ax, ay, aw, ah, bottom;
    /** Rows of the legend: 1 when the controls and the glyph key share a line, else 2. */
    int legendRows;
    /** The ping kind armed by a ping button ("" none), and the army whose Attack waits for a point or a place (null none). */
    String pingMode = "", attackArmy;
    String attackName = "";
    Popup popup;
    /** What the last frame drew: the texture ("parchment", "real", "fallback" or "plain") and every marker's hit box. */
    String drawnTexture = "none";
    Identifier drawnId;
    final List<Hit> hits = new ArrayList<>();
    /** Boxes the frame's labels already cover: a new label moves to the nearest free row (declutter). */
    final List<int[]> taken = new ArrayList<>();
    // press state for the click-or-drag rule
    private double pressX, pressY, lastX, lastY;
    private int pressButton = -1;
    private boolean dragging;

    BattleMapScreen(Screen parent) { super("Battlefield map"); this.parent = parent; }

    /** A marker's box and what a click or a hover there means. kind: point, ally, own, enemy, ping, you. */
    record Hit(String kind, int x0, int y0, int x1, int y1, int point, String army, String group, List<String> tip) {
        boolean has(double x, double y) { return x >= x0 && x < x1 && y >= y0 && y < y1; }
        int cx() { return (x0 + x1) / 2; }
        int cy() { return (y0 + y1) / 2; }
    }
    /** An open popup: a title row and its option rows. kind 0 is a point (point), 1 an ally bot army (army). */
    record Popup(int kind, int point, String army, String title, List<String> rows, int active, double wx, double wz, int x, int y, int w) {
        int h() { return ROW + 3 + rows.size() * ROW + 2; }
        int rowY(int i) { return y + ROW + 3 + i * ROW; }
    }

    // ---------------------------------------------------------------- the look option

    private static Boolean parchment;
    static final String OPTION = "mapLook";

    /** True for the parchment look (the default), false for the real one; remembered in the client options file. */
    static boolean lookParchment() {
        if (parchment == null) {
            parchment = true;
            try {
                var f = GravesAndGloryClient.toolbarFlag();
                if (Files.exists(f)) { Properties p = new Properties(); p.load(new StringReader(Files.readString(f))); parchment = !"real".equals(p.getProperty(OPTION, "parchment")); }
            } catch (Exception e) { System.err.println("[Graves and Glory] Could not read the map look: " + e.getMessage()); }
        }
        return parchment;
    }

    /** Sets the look and writes it into the client options file, keeping its other lines. */
    static void setLook(boolean toParchment) {
        parchment = toParchment;
        try {
            var f = GravesAndGloryClient.toolbarFlag();
            Properties p = new Properties();
            if (Files.exists(f)) p.load(new StringReader(Files.readString(f)));
            p.setProperty(OPTION, toParchment ? "parchment" : "real");
            StringWriter out = new StringWriter();
            p.store(out, "Graves and Glory client options");
            Files.createDirectories(f.getParent());
            SafeFiles.write(f, out.toString());
        } catch (Exception e) { System.err.println("[Graves and Glory] Could not save the map look: " + e.getMessage()); }
    }

    // ---------------------------------------------------------------- layout

    /** The map's controls, named by the player's own bindings (a mouse button as LMB, RMB or MMB). */
    public static String legend() { return GravesAndGloryClient.hint("hit") + " select or plan / " + GravesAndGloryClient.hint("use") + " to move / scroll to zoom / drag to pan"; }
    static final String[] KEY_WORDS = {"Yours", "Ally", "Enemy", "Fight", "Plan"};

    int keyWidth() { int w = 0; for (String s : KEY_WORDS) w += 10 + textRenderer.getWidth(s) + 8; return w - 8; }

    @Override protected void init() {
        int legendW = textRenderer.getWidth(legend()), keyW = keyWidth();
        legendRows = legendW + 16 + keyW <= width - 16 ? 1 : 2;
        bottom = 3 + BUTTON_H + 3 + legendRows * 10 + 2;
        ax = 0; ay = TOP; aw = width; ah = Math.max(20, height - TOP - bottom);
        if (view == null) view = new MapView(Field.halfX(), Field.halfZ());
        view.layout(ax, ay, aw, ah);
        // the bottom bar's buttons: the pings, then Fit, centred
        List<String> labels = new ArrayList<>();
        for (var p : Spec.PINGS) labels.add(p.label());
        labels.add("Fit");
        int gap = 4, total = -gap;
        for (String l : labels) total += textRenderer.getWidth(l) + 12 + gap;
        int x = Math.max(4, (width - total) / 2), y = height - bottom + 3;
        for (int i = 0; i < labels.size(); i++) {
            String l = labels.get(i);
            int w = textRenderer.getWidth(l) + 12;
            if (i < Spec.PINGS.length) { String id = Spec.PINGS[i].id(); addDrawableChild(new WoodButton(x, y, w, BUTTON_H, l, () -> id.equals(pingMode), b -> { pingMode = pingMode.equals(id) ? "" : id; attackArmy = null; popup = null; })); }
            else addDrawableChild(new WoodButton(x, y, w, BUTTON_H, l, () -> false, b -> fit()));
            x += w + gap;
        }
        int tw = toggleWidth();
        addDrawableChild(new ToggleButton(width - tw - 4, 3, tw, BUTTON_H));
    }

    int toggleWidth() { return textRenderer.getWidth("Parchment | Real") + 12; }

    void fit() { view.fit(); }
    /** The look toggle's key: the player's own map_look binding, or while that is unbound (the default) the chat key, T unless rebound. A
     *  binding of its own on T would take T from chat, since a key drives only one binding. */
    static net.minecraft.client.option.KeyBinding lookKey() {
        var k = GravesAndGloryClient.mapLook;
        return k != null && !k.isUnbound() ? k : MinecraftClient.getInstance().options.chatKey;
    }
    void toggleLook() { setLook(!lookParchment()); }

    // ---------------------------------------------------------------- input

    boolean inMap(double x, double y) { return x >= ax && x < ax + aw && y >= ay && y < ay + ah; }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (GravesAndGloryClient.mapKey.matchesKey(key, scan)) { client.setScreen(parent); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && (popup != null || attackArmy != null || !pingMode.isEmpty())) { popup = null; attackArmy = null; pingMode = ""; return true; }
        if (lookKey().matchesKey(key, scan)) { toggleLook(); return true; }
        if (key == GLFW.GLFW_KEY_HOME) { fit(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inMap(mx, my)) return false;
        view.zoomAt(mx, my, amount);
        return true;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (popup == null && super.mouseClicked(mx, my, button)) return true; // the bar buttons
        if (!inMap(mx, my) && popup == null) return false;
        if (button == 1) {
            popup = null;
            if (inMap(mx, my) && client.player != null) CommandSelection.issueSelected("hold", CommandMode.formation, new Vec3d(view.worldX(mx), 65, view.worldZ(my)), client.player.getYaw(), -1);
            return true;
        }
        if (button == 0 || button == 2) { pressX = lastX = mx; pressY = lastY = my; pressButton = button; dragging = false; return true; }
        return false;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != pressButton || pressButton < 0) return super.mouseDragged(mx, my, button, dx, dy);
        if (!dragging && Math.hypot(mx - pressX, my - pressY) > DRAG) { dragging = true; view.pan(mx - pressX, my - pressY); lastX = mx; lastY = my; return true; }
        if (dragging) { view.pan(mx - lastX, my - lastY); lastX = mx; lastY = my; }
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        boolean mine = button == pressButton;
        boolean wasDrag = dragging;
        if (mine) { pressButton = -1; dragging = false; }
        if (mine && button == 0 && !wasDrag) { click(mx, my); return true; }
        return mine || super.mouseReleased(mx, my, button);
    }

    /** A left click without a drag: popup row, armed ping or attack, point, ally bot plaque, own group. */
    void click(double mx, double my) {
        if (popup != null) {
            var p = popup;
            popup = null;
            if (mx >= p.x() && mx < p.x() + p.w() && my >= p.rowY(0) && my < p.rowY(p.rows().size())) choose(p, p.rows().get((int) ((my - p.rowY(0)) / ROW)));
            return; // a click elsewhere only closes it
        }
        if (!inMap(mx, my)) return;
        double wx = view.worldX(mx), wz = view.worldZ(my);
        if (!pingMode.isEmpty()) { CommandMode.ping(pingMode, wx, wz); pingMode = ""; return; }
        Hit point = hit("point", mx, my), ally = hit("ally", mx, my), own = hit("own", mx, my);
        if (attackArmy != null) {
            if (point != null) BattleMapClient.sendPlan(1, point.point(), attackArmy, BattlePlan.Job.ATTACK.ordinal(), 0, 0);
            else BattleMapClient.sendPlan(1, -1, attackArmy, BattlePlan.Job.ATTACK.ordinal(), wx, wz);
            attackArmy = null;
            return;
        }
        if (point != null) { openPointPopup(point.point(), point.cx(), point.cy()); return; }
        if (ally != null) {
            var a = army(ally.army());
            if (a != null && a.bot() && !a.aimed()) { openArmyPopup(a, ally.cx(), ally.cy()); return; }
        }
        if (own != null) CommandSelection.select(own.group(), hasShiftDown());
    }

    Hit hit(String kind, double x, double y) {
        for (int i = hits.size() - 1; i >= 0; i--) { var h = hits.get(i); if (h.kind().equals(kind) && h.has(x, y)) return h; }
        return null;
    }

    void openPointPopup(int i, int x, int y) {
        var pts = Spec.POINTS;
        if (i < 0 || i >= pts.length) return;
        List<String> rows = CampaignClient.showsPoints() ? List.of("Attack", "Defend", "Hold", "Clear") : List.of("Ping here");
        int active = -1;
        var plan = pointPlan(i);
        if (plan != null && CampaignClient.showsPoints()) active = plan.job();
        popup = place(new Popup(0, i, null, pts[i].name(), rows, active, pts[i].x(), pts[i].z(), x, y, 0));
    }

    void openArmyPopup(BattleMapClient.Army a, int x, int y) {
        int active = -1;
        for (var o : orders()) if (o.army().equals(a.id())) active = o.job(); // the rows follow the Job order
        double[] c = centre(a);
        popup = place(new Popup(1, -1, a.id(), a.name(), List.of("Attack", "Defend here", "Hold here", "Follow me", "Clear"), active, c[0], c[1], x, y, 0));
    }

    /** Sizes the popup to its words and sets it beside the anchor, inside the map area. */
    Popup place(Popup p) {
        int w = textRenderer.getWidth(p.title()) + 12;
        for (String r : p.rows()) w = Math.max(w, textRenderer.getWidth(r) + 16);
        int h = ROW + 3 + p.rows().size() * ROW + 2;
        // above-right, above-left, below-right, below-left of the anchor: the first spot that covers no label drawn last frame
        int[][] spots = {{p.x() + 14, p.y() + 8 - h}, {p.x() - 14 - w, p.y() + 8 - h}, {p.x() + 14, p.y() - 8}, {p.x() - 14 - w, p.y() - 8}};
        int x = 0, y = 0;
        for (int k = 0; k < spots.length; k++) {
            int cx = Math.max(ax + 2, Math.min(spots[k][0], ax + aw - w - 2));
            int cy = Math.max(ay + 2, Math.min(spots[k][1], ay + ah - h - 2));
            if (k == 0) { x = cx; y = cy; }
            boolean clear = true;
            for (int[] t : taken) if (cx < t[0] + t[2] && cx + w > t[0] && cy < t[1] + t[3] && cy + h > t[1]) { clear = false; break; }
            if (clear) { x = cx; y = cy; break; }
        }
        return new Popup(p.kind(), p.point(), p.army(), p.title(), p.rows(), p.active(), p.wx(), p.wz(), x, y, w);
    }

    void choose(Popup p, String row) {
        if (p.kind() == 0) {
            switch (row) {
                case "Attack" -> BattleMapClient.sendPlan(0, p.point(), "", BattlePlan.Job.ATTACK.ordinal(), 0, 0);
                case "Defend" -> BattleMapClient.sendPlan(0, p.point(), "", BattlePlan.Job.DEFEND.ordinal(), 0, 0);
                case "Hold" -> BattleMapClient.sendPlan(0, p.point(), "", BattlePlan.Job.HOLD.ordinal(), 0, 0);
                case "Clear" -> BattleMapClient.sendPlan(0, p.point(), "", -1, 0, 0);
                case "Ping here" -> CommandMode.ping(Spec.PINGS.length > 0 ? Spec.PINGS[0].id() : "attack", p.wx(), p.wz());
                default -> {}
            }
            return;
        }
        switch (row) {
            case "Attack" -> { attackArmy = p.army(); attackName = p.title(); }
            case "Defend here" -> BattleMapClient.sendPlan(1, -1, p.army(), BattlePlan.Job.DEFEND.ordinal(), p.wx(), p.wz());
            case "Hold here" -> BattleMapClient.sendPlan(1, -1, p.army(), BattlePlan.Job.HOLD.ordinal(), p.wx(), p.wz());
            case "Follow me" -> BattleMapClient.sendPlan(1, -1, p.army(), BattlePlan.Job.FOLLOW.ordinal(), 0, 0);
            case "Clear" -> BattleMapClient.sendPlan(1, -1, p.army(), -1, 0, 0);
            default -> {}
        }
    }

    // ---------------------------------------------------------------- data

    String me() { return client.player == null ? "" : client.player.getUuid().toString(); }

    List<BattleMapClient.Army> allies() {
        var s = BattleMapClient.state;
        if (s == null) return List.of();
        List<BattleMapClient.Army> out = new ArrayList<>();
        String me = me();
        for (var a : s.armies()) if (!a.id().equals(me) && !a.groups().isEmpty()) out.add(a);
        return out;
    }

    BattleMapClient.Army army(String id) { for (var a : allies()) if (a.id().equals(id)) return a; return null; }
    List<BattleMapClient.Order> orders() { var s = BattleMapClient.state; return s == null ? List.of() : s.orders(); }
    BattleMapClient.PointPlan pointPlan(int i) { var s = BattleMapClient.state; if (s == null) return null; for (var p : s.points()) if (p.point() == i) return p; return null; }

    static int count(BattleMapClient.Army a) { int n = 0; for (var g : a.groups()) n += g.count(); return n; }
    static double[] centre(BattleMapClient.Army a) {
        double x = 0, z = 0, n = 0;
        for (var g : a.groups()) { x += g.x() * g.count(); z += g.z() * g.count(); n += g.count(); }
        return n == 0 ? new double[] {0, 0} : new double[] {x / n, z / n};
    }

    static String groupShort(String id) { return id.length() <= 3 ? id.toUpperCase() : id.substring(0, 3).toUpperCase(); }
    static String groupName(String id) { return id.isEmpty() ? "Group" : Character.toUpperCase(id.charAt(0)) + id.substring(1); }

    String pointName(int i) { return i >= 0 && i < Spec.POINTS.length ? Spec.POINTS[i].name() : "a point"; }

    String jobText(int job, int point, boolean direct) {
        if (job < 0 || job >= JOBS.length) return "Its own plan";
        String what = JOBS[job] + (job == 3 ? "ing you" : point >= 0 ? " " + pointName(point) : " a place");
        return what + (direct ? " (order)" : " (team plan)");
    }

    // ---------------------------------------------------------------- drawing

    @Override public void render(DrawContext g, int mx, int my, float delta) {
        view.layout(ax, ay, aw, ah); // re-clamps when the window changed under a live screen
        hits.clear();
        taken.clear();
        if (client != null && client.player != null) { // the "you" arrow keeps its spot; labels move off it
            int yx = sx(client.player.getX()), yy = sy(client.player.getZ());
            taken.add(new int[] {yx - 8, yy - 8, 17, 17});
        }
        g.fill(0, 0, width, height, TABLE);
        g.enableScissor(ax, ay, ax + aw, ay + ah);
        try {
            drawField(g);
            drawPoints(g);
            drawSightings(g);
            drawAllies(g);
            drawOwn(g);
            drawPings(g);
            drawYou(g);
        } finally { g.disableScissor(); }
        drawTopBar(g);
        drawBottomBar(g);
        super.render(g, mx, my, delta); // the bar buttons
        if (attackArmy != null) banner(g, "Click a point or a place for " + attackName + " to attack / Esc cancels");
        else if (!pingMode.isEmpty()) banner(g, "Click the map to ping / Esc cancels");
        if (popup != null) drawPopup(g, mx, my);
        else if (inMap(mx, my) && pressButton < 0) tooltip(g, mx, my);
    }

    @Override public void renderBackground(DrawContext g) {}

    void drawField(DrawContext g) {
        int hx = Field.halfX(), hz = Field.halfZ();
        boolean parch = lookParchment();
        Identifier tex = parch ? BattleMapArt.parchment() : BattleMapArt.real();
        var grid = BattleMapClient.grid;
        int x0, y0, x1, y1;
        if (tex != null && grid != null) {
            x0 = (int) Math.round(view.screenX(grid.minX)); y0 = (int) Math.round(view.screenY(grid.minZ));
            x1 = (int) Math.round(view.screenX(grid.minX + grid.width)); y1 = (int) Math.round(view.screenY(grid.minZ + grid.depth));
            int[] s = BattleMapArt.size(!parch);
            g.drawTexture(tex, x0, y0, x1 - x0, y1 - y0, 0f, 0f, s[0], s[1], s[0], s[1]);
            drawnTexture = parch ? "parchment" : "real";
            drawnId = tex;
        } else {
            x0 = (int) Math.round(view.screenX(-hx)); y0 = (int) Math.round(view.screenY(-hz));
            x1 = (int) Math.round(view.screenX(hx + 1)); y1 = (int) Math.round(view.screenY(hz + 1));
            if (GravesAndGloryClient.mapTexture != null) {
                g.drawTexture(GravesAndGloryClient.mapTexture, x0, y0, x1 - x0, y1 - y0, 0f, 0f, 281, 201, 281, 201);
                drawnTexture = "fallback"; drawnId = GravesAndGloryClient.mapTexture;
            } else {
                g.fill(x0, y0, x1, y1, PARCH);
                drawnTexture = "plain"; drawnId = null;
            }
        }
        // the brass frame round the sheet
        g.drawBorder(x0 - 2, y0 - 2, x1 - x0 + 4, y1 - y0 + 4, BRASS_LO);
        g.drawBorder(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, DarkUi.BRASS);
    }

    /** The y for a w x h label wanted at (x, y): y itself when free, else the nearest free row a step up or down (three each way), else y.
     *  The box chosen is taken. */
    int free(int x, int y, int w, int h) {
        int step = h + 1;
        for (int k = 0; k <= 6; k++) {
            int ty = y + (k == 0 ? 0 : (k % 2 == 1 ? -1 : 1) * ((k + 1) / 2) * step);
            boolean clear = true;
            for (int[] t : taken) if (x < t[0] + t[2] && x + w > t[0] && ty < t[1] + t[3] && ty + h > t[1]) { clear = false; break; }
            if (clear) { taken.add(new int[] {x, ty, w, h}); return ty; }
        }
        taken.add(new int[] {x, y, w, h});
        return y;
    }

    /** A 1 px leader from an anchor to a label moved off it. */
    static void leader(DrawContext g, int x, int y, int px, int py, int w, int h, int color) {
        int ly = py > y ? py : py + h;
        if (Math.abs(ly - y) > 2) line(g, x, y, Math.max(px, Math.min(px + w - 1, x)), ly, color, 1, 1);
    }

    static String trimName(String n) { return n.length() <= 16 ? n : n.substring(0, 15) + "."; }

    int sx(double wx) { return (int) Math.round(view.screenX(wx)); }
    int sy(double wz) { return (int) Math.round(view.screenY(wz)); }

    void drawPoints(DrawContext g) {
        var pts = Spec.POINTS;
        int mine = GravesAndGloryClient.team;
        for (var p : pts) { int x = sx(p.x() + .5), y = sy(p.z() + .5); taken.add(new int[] {x - 8, y - 8, 17, 17}); }
        for (int i = 0; i < pts.length; i++) {
            var p = pts[i];
            int x = sx(p.x() + .5), y = sy(p.z() + .5);
            int owner = GravesAndGloryClient.ownerAt(i), progress = GravesAndGloryClient.progressAt(i), ch = GravesAndGloryClient.challengerAt(i);
            boolean contested = GravesAndGloryClient.contestedAt(i);
            double r = p.radius() * view.scale();
            if (r >= 14) dottedCircle(g, x, y, (int) r, 0x99241a10);
            // the capture rings: progress in the challenger's colour, a hatched ring while contested
            if (progress > 0 && p.captureSeconds() > 0) {
                arc(g, x, y, 9, Math.min(1, progress / (double) p.captureSeconds()), DarkUi.teamColor(ch), false);
                DarkUi.teamMark(g, ch, x - 19, y - 3); // the challenger's mark left of the ring
            }
            if (contested) arc(g, x, y, 10, 1, DarkUi.CONTEST, true);
            disc(g, x, y, 7, INK);
            disc(g, x, y, 6, owner < 0 ? NEUTRAL : DarkUi.teamColor(owner));
            if (owner >= 0) DarkUi.teamMark(g, owner, x + 12, y - 3); // and the owner's right of it
            String id = p.id();
            g.drawText(textRenderer, id, x - textRenderer.getWidth(id) / 2 + 1, y - 3, INK, false);
            String name = p.name();
            int nw = textRenderer.getWidth(name);
            int ny = free(x - nw / 2 - 2, y + 10, nw + 5, 10);
            g.fill(x - nw / 2 - 2, ny, x + nw / 2 + 3, ny + 10, PLATE);
            g.drawText(textRenderer, name, x - nw / 2 + 1, ny + 1, DarkUi.IVORY, false);
            var plan = pointPlan(i);
            if (plan != null) flag(g, x, y - 8, plan.job(), true);
            List<String> tip = new ArrayList<>();
            tip.add(p.name() + " (" + id + ")");
            tip.add(owner < 0 ? "Neutral" : owner == mine ? "Held by your side" : "Held by the enemy");
            if (contested) tip.add("Contested");
            else if (progress > 0 && p.captureSeconds() > 0) tip.add((ch == mine ? "Your side is taking it: " : "The enemy is taking it: ") + Math.min(100, 100 * progress / p.captureSeconds()) + "%");
            if (plan != null) tip.add("Plan: " + JOBS[Math.max(0, Math.min(3, plan.job()))] + ", set by " + plan.setter());
            else if (CampaignClient.showsPoints()) tip.add("No plan: click to plan");
            hits.add(new Hit("point", x - 8, y - 8, x + 9, y + 9, i, null, null, tip));
        }
    }

    void drawSightings(DrawContext g) {
        int memory = Spec.SCOUTING.length > 0 ? Math.max(1, Spec.SCOUTING[0].memorySeconds() * 20) : 400;
        for (var s : GravesAndGloryClient.sightings) {
            int x = sx(s.x()), y = sy(s.z());
            boolean fresh = s.age() < 20;
            int alpha = fresh ? 255 : Math.max(60, 220 - 160 * s.age() / memory);
            glyph(g, fresh ? TRI : TRI_OPEN, x - 3, y - 4, fresh ? DarkUi.MAP_ENEMY : (alpha << 24) | (DarkUi.MAP_ENEMY & 0xffffff), fresh ? INK : 0);
            hits.add(new Hit("enemy", x - 4, y - 4, x + 5, y + 4, -1, null, null, List.of("Enemy", fresh ? "Seen now" : "Seen " + Math.max(1, s.age() / 20) + " s ago")));
        }
    }

    void drawAllies(DrawContext g) {
        var allies = allies();
        if (BattleMapClient.state == null) { // no battlemap_v1 yet: the state_v10 marks, as rings
            for (var m : GravesAndGloryClient.markers) if (!m.own()) {
                int x = sx(m.x()), y = sy(m.z());
                DarkUi.mapMark(g, "ally", x, y, DarkUi.MAP_TROOP);
                if (m.fighting()) glyph(g, SWORDS, x - 3, y - 11, DarkUi.WARN, INK);
            }
            return;
        }
        for (var a : allies) {
            double[] c = centre(a);
            int x = sx(c[0]), y = sy(c[1]);
            boolean fighting = false;
            double tx = 0, tz = 0, n = 0;
            for (var gr : a.groups()) {
                int gx = sx(gr.x()), gy = sy(gr.z());
                DarkUi.mapMark(g, "ally", gx, gy, DarkUi.MAP_TROOP);
                fighting |= gr.fighting();
                tx += gr.targetX() * gr.count(); tz += gr.targetZ() * gr.count(); n += gr.count();
            }
            // where it is heading: the plan's place, else its groups' goal
            int ex, ey;
            if (a.job() >= 0) { ex = sx(a.planX()); ey = sy(a.planZ()); }
            else { ex = n > 0 ? sx(tx / n) : x; ey = n > 0 ? sy(tz / n) : y; }
            if (Math.hypot(ex - x, ey - y) > 10) {
                line(g, x, y, ex, ey, 0xddeee9df, 1, 3);
                if (a.job() >= 0 && a.point() < 0) glyph(g, jobGlyph(a.job()), ex - 3, ey - 3, DarkUi.IVORY, INK); // a point's own flag shows its plan
            }
            boolean named = a.name().contains(" bot "); // "normal bot 2" says bot already (spec call 12)
            String label = trimName(a.name()) + (a.bot() && !named ? " (bot)" : "") + ": " + count(a);
            int w = textRenderer.getWidth(label) + 6, px = x - w / 2, py = free(px, y - 16, w, 11);
            leader(g, x, y, px, py, w, 11, 0xbbeee9df);
            g.fill(px, py, px + w, py + 11, 0x99120c08);
            g.drawBorder(px, py, w, 11, DarkUi.IVORY);
            g.drawText(textRenderer, label, px + 3, py + 2, DarkUi.IVORY, false);
            if (fighting) glyph(g, SWORDS, px + w + 2, py + 2, DarkUi.WARN, INK);
            List<String> tip = new ArrayList<>();
            tip.add(a.name() + (a.bot() ? " (bot)" : " (player)"));
            tip.add(count(a) + " troops in " + a.groups().size() + (a.groups().size() == 1 ? " group" : " groups"));
            if (a.retreating()) tip.add("Falling back");
            else if (a.aimed()) tip.add("Follows the mode's own plan");
            else if (a.bot()) tip.add(jobText(a.job(), a.point(), a.direct()));
            if (fighting) tip.add("In combat");
            if (a.bot() && !a.aimed()) tip.add("Click to give an order");
            hits.add(new Hit("ally", px, py, px + w, py + 11, -1, a.id(), null, tip));
        }
    }

    void drawOwn(DrawContext g) {
        for (var gr : CommandMode.groups) {
            if (gr.count() <= 0) continue; // a group with nobody left has no place on the map
            int x = sx(gr.cx()), y = sy(gr.cz()), tx = sx(gr.x()), ty = sy(gr.z());
            boolean selected = CommandSelection.isSelected(gr.id());
            if (Math.hypot(tx - x, ty - y) > 10) {
                line(g, x, y, tx, ty, selected ? DarkUi.GOLD : 0xddeee9df, 3, 2);
                glyph(g, orderGlyph(gr.order()), tx - 3, ty - 3, selected ? DarkUi.GOLD : DarkUi.IVORY, INK);
            }
            String label = groupShort(gr.id()) + " " + gr.count();
            int w = textRenderer.getWidth(label) + 6, px = x - w / 2, py = free(px, y - 6, w, 14);
            leader(g, x, y, px, py, w, 14, DarkUi.BRASS);
            g.fill(px, py, px + w, py + 11, 0xf0241810);
            g.drawBorder(px, py, w, 11, selected ? DarkUi.BRASS_HI : BRASS_LO);
            if (selected) g.drawBorder(px - 1, py - 1, w + 2, 13, DarkUi.GOLD);
            g.drawText(textRenderer, label, px + 3, py + 2, DarkUi.IVORY, false);
            // health under the plaque
            int hw = w - 2, fillW = (int) Math.round(hw * Math.max(0, Math.min(1, gr.health())));
            g.fill(px + 1, py + 12, px + 1 + hw, py + 14, 0xcc120c08);
            g.fill(px + 1, py + 12, px + 1 + fillW, py + 14, gr.health() < .35 ? DarkUi.ALARM_TEXT : PARCH);
            if (gr.fighting()) glyph(g, SWORDS, px + w + 2, py + 2, DarkUi.WARN, INK);
            List<String> tip = new ArrayList<>();
            tip.add(groupName(gr.id()) + ": " + gr.count() + " troops");
            tip.add("Health " + Math.round(100 * gr.health()) + "%");
            tip.add("Order: " + gr.order());
            if (gr.fighting()) tip.add("In combat");
            hits.add(new Hit("own", px, py, px + w, py + 14, -1, null, gr.id(), tip));
        }
    }

    void drawPings(DrawContext g) {
        for (var p : CommandMode.pings) {
            int x = sx(p.x()), y = sy(p.z());
            ring(g, x, y, 6, DarkUi.PING);
            String label = p.kind().toUpperCase();
            int w = textRenderer.getWidth(label);
            g.fill(x + 8, y - 1, x + 11 + w, y + 9, PLATE);
            g.drawText(textRenderer, label, x + 10, y, DarkUi.PING, false);
            hits.add(new Hit("ping", x - 6, y - 6, x + 7, y + 7, -1, null, null, List.of("Ping: " + p.kind())));
        }
    }

    void drawYou(DrawContext g) {
        var p = client.player;
        if (p == null) return;
        double yaw = Math.toRadians(p.getYaw()), dx = -Math.sin(yaw), dz = Math.cos(yaw);
        int x = sx(p.getX()), y = sy(p.getZ());
        triangle(g, x, y, dx, dz, 7.5, 4.5, INK);
        triangle(g, x, y, dx, dz, 5.5, 3, DarkUi.MAP_YOU);
        hits.add(new Hit("you", x - 4, y - 4, x + 5, y + 5, -1, null, null, List.of("You")));
    }

    void drawTopBar(DrawContext g) {
        wood(g, 0, 0, width, TOP);
        g.fill(0, TOP - 2, width, TOP - 1, DarkUi.BRASS);
        g.fill(0, TOP - 1, width, TOP, BRASS_LO);
        String mode = CampaignClient.text(CampaignClient.battleMode, "name");
        if (mode.isEmpty()) mode = "Battle";
        g.drawText(textRenderer, mode, 6, 6, DarkUi.BRASS_HI, true);
        // the ledger: mark, score and held pips for each side about the clock
        boolean pips = CampaignClient.showsPoints();
        String a = ModeMath.ledgerText(CampaignClient.ledgerFormat(0), GravesAndGloryClient.dawn, CampaignClient.ledgerMax(0));
        String b = ModeMath.ledgerText(CampaignClient.ledgerFormat(1), GravesAndGloryClient.dusk, CampaignClient.ledgerMax(1));
        String clock = UiMath.clock(CommandMode.seconds);
        int heldA = 0, heldB = 0;
        for (int i = 0; i < Spec.POINTS.length; i++) { int o = GravesAndGloryClient.ownerAt(i); if (o == 0) heldA++; else if (o == 1) heldB++; }
        int wa = 9 + textRenderer.getWidth(a) + (pips ? heldA * 6 + 2 : 0), wb = 9 + textRenderer.getWidth(b) + (pips ? heldB * 6 + 2 : 0), wc = textRenderer.getWidth(clock) + 16;
        int x = width / 2 - wc / 2 - wa;
        int maxRight = width - toggleWidth() - 10;
        if (x + wa + wc + wb > maxRight) x = Math.max(textRenderer.getWidth(mode) + 14, maxRight - (wa + wc + wb));
        DarkUi.teamMark(g, 0, x, 6);
        x += 9;
        g.drawText(textRenderer, a, x, 6, DarkUi.IVORY, true);
        x += textRenderer.getWidth(a) + 2;
        if (pips) for (int i = 0; i < heldA; i++) { plus(g, x + 2, 9, DarkUi.IVORY); x += 6; } // beside the side's own mark
        g.fill(x + 3, 4, x + wc - 3, 16, 0xff120c08);
        g.drawBorder(x + 3, 4, wc - 6, 12, BRASS_LO);
        g.drawText(textRenderer, clock, x + 8, 6, PARCH, false);
        x += wc;
        if (pips) for (int i = 0; i < heldB; i++) { plus(g, x + 2, 9, DarkUi.IVORY); x += 6; }
        if (pips && heldB > 0) x += 2;
        g.drawText(textRenderer, b, x, 6, DarkUi.IVORY, true);
        x += textRenderer.getWidth(b) + 2;
        DarkUi.teamMark(g, 1, x, 6);
    }

    void drawBottomBar(DrawContext g) {
        int y0 = height - bottom;
        wood(g, 0, y0, width, bottom);
        g.fill(0, y0, width, y0 + 1, BRASS_LO);
        g.fill(0, y0 + 1, width, y0 + 2, DarkUi.BRASS);
        String legend = legend();
        int lw = textRenderer.getWidth(legend), kw = keyWidth();
        int ly = y0 + 3 + BUTTON_H + 4;
        if (legendRows == 1) {
            int x = (width - lw - 16 - kw) / 2;
            g.drawText(textRenderer, legend, x, ly, DarkUi.MUTED, false);
            key(g, x + lw + 16, ly);
        } else {
            g.drawText(textRenderer, legend, (width - lw) / 2, ly, DarkUi.MUTED, false);
            key(g, (width - kw) / 2, ly + 10);
        }
    }

    /** The glyph key: Yours, Ally, Enemy, Fight, Plan, each after its map shape. */
    void key(DrawContext g, int x, int y) {
        for (int i = 0; i < KEY_WORDS.length; i++) {
            switch (i) {
                case 0 -> { g.fill(x, y, x + 8, y + 7, 0xf0241810); g.drawBorder(x, y, 8, 7, DarkUi.BRASS); }
                case 1 -> g.drawBorder(x, y, 8, 7, DarkUi.IVORY);
                case 2 -> glyph(g, TRI, x, y, DarkUi.MAP_ENEMY, 0);
                case 3 -> glyph(g, SWORDS, x, y, DarkUi.WARN, 0);
                default -> glyph(g, FLAG_KEY, x, y, PARCH, 0);
            }
            g.drawText(textRenderer, KEY_WORDS[i], x + 10, y, DarkUi.MUTED, false);
            x += 10 + textRenderer.getWidth(KEY_WORDS[i]) + 8;
        }
    }

    void banner(DrawContext g, String text) {
        int w = textRenderer.getWidth(text) + 12, x = (width - w) / 2, y = ay + 4;
        g.fill(x, y, x + w, y + 13, TIP);
        g.drawBorder(x, y, w, 13, DarkUi.BRASS);
        g.drawText(textRenderer, text, x + 6, y + 3, DarkUi.IVORY, false);
    }

    void drawPopup(DrawContext g, int mx, int my) {
        var p = popup;
        int h = p.h();
        g.fill(p.x() + 2, p.y() + 2, p.x() + p.w() + 2, p.y() + h + 2, 0x66000000);
        wood(g, p.x(), p.y(), p.w(), h);
        g.drawBorder(p.x(), p.y(), p.w(), h, DarkUi.BRASS);
        g.drawText(textRenderer, p.title(), p.x() + 6, p.y() + 3, DarkUi.BRASS_HI, false);
        g.fill(p.x() + 3, p.y() + ROW + 1, p.x() + p.w() - 3, p.y() + ROW + 2, BRASS_LO);
        for (int i = 0; i < p.rows().size(); i++) {
            int ry = p.rowY(i);
            boolean hover = mx >= p.x() && mx < p.x() + p.w() && my >= ry && my < ry + ROW;
            if (hover) g.fill(p.x() + 2, ry, p.x() + p.w() - 2, ry + ROW, 0x55dfbd77);
            if (i == p.active()) g.fill(p.x() + 3, ry + 2, p.x() + 5, ry + ROW - 2, DarkUi.GOLD);
            g.drawText(textRenderer, p.rows().get(i), p.x() + 9, ry + 2, hover ? DarkUi.BRASS_HI : DarkUi.IVORY, false);
        }
    }

    void tooltip(DrawContext g, int mx, int my) {
        Hit h = null;
        for (int i = hits.size() - 1; i >= 0 && h == null; i--) if (hits.get(i).has(mx, my)) h = hits.get(i);
        if (h == null) return;
        int w = 0;
        for (String s : h.tip()) w = Math.max(w, textRenderer.getWidth(s));
        w += 10;
        int th = h.tip().size() * 10 + 5, x = mx + 10, y = my + 10;
        if (x + w > width - 2) x = mx - 10 - w;
        if (y + th > ay + ah) y = my - 10 - th;
        x = Math.max(2, x); y = Math.max(ay, y);
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);
        g.fill(x, y, x + w, y + th, TIP);
        g.drawBorder(x, y, w, th, DarkUi.BRASS);
        for (int i = 0; i < h.tip().size(); i++) g.drawText(textRenderer, h.tip().get(i), x + 5, y + 3 + i * 10, i == 0 ? DarkUi.BRASS_HI : DarkUi.IVORY, false);
        g.getMatrices().pop();
    }

    // ---------------------------------------------------------------- shapes

    /** Dark oak: a base, plank seams every 6 px and a fine grain line between. */
    static void wood(DrawContext g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, WOOD);
        for (int j = y + 2; j < y + h; j += 3) g.fill(x, j, x + w, j + 1, (j - y) % 6 == 2 ? WOOD_DARK : WOOD_LINE);
    }

    static void disc(DrawContext g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.floor(Math.sqrt(r * r - dy * dy + r * .8));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    static void ring(DrawContext g, int cx, int cy, int r, int color) { arc(g, cx, cy, r, 1, color, false); }

    /** A 1 px ring of radius r from the top, clockwise, over `part` of the turn; hatched leaves every other 4 px dash out. */
    static void arc(DrawContext g, int cx, int cy, int r, double part, int color, boolean hatched) {
        int steps = Math.max(16, (int) (2 * Math.PI * r * 1.5));
        int end = (int) Math.round(steps * part);
        for (int i = 0; i < end; i++) {
            double a = 2 * Math.PI * i / steps;
            if (hatched && (int) (i * 2 * Math.PI * r / steps / 3) % 2 == 1) continue;
            int x = cx + (int) Math.round(Math.sin(a) * r), y = cy - (int) Math.round(Math.cos(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void dottedCircle(DrawContext g, int cx, int cy, int r, int color) {
        int steps = Math.max(12, (int) (2 * Math.PI * r / 4));
        for (int i = 0; i < steps; i++) {
            double a = 2 * Math.PI * i / steps;
            int x = cx + (int) Math.round(Math.sin(a) * r), y = cy - (int) Math.round(Math.cos(a) * r);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** A line of `on` px dashes with `off` px gaps (1 px wide). */
    static void line(DrawContext g, int x0, int y0, int x1, int y1, int color, int on, int off) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        int n = (int) len;
        for (int i = 0; i < n; i++) {
            if (i % (on + off) >= on) continue;
            int x = x0 + (int) Math.round((x1 - x0) * i / len), y = y0 + (int) Math.round((y1 - y0) * i / len);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    static void plus(DrawContext g, int cx, int cy, int color) {
        g.fill(cx - 2, cy, cx + 3, cy + 1, color);
        g.fill(cx, cy - 2, cx + 1, cy + 3, color);
    }

    /** A filled triangle pointing (dx, dz): tip `len` ahead of the centre, base `len*.6` behind it, `half` wide each side. */
    static void triangle(DrawContext g, int cx, int cy, double dx, double dz, double len, double half, int color) {
        double ax = cx + dx * len, ay = cy + dz * len, bx = cx - dx * len * .6 + -dz * half, by = cy - dz * len * .6 + dx * half, ex = cx - dx * len * .6 - -dz * half, ey = cy - dz * len * .6 - dx * half;
        int r = (int) Math.ceil(len) + 1;
        for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++) {
            double px = x + .5, py = y + .5;
            double d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by), d2 = (px - ex) * (by - ey) - (bx - ex) * (py - ey), d3 = (px - ax) * (ey - ay) - (ex - ax) * (py - ay);
            boolean neg = d1 < 0 || d2 < 0 || d3 < 0, pos = d1 > 0 || d2 > 0 || d3 > 0;
            if (!(neg && pos)) g.fill(x, y, x + 1, y + 1, color);
        }
    }

    /** A plan flag standing on (x, y): a pole and a parchment cloth with the job's glyph. */
    void flag(DrawContext g, int x, int y, int job, boolean big) {
        int h = big ? 13 : 9;
        g.fill(x, y - h, x + 1, y, INK);
        g.fill(x + 1, y - h, x + 12, y - h + 9, INK);
        g.fill(x + 1, y - h + 1, x + 11, y - h + 8, PARCH);
        glyph(g, jobGlyph(job), x + 3, y - h + 1, INK, 0);
    }

    static String[] jobGlyph(int job) { return switch (job) { case 0 -> SWORD; case 1 -> SHIELD; case 2 -> BANNER; default -> FOLLOW; }; }
    static String[] orderGlyph(String order) {
        return switch (order) { case "attack", "charge" -> SWORD; case "hold", "formation" -> BANNER; case "follow" -> FOLLOW; case "retreat" -> BACK; default -> TARGET; };
    }

    /** A 7x7 pixel glyph at (x, y) in color, with a 1 px ink outline when outline is not 0. */
    static void glyph(DrawContext g, String[] rows, int x, int y, int color, int outline) {
        if (outline != 0) for (int j = 0; j < rows.length; j++) for (int i = 0; i < rows[j].length(); i++) if (rows[j].charAt(i) == 'x') g.fill(x + i - 1, y + j - 1, x + i + 2, y + j + 2, outline);
        for (int j = 0; j < rows.length; j++) for (int i = 0; i < rows[j].length(); i++) if (rows[j].charAt(i) == 'x') g.fill(x + i, y + j, x + i + 1, y + j + 1, color);
    }

    static final String[] SWORD = {"......x", ".....x.", "....x..", ".x.x...", "..x....", ".x.x...", "x......"};
    static final String[] SHIELD = {"xxxxxxx", "xxxxxxx", "xxxxxxx", "xxxxxxx", ".xxxxx.", "..xxx..", "...x..."};
    static final String[] BANNER = {"xxxxx..", "xxxxx..", "xxxx...", "xxxxx..", "x......", "x......", "x......"};
    static final String[] FOLLOW = {"...x...", "..xxx..", ".x.x.x.", "...x...", "..xxx..", ".x.x.x.", "...x..."};
    static final String[] BACK = {"...x...", "...x...", "...x...", "...x...", ".x.x.x.", "..xxx..", "...x..."};
    static final String[] TARGET = {"..xxx..", ".x...x.", "x.....x", "x..x..x", "x.....x", ".x...x.", "..xxx.."};
    static final String[] SWORDS = {"x.....x", ".x...x.", "..x.x..", "...x...", "..x.x..", "xx...xx", "x.....x"};
    static final String[] TRI = {"...x...", "...x...", "..xxx..", "..xxx..", ".xxxxx.", ".xxxxx.", "xxxxxxx"};
    static final String[] TRI_OPEN = {"...x...", "...x...", "..x.x..", "..x.x..", ".x...x.", ".x...x.", "xxxxxxx"};
    static final String[] FLAG_KEY = {"xxxxx..", "xxxxx..", "xxxx...", "xxxxx..", "x......", "x......", "x......"};

    // ---------------------------------------------------------------- buttons

    /** A wood plate with a brass edge; lit when on() (the armed ping). */
    final class WoodButton extends ButtonWidget {
        final java.util.function.BooleanSupplier on;
        WoodButton(int x, int y, int w, int h, String label, java.util.function.BooleanSupplier on, PressAction action) { super(x, y, w, h, Text.literal(label), action, DEFAULT_NARRATION_SUPPLIER); this.on = on; }
        @Override public void renderButton(DrawContext g, int mx, int my, float delta) {
            boolean lit = on.getAsBoolean(), hover = isHovered() || isFocused();
            int x = getX(), y = getY();
            g.fill(x, y, x + width, y + height, lit ? 0xff4a3620 : hover ? 0xff3a2a1b : 0xff2f2116);
            g.drawBorder(x, y, width, height, lit || hover ? DarkUi.BRASS_HI : BRASS_LO);
            if (lit) g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, DarkUi.GOLD);
            String label = getMessage().getString();
            g.drawText(textRenderer, label, x + (width - textRenderer.getWidth(label)) / 2, y + (height - 8) / 2, lit || hover ? DarkUi.BRASS_HI : DarkUi.IVORY, false);
            DarkUi.focus(g, this);
        }
    }

    /** "Parchment | Real": the look shown is lit; a click switches. */
    final class ToggleButton extends ButtonWidget {
        ToggleButton(int x, int y, int w, int h) { super(x, y, w, h, Text.literal("Parchment | Real"), b -> toggleLook(), DEFAULT_NARRATION_SUPPLIER); }
        @Override public void renderButton(DrawContext g, int mx, int my, float delta) {
            int x = getX(), y = getY();
            boolean parch = lookParchment(), hover = isHovered() || isFocused();
            g.fill(x, y, x + width, y + height, 0xff2f2116);
            g.drawBorder(x, y, width, height, hover ? DarkUi.BRASS_HI : BRASS_LO);
            String a = "Parchment", sep = " | ", b = "Real";
            int tx = x + 6, ty = y + (height - 8) / 2;
            int wa = textRenderer.getWidth(a), ws = textRenderer.getWidth(sep);
            if (parch) g.fill(tx - 2, y + 2, tx + wa + 1, y + height - 2, 0xff4a3620); else g.fill(tx + wa + ws - 2, y + 2, tx + wa + ws + textRenderer.getWidth(b) + 1, y + height - 2, 0xff4a3620);
            g.drawText(textRenderer, a, tx, ty, parch ? DarkUi.BRASS_HI : DarkUi.MUTED, false);
            g.drawText(textRenderer, sep, tx + wa, ty, BRASS_LO, false);
            g.drawText(textRenderer, b, tx + wa + ws, ty, parch ? DarkUi.MUTED : DarkUi.BRASS_HI, false);
            DarkUi.focus(g, this);
        }
    }

    // ---------------------------------------------------------------- for the checks

    /** The screen centre of point i as the last frame drew it, or null. */
    int[] pointAt(int i) { for (var h : hits) if (h.kind().equals("point") && h.point() == i) return new int[] {h.cx(), h.cy()}; return null; }
    /** The centre of an ally army's plaque as the last frame drew it, or null. */
    int[] armyAt(String id) { for (var h : hits) if (h.kind().equals("ally") && id.equals(h.army())) return new int[] {h.cx(), h.cy()}; return null; }
    /** The centre of the open popup's row with these words, or null. */
    int[] popupRow(String row) {
        if (popup == null) return null;
        int i = popup.rows().indexOf(row);
        return i < 0 ? null : new int[] {popup.x() + popup.w() / 2, popup.rowY(i) + ROW / 2};
    }
    /** A whole left click (press and release in place) as the mouse would give it. */
    void clickAt(double x, double y) { mouseClicked(x, y, 0); mouseReleased(x, y, 0); }
}
