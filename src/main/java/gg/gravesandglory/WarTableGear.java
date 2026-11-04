package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The Arsenal (page gear, the gear screen plan): the commander's gear on its Arsenal tab and the blacksmith on its Forge tab. It
 *  draws in canvas pixels like the other War Table pages and records the boxes it drew for the harness (rects). Every click sends
 *  a gear: or forge: command through CampScreen.action; the server decides.
 *
 *  Layout A: two parchment cards under the rail. The left card holds the commander: summed stats, the portrait frame, the six
 *  worn slots with the set line, and the three relic slots (keys 1-3) with gear power. The right card holds the inventory: its
 *  counter, the filter tags and the sort tag, the 8-column grid that scrolls by whole rows, and the confirm strip or the latest
 *  server notice under it. The tooltip draws over everything, flipped so it stays 12 px inside the canvas. */
final class WarTableGear {
    static final String TITLE = "Arsenal";
    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    /** The boxes {x,y,w,h} in canvas px the last frame drew, by name. */
    static Map<String, int[]> rects() { return rects; }

    /** Slot box, its pitch in the worn and relic rows and the grid, the tooltip's width and the canvas margin it keeps. */
    static final int SLOT = 44, PITCH = 50, TIP_W = 200, MARGIN = 12;
    private static final String PAPER = "table/sheet-field";

    final CampScreen screen;
    String tab;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    String filter = "all", sort = "rarity";
    int scroll;
    /** The relic slot a relic click goes to, -1 for none chosen (GearView.relicTarget picks then). */
    int selectedRelic = -1;
    /** The piece the confirm strip asks about, or -1. */
    int confirmUid = -1;
    // What the hover found this frame: a grid cell index, a worn slot, a relic slot; and the tooltip lines it built.
    private int hoverCell = -1, hoverRelic = -1;
    private String hoverWorn;

    WarTableGear(CampScreen screen, String tab) {
        this.screen = screen;
        this.tab = "forge".equals(tab) ? "forge" : "armoury";
    }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }
    static GearWire.View view() { return CampaignClient.gear; }

    // ---- layout, in canvas px ------------------------------------------------------------------------------------------------
    /** The page box {x, y, w, h}: the 960x540 page, or the compact 854x480 one. */
    static int[] page() {
        int[] o = WarTable.column(0, 0);
        return WarTable.compact() ? new int[]{o[0], o[1], WarTable.COMPACT_W, WarTable.COMPACT_H} : new int[]{o[0], o[1], 960, 540};
    }
    /** Cards start under the tabs; the rail is 66 px tall at canvas y 0. */
    static int cardTop() { return Math.max(page()[1] + 102, 102); }
    static int cardBottom() { int[] p = page(); return p[1] + p[3] - MARGIN; }
    static int[] left() { int[] p = page(); int w = WarTable.compact() ? 340 : 400; return new int[]{p[0] + MARGIN, cardTop(), w, cardBottom() - cardTop()}; }
    static int[] right() { int[] p = page(), l = left(); int x = l[0] + l[2] + 8; return new int[]{x, cardTop(), p[0] + p[2] - MARGIN - x, cardBottom() - cardTop()}; }
    static int[] worn(int i) { int[] l = left(); return new int[]{l[0] + 16 + i * PITCH, l[1] + 214, SLOT, SLOT}; }
    static int[] relic(int i) { int[] l = left(); return new int[]{l[0] + 16 + i * PITCH, l[1] + 298, SLOT, SLOT}; }
    static int[] frame() { int[] l = left(); return new int[]{l[0] + l[2] - 16 - 160, l[1] + 34, 160, 160}; }
    static int gridX() { int[] r = right(); return r[0] + (r[2] - GearView.COLUMNS * PITCH + (PITCH - SLOT)) / 2; }
    static int gridY() { return right()[1] + 72; }
    /** Grid rows that fit above the strip under the grid (28 px) and the card's bottom padding. */
    static int gridRows() { int[] r = right(); return Math.max(1, (r[1] + r[3] - 44 - gridY()) / PITCH); }
    static int[] cell(int i) { return new int[]{gridX() + i % GearView.COLUMNS * PITCH, gridY() + i / GearView.COLUMNS * PITCH, SLOT, SLOT}; }

    static boolean in(int[] b, double x, double y) { return b != null && x >= b[0] && y >= b[1] && x < b[0] + b[2] && y < b[1] + b[3]; }
    static double mouseCanvasX() { var c = MinecraftClient.getInstance(); var w = c.getWindow(); return UiCanvas.toCanvas(c.mouse.getX() * w.getScaledWidth() / w.getWidth()); }
    static double mouseCanvasY() { var c = MinecraftClient.getInstance(); var w = c.getWindow(); return UiCanvas.toCanvas(c.mouse.getY() * w.getScaledHeight() / w.getHeight()); }

    // ---- widgets -------------------------------------------------------------------------------------------------------------
    void init() {
        buttons.clear();
        add("back", WarTable.plaque(56, 120, "icon-table", TITLE, "Esc", 85, 0, b -> screen.close()).quiet());
        int[] l = left();
        add("page-armoury", new UiButton(l[0], l[1] - 30, 88, 24, Text.literal("Arsenal"), b -> setTab("armoury"), (g, x, y, w, h, hot) -> pageTab(g, "page-armoury", "Arsenal", tab.equals("armoury"), x, y, w, h, hot)));
        add("page-forge", new UiButton(l[0] + 94, l[1] - 30, 88, 24, Text.literal("Forge"), b -> setTab("forge"), (g, x, y, w, h, hot) -> pageTab(g, "page-forge", "Forge", tab.equals("forge"), x, y, w, h, hot)));
        int[] r = right();
        int x = r[0] + 16, y = r[1] + 36;
        for (String f : GearView.FILTERS) {
            String label = filterWord(f);
            int w = font().getWidth(DarkUi.body(label)) + 14;
            add("tab-" + f, new UiButton(x, y, w, WarTablePages.CHIP_H, Text.literal(label), b -> setFilter(f), (g, bx, by, bw, bh, hot) -> chip(g, "tab-" + f, label, f.equals(filter), bx, by, bw, bh, hot)));
            x += w + 4;
        }
        int sw = 96;
        add("sort", new UiButton(r[0] + r[2] - 16 - sw, y, sw, WarTablePages.CHIP_H, Text.literal("Sort"), b -> cycleSort(), (g, bx, by, bw, bh, hot) -> chip(g, "sort", "Sort: " + sortWord(sort), false, bx, by, bw, bh, hot)));
        int cy = gridY() + gridRows() * PITCH + 2;
        add("confirm-yes", new UiButton(r[0] + r[2] - 16 - 172, cy, 88, 24, Text.literal("Break down"), b -> confirmYes(), (g, bx, by, bw, bh, hot) -> tag(g, "Break down", bx, by, bw, bh, hot)));
        add("confirm-no", new UiButton(r[0] + r[2] - 16 - 80, cy, 80, 24, Text.literal("Keep"), b -> confirmUid = -1, (g, bx, by, bw, bh, hot) -> tag(g, "Keep", bx, by, bw, bh, hot)));
        sync();
    }

    /** Shows and greys the widgets for this frame's state: nothing acts until the gear view arrives. */
    void sync() {
        boolean ready = view() != null, arsenal = tab.equals("armoury");
        for (var e : buttons.entrySet()) {
            String k = e.getKey();
            UiButton b = e.getValue();
            if (k.startsWith("tab-") || k.equals("sort")) { b.visible = arsenal; b.active = ready; }
            else if (k.startsWith("confirm-")) { b.visible = arsenal && confirmUid >= 0; b.active = ready; }
        }
    }

    void setTab(String t) {
        if (t.equals(tab)) return;
        tab = t;
        confirmUid = -1;
        UiSounds.confirm();
        screen.gearTab = t;
        screen.rebuild();
    }
    void setFilter(String f) { filter = f; scroll = 0; confirmUid = -1; if (!f.equals("relic")) selectedRelic = -1; }
    void cycleSort() { sort = GearView.SORTS.get((GearView.SORTS.indexOf(sort) + 1) % GearView.SORTS.size()); }
    void confirmYes() {
        int uid = confirmUid;
        confirmUid = -1;
        if (uid >= 0) screen.action("forge:breakdown:" + uid);
    }

    static String filterWord(String f) { return switch (f) { case "all" -> "All"; case "weapon" -> "Weapons"; case "relic" -> "Relics"; default -> Character.toUpperCase(f.charAt(0)) + f.substring(1); }; }
    static String sortWord(String s) { return Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    void tick() {
        GearWire.View v = view();
        if (v != null) scroll = GearView.scrollClamp(scroll, count(v), gridRows());
        if (confirmUid >= 0 && GearView.piece(v, confirmUid) == null) confirmUid = -1;
        sync();
    }

    int count(GearWire.View v) { return filter.equals("relic") ? GearView.visibleRelics(v).size() : GearView.visible(v, filter, sort).size(); }

    // ---- painters ------------------------------------------------------------------------------------------------------------
    void pageTab(DrawContext g, String key, String label, boolean on, int x, int y, int w, int h, boolean hot) {
        UiTex.nine(g, "table/btn-vellum-88x24", x, y, w + 1, h + 2);
        Text t = DarkUi.heading(label);
        int lx = WarTableHome.centred(g, t, x + w / 2, y + 6, on ? DarkUi.INK_TEXT : DarkUi.SEPIA);
        if (on) g.fill(x + 8, y + h, x + w - 8, y + h + 2, DarkUi.INK_TEXT);
        else if (hot) g.fill(lx, y + 18, lx + font().getWidth(t), y + 19, DarkUi.SEPIA);
        rectsDrawn.put(key, new int[]{x, y, w, h});
    }

    void chip(DrawContext g, String key, String label, boolean on, int x, int y, int w, int h, boolean hot) {
        UiButton b = buttons.get(key);
        UiTex.nine(g, on ? "table/chip-house-on" : "table/chip-house", x, y, w + 1, on ? h + WarTablePages.CHIP_RULE : h + 2);
        var t = DarkUi.body(label);
        int dy = on ? 1 : 0, ink = b != null && b.active ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        int tx = x + (w - font().getWidth(t)) / 2;
        g.drawText(font(), t, tx, y + 6 + dy, ink, false);
        if (hot && b != null && b.active) g.fill(tx, y + 18 + dy, tx + font().getWidth(t), y + 19 + dy, ink);
        if (b != null && !b.active) DarkUi.disabledVeil(g, x + 1, y + 1, w - 1, h);
        rectsDrawn.put(key, new int[]{x, y, w, h});
    }

    void tag(DrawContext g, String label, int x, int y, int w, int h, boolean hot) {
        if (w == 80 && h == 24) UiTex.draw(g, "table/btn-vellum-80x24", x, y);
        else if (w == 88 && h == 24) UiTex.draw(g, "table/btn-vellum-88x24", x, y);
        else UiTex.nine(g, "table/btn-vellum-64x24", x, y, w + 1, h + 2);
        Text t = DarkUi.heading(label);
        int lx = WarTableHome.centred(g, t, x + w / 2, y + 6, DarkUi.INK_TEXT);
        if (hot) g.fill(lx, y + 18, lx + font().getWidth(t), y + 19, DarkUi.INK_TEXT);
    }

    /** A parchment card of any size cut from the field sheet: its corners whole, its edges and middle tiled from the sheet's own
     *  middle bands, so the torn edge runs all the way round. */
    static void paper(DrawContext g, int x, int y, int w, int h) {
        var e = UiTex.entry(PAPER);
        int c = Math.min(48, Math.min(w, h) / 2), mw = e.w() - 2 * c, mh = e.h() - 2 * c;
        int[][] cols = {{0, c, 0, c}, {c, mw, c, w - 2 * c}, {e.w() - c, c, w - c, c}};
        int[][] rows = {{0, c, 0, c}, {c, mh, c, h - 2 * c}, {e.h() - c, c, h - c, c}};
        for (int[] r : rows) for (int[] k : cols) tile(g, x + k[2], y + r[2], k[3], r[3], k[0], r[0], k[1], r[1]);
    }
    private static void tile(DrawContext g, int dx, int dy, int dw, int dh, int u, int v, int sw, int sh) {
        if (dw <= 0 || dh <= 0 || sw <= 0 || sh <= 0) return;
        for (int y = 0; y < dh; y += sh) for (int x = 0; x < dw; x += sw) UiTex.part(g, PAPER, dx + x, dy + y, u, v, Math.min(sw, dw - x), Math.min(sh, dh - y));
    }

    static int rarityInk(String rarity) {
        var r = Gear.rarity(rarity);
        if (r == null) return DarkUi.SEPIA;
        try { return DarkUi.OPAQUE | Integer.parseInt(r.border().substring(1), 16); } catch (RuntimeException e) { return DarkUi.SEPIA; }
    }
    static int fade(int ink, int alpha) { return (alpha << 24) | (ink & 0xffffff); }
    static void outline(DrawContext g, int x, int y, int w, int h, int t, int ink) {
        g.fill(x, y, x + w, y + t, ink); g.fill(x, y + h - t, x + w, y + h, ink);
        g.fill(x, y + t, x + t, y + h - t, ink); g.fill(x + w - t, y + t, x + w, y + h - t, ink);
    }
    /** Two to three letters for an item placeholder icon: the initials of its name. */
    static String initials(String name) {
        StringBuilder s = new StringBuilder();
        for (String w : name.split(" ")) if (!w.isEmpty() && Character.isLetter(w.charAt(0)) && !w.equals("of") && !w.equals("the") && s.length() < 3) s.append(Character.toUpperCase(w.charAt(0)));
        return s.toString();
    }

    /** An empty slot: a sepia frame on a dimmed well, its word small inside. */
    static void emptySlot(DrawContext g, int[] b, String word, boolean hot) {
        g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], DarkUi.CARD_SHADE);
        outline(g, b[0], b[1], b[2], b[3], 1, hot ? DarkUi.INK_TEXT : DarkUi.SEPIA_LINE);
        var t = DarkUi.small(word);
        g.drawText(font(), t, b[0] + (b[2] - font().getWidth(t)) / 2, b[1] + (b[3] - 8) / 2, DarkUi.SEPIA, false);
    }

    /** A piece in a box: its icon (the art track's gear/<item> when the atlas has it, else a rarity-coloured placeholder with the
     *  item's initials), the rarity border, the durability bar, and the worn mark, duplicate badge and broken dim. */
    static void pieceIcon(DrawContext g, GearWire.View v, Gear.Piece p, int[] b, boolean hot, boolean wornMark, boolean duplicate) {
        var it = Gear.item(p.item);
        int ink = rarityInk(p.rarity), x = b[0], y = b[1], s = b[2];
        g.fill(x, y, x + s, y + s, DarkUi.TAB_IDLE);
        g.fill(x + 2, y + 2, x + s - 2, y + s - 2, fade(ink, 0x40));
        String art = "gear/" + p.item;
        if (UiTex.has(art)) UiTex.draw(g, art, x + (s - UiTex.entry(art).w()) / 2, y + (s - UiTex.entry(art).h()) / 2);
        else {
            var t = DarkUi.heading(initials(it.name()));
            g.drawText(font(), t, x + (s - font().getWidth(t)) / 2, y + (s - 12) / 2, ink, false);
        }
        boolean broken = p.durability <= 0;
        if (broken) g.fill(x + 2, y + 2, x + s - 2, y + s - 2, DarkUi.DROP_DIM);
        outline(g, x, y, s, s, 2, hot ? DarkUi.BRASS_HI : ink);
        int max = Math.max(1, Gear.forge().maxDurability()), bar = s - 8;
        int fill = broken ? bar : Math.max(1, Math.min(bar, bar * p.durability / max));
        g.fill(x + 4, y + s - 6, x + 4 + bar, y + s - 4, DarkUi.CHARCOAL);
        g.fill(x + 4, y + s - 6, x + 4 + fill, y + s - 4, broken ? DarkUi.ALARM_TEXT : DarkUi.DAWN);
        if (wornMark) {
            g.fill(x + 3, y + 3, x + 13, y + 14, DarkUi.CHARCOAL);
            g.drawText(font(), DarkUi.small("E"), x + 5, y + 4, DarkUi.BRASS_HI, false);
        }
        if (duplicate) UiTex.draw(g, "table/wax-badge", x + s - 13, y + 3);
    }

    /** A relic in a box: an IVORY placeholder with its initials (or gear/<relic> art), its role small under them. */
    static void relicIcon(DrawContext g, String id, int[] b, boolean hot, boolean slotted) {
        var r = Gear.relic(id);
        int x = b[0], y = b[1], s = b[2];
        g.fill(x, y, x + s, y + s, DarkUi.TAB_IDLE);
        String art = "gear/" + id;
        if (UiTex.has(art)) UiTex.draw(g, art, x + (s - UiTex.entry(art).w()) / 2, y + (s - UiTex.entry(art).h()) / 2);
        else if (r != null) {
            var t = DarkUi.heading(initials(r.name()));
            g.drawText(font(), t, x + (s - font().getWidth(t)) / 2, y + (s - 12) / 2, DarkUi.EPIC_INK, false);
        }
        outline(g, x, y, s, s, 2, hot ? DarkUi.BRASS_HI : DarkUi.EPIC_INK);
        if (slotted) {
            g.fill(x + 3, y + 3, x + 13, y + 14, DarkUi.CHARCOAL);
            g.drawText(font(), DarkUi.small("E"), x + 5, y + 4, DarkUi.BRASS_HI, false);
        }
    }

    // ---- render --------------------------------------------------------------------------------------------------------------
    void render(DrawContext g) {
        rectsDrawn.clear();
        UiCanvas.begin(g);
        try {
            WarTable.drawChrome(g, "gear");
            rectsDrawn.putAll(WarTable.drawn());
            sync();
            int[] l = left(), r = right();
            paper(g, l[0], l[1], l[2], l[3]);
            paper(g, r[0], r[1], r[2], r[3]);
            rectsDrawn.put("left", l);
            rectsDrawn.put("right", r);
            GearWire.View v = view();
            if (v == null) { loading(g, l); loading(g, r); return; }
            if (tab.equals("armoury")) { commander(g, v); inventory(g, v); }
            else forge(g, v);
        } finally {
            UiCanvas.end(g);
        }
    }

    static void loading(DrawContext g, int[] card) {
        var t = DarkUi.heading("Loading your gear…");
        g.drawText(font(), t, card[0] + (card[2] - font().getWidth(t)) / 2, card[1] + card[3] / 2 - 6, DarkUi.SEPIA, false);
        rectsDrawn.put("loading", card);
    }

    void commander(DrawContext g, GearWire.View v) {
        int[] l = left();
        double mx = mouseCanvasX(), my = mouseCanvasY();
        var font = font();
        g.drawText(font, DarkUi.heading("Commander"), l[0] + 16, l[1] + 14, DarkUi.INK_TEXT, false);
        var power = DarkUi.body("Gear power " + v.power());
        g.drawText(font, power, l[0] + l[2] - 16 - font.getWidth(power), l[1] + 16, DarkUi.SEPIA, false);
        g.fill(l[0] + 16, l[1] + 28, l[0] + l[2] - 16, l[1] + 29, DarkUi.SEPIA_LINE);

        int[] f = frame();
        int sx = l[0] + 16, sw = f[0] - 12 - sx;
        double[] totals = GearView.totals(v);
        for (int i = 0; i < totals.length; i++) {
            int y = l[1] + 40 + i * 21;
            g.drawText(font, DarkUi.body(GearView.statWord(i)), sx, y, DarkUi.SEPIA, false);
            var val = DarkUi.body(GearView.statText(totals[i]));
            g.drawText(font, val, sx + sw - font.getWidth(val), y, DarkUi.INK_TEXT, false);
            g.fill(sx, y + 14, sx + sw, y + 15, fade(DarkUi.SEPIA_LINE, 0x66));
        }
        rectsDrawn.put("stats", new int[]{sx, l[1] + 36, sw, totals.length * 21});

        UiTex.draw(g, "table/frame-back-160", f[0], f[1]);
        var player = MinecraftClient.getInstance().player;
        if (player != null) Portrait.draw(g, player, f[0] + 20, f[1] + 20, 120, 0);
        rectsDrawn.put("frame", f);

        g.drawText(font, DarkUi.heading("Worn"), l[0] + 16, l[1] + 198, DarkUi.INK_TEXT, false);
        hoverWorn = null;
        for (int i = 0; i < Gear.SLOTS.size(); i++) {
            String slot = Gear.SLOTS.get(i);
            int[] b = worn(i);
            boolean hot = in(b, mx, my);
            Gear.Piece p = GearView.wornIn(v, slot);
            if (p != null && Gear.item(p.item) != null && Gear.rarity(p.rarity) != null) pieceIcon(g, v, p, b, hot, false, false);
            else emptySlot(g, b, GearView.SLOT_WORDS.get(slot).replace(" hand", ""), hot);
            if (hot) hoverWorn = slot;
            rectsDrawn.put("worn-" + slot, b);
        }
        String set = GearView.setProgress(v);
        if (!set.isEmpty()) g.drawText(font, DarkUi.body(set), l[0] + 16, l[1] + 264, DarkUi.SEPIA, false);

        g.drawText(font, DarkUi.heading("Relics"), l[0] + 16, l[1] + 282, DarkUi.INK_TEXT, false);
        hoverRelic = -1;
        for (int i = 0; i < 3; i++) {
            int[] b = relic(i);
            boolean hot = in(b, mx, my);
            String id = i < v.relicSlots().size() ? v.relicSlots().get(i) : "";
            if (!id.isEmpty() && Gear.relic(id) != null) relicIcon(g, id, b, hot, false);
            else emptySlot(g, b, "relic", hot);
            if (i == selectedRelic) outline(g, b[0] - 3, b[1] - 3, b[2] + 6, b[3] + 6, 1, DarkUi.IVORY);
            var k = DarkUi.small(String.valueOf(i + 1));
            g.drawText(font, k, b[0] + (b[2] - font.getWidth(k)) / 2, b[1] + b[3] + 4, DarkUi.SEPIA, false);
            if (hot) hoverRelic = i;
            rectsDrawn.put("relic-" + i, b);
        }
        int rx = relic(2)[0] + SLOT + 16, ry = relic(0)[1] + 6;
        String hint = selectedRelic >= 0 ? "Slot " + (selectedRelic + 1) + " chosen: pick a relic" : "Relics fire on keys 1, 2 and 3";
        WarTablePages.lines(g, hint, rx, ry, l[0] + l[2] - 16 - rx, 2, DarkUi.SEPIA);
    }

    void inventory(DrawContext g, GearWire.View v) {
        int[] r = right();
        var font = font();
        double mx = mouseCanvasX(), my = mouseCanvasY();
        var head = DarkUi.heading("Inventory");
        g.drawText(font, head, r[0] + 16, r[1] + 14, DarkUi.INK_TEXT, false);
        var counter = DarkUi.body(GearView.counter(v));
        int cx = r[0] + r[2] - 16 - font.getWidth(counter);
        g.drawText(font, counter, cx, r[1] + 16, v.pieces().size() >= v.inventorySize() ? DarkUi.ROUT_TEXT : DarkUi.SEPIA, false);
        rectsDrawn.put("counter", new int[]{cx, r[1] + 16, font.getWidth(counter), 12});
        g.fill(r[0] + 16, r[1] + 28, r[0] + r[2] - 16, r[1] + 29, DarkUi.SEPIA_LINE);

        int rows = gridRows();
        rectsDrawn.put("grid", new int[]{gridX(), gridY(), GearView.COLUMNS * PITCH - (PITCH - SLOT), rows * PITCH - (PITCH - SLOT)});
        hoverCell = -1;
        boolean relics = filter.equals("relic");
        List<Gear.Piece> pieces = relics ? List.of() : GearView.visible(v, filter, sort);
        List<String> owned = relics ? GearView.visibleRelics(v) : List.of();
        int n = relics ? owned.size() : pieces.size(), first = scroll * GearView.COLUMNS;
        Profile copy = relics ? null : GearView.asProfile(v);
        for (int i = 0; i < rows * GearView.COLUMNS; i++) {
            int[] b = cell(i);
            int at = first + i;
            if (at >= n) { g.fill(b[0], b[1], b[0] + SLOT, b[1] + SLOT, DarkUi.CARD_SHADE); continue; }
            boolean hot = in(b, mx, my);
            if (hot) hoverCell = i;
            if (relics) relicIcon(g, owned.get(at), b, hot, v.relicSlots().contains(owned.get(at)));
            else {
                Gear.Piece p = pieces.get(at);
                pieceIcon(g, v, p, b, hot, GearView.worn(v, p.uid), Forge.duplicate(copy, p));
                if (p.uid == confirmUid) outline(g, b[0] - 3, b[1] - 3, SLOT + 6, SLOT + 6, 1, DarkUi.ALARM_TEXT);
            }
            rectsDrawn.put("cell-" + i, b);
        }
        if (n == 0) {
            var t = DarkUi.body(relics ? "No relics yet. The Forge rolls them." : "Nothing here yet.");
            g.drawText(font, t, r[0] + (r[2] - font.getWidth(t)) / 2, gridY() + 18, DarkUi.SEPIA, false);
        }
        int total = GearView.rows(n);
        if (total > rows) {
            var s = DarkUi.small("Rows " + (scroll + 1) + "-" + Math.min(total, scroll + rows) + " of " + total);
            g.drawText(font, s, r[0] + r[2] - 16 - font.getWidth(s), r[1] + 62, DarkUi.SEPIA, false);
        }

        int sy = gridY() + rows * PITCH + 2;
        if (confirmUid >= 0) {
            Gear.Piece p = GearView.piece(v, confirmUid);
            String name = p == null || Gear.item(p.item) == null ? "this piece" : Gear.rarity(p.rarity).name() + " " + Gear.item(p.item).name();
            int[] box = {r[0] + 16, sy - 2, r[2] - 32, 28};
            g.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], fade(DarkUi.ALARM_TEXT, 0x30));
            WarTablePages.lines(g, "Break down " + name + "? It cannot be undone.", box[0] + 6, sy + 6, box[2] - 190, 1, DarkUi.ROUT_TEXT);
            rectsDrawn.put("confirm", box);
        } else {
            String notice = CampaignClient.shownNotice();
            if (!notice.isEmpty()) {
                WarTablePages.lines(g, notice, r[0] + 16, sy + 6, r[2] - 32, 1, DarkUi.INK_TEXT);
                rectsDrawn.put("notice", new int[]{r[0] + 16, sy + 6, r[2] - 32, 12});
            }
        }
    }

    /** The Forge tab, drawn in Task 4. */
    void forge(DrawContext g, GearWire.View v) {}

    /** The tooltip, over the widgets: the hovered grid cell's, worn slot's or relic slot's lines. */
    void overlay(DrawContext g) {
        GearWire.View v = view();
        if (v == null || !tab.equals("armoury")) return;
        List<String> lines = new ArrayList<>();
        int[] anchor = null;
        int head = DarkUi.IVORY;
        if (hoverCell >= 0) {
            int at = scroll * GearView.COLUMNS + hoverCell;
            anchor = cell(hoverCell);
            if (filter.equals("relic")) {
                var owned = GearView.visibleRelics(v);
                if (at < owned.size()) { lines = relicTip(v, owned.get(at), -1); head = DarkUi.EPIC_INK; }
            } else {
                var pieces = GearView.visible(v, filter, sort);
                if (at < pieces.size()) { lines = GearView.tooltip(v, pieces.get(at)); head = rarityInk(pieces.get(at).rarity); }
            }
        } else if (hoverWorn != null) {
            anchor = worn(Gear.SLOTS.indexOf(hoverWorn));
            Gear.Piece p = GearView.wornIn(v, hoverWorn);
            if (p != null) {
                lines = new ArrayList<>(GearView.tooltip(v, p));
                if (!lines.isEmpty()) lines.set(lines.size() - 1, "Click to take it off");
                head = rarityInk(p.rarity);
            }
        } else if (hoverRelic >= 0) {
            anchor = relic(hoverRelic);
            String id = hoverRelic < v.relicSlots().size() ? v.relicSlots().get(hoverRelic) : "";
            if (!id.isEmpty() && Gear.relic(id) != null) { lines = relicTip(v, id, hoverRelic); head = DarkUi.EPIC_INK; }
        }
        if (anchor == null || lines.isEmpty()) return;
        UiCanvas.begin(g);
        try { tooltip(g, anchor, lines, head); }
        finally { UiCanvas.end(g); }
    }

    static List<String> relicTip(GearWire.View v, String id, int slot) {
        var r = Gear.relic(id);
        List<String> out = new ArrayList<>();
        if (r == null) return out;
        out.add(r.name());
        out.add("Relic · " + r.role() + " · " + (r.cooldownTicks() / 20) + " s cooldown");
        out.add(r.text());
        int at = v.relicSlots().indexOf(id);
        out.add(slot >= 0 ? "Key " + (slot + 1) + " · Shift-click to clear" : at >= 0 ? "In slot " + (at + 1) : "Click to slot it");
        return out;
    }

    /** The hover card, TIP_W wide at the anchor's right, flipped left and up so it stays MARGIN px inside the canvas. */
    static void tooltip(DrawContext g, int[] anchor, List<String> lines, int head) {
        var font = font();
        int pad = 8, inner = TIP_W - 2 * pad;
        List<Text> rows = new ArrayList<>();
        List<Integer> inks = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            int ink = i == 0 ? head : s.startsWith("▲") ? DarkUi.DAWN : s.startsWith("▼") || s.startsWith("Broken") ? DarkUi.ALARM_TEXT : i == lines.size() - 1 ? DarkUi.BRASS : DarkUi.IVORY;
            if (i == 0) { rows.add(DarkUi.heading(s)); inks.add(ink); continue; }
            for (String part : UiMath.lines(s, inner, 4, t -> font.getWidth(DarkUi.body(t)))) { rows.add(DarkUi.body(part)); inks.add(ink); }
        }
        int h = pad * 2 + 14 + (rows.size() - 1) * 12;
        int cw = UiCanvas.width(), ch = UiCanvas.height();
        int x = anchor[0] + anchor[2] + 6, y = anchor[1];
        if (x + TIP_W > cw - MARGIN) x = anchor[0] - 6 - TIP_W;
        if (y + h > ch - MARGIN) y = ch - MARGIN - h;
        x = Math.max(MARGIN, x);
        y = Math.max(MARGIN, y);
        g.getMatrices().push();
        g.getMatrices().translate(0, 0, 400);
        g.fill(x + 2, y + 2, x + TIP_W + 2, y + h + 2, DarkUi.DROP_LIFT);
        g.fill(x, y, x + TIP_W, y + h, DarkUi.CHARCOAL);
        outline(g, x, y, TIP_W, h, 1, DarkUi.BRASS);
        int ty = y + pad;
        for (int i = 0; i < rows.size(); i++) {
            g.drawText(font, rows.get(i), x + pad, ty, inks.get(i), false);
            ty += i == 0 ? 14 : 12;
        }
        g.getMatrices().pop();
        rectsDrawn.put("tooltip", new int[]{x, y, TIP_W, h});
    }

    // ---- input ---------------------------------------------------------------------------------------------------------------
    /** Grid, worn and relic clicks, hit-tested here in canvas px (x, y are GUI coordinates). Left-click equips, takes off or
     *  slots; shift-click breaks a piece down (Epic and above ask first) or clears a relic slot; right-click equips a piece and
     *  clears a relic slot. */
    boolean mouseClicked(double gx, double gy, int button) {
        if (!tab.equals("armoury")) return false;
        double x = UiCanvas.toCanvas(gx), y = UiCanvas.toCanvas(gy);
        GearWire.View v = view();
        if (v == null) return in(left(), x, y) || in(right(), x, y);
        boolean shift = Screen.hasShiftDown();
        for (int i = 0; i < Gear.SLOTS.size(); i++) if (in(worn(i), x, y)) {
            String slot = Gear.SLOTS.get(i);
            if (GearView.wornIn(v, slot) != null && button == 0) screen.action("gear:unequip:" + slot);
            return true;
        }
        for (int i = 0; i < 3; i++) if (in(relic(i), x, y)) {
            boolean full = i < v.relicSlots().size() && !v.relicSlots().get(i).isEmpty();
            if (full && (button == 1 || shift)) { screen.action("gear:relic:" + i + ":-"); selectedRelic = -1; }
            else if (button == 0) {
                selectedRelic = selectedRelic == i ? -1 : i;
                if (selectedRelic >= 0 && !filter.equals("relic")) { filter = "relic"; scroll = 0; confirmUid = -1; }
                UiSounds.toggled(selectedRelic >= 0);
            }
            return true;
        }
        int rows = gridRows();
        for (int i = 0; i < rows * GearView.COLUMNS; i++) {
            if (!in(cell(i), x, y)) continue;
            int at = scroll * GearView.COLUMNS + i;
            if (filter.equals("relic")) {
                var owned = GearView.visibleRelics(v);
                if (at < owned.size() && button == 0) {
                    screen.action("gear:relic:" + GearView.relicTarget(v, selectedRelic) + ":" + owned.get(at));
                    selectedRelic = -1;
                }
                return true;
            }
            var pieces = GearView.visible(v, filter, sort);
            if (at >= pieces.size()) return true;
            Gear.Piece p = pieces.get(at);
            if (shift && button == 0) breakdown(p);
            else if (!GearView.worn(v, p.uid) && (button == 0 || button == 1)) screen.action("gear:equip:" + p.uid);
            return true;
        }
        return false;
    }

    /** Breaks a piece down, or for Epic and above opens the confirm strip first. Package access for the screen suite. */
    void breakdown(Gear.Piece p) {
        if (p == null) return;
        if (GearView.confirmBreakdown(p)) { confirmUid = p.uid; UiSounds.deny(); sync(); }
        else screen.action("forge:breakdown:" + p.uid);
    }

    boolean mouseScrolled(double gx, double gy, double amount) {
        if (!tab.equals("armoury") || view() == null) return false;
        if (!in(right(), UiCanvas.toCanvas(gx), UiCanvas.toCanvas(gy))) return false;
        scroll = GearView.scrollClamp(scroll - (int) Math.signum(amount), count(view()), gridRows());
        return true;
    }

    /** Esc closes the confirm strip first, then the screen; the gear key closes the screen. */
    boolean keyPressed(int key, int scan) {
        var gearKey = GravesAndGloryClient.gearKey;
        if (key == GLFW.GLFW_KEY_ESCAPE && confirmUid >= 0) { confirmUid = -1; sync(); return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE || gearKey != null && gearKey.matchesKey(key, scan)) { screen.close(); return true; }
        return false;
    }
}
