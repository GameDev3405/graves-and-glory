package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * One troop's stride clock (unit animation rebuild, docs/anim-rebuild/PLAN.md, criterion 3). Client only.
 * <p>
 * The walk and run clips each cover a fixed ground distance per loop: their reference speed times their length. Advancing the
 * loop by the distance actually travelled, not by time, keeps a planted foot planted at any speed the unit really moves. Both
 * layers read the same phase, so the arms always counter the legs, and the arms can rejoin the gait after an attack in step.
 * <p>
 * Owned by one troop's controllers. {@link #update} is idempotent per animation time, so whichever controller runs first in a
 * frame advances it.
 */
final class Gait {
    /** Ground speed bands in blocks per tick, with hysteresis so a unit hovering at a band edge does not flicker between clips. */
    static final double IDLE_TO_WALK = .06, WALK_TO_IDLE = .04, WALK_TO_RUN = .22, RUN_TO_WALK = .20;
    /** Speed smoothing, in ticks: long enough to hide the client's per-tick position steps, short enough to start within 2 ticks. */
    static final double SMOOTH = 1.5;
    /**
     * Slower smoothing for the walk/run choice only, in ticks: a server hitch delivers a tick's motion late and then two at once,
     * which the fast average follows and which must not flip a running troop into its walk for a few frames.
     */
    static final double TREND = 4;

    enum Mode { IDLE, WALK, RUN }

    /** Loop position in cycles, 0..1. */
    double phase;
    /** Smoothed horizontal ground speed, blocks per tick; negative while backing up. */
    double speed;
    /** {@link #speed} smoothed over {@link #TREND}: what the walk/run choice reads. */
    double trend;
    Mode mode = Mode.IDLE;
    /** Ground distance one loop of the clip being played covers, blocks; 0 while no gait clip plays (the phase then holds). */
    double stride;
    private double last = Double.NaN;
    /** Each drawn troop's clock, for the in-game checks (test-tools GeoKnightChecks); weak, so unloaded troops drop out. */
    static final Map<Entity, Gait> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    /** Advances the clock to animation time {@code time} (ticks) from the entity's motion over its last tick. */
    void update(Entity e, double time) {
        if (time == last) return;
        if (Double.isNaN(last)) LIVE.put(e, this);
        // First sight, or back in view after not being drawn: the old averages describe another moment, so start from now.
        boolean fresh = Double.isNaN(last) || time - last > 4;
        double dt = fresh ? 0 : Math.max(0, time - last);
        last = time;
        double vx = e.getX() - e.prevX, vz = e.getZ() - e.prevZ, v = Math.sqrt(vx * vx + vz * vz);
        if (v > 1.0) v = 0; // a teleport, not a step
        float yaw = e instanceof LivingEntity l ? l.bodyYaw : e.getYaw();
        double fx = -MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE), fz = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE);
        // Backing up plays the cycle in reverse; sidesteps and shuffles play it forward.
        double signed = vx * fx + vz * fz < -.3 * v ? -v : v;
        if (fresh) { speed = trend = signed; mode = Mode.IDLE; }
        speed += (signed - speed) * (dt <= 0 ? 0 : 1 - Math.exp(-dt / SMOOTH));
        trend += (speed - trend) * (dt <= 0 ? 0 : 1 - Math.exp(-dt / TREND));
        // Starting and stopping follow the fast speed (feet must not slide); walk against run follows the steadier trend. A runner
        // drops to the walk only once both have slowed: after a hitch the fast speed is back up while the trend still lags.
        double s = Math.abs(speed), t = Math.abs(trend);
        mode = s < (mode == Mode.IDLE ? IDLE_TO_WALK : WALK_TO_IDLE) ? Mode.IDLE
            : (mode == Mode.RUN ? Math.max(s, t) > RUN_TO_WALK : t > WALK_TO_RUN) ? Mode.RUN : Mode.WALK;
        if (e instanceof LivingEntity l && (l.isDead() || l.deathTime > 0)) mode = Mode.IDLE;
        if (stride > 0) phase = wrap(phase + dt * speed / stride);
    }

    static double wrap(double p) { return p - Math.floor(p); }
}
