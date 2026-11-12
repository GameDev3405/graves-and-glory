package gg.gravesandglory;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.lwjgl.glfw.GLFW;

/**
 * Suites levels, gear and armoury of the in-match progression plan (Tasks 4, 5 and 8, lane A). Never included in releases.
 * Phase2Checks.SUITES takes these entries through register (GRAVES_PHASE2_CHECKS=<suite>), and the checks print through Phase2Checks.report.
 * Task 4 builds levels, Task 5 gear, Task 8 the Armoury tab (its checks run on the real barracks screen; a suite still unbuilt reports NotBuilt).
 */
final class LevelChecks {
    private LevelChecks() {}

    /** The credits that make level 2 and level 3 (sheet levels.kills). Checks that need "just enough for level N" read these, never a literal. */
    static final int L2 = Spec.LEVELS[1].kills(), L3 = Spec.LEVELS[2].kills();

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("levels", Levels::new);
        suites.put("gear", Gear::new);
        suites.put("armoury", Armoury::new);
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
                    v.level = 3; v.credits = L3;
                    shoot(b, h1, v.mob, 1f); shoot(b, h2, v.mob, 1f); var t = mark(); kill(b, k, v);
                    var kills = events(t, -2, "kill");
                    boolean ok = !v.mob.isAlive() && h1.credits == 3 && h2.credits == 3 && k.credits == 3 && h1.level == LevelMath.levelFor(3) && h2.level == LevelMath.levelFor(3) && k.level == LevelMath.levelFor(3) && kills.size() == 1 && kills.get(0).value() == 3;
                    Phase2Checks.report("p2-bounty", ok, "a level-3 Dusk Squire killed by a Squire with two archers' help: " + cl(k, h1, h2) + " (want 3/" + LevelMath.levelFor(3) + " each: the bounty is the victim's level), kill " + (kills.isEmpty() ? "none" : kills.get(0).detail()));
                    clearFixture(b, "bounty"); next(b, 50);
                }
                case 50 -> { // p2-level-skip and p2-rank-sent: a fresh Squire, credited once the commander tracks him (so the change packets, not START_TRACKING, must carry the rank)
                    flashUnit = at(b, dawn, "knight", 0, -2); next(b, 54);
                }
                case 54 -> { // the level-3 credits at once from level 1
                    var u = flashUnit;
                    boolean tracked = net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(u.mob).contains(sp(b));
                    if (since(b) < 5 || !tracked && since(b) < 60) return;
                    u.mob.setHealth(20f);
                    float max0 = u.mob.getMaxHealth(); int ups0 = Veterans.levelUps, feeds0 = Veterans.pendingFeed.size(); rankSends0 = RankSync.changeSends; var t = mark();
                    Veterans.credit(b, u, L3);
                    var levels = values(events(t, u.mob.getId(), "level")); var ranks = values(events(t, u.mob.getId(), "rank")); var feeds = events(t, u.mob.getId(), "feed");
                    double wantMax = u.spec.health() * LevelMath.healthFactor(3, 1);
                    var feed = Veterans.pendingFeed.size() > feeds0 ? Veterans.pendingFeed.get(Veterans.pendingFeed.size() - 1) : null;
                    boolean ok = u.level == 3 && u.credits == L3 && Math.abs(u.mob.getMaxHealth() - wantMax) < 1e-3 && Math.abs(u.mob.getHealth() - (20 + (wantMax - max0))) < 1e-3
                        && levels.equals(List.of(2, 3)) && Veterans.levelUps == ups0 + 2 && feeds.size() == 1 && feed != null && Veterans.pendingFeed.size() == feeds0 + 1 && feed.team() == 0 && feed.levelName().equals("Knight-Captain") && name(u).equals("Knight-Captain");
                    Phase2Checks.report("p2-level-skip", ok, "a Squire at 0 credits and 20 of " + fmt(max0) + " health got " + L3 + ": " + cl(u) + " (want " + L3 + "/3), max health " + fmt(u.mob.getMaxHealth()) + " (want " + fmt(wantMax) + ", two steps of +5%), health " + fmt(u.mob.getHealth()) + " (want " + fmt(20 + wantMax - max0) + ": raised by the added health only), level ups logged " + levels + " (want [2, 3]), feed lines " + feeds.size() + " (want 1: " + (feed == null ? "none" : feed.levelName() + " for team " + feed.team()) + "), name \"" + name(u) + "\"");
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
                case 52 -> { // p2-level-up: the level-2 credits make an Order Squire a Knight with x1.05 health, current health raised by the delta only
                    var u = at(b, dawn, "knight", 0, -2); u.mob.setHealth(10f);
                    double base = u.spec.health(), wantMax = base * LevelMath.healthFactor(2, 1), wantHealth = 10 + base * (LevelMath.healthFactor(2, 1) - 1);
                    boolean fresh = u.level == 1 && name(u).equals("Order Squire") && Math.abs(u.mob.getMaxHealth() - base) < 1e-3;
                    Veterans.credit(b, u, L2 - 1); boolean still = u.level == 1 && u.credits == L2 - 1;
                    Veterans.credit(b, u, 1);
                    boolean ok = fresh && still && u.level == 2 && name(u).equals("Knight") && Math.abs(u.mob.getMaxHealth() - wantMax) < 1e-3 && Math.abs(u.mob.getHealth() - wantHealth) < 1e-3;
                    Phase2Checks.report("p2-level-up", ok, "an Order Squire (fresh: level 1, \"Order Squire\", " + fmt(base) + " health=" + fresh + ") at 10 health: " + (L2 - 1) + " credits keep him level 1=" + still + ", one more makes him " + cl(u) + " named \"" + name(u) + "\" (want Knight) with max health " + fmt(u.mob.getMaxHealth()) + " (want " + fmt(wantMax) + ") and health " + fmt(u.mob.getHealth()) + " (want " + fmt(wantHealth) + ")");
                    clearFixture(b, "level-up"); next(b, 53);
                }
                case 53 -> { // p2-level-name-veteran: a Mastery II Squire of the commander's own army at level 3
                    var pr = profile(b); knightOwned = pr.owned.get("knight"); knightHad = knightOwned != null;
                    if (!knightHad) { knightOwned = new Profile.Owned(); pr.owned.put("knight", knightOwned); }
                    knightMastery = knightOwned.mastery; knightOwned.mastery = 2;
                    var u = at(b, own, "knight", 0, -2); String fresh = name(u);
                    Veterans.credit(b, u, L3);
                    boolean ok = fresh.equals("Order Squire" + ArmyMobs.VETERAN) && u.level == 3 && name(u).equals("Knight-Captain (Proven)");
                    Phase2Checks.report("p2-level-name-veteran", ok, "a Mastery II Order Squire of the commander's army spawns as \"" + fresh + "\" and at " + cl(u) + " is named \"" + name(u) + "\" (want \"Knight-Captain (Proven)\")");
                    restoreKnight(b); clearFixture(b, "veteran"); next(b, 60);
                }
                case 60 -> { // p2-level-flash: the commander 8 blocks west of a Dawn Squire, looking at him
                    flashUnit = at(b, dawn, "knight", 0, 0);
                    stand(b, -8, 0, -90f, 8f); next(b, 61);
                }
                case 61 -> {
                    if (since(b) < 12) return; // the camera has settled: particles that arrive as the player lands are dropped
                    heard0 = (int) Phase1Checks.heardOnClient(Veterans.FLASH_SOUND.getId().toString()); flashes0 = Veterans.flashes;
                    Veterans.credit(b, flashUnit, L2); requestShot("levels-flash", 3); next(b, 62);
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
                    Veterans.credit(b, g, L2); String named = name(g); int sends = RankSync.changeSends; var t = mark();
                    Houses.forceRise = Boolean.TRUE; try { kill(b, k, g); } finally { Houses.forceRise = Boolean.FALSE; }
                    var after = events(t, -2, null);
                    boolean ok = g.mob.isAlive() && g.rose && g.level == 2 && g.credits == L2 && name(g).equals(named) && named.equals("Revenant") && RankSync.changeSends == sends && k.credits == 0 && after.isEmpty() && dusk.traits.getOrDefault("graveborn", 0) >= 1;
                    Phase2Checks.report("p2-rise-keeps-level", ok, "a level-2 Risen of a Graveborn army (tier " + dusk.traits.getOrDefault("graveborn", 0) + ") named \"" + named + "\" was struck down with the roll forced: alive " + g.mob.isAlive() + ", rose " + g.rose + ", " + cl(g) + " (want " + L2 + "/2), name \"" + name(g) + "\", rank packets +" + (RankSync.changeSends - sends) + " (want 0), the striker " + cl(k) + " (want 0: the rising blow is no kill), progression events " + after.size() + " (want 0)");
                    clearFixture(b, "rise"); next(b, 71);
                }
                case 71 -> { // p2-temporary-no-credit: a temporary Risen kills and earns nothing; a temporary Risen killed counts 1
                    var temp = at(b, dawn, "revenant", -4, -2); temp.temporary = true; temp.expiresAt = b.time + 1000;
                    var h = at(b, dawn, "archer", -4, -8); v = victim(b, dusk, "knight", -4, 0);
                    int troopKills = dawn.stats.troopKills.getOrDefault("revenant", 0);
                    shoot(b, h, v.mob, 1f); blow(b, temp, v.mob, 1f); kill(b, temp, v);
                    boolean earnsNothing = !v.mob.isAlive() && temp.credits == 0 && temp.level == 1 && h.credits == 1 && dawn.stats.troopKills.getOrDefault("revenant", 0) == troopKills;
                    var t2 = at(b, dusk, "revenant", 4, 0); t2.temporary = true; t2.expiresAt = b.time + 1000; t2.level = 3; t2.credits = L3;
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
                    boolean first = down && dk.credits == 3 && da.credits == 3 && dk.level == LevelMath.levelFor(3) && da.level == LevelMath.levelFor(3);
                    String why = "an archer hit the commander and a Squire struck him down (respawning " + down + "): " + cl(dk, da) + " (want 3/" + LevelMath.levelFor(3) + " each: a commander counts progression.commanderBounty " + Spec.PROGRESSION[0].commanderBounty() + ")=" + first + "; ";
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
                    Veterans.credit(b, first, L3); String high = name(first) + " " + cl(first);
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
                        shoot(b, h, v.mob, 1f); kill(b, k, v); Veterans.credit(b, k, L3);
                        var after = events(t, -2, null);
                        boolean ok = !Veterans.enabled() && !v.mob.isAlive() && h.credits == 0 && k.credits == 0 && k.level == 1 && after.isEmpty() && dawn.stats.kills == kills + 1 && dawn.stats.goldEarned == gold + Spec.ECONOMY[0].killGold();
                        Phase2Checks.report("p2-progression-off", ok, "with progression off (the sim override) an archer's hit and a Squire's kill, then " + L3 + " credits handed over: " + cl(h, k) + " (want 0/1 each), progression events " + after.size() + " (want 0), the army's kills +" + (dawn.stats.kills - kills) + " and gold +" + (dawn.stats.goldEarned - gold) + " (want 1 and " + Spec.ECONOMY[0].killGold() + ": unchanged)");
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
                        Veterans.credit(b, u, L2);
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
                    boolean ok = down && !arch.mob.isAlive() && helper.credits == 3 && helper.level == LevelMath.levelFor(3) && arch.credits == 0;
                    Phase2Checks.report("p2-commander-fallen-shot", ok, "a Dusk Squire struck the commander, then a Dusk archer's arrow struck him down " + since(b) + " ticks after the archer fell (respawning " + down + "): " + cl(helper, arch) + " (want the Squire 3/" + LevelMath.levelFor(3) + ": the shot is his army's blow, Battle.fallenSquad; the fallen archer 0)");
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
                    zombie.discard(); revive(b); clearFixture(b, "commander-unknown"); next(b, 98);
                }
                case 98 -> { // p2-level-killing-blow, p2-level-killing-reap: the blow that kills crosses a level and the new row applies on that blow (Task 6 review M1, the comment above Combat.onHit; Task 10)
                    // Order Longbow one kill from level 2: his killing arrow levels him inside target.damage (AFTER_DEATH), and the level-2 pierce (reach 3; reach 2 at level 1) takes the Squire 2.5 behind the victim on that same arrow
                    var lb = at(b, dawn, "longbow", -6, -10); lb.credits = L2 - 1;
                    var vk = at(b, dusk, "knight", -6, 0); var behind = victim(b, dusk, "knight", -6, 2.5);
                    var lb0 = at(b, dawn, "longbow", 6, -10); // the control: no credit, his kill leaves him at level 1, and reach 2 misses the Squire 2.5 behind
                    var vk0 = at(b, dusk, "knight", 6, 0); var behind0 = victim(b, dusk, "knight", 6, 2.5);
                    float pierce = (float) (.7 * Abilities.baseHit(b, lb));
                    var hm = Combat.LAST.peekLast();
                    vk.mob.setHealth(1f); shoot(b, lb, vk.mob, 50f);
                    var onBehind = Phase2Checks.Gilded.hitOn(behind.mob, hm);
                    hm = Combat.LAST.peekLast();
                    vk0.mob.setHealth(1f); shoot(b, lb0, vk0.mob, 50f);
                    var onBehind0 = Phase2Checks.Gilded.hitOn(behind0.mob, hm);
                    boolean crossed = !vk.mob.isAlive() && lb.credits == L2 && lb.level == 2 && Abilities.row(lb).id().equals("longbow_l2");
                    boolean ok = crossed && onBehind != null && Math.abs(onBehind.base() - pierce) < 1e-4 && !vk0.mob.isAlive() && lb0.credits == 1 && lb0.level == 1 && onBehind0 == null;
                    Phase2Checks.report("p2-level-killing-blow", ok, "an Order Longbow with " + (L2 - 1) + " credits shot a Dusk Squire dead (dead " + !vk.mob.isAlive() + "): " + cl(lb) + " (want " + L2 + "/2, the kill made him level 2), his row " + Abilities.row(lb).id() + "; on that same arrow the Squire 2.5 behind took " + (onBehind == null ? "nothing" : fmt(onBehind.base())) + " (want the level-2 pierce 0.7 x " + fmt(Abilities.baseHit(b, lb)) + " = " + fmt(pierce) + "); the control with no credit killed his Squire (dead " + !vk0.mob.isAlive() + "): " + cl(lb0) + " (want 1/1) and his pierce took " + (onBehind0 == null ? "nothing" : fmt(onBehind0.base())) + " from the Squire behind (want nothing: reach 2)");
                    // Grave Reaper one kill from level 3: Veterans.award credits him before it calls Abilities.onKill, so Reap (level 3 only) heals 25% of his new maximum on the kill that makes him level 3; one that stays at level 2 heals nothing
                    var rp = at(b, dawn, "reaper", -2, -3); Veterans.credit(b, rp, L3 - 1);
                    var rp2 = at(b, dawn, "reaper", 2, -3); Veterans.credit(b, rp2, L2);
                    var vr = at(b, dusk, "knight", -2, 0); var vr2 = at(b, dusk, "knight", 2, 0);
                    rp.mob.setHealth(rp.mob.getMaxHealth() * .4f); rp2.mob.setHealth(rp2.mob.getMaxHealth() * .4f);
                    float max2 = rp.mob.getMaxHealth(), h0 = rp.mob.getHealth(), h20 = rp2.mob.getHealth(); int lvl0 = rp.level;
                    kill(b, rp, vr); kill(b, rp2, vr2);
                    float max3 = rp.mob.getMaxHealth(), stepped = Math.min(max3, h0 + (max3 - max2)), reaped = rp.mob.getHealth() - stepped;
                    boolean reap = lvl0 == 2 && rp.level == 3 && rp.credits == L3 && !vr.mob.isAlive() && Math.abs(reaped - .25f * max3) < 1e-2 && rp2.level == 2 && rp2.credits == L2 + 1 && !vr2.mob.isAlive() && Math.abs(rp2.mob.getHealth() - h20) < 1e-3;
                    Phase2Checks.report("p2-level-killing-reap", reap, "a Grave Reaper at level " + lvl0 + " with " + (L3 - 1) + " credits and 40% health killed a Dusk Squire: " + cl(rp) + " (want " + L3 + "/3), health " + fmt(rp.mob.getHealth()) + " of " + fmt(max3) + "; the level up gave " + fmt(stepped - h0) + " and Reap " + fmt(reaped) + " more (want 25% of the new maximum = " + fmt(.25f * max3) + "); the control with " + L2 + " credits killed his Squire and stayed at " + cl(rp2) + " (want " + (L2 + 1) + "/2), health " + fmt(rp2.mob.getHealth()) + " (want " + fmt(h20) + ", no Reap)");
                    clearFixture(b, "killing-blow"); next(b, 99);
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
    static class Gear extends Levels {
        static final String ACTION = "Gear check: an action's notice expires";
        /** The stage that ends the suite: past every numbered stage (the levels suite's 99 would cut this one short at its stage 100). */
        static final int DONE = 999;
        /** How long a check waits before it reads an action's notice as gone: the expiry runs at b.time % 20 == 0 and drops what is older than Campaign.NOTICE_TICKS, so a notice posted on such a tick goes 220 ticks later, in that tick's pass. Both the mod's tick and a suite's serverTick are END_SERVER_TICK handlers and the mod's runs first, so 220 is enough today; one more tick keeps the check right if the order changes (review M4). */
        static final int NOTICE_WAIT = 221;
        Spec.TroopsRow knight, archer, mage;
        List<String> bannerBefore;
        int knightStars; boolean starsSet;
        GravesAndGlory.Capture held, unheld; int heldOwner, unheldOwner, heldProgress; long heldSince; boolean heldContested;
        GravesAndGlory.Unit u1, u2, u3;
        int goldTrained, paid; java.util.Set<Integer> fieldedBefore = new java.util.HashSet<>();
        // p2-armoury-state's client half: the server's Armoury and glint bits the client must hold
        volatile LevelMath.Armoury armouryWant; volatile Map<String, Boolean> glintWant; volatile String armourySeen, armouryLast = "";
        // p2-report-levels' client half: the reports the client page must show (id -> picture name), the levels rows it held and the pictures taken
        final Map<String, String> reportShots = new java.util.concurrent.ConcurrentHashMap<>();
        final Map<String, Integer> reportRows = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.Set<String> reportDone = java.util.concurrent.ConcurrentHashMap.newKeySet();
        volatile String screenId = ""; int screenTicks;
        String firstReport, secondReport, thirdReport;
        boolean reportOk; String reportWhy = "";
        // p2-report-levels-fit: what the client measured (the smallest canvas, every troop, and each page it drew)
        final List<String> fitNotes = new java.util.concurrent.CopyOnWriteArrayList<>(), fitFails = new java.util.concurrent.CopyOnWriteArrayList<>();
        volatile String minFit = ""; volatile boolean minFitOk, minFitDone;
        // p2-notice-standing, p2-notice-wait, p2-notice-left
        String reward, welcome, waiting, standWhy = "", waitWhy = ""; boolean standOk, waitOk;

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
            if (c.currentScreen instanceof CampScreen cs && "results".equals(cs.page) && CampaignClient.result.has("levels") && CampaignClient.result.has("id")) {
                String id = CampaignClient.result.get("id").getAsString(), name = reportShots.get(id);
                if (name != null && !reportDone.contains(id)) {
                    if (!id.equals(screenId)) { screenId = id; screenTicks = 0; }
                    if (++screenTicks == 5) { // five ticks on the page: its rows and its words measured as drawn, then the picture
                        var rows = CampaignClient.result.getAsJsonArray("levels"); reportRows.put(id, rows.size());
                        fitPage(c, cs, name, rows);
                        Phase2Checks.shot(c, name); reportDone.add(id);
                    }
                }
            }
            if (tick == 30) noticeRedismissed();
            if (tick == 40) fitSmallest(c);
            if (stage == DONE && !finished) {
                finished = true; c.setScreen(null);
                Phase2Checks.report("p2-report-levels-fit", minFitDone && minFitOk && fitFails.isEmpty() && reportDone.size() == 3, minFit + "; the pages the client drew: " + (fitNotes.isEmpty() ? "none" : String.join("; ", fitNotes)) + "; pictures taken " + reportDone.size() + " (want 3)");
                Phase2Checks.finish(c, "gear");
            }
            if (tick > 16000 && !finished) { finished = true; Phase2Checks.report("p2-gear-timeout", false, "stuck in stage " + stage); Phase2Checks.finish(c, "gear"); }
        }

        /** Whether a levels row was drawn whole in its cell: the words after the name uncut, the name whole or, in the short form only, given way to an ellipsis that keeps at least 12 letters, and the two together inside the cell. Returns "" when whole, else what was cut. */
        static String cut(net.minecraft.client.font.TextRenderer font, com.google.gson.JsonObject row, int cell, boolean compact) {
            var words = CampScreen.levelWords(row, compact); var drawn = CampScreen.levelDrawn(font, row, cell, compact);
            if (words == null || drawn == null) return "an unknown troop " + row;
            boolean nameWhole = drawn[0].equals(words[0]), nameGaveWay = compact && drawn[0].endsWith("...") && drawn[0].length() >= 15 && words[0].startsWith(drawn[0].substring(0, drawn[0].length() - 3));
            int need = font.getWidth(drawn[0]) + 8 + font.getWidth(drawn[1]);
            if (drawn[1].equals(words[1]) && (nameWhole || nameGaveWay) && need <= cell) return "";
            return words[0] + " \"" + words[1] + "\" drawn as \"" + drawn[0] + "\" + \"" + drawn[1] + "\" needs " + need + " px";
        }
        /** The levels rows of a page the client drew, measured: the cell they got, the form, and every row drawn whole. */
        void fitPage(MinecraftClient c, CampScreen cs, String name, com.google.gson.JsonArray rows) {
            int n = rows.size(), cell = CampScreen.levelCell(n, cs.width - 194 - 22), trimmed = 0, widest = 0; boolean compact = CampScreen.levelCompact(n);
            List<String> clipped = new ArrayList<>();
            for (var e : rows) {
                var row = e.getAsJsonObject(); String why = cut(c.textRenderer, row, cell, compact);
                if (!why.isEmpty()) clipped.add(why);
                var drawn = CampScreen.levelDrawn(c.textRenderer, row, cell, compact);
                if (drawn != null) { widest = Math.max(widest, c.textRenderer.getWidth(drawn[0]) + 8 + c.textRenderer.getWidth(drawn[1])); if (drawn[0].endsWith("...")) trimmed++; }
            }
            fitNotes.add(name + ": " + n + " rows as the " + (compact ? "short" : "long") + " line in cells of " + cell + " px (page " + cs.width + " wide), the widest drawn is " + widest + " px, " + trimmed + " names given way" + (clipped.isEmpty() ? " (all whole)" : " CUT " + clipped));
            if (!clipped.isEmpty()) fitFails.add(name);
        }
        /** The smallest canvas the camp page runs on (760 wide: the page is 544 inside the nav and the margin): for one to eight rows, every troop with the widest words a row can have (level 3, 99 at level 3, Weapon III and Armour III) is drawn whole. */
        void fitSmallest(MinecraftClient c) {
            int page = 760 - 194 - 22, widest = 0, bad = 0, gaveWay = 0; String first = "", wideName = "";
            for (int n = 1; n <= 8; n++) {
                int cell = CampScreen.levelCell(n, page); boolean compact = CampScreen.levelCompact(n);
                for (var t : Spec.TROOPS) {
                    var row = new com.google.gson.JsonObject();
                    row.addProperty("troop", t.id()); row.addProperty("highestLevel", 3); row.addProperty("level3Count", 99); row.addProperty("weapon", 3); row.addProperty("armour", 3);
                    String why = cut(c.textRenderer, row, cell, compact); var drawn = CampScreen.levelDrawn(c.textRenderer, row, cell, compact);
                    int need = drawn == null ? 0 : c.textRenderer.getWidth(drawn[0]) + 8 + c.textRenderer.getWidth(drawn[1]);
                    if (need > widest) { widest = need; wideName = t.name(); }
                    if (drawn != null && drawn[0].endsWith("...")) gaveWay++;
                    if (!why.isEmpty()) { bad++; if (first.isEmpty()) first = "in a block of " + n + " rows (cell " + cell + " px) " + why; }
                }
            }
            minFitOk = bad == 0; minFit = "at the smallest canvas (page " + page + " wide) " + bad + " of " + (8 * Spec.TROOPS.length) + " troop-and-size cells are cut" + (bad == 0 ? "" : ", the first: " + first) + "; the widest drawn row is " + wideName + " at " + widest + " px; " + gaveWay + " names gave way to an ellipsis (short form only)";
            minFitDone = true;
        }
        /** p2-notice-redismissed (client only): a report line the player dismissed stays hidden when the same words arrive again, and shows when the next match's report opens. */
        void noticeRedismissed() {
            String n0 = CampaignClient.notice, d0 = CampaignClient.dismissed, r0 = CampaignClient.shownResult, line = "Victory / +100 collection gold";
            CampaignClient.notice = line; CampaignClient.dismissed = line; // the first match's reward line, left behind by a page change
            CampaignClient.acceptNotice(line); // the next state packet carries the same words
            boolean held = CampaignClient.shownNotice().isEmpty();
            CampaignClient.reportOpened("harness-next-report"); // the second match's report opens with the same words
            boolean again = line.equals(CampaignClient.shownNotice());
            CampaignClient.acceptNotice("Another line"); boolean other = "Another line".equals(CampaignClient.shownNotice());
            CampaignClient.notice = n0; CampaignClient.dismissed = d0; CampaignClient.shownResult = r0;
            Phase2Checks.report("p2-notice-redismissed", held && again && other, "a reward line dismissed by a page change: the same words in the next state packet " + (held ? "stay hidden" : "SHOW AGAIN") + ", the same words when the next report opens " + (again ? "show" : "STAY HIDDEN (the new match's line is swallowed)") + ", another text " + (other ? "shows" : "does not show"));
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
                    boolean lockedOk = locked.equals("Needs a Rank 2 Knight") && own.gold == 1000 && Veterans.peek(b, own, "knight") == null && Veterans.row(b, own, knight).weapon2() == LevelMath.Buy.LOCKED;
                    Phase2Checks.report("p2-gear-locked", lockedOk, "before any Squire reached Rank 2, gear:knight:weapon:2 left the notice \"" + locked + "\" (want \"Needs a Rank 2 Knight\"), gold " + own.gold + " (want 1000), the row " + states(Veterans.row(b, own, knight)));
                    Veterans.credit(b, u1, L2); Veterans.credit(b, u2, L2);
                    var r = Veterans.row(b, own, knight);
                    String three = say(b, "gear:knight:weapon:3");
                    boolean open = u1.level == 2 && r.weapon2() == LevelMath.Buy.OPEN && r.weapon3() == LevelMath.Buy.LOCKED && r.armour2() == LevelMath.Buy.OPEN && r.armour3() == LevelMath.Buy.LOCKED
                        && three.equals("Needs a Rank 3 Knight-Captain") && own.gold == 1000 && Veterans.row(b, own, archer).weapon2() == LevelMath.Buy.LOCKED;
                    Phase2Checks.report("p2-gear-open-l2", open, "two Squires reached " + cl(u1, u2) + ": the row " + states(r) + " (want OPEN/LOCKED/OPEN/LOCKED), gear:knight:weapon:3 \"" + three + "\" (want \"Needs a Rank 3 Knight-Captain\"), the archers' row " + states(Veterans.row(b, own, archer)) + " (want LOCKED: no archer levelled), gold " + own.gold);
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
                    u3.mob.setHealth(10f); Veterans.credit(b, u3, L2);
                    boolean later = tierOne && u3.level == 2 && u3.weaponTier == 2 && u3.armourTier == 2 && Math.abs(u3.mob.getMaxHealth() - want2) < 1e-3 && Math.abs(u3.mob.getHealth() - (10 + base * .15)) < 1e-3;
                    Phase2Checks.report("p2-gear-wear-later", later, "the Squires who levelled before the buy wore tier I until it=" + tierOne + "; a Squire reaching level 2 after both buys: " + cl(u3) + " wears weapon " + u3.weaponTier + " armour " + u3.armourTier + " (want 2/2) at max " + fmt(u3.mob.getMaxHealth()) + " (want " + fmt(want2) + ") and health 10 -> " + fmt(u3.mob.getHealth()) + " (want " + fmt(10 + base * .15) + ": the level step and the armour step)");
                    clearFixture(b, "location"); next(b, 20);
                }
                case 20 -> { // p2-gear-iii-needs-ii: a level-3 archer; Armour III is refused until Armour II is bought
                    var a1 = at(b, own, "archer", 0, -6); float max0 = a1.mob.getMaxHealth(); Veterans.credit(b, a1, L3);
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
                    var m = at(b, own, "mage", 2, -8); Veterans.credit(b, m, L2);
                    var before = Veterans.row(b, own, mage);
                    fall(b, m);
                    var after = Veterans.row(b, own, mage); boolean none = b.fielded(own).noneMatch(u -> u.spec == mage);
                    boolean ok = !m.mob.isAlive() && none && before.weapon2() == LevelMath.Buy.OPEN && after.weapon2() == LevelMath.Buy.OPEN && after.armour2() == LevelMath.Buy.OPEN && after.weapon3() == LevelMath.Buy.LOCKED;
                    Phase2Checks.report("p2-gear-open-stays", ok, "a Sunweaver reached Rank 2 (row " + states(before) + ") and fell (dead " + !m.mob.isAlive() + ", no Sunweaver fielded " + none + "): the row " + states(after) + " (want OPEN/LOCKED/OPEN/LOCKED: what a level opened stays open)");
                    clearFixture(b, "open-stays"); next(b, 40);
                }
                case 40 -> { // p2-gear-price: Common 40/80, Epic 80/160, flat (no discount touches gear)
                    var k3 = at(b, own, "knight", -4, -4); Veterans.credit(b, k3, L3); var m3 = at(b, own, "mage", 4, -8); Veterans.credit(b, m3, L3);
                    toBarracks(b); int g0 = own.gold;
                    String kw3 = say(b, "gear:knight:weapon:3"); int g1 = own.gold;
                    String mw2 = say(b, "gear:mage:weapon:2"); int g2 = own.gold;
                    String mw3 = say(b, "gear:mage:weapon:3"); int g3 = own.gold;
                    home(b);
                    boolean ok = g0 - g1 == 80 && g1 - g2 == 80 && g2 - g3 == 160 && kw3.equals("Order Squire: Weapon III bought for 80 gold") && mw2.equals("Sunweaver: Weapon II bought for 80 gold") && mw3.equals("Sunweaver: Weapon III bought for 160 gold")
                        && k3.weaponTier == 3 && m3.weaponTier == 3 && LevelMath.price("common", 2) == 40 && LevelMath.price("epic", 2) == 80;
                    Phase2Checks.report("p2-gear-price", ok, "a commander bought Squire Weapon III for " + (g0 - g1) + " (want 80; Weapon II was 40), Sunweaver Weapon II for " + (g1 - g2) + " (want 80) and Weapon III for " + (g2 - g3) + " (want 160): \"" + kw3 + "\", \"" + mw2 + "\", \"" + mw3 + "\"; worn " + k3.weaponTier + "/" + m3.weaponTier + " (want 3/3)");
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
                    Veterans.credit(b, fresh, L2);
                    boolean levels = fresh.level == 2 && fresh.weaponTier == 2 && fresh.armourTier == 2;
                    boolean ok = paid == knight.cost() && goldNow >= goldTrained && tierOne && levels && kit.weapon == 3 && kit.armour == 2;
                    Phase2Checks.report("p2-gear-rebuy-tier1", ok, "train:knight at the barracks took " + paid + " gold (want the troop's " + knight.cost() + ", no gear charge), and gold stayed at or above " + goldTrained + " to the spawn (" + goldNow + "); the new Squire arrived at " + arrived + " (want level 1, tier I/I, " + fmt(want) + ") though the army bought " + kit + "; at level 2 he wears " + fresh.weaponTier + "/" + fresh.armourTier + " (want 2/2: min of his level and the tier bought)");
                    clearFixture(b, "rebuy"); next(b, 70);
                }
                case 70 -> { // p2-gear-allies-separate: a second Dawn army keeps its own Armoury
                    dawn.banner = new ArrayList<>(List.of("knight", "archer"));
                    var dk = at(b, dawn, "knight", 0, -4); Veterans.credit(b, dk, L2);
                    var ok2 = at(b, own, "knight", 2, -4); Veterans.credit(b, ok2, L2);
                    var dr = Veterans.row(b, dawn, knight); var or = Veterans.row(b, own, knight); String ownKit = Veterans.peek(b, own, "knight").toString();
                    boolean untouched = dk.weaponTier == 1 && dk.armourTier == 1 && dr.weapon2() == LevelMath.Buy.OPEN && dr.armour2() == LevelMath.Buy.OPEN && or.weapon2() == LevelMath.Buy.BOUGHT && Veterans.peek(b, dawn, "knight") != Veterans.peek(b, own, "knight");
                    String wore = dk.weaponTier + "/" + dk.armourTier;
                    String why = Veterans.refusal(b, dawn, knight, LevelMath.WEAPON, 2, true);
                    if (why.isEmpty()) Veterans.purchase(b, dawn, knight, LevelMath.WEAPON, 2, LevelMath.price("common", 2));
                    boolean apart = why.isEmpty() && dk.weaponTier == 2 && Veterans.peek(b, own, "knight").toString().equals(ownKit) && ok2.weaponTier == 2 && ok2.armourTier == 2 && Veterans.row(b, dawn, knight).weapon2() == LevelMath.Buy.BOUGHT;
                    Phase2Checks.report("p2-gear-allies-separate", untouched && apart, "a Squire of a second Dawn army reached Rank 2 after the commander's army bought Squire gear: he wears " + wore + " before his army buys (want 1/1), his army's row " + states(dr) + " (want OPEN on tier II) against the commander's " + states(or) + "=" + untouched + "; the second army buys Weapon II (refusal \"" + why + "\"): he wears weapon " + dk.weaponTier + " (want 2), the commander's Squires' Armoury " + Veterans.peek(b, own, "knight") + " (want unchanged " + ownKit + ")=" + apart);
                    clearFixture(b, "allies"); next(b, 80);
                }
                case 80 -> { // p2-gear-stats: a star-3 Squire at level 3 with Weapon III and Armour III: health and blows x1.30 each, stars on top
                    var pr = profile(b); var owned = pr.owned.get("knight"); knightHad = owned != null;
                    if (!knightHad) { owned = new Profile.Owned(); pr.owned.put("knight", owned); }
                    knightStars = owned.stars; owned.stars = 3; starsSet = true;
                    double star = b.campaign.health(own, knight);
                    var ks = at(b, own, "knight", 0, -2); double max0 = ks.mob.getMaxHealth();
                    toBarracks(b); String a3 = say(b, "gear:knight:armour:3"); home(b);
                    Veterans.credit(b, ks, L3);
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
                    Veterans.credit(b, ha1, L3); Veterans.credit(b, ha2, L3); Veterans.credit(b, hk, L2);
                    var h = Veterans.botBuy(b, hard);
                    var wantH = List.of(opt("archer", "weapon", 2, 40), opt("archer", "weapon", 3, 80), opt("archer", "armour", 2, 40), opt("archer", "armour", 3, 80));
                    boolean okH = h.equals(wantH) && hard.gold == 60 && hard.gold >= 2 * 22 && ha1.weaponTier == 3 && ha1.armourTier == 3 && hk.weaponTier == 1;
                    Phase2Checks.report("p2-bot-gear-hard", okH, "a Hard bot with 300 gold, two archers at level 3 and one Squire at level 2 (" + cl(ha1, ha2, hk) + ") bought " + opts(h) + " (want " + opts(wantH) + ": the type with the most soldiers at level 2+ first), gold left " + hard.gold + " (want 60, at least the reserve 44); the archers wear " + ha1.weaponTier + "/" + ha1.armourTier + " (want 3/3) at once, the Squire " + hk.weaponTier + " (want 1)");
                    clearFixture(b, "bots"); next(b, 110);
                }
                case 110 -> { // p2-report-levels: the finished report's levels block, then the results page (screenshot gear-report)
                    // The rows below hold only for stages 10 to 100 as they are: Squires at Weapon III (stage 40) and Armour III (stage 80) with two at level 3, archers at Armour III (stage 20) with one at level 3, the Sunweaver at Weapon III (stage 40) with one at level 3. A stage taken out or moved fails this check for that reason, not the report's.
                    var want = Veterans.levelRows(b, own);
                    b.campaign.finish(own.team);
                    var report = b.campaign.results.get(player);
                    StringBuilder got = new StringBuilder(); boolean same = rowsMatch(report, want, got);
                    var expect = List.of(new MatchStats.LevelRow("knight", 3, 2, 3, 3), new MatchStats.LevelRow("archer", 3, 1, 1, 3), new MatchStats.LevelRow("mage", 3, 1, 3, 1));
                    String text = want.isEmpty() ? "" : MatchStats.levelText(want.get(0), knight.name(), LevelMath.levelName(knight, want.get(0).highest()));
                    reportOk = same && want.equals(expect) && text.equals("reached Knight-Captain / 2 at Rank 3 / Weapon III, Armour III") && MatchStats.levelText(new MatchStats.LevelRow("mage", 1, 0, 1, 1), mage.name(), mage.name()).equals("no Rank gains / no gear bought")
                        && MatchStats.levelText(new MatchStats.LevelRow("mage", 3, 1, 3, 1), mage.name(), LevelMath.levelName(mage, 3)).equals("reached Rank 3 / 1 at Rank 3 / Weapon III") && !b.running && b.armouries.isEmpty();
                    reportWhy = "the report's levels rows " + got.toString().trim() + " (want " + want + ", which is " + expect + ")=" + same + "; the Squires' line \"" + text + "\"; the Armoury emptied as the round ended " + b.armouries.isEmpty() + "; ";
                    firstReport = report == null ? "none" : report.get("id").getAsString(); reportShots.put(firstReport, "gear-report");
                    reward = b.campaign.notices.get(player);
                    next(b, 111);
                }
                case 111 -> {
                    if (since(b) < 220 || !reportDone.contains(firstReport) && since(b) < 600) return;
                    boolean shot = reportDone.contains(firstReport); int rows = reportRows.getOrDefault(firstReport, -1);
                    Phase2Checks.report("p2-report-levels", reportOk && shot && rows == 3, reportWhy + "the results page opened on the client with " + rows + " levels rows (want 3) and the picture gear-report was taken=" + shot);
                    String now = b.campaign.notices.get(player);
                    Long at = b.campaign.noticeAt.get(player);
                    boolean stays = reward != null && reward.equals(now) && at != null && at == Campaign.STANDING;
                    standOk = stays; standWhy = "the reward line \"" + reward + "\" after " + since(b) + " ticks: \"" + now + "\" (want the same)=" + stays + "; ";
                    next(b, 112);
                }
                case 112 -> { // an action's notice is posted on the tick the once-a-second expiry runs: the worst phase for the check at NOTICE_WAIT
                    if (b.time % 20 != 0) return;
                    b.campaign.notice(player, ACTION); next(b, 113);
                }
                case 113 -> {
                    if (since(b) < NOTICE_WAIT) return;
                    boolean gone = !ACTION.equals(b.campaign.notices.get(player));
                    standOk &= gone; standWhy += "an action's notice after " + since(b) + " ticks: \"" + b.campaign.notices.getOrDefault(player, "") + "\" (want gone)=" + gone + "; ";
                    var p = sp(b); if (p != null) b.campaign.joined(p); welcome = b.campaign.notices.get(player);
                    next(b, 114);
                }
                case 114 -> {
                    var p = sp(b); if (p == null || since(b) < NOTICE_WAIT) return;
                    String now = b.campaign.notices.get(player);
                    boolean stays = welcome != null && welcome.startsWith("Welcome to Graves and Glory") && welcome.equals(now);
                    // p2-gear-round-reset: an Armoury and a tally seeded in the lobby; a new round starts with neither
                    Veterans.kit(b, own, "knight").weapon = 3; var tally = Veterans.TALLY[0]; tally.level3s = 99; String seeded = kits(b, own);
                    if (!Phase1Checks.skirmish(b, p, 0, 1, "normal")) { Phase2Checks.report("p2-gear-round-reset", false, "the next round did not start at once"); next(b, DONE); return; }
                    boolean reset = b.running && b.armouries.isEmpty() && Veterans.peek(b, own, "knight") == null && Veterans.TALLY[0] != tally && Veterans.TALLY[0].level3s == 0 && Veterans.TALLY[0].bought.isEmpty();
                    Phase2Checks.report("p2-gear-round-reset", reset, "the commander's Armoury seeded " + seeded + " and the tally at 99 level 3s, then a new round started (running " + b.running + "): every Armoury " + kits(b, own) + " (want empty: " + b.armouries.isEmpty() + "), a fresh tally " + (Veterans.TALLY[0] != tally) + " at " + Veterans.TALLY[0].level3s + " level 3s");
                    standOk &= stays; standWhy += "the welcome \"" + welcome + "\" after " + since(b) + " ticks: \"" + now + "\" (want the same)=" + stays;
                    Phase2Checks.report("p2-notice-standing", standOk, standWhy);
                    // the wait line: a commander who joins while a round runs and he holds no seat
                    own.seated = false; b.campaign.joined(p); waiting = b.campaign.notices.get(player); own.seated = true; b.toBase(p, own.team);
                    next(b, 115);
                }
                case 115 -> { // p2-notice-wait: the wait line is an action's notice (gone after 10 s), and it goes when the round ends; p2-notice-left
                    var p = sp(b); if (p == null || since(b) < NOTICE_WAIT) return;
                    String now = b.campaign.notices.get(player);
                    boolean wait = waiting != null && waiting.startsWith("Battle in progress. Wait in camp for the next round."), expired = wait && !waiting.equals(now);
                    waitWhy = "the wait line \"" + waiting + "\" after " + since(b) + " ticks: \"" + now + "\" (want gone)=" + expired + "; ";
                    own.seated = false; b.campaign.joined(p); String second = b.campaign.notices.get(player); own.seated = true; b.toBase(p, own.team);
                    b.campaign.returnLobby();
                    String after = b.campaign.notices.get(player); Long afterAt = b.campaign.noticeAt.get(player);
                    boolean ended = second != null && second.equals(waiting) && !b.running && after == null && afterAt == null;
                    waitWhy += "the wait line posted again \"" + second + "\", then the round ended (running " + b.running + "): \"" + after + "\" (want none)=" + ended;
                    Phase2Checks.report("p2-notice-wait", expired && ended, waitWhy);
                    // a player who leaves takes his notice entries with him, a standing line's too
                    UUID a = UUID.randomUUID(), z = UUID.randomUUID();
                    b.campaign.standing(a, "Harness standing line"); b.campaign.notice(z, "Harness action notice");
                    boolean had = b.campaign.notices.containsKey(a) && b.campaign.noticeAt.containsKey(a) && b.campaign.notices.containsKey(z) && b.campaign.noticeAt.containsKey(z);
                    b.campaign.left(a); b.campaign.left(z);
                    boolean cleared = !b.campaign.notices.containsKey(a) && !b.campaign.noticeAt.containsKey(a) && !b.campaign.notices.containsKey(z) && !b.campaign.noticeAt.containsKey(z);
                    Phase2Checks.report("p2-notice-left", had && cleared, "two players held a standing line and an action's notice (entries present " + had + "); after Campaign.left for each, the entries are gone=" + cleared);
                    next(b, 116);
                }
                case 116 -> { // the report of a banner with five troop types (the page's two columns) with its picture
                    var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return;
                    secondReport = reportOf(b, List.of("knight", "archer", "sentinel", "longbow", "mage", "^", "^", "^"), new int[][] {{3, 2, 3, 3}, {3, 1, 2, 3}, {2, 0, 2, 1}, {3, 1, 1, 2}, {3, 12, 3, 1}}, "gear-report-5");
                    next(b, 117);
                }
                case 117 -> {
                    if (!reportDone.contains(secondReport) && since(b) < 600) return;
                    next(b, 118);
                }
                case 118 -> { // a banner of eight types, the widest names the sheet has (Legendaries keep theirs), with its picture: the page at its fullest
                    var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return;
                    thirdReport = reportOf(b, List.of("knight", "archer", "tc_admiral", "wm_oldhorn", "ic_colossus", "longbow", "mage", "sentinel"), new int[][] {{3, 12, 3, 3}, {3, 1, 2, 3}, {3, 1, 3, 3}, {3, 3, 2, 2}, {3, 1, 3, 2}, {2, 0, 2, 1}, {3, 1, 3, 1}, {1, 0, 1, 1}}, "gear-report-8");
                    next(b, 119);
                }
                case 119 -> {
                    if (!reportDone.contains(thirdReport) && since(b) < 600) return;
                    boolean shots = reportDone.contains(secondReport) && reportDone.contains(thirdReport);
                    int rows5 = reportRows.getOrDefault(secondReport, -1), rows8 = reportRows.getOrDefault(thirdReport, -1);
                    Phase2Checks.report("p2-report-levels-pages", pagesOk && shots && rows5 == 5 && rows8 == 8, pagesWhy + "the client's pages held " + rows5 + " and " + rows8 + " levels rows (want 5 and 8) and the pictures gear-report-5 and gear-report-8 were taken=" + shots);
                    next(b, 120);
                }
                case 120 -> { // p2-gear-refusals, p2-gear-forward, p2-gear-tier-cap: a fresh round, the Armoury seeded here so each stands alone
                    var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return;
                    own.banner = new ArrayList<>(List.of("knight", "knight", "knight", "knight", "archer", "archer", "mage", "^"));
                    Veterans.kit(b, own, "knight").highest = 2; Veterans.kit(b, own, "archer").highest = 3; Veterans.kit(b, own, "mage").highest = 3;
                    refusals(b); forward(b, p); tierCap(b);
                    next(b, DONE);
                }
                default -> {}
            }
        }

        /** The finished report's levels block against the Armoury's rows (Veterans.levelRows): true when it holds the same rows in the same order; got collects the rows as drawn. */
        boolean rowsMatch(com.google.gson.JsonObject report, List<MatchStats.LevelRow> want, StringBuilder got) {
            var rows = report == null || !report.has("levels") ? new com.google.gson.JsonArray() : report.getAsJsonArray("levels");
            boolean same = rows.size() == want.size();
            for (int i = 0; i < rows.size(); i++) {
                var r = rows.get(i).getAsJsonObject(); got.append(r).append(' ');
                if (i < want.size()) { var w = want.get(i); same &= r.get("troop").getAsString().equals(w.troop()) && r.get("highestLevel").getAsInt() == w.highest() && r.get("level3Count").getAsInt() == w.level3s() && r.get("weapon").getAsInt() == w.weapon() && r.get("armour").getAsInt() == w.armour(); }
            }
            return same;
        }

        boolean pagesOk = true; String pagesWhy = "";
        /** In the round just started: the army's banner and the Armoury's Kits ({highest level, level 3 soldiers, weapon tier, armour tier} per distinct banner type in order), then the round ends with a report. Returns the report's id, its picture queued under name. */
        String reportOf(GravesAndGlory.Battle b, List<String> banner, int[][] kits, String name) {
            own.banner = new ArrayList<>(banner);
            var types = Veterans.types(own);
            for (int i = 0; i < types.size(); i++) { var k = Veterans.kit(b, own, types.get(i).id()); k.highest = kits[i][0]; k.level3s = kits[i][1]; k.weapon = kits[i][2]; k.armour = kits[i][3]; }
            var want = Veterans.levelRows(b, own);
            b.campaign.finish(own.team);
            var report = b.campaign.results.get(player);
            StringBuilder got = new StringBuilder(); boolean same = rowsMatch(report, want, got) && want.size() == kits.length;
            pagesOk &= same; pagesWhy += name + ": the report's rows " + got.toString().trim() + " (want " + want + ")=" + same + "; ";
            String id = report == null ? "none-" + name : report.get("id").getAsString();
            reportShots.put(id, name);
            return id;
        }

        /** p2-gear-refusals: the refusals that guard the gold and the buyer, each with its exact words and the gold and Kits untouched. */
        void refusals(GravesAndGlory.Battle b) {
            toBarracks(b); String kit0 = kits(b, own);
            own.gold = 39; String poor = say(b, "gear:archer:weapon:2");
            boolean poorOk = poor.equals("Not enough gold") && own.gold == 39 && kits(b, own).equals(kit0);
            own.gold = 40; String exact = say(b, "gear:archer:weapon:2");
            boolean exactOk = exact.equals("Order Archer: Weapon II bought for 40 gold") && own.gold == 0 && Veterans.peek(b, own, "archer").weapon == 2;
            own.gold = 1000; String kit1 = kits(b, own); List<String> wrong = new ArrayList<>();
            String templar = Campaign.troop("sentinel").name() + " is not in your banner";
            String[][] malformed = {{"gear:archer:weapon:4", "No such gear"}, {"gear:archer:weapon:0", "No such gear"}, {"gear:archer:armour:-1", "No such gear"}, {"gear:archer:armour:two", "No such gear"}, {"gear:archer:shield:2", "No such gear"},
                {"gear:nobody:weapon:2", "No such gear"}, {"gear:archer:weapon", "No such gear"}, {"gear:archer:weapon:2:3", "No such gear"}, {"gear:sentinel:weapon:2", templar}};
            for (var m : malformed) { String got = say(b, m[0]); if (!got.equals(m[1])) wrong.add(m[0] + " said \"" + got + "\" (want \"" + m[1] + "\")"); }
            b.running = false; String idle = say(b, "gear:mage:armour:2"); b.running = true;
            Veterans.override = Boolean.FALSE; String off = say(b, "gear:mage:armour:2"); Veterans.override = null;
            own.seated = false; String seatless = say(b, "gear:mage:armour:2"); own.seated = true;
            home(b);
            boolean ok = poorOk && exactOk && wrong.isEmpty() && idle.equals("Gear is bought during a battle") && off.equals("Ranks are off") && seatless.isEmpty() && own.gold == 1000 && kits(b, own).equals(kit1);
            Phase2Checks.report("p2-gear-refusals", ok, "with 39 gold the archers' Weapon II (40) said \"" + poor + "\" (want \"Not enough gold\", gold and Kits unchanged)=" + poorOk + "; with 40 it said \"" + exact + "\" (want bought, 0 left)=" + exactOk
                + "; malformed or foreign: " + (wrong.isEmpty() ? "all nine said what they should" : wrong) + "; the Sunweaver's Armour II (80, affordable and open) with no round running said \"" + idle + "\" (want \"Gear is bought during a battle\"), with levels off \"" + off + "\" (want \"Ranks are off\"), unseated \"" + seatless + "\" (want nothing: the command is dropped); gold " + own.gold + " (want 1000) and Kits " + kits(b, own) + " (want " + kit1 + ")");
        }

        /** p2-gear-forward: with Battle.forwardReinforcement off no point is a place to buy (troops no longer deploy there), the barracks still is. */
        void forward(GravesAndGlory.Battle b, ServerPlayerEntity p) {
            own.gold = 1000; boolean flag = b.forwardReinforcement;
            hold(b, held); toPoint(b, held);
            boolean anchored = b.reinforcementAnchors(0).stream().anyMatch(a -> a.x() == held.spec.x() && a.z() == held.spec.z()), hereOn = Veterans.buyHere(b, p, own);
            boolean hereOff, barracksOff; String atPoint, atBarracks; int goldPoint;
            b.forwardReinforcement = false;
            try {
                hereOff = Veterans.buyHere(b, p, own); atPoint = say(b, "gear:mage:armour:2"); goldPoint = own.gold;
                toBarracks(b); barracksOff = Veterans.buyHere(b, p, own); atBarracks = say(b, "gear:mage:armour:2");
            } finally { b.forwardReinforcement = flag; restoreHeld(); home(b); }
            boolean ok = anchored && hereOn && !hereOff && atPoint.equals("Buy at your barracks or a held point") && goldPoint == 1000 && barracksOff && atBarracks.equals("Sunweaver: Armour II bought for 80 gold") && own.gold == 920;
            Phase2Checks.report("p2-gear-forward", ok, "at a point Dawn holds (a reinforcement anchor " + anchored + ") a commander may buy " + hereOn + " (want true); with forwardReinforcement off he may buy there " + hereOff + " (want false), the buy said \"" + atPoint + "\" (want \"Buy at your barracks or a held point\"), gold " + goldPoint + " (want 1000); at the barracks with it off he may buy " + barracksOff + " and it said \"" + atBarracks + "\", gold " + own.gold + " (want 920)");
        }

        /** p2-gear-tier-cap: a sheet with a fourth gear tier (the sheet's tiers are rows) must not make the buy throw: gear prices exist for tiers II and III, so a higher tier is no such gear. */
        void tierCap(GravesAndGlory.Battle b) {
            var sheet = Spec.GEARTIERS; own.gold = 1000; String why = "", said = "", thrown = "";
            try {
                Spec.GEARTIERS = new Spec.GearTiersRow[] {sheet[0], sheet[1], sheet[2], new Spec.GearTiersRow("t4", 4, 0.3, 0.3)};
                toBarracks(b);
                try { why = Veterans.refusal(b, own, mage, LevelMath.WEAPON, 4, true); said = say(b, "gear:mage:weapon:4"); } catch (RuntimeException e) { thrown = e.toString(); }
            } finally { Spec.GEARTIERS = sheet; home(b); }
            boolean ok = thrown.isEmpty() && why.equals("No such gear") && said.equals("No such gear") && own.gold == 1000;
            Phase2Checks.report("p2-gear-tier-cap", ok, "with a fourth row in the gearTiers sheet, Weapon IV of the Sunweaver: the refusal \"" + why + "\" and the command's notice \"" + said + "\" (want \"No such gear\" both)" + (thrown.isEmpty() ? "" : ", THREW " + thrown) + ", gold " + own.gold + " (want 1000)");
        }
    }

    /**
     * Task 8: the Armoury tab (in-match progression design 7 and 9.1; War Table port plan Task 8). The fixture is the gear suite's (Gear): a stone platform away from
     * every buy place, the commander's army with a banner of five troop types (an Order Squire, an Order Archer, a Templar, an Order Longbow and a Sunweaver) and Armoury
     * kits that put every buy state on show with the gold held at POT: a Squire with Weapon II bought and Armour II open, an archer with level 2 seen, a Templar with
     * both weapon tiers and Armour II bought, a Longbow nobody has levelled and a Sunweaver short of gold. The commander is moved to his barracks or back to the platform by the
     * server tick, as a buy needs. The client half opens the real screen (the lectern's BarracksScreen and its tab, or the Armoury key), reads the page's own buttons and
     * clicks them through the screen's mouse handlers, so a press is what a player's is. Every expectation comes from the server's own state and refusals (Veterans), LevelMath and
     * the sheet, never from the page's copy; the few words the plan names ("Bought", "Buy", "Not here") are the one thing the page owns.
     */
    static final class Armoury extends Gear {
        static final List<String> BANNER5 = List.of("knight", "archer", "sentinel", "longbow", "mage", "^", "^", "^");
        static final List<String> BANNER8 = List.of("knight", "archer", "sentinel", "longbow", "mage", "tc_admiral", "wm_oldhorn", "ic_colossus");
        /** The gold the fixture holds: enough for a common tier II, short of a common tier III and of an epic tier II (from the sheet's prices; the fixture reports itself bad when that fails). */
        static final int POT = LevelMath.price("common", 3) - 10;
        /** The five sizes of the plan's text-fit rule (the p1-ui-*-scales sizes): window width, height and GUI scale. */
        static final int[][] SIZES = {{1280, 720, 3}, {854, 480, 2}, {1920, 1080, 2}, {1920, 974, 1}, {1920, 1080, 1}, {2560, 1440, 0}}; // the last at the automatic GUI Scale: the War Table's 3x (Kort, 2026-10-31)
        static final String UNBOUND_LINE = "The Armoury key is not bound: set it in Controls";

        /** What the server holds after its last tick: the Armoury it sends, the refusal of every buy as it words it for a commander at a buy place, the refusal of an open buy as the commander stands, the Squire's bought tiers and the gold. */
        record Snap(LevelMath.Armoury armoury, Map<String, String> refusals, String away, int knightWeapon, int knightArmour, int gold, int earned) {}
        volatile Snap snap; volatile int goldPin = -1; volatile String place = "home"; volatile boolean fixtureOk;
        String placed = "";
        // the client's phase machine and what the phases share
        int phase, pt, earnedBefore, sync8 = -1, field8 = -1, oldW, oldH, oldGui, sizeAt, openFill = -1, goldBefore, packets0, packets, boughtAt = -1;
        String lastSentBefore = "", clickState = "", sentCmd = "", fieldBad = "", fieldInfo = "", tabsFirst = "", scrollBad = "", scrollInfo = "";
        final List<String> fitNotes = new ArrayList<>(), fitFails = new ArrayList<>(), keyNotes = new ArrayList<>(), refusalNotes = new ArrayList<>();
        boolean refusalsOk = true, keyOk = true, tabsToggled, tabsClosed, staleKeyOpened;

        // ---- the server half ----

        @Override public void serverTick(GravesAndGlory.Battle b) {
            if (player == null || stage >= DONE) return;
            try { server(b); } catch (RuntimeException e) { Phase2Checks.report("p2-armoury-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, DONE); }
        }

        void server(GravesAndGlory.Battle b) {
            if (stage >= 10) pin(b);
            switch (stage) {
                case 1 -> { var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return; next(b, 2); }
                case 2 -> {
                    if (since(b) < 10) return;
                    stageFixture(b); b.campaign.introUntil = b.time; Veterans.override = null; Houses.forceRise = Boolean.FALSE;
                    knight = Campaign.troop("knight"); archer = Campaign.troop("archer"); mage = Campaign.troop("mage");
                    bannerBefore = own.banner; own.banner = new ArrayList<>(BANNER5);
                    kits(b); place = "home"; placed = ""; home(b);
                    var f = Spec.FACTIONS[0]; double reach = Spec.ECONOMY[0].accessRadius();
                    double fromBarracks = distance(sp(b).getPos(), new Vec3d(f.barracksX(), 0, f.barracksZ()));
                    boolean prices = POT >= LevelMath.price("common", 2) && POT < LevelMath.price("common", 3) && POT < LevelMath.price("epic", 2) && POT < LevelMath.price("rare", 3);
                    boolean ok = prices && Veterans.types(own).size() == 5 && fromBarracks > reach && !Veterans.buyHere(b, sp(b), own);
                    Phase2Checks.report("p2-armoury-fixture", ok, "the army's banner has " + Veterans.types(own).stream().map(Spec.TroopsRow::id).toList() + " (want five types), the gold held at " + POT + " (a common tier II at " + LevelMath.price("common", 2) + " is within it; a common tier III at " + LevelMath.price("common", 3) + ", an epic tier II at " + LevelMath.price("epic", 2) + " and a rare tier III at " + LevelMath.price("rare", 3) + " are beyond it: " + prices + "), the commander on the platform " + fmt(fromBarracks) + " blocks from his barracks (want more than " + fmt(reach) + ")");
                    fixtureOk = ok; next(b, ok ? 10 : DONE);
                }
                default -> {}
            }
        }

        /** The Armoury kits that put every buy state on show with POT gold (see the class comment); nothing for the Longbow. */
        void kits(GravesAndGlory.Battle b) {
            b.armouries.remove(own);
            var k = Veterans.kit(b, own, "knight"); k.highest = 3; k.weapon = 2; k.armour = 1;
            k = Veterans.kit(b, own, "archer"); k.highest = 2;
            k = Veterans.kit(b, own, "sentinel"); k.highest = 3; k.weapon = 3; k.armour = 2;
            k = Veterans.kit(b, own, "mage"); k.highest = 2;
        }

        /** Each server tick after the fixture: the gold held (while pinned), the commander where the phase wants him, and the snapshot the client half compares with. */
        void pin(GravesAndGlory.Battle b) {
            if (goldPin >= 0) own.gold = goldPin;
            if (!place.equals(placed) || b.time % 20 == 0) { placed = place; if (place.equals("barracks")) toBarracks(b); else home(b); }
            hold();
            var p = sp(b); if (p == null) return;
            Map<String, String> refusals = new HashMap<>();
            for (var t : Veterans.types(own)) for (int col = 0; col < 4; col++) refusals.put(t.id() + ":" + ArmouryTab.TRACKS[col] + ":" + ArmouryTab.TIERS[col], Veterans.refusal(b, own, t, ArmouryTab.TRACKS[col], ArmouryTab.TIERS[col], true));
            var sq = Veterans.peek(b, own, "knight");
            snap = new Snap(Veterans.armouryState(b, own, p), refusals, Veterans.refusal(b, own, knight, LevelMath.ARMOUR, 2, Veterans.buyHere(b, p, own)), sq == null ? 1 : sq.weapon, sq == null ? 1 : sq.armour, own.gold, own.stats.goldEarned);
        }

        // ---- the client half ----

        @Override public void step(MinecraftClient c, int tick) {
            if (tick == 20) { c.setScreen(null); UUID id = c.player.getUuid(); Phase2Checks.server(c, b -> { player = id; next(b, 1); }); }
            if (wantShot != null && !shotTaken && ++shotDelay >= shotAfter) { Phase2Checks.shot(c, wantShot); shotTaken = true; }
            if (stage >= 10 && stage < DONE && !finished && phase < 99) {
                try { phases(c); } catch (RuntimeException e) { Phase2Checks.report("p2-armoury-exception", false, "phase " + phase + " tick " + pt + ": " + e); e.printStackTrace(); end(c); }
            }
            if (stage == DONE && !finished) { finished = true; c.setScreen(null); Phase2Checks.finish(c, "armoury"); }
            if (tick > 14000 && !finished) { finished = true; Phase2Checks.report("p2-armoury-timeout", false, "stuck in phase " + phase + " tick " + pt + " (stage " + stage + ")"); Phase2Checks.finish(c, "armoury"); }
        }

        @Override public void cleanup(MinecraftClient c) {
            Veterans.override = null; goldPin = -1;
            try { restoreKeys(c); } catch (RuntimeException ignored) {}
            try { c.options.setKeyCode(GravesAndGloryClient.armoury, GravesAndGloryClient.armoury.getDefaultKey()); KeyBinding.updateKeysByCode(); } catch (RuntimeException ignored) {}
            if (oldW > 0) Phase1Checks.window(c, oldW, oldH, oldGui);
            super.cleanup(c);
        }

        void go(int p) { phase = p; pt = 0; }
        void end(MinecraftClient c) { phase = 99; Phase2Checks.server(c, b -> next(b, DONE)); }
        void bail(MinecraftClient c, String id, String why) { Phase2Checks.report(id, false, why); end(c); }
        static RecruitmentScreen rs(MinecraftClient c) { return c.currentScreen instanceof RecruitmentScreen r ? r : null; }
        String seen() { return "the client's Armoury " + CampaignClient.armoury + " and gold " + GravesAndGloryClient.gold + " against the server's " + snap; }
        /** Whether the client holds what the server holds, and the commander stands (or not) where he may buy. */
        boolean synced(boolean here) { var s = snap; return s != null && CampaignClient.armoury.equals(s.armoury()) && CampaignClient.armoury.here() == here && GravesAndGloryClient.gold == s.gold(); }
        /** A click through the screen's own mouse handlers, at the centre of the widget (design pixels times the screen's scale, as the game hands the screen). */
        static void click(RecruitmentScreen s, ButtonWidget b) {
            double x = (b.getX() + b.getWidth() / 2.0) * s.uiScale, y = (b.getY() + b.getHeight() / 2.0) * s.uiScale;
            s.mouseClicked(x, y, 0); s.mouseReleased(x, y, 0);
        }
        /** The pointer at the window's bottom-left corner, off every widget, as a player leaves it for a picture. */
        static void park(MinecraftClient c) {
            try {
                var fx = net.minecraft.client.Mouse.class.getDeclaredField("x"); fx.setAccessible(true); fx.setDouble(c.mouse, 2);
                var fy = net.minecraft.client.Mouse.class.getDeclaredField("y"); fy.setAccessible(true); fy.setDouble(c.mouse, c.getWindow().getHeight() - 2);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }
        /** The key as the game would hand it to the binding: a press the next client tick reads. Only with no screen open (the game never counts a press a screen took). */
        static void press(KeyBinding k) { KeyBinding.onKeyPressed(KeyBindingHelper.getBoundKeyOf(k)); }
        static LevelMath.Buy stateAt(LevelMath.ArmouryRow r, int col) { return col == 0 ? r.weapon2() : col == 1 ? r.weapon3() : col == 2 ? r.armour2() : r.armour3(); }
        static String key(ArmouryTab.Gear g) { return g.troop.id() + ":" + g.track + ":" + g.tier; }
        /** The words a button must show for a server state, worked out from the sheet, LevelMath and the server's own refusal; null for a locked button (its text is the server's refusal). */
        static List<String> expect(Spec.TroopsRow t, int tier, LevelMath.Buy state, boolean here, String refusal) {
            int price = LevelMath.price(t.rarity(), tier);
            return switch (state) {
                case BOUGHT -> List.of("Bought");
                case OPEN -> here ? List.of("Buy", price + " gold") : List.of(price + " gold", "Not here");
                case UNAFFORDABLE -> List.of(price + " gold", here ? refusal : "Not here");
                case LOCKED -> null;
            };
        }

        void phases(MinecraftClient c) {
            pt++;
            var s = rs(c);
            switch (phase) {
                case 0 -> {
                    if (!fixtureOk) return;
                    oldW = c.getWindow().getWidth(); oldH = c.getWindow().getHeight(); oldGui = c.options.getGuiScale().getValue();
                    go(1);
                }
                case 1 -> { // the kits seeded, the commander at his barracks with POT gold, the window at 960x540 GUI 1 (canvas 960x540)
                    if (pt == 1) { goldPin = POT; place = "barracks"; c.setScreen(null); Phase1Checks.window(c, 960, 540, 1); Phase2Checks.server(c, b -> kits(b)); }
                    if (pt > 20 && synced(true)) go(2); else if (pt > 600) bail(c, "p2-armoury-tab", "the client never held the server's Armoury at the barracks: " + seen());
                }
                case 2 -> { // the barracks opens as the lectern opens it, and the player clicks the Armoury tab
                    if (pt == 1) c.setScreen(new GravesAndGloryClient.BarracksScreen());
                    if (pt == 8) {
                        if (s == null || s.tab != 0 || s.armouryTab == null) { bail(c, "p2-armoury-tab", "the barracks did not open on its own page with an Armoury tab: " + c.currentScreen); return; }
                        click(s, s.armouryTab);
                    }
                    if (pt == 18) { if (s == null) { bail(c, "p2-armoury-tab", "the screen closed"); return; } evalTab(s); park(c); }
                    if (pt == 24) Phase2Checks.shot(c, "armoury-tab");
                    if (pt == 28) go(3);
                }
                case 3 -> { // the platform, away from every buy place
                    if (pt == 1) { place = "home"; c.setScreen(null); }
                    if (pt > 6 && synced(false)) go(4); else if (pt > 600) bail(c, "p2-armoury-field", "the client never held the server's Armoury away from the barracks: " + seen());
                }
                case 4 -> { // the Armoury key opens the tab in the field; a press on an open button sends nothing
                    if (pt == 1) { lastSentBefore = GravesAndGloryClient.lastSent; press(GravesAndGloryClient.armoury); }
                    if (pt == 10) {
                        if (s == null || s.tab != 1) { bail(c, "p2-armoury-field", "the key did not open the Armoury tab: the screen is " + (s == null ? c.currentScreen : "the barracks page " + s.tab)); return; }
                        evalField(s);
                        GravesAndGloryClient.lastSent = ""; click(s, s.page.buttons.get(2)); // the Squire's Armour II: open at the barracks, greyed here
                    }
                    if (pt == 16) { evalFieldPress(s); park(c); }
                    if (pt == 20 && s != null) s.setFocused(null); // the picture shows the page as a player leaves it, with no button picked out
                    if (pt == 22) Phase2Checks.shot(c, "armoury-field");
                    if (pt == 26) go(5);
                }
                case 5 -> { // back at the barracks, the tab still open
                    if (pt == 1) { place = "barracks"; goldPin = POT; }
                    if (pt > 6 && synced(true)) go(6); else if (pt > 600) bail(c, "p2-armoury-buy-click", "the client never held the server's Armoury at the barracks again: " + seen());
                }
                case 6 -> { // a click on the open Squire Armour II buys it; the page shows the server's answer within two state packets; presses that cannot buy answer with the reason
                    if (s == null || s.tab != 1) { bail(c, "p2-armoury-buy-click", "the tab closed before the click"); return; }
                    if (pt == 1) goldPin = -1; // free the gold: the match may earn some before the buy, and the server's own tally of earnings says how much
                    if (pt == 3) { goldBefore = snap.gold(); earnedBefore = snap.earned(); } // a snapshot of a tick after the release: gold and earnings move together from here
                    if (pt == 4) {
                        var g = s.page.buttons.get(2); g.refresh(); clickState = g.state + " pressable " + g.pressable;
                        packets0 = CampaignClient.acceptedStates; GravesAndGloryClient.lastSent = ""; click(s, g); sentCmd = GravesAndGloryClient.lastSent; boughtAt = -1;
                    }
                    if (pt > 4 && boughtAt < 0) { var g = s.page.buttons.get(2); g.refresh(); if (g.state == LevelMath.Buy.BOUGHT) { boughtAt = pt; packets = CampaignClient.acceptedStates - packets0; } }
                    if (boughtAt > 0 && pt == boughtAt + 8) { evalBuy(s); goldPin = snap.gold(); GravesAndGloryClient.lastSent = ""; click(s, s.page.buttons.get(2)); } // the bought button again
                    if (boughtAt > 0 && pt == boughtAt + 14) { answer(s, "a bought button", "bought", true); GravesAndGloryClient.lastSent = ""; click(s, s.page.buttons.get(12)); } // the Longbow's Weapon II: locked
                    if (boughtAt > 0 && pt == boughtAt + 20) { answer(s, "a locked button", snap.refusals().get("longbow:weapon:2"), false); GravesAndGloryClient.lastSent = ""; click(s, s.page.buttons.get(16)); } // the Sunweaver's Weapon II: short of gold
                    if (boughtAt > 0 && pt == boughtAt + 26) { answer(s, "an unaffordable button", snap.refusals().get("mage:weapon:2"), false); evalRefusals(); go(7); }
                    if (pt > 400 && boughtAt < 0) bail(c, "p2-armoury-buy-click", "the Squire's Armour II never read bought after the click (sent \"" + sentCmd + "\", the button was " + clickState + "): " + seen());
                }
                case 7 -> { // the key's real name, a rebound key, an unbound key
                    if (pt == 1) { c.setScreen(null); goldPin = POT; Phase2Checks.server(c, b -> kits(b)); }
                    if (pt == 8) { keyDefault(c); press(GravesAndGloryClient.armoury); }
                    if (pt == 14) { keyScreen(c, "the default key"); rebind(c, InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_J)); c.setScreen(null); }
                    if (pt == 17) { KeyBinding.onKeyPressed(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_U)); }
                    if (pt == 19) { staleKeyOpened = c.currentScreen != null; }
                    if (pt == 20) press(GravesAndGloryClient.armoury);
                    if (pt == 26) { keyScreen(c, "the key J"); freeKey(c, InputUtil.Type.MOUSE.createFromCode(2)); rebind(c, InputUtil.Type.MOUSE.createFromCode(2)); c.setScreen(null); }
                    if (pt == 32) press(GravesAndGloryClient.armoury);
                    if (pt == 38) { keyScreen(c, "the middle mouse button"); restoreKeys(c); rebind(c, InputUtil.UNKNOWN_KEY); c.setScreen(null); }
                    if (pt == 44) c.setScreen(new GravesAndGloryClient.BarracksScreen());
                    if (pt == 50 && s != null) click(s, s.armouryTab);
                    if (pt == 56) { keyScreen(c, "no key"); rebind(c, InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_U)); c.setScreen(null); }
                    if (pt == 62) { keyReport(); go(8); }
                }
                case 8 -> { // 854x480 GUI 2 (canvas 640x360): the Armoury at the barracks, then in the field; each waits until the client holds what the server holds
                    if (pt == 1) { c.setScreen(null); place = "barracks"; Phase1Checks.window(c, 854, 480, 2); sync8 = -1; field8 = -1; }
                    if (sync8 < 0) {
                        if (pt >= 30) { if (synced(true)) { sync8 = pt; c.setScreen(new RecruitmentScreen(1)); } else if (pt > 600) { bail(c, "p2-armoury-fits", "no Armoury at 854x480: " + seen()); } }
                        return;
                    }
                    int q = pt - sync8;
                    if (q == 10) park(c);
                    if (q == 14) Phase2Checks.shot(c, "armoury-tab-854");
                    if (q == 16) { if (s != null) evalFit(c, s, "854x480 GUI 2, at the barracks"); place = "home"; }
                    if (q > 20 && field8 < 0) {
                        if (synced(false)) field8 = q; else if (q > 600) { bail(c, "p2-armoury-fits", "no field Armoury at 854x480: " + seen()); }
                        return;
                    }
                    if (field8 < 0) return;
                    int f = q - field8;
                    if (f == 10) park(c);
                    if (f == 14) Phase2Checks.shot(c, "armoury-field-854");
                    if (f == 16) { if (s != null) evalFit(c, s, "854x480 GUI 2, in the field"); go(9); }
                }
                case 9 -> { // levels off: the tab says so
                    if (pt == 1) Veterans.override = Boolean.FALSE;
                    if (pt == 40) { park(c); evalOff(s); }
                    if (pt == 44) Phase2Checks.shot(c, "armoury-off-854");
                    if (pt == 46) { Veterans.override = null; place = "barracks"; Phase2Checks.server(c, b -> { own.banner = new ArrayList<>(BANNER8); kits(b); }); }
                    if (pt > 60 && CampaignClient.armoury.rows().size() == 8 && s != null && s.page.total == 8) go(10); else if (pt > 600) bail(c, "p2-armoury-scroll", "the eight-type banner never reached the client: " + seen());
                }
                case 10 -> { // eight troop types at 854x480: the rows that fit, and the rest by the arrows, the wheel and Page Up and Down
                    if (pt == 4) { if (s == null) { bail(c, "p2-armoury-scroll", "the screen closed"); return; } scrollSteps(s); }
                    if (pt == 20) park(c);
                    if (pt == 24) Phase2Checks.shot(c, "armoury-scroll-854");
                    if (pt == 26) { scrollReport(s); go(11); }
                }
                case 11 -> { // the five sizes, the banner at its widest
                    if (pt == 1) { var z = SIZES[sizeAt]; Phase1Checks.window(c, z[0], z[1], z[2], z[0] >= 1920); }
                    if (pt == 40) { park(c); String label = SIZES[sizeAt][0] + "x" + SIZES[sizeAt][1] + " GUI " + SIZES[sizeAt][2]; if (s != null && s.tab == 1) evalFit(c, s, label); else fitFails.add("no Armoury tab at " + label); }
                    if (pt == 44 && sizeAt == 2) Phase2Checks.shot(c, "armoury-tab-1920");
                    if (pt == 44 && sizeAt == 5) Phase2Checks.shot(c, "armoury-tab-1440");
                    if (pt == 46) { if (++sizeAt < SIZES.length) go(11); else { fitReport(); go(12); } }
                }
                case 12 -> { // the two pages as tabs of one screen, and the key as a toggle
                    if (pt == 1) { Phase1Checks.window(c, 960, 540, 1); place = "barracks"; Phase2Checks.server(c, b -> { own.banner = new ArrayList<>(BANNER5); kits(b); }); }
                    if (pt == 40) { if (s == null || s.tab != 1) { bail(c, "p2-armoury-tabs", "no Armoury tab open: " + c.currentScreen); return; } tabsFirst = backAt(s); click(s, s.barracksTab); }
                    if (pt == 46) { if (s != null) evalTabs(s); }
                    if (pt == 52) { if (s != null) s.keyPressed(KeyBindingHelper.getBoundKeyOf(GravesAndGloryClient.armoury).getCode(), 0, 0); }
                    if (pt == 56) { tabsToggled = s != null && s.tab == 1 && rs(c) == s; if (s != null) s.keyPressed(KeyBindingHelper.getBoundKeyOf(GravesAndGloryClient.armoury).getCode(), 0, 0); }
                    if (pt == 60) { tabsClosed = c.currentScreen == null; tabsReport(); end(c); }
                }
                default -> {}
            }
        }

        // ---- the checks ----

        /** p2-armoury-tab and p2-armoury-states: the page after a click on the Armoury tab, at the barracks, against the server's rows and refusals. */
        void evalTab(RecruitmentScreen s) {
            var sn = snap; var a = s.page; var rows = sn.armoury().rows(); StringBuilder bad = new StringBuilder(), words = new StringBuilder(), bad2 = new StringBuilder();
            if (s.tab != 1) bad.append("the tab is " + s.tab + " after the click; ");
            if (!s.armouryTab.getMessage().getString().startsWith("> ") || s.barracksTab.getMessage().getString().startsWith("> ")) bad.append("the open tab is not the one marked; ");
            if (a.total != rows.size() || a.shown != rows.size() || a.scrolling) bad.append("the page shows " + a.shown + " of " + a.total + " rows (want all " + rows.size() + ", no scroll); ");
            if (a.buttons.size() != 4 * rows.size()) bad.append(a.buttons.size() + " buttons (want " + 4 * rows.size() + "); ");
            String names = String.join(" ", a.nameDrawn);
            for (var r : rows) { var t = Campaign.troop(r.troop()); if (!names.contains(t.name())) bad.append("the name " + t.name() + " is not drawn (" + names + "); "); }
            Set<LevelMath.Buy> seen = new HashSet<>(); Map<LevelMath.Buy, Integer> count = new java.util.EnumMap<>(LevelMath.Buy.class);
            for (int i = 0; i < a.buttons.size() && i / 4 < rows.size(); i++) {
                var g = a.buttons.get(i); g.refresh(); var row = rows.get(i / 4); int col = i % 4; var want = stateAt(row, col); seen.add(want); count.merge(want, 1, Integer::sum);
                String who = key(g);
                if (!g.troop.id().equals(row.troop()) || !g.track.equals(ArmouryTab.TRACKS[col]) || g.tier != ArmouryTab.TIERS[col]) bad.append(who + " is in the place of " + row.troop() + " column " + col + "; ");
                if (g.state != want) bad.append(who + " reads " + g.state + ", the server's row says " + want + "; ");
                String refusal = sn.refusals().get(who), text = String.join(" ", g.lines);
                var exp = expect(g.troop, g.tier, want, true, refusal);
                if (want == LevelMath.Buy.LOCKED) {
                    boolean second = g.tier == 3 && (stateAt(row, col - 1) == LevelMath.Buy.OPEN || stateAt(row, col - 1) == LevelMath.Buy.UNAFFORDABLE) && text.equals("Buy " + (g.track.equals(LevelMath.ARMOUR) ? "Armour" : "Weapon") + " II first");
                    if (!(text.equals(refusal) || second)) bad2.append(who + " says \"" + text + "\", the server's own refusal is \"" + refusal + "\"; ");
                } else if (!g.lines.equals(exp)) bad2.append(who + " says " + g.lines + ", want " + exp + "; ");
                if ((want == LevelMath.Buy.OPEN) != g.pressable) bad2.append(who + " " + want + " but pressable " + g.pressable + "; ");
                if (g.hasGlyph() != (want == LevelMath.Buy.LOCKED || want == LevelMath.Buy.BOUGHT)) bad2.append(who + " glyph " + g.hasGlyph() + " for " + want + "; ");
                double c1 = UiMath.contrast(g.mainUsed, g.fillUsed), c2 = g.lines.size() > 1 ? UiMath.contrast(g.secondUsed, g.fillUsed) : 21;
                if (c1 < 4.5 || c2 < 4.5) bad2.append(who + " text contrast " + fmt(c1) + " and " + fmt(c2) + " (want 4.5); ");
                if (want == LevelMath.Buy.OPEN && openFill < 0) openFill = g.fillUsed;
                if (g.drawnLines == 0) bad2.append(who + " was never drawn; ");
                if (i < 8) words.append(who).append("=").append(want).append(g.lines).append(" ");
            }
            double focusContrast = UiMath.contrast(DarkUi.INK_TEXT, ArmouryTab.OPEN_FOCUS); // the hovered tag keeps its ink and gains an underline
            if (focusContrast < 4.5) bad2.append("the hovered buy has contrast " + fmt(focusContrast) + "; ");
            Phase2Checks.report("p2-armoury-tab", bad.length() == 0, "the Armoury tab opened by a click on the barracks' tab: " + a.shown + " rows for the banner " + rows.stream().map(LevelMath.ArmouryRow::troop).toList() + ", " + a.buttons.size() + " buttons each in the state of the server's row, the names drawn " + a.nameDrawn + "; the screen " + s.width + "x" + s.height + (bad.length() == 0 ? " (all as the server reports)" : "; WRONG: " + bad));
            Phase2Checks.report("p2-armoury-states", seen.size() == 4 && bad2.length() == 0, "the fixture shows " + count + " (want all four states); the words as drawn, from the first two rows: " + words.toString().trim() + (bad2.length() == 0 ? "; every label is the sheet's price, \"Bought\", or the server's own refusal, every open buy is pressable and no other, every locked or bought button carries its glyph, text contrast at least 4.5" : "; WRONG: " + bad2));
        }

        /** p2-armoury-field, first half: the tab opened by the key with the commander on the platform; every button greyed (or locked, or bought), the footer the server's reason. */
        void evalField(RecruitmentScreen s) {
            var sn = snap; var a = s.page; var rows = sn.armoury().rows(); StringBuilder bad = new StringBuilder(); int greyed = 0;
            if (sn.armoury().here()) bad.append("the server says he may buy here; ");
            if (a.buttons.size() != 4 * rows.size()) bad.append(a.buttons.size() + " buttons; ");
            for (int i = 0; i < a.buttons.size() && i / 4 < rows.size(); i++) {
                var g = a.buttons.get(i); g.refresh(); var st = stateAt(rows.get(i / 4), i % 4);
                if (g.pressable) bad.append(key(g) + " can be pressed away from a buy place; ");
                if (st == LevelMath.Buy.OPEN || st == LevelMath.Buy.UNAFFORDABLE) {
                    greyed++; var exp = expect(g.troop, g.tier, st, false, "");
                    if (!g.lines.equals(exp)) bad.append(key(g) + " says " + g.lines + ", want " + exp + "; ");
                    if (g.fillUsed == openFill) bad.append(key(g) + " is drawn as a buy that is ready; ");
                }
            }
            if (greyed == 0) bad.append("the fixture has no open or unaffordable buy to grey; ");
            String footer = String.join(" ", a.footerDrawn);
            if (sn.away().isEmpty() || !footer.equals(sn.away())) bad.append("the footer says \"" + footer + "\", the server's refusal of an open buy here is \"" + sn.away() + "\"; ");
            fieldBad = bad.toString(); fieldInfo = greyed + " open or short-of-gold buttons greyed, the footer \"" + footer + "\" (the server's own refusal \"" + sn.away() + "\")";
        }

        /** p2-armoury-field, second half: a press on the greyed Squire Armour II sent nothing, bought nothing and kept the reason on the page. */
        void evalFieldPress(RecruitmentScreen s) {
            var sn = snap; String sent = GravesAndGloryClient.lastSent, footer = s == null ? "" : String.join(" ", s.page.footerDrawn);
            boolean none = sent.isEmpty() && sn.knightArmour() == 1 && sn.gold() == POT && footer.equals(sn.away());
            Phase2Checks.report("p2-armoury-field", fieldBad.isEmpty() && none, "the Armoury key opened the tab on the platform: " + fieldInfo + (fieldBad.isEmpty() ? "" : "; WRONG: " + fieldBad) + "; a press on the greyed Squire Armour II sent \"" + sent + "\" (want nothing), the server's Squire armour tier " + sn.knightArmour() + " (want 1) and gold " + sn.gold() + " (want " + POT + "), the footer \"" + footer + "\"; the key was pressed with the last sent action \"" + lastSentBefore + "\"");
        }

        /** p2-armoury-buy-click: the click at the barracks sent the command, the server bought it, and the page read it within two state packets, with the server's notice in the footer. */
        void evalBuy(RecruitmentScreen s) {
            var sn = snap; var t = Campaign.troop("knight"); int price = LevelMath.price(t.rarity(), 2);
            String wantNotice = t.name() + ": Armour " + MatchStats.roman(2) + " bought for " + price + " gold", footer = String.join(" ", s.page.footerDrawn);
            var a3 = s.page.buttons.get(3); a3.refresh(); var a3Server = sn.armoury().rows().get(0).armour3();
            boolean ok = sentCmd.equals("gear:knight:armour:2") && clickState.equals("OPEN pressable true") && packets >= 0 && packets <= 2 && sn.knightArmour() == 2 && sn.gold() == goldBefore - price + (sn.earned() - earnedBefore)
                && footer.equals(wantNotice) && a3.state == a3Server && a3.state != LevelMath.Buy.LOCKED;
            Phase2Checks.report("p2-armoury-buy-click", ok, "a click at the barracks on the " + clickState + " Squire Armour II sent \"" + sentCmd + "\" (want gear:knight:armour:2); the server's Squire armour tier is " + sn.knightArmour() + " (want 2), gold " + goldBefore + " -> " + sn.gold() + " (want -" + price + " and the " + (sn.earned() - earnedBefore) + " the match earned meanwhile); the page read it bought " + packets + " state packets after the click, " + boughtAt + " ticks (want 2 packets at most); the footer \"" + footer + "\" (want \"" + wantNotice + "\"); Armour III went from locked to " + a3.state + " (the server's row says " + a3Server + ")");
        }

        /** What a press that cannot buy answered: nothing sent, the reason in the footer. */
        void answer(RecruitmentScreen s, String what, String want, boolean contains) {
            String footer = String.join(" ", s.page.footerDrawn), sent = GravesAndGloryClient.lastSent;
            boolean ok = sent.isEmpty() && want != null && !want.isEmpty() && (contains ? footer.contains(want) : footer.equals(want));
            refusalsOk &= ok;
            refusalNotes.add(what + " sent \"" + sent + "\" and the footer said \"" + footer + "\" (want " + (contains ? "a line with" : "exactly") + " \"" + want + "\")=" + ok);
        }
        void evalRefusals() { Phase2Checks.report("p2-armoury-refusals", refusalsOk && refusalNotes.size() == 3, "presses that cannot buy: " + String.join("; ", refusalNotes)); }

        void keyDefault(MinecraftClient c) {
            var k = GravesAndGloryClient.armoury; var def = k.getDefaultKey();
            boolean u = def.getCategory() == InputUtil.Type.KEYSYM && def.getCode() == GLFW.GLFW_KEY_U;
            List<String> mod = new ArrayList<>(), other = new ArrayList<>(); int mine = 0;
            for (var b : c.options.allKeys) { if (b.getTranslationKey().startsWith("key.gravesandglory.")) mine++; if (b == k || !b.getDefaultKey().equals(def)) continue; (b.getTranslationKey().startsWith("key.gravesandglory.") ? mod : other).add(b.getTranslationKey()); }
            boolean lang = I18n.hasTranslation("key.gravesandglory.armoury") && !I18n.translate("key.gravesandglory.armoury").equals("key.gravesandglory.armoury");
            String hint = GravesAndGloryClient.hint("armoury");
            boolean ok = u && k.getTranslationKey().equals("key.gravesandglory.armoury") && mod.isEmpty() && lang && hint.equals(k.getBoundKeyLocalizedText().getString());
            keyOk &= ok;
            keyNotes.add("key.gravesandglory.armoury defaults to " + def.getTranslationKey() + " (U=" + u + "); of " + mine + " mod bindings none else defaults to it=" + mod.isEmpty() + (mod.isEmpty() ? "" : " " + mod) + (other.isEmpty() ? "" : " (vanilla or other mods on it: " + other + ")") + "; its Controls name is translated=" + lang + "; hint(armoury)=\"" + hint + "\"");
        }
        /** The bindings freeKey took off a key, to give back. */
        final List<Map.Entry<KeyBinding, InputUtil.Key>> freed = new ArrayList<>();
        /** Minecraft hands a press of a key shared by two bindings to only one of them (KeyBinding.KEY_TO_BINDINGS holds the last registered), and which one depends on the registry's order: the vanilla Pick Block shares the middle mouse button by default.
         *  A player who binds the Armoury there sees the red conflict in Controls and frees it; this does the same, so the check tests the Armoury's own handling of the button. */
        void freeKey(MinecraftClient c, InputUtil.Key key) {
            for (var other : c.options.allKeys) if (other != GravesAndGloryClient.armoury && KeyBindingHelper.getBoundKeyOf(other).equals(key)) { freed.add(Map.entry(other, key)); c.options.setKeyCode(other, InputUtil.UNKNOWN_KEY); }
            KeyBinding.updateKeysByCode();
        }
        void restoreKeys(MinecraftClient c) { for (var e : freed) c.options.setKeyCode(e.getKey(), e.getValue()); freed.clear(); KeyBinding.updateKeysByCode(); }
        /** The translation keys of every other binding on this key. */
        static List<String> sharing(MinecraftClient c, InputUtil.Key key) {
            List<String> out = new ArrayList<>();
            for (var other : c.options.allKeys) if (other != GravesAndGloryClient.armoury && KeyBindingHelper.getBoundKeyOf(other).equals(key)) out.add(other.getTranslationKey());
            return out;
        }
        void rebind(MinecraftClient c, InputUtil.Key key) { c.options.setKeyCode(GravesAndGloryClient.armoury, key); KeyBinding.updateKeysByCode(); }
        /** The key line on the open tab: the player's own key for the Armoury as the hints name it, or how to bind one. */
        void keyScreen(MinecraftClient c, String label) {
            var s = rs(c); var k = GravesAndGloryClient.armoury;
            if (s == null || s.tab != 1) { keyOk = false; keyNotes.add(label + ": the Armoury tab is not open (" + c.currentScreen + "); other bindings on its key: " + sharing(c, KeyBindingHelper.getBoundKeyOf(k))); return; }
            var bound = KeyBindingHelper.getBoundKeyOf(k); String hint = GravesAndGloryClient.hint("armoury"), line = s.page.hintDrawn;
            String name = bound.getCategory() == InputUtil.Type.MOUSE ? HintText.mouseChip(bound.getCode()) : k.getBoundKeyLocalizedText().getString();
            String want = k.isUnbound() ? UNBOUND_LINE : name + " opens the Armoury anywhere in battle";
            boolean ok = line.equals(want) && (k.isUnbound() || hint.equals(name)) && !name.isEmpty() && s.page.fitFailures().isEmpty();
            keyOk &= ok;
            keyNotes.add(label + ": the footer reads \"" + line + "\" (want \"" + want + "\"), hint(armoury) \"" + hint + "\"" + (s.page.fitFailures().isEmpty() ? "" : ", the page does not fit " + s.page.fitFailures()) + "=" + ok);
        }
        void keyReport() {
            keyOk &= !staleKeyOpened && keyNotes.size() == 5;
            Phase2Checks.report("p2-armoury-key-hint", keyOk, String.join("; ", keyNotes) + "; the old key U after the rebind to J opened a screen=" + staleKeyOpened + " (want false)");
        }

        /** The page at a canvas: every button inside it and at least 20 x 20, every line of text whole in its box (the words, the names, the footer), nothing overlapping. */
        void evalFit(MinecraftClient c, RecruitmentScreen s, String label) {
            var a = s.page; List<String> bad = new ArrayList<>(a.fitFailures());
            int cw = (int) (c.getWindow().getScaledWidth() / s.uiScale), ch = (int) (c.getWindow().getScaledHeight() / s.uiScale);
            if (s.width != cw || s.height != ch) bad.add("the screen's canvas " + s.width + "x" + s.height + " is not the window's " + cw + "x" + ch);
            if (a.buttons.isEmpty()) bad.add("no buy buttons");
            int small = 999;
            int kw = UiCanvas.width(), kh = UiCanvas.height();
            for (var e : s.children()) if (e instanceof UiButton u && u.visible) { // the War Table page's widgets: their canvas boxes, the art's own bounds
                int[] r = u.canvasBox(); small = Math.min(small, Math.min(r[2], r[3]));
                if (r[2] < 20 || r[3] < 20) bad.add("a hit target of " + r[2] + "x" + r[3] + " canvas px at " + r[0] + "," + r[1]);
                if (r[0] < 0 || r[1] < 0 || r[0] + r[2] > kw || r[1] + r[3] > kh) bad.add("a button outside the canvas " + kw + "x" + kh + " at " + r[0] + "," + r[1] + " " + r[2] + "x" + r[3]);
            } else if (e instanceof ButtonWidget b && b.visible) {
                small = Math.min(small, Math.min(b.getWidth(), b.getHeight()));
                if (b.getWidth() < 20 || b.getHeight() < 20) bad.add("a hit target of " + b.getWidth() + "x" + b.getHeight() + " at " + b.getX() + "," + b.getY());
                if (b.getX() < 0 || b.getY() < 0 || b.getX() + b.getWidth() > s.width || b.getY() + b.getHeight() > s.height) bad.add("a button outside the canvas at " + b.getX() + "," + b.getY() + " " + b.getWidth() + "x" + b.getHeight());
            }
            int widest = 0; for (var g : a.buttons) widest = Math.max(widest, g.drawnWidest);
            fitNotes.add(label + ": GUI " + s.width + "x" + s.height + ", canvas " + UiCanvas.width() + "x" + UiCanvas.height() + " at k=" + UiCanvas.k() + ", " + a.shown + " of " + a.total + " rows" + (a.scrolling ? " scrolling" : "") + ", buy buttons " + a.bw + " wide, rows " + a.rowH + " high, the widest drawn line " + widest + " px, the smallest hit target " + small + " px, " + (bad.isEmpty() ? "all whole and inside" : "FAILS " + bad));
            if (!bad.isEmpty()) fitFails.add(label + ": " + bad);
        }
        void fitReport() {
            boolean at854 = fitNotes.stream().anyMatch(n -> n.startsWith("854x480"));
            Phase2Checks.report("p2-armoury-fits", fitFails.isEmpty() && at854 && fitNotes.size() >= 2 + SIZES.length, "the page measured as drawn at 854x480 (barracks and field) and at the six sizes with eight troop types: " + String.join(" | ", fitNotes) + (fitFails.isEmpty() ? "" : " | FAILED: " + fitFails));
        }

        /** p2-armoury-off: with levels off the tab says so and offers nothing to buy. */
        void evalOff(RecruitmentScreen s) {
            boolean ok = s != null && s.tab == 1 && s.page.message.equals("Ranks are off") && s.page.buttons.isEmpty() && s.page.arrows.isEmpty() && CampaignClient.armoury.rows().isEmpty() && snap != null && snap.armoury().rows().isEmpty() && !Veterans.enabled() && s.page.headerDrawn.contains("Ranks are off");
            Phase2Checks.report("p2-armoury-off", ok, "with Veterans.override false the server sends " + (snap == null ? "nothing" : snap.armoury()) + " and the tab says \"" + (s == null ? "-" : s.page.message) + "\" with " + (s == null ? "-" : s.page.buttons.size() + " buy buttons") + " (want \"Ranks are off\" and none)");
        }

        /** The eight-type banner on the 854x480 canvas: the rows that fit, then the arrows, the wheel and Page Up and Down move by a row, never past either end. */
        void scrollSteps(RecruitmentScreen s) {
            var a = s.page; StringBuilder bad = new StringBuilder(), info = new StringBuilder();
            var all = BANNER8; int n = all.size();
            if (!a.scrolling || a.total != n || a.shown >= n || a.shown < 3) bad.append("the page scrolls=" + a.scrolling + " showing " + a.shown + " of " + a.total + "; ");
            int shown = a.shown; info.append(shown + " of " + n + " rows; ");
            if (a.arrows.size() != 2) { bad.append(a.arrows.size() + " arrows; "); scrollBad = bad.toString(); return; }
            if (a.arrows.get(0).live() || !a.arrows.get(1).live()) bad.append("at the top the up arrow is " + a.arrows.get(0).live() + " and the down arrow " + a.arrows.get(1).live() + "; ");
            if (!ids(a).equals(all.subList(0, shown))) bad.append("the first rows are " + ids(a) + "; ");
            click(s, a.arrows.get(1)); a = s.page;
            if (a.first != 1 || !ids(a).equals(all.subList(1, 1 + shown)) || !a.arrows.get(0).live()) bad.append("after the down arrow the first row is " + a.first + " showing " + ids(a) + "; ");
            for (int i = 0; i < n + 3; i++) s.mouseScrolled(120 * s.uiScale, 200 * s.uiScale, -1.0);
            a = s.page;
            if (a.first != n - shown || !ids(a).equals(all.subList(n - shown, n)) || a.arrows.get(1).live()) bad.append("the wheel down to the end left the first row at " + a.first + " (want " + (n - shown) + ") and the down arrow " + a.arrows.get(1).live() + "; ");
            s.mouseScrolled(120 * s.uiScale, 200 * s.uiScale, 1.0); a = s.page;
            if (a.first != n - shown - 1) bad.append("the wheel up from the end left the first row at " + a.first + "; ");
            s.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0); a = s.page;
            if (a.first != n - shown) bad.append("Page Down left the first row at " + a.first + "; ");
            s.keyPressed(GLFW.GLFW_KEY_PAGE_UP, 0, 0); s.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0); a = s.page;
            if (a.first != n - shown) bad.append("Page Up then Down left the first row at " + a.first + "; ");
            List<String> cut = a.fitFailures(); if (!cut.isEmpty()) bad.append("the last page does not fit: " + cut + "; ");
            info.append("the last page shows " + ids(a) + " from row " + (a.first + 1));
            scrollBad = bad.toString(); scrollInfo = info.toString();
        }
        static List<String> ids(ArmouryTab a) { List<String> out = new ArrayList<>(); for (var g : a.buttons) if (!out.contains(g.troop.id())) out.add(g.troop.id()); return out; }
        void scrollReport(RecruitmentScreen s) {
            var a = s == null ? null : s.page; String count = a == null ? "" : "Troops " + (a.first + 1) + "-" + (a.first + a.shown) + " of " + a.total;
            boolean counted = a != null && a.headerDrawn.contains(count);
            Phase2Checks.report("p2-armoury-scroll", scrollBad.isEmpty() && counted, "eight troop types at 854x480: " + scrollInfo + "; the header reads \"" + count + "\" as drawn=" + counted + (scrollBad.isEmpty() ? "" : "; WRONG: " + scrollBad));
        }

        /** The Back button's place on a page: it keeps the barracks' own place on both tabs. */
        static String backAt(RecruitmentScreen s) {
            for (var e : s.children()) if (e instanceof ButtonWidget b && b.getMessage().getString().equals("Esc  Back")) return b.getX() + "," + b.getY() + " " + b.getWidth() + "x" + b.getHeight();
            for (var e : s.children()) if (e instanceof UiButton u && u.getMessage().getString().equals("Back")) { int[] r = u.canvasBox(); return "rail " + r[0] + "," + r[1] + " " + r[2] + "x" + r[3]; } // the Armoury's rail plaque
            return "none";
        }
        String tabsBad = "", tabsInfo = "";
        void evalTabs(RecruitmentScreen s) {
            StringBuilder bad = new StringBuilder();
            // the barracks page is a War Table page (BarracksPage): its three plates on the ladder's axis, the rail as the Armoury has it
            int[] la = WarTableRecruit.ladderAt(); int px = la[0] + WarTableRecruit.AXIS - BarracksPage.PLATE_W / 2;
            if (s.tab != 0) bad.append("the click on the Barracks tab left the page at " + s.tab + "; ");
            String plates = "";
            int[][] want = {{px, la[1] + BarracksPage.RECRUIT_Y}, {px, la[1] + BarracksPage.DISBAND_Y}, {px, la[1] + BarracksPage.CANCEL_Y}};
            ButtonWidget[] got = {s.recruit, s.disband, s.cancel}; String[] names = {"Recruit", "Disband", "Cancel"};
            for (int i = 0; i < 3; i++) {
                if (!(got[i] instanceof UiButton u)) { bad.append("the " + names[i] + " plate is not a War Table plate; "); continue; }
                int[] r = u.canvasBox(); plates += names[i] + " " + r[0] + "," + r[1] + " ";
                if (r[0] != want[i][0] || r[1] != want[i][1] || r[2] != BarracksPage.PLATE_W || r[3] != BarracksPage.PLATE_H) bad.append("the " + names[i] + " plate moved; ");
            }
            String back = backAt(s); // the rail's Back plaque, in the same place on both pages
            if (!back.equals(tabsFirst) || !back.startsWith("rail ")) bad.append("Back is at " + back + " here and " + tabsFirst + " on the Armoury tab (want the same rail plaque); ");
            if (!s.barracksTab.getMessage().getString().startsWith("> ") || s.armouryTab.getMessage().getString().startsWith("> ")) bad.append("the rail does not mark the barracks as the page you are on; ");
            if (s.page.buttons.stream().anyMatch(g -> s.children().contains(g))) bad.append("the Armoury's buttons are still on the barracks page; ");
            tabsBad = bad.toString(); tabsInfo = "the barracks page after the tab click: plates " + plates.trim() + ", Back " + back;
        }
        void tabsReport() {
            Phase2Checks.report("p2-armoury-tabs", tabsBad.isEmpty() && tabsToggled && tabsClosed, tabsInfo + "; the key on the barracks page opened the Armoury tab of the same screen=" + tabsToggled + ", the key on the Armoury tab closed the screen=" + tabsClosed + (tabsBad.isEmpty() ? "" : "; WRONG: " + tabsBad));
        }
    }
}
