package gg.gravesandglory;

import net.minecraft.entity.Entity;
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
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The hit layer (unit animation rebuild, docs/anim-rebuild/PLAN.md, "Damage"). Registered last, it adds each recent blow's reaction
 * on top of whatever the other layers wrote, so a knight flinches mid-swing, mid-stride or in the shield wall without losing it.
 * <p>
 * The reaction plays from the tick the blow landed (MotionEvents hit_v1). A blow from the front drives the chest back, one from
 * the right drives the right shoulder back and bends the body away; the four directional clips are mixed by the blow's bearing.
 * Light blows flinch, medium and heavy ones stagger (heavy deeper). A blow the shield took plays the block instead: the shield arm
 * is driven back and the body absorbs it. Nothing here feeds back to the server.
 */
final class HitController extends AnimationController<GeoTroop> {
    /** A blow first heard of late still eases in over this many ticks from when it arrived (see AttackController.LATE_IN). */
    static final double LATE_IN = 1.5;
    /** Heavy blows play the medium stagger this much deeper; a blocked blow is scaled by weight with {@link #BLOCK_SCALE}. */
    static final double HEAVY = 1.55;
    static final double[] BLOCK_SCALE = {0.6, 1.0, 1.3};
    static final String[] DIRS = {"front", "back", "right", "left"};

    private long seenTick = Long.MIN_VALUE;
    private double seenAt;
    /** The newest blow this frame played, for the in-game checks (GeoKnightChecks): its clip(s), scale and time. Never read in play. */
    String shownClip = "";
    double shownScale, shownT = -1;
    /** That blow's direction mix, front back right left. */
    final double[] shownMix = new double[4];
    private final double[] mixBuf = new double[4];
    private boolean listed;
    /** The stance layer, whose standing factor scales each reaction's footwork (&lt;clip&gt;_legs): a walking troop keeps its gait. */
    private final StanceController stance;
    /** That frame's footwork weight, for the in-game checks. */
    double shownLegsWeight;
    static final Map<Entity, HitController> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    HitController(GeoTroop troop, StanceController stance) {
        super(troop, "hit", 0, s -> PlayState.STOP);
        this.stance = stance;
    }

    /** The share of each directional clip (front, back, right, left) for a blow from {@code bearing} degrees (+ from the right). */
    static void mix(double bearing, double[] out) {
        double r = Math.toRadians(bearing), c = Math.cos(r), s = Math.sin(r), n = Math.abs(c) + Math.abs(s);
        out[0] = Math.max(0, c) / n; out[1] = Math.max(0, -c) / n; out[2] = Math.max(0, s) / n; out[3] = Math.max(0, -s) / n;
    }

    @Override
    public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Entity e = state.getData(DataTickets.ENTITY);
        if (e == null || e.getWorld() == null) return;
        if (!listed) { listed = true; LIVE.put(e, this); }
        double now = e.getWorld().getTime() + state.getPartialTick();
        MotionInput.Hit latest = null;
        for (MotionInput.Hit h : MotionEvents.Client.hits(e.getId())) if (h.tick() <= now && (latest == null || h.tick() >= latest.tick())) latest = h;
        if (latest != null && latest.tick() != seenTick) { seenTick = latest.tick(); seenAt = now; }
        shownScale = 0; shownT = -1; shownClip = ""; shownLegsWeight = 0;
        double still = AttackController.smooth(stance.still());
        double[] m = mixBuf;
        for (MotionInput.Hit h : MotionEvents.Client.hits(e.getId())) {
            double t = now - h.tick();
            if (t < 0) continue;
            double k;
            if (h.blocked() && !(h.knockback() && model.getAnimation(animatable, "hit_knockback") != null)) { // a thrown target shows the throw even behind a shield, when the model has one
                k = BLOCK_SCALE[Math.max(0, Math.min(2, h.weight()))];
                Animation clip = model.getAnimation(animatable, "hit_block");
                if (clip == null || t >= clip.length()) continue;
                if (h.tick() == seenTick) k *= AttackController.smooth((now - seenAt) / LATE_IN);
                add(clip, t, bones, (float) k);
                legs(model, clip, t, bones, k * still);
                if (h == latest) { shownClip = clip.name(); shownScale = k; shownT = t; java.util.Arrays.fill(shownMix, 0); }
                continue;
            }
            if (h.knockback()) { // thrown: the model's own hit_knockback (and hit_knockback_legs) when it has one; else the medium stagger below
                Animation clip = model.getAnimation(animatable, "hit_knockback");
                if (clip != null && t < clip.length()) {
                    k = h.weight() >= 2 ? HEAVY : 1;
                    if (h.tick() == seenTick) k *= AttackController.smooth((now - seenAt) / LATE_IN);
                    add(clip, t, bones, (float) k);
                    legs(model, clip, t, bones, k * still);
                    if (h == latest) { shownClip = clip.name(); shownScale = k; shownT = t; java.util.Arrays.fill(shownMix, 0); }
                    continue;
                }
            }
            String w = h.weight() == 0 && !h.knockback() ? "light" : "medium";
            k = h.weight() >= 2 ? HEAVY : 1;
            if (h.tick() == seenTick) k *= AttackController.smooth((now - seenAt) / LATE_IN);
            mix(h.bearing(), m);
            boolean any = false;
            for (int i = 0; i < 4; i++) {
                if (m[i] <= 0) continue;
                Animation clip = model.getAnimation(animatable, "hit_" + w + "_" + DIRS[i]);
                if (clip == null || t >= clip.length()) continue;
                add(clip, t, bones, (float) (k * m[i]));
                legs(model, clip, t, bones, k * m[i] * still);
                any = true;
            }
            if (any && h == latest) { shownClip = "hit_" + w; shownScale = k; shownT = t; System.arraycopy(m, 0, shownMix, 0, 4); }
        }
    }

    /** Adds the footwork under {@code clip} (its _legs clip: hips, legs and feet, rotations and the hips' drop), times {@code k}. */
    private void legs(CoreGeoModel<GeoTroop> model, Animation clip, double t, Map<String, CoreGeoBone> bones, double k) {
        if (k <= 0) return;
        Animation legs = model.getAnimation(animatable, clip.name() + "_legs");
        if (legs == null || t >= legs.length()) return;
        add(legs, t, bones, (float) k);
        shownLegsWeight += k;   // total footwork weight this frame (bearing mixes split it across clips)
    }

    /** Adds the clip's offsets at {@code t}, times {@code k}, to every bone it keys. The clips start and end at zero. */
    private static void add(Animation clip, double t, Map<String, CoreGeoBone> bones, float k) {
        for (BoneAnimation b : clip.boneAnimations()) {
            CoreGeoBone bone = bones.get(b.boneName());
            if (bone == null) continue;
            var rot = b.rotationKeyFrames();
            if (rot.xKeyframes().isEmpty()) continue;
            bone.setRotX(bone.getRotX() + k * AttackController.sample(rot.xKeyframes(), t));
            bone.setRotY(bone.getRotY() + k * AttackController.sample(rot.yKeyframes(), t));
            bone.setRotZ(bone.getRotZ() + k * AttackController.sample(rot.zKeyframes(), t));
            bone.markRotationAsChanged();
            var pos = b.positionKeyFrames();
            if (pos.xKeyframes().isEmpty()) continue;
            bone.setPosX(bone.getPosX() + k * AttackController.sample(pos.xKeyframes(), t));
            bone.setPosY(bone.getPosY() + k * AttackController.sample(pos.yKeyframes(), t));
            bone.setPosZ(bone.getPosZ() + k * AttackController.sample(pos.zKeyframes(), t));
            bone.markPositionAsChanged();
        }
    }
}
