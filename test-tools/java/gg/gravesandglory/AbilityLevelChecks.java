package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.*;
import java.util.function.Supplier;

/**
 * Suites statuses and levels-gilded to levels-tidecallers of the in-match progression plan (Tasks 6 and 7, lane B). Never included in releases.
 * Task 6 builds statuses (StatusChecks): the status engine and the thirteen small mechanics. Task 7 builds the five House suites (HouseSuite): one proof
 * per level row of design 6.2, p2-l2-<troop> and p2-l3-<troop>, and one picture per House (levels-<house>) of a level-3 troop using his second ability.
 * Phase2Checks.SUITES takes these entries through register (GRAVES_PHASE2_CHECKS=<suite>), and the checks print through Phase2Checks.report.
 */
final class AbilityLevelChecks {
    private AbilityLevelChecks() {}

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("statuses", StatusChecks::new);
        suites.put("levels-gilded", GildedLevels::new);
        suites.put("levels-graveborn", GravebornLevels::new);
        suites.put("levels-wildmoot", WildmootLevels::new);
        suites.put("levels-ironclad", IroncladLevels::new);
        suites.put("levels-tidecallers", TidecallersLevels::new);
    }

    /**
     * The bench every suite of this file stands on. Gilded's fixture (Phase2Checks.Staged): a stone platform in the sky, two ad hoc bot armies (dawn, dusk)
     * and an allied army on each side; troops are posted frozen and pinned, each check sets their level (u.level, then Abilities.rowsChanged, as lane A's
     * level up does), clears the armies' House traits so no trait shapes a number, does one thing through the real path (Combat.resolve with its on-hit
     * abilities, Abilities.tick and attack, Statuses.apply) and reads the result: Combat.LAST, the statuses, the kill-credit logs (Battle.credit).
     */
    abstract static class Bench extends Phase2Checks.Staged {
        final String suite;
        GravesAndGlory.Squad ally, duskAlly;
        GravesAndGlory.Unit a, a2, v, w, x, y, z;
        final List<GravesAndGlory.Unit> group = new ArrayList<>(), shields = new ArrayList<>();
        final List<BlockPos> roofs = new ArrayList<>();
        Combat.HitLog h0, h1;
        float hp0, hp1;
        long t0;
        int n0, n1;
        double d0;
        boolean flag;
        String why = "";

        Bench(String suite) { this.suite = suite; }

        @Override void cleared() { group.clear(); shields.clear(); a = a2 = v = w = x = y = z = null; }
        /** The pins, and every shield the check raised kept up (a frozen troop lowers it otherwise). */
        @Override void hold() { super.hold(); for (var u : shields) if (u.mob.isAlive() && !u.mob.isUsingItem()) u.mob.setCurrentHand(Hand.OFF_HAND); }

        /** The soldier at a level, his rows and tags read again (lane A's level up calls the same seam). */
        static GravesAndGlory.Unit lv(GravesAndGlory.Unit u, int level) { u.level = level; Abilities.rowsChanged(u); return u; }
        /** A frozen troop's own cooldown ready and no stun, so the one action the check does may fire (Phase1Checks.freeze set abilityAt to never). */
        static GravesAndGlory.Unit ready(GravesAndGlory.Unit u) { u.abilityAt = 0; u.stunnedUntil = 0; return u; }
        /** No House trait shapes the stage's numbers: the armies' tiers off (b.spawn counts them again with each troop). */
        void traits() { for (var s : new GravesAndGlory.Squad[]{dawn, dusk, ally, duskAlly}) s.traits.clear(); }
        /** A real blow of 1 from the attacker, past the target's hurt cooldown, with its on-hit abilities (Combat.resolve, not secondary). */
        static float blow(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity to, String kind) { to.timeUntilRegen = 0; to.hurtTime = 0; return Combat.resolve(b, from, to, 1f, kind, false); }
        /** A secondary blow of 1 (no on-hit), its HitLog or null (Phase2Checks.Commanders.hit). */
        static Combat.HitLog probe(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity to, String kind) { to.timeUntilRegen = 0; return Phase2Checks.Commanders.hit(b, from, to, kind); }
        /** A dummy target of an army: armour off, the most health a mob may have (tough). */
        GravesAndGlory.Unit dummy(GravesAndGlory.Battle b, GravesAndGlory.Squad s, double dx, double dz) { return tough(bare(post(b, s, "ic_breaker", dx, dz, s.team == 0 ? 0 : 180, true))); }
        /** Who the kill-credit log credits for this victim dying now to the team, with no killer (design 3.2). */
        static Set<Integer> credited(GravesAndGlory.Battle b, LivingEntity victim, int team) { return b.credit.credited(victim.getId(), -1, team, b.time, Veterans.window()); }
        /** The troops logged as supporting this one now (the supporters CreditLog.credited adds for a killer). */
        static Set<Integer> supporters(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return b.credit.credited(-7, u.mob.getId(), u.squad.team, b.time, Veterans.window()); }
        Vec3d spot(double dx, double dz) { return new Vec3d(site.getX() + .5 + dx, site.getY(), site.getZ() + .5 + dz); }
        static double horizontal(Vec3d v) { return Math.hypot(v.x, v.z); }
        static double apart(GravesAndGlory.Unit p, GravesAndGlory.Unit q) { return Math.sqrt(GravesAndGlory.Battle.horizontalDistance(p.mob.getPos(), q.mob.getPos())); }
        static String st(GravesAndGlory.Battle b, LivingEntity e) {
            var s = e.getStatusEffect(StatusEffects.SLOWNESS);
            return "slow " + (s == null ? "none" : "level " + s.getAmplifier() + " for " + s.getDuration()) + ", weaken " + Statuses.remaining(b, e, "weaken") + ", bleed " + Statuses.remaining(b, e, "bleed") + ", mark " + Statuses.remaining(b, e, "mark");
        }
        /** Slowness at exactly this amplifier and duration on the entity. */
        static boolean slowIs(LivingEntity e, int amplifier, int ticks) { var s = e.getStatusEffect(StatusEffects.SLOWNESS); return s != null && s.getAmplifier() == amplifier && s.getDuration() == ticks; }
        /** The HitLog's counters and exactly these rows. */
        static boolean is(Combat.HitLog h, double counters, String... rows) { return h != null && Math.abs(h.counters() - counters) < 1e-9 && h.rows().size() == rows.length && new HashSet<>(h.rows()).equals(Set.of(rows)); }
        static Combat.HitLog mark() { return Combat.LAST.peekLast(); }
        /** The last HitLog on the target after the mark (Phase2Checks.Gilded.hitOn). */
        static Combat.HitLog after(LivingEntity t, Combat.HitLog mark) { return Phase2Checks.Gilded.hitOn(t, mark); }
        static String base(Combat.HitLog h) { return h == null ? "nothing" : fmt(h.base()); }
        static String hit(Combat.HitLog h) { return h == null ? "none" : show(h); }
        /** Whether the soldier reads the rows of his level now: his level-2 row and none at level 2, his level-2 and level-3 rows at level 3 (Abilities.row, second). */
        static boolean picks(GravesAndGlory.Unit u) {
            String two = u.spec.id() + "_l2", three = u.spec.id() + "_l3";
            return Abilities.row(u).id().equals(two) && Abilities.second(u).id().equals(u.level >= 3 ? three : "none");
        }
        static String rows(GravesAndGlory.Unit u) { return Abilities.row(u).id() + " + " + Abilities.second(u).id(); }
        /** The last mend the troop sent that is still in flight (Battle.shots), or null. */
        static Projectiles.Shot mendOf(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { Projectiles.Shot out = null; for (var s : b.shots) if (s.shooter() == u && s.kind().equals("mend")) out = s; return out; }
        /** The standing banner the troop planted, or null. */
        static Abilities.Banner bannerOf(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { for (var bn : b.banners) if (bn.by() == u) return bn; return null; }
        /** The last cloud or burning ground standing, or null. */
        static Abilities.Mist lastMist(GravesAndGlory.Battle b) { return b.mists.isEmpty() ? null : b.mists.get(b.mists.size() - 1); }
        /** Raises the troop's shield and keeps it up (hold). */
        void raise(GravesAndGlory.Unit u) { shields.add(u); u.mob.setCurrentHand(Hand.OFF_HAND); }
        /** A one-block stone roof over the head of a troop standing at (dx, dz): no sky over him (the ambush stance's condition). */
        void roof(GravesAndGlory.Battle b, double dx, double dz) { var p = site.add((int) Math.floor(.5 + dx), 2, (int) Math.floor(.5 + dz)); b.world.setBlockState(p, Blocks.STONE.getDefaultState()); roofs.add(p); }
        void unroof(GravesAndGlory.Battle b) { for (var p : roofs) b.world.setBlockState(p, Blocks.AIR.getDefaultState()); roofs.clear(); }
        /** The commander as a spectator camera at (dx, eye, dz) from the platform's centre, looking at (lx, ly, lz). */
        void look(GravesAndGlory.Battle b, double dx, double eye, double dz, double lx, double ly, double lz) {
            var p = sp(b); if (p == null) return;
            p.changeGameMode(GameMode.SPECTATOR);
            Vec3d cam = spot(dx, dz).add(0, eye, 0), at = spot(lx, lz).add(0, ly, 0);
            double ex = at.x - cam.x, ey = at.y - cam.y, ez = at.z - cam.z;
            b.relocate(p, cam.x, cam.y - 1.62, cam.z, (float) -Math.toDegrees(Math.atan2(ex, ez)), (float) -Math.toDegrees(Math.atan2(ey, Math.hypot(ex, ez))));
        }

        public void cleanup(MinecraftClient c) {
            Phase2Checks.server(c, b -> {
                Houses.forceRise = null; Houses.reset(b); unroof(b); clearFixture(b, "cleanup"); b.mists.clear();
                for (var bn : b.banners) Abilities.lower(b, bn);
                b.banners.clear();
                if (dawn != null) { dawn.order = "hold"; dusk.order = "hold"; }
                if (site != null) platform(b, site, 12, Blocks.AIR.getDefaultState());
                unforce(b);
                if (b.running) b.campaign.returnLobby();
            });
        }

        int limit() { return 12000; }

        public void step(MinecraftClient c, int tick) {
            if (tick == 20) { c.setScreen(null); UUID id = c.player.getUuid(); Phase2Checks.server(c, b -> { player = id; next(b, 1); }); }
            if (wantShot != null && !shotTaken && ++shotDelay >= shotAfter) { c.setScreen(null); Phase2Checks.shot(c, wantShot); shotTaken = true; }
            if (stage == 99 && !finished) { finished = true; c.setScreen(null); Phase2Checks.finish(c, suite); }
            if (tick > limit() && !finished) { finished = true; Phase2Checks.report("p2-" + suite + "-timeout", false, "stuck in stage " + stage + where()); Phase2Checks.finish(c, suite); }
        }

        String where() { return ""; }

        public void serverTick(GravesAndGlory.Battle b) {
            if (player == null || stage >= 99) return;
            try { hold(); if (setup(b)) return; run(b); } catch (RuntimeException e) { Phase2Checks.report("p2-" + suite + "-exception", false, "stage " + stage + where() + ": " + e); e.printStackTrace(); next(b, 99); }
        }

        /** Stages 1 to 6: back to the lobby, a practice round, the sky platform and the four armies. True while setting up. */
        boolean setup(GravesAndGlory.Battle b) {
            switch (stage) {
                case 1 -> { var p = sp(b); if (p == null) return true; if (b.running) { b.campaign.returnLobby(); return true; } next(b, 5); }
                case 5 -> { var p = sp(b); if (p == null || !Phase1Checks.skirmish(b, p, 0, 1, "normal")) return true; next(b, 6); }
                case 6 -> {
                    if (since(b) < 10) return true;
                    stageFixture(b); Statuses.clear(b);
                    var base = GravesAndGlory.Battle.base(0);
                    ally = new GravesAndGlory.Squad(UUID.randomUUID(), 0, base); ally.order = "hold"; ally.bot = true;
                    duskAlly = new GravesAndGlory.Squad(UUID.randomUUID(), 1, base); duskAlly.order = "hold"; duskAlly.bot = true;
                    next(b, 10);
                }
                default -> { return stage < 10; }
            }
            return true;
        }

        abstract void run(GravesAndGlory.Battle b);
    }

    /**
     * Suite statuses (in-match progression Task 6): the status engine (Statuses) and the thirteen small mechanics N1 to N13, with the per-level ability rows
     * (Abilities.row(u), second(u), rowsChanged). Task 7 adds two checks from the Task 6 review: a stunned caster casts nothing (M2) and a bleed tick sends no
     * velocity of its own (M3).
     */
    static final class StatusChecks extends Bench {
        boolean fogCredit, cleanseCredit;

        StatusChecks() { super("statuses"); }

        void run(GravesAndGlory.Battle b) {
            switch (stage) {
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
                    v.mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40); v.mob.setHealth(12f); // exactly 30%: the tough dummy's max is clamped to 1024, whose 30% is no float (307.2f lies above the line)
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
                    var fog = lastMist(b);
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
                    var fire = lastMist(b);
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
                    var heal = mendOf(b, a);
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
                    lv(a, 1); var m = mark(); blow(b, a, v.mob, "arrow"); var one = after(w.mob, m);
                    lv(a, 2); w.mob.timeUntilRegen = 0; m = mark(); blow(b, a, v.mob, "arrow"); var two = after(w.mob, m); boolean slowTwo = Statuses.has(b, v.mob, "slow");
                    String rows2 = rows(a);
                    lv(a, 3); w.mob.timeUntilRegen = 0; m = mark(); blow(b, a, v.mob, "arrow"); var three = after(w.mob, m);
                    var s = v.mob.getStatusEffect(StatusEffects.SLOWNESS);
                    float share = (float) (.7 * Abilities.baseHit(b, a));
                    boolean ok = one == null && two != null && Math.abs(two.base() - share) < 1e-4 && !slowTwo && three != null && Math.abs(three.base() - share) < 1e-4 && s != null && s.getAmplifier() == 0 && s.getDuration() == 30;
                    Phase2Checks.report("p2-level-row-select", ok, "a dummy 2.5 behind the target: at level 1 the pierce (reach 2) misses it=" + (one == null) + "; at level 2 (" + rows2 + ") it takes " + base(two) + " (want 0.7 x " + fmt(Abilities.baseHit(b, a)) + " = " + fmt(share) + ") and the target is not slowed=" + !slowTwo + "; at level 3 (" + rows(a) + ") it takes " + base(three) + " and the target has " + st(b, v.mob) + " (want Slowness level 0 for 30)");
                    clearFixture(b, "longbow"); next(b, 46);
                }
                case 46 -> { // p2-commander-status: Marrow arrows on the commander
                    var p = sp(b); if (p == null) return;
                    p.changeGameMode(GameMode.ADVENTURE); b.relocate(p, site.getX() + .5, site.getY(), site.getZ() + .5 - 4, 0, 0); p.setHealth(p.getMaxHealth());
                    a = lv(post(b, dusk, "bonebow", 0, 6, 180, true), 3); v = dummy(b, dusk, 4, 2); traits();
                    double f0 = Commander.playerFactor(b, own, v.mob);
                    p.timeUntilRegen = 0; p.hurtTime = 0;
                    float dealt = Combat.resolve(b, a, p, 1f, "arrow", false);
                    long left = Statuses.remaining(b, p, "weaken");
                    double f1 = Commander.playerFactor(b, own, v.mob);
                    boolean ok = dealt > 0 && left == 40 && Math.abs(f1 / f0 - .85) < 1e-9;
                    Phase2Checks.report("p2-commander-status", ok, "a level-3 Bone Archer's arrow on the seated commander (dealt " + fmt(dealt) + ") weakens him for " + left + " ticks (want 40); his own blow's commander share goes from " + fmt(f0) + " to " + fmt(f1) + " (want x0.85)");
                    p.setHealth(p.getMaxHealth()); p.changeGameMode(GameMode.SPECTATOR);
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
                    boolean after = !Statuses.has(b, v.mob, "weaken") && !Statuses.has(b, v.mob, "bleed") && !Statuses.has(b, v.mob, "mark") && Statuses.held() == 0 && Abilities.SECOND_AT.isEmpty() && Abilities.RAISED.isEmpty() && near(Statuses.factorOut(b, v.mob), 1) && near(Statuses.markFactor(b, dawn, v.mob), 1);
                    Statuses.apply(b, a, v.mob, "bleed", 400, 0); blow(b, a, v.mob, "melee"); // and the server stopping (SERVER_STOPPING: Abilities.clearBanners) lets every store go
                    Abilities.SECOND_AT.put(a, b.time + 100); Abilities.RAISED.put(a, new ArrayList<>(List.of(v)));
                    boolean laid = Statuses.held() == 1 && Statuses.has(b, v.mob, "bleed") && !Abilities.SECOND_AT.isEmpty() && !Abilities.RAISED.isEmpty();
                    Abilities.clearBanners(b);
                    // has() reads nothing once the store has no owner, so only held() sees ON itself: a release that forgot ON.clear() would keep every statused troop of a stopped world
                    int kept = Statuses.held(), keptCooldowns = Abilities.SECOND_AT.size(), keptCounts = Abilities.RAISED.size();
                    boolean released = kept == 0 && !Statuses.has(b, v.mob, "bleed") && keptCooldowns == 0 && keptCounts == 0;
                    Statuses.clear(b);
                    Statuses.apply(b, a, v.mob, "bleed", 400, 0); // a store claimed by one battle lets go of its troops when another asks (Statuses.on, the owner != b branch; null is a battle that is not b)
                    Abilities.SECOND_AT.put(a, b.time + 100); Abilities.RAISED.put(a, new ArrayList<>(List.of(v)));
                    boolean held = Statuses.held() == 1 && Statuses.has(b, v.mob, "bleed") && !Abilities.SECOND_AT.isEmpty() && !Abilities.RAISED.isEmpty();
                    Statuses.on(null);
                    int heldNow = Statuses.held(), cooldownsNow = Abilities.SECOND_AT.size(), countsNow = Abilities.RAISED.size();
                    boolean swapped = heldNow == 0 && cooldownsNow == 0 && countsNow == 0;
                    Statuses.clear(b);
                    Phase2Checks.report("p2-status-clear-round", before && after && laid && released && held && swapped, "a weaken, a bleed, a mark and a Shield bash cooldown stand=" + before + "; after Battle.clearProgression (a new round) none does and the store is empty=" + after + "; after Abilities.clearBanners (the server stopping) a bleed, a cooldown and a Death knell count laid again stood=" + laid + " and are gone (statused troops kept " + kept + ", cooldowns " + keptCooldowns + ", counts " + keptCounts + ", want 0 each)=" + released + " (review M7); a store holding a bleed, a cooldown and a count=" + held + " lets all three go when another battle asks for it (the owner != b branch: kept " + heldNow + ", " + cooldownsNow + " and " + countsNow + ", want 0 each)=" + swapped);
                    clearFixture(b, "clear"); next(b, 54);
                }
                case 54 -> { // p2-stun-no-cast (review M2): a Shield bash on a Sunweaver and on a Lord Marshal: neither mends nor plants until the stun ends
                    a = ready(lv(post(b, dawn, "mage", -6, -4, 0, true), 1)); v = dummy(b, dawn, -6, -1); v.mob.setHealth(v.mob.getMaxHealth() - 20);
                    x = ready(lv(post(b, dawn, "gl_marshal", 6, -4, 0, true), 1));
                    y = lv(post(b, dusk, "knight", -6, -2.5, 180, true), 3); z = lv(post(b, dusk, "knight", 6, -2.5, 180, true), 3); traits();
                    t0 = b.time;
                    blow(b, y, a.mob, "melee"); blow(b, z, x.mob, "melee");
                    a.abilityAt = 0; x.abilityAt = 0; x.seenOrder = "charge"; // her mend is ready; his group's order turned to Hold since his last beat
                    Abilities.tick(b, a, null, b.time); Abilities.tick(b, x, null, b.time);
                    flag = a.stunnedUntil - t0 == 20 && x.stunnedUntil - t0 == 20 && a.abilityAt == 0 && mendOf(b, a) == null && bannerOf(b, x) == null && "charge".equals(x.seenOrder);
                    why = "a level-3 Squire's bash stuns a Sunweaver for " + (a.stunnedUntil - t0) + " and a Lord Marshal for " + (x.stunnedUntil - t0) + " ticks (want 20 each); their beat in the stun: no mend sent=" + (mendOf(b, a) == null && a.abilityAt == 0) + ", no banner planted=" + (bannerOf(b, x) == null) + ", the Hold he has not seen kept for later=" + "charge".equals(x.seenOrder);
                    w = ready(lv(post(b, dawn, "gl_marshal", 0, -9, 0, true), 1)); traits(); // review M6: a Marshal whose very first beat is in a stun (seenOrder null) remembers the order he finds, so the Hold that follows plants
                    boolean fresh = w.seenOrder == null;
                    dawn.order = "charge"; Statuses.apply(b, null, w.mob, "stun", 20, 0);
                    Abilities.tick(b, w, null, b.time);
                    boolean first = fresh && w.stunnedUntil - t0 == 20 && "charge".equals(w.seenOrder) && bannerOf(b, w) == null;
                    dawn.order = "hold"; // the Hold is given during the stun
                    flag &= first;
                    why += "; a Marshal stunned on his very first beat (fresh=" + fresh + ") remembers the Charge he found=" + first;
                    next(b, 55);
                }
                case 55 -> {
                    if (since(b) == 10) { flag &= a.abilityAt == 0 && bannerOf(b, x) == null; why += "; 10 ticks into the stun still none=" + (a.abilityAt == 0 && bannerOf(b, x) == null); }
                    if (since(b) < 21) return;
                    Abilities.tick(b, a, null, b.time); Abilities.tick(b, x, null, b.time); Abilities.tick(b, w, null, b.time);
                    boolean cast = a.abilityAt > t0 + 20 && bannerOf(b, x) != null && bannerOf(b, w) != null;
                    Phase2Checks.report("p2-stun-no-cast", flag && cast, why + "; after the stun she mends (her cooldown now runs to +" + (a.abilityAt - t0) + "), he plants=" + (bannerOf(b, x) != null) + " and so does the one stunned on his first beat, whose Hold came during the stun=" + (bannerOf(b, w) != null));
                    for (var bn : b.banners) Abilities.lower(b, bn);
                    b.banners.clear(); Projectiles.clear(b); clearFixture(b, "stun cast"); next(b, 56);
                }
                case 56 -> { // p2-bleed-velocity (review M3): a bleed tick on a commander sends no velocity of its own; a troop's knock is taken back
                    var p = sp(b); if (p == null) return;
                    p.changeGameMode(GameMode.ADVENTURE); b.relocate(p, site.getX() + .5, site.getY(), site.getZ() + .5 - 4, 0, 0); p.setHealth(p.getMaxHealth());
                    a = post(b, dusk, "revenant", 0, 4, 180, true); v = dummy(b, dawn, 4, -2); traits();
                    Statuses.apply(b, a, p, "bleed", 200, 0); Statuses.apply(b, a, v.mob, "bleed", 200, 0);
                    var sp = Statuses.get(b, p, "bleed"); var sv = Statuses.get(b, v.mob, "bleed");
                    Vec3d run = new Vec3d(.3, 0, 0);
                    p.timeUntilRegen = 0; p.hurtTime = 0; p.setVelocity(run); p.velocityModified = false; float hp = p.getHealth();
                    if (sp != null) Statuses.bleed(b, p, sp);
                    boolean quiet = !p.velocityModified && p.getVelocity().distanceTo(run) < 1e-9, hurt = p.getHealth() < hp;
                    p.timeUntilRegen = 0; p.setVelocity(run); p.velocityModified = true;
                    if (sp != null) Statuses.bleed(b, p, sp);
                    boolean due = p.velocityModified && p.getVelocity().distanceTo(run) < 1e-9;
                    v.mob.timeUntilRegen = 0; v.mob.setVelocity(Vec3d.ZERO);
                    if (sv != null) Statuses.bleed(b, v.mob, sv);
                    boolean still = horizontal(v.mob.getVelocity()) < 1e-9;
                    Phase2Checks.report("p2-bleed-velocity", sp != null && sv != null && quiet && hurt && due && still, "a bleed tick from a live Risen on the seated commander hurts him=" + hurt + " and leaves his server velocity as it was with no velocity update sent=" + quiet + "; one with a knock already due this tick still sends it=" + due + "; on a troop the blow's knock is taken back=" + still);
                    p.setHealth(p.getMaxHealth()); p.changeGameMode(GameMode.SPECTATOR);
                    Statuses.clear(b); clearFixture(b, "bleed velocity"); next(b, 99);
                }
                default -> {}
            }
        }
    }

    /**
     * A House suite of Task 7: one proof per level row of the House's six troops (design 6.2), p2-l2-<troop> and p2-l3-<troop>, then the House's picture
     * (levels-<house>). Each check posts its troops at a level against fixed targets, does one action through the real path and asserts the row's number
     * (in Combat.LAST, the statuses, the troop's cooldown or the cast it made) and that the soldier reads the rows of his level (picks). Between checks the
     * fixture, the statuses, the clouds and the banners go, and both armies stand on Hold again.
     */
    abstract static class HouseSuite extends Bench {
        interface Check { boolean run(GravesAndGlory.Battle b); }
        final List<String> ids = new ArrayList<>();
        final List<Check> checks = new ArrayList<>();
        int at, phase;

        HouseSuite(String suite) { super(suite); }

        void add(String id, Check c) { ids.add(id); checks.add(c); }
        @Override String where() { return at < ids.size() ? " (" + ids.get(at) + ", phase " + phase + ")" : ""; }

        void run(GravesAndGlory.Battle b) {
            if (stage != 10) return;
            if (at >= checks.size()) { next(b, 99); return; }
            boolean done;
            try { done = checks.get(at).run(b); } catch (RuntimeException e) { Phase2Checks.report(ids.get(at), false, "threw " + e); e.printStackTrace(); done = true; }
            if (!done) return;
            tidy(b, ids.get(at)); at++; phase = 0; stageAt = b.time;
        }

        void tidy(GravesAndGlory.Battle b, String where) {
            unroof(b); b.mists.clear(); Projectiles.clear(b);
            for (var bn : b.banners) Abilities.lower(b, bn);
            b.banners.clear();
            for (var s : new GravesAndGlory.Squad[]{dawn, dusk, ally, duskAlly}) s.order = "hold";
            Houses.forceRise = null; Houses.reset(b); Statuses.clear(b); clearFixture(b, where);
            wantShot = null;
        }

        // ---- check shapes shared by more than one troop ----

        /** Better guard (guard2): the troop's raised shield takes an arrow x0.30 (C1g for C1 0.35) and a blow x0.5 (C3g for C3 0.6) at level 2. */
        Check guard(String troop) {
            return b -> {
                if (phase == 0) { v = tough(bare(post(b, dusk, troop, 0, 0, 180, true))); raise(v); a = post(b, dawn, "archer", 0, -8, 0, true); x = post(b, dawn, "revenant", 0, -2, 0, true); traits(); phase = 1; return false; }
                if (since(b) < 10) return false; // the shield has been up ten ticks
                dawn.order = "attack"; // the Archer's Steady (C18a, on Hold) stays out of the numbers
                lv(v, 1); var s1 = probe(b, a, v.mob, "arrow"); var m1 = probe(b, x, v.mob, "melee");
                lv(v, 2); boolean p = picks(v); var s2 = probe(b, a, v.mob, "arrow"); var m2 = probe(b, x, v.mob, "melee");
                boolean up = v.mob.isUsingItem(), ok = up && p && is(s1, .35, "C1") && is(m1, .6, "C3") && is(s2, .3, "C1g") && is(m2, .5, "C3g");
                Phase2Checks.report("p2-l2-" + troop, ok, "a " + v.spec.name() + " with his shield up=" + up + ": at level 1 an arrow " + hit(s1) + " (want 0.35, C1) and a blow " + hit(m1) + " (want 0.6, C3); at level 2 (" + rows(v) + ", tags " + v.tags + ")=" + p + " an arrow " + hit(s2) + " (want 0.30, C1g) and a blow " + hit(m2) + " (want 0.5, C3g)");
                return true;
            };
        }

        /** Fearless improved: at 40% of max health the level-2 soldier deals x1.3 (stance), the level-1 one x1.0 (his share is 30%: x1.2 at 30%). */
        Check fearless(String troop, String kind) {
            return b -> {
                a = post(b, dawn, troop, 0, kind.equals("melee") ? -2 : -8, 0, true); v = dummy(b, dusk, 0, 0); traits();
                a.mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40); a.mob.setHealth(16f);
                lv(a, 1); var one40 = probe(b, a, v.mob, kind);
                lv(a, 2); boolean p = picks(a); var two40 = probe(b, a, v.mob, kind);
                a.mob.setHealth(16.5f); var two41 = probe(b, a, v.mob, kind);
                lv(a, 1); a.mob.setHealth(12f); var one30 = probe(b, a, v.mob, kind);
                boolean ok = p && one40 != null && near(one40.stance(), 1) && two40 != null && near(two40.stance(), 1.3) && two41 != null && near(two41.stance(), 1) && one30 != null && near(one30.stance(), 1.2);
                Phase2Checks.report("p2-l2-" + troop, ok, "a " + a.spec.name() + " of 40 health (level 2 rows " + (p ? "read" : "NOT read") + "): at 16 (40%) level 1 deals stance x" + (one40 == null ? "none" : fmt(one40.stance())) + " (want 1.0), level 2 x" + (two40 == null ? "none" : fmt(two40.stance())) + " (want 1.3); at 16.5 level 2 x" + (two41 == null ? "none" : fmt(two41.stance())) + " (want 1.0); at 12 (30%) level 1 x" + (one30 == null ? "none" : fmt(one30.stance())) + " (want 1.2)");
                return true;
            };
        }

        /**
         * A second hit (pierce, cleave or slam) from one blow at the level: the dummy at (dx, dz) takes share x the base hit; at level `without` it takes `shareWithout` (0: none).
         * reach is the row's stated radius at the level. A second dummy stands on the same line half a block past it and is the only other enemy of a first blow at the level, so a
         * row whose radius grew past the sheet's hits it (the nearest other enemy is the one a pierce or cleave takes, so a nearer dummy would hide it; a slam hits both).
         */
        Check share(String id, String troop, int level, String kind, double share, double reach, double dx, double dz, int without, double shareWithout, String what) {
            return b -> {
                boolean ranged = !kind.equals("melee");
                a = ready(post(b, dawn, troop, 0, ranged ? -9 : -2, 0, true)); v = dummy(b, dusk, 0, 0);
                double past = (reach + .5) / Math.hypot(dx, dz);
                y = dummy(b, dusk, dx * past, dz * past); traits();
                lv(a, level); boolean p = picks(a); var m = mark(); blow(b, a, v.mob, kind); var hy = after(y.mob, m);
                w = dummy(b, dusk, dx, dz); traits();
                ready(a); lv(a, without); v.mob.timeUntilRegen = 0; m = mark(); blow(b, a, v.mob, kind); var hw0 = after(w.mob, m);
                ready(a); lv(a, level); v.mob.timeUntilRegen = 0; w.mob.timeUntilRegen = 0; m = mark(); blow(b, a, v.mob, kind); var hw = after(w.mob, m);
                float want = (float) (share * Abilities.baseHit(b, a)), wantWithout = (float) (shareWithout * Abilities.baseHit(b, a));
                boolean before = shareWithout <= 0 ? hw0 == null : hw0 != null && Math.abs(hw0.base() - wantWithout) < 1e-4;
                boolean ok = p && hy == null && hw != null && Math.abs(hw.base() - want) < 1e-4 && before;
                Phase2Checks.report(id, ok, what + ": the dummy " + fmt(Math.hypot(dx, dz)) + " from the target takes " + base(hw) + " at level " + level + " (" + rows(a) + ")=" + p + " (want " + fmt(share) + " x " + fmt(Abilities.baseHit(b, a)) + " = " + fmt(want) + "); at level " + without + " " + base(hw0) + " (want " + (shareWithout <= 0 ? "nothing" : fmt(wantWithout)) + "); a dummy " + fmt(reach + .5) + " from it (past the reach " + fmt(reach) + "), the only other enemy of a blow at level " + level + ", is hit=" + (hy != null) + " (want no)");
                return true;
            };
        }

        /** An on-hit Slowness at the level: exactly the amplifier and duration on the target after one hit. */
        Check slows(String id, String troop, int level, String kind, int amplifier, int ticks, long cooldown, String what) {
            return b -> {
                a = ready(lv(post(b, dawn, troop, 0, kind.equals("melee") ? -2 : -8, 0, true), level)); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(a); t0 = b.time; blow(b, a, v.mob, kind);
                long cd = a.abilityAt - t0;
                boolean ok = p && slowIs(v.mob, amplifier, ticks) && (cooldown <= 0 || cd == cooldown);
                Phase2Checks.report(id, ok, what + ": a level-" + level + " " + a.spec.name() + " (" + rows(a) + ")=" + p + " leaves " + st(b, v.mob) + " (want Slowness level " + amplifier + " for " + ticks + ")" + (cooldown > 0 ? "; his row waits " + cd + " ticks (want " + cooldown + ")" : ""));
                return true;
            };
        }

        /** A level-3 on-hit status (bleed, weaken) for its ticks, with weaken's share. */
        Check lays(String id, String troop, String kind, String status, int ticks, double amount, String what) {
            return b -> {
                a = ready(lv(post(b, dawn, troop, 0, kind.equals("melee") ? -2 : -8, 0, true), 3)); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(a); blow(b, a, v.mob, kind);
                long left = Statuses.remaining(b, v.mob, status);
                boolean share = !status.equals("weaken") || near(Statuses.factorOut(b, v.mob), 1 - amount);
                boolean credit = credited(b, v.mob, 0).contains(a.mob.getId());
                Phase2Checks.report(id, p && left == ticks && share && credit, what + ": a level-3 " + a.spec.name() + " (" + rows(a) + ")=" + p + " leaves " + st(b, v.mob) + " (want " + status + " for " + ticks + ")" + (status.equals("weaken") ? ", the target's blows x" + fmt(Statuses.factorOut(b, v.mob)) + " (want " + fmt(1 - amount) + ")" : "") + "; the striker in its credit log=" + credit);
                return true;
            };
        }

        /** A counter row that a level grants the attacker: at the lower level the base row (or none), at the level the new one. order is both armies' order for it ("charge" on the target's side makes it charging). */
        Check counter(String id, String troop, String kind, int low, double lowCounters, String lowRow, int level, double counters, String row, String targetTroop, boolean chargingTarget, String what) {
            return b -> {
                a = post(b, dawn, troop, 0, kind.equals("melee") ? -2 : -8, 0, true);
                v = targetTroop == null ? dummy(b, dusk, 0, 0) : tough(bare(post(b, dusk, targetTroop, 0, 0, 180, true))); traits();
                if (chargingTarget) dusk.order = "charge";
                lv(a, low); var h1 = probe(b, a, v.mob, kind);
                lv(a, level); boolean p = picks(a); var h2 = probe(b, a, v.mob, kind);
                boolean ok = p && (lowRow == null ? h1 != null && h1.rows().isEmpty() && near(h1.counters(), lowCounters) : is(h1, lowCounters, lowRow)) && is(h2, counters, row);
                Phase2Checks.report(id, ok, what + ": at level " + low + " " + hit(h1) + " (want " + fmt(lowCounters) + (lowRow == null ? ", no row" : ", " + lowRow) + "); at level " + level + " (" + rows(a) + ", tags " + a.tags + ")=" + p + " " + hit(h2) + " (want " + fmt(counters) + ", " + row + ")");
                return true;
            };
        }

        /** Thick hide (thickhide, C19): an arrow on the level-3 troop x0.75, a blow on it and an arrow on the level-2 one not. */
        Check thickhide(String id, String troop, String what) {
            return b -> {
                a = post(b, dawn, "bonebow", 0, -8, 0, true); x = post(b, dawn, "revenant", 0, -3, 0, true);
                v = lv(tough(post(b, dusk, troop, -3, 0, 180, true)), 3); w = lv(tough(post(b, dusk, troop, 4, 0, 180, true)), 2); traits();
                boolean p = picks(v);
                var shot = probe(b, a, v.mob, "arrow"); var bite = probe(b, x, v.mob, "melee"); var two = probe(b, a, w.mob, "arrow");
                boolean ok = p && is(shot, .75, "C19") && bite != null && !bite.rows().contains("C19") && two != null && !two.rows().contains("C19");
                Phase2Checks.report(id, ok, what + ": a level-3 " + v.spec.name() + " (" + rows(v) + ", tags " + v.tags + ")=" + p + ": an arrow " + hit(shot) + " (want 0.75, C19); a blow " + hit(bite) + " (want no C19); an arrow on a level-2 one " + hit(two) + " (want no C19)");
                return true;
            };
        }

        /** A cast at the level through the real path: attack (the windup and the cooldown it sets), the windup cut short, then the beat (Abilities.tick) that bursts. */
        long[] cast(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity at) {
            ready(u); long t = b.time;
            Abilities.attack(b, u, at, t);
            long[] out = {u.windupUntil - t, u.abilityAt - t};
            u.windupUntil = t;
            Abilities.tick(b, u, null, t);
            return out;
        }

        /** The House's picture: scene sets the troops and the camera at phase 0; beat runs every tick while the picture waits; seen says the second ability acted. */
        Check picture(String house, java.util.function.Consumer<GravesAndGlory.Battle> scene, java.util.function.Consumer<GravesAndGlory.Battle> beat, java.util.function.Predicate<GravesAndGlory.Battle> seen, String what) {
            return b -> {
                if (phase == 0) { n0 = 0; n1 = 0; flag = false; scene.accept(b); requestShot("levels-" + house, 30); phase = 1; return false; }
                beat.accept(b);
                if (!flag && seen.test(b)) flag = true;
                if (!shotTaken && since(b) < 300) return false;
                Phase2Checks.report("p2-levels-" + house + "-shot", shotTaken && flag, what + "; the second ability acted=" + flag + "; the picture levels-" + house + " was taken=" + shotTaken);
                wantShot = null;
                return true;
            };
        }

        /** A dummy pinned facing the given yaw (the dummy helper faces its army's front). */
        GravesAndGlory.Unit facing(GravesAndGlory.Battle b, GravesAndGlory.Squad s, double dx, double dz, float yaw) { return tough(bare(post(b, s, "ic_breaker", dx, dz, yaw, true))); }
    }

    /** Suite levels-gilded: the Gilded Order's twelve level rows (design 6.2) and its picture (a level-3 Order Squire's Shield bash and a Lord Marshal's sweep). */
    static final class GildedLevels extends HouseSuite {
        GildedLevels() {
            super("levels-gilded");
            add("p2-l2-knight", guard("knight"));
            add("p2-l3-knight", b -> { // Shield bash: knockback 1.0 and a stun of 20, every 100
                a = lv(post(b, dawn, "knight", 0, -2, 0, true), 3); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(a); n0 = Statuses.knocks; t0 = b.time;
                blow(b, a, v.mob, "melee");
                long stun = v.stunnedUntil - t0, cd = Abilities.SECOND_AT.getOrDefault(a, 0L) - t0;
                boolean ok = p && stun == 20 && cd == 100 && Statuses.knocks - n0 == 1 && near(Statuses.lastKnock, 1);
                Phase2Checks.report("p2-l3-knight", ok, "Shield bash: a level-3 Order Squire (" + rows(a) + ")=" + p + " stuns his target for " + stun + " ticks (want 20), throws it at " + fmt(Statuses.lastKnock) + " (want 1.0, " + (Statuses.knocks - n0) + " knock) and waits " + cd + " ticks (want 100)");
                return true;
            });
            add("p2-l2-archer", counter("p2-l2-archer", "archer", "arrow", 1, 1.2, "C18a", 2, 1.3, "C18a2", null, false, "Steady +20% to +30% on Hold"));
            add("p2-l3-archer", b -> { // Bodkin points: x0.6 (C15b) in place of C1 against a raised shield
                if (phase == 0) { v = tough(bare(post(b, dusk, "knight", 0, 0, 180, true))); raise(v); a = post(b, dawn, "archer", 0, -8, 0, true); traits(); phase = 1; return false; }
                if (since(b) < 10) return false;
                dawn.order = "attack"; // Steady (on Hold) out of the numbers
                lv(a, 2); var h2 = probe(b, a, v.mob, "arrow");
                lv(a, 3); boolean p = picks(a); var h3 = probe(b, a, v.mob, "arrow");
                boolean up = v.mob.isUsingItem(), ok = up && p && is(h2, .35, "C1") && is(h3, .6, "C15b");
                Phase2Checks.report("p2-l3-archer", ok, "Bodkin points: against a raised shield (up=" + up + ") a level-2 Order Archer's arrow " + hit(h2) + " (want 0.35, C1); a level-3 one's (" + rows(a) + ", tags " + a.tags + ")=" + p + " " + hit(h3) + " (want 0.6, C15b)");
                return true;
            });
            add("p2-l2-sentinel", guard("sentinel"));
            add("p2-l3-sentinel", b -> { // Unbroken: at or below 30% of max health x0.8 taken (C21)
                v = tough(bare(post(b, dusk, "sentinel", 0, 0, 180, true))); x = post(b, dawn, "revenant", 0, -2, 0, true); traits();
                lv(v, 3); boolean p = picks(v); String three = rows(v) + ", tags " + v.tags; var whole = probe(b, x, v.mob, "melee");
                v.mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40); v.mob.setHealth(12.5f);
                var above = probe(b, x, v.mob, "melee"); // 31% of his health: not wounded yet, so a wounded share above 0.3 shows
                v.mob.setHealth(12f);
                var low = probe(b, x, v.mob, "melee");
                lv(v, 2); var two = probe(b, x, v.mob, "melee");
                boolean ok = p && whole != null && !whole.rows().contains("C21") && near(whole.counters(), 1) && above != null && !above.rows().contains("C21") && near(above.counters(), 1) && is(low, .8, "C21") && two != null && !two.rows().contains("C21") && near(two.counters(), 1);
                Phase2Checks.report("p2-l3-sentinel", ok, "Unbroken: a level-3 Templar (" + three + ")=" + p + " whole takes " + hit(whole) + " (want 1.0, no row); at 12.5 of 40 (31%) " + hit(above) + " (want 1.0, no row); at 12 of 40 (30%) " + hit(low) + " (want 0.8, C21); a level-2 one at 30% " + hit(two) + " (want 1.0, no row)");
                return true;
            });
            add("p2-l2-longbow", share("p2-l2-longbow", "longbow", 2, "arrow", .7, 3, 0, 2.5, 1, 0, "Pierce 50% to 70%, reach 2 to 3"));
            add("p2-l3-longbow", slows("p2-l3-longbow", "longbow", 3, "arrow", 0, 30, 0, "Pinning shot: Slowness I for 30"));
            add("p2-l2-mage", b -> { // Mend: heal 5 to 7
                a = ready(lv(post(b, dawn, "mage", 0, -4, 0, true), 2)); v = dummy(b, dawn, 0, 2); v.mob.setHealth(v.mob.getMaxHealth() - 20); traits();
                boolean p = picks(a); t0 = b.time;
                Abilities.tick(b, a, null, t0);
                var s = mendOf(b, a);
                double want = 7 * b.campaign.damage(a) * Veterans.outputFactor(a);
                boolean ok = p && s != null && s.target() == v.mob && Math.abs(s.amount() - want) < 1e-4 && a.abilityAt - t0 == 80;
                Phase2Checks.report("p2-l2-mage", ok, "Mend: a level-2 Sunweaver (" + rows(a) + ")=" + p + " sends a heal of " + (s == null ? "none" : fmt(s.amount())) + " (want 7 x " + fmt(b.campaign.damage(a)) + " x her output " + fmt(Veterans.outputFactor(a)) + " = " + fmt(want) + ") and waits " + (a.abilityAt - t0) + " ticks (want 80)");
                return true;
            });
            add("p2-l3-mage", b -> { // Dawnlight: the mend also cleanses slow, weaken and bleed
                a = ready(lv(post(b, dawn, "mage", 0, -4, 0, true), 3)); v = dummy(b, dawn, 0, 2); z = post(b, dusk, "revenant", 0, 9, 180, true); traits();
                v.mob.setHealth(v.mob.getMaxHealth() - 20);
                Statuses.apply(b, z, v.mob, "slow", 200, 0); Statuses.apply(b, z, v.mob, "weaken", 200, 0, .15); Statuses.apply(b, z, v.mob, "bleed", 200, 0);
                boolean p = picks(a); String was = st(b, v.mob);
                Abilities.tick(b, a, null, b.time);
                boolean sent = mendOf(b, a) != null, clean = !Statuses.has(b, v.mob, "slow") && !Statuses.has(b, v.mob, "weaken") && !Statuses.has(b, v.mob, "bleed");
                Phase2Checks.report("p2-l3-mage", p && sent && clean, "Dawnlight: a level-3 Sunweaver (" + rows(a) + ")=" + p + " mends (sent=" + sent + ") an ally with " + was + " and leaves " + st(b, v.mob) + " (want none of the three)");
                return true;
            });
            add("p2-l2-gl_marshal", b -> { // Oath Banner: 300 to 400 ticks, +10% to +15%
                a = ready(lv(post(b, dawn, "gl_marshal", 0, -2, 0, true), 2)); traits();
                boolean p = picks(a); a.seenOrder = "charge"; t0 = b.time; // his group's order turned to Hold since his last beat
                Abilities.tick(b, a, null, t0);
                var bn = bannerOf(b, a);
                boolean ok = p && bn != null && bn.until() - t0 == 400 && near(bn.amount(), .15) && near(bn.radius(), 12) && a.abilityAt - t0 == 600;
                Phase2Checks.report("p2-l2-gl_marshal", ok, "Oath Banner: a level-2 Lord Marshal (" + rows(a) + ")=" + p + " plants a banner " + (bn == null ? "(none)" : "for " + (bn.until() - t0) + " ticks at +" + fmt(bn.amount()) + " within " + fmt(bn.radius())) + " (want 400, +0.15, 12) and waits " + (a.abilityAt - t0) + " (want 600)");
                return true;
            });
            add("p2-l3-gl_marshal", share("p2-l3-gl_marshal", "gl_marshal", 3, "melee", .5, 2, 1.5, 0, 2, 0, "Marshal's sweep: a cleave of 50% within 2"));
            add("p2-levels-gilded-shot", picture("gilded", b -> {
                a = lv(post(b, dawn, "knight", -2, -1.6, 0, true), 3); v = dummy(b, dusk, -2, 0);
                x = lv(post(b, dawn, "gl_marshal", 2.5, -1.8, 0, true), 3); w = dummy(b, dusk, 2.5, 0); y = dummy(b, dusk, 3.8, .6);
                a2 = lv(post(b, dawn, "longbow", .3, -5, 0, true), 3); z = dummy(b, dusk, .3, 2.5); traits(); // the bash and the sweep show no effect of their own; the Pinning shot's Slowness swirls do
                look(b, -4, 4, -8, .5, 1, 0);
            }, b -> {
                if (since(b) % 6 != 0) return;
                Abilities.SECOND_AT.remove(a); blow(b, a, v.mob, "melee"); if (v.stunnedUntil > b.time) n0++;
                var m = mark(); blow(b, x, w.mob, "melee"); if (after(y.mob, m) != null) n1++;
                blow(b, a2, z.mob, "arrow");
            }, b -> n0 > 0 && n1 > 0 && z.mob.hasStatusEffect(StatusEffects.SLOWNESS),
                "a level-3 Order Squire bashes his target every 6 ticks, a level-3 Lord Marshal's blows sweep a second enemy and a level-3 Order Longbow's Pinning shot slows his"));
        }
    }

    /** Suite levels-graveborn: the Graveborn's twelve level rows and its picture (a level-3 Gravecaller's Death knell raising Risen, a level-3 Risen's Rotten blade). */
    static final class GravebornLevels extends HouseSuite {
        GravebornLevels() {
            super("levels-graveborn");
            add("p2-l2-revenant", fearless("revenant", "melee"));
            add("p2-l3-revenant", lays("p2-l3-revenant", "revenant", "melee", "bleed", 80, 0, "Rotten blade: hits bleed for 80"));
            add("p2-l2-bonebow", fearless("bonebow", "arrow"));
            add("p2-l3-bonebow", lays("p2-l3-bonebow", "bonebow", "arrow", "weaken", 40, .15, "Marrow arrows: hits weaken for 40"));
            add("p2-l2-reaper", share("p2-l2-reaper", "reaper", 2, "melee", .77, 2, 1.5, 0, 1, .57, "Cleave 57% to 77%"));
            add("p2-l3-reaper", b -> { // Reap: a killing blow heals 25% of max health
                a = lv(post(b, dawn, "reaper", -3, -2, 0, true), 3); x = lv(post(b, dawn, "reaper", 3, -2, 0, true), 2); traits();
                boolean p = picks(a);
                for (var u : List.of(a, x)) u.mob.setHealth(u.mob.getMaxHealth() * .5f);
                Abilities.onKill(b, a); Abilities.onKill(b, x);
                float sa = a.mob.getHealth() / a.mob.getMaxHealth(), sx = x.mob.getHealth() / x.mob.getMaxHealth();
                Phase2Checks.report("p2-l3-reaper", p && Math.abs(sa - .75) < 1e-3 && Math.abs(sx - .5) < 1e-3, "Reap: a level-3 Grave Reaper (" + rows(a) + ")=" + p + " at half health lands a killing blow (Abilities.onKill) and stands at " + fmt(sa) + " (want 0.75); a level-2 one at " + fmt(sx) + " (want 0.50)");
                return true;
            });
            add("p2-l2-stalker", slows("p2-l2-stalker", "stalker", 2, "arrow", 0, 60, 0, "Frostshot: Slowness I 40 to 60"));
            add("p2-l3-stalker", share("p2-l3-stalker", "stalker", 3, "arrow", .4, 2, 0, 1.5, 2, 0, "Ice lance: a pierce of 40% within 2 behind"));
            add("p2-l2-hexer", b -> { // Blight: radius 2.5 to 3.0, slow 40 to 60
                a = lv(post(b, dawn, "hexer", 0, -10, 0, true), 2); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 2.8, 0); y = dummy(b, dusk, 3.3, 0); traits();
                boolean p = picks(a); var m = mark();
                long[] c = cast(b, a, v.mob);
                var hv = after(v.mob, m); var hw = after(w.mob, m); var hy = after(y.mob, m);
                boolean ok = p && c[0] == 20 && c[1] == 100 && hv != null && hw != null && hy == null && slowIs(w.mob, 0, 60);
                Phase2Checks.report("p2-l2-hexer", ok, "Blight: a level-2 Gravecaller (" + rows(a) + ")=" + p + " winds up " + c[0] + " and waits " + c[1] + " (want 20, 100); her burst hits the target=" + (hv != null) + " and a dummy 2.8 off=" + (hw != null) + " (want both) and not one 3.3 off=" + (hy == null) + "; the 2.8 one has " + st(b, w.mob) + " (want Slowness level 0 for 60)");
                return true;
            });
            add("p2-l3-hexer", b -> { // Death knell: her blight's kills rise as temporary Risen of her army, 15 s, at most 2
                a = lv(post(b, dawn, "hexer", 0, -10, 0, true), 3); traits();
                for (double[] at : new double[][]{{0, 0}, {1.2, .6}, {-1.2, .6}}) { var u = bare(post(b, dusk, "knight", at[0], at[1], 180, true)); u.mob.setHealth(1f); group.add(u); }
                traits();
                boolean p = picks(a); t0 = b.time;
                cast(b, a, group.get(0).mob);
                boolean killed = group.stream().noneMatch(u -> u.mob.isAlive());
                var raised = b.units.stream().filter(u -> u.temporary && u.squad == dawn && u.mob.isAlive()).toList();
                boolean timed = raised.stream().allMatch(u -> u.expiresAt == t0 + 300 && u.spec.id().equals(Commander.RISEN));
                Phase2Checks.report("p2-l3-hexer", p && killed && raised.size() == 2 && timed, "Death knell: a level-3 Gravecaller (" + rows(a) + ")=" + p + " kills three Squires with her blight=" + killed + " and " + raised.size() + " rise as Risen of her army (want 2) for 300 ticks=" + timed);
                return true;
            });
            add("p2-l2-gb_hollowking", b -> { // Dread: radius 10 to 12, enemy damage -15% to -20%
                var king = post(b, dusk, "gb_hollowking", 0, 6, 180, true);
                a = post(b, dawn, "revenant", 0, -5, 0, true); x = post(b, dawn, "revenant", 0, -3, 0, true); y = post(b, dawn, "revenant", 0, -6.5, 0, true); v = dummy(b, dusk, 2, 0); traits();
                lv(king, 1); var far1 = probe(b, a, v.mob, "melee"); var near1 = probe(b, x, v.mob, "melee"); var past1 = probe(b, y, v.mob, "melee");
                lv(king, 2); boolean p = picks(king); var far2 = probe(b, a, v.mob, "melee"); var near2 = probe(b, x, v.mob, "melee"); var past2 = probe(b, y, v.mob, "melee");
                boolean ok = p && far1 != null && near(far1.auras(), 1) && near1 != null && near(near1.auras(), .85) && past1 != null && near(past1.auras(), 1) && far2 != null && near(far2.auras(), .8) && near2 != null && near(near2.auras(), .8) && past2 != null && near(past2.auras(), 1);
                Phase2Checks.report("p2-l2-gb_hollowking", ok, "Dread: an enemy Risen 11 from a level-1 Hollow King deals auras x" + (far1 == null ? "none" : fmt(far1.auras())) + " (want 1: outside 10), one 9 from him x" + (near1 == null ? "none" : fmt(near1.auras())) + " (want 0.85), one 12.5 from him x" + (past1 == null ? "none" : fmt(past1.auras())) + " (want 1); at level 2 (" + rows(king) + ")=" + p + " x" + (far2 == null ? "none" : fmt(far2.auras())) + " and x" + (near2 == null ? "none" : fmt(near2.auras())) + " (want 0.8 each: within 12), the one 12.5 away x" + (past2 == null ? "none" : fmt(past2.auras())) + " (want 1: past 12)");
                return true;
            });
            add("p2-l3-gb_hollowking", b -> { // Undying King: rises once at 50%
                x = lv(post(b, dawn, "gb_hollowking", -5, 0, 0, true), 2); a = lv(post(b, dawn, "gb_hollowking", 5, 0, 0, true), 3); traits();
                boolean p = picks(a);
                Phase2Checks.Graveborn.fall(b, x); Phase2Checks.Graveborn.fall(b, a);
                boolean two = !x.mob.isAlive(), rose = a.mob.isAlive() && a.rose, half = Math.abs(a.mob.getHealth() / a.mob.getMaxHealth() - .5) < 1e-3;
                Phase2Checks.Graveborn.fall(b, a);
                boolean once = !a.mob.isAlive();
                Phase2Checks.report("p2-l3-gb_hollowking", p && two && rose && half && once, "Undying King: a fall kills a level-2 Hollow King=" + two + "; a level-3 one (" + rows(a) + ")=" + p + " rises=" + rose + " at half health=" + half + ", once=" + once);
                return true;
            });
            add("p2-levels-graveborn-shot", picture("graveborn", b -> {
                a = lv(post(b, dawn, "hexer", 0, -8, 0, true), 3);
                for (double[] at : new double[][]{{0, 0}, {1.4, .8}, {-1.4, .8}}) { var u = bare(post(b, dusk, "knight", at[0], at[1], 180, true)); u.mob.setHealth(1f); group.add(u); }
                x = lv(post(b, dawn, "revenant", 4.5, -1.6, 0, true), 3); v = dummy(b, dusk, 4.5, 0); traits();
                cast(b, a, group.get(0).mob);
                look(b, -5, 3.5, -7, 1.5, 1, 0);
            }, b -> { if (since(b) % 10 == 0) blow(b, x, v.mob, "melee"); }, b -> b.units.stream().filter(u -> u.temporary && u.squad == dawn && u.mob.isAlive()).count() == 2 && Statuses.has(b, v.mob, "bleed"),
                "a level-3 Gravecaller's blight killed three Squires and two rose as her Risen; a level-3 Risen's Rotten blade bleeds a dummy"));
        }
    }

    /** Suite levels-wildmoot: the Wildmoot's twelve level rows and its picture (a level-3 Bloodhowl Seer's Rousing howl cleansing his army). */
    static final class WildmootLevels extends HouseSuite {
        WildmootLevels() {
            super("levels-wildmoot");
            add("p2-l2-wm_hound", b -> { // Pounce: x1.8 to x2.0, cooldown 100 to 80
                a = ready(lv(post(b, dawn, "wm_hound", 0, -2, 0, true), 1)); v = dummy(b, dusk, 0, 0); traits();
                a.travelSinceAttack = 7; var one = probe(b, a, v.mob, "melee");
                lv(a, 2); boolean p = picks(a); a.travelSinceAttack = 7; var two = probe(b, a, v.mob, "melee");
                t0 = b.time; blow(b, a, v.mob, "melee"); long cd = a.abilityAt - t0;
                boolean ok = p && one != null && near(one.auras(), 1.8) && two != null && near(two.auras(), 2.0) && cd == 80 && a.travelSinceAttack == 0;
                Phase2Checks.report("p2-l2-wm_hound", ok, "Pounce: after 7 blocks a level-1 Moot Hound's blow has auras x" + (one == null ? "none" : fmt(one.auras())) + " (want 1.8), a level-2 one's (" + rows(a) + ")=" + p + " x" + (two == null ? "none" : fmt(two.auras())) + " (want 2.0); his pounce then waits " + cd + " ticks (want 80)");
                return true;
            });
            add("p2-l3-wm_hound", b -> { // Run them down: +30% on a slowed or wounded target (C20)
                a = lv(post(b, dawn, "wm_hound", 0, -2, 0, true), 3); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(a);
                var plain = probe(b, a, v.mob, "melee");
                v.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0)); var slowed = probe(b, a, v.mob, "melee");
                v.mob.removeStatusEffect(StatusEffects.SLOWNESS); v.mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40);
                v.mob.setHealth(12.5f); var above = probe(b, a, v.mob, "melee"); // 31%: not wounded yet
                v.mob.setHealth(12f); var wounded = probe(b, a, v.mob, "melee");
                boolean ok = p && plain != null && near(plain.counters(), 1) && !plain.rows().contains("C20") && is(slowed, 1.3, "C20") && above != null && near(above.counters(), 1) && !above.rows().contains("C20") && is(wounded, 1.3, "C20");
                Phase2Checks.report("p2-l3-wm_hound", ok, "Run them down: a level-3 Moot Hound (" + rows(a) + ", tags " + a.tags + ")=" + p + " on a whole target " + hit(plain) + " (want no row), slowed " + hit(slowed) + ", at 31% (12.5 of 40) " + hit(above) + " (want 1.0, no row), at 30% " + hit(wounded) + " (want 1.3, C20, as the slowed one)");
                return true;
            });
            add("p2-l2-wm_hunter", b -> { // Ambush: +25% to +35% with no sky (C18b2)
                if (phase == 0) { a = post(b, dawn, "wm_hunter", -6, 0, -90, true); v = facing(b, dusk, 0, 0, 90); traits(); roof(b, -6, 0); phase = 1; return false; }
                if (since(b) < 20) return false; // the sky light has settled under the roof (the light engine runs behind the block change)
                lv(a, 1); var one = probe(b, a, v.mob, "arrow");
                lv(a, 2); boolean p = picks(a); var two = probe(b, a, v.mob, "arrow");
                boolean ok = p && is(one, 1.25, "C18b") && is(two, 1.35, "C18b2");
                Phase2Checks.report("p2-l2-wm_hunter", ok, "Ambush: under a roof a level-1 Hide Hunter's shot " + hit(one) + " (want 1.25, C18b); a level-2 one's (" + rows(a) + ", tags " + a.tags + ")=" + p + " " + hit(two) + " (want 1.35, C18b2)");
                return true;
            });
            add("p2-l3-wm_hunter", b -> { // Hunter's mark: 60 ticks, +10% from his army's troops
                a = lv(post(b, dawn, "wm_hunter", 0, -8, 0, true), 3); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(a); blow(b, a, v.mob, "arrow");
                long left = Statuses.remaining(b, v.mob, "mark"); double mine = Statuses.markFactor(b, dawn, v.mob), other = Statuses.markFactor(b, ally, v.mob);
                Phase2Checks.report("p2-l3-wm_hunter", p && left == 60 && near(mine, 1.1) && near(other, 1), "Hunter's mark: a level-3 Hide Hunter's (" + rows(a) + ")=" + p + " arrow marks for " + left + " ticks (want 60): his army's troops deal x" + fmt(mine) + " on it (want 1.10), an allied army's x" + fmt(other) + " (want 1)");
                return true;
            });
            add("p2-l2-wm_stalker", slows("p2-l2-wm_stalker", "wm_stalker", 2, "melee", 1, 50, 80, "Ensnare: Slowness II 30 to 50, cooldown 80"));
            add("p2-l3-wm_stalker", lays("p2-l3-wm_stalker", "wm_stalker", "melee", "bleed", 80, 0, "Venom bite: hits bleed for 80"));
            add("p2-l2-wm_tusk", counter("p2-l2-wm_tusk", "wm_tusk", "melee", 1, 1.3, "C18c", 2, 1.5, "C18c2", null, true, "Brace x1.3 to x1.5 against a charging enemy on Hold"));
            add("p2-l3-wm_tusk", share("p2-l3-wm_tusk", "wm_tusk", 3, "melee", .4, 2, 0, 1.5, 2, 0, "Long spear: a thrust also hits one enemy within 2 behind for 40%"));
            add("p2-l2-wm_seer", b -> { // Howl: +15% to +20% damage and speed, cooldown 300 to 240
                a = ready(lv(post(b, dawn, "wm_seer", 0, -4, 0, true), 2)); v = post(b, dawn, "revenant", 0, 4, 0, true); traits();
                boolean p = picks(a); t0 = b.time;
                Abilities.tick(b, a, null, t0);
                boolean ok = p && near(v.howlAmount, .2) && near(v.howlSpeed, .2) && v.howlUntil - t0 == 120 && a.abilityAt - t0 == 240;
                Phase2Checks.report("p2-l2-wm_seer", ok, "Howl: a level-2 Bloodhowl Seer (" + rows(a) + ")=" + p + " gives his troop 8 away +" + fmt(v.howlAmount) + " damage and +" + fmt(v.howlSpeed) + " speed for " + (v.howlUntil - t0) + " ticks (want 0.20, 0.20, 120) and waits " + (a.abilityAt - t0) + " (want 240)");
                return true;
            });
            add("p2-l3-wm_seer", b -> { // Rousing howl: the howl also cleanses slow, weaken and bleed
                a = ready(lv(post(b, dawn, "wm_seer", 0, -4, 0, true), 3)); v = post(b, dawn, "revenant", 0, 4, 0, true); z = post(b, dusk, "revenant", 0, 10, 180, true); traits();
                Statuses.apply(b, z, v.mob, "slow", 200, 0); Statuses.apply(b, z, v.mob, "weaken", 200, 0, .15); Statuses.apply(b, z, v.mob, "bleed", 200, 0);
                boolean p = picks(a); String was = st(b, v.mob);
                Abilities.tick(b, a, null, b.time);
                boolean clean = !Statuses.has(b, v.mob, "slow") && !Statuses.has(b, v.mob, "weaken") && !Statuses.has(b, v.mob, "bleed"), kept = near(v.howlAmount, .2);
                Phase2Checks.report("p2-l3-wm_seer", p && clean && kept, "Rousing howl: a level-3 Seer's (" + rows(a) + ")=" + p + " howl takes his troop from " + was + " to " + st(b, v.mob) + " (want none of the three) and still gives +" + fmt(v.howlAmount) + " (want 0.20)");
                return true;
            });
            add("p2-l2-wm_oldhorn", b -> { // Trample: 6 to 8 damage, cooldown 200 to 160
                a = ready(lv(post(b, dawn, "wm_oldhorn", 0, -2, 0, true), 2)); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 1.5, -1); traits();
                boolean p = picks(a);
                dawn.order = "charge"; b.markCharge(dawn, a.spec.group());
                t0 = b.time; var m = mark(); blow(b, a, v.mob, "melee");
                var hw = after(w.mob, m); float want = (float) (8 * b.campaign.damage(a));
                boolean ok = p && hw != null && Math.abs(hw.base() - want) < 1e-4 && a.abilityAt - t0 == 160;
                Phase2Checks.report("p2-l2-wm_oldhorn", ok, "Trample: a level-2 Old Horn (" + rows(a) + ")=" + p + " under a fresh Charge tramples a dummy 1.8 from him for " + base(hw) + " (want 8 x " + fmt(b.campaign.damage(a)) + " = " + fmt(want) + ") and waits " + (a.abilityAt - t0) + " (want 160)");
                return true;
            });
            add("p2-l3-wm_oldhorn", thickhide("p2-l3-wm_oldhorn", "wm_oldhorn", "Thick hide: shots x0.75"));
            add("p2-levels-wildmoot-shot", picture("wildmoot", b -> {
                a = lv(post(b, dawn, "wm_seer", 0, -3, 0, true), 3);
                for (double[] at : new double[][]{{-2, 1}, {2, 1}, {-1, 3}, {1, 3}}) group.add(post(b, dawn, "wm_tusk", at[0], at[1], 0, true));
                z = post(b, dusk, "revenant", 0, 9, 180, true); traits();
                look(b, -6, 4, -8, 0, 1, 1);
            }, b -> {
                if (since(b) % 8 != 0) return;
                for (var u : group) { Statuses.apply(b, z, u.mob, "slow", 100, 0); Statuses.apply(b, z, u.mob, "weaken", 100, 0, .15); Statuses.apply(b, z, u.mob, "bleed", 100, 0); }
                int c0 = Statuses.cleanses; ready(a); Abilities.tick(b, a, null, b.time); n0 += Statuses.cleanses - c0;
            }, b -> n0 >= 4, "a level-3 Bloodhowl Seer howls every 8 ticks over four Tusk Spears that keep being slowed, weakened and bled, and his Rousing howl cleanses them"));
        }
    }

    /** Suite levels-ironclad: the Ironclad's twelve level rows and its picture (a level-3 Ironhold Mortar's Firepot). */
    static final class IroncladLevels extends HouseSuite {
        IroncladLevels() {
            super("levels-ironclad");
            add("p2-l2-ic_hearthguard", guard("ic_hearthguard"));
            add("p2-l3-ic_hearthguard", counter("p2-l3-ic_hearthguard", "ic_hearthguard", "melee", 2, 1, null, 3, 1.3, "C18d", null, true, "Brace the wall: x1.3 against a charging enemy on Hold"));
            add("p2-l2-ic_arbalist", b -> { // Armour bolt: x0.7 to x0.85 against a raised shield (C15v)
                if (phase == 0) { v = tough(bare(post(b, dusk, "knight", 0, 0, 180, true))); raise(v); a = post(b, dawn, "ic_arbalist", 0, -8, 0, true); traits(); phase = 1; return false; }
                if (since(b) < 10) return false;
                lv(a, 1); var one = probe(b, a, v.mob, "arrow");
                lv(a, 2); boolean p = picks(a); var two = probe(b, a, v.mob, "arrow");
                boolean up = v.mob.isUsingItem(), ok = up && p && is(one, .7, "C15") && is(two, .85, "C15v");
                Phase2Checks.report("p2-l2-ic_arbalist", ok, "Armour bolt: against a raised shield (up=" + up + ") a level-1 Arbalist's bolt " + hit(one) + " (want 0.7, C15); a level-2 one's (" + rows(a) + ", tags " + a.tags + ")=" + p + " " + hit(two) + " (want 0.85, C15v)");
                return true;
            });
            add("p2-l3-ic_arbalist", b -> { // Heavy bolt: hits knock the target back 0.6
                a = lv(post(b, dawn, "ic_arbalist", 0, -8, 0, true), 3); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(a); n0 = Statuses.knocks;
                blow(b, a, v.mob, "arrow");
                boolean ok = p && Statuses.knocks - n0 == 1 && near(Statuses.lastKnock, .6);
                Phase2Checks.report("p2-l3-ic_arbalist", ok, "Heavy bolt: a level-3 Arbalist's (" + rows(a) + ")=" + p + " bolt throws " + (Statuses.knocks - n0) + " knock (want 1) of " + fmt(Statuses.lastKnock) + " (want 0.6)");
                return true;
            });
            add("p2-l2-ic_breaker", counter("p2-l2-ic_breaker", "ic_breaker", "melee", 1, 1.4, "C14", 2, 1.6, "C14v", "knight", false, "Sunder x1.4 to x1.6 against shield troops"));
            add("p2-l3-ic_breaker", lays("p2-l3-ic_breaker", "ic_breaker", "melee", "weaken", 60, .15, "Shatter: hits weaken for 60"));
            add("p2-l2-ic_ballista", share("p2-l2-ic_ballista", "ic_ballista", 2, "arrow", .8, 4, 0, 3.5, 1, .6, "Pierce 60% to 80%"));
            add("p2-l3-ic_ballista", slows("p2-l3-ic_ballista", "ic_ballista", 3, "arrow", 1, 30, 0, "Pinning bolt: Slowness II for 30"));
            add("p2-l2-ic_mortar", b -> { // Shell: radius 3.5 to 4.0
                a = lv(post(b, dawn, "ic_mortar", 0, -12, 0, true), 2); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 3.8, 0); y = dummy(b, dusk, 4.3, 0); traits();
                boolean p = picks(a); var m = mark();
                long[] c = cast(b, a, v.mob);
                var hv = after(v.mob, m); var hw = after(w.mob, m); var hy = after(y.mob, m);
                boolean ok = p && c[0] == 30 && c[1] == 100 && hv != null && hw != null && hy == null;
                Phase2Checks.report("p2-l2-ic_mortar", ok, "Shell: a level-2 Ironhold Mortar (" + rows(a) + ")=" + p + " winds up " + c[0] + " and waits " + c[1] + " (want 30, 100); the shell hits the target=" + (hv != null) + " and a dummy 3.8 off=" + (hw != null) + " (want both) and not one 4.3 off=" + (hy == null));
                return true;
            });
            add("p2-l3-ic_mortar", b -> { // Firepot: burning ground of radius 2.5 for 60; enemies in it bleed
                if (phase == 0) {
                    a = lv(post(b, dawn, "ic_mortar", 0, -12, 0, true), 3); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 3, 0); traits();
                    flag = picks(a); t0 = b.time;
                    cast(b, a, v.mob);
                    var fire = lastMist(b);
                    flag &= fire != null && fire.burning() && !fire.penalises() && near(fire.radius(), 2.5) && fire.until() - t0 == 60 && fire.row().id().equals("ic_mortar_l3");
                    why = "Firepot: a level-3 Mortar's (" + rows(a) + ") shell leaves " + (fire == null ? "nothing" : "ground of radius " + fmt(fire.radius()) + " for " + (fire.until() - t0) + " ticks, burning=" + fire.burning() + ", halving shots=" + fire.penalises()) + " (want 2.5, 60, true, false)";
                    phase = 1; return false;
                }
                if (since(b) < 12) return false;
                boolean bleeds = Statuses.has(b, v.mob, "bleed"), spared = !Statuses.has(b, w.mob, "bleed");
                Phase2Checks.report("p2-l3-ic_mortar", flag && bleeds && spared, why + "; the target in the ground bleeds=" + bleeds + ", a dummy 3 off (in the shell, out of the ground) does not=" + spared);
                return true;
            });
            add("p2-l2-ic_colossus", share("p2-l2-ic_colossus", "ic_colossus", 2, "melee", .75, 2.5, 1.5, 0, 1, .6, "Forge slam 60% to 75%"));
            add("p2-l3-ic_colossus", b -> { // Furnace heart: own troops within 6 take 10% less
                var heart = lv(post(b, dusk, "ic_colossus", 0, 4, 180, true), 3);
                v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 6.5, 4); y = dummy(b, dusk, 5.5, 4); a = post(b, dawn, "revenant", 0, -3, 0, true); traits();
                boolean p = picks(heart);
                var in = probe(b, a, v.mob, "melee"); var edge = probe(b, a, y.mob, "melee"); var out = probe(b, a, w.mob, "melee");
                boolean ok = p && in != null && near(in.auras(), .9) && edge != null && near(edge.auras(), .9) && out != null && near(out.auras(), 1);
                Phase2Checks.report("p2-l3-ic_colossus", ok, "Furnace heart: a level-3 Brassjaw's (" + rows(heart) + ")=" + p + " troop 4 from him takes auras x" + (in == null ? "none" : fmt(in.auras())) + " (want 0.9), one 5.5 from him x" + (edge == null ? "none" : fmt(edge.auras())) + " (want 0.9: within 6), one 6.5 from him x" + (out == null ? "none" : fmt(out.auras())) + " (want 1: past 6)");
                return true;
            });
            add("p2-levels-ironclad-shot", picture("ironclad", b -> {
                a = lv(post(b, dawn, "ic_mortar", 0, -11, 0, true), 3);
                v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 1.2, 1); y = dummy(b, dusk, -1.2, .8); traits();
                cast(b, a, v.mob);
                look(b, -6, 4, -6, 0, .5, 0);
            }, b -> { if (since(b) % 50 == 0) cast(b, a, v.mob); }, b -> lastMist(b) != null && lastMist(b).burning() && Statuses.has(b, v.mob, "bleed"),
                "a level-3 Ironhold Mortar's shell leaves burning ground under three dummies, which bleed"));
        }
    }

    /** Suite levels-tidecallers: the Tidecallers' twelve level rows and its picture (a level-3 Mist Caller's Chill fog). */
    static final class TidecallersLevels extends HouseSuite {
        TidecallersLevels() {
            super("levels-tidecallers");
            add("p2-l2-tc_raider", slows("p2-l2-tc_raider", "tc_raider", 2, "melee", 0, 40, 0, "Brine: Slowness I 20 to 40"));
            add("p2-l3-tc_raider", b -> { // Boarding rush: the first hit after closing 6 blocks x1.5, on his level-3 row's own cooldown (100)
                a = ready(lv(post(b, dawn, "tc_raider", 0, -2, 0, true), 3)); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(a); String three = rows(a);
                a.travelSinceAttack = 5.5; var short5 = probe(b, a, v.mob, "melee"); // a run short of the 6 blocks is no rush
                a.travelSinceAttack = 6.5; var rush = probe(b, a, v.mob, "melee");
                t0 = b.time; blow(b, a, v.mob, "melee"); long cd = Abilities.SECOND_AT.getOrDefault(a, 0L) - t0;
                a.travelSinceAttack = 7; var waiting = probe(b, a, v.mob, "melee");
                lv(a, 2); a.travelSinceAttack = 7; var two = probe(b, a, v.mob, "melee");
                boolean ok = p && short5 != null && near(short5.auras(), 1) && rush != null && near(rush.auras(), 1.5) && cd == 100 && waiting != null && near(waiting.auras(), 1) && two != null && near(two.auras(), 1);
                Phase2Checks.report("p2-l3-tc_raider", ok, "Boarding rush: after 5.5 blocks a level-3 Drowned Raider's (" + three + ")=" + p + " blow has auras x" + (short5 == null ? "none" : fmt(short5.auras())) + " (want 1: short of 6), after 6.5 blocks x" + (rush == null ? "none" : fmt(rush.auras())) + " (want 1.5); the rush then waits " + cd + " ticks (want 100) and the next run's blow is x" + (waiting == null ? "none" : fmt(waiting.auras())) + " (want 1); a level-2 Raider's x" + (two == null ? "none" : fmt(two.auras())) + " (want 1)");
                return true;
            });
            add("p2-l2-tc_harpooner", b -> { // Harpoon: pull 1 to 2 blocks, cooldown 60 to 50
                a = ready(lv(post(b, dawn, "tc_harpooner", -4, -6, 0, true), 1)); x = ready(lv(post(b, dawn, "tc_harpooner", 4, -6, 0, true), 2));
                v = tough(bare(post(b, dusk, "revenant", -4, 0, 180, true))); w = tough(bare(post(b, dusk, "revenant", 4, 0, 180, true))); traits();
                boolean p = picks(x);
                var resist = w.mob.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE); double keep = 1 - (resist == null ? 0 : resist.getValue());
                double d1 = apart(a, v), d2 = apart(x, w); t0 = b.time;
                blow(b, a, v.mob, "arrow"); blow(b, x, w.mob, "arrow");
                double one = d1 - apart(a, v), two = d2 - apart(x, w); long cd = x.abilityAt - t0;
                boolean ok = p && Math.abs(one - keep) < .05 && Math.abs(two - 2 * keep) < .05 && cd == 50 && slowIs(w.mob, 0, 30);
                Phase2Checks.report("p2-l2-tc_harpooner", ok, "Harpoon: a level-1 Harpooner pulls his target " + fmt(one) + " blocks, a level-2 one (" + rows(x) + ")=" + p + " " + fmt(two) + " (want 1 and 2, times the target's 1 - knockback resistance = " + fmt(keep) + "); the level-2 harpoon waits " + cd + " ticks (want 50) and slows " + st(b, w.mob) + " (want level 0 for 30)");
                return true;
            });
            add("p2-l3-tc_harpooner", lays("p2-l3-tc_harpooner", "tc_harpooner", "arrow", "bleed", 80, 0, "Barbed hook: hits bleed for 80"));
            add("p2-l2-tc_tideguard", counter("p2-l2-tc_tideguard", "tc_tideguard", "melee", 1, 1.3, "C18c", 2, 1.5, "C18c2", null, true, "Brace x1.3 to x1.5 against a charging enemy on Hold"));
            add("p2-l3-tc_tideguard", share("p2-l3-tc_tideguard", "tc_tideguard", 3, "melee", .4, 2, 1.5, 0, 2, 0, "Trident sweep: a cleave of 40% within 2"));
            add("p2-l2-tc_mist", b -> { // Mist: radius 4 to 5, 100 to 140 ticks (times her output factor: design 4.1, the level's bonus included)
                if (phase == 0) {
                    a = lv(post(b, dawn, "tc_mist", 0, -12, 0, true), 2); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 4.6, 0); y = dummy(b, dusk, 5.6, 0); traits();
                    flag = picks(a); t0 = b.time;
                    long[] c = cast(b, a, v.mob);
                    var fog = lastMist(b); long want = Math.round(140 * Veterans.outputFactor(a));
                    flag &= c[0] == 20 && c[1] == 160 && fog != null && near(fog.radius(), 5) && fog.until() - t0 == want && fog.row().id().equals("tc_mist_l2") && fog.rider() == null;
                    why = "Mist: a level-2 Mist Caller (" + rows(a) + ") winds up " + c[0] + " and waits " + c[1] + " (want 20, 160); her cloud " + (fog == null ? "(none)" : "of radius " + fmt(fog.radius()) + " stands " + (fog.until() - t0) + " ticks") + " (want 5 and 140 x her output " + fmt(Veterans.outputFactor(a)) + " = " + want + ")";
                    phase = 1; return false;
                }
                if (since(b) < 12) return false;
                boolean in = Statuses.has(b, v.mob, "slow") && Statuses.has(b, w.mob, "slow"), out = !Statuses.has(b, y.mob, "slow"), weak = !Statuses.has(b, v.mob, "weaken");
                Phase2Checks.report("p2-l2-tc_mist", flag && in && out && weak, why + "; it slows the target and a dummy 4.6 off=" + in + ", not one 5.6 off=" + out + ", and weakens nobody (no Chill fog at level 2)=" + weak);
                return true;
            });
            add("p2-l3-tc_mist", b -> { // Chill fog: enemies inside her cloud are weakened too
                if (phase == 0) {
                    a = lv(post(b, dawn, "tc_mist", 0, -12, 0, true), 3); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 7, 0); traits();
                    flag = picks(a);
                    cast(b, a, v.mob);
                    var fog = lastMist(b);
                    flag &= fog != null && fog.rider() != null && fog.rider().id().equals("tc_mist_l3") && fog.penalises();
                    why = "Chill fog: a level-3 Mist Caller's (" + rows(a) + ") cloud rides " + (fog == null || fog.rider() == null ? "nothing" : fog.rider().id()) + " (want tc_mist_l3)";
                    phase = 1; return false;
                }
                if (since(b) < 12) return false;
                boolean in = Statuses.has(b, v.mob, "weaken") && near(Statuses.factorOut(b, v.mob), .85), out = !Statuses.has(b, w.mob, "weaken");
                Phase2Checks.report("p2-l3-tc_mist", flag && in && out, why + "; inside it " + st(b, v.mob) + " (the target's blows x" + fmt(Statuses.factorOut(b, v.mob)) + ", want 0.85)=" + in + "; a dummy 7 off is not weakened=" + out);
                return true;
            });
            add("p2-l2-tc_brute", b -> { // Tentacle sweep: 70% to 85%, cooldown 60
                a = ready(lv(post(b, dawn, "tc_brute", 0, -2, 0, true), 2)); v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 1.5, 0); traits();
                boolean p = picks(a); t0 = b.time; var m = mark();
                blow(b, a, v.mob, "melee");
                var hw = after(w.mob, m); float want = (float) (.85 * Abilities.baseHit(b, a));
                boolean ok = p && hw != null && Math.abs(hw.base() - want) < 1e-4 && a.abilityAt - t0 == 60;
                Phase2Checks.report("p2-l2-tc_brute", ok, "Tentacle sweep: a level-2 Kraken-touched Brute's (" + rows(a) + ")=" + p + " blow sweeps a dummy 1.5 off for " + base(hw) + " (want 0.85 x " + fmt(Abilities.baseHit(b, a)) + " = " + fmt(want) + ") and the sweep waits " + (a.abilityAt - t0) + " (want 60)");
                return true;
            });
            add("p2-l3-tc_brute", thickhide("p2-l3-tc_brute", "tc_brute", "Barnacle hide: shots x0.75"));
            add("p2-l2-tc_admiral", b -> { // Undertow: cooldown 200 to 160, radius 7 to 8
                a = ready(lv(post(b, dawn, "tc_admiral", 0, -2, 0, true), 2)); v = dummy(b, dusk, 0, 5.5); w = dummy(b, dusk, 0, 6.6); traits();
                boolean p = picks(a); t0 = b.time; var m = mark();
                Abilities.tick(b, a, null, t0);
                var hv = after(v.mob, m); var hw = after(w.mob, m); float want = (float) (4 * b.campaign.damage(a));
                boolean ok = p && hv != null && Math.abs(hv.base() - want) < 1e-4 && hw == null && a.abilityAt - t0 == 160;
                Phase2Checks.report("p2-l2-tc_admiral", ok, "Undertow: a level-2 Admiral Vael (" + rows(a) + ")=" + p + " drags a dummy 7.5 from her for " + base(hv) + " (want 4 x " + fmt(b.campaign.damage(a)) + " = " + fmt(want) + "), not one 8.6 from her=" + (hw == null) + ", and waits " + (a.abilityAt - t0) + " (want 160)");
                return true;
            });
            add("p2-l3-tc_admiral", b -> { // Admiral's call: own troops within 10 deal +10%
                var call = lv(post(b, dawn, "tc_admiral", 0, -4, 0, true), 3);
                a = post(b, dawn, "revenant", 0, -2, 0, true); x = post(b, dawn, "revenant", 10.5, -4, 0, true); y = post(b, dawn, "revenant", 9.5, -4, 0, true); v = dummy(b, dusk, 0, 0); traits();
                boolean p = picks(call);
                var close = probe(b, a, v.mob, "melee"); var edge = probe(b, y, v.mob, "melee"); var far = probe(b, x, v.mob, "melee");
                boolean ok = p && close != null && near(close.auras(), 1.1) && edge != null && near(edge.auras(), 1.1) && far != null && near(far.auras(), 1);
                Phase2Checks.report("p2-l3-tc_admiral", ok, "Admiral's call: a level-3 Admiral's (" + rows(call) + ")=" + p + " Risen 2 from her deals auras x" + (close == null ? "none" : fmt(close.auras())) + " (want 1.1), one 9.5 from her x" + (edge == null ? "none" : fmt(edge.auras())) + " (want 1.1: within 10), one 10.5 from her x" + (far == null ? "none" : fmt(far.auras())) + " (want 1: past 10)");
                return true;
            });
            add("p2-levels-tidecallers-shot", picture("tidecallers", b -> {
                a = lv(post(b, dawn, "tc_mist", 0, -11, 0, true), 3);
                v = dummy(b, dusk, 0, 0); w = dummy(b, dusk, 2, 1); y = dummy(b, dusk, -2, 1); z = dummy(b, dusk, 1, -2); traits();
                cast(b, a, v.mob);
                look(b, -7, 5, -6, 0, .5, 0);
            }, b -> {}, b -> lastMist(b) != null && lastMist(b).rider() != null && Statuses.has(b, v.mob, "weaken"),
                "a level-3 Mist Caller's Chill fog stands over four dummies, which are slowed and weakened"));
        }
    }
}
