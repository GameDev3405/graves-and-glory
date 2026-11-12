package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.WeakHashMap;

/**
 * The stance layer (unit animation rebuild, docs/anim-rebuild/PLAN.md, "Shield"): what the knight holds while it obeys an order.
 * Registered after {@code upper} and before {@code attack}, it blends the order's pose (MotionEvents stance, plus the synced raised
 * shield) over the gait, so a Hold or a shield wall reads at range and a swing starts from, and returns to, that pose.
 * <p>
 * Every pose has its own ramp that moves 1/{@link #RAMP} per tick toward on or off, so a new order shows within 2 ticks and two
 * poses crossfade without a pop. The upper poses go over any gait. The leg poses, and the "ready" combat idle, play only while the
 * troop stands: they are scaled by how still it is, so a planted foot never slides when the unit moves off.
 */
final class StanceController extends AnimationController<GeoTroop> {
    /** Ticks for a pose to fully come in or go out. */
    static final double RAMP = 2;
    /** Ticks for the standing factor to follow the gait (legs poses and ready fade over this when the troop starts or stops). */
    static final double STILL = 3;
    /** A troop that swung or was struck within this many ticks, and has no order stance, stands in the ready guard. */
    static final long FIGHTING = 100;

    private final Gait gait;
    /** Each pose's ramp, 0..1, by clip name; insertion order is the blend order. */
    private final Map<String, Double> ramps = new LinkedHashMap<>();
    private double still = 1, last = Double.NaN;

    /** How still the troop stands this frame, 0 moving to 1 planted (eased by the caller); the attack layer scales footwork by it. */
    double still() { return still; }
    /** Per-bone blend accumulator, reused every frame: rot xyz, pos xyz, rot weight, pos weight. */
    private final Map<String, float[]> acc = new HashMap<>();
    /** What the last frame chose and showed, for the in-game checks (GeoKnightChecks). Never read in play. */
    String shownUpper = "", shownLegs = "", shownStance = "";
    double shownUpperWeight, shownLegsWeight, shownStill;
    boolean shownShield;
    /** The entity this controller is listed under in {@link #LIVE}. A troop tracked anew is a new entity object on the same controller (GeckoLib keys them by entity id), so it is listed again; held weakly, as LIVE holds it. */
    private java.lang.ref.WeakReference<Entity> listed;
    /**
     * AIM_PITCH_HOOK: the aim pitch the server sent with a ranged_aim stance (degrees, positive down; 0 in any other stance), refreshed every frame. It is eased into
     * {@link #aimSmooth} and read each frame to weight {@code stance_aim_upper} (authored at pitch 0) against {@code stance_aim_up_upper} (-45) and
     * {@code stance_aim_down_upper} (+30), so the bow, draw arm and spine follow the aim.
     */
    private float aimPitch;
    /** The aim pitch eased toward the sent one (degrees, positive down), so a re-aim glides instead of stepping. */
    private float aimSmooth;
    /** The pitched copies of the aim pose: weights blend the level clip with these (-45 up, +30 down) by the smoothed pitch. */
    static final String AIM = "stance_aim_upper", AIM_UP = "stance_aim_up_upper", AIM_DOWN = "stance_aim_down_upper";
    static final float AIM_UP_DEG = 45, AIM_DOWN_DEG = 30;
    /** The tick the current rally began (the rally clip plays once and holds its last pose, unlike the other stance clips, which loop). */
    private double rallyStart = Double.NaN;
    private boolean rallyOn;
    /**
     * The stun clips (stun_upper, stun_legs) are one hit-snap entry of {@link #STUN_ENTRY} ticks followed by a loop of the rest of the clip: they play from the tick the
     * stun began (so the snap shows once), then repeat from the end of the entry. {@code stunEnd} is the tick the stun stopped, for the additive shake-off over the next ticks.
     */
    private double stunStart = Double.NaN, stunEnd = Double.NaN, shuffle;
    private boolean stunOn;
    static final double STUN_ENTRY = 4;
    /**
     * Ticks for a stun pose to come in (fast, so the snap lands with the hit) and to go out (slower, under the shake-off). The pose that replaces the stun (the ready
     * guard, the order's stance, the walk legs) comes in over the same {@link #STUN_OUT} ticks, so the two crossfade evenly instead of the new pose arriving in two ticks
     * while the stun is still half there; the shake-off clip is stretched over the same span.
     */
    static final double STUN_IN = 1, STUN_OUT = 8;
    /** The stun legs ease into the dazed stance over this many ticks (the upper body still snaps in {@link #STUN_IN}), and the legs the stun replaces hand over at the same pace. */
    static final double STUN_LEGS_IN = 3;
    static final String STUN_OUT_CLIP = "stun_out_upper";
    /** The charge upper-body cycle: as long as the run legs and timed by the same stride clock (Gait.phase), not by world time. */
    static final String CHARGE = "charge_run_upper";
    /** The ranged_aim pitch this frame (AIM_PITCH_HOOK), 0 when not aiming. */
    float aimPitch() { return aimPitch; }
    static final Map<Entity, StanceController> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    StanceController(GeoTroop troop, Gait gait) {
        super(troop, "stance", 0, s -> PlayState.STOP);
        this.gait = gait;
    }

    /** The upper and legs pose for a stance name, a raised shield and recent fighting (either may be null: none), for a model whose file has every clip. */
    static String[] target(String stance, boolean shield, boolean fighting) { return target(stance, shield, fighting, k -> true); }

    /**
     * The same, for a model that has only the clips {@code has} names. The newer stances (charge, stunned, rally, ranged_aim, ranged_rest, hold) prefer a clip of
     * their own ({@code charge_run_upper}, {@code stun_upper}/{@code stun_legs}, {@code stance_rally_upper}, {@code stance_aim_upper}, {@code stance_rest_upper},
     * {@code stance_hold_upper}/{@code stance_hold_legs}); a model with none of a stance's clips gets what it got before the stance had any (charge as run: none;
     * the others as the default), so a clip not yet authored changes nothing. A model with only some of a pair plays those and leaves the other empty.
     */
    static String[] target(String stance, boolean shield, boolean fighting, Predicate<String> has) {
        String[] own = switch (stance) {
            case "charge" -> new String[]{"charge_run_upper", null};
            case "stunned" -> new String[]{"stun_upper", "stun_legs"};
            case "rally" -> new String[]{"stance_rally_upper", null};
            case "ranged_aim" -> new String[]{"stance_aim_upper", null};
            case "ranged_rest" -> new String[]{"stance_rest_upper", null};
            case "hold" -> new String[]{"stance_hold_upper", "stance_hold_legs"};
            case "fall_back" -> new String[]{"stance_fallback_upper", "stance_fallback_legs"};
            default -> null;
        };
        if (own != null) {
            String up = own[0] != null && has.test(own[0]) ? own[0] : null, legs = own[1] != null && has.test(own[1]) ? own[1] : null;
            if (up != null || legs != null) return new String[]{up, legs};
        }
        return switch (stance) {
            case "wall" -> new String[]{shield ? "stance_wall_up_upper" : "stance_wall_upper", "stance_wall_legs"};
            case "brace" -> new String[]{shield ? "stance_brace_up_upper" : "stance_brace_upper", "stance_brace_legs"};
            case "shield_move" -> new String[]{"stance_shield_up_upper", null};
            case "fall_back" -> new String[]{shield ? "stance_shield_up_upper" : "stance_ward_upper", null};
            case "run", "charge" -> new String[]{null, null};
            default -> new String[]{shield ? "stance_shield_up_upper" : fighting ? "stance_ready_upper" : null,
                    fighting ? "stance_ready_legs" : null};
        };
    }

    @Override
    public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Entity e = state.getData(DataTickets.ENTITY);
        if (e == null || e.getWorld() == null) return;
        if (listed == null || listed.get() != e) { listed = new java.lang.ref.WeakReference<>(e); LIVE.put(e, this); }
        double now = EndingClient.worldTime(e.getWorld()) + state.getPartialTick();
        double dt = Double.isNaN(last) || now - last > 4 ? 1e9 : Math.max(0, now - last);
        last = now;

        boolean dead = e instanceof LivingEntity l && (l.isDead() || l.deathTime > 0);
        boolean shield = e instanceof LivingEntity l && l.isUsingItem();
        MotionEvents.Stance sent = MotionEvents.Client.stance(e.getId());
        String stance = sent.name();
        // a troop that rose plays its fall and rise on the death layer instead of the stun (rise_v1), where its model has them
        if (stance.equals("stunned") && DeathController.rising(model, animatable, e, now) != null) stance = "";
        aimPitch = stance.equals("ranged_aim") ? sent.aimPitch() : 0; // AIM_PITCH_HOOK: read below as aimSmooth, which blends the three aim clips
        long recent = Long.MIN_VALUE;
        for (MotionInput.Attack a : MotionEvents.Client.attacks(e.getId())) recent = Math.max(recent, a.startTick());
        for (MotionInput.Hit h : MotionEvents.Client.hits(e.getId())) recent = Math.max(recent, h.tick());
        boolean fighting = recent != Long.MIN_VALUE && now - recent < FIGHTING;
        boolean standing = gait.mode == Gait.Mode.IDLE;
        still = step(still, standing ? 1 : 0, dt / STILL);
        String[] want = dead ? new String[]{null, null} : target(stance, shield, fighting, k -> model.getAnimation(animatable, k) != null);
        shownStance = stance; shownShield = shield; shownStill = still;

        boolean rallying = "stance_rally_upper".equals(want[0]);
        if (rallying && !rallyOn) rallyStart = now;
        rallyOn = rallying;
        boolean stunning = "stun_upper".equals(want[0]) || "stun_legs".equals(want[1]);
        if (stunning && !stunOn) { stunStart = now; stunEnd = Double.NaN; }
        if (!stunning && stunOn) stunEnd = now;
        stunOn = stunning;
        double drift = stunDrift(dt, stunning);
        boolean aiming = AIM.equals(want[0]);
        aimSmooth = aiming ? aimSmooth + (aimPitch - aimSmooth) * (float) Math.min(1, dt / 3) : 0;
        float up = aiming ? clamp01(-aimSmooth / AIM_UP_DEG) : 0, down = aiming ? clamp01(aimSmooth / AIM_DOWN_DEG) : 0;
        String[] pitched = aiming ? new String[]{AIM_UP, AIM_DOWN} : new String[0];

        for (String w : want) if (w != null) ramps.putIfAbsent(w, 0.0);
        for (String w : pitched) if (model.getAnimation(animatable, w) != null) ramps.putIfAbsent(w, 0.0);
        // the stun is going out (a stun pose still has weight): what takes over rises as slowly as the stun falls
        boolean handover = !stunning && (ramps.getOrDefault("stun_upper", 0.0) > 0 || ramps.getOrDefault("stun_legs", 0.0) > 0);
        for (Map.Entry<String, Double> r : ramps.entrySet()) {
            String k = r.getKey();
            boolean on = k.equals(want[0]) || k.equals(want[1]) || (aiming && (k.equals(AIM_UP) || k.equals(AIM_DOWN)));
            r.setValue(step(r.getValue(), on ? 1 : 0, dt / (isStun(k) ? (on ? (k.endsWith("_legs") ? STUN_LEGS_IN : STUN_IN) : STUN_OUT)
                    : stunning && k.endsWith("_legs") ? STUN_LEGS_IN : handover && on ? STUN_OUT : RAMP)));
        }

        for (float[] a : acc.values()) java.util.Arrays.fill(a, 0);
        shownUpper = ""; shownLegs = ""; shownUpperWeight = 0; shownLegsWeight = 0;
        for (Map.Entry<String, Double> r : ramps.entrySet()) {
            String k = r.getKey();
            double w = AttackController.smooth(r.getValue());
            boolean legs = k.endsWith("_legs");
            // the fall-back legs are a walk of their own: they play while the troop moves, so they are not scaled by standing
            // the stun legs are not: a stunned troop is slid by a knockback or still moving, and its legs must hold the dazed stance through that, not hand back to the gait's walk
            if ((legs && !k.equals("stance_fallback_legs") && !k.equals("stun_legs")) || k.equals("stance_ready_upper")) w *= AttackController.smooth(still);
            // the charge arms are a stride-locked cycle: a troop standing still in the charge stance falls back to its idle upper body
            if (k.equals(CHARGE)) w *= AttackController.smooth(1 - still);
            if (aiming && (k.equals(AIM) || k.equals(AIM_UP) || k.equals(AIM_DOWN))) {
                // the level aim and its two pitched copies share the aim's weight by the smoothed pitch (the three weights sum to 1)
                w *= k.equals(AIM_UP) ? up : k.equals(AIM_DOWN) ? down : 1 - up - down;
            }
            // a stunned troop that has stopped being thrown but still drifts (the server stuns attacks, not movement) shuffles: the walk legs share the pose
            // in proportion to how fast it drifts, so the feet are carried by the stride clock rather than skating on the dazed stance
            if (k.equals("stun_legs")) w *= 1 - STUN_SHUFFLE * drift;
            if (w <= 0) continue;
            Animation clip = model.getAnimation(animatable, k);
            if (clip == null) continue;
            if (legs ? w > shownLegsWeight : w > shownUpperWeight) {
                if (legs) { shownLegs = k; shownLegsWeight = w; } else { shownUpper = k; shownUpperWeight = w; }
            }
            // the rally plays once from its start and holds its last pose; the rest loop
            double clipTime = k.equals("stance_rally_upper") && !Double.isNaN(rallyStart)
                    ? Math.min(Math.max(0, now - rallyStart), clip.length()) : isStun(k) && !Double.isNaN(stunStart)
                    ? stunTime(now - stunStart, clip.length()) : k.equals(CHARGE)
                    ? gait.phase * clip.length() : gait.standingTime(now, clip.length());   // the charge arms swing in step with the run legs, which the stride clock drives; the rest loop on the world clock, each troop at its own point in the loop
            accumulate(clip, clipTime, bones, (float) w);
        }
        ramps.values().removeIf(v -> v <= 0);
        apply(bones);
        // the shake-off: for the clip's length after the stun ends, the head shakes and the chest straightens over the stun pose fading out
        Animation out = Double.isNaN(stunEnd) || now < stunEnd ? null : model.getAnimation(animatable, STUN_OUT_CLIP);
        if (out != null && now - stunEnd < STUN_OUT) addClip(out, (now - stunEnd) * out.length() / STUN_OUT, bones);
    }

    /** Share of the walk legs mixed into the stun legs at a full drift (a slow slide that is no longer the knockback itself). */
    static final double STUN_SHUFFLE = 0.6;

    /** Ticks the shuffle takes to come in and out: slow enough that the knockback slowing through its band and a unit reversing at a standstill do not make the legs pulse. */
    static final double STUN_SHUFFLE_RAMP = 6;

    /** The shuffle share, 0 planted or still being thrown up to 1 drifting at walking pace; eased, and held while the stun legs are still fading out so their weight does not bump. */
    private double stunDrift(double dt, boolean stunning) {
        double s = Math.abs(gait.speed);
        shuffle = step(shuffle, (stunning || ramps.getOrDefault("stun_legs", 0.0) > 0) && s > 0.05 && s < 0.30 ? 1 : 0, dt / STUN_SHUFFLE_RAMP);
        return shuffle;
    }

    private static boolean isStun(String clip) { return clip.equals("stun_upper") || clip.equals("stun_legs"); }

    /** The stun clip time {@code elapsed} ticks after the stun began: the entry once, then the rest of the clip on a loop. */
    static double stunTime(double elapsed, double length) {
        elapsed = Math.max(0, elapsed);
        if (elapsed < length || length <= STUN_ENTRY) return Math.min(elapsed, length);
        return STUN_ENTRY + (elapsed - STUN_ENTRY) % (length - STUN_ENTRY);
    }

    /** Adds an additive clip's offsets at {@code t} to every bone it keys (the clip starts and ends at zero). */
    private static void addClip(Animation clip, double t, Map<String, CoreGeoBone> bones) {
        for (BoneAnimation b : clip.boneAnimations()) {
            CoreGeoBone bone = bones.get(b.boneName());
            if (bone == null) continue;
            var rot = b.rotationKeyFrames();
            if (rot.xKeyframes().isEmpty()) continue;
            bone.setRotX(bone.getRotX() + AttackController.sample(rot.xKeyframes(), t));
            bone.setRotY(bone.getRotY() + AttackController.sample(rot.yKeyframes(), t));
            bone.setRotZ(bone.getRotZ() + AttackController.sample(rot.zKeyframes(), t));
            bone.markRotationAsChanged();
        }
    }

    /** Moves {@code v} toward {@code to} by at most {@code by}. */
    private static double step(double v, double to, double by) {
        return v < to ? Math.min(to, v + by) : Math.max(to, v - by);
    }

    /** Adds the clip's pose at {@code t}, by weight {@code w}, into each keyed bone's accumulator. */
    private void accumulate(Animation clip, double t, Map<String, CoreGeoBone> bones, float w) {
        for (BoneAnimation b : clip.boneAnimations()) {
            CoreGeoBone bone = bones.get(b.boneName());
            if (bone == null) continue;
            float[] a = acc.computeIfAbsent(b.boneName(), k -> new float[8]);
            var rot = b.rotationKeyFrames();
            if (!rot.xKeyframes().isEmpty()) {
                BoneSnapshot init = bone.getInitialSnapshot();
                // every pose is taken on the branch nearest what the gait wrote, so poses written either side of the gimbal blend as one rotation
                float[] to = AttackController.near(bone.getRotX(), bone.getRotY(), bone.getRotZ(), AttackController.sample(rot.xKeyframes(), t) + init.getRotX(),
                        AttackController.sample(rot.yKeyframes(), t) + init.getRotY(), AttackController.sample(rot.zKeyframes(), t) + init.getRotZ());
                a[0] += w * to[0];
                a[1] += w * to[1];
                a[2] += w * to[2];
                a[6] += w;
            }
            var pos = b.positionKeyFrames();
            if (!pos.xKeyframes().isEmpty()) {
                a[3] += w * AttackController.sample(pos.xKeyframes(), t);
                a[4] += w * AttackController.sample(pos.yKeyframes(), t);
                a[5] += w * AttackController.sample(pos.zKeyframes(), t);
                a[7] += w;
            }
        }
    }

    /**
     * Lerps each accumulated bone from what the gait wrote toward the weighted mean of the poses, by their summed weight (at most 1):
     * one pose alone at weight w is a plain lerp, two poses crossfading share the bone without overshooting.
     */
    private void apply(Map<String, CoreGeoBone> bones) {
        for (Map.Entry<String, float[]> en : acc.entrySet()) {
            float[] a = en.getValue();
            CoreGeoBone bone = bones.get(en.getKey());
            if (bone == null) continue;
            if (a[6] > 0) {
                float k = Math.min(1, a[6]);
                float[] r = AttackController.principal(lerp(bone.getRotX(), a[0] / a[6], k), lerp(bone.getRotY(), a[1] / a[6], k), lerp(bone.getRotZ(), a[2] / a[6], k));
                bone.setRotX(r[0]);
                bone.setRotY(r[1]);
                bone.setRotZ(r[2]);
                bone.markRotationAsChanged();
            }
            if (a[7] > 0) {
                float k = Math.min(1, a[7]);
                bone.setPosX(lerp(bone.getPosX(), a[3] / a[7], k));
                bone.setPosY(lerp(bone.getPosY(), a[4] / a[7], k));
                bone.setPosZ(lerp(bone.getPosZ(), a[5] / a[7], k));
                bone.markPositionAsChanged();
            }
        }
    }

    private static float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    private static float lerp(float a, float b, float w) { return a + (b - a) * w; }
}
