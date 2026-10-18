package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The UI kit's textures: textures/gui/<folder>/<name>.png as tools/build_ui_art.py exports them, described by
 *  textures/gui/atlas.json. Names are "<folder>/<name>", such as "hud/card-sel". Positions are canvas pixels (draw
 *  between UiCanvas.begin and end) and name the box, not the PNG: a texture whose shadow or overhang sits outside
 *  its box is drawn ox, oy up and left of the given point. Every texture draws at one canvas pixel per art pixel;
 *  slices tile their edges and middles and never stretch. */
public final class UiTex {
    private UiTex() {}

    /** One atlas entry: w, h the PNG size; ox, oy the box origin inside it; kind fixed, nine, three-h, three-v or
     *  sheet; l, t, r, b the slice insets from the PNG's edges; frames the equal horizontal cells of a sheet. */
    public record Entry(Identifier id, int w, int h, int ox, int oy, String kind, int l, int t, int r, int b, int frames) {}

    static final Identifier ATLAS = new Identifier("gravesandglory", "textures/gui/atlas.json");
    private static volatile Map<String, Entry> entries = Map.of();

    /** Reads atlas.json at every resource reload; GravesAndGloryClient.onInitializeClient registers it. */
    public static final SimpleSynchronousResourceReloadListener RELOADER = new SimpleSynchronousResourceReloadListener() {
        @Override public Identifier getFabricId() { return new Identifier("gravesandglory", "ui_atlas"); }
        @Override public void reload(ResourceManager manager) { load(manager); }
    };

    /** Reads the atlas. A reload that finds no atlas, or one it cannot read, logs it and keeps the textures read before;
     *  with none read yet, entry throws for every name until a reload finds the atlas. */
    static void load(ResourceFactory manager) {
        var resource = manager.getResource(ATLAS);
        if (resource.isEmpty()) { keep("is missing"); return; }
        Map<String, Entry> next = new LinkedHashMap<>();
        try (var reader = resource.get().getReader()) {
            JsonObject textures = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("textures");
            for (var e : textures.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                next.put(e.getKey(), new Entry(new Identifier("gravesandglory", "textures/gui/" + e.getKey() + ".png"),
                    o.get("w").getAsInt(), o.get("h").getAsInt(), o.get("ox").getAsInt(), o.get("oy").getAsInt(), o.get("kind").getAsString(),
                    o.get("l").getAsInt(), o.get("t").getAsInt(), o.get("r").getAsInt(), o.get("b").getAsInt(), o.get("frames").getAsInt()));
            }
        } catch (Exception e) { keep("could not be read (" + e + ")"); return; }
        entries = Collections.unmodifiableMap(next);
    }

    private static void keep(String why) {
        int n = entries.size();
        System.err.println("[Graves and Glory] The UI atlas " + ATLAS + " " + why + "; " + (n > 0 ? "keeping the " + n + " UI textures read before."
            : "no UI texture is known yet, so UiTex.entry throws for every name and no UI texture draws until a resource reload finds it."));
    }

    /** How many textures the atlas named at the last good read. */
    static int count() { return entries.size(); }

    public static Entry entry(String name) {
        Entry e = entries.get(name);
        if (e == null) throw new IllegalArgumentException("No UI texture named \"" + name + "\" in " + ATLAS);
        return e;
    }

    public static boolean has(String name) { return entries.containsKey(name); }

    /** The whole PNG, its box at (x, y). */
    public static void draw(DrawContext g, String name, int x, int y) {
        Entry e = entry(name);
        blit(g, e, x - e.ox, y - e.oy, 0, 0, e.w, e.h);
    }

    /** Cell `frame` of a sheet (wrapping), its box at (x, y). */
    public static void frame(DrawContext g, String name, int frame, int x, int y) {
        Entry e = entry(name);
        int n = Math.max(1, e.frames), cw = e.w / n;
        blit(g, e, x - e.ox, y - e.oy, Math.floorMod(frame, n) * cw, 0, cw, e.h);
    }

    /** A nine-slice drawn w x h (w, h measured as the PNG is; the atlas w, h reproduce it), its box at (x, y). */
    public static void nine(DrawContext g, String name, int x, int y, int w, int h) {
        Entry e = entry(name);
        slice(g, e, x - e.ox, y - e.oy, w, h);
    }

    /** A three-slice drawn `length` long, across for three-h and down for three-v, its box at (x, y). */
    public static void three(DrawContext g, String name, int x, int y, int length) {
        Entry e = entry(name);
        if (e.kind.equals("three-v")) slice(g, e, x - e.ox, y - e.oy, e.w, length);
        else slice(g, e, x - e.ox, y - e.oy, length, e.h);
    }

    /** The sub-rectangle (u, v, w, h) of the PNG drawn at (x, y), with no box offset. */
    public static void part(DrawContext g, String name, int x, int y, int u, int v, int w, int h) {
        blit(g, entry(name), x, y, u, v, w, h);
    }

    /** A fill cut to `length` columns (0 draws nothing) whose dark edge, the PNG's end column, stays at the inner end,
     *  exactly as hp_fill(n) and track(56, f) paint it. From the left: the first length-1 columns, then the last
     *  column. From the right: the first column, then the last length-1 columns, flush with the right end of the
     *  full bar. (x, y) places the full bar's box, wherever the cut ends. */
    public static void bar(DrawContext g, String name, int x, int y, int length, boolean fromRight) {
        Entry e = entry(name);
        int n = Math.min(length, e.w), x0 = x - e.ox, y0 = y - e.oy;
        if (n <= 0) return;
        if (!fromRight) {
            blit(g, e, x0, y0, 0, 0, n - 1, e.h);
            blit(g, e, x0 + n - 1, y0, e.w - 1, 0, 1, e.h);
        } else {
            int s = x0 + e.w - n;
            blit(g, e, s, y0, 0, 0, 1, e.h);
            blit(g, e, s + 1, y0, e.w - n + 1, 0, n - 1, e.h);
        }
    }

    /** Slices the PNG by its insets into three columns and three rows; corners once, edges and middle tiled. */
    static void slice(DrawContext g, Entry e, int x, int y, int w, int h) {
        int lw = Math.min(e.l, w), rw = Math.min(e.r, Math.max(0, w - lw));
        int th = Math.min(e.t, h), bh = Math.min(e.b, Math.max(0, h - th));
        int[][] cols = {{0, lw, 0, lw}, {e.l, e.w - e.l - e.r, lw, w - lw - rw}, {e.w - rw, rw, w - rw, rw}};
        int[][] rows = {{0, th, 0, th}, {e.t, e.h - e.t - e.b, th, h - th - bh}, {e.h - bh, bh, h - bh, bh}};
        for (int[] r : rows) for (int[] c : cols) tile(g, e, x + c[2], y + r[2], c[3], r[3], c[0], r[0], c[1], r[1]);
    }

    /** Fills dw x dh at (dx, dy) with whole copies of the source rectangle (u, v, sw, sh), cutting the last ones. */
    static void tile(DrawContext g, Entry e, int dx, int dy, int dw, int dh, int u, int v, int sw, int sh) {
        if (dw <= 0 || dh <= 0 || sw <= 0 || sh <= 0) return;
        for (int oy = 0; oy < dh; oy += sh)
            for (int ox = 0; ox < dw; ox += sw) blit(g, e, dx + ox, dy + oy, u, v, Math.min(sw, dw - ox), Math.min(sh, dh - oy));
    }

    /** drawTexture leaves blending as it finds it, and text and fills turn it off after they draw; the art's shadows are
     *  translucent, so every blit blends. */
    static void blit(DrawContext g, Entry e, int x, int y, int u, int v, int w, int h) {
        if (w <= 0 || h <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.drawTexture(e.id, x, y, w, h, u, v, w, h, e.w, e.h);
    }
}
