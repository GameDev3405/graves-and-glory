package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * House traits (spec 1.3). A squad's trait tier in a House comes from the slot weights of its living troops of that House against the
 * houseTraits sheet's slot thresholds; the tiers feed the HUD strip, the trait seconds of the battle report and, in each House's task, the
 * trait multipliers. The pure maths is RosterMath and CounterMath. Gilded Bulwark is in: traitFactor's ranged share and the covered flanks (covered, which the counters table's flank condition reads). Graveborn Rise Again
 * is in: allowDeath, the death log Grave Bloom raises from, and the expiry of the temporary troops it makes. Ironclad Entrench is in (traitFactor, still). Tidecallers
 * Riptide is in: the wet speed (swim), the slow every hit of theirs gives (Abilities.onHit) and the ford (fords, which Tactics reads).
 */
final class Houses {
    private Houses() {}

    private static Spec.HouseTraitsRow[] cachedRows;
    private static Map<String, int[]> cachedThresholds = Map.of();
    /** The slot thresholds of a House's tiers I to III (houseTraits.slots), in tier order; empty for a House the sheet gives no trait. */
    static synchronized int[] thresholds(String house) {
        if (cachedRows != Spec.HOUSETRAITS) {
            Map<String, int[]> map = new HashMap<>();
            for (var h : Spec.HOUSES) map.put(h.id(), Arrays.stream(Spec.HOUSETRAITS).filter(t -> t.house().equals(h.id())).sorted(Comparator.comparingInt(Spec.HouseTraitsRow::tier)).mapToInt(Spec.HouseTraitsRow::slots).toArray());
            cachedThresholds = map;
            cachedRows = Spec.HOUSETRAITS;
        }
        return cachedThresholds.getOrDefault(house, new int[0]);
    }

    /** Sets the squad's House tiers from its living troops' slot weights, in House order, keeping only the Houses at tier I or higher. Temporary troops do not count. */
    static void recompute(GravesAndGlory.Squad s) {
        var battle = GravesAndGlory.battle;
        if (battle == null) return;
        Map<String, Integer> slots = new HashMap<>();
        for (var u : battle.units) if (u.squad == s && !u.temporary && u.mob.isAlive()) slots.merge(u.spec.house(), u.spec.slotWeight(), Integer::sum);
        s.traits.clear();
        for (var h : Spec.HOUSES) { int tier = RosterMath.traitTier(slots.getOrDefault(h.id(), 0), thresholds(h.id())); if (tier > 0) s.traits.put(h.id(), tier); }
        battle.mode.traits(battle, s); // a mode may set an army's tiers itself (Survival's dead take the tier's waveTraitTier)
    }

    /** The tiers a squad's banner reaches, House order, tier I or higher only; battle rewards read them. */
    static Map<String, Integer> bannerTiers(GravesAndGlory.Squad s) {
        Map<String, Integer> slots = RosterMath.houseSlots(s.banner, RosterMath.Catalog.fromSpec()), out = new LinkedHashMap<>();
        for (var h : Spec.HOUSES) { int tier = RosterMath.traitTier(slots.getOrDefault(h.id(), 0), thresholds(h.id())); if (tier > 0) out.put(h.id(), tier); }
        return out;
    }

    /**
     * The trait share of the factor in Combat.resolve: the attacker's House traits and the target's. Wildmoot Pack Hunters is the attacker's
     * (packHunters, with the blow's bearing off the target's facing: 0 for a blow with no direction). Gilded Bulwark is the target's: a troop
     * of a House whose Bulwark its squad reaches, with the shield tag, takes 1 - value from an arrow, bolt or blight (CounterMath.bulwark).
     * Ironclad Entrench is the target's too: a troop of Ironclad that stands still (still) takes 1 - value, and 1 - 2 x value inside the radius
     * of a point its team owns (CounterMath.entrench). Graveborn's trait does not shape a blow.
     */
    static double traitFactor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target, String kind, double bearing) {
        double factor = packHunters(b, attacker, target, bearing);
        var victim = Combat.unitOf(b, target);
        if (victim == null) return factor;
        String house = victim.spec.house();
        var own = CounterMath.trait(Spec.HOUSETRAITS, house, victim.squad.traits.getOrDefault(house, 0));
        factor *= CounterMath.bulwark(own, victim.tags.contains("shield"), kind);
        if (own != null && own.mechanic().equals("entrench")) factor *= CounterMath.entrench(own, still(victim), onOwnedPoint(b, victim));
        return factor;
    }

    /**
     * Wildmoot Pack Hunters (the attacker's House trait at its army's tier): x(1 + value) when value2 or more of the attacker's own army's troops of that House
     * have the target (Battle.targeters, counted every 10 ticks from each troop's target), or the attacker stands more than CounterMath.flankDegrees off the
     * target's facing (CounterMath.packHunters). It stacks on the flank row C16, and cover (Bulwark) does not void it (no-routing design 4.1 and 4.2); a blow with no direction (bearing 0) has only the count.
     */
    static double packHunters(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target, double bearing) {
        String house = attacker.spec.house();
        var own = CounterMath.trait(Spec.HOUSETRAITS, house, attacker.squad.traits.getOrDefault(house, 0));
        return CounterMath.packHunters(own, b.targeters(attacker.squad, target), bearing);
    }

    /** Whether a House has Pack Hunters at any tier, so its troops are counted in Battle.targeters. */
    static boolean packHouse(String house) {
        for (var r : Spec.HOUSETRAITS) if (r.house().equals(house) && r.mechanic().equals("pack_hunters")) return true;
        return false;
    }

    /** Rebuilds Battle.targeters: per army, its living troops of a Pack Hunters House by the entity id of their current target. Battle.tick calls it every 10 ticks before the units tick. */
    static void countTargeters(GravesAndGlory.Battle b) {
        b.targeters.clear();
        for (var u : b.units) {
            if (!u.mob.isAlive() || !packHouse(u.spec.house())) continue;
            var t = u.mob.getTarget();
            if (t != null) b.targeters.computeIfAbsent(u.squad, k -> new HashMap<>()).merge(t.getId(), 1, Integer::sum);
        }
    }

    /** The troop's own House trait at its army's tier when that trait is Entrench, else null. */
    static Spec.HouseTraitsRow entrench(GravesAndGlory.Unit u) {
        var t = CounterMath.trait(Spec.HOUSETRAITS, u.spec.house(), u.squad.traits.getOrDefault(u.spec.house(), 0));
        return t != null && t.mechanic().equals("entrench") ? t : null;
    }

    /** The troop's own House trait at its army's tier when that trait is Riptide (Tidecallers), else null. */
    static Spec.HouseTraitsRow riptide(GravesAndGlory.Unit u) {
        var t = CounterMath.trait(Spec.HOUSETRAITS, u.spec.house(), u.squad.traits.getOrDefault(u.spec.house(), 0));
        return t != null && t.mechanic().equals("riptide") ? t : null;
    }

    /**
     * Whether the troop fords the river: a Tidecallers troop whose army's Riptide tier says so (value2 is 1 on tiers II and III of the sheet). The tier is the
     * one the army's living troops reach in battle (recompute), like every other House trait; a banner that falls to tier I as troops die stops fording.
     * Tactics.route and walk skip the bridge detour and the river snap for such a troop, and its water path penalty is 0.
     */
    static boolean fords(GravesAndGlory.Unit u) {
        var t = riptide(u);
        return t != null && t.value2() >= 1;
    }

    /** The fixed id of Riptide's speed modifier on a Tidecallers troop. */
    static final UUID RIPTIDE = UUID.fromString("5a0f6c1e-2b7d-4c43-9f1a-7e3d8b94c2a6");

    /**
     * Riptide's speed, on each of the troop's Tactics beats: the troop travels x(1 + value) of the army's tier while it touches water or it rains on the map, else
     * as it did. It is a temporary multiplier on the movement speed (never saved), so it stacks with Muster's, and the beat adds it, changes it with the tier or the
     * ground, or takes it off. What the multiplier must be on the attribute depends on the ground (BattleMath.speedModifier): a swimmer's stroke is linear in the
     * movement speed, so in water the attribute takes 1 + value as it stands; a land mob's walk is quadratic in it, so in the rain on land the attribute takes the
     * square root of 1 + value and the troop walks the 15% the sheet says (a Raider keeps 3.6 blocks a second on land, and 0.57 to 0.65 measured in water, where
     * vanilla drag makes every land mob slow).
     */
    static void swim(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        double travel = CounterMath.riptideSpeed(riptide(u), u.mob.isTouchingWater() || b.world.isRaining());
        Abilities.keepSpeed(u.mob, RIPTIDE, "Graves and Glory riptide", BattleMath.speedModifier(travel, u.mob.isTouchingWater()));
    }

    /** The sheet's rest rule: its first Entrench row (the radius and the durationTicks are the same on every tier), or null when the sheet has none. Any troop's rest is measured by it, whatever its House. */
    static Spec.HouseTraitsRow restRule() {
        for (var t : Spec.HOUSETRAITS) if (t.mechanic().equals("entrench")) return t;
        return null;
    }

    /**
     * Whether the troop has moved less than the rest rule's radius over its durationTicks (0.5 blocks, 60 ticks on the sheet), as sampled on its
     * Tactics beats (sample). True or false for any troop, whatever its House or its army's tier; false when the sheet has no rest rule.
     */
    static boolean still(GravesAndGlory.Unit u) {
        var b = GravesAndGlory.battle;
        var rule = restRule();
        return b != null && rule != null && u.stillAt != null && b.time - u.stillSince >= rule.durationTicks();
    }

    /** One sample of a troop's rest, on each of its Tactics beats: it stands still from the spot it was last seen to have left by more than the rest rule's radius. Nothing without a rest rule. */
    static void sample(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        var rule = restRule();
        if (rule == null) return;
        Vec3d at = u.mob.getPos();
        if (u.stillAt == null || at.squaredDistanceTo(u.stillAt) >= rule.radius() * rule.radius()) { u.stillAt = at; u.stillSince = b.time; }
    }

    /** Whether the troop stands within the capture radius of a point its team owns (horizontal distance, as the capture rule measures it). */
    static boolean onOwnedPoint(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        for (var p : b.points)
            if (p.owner == u.squad.team && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), new Vec3d(p.spec.x(), u.mob.getY(), p.spec.z())) <= (double) p.spec.radius() * p.spec.radius()) return true;
        return false;
    }

    /**
     * Gilded Bulwark's covered flanks: a troop takes no flank bonus (the counters table's flank condition, rows C16 and C16s) while a living
     * troop of its own army from the House whose Bulwark the army reaches, one of another group (a group never covers itself: Gilded archers
     * cover the line in front of them and Gilded knights the archers behind), stands within the tier's radius of it (5, 8 or 12). Measured
     * from the struck troop itself, since the rule acts per blow. Pack Hunters' own flank bonus is not taken off.
     */
    static boolean covered(GravesAndGlory.Battle b, GravesAndGlory.Unit target) {
        var s = target.squad;
        for (var e : s.traits.entrySet()) {
            var t = CounterMath.trait(Spec.HOUSETRAITS, e.getKey(), e.getValue());
            if (t == null || !t.mechanic().equals("bulwark")) continue;
            for (var u : b.units)
                if (u.squad == s && u != target && u.spec.house().equals(t.house()) && !u.spec.group().equals(target.spec.group()) && u.mob.isAlive()
                    && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), target.mob.getPos()) <= t.radius() * t.radius()) return true;
        }
        return false;
    }

    /** Harness hook: true makes every Rise Again roll succeed, false makes it fail, null leaves it to the dice. */
    static Boolean forceRise;

    /** One Rise Again roll: succeeds when a roll in [0, 1) falls below the tier's value. The harness hook decides instead while it is set. */
    static boolean rolls(GravesAndGlory.Battle b, Spec.HouseTraitsRow t) { return rolls(b, t.value()); }

    /** One rise roll at a chance: succeeds when a roll in [0, 1) falls below it (a chance of 1 or more always does). The harness hook decides instead while it is set. */
    static boolean rolls(GravesAndGlory.Battle b, double chance) { return forceRise != null ? forceRise : b.world.random.nextDouble() < chance; }

    /**
     * Rise Again (ServerLivingEntityEvents.ALLOW_DEATH, which runs as a blow takes a troop's health to 0): a troop whose own House's trait at its
     * army's tier is rise_again (Graveborn) may rise instead of dying, once, when a roll falls below the tier's value. It does not for a Legendary,
     * a temporary troop (Grave Bloom's), one that already rose, damage that bypasses invulnerability (the void, /kill) or an army below tier I.
     * A rise sets its health to value2 of its maximum, marks it rose, stuns it for the row's durationTicks (Tactics skips its attacks, and a spell it had begun is lost) and returns
     * false, which skips vanilla's death and AFTER_DEATH: so the army loses nothing, the enemy earns no kill and the death log gains no entry.
     * A soldier's own rise row (N7, Undying King: Abilities.riseRow, his level-3 row) lets him rise too, Legendary or not, at its own chance (amount2), added to the
     * trait's where both apply; such a rise takes the row's health share (amount) and stun (durationTicks). It is the same once-only path, and the soldier keeps
     * his level, kills and gear: he never died.
     */
    static boolean allowDeath(LivingEntity e, DamageSource src) {
        var b = GravesAndGlory.battle;
        if (b == null) return true;
        var u = Combat.unitOf(b, e);
        if (u == null || u.temporary || u.rose || src.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        var own = Abilities.riseRow(u);
        var t = CounterMath.trait(Spec.HOUSETRAITS, u.spec.house(), u.squad.traits.getOrDefault(u.spec.house(), 0));
        boolean trait = t != null && t.mechanic().equals("rise_again") && !u.spec.rarity().equals("legendary");
        if (!trait && own == null) return true;
        if (!rolls(b, (trait ? t.value() : 0) + (own != null ? own.amount2() : 0))) return true;
        u.rose = true;
        u.stunnedUntil = b.time + (own != null ? own.durationTicks() : t.durationTicks());
        u.spellCenter = null; // a stunned caster loses the spell it had begun
        e.setHealth((float) ((own != null ? own.amount() : t.value2()) * e.getMaxHealth()));
        BattleEffects.soul(b, e.getPos());
        return false;
    }

    /** How many ticks the death log keeps an entry: the Warlock's value3 (the classes row whose active is Grave Bloom), 0 when no class carries it. */
    static long deathWindow() { var c = Commander.byActive("grave_bloom"); return c == null ? 0 : Math.round(c.value3()); }

    /** Logs a troop that really died (AFTER_DEATH), where it fell, and drops what is older than the window. Grave Bloom raises from this log. */
    static void logDeath(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        b.deaths.add(new GravesAndGlory.Death(u.mob.getPos(), b.time, u.spec.id(), u.squad.team, u.mob));
        prune(b);
    }

    /** Drops the log's entries older than deathWindow. */
    static void prune(GravesAndGlory.Battle b) { long window = deathWindow(); b.deaths.removeIf(d -> b.time - d.time() > window); }

    /** Every tick of a running round: the death log is pruned and a temporary troop whose time is up is discarded. */
    static void expire(GravesAndGlory.Battle b) {
        prune(b);
        for (var u : new ArrayList<>(b.units)) if (u.temporary && b.time >= u.expiresAt) discard(b, u);
    }

    /** Takes a troop out of the battle: its side-team membership, its entity and its place in b.units. */
    static void discard(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        GravesAndGlory.Battle.leaveSide(u.mob);
        u.mob.discard();
        b.units.remove(u);
    }

    /** Every temporary troop is discarded, the death log emptied and the Pack Hunters' targeters forgotten: startRound, returnLobby and switchMap call it, so nothing a round raised, logged or counted outlives it. */
    static void reset(GravesAndGlory.Battle b) {
        for (var u : new ArrayList<>(b.units)) if (u.temporary) discard(b, u);
        b.deaths.clear();
        b.targeters.clear();
    }

    /** Once a second while a round runs: refreshes every seated squad's tiers and adds a second to the trait seconds of each House at tier I or higher. */
    static void tick(GravesAndGlory.Battle b) {
        for (var s : b.squads.values()) {
            if (!s.seated) continue;
            recompute(s);
            for (String house : s.traits.keySet()) s.stats.traitSeconds.merge(house, 1, Integer::sum);
        }
    }
}
