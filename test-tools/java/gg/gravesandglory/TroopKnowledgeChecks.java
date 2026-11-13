package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;

import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/** GRAVES_PHASE1_CHECKS=ui-troop-know (Kort, 2026-11-12): a troop the commander owns shows its Stats face with the sheet's numbers; one he
 *  has not recruited shows "Unknown troop", a silhouette and no numbers, stars, Ranks or Mastery on the roll, the letter, the Odds note,
 *  the tips and every face of its troop page, at 854x480, 960x540, 1920x1080 and 2560x1440. Never included in releases. */
final class TroopKnowledgeChecks implements Phase1Checks.Suite {
    static final int WRITS = 10; // the old 1000 gold, paid in troop writs (army rework)
    static final String H0 = Spec.HOUSES[0].id();
    static final String[] TABS = {"troop", "matchups", "growth", "stats"};
    static final int[][] SIZES = {{854, 480, 2}, {1920, 1080, 2}, {2560, 1440, 0}};
    /** The texts an unrecruited troop's page must not draw: stars, Ranks, Mastery, numbers, matchups, growth and stats. */
    static final String[] SECRET = {"letter-stars", "letter-line2", "rung-", "mastery", "level-", "stat-", "beats", "beaten-by", "counter-", "growth", "held", "head-levels", "head-stats"};

    volatile int stage; volatile UUID player;
    int mark = -1, from, oldW, oldH, oldGui, size, tabAt; boolean finished;
    String owned, unowned, legendary; List<String> unownedNames = new ArrayList<>(); final List<String> bad = new ArrayList<>();

    int since(int tick) { if (mark != stage) { mark = stage; from = tick; } return tick - from; }
    static CampScreen screen(MinecraftClient c) { return c.currentScreen instanceof CampScreen s ? s : null; }

    /** The real names of troops not owned that a text shows, by key. */
    List<String> leaks(Map<String, String> texts) {
        List<String> out = new ArrayList<>();
        for (var e : texts.entrySet()) for (String n : unownedNames) if (e.getValue() != null && e.getValue().contains(n)) out.add(e.getKey() + " says \"" + n + "\"");
        return out;
    }

    void end(MinecraftClient c) { c.setScreen(null); if (oldW > 0) window(c, oldW, oldH, oldGui); stage = 99; }

    public void step(MinecraftClient c, int tick) {
        if (tick > 9000 && !finished) { report("p1-know-timeout", false, "stuck in stage " + stage + ": " + bad); end(c); }
        switch (stage) {
            case 0 -> { // a new commander (the Commons owned) with troop writs and a Pact, at camp, the window 960x540 GUI 1
                if (tick < 20) return;
                var w = c.getWindow(); oldW = w.getWidth(); oldH = w.getHeight(); oldGui = c.options.getGuiScale().getValue(); player = c.player.getUuid();
                c.setScreen(null); window(c, 960, 540, 1); stage = -1;
                server(c, b -> { if (b.running) b.campaign.returnLobby(); var p = new Profile(); p.troopWrits = WRITS; p.pacts = 1; b.campaign.profiles.put(player, p); b.campaign.save(player); stage = 1; });
            }
            case 1 -> {
                var p = CampaignClient.profile;
                if (p.troopWrits != WRITS || GravesAndGloryClient.running) { if (since(tick) > 300) { report("p1-know-unowned", false, "no fresh profile"); end(c); } return; }
                for (var t : Spec.TROOPS) {
                    boolean mine = p.owned.containsKey(t.id());
                    if (!mine) unownedNames.add(t.name());
                    if (!t.house().equals(H0)) continue;
                    if (mine && owned == null) owned = t.id();
                    if (!mine && unowned == null && (t.rarity().equals("common") || t.rarity().equals("rare"))) unowned = t.id();
                    if (!mine && t.rarity().equals("legendary")) legendary = t.id();
                }
                if (owned == null || unowned == null) { report("p1-know-unowned", false, "no owned and unowned troop in " + H0 + ": " + p.owned.keySet()); end(c); return; }
                CampScreen.open("recruit"); stage = 2;
            }
            case 2 -> { // the roll and the letter of an unowned troop, then the Odds note, on every House
                int k = since(tick);
                var s = screen(c); var r = s == null ? null : s.recruit;
                if (k < 10 || r == null) { if (k > 200) { report("p1-know-unowned", false, "no recruit page"); end(c); } return; }
                if (k == 10) { r.selected = unowned; r.refresh(); return; }
                if (k == 16) {
                    for (var row : WarTableRecruit.rows()) {
                        boolean mine = TroopKnowledge.owned(row.troop());
                        if (!mine && (!row.name().equals(TroopKnowledge.UNKNOWN) || !row.silhouette())) bad.add("row " + row.troop() + " reads \"" + row.name() + "\"" + (row.silhouette() ? "" : " in colour"));
                    }
                    for (var e : r.buttons.entrySet()) if (e.getKey().startsWith("row-")) for (String n : unownedNames) if (e.getValue().getMessage().getString().equals(n)) bad.add(e.getKey() + " narrates \"" + n + "\"");
                    var t = WarTableRecruit.texts();
                    if (!TroopKnowledge.UNKNOWN.equals(t.get("letter-name"))) bad.add("letter name \"" + t.get("letter-name") + "\"");
                    if (!TroopKnowledge.NOT_RECRUITED.equals(t.get("letter-line1"))) bad.add("letter kind \"" + t.get("letter-line1") + "\"");
                    if (t.containsKey("letter-line2") || t.containsKey("letter-stars")) bad.add("letter shows " + t.get("letter-line2") + " / stars " + t.get("letter-stars"));
                    for (var p : WarTableRecruit.portraits()) if (p.where().equals("letter") && (p.drawn() == null || !p.drawn().silhouette())) bad.add("letter portrait in colour");
                    bad.addAll(leaks(t));
                    shot(c, "know-recruit-unowned");
                    r.odds = true; return;
                }
                if (k == 22) { bad.addAll(leaks(WarTableRecruit.texts())); if (!TroopKnowledge.UNKNOWN.equals(WarTableRecruit.texts().get("letter-name"))) bad.add("odds: letter name shown"); shot(c, "know-odds-unowned"); r.odds = false; return; }
                if (k == 26 && legendary != null) { r.selected = legendary; r.refresh(); return; }
                if (k == 32) { // the Pact's tip under the tag names no unowned troop
                    var a = RecruitView.action(legendary == null ? unowned : legendary);
                    if (a != null) for (String n : unownedNames) if (a.tip().contains(n)) bad.add("tip \"" + a.tip() + "\"");
                    bad.addAll(leaks(WarTableRecruit.texts()));
                    for (String lockedId : WarTableRecruit.padlocks()) if (!TroopKnowledge.owned(lockedId)) bad.add("a padlock on the unowned row " + lockedId + " (it marks a Legendary)");
                    shot(c, "know-recruit-legendary");
                    return;
                }
                if (k >= 34 && k < 34 + 6 * Spec.HOUSES.length) { // every House's roll
                    int i = (k - 34) / 6, j = (k - 34) % 6;
                    if (j == 0) { var b = r.button("house-" + Spec.HOUSES[i].id()); if (b != null) b.onPress(); }
                    if (j == 5) { for (var row : WarTableRecruit.rows()) if (!TroopKnowledge.owned(row.troop()) && !row.name().equals(TroopKnowledge.UNKNOWN)) bad.add(Spec.HOUSES[i].id() + " row " + row.troop() + " \"" + row.name() + "\""); bad.addAll(leaks(WarTableRecruit.texts())); }
                    return;
                }
                if (k == 34 + 6 * Spec.HOUSES.length) {
                    report("p1-know-recruit", bad.isEmpty(), bad.isEmpty() ? "every House's roll, the letter, the Odds note and the Pact tip show \"Unknown troop\" and silhouettes for the " + unownedNames.size() + " troops not owned" : String.join("; ", bad));
                    bad.clear(); s.troop = unowned; s.page("troop"); s.troopSheet.show(unowned); tabAt = 0; stage = 3;
                }
            }
            case 3 -> unownedTabs(c, tick, "", 4);
            case 4 -> { // the owned troop's Stats face, its numbers read back from the sheet
                int k = since(tick); var s = screen(c);
                if (k == 0) { s.troopSheet.show(owned); s.troopSheet.face("stats"); return; }
                if (k == 8) {
                    var t = ArmyCards.spec(owned); var w = WarTableTroop.texts();
                    double m = UpgradeStats.starMult(t);
                    Map<String, String> want = new LinkedHashMap<>();
                    want.put("stat-health", UpgradeStats.num(t.health() * m)); want.put("stat-damage", UpgradeStats.num(t.damage() * m));
                    want.put("stat-interval", String.format(java.util.Locale.ROOT, "%.2f", t.cooldown() / 20.0) + " s"); want.put("stat-range", UpgradeStats.num(t.range()) + " blocks");
                    want.put("stat-speed", TroopKnowledge.speedLabel(t.speed())); want.put("stat-cost", t.cost() + " gold");
                    want.put("stat-training", UpgradeStats.num(t.trainingSeconds()) + " s"); want.put("stat-slots", String.valueOf(t.slotWeight()));
                    for (var e : want.entrySet()) if (!e.getValue().equals(w.get(e.getKey()))) bad.add(e.getKey() + " \"" + w.get(e.getKey()) + "\" (want \"" + e.getValue() + "\")");
                    if (!t.name().equals(w.get("letter-name"))) bad.add("name \"" + w.get("letter-name") + "\"");
                    for (var p : WarTableTroop.portraits()) if (p.where().equals("letter") && (p.drawn() == null || p.drawn().silhouette())) bad.add("owned portrait is a silhouette");
                    int[] sheet = WarTableTroop.rects().get("sheet");
                    for (var e : WarTableTroop.rects().entrySet()) if (e.getKey().startsWith("stat-") && sheet != null && (e.getValue()[0] + e.getValue()[2] > sheet[0] + sheet[2] || e.getValue()[1] + e.getValue()[3] > sheet[1] + WarTableTroop.CONTENT_END)) bad.add(e.getKey() + " runs off the sheet");
                    for (String tab : new String[]{"tab-troop", "tab-matchups", "tab-growth", "tab-stats"}) { int[] b = WarTableTroop.rects().get(tab); if (b == null || sheet == null || b[0] + b[2] > sheet[0] + sheet[2]) bad.add(tab + " off the sheet"); }
                    report("p1-know-owned-stats", bad.isEmpty(), (bad.isEmpty() ? "" : String.join("; ", bad) + "; ") + "the Stats face of " + owned + ": " + statLine(w));
                    bad.clear(); shot(c, "know-owned-stats-960"); size = 0; stage = 5;
                }
            }
            case 5 -> { // the sizes: the owned Stats face, then the unowned troop's four faces and the Odds note
                int k = since(tick); var s = screen(c);
                if (k == 0) { var z = SIZES[size]; window(c, z[0], z[1], z[2], z[0] >= 1920); return; }
                if (k == 30) { s.troopSheet.show(owned); s.troopSheet.face("stats"); return; }
                if (k == 36) { shot(c, "know-owned-stats-" + SIZES[size][0]); s.troopSheet.show(unowned); tabAt = 0; stage = 6; }
            }
            case 6 -> unownedTabs(c, tick, "-" + SIZES[size][0], 7);
            case 7 -> { // the Odds note at this size
                int k = since(tick); var s = screen(c);
                if (k == 0) { s.page("recruit"); return; }
                if (k == 6 && s.recruit != null) { s.recruit.selected = unowned; s.recruit.refresh(); s.recruit.odds = true; return; }
                if (k == 12) {
                    bad.addAll(leaks(WarTableRecruit.texts()));
                    shot(c, "know-odds-unowned-" + SIZES[size][0]);
                    if (s.recruit != null) s.recruit.odds = false;
                    if (++size < SIZES.length) { s.troop = unowned; s.page("troop"); stage = 5; mark = -1; }
                    else { report("p1-know-sizes", bad.isEmpty(), bad.isEmpty() ? "854, 1920 and 2560: no unowned name, number or star on any face or the Odds note" : String.join("; ", bad)); end(c); }
                }
            }
            case 99 -> { if (!finished) { finished = true; finish(c, "ui-troop-know"); } }
            default -> {}
        }
    }

    static String statLine(Map<String, String> w) {
        StringBuilder b = new StringBuilder();
        for (var e : w.entrySet()) if (e.getKey().startsWith("stat-")) b.append(e.getKey().substring(5)).append('=').append(e.getValue()).append(", ");
        return b.toString();
    }

    /** The unowned troop's four faces in turn: the face's texts hold no secret and no unowned name, the letter's portrait is the
     *  silhouette and its name "Unknown troop"; a shot of each; then the next stage. */
    void unownedTabs(MinecraftClient c, int tick, String suffix, int next) {
        int k = since(tick); var s = screen(c);
        if (s == null || s.troopSheet == null) { if (k > 100) { report("p1-know-unowned", false, "no troop page"); end(c); } return; }
        int j = k % 8;
        if (j == 0) { s.troopSheet.face(TABS[tabAt]); return; }
        if (j != 7) return;
        var w = WarTableTroop.texts();
        String at = TABS[tabAt] + suffix;
        if (!"troop".equals(w.get("tab"))) bad.add(at + ": face " + w.get("tab") + " opened (only the Troop face opens for a troop not yet recruited)");
        for (String t : TABS) {
            var b = s.troopSheet.buttons.get("tab-" + t);
            boolean want = t.equals("troop");
            if (b == null || b.active != want) bad.add(at + ": tab-" + t + " active=" + (b == null ? "none" : b.active) + " (want " + want + ")");
        }
        if (!TroopKnowledge.UNKNOWN.equals(w.get("letter-name"))) bad.add(at + ": name \"" + w.get("letter-name") + "\"");
        if (!TroopKnowledge.HIDDEN.equals(w.get("hidden-line"))) bad.add(at + ": no placeholder line");
        for (var e : w.keySet()) for (String p : SECRET) if (e.startsWith(p)) bad.add(at + ": draws " + e + " \"" + w.get(e) + "\"");
        for (var p : WarTableTroop.portraits()) if (p.where().equals("letter") && (p.drawn() == null || !p.drawn().silhouette())) bad.add(at + ": portrait in colour");
        bad.addAll(leaks(w));
        shot(c, "know-unowned-" + at);
        if (++tabAt < TABS.length) return;
        if (suffix.isEmpty()) { report("p1-know-unowned", bad.isEmpty(), bad.isEmpty() ? "pressing each of the four faces of " + unowned + " keeps the Troop face (Matchups, Growth and Stats greyed and closed), with \"Unknown troop\", a silhouette and only \"" + TroopKnowledge.HIDDEN + "\"" : String.join("; ", bad)); bad.clear(); }
        stage = next;
    }
}
