package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.*;

/**
 * The match ending's stats and rewards drawing (spec 5, 9, 10), shared by the ending's own screen (EndingScreen, STATS and REWARDS) and
 * the War Table report (WarTableReport: the Battle face's commanders table, the You face, the note's rewards). Everything is drawn on
 * parchment in the coordinates the caller is in (canvas pixels on the War Table; sheet pixels on EndingScreen), from the report JSON
 * Campaign builds: squads (score, ck, cd, downs, kept, mvp), you (commander, army, objectives, economy), rewards, claimable and the mode
 * block. Every word is fitted to the room it is given (UiMath.trim and lines), so nothing is drawn past its panel at any window size.
 * What the last frame drew is kept for the checks (rects, texts, scoreRows, groups, rewardRows, total, claim, layout).
 */
final class EndingPanels {
    private EndingPanels() {}

    /** Parchment inks: the report's ink and sepia, the mockup's brown for headings and amounts, the amount still counting, a row's highlight and a box's rule. */
    static final int INK = DarkUi.INK_TEXT, SEPIA = DarkUi.SEPIA, BROWN = 0xff5a3b17, COUNTING = 0xff9b6a1d, YOU_FILL = 0x80f4e7c4, BOX = 0xffa88a5c,
        RULE = 0xff8a6a3f, CLAIM_FILL = 0xff3a2c21, CLAIM_INK = 0xffe8c26a, CLAIM_TEXT = 0xffe9dcbf, MVP_FILL = 0xffe8c26a, AWARD_FILL = 0xffd9c497;
    /** The commanders table's row step and the gap where the team changes; a group's key-value step; a reward row with and without its reason line. */
    static final int ROW = 13, TEAM_GAP = 5, KV = 12, REWARD_TALL = 22, REWARD_SHORT = 13, CLAIM_H = 18, COLUMN_ROW = 12;
    /** The share of the rewards step the rows take to appear and count up; the total and the claim strip follow. */
    static final float ROWS_END = .6f;
    /** Pass 2: a bar's row height, the share of the step one bar takes to fill once its reward row has counted up, and how long a crossing flashes. */
    static final int BAR_ROW = 14;
    static final float BAR_FILL = .14f, FLASH = .09f;
    static final int BAR_TRACK = 0xffd8c79f, BAR_INK = 0xff9b6a1d, BAR_DONE = 0xffc8902a, FLASH_INK = 0xfffff4c8;

    // ---- what the last frame drew (the checks read these) ----
    static final Map<String, int[]> rects = new LinkedHashMap<>();
    static final Map<String, String> texts = new LinkedHashMap<>();
    /** Each panel's own box, keyed by its prefix (score, group, troops, reward). */
    static final Map<String, int[]> areas = new LinkedHashMap<>();
    static final List<String> scoreRows = new ArrayList<>(), groups = new ArrayList<>();
    static int rewardRows, rewardsTotal;
    /** Pass 2, the REWARDS bars as last drawn: how many, each one's fill (0..1 of its own climb), the thresholds crossed so far and the flash words shown. */
    static int barsDrawn, barsCrossed;
    static final List<Float> barFill = new ArrayList<>();
    static final Set<String> flashes = new LinkedHashSet<>();
    /** Whether the last rewards drawn were the note's two columns (the checks read it). */
    static boolean columns;
    static String total = "", claim = "";

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }
    static Text body(String s) { return DarkUi.body(s); }
    static Text small(String s) { return DarkUi.small(s); }
    static Text heading(String s) { return DarkUi.heading(s); }
    static int width(Text t) { return font().getWidth(t); }

    static void reset(String prefix) {
        rects.keySet().removeIf(k -> k.startsWith(prefix + "-"));
        texts.keySet().removeIf(k -> k.startsWith(prefix + "-"));
        areas.remove(prefix);
    }
    static void record(String key, String text, int x, int y, int w, int h) { rects.put(key, new int[]{x, y, w, h}); texts.put(key, text); }

    /** Words in the body font at (x, y), recorded under key; returns the width. */
    static int put(DrawContext g, String key, Text t, int x, int y, int color) {
        g.drawText(font(), t, x, y, color, false);
        int w = width(t);
        record(key, t.getString(), x, y, w, 10);
        return w;
    }
    static int right(DrawContext g, String key, Text t, int right, int y, int color) { return put(g, key, t, right - width(t), y, color); }

    // ---- reading the report ----
    static int num(JsonObject o, String k) {
        if (o == null || !o.has(k)) return 0;
        try { return o.get(k).getAsInt(); } catch (RuntimeException e) { return 0; }
    }
    static String str(JsonObject o, String k) {
        if (o == null || !o.has(k)) return "";
        try { return o.get(k).getAsString(); } catch (RuntimeException e) { return ""; }
    }
    static JsonObject obj(JsonObject o, String k) { return o != null && o.has(k) && o.get(k).isJsonObject() ? o.getAsJsonObject(k) : null; }
    static JsonArray arr(JsonObject o, String k) { return o != null && o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray(); }
    static String clock(int seconds) { return (seconds / 60) + ":" + String.format(Locale.ROOT, "%02d", seconds % 60); }
    static String cap(String s) { return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1); }
    static String troopName(String id) { for (var t : Spec.TROOPS) if (t.id().equals(id)) return t.name(); return cap(id.replace('_', ' ')); }

    /** A squad row's match score: the report's, or worked out from the columns a report from before the ending carries. */
    static int score(JsonObject row) {
        if (row.has("score")) return num(row, "score");
        var e = new MatchEnding.Entry("", num(row, "team"), false, 0, num(row, "kills"), num(row, "downs"), num(row, "objective"), num(row, "kept"), num(row, "lost"));
        return MatchEnding.score(e, Spec.ENDING[0]);
    }

    // ---- the commanders table ----
    /** Every commander of both teams in the report's order (yours first), with the gap where the team changes, your row lit and a bar
     *  before it, and the MVP chip; columns Score, K / D, Army kills, Lost and On points (Objective in a mode without points). Returns the y under it. */
    static int scoreboard(DrawContext g, JsonObject r, int x, int y, int w) {
        reset("score"); scoreRows.clear();
        var font = font();
        JsonArray squads = arr(r, "squads");
        String[] heads = {UiText.t("ending.score.headScore", "Score"), UiText.t("ending.score.headKd", "K / D"), UiText.t("ending.score.headArmyKills", "Army kills"), UiText.t("ending.score.headLost", "Lost"),
            ReportSections.points(r) ? UiText.t("ending.score.headPoints", "On points") : UiText.t("ending.score.headObjective", "Objective")};
        int gap = 10, edge = x + w;
        int[] right = new int[heads.length];
        for (int c = heads.length - 1; c >= 0; c--) { right[c] = edge; edge -= Math.max(width(small(heads[c])), c == 1 ? width(body("00 / 00")) : width(body("0,000"))) + gap; }
        int nameX = x + 22, nameRight = edge + gap - 6;
        put(g, "score-head", small(UiText.t("ending.score.headCommander", "Commander")), nameX, y + 1, SEPIA);
        for (int c = 0; c < heads.length; c++) right(g, "score-head-" + c, small(heads[c]), right[c], y + 1, SEPIA);
        g.fill(x, y + 11, x + w, y + 12, DarkUi.SEPIA_LINE);
        int ry = y + 16;
        String mvpWord = UiText.t("ending.score.mvp", "MVP");
        // the MVP: the report's mark, or (a report from before the ending) MatchStats' pick
        int mvp = -1;
        boolean marked = false;
        for (int i = 0; i < squads.size(); i++) { var o = squads.get(i).getAsJsonObject(); if (o.has("mvp")) { marked = true; if (o.get("mvp").getAsBoolean()) mvp = i; } }
        if (!marked) {
            List<MatchStats.SquadStats> tally = new ArrayList<>();
            for (var e : squads) { var s = new MatchStats.SquadStats(); s.kills = num(e.getAsJsonObject(), "kills"); s.objectiveSeconds = num(e.getAsJsonObject(), "objective"); tally.add(s); }
            mvp = MatchStats.mvp(tally);
        }
        for (int i = 0; i < squads.size(); i++) {
            var row = squads.get(i).getAsJsonObject();
            int team = num(row, "team");
            boolean you = row.has("you") && row.get("you").getAsBoolean();
            if (i > 0 && team != num(squads.get(i - 1).getAsJsonObject(), "team")) { g.fill(x, ry - 2, x + w, ry - 1, RULE); ry += TEAM_GAP; }
            int ink = you ? INK : SEPIA;
            if (you) { g.fill(x, ry - 2, x + w, ry + 10, YOU_FILL); g.fill(x, ry - 2, x + 2, ry + 10, INK); }
            DarkUi.teamMark(g, team, x + 8, ry + 1);
            int nx = nameX;
            if (i == mvp) {
                int cw = width(small(mvpWord)) + 4;
                g.fill(nx, ry, nx + cw, ry + 9, MVP_FILL);
                g.drawText(font, small(mvpWord), nx + 2, ry + 1, INK, false);
                rects.put("score-mvp", new int[]{nx, ry, cw, 9});
                nx += cw + 4;
            }
            String name = EndingClient.display(str(row, "name"));
            // the award chip, or "Left the battle", after the name (ending polish): it gives way to the name when the room is short
            boolean gone = row.has("left") && row.get("left").getAsBoolean();
            String tag = gone ? EndingClient.leftLine() : str(row, "award");
            int room = nameRight - nx, nameW = Math.min(font.getWidth(name), Math.max(30, room / 2));
            if (!tag.isEmpty()) tag = UiMath.trim(tag, room - nameW - 10, s -> width(small(s)));
            if (tag.equals("...")) tag = "";
            int tagW = tag.isEmpty() ? 0 : width(small(tag)) + 4 + 4;
            String shown = UiMath.trim(name, room - tagW, s -> font.getWidth(s));
            g.drawText(font, shown, nx, ry + 1, ink, false); // a commander's name is a player's: Minecraft's face, as the party's seats are
            record("score-" + i, shown, nx, ry, font.getWidth(shown), 10);
            scoreRows.add(shown);
            if (!tag.isEmpty()) {
                int tx = nx + font.getWidth(shown) + 4, tw = width(small(tag));
                if (gone) put(g, "score-" + i + "-left", small(tag), tx, ry + 1, DarkUi.ROUT_TEXT);
                else { g.fill(tx, ry, tx + tw + 4, ry + 9, AWARD_FILL); put(g, "score-" + i + "-award", small(tag), tx + 2, ry + 1, INK); rects.put("score-" + i + "-award", new int[]{tx, ry, tw + 4, 9}); }
            }
            int ck = num(row, "ck"), army = Math.max(0, num(row, "kills") - ck);
            String[] cells = {UiMath.thousands(score(row)), row.has("cd") ? MatchEnding.kd(ck, num(row, "cd")) : "-", String.valueOf(army), String.valueOf(num(row, "lost")), clock(num(row, "objective"))};
            for (int c = 0; c < cells.length; c++) right(g, "score-" + i + "-" + c, body(cells[c]), right[c], ry, ink);
            ry += ROW;
        }
        areas.put("score", new int[]{x, y, w, ry - y});
        return ry;
    }

    // ---- the four groups ----
    record Kv(String label, String value) {}
    record Group(String title, List<Kv> rows, String foot) {}

    /** The four groups of the viewer's own battle (report "you"), as the mockup: Commander, Army, Objectives (the mode's own rows for
     *  Siege, Convoy and Survival) and Economy & orders. */
    static List<Group> groupsOf(JsonObject r) {
        var you = obj(r, "you");
        var cmd = obj(you, "commander"); var army = obj(you, "army"); var objv = obj(you, "objectives"); var eco = obj(you, "economy");
        int ck = num(cmd, "kills"), deaths = num(cmd, "deaths");
        List<Group> out = new ArrayList<>();
        out.add(new Group(UiText.t("ending.group.commander", "Commander"), List.of(new Kv(UiText.t("ending.group.commanderKills", "Kills"), String.valueOf(ck)), new Kv(UiText.t("ending.group.commanderDeaths", "Deaths"), String.valueOf(deaths)),
            new Kv(UiText.t("ending.group.commanderKd", "K/D"), MatchEnding.ratio(ck, deaths)), new Kv(UiText.t("ending.group.commanderDamage", "Damage"), UiMath.thousands(num(cmd, "damage")))), ""));
        int kept = num(army, "kept"), lost = num(army, "lost");
        String best = str(army, "best"), foot = "";
        if (!best.isEmpty()) {
            int kills = 0;
            for (var t : arr(army, "troops")) if (str(t.getAsJsonObject(), "id").equals(best)) kills = num(t.getAsJsonObject(), "kills");
            foot = kills == 1 ? UiText.t("ending.group.armyBestOne", "Best: {troop}, {kills} kill", "troop", troopName(best), "kills", kills) : UiText.t("ending.group.armyBestMany", "Best: {troop}, {kills} kills", "troop", troopName(best), "kills", kills);
        }
        out.add(new Group(UiText.t("ending.group.army", "Army"), List.of(new Kv(UiText.t("ending.group.armyKills", "Kills"), String.valueOf(Math.max(0, num(army, "kills") - ck))), new Kv(UiText.t("ending.group.armyLosses", "Losses"), String.valueOf(lost)),
            new Kv(UiText.t("ending.group.armyKept", "Kept alive"), kept + " / " + (kept + lost))), foot));
        List<Kv> o = new ArrayList<>();
        o.add(new Kv(UiText.t("ending.group.captures", "Captures"), String.valueOf(num(objv, "captures"))));
        o.add(new Kv(UiText.t("ending.group.holdTime", "Hold time"), clock(num(objv, "holdSeconds"))));
        String mode = ReportSections.id(r);
        if (mode.isEmpty() || mode.equals(Modes.DOMINATION)) {
            var m = ReportSections.mode(r);
            o.add(new Kv(UiText.t("ending.group.finalScore", "Final score"), ReportSections.number(m, "ledger", 0, 0) + " / " + ReportSections.number(m, "ledger", 1, 0)));
        } else for (var row : ReportSections.rows(r)) { if (o.size() >= 4) break; o.add(new Kv(row.label(), row.value())); }
        o.add(new Kv(UiText.t("ending.group.length", "Length"), clock((int) Math.round(r.has("seconds") ? r.get("seconds").getAsDouble() : 0))));
        out.add(new Group(UiText.t("ending.group.objectives", "Objectives"), o, ""));
        out.add(new Group(UiText.t("ending.group.economy", "Economy & orders"), List.of(new Kv(UiText.t("ending.group.goldEarned", "Gold earned"), UiMath.thousands(num(eco, "earned"))), new Kv(UiText.t("ending.group.goldSpent", "Gold spent"), UiMath.thousands(num(eco, "spent"))),
            new Kv(UiText.t("ending.group.trained", "Trained"), String.valueOf(num(eco, "trained"))), new Kv(UiText.t("ending.group.upgrades", "Upgrades"), String.valueOf(num(eco, "upgrades"))), new Kv(UiText.t("ending.group.orders", "Orders"), String.valueOf(num(eco, "orders")))), ""));
        return out;
    }

    /** The height one group needs at width gw: its title, its rows and its foot (up to two lines). */
    static int groupHeight(Group gr, int gw) {
        int foot = gr.foot().isEmpty() ? 0 : 2 + 11 * UiMath.lines(gr.foot(), gw - 10, 2, s -> width(body(s))).size();
        return 5 + 15 + gr.rows().size() * KV + foot + 3;
    }

    /** The four groups in a row when w holds them (440 and over), else two by two; each box no taller than its tallest group needs.
     *  A report without "you" (from before the ending) says so. Returns the y under the boxes. */
    static int groups(DrawContext g, JsonObject r, int x, int y, int w, int h) {
        reset("group"); groups.clear();
        if (obj(r, "you") == null) { put(g, "group-none", body(UiText.t("ending.group.none", "No stats for this battle.")), x, y, SEPIA); areas.put("group", new int[]{x, y, w, 12}); return y + 13; }
        var list = groupsOf(r);
        int cols = w >= 440 ? 4 : 2, lines = (list.size() + cols - 1) / cols, gap = 8;
        int gw = (w - (cols - 1) * gap) / cols, need = 0;
        for (var gr : list) need = Math.max(need, groupHeight(gr, gw));
        int gh = Math.min(need, (h - (lines - 1) * gap) / lines);
        var font = font();
        for (int i = 0; i < list.size(); i++) {
            var gr = list.get(i);
            int gx = x + (i % cols) * (gw + gap), gy = y + (i / cols) * (gh + gap), in = gw - 10;
            g.drawBorder(gx, gy, gw, gh, BOX);
            String title = UiMath.trim(gr.title(), in, s -> width(heading(s)));
            put(g, "group-" + i + "-title", heading(title), gx + 5, gy + 5, BROWN);
            groups.add(gr.title());
            int ry = gy + 20, bottom = gy + gh - 3;
            for (int k = 0; k < gr.rows().size(); k++) {
                if (ry + 10 > bottom) break;
                var kv = gr.rows().get(k);
                // pass 2 fit: a label and value too wide together ("Kept alive" and "14 / 22" in a narrow box) narrow the value to the small font first,
                // then the label, and trim only after that
                boolean smallValue = width(body(kv.label())) + width(body(kv.value())) + 6 > in;
                Text value = smallValue ? small(kv.value()) : body(kv.value());
                int vw = width(value);
                boolean smallLabel = width(body(kv.label())) + vw + 6 > in;
                String label = UiMath.trim(kv.label(), in - vw - 6, s -> width(smallLabel ? small(s) : body(s)));
                put(g, "group-" + i + "-" + k, smallLabel ? small(label) : body(label), gx + 5, ry + (smallLabel ? 1 : 0), SEPIA);
                right(g, "group-" + i + "-" + k + "-value", value, gx + gw - 5, ry + (smallValue ? 1 : 0), INK);
                ry += KV;
            }
            if (!gr.foot().isEmpty() && ry + 12 <= bottom) {
                var foot = UiMath.lines(gr.foot(), in, Math.max(1, Math.min(2, (bottom - ry - 2) / 11)), s -> width(body(s)));
                g.fill(gx + 3, ry, gx + gw - 3, ry + 2 + 11 * foot.size(), YOU_FILL);
                for (int k = 0; k < foot.size(); k++) put(g, "group-" + i + "-foot-" + k, body(foot.get(k)), gx + 5, ry + 2 + 11 * k, INK);
            }
            rects.put("group-" + i, new int[]{gx, gy, gw, gh});
        }
        int bottom = y + lines * gh + (lines - 1) * gap;
        areas.put("group", new int[]{x, y, w, bottom - y});
        return bottom;
    }

    /** The viewer's army by troop type (report you.army.troops), most kills first: Fielded, Kills, Lost; as many rows as h holds, then "and N more". Returns the y under it. */
    static int troops(DrawContext g, JsonObject r, int x, int y, int w, int h) {
        reset("troops");
        var army = obj(obj(r, "you"), "army");
        List<JsonObject> rows = new ArrayList<>();
        for (var e : arr(army, "troops")) rows.add(e.getAsJsonObject());
        rows.sort((a, b) -> num(b, "kills") != num(a, "kills") ? num(b, "kills") - num(a, "kills") : num(a, "lost") - num(b, "lost"));
        areas.put("troops", new int[]{x, y, w, h});
        if (rows.isEmpty()) { put(g, "troops-none", body(UiText.t("ending.troops.none", "No troops fielded.")), x, y, SEPIA); return y + 13; }
        String[] heads = {UiText.t("ending.troops.headFielded", "Fielded"), UiText.t("ending.troops.headKills", "Kills"), UiText.t("ending.troops.headLost", "Lost")};
        int[] right = {x + w - 96, x + w - 48, x + w};
        put(g, "troops-head", small(UiText.t("ending.troops.headTroop", "Troop")), x, y + 1, SEPIA);
        for (int c = 0; c < heads.length; c++) right(g, "troops-head-" + c, small(heads[c]), right[c], y + 1, SEPIA);
        g.fill(x, y + 11, x + w, y + 12, DarkUi.SEPIA_LINE);
        int ry = y + 15, room = (y + h - ry) / KV;
        int shownRows = rows.size() <= room ? rows.size() : room >= 2 ? room - 1 : Math.max(0, room); // one row's room shows the best troop, not just "and N more"
        for (int i = 0; i < shownRows; i++) {
            var t = rows.get(i);
            String name = UiMath.trim(troopName(str(t, "id")), right[0] - width(small(heads[0])) - 8 - x, s -> width(body(s)));
            put(g, "troops-" + i, body(name), x, ry, INK);
            int[] v = {num(t, "fielded"), num(t, "kills"), num(t, "lost")};
            for (int c = 0; c < v.length; c++) right(g, "troops-" + i + "-" + c, body(String.valueOf(v[c])), right[c], ry, SEPIA);
            ry += KV;
        }
        if (shownRows < rows.size() && ry + 10 <= y + h) { put(g, "troops-more", body(UiText.t("ending.troops.more", "and {count} more", "count", rows.size() - shownRows)), x, ry, SEPIA); ry += KV; }
        return ry;
    }

    /** Highlights (ending polish): the awards earned this match, the viewer's first, then in award order; up to three lines, each "Award: Name, detail"
     *  fitted to w. "No awards this battle." when none. Returns the y under it. */
    static int highlights(DrawContext g, JsonObject r, int x, int y, int w, int h) {
        reset("highlight");
        areas.put("highlight", new int[]{x, y, w, h});
        int[] mineCount = new int[1];
        List<String> lines = highlightLines(r, mineCount);
        if (lines.isEmpty()) { put(g, "highlight-none", body(UiText.t("ending.highlights.none", "No awards this battle.")), x, y, SEPIA); return y + 13; }
        int ry = y;
        for (int i = 0; i < lines.size() && i < 3 && ry + 10 <= y + h; i++) {
            Text t = fit(lines.get(i), w);
            put(g, "highlight-" + i, t, x, ry, i == 0 && mineCount[0] > 0 ? INK : SEPIA);
            ry += KV;
        }
        return ry;
    }
    /** The highlight lines in order: the viewer's award first ("You: Kingslayer, 2 commanders downed"), then the others' in award order. */
    static List<String> highlightLines(JsonObject r) { return highlightLines(r, new int[1]); }
    /** highlightLines, with mineCount[0] set to how many of the lines (counted before the cut to three) are the viewer's own, which come first. */
    static List<String> highlightLines(JsonObject r, int[] mineCount) {
        List<String> mine = new ArrayList<>(), others = new ArrayList<>();
        for (var a : EndingAwards.Award.values())
            for (var e : arr(r, "squads")) {
                var o = e.getAsJsonObject();
                if (!a.title.equals(str(o, "award"))) continue;
                String detail = str(o, "awardText");
                if (o.has("you") && o.get("you").getAsBoolean()) mine.add(detail.isEmpty() ? UiText.t("ending.highlights.youBare", "You: {award}", "award", a.title) : UiText.t("ending.highlights.you", "You: {award}, {detail}", "award", a.title, "detail", detail));
                else others.add(detail.isEmpty() ? UiText.t("ending.highlights.otherBare", "{award}: {name}", "award", a.title, "name", EndingClient.display(str(o, "name"))) : UiText.t("ending.highlights.other", "{award}: {name}, {detail}", "award", a.title, "name", EndingClient.display(str(o, "name")), "detail", detail));
            }
        mineCount[0] = mine.size();
        mine.addAll(others);
        return mine.size() > 3 ? new ArrayList<>(mine.subList(0, 3)) : mine;
    }

    // ---- the score graph (ending polish pass 3) ----
    /** The graph's shortest and tallest plot, and the shortest match that has one (seconds). */
    static final int GRAPH_MIN_PLOT = 22, GRAPH_MAX_PLOT = 56, GRAPH_MIN_SECONDS = 15;
    /** What the last graph drew (the checks read them): its samples per row (0 when none was drawn) and each row's final value as written. */
    static int graphSamples;
    static final List<String> graphFinals = new ArrayList<>();
    /** A team's line colour (its side's colour) and the darker ink its final value is written in on parchment. */
    static int teamColour(int team) { var f = DarkUi.side(team); return f == null ? SEPIA : 0xff000000 | f.color(); }
    static int teamInk(int team) {
        int c = teamColour(team), r = (c >> 16) & 255, gr = (c >> 8) & 255, b = c & 255;
        return 0xff000000 | (r * 3 / 5) << 16 | (gr * 3 / 5) << 8 | (b * 3 / 5);
    }
    /**
     * The report's timeline as a small two-line chart in the teams' colours (spec 5): the label, then each team's line from the first sample to the
     * last across the plot, its final value at the right edge. Points modes share one scale (the higher row's top); a mode without points scales each
     * line to its own max. Nothing when the match was shorter than GRAPH_MIN_SECONDS, there are fewer than two samples or h has no room for a plot.
     * Returns the y under it (y when nothing was drawn).
     */
    static int graph(DrawContext g, JsonObject r, int x, int y, int w, int h) {
        reset("graph"); graphSamples = 0; graphFinals.clear();
        var tl = obj(r, "timeline");
        if (tl == null || num(r, "seconds") < GRAPH_MIN_SECONDS) return y;
        var teams = arr(tl, "teams");
        if (teams.size() < 2) return y;
        int[][] rows = new int[2][];
        for (int t = 0; t < 2; t++) {
            try { rows[t] = MatchEnding.decodeRow(teams.get(t).getAsString()); } catch (RuntimeException e) { rows[t] = null; }
            if (rows[t] == null) return y;
        }
        int n = Math.min(rows[0].length, rows[1].length);
        int plotH = Math.min(GRAPH_MAX_PLOT, h - 14);
        if (n < 2 || plotH < GRAPH_MIN_PLOT) return y;
        var max = arr(tl, "max"); var format = arr(tl, "format");
        boolean own = max.size() >= 2;
        String[] finals = new String[2]; int[] top = new int[2];
        int shared = 1; for (int t = 0; t < 2; t++) for (int i = 0; i < n; i++) shared = Math.max(shared, rows[t][i]);
        for (int t = 0; t < 2; t++) {
            int mine = 1; for (int i = 0; i < n; i++) mine = Math.max(mine, rows[t][i]);
            top[t] = own ? Math.max(mine, max.get(t).getAsInt()) : shared;
            boolean pct = format.size() > t && "percent".equals(format.get(t).getAsString());
            finals[t] = UiMath.thousands(rows[t][n - 1]) + (pct ? "%" : "");
        }
        areas.put("graph", new int[]{x, y, w, 13 + plotH + 1});
        put(g, "graph-label", fit(str(tl, "label").isEmpty() ? UiText.t("ending.graph.label", "Score") : str(tl, "label"), w), x, y, SEPIA);
        int gutter = Math.max(width(small(finals[0])), width(small(finals[1]))) + 6;
        int px0 = x + 1, px1 = x + w - gutter, py0 = y + 13, py1 = py0 + plotH - 1;
        g.fill(x, py0, x + 1, py1 + 1, RULE); g.fill(x, py1, px1, py1 + 1, RULE); // the axes
        int half = py0 + (py1 - py0) / 2;
        for (int d = x + 2; d < px1; d += 4) g.fill(d, half, Math.min(px1, d + 2), half + 1, 0x60b79f73); // a faint dashed half line
        int[] endY = new int[2];
        int span = px1 - 1 - px0, lift = plotH - 3;
        for (int t = 1; t >= 0; t--) { // team 0 last, so it draws on top where they cross
            int colour = teamColour(t);
            int prevX = px0, prevY = py1 - 1 - Math.round(lift * rows[t][0] / (float) top[t]);
            for (int i = 1; i < n; i++) {
                int cx = px0 + Math.round(span * i / (float) (n - 1)), cy = py1 - 1 - Math.round(lift * rows[t][i] / (float) top[t]);
                for (int col = prevX; col < cx; col++) { // one column at a time, filled between its two neighbouring heights so the line has no gaps
                    int ya = Math.round(prevY + (cy - prevY) * (col - prevX) / (float) (cx - prevX));
                    int yb = Math.round(prevY + (cy - prevY) * (col + 1 - prevX) / (float) (cx - prevX));
                    g.fill(col, Math.min(ya, yb) - 1, col + 1, Math.max(ya, yb) + 1, colour);
                }
                prevX = cx; prevY = cy;
            }
            g.fill(prevX, prevY - 1, prevX + 1, prevY + 1, colour);
            endY[t] = prevY;
        }
        // the final values right of the plot, beside each line's end, kept 9 apart and inside the plot's height
        int lo = py0 - 2, hi = py1 - 7;
        int ya = Math.max(lo, Math.min(hi, endY[0] - 4)), yb = Math.max(lo, Math.min(hi, endY[1] - 4));
        if (Math.abs(ya - yb) < 9) {
            boolean firstUp = ya < yb || ya == yb && rows[0][n - 1] >= rows[1][n - 1];
            int upper = Math.max(lo, Math.min(hi - 9, Math.min(ya, yb)));
            if (firstUp) { ya = upper; yb = upper + 9; } else { yb = upper; ya = upper + 9; }
        }
        int[] ty = {ya, yb};
        for (int t = 0; t < 2; t++) { put(g, "graph-final-" + t, small(finals[t]), px1 + 4, ty[t], teamInk(t)); graphFinals.add(finals[t]); }
        graphSamples = n;
        return py1 + 2;
    }

    // ---- personal bests (ending polish pass 3) ----
    /** The report's bests as REWARDS words ("New best: 14 kills (was 11)"), at most 3, in the report's order. */
    static List<String> bestsOf(JsonObject r) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : arr(r, "bests")) {
            if (!e.isJsonObject() || out.size() >= 3) continue;
            var o = e.getAsJsonObject();
            out.add(MatchEnding.bestText(str(o, "key"), num(o, "value"), num(o, "previous")));
        }
        return out;
    }
    /** The bests as last drawn on REWARDS (the checks read them). */
    static final List<String> bestsShown = new ArrayList<>();

    // ---- the rewards ----
    record Reward(String label, int amount, String reason) {}
    static List<Reward> rewardsOf(JsonObject r) {
        List<Reward> out = new ArrayList<>();
        for (JsonElement e : arr(r, "rewards")) { var o = e.getAsJsonObject(); out.add(new Reward(str(o, "label"), num(o, "amount"), cap(str(o, "reason")))); }
        return out;
    }
    static boolean hasRewards(JsonObject r) { return r.has("rewards") && r.get("rewards").isJsonArray(); }
    /** "+120", or "" for a title (a row whose label carries the thing). */
    static String amountText(Reward w, int amount) { return w.label().startsWith("New title") ? "" : "+" + UiMath.thousands(amount); }

    /** The total, summed by kind: "1 writ · 250 gear shards · 250 XP · 24 shards · 10 Renown · 1 Pact · 1 new title". */
    static String totalText(List<Reward> rows) {
        Map<String, Integer> sum = new LinkedHashMap<>();
        for (var w : rows) {
            String l = w.label(), kind = l.equals("Troop writs") ? "writs" : l.equals("XP") ? "XP" : l.equals("Gear shards") ? "gear shards" : l.endsWith(" shards") ? "shards"
                : l.endsWith(" Renown") ? "Renown" : l.startsWith("Pact") ? "Pact" : l.startsWith("New title") ? "new title" : l;
            sum.merge(kind, l.startsWith("New title") ? 1 : w.amount(), Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        for (var e : sum.entrySet()) {
            String k = e.getKey(); int n = e.getValue();
            String word = switch (k) {
                case "writs" -> n == 1 ? UiText.t("ending.total.writ", "writ") : UiText.t("ending.total.writs", "writs");
                case "XP" -> UiText.t("ending.total.xp", "XP");
                case "gear shards" -> UiText.t("ending.total.gearShards", "gear shards");
                case "shards" -> UiText.t("ending.total.shards", "shards");
                case "Renown" -> UiText.t("ending.total.renown", "Renown");
                case "Pact" -> n != 1 ? UiText.t("ending.total.pacts", "Pacts") : UiText.t("ending.total.pact", "Pact");
                case "new title" -> n != 1 ? UiText.t("ending.total.newTitles", "new titles") : UiText.t("ending.total.newTitle", "new title");
                default -> k;
            };
            parts.add(UiMath.thousands(n) + " " + word);
        }
        return String.join(" · ", parts);
    }

    /** "Ready to claim: 2 contracts · 1 chapter tier", or "" when nothing waits. */
    static String claimText(JsonObject r) {
        var c = obj(r, "claimable");
        int contracts = num(c, "contracts"), tiers = num(c, "tiers");
        if (contracts == 0 && tiers == 0) return "";
        List<String> parts = new ArrayList<>();
        if (contracts > 0) parts.add(contracts == 1 ? UiText.t("ending.claim.contractOne", "{count} contract", "count", contracts) : UiText.t("ending.claim.contractMany", "{count} contracts", "count", contracts));
        if (tiers > 0) parts.add(tiers == 1 ? UiText.t("ending.claim.tierOne", "{count} chapter tier", "count", tiers) : UiText.t("ending.claim.tierMany", "{count} chapter tiers", "count", tiers));
        return UiText.t("ending.claim.ready", "Ready to claim: {list}", "list", String.join(" · ", parts));
    }

    /** A line fitted to w: the body font, else the small one, else trimmed in the small one. */
    static Text fit(String s, int w) {
        if (width(body(s)) <= w) return body(s);
        if (width(small(s)) <= w) return small(s);
        return small(UiMath.trim(s, w, v -> width(small(v))));
    }

    /**
     * The rewards: each row with its reason, appearing one at a time and counting up over progress 0..ROWS_END, then the total and the claim
     * strip (nothing drawn when nothing waits); progress 1 draws everything final. The rows take two lines each (label and reason) when h holds
     * them, else one; when even that does not fit (the War Table note's few lines) the rows give way to the total, wrapped. Returns the y under it.
     */
    static int rewards(DrawContext g, JsonObject r, int x, int y, int w, int h, float progress) { return rewards(g, r, x, y, w, h, progress, true); }

    /** The rewards in their static form (the War Table note): every row final, one line each when h holds them, else the total wrapped; no total line or claim strip (the note's tags carry the claim). */
    static int rewards(DrawContext g, JsonObject r, int x, int y, int w, int h) { return rewards(g, r, x, y, w, h, 1f, false); }

    static int rewards(DrawContext g, JsonObject r, int x, int y, int w, int h, float progress, boolean foot) {
        reset("reward"); rewardRows = 0; total = ""; claim = ""; columns = false; barsDrawn = 0; barsCrossed = 0; barFill.clear(); bestsShown.clear();
        var rows = rewardsOf(r);
        rewardsTotal = rows.size();
        areas.put("reward", new int[]{x, y, w, h});
        var font = font();
        if (rows.isEmpty()) {
            boolean saved = !r.has("saved") || r.get("saved").getAsBoolean(), eligible = !r.has("eligible") || r.get("eligible").getAsBoolean();
            String why = !saved ? UiText.t("ending.rewards.notSaved", "Rewards not saved. Progress unchanged.") : !eligible ? UiText.t("ending.rewards.ineligible", "No match rewards: play at least 3 minutes to qualify.") : UiText.t("ending.rewards.nothing", "Nothing earned this battle.");
            int yy = y;
            for (String l : UiMath.lines(why, w, Math.max(1, h / 12), s -> width(body(s)))) { put(g, "reward-none-" + (yy - y), body(l), x, yy, saved ? SEPIA : DarkUi.ROUT_TEXT); yy += 12; }
            return yy;
        }
        String claimLine = foot ? claimText(r) : "";
        boolean full = progress >= 1;
        List<Bar> bars = foot ? barsOf(r) : List.of();
        int barsH = bars.isEmpty() ? 0 : bars.size() * BAR_ROW + 6;
        List<String> bests = foot ? bestsOf(r) : List.of(); // pass 3: the bests, in the gap above the bars
        int bestsH = bests.isEmpty() ? 0 : bests.size() * KV + 4;
        int room = h - (foot ? 20 + (claimLine.isEmpty() ? 0 : CLAIM_H + 6) : 0) - barsH - bestsH; // the total's double rule and line, the claim strip, the bars and the bests
        int step = rows.size() * REWARD_TALL <= room ? REWARD_TALL : rows.size() * REWARD_SHORT <= room ? REWARD_SHORT : 0;
        if (!foot && progress >= 1) { // the note (ending polish): every row in two compact columns, top to bottom then the second column, when they fit
            int gap = 10, cw = (w - gap) / 2, lines = (rows.size() + 1) / 2;
            if (lines * COLUMN_ROW <= h) {
                for (int i = 0; i < rows.size(); i++) {
                    var w0 = rows.get(i);
                    int cx = x + (i / lines) * (cw + gap), cy = y + (i % lines) * COLUMN_ROW;
                    String amount = amountText(w0, w0.amount());
                    int aw = amount.isEmpty() ? 0 : width(body(amount)) + 4;
                    put(g, "reward-" + i, small(UiMath.trim(w0.label(), cw - aw, s -> width(small(s)))), cx, cy + 1, INK);
                    if (!amount.isEmpty()) right(g, "reward-" + i + "-amount", body(amount), cx + cw, cy, BROWN);
                }
                rewardRows = rows.size(); total = totalText(rows); columns = true;
                return y + lines * COLUMN_ROW;
            }
        }
        if (step == 0) { // the note: the total alone, wrapped to the lines it has
            String all = totalText(rows);
            int yy = y;
            var lines = UiMath.lines(all, w, Math.max(1, h / 12), s -> width(body(s)));
            for (int i = 0; i < lines.size(); i++) { put(g, "reward-sum-" + i, body(lines.get(i)), x, yy, INK); yy += 12; }
            rewardRows = rows.size(); total = all;
            return yy;
        }
        float slot = ROWS_END / rows.size();
        int ry = y;
        for (int i = 0; i < rows.size(); i++) {
            var w0 = rows.get(i);
            float t = full ? 1 : (progress - i * slot) / slot;
            boolean waiting = t < 0;
            if (!waiting) rewardRows++;
            float e = Math.min(1, Math.max(0, t)); e = 1 - (1 - e) * (1 - e);
            String amount = waiting ? "" : amountText(w0, Math.round(w0.amount() * e));
            Text amt = heading(amount);
            int aw = amount.isEmpty() ? 0 : width(amt) + 8;
            int labelInk = waiting ? 0x47000000 | (INK & 0xffffff) : INK, reasonInk = waiting ? 0x47000000 | (SEPIA & 0xffffff) : SEPIA;
            if (step == REWARD_TALL) {
                put(g, "reward-" + i, body(UiMath.trim(w0.label(), w - aw, s -> width(body(s)))), x, ry, labelInk);
                if (!w0.reason().isEmpty()) put(g, "reward-" + i + "-why", small(UiMath.trim(w0.reason(), w - aw, s -> width(small(s)))), x, ry + 11, reasonInk);
            } else {
                int lw = put(g, "reward-" + i, body(UiMath.trim(w0.label(), w - aw, s -> width(body(s)))), x, ry, labelInk);
                int room2 = w - aw - lw - 8;
                if (!w0.reason().isEmpty() && room2 > 30) put(g, "reward-" + i + "-why", small(UiMath.trim(w0.reason(), room2, s -> width(small(s)))), x + lw + 6, ry + 1, reasonInk);
            }
            if (!amount.isEmpty()) right(g, "reward-" + i + "-amount", amt, x + w, ry + (step == REWARD_TALL ? 3 : 0), t < 1 ? COUNTING : BROWN);
            // a dashed rule under the row
            for (int d = x; d < x + w; d += 4) g.fill(d, ry + step - 3, Math.min(x + w, d + 2), ry + step - 2, waiting ? 0x30b79f73 : 0xffb79f73);
            ry += step;
        }
        int barTop = Math.max(ry + 2, y + room) + bestsH;
        if (!bests.isEmpty() && (full || progress >= ROWS_END)) // pass 3: the bests, once the rows have counted up
            for (int i = 0; i < bests.size(); i++) { put(g, "reward-best-" + i, fit(bests.get(i), w), x, barTop - bestsH + 2 + i * KV, BROWN); bestsShown.add(bests.get(i)); }
        if (!bars.isEmpty()) bars(g, bars, rows, x, barTop + 6, w, full ? 1 : progress, slot);
        if (!foot || !full && progress < ROWS_END) return ry;
        // the total, under a double rule
        int ty = barTop + barsH;
        g.fill(x, ty, x + w, ty + 1, RULE); g.fill(x, ty + 2, x + w, ty + 3, RULE);
        int tw = put(g, "reward-total-label", heading(UiText.t("ending.rewards.total", "Total")), x, ty + 6, BROWN);
        String sum = totalText(rows);
        Text st = fit(sum, w - tw - 12);
        right(g, "reward-total", st, x + w, ty + 7, BROWN);
        total = st.getString();
        int by = ty + 20;
        if (!claimLine.isEmpty()) {
            int cy = by + 6;
            g.fill(x, cy, x + w, cy + CLAIM_H, CLAIM_FILL);
            String go = UiText.t("ending.rewards.claimGo", "Contracts on the War Table");
            int gw = width(small(go)), inner = w - 12;
            boolean both = width(body(claimLine)) + gw + 16 <= inner;
            Text ct = fit(claimLine, both ? inner - gw - 16 : inner);
            put(g, "reward-claim", ct, x + 6, cy + 5, CLAIM_INK);
            if (both) right(g, "reward-claim-go", small(go), x + w - 6, cy + 6, CLAIM_TEXT);
            claim = ct.getString();
            by = cy + CLAIM_H;
        }
        return by;
    }

    // ---- the progress bars (ending polish pass 2) ----
    record Bar(String label, String kind, int before, int after, int base, int next, int tier, int crossings, int finalBase, int finalNext, int finalTier) {
        boolean crossed() { return after >= next; }
        /** Two or more thresholds in one match: the bar climbs, flashes, empties and climbs again, ending on the last climb. */
        boolean multi() { return crossings >= 2; }
        /** What a crossing reads: Level up! for XP, Tier N! for Renown, Contract done! for a contract. */
        String word() { return kind.equals("xp") ? UiText.t("ending.bar.wordLevel", "Level up!") : kind.equals("renown") ? UiText.t("ending.bar.wordTier", "Tier {tier}!", "tier", tier) : contractDone(); }
        /** What crossing number m (1-based) reads: Renown names each tier reached in turn. */
        String word(int m) { return kind.equals("renown") ? UiText.t("ending.bar.wordTierNth", "Tier {tier}!", "tier", tier + m - 1) : word(); }
        /** The level or tier a multi-crossing bar ends on. */
        int reached() { return finalNext > finalBase ? finalTier - 1 : finalTier; }
        /** The label once every crossing is made: it names the final level or tier ("XP: Level 3", "Ironclad Renown: Tier 2"). */
        String finalLabel() { return kind.equals("xp") ? UiText.t("ending.bar.finalLevel", "{label}: Level {level}", "label", label, "level", reached()) : UiText.t("ending.bar.finalTier", "{label}: Tier {tier}", "label", label, "tier", reached()); }
    }
    /** The crossing word of a contract bar, also the widest word the value column is sized for. */
    static String contractDone() { return UiText.t("ending.bar.wordContract", "Contract done!"); }
    static List<Bar> barsOf(JsonObject r) {
        List<Bar> out = new ArrayList<>();
        for (JsonElement e : arr(r, "progress")) {
            if (!e.isJsonObject()) continue;
            var o = e.getAsJsonObject();
            int base = num(o, "base"), next = num(o, "next"), tier = num(o, "tier");
            if (next <= base) continue;
            int cross = num(o, "crossings"), fb = base, fn = next, ft = tier;
            if (cross >= 2 && num(o, "finalNext") > 0) { fb = num(o, "finalBase"); fn = num(o, "finalNext"); ft = num(o, "finalTier"); } else cross = num(o, "after") >= next ? 1 : 0;
            out.add(new Bar(str(o, "label"), str(o, "kind"), num(o, "before"), num(o, "after"), base, next, tier, cross, fb, fn, ft));
        }
        return out;
    }
    /** The reward row a bar waits for: XP's row, the House's Renown row; -1 (the rows' end) for a contract or a row not found. */
    static int anchor(Bar b, List<Reward> rows) {
        String want = b.kind().equals("xp") ? "XP" : b.kind().equals("renown") ? b.label() : null;
        if (want != null) for (int i = 0; i < rows.size(); i++) if (rows.get(i).label().equals(want)) return i;
        return -1;
    }
    /**
     * One line per bar: the label, the track with the share already reached, then the climb filling once its reward row has counted up (over BAR_FILL
     * of the step), and the value right ("612 / 750"). A bar that reaches its threshold fills, flashes for FLASH of the step and then reads its word
     * (Level up!, Tier N!, Contract done!). Words are recorded under reward-bar- so the rewards' layout check covers them.
     */
    static void bars(DrawContext g, List<Bar> bars, List<Reward> rows, int x, int y, int w, float progress, float slot) {
        int labelW = Math.min(150, w * 3 / 10), valueW = Math.max(width(heading(contractDone())), width(body("00,000 / 00,000"))) + 4;
        int tx0 = x + labelW + 8, tx1 = x + w - valueW - 8;
        g.fill(x, y - 5, x + w, y - 4, RULE);
        for (int i = 0; i < bars.size(); i++) {
            var b = bars.get(i);
            int by = y + i * BAR_ROW, a = anchor(b, rows);
            float start = a < 0 ? ROWS_END : (a + 1) * slot, f = Math.max(0, Math.min(1, (progress - start) / BAR_FILL)), ease = 1 - (1 - f) * (1 - f);
            int span = b.next() - b.base();
            float value = b.before() + (b.after() - b.before()) * ease;
            float share = Math.max(0, Math.min(1, (value - b.base()) / span)), was = Math.max(0, Math.min(1, (b.before() - b.base()) / (float) span));
            boolean waiting = progress < start, crossed = b.crossed() && value >= b.next() - .001f;
            // the moment the eased climb reaches the threshold: ease(f) = k at f = 1 - sqrt(1 - k)
            float k = (b.next() - b.before()) / (float) Math.max(1, b.after() - b.before());
            float crossAt = start + BAR_FILL * (float) (1 - Math.sqrt(Math.max(0, 1 - Math.min(1, k))));
            float flash = crossed ? Math.max(0, 1 - (progress - crossAt) / FLASH) : 0;
            String label = b.label(), word = b.word(), numeric = UiMath.thousands(Math.round(value)) + " / " + UiMath.thousands(b.next());
            int fill = crossed ? BAR_DONE : BAR_INK, crossedNow = crossed ? 1 : 0;
            boolean wordShown = crossed;
            float fillShown = f;
            if (b.multi()) {
                // several thresholds: climb to full, flash, empty and climb again; the last climb stops where the match left the bar and the label names that level
                float finalShare = b.finalNext() > b.finalBase() ? Math.max(0, Math.min(1, (b.after() - b.finalBase()) / (float) (b.finalNext() - b.finalBase()))) : 1f;
                float seg = MatchEnding.climbSpan(b.crossings(), BAR_FILL);
                var c = MatchEnding.climb(b.crossings(), (progress - start) / seg, was, finalShare);
                boolean last = c.index() >= b.crossings(), counted = b.finalNext() > b.finalBase();
                share = c.share(); crossedNow = c.crossed();
                flash = c.crossed() > 0 ? Math.max(0, 1 - c.since() * seg / FLASH) : 0;
                word = b.word(Math.max(1, c.crossed()));
                if (c.crossed() >= b.crossings()) label = b.finalLabel();
                fill = last || c.crossed() == 0 ? BAR_INK : BAR_DONE;
                if (c.index() > 0) was = 0;
                if (last && counted) numeric = UiMath.thousands(Math.round(b.finalBase() + share * (b.finalNext() - b.finalBase()))) + " / " + UiMath.thousands(b.finalNext());
                else if (c.index() == 0) numeric = UiMath.thousands(Math.round(b.before() + (b.next() - b.before()) * share)) + " / " + UiMath.thousands(b.next());
                // the numbers of a climb in the middle are not in the report: the word of the level just reached stands in
                wordShown = !waiting && (flash > 0 || (c.index() > 0 && !(last && counted)));
                fillShown = Math.max(0, Math.min(1, (progress - start) / (seg * (b.crossings() + MatchEnding.CLIMB_FILL))));
            }
            put(g, "reward-bar-" + i, fit(label, labelW), x, by, waiting ? 0x80000000 | (SEPIA & 0xffffff) : INK);
            int ty = by + 2, th = 6;
            g.fill(tx0 - 1, ty - 1, tx1 + 1, ty + th + 1, RULE);
            g.fill(tx0, ty, tx1, ty + th, BAR_TRACK);
            int wasX = tx0 + Math.round((tx1 - tx0) * was), nowX = tx0 + Math.round((tx1 - tx0) * share);
            g.fill(tx0, ty, wasX, ty + th, BROWN);
            if (nowX > wasX) g.fill(wasX, ty, nowX, ty + th, fill);
            if (flash > 0) {
                int al = (int) (255 * flash);
                g.fill(tx0 - 2, ty - 2, tx1 + 2, ty + th + 2, (al << 24) | (FLASH_INK & 0xffffff));
                flashes.add(word);
            }
            Text val = wordShown ? heading(word) : body(numeric);
            right(g, "reward-bar-" + i + "-value", val, x + w, by + (wordShown ? -1 : 0), wordShown ? BROWN : waiting ? SEPIA : INK);
            barFill.add(fillShown); barsDrawn++;
            barsCrossed += crossedNow;
        }
    }

    // ---- the checks' view ----
    /** What is wrong with the panel under prefix as last drawn ("" when nothing): a word outside the panel's box, or two words overlapping. */
    static String layout(String prefix) {
        var area = areas.get(prefix);
        if (area == null) return prefix + " not drawn";
        StringBuilder bad = new StringBuilder();
        List<String> keys = new ArrayList<>();
        for (var k : texts.keySet()) if (k.startsWith(prefix + "-")) keys.add(k);
        for (var k : keys) {
            int[] b = rects.get(k);
            if (b[0] < area[0] || b[1] < area[1] - 1 || b[0] + b[2] > area[0] + area[2] + 1 || b[1] + b[3] > area[1] + area[3] + 2)
                bad.append(k).append(' ').append(Arrays.toString(b)).append(" outside ").append(Arrays.toString(area)).append("; ");
        }
        for (int i = 0; i < keys.size(); i++) for (int j = i + 1; j < keys.size(); j++) {
            int[] a = rects.get(keys.get(i)), b = rects.get(keys.get(j));
            if (a[0] + 1 < b[0] + b[2] - 1 && b[0] + 1 < a[0] + a[2] - 1 && a[1] + 2 < b[1] + b[3] - 2 && b[1] + 2 < a[1] + a[3] - 2)
                bad.append(keys.get(i)).append(" overlaps ").append(keys.get(j)).append("; ");
        }
        return bad.toString();
    }
}
