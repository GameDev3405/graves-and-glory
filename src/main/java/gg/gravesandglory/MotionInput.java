package gg.gravesandglory;

import java.util.Objects;

/**
 * One frame of input to {@link MotionEngine} (animation rework design section 6.1). Pure.
 *
 * <p>time: render time in seconds, (world ticks + partial tick) / 20, so {@link Attack#startTick()} / 20 and {@link Hit#tick()} / 20
 * share its clock. limbAngle and limbSpeed: Minecraft's limb swing and limb distance. headYaw and headPitch: degrees, head
 * relative to body (positive pitch looks down). bodyYawRate: degrees per second. stance: the server stance string ("" when
 * none). aimPitch: degrees the arms follow while aiming (positive aims down). deathSeconds: seconds since death.
 */
public record MotionInput(double time, double limbAngle, double limbSpeed, double headYaw, double headPitch, double bodyYawRate,
                          String stance, float aimPitch, boolean shieldUp, boolean enemyNear, boolean dead, double deathSeconds,
                          Attack attack, Hit hit, MotionMath.Seed seed) {
    public MotionInput {
        Objects.requireNonNull(seed, "seed");
        if (stance == null) stance = "";
    }

    /** The server started attack variant {@code variant} at {@code startTick}. */
    public record Attack(int variant, long startTick) {}

    /**
     * A blow landed at {@code tick}. bearing: yaw of the blow relative to the victim's facing in degrees, 0 from the front,
     * 180 from behind, negative from the victim's left. weight: 0 light, 1 medium, 2 heavy.
     */
    public record Hit(String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, long tick) {}
}
