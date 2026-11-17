package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The UI editor's sample screens (client only): the battle map, the battle report and the battle HUD, each over a made-up mid-battle
 * (Domination) so an owner can see and retype their lines without a running or finished battle. The sample state is swapped into the
 * client's globals only around the sample screen's own frame (render thread; packets are handled between frames) and the real values
 * are put back in a finally, so nothing of it outlives the frame. The samples send nothing: the map's input that would order, plan,
 * ping or select is off (BattleMapScreen.sample), and the report leaves its sample the moment it turns to another page.
 * The point names and places are the loaded map's own (Spec.POINTS is shared with the integrated server, so it is never swapped).
 */
public final class UiSamples {
    private UiSamples() {}

    /** Set while a sample frame runs: the screens' hooks call back in through this, and a nested call does not swap twice. */
    private static int depth;
    static boolean in() { return depth > 0; }

    /** The battle map (BattleMapScreen) over a sample battle: points with owners and names, own groups, allies, enemies, both scores, the clock. */
    public static Screen battleMap(Screen parent) {
        var s = new BattleMapScreen(parent);
        s.sample = true;
        return s;
    }

    /** The War Table battle report (CampScreen page "results") over a sample finished battle. Needs a world (the camp). */
    public static Screen battleReport() {
        var s = new CampScreen("results");
        s.sample = true;
        return s;
    }

    /** A screen that draws the battle HUD (BattleHud.render and what it hangs off) with a sample battle state, over a plain dark backdrop. */
    public static Screen battleHud(Screen parent) { return new HudSample(parent); }

    /** Why a sample cannot open right now (e.g. "join a world first"), or null when it can. Same idea for each: whyNot("map"|"report"|"hud"). */
    public static String whyNot(String which) {
        var c = MinecraftClient.getInstance();
        return switch (which == null ? "" : which) {
            case "map" -> null;
            case "report" -> c.world == null ? "join a world first" : GravesAndGloryClient.battleActive() ? "not while a battle runs: the War Table closes in a battle" : null;
            case "hud" -> c.world == null || c.player == null ? "join a world first" : GravesAndGloryClient.battleActive() ? "not while a battle runs: its lines appear under Lines" : null;
            default -> "no such sample";
        };
    }

    // ---------------------------------------------------------------- the swaps

    /** Runs body with the sample battle in the map's and the HUD's globals, then puts the real ones back. */
    static void withBattle(Runnable body) { withBattle(false, body); }

    static void withBattle(boolean hud, Runnable body) {
        if (in()) { body.run(); return; }
        // the real values, read now: a packet may have changed them since the last frame
        boolean ready = GravesAndGloryClient.ready, running = GravesAndGloryClient.running, waiting = CampaignClient.waiting;
        int team = GravesAndGloryClient.team, dawn = GravesAndGloryClient.dawn, dusk = GravesAndGloryClient.dusk, gold = GravesAndGloryClient.gold, respawn = GravesAndGloryClient.respawn;
        int[] owners = GravesAndGloryClient.owners, progress = GravesAndGloryClient.progress, challengers = GravesAndGloryClient.challengers;
        boolean[] contested = GravesAndGloryClient.contested;
        var sightings = GravesAndGloryClient.sightings;
        var markers = GravesAndGloryClient.markers;
        var houses = GravesAndGloryClient.houses;
        String abilityId = GravesAndGloryClient.abilityId;
        int abilityRemaining = GravesAndGloryClient.abilityRemaining, abilityTotal = GravesAndGloryClient.abilityTotal;
        String[] relicShown = GravesAndGloryClient.relicShown;
        int[] relicLeft = GravesAndGloryClient.relicLeft, relicTotal = GravesAndGloryClient.relicTotal;
        JsonObject battleMode = CampaignClient.battleMode;
        int tutorial = CampaignClient.tutorial;
        var groups = CommandMode.groups;
        var pings = CommandMode.pings;
        int seconds = CommandMode.seconds;
        String selected = CommandMode.selected, pending = CommandMode.pending;
        var mapState = BattleMapClient.state;
        long roundShown = BattleHud.roundShownMs;
        var tiersSeen = BattleHud.tiersSeen;
        int hudFrames = BattleHud.frames;
        long now = Util.getMeasuringTimeMs();
        List<EventFeed.Line> feed = hud ? EventFeed.lines(now) : List.of();
        depth++;
        try {
            int n = Math.max(Spec.POINTS.length, hud ? 3 : 0); // the HUD hangs a seal per entry; a field with no points still shows three
            int[] o = new int[n], p = new int[n], ch = new int[n];
            boolean[] ct = new boolean[n];
            for (int i = 0; i < n; i++) {
                ch[i] = -1;
                switch (i % 3) {
                    case 0 -> o[i] = 0; // yours
                    case 1 -> { o[i] = 1; ct[i] = i == 1; } // the enemy's, the first of them contested
                    default -> { o[i] = -1; ch[i] = 0; p[i] = Math.max(1, captureSeconds(i) * 5 / 8); } // neutral, your side taking it
                }
            }
            GravesAndGloryClient.ready = true; GravesAndGloryClient.running = true; CampaignClient.waiting = false;
            GravesAndGloryClient.team = 0; GravesAndGloryClient.dawn = 412; GravesAndGloryClient.dusk = 377; GravesAndGloryClient.gold = 260; GravesAndGloryClient.respawn = 0;
            GravesAndGloryClient.owners = o; GravesAndGloryClient.progress = p; GravesAndGloryClient.challengers = ch; GravesAndGloryClient.contested = ct;
            GravesAndGloryClient.sightings = sightings();
            GravesAndGloryClient.markers = List.of();
            CampaignClient.battleMode = domination();
            CampaignClient.tutorial = -1;
            CommandMode.groups = ownGroups();
            CommandMode.pings = List.of(new CommandMode.Ping("attack", (float) at(1)[0] + 6, (float) at(1)[1] - 8));
            CommandMode.seconds = 437;
            CommandMode.selected = "infantry"; CommandMode.pending = "";
            BattleMapClient.state = allies();
            if (hud) {
                GravesAndGloryClient.houses = Spec.HOUSES.length > 0 ? Map.of(Spec.HOUSES[0].id(), 2) : Map.of();
                GravesAndGloryClient.abilityId = "rally_horn"; GravesAndGloryClient.abilityRemaining = 140; GravesAndGloryClient.abilityTotal = 600;
                GravesAndGloryClient.relicShown = new String[] {"none", "none", "none"}; GravesAndGloryClient.relicLeft = new int[3]; GravesAndGloryClient.relicTotal = new int[3];
                BattleHud.roundShownMs = now; // the round's first minute: the hint slip hangs above the column
                BattleHud.tiersSeen = null; // no baseline: the sample's House tier is not a change, so no feed notice
                EventFeed.clear();
                for (var l : sampleFeed(now)) EventFeed.push(l.kind(), l.team(), l.text(), l.bornMs());
            }
            body.run();
        } finally {
            depth--;
            if (hud) {
                BattleHud.clear(); // nothing of the sample frame reads as drawn
                EventFeed.clear();
                for (var l : feed) EventFeed.push(l.kind(), l.team(), l.text(), l.bornMs());
            }
            GravesAndGloryClient.ready = ready; GravesAndGloryClient.running = running; CampaignClient.waiting = waiting;
            GravesAndGloryClient.team = team; GravesAndGloryClient.dawn = dawn; GravesAndGloryClient.dusk = dusk; GravesAndGloryClient.gold = gold; GravesAndGloryClient.respawn = respawn;
            GravesAndGloryClient.owners = owners; GravesAndGloryClient.progress = progress; GravesAndGloryClient.challengers = challengers; GravesAndGloryClient.contested = contested;
            GravesAndGloryClient.sightings = sightings; GravesAndGloryClient.markers = markers; GravesAndGloryClient.houses = houses;
            GravesAndGloryClient.abilityId = abilityId; GravesAndGloryClient.abilityRemaining = abilityRemaining; GravesAndGloryClient.abilityTotal = abilityTotal;
            GravesAndGloryClient.relicShown = relicShown; GravesAndGloryClient.relicLeft = relicLeft; GravesAndGloryClient.relicTotal = relicTotal;
            CampaignClient.battleMode = battleMode; CampaignClient.tutorial = tutorial;
            CommandMode.groups = groups; CommandMode.pings = pings; CommandMode.seconds = seconds; CommandMode.selected = selected; CommandMode.pending = pending;
            BattleMapClient.state = mapState;
            BattleHud.roundShownMs = roundShown; BattleHud.tiersSeen = tiersSeen; BattleHud.frames = hudFrames;
        }
    }

    /** Runs body with the sample report in CampaignClient.result, then puts the real one back (the camp's state packets set it between frames). */
    static void withReport(Runnable body) {
        if (in()) { body.run(); return; }
        JsonObject real = CampaignClient.result;
        depth++;
        try { if (REPORT == null) REPORT = report(); CampaignClient.result = REPORT; body.run(); }
        finally { depth--; CampaignClient.result = real; }
    }

    // ---------------------------------------------------------------- the sample battle

    static int captureSeconds(int i) { return i < Spec.POINTS.length ? Spec.POINTS[i].captureSeconds() : 8; }

    /** Point i's place {x, z}, or a place across the field's middle when the map has fewer points. */
    static double[] at(int i) {
        if (i < Spec.POINTS.length) return new double[] {Spec.POINTS[i].x() + .5, Spec.POINTS[i].z() + .5};
        double hx = Field.halfX(), hz = Field.halfZ();
        return switch (i % 3) { case 0 -> new double[] {-hx * .4, -hz * .3}; case 1 -> new double[] {0, hz * .25}; default -> new double[] {hx * .4, -hz * .3}; };
    }

    /** The round's mode block as the snapshot carries it: Domination, both scores out of 500, plain numbers, points played. */
    static JsonObject domination() {
        var m = new JsonObject();
        m.addProperty("id", Modes.DOMINATION);
        m.addProperty("name", "Domination");
        m.addProperty("rule", "Hold more points than the enemy to fill your score first.");
        m.addProperty("points", true);
        m.addProperty("overtime", false);
        pair(m, "max", 500, 500);
        strs(m, "format", "plain", "plain");
        return m;
    }

    /** The player's three groups: infantry pushing on the contested point, archers holding by their own point, support following. */
    static List<CommandMode.Group> ownGroups() {
        double[] a = at(0), b = at(1);
        float ix = (float) (a[0] + (b[0] - a[0]) * .6), iz = (float) (a[1] + (b[1] - a[1]) * .6);
        List<CommandMode.Group> out = new ArrayList<>();
        out.add(new CommandMode.Group("infantry", 24, .78f, "attack", "block", (float) b[0], (float) b[1], 0f, ix, iz, true, false, 30, .74f, 4, 1, false));
        out.add(new CommandMode.Group("ranged", 16, .95f, "hold", "banner", (float) a[0] + 9, (float) a[1] + 14, 0f, (float) a[0] + 9, (float) a[1] + 14, false, false, 18, .9f, 2, 0, false));
        out.add(new CommandMode.Group("support", 6, .55f, "follow", "loose", (float) a[0] - 12, (float) a[1] + 16, 0f, (float) a[0] - 12, (float) a[1] + 16, false, false, 8, .5f, 0, 0, false));
        return out;
    }

    /** Two allies (a player taking the neutral point, a bot defending yours) and a team plan on the contested point. */
    static BattleMapClient.State allies() {
        double[] a = at(0), b = at(1), c = at(2);
        var player = new BattleMapClient.Army("sample-ally-player", "Aldric", false, false, false, List.of(
                new BattleMapClient.Group("infantry", c[0] - 10, c[1] + 8, 18, .82, "attack", c[0], c[1], false),
                new BattleMapClient.Group("cavalry", c[0] + 16, c[1] - 12, 8, .9, "move", c[0], c[1], false)), -1, -1, 0, 0, false);
        var bot = new BattleMapClient.Army("sample-ally-bot", "normal bot 2", true, false, false, List.of(
                new BattleMapClient.Group("infantry", a[0] + 14, a[1] - 10, 20, .7, "hold", a[0], a[1], true),
                new BattleMapClient.Group("ranged", a[0] + 22, a[1] + 2, 12, 1.0, "hold", a[0], a[1], false)), BattlePlan.Job.DEFEND.ordinal(), 0, a[0], a[1], true);
        return new BattleMapClient.State(0L, List.of(player, bot),
                List.of(new BattleMapClient.PointPlan(1, BattlePlan.Job.ATTACK.ordinal(), "Aldric")),
                List.of(new BattleMapClient.Order("sample-ally-bot", BattlePlan.Job.DEFEND.ordinal(), 0, a[0], a[1], "Aldric")));
    }

    /** Enemies seen round the contested point: two now, two a few seconds ago. */
    static List<GravesAndGloryClient.Sight> sightings() {
        double[] b = at(1);
        return List.of(new GravesAndGloryClient.Sight((float) b[0] + 5, (float) b[1] - 7, 0), new GravesAndGloryClient.Sight((float) b[0] - 8, (float) b[1] - 3, 0),
                new GravesAndGloryClient.Sight((float) b[0] + 12, (float) b[1] + 6, 80), new GravesAndGloryClient.Sight((float) b[0] + 30, (float) b[1] - 22, 200));
    }

    /** Three feed lines, the newest last, as the server words them. */
    static List<EventFeed.Line> sampleFeed(long now) {
        String point = Spec.POINTS.length > 0 ? Spec.POINTS[0].name() : "Old Mill";
        List<EventFeed.Line> out = new ArrayList<>();
        try { out.add(new EventFeed.Line("point", 0, FeedLines.pointTaken("Dawn", point), now - 3000)); } catch (RuntimeException ignored) {} // a sheet without the mode line: left out
        out.add(new EventFeed.Line("loss", 0, FeedLines.lost(3, "Spearman"), now - 1500));
        out.add(new EventFeed.Line("kill", 0, FeedLines.lost(5, "Footman"), now - 400));
        return out;
    }

    static void pair(JsonObject o, String k, int a, int b) { var j = new JsonArray(); j.add(a); j.add(b); o.add(k, j); }
    static void strs(JsonObject o, String k, String a, String b) { var j = new JsonArray(); j.add(a); j.add(b); o.add(k, j); }

    // ---------------------------------------------------------------- the sample report

    /** A finished Domination round as the camp snapshot carries it (after UiReportChecks.synth): won 500 to 431, three commanders a side,
     *  three points, two Houses, six troop rows (one Troops page), rewards saved. */
    static JsonObject REPORT;

    static JsonObject report() {
        var r = new JsonObject();
        r.addProperty("id", "ui-editor-sample-report");
        r.addProperty("outcome", "VICTORY");
        r.addProperty("map", "Kingsfield Crossroads");
        r.addProperty("seconds", 437);
        r.addProperty("saved", true); r.addProperty("eligible", true);
        r.addProperty("gold", 46); r.addProperty("xp", 120); r.addProperty("beforeXp", 340);
        var squads = new JsonArray();
        String[] names = {"You", "Aldric", "normal bot", "hard bot", "Brannoc", "normal bot"};
        for (int i = 0; i < names.length; i++) {
            var row = new JsonObject(); String n = names[i]; boolean bot = n.endsWith(" bot");
            row.addProperty("name", n); row.addProperty("team", i < 3 ? 0 : 1); row.addProperty("bot", bot); row.addProperty("tier", bot ? n.substring(0, n.length() - 4) : ""); row.addProperty("you", i == 0);
            row.addProperty("kills", 14 + i); row.addProperty("lost", 6 + i); row.addProperty("gold", 310 + 40 * i); row.addProperty("objective", 120 + 9 * i);
            squads.add(row);
        }
        r.add("squads", squads);
        var pts = new JsonArray();
        String[] pn = {"Old Mill", "Chapel Hill", "Ford"};
        for (int i = 0; i < pn.length; i++) { var p = new JsonObject(); p.addProperty("name", pn[i]); p.addProperty("dawn", 120 + i * 13); p.addProperty("dusk", 90 + i * 5); pts.add(p); }
        r.add("points", pts);
        var hs = new JsonArray();
        var order = Profile.houseOrder();
        for (int i = 0; i < 2 && i < order.size(); i++) { var h = new JsonObject(); h.addProperty("house", order.get(i)); h.addProperty("tier", 1 + i); h.addProperty("traitSeconds", 95 + i * 10); h.addProperty("shards", 3 + i); h.addProperty("renown", 1 + i); hs.add(h); }
        r.add("houses", hs);
        var lv = new JsonArray();
        for (int i = 0; i < 6 && i < Spec.TROOPS.length; i++) { var l = new JsonObject(); l.addProperty("troop", Spec.TROOPS[i].id()); l.addProperty("highestLevel", 1 + i % 3); l.addProperty("level3Count", i % 3 == 2 ? 2 + i : 0); l.addProperty("weapon", 1 + i % 3); l.addProperty("armour", 1 + (i + 1) % 3); lv.add(l); }
        r.add("levels", lv);
        var m = domination();
        pair(m, "ledger", 500, 431);
        r.add("mode", m);
        return r;
    }

    // ---------------------------------------------------------------- the sample HUD

    /** The battle HUD alone over a dark backdrop. An ArmyScreen, so the real HUD under it stands down (GravesAndGloryClient.renderHud). */
    static final class HudSample extends GravesAndGloryClient.ArmyScreen {
        final Screen parent;
        HudSample(Screen parent) { super("Battle HUD"); this.parent = parent; }
        @Override public void render(DrawContext g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xff16120e);
            withBattle(true, () -> BattleHud.render(g, delta));
            super.render(g, mx, my, delta);
        }
        @Override public void close() { client.setScreen(parent); }
    }
}
