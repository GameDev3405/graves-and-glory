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
import net.minecraft.client.resource.language.I18n;

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

    /** A moved part: its anchors and offsets (shares of the screen's width and height). */
    public record Place(String h, String v, double x, double y) {}

    static final Map<String, Place> places = new LinkedHashMap<>();
    /** This frame's box of each part {x, y, w, h}: canvas px, GUI px for relics; and whether the part was drawn (false: an edit-mode ghost). */
    static final Map<String, int[]> boxes = new LinkedHashMap<>();
    static final Map<String, Boolean> live = new LinkedHashMap<>();

    static String loadedFor;
    public static int loads, saves;
    /** The part being dragged, or null; the grab point inside it and its size, in its own space. */
    static String dragging;
    static double grabX, grabY;

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

    /** Back to the default layout: every part where UiMath.hudLayout puts it. */
    public static void reset() { places.clear(); dragging = null; }

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
    static void record(String part, double left, double top, int w, int h, int W, int H) {
        left = clamp(left, MARGIN, W - MARGIN - w); top = clamp(top, MARGIN, H - MARGIN - h);
        double cx = left + w / 2.0, cy = top + h / 2.0;
        String ah = cx < W / 3.0 ? "left" : cx > 2 * W / 3.0 ? "right" : "center", av = cy < H / 3.0 ? "top" : cy > 2 * H / 3.0 ? "bottom" : "middle";
        double ox = switch (ah) { case "left" -> left - MARGIN; case "right" -> W - MARGIN - w - left; default -> cx - W / 2.0; };
        double oy = switch (av) { case "top" -> top - MARGIN; case "bottom" -> H - MARGIN - h - top; default -> cy - H / 2.0; };
        places.put(part, new Place(ah, av, round(ox / Math.max(1, W)), round(oy / Math.max(1, H))));
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
        boxes.keySet().removeIf(p -> !p.equals("relics")); live.keySet().removeIf(p -> !p.equals("relics"));
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
            int[] t = target(part, u[2], u[3], W, H);
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

    /** The relic slots' box in GUI px on a sw x sh GUI (the union of UiMath.relicAt's three 50 x 32 slots, at their place). */
    static int[] relicsBox(int sw, int sh) {
        int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE;
        for (int i = 0; i < 3; i++) { int[] a = UiMath.relicAt(i, sw, sh); x0 = Math.min(x0, a[0]); y0 = Math.min(y0, a[1]); x1 = Math.max(x1, a[0] + 50); y1 = Math.max(y1, a[1] + 32); }
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /** How far the relic slots move from where UiMath.relicAt puts them, GUI px {dx, dy}; records their box for edit mode. BattleHud.relicSlot adds it. */
    public static int[] relicShift(int sw, int sh) {
        ensureLoaded(MinecraftClient.getInstance());
        int[] u = relicsBox(sw, sh), t = target("relics", u[2], u[3], sw, sh);
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
        String key = "gui.gravesandglory.hud_edit." + part;
        return I18n.hasTranslation(key) ? I18n.translate(key) : part;
    }

    /** Edit mode, after the HUD is drawn: each part's dashed outline and its name (dim for a part not on screen now), the one under the cursor or dragged lit. */
    public static void drawEdit(DrawContext g, TextRenderer font, int sw, int sh, double[] mouse) {
        if (!CommandPanel.editing()) return;
        relicShift(sw, sh); // the relics' box on this GUI, drawn or not
        String hot = dragging != null ? dragging : at(mouse);
        List<int[]> tags = new ArrayList<>();
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
            // the name on a dark tag in the box's top-left corner
            String name = label(part);
            int tw = font.getWidth(name) + 4, tx = Math.max(0, Math.min(sw - tw, x0 + 1)), ty = Math.max(0, Math.min(sh - 10, y0 + 1));
            // another part's tag already there: the box's foot, then its top right, so every name stays readable
            int[][] tries = {{tx, ty}, {tx, Math.max(0, Math.min(sh - 10, y1 - 11))}, {Math.max(0, Math.min(sw - tw, x1 - 1 - tw)), ty}, {Math.max(0, Math.min(sw - tw, x1 - 1 - tw)), Math.max(0, Math.min(sh - 10, y1 - 11))}};
            for (int[] t : tries) { boolean free = true; for (int[] o : tags) if (t[0] < o[0] + o[2] && o[0] < t[0] + tw && t[1] < o[1] + 10 && o[1] < t[1] + 10) free = false; if (free) { tx = t[0]; ty = t[1]; break; } }
            tags.add(new int[]{tx, ty, tw});
            g.fill(tx, ty, tx + tw, ty + 10, lit ? 0xe0000000 : 0xb0000000);
            g.drawText(font, name, tx + 2, ty + 1, ink, false);
        }
    }

    /** The part under a GUI point, smallest first, or null. */
    static String at(double[] m) {
        if (m == null) return null;
        for (String part : PARTS) { int[] b = guiBox(part); if (b != null && m[0] >= b[0] && m[0] < b[0] + b[2] && m[1] >= b[1] && m[1] < b[1] + b[3]) return part; }
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

    /** The cursor moved in edit mode (GUI px): the dragged part follows, clamped on screen. */
    public static void drag(double gx, double gy) {
        if (dragging == null) return;
        int[] b = boxes.get(dragging);
        if (b == null) return;
        double[] p = toSpace(dragging, gx, gy);
        int[] wh = space(dragging);
        record(dragging, p[0] - grabX, p[1] - grabY, b[2], b[3], wh[0], wh[1]);
        int[] t = target(dragging, b[2], b[3], wh[0], wh[1]);
        if (t != null) boxes.put(dragging, t); // this frame's outline follows at once
    }

    public static void release() { dragging = null; }

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
    public static void forget() { loadedFor = null; dragging = null; boxes.clear(); live.clear(); }

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
                if (!validH(h) || !validV(v) || !Double.isFinite(x) || !Double.isFinite(y)) continue;
                places.put(part.getKey(), new Place(h, v, clamp(x, -1, 1), clamp(y, -1, 1))); // place() clamps it on screen besides
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
