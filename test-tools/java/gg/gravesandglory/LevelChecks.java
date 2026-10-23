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
 * Task 4 builds levels, Task 5 gear; armoury (Task 8) still reports fail p2-armoury-not-built.
 */
final class LevelChecks {
    private LevelChecks() {}

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("levels", Levels::new);
        suites.put("gear", Gear::new);
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
    static class Levels extends Phase2Checks.Staged {
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
                case 50 -> { // p2-level-skip and p2-rank-sent: a fresh Squire, credited once the commander tracks him (so the change packets, not START_TRACKING, must carry the rank)
                    flashUnit = at(b, dawn, "knight", 0, -2); next(b, 54);
                }
                case 54 -> { // 9 credits at once from level 1
                    var u = flashUnit;
                    boolean tracked = net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(u.mob).contains(sp(b));
                    if (since(b) < 5 || !tracked && since(b) < 60) return;
                    u.mob.setHealth(20f);
                    float max0 = u.mob.getMaxHealth(); int ups0 = Veterans.levelUps, feeds0 = Veterans.pendingFeed.size(); rankSends0 = RankSync.changeSends; var t = mark();
                    Veterans.credit(b, u, 9);
                    var levels = values(events(t, u.mob.getId(), "level")); var ranks = values(events(t, u.mob.getId(), "rank")); var feeds = events(t, u.mob.getId(), "feed");
                    double wantMax = u.spec.health() * LevelMath.healthFactor(3, 1);
                    var feed = Veterans.pendingFeed.size() > feeds0 ? Veterans.pendingFeed.get(Veterans.pendingFeed.size() - 1) : null;
                    boolean ok = u.level == 3 && u.credits == 9 && Math.abs(u.mob.getMaxHealth() - wantMax) < 1e-3 && Math.abs(u.mob.getHealth() - (20 + (wantMax - max0))) < 1e-3
                        && levels.equals(List.of(2, 3)) && Veterans.levelUps == ups0 + 2 && feeds.size() == 1 && feed != null && Veterans.pendingFeed.size() == feeds0 + 1 && feed.team() == 0 && feed.levelName().equals("Knight-Captain") && name(u).equals("Knight-Captain");
                    Phase2Checks.report("p2-level-skip", ok, "a Squire at 0 credits and 20 of " + fmt(max0) + " health got 9: " + cl(u) + " (want 9/3), max health " + fmt(u.mob.getMaxHealth()) + " (want " + fmt(wantMax) + ", two steps of +5%), health " + fmt(u.mob.getHealth()) + " (want " + fmt(20 + wantMax - max0) + ": raised by the added health only), level ups logged " + levels + " (want [2, 3]), feed lines " + feeds.size() + " (want 1: " + (feed == null ? "none" : feed.levelName() + " for team " + feed.team()) + "), name \"" + name(u) + "\"");
                    rankOk = ranks.equals(List.of(2, 3)) && tracked && RankSync.changeSends >= rankSends0 + 2;
                    rankWhy = "each level up sent the rank: logged " + ranks + " (want [2, 3]), rank_v1 change packets +" + (RankSync.changeSends - rankSends0) + " (want +2) to the commander, who tracks him=" + tracked + " (want true: credited " + since(b) + " ticks after he was posted); ";
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
                    clearFixture(b, "off"); next(b, 92);
                }
                case 92 -> { // p2-credit-fall, p2-credit-splash, p2-level-full-health (Task 4 review M4)
                    { // a Dusk Squire an archer hit falls to his death inside the credit window: the archer is credited, no soldier has the kill
                        var a = at(b, dawn, "archer", -4, -8); var vf = at(b, dusk, "knight", -4, 0);
                        shoot(b, a, vf.mob, 1f); var t = mark(); fall(b, vf);
                        var kills = events(t, -2, "kill");
                        boolean ok = !vf.mob.isAlive() && a.credits == 1 && kills.size() == 1 && kills.get(0).unit() == -1 && kills.get(0).value() == 1;
                        Phase2Checks.report("p2-credit-fall", ok, "an archer's shot hit a Dusk Squire, who then fell with no attacker in the same tick (dead " + !vf.mob.isAlive() + "): " + cl(a) + " (want 1: a helper of the window), kill events " + kills.size() + (kills.isEmpty() ? "" : " (unit " + kills.get(0).unit() + ", bounty " + kills.get(0).value() + ": " + kills.get(0).detail() + ")") + " (want one, unit -1: no soldier's blow)");
                    }
                    { // a slam's splash (a secondary hit) kills a Dusk Squire an archer hit: the slammer has the kill, the archer the assist
                        var h = at(b, dawn, "archer", 4, -8); var ks = at(b, dawn, "knight", 4, -3); var vs = at(b, dusk, "knight", 4, 0);
                        shoot(b, h, vs.mob, 1f); vs.mob.setHealth(1f); vs.mob.timeUntilRegen = 0; vs.mob.hurtTime = 0; var t = mark();
                        int struck = Abilities.splash(b, ks, vs.mob.getPos(), null, 50f, 2.0, 0.0, ks.mob.getPos());
                        var kills = events(t, -2, "kill");
                        boolean ok = struck >= 1 && !vs.mob.isAlive() && ks.credits == 1 && h.credits == 1 && kills.size() == 1 && kills.get(0).unit() == ks.mob.getId();
                        Phase2Checks.report("p2-credit-splash", ok, "a Squire's splash (Abilities.splash, a secondary slam hit) struck " + struck + " and killed a Dusk Squire at 1 health an archer had hit (dead " + !vs.mob.isAlive() + "): " + cl(ks, h) + " (want 1 each), the kill's soldier " + (kills.isEmpty() ? "none" : "#" + kills.get(0).unit()) + " (want the Squire #" + ks.mob.getId() + ")");
                    }
                    { // a level up at full health: current health rises with the maximum and ends at it
                        var u = at(b, dawn, "knight", 0, 4); float max0 = u.mob.getMaxHealth(); boolean full = u.mob.getHealth() == max0;
                        Veterans.credit(b, u, 3);
                        double want = u.spec.health() * LevelMath.healthFactor(2, 1);
                        boolean ok = full && u.level == 2 && Math.abs(u.mob.getMaxHealth() - want) < 1e-3 && Math.abs(u.mob.getHealth() - u.mob.getMaxHealth()) < 1e-3;
                        Phase2Checks.report("p2-level-full-health", ok, "a Squire at full health (" + fmt(max0) + "=" + full + ") reached " + cl(u) + ": max health " + fmt(u.mob.getMaxHealth()) + " (want " + fmt(want) + "), health " + fmt(u.mob.getHealth()) + " (want the new maximum: raised by the step, never above it)");
                    }
                    clearFixture(b, "fall-splash"); next(b, 93);
                }
                case 93 -> { // p2-commander-fallen-shot: a Dusk Squire struck the commander; a Dusk archer's arrow strikes him down after the archer fell
                    var p = sp(b); if (p == null) return;
                    helper = at(b, dusk, "knight", -8, 2); arch = at(b, dusk, "archer", -4, 6);
                    blow(b, helper, p, 1f);
                    p.setHealth(1f); p.timeUntilRegen = 0; p.hurtTime = 0;
                    b.shots.add(new Projectiles.Shot(arch, dusk, p, "arrow", 50f, arch.mob.getEyePos(), b.time, b.time + 4, null, null));
                    fall(b, arch); next(b, 94);
                }
                case 94 -> {
                    boolean down = b.respawns.containsKey(player);
                    if (!down && since(b) < 20) return;
                    boolean ok = down && !arch.mob.isAlive() && helper.credits == 3 && helper.level == 2 && arch.credits == 0;
                    Phase2Checks.report("p2-commander-fallen-shot", ok, "a Dusk Squire struck the commander, then a Dusk archer's arrow struck him down " + since(b) + " ticks after the archer fell (respawning " + down + "): " + cl(helper, arch) + " (want the Squire 3/2: the shot is his army's blow, Battle.fallenSquad; the fallen archer 0)");
                    revive(b); clearFixture(b, "fallen-shot"); next(b, 95);
                }
                case 95 -> { // p2-commander-fall: a Dusk Squire's blow, then a death with no attacker inside the window; then another outside it
                    var p = sp(b); if (p == null) return;
                    helper = at(b, dusk, "knight", -8, 2);
                    blow(b, helper, p, 1f); p.setHealth(1f); p.timeUntilRegen = 0; p.hurtTime = 0; p.damage(b.world.getDamageSources().generic(), 1000f);
                    boolean down = b.respawns.containsKey(player);
                    killerOk = down && helper.credits == 3;
                    killerWhy = "a Dusk Squire struck the commander and he died to a blow with no attacker in the same tick (respawning " + down + "): " + cl(helper) + " (want 3: a pull into lava or a knock off an edge is the striker's army's, as a troop's is)=" + killerOk + "; ";
                    revive(b);
                    target = at(b, dusk, "knight", -8, -2); blow(b, target, p, 1f); next(b, 96);
                }
                case 96 -> {
                    if (since(b) < Projectiles.CREDIT_WINDOW_TICKS + 6) return;
                    var p = sp(b); if (p == null) return;
                    p.setHealth(1f); p.timeUntilRegen = 0; p.hurtTime = 0; p.damage(b.world.getDamageSources().generic(), 1000f);
                    boolean down = b.respawns.containsKey(player), late = down && target.credits == 0;
                    Phase2Checks.report("p2-commander-fall", killerOk && late, killerWhy + "a second Dusk Squire struck him and he died with no attacker " + since(b) + " ticks later, past Projectiles.CREDIT_WINDOW_TICKS " + Projectiles.CREDIT_WINDOW_TICKS + " (respawning " + down + "): " + cl(target) + " (want 0: nobody's kill)=" + late);
                    revive(b); clearFixture(b, "commander-fall"); next(b, 97);
                }
                case 97 -> { // p2-commander-unknown: an attacker the battle does not know (a vanilla zombie) strikes the commander down after a Dusk Squire's blow
                    var p = sp(b); if (p == null) return;
                    helper = at(b, dusk, "knight", -8, 2); blow(b, helper, p, 1f);
                    var zombie = new net.minecraft.entity.mob.ZombieEntity(b.world); zombie.setPosition(p.getPos().add(2, 0, 0));
                    p.setHealth(1f); p.timeUntilRegen = 0; p.hurtTime = 0; p.damage(b.world.getDamageSources().mobAttack(zombie), 1000f);
                    boolean down = b.respawns.containsKey(player);
                    boolean ok = down && helper.credits == 0 && helper.level == 1;
                    Phase2Checks.report("p2-commander-unknown", ok, "a Dusk Squire struck the commander and a vanilla zombie (no troop, no commander) struck him down (respawning " + down + "): " + cl(helper) + " (want 0: an unknown attacker is nobody's, never the enemy's by default)");
                    zombie.discard(); revive(b); clearFixture(b, "commander-unknown"); next(b, 99);
                }
                default -> {}
            }
        }
    }

    /**
     * Task 5: gear, the gear command, the Armoury state, bot buys, the report's levels block and the standing notices (in-match progression design 4, 5,
     * 7 and 9.1). The fixture is Phase2Checks.Staged's platform, 46 blocks from Dawn's barracks and farther from every point, so the commander stands
     * "away" there. His army's banner is set to four Order Squires, two Order Archers and a Sunweaver; his buys go through Battle.command as a client's
     * gear: command does, and he is moved to his barracks or to a point Dawn is made to hold for the tick of a buy, then back. It reuses the levels
     * suite's helpers (Levels) and runs its own stages.
     */
    static final class Gear extends Levels {
        static final String ACTION = "Gear check: an action's notice expires";
        /** The stage that ends the suite: past every numbered stage (the levels suite's 99 would cut this one short at its stage 100). */
        static final int DONE = 999;
        Spec.TroopsRow knight, archer, mage;
        List<String> bannerBefore; String commanderBefore;
        int knightStars; boolean starsSet;
        GravesAndGlory.Capture held, unheld; int heldOwner, unheldOwner, heldProgress; long heldSince; boolean heldContested;
        GravesAndGlory.Unit u1, u2, u3;
        int goldTrained, paid; java.util.Set<Integer> fieldedBefore = new java.util.HashSet<>();
        // p2-armoury-state's client half: the server's Armoury and glint bits the client must hold
        volatile LevelMath.Armoury armouryWant; volatile Map<String, Boolean> glintWant; volatile String armourySeen, armouryLast = "";
        // p2-report-levels' client half
        volatile String reportId; volatile boolean reportShot; volatile int reportRowsClient = -1; int screenTicks;
        boolean reportOk; String reportWhy = "";
        // p2-notice-standing
        String reward, welcome, waiting, standWhy = ""; boolean standOk;

        /** The commander's command as his client sends it (order_v4); returns the notice it left ("" for none). */
        String say(GravesAndGlory.Battle b, String cmd) { b.campaign.notices.remove(player); var p = sp(b); if (p != null) b.command(p, cmd); return b.campaign.notices.getOrDefault(player, ""); }
        /** The commander at a spot on the ground (x, z) for this tick. */
        void to(GravesAndGlory.Battle b, double x, double z) { var p = sp(b); if (p != null) b.relocate(p, x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z, 0f, 0f); }
        void toBarracks(GravesAndGlory.Battle b) { var f = Spec.FACTIONS[0]; to(b, f.barracksX() + 2.5, f.barracksZ() + .5); }
        void toPoint(GravesAndGlory.Battle b, GravesAndGlory.Capture c) { to(b, c.spec.x() + 2.5, c.spec.z() + .5); }
        void home(GravesAndGlory.Battle b) { stand(b, -10, 0, -90f, 10f); }
        /** Dawn holds the point for the forward-deploy rule (held longer than economy.forwardHoldSeconds, uncontested); restoreHeld puts it back. */
        void hold(GravesAndGlory.Battle b, GravesAndGlory.Capture c) { c.owner = 0; c.contested = false; c.progress = 100; c.ownedSince = b.time - Spec.ECONOMY[0].forwardHoldSeconds() * 20L - 20; }
        void restoreHeld() { if (held != null) { held.owner = heldOwner; held.ownedSince = heldSince; held.contested = heldContested; held.progress = heldProgress; } if (unheld != null) unheld.owner = unheldOwner; }
        static String states(LevelMath.ArmouryRow r) { return r.troop() + " " + r.weapon2() + "/" + r.weapon3() + "/" + r.armour2() + "/" + r.armour3(); }
        String kits(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { var m = b.armouries.get(s); return m == null ? "{}" : m.toString(); }
        static String opts(List<LevelMath.Option> os) { StringBuilder sb = new StringBuilder(); for (var o : os) { if (sb.length() > 0) sb.append(", "); sb.append(o.troop()).append(':').append(o.track()).append(':').append(o.tier()).append('=').append(o.price()); } return "[" + sb + "]"; }
        static LevelMath.Option opt(String troop, String track, int tier, int price) { return new LevelMath.Option(troop, track, tier, price); }
        GravesAndGlory.Squad bot(GravesAndGlory.Battle b, String tier, int gold, String... banner) {
            var s = new GravesAndGlory.Squad(UUID.randomUUID(), 1, GravesAndGlory.Battle.base(1)); s.bot = true; s.order = "hold"; s.tier = tier; s.gold = gold; s.banner = new ArrayList<>(List.of(banner)); return s;
        }

        @Override public void cleanup(MinecraftClient c) {
            Phase2Checks.server(c, b -> {
                Veterans.override = null; restoreHeld();
                var s = player == null ? null : b.squads.get(player);
                if (s != null && bannerBefore != null) s.banner = bannerBefore;
                if (s != null && commanderBefore != null) s.commander = commanderBefore;
                restoreStars(b);
                clearFixture(b, "cleanup");
                if (site != null) platform(b, site, 12, net.minecraft.block.Blocks.AIR.getDefaultState());
                unforce(b); b.respawns.remove(player);
                if (b.running) b.campaign.returnLobby();
            });
        }
        void restoreStars(GravesAndGlory.Battle b) {
            if (!starsSet) return;
            var pr = profile(b); if (knightHad) pr.owned.get("knight").stars = knightStars; else pr.owned.remove("knight");
            starsSet = false;
        }

        @Override public void step(MinecraftClient c, int tick) {
            if (tick == 20) { c.setScreen(null); UUID id = c.player.getUuid(); Phase2Checks.server(c, b -> { player = id; next(b, 1); }); }
            if (wantShot != null && !shotTaken && ++shotDelay >= shotAfter) { Phase2Checks.shot(c, wantShot); shotTaken = true; }
            var want = armouryWant;
            if (want != null && armourySeen == null) {
                armouryLast = CampaignClient.armoury.toString();
                boolean glint = true; var gw = glintWant;
                if (gw != null) for (var e : gw.entrySet()) glint &= CommandMode.groups.stream().anyMatch(g -> g.id().equals(e.getKey()) && g.armouryOpen() == e.getValue());
                if (want.equals(CampaignClient.armoury) && glint) {
                    StringBuilder g = new StringBuilder(); for (var gr : CommandMode.groups) g.append(gr.id()).append('=').append(gr.armouryOpen()).append(' ');
                    armourySeen = CampaignClient.armoury + " with the glint bits " + g.toString().trim();
                }
            }
            var id = reportId;
            if (id != null && !reportShot && c.currentScreen instanceof CampScreen cs && "results".equals(cs.page) && CampaignClient.result.has("levels") && CampaignClient.result.has("id") && CampaignClient.result.get("id").getAsString().equals(id)) {
                if (++screenTicks == 5) { reportRowsClient = CampaignClient.result.getAsJsonArray("levels").size(); Phase2Checks.shot(c, "gear-report"); reportShot = true; }
            }
            if (stage == DONE && !finished) { finished = true; c.setScreen(null); Phase2Checks.finish(c, "gear"); }
            if (tick > 16000 && !finished) { finished = true; Phase2Checks.report("p2-gear-timeout", false, "stuck in stage " + stage); Phase2Checks.finish(c, "gear"); }
        }

        @Override public void serverTick(GravesAndGlory.Battle b) {
            if (player == null || stage >= DONE) return;
            try { gear(b); } catch (RuntimeException e) { Phase2Checks.report("p2-gear-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, DONE); }
        }

        void gear(GravesAndGlory.Battle b) {
            if (stage < 110) hold();
            switch (stage) {
                case 1 -> { var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return; next(b, 2); }
                case 2 -> {
                    if (since(b) < 10) return;
                    stageFixture(b); b.campaign.introUntil = b.time; Veterans.override = null; Houses.forceRise = Boolean.FALSE;
                    knight = Campaign.troop("knight"); archer = Campaign.troop("archer"); mage = Campaign.troop("mage");
                    bannerBefore = own.banner; own.banner = new ArrayList<>(List.of("knight", "knight", "knight", "knight", "archer", "archer", "mage", "^"));
                    commanderBefore = own.commander;
                    home(b);
                    // two points far from the barracks and the platform: one Dawn will hold for a buy, one nobody holds
                    var f = Spec.FACTIONS[0]; double reach = Spec.ECONOMY[0].accessRadius();
                    Vec3d barracks = new Vec3d(f.barracksX(), 0, f.barracksZ()), here = sp(b).getPos();
                    for (var c : b.points) {
                        Vec3d at = new Vec3d(c.spec.x(), 0, c.spec.z());
                        if (distance(at, barracks) < 2 * reach + 2 || distance(at, here) < 2 * reach + 2) continue;
                        if (held == null) held = c; else if (unheld == null) unheld = c;
                    }
                    if (held != null) { heldOwner = held.owner; heldSince = held.ownedSince; heldContested = held.contested; heldProgress = held.progress; }
                    if (unheld != null) unheldOwner = unheld.owner;
                    double fromBarracks = distance(here, barracks);
                    boolean ok = held != null && unheld != null && fromBarracks > reach && !Veterans.buyHere(b, sp(b), own) && Veterans.types(own).equals(List.of(knight, archer, mage));
                    Phase2Checks.report("p2-gear-fixture", ok, "the commander on the platform " + fmt(fromBarracks) + " blocks from his barracks (want more than economy.accessRadius " + fmt(reach) + "), may buy there " + Veterans.buyHere(b, sp(b), own) + " (want false); a point to hold " + (held == null ? "none" : held.spec.id()) + " and one left unheld " + (unheld == null ? "none" : unheld.spec.id()) + "; the banner's types " + Veterans.types(own).stream().map(Spec.TroopsRow::id).toList());
                    next(b, ok ? 10 : DONE);
                }
                case 10 -> { // p2-gear-locked, p2-gear-open-l2, p2-gear-location, p2-gear-wear-now, p2-gear-wear-later
                    var p = sp(b); if (p == null) return;
                    own.gold = 1000;
                    u1 = at(b, own, "knight", -6, -2); u2 = at(b, own, "knight", -4, -2); u3 = at(b, own, "knight", -2, -2);
                    double base = u1.mob.getMaxHealth();
                    String locked = say(b, "gear:knight:weapon:2");
                    boolean lockedOk = locked.equals("Needs a level-2 Knight") && own.gold == 1000 && Veterans.peek(b, own, "knight") == null && Veterans.row(b, own, knight).weapon2() == LevelMath.Buy.LOCKED;
                    Phase2Checks.report("p2-gear-locked", lockedOk, "before any Squire reached level 2, gear:knight:weapon:2 left the notice \"" + locked + "\" (want \"Needs a level-2 Knight\"), gold " + own.gold + " (want 1000), the row " + states(Veterans.row(b, own, knight)));
                    Veterans.credit(b, u1, 3); Veterans.credit(b, u2, 3);
                    var r = Veterans.row(b, own, knight);
                    String three = say(b, "gear:knight:weapon:3");
                    boolean open = u1.level == 2 && r.weapon2() == LevelMath.Buy.OPEN && r.weapon3() == LevelMath.Buy.LOCKED && r.armour2() == LevelMath.Buy.OPEN && r.armour3() == LevelMath.Buy.LOCKED
                        && three.equals("Needs a level-3 Knight-Captain") && own.gold == 1000 && Veterans.row(b, own, archer).weapon2() == LevelMath.Buy.LOCKED;
                    Phase2Checks.report("p2-gear-open-l2", open, "two Squires reached " + cl(u1, u2) + ": the row " + states(r) + " (want OPEN/LOCKED/OPEN/LOCKED), gear:knight:weapon:3 \"" + three + "\" (want \"Needs a level-3 Knight-Captain\"), the archers' row " + states(Veterans.row(b, own, archer)) + " (want LOCKED: no archer levelled), gold " + own.gold);
                    boolean tierOne = u1.weaponTier == 1 && u2.weaponTier == 1 && u1.armourTier == 1;
                    String away = say(b, "gear:knight:weapon:2"); int goldAway = own.gold;
                    unheld.owner = -1; toPoint(b, unheld); boolean hereUnheld = Veterans.buyHere(b, p, own); String atUnheld = say(b, "gear:knight:weapon:2"); home(b);
                    toBarracks(b); boolean hereBarracks = Veterans.buyHere(b, p, own); var t = mark(); String atBarracks = say(b, "gear:knight:weapon:2"); home(b);
                    int goldBarracks = own.gold; var worn = events(t, -2, "wear");
                    boolean weaponWorn = u1.weaponTier == 2 && u2.weaponTier == 2 && u3.weaponTier == 1 && worn.size() == 2;
                    u1.mob.setHealth(10f); u2.mob.setHealth(15f); double u3max = u3.mob.getMaxHealth();
                    hold(b, held); boolean anchored = b.reinforcementAnchors(0).stream().anyMatch(a -> a.x() == held.spec.x() && a.z() == held.spec.z());
                    toPoint(b, held); boolean hereHeld = Veterans.buyHere(b, p, own); String atHeld = say(b, "gear:knight:armour:2"); home(b); restoreHeld();
                    boolean location = away.equals("Buy at your barracks or a held point") && goldAway == 1000 && !hereUnheld && atUnheld.equals("Buy at your barracks or a held point")
                        && hereBarracks && atBarracks.equals("Order Squire: Weapon II bought for 40 gold") && goldBarracks == 960 && anchored && hereHeld && atHeld.equals("Order Squire: Armour II bought for 40 gold") && own.gold == 920;
                    Phase2Checks.report("p2-gear-location", location, "on the platform \"" + away + "\" (gold " + goldAway + "); at the unheld point " + unheld.spec.id() + " (may buy " + hereUnheld + ") \"" + atUnheld + "\"; at the barracks (may buy " + hereBarracks + ") \"" + atBarracks + "\", gold " + goldBarracks + " (want 960); at point " + held.spec.id() + " held by Dawn (a reinforcement anchor " + anchored + ", may buy " + hereHeld + ") \"" + atHeld + "\", gold " + own.gold + " (want 920)");
                    double want2 = base * LevelMath.healthFactor(2, 2) / LevelMath.healthFactor(1, 1);
                    boolean wearNow = weaponWorn && u1.armourTier == 2 && u2.armourTier == 2 && u3.armourTier == 1 && Math.abs(u1.mob.getMaxHealth() - want2) < 1e-3 && Math.abs(u2.mob.getMaxHealth() - want2) < 1e-3
                        && Math.abs(u1.mob.getHealth() - (10 + base * .10)) < 1e-3 && Math.abs(u2.mob.getHealth() - (15 + base * .10)) < 1e-3 && Math.abs(u3.mob.getMaxHealth() - u3max) < 1e-3;
                    Phase2Checks.report("p2-gear-wear-now", wearNow, "Weapon II bought: tiers " + u1.weaponTier + "/" + u2.weaponTier + "/" + u3.weaponTier + " (want 2/2/1: the level-1 Squire cannot wear it), wear events " + worn.size() + " (want 2, each a rank_v1 change); Armour II bought: the level-2 Squires at max " + fmt(u1.mob.getMaxHealth()) + " and " + fmt(u2.mob.getMaxHealth()) + " (want " + fmt(want2) + ": +10%), health 10 -> " + fmt(u1.mob.getHealth()) + " and 15 -> " + fmt(u2.mob.getHealth()) + " (want +" + fmt(base * .10) + ": the delta only), the level-1 Squire tier " + u3.armourTier + " at " + fmt(u3.mob.getMaxHealth()) + " (want 1 at " + fmt(u3max) + ")");
                    u3.mob.setHealth(10f); Veterans.credit(b, u3, 3);
                    boolean later = tierOne && u3.level == 2 && u3.weaponTier == 2 && u3.armourTier == 2 && Math.abs(u3.mob.getMaxHealth() - want2) < 1e-3 && Math.abs(u3.mob.getHealth() - (10 + base * .15)) < 1e-3;
                    Phase2Checks.report("p2-gear-wear-later", later, "the Squires who levelled before the buy wore tier I until it=" + tierOne + "; a Squire reaching level 2 after both buys: " + cl(u3) + " wears weapon " + u3.weaponTier + " armour " + u3.armourTier + " (want 2/2) at max " + fmt(u3.mob.getMaxHealth()) + " (want " + fmt(want2) + ") and health 10 -> " + fmt(u3.mob.getHealth()) + " (want " + fmt(10 + base * .15) + ": the level step and the armour step)");
                    clearFixture(b, "location"); next(b, 20);
                }
                case 20 -> { // p2-gear-iii-needs-ii: a level-3 archer; Armour III is refused until Armour II is bought
                    var a1 = at(b, own, "archer", 0, -6); float max0 = a1.mob.getMaxHealth(); Veterans.credit(b, a1, 8);
                    toBarracks(b); int g0 = own.gold;
                    String first = say(b, "gear:archer:armour:3"); int g1 = own.gold;
                    String two = say(b, "gear:archer:armour:2"), three = say(b, "gear:archer:armour:3");
                    home(b);
                    boolean ok = a1.level == 3 && first.equals("Buy Armour II first") && g1 == g0 && two.equals("Order Archer: Armour II bought for 40 gold") && three.equals("Order Archer: Armour III bought for 80 gold")
                        && own.gold == g0 - 120 && a1.armourTier == 3 && Math.abs(a1.mob.getMaxHealth() - max0 * LevelMath.healthFactor(3, 3)) < 1e-3;
                    Phase2Checks.report("p2-gear-iii-needs-ii", ok, "an archer at " + cl(a1) + ": gear:archer:armour:3 first \"" + first + "\" (want \"Buy Armour II first\", gold unchanged " + (g1 == g0) + "), then Armour II \"" + two + "\" and Armour III \"" + three + "\", gold " + g0 + " -> " + own.gold + " (want -120), his armour tier " + a1.armourTier + " and max " + fmt(a1.mob.getMaxHealth()) + " (want 3 at " + fmt(max0 * LevelMath.healthFactor(3, 3)) + ")");
                    clearFixture(b, "iii-needs-ii"); next(b, 30);
                }
                case 30 -> { // p2-gear-open-stays: the only levelled Sunweaver dies; its buys stay open for the round
                    var m = at(b, own, "mage", 2, -8); Veterans.credit(b, m, 3);
                    var before = Veterans.row(b, own, mage);
                    fall(b, m);
                    var after = Veterans.row(b, own, mage); boolean none = b.fielded(own).noneMatch(u -> u.spec == mage);
                    boolean ok = !m.mob.isAlive() && none && before.weapon2() == LevelMath.Buy.OPEN && after.weapon2() == LevelMath.Buy.OPEN && after.armour2() == LevelMath.Buy.OPEN && after.weapon3() == LevelMath.Buy.LOCKED;
                    Phase2Checks.report("p2-gear-open-stays", ok, "a Sunweaver reached level 2 (row " + states(before) + ") and fell (dead " + !m.mob.isAlive() + ", no Sunweaver fielded " + none + "): the row " + states(after) + " (want OPEN/LOCKED/OPEN/LOCKED: what a level opened stays open)");
                    clearFixture(b, "open-stays"); next(b, 40);
                }
                case 40 -> { // p2-gear-price: Common 40/80, Epic 80/160, for a Warlock too (his discount is on troops, never gear)
                    own.commander = "warlock";
                    var k3 = at(b, own, "knight", -4, -4); Veterans.credit(b, k3, 8); var m3 = at(b, own, "mage", 4, -8); Veterans.credit(b, m3, 8);
                    int troop = Commander.cost(own, mage);
                    toBarracks(b); int g0 = own.gold;
                    String kw3 = say(b, "gear:knight:weapon:3"); int g1 = own.gold;
                    String mw2 = say(b, "gear:mage:weapon:2"); int g2 = own.gold;
                    String mw3 = say(b, "gear:mage:weapon:3"); int g3 = own.gold;
                    home(b); own.commander = commanderBefore;
                    boolean ok = troop < mage.cost() && g0 - g1 == 80 && g1 - g2 == 80 && g2 - g3 == 160 && kw3.equals("Order Squire: Weapon III bought for 80 gold") && mw2.equals("Sunweaver: Weapon II bought for 80 gold") && mw3.equals("Sunweaver: Weapon III bought for 160 gold")
                        && k3.weaponTier == 3 && m3.weaponTier == 3 && LevelMath.price("common", 2) == 40 && LevelMath.price("epic", 2) == 80;
                    Phase2Checks.report("p2-gear-price", ok, "a Warlock (a Sunweaver costs him " + troop + " gold, the sheet " + mage.cost() + ": his discount is on) bought Squire Weapon III for " + (g0 - g1) + " (want 80; Weapon II was 40), Sunweaver Weapon II for " + (g1 - g2) + " (want 80) and Weapon III for " + (g2 - g3) + " (want 160): \"" + kw3 + "\", \"" + mw2 + "\", \"" + mw3 + "\"; worn " + k3.weaponTier + "/" + m3.weaponTier + " (want 3/3)");
                    clearFixture(b, "price"); next(b, 50);
                }
                case 50 -> { // p2-gear-no-refund: nothing sells gear back; a disband refunds the troop's refund only
                    var k = at(b, own, "knight", 0, -4); String kit0 = Veterans.peek(b, own, "knight").toString();
                    toBarracks(b); int g0 = own.gold;
                    String again = say(b, "gear:knight:weapon:2"), one = say(b, "gear:knight:weapon:1");
                    List<String> said = new ArrayList<>();
                    for (String cmd : List.of("gear:knight:weapon:-2", "gear:knight:weapon", "gear:knight:sell:2", "gear:knight:weapon:sell", "gear-sell:knight:weapon:2", "refund:knight:weapon:2", "sell:knight:weapon:2")) said.add(cmd + " \"" + say(b, cmd) + "\"");
                    int g1 = own.gold; boolean alive = k.mob.isAlive();
                    say(b, "disband:knight"); int g2 = own.gold;
                    home(b);
                    String kit1 = Veterans.peek(b, own, "knight").toString();
                    boolean ok = again.equals("Already bought") && one.equals("Already bought") && g1 == g0 && alive && !k.mob.isAlive() && g2 == g0 + Spec.ECONOMY[0].disbandRefund() && kit1.equals(kit0);
                    Phase2Checks.report("p2-gear-no-refund", ok, "Weapon II again \"" + again + "\", Weapon I \"" + one + "\" (want \"Already bought\" both); " + String.join(", ", said) + ": gold " + g0 + " -> " + g1 + " (want unchanged); a Squire disbanded (gone " + !k.mob.isAlive() + "): gold " + g2 + " (want +" + Spec.ECONOMY[0].disbandRefund() + ", the troop's refund only); the Squires' Armoury " + kit0 + " -> " + kit1 + " (want unchanged)");
                    clearFixture(b, "no-refund"); next(b, 60);
                }
                case 60 -> { // p2-gear-rebuy-tier1: a Squire trained again at the barracks pays the troop's price only and wears tier I until he levels
                    fieldedBefore.clear(); b.units.forEach(u -> fieldedBefore.add(u.mob.getId()));
                    toBarracks(b); int g0 = own.gold; say(b, "train:knight"); goldTrained = own.gold; paid = g0 - goldTrained; home(b);
                    next(b, 61);
                }
                case 61 -> {
                    var fresh = b.units.stream().filter(u -> u.squad == own && u.spec == knight && u.mob.isAlive() && !fieldedBefore.contains(u.mob.getId())).findFirst().orElse(null);
                    if (fresh == null && since(b) < 900) return;
                    if (fresh == null) { Phase2Checks.report("p2-gear-rebuy-tier1", false, "no Squire was trained within 900 ticks (queue " + own.training.size() + ", paid " + paid + ")"); next(b, 70); return; }
                    fixture.add(fresh); Phase1Checks.freeze(fresh); fresh.mob.setAiDisabled(true);
                    double want = knight.health() * b.campaign.health(own, knight);
                    boolean tierOne = fresh.level == 1 && fresh.weaponTier == 1 && fresh.armourTier == 1 && Math.abs(fresh.mob.getMaxHealth() - want) < 1e-3;
                    var kit = Veterans.peek(b, own, "knight"); int goldNow = own.gold;
                    String arrived = "level " + fresh.level + " wearing " + fresh.weaponTier + "/" + fresh.armourTier + " at max " + fmt(fresh.mob.getMaxHealth());
                    Veterans.credit(b, fresh, 3);
                    boolean levels = fresh.level == 2 && fresh.weaponTier == 2 && fresh.armourTier == 2;
                    boolean ok = paid == Commander.cost(own, knight) && goldNow >= goldTrained && tierOne && levels && kit.weapon == 3 && kit.armour == 2;
                    Phase2Checks.report("p2-gear-rebuy-tier1", ok, "train:knight at the barracks took " + paid + " gold (want the troop's " + Commander.cost(own, knight) + ", no gear charge), and gold stayed at or above " + goldTrained + " to the spawn (" + goldNow + "); the new Squire arrived at " + arrived + " (want level 1, tier I/I, " + fmt(want) + ") though the army bought " + kit + "; at level 2 he wears " + fresh.weaponTier + "/" + fresh.armourTier + " (want 2/2: min of his level and the tier bought)");
                    clearFixture(b, "rebuy"); next(b, 70);
                }
                case 70 -> { // p2-gear-allies-separate: a second Dawn army keeps its own Armoury
                    dawn.banner = new ArrayList<>(List.of("knight", "archer"));
                    var dk = at(b, dawn, "knight", 0, -4); Veterans.credit(b, dk, 3);
                    var ok2 = at(b, own, "knight", 2, -4); Veterans.credit(b, ok2, 3);
                    var dr = Veterans.row(b, dawn, knight); var or = Veterans.row(b, own, knight); String ownKit = Veterans.peek(b, own, "knight").toString();
                    boolean untouched = dk.weaponTier == 1 && dk.armourTier == 1 && dr.weapon2() == LevelMath.Buy.OPEN && dr.armour2() == LevelMath.Buy.OPEN && or.weapon2() == LevelMath.Buy.BOUGHT && Veterans.peek(b, dawn, "knight") != Veterans.peek(b, own, "knight");
                    String wore = dk.weaponTier + "/" + dk.armourTier;
                    String why = Veterans.refusal(b, dawn, knight, LevelMath.WEAPON, 2, true);
                    if (why.isEmpty()) Veterans.purchase(b, dawn, knight, LevelMath.WEAPON, 2, LevelMath.price("common", 2));
                    boolean apart = why.isEmpty() && dk.weaponTier == 2 && Veterans.peek(b, own, "knight").toString().equals(ownKit) && ok2.weaponTier == 2 && ok2.armourTier == 2 && Veterans.row(b, dawn, knight).weapon2() == LevelMath.Buy.BOUGHT;
                    Phase2Checks.report("p2-gear-allies-separate", untouched && apart, "a Squire of a second Dawn army reached level 2 after the commander's army bought Squire gear: he wears " + wore + " before his army buys (want 1/1), his army's row " + states(dr) + " (want OPEN on tier II) against the commander's " + states(or) + "=" + untouched + "; the second army buys Weapon II (refusal \"" + why + "\"): he wears weapon " + dk.weaponTier + " (want 2), the commander's Squires' Armoury " + Veterans.peek(b, own, "knight") + " (want unchanged " + ownKit + ")=" + apart);
                    clearFixture(b, "allies"); next(b, 80);
                }
                case 80 -> { // p2-gear-stats: a star-3 Squire at level 3 with Weapon III and Armour III: health and blows x1.30 each, stars on top
                    var pr = profile(b); var owned = pr.owned.get("knight"); knightHad = owned != null;
                    if (!knightHad) { owned = new Profile.Owned(); pr.owned.put("knight", owned); }
                    knightStars = owned.stars; owned.stars = 3; starsSet = true;
                    double star = b.campaign.health(own, knight);
                    var ks = at(b, own, "knight", 0, -2); double max0 = ks.mob.getMaxHealth();
                    toBarracks(b); String a3 = say(b, "gear:knight:armour:3"); home(b);
                    Veterans.credit(b, ks, 8);
                    var v = victim(b, dusk, "knight", 0, 0);
                    float base = (float) Abilities.baseHit(b, ks);
                    float dealt = strike(b, ks, v, base); var log = Combat.LAST.peekLast();
                    double wantMax = knight.health() * star * 1.30;
                    boolean ok = star > 1 && Math.abs(max0 - knight.health() * star) < 1e-3 && a3.equals("Order Squire: Armour III bought for 80 gold")
                        && ks.level == 3 && ks.weaponTier == 3 && ks.armourTier == 3 && Math.abs(LevelMath.healthFactor(3, 3) - 1.30) < 1e-9 && Math.abs(ks.mob.getMaxHealth() - wantMax) < 1e-3 && Math.abs(ks.mob.getHealth() - wantMax) < 1e-3
                        && log != null && log.attacker() == ks.mob.getId() && Math.abs(log.veteran() - 1.30) < 1e-9 && Math.abs(log.base() - knight.damage() * star) < 1e-4 && Math.abs(dealt - log.base() * log.veteran() * log.total()) < 1e-3;
                    Phase2Checks.report("p2-gear-stats", ok, "a star-3 Squire (star factor " + fmt(star) + ", spawned at " + fmt(max0) + ") bought Armour III (\"" + a3 + "\") and reached " + cl(ks) + " wearing " + ks.weaponTier + "/" + ks.armourTier + ": max health " + fmt(ks.mob.getMaxHealth()) + " (want " + knight.health() + " x " + fmt(star) + " x 1.30 = " + fmt(wantMax) + "), health " + fmt(ks.mob.getHealth()) + "; his blow " + (log == null ? "none" : show(log)) + ": base " + (log == null ? "-" : fmt(log.base())) + " (want " + knight.damage() + " x " + fmt(star) + ": stars in the base), veteran " + (log == null ? "-" : fmt(log.veteran())) + " (want 1.30), dealt " + fmt(dealt) + " (want base x veteran x total)");
                    restoreStars(b); clearFixture(b, "stats"); next(b, 90);
                }
                case 90 -> { // p2-armoury-state: the client's CampaignClient.armoury and glint bits match the server's, with every buy state on show
                    own.gold = 50; home(b);
                    var a = Veterans.armouryState(b, own, sp(b));
                    java.util.Set<LevelMath.Buy> all = new java.util.HashSet<>(); for (var r : a.rows()) { all.add(r.weapon2()); all.add(r.weapon3()); all.add(r.armour2()); all.add(r.armour3()); }
                    if (all.size() != 4 || a.here()) { Phase2Checks.report("p2-armoury-state", false, "BAD fixture: the server's Armoury " + a + " (want all four buy states, not here)"); next(b, 100); return; }
                    Map<String, Boolean> glint = new HashMap<>(); for (String g : Morale.GROUPS) glint.put(g, Veterans.armouryOpen(b, own, g));
                    armourySeen = null; glintWant = glint; armouryWant = a; next(b, 91);
                }
                case 91 -> {
                    var a = Veterans.armouryState(b, own, sp(b)); if (!a.equals(armouryWant)) armouryWant = a; // a gold tick may move a state: the client must catch up with the newest
                    if (armourySeen == null && since(b) < 100) return;
                    String seen = armourySeen; armouryWant = null;
                    boolean glint = glintWant.get("infantry") == Boolean.FALSE && glintWant.get("ranged") == Boolean.TRUE && glintWant.get("support") == Boolean.TRUE;
                    Phase2Checks.report("p2-armoury-state", seen != null && glint, "the server's Armoury " + a + " and glint bits " + glintWant + " (want infantry false: all bought; ranged true: Weapon II open; support true: Armour II unaffordable); the client " + (seen == null ? "never matched it in 100 ticks, last " + armouryLast : "holds " + seen));
                    next(b, 100);
                }
                case 100 -> { // p2-bot-gear-easy, -normal, -hard: fixed gold and levels; each bot buys LevelMath.botPicks' list in order and keeps its reserve
                    var easy = bot(b, "easy", 400, "knight", "archer", "mage", "^");
                    Veterans.kit(b, easy, "knight").highest = 3; Veterans.kit(b, easy, "archer").highest = 3; Veterans.kit(b, easy, "mage").highest = 2;
                    var e = Veterans.botBuy(b, easy);
                    var rich = bot(b, "easy", 5000, "knight", "archer", "mage", "^");
                    Veterans.kit(b, rich, "knight").highest = 3; Veterans.kit(b, rich, "archer").highest = 3; Veterans.kit(b, rich, "mage").highest = 3;
                    var er = Veterans.botBuy(b, rich);
                    var wantE = List.of(opt("knight", "weapon", 2, 40), opt("knight", "armour", 2, 40), opt("archer", "weapon", 2, 40), opt("archer", "armour", 2, 40));
                    int reserveE = 8 * 22;
                    boolean okE = e.equals(wantE) && easy.gold == 240 && easy.gold >= reserveE && er.size() == 6 && er.stream().allMatch(o -> o.tier() == 2) && Veterans.peek(b, easy, "knight").weapon == 2 && Veterans.peek(b, easy, "archer").armour == 2;
                    Phase2Checks.report("p2-bot-gear-easy", okE, "an Easy bot with 400 gold, the Squires and archers seen at level 3 and the Sunweaver at 2 bought " + opts(e) + " (want " + opts(wantE) + ": banner order, weapon then armour, tier II at most), gold left " + easy.gold + " (want 240, at least the reserve " + reserveE + "); an Easy bot with 5000 gold and every type at level 3 bought " + opts(er) + " (want the six tier II buys, never tier III)");
                    var normal = bot(b, "normal", 500, "knight", "archer", "mage", "^");
                    Veterans.kit(b, normal, "knight").highest = 3; Veterans.kit(b, normal, "archer").highest = 3; Veterans.kit(b, normal, "mage").highest = 2;
                    var n = Veterans.botBuy(b, normal);
                    var wantN = List.of(opt("knight", "armour", 2, 40), opt("knight", "armour", 3, 80), opt("knight", "weapon", 2, 40), opt("knight", "weapon", 3, 80), opt("archer", "weapon", 2, 40), opt("archer", "weapon", 3, 80));
                    boolean okN = n.equals(wantN) && normal.gold == 140 && normal.gold >= 5 * 22;
                    Phase2Checks.report("p2-bot-gear-normal", okN, "a Normal bot with 500 gold (same levels) bought " + opts(n) + " (want " + opts(wantN) + ": infantry armour first, ranged weapon first, up to tier III), gold left " + normal.gold + " (want 140, at least the reserve " + 5 * 22 + ")");
                    var hard = bot(b, "hard", 300, "knight", "archer");
                    var ha1 = at(b, hard, "archer", -4, 6); var ha2 = at(b, hard, "archer", -2, 6); var hk = at(b, hard, "knight", 2, 6);
                    Veterans.credit(b, ha1, 8); Veterans.credit(b, ha2, 8); Veterans.credit(b, hk, 3);
                    var h = Veterans.botBuy(b, hard);
                    var wantH = List.of(opt("archer", "weapon", 2, 40), opt("archer", "weapon", 3, 80), opt("archer", "armour", 2, 40), opt("archer", "armour", 3, 80));
                    boolean okH = h.equals(wantH) && hard.gold == 60 && hard.gold >= 2 * 22 && ha1.weaponTier == 3 && ha1.armourTier == 3 && hk.weaponTier == 1;
                    Phase2Checks.report("p2-bot-gear-hard", okH, "a Hard bot with 300 gold, two archers at level 3 and one Squire at level 2 (" + cl(ha1, ha2, hk) + ") bought " + opts(h) + " (want " + opts(wantH) + ": the type with the most soldiers at level 2+ first), gold left " + hard.gold + " (want 60, at least the reserve 44); the archers wear " + ha1.weaponTier + "/" + ha1.armourTier + " (want 3/3) at once, the Squire " + hk.weaponTier + " (want 1)");
                    clearFixture(b, "bots"); next(b, 110);
                }
                case 110 -> { // p2-report-levels: the finished report's levels block, then the results page (screenshot gear-report)
                    var want = Veterans.levelRows(b, own);
                    b.campaign.finish(own.team);
                    var report = b.campaign.results.get(player);
                    var rows = report == null || !report.has("levels") ? new com.google.gson.JsonArray() : report.getAsJsonArray("levels");
                    boolean same = rows.size() == want.size();
                    StringBuilder got = new StringBuilder();
                    for (int i = 0; i < rows.size(); i++) {
                        var r = rows.get(i).getAsJsonObject(); got.append(r).append(' ');
                        if (i < want.size()) { var w = want.get(i); same &= r.get("troop").getAsString().equals(w.troop()) && r.get("highestLevel").getAsInt() == w.highest() && r.get("level3Count").getAsInt() == w.level3s() && r.get("weapon").getAsInt() == w.weapon() && r.get("armour").getAsInt() == w.armour(); }
                    }
                    var expect = List.of(new MatchStats.LevelRow("knight", 3, 2, 3, 3), new MatchStats.LevelRow("archer", 3, 1, 1, 3), new MatchStats.LevelRow("mage", 3, 1, 3, 1));
                    String text = want.isEmpty() ? "" : MatchStats.levelText(want.get(0), knight.name(), LevelMath.levelName(knight, want.get(0).highest()));
                    reportOk = same && want.equals(expect) && text.equals("reached Knight-Captain / 2 at level 3 / Weapon III, Armour III") && MatchStats.levelText(new MatchStats.LevelRow("mage", 1, 0, 1, 1), mage.name(), mage.name()).equals("no level ups / no gear bought")
                        && MatchStats.levelText(new MatchStats.LevelRow("mage", 3, 1, 3, 1), mage.name(), LevelMath.levelName(mage, 3)).equals("reached level 3 / 1 at level 3 / Weapon III") && !b.running && b.armouries.isEmpty();
                    reportWhy = "the report's levels rows " + got.toString().trim() + " (want " + want + ", which is " + expect + ")=" + same + "; the Squires' line \"" + text + "\"; the Armoury emptied as the round ended " + b.armouries.isEmpty() + "; ";
                    reportId = report == null ? null : report.get("id").getAsString();
                    reward = b.campaign.notices.get(player);
                    next(b, 111);
                }
                case 111 -> {
                    if (since(b) < 220 || !reportShot && since(b) < 600) return;
                    Phase2Checks.report("p2-report-levels", reportOk && reportShot && reportRowsClient == 3, reportWhy + "the results page opened on the client with " + reportRowsClient + " levels rows (want 3) and the picture gear-report was taken=" + reportShot);
                    String now = b.campaign.notices.get(player);
                    Long at = b.campaign.noticeAt.get(player);
                    boolean stays = reward != null && reward.equals(now) && at != null && at == Campaign.STANDING;
                    standOk = stays; standWhy = "the reward line \"" + reward + "\" after " + since(b) + " ticks: \"" + now + "\" (want the same)=" + stays + "; ";
                    b.campaign.notice(player, ACTION); next(b, 112);
                }
                case 112 -> {
                    if (since(b) < 220) return;
                    boolean gone = !ACTION.equals(b.campaign.notices.get(player));
                    standOk &= gone; standWhy += "an action's notice after " + since(b) + " ticks: \"" + b.campaign.notices.getOrDefault(player, "") + "\" (want gone)=" + gone + "; ";
                    var p = sp(b); if (p != null) b.campaign.joined(p); welcome = b.campaign.notices.get(player);
                    next(b, 113);
                }
                case 113 -> {
                    var p = sp(b); if (p == null || since(b) < 220) return;
                    String now = b.campaign.notices.get(player);
                    boolean stays = welcome != null && welcome.startsWith("Welcome to Graves and Glory") && welcome.equals(now);
                    // p2-gear-round-reset: an Armoury and a tally seeded in the lobby; a new round starts with neither
                    Veterans.kit(b, own, "knight").weapon = 3; var tally = Veterans.TALLY[0]; tally.level3s = 99; String seeded = kits(b, own);
                    if (!Phase1Checks.skirmish(b, p, 0, 1, "normal")) { Phase2Checks.report("p2-gear-round-reset", false, "the next round did not start at once"); next(b, DONE); return; }
                    boolean reset = b.running && b.armouries.isEmpty() && Veterans.peek(b, own, "knight") == null && Veterans.TALLY[0] != tally && Veterans.TALLY[0].level3s == 0 && Veterans.TALLY[0].bought.isEmpty();
                    Phase2Checks.report("p2-gear-round-reset", reset, "the commander's Armoury seeded " + seeded + " and the tally at 99 level 3s, then a new round started (running " + b.running + "): every Armoury " + kits(b, own) + " (want empty: " + b.armouries.isEmpty() + "), a fresh tally " + (Veterans.TALLY[0] != tally) + " at " + Veterans.TALLY[0].level3s + " level 3s");
                    standOk &= stays; standWhy += "the welcome \"" + welcome + "\" after 220 ticks: \"" + now + "\" (want the same)=" + stays + "; ";
                    // the wait line: a commander who joins while a round runs and he holds no seat
                    own.seated = false; b.campaign.joined(p); waiting = b.campaign.notices.get(player); own.seated = true; b.toBase(p, own.team);
                    next(b, 114);
                }
                case 114 -> {
                    if (since(b) < 220) return;
                    String now = b.campaign.notices.get(player);
                    boolean stays = waiting != null && waiting.startsWith("Battle in progress. Wait in camp for the next round.") && waiting.equals(now);
                    standOk &= stays; standWhy += "the wait line \"" + waiting + "\" after " + since(b) + " ticks: \"" + now + "\" (want the same)=" + stays;
                    Phase2Checks.report("p2-notice-standing", standOk, standWhy);
                    next(b, DONE);
                }
                default -> {}
            }
        }
    }
}
