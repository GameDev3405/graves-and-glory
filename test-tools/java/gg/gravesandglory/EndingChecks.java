package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.lwjgl.glfw.GLFW;

/**
 * The match ending on a real integrated server (GRAVES_PHASE4_CHECKS=ending, run with GRAVES_INSTANT_ENDING=false). Five rounds, each started
 * as practice (the last a 4v4) on the elapsed-time fixture the other suites use (roundStarted 181 s back): the full sequence (steps, freeze, paid
 * once, no second finish, report, podium spot), the skip, a defeat with a rejoin, a draw with a commander waiting to respawn then a server stop,
 * and the 4v4 snapshot size. The report and snapshot of the first round are written to evidence/ending-report.json and ending-snapshot.json.
 * Task 6 on the client: the stats and rewards screens (first round), where DONE lands (first round: no room, the War Table home; the skip round:
 * a room, the war council), the War Table report's You face (between the first two rounds, while the server holds), and Escape (the skip round).
 */
final class EndingChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    volatile UUID player; volatile boolean done; volatile String shot; volatile int skips;
    int scenario, stage; long at; String savedProfile;
    // per round
    final Map<MobEntity, Vec3d> pos = new HashMap<>(); final Map<MobEntity, Float> hp = new HashMap<>();
    int gold1, xp1; String resultId; boolean froze, podium, stats, rewards, stopped;

    public int limit() { return 30000; }
    public void cleanup(MinecraftClient c) {
        Phase4Checks.server(c, b -> {
            if (b.running) b.campaign.returnLobby();
            if (savedProfile != null && player != null) { b.campaign.profiles.put(player, Campaign.JSON.fromJson(savedProfile, Profile.class)); b.campaign.save(player); }
        });
    }
    public void step(MinecraftClient c, int tick) {
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); Phase1Checks.window(c, 1280, 720, 3); }
        if (skips > 0 && tick % 3 == 0) { GravesAndGloryClient.send("ending:skip"); skips--; }
        String s = shot;
        if (s != null) { if (s.startsWith("!")) { Phase4Checks.shot(c, s.substring(1)); shot = null; } else shot = "!" + s; }
        client(c, tick);
        if (done) {
            boolean all = banners.size() == 3; for (var e : banners.entrySet()) all &= e.getKey().equals(e.getValue());
            report("ending-client-banner", all, "outcome -> banner word drawn: " + banners);
            Phase4Checks.finish(c, "ending");
        }
    }

    // ---- the client half (Task 5): EndingClient through the same rounds, on the client thread ----
    /** Set by the server rounds: the skip round wants the menu key pressed in RESULT; the 4v4 round holds into PODIUM for the three plates. */
    volatile boolean pressMenu, podiumThree;
    /** Outcome -> the banner word EndingClient drew for it. */
    final Map<String, String> banners = new LinkedHashMap<>();
    String clientShot; int shotAt = -1, resultAt = -1, podiumAt = -1, pressAt = -1, resizeAt = -1, restoreAt = -1;
    MatchEnding.Step lastStep; String lastOutcome = "";
    boolean frozenDone, podiumDone, skipDone, threeDone;
    // Task 6: the server waits (hold) while the client reads the War Table report; the endings this client has seen (1 the full round, 2 the skip round)
    volatile boolean hold, roomChecked; volatile long holdAt = -1;
    int endings, statsAt = -1, rewardsAt = -1, endedAt = -1, escStage, escAt, youStage, youAt;
    boolean statsDone, rewardsDone, menuSeen, rewardsMid;
    String statsNormal = "", rewardsSmall = "", roomHome = "", escWhy = "", youNormal = "", youVia = "";
    long hands0;
    final TreeMap<Integer, List<Runnable>> later = new TreeMap<>();
    void later(int tick, Runnable r) { later.computeIfAbsent(tick, k -> new ArrayList<>()).add(r); }
    static void escape(MinecraftClient c) { long w = c.getWindow().getHandle(); c.keyboard.onKey(w, GLFW.GLFW_KEY_ESCAPE, 1, GLFW.GLFW_PRESS, 0); c.keyboard.onKey(w, GLFW.GLFW_KEY_ESCAPE, 1, GLFW.GLFW_RELEASE, 0); }
    static int squads() { return CampaignClient.result.has("squads") ? CampaignClient.result.getAsJsonArray("squads").size() : -1; }
    static int rewardsSize() { return CampaignClient.result.has("rewards") ? CampaignClient.result.getAsJsonArray("rewards").size() : -1; }
    static String where(MinecraftClient c) { return c.currentScreen instanceof CampScreen s ? "CampScreen " + s.page : c.currentScreen == null ? "no screen" : c.currentScreen.getClass().getSimpleName(); }
    Entity watched; double clock0; int age0; Vec3d pos0;

    /** The client checks, each tick: the step EndingClient shows and what it drew. Screens at 1280x720 gui 3; the defeat banner and one podium shot at 854x480 gui 2. */
    void client(MinecraftClient c, int tick) {
        var st = EndingClient.step();
        if (st != null) c.options.hudHidden = false; // the harness hides the HUD each tick; the ending draws its own
        if (clientShot != null && tick >= shotAt) { Phase4Checks.shot(c, clientShot); clientShot = null; }
        if (resizeAt >= 0 && tick >= resizeAt) { resizeAt = -1; Phase1Checks.window(c, 854, 480, 2); }
        if (restoreAt >= 0 && tick >= restoreAt) { restoreAt = -1; Phase1Checks.window(c, 1280, 720, 3); }
        if (st != lastStep || !EndingClient.outcome.equals(lastOutcome)) {
            if (st == MatchEnding.Step.RESULT) { resultAt = tick; if (lastStep == null) { endings++; hands0 = EndingClient.handsHidden; } }
            if (st == MatchEnding.Step.PODIUM) podiumAt = tick;
            if (st == MatchEnding.Step.STATS) statsAt = tick;
            if (st == MatchEnding.Step.REWARDS) rewardsAt = tick;
            if (st == null && lastStep != null) endedAt = tick;
            if (lastStep == MatchEnding.Step.REWARDS && endings == 1 && !rewardsDone) rewardsCheck(c, "the step ended");
            if (st == MatchEnding.Step.RESULT && EndingClient.outcome.equals("DEFEAT") && !banners.containsKey("DEFEAT")) { Phase1Checks.window(c, 854, 480, 2); restoreAt = tick + 30; }
            lastStep = st; lastOutcome = EndingClient.outcome;
        }
        // ending-client-banner: the word drawn for each outcome, after its 6-tick scale-in
        if (st == MatchEnding.Step.RESULT && resultAt >= 0 && tick == resultAt + 16 && !pressMenu && !banners.containsKey(EndingClient.outcome)) {
            banners.put(EndingClient.outcome, EndingClient.bannerFrames > 0 ? EndingClient.bannerDrawn : "nothing");
            clientShot = "ending-banner-" + EndingClient.outcome.toLowerCase(Locale.ROOT); shotAt = tick;
        }
        // ending-client-frozen: a troop's animation clock, age and position across 20 ticks of RESULT (the first round)
        if (st == MatchEnding.Step.RESULT && !frozenDone && resultAt >= 0 && scenario == 0) {
            if (tick == resultAt + 4) {
                watched = null;
                for (Entity e : c.world.getEntities()) if (e instanceof MobEntity m && m.isAlive() && TroopModels.troopOf(m) != null) { watched = e; break; }
                if (watched != null) { clock0 = EndingClient.clock(watched); age0 = watched.age; pos0 = watched.getPos(); }
            }
            if (tick == resultAt + 24) {
                frozenDone = true;
                if (watched == null) report("ending-client-frozen", false, "no troop in the client world during RESULT");
                else {
                    double clock1 = EndingClient.clock(watched);
                    boolean same = clock1 == clock0 && watched.age == age0 && watched.getPos().equals(pos0) && EndingClient.frozen();
                    report("ending-client-frozen", same, TroopModels.troopOf(watched).id() + ": clock " + clock0 + " -> " + clock1 + " s, age " + age0 + " -> " + watched.age + ", moved "
                        + String.format(Locale.ROOT, "%.4f", watched.getPos().distanceTo(pos0)) + " over 20 ticks");
                }
            }
        }
        // ending-client-podium: the camera off the player near the view spot, figures on the stage (the first round), shot at both window sizes
        if (st == MatchEnding.Step.PODIUM && !podiumDone && podiumAt >= 0 && scenario == 0) {
            if (tick == podiumAt + 40) {
                double[] v = PodiumLayout.viewSpot(); var cam = c.gameRenderer.getCamera().getPos(); double[] p1 = PodiumLayout.plinths()[0];
                boolean near = EndingClient.figures.stream().anyMatch(f -> f.commander() && f.entity().squaredDistanceTo(p1[0], p1[1], p1[2]) < 1.5 * 1.5);
                double off = cam.distanceTo(new Vec3d(v[0], v[1], v[2])), eye = cam.distanceTo(c.player.getEyePos());
                report("ending-client-podium", EndingClient.cinematic() && off < 3.6 && eye > .2 && near,
                    String.format(Locale.ROOT, "camera %.2f blocks from the view spot, %.2f from the player's eye; ", off, eye) + EndingClient.figures.size() + " figures, rank 1 on plinth 1=" + near + ", plates " + EndingClient.plates.size());
                clientShot = "ending-podium"; shotAt = tick; resizeAt = tick + 3;
            }
            if (tick == podiumAt + 70) { clientShot = "ending-podium-small"; shotAt = tick; restoreAt = tick + 3; podiumDone = true; }
        }
        var due = later.headMap(tick, true);
        if (!due.isEmpty()) { var run = new ArrayList<Runnable>(); for (var l : due.values()) run.addAll(l); due.clear(); for (var r : run) r.run(); }
        task6(c, tick, st);
        // ending-client-skip: the menu key in RESULT moves this client to PODIUM at once; then Escape three times takes him to camp (ending-client-escape)
        if (pressMenu && st == MatchEnding.Step.RESULT && pressAt < 0) { pressAt = tick; KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(GravesAndGloryClient.menu)); }
        if (pressAt >= 0 && !skipDone && (st == MatchEnding.Step.PODIUM || tick - pressAt > 10)) {
            skipDone = true; pressMenu = false; escStage = 1;
            report("ending-client-skip", st == MatchEnding.Step.PODIUM, "menu key in RESULT: step " + st + " after " + (tick - pressAt) + " ticks");
        }
        // ending-client-plates: the 4v4 podium's three plates never overlap
        if (podiumThree && st == MatchEnding.Step.PODIUM && !threeDone && podiumAt >= 0 && tick >= podiumAt + 30) {
            threeDone = true;
            var r = new ArrayList<>(EndingClient.plateRects); boolean overlap = false;
            for (int i = 0; i < r.size(); i++) for (int j = i + 1; j < r.size(); j++) { int[] a = r.get(i), b = r.get(j); if (a[0] < b[2] && b[0] < a[2] && a[1] < b[3] && b[1] < a[3]) overlap = true; }
            StringBuilder why = new StringBuilder(); for (int[] a : r) why.append(Arrays.toString(a));
            report("ending-client-plates", r.size() == 3 && !overlap, r.size() + " plates, overlap=" + overlap + " " + why + " at " + c.getWindow().getScaledWidth() + "x" + c.getWindow().getScaledHeight());
            clientShot = "ending-podium-three"; shotAt = tick;
        }
    }

    /** The Task 6 client checks, each tick: the hand and the stats and rewards screens (the first ending), Escape (the second), where DONE lands, and the report's You face. */
    void task6(MinecraftClient c, int tick, MatchEnding.Step st) {
        // ending-client-hand: no first-person hand drawn during the first RESULT
        if (endings == 1 && st == MatchEnding.Step.RESULT && resultAt >= 0 && tick == resultAt + 20)
            report("ending-client-hand", EndingClient.handsHidden > hands0 + 5, (EndingClient.handsHidden - hands0) + " frames drew no hand in 20 ticks of RESULT");
        // ending-client-stats: the screen in STATS, every commander row and the four groups, fitted at 1280x720 gui 3 and at 854x480 gui 2 (427x240)
        if (endings == 1 && st == MatchEnding.Step.STATS && statsAt >= 0 && !statsDone) {
            if (tick == statsAt + 20) { statsNormal = statsState(c); Phase4Checks.shot(c, "ending-stats"); later(tick + 3, () -> Phase1Checks.window(c, 854, 480, 2)); }
            if (tick == statsAt + 50) {
                statsDone = true; String small = statsState(c);
                report("ending-client-stats", statsNormal.startsWith("ok") && small.startsWith("ok"), "1280x720 gui 3: " + statsNormal + " | 854x480 gui 2: " + small);
                Phase4Checks.shot(c, "ending-stats-small"); later(tick + 3, () -> Phase1Checks.window(c, 1280, 720, 3));
            }
        }
        // ending-client-rewards: the rows count up one at a time; by the step's end every row and the total are drawn (shots counting, small, final)
        if (endings == 1 && st == MatchEnding.Step.REWARDS && rewardsAt >= 0 && !rewardsDone) {
            if (tick == rewardsAt + 25) { rewardsMid = EndingPanels.rewardRows < EndingPanels.rewardsTotal || EndingPanels.rewardsTotal <= 1; Phase4Checks.shot(c, "ending-rewards-counting"); later(tick + 3, () -> Phase1Checks.window(c, 854, 480, 2)); }
            if (tick == rewardsAt + 78) { rewardsSmall = rewardsState(c); Phase4Checks.shot(c, "ending-rewards-small"); later(tick + 3, () -> Phase1Checks.window(c, 1280, 720, 3)); }
            if (tick == rewardsAt + 100) { Phase4Checks.shot(c, "ending-rewards"); rewardsCheck(c, "100 ticks in"); }
        }
        // ending-client-escape: Escape with no screen (PODIUM) and with the ending's screen (STATS, REWARDS) moves the step on, and no game menu opens
        if (escStage > 0 && escStage < 9 && c.currentScreen instanceof GameMenuScreen) menuSeen = true;
        if (escStage == 1 && st == MatchEnding.Step.PODIUM && c.currentScreen == null) { escape(c); escAt = tick; escStage = 2; }
        else if (escStage == 2 && (st == MatchEnding.Step.STATS || tick - escAt > 10) && tick - escAt >= 2) {
            escWhy = "PODIUM -> " + st + " (" + where(c) + ")";
            if (st == MatchEnding.Step.STATS && c.currentScreen instanceof EndingScreen) { escape(c); escAt = tick; escStage = 3; } else escStage = 8;
        } else if (escStage == 3 && (st == MatchEnding.Step.REWARDS || tick - escAt > 10) && tick - escAt >= 2) {
            escWhy += ", STATS -> " + st + " (" + where(c) + ")";
            if (st == MatchEnding.Step.REWARDS && c.currentScreen instanceof EndingScreen) { escape(c); escAt = tick; escStage = 4; } else escStage = 8;
        } else if (escStage == 4 && (st == null || st == MatchEnding.Step.DONE || tick - escAt > 10) && tick - escAt >= 2) {
            escWhy += ", REWARDS -> " + (st == null ? "ended" : st) + " (" + where(c) + ")";
            report("ending-client-escape", (st == null || st == MatchEnding.Step.DONE) && !menuSeen && !(c.currentScreen instanceof GameMenuScreen), escWhy + "; game menu seen=" + menuSeen);
            escStage = 9;
        }
        if (escStage == 8) { report("ending-client-escape", false, escWhy + "; game menu seen=" + menuSeen); escStage = 9; }
        // ending-client-room: where DONE lands, 10 ticks after the ending left: the War Table home with no room (first), the war council in a room (second)
        if (endedAt >= 0 && tick == endedAt + 10) {
            String at = where(c);
            if (endings == 1) { roomHome = at; youStage = 1; youAt = tick; }
            else if (endings == 2) {
                boolean lobby = c.currentScreen instanceof CampScreen s && s.page.equals("lobby");
                report("ending-client-room", roomHome.equals("CampScreen home") && lobby, "no room: " + roomHome + "; in a room (" + CampaignClient.social.get("room") + "): " + at);
                roomChecked = true;
            }
        }
        // ending-report-you: Last battle on the War Table home, then the You face: the four groups fitted, at both sizes; then the Battle face's table
        if (youStage > 0) reportYou(c, tick);
    }

    String statsState(MinecraftClient c) {
        boolean open = c.currentScreen instanceof EndingScreen && "stats".equals(EndingScreen.drawn);
        String score = EndingPanels.layout("score"), group = EndingPanels.layout("group");
        boolean ok = open && EndingPanels.scoreRows.size() == squads() && EndingPanels.groups.size() == 4 && score.isEmpty() && group.isEmpty();
        return (ok ? "ok" : "FAIL") + " screen=" + where(c) + " face=" + EndingScreen.drawn + " scale " + EndingScreen.scaleDrawn + ", rows " + EndingPanels.scoreRows + " of " + squads()
            + ", groups " + EndingPanels.groups + (score.isEmpty() ? "" : ", table: " + score) + (group.isEmpty() ? "" : ", groups: " + group);
    }
    String rewardsState(MinecraftClient c) {
        String lay = EndingPanels.layout("reward");
        boolean ok = c.currentScreen instanceof EndingScreen && "rewards".equals(EndingScreen.drawn) && EndingPanels.rewardRows == rewardsSize() && rewardsSize() > 0 && !EndingPanels.total.isEmpty() && lay.isEmpty();
        return (ok ? "ok" : "FAIL") + " screen=" + where(c) + " rows " + EndingPanels.rewardRows + " of " + rewardsSize() + ", total '" + EndingPanels.total + "', claim '" + EndingPanels.claim + "'" + (lay.isEmpty() ? "" : ", layout: " + lay);
    }
    void rewardsCheck(MinecraftClient c, String when) {
        if (rewardsDone) return;
        rewardsDone = true;
        String fin = rewardsState(c);
        report("ending-client-rewards", rewardsMid && rewardsSmall.startsWith("ok") && fin.startsWith("ok"), "rows not all shown 25 ticks in=" + rewardsMid + "; 854x480 gui 2: " + rewardsSmall + " | 1280x720 gui 3 (" + when + "): " + fin);
    }

    void reportYou(MinecraftClient c, int tick) {
        int k = tick - youAt;
        if (youStage == 1) { // Last battle on the home page
            var last = c.currentScreen instanceof CampScreen s && s.page.equals("home") && s.home != null ? s.home.button("last") : null;
            if (last != null) WarTableRecruit.press(last); else c.setScreen(new CampScreen("results"));
            youVia = last != null ? "via Last battle" : "opened directly (no Last battle tag: " + where(c) + ")";
            youStage = 2; youAt = tick; return;
        }
        if (youStage == 2 && k >= 10) {
            var tab = c.currentScreen instanceof CampScreen s && s.report != null ? s.report.button("tab-you") : null;
            if (tab == null) { report("ending-report-you", false, "the report did not open (" + where(c) + ", " + youVia + ")"); hold = false; youStage = 0; return; }
            WarTableRecruit.press(tab); youStage = 3; youAt = tick; return;
        }
        if (youStage == 3 && k >= 10) { youNormal = youState(); Phase4Checks.shot(c, "ending-report-you"); later(tick + 3, () -> Phase1Checks.window(c, 854, 480, 2)); youStage = 4; youAt = tick; return; }
        if (youStage == 4 && k >= 30) {
            String small = youState();
            report("ending-report-you", youNormal.startsWith("ok") && small.startsWith("ok"), youVia + "; 1280x720 gui 3: " + youNormal + " | 854x480 gui 2: " + small);
            Phase4Checks.shot(c, "ending-report-you-small");
            var tab = c.currentScreen instanceof CampScreen s && s.report != null ? s.report.button("tab-battle") : null;
            if (tab != null) WarTableRecruit.press(tab);
            youStage = 5; youAt = tick; return;
        }
        if (youStage == 5 && k >= 10) {
            String lay = EndingPanels.layout("score");
            report("ending-report-battle", lay.isEmpty() && EndingPanels.scoreRows.size() == squads() && "battle".equals(WarTableReport.texts().get("face")), "Battle face at 854x480 gui 2: rows " + EndingPanels.scoreRows + (lay.isEmpty() ? ", layout clean" : ", layout: " + lay));
            Phase4Checks.shot(c, "ending-report-battle-small");
            later(tick + 3, () -> Phase1Checks.window(c, 1280, 720, 3));
            youStage = 6; youAt = tick; return;
        }
        if (youStage == 6 && k >= 25) { Phase4Checks.shot(c, "ending-report-battle"); c.setScreen(null); youStage = 0; hold = false; }
    }
    String youState() {
        String lay = EndingPanels.layout("group"), troops = EndingPanels.layout("troops");
        boolean ok = "you".equals(WarTableReport.texts().get("face")) && EndingPanels.groups.equals(List.of("Commander", "Army", "Objectives", "Economy & orders")) && lay.isEmpty() && (troops.isEmpty() || troops.endsWith("not drawn"));
        return (ok ? "ok" : "FAIL") + " face " + WarTableReport.texts().get("face") + ", groups " + EndingPanels.groups + " (k " + UiCanvas.k() + ")" + (lay.isEmpty() ? "" : ", groups: " + lay) + (troops.isEmpty() ? "" : ", troops: " + troops);
    }

    static void report(String id, boolean pass, String why) { Phase4Checks.report(id, pass, why); }
    static JsonObject snap(Campaign c, ServerPlayerEntity p) { return JsonParser.parseString(c.snapshot(p)).getAsJsonObject(); }
    static String step(JsonObject snap) { return snap.has("ending") ? snap.getAsJsonObject("ending").get("step").getAsString() : "none"; }
    static void evidence(String name, String text) {
        try { Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("../evidence"); Files.createDirectories(dir); Files.writeString(dir.resolve(name), text); }
        catch (Exception e) { report("ending-evidence", false, name + ": " + e); }
    }
    static double toView(ServerPlayerEntity p) { double[] v = PodiumLayout.viewSpot(); return Math.sqrt(p.getEyePos().squaredDistanceTo(v[0], v[1], v[2])); }
    static int total(Campaign c) { var t = c.endingTiming; return t.resultTicks() + t.podiumTicks() + t.statsTicks() + t.rewardsTicks(); }
    void next() { scenario++; stage = 0; pos.clear(); hp.clear(); froze = podium = stats = rewards = stopped = false; if (scenario > 4) done = true; }

    /** Starts practice (or the 4v4 for the size check) once the lobby is quiet; true when the round is past its intro and the fixture is set. */
    boolean started(GravesAndGlory.Battle b, ServerPlayerEntity p, boolean big) {
        var c = b.campaign;
        if (stage == 0) {
            if (b.running) { c.returnLobby(); return false; }
            if (!b.ready) return false;
            if (savedProfile == null) savedProfile = Campaign.JSON.toJson(c.profile(player));
            c.notices.remove(player);
            if (big) { c.closeParty(); c.partyLeader = p.getUuid(); c.party.add(p.getUuid()); c.handle(p, "mode:4"); c.handle(p, "start"); }
            else { c.closeParty(); c.handle(p, "camp:practice"); }
            stage = 1; at = b.time; return false;
        }
        if (stage == 1) {
            if (b.time - at > 2000) { report("ending-start-" + scenario, false, "no round within 2000 ticks; notice=" + c.notices.getOrDefault(player, "")); next(); return false; }
            if (!b.running || b.time < c.introUntil + 20) return false;
            p.changeGameMode(GameMode.ADVENTURE); p.setHealth(p.getMaxHealth());
            b.roundStarted = b.time - 181 * 20; c.joinedAt.put(player, b.roundStarted);
            stage = 2; return true;
        }
        return stage >= 2;
    }

    public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || done) return;
        if (hold) { if (holdAt < 0) holdAt = b.time; if (b.time - holdAt > 2400) { hold = false; report("ending-report-you", false, "the client never finished the report checks"); } return; }
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        var c = b.campaign;
        switch (scenario) {
            case 0 -> full(b, c, p);
            case 1 -> skip(b, c, p);
            case 2 -> defeat(b, c, p);
            case 3 -> draw(b, c, p);
            case 4 -> size(b, c, p);
            default -> done = true;
        }
    }

    /** ending-steps, ending-freeze, ending-paid-once, ending-no-double-finish, ending-report, ending-stage. */
    void full(GravesAndGlory.Battle b, Campaign c, ServerPlayerEntity p) {
        if (stage < 2) { started(b, p, false); return; }
        var s = b.squads.get(player);
        if (stage == 2) {
            for (var u : b.units) if (u.mob.isAlive()) { pos.put(u.mob, u.mob.getPos()); hp.put(u.mob, u.mob.getHealth()); }
            c.finish(s.team);
            var pr = c.profile(player); gold1 = pr.gold; xp1 = pr.progression.xp;
            var res = c.results.get(player); resultId = res == null ? "" : res.get("id").getAsString();
            JsonObject sn = snap(c, p);
            var e = sn.has("ending") ? sn.getAsJsonObject("ending") : new JsonObject();
            boolean begun = c.ending() && b.running && step(sn).equals("RESULT") && e.has("outcome") && e.get("outcome").getAsString().equals("VICTORY");
            report("ending-begins", begun, "ending=" + c.ending() + " running=" + b.running + " snapshot ending=" + e);
            evidence("ending-report.json", Campaign.JSON.toJson(res)); evidence("ending-snapshot.json", Campaign.JSON.toJson(e));
            // the report
            boolean podiumRows = res != null && res.has("podium") && res.getAsJsonArray("podium").size() >= 1;
            boolean mvpFirst = false;
            if (podiumRows) { String top = res.getAsJsonArray("podium").get(0).getAsJsonObject().get("uuid").getAsString();
                for (var r : res.getAsJsonArray("squads")) { var o = r.getAsJsonObject(); if (o.get("uuid").getAsString().equals(top)) mvpFirst = o.get("mvp").getAsBoolean() && o.get("rank").getAsInt() == 1; } }
            boolean goldRow = false;
            if (res != null && res.has("rewards")) for (var r : res.getAsJsonArray("rewards")) if (r.getAsJsonObject().get("label").getAsString().equals("Gold") && r.getAsJsonObject().get("amount").getAsInt() > 0) goldRow = true;
            boolean you = res != null && res.has("you") && res.getAsJsonObject("you").has("commander") && res.getAsJsonObject("you").has("economy");
            boolean line = res != null && res.has("line") && res.get("line").getAsString().contains(" · ");
            report("ending-report", podiumRows && mvpFirst && goldRow && you && line && res.has("claimable"), "podium=" + podiumRows + " mvp rank 1=" + mvpFirst + " gold row=" + goldRow + " you=" + you + " line=" + (res == null ? "" : res.get("line")));
            // the freeze: a forced hit on the commander and on a troop, and an order
            float before = p.getHealth();
            var enemy = b.units.stream().filter(u -> u.squad.team != s.team && u.mob.isAlive()).findFirst().orElse(null);
            var own = b.units.stream().filter(u -> u.squad == s && u.mob.isAlive()).findFirst().orElse(null);
            if (enemy != null) p.damage(b.world.getDamageSources().mobAttack(enemy.mob), 6);
            if (enemy != null) enemy.mob.damage(b.world.getDamageSources().playerAttack(p), 6);
            String orderBefore = s.order; String other = Arrays.stream(Spec.ORDERS).map(o -> o.id()).filter(id -> !id.equals(orderBefore) && !id.equals("charge")).findFirst().orElse("attack");
            s.lastOrder = -100000; b.command(p, other);
            OrderRules.playerOrder(b, p, List.of(new OrderRules.At("all", p.getX() + 10, p.getZ())), "hold", Spec.FORMATIONS[0].id(), 0, -1);
            froze = p.getHealth() == before && s.order.equals(orderBefore) && own != null;
            if (!froze) report("ending-freeze", false, "health " + before + "->" + p.getHealth() + " order " + orderBefore + "->" + s.order + " (sent " + other + ")");
            b.roundStarted = b.time - b.roundSeconds * 20L - 200; // the round clock is out while the ending runs
            stage = 3; at = b.time; return;
        }
        long elapsed = b.time - c.endingStart;
        if (b.running && c.ending()) {
            if (elapsed >= 40 && !stopped) { stopped = true;
                double worst = 0; int changed = 0;
                for (var e : pos.entrySet()) if (!e.getKey().isRemoved()) { worst = Math.max(worst, e.getKey().getPos().distanceTo(e.getValue())); if (e.getKey().getHealth() != hp.get(e.getKey())) changed++; }
                if (froze) report("ending-freeze", worst <= 0.05 && changed == 0, pos.size() + " troops: worst drift " + String.format(Locale.ROOT, "%.3f", worst) + " blocks after 40 ticks, " + changed + " health changes; forced hits and an order refused");
                var res = c.results.get(player);
                report("ending-no-double-finish", res != null && res.get("id").getAsString().equals(resultId) && c.profile(player).gold == gold1 && c.ending(), "clock out during RESULT: result id kept=" + (res != null && res.get("id").getAsString().equals(resultId)) + " gold " + gold1 + "->" + c.profile(player).gold);
            }
            if (elapsed >= c.endingTiming.resultTicks() + 5 && !podium) { podium = true; String st = step(snap(c, p)); double d = toView(p);
                if (!st.equals("PODIUM") || d > 3) report("ending-steps", false, "at " + elapsed + " step " + st + " view distance " + d);
                else shot = "ending-podium"; }
            if (elapsed >= c.endingTiming.resultTicks() + c.endingTiming.podiumTicks() + 5 && !stats) { stats = true; String st = step(snap(c, p)); if (!st.equals("STATS")) report("ending-steps", false, "at " + elapsed + " step " + st); }
            if (elapsed >= c.endingTiming.resultTicks() + c.endingTiming.podiumTicks() + c.endingTiming.statsTicks() + 5 && !rewards) { rewards = true; String st = step(snap(c, p)); if (!st.equals("REWARDS")) report("ending-steps", false, "at " + elapsed + " step " + st); }
            if (elapsed > total(c) + 100) { report("ending-steps", false, "still ending " + elapsed + " ticks in"); c.returnLobby(); next(); }
            return;
        }
        long took = b.time - at; var pr = c.profile(player);
        report("ending-steps", podium && stats && rewards && !b.running && b.units.isEmpty() && CampWorld.inCamp(p.getX(), p.getY(), p.getZ()) && took >= total(c) - 2 && took <= total(c) + 5,
            "RESULT, PODIUM (within 3 blocks of the view spot), STATS, REWARDS seen=" + (podium && stats && rewards) + "; back in camp after " + took + " ticks (sequence " + total(c) + "), units " + b.units.size());
        report("ending-paid-once", pr.gold == gold1 && pr.progression.xp == xp1, "gold " + gold1 + "->" + pr.gold + " xp " + xp1 + "->" + pr.progression.xp);
        holdAt = -1; hold = true; // the client opens the War Table report (ending-report-you) before the next round starts
        next();
    }

    /** ending-skip: four skips from the client take the lone commander to camp before the clock's DONE. */
    void skip(GravesAndGlory.Battle b, Campaign c, ServerPlayerEntity p) {
        if (stage < 2) { started(b, p, false); return; }
        if (stage == 2) { // a room is open (ending-client-room: DONE lands on its war council); the client presses the menu key, then Escape three times
            c.handle(p, "social:lobby"); c.finish(b.squads.get(player).team); pressMenu = true; stage = 3; at = b.time; return; }
        long took = b.time - at;
        if (stage == 3) {
            if (b.running) { if (took > total(c) + 40) { report("ending-skip", false, "still running after " + took + " ticks"); c.returnLobby(); stage = 4; at = b.time; } return; }
            report("ending-skip", took < total(c) - 100 && CampWorld.inCamp(p.getX(), p.getY(), p.getZ()) && !c.ending(), "camp after " + took + " ticks of " + total(c) + " (skips sent from the client)");
            stage = 4; at = b.time; return;
        }
        if (!roomChecked && b.time - at < 300) return; // the room stays until the client has seen where DONE put him
        c.handle(p, "social:exit");
        next();
    }

    /** ending-defeat and ending-rejoin. */
    void defeat(GravesAndGlory.Battle b, Campaign c, ServerPlayerEntity p) {
        if (stage < 2) { started(b, p, false); return; }
        if (stage == 2) {
            c.finish(1 - b.squads.get(player).team);
            var sn = snap(c, p); var res = c.results.get(player);
            String out = sn.has("ending") ? sn.getAsJsonObject("ending").get("outcome").getAsString() : "none";
            report("ending-defeat", out.equals("DEFEAT") && res != null && res.get("outcome").getAsString().equals("DEFEAT"), "snapshot " + out + ", report " + (res == null ? "none" : res.get("outcome")));
            stage = 3; at = b.time; return;
        }
        long elapsed = b.time - c.endingStart;
        if (stage == 3) {
            if (!c.ending()) { report("ending-rejoin", false, "the ending stopped before STATS"); next(); return; }
            if (elapsed < c.endingTiming.resultTicks() + c.endingTiming.podiumTicks() + 20) return;
            c.participants.remove(player); c.joined(p); stage = 4; at = b.time; return;
        }
        if (stage == 4) {
            boolean camp = CampWorld.inCamp(p.getX(), p.getY(), p.getZ());
            report("ending-rejoin", camp && !snap(c, p).has("ending") && c.results.containsKey(player), "rejoined during STATS: in camp=" + camp + " at " + p.getPos() + ", report kept=" + c.results.containsKey(player));
            stage = 5; at = b.time; return;
        }
        if (b.running && b.time - at < 200) return;
        if (b.running) c.returnLobby();
        next();
    }

    /** ending-draw, ending-respawn-pending and ending-stop. */
    void draw(GravesAndGlory.Battle b, Campaign c, ServerPlayerEntity p) {
        if (stage < 2) { started(b, p, false); return; }
        if (stage == 2) {
            b.respawns.put(player, b.time + 10); p.changeGameMode(GameMode.SPECTATOR); // waiting to respawn, due during RESULT
            c.finish(-1);
            var sn = snap(c, p); var res = c.results.get(player);
            String out = sn.has("ending") ? sn.getAsJsonObject("ending").get("outcome").getAsString() : "none";
            report("ending-draw", out.equals("DRAW") && res != null && res.get("outcome").getAsString().equals("DRAW"), "snapshot " + out + ", report " + (res == null ? "none" : res.get("outcome")));
            stage = 3; at = b.time; return;
        }
        long elapsed = b.time - c.endingStart;
        if (!c.ending()) { report("ending-stop", false, "the ending stopped early"); next(); return; }
        if (stage == 3 && elapsed >= 40) {
            if (!p.isSpectator()) { report("ending-respawn-pending", false, "respawned during RESULT: mode " + p.interactionManager.getGameMode()); next(); c.returnLobby(); return; }
            stage = 4;
        }
        if (stage == 4 && elapsed >= c.endingTiming.resultTicks() + 5) {
            String st = step(snap(c, p)); double d = toView(p);
            report("ending-respawn-pending", st.equals("PODIUM") && d <= 3 && p.isSpectator(), "not respawned in RESULT; at " + elapsed + " step " + st + ", " + String.format(Locale.ROOT, "%.1f", d) + " blocks from the view spot");
            stage = 5;
        }
        if (stage == 5 && elapsed >= c.endingTiming.resultTicks() + c.endingTiming.podiumTicks() + 20) {
            int gold = c.profile(player).gold;
            c.voidMatch(MatchRules.Ending.STOPPED);
            report("ending-stop", !b.running && !c.ending() && b.units.isEmpty() && c.profile(player).gold == gold && c.results.containsKey(player),
                "stop during STATS: running=" + b.running + " units=" + b.units.size() + " gold " + gold + "->" + c.profile(player).gold + " report kept=" + c.results.containsKey(player));
            next();
        }
    }

    /** ending-snapshot-size: a 4v4 report and snapshot fit the limit the client reads. */
    void size(GravesAndGlory.Battle b, Campaign c, ServerPlayerEntity p) {
        if (stage < 2) { started(b, p, true); return; }
        if (stage == 3) { sizeHold(b, c); return; }
        c.finish(b.squads.get(player).team);
        String raw = Campaign.JSON.toJson(c.results.get(player)), sent = c.snapshot(p);
        var sn = JsonParser.parseString(sent).getAsJsonObject();
        boolean whole = sn.has("result") && sn.getAsJsonObject("result").has("podium") && sn.getAsJsonObject("result").getAsJsonArray("squads").size() == 8 && sn.has("ending");
        report("ending-snapshot-size", sent.length() <= Campaign.SNAPSHOT_LIMIT && whole, "4v4: snapshot " + sent.length() + " of " + Campaign.SNAPSHOT_LIMIT + " characters with the report (" + raw.length() + " pretty) and ending=" + whole + " (" + b.squads.size() + " squads)");
        evidence("ending-report-4v4.json", raw);
        podiumThree = true; stage = 3; at = b.time;
    }
    /** The 4v4 ending held into PODIUM for the client's three plates (ending-client-plates), then torn down. */
    void sizeHold(GravesAndGlory.Battle b, Campaign c) {
        if (c.ending() && b.time - at < c.endingTiming.resultTicks() + 80) return;
        c.returnLobby();
        next();
    }
}
