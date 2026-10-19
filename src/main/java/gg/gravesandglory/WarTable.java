package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.*;

/** The War Table's chrome, shared by every camp page (UI spec 4.1 and 4.2, plan Task 5): the oak, the back rail and its brass
 *  plaques. Everything is in canvas pixels, drawn between UiCanvas.begin and end. The 960x540 page sits in the middle of the
 *  canvas: content below the rail adds origin(), rail items add only its x, and the rail runs the whole canvas width. */
final class WarTable {
    private WarTable() {}

    /** {ox, oy}: the canvas point the 960x540 page's top left sits on (negative when the canvas is smaller than the page). */
    static int[] origin() { return new int[]{(UiCanvas.width() - 960) / 2, (UiCanvas.height() - 540) / 2}; }

    /** Home's oak, the table every page falls back to until its own is exported. */
    static final String HOME_TABLE = "table/oak-table";
    /** The pages that have a seed in tools/ui_art/kit_props.TABLE_SEEDS: each has, or will have, its own table/oak-table-<page>. */
    static final Set<String> PAGES = Set.of("home", "recruit", "banner", "contracts", "chapter", "friends", "field", "results");
    /** Where home's rolled map lies, on the rail line (canvas y), its far end pushed under the rail. */
    static final int MAPROLL_Y = 48;
    /** The table's size, and the 4 px of shadow its planks cast on the floor round them. */
    static final int TABLE_W = 1280, TABLE_H = 720, SHADOW = 4;
    /** The shadow's alpha on the floor, nearest the planks first. */
    static final int[] SHADOW_ALPHA = {0xb0, 0x80, 0x50, 0x28};

    private static final Set<String> warned = new HashSet<>();
    // What the last drawChrome passed to the draw calls: the floor fill, the table and the rail, in canvas px, and the table's name.
    private static final Map<String, int[]> drawnRects = new LinkedHashMap<>();
    private static final Map<String, int[]> drawn = Collections.unmodifiableMap(drawnRects);
    private static String drawnTable = "none";
    private static final Map<String, Integer> badgesDrawn = new LinkedHashMap<>();
    private static final Map<String, Integer> badges = Collections.unmodifiableMap(badgesDrawn);
    /** The count each rail plaque drew on its wax badge (0: none) the last frame it was drawn, by label. */
    static Map<String, Integer> badges() { return badges; }
    /** Boxes {x,y,w,h} the last drawChrome drew: floor (the whole canvas), table, rail and, on home, maproll. */
    static Map<String, int[]> drawn() { return drawn; }
    /** The table texture the last drawChrome drew. */
    static String drawnTable() { return drawnTable; }

    /** The oak a page lies on (Kort, 2026-10-18 late): each War Table page has its own stretch of the table, its plank layout,
     *  knots and nail heads cut by tools/ui_art/kit_props.table(page) from the page's own seed (kit_props.TABLE_SEEDS, where the
     *  seeds of the pages to come are kept; nothing else, never a brass fitting). Home's is table/oak-table; another page's is
     *  table/oak-table-<page> once its task exports it, and home's until then. A full texture name ("table/oak-table-recruit")
     *  is taken as it is. A name that is neither a known page nor a texture the atlas has logs one warning and draws home's oak. */
    static String tableTexture(String page) {
        String own = page.startsWith("table/") ? page : page.equals("home") ? HOME_TABLE : "table/oak-table-" + page;
        if (UiTex.has(own)) return own;
        if (!page.startsWith("table/") && PAGES.contains(page)) return HOME_TABLE; // a known page whose oak is not exported yet
        if (warned.add(page)) System.err.println("[Graves and Glory] War Table: no table for \"" + page + "\" (a page name from " + PAGES + " or a table/... texture); drawing home's oak.");
        return HOME_TABLE;
    }

    /** Fills the canvas with the floor, draws the page's 1280x720 table centred on it with its shadow on the floor round it
     *  (a canvas larger than the table shows both), what the page keeps under the rail (home's rolled campaign map, its far end
     *  pushed under the rail), then the rail across the canvas. page is a page name or a full table/... texture name. */
    static void drawChrome(DrawContext g, String page) {
        int w = UiCanvas.width(), h = UiCanvas.height(), ox = origin()[0];
        String table = tableTexture(page);
        int tx = (w - TABLE_W) / 2, ty = (h - TABLE_H) / 2, railH = UiTex.entry("table/rail").h();
        g.fill(0, 0, w, h, DarkUi.FLOOR);
        drawnRects.clear();
        drawnRects.put("floor", new int[]{0, 0, w, h});
        for (int i = 1; i <= SHADOW; i++) { // a ring of the shadow round the planks per pixel, nearest the planks darkest
            int a = SHADOW_ALPHA[i - 1] << 24, x0 = tx - i, y0 = ty - i, x1 = tx + TABLE_W + i, y1 = ty + TABLE_H + i;
            g.fill(x0, y0, x1, y0 + 1, a);
            g.fill(x0, y1 - 1, x1, y1, a);
            g.fill(x0, y0 + 1, x0 + 1, y1 - 1, a);
            g.fill(x1 - 1, y0 + 1, x1, y1 - 1, a);
        }
        UiTex.draw(g, table, tx, ty);
        drawnTable = table;
        drawnRects.put("table", new int[]{tx, ty, TABLE_W, TABLE_H});
        if ((page.equals("home") || page.equals(HOME_TABLE)) && ty <= railH) { // the roll lies on the planks while the rail covers the table's top edge; on a taller canvas it would hang over bare floor, so it stays off
            UiTex.draw(g, "table/prop-maproll", ox + 148, MAPROLL_Y);
            drawnRects.put("maproll", new int[]{ox + 148, MAPROLL_Y, UiTex.entry("table/prop-maproll").w(), UiTex.entry("table/prop-maproll").h()});
        }
        UiTex.three(g, "table/rail", 0, 0, w);
        drawnRects.put("rail", new int[]{0, 0, w, railH});
    }

    /** A brass plaque on the rail at page x (plaque-<width>, rail y 12, 28 tall): its icon at (8,7) and its name, a heading in
     *  IVORY at (28,8); its key, unless null, on a brass cap at (keyX,7) with the text in 8 px RAIL_KEY at (keyX+4,10); its count,
     *  when badge is above 0, on a wax drop at (93,8) in 8 px CHIP_TEXT at (96,10). Hovered or focused, the name turns BRASS_HI. */
    static UiButton plaque(int x, int width, String icon, String label, String key, int keyX, int badge, ButtonWidget.PressAction a) {
        // the names are built here, not in the UiTex calls: build_ui_art --verify checks only the names written whole in a call
        String plate = "table/plaque-" + width, mark = "table/" + icon;
        return new UiButton(origin()[0] + x, 12, width, 28, Text.literal(label), a, (g, bx, by, bw, bh, hot) -> {
            var font = MinecraftClient.getInstance().textRenderer;
            UiTex.draw(g, plate, bx, by);
            UiTex.draw(g, mark, bx + 8, by + 7);
            g.drawText(font, DarkUi.heading(label), bx + 28, by + 8, hot ? DarkUi.BRASS_HI : DarkUi.IVORY, false);
            if (key != null) {
                String cap = "table/key-brass-" + UiMath.brassKeyWidth(font.getWidth(key), key.length());
                UiTex.draw(g, cap, bx + keyX, by + 7);
                g.drawText(font, key, bx + keyX + 4, by + 10, DarkUi.RAIL_KEY, false);
            }
            int drawn = 0;
            if (badge > 0) {
                drawn = Math.min(9, badge);
                UiTex.draw(g, "table/wax-badge", bx + 93, by + 8);
                g.drawText(font, String.valueOf(drawn), bx + 96, by + 10, DarkUi.CHIP_TEXT, false);
            }
            badgesDrawn.put(label, drawn);
        });
    }

    /** "War table", the way back to home from every other page: plaque-120 at 56 with icon-table and Esc. */
    static UiButton backPlaque(ButtonWidget.PressAction a) { return plaque(56, 120, "icon-table", "War table", "Esc", 85, 0, a); }
}
