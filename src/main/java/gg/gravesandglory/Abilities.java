package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import java.util.Comparator;

/**
 * Troop abilities (spec 2, the abilities sheet). A troop's ability id names a row; the row's mechanic decides what it does. The shield is a tag
 * the shield logic and the counters table read; mend and blight behave here; the stances (steady, ambush, brace) act through the counters table;
 * every other mechanic is a no-op until the task of its House.
 * Every number comes from the row, with the combatDiscipline defaults where a row says 0.
 */
final class Abilities {
    private Abilities() {}
    static final Spec.CombatDisciplineRow D = Spec.COMBATDISCIPLINE[0];

    /** The abilities row of a troop's ability id; the sheet's none row for an id it lacks. */
    static Spec.AbilitiesRow row(Spec.TroopsRow t) {
        Spec.AbilitiesRow none = null;
        for (var a : Spec.ABILITIES) { if (a.id().equals(t.ability())) return a; if (a.id().equals("none")) none = a; }
        return none;
    }

    /** The beat's cast and aura triggers for a troop: a mender heals, a caster's spell bursts when its windup ends. enemy is the troop's current target, or null. */
    static void tick(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity enemy, long time) {
        var row = row(u.spec);
        switch (row.mechanic()) {
            case "mend" -> mend(b, u, row, time);
            case "blight" -> burst(b, u, row, time);
            default -> {} // the stances (steady, ambush, brace) act through the counters table and auras through auraFactor; every other mechanic waits for the task of its House
        }
    }

    /** A mender sends a heal to the most hurt ally in sight within the row's radius, once per cooldown. */
    static void mend(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (time < u.abilityAt) return;
        var mob = u.mob;
        double radius = row.radius() > 0 ? row.radius() : D.healRadius();
        var ally = b.units.stream().filter(v -> v.squad.team == u.squad.team && v.mob.isAlive() && v.mob.getHealth() < v.mob.getMaxHealth() && v.mob.squaredDistanceTo(mob) < radius * radius && mob.canSee(v.mob))
            .min(Comparator.comparingDouble(v -> v.mob.getHealth() / v.mob.getMaxHealth())).orElse(null);
        if (ally == null) return;
        double amount = row.amount() > 0 ? row.amount() : D.healAmount();
        Projectiles.fire(b, u, ally.mob, "mend", (float) (amount * b.campaign.damage(u)));
        u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
    }

    /** A caster's spell: the ring marks the centre while the windup runs; when it ends the burst hurts every enemy troop inside the radius and applies the row's status. */
    static void burst(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (u.spellCenter == null) return;
        double radius = row.radius() > 0 ? row.radius() : D.spellRadius();
        for (int i = 0; i < 12; i++) { double angle = i * Math.PI / 6; b.world.spawnParticles(ParticleTypes.WITCH, u.spellCenter.x + Math.cos(angle) * radius, u.spellCenter.y + .2, u.spellCenter.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0); }
        if (time < u.windupUntil) return;
        float base = (float) ((row.amount() > 0 ? row.amount() : u.spec.damage()) * b.campaign.damage(u));
        for (var v : new java.util.ArrayList<>(b.units)) if (v.squad.team != u.squad.team && v.mob.isAlive() && v.mob.squaredDistanceTo(u.spellCenter) < radius * radius) {
            Combat.resolve(b, u, v.mob, base, "blight", false);
            if (row.statusTicks() > 0 && v.mob.isAlive()) v.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, row.statusTicks(), row.statusLevel()));
        }
        u.spellCenter = null;
    }

    /**
     * The troop's attack when its cooldown is ready and its target is in reach: a caster starts its spell, a mender does not strike, anything
     * else shoots an arrow (a ranged troop) or hits (a melee one) through Combat.resolve.
     */
    static void attack(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity enemy, long time) {
        var row = row(u.spec);
        switch (row.mechanic()) {
            case "blight" -> {
                if (time < u.abilityAt) return;
                u.spellCenter = enemy.getPos();
                u.windupUntil = time + (row.windupTicks() > 0 ? row.windupTicks() : D.spellWindupTicks());
                u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
                Projectiles.bolt(b, u, u.spellCenter);
            }
            case "mend" -> {}
            default -> {
                float amount = (float) (u.spec.damage() * b.campaign.damage(u));
                // The hit is decided now; a ranged hit lands when its visible arrow arrives.
                if (u.spec.ranged()) Projectiles.fire(b, u, enemy, "arrow", amount); else Combat.resolve(b, u, enemy, amount, "melee", false);
            }
        }
    }

    /** What a landed hit sets off on its attacker's ability (pierce, cleave, slows, slams, pounce). dealt is the amount applied. Combat.resolve skips it for a secondary hit. A no-op until the Houses that use it. */
    static void onHit(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity target, float dealt, String kind) {}

    /** The reach of a troop's attack in blocks: its sheet range. Commander and trait range bonuses (siege under Entrench) will add here. */
    static double range(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return u.spec.range(); }

    /** The aura share of the factor (Oath Banner and Howl for the attacker's side, Dread for the foe's). 1 until the Houses that have auras. */
    static double auraFactor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target) { return 1; }

    /** Whether a shooter or its target stands in a Mist cloud (counters row C17). No clouds exist until the Tidecallers. */
    static boolean inMist(GravesAndGlory.Battle b, LivingEntity shooter, LivingEntity target) { return false; }
}
