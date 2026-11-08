package gg.gravesandglory;

import net.minecraft.entity.Entity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Map;

/**
 * A GeckoLib controller whose walk and run loops are driven by ground distance, not time (see {@link Gait}). Every other clip
 * (idle, transitions, later attacks) keeps GeckoLib's normal clock.
 * <p>
 * GeckoLib transitions always blend into a clip's first frame. The {@code owner} (the legs layer) therefore restarts the shared
 * phase at 0 the moment its transition into a gait clip ends, so the blend lands exactly where the stride clock starts and nothing
 * snaps; the arms layer, registered after it, reads the same phase.
 */
final class GaitController extends AnimationController<GeoTroop> {
    /** Reference speed of each gait clip (blocks per tick) as built by art/anim/knight/build_anims.py; the clip name is its key prefix. */
    static double refSpeed(String clip) {
        return clip.startsWith("walk_") ? .16 : clip.startsWith("run_") ? .26 : 0;
    }

    final Gait gait;
    final boolean owner;
    private final int blend;
    private Gait.Mode lastMode = Gait.Mode.IDLE;
    /** A walk-to-run (or back) switch: no timed blend, and the stride clock keeps its phase, so the feet stay on the stride clock instead of skating through the blend. */
    private boolean keepPhase;

    GaitController(GeoTroop troop, String name, int transition, Gait gait, boolean owner, AnimationStateHandler<GeoTroop> handler) {
        super(troop, name, transition, handler);
        this.gait = gait;
        this.owner = owner;
        this.blend = transition;
    }

    /** The playing clip's reference speed, or 0 when no gait clip is running. */
    double playingRef() {
        return currentAnimation == null ? 0 : refSpeed(currentAnimation.animation().name());
    }

    @Override
    public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Entity e = state.getData(DataTickets.ENTITY);
        if (e != null) gait.update(e, seekTime);
        Gait.Mode m = gait.mode;
        if (m != lastMode && m != Gait.Mode.IDLE && lastMode != Gait.Mode.IDLE) { transitionLength = 0; keepPhase = true; }
        else if (!keepPhase) transitionLength = blend;
        lastMode = m;
        super.process(model, state, bones, snapshots, seekTime, crashWhenCantFindBone);
        if (keepPhase && !shouldResetTick && getAnimationState() == State.RUNNING) keepPhase = false;
        if (owner) {
            double ref = getAnimationState() == State.RUNNING ? playingRef() : 0;
            gait.stride = ref > 0 ? ref * currentAnimation.animation().length() : 0;
        }
    }

    @Override
    protected double adjustTick(double tick) {
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
