package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The War Table battle report (plan 4F.2; CampScreen page results): the last battle on two papers, on the same 3x canvas page as
 *  the troop sheet and the lobby. On the left the orders note: the outcome, the map and how long the battle ran, the section of the
 *  mode that was played (ReportSections: the mode's name and rule line, then its rows), what the battle paid, and the tags that go
 *  on (Contracts, Chapter, and Back to camp, or Return to room when a private room is open). On the right the sheet with three
 *  faces: Battle (the commanders table, one row per army, your team first, and a row for each House of the banner), Troops (what
 *  each troop type reached, and the gear its army bought) and Chapter (the Founders tier bar and the contracts). The rail's back
 *  plaque (Esc) returns to the War Table.
 *
 *  A mode's section is a function registered in ReportSections under the mode's id; this class draws whatever rows it gives, so a
 *  new mode adds a section without a change here. Everything is drawn in canvas pixels; page coordinates (the 960x540 page) are
 *  offset by WarTable.column(COMPACT). Words the checks read are kept as the troop sheet keeps its own. */
final class WarTableReport {
    /** The sheet and the note on the 960 page: the pair stands centred, 24 apart, the note first (the lobby's papers, mirrored). */
    static final int NOTE_X = 150, SHEET_X = 406, TOP = 124, NOTE_W = 232, SHEET_W = 404, PAPER_H = 373;
    /** The compact page (WarTable.compact): both papers rise 32 and close up 53, as the lobby's do. */
    static final int[] COMPACT = {-53, -32};
    /** On the sheet (sheet-relative): the text's left edge and width, the tags' row, the first section's head, the last row the content may use. */
    static final int PAD = 22, TEXT_W = 360, TAB_Y = 14, TAB_W = 88, TAB_H = 24, TAB_GAP = 8, HEAD_Y = 48, CONTENT_END = 340, STEP = 12;
    /** On the note (note-relative): the text's left edge and width, then the tops of the outcome, the map line, the mode head and its rule line, the section's rows, the rewards and the tags. */
    static final int NPAD = 18, NOTE_TEXT_W = 196, OUTCOME_Y = 22, MAP_Y = 44, MODE_Y = 70, RULE_Y = 88, ROWS_Y = 116, ROW_STEP = 13, REWARDS_Y = 234,
        TAGS_Y = 304, TAG_W = 96, TAG_GAP = 4, GO_Y = 336, GO_H = 24;
    /** The commanders table: the rows' step, the gap where the team changes, and the most armies a battle seats. */
    static final int TABLE_STEP = 13, TEAM_GAP = 4;
    static final String[] FACES = {"battle", "troops", "chapter"};
    static final String[] FACE_LABELS = {"Battle", "Troops", "Chapter"};
    static final String NONE = "No battle to report yet.";

    /** A commander's row the last frame drew: the name as shown, the team and whether it is the viewer's and the MVP's. */
    record RowDraw(String name, int team, boolean you, boolean mvp) {}

    // What the last frame drew, refilled every frame and read by the checks through the views below.
    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> textsDrawn = new LinkedHashMap<>();
    private static final Map<String, Integer> colorsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> texturesDrawn = new LinkedHashMap<>();
    private static final List<ReportSections.Row> sectionDrawn = new ArrayList<>();
    private static final List<RowDraw> commandersDrawn = new ArrayList<>();
    private static final Set<String> underlinedDrawn = new LinkedHashSet<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String, String> texts = Collections.unmodifiableMap(textsDrawn);
    private static final Map<String, Integer> colors = Collections.unmodifiableMap(colorsDrawn);
    private static final Map<String, String> textures = Collections.unmodifiableMap(texturesDrawn);
    private static final List<ReportSections.Row> section = Collections.unmodifiableList(sectionDrawn);
    private static final List<RowDraw> commanders = Collections.unmodifiableList(commandersDrawn);
    private static final Set<String> underlined = Collections.unmodifiableSet(underlinedDrawn);

    /** Rects {x,y,w,h} in canvas px, as drawn: table and rail (WarTable.drawChrome), note, sheet (the papers), tab-(face), contracts,
     *  chapter and go (the widgets' boxes), and the blocks of the words: outcome, map, head-(key), rule, section-row-(0..), rewards,
     *  rewards-note, header, commander-(0..), house-(0..), level-(0..), tier, tier-bar, next, contract-(0..), weekly. */
    static Map<String, int[]> rects() { return rects; }
    /** Words drawn, keyed as rects are, plus face (the face shown), mode (the mode's id), and the widgets' labels (contracts, chapter, go). */
    static Map<String, String> texts() { return texts; }
    static Map<String, Integer> colors() { return colors; }
    /** The texture each part was drawn from: table, note, sheet and the tabs. */
    static Map<String, String> textures() { return textures; }
    /** The rows of the mode's section as the last frame drew them. */
    static List<ReportSections.Row> section() { return section; }
    /** The commanders table's rows as the last frame drew them, in order. */
    static List<RowDraw> commanders() { return commanders; }
    /** The widgets the last frame drew with their label underlined (hot: under the pointer, or holding the keyboard's focus). */
    static Set<String> underlined() { return underlined; }

    final CampScreen screen;
    String face = "battle";
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    long ticks;
    /** The Troops face's page (0-based). */
    int troopPage;
    int builtContracts, builtFriends;

    WarTableReport(CampScreen screen) { this.screen = screen; }

    static int[] columnAt() { return WarTable.column(COMPACT[0], COMPACT[1]); }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    static Text heading(String s) { return DarkUi.heading(s); }

    static JsonObject report() { return CampaignClient.result; }

    static boolean has() { return report().has("id"); }

    UiButton button(String name) { return buttons.get(name); }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    /** Where the primary tag goes: the private room's page while a room is open, the War Table otherwise. */
    String leaveTarget() { return screen.value("room").isEmpty() ? "home" : "friends"; }

    String leaveLabel() { return screen.value("room").isEmpty() ? "Back to camp" : "Return to room"; }

    /** Builds the widgets (CampScreen.init on page results): again whenever the camp state or a rail badge changes. */
    void init() {
        buttons.clear();
        builtContracts = WarTableHome.contractsBadge();
        builtFriends = WarTableHome.friendsBadge();
        add("back", WarTable.backPlaque(b -> back()));
        for (int i = 0; i < WarTableHome.RAIL.length; i++) {
            final String target = WarTableHome.RAIL[i];
            int badge = target.equals("contracts") ? builtContracts : target.equals("friends") ? builtFriends : 0;
            add(target, WarTable.plaque(WarTableHome.RAIL_X[i], WarTableHome.RAIL_W, WarTableHome.RAIL_ICONS[i], WarTableHome.RAIL_LABELS[i], null, 0, badge, b -> screen.page(target)));
        }
        int[] o = columnAt();
        int sx = o[0] + SHEET_X, sy = o[1] + TOP, nx = o[0] + NOTE_X, ny = o[1] + TOP;
        for (int i = 0; i < FACES.length; i++) {
            final String which = FACES[i], label = FACE_LABELS[i];
            add("tab-" + which, new UiButton(sx + PAD + i * (TAB_W + TAB_GAP), sy + TAB_Y, TAB_W, TAB_H, Text.literal(label), b -> face = which, (g, x, y, w, h, hot) -> paintTab(g, which, label, x, y, w, h)));
        }
        add("note-contracts", new UiButton(nx + NPAD, ny + TAGS_Y, TAG_W, TAB_H, Text.literal("Contracts"), b -> screen.page("contracts"), (g, x, y, w, h, hot) -> paintTag(g, "note-contracts", "Contracts", x, y, w, h, DarkUi.INK_TEXT)));
        add("note-chapter", new UiButton(nx + NPAD + TAG_W + TAG_GAP, ny + TAGS_Y, TAG_W, TAB_H, Text.literal("Chapter"), b -> screen.page("chapter"), (g, x, y, w, h, hot) -> paintTag(g, "note-chapter", "Chapter", x, y, w, h, DarkUi.INK_TEXT)));
        add("go", new UiButton(nx + NPAD, ny + GO_Y, NOTE_TEXT_W, GO_H, Text.literal(leaveLabel()), b -> go(), (g, x, y, w, h, hot) -> paintTag(g, "go", leaveLabel(), x, y, w, h, DarkUi.INK_TEXT)));
        // the Troops face's paging, shown only when the list is longer than a page (sheet)
        add("page-prev", new UiButton(sx + PAD, sy + CONTENT_END - TAB_H, TAG_W, TAB_H, Text.literal("Previous"), b -> turnPage(-1), (g, x, y, w, h, hot) -> paintTag(g, "page-prev", "Previous", x, y, w, h, DarkUi.INK_TEXT)));
        add("page-next", new UiButton(sx + PAD + TEXT_W - TAG_W, sy + CONTENT_END - TAB_H, TAG_W, TAB_H, Text.literal("Next"), b -> turnPage(1), (g, x, y, w, h, hot) -> paintTag(g, "page-next", "Next", x, y, w, h, DarkUi.INK_TEXT)));
        showPaging(false);
    }

    /** The troop rows the report holds (its levels array). */
    static int troopCount() { return report().has("levels") ? report().getAsJsonArray("levels").size() : 0; }

    void turnPage(int step) { troopPage = ReportSections.turnTroopPage(troopPage, step, troopCount()); }

    void showPaging(boolean on) {
        for (String k : new String[]{"page-prev", "page-next"}) { var b = buttons.get(k); if (b != null) { b.visible = on; b.active = on; } }
    }

    void back() { UiSounds.back(); screen.page("home"); }

    void go() { screen.page(leaveTarget()); }

    void tick() {
        ticks++;
        if (WarTableHome.contractsBadge() != builtContracts || WarTableHome.friendsBadge() != builtFriends) screen.rebuild();
    }

    /** A key's press: Esc returns to the War Table; Enter or Space presses the widget the keyboard's focus is on, and Enter with none presses the primary tag. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { back(); return true; }
        if (face.equals("troops") && ReportSections.troopPages(troopCount()) > 1 && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) { turnPage(key == GLFW.GLFW_KEY_LEFT ? -1 : 1); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            var target = keyboardFocus();
            if (target == null && key != GLFW.GLFW_KEY_SPACE) target = buttons.get("go");
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
        rectsDrawn.clear(); textsDrawn.clear(); colorsDrawn.clear(); texturesDrawn.clear(); sectionDrawn.clear(); commandersDrawn.clear(); underlinedDrawn.clear();
        UiCanvas.begin(g);
        try {
            int[] o = columnAt();
            WarTable.drawChrome(g, "results");
            rectsDrawn.putAll(WarTable.drawn());
            texturesDrawn.put("table", WarTable.drawnTable());
            note(g, o[0] + NOTE_X, o[1] + TOP);
            sheet(g, o[0] + SHEET_X, o[1] + TOP);
        } finally {
            UiCanvas.end(g);
        }
    }

    // ---- the tags ----
    /** A face tag of the sheet: a vellum tag with the word; the face shown has a heavy bar under its word (a shape, not only an ink), the other a thin line while hot. */
    void paintTab(DrawContext g, String which, String label, int x, int y, int w, int h) {
        var font = font();
        boolean on = face.equals(which);
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

    /** A tag of the note: a vellum plate (the 88-wide one stretched to the tag) with its word centred; underlined while hot. */
    void paintTag(DrawContext g, String name, String label, int x, int y, int w, int h, int ink) {
        var font = font();
        String plate = "table/btn-vellum-88x24";
        UiTex.nine(g, plate, x, y, w + 1, h + 2);
        var t = heading(label);
        int tw = font.getWidth(t), lx = x + (w - tw) / 2;
        g.drawText(font, t, lx, y + 6, ink, false);
        if (WarTableRecruit.hot(buttons.get(name))) underline(g, name, lx, y + 18, tw, ink);
        rectsDrawn.put(name, new int[]{x, y, w, h});
        texturesDrawn.put(name, plate);
        textsDrawn.put(name, label);
        colorsDrawn.put(name, ink);
    }

    // ---- drawing helpers ----
    /** A section's head: its word in the heading font and a rule across w, and the line that goes with it on the right (null for none). */
    void head(DrawContext g, String key, String word, String right, int x, int y, int w) {
        var font = font();
        g.drawText(font, heading(word), x, y, DarkUi.INK_TEXT, false);
        g.fill(x, y + 12, x + w, y + 13, DarkUi.SEPIA_LINE);
        rectsDrawn.put("head-" + key, new int[]{x, y, font.getWidth(heading(word)), 11});
        textsDrawn.put("head-" + key, word);
        if (right != null) {
            int rw = font.getWidth(DarkUi.body(right));
            g.drawText(font, DarkUi.body(right), x + w - rw, y + 1, DarkUi.SEPIA, false);
            rectsDrawn.put(key, new int[]{x + w - rw, y + 1, rw, 11});
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

    /** One line ending at right. */
    int lineRight(DrawContext g, String key, String s, int right, int y, int color) {
        int w = font().getWidth(DarkUi.body(s));
        line(g, key, s, right - w, y, color);
        return w;
    }

    /** Words wrapped to w in at most max lines of the body font, STEP apart; recorded under key as one block; returns the y under the last line. */
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

    /** A centred focal line (the outcome); trimmed to w; recorded under key. */
    void focal(DrawContext g, String key, String s, int cx, int y, int w, int color) {
        var font = font();
        String shown = UiMath.trim(s, w, v -> font.getWidth(DarkUi.focal(v)));
        int x = WarTableHome.centred(g, DarkUi.focal(shown), cx, y, color);
        rectsDrawn.put(key, new int[]{x, y, font.getWidth(DarkUi.focal(shown)), 11});
        textsDrawn.put(key, shown);
        colorsDrawn.put(key, color);
    }

    // ---- the note ----
    static String clock(int seconds) { return (seconds / 60) + ":" + String.format(Locale.ROOT, "%02d", seconds % 60); }

    void note(DrawContext g, int x, int y) {
        var font = font();
        rectsDrawn.put("note", UiTex.draw(g, "table/note-room", x, y));
        texturesDrawn.put("note", "table/note-room");
        int lx = x + NPAD, cx = x + NOTE_W / 2;
        if (!has()) {
            WarTableHome.centred(g, DarkUi.body(NONE), cx, y + OUTCOME_Y, DarkUi.SEPIA);
            textsDrawn.put("outcome", NONE);
            return;
        }
        var r = report();
        focal(g, "outcome", r.get("outcome").getAsString(), cx, y + OUTCOME_Y, NOTE_TEXT_W, DarkUi.INK_TEXT);
        String map = r.get("map").getAsString() + ", " + clock((int) Math.round(r.get("seconds").getAsDouble()));
        map = UiMath.trim(map, NOTE_TEXT_W, s -> font.getWidth(DarkUi.body(s)));
        int mx = WarTableHome.centred(g, DarkUi.body(map), cx, y + MAP_Y, DarkUi.SEPIA);
        rectsDrawn.put("map", new int[]{mx, y + MAP_Y, font.getWidth(DarkUi.body(map)), 11});
        textsDrawn.put("map", map);
        colorsDrawn.put("map", DarkUi.SEPIA);
        // the mode's section
        head(g, "mode", ReportSections.title(r), null, lx, y + MODE_Y, NOTE_TEXT_W);
        textsDrawn.put("mode", ReportSections.id(r));
        String rule = ReportSections.rule(r);
        if (!rule.isEmpty()) block(g, "rule", rule, lx, y + RULE_Y, NOTE_TEXT_W, 2, DarkUi.SEPIA);
        int ry = y + ROWS_Y;
        var rows = ReportSections.rows(r);
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            sectionDrawn.add(row);
            String value = row.value();
            int vw = font.getWidth(DarkUi.body(value));
            String label = UiMath.trim(row.label(), NOTE_TEXT_W - vw - 8, s -> font.getWidth(DarkUi.body(s)));
            line(g, "section-label-" + i, label, lx, ry, DarkUi.SEPIA);
            lineRight(g, "section-value-" + i, value, lx + NOTE_TEXT_W, ry, DarkUi.INK_TEXT);
            rectsDrawn.put("section-row-" + i, new int[]{lx, ry, NOTE_TEXT_W, 11});
            ry += ROW_STEP;
        }
        // what the battle paid
        boolean saved = r.get("saved").getAsBoolean();
        head(g, "rewards", "Rewards", null, lx, y + REWARDS_Y, NOTE_TEXT_W);
        int wy = y + REWARDS_Y + 18;
        if (saved) {
            line(g, "rewards", "+" + r.get("gold").getAsInt() + " gold   +" + r.get("xp").getAsInt() + " chapter XP", lx, wy, DarkUi.INK_TEXT);
            if (r.has("march")) march(g, r, lx, wy + STEP + 2, r.get("eligible").getAsBoolean());
            else block(g, "rewards-note", r.get("eligible").getAsBoolean() ? "Rewards saved. Contract and tier rewards are claimed separately." : "No match rewards: play at least 3 minutes to qualify.", lx, wy + STEP + 2, NOTE_TEXT_W, 3, DarkUi.SEPIA);
        } else {
            line(g, "rewards", "Rewards not saved", lx, wy, DarkUi.ROUT_TEXT);
            block(g, "rewards-note", "Progress unchanged. Check disk space before playing again.", lx, wy + STEP + 2, NOTE_TEXT_W, 3, DarkUi.ROUT_TEXT);
        }
    }

    /** A Long March battle's own rewards under the gold line (plan 2C.5), one row each for the bonus and the Pact, in the report's label
     *  and value rows: "Bonus kept" with what it paid, "Bonus missed", "Bonus claimed before" or nothing after a loss; "A Pact earned"
     *  (held N) when the battle cleared a chapter, "Pacts held" when it did not. A battle under the 3-minute floor says so in the room left. */
    void march(DrawContext g, JsonObject r, int x, int y, boolean eligible) {
        var m = r.getAsJsonObject("march");
        String state = m.get("bonusState").getAsString();
        int pacts = m.get("pacts").getAsInt(), held = m.get("writs").getAsInt();
        if (!state.equals("none") && !state.equals("lost")) {
            String label = switch (state) { case "paid" -> "Bonus kept"; case "claimed" -> "Bonus claimed before"; default -> "Bonus missed"; };
            String value = state.equals("paid") ? "+" + m.get("bonusShards").getAsInt() + " shards, +" + m.get("bonusGold").getAsInt() + " gold" : "";
            y = row(g, "march-bonus", label, value, x, y, state.equals("paid") ? DarkUi.INK_TEXT : DarkUi.SEPIA);
        }
        if (pacts > 0) y = row(g, "march-pact", pacts == 1 ? "A Pact earned" : pacts + " Pacts earned", "held " + held, x, y, DarkUi.INK_TEXT);
        else if (held > 0) y = row(g, "march-pact", "Pacts held", String.valueOf(held), x, y, DarkUi.SEPIA);
        if (!eligible) line(g, "rewards-note", "No gold: under 3 minutes.", x, y + 1, DarkUi.SEPIA);
    }

    /** One label and value row of the note (the label trimmed to leave the value its room); returns the y under it. */
    int row(DrawContext g, String key, String label, String value, int x, int y, int color) {
        var font = font();
        int vw = value.isEmpty() ? 0 : font.getWidth(DarkUi.body(value)) + 8;
        String shown = UiMath.trim(label, NOTE_TEXT_W - vw, s -> font.getWidth(DarkUi.body(s)));
        line(g, key, shown, x, y, color);
        if (!value.isEmpty()) lineRight(g, key + "-value", value, x + NOTE_TEXT_W, y, color);
        return y + ROW_STEP;
    }

    // ---- the sheet ----
    void sheet(DrawContext g, int x, int y) {
        rectsDrawn.put("sheet", UiTex.draw(g, "table/sheet-banner", x, y));
        texturesDrawn.put("sheet", "table/sheet-banner");
        textsDrawn.put("face", face);
        showPaging(has() && face.equals("troops") && ReportSections.troopPages(troopCount()) > 1);
        if (!has()) return;
        switch (face) {
            case "troops" -> troops(g, x + PAD, y);
            case "chapter" -> chapter(g, x + PAD, y);
            default -> battle(g, x + PAD, y);
        }
    }

    // The Battle face -------------------------------------------------------------------------------------------------
    void battle(DrawContext g, int x, int y) {
        var font = font();
        var r = report();
        boolean points = ReportSections.points(r);
        JsonArray squads = r.has("squads") ? r.getAsJsonArray("squads") : new JsonArray();
        int ry = y + HEAD_Y;
        head(g, "commanders", "Commanders", squads.size() + (squads.size() == 1 ? " army" : " armies"), x, ry, TEXT_W);
        ry += 18;
        String[] heads = points ? new String[]{"Kills", "Lost", "Gold", "Points"} : new String[]{"Kills", "Lost", "Gold"};
        String[] keys = points ? new String[]{"kills", "lost", "gold", "objective"} : new String[]{"kills", "lost", "gold"};
        int[] right = points ? new int[]{x + 216, x + 260, x + 312, x + TEXT_W} : new int[]{x + 264, x + 312, x + TEXT_W};
        int nameRight = right[0] - font.getWidth(DarkUi.body("Kills")) - 14;
        line(g, "header", "Commander", x + 22, ry, DarkUi.SEPIA);
        for (int c = 0; c < heads.length; c++) lineRight(g, "header-" + keys[c], heads[c], right[c], ry, DarkUi.SEPIA);
        g.fill(x, ry + 12, x + TEXT_W, ry + 13, DarkUi.SEPIA_LINE);
        ry += 17;
        List<MatchStats.SquadStats> tally = new ArrayList<>();
        for (var e : squads) {
            var s = new MatchStats.SquadStats();
            s.kills = e.getAsJsonObject().get("kills").getAsInt();
            s.objectiveSeconds = e.getAsJsonObject().get("objective").getAsInt();
            tally.add(s);
        }
        int mvp = MatchStats.mvp(tally);
        for (int i = 0; i < squads.size(); i++) {
            var row = squads.get(i).getAsJsonObject();
            int team = row.get("team").getAsInt();
            boolean you = row.get("you").getAsBoolean();
            if (i > 0 && team != squads.get(i - 1).getAsJsonObject().get("team").getAsInt()) {
                g.fill(x + 14, ry, x + TEXT_W, ry + 1, DarkUi.SEPIA_LINE);
                ry += TEAM_GAP;
            }
            int ink = you ? DarkUi.INK_TEXT : DarkUi.SEPIA;
            if (you) g.fill(x, ry - 1, x + 2, ry + 10, DarkUi.INK_TEXT); // a bar before the viewer's own row: a shape, not only an ink
            DarkUi.teamMark(g, team, x + 8, ry + 1);
            String tag = i == mvp ? "  MVP" : "";
            String name = row.get("name").getAsString();
            String shown = UiMath.trim(name + tag, nameRight - (x + 22), s -> font.getWidth(s));
            g.drawText(font, shown, x + 22, ry, ink, false); // a commander's name is a player's: Minecraft's face, as the party's seats are
            rectsDrawn.put("commander-" + i, new int[]{x + 22, ry, font.getWidth(shown), 11});
            textsDrawn.put("commander-" + i, shown);
            colorsDrawn.put("commander-" + i, ink);
            commandersDrawn.add(new RowDraw(shown, team, you, i == mvp));
            for (int c = 0; c < keys.length; c++) lineRight(g, "commander-" + i + "-" + keys[c], row.get(keys[c]).getAsInt() + (c == 3 ? "s" : ""), right[c], ry, ink);
            ry += TABLE_STEP;
        }
        ry = story(g, r, x, ry);
        // the Houses of the banner
        JsonArray houses = r.has("houses") ? r.getAsJsonArray("houses") : new JsonArray();
        if (houses.isEmpty()) return;
        ry += 10;
        head(g, "houses", "Houses", null, x, ry, TEXT_W);
        ry += 18;
        for (int i = 0; i < houses.size(); i++) {
            if (ry + 16 > y + CONTENT_END) break; // a campaign story above can leave room for fewer House rows; never draw past the sheet
            ry = houseRow(g, i, houses.get(i).getAsJsonObject(), x, ry);
        }
    }

    /** A campaign battle's story under the commanders (chapter 1 brief N2): the after line of a win, then the chapter's end line the first
     *  time its last battle is won; {key:name} reads the player's own keys. Returns the y under it (unchanged when there is none). */
    int story(DrawContext g, JsonObject r, int x, int y) {
        var m = r.has("march") ? r.getAsJsonObject("march") : null;
        String after = m != null && m.has("after") ? HintText.expand(m.get("after").getAsString(), GravesAndGloryClient::hint) : "";
        String end = m != null && m.has("chapterEnd") ? HintText.expand(m.get("chapterEnd").getAsString(), GravesAndGloryClient::hint) : "";
        if (after.isEmpty() && end.isEmpty()) return y;
        y += 10;
        head(g, "story", "After the battle", null, x, y, TEXT_W);
        y += 18;
        if (!after.isEmpty()) y = block(g, "story-after", after, x, y, TEXT_W, 3, DarkUi.INK_TEXT) + 4;
        if (!end.isEmpty()) y = block(g, "story-end", end, x, y, TEXT_W, 3, DarkUi.SEPIA) + 4;
        return y;
    }

    int houseRow(DrawContext g, int i, JsonObject h, int x, int y) {
        var font = font();
        Spec.HousesRow row = null;
        for (var s : Spec.HOUSES) if (s.id().equals(h.get("house").getAsString())) row = s;
        if (row == null) return y;
        g.drawTexture(new net.minecraft.util.Identifier(row.glyph()), x, y - 2, 0f, 0f, 16, 16, 16, 16);
        g.drawText(font, heading(row.name()), x + 22, y + 2, DarkUi.INK_TEXT, false);
        int tier = h.get("tier").getAsInt();
        String trait = tier > 0 ? "Trait " + MatchStats.roman(tier) + " held " + h.get("traitSeconds").getAsInt() + "s" : "No trait tier";
        String paid = "+" + h.get("shards").getAsInt() + " shards  +" + h.get("renown").getAsInt() + " Renown";
        int nameEnd = x + 22 + font.getWidth(heading(row.name())) + 10;
        int pw = font.getWidth(DarkUi.body(paid));
        String traitShown = UiMath.trim(trait, x + TEXT_W - pw - 10 - nameEnd, s -> font.getWidth(DarkUi.body(s)));
        line(g, "house-" + i, traitShown, nameEnd, y + 2, tier > 0 ? DarkUi.INK_TEXT : DarkUi.SEPIA);
        lineRight(g, "house-" + i + "-paid", paid, x + TEXT_W, y + 2, DarkUi.SEPIA);
        rectsDrawn.put("house-" + i + "-name", new int[]{x + 22, y + 2, font.getWidth(heading(row.name())), 11});
        textsDrawn.put("house-" + i + "-name", row.name());
        return y + 16;
    }

    // The Troops face -------------------------------------------------------------------------------------------------
    void troops(DrawContext g, int x, int y) {
        var font = font();
        var r = report();
        JsonArray levels = r.has("levels") ? r.getAsJsonArray("levels") : new JsonArray();
        int ry = y + HEAD_Y;
        head(g, "troops", "Troops in battle", null, x, ry, TEXT_W);
        ry += 18;
        if (levels.isEmpty()) { line(g, "levels-none", "No troop levels this battle.", x, ry, DarkUi.SEPIA); return; }
        // every row in the long form, whole: a long list pages rather than trimming a name
        var rows = new ArrayList<String[]>();
        for (int i = 0; i < levels.size(); i++) { var words = CampScreen.levelWords(levels.get(i).getAsJsonObject(), false); if (words != null) rows.add(words); }
        int pages = ReportSections.troopPages(rows.size());
        troopPage = Math.max(0, Math.min(troopPage, pages - 1));
        int from = troopPage * ReportSections.TROOPS_PER_PAGE, to = Math.min(rows.size(), from + ReportSections.TROOPS_PER_PAGE);
        for (int i = from; i < to; i++) {
            var words = rows.get(i);
            int cy = ry + (i - from) * STEP;
            int nw = line(g, "level-" + i + "-name", words[0], x, cy, DarkUi.INK_TEXT);
            line(g, "level-" + i, words[1], x + nw + 8, cy, DarkUi.SEPIA);
        }
        if (pages > 1) {
            String label = "Page " + (troopPage + 1) + " of " + pages;
            int lw = font.getWidth(DarkUi.body(label));
            line(g, "page", label, x + (TEXT_W - lw) / 2, y + CONTENT_END - TAB_H + 7, DarkUi.SEPIA);
        }
    }

    // The Chapter face ------------------------------------------------------------------------------------------------
    void chapter(DrawContext g, int x, int y) {
        var p = CampaignClient.profile.progression;
        int tier = Progression.tier(p), xp = Math.min(p.xp, 7500);
        int ry = y + HEAD_Y;
        head(g, "founders", "Founders", "Tier " + tier + " of 30, " + xp + " XP", x, ry, TEXT_W);
        ry += 20;
        int fill = tier == 30 ? TEXT_W : (int) (TEXT_W * ((xp % 250) / 250f));
        g.fill(x, ry, x + TEXT_W, ry + 6, DarkUi.SEPIA_LINE);
        g.fill(x, ry, x + fill, ry + 6, DarkUi.INK_TEXT);
        g.drawBorder(x - 1, ry - 1, TEXT_W + 2, 8, DarkUi.SEPIA);
        rectsDrawn.put("tier-bar", new int[]{x, ry, TEXT_W, 6});
        textsDrawn.put("tier-fill", String.valueOf(fill));
        ry += 14;
        String next = tier == 30 ? "Chapter complete. Troops and titles stay yours." : "Next, tier " + (tier + 1) + ": " + Progression.reward(tier + 1) + ". " + (250 - xp % 250) + " XP to go.";
        ry = block(g, "next", next, x, ry, TEXT_W, 2, DarkUi.INK_TEXT) + 14;
        head(g, "contracts-list", "Contracts", null, x, ry, TEXT_W);
        ry += 18;
        for (int i = 0; i < 3; i++) {
            int have = Math.min(Progression.progress(p, i), Progression.target(p, i)), want = Progression.target(p, i);
            ry = contract(g, "contract-" + i, Progression.contract(p, i), have + "/" + want, p.claims.contains("daily:" + i) ? "Claimed" : have >= want ? "Ready to claim" : "", x, ry);
        }
        int weekly = Math.min(p.weeklyBattles, Progression.WEEKLY_BATTLES);
        ry = contract(g, "weekly", "Weekly battles", weekly + "/" + Progression.WEEKLY_BATTLES, p.claims.contains("weekly") ? "Claimed" : Progression.claimable(p, "weekly") ? "Ready to claim" : "", x, ry);
        longMarch(g, x, ry + 8);
    }

    /** The Long March under the contracts (plan 2C.5, the Pact report): battles won of all, the bonuses claimed, the next battle and the
     *  Pacts held. A Pact comes with every cleared chapter; the words are the camp notice's. */
    void longMarch(DrawContext g, int x, int y) {
        var prof = CampaignClient.profile;
        var next = MarchMath.nextUnwon(prof.battlesWon, Spec.CAMPAIGNBATTLES);
        var rows = MarchMath.byOrder(Spec.CAMPAIGNBATTLES);
        String name = MarchMath.chapterName(next != null ? next : rows.isEmpty() ? null : rows.get(rows.size() - 1), Spec.CAMPAIGNCHAPTERS);
        var chapterRows = MarchMath.inChapter(next != null ? next.chapter() : rows.isEmpty() ? 1 : rows.get(rows.size() - 1).chapter(), Spec.CAMPAIGNBATTLES);
        int total = chapterRows.size(), won = (int) chapterRows.stream().filter(r -> prof.battlesWon.contains(r.id())).count(); // brief G3: counted within the chapter
        head(g, "march", name, won + " of " + total + " won", x, y, TEXT_W);
        y += 18;
        y = block(g, "march-next", next == null ? "Every battle of " + name + " is won." : "Next, battle " + MarchMath.number(next, Spec.CAMPAIGNBATTLES) + ": " + next.name() + ".", x, y, TEXT_W, 2, DarkUi.INK_TEXT);
        y = block(g, "march-bonuses", "Bonuses claimed: " + prof.bonusClaimed.size() + " of " + Spec.BONUSOBJECTIVES.length + ".", x, y, TEXT_W, 1, DarkUi.SEPIA);
        String pact = prof.writs > 0 ? "Pacts held: " + prof.writs + "." : "You hold no Pact. Clear every battle of a campaign chapter to earn one.";
        block(g, "march-pacts", pact, x, y, TEXT_W, 2, prof.writs > 0 ? DarkUi.INK_TEXT : DarkUi.SEPIA);
    }

    /** One contract: its name on the left, its count and (when it has one) its state on the right; a claimed one in sepia, a ready one in the ink. */
    int contract(DrawContext g, String key, String name, String count, String state, int x, int y) {
        var font = font();
        String right = state.isEmpty() ? count : count + "  " + state;
        int rw = font.getWidth(DarkUi.body(right));
        String label = UiMath.trim(name, TEXT_W - rw - 12, s -> font.getWidth(DarkUi.body(s)));
        boolean ready = state.equals("Ready to claim");
        line(g, key, label, x, y, state.equals("Claimed") ? DarkUi.SEPIA : DarkUi.INK_TEXT);
        lineRight(g, key + "-state", right, x + TEXT_W, y, ready ? DarkUi.INK_TEXT : DarkUi.SEPIA);
        if (ready) g.fill(x + TEXT_W - rw, y + 10, x + TEXT_W, y + 11, DarkUi.INK_TEXT); // a line under the claim that is waiting: a shape as well as an ink
        return y + STEP + 2;
    }
}
