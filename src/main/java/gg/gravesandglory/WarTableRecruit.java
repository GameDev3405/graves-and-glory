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
 *  Enlist sends roll:<house> for a letter of the House (a troop it holds already gives a copy toward its next star). The letter's
 *  action tag is RecruitPages.action for the troop shown, so the page and the old camp screen never disagree on a rule: Buy
 *  (buy:<troop>, a Common or a Rare, owned or not), Star (star:<troop>, an owned Epic or Legendary), Pact or shards (legendary:<house>:
 *  pact|shards, a Legendary not yet found); an Epic not yet found shows the words "From letters only". The tag is still the widget
 *  "choose". When a recruit arrives (CampaignClient's reveal) the page selects it and lays the broken House seal on its letter
 *  until another row is selected; a duplicate gets no seal: the letter lights the star it gained or the copy it counted and
 *  says so in a line (RecruitPages.outcome). The server plays the reveal's sound. A notice the server sends (a refusal, a save
 *  that failed) shows on the ladder's axis under its foot, as home shows one under Ride. */
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
    /** The letter's box, how wide its lines may run and its action tag (box-relative: the tag is CHOOSE_W wide, centred on the
     *  letter, with what it does, or why it cannot be pressed, in up to REASON_LINES lines REASON_STEP apart under it; the wax
     *  seal of a recruited troop stands at WAX_X, WAX_Y). */
    static final int LETTER_X = 328, LETTER_Y = 124, LETTER_W = 304, LETTER_H = 368, LINE_W = 272, CHOOSE_X = 92, CHOOSE_Y = 312, CHOOSE_W = 120, CHOOSE_H = 24;
    static final int REASON_Y = 340, REASON_STEP = 12, REASON_LINES = 2, WAX_X = 36, WAX_Y = 304;
    /** How long a press of the tag holds the next one (client ticks) while the camp has not yet answered it. */
    static final int PENDING_TICKS = 30;
    /** The right column's axis and its rungs, top to bottom: the sealed letters, Enlist, the purse, the rung's line and Odds. */
    static final int AXIS = 784, BUNDLE_X = 664, BUNDLE_Y = 124, ENLIST_X = 664, ENLIST_Y = 354, PURSE_Y = 406, RUNG_Y = 442, ODDS_X = 752, ODDS_Y = 468;
    /** The Odds note's corner, how wide its lines may run and how many lines it may take under its heading. */
    static final int NOTE_X = 360, NOTE_Y = 200, NOTE_W = 208, NOTE_LINES = 9;
    /** The server's notice: its first line's top, 12 px under the ladder's foot (492) as home's is 12 px under Ride (the purse
     *  takes the 12 px under the Enlist plate); its lines, centred on the axis, 13 px apart; how wide a line may run (to 12 px from
     *  the page's right edge) and how many lines it may take (the second ends 12 px above the page's foot). */
    static final int NOTICE_Y = 504, NOTICE_STEP = 13, NOTICE_W = 328, NOTICE_LINES = 2;
    /** The compact page (UI Task 8b-2, WarTable.compact): the three columns close up and rise 32 px (roll box at 27, letter at 286,
     *  ladder at 600 on the 854 page, their inks 10 px apart), the rung's line and Odds share one row either side of the axis
     *  under the purse, and the notice comes up under them, 240 px wide; the Banner plaque stands 12 px from the page's right
     *  edge. Each column's offset from its 960 place: */
    static final int[] COMPACT_ROLL = {-29, -32}, COMPACT_LETTER = {-42, -32}, COMPACT_LADDER = {-64, -32};
    static final int COMPACT_ODDS_Y = 440, COMPACT_NOTICE_Y = 472, COMPACT_NOTICE_W = 240, COMPACT_BANNER_X = 738;

    static int[] rollAt() { return WarTable.column(COMPACT_ROLL[0], COMPACT_ROLL[1]); }
    static int[] letterAt() { return WarTable.column(COMPACT_LETTER[0], COMPACT_LETTER[1]); }
    static int[] ladderAt() { return WarTable.column(COMPACT_LADDER[0], COMPACT_LADDER[1]); }

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
    private static final Set<String> underlinedDrawn = new LinkedHashSet<>();
    private static final List<String> sentCommands = new ArrayList<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String, String> texts = Collections.unmodifiableMap(textsDrawn);
    private static final Map<String, String> textures = Collections.unmodifiableMap(texturesDrawn);
    private static final Map<String, Integer> colors = Collections.unmodifiableMap(colorsDrawn);
    private static final List<PortraitDraw> portraits = Collections.unmodifiableList(portraitsDrawn);
    private static final List<RowDraw> rows = Collections.unmodifiableList(rowsDrawn);
    private static final Set<String> underlined = Collections.unmodifiableSet(underlinedDrawn);
    private static final List<String> sent = Collections.unmodifiableList(sentCommands);

    /** Rects {x,y,w,h} in canvas px, as drawn. The art each texture covered, as UiTex.draw and three report it (the box's corner
     *  less the atlas entry's offsets, and the PNG's size or the length drawn): rod, roll (the muster roll), mark (the selected
     *  row's), rule-(i) (above row i), letter, broken (the seal), bundle and note (the Odds note). The boxes the widgets handed
     *  their painters: banner-(house), choose, enlist and odds; row-(i) as rowAt drew it. The portrait's window as Portrait.draw
     *  scissored it: window. The words' extents: the banners' labels label-(house), the purse (pouch and sum), rung and notice
     *  (all its lines). Also table and rail as WarTable.drawChrome drew them. */
    static Map<String, int[]> rects() { return rects; }
    /** Words drawn: banner-(house), letter-troop (the troop's id), letter-name, letter-line1, letter-line2 (the deploy line, absent while
     *  a duplicate's outcome takes its place), letter-outcome, letter-stars (the stars lit, "1+" while one is growing) and letter-fill (its
     *  percent) while a duplicate is lit, letter-none ("From letters only", "Full stars") where the troop has no tag, choose (the tag's
     *  label), choose-command (what it sends) and reason (the words under it), enlist (the plate's label), enlist-cost ("100" or "Need
     *  100"), purse, rung, note-(i) (the Odds note's lines under its heading) and notice (its lines joined by a space). */
    static Map<String, String> texts() { return texts; }
    /** The texture each part was drawn from: table, banner-(house), enlist and choose. */
    static Map<String, String> textures() { return textures; }
    /** The colour each text was drawn in, keyed as texts() is (choose for the word Choose). */
    static Map<String, Integer> colors() { return colors; }
    static List<PortraitDraw> portraits() { return portraits; }
    static List<RowDraw> rows() { return rows; }
    /** The widgets the last frame drew with their label underlined (hot: under the pointer, or holding the keyboard's focus). */
    static Set<String> underlined() { return underlined; }
    /** The last WarTablePages.SENT_KEPT commands the page handed to the server (after CampScreen.action's throttle let it
     *  through), oldest first. */
    static List<String> sent() { return sent; }

    final CampScreen screen;
    /** The chosen House's id, the selected troop's id, and the troop whose letter shows the broken seal (null: none). */
    String house, selected, broken;
    /** The duplicate roll the letter is lighting, and the troop it came for (null: none): until the reveal ends or another troop is selected. */
    CampaignClient.LastRoll lit;
    String litTroop;
    /** The camp state when the action tag was last pressed, and the tick: the tag takes no second press until the camp's state moves or PENDING_TICKS pass. */
    String pendingState = "";
    long pendingAt = -1000;
    /** CampaignClient.revealUntil when the page last took a reveal: a reveal from before the page opened stays untaken. */
    long revealSeen;
    /** Whether the Odds note is open, the roll's first row shown, and the wheel's turn carried toward the next whole row. */
    boolean odds;
    int scroll;
    double wheel;
    /** The page's clock (client ticks), and the notice line: the server's notice serial last taken, its text and the tick it
     *  appeared. A notice shows when the serial moves, so one already waiting when the page opened stays hidden. */
    long ticks;
    int noticeSerialSeen;
    String noticeText = "";
    long noticeAt = -1000;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    UiButton choose, enlist;

    WarTableRecruit(CampScreen screen) {
        this.screen = screen;
        house = RecruitView.houses().stream().anyMatch(h -> h.id().equals(screen.stationHouse)) ? screen.stationHouse : RecruitView.houses().get(0).id(); // a House ring tent (recruit:<house>) opens on its House
        revealSeen = CampaignClient.revealUntil;
        noticeSerialSeen = CampaignClient.noticeSerial;
        var back = ArmyCards.spec(screen.troop); // back from the troop sheet (WarTableTroop.back): its House, its troop selected; read once
        screen.troop = "";
        if (back != null && RecruitView.houses().stream().anyMatch(h -> h.id().equals(back.house()))) { house = back.house(); selected = back.id(); }
    }

    /** The widget a name stands for (back, banner, house-(id), row-(i), choose, sheet, enlist, odds), or null. */
    UiButton button(String name) { return buttons.get(name); }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    static int rodX(int houses) { return ROD_X + (5 - houses) * 56; }

    /** Builds the widgets (CampScreen.init on page recruit): again whenever the camp state changes. The rows, Choose and the
     *  banners' boxes follow the House, the selection and the purse through refresh, so a click needs no rebuild. */
    void init() {
        buttons.clear();
        int[] ro = rollAt(), lo = letterAt(), la = ladderAt();
        boolean compact = WarTable.compact();
        add("back", WarTable.backPlaque(b -> back()));
        int bannerX = compact ? WarTable.compactOrigin()[0] + COMPACT_BANNER_X : WarTable.railX(800);
        // the plaque names the player's own Banner key (Controls), trimmed to the room by the name; an unbound key shows no cap
        String bannerKey = GravesAndGloryClient.hintShort("banner");
        bannerKey = UiMath.trimKey(bannerKey, 17, s -> MinecraftClient.getInstance().textRenderer.getWidth(s));
        boolean capped = !bannerKey.isEmpty() && !HintText.UNBOUND_SHORT.equals(bannerKey);
        int bannerCap = capped ? UiMath.brassKeyWidth(MinecraftClient.getInstance().textRenderer.getWidth(bannerKey), bannerKey.length()) : 0;
        add("banner", WarTable.plaqueAt(bannerX, 104, "icon-banner", "Banner", capped ? bannerKey : null, 92 - bannerCap, 0, b -> screen.page("banner")));
        for (var h : RecruitView.houses())
            add("house-" + h.id(), new UiButton(0, BANNER_Y, BANNER_W, BANNER_H, Text.literal(h.name()), b -> chooseHouse(h.id()), (g, x, y, w, bh, hot) -> paintBanner(g, h, x, y, w, bh)));
        for (int i = 0; i < ROWS; i++) {
            final int index = i;
            add("row-" + i, new UiButton(ro[0] + ROLL_X, ro[1] + ROW_TOP + ROW_H * i, ROLL_W, ROW_H, Text.literal(""), b -> pick(index), (g, x, y, w, h, hot) -> {}));
        }
        choose = add("choose", new UiButton(lo[0] + LETTER_X + CHOOSE_X, lo[1] + LETTER_Y + CHOOSE_Y, CHOOSE_W, CHOOSE_H, Text.literal("Buy"), b -> chooseTroop(), this::paintChoose));
        add("sheet", new UiButton(lo[0] + LETTER_X + 152 - LINE_W / 2, lo[1] + LETTER_Y + WarTableTroop.NAME_Y, LINE_W, WarTableTroop.NAME_H, Text.literal("Troop sheet"), b -> openSheet(), this::paintSheet));
        enlist = add("enlist", new UiButton(la[0] + ENLIST_X, la[1] + ENLIST_Y, 240, 40, Text.literal("Enlist"), b -> enlist(), this::paintEnlist));
        add("odds", new UiButton(compact ? la[0] + AXIS + 4 : la[0] + ODDS_X, la[1] + (compact ? COMPACT_ODDS_Y : ODDS_Y), 64, 24, Text.literal("Odds"), b -> odds = !odds, this::paintOdds));
        refresh();
    }

    /** Puts the widgets in step with the House, the selection and the purse: the chosen banner hangs longer, a row past the roll
     *  hides, the action tag shows under a troop that has an action (RecruitPages.action), and Enlist and the tag take presses
     *  only while they can send. */
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
            var a = row == null ? null : RecruitView.action(row.troopId());
            choose.visible = a != null;
            choose.setMessage(Text.literal(a == null ? "" : a.label()));
            choose.active = a != null && a.active() && battleFree() && !pending();
        }
        var sheet = buttons.get("sheet");
        if (sheet != null) {
            sheet.visible = row != null;
            if (row != null) {
                var font = MinecraftClient.getInstance().textRenderer;
                int[] lo = letterAt(), w = {font.getWidth(DarkUi.focal(UiMath.trim(row.name(), LINE_W, s -> font.getWidth(DarkUi.focal(s))))) + 8};
                sheet.moveTo(lo[0] + LETTER_X + 152 - w[0] / 2, lo[1] + LETTER_Y + WarTableTroop.NAME_Y, w[0], WarTableTroop.NAME_H);
            }
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

    boolean canEnlist() { return CampaignClient.profile.gold >= RecruitView.cost() && battleFree(); }

    /** Whether a press of the tag is still on its way: the camp has not answered it (its state is as at the press) and PENDING_TICKS have not passed. */
    boolean pending() { return !pendingState.isEmpty() && ticks - pendingAt < PENDING_TICKS && pendingState.equals(screen.state()); }

    /** The words under the tag: what its action does, or why it cannot be pressed (RecruitPages.Action.tip), or, while a battle is on or a press is on its way, that. */
    String reason(RecruitPages.Action a) {
        if (!battleFree()) return "A battle is under way.";
        if (pending()) return "Sent. Waiting for the camp.";
        return a.tip();
    }

    void back() { UiSounds.back(); screen.page("home"); }

    /** Hands a command to the server through the screen's throttle; recorded when it goes. Returns whether it went. */
    boolean send(String cmd) {
        if (!screen.action(cmd)) return false;
        if (sentCommands.size() >= WarTablePages.SENT_KEPT) sentCommands.remove(0);
        sentCommands.add(cmd);
        return true;
    }

    void enlist() { if (canEnlist()) send("roll:" + house); }

    /** The action tag: sends what RecruitPages.action gives for the troop shown (buy:, star: or legendary:), once; the page never sends unlock: or army:. A second press waits for the camp's answer (pending). */
    void chooseTroop() {
        var row = row(RecruitView.roll(house), selected);
        var a = row == null ? null : RecruitView.action(row.troopId());
        if (a == null || !a.active() || !battleFree() || pending()) return;
        if (send(a.command())) { pendingState = screen.state(); pendingAt = ticks; refresh(); }
    }

    /** A click on the letter's name: the troop sheet of the troop shown (CampScreen page troop); it takes the reveal's end with it. */
    void openSheet() {
        if (selected != null) screen.openTroop(selected);
    }

    /** A click on a banner: its House's roll, nothing selected (refresh picks), the seal, the scroll and the wheel's carry gone. */
    void chooseHouse(String id) {
        if (id.equals(house)) return;
        house = id; selected = null; broken = null; lit = null; litTroop = null; scroll = 0; wheel = 0;
        refresh();
    }

    /** A click on row i: its troop on the letter; the broken seal and the lit star stay only on the troop they came with. */
    void pick(int i) {
        var roll = RecruitView.roll(house);
        if (scroll + i < roll.size()) select(roll.get(scroll + i).troopId());
    }

    void select(String troop) {
        selected = troop;
        if (!troop.equals(broken)) broken = null;
        if (!troop.equals(litTroop)) { lit = null; litTroop = null; }
        refresh();
    }

    /** The wheel over the page moves the roll a row for each whole unit of turn (UiMath.wheel): a turn of 0 (a sideways-only
     *  scroll) moves nothing, a touchpad's small turns add up to a row, and nothing carries past either end. */
    boolean scrolled(double amount) {
        var w = UiMath.wheel(scroll, Math.max(0, RecruitView.roll(house).size() - ROWS), wheel, amount);
        wheel = w.carry();
        if (w.first() != scroll) { scroll = w.first(); refresh(); }
        return true;
    }

    /** A new reveal (CampaignClient.revealUntil moved) naming a troop on a House's roll: that House, that troop, its row in view and
     *  the broken seal on its letter, or, when the roll that brought it was a duplicate (CampaignClient.lastRoll), no seal: the
     *  letter lights the star or the copy instead. The troop is found by its name, so a duplicate finds the troop it raised and
     *  never lays a seal on it again. A new notice (its serial moved) shows on the notice line, unless it came with the reveal:
     *  that one is the recruit's own ("Joined your collection: ..."), which the letter already says. */
    void tick() {
        ticks++;
        boolean revealed = false;
        if (CampaignClient.revealUntil != revealSeen) {
            revealSeen = CampaignClient.revealUntil;
            for (var h : RecruitView.houses()) {
                var roll = RecruitView.roll(h.id());
                for (int i = 0; i < roll.size(); i++) {
                    if (!roll.get(i).name().equals(CampaignClient.revealUnit)) continue;
                    if (!h.id().equals(house)) { house = h.id(); scroll = 0; wheel = 0; }
                    selected = roll.get(i).troopId(); revealed = true;
                    var last = CampaignClient.lastRoll;
                    boolean duplicate = CampaignClient.rollShowing() && last != null && last.duplicate() && last.troop().equals(selected);
                    broken = duplicate ? null : selected;
                    lit = duplicate ? last : null;
                    litTroop = duplicate ? selected : null;
                    if (i < scroll) scroll = i;
                    else if (i >= scroll + ROWS) scroll = i - ROWS + 1;
                }
            }
        }
        if (CampaignClient.noticeSerial != noticeSerialSeen) {
            noticeSerialSeen = CampaignClient.noticeSerial;
            noticeText = revealed ? "" : CampaignClient.notice;
            if (!noticeText.isEmpty()) noticeAt = ticks;
        }
        refresh();
    }

    /** A key's press; CampScreen hands over only the press (a held key's repeats go no further). Esc closes the Odds note, else
     *  goes home. Enter or Space presses the widget the keyboard's focus is on (Tab or the arrows put it there); with none, Enter
     *  is Enlist and Space presses nothing (a click leaves its widget focused, and Space must not press it again). B opens the
     *  Banner, as the plaque does. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { if (odds) odds = false; else back(); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            var target = keyboardFocus();
            if (target == null && key != GLFW.GLFW_KEY_SPACE) target = enlist;
            if (target != null && target.visible && target.active) press(target);
            return true;
        }
        if (GravesAndGloryClient.banner != null && GravesAndGloryClient.banner.matchesKey(key, -1)) { screen.page("banner"); return true; }
        return false;
    }

    static void press(UiButton b) {
        b.playDownSound(MinecraftClient.getInstance().getSoundManager());
        b.onPress();
    }

    /** The page's widget the keyboard's focus is on, or null: Tab and the arrows put the focus on a widget (the client's navigation
     *  is then the keyboard's); a click leaves the focus on what it clicked and sets the navigation to the mouse's, so it does not
     *  count. */
    UiButton keyboardFocus() {
        if (!MinecraftClient.getInstance().getNavigationType().isKeyboard()) return null;
        for (var b : buttons.values()) if (b.isFocused() && b.visible) return b;
        return null;
    }

    /** A click anywhere while the Odds note is open closes it, and goes no further. */
    boolean clickCloses() {
        if (!odds) return false;
        odds = false;
        return true;
    }

    /** Whether a widget's label is underlined: the pointer over its art, or the keyboard's focus on it (see keyboardFocus), and
     *  never while it cannot be pressed. */
    static boolean hot(UiButton b) {
        if (b == null || !b.visible || !b.active) return false;
        var c = MinecraftClient.getInstance();
        var w = c.getWindow();
        return (b.isFocused() && c.getNavigationType().isKeyboard()) || b.overArt(c.mouse.getX() * w.getScaledWidth() / w.getWidth(), c.mouse.getY() * w.getScaledHeight() / w.getHeight());
    }

    /** Underlines width px of a label at (x, y) in its ink, and records that the widget drew it. */
    static void underline(DrawContext g, String widget, int x, int y, int width, int color) {
        g.fill(x, y, x + width, y + 1, color);
        underlinedDrawn.add(widget);
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
        rectsDrawn.clear(); textsDrawn.clear(); texturesDrawn.clear(); colorsDrawn.clear(); portraitsDrawn.clear(); rowsDrawn.clear(); underlinedDrawn.clear();
        UiCanvas.begin(g);
        try {
            int[] ro = rollAt(), lo = letterAt(), la = ladderAt();
            WarTable.drawChrome(g, "recruit");
            rectsDrawn.putAll(WarTable.drawn()); // table and rail, as drawChrome drew them
            texturesDrawn.put("table", WarTable.drawnTable());
            rod(g, WarTable.origin()[0]);
            var roll = RecruitView.roll(house);
            roll(g, roll, ro[0] + ROLL_X, ro[1] + ROLL_Y, ro[1]);
            letter(g, row(roll, selected), lo[0] + LETTER_X, lo[1] + LETTER_Y);
            ladder(g, la[0], la[1]);
            notice(g, la[0], la[1]);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** What lies over the widgets: the Odds note. CampScreen calls it after they have drawn. */
    void overlay(DrawContext g) {
        if (!odds) return;
        UiCanvas.begin(g);
        try {
            int[] o = letterAt();
            note(g, o[0] + NOTE_X, o[1] + NOTE_Y);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** The brass rod the banners hang from (the banners are widgets, drawn after it). */
    void rod(DrawContext g, int ox) {
        int n = RecruitView.houses().size(), x = ox + rodX(n), length = BANNER_STEP * n + 8;
        rectsDrawn.put("rod", UiTex.three(g, "recruit/banner-rod", x, ROD_Y, length));
    }

    /** A House's banner in the box (x, y, w, h) its widget hands over: its cloth, longer and brighter when chosen, and its name in
     *  the ink that reads on that cloth, underlined while hot unless it is the chosen House (a click on it does nothing). */
    void paintBanner(DrawContext g, RecruitView.House h, int x, int y, int w, int bh) {
        var font = MinecraftClient.getInstance().textRenderer;
        boolean on = h.id().equals(house);
        String key = "banner-" + h.id(), widget = "house-" + h.id();
        String cloth = "recruit/banner-" + h.cloth() + (on ? "-on" : ""); // exercised by the suite: build_ui_art --verify sees only whole names
        UiTex.draw(g, cloth, x, y);
        String name = UiMath.trim(h.name(), LABEL_W, s -> font.getWidth(heading(s)));
        int color = LIGHT_CLOTHS.contains(h.cloth()) ? DarkUi.INK_TEXT : DarkUi.IVORY, width = font.getWidth(heading(name));
        int lx = WarTableHome.centred(g, heading(name), x + 52, y + LABEL_Y, color);
        if (!on && hot(buttons.get(widget))) underline(g, widget, lx, y + LABEL_Y + 12, width, color);
        rectsDrawn.put(key, new int[]{x, y, w, bh});
        rectsDrawn.put("label-" + h.id(), new int[]{lx, y + LABEL_Y, width, 11});
        texturesDrawn.put(key, cloth);
        textsDrawn.put(key, name);
        colorsDrawn.put(key, color);
    }

    /** The muster roll: the paper, the selected row's mark, the rules between rows (none at the selected row's top, where the mark's
     *  edge is; the mock keeps the one under it), then each row shown. */
    void roll(DrawContext g, List<RecruitView.Row> roll, int x, int y, int oy) {
        rectsDrawn.put("roll", UiTex.draw(g, "recruit/muster-roll", x, y));
        int shown = Math.min(ROWS, roll.size() - scroll), sel = index(roll, selected) - scroll;
        if (sel >= 0 && sel < shown) rectsDrawn.put("mark", UiTex.draw(g, "recruit/row-mark", x, oy + ROW_TOP + ROW_H * sel));
        for (int i = 1; i < shown; i++) {
            if (i == sel) continue;
            rectsDrawn.put("rule-" + i, UiTex.draw(g, "recruit/rule-208", x + 16, oy + ROW_TOP + ROW_H * i));
        }
        for (int i = 0; i < shown; i++) rowAt(g, roll.get(scroll + i), i, x, oy + ROW_TOP + ROW_H * i, i != sel && hot(buttons.get("row-" + i)));
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
        if (hot) underline(g, "row-" + i, x + 60, ny + 12, font.getWidth(heading(name)), color);
        if (r.locked()) UiTex.draw(g, "recruit/padlock-brass", x + 23, y + 22);
        else if (r.stars() > 0)
            for (int s = 0; s < 3; s++) {
                if (s < r.stars()) UiTex.draw(g, "recruit/star-s", x + 60 + 12 * s, y + 30);
                else UiTex.draw(g, "recruit/star-s-empty", x + 60 + 12 * s, y + 30);
            }
        rectsDrawn.put("row-" + i, new int[]{x, y, ROLL_W, ROW_H});
        rowsDrawn.add(new RowDraw(i, r.troopId(), name, color, silhouette));
    }

    /** Whether a reveal is on: the reveal's 2400 ms have not run out, or the harness holds its clock (CampaignClient.revealHeldAt). */
    static boolean revealing() { return CampaignClient.revealHeldAt >= 0 || CampaignClient.revealUntil > System.currentTimeMillis(); }

    /** The opened letter: the selected troop's live model in the ornate frame, its name, what it is, its stars, what it costs to
     *  deploy, the broken House seal while it is the troop just recruited, and, for a duplicate roll still being shown, the star
     *  that fills over CampaignClient.ROLL_FILL_MS and the line of what the copy did (RecruitPages.outcome) in place of the deploy
     *  line. The action tag is a widget (paintChoose); under it the page writes what it does or why it cannot be pressed, and where
     *  a troop has no tag, the words that say so ("From letters only", "Full stars"). */
    void letter(DrawContext g, RecruitView.Row r, int x, int y) {
        var font = MinecraftClient.getInstance().textRenderer;
        rectsDrawn.put("letter", UiTex.draw(g, "recruit/letter-hi", x, y));
        if (r == null) return;
        UiTex.draw(g, "table/frame-back-160", x + 72, y + 40);
        var e = model(r.troopId());
        if (e != null) {
            Portrait.draw(g, e, x + 72, y + 40, 160, 0); // Phase 2: tall 2 for the troops it marks Tall
            var d = Portrait.last();
            portraitsDrawn.add(new PortraitDraw(r.troopId(), "letter", d));
            rectsDrawn.put("window", new int[]{d.left(), d.top(), d.window(), d.window()}); // the window Portrait scissored
        }
        UiTex.draw(g, "table/frame-ornate-184", x + 60, y + 28);
        String name = UiMath.trim(r.name(), LINE_W, s -> font.getWidth(DarkUi.focal(s)));
        int nameInk = DarkUi.INK_TEXT;
        int nameX = WarTableHome.centred(g, DarkUi.focal(name), x + 152, y + 224, nameInk);
        rectsDrawn.put("name", new int[]{nameX, y + 224, font.getWidth(DarkUi.focal(name)), 11}); // the words the sheet widget covers
        String kind = r.rarity().isEmpty() ? r.group() + (r.owned() ? ", recruited" : ", not yet recruited") : r.rarity() + " " + r.group().toLowerCase(Locale.ROOT);
        kind = UiMath.trim(kind, LINE_W, s -> font.getWidth(DarkUi.body(s)));
        int kindInk = DarkUi.SEPIA;
        WarTableHome.centred(g, DarkUi.body(kind), x + 152, y + 246, kindInk);
        boolean lighting = lit != null && r.troopId().equals(litTroop) && revealing();
        var fill = lighting ? RecruitPages.fill(lit.starsBefore(), Math.max(lit.starsBefore(), lit.starsAfter()), CampaignClient.rollElapsed()) : new RecruitPages.Fill(r.stars(), -1, 0);
        if (r.stars() > 0 || lighting) {
            for (int s = 0; s < 3; s++) {
                int sx = x + 127 + 19 * s, sy = y + 268;
                if (s < fill.filled()) UiTex.draw(g, "recruit/star-l", sx, sy);
                else {
                    UiTex.draw(g, "recruit/star-l-empty", sx, sy);
                    if (s == fill.growing()) { // the star filling: the art's lower part, from its foot up
                        int h = Math.max(1, Math.round(14 * fill.part()));
                        UiTex.part(g, "recruit/star-l", sx, sy + 14 - h, 0, 14 - h, 14, h);
                    }
                }
            }
            int newest = fill.growing() >= 0 ? fill.growing() : fill.filled() - 1;
            if (newest >= 0) UiTex.draw(g, "recruit/sparkle", x + 127 + 19 * newest - 2, y + 266); // on the newest star
            textsDrawn.put("letter-stars", fill.filled() + (fill.growing() >= 0 ? "+" : ""));
            if (fill.growing() >= 0) textsDrawn.put("letter-fill", String.valueOf(Math.round(fill.part() * 100))); // how far the growing star has filled, in percent
        }
        var spec = ArmyCards.spec(r.troopId());
        String deploy = spec == null ? "" : "Deploys in battle for " + spec.cost() + " gold"; // Phase 2: the star bonus
        String outcome = lighting ? RecruitPages.outcome(lit, CampaignClient.profile) : "";
        int deployInk = DarkUi.INK_TEXT;
        if (outcome.isEmpty()) WarTableHome.centred(g, DarkUi.body(deploy), x + 152, y + 292, deployInk);
        else {
            String line = UiMath.trim(outcome, LINE_W, s -> font.getWidth(DarkUi.body(s)));
            int lx = WarTableHome.centred(g, DarkUi.body(line), x + 152, y + 292, DarkUi.INK_TEXT);
            rectsDrawn.put("outcome", new int[]{lx, y + 292, font.getWidth(DarkUi.body(line)), 11});
            textsDrawn.put("letter-outcome", line);
            colorsDrawn.put("letter-outcome", DarkUi.INK_TEXT);
        }
        if (r.owned() && r.stars() > 0) UiTex.draw(g, "recruit/wax-star", x + WAX_X, y + WAX_Y);
        if (r.troopId().equals(broken)) rectsDrawn.put("broken", UiTex.draw(g, "recruit/wax-broken", x + 135, y - 16));
        var a = RecruitView.action(r.troopId());
        int below = y + REASON_Y;
        if (a != null) reasonLines(g, font, a, x, below);
        else {
            String none = RecruitView.noAction(r.troopId());
            if (!none.isEmpty()) {
                int nx = WarTableHome.centred(g, DarkUi.body(none), x + 152, y + CHOOSE_Y + 6, DarkUi.SEPIA);
                rectsDrawn.put("noaction", new int[]{nx, y + CHOOSE_Y + 6, font.getWidth(DarkUi.body(none)), 11});
                textsDrawn.put("letter-none", none);
                colorsDrawn.put("letter-none", DarkUi.SEPIA);
            }
        }
        textsDrawn.put("letter-troop", r.troopId());
        textsDrawn.put("letter-name", name);
        textsDrawn.put("letter-line1", kind);
        if (outcome.isEmpty()) textsDrawn.put("letter-line2", deploy);
        colorsDrawn.put("letter-name", nameInk);
        colorsDrawn.put("letter-line1", kindInk);
        colorsDrawn.put("letter-line2", deployInk);
    }

    /** The words under the action tag: what the action does while it can be pressed (sepia), why it cannot while it cannot (the
     *  ink, "Need 300 gold"), in at most REASON_LINES lines of LINE_W (UiMath.lines), centred on the letter. The words say which it
     *  is; the ink only stresses it. */
    void reasonLines(DrawContext g, net.minecraft.client.font.TextRenderer font, RecruitPages.Action a, int x, int top) {
        boolean usable = choose != null && choose.active;
        var lines = UiMath.lines(reason(a), LINE_W, REASON_LINES, s -> font.getWidth(DarkUi.body(s)));
        int ink = usable ? DarkUi.SEPIA : DarkUi.INK_TEXT, left = Integer.MAX_VALUE, right = Integer.MIN_VALUE;
        for (int i = 0; i < lines.size(); i++) {
            int lx = WarTableHome.centred(g, DarkUi.body(lines.get(i)), x + 152, top + REASON_STEP * i, ink);
            left = Math.min(left, lx);
            right = Math.max(right, lx + font.getWidth(DarkUi.body(lines.get(i))));
        }
        if (lines.isEmpty()) return;
        rectsDrawn.put("reason", new int[]{left, top, right - left, REASON_STEP * (lines.size() - 1) + 11});
        textsDrawn.put("reason", String.join(" ", lines));
        colorsDrawn.put("reason", ink);
    }

    /** The sheet widget over the letter's name: a thin rule under the name says it can be pressed (a line, not only a colour); the name's ink
     *  and a second line under it while hot (the pointer or the keyboard's focus). The widget is as wide as the name, so the paper beside it is not a button. */
    void paintSheet(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        boolean hot = hot(buttons.get("sheet"));
        int ink = hot ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        g.fill(x + 4, y + 15, x + w - 4, y + 16, ink);
        if (hot) underline(g, "sheet", x + 4, y + 17, w - 8, ink);
        rectsDrawn.put("sheet", new int[]{x, y, w, h});
    }

    /** The letter's action tag (the widget "choose"): a vellum tag with the action's label (RecruitPages.Action.label: "Buy 300",
     *  "Star 2", "Pact", "50 shards"), the coin before a Buy; sepia, and no press, while it cannot be pressed. */
    void paintChoose(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var font = MinecraftClient.getInstance().textRenderer;
        var row = row(RecruitView.roll(house), selected);
        var a = row == null ? null : RecruitView.action(row.troopId());
        if (a == null) return;
        int ink = choose != null && choose.active ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        String tag = "table/btn-vellum-88x24";
        var label = heading(a.label());
        boolean gold = a.command().startsWith("buy:");
        int lw = font.getWidth(label), span = lw + (gold ? 11 : 0), lx = x + (w - span) / 2 + (gold ? 11 : 0);
        UiTex.nine(g, tag, x, y, w + 1, h + 2);
        if (gold) UiTex.draw(g, "recruit/coin-m", lx - 11, y + 8);
        g.drawText(font, label, lx, y + 6, ink, false);
        if (hot(choose)) underline(g, "choose", lx, y + 18, lw, ink);
        rectsDrawn.put("choose", new int[]{x, y, w, h});
        texturesDrawn.put("choose", tag);
        textsDrawn.put("choose", a.label());
        textsDrawn.put("choose-command", a.command());
        colorsDrawn.put("choose", ink);
    }

    /** The ladder: the sealed letters while any are left, the purse centred on the axis, the rung's line (Enlist and Odds are
     *  widgets: paintEnlist, paintOdds). */
    void ladder(DrawContext g, int ox, int oy) {
        var font = MinecraftClient.getInstance().textRenderer;
        int pity = RecruitView.pityLeft(house);
        rectsDrawn.put("bundle", UiTex.draw(g, "recruit/letter-bundle", ox + BUNDLE_X, oy + BUNDLE_Y));
        String gold = String.valueOf(CampaignClient.profile.gold);
        int pw = 34 + font.getWidth(gold), px = ox + AXIS - (pw + 1) / 2, py = oy + PURSE_Y; // the pouch and the sum, centred on the axis
        int[] pouch = UiTex.draw(g, "recruit/pouch", px, py);
        g.drawText(font, gold, px + 34, py + 10, DarkUi.IVORY, true);
        rectsDrawn.put("purse", new int[]{px, py, pw, pouch[3]});
        textsDrawn.put("purse", gold);
        String rung = UiMath.pityLine(pity);
        int rungInk = DarkUi.MUTED, rw = font.getWidth(DarkUi.body(rung)), ry = WarTable.compact() ? oy + COMPACT_ODDS_Y + 7 : oy + RUNG_Y;
        int rx = ox + AXIS - 4 - rw; // compact: right of it, Odds on the same row
        if (WarTable.compact()) g.drawText(font, DarkUi.body(rung), rx, ry, rungInk, false);
        else rx = WarTableHome.centred(g, DarkUi.body(rung), ox + AXIS, ry, rungInk);
        rectsDrawn.put("rung", new int[]{rx, ry, rw, 11});
        textsDrawn.put("rung", rung);
        colorsDrawn.put("rung", rungInk);
    }

    /** The server's notice (a refusal; a collection that could not be saved), centred on the ladder's axis under its foot in IVORY,
     *  on two lines when one is too short (UiMath.lines: after a sentence if one fits): shown WarTableHome.NOTICE_TICKS, then
     *  faded over NOTICE_FADE_TICKS, as home's line under Ride is. */
    void notice(DrawContext g, int ox, int oy) {
        long age = ticks - noticeAt;
        if (noticeText.isEmpty() || age >= WarTableHome.NOTICE_TICKS + WarTableHome.NOTICE_FADE_TICKS) return;
        var font = MinecraftClient.getInstance().textRenderer;
        float fade = age < WarTableHome.NOTICE_TICKS ? 1 : 1 - (age - WarTableHome.NOTICE_TICKS) / (float) WarTableHome.NOTICE_FADE_TICKS;
        boolean compact = WarTable.compact();
        var lines = UiMath.lines(HintText.expand(noticeText, GravesAndGloryClient::hint), compact ? COMPACT_NOTICE_W : NOTICE_W, NOTICE_LINES, s -> font.getWidth(DarkUi.body(s)));
        if (lines.isEmpty()) return;
        int alpha = Math.max(4, Math.round(fade * 255)), ink = (alpha << 24) | (DarkUi.IVORY & 0xffffff);
        int left = Integer.MAX_VALUE, right = Integer.MIN_VALUE, top = oy + (compact ? COMPACT_NOTICE_Y : NOTICE_Y);
        for (int i = 0; i < lines.size(); i++) {
            int x = WarTableHome.centred(g, DarkUi.body(lines.get(i)), ox + AXIS, top + NOTICE_STEP * i, ink);
            left = Math.min(left, x);
            right = Math.max(right, x + font.getWidth(DarkUi.body(lines.get(i))));
        }
        rectsDrawn.put("notice", new int[]{left, top, right - left, NOTICE_STEP * (lines.size() - 1) + 11});
        textsDrawn.put("notice", String.join(" ", lines));
        colorsDrawn.put("notice", ink);
    }

    /** Enlist: brass with the Enter key, the word and the cost; iron with "Need" and the cost in alarm while the purse is short. */
    void paintEnlist(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var font = MinecraftClient.getInstance().textRenderer;
        int cost = RecruitView.cost();
        boolean short_ = CampaignClient.profile.gold < cost;
        String plate = short_ ? "table/btn-iron-240x40" : "table/btn-brass-240x40";
        String label = "Enlist", price = null;
        int ink = short_ ? DarkUi.IVORY : DarkUi.PLATE_INK, priceInk = 0;
        UiTex.draw(g, plate, x, y);
        if (!short_) {
            UiTex.draw(g, "table/key-brass-37", x + 14, y + 13);
            g.drawText(font, "Enter", x + 18, y + 16, DarkUi.KEY_INK, false);
        }
        int lx = WarTableHome.centred(g, heading(label), x + 120, y + 14, ink);
        if (hot(enlist)) underline(g, "enlist", lx, y + 26, font.getWidth(heading(label)), ink);
        if (short_) {
            price = "Need " + cost; priceInk = DarkUi.ALARM_TEXT;
            g.drawText(font, price, x + 224 - font.getWidth(price), y + 16, priceInk, false);
        } else {
            price = String.valueOf(cost); priceInk = DarkUi.PLATE_INK;
            int cx = x + 224 - font.getWidth(price);
            g.drawText(font, price, cx, y + 16, priceInk, false);
            UiTex.draw(g, "recruit/coin-m", cx - 10, y + 16); // 3 px before the cost
        }
        if (price != null) {
            textsDrawn.put("enlist-cost", price);
            colorsDrawn.put("enlist-cost", priceInk);
        }
        rectsDrawn.put("enlist", new int[]{x, y, w, h});
        texturesDrawn.put("enlist", plate);
        textsDrawn.put("enlist", label);
        colorsDrawn.put("enlist", ink);
    }

    /** Odds: a vellum tag with the die and the word; it opens and closes the note. */
    void paintOdds(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var font = MinecraftClient.getInstance().textRenderer;
        var label = heading("Odds");
        int ink = DarkUi.INK_TEXT;
        UiTex.draw(g, "table/btn-vellum-64x24", x, y);
        UiTex.draw(g, "recruit/die", x + 12, y + 7);
        g.drawText(font, label, x + 26, y + 6, ink, false);
        if (hot(buttons.get("odds"))) underline(g, "odds", x + 26, y + 18, font.getWidth(label), ink);
        rectsDrawn.put("odds", new int[]{x, y, w, h});
    }

    /** The Odds note, pinned over the letter: the chance of each rarity in this House's letters (the weights of the sheet), the
     *  letters left until its Epic is sure, the rule that a troop you own gives a copy toward its next star, and where a Legendary
     *  comes from. Each rule wraps to the note's width (UiMath.lines), none is cut. */
    void note(DrawContext g, int x, int y) {
        var font = MinecraftClient.getInstance().textRenderer;
        rectsDrawn.put("note", UiTex.draw(g, "recruit/odds-note", x, y));
        g.drawText(font, heading("Odds"), x + 16, y + 12, DarkUi.INK_TEXT, false);
        List<String> lines = new ArrayList<>();
        for (String rule : RecruitView.note(house)) lines.addAll(UiMath.lines(rule, NOTE_W, 2, s -> font.getWidth(DarkUi.body(s))));
        for (int i = 0; i < lines.size() && i < NOTE_LINES; i++) {
            g.drawText(font, DarkUi.body(lines.get(i)), x + 16, y + 34 + 12 * i, DarkUi.INK_TEXT, false);
            textsDrawn.put("note-" + i, lines.get(i));
        }
    }
}
