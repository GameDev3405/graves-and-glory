package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * In-match progression's server facade (in-match progression design 3 and 4): kill credit, level ups, gear, the Armoury state and the bots' buys.
 * Task 3 lays its seams, with no behaviour that changes a match:
 * - helped and supported write the kill-credit logs (Battle.credit, a CreditLog) for every troop blow (Battle.allowDamage), slow and pull
 *   (Abilities.slow, Abilities.pull), enemy held in a cloud (Abilities.mistTick), mend, Howl and Oath Banner. Nothing reads the logs until Task 4.
 * - damageFactor and outputFactor read the soldier's level and worn weapon tier (LevelMath.damageFactor): 1.0 at level 1 with tier I, which every
 *   soldier holds until Task 4 levels him. Combat.resolve multiplies damageFactor onto the blow's base, outside the clamp, as stars do.
 * - armouryState and armouryOpen give the empty Armoury that state_v9 carries until Task 5 fills it.
 * - clear empties the logs at a round boundary (Battle.clearProgression: startRound, Campaign.returnLobby and switchMap).
 * Lane A owns this file from Task 4 on.
 */
final class Veterans {
    private Veterans() {}

    /** The credit window, progression.assistSeconds in battle ticks. */
    static long window() { return Spec.PROGRESSION[0].assistSeconds() * 20L; }

    /** How often (battle ticks) helped and supported trim the logs to the window, so they hold no more than the last few seconds. */
    static final int TRIM_TICKS = 20;

    /** Whether this unit may earn credit: a living troop of the battle that is not temporary (a Grave Bloom Risen never earns; a player commander is no Unit). */
    static boolean earns(GravesAndGlory.Unit u) { return u != null && !u.temporary && u.mob.isAlive(); }

    /**
     * A troop damaged, slowed, pulled or weakened the victim, or held it in one of his zones (design 3.2): the victim's log gains him at this battle
     * time. Temporary troops, players and dead helpers are not logged, and a structure is no victim (design 3.2: structures are not kills).
     */
    static void helped(GravesAndGlory.Battle b, GravesAndGlory.Unit helper, LivingEntity victim) {
        if (b == null || victim == null || !earns(helper) || helper.mob == victim || Combat.structure(victim)) return;
        b.credit.hit(victim.getId(), helper.mob.getId(), helper.squad.team, b.time);
        trim(b);
    }

    /**
     * A troop healed or buffed an own troop (mend, Howl, the Oath Banner): the target's support log gains him at this battle time. Temporary troops
     * and players are not logged on either side, a troop does not support himself, and only a troop of his own team is supported.
     */
    static void supported(GravesAndGlory.Battle b, GravesAndGlory.Unit supporter, LivingEntity target) {
        if (b == null || target == null || !earns(supporter) || supporter.mob == target || target instanceof PlayerEntity) return;
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

    /** The soldier's damage factor from his level and worn weapon tier (design 4.1): 1 + levels.damagePct + gearTiers.weaponPct. 1 for no unit. */
    static double damageFactor(GravesAndGlory.Unit u) { return u == null ? 1 : LevelMath.damageFactor(u.level, u.weaponTier); }

    /** The same factor for a troop whose main output is no hit (design 4.1): the Sunweaver's heal amount and the Mist Caller's cloud duration. Lane B applies it in Task 6. */
    static double outputFactor(GravesAndGlory.Unit u) { return damageFactor(u); }

    /** The army's Armoury as state_v9 carries it to its commander. Empty until Task 5. */
    static LevelMath.Armoury armouryState(GravesAndGlory.Squad s) { return LevelMath.Armoury.EMPTY; }

    /** Whether an Armoury buy for a troop of this group is open and not yet bought (state_v9's per-group armouryOpen, the Iron Ledger glint). False until Task 5. */
    static boolean armouryOpen(GravesAndGlory.Squad s, String group) { return false; }

    /** A round boundary: every credit entry goes. Task 4 adds the pending feed lines, Task 5 every Armoury. */
    static void clear(GravesAndGlory.Battle b) {
        b.credit.clear();
        b.creditTrimmedAt = 0;
    }
}
