package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The commander engine (spec section 5): the squad's three relic slots on hotbar keys 1 to 3 in battle (gear-combat Task 5), each with its
 * own cooldown (relics.cooldownTicks x quickened_relic) and run (relicTuning.durationTicks). What a
 * relic does is in RelicEffects.ACTIVES by its tuning effect id; a relic nobody registered refuses with NOT_READY and starts no cooldown.
 */
public final class Commander {
    private Commander() {}

    /** The troop Grave Bloom raises, by its troops sheet id: a Risen. */
    static final String RISEN = "revenant";

    /**
     * One temporary troop of an army where a troop fell (Grave Bloom's Risen, and Death knell's: Abilities.raise): in the first slot past the army's own that
     * none of its troops holds, at full health, gone at until (Houses.expire, and with the round: Houses.reset), with the soul effect where it rises. The caller
     * sets its health and calls Houses.recompute once it has raised all it raises.
     */
    static GravesAndGlory.Unit temporary(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.TroopsRow spec, Vec3d at, long until) {
        Set<Integer> used = new HashSet<>();
        for (var u : b.units) if (u.squad == s) used.add(u.slot);
        int slot = GravesAndGlory.R.squadSize();
        while (used.contains(slot)) slot++;
        var u = b.spawn(s, spec, slot, at);
        u.temporary = true;
        u.expiresAt = until;
        BattleEffects.soul(b, at);
        return u;
    }

    /** Whether a blow feeds the Reaver (Bloodrush): his own blow by hand (the source is the attacker himself, not an arrow or a spell) while it runs. Battle.allowDamage asks before the blow lands and then measures what the target lost. */
    static boolean feeds(GravesAndGlory.Battle b, GravesAndGlory.Squad s, DamageSource source) { return source.getSource() == source.getAttacker() && running(b, s, "bloodrush"); }

    /** The Reaver's lifesteal (Bloodrush): value1 of the health (absorption included) the target of his blow by hand really lost, which a raised shield or the target's hurt cooldown can make 0. */
    static void lifesteal(GravesAndGlory.Battle b, GravesAndGlory.Squad s, ServerPlayerEntity p, float lost) { int i = runningSlot(b, s, "bloodrush"); if (lost > 0 && i >= 0) p.heal((float) (lost * tuningIn(s, i).value1())); }

    /** An enemy troop the commander killed by his own hand (Battle's AFTER_DEATH, when the killing blow's source is himself, after the kill's gold): while his Bloodrush runs, one troop comes back (Bloodrush.refund). */
    static void playerKill(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { int i = runningSlot(b, s, "bloodrush"); if (i >= 0) RelicEffects.Bloodrush.refund(b, s, i, tuningIn(s, i)); }

    /** A relic's tuning row by its relics id, or null. */
    public static Spec.RelicTuningRow tuning(String relicId) {
        if (relicId == null) return null;
        for (var t : Spec.RELICTUNING) if (t.id().equals(relicId)) return t;
        return null;
    }

    /** The first tuning row with this effect, or null (a reader that needs an effect's numbers outside any slot: the forward-supply ring, Grave Bloom's death log window). */
    public static Spec.RelicTuningRow tuningByEffect(String effect) {
        for (var t : Spec.RELICTUNING) if (t.effect().equals(effect)) return t;
        return null;
    }

    /** A relic's relics row by id, or null. */
    public static Spec.RelicsRow relic(String relicId) {
        if (relicId == null) return null;
        for (var r : Spec.RELICS) if (r.id().equals(relicId)) return r;
        return null;
    }

    /** The relic id in a squad's slot, or "" for an empty slot or one past its slots. */
    public static String relicIn(GravesAndGlory.Squad s, int slot) {
        return s == null || s.relics == null || slot < 0 || slot >= s.relics.length || s.relics[slot] == null ? "" : s.relics[slot];
    }

    /** The tuning row of the relic in a squad's slot, or null. */
    static Spec.RelicTuningRow tuningIn(GravesAndGlory.Squad s, int slot) { return tuning(relicIn(s, slot)); }

    /** The slot whose relic has this effect and runs now, or -1. */
    static int runningSlot(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String effect) {
        if (s == null) return -1;
        for (int i = 0; i < s.relicState.size(); i++) {
            var t = tuningIn(s, i);
            if (t != null && t.effect().equals(effect) && s.relicState.active(i, b.time)) return i;
        }
        return -1;
    }

    /** A relic's cooldown for this squad: the relics sheet's cooldownTicks x GearEffects.relicCooldownFactor (quickened_relic), rounded. */
    static int cooldownTicks(GravesAndGlory.Squad s, Spec.RelicsRow r) { return (int) Math.round(r.cooldownTicks() * GearEffects.relicCooldownFactor(s.gear)); }

    /** An active id as the HUD tile and the class cards name it: stand_fast reads Stand Fast, bloodrush Bloodrush; rally_horn reads Muster (the no-routing design renamed it; the sheet id stays). */
    public static String activeName(String id) {
        if (id.equals("rally_horn")) return "Muster";
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(" ");
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** The commander in the field for a squad: its seated human owner, online in the battle world, alive, not a spectator and not waiting to respawn. Null for a bot. */
    public static ServerPlayerEntity player(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (s == null || s.bot || !s.seated || b.respawns.containsKey(s.owner)) return null;
        var p = b.server.getPlayerManager().getPlayer(s.owner);
        return p == null || !p.isAlive() || p.isSpectator() || p.getWorld() != b.world ? null : p;
    }

    /** Whether any of the squad's relics runs now. */
    public static boolean running(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (s == null) return false;
        for (int i = 0; i < s.relicState.size(); i++) if (s.relicState.active(i, b.time)) return true;
        return false;
    }

    /** Whether a relic of this effect (relicTuning.effect) runs now in one of the squad's slots. */
    static boolean running(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String effect) { return runningSlot(b, s, effect) >= 0; }

    /**
     * A relic key (the "relic:<slot>" command, hotbar keys 1 to 3 in battle). Refused without a word while the commander is unseated, a bot,
     * dead or waiting to respawn, or the round is not running (its entrance included). An empty slot says so; a relic this build has no
     * behaviour for gives NOT_READY; one on cooldown says when it is ready. Otherwise its start runs and, if it accepts, the slot's cooldown
     * (cooldownTicks) and duration begin now.
     */
    public static void activate(GravesAndGlory.Battle b, ServerPlayerEntity p, int slot) {
        var s = b.squads.get(p.getUuid());
        if (s == null || !b.running || b.time < b.campaign.introUntil || player(b, s) != p || slot < 0 || slot >= s.relicState.size()) return;
        String id = relicIn(s, slot);
        var t = tuning(id);
        var r = relic(id);
        if (t == null || r == null) { b.campaign.notice(p.getUuid(), "No relic in slot " + (slot + 1) + "."); return; }
        RelicEffects.Active active = RelicEffects.ACTIVES.get(t.effect());
        if (active == null) { b.campaign.notice(p.getUuid(), RelicEffects.NOT_READY); return; }
        if (b.time < s.relicState.readyAt[slot]) { b.campaign.notice(p.getUuid(), activeName(t.effect()) + " is ready in " + (s.relicState.readyAt[slot] - b.time + 19) / 20 + " s."); return; }
        if (!s.relicState.ready(slot, b.time) || !active.start(b, p, s, slot, t)) return;
        s.relicState.begin(slot, b.time, cooldownTicks(s, r), t.durationTicks());
        GravesAndGlory.sendRelics(b, s);
    }

    /** Runs each squad's running relics for their duration and stops them on time. Battle.tick calls it every tick of a running round after the entrance. */
    public static void tick(GravesAndGlory.Battle b) {
        for (var s : new ArrayList<>(b.squads.values())) for (int i = 0; i < s.relicState.size(); i++) {
            if (!s.relicState.started(i)) continue;
            var t = tuningIn(s, i);
            RelicEffects.Active active = t == null ? null : RelicEffects.ACTIVES.get(t.effect());
            if (b.time >= s.relicState.until[i] || active == null) stop(b, s, i);
            else active.tick(b, s, i, t);
        }
    }

    /** Stops every running relic of a squad now (each end runs once) and clears their data. The cooldowns stand. */
    public static void stop(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (s == null) return;
        for (int i = 0; i < s.relicState.size(); i++) stop(b, s, i);
    }

    /** Stops one slot's relic now (its end runs once) and clears its data. The cooldown stands. */
    public static void stop(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {
        if (s == null || slot < 0 || slot >= s.relicState.size()) return;
        boolean was = s.relicState.started(slot);
        s.relicState.until[slot] = 0;
        var t = tuningIn(s, slot);
        RelicEffects.Active active = t == null ? null : RelicEffects.ACTIVES.get(t.effect());
        try { if (was && active != null) active.end(b, s, slot); }
        finally { s.relicState.data[slot] = null; }
    }

    /** A fresh round or the lobby: every relic stops and every cooldown is clear. */
    public static void reset(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        stop(b, s);
        if (s != null) s.relicState.clear();
    }

    /** Ticks until the slot's relic may start again: 0 when ready, -1 for an empty slot or a relic this build has no behaviour for. The relics packet sends it. */
    public static int readyIn(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {
        if (s == null) return 0;
        var t = tuningIn(s, slot);
        if (t == null || !RelicEffects.ACTIVES.containsKey(t.effect())) return -1;
        return s.relicState.readyIn(slot, b.time);
    }

    /** readyIn of slot 0 (state_v9's ability field). */
    public static int readyIn(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return readyIn(b, s, 0); }

    /** Ticks the slot's relic still runs; 0 when it is not running. */
    public static int activeFor(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) { return s == null ? 0 : s.relicState.activeFor(slot, b.time); }

    /** activeFor of slot 0 (state_v9's ability field). */
    public static int activeFor(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return activeFor(b, s, 0); }

    /**
     * The commander share of a troop's blow, on the attacker's side (Combat.resolve): the Champion's infantry within passiveRadius of
     * him x(1 + passiveValue2), the Warden's ranged troops x(1 + passiveValue), and x(1 + value1) on the entity his running Mark Prey marked. Beside Mark Prey,
     * Hunter's mark (N8, Statuses.markFactor): x(1 + amount) on an entity a troop of the attacker's own army marked, and from no other army.
     */
    public static double factor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target) {
        var s = attacker.squad;
        double f = 1;
        var p = player(b, s); // the command aura (gear-combat Task 4): the worn command stat, a banner's wider reach, formation_charge on Attack
        double r = GearEffects.commandRadius(s.gear);
        boolean inAura = p != null && GravesAndGlory.Battle.horizontalDistance(p.getPos(), attacker.mob.getPos()) <= r * r;
        var a = b.assignment(attacker);
        f *= GearEffects.troopFactor(s.gear, inAura, a != null && "attack".equals(a.order));
        int w = runningSlot(b, s, "war_cry"); // Battle Cry: his own troops within its radius hit x(1 + value1) while it runs
        if (w >= 0 && p != null) { var t = tuningIn(s, w); if (GravesAndGlory.Battle.horizontalDistance(p.getPos(), attacker.mob.getPos()) <= t.radius() * t.radius()) f *= 1 + t.value1(); }
        return f * marked(b, s, target) * Statuses.markFactor(b, s, target);
    }

    /** Mark Prey's share: x(1 + value1) when the squad's running mark_prey marked this entity (its slot's data holds it or its UUID), else 1. */
    static double marked(GravesAndGlory.Battle b, GravesAndGlory.Squad s, LivingEntity target) {
        int i = target == null ? -1 : runningSlot(b, s, "mark_prey");
        if (i < 0) return 1;
        Object mark = s.relicState.data[i];
        return mark == target || target.getUuid().equals(mark) ? 1 + tuningIn(s, i).value1() : 1;
    }

    /** The commander share on the receiving side: for a troop, oathkeeper inside the command aura and x(1 - value1) while its squad's Stand Fast runs within radius of its commander; for a seated commander, surrounded_guard. Else 1. */
    public static double takenFactor(GravesAndGlory.Battle b, LivingEntity target) {
        if (target instanceof ServerPlayerEntity pl) { // surrounded_guard: a seated commander with enough foes about him (gear-combat Task 6)
            var s = b.squads.get(pl.getUuid());
            if (s == null || s.bot || !s.seated || !GearStats.has(s.gear, "surrounded_guard")) return 1;
            return GearEffects.surroundedFactor(s.gear, GearCombat.foesNear(b, pl, s.team, GearStats.combat().surroundedRadius()));
        }
        var u = Combat.unitOf(b, target);
        if (u == null) return 1;
        var p = player(b, u.squad);
        double f = 1;
        if (p != null && GearStats.has(u.squad.gear, "oathkeeper")) { // oathkeeper: a troop inside his command aura
            double r = GearEffects.commandRadius(u.squad.gear);
            f *= GearEffects.oathFactor(u.squad.gear, GravesAndGlory.Battle.horizontalDistance(p.getPos(), u.mob.getPos()) <= r * r);
        }
        int i = runningSlot(b, u.squad, "stand_fast");
        if (i < 0) return f;
        var c = tuningIn(u.squad, i);
        return p != null && GravesAndGlory.Battle.horizontalDistance(p.getPos(), u.mob.getPos()) <= c.radius() * c.radius() ? f * (1 - c.value1()) : f;
    }

    /**
     * The commander share of a blow a commander strikes himself (Battle.allowDamage): his own Mark Prey on its mark, the target side's Stand Fast, and a weaken
     * on the commander himself (Statuses.factorOut: Kort, round 34 question 3, statuses hit commanders too).
     */
    public static double playerFactor(GravesAndGlory.Battle b, GravesAndGlory.Squad attacker, LivingEntity target) { return playerFactor(b, attacker, target, null); }
    /** With the blow's source: the commander's gear share too (GearCombat.blowFactor: ranged, hawkeye, grudge, last_light). */
    public static double playerFactor(GravesAndGlory.Battle b, GravesAndGlory.Squad attacker, LivingEntity target, net.minecraft.entity.damage.DamageSource source) {
        var p = attacker == null ? null : player(b, attacker);
        return marked(b, attacker, target) * takenFactor(b, target) * (p == null ? 1 : Statuses.factorOut(b, p)) * GearCombat.blowFactor(attacker, p, target, source);
    }

    /** Ticks a troop trains for: the sheet seconds times 20, rounded (no class discount: the classes are gone). */
    public static long trainingTicks(Spec.TroopsRow t) { return BattleMath.trainingTicks(t.trainingSeconds(), 1); }
}
