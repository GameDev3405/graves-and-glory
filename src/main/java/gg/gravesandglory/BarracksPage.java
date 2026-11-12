package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import java.util.*;
import static gg.gravesandglory.GravesAndGloryClient.*;

/**
 * The barracks page of the barracks screen (RecruitmentScreen tab 0), in the War Table's dress as the Armoury and the recruit page wear it
 * (Kort, 2026-11-11: the mid-match training screen was old and generic). Canvas pixels at UiMath.pageScale on the 960x540 page, each column offset by
 * WarTable.column on the compact page, as WarTableRecruit lays its own out:
 * the muster roll on the left lists the banner's troop types (portrait, name, fielded of listed, price), a click picks one; the letter in the
 * middle shows the picked troop's live model, what it is, its stats and ability and why it can or cannot be trained; the ladder on the right holds
 * the training queue on a note, the purse and the army's slots, and the three plates Recruit, Disband and Cancel training.
 * Client UI only: every plate sends the same commands the old screen sent (train:, disband:, cancel-training), which the server checks again.
 */
final class BarracksPage {
    /** The roll's rows: shown at most ROWS; past that the last slot holds the scroll arrows. */
    static final int ROWS = WarTableRecruit.ROWS, ROW_H = WarTableRecruit.ROW_H, ARROW = 24;
    /** The ladder (from its column's origin): the training note, the purse, the army line, the three plates, all on WarTableRecruit.AXIS. */
    static final int QUEUE_Y = 124, PURSE_Y = 234, ARMY_Y = 264, RECRUIT_Y = 284, DISBAND_Y = 332, CANCEL_Y = 380, PLATE_W = 240, PLATE_H = 40;
    /** The letter's lines (from the letter's top), centred on its middle (letter x + 152). */
    static final int NAME_Y = 224, KIND_Y = 244, STATS_Y = 262, ABILITY_Y = 282, FIELDED_Y = 312, REASON_Y = 330, LINE = 11;
    private static final String[] UP = {"...x...", "..xxx..", ".xxxxx.", "xxxxxxx"}, DOWN = {"xxxxxxx", ".xxxxx.", "..xxx..", "...x..."};

    final RecruitmentScreen screen;
    /** The troop rows built: indexes into Spec.TROOPS, the first one shown, how many show. */
    List<Integer> listed = List.of(); int first, shown; boolean scrolling;
    final List<UiButton> rows = new ArrayList<>();
    UiButton up, down;
    /** What the last frame drew (the checks read them): each row's box by troop index, the letter's lines, the queue note's lines, the reason. */
    final Map<Integer, int[]> rowBoxes = new LinkedHashMap<>();
    List<String> letterDrawn = List.of(), queueDrawn = List.of(); String reasonDrawn = "";
    int[] queueBox, purseBox;

    BarracksPage(RecruitmentScreen screen) { this.screen = screen; }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }
    static int[] rollAt() { return WarTableRecruit.rollAt(); }
    static int[] letterAt() { return WarTableRecruit.letterAt(); }
    static int[] ladderAt() { return WarTableRecruit.ladderAt(); }

    /** Builds the rows and the plates; the screen's init calls it, and a scroll or a new banner builds it again. */
    void init() {
        listed = RecruitmentScreen.bannerTroops();
        rows.clear(); rowBoxes.clear(); up = down = null;
        if (!listed.contains(screen.selected)) screen.selected = listed.isEmpty() ? -1 : listed.get(0);
        scrolling = listed.size() > ROWS;
        shown = scrolling ? ROWS - 1 : listed.size();
        first = Math.max(0, Math.min(first, listed.size() - shown));
        int[] ro = rollAt(), la = ladderAt();
        int rx = ro[0] + WarTableRecruit.ROLL_X, top = ro[1] + WarTableRecruit.ROW_TOP;
        for (int r = 0; r < shown; r++) {
            int index = listed.get(first + r);
            var b = screen.add(new UiButton(rx, top + ROW_H * r, WarTableRecruit.ROLL_W, ROW_H, Text.literal(Spec.TROOPS[index].name()), x -> pick(index), (g, x, y, w, h, hot) -> {}));
            rows.add(b); rowBoxes.put(index, b.canvasBox());
        }
        if (scrolling) {
            int ay = top + ROW_H * shown + (ROW_H - ARROW) / 2, ax = rx + WarTableRecruit.ROLL_W - 16 - ARROW;
            up = screen.add(new UiButton(ax - ARROW - 4, ay, ARROW, ARROW, Text.literal("Earlier troops"), x -> scroll(-1), (g, x, y, w, h, hot) -> arrow(g, true, x, y, w, h, hot)));
            down = screen.add(new UiButton(ax, ay, ARROW, ARROW, Text.literal("Later troops"), x -> scroll(1), (g, x, y, w, h, hot) -> arrow(g, false, x, y, w, h, hot)));
        }
        int px = la[0] + WarTableRecruit.AXIS - PLATE_W / 2;
        screen.recruit = screen.add(new UiButton(px, la[1] + RECRUIT_Y, PLATE_W, PLATE_H, Text.literal("Recruit"), x -> { var c = screen.chosen(); if (c != null && c.train().active) c.train().onPress(); }, this::paintRecruit));
        screen.disband = screen.add(new UiButton(px, la[1] + DISBAND_Y, PLATE_W, PLATE_H, Text.literal("Disband one"), x -> { var c = screen.chosen(); if (c != null && c.disband().active) c.disband().onPress(); }, this::paintDisband));
        screen.cancel = screen.add(new UiButton(px, la[1] + CANCEL_Y, PLATE_W, PLATE_H, Text.literal("Cancel training"), x -> send("cancel-training"), this::paintCancel));
    }

    void pick(int index) { if (screen.selected != index) { screen.selected = index; } }

    /** Moves the pick one row up or down the banner, scrolling the roll to keep it in sight. */
    void step(int dir) {
        if (listed.isEmpty()) return;
        int at = Math.max(0, listed.indexOf(screen.selected)), next = Math.max(0, Math.min(listed.size() - 1, at + dir));
        screen.selected = listed.get(next);
        if (next < first) { first = next; screen.rebuild(); } else if (next >= first + shown) { first = next - shown + 1; screen.rebuild(); }
    }

    /** Scrolls the roll by rows; true when it scrolls at all (so the wheel is not passed on). */
    boolean scroll(int by) {
        if (!scrolling) return false;
        int next = Math.max(0, Math.min(first + by, listed.size() - shown));
        if (next != first) { first = next; screen.rebuild(); }
        return true;
    }

    /** A new banner (or the first state packet) changes the rows: the page is built again. */
    void tick() { if (!RecruitmentScreen.bannerTroops().equals(listed)) screen.rebuild(); }

    // ---- drawing (canvas pixels, between UiCanvas.begin and end) ----

    static Text heading(String s) { return DarkUi.heading(s); }
    static Text body(String s) { return DarkUi.body(s); }
    static int bodyW(String s) { return font().getWidth(body(s)); }

    void render(DrawContext g) {
        WarTable.drawChrome(g, "recruit");
        int[] ro = rollAt(), lo = letterAt(), la = ladderAt();
        roll(g, ro[0] + WarTableRecruit.ROLL_X, ro[1] + WarTableRecruit.ROLL_Y, ro[1] + WarTableRecruit.ROW_TOP);
        letter(g, lo[0] + WarTableRecruit.LETTER_X, lo[1] + WarTableRecruit.LETTER_Y);
        ladder(g, la[0], la[1]);
    }

    void roll(DrawContext g, int x, int y, int top) {
        var font = font();
        UiTex.draw(g, "recruit/muster-roll", x, y);
        // the roll's head on the table above it, as the ladder's lines sit on the wood: what the roll is and how many types it lists
        g.drawText(font, heading("Troops"), x + 8, y - 18, DarkUi.IVORY, true);
        String types = listed.size() + (listed.size() == 1 ? " type in your banner" : " types in your banner");
        g.drawText(font, body(types), x + WarTableRecruit.ROLL_W - 8 - bodyW(types), y - 17, DarkUi.IVORY, true);
        int sel = listed.indexOf(screen.selected) - first;
        if (sel >= 0 && sel < shown) UiTex.draw(g, "recruit/row-mark", x, top + ROW_H * sel);
        for (int i = 1; i < shown + (scrolling ? 1 : 0); i++) if (i != sel) UiTex.draw(g, "recruit/rule-208", x + 16, top + ROW_H * i);
        for (int i = 0; i < shown; i++) row(g, listed.get(first + i), x, top + ROW_H * i, i != sel && WarTableRecruit.hot(rows.get(i)));
        if (listed.isEmpty()) {
            WarTableHome.centred(g, body("Your banner has no troops."), x + WarTableRecruit.ROLL_W / 2, top + 20, DarkUi.SEPIA);
        } else if (!scrolling) { // the roll's spare room says where the list comes from and what a pick does
            int fy = top + ROW_H * shown + 14, cx = x + WarTableRecruit.ROLL_W / 2;
            g.fill(x + 40, fy - 6, x + WarTableRecruit.ROLL_W - 40, fy - 5, DarkUi.SEPIA_LINE);
            WarTableHome.centred(g, body("Pick a troop to train or disband."), cx, fy + 2, DarkUi.SEPIA);
            WarTableHome.centred(g, body("Your banner sets this list:"), cx, fy + 2 + LINE + 4, DarkUi.SEPIA);
            WarTableHome.centred(g, body("change it in the war council."), cx, fy + 2 + 2 * LINE + 4, DarkUi.SEPIA);
        }
        if (scrolling) {
            String span = "Troops " + (first + 1) + "-" + (first + shown) + " of " + listed.size();
            g.drawText(font, body(span), x + 16, top + ROW_H * shown + (ROW_H - 8) / 2, DarkUi.SEPIA, false);
        }
    }

    /** One troop row: the live portrait in its frame, the name, what is fielded and training against what the banner lists, the price. */
    void row(DrawContext g, int index, int x, int y, boolean hot) {
        var font = font(); var t = Spec.TROOPS[index];
        UiTex.draw(g, "table/frame-back-48", x + 8, y + 8);
        var e = ArmyCards.portrait(t);
        if (e != null) { var c = MinecraftClient.getInstance(); e.age = c.player == null ? 0 : c.player.age; Portrait.draw(g, e, x + 8, y + 8, 40, 0); }
        UiTex.draw(g, "table/frame-rim-48", x + 4, y + 4);
        String price = String.valueOf(RecruitmentScreen.price(t));
        int pw = bodyW(price), px = x + WarTableRecruit.ROLL_W - 12 - pw;
        String name = UiMath.trim(t.name(), px - 16 - (x + 60), s -> font.getWidth(heading(s)));
        g.drawText(font, heading(name), x + 60, y + 12, DarkUi.INK_TEXT, false);
        if (hot) g.fill(x + 60, y + 24, x + 60 + font.getWidth(heading(name)), y + 25, DarkUi.INK_TEXT);
        g.drawText(font, body(price), px, y + 13, DarkUi.INK_TEXT, false);
        UiTex.draw(g, "recruit/coin-m", px - 10, y + 13);
        String count = roster[index] + " of " + RecruitmentScreen.listed(t) + " fielded" + (pending[index] > 0 ? ", " + pending[index] + " training" : "");
        g.drawText(font, body(UiMath.trim(count, WarTableRecruit.ROLL_W - 76, BarracksPage::bodyW)), x + 60, y + 30, DarkUi.SEPIA, false);
    }

    void arrow(DrawContext g, boolean upward, int x, int y, int w, int h, boolean hot) {
        boolean live = upward ? first > 0 : first < listed.size() - shown;
        if (live) UiTex.nine(g, "table/btn-vellum-64x24", x, y, w + 1, h + 2);
        else { g.fill(x, y, x + w, y + h, DarkUi.VELLUM); g.drawBorder(x, y, w, h, DarkUi.SEPIA_LINE); }
        if (hot) g.drawBorder(x, y, w, h, DarkUi.SEPIA);
        ArmouryTab.glyph(g, upward ? UP : DOWN, x + (w - 7) / 2, y + (h - 4) / 2, live ? DarkUi.INK_TEXT : DarkUi.SEPIA_LINE);
    }

    /** The open letter: the picked troop's live model in the ornate frame, its name, what it is, its stats and ability, what is fielded, and the reason. */
    void letter(DrawContext g, int x, int y) {
        var font = font();
        UiTex.draw(g, "recruit/letter-hi", x, y);
        List<String> drawn = new ArrayList<>();
        if (screen.selected < 0) { letterDrawn = drawn; reasonDrawn = ""; return; }
        var t = Spec.TROOPS[screen.selected]; int cx = x + 152, lw = WarTableRecruit.LINE_W;
        UiTex.draw(g, "table/frame-back-160", x + 72, y + 40);
        var e = ArmyCards.portrait(t);
        if (e != null) { var c = MinecraftClient.getInstance(); e.age = c.player == null ? 0 : c.player.age; Portrait.draw(g, e, x + 72, y + 40, 160, 0); }
        UiTex.draw(g, "table/frame-ornate-184", x + 60, y + 28);
        String name = UiMath.trim(t.name(), lw, s -> font.getWidth(DarkUi.focal(s)));
        WarTableHome.centred(g, DarkUi.focal(name), cx, y + NAME_Y, DarkUi.INK_TEXT); drawn.add(name);
        String kind = Morale.label(t.group()) + ", " + t.slotWeight() + (t.slotWeight() == 1 ? " army slot" : " army slots");
        WarTableHome.centred(g, body(kind), cx, y + KIND_Y, DarkUi.SEPIA); drawn.add(kind);
        String stats = "Health " + t.health() + "    Damage " + t.damage();
        WarTableHome.centred(g, body(stats), cx, y + STATS_Y, DarkUi.INK_TEXT); drawn.add(stats);
        for (int i = 0; i < Math.min(2, UiMath.lines(ability(t), lw, 2, BarracksPage::bodyW).size()); i++) {
            String line = UiMath.lines(ability(t), lw, 2, BarracksPage::bodyW).get(i);
            WarTableHome.centred(g, body(line), cx, y + ABILITY_Y + LINE * i, DarkUi.SEPIA); drawn.add(line);
        }
        int i = screen.selected;
        String fielded = roster[i] + " of " + RecruitmentScreen.listed(t) + " fielded" + (pending[i] > 0 ? ", " + pending[i] + " in training" : "");
        WarTableHome.centred(g, body(fielded), cx, y + FIELDED_Y, DarkUi.INK_TEXT); drawn.add(fielded);
        String reason = screen.unavailable(t); boolean can = RecruitmentScreen.trainable(t);
        var lines = UiMath.lines(reason, lw, 2, BarracksPage::bodyW);
        for (int k = 0; k < lines.size(); k++) WarTableHome.centred(g, body(lines.get(k)), cx, y + REASON_Y + LINE * k, can ? DarkUi.SEPIA : DarkUi.INK_TEXT); // the words say which; the ink only stresses it, as the recruit letter does
        letterDrawn = drawn; reasonDrawn = reason;
    }

    /** The troop's own ability text from the sheet, as its sheet page shows it; else what kind of fighter it is. */
    static String ability(Spec.TroopsRow t) {
        String own = RecruitPages.abilityLines(t).get(0);
        if (!own.isEmpty()) return own;
        return switch (Abilities.row(t).mechanic()) {
            case "shield" -> "Shield: frontline protection";
            case "mend" -> "Mend: restores allied health";
            case "blight" -> "Blight: offensive magic";
            default -> t.group().equals("ranged") ? "Ranged: attacks from a distance" : "Melee: frontline fighter";
        };
    }

    /** The ladder: the training note, the purse and the army's slots, all centred on the axis (the plates are widgets). */
    void ladder(DrawContext g, int ox, int oy) {
        var font = font(); int axis = ox + WarTableRecruit.AXIS;
        var note = UiTex.entry("table/note-contract");
        int nx = axis - note.w() / 2, ny = oy + QUEUE_Y, tx = nx + 14, tw = note.w() - 28;
        UiTex.draw(g, "table/note-contract", nx, ny);
        queueBox = new int[]{nx, ny, note.w(), note.h()};
        List<String> q = new ArrayList<>();
        g.drawText(font, heading("Training"), tx, ny + 12, DarkUi.INK_TEXT, false); q.add("Training");
        String count = queued > 0 ? String.valueOf(queued) : "";
        if (!count.isEmpty()) g.drawText(font, body(count + " queued"), tx + tw - bodyW(count + " queued"), ny + 13, DarkUi.INK_TEXT, false);
        g.fill(tx, ny + 25, tx + tw, ny + 26, DarkUi.SEPIA_LINE);
        List<String> lines = new ArrayList<>();
        if (queued > 0) {
            lines.add("Next joins in " + trainingSeconds + (trainingSeconds == 1 ? " second" : " seconds"));
            List<String> who = new ArrayList<>();
            for (int i = 0; i < pending.length && i < Spec.TROOPS.length; i++) if (pending[i] > 0) who.add(Spec.TROOPS[i].name() + " x" + pending[i]);
            lines.addAll(UiMath.lines(String.join(", ", who), tw, 2, BarracksPage::bodyW));
        } else lines.add("No one in training");
        lines.add("Replacements join their group.");
        int ly = ny + 32;
        for (int k = 0; k < lines.size() && ly + 9 <= ny + note.h() - 8; k++, ly += LINE) {
            String s = UiMath.trim(lines.get(k), tw, BarracksPage::bodyW);
            g.drawText(font, body(s), tx, ly, k == lines.size() - 1 ? DarkUi.SEPIA : DarkUi.INK_TEXT, false); q.add(s);
        }
        queueDrawn = q;
        String purse = gold + " gold"; // as the Armoury writes it
        int pw = 10 + font.getWidth(heading(purse)), px = axis - (pw + 1) / 2, py = oy + PURSE_Y + 8;
        UiTex.draw(g, "recruit/coin-m", px, py + 2);
        g.drawText(font, heading(purse), px + 10, py, DarkUi.IVORY, true);
        purseBox = new int[]{px, py, pw, 12};
        String army = "Army " + RecruitmentScreen.weight(false) + " of " + GravesAndGlory.R.squadSize() + " slots" + (queued > 0 ? ", " + (RecruitmentScreen.weight(true) - RecruitmentScreen.weight(false)) + " queued" : "");
        int aw = bodyW(army);
        g.drawText(font, body(army), axis - aw / 2, oy + ARMY_Y, DarkUi.IVORY, true);
    }

    /** A plate in its state: brass (the main action open), iron (a secondary action open) or dull (shut); the word, an underline when hot, a right-hand note. */
    void plate(DrawContext g, net.minecraft.client.gui.widget.ButtonWidget b, String art, String label, String key, String right, int rightInk, int x, int y) {
        plate(g, b, art, label, key, right, rightInk, x, y, -1, 16);
    }
    /** As above, with the word's ink given (labelInk, or -1 for the plate's own) and where the room for it starts (left, from the plate's edge).
     *  The word is centred in the room between the key chip or icon and the right-hand note, so the two never meet on a narrow page. */
    void plate(DrawContext g, net.minecraft.client.gui.widget.ButtonWidget b, String art, String label, String key, String right, int rightInk, int x, int y, int labelInk, int left) {
        var font = font(); boolean live = b.active;
        UiTex.draw(g, live ? art : "table/btn-dull-240x40", x, y);
        int ink = !live ? DarkUi.MUTED : labelInk != -1 ? labelInk : art.contains("brass") ? DarkUi.PLATE_INK : DarkUi.IVORY;
        if (key != null && live) {
            UiTex.draw(g, "table/key-brass-37", x + 14, y + 13);
            g.drawText(font, key, x + 18, y + 16, DarkUi.KEY_INK, false);
        }
        int from = key != null && live ? 56 : left, to = right != null && !right.isEmpty() ? 224 - font.getWidth(right) - 8 : 224;
        int lx = WarTableHome.centred(g, heading(label), x + (from + to) / 2, y + 14, ink);
        if (b instanceof UiButton u && WarTableRecruit.hot(u)) g.fill(lx, y + 26, lx + font.getWidth(heading(label)), y + 27, ink);
        if (right != null && !right.isEmpty()) g.drawText(font, right, x + 224 - font.getWidth(right), y + 16, rightInk, false);
    }

    /** Why Recruit is shut, in the plate's few words (the letter gives the whole reason). */
    String shortReason(Spec.TroopsRow t) {
        int i = screen.selected, listed = RecruitmentScreen.listed(t);
        if (!nearBarracks) return "Not here";
        if (listed == 0) return "Not in banner";
        if (roster[i] + pending[i] >= listed) return "All fielded";
        if (RecruitmentScreen.weight(true) + t.slotWeight() > GravesAndGlory.R.squadSize()) return "Army full";
        if (gold < RecruitmentScreen.price(t)) return "Need " + RecruitmentScreen.price(t);
        return null;
    }

    void paintRecruit(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var t = screen.selected < 0 ? null : Spec.TROOPS[screen.selected];
        String price = t == null ? "" : String.valueOf(RecruitmentScreen.price(t));
        boolean live = screen.recruit.active;
        String why = t == null || live ? null : shortReason(t);
        plate(g, screen.recruit, "table/btn-brass-240x40", "Recruit", "Enter", live ? price : why, live ? DarkUi.PLATE_INK : DarkUi.ALARM_TEXT, x, y);
        if (live) UiTex.draw(g, "recruit/coin-m", x + 224 - font().getWidth(price) - 10, y + 16); // 3 px before the price, as Enlist has it
    }

    void paintDisband(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        boolean live = screen.disband.active;
        // the destructive plate: its word in red and the broken seal at its left, so it never reads as Cancel training below it
        plate(g, screen.disband, "table/btn-iron-240x40", "Disband one", null, live ? "No refund" : null, DarkUi.MUTED, x, y, DarkUi.ALARM_TEXT, live ? 40 : 16);
        if (live) { var c = g.getMatrices(); c.push(); c.translate(x + 12, y + 7, 0); c.scale(0.75f, 0.75f, 1); UiTex.draw(g, "recruit/wax-broken", 0, 0); c.pop(); }
    }

    void paintCancel(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        plate(g, screen.cancel, "table/btn-iron-240x40", "Cancel training", null, screen.cancel.active ? "Full refund" : null, DarkUi.MUTED, x, y);
    }
}
