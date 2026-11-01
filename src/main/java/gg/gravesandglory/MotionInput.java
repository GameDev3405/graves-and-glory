package gg.gravesandglory;

import java.util.Objects;

/**
 * One frame of input to {@link MotionEngine} (animation rework design section 6.1). Pure.
 *
 * <p>time: render time in seconds, (world ticks + partial tick) / 20, so {@link Attack#startTick()} / 20 and {@link Hit#tick()} / 20
 * share its clock. limbAngle and limbSpeed: Minecraft's limb swing and limb distance. headYaw and headPitch: degrees, head
 * relative to body (positive pitch looks down). bodyYawRate: degrees per second. stance: the server stance string ("" when
 * none). aimPitch: degrees the arms follow while aiming (positive aims down). deathSeconds: seconds since death. ground: where
 * the entity stands this frame, or null when unknown (the engine then strides by limbAngle and limbSpeed).
 */
public record MotionInput(double time, double limbAngle, double limbSpeed, double headYaw, double headPitch, double bodyYawRate,
                          String stance, float aimPitch, boolean shieldUp, boolean enemyNear, boolean dead, double deathSeconds,
                          Attack attack, Hit hit, MotionMath.Seed seed, Ground ground) {
    public MotionInput {
        Objects.requireNonNull(seed, "seed");
        if (stance == null) stance = "";
    }

    /** Without a ground: the engine strides by limbAngle and limbSpeed. */
    public MotionInput(double time, double limbAngle, double limbSpeed, double headYaw, double headPitch, double bodyYawRate,
                       String stance, float aimPitch, boolean shieldUp, boolean enemyNear, boolean dead, double deathSeconds,
                       Attack attack, Hit hit, MotionMath.Seed seed) {
        this(time, limbAngle, limbSpeed, headYaw, headPitch, bodyYawRate, stance, aimPitch, shieldUp, enemyNear, dead, deathSeconds, attack, hit, seed, null);
    }

    /**
     * Where the entity stands this frame (Task 16, G1/G3): its render position in blocks, its body yaw in degrees (Minecraft's: 0
     * faces +z, 90 faces -x) and whether it is on the ground. Client-only; nothing new crosses the network.
     */
    public record Ground(double x, double y, double z, double yawDeg, boolean onGround) {}

    /** The server started attack variant {@code variant} at {@code startTick}. */
    public record Attack(int variant, long startTick) {}

    /**
     * A blow landed at {@code tick}. bearing: yaw of the blow relative to the victim's facing in degrees, 0 from the front,
     * 180 from behind, negative from the victim's left. weight: 0 light, 1 medium, 2 heavy.
     */
    public record Hit(String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, long tick) {}
}
