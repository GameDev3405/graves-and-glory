package gg.gravesandglory;

import net.minecraft.entity.Entity;

/**
 * What a formation shape does in the fight (command UI plan): its speed share, the arrows a Shield wall stops, a Wedge's shove and whether a
 * troop is surrounded. Task 3 adds these as neutral stubs so callers compile; Task 6 fills them from the formations sheet
 * (moveMultiplier, arrowBlock, shove).
 */
final class FormationRules {
    private FormationRules() {}
    /** The share of its normal speed a unit moves at under this assignment's shape (1.0: unchanged). */
    static double moveShare(GravesAndGlory.Unit u, GravesAndGlory.Assignment a) { return 1.0; }
    /** The share of an arrow's damage the victim's formation stops (0: none). */
    static float arrowBlock(GravesAndGlory.Battle b, GravesAndGlory.Unit victim, Entity arrow) { return 0f; }
    /** A unit's push into the enemy ahead under this assignment's shape (no-op until Task 6). */
    static void shove(GravesAndGlory.Battle b, GravesAndGlory.Unit u, GravesAndGlory.Assignment a) {}
    /** Whether the unit has enemies on enough sides to count as surrounded (false until Task 6). */
    static boolean surrounded(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return false; }
}
