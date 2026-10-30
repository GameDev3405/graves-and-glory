package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Direction;
import org.joml.Vector4f;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static gg.gravesandglory.Phase2Checks.report;
import static gg.gravesandglory.Phase2Checks.server;

/**
 * The motion suite (animation rework, GRAVES_PHASE2_CHECKS=motion). Never included in releases.
 * <p>mo-rig (Task 9, the jointed rig): every modelled biped troop at each armour stage it reaches (and as a Veteran when it has
 * trim) shot at rest (the idle clip at 0 s) from 8 angles round it, plus its weapon hand at rest from the front and the weapon
 * side, with the camera placed from the arm bone (which the split leaves alone), so a grip that moved shows as a diff rather than
 * moving the camera with it. The same 8 camera spots with every troop parked away give the empty frames that mark which pixels
 * are troop. With GRAVES_MOTION_BASELINE set to an earlier run's screenshots folder, each rest and hand shot is compared with the
 * same file there: a pixel is changed when any channel differs by more than 8; the check passes when no shot changes more than
 * 0.5% of its frame, and it also logs the share of troop pixels changed (the stricter number). Without a baseline the run only
 * captures. On a rig with forearms and shins, each stage also gets bend shots: both elbows at 90 degrees (the forearms pitched
 * -90) and both knees at 90 (the shins pitched +90, or the troop's own knee limit when it has one), added by the harness after
 * the clip (MotionRigPoseMixin), so the joint caps and cops can be looked at.
 * <p>mo-loader-faceuv (Task 9 fix round 1): a faceuv box built through the loader against build_skins.cuboid_faces; the motion
 * suite runs it first, and GRAVES_PHASE2_CHECKS=motion-loader runs it alone.
 */
public final class MotionChecks {
    private MotionChecks() {}
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("motion", Rig::new);
        suites.put("motion-loader", () -> (c, tick) -> { loaderFaceUv(); Phase2Checks.finish(c, "motion-loader"); });
    }

    /**
     * Boxes for mo-loader-faceuv, each a bone of its own: its name, its box json and, per face, the four corners with the texel
     * each reads ("d:x,y,z,u,v ..." by Direction id, ";" between faces), as build_skins.cuboid_faces lays them out (generated
     * from it). An uneven 3x5x2 box with all six faces moved, so a width and depth mix-up shows; the rig's right forearm box,
     * its top and bottom moved; the uneven box without faceuv, the vanilla Cuboid against the same transcription.
     */
    static final String[][] FACEUV_BOXES = {
        {"moved", "{\"origin\":[-1,-2,-3],\"size\":[3,5,2],\"uv\":[2,3],\"faceuv\":{\"top\":[0,40],\"bottom\":[10,40],\"right\":[20,40],\"front\":[30,40],\"left\":[40,40],\"back\":[50,40]}}",
            "0:2,-2,-1,3,40 -1,-2,-1,0,40 -1,-2,-3,0,42 2,-2,-3,3,42;1:2,3,-3,13,42 -1,3,-3,10,42 -1,3,-1,10,40 2,3,-1,13,40;2:2,-2,-3,33,40 -1,-2,-3,30,40 -1,3,-3,30,45 2,3,-3,33,45;3:-1,-2,-1,53,40 2,-2,-1,50,40 2,3,-1,50,45 -1,3,-1,53,45;4:-1,-2,-3,22,40 -1,-2,-1,20,40 -1,3,-1,20,45 -1,3,-3,22,45;5:2,-2,-1,42,40 2,-2,-3,40,40 2,3,-3,40,45 2,3,-1,42,45"},
        {"right_forearm", "{\"origin\":[-2,0,-2],\"size\":[4,6,4],\"uv\":[40,22],\"faceuv\":{\"top\":[44,26],\"bottom\":[48,16]}}",
            "0:2,0,2,48,26 -2,0,2,44,26 -2,0,-2,44,30 2,0,-2,48,30;1:2,6,-2,52,20 -2,6,-2,48,20 -2,6,2,48,16 2,6,2,52,16;2:2,0,-2,48,26 -2,0,-2,44,26 -2,6,-2,44,32 2,6,-2,48,32;3:-2,0,2,56,26 2,0,2,52,26 2,6,2,52,32 -2,6,2,56,32;4:-2,0,-2,44,26 -2,0,2,40,26 -2,6,2,40,32 -2,6,-2,44,32;5:2,0,2,52,26 2,0,-2,48,26 2,6,-2,48,32 2,6,2,52,32"},
        {"plain", "{\"origin\":[-1,-2,-3],\"size\":[3,5,2],\"uv\":[2,3]}",
            "0:2,-2,-1,7,3 -1,-2,-1,4,3 -1,-2,-3,4,5 2,-2,-3,7,5;1:2,3,-3,10,5 -1,3,-3,7,5 -1,3,-1,7,3 2,3,-1,10,3;2:2,-2,-3,7,5 -1,-2,-3,4,5 -1,3,-3,4,10 2,3,-3,7,10;3:-1,-2,-1,12,5 2,-2,-1,9,5 2,3,-1,9,10 -1,3,-1,12,10;4:-1,-2,-3,4,5 -1,-2,-1,2,5 -1,3,-1,2,10 -1,3,-3,4,10;5:2,-2,-1,9,5 2,-2,-3,7,5 2,3,-3,7,10 2,3,-1,9,10"}};

    /**
     * mo-loader-faceuv (Task 9 fix round 1, review M1): no troop model uses faceuv yet, so no render reaches the loader's faceuv
     * path. Each FACEUV_BOXES box, loaded through TroopModelLoader on a 64x64 sheet, must give exactly one quad a face, with the
     * quad's normal that face's direction and its four corners each reading the texel the Python side expects.
     */
    static void loaderFaceUv() {
        List<String> bad = new ArrayList<>(); int quads = 0;
        StringBuilder bones = new StringBuilder("{\"name\":\"root\",\"parent\":\"\",\"pivot\":[0,0,0],\"boxes\":[]}");
        for (String[] b : FACEUV_BOXES) bones.append(",{\"name\":\"").append(b[0]).append("\",\"parent\":\"root\",\"pivot\":[0,0,0],\"boxes\":[").append(b[1]).append("]}");
        try {
            var j = com.google.gson.JsonParser.parseString("{\"format\":3,\"troop\":\"probe\",\"rig\":\"biped\",\"size\":[64,64],"
                + "\"skins\":{\"1\":\"gravesandglory:textures/entity/troop/knight_s1.png\"},\"bones\":[" + bones + "]}").getAsJsonObject();
            TroopModelLoader.checkTroop(j);
            var m = new TroopModel<>(j, TroopModelLoader.parse(j).createModel(), Map.of("right", "root", "left", "root"), null, List.of());
            for (String[] b : FACEUV_BOXES) {
                Map<Integer, Set<String>> want = new TreeMap<>(), got = new TreeMap<>();
                for (String face : b[2].split(";")) { String[] f = face.split(":"); want.put(Integer.parseInt(f[0]), new TreeSet<>(List.of(f[1].split(" ")))); }
                ModelPart part = m.part(b[0]);
                if (part == null) { bad.add(b[0] + ": no part"); continue; }
                for (var cuboid : Phase2Checks.Depth.cuboidFaces(part)) for (float[] q : cuboid) {
                    quads++;
                    Direction dir = Direction.fromVector(Math.round(q[0]), Math.round(q[1]), Math.round(q[2]));
                    if (dir == null) { bad.add(b[0] + ": a quad with normal " + q[0] + "," + q[1] + "," + q[2]); continue; }
                    Set<String> corners = new TreeSet<>();
                    for (int k = 0; k < 4; k++) corners.add(String.format(Locale.ROOT, "%s,%s,%s,%s,%s", num(q[3 + 5 * k]), num(q[4 + 5 * k]), num(q[5 + 5 * k]), num(q[6 + 5 * k] * 64), num(q[7 + 5 * k] * 64)));
                    if (got.put(dir.getId(), corners) != null) bad.add(b[0] + ": two quads face " + dir);
                }
                for (int d = 0; d < 6; d++) if (!Objects.equals(want.get(d), got.get(d))) bad.add(b[0] + " " + Direction.byId(d) + ": got " + got.get(d) + ", want " + want.get(d));
            }
        } catch (Exception e) { bad.add("threw " + e); }
        report("mo-loader-faceuv", bad.isEmpty() && quads == 18, quads + " quads of 3 boxes (an uneven box with every face moved, the rig's right forearm, the uneven box without faceuv) against build_skins.cuboid_faces" + (bad.isEmpty() ? ": every corner and texel as expected" : ": " + bad));
    }
    /** A coordinate or texel as cuboid_faces prints it: whole numbers without a point, to 3 places otherwise. */
    static String num(float v) { float r = Math.round(v * 1000) / 1000f; return r == Math.round(r) ? Integer.toString(Math.round(r)) : String.format(Locale.ROOT, "%.3f", r).replaceAll("0+$", ""); }

    /** Harness only: pitch added to bones after the clip, by entity id (radians, bone name to added pitch). Read by MotionRigPoseMixin. */
    public static final Map<Integer, Map<String, Float>> BENDS = new ConcurrentHashMap<>();
    /** MotionRigPoseMixin: adds the entity's bends to its troop model's bones, right after setAngles. */
    public static void bend(Entity e, EntityModel<?> model) {
        var b = BENDS.get(e.getId());
        if (b == null || !(model instanceof TroopModel<?> m)) return;
        add(m, b);
    }
    static void add(TroopModel<?> m, Map<String, Float> b) {
        for (var x : b.entrySet()) { ModelPart p = m.bone(x.getKey()); if (p != null) p.pitch += x.getValue(); }
    }

    static final String BASELINE = System.getProperty("gravesandglory.motionBaseline", "");

    static final class Rig extends Phase2Checks.Stage {
        static final double DIST = 3, EYE = 1.2, CLOSE = 2.0, PARK = 24;
        static final float YAW = 180;
        static final int[] ANGLES = {0, 45, 90, 135, 180, 225, 270, 315};
        /** One posted troop: the troop, its armour stage and whether it is a Veteran. */
        record Who(String troop, int stage, boolean vet) {
            String key() { return troop + "-s" + stage + (vet ? "v" : ""); }
        }
        /** One shot: its file name (without .png), who (null: the empty frame), the kind (rest, hand, elbow, knee, bg), the angle round the troop and the hand view (front, right or left; null for the body). */
        record Shot(String name, Who who, String kind, int angle, String view) {}
        final List<Who> cast = new ArrayList<>();
        final Map<String, GravesAndGlory.Unit> units = new LinkedHashMap<>();
        final List<Shot> shots = new ArrayList<>();
        final List<String> bad = new ArrayList<>();
        int si, held, steady, lastCount, settle; String on = ""; double[] cam;
        CloudRenderMode cloudsBefore;
        String name() { return "motion"; }

        Rig() {
            loaderFaceUv();
            Phase2Checks.Turn.hook();
            for (String t : TroopModels.ids()) {
                var m = TroopModels.model(t);
                if (m == null || !"biped".equals(m.rig)) continue;
                int top = FacesChecks.Faces.hasStage3(t) ? 3 : 1;
                for (int s = 1; s <= top; s++) cast.add(new Who(t, s, false));
                if (m.veteran != null) cast.add(new Who(t, top, true));
            }
            for (int a : ANGLES) shots.add(new Shot("mo-rig-bg-" + a, null, "bg", a, null));
            for (Who w : cast) {
                for (int a : ANGLES) shots.add(new Shot("mo-rig-" + w.key() + "-rest-" + a, w, "rest", a, null));
                if (w.stage() == 1 && !w.vet()) {
                    var m = TroopModels.model(w.troop());
                    String hand = m.hand();
                    shots.add(new Shot("mo-rig-" + w.key() + "-hand-front", w, "hand", 0, "front"));
                    shots.add(new Shot("mo-rig-" + w.key() + "-hand-" + hand, w, "hand", 0, hand));
                }
                if (!w.vet() && TroopModels.model(w.troop()).bone("right_forearm") != null) {
                    for (int a : new int[]{0, 45, 90, 270}) shots.add(new Shot("mo-rig-" + w.key() + "-elbow-" + a, w, "elbow", a, null));
                    for (int a : new int[]{0, 90, 180, 270}) shots.add(new Shot("mo-rig-" + w.key() + "-knee-" + a, w, "knee", a, null));
                }
            }
        }

        void cast(GravesAndGlory.Battle b) {
            for (int i = 0; i < cast.size(); i++) {
                Who w = cast.get(i);
                var u = post(b, gold, w.troop(), 0, 0, YAW);
                if (w.vet()) u.mob.setCustomName(net.minecraft.text.Text.literal(ArmyMobs.levelName(u.spec, w.stage()) + ArmyMobs.VETERAN));
                if (w.stage() > 1 || w.vet()) stage(u, w.stage(), 1, w.stage());
                units.put(w.key(), u);
                place(u, i, false);
            }
            camera(b, 0, EYE, DIST, 0, 0);
        }

        /** Server: the troop at the platform's centre facing north, or parked high over the floor out of every view, on a grid kept inside the forced chunks. */
        void place(GravesAndGlory.Unit u, int i, boolean up) {
            var where = up ? at(0, 0) : at((i % 6) * 5 - 12.5, (i / 6) * 5 - 10).add(0, PARK, 0);
            pins.removeIf(p -> p.mob() == u.mob); pins.add(new Pin(u.mob, where, YAW)); pin(u.mob, where, YAW);
        }
        void placeAll(GravesAndGlory.Battle b, String upKey) {
            for (int i = 0; i < cast.size(); i++) place(units.get(cast.get(i).key()), i, cast.get(i).key().equals(upKey));
        }

        /** The knee limit of a troop model in degrees (90 when it has none). */
        static float kneeLimit(TroopModel<?> m) {
            float k = 90f;
            for (String shin : List.of("right_shin", "left_shin")) { float[] l = m.jointLimit(shin); if (l != null) k = Math.min(k, l[1]); }
            return k;
        }
        /** The bends of a shot: both elbows -90 degrees or both knees +90 (capped at the troop's knee limit); empty for the rest of the shots. */
        static Map<String, Float> bends(TroopModel<?> m, Shot s) {
            Map<String, Float> b = new TreeMap<>();
            if (s.kind().equals("elbow")) { b.put("right_forearm", -90 * TroopModel.RAD); b.put("left_forearm", -90 * TroopModel.RAD); }
            if (s.kind().equals("knee")) { float k = kneeLimit(m) * TroopModel.RAD; b.put("right_shin", k); b.put("left_shin", k); }
            return b;
        }

        public void step(MinecraftClient c, int tick) {
            if (tick == 20) { c.setScreen(null); var id = c.player.getUuid(); server(c, b -> { player = id; stage = 1; }); }
            if (finished) return;
            if (tick > 30000) { report("p2-motion-timeout", false, "stuck at shot " + si + " of " + shots.size() + ", client step " + cs + ", server stage " + stage); end(c); return; }
            switch (cs) {
                case 0 -> {
                    boolean lit = litStart(c);
                    if (lit && ready && posted.size() == cast.size() && posted.stream().allMatch(u -> client(c, u.mob) != null)) {
                        c.options.hudHidden = true; cloudsBefore = c.options.getCloudRenderMode().getValue(); c.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                        wait = 0; cs = 1;
                    } else if (++wait % 600 == 0) System.out.println("[p2-wait] motion step 0 after " + wait + " ticks: ready=" + ready + ", posted " + posted.size() + " of " + cast.size() + "; " + litWhy(c));
                }
                case 1 -> shoot(c);
                case 2 -> {
                    if (++wait < 60) return;
                    if (!compare(c) && wait < 400) return;
                    serverDone = true; end(c);
                }
                default -> {}
            }
        }

        public void cleanup(MinecraftClient c) {
            Phase2Checks.Turn.noTarget = false; TroopAnimator.forced.clear(); BENDS.clear();
            if (cloudsBefore != null) c.options.getCloudRenderMode().setValue(cloudsBefore);
            super.cleanup(c);
        }

        static void save(MinecraftClient c, String name) { ScreenshotRecorder.saveScreenshot(c.runDirectory, name + ".png", c.getFramebuffer(), t -> {}); }

        void shoot(MinecraftClient c) {
            if (si >= shots.size()) {
                TroopAnimator.forced.clear(); BENDS.clear();
                report("mo-rig-shots", bad.isEmpty(), shots.size() + " shots of " + cast.size() + " troops and stages" + (bad.isEmpty() ? ", each taken lit, in gold, with the camera where it was set and the pose drawn as asked (1 degree)" : ", not as asked: " + bad));
                cs = 2; wait = 0; return;
            }
            if (awayFor > 0) { litStart(c); return; }
            Shot s = shots.get(si);
            wait++;
            if (s.who() == null) { // an empty frame: the camera of the angle, every troop parked
                if (wait == 1) {
                    on = ""; TroopAnimator.forced.clear(); BENDS.clear(); cam = view(s, null); final double[] v = cam;
                    server(c, b -> { placeAll(b, ""); camera(b, v[0], v[1], v[2], (float) v[3], (float) v[4]); });
                    settle = 20; held = 0; return;
                }
                if (wait < 6 + settle) return;
                held++;
                boolean there = there(c), away = posted.stream().allMatch(u -> { var e = client(c, u.mob); return e == null || e.getY() > site.getY() + 5; });
                if ((there && away) || held > 300) {
                    if (!shotReady(c, s.name())) return;
                    if (!(there && away)) bad.add(s.name() + ": taken anyway after 300 ticks (camera " + there + ", troops away " + away + ")");
                    save(c, s.name()); si++; wait = 0; held = 0;
                }
                return;
            }
            var u = units.get(s.who().key()); var e = client(c, u.mob); var m = TroopModels.model(s.who().troop());
            if (e == null || m == null) { if (++held > 400) { bad.add(s.name() + ": no client entity or model"); si++; held = 0; wait = 0; } return; }
            TroopModels.forceFace(e, 0);
            if (wait == 1) {
                boolean change = !s.who().key().equals(on);
                if (change) { on = s.who().key(); final String k = on; server(c, b -> placeAll(b, k)); }
                TroopAnimator.forced.clear(); BENDS.clear();
                TroopAnimator.forced.put(e.getId(), new TroopAnimator.Forced("idle", 0f));
                var bends = bends(m, s); if (!bends.isEmpty()) BENDS.put(e.getId(), bends);
                cam = view(s, m); final double[] v = cam; server(c, b -> camera(b, v[0], v[1], v[2], (float) v[3], (float) v[4]));
                settle = change ? 15 : 0; steady = 0; held = 0; return;
            }
            if (wait < 6 + settle) return;
            if (steady == 0) { // a stage rename replaces the client entity: re-assert the forced pose and the bends on its id
                TroopAnimator.forced.put(e.getId(), new TroopAnimator.Forced("idle", 0f));
                var bends = bends(m, s); if (!bends.isEmpty()) BENDS.put(e.getId(), bends);
            }
            var d = TroopRenderer.drawn(e); boolean drawn = d != null && d.pose != null && d.last != null && d.count > lastCount; lastCount = d == null ? 0 : d.count;
            boolean gilt = drawn && "gold".equals(d.last.cloth());
            boolean ranked = drawn && d.last.look().armourTier() == s.who().stage() && d.last.look().veteran() == s.who().vet();
            float[] want = want(m, s, !u.mob.getOffHandStack().isEmpty());
            boolean posed = drawn && Phase2Checks.Anim.maxRot(d.pose, want) <= 1 && Phase2Checks.Anim.maxPos(d.pose, want) <= 0.1;
            boolean there = there(c);
            if (!there && (wait == 60 || wait == 120 || wait == 180 || wait == 240)) { final double[] v = cam; server(c, b -> camera(b, v[0], v[1], v[2], (float) v[3], (float) v[4])); }
            boolean faced = e instanceof net.minecraft.entity.LivingEntity le && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(le.getBodyYaw() - YAW)) < 2 && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(le.prevBodyYaw - YAW)) < 2;
            steady = gilt && ranked && posed && there && faced ? steady + 1 : 0; held++;
            if (steady >= 3 || held > 300) {
                if (!shotReady(c, s.name())) return;
                if (steady < 3) bad.add(s.name() + ": taken anyway after 300 ticks (drawn " + drawn + ", in gold " + gilt + ", rank " + ranked + ", pose " + posed + (drawn ? " (off by " + Phase2Checks.Anim.maxRot(d.pose, want) + " degrees)" : "") + ", camera " + there + ", facing " + faced + ")");
                save(c, s.name()); si++; wait = 0; steady = 0; held = 0;
            }
        }

        boolean there(MinecraftClient c) {
            var p = c.player;
            return Math.abs(p.getX() - (site.getX() + .5 + cam[0])) < .05 && Math.abs(p.getEyeY() - (site.getY() + cam[1])) < .05 && Math.abs(p.getZ() - (site.getZ() + .5 + cam[2])) < .05
                && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw() - (float) cam[3])) < 1 && Math.abs(p.getPitch() - cam[4]) < 1;
        }

        /** The pose a shot asks for: the idle clip at 0 s, then the shot's bends. */
        static float[] want(TroopModel<?> m, Shot s, boolean shield) {
            m.getPart().traverse().forEach(ModelPart::resetTransform);
            TroopAnimator.applyForced(m, new TroopAnimator.Forced("idle", 0f), TroopAnimator.row(m), shield);
            add(m, bends(m, s));
            float[] out = m.snapshot(null); m.getPart().traverse().forEach(ModelPart::resetTransform); return out;
        }

        /** The camera of a shot: {dx, eye, dz, yaw, pitch} from the platform's centre. A hand view stands CLOSE blocks from the
         *  hand's rest point, found through the arm bone (the hand bone's parent before the split, its grandparent after). */
        double[] view(Shot s, TroopModel<?> m) {
            double y = Math.toRadians(YAW), fx = -Math.sin(y), fz = Math.cos(y), rx = -Math.cos(y), rz = -Math.sin(y);
            if (s.view() == null) {
                double a = Math.toRadians(s.angle()), ux = Math.cos(a) * fx + Math.sin(a) * rx, uz = Math.cos(a) * fz + Math.sin(a) * rz;
                return new double[]{DIST * ux, EYE, DIST * uz, Phase2Checks.Turn.yawOf(-ux, -uz), 0};
            }
            boolean right = "right".equals(m.hand());
            double[] g = handRest(m, right ? "right_arm" : "left_arm", right ? -1 : 1, !units.get(s.who().key()).mob.getOffHandStack().isEmpty());
            return switch (s.view()) {
                case "front" -> new double[]{g[0] + CLOSE * fx, g[1], g[2] + CLOSE * fz, Phase2Checks.Turn.yawOf(-fx, -fz), 0};
                case "right" -> new double[]{g[0] + CLOSE * rx, g[1], g[2] + CLOSE * rz, Phase2Checks.Turn.yawOf(-rx, -rz), 0};
                default -> new double[]{g[0] - CLOSE * rx, g[1], g[2] - CLOSE * rz, Phase2Checks.Turn.yawOf(rx, rz), 0};
            };
        }
        /** The hand's rest point (arm frame (x, 8, 0)) at the idle pose: world axes, blocks from the feet. */
        static double[] handRest(TroopModel<?> m, String arm, float x, boolean shield) {
            m.getPart().traverse().forEach(ModelPart::resetTransform);
            TroopAnimator.applyForced(m, new TroopAnimator.Forced("idle", 0f), TroopAnimator.row(m), shield);
            MatrixStack ms = Phase2Checks.Turn.base(); for (ModelPart p : m.path(arm)) p.rotate(ms);
            Vector4f v = new Vector4f(x / 16f, 8 / 16f, 0, 1f); ms.peek().getPositionMatrix().transform(v);
            m.getPart().traverse().forEach(ModelPart::resetTransform);
            return new double[]{v.x, v.y, v.z};
        }

        // ---- the comparison ----
        /** Compares every rest and hand shot with the baseline's; false while a shot file is not yet written. */
        boolean compare(MinecraftClient c) {
            File dir = new File(c.runDirectory, "screenshots");
            for (Shot s : shots) if (!new File(dir, s.name() + ".png").isFile()) { if (wait < 399) return false; }
            if (BASELINE.isBlank()) {
                report("mo-rig", true, "no baseline asked (GRAVES_MOTION_BASELINE unset): captured " + shots.size() + " shots in " + dir + " for a later run to compare with");
                return true;
            }
            File base = new File(BASELINE);
            Map<String, double[]> worst = new LinkedHashMap<>(); List<String> missing = new ArrayList<>(), over = new ArrayList<>();
            int compared = 0;
            for (Shot s : shots) {
                if (!s.kind().equals("rest") && !s.kind().equals("hand")) continue;
                File now = new File(dir, s.name() + ".png"), was = new File(base, s.name() + ".png");
                if (!now.isFile() || !was.isFile()) { missing.add(s.name()); continue; }
                try {
                    BufferedImage a = ImageIO.read(was), b = ImageIO.read(now);
                    BufferedImage bgA = null, bgB = null;
                    if (s.kind().equals("rest")) {
                        File ba = new File(base, "mo-rig-bg-" + s.angle() + ".png"), bb = new File(dir, "mo-rig-bg-" + s.angle() + ".png");
                        if (ba.isFile()) bgA = ImageIO.read(ba);
                        if (bb.isFile()) bgB = ImageIO.read(bb);
                    }
                    double[] r = diff(a, b, bgA, bgB);
                    compared++;
                    System.out.println(String.format(Locale.ROOT, "[mo-rig-diff] %s: %.3f%% of the frame (%d px), %.2f%% of %d troop px", s.name(), r[0], (long) r[2], r[1], (long) r[3]));
                    String k = s.who().troop();
                    double[] w = worst.computeIfAbsent(k, x -> new double[]{0, 0});
                    w[0] = Math.max(w[0], r[0]); if (r[1] >= 0) w[1] = Math.max(w[1], r[1]);
                    if (r[0] > 0.5) over.add(s.name() + String.format(Locale.ROOT, " %.3f%%", r[0]));
                } catch (Exception x) { missing.add(s.name() + " (" + x + ")"); }
            }
            StringBuilder sb = new StringBuilder();
            for (var x : worst.entrySet()) sb.append(x.getKey()).append(String.format(Locale.ROOT, " worst %.3f%% of the frame, %.2f%% of troop px; ", x.getValue()[0], x.getValue()[1]));
            report("mo-rig", missing.isEmpty() && over.isEmpty() && compared > 0, compared + " rest and hand shots against " + base + ": " + sb + (over.isEmpty() ? "none over 0.5% of the frame" : "over 0.5%: " + over) + (missing.isEmpty() ? "" : "; missing: " + missing));
            return true;
        }

        /** {frame %, troop %, changed px, troop px}: a pixel is changed when a channel differs by more than 8; it is troop where either shot differs from its empty frame (troop % is -1 without empty frames). */
        static double[] diff(BufferedImage a, BufferedImage b, BufferedImage bgA, BufferedImage bgB) {
            int w = Math.min(a.getWidth(), b.getWidth()), h = Math.min(a.getHeight(), b.getHeight());
            boolean masks = bgA != null && bgB != null && bgA.getWidth() >= w && bgB.getWidth() >= w && bgA.getHeight() >= h && bgB.getHeight() >= h;
            long changed = 0, troop = 0, total = (long) w * h;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int p = a.getRGB(x, y), q = b.getRGB(x, y);
                boolean ch = differs(p, q, 8);
                if (ch) changed++;
                if (masks && (differs(p, bgA.getRGB(x, y), 8) || differs(q, bgB.getRGB(x, y), 8))) troop++;
            }
            return new double[]{100.0 * changed / total, masks && troop > 0 ? 100.0 * changed / troop : -1, changed, troop};
        }
        static boolean differs(int p, int q, int tol) {
            for (int sh = 0; sh <= 16; sh += 8) if (Math.abs(((p >> sh) & 255) - ((q >> sh) & 255)) > tol) return true;
            return false;
        }
    }
}
