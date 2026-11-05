package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * The status engine of in-match progression design 6.1 (Task 6): what a level ability lays on a troop or a seated commander (Kort, round 34 question 3:
 * statuses hit commanders too).
 * - slow is vanilla Slowness through Abilities.slow, as before (an anchored troop takes none).
 * - knockback throws the target from the striker's spot by the row's knockback (Abilities.knock; an anchored troop is not moved).
 * - stun stops a troop's attacks and casts for its ticks (Unit.stunnedUntil, which Tactics and the casts read). A commander is not stunned: his body is his
 *   player's to move, and nothing stops a player's hands (a ruling for Kort, in the Task 6 report).
 * - weaken: the target's own blows deal x(1 - amount) for its ticks (factorOut, which Combat.resolve multiplies in beside the auras and Commander.playerFactor
 *   onto a commander's own blow). Not vanilla Weakness, which troop damage does not read.
 * - bleed: 1 damage every bleedEvery() ticks for its ticks, through the troop damage type (Combat.TROOP), outside Combat.resolve, so an undead troop takes it
 *   (vanilla Poison would not hurt him). Armour reduces it as it does every troop blow.
 * - mark (Hunter's mark): the target takes x(1 + amount) from the marking army's troops (markFactor, which Commander.factor reads beside Mark Prey), and
 *   from no other army, an allied one included. No glow.
 * Statuses of the same kind on one target do not stack (design 6.1, as Brine and Riptide): the one with the longer time left stands, and a new one that
 * outlasts it takes its place with its own amount and striker. A mark is one per marking army, since each army's mark serves only that army.
 * Every status a troop lays logs him as a helper on the target (Veterans.helped, kill credit design 3.2, review note I3), even where a longer one already
 * stands; a bleed tick logs its striker again, and a bleed whose striker has fallen still names his army as the last to hurt the target.
 * cleanse takes slow, weaken and bleed off (Dawnlight, Rousing howl: Abilities.mend and Abilities.howl). clear empties everything at a round boundary.
 * Lane B owns this file.
 */
final class Statuses {
    private Statuses() {}

    /** A bleed's damage per beat and the battle ticks between beats (design N3: 1 damage every 20 ticks): the combatDiscipline cells bleedDamage and bleedEveryTicks (Task 10 moved them out of code). */
    static float bleedDamage() { return (float) Spec.COMBATDISCIPLINE[0].bleedDamage(); }
    static int bleedEvery() { return Spec.COMBATDISCIPLINE[0].bleedEveryTicks(); }
    /** The hurt cooldown a bleed tick waits out (vanilla takes only the excess of a blow inside the last half of it): a tick waits while the target's timeUntilRegen is above this. */
    static final int HURT_HALF = 10;
    /** The latest a bleed tick that had to wait for a hurt cooldown may come after its status ran out. */
    static final int BLEED_GRACE = 40;

    /** One status on one entity: its kind, the battle time it wears off, its amount (weaken's share, mark's bonus), the troop that laid it, his army and troop id, and a bleed's next tick. */
    static final class Status {
        final String kind;
        long until, nextTick;
        double amount;
        GravesAndGlory.Unit from;
        GravesAndGlory.Squad by;
        String troop;
        Status(String kind) { this.kind = kind; }
    }

    /** The statuses that stand, by entity and kind (mark:<army uuid> for a mark). The battle they belong to: a new one starts empty. Server thread only. */
    private static final Map<LivingEntity, Map<String, Status>> ON = new IdentityHashMap<>();
    private static GravesAndGlory.Battle owner;

    /** Harness counters: bleed ticks dealt, knockbacks thrown and the strength of the last one, cleanses that took something off, and how many times the store was emptied (clear and release). */
    static int bleedTicks, knocks, cleanses, wipes;
    static double lastKnock;

    /** The store of a battle: a different battle than the owner's takes over an empty one. Package-private for the harness (p2-status-clear-round reads the branch). */
    static Map<LivingEntity, Map<String, Status>> on(GravesAndGlory.Battle b) {
        if (owner != b) { ON.clear(); owner = b; Abilities.clearRound(); } // a new battle (a world opened again): nothing of the old one's troops stays
        return ON;
    }

    /** How many entities carry a status now, whatever battle owns the store (the harness: has() and get() read nothing once owner is null, so only this sees ON itself). */
    static int held() { return ON.size(); }

    /** The server stops (Abilities.serverStopping, from SERVER_STOPPING): every static store lets go, the battle it belonged to included. A round boundary does not call this; Battle.clearProgression empties the store there, once (clear). */
    static void release() {
        wipes++;
        ON.clear();
        owner = null;
        Abilities.clearRound();
    }

    /** The row's status from a troop's blow, zone or slam: status, statusTicks, statusLevel, and amount (knockback for a knockback). */
    static void apply(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity target, Spec.AbilitiesRow row) {
        apply(b, from, target, row.status(), row.statusTicks(), row.statusLevel(), row.status().equals("knockback") ? row.knockback() : row.amount());
    }

    /** A status with no amount (slow, stun, bleed): the plan's interface. */
    static void apply(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity target, String status, int ticks, int level) { apply(b, from, target, status, ticks, level, 0); }

    /**
     * Lays a status on a living target that is no structure. level is Slowness's amplifier; amount is weaken's share, mark's bonus or a knockback's strength.
     * from is the troop that laid it (null: none, so nobody is credited and a bleed names no army).
     */
    static void apply(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity target, String status, int ticks, int level, double amount) {
        if (b == null || target == null || !target.isAlive() || Combat.structure(target)) return;
        if (target instanceof PlayerEntity p && p.isSpectator()) return;
        switch (status) {
            case "slow" -> Abilities.slow(b, from, target, level, ticks);
            case "knockback" -> {
                if (amount <= 0 || Abilities.anchored(b, target)) return;
                Vec3d at = from == null ? target.getPos() : from.mob.getPos();
                Abilities.knock(b, target, at, at, amount);
                knocks++; lastKnock = amount;
                if (from != null) Veterans.helped(b, from, target);
            }
            case "stun" -> {
                if (ticks <= 0) return;
                var u = Combat.unitOf(b, target);
                if (u == null) return; // a commander is not stunned (see the class note)
                u.stunnedUntil = Math.max(u.stunnedUntil, b.time + ticks);
                if (from != null) Veterans.helped(b, from, target);
                b.world.spawnParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getHeight() + .2, target.getZ(), 6, .25, .05, .25, .05);
            }
            case "weaken", "bleed", "mark" -> {
                if (ticks <= 0) return;
                if (from != null) Veterans.helped(b, from, target);
                String key = status.equals("mark") ? markKey(from == null ? null : from.squad) : status;
                if (key == null) return;
                var mine = on(b).computeIfAbsent(target, k -> new HashMap<>());
                var have = mine.get(key);
                long until = b.time + ticks;
                if (have != null && have.until >= until) return; // the longer one stands
                var s = have != null ? have : new Status(status);
                if (have == null) s.nextTick = b.time + bleedEvery();
                s.until = until; s.amount = amount;
                s.from = from; s.by = from == null ? null : from.squad; s.troop = from == null || from.temporary ? null : from.spec.id();
                mine.put(key, s);
            }
            default -> {}
        }
    }

    static String markKey(GravesAndGlory.Squad s) { return s == null ? null : "mark:" + s.owner; }

    /** Every tick of a running round, after the intro: a bleed ticks, a status that ran out goes, and an entity that died or left goes with all it had. */
    static void tick(GravesAndGlory.Battle b) {
        var map = on(b);
        if (b.time % 200 == 0) Abilities.prune(b);
        if (map.isEmpty()) return;
        for (var e : ordered(map)) {
            LivingEntity target = e.getKey();
            if (!target.isAlive() || target.isRemoved() || target instanceof PlayerEntity p && p.isSpectator()) { map.remove(target); continue; }
            var mine = e.getValue();
            var bleed = mine.get("bleed");
            if (bleed != null && b.time >= bleed.nextTick && bleed.nextTick <= bleed.until && target.timeUntilRegen <= HURT_HALF) {
                bleed.nextTick = BattleMath.nextBleedBeat(bleed.nextTick, b.time, bleedEvery());
                bleed(b, target, bleed);
                if (!target.isAlive()) { map.remove(target); continue; }
            }
            mine.values().removeIf(s -> b.time >= s.until && (!s.kind.equals("bleed") || s.nextTick > s.until || b.time >= s.until + BLEED_GRACE));
            if (mine.isEmpty()) { map.remove(target); continue; }
            if (b.time % 10 == 0) show(b, target, mine);
        }
    }

    /**
     * The store's entries in the order tick walks them: ascending entity id, which is the order the troops were spawned in. The store itself is an
     * IdentityHashMap, whose own order follows each entity's identity hash and so changes from one JVM run to the next; two bleeds that kill on the same
     * tick, and the level ups and feed lines their kills cause, would resolve differently in two runs of the same seeded match. A copy, so tick may remove from the store while it walks.
     */
    static List<Map.Entry<LivingEntity, Map<String, Status>>> ordered(Map<LivingEntity, Map<String, Status>> map) {
        var list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparingInt(e -> e.getKey().getId()));
        return list;
    }

    /**
     * One bleed tick: bleedDamage() through the troop damage type. A striker that lives is its source (Battle.allowDamage logs him and the kill is his); one that
     * has fallen is not, and the target's lastHitBy names his army so the kill still goes to it (the AFTER_DEATH fallback). The knock vanilla gives a blow with an
     * attacker is taken back, and the target's hurt cooldown is left as it was, so a bleed neither shoves nor eats the next real blow. The velocity goes back
     * on the server only: the update flag stays as the tick found it, so a bleed never sends a velocity of its own (a commander's server velocity is not
     * his client's, and sending it would stop his run or replay an old knock; Task 6 review M3), while a knock or pull already due this tick is still sent.
     */
    static void bleed(GravesAndGlory.Battle b, LivingEntity target, Status s) {
        boolean live = s.from != null && s.from.mob.isAlive() && b.units.contains(s.from);
        var victim = Combat.unitOf(b, target);
        if (victim != null && s.by != null) { victim.lastHitBy = s.by; victim.lastHitAt = b.time; victim.lastHitTroop = s.troop; }
        if (live) Veterans.helped(b, s.from, target);
        var source = live ? Combat.source(b, s.from.mob) : b.world.getDamageSources().create(Combat.TROOP);
        Vec3d stood = target.getVelocity();
        int regen = target.timeUntilRegen;
        boolean due = target.velocityModified;
        boolean hurt = target.damage(source, bleedDamage());
        if (target.isAlive()) { target.setVelocity(stood); target.velocityModified = due; target.timeUntilRegen = regen; }
        if (hurt) bleedTicks++;
        b.world.spawnParticles(Abilities.dust(BLOOD), target.getX(), target.getBodyY(.5), target.getZ(), 5, .2, .3, .2, 0);
    }

    /** The bleed's and the burning ground's dark red, the weaken's ash grey and the mark's hunter green (presentation numbers). */
    static final int BLOOD = 0x8A1C1C, ASH = 0x8C8C96, MARK = 0x6B8E23;

    /** What a status looks like while it stands, twice a second: blood drips for a bleed, grey ash for a weaken, a green fleck over the head for a mark. */
    static void show(GravesAndGlory.Battle b, LivingEntity target, Map<String, Status> mine) {
        double x = target.getX(), z = target.getZ();
        for (var s : mine.values()) {
            switch (s.kind) {
                case "weaken" -> b.world.spawnParticles(Abilities.dust(ASH), x, target.getBodyY(.7), z, 2, .25, .2, .25, 0);
                case "mark" -> b.world.spawnParticles(Abilities.dust(MARK), x, target.getY() + target.getHeight() + .35, z, 1, .05, .05, .05, 0);
                default -> {}
            }
        }
    }

    /** A round boundary (Battle.clearProgression: startRound, Campaign.returnLobby, switchMap): every status goes, and lane B's per-soldier ability state (Abilities.clearRound). The store now belongs to this battle. */
    static void clear(GravesAndGlory.Battle b) {
        wipes++;
        ON.clear();
        owner = b;
        Abilities.clearRound();
    }

    /** The share of an attacker's outgoing blow its statuses leave: x(1 - amount) while a weaken stands on it, else 1. */
    static double factorOut(GravesAndGlory.Battle b, LivingEntity attacker) {
        var s = get(b, attacker, "weaken");
        return s == null ? 1.0 : 1 - s.amount;
    }

    /** Hunter's mark: x(1 + amount) for a blow by a troop of the army whose mark stands on the target, else 1 (an allied army gets none). */
    static double markFactor(GravesAndGlory.Battle b, GravesAndGlory.Squad attacker, LivingEntity target) {
        String key = markKey(attacker);
        var s = key == null ? null : get(b, target, key);
        return s == null ? 1.0 : 1 + s.amount;
    }

    /** The status of a kind standing on an entity now, or null. */
    static Status get(GravesAndGlory.Battle b, LivingEntity e, String key) {
        if (b == null || e == null || owner != b) return null;
        var mine = ON.get(e);
        var s = mine == null ? null : mine.get(key);
        return s == null || b.time >= s.until ? null : s;
    }

    /** Whether a status of a kind stands on the entity (slow: vanilla Slowness; mark: any army's). The harness reads it. */
    static boolean has(GravesAndGlory.Battle b, LivingEntity e, String kind) { return remaining(b, e, kind) > 0; }

    /** The ticks a status of a kind has left on the entity, 0 when none stands (slow: vanilla Slowness's duration; mark: the longest of any army's). */
    static long remaining(GravesAndGlory.Battle b, LivingEntity e, String kind) {
        if (kind.equals("slow")) { var s = e.getStatusEffect(StatusEffects.SLOWNESS); return s == null ? 0 : s.getDuration(); }
        if (b == null || owner != b) return 0;
        var mine = ON.get(e);
        long left = 0;
        if (mine != null) for (var s : mine.values()) if (s.kind.equals(kind)) left = Math.max(left, s.until - b.time);
        return left;
    }

    /**
     * Dawnlight and Rousing howl (N13): slow, weaken and bleed come off the entity. by, the troop that cleansed, is logged as supporting it where it took
     * something off (Veterans.supported: design 3.2, review note I3). Returns whether anything came off.
     */
    static boolean cleanse(GravesAndGlory.Battle b, GravesAndGlory.Unit by, LivingEntity e) {
        boolean took = false;
        if (e.hasStatusEffect(StatusEffects.SLOWNESS)) { e.removeStatusEffect(StatusEffects.SLOWNESS); took = true; }
        var mine = owner == b ? ON.get(e) : null;
        if (mine != null) {
            took |= mine.remove("weaken") != null;
            took |= mine.remove("bleed") != null;
            if (mine.isEmpty()) ON.remove(e);
        }
        if (took) {
            cleanses++;
            if (by != null) Veterans.supported(b, by, e);
            b.world.spawnParticles(ParticleTypes.END_ROD, e.getX(), e.getBodyY(.6), e.getZ(), 6, .25, .35, .25, .02);
            Audio.at(b, AudioCues.Cue.HEALER_CAST, e.getPos(), "cl" + e.getId());
        }
        return took;
    }
}
