package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoModel;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The death layer (unit animation rebuild, docs/anim-rebuild/PLAN.md, "Death"). Registered last, so while a troop is dead it is the
 * only writer that counts: it lerps every bone toward the fall the killing blow called for and, within {@link #FADE_IN} ticks of
 * death, has fully replaced whatever the gait, stance, swing and flinch layers wrote.
 * <p>
 * The fall is picked once, on the first dead frame, from the lethal blow's bearing (MotionEvents hit_v1; else the newest blow, else
 * the entity id so deaths with no blow vary): a blow from the front throws it on its back, from behind it pitches face down, from
 * its right it falls on its left side and from its left on its right. The clip plays on deathTime, lands by
 * BattleMath.DEATH_FALL_TICKS and holds its last frame while the corpse lies and fades (BodyCache). Nothing here feeds back.
 */
final class DeathController extends AnimationController<GeoTroop> {
    /** Blend from the living pose into the fall over this many ticks of deathTime. */
    static final double FADE_IN = 2;
    static final String[] CLIPS = {"death_back", "death_forward", "death_left", "death_right"};

    /** The fall this troop latched, or null while alive. */
    private String latched;
    /** What the last frame showed, for the in-game checks (GeoKnightChecks): clip, clip time and weight. Never read in play. */
    String shownClip = "";
    double shownT = -1, shownWeight;
    private boolean listed;
    static final Map<Entity, DeathController> LIVE = Collections.synchronizedMap(new WeakHashMap<>());

    DeathController(GeoTroop troop) {
        super(troop, "death", 0, s -> PlayState.STOP);
    }

    /** The fall for a blow from {@code bearing} degrees (0 from the front, + from the troop's right). */
    static String clipFor(double bearing) {
        double b = ((bearing % 360) + 540) % 360 - 180;
        if (Math.abs(b) <= 45) return "death_back";
        if (Math.abs(b) >= 135) return "death_forward";
        return b > 0 ? "death_left" : "death_right";
    }

    /** The lethal blow's fall, else the newest blow's, else one picked by the entity id. */
    static String pick(Iterable<MotionInput.Hit> hits, int id) {
        MotionInput.Hit lethal = null, newest = null;
        for (MotionInput.Hit h : hits) {
            if (h.lethal() && (lethal == null || h.tick() >= lethal.tick())) lethal = h;
            if (newest == null || h.tick() >= newest.tick()) newest = h;
        }
        MotionInput.Hit h = lethal != null ? lethal : newest;
        return h != null ? clipFor(h.bearing()) : CLIPS[Math.floorMod(id * 31 + 7, CLIPS.length)];
    }

    @Override
    public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Entity e = state.getData(DataTickets.ENTITY);
        if (e == null) return;
        if (!listed) { listed = true; LIVE.put(e, this); }
        shownWeight = 0; shownT = -1; shownClip = "";
        if (!(e instanceof LivingEntity l) || !(l.isDead() || l.deathTime > 0)) { latched = null; return; }
        if (latched == null) latched = pick(MotionEvents.Client.hits(e.getId()), e.getId());
        Animation clip = model.getAnimation(animatable, latched);
        if (clip == null) return;
        double dt = l.deathTime + state.getPartialTick();
        double t = Math.min(dt, clip.length());
        double w = AttackController.smooth(dt / FADE_IN);
        if (w > 0) AttackController.apply(clip, t, bones, (float) w);
        shownClip = clip.name(); shownT = t; shownWeight = w;
    }
}
