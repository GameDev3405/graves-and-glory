package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
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

    public static final int POLE = 1, PILL = 100, PENNANT = 200, TRAY = 300, CELL = 400, SWEEP = 500;
    /** The orders in pennant order, with their names. */
    public static final String[] ORDERS = {"attack", "charge", "follow", "retreat", "formation"};
    static final String[] NAMES = {"Attack", "Charge", "Follow me", "Retreat", "Formation"};
    static final String[] SHAPE_NAMES = {"Line", "Wall", "Wedge", "Loose"};
    static final String[] ICONS = {"command/icon_attack", "command/icon_charge", "command/icon_follow", "command/icon_retreat", "command/icon_formation"};
    static final String[] SHAPE_ICONS = {"command/shape_line", "command/shape_block", "command/shape_wedge", "command/shape_loose"};
    static final int CREAM = 0xfff6dc, GOLD = 0xffd27a, TRIM = 0xe6c46a, DARK = 0x1b1a1f;
    static final int[] PILL_INSET = {4, 2, 1, 1, 0, 0, 0, 0, 1, 1, 2, 4};
    static final int PENNANT_H = 24, PILL_H = 12, CELL_W = 32, CELL_W_WIDE = 36, CELL_H = 44;

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
    static boolean wasCapturing;

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

    /** Whether the panel holds the mouse: open, no order being aimed (the aim relocks the cursor for its crosshair), no Screen, in battle. */
    public static boolean capturing() {
        var c = MinecraftClient.getInstance();
        return CommandSelection.panelOpen() && !CommandMode.active() && c.currentScreen == null && c.player != null && GravesAndGloryClient.battleActive();
    }

    /**
     * Frees the cursor while capturing and takes it back the tick capturing ends with no Screen up (PanelInputMixin blocks the game
     * relocking it meanwhile). Runs at the end of every client tick, so a pause and resume, the battle's end or a cinematic still relock.
     */
    static void syncCursor(MinecraftClient c) {
        boolean cap = capturing();
        if (cap) { if (c.mouse.isCursorLocked()) c.mouse.unlockCursor(); }
        else if (wasCapturing && c.currentScreen == null && !c.mouse.isCursorLocked()) c.mouse.lockCursor();
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
        lastDrawn.clear(); lastAlpha.clear(); hits.clear();
        // a key's press shows on its pennant as a click does
        for (int i = 0; i < ORDERS.length; i++) {
            KeyBinding k = binding(i);
            boolean down = k != null && k.isPressed();
            if (down && !keyWas[i] && CommandSelection.panelOpen()) flash(PENNANT + i);
            keyWas[i] = down;
        }
        double s = slide();
        if (s <= 0) return;
        TextRenderer font = c.textRenderer;
        var win = c.getWindow();
        int sw = win.getScaledWidth(), sh = win.getScaledHeight();
        double eased = 1 - Math.pow(1 - s, 3);
        int dx = (int) Math.round(-(1 - eased) * (sw * 0.30 + 8));
        double[] mouse = cursor(c);
        g.getMatrices().push();
        g.getMatrices().translate(dx, 0, 0);
        try { draw(g, font, sw, sh, dx, mouse); }
        finally { g.getMatrices().pop(); RenderSystem.setShaderColor(1, 1, 1, 1); }
    }

    static void flash(int id) { flashId = id; flashAt = Util.getMeasuringTimeMs(); }
    private static double flashLeft(int id) {
        if (id != flashId) return 0;
        double t = (Util.getMeasuringTimeMs() - flashAt) / 250.0;
        return t >= 1 ? 0 : 1 - t;
    }

    private static void record(int id, int x, int y, int w, int h, int dx) { lastDrawn.add(new int[]{id, x + dx, y, w, h}); hits.put(id, new int[]{x + dx, y, w, h}); }
    private static boolean over(double[] m, int x, int y, int w, int h, int dx) { return m != null && m[0] >= x + dx && m[0] < x + dx + w && m[1] >= y && m[1] < y + h; }

    /** One frame of the panel at slide offset dx (GUI px, already applied to the matrices; dx is added to the recorded rects only). */
    static void draw(DrawContext g, TextRenderer font, int sw, int sh, int dx, double[] mouse) {
        float alpha = (float) Spec.COMMANDINTERFACE[0].pennantAlpha();
        int limit = (int) Math.floor(sw * 0.30), trayLimit = (int) Math.floor(sw * 0.45);
        int poleX = 2, flagX = 7;
        // pennant width: the mockup's 150 of 960, or as long as its words need
        int words = 0;
        for (int i = 0; i < ORDERS.length; i++) words = Math.max(words, font.getWidth(NAMES[i]) + 4 + font.getWidth(key(ORDERS[i])));
        int W = Math.max(64, Math.max((int) Math.round(sw * 150 / 960.0), 22 + words + 24));
        int ext = sw >= 600 ? (int) Math.round(W * 0.19) : Math.max(4, (int) Math.round(W * 0.08));
        W = Math.min(W, limit - flagX - ext);
        int pitch = Math.max(26, (int) Math.round(sh * 42 / 540.0));
        // pills, wrapped inside the limit
        var pills = CommandSelection.pills();
        List<int[]> pillBox = new ArrayList<>();
        List<String> pillText = new ArrayList<>();
        int px = flagX + 2, rows = 1, rowW = 0;
        for (int i = 0; i < pills.size(); i++) {
            String t = pills.get(i).custom() ? pills.get(i).label() : (i < 4 && sw >= 500 ? (i + 1) + " " : "") + shortName(pills.get(i).id()); // narrow screens drop the number: one row; a custom group's pill is "<n> <name>" (Task 12)
            int w = font.getWidth(t) + 10;
            if (px + w > limit && px > flagX + 2) { px = flagX + 2; rows++; }
            pillBox.add(new int[]{px, rows - 1, w});
            pillText.add(t);
            px += w + 3;
        }
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

        // pole
        int[] pole = UiTex.three(g, "command/pole", poleX, top - 4, poleBottom - (top - 4));
        record(POLE, pole[0], pole[1], pole[2], pole[3], dx);

        // pills
        for (int i = 0; i < pills.size(); i++) {
            int[] b = pillBox.get(i);
            int x = b[0], y = top + b[1] * (PILL_H + 3), w = b[2];
            boolean sel = CommandSelection.selected().contains(pills.get(i).id());
            boolean hov = over(mouse, x, y, w, PILL_H, dx);
            if (sel) {
                for (int r = 0; r < PILL_H; r++) g.fill(x + PILL_INSET[r], y + r, x + w - PILL_INSET[r], y + r + 1, 0xe6000000 | GOLD);
            } else {
                RenderSystem.setShaderColor(1, 1, 1, hov ? 1f : 0.9f);
                UiTex.nine(g, "command/pill", x, y, w, PILL_H);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                int edge = (hov ? 0xcc000000 : 0x99000000) | GOLD;
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
            g.drawText(font, t, x + (w - font.getWidth(t) + 1) / 2, y + 2, sel ? 0xff000000 | DARK : 0xff000000 | CREAM, false);
            record(PILL + i, x, y, w, PILL_H, dx);
            lastAlpha.put(PILL + i, sel ? 0.9f : 1f);
        }

        // pennants
        int team = GravesAndGloryClient.team;
        int cloth = DarkUi.teamColor(team);
        DarkUi.teamMark(g, team, poleX - 1, Math.max(0, top - 12)); // the House shape on the finial: the cloth colour never stands alone
        int chargeLeft = CommandNet.Client.chargeTicksLeft(), chargeTotal = Math.max(1, CommandNet.Client.chargeReadyIn);
        int formationY = 0, formationW = W;
        for (int i = 0; i < ORDERS.length; i++) {
            int y = flagTop + i * pitch;
            boolean hov = over(mouse, flagX, y, W, PENNANT_H, dx);
            boolean active = i == 4 && CommandSelection.trayOpen();
            boolean cooling = i == 1 && chargeLeft > 0;
            int w = active ? W + ext : hov ? W + Math.max(2, ext / 3) : W;
            double fl = flashLeft(PENNANT + i);
            int press = fl > 0.4 ? 1 : 0;
            int x = flagX + press, yy = y + press;
            float a = active || hov ? Math.min(1f, alpha + 0.10f) : alpha;
            float k = cooling ? 0.45f : 1f;
            RenderSystem.setShaderColor(k * ((cloth >> 16) & 255) / 255f, k * ((cloth >> 8) & 255) / 255f, k * (cloth & 255) / 255f, a);
            pennant(g, "command/pennant", x, yy, w);
            if (active) {
                RenderSystem.setShaderColor(((GOLD >> 16) & 255) / 255f, ((GOLD >> 8) & 255) / 255f, (GOLD & 255) / 255f, 1f);
                pennant(g, "command/pennant_trim", x, yy, w);
                pennant(g, "command/pennant_trim", x + 1, yy, w);
            } else {
                RenderSystem.setShaderColor(((TRIM >> 16) & 255) / 255f, ((TRIM >> 8) & 255) / 255f, (TRIM & 255) / 255f, cooling ? 0.5f : 0.8f);
                pennant(g, "command/pennant_trim", x, yy, w);
            }
            if (cooling) BattleHud.sweep(g, x + 1, yy, Math.min(1, chargeLeft / (double) chargeTotal));
            RenderSystem.setShaderColor(1, 1, 1, cooling ? 0.55f : 1f);
            UiTex.draw(g, ICONS[i], x + 5, yy + 4);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            // cream ink with the vanilla shadow reads on any House cloth (the gilded one included); gold stays on the active trim only
            int ink = cooling ? 0xb3000000 | CREAM : 0xff000000 | CREAM;
            g.drawText(font, NAMES[i], x + 22, yy + 8, ink, true);
            String kk = key(ORDERS[i]);
            int keyRight = x + w - 24; // inside the solid body, clear of the swallowtail notch
            g.drawText(font, kk, keyRight - font.getWidth(kk), yy + 8, cooling ? ink : 0xe6000000 | CREAM, true);
            if (cooling) {
                // the seconds left sit just past the swallowtail tip, clear of the icon
                String secs = (chargeLeft + 19) / 20 + "s";
                int sx = Math.min(x + w - 2, limit - 1 - font.getWidth(secs));
                g.drawText(font, secs, sx, yy + 8, 0xffffffff, true);
                record(SWEEP, x + 1, yy, Math.max(24, sx + font.getWidth(secs) - x - 1), 24, dx);
            }
            if (fl > 0) {
                int fa = (int) Math.round(fl * 0.55 * 255);
                g.fill(x + 4, yy + 1, x + Math.min(w - 16, 21), yy + PENNANT_H - 1, (fa << 24) | GOLD);
                g.fill(x + 21, yy + 3, x + w - 16, yy + PENNANT_H - 3, (fa << 24) | GOLD);
            }
            record(PENNANT + i, x, yy, w - 4, PENNANT_H, dx); // the art's notch tip ends 4 texels short of its box
            lastAlpha.put(PENNANT + i, a);
            if (i == 4) { formationY = yy; formationW = w; }
        }

        // the Formation tray
        if (CommandSelection.trayOpen()) {
            int cells = CommandSelection.SHAPES.length, pad = 6;
            int ax = flagX + formationW - 2;
            int tw, th, tx, ty, cols, cw, gap;
            // a 1x4 strip beside the pennant when it fits (wide cells first, so the names never touch), else 2x2 above it, else below
            if (ax + pad * 2 + cells * CELL_W_WIDE + (cells - 1) * 3 <= trayLimit) { cols = 4; cw = CELL_W_WIDE; gap = 3; }
            else if (ax + pad * 2 + cells * CELL_W + (cells - 1) * 3 <= trayLimit) { cols = 4; cw = CELL_W; gap = 3; }
            else { cols = 2; gap = 2; cw = ax + pad * 2 + 2 * CELL_W_WIDE + gap <= trayLimit ? CELL_W_WIDE : CELL_W; }
            int rowsT = (cells + cols - 1) / cols;
            tw = pad * 2 + cols * cw + (cols - 1) * gap; th = pad * 2 + rowsT * CELL_H + (rowsT - 1) * gap;
            if (cols == 4) { tx = ax; ty = formationY + PENNANT_H / 2 - th / 2; }
            else { tx = Math.max(flagX, Math.min(ax, trayLimit - tw)); ty = formationY + PENNANT_H - th; } // above the anchor, never down over the group cards
            if (ty < 2) ty = 2;
            if (ty + th > sh - 2) ty = sh - 2 - th;
            RenderSystem.setShaderColor(1, 1, 1, 1);
            UiTex.nine(g, "command/tray", tx, ty, tw, th);
            record(TRAY, tx, ty, tw, th, dx);
            for (int j = 0; j < cells; j++) {
                int cx = tx + pad + (j % cols) * (cw + gap), cy = ty + pad + (j / cols) * (CELL_H + gap);
                boolean picked = CommandSelection.SHAPES[j].equals(CommandMode.formation);
                boolean hov = over(mouse, cx, cy, cw, CELL_H, dx);
                double fl = flashLeft(CELL + j);
                if (picked) g.fill(cx, cy, cx + cw, cy + CELL_H, 0x4d000000 | GOLD);
                else if (hov) g.fill(cx, cy, cx + cw, cy + CELL_H, 0x26000000 | GOLD);
                if (fl > 0) g.fill(cx, cy, cx + cw, cy + CELL_H, ((int) Math.round(fl * 0.5 * 255) << 24) | GOLD);
                if (picked) RenderSystem.setShaderColor(((GOLD >> 16) & 255) / 255f, ((GOLD >> 8) & 255) / 255f, (GOLD & 255) / 255f, 1f);
                // the key number in its own row at the top-left, the shape below it, the name at the foot: nothing overlaps
                UiTex.draw(g, SHAPE_ICONS[j], cx + (cw - 24) / 2, cy + 9);
                RenderSystem.setShaderColor(1, 1, 1, 1);
                String name = SHAPE_NAMES[j];
                int ink = 0xff000000 | (picked ? GOLD : CREAM);
                g.drawText(font, name, cx + (cw - font.getWidth(name) + 1) / 2, cy + CELL_H - 9, ink, true);
                g.drawText(font, String.valueOf(j + 1), cx + 2, cy + 1, 0xcc000000 | (picked ? GOLD : CREAM), true);
                record(CELL + j, cx, cy, cw, CELL_H, dx);
            }
        }
    }

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
        // the tray lies over the pennants' right ends, so it is hit first
        List<Map.Entry<Integer, int[]>> order = new ArrayList<>(hits.entrySet());
        order.sort(Comparator.comparingInt(e -> e.getKey() >= TRAY && e.getKey() < SWEEP ? 0 : 1));
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
        return spec == null ? null : spec.group();
    }
}
