package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The War Table troop sheet (War Table port plan Task 5; Kort, 2026-10-24: build it): one troop on two papers, opened by a click on
 *  the name on the recruit page's letter (CampScreen page troop). On the left the letter as the recruit page draws it: the troop's
 *  live model, its name, its House and rarity, its stars, what it costs to deploy, the wax of a troop recruited and the same action
 *  tag (RecruitPages.action: Buy, Star, Pact or shards, with the reason under it when it cannot be pressed). On the right a sheet
 *  with two faces, chosen by two tags: the Troop face (the star ladder with the cost of the next star and the copies or shards
 *  held, the Mastery title and the progress toward the next level, and the three levels of the troop in battle with the name and
 *  the ability of each, read from the rows the card and Abilities read) and the Matchups face (what the troop beats and what beats
 *  it, and its counter lines). The rail's back plaque (Esc) returns to the recruit page on the same House and troop.
 *
 *  A buy that comes back as a roll of this troop shows on the letter as on the recruit page: the broken seal for a troop new to the
 *  collection, the lit star or copy and its line for a duplicate (CampaignClient's reveal); leaving the sheet ends the reveal.
 *  Everything is drawn in canvas pixels; page coordinates (the 960x540 page) are offset by WarTable.column(COMPACT). */
final class WarTableTroop {
    /** The letter and the sheet on the 960 page: the pair stands centred (113 from the left edge, 24 apart), both papers 124 from the top. */
    static final int LETTER_X = 113, SHEET_X = 447, TOP = 124, SHEET_W = 404, SHEET_H = 373;
    /** The compact page (WarTable.compact): both papers rise 32 and close up 53 (letter at 60, sheet at 394 on the 854 page). */
    static final int[] COMPACT = {-53, -32};
    /** On the sheet (sheet-relative): the text's left edge and width, the tags' row, the first section's head and the last row the
     *  content may use (the notice line lies under it). */
    static final int PAD = 22, TEXT_W = 360, TAB_Y = 14, TAB_W = 88, TAB_H = 24, TAB_GAP = 8, HEAD_Y = 48, CONTENT_END = 334, NOTICE_Y = 338, STEP = 12;
    /** The name's hit box on the letter (letter-relative, as WarTableRecruit places the name): its top and height. */
    static final int NAME_Y = 222, NAME_H = 18;

    /** A portrait the last frame drew: the troop, where (letter) and what Portrait.draw passed to drawEntity. */
    record PortraitDraw(String troop, String where, Portrait.Drawn drawn) {}
    /** A counter line the last frame drew, in order: a troop can draw the same row for both what it deals and what it takes, so the
     *  id-keyed texts map would keep only the last; this list keeps every line. */
    record CounterDraw(String id, boolean favour, String text) {}

    // What the last frame drew, refilled every frame and read by the checks through the views below.
    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> textsDrawn = new LinkedHashMap<>();
    private static final Map<String, Integer> colorsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> texturesDrawn = new LinkedHashMap<>();
    private static final List<PortraitDraw> portraitsDrawn = new ArrayList<>();
    private static final List<CounterDraw> countersDrawn = new ArrayList<>();
    private static final Set<String> underlinedDrawn = new LinkedHashSet<>();
    private static final List<String> sentCommands = new ArrayList<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String, String> texts = Collections.unmodifiableMap(textsDrawn);
    private static final Map<String, Integer> colors = Collections.unmodifiableMap(colorsDrawn);
    private static final Map<String, String> textures = Collections.unmodifiableMap(texturesDrawn);
    private static final List<PortraitDraw> portraits = Collections.unmodifiableList(portraitsDrawn);
    private static final List<CounterDraw> counters = Collections.unmodifiableList(countersDrawn);
    private static final Set<String> underlined = Collections.unmodifiableSet(underlinedDrawn);
    private static final List<String> sent = Collections.unmodifiableList(sentCommands);

    /** Rects {x,y,w,h} in canvas px, as drawn: table and rail (WarTable.drawChrome), letter, sheet (the papers), window (the portrait's),
     *  broken (the seal), outcome, noaction and reason (the letter's lines), choose and tab-troop, tab-matchups (the widgets' boxes), and
     *  the blocks of the sheet's words: head-(section), held, rung-(1 to 3), mastery-level, mastery-title, mastery-next, mastery-kills,
     *  mastery-fights, level-(0 to 2)-name and level-(0 to 2) (the ability), beats, beaten-by, counter-(id) and notice. */
    static Map<String, int[]> rects() { return rects; }
    /** Words drawn, keyed as rects are: letter-troop (the troop's id), letter-name, letter-line1 (House and rarity), letter-line2 (the
     *  deploy line, absent while a duplicate's outcome takes its place), letter-outcome, letter-stars, letter-fill, letter-none ("From
     *  letters only", "Full stars"), choose (the tag's label), choose-command, reason, tab (the face shown: troop or matchups), held,
     *  rung-(1 to 3), mastery-*, level-(0 to 2)-name, level-(0 to 2) (the ability's lines joined by a space), beats, beaten-by,
     *  counter-(id) and glyph-(id) ("favour" or "against"), notice (its lines joined by a space). */
    static Map<String, String> texts() { return texts; }
    static Map<String, Integer> colors() { return colors; }
    /** The texture each part was drawn from: table, choose, tab-troop and tab-matchups. */
    static Map<String, String> textures() { return textures; }
    static List<PortraitDraw> portraits() { return portraits; }
    /** The counter lines the last frame drew, in order, including a row drawn both ways. */
    static List<CounterDraw> counters() { return counters; }
    /** The widgets the last frame drew with their label underlined (hot: under the pointer, or holding the keyboard's focus). */
    static Set<String> underlined() { return underlined; }
    /** The last WarTablePages.SENT_KEPT commands the sheet handed to the server (after CampScreen.action's throttle let it through). */
    static List<String> sent() { return sent; }

    final CampScreen screen;
    /** The troop shown (a Spec.TROOPS id), the face of the sheet (troop or matchups) and whether the troop's seal is broken or a duplicate's star lit. */
    String troop, tab = "troop";
    boolean broken;
    CampaignClient.LastRoll lit;
    /** CampaignClient.revealUntil when the sheet last took a reveal: a reveal from before the sheet opened stays untaken. */
    long revealSeen;
    /** The camp state when the tag was last pressed, and the tick: the tag takes no second press until the camp's state moves or WarTableRecruit.PENDING_TICKS pass. */
    String pendingState = "";
    long pendingAt = -1000;
    /** The page's clock (client ticks) and the notice line: the server's notice serial last taken, its text and the tick it appeared. */
    long ticks;
    int noticeSerialSeen;
    String noticeText = "";
    long noticeAt = -1000;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    UiButton choose;

    WarTableTroop(CampScreen screen) {
        this.screen = screen;
        troop = ArmyCards.spec(screen.troop) != null ? screen.troop : Spec.TROOPS[0].id();
        revealSeen = CampaignClient.revealUntil;
        noticeSerialSeen = CampaignClient.noticeSerial;
    }

    static int[] columnAt() { return WarTable.column(COMPACT[0], COMPACT[1]); }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    static Text heading(String s) { return DarkUi.heading(s); }

    /** The widget a name stands for (back, choose, tab-troop, tab-matchups), or null. */
    UiButton button(String name) { return buttons.get(name); }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    Spec.TroopsRow spec() { return ArmyCards.spec(troop); }

    Profile.Owned owned() { return CampaignClient.profile.owned.get(troop); }

    /** Builds the widgets (CampScreen.init on page troop): again whenever the camp state changes. */
    void init() {
        buttons.clear();
        int[] o = columnAt();
        add("back", WarTable.plaque(56, 120, "icon-recruit", "Recruits", "Esc", 85, 0, b -> back()));
        int sx = o[0] + SHEET_X, sy = o[1] + TOP;
        add("tab-troop", new UiButton(sx + PAD, sy + TAB_Y, TAB_W, TAB_H, Text.literal("Troop"), b -> face("troop"), (g, x, y, w, h, hot) -> paintTab(g, "troop", "Troop", x, y, w, h)));
        add("tab-matchups", new UiButton(sx + PAD + TAB_W + TAB_GAP, sy + TAB_Y, TAB_W, TAB_H, Text.literal("Matchups"), b -> face("matchups"), (g, x, y, w, h, hot) -> paintTab(g, "matchups", "Matchups", x, y, w, h)));
        choose = add("choose", new UiButton(o[0] + LETTER_X + WarTableRecruit.CHOOSE_X, o[1] + TOP + WarTableRecruit.CHOOSE_Y, WarTableRecruit.CHOOSE_W, WarTableRecruit.CHOOSE_H, Text.literal("Buy"), b -> chooseTroop(), this::paintChoose));
        refresh();
    }

    /** Puts the tag in step with the troop and the purse: it shows when the troop has an action (RecruitPages.action) and takes presses only while it can send. */
    void refresh() {
        if (choose == null) return;
        var a = RecruitView.action(troop);
        choose.visible = a != null;
        choose.setMessage(Text.literal(a == null ? "" : a.label()));
        choose.active = a != null && a.active() && WarTableRecruit.battleFree() && !pending();
    }

    /** Shows another troop of the roll in place (the harness walks the thirty with it), the seal and the lit star left behind. */
    void show(String id) {
        if (ArmyCards.spec(id) == null) return;
        troop = id; broken = false; lit = null; pendingState = "";
        refresh();
    }

    void face(String which) { tab = which; }

    /** Whether a press of the tag is still on its way: the camp has not answered it (its state is as at the press) and PENDING_TICKS have not passed. */
    boolean pending() { return !pendingState.isEmpty() && ticks - pendingAt < WarTableRecruit.PENDING_TICKS && pendingState.equals(screen.state()); }

    String reason(RecruitPages.Action a) {
        if (!WarTableRecruit.battleFree()) return "A battle is under way.";
        if (pending()) return "Sent. Waiting for the camp.";
        return a.tip();
    }

    /** Back to the recruit page on this troop's House, the troop selected (the recruit page reads CampScreen.troop once); leaving ends a reveal. */
    void back() { UiSounds.back(); screen.troop = troop; screen.page("recruit"); }

    /** Hands a command to the server through the screen's throttle; recorded when it goes. Returns whether it went. */
    boolean send(String cmd) {
        if (!screen.action(cmd)) return false;
        if (sentCommands.size() >= WarTablePages.SENT_KEPT) sentCommands.remove(0);
        sentCommands.add(cmd);
        return true;
    }

    /** The action tag: sends what RecruitPages.action gives for the troop (buy:, star: or legendary:), once; a second press waits for the camp's answer. */
    void chooseTroop() {
        var a = RecruitView.action(troop);
        if (a == null || !a.active() || !WarTableRecruit.battleFree() || pending()) return;
        if (send(a.command())) { pendingState = screen.state(); pendingAt = ticks; refresh(); }
    }

    /** A new reveal (CampaignClient.revealUntil moved) naming this troop: the broken seal on its letter, or, when the roll that brought
     *  it was a duplicate (CampaignClient.lastRoll), the lit star or copy instead. A new notice shows on the sheet's notice line. */
    void tick() {
        ticks++;
        var t = spec();
        boolean revealed = false;
        if (CampaignClient.revealUntil != revealSeen) {
            revealSeen = CampaignClient.revealUntil;
            if (t != null && t.name().equals(CampaignClient.revealUnit)) {
                var last = CampaignClient.lastRoll;
                boolean duplicate = CampaignClient.rollShowing() && last != null && last.duplicate() && last.troop().equals(troop);
                broken = !duplicate;
                lit = duplicate ? last : null;
                revealed = true;
            }
        }
        if (CampaignClient.noticeSerial != noticeSerialSeen) {
            noticeSerialSeen = CampaignClient.noticeSerial;
            noticeText = revealed ? "" : CampaignClient.notice;
            if (!noticeText.isEmpty()) noticeAt = ticks;
        }
        refresh();
    }

    /** A key's press (CampScreen hands over only the press): Esc goes back to the recruit page; Enter or Space presses the widget the
     *  keyboard's focus is on, and Enter with none presses the action tag when it can send. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { back(); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            var target = keyboardFocus();
            if (target == null && key != GLFW.GLFW_KEY_SPACE) target = choose;
            if (target != null && target.visible && target.active) WarTableRecruit.press(target);
            return true;
        }
        return false;
    }

    UiButton keyboardFocus() {
        if (!MinecraftClient.getInstance().getNavigationType().isKeyboard()) return null;
        for (var b : buttons.values()) if (b.isFocused() && b.visible) return b;
        return null;
    }

    void underline(DrawContext g, String widget, int x, int y, int width, int color) {
        g.fill(x, y, x + width, y + 1, color);
        underlinedDrawn.add(widget);
    }

    void render(DrawContext g) {
        rectsDrawn.clear(); textsDrawn.clear(); colorsDrawn.clear(); texturesDrawn.clear(); portraitsDrawn.clear(); countersDrawn.clear(); underlinedDrawn.clear();
        var t = spec();
        UiCanvas.begin(g);
        try {
            int[] o = columnAt();
            WarTable.drawChrome(g, "recruit");
            rectsDrawn.putAll(WarTable.drawn());
            texturesDrawn.put("table", WarTable.drawnTable());
            if (t == null) return;
            letter(g, t, o[0] + LETTER_X, o[1] + TOP);
            sheet(g, t, o[0] + SHEET_X, o[1] + TOP);
        } finally {
            UiCanvas.end(g);
        }
    }

    // ---- the letter (the recruit page's, with the House on its kind line) ----
    void letter(DrawContext g, Spec.TroopsRow t, int x, int y) {
        var font = font();
        var o = owned();
        rectsDrawn.put("letter", UiTex.draw(g, "recruit/letter-hi", x, y));
        UiTex.draw(g, "table/frame-back-160", x + 72, y + 40);
        var e = WarTableRecruit.model(troop);
        if (e != null) {
            Portrait.draw(g, e, x + 72, y + 40, 160, 0);
            var d = Portrait.last();
            portraitsDrawn.add(new PortraitDraw(troop, "letter", d));
            rectsDrawn.put("window", new int[]{d.left(), d.top(), d.window(), d.window()});
        }
        UiTex.draw(g, "table/frame-ornate-184", x + 60, y + 28);
        String name = UiMath.trim(t.name(), WarTableRecruit.LINE_W, s -> font.getWidth(DarkUi.focal(s)));
        int nw = font.getWidth(DarkUi.focal(name));
        WarTableHome.centred(g, DarkUi.focal(name), x + 152, y + 224, DarkUi.INK_TEXT);
        String kind = RosterMath.houseName(t.house()) + ", " + t.rarity() + " " + RecruitView.group(t.group()).toLowerCase(Locale.ROOT);
        kind = UiMath.trim(kind, WarTableRecruit.LINE_W, s -> font.getWidth(DarkUi.body(s)));
        WarTableHome.centred(g, DarkUi.body(kind), x + 152, y + 246, DarkUi.SEPIA);
        boolean lighting = lit != null && troop.equals(lit.troop()) && WarTableRecruit.revealing();
        int stars = o == null ? 0 : o.stars;
        var fill = lighting ? RecruitPages.fill(lit.starsBefore(), Math.max(lit.starsBefore(), lit.starsAfter()), CampaignClient.rollElapsed()) : new RecruitPages.Fill(stars, -1, 0);
        if (stars > 0 || lighting) {
            for (int s = 0; s < 3; s++) {
                int sx = x + 127 + 19 * s, sy = y + 268;
                if (s < fill.filled()) UiTex.draw(g, "recruit/star-l", sx, sy);
                else {
                    UiTex.draw(g, "recruit/star-l-empty", sx, sy);
                    if (s == fill.growing()) {
                        int h = Math.max(1, Math.round(14 * fill.part()));
                        UiTex.part(g, "recruit/star-l", sx, sy + 14 - h, 0, 14 - h, 14, h);
                    }
                }
            }
            int newest = fill.growing() >= 0 ? fill.growing() : fill.filled() - 1;
            if (newest >= 0) UiTex.draw(g, "recruit/sparkle", x + 127 + 19 * newest - 2, y + 266);
            textsDrawn.put("letter-stars", fill.filled() + (fill.growing() >= 0 ? "+" : ""));
            if (fill.growing() >= 0) textsDrawn.put("letter-fill", String.valueOf(Math.round(fill.part() * 100)));
        }
        String deploy = "Deploys in battle for " + t.cost() + " gold";
        String outcome = lighting ? RecruitPages.outcome(lit, CampaignClient.profile) : "";
        if (outcome.isEmpty()) WarTableHome.centred(g, DarkUi.body(deploy), x + 152, y + 292, DarkUi.INK_TEXT);
        else {
            String line = UiMath.trim(outcome, WarTableRecruit.LINE_W, s -> font.getWidth(DarkUi.body(s)));
            int lx = WarTableHome.centred(g, DarkUi.body(line), x + 152, y + 292, DarkUi.INK_TEXT);
            rectsDrawn.put("outcome", new int[]{lx, y + 292, font.getWidth(DarkUi.body(line)), 11});
            textsDrawn.put("letter-outcome", line);
            colorsDrawn.put("letter-outcome", DarkUi.INK_TEXT);
        }
        if (o != null && o.stars > 0) UiTex.draw(g, "recruit/wax-star", x + WarTableRecruit.WAX_X, y + WarTableRecruit.WAX_Y);
        if (broken) rectsDrawn.put("broken", UiTex.draw(g, "recruit/wax-broken", x + 135, y - 16));
        var a = RecruitView.action(troop);
        if (a != null) reasonLines(g, font, a, x, y + WarTableRecruit.REASON_Y);
        else {
            String none = RecruitView.noAction(troop);
            if (!none.isEmpty()) {
                int nx = WarTableHome.centred(g, DarkUi.body(none), x + 152, y + WarTableRecruit.CHOOSE_Y + 6, DarkUi.SEPIA);
                rectsDrawn.put("noaction", new int[]{nx, y + WarTableRecruit.CHOOSE_Y + 6, font.getWidth(DarkUi.body(none)), 11});
                textsDrawn.put("letter-none", none);
                colorsDrawn.put("letter-none", DarkUi.SEPIA);
            }
        }
        rectsDrawn.put("name", new int[]{x + 152 - nw / 2, y + 224, nw, 11});
        textsDrawn.put("letter-troop", troop);
        textsDrawn.put("letter-name", name);
        textsDrawn.put("letter-line1", kind);
        if (outcome.isEmpty()) textsDrawn.put("letter-line2", deploy);
        colorsDrawn.put("letter-name", DarkUi.INK_TEXT);
        colorsDrawn.put("letter-line1", DarkUi.SEPIA);
        colorsDrawn.put("letter-line2", DarkUi.INK_TEXT);
    }

    /** The words under the action tag: what the action does while it can be pressed (sepia), why it cannot while it cannot (the ink), in at most REASON_LINES lines. */
    void reasonLines(DrawContext g, TextRenderer font, RecruitPages.Action a, int x, int top) {
        boolean usable = choose != null && choose.active;
        var lines = UiMath.lines(reason(a), WarTableRecruit.LINE_W, WarTableRecruit.REASON_LINES, s -> font.getWidth(DarkUi.body(s)));
        int ink = usable ? DarkUi.SEPIA : DarkUi.INK_TEXT, left = Integer.MAX_VALUE, right = Integer.MIN_VALUE;
        for (int i = 0; i < lines.size(); i++) {
            int lx = WarTableHome.centred(g, DarkUi.body(lines.get(i)), x + 152, top + WarTableRecruit.REASON_STEP * i, ink);
            left = Math.min(left, lx);
            right = Math.max(right, lx + font.getWidth(DarkUi.body(lines.get(i))));
        }
        if (lines.isEmpty()) return;
        rectsDrawn.put("reason", new int[]{left, top, right - left, WarTableRecruit.REASON_STEP * (lines.size() - 1) + 11});
        textsDrawn.put("reason", String.join(" ", lines));
        colorsDrawn.put("reason", ink);
    }

    /** The letter's action tag, as the recruit page paints it: a vellum tag with the action's label, the coin before a Buy; sepia, and no press, while it cannot be pressed. */
    void paintChoose(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var font = font();
        var a = RecruitView.action(troop);
        if (a == null) return;
        int ink = choose != null && choose.active ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        String tag = "table/btn-vellum-88x24";
        var label = heading(a.label());
        boolean gold = a.command().startsWith("buy:");
        int lw = font.getWidth(label), span = lw + (gold ? 11 : 0), lx = x + (w - span) / 2 + (gold ? 11 : 0);
        UiTex.nine(g, tag, x, y, w + 1, h + 2);
        if (gold) UiTex.draw(g, "recruit/coin-m", lx - 11, y + 8);
        g.drawText(font, label, lx, y + 6, ink, false);
        if (WarTableRecruit.hot(choose)) underline(g, "choose", lx, y + 18, lw, ink);
        rectsDrawn.put("choose", new int[]{x, y, w, h});
        texturesDrawn.put("choose", tag);
        textsDrawn.put("choose", a.label());
        textsDrawn.put("choose-command", a.command());
        colorsDrawn.put("choose", ink);
    }

    /** A face tag of the sheet: a vellum tag with the word; the face shown has a heavy bar under its word (a shape, not only an ink), the other a thin line while hot. */
    void paintTab(DrawContext g, String which, String label, int x, int y, int w, int h) {
        var font = font();
        boolean on = tab.equals(which);
        String plate = "table/btn-vellum-88x24";
        UiTex.nine(g, plate, x, y, w + 1, h + 2);
        var t = heading(label);
        int tw = font.getWidth(t), lx = x + (w - tw) / 2, ink = on ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        g.drawText(font, t, lx, y + 6, ink, false);
        if (on) g.fill(lx, y + 18, lx + tw, y + 20, DarkUi.INK_TEXT);
        else if (WarTableRecruit.hot(buttons.get("tab-" + which))) underline(g, "tab-" + which, lx, y + 18, tw, ink);
        rectsDrawn.put("tab-" + which, new int[]{x, y, w, h});
        texturesDrawn.put("tab-" + which, plate);
        colorsDrawn.put("tab-" + which, ink);
    }

    // ---- the sheet ----
    void sheet(DrawContext g, Spec.TroopsRow t, int x, int y) {
        rectsDrawn.put("sheet", UiTex.draw(g, "table/sheet-banner", x, y));
        textsDrawn.put("tab", tab);
        if (tab.equals("matchups")) matchups(g, t, x + PAD, y);
        else levels(g, t, x + PAD, y);
        notice(g, x + PAD, y);
    }

    /** A section's head: its word in the heading font and a rule across the text's width, and the line that goes with it on the right (null for none). */
    void head(DrawContext g, String key, String word, String right, int x, int y) {
        var font = font();
        g.drawText(font, heading(word), x, y, DarkUi.INK_TEXT, false);
        g.fill(x, y + 12, x + TEXT_W, y + 13, DarkUi.SEPIA_LINE);
        rectsDrawn.put("head-" + key, new int[]{x, y, font.getWidth(heading(word)), 11});
        textsDrawn.put("head-" + key, word);
        if (right != null) {
            int w = font.getWidth(DarkUi.body(right));
            g.drawText(font, DarkUi.body(right), x + TEXT_W - w, y + 1, DarkUi.SEPIA, false);
            rectsDrawn.put(key, new int[]{x + TEXT_W - w, y + 1, w, 11});
            textsDrawn.put(key, right);
            colorsDrawn.put(key, DarkUi.SEPIA);
        }
    }

    /** One line of words at (x, y) in the body font, recorded under key (null for no record); returns its width. */
    int line(DrawContext g, String key, String s, int x, int y, int color) {
        var font = font();
        int w = font.getWidth(DarkUi.body(s));
        g.drawText(font, DarkUi.body(s), x, y, color, false);
        if (key != null) { rectsDrawn.put(key, new int[]{x, y, w, 11}); textsDrawn.put(key, s); colorsDrawn.put(key, color); }
        return w;
    }

    /** Words wrapped to w in at most max lines of the body font, step apart (UiMath.lines: nothing is cut while the text fits max lines);
     *  recorded under key as one block; returns the y under the last line. */
    int block(DrawContext g, String key, String s, int x, int y, int w, int max, int color) {
        var font = font();
        var lines = UiMath.lines(s, w, max, v -> font.getWidth(DarkUi.body(v)));
        int right = x;
        for (int i = 0; i < lines.size(); i++) {
            g.drawText(font, DarkUi.body(lines.get(i)), x, y + STEP * i, color, false);
            right = Math.max(right, x + font.getWidth(DarkUi.body(lines.get(i))));
        }
        if (key != null && !lines.isEmpty()) {
            rectsDrawn.put(key, new int[]{x, y, right - x, STEP * (lines.size() - 1) + 11});
            textsDrawn.put(key, String.join(" ", lines));
            colorsDrawn.put(key, color);
        }
        return y + STEP * Math.max(1, lines.size());
    }

    // The Troop face --------------------------------------------------------------------------------------------------
    /** The copies a troop needs held for a star (RosterMath.starFromCopies), 0 where it never gets there. */
    static int copiesFor(int star, Spec.RaritiesRow r) {
        for (int c = 1; c < 64; c++) if (RosterMath.starFromCopies(c, r) >= star) return c;
        return 0;
    }

    /** What reaching a star takes: the copies held in all for a Common or a Rare, the House's shards for an Epic or a Legendary. */
    static String cost(Spec.TroopsRow t, int star) {
        var r = RosterMath.rarityRow(t.rarity());
        boolean copies = t.rarity().equals("common") || t.rarity().equals("rare");
        if (copies) { int n = copiesFor(star, r); return n + (n == 1 ? " copy" : " copies"); }
        int n = r == null ? 0 : star <= 2 ? r.shardStar2() : r.shardStar3();
        return n + " " + RosterMath.houseName(t.house()) + " shards";
    }

    void levels(DrawContext g, Spec.TroopsRow t, int x, int y) {
        var font = font();
        var o = owned();
        var p = CampaignClient.profile;
        boolean copies = t.rarity().equals("common") || t.rarity().equals("rare");
        int have = o == null ? 0 : o.stars;
        // stars
        String held = copies ? "Copies held " + (o == null ? 0 : o.copies) : RosterMath.houseName(t.house()) + " shards held " + p.shards.getOrDefault(t.house(), 0);
        int ry = y + HEAD_Y;
        head(g, "held", "Stars", held, x, ry);
        ry += 18;
        for (int k = 1; k <= 3; k++) {
            for (int s = 0; s < 3; s++) UiTex.draw(g, s < k ? "recruit/star-s" : "recruit/star-s-empty", x + 12 * s, ry);
            line(g, null, "Star " + k, x + 44, ry + 1, DarkUi.INK_TEXT);
            String status = have >= k ? "Reached" : have + 1 == k ? "Next: " + (o == null ? "joins your collection" : cost(t, k) + (copies ? ", " + o.copies + " held" : "")) : cost(t, k);
            line(g, "rung-" + k, status, x + 98, ry + 1, have >= k || have + 1 == k ? DarkUi.INK_TEXT : DarkUi.SEPIA);
            ry += 14;
        }
        // Mastery
        ry += 8;
        int level = o == null ? 0 : o.mastery, top = Spec.MASTERY.length;
        head(g, "mastery-level", "Mastery", "Level " + level + " of " + top, x, ry);
        ry += 18;
        String title = masteryTitle(level);
        line(g, "mastery-title", title.isEmpty() ? "No title yet" : "Title: " + title, x, ry, title.isEmpty() ? DarkUi.SEPIA : DarkUi.INK_TEXT);
        ry += STEP;
        var next = level >= top ? null : Spec.MASTERY[level];
        line(g, "mastery-next", next == null ? "Mastery complete" : "Next, level " + next.level() + ": " + RecruitPages.masteryReward(next), x, ry, DarkUi.SEPIA);
        ry += STEP + 3;
        var m = next == null ? Spec.MASTERY[top - 1] : next;
        int kills = o == null ? 0 : o.kills, fights = o == null ? 0 : o.survived;
        bar(g, "mastery-kills", "Kills", Math.min(kills, m.kills()), m.kills(), x, ry);
        ry += 13;
        bar(g, "mastery-fights", "Fights survived", Math.min(fights, m.matches()), m.matches(), x, ry);
        ry += 22;
        // the three levels in battle
        head(g, "levels", "Levels in battle", null, x, ry);
        ry += 18;
        var names = RecruitPages.levelNames(t);
        var lines = RecruitPages.abilityLines(t);
        for (int i = 0; i < 3; i++) {
            String roman = MatchStats.roman(i + 1);
            g.drawText(font, heading(roman), x, ry, DarkUi.INK_TEXT, false);
            boolean named = i == 0 || !names.get(i).equals(names.get(i - 1));
            if (named) {
                String n = UiMath.trim(names.get(i), TEXT_W - 30, s -> font.getWidth(heading(s)));
                g.drawText(font, heading(n), x + 30, ry, DarkUi.INK_TEXT, false);
                rectsDrawn.put("level-" + i + "-name", new int[]{x + 30, ry, font.getWidth(heading(n)), 11});
                textsDrawn.put("level-" + i + "-name", n);
                colorsDrawn.put("level-" + i + "-name", DarkUi.INK_TEXT);
                ry += 13;
            }
            String text = lines.get(i).isEmpty() ? "No change at this level." : lines.get(i);
            ry = block(g, "level-" + i, text, x + 30, ry, TEXT_W - 30, 3, DarkUi.INK_TEXT) + 4;
        }
    }

    /** The title of the highest Mastery level reached (the title: part of mastery.reward), or "". */
    static String masteryTitle(int level) {
        String title = "";
        for (var m : Spec.MASTERY) if (m.level() <= level) for (String part : m.reward().split(";")) if (part.startsWith("title:")) title = part.substring(6);
        return title;
    }

    /** A progress row: the word, a track and its fill to have of want, and the count "60 of 150" after it. */
    void bar(DrawContext g, String key, String word, int have, int want, int x, int y) {
        var font = font();
        int bx = x + 112, bw = 150;
        g.drawText(font, DarkUi.body(word), x, y, DarkUi.INK_TEXT, false);
        g.fill(bx, y + 2, bx + bw, y + 8, DarkUi.SEPIA_LINE);
        int fill = want <= 0 ? bw : (int) Math.min(bw, (long) bw * have / want);
        g.fill(bx, y + 2, bx + fill, y + 8, DarkUi.INK_TEXT);
        g.drawBorder(bx - 1, y + 1, bw + 2, 8, DarkUi.SEPIA);
        String count = have + " of " + want;
        g.drawText(font, DarkUi.body(count), bx + bw + 8, y, DarkUi.INK_TEXT, false);
        rectsDrawn.put(key, new int[]{x, y, bx + bw + 8 + font.getWidth(DarkUi.body(count)) - x, 11});
        textsDrawn.put(key, word + " " + count);
        colorsDrawn.put(key, DarkUi.INK_TEXT);
        rectsDrawn.put(key + "-bar", new int[]{bx, y + 2, bw, 6});
        textsDrawn.put(key + "-fill", String.valueOf(fill));
    }

    // The Matchups face -----------------------------------------------------------------------------------------------
    void matchups(DrawContext g, Spec.TroopsRow t, int x, int y) {
        int ry = y + HEAD_Y;
        head(g, "matchups", "Matchups", null, x, ry);
        ry += 18;
        var notes = ArmyCards.notes(t);
        int label = Math.max(font().getWidth(DarkUi.body("Beats")), font().getWidth(DarkUi.body("Beaten by"))) + 10;
        for (String[] kv : new String[][]{{"Beats", notes == null ? "" : notes.beats(), "beats"}, {"Beaten by", notes == null ? "" : notes.beatenBy(), "beaten-by"}}) {
            line(g, null, kv[0], x, ry, DarkUi.SEPIA);
            ry = block(g, kv[2], kv[1].isEmpty() ? "Nothing in particular." : kv[1], x + label, ry, TEXT_W - label, 2, DarkUi.INK_TEXT) + 3;
        }
        ry += 5;
        head(g, "damage", "Damage rules", null, x, ry);
        ry += 18;
        var rows = ArmyCards.counterRows(t);
        if (rows.isEmpty()) line(g, "counter-none", "No damage rules apply.", x, ry, DarkUi.SEPIA);
        for (var r : rows) {
            boolean good = ArmyCards.favours(r);
            String text = ArmyCards.counterText(r);
            ArmyCards.swords(g, x, ry + 1, good ? DarkUi.INK_TEXT : DarkUi.ROUT_TEXT, !good);
            textsDrawn.put("glyph-" + r.id(), good ? "favour" : "against");
            countersDrawn.add(new CounterDraw(r.id(), good, text));
            ry = block(g, "counter-" + r.id(), text, x + 14, ry, TEXT_W - 14, 2, good ? DarkUi.INK_TEXT : DarkUi.ROUT_TEXT) + 1;
        }
    }

    // The notice ------------------------------------------------------------------------------------------------------
    /** The server's notice (a refusal; a collection that could not be saved) under the sheet's content, in the ink, on two lines when one is too short; shown WarTableHome.NOTICE_TICKS, then faded. */
    void notice(DrawContext g, int x, int y) {
        long age = ticks - noticeAt;
        if (noticeText.isEmpty() || age >= WarTableHome.NOTICE_TICKS + WarTableHome.NOTICE_FADE_TICKS) return;
        var font = font();
        float fade = age < WarTableHome.NOTICE_TICKS ? 1 : 1 - (age - WarTableHome.NOTICE_TICKS) / (float) WarTableHome.NOTICE_FADE_TICKS;
        var lines = UiMath.lines(HintText.expand(noticeText, GravesAndGloryClient::hint), TEXT_W, 2, s -> font.getWidth(DarkUi.body(s)));
        if (lines.isEmpty()) return;
        int alpha = Math.max(4, Math.round(fade * 255)), ink = (alpha << 24) | (DarkUi.INK_TEXT & 0xffffff);
        int top = y + NOTICE_Y, right = x;
        for (int i = 0; i < lines.size(); i++) {
            g.drawText(font, DarkUi.body(lines.get(i)), x, top + STEP * i, ink, false);
            right = Math.max(right, x + font.getWidth(DarkUi.body(lines.get(i))));
        }
        rectsDrawn.put("notice", new int[]{x, top, right - x, STEP * (lines.size() - 1) + 11});
        textsDrawn.put("notice", String.join(" ", lines));
        colorsDrawn.put("notice", ink);
    }
}
