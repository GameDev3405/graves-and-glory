package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.*;

/**
 * The in-battle command panel (command UI plan Task 7, mockup variation 2 "Pennants down the left edge"): a pole on the left edge, the
 * group pills above five pennants (Attack, Charge, Follow me, Retreat, Formation), each with its icon, name and bound key, in the House
 * colour at Spec pennantAlpha; the Formation pennant opens the shape tray to its right. Drawn in GUI space at the player's GUI Scale,
 * texel-true, inside the left 30% of the screen (the tray inside 45%). It slides in and out over panelSlideTicks.
 *
 * Clicks call the very paths the keys do (CommandSelection.order, chooseShape, select), so the panel sends nothing a key would not.
 * While the panel is open and no order is being aimed, PanelInputMixin keeps the cursor free: a left click goes to mouseClicked (a
 * pennant, pill or tray cell, else the troop under the cursor), a right drag turns the camera, and neither swings nor uses an item.
 */
public final class CommandPanel {
    private CommandPanel() {}

    public static final int POLE = 1, PILL = 100, PENNANT = 200, TRAY = 300, CELL = 400, SWEEP = 500, SECS = 501;
    /** Edit mode's chrome (PanelLayout): the dashed outline, the caption, the toolbar and its controls. */
    public static final int EDGE = 600, CAPTION = 601, TOOLBAR = 602, SLIDER = 603, HIDE = 604, RESET = 605, DONE = 606;
    /** The orders in pennant order, with their names. */
    public static final String[] ORDERS = {"attack", "charge", "follow", "retreat", "formation"};
    static final String[] NAMES = {"Attack", "Charge", "Follow me", "Retreat", "Formation"};
    static final String[] SHAPE_NAMES = {"Line", "Wall", "Wedge", "Loose"};
    static final String[] ICONS = {"command/icon_attack", "command/icon_charge", "command/icon_follow", "command/icon_retreat", "command/icon_formation"};
    static final String[] SHAPE_ICONS = {"command/shape_line", "command/shape_block", "command/shape_wedge", "command/shape_loose"};
    static final int CREAM = 0xfff6dc, GOLD = 0xffd27a, TRIM = 0xe6c46a, DARK = 0x1b1a1f;
    static final int[] PILL_INSET = {4, 2, 1, 1, 0, 0, 0, 0, 1, 1, 2, 4};
    /** Pennant text layout: icon at ICON_X (16 wide), name at NAME_X, at least KEY_GAP between the name and the key, the key ending KEY_PAD before the box's end. */
    static final int ICON_X = 4, NAME_X = 24, KEY_GAP = 6, KEY_PAD = 26;
    static final int PENNANT_H = 24, PILL_H = 12, CELL_W = 32, CELL_W_WIDE = 36, CELL_H = 44;

    /** The panel's bounds in the last frame {x, y, w, h}, GUI px (placement and slide included); null when it was not drawn. */
    public static int[] bounds;
    /** Its resting bounds in the last frame {x, y, w, h, screen w, screen h} (no slide) and GUI px per HUD canvas px, for a drag. */
    static int[] rest;
    static double restK = 1;
    /** The pennant or tray cell under the cursor in the last frame (PENNANT + i, CELL + j), or -1. */
    public static int hovered = -1;
    /** Frames rendered, and the slide the last one used (probes for the checks: a shot waits for a fresh, settled frame). */
    public static int frames;
    public static double lastSlide;
    // edit mode (the Arrange command panel key): the panel is moved, hidden or faded; orders wait until it is left
    static boolean editing, dragging, sliding;
    static double grabX, grabY;
    static int sliderX, sliderW = 1;

    /** What the last frame drew, {id, x, y, w, h} in GUI px (the slide's offset included). */
    public static final List<int[]> lastDrawn = new ArrayList<>();
    /** The alpha each drawn id's body used in the last frame. */
    public static final Map<Integer, Float> lastAlpha = new HashMap<>();
    /** Clickable boxes of the last frame (GUI px): id -> {x, y, w, h}. */
    static final Map<Integer, int[]> hits = new LinkedHashMap<>();

    // slide state
    private static boolean slideOpen;
    private static double slideFrom;
    private static long slideAt;
    // gold flash after an order
    private static int flashId = -1;
    private static long flashAt;
    private static final boolean[] keyWas = new boolean[ORDERS.length];
    // cursor bookkeeping (PanelInputMixin): whether the panel held the mouse at the last client tick
    static boolean wasCapturing, relockPending;

    // the world camera of the last frame, for the cursor ray and the projection of a troop onto the screen
    private static final Matrix4f view = new Matrix4f(), proj = new Matrix4f();
    private static Vec3d camPos = Vec3d.ZERO;
    private static boolean haveCamera;

    static {
        WorldRenderEvents.START.register(ctx -> {
            view.set(ctx.matrixStack().peek().getPositionMatrix());
            proj.set(ctx.projectionMatrix());
            camPos = ctx.camera().getPos();
            haveCamera = true;
        });
        // the cursor follows the panel every client tick, HUD drawn or not (a pause, a cinematic, the battle's end)
        ClientTickEvents.END_CLIENT_TICK.register(CommandPanel::syncCursor);
    }

    /** 0 closed .. 1 open: the panel's slide, eased over Spec panelSlideTicks from the moment it was opened or shut. */
    public static double slide() {
        boolean open = CommandSelection.panelOpen();
        long now = Util.getMeasuringTimeMs();
        if (open != slideOpen) { slideFrom = value(now); slideOpen = open; slideAt = now; }
        return value(now);
    }
    private static double value(long now) {
        double span = Math.max(1, Spec.COMMANDINTERFACE[0].panelSlideTicks()) * 50.0, moved = (now - slideAt) / span;
        return slideOpen ? Math.min(1, slideFrom + moved) : Math.max(0, slideFrom - moved);
    }

    /** Keyboard attack / use presses swallowed while capturing (CommandInputMixin); a probe for the checks. */
    public static int swallowedKeys;

    /** Whether the panel holds the mouse: open, no order being aimed (the aim relocks the cursor for its crosshair), no Screen, in battle,
     *  and drawn: a hidden HUD (F1), a cinematic or a diagnostics run that hides the HUD leaves an invisible panel that must not keep the cursor. */
    public static boolean capturing() {
        var c = MinecraftClient.getInstance();
        return (editing || CommandSelection.panelOpen() && !CommandMode.active()) && hudShown(c);
    }

    /** Whether the battle HUD, and so the panel, is on screen: in battle, no Screen, the HUD not hidden by F1, a cinematic or a diagnostics run. */
    public static boolean hudShown(MinecraftClient c) {
        return c.currentScreen == null && c.player != null && GravesAndGloryClient.battleActive()
            && !c.options.hudHidden && !CampaignClient.cinematic() && !(Diagnostics.active() && Diagnostics.hideHud());
    }

    /**
     * Frees the cursor while capturing and takes it back the tick capturing ends with no Screen up (PanelInputMixin blocks the game
     * relocking it meanwhile). Runs at the end of every client tick, so a pause and resume, the battle's end or a cinematic still relock.
     */
    static void syncCursor(MinecraftClient c) {
        boolean cap = capturing();
        if (cap) { if (c.mouse.isCursorLocked()) c.mouse.unlockCursor(); relockPending = false; }
        else if ((wasCapturing || relockPending) && c.currentScreen == null && !c.mouse.isCursorLocked()) {
            c.mouse.lockCursor();
            relockPending = !c.mouse.isCursorLocked(); // the game locks only a focused window: try again on the next ticks
        } else relockPending = false;
        wasCapturing = cap;
    }

    /** The cursor in GUI px, or null while the cursor is locked. */
    static double[] cursor(MinecraftClient c) {
        if (c.mouse.isCursorLocked()) return null;
        var w = c.getWindow();
        return new double[]{c.mouse.getX() * w.getScaledWidth() / (double) w.getWidth(), c.mouse.getY() * w.getScaledHeight() / (double) w.getHeight()};
    }

    static String key(String id) {
        String k = GravesAndGloryClient.hint(id);
        return MinecraftClient.getInstance().textRenderer.getWidth(k) > 30 ? GravesAndGloryClient.hintShort(id) : k;
    }
    static KeyBinding binding(int i) {
        return switch (i) { case 0 -> GravesAndGloryClient.attack; case 1 -> GravesAndGloryClient.charge; case 2 -> GravesAndGloryClient.followMe; case 3 -> GravesAndGloryClient.retreat; default -> GravesAndGloryClient.formation; };
    }

    public static void render(DrawContext g, float delta) {
        var c = MinecraftClient.getInstance();
        lastDrawn.clear(); lastAlpha.clear(); hits.clear(); bounds = null; hovered = -1;
        // a key's press shows on its pennant as a click does
        for (int i = 0; i < ORDERS.length; i++) {
            KeyBinding k = binding(i);
            boolean down = k != null && k.isPressed();
            if (down && !keyWas[i] && CommandSelection.panelOpen() && !editing) flash(PENNANT + i);
            keyWas[i] = down;
        }
        PanelLayout.ensureLoaded(c);
        double s = editing ? 1 : slide(); // edit mode shows the panel fully open; closed, the panel is off screen (hidden or not)
        frames++; lastSlide = s;
        if (s <= 0) return;
        TextRenderer font = c.textRenderer;
        var win = c.getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();
        double eased = 1 - Math.pow(1 - s, 3);
        double[] mouse = cursor(c);
        try { draw(g, font, sw, sh, eased, mouse); }
        finally { RenderSystem.setShaderColor(1, 1, 1, 1); }
        if (editing) {
            try { editChrome(g, font, sw, sh, mouse); }
            finally { RenderSystem.setShaderColor(1, 1, 1, 1); }
        }
    }

    static void flash(int id) { flashId = id; flashAt = Util.getMeasuringTimeMs(); }
    private static double flashLeft(int id) {
        if (id != flashId) return 0;
        double t = (Util.getMeasuringTimeMs() - flashAt) / 250.0;
        return t >= 1 ? 0 : 1 - t;
    }

    /** The offset of this frame's panel from its default layout (the player's placement plus the slide), GUI px. */
    private static int ox, oy;
    /** This frame's opacity: the whole panel's, and the floor its text and icons keep (60%). */
    static float op = 1f, ink = 1f;
    public static float op() { return op; }
    public static float ink() { return ink; }
    private static void record(int id, int x, int y, int w, int h) { lastDrawn.add(new int[]{id, x + ox, y + oy, w, h}); hits.put(id, new int[]{x + ox, y + oy, w, h}); }
    private static boolean over(double[] m, int x, int y, int w, int h) { return m != null && m[0] >= x + ox && m[0] < x + ox + w && m[1] >= y + oy && m[1] < y + oy + h; }
    /** A fill's ARGB with the panel's opacity applied; text's with its 60% floor. */
    static int fa(int argb) { return ((int) Math.round((argb >>> 24) * op) << 24) | (argb & 0xffffff); }
    static int ta(int argb) { return ((int) Math.round((argb >>> 24) * ink) << 24) | (argb & 0xffffff); }

    /** Lays the pills left to right from the pole, wrapping inside limit; returns the rows. */
    static int layPills(TextRenderer font, List<CommandSelection.Pill> pills, int sw, int limit, int flagX, int pad, int gap, List<int[]> box, List<String> text) {
        int px = flagX + 2, rows = 1;
        for (int i = 0; i < pills.size(); i++) {
            String t = pills.get(i).custom() ? pills.get(i).label() : (i < 4 && sw >= 500 ? (i + 1) + " " : "") + shortName(pills.get(i).id()); // narrow screens drop the number: one row; a custom group's pill is "<n> <name>" (Task 12)
            int w = font.getWidth(t) + pad;
            if (px + w > limit && px > flagX + 2) { px = flagX + 2; rows++; }
            box.add(new int[]{px, rows - 1, w});
            text.add(t);
            px += w + gap;
        }
        return rows;
    }
    /** Whether a first-row pill, shifted by sx, ends past x. */
    static boolean reaches(List<int[]> box, int sx, int x) { for (int[] b : box) if (b[1] == 0 && b[0] + b[2] + sx > x) return true; return false; }

    /** One frame of the panel: its default layout, moved to the player's placement (PanelLayout) and slid in by eased (0 off screen .. 1 open). */
    static void draw(DrawContext g, TextRenderer font, int sw, int sh, double eased, double[] mouse) {
        float alpha = (float) Spec.COMMANDINTERFACE[0].pennantAlpha();
        int limit = (int) Math.floor(sw * 0.30), trayLimit = (int) Math.floor(sw * 0.45);
        int poleX = 2, flagX = 7;
        // pennant width: the mockup's 150 of 960, or as long as its words need
        int words = 0;
        for (int i = 0; i < ORDERS.length; i++) words = Math.max(words, font.getWidth(NAMES[i]) + KEY_GAP + keySlot(font, i));
        int W = Math.max(64, Math.max((int) Math.round(sw * 150 / 960.0), NAME_X + words + KEY_PAD));
        int ext = sw >= 600 ? (int) Math.round(W * 0.19) : Math.max(4, (int) Math.round(W * 0.08));
        W = Math.min(W, limit - flagX - ext);
        int pitch = Math.max(26, (int) Math.round(sh * 42 / 540.0));
        // pills, wrapped inside the limit
        var pills = CommandSelection.pills();
        List<int[]> pillBox = new ArrayList<>();
        List<String> pillText = new ArrayList<>();
        int pillPad = sw < 600 ? 6 : 10, pillGap = sw < 600 ? 2 : 3; // narrow screens: tighter pills, so they keep one row
        int rows = layPills(font, pills, sw, limit, flagX, pillPad, pillGap, pillBox, pillText);
        int pillsH = rows * PILL_H + (rows - 1) * 3;
        // the stack ends above the group column and hint slip (lower left); on a short screen the pennants close up to fit
        int floor = (int) Math.floor(sh * 0.74);
        int column = BattleHud.lowerLeftTop(MinecraftClient.getInstance().getWindow().getScaleFactor());
        if (column > 0) floor = Math.min(floor, column - 4 - 6); // the pole runs 6 past the last pennant
        int room = floor - 4 - pillsH - 6 - PENNANT_H;
        if (room < (ORDERS.length - 1) * pitch) pitch = Math.max(PENNANT_H + 2, room / (ORDERS.length - 1));
        int total = pillsH + 6 + (ORDERS.length - 1) * pitch + PENNANT_H;
        int top = Math.max(4, Math.min((int) Math.round(sh * 98 / 540.0), floor - total));
        int flagTop = top + pillsH + 6;
        int poleBottom = flagTop + (ORDERS.length - 1) * pitch + PENNANT_H + 6;

        // the panel's bounds in its default layout: the finial mark and the pole's top down to the pole's foot, the pills and the longest pennant across
        int bx0 = 0, by0 = Math.min(Math.max(0, top - 12), top - 4), bx1 = flagX + W + ext, by1 = poleBottom;
        for (int[] b : pillBox) bx1 = Math.max(bx1, b[0] + b[2]);
        int bw = bx1 - bx0, bh = by1 - by0;
        var win = MinecraftClient.getInstance().getWindow();
        double k = UiCanvas.hudK() / win.getScaleFactor(); // GUI px per HUD canvas px
        double[] at = PanelLayout.place(bw, bh, sw, sh, k, vanillaHud(sw, sh));
        int sx = at == null ? 0 : (int) Math.round(at[0]) - bx0, sy = at == null ? 0 : (int) Math.round(at[1]) - by0;
        boolean rightSide = at != null && bx0 + sx + bw / 2.0 > sw / 2.0;
        // a first pill row that reaches the HUD's gold plaque (a narrow screen squeezes the stack up to it): the pills close up, one row still
        int[] gold = BattleHud.goldGui(win.getScaleFactor());
        if (gold != null && top + sy < gold[1] + gold[3] && top + sy + PILL_H > gold[1]) {
            for (int pad = pillPad - 1, gap = Math.max(1, pillGap - 1); pad >= 4 && reaches(pillBox, sx, gold[0] - 3); pad--) {
                List<int[]> box = new ArrayList<>(); List<String> text = new ArrayList<>();
                if (layPills(font, pills, sw, limit, flagX, pad, gap, box, text) != rows) break;
                pillBox = box; pillText = text;
            }
        }
        // the slide: in from the left edge, or from the right edge for a panel placed in the right half
        int dx = at == null ? (int) Math.round(-(1 - eased) * (sw * 0.30 + 8))
            : rightSide ? (int) Math.round((1 - eased) * (sw - (bx0 + sx) + 8)) : (int) Math.round(-(1 - eased) * (bx0 + sx + bw + 8));
        rest = new int[]{bx0 + sx, by0 + sy, bw, bh, sw, sh}; restK = k;
        ox = dx + sx; oy = sy;
        bounds = new int[]{bx0 + ox, by0 + oy, bw, bh};
        op = editing && PanelLayout.hidden ? 0.35f : PanelLayout.opacity;
        ink = Math.max(op, 0.6f);
        if (editing) mouse = null; // no hover in edit mode: the panel is only moved there
        g.getMatrices().push();
        g.getMatrices().translate(ox, oy, 0);
        try { drawBody(g, font, sw, sh, mouse, alpha, trayLimit, poleX, flagX, W, ext, pitch, pills, pillBox, pillText, top, flagTop, poleBottom, bx0, sx, sy, at != null, rightSide); }
        finally { g.getMatrices().pop(); }
    }

    /** The panel's parts in its default layout's coordinates (the matrices carry the placement and the slide). */
    private static void drawBody(DrawContext g, TextRenderer font, int sw, int sh, double[] mouse, float alpha, int trayLimit, int poleX, int flagX, int W, int ext, int pitch,
                                 List<CommandSelection.Pill> pills, List<int[]> pillBox, List<String> pillText, int top, int flagTop, int poleBottom, int bx0, int sx, int sy, boolean moved, boolean rightSide) {

        // pole
        RenderSystem.setShaderColor(1, 1, 1, op);
        int[] pole = UiTex.three(g, "command/pole", poleX, top - 4, poleBottom - (top - 4));
        RenderSystem.setShaderColor(1, 1, 1, 1);
        record(POLE, pole[0], pole[1], pole[2], pole[3]);

        // pills
        for (int i = 0; i < pills.size(); i++) {
            int[] b = pillBox.get(i);
            int x = b[0], y = top + b[1] * (PILL_H + 3), w = b[2];
            boolean sel = CommandSelection.selected().contains(pills.get(i).id());
            boolean hov = over(mouse, x, y, w, PILL_H);
            if (sel) {
                for (int r = 0; r < PILL_H; r++) g.fill(x + PILL_INSET[r], y + r, x + w - PILL_INSET[r], y + r + 1, fa(0xe6000000 | GOLD));
            } else {
                // glass: a see-through smoked body with a pale sheen along its top, framed by the gold edge
                int body = hov ? 0x8c3a4a63 : 0x664a5a73;
                for (int r = 1; r < PILL_H - 1; r++) g.fill(x + PILL_INSET[r], y + r, x + w - PILL_INSET[r], y + r + 1, fa(body));
                for (int r = 1; r < PILL_H / 2; r++) g.fill(x + PILL_INSET[r], y + r, x + w - PILL_INSET[r], y + r + 1, fa(hov ? 0x33ffffff : 0x26ffffff)); // the sheen on the upper half
                g.fill(x + PILL_INSET[1] + 1, y + 1, x + w - PILL_INSET[1] - 1, y + 2, fa(hov ? 0xa6ffffff : 0x8cffffff)); // the bright top highlight
                g.fill(x + PILL_INSET[PILL_H - 2], y + PILL_H - 2, x + w - PILL_INSET[PILL_H - 2], y + PILL_H - 1, fa(0x40000000)); // a darker foot
                int edge = fa((hov ? 0xcc000000 : 0x99000000) | GOLD);
                g.fill(x + PILL_INSET[0], y, x + w - PILL_INSET[0], y + 1, edge);
                g.fill(x + PILL_INSET[0], y + PILL_H - 1, x + w - PILL_INSET[0], y + PILL_H, edge);
                for (int r = 1; r < PILL_H - 1; r++) {
                    int in = PILL_INSET[r], prev = Math.min(PILL_INSET[r - 1], PILL_INSET[r + 1]);
                    int run = Math.max(1, prev - in);
                    g.fill(x + in, y + r, x + in + run, y + r + 1, edge);
                    g.fill(x + w - in - run, y + r, x + w - in, y + r + 1, edge);
                }
            }
            String t = pillText.get(i);
            g.drawText(font, t, x + (w - font.getWidth(t) + 1) / 2, y + 2, ta(sel ? 0xff000000 | DARK : 0xff000000 | CREAM), !sel);
            record(PILL + i, x, y, w, PILL_H);
            lastAlpha.put(PILL + i, (sel ? 0.9f : 1f) * op);
        }

        // pennants
        int team = GravesAndGloryClient.team;
        int cloth = DarkUi.teamColor(team);
        RenderSystem.setShaderColor(1, 1, 1, ink);
        DarkUi.teamMark(g, team, poleX - 1, Math.max(0, top - 12));
        RenderSystem.setShaderColor(1, 1, 1, 1); // the House shape on the finial: the cloth colour never stands alone
        int chargeLeft = CommandNet.Client.chargeTicksLeft(), chargeTotal = Math.max(1, CommandNet.Client.chargeReadyIn);
        int formationY = 0, formationW = W;
        int secsRight = -1, secsBottom = -1; // the Charge cooldown seconds' right edge and the foot of that row, for the tray to keep clear of
        for (int i = 0; i < ORDERS.length; i++) {
            int y = flagTop + i * pitch;
            boolean hov = over(mouse, flagX, y, W, PENNANT_H);
            if (hov) hovered = PENNANT + i;
            boolean active = i == 4 && CommandSelection.trayOpen();
            boolean cooling = i == 1 && chargeLeft > 0;
            int w = active ? W + ext : hov ? W + Math.max(4, ext / 2) : W;
            double fl = flashLeft(PENNANT + i);
            int press = fl > 0.4 ? 1 : 0;
            int x = flagX + press, yy = y + press;
            float a = active ? Math.min(1f, alpha + 0.10f) : hov ? Math.min(1f, alpha + 0.25f) : alpha;
            float k = cooling ? 0.55f : 1f;
            if (cooling) a = Math.max(a, 0.75f); // darkened, but solid enough to read as a pennant over bright ground
            RenderSystem.setShaderColor(k * ((cloth >> 16) & 255) / 255f, k * ((cloth >> 8) & 255) / 255f, k * (cloth & 255) / 255f, a * op);
            pennant(g, "command/pennant", x, yy, w);
            if (active) {
                RenderSystem.setShaderColor(((GOLD >> 16) & 255) / 255f, ((GOLD >> 8) & 255) / 255f, (GOLD & 255) / 255f, op);
                pennant(g, "command/pennant_trim", x, yy, w);
                pennant(g, "command/pennant_trim", x + 1, yy, w);
            } else if (hov && !cooling) {
                // hover: the trim turns full gold, so the pennant under the cursor stands out at any size
                RenderSystem.setShaderColor(((GOLD >> 16) & 255) / 255f, ((GOLD >> 8) & 255) / 255f, (GOLD & 255) / 255f, op);
                pennant(g, "command/pennant_trim", x, yy, w);
                pennant(g, "command/pennant_trim", x + 1, yy, w); // doubled, so the hover reads at 854 as well as 2560
            } else {
                RenderSystem.setShaderColor(((TRIM >> 16) & 255) / 255f, ((TRIM >> 8) & 255) / 255f, (TRIM & 255) / 255f, (cooling ? 0.75f : 1f) * op);
                pennant(g, "command/pennant_trim", x, yy, w);
            }
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (cooling) {
                // the sweep: a dark veil over the part of the body still cooling, edged by a bright 2 px gold line, and a recharge bar along the
                // foot of the body (a dark track the gold fills as Charge recharges), so the cooldown reads at every size
                int bodyL = x + 4, bodyR = x + w - 16, span = bodyR - bodyL, veil = (int) Math.round(span * Math.min(1, chargeLeft / (double) chargeTotal));
                int edgeX = bodyR - veil;
                if (veil > 0) {
                    g.enableScissor(edgeX + ox, yy + oy, x + w + ox, yy + PENNANT_H + oy); // scissor rects are screen GUI px: the matrices' offset added
                    RenderSystem.setShaderColor(0, 0, 0, 0.65f * op);
                    pennant(g, "command/pennant", x, yy, w);
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                    g.disableScissor();
                }
                g.fill(bodyL, yy + PENNANT_H - 5, bodyR, yy + PENNANT_H - 2, ta(0xd9000000));
                if (edgeX > bodyL) g.fill(bodyL, yy + PENNANT_H - 4, edgeX, yy + PENNANT_H - 3, ta(0xff000000 | GOLD));
                int line = Math.max(bodyL, edgeX - 1);
                g.fill(line, yy + 1, line + 2, yy + PENNANT_H - 1, ta(0xff000000 | GOLD));
            } else if (hov) {
                g.fill(x + 2, yy + 2, x + w - 18, yy + PENNANT_H - 2, fa(0x4dfff6dc)); // hover: a clear light wash over the body
            }
            RenderSystem.setShaderColor(1, 1, 1, (cooling ? 0.8f : 1f) * ink);
            UiTex.draw(g, ICONS[i], x + ICON_X, yy + 4);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            int keyRight = x + w - KEY_PAD; // the key ends 10 px before the swallowtail notch's inner point (w - 16)
            // a soft dark band under the words keeps them legible over bright terrain; the cloth around it stays see-through
            int bandL = x + NAME_X - 3, bandR = keyRight + 3, band = fa(hov && !cooling ? 0x40000000 : 0x59000000);
            g.fill(bandL + 1, yy + 6, bandR - 1, yy + 18, band);
            g.fill(bandL, yy + 7, bandL + 1, yy + 17, band); g.fill(bandR - 1, yy + 7, bandR, yy + 17, band);
            // cream ink with the vanilla shadow reads on any House cloth (the gilded one included); gold stays on the active trim only
            int words = ta(cooling ? 0xd9000000 | CREAM : 0xff000000 | CREAM);
            g.drawText(font, NAMES[i], x + NAME_X, yy + 8, words, true);
            if (!cooling) {
                String kk = key(ORDERS[i]);
                g.drawText(font, kk, keyRight - font.getWidth(kk), yy + 8, ta(hov ? 0xff000000 | GOLD : 0xe6000000 | CREAM), true);
            } else {
                // the seconds left take the key's place (the key does nothing while Charge cools), inside the body like the key
                String secs = (chargeLeft + 19) / 20 + "s";
                int secsX = keyRight - font.getWidth(secs);
                g.drawText(font, secs, secsX, yy + 8, ta(0xffffffff), true);
                record(SWEEP, x + 1, yy, Math.max(24, secsX + font.getWidth(secs) - x - 1), 24);
                lastDrawn.add(new int[]{SECS, secsX + ox, yy + 8 + oy, font.getWidth(secs), 9}); // the seconds alone, for the tray check
                secsRight = secsX + font.getWidth(secs); secsBottom = yy + PENNANT_H;
            }
            if (fl > 0) {
                int fl8 = (int) Math.round(fl * 0.55 * 255);
                g.fill(x + 4, yy + 1, x + Math.min(w - 16, 21), yy + PENNANT_H - 1, fa((fl8 << 24) | GOLD));
                g.fill(x + 21, yy + 3, x + w - 16, yy + PENNANT_H - 3, fa((fl8 << 24) | GOLD));
            }
            record(PENNANT + i, x, yy, w - 4, PENNANT_H); // the art's notch tip ends 4 texels short of its box
            lastAlpha.put(PENNANT + i, a * op);
            if (i == 4) { formationY = yy; formationW = w; }
        }

        // the Formation tray
        if (CommandSelection.trayOpen() && !editing) {
            int cells = CommandSelection.SHAPES.length, pad = 6;
            int ax = flagX + formationW - 14; // the tray meets the pennant at its swallowtail notch
            int tw, th, tx, ty, cols, cw, gap;
            // the right limit in panel coordinates: 45% of the screen in the default layout, the screen's edge once the player has moved the panel
            int lim = moved ? sw - 2 - sx : trayLimit;
            int need4 = pad * 2 + cells * CELL_W + (cells - 1) * 5, leftEdge = bx0 - 2, leftRoom = leftEdge - (2 - sx);
            // a moved panel opens the tray to its left when the panel sits in the right half or the strip would leave the screen on the right
            boolean toLeft = moved && (rightSide || ax + need4 > lim) && leftRoom >= need4;
            // a 1x4 strip beside the pennant when it fits (cells as wide as the room allows, 5 px apart, so the names never crowd), else 2x2 above it
            if (toLeft) { cols = 4; gap = 5; cw = Math.min(CELL_W_WIDE, (leftRoom - pad * 2 - (cells - 1) * gap) / cells); }
            else if (ax + need4 <= lim) { cols = 4; gap = 5; cw = Math.min(CELL_W_WIDE, (lim - ax - pad * 2 - (cells - 1) * gap) / cells); }
            else { cols = 2; gap = 4; cw = ax + pad * 2 + 2 * CELL_W_WIDE + gap <= lim ? CELL_W_WIDE : CELL_W; }
            int rowsT = (cells + cols - 1) / cols;
            tw = pad * 2 + cols * cw + (cols - 1) * gap; th = pad * 2 + rowsT * CELL_H + (rowsT - 1) * gap;
            if (cols == 4) { tx = toLeft ? leftEdge - tw : ax; ty = formationY + PENNANT_H / 2 - th / 2; }
            else { tx = Math.max(moved ? 2 - sx : flagX, Math.min(ax, lim - tw)); ty = formationY + PENNANT_H - th; } // above the anchor, never down over the group cards
            if (secsRight >= 0 && cols == 2 && tx < secsRight + 2 && ty < secsBottom) {
                // the tray would cover the cooldown seconds: slide right of them if the limit allows, else drop below the Charge row
                if (secsRight + 2 + tw <= lim) tx = secsRight + 2; else ty = secsBottom + 1;
            }
            // never over the vanilla hotbar, health and armour rows (bottom centre): lift the tray above them when it reaches into their columns
            int[] vh = vanillaHud(sw, sh);
            if (tx + sx < vh[0] + vh[2] && vh[0] < tx + sx + tw && ty + sy + th > vh[1]) ty = vh[1] - 1 - th - sy;
            if (ty + sy < 2) ty = 2 - sy;
            if (ty + sy + th > sh - 2) ty = sh - 2 - th - sy;
            RenderSystem.setShaderColor(1, 1, 1, 1);
            g.fill(tx + 2, ty + 2, tx + tw - 2, ty + th - 2, fa(0xb31b120c));
            RenderSystem.setShaderColor(1, 1, 1, op);
            UiTex.nine(g, "command/tray", tx, ty, tw, th);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            record(TRAY, tx, ty, tw, th);
            for (int j = 0; j < cells; j++) {
                int cx = tx + pad + (j % cols) * (cw + gap), cy = ty + pad + (j / cols) * (CELL_H + gap);
                boolean picked = CommandSelection.SHAPES[j].equals(CommandMode.formation);
                boolean hov = over(mouse, cx, cy, cw, CELL_H);
                double fl = flashLeft(CELL + j);
                if (picked) g.fill(cx, cy, cx + cw, cy + CELL_H, 0x4d000000 | GOLD);
                else if (hov) g.fill(cx, cy, cx + cw, cy + CELL_H, 0x4d000000 | GOLD);
                if (hov) { g.drawBorder(cx, cy, cw, CELL_H, 0xff000000 | TRIM); if (sw < 600) g.drawBorder(cx + 1, cy + 1, cw - 2, CELL_H - 2, 0xff000000 | TRIM); hovered = CELL + j; } // a click picks this shape: the cell's rim turns gold under the cursor
                if (fl > 0) g.fill(cx, cy, cx + cw, cy + CELL_H, ((int) Math.round(fl * 0.5 * 255) << 24) | GOLD);
                if (picked) RenderSystem.setShaderColor(((GOLD >> 16) & 255) / 255f, ((GOLD >> 8) & 255) / 255f, (GOLD & 255) / 255f, 1f);
                // the key number in its own row at the top-left, the shape below it, the name at the foot: nothing overlaps
                UiTex.draw(g, SHAPE_ICONS[j], cx + (cw - 24) / 2, cy + 9);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                String name = SHAPE_NAMES[j];
                int ink = 0xff000000 | (picked ? GOLD : CREAM);
                g.drawText(font, name, cx + (cw - font.getWidth(name) + 1) / 2, cy + CELL_H - 9, ink, true);
                g.drawText(font, String.valueOf(j + 1), cx + 2, cy + 1, 0x80000000 | (picked ? GOLD : CREAM), false); // the key number dim: the cell is clicked, the number a shortcut
                record(CELL + j, cx, cy, cw, CELL_H);
            }
        }
    }

    // --- edit mode ---

    public static boolean editing() { return editing; }
    public static boolean dragging() { return dragging; }

    /** The Arrange command panel key: enters edit mode where the battle HUD shows, leaves it (saving) when in it. */
    public static void toggleEdit(MinecraftClient c) { if (editing) leaveEdit(c); else enterEdit(c); }

    /** Edit mode: the cursor freed, the panel drawn fully open with its outline, caption and toolbar; any aim is dropped and the tray folded. */
    public static boolean enterEdit(MinecraftClient c) {
        if (editing || !hudShown(c)) return false;
        PanelLayout.ensureLoaded(c);
        CommandMode.cancel();
        CommandSelection.setTrayOpen(false);
        editing = true; dragging = sliding = false;
        c.inGameHud.setOverlayMessage(net.minecraft.text.Text.empty(), false); // the hidden-panel note (or any action-bar line) never sits under the caption
        return true;
    }

    /** Leaves edit mode and saves the arrangement (Done, the key again, Esc, or the battle's end). */
    public static void leaveEdit(MinecraftClient c) {
        if (!editing) return;
        editing = false; dragging = sliding = false;
        if (PanelLayout.hidden) CommandSelection.setPanelOpen(false); // keyboard-only mode: the panel stays shut
        PanelLayout.save(c);
    }

    /** Every client tick: edit mode ends (saving) when the battle does. */
    static void editTick(MinecraftClient c) {
        if (editing && (c.player == null || !GravesAndGloryClient.battleActive())) leaveEdit(c);
    }

    /** A disconnect: edit mode is left (saved) and the arrangement is read from the file again on the next join. */
    public static void disconnected(MinecraftClient c) {
        if (editing) leaveEdit(c);
        dragging = sliding = false;
        PanelLayout.forget();
    }

    /** A left press in edit mode (GUI px): a toolbar control, else a press on the panel starts a drag. Never an order. */
    public static void editPress(double x, double y) {
        if (!editing) return;
        var c = MinecraftClient.getInstance();
        if (inside(SLIDER, x, y)) { sliding = true; slideTo(x); return; }
        if (inside(HIDE, x, y)) { PanelLayout.hidden = !PanelLayout.hidden; return; }
        if (inside(RESET, x, y)) { PanelLayout.reset(); return; }
        if (inside(DONE, x, y)) { leaveEdit(c); return; }
        if (inside(TOOLBAR, x, y) || rest == null) return;
        if (x >= rest[0] && x < rest[0] + rest[2] && y >= rest[1] && y < rest[1] + rest[3]) { dragging = true; grabX = x - rest[0]; grabY = y - rest[1]; }
    }

    /** The cursor moved in edit mode (GUI px): the panel follows a drag, the opacity follows the slider. */
    public static void editDrag(double x, double y) {
        if (!editing) return;
        if (sliding) slideTo(x);
        else if (dragging && rest != null) PanelLayout.moveTo(x - grabX, y - grabY, rest[2], rest[3], rest[4], rest[5], restK);
    }

    public static void editRelease() { dragging = false; sliding = false; }

    private static void slideTo(double x) { PanelLayout.opacity = PanelLayout.snapOpacity(PanelLayout.MIN_OPACITY + (x - sliderX) / Math.max(1, sliderW) * (1 - PanelLayout.MIN_OPACITY)); }

    private static boolean inside(int id, double x, double y) { int[] b = hits.get(id); return b != null && x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]; }

    private static void chrome(int id, int x, int y, int w, int h) { lastDrawn.add(new int[]{id, x, y, w, h}); hits.put(id, new int[]{x, y, w, h}); }

    static final int WOOD = 0xff3b2818, WOOD_HOVER = 0xff5a3d24, BRASS = 0xffb08d3c, BRASS_LIGHT = 0xffe0bf6a;

    /** Edit mode's chrome in screen GUI px: a dashed gold outline around the panel, the caption above it and the toolbar below it (above when there is no room). */
    static void editChrome(DrawContext g, TextRenderer font, int sw, int sh, double[] mouse) {
        int[] b = bounds;
        if (b == null) return;
        // the dashed outline, 1 px outside the bounds (kept on screen)
        int x0 = Math.max(0, b[0] - 1), y0 = Math.max(0, b[1] - 1), x1 = Math.min(sw, b[0] + b[2] + 1), y1 = Math.min(sh, b[1] + b[3] + 1);
        int dash = 0xff000000 | GOLD;
        for (int x = x0; x < x1; x += 5) { g.fill(x, y0, Math.min(x + 3, x1), y0 + 1, dash); g.fill(x, y1 - 1, Math.min(x + 3, x1), y1, dash); }
        for (int y = y0; y < y1; y += 5) { g.fill(x0, y, x0 + 1, Math.min(y + 3, y1), dash); g.fill(x1 - 1, y, x1, Math.min(y + 3, y1), dash); }
        lastDrawn.add(new int[]{EDGE, x0, y0, x1 - x0, y1 - y0});

        String cap = I18n.translate(PanelLayout.hidden ? "gui.gravesandglory.panel_edit.caption_hidden" : "gui.gravesandglory.panel_edit.caption");
        int capW = font.getWidth(cap) + 8, capH = 12;
        String opl = I18n.translate("gui.gravesandglory.panel_edit.opacity"), pct = Math.round(PanelLayout.opacity * 100) + "%";
        String hideL = I18n.translate(PanelLayout.hidden ? "gui.gravesandglory.panel_edit.show" : "gui.gravesandglory.panel_edit.hide");
        String resetL = I18n.translate("gui.gravesandglory.panel_edit.reset"), doneL = I18n.translate("gui.gravesandglory.panel_edit.done");
        int track = sw < 600 ? 44 : 64, pctW = font.getWidth("100%"), pad = 5, gap = 4, bh = 14, th = bh + pad * 2;
        int hideW = Math.max(font.getWidth(I18n.translate("gui.gravesandglory.panel_edit.hide")), font.getWidth(I18n.translate("gui.gravesandglory.panel_edit.show"))) + 10;
        int resetW = font.getWidth(resetL) + 10, doneW = font.getWidth(doneL) + 10;
        int tw = pad + font.getWidth(opl) + gap + 3 + track + 3 + gap + pctW + gap * 2 + hideW + gap + resetW + gap + doneW + pad;

        // the caption above the panel (below it at the top edge); the toolbar under the panel, or above it when there is no room below
        boolean capAbove = b[1] - 2 - capH >= 0;
        int capY = capAbove ? b[1] - 2 - capH : b[1] + b[3] + 2;
        int belowTop = (capAbove ? b[1] + b[3] : capY + capH) + 3;
        int capX = Math.max(1, Math.min(sw - 1 - capW, b[0]));
        int[] vh = vanillaHud(sw, sh);
        int ax = Math.max(2, Math.min(sw - 2 - tw, b[0])), sy = Math.max(2, Math.min(sh - 2 - th, b[1]));
        int[][] spots = {{ax, belowTop}, {ax, (capAbove ? capY : b[1]) - 3 - th}, {b[0] - 3 - tw, sy}, {b[0] + b[2] + 3, sy}};
        int tx = ax, ty = Math.max(2, Math.min(sh - 2 - th, belowTop));
        for (int[] o : spots) {
            if (o[0] < 2 || o[1] < 2 || o[0] + tw > sw - 2 || o[1] + th > sh - 2) continue;
            if (meets(o[0], o[1], tw, th, vh) || meets(o[0], o[1], tw, th, b) || meets(o[0], o[1], tw, th, new int[]{capX, capY, capW, capH})) continue;
            tx = o[0]; ty = o[1]; break;
        }

        g.fill(capX, capY, capX + capW, capY + capH, 0xb3000000);
        g.drawText(font, cap, capX + 4, capY + 2, 0xff000000 | GOLD, true);
        chrome(CAPTION, capX, capY, capW, capH);

        RenderSystem.setShaderColor(1, 1, 1, 1);
        UiTex.nine(g, "command/tray", tx, ty, tw, th);
        chrome(TOOLBAR, tx, ty, tw, th);
        int x = tx + pad, cy = ty + pad, textY = cy + (bh - 8) / 2;
        g.drawText(font, opl, x, textY, 0xff000000 | CREAM, true);
        x += font.getWidth(opl) + gap + 3;
        // the slider: a dark groove filled gold to the value, a brass knob
        sliderX = x; sliderW = track;
        int gy = cy + bh / 2 - 2;
        g.fill(x - 1, gy - 1, x + track + 1, gy + 4, BRASS);
        g.fill(x, gy, x + track, gy + 3, 0xff1b120c);
        int knob = x + (int) Math.round((PanelLayout.opacity - PanelLayout.MIN_OPACITY) / (1 - PanelLayout.MIN_OPACITY) * track);
        g.fill(x, gy, knob, gy + 3, 0xff000000 | GOLD);
        boolean onSlider = mouse != null && mouse[0] >= x - 3 && mouse[0] < x + track + 3 && mouse[1] >= cy && mouse[1] < cy + bh;
        g.fill(knob - 2, cy + 1, knob + 3, cy + bh - 1, 0xff1b120c);
        g.fill(knob - 1, cy + 2, knob + 2, cy + bh - 2, sliding || onSlider ? BRASS_LIGHT : BRASS);
        chrome(SLIDER, x - 3, cy, track + 6, bh);
        x += track + 3 + gap;
        g.drawText(font, pct, x + pctW - font.getWidth(pct), textY, 0xff000000 | GOLD, true);
        x += pctW + gap * 2;
        x = button(g, font, HIDE, hideL, x, cy, hideW, bh, mouse) + gap;
        x = button(g, font, RESET, resetL, x, cy, resetW, bh, mouse) + gap;
        button(g, font, DONE, doneL, x, cy, doneW, bh, mouse);
    }

    private static boolean meets(int x, int y, int w, int h, int[] r) { return x < r[0] + r[2] && r[0] < x + w && y < r[1] + r[3] && r[1] < y + h; }

    /** A dark-wood button with a brass edge (lighter under the cursor); returns its right edge. */
    private static int button(DrawContext g, TextRenderer font, int id, String label, int x, int y, int w, int h, double[] mouse) {
        boolean hov = mouse != null && mouse[0] >= x && mouse[0] < x + w && mouse[1] >= y && mouse[1] < y + h;
        g.fill(x, y, x + w, y + h, hov ? BRASS_LIGHT : BRASS);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hov ? WOOD_HOVER : WOOD);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x33ffffff);
        g.drawText(font, label, x + (w - font.getWidth(label) + 1) / 2, y + (h - 8) / 2, 0xff000000 | (hov ? GOLD : CREAM), true);
        chrome(id, x, y, w, h);
        return x + w;
    }

    /** The vanilla survival HUD's box in GUI px {x, y, w, h}: the hotbar and the health and armour rows above it (as InGameHud lays them out). */
    public static int[] vanillaHud(int sw, int sh) {
        var p = MinecraftClient.getInstance().player;
        float hp = p == null ? 20 : p.getMaxHealth() + p.getAbsorptionAmount();
        int rows = Math.max(1, (int) Math.ceil(hp / 2f / 10f)), rowH = Math.max(10 - (rows - 2), 3);
        int top = sh - 39 - (rows - 1) * rowH - 10; // the armour row sits one row above the top heart row
        return new int[]{sw / 2 - 91, top, 182, sh - top};
    }

    /** Room a pennant keeps for its key (Charge: also its cooldown seconds, which take the key's place). */
    static int keySlot(TextRenderer font, int i) { return Math.max(font.getWidth(key(ORDERS[i])), i == 1 ? font.getWidth("20s") : 0); }

    /** The pennant art (64 x 24) drawn w long: its hoist, its clean middle columns 8..19 tiled, then the swallowtail end. */
    static void pennant(DrawContext g, String name, int x, int y, int w) {
        int mid = Math.max(0, w - 52);
        UiTex.part(g, name, x, y, 0, 0, 8, PENNANT_H);
        for (int o = 0; o < mid; o += 12) UiTex.part(g, name, x + 8 + o, y, 8, 0, Math.min(12, mid - o), PENNANT_H);
        UiTex.part(g, name, x + 8 + mid, y, 20, 0, 44, PENNANT_H);
    }

    static String shortName(String id) {
        return switch (id) { case "infantry" -> "Inf"; case "ranged" -> "Arch"; case "cavalry" -> "Ride"; case "all" -> "All"; case "support" -> "Sup"; case "siege" -> "Siege"; default -> id; };
    }

    /**
     * A click at (x, y) in GUI px while the panel is open: a pill selects its group (Shift adds), a pennant gives its order and a tray cell
     * its shape through the same calls as the keys; a click on nothing of the panel picks the troop under the cursor. Returns whether
     * the click did something.
     */
    public static boolean mouseClicked(double x, double y, int button, boolean shift) {
        if (button != 0 || !CommandSelection.panelOpen()) return false;
        int id = -1;
        // the tray lies over the pennants' right ends, so it is hit first, and its cells before the tray's own box, which holds them
        // (in the map's order the box could come first and swallow a cell's click: Kort, 2026-11-11, "why cant i click which hold line i want?")
        List<Map.Entry<Integer, int[]>> order = new ArrayList<>(hits.entrySet());
        order.sort(Comparator.comparingInt(e -> e.getKey() >= CELL && e.getKey() < SWEEP ? 0 : e.getKey() == TRAY ? 1 : 2));
        for (var e : order) {
            int[] b = e.getValue();
            if (e.getKey() == POLE || e.getKey() == SWEEP) continue;
            if (x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]) { id = e.getKey(); break; }
        }
        if (id >= CELL && id < SWEEP) {
            int j = id - CELL;
            flash(id);
            CommandSelection.chooseShape(CommandSelection.SHAPES[j], shift);
            return true;
        }
        if (id == TRAY) return true;
        if (id >= PENNANT && id < TRAY) {
            int i = id - PENNANT;
            flash(id);
            CommandSelection.order(ORDERS[i], shift);
            return true;
        }
        if (id >= PILL && id < PENNANT) {
            var pills = CommandSelection.pills();
            int i = id - PILL;
            if (i < pills.size()) CommandSelection.select(pills.get(i).id(), shift);
            return true;
        }
        // the battle HUD's group column: a card's formation opens the tray for its group, any other part of a card or chip selects it
        String[] col = BattleHud.columnAt(x, y);
        if (col != null) {
            if (col[0].equals("form")) CommandSelection.openTrayFor(col[1], shift);
            else CommandSelection.select(col[1], shift);
            return true;
        }
        String group = troopAt(x, y);
        if (group != null) { CommandSelection.select(group, shift); return true; }
        return false;
    }

    /** Where a world point shows on the screen, {x, y} in GUI px, from the last frame's camera; null behind the camera or before a frame. */
    public static double[] project(Vec3d p) {
        if (!haveCamera) return null;
        var w = MinecraftClient.getInstance().getWindow();
        Vector4f v = new Vector4f((float) (p.x - camPos.x), (float) (p.y - camPos.y), (float) (p.z - camPos.z), 1f);
        view.transform(v); proj.transform(v);
        if (v.w <= 1e-4f) return null;
        double nx = v.x / v.w, ny = v.y / v.w;
        return new double[]{(nx + 1) / 2 * w.getScaledWidth(), (1 - ny) / 2 * w.getScaledHeight()};
    }

    /** The group of the army's troop under the cursor at (x, y) GUI px: the nearest of our team's units the cursor ray meets. */
    static String troopAt(double x, double y) {
        var c = MinecraftClient.getInstance();
        if (!haveCamera || c.world == null) return null;
        var w = c.getWindow();
        float nx = (float) (x / w.getScaledWidth() * 2 - 1), ny = (float) (1 - y / w.getScaledHeight() * 2);
        Matrix4f inv = new Matrix4f(proj).mul(view).invert();
        Vector4f near = inv.transform(new Vector4f(nx, ny, -1, 1)), far = inv.transform(new Vector4f(nx, ny, 1, 1));
        Vec3d a = new Vec3d(near.x / near.w, near.y / near.w, near.z / near.w), b = new Vec3d(far.x / far.w, far.y / far.w, far.z / far.w);
        Vec3d dir = b.subtract(a).normalize();
        Vec3d from = camPos.add(a), to = from.add(dir.multiply(160));
        String best = null; double bestD = Double.MAX_VALUE;
        for (Entity e : c.world.getEntities()) {
            if (e == c.player || !e.isAlive()) continue;
            String g = troopGroup(e);
            if (g == null) continue;
            var hit = e.getBoundingBox().expand(0.25).raycast(from, to);
            if (hit.isEmpty()) continue;
            double d = hit.get().squaredDistanceTo(from);
            if (d < bestD) { bestD = d; best = g; }
        }
        return best;
    }

    /** The group a unit of our own team belongs to, by its name (its sheet row) and its name colour (its team); null for anything else. */
    public static String troopGroup(Entity e) {
        var name = e.getCustomName();
        if (name == null || CommandMode.nameTeam(e) != GravesAndGloryClient.team || GravesAndGloryClient.team < 0) return null;
        var spec = ArmyMobs.named(name.getString());
        if (spec == null || !isOwn(e, spec, GravesAndGloryClient.markers)) return null;
        return spec.group();
    }

    /** How far (blocks) a unit may stand from its marker, which the state packet sends a moment earlier. */
    static final double OWN_MARKER_REACH = 5;

    /** Whether the unit is one of this player's own troops. Allies share the team and its name colour, so the name cannot tell: the
     *  state packet's markers can. The unit is own when the nearest marker of its troop, within reach, is an own marker. */
    static boolean isOwn(Entity e, Spec.TroopsRow spec, List<GravesAndGloryClient.Marker> markers) {
        GravesAndGloryClient.Marker best = null;
        double bestD = OWN_MARKER_REACH * OWN_MARKER_REACH;
        for (var m : markers) {
            if (ArmyMobs.named(m.name()) != spec) continue;
            double dx = m.x() - e.getX(), dz = m.z() - e.getZ(), d = dx * dx + dz * dz;
            if (d <= bestD) { bestD = d; best = m; }
        }
        return best != null && best.own();
    }
}
