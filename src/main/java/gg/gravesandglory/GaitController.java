package gg.gravesandglory;

import net.minecraft.entity.Entity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * A GeckoLib controller whose walk and run loops are driven by ground distance, not time (see {@link Gait}). The idle loop runs on
 * a free clock at each troop's own point in the loop ({@link Gait#standingTime}), so troops standing together do not breathe as one.
 * <p>
 * GeckoLib transitions always blend into a clip's first frame. The {@code owner} (the legs layer) therefore restarts the shared
 * phase at 0 the moment its transition into a gait clip ends, so the blend lands exactly where the stride clock starts and nothing
 * snaps; the arms layer, registered after it, reads the same phase.
 * <p>
 * Setting off from a standstill is handled apart from GeckoLib's transition, which blends over several ticks toward the clip's first frame while the body already
 * travels (the planted feet are carried with it: about 3.5 px a tick for ~4 ticks). The clip instead starts at once on the phase whose legs sit nearest the idle pose
 * ({@link #matchPhase}), and this controller blends the idle pose it was holding into the moving clip itself, over {@code startBlend} ticks, with the clip already
 * running on the stride clock. Stopping is blended by hand the same way, straight over GeckoLib's transition length: its transition would land on the idle loop's
 * first frame, and the loop is already running somewhere else.
 * <p>
 * GeckoLib puts a controller's clip pose on the bones after {@link #process} returns, so a blend written inside it is overwritten. The hand blends therefore run in
 * {@link #after}, which {@link After}, registered behind both gait layers, calls once their poses are on the bones. Until 2026-11-12 the setting-off blend ran inside
 * {@code process} and so never showed in play: a troop setting off shows the moving clip's first frame for one render frame, then the matched phase.
 * {@link #START_BLEND} keeps that as it was approved.
 * <p>
 * {@link After} also puts the bones no gait clip keyed this frame at rest ({@link #rest}), so the layers behind it never build on what an earlier draw left there.
 */
final class GaitController extends AnimationController<GeoTroop> {
    /** Reference speed of each gait clip (blocks per tick) as built by art/anim/knight/build_anims.py; the clip name is its key prefix. */
    static double refSpeed(String clip) {
        return clip.startsWith("walk_") ? .16 : clip.startsWith("run_") ? .26 : 0;
    }

    final Gait gait;
    final boolean owner;
    private final int blend;
    /** Ticks the idle pose is blended into a moving clip after setting off from a standstill (see the class comment). */
    private final double startBlend;
    /** What this controller last wrote to the bones its clip keys (rotation xyz, position xyz), and the idle pose being blended out of while a start blend runs. */
    private final Map<String, float[]> out = new HashMap<>();
    /** What this controller's own clip, and its hand blend, put on its bones last frame: the pose under the stance, attack and hit layers, which a stop blends out of. */
    private final Map<String, float[]> wrote = new HashMap<>();
    /**
     * Whether a troop setting off shows the blend out of its idle pose. Off: that blend never showed in play before {@link #after} existed (see the class comment), and
     * the knight was approved without it; turning it on is a change to how a troop sets off, for Kort to look at.
     */
    static final boolean START_BLEND = false;
    private Map<String, float[]> from;
    private double fromAt, fromLength;
    /** Whether the blend out of {@link #from} is eased (setting off) or straight (stopping, as GeckoLib's own transition was). */
    private boolean fromEased;
    private Gait.Mode lastMode = Gait.Mode.IDLE;
    /** A walk-to-run (or back) switch: no timed blend, and the stride clock keeps its phase, so the feet stay on the stride clock instead of skating through the blend. */
    private boolean keepPhase;

    GaitController(GeoTroop troop, String name, int transition, double startBlend, Gait gait, boolean owner, AnimationStateHandler<GeoTroop> handler) {
        super(troop, name, transition, handler);
        this.gait = gait;
        this.owner = owner;
        this.blend = transition;
        this.startBlend = startBlend;
    }

    /** The playing clip's reference speed, or 0 when no gait clip is running. */
    double playingRef() {
        return currentAnimation == null ? 0 : refSpeed(currentAnimation.animation().name());
    }

    @Override
    public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Entity e = state.getData(DataTickets.ENTITY);
        if (e != null) gait.update(e, seekTime, state.getPartialTick());
        Gait.Mode m = gait.mode;
        if (m != lastMode && m != Gait.Mode.IDLE && lastMode != Gait.Mode.IDLE) { transitionLength = 0; keepPhase = true; }
        else if (m != lastMode && m != Gait.Mode.IDLE && !out.isEmpty()) {
            // setting off from a standstill: straight into the moving clip on the phase that matches the idle pose, blended by hand
            transitionLength = 0; keepPhase = true;
            if (START_BLEND && !wrote.isEmpty()) { from = copy(wrote); fromAt = seekTime; fromLength = startBlend; fromEased = true; }
            else from = null;
            if (owner) gait.phase = matchPhase(model, bones, m == Gait.Mode.RUN ? "run_legs" : "walk_legs");
        }
        else if (m != lastMode && m == Gait.Mode.IDLE && !wrote.isEmpty()) {
            // stopping: the idle loop is already running at this troop's own point in it (Gait.standingTime), so GeckoLib's transition, which lands on the loop's
            // first frame, would cut to that point when it ends; the pose held is blended into the running loop by hand instead, over the same ticks
            transitionLength = 0; keepPhase = true;
            from = copy(wrote); fromAt = seekTime; fromLength = blend; fromEased = false;
        }
        else if (out.isEmpty()) { transitionLength = 0; keepPhase = true; }   // first drawn: straight into its loop, no blend out of the bind pose
        else if (!keepPhase) transitionLength = blend;
        lastMode = m;
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        if (keepPhase && !shouldResetTick && getAnimationState() == State.RUNNING) keepPhase = false;
        capture(bones);
        if (owner) {
            double ref = getAnimationState() == State.RUNNING ? playingRef() : 0;
            gait.stride = ref > 0 ? ref * currentAnimation.animation().length() : 0;
        }
    }

    /**
     * What this controller's bones read now, kept for the next start. Every bone it has ever keyed is read, not only those of the clip playing: a bone the idle clip leaves unkeyed
     * would otherwise keep the stale mid-stride value of the moving clip, and the next start would blend out of a pose the troop never held.
     */
    private void capture(Map<String, CoreGeoBone> bones) {
        if (currentAnimation != null) for (BoneAnimation b : currentAnimation.animation().boneAnimations()) out.computeIfAbsent(b.boneName(), k -> new float[6]);
        for (Map.Entry<String, float[]> en : out.entrySet()) {
            CoreGeoBone bone = bones.get(en.getKey());
            if (bone == null) continue;
            float[] a = en.getValue();
            a[0] = bone.getRotX(); a[1] = bone.getRotY(); a[2] = bone.getRotZ();
            a[3] = bone.getPosX(); a[4] = bone.getPosY(); a[5] = bone.getPosZ();
        }
    }

    /**
     * Runs once this controller's clip pose is on the bones (see the class comment): blends the pose held when the troop stopped (or set off, with {@link #START_BLEND})
     * into it, then keeps what the layer now shows for the next blend to start from.
     */
    void after(Map<String, CoreGeoBone> bones, double seekTime) {
        if (from != null && seekTime < fromAt) from = null;   // the animation time went back (the troop was tracked anew): nothing to finish
        if (from != null) {
            double k = fromLength <= 0 ? 1 : Math.min(1, (seekTime - fromAt) / fromLength);
            blendFrom(bones, fromEased ? AttackController.smooth(k) : k, k >= 1);
        }
        for (String name : out.keySet()) {
            CoreGeoBone bone = bones.get(name);
            if (bone == null) continue;
            float[] a = wrote.computeIfAbsent(name, n -> new float[6]);
            a[0] = bone.getRotX(); a[1] = bone.getRotY(); a[2] = bone.getRotZ();
            a[3] = bone.getPosX(); a[4] = bone.getPosY(); a[5] = bone.getPosZ();
        }
    }

    /** The stage behind both gait layers: by the time it runs GeckoLib has put their clip poses on the bones, so their hand blends hold ({@link GaitController#after}). */
    static final class After extends AnimationController<GeoTroop> {
        private final GaitController legs, upper;

        After(GeoTroop troop, GaitController legs, GaitController upper) {
            super(troop, "gait_after", 0, s -> software.bernie.geckolib.core.object.PlayState.STOP);
            this.legs = legs;
            this.upper = upper;
        }

        @Override
        public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                            Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
            rest(bones);
            legs.after(bones, seekTime);
            upper.after(bones, seekTime);
        }
    }

    /**
     * Puts every bone the gait layers did not write this frame at its rest pose, before anything reads it. The bones are the model's, shared by every troop drawn with it,
     * and GeckoLib resets only a bone nothing touched: one the playing gait clips do not key still holds what the last draw left on it, this troop's last frame or another
     * troop's. The stop blend, and the stance, attack, hit and death layers behind it, all read a bone before they write it, so on such a bone they built on that leftover:
     * a hit clip's offset was added onto last frame's offset, every frame (a pauldron that span through a blow), and a pose faded in and out from itself. Started from rest,
     * each layer's result depends only on this frame. Setting a bone marks it changed, so GeckoLib's own reset (a one-tick lerp to this same pose) no longer runs for these.
     */
    static void rest(Map<String, CoreGeoBone> bones) {
        for (CoreGeoBone bone : bones.values()) {
            BoneSnapshot init = bone.getInitialSnapshot();
            if (!bone.hasRotationChanged()) { bone.setRotX(init.getRotX()); bone.setRotY(init.getRotY()); bone.setRotZ(init.getRotZ()); }
            if (!bone.hasPositionChanged()) { bone.setPosX(init.getOffsetX()); bone.setPosY(init.getOffsetY()); bone.setPosZ(init.getOffsetZ()); }
        }
    }

    private static Map<String, float[]> copy(Map<String, float[]> m) {
        Map<String, float[]> c = new HashMap<>();
        for (Map.Entry<String, float[]> en : m.entrySet()) c.put(en.getKey(), en.getValue().clone());
        return c;
    }

    /** Lerps each bone from the pose it held at the start (setting off: the idle pose; stopping: the stride) toward what the clip wrote this frame, by {@code w}; done once the blend has run its length. */
    private void blendFrom(Map<String, CoreGeoBone> bones, double w, boolean done) {
        float k = (float) w;
        for (Map.Entry<String, float[]> en : from.entrySet()) {
            CoreGeoBone bone = bones.get(en.getKey());
            if (bone == null) continue;
            float[] a = en.getValue();
            float[] r = AttackController.near(a[0], a[1], a[2], bone.getRotX(), bone.getRotY(), bone.getRotZ());
            bone.setRotX(a[0] + (r[0] - a[0]) * k);
            bone.setRotY(a[1] + (r[1] - a[1]) * k);
            bone.setRotZ(a[2] + (r[2] - a[2]) * k);
            bone.setPosX(a[3] + (bone.getPosX() - a[3]) * k);
            bone.setPosY(a[4] + (bone.getPosY() - a[4]) * k);
            bone.setPosZ(a[5] + (bone.getPosZ() - a[5]) * k);
            bone.markRotationAsChanged();
            bone.markPositionAsChanged();
        }
        if (done) from = null;
    }

    /** How much a bone's turn moves the foot, by name: a thigh most, a shin half, a foot little; 0 for every other bone. */
    private static double legWeight(String bone) {
        return bone.endsWith("leg") ? 1 : bone.endsWith("shin") ? .5 : bone.endsWith("foot") ? .15 : 0;
    }

    /**
     * The phase (0..1) of the gait clip {@code name} whose leg rotations lie nearest the pose this controller was holding, so a troop that sets off steps out of the idle
     * stance without its planted foot jumping. 0 when the model lacks the clip or its leg bones.
     */
    private double matchPhase(CoreGeoModel<GeoTroop> model, Map<String, CoreGeoBone> bones, String name) {
        Animation clip = model.getAnimation(animatable, name);
        if (clip == null) return 0;
        final int steps = 48;
        double best = Double.MAX_VALUE, at = 0;
        for (int i = 0; i < steps; i++) {
            double t = clip.length() * i / steps, cost = 0;
            for (BoneAnimation b : clip.boneAnimations()) {
                double w = legWeight(b.boneName());
                float[] now = out.get(b.boneName());
                CoreGeoBone bone = bones.get(b.boneName());
                var rot = b.rotationKeyFrames();
                if (w == 0 || now == null || bone == null || rot.xKeyframes().isEmpty()) continue;
                BoneSnapshot init = bone.getInitialSnapshot();
                float[] to = AttackController.near(now[0], now[1], now[2], AttackController.sample(rot.xKeyframes(), t) + init.getRotX(),
                        AttackController.sample(rot.yKeyframes(), t) + init.getRotY(), AttackController.sample(rot.zKeyframes(), t) + init.getRotZ());
                cost += w * (Math.abs(to[0] - now[0]) + Math.abs(to[1] - now[1]) + Math.abs(to[2] - now[2]));
            }
            if (cost < best) { best = cost; at = (double) i / steps; }
        }
        return at;
    }

    /** Whether the idle loop is the clip playing. */
    private boolean idling() {
        return currentAnimation != null && currentAnimation.animation().name().startsWith("idle_");
    }

    @Override
    protected double adjustTick(double tick) {
        if (getAnimationState() == State.RUNNING && idling()) {
            // The idle loop runs on a free clock at this troop's own point in the loop (Gait.standingTime), never from its first frame: a rank does not breathe as one.
            // keepPhase is left for process to clear: a troop that stops and sets off again on the next frame still needs it for the moving clip
            if (shouldResetTick) super.adjustTick(tick);
            // read on the animation time, which runs evenly frame to frame; the world clock is set again from the server about once a second and would step the loop on a slow one
            return gait.standingTime(tick, currentAnimation.animation().length());
        }
        double ref = playingRef();
        if (ref <= 0 || getAnimationState() != State.RUNNING) return super.adjustTick(tick);
        if (shouldResetTick) {
            // The transition into this loop just finished on its first frame: start the stride clock there.
            super.adjustTick(tick);
            if (keepPhase) { keepPhase = false; return gait.phase * currentAnimation.animation().length(); }
            if (owner) gait.phase = 0;
            return 0;
        }
        return gait.phase * currentAnimation.animation().length();
    }
}
