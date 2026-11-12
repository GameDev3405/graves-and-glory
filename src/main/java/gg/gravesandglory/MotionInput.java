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

    /** attack_v1's variant for a cancel (coordinator 2026-11-11): the server dropped the announced shot that starts at startTick (Projectiles.dropDraw); an old client drops it as an invalid variant. */
    public static final int CANCEL = -1;
    /** A cancelled attack: the start tick it was announced with (the client's dedup key, with the troop's id) and the world tick the client heard the cancel on. */
    public record Cancel(long startTick, long at) {}
    /** The kept attack that starts at startTick, or null. */
    static Attack kept(java.util.Collection<Attack> kept, long startTick) { for (Attack a : kept) if (a.startTick() == startTick) return a; return null; }
    /** The cancel for startTick, or null. */
    static Cancel cancelled(java.util.Collection<Cancel> done, long startTick) { for (Cancel c : done) if (c.startTick() == startTick) return c; return null; }
    /**
     * The cancel to keep for startTick heard at now (MotionEvents.Client.cancel), or null: an attack of that start is kept, none cancelled it yet, and its clip may
     * still be playing (no more than maxAge ticks since it began: BattleMath.ATTACK_CLIP_MAX_TICKS). The server never cancels a shot that left, so a cancel heard
     * at or after the loose still takes (fix round 1: the draw it found, a release with no arrow, is lowered); one for an unknown or finished attack does nothing.
     */
    static Cancel cancel(java.util.Collection<Attack> kept, java.util.Collection<Cancel> done, long startTick, long now, int maxAge) {
        return kept(kept, startTick) != null && cancelled(done, startTick) == null && now >= startTick && now - startTick <= maxAge ? new Cancel(startTick, now) : null;
    }
    /** Whether an attack is kept: not a repeat of one kept (a catch-up and the live send) nor of a cancelled one (a late repeat of the announce never restarts the draw). */
    static boolean admits(java.util.Collection<Attack> kept, java.util.Collection<Cancel> done, Attack a) { return !kept.contains(a) && cancelled(done, a.startTick()) == null; }

    /**
     * A blow landed at {@code tick}. bearing: yaw of the blow relative to the victim's facing in degrees, 0 from the front,
     * 180 from behind, negative from the victim's left. weight: 0 light, 1 medium, 2 heavy. knockback: the blow also threw the victim (hit_v1 flag 4);
     * pulled: it also dragged the victim toward the striker (hit_v1 flag 8, Abilities.pull).
     */
    public record Hit(String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, boolean knockback, boolean pulled, long tick) {
        public Hit(String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, long tick) { this(kind, zone, bearing, weight, blocked, lethal, false, false, tick); }
        public Hit(String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, boolean knockback, long tick) { this(kind, zone, bearing, weight, blocked, lethal, knockback, false, tick); }
    }
}
