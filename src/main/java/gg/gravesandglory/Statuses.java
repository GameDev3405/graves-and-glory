package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;

/**
 * The status engine of in-match progression design 6.1: weaken, bleed, mark, the single-target stun and cleanse. Task 3 lays its seams only:
 * Battle.tick calls tick every tick of a running round, Battle.clearProgression calls clear at a round boundary (startRound,
 * Campaign.returnLobby and switchMap), and Combat.resolve multiplies factorOut into the clamped product beside the auras (HitLog.status).
 * With no status yet, factorOut is 1.0 and the other two do nothing. Lane B owns this file from Task 6 on.
 */
final class Statuses {
    private Statuses() {}

    /** Every tick of a running round, after the intro: statuses wear off and bleed ticks (Task 6). */
    static void tick(GravesAndGlory.Battle b) {}

    /** A round boundary: every status goes (Task 6). */
    static void clear(GravesAndGlory.Battle b) {}

    /** The share of an attacker's outgoing blow its statuses leave (weaken: x(1 - amount)); 1.0 until Task 6. */
    static double factorOut(GravesAndGlory.Battle b, LivingEntity attacker) { return 1.0; }
}
