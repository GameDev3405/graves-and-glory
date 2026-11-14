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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A GeckoLib controller whose walk and run loops are driven by ground distance, not time (see {@link Gait}). The idle loop runs on
 * a free clock at each troop's own point in the loop ({@link Gait#standingTime}), so troops standing together do not breathe as one.
 * <p>
 * GeckoLib transitions always blend into a clip's first frame. The {@code owner} (the legs layer) therefore restarts the shared
 * phase at 0 the moment its transition into a gait clip ends, so the blend lands exactly where the stride clock starts and nothing
 * snaps; the arms layer, registered after it, reads the same phase.
 * <p>
 * Setting off from a standstill is handled apart from GeckoLib's transition, which blends over several ticks toward the clip's first frame while the body already
 * travels (the planted feet are carried with it: about 3.5 px a tick for ~4 ticks). The clip instead starts at once on the phase whose legs sit nearest the legs the
 * troop was last drawn with ({@link #matchPhase}, {@link Shown}), and this controller blends the idle pose it was holding into the moving clip itself, over {@code startBlend} ticks, with the clip already
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
    /** Every bone a clip of this controller has keyed so far: the bones {@link #after} keeps the pose of. Empty until the troop is first drawn. */
    private final Set<String> keyed = new HashSet<>();
    /**
     * What this controller's own clip, and its hand blend, put on its bones last frame (rotation xyz, position xyz): this troop's pose under the stance, attack and hit
     * layers, which a stop blends out of. Read from the bones in {@link #after}, once this frame's pose is on them: what they hold when {@link #process} starts is the
     * last draw of the model, which may be another troop's.
     */
    private final Map<String, float[]> wrote = new HashMap<>();
    /**
     * The legs layer only: this troop's leg bones as it was last drawn, with every layer on (rotation xyz), which a set-off matches its stride to ({@link #matchPhase}).
     * Kept by {@link Shown}, behind the last layer, for the same reason {@link #wrote} is kept: the bones themselves hold the model's last draw, not this troop's.
     */
    private final Map<String, float[]> shown = new HashMap<>();
    /** The model's leg bones ({@link #legWeight}), found on the first frame. */
    private String[] legBones;
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
        else if (m != lastMode && m != Gait.Mode.IDLE && !keyed.isEmpty()) {
            // setting off from a standstill: straight into the moving clip on the phase that matches the legs this troop was last drawn with, blended by hand
            transitionLength = 0; keepPhase = true;
            if (START_BLEND && !wrote.isEmpty()) { from = copy(wrote); fromAt = seekTime; fromLength = startBlend; fromEased = true; }
            else from = null;
            Animation legs = owner ? model.getAnimation(animatable, m == Gait.Mode.RUN ? "run_legs" : "walk_legs") : null;
            if (owner) gait.phase = legs == null ? 0 : matchPhase(legs, bones, shown);
        }
        else if (m != lastMode && m == Gait.Mode.IDLE && !wrote.isEmpty()) {
            // stopping: the idle loop is already running at this troop's own point in it (Gait.standingTime), so GeckoLib's transition, which lands on the loop's
            // first frame, would cut to that point when it ends; the pose held is blended into the running loop by hand instead, over the same ticks
            transitionLength = 0; keepPhase = true;
            from = copy(wrote); fromAt = seekTime; fromLength = blend; fromEased = false;
        }
        else if (keyed.isEmpty()) { transitionLength = 0; keepPhase = true; }   // first drawn: straight into its loop, no blend out of the bind pose
        else if (!keepPhase) transitionLength = blend;
        lastMode = m;
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        if (keepPhase && !shouldResetTick && getAnimationState() == State.RUNNING) keepPhase = false;
        if (currentAnimation != null) for (BoneAnimation b : currentAnimation.animation().boneAnimations()) keyed.add(b.boneName());
        if (owner) {
            double ref = getAnimationState() == State.RUNNING ? playingRef() : 0;
            gait.stride = ref > 0 ? ref * currentAnimation.animation().length() : 0;
        }
    }

    /**
     * Runs once this controller's clip pose is on the bones (see the class comment): blends the pose held when the troop stopped (or set off, with {@link #START_BLEND})
     * into it, then keeps what the layer now shows for the next blend to start from. Every bone it has ever keyed is kept, not only those of the clip playing: a bone
     * the idle clip leaves unkeyed would otherwise keep the stale mid-stride value of the moving clip, a pose the troop no longer holds.
     */
    void after(Map<String, CoreGeoBone> bones, double seekTime) {
        if (from != null && seekTime < fromAt) from = null;   // the animation time went back (the troop was tracked anew): nothing to finish
        if (from != null) {
            double k = fromLength <= 0 ? 1 : Math.min(1, (seekTime - fromAt) / fromLength);
            blendFrom(bones, fromEased ? AttackController.smooth(k) : k, k >= 1);
        }
        for (String name : keyed) {
            CoreGeoBone bone = bones.get(name);
            if (bone == null) continue;
            float[] a = wrote.computeIfAbsent(name, n -> new float[6]);
            a[0] = bone.getRotX(); a[1] = bone.getRotY(); a[2] = bone.getRotZ();
            a[3] = bone.getPosX(); a[4] = bone.getPosY(); a[5] = bone.getPosZ();
        }
    }

    /** Keeps the legs as drawn this frame, for the next set-off (see {@link #shown}); called by {@link Shown} once every layer's pose is on the bones. */
    void shown(Map<String, CoreGeoBone> bones) {
        if (legBones == null) legBones = bones.keySet().stream().filter(n -> legWeight(n) > 0).toArray(String[]::new);
        for (String name : legBones) {
            CoreGeoBone bone = bones.get(name);
            if (bone == null) continue;
            float[] a = shown.computeIfAbsent(name, n -> new float[3]);
            a[0] = bone.getRotX(); a[1] = bone.getRotY(); a[2] = bone.getRotZ();
        }
    }

    /**
     * The stage behind every layer, registered last: by the time it runs the gait, stance, attack, hit and death poses are all on the bones, so the legs it reads are
     * this troop's as drawn this frame ({@link GaitController#shown}).
     */
    static final class Shown extends AnimationController<GeoTroop> {
        private final GaitController legs;

        Shown(GeoTroop troop, GaitController legs) {
            super(troop, "gait_shown", 0, s -> software.bernie.geckolib.core.object.PlayState.STOP);
            this.legs = legs;
        }

        @Override
        public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                            Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
            legs.shown(bones);
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
     * troop's. The stance, attack, hit and death layers behind this all read a bone before they write it, so on such a bone they built on that leftover: a hit clip's
     * offset was added onto last frame's offset, every frame (a pauldron that spun through a blow), and a pose faded in and out from itself. Started from rest, each
     * layer's result depends only on this frame. (The stop blend reads only bones the gait clips key; it would build on a leftover only on a model whose gait clips key
     * different bones, and none does.) Setting a bone marks it changed, so GeckoLib's own end-of-tick reset (to this same pose: these bones' snapshots never leave rest) no
     * longer runs for them. The head keeps the mark the renderer's look sets after the tick, so its rotation is left to the upper gait clips, which all key it.
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

    /** A spider's upper leg bone (wm_stalker: left_leg_1..4, right_leg_1..4); its lower one adds {@code _low}. */
    private static final Pattern SPIDER_LEG = Pattern.compile("(left|right)_leg_\\d+"), SPIDER_LEG_LOW = Pattern.compile("(left|right)_leg_\\d+_low");

    /**
     * How much a bone's turn moves the foot, by name: a thigh most, a shin half, a foot little; 0 for every other bone. The names: a biped's or beast's
     * {@code *leg} (1), {@code *shin} (.5), {@code *foot} (.15); a spider's (wm_stalker) eight legs, upper {@code <side>_leg_<i>} (1) and lower
     * {@code <side>_leg_<i>_low} (.5), whole-name matches that no other troop's bone has (checked 2026-11-14 over every geo/troop model).
     */
    static double legWeight(String bone) {
        if (SPIDER_LEG.matcher(bone).matches()) return 1;
        if (SPIDER_LEG_LOW.matcher(bone).matches()) return .5;
        return bone.endsWith("leg") ? 1 : bone.endsWith("shin") ? .5 : bone.endsWith("foot") ? .15 : 0;
    }

    /**
     * The phase (0..1) of the gait clip {@code clip} whose leg rotations lie nearest {@code pose} (each bone's rotation xyz first), so a troop that sets off steps out of
     * the stance it held without its planted foot jumping. 0 when the pose holds none of the clip's leg bones. The pose given is the troop's own legs as last drawn
     * ({@link #shown}): a standing stance (all but the stun and fall-back legs) lerps the legs toward its own pose and lets go over a few ticks once the troop moves, so the stride that slides the feet least through
     * that is the one nearest the drawn legs, not the idle legs under the stance. Until 2026-11-14 it was what the bones held when {@link #process} began, the last draw
     * of the model: a lone troop's own drawn legs, but in a battle another troop's.
     */
    static double matchPhase(Animation clip, Map<String, CoreGeoBone> bones, Map<String, float[]> pose) {
        final int steps = 48;
        double best = Double.MAX_VALUE, at = 0;
        for (int i = 0; i < steps; i++) {
            double t = clip.length() * i / steps, cost = 0;
            for (BoneAnimation b : clip.boneAnimations()) {
                double w = legWeight(b.boneName());
                float[] now = pose.get(b.boneName());
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
