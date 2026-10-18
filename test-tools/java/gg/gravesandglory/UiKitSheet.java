package gg.gravesandglory;

import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Harness only, never shipped: every UI kit texture laid out on the canvas at one canvas pixel per art pixel, in
 *  pages of rows 8 px apart. Fixed textures draw once through UiTex.draw; nine and three slices draw twice through
 *  nine/three, at native size and at 1.5x native; the bar cuts sit in their channel and track. A texture larger
 *  than the canvas (the 1280 x 720 tables) gets a page of its own, centred, so its middle shows. */
public final class UiKitSheet extends Screen {
    static final int MARGIN = 12, GAP = 8, BACKGROUND = 0xff595d60;
    /** One thing on the sheet: how it draws (draw, nine, three, bar), the texture, the size it covers. For a bar,
     *  `bar` picks the cut (UiKitSheet.drawBar) and `length` is its length. */
    record Item(String how, String name, int w, int h, int length, int bar) {}
    record Placed(Item item, int page, int x, int y) {}
    static final int[] BAR_LENGTHS = {1, 68, 102, 27, 54, 27, 54};

    final List<String> names;
    final List<Placed> placed = new ArrayList<>();
    int page, pages, drawnOnPage, shownPage = -1;   // shownPage: the page the last frame drew
    Throwable error;

    public UiKitSheet() {
        super(Text.literal("UI kit sheet"));
        names = atlasNames();
    }

    /** The texture names in atlas.json order, read through the resource manager (empty when there is no atlas). */
    static List<String> atlasNames() {
        var resource = MinecraftClient.getInstance().getResourceManager().getResource(UiTex.ATLAS);
        List<String> out = new ArrayList<>();
        if (resource.isEmpty()) return out;
        try (var reader = resource.get().getReader()) {
            out.addAll(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("textures").keySet());
        } catch (Exception e) { throw new IllegalStateException("atlas.json unreadable: " + e, e); }
        return out;
    }

    List<Item> items() {
        List<Item> items = new ArrayList<>();
        for (String n : names) {
            UiTex.Entry e = UiTex.entry(n);
            switch (e.kind()) {
                case "nine" -> { items.add(new Item("nine", n, e.w(), e.h(), 0, -1)); items.add(new Item("nine", n, e.w() * 3 / 2, e.h() * 3 / 2, 0, -1)); }
                case "three-h" -> { items.add(new Item("three", n, e.w(), e.h(), e.w(), -1)); items.add(new Item("three", n, e.w() * 3 / 2, e.h(), e.w() * 3 / 2, -1)); }
                case "three-v" -> { items.add(new Item("three", n, e.w(), e.h(), e.h(), -1)); items.add(new Item("three", n, e.w(), e.h() * 3 / 2, e.h() * 3 / 2, -1)); }
                default -> items.add(new Item("draw", n, e.w(), e.h(), 0, -1));
            }
        }
        for (int i = 0; i < BAR_LENGTHS.length; i++) {
            String back = i < 3 ? "hud/hp-channel" : "hud/track-empty";
            items.add(new Item("bar", i < 3 ? "hud/hp-full-dawn" : i < 5 ? "hud/track-dawn-fill" : "hud/track-dusk-fill", UiTex.entry(back).w(), UiTex.entry(back).h(), BAR_LENGTHS[i], i));
        }
        // grouped by folder, tallest first so the rows pack; textures taller than the canvas last, a page each
        int tall = UiCanvas.height() - 2 * MARGIN;
        items.sort(Comparator.<Item>comparingInt(it -> it.h() > tall ? 1 : 0).thenComparingInt(it -> folder(it.name())).thenComparingInt(it -> -it.h()));
        return items;
    }

    static int folder(String name) { return name.startsWith("hud/") ? 0 : name.startsWith("table/") ? 1 : 2; }

    @Override protected void init() {
        placed.clear();
        int W = UiCanvas.width(), H = UiCanvas.height(), x = MARGIN, y = MARGIN, rowH = 0, pg = 0;
        for (Item it : items()) {
            if (it.h() > H - 2 * MARGIN) {                  // a page of its own, centred
                if (x > MARGIN || y > MARGIN) pg++;
                placed.add(new Placed(it, pg, (W - it.w()) / 2, (H - it.h()) / 2));
                pg++; x = MARGIN; y = MARGIN; rowH = 0;
                continue;
            }
            if (it.w() > W - 2 * MARGIN) {                  // a row of its own, centred across
                if (x > MARGIN) { x = MARGIN; y += rowH + GAP; rowH = 0; }
                if (y + it.h() > H - MARGIN) { pg++; y = MARGIN; }
                placed.add(new Placed(it, pg, (W - it.w()) / 2, y));
                y += it.h() + GAP;
                continue;
            }
            if (x + it.w() > W - MARGIN) { x = MARGIN; y += rowH + GAP; rowH = 0; }
            if (y + it.h() > H - MARGIN) { pg++; x = MARGIN; y = MARGIN; rowH = 0; }
            placed.add(new Placed(it, pg, x, y));
            x += it.w() + GAP; rowH = Math.max(rowH, it.h());
        }
        pages = placed.isEmpty() ? 0 : placed.get(placed.size() - 1).page() + 1;
    }

    @Override public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        UiCanvas.begin(g);
        g.fill(0, 0, UiCanvas.width(), UiCanvas.height(), BACKGROUND);
        int n = 0;
        for (Placed p : placed) {
            if (p.page() != page) continue;
            try { draw(g, p); n++; }
            catch (Exception e) { if (error == null) { error = e; e.printStackTrace(); } }
        }
        drawnOnPage = n; shownPage = page;
        UiCanvas.end(g);
    }

    static void draw(DrawContext g, Placed p) {
        Item it = p.item();
        if (it.how().equals("bar")) { drawBar(g, it.bar(), p.x(), p.y(), it.length()); return; }
        UiTex.Entry e = UiTex.entry(it.name());
        int x = p.x() + e.ox(), y = p.y() + e.oy();       // the PNG's top-left lands on the placed point
        switch (it.how()) {
            case "nine" -> UiTex.nine(g, it.name(), x, y, it.w(), it.h());
            case "three" -> UiTex.three(g, it.name(), x, y, it.length());
            default -> UiTex.draw(g, it.name(), x, y);
        }
    }

    /** The bar cuts in their channel and track, where the HUD will draw them: health from the left inside the channel
     *  at (1, 1); Dawn's score from the left and Dusk's from the right inside the groove at (1, 2). */
    static void drawBar(DrawContext g, int which, int x, int y, int length) {
        if (which < 3) {
            UiTex.draw(g, "hud/hp-channel", x, y);
            UiTex.bar(g, "hud/hp-full-dawn", x + 1, y + 1, length, false);
        } else {
            UiTex.draw(g, "hud/track-empty", x, y);
            if (which < 5) UiTex.bar(g, "hud/track-dawn-fill", x + 1, y + 2, length, false);
            else UiTex.bar(g, "hud/track-dusk-fill", x + 1, y + 2, length, true);
        }
    }

    @Override public boolean shouldPause() { return false; }

    int count(String how, boolean wide) {
        int n = 0;
        for (Placed p : placed) {
            Item it = p.item();
            if (!it.how().equals(how)) continue;
            boolean isWide = !how.equals("bar") && (it.w() != UiTex.entry(it.name()).w() || it.h() != UiTex.entry(it.name()).h());
            if (isWide == wide) n++;
        }
        return n;
    }

    /** Compares the framebuffer of the current page with the art: every opaque pixel of every texture drawn at native
     *  size (draw, and nine and three at their PNG size, which must reproduce the PNG) and of every bar cut must show
     *  exactly. Returns {pixels compared, pixels wrong}; `first` gets the first wrong one. */
    int[] compare(NativeImage shot, Map<String, NativeImage> art, StringBuilder first) {
        int compared = 0, wrong = 0;
        for (Placed p : placed) {
            if (p.page() != page) continue;
            Item it = p.item();
            if (it.how().equals("bar")) {
                boolean right = it.bar() >= 5;
                NativeImage png = art.get(it.name());
                int W = png.getWidth(), n = it.length(), bx = p.x() + 1, by = p.y() + (it.bar() < 3 ? 1 : 2);
                for (int c = 0; c < n; c++) {
                    int col = right ? W - n + c : c;                       // where the cut column sits in the full bar
                    int src = right ? (c == 0 ? 0 : col) : (c < n - 1 ? c : W - 1);
                    for (int r = 0; r < png.getHeight(); r++) {
                        int[] cw = check(shot, png, src, r, bx + col, by + r, first, it.name() + " cut " + n);
                        compared += cw[0]; wrong += cw[1];
                    }
                }
                continue;
            }
            NativeImage png = art.get(it.name());
            if (it.w() != png.getWidth() || it.h() != png.getHeight()) continue;   // the 1.5x slices are judged by eye
            for (int j = 0; j < png.getHeight(); j++)
                for (int i = 0; i < png.getWidth(); i++) {
                    int[] cw = check(shot, png, i, j, p.x() + i, p.y() + j, first, it.name() + " (" + it.how() + ")");
                    compared += cw[0]; wrong += cw[1];
                }
        }
        return new int[]{compared, wrong};
    }

    static int[] check(NativeImage shot, NativeImage png, int u, int v, int sx, int sy, StringBuilder first, String what) {
        if (sx < 0 || sy < 0 || sx >= shot.getWidth() || sy >= shot.getHeight()) return new int[]{0, 0};
        int want = png.getColor(u, v);
        if ((want >>> 24) != 0xff) return new int[]{0, 0};                      // translucent pixels blend with the page
        int got = shot.getColor(sx, sy);
        if ((got & 0xffffff) == (want & 0xffffff)) return new int[]{1, 0};
        if (first.isEmpty()) first.append(what).append(" art pixel ").append(u).append(',').append(v).append(" shows ").append(Integer.toHexString(got & 0xffffff)).append(" at ").append(sx).append(',').append(sy).append(", not ").append(Integer.toHexString(want & 0xffffff));
        return new int[]{1, 1};
    }

    /** The PNGs of every texture on the sheet, read through the resource manager. Close them with closeAll. */
    static Map<String, NativeImage> loadArt(List<String> names) throws java.io.IOException {
        Map<String, NativeImage> art = new HashMap<>();
        var manager = MinecraftClient.getInstance().getResourceManager();
        for (String n : names) try (var in = manager.getResource(UiTex.entry(n).id()).orElseThrow().getInputStream()) { art.put(n, NativeImage.read(in)); }
        return art;
    }

    static void closeAll(Map<String, NativeImage> art) { for (NativeImage i : art.values()) i.close(); }
}
