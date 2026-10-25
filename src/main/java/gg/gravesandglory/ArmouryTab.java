package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import java.util.*;
import java.util.function.ToIntFunction;
import static gg.gravesandglory.GravesAndGloryClient.*;

/**
 * The Armoury tab of the barracks screen (in-match progression Task 8, War Table port plan Task 8), in today's barracks style: DarkUi tokens, Marcellus
 * headings, Lato body, the barracks' own flat widgets. One row per troop type of the banner: its portrait at the best tiers the army has bought, its name
 * and four buttons, Weapon II, Weapon III, Armour II and Armour III. The page READS the server's buy states (CampaignClient.armoury, state_v9) and never
 * works one out: a button is locked, open, bought or unaffordable as the server says, and a press sends gear:(troop):(weapon|armour):(tier), which the server
 * checks again. Every state carries a word and a glyph, never colour alone: a padlock and what is needed, "Buy" and the price, a check and "Bought",
 * the price and "Not enough gold". Away from a buy place every buy is greyed and says "Not here", the footer gives the reason; with progression off the
 * tab says "Levels are off". The look is the barracks'; the War Table restyle comes after Kort's gate.
 */
final class ArmouryTab {
    static final int TOP = 84, LINE = 11, GAP = 4, GUTTER = 10, PORTRAIT = 44, SCROLL = 28, MAX_ROW = 60, MARGIN = 18, ARROW = 24, MAX_CONTENT = 1240;
    /** The barracks' own fills and edges (DarkUi tokens and FantasyButton's): the row card, an open buy, a greyed button, a locked one. */
    static final int ROW = 0xff222d32, OPEN_FILL = DarkUi.TAB_CHOSEN, OPEN_FOCUS = DarkUi.TAB_FOCUS, OFF_FILL = 0xff20272b, LOCKED_FILL = 0xff1b2226, LOCKED_EDGE = 0xff39464b, EDGE = DarkUi.TAB_EDGE;
    private static final String[] LOCK = {"..xxx..", ".x...x.", ".x...x.", "xxxxxxx", "xxxxxxx", "xxx.xxx", "xxx.xxx", "xxxxxxx", "xxxxxxx"};
    private static final String[] CHECK = {"......x", ".....xx", "x...xx.", "xx.xx..", ".xxx...", "..x...."};
    private static final String[] UP = {"..x..", ".xxx.", "xxxxx"}, DOWN = {"xxxxx", ".xxx.", "..x.."};
    /** The words of the fixed lines. */
    static final String SUBTITLE = "Weapons and armour for this round, bought once per troop type", HEAD = "YOUR BANNER'S TROOPS", OFF = "Levels are off", OFF_WHY = "Soldiers do not level and no gear is sold this match.",
        AWAY = "Buy at your barracks or a held point", NOT_HERE = "Not here", NO_GOLD = "Not enough gold", BOUGHT = "Bought", EMPTY = "Your banner has no troop types to buy gear for.";
    static final String[] COLUMNS = {"WEAPON II", "WEAPON III", "ARMOUR II", "ARMOUR III"};
    /** The wire tracks and tiers of the four columns, in order. */
    static final String[] TRACKS = {LevelMath.WEAPON, LevelMath.WEAPON, LevelMath.ARMOUR, LevelMath.ARMOUR};
    static final int[] TIERS = {2, 3, 2, 3};

    record Rect(int x, int y, int w, int h) {
        boolean has(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
        boolean inside(int cw, int ch) { return x >= 0 && y >= 0 && x + w <= cw && y + h <= ch; }
        boolean overlaps(Rect o) { return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h; }
        int cx() { return x + w / 2; }
        int cy() { return y + h / 2; }
    }

    final RecruitmentScreen screen;
    /** The troop ids the widgets were built for, and whether levels were on: a change rebuilds the page. */
    List<String> signature = List.of(); boolean builtOn;
    final List<Gear> buttons = new ArrayList<>();
    final List<Arrow> arrows = new ArrayList<>();
    /** The first visible troop row, how many fit, how many rows the banner has, the row height, the most lines a button holds and whether the scroll gutter is there. */
    int first, shown, total, rowH, lineCap = 2; boolean scrolling;
    /** The content's left edge and width (centred on a canvas wider than MAX_CONTENT), the name block's width, the first button's x and a button's width. */
    int left = MARGIN, contentW, nameW, bx0, bw;
    /** What the page drew in its last frame (the checks read them): the footer's left lines and right hint, the name lines of each visible row, the header's lines. */
    List<String> footerDrawn = List.of(), nameDrawn = List.of(), headerDrawn = List.of(); String hintDrawn = "", message = "";
    int footerX, footerRoom, hintX;
    /** A reason a greyed button gave when it was pressed, shown in the footer for a few seconds, until the server says something newer (noticeSerial moves). */
    String note = ""; long noteUntil; int noteSerial;
    /** The server's notice count when the page opened: a notice newer than that is feedback for a buy. */
    int serialAtOpen = -1;
    final List<Rect> rowBoxes = new ArrayList<>(), nameBoxes = new ArrayList<>(), portraitBoxes = new ArrayList<>();
    Rect list = new Rect(0, 0, 0, 0);

    ArmouryTab(RecruitmentScreen screen) { this.screen = screen; }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }
    static ToIntFunction<String> width() { var f = font(); return s -> f.getWidth(DarkUi.body(s)); }
    static boolean levelsOn() { return Veterans.enabled(); }

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
     * Why a locked button is locked, in the game's own copy (LevelMath.refusal): "Needs a level-2 Knight" until a soldier of the type has reached the tier's level, and for a
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
            case BOUGHT -> "bought. Every soldier of the type whose level allows it wears it.";
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

    /** The height a row needs for this many text lines in a button: the lines, the button's padding and the gaps between rows. */
    static int rowFor(int lines) { return Math.max(34, lines * LINE + 12); }

    /** The page's measures on a canvas: the content's left edge and width, the name block, the first button and a button's width, the row height, rows shown, the scroll gutter and the lines a button holds. */
    record Layout(int left, int contentW, int nameW, int bx0, int bw, int rowH, int shown, boolean scrolling, int lines) {}

    static Layout layout(int W, int H, List<LevelMath.ArmouryRow> rows, ToIntFunction<String> width) {
        int n = rows.size(), contentW = Math.min(W - 2 * MARGIN, MAX_CONTENT), left = (W - contentW) / 2;
        int nameW = Math.max(130, Math.min(220, contentW * 20 / 100)), bx0 = left + PORTRAIT + nameW, avail = H - 45 - TOP;
        boolean scroll = false; int bw = 0, lines = 2, rowMin = 34, capacity = 1;
        for (int pass = 0; pass < 2; pass++) {
            int right = left + contentW - (scroll ? SCROLL : 0);
            bw = (right - 4 - bx0 - 3 * GAP - (GUTTER - GAP)) / 4;
            lines = worstLines(rows, bw - 8 - 12, width); rowMin = rowFor(lines); capacity = Math.max(1, avail / rowMin);
            if (n <= capacity || scroll) break;
            scroll = true;
        }
        int shown = Math.min(Math.max(n, 1), capacity), rowH = Math.min(MAX_ROW, avail / Math.max(1, shown));
        return new Layout(left, contentW, nameW, bx0, bw, Math.max(rowMin, rowH), shown, scroll, lines);
    }

    /** x of a column's button: the two weapon columns, then the two armour columns after a wider gutter. */
    int columnX(int col) { return bx0 + col * (bw + GAP) + (col >= 2 ? GUTTER - GAP : 0); }

    /** Builds the page: the layout, the button of every visible row and the scroll arrows. Called by the screen's init and when the rows change. */
    void init() {
        var rows = CampaignClient.armoury.rows();
        signature = ids(rows); builtOn = levelsOn(); buttons.clear(); arrows.clear(); rowBoxes.clear(); nameBoxes.clear(); portraitBoxes.clear();
        if (serialAtOpen < 0) serialAtOpen = CampaignClient.noticeSerial;
        total = rows.size(); shown = 0; scrolling = false; list = new Rect(MARGIN, TOP, screen.width - 2 * MARGIN, 0);
        if (!builtOn || rows.isEmpty()) return;
        var lay = layout(screen.width, screen.height, rows, width());
        left = lay.left(); contentW = lay.contentW(); nameW = lay.nameW(); bx0 = lay.bx0(); bw = lay.bw(); rowH = lay.rowH(); shown = lay.shown(); scrolling = lay.scrolling(); lineCap = lay.lines();
        first = Math.max(0, Math.min(first, total - shown));
        int right = left + contentW - (scrolling ? SCROLL : 0);
        list = new Rect(left, TOP, right - left, shown * rowH - 3);
        for (int r = 0; r < shown; r++) {
            var row = rows.get(first + r); var t = ArmyCards.spec(row.troop()); if (t == null) continue;
            int y = TOP + r * rowH, cardH = rowH - 3;
            rowBoxes.add(new Rect(left, y, right - left, cardH));
            nameBoxes.add(new Rect(left + PORTRAIT, y, nameW, cardH));
            portraitBoxes.add(new Rect(left, y, PORTRAIT - 4, cardH));
            for (int col = 0; col < 4; col++) buttons.add(screen.add(new Gear(this, t, TRACKS[col], TIERS[col], new Rect(columnX(col), y + 2, bw, cardH - 4))));
        }
        if (scrolling) {
            int ax = left + contentW - ARROW;
            arrows.add(screen.add(new Arrow(this, true, new Rect(ax, TOP, ARROW, ARROW))));
            arrows.add(screen.add(new Arrow(this, false, new Rect(ax, TOP + shown * rowH - 3 - ARROW, ARROW, ARROW))));
        }
    }

    /** One tick: the rows the server sent changed (a new banner) or levels were switched, so the page is built again. */
    void tick() {
        if (!ids(CampaignClient.armoury.rows()).equals(signature) || levelsOn() != builtOn) screen.rebuild();
    }

    /** Scrolls by rows; true when the page scrolls at all (so the wheel is not passed on). */
    boolean scroll(int rows) {
        if (!scrolling) return false;
        int next = Math.max(0, Math.min(first + rows, total - shown));
        if (next != first) { first = next; screen.rebuild(); }
        return true;
    }

    // ---- drawing ----

    void text(DrawContext g, String s, int x, int y, int colour) { g.drawText(font(), DarkUi.body(s), x, y, colour, false); }
    void centre(DrawContext g, String s, int cx, int y, int colour) { text(g, s, cx - width().applyAsInt(s) / 2, y, colour); }

    /** The page, behind its buttons: the header, the column heads, each row's card, name and portrait, the footer. The screen draws the buttons after this. */
    void render(DrawContext g) {
        int W = screen.width; var w = width();
        List<String> head = new ArrayList<>();
        DarkUi.title(g, "ARMOURY", MARGIN, 15, DarkUi.IVORY);
        text(g, SUBTITLE, MARGIN, 40, DarkUi.MUTED); head.add(SUBTITLE);
        String gold = "GOLD  " + GravesAndGloryClient.gold; text(g, gold, W - 190, 19, DarkUi.GOLD); head.add(gold);
        var rows = CampaignClient.armoury.rows();
        List<String> names = new ArrayList<>();
        message = "";
        if (!builtOn) {
            centre(g, OFF, W / 2, TOP + 56, DarkUi.IVORY); centre(g, OFF_WHY, W / 2, TOP + 74, DarkUi.MUTED); message = OFF; head.add(OFF); head.add(OFF_WHY);
        } else if (rows.isEmpty()) {
            message = EMPTY; centre(g, message, W / 2, TOP + 56, DarkUi.IVORY); head.add(message);
        } else {
            text(g, HEAD, left, 70, DarkUi.MUTED); head.add(HEAD);
            for (int col = 0; col < 4; col++) { centre(g, COLUMNS[col], columnX(col) + bw / 2, 70, DarkUi.MUTED); head.add(COLUMNS[col]); }
            if (scrolling) { String count = "TROOPS  " + (first + 1) + "-" + (first + shown) + " OF " + total; text(g, count, W - 190, 39, DarkUi.MUTED); head.add(count); }
            for (int r = 0; r < rowBoxes.size() && first + r < rows.size(); r++) {
                var card = rowBoxes.get(r); var t = ArmyCards.spec(rows.get(first + r).troop());
                g.fill(card.x(), card.y(), card.x() + card.w(), card.y() + card.h(), ROW);
                var nb = nameBoxes.get(r); var lines = UiMath.lines(t.name(), nb.w() - 6, 2, w);
                if (lines.size() == 1) lines = List.of(lines.get(0), kind(t));
                int ty = card.y() + (card.h() - LINE * lines.size()) / 2 + 1;
                for (int i = 0; i < lines.size(); i++) { text(g, lines.get(i), nb.x(), ty + LINE * i, i == 0 ? DarkUi.IVORY : DarkUi.MUTED); names.add(lines.get(i)); }
            }
            // the models last, over the cards: each at the best tiers its army has bought
            var c = MinecraftClient.getInstance();
            for (int r = 0; r < portraitBoxes.size() && first + r < rows.size(); r++) {
                var row = rows.get(first + r); var t = ArmyCards.spec(row.troop()); var pb = portraitBoxes.get(r);
                var e = ArmyCards.portrait(t, bestTier(row, LevelMath.WEAPON), bestTier(row, LevelMath.ARMOUR));
                if (e != null) { e.age = c.player == null ? 0 : c.player.age; ArmyCards.draw(g, pb.x() + pb.w() / 2, pb.y() + pb.h() - 4, ArmyCards.size(e, Math.max(8, (pb.h() - 8) / 2)), 22, 0, e); }
            }
        }
        nameDrawn = names; headerDrawn = head;
        footer(g);
    }

    /** The footer, right of the Back button: a reason a greyed button gave, else a fresh notice of the server's (a buy's outcome), else why nothing can be bought here; the key line at the right. */
    void footer(DrawContext g) {
        int W = screen.width, H = screen.height; var w = width();
        String keys = keyLine(); int hx = W - MARGIN - w.applyAsInt(keys);
        text(g, keys, hx, H - 26, DarkUi.MUTED); hintDrawn = keys; hintX = hx;
        String line; int colour;
        if (!note.isEmpty() && System.currentTimeMillis() < noteUntil && CampaignClient.noticeSerial == noteSerial) { line = note; colour = DarkUi.GOLD; }
        else if (CampaignClient.noticeSerial > serialAtOpen && !CampaignClient.shownNotice().isEmpty()) { line = CampaignClient.shownNotice(); colour = DarkUi.IVORY; }
        else if (builtOn && !CampaignClient.armoury.rows().isEmpty() && !CampaignClient.armoury.here()) { line = AWAY; colour = DarkUi.GOLD; }
        else { line = ""; colour = DarkUi.MUTED; }
        int x = 130, room = hx - 12 - x;
        var lines = line.isEmpty() ? List.<String>of() : UiMath.lines(line, room, 2, w);
        int y = H - 26 - (lines.size() > 1 ? 6 : 0);
        for (int i = 0; i < lines.size(); i++) text(g, lines.get(i), x, y + LINE * i, colour);
        footerDrawn = lines; footerX = x; footerRoom = room;
    }

    // ---- the buttons ----

    /** One of the four buy buttons of a troop row. It reads its state from the server's Armoury each frame and sends only when the server has the buy open and the commander stands where he may buy. */
    static final class Gear extends ButtonWidget {
        final ArmouryTab tab; final Spec.TroopsRow troop; final String track; final int tier; final Rect box;
        LevelMath.Buy state = LevelMath.Buy.LOCKED; boolean here, pressable; List<String> lines = List.of(); String tip = "";
        /** The widest line, the line count and the colours (fill, first line, other lines) as the last frame drew them. */
        int drawnWidest, drawnLines, fillUsed, mainUsed, secondUsed;
        long sentUntil; int sentAt = -1;
        Gear(ArmouryTab tab, Spec.TroopsRow troop, String track, int tier, Rect box) {
            super(box.x(), box.y(), box.w(), box.h(), Text.literal(troop.name() + " " + trackWord(track) + " " + MatchStats.roman(tier)), b -> {}, DEFAULT_NARRATION_SUPPLIER);
            this.tab = tab; this.troop = troop; this.track = track; this.tier = tier; this.box = box; refresh();
        }
        LevelMath.ArmouryRow row() { for (var r : CampaignClient.armoury.rows()) if (r.troop().equals(troop.id())) return r; return null; }
        String command() { return "gear:" + troop.id() + ":" + track + ":" + tier; }
        boolean hasGlyph() { return state == LevelMath.Buy.LOCKED || state == LevelMath.Buy.BOUGHT; }
        /** The width of the box's text area: the box less its padding and the glyph's room (a padlock or a check, 8 px and a gap). */
        int innerW() { return box.w() - 8 - (hasGlyph() ? 12 : 0); }
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
            } else { tab.note = reason(); tab.noteUntil = System.currentTimeMillis() + 4000; tab.noteSerial = CampaignClient.noticeSerial; }
        }
        @Override public void playDownSound(SoundManager sounds) { if (pressable) super.playDownSound(sounds); }
        @Override public void renderButton(DrawContext g, int mx, int my, float delta) {
            refresh();
            int x = getX(), y = getY(), w = width, h = height; boolean focus = isHovered() || isFocused();
            int fill, edge, main, second; boolean lock = false, check = false;
            switch (state) {
                case OPEN -> {
                    if (pressable) { fill = focus ? OPEN_FOCUS : OPEN_FILL; edge = DarkUi.GOLD; main = DarkUi.BRASS_HI; second = DarkUi.IVORY; }
                    else { fill = OFF_FILL; edge = EDGE; main = DarkUi.MUTED; second = DarkUi.MUTED; }
                }
                case UNAFFORDABLE -> { fill = OFF_FILL; edge = EDGE; main = DarkUi.MUTED; second = DarkUi.MUTED; }
                case BOUGHT -> { fill = ROW; edge = EDGE; main = DarkUi.IVORY; second = DarkUi.IVORY; check = true; }
                default -> { fill = LOCKED_FILL; edge = LOCKED_EDGE; main = DarkUi.MUTED; second = DarkUi.MUTED; lock = true; }
            }
            if (focus && edge != DarkUi.GOLD) edge = DarkUi.IVORY; // a greyed button under the pointer or the keyboard is lit in ivory, never in the brass an open buy wears
            g.fill(x, y, x + w, y + h, fill); g.drawBorder(x, y, w, h, edge);
            var fw = width(); int n = lines.size(), ty = y + (h - LINE * n) / 2 + 1, widest = 0;
            if (lock) glyph(g, LOCK, x + 4, y + (h - LOCK.length) / 2, DarkUi.MUTED);
            if (check) glyph(g, CHECK, x + 4, y + (h - CHECK.length) / 2, DarkUi.IVORY);
            for (int i = 0; i < n; i++) {
                String s = lines.get(i); int sw = fw.applyAsInt(s); widest = Math.max(widest, sw);
                int tx = lock || check ? x + 4 + 8 + 4 : x + (w - sw) / 2;
                g.drawText(font(), DarkUi.body(s), tx, ty + LINE * i, i == 0 ? main : second, false);
            }
            drawnWidest = widest; drawnLines = n; fillUsed = fill; mainUsed = main; secondUsed = second;
        }
    }

    static void glyph(DrawContext g, String[] rows, int x, int y, int colour) {
        for (int j = 0; j < rows.length; j++) for (int i = 0; i < rows[j].length(); i++) if (rows[j].charAt(i) == 'x') g.fill(x + i, y + j, x + i + 1, y + j + 1, colour);
    }

    /** The two scroll buttons of the gutter: a triangle each (a shape, not a colour), 24 x 24. */
    static final class Arrow extends ButtonWidget {
        final ArmouryTab tab; final boolean up; final Rect box;
        Arrow(ArmouryTab tab, boolean up, Rect box) {
            super(box.x(), box.y(), box.w(), box.h(), Text.literal(up ? "Earlier troops" : "Later troops"), b -> {}, DEFAULT_NARRATION_SUPPLIER);
            this.tab = tab; this.up = up; this.box = box; setTooltip(Tooltip.of(getMessage()));
        }
        boolean live() { return up ? tab.first > 0 : tab.first < tab.total - tab.shown; }
        @Override public void onPress() { tab.scroll(up ? -1 : 1); }
        @Override public void renderButton(DrawContext g, int mx, int my, float delta) {
            int x = getX(), y = getY(); boolean live = live(), focus = isHovered() || isFocused();
            g.fill(x, y, x + width, y + height, live ? DarkUi.TAB_IDLE : OFF_FILL); g.drawBorder(x, y, width, height, focus ? DarkUi.BRASS_HI : EDGE);
            glyph(g, up ? UP : DOWN, x + (width - 5) / 2, y + (height - 3) / 2, live ? DarkUi.IVORY : DarkUi.MUTED);
        }
    }

    // ---- what the checks read ----

    /** Every way the page fails to fit its canvas, in words (an empty list for none): a button outside the canvas, under 20 x 20, overlapping another, a line wider than its box or cut with an ellipsis, a name cut, the footer's lines meeting the key line. */
    List<String> fitFailures() {
        List<String> bad = new ArrayList<>(); var w = width(); int W = screen.width, H = screen.height;
        var all = new ArrayList<Rect>();
        for (var b : buttons) {
            var r = b.box; all.add(r); String who = b.troop.id() + " " + b.track + " " + b.tier;
            if (!r.inside(W, H - 45)) bad.add(who + " outside the canvas " + W + "x" + H);
            if (r.w() < 20 || r.h() < 20) bad.add(who + " is " + r.w() + "x" + r.h() + ", under 20x20");
            int widest = 0; for (var s : b.lines) widest = Math.max(widest, w.applyAsInt(s));
            if (widest > b.innerW()) bad.add(who + " line of " + widest + " px in " + b.innerW());
            if (b.lines.stream().anyMatch(s -> s.endsWith("..."))) bad.add(who + " has a cut line " + b.lines);
            if (b.lines.size() * LINE > r.h() - 2) bad.add(who + " " + b.lines.size() + " lines in " + r.h() + " px");
        }
        for (var a : arrows) { all.add(a.box); if (!a.box.inside(W, H - 45)) bad.add("a scroll button outside the canvas"); if (a.box.w() < 20 || a.box.h() < 20) bad.add("a scroll button under 20x20"); }
        for (int i = 0; i < all.size(); i++) for (int j = i + 1; j < all.size(); j++) if (all.get(i).overlaps(all.get(j))) bad.add("buttons " + i + " and " + j + " overlap");
        for (int r = 0; r < rowBoxes.size(); r++) {
            if (first + r >= total) break;
            var nb = nameBoxes.get(r);
            var t = ArmyCards.spec(CampaignClient.armoury.rows().get(first + r).troop());
            for (var s : UiMath.lines(t.name(), nb.w() - 6, 2, w)) if (w.applyAsInt(s) > nb.w() - 6 || s.endsWith("...")) bad.add("the name " + s + " is cut in " + (nb.w() - 6));
            if (rowBoxes.get(r).y() + rowBoxes.get(r).h() > H - 45) bad.add("a row reaches into the footer");
        }
        int widest = 0; for (var s : footerDrawn) widest = Math.max(widest, w.applyAsInt(s));
        if (!footerDrawn.isEmpty() && footerX + widest > hintX - 8) bad.add("the footer line meets the key line");
        if (footerDrawn.stream().anyMatch(s -> s.endsWith("..."))) bad.add("the footer is cut " + footerDrawn);
        return bad;
    }
}
