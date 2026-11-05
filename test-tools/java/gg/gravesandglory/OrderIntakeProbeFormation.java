package gg.gravesandglory;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Test-only reach into the package-private formation rules for cmdui.FormationRulesSuite (command UI plan Task 6): FormationRules' readings and
 * counters, bot-style orders with a formation (Battle.issue), a synthetic Charge as a player's is stored, and target preparation. Never included
 * in releases (its name carries the OrderIntakeProbe guard token).
 */
public final class OrderIntakeProbeFormation {
    private OrderIntakeProbeFormation() {}

    public static int shoves() { return FormationRules.shoves; }
    public static int firstHits() { return FormationRules.firstHits; }
    public static float facing(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return FormationRules.facing(b, u); }
    public static double run(GravesAndGlory.Unit u) { return FormationRules.run(u); }
    public static boolean surrounded(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return FormationRules.surrounded(b, u); }
    /** Whether the unit stands in its formation's front rank (in a square, any shield man of it), as FormationRules.arrowBlock reads it. */
    public static boolean frontRank(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        var p = FormationRules.recorded(b, u); return p != null && p.frontRank();
    }
    /** The unit's wedge role as FormationRules.shove reads it: "point", "flank" or "none". */
    public static String wedgeRole(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        var p = FormationRules.recorded(b, u); int r = p == null ? FormationRules.NONE : p.wedge();
        return r == FormationRules.POINT ? "point" : r == FormationRules.FLANK ? "flank" : "none";
    }
    /** Kills the troop outright (a generic kill). */
    public static void kill(GravesAndGlory.Unit u) { u.mob.kill(); }
    public static boolean square(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { var p = FormationRules.recorded(b, u); return p != null && p.square(); }
    public static String order(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return b.assignment(u).order; }
    public static String formation(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return b.assignment(u).formation; }
    public static Vec3d target(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return b.assignment(u).target; }
    public static boolean shield(GravesAndGlory.Unit u) { return u.tags.contains("shield"); }
    public static int id(GravesAndGlory.Unit u) { return u.mob.getId(); }

    /** A bot-style order with a formation (Battle.issue); holdSlots as a player's Formation sets it (melee keep their slots once in place). */
    public static void issue(GravesAndGlory.Battle b, Object squad, String group, String action, String formation, Vec3d dest, float yaw, boolean holdSlots) {
        var s = (GravesAndGlory.Squad) squad;
        b.issue(s, group, action, formation, dest, yaw, -1);
        s.assignments.get(group).holdSlots = holdSlots;
    }
    /** A Charge stored as OrderRules.playerOrder stores one: the order to dest, its direction, and the group's Charge stamp now. */
    public static void charge(GravesAndGlory.Battle b, Object squad, String group, Vec3d dest, Vec3d dir, float yaw) {
        var s = (GravesAndGlory.Squad) squad;
        b.issue(s, group, "charge", "line", dest, yaw, -1);
        s.assignments.get(group).chargeDir = dir.normalize();
        b.markCharge(s, group);
    }
    public static List<GravesAndGlory.Unit> members(GravesAndGlory.Battle b, Object squad, String group) { return Morale.members(b, (GravesAndGlory.Squad) squad, group); }
    /** Troops that never attack but keep their AI (so a push or a knockback moves them; Tactics still walks them back to their place). */
    public static void stun(List<GravesAndGlory.Unit> units) { for (var u : units) u.stunnedUntil = Long.MAX_VALUE / 4; }
    /** A target whose health loss is the blow's amount: no armour (worn or natural) and a deep pool of health. */
    public static void bare(GravesAndGlory.Unit u, double health) {
        var mob = u.mob;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) mob.equipStack(slot, ItemStack.EMPTY);
        var armour = mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR); if (armour != null) armour.setBaseValue(0);
        var tough = mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR_TOUGHNESS); if (tough != null) tough.setBaseValue(0);
        var max = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH); if (max != null) max.setBaseValue(health);
        mob.setHealth((float) health);
    }
    public static double armour(GravesAndGlory.Unit u) { return u.mob.getArmor(); }
    /** Moves a troop to (x, z) on the ground, its walk stopped. */
    public static void place(GravesAndGlory.Unit u, double x, double z) {
        u.mob.getNavigation().stop(); u.mob.requestTeleport(x, OrderIntakeProbeBehaviour.ground(x, z), z); u.mob.setVelocity(Vec3d.ZERO);
    }
    public static double arenaHalfX() { return GravesAndGlory.R.arenaHalfX(); }
    public static double arenaHalfZ() { return GravesAndGlory.R.arenaHalfZ(); }
}
