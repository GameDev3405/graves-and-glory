package gg.gravesandglory;

import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Vector3f;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Troop abilities (spec 2, the abilities sheet). A troop's ability id names a row; the row's mechanic decides what it does. The shield is a tag
 * the shield logic and the counters table read; mend and blight behave here; the stances (steady, ambush, brace) act through the counters table
 * (the ability id is one of the troop's tags, and Combat.resolve sets the attacker group's effective order); fearless is a stance Combat.resolve reads
 * by the soldier's own health; pierce hits a second enemy behind the target (secondary); the Oath Banner aura plants a Banner (Battle.banners) that
 * lends its planter's army damage (auraFactor) and anchors its troops inside (anchored: no knockback, pull or Slowness from any troop source, and a slow
 * already on them is taken off). The slam (Brassjaw's) hits every other enemy within its radius of the target for the row's share, with knockback; the shell (the
 * Ironhold Mortar's) is a blight that cannot be cast at anything nearer than the row's minRange.
 * The Tidecallers' rows: the mist (a blight variant whose burst is a standing cloud, Battle.mists: it slows the enemies in it and counters row C17 halves the shots through it), the
 * harpoon (an on-hit slow with a cooldown and a pull toward the shooter), the tentacle sweep (a slam on hit with a cooldown) and the undertow (a slam on a timer that pulls and hurts).
 * The Wildmoot's rows: the pounce (x amount on the first blow after radius blocks walked since the last attack, then its cooldown: auraFactor and onHit), the ambush
 * stance (counters row C18b, from noSky: no sky over the shooter's head), the ensnare (an on-hit slow with a cooldown), the howl (the aura row on a timer: damage and
 * speed to the army's troops within its radius for its duration) and the trample (a slam on charge_contact: once per fresh Charge, on the bear's first blow under it,
 * every enemy within the radius of the bear, the one struck among them, with knockback and the bear's rear). Every number comes from the row, with the combatDiscipline defaults where a row says 0.
 * In-match progression (design 6, Task 6): a soldier's rows follow his level. row(u) is his level-2 row in place of the base from level 2 (row(spec) stays the
 * base row the cards and the sheet checks read); second(u) is his level-3 row, added from level 3 (the sheet's none row below it). tick and onHit run both rows.
 * A level-3 row on the rider trigger (ruling I1) is no second cast: it changes what the level-2 row does (a mend or the howl that also cleanses, a cloud that
 * also weakens, a shell that leaves burning ground), and one on the onkill trigger acts on a kill (Reap: onKill; Death knell: burst). The cooldowns of the
 * on-hit level-3 rows (Shield bash, Boarding rush) and the Risen Death knell raised live in this file's own store (SECOND_AT, RAISED: review note I2), never
 * on the Unit, and go with the round (clearRound). The statuses those rows lay (weaken, bleed, mark, stun, knockback) are Statuses'.
 */
final class Abilities {
    private Abilities() {}
    static final Spec.CombatDisciplineRow D = Spec.COMBATDISCIPLINE[0];

    /** The abilities rows by id, and each troop's {base, level 2, level 3} rows (levelAbilities), read once per sheet. */
    private record Index(Spec.AbilitiesRow[] abilities, Spec.LevelAbilitiesRow[] levels, Spec.TroopsRow[] troops, Map<String, Spec.AbilitiesRow> byId, Map<String, Spec.AbilitiesRow[]> byTroop, Spec.AbilitiesRow none) {}
    private static volatile Index index;
    private static Index index() {
        var i = index;
        if (i != null && i.abilities() == Spec.ABILITIES && i.levels() == Spec.LEVELABILITIES && i.troops() == Spec.TROOPS) return i;
        Map<String, Spec.AbilitiesRow> byId = new HashMap<>();
        Spec.AbilitiesRow none = null;
        for (var a : Spec.ABILITIES) { byId.putIfAbsent(a.id(), a); if (none == null && a.id().equals("none")) none = a; }
        Map<String, Spec.AbilitiesRow[]> byTroop = new HashMap<>();
        for (var t : Spec.TROOPS) byTroop.put(t.id(), new Spec.AbilitiesRow[]{byId.getOrDefault(t.ability(), none), null, null});
        for (var l : Spec.LEVELABILITIES) { var rows = byTroop.get(l.troop()); if (rows != null && (l.level() == 2 || l.level() == 3)) rows[l.level() - 1] = byId.get(l.ability()); }
        index = i = new Index(Spec.ABILITIES, Spec.LEVELABILITIES, Spec.TROOPS, byId, byTroop, none);
        return i;
    }

    /** The abilities row of a troop's ability id (the base row, as cards and the sheet checks read it); the sheet's none row for an id it lacks. */
    static Spec.AbilitiesRow row(Spec.TroopsRow t) { var i = index(); return i.byId().getOrDefault(t.ability(), i.none()); }

    /** The soldier's own ability row: his level-2 row in place of the base one from level 2 (levelAbilities), else the base row. */
    static Spec.AbilitiesRow row(GravesAndGlory.Unit u) {
        if (u.level >= 2) { var rows = index().byTroop().get(u.spec.id()); if (rows != null && rows[1] != null) return rows[1]; }
        return row(u.spec);
    }

    /** The soldier's second ability row: his level-3 row from level 3 (levelAbilities), else the sheet's none row. */
    static Spec.AbilitiesRow second(GravesAndGlory.Unit u) {
        var i = index();
        if (u.level >= 3) { var rows = i.byTroop().get(u.spec.id()); if (rows != null && rows[2] != null) return rows[2]; }
        return i.none();
    }

    /** The soldier's level-3 row when it rides on his level-2 row with this mechanic (trigger rider, ruling I1), else null. */
    static Spec.AbilitiesRow rider(GravesAndGlory.Unit u, String mechanic) {
        var two = second(u);
        return two.trigger().equals("rider") && two.mechanic().equals(mechanic) ? two : null;
    }

    /** Every counter tag an abilities row grants (abilities.tags, | joined), "none" giving none. */
    static List<String> granted(Spec.AbilitiesRow row) { return row == null || row.tags().equals("none") ? List.of() : Arrays.asList(row.tags().split("\\|")); }

    /**
     * The soldier's level changed (lane A's level up calls it, and the harness after it sets a level): his counter tags are his troop's own (ArmyMobs.tags)
     * and those his level-2 and level-3 rows grant (abilities.tags), every tag a lower level granted coming off; below level 3 his level-3 row's cooldown and
     * the Risen it raised are forgotten. The rows themselves are read from his level at every use (row, second).
     */
    static void rowsChanged(GravesAndGlory.Unit u) {
        var base = ArmyMobs.tags(u.spec);
        var all = CounterMath.grantedTags();
        u.tags.removeIf(t -> all.contains(t) && !base.contains(t));
        u.tags.addAll(granted(row(u)));
        u.tags.addAll(granted(second(u)));
        if (u.level < 3) { SECOND_AT.remove(u); RAISED.remove(u); }
    }

    /** The cooldown of each soldier's on-hit level-3 row (Shield bash, Boarding rush): the battle time it is ready. A soldier with none is ready. Review note I2. */
    static final Map<GravesAndGlory.Unit, Long> SECOND_AT = new IdentityHashMap<>();
    /** The temporary Risen each Gravecaller's Death knell raised (at most the row's amount standing at once). */
    static final Map<GravesAndGlory.Unit, List<GravesAndGlory.Unit>> RAISED = new IdentityHashMap<>();

    /** A round boundary (Statuses.clear): every level-3 cooldown and Death knell count goes. */
    static void clearRound() { SECOND_AT.clear(); RAISED.clear(); }

    /** Every 200 ticks of a running round (Statuses.tick): the soldiers who fell or left drop out of the store, and the Risen who went out of each list. */
    static void prune(GravesAndGlory.Battle b) {
        SECOND_AT.keySet().removeIf(u -> !u.mob.isAlive() || !b.units.contains(u));
        RAISED.values().forEach(l -> l.removeIf(r -> !r.mob.isAlive() || !b.units.contains(r)));
        RAISED.entrySet().removeIf(e -> e.getValue().isEmpty() && (!e.getKey().mob.isAlive() || !b.units.contains(e.getKey())));
    }

    /** The battle time a row of the soldier's is ready: his own abilityAt for his first row, the store for his level-3 row. */
    static long readyAt(GravesAndGlory.Unit u, boolean second) { return second ? SECOND_AT.getOrDefault(u, 0L) : u.abilityAt; }
    static void spend(GravesAndGlory.Unit u, boolean second, long until) { if (second) SECOND_AT.put(u, until); else u.abilityAt = until; }

    /**
     * The soldier landed a killing blow (lane A's kill path calls it; Task 4). Reap (onkill_heal, his level-3 row on the onkill trigger): he heals the row's
     * amount of his max health.
     */
    static void onKill(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        if (u == null || !u.mob.isAlive()) return;
        var two = second(u);
        if (!two.mechanic().equals("onkill_heal") || two.amount() <= 0) return;
        u.mob.heal((float) (two.amount() * u.mob.getMaxHealth()));
        b.world.spawnParticles(dust(Statuses.BLOOD), u.mob.getX(), u.mob.getBodyY(.6), u.mob.getZ(), 10, .3, .4, .3, 0);
        b.world.spawnParticles(ParticleTypes.HEART, u.mob.getX(), u.mob.getY() + u.mob.getHeight() + .2, u.mob.getZ(), 1, 0, 0, 0, 0);
    }

    /** The troop's ability row when it is a dread aura (mechanic aura, trigger passive, aimed at enemies: the Hollow King's), else null. auraFactor reads its amount. */
    static Spec.AbilitiesRow dreadRow(GravesAndGlory.Unit u) {
        var row = row(u);
        return row.mechanic().equals("aura") && row.trigger().equals("passive") && row.target().equals("enemies") ? row : null;
    }

    /** The soldier's level-3 aura that serves his own army where he stands (Furnace heart's taken, Admiral's call's amount: an aura on allies, passive), else null. */
    static Spec.AbilitiesRow allyAura(GravesAndGlory.Unit u) {
        var two = second(u);
        return two.mechanic().equals("aura") && two.trigger().equals("passive") && two.target().equals("allies") ? two : null;
    }

    /** The soldier's own rise row (the Hollow King's Undying King: mechanic rise, on his level-3 row), else null. Houses.allowDeath reads it. */
    static Spec.AbilitiesRow riseRow(GravesAndGlory.Unit u) {
        var two = second(u);
        return two.mechanic().equals("rise") ? two : null;
    }

    /**
     * The beat's cast and aura triggers for a troop: a mender heals, a caster's spell bursts when its windup ends. enemy is the troop's current target, or null.
     * His level-3 row on the beat: an aura that serves his army where he stands (Furnace heart, Admiral's call) logs each of his army's troops inside it as
     * supported (lend). A rider acts through the level-2 row's own path, and the on-hit rows in onHit.
     */
    static void tick(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity enemy, long time) {
        var row = row(u);
        switch (row.mechanic()) {
            case "mend" -> mend(b, u, row, time);
            case "blight" -> burst(b, u, row, time);
            case "aura" -> aura(b, u, row, time);
            case "mist" -> cloud(b, u, row, time);
            case "slam" -> { if (row.trigger().equals("timer")) undertow(b, u, row, time); } // the Admiral's; a slam on hit acts in onHit
            default -> {} // the stances (steady, ambush, brace) act through the counters table; the pounce, the on-hit slows (ensnare, the harpoon) and the trample act in onHit
        }
        var lend = allyAura(u);
        if (lend != null) lend(b, u, lend);
    }

    /** A level-3 aura on allies (Furnace heart, Admiral's call) serves its army's troops within its radius: each is logged as supported by its bearer (Veterans.supported: kill credit design 3.2, review note I3). */
    static void lend(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row) {
        if (!u.mob.isAlive()) return;
        double reach = row.radius() * row.radius();
        for (var v : b.units) if (v.squad == u.squad && v != u && v.mob.isAlive() && GravesAndGlory.Battle.horizontalDistance(v.mob.getPos(), u.mob.getPos()) <= reach) Veterans.supported(b, u, v.mob);
    }

    /**
     * A mender sends a heal to the most hurt ally in sight within the row's radius, once per cooldown. The heal is the row's amount times his army's progression and
     * his output factor (Veterans.outputFactor: his level and weapon tier, design 4.1). Dawnlight (a mend rider that cleanses) takes slow, weaken and bleed off the
     * ally as the heal is sent.
     */
    static void mend(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (time < u.abilityAt || time < u.stunnedUntil) return; // a stunned mender sends nothing (Shield bash, Rise Again: no attacks, no casts); her cooldown waits for the stun to end
        var mob = u.mob;
        double radius = row.radius() > 0 ? row.radius() : D.healRadius();
        var ally = b.units.stream().filter(v -> v.squad.team == u.squad.team && v.mob.isAlive() && v.mob.getHealth() < v.mob.getMaxHealth() && v.mob.squaredDistanceTo(mob) < radius * radius && mob.canSee(v.mob))
            .min(Comparator.comparingDouble(v -> v.mob.getHealth() / v.mob.getMaxHealth())).orElse(null);
        if (ally == null) return;
        double amount = row.amount() > 0 ? row.amount() : D.healAmount();
        Projectiles.fire(b, u, ally.mob, "mend", (float) (amount * b.campaign.damage(u) * Veterans.outputFactor(u)));
        Veterans.supported(b, u, ally.mob); // the mended troop is supported by the mender (kill credit, design 3.2)
        var dawnlight = rider(u, "mend");
        if (dawnlight != null && dawnlight.cleanse()) Statuses.cleanse(b, u, ally.mob);
        u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
    }

    /**
     * A caster's spell: the ring marks the centre while the windup runs; when it ends the burst hurts every enemy troop inside the radius and applies the row's status.
     * The caster's level-3 row rides on it: the Firepot (a mist rider on a blight) leaves its burning ground at the centre, and Death knell (raise) raises each
     * enemy troop the burst killed (raise).
     */
    static void burst(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (u.spellCenter == null) return;
        if (time < u.stunnedUntil) { u.spellCenter = null; return; } // a stunned caster cannot land a spell it began before the stun
        double radius = row.radius() > 0 ? row.radius() : D.spellRadius();
        for (int i = 0; i < 12; i++) { double angle = i * Math.PI / 6; b.world.spawnParticles(ParticleTypes.WITCH, u.spellCenter.x + Math.cos(angle) * radius, u.spellCenter.y + .2, u.spellCenter.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0); }
        if (time < u.windupUntil) return;
        if (u.spec.magic()) Audio.at(b, AudioCues.Cue.MAGIC_IMPACT, u.spellCenter, "m" + u.mob.getId()); // the burst lands as magic even on empty ground; the hits below share its key, so it sounds once
        float base = (float) ((row.amount() > 0 ? row.amount() : u.spec.damage()) * b.campaign.damage(u));
        var two = second(u);
        var knell = two.mechanic().equals("raise") ? two : null;
        for (var v : new java.util.ArrayList<>(b.units)) if (v.squad.team != u.squad.team && v.mob.isAlive() && v.mob.squaredDistanceTo(u.spellCenter) < radius * radius) {
            Combat.resolve(b, u, v.mob, base, "blight", false);
            if (row.statusTicks() > 0 && v.mob.isAlive() && !anchored(b, v)) v.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, row.statusTicks(), row.statusLevel()));
            if (knell != null && !v.mob.isAlive()) raise(b, u, v, knell);
        }
        for (var st : new java.util.ArrayList<>(b.structures)) if (st.team() != u.squad.team && st.isAlive() && st.squaredDistanceTo(u.spellCenter) < radius * radius) Combat.resolve(b, u, st, base, "blight", false); // a shell bursts on a supply banner too (counters row C10 for a siege troop)
        if (u.tags.contains("siege")) PropGameplay.siegeFire(b.world, u.spellCenter, radius, "a shell"); // a siege troop's shell is siege fire on a destructible prop (stake barricade)
        var firepot = rider(u, "mist");
        if (firepot != null) {
            b.mists.add(new Mist(u.squad.team, u.spellCenter, firepot.radius(), time + firepot.durationTicks(), u, firepot, null));
            Audio.at(b, AudioCues.Cue.FIREPOT_BURN, u.spellCenter, "f" + u.mob.getId());
        }
        u.spellCenter = null;
    }

    /**
     * Death knell (N6, raise): an enemy troop the caster's blight killed rises where it fell as a temporary Risen of her army for the row's durationTicks, at full
     * health, while fewer than the row's amount she raised still stand. Grave Bloom's temporary troop (Commander.temporary): it never counts toward the slot
     * check, a House tier, morale, mastery or the report's losses, goes at its time (Houses.expire) and with the round (Houses.reset). A temporary troop (a Risen
     * already) does not rise, and the one that rises leaves the death log, so Grave Bloom cannot raise it again.
     */
    static void raise(GravesAndGlory.Battle b, GravesAndGlory.Unit u, GravesAndGlory.Unit victim, Spec.AbilitiesRow row) {
        if (victim.temporary || victim.rose) return;
        var mine = RAISED.computeIfAbsent(u, k -> new ArrayList<>());
        mine.removeIf(r -> !r.mob.isAlive() || !b.units.contains(r));
        if (mine.size() >= Math.round(row.amount()) || row.durationTicks() <= 0) return;
        var spec = Campaign.troop(Commander.RISEN);
        if (spec == null) return;
        Vec3d at = victim.mob.getPos();
        var risen = Commander.temporary(b, u.squad, spec, at, b.time + row.durationTicks());
        mine.add(risen);
        long now = b.time;
        b.deaths.removeIf(d -> d.time() == now && d.troop().equals(victim.spec.id()) && d.at().squaredDistanceTo(at) < 1e-6);
        Houses.recompute(u.squad);
    }

    /**
     * The troop's attack when its cooldown is ready and its target is in reach: a caster starts its spell, a mender does not strike, anything
     * else shoots an arrow (a ranged troop) or hits (a melee one with no swings in the sheet) through Combat.resolve. A melee troop with swings strikes
     * through Tactics.swing, on its swing's impact frame: called for one here, this does nothing but log it (misrouted), so no blow lands twice or early.
     */
    static boolean attack(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity enemy, long time) {
        var row = row(u);
        switch (row.mechanic()) {
            case "blight", "mist" -> {
                if (time < u.abilityAt || tooClose(u, u.mob.squaredDistanceTo(enemy))) return false; // a shell is never lobbed at an enemy inside the row's minimum range
                u.spellCenter = enemy.getPos();
                u.windupUntil = time + (row.windupTicks() > 0 ? row.windupTicks() : D.spellWindupTicks());
                u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
                Projectiles.bolt(b, u, u.spellCenter);
                return true;
            }
            case "mend" -> { return false; }
            default -> {
                if (melee(u) && !Tactics.variants(u.spec.family()).isEmpty()) {misrouted++; LOG.warn("Abilities.attack called for {} ({}), a melee troop whose blow is Tactics.swing's: no blow", u.spec.id(), u.mob.getId()); return false;}
                animate(b, u);
                // The hit is decided now; a ranged hit lands when its visible arrow arrives.
                if (u.spec.ranged()) Projectiles.fire(b, u, enemy, visual(u), (float) baseHit(b, u)); else land(b, u, enemy);
                return true;
            }
        }
    }

    static final Logger LOG = LoggerFactory.getLogger("gravesandglory");
    /** Calls of attack for a melee troop with swings, refused (its blow is Tactics.swing's), since the server started. */
    static int misrouted;

    /** Whether the troop's attack is a melee blow (a troop not ranged whose ability row strikes rather than casts or mends): the blow Tactics lands on its swing's impact frame. */
    static boolean melee(GravesAndGlory.Unit u) {
        if (u.spec.ranged()) return false;
        return switch (row(u).mechanic()) {case "blight", "mist", "mend" -> false; default -> true;};
    }

    /** A melee troop's blow on the enemy: its base hit through Combat.resolve, which runs the on-hit abilities (attack, and Tactics.land on the impact frame). */
    static void land(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity enemy) {
        Combat.resolve(b, u, enemy, (float) baseHit(b, u), "melee", false);
    }

    /** The swing a golem or a ravager shows: the client plays it on entity status 4, which their own attack goal would send and a troop has no such goal. */
    static void animate(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        if (u.mob instanceof net.minecraft.entity.passive.IronGolemEntity || u.mob instanceof net.minecraft.entity.mob.RavagerEntity) {
            b.world.sendEntityStatus(u.mob, (byte) 4);
            BattleEffects.attackStatuses++;
        }
    }

    /**
     * The look of a ranged troop's shot, which Projectiles.fire takes as its kind: bolt for a magic troop, trident for a ranged troop whose weapon is the trident (the
     * Harpooner), arrow for the rest. The hit is resolved as an arrow, or a bolt for magic, whatever the look.
     */
    static String visual(GravesAndGlory.Unit u) {
        if (u.spec.magic()) return "bolt";
        return u.spec.ranged() && u.spec.weapon().equals("minecraft:trident") ? "trident" : "arrow";
    }

    /** The troop's ability row's minRange: the nearest an enemy may be and still be cast at (the Mortar's shell, 10); 0 for a troop with none. */
    static double minRange(GravesAndGlory.Unit u) { return row(u).minRange(); }

    /** Whether an enemy at this squared distance is inside the troop's minimum range. */
    static boolean tooClose(GravesAndGlory.Unit u, double distanceSquared) { double min = minRange(u); return min > 0 && distanceSquared < min * min; }

    /** A troop's base hit: its sheet damage times its army's progression (and every later damage bonus that belongs to the hit itself), before counters, auras and factors. attack and secondary both start here. */
    static double baseHit(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return u.spec.damage() * b.campaign.damage(u); }

    /**
     * What a landed hit sets off on its attacker's ability: pierce hits the nearest other enemy within the row's radius behind the target for the
     * row's amount (secondary); cleave hits the nearest other enemy within the row's radius of the target, any side of it, for the row's amount
     * (secondary); an onhit_status row lays its status on the target, once per its cooldown (a slow through slow, with the Harpoon's pull; weaken, bleed, stun and
     * knockback through Statuses); the pounce starts its cooldown and ends its run (pounce); a slam on hit with a radius sweeps (slam), one with none is a Shield bash
     * on the target alone (bash); a slam on charge_contact tramples (trample); a mark marks the target for the attacker's army (Statuses). The soldier's level-3 row
     * on hit acts after his own row, on its own cooldown (SECOND_AT). dealt is the amount applied. Combat.resolve skips it for a secondary hit.
     */
    static void onHit(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity target, float dealt, String kind) {
        act(b, u, target, kind, row(u), false);
        var two = second(u);
        if (two.trigger().equals("onhit")) act(b, u, target, kind, two, true);
        var riptide = Houses.riptide(u);
        if (riptide != null && target.isAlive() && !Combat.structure(target)) slow(b, u, target, SLOWNESS_I, riptide.durationTicks()); // Riptide: every hit of a Tidecaller slows; with brine or a harpoon the longer duration stands
    }

    /** One row's part of a landed hit (onHit); second says it is the soldier's level-3 row, whose cooldown is the store's. */
    static void act(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity target, String kind, Spec.AbilitiesRow row, boolean second) {
        switch (row.mechanic()) {
            case "pierce" -> secondary(b, u, target, row.radius(), row.amount(), true, kind);
            case "cleave" -> secondary(b, u, target, row.radius(), row.amount(), false, kind);
            case "onhit_status" -> {
                if (!target.isAlive() || Combat.structure(target) || !offCooldown(b, u, row, second)) return;
                if (row.status().equals("slow")) {
                    slow(b, u, target, row);
                    if (row.knockback() < 0) pull(b, u, target, u.mob.getPos(), -row.knockback()); // the Harpooner's: a negative knockback drags the target toward the shooter
                } else Statuses.apply(b, u, target, row);
            }
            case "slam" -> {
                if (row.trigger().equals("onhit")) { if (row.radius() > 0) slam(b, u, target, row, second); else bash(b, u, target, row, second); }
                else if (row.trigger().equals("charge_contact")) trample(b, u, target, row);
            }
            case "pounce" -> pounce(b, u, row, kind, second);
            case "mark" -> { if (target.isAlive() && !Combat.structure(target)) Statuses.apply(b, u, target, "mark", row.durationTicks(), 0, row.amount()); }
            default -> {}
        }
    }

    /**
     * Shield bash (N4, a slam on hit with radius 0): once per the row's cooldown, the struck target alone is thrown back by the row's knockback and stunned for
     * its statusTicks (Statuses: a troop is stunned, a commander is only thrown; an anchored troop is not moved but still stunned).
     */
    static void bash(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity target, Spec.AbilitiesRow row, boolean second) {
        if (!target.isAlive() || Combat.structure(target) || !offCooldown(b, u, row, second)) return;
        Statuses.apply(b, u, target, "knockback", 0, 0, row.knockback());
        Statuses.apply(b, u, target, row.status().equals("none") ? "stun" : row.status(), row.statusTicks(), row.statusLevel(), row.amount());
        Vec3d struck = target.getPos();
        Audio.later(b, Audio.contact(u), () -> Audio.at(b, AudioCues.Cue.SHIELD_BLOCK, struck, "bash" + u.mob.getId(), .8f)); // the shield meets him at the swing's contact frame, with the blow
    }

    /** Slowness I is the amplifier 0 of Riptide's slow (the spec's fixed effect kind); the abilities rows carry their own statusLevel. */
    static final int SLOWNESS_I = 0;

    /**
     * Whether an on-hit row may fire now. A row with a cooldown (the Harpoon's 60 ticks, Ensnare's 80) fires once and then waits it out, on the troop's own
     * abilityAt, or for his level-3 row (second) on the store (SECOND_AT); a row with none (brine, frostshot) always fires.
     */
    static boolean offCooldown(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, boolean second) {
        if (row.cooldownTicks() <= 0) return true;
        if (b.time < readyAt(u, second)) return false;
        spend(u, second, b.time + row.cooldownTicks());
        return true;
    }

    /**
     * Brassjaw's slam: the row's amount of the attacker's base hit to every other enemy within the row's radius of the target (secondary hits, so their
     * own counters, traits, auras and commander factors apply and no on-hit ability runs again), and each one that took it and lives is thrown back
     * from the target's spot with the row's knockback (a structure is not moved). For either of the soldier's rows (second: his level-3 row, on the store's cooldown).
     */
    static void slam(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity primary, Spec.AbilitiesRow row, boolean second) {
        if (row.cooldownTicks() > 0 && b.time < readyAt(u, second)) return; // the Brute's sweep: at most once per cooldown
        Vec3d at = primary.getPos();
        int swept = splash(b, u, at, primary, (float) (row.amount() * baseHit(b, u)), row.radius(), row.knockback(), u.mob.getPos());
        if (swept > 0 && row.cooldownTicks() > 0) spend(u, second, b.time + row.cooldownTicks()); // a sweep that struck nobody keeps its cooldown for the next hit
        b.world.spawnParticles(ParticleTypes.CLOUD, at.x, at.y + .2, at.z, 16, row.radius() / 2, .1, row.radius() / 2, .06);
        // The slam sounds at the swing's contact frame under the blow's own key, so the primary's heavy thud and the slam are one sound, not two.
        Vec3d slammed = primary.getPos();
        Audio.later(b, Audio.contact(u), () -> Audio.at(b, AudioCues.Cue.HEAVY_METAL_HIT, slammed, "h" + u.mob.getId(), .8f));
    }

    /**
     * The splash of a slam or a trample: every enemy within the radius of the spot (but `except`: the slam's primary, which its own blow struck) takes base as a
     * secondary slam hit (its own counters, traits, auras and commander factors apply and no on-hit ability runs again), each that took it flashes, and each that
     * took it and lives is thrown from the spot with the knockback (a structure is not moved; `fallback` gives the direction for one standing on the spot).
     * Returns how many took it.
     */
    static int splash(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Vec3d at, LivingEntity except, float base, double radius, double knockback, Vec3d fallback) {
        int struck = 0;
        double reach = radius * radius;
        PropGameplay.slam(b.world, at, radius); // a heavy hit on a destructible prop in reach (stake barricade)
        for (var e : enemies(b, u.squad.team)) {
            if (e == except || e.squaredDistanceTo(at) > reach) continue;
            float applied = Combat.resolve(b, u, e, base, "slam", true);
            if (applied <= 0) continue;
            struck++;
            BattleEffects.flash(b, new Vec3d(e.getX(), e.getBodyY(.6), e.getZ()), u.squad.team);
            if (e.isAlive() && !Combat.structure(e)) knock(b, e, at, fallback, knockback);
        }
        return struck;
    }

    /**
     * Old Horn's trample (the slam row on charge_contact): once per fresh Charge, on his first blow under it with his cooldown up (Unit.trampledCharge holds the
     * Charge stamp, Squad.chargedAt of his group, he last trampled under), every enemy within the row's radius of him, the one his blow struck among them, takes
     * the row's amount (times his army's progression) as a secondary slam hit, which their counters and traits shape, and each that lives is thrown from his spot
     * with the row's knockback (a structure is not moved); the cooldown starts and he rears (ArmyBear.rear: the warning pose for its pulse, his attack animation)
     * in a cloud of dust. The order read is the one his blow's swing was judged under (Combat.charge). On Hold, under a Charge order with no stamp or one he trampled under already, or inside the cooldown, his blow is a bite alone. The
     * trample lands with the blow that makes contact, not before it: the struck enemy took the bite this tick, so its hurt cooldown is cleared first, or vanilla
     * would swallow the smaller blow.
     */
    static void trample(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity primary, Spec.AbilitiesRow row) {
        if (!Combat.charge(b, u, true).charging() || b.time < u.abilityAt) return; // the order his blow's swing was judged under (Ruling R29)
        Long charged = u.squad.chargedAt.get(u.spec.group());
        if (charged == null || u.trampledCharge >= charged) return;
        u.trampledCharge = charged;
        u.abilityAt = b.time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
        Vec3d at = u.mob.getPos();
        primary.timeUntilRegen = 0;
        splash(b, u, at, null, (float) (row.amount() * b.campaign.damage(u)), row.radius(), row.knockback(), primary.getPos());
        if (u.mob instanceof GravesAndGlory.ArmyBear bear) bear.rear();
        b.world.spawnParticles(ParticleTypes.CLOUD, at.x, at.y + .2, at.z, 20, row.radius() / 2, .1, row.radius() / 2, .06);
        Audio.at(b, AudioCues.Cue.BRUTE_ATTACK, at, "t" + u.mob.getId());
    }

    /** Whether a pounce troop's next blow is the pounce: its row's mechanic, the row's radius or more blocks walked since its last attack (Unit.travelSinceAttack), and its cooldown up. For either of the soldier's rows (second: his level-3 row, the Drowned Raider's Boarding rush, on the store's cooldown). auraFactor and pounce read it. */
    static boolean pounceReady(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, boolean second) {
        return row.mechanic().equals("pounce") && u.travelSinceAttack >= row.radius() && b.time >= readyAt(u, second);
    }

    /** Whether either of the soldier's rows is a pounce, so his run since his last attack is measured (unitTick). */
    static boolean pounces(GravesAndGlory.Unit u) { return row(u).mechanic().equals("pounce") || second(u).mechanic().equals("pounce"); }

    /** A blow of a pounce troop landed: when it was the pounce (its x amount was auraFactor's) the cooldown starts, the hound growls and clay dust bursts at its feet; either way its run ends (travelSinceAttack back to 0). For either of the soldier's rows (second: his level-3 row, on the store's cooldown). */
    static void pounce(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, String kind, boolean second) {
        if (kind.equals("melee") && pounceReady(b, u, row, second)) {
            spend(u, second, b.time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks()));
            Audio.at(b, AudioCues.Cue.HOUND_ATTACK, u.mob.getPos(), "p" + u.mob.getId());
            b.world.spawnParticles(dust(CLAY), u.mob.getX(), u.mob.getY() + .2, u.mob.getZ(), 12, .5, .1, .5, 0);
        }
        u.travelSinceAttack = 0;
    }

    /** Throws an entity away from a spot (the attacker's own, for one standing on it) with vanilla's knockback of that strength, which its knockback resistance reduces. An anchored troop is not moved. */
    static void knock(GravesAndGlory.Battle b, LivingEntity e, Vec3d from, Vec3d fallback, double strength) {
        if (anchored(b, e)) return;
        double dx = from.x - e.getX(), dz = from.z - e.getZ();
        if (dx * dx + dz * dz < 1e-6) { dx = fallback.x - e.getX(); dz = fallback.z - e.getZ(); }
        e.takeKnockback(strength, dx, dz);
        e.velocityModified = true;
    }

    /** Every living enemy of a team that a blow can fall on: the other side's troops, its seated commanders in the field and its structures. */
    static List<LivingEntity> enemies(GravesAndGlory.Battle b, int team) {
        List<LivingEntity> out = new ArrayList<>();
        for (var v : b.units) if (v.squad.team != team && v.mob.isAlive()) out.add(v.mob);
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            var other = b.squads.get(p.getUuid());
            if (other != null && other.seated && other.team != team && p.isAlive() && !p.isSpectator() && p.getWorld() == b.world) out.add(p);
        }
        for (var st : b.structures) if (st.team() != team && st.isAlive()) out.add(st);
        return out;
    }

    /**
     * The row's slow on a target: Slowness at the row's statusLevel for its statusTicks, unless the target already has that level or more for
     * longer (the longer one stands). A row's own cooldown (Ensnare's, the Harpoon's) is offCooldown's, and the Harpoon's pull is onHit's (pull).
     */
    static void slow(GravesAndGlory.Battle b, LivingEntity target, Spec.AbilitiesRow row) { slow(b, null, target, row); }

    /** The row's slow by a troop: from, when not null, is logged as a helper on the target (Veterans.helped: kill credit, design 3.2) whenever the target can be slowed. */
    static void slow(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity target, Spec.AbilitiesRow row) { slow(b, from, target, row.statusLevel(), row.statusTicks()); }

    /** Slowness at an amplifier for some ticks, unless the target already has that level or more for as long or longer (the longer one stands: brine against Riptide, a harpoon against either). An anchored troop takes none. */
    static void slow(GravesAndGlory.Battle b, LivingEntity target, int level, int ticks) { slow(b, null, target, level, ticks); }

    /** The same, by a troop: from, when not null, is logged as a helper on a target that can be slowed (Veterans.helped), even when a longer slow already stands on it. */
    static void slow(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity target, int level, int ticks) {
        if (anchored(b, target)) return;
        if (from != null) Veterans.helped(b, from, target);
        var have = target.getStatusEffect(StatusEffects.SLOWNESS);
        if (have != null && have.getAmplifier() >= level && have.getDuration() >= ticks) return;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, level));
    }

    /** The nearest a pull brings a target to its puller: a body's width and a half, so nothing is dragged into the one that drags it. */
    static final double PULL_STOP = 1.5;

    /**
     * Drags an entity toward a spot, along the ground, by up to blocks (less its knockback resistance, and never closer than PULL_STOP to the spot):
     * the Harpoon's 1 block and the Undertow's 3. A troop is moved at once with a collision-checked step (a wall stops it where it meets it) and its velocity is
     * cleared, which also cancels the knockback the blow that pulled it gave (a sliding pull would be undone by it); a commander, whose position his client owns,
     * gets the velocity that carries him about that far. A structure is never moved, and nor is an anchored troop.
     */
    static void pull(GravesAndGlory.Battle b, LivingEntity e, Vec3d toward, double blocks) { pull(b, null, e, toward, blocks); }

    /** The same pull by a troop: from, when not null, is logged as a helper on a target that can be pulled (Veterans.helped: kill credit, design 3.2). */
    static void pull(GravesAndGlory.Battle b, GravesAndGlory.Unit from, LivingEntity e, Vec3d toward, double blocks) {
        if (Combat.structure(e) || blocks <= 0 || anchored(b, e)) return;
        if (from != null) Veterans.helped(b, from, e);
        double dx = toward.x - e.getX(), dz = toward.z - e.getZ(), distance = Math.hypot(dx, dz);
        var resist = e.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        double step = Math.min(blocks * (1 - (resist == null ? 0 : resist.getValue())), distance - PULL_STOP);
        if (step < 0.05 || distance < 1e-6) return;
        Vec3d by = new Vec3d(dx / distance * step, 0, dz / distance * step);
        if (e instanceof ServerPlayerEntity) e.setVelocity(by.multiply(PLAYER_PULL));
        else { e.move(MovementType.SELF, by); e.setVelocity(0, e.getVelocity().y, 0); }
        e.velocityModified = true;
    }

    /** A walking body's horizontal velocity carries it about 2.2 times that far over the ticks that follow, on ordinary ground (the friction series 1 / (1 - 0.546)): what a commander's pull is scaled by. */
    static final double PLAYER_PULL = 0.454;

    /**
     * A second target for pierce and cleave: the nearest other living enemy (a troop, or a seated enemy commander in the field) within radius
     * of the primary target. With behind, only one beyond the target on the shot line: inside the cone of half-angle
     * 180 - combatDiscipline.flankArcDegrees around the direction from the attacker through the target, the flank arc's "behind" that counters
     * row C16 reads. It takes share x the attacker's base hit (its sheet damage times its progression) through Combat.resolve as a
     * secondary hit, so its own counters, traits, auras and commander factors apply once and no on-hit ability runs again. Returns the
     * amount applied, 0 when there was no second target or it took nothing.
     */
    static float secondary(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity primary, double radius, double share, boolean behind, String kind) {
        Vec3d at = primary.getPos();
        double dx = at.x - u.mob.getX(), dz = at.z - u.mob.getZ(), length = Math.hypot(dx, dz), cone = Math.cos(Math.toRadians(180 - CounterMath.flankDegrees()));
        List<LivingEntity> enemies = enemies(b, u.squad.team);
        LivingEntity best = null;
        double nearest = radius * radius;
        for (var e : enemies) {
            if (e == primary) continue;
            double d = e.squaredDistanceTo(primary);
            if (d > nearest) continue;
            if (behind) {
                double vx = e.getX() - at.x, vz = e.getZ() - at.z, v = Math.hypot(vx, vz);
                if (length < 1e-9 || v < 1e-9 || (vx * dx + vz * dz) / (v * length) <= cone) continue;
            }
            best = e;
            nearest = d;
        }
        if (best == null) return 0;
        float applied = Combat.resolve(b, u, best, (float) (share * baseHit(b, u)), kind, true);
        if (applied > 0) BattleEffects.flash(b, new Vec3d(best.getX(), best.getBodyY(.6), best.getZ()), u.squad.team);
        return applied;
    }

    /**
     * The reach of a troop's attack in blocks: its sheet range, times 1 + the Entrench
     * tier's value2 for a siege-tag troop of Ironclad that stands entrenched (Houses.still).
     */
    static double range(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        double reach = u.spec.range();
        var entrench = Houses.entrench(u);
        return entrench != null && u.tags.contains("siege") && Houses.still(u) ? reach * (1 + entrench.value2()) : reach;
    }

    /**
     * The aura share of the factor: x(1 + amount) for an attacker of the planting army standing within the radius of its standing banner
     * (the Oath Banner), the largest one only when banners overlap, times x(1 - amount) while a living enemy troop with a dread row (the Hollow
     * King) stands within its radius, the strongest one only, times x(1 + amount) of the Howl on the attacker while it lasts (howl), times the pounce's x amount
     * when the attacker's blow is the pounce of either of his rows (pounceReady). The level-3 auras on allies (allyAura): x(1 + amount) for an attacker within the
     * radius of a living troop of his own army that bears Admiral's call (the strongest one only), and x(1 - taken) for a target within the radius of one of its own
     * army that bears Furnace heart (the strongest one only): the damage an own troop takes (N11). A commander struck by a troop is no troop of an army here, so
     * Furnace heart never covers him.
     */
    static double auraFactor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target) {
        double f = 1;
        for (var banner : b.banners) if (covers(b, banner, attacker.squad, attacker.mob.getPos())) f = Math.max(f, 1 + banner.amount());
        if (b.time < attacker.howlUntil) f *= 1 + attacker.howlAmount;
        var own = row(attacker);
        if (pounceReady(b, attacker, own, false)) f *= own.amount();
        var two = second(attacker);
        if (pounceReady(b, attacker, two, true)) f *= two.amount();
        var victim = target == null ? null : Combat.unitOf(b, target);
        double dread = 1, call = 1, heart = 1;
        for (var e : b.units) {
            if (!e.mob.isAlive()) continue;
            if (e.squad.team != attacker.squad.team) {
                var row = dreadRow(e);
                if (row != null && GravesAndGlory.Battle.horizontalDistance(e.mob.getPos(), attacker.mob.getPos()) <= row.radius() * row.radius()) dread = Math.min(dread, 1 - row.amount());
            }
            var aura = e.level >= 3 ? allyAura(e) : null;
            if (aura == null) continue;
            double reach = aura.radius() * aura.radius();
            if (aura.amount() > 0 && e.squad == attacker.squad && GravesAndGlory.Battle.horizontalDistance(e.mob.getPos(), attacker.mob.getPos()) <= reach) call = Math.max(call, 1 + aura.amount());
            if (aura.taken() > 0 && victim != null && e.squad == victim.squad && GravesAndGlory.Battle.horizontalDistance(e.mob.getPos(), victim.mob.getPos()) <= reach) heart = Math.min(heart, 1 - aura.taken());
        }
        return f * call * dread * heart;
    }

    /**
     * A planted banner (the Lord Marshal's Oath Banner): the army (squad) whose troop planted it, and so the only one it serves (its aura and its
     * anchoring, as Bulwark and Stand Fast serve their own army: an allied army of the same team gets none), where it stands, the battle time it
     * falls, the radius of its aura and anchoring, the damage share it adds (the row's amount) and whether it anchors (the row's anchors). block is
     * the banner block it set, null when no spot was free, and was the block state that block replaced, put back when it falls. Battle.banners keeps the standing ones.
     */
    public record Banner(GravesAndGlory.Squad squad, Vec3d at, long until, double radius, double amount, boolean anchors, BlockPos block, BlockState was, GravesAndGlory.Unit by) {
        /** A banner no troop planted (the harness's): it supports nobody for kill credit. */
        public Banner(GravesAndGlory.Squad squad, Vec3d at, long until, double radius, double amount, boolean anchors, BlockPos block, BlockState was) { this(squad, at, until, radius, amount, anchors, block, was, null); }
        /** A banner with no damage share, no anchoring and no block. */
        public Banner(GravesAndGlory.Squad squad, Vec3d at, long until, double radius) { this(squad, at, until, radius, 0, false, null, null, null); }
        /** The team its army fights for. */
        public int team() { return squad.team; }
    }

    /** Whether the banner stands now and serves this army at this spot (horizontal distance, as Morale measures). */
    static boolean covers(GravesAndGlory.Battle b, Banner banner, GravesAndGlory.Squad s, Vec3d at) {
        return banner.squad() == s && b.time < banner.until() && GravesAndGlory.Battle.horizontalDistance(at, banner.at()) <= banner.radius() * banner.radius();
    }

    /**
     * Whether the troop is anchored: a standing banner of its own army that anchors (the Oath Banner) covers where it stands. An anchored troop is not
     * knocked back, pulled or slowed by any troop source: the blow itself (Combat.resolve), a slam (knock), a harpoon or the undertow (pull), and every
     * Slowness (slow, the blight's and the mist's). Stuns, weaken and bleed still land: immovable is about being moved.
     */
    static boolean anchored(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        if (b.banners.isEmpty()) return false;
        for (var banner : b.banners) if (banner.anchors() && covers(b, banner, u.squad, u.mob.getPos())) return true;
        return false;
    }

    /** The same for any entity: a troop of the battle may be anchored; a commander, a structure or anything else never is. */
    static boolean anchored(GravesAndGlory.Battle b, LivingEntity e) {
        if (b.banners.isEmpty()) return false;
        var u = Combat.unitOf(b, e);
        return u != null && anchored(b, u);
    }

    /** The centre of a squad's group, from the same members Morale counts (its living troops that are not temporary), or null when it has none. */
    static Vec3d centre(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) {
        List<GravesAndGlory.Unit> members = Morale.members(b, s, group);
        return members.isEmpty() ? null : Morale.centre(members);
    }

    /**
     * An aura troop's beat. The Oath Banner (trigger hold) plants when the troop's group order, the stored one its commander gave, has turned
     * to Hold since its last beat and its cooldown is ready; the order it finds on its first beat is only remembered, so a round that starts
     * on Hold plants nothing. Dread (trigger passive) is not planted on a beat: it acts through auraFactor. The Howl (trigger timer) sounds whenever its cooldown is up (howl).
     * A stun (Unit.stunnedUntil) stops the casts as it stops the attacks: the banner plant, the howl, the mend and the spells wait it out. The passive auras
     * (dread, Furnace heart, Admiral's call) are the bearer's presence, not a cast, and still stand while he is stunned (Task 7 ruling on the Task 6 review M2).
     */
    static void aura(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        switch (row.trigger()) {
            case "hold" -> {
                if (u.seenOrder == null) u.seenOrder = b.assignment(u).order; // the order found on his first beat is remembered even in a stun, so a Hold given during it plants when it ends (review M6)
                if (time < u.stunnedUntil) return; // a stunned Marshal plants nothing; the order he has not seen yet waits, so a Hold given during the stun plants when it ends
                String order = b.assignment(u).order, was = u.seenOrder; // his own order, a custom group's included (Task 12 fix I2)
                u.seenOrder = order;
                if (was != null && !was.equals(order) && order.equals("hold") && time >= u.abilityAt) plant(b, u, row, time);
            }
            case "timer" -> howl(b, u, row, time);
            default -> {}
        }
    }

    /** The clay of the Wildmoot's tells (#C8B48A): the Seer's howl ring and the hound's pounce dust. */
    static final int CLAY = 0xC8B48A;

    /** The fixed id of the Howl's speed modifier on a troop, so a new one always replaces the old. */
    static final UUID HOWL = UUID.fromString("5b6f0c3e-2a7d-4c3b-9f1e-7d2a4b8c6e01");

    /**
     * The Seer's Howl (the aura row on a timer): when its cooldown is up and the Seer is not stunned, every living troop of its own army within the row's radius
     * (itself included) gets +amount damage (auraFactor) and +amount2 speed (hasten: the attribute share that makes a real x(1 + amount2) on its ground, kept
     * by unitTick) for the row's durationTicks; the cooldown starts, the Seer howls and a ring of clay dust marks the radius. A Howl always spends its cooldown:
     * the Seer is always within its own reach. Rousing howl (an aura rider that cleanses, on the Seer's level-3 row) also takes slow, weaken and bleed off each
     * troop the howl reaches.
     */
    static void howl(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (time < u.abilityAt || time < u.stunnedUntil) return;
        u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
        double reach = row.radius() * row.radius();
        var rousing = rider(u, "aura");
        boolean cleanse = rousing != null && rousing.cleanse();
        for (var v : b.units) {
            if (v.squad != u.squad || !v.mob.isAlive() || GravesAndGlory.Battle.horizontalDistance(v.mob.getPos(), u.mob.getPos()) > reach) continue;
            v.howlUntil = time + row.durationTicks(); v.howlAmount = row.amount(); v.howlSpeed = row.amount2();
            hasten(v);
            Veterans.supported(b, u, v.mob); // each howled troop is supported by the Seer (kill credit, design 3.2; the Seer himself is not)
            if (cleanse) Statuses.cleanse(b, u, v.mob);
        }
        b.world.playSound(null, u.mob.getBlockPos(), SoundEvents.ENTITY_WOLF_HOWL, SoundCategory.HOSTILE, 1.5f, .8f);
        ring(b, u.mob.getPos(), row.radius(), CLAY);
    }

    /** The Howl's speed modifier on a troop: BattleMath.speedModifier's value for x(1 + howlSpeed) on its ground, kept by keepSpeed (as Muster's hasten). */
    static void hasten(GravesAndGlory.Unit u) { keepSpeed(u.mob, HOWL, "Graves and Glory howl", BattleMath.speedModifier(1 + u.howlSpeed, u.mob.isTouchingWater())); }

    /**
     * Keeps one speed modifier (by its id) on a mob at a value: added where it lacks, replaced where the value changed (the ground did: BattleMath.speedModifier
     * differs on land and in water), left alone where it stands at that value already, and only taken off for a value of 0. The Howl (hasten), Muster
     * (RelicEffects.Muster.hasten) and Riptide (Houses.swim) keep theirs through it; a mob without the attribute is left alone.
     */
    static void keepSpeed(LivingEntity mob, UUID id, String name, double value) {
        var speed = mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed == null) return;
        var have = speed.getModifier(id);
        if (have != null && Math.abs(have.getValue() - value) < 1e-9) return;
        if (have != null) speed.removeModifier(id);
        if (value != 0) speed.addTemporaryModifier(new EntityAttributeModifier(id, name, value, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /**
     * Every tick of every troop, before Tactics (Battle.unitTick): a pounce troop's run since its last attack grows by the ground it covered (horizontal), and a
     * howled troop's speed share is kept on its ground and taken off when its Howl ends.
     */
    static void unitTick(GravesAndGlory.Battle b, GravesAndGlory.Unit u, long time) {
        if (pounces(u)) {
            Vec3d at = u.mob.getPos();
            if (u.travelAt != null) u.travelSinceAttack += Math.sqrt(GravesAndGlory.Battle.horizontalDistance(at, u.travelAt));
            u.travelAt = at;
        }
        if (u.howlUntil > 0) {
            if (time < u.howlUntil) hasten(u);
            else { u.howlUntil = 0; var speed = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED); if (speed != null) speed.removeModifier(HOWL); }
        }
    }

    /** The ambush stance's condition (counters row C18b, Hit.noSky): no sky over the shooter's head, the block above its feet. A roof one block up is enough; the open is not. */
    static boolean noSky(GravesAndGlory.Unit u) { return !u.mob.getWorld().isSkyVisible(u.mob.getBlockPos().up()); }

    /**
     * Plants the Oath Banner: a standing banner block in the nearest dye to its team's colour, one step behind the troop against its group's
     * facing (or where it stands; no block when neither spot is free), that stands for the row's durationTicks with the row's radius and
     * amount, anchoring its army's troops inside (a slow already on one is taken off at once). No horn sounds (no horn is heard in the field), and the cooldown starts now.
     */
    static void plant(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
        float yaw = b.assignment(u).yaw;
        BlockPos block = spot(b, u, yaw);
        BlockState was = null;
        Vec3d at = u.mob.getPos();
        if (block != null) {
            was = b.world.getBlockState(block);
            b.world.setBlockState(block, BannerBlock.getForColor(dye(b.side(u.squad.team).color())).getDefaultState().with(BannerBlock.ROTATION, RotationPropertyHelper.fromYaw(yaw)));
            at = new Vec3d(block.getX() + .5, block.getY(), block.getZ() + .5);
        }
        var banner = new Banner(u.squad, at, time + row.durationTicks(), row.radius(), row.amount(), row.anchors(), block, was, u);
        b.banners.add(banner);
        b.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1, at.z, 16, .5, .8, .5, 0);
        anchor(b, banner);
        support(b, banner);
    }

    /** One step behind the troop, against the facing yaw, then the spot it stands on: the first that is open (air, or a plant that is not two blocks tall), with air above, firm ground below, within 1.5 blocks of its feet and no other banner. Null when neither is. */
    static BlockPos spot(GravesAndGlory.Battle b, GravesAndGlory.Unit u, float yaw) { return spot(b, u.mob.getPos(), yaw); }

    /** The same for any pair of feet (a commander's, for the Forward Supply banner): a spot also keeps clear of the block of a standing supply banner. */
    static BlockPos spot(GravesAndGlory.Battle b, Vec3d feet, float yaw) {
        double r = Math.toRadians(yaw);
        for (Vec3d at : List.of(feet.add(Math.sin(r) * 1.5, 0, -Math.cos(r) * 1.5), feet)) {
            int x = (int) Math.floor(at.x), z = (int) Math.floor(at.z), y = b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (Math.abs(y - feet.y) > 1.5 || b.banners.stream().anyMatch(banner -> p.equals(banner.block())) || b.structures.stream().anyMatch(st -> p.equals(st.block))) continue;
            BlockState state = b.world.getBlockState(p);
            boolean open = state.isAir() || state.isReplaceable() && state.getFluidState().isEmpty() && !(state.getBlock() instanceof TallPlantBlock);
            if (open && b.world.getBlockState(p.up()).isAir() && b.world.getBlockState(p.down()).isSideSolidFullSquare(b.world, p.down(), Direction.UP)) return p;
        }
        return null;
    }

    /** The dye nearest an RGB colour (a banner block has only the sixteen). */
    static DyeColor dye(int rgb) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, blue = (rgb & 255) / 255f;
        DyeColor best = DyeColor.WHITE;
        double nearest = Double.MAX_VALUE;
        for (var d : DyeColor.values()) {
            float[] c = d.getColorComponents();
            double e = (c[0] - r) * (c[0] - r) + (c[1] - g) * (c[1] - g) + (c[2] - blue) * (c[2] - blue);
            if (e < nearest) { nearest = e; best = d; }
        }
        return best;
    }

    /** Ring particles on a banner's edge (a presentation number). */
    static final int RING = 32;

    /** The banner's beat in ticks: how often a standing banner takes the Slowness off its army's troops inside and shows its particles (a presentation number; the radius and the anchoring are the row's). */
    static final int BEAT = 10;

    /** Every tick of a running round: a banner falls once its time is up (its block goes back); while it stands it anchors its army's troops in its radius (a slow on one goes) and shows its particles, every BEAT ticks. */
    static void bannerTick(GravesAndGlory.Battle b) {
        mistTick(b);
        if (b.banners.isEmpty()) return;
        for (var banner : new ArrayList<>(b.banners)) {
            if (b.time >= banner.until()) { lower(b, banner); b.banners.remove(banner); continue; }
            if (b.time % BEAT == 0) { anchor(b, banner); show(b, banner); support(b, banner); }
        }
    }

    /** The banner buffs its army's troops inside its radius: each is logged as supported by the troop that planted it (Veterans.supported: kill credit, design 3.2), at the plant and every BEAT while it stands. Nothing for a banner no troop planted. */
    static void support(GravesAndGlory.Battle b, Banner banner) {
        if (banner.by() == null) return;
        for (var u : b.units) if (u.squad == banner.squad() && u.mob.isAlive() && covers(b, banner, u.squad, u.mob.getPos())) Veterans.supported(b, banner.by(), u.mob);
    }

    /** The anchoring's sweep: the Slowness off every living troop of the banner's own army that stands in its radius, with a burst of particles at each. The plant sweeps once at once (a slow already on a troop goes); the beat repeats it while the banner stands, so a slowed troop that walks in is freed within BEAT ticks. Nothing for a banner that does not anchor. */
    static void anchor(GravesAndGlory.Battle b, Banner banner) {
        if (!banner.anchors()) return;
        var s = banner.squad();
        for (var u : b.units) {
            if (u.squad != s || !u.mob.isAlive() || !u.mob.hasStatusEffect(StatusEffects.SLOWNESS) || !covers(b, banner, s, u.mob.getPos())) continue;
            u.mob.removeStatusEffect(StatusEffects.SLOWNESS);
            b.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, u.mob.getX(), u.mob.getY() + 1, u.mob.getZ(), 6, .4, .5, .4, 0);
        }
    }

    /** The banner's look while it stands: a ring of its team's colour on the ground at the edge of its radius, and sparks and colour rising at its head. */
    static void show(GravesAndGlory.Battle b, Banner banner) {
        int rgb = b.side(banner.team()).color();
        ring(b, banner.at(), banner.radius(), rgb);
        Vec3d at = banner.at();
        b.world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y + 2.1, at.z, 3, .3, .2, .3, .02);
        b.world.spawnParticles(dust(rgb), at.x, at.y + 1.2, at.z, 4, .3, .6, .3, 0);
    }

    /** A ring of RING dust particles in a colour on the ground at radius around a spot: the reach of a banner or of a commander's active. */
    static void ring(GravesAndGlory.Battle b, Vec3d at, double radius, int rgb) {
        var dust = dust(rgb);
        for (int i = 0; i < RING; i++) {
            double angle = i * 2 * Math.PI / RING, x = at.x + Math.cos(angle) * radius, z = at.z + Math.sin(angle) * radius;
            b.world.spawnParticles(dust, x, b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) + .2, z, 1, 0, 0, 0, 0);
        }
    }

    /** Dust particles in an RGB colour. */
    static DustParticleEffect dust(int rgb) { return new DustParticleEffect(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 1.2f); }

    /** Takes a banner's block down: the block it replaced goes back, if the banner block still stands there. */
    static void lower(GravesAndGlory.Battle b, Banner banner) {
        if (banner.block() != null && b.world.getBlockState(banner.block()).getBlock() instanceof BannerBlock)
            b.world.setBlockState(banner.block(), banner.was() == null ? Blocks.AIR.getDefaultState() : banner.was());
    }

    /**
     * Every banner falls now, the Oath Banners and the Forward Supply banners, and every Mist cloud goes (startRound, returnLobby, switchMap, and the server
     * stopping). It leaves the statuses and the level-3 cooldown stores alone: a round boundary empties those once, through Battle.clearProgression
     * (Statuses.clear), and the server stopping lets them go in serverStopping.
     */
    static void clearBanners(GravesAndGlory.Battle b) {
        for (var banner : b.banners) lower(b, banner);
        b.banners.clear();
        for (var st : new ArrayList<>(b.structures)) st.discard();
        b.structures.clear();
        b.mists.clear();
    }

    /**
     * The server is stopping (SERVER_STOPPING): the banners and clouds come down, and lane B's static stores let go too (Statuses.release: the statuses, SECOND_AT
     * and RAISED, and the battle that owned them), so a world closed and opened again keeps no old troop.
     */
    static void serverStopping(GravesAndGlory.Battle b) {
        clearBanners(b);
        Statuses.release();
    }

    /**
     * A Mist Caller's cloud (the mist row): the team that cast it, its centre, its radius and the battle time it thins away. It stands in Battle.mists.
     * It is a ball (radius around the centre, up and down as well as across), so a troop on a bridge above it is not in it.
     * row is the row it acts by (its caster's mist row, or the Firepot's for the burning ground a shell leaves: N12) and rider the level-3 row that rides on it
     * (Chill fog's weaken), or null.
     */
    public record Mist(int team, Vec3d at, double radius, long until, GravesAndGlory.Unit caster, Spec.AbilitiesRow row, Spec.AbilitiesRow rider) {
        /** A cloud cast by a troop that acts by the sheet's mist row (mistRow) with no rider. */
        public Mist(int team, Vec3d at, double radius, long until, GravesAndGlory.Unit caster) { this(team, at, radius, until, caster, mistRow(), null); }
        /** A cloud no troop cast (the harness's): it credits nobody. */
        public Mist(int team, Vec3d at, double radius, long until) { this(team, at, radius, until, null); }
        /** Whether a point is in the cloud at this battle time. */
        public boolean holds(Vec3d p, long time) {
            double dx = p.x - at.x, dy = p.y - at.y, dz = p.z - at.z;
            return time < until && dx * dx + dy * dy + dz * dz <= radius * radius;
        }
        /** Whether it halves the shots fired from or into it (counters row C17): a mist does, the Firepot's burning ground does not (its row's rangedPenalty). */
        public boolean penalises() { return row == null || row.rangedPenalty(); }
        /** Whether it is burning ground (its row lays a bleed), which looks like fire, not mist. */
        public boolean burning() { return row != null && row.status().equals("bleed"); }
    }

    /** The mist row (the sheet's first row whose mechanic is mist): the numbers the cloud's slow reads. Null when the sheet has none. */
    static Spec.AbilitiesRow mistRow() {
        for (var a : Spec.ABILITIES) if (a.mechanic().equals("mist")) return a;
        return null;
    }

    /**
     * A Mist Caller's beat: the cast began in attack (the target's spot is spellCenter, a bolt of mist flies to it) and the ring marks the cloud-to-be while the
     * windup runs; when it ends the cloud stands there for the row's durationTicks, radius and all from the row. A stunned caster loses the cast.
     * The duration takes the caster's output factor (Veterans.outputFactor: his level and weapon tier, design 4.1), and her level-3 row rides on the cloud
     * when it is a mist rider (Chill fog).
     */
    static void cloud(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (u.spellCenter == null) return;
        if (time < u.stunnedUntil) { u.spellCenter = null; return; }
        double radius = row.radius() > 0 ? row.radius() : D.spellRadius();
        for (int i = 0; i < 12; i++) { double angle = i * Math.PI / 6; b.world.spawnParticles(ParticleTypes.CLOUD, u.spellCenter.x + Math.cos(angle) * radius, u.spellCenter.y + .2, u.spellCenter.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0); }
        if (time < u.windupUntil) return;
        long lasts = Math.round(row.durationTicks() * Veterans.outputFactor(u));
        b.mists.add(new Mist(u.squad.team, u.spellCenter, radius, time + lasts, u, row, rider(u, "mist")));
        Audio.at(b, AudioCues.Cue.MAGIC_IMPACT, u.spellCenter, "mist" + u.mob.getId()); // the cloud lands: an impact, not a second cast (the cast sounded when the windup began)
        u.spellCenter = null;
    }

    /** The cloud's look: sea-foam colour, drawn as a ring of its radius twice a second and as drifting cloud in it every beat. */
    static final int FOAM = 0xCFF3E4;

    /**
     * Every tick of a running round: a cloud thins away when its time is up; while it stands, every BEAT ticks, each enemy troop and seated enemy
     * commander in it is slowed again by the row's statusLevel for its statusTicks (so it holds while they stay and wears off a moment after they leave),
     * and it shows its particles. The shots through it are halved by counters row C17 (Combat.resolve reads inMist), not here.
     * Each cloud acts by its own row (N12): a row whose status is none or slow slows, any other lays its status through Statuses (the Firepot's burning ground
     * bleeds); its rider lays its own status too (Chill fog weakens). Each enemy held logs the caster as a helper (kill credit, design 3.2, review note I3).
     */
    static void mistTick(GravesAndGlory.Battle b) {
        if (b.mists.isEmpty()) return;
        b.mists.removeIf(m -> b.time >= m.until());
        if (b.time % BEAT != 0) return;
        for (var m : b.mists) {
            if (m.burning()) {
                b.world.spawnParticles(ParticleTypes.FLAME, m.at().x, m.at().y + .2, m.at().z, 8, m.radius() / 2, .05, m.radius() / 2, .01);
                b.world.spawnParticles(ParticleTypes.SMOKE, m.at().x, m.at().y + .5, m.at().z, 4, m.radius() / 2, .2, m.radius() / 2, .01);
                if (b.time % (2 * BEAT) == 0) ring(b, m.at(), m.radius(), EMBER);
            } else {
                b.world.spawnParticles(ParticleTypes.CLOUD, m.at().x, m.at().y + .6, m.at().z, 10, m.radius() / 2, .35, m.radius() / 2, .01);
                if (b.time % (2 * BEAT) == 0) ring(b, m.at(), m.radius(), FOAM);
            }
            var row = m.row();
            if (row == null) continue;
            boolean slows = row.status().equals("none") || row.status().equals("slow");
            for (var e : enemies(b, m.team())) {
                if (Combat.structure(e) || !m.holds(e.getPos().add(0, e.getHeight() / 2, 0), b.time)) continue;
                Veterans.helped(b, m.caster(), e); // every enemy held in the cloud logs its caster as a helper (kill credit, design 3.2)
                if (slows) slow(b, e, row); else Statuses.apply(b, m.caster(), e, row);
                if (m.rider() != null && e.isAlive()) Statuses.apply(b, m.caster(), e, m.rider());
            }
        }
    }

    /** The burning ground's ring colour (a presentation number). */
    static final int EMBER = 0xE0702A;

    /** Whether a point stands in a Mist cloud now that halves shots (the shooter's or the target's middle, for counters row C17); burning ground does not. */
    static boolean inMist(GravesAndGlory.Battle b, Vec3d p) {
        for (var m : b.mists) if (m.penalises() && m.holds(p, b.time)) return true;
        return false;
    }

    /** Whether a shooter or its target stands in a Mist cloud (counters row C17): a ranged hit fired from or into a cloud, from either side. */
    static boolean inMist(GravesAndGlory.Battle b, LivingEntity shooter, LivingEntity target) {
        if (b.mists.isEmpty()) return false;
        return inMist(b, shooter.getPos().add(0, shooter.getHeight() / 2, 0)) || inMist(b, target.getPos().add(0, target.getHeight() / 2, 0));
    }

    /**
     * The Admiral's undertow (the slam row on a timer): when its cooldown is ready and an enemy troop or commander stands within the row's radius, it drags
     * each of them knockback blocks (negative: toward her) and they take the row's amount (times her army's progression) as a secondary slam hit, which
     * their counters and traits shape, and Slowness at the row's statusLevel for its statusTicks. Nothing happens, and no cooldown is spent, with nobody near.
     */
    static void undertow(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (time < u.abilityAt || time < u.stunnedUntil) return;
        double reach = row.radius() * row.radius();
        List<LivingEntity> caught = new ArrayList<>();
        for (var e : enemies(b, u.squad.team)) if (!Combat.structure(e) && e.squaredDistanceTo(u.mob) <= reach) caught.add(e);
        if (caught.isEmpty()) return;
        u.abilityAt = time + row.cooldownTicks();
        float base = (float) (row.amount() * b.campaign.damage(u));
        Vec3d at = u.mob.getPos();
        for (var e : caught) {
            Combat.resolve(b, u, e, base, "slam", true);
            if (!e.isAlive()) continue;
            if (row.statusTicks() > 0) slow(b, u, e, row);
            pull(b, u, e, at, -row.knockback());
            BattleEffects.flash(b, new Vec3d(e.getX(), e.getBodyY(.6), e.getZ()), u.squad.team);
        }
        b.world.spawnParticles(ParticleTypes.SPLASH, at.x, at.y + 1, at.z, 40, row.radius() / 3, .6, row.radius() / 3, .2);
        b.world.playSound(null, u.mob.getBlockPos(), SoundEvents.ENTITY_DROWNED_AMBIENT_WATER, SoundCategory.HOSTILE, 1f, .6f);
    }
}
