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

    // ---- what the last frame drew (the checks read these) ----
    static final Map<String, int[]> rects = new LinkedHashMap<>();
    static final Map<String, String> texts = new LinkedHashMap<>();
    /** Each panel's own box, keyed by its prefix (score, group, troops, reward). */
    static final Map<String, int[]> areas = new LinkedHashMap<>();
    static final List<String> scoreRows = new ArrayList<>(), groups = new ArrayList<>();
    static int rewardRows, rewardsTotal;
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
        String[] heads = {"Score", "K / D", "Army kills", "Lost", ReportSections.points(r) ? "On points" : "Objective"};
        int gap = 10, edge = x + w;
        int[] right = new int[heads.length];
        for (int c = heads.length - 1; c >= 0; c--) { right[c] = edge; edge -= Math.max(width(small(heads[c])), c == 1 ? width(body("00 / 00")) : width(body("0,000"))) + gap; }
        int nameX = x + 22, nameRight = edge + gap - 6;
        put(g, "score-head", small("Commander"), nameX, y + 1, SEPIA);
        for (int c = 0; c < heads.length; c++) right(g, "score-head-" + c, small(heads[c]), right[c], y + 1, SEPIA);
        g.fill(x, y + 11, x + w, y + 12, DarkUi.SEPIA_LINE);
        int ry = y + 16;
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
                int cw = width(small("MVP")) + 4;
                g.fill(nx, ry, nx + cw, ry + 9, MVP_FILL);
                g.drawText(font, small("MVP"), nx + 2, ry + 1, INK, false);
                rects.put("score-mvp", new int[]{nx, ry, cw, 9});
                nx += cw + 4;
            }
            String name = EndingClient.display(str(row, "name"));
            // the award chip, or "Left the battle", after the name (ending polish): it gives way to the name when the room is short
            boolean gone = row.has("left") && row.get("left").getAsBoolean();
            String tag = gone ? EndingClient.LEFT : str(row, "award");
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
        out.add(new Group("Commander", List.of(new Kv("Kills", String.valueOf(ck)), new Kv("Deaths", String.valueOf(deaths)),
            new Kv("K/D", MatchEnding.ratio(ck, deaths)), new Kv("Damage", UiMath.thousands(num(cmd, "damage")))), ""));
        int kept = num(army, "kept"), lost = num(army, "lost");
        String best = str(army, "best"), foot = "";
        if (!best.isEmpty()) {
            int kills = 0;
            for (var t : arr(army, "troops")) if (str(t.getAsJsonObject(), "id").equals(best)) kills = num(t.getAsJsonObject(), "kills");
            foot = "Best: " + troopName(best) + ", " + kills + (kills == 1 ? " kill" : " kills");
        }
        out.add(new Group("Army", List.of(new Kv("Kills", String.valueOf(Math.max(0, num(army, "kills") - ck))), new Kv("Losses", String.valueOf(lost)),
            new Kv("Kept alive", kept + " / " + (kept + lost))), foot));
        List<Kv> o = new ArrayList<>();
        o.add(new Kv("Captures", String.valueOf(num(objv, "captures"))));
        o.add(new Kv("Hold time", clock(num(objv, "holdSeconds"))));
        String mode = ReportSections.id(r);
        if (mode.isEmpty() || mode.equals(Modes.DOMINATION)) {
            var m = ReportSections.mode(r);
            o.add(new Kv("Final score", ReportSections.number(m, "ledger", 0, 0) + " / " + ReportSections.number(m, "ledger", 1, 0)));
        } else for (var row : ReportSections.rows(r)) { if (o.size() >= 4) break; o.add(new Kv(row.label(), row.value())); }
        o.add(new Kv("Length", clock((int) Math.round(r.has("seconds") ? r.get("seconds").getAsDouble() : 0))));
        out.add(new Group("Objectives", o, ""));
        out.add(new Group("Economy & orders", List.of(new Kv("Gold earned", UiMath.thousands(num(eco, "earned"))), new Kv("Gold spent", UiMath.thousands(num(eco, "spent"))),
            new Kv("Trained", String.valueOf(num(eco, "trained"))), new Kv("Upgrades", String.valueOf(num(eco, "upgrades"))), new Kv("Orders", String.valueOf(num(eco, "orders")))), ""));
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
        if (obj(r, "you") == null) { put(g, "group-none", body("No stats for this battle."), x, y, SEPIA); areas.put("group", new int[]{x, y, w, 12}); return y + 13; }
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
                int vw = width(body(kv.value()));
                String label = UiMath.trim(kv.label(), in - vw - 6, s -> width(body(s)));
                put(g, "group-" + i + "-" + k, body(label), gx + 5, ry, SEPIA);
                right(g, "group-" + i + "-" + k + "-value", body(kv.value()), gx + gw - 5, ry, INK);
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
        if (rows.isEmpty()) { put(g, "troops-none", body("No troops fielded."), x, y, SEPIA); return y + 13; }
        String[] heads = {"Fielded", "Kills", "Lost"};
        int[] right = {x + w - 96, x + w - 48, x + w};
        put(g, "troops-head", small("Troop"), x, y + 1, SEPIA);
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
        if (shownRows < rows.size() && ry + 10 <= y + h) { put(g, "troops-more", body("and " + (rows.size() - shownRows) + " more"), x, ry, SEPIA); ry += KV; }
        return ry;
    }

    /** Highlights (ending polish): the awards earned this match, the viewer's first, then in award order; up to three lines, each "Award: Name, detail"
     *  fitted to w. "No awards this battle." when none. Returns the y under it. */
    static int highlights(DrawContext g, JsonObject r, int x, int y, int w, int h) {
        reset("highlight");
        areas.put("highlight", new int[]{x, y, w, h});
        List<String> lines = highlightLines(r);
        if (lines.isEmpty()) { put(g, "highlight-none", body("No awards this battle."), x, y, SEPIA); return y + 13; }
        int ry = y;
        for (int i = 0; i < lines.size() && i < 3 && ry + 10 <= y + h; i++) {
            Text t = fit(lines.get(i), w);
            put(g, "highlight-" + i, t, x, ry, i == 0 && lines.get(i).startsWith("You") ? INK : SEPIA);
            ry += KV;
        }
        return ry;
    }
    /** The highlight lines in order: the viewer's award first ("You: Kingslayer, 2 commanders downed"), then the others' in award order. */
    static List<String> highlightLines(JsonObject r) {
        List<String> mine = new ArrayList<>(), others = new ArrayList<>();
        for (var a : EndingAwards.Award.values())
            for (var e : arr(r, "squads")) {
                var o = e.getAsJsonObject();
                if (!a.title.equals(str(o, "award"))) continue;
                String detail = str(o, "awardText");
                if (o.has("you") && o.get("you").getAsBoolean()) mine.add("You: " + a.title + (detail.isEmpty() ? "" : ", " + detail));
                else others.add(a.title + ": " + EndingClient.display(str(o, "name")) + (detail.isEmpty() ? "" : ", " + detail));
            }
        mine.addAll(others);
        return mine.size() > 3 ? new ArrayList<>(mine.subList(0, 3)) : mine;
    }

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

    /** The total, summed by kind: "150 gold · 100 Forge marks · 250 XP · 24 shards · 10 Renown · 1 Pact · 1 new title". */
    static String totalText(List<Reward> rows) {
        Map<String, Integer> sum = new LinkedHashMap<>();
        for (var w : rows) {
            String l = w.label(), kind = l.equals("Gold") ? "gold" : l.equals("XP") ? "XP" : l.equals("Forge marks") ? "Forge marks" : l.endsWith(" shards") ? "shards"
                : l.endsWith(" Renown") ? "Renown" : l.startsWith("Pact") ? "Pact" : l.startsWith("New title") ? "new title" : l;
            sum.merge(kind, l.startsWith("New title") ? 1 : w.amount(), Integer::sum);
        }
        List<String> parts = new ArrayList<>();
        for (var e : sum.entrySet()) {
            String k = e.getKey(); int n = e.getValue();
            parts.add(UiMath.thousands(n) + " " + (n != 1 && (k.equals("Pact") || k.equals("new title")) ? k + "s" : k));
        }
        return String.join(" · ", parts);
    }

    /** "Ready to claim: 2 contracts · 1 chapter tier", or "" when nothing waits. */
    static String claimText(JsonObject r) {
        var c = obj(r, "claimable");
        int contracts = num(c, "contracts"), tiers = num(c, "tiers");
        if (contracts == 0 && tiers == 0) return "";
        List<String> parts = new ArrayList<>();
        if (contracts > 0) parts.add(contracts + (contracts == 1 ? " contract" : " contracts"));
        if (tiers > 0) parts.add(tiers + (tiers == 1 ? " chapter tier" : " chapter tiers"));
        return "Ready to claim: " + String.join(" · ", parts);
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
        reset("reward"); rewardRows = 0; total = ""; claim = ""; columns = false;
        var rows = rewardsOf(r);
        rewardsTotal = rows.size();
        areas.put("reward", new int[]{x, y, w, h});
        var font = font();
        if (rows.isEmpty()) {
            boolean saved = !r.has("saved") || r.get("saved").getAsBoolean(), eligible = !r.has("eligible") || r.get("eligible").getAsBoolean();
            String why = !saved ? "Rewards not saved. Progress unchanged." : !eligible ? "No match rewards: play at least 3 minutes to qualify." : "Nothing earned this battle.";
            int yy = y;
            for (String l : UiMath.lines(why, w, Math.max(1, h / 12), s -> width(body(s)))) { put(g, "reward-none-" + (yy - y), body(l), x, yy, saved ? SEPIA : DarkUi.ROUT_TEXT); yy += 12; }
            return yy;
        }
        String claimLine = foot ? claimText(r) : "";
        boolean full = progress >= 1;
        int room = h - (foot ? 20 + (claimLine.isEmpty() ? 0 : CLAIM_H + 6) : 0); // the total's double rule and line, and the claim strip
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
        if (!foot || !full && progress < ROWS_END) return ry;
        // the total, under a double rule
        int ty = Math.max(ry + 2, y + room);
        g.fill(x, ty, x + w, ty + 1, RULE); g.fill(x, ty + 2, x + w, ty + 3, RULE);
        int tw = put(g, "reward-total-label", heading("Total"), x, ty + 6, BROWN);
        String sum = totalText(rows);
        Text st = fit(sum, w - tw - 12);
        right(g, "reward-total", st, x + w, ty + 7, BROWN);
        total = st.getString();
        int by = ty + 20;
        if (!claimLine.isEmpty()) {
            int cy = by + 6;
            g.fill(x, cy, x + w, cy + CLAIM_H, CLAIM_FILL);
            String go = "Contracts on the War Table";
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
