package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * In-match progression's server facade (in-match progression design 3 and 4): kill credit, level ups, gear, the Armoury state and the bots' buys.
 * - helped and supported write the kill-credit logs (Battle.credit, a CreditLog) for every troop blow (Battle.allowDamage), slow and pull
 *   (Abilities.slow, Abilities.pull), enemy held in a cloud (Abilities.mistTick), mend, Howl and Oath Banner.
 * - onDeath (AFTER_DEATH, every troop that really dies, a temporary one too) and onCommanderDeath (a player commander struck down, Battle.allowDamage)
 *   credit the killing blow's troop, his team's helpers in the window and their supporters with the victim's bounty (LevelMath.bounty); credit
 *   raises a soldier's credits and levelUp applies each level he reaches: the health step, the level name, the flash, his rank (rank_v1), his ability
 *   rows and the pending feed line.
 * - damageFactor and outputFactor read the soldier's level and worn weapon tier (LevelMath.damageFactor). Combat.resolve multiplies damageFactor onto
 *   the blow's base, outside the clamp, as stars do.
 * - armouryState and armouryOpen give the empty Armoury that state_v9 carries until Task 5 fills it.
 * - clear empties the logs and the pending feed lines at a round boundary (Battle.clearProgression: startRound, Campaign.returnLobby and switchMap).
 * Everything runs on the server thread. Lane A owns this file.
 */
final class Veterans {
    private Veterans() {}

    /** The credit window, progression.assistSeconds in battle ticks. */
    static long window() { return Spec.PROGRESSION[0].assistSeconds() * 20L; }

    /** How often (battle ticks) helped and supported trim the logs to the window, so they hold no more than the last few seconds. */
    static final int TRIM_TICKS = 20;

    /**
     * The switch over progression.enabled (in-match progression plan, spec gap 12): null reads the sheet; true or false overrides it. The bot
     * simulation's -PsimProgression (Task 5) and the harness set it; play never does. Off, nobody is logged, credited or levelled.
     */
    static volatile Boolean override;

    /** Whether in-match progression runs: the override when it is set, else progression.enabled. */
    static boolean enabled() { Boolean o = override; return o != null ? o : Spec.PROGRESSION[0].enabled(); }

    /** Whether this unit may earn credit: a living troop of the battle that is not temporary (a Grave Bloom Risen never earns; a player commander is no Unit). */
    static boolean earns(GravesAndGlory.Unit u) { return u != null && !u.temporary && u.mob.isAlive(); }

    /**
     * A troop damaged, slowed, pulled or weakened the victim, or held it in one of his zones (design 3.2): the victim's log gains him at this battle
     * time. Temporary troops, players and dead helpers are not logged, and a structure is no victim (design 3.2: structures are not kills).
     */
    static void helped(GravesAndGlory.Battle b, GravesAndGlory.Unit helper, LivingEntity victim) {
        if (b == null || victim == null || !enabled() || !earns(helper) || helper.mob == victim || Combat.structure(victim)) return;
        b.credit.hit(victim.getId(), helper.mob.getId(), helper.squad.team, b.time);
        trim(b);
    }

    /**
     * A troop healed or buffed an own troop (mend, Howl, the Oath Banner): the target's support log gains him at this battle time. Temporary troops
     * and players are not logged on either side, a troop does not support himself, and only a troop of his own team is supported.
     */
    static void supported(GravesAndGlory.Battle b, GravesAndGlory.Unit supporter, LivingEntity target) {
        if (b == null || target == null || !enabled() || !earns(supporter) || supporter.mob == target || target instanceof PlayerEntity) return;
        var t = Combat.unitOf(b, target);
        if (t == null || t.temporary || t.squad.team != supporter.squad.team) return;
        b.credit.support(target.getId(), supporter.mob.getId(), supporter.squad.team, b.time);
        trim(b);
    }

    /** Trims the logs to the window once every TRIM_TICKS. */
    static void trim(GravesAndGlory.Battle b) {
        if (b.time - b.creditTrimmedAt < TRIM_TICKS) return;
        b.creditTrimmedAt = b.time;
        b.credit.trim(b.time, window());
    }

    // ---- kill credit (design 3.2) ----

    /**
     * A troop of the battle really died (AFTER_DEATH; a Rise Again stands him up instead and never reaches here, so the rising blow credits nobody).
     * winner is the army the kill goes to, as the gold line finds it: the attacker's army, or within the credit window the army whose blow last hurt
     * the victim (a shot that lands after its archer fell). When it is the other team's, the killing blow's troop (found among b.units, alive and not
     * temporary; a commander's own blow, a temporary troop's and a fallen archer's shot have none), every helper of that team in the window and their
     * supporters each get the victim's bounty: his level, 1 for a temporary troop. The killer's onKill runs after. Whatever happens, the victim is
     * forgotten by the credit logs. Called above AFTER_DEATH's temporary skip, so a Risen killed counts 1.
     */
    static void onDeath(GravesAndGlory.Battle b, GravesAndGlory.Unit victim, Entity attacker, GravesAndGlory.Squad winner) {
        if (b == null || victim == null) return;
        int id = victim.mob.getId();
        try {
            if (!enabled() || winner == null || winner.team == victim.squad.team) return;
            var striker = attacker == null ? null : Combat.unitOf(b, attacker);
            var killer = earns(striker) && striker.squad.team == winner.team ? striker : null;
            award(b, id, killer, winner.team, LevelMath.bounty(victim.level, false, victim.temporary), victim.spec.id());
        } finally {
            b.credit.forget(id);
        }
    }

    /**
     * A commander was struck down (Battle.allowDamage's respawn branch: a player commander never reaches AFTER_DEATH). The other team's striker, his
     * helpers in the window and their supporters each get progression.commanderBounty. The striker is found among b.units; a troop that fell before
     * its shot landed is gone from them, and then the side is the commander's enemy (only the other side's blows reach him: friendly fire is off). A
     * death with no attacker (a fall, lava, the void) credits nobody. Whatever happens, the commander's own log is forgotten: he stands again within
     * the window (the Reaver in 9 s), and a second death must not credit the first one's helpers again. A bot commander has no body today; a body
     * would call this too.
     */
    static void onCommanderDeath(GravesAndGlory.Battle b, LivingEntity commander, int team, Entity attacker) {
        if (b == null || commander == null) return;
        int id = commander.getId();
        try {
            if (!enabled() || attacker == null) return;
            var striker = Combat.unitOf(b, attacker);
            int side = striker != null ? striker.squad.team : b.teamOf(attacker);
            if (side < 0 && (team == 0 || team == 1)) side = 1 - team;
            if (side < 0 || side == team) return;
            award(b, id, earns(striker) ? striker : null, side, LevelMath.bounty(1, true, false), "commander");
        } finally {
            b.credit.forget(id);
        }
    }

    /** Credits the killer (null: no troop's blow), every helper of the team on the victim in the window and their supporters with the bounty; each is found among b.units (a dead or departed id gets nothing). Then the killer's onKill. */
    static void award(GravesAndGlory.Battle b, int victim, GravesAndGlory.Unit killer, int team, int bounty, String what) {
        Set<Integer> ids = b.credit.credited(victim, killer == null ? -1 : killer.mob.getId(), team, b.time, window());
        log(b, "kill", killer, bounty, what + " #" + victim + " for team " + team + ", bounty " + bounty + ", credited " + ids);
        for (var u : new ArrayList<>(b.units)) if (ids.contains(u.mob.getId()) && earns(u) && u.squad.team == team) credit(b, u, bounty);
        if (killer != null) Abilities.onKill(b, killer);
    }

    /** The soldier gains kill credits; each level they carry him to applies at once (9 at level 1 is two level ups). Nothing for a dead or temporary troop, or with progression off. */
    static void credit(GravesAndGlory.Battle b, GravesAndGlory.Unit u, int credits) {
        if (b == null || credits <= 0 || !enabled() || !earns(u)) return;
        int from = u.level;
        u.credits += credits;
        log(b, "credit", u, credits, "now " + u.credits);
        int to = Math.max(from, LevelMath.levelFor(u.credits));
        if (to > from) levelUp(b, u, from, to);
    }

    // ---- level ups (design 3.1, 3.3 and 7) ----

    /** A line for the event feed (design 7): a soldier of this team reached progression.feedLevel under this level name. */
    record Feed(int team, String levelName) {}

    /** This round's feed lines, oldest first, one per soldier reaching progression.feedLevel; the HUD task (Task 12) reads them, clear empties them at a round boundary. */
    static final List<Feed> pendingFeed = new ArrayList<>();

    /**
     * The soldier goes from one level to a higher one. Per level: his max health rises by that level's step (the base value set at spawn, stars in it,
     * scaled by the new factor over the old) and his current health by the same amount only; his rank goes to every player tracking him (rank_v1).
     * Then, once: his custom name becomes the level name (plus the veteran suffix), the flash plays at him, his ability rows are read again
     * (Abilities.rowsChanged) and, when he crossed progression.feedLevel, one feed line waits.
     */
    static void levelUp(GravesAndGlory.Battle b, GravesAndGlory.Unit u, int from, int to) {
        for (int level = from + 1; level <= to; level++) {
            healthStep(u, LevelMath.healthFactor(level - 1, u.armourTier), LevelMath.healthFactor(level, u.armourTier));
            u.level = level;
            levelUps++;
            log(b, "level", u, level, "max health " + u.mob.getMaxHealth() + ", health " + u.mob.getHealth());
            RankSync.changed(b, u);
            log(b, "rank", u, level, "rank_v1 level " + u.level + " weapon " + u.weaponTier + " armour " + u.armourTier);
        }
        u.mob.setCustomName(b.troopName(u.squad, u.spec, u.level));
        flash(b, u);
        Abilities.rowsChanged(u);
        int feed = Spec.PROGRESSION[0].feedLevel();
        if (from < feed && to >= feed) {
            String name = ArmyMobs.levelName(u.spec, to);
            pendingFeed.add(new Feed(u.squad.team, name));
            log(b, "feed", u, to, name + " rises");
        }
    }

    /** Level ups applied since the server started (the harness reads it). */
    static int levelUps;

    /**
     * Scales the soldier's max health by after / before, on the attribute's base value (the value Battle.spawn set: the sheet's health times his star
     * bonus, so stars stay on top), and raises his current health by exactly what the maximum gained. Task 5's armour wear takes the same step.
     */
    static void healthStep(GravesAndGlory.Unit u, double before, double after) {
        if (before <= 0 || after == before) return;
        var max = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (max == null) return;
        float was = u.mob.getMaxHealth();
        max.setBaseValue(max.getBaseValue() * after / before);
        float added = u.mob.getMaxHealth() - was;
        if (u.mob.isAlive() && added > 0) u.mob.setHealth(Math.min(u.mob.getMaxHealth(), u.mob.getHealth() + added));
    }

    /** The level-up flash's presentation (design 7: readable within about 24 blocks): dust in the side's colour, a ring of it at his feet and a column round him, and the sound at a volume vanilla carries 24 blocks. */
    static final int FLASH_RING = 16, FLASH_COLUMN = 30;
    static final float FLASH_SIZE = 1.7f, FLASH_VOLUME = 1.5f, FLASH_PITCH = 0.9f;
    static final SoundEvent FLASH_SOUND = SoundEvents.ENTITY_PLAYER_LEVELUP;

    /** One flash as it played: the soldier's entity id, where, the dust's RGB, the sound's id and the battle time (the harness reads it). */
    record Flash(int unit, Vec3d at, int rgb, String sound, long time) {}
    static int flashes;
    static Flash lastFlash;

    /** The level-up flash at the soldier, on the server, so every client near him sees and hears it with no packet of our own (spec gap 10). */
    static void flash(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        int rgb = b.side(u.squad.team).color();
        var dust = new DustParticleEffect(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), FLASH_SIZE);
        double x = u.mob.getX(), y = u.mob.getY(), z = u.mob.getZ(), h = u.mob.getHeight();
        for (int i = 0; i < FLASH_RING; i++) {
            double a = i * Math.PI * 2 / FLASH_RING;
            b.world.spawnParticles(dust, x + Math.cos(a) * .75, y + .15, z + Math.sin(a) * .75, 1, 0, 0, 0, 0);
        }
        b.world.spawnParticles(dust, x, y + h * .55, z, FLASH_COLUMN, .35, h * .4, .35, 0);
        b.world.playSound(null, x, y, z, FLASH_SOUND, SoundCategory.HOSTILE, FLASH_VOLUME, FLASH_PITCH);
        flashes++;
        lastFlash = new Flash(u.mob.getId(), new Vec3d(x, y, z), rgb, FLASH_SOUND.getId().toString(), b.time);
    }

    // ---- the log the harness reads ----

    /** One progression event: kill (value the bounty), credit (credits added), level (the new level), rank (the level sent), feed (the level reached); unit is the soldier's entity id, -1 for none. */
    record Logged(String kind, int unit, int value, long time, String detail) {}
    /** The newest events, oldest first, at most LOG_SIZE. Server thread only. */
    static final ArrayDeque<Logged> LOG = new ArrayDeque<>();
    static final int LOG_SIZE = 256;

    static void log(GravesAndGlory.Battle b, String kind, GravesAndGlory.Unit u, int value, String detail) {
        int id = u == null ? -1 : u.mob.getId();
        LOG.addLast(new Logged(kind, id, value, b.time, detail));
        while (LOG.size() > LOG_SIZE) LOG.removeFirst();
        if (Diagnostics.active()) System.out.println("[levels] " + kind + " " + (u == null ? "-" : u.spec.id() + "#" + id) + " " + value + " " + detail);
    }

    // ---- stat factors (design 4.1) ----

    /** The soldier's damage factor from his level and worn weapon tier (design 4.1): 1 + levels.damagePct + gearTiers.weaponPct. 1 for no unit. */
    static double damageFactor(GravesAndGlory.Unit u) { return u == null ? 1 : LevelMath.damageFactor(u.level, u.weaponTier); }

    /** The same factor for a troop whose main output is no hit (design 4.1): the Sunweaver's heal amount and the Mist Caller's cloud duration. Lane B applies it in Task 6. */
    static double outputFactor(GravesAndGlory.Unit u) { return damageFactor(u); }

    // ---- the Armoury (Task 5) ----

    /** The army's Armoury as state_v9 carries it to its commander. Empty until Task 5. */
    static LevelMath.Armoury armouryState(GravesAndGlory.Squad s) { return LevelMath.Armoury.EMPTY; }

    /** Whether an Armoury buy for a troop of this group is open and not yet bought (state_v9's per-group armouryOpen, the Iron Ledger glint). False until Task 5. */
    static boolean armouryOpen(GravesAndGlory.Squad s, String group) { return false; }

    /** A round boundary: every credit entry and every pending feed line goes. Task 5 adds every Armoury. */
    static void clear(GravesAndGlory.Battle b) {
        b.credit.clear();
        b.creditTrimmedAt = 0;
        pendingFeed.clear();
    }
}
