package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.function.Supplier;

/**
 * Suites statuses and levels-gilded to levels-tidecallers of the in-match progression plan (Tasks 6 and 7, lane B). Never included in releases.
 * Task 6 builds statuses (StatusChecks); the levels-<house> suites stay skeletons, each reporting fail p2-<suite>-not-built, until Task 7 replaces them.
 * Phase2Checks.SUITES takes these entries through register (GRAVES_PHASE2_CHECKS=<suite>), and the checks print through Phase2Checks.report.
 */
final class AbilityLevelChecks {
    private AbilityLevelChecks() {}

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("statuses", StatusChecks::new);
        suites.put("levels-gilded", () -> new NotBuilt("levels-gilded", "Task 7 (lane B): the Gilded level rows"));
        suites.put("levels-graveborn", () -> new NotBuilt("levels-graveborn", "Task 7 (lane B): the Graveborn level rows"));
        suites.put("levels-wildmoot", () -> new NotBuilt("levels-wildmoot", "Task 7 (lane B): the Wildmoot level rows"));
        suites.put("levels-ironclad", () -> new NotBuilt("levels-ironclad", "Task 7 (lane B): the Ironclad level rows"));
        suites.put("levels-tidecallers", () -> new NotBuilt("levels-tidecallers", "Task 7 (lane B): the Tidecallers level rows"));
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
     * Suite statuses (in-match progression Task 6): the status engine (Statuses) and the thirteen small mechanics N1 to N13, with the per-level ability rows
     * (Abilities.row(u), second(u), rowsChanged). Gilded's fixture (Phase2Checks.Staged): a stone platform in the sky, two ad hoc bot armies (dawn, dusk) and an
     * allied army on each side; troops are posted frozen and pinned with their armour off, each stage sets their level (u.level, then Abilities.rowsChanged, as
     * lane A's level up does), clears the armies' House traits so no trait shapes a number, does one thing through the real path (Combat.resolve with its on-hit
     * abilities, a cast's own method, Statuses.apply) and reads the result: Combat.LAST, the statuses, the kill-credit logs (Battle.credit).
     */
    static final class StatusChecks extends Phase2Checks.Staged {
        GravesAndGlory.Squad ally, duskAlly;
        GravesAndGlory.Unit a, a2, v, w, x, y, z;
        final List<GravesAndGlory.Unit> group = new ArrayList<>();
        Combat.HitLog h0, h1;
        float hp0, hp1;
        long t0;
        int n0, n1;
        double d0;
        boolean flag, fogCredit, cleanseCredit;
        String why = "";

        @Override void cleared() { group.clear(); a = a2 = v = w = x = y = z = null; }

        /** The soldier at a level, his rows and tags read again (lane A's level up calls the same seam). */
        static GravesAndGlory.Unit lv(GravesAndGlory.Unit u, int level) { u.level = level; Abilities.rowsChanged(u); return u; }
        /** No House trait shapes the stage's numbers: the armies' tiers off (b.spawn counts them again with each troop). */
        void traits() { for (var s : new GravesAndGlory.Squad[]{dawn, dusk, ally, duskAlly}) s.traits.clear(); }
        /** A real blow of 1 from the attacker, past the target's hurt cooldown, with its on-hit abilities (Combat.resolve, not secondary). */
        static float blow(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity to, String kind) { to.timeUntilRegen = 0; to.hurtTime = 0; return Combat.resolve(b, from, to, 1f, kind, false); }
        /** A secondary blow of 1 (no on-hit), its HitLog or null (Phase2Checks.Commanders.hit). */
        static Combat.HitLog probe(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity to, String kind) { to.timeUntilRegen = 0; return Phase2Checks.Commanders.hit(b, from, to, kind); }
        /** A dummy target of an army: armour off, 4000 health. */
        GravesAndGlory.Unit dummy(GravesAndGlory.Battle b, GravesAndGlory.Squad s, double dx, double dz) { return tough(bare(post(b, s, "ic_breaker", dx, dz, s.team == 0 ? 0 : 180, true))); }
        /** Who the kill-credit log credits for this victim dying now to the team, with no killer (design 3.2). */
        static Set<Integer> credited(GravesAndGlory.Battle b, LivingEntity victim, int team) { return b.credit.credited(victim.getId(), -1, team, b.time, Veterans.window()); }
        /** The troops logged as supporting this one now (the supporters CreditLog.credited adds for a killer). */
        static Set<Integer> supporters(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return b.credit.credited(-7, u.mob.getId(), u.squad.team, b.time, Veterans.window()); }
        Vec3d spot(double dx, double dz) { return new Vec3d(site.getX() + .5 + dx, site.getY(), site.getZ() + .5 + dz); }
        static double horizontal(Vec3d v) { return Math.hypot(v.x, v.z); }
        static String st(GravesAndGlory.Battle b, LivingEntity e) {
            var s = e.getStatusEffect(StatusEffects.SLOWNESS);
            return "slow " + (s == null ? "none" : "level " + s.getAmplifier() + " for " + s.getDuration()) + ", weaken " + Statuses.remaining(b, e, "weaken") + ", bleed " + Statuses.remaining(b, e, "bleed") + ", mark " + Statuses.remaining(b, e, "mark");
        }

        public void cleanup(MinecraftClient c) {
            Phase2Checks.server(c, b -> {
                Houses.forceRise = null; Houses.reset(b); clearFixture(b, "cleanup"); b.mists.clear();
                if (site != null) platform(b, site, 12, Blocks.AIR.getDefaultState());
                unforce(b);
                if (b.running) b.campaign.returnLobby();
            });
        }

        public void step(MinecraftClient c, int tick) {
            if (tick == 20) { c.setScreen(null); UUID id = c.player.getUuid(); Phase2Checks.server(c, b -> { player = id; next(b, 1); }); }
            if (stage == 99 && !finished) { finished = true; c.setScreen(null); Phase2Checks.finish(c, "statuses"); }
            if (tick > 12000 && !finished) { finished = true; Phase2Checks.report("p2-statuses-timeout", false, "stuck in stage " + stage); Phase2Checks.finish(c, "statuses"); }
        }

        public void serverTick(GravesAndGlory.Battle b) {
            if (player == null || stage >= 99) return;
            try { stages(b); } catch (RuntimeException e) { Phase2Checks.report("p2-statuses-exception", false, "stage " + stage + ": " + e); e.printStackTrace(); next(b, 99); }
        }

        void stages(GravesAndGlory.Battle b) {
            hold();
            switch (stage) {
                case 1 -> { var p = sp(b); if (p == null) return; if (b.running) { b.campaign.returnLobby(); return; } next(b, 5); }
                case 5 -> { var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return; next(b, 6); }
                case 6 -> {
                    if (since(b) < 10) return;
                    stageFixture(b); Statuses.clear(b);
                    var base = GravesAndGlory.Battle.base(0);
                    ally = new GravesAndGlory.Squad(UUID.randomUUID(), 0, base); ally.order = "hold"; ally.bot = true;
                    duskAlly = new GravesAndGlory.Squad(UUID.randomUUID(), 1, base); duskAlly.order = "hold"; duskAlly.bot = true;
                    next(b, 10);
                }
                case 10 -> { // p2-status-slow-as-before: a Barrow Stalker's frostshot at level 1 lays exactly the base row's Slowness, and nothing else
                    a = lv(post(b, dawn, "stalker", 0, -6, 0, true), 1); v = dummy(b, dusk, 0, 0); traits();
                    var row = Abilities.row(a.spec);
                    blow(b, a, v.mob, "arrow");
                    var s = v.mob.getStatusEffect(StatusEffects.SLOWNESS);
                    boolean rows = Abilities.row(a) == row && row.id().equals("frostshot") && Abilities.second(a).id().equals("none");
                    boolean slowed = s != null && s.getAmplifier() == row.statusLevel() && s.getDuration() == row.statusTicks();
                    boolean only = !Statuses.has(b, v.mob, "weaken") && !Statuses.has(b, v.mob, "bleed") && !Statuses.has(b, v.mob, "mark") && v.stunnedUntil <= b.time;
                    Phase2Checks.report("p2-status-slow-as-before", rows && slowed && only, "a level-1 Barrow Stalker's rows: " + Abilities.row(a).id() + " and " + Abilities.second(a).id() + " (want frostshot and none)=" + rows + "; its shot leaves " + st(b, v.mob) + " (want Slowness level " + row.statusLevel() + " for " + row.statusTicks() + ", the row's)=" + slowed + "; no other status or stun=" + only);
                    clearFixture(b, "slow"); next(b, 12);
                }
                case 12 -> { // p2-status-weaken: Marrow arrows on a Squire
                    a = lv(post(b, dawn, "bonebow", 0, -8, 0, true), 3); v = tough(bare(post(b, dusk, "knight", 0, 0, 180, true))); w = dummy(b, dawn, 0, -2); traits();
                    h0 = probe(b, v, w.mob, "melee");
                    blow(b, a, v.mob, "arrow");
                    d0 = Statuses.remaining(b, v.mob, "weaken");
                    h1 = probe(b, v, w.mob, "melee");
                    next(b, 13);
                }
                case 13 -> {
                    if (since(b) < 41) return;
                    var later = probe(b, v, w.mob, "melee");
                    boolean ok = h0 != null && h1 != null && later != null && near(h0.status(), 1) && near(h1.status(), .85) && near(d0, 40) && near(later.status(), 1);
                    Phase2Checks.report("p2-status-weaken", ok, "a level-3 Bone Archer's arrow weakens a Squire for " + (long) d0 + " ticks (want 40); the Squire's blows: before " + (h0 == null ? "none" : "status " + fmt(h0.status())) + " (want 1), after " + (h1 == null ? "none" : show(h1)) + " (want status 0.85), 41 ticks on " + (later == null ? "none" : "status " + fmt(later.status())) + " (want 1: it wore off)");
                    clearFixture(b, "weaken"); next(b, 14);
                }
                case 14 -> { // p2-status-bleed, p2-status-bleed-undead: Rotten blade on a Risen (undead) and on a Hearthguard (not), 80 ticks of bleed
                    a = lv(post(b, dawn, "revenant", -3, -2, 0, true), 3); x = lv(post(b, dawn, "revenant", 3, -2, 0, true), 3);
                    v = tough(bare(post(b, dusk, "revenant", -3, 0, 180, true))); w = tough(bare(post(b, dusk, "ic_hearthguard", 3, 0, 180, true))); traits();
                    blow(b, a, v.mob, "melee"); blow(b, x, w.mob, "melee");
                    hp0 = v.mob.getHealth(); hp1 = w.mob.getHealth(); n0 = Statuses.bleedTicks; d0 = Statuses.remaining(b, v.mob, "bleed");
                    flag = credited(b, v.mob, 0).contains(a.mob.getId());
                    next(b, 15);
                }
                case 15 -> {
                    if (since(b) < 100) return;
                    float lostV = hp0 - v.mob.getHealth(), lostW = hp1 - w.mob.getHealth();
                    boolean gone = !Statuses.has(b, v.mob, "bleed") && !Statuses.has(b, w.mob, "bleed");
                    Phase2Checks.report("p2-status-bleed", near(lostW, 4) && Statuses.bleedTicks - n0 == 8 && gone && near(d0, 80) && flag, "Rotten blade bleeds for " + (long) d0 + " ticks (want 80); over 100 ticks a Hearthguard lost " + fmt(lostW) + " (want 4: 1 every 20 ticks through the troop damage type), " + (Statuses.bleedTicks - n0) + " bleed ticks in all (want 8, both targets), both bleeds gone=" + gone + ", the striker in the target's credit log=" + flag);
                    Phase2Checks.report("p2-status-bleed-undead", v.mob.isUndead() && near(lostV, 4), "a Risen (undead=" + v.mob.isUndead() + ") bleeding for 80 ticks lost " + fmt(lostV) + " (want 4: vanilla Poison would leave him whole)");
                    clearFixture(b, "bleed"); next(b, 16);
                }
                case 16 -> { // p2-status-nostack
                    a = post(b, dawn, "revenant", 0, -2, 0, true); v = dummy(b, dusk, 0, 0); traits();
                    Statuses.apply(b, a, v.mob, "bleed", 60, 0); long r60 = Statuses.remaining(b, v.mob, "bleed");
                    Statuses.apply(b, a, v.mob, "bleed", 40, 0); long r40 = Statuses.remaining(b, v.mob, "bleed");
                    Statuses.apply(b, a, v.mob, "weaken", 60, 0, .15); Statuses.apply(b, a, v.mob, "weaken", 40, 0, .3); double f = Statuses.factorOut(b, v.mob);
                    Statuses.apply(b, a, v.mob, "bleed", 90, 0); long r90 = Statuses.remaining(b, v.mob, "bleed");
                    boolean ok = r60 == 60 && r40 == 60 && near(f, .85) && r90 == 90;
                    Phase2Checks.report("p2-status-nostack", ok, "a 60-tick bleed leaves " + r60 + "; a 40-tick one over it leaves " + r40 + " (want 60: the longer stands); a 40-tick weaken of 0.30 over a 60-tick one of 0.15 leaves x" + fmt(f) + " (want 0.85); a 90-tick bleed over the 60 leaves " + r90 + " (want 90: the longer takes its place)");
                    clearFixture(b, "nostack"); next(b, 18);
                }
                case 18 -> { // p2-status-knockback: Heavy bolt
                    a2 = lv(post(b, dawn, "ic_arbalist", -4, -8, 0, true), 2); a = lv(post(b, dawn, "ic_arbalist", 4, -8, 0, true), 3);
                    v = dummy(b, dusk, -4, 0); w = dummy(b, dusk, 4, 0); traits();
                    v.mob.setVelocity(Vec3d.ZERO); w.mob.setVelocity(Vec3d.ZERO); n0 = Statuses.knocks;
                    blow(b, a2, v.mob, "arrow"); double s2 = horizontal(v.mob.getVelocity());
                    blow(b, a, w.mob, "arrow"); double s3 = horizontal(w.mob.getVelocity());
                    boolean ok = Statuses.knocks - n0 == 1 && near(Statuses.lastKnock, .6) && Math.abs(s3 - s2 / 2 - .6) < .05;
                    Phase2Checks.report("p2-status-knockback", ok, "a level-2 Arbalist's bolt leaves its target moving " + fmt(s2) + " (the blow's own knock); a level-3 one's leaves " + fmt(s3) + ", so Heavy bolt added " + fmt(s3 - s2 / 2) + " (want 0.60: vanilla halves the motion it finds); knockbacks thrown " + (Statuses.knocks - n0) + " (want 1) of strength " + fmt(Statuses.lastKnock));
                    clearFixture(b, "knockback"); next(b, 20);
                }
                case 20 -> { // p2-stun-single: Shield bash
                    a = lv(post(b, dawn, "knight", 0, -2, 0, true), 3); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 1.2, .4); traits();
                    n0 = Statuses.knocks; t0 = b.time;
                    blow(b, a, v.mob, "melee");
                    long stunV = v.stunnedUntil - t0, cd = Abilities.SECOND_AT.getOrDefault(a, 0L) - t0; boolean neighbour = w.stunnedUntil <= t0; int knocked = Statuses.knocks - n0;
                    blow(b, a, v.mob, "melee");
                    boolean once = v.stunnedUntil - t0 == stunV && Statuses.knocks - n0 == knocked;
                    flag = stunV == 20 && cd == 100 && neighbour && knocked == 1 && near(Statuses.lastKnock, 1) && once;
                    why = "a level-3 Squire's blow stuns its target for " + stunV + " ticks (want 20) and throws it at " + fmt(Statuses.lastKnock) + " (want 1.0, " + knocked + " knock); the Squire beside it is not stunned=" + neighbour + "; the bash waits " + cd + " ticks (want 100) and a second blow at once neither stuns nor throws again=" + once;
                    next(b, 21);
                }
                case 21 -> {
                    if (b.time - t0 < 100) return;
                    n1 = Statuses.knocks;
                    blow(b, a, v.mob, "melee");
                    boolean again = v.stunnedUntil == b.time + 20 && Statuses.knocks - n1 == 1;
                    Phase2Checks.report("p2-stun-single", flag && again, why + "; 100 ticks on the next blow bashes again=" + again);
                    clearFixture(b, "stun"); next(b, 22);
                }
                case 22 -> { // p2-onkill-heal: Reap
                    a = lv(post(b, dawn, "reaper", -3, -2, 0, true), 3); x = lv(post(b, dawn, "reaper", 3, -2, 0, true), 2); traits();
                    for (var u : List.of(a, x)) u.mob.setHealth(u.mob.getMaxHealth() * .5f);
                    Abilities.onKill(b, a); Abilities.onKill(b, x);
                    float sa = a.mob.getHealth() / a.mob.getMaxHealth(), sx = x.mob.getHealth() / x.mob.getMaxHealth();
                    Phase2Checks.report("p2-onkill-heal", Math.abs(sa - .75) < 1e-3 && Math.abs(sx - .5) < 1e-3, "a level-3 Grave Reaper at half health lands a killing blow (Abilities.onKill, which lane A's kill path calls) and stands at " + fmt(sa) + " of his health (want 0.75: Reap's 25%); a level-2 one stays at " + fmt(sx) + " (want 0.50)");
                    clearFixture(b, "reap"); next(b, 24);
                }
                case 24 -> { // p2-raise: Death knell
                    a = lv(post(b, dawn, "hexer", 0, -10, 0, true), 3); traits();
                    for (double[] at : new double[][]{{0, 0}, {1.2, .6}, {-1.2, .6}}) { var u = bare(post(b, dusk, "knight", at[0], at[1], 180, true)); u.mob.setHealth(1f); group.add(u); }
                    traits();
                    n0 = b.deaths.size(); t0 = b.time;
                    a.spellCenter = spot(0, .3); a.windupUntil = b.time; a.stunnedUntil = 0;
                    Abilities.burst(b, a, Abilities.row(a), b.time);
                    boolean killed = group.stream().noneMatch(u -> u.mob.isAlive());
                    var raised = b.units.stream().filter(u -> u.temporary && u.squad == dawn && u.mob.isAlive()).toList();
                    boolean timed = !raised.isEmpty() && raised.stream().allMatch(u -> u.expiresAt == t0 + 300 && u.spec.id().equals(Commander.RISEN) && group.stream().anyMatch(g -> g.mob.getPos().squaredDistanceTo(u.mob.getPos()) < .01));
                    int logged = b.deaths.size() - n0;
                    var more = bare(post(b, dusk, "knight", 0, 0, 180, true)); more.mob.setHealth(1f); traits();
                    a.spellCenter = spot(0, 0); a.windupUntil = b.time;
                    Abilities.burst(b, a, Abilities.row(a), b.time);
                    long standing = b.units.stream().filter(u -> u.temporary && u.squad == dawn && u.mob.isAlive()).count();
                    x = lv(post(b, dawn, "hexer", 8, -10, 0, true), 2); var lone = bare(post(b, dusk, "knight", 8, 0, 180, true)); lone.mob.setHealth(1f); traits();
                    x.spellCenter = spot(8, 0); x.windupUntil = b.time; x.stunnedUntil = 0;
                    Abilities.burst(b, x, Abilities.row(x), b.time);
                    long fromTwo = b.units.stream().filter(u -> u.temporary && u.squad == dawn && u.mob.isAlive()).count() - standing;
                    Houses.reset(b); Statuses.clear(b); // a round's end (startRound, returnLobby, switchMap: Battle.clearProgression and Houses.reset)
                    boolean vanished = raised.stream().noneMatch(u -> u.mob.isAlive() || b.units.contains(u)) && Abilities.RAISED.isEmpty();
                    boolean ok = killed && raised.size() == 2 && timed && logged == 1 && standing == 2 && !more.mob.isAlive() && fromTwo == 0 && !lone.mob.isAlive() && vanished;
                    Phase2Checks.report("p2-raise", ok, "a level-3 Gravecaller's blight kills three Squires=" + killed + " and raises " + raised.size() + " temporary Risen of her army (want 2) where they fell, for 300 ticks=" + timed + "; the death log keeps " + logged + " of the three (want 1: the raised leave it); a fourth kill raises none, " + standing + " stand (want 2); a level-2 Gravecaller's kill raises " + fromTwo + " (want 0); at a round's end they are gone and the count forgotten=" + vanished);
                    clearFixture(b, "raise"); next(b, 26);
                }
                case 26 -> { // p2-rise-l3: Undying King
                    Houses.forceRise = null;
                    x = lv(post(b, dawn, "gb_hollowking", -5, 0, 0, true), 2); a = lv(post(b, dawn, "gb_hollowking", 5, 0, 0, true), 3); traits();
                    Phase2Checks.Graveborn.fall(b, x); Phase2Checks.Graveborn.fall(b, a);
                    boolean two = !x.mob.isAlive(), rose = a.mob.isAlive() && a.rose, half = Math.abs(a.mob.getHealth() / a.mob.getMaxHealth() - .5) < 1e-3;
                    Phase2Checks.Graveborn.fall(b, a);
                    boolean once = !a.mob.isAlive();
                    Phase2Checks.report("p2-rise-l3", two && rose && half && once, "a fall kills a level-2 Hollow King (a Legendary, no Rise Again)=" + two + "; a level-3 one rises=" + rose + " at half health=" + half + " and the next fall kills him (once)=" + once);
                    clearFixture(b, "rise"); next(b, 28);
                }
                case 28 -> { // p2-mark: Hunter's mark
                    a = lv(post(b, dawn, "wm_hunter", 0, -8, 0, true), 3); x = post(b, dawn, "revenant", -1.5, -2, 0, true); y = post(b, ally, "revenant", 1.5, -2, 0, true); v = dummy(b, dusk, 0, 0); traits();
                    var mb = probe(b, x, v.mob, "melee"); var ab = probe(b, y, v.mob, "melee");
                    blow(b, a, v.mob, "arrow");
                    long left = Statuses.remaining(b, v.mob, "mark");
                    var ma = probe(b, x, v.mob, "melee"); var aa = probe(b, y, v.mob, "melee");
                    boolean ok = mb != null && ab != null && ma != null && aa != null && near(mb.commander(), ab.commander()) && Math.abs(ma.commander() / mb.commander() - 1.1) < 1e-9 && near(aa.commander(), ab.commander()) && left == 60;
                    Phase2Checks.report("p2-mark", ok, "a level-3 Hide Hunter's arrow marks its target for " + left + " ticks (want 60); the commander share of a blow from his army's Risen: " + (mb == null ? "none" : fmt(mb.commander())) + " before, " + (ma == null ? "none" : fmt(ma.commander())) + " after (want x1.10); from an allied army's Risen: " + (ab == null ? "none" : fmt(ab.commander())) + " before, " + (aa == null ? "none" : fmt(aa.commander())) + " after (want the same)");
                    clearFixture(b, "mark"); next(b, 30);
                }
                case 30 -> { // p2-cond-slowed-wounded: Run them down
                    a = lv(post(b, dawn, "wm_hound", -3, -2, 0, true), 3); x = lv(post(b, dawn, "wm_hound", 3, -2, 0, true), 2); v = dummy(b, dusk, -3, 0); w = dummy(b, dusk, 3, 0); traits();
                    var plain = probe(b, a, v.mob, "melee");
                    v.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0));
                    var slowed = probe(b, a, v.mob, "melee");
                    v.mob.removeStatusEffect(StatusEffects.SLOWNESS);
                    v.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40); v.mob.setHealth(12f); // exactly 30%: the tough dummy's max is clamped to 1024, whose 30% is no float (307.2f lies above the line)
                    var wounded = probe(b, a, v.mob, "melee");
                    v.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0));
                    var both = probe(b, a, v.mob, "melee");
                    w.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0));
                    var two = probe(b, x, w.mob, "melee");
                    boolean ok = plain != null && near(plain.counters(), 1) && !plain.rows().contains("C20") && Phase2Checks.Gilded.is(slowed, 1.3, "C20") && Phase2Checks.Gilded.is(wounded, 1.3, "C20") && Phase2Checks.Gilded.is(both, 1.3, "C20") && two != null && near(two.counters(), 1) && !two.rows().contains("C20");
                    Phase2Checks.report("p2-cond-slowed-wounded", ok, "a level-3 Moot Hound (tags " + a.tags + ") on a whole, unslowed target " + show(plain) + " (want 1.0); slowed " + show(slowed) + "; at 30% health (12 of 40) " + show(wounded) + "; both " + show(both) + " (want x1.3, C20, each); a level-2 hound on a slowed target " + show(two) + " (want 1.0)");
                    clearFixture(b, "rundown"); next(b, 32);
                }
                case 32 -> { // p2-thickhide
                    a = post(b, dawn, "bonebow", 0, -8, 0, true); x = post(b, dawn, "revenant", 0, -3, 0, true);
                    v = lv(tough(post(b, dusk, "wm_oldhorn", -3, 0, 180, true)), 3); w = lv(tough(post(b, dusk, "wm_oldhorn", 4, 0, 180, true)), 2); traits();
                    var shot = probe(b, a, v.mob, "arrow"); var bite = probe(b, x, v.mob, "melee"); var two = probe(b, a, w.mob, "arrow");
                    boolean ok = v.tags.contains("thickhide") && Phase2Checks.Gilded.is(shot, .75, "C19") && bite != null && !bite.rows().contains("C19") && two != null && !two.rows().contains("C19") && near(two.counters(), 1);
                    Phase2Checks.report("p2-thickhide", ok, "a level-3 Old Horn (tags " + v.tags + "): an arrow " + show(shot) + " (want 0.75, C19); a blow " + show(bite) + " (want no C19); an arrow on a level-2 Old Horn " + show(two) + " (want 1.0)");
                    clearFixture(b, "thickhide"); next(b, 34);
                }
                case 34 -> { // p2-aura-taken (Furnace heart) and p2-aura-call (Admiral's call)
                    var heart = lv(post(b, dusk, "ic_colossus", 0, 4, 180, true), 3);
                    v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 9, 4); y = dummy(b, duskAlly, -3, 2); a = post(b, dawn, "revenant", 0, -3, 0, true); traits();
                    var in = probe(b, a, v.mob, "melee"); var out = probe(b, a, w.mob, "melee"); var allied = probe(b, a, y.mob, "melee");
                    lv(heart, 2); var two = probe(b, a, v.mob, "melee");
                    boolean ok = in != null && near(in.auras(), .9) && out != null && near(out.auras(), 1) && allied != null && near(allied.auras(), 1) && two != null && near(two.auras(), 1);
                    Phase2Checks.report("p2-aura-taken", ok, "a level-3 Brassjaw's army: a troop 4 from him takes auras x" + (in == null ? "none" : fmt(in.auras())) + " (want 0.9), one 9 from him x" + (out == null ? "none" : fmt(out.auras())) + " (want 1), an allied army's troop 3.6 from him x" + (allied == null ? "none" : fmt(allied.auras())) + " (want 1: his own army only); at level 2 the near one takes x" + (two == null ? "none" : fmt(two.auras())) + " (want 1)");
                    clearFixture(b, "furnace");
                    var call = lv(post(b, dawn, "tc_admiral", 0, -4, 0, true), 3);
                    a = post(b, dawn, "revenant", 0, -2, 0, true); x = post(b, dawn, "revenant", 11, -4, 0, true); y = post(b, ally, "revenant", -2, -2, 0, true); v = dummy(b, dusk, 0, 0); traits();
                    var close = probe(b, a, v.mob, "melee"); var far = probe(b, x, v.mob, "melee"); var other = probe(b, y, v.mob, "melee");
                    lv(call, 2); var low = probe(b, a, v.mob, "melee");
                    boolean callOk = close != null && near(close.auras(), 1.1) && far != null && near(far.auras(), 1) && other != null && near(other.auras(), 1) && low != null && near(low.auras(), 1);
                    Phase2Checks.report("p2-aura-call", callOk, "a level-3 Admiral Vael's army: a Risen 2 from her deals auras x" + (close == null ? "none" : fmt(close.auras())) + " (want 1.1), one 11 from her x" + (far == null ? "none" : fmt(far.auras())) + " (want 1), an allied army's Risen 2.8 from her x" + (other == null ? "none" : fmt(other.auras())) + " (want 1); at level 2 x" + (low == null ? "none" : fmt(low.auras())) + " (want 1)");
                    clearFixture(b, "call"); next(b, 36);
                }
                case 36 -> { // p2-zone-status, first Chill fog: a level-3 Mist Caller's cloud on a dummy
                    a = lv(post(b, dawn, "tc_mist", 0, -12, 0, true), 3); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 8, 0); traits();
                    b.mists.clear();
                    a.spellCenter = spot(0, 0); a.windupUntil = b.time; a.stunnedUntil = 0;
                    Abilities.cloud(b, a, Abilities.row(a), b.time);
                    var fog = b.mists.isEmpty() ? null : b.mists.get(b.mists.size() - 1);
                    flag = fog != null && fog.rider() != null && fog.rider().id().equals("tc_mist_l3") && fog.row().id().equals("tc_mist_l2") && fog.penalises() && !fog.burning();
                    why = "Chill fog: the cloud acts by " + (fog == null ? "none" : fog.row().id() + " with rider " + (fog.rider() == null ? "none" : fog.rider().id()) + ", halving shots=" + fog.penalises()) + " (want tc_mist_l2, tc_mist_l3, true)=" + flag;
                    next(b, 37);
                }
                case 37 -> {
                    if (since(b) < 12) return;
                    boolean inside = Statuses.has(b, v.mob, "weaken") && near(Statuses.factorOut(b, v.mob), .85) && Statuses.has(b, v.mob, "slow"), outside = !Statuses.has(b, w.mob, "weaken") && !Statuses.has(b, w.mob, "slow");
                    fogCredit = credited(b, v.mob, 0).contains(a.mob.getId());
                    flag &= inside && outside;
                    why += "; inside it " + st(b, v.mob) + " (want slowed and weakened, x" + fmt(Statuses.factorOut(b, v.mob)) + ")=" + inside + "; outside " + st(b, w.mob) + " (want neither)=" + outside;
                    b.mists.clear(); clearFixture(b, "fog");
                    a = lv(post(b, dawn, "ic_mortar", 0, -14, 0, true), 3); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 3.5, 0); traits();
                    a.spellCenter = spot(0, 0); a.windupUntil = b.time; a.stunnedUntil = 0;
                    hp1 = w.mob.getHealth();
                    Abilities.burst(b, a, Abilities.row(a), b.time);
                    var fire = b.mists.isEmpty() ? null : b.mists.get(b.mists.size() - 1);
                    boolean ground = fire != null && fire.burning() && !fire.penalises() && near(fire.radius(), 2.5) && fire.until() - b.time == 60 && fire.row().id().equals("ic_mortar_l3") && !Abilities.inMist(b, v.mob.getPos().add(0, 1, 0));
                    flag &= ground;
                    why += "; Firepot: the shell leaves " + (fire == null ? "nothing" : "ground of radius " + fmt(fire.radius()) + " for " + (fire.until() - b.time) + " ticks by " + fire.row().id() + ", burning=" + fire.burning() + ", halving shots=" + fire.penalises()) + " (want 2.5, 60, ic_mortar_l3, true, false)=" + ground;
                    next(b, 38);
                }
                case 38 -> {
                    if (since(b) < 12) return;
                    boolean bleeds = Statuses.has(b, v.mob, "bleed"), spared = !Statuses.has(b, w.mob, "bleed") && w.mob.getHealth() < hp1;
                    Phase2Checks.report("p2-zone-status", flag && bleeds && spared, why + "; a dummy in the burning ground bleeds=" + bleeds + ", one 3.5 from the centre (inside the shell, outside the ground) was hurt by the shell and does not=" + spared);
                    b.mists.clear(); clearFixture(b, "firepot"); next(b, 40);
                }
                case 40 -> { // p2-cleanse: Dawnlight and Rousing howl
                    a = lv(post(b, dawn, "mage", -8, -4, 0, true), 3); a2 = lv(post(b, dawn, "mage", 8, -4, 0, true), 2);
                    v = dummy(b, dawn, -8, 0); w = dummy(b, dawn, 8, 0); z = post(b, dusk, "revenant", 0, 8, 180, true); traits();
                    for (var e : List.of(v, w)) { e.mob.setHealth(e.mob.getMaxHealth() - 10); Statuses.apply(b, z, e.mob, "slow", 200, 0); Statuses.apply(b, z, e.mob, "weaken", 200, 0, .15); Statuses.apply(b, z, e.mob, "bleed", 200, 0); }
                    a.abilityAt = 0; a2.abilityAt = 0;
                    Abilities.mend(b, a, Abilities.row(a), b.time); Abilities.mend(b, a2, Abilities.row(a2), b.time);
                    String dawnV = st(b, v.mob), dawnW = st(b, w.mob);
                    boolean dawnlight = !Statuses.has(b, v.mob, "slow") && !Statuses.has(b, v.mob, "weaken") && !Statuses.has(b, v.mob, "bleed") && Statuses.has(b, w.mob, "slow") && Statuses.has(b, w.mob, "weaken") && Statuses.has(b, w.mob, "bleed");
                    cleanseCredit = supporters(b, v).contains(a.mob.getId());
                    var seer = lv(post(b, dawn, "wm_seer", -8, 6, 0, true), 3); var seer2 = lv(post(b, dawn, "wm_seer", 8, 6, 0, true), 2); traits();
                    for (var e : List.of(v, w)) { Statuses.apply(b, z, e.mob, "slow", 200, 0); Statuses.apply(b, z, e.mob, "weaken", 200, 0, .15); Statuses.apply(b, z, e.mob, "bleed", 200, 0); }
                    seer.abilityAt = 0; seer.stunnedUntil = 0; seer2.abilityAt = 0; seer2.stunnedUntil = 0;
                    Abilities.howl(b, seer, Abilities.row(seer), b.time); Abilities.howl(b, seer2, Abilities.row(seer2), b.time);
                    boolean rousing = !Statuses.has(b, v.mob, "slow") && !Statuses.has(b, v.mob, "weaken") && !Statuses.has(b, v.mob, "bleed") && Statuses.has(b, w.mob, "weaken") && Statuses.has(b, w.mob, "bleed");
                    boolean kept = near(v.howlAmount, .2) && near(v.howlSpeed, .2);
                    cleanseCredit &= supporters(b, v).contains(seer.mob.getId());
                    Phase2Checks.report("p2-cleanse", dawnlight && rousing && kept, "Dawnlight: a level-3 Sunweaver's mend leaves her ally " + dawnV + " (want none of the three); a level-2 one's leaves hers " + dawnW + " (want all three)=" + dawnlight + "; Rousing howl: a level-3 Seer's howl leaves his troop " + st(b, v.mob) + ", a level-2 one's leaves his " + st(b, w.mob) + " (want weaken and bleed still)=" + rousing + "; the howl still gives the level-2 +" + fmt(v.howlAmount) + " damage and +" + fmt(v.howlSpeed) + " speed (want 0.20 each: the rider does not reset it)=" + kept);
                    clearFixture(b, "cleanse"); next(b, 42);
                }
                case 42 -> { // p2-output-tier
                    a = post(b, dawn, "mage", -8, -4, 0, true); a.weaponTier = 3; v = dummy(b, dawn, -8, 0); traits();
                    v.mob.setHealth(v.mob.getMaxHealth() - 20); a.abilityAt = 0;
                    Abilities.mend(b, a, Abilities.row(a), b.time);
                    Projectiles.Shot heal = null; for (var s : b.shots) if (s.shooter() == a && s.kind().equals("mend")) heal = s;
                    double want = 5 * b.campaign.damage(a) * 1.2;
                    boolean heals = heal != null && Math.abs(heal.amount() - want) < 1e-4 && near(Veterans.outputFactor(a), 1.2);
                    x = post(b, dawn, "tc_mist", 8, -12, 0, true); x.weaponTier = 3; y = post(b, dawn, "tc_mist", 4, -12, 0, true); traits();
                    b.mists.clear();
                    for (var u : List.of(x, y)) { u.spellCenter = spot(6, 0); u.windupUntil = b.time; u.stunnedUntil = 0; Abilities.cloud(b, u, Abilities.row(u), b.time); }
                    long three = b.mists.size() < 2 ? -1 : b.mists.get(0).until() - b.time, one = b.mists.size() < 2 ? -1 : b.mists.get(1).until() - b.time;
                    Phase2Checks.report("p2-output-tier", heals && three == 120 && one == 100, "a level-1 Sunweaver with weapon tier III (output x" + fmt(Veterans.outputFactor(a)) + ") sends a mend of " + (heal == null ? "none" : fmt(heal.amount())) + " (want 5 x " + fmt(b.campaign.damage(a)) + " x 1.2 = " + fmt(want) + "); a level-1 Mist Caller with tier III lays a cloud of " + three + " ticks (want 120), one with tier I " + one + " (want 100)");
                    b.mists.clear(); Projectiles.clear(b); clearFixture(b, "tier"); next(b, 44);
                }
                case 44 -> { // p2-level-row-select: the Order Longbow
                    a = post(b, dawn, "longbow", 0, -10, 0, true); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 0, 2.5); traits();
                    lv(a, 1); var mark = Combat.LAST.peekLast(); blow(b, a, v.mob, "arrow"); var one = Phase2Checks.Gilded.hitOn(w.mob, mark);
                    lv(a, 2); w.mob.timeUntilRegen = 0; mark = Combat.LAST.peekLast(); blow(b, a, v.mob, "arrow"); var two = Phase2Checks.Gilded.hitOn(w.mob, mark); boolean slowTwo = Statuses.has(b, v.mob, "slow");
                    String rows2 = Abilities.row(a).id() + " + " + Abilities.second(a).id();
                    lv(a, 3); w.mob.timeUntilRegen = 0; mark = Combat.LAST.peekLast(); blow(b, a, v.mob, "arrow"); var three = Phase2Checks.Gilded.hitOn(w.mob, mark);
                    var s = v.mob.getStatusEffect(StatusEffects.SLOWNESS);
                    float share = (float) (.7 * Abilities.baseHit(b, a));
                    boolean ok = one == null && two != null && Math.abs(two.base() - share) < 1e-4 && !slowTwo && three != null && Math.abs(three.base() - share) < 1e-4 && s != null && s.getAmplifier() == 0 && s.getDuration() == 30;
                    Phase2Checks.report("p2-level-row-select", ok, "a dummy 2.5 behind the target: at level 1 the pierce (reach 2) misses it=" + (one == null) + "; at level 2 (" + rows2 + ") it takes " + (two == null ? "nothing" : fmt(two.base())) + " (want 0.7 x " + fmt(Abilities.baseHit(b, a)) + " = " + fmt(share) + ") and the target is not slowed=" + !slowTwo + "; at level 3 (" + Abilities.row(a).id() + " + " + Abilities.second(a).id() + ") it takes " + (three == null ? "nothing" : fmt(three.base())) + " and the target has " + st(b, v.mob) + " (want Slowness level 0 for 30)");
                    clearFixture(b, "longbow"); next(b, 46);
                }
                case 46 -> { // p2-commander-status: Marrow arrows on the commander
                    var p = sp(b); if (p == null) return;
                    p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE); b.relocate(p, site.getX() + .5, site.getY(), site.getZ() + .5 - 4, 0, 0); p.setHealth(p.getMaxHealth());
                    a = lv(post(b, dusk, "bonebow", 0, 6, 180, true), 3); v = dummy(b, dusk, 4, 2); traits();
                    double f0 = Commander.playerFactor(b, own, v.mob);
                    p.timeUntilRegen = 0; p.hurtTime = 0;
                    float dealt = Combat.resolve(b, a, p, 1f, "arrow", false);
                    long left = Statuses.remaining(b, p, "weaken");
                    double f1 = Commander.playerFactor(b, own, v.mob);
                    boolean ok = dealt > 0 && left == 40 && Math.abs(f1 / f0 - .85) < 1e-9;
                    Phase2Checks.report("p2-commander-status", ok, "a level-3 Bone Archer's arrow on the seated commander (dealt " + fmt(dealt) + ") weakens him for " + left + " ticks (want 40); his own blow's commander share goes from " + fmt(f0) + " to " + fmt(f1) + " (want x0.85)");
                    p.setHealth(p.getMaxHealth()); p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                    clearFixture(b, "commander"); next(b, 48);
                }
                case 48 -> { // p2-credit-status: each status logs its striker, and a bleed whose striker fell still names his army
                    group.clear();
                    for (int i = 0; i < 5; i++) group.add(post(b, dawn, "revenant", -8 + i * 2.5, -3, 0, true));
                    v = dummy(b, dusk, -3, 0); traits();
                    Statuses.apply(b, group.get(0), v.mob, "weaken", 60, 0, .15); Statuses.apply(b, group.get(1), v.mob, "bleed", 60, 0); Statuses.apply(b, group.get(2), v.mob, "mark", 60, 0, .1);
                    Statuses.apply(b, group.get(3), v.mob, "stun", 20, 0); Statuses.apply(b, group.get(4), v.mob, "knockback", 0, 0, .6);
                    var c = credited(b, v.mob, 0);
                    n0 = 0; for (var u : group) if (c.contains(u.mob.getId())) n0++;
                    x = post(b, dawn, "revenant", 8, -3, 0, true); z = bare(post(b, dusk, "ic_breaker", 8, 0, 180, true)); traits();
                    z.mob.setHealth(1f);
                    Statuses.apply(b, x, z.mob, "bleed", 60, 0);
                    Phase2Checks.Graveborn.fall(b, x);
                    n1 = dawn.stats.kills; flag = !x.mob.isAlive();
                    next(b, 49);
                }
                case 49 -> {
                    if (since(b) < 30) return;
                    boolean fell = !z.mob.isAlive(), named = z.lastHitBy == dawn && "revenant".equals(z.lastHitTroop), kill = dawn.stats.kills == n1 + 1;
                    Phase2Checks.report("p2-credit-status", n0 == 5 && fogCredit && flag && fell && named && kill, "weaken, bleed, mark, stun and knockback laid by five Risen: " + n0 + " of them in the target's credit log (want 5); a Chill fog cloud's beat logged its caster=" + fogCredit + "; a bleed whose striker fell (dead=" + flag + ") killed its 1-health target=" + fell + ", which names his army and troop as the last to hurt it=" + named + ", and the kill went to his army=" + kill);
                    clearFixture(b, "credit"); next(b, 50);
                }
                case 50 -> { // p2-credit-aura: Furnace heart, Admiral's call and the cleanses log their bearer as supporting
                    var heart = lv(post(b, dawn, "ic_colossus", -6, 0, 0, true), 3); v = post(b, dawn, "revenant", -6, 4, 0, true); w = post(b, dawn, "revenant", -6, -9, 0, true);
                    var call = lv(post(b, dawn, "tc_admiral", 6, 0, 0, true), 3); y = post(b, dawn, "revenant", 6, 8, 0, true); traits();
                    Abilities.tick(b, heart, null, b.time); Abilities.tick(b, call, null, b.time);
                    boolean h = supporters(b, v).contains(heart.mob.getId()) && !supporters(b, w).contains(heart.mob.getId()), c = supporters(b, y).contains(call.mob.getId());
                    Phase2Checks.report("p2-credit-aura", h && c && cleanseCredit, "a level-3 Brassjaw's beat logs him as supporting his troop 4 away and not one 9 away=" + h + "; a level-3 Admiral's beat logs her as supporting her troop 8 away=" + c + "; Dawnlight and Rousing howl logged the Sunweaver and the Seer as supporting the troop they cleansed=" + cleanseCredit);
                    clearFixture(b, "aura credit"); next(b, 52);
                }
                case 52 -> { // p2-status-clear-round
                    a = lv(post(b, dawn, "knight", 0, -2, 0, true), 3); v = dummy(b, dusk, 0, 0); traits();
                    Statuses.apply(b, a, v.mob, "weaken", 400, 0, .15); Statuses.apply(b, a, v.mob, "bleed", 400, 0); Statuses.apply(b, a, v.mob, "mark", 400, 0, .1);
                    blow(b, a, v.mob, "melee");
                    boolean before = Statuses.has(b, v.mob, "weaken") && Statuses.has(b, v.mob, "bleed") && Statuses.has(b, v.mob, "mark") && Abilities.SECOND_AT.containsKey(a);
                    b.clearProgression(); // what startRound, Campaign.returnLobby and switchMap call at a round boundary
                    boolean after = !Statuses.has(b, v.mob, "weaken") && !Statuses.has(b, v.mob, "bleed") && !Statuses.has(b, v.mob, "mark") && Abilities.SECOND_AT.isEmpty() && Abilities.RAISED.isEmpty() && near(Statuses.factorOut(b, v.mob), 1) && near(Statuses.markFactor(b, dawn, v.mob), 1);
                    Phase2Checks.report("p2-status-clear-round", before && after, "a weaken, a bleed, a mark and a Shield bash cooldown stand=" + before + "; after Battle.clearProgression (a new round) none does=" + after);
                    clearFixture(b, "clear"); next(b, 99);
                }
                default -> {}
            }
        }
    }
}
