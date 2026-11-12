package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Where the command panel sits, how opaque it is and whether it hides at rest: the player's own arrangement, client only. Kept per player
 * profile on this machine in config/gravesandglory/command-panel.json, keyed by player UUID ("default" when no entry matches):
 * {"version":1,"profiles":{"&lt;uuid&gt;":{"anchor":"top-right","x":12.0,"y":30.0,"opacity":0.75,"hidden":false,"scale":1.2}}} (no scale: 1). anchor is the
 * screen corner the panel is nearest ("default" for the left-edge layout) and x, y the gap from that corner to the panel's bounds in HUD
 * canvas pixels (UiCanvas.hudK), so a resize keeps it in the same corner; CommandPanel clamps it on screen every frame. A missing or
 * corrupt file means the defaults. Written only when edit mode is left (CommandPanel.leaveEdit).
 */
public final class PanelLayout {
    private PanelLayout() {}

    public static final String DEFAULT = "default";
    public static final float MIN_OPACITY = 0.20f, STEP = 0.05f;
    /** A moved panel keeps this many GUI px from the screen edges, so its edit outline (1 px outside) stays on screen. */
    public static final int MARGIN = 2;

    /** "default" (the left-edge layout) or top-left, top-right, bottom-left, bottom-right. */
    public static String anchor = DEFAULT;
    /** The gap from the anchor corner to the panel's bounds, canvas px. */
    public static double offX, offY;
    public static float opacity = 1f;
    public static boolean hidden;
    /** The panel's size (1 = as laid out), set by its corner handle in edit mode; the HUD parts keep theirs in HudLayout. */
    public static double scale = 1;
    /** The sizes a corner handle allows, in 5% steps: every part and the panel carry text, which stops reading below 70%. */
    public static final double MIN_SCALE = 0.7, MAX_SCALE = 2, SCALE_STEP = 0.05;

    /** The profile key the settings in memory came from (null: read the file again before use); probes for the checks. */
    static String loadedFor;
    public static int loads, saves;

    public static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("gravesandglory").resolve("command-panel.json"); }

    public static boolean moved() { return !DEFAULT.equals(anchor); }

    /** The defaults: the left edge, fully opaque, shown. */
    public static void reset() { anchor = DEFAULT; offX = offY = 0; opacity = 1f; hidden = false; scale = 1; }

    /** A size snapped to 5% steps within 70% to 200%. */
    public static double snapScale(double v) {
        if (!Double.isFinite(v)) return 1;
        double s = Math.round(v / SCALE_STEP) * SCALE_STEP;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.round(s * 100) / 100.0));
    }

    static String profileKey(MinecraftClient c) { return c.player == null ? DEFAULT : c.player.getUuid().toString(); }

    /** Reads the file for this player unless it was read for them already. */
    public static void ensureLoaded(MinecraftClient c) {
        String key = profileKey(c);
        if (!key.equals(loadedFor)) load(key);
    }

    /** Forget what was read: the next use reads the file again (a disconnect, so a rejoin reloads). */
    public static void forget() { loadedFor = null; }

    static void load(String key) {
        reset();
        loadedFor = key;
        loads++;
        try {
            Path f = file();
            if (!Files.isRegularFile(f)) return;
            JsonObject profiles = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("profiles");
            if (profiles == null) return;
            JsonElement e = profiles.has(key) ? profiles.get(key) : profiles.get(DEFAULT);
            if (e == null || !e.isJsonObject()) return;
            JsonObject o = e.getAsJsonObject();
            String a = o.has("anchor") ? o.get("anchor").getAsString() : DEFAULT;
            double x = o.has("x") ? o.get("x").getAsDouble() : 0, y = o.has("y") ? o.get("y").getAsDouble() : 0;
            double op = o.has("opacity") ? o.get("opacity").getAsDouble() : 1;
            boolean hide = o.has("hidden") && o.get("hidden").getAsBoolean();
            double sc = 1; // a file from before sizes, or a size that cannot be read: its own size
            try { if (o.has("scale")) sc = o.get("scale").getAsDouble(); } catch (RuntimeException bad) { sc = 1; }
            if (!validAnchor(a) || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(op)) return;
            anchor = a; offX = Math.max(0, x); offY = Math.max(0, y); opacity = snapOpacity(op); hidden = hide; scale = snapScale(sc);
        } catch (Exception ex) {
            reset(); // a corrupt file means the defaults
            System.err.println("[Graves and Glory] The command panel layout could not be read, using the defaults: " + ex.getMessage());
        }
    }

    /** Writes this player's entry, keeping the other profiles' (a corrupt file is replaced). */
    public static void save(MinecraftClient c) {
        String key = loadedFor != null ? loadedFor : profileKey(c);
        try {
            Path f = file();
            JsonObject root = null;
            if (Files.isRegularFile(f)) try { root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject(); } catch (Exception ignored) { }
            if (root == null) root = new JsonObject();
            root.addProperty("version", 1);
            JsonObject profiles = root.has("profiles") && root.get("profiles").isJsonObject() ? root.getAsJsonObject("profiles") : new JsonObject();
            JsonObject o = new JsonObject();
            o.addProperty("anchor", anchor);
            o.addProperty("x", Math.round(offX * 100) / 100.0);
            o.addProperty("y", Math.round(offY * 100) / 100.0);
            o.addProperty("opacity", Math.round(opacity * 100) / 100.0);
            o.addProperty("hidden", hidden);
            o.addProperty("scale", scale);
            profiles.add(key, o);
            root.add("profiles", profiles);
            Files.createDirectories(f.getParent());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(f, gson.toJson(root), StandardCharsets.UTF_8);
            saves++;
        } catch (Exception ex) {
            System.err.println("[Graves and Glory] The command panel layout could not be saved: " + ex.getMessage());
        }
    }

    static boolean validAnchor(String a) {
        return switch (a) { case DEFAULT, "top-left", "top-right", "bottom-left", "bottom-right" -> true; default -> false; };
    }

    /** 20% to 100% in 5% steps. */
    public static float snapOpacity(double v) {
        double s = Math.round(v / STEP) * STEP;
        return (float) Math.max(MIN_OPACITY, Math.min(1, Math.round(s * 100) / 100.0));
    }

    /**
     * The panel's bounds' top-left on screen (GUI px) for bounds bw x bh, clamped so the whole of them stays on the screen; null for the
     * default layout. k is GUI px per canvas px.
     */
    static double[] place(int bw, int bh, int sw, int sh, double k, int[] avoid) {
        if (!moved()) return null;
        double left = anchor.endsWith("left") ? MARGIN + offX * k : sw - bw - MARGIN - offX * k; // offsets are the gap beyond the margin, so a snapped edge keeps the margin at every size
        double top = anchor.startsWith("top") ? MARGIN + offY * k : sh - bh - MARGIN - offY * k;
        left = clamp(left, MARGIN, sw - bw - MARGIN); top = clamp(top, MARGIN, sh - bh - MARGIN);
        // never over the vanilla hotbar, health and armour rows: the nearest of beside them (either side) or above them that fits on screen
        if (avoid != null && left < avoid[0] + avoid[2] && avoid[0] < left + bw && top < avoid[1] + avoid[3] && avoid[1] < top + bh) {
            double[][] options = {{avoid[0] + avoid[2] + MARGIN, top}, {avoid[0] - bw - MARGIN, top}, {left, avoid[1] - bh - MARGIN}};
            double best = Double.MAX_VALUE; double[] at = null;
            for (double[] o : options) {
                if (o[0] < MARGIN || o[0] + bw > sw - MARGIN || o[1] < MARGIN || o[1] + bh > sh - MARGIN) continue;
                double d = Math.abs(o[0] - left) + Math.abs(o[1] - top);
                if (d < best) { best = d; at = o; }
            }
            if (at != null) { left = at[0]; top = at[1]; }
        }
        return new double[]{left, top};
    }

    /** Records a dragged position (bounds top-left in GUI px): clamped on screen, snapped to an edge within 8 canvas px, kept by its nearest corner. */
    static void moveTo(double left, double top, int bw, int bh, int sw, int sh, double k) {
        left = clamp(left, MARGIN, sw - bw - MARGIN); top = clamp(top, MARGIN, sh - bh - MARGIN);
        double snap = 8 * k;
        if (left - MARGIN < snap) left = MARGIN;
        if (sw - bw - MARGIN - left < snap) left = Math.max(MARGIN, sw - bw - MARGIN);
        if (top - MARGIN < snap) top = MARGIN;
        if (sh - bh - MARGIN - top < snap) top = Math.max(MARGIN, sh - bh - MARGIN);
        boolean leftSide = left + bw / 2.0 < sw / 2.0, topSide = top + bh / 2.0 < sh / 2.0;
        anchor = (topSide ? "top-" : "bottom-") + (leftSide ? "left" : "right");
        offX = Math.max(0, (leftSide ? left : sw - bw - left) - MARGIN) / k;
        offY = Math.max(0, (topSide ? top : sh - bh - top) - MARGIN) / k;
    }

    static double clamp(double v, double lo, double hi) { return hi < lo ? lo : Math.max(lo, Math.min(hi, v)); }
}
