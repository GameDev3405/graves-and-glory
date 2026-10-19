package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** The War Table's chrome, shared by every camp page (UI spec 4.1 and 4.2, plan Task 5): the oak, the back rail and its brass
 *  plaques. Everything is in canvas pixels, drawn between UiCanvas.begin and end. The 960x540 page sits in the middle of the
 *  canvas: content below the rail adds origin(), rail items add only its x, and the rail runs the whole canvas width. */
final class WarTable {
    private WarTable() {}

    /** {ox, oy}: the canvas point the 960x540 page's top left sits on (negative when the canvas is smaller than the page). */
    static int[] origin() { return new int[]{(UiCanvas.width() - 960) / 2, (UiCanvas.height() - 540) / 2}; }

    /** The oak a page lies on (Kort, 2026-10-18 late): each War Table page has its own stretch of the table, its plank layout,
     *  knots and nail heads cut by tools/ui_art/kit_props.table(page) from the page's own seed (kit_props.TABLE_SEEDS, where the
     *  seeds of the pages to come are kept; nothing else, never a brass fitting). Home's is table/oak-table; another page's is
     *  table/oak-table-<page> once its task exports it, and home's until then. */
    static String tableTexture(String page) {
        if (page.equals("home")) return "table/oak-table";
        String own = "table/oak-table-" + page;
        return UiTex.has(own) ? own : "table/oak-table";
    }

    /** Fills the canvas with OAK0, draws the page's 1280x720 table centred on it, what the page keeps under the rail (home's
     *  rolled campaign map, its far end pushed under the rail), then the rail across the canvas. */
    static void drawChrome(DrawContext g, String page) {
        int w = UiCanvas.width(), h = UiCanvas.height(), ox = origin()[0];
        g.fill(0, 0, w, h, DarkUi.OAK0);
        UiTex.draw(g, tableTexture(page), (w - 1280) / 2, (h - 720) / 2);
        if (page.equals("home")) UiTex.draw(g, "table/prop-maproll", ox + 148, 48);
        UiTex.three(g, "table/rail", 0, 0, w);
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
            if (badge > 0) {
                UiTex.draw(g, "table/wax-badge", bx + 93, by + 8);
                g.drawText(font, String.valueOf(Math.min(9, badge)), bx + 96, by + 10, DarkUi.CHIP_TEXT, false);
            }
        });
    }

    /** "War table", the way back to home from every other page: plaque-120 at 56 with icon-table and Esc. */
    static UiButton backPlaque(ButtonWidget.PressAction a) { return plaque(56, 120, "icon-table", "War table", "Esc", 85, 0, a); }
}
