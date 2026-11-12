package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.function.ToIntFunction;
import static gg.gravesandglory.GravesAndGloryClient.*;

/**
 * The Armoury tab of the barracks screen (in-match progression Task 8, War Table port plan Task 8), in the War Table's dress (Kort, 2026-10-31: it matches
 * the troop sheet): the oak and the rail, a note listing the banner's troop types (portrait, House seal and name) and a sheet beside it, a ledger with the
 * four buys of each troop on its row, Weapon II, Weapon III, Armour II and Armour III. Canvas pixels throughout (UiCanvas, at UiMath.pageScale as every
 * War Table page); the 960x540 page is offset by WarTable.column(COMPACT). The page READS the server's buy states (CampaignClient.armoury, state_v9) and
 * never works one out: a button is locked, open, bought or unaffordable as the server says, and a press sends gear:(troop):(weapon|armour):(tier), which
 * the server checks again. Every state carries a word and a glyph or a shape, never colour alone: a padlock and what is needed, a vellum tag with "Buy" and
 * the price, a check and "Bought", a flat greyed box with the price and "Not enough gold". Away from a buy place every buy is greyed and says "Not here",
 * the sheet's notice line gives the reason; with progression off the tab says "Ranks are off".
 * A tag on the note under the rows turns the sheet to the custom groups (command UI plan Task 12): the four columns become groups 1 to 4, each head a
 * name tag (press, type, Enter) over a Delete tag (pressed twice), the lowest empty column a New group tag, and each cell a tag that puts the row's
 * troop type in or out of that group. Every edit is a group: command (CustomGroups.command), which the server checks and saves with the profile.
 */
final class ArmouryTab {
    /** The page (960x540 coordinates): the note at NOTE_X, the sheet at SHEET_X, both from TOP; on the compact page every box moves by COMPACT. */
    static final int NOTE_X = 94, NOTE_W = 232, SHEET_X = 342, TOP = 124, NOTE_PAD = 16, PAD = 22, TEXT_W = 480;
    static final int[] COMPACT = {-53, -32};
    /** The rows' band on both papers (from the paper's top), a row's least height (the 40 px portrait and its frame), the most it grows to, the body line. */
    static final int ROW_Y = 52, ROWS_H = 220, MIN_ROW = 44, MAX_ROW = 48, LINE = 11, GAP = 4, GUTTER = 12, PORTRAIT = 40, ARROW = 24;
    /** The sheet's notice line (up to two lines) and, under it, the key line (from the sheet's top). */
    static final int NOTICE_Y = 278, HINT_Y = 302;
    /** The mode tag on the note, under the rows (from the note's top); the group heads' two tags (from the sheet's top). */
    static final int MODE_Y = 284, NAME_Y = 6, DELETE_Y = 28, HEAD_H = 20;
    /** The fills a buy is drawn on: the vellum tag of an open buy (the same whether hovered or not: the hover is an underline), the flat greyed box, the paper itself (bought, locked). */
    static final int OPEN_FILL = 0xfff1e7cb, OPEN_FOCUS = OPEN_FILL, OFF_FILL = DarkUi.VELLUM, PAPER_FILL = 0xffdccb9f;
    private static final String[] UP = {"...x...", "..xxx..", ".xxxxx.", "xxxxxxx"}, DOWN = {"xxxxxxx", ".xxxxx.", "..xxx..", "...x..."};
    /** The words of the fixed lines. */
    static final String TITLE = "Armoury", SUBTITLE = "Gear for this round, per troop type", OFF = "Ranks are off", OFF_WHY = "Troops earn no Rank and no Armoury gear is sold this match.",
        AWAY = "Buy at your barracks or a held point", GROUPS_SUB = "Numbered groups for orders", TO_GROUPS = "Custom groups", TO_GEAR = "Back to gear", NOT_HERE = "Not here", NO_GOLD = "Not enough gold", BOUGHT = "Bought", EMPTY = "Your banner has no troop types to buy gear for.";
    static final String[] GROUPS = {"Weapons", "Armour"}, COLUMNS = {"II", "III", "II", "III"};
    /** The wire tracks and tiers of the four columns, in order. */
    static final String[] TRACKS = {LevelMath.WEAPON, LevelMath.WEAPON, LevelMath.ARMOUR, LevelMath.ARMOUR};
    static final int[] TIERS = {2, 3, 2, 3};

    record Rect(int x, int y, int w, int h) {
        boolean has(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
        boolean inside(int cw, int ch) { return x >= 0 && y >= 0 && x + w <= cw && y + h <= ch; }
        boolean within(Rect o) { return x >= o.x && y >= o.y && x + w <= o.x + o.w && y + h <= o.y + o.h; }
        boolean overlaps(Rect o) { return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h; }
        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
    }

    final RecruitmentScreen screen;
    /** The troop ids the widgets were built for, and whether levels were on: a change rebuilds the page. */
    List<String> signature = List.of(); boolean builtOn;
    final List<Gear> buttons = new ArrayList<>();
    final List<Arrow> arrows = new ArrayList<>();
    /** Whether the sheet shows the custom groups (else the gear), the tags of that view, the mode tag, the group numbers it was built for. */
    boolean groupsMode; final List<GroupTag> groupTags = new ArrayList<>(); GroupTag modeTag; List<Integer> groupSignature = List.of();
    /** The group whose name is being typed (0 for none) and the name so far; a Delete pressed once (the group, until when). */
    int editing; String draft = ""; int deleteArmed; long deleteUntil;
    /** The first visible troop row, how many fit, how many rows the banner has, the row height, the most lines a button holds and whether the rows scroll. */
    int first, shown, total, rowH, lineCap = 2; boolean scrolling;
    /** The papers' top left corners in canvas px (the note's, the sheet's) and a buy tag's width. */
    int nx, ny, sx, sy, bw;
    /** What the page drew in its last frame (the checks read them): the notice line, the key line, the name lines of each visible row, the header's lines. */
    List<String> footerDrawn = List.of(), nameDrawn = List.of(), headerDrawn = List.of(), hintLines = List.of(); String hintDrawn = "", message = "";
    int footerX, footerRoom, hintX;
    /** A reason a greyed button gave when it was pressed, shown on the notice line for a few seconds, until the server says something newer (noticeSerial moves). */
    String note = ""; long noteUntil; int noteSerial;
    /** The server's notice count when the page opened: a notice newer than that is feedback for a buy. */
    int serialAtOpen = -1;
    final List<Rect> rowBoxes = new ArrayList<>(), nameBoxes = new ArrayList<>(), portraitBoxes = new ArrayList<>();
    Rect list = new Rect(0, 0, 0, 0);

    ArmouryTab(RecruitmentScreen screen) { this.screen = screen; }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }
    static ToIntFunction<String> width() { var f = font(); return s -> f.getWidth(DarkUi.body(s)); }
    static boolean levelsOn() { return Veterans.enabled(); }
    static int[] columnAt() { return WarTable.column(COMPACT[0], COMPACT[1]); }


    // ---- the words (one place for the copy; the checks derive their expectations from LevelMath and the sheet, never from these strings) ----

    /** The wire track as a word: Weapon or Armour. */
    static String trackWord(String track) { return LevelMath.ARMOUR.equals(track) ? "Armour" : "Weapon"; }

    /** The row's buy state for one of its four buttons. */
    static LevelMath.Buy stateOf(LevelMath.ArmouryRow row, String track, int tier) {
        boolean weapon = LevelMath.WEAPON.equals(track);
        return tier == 2 ? (weapon ? row.weapon2() : row.armour2()) : (weapon ? row.weapon3() : row.armour3());
    }

    /** The troop's name at the tier's level ("Knight" for tier II of the Order Squire; its own name where the troop's name does not change with level): the words the server's own refusal uses (Veterans.refusal). */
    static String ownName(Spec.TroopsRow t, int tier) { return LevelMath.levelName(t, tier); }

    /**
     * Why a locked button is locked, in the game's own copy (LevelMath.refusal): "Needs a Rank 2 Knight" until a soldier of the type has reached the tier's level, and for a
     * tier III with tier II still to buy, "Buy Weapon II first". The state packet carries the four states, not the highest level seen, so this names the one thing the
     * player can do next and is true whichever reason the server holds.
     */
    static String lockedText(Spec.TroopsRow t, String track, int tier, LevelMath.ArmouryRow row) {
        var below = tier >= 3 ? stateOf(row, track, tier - 1) : LevelMath.Buy.BOUGHT;
        boolean tierBelowOpen = below == LevelMath.Buy.OPEN || below == LevelMath.Buy.UNAFFORDABLE;
        return LevelMath.refusal(tier, tierBelowOpen ? tier : tier - 1, 1, 0, 0, true, track, ownName(t, tier));
    }

    /** The words on a button: its state, its price, what is needed. At most cap lines of w px (UiMath.lines); a locked text breaks at spaces and is never cut while the row has the room. */
    static List<String> words(Spec.TroopsRow t, String track, int tier, LevelMath.ArmouryRow row, LevelMath.Buy state, boolean here, int w, int cap, ToIntFunction<String> width) {
        int price = LevelMath.price(t.rarity(), tier);
        return switch (state) {
            case BOUGHT -> List.of(BOUGHT);
            case LOCKED -> UiMath.lines(lockedText(t, track, tier, row), w, cap, width);
            case OPEN -> here ? List.of("Buy", price + " gold") : List.of(price + " gold", NOT_HERE);
            case UNAFFORDABLE -> List.of(price + " gold", here ? NO_GOLD : NOT_HERE);
        };
    }

    /** The tooltip and the narrated sentence of a button: the troop, the tier, the state and what to do. */
    static String tipText(Spec.TroopsRow t, String track, int tier, LevelMath.ArmouryRow row, LevelMath.Buy state, boolean here, int gold) {
        String what = t.name() + ", " + trackWord(track) + " " + MatchStats.roman(tier) + ": ";
        int price = LevelMath.price(t.rarity(), tier);
        return what + switch (state) {
            case BOUGHT -> "bought. Every soldier of the type whose Rank allows it wears it.";
            case LOCKED -> lockedText(t, track, tier, row) + ".";
            case OPEN -> here ? price + " gold. Press to buy it for every " + t.name() + " this round." : price + " gold. " + AWAY + ".";
            case UNAFFORDABLE -> price + " gold, and you have " + gold + "." + (here ? "" : " " + AWAY + ".");
        };
    }

    /** The troop's best bought tier on a track, 1 when none: the tier its portrait wears. */
    static int bestTier(LevelMath.ArmouryRow row, String track) {
        boolean weapon = LevelMath.WEAPON.equals(track);
        if ((weapon ? row.weapon3() : row.armour3()) == LevelMath.Buy.BOUGHT) return 3;
        return (weapon ? row.weapon2() : row.armour2()) == LevelMath.Buy.BOUGHT ? 2 : 1;
    }

    /** The footer's key line: the player's own key for the Armoury, or how to bind one. */
    static String keyLine() {
        var k = binding("key.gravesandglory.armoury");
        if (k == null || k.isUnbound()) return "The Armoury key is not bound: set it in Controls";
        return hint("armoury") + " opens the Armoury anywhere in battle";
    }

    /** The rarity and group under a one-line name: "Common infantry". */
    static String kind(Spec.TroopsRow t) { return ArmyCards.rarityName(t) + " " + Morale.label(t.group()).toLowerCase(Locale.ROOT); }

    static List<String> ids(List<LevelMath.ArmouryRow> rows) { List<String> out = new ArrayList<>(); for (var r : rows) out.add(r.troop()); return out; }

    // ---- layout ----

    /** The most lines any button of the banner could need at this inner width, over every troop and both reasons a locked button gives: the row is tall enough for it, at least two lines. */
    static int worstLines(List<LevelMath.ArmouryRow> rows, int w, ToIntFunction<String> width) {
        int worst = 2;
        for (var r : rows) {
            var t = ArmyCards.spec(r.troop()); if (t == null) continue;
            for (String track : new String[] {LevelMath.WEAPON, LevelMath.ARMOUR}) for (int tier = 2; tier <= 3; tier++) {
                worst = Math.max(worst, UiMath.lines(LevelMath.refusal(tier, tier - 1, 1, 0, 0, true, track, ownName(t, tier)), w, 5, width).size());
                if (tier == 3) worst = Math.max(worst, UiMath.lines(LevelMath.refusal(3, 3, 1, 0, 0, true, track, null), w, 5, width).size());
            }
        }
        return worst;
    }

    /** The height a row needs for this many text lines in a buy tag: the lines and the tag's padding, never less than the portrait's frame. */
    static int rowFor(int lines) { return Math.max(MIN_ROW, lines * LINE + 10); }

    /** The width of a buy tag: the sheet's text width over the four columns, a gap between the two of a track and a wider gutter between the tracks. */
    static int tagWidth() { return (TEXT_W - 2 * GAP - GUTTER) / 4; }

    /** The page's measures: a buy tag's width, the row height, rows shown, whether the rows scroll and the lines a buy holds. */
    record Layout(int bw, int rowH, int shown, boolean scrolling, int lines) {}

    static Layout layout(List<LevelMath.ArmouryRow> rows, ToIntFunction<String> width) {
        int n = rows.size(), bw = tagWidth(), lines = worstLines(rows, bw - 8 - 14, width), rowMin = rowFor(lines), capacity = Math.max(1, ROWS_H / rowMin);
        int shown = Math.min(Math.max(n, 1), capacity), rowH = Math.max(rowMin, Math.min(MAX_ROW, ROWS_H / Math.max(1, shown)));
        return new Layout(bw, rowH, shown, n > capacity, lines);
    }

    /** x of a column's buy tag on the sheet: the two weapon columns, then the two armour columns after the gutter. */
    int columnX(int col) { return sx + PAD + col * (bw + GAP) + (col >= 2 ? GUTTER - GAP : 0); }

    /** Builds the page: the layout, the buy tags of every visible row and the scroll arrows. Called by the screen's init and when the rows change. */
    void init() {
        var rows = CampaignClient.armoury.rows();
        signature = ids(rows); builtOn = levelsOn(); buttons.clear(); arrows.clear(); rowBoxes.clear(); nameBoxes.clear(); portraitBoxes.clear(); groupTags.clear(); modeTag = null;
        groupSignature = groupNumbers();
        if (serialAtOpen < 0) serialAtOpen = CampaignClient.noticeSerial;
        int[] o = columnAt(); nx = o[0] + NOTE_X; ny = o[1] + TOP; sx = o[0] + SHEET_X; sy = o[1] + TOP; bw = tagWidth();
        total = rows.size(); shown = 0; scrolling = false; list = new Rect(sx + PAD, sy + ROW_Y, TEXT_W, 0);
        if (rows.isEmpty()) { groupsMode = false; return; }
        modeTag = screen.add(new GroupTag(this, GroupTag.MODE, 0, null, new Rect(nx + NOTE_PAD, ny + MODE_Y, NOTE_W - 2 * NOTE_PAD, ARROW)));
        if (!builtOn && !groupsMode) return;
        var lay = layout(rows, width());
        rowH = lay.rowH(); shown = lay.shown(); scrolling = lay.scrolling(); lineCap = lay.lines();
        first = Math.max(0, Math.min(first, total - shown));
        list = new Rect(sx + PAD, sy + ROW_Y, TEXT_W, shown * rowH);
        for (int r = 0; r < shown; r++) {
            var row = rows.get(first + r); var t = ArmyCards.spec(row.troop()); if (t == null) continue;
            int y = sy + ROW_Y + r * rowH;
            rowBoxes.add(new Rect(sx + PAD, y, TEXT_W, rowH));
            nameBoxes.add(new Rect(nx + NOTE_PAD + PORTRAIT + 28, y, NOTE_W - NOTE_PAD * 2 - PORTRAIT - 28, rowH));
            portraitBoxes.add(new Rect(nx + NOTE_PAD + 1, y + (rowH - PORTRAIT) / 2, PORTRAIT, PORTRAIT));
            for (int col = 0; col < 4; col++) {
                var box = new Rect(columnX(col), y + 3, bw, rowH - 6);
                if (!groupsMode) buttons.add(screen.add(new Gear(this, t, TRACKS[col], TIERS[col], box)));
                else if (group(col + 1) != null) groupTags.add(screen.add(new GroupTag(this, GroupTag.MEMBER, col + 1, t, box)));
            }
        }
        if (groupsMode) {
            int fresh = freeNumber();
            for (int col = 0; col < 4; col++) {
                int n = col + 1, x = columnX(col);
                if (group(n) != null) {
                    groupTags.add(screen.add(new GroupTag(this, GroupTag.NAME, n, null, new Rect(x, sy + NAME_Y, bw, HEAD_H))));
                    groupTags.add(screen.add(new GroupTag(this, GroupTag.DELETE, n, null, new Rect(x, sy + DELETE_Y, bw, HEAD_H))));
                } else if (n == fresh) groupTags.add(screen.add(new GroupTag(this, GroupTag.NEW, n, null, new Rect(x, sy + NAME_Y, bw, DELETE_Y + HEAD_H - NAME_Y))));
            }
        }
        if (scrolling) {
            int ax = sx + PAD + TEXT_W - ARROW, ay = sy + NOTICE_Y - 2;
            arrows.add(screen.add(new Arrow(this, true, new Rect(ax - ARROW - GAP, ay, ARROW, ARROW))));
            arrows.add(screen.add(new Arrow(this, false, new Rect(ax, ay, ARROW, ARROW))));
        }
    }

    /** One tick: the rows the server sent changed (a new banner) or levels were switched, so the page is built again. */
    void tick() {
        if (!ids(CampaignClient.armoury.rows()).equals(signature) || levelsOn() != builtOn || groupsMode && !groupNumbers().equals(groupSignature)) screen.rebuild();
    }

    // ---- the custom groups (Profile.customGroups, the client's copy from the snapshot; the server keeps the real one) ----

    static Profile.CustomGroup group(int n) { return CampaignClient.profile.customGroup(n); }
    static List<Integer> groupNumbers() {
        List<Integer> out = new ArrayList<>();
        if (CampaignClient.profile.customGroups != null) for (var g : CampaignClient.profile.customGroups) out.add(g.number());
        return out;
    }
    /** The number group:new would take (the lowest free one), or 0 when all four are kept. */
    static int freeNumber() { for (int n = 1; n <= Profile.MAX_CUSTOM_GROUPS; n++) if (group(n) == null) return n; return 0; }
    /** Switches the sheet between the gear and the groups. */
    void toggleGroups() { groupsMode = !groupsMode; editing = 0; deleteArmed = 0; first = 0; screen.rebuild(); }
    void say(String text) { note = text; noteUntil = System.currentTimeMillis() + 4000; noteSerial = CampaignClient.noticeSerial; }

    /** Scrolls by rows; true when the page scrolls at all (so the wheel is not passed on). */
    boolean scroll(int rows) {
        if (!scrolling) return false;
        int next = Math.max(0, Math.min(first + rows, total - shown));
        if (next != first) { first = next; screen.rebuild(); }
        return true;
    }

    // ---- drawing (canvas pixels, between UiCanvas.begin and end) ----

    static int text(DrawContext g, String s, int x, int y, int colour) { g.drawText(font(), DarkUi.body(s), x, y, colour, false); return width().applyAsInt(s); }
    static void centre(DrawContext g, String s, int cx, int y, int colour) { text(g, s, cx - width().applyAsInt(s) / 2, y, colour); }
    static void headCentre(DrawContext g, String s, int cx, int y, int colour) { var t = DarkUi.heading(s); g.drawText(font(), t, cx - font().getWidth(t) / 2, y, colour, false); }

    /** The page behind its buttons: the oak and the rail, the note (title, purse, each row's portrait, seal and name, the key line) and the sheet (the column heads, the rows' rules, the notice line). The screen draws the buttons after this. */
    void render(DrawContext g) {
        var w = width(); var font = font();
        WarTable.drawChrome(g, "recruit");
        UiTex.draw(g, "table/note-armoury", nx, ny); // as tall as the sheet
        UiTex.draw(g, "table/sheet-field", sx, sy);
        List<String> head = new ArrayList<>();
        // the note's head: the title and a rule, the purse at the right, the subtitle (or which rows show) under it
        int tx = nx + NOTE_PAD, tw = NOTE_W - 2 * NOTE_PAD;
        g.drawText(font, DarkUi.heading(TITLE), tx, ny + 16, DarkUi.INK_TEXT, false); head.add(TITLE);
        g.fill(tx, ny + 28, tx + tw, ny + 29, DarkUi.SEPIA_LINE);
        String gold = GravesAndGloryClient.gold + " gold"; int gw = w.applyAsInt(gold);
        text(g, gold, tx + tw - gw, ny + 17, DarkUi.INK_TEXT); UiTex.draw(g, "recruit/coin-m", tx + tw - gw - 10, ny + 19); head.add(gold);
        var rows = CampaignClient.armoury.rows();
        String sub = scrolling ? "Troops " + (first + 1) + "-" + (first + shown) + " of " + total : groupsMode ? GROUPS_SUB : SUBTITLE;
        text(g, sub, tx, ny + 34, DarkUi.SEPIA); head.add(sub);
        // the sheet's heads: the two tracks over their columns, each with its rule, the tiers under them
        if (groupsMode) {
            for (int col = 0; col < 4; col++) if (group(col + 1) == null && col + 1 != freeNumber()) { centre(g, "No group " + (col + 1), columnX(col) + bw / 2, sy + 22, DarkUi.SEPIA); head.add("No group " + (col + 1)); }
        } else for (int t = 0; t < 2; t++) {
            int x0 = columnX(2 * t), x1 = columnX(2 * t + 1) + bw;
            headCentre(g, GROUPS[t], (x0 + x1) / 2, sy + 16, DarkUi.INK_TEXT); head.add(GROUPS[t]);
            g.fill(x0, sy + 28, x1, sy + 29, DarkUi.SEPIA_LINE);
        }
        if (!groupsMode) for (int col = 0; col < 4; col++) { headCentre(g, COLUMNS[col], columnX(col) + bw / 2, sy + 35, DarkUi.SEPIA); head.add(COLUMNS[col]); }
        List<String> names = new ArrayList<>();
        message = "";
        if (!builtOn && !groupsMode) {
            headCentre(g, OFF, sx + PAD + TEXT_W / 2, sy + 110, DarkUi.INK_TEXT); centre(g, OFF_WHY, sx + PAD + TEXT_W / 2, sy + 128, DarkUi.SEPIA); message = OFF; head.add(OFF); head.add(OFF_WHY);
        } else if (rows.isEmpty()) {
            message = EMPTY; centre(g, message, sx + PAD + TEXT_W / 2, sy + 110, DarkUi.INK_TEXT); head.add(message);
        } else {
            for (int r = 0; r < rowBoxes.size() && first + r < rows.size(); r++) {
                var card = rowBoxes.get(r); var t = ArmyCards.spec(rows.get(first + r).troop()); var house = ArmyCards.house(t);
                if (r > 0) { // a rule between rows, across both papers
                    g.fill(tx, card.y(), tx + tw, card.y() + 1, DarkUi.SEPIA_LINE);
                    g.fill(card.x(), card.y(), card.x() + card.w(), card.y() + 1, DarkUi.SEPIA_LINE);
                }
                var pb = portraitBoxes.get(r);
                UiTex.draw(g, "table/frame-back-48", pb.x(), pb.y());
                int edge = house == null ? DarkUi.SEPIA : 0xff000000 | house.accent(); // the frame in the House's own colour
                g.drawBorder(pb.x() - 1, pb.y() - 1, pb.w() + 2, pb.h() + 2, edge);
                if (house != null) g.drawTexture(new Identifier(house.glyph()), pb.x() + pb.w() + 6, card.y() + (card.h() - 16) / 2, 0, 0, 16, 16, 16, 16);
                var nb = nameBoxes.get(r); var lines = UiMath.lines(t.name(), nb.w(), 2, w);
                if (lines.size() == 1) lines = List.of(lines.get(0), kind(t));
                int ty = card.y() + (card.h() - LINE * lines.size()) / 2 + 1;
                for (int i = 0; i < lines.size(); i++) { text(g, lines.get(i), nb.x(), ty + LINE * i, i == 0 ? DarkUi.INK_TEXT : DarkUi.SEPIA); names.add(lines.get(i)); }
            }
            // the models last, over the papers: each at the best tiers its army has bought
            var c = MinecraftClient.getInstance();
            for (int r = 0; r < portraitBoxes.size() && first + r < rows.size(); r++) {
                var row = rows.get(first + r); var t = ArmyCards.spec(row.troop()); var pb = portraitBoxes.get(r);
                var e = ArmyCards.portrait(t, bestTier(row, LevelMath.WEAPON), bestTier(row, LevelMath.ARMOUR));
                if (e != null) { e.age = c.player == null ? 0 : c.player.age; Portrait.draw(g, e, pb.x(), pb.y(), PORTRAIT); }
            }
        }
        nameDrawn = names; headerDrawn = head;
        footer(g);
    }

    /** The sheet's notice line: a reason a greyed button gave, else a fresh notice of the server's (a buy's outcome), else why nothing can be bought here; the key line on the note. */
    void footer(DrawContext g) {
        var w = width();
        String keys = keyLine(); int hx = sx + PAD;
        var hl = UiMath.lines(keys, TEXT_W, 1, w);
        for (int i = 0; i < hl.size(); i++) text(g, hl.get(i), hx, sy + HINT_Y + LINE * i, DarkUi.SEPIA);
        hintDrawn = keys; hintLines = hl; hintX = hx;
        String line; int colour;
        if (!note.isEmpty() && System.currentTimeMillis() < noteUntil && CampaignClient.noticeSerial == noteSerial) { line = note; colour = DarkUi.INK_TEXT; }
        else if (CampaignClient.noticeSerial > serialAtOpen && !CampaignClient.shownNotice().isEmpty()) { line = CampaignClient.shownNotice(); colour = DarkUi.INK_TEXT; }
        else if (!groupsMode && builtOn && !CampaignClient.armoury.rows().isEmpty() && !CampaignClient.armoury.here()) { line = AWAY; colour = DarkUi.SEPIA; }
        else { line = ""; colour = DarkUi.SEPIA; }
        int x = sx + PAD, room = TEXT_W - (scrolling ? 2 * ARROW + GAP + 8 : 0);
        var lines = line.isEmpty() ? List.<String>of() : UiMath.lines(line, room, 2, w);
        for (int i = 0; i < lines.size(); i++) text(g, lines.get(i), x, sy + NOTICE_Y + LINE * i, colour);
        footerDrawn = lines; footerX = x; footerRoom = room;
    }

    // ---- the buttons ----

    /** One of the four buy tags of a troop row. It reads its state from the server's Armoury each frame and sends only when the server has the buy open and the commander stands where he may buy. */
    static final class Gear extends UiButton {
        final ArmouryTab tab; final Spec.TroopsRow troop; final String track; final int tier; final Rect box;
        LevelMath.Buy state = LevelMath.Buy.LOCKED; boolean here, pressable; List<String> lines = List.of(); String tip = "";
        /** The widest line, the line count and the colours (fill, first line, other lines) as the last frame drew them. */
        int drawnWidest, drawnLines, fillUsed, mainUsed, secondUsed;
        long sentUntil; int sentAt = -1;
        Gear(ArmouryTab tab, Spec.TroopsRow troop, String track, int tier, Rect box) {
            super(box.x(), box.y(), box.w(), box.h(), Text.literal(troop.name() + " " + trackWord(track) + " " + MatchStats.roman(tier)), b -> {}, null);
            this.tab = tab; this.troop = troop; this.track = track; this.tier = tier; this.box = box; refresh();
        }
        LevelMath.ArmouryRow row() { for (var r : CampaignClient.armoury.rows()) if (r.troop().equals(troop.id())) return r; return null; }
        String command() { return "gear:" + troop.id() + ":" + track + ":" + tier; }
        boolean hasGlyph() { return state == LevelMath.Buy.LOCKED || state == LevelMath.Buy.BOUGHT; }
        /** The width of the tag's text area: the tag less its padding and the glyph's room (a padlock or a check, 10 px and a gap). */
        int innerW() { return box.w() - 8 - (hasGlyph() ? 14 : 0); }
        void refresh() {
            var row = row();
            here = CampaignClient.armoury.here(); state = row == null ? LevelMath.Buy.LOCKED : stateOf(row, track, tier);
            boolean sent = sentAt == CampaignClient.acceptedStates && System.currentTimeMillis() < sentUntil;
            pressable = row != null && state == LevelMath.Buy.OPEN && here && !sent;
            if (row != null) {
                lines = words(troop, track, tier, row, state, here, innerW(), Math.max(2, tab.lineCap), width());
                String next = tipText(troop, track, tier, row, state, here, GravesAndGloryClient.gold);
                if (!next.equals(tip)) { tip = next; setTooltip(Tooltip.of(Text.literal(next))); setMessage(Text.literal(next)); }
            }
        }
        /** What a press says when the buy is not open: why. A silent button is never offered. */
        String reason() {
            var row = row(); if (row == null) return "";
            return switch (state) {
                case LOCKED -> lockedText(troop, track, tier, row);
                case BOUGHT -> trackWord(track) + " " + MatchStats.roman(tier) + " is bought for the " + troop.name() + ".";
                default -> !here ? AWAY : state == LevelMath.Buy.UNAFFORDABLE ? NO_GOLD : "";
            };
        }
        @Override public void onPress() {
            refresh();
            if (pressable) {
                sentAt = CampaignClient.acceptedStates; sentUntil = System.currentTimeMillis() + 1500; // a second press waits for the next state packet, which shows the buy
                tab.note = ""; send(command());
            } else { UiSounds.deny(); tab.note = reason(); tab.noteUntil = System.currentTimeMillis() + 4000; tab.noteSerial = CampaignClient.noticeSerial; }
        }
        @Override public void playDownSound(SoundManager sounds) { if (pressable) super.playDownSound(sounds); }
        /** An open buy is a vellum tag in ink; a greyed one a flat vellum box with a ruled edge in sepia; a bought one the paper with a check in ink; a locked one the paper with a padlock in sepia. Hovered or focused, an open tag underlines its first line and a greyed box darkens its edge. */
        @Override protected void paint(DrawContext g, int x, int y, int w, int h, boolean hot) {
            refresh();
            int fill, main, second; String glyph = null;
            switch (state) {
                case OPEN -> {
                    if (pressable) { fill = OPEN_FILL; main = DarkUi.INK_TEXT; second = DarkUi.INK_TEXT; }
                    else { fill = OFF_FILL; main = DarkUi.SEPIA; second = DarkUi.SEPIA; }
                }
                case UNAFFORDABLE -> { fill = OFF_FILL; main = DarkUi.SEPIA; second = DarkUi.SEPIA; }
                case BOUGHT -> { fill = PAPER_FILL; main = DarkUi.INK_TEXT; second = DarkUi.INK_TEXT; glyph = "check"; }
                default -> { fill = PAPER_FILL; main = DarkUi.SEPIA; second = DarkUi.SEPIA; glyph = "lock"; }
            }
            if (fill == OPEN_FILL) UiTex.nine(g, "table/btn-vellum-64x24", x, y, w + 1, h + 2);
            else { g.fill(x, y, x + w, y + h, fill); g.drawBorder(x, y, w, h, hot ? DarkUi.SEPIA : DarkUi.SEPIA_LINE); }
            if ("lock".equals(glyph)) UiTex.draw(g, "recruit/padlock-brass", x + 5, y + (h - 12) / 2);
            if ("check".equals(glyph)) UiTex.draw(g, "table/ready-check", x + 5, y + (h - 8) / 2);
            var fw = width(); int n = lines.size(), ty = y + (h - LINE * n) / 2 + 1, widest = 0;
            for (int i = 0; i < n; i++) {
                String s = lines.get(i); int sw = fw.applyAsInt(s); widest = Math.max(widest, sw);
                int tx = glyph != null ? x + 18 : x + (w - sw) / 2;
                g.drawText(font(), DarkUi.body(s), tx, ty + LINE * i, i == 0 ? main : second, false);
                if (i == 0 && hot && pressable) g.fill(tx, ty + 9, tx + sw, ty + 10, DarkUi.INK_TEXT);
            }
            drawnWidest = widest; drawnLines = n; fillUsed = fill; mainUsed = main; secondUsed = second;
        }
    }

    static void glyph(DrawContext g, String[] rows, int x, int y, int colour) {
        for (int j = 0; j < rows.length; j++) for (int i = 0; i < rows[j].length(); i++) if (rows[j].charAt(i) == 'x') g.fill(x + i, y + j, x + i + 1, y + j + 1, colour);
    }

    /** The two scroll buttons on the sheet's notice line: a triangle each (a shape, not a colour), 24 x 24, a vellum tag while there is more to see. */
    static final class Arrow extends UiButton {
        final ArmouryTab tab; final boolean up; final Rect box;
        Arrow(ArmouryTab tab, boolean up, Rect box) {
            super(box.x(), box.y(), box.w(), box.h(), Text.literal(up ? "Earlier troops" : "Later troops"), b -> {}, null);
            this.tab = tab; this.up = up; this.box = box; setTooltip(Tooltip.of(getMessage()));
        }
        boolean live() { return up ? tab.first > 0 : tab.first < tab.total - tab.shown; }
        @Override public void onPress() { tab.scroll(up ? -1 : 1); }
        @Override protected void paint(DrawContext g, int x, int y, int w, int h, boolean hot) {
            boolean live = live();
            if (live) UiTex.nine(g, "table/btn-vellum-64x24", x, y, w + 1, h + 2);
            else { g.fill(x, y, x + w, y + h, OFF_FILL); g.drawBorder(x, y, w, h, DarkUi.SEPIA_LINE); }
            if (hot) g.drawBorder(x, y, w, h, DarkUi.SEPIA);
            glyph(g, up ? UP : DOWN, x + (w - 7) / 2, y + (h - 4) / 2, live ? DarkUi.INK_TEXT : DarkUi.SEPIA_LINE);
        }
    }

    /**
     * A tag of the groups view: MODE (on the note, gear or groups), NEW (makes the lowest free group), NAME (the group's number and name; pressed,
     * it takes typing, Enter or a second press sends the name, Backspace takes a letter), DELETE (pressed twice within three seconds) and MEMBER
     * (the row's troop type in or out of the group: a check and "In group" on the paper, or a vellum "Add"). Same dress as the buy tags.
     */
    static final class GroupTag extends UiButton {
        static final String MODE = "mode", NEW = "new", NAME = "name", DELETE = "delete", MEMBER = "member";
        final ArmouryTab tab; final String kind; final int number; final Spec.TroopsRow troop; final Rect box;
        List<String> lines = List.of(); String tip = "";
        GroupTag(ArmouryTab tab, String kind, int number, Spec.TroopsRow troop, Rect box) {
            super(box.x(), box.y(), box.w(), box.h(), Text.literal(kind), b -> {}, null);
            this.tab = tab; this.kind = kind; this.number = number; this.troop = troop; this.box = box; refresh();
        }
        Profile.CustomGroup group() { return ArmouryTab.group(number); }
        boolean member() { var g = group(); return troop != null && g != null && g.troopIds().contains(troop.id()); }
        boolean hasGlyph() { return MEMBER.equals(kind) && member(); }
        boolean typing() { return NAME.equals(kind) && tab.editing == number; }
        boolean armed() { return DELETE.equals(kind) && tab.deleteArmed == number && System.currentTimeMillis() < tab.deleteUntil; }
        void refresh() {
            var g = group(); String label, next;
            switch (kind) {
                case MODE -> { label = tab.groupsMode ? TO_GEAR : TO_GROUPS; next = tab.groupsMode ? "Show the gear buys again." : "Make numbered groups of troop types to order together."; }
                case NEW -> { label = "New group"; next = "Makes group " + number + ". Then add troop types to it."; }
                case NAME -> { String name = typing() ? tab.draft + "_" : g == null ? "" : g.name(); label = number + " " + name; next = "Group " + number + ": press to rename, type, then Enter."; }
                case DELETE -> { label = armed() ? "Press to delete" : "Delete"; next = "Deletes group " + number + ". Press twice."; }
                default -> { boolean in = member(); label = in ? "In group" : "Add"; next = troop.name() + (in ? " is in group " + number + ". Press to take it out." : " is not in group " + number + ". Press to add it."); }
            }
            lines = UiMath.lines(label, box.w() - 8 - (hasGlyph() ? 14 : 0), 1, width());
            if (!next.equals(tip)) { tip = next; setTooltip(Tooltip.of(Text.literal(next))); setMessage(Text.literal(next)); }
        }
        @Override public void onPress() {
            var g = group();
            switch (kind) {
                case MODE -> tab.toggleGroups();
                case NEW -> send("group:new");
                case NAME -> { if (typing()) commit(); else if (g != null) { tab.editing = number; tab.draft = g.name(); } }
                case DELETE -> {
                    if (armed()) { tab.deleteArmed = 0; send("group:delete:" + number); }
                    else { tab.deleteArmed = number; tab.deleteUntil = System.currentTimeMillis() + 3000; tab.say("Press Delete again to delete group " + number + "."); }
                }
                default -> { if (g != null && troop != null) send("group:" + (member() ? "remove:" : "add:") + number + ":" + troop.id()); }
            }
            refresh();
        }
        void commit() {
            String name = Profile.groupName(tab.draft, number); tab.editing = 0;
            var g = group(); if (g != null && !name.equals(g.name())) send("group:name:" + number + ":" + name);
        }
        @Override public boolean charTyped(char chr, int mods) {
            if (!typing()) return super.charTyped(chr, mods);
            if (chr >= ' ' && chr != 127 && tab.draft.length() < Profile.GROUP_NAME_MAX) tab.draft += chr;
            refresh(); return true;
        }
        @Override public boolean keyPressed(int key, int scan, int mods) {
            if (!typing()) return super.keyPressed(key, scan, mods);
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) return false; // the screen closes; the typing is dropped
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) commit();
            else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !tab.draft.isEmpty()) tab.draft = tab.draft.substring(0, tab.draft.length() - 1);
            refresh(); return true;
        }
        @Override public void setFocused(boolean focused) { super.setFocused(focused); if (!focused && typing()) commit(); }
        /** A tag that acts is a vellum tag in ink; an "In group" cell the paper with a check in ink; the name being typed a ruled box with a caret. Hovered, the first line is underlined. */
        @Override protected void paint(DrawContext g, int x, int y, int w, int h, boolean hot) {
            refresh();
            boolean check = hasGlyph(), typing = typing();
            if (check || typing) { g.fill(x, y, x + w, y + h, PAPER_FILL); g.drawBorder(x, y, w, h, hot || typing ? DarkUi.SEPIA : DarkUi.SEPIA_LINE); }
            else UiTex.nine(g, "table/btn-vellum-64x24", x, y, w + 1, h + 2);
            if (check) UiTex.draw(g, "table/ready-check", x + 5, y + (h - 8) / 2);
            var fw = width(); int n = lines.size(), ty = y + (h - LINE * n) / 2 + 1;
            for (int i = 0; i < n; i++) {
                String s = lines.get(i); int sw = fw.applyAsInt(s), tx = check ? x + 18 : typing ? x + 4 : x + (w - sw) / 2;
                g.drawText(font(), DarkUi.body(s), tx, ty + LINE * i, DarkUi.INK_TEXT, false);
                if (i == 0 && hot && !typing) g.fill(tx, ty + 9, tx + sw, ty + 10, DarkUi.INK_TEXT);
            }
        }
    }

    // ---- what the checks read ----

    /** Every way the page fails to fit its canvas, in words (an empty list for none): a tag outside the canvas or off its sheet, under 20 x 20, overlapping another, a line wider than its box or cut with an ellipsis, a name cut or off the note, the notice line past its room, the key line past the note. */
    List<String> fitFailures() {
        List<String> bad = new ArrayList<>(); var w = width(); int W = UiCanvas.width(), H = UiCanvas.height();
        Rect sheet = new Rect(sx, sy, UiTex.entry("table/sheet-field").w(), UiTex.entry("table/sheet-field").h()), paper = new Rect(nx, ny, NOTE_W, UiTex.entry("table/note-armoury").h());
        if (!sheet.inside(W, H) || !paper.inside(W, H)) bad.add("a paper outside the canvas " + W + "x" + H);
        var all = new ArrayList<Rect>();
        for (var b : buttons) {
            var r = b.box; all.add(r); String who = b.troop.id() + " " + b.track + " " + b.tier;
            if (!r.inside(W, H)) bad.add(who + " outside the canvas " + W + "x" + H);
            if (!r.within(sheet)) bad.add(who + " off the sheet");
            if (r.w() < 20 || r.h() < 20) bad.add(who + " is " + r.w() + "x" + r.h() + ", under 20x20");
            int widest = 0; for (var s : b.lines) widest = Math.max(widest, w.applyAsInt(s));
            if (widest > b.innerW()) bad.add(who + " line of " + widest + " px in " + b.innerW());
            if (b.lines.stream().anyMatch(s -> s.endsWith("..."))) bad.add(who + " has a cut line " + b.lines);
            if (b.lines.size() * LINE > r.h() - 2) bad.add(who + " " + b.lines.size() + " lines in " + r.h() + " px");
            if (r.y() + r.h() > sy + NOTICE_Y - 2) bad.add(who + " reaches the notice line");
        }
        for (var t : groupTags) {
            all.add(t.box); String who = "group tag " + t.kind + " " + t.number;
            if (!t.box.inside(W, H) || !t.box.within(sheet)) bad.add(who + " off the sheet");
            if (t.box.w() < 20 || t.box.h() < 20) bad.add(who + " under 20x20");
            for (var s : t.lines) if (w.applyAsInt(s) > t.box.w() - 8 - (t.hasGlyph() ? 14 : 0) || s.endsWith("...")) bad.add(who + " line " + s + " does not fit");
            if (GroupTag.MEMBER.equals(t.kind) && t.box.y() + t.box.h() > sy + NOTICE_Y - 2) bad.add(who + " reaches the notice line");
        }
        if (modeTag != null && (!modeTag.box.within(paper) || modeTag.box.h() < 20)) bad.add("the groups tag is off the note");
        for (var a : arrows) { all.add(a.box); if (!a.box.inside(W, H) || !a.box.within(sheet)) bad.add("a scroll button off the sheet"); if (a.box.w() < 20 || a.box.h() < 20) bad.add("a scroll button under 20x20"); }
        for (int i = 0; i < all.size(); i++) for (int j = i + 1; j < all.size(); j++) if (all.get(i).overlaps(all.get(j))) bad.add("buttons " + i + " and " + j + " overlap");
        for (int r = 0; r < rowBoxes.size(); r++) {
            if (first + r >= total) break;
            var nb = nameBoxes.get(r);
            var t = ArmyCards.spec(CampaignClient.armoury.rows().get(first + r).troop());
            for (var s : UiMath.lines(t.name(), nb.w(), 2, w)) if (w.applyAsInt(s) > nb.w() || s.endsWith("...")) bad.add("the name " + s + " is cut in " + nb.w());
            if (!nb.within(paper) || !portraitBoxes.get(r).within(paper)) bad.add("row " + r + " off the note");
        }
        int widest = 0; for (var s : footerDrawn) widest = Math.max(widest, w.applyAsInt(s));
        if (widest > footerRoom) bad.add("the notice line is " + widest + " px in " + footerRoom);
        if (footerDrawn.stream().anyMatch(s -> s.endsWith("..."))) bad.add("the notice line is cut " + footerDrawn);
        int hw = 0; for (var s : hintLines) hw = Math.max(hw, w.applyAsInt(s));
        if (hw > TEXT_W || hintLines.stream().anyMatch(s -> s.endsWith("..."))) bad.add("the key line does not fit the sheet " + hintLines);
        if (sy + HINT_Y + LINE * hintLines.size() > sheet.y() + sheet.h() - 8) bad.add("the key line runs off the sheet");
        if (paper.h() != sheet.h()) bad.add("the note is " + paper.h() + " px tall and the sheet " + sheet.h());
        return bad;
    }
}
