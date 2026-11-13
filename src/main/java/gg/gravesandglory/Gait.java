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
    /**
     * Ground speed bands in blocks per tick, with hysteresis so a unit hovering at a band edge does not flicker between clips. The
     * walk/run bands are a knight's (troops-row speed {@link #KNIGHT_SPEED}); each troop scales them by its own speed, so a slow
     * heavy troop's run (a sentinel's .236) still clears its band instead of sitting on the knight's.
     */
    static final double IDLE_TO_WALK = .06, WALK_TO_IDLE = .04, WALK_TO_RUN = .22, RUN_TO_WALK = .20, KNIGHT_SPEED = .28;
    /** Speed smoothing, in ticks: long enough to hide the client's per-tick position steps, short enough to start within 2 ticks. */
    static final double SMOOTH = 1.5;
    /**
     * Slower smoothing for the walk/run choice only, in ticks: a server hitch delivers a tick's motion late and then two at once,
     * which the fast average follows and which must not flip a running troop into its walk for a few frames.
     */
    static final double TREND = 4;
    /** Fewest ticks a troop that has set off stays out of the idle pose: a nudge or a push that the raw step catches must not flicker idle, walk, idle. */
    static final double MIN_MOVE = 3;

    enum Mode { IDLE, WALK, RUN }

    /** This troop's walk/run bands: the knight's scaled by its troops-row speed. */
    private final double toRun, toWalk;

    Gait(double troopSpeed) {
        double k = troopSpeed > 0 ? troopSpeed / KNIGHT_SPEED : 1;
        toRun = WALK_TO_RUN * k;
        toWalk = RUN_TO_WALK * k;
    }

    /** Loop position in cycles, 0..1. */
    double phase;
    /** Smoothed horizontal ground speed, blocks per tick; negative while backing up. */
    double speed;
    /** {@link #speed} smoothed over {@link #TREND}: what the walk/run choice reads. */
    double trend;
    Mode mode = Mode.IDLE;
    /** Ground distance one loop of the clip being played covers, blocks; 0 while no gait clip plays (the phase then holds). */
    double stride;
    /**
     * Where in its standing loops (the idle and the stance poses) this troop is, as a share of a loop, 0..1: from its UUID, the seed the old renderer offsets its idle
     * by, so a rank of troops does not breathe as one. 0 until the troop is first drawn.
     */
    double idlePhase;
    private boolean seeded;
    private double last = Double.NaN, lastRx = Double.NaN, lastRz = Double.NaN, movedAt = Double.NaN;
    /** Each drawn troop's clock, for the in-game checks (test-tools GeoKnightChecks); weak, so unloaded troops drop out. */
    static final Map<Entity, Gait> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    /** Advances the clock to animation time {@code time} (ticks); the partial tick is taken from the time's fraction. */
    void update(Entity e, double time) { update(e, time, time - Math.floor(time)); }

    /**
     * Advances the clock to animation time {@code time} (ticks) from the entity's motion. {@code partial} is the frame's partial tick, which places the render position
     * the stride clock is walked along.
     */
    void update(Entity e, double time, double partial) {
        if (!seeded) {
            java.util.UUID id = e.getUuid();
            idlePhase = MotionMath.seed(id.getMostSignificantBits(), id.getLeastSignificantBits()).phase() / MotionMath.TAU;
            seeded = true;
        }
        if (time == last) return;
        if (Double.isNaN(last)) LIVE.put(e, this);
        // First sight, or back in view after not being drawn: the old averages describe another moment, so start from now.
        boolean fresh = Double.isNaN(last) || time - last > 4;
        double dt = fresh ? 0 : Math.max(0, time - last);
        last = time;
        // The ground the troop really covered since the last frame, at its render position: the stride clock walks this, not the smoothed speed, which lags a troop that
        // sets off or stops (the server accelerates it over a few ticks) and lets the planted foot slide by the difference.
        double rx = e.prevX + (e.getX() - e.prevX) * partial, rz = e.prevZ + (e.getZ() - e.prevZ) * partial;
        double step = fresh || Double.isNaN(lastRx) ? 0 : Math.hypot(rx - lastRx, rz - lastRz);
        lastRx = rx; lastRz = rz;
        if (step > 1.0) step = 0; // a teleport, not a step
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
        // A planted troop sets off on the raw step, a tick or two before the smoothed speed shows it: every tick it waits in the idle pose is a tick its feet are carried.
        double gate = mode == Mode.IDLE ? Math.max(s, v) : s;
        Mode was = mode;
        mode = gate < (mode == Mode.IDLE ? IDLE_TO_WALK : WALK_TO_IDLE) ? Mode.IDLE
            : (mode == Mode.RUN ? Math.max(s, t) > toWalk : t > toRun) ? Mode.RUN : Mode.WALK;
        if (was == Mode.IDLE && mode != Mode.IDLE) movedAt = time;
        else if (was != Mode.IDLE && mode == Mode.IDLE && time - movedAt < MIN_MOVE) mode = was;
        if (e instanceof LivingEntity l && (l.isDead() || l.deathTime > 0)) mode = Mode.IDLE;
        // Setting off and cruising walk the rendered distance. Slowing down keeps the smoothed speed: the stop is as it was (polish batch 2: the rendered-distance clock made the stop slide worse).
        boolean slowing = s < t - .002;
        if (stride > 0) phase = wrap(phase + (slowing ? dt * speed : speed < 0 ? -step : step) / stride);
    }

    static double wrap(double p) { return p - Math.floor(p); }

    /**
     * The time (ticks) into a standing loop {@code length} ticks long at clock time {@code now}: the clock runs free, never restarted, and each troop sits
     * {@link #idlePhase} of a loop ahead of it. A loop entered late therefore starts mid-cycle, so whatever shows it blends into it rather than cutting to its first frame.
     */
    double standingTime(double now, double length) {
        return length <= 0 ? 0 : wrap(now / length + idlePhase) * length;
    }
}
