package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** The battle map (spec 2026-11-13 sections 0, 2, 4, 5 and 6; it replaces MapScreen): the whole field under a {@link MapView}, in the real
 *  look toned down ({@link BattleMapArt}), between two dark wood bars with brass edges (the War Table direction). Revision 2 (Kort,
 *  work/map-declutter-mockup.png): points are large lettered discs in the owner's colour with the name under them as outlined text; troops
 *  are one small type icon per group with its count beside it, a square filled in the team colour for your own, a dark ring for allies;
 *  names, jobs and health live in the hover tooltip; order arrows show only for selected own groups and the hovered group. The top bar
 *  holds the mode, both scores labelled "Your team" and "Enemy" with the held-point pips, and the clock; the bottom bar a row of the ping
 *  buttons and Fit over a row of control key chips; the glyph key is a foldable panel in the map's bottom-left corner. A left click without a drag picks, in
 *  order: a popup row, an armed ping or attack target, a point disc (the plan popup), an ally bot's icon (the order popup), an own group's
 *  icon (select). Right click moves the selected own groups. Scroll zooms, a drag past 4 px pans, Home fits, M and Esc close. */
public final class BattleMapScreen extends GravesAndGloryClient.ArmyScreen {
    static final int TOP = 20, ROW = 12, BUTTON_H = 14, DRAG = 4;
    // the War Table palette: dark oak, brass, parchment and ink
    static final int WOOD = 0xff2a1c13, WOOD_DARK = 0xff1d130c, WOOD_LINE = 0xff342317, BRASS_LO = 0xff6b5530, TABLE = 0xff170f0a;
    static final int PARCH = 0xffdccba4, INK = 0xff120e0a, PLATE = 0xe61a120b, TIP = 0xf21c140d;
    /** The mockup's colours: a neutral point, label text, the selected edge, the field with no grid. */
    static final int NEUTRAL = 0xffe4e0d4, LABEL = 0xfff6f0e2, SELECTED = 0xfffff0aa, PLAIN = 0xff2a2620;
    /** Point disc radius (the ink rim is one more) and the half size of a troop icon (13 px square). */
    static final int DISC = 8, ICON = 6;
    /** The plan jobs in the Job order (the count is the logic; the words are jobWord's, through the UI editor's gate). */
    static final String[] JOBS = {"Attack", "Defend", "Hold", "Follow"};
    static String jobWord(int job) {
        return switch (job) {
            case 0 -> UiText.t("map.job.attack", "Attack");
            case 1 -> UiText.t("map.job.defend", "Defend");
            case 2 -> UiText.t("map.job.hold", "Hold");
            default -> UiText.t("map.job.follow", "Follow");
        };
    }
    /** The words of a popup row, by its row key (the key is what a click acts on; the words are what the row shows). */
    static String rowText(String row) {
        return switch (row) {
            case "Attack" -> UiText.t("map.popup.attack", "Attack");
            case "Defend" -> UiText.t("map.popup.defend", "Defend");
            case "Hold" -> UiText.t("map.popup.hold", "Hold");
            case "Clear" -> UiText.t("map.popup.clear", "Clear");
            case "Ping here" -> UiText.t("map.popup.pingHere", "Ping here");
            case "Defend here" -> UiText.t("map.popup.defendHere", "Defend here");
            case "Hold here" -> UiText.t("map.popup.holdHere", "Hold here");
            case "Follow me" -> UiText.t("map.popup.followMe", "Follow me");
            default -> row;
        };
    }
    /** The glyph key's word for row i (KEY_WORDS order). */
    static String keyWord(int i) {
        return switch (i) {
            case 0 -> UiText.t("map.key.you", "You");
            case 1 -> UiText.t("map.key.yours", "Yours");
            case 2 -> UiText.t("map.key.ally", "Ally");
            case 3 -> UiText.t("map.key.enemy", "Enemy");
            case 4 -> UiText.t("map.key.fight", "Fight");
            default -> UiText.t("map.key.plan", "Plan");
        };
    }

    /** The screen that opened the map; M returns there (the game when the map key opened it). */
    final Screen parent;
    /** The view of the field; built on the first init and kept across resizes. */
    MapView view;
    /** The field size the view was built for; a different map's screen rebuilds it. */
    private int viewHx, viewHz;
    /** The map area: the screen between the bars. */
    int ax, ay, aw, ah, bottom;
    /** The glyph key panel in the map area's bottom-left corner as the last frame drew it {x, y, w, h}; open or folded to its header. */
    int[] keyBox = {0, 0, 0, 0};
    static boolean keyOpen = true;
    /** The x of the brass rule between the ping buttons and Fit; the top bar's side labels as last drawn ("left|right"). */
    int fitRule;
    String drawnSides = "";
    /** The ping kind armed by a ping button ("" none), and the army whose Attack waits for a point or a place (null none). */
    String pingMode = "", attackArmy;
    String attackName = "";
    Popup popup;
    /** What the last frame drew: the texture ("real", "fallback" or "plain") and every marker's hit box. */
    String drawnTexture = "none";
    Identifier drawnId;
    final List<Hit> hits = new ArrayList<>();
    /** Boxes the frame's labels and icons cover: the popup is placed off them. */
    final List<int[]> taken = new ArrayList<>();
    /** Boxes {x0, y0, x1, y1} of the troop icons and their counts drawn this frame: a new icon nudges off one it would cover. */
    final List<int[]> icons = new ArrayList<>();
    /** What the mouse was over at the frame's start (from the last frame's hits): an own group, an ally army and its group. */
    String hoverOwn, hoverArmy, hoverAllyGroup;
    // press state for the click-or-drag rule
    private double pressX, pressY, lastX, lastY;
    private int pressButton = -1;
    private boolean dragging;

    /** The UI editor's sample (UiSamples.battleMap): each frame draws over the sample battle, it never closes itself, and no click orders, plans, pings or selects. */
    boolean sample;

    BattleMapScreen(Screen parent) { super("Battlefield map"); this.parent = parent; }

    /** A marker's box and what a click or a hover there means. kind: point, ally, own, enemy, ping, you. A point's box is its disc. */
    record Hit(String kind, int x0, int y0, int x1, int y1, int point, String army, String group, List<String> tip) {
        boolean has(double x, double y) {
            if (kind.equals("point")) { double r = (x1 - x0) / 2.0; return Math.hypot(x - (x0 + r), y - (y0 + r)) <= r; }
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
        /** Stacking for a hover: an icon over the you arrow over a ping over a sighting over a point. */
        int rank() { return switch (kind) { case "ally", "own" -> 4; case "you" -> 3; case "ping" -> 2; case "enemy" -> 1; default -> 0; }; }
        int cx() { return (x0 + x1) / 2; }
        int cy() { return (y0 + y1) / 2; }
    }
    /** An open popup: a title row and its option rows. kind 0 is a point (point), 1 an ally bot army (army). */
    record Popup(int kind, int point, String army, String title, List<String> rows, int active, double wx, double wz, int x, int y, int w) {
        int h() { return ROW + 3 + rows.size() * ROW + 2; }
        boolean has(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h(); }
        int rowY(int i) { return y + ROW + 3 + i * ROW; }
    }

    // ---------------------------------------------------------------- layout

    /** The map's controls as key chips {key, word}, the keys named by the player's own bindings (a mouse button as LMB, RMB or MMB). */
    public static String[][] chips() { return new String[][] {{GravesAndGloryClient.hint("hit"), UiText.t("map.chip.select", "Select/plan")}, {GravesAndGloryClient.hint("use"), UiText.t("map.chip.move", "Move")}, {UiText.t("map.chip.wheel", "Wheel"), UiText.t("map.chip.zoom", "Zoom")}, {UiText.t("map.chip.drag", "Drag"), UiText.t("map.chip.pan", "Pan")}}; }
    /** The controls row as text, as the bottom bar draws it: "[LMB] Select/plan  [RMB] Move  [Wheel] Zoom  [Drag] Pan". */
    public static String legend() {
        StringBuilder b = new StringBuilder();
        for (var c : chips()) b.append(b.length() == 0 ? "" : "  ").append('[').append(c[0]).append("] ").append(c[1]);
        return b.toString();
    }
    /** The glyph key's rows, top to bottom, each beside the map's own icon at map size (the row count and the default words; the words drawn are keyWord's); and each row's height. */
    static final String[] KEY_WORDS = {"You", "Yours", "Ally", "Enemy", "Fight", "Plan"};
    static final int[] KEY_ROWS = {14, 16, 16, 11, 11, 18};
    static final int KEY_HEAD = 12, CHIP_H = 11, CHIP_PAD = 3, CHIP_GAP = 4, CHIP_SEP = 11, FIT_SEP = 11;

    int chipsWidth() {
        int w = -CHIP_SEP;
        for (var c : chips()) w += textRenderer.getWidth(c[0]) + 2 * CHIP_PAD + CHIP_GAP + textRenderer.getWidth(c[1]) + CHIP_SEP;
        return w;
    }

    @Override protected void init() {
        bottom = 3 + BUTTON_H + 4 + CHIP_H + 4; // the buttons' row, then the key chips' row
        ax = 0; ay = TOP; aw = width; ah = Math.max(20, height - TOP - bottom);
        popup = null; // a resize moves everything under it
        ensureView();
        // the bottom bar's buttons in one row: the pings, a brass rule, then Fit, centred
        List<String> labels = new ArrayList<>();
        for (var p : Spec.PINGS) labels.add(p.label());
        labels.add(UiText.t("map.button.fit", "Fit"));
        int gap = 4, total = -gap + FIT_SEP - gap;
        for (String l : labels) total += textRenderer.getWidth(l) + 12 + gap;
        int x = Math.max(4, (width - total) / 2), y = height - bottom + 3;
        for (int i = 0; i < labels.size(); i++) {
            String l = labels.get(i);
            int w = textRenderer.getWidth(l) + 12;
            if (i < Spec.PINGS.length) { String id = Spec.PINGS[i].id(); addDrawableChild(new WoodButton(x, y, w, BUTTON_H, l, () -> id.equals(pingMode), b -> { pingMode = pingMode.equals(id) ? "" : id; attackArmy = null; popup = null; })); }
            else addDrawableChild(new WoodButton(x, y, w, BUTTON_H, l, () -> false, b -> fit()));
            x += w + gap;
            if (i == Spec.PINGS.length - 1) { fitRule = x + (FIT_SEP - gap) / 2 - 1; x += FIT_SEP - gap; }
        }
    }

    /** The view for this field: built on the first call and again when the field's size changes (another map). */
    void ensureView() {
        int hx = Field.halfX(), hz = Field.halfZ();
        if (view == null || hx != viewHx || hz != viewHz) { view = new MapView(hx, hz); viewHx = hx; viewHz = hz; }
        view.layout(ax, ay, aw, ah);
    }

    @Override public void tick() { if (!sample && !GravesAndGloryClient.battleActive()) client.setScreen(null); } // the round ended under the screen

    void fit() { view.fit(); popup = null; }

    // ---------------------------------------------------------------- input

    boolean inMap(double x, double y) { return x >= ax && x < ax + aw && y >= ay && y < ay + ah; }
    boolean inKey(double x, double y) { return x >= keyBox[0] && x < keyBox[0] + keyBox[2] && y >= keyBox[1] && y < keyBox[1] + keyBox[3]; }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (GravesAndGloryClient.mapKey.matchesKey(key, scan)) { client.setScreen(parent); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && (popup != null || attackArmy != null || !pingMode.isEmpty())) { popup = null; attackArmy = null; pingMode = ""; return true; }
        if (key == GLFW.GLFW_KEY_HOME) { fit(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double amount) { // 1.20.2+: the wheel's two axes; 1.20.1 handed the vertical one
        if (!inMap(mx, my)) return false;
        popup = null;
        view.zoomAt(mx, my, amount);
        return true;
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if ((popup == null || !popup.has(mx, my)) && super.mouseClicked(mx, my, button)) { popup = null; return true; } // a bar button acts, and closes an open popup
        if (popup == null && inKey(mx, my)) { if (button == 0 && my < keyBox[1] + KEY_HEAD) keyOpen = !keyOpen; return true; } // the key's header folds it; the key hides the map under it
        if (!inMap(mx, my) && popup == null) return false;
        if (button == 1) {
            if (popup != null || attackArmy != null || !pingMode.isEmpty()) { popup = null; attackArmy = null; pingMode = ""; return true; } // only cancels
            double wx = view.worldX(mx), wz = view.worldZ(my);
            if (!sample && inMap(mx, my) && inField(wx, wz) && client.player != null) CommandSelection.issueSelected("hold", CommandMode.formation, new Vec3d(wx, 65, wz), client.player.getYaw(), -1);
            return true;
        }
        if (button == 0 || button == 2) { pressX = lastX = mx; pressY = lastY = my; pressButton = button; dragging = false; return true; }
        return false;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button != pressButton || pressButton < 0) return super.mouseDragged(mx, my, button, dx, dy);
        if (!dragging && Math.hypot(mx - pressX, my - pressY) > DRAG) { dragging = true; popup = null; view.pan(mx - pressX, my - pressY); lastX = mx; lastY = my; return true; }
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

    /** A left click without a drag: popup row, armed ping or attack, point disc, ally bot icon, own group icon. */
    void click(double mx, double my) {
        if (sample) return; // the sample sends and selects nothing
        if (popup != null) {
            var p = popup;
            popup = null;
            if (mx >= p.x() && mx < p.x() + p.w() && my >= p.rowY(0) && my < p.rowY(p.rows().size())) choose(p, p.rows().get((int) ((my - p.rowY(0)) / ROW)));
            return; // a click elsewhere only closes it
        }
        if (!inMap(mx, my)) return;
        double wx = view.worldX(mx), wz = view.worldZ(my);
        if (!pingMode.isEmpty()) { if (inField(wx, wz)) { CommandMode.ping(pingMode, wx, wz); pingMode = ""; } return; } // off the sheet: ignored, still armed
        Hit point = hit("point", mx, my), ally = hit("ally", mx, my), own = hit("own", mx, my);
        if (attackArmy != null) {
            if (!inField(wx, wz)) return; // off the sheet: ignored, still armed
            if (point != null && CampaignClient.showsPoints()) BattleMapClient.sendPlan(1, point.point(), attackArmy, BattlePlan.Job.ATTACK.ordinal(), 0, 0);
            else { double[] at = arena(wx, wz); BattleMapClient.sendPlan(1, -1, attackArmy, BattlePlan.Job.ATTACK.ordinal(), at[0], at[1]); } // a mode with no points: the place under the click
            attackArmy = null;
            return;
        }
        if (point != null) { openPointPopup(point.point(), point.cx(), point.cy()); return; }
        if (ally != null && ally.army() != null) {
            var a = army(ally.army());
            if (a != null && takesOrder(a)) { openArmyPopup(a, ally.cx(), ally.cy()); return; }
        }
        if (own != null) CommandSelection.select(own.group(), hasShiftDown());
    }

    /** True for a world point inside the field's sheet. */
    static boolean inField(double wx, double wz) { return wx >= -Field.halfX() && wx <= Field.halfX() + 1 && wz >= -Field.halfZ() && wz <= Field.halfZ() + 1; }

    /** A place pulled inside the arena the server accepts orders for (10 blocks in from the edge). */
    static double[] arena(double wx, double wz) {
        double mx = Field.halfX() - 10.5, mz = Field.halfZ() - 10.5;
        return new double[] {Math.max(-mx, Math.min(mx, wx)), Math.max(-mz, Math.min(mz, wz))};
    }

    /** True for an ally bot that takes an order: not one whose goal the mode sets (Siege, Convoy, Survival), not a player. */
    static boolean takesOrder(BattleMapClient.Army a) { return a.bot() && !a.aimed(); }

    Hit hit(String kind, double x, double y) {
        for (int i = hits.size() - 1; i >= 0; i--) { var h = hits.get(i); if (h.kind().equals(kind) && h.has(x, y)) return h; }
        return null;
    }

    /** The topmost marker under (x, y) in the last frame, or null. */
    Hit top(double x, double y) {
        Hit h = null;
        for (int i = hits.size() - 1; i >= 0; i--) { var c = hits.get(i); if (c.has(x, y) && (h == null || c.rank() > h.rank())) h = c; }
        return h;
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
        if (!takesOrder(a)) return; // the server would refuse it
        int active = -1;
        for (var o : orders()) if (o.army().equals(a.id())) active = o.job(); // the rows follow the Job order
        double[] c = centre(a);
        popup = place(new Popup(1, -1, a.id(), a.name(), List.of("Attack", "Defend here", "Hold here", "Follow me", "Clear"), active, c[0], c[1], x, y, 0));
    }

    /** Sizes the popup to its words and sets it beside the anchor, inside the map area. */
    Popup place(Popup p) {
        int w = textRenderer.getWidth(p.title()) + 12;
        for (String r : p.rows()) w = Math.max(w, textRenderer.getWidth(rowText(r)) + 16);
        int h = ROW + 3 + p.rows().size() * ROW + 2;
        // above-right, above-left, below-right, below-left of the anchor: the first spot that covers no label or icon drawn last frame
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
            case "Defend here" -> { double[] at = arena(p.wx(), p.wz()); BattleMapClient.sendPlan(1, -1, p.army(), BattlePlan.Job.DEFEND.ordinal(), at[0], at[1]); }
            case "Hold here" -> { double[] at = arena(p.wx(), p.wz()); BattleMapClient.sendPlan(1, -1, p.army(), BattlePlan.Job.HOLD.ordinal(), at[0], at[1]); }
            case "Follow me" -> BattleMapClient.sendPlan(1, -1, p.army(), BattlePlan.Job.FOLLOW.ordinal(), 0, 0);
            case "Clear" -> BattleMapClient.sendPlan(1, -1, p.army(), -1, 0, 0);
            default -> {}
        }
    }

    // ---------------------------------------------------------------- data

    String me() { return client.player == null ? "" : client.player.getUuid().toString(); }

    List<BattleMapClient.Army> allies() {
        var s = BattleMapClient.current();
        if (s == null) return List.of();
        List<BattleMapClient.Army> out = new ArrayList<>();
        String me = me();
        for (var a : s.armies()) if (!a.id().equals(me) && !a.groups().isEmpty()) out.add(a);
        return out;
    }

    BattleMapClient.Army army(String id) { for (var a : allies()) if (a.id().equals(id)) return a; return null; }
    List<BattleMapClient.Order> orders() { var s = BattleMapClient.current(); return s == null ? List.of() : s.orders(); }
    BattleMapClient.PointPlan pointPlan(int i) { var s = BattleMapClient.current(); if (s == null) return null; for (var p : s.points()) if (p.point() == i) return p; return null; }

    /** Where the player this army follows stands as the client knows it (the own player, a player in view, a player's army on the map), or null. */
    double[] followTarget(BattleMapClient.Army a) {
        String setter = null;
        for (var o : orders()) if (o.army().equals(a.id()) && o.job() == BattlePlan.Job.FOLLOW.ordinal()) setter = o.setter();
        if (setter == null || setter.isEmpty() || client == null || client.player == null) return null;
        if (setter.equals(client.player.getGameProfile().getName())) return new double[] {client.player.getX(), client.player.getZ()};
        if (client.world != null) for (var pl : client.world.getPlayers()) if (setter.equals(pl.getGameProfile().getName())) return new double[] {pl.getX(), pl.getZ()};
        for (var o : allies()) if (!o.bot() && setter.equals(o.name())) return centre(o);
        return null;
    }

    static int count(BattleMapClient.Army a) { int n = 0; for (var g : a.groups()) n += g.count(); return n; }
    static double[] centre(BattleMapClient.Army a) {
        double x = 0, z = 0, n = 0;
        for (var g : a.groups()) { x += g.x() * g.count(); z += g.z() * g.count(); n += g.count(); }
        return n == 0 ? new double[] {0, 0} : new double[] {x / n, z / n};
    }

    static String groupName(String id) { return id.isEmpty() ? "Group" : Character.toUpperCase(id.charAt(0)) + id.substring(1); }
    /** The troop type's name as the groups table has it ("ranged" is Archers), else the id capitalised. */
    static String typeName(String id) { for (var r : Spec.GROUPS) if (r.id().equals(id)) return r.label(); return groupName(id); }

    String pointName(int i) { return i >= 0 && i < Spec.POINTS.length ? Spec.POINTS[i].name() : UiText.t("map.job.aPoint", "a point"); }

    String jobText(int job, int point, boolean direct) {
        if (job < 0 || job >= JOBS.length) return UiText.t("map.job.ownPlan", "Its own plan");
        String what = job == 3 ? UiText.t("map.job.followingYou", "Following you") : point >= 0 ? UiText.t("map.job.atPoint", "{job} {point}", "job", jobWord(job), "point", pointName(point)) : UiText.t("map.job.atPlace", "{job} a place", "job", jobWord(job));
        return direct ? UiText.t("map.job.order", "{plan} (order)", "plan", what) : UiText.t("map.job.teamPlan", "{plan} (team plan)", "plan", what);
    }

    /** Where an ally group is heading: the plan's place, the one it follows where known, else the group's own goal; null when unknown. */
    double[] allyGoal(BattleMapClient.Army a, BattleMapClient.Group gr) {
        if (a.job() == BattlePlan.Job.FOLLOW.ordinal()) return followTarget(a);
        if (a.job() >= 0) return new double[] {a.planX(), a.planZ()};
        return new double[] {gr.targetX(), gr.targetZ()};
    }

    // ---------------------------------------------------------------- drawing

    @Override public void render(DrawContext g, int mx, int my, float delta) {
        if (sample && !UiSamples.in()) { UiSamples.withBattle(() -> render(g, mx, my, delta)); return; }
        ensureView(); // re-clamps when the window changed under a live screen; rebuilt for another field
        clipX0 = ax; clipY0 = ay; clipX1 = ax + aw; clipY1 = ay + ah;
        hoverOwn = hoverArmy = hoverAllyGroup = null;
        if (popup == null && pressButton < 0 && inMap(mx, my) && !inKey(mx, my)) { // what the mouse is over, by the last frame's boxes
            Hit h = top(mx, my);
            if (h != null && h.kind().equals("own")) hoverOwn = h.group();
            if (h != null && h.kind().equals("ally")) { hoverArmy = h.army(); hoverAllyGroup = h.group(); }
        }
        hits.clear();
        taken.clear();
        icons.clear();
        g.fill(0, 0, width, height, TABLE);
        g.enableScissor(ax, ay, ax + aw, ay + ah);
        try {
            drawField(g);
            drawOrderLines(g);
            drawPoints(g);
            drawSightings(g);
            keepYouClear();
            drawAllies(g);
            drawOwn(g);
            drawPings(g);
            drawYou(g);
            drawKey(g);
        } finally { g.disableScissor(); }
        drawTopBar(g);
        drawBottomBar(g);
        super.render(g, mx, my, delta); // the bar buttons
        if (attackArmy != null) banner(g, UiText.t("map.banner.attack", "Click a point or a place for {army} to attack / Esc cancels", "army", attackName));
        else if (!pingMode.isEmpty()) banner(g, UiText.t("map.banner.ping", "Click the map to ping / Esc cancels"));
        if (popup != null) drawPopup(g, mx, my);
        else if (inMap(mx, my) && !inKey(mx, my) && pressButton < 0) tooltip(g, mx, my);
    }

    @Override public void renderBackground(DrawContext g, int mx, int my, float delta) {} // 1.21: Screen.render draws this (blur and dimming) under the widgets; this screen draws its own

    void drawField(DrawContext g) {
        int hx = Field.halfX(), hz = Field.halfZ();
        Identifier tex = BattleMapArt.real();
        var grid = BattleMapClient.grid;
        if (grid != null && !grid.map.equals(Spec.MAPS[Math.max(0, Math.min(Spec.MAPS.length - 1, CampaignMaps.selected))].id())) grid = null; // another map's field: the fallback
        int x0, y0, x1, y1;
        if (tex != null && grid != null) { // the texture is toned down when painted (BattleMapPaint.tone)
            x0 = (int) Math.round(view.screenX(grid.minX)); y0 = (int) Math.round(view.screenY(grid.minZ));
            x1 = (int) Math.round(view.screenX(grid.minX + grid.width)); y1 = (int) Math.round(view.screenY(grid.minZ + grid.depth));
            int[] s = BattleMapArt.size();
            g.drawTexture(tex, x0, y0, x1 - x0, y1 - y0, 0f, 0f, s[0], s[1], s[0], s[1]);
            drawnTexture = "real";
            drawnId = tex;
        } else {
            x0 = (int) Math.round(view.screenX(-hx)); y0 = (int) Math.round(view.screenY(-hz));
            x1 = (int) Math.round(view.screenX(hx + 1)); y1 = (int) Math.round(view.screenY(hz + 1));
            if (GravesAndGloryClient.mapTexture != null) { // the old map colours, dimmed as the real look: darkened, then a grey wash for the colour
                float b = BattleMapPaint.BRIGHTNESS;
                RenderSystem.setShaderColor(b, b, b, 1f);
                g.drawTexture(GravesAndGloryClient.mapTexture, x0, y0, x1 - x0, y1 - y0, 0f, 0f, 281, 201, 281, 201);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                g.fill(x0, y0, x1, y1, 0x40585450);
                drawnTexture = "fallback"; drawnId = GravesAndGloryClient.mapTexture;
            } else {
                g.fill(x0, y0, x1, y1, PLAIN);
                drawnTexture = "plain"; drawnId = null;
            }
        }
        // the brass frame round the sheet
        g.drawBorder(x0 - 2, y0 - 2, x1 - x0 + 4, y1 - y0 + 4, BRASS_LO);
        g.drawBorder(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, DarkUi.BRASS);
    }

    int sx(double wx) { return (int) Math.round(view.screenX(wx)); }
    int sy(double wz) { return (int) Math.round(view.screenY(wz)); }

    /** Your side's colour for the troop icons, pennants and arrows. There colour carries no meaning alone: everything on that layer is
     *  your side and the shape says the rest (a filled square yours, a ring an ally); the legend shows your side's mark after "Yours" (oursMark),
     *  and a point's owner and challenger, where colour is the state, get their marks beside the disc (drawPoints). */
    int ours() { return DarkUi.teamColor(GravesAndGloryClient.team); }
    /** Your side's 7x7 mark. */
    void oursMark(DrawContext g, int x, int y) { DarkUi.teamMark(g, GravesAndGloryClient.team, x, y); }

    /** Order arrows: only for the selected own groups and the hovered own group or ally group (Revision 2). */
    void drawOrderLines(DrawContext g) {
        int col = ours();
        for (var gr : CommandMode.groups) {
            if (gr.count() <= 0 || !(CommandSelection.isSelected(gr.id()) || gr.id().equals(hoverOwn))) continue;
            arrowTo(g, sx(gr.cx()), sy(gr.cz()), sx(gr.x()), sy(gr.z()), col);
        }
        if (hoverArmy != null) {
            var a = army(hoverArmy);
            if (a != null) for (var gr : a.groups()) {
                if (!gr.group().equals(hoverAllyGroup)) continue;
                double[] t = allyGoal(a, gr);
                if (t != null) arrowTo(g, sx(gr.x()), sy(gr.z()), sx(t[0]), sy(t[1]), col);
            }
        }
    }

    /** An arrow from an icon to a goal, stopping short of a point disc it ends on; nothing for a goal under the icon. */
    void arrowTo(DrawContext g, int x0, int y0, int x1, int y1, int color) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        if (len < ICON * 2 + 4) return;
        for (var p : Spec.POINTS) {
            int px = sx(p.x() + .5), py = sy(p.z() + .5);
            if (Math.hypot(x1 - px, y1 - py) <= DISC + 2) { double k = (len - (DISC + 4)) / len; if (k <= 0) return; x1 = x0 + (int) Math.round((x1 - x0) * k); y1 = y0 + (int) Math.round((y1 - y0) * k); break; }
        }
        arrow(g, x0, y0, x1, y1, color);
    }

    void drawPoints(DrawContext g) {
        var pts = Spec.POINTS;
        int mine = GravesAndGloryClient.team;
        for (int i = 0; i < pts.length; i++) {
            var p = pts[i];
            int x = sx(p.x() + .5), y = sy(p.z() + .5);
            int owner = GravesAndGloryClient.ownerAt(i), progress = GravesAndGloryClient.progressAt(i), ch = GravesAndGloryClient.challengerAt(i);
            boolean contested = GravesAndGloryClient.contestedAt(i);
            double r = p.radius() * view.scale();
            if (r >= 16) dottedCircle(g, x, y, (int) r, 0x88e4e0d4);
            // the capture ring: progress in the challenger's colour on an ink track; a hatched ring while contested
            if (contested) { arc(g, x, y, DISC + 3, 1, DarkUi.CONTEST, true); arc(g, x, y, DISC + 4, 1, DarkUi.CONTEST, true); }
            else if (progress > 0 && p.captureSeconds() > 0) {
                double part = Math.min(1, progress / (double) p.captureSeconds());
                for (int k = 2; k <= 5; k++) arc(g, x, y, DISC + k, 1, 0xaa120e0a, false);
                arc(g, x, y, DISC + 3, part, DarkUi.teamColor(ch), false);
                arc(g, x, y, DISC + 4, part, DarkUi.teamColor(ch), false);
                DarkUi.teamMark(g, ch, x - DISC - 14, y - 3); // the challenger's mark left of the ring
            }
            disc(g, x, y, DISC + 1, INK);
            String name = p.name();
            int nw = textRenderer.getWidth(name);
            disc(g, x, y, DISC, owner < 0 ? NEUTRAL : DarkUi.teamColor(owner));
            if (owner >= 0) DarkUi.teamMark(g, owner, x - nw / 2 - 10, y + DISC + 4); // the owner's mark before the name
            String id = p.id();
            g.drawText(textRenderer, id, x - (textRenderer.getWidth(id) - 1) / 2, y - 3, INK, false);
            outlined(g, name, x - nw / 2, y + DISC + 4, LABEL);
            taken.add(new int[] {x - nw / 2 - 1, y + DISC + 3, nw + 2, 10});
            taken.add(new int[] {x - DISC - 1, y - DISC - 1, 2 * DISC + 3, 2 * DISC + 3});
            icons.add(new int[] {x - DISC - 1, y - DISC - 1, x + DISC + 2, y + DISC + 2}); // troop icons nudge off the disc
            icons.add(new int[] {x - nw / 2 - 3, y + DISC + 3, x + nw / 2 + 3, y + DISC + 13}); // and off the name, with a gap
            var plan = pointPlan(i);
            if (plan != null) pennant(g, x + DISC - 3, y - DISC + 3, plan.job(), ours());
            List<String> tip = new ArrayList<>();
            tip.add(p.name() + " (" + id + ")");
            tip.add(owner < 0 ? UiText.t("map.point.neutral", "Neutral") : owner == mine ? UiText.t("map.point.heldYours", "Held by your side") : UiText.t("map.point.heldEnemy", "Held by the enemy"));
            if (contested) tip.add(UiText.t("map.point.contested", "Contested"));
            else if (progress > 0 && p.captureSeconds() > 0) { int pct = Math.min(100, 100 * progress / p.captureSeconds()); tip.add(ch == mine ? UiText.t("map.point.takingYours", "Your side is taking it: {percent}%", "percent", pct) : UiText.t("map.point.takingEnemy", "The enemy is taking it: {percent}%", "percent", pct)); }
            if (plan != null) tip.add(UiText.t("map.point.plan", "Plan: {job}, set by {setter}", "job", jobWord(Math.max(0, Math.min(3, plan.job()))), "setter", plan.setter()));
            else if (CampaignClient.showsPoints()) tip.add(UiText.t("map.point.noPlan", "No plan: click to plan"));
            hits.add(new Hit("point", x - DISC - 1, y - DISC - 1, x + DISC + 2, y + DISC + 2, i, null, null, tip));
        }
    }

    void drawSightings(DrawContext g) {
        int memory = Spec.SCOUTING.length > 0 ? Math.max(1, Spec.SCOUTING[0].memorySeconds() * 20) : 400;
        for (var s : GravesAndGloryClient.sightings) {
            int x = sx(s.x()), y = sy(s.z());
            boolean fresh = s.age() < 20;
            int alpha = fresh ? 255 : Math.max(60, 220 - 160 * s.age() / memory);
            glyph(g, fresh ? TRI : TRI_OPEN, x - 3, y - 4, fresh ? DarkUi.MAP_ENEMY : (alpha << 24) | (DarkUi.MAP_ENEMY & 0xffffff), fresh ? INK : 0);
            hits.add(new Hit("enemy", x - 4, y - 4, x + 5, y + 4, -1, null, null, List.of(UiText.t("map.sighting.enemy", "Enemy"), fresh ? UiText.t("map.sighting.now", "Seen now") : UiText.t("map.sighting.ago", "Seen {seconds} s ago", "seconds", Math.max(1, s.age() / 20)))));
        }
    }

    void drawAllies(DrawContext g) {
        int col = ours();
        if (BattleMapClient.current() == null) { // no battlemap_v1 yet: the state_v10 marks, as outlined icons with no type
            for (var m : GravesAndGloryClient.markers) if (!m.own()) {
                int[] c = nudge(sx(m.x()), sy(m.z()), 0);
                allyIcon(g, c[0], c[1], DOT, col);
                if (m.fighting()) glyph(g, SWORDS, c[0] + ICON + 2, c[1] - ICON - 5, DarkUi.WARN, INK);
                hits.add(new Hit("ally", c[0] - ICON, c[1] - ICON, c[0] + ICON + 1, c[1] + ICON + 1, -1, null, null, List.of(UiText.t("map.ally.troops", "Allied troops"))));
            }
            return;
        }
        for (var a : allies()) {
            boolean named = a.name().contains(" bot "); // "normal bot 2" says bot already (spec call 12)
            String job = a.retreating() ? UiText.t("map.ally.fallingBack", "Falling back") : a.aimed() ? UiText.t("map.ally.modePlan", "Follows the mode's own plan") : a.bot() ? jobText(a.job(), a.point(), a.direct()) : null;
            for (var gr : a.groups()) {
                if (gr.count() <= 0) continue;
                int[] c = nudge(sx(gr.x()), sy(gr.z()), textRenderer.getWidth(String.valueOf(gr.count())));
                int x = c[0], y = c[1];
                allyIcon(g, x, y, typeGlyph(gr.group()), col);
                countLabel(g, x, y, gr.count());
                if (gr.fighting()) glyph(g, SWORDS, x + ICON + 2, y - ICON - 5, DarkUi.WARN, INK);
                List<String> tip = new ArrayList<>();
                tip.add(a.bot() ? named ? a.name() : UiText.t("map.ally.nameBot", "{name} (bot)", "name", a.name()) : UiText.t("map.ally.namePlayer", "{name} (player)", "name", a.name()));
                tip.add(UiText.t("map.ally.troopCount", "{type}: {count} troops (army {army})", "type", typeName(gr.group()), "count", gr.count(), "army", count(a)));
                tip.add(UiText.t("map.ally.health", "Health {percent}%", "percent", Math.round(100 * Math.max(0, Math.min(1, gr.health())))));
                if (job != null) tip.add(job);
                if (gr.fighting()) tip.add(UiText.t("map.ally.inCombat", "In combat"));
                if (takesOrder(a)) tip.add(UiText.t("map.ally.clickOrder", "Click to give an order"));
                hits.add(new Hit("ally", x - ICON, y - ICON, x + ICON + 1, y + ICON + 1, -1, a.id(), gr.group(), tip));
            }
        }
    }

    void drawOwn(DrawContext g) {
        int col = ours();
        for (var gr : CommandMode.groups) {
            if (gr.count() <= 0) continue; // a group with nobody left has no place on the map
            int[] c = nudge(sx(gr.cx()), sy(gr.cz()), textRenderer.getWidth(String.valueOf(gr.count())));
            int x = c[0], y = c[1];
            boolean selected = CommandSelection.isSelected(gr.id());
            icon(g, x, y, typeGlyph(gr.id()), col, selected);
            countLabel(g, x, y, gr.count());
            float hp = Math.max(0, Math.min(1, gr.health()));
            if (hp < .95f) { // a thin health bar under a hurt group only
                g.fill(x - ICON, y + ICON + 2, x + ICON + 1, y + ICON + 4, INK);
                g.fill(x - ICON + 1, y + ICON + 2, x - ICON + 1 + Math.round((2 * ICON - 1) * hp), y + ICON + 3, hp < .35f ? DarkUi.ALARM_TEXT : DarkUi.IVORY);
            }
            if (gr.fighting()) glyph(g, SWORDS, x + ICON + 2, y - ICON - 5, DarkUi.WARN, INK);
            List<String> tip = new ArrayList<>();
            tip.add(UiText.t("map.own.troopCount", "{type}: {count} troops", "type", typeName(gr.id()), "count", gr.count()));
            tip.add(UiText.t("map.own.health", "Health {percent}%", "percent", Math.round(100 * hp)));
            tip.add(UiText.t("map.own.order", "Order: {order}", "order", gr.order()));
            if (gr.fighting()) tip.add(UiText.t("map.own.inCombat", "In combat"));
            hits.add(new Hit("own", x - ICON, y - ICON, x + ICON + 1, y + ICON + 1, -1, null, gr.id(), tip));
        }
    }

    /** Grid steps {column, row} out to 4 each way, nearest first (rows count a little less than columns, so a crowd stacks up and down). */
    static final int[][] STEPS;
    static {
        List<int[]> s = new ArrayList<>();
        for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) s.add(new int[] {i, j});
        s.sort(java.util.Comparator.comparingDouble((int[] v) -> Math.hypot(v[0] * 1.6, v[1])).thenComparingInt(v -> Math.abs(v[0])).thenComparingInt(v -> v[1] < 0 ? 0 : 1));
        STEPS = s.toArray(new int[0][]);
    }

    /** Where a troop icon goes: its own spot, or the nearest grid step off the icons and counts already drawn (and the you arrow), else
     *  its own spot anyway. The box is the icon with its count label (labelW px) beside it. */
    int[] nudge(int x, int y, int labelW) {
        int l = ICON + 1, r = ICON + 5 + labelW, t = ICON + 1, b = ICON + 2, dy = 2 * ICON + 3, dx = l + r + 2;
        int[] at = {x, y};
        for (int[] st : STEPS) {
            int cx = x + st[0] * dx, cy = y + st[1] * dy;
            boolean clear = true;
            for (int[] o : icons) if (cx - l < o[2] && cx + r > o[0] && cy - t < o[3] && cy + b > o[1]) { clear = false; break; }
            if (clear) { at = new int[] {cx, cy}; break; }
        }
        icons.add(new int[] {at[0] - l, at[1] - t, at[0] + r, at[1] + b});
        taken.add(new int[] {at[0] - l - 1, at[1] - t - 1, l + r + 2, t + b + 2});
        return at;
    }

    /** Your own troop icon: an ink square filled with the team colour and an ink glyph; a selected one gets a light edge with a gap. */
    void icon(DrawContext g, int x, int y, String[] glyph, int color, boolean selected) {
        if (selected) g.drawBorder(x - ICON - 2, y - ICON - 2, 2 * ICON + 5, 2 * ICON + 5, SELECTED);
        g.fill(x - ICON, y - ICON, x + ICON + 1, y + ICON + 1, INK);
        g.fill(x - ICON + 1, y - ICON + 1, x + ICON, y + ICON, color);
        glyph(g, glyph, x - 3, y - 3, INK, 0);
    }

    /** An ally's troop icon, round where yours is square (Revision 2 follow-up): an ink rim, a thin team-colour ring, a dark inside and the
     *  same type glyph in the team colour. Its hit box is the same 13 px as yours. */
    void allyIcon(DrawContext g, int x, int y, String[] glyph, int color) {
        disc(g, x, y, ICON + 1, INK);
        disc(g, x, y, ICON, color);
        disc(g, x, y, ICON - 1, INK);
        glyph(g, glyph, x - 3, y - 3, color, 0);
    }

    /** Reserves the "you" arrow's box before the troop icons are placed, so no icon or count sits on it. */
    void keepYouClear() {
        var p = client.player;
        if (p == null) return;
        int x = sx(p.getX()), y = sy(p.getZ());
        icons.add(new int[] {x - 8, y - 8, x + 9, y + 9});
    }

    void countLabel(DrawContext g, int x, int y, int n) { outlined(g, String.valueOf(n), x + ICON + 3, y - 1, LABEL); }

    /** Text with a 1 px ink outline all round and no box. */
    void outlined(DrawContext g, String s, int x, int y, int color) {
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (dx != 0 || dy != 0) g.drawText(textRenderer, s, x + dx, y + dy, INK, false);
        g.drawText(textRenderer, s, x, y, color, false);
    }

    void drawPings(DrawContext g) {
        for (var p : CommandMode.pings) {
            int x = sx(p.x()), y = sy(p.z());
            ring(g, x, y, 7, INK);
            ring(g, x, y, 6, DarkUi.PING);
            ring(g, x, y, 5, INK);
            outlined(g, p.kind().toUpperCase(), x + 10, y - 3, DarkUi.PING);
            hits.add(new Hit("ping", x - 6, y - 6, x + 7, y + 7, -1, null, null, List.of(UiText.t("map.ping.tip", "Ping: {kind}", "kind", p.kind()))));
        }
    }

    void drawYou(DrawContext g) {
        var p = client.player;
        if (p == null) return;
        double yaw = Math.toRadians(p.getYaw()), dx = -Math.sin(yaw), dz = Math.cos(yaw);
        int x = sx(p.getX()), y = sy(p.getZ());
        triangle(g, x, y, dx, dz, 7.5, 4.5, INK);
        triangle(g, x, y, dx, dz, 5.5, 3, DarkUi.MAP_YOU);
        hits.add(new Hit("you", x - 4, y - 4, x + 5, y + 5, -1, null, null, List.of(UiText.t("map.you.tip", "You"))));
    }

    void drawTopBar(DrawContext g) {
        wood(g, 0, 0, width, TOP);
        g.fill(0, TOP - 2, width, TOP - 1, DarkUi.BRASS);
        g.fill(0, TOP - 1, width, TOP, BRASS_LO);
        String mode = CampaignClient.text(CampaignClient.battleMode, "name");
        if (mode.isEmpty()) mode = UiText.t("map.top.modeDefault", "Battle");
        g.drawText(textRenderer, mode, 6, 6, DarkUi.BRASS_HI, true);
        // the ledger about the clock: for each side a plain label (Your team / Enemy), its mark, score and held pips
        boolean pips = CampaignClient.showsPoints();
        int mine = GravesAndGloryClient.team;
        String yours = UiText.t("map.top.yourTeam", "Your team"), enemy = UiText.t("map.top.enemy", "Enemy");
        String la = mine == 0 ? yours : mine == 1 ? enemy : "", lb = mine == 1 ? yours : mine == 0 ? enemy : "";
        drawnSides = la + "|" + lb;
        String a = ModeMath.ledgerText(CampaignClient.ledgerFormat(0), GravesAndGloryClient.dawn, CampaignClient.ledgerMax(0));
        String b = ModeMath.ledgerText(CampaignClient.ledgerFormat(1), GravesAndGloryClient.dusk, CampaignClient.ledgerMax(1));
        String clock = UiMath.clock(CommandMode.seconds);
        int heldA = 0, heldB = 0;
        for (int i = 0; i < Spec.POINTS.length; i++) { int o = GravesAndGloryClient.ownerAt(i); if (o == 0) heldA++; else if (o == 1) heldB++; }
        int lwa = la.isEmpty() ? 0 : textRenderer.getWidth(la) + 5, lwb = lb.isEmpty() ? 0 : textRenderer.getWidth(lb) + 5;
        int wa = lwa + 9 + textRenderer.getWidth(a) + (pips ? heldA * 6 + 2 : 0), wb = 9 + textRenderer.getWidth(b) + (pips ? heldB * 6 + 2 : 0) + lwb, wc = textRenderer.getWidth(clock) + 16;
        int x = width / 2 - wc / 2 - wa;
        int maxRight = width - 6;
        if (x + wa + wc + wb > maxRight) x = Math.max(textRenderer.getWidth(mode) + 14, maxRight - (wa + wc + wb));
        if (lwa > 0) { g.drawText(textRenderer, la, x, 6, mine == 0 ? DarkUi.BRASS_HI : DarkUi.MUTED, true); x += lwa; }
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
        if (lwb > 0) g.drawText(textRenderer, lb, x + 7 + 5, 6, mine == 1 ? DarkUi.BRASS_HI : DarkUi.MUTED, true);
    }

    /** The bottom bar: the buttons' row (drawn as widgets) with a brass rule before Fit, then the controls as key chips, centred. */
    void drawBottomBar(DrawContext g) {
        int y0 = height - bottom;
        wood(g, 0, y0, width, bottom);
        g.fill(0, y0, width, y0 + 1, BRASS_LO);
        g.fill(0, y0 + 1, width, y0 + 2, DarkUi.BRASS);
        g.fill(fitRule, y0 + 5, fitRule + 1, y0 + 3 + BUTTON_H - 2, BRASS_LO);
        int y = y0 + 3 + BUTTON_H + 4, x = (width - chipsWidth()) / 2;
        for (var c : chips()) {
            int kw = textRenderer.getWidth(c[0]) + 2 * CHIP_PAD;
            g.fill(x, y, x + kw, y + CHIP_H, 0xff120c08);
            g.drawBorder(x, y, kw, CHIP_H, BRASS_LO);
            g.drawText(textRenderer, c[0], x + CHIP_PAD, y + 2, DarkUi.BRASS_HI, false);
            x += kw + CHIP_GAP;
            g.drawText(textRenderer, c[1], x, y + 2, DarkUi.IVORY, false);
            x += textRenderer.getWidth(c[1]) + CHIP_SEP;
        }
    }

    /** The glyph key: a wood panel with a brass edge in the map area's bottom-left corner, each row the map's own icon at map size and a
     *  word. Its header ("Key", with - or +) folds it away and back. */
    void drawKey(DrawContext g) {
        String keyHead = UiText.t("map.key.title", "Key");
        int tw = 0;
        for (int i = 0; i < KEY_WORDS.length; i++) tw = Math.max(tw, textRenderer.getWidth(keyWord(i)) + (i == 1 ? 10 : 0)); // 10: your mark after Yours
        int rows = 0;
        for (int h : KEY_ROWS) rows += h;
        int w = keyOpen ? 4 + 15 + 4 + tw + 5 : textRenderer.getWidth(keyHead) + 20, h = KEY_HEAD + (keyOpen ? rows + 3 : 0);
        int x = ax + 4, y = ay + ah - 4 - h;
        keyBox = new int[] {x, y, w, h};
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x66000000);
        wood(g, x, y, w, h);
        g.drawBorder(x, y, w, h, DarkUi.BRASS);
        g.drawText(textRenderer, keyHead, x + 5, y + 2, DarkUi.BRASS_HI, false);
        String fold = keyOpen ? "-" : "+";
        g.drawText(textRenderer, fold, x + w - 5 - textRenderer.getWidth(fold), y + 2, DarkUi.MUTED, false);
        taken.add(new int[] {x, y, w + 2, h + 2});
        if (!keyOpen) return;
        g.fill(x + 3, y + KEY_HEAD - 1, x + w - 3, y + KEY_HEAD, BRASS_LO);
        int col = ours(), cx = x + 4 + 7, tx = x + 4 + 15 + 4, ry = y + KEY_HEAD + 1;
        for (int i = 0; i < KEY_WORDS.length; i++) {
            int rh = KEY_ROWS[i], my = ry + rh / 2;
            switch (i) {
                case 0 -> { triangle(g, cx, my + 1, 0, -1, 7.5, 4.5, INK); triangle(g, cx, my + 1, 0, -1, 5.5, 3, DarkUi.MAP_YOU); }
                case 1 -> icon(g, cx, my, SHIELD, col, false);
                case 2 -> allyIcon(g, cx, my, SHIELD, col);
                case 3 -> glyph(g, TRI, cx - 3, my - 4, DarkUi.MAP_ENEMY, INK);
                case 4 -> glyph(g, SWORDS, cx - 3, my - 3, DarkUi.WARN, INK);
                default -> pennant(g, cx - 8, my + 8, 0, col); // the cloth beside the word, the pole into the panel's foot
            }
            String word = keyWord(i);
            g.drawText(textRenderer, word, tx, my - 3, DarkUi.IVORY, false);
            if (i == 1) oursMark(g, tx + textRenderer.getWidth(word) + 3, my - 3);
            ry += rh;
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
            g.drawText(textRenderer, rowText(p.rows().get(i)), p.x() + 9, ry + 2, hover ? DarkUi.BRASS_HI : DarkUi.IVORY, false);
        }
    }

    void tooltip(DrawContext g, int mx, int my) {
        Hit h = top(mx, my); // the topmost, an icon over a ping
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

    /** A 1 px ring of radius r from the top, clockwise, over `part` of the turn; hatched leaves every other 3 px dash out. */
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

    /** The map area the frame's lines are cut to (set by render). */
    private static int clipX0, clipY0, clipX1, clipY1;

    /** A solid line `w` px wide, cut to the map area so a far goal costs only what shows. */
    static void stroke(DrawContext g, double x0, double y0, double x1, double y1, int color, int w) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        if (len < 1) return;
        int n = (int) Math.ceil(len);
        double t0 = 0, t1 = 1, dx = x1 - x0, dy = y1 - y0;
        double[] p = {-dx, dx, -dy, dy}, q = {x0 - clipX0 + w, clipX1 - 1 + w - x0, y0 - clipY0 + w, clipY1 - 1 + w - y0};
        for (int k = 0; k < 4; k++) { // Liang-Barsky
            if (p[k] == 0) { if (q[k] < 0) return; continue; }
            double r = q[k] / p[k];
            if (p[k] < 0) t0 = Math.max(t0, r); else t1 = Math.min(t1, r);
            if (t0 > t1) return;
        }
        int lo = w / 2;
        for (int i = (int) Math.floor(t0 * n); i <= (int) Math.ceil(t1 * n); i++) {
            int x = (int) Math.round(x0 + dx * i / n), y = (int) Math.round(y0 + dy * i / n);
            g.fill(x - lo, y - lo, x - lo + w, y - lo + w, color);
        }
    }

    /** A solid arrow in `color` with a dark outline, its head's tip on (x1, y1). */
    static void arrow(DrawContext g, int x0, int y0, int x1, int y1, int color) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        if (len < 8) return;
        double ux = (x1 - x0) / len, uy = (y1 - y0) / len;
        double bx = x1 - ux * 6, by = y1 - uy * 6; // the shaft stops inside the head
        stroke(g, x0, y0, bx, by, INK, 3);
        triangle(g, (int) Math.round(x1 - ux * 6.5), (int) Math.round(y1 - uy * 6.5), ux, uy, 6.5, 4.5, INK);
        stroke(g, x0, y0, bx, by, color, 1);
        triangle(g, (int) Math.round(x1 - ux * 6.5), (int) Math.round(y1 - uy * 6.5), ux, uy, 4.8, 3, color);
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

    /** A plan pennant whose pole stands on (x, y): an ink pole and a team-colour cloth with the job's glyph in ink. */
    void pennant(DrawContext g, int x, int y, int job, int color) {
        int top = y - 16;
        g.fill(x, top, x + 2, y + 1, INK);
        g.fill(x + 2, top, x + 13, top + 10, INK);
        g.fill(x + 2, top + 1, x + 12, top + 9, color);
        g.fill(x + 12, top + 3, x + 14, top + 7, INK); // a short tail on the fly end
        glyph(g, jobGlyph(job), x + 4, top + 1, INK, 0);
    }

    static String[] jobGlyph(int job) { return switch (job) { case 0 -> SWORD; case 1 -> SHIELD; case 2 -> BANNER; default -> FOLLOW; }; }
    /** The troop type's glyph: shield for infantry, bow for archers, horse for riders, cross for support, catapult for siege, else a dot. */
    static String[] typeGlyph(String group) {
        return switch (group) { case "infantry" -> SHIELD; case "ranged" -> BOW; case "cavalry" -> HORSE; case "support" -> CROSS; case "siege" -> SIEGE; default -> DOT; };
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
    static final String[] SWORDS = {"x.....x", ".x...x.", "..x.x..", "...x...", "..x.x..", "xx...xx", "x.....x"};
    static final String[] TRI = {"...x...", "...x...", "..xxx..", "..xxx..", ".xxxxx.", ".xxxxx.", "xxxxxxx"};
    static final String[] TRI_OPEN = {"...x...", "...x...", "..x.x..", "..x.x..", ".x...x.", ".x...x.", "xxxxxxx"};
    // troop types (Revision 2)
    static final String[] BOW = {"xxx....", "x..x...", "x...xx.", "xxxxxxx", "x...xx.", "x..x...", "xxx...."};
    static final String[] HORSE = {"..x.x..", ".xxxxx.", "xxxxxx.", "xx.xxx.", "..xxxx.", ".xxxxx.", "xxxxxxx"};
    static final String[] CROSS = {"..xxx..", "..xxx..", "xxxxxxx", "xxxxxxx", "xxxxxxx", "..xxx..", "..xxx.."};
    static final String[] SIEGE = {"......x", ".....x.", "....x..", "xxxxxxx", ".x...x.", "x.x.x.x", ".x...x."};
    static final String[] DOT = {".......", "..xxx..", ".xxxxx.", ".xxxxx.", ".xxxxx.", "..xxx..", "......."};

    // ---------------------------------------------------------------- buttons

    /** A wood plate with a brass edge; lit when on() (the armed ping). */
    final class WoodButton extends ButtonWidget {
        final java.util.function.BooleanSupplier on;
        WoodButton(int x, int y, int w, int h, String label, java.util.function.BooleanSupplier on, PressAction action) { super(x, y, w, h, Text.literal(label), action, DEFAULT_NARRATION_SUPPLIER); this.on = on; }
        @Override public void renderWidget(DrawContext g, int mx, int my, float delta) {
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

    // ---------------------------------------------------------------- for the checks

    /** The point's hit box as the last frame drew it, or null. */
    Hit pointHit(int i) { for (var h : hits) if (h.kind().equals("point") && h.point() == i) return h; return null; }
    /** The screen centre of point i as the last frame drew it, or null. */
    int[] pointAt(int i) { var h = pointHit(i); return h == null ? null : new int[] {h.cx(), h.cy()}; }
    /** An ally army's first icon box as the last frame drew it, or null. */
    Hit armyHit(String id) { for (var h : hits) if (h.kind().equals("ally") && id.equals(h.army())) return h; return null; }
    /** The centre of an ally army's first icon as the last frame drew it, or null. */
    int[] armyAt(String id) { var h = armyHit(id); return h == null ? null : new int[] {h.cx(), h.cy()}; }
    /** The centre of the open popup's row with these words, or null. */
    int[] popupRow(String row) {
        if (popup == null) return null;
        int i = popup.rows().indexOf(row);
        return i < 0 ? null : new int[] {popup.x() + popup.w() / 2, popup.rowY(i) + ROW / 2};
    }
    /** True when the key chips fit the bar's width, the buttons' row does not run into them, and the drawn key panel sits inside the map
     *  area with room left above it. */
    boolean barFits() {
        int cw = chipsWidth();
        boolean buttons = true;
        for (var w : children()) if (w instanceof ButtonWidget b && (b.getX() < 2 || b.getX() + b.getWidth() > width - 2 || b.getY() + b.getHeight() > height - bottom + 3 + BUTTON_H)) buttons = false;
        boolean key = keyBox[2] > 0 && keyBox[0] >= ax && keyBox[1] >= ay + 20 && keyBox[0] + keyBox[2] <= ax + aw && keyBox[1] + keyBox[3] <= ay + ah;
        return cw <= width - 8 && buttons && key;
    }
    /** A whole left click (press and release in place) as the mouse would give it. */
    void clickAt(double x, double y) { mouseClicked(x, y, 0); mouseReleased(x, y, 0); }
}
