package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.keyframe.BoneAnimation;
import software.bernie.geckolib.core.state.BoneSnapshot;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Test harness only: proves a Geo troop's drawn pose is built from this frame and this troop alone (the in-game rows &lt;suite&gt;-layer-rest and &lt;suite&gt;-start-own-legs).
 * <p>
 * The bones are the model's, shared by every troop drawn with it, and GeckoLib resets only a bone nothing touched. The stance, attack, hit and death layers read a bone
 * before they write it, so on a bone the playing gait clips do not key they built on whatever the last draw left there: a hit clip's offset was added onto last frame's
 * offset, every frame (a pauldron or a cape that spun through a blow), and a pose faded in and out from itself. GaitController.rest puts those bones at rest first.
 * <p>
 * <b>layer-rest.</b> After every Geo draw this leaves on each bone a value no pose reaches ({@link #ROT} radians and {@link #POS} pixels past rest) with its changed marks
 * cleared, which is what an earlier draw's leftover is to the next one, only unmistakable. The next draw of that model must carry none of it: a bone that still does was
 * built on, not written. A draw in which every bone still carries it was not animated at all (GeckoLib skips the second draw of one troop in one frame) and is not counted.
 * <p>
 * <b>start-own-legs.</b> A troop that sets off from a standstill starts its stride on the phase whose legs lie nearest the legs it was holding
 * (GaitController.matchPhase). The legs it reads must be its own, not what the last draw of the model left on the bones. So the leg bones the walk clip keys are not
 * left on the impossible value but on a real pose: the walk clip half a stride away from the phase the idle pose matches ({@link Legs}). A set-off that takes the phase
 * matching that leftover read the bones, not its own legs.
 * <p>
 * A suite that puts its own probe in (GeoGaps) calls {@link #tap} from it, so its draws are read too.
 */
final class GeoLayerRest {
    static final float ROT = 64, POS = 10000;
    /** A bone this far from rest is carrying a leftover: no clip turns a bone two full turns or moves it 100 px. A pose blended over the leftover at up to 99% still shows. */
    static final float ROT_FAR = 12, POS_FAR = 100;
    /** matchPhase tries this many phases; two phases nearer than {@link #SAME} of a loop are the same choice, and a model is judged only when the leftover's phase and the idle pose's are at least {@link #APART} apart. */
    static final double STEP = 1.0 / 48, SAME = .5 * STEP, APART = 6 * STEP;

    private static final Map<BakedGeoModel, List<GeoBone>> BONES = new IdentityHashMap<>();
    private static final Map<BakedGeoModel, Legs> LEGS = new IdentityHashMap<>();
    private static final Map<BakedGeoModel, Boolean> LEFT = new IdentityHashMap<>();
    /** Each troop's gait mode and world time at its last draw, to see it set off. */
    private static final Map<Entity, Object[]> LAST = new WeakHashMap<>();
    private static GeoTroopRenderer.Probe before, mine;
    private static boolean on;
    /** Animated draws read, and draws that were not animated (see the class comment). */
    static int draws, skipped;
    /** Each bone found carrying a leftover, with where the check was when it first did. */
    static final Map<String, String> leaks = new LinkedHashMap<>();
    /** Set-offs judged, set-offs on a model that cannot be judged, and each set-off that took the leftover's phase. */
    static int starts, unjudged;
    static final List<String> matched = new ArrayList<>();
    /** The furthest a judged set-off's phase lay from the phase the model's idle pose matches, in loops; and each model's phases that had a set-off, for the report. */
    static double ownWorst;
    static final Map<String, String> legsNotes = new LinkedHashMap<>();
    /** What the check is doing now, for a leak's report. */
    static volatile String where = "";

    /** Starts reading every Geo draw, handing each on to a probe already there. */
    static void start() {
        if (on) return;
        on = true; draws = 0; skipped = 0; leaks.clear(); LEFT.clear(); LAST.clear();
        starts = 0; unjudged = 0; matched.clear(); ownWorst = 0; legsNotes.clear();
        GeoTroopRenderer.Probe prev = before = GeoTroopRenderer.PROBE;
        GeoTroopRenderer.PROBE = mine = (e, model, td) -> {
            if (prev != null) prev.seen(e, model, td);
            tap(e, model);
        };
    }

    /** Reads one draw, when the check is on. For a probe that replaced this one. */
    static void tap(Entity e, BakedGeoModel model) {
        if (!on) return;
        try { seen(e, model); } catch (RuntimeException ex) { ex.printStackTrace(); }
    }

    /** Stops reading and puts every bone back at rest. */
    static void stop() {
        if (!on) return;
        on = false;
        if (GeoTroopRenderer.PROBE == mine) GeoTroopRenderer.PROBE = before;   // a suite that replaced the probe has already taken it out
        before = null; mine = null;
        for (BakedGeoModel model : LEFT.keySet()) {
            List<GeoBone> all = new ArrayList<>(bones(model));
            all.addAll(legs(model).bones);
            for (GeoBone b : all) {
                BoneSnapshot init = b.getInitialSnapshot();
                b.setRotX(init.getRotX()); b.setRotY(init.getRotY()); b.setRotZ(init.getRotZ());
                b.setPosX(init.getOffsetX()); b.setPosY(init.getOffsetY()); b.setPosZ(init.getOffsetZ());
                b.resetStateChanges();
            }
        }
        LEFT.clear(); LAST.clear();
    }

    static boolean clean() { return draws > 0 && leaks.isEmpty(); }

    static String result() {
        String base = draws + " animated Geo draws, each begun with the bones " + (int) ROT + " rad and " + (int) POS + " px off rest as an earlier draw's leftover (" + skipped + " redraws not animated, not counted)";
        if (draws == 0) return base + ": nothing was drawn";
        if (leaks.isEmpty()) return base + ": no drawn bone carried any of it";
        List<String> first = new ArrayList<>();
        for (Map.Entry<String, String> l : leaks.entrySet()) { if (first.size() == 12) break; first.add(l.getKey() + " (" + l.getValue() + ")"); }
        return base + ": " + leaks.size() + " bones were drawn built on it " + first;
    }

    /** Whether at least {@code least} set-offs were judged and none took the leftover's phase. */
    static boolean ownLegs(int least) { return starts >= least && matched.isEmpty(); }

    static String startResult() {
        String base = starts + " troops set off from a standstill with another draw's legs left on the bones " + legsNotes.values()
            + (unjudged > 0 ? ", " + unjudged + " more on a model whose leftover cannot be told from its idle pose" : "");
        if (starts == 0) return base + ": none to judge";
        if (matched.isEmpty()) return base + String.format(java.util.Locale.ROOT, ": none started on the leftover's phase; furthest from the idle pose's phase %.3f of a loop", ownWorst);
        return base + ": " + matched.size() + " started on the leftover's phase " + matched.subList(0, Math.min(8, matched.size()));
    }

    /**
     * A model's leg bones as the walk clip keys them, and what is left on them after each draw: the walk clip's pose half a stride from the phase the idle pose matches.
     * {@code walkLeft} and {@code runLeft} are the phases a set-off reading that leftover takes, {@code walkOwn} and {@code runOwn} those the idle pose itself matches; a
     * gait is judged only when the two lie well apart.
     */
    private static final class Legs {
        final List<GeoBone> bones = new ArrayList<>();
        final List<float[]> left = new ArrayList<>();
        double walkLeft = -1, runLeft = -1, walkOwn = -1, runOwn = -1;
        boolean walk, run;
        String note = "";
    }

    private static double circ(double a, double b) { double d = Math.abs(a - b) % 1; return Math.min(d, 1 - d); }

    private static Legs legs(BakedGeoModel model) {
        return LEGS.computeIfAbsent(model, m -> {
            Legs l = new Legs();
            Identifier id = null;
            for (Map.Entry<Identifier, BakedGeoModel> en : GeckoLibCache.getBakedModels().entrySet()) if (en.getValue() == m) id = en.getKey();
            if (id == null) return l;
            BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(new Identifier(id.getNamespace(), id.getPath().replace("geo/", "animations/").replace(".geo.json", ".animation.json")));
            Animation walk = baked == null ? null : baked.getAnimation("walk_legs");
            if (walk == null) return l;
            Animation run = baked.getAnimation("run_legs"), idle = baked.getAnimation("idle_legs");
            Map<String, CoreGeoBone> byName = new HashMap<>();
            List<GeoBone> todo = new ArrayList<>(m.topLevelBones());
            while (!todo.isEmpty()) { GeoBone b = todo.remove(todo.size() - 1); byName.put(b.getName(), b); todo.addAll(b.getChildBones()); }
            // the pose a standing troop's gait layer holds: the idle clip's first frame on the leg bones (rest where it keys none)
            Map<String, float[]> idlePose = new HashMap<>();
            for (BoneAnimation b : walk.boneAnimations()) {
                CoreGeoBone bone = byName.get(b.boneName());
                if (GaitController.legWeight(b.boneName()) == 0 || bone == null || b.rotationKeyFrames().xKeyframes().isEmpty()) continue;
                idlePose.put(b.boneName(), pose(bone, idle, b.boneName(), 0));
            }
            if (idlePose.isEmpty()) return l;
            l.walkOwn = GaitController.matchPhase(walk, byName, idlePose);
            double q = Gait.wrap(l.walkOwn + .5);
            Map<String, float[]> leftPose = new HashMap<>();
            for (String name : idlePose.keySet()) {
                float[] p = pose(byName.get(name), walk, name, q * walk.length());
                leftPose.put(name, p);
                l.bones.add((GeoBone) byName.get(name)); l.left.add(p);
            }
            l.walkLeft = GaitController.matchPhase(walk, byName, leftPose);
            l.walk = circ(l.walkLeft, l.walkOwn) >= APART;
            if (run != null) {
                l.runOwn = GaitController.matchPhase(run, byName, idlePose);
                l.runLeft = GaitController.matchPhase(run, byName, leftPose);
                l.run = circ(l.runLeft, l.runOwn) >= APART;
            }
            l.note = String.format(java.util.Locale.ROOT, "%s: walk phase %.3f, idle pose's %.3f; run %.3f, idle pose's %.3f", id.getPath().replaceAll(".*/|\\.geo\\.json", ""), l.walkLeft, l.walkOwn, l.runLeft, l.runOwn);
            return l;
        });
    }

    /** Rest plus the clip's rotation on {@code name} at {@code t} (rest alone when the clip is missing or does not key it); rotation xyz, then the rest position. */
    private static float[] pose(CoreGeoBone bone, Animation clip, String name, double t) {
        BoneSnapshot init = bone.getInitialSnapshot();
        float[] p = {init.getRotX(), init.getRotY(), init.getRotZ(), init.getOffsetX(), init.getOffsetY(), init.getOffsetZ()};
        if (clip != null) for (BoneAnimation b : clip.boneAnimations()) {
            if (!b.boneName().equals(name) || b.rotationKeyFrames().xKeyframes().isEmpty()) continue;
            p[0] += AttackController.sample(b.rotationKeyFrames().xKeyframes(), t);
            p[1] += AttackController.sample(b.rotationKeyFrames().yKeyframes(), t);
            p[2] += AttackController.sample(b.rotationKeyFrames().zKeyframes(), t);
        }
        return p;
    }

    /** Every bone but the legs the walk clip keys ({@link Legs}), which carry a pose instead of the impossible value. */
    private static List<GeoBone> bones(BakedGeoModel model) {
        return BONES.computeIfAbsent(model, m -> {
            List<GeoBone> legs = legs(m).bones;
            List<GeoBone> all = new ArrayList<>();
            List<GeoBone> todo = new ArrayList<>(m.topLevelBones());
            while (!todo.isEmpty()) {
                GeoBone b = todo.remove(todo.size() - 1);
                if (!legs.contains(b)) all.add(b);
                todo.addAll(b.getChildBones());
            }
            return all;
        });
    }

    private static boolean far(float v, float rest, float limit) { return Math.abs(v - rest) > limit; }

    /** One draw: reads what it carried over and whether the troop set off on its own legs, then leaves the bones off rest for the next. */
    private static void seen(Entity e, BakedGeoModel model) {
        List<GeoBone> bones = bones(model);
        boolean animated = true;
        if (LEFT.containsKey(model)) {
            List<GeoBone> carried = new ArrayList<>();
            for (GeoBone b : bones) {
                BoneSnapshot init = b.getInitialSnapshot();
                if (far(b.getRotX(), init.getRotX(), ROT_FAR) || far(b.getRotY(), init.getRotY(), ROT_FAR) || far(b.getRotZ(), init.getRotZ(), ROT_FAR)
                    || far(b.getPosX(), init.getOffsetX(), POS_FAR) || far(b.getPosY(), init.getOffsetY(), POS_FAR) || far(b.getPosZ(), init.getOffsetZ(), POS_FAR)) carried.add(b);
            }
            if (carried.size() == bones.size()) { skipped++; animated = false; }
            else {
                draws++;
                for (GeoBone b : carried) leaks.putIfAbsent(b.getName(), where);
            }
            if (animated) setOff(e, model);
        }
        for (GeoBone b : bones) {
            BoneSnapshot init = b.getInitialSnapshot();
            b.setRotX(init.getRotX() + ROT); b.setRotY(init.getRotY() + ROT); b.setRotZ(init.getRotZ() + ROT);
            b.setPosX(init.getOffsetX() + POS); b.setPosY(init.getOffsetY() + POS); b.setPosZ(init.getOffsetZ() + POS);
            b.resetStateChanges();
        }
        Legs l = legs(model);
        for (int i = 0; i < l.bones.size(); i++) {
            GeoBone b = l.bones.get(i);
            float[] p = l.left.get(i);
            b.setRotX(p[0]); b.setRotY(p[1]); b.setRotZ(p[2]);
            b.resetStateChanges();
        }
        LEFT.put(model, Boolean.TRUE);
    }

    /**
     * Whether this draw is the troop setting off (idle at its last draw, a tick or two ago, and moving now), and if so whether its stride began on the leftover's phase.
     * The gait clock holds the phase matchPhase chose until the next frame walks it on.
     */
    private static void setOff(Entity e, BakedGeoModel model) {
        Gait g = Gait.LIVE.get(e);
        if (g == null || e.getWorld() == null) return;
        long now = e.getWorld().getTime();
        Object[] last = LAST.put(e, new Object[]{g.mode, now});
        if (last == null || last[0] != Gait.Mode.IDLE || g.mode == Gait.Mode.IDLE || now - (Long) last[1] > 3) return;
        Legs l = legs(model);
        legsNotes.putIfAbsent(l.note, l.note);
        boolean run = g.mode == Gait.Mode.RUN;
        if (!(run ? l.run : l.walk)) { unjudged++; return; }
        starts++;
        if (circ(g.phase, run ? l.runLeft : l.walkLeft) < SAME) matched.add(String.format(java.util.Locale.ROOT, "%d %s phase %.3f (%s)", e.getId(), run ? "run" : "walk", g.phase, where));
        ownWorst = Math.max(ownWorst, circ(g.phase, run ? l.runOwn : l.walkOwn));
    }
}
