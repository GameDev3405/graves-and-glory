package gg.gravesandglory;

import com.eliotlash.mclib.math.IValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.keyframe.Keyframe;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The attack layer (unit animation rebuild, docs/anim-rebuild/PLAN.md, Bone ownership: the one exception). Registered after the
 * {@code upper} layer, it weight-blends the swing the server announced (MotionEvents attack_v1) over the upper bones that layer has
 * just written, so the arms leave the gait, strike, and hand back to it without a snap.
 * <p>
 * The swing plays on the world clock from the tick the server began it, so the clip's impact frame lands on the tick the server's
 * PendingStrike resolves (Tactics.swing): the animation shows the blow, the server alone decides it. Nothing here feeds back.
 * <p>
 * Swings still playing are applied oldest first, each over the result of the last, so a swing that starts while another is
 * finishing crossfades instead of popping.
 */
final class AttackController extends AnimationController<GeoTroop> {
    /** Fade into the swing over this many ticks from its start, and out over its last {@link #FADE_OUT} ticks. */
    static final double FADE_IN = 2.5, FADE_OUT = 6;
    /** A swing first heard of late (the packet came after its start) still eases in over this many ticks from when it arrived. */
    static final double LATE_IN = 2;
    /** A troop that dies mid-swing lets the swing go over this many ticks. */
    static final double DEATH_OUT = 3;

    private final String family;
    /** Whether this troop's shots hold their full draw (Tactics.drawsAhead: a non-magic ranged bow troop), as Projectiles.hold does on the server. */
    private final boolean draws;
    /** The stance layer, whose standing factor scales the swing's footwork (attack_&lt;name&gt;_legs): planted feet never slide. */
    private final StanceController stance;
    /** The swing being tracked for the late-arrival fade: its start tick and the world time it was first seen. */
    private long seenStart = Long.MIN_VALUE;
    private double seenAt;
    /** The animation time of the last frame this troop was drawn: a troop the camera did not draw (culled) hears nothing late. */
    private double drawnAt = Double.NEGATIVE_INFINITY;
    /**
     * What the last frame showed, for the in-game checks (test-tools GeoKnightChecks): the strongest swing weight applied, the newest
     * swing's start tick, clip and clip time, and that clip's impact tick (its "timeline" impact marker, -1 if none). Never read in play.
     */
    /** `still` at or below this counts as walking: the swing's standing footwork is off (weight exactly 0). */
    static final double STILL_SETTLED = .001;
    double shownWeight, shownLegsWeight, shownT = -1, shownImpact = -1;
    /** That frame's animation time (world ticks plus partial), and how far into dying the troop was (deathTime plus partial; -1 alive). */
    double shownAt = -1, shownDeath = -1;
    /** When the newest swing was first heard of (its late-arrival fade's start). */
    double shownSeenAt = -1;
    /** How fast the newest swing's clip ran against the world clock that frame (above 1: a late wind-up quickened). */
    double shownRate = 1;
    long shownStart = Long.MIN_VALUE;
    String shownClip = "";
    private boolean listed;
    /** Each drawn troop's attack layer, for the checks; weak, so unloaded troops drop out. */
    static final Map<Entity, AttackController> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    AttackController(GeoTroop troop, StanceController stance) {
        super(troop, "attack", 0, s -> PlayState.STOP);
        this.stance = stance;
        String f = "";
        boolean d = false;
        for (Spec.TroopsRow r : Spec.TROOPS) if (r.id().equals(troop.troop)) { f = r.family(); d = r.ranged() && !r.magic() && "bow".equals(f); break; }
        family = f;
        draws = d;
    }

    /** The clip a swing plays: attack_&lt;variant id without its family prefix&gt;, e.g. sword_shield_thrust plays attack_thrust. */
    static String clipName(String family, String variantId) {
        return "attack_" + (variantId.startsWith(family + "_") ? variantId.substring(family.length() + 1) : variantId);
    }

    @Override
    public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Entity e = state.getData(DataTickets.ENTITY);
        if (e == null || e.getWorld() == null || family.isEmpty()) return;
        if (!listed) { listed = true; LIVE.put(e, this); }
        shownWeight = 0;
        shownLegsWeight = 0;
        List<Spec.AttackVariantsRow> variants = Tactics.variants(family);
        if (variants.isEmpty()) return;
        double now = e.getWorld().getTime() + state.getPartialTick();
        double death = 1;
        shownAt = now; shownDeath = -1;
        if (e instanceof LivingEntity l && (l.isDead() || l.deathTime > 0)) {
            shownDeath = l.deathTime + state.getPartialTick();
            death = 1 - smooth(shownDeath / DEATH_OUT);
            if (death <= 0) return;
        }
        MotionInput.Attack latest = null;
        for (MotionInput.Attack a : MotionEvents.Client.attacks(e.getId())) if (a.startTick() <= now && (latest == null || a.startTick() > latest.startTick())) latest = a;
        // A swing first seen when the troop comes back into view (it was not drawn last frame) was not heard late: the clip plays
        // from its real time at full weight instead of fading in, so panning onto a troop mid-shot shows the shot as it is.
        boolean wasDrawn = now - drawnAt <= 1.5;
        drawnAt = now;
        if (latest != null && latest.startTick() != seenStart) { seenStart = latest.startTick(); seenAt = wasDrawn ? now : Math.min(now, latest.startTick()); }

        // Oldest first: each playing swing blends over what is already on the bones.
        for (MotionInput.Attack a : MotionEvents.Client.attacks(e.getId())) {
            if (a.variant() < 0 || a.variant() >= variants.size()) continue;
            double raw = now - a.startTick();
            if (raw < 0) continue;
            Animation clip = model.getAnimation(animatable, clipName(family, variants.get(a.variant()).id()));
            if (clip == null) continue;
            // A bow shot holds its full draw Seed.hold ticks (the server started its clip that much earlier, Projectiles.shotStart),
            // so the loose still lands on the impact marker. The held span stretches over the clip's own full-to-impact hold (its
            // trembling aim) rather than freezing a frame (STYLE.md rule 2: alive even when still).
            int hold = draws ? Projectiles.seed(e.getUuid()).hold() : 0;
            double full = hold > 0 ? marker(clip, "full") : -1;
            double held = hold > 0 ? impact(clip) : -1;
            if (full < 0 || held <= full) hold = 0;
            double len = clip.length() + hold;
            if (raw >= len) continue;
            double t = raw;
            if (hold > 0 && raw > full)
                t = raw < held + hold ? full + (raw - full) * (held - full) / (held - full + hold) : raw - hold;
            // A swing first heard of a tick or more after it began (a bow's draw is announced one beat, 10 ticks, ahead of a loose its
            // clip reaches 16 or more ticks after it starts) plays its whole wind-up, quickened, from when it arrived up to its full
            // draw (a melee swing: its impact), so the loose or the blow still lands on the server's tick.
            double late = a.startTick() == seenStart ? seenAt - seenStart : 0;
            double until = hold > 0 ? full : impact(clip);
            double rate = 1;
            if (late >= 1 && until - late >= 2 && raw < until) { rate = until / (until - late); t = Math.max(0, (raw - late) * rate); }
            double w = Math.min(smooth(raw / FADE_IN), smooth((len - raw) / FADE_OUT)) * death;
            if (a == latest || a.startTick() == seenStart) w *= smooth((now - seenAt) / LATE_IN);
            if (w > 0) apply(clip, t, bones, (float) w);
            // Standing, the swing's own footwork (STYLE.md rule 4) plays over the legs: the step in, the hip drop, the step back.
            // Once the gait has walked long enough for `still` to settle (<= STILL_SETTLED) the footwork is gone outright, not left as a
            // smooth(still) residue of a few millionths: the walk checks (and any later layer that tests weight > 0) see exactly 0.
            double sv = stance.still();
            double lw = sv <= STILL_SETTLED ? 0 : w * smooth(sv);
            Animation legs = lw > 0 ? model.getAnimation(animatable, clip.name() + "_legs") : null;
            if (legs != null && t < legs.length()) {
                apply(legs, t, bones, (float) lw);
                shownLegsWeight = Math.max(shownLegsWeight, lw);
            }
            shownWeight = Math.max(shownWeight, w);
            if (a == latest) { shownStart = a.startTick(); shownT = t; shownClip = clip.name(); shownImpact = impact(clip); shownSeenAt = a.startTick() == seenStart ? seenAt : -1; shownRate = rate; }
        }
    }

    /** The clip's impact tick: its "timeline" marker named impact (build_anims.py IMPACT), or -1. */
    static double impact(Animation clip) { return marker(clip, "impact"); }

    /** The tick of the clip's "timeline" marker with this name (build_anims.py: impact; a bow shot's full draw: full), or -1. */
    static double marker(Animation clip, String name) {
        for (var k : clip.keyFrames().customInstructions()) if (name.equals(k.getInstructions())) return k.getStartTick();
        return -1;
    }

    /** Lerps every bone the clip keys toward the clip's pose at tick {@code t}, by weight {@code w}. */
    static void apply(Animation clip, double t, Map<String, CoreGeoBone> bones, float w) {
        for (BoneAnimation b : clip.boneAnimations()) {
            CoreGeoBone bone = bones.get(b.boneName());
            if (bone == null) continue;
            var rot = b.rotationKeyFrames();
            if (!rot.xKeyframes().isEmpty()) {
                BoneSnapshot init = bone.getInitialSnapshot();
                float[] to = near(bone.getRotX(), bone.getRotY(), bone.getRotZ(), sample(rot.xKeyframes(), t) + init.getRotX(),
                        sample(rot.yKeyframes(), t) + init.getRotY(), sample(rot.zKeyframes(), t) + init.getRotZ());
                float[] r = principal(lerp(bone.getRotX(), to[0], w), lerp(bone.getRotY(), to[1], w), lerp(bone.getRotZ(), to[2], w));
                bone.setRotX(r[0]);
                bone.setRotY(r[1]);
                bone.setRotZ(r[2]);
                bone.markRotationAsChanged();
            }
            var pos = b.positionKeyFrames();
            if (!pos.xKeyframes().isEmpty()) {
                bone.setPosX(lerp(bone.getPosX(), sample(pos.xKeyframes(), t), w));
                bone.setPosY(lerp(bone.getPosY(), sample(pos.yKeyframes(), t), w));
                bone.setPosZ(lerp(bone.getPosZ(), sample(pos.zKeyframes(), t), w));
                bone.markPositionAsChanged();
            }
        }
    }

    /** One channel's value at tick {@code t}: the clips are baked densely (build_anims.py), so linear between keys is exact enough. */
    static float sample(List<Keyframe<IValue>> keys, double t) {
        double at = 0;
        for (Keyframe<IValue> k : keys) {
            if (t <= at + k.length()) {
                double f = k.length() <= 0 ? 1 : (t - at) / k.length();
                return (float) (k.startValue().get() + (k.endValue().get() - k.startValue().get()) * f);
            }
            at += k.length();
        }
        return keys.isEmpty() ? 0 : (float) keys.get(keys.size() - 1).endValue().get();
    }

    static double smooth(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private static float lerp(float a, float b, float w) { return a + (b - a) * w; }

    private static final float PI = (float) Math.PI, TAU = 2 * PI;

    /** {@code a} plus a whole number of turns, as near {@code ref} as it gets. */
    private static float wrapTo(float a, float ref) { return a - TAU * Math.round((a - ref) / TAU); }

    /**
     * The Euler triple (radians, applied Z then Y then X as GeckoLib does) with the same rotation as (x, y, z) that lies nearest the pose (rx, ry, rz): each angle taken
     * within half a turn of the reference, or the other triple of the pair, (x + 180, 180 - y, z + 180), when that is nearer. Blending two poses angle by angle is only
     * sound between triples on the same branch: a pose written past the gimbal (the open aim stance has its torso at about (-178, 87, 180) degrees, the same rotation as
     * (2, 93, 0)) lerped against one written plainly sweeps through a tumble that is nothing like either, for the ticks the blend lasts.
     */
    static float[] near(float rx, float ry, float rz, float x, float y, float z) {
        float ax = wrapTo(x, rx), ay = wrapTo(y, ry), az = wrapTo(z, rz);
        float bx = wrapTo(x + PI, rx), by = wrapTo(PI - y, ry), bz = wrapTo(z + PI, rz);
        float da = sq(ax - rx) + sq(ay - ry) + sq(az - rz), db = sq(bx - rx) + sq(by - ry) + sq(bz - rz);
        return db < da - 1e-4f ? new float[]{bx, by, bz} : new float[]{ax, ay, az};
    }

    /** The same rotation with Y inside a quarter turn (the branch the clips are written on and the head look and the other offsets are added to), when it was not already. */
    static float[] principal(float x, float y, float z) {
        if (Math.abs(y) <= PI / 2) return new float[]{x, y, z};
        y = wrapTo(y, 0);
        if (Math.abs(y) <= PI / 2) return new float[]{x, y, z};
        return new float[]{wrapTo(x + PI, 0), (y > 0 ? PI : -PI) - y, wrapTo(z + PI, 0)};
    }

    private static float sq(float v) { return v * v; }
}
