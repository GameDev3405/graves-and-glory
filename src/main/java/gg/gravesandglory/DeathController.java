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
 * BattleMath.DEATH_FALL_TICKS and holds its last frame while the corpse lies and fades (BodyCache). Where the troop has them, a
 * gear variant of the fall ({@link #drop}: keep the gear, drop the weapon, drop both) is picked from the entity's UUID. Nothing here
 * feeds back.
 * <p>
 * A troop that rose instead of dying (rise_v1: Rise Again, the Hollow King's Undying King) and whose model has {@link #FAKE} and {@link #RISE}
 * plays them here too, alive, over every other layer: it falls (fake_death, held on its last frame) and stands back up (rise), timed by
 * {@link #riseAt} so the rise ends on the tick the server's window ends; the stance layer shows no stun meanwhile. Killed for good during it
 * (blows still land), it does not snap to a fall from the guard: still falling, the fall plays on and holds; risen off the ground, the pose
 * it died in sinks back into the fall's last frame (face down) over {@link #COLLAPSE} ticks; back on its feet (the rise's last
 * {@link #RISE_STOOD} ticks, the guard), it falls as any troop does.
 */
final class DeathController extends AnimationController<GeoTroop> {
    /** Blend from the living pose into the fall over this many ticks of deathTime. */
    static final double FADE_IN = 2;
    static final String[] CLIPS = {"death_back", "death_forward", "death_left", "death_right"};
    /** The rise clips (a model has both or the rise plays as before: the stun). */
    static final String FAKE = "fake_death", RISE = "rise";
    /** Ticks the rise fades back into the live layers at its end (its last frame is the ready guard, which the live layers show by then). */
    static final double RISE_OUT = 4;
    /** A troop killed while risen off the ground sinks back face down over this many ticks; in the rise's last RISE_STOOD ticks it stands (the guard) and falls as usual. */
    static final double COLLAPSE = 8, RISE_STOOD = 6;

    /** The fall this troop latched, or null while alive. */
    private String latched;
    /** The rise the last live frame showed ({phase 0 fake_death or 1 rise, clip time, weight}), or null; and, latched at a death during it, that state. */
    private double[] lastRise, riseDeath;
    /** What the last frame showed, for the in-game checks (GeoKnightChecks): clip, clip time and weight; for a rise, the ticks since it fired and its span. Never read in play. */
    String shownClip = "";
    double shownT = -1, shownWeight, shownRiseElapsed = -1, shownRiseSpan;
    /** The entity this controller is listed under in {@link #LIVE}. A troop tracked anew is a new entity object on the same controller (GeckoLib keys them by entity id), so it is listed again; held weakly, as LIVE holds it. */
    private java.lang.ref.WeakReference<Entity> listed;
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

    /** Gear variants of a fall: keep everything, drop the weapon, drop both. Troops without the drop clips play the plain fall. */
    static final String[] DROPS = {"", "_drop1", "_drop2"};

    /** The gear variant this troop dies with: cosmetic, stable per entity. */
    static String drop(java.util.UUID id) {
        return DROPS[(int) Math.floorMod(MotionMath.mix(id.hashCode()), (long) DROPS.length)];
    }

    /**
     * Where a rise is {@code elapsed} ticks after it fired: {0 (fake_death) or 1 (rise), clip time, span}, or null before it or once it is over. The span is
     * the server's window (the ticks it keeps the troop out of the fight); a window of 0 (the troop fights on at once: the Hollow King's row) plays the clips
     * at their own length instead, cosmetic. A span as long as both clips or longer plays the fall, holds its last frame and starts the rise so it ends on
     * the span's last tick; a shorter one plays both clips quickened to fit it.
     */
    static double[] riseAt(double elapsed, int window, double fake, double rise) { return MotionEngine.riseAt(elapsed, window, fake, rise); } // moved unchanged: the old model shares it (TroopMotion.rise)

    /** The rise the troop is in at world time {@code now} ({@link #riseAt}), or null: none heard, none in its span, or its model lacks either clip. */
    static double[] rising(CoreGeoModel<GeoTroop> model, GeoTroop troop, Entity e, double now) {
        MotionEvents.Rise r = MotionEvents.Client.rise(e.getId());
        if (r == null) return null;
        Animation fake = model.getAnimation(troop, FAKE), rise = model.getAnimation(troop, RISE);
        if (fake == null || rise == null) return null;
        return riseAt(now - r.tick(), r.window(), fake.length(), rise.length());
    }

    @Override
    public void process(CoreGeoModel<GeoTroop> model, AnimationState<GeoTroop> state, Map<String, CoreGeoBone> bones,
                        Map<String, BoneSnapshot> snapshots, double seekTime, boolean crashWhenCantFindBone) {
        Entity e = state.getData(DataTickets.ENTITY);
        if (e == null) return;
        if (listed == null || listed.get() != e) { listed = new java.lang.ref.WeakReference<>(e); LIVE.put(e, this); }
        shownWeight = 0; shownT = -1; shownClip = ""; shownRiseElapsed = -1;
        if (!(e instanceof LivingEntity l) || !(l.isDead() || l.deathTime > 0)) {
            latched = null; riseDeath = null; lastRise = null;
            if (e.getWorld() == null) return;
            double now = EndingClient.worldTime(e.getWorld()) + state.getPartialTick();
            double[] at = rising(model, animatable, e, now);
            if (at == null) { lastRise = null; return; }
            Animation clip = model.getAnimation(animatable, at[0] == 0 ? FAKE : RISE);
            double elapsed = now - MotionEvents.Client.rise(e.getId()).tick();
            double w = AttackController.smooth(elapsed / FADE_IN) * AttackController.smooth((at[2] - elapsed) / RISE_OUT);
            if (w > 0) AttackController.apply(clip, at[1], bones, (float) w);
            shownClip = clip.name(); shownT = at[1]; shownWeight = w; shownRiseElapsed = elapsed; shownRiseSpan = at[2];
            lastRise = new double[]{at[0], at[1], w};
            return;
        }
        if (latched == null) {
            String fall = pick(MotionEvents.Client.hits(e.getId()), e.getId()), dropped = fall + drop(e.getUuid());
            latched = model.getAnimation(animatable, dropped) != null ? dropped : fall;
            Animation rise = model.getAnimation(animatable, RISE);
            if (lastRise != null && rise != null && model.getAnimation(animatable, FAKE) != null && (lastRise[0] == 0 || lastRise[1] < rise.length() - RISE_STOOD))
                riseDeath = lastRise;
            lastRise = null;
        }
        if (riseDeath != null) { // killed mid-rise: from the pose it died in, never from the guard
            Animation fake = model.getAnimation(animatable, FAKE), rise = model.getAnimation(animatable, RISE);
            double dt = l.deathTime + state.getPartialTick();
            if (riseDeath[0] == 0) { // still falling (or lying): the fall plays on and holds its last frame
                double t = Math.min(fake.length(), riseDeath[1] + dt), w = Math.max(riseDeath[2], AttackController.smooth(dt / FADE_IN));
                AttackController.apply(fake, t, bones, (float) w);
                shownClip = fake.name(); shownT = t; shownWeight = w;
            } else { // risen off the ground: that pose, sinking back face down
                AttackController.apply(rise, riseDeath[1], bones, (float) riseDeath[2]);
                double k = AttackController.smooth(dt / COLLAPSE);
                AttackController.apply(fake, fake.length(), bones, (float) k);
                shownClip = fake.name(); shownT = fake.length(); shownWeight = k;
            }
            return;
        }
        Animation clip = model.getAnimation(animatable, latched);
        if (clip == null) return;
        double dt = l.deathTime + state.getPartialTick();
        double t = Math.min(dt, clip.length());
        double w = AttackController.smooth(dt / FADE_IN);
        if (w > 0) AttackController.apply(clip, t, bones, (float) w);
        shownClip = clip.name(); shownT = t; shownWeight = w;
    }
}
