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

/**
 * The match ending on a real integrated server (GRAVES_PHASE4_CHECKS=ending, run with GRAVES_INSTANT_ENDING=false). Five rounds, each started
 * as practice (the last a 4v4) on the elapsed-time fixture the other suites use (roundStarted 181 s back): the full sequence (steps, freeze, paid
 * once, no second finish, report, podium spot), the skip, a defeat with a rejoin, a draw with a commander waiting to respawn then a server stop,
 * and the 4v4 snapshot size. The report and snapshot of the first round are written to evidence/ending-report.json and ending-snapshot.json.
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
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); }
        if (skips > 0 && tick % 3 == 0) { GravesAndGloryClient.send("ending:skip"); skips--; }
        String s = shot;
        if (s != null) { if (s.startsWith("!")) { Phase4Checks.shot(c, s.substring(1)); shot = null; } else shot = "!" + s; }
        if (done) Phase4Checks.finish(c, "ending");
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
        next();
    }

    /** ending-skip: four skips from the client take the lone commander to camp before the clock's DONE. */
    void skip(GravesAndGlory.Battle b, Campaign c, ServerPlayerEntity p) {
        if (stage < 2) { started(b, p, false); return; }
        if (stage == 2) { c.finish(b.squads.get(player).team); skips = 4; stage = 3; at = b.time; return; }
        long took = b.time - at;
        if (b.running) { if (took > total(c) + 40) { report("ending-skip", false, "still running after " + took + " ticks"); c.returnLobby(); next(); } return; }
        report("ending-skip", took < total(c) - 100 && CampWorld.inCamp(p.getX(), p.getY(), p.getZ()) && !c.ending(), "camp after " + took + " ticks of " + total(c) + " (skips sent from the client)");
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
        c.finish(b.squads.get(player).team);
        String raw = Campaign.JSON.toJson(c.results.get(player)), sent = c.snapshot(p);
        var sn = JsonParser.parseString(sent).getAsJsonObject();
        boolean whole = sn.has("result") && sn.getAsJsonObject("result").has("podium") && sn.getAsJsonObject("result").getAsJsonArray("squads").size() == 8 && sn.has("ending");
        report("ending-snapshot-size", sent.length() <= Campaign.SNAPSHOT_LIMIT && whole, "4v4: snapshot " + sent.length() + " of " + Campaign.SNAPSHOT_LIMIT + " characters with the report (" + raw.length() + " pretty) and ending=" + whole + " (" + b.squads.size() + " squads)");
        evidence("ending-report-4v4.json", raw);
        c.returnLobby();
        next();
    }
}
