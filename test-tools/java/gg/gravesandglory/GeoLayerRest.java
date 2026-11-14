package gg.gravesandglory;

import net.minecraft.entity.Entity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.state.BoneSnapshot;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test harness only: proves a Geo troop's drawn pose is built from this frame alone (the in-game row &lt;suite&gt;-layer-rest).
 * <p>
 * The bones are the model's, shared by every troop drawn with it, and GeckoLib resets only a bone nothing touched. The stance, attack, hit and death layers read a bone
 * before they write it, so on a bone the playing gait clips do not key they built on whatever the last draw left there: a hit clip's offset was added onto last frame's
 * offset, every frame (a pauldron or a cape that spun through a blow), and a pose faded in and out from itself. GaitController.rest puts those bones at rest first.
 * <p>
 * After every Geo draw this leaves on each bone a value no pose reaches ({@link #ROT} radians and {@link #POS} pixels past rest) with its changed marks cleared, which is
 * what an earlier draw's leftover is to the next one, only unmistakable. The next draw of that model must carry none of it: a bone that still does was built on, not
 * written. A draw in which every bone still carries it was not animated at all (GeckoLib skips the second draw of one troop in one frame) and is not counted. The left
 * and right leg, shin and foot bones are left alone: GaitController.matchPhase reads the legs' last pose when a troop sets off. A suite that puts its own probe in
 * (GeoGaps) calls {@link #tap} from it, so its draws are read too.
 */
final class GeoLayerRest {
    static final float ROT = 64, POS = 10000;
    /** A bone this far from rest is carrying a leftover: no clip turns a bone two full turns or moves it 100 px. A pose blended over the leftover at up to 99% still shows. */
    static final float ROT_FAR = 12, POS_FAR = 100;

    private static final Map<BakedGeoModel, List<GeoBone>> BONES = new IdentityHashMap<>();
    private static final Map<BakedGeoModel, Boolean> LEFT = new IdentityHashMap<>();
    private static GeoTroopRenderer.Probe before, mine;
    private static boolean on;
    /** Animated draws read, and draws that were not animated (see the class comment). */
    static int draws, skipped;
    /** Each bone found carrying a leftover, with where the check was when it first did. */
    static final Map<String, String> leaks = new LinkedHashMap<>();
    /** What the check is doing now, for a leak's report. */
    static volatile String where = "";

    /** Starts reading every Geo draw, handing each on to a probe already there. */
    static void start() {
        if (on) return;
        on = true; draws = 0; skipped = 0; leaks.clear(); LEFT.clear();
        GeoTroopRenderer.Probe prev = before = GeoTroopRenderer.PROBE;
        GeoTroopRenderer.PROBE = mine = (e, model, td) -> {
            if (prev != null) prev.seen(e, model, td);
            tap(model);
        };
    }

    /** Reads one draw, when the check is on. For a probe that replaced this one. */
    static void tap(BakedGeoModel model) {
        if (!on) return;
        try { seen(model); } catch (RuntimeException ex) { ex.printStackTrace(); }
    }

    /** Stops reading and puts every bone back at rest. */
    static void stop() {
        if (!on) return;
        on = false;
        if (GeoTroopRenderer.PROBE == mine) GeoTroopRenderer.PROBE = before;   // a suite that replaced the probe has already taken it out
        before = null; mine = null;
        for (BakedGeoModel model : LEFT.keySet()) for (GeoBone b : bones(model)) {
            BoneSnapshot init = b.getInitialSnapshot();
            b.setRotX(init.getRotX()); b.setRotY(init.getRotY()); b.setRotZ(init.getRotZ());
            b.setPosX(init.getOffsetX()); b.setPosY(init.getOffsetY()); b.setPosZ(init.getOffsetZ());
            b.resetStateChanges();
        }
        LEFT.clear();
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

    private static List<GeoBone> bones(BakedGeoModel model) {
        return BONES.computeIfAbsent(model, m -> {
            List<GeoBone> all = new ArrayList<>();
            List<GeoBone> todo = new ArrayList<>(m.topLevelBones());
            while (!todo.isEmpty()) {
                GeoBone b = todo.remove(todo.size() - 1);
                String n = b.getName();
                boolean leg = (n.contains("left_") || n.contains("right_")) && (n.endsWith("leg") || n.endsWith("shin") || n.endsWith("foot"));
                if (!leg) all.add(b);
                todo.addAll(b.getChildBones());
            }
            return all;
        });
    }

    private static boolean far(float v, float rest, float limit) { return Math.abs(v - rest) > limit; }

    /** One draw: reads what it carried over, then leaves the bones off rest for the next. */
    private static void seen(BakedGeoModel model) {
        List<GeoBone> bones = bones(model);
        if (LEFT.containsKey(model)) {
            List<GeoBone> carried = new ArrayList<>();
            for (GeoBone b : bones) {
                BoneSnapshot init = b.getInitialSnapshot();
                if (far(b.getRotX(), init.getRotX(), ROT_FAR) || far(b.getRotY(), init.getRotY(), ROT_FAR) || far(b.getRotZ(), init.getRotZ(), ROT_FAR)
                    || far(b.getPosX(), init.getOffsetX(), POS_FAR) || far(b.getPosY(), init.getOffsetY(), POS_FAR) || far(b.getPosZ(), init.getOffsetZ(), POS_FAR)) carried.add(b);
            }
            if (carried.size() == bones.size()) skipped++;
            else {
                draws++;
                for (GeoBone b : carried) leaks.putIfAbsent(b.getName(), where);
            }
        }
        for (GeoBone b : bones) {
            BoneSnapshot init = b.getInitialSnapshot();
            b.setRotX(init.getRotX() + ROT); b.setRotY(init.getRotY() + ROT); b.setRotZ(init.getRotZ() + ROT);
            b.setPosX(init.getOffsetX() + POS); b.setPosY(init.getOffsetY() + POS); b.setPosZ(init.getOffsetZ() + POS);
            b.resetStateChanges();
        }
        LEFT.put(model, Boolean.TRUE);
    }
}
