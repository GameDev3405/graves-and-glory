package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Where the player has moved each part of the battle HUD (Kort's playtest, 2026-11-12: every HUD object movable, not only the command
 * panel). The command panel's edit mode (PanelLayout, CommandPanel.editing) moves these too: each part is outlined there with its name,
 * and a drag moves it. Client only; no gameplay, save or network change.
 *
 * <p>The parts and the HUD rects (BattleHud.rects keys) each carries: score (score, dawn-boss, dusk-boss; Convoy's road strip hangs from the
 * score card), objectives (seal-(i), aim-name), gold, houses (trait-(house)), column (spine, card-(group), hints), feed, lesson, respawn,
 * all on the HUD canvas (UiCanvas.hudK), and relics (the three relic slots beside the hotbar, in vanilla GUI px).
 *
 * <p>A moved part is kept as an anchor and a normalised offset, so it holds its place across GUI Scale, HUD size and window size: h is
 * left, center or right (the third of the screen its centre was dropped in) and x the gap from that edge (or from the centre, signed) as a
 * share of the screen's width; v and y alike down the screen. A part not in the file stays where UiMath.hudLayout puts it. Every frame
 * (place) a moved part is clamped wholly on screen, so neither a drag, a file nor a resize can put it off screen.
 *
 * <p>Kept per player in config/gravesandglory/hud-layout.json as command-panel.json is kept:
 * {"version":1,"profiles":{"&lt;uuid&gt;":{"gold":{"h":"left","v":"top","x":0.05,"y":0.1},...}}}. Read on first use per player, written
 * when edit mode is left (CommandPanel.leaveEdit), forgotten on a disconnect. A missing or corrupt file means the defaults.
 */
public final class HudLayout {
    private HudLayout() {}

    /** The movable parts, in the order a press tries them (small parts first, so a part lying over a larger one stays reachable). */
    public static final List<String> PARTS = List.of("relics", "respawn", "lesson", "gold", "houses", "objectives", "feed", "score", "column");
    /** Canvas px (GUI px for the relics) a part keeps from the screen's edges. */
    public static final int MARGIN = 2;

    /** A moved part: its anchors and offsets (shares of the screen's width and height) and its size (1 = as UiMath.hudLayout draws it). */
    public record Place(String h, String v, double x, double y, double s) {
        public Place(String h, String v, double x, double y) { this(h, v, x, y, 1); }
    }

    static final Map<String, Place> places = new LinkedHashMap<>();
    /** This frame's box of each part {x, y, w, h}: canvas px, GUI px for relics; and whether the part was drawn (false: an edit-mode ghost). */
    static final Map<String, int[]> boxes = new LinkedHashMap<>();
    static final Map<String, Boolean> live = new LinkedHashMap<>();

    static String loadedFor;
    public static int loads, saves;
    /** The part being dragged, or null; the grab point inside it and its size, in its own space. */
    static String dragging;
    static double grabX, grabY;
    /** The part being resized by its corner handle, or null; the corner that stays put (its own space), the handle's side (+1 right or
     *  down, -1 left or up) and the part's unscaled size. */
    static String resizing;
    static double fixX, fixY, baseW, baseH; static int handX, handY;
    /** This frame's unscaled size {w, h} of each part (canvas px, GUI px for the relics) and the scale it is drawn at around its box's top
     *  left {x, y, s} (same space): what drawPart applies. */
    static final Map<String, double[]> base = new LinkedHashMap<>(), xf = new LinkedHashMap<>();
    /** The part drawPart is drawing now, or null. */
    static String drawingPart;
    /** A corner handle's size, GUI px. */
    public static final int HANDLE = 9;

    public static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("gravesandglory").resolve("hud-layout.json"); }

    /** The part a HUD rect key belongs to, or null. */
    public static String partOf(String key) {
        if (key.equals("score") || key.equals("dawn-boss") || key.equals("dusk-boss") || key.equals("road")) return "score";
        if (key.startsWith("seal-") || key.equals("aim-name")) return "objectives";
        if (key.equals("gold")) return "gold";
        if (key.startsWith("trait-")) return "houses";
        if (key.equals("spine") || key.startsWith("card-") || key.equals("hints") || key.equals("keys")) return "column";
        if (key.equals("feed") || key.equals("lesson") || key.equals("respawn")) return key;
        return null;
    }

    public static boolean moved(String part) { return places.containsKey(part); }
    public static Place place(String part) { return places.get(part); }
    public static Map<String, int[]> boxes() { return Collections.unmodifiableMap(boxes); }
    public static boolean drawn(String part) { return Boolean.TRUE.equals(live.get(part)); }
    public static String dragging() { return dragging; }

    /** Back to the default layout: every part where UiMath.hudLayout puts it, at its own size. */
    public static void reset() { places.clear(); dragging = null; resizing = null; }

    /** A part's saved size (1 when it has none). */
    public static double scale(String part) { Place p = places.get(part); return p == null ? 1 : p.s(); }
    /** The size a part is drawn at this frame (its saved size, smaller where the screen has no room); 1 for one at its own size. */
    public static double drawnScale(String part) { double[] x = xf.get(part); return x == null ? 1 : x[2]; }
    public static String resizing() { return resizing; }

    /** The largest size at which a w x h piece still fits a W x H screen inside the margins. */
    static double fit(double w, double h, int W, int H, int margin) { return Math.min((W - 2.0 * margin) / Math.max(1, w), (H - 2.0 * margin) / Math.max(1, h)); }

    // ---- placing: every frame ----

    /** The box a part of size w x h takes on a W x H screen: its place resolved and clamped wholly on screen; null when not moved. */
    static int[] target(String part, int w, int h, int W, int H) {
        Place p = places.get(part);
        if (p == null) return null;
        double x = switch (p.h()) { case "left" -> MARGIN + p.x() * W; case "right" -> W - MARGIN - w - p.x() * W; default -> W / 2.0 - w / 2.0 + p.x() * W; };
        double y = switch (p.v()) { case "top" -> MARGIN + p.y() * H; case "bottom" -> H - MARGIN - h - p.y() * H; default -> H / 2.0 - h / 2.0 + p.y() * H; };
        return new int[]{(int) Math.round(clamp(x, MARGIN, W - MARGIN - w)), (int) Math.round(clamp(y, MARGIN, H - MARGIN - h)), w, h};
    }

    /** Records a part dropped with its box's top left at (left, top) on a W x H screen: clamped on screen, anchored by the third its centre is in. */
    static void record(String part, double left, double top, int w, int h, int W, int H) { record(part, left, top, w, h, W, H, scale(part)); }

    static void record(String part, double left, double top, int w, int h, int W, int H, double scale) {
        left = clamp(left, MARGIN, W - MARGIN - w); top = clamp(top, MARGIN, H - MARGIN - h);
        double cx = left + w / 2.0, cy = top + h / 2.0;
        String ah = cx < W / 3.0 ? "left" : cx > 2 * W / 3.0 ? "right" : "center", av = cy < H / 3.0 ? "top" : cy > 2 * H / 3.0 ? "bottom" : "middle";
        double ox = switch (ah) { case "left" -> left - MARGIN; case "right" -> W - MARGIN - w - left; default -> cx - W / 2.0; };
        double oy = switch (av) { case "top" -> top - MARGIN; case "bottom" -> H - MARGIN - h - top; default -> cy - H / 2.0; };
        places.put(part, new Place(ah, av, round(ox / Math.max(1, W)), round(oy / Math.max(1, H)), scale));
    }

    static double round(double v) { return Math.round(v * 100000) / 100000.0; }

    /** The union of a layout's rects that belong to a part, or null. */
    static int[] union(Map<String, int[]> at, String part) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (var e : at.entrySet()) {
            int[] b = e.getValue();
            if (b == null || b.length < 4 || !part.equals(partOf(e.getKey()))) continue;
            x0 = Math.min(x0, b[0]); y0 = Math.min(y0, b[1]); x1 = Math.max(x1, b[0] + b[2]); y1 = Math.max(y1, b[1] + b[3]);
        }
        return x0 == Integer.MAX_VALUE ? null : new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /**
     * Moves this frame's layout (UiMath.hudLayout, canvas px) to the player's places, in place: each moved part's rects shift together,
     * clamped on the W x H canvas. In edit mode, parts this frame does not draw (the feed, the lesson, the respawn card...) get a ghost box
     * from the layout of a fuller state, so the player can move them too. Called by BattleHud.render before it draws.
     */
    public static void place(Map<String, int[]> at, int fbW, int fbH, int gui, int k, int size, UiMath.HudState s) {
        ensureLoaded(MinecraftClient.getInstance());
        int W = fbW / Math.max(1, k), H = fbH / Math.max(1, k);
        boxes.keySet().removeIf(p -> !p.equals("relics")); live.keySet().removeIf(p -> !p.equals("relics")); xf.keySet().removeIf(p -> !p.equals("relics"));
        Map<String, int[]> ghost = null;
        if (CommandPanel.editing()) {
            List<String> groups = s.groups().isEmpty() ? UiMath.DEFAULT_STATE.groups() : s.groups();
            ghost = new LinkedHashMap<>(UiMath.hudLayout(fbW, fbH, gui, k, size, new UiMath.HudState(s.points(), groups, s.selected(), true, 2, false, true, s.houses(), s.road())));
            var withRespawn = UiMath.hudLayout(fbW, fbH, gui, k, size, new UiMath.HudState(s.points(), groups, s.selected(), false, 0, true, false, s.houses(), s.road()));
            ghost.put("respawn", withRespawn.get("respawn"));
        }
        for (String part : PARTS) {
            if (part.equals("relics")) continue;
            int[] u = union(at, part);
            boolean drawn = u != null;
            if (u == null && ghost != null) u = union(ghost, part);
            if (u == null) continue;
            double sc = Math.min(scale(part), fit(u[2], u[3], W, H, MARGIN));
            int[] t = target(part, (int) Math.round(u[2] * sc), (int) Math.round(u[3] * sc), W, H);
            base.put(part, new double[]{u[2], u[3]});
            if (t != null) xf.put(part, new double[]{t[0], t[1], sc}); else xf.remove(part);
            if (t != null && drawn) {
                int dx = t[0] - u[0], dy = t[1] - u[1];
                if (dx != 0 || dy != 0) for (var e : at.entrySet()) {
                    int[] b = e.getValue();
                    if (b != null && b.length >= 4 && part.equals(partOf(e.getKey()))) e.setValue(new int[]{b[0] + dx, b[1] + dy, b[2], b[3]});
                }
            }
            boxes.put(part, t != null ? t : u);
            live.put(part, drawn);
        }
    }

    // ---- drawing a part at its size ----

    /**
     * Draws one part (BattleHud's call for it) at the player's size: a matrix scale around the part's box's top left, so its art and
     * text keep their shapes; then the rects the call recorded in each map (canvas px) are moved to where they show, so hit tests, the
     * chat's room and the checks see the part as drawn. The relics draw in GUI px; their rects are canvas px all the same.
     */
    @SafeVarargs
    public static void drawPart(DrawContext g, String part, Runnable draw, Map<String, int[]>... recorded) {
        double[] x = xf.get(part);
        if (x == null || x[2] == 1) { draw.run(); return; }
        List<Map<String, int[]>> before = new ArrayList<>();
        for (var m : recorded) before.add(new HashMap<>(m));
        g.getMatrices().push();
        g.getMatrices().translate(x[0], x[1], 0);
        g.getMatrices().scale((float) x[2], (float) x[2], 1);
        g.getMatrices().translate(-x[0], -x[1], 0);
        drawingPart = part;
        try { draw.run(); }
        finally { g.getMatrices().pop(); drawingPart = null; }
        double a = part.equals("relics") ? MinecraftClient.getInstance().getWindow().getScaleFactor() / UiCanvas.hudK() : 1; // GUI px to canvas px
        for (int i = 0; i < recorded.length; i++)
            for (var e : recorded[i].entrySet()) {
                int[] b = e.getValue();
                if (b == null || b.length < 4 || before.get(i).get(e.getKey()) == b) continue; // only what this call put
                e.setValue(scaleRect(b, x[0] * a, x[1] * a, x[2]));
            }
    }

    /** A rect scaled by s around (ax, ay), rounded outward. */
    static int[] scaleRect(int[] b, double ax, double ay, double s) {
        int x0 = (int) Math.floor(ax + (b[0] - ax) * s + 1e-9), y0 = (int) Math.floor(ay + (b[1] - ay) * s + 1e-9);
        int x1 = (int) Math.ceil(ax + (b[0] + b[2] - ax) * s - 1e-9), y1 = (int) Math.ceil(ay + (b[1] + b[3] - ay) * s - 1e-9);
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /** A point where it shows, back in the coordinates the part being drawn now (drawPart) is drawn in: a hover test inside it. */
    public static double[] unscale(double[] m) {
        double[] x = drawingPart == null ? null : xf.get(drawingPart);
        if (m == null || x == null || x[2] == 1) return m;
        return new double[]{x[0] + (m[0] - x[0]) / x[2], x[1] + (m[1] - x[1]) / x[2]};
    }

    /** The relic slots' box in GUI px on a sw x sh GUI (the union of UiMath.relicAt's three 50 x 32 slots, at their place). */
    static int[] relicsBox(int sw, int sh) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (int i = 0; i < 3; i++) { int[] a = UiMath.relicAt(i, sw, sh); x0 = Math.min(x0, a[0]); y0 = Math.min(y0, a[1]); x1 = Math.max(x1, a[0] + 50); y1 = Math.max(y1, a[1] + 32); }
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /** How far the relic slots move from where UiMath.relicAt puts them, GUI px {dx, dy}; records their box for edit mode. BattleHud.relicSlot adds it. */
    public static int[] relicShift(int sw, int sh) {
        ensureLoaded(MinecraftClient.getInstance());
        int[] u = relicsBox(sw, sh);
        double sc = Math.min(scale("relics"), fit(u[2], u[3], sw, sh, MARGIN));
        int[] t = target("relics", (int) Math.round(u[2] * sc), (int) Math.round(u[3] * sc), sw, sh);
        base.put("relics", new double[]{u[2], u[3]});
        if (t != null) xf.put("relics", new double[]{t[0], t[1], sc}); else xf.remove("relics");
        boxes.put("relics", t != null ? t : u);
        live.put("relics", !GravesAndGloryClient.abilityId.equals("none") && !GravesAndGloryClient.abilityId.isEmpty());
        return t == null ? new int[]{0, 0} : new int[]{t[0] - u[0], t[1] - u[1]};
    }

    // ---- edit mode: outlines and drags, in GUI px ----

    /** GUI px per canvas px. */
    static double kGui() { return UiCanvas.hudK() / MinecraftClient.getInstance().getWindow().getScaleFactor(); }

    /** A part's box this frame in GUI px, or null. */
    public static int[] guiBox(String part) {
        int[] b = boxes.get(part);
        if (b == null) return null;
        if (part.equals("relics")) return b.clone();
        double k = kGui();
        int x0 = (int) Math.floor(b[0] * k), y0 = (int) Math.floor(b[1] * k);
        return new int[]{x0, y0, (int) Math.ceil((b[0] + b[2]) * k) - x0, (int) Math.ceil((b[1] + b[3]) * k) - y0};
    }

    static String label(String part) {
        return switch (part) {
            case "score" -> UiText.t("hudLayout.part.score", "Score");
            case "objectives" -> UiText.t("hudLayout.part.objectives", "Objectives");
            case "gold" -> UiText.t("hudLayout.part.gold", "Gold");
            case "houses" -> UiText.t("hudLayout.part.houses", "House traits");
            case "column" -> UiText.t("hudLayout.part.column", "Army column");
            case "feed" -> UiText.t("hudLayout.part.feed", "Battle feed");
            case "lesson" -> UiText.t("hudLayout.part.lesson", "Lesson");
            case "respawn" -> UiText.t("hudLayout.part.respawn", "Respawn");
            case "relics" -> UiText.t("hudLayout.part.relics", "Relics");
            default -> part;
        };
    }

    /** Edit mode, after the HUD is drawn: each part's dashed outline and its name (dim for a part not on screen now), the one under the cursor or dragged lit. */
    /** The name tags drawn last frame in edit mode, part to (x, y, w, h) in GUI px (for the checks). */
    public static final Map<String, int[]> lastTags = new LinkedHashMap<>();
    /** The leaders drawn last frame, part to its one or two segments (x, y, w, h) in GUI px (for the checks). */
    public static final Map<String, int[][]> lastLeaders = new LinkedHashMap<>();

    /** A chip's leader to its box's nearest edge as two thin segments (across, then up or down), or null when the chip is within 12 px. */
    static int[][] leader(int[] t, int tw, int x0, int y0, int x1, int y1) {
        int cx = Math.max(x0, Math.min(x1 - 1, t[0] + tw / 2)), cy = Math.max(y0, Math.min(y1 - 1, t[1] + 5)), fx = t[0] + tw / 2, fy = t[1] + 5;
        if (Math.abs(cx - fx) + Math.abs(cy - fy) <= 12) return null;
        // both segments stop one pixel short of the box's edge point (cx, cy): it lies on the own box, which may overlap another part
        if (fy == cy) return new int[][]{{fx < cx ? fx : cx + 1, fy, Math.abs(fx - cx), 1}};
        return new int[][]{{Math.min(fx, cx), fy, Math.abs(fx - cx) + 1, 1}, {cx, fy < cy ? fy : cy + 1, 1, Math.abs(fy - cy)}};
    }
    static boolean rectsMeet(int[] a, int[] b) { return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3]; }
    /** Whether a leader (leader) crosses any of these rects. */
    static boolean hitsAny(int[][] lead, List<int[]> rects) {
        if (lead == null) return false;
        for (int[] l : lead) for (int[] o : rects) if (rectsMeet(l, o)) return true;
        return false;
    }

    public static void drawEdit(DrawContext g, TextRenderer font, int sw, int sh, double[] mouse, int[]... keepClear) {
        if (!CommandPanel.editing()) return;
        relicShift(sw, sh); // the relics' box on this GUI, drawn or not
        String hot = dragging != null ? dragging : at(mouse);
        List<int[]> tags = new ArrayList<>();
        lastTags.clear(); lastLeaders.clear(); lastHandles.clear(); if (!CommandPanel.resizing()) lastSizeLabel = ""; // the panel draws its own just before
        List<int[]> clear = new ArrayList<>(); // the panel's outline, caption and toolbar (x, y, w, h): no name tag may cover them
        for (int[] k : keepClear) if (k != null) clear.add(k);
        for (String part : PARTS) {
            int[] b = guiBox(part);
            if (b == null) continue;
            boolean lit = part.equals(hot), shown = drawn(part);
            int x0 = Math.max(0, b[0] - 1), y0 = Math.max(0, b[1] - 1), x1 = Math.min(sw, b[0] + b[2] + 1), y1 = Math.min(sh, b[1] + b[3] + 1);
            if (!shown) g.fill(x0, y0, x1, y1, 0x40000000);
            if (lit) g.fill(x0, y0, x1, y1, 0x30ffd27a);
            int ink = lit ? 0xffffd27a : shown ? 0xffe6c46a : 0xb0e6c46a;
            for (int x = x0; x < x1; x += 4) { g.fill(x, y0, Math.min(x + 2, x1), y0 + 1, ink); g.fill(x, y1 - 1, Math.min(x + 2, x1), y1, ink); }
            for (int y = y0; y < y1; y += 4) { g.fill(x0, y, x0 + 1, Math.min(y + 2, y1), ink); g.fill(x1 - 1, y, x1, Math.min(y + 2, y1), ink); }
            // the name on a small dark chip outside the box, so it never covers the part's own content (tag)
            String name = label(part);
            int tw = font.getWidth(name) + 4;
            int[] t = tag(part, tw, x0, y0, x1, y1, sw, sh, tags, clear);
            int tx = t[0], ty = t[1];
            // a chip set away from its box keeps a thin leader to the box's nearest edge; none within 12 px, and none that would cross the
            // panel, its toolbar or the vanilla HUD (tag tries for a spot whose leader is clear first)
            int[][] lead = leader(t, tw, x0, y0, x1, y1);
            if (lead != null && !hitsAny(lead, clear)) { for (int[] l : lead) g.fill(l[0], l[1], l[0] + l[2], l[1] + l[3], ink); lastLeaders.put(part, lead); }
            tags.add(new int[]{tx - 4, ty - 1, tw + 8, 12}); // with a gap, so two chips never read as one name
            lastTags.put(part, new int[]{tx, ty, tw, 10});
            g.fill(tx, ty, tx + tw, ty + 10, lit ? 0xe0000000 : 0xb0000000);
            g.drawText(font, name, tx + 2, ty + 1, ink, false);
            // the corner handle (the corner facing the screen's centre): a drag there resizes the part
            int[] hb = handleBox(part, sw, sh);
            if (hb != null) {
                boolean on = part.equals(resizing) || mouse != null && onHandle(hb, mouse[0], mouse[1]);
                drawHandle(g, hb, on);
                lastHandles.put(part, hb);
                if (part.equals(resizing)) sizeLabel(g, font, hb, drawnScale(part), sw, sh);
            }
        }
    }

    /** The sizes a handle drag clings to, and how near (as a size) it must come. */
    static final double[] MAGNETS = {1, 1.5, 2};
    static final double MAGNET = 0.04;

    /** The handles drawn last frame in edit mode, part to its box (GUI px), for the checks. */
    public static final Map<String, int[]> lastHandles = new LinkedHashMap<>();

    /** A part's corner handle in GUI px (HANDLE square inside the corner of its box that faces the screen's centre), or null. */
    public static int[] handleBox(String part, int sw, int sh) {
        int[] b = guiBox(part);
        if (b == null) return null;
        return corner(b, sw, sh, HANDLE);
    }

    /** The n x n square inside the corner of box b {x, y, w, h} that faces the centre of a sw x sh screen. */
    public static int[] corner(int[] b, int sw, int sh, int n) {
        boolean right = b[0] + b[2] / 2.0 < sw / 2.0, down = b[1] + b[3] / 2.0 < sh / 2.0;
        n = Math.max(5, Math.min(n, Math.min(b[2], b[3]) / 3)); // never larger than a third of a small part
        int x = right ? b[0] + b[2] : b[0] - n, y = down ? b[1] + b[3] : b[1] - n; // just outside the corner: it covers none of the part
        return new int[]{Math.max(0, Math.min(sw - n, x)), Math.max(0, Math.min(sh - n, y)), n, n};
    }

    /** A press or hover on handle h (GUI px): the square grown to at least 11 px, easy to hit at GUI Scale 1 when drawn small. */
    public static boolean onHandle(int[] h, double x, double y) {
        if (h == null) return false;
        int p = Math.max(1, (11 - h[2] + 1) / 2);
        return x >= h[0] - p && x < h[0] + h[2] + p && y >= h[1] - p && y < h[1] + h[3] + p;
    }

    /** A corner handle: a brass square with a dark core and a light diagonal, brighter under the cursor or while dragged. */
    public static void drawHandle(DrawContext g, int[] h, boolean on) {
        g.fill(h[0], h[1], h[0] + h[2], h[1] + h[3], on ? 0xffffd27a : 0xffe6c46a);
        g.fill(h[0] + 1, h[1] + 1, h[0] + h[2] - 1, h[1] + h[3] - 1, 0xff1b120c);
        for (int i = 1; i < h[2] - 1; i++) g.fill(h[0] + i, h[1] + h[3] - 1 - i, h[0] + i + 1, h[1] + h[3] - i, on ? 0xffffd27a : 0xffe6c46a);
    }

    /** The size while a handle is dragged ("120%"), on a dark chip beside the handle, kept on screen. */
    public static void sizeLabel(DrawContext g, TextRenderer font, int[] h, double scale, int sw, int sh) {
        String t = UiText.t("hudLayout.sizeLabel", "{percent}%", "percent", Math.round(scale * 100));
        int w = font.getWidth(t) + 4, x = Math.max(0, Math.min(sw - w, h[0] + h[2] + 2)), y = Math.max(0, Math.min(sh - 10, h[1] + h[3] + 2));
        if (x < h[0] + h[2] && y < h[1] + h[3]) y = Math.max(0, h[1] - 12);
        g.fill(x, y, x + w, y + 10, 0xe0000000);
        g.drawText(font, t, x + 2, y + 1, 0xffffd27a, false);
        lastSizeLabel = t;
    }
    /** The size label the last frame showed while a handle was dragged ("" when none), for the checks. */
    public static String lastSizeLabel = "";

    /**
     * Where a part's name chip (tw x 10) goes: above the box, then below it, then to its left or right, each at the box's left and right
     * ends; a spot must be on screen and clear of the tags already placed and of the panel (its outline, caption and toolbar). First a spot
     * clear of every part's box, then one clear of the own box only; only when nothing outside fits, a corner inside the box.
     */
    static int[] tag(String part, int tw, int x0, int y0, int x1, int y1, int sw, int sh, List<int[]> tags, List<int[]> clear) {
        List<int[]> others = new ArrayList<>();
        for (String o : PARTS) { int[] ob = guiBox(o); if (ob != null && !o.equals(part)) others.add(new int[]{ob[0] - 1, ob[1] - 1, ob[2] + 2, ob[3] + 2}); }
        int[] own = {x0, y0, x1 - x0, y1 - y0};
        int far = Math.max(sw, sh) / 3;
        // rings ever further out: at gap d, the row above the box (left to right), the row below, the column left of it, the column right of it;
        // the chip off every part: 0 with its leader off every part too, 1 with a leader off the panel, toolbar, vanilla HUD and chips,
        // 2 with no leader (drawEdit draws none that would cross the panel or the vanilla HUD); then, only if none fits, the chip on another
        // part: 3 with a clear leader, 4 with none
        for (int pass = 0; pass < 5; pass++)
            for (int d = 0; d <= far; d += 4) {
                List<int[]> ring = new ArrayList<>();
                for (int y : new int[]{y0 - 11 - d, y1 + 1 + d}) for (int x = x0 - d; x <= Math.max(x0 - d, x1 + d - tw); x += 4) ring.add(new int[]{x, y});
                for (int x : new int[]{x0 - tw - 1 - d, x1 + 1 + d}) for (int y = y0 - 11 - d; y <= y1 + 1 + d; y += 4) ring.add(new int[]{x, y});
                for (int[] t : ring) {
                    if (t[0] < 0 || t[1] < 0 || t[0] + tw > sw || t[1] + 10 > sh) continue;
                    if (meets(t, tw, own) || hits(t, tw, tags) || hits(t, tw, clear) || (pass < 3 && hits(t, tw, others))) continue;
                    int[][] lead = pass == 2 || pass == 4 ? null : leader(t, tw, x0, y0, x1, y1);
                    if (lead != null && (hitsAny(lead, clear) || hitsAny(lead, tags) || (pass == 0 && hitsAny(lead, others)))) continue;
                    return t;
                }
            }
        int left = Math.max(0, Math.min(sw - tw, x0 + 1)), right = Math.max(0, Math.min(sw - tw, x1 - 1 - tw)), top = Math.max(0, Math.min(sh - 10, y0 + 1)), foot = Math.max(0, Math.min(sh - 10, y1 - 11));
        int[][] inside = {{left, top}, {left, foot}, {right, top}, {right, foot}};
        for (int[] t : inside) if (!hits(t, tw, tags) && !hits(t, tw, clear)) return t;
        return inside[0];
    }
    static boolean meets(int[] t, int tw, int[] o) { return t[0] < o[0] + o[2] && o[0] < t[0] + tw && t[1] < o[1] + (o.length > 3 ? o[3] : 10) && o[1] < t[1] + 10; }
    static boolean hits(int[] t, int tw, List<int[]> rects) { for (int[] o : rects) if (meets(t, tw, o)) return true; return false; }

    /** The part under a GUI point, or null: parts drawn now first, then edit-mode ghosts (so a ghost never takes a press from a live part); smallest first within each. */
    static String at(double[] m) {
        if (m == null) return null;
        for (int pass = 0; pass < 2; pass++)
            for (String part : PARTS) { if (drawn(part) != (pass == 0)) continue; int[] b = guiBox(part); if (b != null && m[0] >= b[0] && m[0] < b[0] + b[2] && m[1] >= b[1] && m[1] < b[1] + b[3]) return part; }
        return null;
    }

    /** A left press in edit mode (GUI px) that met no toolbar control and not the panel: starts a drag of the part under it. */
    public static boolean press(double gx, double gy) {
        String part = at(new double[]{gx, gy});
        if (part == null) return false;
        int[] b = boxes.get(part);
        double[] p = toSpace(part, gx, gy);
        dragging = part; grabX = p[0] - b[0]; grabY = p[1] - b[1];
        return true;
    }

    /** A left press in edit mode on a part's corner handle (GUI px): starts a resize of that part, its opposite corner held. */
    public static boolean pressHandle(double gx, double gy) {
        var w = MinecraftClient.getInstance().getWindow();
        int sw = w.getScaledWidth(), sh = w.getScaledHeight();
        for (int pass = 0; pass < 2; pass++)
            for (String part : PARTS) {
                if (drawn(part) != (pass == 0)) continue;
                int[] h = handleBox(part, sw, sh), b = boxes.get(part);
                double[] u = base.get(part);
                if (h == null || b == null || u == null || !onHandle(h, gx, gy)) continue;
                int[] gb = guiBox(part);
                handX = h[0] > gb[0] + gb[2] / 2.0 ? 1 : -1; handY = h[1] > gb[1] + gb[3] / 2.0 ? 1 : -1;
                fixX = handX > 0 ? b[0] : b[0] + b[2]; fixY = handY > 0 ? b[1] : b[1] + b[3];
                baseW = u[0]; baseH = u[1];
                double[] p0 = toSpace(part, gx, gy); // where on the handle it was taken, from the corner it moves: no jump on the first move
                grabX = p0[0] - (handX > 0 ? b[0] + b[2] : b[0]); grabY = p0[1] - (handY > 0 ? b[1] + b[3] : b[1]);
                resizing = part; dragging = null;
                return true;
            }
        return false;
    }

    /** The size a handle drag at (px, py) asks for: the cursor's reach from the held corner along the part's diagonal, in steps, within
     *  the bounds and never larger than the screen. Shared with the command panel. */
    public static double resizeScale(double px, double py, double fx, double fy, int hx, int hy, double bw, double bh, int W, int H, int margin) {
        double dw = (px - fx) * hx, dh = (py - fy) * hy;
        double s = (dw * bw + dh * bh) / Math.max(1, bw * bw + bh * bh);
        for (double m : MAGNETS) if (Math.abs(s - m) < MAGNET) s = m; // 100%, 150% and 200% hold the handle a little
        return Math.min(PanelLayout.snapScale(s), Math.max(PanelLayout.MIN_SCALE, fit(bw, bh, W, H, margin)));
    }

    /** The cursor moved during a resize (GUI px): the part takes the size, its held corner kept, clamped on screen. */
    static void resizeTo(double gx, double gy) {
        double[] p = toSpace(resizing, gx, gy);
        p[0] -= grabX; p[1] -= grabY;
        int[] wh = space(resizing);
        double s = resizeScale(p[0], p[1], fixX, fixY, handX, handY, baseW, baseH, wh[0], wh[1], MARGIN);
        int w = (int) Math.round(baseW * s), h = (int) Math.round(baseH * s);
        record(resizing, handX > 0 ? fixX : fixX - w, handY > 0 ? fixY : fixY - h, w, h, wh[0], wh[1], s);
        int[] t = target(resizing, w, h, wh[0], wh[1]);
        if (t != null) { boxes.put(resizing, t); xf.put(resizing, new double[]{t[0], t[1], s}); } // this frame's outline follows at once
    }

    /** The cursor moved in edit mode (GUI px): the dragged part follows, clamped on screen. */
    public static void drag(double gx, double gy) {
        if (resizing != null) { resizeTo(gx, gy); return; }
        if (dragging == null) return;
        int[] b = boxes.get(dragging);
        if (b == null) return;
        double[] p = toSpace(dragging, gx, gy);
        int[] wh = space(dragging);
        record(dragging, p[0] - grabX, p[1] - grabY, b[2], b[3], wh[0], wh[1]);
        int[] t = target(dragging, b[2], b[3], wh[0], wh[1]);
        if (t != null) boxes.put(dragging, t); // this frame's outline follows at once
    }

    public static void release() { dragging = null; resizing = null; }

    static double[] toSpace(String part, double gx, double gy) {
        if (part.equals("relics")) return new double[]{gx, gy};
        double k = kGui();
        return new double[]{gx / k, gy / k};
    }

    /** The screen's size in a part's space: the HUD canvas, or the GUI for the relics. */
    static int[] space(String part) {
        var w = MinecraftClient.getInstance().getWindow();
        return part.equals("relics") ? new int[]{w.getScaledWidth(), w.getScaledHeight()} : new int[]{UiCanvas.hudWidth(), UiCanvas.hudHeight()};
    }

    // ---- the file ----

    static String profileKey(MinecraftClient c) { return c.player == null ? PanelLayout.DEFAULT : c.player.getUuid().toString(); }

    public static void ensureLoaded(MinecraftClient c) { String key = profileKey(c); if (!key.equals(loadedFor)) load(key); }

    /** Forget what was read: the next use reads the file again (a disconnect). */
    public static void forget() { loadedFor = null; dragging = null; resizing = null; boxes.clear(); live.clear(); xf.clear(); base.clear(); }

    static boolean validH(String a) { return a.equals("left") || a.equals("center") || a.equals("right"); }
    static boolean validV(String a) { return a.equals("top") || a.equals("middle") || a.equals("bottom"); }

    static void load(String key) {
        reset();
        loadedFor = key;
        loads++;
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) return;
            JsonObject profiles = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("profiles");
            if (profiles == null) return;
            JsonElement e = profiles.has(key) ? profiles.get(key) : profiles.get(PanelLayout.DEFAULT);
            if (e == null || !e.isJsonObject()) return;
            for (var part : e.getAsJsonObject().entrySet()) {
                if (!PARTS.contains(part.getKey()) || !part.getValue().isJsonObject()) continue;
                JsonObject o = part.getValue().getAsJsonObject();
                if (!o.has("h") || !o.has("v") || !o.has("x") || !o.has("y")) continue;
                String h = o.get("h").getAsString(), v = o.get("v").getAsString();
                double x = o.get("x").getAsDouble(), y = o.get("y").getAsDouble();
                double sc = 1; // a layout saved before sizes, or a size that cannot be read: its own size, the place kept
                try { if (o.has("s")) sc = o.get("s").getAsDouble(); } catch (RuntimeException bad) { sc = 1; }
                if (!validH(h) || !validV(v) || !Double.isFinite(x) || !Double.isFinite(y)) continue;
                places.put(part.getKey(), new Place(h, v, clamp(x, -1, 1), clamp(y, -1, 1), Double.isFinite(sc) ? PanelLayout.snapScale(sc) : 1)); // place() clamps it on screen besides
            }
        } catch (Exception ex) {
            reset();
            System.err.println("[Graves and Glory] The battle HUD layout could not be read, using the defaults: " + ex.getMessage());
        }
    }

    /** Writes this player's entry (an empty one after a Reset), keeping the other profiles'. */
    public static void save(MinecraftClient c) {
        String key = loadedFor != null ? loadedFor : profileKey(c);
        try {
            Path f = file();
            JsonObject root = null;
            if (Files.isRegularFile(f)) try { root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject(); } catch (Exception ignored) { }
            if (root == null) root = new JsonObject();
            root.addProperty("version", 1);
            JsonObject profiles = root.has("profiles") && root.get("profiles").isJsonObject() ? root.getAsJsonObject("profiles") : new JsonObject();
            JsonObject mine = new JsonObject();
            for (var e : places.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("h", e.getValue().h()); o.addProperty("v", e.getValue().v());
                o.addProperty("x", e.getValue().x()); o.addProperty("y", e.getValue().y());
                if (e.getValue().s() != 1) o.addProperty("s", e.getValue().s());
                mine.add(e.getKey(), o);
            }
            profiles.add(key, mine);
            root.add("profiles", profiles);
            Files.createDirectories(f.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(f, gson.toJson(root), StandardCharsets.UTF_8);
            saves++;
        } catch (Exception ex) {
            System.err.println("[Graves and Glory] The battle HUD layout could not be saved: " + ex.getMessage());
        }
    }

    static double clamp(double v, double lo, double hi) { return hi < lo ? lo : Math.max(lo, Math.min(hi, v)); }
}
