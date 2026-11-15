package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.keyframe.BoneAnimation;
import software.bernie.geckolib.animation.state.BoneSnapshot;
import software.bernie.geckolib.loading.object.BakedAnimations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * <b>start-own-legs.</b> A troop that sets off from a standstill starts its stride on the phase whose legs lie nearest the legs it was last drawn with
 * (GaitController.matchPhase). Those must be its own, kept by the controller (GaitController.Shown), not what the last draw of the model left on the bones. So the leg
 * bones the walk clip keys are not left on the impossible value but on a real pose, the walk clip half a stride away from the phase the idle pose matches ({@link Legs}),
 * and this keeps each troop's legs as it was last drawn. A troop that sets off must start on the phase its own last drawn legs match; one that read the bones starts
 * on the leftover's phase instead.
 * <p>
 * A suite that puts its own probe in (GeoGaps) calls {@link #tap} from it, so its draws are read too.
 */
final class GeoLayerRest {
    static final float ROT = 64, POS = 10000;
    /** A bone this far from rest is carrying a leftover: no clip turns a bone two full turns or moves it 100 px. A pose blended over the leftover at up to 99% still shows. */
    static final float ROT_FAR = 12, POS_FAR = 100;
    /** matchPhase tries 48 phases; two phases nearer than half a step are the same choice. */
    static final double SAME = .5 / 48;

    private static final Map<BakedGeoModel, List<GeoBone>> BONES = new IdentityHashMap<>();
    private static final Map<BakedGeoModel, Legs> LEGS = new IdentityHashMap<>();
    private static final Map<BakedGeoModel, Boolean> LEFT = new IdentityHashMap<>();
    /** Each troop's gait mode and legs at its last animated draw, to see it set off and to know what it set off from. */
    private static final Map<Entity, Drawn> LAST = new WeakHashMap<>();
    private static GeoTroopRenderer.Probe before, mine;
    private static boolean on;
    /** Animated draws read, and draws that were not animated (see the class comment). */
    static int draws, skipped;
    /** Each bone found carrying a leftover, with where the check was when it first did. */
    static final Map<String, String> leaks = new LinkedHashMap<>();
    /**
     * Set-offs judged; those of them whose own legs and the leftover match different phases, so reading the bones would have shown; those that started on the leftover's
     * phase; those made with a stance holding the legs, where the drawn legs are not the gait layer's; and set-offs on a model with no clip for that gait or no leg
     * bones the walk keys, not judged.
     */
    static int starts, telling, onLeftover, stanced, unjudged;
    /** Each set-off that did not start on the phase its own last drawn legs match. */
    static final List<String> strayed = new ArrayList<>();
    /** The phases of each model that had a set-off, for the report. */
    static final Map<String, String> legsNotes = new LinkedHashMap<>();
    /** What the check is doing now, for a leak's report. */
    static volatile String where = "";

    /** Starts reading every Geo draw, handing each on to a probe already there. */
    static void start() {
        if (on) return;
        on = true; draws = 0; skipped = 0; leaks.clear(); LEFT.clear(); LAST.clear(); BONES.clear(); LEGS.clear();
        starts = 0; telling = 0; onLeftover = 0; stanced = 0; unjudged = 0; strayed.clear(); legsNotes.clear();
        GeoTroopRenderer.Probe prev = before = GeoTroopRenderer.PROBE;
        GeoTroopRenderer.PROBE = mine = (e, model, td) -> {
            if (prev != null) prev.seen(e, model, td);
            tap(e, model);
        };
    }

    /**
     * Reads one draw, when the check is on. For a probe that replaced this one. It must be handed every Geo draw from then on: a set-off is told by the troop's mode
     * at its last animated draw, and a draw this missed would leave that out of date.
     */
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

    /** Whether every set-off started on its own legs' phase, with at least {@code least} of them able to show the fault. */
    static boolean ownLegs(int least) { return telling >= least && strayed.isEmpty(); }

    static String startResult() {
        String base = starts + " set-offs from a standstill, each with another draw's walk pose left on the leg bones " + legsNotes.values()
            + (unjudged > 0 ? ", " + unjudged + " more on a model with no clip for that gait" : "");
        if (starts == 0) return base + ": none to judge";
        String tell = telling + " of them could tell the two apart, " + onLeftover + " started on the leftover's phase, " + stanced + " set off with a stance on the legs";
        if (strayed.isEmpty()) return base + ": each started on the phase its own last drawn legs match (" + tell + ")";
        return base + ": " + strayed.size() + " did not start on the phase their own last drawn legs match (" + tell + ") " + strayed.subList(0, Math.min(8, strayed.size()));
    }

    /** A troop at its last animated draw: its stride clock and gait mode, and its leg bones' rotations as drawn (by bone name). */
    private static final class Drawn {
        Gait gait;
        Gait.Mode mode;
        final Map<String, float[]> legs = new HashMap<>();
    }

    /**
     * A model's legs. {@code bones} are the leg bones the walk clip keys, and {@code left} what is left on them after each draw: the walk clip's pose half a stride from
     * the phase the idle pose matches. {@code all} is every leg bone of the model (GaitController.legWeight), the bones a troop's drawn legs are kept from, as
     * GaitController.shown keeps them. {@code walkLeft} and {@code runLeft} are the phases a set-off reading the leftover takes.
     */
    private static final class Legs {
        final List<GeoBone> bones = new ArrayList<>();
        final List<float[]> left = new ArrayList<>();
        final List<GeoBone> all = new ArrayList<>();
        final Map<String, GeoBone> byName = new HashMap<>();
        Animation walk, run;
        double walkLeft = -1, runLeft = -1;
        String note = "";
    }

    private static double circ(double a, double b) { double d = Math.abs(a - b) % 1; return Math.min(d, 1 - d); }

    private static Legs legs(BakedGeoModel model) {
        return LEGS.computeIfAbsent(model, m -> {
            Legs l = new Legs();
            List<GeoBone> todo = new ArrayList<>(m.topLevelBones());
            while (!todo.isEmpty()) {
                GeoBone b = todo.remove(todo.size() - 1);
                l.byName.put(b.getName(), b);
                if (GaitController.legWeight(b.getName()) > 0) l.all.add(b);
                todo.addAll(b.getChildBones());
            }
            Identifier id = null;
            for (Map.Entry<Identifier, BakedGeoModel> en : GeckoLibCache.getBakedModels().entrySet()) if (en.getValue() == m) id = en.getKey();
            if (id == null) return l;
            BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(Identifier.of(id.getNamespace(), id.getPath().replace("geo/", "animations/").replace(".geo.json", ".animation.json")));
            l.walk = baked == null ? null : baked.getAnimation("walk_legs");
            if (l.walk == null) return l;
            l.run = baked.getAnimation("run_legs");
            Animation idle = baked.getAnimation("idle_legs");
            // the legs a standing troop's gait layer holds: the idle clip's first frame on the leg bones the walk keys (rest where the idle keys none)
            Map<String, float[]> idlePose = new HashMap<>();
            for (BoneAnimation b : l.walk.boneAnimations()) {
                GeoBone bone = l.byName.get(b.boneName());
                if (GaitController.legWeight(b.boneName()) == 0 || bone == null || b.rotationKeyFrames().xKeyframes().isEmpty()) continue;
                idlePose.put(b.boneName(), pose(bone, idle, b.boneName(), 0));
            }
            if (idlePose.isEmpty()) return l;
            double walkIdle = GaitController.matchPhase(l.walk, l.byName, idlePose);
            double q = Gait.wrap(walkIdle + .5);
            Map<String, float[]> leftPose = new HashMap<>();
            for (String name : idlePose.keySet()) {
                float[] p = pose(l.byName.get(name), l.walk, name, q * l.walk.length());
                leftPose.put(name, p);
                l.bones.add((GeoBone) l.byName.get(name)); l.left.add(p);
            }
            l.walkLeft = GaitController.matchPhase(l.walk, l.byName, leftPose);
            if (l.run != null) l.runLeft = GaitController.matchPhase(l.run, l.byName, leftPose);
            l.note = String.format(Locale.ROOT, "%s: the leftover matches walk phase %.3f and run %.3f, the idle pose walk %.3f", id.getPath().replaceAll(".*/|\\.geo\\.json", ""), l.walkLeft, l.runLeft, walkIdle);
            return l;
        });
    }

    /** Rest plus the clip's rotation on {@code name} at {@code t} (rest alone when the clip is missing or does not key it). */
    private static float[] pose(GeoBone bone, Animation clip, String name, double t) {
        BoneSnapshot init = bone.getInitialSnapshot();
        float[] p = {init.getRotX(), init.getRotY(), init.getRotZ()};
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
        boolean animated = true;   // the first draw of a model has no leftover on it to tell by: taken as animated
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
        }
        if (animated) setOff(e, model);
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
     * One animated draw of a troop, read before the bones are left off rest. If the troop was standing at its last animated draw and moves now, it set off in this
     * draw: the stride clock still holds the phase matchPhase chose (the next frame walks it on), and that must be the phase the troop's own legs, as last drawn,
     * match. Then keeps the legs as drawn now.
     */
    private static void setOff(Entity e, BakedGeoModel model) {
        Gait g = Gait.LIVE.get(e);
        if (g == null) return;
        Legs l = legs(model);
        Drawn last = LAST.get(e);
        // the same clock as at that draw: a troop given a new one (a resource reload) starts over, it does not set off
        if (last != null && last.gait == g && last.mode == Gait.Mode.IDLE && g.mode != Gait.Mode.IDLE) {
            boolean run = g.mode == Gait.Mode.RUN;
            Animation clip = run ? l.run : l.walk;
            if (clip == null || l.bones.isEmpty()) unjudged++;
            else {
                double own = GaitController.matchPhase(clip, l.byName, last.legs), left = run ? l.runLeft : l.walkLeft;
                legsNotes.putIfAbsent(l.note, l.note);
                starts++;
                if (circ(own, left) >= SAME) telling++;
                if (circ(g.phase, left) < SAME) onLeftover++;
                StanceController stance = StanceController.LIVE.get(e);
                if (stance != null && stance.shownLegsWeight > 0) stanced++;
                if (circ(g.phase, own) >= SAME) strayed.add(String.format(Locale.ROOT, "%d %s phase %.3f, own legs' %.3f, leftover's %.3f (%s)", e.getId(), run ? "run" : "walk", g.phase, own, left, where));
            }
        }
        if (last == null) LAST.put(e, last = new Drawn());
        last.gait = g; last.mode = g.mode;
        for (GeoBone b : l.all) {
            float[] a = last.legs.computeIfAbsent(b.getName(), n -> new float[3]);
            a[0] = b.getRotX(); a[1] = b.getRotY(); a[2] = b.getRotZ();
        }
    }
}
