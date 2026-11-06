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
    /** The swing being tracked for the late-arrival fade: its start tick and the world time it was first seen. */
    private long seenStart = Long.MIN_VALUE;
    private double seenAt;
    /**
     * What the last frame showed, for the in-game checks (test-tools GeoKnightChecks): the strongest swing weight applied, the newest
     * swing's start tick, clip and clip time, and that clip's impact tick (its "timeline" impact marker, -1 if none). Never read in play.
     */
    double shownWeight, shownT = -1, shownImpact = -1;
    /** That frame's animation time (world ticks plus partial), and how far into dying the troop was (deathTime plus partial; -1 alive). */
    double shownAt = -1, shownDeath = -1;
    long shownStart = Long.MIN_VALUE;
    String shownClip = "";
    private boolean listed;
    /** Each drawn troop's attack layer, for the checks; weak, so unloaded troops drop out. */
    static final Map<Entity, AttackController> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    AttackController(GeoTroop troop) {
        super(troop, "attack", 0, s -> PlayState.STOP);
        String f = "";
        for (Spec.TroopsRow r : Spec.TROOPS) if (r.id().equals(troop.troop)) { f = r.family(); break; }
        family = f;
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
        if (latest != null && latest.startTick() != seenStart) { seenStart = latest.startTick(); seenAt = now; }

        // Oldest first: each playing swing blends over what is already on the bones.
        for (MotionInput.Attack a : MotionEvents.Client.attacks(e.getId())) {
            if (a.variant() < 0 || a.variant() >= variants.size()) continue;
            double t = now - a.startTick();
            if (t < 0) continue;
            Animation clip = model.getAnimation(animatable, clipName(family, variants.get(a.variant()).id()));
            if (clip == null || t >= clip.length()) continue;
            double w = Math.min(smooth(t / FADE_IN), smooth((clip.length() - t) / FADE_OUT)) * death;
            if (a == latest || a.startTick() == seenStart) w *= smooth((now - seenAt) / LATE_IN);
            if (w > 0) apply(clip, t, bones, (float) w);
            shownWeight = Math.max(shownWeight, w);
            if (a == latest) { shownStart = a.startTick(); shownT = t; shownClip = clip.name(); shownImpact = impact(clip); }
        }
    }

    /** The clip's impact tick: its "timeline" marker named impact (build_anims.py IMPACT), or -1. */
    static double impact(Animation clip) {
        for (var k : clip.keyFrames().customInstructions()) if ("impact".equals(k.getInstructions())) return k.getStartTick();
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
                bone.setRotX(lerp(bone.getRotX(), sample(rot.xKeyframes(), t) + init.getRotX(), w));
                bone.setRotY(lerp(bone.getRotY(), sample(rot.yKeyframes(), t) + init.getRotY(), w));
                bone.setRotZ(lerp(bone.getRotZ(), sample(rot.zKeyframes(), t) + init.getRotZ(), w));
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
}
