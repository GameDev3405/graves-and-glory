package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Suites levels, gear and armoury of the in-match progression plan (Tasks 4, 5 and 8, lane A). Never included in releases.
 * Phase2Checks.SUITES takes these entries through register (GRAVES_PHASE2_CHECKS=<suite>), and the checks print through Phase2Checks.report.
 * Task 4 builds levels; gear (Task 5) and armoury (Task 8) still report fail p2-<suite>-not-built.
 */
final class LevelChecks {
    private LevelChecks() {}

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("levels", Levels::new);
        suites.put("gear", () -> new NotBuilt("gear", "Task 5 (lane A): gear, the gear command, the Armoury state, bot buys"));
        suites.put("armoury", () -> new NotBuilt("armoury", "Task 8: the Armoury tab"));
    }

    /** A suite whose checks are not written yet: one fail line naming the task that writes them, then the suite ends. */
    static final class NotBuilt implements Phase2Checks.Suite {
        final String suite, task;
        NotBuilt(String suite, String task) { this.suite = suite; this.task = task; }
        public void step(MinecraftClient c, int tick) {
            if (tick != 1) return;
            Phase2Checks.report("p2-" + suite + "-not-built", false, "suite " + suite + " is a skeleton until " + task);
            Phase2Checks.finish(c, suite);
        }
    }

    /**
     * Task 4: kill credit and levels (in-match progression design 3 and 7). The fixture is Phase2Checks.Staged's: a stone platform in the sky east of
     * Dawn's base, two ad hoc bot armies and the commander's own. Troops are posted frozen and pinned; every blow goes through Combat.resolve, a player's
     * blow through damage, a fall through damage with no attacker, so allowDamage, ALLOW_DEATH and AFTER_DEATH run as they do in a round.
     */
    static final class Levels extends Phase2Checks.Staged {
        static final String NOTICE = "Levels check: this notice expires";
        GravesAndGlory.Squad dawn2;
        // the stages' troops
        GravesAndGlory.Unit k, v, a1, a2, a3, m1, m2, by, helper, target, arch, flashUnit;
        boolean killerOk, didA2, didFall, didMend, noticeGone;
        String killerWhy = "", mendWhy = "", noticeWhy = "";
        int kills0, heard0, flashes0, rankSends0;
        Map<String, Integer> types0 = new HashMap<>();
        // the client half of p2-rank-sent: the entity whose rank the client must hold at rankWant
        volatile int rankId = -1, rankWant;
        volatile String rankSeen;
        String rankWhy = "";
        boolean rankOk;
        // the profile entry p2-level-name-veteran changes, to restore
        Profile.Owned knightOwned;
        boolean knightHad;
        int knightMastery;

        /** A real ranged blow past the hurt cooldown, through Combat.resolve. */
        static float shoot(GravesAndGlory.Battle b, GravesAndGlory.Unit a, LivingEntity t, float base) { t.timeUntilRegen = 0; t.hurtTime = 0; return Combat.resolve(b, a, t, base, "arrow", false); }
        /** A real melee blow on any entity (a player too), past the hurt cooldown. */
        static float blow(GravesAndGlory.Battle b, GravesAndGlory.Unit a, LivingEntity t, float base) { t.timeUntilRegen = 0; t.hurtTime = 0; return Combat.resolve(b, a, t, base, "melee", false); }
        /** The victim at 1 health takes a killing blow from the attacker. */
        static void kill(GravesAndGlory.Battle b, GravesAndGlory.Unit a, GravesAndGlory.Unit victim) { victim.mob.setHealth(1f); strike(b, a, victim, 50f); }
        /** A fall with no attacker, past the hurt cooldown: nobody's kill. */
        static void fall(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { u.mob.timeUntilRegen = 0; u.mob.hurtTime = 0; u.mob.damage(b.world.getDamageSources().generic(), 10000f); }
        /** A victim that the stage's small blows never kill (4000 health). */
        GravesAndGlory.Unit victim(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troop, double dx, double dz) { return tough(post(b, s, troop, dx, dz, 180, true)); }
        GravesAndGlory.Unit at(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String troop, double dx, double dz) { return post(b, s, troop, dx, dz, s.team == 0 ? 0 : 180, true); }
        /** credits/level of each unit, for the reports. */
        static String cl(GravesAndGlory.Unit... us) {
            StringBuilder sb = new StringBuilder();
            for (var u : us) { if (sb.length() > 0) sb.append(", "); sb.append(u.spec.id()).append('#').append(u.mob.getId()).append(' ').append(u.credits).append('/').append(u.level).append(u.mob.isAlive() ? "" : " (dead)"); }
            return sb.toString();
        }
        /** A mark in the progression log: its newest event now (null when it is empty). */
        static Veterans.Logged mark() { return Veterans.LOG.peekLast(); }
        /** The progression events logged after the mark, of a kind (any with null), for a unit (any unit with -2). A mark the bounded log has dropped counts every event as new. */
        static List<Veterans.Logged> events(Veterans.Logged mark, int unit, String kind) {
            List<Veterans.Logged> out = new ArrayList<>();
            boolean on = mark == null || !Veterans.LOG.contains(mark);
            for (var e : Veterans.LOG) {
                if (!on) { on = e == mark; continue; }
                if ((unit == -2 || e.unit() == unit) && (kind == null || e.kind().equals(kind))) out.add(e);
            }
            return out;
        }
        static List<Integer> values(List<Veterans.Logged> es) { List<Integer> out = new ArrayList<>(); for (var e : es) out.add(e.value()); return out; }
        static String name(GravesAndGlory.Unit u) { var n = u.mob.getCustomName(); return n == null ? "" : n.getString(); }
        /** The commander on the platform at (dx, dz) from its centre, in Adventure, facing yaw and looking down by pitch. */
        void stand(GravesAndGlory.Battle b, double dx, double dz, float yaw, float pitch) {
            var p = sp(b); if (p == null) return;
            p.changeGameMode(GameMode.ADVENTURE);
            b.relocate(p, site.getX() + .5 + dx, site.getY(), site.getZ() + .5 + dz, yaw, pitch);
        }
        /** The commander stands again at once after a death the stage dealt him (no respawn wait), back on the platform. */
        void revive(GravesAndGlory.Battle b) {
            var p = sp(b); if (p == null) return;
            b.respawns.remove(player); p.changeGameMode(GameMode.ADVENTURE); b.equipPlayer(p); stand(b, -10, 0, -90f, 10f);
        }

        public void cleanup(MinecraftClient c) {
            Phase2Checks.server(c, b -> {
                Veterans.override = null; Houses.forceRise = null; b.mists.clear();
                clearFixture(b, "cleanup");
                if (site != null) platform(b, site, 12, net.minecraft.block.Blocks.AIR.getDefaultState());
                unforce(b);
                restoreKnight(b);
                b.respawns.remove(player);
                if (b.running) b.campaign.returnLobby();
            });
        }
        void restoreKnight(GravesAndGlory.Battle b) {
            if (knightOwned == null) return;
            var pr = profile(b);
            if (knightHad) knightOwned.mastery = knightMastery; else pr.owned.remove("knight");
            knightOwned = null;
        }

        public void step(MinecraftClient c, int tick) {
            if (tick == 20) { c.setScreen(null); UUID id = c.player.getUuid(); Phase2Checks.server(c, b -> { player = id; next(b, 1); }); }
            if (wantShot != null && !shotTaken && ++shotDelay >= shotAfter) { Phase2Checks.shot(c, wantShot); shotTaken = true; }
            int id = rankId;
            if (id >= 0 && rankSeen == null && c.world != null) {
                var e = c.world.getEntityById(id); var r = RankSync.client(e);
                if (r != null && r.level() == rankWant) rankSeen = r + " (rank_v1 packets read " + RankSync.received + ")";
            }
            if (stage == 99 && !finished) { finished = true; c.setScreen(null); Phase2Checks.finish(c, "levels"); }
            if (tick > 12000 && !finished) { finished = true; Phase2Checks.report("p2-levels-timeout", false, "stuck in stage " + stage); Phase2Checks.finish(c, "levels"); }
        }

        public void serverTick(GravesAndGlory.Battle b) {
            if (player == null || stage >= 99) return;
            try { stages(b); } catch (RuntimeException e) { Phase2Checks.report("p2-levels-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, 99); }
        }

        void stages(GravesAndGlory.Battle b) {
            hold();
            switch (stage) {
                case 1 -> { var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return; next(b, 2); }
                case 2 -> {
                    if (since(b) < 10) return;
                    stageFixture(b); b.campaign.introUntil = b.time; Veterans.override = null; Houses.forceRise = Boolean.FALSE;
                    dawn2 = new GravesAndGlory.Squad(UUID.randomUUID(), 0, GravesAndGlory.Battle.base(0)); dawn2.order = "hold"; dawn2.bot = true;
                    stand(b, -10, 0, -90f, 10f);
                    var sheet = Spec.PROGRESSION[0];
                    boolean rows = sheet.enabled() && sheet.assistSeconds() == 10 && sheet.commanderBounty() == 3 && sheet.feedLevel() == 3 && Veterans.window() == 200 && Veterans.enabled();
                    Phase2Checks.report("p2-levels-sheet", rows, "the progression row " + sheet + " (want enabled, 10 s, bounty 3, feed level 3), the credit window " + Veterans.window() + " ticks (want 200), progression on " + Veterans.enabled());
                    next(b, 10);
                }
                case 10 -> { // p2-credit-killer: one Dawn Squire kills a level-1 Dusk Squire; then an archer's shot lands after the archer fell
                    k = at(b, dawn, "knight", -4, -2); v = at(b, dusk, "knight", -4, 0);
                    var t = mark(); kill(b, k, v);
                    var kills = events(t, -2, "kill");
                    killerOk = !v.mob.isAlive() && k.credits == 1 && k.level == 1 && name(k).equals("Order Squire") && kills.size() == 1 && kills.get(0).unit() == k.mob.getId() && kills.get(0).value() == 1;
                    killerWhy = "a Squire's blow killed a level-1 Dusk Squire (dead " + !v.mob.isAlive() + "): " + cl(k) + " (want 1/1), name \"" + name(k) + "\", kill events " + kills.size() + (kills.isEmpty() ? "" : " (" + kills.get(0).detail() + ")") + "; ";
                    arch = at(b, dawn, "archer", 4, -6); helper = at(b, dawn, "knight", 4, -2); target = victim(b, dusk, "knight", 4, 0);
                    strike(b, helper, target, 1f); target.mob.setHealth(1f);
                    b.shots.add(new Projectiles.Shot(arch, dawn, target.mob, "arrow", 50f, arch.mob.getEyePos(), b.time, b.time + 4, null, null));
                    fall(b, arch); // the archer falls before his arrow lands
                    next(b, 11);
                }
                case 11 -> {
                    if (since(b) < 10) return;
                    boolean late = !target.mob.isAlive() && !arch.mob.isAlive() && arch.credits == 0 && helper.credits == 1;
                    killerOk &= late;
                    killerWhy += "an arrow landing after its archer fell killed a Squire (dead " + !target.mob.isAlive() + ") that a Dawn Squire had struck: " + cl(arch, helper) + " (want the archer 0, the Squire 1)=" + late;
                    Phase2Checks.report("p2-credit-killer", killerOk, killerWhy);
                    clearFixture(b, "killer"); next(b, 20);
                }
                case 20 -> { // p2-credit-assist, p2-credit-support, p2-mastery-unchanged, p2-notice-expires: hits at t, t+6 s, mends at t+7 s, the kill at t+11 s
                    a1 = at(b, dawn, "archer", -8, -8); a2 = at(b, dawn, "archer", -2, -8); a3 = at(b, dawn, "archer", 2, -8);
                    k = at(b, dawn, "knight", -6, -2); v = victim(b, dusk, "knight", -6, 0);
                    m1 = at(b, dawn, "mage", -6, -6); by = at(b, dawn, "knight", 8, 6); m2 = at(b, dawn, "mage", 8, 9);
                    shoot(b, a1, v.mob, 1f);
                    kills0 = dawn.stats.kills; types0 = new HashMap<>(dawn.stats.troopKills);
                    b.campaign.notice(player, NOTICE);
                    didA2 = didFall = didMend = false; t0 = b.time; next(b, 21);
                }
                case 21 -> {
                    long t = since(b);
                    if (t >= 120 && !didA2) { didA2 = true; shoot(b, a2, v.mob, 1f); shoot(b, a3, v.mob, 1f); }
                    if (t >= 130 && !didFall) { didFall = true; fall(b, a3); } // a helper already dead when the kill comes
                    if (t >= 140 && !didMend) {
                        didMend = true;
                        k.mob.setHealth(k.mob.getMaxHealth() - 6); by.mob.setHealth(by.mob.getMaxHealth() - 6);
                        int before = b.shots.size();
                        m1.abilityAt = 0; m2.abilityAt = 0; Abilities.tick(b, m1, null, b.time); Abilities.tick(b, m2, null, b.time); Phase1Checks.freeze(m1); Phase1Checks.freeze(m2);
                        LivingEntity to1 = null, to2 = null;
                        for (var s : b.shots.subList(before, b.shots.size())) if (s.kind().equals("mend")) { if (s.shooter() == m1) to1 = s.target(); if (s.shooter() == m2) to2 = s.target(); }
                        mendWhy = "the first Sunweaver's mend went to " + (to1 == k.mob ? "the killer" : to1 == null ? "nobody" : "#" + to1.getId()) + " and the second's to " + (to2 == by.mob ? "a bystander" : to2 == null ? "nobody" : "#" + to2.getId()) + "; ";
                        if (to1 != k.mob || to2 != by.mob) mendWhy += "(BAD fixture) ";
                    }
                    if (t == 100) { // half way, the notice still stands
                        noticeWhy = "at 5 s the server still holds the notice=" + NOTICE.equals(b.campaign.notices.get(player)) + "; ";
                        noticeGone = NOTICE.equals(b.campaign.notices.get(player));
                    }
                    if (t < 220) return;
                    { // the notice posted at t: gone from the camp snapshot after 10 s (Campaign.expireNotices runs once a second, before this hook in the tick)
                        var p = sp(b); String inSnapshot = p == null ? "?" : com.google.gson.JsonParser.parseString(b.campaign.snapshot(p)).getAsJsonObject().get("notice").getAsString();
                        boolean gone = !NOTICE.equals(b.campaign.notices.get(player)) && !NOTICE.equals(inSnapshot);
                        noticeGone &= gone;
                        noticeWhy += "after " + t + " ticks the server holds \"" + b.campaign.notices.getOrDefault(player, "") + "\" and the snapshot \"" + inSnapshot + "\" (want the notice gone)=" + gone + "; ";
                    }
                    kill(b, k, v);
                    boolean assist = !v.mob.isAlive() && k.credits == 1 && a2.credits == 1 && a1.credits == 0 && !a3.mob.isAlive() && a3.credits == 0;
                    Phase2Checks.report("p2-credit-assist", assist, "an archer hit the Dusk Squire at t, a second at t+6 s, a third at t+6 s and fell at t+6.5 s, a Squire killed it at t+11 s (dead " + !v.mob.isAlive() + "): " + cl(k, a2, a1, a3) + " (want the Squire 1, the archer of t+6 s 1, the archer of t 0: 11 s is outside the 10 s window, the fallen archer 0)");
                    boolean support = m1.credits == 1 && m2.credits == 0 && by.credits == 0 && mendWhy.indexOf("BAD") < 0;
                    Phase2Checks.report("p2-credit-support", support, mendWhy + "at the kill 4 s later: " + cl(m1, m2, by) + " (want the killer's mender 1, the bystander's mender 0, the bystander 0)");
                    var types = dawn.stats.troopKills;
                    boolean mastery = dawn.stats.kills == kills0 + 1 && types.getOrDefault("knight", 0) == types0.getOrDefault("knight", 0) + 1 && types.getOrDefault("archer", 0).equals(types0.getOrDefault("archer", 0)) && types.getOrDefault("mage", 0).equals(types0.getOrDefault("mage", 0));
                    Phase2Checks.report("p2-mastery-unchanged", mastery, "one kill with three helpers and a mender: the army's kills +" + (dawn.stats.kills - kills0) + " (want 1), troop kills " + types0 + " -> " + types + " (want knight +1 only: assists add nothing)");
                    var p = sp(b);
                    b.campaign.notice(player, NOTICE);
                    String again = p == null ? "?" : com.google.gson.JsonParser.parseString(b.campaign.snapshot(p)).getAsJsonObject().get("notice").getAsString();
                    boolean back = NOTICE.equals(again);
                    Phase2Checks.report("p2-notice-expires", noticeGone && back, noticeWhy + "the same words posted again show in the snapshot: \"" + again + "\"=" + back);
                    b.campaign.notices.remove(player);
                    clearFixture(b, "assist"); next(b, 30);
                }
                case 30 -> { // p2-credit-slow: a Barrow Stalker's frostshot slows the victim (no blow of his), a Squire kills
                    var st = at(b, dawn, "stalker", 0, -8); k = at(b, dawn, "knight", 0, -2); v = victim(b, dusk, "knight", 0, 0);
                    var row = Abilities.row(st.spec);
                    Abilities.slow(b, st, v.mob, row);
                    boolean slowed = v.mob.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS) && v.mob.getHealth() == v.mob.getMaxHealth();
                    kill(b, k, v);
                    boolean ok = slowed && !v.mob.isAlive() && st.credits == 1 && k.credits == 1 && row.status().equals("slow");
                    Phase2Checks.report("p2-credit-slow", ok, "the Stalker's " + row.id() + " (" + row.mechanic() + ", status " + row.status() + ") slowed the Dusk Squire without a blow (slowed and unhurt " + slowed + "), a Squire killed it: " + cl(st, k) + " (want 1 each)");
                    clearFixture(b, "slow"); next(b, 31);
                }
                case 31 -> { // p2-credit-zone: an enemy held in a Mist Caller's cloud dies
                    var mc = at(b, dawn, "tc_mist", 0, -8); k = at(b, dawn, "knight", 4, -2); v = victim(b, dusk, "knight", 4, 0);
                    b.mists.add(new Abilities.Mist(0, v.mob.getPos().add(0, v.mob.getHeight() / 2, 0), 2.5, b.time + 200, mc));
                    m1 = mc; next(b, 32);
                }
                case 32 -> {
                    if (since(b) < Abilities.BEAT + 2) return; // a beat has held the victim in the cloud
                    boolean slowed = v.mob.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
                    kill(b, k, v); b.mists.clear();
                    boolean ok = slowed && !v.mob.isAlive() && m1.credits == 1 && k.credits == 1;
                    Phase2Checks.report("p2-credit-zone", ok, "a Dusk Squire stood in the Mist Caller's cloud for a beat (slowed " + slowed + "), a Squire killed it: " + cl(m1, k) + " (want the caller 1, the Squire 1)");
                    clearFixture(b, "zone"); next(b, 40);
                }
                case 40 -> { // p2-credit-allied: a helper of a second Dawn army gets credit
                    var h = at(b, dawn2, "archer", 0, -8); k = at(b, dawn, "knight", 0, -2); v = victim(b, dusk, "knight", 0, 0);
                    shoot(b, h, v.mob, 1f); kill(b, k, v);
                    boolean ok = !v.mob.isAlive() && h.credits == 1 && k.credits == 1 && h.squad != k.squad && h.squad.team == k.squad.team;
                    Phase2Checks.report("p2-credit-allied", ok, "an archer of a second Dawn army hit the Dusk Squire and a Squire of the first killed it: " + cl(h, k) + " (want 1 each)");
                    clearFixture(b, "allied"); next(b, 41);
                }
                case 41 -> { // p2-bounty: a level-3 victim and two helpers: each of the three gets 3
                    var h1 = at(b, dawn, "archer", -2, -8); var h2 = at(b, dawn, "archer", 2, -8); k = at(b, dawn, "knight", 0, -2); v = victim(b, dusk, "knight", 0, 0);
                    v.level = 3; v.credits = 8;
                    shoot(b, h1, v.mob, 1f); shoot(b, h2, v.mob, 1f); var t = mark(); kill(b, k, v);
                    var kills = events(t, -2, "kill");
                    boolean ok = !v.mob.isAlive() && h1.credits == 3 && h2.credits == 3 && k.credits == 3 && h1.level == 2 && h2.level == 2 && k.level == 2 && kills.size() == 1 && kills.get(0).value() == 3;
                    Phase2Checks.report("p2-bounty", ok, "a level-3 Dusk Squire killed by a Squire with two archers' help: " + cl(k, h1, h2) + " (want 3/2 each: the bounty is the victim's level), kill " + (kills.isEmpty() ? "none" : kills.get(0).detail()));
                    clearFixture(b, "bounty"); next(b, 50);
                }
                case 50 -> { // p2-level-skip and p2-rank-sent: 9 credits at once from level 1
                    var u = at(b, dawn, "knight", 0, -2); u.mob.setHealth(20f);
                    float max0 = u.mob.getMaxHealth(); int ups0 = Veterans.levelUps, feeds0 = Veterans.pendingFeed.size(); rankSends0 = RankSync.changeSends; var t = mark();
                    boolean tracked = net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(u.mob).contains(sp(b));
                    Veterans.credit(b, u, 9);
                    var levels = values(events(t, u.mob.getId(), "level")); var ranks = values(events(t, u.mob.getId(), "rank")); var feeds = events(t, u.mob.getId(), "feed");
                    double wantMax = u.spec.health() * LevelMath.healthFactor(3, 1);
                    var feed = Veterans.pendingFeed.size() > feeds0 ? Veterans.pendingFeed.get(Veterans.pendingFeed.size() - 1) : null;
                    boolean ok = u.level == 3 && u.credits == 9 && Math.abs(u.mob.getMaxHealth() - wantMax) < 1e-3 && Math.abs(u.mob.getHealth() - (20 + (wantMax - max0))) < 1e-3
                        && levels.equals(List.of(2, 3)) && Veterans.levelUps == ups0 + 2 && feeds.size() == 1 && feed != null && Veterans.pendingFeed.size() == feeds0 + 1 && feed.team() == 0 && feed.levelName().equals("Knight-Captain") && name(u).equals("Knight-Captain");
                    Phase2Checks.report("p2-level-skip", ok, "a Squire at 0 credits and 20 of " + fmt(max0) + " health got 9: " + cl(u) + " (want 9/3), max health " + fmt(u.mob.getMaxHealth()) + " (want " + fmt(wantMax) + ", two steps of +5%), health " + fmt(u.mob.getHealth()) + " (want " + fmt(20 + wantMax - max0) + ": raised by the added health only), level ups logged " + levels + " (want [2, 3]), feed lines " + feeds.size() + " (want 1: " + (feed == null ? "none" : feed.levelName() + " for team " + feed.team()) + "), name \"" + name(u) + "\"");
                    rankOk = ranks.equals(List.of(2, 3)) && (!tracked || RankSync.changeSends >= rankSends0 + 2);
                    rankWhy = "each level up sent the rank: logged " + ranks + " (want [2, 3]), rank_v1 change packets +" + (RankSync.changeSends - rankSends0) + " to the " + (tracked ? "tracking" : "untracking") + " commander; ";
                    rankSeen = null; rankWant = 3; rankId = u.mob.getId(); flashUnit = u; next(b, 51);
                }
                case 51 -> {
                    if (rankSeen == null && since(b) < 80) return;
                    String seen = rankSeen; rankId = -1;
                    boolean ok = rankOk && seen != null;
                    Phase2Checks.report("p2-rank-sent", ok, rankWhy + "the client holds for him " + (seen == null ? "nothing at level 3 after 80 ticks" : seen));
                    clearFixture(b, "skip"); next(b, 52);
                }
                case 52 -> { // p2-level-up: 3 credits make an Order Squire a Knight with x1.05 health, current health raised by the delta only
                    var u = at(b, dawn, "knight", 0, -2); u.mob.setHealth(10f);
                    double base = u.spec.health(), wantMax = base * LevelMath.healthFactor(2, 1), wantHealth = 10 + base * (LevelMath.healthFactor(2, 1) - 1);
                    boolean fresh = u.level == 1 && name(u).equals("Order Squire") && Math.abs(u.mob.getMaxHealth() - base) < 1e-3;
                    Veterans.credit(b, u, 2); boolean still = u.level == 1 && u.credits == 2;
                    Veterans.credit(b, u, 1);
                    boolean ok = fresh && still && u.level == 2 && name(u).equals("Knight") && Math.abs(u.mob.getMaxHealth() - wantMax) < 1e-3 && Math.abs(u.mob.getHealth() - wantHealth) < 1e-3;
                    Phase2Checks.report("p2-level-up", ok, "an Order Squire (fresh: level 1, \"Order Squire\", " + fmt(base) + " health=" + fresh + ") at 10 health: 2 credits keep him level 1=" + still + ", the third makes him " + cl(u) + " named \"" + name(u) + "\" (want Knight) with max health " + fmt(u.mob.getMaxHealth()) + " (want " + fmt(wantMax) + ") and health " + fmt(u.mob.getHealth()) + " (want " + fmt(wantHealth) + ")");
                    clearFixture(b, "level-up"); next(b, 53);
                }
                case 53 -> { // p2-level-name-veteran: a Mastery II Squire of the commander's own army at level 3
                    var pr = profile(b); knightOwned = pr.owned.get("knight"); knightHad = knightOwned != null;
                    if (!knightHad) { knightOwned = new Profile.Owned(); pr.owned.put("knight", knightOwned); }
                    knightMastery = knightOwned.mastery; knightOwned.mastery = 2;
                    var u = at(b, own, "knight", 0, -2); String fresh = name(u);
                    Veterans.credit(b, u, 8);
                    boolean ok = fresh.equals("Order Squire" + ArmyMobs.VETERAN) && u.level == 3 && name(u).equals("Knight-Captain (Veteran)");
                    Phase2Checks.report("p2-level-name-veteran", ok, "a Mastery II Order Squire of the commander's army spawns as \"" + fresh + "\" and at " + cl(u) + " is named \"" + name(u) + "\" (want \"Knight-Captain (Veteran)\")");
                    restoreKnight(b); clearFixture(b, "veteran"); next(b, 60);
                }
                case 60 -> { // p2-level-flash: the commander 8 blocks west of a Dawn Squire, looking at him
                    flashUnit = at(b, dawn, "knight", 0, 0);
                    stand(b, -8, 0, -90f, 8f); next(b, 61);
                }
                case 61 -> {
                    if (since(b) < 12) return; // the camera has settled: particles that arrive as the player lands are dropped
                    heard0 = (int) Phase1Checks.heardOnClient(Veterans.FLASH_SOUND.getId().toString()); flashes0 = Veterans.flashes;
                    Veterans.credit(b, flashUnit, 3); requestShot("levels-flash", 3); next(b, 62);
                }
                case 62 -> {
                    String sound = Veterans.FLASH_SOUND.getId().toString(); int heard = (int) Phase1Checks.heardOnClient(sound);
                    if ((!shotTaken || heard <= heard0) && since(b) < 60) return;
                    var f = Veterans.lastFlash; var u = flashUnit; int rgb = b.side(0).color(); var p = sp(b);
                    double from = p == null ? -1 : Phase2Checks.Staged.distance(p.getPos(), u.mob.getPos());
                    boolean at = f != null && f.unit() == u.mob.getId() && f.at().distanceTo(u.mob.getPos()) < .01 && f.rgb() == rgb && f.sound().equals(sound) && Veterans.flashes == flashes0 + 1;
                    boolean ok = at && heard > heard0 && shotTaken && from > 7 && from < 9 && u.level == 2;
                    Phase2Checks.report("p2-level-flash", ok, "the level up of " + cl(u) + " played one flash (+" + (Veterans.flashes - flashes0) + ") at him in Dawn's colour " + Integer.toHexString(rgb) + " with " + (f == null ? "none" : "dust " + Integer.toHexString(f.rgb()) + " and the sound " + f.sound() + " at " + fmt(f.at().distanceTo(u.mob.getPos())) + " from him") + "=" + at + "; the commander " + fmt(from) + " blocks away heard it " + (heard - heard0) + " time(s); the picture levels-flash was taken=" + shotTaken);
                    wantShot = null; stand(b, -10, 0, -90f, 10f); clearFixture(b, "flash"); next(b, 70);
                }
                case 70 -> { // p2-rise-keeps-level: a level-2 Risen rises through Rise Again
                    var g = at(b, dusk, "revenant", 0, 0); for (int i = 0; i < 5; i++) at(b, dusk, "revenant", -8 + i * 1.5, 10); Houses.recompute(dusk);
                    k = at(b, dawn, "knight", 0, -2);
                    Veterans.credit(b, g, 3); String named = name(g); int sends = RankSync.changeSends; var t = mark();
                    Houses.forceRise = Boolean.TRUE; try { kill(b, k, g); } finally { Houses.forceRise = Boolean.FALSE; }
                    var after = events(t, -2, null);
                    boolean ok = g.mob.isAlive() && g.rose && g.level == 2 && g.credits == 3 && name(g).equals(named) && named.equals("Revenant") && RankSync.changeSends == sends && k.credits == 0 && after.isEmpty() && dusk.traits.getOrDefault("graveborn", 0) >= 1;
                    Phase2Checks.report("p2-rise-keeps-level", ok, "a level-2 Risen of a Graveborn army (tier " + dusk.traits.getOrDefault("graveborn", 0) + ") named \"" + named + "\" was struck down with the roll forced: alive " + g.mob.isAlive() + ", rose " + g.rose + ", " + cl(g) + " (want 3/2), name \"" + name(g) + "\", rank packets +" + (RankSync.changeSends - sends) + " (want 0), the striker " + cl(k) + " (want 0: the rising blow is no kill), progression events " + after.size() + " (want 0)");
                    clearFixture(b, "rise"); next(b, 71);
                }
                case 71 -> { // p2-temporary-no-credit: a temporary Risen kills and earns nothing; a temporary Risen killed counts 1
                    var temp = at(b, dawn, "revenant", -4, -2); temp.temporary = true; temp.expiresAt = b.time + 1000;
                    var h = at(b, dawn, "archer", -4, -8); v = victim(b, dusk, "knight", -4, 0);
                    int troopKills = dawn.stats.troopKills.getOrDefault("revenant", 0);
                    shoot(b, h, v.mob, 1f); blow(b, temp, v.mob, 1f); kill(b, temp, v);
                    boolean earnsNothing = !v.mob.isAlive() && temp.credits == 0 && temp.level == 1 && h.credits == 1 && dawn.stats.troopKills.getOrDefault("revenant", 0) == troopKills;
                    var t2 = at(b, dusk, "revenant", 4, 0); t2.temporary = true; t2.expiresAt = b.time + 1000; t2.level = 3; t2.credits = 8;
                    var k2 = at(b, dawn, "knight", 4, -2); int lost = dusk.stats.lost, kills = dawn.stats.kills;
                    kill(b, k2, t2);
                    boolean countsOne = !t2.mob.isAlive() && k2.credits == 1 && dusk.stats.lost == lost && dawn.stats.kills == kills;
                    Phase2Checks.report("p2-temporary-no-credit", earnsNothing && countsOne, "a temporary Risen struck and then killed a Dusk Squire an archer had hit (dead " + !v.mob.isAlive() + "): " + cl(temp, h) + " (want the Risen 0/1, the archer 1), the army's revenant kills unchanged=" + (dawn.stats.troopKills.getOrDefault("revenant", 0) == troopKills) + "=" + earnsNothing + "; a temporary Dusk Risen set at level 3 killed by a Squire: " + cl(k2) + " (want 1: a temporary victim counts 1), no loss and no army kill counted (lost +" + (dusk.stats.lost - lost) + ", kills +" + (dawn.stats.kills - kills) + ")=" + countsOne);
                    clearFixture(b, "temporary"); next(b, 72);
                }
                case 72 -> { // p2-structure-no-kill: a Dawn Squire fells a Dusk Forward Supply banner
                    k = at(b, dawn, "knight", 4, -2);
                    var banner = GravesAndGlory.ArmySupplyBanner.place(b, dusk, new Vec3d(site.getX() + .5 + 4, site.getY(), site.getZ() + .5 + 1), 0f, 40f);
                    var t = mark(); int blows = 0;
                    if (banner != null) for (; blows < 12 && !banner.isRemoved(); blows++) Combat.resolve(b, k, banner, 100f, "melee", false);
                    var after = events(t, -2, null);
                    boolean ok = banner != null && banner.isRemoved() && k.credits == 0 && after.isEmpty();
                    Phase2Checks.report("p2-structure-no-kill", ok, banner == null ? "no spot for the banner on the platform" : "a Squire felled a Dusk Forward Supply banner in " + blows + " blows (removed " + banner.isRemoved() + "): " + cl(k) + " (want 0), progression events " + after.size() + " (want 0)");
                    if (banner != null && !banner.isRemoved()) banner.discard();
                    clearFixture(b, "structure"); next(b, 80);
                }
                case 80 -> { // p2-commander-bounty: the commander struck down by a Dusk Squire an archer helped; he stands again and falls to another Squire within 10 s
                    var p = sp(b); if (p == null) return;
                    var dk = at(b, dusk, "knight", -8, 2); var da = at(b, dusk, "archer", -4, 6);
                    shoot(b, da, p, 1f);
                    p.setHealth(1f); blow(b, dk, p, 50f);
                    boolean down = b.respawns.containsKey(player) && p.isSpectator();
                    boolean first = down && dk.credits == 3 && da.credits == 3 && dk.level == 2 && da.level == 2;
                    String why = "an archer hit the commander and a Squire struck him down (respawning " + down + "): " + cl(dk, da) + " (want 3/2 each: a commander counts progression.commanderBounty " + Spec.PROGRESSION[0].commanderBounty() + ")=" + first + "; ";
                    revive(b);
                    var dk2 = at(b, dusk, "knight", -8, -2);
                    p.setHealth(1f); blow(b, dk2, p, 50f);
                    boolean second = b.respawns.containsKey(player) && dk2.credits == 3 && da.credits == 3 && dk.credits == 3;
                    why += "he stood again and a second Squire struck him down at once: " + cl(dk2, da, dk) + " (want the second Squire 3, the archer and the first Squire still 3: his log was forgotten at the first death)=" + second;
                    Phase2Checks.report("p2-commander-bounty", first && second, why + "; no bot commander has a body in this build, so the player commander is the commander here");
                    revive(b); clearFixture(b, "commander"); next(b, 81);
                }
                case 81 -> { // p2-commander-never-levels: the commander's own killing blow on a Dusk Squire a Dawn Squire hit
                    var p = sp(b); if (p == null) return;
                    var h = at(b, dawn, "knight", -6, -2); v = victim(b, dusk, "knight", -6, 0);
                    strike(b, h, v, 1f); v.mob.setHealth(1f); v.mob.timeUntilRegen = 0; v.mob.hurtTime = 0; var t = mark(); int kills = own.stats.kills;
                    v.mob.damage(p.getDamageSources().playerAttack(p), 1000f);
                    var killEvents = events(t, -2, "kill");
                    boolean ok = !v.mob.isAlive() && h.credits == 1 && killEvents.size() == 1 && killEvents.get(0).unit() == -1 && own.stats.kills == kills + 1;
                    Phase2Checks.report("p2-commander-never-levels", ok, "the commander's own blow killed a Dusk Squire that a Dawn Squire had struck (dead " + !v.mob.isAlive() + "): the helper " + cl(h) + " (want 1), the kill went to no soldier (" + (killEvents.isEmpty() ? "no kill event" : "unit " + killEvents.get(0).unit() + ": " + killEvents.get(0).detail()) + "), the commander's army kills +" + (own.stats.kills - kills) + " (want 1, unchanged rule)");
                    clearFixture(b, "commander-blow"); next(b, 90);
                }
                case 90 -> { // p2-rebuy-level-1: a level-3 soldier dies and his slot is trained again
                    var rebuy = new GravesAndGlory.Squad(UUID.randomUUID(), 0, GravesAndGlory.Battle.base(0)); rebuy.order = "hold"; rebuy.bot = true; rebuy.banner = new ArrayList<>(List.of("knight"));
                    b.reinforce(rebuy);
                    var first = b.units.stream().filter(u -> u.squad == rebuy).findFirst().orElse(null);
                    if (first == null) { Phase2Checks.report("p2-rebuy-level-1", false, "reinforce fielded no Squire"); next(b, 91); return; }
                    fixture.add(first); Phase1Checks.freeze(first); first.mob.setAiDisabled(true);
                    Veterans.credit(b, first, 8); String high = name(first) + " " + cl(first);
                    fall(b, first);
                    b.reinforce(rebuy);
                    var again = b.units.stream().filter(u -> u.squad == rebuy && u.mob.isAlive()).findFirst().orElse(null);
                    if (again != null) { fixture.add(again); Phase1Checks.freeze(again); again.mob.setAiDisabled(true); }
                    boolean ok = !first.mob.isAlive() && first.level == 3 && again != null && again != first && again.slot == first.slot && again.level == 1 && again.credits == 0 && name(again).equals("Order Squire") && Math.abs(again.mob.getMaxHealth() - again.spec.health()) < 1e-3 && again.weaponTier == 1 && again.armourTier == 1;
                    Phase2Checks.report("p2-rebuy-level-1", ok, "a Squire raised to " + high + " fell; his slot " + first.slot + " was filled again by " + (again == null ? "nobody" : cl(again) + " in slot " + again.slot + " named \"" + name(again) + "\" with " + fmt(again.mob.getMaxHealth()) + " health, tiers " + again.weaponTier + "/" + again.armourTier) + " (want a new soldier at 0/1, \"Order Squire\", the sheet's health, tier I)");
                    clearFixture(b, "rebuy"); next(b, 91);
                }
                case 91 -> { // p2-progression-off: with the switch off nobody gains credit, and the kill still counts for gold and the army
                    Veterans.override = Boolean.FALSE;
                    try {
                        var h = at(b, dawn, "archer", 0, -8); k = at(b, dawn, "knight", 0, -2); v = victim(b, dusk, "knight", 0, 0);
                        int kills = dawn.stats.kills, gold = dawn.stats.goldEarned; var t = mark();
                        shoot(b, h, v.mob, 1f); kill(b, k, v); Veterans.credit(b, k, 9);
                        var after = events(t, -2, null);
                        boolean ok = !Veterans.enabled() && !v.mob.isAlive() && h.credits == 0 && k.credits == 0 && k.level == 1 && after.isEmpty() && dawn.stats.kills == kills + 1 && dawn.stats.goldEarned == gold + Spec.ECONOMY[0].killGold();
                        Phase2Checks.report("p2-progression-off", ok, "with progression off (the sim override) an archer's hit and a Squire's kill, then 9 credits handed over: " + cl(h, k) + " (want 0/1 each), progression events " + after.size() + " (want 0), the army's kills +" + (dawn.stats.kills - kills) + " and gold +" + (dawn.stats.goldEarned - gold) + " (want 1 and " + Spec.ECONOMY[0].killGold() + ": unchanged)");
                    } finally { Veterans.override = null; }
                    clearFixture(b, "off"); next(b, 99);
                }
                default -> {}
            }
        }
    }
}
