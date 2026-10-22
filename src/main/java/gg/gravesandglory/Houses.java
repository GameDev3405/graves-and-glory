package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * House traits (spec 1.3). A squad's trait tier in a House comes from the slot weights of its living troops of that House against the
 * houseTraits sheet's slot thresholds; the tiers feed the HUD strip, the trait seconds of the battle report and, in each House's task, the
 * trait multipliers. The pure maths is RosterMath and CounterMath. Gilded Bulwark is in: traitFactor's ranged share and BULWARK_GUARD. Graveborn Rise Again
 * is in: allowDeath, the death log Grave Bloom raises from, and the expiry of the temporary troops it makes.
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
    }

    /** The tiers a squad's banner reaches, House order, tier I or higher only; battle rewards read them. */
    static Map<String, Integer> bannerTiers(GravesAndGlory.Squad s) {
        Map<String, Integer> slots = RosterMath.houseSlots(s.banner, RosterMath.Catalog.fromSpec()), out = new LinkedHashMap<>();
        for (var h : Spec.HOUSES) { int tier = RosterMath.traitTier(slots.getOrDefault(h.id(), 0), thresholds(h.id())); if (tier > 0) out.put(h.id(), tier); }
        return out;
    }

    /**
     * The trait share of the factor in Combat.resolve: the attacker's House traits and the target's. Gilded Bulwark is the target's: a troop
     * of a House whose Bulwark its squad reaches, with the shield tag, takes 1 - value from an arrow, bolt or blight (CounterMath.bulwark).
     * Ironclad Entrench is the target's too: a troop of Ironclad that stands still (still) takes 1 - value, and 1 - 2 x value inside the radius
     * of a point its team owns (CounterMath.entrench). The other Houses' traits join in their tasks.
     */
    static double traitFactor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target, String kind) {
        var victim = Combat.unitOf(b, target);
        if (victim == null) return 1;
        String house = victim.spec.house();
        var own = CounterMath.trait(Spec.HOUSETRAITS, house, victim.squad.traits.getOrDefault(house, 0));
        double factor = CounterMath.bulwark(own, victim.tags.contains("shield"), kind);
        if (own != null && own.mechanic().equals("entrench")) factor *= CounterMath.entrench(own, still(victim), onOwnedPoint(b, victim));
        return factor;
    }

    /** The troop's own House trait at its army's tier when that trait is Entrench, else null. */
    static Spec.HouseTraitsRow entrench(GravesAndGlory.Unit u) {
        var t = CounterMath.trait(Spec.HOUSETRAITS, u.spec.house(), u.squad.traits.getOrDefault(u.spec.house(), 0));
        return t != null && t.mechanic().equals("entrench") ? t : null;
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
     * Gilded Bulwark's rout guard: a group cannot rout while its centre stands within the tier's radius of a living troop of its own army
     * from the House whose Bulwark the army reaches, one outside the group itself (a group never holds itself).
     */
    static final Morale.RoutGuard BULWARK_GUARD = (b, s, group) -> {
        for (var e : s.traits.entrySet()) {
            var t = CounterMath.trait(Spec.HOUSETRAITS, e.getKey(), e.getValue());
            if (t == null || !t.mechanic().equals("bulwark")) continue;
            Vec3d centre = Abilities.centre(b, s, group);
            if (centre == null) return false;
            for (var u : b.units)
                if (u.squad == s && u.spec.house().equals(t.house()) && !u.spec.group().equals(group) && u.mob.isAlive()
                    && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), centre) <= t.radius() * t.radius()) return true;
        }
        return false;
    };

    /** Harness hook: true makes every Rise Again roll succeed, false makes it fail, null leaves it to the dice. */
    static Boolean forceRise;

    /** One Rise Again roll: succeeds when a roll in [0, 1) falls below the tier's value. The harness hook decides instead while it is set. */
    static boolean rolls(GravesAndGlory.Battle b, Spec.HouseTraitsRow t) { return forceRise != null ? forceRise : b.world.random.nextDouble() < t.value(); }

    /**
     * Rise Again (ServerLivingEntityEvents.ALLOW_DEATH, which runs as a blow takes a troop's health to 0): a troop whose own House's trait at its
     * army's tier is rise_again (Graveborn) may rise instead of dying, once, when a roll falls below the tier's value. It does not for a Legendary,
     * a temporary troop (Grave Bloom's), one that already rose, damage that bypasses invulnerability (the void, /kill) or an army below tier I.
     * A rise sets its health to value2 of its maximum, marks it rose, stuns it for the row's durationTicks (Tactics skips its attacks, and a spell it had begun is lost) and returns
     * false, which skips vanilla's death and AFTER_DEATH: so the army loses nothing, the enemy earns no kill and the death log gains no entry.
     */
    static boolean allowDeath(LivingEntity e, DamageSource src) {
        var b = GravesAndGlory.battle;
        if (b == null) return true;
        var u = Combat.unitOf(b, e);
        if (u == null || u.temporary || u.rose || u.spec.rarity().equals("legendary") || src.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
        var t = CounterMath.trait(Spec.HOUSETRAITS, u.spec.house(), u.squad.traits.getOrDefault(u.spec.house(), 0));
        if (t == null || !t.mechanic().equals("rise_again") || !rolls(b, t)) return true;
        u.rose = true;
        u.stunnedUntil = b.time + t.durationTicks();
        u.spellCenter = null; // a stunned caster loses the spell it had begun
        e.setHealth((float) (t.value2() * e.getMaxHealth()));
        BattleEffects.soul(b, e.getPos());
        return false;
    }

    /** How many ticks the death log keeps an entry: the Warlock's value3 (the classes row whose active is Grave Bloom), 0 when no class carries it. */
    static long deathWindow() { var c = Commander.byActive("grave_bloom"); return c == null ? 0 : Math.round(c.value3()); }

    /** Logs a troop that really died (AFTER_DEATH), where it fell, and drops what is older than the window. Grave Bloom raises from this log. */
    static void logDeath(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        b.deaths.add(new GravesAndGlory.Death(u.mob.getPos(), b.time, u.spec.id(), u.squad.team));
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

    /** Every temporary troop is discarded and the death log emptied: startRound, returnLobby and switchMap call it, so nothing a round raised or logged outlives it. */
    static void reset(GravesAndGlory.Battle b) {
        for (var u : new ArrayList<>(b.units)) if (u.temporary) discard(b, u);
        b.deaths.clear();
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
