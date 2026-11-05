package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What a formation shape does in the fight (command UI plan Task 6), for every unit holding that shape, a bot's included (Ruling 10). The numbers
 * are the formations sheet's (moveMultiplier, arrowBlock, shove) and combatDiscipline's (chargeBonusMultiplier, chargeKnockback).
 * <ul>
 *   <li>Shield wall (block): its troops walk at moveMultiplier of their speed; a shield troop of its front rank with his shield up stops arrowBlock
 *   of an arrow's damage from within BattleMath.FRONT_ARC of his facing. A wall with enemies sighted in three or more of the four quadrants round
 *   its group (surrounded) closes its shield troops into a square facing out (BattleMath.squareLayout, through Battle.formation's place hook);
 *   every man of the square is front rank and faces his own edge's way.</li>
 *   <li>Wedge: its point and the two men flanking it, walking, push enemy troops within SHOVE_REACH sideways off the wedge's axis at shove
 *   blocks per tick, an enemy at most once per SHOVE_GAP ticks.</li>
 *   <li>Loose: its spacing only (the sheet's), no rule here.</li>
 *   <li>Charge impact: the first landed melee blow of each charging troop after a run of CHARGE_RUN blocks or more since its Charge was stamped
 *   deals chargeBonusMultiplier (a factor of its own beside the counters rows C5 and C6) and knocks back by chargeKnockback; once per troop per
 *   Charge (Combat.resolve asks firstChargeHit).</li>
 * </ul>
 */
final class FormationRules {
    private FormationRules() {}

    /** Blocks a Wedge's leading men reach to shove an enemy. */
    static final double SHOVE_REACH = 1.2;
    /** Ticks before the same enemy may be shoved again. */
    static final int SHOVE_GAP = 10;
    /** Blocks a charging troop must have run since its Charge for its first blow to crash in (chargeBonusMultiplier, chargeKnockback). */
    static final double CHARGE_RUN = 6;
    /** Ticks a group's surrounded reading is kept before it is looked at again. */
    static final int SURROUND_TICKS = 10;
    /** Ticks a troop's recorded place stays good for the arrow and facing rules before Battle.formation is asked again. */
    static final int PLACE_TICKS = 20;

    /** A troop's place as Battle.formation last gave it: {side, back} in the formation's frame, the group's front back (its first rank's), the square facing offset (degrees from the order's yaw) and whether it stood in the square. */
    record Place(long time, double side, double back, double front, float facing, boolean square) {}
    static final Map<GravesAndGlory.Unit, Place> PLACES = new WeakHashMap<>();
    /** The surrounded reading per squad and group: {time, 1 or 0}. */
    static final Map<GravesAndGlory.Squad, Map<String, long[]>> SURROUNDED = new WeakHashMap<>();
    /** The battle tick each enemy was last shoved. */
    static final Map<Entity, Long> SHOVED = new WeakHashMap<>();
    /** Each troop's run under its group's Charge: {stamp, blocks run since it}; and where it stood last tick. */
    static final Map<GravesAndGlory.Unit, double[]> RUNS = new WeakHashMap<>();
    static final Map<GravesAndGlory.Unit, Vec3d> RUN_AT = new WeakHashMap<>();
    /** The Charge stamp whose first-blow bonus each troop has spent. */
    static final Map<GravesAndGlory.Unit, Long> SPENT = new WeakHashMap<>();
    /** Shoves and charge first hits so far (test evidence). */
    static int shoves, firstHits;

    static Spec.FormationsRow row(String formation) {
        for (var f : Spec.FORMATIONS) if (f.id().equals(formation)) return f;
        return Spec.FORMATIONS[0];
    }

    /** The share of its normal speed a unit moves at under this assignment's shape: the formation's moveMultiplier (Shield wall 0.85). */
    static double moveShare(GravesAndGlory.Unit u, GravesAndGlory.Assignment a) { return row(a.formation).moveMultiplier(); }

    /**
     * Battle.formation's hook: the unit's place, or its place in the square when its group is a surrounded Shield wall on Hold with four or more
     * shield troops. In the square the shield troops stand on its edges (BattleMath.squareLayout at the widest of their wall cells, in their group
     * order) and the rest keep their ranks, moved back behind the square. Records the place for the arrow, facing and shove rules.
     */
    static double[] place(GravesAndGlory.Battle b, GravesAndGlory.Unit u, GravesAndGlory.Assignment a, List<GravesAndGlory.Unit> group, boolean[] shield, double[][] layout, int at, Spec.FormationsRow f, double[] place) {
        double front = Double.MAX_VALUE;
        for (double[] p : layout) front = Math.min(front, p[1]);
        int shields = 0, index = 0; double cell = 0, wallDepth = 0;
        for (int i = 0; i < shield.length; i++) if (shield[i]) {
            if (i < at) index++;
            shields++; cell = Math.max(cell, BattleMath.shieldCell(group.get(i).mob.getWidth(), Spec.COMBATDISCIPLINE[0].shieldWallCell())); wallDepth = Math.max(wallDepth, layout[i][1]);
        }
        double[] out = place; float facing = 0; boolean square = false;
        if (f.id().equals("block") && a.order.equals("hold") && shields >= 4 && surrounded(b, u)) {
            double[][] ring = BattleMath.squareLayout(shields, cell);
            double depth = 0; for (double[] r : ring) depth = Math.max(depth, r[1]);
            if (shield[at]) { double[] r = ring[index]; out = new double[]{r[0], r[1] + cell}; facing = (float) r[2]; }
            else out = new double[]{layout[at][0], layout[at][1] - wallDepth + depth + cell + cell};
            square = true; front = cell;
        }
        PLACES.put(u, new Place(b.time, out[0], out[1], front, facing, square));
        return out;
    }

    /** The unit's recorded place, asking Battle.formation again when it is missing or old; null on a player's default Hold (its deploy spot, no place). */
    static Place recorded(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        Place p = PLACES.get(u);
        if (p == null || b.time - p.time() > PLACE_TICKS) { PLACES.remove(u); b.formation(u); p = PLACES.get(u); }
        return p;
    }

    /** The unit's facing in its formation: the order's yaw, or in a square its edge's way out. */
    static float facing(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        var a = b.assignment(u); Place p = recorded(b, u);
        return p != null && p.square() ? a.yaw + p.facing() : a.yaw;
    }

    /**
     * The share of an arrow's damage the victim's formation stops: arrowBlock (Shield wall 0.75) for a shield troop in its front rank (in a square,
     * any man of it) with his shield up, when the arrow comes from within FRONT_ARC of his facing; else 0. arrow is the shot's source (its shooter).
     */
    static float arrowBlock(GravesAndGlory.Battle b, GravesAndGlory.Unit victim, Entity arrow) {
        if (victim == null || arrow == null || !victim.tags.contains("shield")) return 0f;
        var mob = victim.mob;
        if (!mob.isUsingItem() || !mob.getActiveItem().isOf(Items.SHIELD)) return 0f;
        var f = row(b.assignment(victim).formation);
        if (f.arrowBlock() <= 0) return 0f;
        Place p = recorded(b, victim);
        if (p == null || !(p.square() || p.back() <= p.front() + 0.25)) return 0f;
        return BattleMath.frontArc(facing(b, victim), arrow.getX() - mob.getX(), arrow.getZ() - mob.getZ(), BattleMath.FRONT_ARC) ? (float) f.arrowBlock() : 0f;
    }

    /**
     * A Wedge's push while its troops walk (Tactics, on a walking unit's beat): when u is the wedge's point or one of the two men flanking it, every
     * enemy troop within SHOVE_REACH of him is set moving sideways off the wedge's axis (the line through the point along the order's yaw) at
     * the sheet's shove, at most once per SHOVE_GAP ticks each. An anchored troop is not moved.
     */
    static void shove(GravesAndGlory.Battle b, GravesAndGlory.Unit u, GravesAndGlory.Assignment a) {
        var f = row(a.formation);
        if (f.shove() <= 0 || !f.wedge() || !u.mob.isAlive()) return;
        Place p = recorded(b, u);
        if (p == null || p.square()) return;
        double cell = p.front();
        boolean point = p.back() <= cell * 1.25 && Math.abs(p.side()) < cell * 0.5;
        boolean flank = Math.abs(p.back() - 2 * cell) <= cell * 0.25 && Math.abs(p.side()) >= cell * 0.5 && Math.abs(p.side()) <= cell * 1.5;
        if (!point && !flank) return;
        double yaw = Math.toRadians(a.yaw), rx = Math.cos(yaw), rz = Math.sin(yaw); // the formation's +side direction
        double axis = rx * u.mob.getX() + rz * u.mob.getZ() - p.side(); // the axis's side coordinate, from where this man stands and his place
        for (var other : b.units) {
            if (other.squad.team == u.squad.team || !other.mob.isAlive()) continue;
            LivingEntity e = other.mob;
            double dx = e.getX() - u.mob.getX(), dz = e.getZ() - u.mob.getZ();
            if (dx * dx + dz * dz > SHOVE_REACH * SHOVE_REACH) continue;
            Long last = SHOVED.get(e);
            if (last != null && b.time - last < SHOVE_GAP) continue;
            if (Abilities.anchored(b, other)) continue;
            double lateral = rx * e.getX() + rz * e.getZ() - axis;
            double sign = Math.abs(lateral) > 1e-3 ? Math.signum(lateral) : (e.getId() % 2 == 0 ? 1 : -1);
            e.setVelocity(rx * sign * f.shove(), e.getVelocity().y, rz * sign * f.shove());
            e.velocityModified = true;
            SHOVED.put(e, b.time); shoves++;
        }
    }

    /**
     * Whether the unit's group has enemies sighted (living enemy troops, and enemy commanders in the field, within scouting range of the group's
     * centre) in three or more of the four quadrants round it (front, right, back and left of its order's yaw). Kept SURROUND_TICKS per group.
     */
    static boolean surrounded(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        var byGroup = SURROUNDED.computeIfAbsent(u.squad, s -> new HashMap<>());
        long[] kept = byGroup.get(u.spec.group());
        if (kept != null && b.time - kept[0] < SURROUND_TICKS && b.time >= kept[0]) return kept[1] == 1;
        var members = Morale.members(b, u.squad, u.spec.group());
        Vec3d c = members.isEmpty() ? u.mob.getPos() : Morale.centre(members);
        float yaw = b.assignment(u).yaw; double range = Spec.SCOUTING[0].range();
        boolean[] seen = new boolean[4];
        for (var other : b.units) if (other.squad.team != u.squad.team && other.mob.isAlive()) mark(seen, yaw, c, other.mob.getPos(), range);
        for (var p : b.server.getPlayerManager().getPlayerList()) {
            var s = b.squads.get(p.getUuid());
            if (s != null && s.team != u.squad.team && !p.isSpectator() && p.isAlive() && !b.respawns.containsKey(p.getUuid())) mark(seen, yaw, c, p.getPos(), range);
        }
        int n = 0; for (boolean q : seen) if (q) n++;
        boolean out = n >= 3;
        byGroup.put(u.spec.group(), new long[]{b.time, out ? 1 : 0});
        return out;
    }
    static void mark(boolean[] seen, float yaw, Vec3d c, Vec3d p, double range) {
        double dx = p.x - c.x, dz = p.z - c.z;
        if (dx * dx + dz * dz > range * range || dx * dx + dz * dz < 1e-6) return;
        double deg = BattleMath.signedBearing(yaw, dx, dz);
        seen[deg >= -45 && deg < 45 ? 0 : deg >= 45 && deg < 135 ? 1 : deg >= -135 && deg < -45 ? 3 : 2] = true;
    }

    /**
     * Every tick of every troop, after Tactics (Battle.unitTick): its run under its group's Charge (blocks walked since the stamp, for
     * firstChargeHit), and a square's man standing on his place with nothing to strike faces his edge's way out.
     */
    static void tick(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        var mob = u.mob; if (!mob.isAlive()) return;
        var a = b.assignment(u);
        Long stamp = u.squad.chargedAt.get(u.spec.group());
        if (stamp != null && a.order.equals("charge")) {
            double[] run = RUNS.get(u); Vec3d last = RUN_AT.get(u), at = mob.getPos();
            if (run == null || run[0] != stamp) { run = new double[]{stamp, 0}; RUNS.put(u, run); }
            else if (last != null) run[1] += Math.sqrt(GravesAndGlory.Battle.horizontalDistance(at, last));
            RUN_AT.put(u, at);
        } else RUN_AT.remove(u);
        Place p = PLACES.get(u);
        if (p == null || !p.square() || b.time - p.time() > PLACE_TICKS || !a.order.equals("hold")) return;
        if (u.lockedTarget != null && u.lockedTarget.isAlive() || !mob.getNavigation().isIdle()) return;
        float yaw = a.yaw + p.facing();
        mob.setYaw(yaw); mob.setHeadYaw(yaw); mob.setBodyYaw(yaw);
    }

    /** Blocks the troop has run under its group's current Charge (0 when it is not charging). */
    static double run(GravesAndGlory.Unit u) {
        double[] run = RUNS.get(u); Long stamp = u.squad.chargedAt.get(u.spec.group());
        return run == null || stamp == null || run[0] != stamp ? 0 : run[1];
    }

    /**
     * Whether this melee blow is the attacker's first landed blow of its Charge after a run of CHARGE_RUN blocks or more: the blow is judged under
     * the Charge (Combat.charge: its swing's order), the troop ran that far since its group's stamp, and it has not had this bonus under that stamp.
     * A true answer spends it, so never twice per troop per Charge.
     */
    static boolean firstChargeHit(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker) {
        if (!Combat.charge(b, attacker, true).charging()) return false;
        Long stamp = attacker.squad.chargedAt.get(attacker.spec.group());
        if (stamp == null || run(attacker) < CHARGE_RUN) return false;
        Long spent = SPENT.get(attacker);
        if (spent != null && spent.longValue() == stamp) return false;
        SPENT.put(attacker, stamp); firstHits++;
        return true;
    }
}
