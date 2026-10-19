package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The War Table recruit page (plan Task 6, mock v4-ui-war-table-recruit): the Houses' banners on a rod under the rail, the chosen
 *  House's muster roll on the left, the opened letter in the middle with its troop's live model, and on the right one ladder on
 *  one axis (x 784): the sealed letters, Enlist, the purse, the rung and Odds. The three columns share one top (124) and one
 *  bottom (492). What it shows comes from RecruitView; page coordinates (the 960x540 page) are offset by WarTable.origin(), the
 *  rail's plaques and the banners by its x only, and everything is drawn in canvas pixels. Home's candle and inkwell are left
 *  off this page: the roll and the ladder stand where they would.
 *
 *  Enlist sends roll:<house> for a random troop of the House not yet recruited; the letter's Choose sends unlock:<troop> for the
 *  one shown. When a recruit arrives (CampaignClient's reveal) the page selects it and lays the broken House seal on its letter
 *  until another row is selected; the server plays the reveal's sound. */
final class WarTableRecruit {
    /** The rod under the rail: n Houses hang from x 196 + (5 - n) * 56 at y 12, the rod 112 n + 8 long; banner i hangs at rod x +
     *  8 + 112 i, top 13, 104 wide and 50 tall, the chosen one 60. Its name is centred 52 in, 27 down, trimmed to 96 px. */
    static final int ROD_X = 196, ROD_Y = 12, BANNER_Y = 13, BANNER_STEP = 112, BANNER_W = 104, BANNER_H = 50, BANNER_ON_H = 60, LABEL_Y = 27, LABEL_W = 96;
    /** The cloths light enough for INK_TEXT; the others are dark and take IVORY. A label takes the ink that reads on its cloth,
     *  chosen or not: on the mock's five banners that is INK_TEXT on the chosen gilded and IVORY on the four dark ones, and the
     *  chosen graveborn (Dusk), which the mock never drew, keeps IVORY (INK_TEXT on it measures 2.4:1, IVORY 5.4:1). */
    static final Set<String> LIGHT_CLOTHS = Set.of("gilded");
    /** The muster roll's box, its first row's top, a row's height, the rows shown at once and how wide a name may run. */
    static final int ROLL_X = 56, ROLL_Y = 124, ROLL_W = 240, ROLL_H = 368, ROW_TOP = 140, ROW_H = 56, ROWS = 6, NAME_W = 172;
    /** The letter's box, how wide its lines may run and its Choose tag (box-relative). */
    static final int LETTER_X = 328, LETTER_Y = 124, LETTER_W = 304, LETTER_H = 368, LINE_W = 272, CHOOSE_X = 108, CHOOSE_Y = 316;
    /** The right column's axis and its rungs, top to bottom: the sealed letters, Enlist, the purse, the rung's line and Odds. */
    static final int AXIS = 784, BUNDLE_X = 664, BUNDLE_Y = 124, ENLIST_X = 664, ENLIST_Y = 354, PURSE_Y = 406, RUNG_Y = 442, ODDS_X = 752, ODDS_Y = 468;
    /** The Odds note's corner, how wide its lines may run and how many troop names it lists. */
    static final int NOTE_X = 360, NOTE_Y = 200, NOTE_W = 208, NOTE_NAMES = 6;
    static final String NO_DUPLICATES = "No duplicates: every letter is a new troop.";

    /** A portrait the last frame drew: the troop, where (row-(i) or letter) and what Portrait.draw passed to drawEntity. */
    record PortraitDraw(String troop, String where, Portrait.Drawn drawn) {}
    /** A roll row the last frame drew: its place on the page (0 the top row shown), the troop, the name as drawn, the name's
     *  colour and whether the portrait was the silhouette. */
    record RowDraw(int index, String troop, String name, int color, boolean silhouette) {}

    // What the last frame drew, refilled every frame and read by the checks through the views below.
    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> textsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> texturesDrawn = new LinkedHashMap<>();
    private static final Map<String, Integer> colorsDrawn = new LinkedHashMap<>();
    private static final List<PortraitDraw> portraitsDrawn = new ArrayList<>();
    private static final List<RowDraw> rowsDrawn = new ArrayList<>();
    private static final List<String> sentCommands = new ArrayList<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String, String> texts = Collections.unmodifiableMap(textsDrawn);
    private static final Map<String, String> textures = Collections.unmodifiableMap(texturesDrawn);
    private static final Map<String, Integer> colors = Collections.unmodifiableMap(colorsDrawn);
    private static final List<PortraitDraw> portraits = Collections.unmodifiableList(portraitsDrawn);
    private static final List<RowDraw> rows = Collections.unmodifiableList(rowsDrawn);
    private static final List<String> sent = Collections.unmodifiableList(sentCommands);

    /** Boxes {x,y,w,h} in canvas px: floor, table and rail (as WarTable.drawChrome drew them), rod, banner-(house), roll, mark (the
     *  selected row's), rule-(i) (above row i), row-(i), letter, window (the letter's portrait), broken (the seal), choose, bundle,
     *  enlist, purse, rung, odds and note. */
    static Map<String, int[]> rects() { return rects; }
    /** Words drawn: banner-(house), letter-troop (the troop's id), letter-name, letter-line1, letter-line2, choose-cost, enlist
     *  (the plate's label), enlist-cost ("100", "Need 100", or none when nothing is left), purse, rung, and note-(i) (the Odds
     *  note's lines under its heading). */
    static Map<String, String> texts() { return texts; }
    /** The texture each part was drawn from: table, banner-(house), enlist and choose. */
    static Map<String, String> textures() { return textures; }
    /** The colour each text was drawn in, keyed as texts() is. */
    static Map<String, Integer> colors() { return colors; }
    static List<PortraitDraw> portraits() { return portraits; }
    static List<RowDraw> rows() { return rows; }
    /** Every command the page handed to the server (after CampScreen.action's throttle let it through), oldest first. */
    static List<String> sent() { return sent; }

    final CampScreen screen;
    /** The chosen House's id, the selected troop's id, and the troop whose letter shows the broken seal (null: none). */
    String house, selected, broken;
    /** CampaignClient.revealUntil when the page last took a reveal: a reveal from before the page opened stays untaken. */
    long revealSeen;
    /** Whether the Odds note is open, and the roll's first row shown. */
    boolean odds;
    int scroll;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    UiButton choose, enlist;

    WarTableRecruit(CampScreen screen) {
        this.screen = screen;
        house = RecruitView.houses().get(0).id();
        revealSeen = CampaignClient.revealUntil;
    }

    /** The widget a name stands for (back, banner, house-(id), row-(i), choose, enlist, odds), or null. */
    UiButton button(String name) { return buttons.get(name); }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    static int rodX(int houses) { return ROD_X + (5 - houses) * 56; }

    /** Builds the widgets (CampScreen.init on page recruit): again whenever the camp state changes. The rows, Choose and the
     *  banners' boxes follow the House, the selection and the purse through refresh, so a click needs no rebuild. */
    void init() {
        buttons.clear();
        int[] o = WarTable.origin();
        int ox = o[0], oy = o[1];
        add("back", WarTable.backPlaque(b -> back()));
        add("banner", WarTable.plaque(800, 104, "icon-banner", "Banner", "B", 79, 0, b -> CampScreen.open("army")));
        for (var h : RecruitView.houses())
            add("house-" + h.id(), new UiButton(0, BANNER_Y, BANNER_W, BANNER_H, Text.literal(h.name()), b -> chooseHouse(h.id()), (g, x, y, w, bh, hot) -> paintBanner(g, h, x, y, hot)));
        for (int i = 0; i < ROWS; i++) {
            final int index = i;
            add("row-" + i, new UiButton(ox + ROLL_X, oy + ROW_TOP + ROW_H * i, ROLL_W, ROW_H, Text.literal(""), b -> pick(index), (g, x, y, w, h, hot) -> {}));
        }
        choose = add("choose", new UiButton(ox + LETTER_X + CHOOSE_X, oy + LETTER_Y + CHOOSE_Y, 88, 24, Text.literal("Choose"), b -> chooseTroop(), this::paintChoose));
        enlist = add("enlist", new UiButton(ox + ENLIST_X, oy + ENLIST_Y, 240, 40, Text.literal("Enlist"), b -> enlist(), this::paintEnlist));
        add("odds", new UiButton(ox + ODDS_X, oy + ODDS_Y, 64, 24, Text.literal("Odds"), b -> odds = !odds, this::paintOdds));
        refresh();
    }

    /** Puts the widgets in step with the House, the selection and the purse: the chosen banner hangs longer, a row past the roll
     *  hides, Choose shows under a troop not yet recruited, and Enlist and Choose take presses only while they can send. */
    void refresh() {
        var houses = RecruitView.houses();
        int ox = WarTable.origin()[0], rod = rodX(houses.size());
        for (int i = 0; i < houses.size(); i++) {
            var h = houses.get(i);
            var b = buttons.get("house-" + h.id());
            if (b != null) b.moveTo(ox + rod + 8 + BANNER_STEP * i, BANNER_Y, BANNER_W, h.id().equals(house) ? BANNER_ON_H : BANNER_H);
        }
        var roll = RecruitView.roll(house);
        scroll = Math.max(0, Math.min(scroll, roll.size() - ROWS));
        if (index(roll, selected) < 0) { // nothing selected: the first troop not yet recruited, else the first
            selected = roll.isEmpty() ? null : roll.stream().filter(r -> !r.owned()).findFirst().orElse(roll.get(0)).troopId();
            broken = null;
        }
        for (int i = 0; i < ROWS; i++) {
            var b = buttons.get("row-" + i);
            if (b == null) continue;
            b.visible = scroll + i < roll.size();
            b.setMessage(Text.literal(b.visible ? roll.get(scroll + i).name() : ""));
        }
        var row = row(roll, selected);
        if (choose != null) {
            choose.visible = row != null && !row.owned() && !row.locked();
            choose.active = choose.visible && canChoose();
        }
        if (enlist != null) enlist.active = canEnlist();
    }

    static int index(List<RecruitView.Row> roll, String troop) {
        for (int i = 0; i < roll.size(); i++) if (roll.get(i).troopId().equals(troop)) return i;
        return -1;
    }

    static RecruitView.Row row(List<RecruitView.Row> roll, String troop) {
        int i = index(roll, troop);
        return i < 0 ? null : roll.get(i);
    }

    static boolean battleFree() { return !GravesAndGloryClient.running || CampaignClient.waiting; }

    boolean canEnlist() { return RecruitView.newLeft(house) > 0 && CampaignClient.profile.gold >= RecruitView.cost() && battleFree(); }

    boolean canChoose() { return CampaignClient.profile.gold >= RecruitView.chooseCost() && battleFree(); }

    void back() { screen.page("home"); }

    /** Hands a command to the server through the screen's throttle; recorded when it goes. */
    void send(String cmd) { if (screen.action(cmd)) sentCommands.add(cmd); }

    void enlist() { if (canEnlist()) send("roll:" + house); }

    void chooseTroop() {
        var row = row(RecruitView.roll(house), selected);
        if (row != null && !row.owned() && !row.locked() && canChoose()) send("unlock:" + row.troopId());
    }

    /** A click on a banner: its House's roll, nothing selected (refresh picks), the seal and the scroll gone. */
    void chooseHouse(String id) {
        if (id.equals(house)) return;
        house = id; selected = null; broken = null; scroll = 0;
        refresh();
    }

    /** A click on row i: its troop on the letter; the broken seal stays only on the troop it came with. */
    void pick(int i) {
        var roll = RecruitView.roll(house);
        if (scroll + i < roll.size()) select(roll.get(scroll + i).troopId());
    }

    void select(String troop) {
        selected = troop;
        if (!troop.equals(broken)) broken = null;
        refresh();
    }

    /** The wheel moves the roll by a row. */
    boolean scrolled(double amount) {
        int last = Math.max(0, RecruitView.roll(house).size() - ROWS), next = Math.max(0, Math.min(last, scroll + (amount < 0 ? 1 : -1)));
        if (next != scroll) { scroll = next; refresh(); }
        return true;
    }

    /** A new reveal (CampaignClient.revealUntil moved) naming a troop on a House's roll: that House, that troop, its row in view and
     *  the broken seal on its letter. */
    void tick() {
        if (CampaignClient.revealUntil != revealSeen) {
            revealSeen = CampaignClient.revealUntil;
            for (var h : RecruitView.houses()) {
                var roll = RecruitView.roll(h.id());
                for (int i = 0; i < roll.size(); i++) {
                    if (!roll.get(i).name().equals(CampaignClient.revealUnit)) continue;
                    if (!h.id().equals(house)) { house = h.id(); scroll = 0; }
                    selected = roll.get(i).troopId(); broken = selected;
                    if (i < scroll) scroll = i;
                    else if (i >= scroll + ROWS) scroll = i - ROWS + 1;
                }
            }
        }
        refresh();
    }

    /** Esc closes the Odds note, else goes back to the home; Enter presses Enlist whatever holds the keyboard focus; B opens the
     *  banner builder. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { if (odds) odds = false; else back(); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (enlist != null && enlist.active) { enlist.playDownSound(MinecraftClient.getInstance().getSoundManager()); enlist.onPress(); }
            return true;
        }
        if (key == GLFW.GLFW_KEY_B) { CampScreen.open("army"); return true; }
        return false;
    }

    /** A click anywhere while the Odds note is open closes it, and goes no further. */
    boolean clickCloses() {
        if (!odds) return false;
        odds = false;
        return true;
    }

    /** Hovered (the art under the pointer) or holding the keyboard focus. */
    static boolean hot(UiButton b) {
        if (b == null || !b.visible || !b.active) return false;
        var c = MinecraftClient.getInstance();
        var w = c.getWindow();
        return b.isFocused() || b.overArt(c.mouse.getX() * w.getScaledWidth() / w.getWidth(), c.mouse.getY() * w.getScaledHeight() / w.getHeight());
    }

    /** The troop's live model, as ArmyCards keeps it, breathing on the player's clock; null without a world. */
    static LivingEntity model(String troop) {
        var spec = ArmyCards.spec(troop);
        var c = MinecraftClient.getInstance();
        var e = spec == null ? null : ArmyCards.portrait(spec);
        if (e != null) e.age = c.player == null ? 0 : c.player.age;
        return e;
    }

    static Text heading(String s) { return DarkUi.heading(s); }

    void render(DrawContext g) {
        rectsDrawn.clear(); textsDrawn.clear(); texturesDrawn.clear(); colorsDrawn.clear(); portraitsDrawn.clear(); rowsDrawn.clear();
        UiCanvas.begin(g);
        try {
            int[] o = WarTable.origin();
            int ox = o[0], oy = o[1];
            WarTable.drawChrome(g, "recruit");
            rectsDrawn.putAll(WarTable.drawn()); // floor, table and rail, as drawChrome drew them
            texturesDrawn.put("table", WarTable.drawnTable());
            rod(g, ox);
            var roll = RecruitView.roll(house);
            roll(g, roll, ox + ROLL_X, oy + ROLL_Y, oy);
            letter(g, row(roll, selected), ox + LETTER_X, oy + LETTER_Y);
            ladder(g, ox, oy);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** What lies over the widgets: the Odds note. CampScreen calls it after they have drawn. */
    void overlay(DrawContext g) {
        if (!odds) return;
        UiCanvas.begin(g);
        try {
            int[] o = WarTable.origin();
            note(g, o[0] + NOTE_X, o[1] + NOTE_Y);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** The brass rod the banners hang from (the banners are widgets, drawn after it). */
    void rod(DrawContext g, int ox) {
        int n = RecruitView.houses().size(), x = ox + rodX(n), length = BANNER_STEP * n + 8;
        UiTex.three(g, "recruit/banner-rod", x, ROD_Y, length);
        rectsDrawn.put("rod", new int[]{x, ROD_Y, length, UiTex.entry("recruit/banner-rod").h()});
    }

    /** A House's banner: its cloth, longer and brighter when chosen, and its name in the ink that reads on that cloth. */
    void paintBanner(DrawContext g, RecruitView.House h, int x, int y, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        boolean on = h.id().equals(house);
        String cloth = "recruit/banner-" + h.cloth() + (on ? "-on" : ""); // exercised by the suite: build_ui_art --verify sees only whole names
        UiTex.draw(g, cloth, x, y);
        String name = UiMath.trim(h.name(), LABEL_W, s -> font.getWidth(heading(s)));
        int color = LIGHT_CLOTHS.contains(h.cloth()) ? DarkUi.INK_TEXT : DarkUi.IVORY;
        int lx = WarTableHome.centred(g, heading(name), x + 52, y + LABEL_Y, color);
        if (hot) g.fill(lx, y + LABEL_Y + 12, lx + font.getWidth(heading(name)), y + LABEL_Y + 13, color);
        String key = "banner-" + h.id();
        rectsDrawn.put(key, new int[]{x, y, BANNER_W, on ? BANNER_ON_H : BANNER_H});
        texturesDrawn.put(key, cloth);
        textsDrawn.put(key, name);
        colorsDrawn.put(key, color);
    }

    /** The muster roll: the paper, the selected row's mark, the rules between rows (none at the selected row's top, where the mark's
     *  edge is; the mock keeps the one under it), then each row shown. */
    void roll(DrawContext g, List<RecruitView.Row> roll, int x, int y, int oy) {
        UiTex.draw(g, "recruit/muster-roll", x, y);
        rectsDrawn.put("roll", new int[]{x, y, ROLL_W, ROLL_H});
        int shown = Math.min(ROWS, roll.size() - scroll), sel = index(roll, selected) - scroll;
        if (sel >= 0 && sel < shown) {
            int my = oy + ROW_TOP + ROW_H * sel;
            UiTex.draw(g, "recruit/row-mark", x, my);
            rectsDrawn.put("mark", new int[]{x, my, ROLL_W, ROW_H});
        }
        for (int i = 1; i < shown; i++) {
            if (i == sel) continue;
            int ry = oy + ROW_TOP + ROW_H * i;
            UiTex.draw(g, "recruit/rule-208", x + 16, ry);
            rectsDrawn.put("rule-" + i, new int[]{x + 16, ry, 208, 1});
        }
        for (int i = 0; i < shown; i++) rowAt(g, roll.get(scroll + i), i, x, oy + ROW_TOP + ROW_H * i, hot(buttons.get("row-" + i)) && i != sel);
    }

    /** Row i: the portrait in its frame (the silhouette until the troop is recruited), the name (ink when recruited, sepia when
     *  not), and the stars or, on a locked troop, the padlock with the name lower. */
    void rowAt(DrawContext g, RecruitView.Row r, int i, int x, int y, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        UiTex.draw(g, "table/frame-back-48", x + 8, y + 8);
        var e = model(r.troopId());
        boolean silhouette = !r.owned();
        if (e != null) {
            Portrait.draw(g, e, x + 8, y + 8, 40, 0, silhouette);
            portraitsDrawn.add(new PortraitDraw(r.troopId(), "row-" + i, Portrait.last())); // what reached drawEntity
        }
        UiTex.draw(g, "table/frame-rim-48", x + 4, y + 4);
        int color = r.owned() ? DarkUi.INK_TEXT : DarkUi.SEPIA, ny = y + (r.locked() ? 20 : 12);
        String name = UiMath.trim(r.name(), NAME_W, s -> font.getWidth(heading(s)));
        g.drawText(font, heading(name), x + 60, ny, color, false);
        if (hot) g.fill(x + 60, ny + 12, x + 60 + font.getWidth(heading(name)), ny + 13, color);
        if (r.locked()) UiTex.draw(g, "recruit/padlock-brass", x + 23, y + 22);
        else if (r.stars() > 0)
            for (int s = 0; s < 3; s++) {
                if (s < r.stars()) UiTex.draw(g, "recruit/star-s", x + 60 + 12 * s, y + 30);
                else UiTex.draw(g, "recruit/star-s-empty", x + 60 + 12 * s, y + 30);
            }
        rectsDrawn.put("row-" + i, new int[]{x, y, ROLL_W, ROW_H});
        rowsDrawn.add(new RowDraw(i, r.troopId(), name, color, silhouette));
    }

    /** The opened letter: the selected troop's live model in the ornate frame, its name, what it is, its stars (Phase 2), what it
     *  costs to deploy, and the broken House seal while it is the troop just recruited. Choose is a widget (paintChoose). */
    void letter(DrawContext g, RecruitView.Row r, int x, int y) {
        var font = MinecraftClient.getInstance().textRenderer;
        UiTex.draw(g, "recruit/letter-hi", x, y);
        rectsDrawn.put("letter", new int[]{x, y, LETTER_W, LETTER_H});
        if (r == null) return;
        UiTex.draw(g, "table/frame-back-160", x + 72, y + 40);
        var e = model(r.troopId());
        if (e != null) {
            Portrait.draw(g, e, x + 72, y + 40, 160, 0); // Phase 2: tall 2 for the troops it marks Tall
            portraitsDrawn.add(new PortraitDraw(r.troopId(), "letter", Portrait.last()));
        }
        rectsDrawn.put("window", new int[]{x + 72, y + 40, 160, 160});
        UiTex.draw(g, "table/frame-ornate-184", x + 60, y + 28);
        String name = UiMath.trim(r.name(), LINE_W, s -> font.getWidth(DarkUi.focal(s)));
        WarTableHome.centred(g, DarkUi.focal(name), x + 152, y + 224, DarkUi.INK_TEXT);
        String kind = r.rarity().isEmpty() ? r.group() + (r.owned() ? ", recruited" : ", not yet recruited") : r.rarity() + " " + r.group().toLowerCase(Locale.ROOT);
        kind = UiMath.trim(kind, LINE_W, s -> font.getWidth(DarkUi.body(s)));
        WarTableHome.centred(g, DarkUi.body(kind), x + 152, y + 246, DarkUi.SEPIA);
        if (r.stars() > 0) {
            for (int s = 0; s < 3; s++) {
                if (s < r.stars()) UiTex.draw(g, "recruit/star-l", x + 127 + 19 * s, y + 268);
                else UiTex.draw(g, "recruit/star-l-empty", x + 127 + 19 * s, y + 268);
            }
            UiTex.draw(g, "recruit/sparkle", x + 127 + 19 * (r.stars() - 1) - 2, y + 266); // on the newest star
        }
        var spec = ArmyCards.spec(r.troopId());
        String deploy = spec == null ? "" : "Deploys in battle for " + spec.cost() + " gold"; // Phase 2: the star bonus
        WarTableHome.centred(g, DarkUi.body(deploy), x + 152, y + 292, DarkUi.INK_TEXT);
        if (r.owned() && r.stars() > 0) UiTex.draw(g, "recruit/wax-star", x + 136, y + 314);
        if (r.troopId().equals(broken)) {
            UiTex.draw(g, "recruit/wax-broken", x + 135, y - 16);
            rectsDrawn.put("broken", new int[]{x + 135, y - 16, 35, 36});
        }
        textsDrawn.put("letter-troop", r.troopId());
        textsDrawn.put("letter-name", name);
        textsDrawn.put("letter-line1", kind);
        textsDrawn.put("letter-line2", deploy);
        colorsDrawn.put("letter-line1", DarkUi.SEPIA);
        colorsDrawn.put("letter-line2", DarkUi.INK_TEXT);
    }

    /** Choose: a vellum tag with the coin, the cost and the word; sepia while the purse is short. */
    void paintChoose(DrawContext g, int x, int y, int w, int h, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        boolean can = canChoose();
        int ink = can ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        String cost = String.valueOf(RecruitView.chooseCost());
        var label = heading("Choose");
        UiTex.draw(g, "table/btn-vellum-88x24", x, y);
        UiTex.draw(g, "recruit/coin-m", x + 8, y + 8);
        g.drawText(font, cost, x + 18, y + 8, ink, false);
        g.drawText(font, label, x + 40, y + 6, ink, false);
        if (hot) g.fill(x + 40, y + 18, x + 40 + font.getWidth(label), y + 19, ink);
        rectsDrawn.put("choose", new int[]{x, y, w, h});
        texturesDrawn.put("choose", "table/btn-vellum-88x24");
        textsDrawn.put("choose-cost", cost);
        colorsDrawn.put("choose-cost", ink);
    }

    /** The ladder: the sealed letters while any are left, the purse centred on the axis, the rung's line (Enlist and Odds are
     *  widgets: paintEnlist, paintOdds). */
    void ladder(DrawContext g, int ox, int oy) {
        var font = MinecraftClient.getInstance().textRenderer;
        int left = RecruitView.newLeft(house), pity = RecruitView.pityLeft(house);
        if (left > 0) {
            UiTex.draw(g, "recruit/letter-bundle", ox + BUNDLE_X, oy + BUNDLE_Y);
            rectsDrawn.put("bundle", new int[]{ox + BUNDLE_X, oy + BUNDLE_Y, 240, 218});
        }
        String gold = String.valueOf(CampaignClient.profile.gold);
        int pw = 34 + font.getWidth(gold), px = ox + AXIS - (pw + 1) / 2, py = oy + PURSE_Y; // the pouch and the sum, centred on the axis
        UiTex.draw(g, "recruit/pouch", px, py);
        g.drawText(font, gold, px + 34, py + 10, DarkUi.IVORY, true);
        rectsDrawn.put("purse", new int[]{px, py, pw, 26});
        textsDrawn.put("purse", gold);
        String rung = pity >= 0 ? UiMath.pityLine(pity) : UiMath.lettersLine(left);
        int rx = WarTableHome.centred(g, DarkUi.body(rung), ox + AXIS, oy + RUNG_Y, DarkUi.MUTED);
        rectsDrawn.put("rung", new int[]{rx, oy + RUNG_Y, font.getWidth(DarkUi.body(rung)), 11});
        textsDrawn.put("rung", rung);
        colorsDrawn.put("rung", DarkUi.MUTED);
    }

    /** Enlist: brass with the Enter key, the word and the cost; iron with "Need" and the cost in alarm while the purse is short;
     *  iron with "Every troop found" once the House has none left to draw. */
    void paintEnlist(DrawContext g, int x, int y, int w, int h, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        int left = RecruitView.newLeft(house), cost = RecruitView.cost();
        String label, plate;
        if (left <= 0) {
            plate = "table/btn-iron-240x40";
            label = "Every troop found";
            UiTex.draw(g, "table/btn-iron-240x40", x, y);
            WarTableHome.centred(g, heading(label), x + 120, y + 14, DarkUi.IVORY);
            colorsDrawn.put("enlist", DarkUi.IVORY);
        } else if (CampaignClient.profile.gold < cost) {
            plate = "table/btn-iron-240x40";
            label = "Enlist";
            UiTex.draw(g, "table/btn-iron-240x40", x, y);
            WarTableHome.centred(g, heading(label), x + 120, y + 14, DarkUi.IVORY);
            String need = "Need " + cost;
            g.drawText(font, need, x + 224 - font.getWidth(need), y + 16, DarkUi.ALARM_TEXT, false);
            textsDrawn.put("enlist-cost", need);
            colorsDrawn.put("enlist-cost", DarkUi.ALARM_TEXT);
            colorsDrawn.put("enlist", DarkUi.IVORY);
        } else {
            plate = "table/btn-brass-240x40";
            label = "Enlist";
            UiTex.draw(g, "table/btn-brass-240x40", x, y);
            UiTex.draw(g, "table/key-brass-37", x + 14, y + 13);
            g.drawText(font, "Enter", x + 18, y + 16, DarkUi.KEY_INK, false);
            int lx = WarTableHome.centred(g, heading(label), x + 120, y + 14, DarkUi.PLATE_INK);
            if (hot) g.fill(lx, y + 26, lx + font.getWidth(heading(label)), y + 27, DarkUi.PLATE_INK);
            String price = String.valueOf(cost);
            int cx = x + 224 - font.getWidth(price);
            g.drawText(font, price, cx, y + 16, DarkUi.PLATE_INK, false);
            UiTex.draw(g, "recruit/coin-m", cx - 10, y + 16); // 3 px before the cost
            textsDrawn.put("enlist-cost", price);
            colorsDrawn.put("enlist-cost", DarkUi.PLATE_INK);
            colorsDrawn.put("enlist", DarkUi.PLATE_INK);
        }
        rectsDrawn.put("enlist", new int[]{x, y, w, h});
        texturesDrawn.put("enlist", plate);
        textsDrawn.put("enlist", label);
    }

    /** Odds: a vellum tag with the die and the word; it opens and closes the note. */
    void paintOdds(DrawContext g, int x, int y, int w, int h, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        var label = heading("Odds");
        UiTex.draw(g, "table/btn-vellum-64x24", x, y);
        UiTex.draw(g, "recruit/die", x + 12, y + 7);
        g.drawText(font, label, x + 26, y + 6, DarkUi.INK_TEXT, false);
        if (hot) g.fill(x + 26, y + 18, x + 26 + font.getWidth(label), y + 19, DarkUi.INK_TEXT);
        rectsDrawn.put("odds", new int[]{x, y, w, h});
    }

    /** The Odds note, pinned over the letter: the chance of each troop still to be drawn, the no-duplicates rule, and their names. */
    void note(DrawContext g, int x, int y) {
        var font = MinecraftClient.getInstance().textRenderer;
        UiTex.draw(g, "recruit/odds-note", x, y);
        rectsDrawn.put("note", new int[]{x, y, 240, 160});
        g.drawText(font, heading("Odds"), x + 16, y + 12, DarkUi.INK_TEXT, false);
        List<String> lines = new ArrayList<>(List.of(UiMath.oddsLine(RecruitView.newLeft(house)), NO_DUPLICATES));
        for (var r : RecruitView.roll(house)) if (!r.owned() && !r.locked() && lines.size() < 2 + NOTE_NAMES) lines.add(r.name());
        for (int i = 0; i < lines.size(); i++) {
            String line = UiMath.trim(lines.get(i), NOTE_W, s -> font.getWidth(DarkUi.body(s)));
            g.drawText(font, DarkUi.body(line), x + 16, y + 36 + 14 * i, DarkUi.INK_TEXT, false);
            textsDrawn.put("note-" + i, line);
        }
    }
}
