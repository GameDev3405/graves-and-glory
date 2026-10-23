package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * All troop damage (spec section 3). resolve multiplies the matching counters rows, the House traits, the auras and the commander
 * effects into one factor, clamps it to [combatDiscipline.multiplierMin, multiplierMax] and hurts the target with the damage type
 * gravesandglory:troop, which is tagged bypasses_shield: a raised shield takes no part in the vanilla block, the counters table is the
 * only shield maths. Armour still applies. Every number comes from the counters, houseTraits, abilities and combatDiscipline sheets,
 * the charge-impact window of counters row C5 included (combatDiscipline.chargeImpactTicks).
 */
public final class Combat {
    private Combat() {}
    /** The damage type of every troop blow (data/gravesandglory/damage_type/troop.json). */
    public static final RegistryKey<DamageType> TROOP = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier("gravesandglory", "troop"));
    /** How many hits LAST keeps. */
    static final int LOG_SIZE = 64;

    /**
     * One resolved hit: the attacker's and target's entity ids, the base amount as given (stars in it), each factor, the clamped total and the ids of the
     * counters rows that applied. veteran is the attacker's level-and-weapon factor (Veterans.damageFactor), which multiplies the base outside the clamp;
     * status is his statuses' share (Statuses.factorOut), inside the clamped total beside the auras. The amount dealt is base x veteran x total.
     */
    public record HitLog(int attacker, int target, float base, double counters, double stance, double traits, double auras, double commander, double veteran, double status, double total, List<String> rows) {}
    /** The newest resolved hits, oldest first, at most 64. Server thread only. */
    public static final ArrayDeque<HitLog> LAST = new ArrayDeque<>();

    /** The troop damage source for a blow by this mob. */
    public static DamageSource source(GravesAndGlory.Battle b, LivingEntity attacker) { return b.world.getDamageSources().create(TROOP, attacker); }

    /**
     * Resolves one troop hit and applies it. kind is melee, arrow, bolt, blight or slam; a secondary hit (a cleave or pierce second target, a
     * slam's splash) does not run the on-hit abilities again. A base of 0 does nothing. Returns the amount handed to the target, 0 when nothing
     * was applied. This is the one place a blow's multiplier is built: the counters table (with the target's covered flanks and whether the blow
     * has a direction), the attacker's fearless stance, the House traits, the auras and the commanders, clamped as one.
     */
    public static float resolve(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target, float base, String kind, boolean secondary) {
        if (base <= 0 || target == null || !target.isAlive()) return 0;
        var D = Spec.COMBATDISCIPLINE[0];
        GravesAndGlory.Unit victim = unitOf(b, target);
        boolean melee = kind.equals("melee") || kind.equals("slam");
        // A blow with no direction takes no flank bonus: blight (a shell too), a slam's splash and the undertow (the slam kind is only ever the splash: the primary blow is melee), a cleave's second target.
        boolean area = kind.equals("blight") || kind.equals("slam") || secondary && kind.equals("melee");

        Set<String> from = new HashSet<>(attacker.tags);
        from.addAll(CounterMath.kindTags(kind)); // the troop card's counter lines read the same tags (CounterMath.cardRows)
        if (!from.contains("siege")) from.add("nonsiege");
        Set<String> to = victim == null ? new HashSet<>() : new HashSet<>(victim.tags);
        if (structure(target)) to.add("structure");
        if (victim != null) {
            String theirs = b.assignment(victim).order;
            if (theirs.equals("charge")) to.add("charging");
            if (theirs.equals("hold")) to.add("holding");
        }

        String order = b.assignment(attacker).order;
        boolean charging = melee && order.equals("charge"), holding = order.equals("hold");
        Long charged = attacker.squad.chargedAt.get(attacker.spec.group());
        boolean impact = charging && charged != null && b.time - charged <= D.chargeImpactTicks() && attacker.chargeHitAt < charged;
        double bearing = structure(target) ? 0 : BattleMath.relativeBearing(target.getYaw(), attacker.mob.getX() - target.getX(), attacker.mob.getZ() - target.getZ()); // a structure has no facing, so no flank (C16, C16s)
        boolean raised = victim != null && victim.tags.contains("shield") && victim.mob.isUsingItem() && victim.mob.getActiveItem().isOf(Items.SHIELD);
        boolean noSky = from.contains("ambush") && Abilities.noSky(attacker);
        boolean covered = victim != null && Houses.covered(b, victim);
        boolean slowed = target.hasStatusEffect(StatusEffects.SLOWNESS), wounded = BattleMath.wounded(target.getHealth(), target.getMaxHealth(), D.woundedShare()); // Run them down (C20) and Unbroken (C21)
        var hit = new CounterMath.Hit(from, to, bearing, raised, impact, charging, holding, noSky, Abilities.inMist(b, attacker.mob, target), covered, area, slowed, wounded);

        double counters = CounterMath.counters(hit, Spec.COUNTERS), stance = stance(attacker);
        double traits = Houses.traitFactor(b, attacker, target, kind, area ? 0 : bearing), auras = Abilities.auraFactor(b, attacker, target), commander = commanderFactor(b, attacker, target);
        double veteran = Veterans.damageFactor(attacker), status = Statuses.factorOut(b, attacker.mob); // in-match progression: the level-and-weapon factor goes on the base, outside the clamp, as stars do; the statuses (weaken, Task 6) sit in the product with the auras
        double total = CounterMath.total(counters * stance, traits, auras * status, commander, D.multiplierMin(), D.multiplierMax());
        List<String> rows = List.copyOf(CounterMath.applied(hit, Spec.COUNTERS));
        if (rows.contains("C16")) attacker.squad.stats.flankHits++; // the round's flank tally per army (MatchStats.SquadStats)
        if (rows.contains("C16s")) attacker.squad.stats.flankShots++;
        LAST.addLast(new HitLog(attacker.mob.getId(), target.getId(), base, counters, stance, traits, auras, commander, veteran, status, total, rows));
        while (LAST.size() > LOG_SIZE) LAST.removeFirst();

        if (impact) attacker.chargeHitAt = b.time;
        if (victim != null) { victim.lastHitBy = attacker.squad; victim.lastHitAt = b.time; victim.lastHitTroop = attacker.temporary ? null : attacker.spec.id(); } // the troop type too, for the mastery ledger (a shot whose archer fell still credits it)
        float amount = (float) (base * veteran * total);
        boolean anchored = victim != null && Abilities.anchored(b, victim);
        Vec3d stood = target.getVelocity();
        boolean hurt = target.damage(source(b, attacker.mob), amount);
        if (anchored) target.setVelocity(stood); // an anchored troop (the Oath Banner's) is not knocked back by the blow itself either
        if (!hurt) return 0;
        if (!secondary) Abilities.onHit(b, attacker, target, amount, kind);
        return amount;
    }

    /** The commander classes' share of the factor: the attacker side's (Champion's infantry aura, Warden's archers, Mark Prey) times the target side's (Stand Fast). */
    static double commanderFactor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target) { return Commander.factor(b, attacker, target) * Commander.takenFactor(b, target); }

    /**
     * The attacker's stance share, per soldier, from his ability row: fearless (the Risen, the Bone Archer) deals x(1 + amount) while his own health is at or
     * below amount2 of his maximum (BattleMath.wounded), else 1; a troop that rose through Rise Again stands at 30% and so is fearless already. The
     * other stances (steady, ambush, brace) are counters rows and act through the table, not here. The row is his own (Abilities.row(u)): at level 2 the
     * fearless row's larger bonus and share (in-match progression design 6.2); the level rows that act on the table (Better guard, Bodkin points, Run them
     * down, Thick hide and the rest) do so through the tags they grant (Abilities.rowsChanged).
     */
    static double stance(GravesAndGlory.Unit attacker) {
        var row = Abilities.row(attacker);
        return row.mechanic().equals("fearless") && BattleMath.wounded(attacker.mob.getHealth(), attacker.mob.getMaxHealth(), row.amount2()) ? 1 + row.amount() : 1;
    }

    /** Whether the entity is a structure (it carries the command tag structure: the Forward Supply banner), which counters rows C10 and C11 read. */
    static boolean structure(net.minecraft.entity.Entity e) { return e.getCommandTags().contains("structure"); }

    /** The share of a commander's own blow that a structure takes: a non-siege blow on a structure, the sheet's C11. */
    static double structureBlow() {
        return CounterMath.counters(new CounterMath.Hit(Set.of("nonsiege", "melee"), Set.of("structure"), 0, false, false, false, false, false, false), Spec.COUNTERS);
    }

    /** The Unit of this battle that is the entity, or null for a player or any other entity. */
    static GravesAndGlory.Unit unitOf(GravesAndGlory.Battle b, net.minecraft.entity.Entity e) {
        for (var u : b.units) if (u.mob == e) return u;
        return null;
    }
}
