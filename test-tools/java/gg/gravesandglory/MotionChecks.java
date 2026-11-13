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
        suites.put("motion", Motion::new);
        suites.put("motion-live", () -> new Live(false));
        suites.put("motion-rig", () -> new Rig("motion-rig"));
        suites.put("motion-grade", MotionGrade::new);
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
    /**
     * R60: the game paused with nothing over the world for a shot. A screen (the pause menu the shots used before) makes the game
     * draw its HUD whatever options.hudHidden says (and its "Game Paused" title), so the pause is a harness overlay that pauses the
     * game (Overlay.pausesGame) and draws nothing, with the HUD hidden; unstill restores hudHidden as it was. mo-fade, mo-fade-smoke,
     * mo-fade-overlap and hl-corpse's cache and fade shots pause this way.
     */
    static final class Still extends net.minecraft.client.gui.screen.Overlay {
        @Override public void render(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {}
    }
    static Boolean hudBefore;
    static void still(MinecraftClient c) {
        track(); if (c.getOverlay() instanceof Still) return;
        hudBefore = c.options.hudHidden; c.options.hudHidden = true; c.getToastManager().clear(); c.setOverlay(new Still());
    }
    static void unstill(MinecraftClient c) {
        if (!(c.getOverlay() instanceof Still)) return;
        c.setOverlay(null); if (hudBefore != null) c.options.hudHidden = hudBefore; hudBefore = null;
    }
    /** R61: the partial tick the last frame was drawn at (WorldRenderContext.tickDelta, the renderer's own; paused, the client's pausedTickDelta), from the harness's own listener; NaN before the first frame. */
    static volatile float frameDelta = Float.NaN; static boolean tracking;
    static void track() { if (tracking) return; tracking = true; net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(ctx -> frameDelta = ctx.tickDelta()); }
    /** mo-fade-smoke (R57): the body watched for poofs (its middle), whether the count is on, and the poofs within 2 blocks of it (and anywhere) counted. */
    static volatile net.minecraft.util.math.Vec3d SMOKE_AT; static volatile boolean smokeCounting; static int smokePoofs, smokeAll;
    /** ParticleCountMixin: each particle the client asks to spawn; a poof within 2 blocks of the watched body counts while counting is on. */
    public static void particle(net.minecraft.particle.ParticleEffect p, double x, double y, double z) {
        var at = SMOKE_AT; if (!smokeCounting || at == null || p.getType() != net.minecraft.particle.ParticleTypes.POOF) return;
        smokeAll++; if (at.squaredDistanceTo(x, y, z) <= 4) smokePoofs++;
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
        /**
         * GeckoLib troops (GeoTroopRenderer: the knight first) draw outside TroopRenderer's record, the forced clip and the bends
         * (MotionRigPoseMixin hooks the legacy setAngles): their shots read GeoDraws (drawn, the gold sheet, the armour tier; the Geo
         * draw has no veteran flag and its idle runs on entity age, so neither the rank's veteran part nor the pose is asserted), their
         * bend shots are not made and their shots are left out of the baseline compare. These count what that removes.
         */
        final java.util.Set<String> geoTroops = new java.util.TreeSet<>(); int geoBendsSkipped, geoPoseUnchecked;
        CloudRenderMode cloudsBefore;
        final String suite;
        String name() { return suite; }

        Rig() { this("motion"); }
        Rig(String suite) {
            this.suite = suite;
            loaderFaceUv();
            Phase2Checks.Turn.hook();
            GeoDraws.install();
            for (String t : TroopModels.ids()) {
                var m = TroopModels.model(t);
                if (m == null || !"biped".equals(m.rig)) continue;
                if (GeoDraws.geo(t)) geoTroops.add(t);
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
                if (!w.vet() && TroopModels.model(w.troop()).bone("right_forearm") != null && geoTroops.contains(w.troop())) geoBendsSkipped += 12; // no bend hook on the Geo path
                else if (!w.vet() && TroopModels.model(w.troop()).bone("right_forearm") != null) {
                    for (int a : new int[]{0, 45, 90, 270}) shots.add(new Shot("mo-rig-" + w.key() + "-elbow-" + a, w, "elbow", a, null));
                    for (int a : new int[]{0, 90, 180, 270}) shots.add(new Shot("mo-rig-" + w.key() + "-knee-" + a, w, "knee", a, null));
                    // R98: the seams at a half bend too (the guard, wall and brace hold elbows and knees near 30-60 degrees)
                    for (int a : new int[]{0, 90}) shots.add(new Shot("mo-rig-" + w.key() + "-elbow45-" + a, w, "elbow45", a, null));
                    for (int a : new int[]{0, 90}) shots.add(new Shot("mo-rig-" + w.key() + "-knee45-" + a, w, "knee45", a, null));
                }
            }
            for (Shot x : shots) if (x.who() != null && geoTroops.contains(x.who().troop())) geoPoseUnchecked++;
        }
        /** What the GeckoLib troops' shots leave out, for the reports. */
        String geoNote() {
            return geoTroops.isEmpty() ? "" : "; GeckoLib " + geoTroops + ": " + geoBendsSkipped + " bend shots not made (no bend hook on the Geo path), " + geoPoseUnchecked
                + " shots read from GeoDraws with no pose or veteran check (the Geo idle runs on entity age) and left out of the baseline compare";
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
        /**
         * The bends of a shot: both elbows -90 degrees or both knees +90 (capped at the troop's knee limit), or half that for the 45
         * shots; empty for the rest of the shots.
         */
        static Map<String, Float> bends(TroopModel<?> m, Shot s) {
            Map<String, Float> b = new TreeMap<>();
            if (s.kind().equals("elbow")) { b.put("right_forearm", -90 * TroopModel.RAD); b.put("left_forearm", -90 * TroopModel.RAD); }
            if (s.kind().equals("knee")) { float k = kneeLimit(m) * TroopModel.RAD; b.put("right_shin", k); b.put("left_shin", k); }
            if (s.kind().equals("elbow45")) { b.put("right_forearm", -45 * TroopModel.RAD); b.put("left_forearm", -45 * TroopModel.RAD); }
            if (s.kind().equals("knee45")) { float k = Math.min(45f, kneeLimit(m)) * TroopModel.RAD; b.put("right_shin", k); b.put("left_shin", k); }
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
            Phase2Checks.Turn.noTarget = false; TroopMotion.clearForced(); BENDS.clear();
            if (cloudsBefore != null) c.options.getCloudRenderMode().setValue(cloudsBefore);
            super.cleanup(c);
        }

        static void save(MinecraftClient c, String name) { ScreenshotRecorder.saveScreenshot(c.runDirectory, name + ".png", c.getFramebuffer(), t -> {}); }

        void shoot(MinecraftClient c) {
            if (si >= shots.size()) {
                TroopMotion.clearForced(); BENDS.clear();
                report("mo-rig-shots", bad.isEmpty(), shots.size() + " shots of " + cast.size() + " troops and stages" + (bad.isEmpty() ? ", each taken lit, in gold, with the camera where it was set and the pose drawn as asked (1 degree)" : ", not as asked: " + bad) + geoNote());
                cs = 2; wait = 0; return;
            }
            if (awayFor > 0) { litStart(c); return; }
            Shot s = shots.get(si);
            wait++;
            if (s.who() == null) { // an empty frame: the camera of the angle, every troop parked
                if (wait == 1) {
                    on = ""; TroopMotion.clearForced(); BENDS.clear(); cam = view(s, null); final double[] v = cam;
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
            boolean geo = GeoDraws.geo(e);
            if (wait == 1) {
                boolean change = !s.who().key().equals(on);
                if (change) { on = s.who().key(); final String k = on; server(c, b -> placeAll(b, k)); }
                TroopMotion.clearForced(); BENDS.clear();
                if (!geo) { TroopMotion.force(e.getId(), "idle", 0); var bends = bends(m, s); if (!bends.isEmpty()) BENDS.put(e.getId(), bends); }
                cam = view(s, m); final double[] v = cam; server(c, b -> camera(b, v[0], v[1], v[2], (float) v[3], (float) v[4]));
                settle = change ? 15 : 0; steady = 0; held = 0; return;
            }
            if (wait < 6 + settle) return;
            if (steady == 0 && !geo) { // a stage rename replaces the client entity: re-assert the forced pose and the bends on its id
                TroopMotion.force(e.getId(), "idle", 0);
                var bends = bends(m, s); if (!bends.isEmpty()) BENDS.put(e.getId(), bends);
            }
            var d = geo ? null : TroopRenderer.drawn(e); var g = geo ? GeoDraws.drawn(e) : null;
            boolean drawn, gilt, ranked, posed; float[] want = null;
            if (geo) {
                drawn = g != null && g.pose != null && g.count > lastCount; lastCount = g == null ? 0 : g.count;
                gilt = drawn && g.texture.endsWith("_gold.png");
                ranked = drawn && g.armour == s.who().stage();
                posed = drawn;
            } else {
                drawn = d != null && d.pose != null && d.last != null && d.count > lastCount; lastCount = d == null ? 0 : d.count;
                gilt = drawn && "gold".equals(d.last.cloth());
                ranked = drawn && d.last.look().armourTier() == s.who().stage() && d.last.look().veteran() == s.who().vet();
                want = want(m, s, !u.mob.getOffHandStack().isEmpty());
                posed = drawn && Phase2Checks.Anim.maxRot(d.pose, want) <= 1 && Phase2Checks.Anim.maxPos(d.pose, want) <= 0.1;
            }
            boolean there = there(c);
            if (!there && (wait == 60 || wait == 120 || wait == 180 || wait == 240)) { final double[] v = cam; server(c, b -> camera(b, v[0], v[1], v[2], (float) v[3], (float) v[4])); }
            boolean faced = e instanceof net.minecraft.entity.LivingEntity le && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(le.getBodyYaw() - YAW)) < 2 && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(le.prevBodyYaw - YAW)) < 2;
            steady = gilt && ranked && posed && there && faced ? steady + 1 : 0; held++;
            if (steady >= 3 || held > 300) {
                if (!shotReady(c, s.name())) return;
                if (steady < 3) bad.add(s.name() + ": taken anyway after 300 ticks (drawn " + drawn + ", in gold " + gilt + ", rank " + ranked + ", pose " + posed + (drawn && !geo ? " (off by " + Phase2Checks.Anim.maxRot(d.pose, want) + " degrees)" : "") + (geo ? " [GeckoLib: " + (g == null ? "never drawn" : g.texture + ", tier " + g.armour) + "]" : "") + ", camera " + there + ", facing " + faced + ")");
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
            TroopMotion.applyForced(m,"idle",0f,shield);
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
            TroopMotion.applyForced(m,"idle",0f,shield);
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
                report("mo-rig", true, "no baseline asked (GRAVES_MOTION_BASELINE unset): captured " + shots.size() + " shots in " + dir + " for a later run to compare with" + geoNote());
                return true;
            }
            File base = new File(BASELINE);
            Map<String, double[]> worst = new LinkedHashMap<>(); List<String> missing = new ArrayList<>(), over = new ArrayList<>();
            int compared = 0;
            for (Shot s : shots) {
                if (!s.kind().equals("rest") && !s.kind().equals("hand")) continue;
                if (geoTroops.contains(s.who().troop())) continue; // a Geo idle runs on entity age: no fixed pose to compare
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
            report("mo-rig", missing.isEmpty() && over.isEmpty() && compared > 0, compared + " rest and hand shots against " + base + ": " + sb + (over.isEmpty() ? "none over 0.5% of the frame" : "over 0.5%: " + over) + (missing.isEmpty() ? "" : "; missing: " + missing) + geoNote());
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

    // ---- Task 13: the motion engine drives troops ----

    /**
     * The motion suite: the live checks (Live: mo-stance, mo-cull with mo-limits, mo-stance-live, mo-crowd, mo-late-join) on a platform of their
     * own, then the rig shots (Rig) on a fresh one. Each stage's client tick counts from its own start.
     */
    static final class Motion implements Phase2Checks.Suite, Phase2Checks.ServerWatch {
        final Live live = new Live(true);
        volatile Phase2Checks.Stage cur = live;
        int base;
        public void step(MinecraftClient c, int tick) {
            if (cur == live) {
                live.step(c, tick);
                if (live.done) { live.cleanup(c); base = tick; cur = new Rig(); }
                return;
            }
            cur.step(c, tick - base);
        }
        public void serverTick(GravesAndGlory.Battle b) { cur.serverTick(b); }
        public void cleanup(MinecraftClient c) { cur.cleanup(c); }
    }

    /** The largest seed the engine can draw (MotionMath.seed's ranges at their top): the widest swing and hit gains. */
    static final MotionMath.Seed MAX_SEED = new MotionMath.Seed(0, 1.1, 1.1, 1.15, 0, 0, 0, 0, 0);
    /** Where the simulated sequences start (render seconds): any time well past 0. */
    static final double T0 = 100;

    /**
     * mo-stance (carry B11, replacing p2-anim-stance): the plain-hold rule for every troop of the sheet. On Hold a shield or spear troop
     * braces (a shield troop formed in its wall shows the wall) and every other troop holds; the engine plays the brace stance for a
     * brace and the guard for a hold.
     */
    static void stanceRule() {
        List<String> bad = new ArrayList<>(); int brace = 0, hold = 0, walls = 0;
        for (var row : Spec.TROOPS) {
            var tags = ArmyMobs.tags(row); boolean sh = tags.contains("shield"), sp = tags.contains("spear");
            String s = BattleMath.stance("hold", sh, sp, row.ranged(), false, false, false), want = sh || sp ? "brace" : "hold";
            String key = MotionEngine.stanceKey(s, false, false, false, false), wantKey = sh || sp ? "brace" : "guard";
            if (!s.equals(want) || !key.equals(wantKey)) bad.add(row.id() + ": " + s + " playing " + key + ", want " + want + " playing " + wantKey);
            if (sh) { String f = BattleMath.stance("hold", true, sp, row.ranged(), true, false, false); if (!f.equals("wall")) bad.add(row.id() + ": formed " + f + ", want wall"); else walls++; }
            if (want.equals("brace")) brace++; else hold++;
        }
        report("mo-stance", bad.isEmpty() && brace > 0 && hold > 0, Spec.TROOPS.length + " troops on Hold: " + brace + " shield or spear troops brace (the engine's brace stance; " + walls + " shield troops show the wall when formed), " + hold + " others hold (the engine's guard)" + (bad.isEmpty() ? "" : "; wrong: " + bad));
    }

    // ---- synthetic clips (no clip assets ship yet: mo-cull and mo-late-join install these and put the real ones back) ----
    /** Peaks per bone: rotation x, y, z (degrees) then position x, y, z (pixels). */
    static Map<String, double[]> peaks(Object... kv) {
        Map<String, double[]> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (double[]) kv[i + 1]);
        return m;
    }
    static double[] p(double... v) { double[] o = new double[6]; System.arraycopy(v, 0, o, 0, v.length); return o; }
    static final Map<String, double[]> ATTACK_PEAKS = peaks("root", p(15, 30, 0, 0, -3, 6), "body", p(30, 45, 10), "head", p(20, -30, 0),
        "right_arm", p(-170, 20, 30), "right_forearm", p(-110, 0, 0), "left_arm", p(-60, -20, -30), "left_forearm", p(-80, 0, 0),
        "right_leg", p(-40, 0, 0), "left_leg", p(30, 0, 0), "right_shin", p(60, 0, 0), "left_shin", p(40, 0, 0));
    static final Map<String, Map<String, double[]>> COMMON_PEAKS = new LinkedHashMap<>();
    static {
        COMMON_PEAKS.put("idle", peaks("body", p(3, 0, 0), "head", p(-5, 0, 0)));
        COMMON_PEAKS.put("carry", peaks("right_arm", p(-30, 0, 0), "right_forearm", p(-40, 0, 0)));
        COMMON_PEAKS.put("guard", peaks("right_arm", p(-60, 0, 10), "right_forearm", p(-50, 0, 0), "left_arm", p(-40, 0, -10), "root", p(0, 0, 0, 0, 1, 0)));
        COMMON_PEAKS.put("brace", peaks("root", p(0, 0, 0, 0, 3, 0), "right_leg", p(-25, 0, 0), "left_leg", p(25, 0, 0), "right_shin", p(40, 0, 0), "left_shin", p(40, 0, 0), "left_arm", p(-70, 0, -10)));
        COMMON_PEAKS.put("wall", peaks("left_arm", p(-90, 0, -10), "left_forearm", p(-30, 0, 0), "root", p(0, 0, 0, 0, 2, 1)));
        COMMON_PEAKS.put("shield", peaks("left_arm", p(-80, 10, -10)));
        COMMON_PEAKS.put("aim", peaks("right_arm", p(-90, 0, 0), "left_arm", p(-90, 30, 0), "body", p(0, -30, 0)));
        COMMON_PEAKS.put("rest", peaks("right_arm", p(-15, 0, 0)));
        COMMON_PEAKS.put("hit_head", peaks("head", p(-35, 25, 0), "body", p(-15, 0, 0), "root", p(0, 0, 0, 0, 0, -3)));
        COMMON_PEAKS.put("hit_body", peaks("body", p(-30, 15, 0), "root", p(-10, 0, 0, 0, 0, -3), "right_arm", p(-30, 0, 20), "left_arm", p(-30, 0, -20)));
        COMMON_PEAKS.put("hit_legs", peaks("right_leg", p(30, 0, 0), "left_leg", p(-20, 0, 0), "right_shin", p(50, 0, 0), "root", p(0, 0, 0, 0, 2, -2)));
        COMMON_PEAKS.put("block", peaks("left_arm", p(-100, 0, -20), "left_forearm", p(-60, 0, 0), "right_arm", p(-60, 0, 0), "body", p(-10, -20, 0), "root", p(0, 0, 0, 0, 0, -2)));
        COMMON_PEAKS.put("death", peaks("root", p(-90, 0, 0, 0, 10, -6), "right_arm", p(-150, 0, 0), "left_arm", p(-150, 0, 0), "head", p(30, 0, 0)));
        COMMON_PEAKS.put("death_back", peaks("root", p(90, 0, 0, 0, 10, 6), "right_arm", p(-150, 0, 0), "head", p(-30, 0, 0)));
        COMMON_PEAKS.put("death_left", peaks("root", p(0, 0, -90, 0, 10, 0), "left_arm", p(-150, 0, 0)));
        COMMON_PEAKS.put("death_right", peaks("root", p(0, 0, 90, 0, 10, 0), "right_arm", p(-150, 0, 0)));
        COMMON_PEAKS.put("fall", peaks("root", p(-90, 0, 0, 0, 10, -6)));
        COMMON_PEAKS.put("raise", peaks("right_arm", p(-170, 0, 0), "right_forearm", p(-30, 0, 0)));
    }
    /** A synthetic clip: each bone's rotation and position keyed linear at the times, at the scales of its peak. */
    static Clip clip(String name, double length, boolean loop, double impact, Map<String, double[]> peaks, double[] times, double[] scales) {
        StringBuilder sb = new StringBuilder("{\"format\":1,\"clip\":\"").append(name).append("\",\"length\":").append(length).append(",\"loop\":").append(loop);
        if (impact >= 0) sb.append(",\"markers\":{\"impact\":").append(impact).append('}');
        sb.append(",\"bones\":{"); boolean first = true;
        for (var e : peaks.entrySet()) {
            if (!first) sb.append(','); first = false;
            sb.append('"').append(e.getKey()).append("\":{\"rotation\":").append(keys(times, scales, e.getValue(), 0)).append(",\"position\":").append(keys(times, scales, e.getValue(), 3)).append('}');
        }
        return Clip.parse(sb.append("}}").toString());
    }
    static String keys(double[] times, double[] scales, double[] v, int o) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < times.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(String.format(Locale.ROOT, "{\"t\":%.4f,\"i\":\"linear\",\"v\":[%.3f,%.3f,%.3f]}", times[i], v[o] * scales[i], v[o + 1] * scales[i], v[o + 2] * scales[i]));
        }
        return sb.append(']').toString();
    }
    /** A synthetic attack: a wind-up to -0.6 of its peaks half way to the impact, the peaks at the impact, rest at the end. */
    static Clip attack(String name, double impact, double length, Map<String, double[]> peaks) {
        return clip(name, length, false, impact, peaks, new double[]{0, impact / 2, impact, length}, new double[]{0, -.6, 1, 0});
    }
    /** The synthetic clips of one rig and family into out: every attack variant of the family, and the common stances, hits, deaths and scene clips. */
    static void synthetic(String rig, String family, Map<String, Clip> out) {
        for (var row : Tactics.variants(family)) {
            double impact = row.impactTicks() / (double) Clip.TICKS;
            out.put(rig + "/" + family + "/" + row.id(), attack(row.id(), impact, Math.max(.8, impact + .6), ATTACK_PEAKS));
        }
        for (var e : COMMON_PEAKS.entrySet()) {
            String n = e.getKey(); Clip c;
            if (TroopMotion.STANCES.contains(n)) c = clip(n, 2, true, -1, e.getValue(), new double[]{0, 1, 2}, new double[]{1, .5, 1});
            else if (n.startsWith("hit_") || n.equals("block")) c = clip(n, .5, false, -1, e.getValue(), new double[]{0, .1, .5}, new double[]{0, 1, 0});
            else c = clip(n, 1.5, false, -1, e.getValue(), new double[]{0, .9, 1.5}, new double[]{0, 1, 1});
            out.put(rig + "/" + TroopMotion.COMMON + "/" + n, c);
        }
    }

    // ---- mo-cull ----
    interface Frames { MotionInput at(int frame, double now); }
    /** The worst margin found so far: how far a pose reached outside the cull reach (blocks, negative inside), where, and how many poses. */
    static final class Worst {
        double out = Double.NEGATIVE_INFINITY; String at = ""; int poses;
        /** The farthest any pose reached on each side of the box (blocks over the feet): radius, lowest bottom, highest top (fix round 1, I1: the slack). */
        double radius = Double.NEGATIVE_INFINITY, bottom = Double.POSITIVE_INFINITY, top = Double.NEGATIVE_INFINITY;
        /** Carry C13: the most a jointed bone went past its limit (degrees, negative inside) and where; the deepest shin bend (degrees past the file pose). */
        double over = Double.NEGATIVE_INFINITY, shin = Double.NEGATIVE_INFINITY; String overAt = "";
        void see(TroopModel<?> m, double[] reach, String label) {
            double[] x = m.extent(); double o = Phase2Checks.Anim.outside(x, reach); poses++;
            if (o > out) { out = o; at = label; }
            radius = Math.max(radius, x[0]); bottom = Math.min(bottom, x[1]); top = Math.max(top, x[2]);
            for (var e : m.limits().entrySet()) {
                ModelPart p = m.bone(e.getKey()); if (p == null) continue;
                double d = (p.pitch - p.getDefaultTransform().pitch) / TroopModel.RAD, v = Math.max(d - e.getValue()[1], e.getValue()[0] - d);
                if (v > over) { over = v; overAt = e.getKey() + " " + String.format(Locale.ROOT, "%.2f", d) + " deg, " + label; }
            }
            for (String b : List.of("right_shin", "left_shin")) { ModelPart p = m.bone(b); if (p != null) shin = Math.max(shin, (p.pitch - p.getDefaultTransform().pitch) / TroopModel.RAD); }
        }
    }
    static MotionInput in(double now, double limbAngle, double speed, double headYaw, double headPitch, double yawRate, String stance, float aim,
                          boolean shieldUp, boolean enemy, boolean dead, double deathS, MotionInput.Attack a, MotionInput.Hit h) {
        return new MotionInput(now, limbAngle, speed, headYaw, headPitch, yawRate, stance, aim, shieldUp, enemy, dead, deathS, a, h, MAX_SEED);
    }
    static void run(TroopModel<?> m, double[] reach, Worst w, String label, int frames, boolean shield, Frames f) {
        MotionEngine.State st = new MotionEngine.State();
        for (int i = 0; i < frames; i++) { TroopMotion.simulate(m, st, f.at(i, T0 + i * .05), shield); w.see(m, reach, label + " at frame " + i); }
    }

    /** Every pose mo-cull measures of one model (the method comment of cull), each seen by w. */
    static void sweep(TroopModel<?> m, TroopMotion.Fam fam, double[] reach, Worst w) {
        for (var e : fam.all().entrySet()) for (boolean sh : new boolean[]{false, true}) {
            double len = e.getValue().length();
            for (int i = 0; i * .05 <= len + 1e-9; i++) {
                m.getPart().traverse().forEach(ModelPart::resetTransform);
                TroopMotion.applyForced(m, e.getKey(), i * .05, sh); w.see(m, reach, "forced " + e.getKey() + " at " + String.format(Locale.ROOT, "%.2f", i * .05) + " s" + (sh ? " with a shield" : ""));
            }
        }
        long tick0 = Math.round(T0 * Clip.TICKS) + 2;
        int attacks = fam.family().attacks().size();
        double[][] ctx = {{0, 0}, {1, 0}, {1.5, 0}, {0, 720}}; String[] ctxName = {"standing", "walking", "running", "turning"};
        for (int a = 0; a < attacks; a++) for (int k = 0; k < ctx.length; k++) for (boolean sh : new boolean[]{false, true}) {
            final int ai = a; final double sp = ctx[k][0], yr = ctx[k][1];
            run(m, reach, w, "attack " + fam.family().attacks().get(a).name() + " " + ctxName[k] + (sh ? " with a shield" : ""), 50, sh,
                (i, now) -> in(now, i * sp, sp, 85, 60, yr, "hold", 0, false, true, false, 0, new MotionInput.Attack(ai, tick0), null));
        }
        for (String zone : List.of("head", "body", "legs")) for (boolean blocked : new boolean[]{false, true}) for (boolean sh : new boolean[]{false, true}) {
            var hit = new MotionInput.Hit("slash", zone, 0f, 2, blocked, false, tick0);
            run(m, reach, w, "hit " + zone + (blocked ? " blocked" : "") + (sh ? " with a shield" : ""), 30, sh,
                (i, now) -> in(now, 0, 0, 85, 60, 0, "hold", 0, blocked, true, false, 0, null, hit));
        }
        for (float bearing : new float[]{0, 90, 180, -90}) for (boolean sh : new boolean[]{false, true}) {
            var hit = new MotionInput.Hit("slash", "body", bearing, 2, false, true, tick0);
            run(m, reach, w, "death from " + bearing + (sh ? " with a shield" : ""), 80, sh,
                (i, now) -> in(now, 0, 0, 0, 0, 0, "hold", 0, false, i < 4, i >= 4, i >= 4 ? (i - 4) * .05 : 0, null, hit));
        }
        for (String s : BattleMath.STANCES) for (float aim : new float[]{-60, 60}) for (boolean up : new boolean[]{false, true}) {
            double sp = s.equals("run") || s.equals("charge") || s.equals("fall_back") || s.equals("shield_move") ? 1 : 0;
            run(m, reach, w, "stance '" + s + "' aim " + aim + (up ? " shield up" : ""), 30, up,
                (i, now) -> in(now, i * sp, sp, -85, -60, 0, s, aim, up, up, false, 0, null, null));
        }
        m.getPart().traverse().forEach(ModelPart::resetTransform);
    }

    /**
     * mo-cull (Task 13 step 1): every clip's posed bounds lie inside the cull box. No clip assets ship yet, so synthetic clips with
     * large moves (every attack variant of each family, the common stances, hits, block, deaths and scene clips) are installed in
     * ClipLibrary and the cull reach worked out again from them. Each troop model's extent (TroopModel.extent) is measured against
     * its cull reach: every clip forced whole-body at full weight every 0.05 s, shield down and up; then engine sequences on the
     * largest seed (attacks standing, walking, running and turning at 720 degrees a second, with the head at its limits; every
     * hit zone at weight 2, blocked and not; deaths from the front, back and both sides; every server stance with the aim at
     * +-60 and the shield both ways). The real clips go back after. mo-limits (carry C13, R13): over the same poses no bone with a joint
     * limit (the mage's shins, [0, 10] degrees) goes past it, and with TroopMotion.noLimits on the same poses do (the red side).
     */
    static int synthCount;
    static void cull() {
        Map<String, Clip> before = ClipLibrary.all(); List<String> bad = new ArrayList<>(); StringBuilder sb = new StringBuilder(); int poses = 0, models = 0;
        StringBuilder lim = new StringBuilder(); List<String> limBad = new ArrayList<>(); int limModels = 0; double shinFree = Double.NEGATIVE_INFINITY; String shinFreeAt = "";
        double slackMin = Double.POSITIVE_INFINITY, slackMax = Double.NEGATIVE_INFINITY, sideMax = Double.NEGATIVE_INFINITY; String slackMinAt = "", slackMaxAt = "", sideMaxAt = "";
        double analyticMax = Double.NEGATIVE_INFINITY; String analyticMaxAt = "";
        try {
            // synthetic clips for whatever has no real one yet; every real clip (Task 16 on: sword_shield and common) wins (R54)
            Map<String, Clip> synth = new TreeMap<>();
            for (String t : TroopModels.ids()) { var m = TroopModels.model(t); var row = m == null ? null : TroopMotion.row(m); if (row != null) synthetic(m.rig, row.family(), synth); }
            synth.putAll(before); synthCount = 0; for (String k : synth.keySet()) if (!before.containsKey(k)) synthCount++;
            ClipLibrary.install(synth);
            for (String t : new TreeSet<>(TroopModels.ids())) {
                var m = TroopModels.model(t); var row = m == null ? null : TroopMotion.row(m); var fam = m == null ? null : TroopMotion.family(m);
                if (row == null || fam == null) continue;
                models++;
                double[] reach = m.cullReach(); Worst w = new Worst();
                sweep(m, fam, reach, w);
                if (!m.limits().isEmpty()) {
                    // carry C13's red side: the same poses with the joint limits off must go past them, or the check proves nothing
                    Worst raw = new Worst(); TroopMotion.noLimits = true;
                    try { sweep(m, fam, reach, raw); } finally { TroopMotion.noLimits = false; }
                    lim.append(String.format(Locale.ROOT, "%s: %d poses, worst %.3f deg past a limit (%s), deepest shin %.2f; with the limits off %.3f deg past (%s); ",
                        t, w.poses, w.over, w.overAt, w.shin, raw.over, raw.overAt));
                    if (!(w.over <= 1e-3) || !(raw.over > 1)) limBad.add(t + (w.over > 1e-3 ? " goes " + String.format(Locale.ROOT, "%.3f", w.over) + " deg past at " + w.overAt : " never needs its limit (red side " + String.format(Locale.ROOT, "%.3f", raw.over) + ")"));
                    limModels++;
                } else if (w.shin > shinFree) { shinFree = w.shin; shinFreeAt = t; }
                poses += w.poses;
                // the slack: how far inside the box the pose that came closest stays (-out), and each side's own (the box less the farthest pose on that side)
                double sr = reach[0] - w.radius, sbot = w.bottom - reach[1], stop = reach[2] - w.top, side = Math.max(sr, Math.max(sbot, stop));
                if (-w.out < slackMin) { slackMin = -w.out; slackMinAt = t; }
                if (-w.out > slackMax) { slackMax = -w.out; slackMaxAt = t; }
                if (side > sideMax) { sideMax = side; sideMaxAt = t; }
                // M2's red side: the same poses against the analytic reach (every bone turned any way) the sampled bounds replace
                double[] ar = m.analyticReach(); double aSlack = Math.min(ar[0] - w.radius, Math.min(w.bottom - ar[1], ar[2] - w.top));
                if (aSlack > analyticMax) { analyticMax = aSlack; analyticMaxAt = t; }
                sb.append(String.format(Locale.ROOT, "%s %d poses, reach r%.2f %.2f..%.2f, posed r%.2f %.2f..%.2f, worst %.3f blocks %s (%s), slack %.3f (radius %.3f, bottom %.3f, top %.3f); ",
                    t, w.poses, reach[0], reach[1], reach[2], w.radius, w.bottom, w.top, w.out, w.out <= 0 ? "inside" : "OUTSIDE", w.at, -w.out, sr, sbot, stop));
                if (!(w.out <= 0)) bad.add(t + " reaches " + String.format(Locale.ROOT, "%.3f", w.out) + " blocks outside at " + w.at);
            }
        } catch (RuntimeException x) { bad.add("threw " + x); x.printStackTrace(); }
        finally {
            ClipLibrary.install(before);
            for (String t : TroopModels.ids()) { var m = TroopModels.model(t); if (m != null) m.getPart().traverse().forEach(ModelPart::resetTransform); }
        }
        report("mo-limits", limBad.isEmpty() && limModels > 0, limModels + " troop models with joint limits (carry C13, R13): " + lim + "deepest shin bend on a model without limits "
            + String.format(Locale.ROOT, "%.2f", shinFree) + " deg (" + shinFreeAt + ")" + (limBad.isEmpty() ? "" : "; failing: " + limBad));
        report("mo-cull", bad.isEmpty() && models > 0 && poses > 0 && slackMax <= 1.0, models + " troop models, " + poses + " posed bounds against cullBox (" + before.size() + " real clips, " + synthCount + " synthetic for the rest; the real clips put back): " + sb + (bad.isEmpty() ? "every pose inside" : "outside: " + bad)
            + String.format(Locale.ROOT, "; slack (blocks between the pose that came closest and the box, the margin %.2f included): least %.3f (%s), most %.3f (%s); the loosest single side %.3f (%s)",
                TroopMotion.CULL_MARGIN, slackMin, slackMinAt, slackMax, slackMaxAt, sideMax, sideMaxAt)
            + String.format(Locale.ROOT, " (R107: the most slack at most 1.0); against the analytic reach the sampled bounds replace (M2's red side) the most slack would be %.3f (%s); the sampled bounds (TroopMotion.bounds, M2) took at most %.1f ms a model", analyticMax, analyticMaxAt, TroopMotion.boundsMs));
    }

    /**
     * The live checks (Task 13 step 1), on a platform of their own: 20 knights in a 5 by 4 grid 1.5 blocks apart on Hold, the camera
     * 30 blocks in front of them. mo-stance-live: each knight's client stance is the one its server keeps, by the plain-hold rule
     * (a shield troop braces, or shows the wall formed). mo-crowd: with the real clips, after 60 ticks to settle, 80 ticks of frames
     * in which every knight drew a new pose; for each pair of knights the RMS of the angle between their poses over every bone
     * rotation the engine moves (channels that never move on any knight are left out, and the RMS over every channel is logged
     * too) must be over 1 degree (on GeckoLib the mean over every pair must be over 0.3: decision 9, a phase offset only); mo-crowd.png is the view. mo-late-join: a synthetic swing (the knight family's first variant, 1.6
     * s) is sent to the front knight; once its clip time is past impactTicks + 12 ticks (where the old impactTicks + 10 catch-up window
     * had closed: carry B8, fix round 1 I2) and at least 4 ticks before the clip ends, this client forgets the knight as an unload would
     * (MotionEvents.Client.forget and TroopMotion.forget) and the server sends what a player who starts tracking it is owed
     * (MotionEvents.startTracking). The swing must come back within 5 ticks and play, every tick until it ends, within 2 ticks of the
     * clip time the first client's track shows; a second catch-up of the same swing 3 ticks later must not be kept twice (carry B6).
     * Then, 2 ticks past the clip's end (inside the server's ATTACK_CLIP_MAX_TICKS), the client forgets the knight again: for 10 ticks
     * no track of the swing may play (the client skips a swing whose clip has ended).
     * <p>GeckoLib (GeoTroopRenderer, the knight first): TroopRenderer returns before its draw record and TroopMotion for a Geo troop, so
     * mo-crowd reads a Geo knight's draws and bone pose from GeoDraws (the baked model's bones, laid out as TroopModel.snapshot), and
     * mo-late-join reads a Geo knight's attack layer (AttackController.LIVE: the swing it shows, its clip time against the world clock it
     * plays on) with the knight's own clip in place of a synthetic one; see lateJoinGeo. mo-settle, mo-light, mo-fade-overlap and mo-fade
     * measure BodyCache (a settled body drawn from its recorded buffers, its per-part fade switches, its traced paths) and TroopMotion's death
     * pick, which only the legacy renderer has: they run on BODY, the first of the knight, the sentinel, the Marshal and the hearthguard still
     * drawn the legacy way (with a Geo knight, an extra one stands since the cast as mo-settle's old body), and say which.
     */
    static final class Live extends Phase2Checks.Stage {
        static final int COLS = 5, ROWS = 4; static final double GAP = 1.5, CAM_DZ = -30, CAM_EYE = 6; static final float YAW = 180;
        final boolean chained; volatile boolean done;
        CloudRenderMode cloudsBefore; Map<String, Clip> clipsBefore;
        int[] lastCount; final List<float[][]> samples = new ArrayList<>(); int steady;
        int ls; Clip synth; MotionStack.Track orig; int forgetWait, backAt = -1, compared, maxTracks, maxQueue; boolean dupSent, lateReal; double maxDiff;
        /** Fix round 1, I2: the variant's impactTicks, the clip time (s) of the forget, and the probe past the clip's end (its ticks, the tracks and queued swings it saw). */
        int impactTicks, probeWait, probeTracks = -1, probeQueued; double forgetAt = -1, probeAt = -1; String lateWhy = "";
        Live(boolean chained) { this.chained = chained; }
        /** The troop the BodyCache checks strike down (see the class comment); null when every candidate is drawn by GeckoLib. */
        volatile String body;
        /** With a Geo knight: mo-settle's old body, the BODY troop standing off the grid since the cast; null when the grid's knights serve. */
        GravesAndGlory.Unit standOld;
        boolean standIn() { return body != null && !body.equals("knight"); }
        /** The 20 knights of the grid (posted first). */
        List<GravesAndGlory.Unit> grid() { return posted.subList(0, Math.min(posted.size(), COLS * ROWS)); }
        /** What the BodyCache checks say of their troop. */
        String bodyNote() { return body == null ? "" : " [" + body + ", drawn the legacy way: BodyCache and TroopMotion's death pick are legacy-only" + (GeoDraws.geo("knight") ? "; the knight is GeckoLib" : "") + "]"; }
        /** An entity's draws so far and its last pose, from whichever renderer drew it (GeoDraws for a GeckoLib troop, TroopRenderer.drawn else). */
        static int drawCount(Entity e) { if (GeoDraws.geo(e)) return GeoDraws.count(e); var d = TroopRenderer.drawn(e); return d == null ? 0 : d.count; }
        static float[] lastPose(Entity e) { if (GeoDraws.geo(e)) { var g = GeoDraws.drawn(e); return g == null ? null : g.pose; } var d = TroopRenderer.drawn(e); return d == null ? null : d.pose; }
        String name() { return chained ? "motion" : "motion-live"; }
        static float pitch() { return (float) Math.toDegrees(Math.atan2(CAM_EYE - 1.5, -CAM_DZ + (ROWS - 1) * GAP / 2)); }

        void cast(GravesAndGlory.Battle b) {
            for (int r = 0; r < ROWS; r++) for (int k = 0; k < COLS; k++) post(b, gold, "knight", (k - (COLS - 1) / 2.0) * GAP, r * GAP, YAW);
            if (standIn()) standOld = post(b, gold, body, -(COLS + 1) * GAP, 0, YAW); // mo-settle's old body: stands from the cast, off the grid
            camera(b, 0, CAM_EYE, CAM_DZ, 0, pitch());
        }
        void end(MinecraftClient c) {
            if (!chained) { super.end(c); return; }
            c.options.hudHidden = false; finished = true; done = true;
        }
        boolean there(MinecraftClient c) {
            var p = c.player;
            return Math.abs(p.getX() - (site.getX() + .5)) < .05 && Math.abs(p.getEyeY() - (site.getY() + CAM_EYE)) < .05 && Math.abs(p.getZ() - (site.getZ() + .5 + CAM_DZ)) < .05
                && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw())) < 1 && Math.abs(p.getPitch() - pitch()) < 1;
        }

        public void step(MinecraftClient c, int tick) {
            if (tick == 1) { stanceRule(); cull(); GeoDraws.install(); body = GeoDraws.legacy("knight", "sentinel", "gl_marshal", "ic_hearthguard"); } // the models are loaded by now
            if (tick == 20) { c.setScreen(null); var id = c.player.getUuid(); server(c, b -> { player = id; stage = 1; }); }
            if (finished) return;
            if (tick > 12000) { report("p2-motion-live-timeout", false, "stuck at client step " + cs + " (late join step " + ls + "), server stage " + stage); end(c); return; }
            switch (cs) {
                case 0 -> {
                    if (litStart(c) && ready && posted.size() == COLS * ROWS + (standIn() ? 1 : 0) && posted.stream().allMatch(u -> client(c, u.mob) != null)) {
                        c.options.hudHidden = true; cloudsBefore = c.options.getCloudRenderMode().getValue(); c.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                        wait = 0; cs = 1;
                    } else if (++wait % 600 == 0) System.out.println("[p2-wait] motion-live step 0 after " + wait + " ticks: ready=" + ready + ", posted " + posted.size() + "; " + litWhy(c));
                }
                case 1 -> stances(c);
                case 2 -> crowd(c);
                case 3 -> {
                    if (!there(c) && ++wait % 60 == 0) server(c, b -> camera(b, 0, CAM_EYE, CAM_DZ, 0, pitch()));
                    if (!there(c) && wait < 300) return;
                    if (!shotReady(c, "mo-crowd")) return;
                    Rig.save(c, "mo-crowd"); wait = 0; cs = 4;
                }
                case 4 -> lateJoin(c);
                case 5 -> settle(c);
                default -> {}
            }
        }

        /** mo-stance-live: every knight's client stance equals its server's, which the plain-hold rule gives (brace, or the wall formed). */
        void stances(MinecraftClient c) {
            wait++;
            List<String> off = new ArrayList<>(); Map<String, Integer> seen = new TreeMap<>();
            for (var u : grid()) {
                var e = client(c, u.mob); String server = u.stance, client = e == null ? "?" : MotionEvents.Client.stance(e.getId()).name();
                boolean sh = u.tags.contains("shield"), sp = u.tags.contains("spear");
                boolean rule = server.equals(BattleMath.holdStance(sh, sp, false)) || server.equals(BattleMath.holdStance(sh, sp, true));
                seen.merge(client, 1, Integer::sum);
                if (!client.equals(server) || !rule) off.add("#" + u.mob.getId() + " client '" + client + "' server '" + server + "'");
            }
            steady = off.isEmpty() ? steady + 1 : 0;
            if (steady >= 10 || wait > 300) {
                report("mo-stance-live", steady >= 10, grid().size() + " knights on Hold, client stances " + seen + (off.isEmpty() ? ", each its server's and the plain-hold rule's for 10 ticks" : " after " + wait + " ticks, off: " + off));
                cs = 2; wait = 0;
            }
        }

        /** mo-crowd: 60 ticks to settle, then up to 80 ticks of frames in which every knight drew a new pose. */
        void crowd(MinecraftClient c) {
            if (++wait <= 60) return;
            int n = grid().size(); if (lastCount == null) lastCount = new int[n];
            float[][] frame = new float[n][]; boolean fresh = true;
            for (int i = 0; i < n; i++) {
                var e = client(c, posted.get(i).mob); int count = e == null ? 0 : drawCount(e); float[] pose = e == null ? null : lastPose(e);
                if (pose == null || count <= lastCount[i]) fresh = false; else frame[i] = pose.clone();
                lastCount[i] = count;
            }
            if (fresh && wait > 61) samples.add(frame);
            if (samples.size() < 80 && wait < 60 + 400) return;
            judgeCrowd(wait - 61);
            cs = 3; wait = 0;
        }
        void judgeCrowd(int ticks) {
            String via = GeoDraws.geo("knight") ? " (GeckoLib: GeoDraws, the baked bones)" : " (TroopRenderer.drawn)";
            if (samples.size() < 60) { report("mo-crowd", false, "only " + samples.size() + " frames in " + ticks + " ticks had a new pose from all " + grid().size() + " knights" + via + " (want 60 or more)"); return; }
            int n = samples.get(0).length, len = samples.get(0)[0].length;
            boolean[] moves = new boolean[len];
            for (int k = 0; k < len; k++) {
                if (k % 6 < 3) continue;
                float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
                for (float[][] s : samples) for (float[] pose : s) { lo = Math.min(lo, pose[k]); hi = Math.max(hi, pose[k]); }
                moves[k] = Math.toDegrees(hi - lo) > 0.01;
            }
            int moving = 0; for (boolean b : moves) if (b) moving++;
            double minMove = Double.MAX_VALUE, minAll = Double.MAX_VALUE, sumMove = 0; int pairs = 0; String closest = "";
            for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++) {
                double all = 0, mv = 0; int na = 0, nm = 0;
                for (float[][] s : samples) for (int k = 0; k < len; k++) {
                    if (k % 6 < 3) continue;
                    double d = net.minecraft.util.math.MathHelper.wrapDegrees(Math.toDegrees(s[i][k] - s[j][k]));
                    all += d * d; na++;
                    if (moves[k]) { mv += d * d; nm++; }
                }
                double rAll = Math.sqrt(all / Math.max(1, na)), rMove = nm == 0 ? 0 : Math.sqrt(mv / nm);
                if (rMove < minMove) { minMove = rMove; closest = "knights " + i + " and " + j; }
                minAll = Math.min(minAll, rAll); sumMove += rMove; pairs++;
            }
            // Geo (Kort 2026-11-12, decision 9, via the animation session: a per-knight phase offset only, no posture variety): two random phases can
            // land together, so the GeckoLib crowd passes on the mean pair RMS over 0.3 degrees (lockstep reads 0.00, the offset 0.70 to 0.75);
            // the legacy path, whose idles carry per-troop variety, keeps every pair over 1 degree
            boolean geo = GeoDraws.geo("knight"); double mean = sumMove / Math.max(1, pairs);
            boolean pass = geo ? mean > 0.3 : minMove > 1;
            report("mo-crowd", pass, String.format(Locale.ROOT, "%d knights" + via + " idle on Hold, %d frames over %d ticks (%.1f s) with every knight newly drawn; %d of %d bone rotation channels move; pair RMS over the moving channels: least %.2f degrees (%s), mean %.2f; over every rotation channel: least %.2f; %s",
                n, samples.size(), ticks, ticks / 20.0, moving, len / 2, minMove, closest, sumMove / pairs, minAll, geo ? (pass ? "mean over 0.3 degrees (GeckoLib rule)" : "mean 0.3 degrees or less: lockstep (GeckoLib rule)") : (pass ? "no two within 1 degree" : "two within 1 degree")));
        }

        /** mo-late-join, in steps: the swing sent; its track on this client; the forget and the catch-up; the comparison each tick. */
        void lateJoin(MinecraftClient c) {
            var u = posted.get(0); var e = client(c, u.mob);
            if (e == null) { report("mo-late-join", false, "the front knight has no client entity"); afterLate(); return; }
            if (GeoDraws.geo(e)) { lateJoinGeo(c, u, e); return; }
            int id = e.getId(); wait++;
            switch (ls) {
                case 0 -> {
                    var m = TroopModels.model("knight"); var row = m == null ? null : TroopMotion.row(m);
                    var rows = row == null ? List.<Spec.AttackVariantsRow>of() : Tactics.variants(row.family());
                    if (rows.isEmpty()) { report("mo-late-join", false, "the knight has no attack variants"); afterLate(); return; }
                    var v = rows.get(0); double impact = v.impactTicks() / (double) Clip.TICKS; impactTicks = v.impactTicks();
                    // the real clip when the family has one (Task 16, R54: the sword_shield thrust), else a synthetic 1.6 s swing
                    clipsBefore = ClipLibrary.all(); String key = m.rig + "/" + row.family() + "/" + v.id(); Clip real = clipsBefore.get(key);
                    // the forget comes once the clip time is past impactTicks + 12 ticks and the comparison needs 5 ticks after it with 4 to spare: a real swing shorter than impactTicks + 20 ticks leaves no window (the knight's 1.25 s thrust, impact 7, had 2), so a synthetic one stands in
                    if (real != null && real.length() * Clip.TICKS < v.impactTicks() + 20) real = null;
                    synth = real != null ? real : attack(v.id(), impact, 1.6, ATTACK_PEAKS); lateReal = real != null;
                    Map<String, Clip> map = new TreeMap<>(clipsBefore); map.put(key, synth);
                    // the knight holds in the wall or brace, where the engine plays the family's <id>_wall strike (Task 16), so that one is the swing the first client shows
                    if (clipsBefore.containsKey(key + "_wall")) map.put(key + "_wall", synth);
                    ClipLibrary.install(map);
                    server(c, b -> { u.lastVariant = 0; u.catchVariant = 0; u.swingTick = b.world.getTime(); MotionEvents.sendAttack(u, 0, u.swingTick); });
                    ls = 1; wait = 0;
                }
                case 1 -> {
                    var st = TroopMotion.state(id);
                    if (st != null) for (var tr : st.tracks) if (tr.kind == MotionEngine.ATTACK && tr.a == synth) orig = tr;
                    if (orig != null) { ls = 2; wait = 0; return; }
                    if (wait > 60) { report("mo-late-join", false, "the swing never played on the first client (60 ticks)"); afterLate(); }
                }
                case 2 -> {
                    // forget once the old impactTicks + 10 window would have closed (12 ticks past the impact), with 4 ticks of the clip left
                    var st = TroopMotion.state(id); double after = (impactTicks + 12) / (double) Clip.TICKS, latest = synth.length() - 4.0 / Clip.TICKS;
                    double t = st == null ? -1 : orig.time(st.frame.now);
                    if (t > latest) { report("mo-late-join", false, String.format(Locale.ROOT, "the forget came too late: clip time %.3f s, past the clip's end less 4 ticks (%.3f s)", t, latest)); afterLate(); return; }
                    if (!(t > after)) { if (wait > 60) { report("mo-late-join", false, "the swing never passed impactTicks + 12 ticks"); afterLate(); } return; }
                    forgetAt = t;
                    MotionEvents.Client.forget(id); TroopMotion.forget(id);
                    server(c, b -> { var p = sp(b); if (p != null) MotionEvents.startTracking(u, p); });
                    ls = 3; wait = 0; forgetWait = 0;
                }
                case 3 -> {
                    forgetWait++;
                    var st = TroopMotion.state(id);
                    if (st != null) {
                        double now = st.frame.now; int tracks = 0;
                        for (var tr : st.tracks) if (tr.kind == MotionEngine.ATTACK && tr.a == synth) tracks++;
                        maxTracks = Math.max(maxTracks, tracks); maxQueue = Math.max(maxQueue, MotionEvents.Client.attacks(id).size());
                        for (var p : MotionEngine.playing(st, now)) if (p.clip() == synth) {
                            if (backAt < 0) backAt = forgetWait;
                            maxDiff = Math.max(maxDiff, Math.abs(p.time() - orig.time(now))); compared++; break;
                        }
                        if (backAt >= 0 && !dupSent && forgetWait >= backAt + 3) { dupSent = true; server(c, b -> { var p = sp(b); if (p != null) MotionEvents.catchUpAttack(u, p); }); }
                        if (orig.time(now) >= synth.length() - .05 || forgetWait > 60) { ls = 4; wait = 0; }
                    } else if (forgetWait > 60) { ls = 4; wait = 0; }
                }
                case 4 -> {
                    // the probe past the clip's end: 2 ticks after it, while the server still holds the swing current (ATTACK_CLIP_MAX_TICKS)
                    double now = c.world.getTime() / (double) Clip.TICKS;
                    if (orig.time(now) < synth.length() + 2.0 / Clip.TICKS) { if (wait > 60) { lateWhy = "the clip's end never came"; judgeLate(c); } return; }
                    probeAt = orig.time(now);
                    MotionEvents.Client.forget(id); TroopMotion.forget(id);
                    server(c, b -> { var p = sp(b); if (p != null) MotionEvents.startTracking(u, p); });
                    ls = 5; probeWait = 0; probeTracks = 0;
                }
                case 5 -> {
                    probeWait++;
                    var st = TroopMotion.state(id);
                    if (st != null) for (var tr : st.tracks) if (tr.kind == MotionEngine.ATTACK && tr.a == synth) probeTracks++;
                    probeQueued = Math.max(probeQueued, MotionEvents.Client.attacks(id).size());
                    if (probeWait >= 10) judgeLate(c);
                }
                default -> {}
            }
        }
        // ---- mo-late-join on a GeckoLib knight ----
        /** The swing's variant, its clip (name, length and impact marker, ticks), its start tick on the world clock, and the attack layer's last frame read. */
        int gVariant = -1; String gClip = ""; double gLen = -1, gImpact = -1, gLastAt = -1; long gStart = Long.MIN_VALUE; boolean gRate1 = true; int gProbeQueued = -1;
        /**
         * mo-late-join for a Geo knight. GeckoLib plays the swing on the world clock from its start tick (AttackController), not on a
         * TroopMotion track, and its clips are the knight's own: no synthetic clip can stand in, so the variant whose clip leaves the most room
         * past the old impactTicks + 10 window is sent (the knight's 22-tick thrust, impact 7, leaves ticks 18 and 19). The first client must
         * show it (the attack layer's newest swing, weight above 0); once its clip time is past impactTicks + 10 and with 3 ticks or more of the
         * clip left, the client forgets the knight and the server sends what a late tracker is owed (MotionEvents.startTracking). Every frame
         * the swing shows again, its clip time must be within 2 ticks of the world clock's (now - start, the time the first client showed),
         * at rate 1 (no late wind-up quickening past the impact); it must be back within 5 ticks and before its clip ends, and kept once after a
         * repeated catch-up (queued at most once). 2 ticks past its clip's end, forgotten and caught up again (the server still holds it
         * current), no frame may show it for 10 ticks.
         */
        void lateJoinGeo(MinecraftClient c, GravesAndGlory.Unit u, Entity e) {
            int id = e.getId(); wait++;
            AttackController ac = AttackController.LIVE.get(e);
            double now = c.world.getTime(), raw = gStart == Long.MIN_VALUE ? -1 : now - gStart;
            boolean fresh = ac != null && ac.shownAt > gLastAt; if (fresh) gLastAt = ac.shownAt;
            boolean showing = fresh && ac.shownWeight > 0 && ac.shownStart == gStart && gClip.equals(ac.shownClip);
            int queued = 0; for (var a : MotionEvents.Client.attacks(id)) if (a.startTick() == gStart && a.variant() == gVariant) queued++;
            switch (ls) {
                case 0 -> {
                    String family = u.spec.family(); var rows = Tactics.variants(family);
                    var anims = software.bernie.geckolib.cache.GeckoLibCache.getBakedAnimations().get(new net.minecraft.util.Identifier("gravesandglory", "animations/troop/knight.animation.json"));
                    double best = Double.NEGATIVE_INFINITY;
                    for (int v = 0; v < rows.size(); v++) {
                        var clip = anims == null ? null : anims.getAnimation(AttackController.clipName(family, rows.get(v).id()));
                        if (clip == null) continue;
                        double imp = AttackController.impact(clip) >= 0 ? AttackController.impact(clip) : rows.get(v).impactTicks();
                        double room = clip.length() - 3 - (imp + 10);
                        if (room > best) { best = room; gVariant = v; gClip = clip.name(); gLen = clip.length(); gImpact = imp; impactTicks = rows.get(v).impactTicks(); }
                    }
                    if (gVariant < 0) { report("mo-late-join", false, "the GeckoLib knight has no attack clip for its " + family + " variants"); afterLate(); return; }
                    final int vi = gVariant;
                    server(c, b -> { u.lastVariant = vi; u.catchVariant = vi; u.swingTick = b.world.getTime(); MotionEvents.sendAttack(u, vi, u.swingTick); });
                    ls = 1; wait = 0;
                }
                case 1 -> {
                    if (gStart == Long.MIN_VALUE) for (var a : MotionEvents.Client.attacks(id)) if (a.variant() == gVariant) gStart = a.startTick();
                    if (gStart != Long.MIN_VALUE && fresh && ac.shownWeight > 0 && ac.shownStart == gStart && gClip.equals(ac.shownClip)) { ls = 2; wait = 0; return; }
                    if (wait > 60) { report("mo-late-join", false, "the swing never played on the first client (60 ticks; GeckoLib attack layer " + (ac == null ? "never drawn" : "showing " + ac.shownClip + " weight " + ac.shownWeight) + ")"); afterLate(); }
                }
                case 2 -> {
                    double after = impactTicks + 10, latest = gLen - 3;
                    if (raw > latest) { report("mo-late-join", false, String.format(Locale.ROOT, "the forget came too late (GeckoLib): clip time %.0f ticks, past the clip's end less 3 ticks (%.0f)", raw, latest)); afterLate(); return; }
                    if (!(raw > after)) { if (wait > 60) { report("mo-late-join", false, "the swing never passed impactTicks + 10 ticks (GeckoLib)"); afterLate(); } return; }
                    forgetAt = raw;
                    MotionEvents.Client.forget(id); TroopMotion.forget(id);
                    server(c, b -> { var p = sp(b); if (p != null) MotionEvents.startTracking(u, p); });
                    ls = 3; wait = 0; forgetWait = 0;
                }
                case 3 -> {
                    forgetWait++;
                    maxQueue = Math.max(maxQueue, queued);
                    if (showing) {
                        if (backAt < 0) backAt = forgetWait;
                        maxDiff = Math.max(maxDiff, Math.abs(ac.shownT - (ac.shownAt - gStart - ac.shownLead))); compared++;
                        gRate1 &= Math.abs(ac.shownRate - 1) < 1e-6;
                    }
                    if (backAt >= 0 && !dupSent && forgetWait >= backAt + 1) { dupSent = true; server(c, b -> { var p = sp(b); if (p != null) MotionEvents.catchUpAttack(u, p); }); }
                    if (raw >= gLen || forgetWait > 60) { ls = 4; wait = 0; }
                }
                case 4 -> {
                    maxQueue = Math.max(maxQueue, queued);
                    if (raw < gLen + 2) { if (wait > 60) { lateWhy = "the clip's end never came"; judgeLateGeo(); } return; }
                    probeAt = raw;
                    MotionEvents.Client.forget(id); TroopMotion.forget(id);
                    server(c, b -> { var p = sp(b); if (p != null) MotionEvents.startTracking(u, p); });
                    ls = 5; probeWait = 0; probeTracks = 0; gProbeQueued = 0;
                }
                case 5 -> {
                    probeWait++;
                    if (showing) probeTracks++;
                    gProbeQueued = Math.max(gProbeQueued, queued);
                    if (probeWait >= 10) judgeLateGeo();
                }
                default -> {}
            }
        }
        void judgeLateGeo() {
            boolean pass = lateWhy.isEmpty() && forgetAt > impactTicks + 10 && backAt >= 0 && backAt <= 5 && maxDiff <= 2 && gRate1 && maxQueue == 1 && compared >= 2 && dupSent
                && probeTracks == 0 && probeWait >= 10;
            report("mo-late-join", pass, String.format(Locale.ROOT, "GeckoLib knight (AttackController, the world clock): its clip %s (%.0f ticks, impact marker %.0f, variant impactTicks %d) forgotten and caught up %.0f ticks in, where the old impactTicks + 10 window had closed at tick %d: back %s (want 5 or less, before the clip ends), shown on %d frames read, largest gap between its clip time and the world clock's %.2f ticks (want 2 or less), rate 1 throughout %b; queued at most %d after a repeated catch-up %b (want 1); forgotten again %s: %d frames showed the ended swing over %d ticks (want 0 over 10), the catch-up queued it %d times%s",
                gClip, gLen, gImpact, impactTicks, forgetAt, impactTicks + 10, backAt < 0 ? "never" : "after " + backAt + " ticks", compared, maxDiff, gRate1, maxQueue, dupSent,
                probeAt < 0 ? "never" : String.format(Locale.ROOT, "%.0f ticks in, past the clip's end", probeAt), probeTracks, probeWait, gProbeQueued, lateWhy.isEmpty() ? "" : "; " + lateWhy));
            afterLate();
        }
        void judgeLate(MinecraftClient c) {
            int at = (int) Math.round(forgetAt * Clip.TICKS), probe = (int) Math.round(probeAt * Clip.TICKS);
            boolean pass = lateWhy.isEmpty() && forgetAt > (impactTicks + 10) / (double) Clip.TICKS && backAt >= 0 && backAt <= 5 && maxDiff <= 2.0 / Clip.TICKS && maxTracks == 1 && maxQueue == 1 && compared >= 5 && dupSent
                && probeTracks == 0 && probeWait >= 10;
            report("mo-late-join", pass, (lateReal ? "the real clip " + synth.name() + ": " : "a synthetic clip: ") + String.format(Locale.ROOT, "a %.2f s swing (impact at %d ticks) forgotten and caught up %.3f s (%d ticks) in, where the old impactTicks + 10 window had closed at tick %d: back %s, compared on %d ticks, largest gap to the first client's clip time %.3f s (%.1f ticks; want 2 or less); swing tracks at most %d, queued swings at most %d after a repeated catch-up (want 1 and 1); forgotten again %s: %d ticks of tracks of the ended swing over %d ticks (want 0 over 10), %d swings queued%s",
                synth.length(), impactTicks, forgetAt, at, impactTicks + 10, backAt < 0 ? "never" : "after " + backAt + " ticks (want 5 or less)", compared, maxDiff, maxDiff * Clip.TICKS, maxTracks, maxQueue,
                probeAt < 0 ? "never" : String.format(Locale.ROOT, "%.3f s (%d ticks) in, past the clip's end", probeAt, probe), probeTracks, probeWait, probeQueued, lateWhy.isEmpty() ? "" : "; " + lateWhy));
            afterLate();
        }
        // ---- mo-settle (carry D20) and mo-light (carry D21, R45) ----
        GravesAndGlory.Unit young, old; int ss, youngAt = -1, oldAt = -1, youngLies = -1, oldLies = -1; double youngDur = -1, oldDur = -1; net.minecraft.util.math.BlockPos lamp;
        /** The frame probe of mo-light: the body, its light before the lamp, and what the first frame with another light drew. */
        static volatile LightProbe PROBE; static boolean listening;
        static final class LightProbe {
            final Entity e; final int before; volatile int frames, changedFrame = -1, now, drew, lastCount = Integer.MIN_VALUE, drewBefore = Integer.MIN_VALUE; volatile boolean counted, cachedBefore;
            LightProbe(Entity e, int before) { this.e = e; this.before = before; }
        }
        @SuppressWarnings({"unchecked", "rawtypes"})
        static int lightOf(MinecraftClient c, Entity e, float td) { net.minecraft.client.render.entity.EntityRenderer r = c.getEntityRenderDispatcher().getRenderer(e); return r.getLight(e, td); }
        static void listen() {
            if (listening) return; listening = true;
            net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.END.register(ctx -> {
                LightProbe p = PROBE; if (p == null || p.changedFrame >= 0) return;
                MinecraftClient c = MinecraftClient.getInstance(); p.frames++;
                int light = lightOf(c, p.e, ctx.tickDelta()), drew = BodyCache.drewLight(p.e);
                var d = TroopRenderer.drawn(p.e); int count = d == null ? -1 : d.count; boolean drawn = count != p.lastCount; p.lastCount = count;
                if (light == p.before) { p.drewBefore = drew; p.cachedBefore = drew == p.before; return; }
                p.changedFrame = p.frames; p.now = light; p.drew = drew; p.counted = drawn;
            });
        }
        /**
         * mo-settle (carry D20): the front row's right knight, which has stood since the cast, and a knight posted now are struck down on the
         * same tick (the new one in its first ticks of age); each is drawn from BodyCache's buffers (it settled) from the client death tick
         * recorded. The new one must settle within 2 ticks of the old one and both before their fade begins. mo-light (carry D21, R45):
         * then a light block (level 15) goes in the settled old knight's block; on the first frame its light differs, the body must draw
         * in the new light: from its buffers recorded in it, or the normal way, which reads it.
         */
        /** Which of six death variants a troop's first death picks (the formula of MotionEngine.death). */
        static int deathPick(net.minecraft.entity.Entity e) {
            var u = e.getUuid();
            return (int) Math.floorMod(MotionMath.mix(MotionMath.seed(u.getMostSignificantBits(), u.getLeastSignificantBits()).variantBias() ^ 0xD6E8FEB86659FD93L), 6L);
        }
        /** The length in ticks of the death clip playing on the client entity (render time, tempo applied); -1 when none yet. */
        static double deathTicks(int id) {
            var st = TroopMotion.state(id);
            if (st != null) for (var tr : st.tracks) if (tr.kind == MotionEngine.DEATH) return (tr.end() - tr.start) * Clip.TICKS;
            return -1;
        }
        void settle(MinecraftClient c) {
            wait++;
            int lie = Spec.COMBATDISCIPLINE[0].corpseLieTicks(), lieEnd = BattleMath.DEATH_FALL_TICKS + lie - 1;
            switch (ss) {
                case 0 -> {
                    if (body == null) { // every candidate draws through GeckoLib: nothing here draws from BodyCache's buffers
                        String why = "not run: the knight, the sentinel, the Marshal and the hearthguard are all drawn by GeckoLib, which never draws from BodyCache (legacy-only)";
                        for (String k : new String[]{"mo-settle", "mo-light", "mo-fade-overlap", "mo-fade", "mo-fade-smoke"}) report(k, false, why);
                        ss = 30; wait = 0; return;
                    }
                    old = standIn() ? standOld : posted.get(COLS - 1); server(c, b -> young = post(b, gold, body, 0, -5, YAW)); ss = 1; wait = 0;
                }
                case 1 -> {
                    if (young == null || client(c, young.mob) == null) { if (wait > 100) { report("mo-settle", false, "the new " + body + " never reached the client"); report("mo-light", false, "not run: mo-settle could not start"); ss = 10; } return; }
                    server(c, b -> { for (var u : List.of(young, old)) { u.mob.timeUntilRegen = 0; u.mob.hurtTime = 0; u.mob.damage(b.world.getDamageSources().generic(), 10000f); } });
                    ss = 2; wait = 0;
                }
                case 2 -> {
                    var ey = client(c, young.mob); var eo = client(c, old.mob);
                    // each body's death clip length in ticks: the clip is picked by the troop's own seed (1.4 to 1.9 s in the clips), so two knights settle apart by their clips' difference
                    if (ey != null && youngDur < 0) youngDur = deathTicks(ey.getId());
                    if (eo != null && oldDur < 0) oldDur = deathTicks(eo.getId());
                    if (ey instanceof net.minecraft.entity.LivingEntity ly) { if (youngLies < 0 && TroopMotion.lies(ly)) youngLies = ly.deathTime; if (youngAt < 0 && BodyCache.cached(ly)) youngAt = ly.deathTime; }
                    if (eo instanceof net.minecraft.entity.LivingEntity lo) { if (oldLies < 0 && TroopMotion.lies(lo)) oldLies = lo.deathTime; if (oldAt < 0 && BodyCache.cached(lo)) oldAt = lo.deathTime; }
                    if ((youngAt >= 0 && oldAt >= 0) || wait > 120) {
                        boolean pass = youngAt >= 0 && oldAt >= 0 && youngAt <= lieEnd && oldAt <= lieEnd && youngDur >= 0 && oldDur >= 0 && Math.abs((youngAt - youngDur) - (oldAt - oldDur)) <= 2;
                        report("mo-settle", pass, String.format(Locale.ROOT, "struck down on the same tick" + bodyNote() + ": a " + body + " %d ticks old at the blow lies from death tick %d and draws from its buffers from death tick %s; one %d ticks old lies from %d and draws from its buffers from %s; want both by %d (the fade starts after; lie %d) and within 2 ticks of each other once each one's death clip length is taken off (%.1f and %.1f ticks); BodyCache: %s",
                            young.mob.age - wait, youngLies, youngAt < 0 ? "never" : String.valueOf(youngAt), old.mob.age - wait, oldLies, oldAt < 0 ? "never" : String.valueOf(oldAt), lieEnd, lie, youngDur, oldDur, BodyCache.diag()));
                        if (oldAt >= 0) ss = 3; else { report("mo-light", false, "not run: the old " + body + "'s body never drew from its buffers" + bodyNote()); ss = 10; }
                        wait = 0;
                    }
                }
                case 3 -> {
                    var eo = client(c, old.mob); if (eo == null) { report("mo-light", false, "the old " + body + "'s body left the client"); ss = 10; return; }
                    listen(); PROBE = new LightProbe(eo, lightOf(c, eo, 1f));
                    var at = net.minecraft.util.math.BlockPos.ofFloored(eo.getPos()); lamp = at;
                    server(c, b -> b.world.setBlockState(at, net.minecraft.block.Blocks.LIGHT.getDefaultState().with(net.minecraft.block.LightBlock.LEVEL_15, 15)));
                    ss = 4; wait = 0;
                }
                case 4 -> {
                    var p = PROBE;
                    if (p.changedFrame < 0 && wait < 40) return;
                    boolean shown = p.changedFrame >= 0 && (p.drew == p.now || p.drew == -1 && p.counted);
                    report("mo-light", shown && p.cachedBefore, String.format(Locale.ROOT, "a settled " + body + "'s body" + bodyNote() + " (death tick %d) drew from its buffers in its light (%s) the frame before a level-15 light block went into its block=%b; the first frame its light read another value (%s; frame %d of the probe) it drew %s=%b",
                        old.mob.deathTime, hex(p.before), p.cachedBefore, p.changedFrame < 0 ? "never, in 40 ticks" : hex(p.now), p.changedFrame,
                        p.drew == -1 ? (p.counted ? "the normal way, which reads the light of the frame" : "nothing") : "from its buffers in light " + hex(p.drew), shown));
                    PROBE = null; ss = 10;
                }
                case 10 -> {
                    var at = lamp; if (at != null) { lamp = null; server(c, b -> b.world.setBlockState(at, net.minecraft.block.Blocks.AIR.getDefaultState())); }
                    server(c, b -> {
                        var foot = at(OX, OZ); var blk = net.minecraft.util.math.BlockPos.ofFloored(foot.x, foot.y + OLIFT - 1, foot.z); barrier = blk;
                        b.world.setBlockState(blk, net.minecraft.block.Blocks.BARRIER.getDefaultState());
                        low = post(b, gold, body, OX, OZ, YAW); var up = foot.add(0, OLIFT, 0);
                        // the death clip is picked from the troop's UUID (MotionEngine.death: variantBias ^ const ^ deaths, mod 6) and the variants lie differently, so
                        // two random knights overlap on screen by anything from nothing to most of the body: post the high one again until it falls as the low one does
                        GravesAndGlory.Unit h = post(b, gold, body, OX, OZ, YAW);
                        for (int tries = 0; tries < 200 && deathPick(h.mob) != deathPick(low.mob); tries++) {
                            final var gone = h; gone.mob.discard(); b.units.remove(gone); posted.remove(gone); pins.removeIf(q -> q.mob() == gone.mob);
                            h = post(b, gold, body, OX, OZ, YAW);
                        }
                        final var hh = h; pins.removeIf(q -> q.mob() == hh.mob); pins.add(new Pin(hh.mob, up, YAW)); pin(hh.mob, up, YAW); high = hh;
                        camera(b, OX, OLIFT + 6, OZ, 0, 90);
                    });
                    ss = 11; wait = 0;
                }
                case 11 -> {
                    if (low == null || high == null || client(c, low.mob) == null || client(c, high.mob) == null || wait < 20) {
                        if (wait > 200) { report("mo-fade-overlap", false, "the two " + body + " bodies never reached the client"); ss = 99; }
                        return;
                    }
                    server(c, b -> { for (var u : List.of(low, high)) { u.mob.timeUntilRegen = 0; u.mob.hurtTime = 0; u.mob.damage(b.world.getDamageSources().generic(), 10000f); } });
                    ss = 12; wait = 0;
                }
                case 12 -> {
                    var eh = client(c, high.mob); var el = client(c, low.mob);
                    if (!(eh instanceof net.minecraft.entity.LivingEntity lh) || !(el instanceof net.minecraft.entity.LivingEntity ll)) { if (wait > 200) { report("mo-fade-overlap", false, "the client lost the two bodies before their mid-fade"); ss = 99; } return; }
                    var row = Spec.COMBATDISCIPLINE[0]; int mid = BattleMath.DEATH_FALL_TICKS + row.corpseLieTicks() + Math.max(1, row.corpseFadeTicks()) / 2;
                    if (lh.deathTime < mid || ll.deathTime < mid) { if (wait > 400) { report("mo-fade-overlap", false, "the bodies never reached their mid-fade"); ss = 99; } return; }
                    oDeath = lh.deathTime; oDeathLow = ll.deathTime; oCached = BodyCache.cached(lh) && BodyCache.cached(ll); oGroupsBefore = BodyCache.fadeGroupsMax;
                    still(c); oPaused = true; ss = 13; wait = 0;
                }
                case 13 -> overlapShots(c);
                case 99 -> { ss = 20; wait = 0; }
                default -> { if (ss >= 20 && ss < 100) fadeParts(c); else finishLive(c); }
            }
        }
        // ---- mo-fade-overlap (carry D14) ----
        /** Where the two stacked knights stand (off the crowd's grid) and how far above the low one the high one lies, on a barrier. */
        static final double OX = 10, OZ = 2; static final int OLIFT = 3;
        /** R62: how far the low body's share through the high one may be from 1 - alpha (0.1 until fix round 1 put four runs within 0.003 of it). */
        static final double OTOL = 0.05;
        GravesAndGlory.Unit low, high; net.minecraft.util.math.BlockPos barrier; int oDeath, oDeathLow; long oGroupsBefore; boolean oPaused, oCached = true; float oAlpha, oAlphaLow, oTd;
        /** Frames: both drawn (F), both in one fade group as before the fix (F0), the low one hidden (HA), the high one hidden (HB), both hidden (HAB), both opaque (O), the low one opaque alone (OL), the high one opaque alone (OH). */
        final net.minecraft.client.texture.NativeImage[] oShots = new net.minecraft.client.texture.NativeImage[8];
        static final String[] O_NAMES = {"f", "f0", "ha", "hb", "hab", "o", "ol", "oh"};
        /** R61/R62: each shot's partial tick and the two bodies' fading draws (BodyCache.traced). */
        final String[] oTrace = new String[8];
        /**
         * mo-fade-overlap (carry D14): a knight lying on a barrier 3 blocks over another, the camera straight above, both struck down on the
         * same tick, the game paused (R60: no menu, no HUD) at their mid-fade (alpha a, BattleMath.corpseAlpha at their death tick and the
         * frame's partial tick). Over the pixels both bodies cover (each one's footprint: where hiding it changes the frame), the low body's
         * share in the frame is |F - HA| summed, against its share alone |HB - HAB|: a nearer body at alpha a passes 1 - a of what lies
         * behind it, so the ratio must be within OTOL of 1 - a. Drawn in one group as before the fix (F0), the high body's depth pass hides
         * the low body, so that ratio must be under 0.1 (the check sees the fault it guards). R62, reported: the regions by each body's
         * opaque frame alone (OL, OH: low only, high only, shared; not by O against OL, which calls a pixel the high body covers low
         * only wherever the two identical knights' colours there agree), each body's alpha where it lies alone (its hidden-other frame
         * and F over its opaque one), the shared ratio there, and how far F is from a over (1 - a) composited from the opaque frames.
         */
        void overlapShots(MinecraftClient c) {
            int k = wait / 6; if (wait % 6 != 5 || k >= oShots.length) return;
            var eh = client(c, high.mob); var el = client(c, low.mob);
            if (!(eh instanceof net.minecraft.entity.LivingEntity lh) || !(el instanceof net.minecraft.entity.LivingEntity ll)) { report("mo-fade-overlap", false, "the client lost a body while paused"); overlapEnd(c); ss = 99; return; }
            oPaused &= c.isPaused();
            if (k == 0) {
                var row = Spec.COMBATDISCIPLINE[0]; oDeath = lh.deathTime; oDeathLow = ll.deathTime; oTd = frameDelta;
                oAlpha = BattleMath.corpseAlpha(oDeath + oTd, row.corpseLieTicks(), row.corpseFadeTicks()); oAlphaLow = BattleMath.corpseAlpha(oDeathLow + oTd, row.corpseLieTicks(), row.corpseFadeTicks());
            }
            oTrace[k] = String.format(Locale.ROOT, "%s (partial tick %.3f): low %s; high %s", O_NAMES[k], frameDelta, BodyCache.traced(el), BodyCache.traced(eh));
            oShots[k] = ScreenshotRecorder.takeScreenshot(c.getFramebuffer());
            switch (k) {
                case 0 -> BodyCache.noGroups = true;
                case 1 -> { BodyCache.noGroups = false; BodyCache.hide = el.getId(); }
                case 2 -> BodyCache.hide = eh.getId();
                case 3 -> { BodyCache.hide = el.getId(); BodyCache.hide2 = eh.getId(); }
                case 4 -> { BodyCache.hide = -1; BodyCache.hide2 = -1; BodyCache.opaque = true; }
                case 5 -> BodyCache.hide = eh.getId();
                case 6 -> BodyCache.hide = el.getId();
                default -> {}
            }
            if (k < oShots.length - 1) return;
            var f = oShots[0]; var f0 = oShots[1]; var ha = oShots[2]; var hb = oShots[3]; var hab = oShots[4]; var o = oShots[5]; var ol = oShots[6]; var oh = oShots[7];
            int w = Integer.MAX_VALUE, h = Integer.MAX_VALUE; for (var s : oShots) { w = Math.min(w, s.getWidth()); h = Math.min(h, s.getHeight()); }
            long both = 0, footLow = 0, footHigh = 0; double through = 0, through0 = 0, alone = 0;
            long[] rn = new long[3]; double[] rOwn = new double[3], rF = new double[3], rDen = new double[3]; double sThrough = 0, sAlone = 0, resid = 0, mag = 0;
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int bg = hab.getColor(x, y), pf = f.getColor(x, y), pa = ha.getColor(x, y), pb = hb.getColor(x, y), po = o.getColor(x, y), pl = ol.getColor(x, y);
                boolean inLow = channel(pb, bg) > 2, inHigh = channel(pa, bg) > 2;
                if (inLow) footLow++; if (inHigh) footHigh++;
                if (inLow && inHigh) { both++; through += sum(pf, pa); through0 += sum(f0.getColor(x, y), pa); alone += sum(pb, bg); }
                // R62: low only (the low body opaque alone off the background, the high one opaque alone not), high only, shared (both)
                boolean lowO = channel(pl, bg) > 2, highO = channel(oh.getColor(x, y), bg) > 2;
                int r = lowO ? (highO ? 2 : 0) : (highO ? 1 : -1); if (r < 0) continue; rn[r]++;
                if (r == 0) { rOwn[0] += sum(pb, bg); rF[0] += sum(pf, bg); rDen[0] += sum(pl, bg); }
                else if (r == 1) { rOwn[1] += sum(pa, bg); rF[1] += sum(pf, bg); rDen[1] += sum(po, bg); }
                else {
                    sThrough += sum(pf, pa); sAlone += sum(pb, bg);
                    for (int q = 0; q < 24; q += 8) {
                        double g = bg >> q & 255, lo = pl >> q & 255, hi = po >> q & 255, pred = oAlpha * hi + (1 - oAlpha) * (oAlphaLow * lo + (1 - oAlphaLow) * g);
                        resid += Math.abs((pf >> q & 255) - pred); mag += Math.abs((pf >> q & 255) - g);
                    }
                }
            }
            double r = alone <= 0 ? -1 : through / alone, r0 = alone <= 0 ? -1 : through0 / alone, want = 1 - oAlpha;
            boolean pass = oPaused && both >= 1000 && Math.abs(r - want) <= OTOL && r0 >= 0 && r0 < 0.1 && BodyCache.fadeGroupsMax >= 2;
            var dir = new File(c.runDirectory, "screenshots"); dir.mkdirs();
            for (int i = 0; i < oShots.length; i++) try { oShots[i].writeTo(new File(dir, "mo-fade-overlap-" + O_NAMES[i] + ".png")); } catch (java.io.IOException ex) { ex.printStackTrace(); }
            String regions = String.format(Locale.ROOT, "regions by each body's opaque frame alone: low only %d px, its alpha alone (HB) %.3f and in F %.3f (want %.3f); high only %d px, its alpha alone (HA) %.3f and in F %.3f (want %.3f); shared %d px, the low body's share through the high one %.3f (want %.3f), F off a over (1 - a) composited from O and OL by %.3f of F's difference from the background",
                rn[0], div(rOwn[0], rDen[0]), div(rF[0], rDen[0]), oAlphaLow, rn[1], div(rOwn[1], rDen[1]), div(rF[1], rDen[1]), oAlpha, rn[2], div(sThrough, sAlone), want, div(resid, mag));
            report("mo-fade-overlap", pass, String.format(Locale.ROOT, "a " + body + bodyNote() + " lying 3 blocks over another, both fading (death ticks %d and %d at partial tick %.3f, alpha %.3f and %.3f), the game paused with no HUD %b, both drawn from their buffers %b; footprints %d (low) and %d (high) pixels, %d shared; over the shared pixels the low body's share through the high one is %.3f of its share alone (want %.3f = 1 - alpha, within %.2f); drawn in one group as before the fix %.3f (want under 0.1); most fade groups in a pass %d (want 2 or more); R62 %s; screenshots mo-fade-overlap-f/f0/ha/hb/hab/o/ol/oh.png; BodyCache: %s",
                oDeath, oDeathLow, oTd, oAlpha, oAlphaLow, oPaused, oCached, footLow, footHigh, both, r, want, OTOL, r0, BodyCache.fadeGroupsMax, regions, BodyCache.diag()));
            System.out.println("[p2] mo-fade-overlap trace: " + String.join(" | ", oTrace));
            overlapEnd(c); ss = 99;
        }
        static double div(double n, double d) { return d <= 0 ? -1 : n / d; }
        static int channel(int p, int q) { int d = 0; for (int k = 0; k < 24; k += 8) d = Math.max(d, Math.abs((p >> k & 255) - (q >> k & 255))); return d; }
        static int sum(int p, int q) { int d = 0; for (int k = 0; k < 24; k += 8) d += Math.abs((p >> k & 255) - (q >> k & 255)); return d; }
        void overlapEnd(MinecraftClient c) {
            BodyCache.noGroups = false; BodyCache.hide = -1; BodyCache.hide2 = -1; BodyCache.opaque = false;
            for (int i = 0; i < oShots.length; i++) if (oShots[i] != null) { oShots[i].close(); oShots[i] = null; }
            unstill(c);
            var at = barrier; if (at != null) { barrier = null; server(c, b -> b.world.setBlockState(at, net.minecraft.block.Blocks.AIR.getDefaultState())); }
        }
        // ---- mo-fade (R56) and mo-fade-smoke (R57) ----
        /** Where the faded knight lies (off the crowd's grid and the overlap pair), the camera's eye over it, and the death ticks sampled (lie 30: the fade runs 50 to 90). */
        static final double FX = -12, FZ = 2, FEYE = 4.5; static final int[] FTICKS = {52, 70, 88};
        /** The parts measured (BodyCache's harness bits) and their names. */
        static final int[] FPARTS = {BodyCache.BODY, BodyCache.ARMOUR, BodyCache.WEAPON, BodyCache.SHIELD}; static final String[] FNAMES = {"body", "armour", "weapon", "shield"};
        /** Shots per sample: the whole body fading (0), then on each path (the cached, then the normal one) the body hidden and each part opaque then fading. */
        static final int FSHOTS = 19;
        GravesAndGlory.Unit fUnit, sUnit; int fs, fRemovedAfter = -1, fAfterPixels = -1, smokeEnd = -1, smokeArmedAt = -1, sClearWait = -1, sTaken = -1; boolean fRed, fPaused = true, fCachedOk = true, fNormalOk = true; Boolean shadowsBefore;
        final int[] fDeath = new int[3]; final float[] fAlpha = new float[3];
        /** R59/R61: each sample's partial tick (the paused frame's) and the traced fading draws of the whole body (shot 0), the cached path's body (3) and the normal path's (12). */
        final float[] fTd = new float[3]; final String[][] fTrace = new String[3][3]; static final int[] F_TRACED = {0, 3, 12};
        /** R59: the body's alpha may be this far from BattleMath.corpseAlpha on either path, and the two paths this far apart. */
        static final double FWANT = 0.03, FPATHS = 0.02;
        /** M2: the shots whose fading draw came on the wrong path (the cached path's from a normal draw, or the normal path's from buffers). */
        final List<String> fWrongPath = new ArrayList<>();
        /** R60: the body's footprint (any opaque part shot of the last sample off its hidden frame), where the frame after removal is compared; and that frame's whole-frame count, for information. */
        boolean[] fMask; int fMaskW, fMaskH; long fMaskPixels, fAfterWhole = -1;
        /** Each sample's measured alpha and opaque footprint (pixels), by path and part. */
        final double[][][] fPart = new double[3][2][4]; final long[][][] fFoot = new long[3][2][4];
        final net.minecraft.client.texture.NativeImage[] fShots = new net.minecraft.client.texture.NativeImage[FSHOTS];
        /** The contact sheet's frames: the whole body at each sample, then the frame after its removal; and the last sample's body-hidden frame. */
        final net.minecraft.client.texture.NativeImage[] fSheet = new net.minecraft.client.texture.NativeImage[4]; net.minecraft.client.texture.NativeImage fHidden;
        /**
         * mo-fade (R56): a knight struck down alone, the camera straight above it, shadows off; at death ticks 52, 70 and 88 the game
         * pauses and each part of it (body, armour layers, weapon, shield; BodyCache.parts) is shot opaque (O) and fading (F) against
         * the body hidden (H), on the cached path and on the normal one (BodyCache.off). A part's alpha is the sum of |F - H| over the
         * sum of |O - H|. Each part's alpha must fall strictly over the samples and keep within 0.08 of the body's; the frame after the
         * body's removal must equal the hidden frame (0 pixels apart; particles cleared first, mo-fade-smoke counts them). With
         * -Dgravesandglory.fadeRed (GRAVES_FADE_RED) the weapon and shield draw whole while the rest fades: mo-fade must fail.
         * mo-fade-smoke (R57): the poofs the client spawns within 2 blocks of the body from death tick 85 to 5 unpaused ticks past its removal
         * (95): want 0. mo-fade-smoke-clear: a body lying (death tick 30) taken by Battle.clearCorpses (the round end's and lobby's
         * sweep, the discard that Grave Bloom's raise and the corpse sweep use too): want 0 poofs within 2 blocks over 10 ticks.
         */
        void fadeParts(MinecraftClient c) { // settle counts wait
            switch (ss) {
                case 20 -> {
                    String red = System.getProperty("gravesandglory.fadeRed", "");
                    fRed = "1".equals(red) || "true".equalsIgnoreCase(red);
                    shadowsBefore = c.options.getEntityShadows().getValue(); c.options.getEntityShadows().setValue(false);
                    server(c, b -> { fUnit = post(b, gold, body, FX, FZ, YAW); camera(b, FX, FEYE, FZ, 0, 90); });
                    ss = 21; wait = 0;
                }
                case 21 -> {
                    if (fUnit == null || client(c, fUnit.mob) == null || wait < 40) {
                        if (wait > 300) { report("mo-fade", false, "the " + body + " never reached the client"); report("mo-fade-smoke", false, "not run: mo-fade could not start"); fadeEnd(c); ss = 30; wait = 0; }
                        return;
                    }
                    server(c, b -> { fUnit.mob.timeUntilRegen = 0; fUnit.mob.hurtTime = 0; fUnit.mob.damage(b.world.getDamageSources().generic(), 10000f); });
                    fs = 0; ss = 22; wait = 0;
                }
                case 22 -> {
                    var e = client(c, fUnit.mob);
                    if (!(e instanceof net.minecraft.entity.LivingEntity le)) { if (wait > 200) { report("mo-fade", false, "the client lost the body before death tick " + FTICKS[fs]); smokeReport(); fadeEnd(c); ss = 30; wait = 0; } return; }
                    arm(le);
                    if (le.deathTime < FTICKS[fs]) { if (wait > 400) { report("mo-fade", false, "the body never reached death tick " + FTICKS[fs]); smokeReport(); fadeEnd(c); ss = 30; wait = 0; } return; }
                    shotState(0, le); still(c); ss = 23; wait = 0;
                }
                case 23 -> fadeShots(c);
                case 24 -> {
                    var e = client(c, fUnit.mob);
                    if (e instanceof net.minecraft.entity.LivingEntity le) { arm(le); if (wait > 200) { report("mo-fade", false, "the body was still on the client 200 ticks after death tick 88 (death tick " + le.deathTime + ")"); smokeReport(); fadeEnd(c); ss = 30; wait = 0; } return; }
                    // removed: clear the particles (mo-fade-smoke counted them as they spawned) so the next frame shows only what is drawn, and
                    // pause as the hidden frame was taken (no menu, no HUD: R60), so that frame is the first drawn after the removal. M6: this
                    // frame says nothing about particles (they were just cleared); mo-fade-smoke is the particle check.
                    fRemovedAfter = wait; c.particleManager.setWorld(c.world); /* clears every particle, the same world kept */
                    still(c); smokeEnd = 7; ss = 25; wait = 0;
                }
                case 25 -> {
                    if (wait == 1) {
                        var x = ScreenshotRecorder.takeScreenshot(c.getFramebuffer()); fSheet[3] = x;
                        long n = 0, whole = 0; int w = Math.min(x.getWidth(), fHidden.getWidth()), h = Math.min(x.getHeight(), fHidden.getHeight());
                        for (int y = 0; y < h; y++) for (int i = 0; i < w; i++) if (channel(x.getColor(i, y), fHidden.getColor(i, y)) > 2) {
                            whole++; if (fMask != null && i < fMaskW && y < fMaskH && fMask[y * fMaskW + i]) n++;
                        }
                        fAfterPixels = fMask == null || fMaskPixels == 0 ? -1 : (int) n; fAfterWhole = whole;
                    }
                    if (wait == 2) unstill(c); // on for the poof count's last 5 ticks
                    if (wait < smokeEnd) return;
                    smokeCounting = false; fadeReport(c); smokeReport(); fadeEnd(c); ss = 30; wait = 0;
                }
                case 30 -> { server(c, b -> { sUnit = post(b, gold, "knight", FX, FZ, YAW); camera(b, FX, FEYE, FZ, 0, 90); }); ss = 31; wait = 0; }
                case 31 -> {
                    if (sUnit == null || client(c, sUnit.mob) == null || wait < 20) { if (wait > 300) { report("mo-fade-smoke-clear", false, "the knight never reached the client"); ss = 100; } return; }
                    server(c, b -> { sUnit.mob.timeUntilRegen = 0; sUnit.mob.hurtTime = 0; sUnit.mob.damage(b.world.getDamageSources().generic(), 10000f); });
                    ss = 32; wait = 0;
                }
                case 32 -> {
                    var e = client(c, sUnit.mob);
                    if (!(e instanceof net.minecraft.entity.LivingEntity le) || le.deathTime < 30) { if (wait > 200) { report("mo-fade-smoke-clear", false, "the body never lay to death tick 30 on the client"); ss = 100; } return; }
                    sTaken = le.deathTime; SMOKE_AT = le.getPos().add(0, le.getHeight() / 2, 0); smokePoofs = 0; smokeAll = 0; smokeCounting = true;
                    server(c, b -> b.clearCorpses()); ss = 33; wait = 0;
                }
                case 33 -> {
                    if (sClearWait < 0 && client(c, sUnit.mob) == null) sClearWait = wait;
                    if (wait < 10) return;
                    smokeCounting = false;
                    report("mo-fade-smoke-clear", sClearWait >= 0 && smokePoofs == 0, String.format(Locale.ROOT, "a knight's body lying (client death tick %d) taken by Battle.clearCorpses (the discard the round end, lobby, corpse sweep and Grave Bloom's raise use): it left the client %s; poofs within 2 blocks over the next 10 ticks %d (want 0), anywhere %d",
                        sTaken, sClearWait < 0 ? "never, in 10 ticks" : "after " + sClearWait + " ticks", smokePoofs, smokeAll));
                    SMOKE_AT = null; ss = 100;
                }
                default -> ss = 100;
            }
        }
        /** Starts the poof count once the body reaches death tick 85. */
        void arm(net.minecraft.entity.LivingEntity le) {
            if (smokeArmedAt >= 0 || le.deathTime < 85) return;
            smokeArmedAt = le.deathTime; SMOKE_AT = le.getPos().add(0, le.getHeight() / 2, 0); smokePoofs = 0; smokeAll = 0; smokeCounting = true;
        }
        void smokeReport() {
            smokeCounting = false;
            report("mo-fade-smoke", smokeArmedAt >= 0 && fRemovedAfter >= 0 && smokePoofs == 0, String.format(Locale.ROOT, "a " + body + "'s body faded and removed (client death tick %s to %s past its removal): poofs the client spawned within 2 blocks of it %d (want 0), anywhere %d",
                smokeArmedAt < 0 ? "never 85" : String.valueOf(smokeArmedAt), fRemovedAfter < 0 ? "never removed" : "5 unpaused ticks", smokePoofs, smokeAll));
            SMOKE_AT = null;
        }
        /** Sets BodyCache's harness switches for shot k of a sample (see FSHOTS) and drops the body's buffers so it records them afresh. */
        void shotState(int k, Entity e) {
            int path = k <= 9 ? 0 : 1, j = k == 0 ? -1 : path == 0 ? k - 1 : k - 10;
            BodyCache.off = path == 1; BodyCache.heldOpaque = fRed;
            if (j < 0) { BodyCache.hide = -1; BodyCache.opaque = false; BodyCache.parts = BodyCache.ALL; }
            else if (j == 0) { BodyCache.hide = e.getId(); BodyCache.opaque = false; BodyCache.parts = BodyCache.ALL; }
            else { BodyCache.hide = -1; BodyCache.opaque = (j - 1) % 2 == 0; BodyCache.parts = FPARTS[(j - 1) / 2]; }
            BodyCache.forget(e);
        }
        void playState() { BodyCache.off = false; BodyCache.heldOpaque = false; BodyCache.hide = -1; BodyCache.opaque = false; BodyCache.parts = BodyCache.ALL; }
        void fadeShots(MinecraftClient c) {
            int k = wait / 6; if (wait % 6 != 5 || k >= FSHOTS) return;
            var e = client(c, fUnit.mob);
            if (e == null) { report("mo-fade", false, "the client lost the body while paused at death tick " + FTICKS[fs]); smokeReport(); fadeEnd(c); ss = 30; wait = 0; return; }
            if (k == 0) {
                // R59/R61: the want is the body's alpha at its death tick and the paused frame's partial tick (the time base every path must draw at)
                var row = Spec.COMBATDISCIPLINE[0]; fDeath[fs] = e instanceof net.minecraft.entity.LivingEntity le ? le.deathTime : -1; fTd[fs] = frameDelta;
                fAlpha[fs] = BattleMath.corpseAlpha(fDeath[fs] + fTd[fs], row.corpseLieTicks(), row.corpseFadeTicks());
            }
            fPaused &= c.isPaused();
            boolean cached = BodyCache.cached(e);
            if (k == 0 || (k >= 2 && k <= 9)) fCachedOk &= cached;
            if (k >= 11) fNormalOk &= !cached;
            for (int t = 0; t < F_TRACED.length; t++) if (k == F_TRACED[t]) fTrace[fs][t] = String.format(Locale.ROOT, "shot %d (partial tick %.3f): %s", k, frameDelta, BodyCache.traced(e));
            if (k == 0 || (k <= 9 && k % 2 == 1 && k > 1) || (k >= 12 && k % 2 == 0)) {
                var tf = BodyCache.tracedFade(e); boolean normal = k >= 12;
                if (tf == null || (normal ? tf.path != 2 : tf.path == 2)) fWrongPath.add("death tick " + FTICKS[fs] + " shot " + k + ": " + (tf == null ? "no fading draw" : BodyCache.TRACE_PATHS[tf.path]));
            }
            fShots[k] = ScreenshotRecorder.takeScreenshot(c.getFramebuffer());
            if (k + 1 < FSHOTS) { shotState(k + 1, e); return; }
            for (int path = 0; path < 2; path++) {
                var h = fShots[path == 0 ? 1 : 10];
                for (int q = 0; q < 4; q++) {
                    var o = fShots[(path == 0 ? 2 : 11) + 2 * q]; var f = fShots[(path == 0 ? 3 : 12) + 2 * q];
                    int w = Math.min(o.getWidth(), h.getWidth()), ht = Math.min(o.getHeight(), h.getHeight()); long foot = 0; double num = 0, den = 0;
                    for (int y = 0; y < ht; y++) for (int x = 0; x < w; x++) {
                        int bg = h.getColor(x, y); if (channel(o.getColor(x, y), bg) > 2) foot++;
                        num += sum(f.getColor(x, y), bg); den += sum(o.getColor(x, y), bg);
                    }
                    fPart[fs][path][q] = den <= 0 ? -1 : num / den; fFoot[fs][path][q] = foot;
                }
            }
            if (fs == 2) {
                // R60: the body's footprint, where any opaque part shot (either path) differs from its hidden frame
                var h0 = fShots[1]; fMaskW = h0.getWidth(); fMaskH = h0.getHeight(); fMask = new boolean[fMaskW * fMaskH]; fMaskPixels = 0;
                for (int path = 0; path < 2; path++) {
                    var h = fShots[path == 0 ? 1 : 10];
                    for (int q = 0; q < 4; q++) {
                        var o = fShots[(path == 0 ? 2 : 11) + 2 * q]; int w = Math.min(Math.min(o.getWidth(), h.getWidth()), fMaskW), ht = Math.min(Math.min(o.getHeight(), h.getHeight()), fMaskH);
                        for (int y = 0; y < ht; y++) for (int x = 0; x < w; x++) if (!fMask[y * fMaskW + x] && channel(o.getColor(x, y), h.getColor(x, y)) > 2) { fMask[y * fMaskW + x] = true; fMaskPixels++; }
                    }
                }
            }
            fSheet[fs] = fShots[0]; fShots[0] = null;
            if (fs == 2) { fHidden = fShots[1]; fShots[1] = null; }
            for (int i = 0; i < FSHOTS; i++) if (fShots[i] != null) { fShots[i].close(); fShots[i] = null; }
            playState(); BodyCache.forget(e); unstill(c);
            if (++fs < 3) { ss = 22; wait = 0; } else { ss = 24; wait = 0; }
        }
        void fadeReport(MinecraftClient c) {
            var dir = new File(c.runDirectory, "screenshots"); dir.mkdirs();
            String[] tags = {"52", "70", "88", "after"};
            for (int i = 0; i < 4; i++) if (fSheet[i] != null) try { fSheet[i].writeTo(new File(dir, "mo-fade-" + tags[i] + ".png")); } catch (java.io.IOException ex) { ex.printStackTrace(); }
            if (fHidden != null) try { fHidden.writeTo(new File(dir, "mo-fade-hidden.png")); } catch (java.io.IOException ex) { ex.printStackTrace(); }
            boolean sheet = false;
            if (fSheet[0] != null && fSheet[1] != null && fSheet[2] != null && fSheet[3] != null) {
                int w = fSheet[0].getWidth() / 2, h = fSheet[0].getHeight() / 2;
                try (var cs = new net.minecraft.client.texture.NativeImage(w * 4, h, false)) {
                    for (int i = 0; i < 4; i++) { var src = fSheet[i]; for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) cs.setColor(i * w + x, y, src.getColor(Math.min(2 * x, src.getWidth() - 1), Math.min(2 * y, src.getHeight() - 1))); }
                    cs.writeTo(new File(dir, "mo-fade-contact.png")); sheet = true;
                } catch (java.io.IOException ex) { ex.printStackTrace(); }
            }
            List<String> bad = new ArrayList<>(); StringBuilder t = new StringBuilder();
            for (int path = 0; path < 2; path++) {
                t.append(path == 0 ? "; cached path" : "; normal path");
                for (int q = 0; q < 4; q++) {
                    t.append(String.format(Locale.ROOT, " %s %.3f/%.3f/%.3f (%d/%d/%d px)", FNAMES[q], fPart[0][path][q], fPart[1][path][q], fPart[2][path][q], fFoot[0][path][q], fFoot[1][path][q], fFoot[2][path][q]));
                    for (int s2 = 0; s2 < 3; s2++) {
                        String where = FNAMES[q] + " (" + (path == 0 ? "cached" : "normal") + ")";
                        if (fFoot[s2][path][q] < 30) bad.add(where + " barely seen at death tick " + fDeath[s2] + ": " + fFoot[s2][path][q] + " px");
                        if (s2 > 0 && !(fPart[s2][path][q] < fPart[s2 - 1][path][q])) bad.add(where + " does not fall from death tick " + fDeath[s2 - 1] + " to " + fDeath[s2]);
                        if (q > 0 && Math.abs(fPart[s2][path][q] - fPart[s2][path][0]) > 0.08) bad.add(String.format(Locale.ROOT, "%s %.3f at death tick %d, %.3f off the body's %.3f", where, fPart[s2][path][q], fDeath[s2], Math.abs(fPart[s2][path][q] - fPart[s2][path][0]), fPart[s2][path][0]));
                    }
                }
            }
            StringBuilder want = new StringBuilder();
            for (int s2 = 0; s2 < 3; s2++) {
                double cachedA = fPart[s2][0][0], normalA = fPart[s2][1][0];
                want.append(String.format(Locale.ROOT, "%s death tick %d + %.3f: want %.4f, cached %.4f (off %.4f), normal %.4f (off %.4f), apart %.4f", s2 == 0 ? "" : "; ", fDeath[s2], fTd[s2], fAlpha[s2], cachedA, cachedA - fAlpha[s2], normalA, normalA - fAlpha[s2], cachedA - normalA));
                if (Math.abs(cachedA - fAlpha[s2]) > FWANT) bad.add(String.format(Locale.ROOT, "R59: the cached path's body %.4f at death tick %d + %.3f, %.4f off its alpha %.4f (want within %.2f)", cachedA, fDeath[s2], fTd[s2], Math.abs(cachedA - fAlpha[s2]), fAlpha[s2], FWANT));
                if (Math.abs(normalA - fAlpha[s2]) > FWANT) bad.add(String.format(Locale.ROOT, "R59: the normal path's body %.4f at death tick %d + %.3f, %.4f off its alpha %.4f (want within %.2f)", normalA, fDeath[s2], fTd[s2], Math.abs(normalA - fAlpha[s2]), fAlpha[s2], FWANT));
                if (Math.abs(cachedA - normalA) > FPATHS) bad.add(String.format(Locale.ROOT, "R59: the two paths' bodies %.4f apart at death tick %d (want within %.2f)", Math.abs(cachedA - normalA), fDeath[s2], FPATHS));
                for (int tr = 0; tr < F_TRACED.length; tr++) System.out.println("[p2] mo-fade trace: death tick " + FTICKS[s2] + " " + fTrace[s2][tr]);
            }
            if (!fPaused) bad.add("the game did not stay paused for the shots");
            if (!fCachedOk) bad.add("the cached path's body did not draw from its buffers");
            if (!fNormalOk) bad.add("the normal path's body drew from its buffers");
            if (!fWrongPath.isEmpty()) bad.add("fading draws on the wrong path: " + fWrongPath);
            if (fAfterPixels != 0) bad.add(fAfterPixels < 0 ? "no body footprint for the frame after removal" : "the frame after removal is " + fAfterPixels + " px off the body-hidden frame over the body's footprint");
            if (!sheet) bad.add("no contact sheet");
            report("mo-fade", bad.isEmpty(), String.format(Locale.ROOT, "a " + body + bodyNote() + " fading alone (lie %d, fade %d), paused with no menu or HUD at death ticks %d/%d/%d (alpha %.3f/%.3f/%.3f)%s; R59 the body's alpha against BattleMath.corpseAlpha (within %.2f each path, %.2f apart): %s; per part alpha (|F - H| over |O - H|) and opaque footprint at the three samples%s; the frame after its removal (seen %d ticks after the last shots) %d px off the body-hidden frame over the body's footprint of %d px (want 0; whole frame %d px, for information); contact sheet mo-fade-contact.png (52, 70, 88, after) %b%s",
                Spec.COMBATDISCIPLINE[0].corpseLieTicks(), Spec.COMBATDISCIPLINE[0].corpseFadeTicks(), fDeath[0], fDeath[1], fDeath[2], fAlpha[0], fAlpha[1], fAlpha[2], fRed ? " [red switch: held items opaque]" : "", FWANT, FPATHS, want, t, fRemovedAfter, fAfterPixels, fMaskPixels, fAfterWhole, sheet,
                bad.isEmpty() ? "" : "; off: " + bad));
        }
        void fadeEnd(MinecraftClient c) {
            playState(); smokeCounting = false;
            for (int i = 0; i < fShots.length; i++) if (fShots[i] != null) { fShots[i].close(); fShots[i] = null; }
            for (int i = 0; i < fSheet.length; i++) if (fSheet[i] != null) { fSheet[i].close(); fSheet[i] = null; }
            if (fHidden != null) { fHidden.close(); fHidden = null; }
            if (shadowsBefore != null) { c.options.getEntityShadows().setValue(shadowsBefore); shadowsBefore = null; }
            unstill(c);
        }
        static String hex(int light) { return "block " + (light >> 4 & 0xF) + " sky " + (light >> 20 & 0xF); }
        /** The late join done (or given up): the real clips back, on to the settle and light checks. */
        void afterLate() { restoreClips(); ls = 99; cs = 5; wait = 0; }
        void finishLive(MinecraftClient c) { restoreClips(); serverDone = true; end(c); }
        void restoreClips() { if (clipsBefore != null) { ClipLibrary.install(clipsBefore); clipsBefore = null; } }

        public void cleanup(MinecraftClient c) {
            restoreClips(); TroopMotion.clearForced(); PROBE = null; overlapEnd(c); fadeEnd(c); SMOKE_AT = null;
            var at = lamp; if (at != null) { lamp = null; server(c, b -> b.world.setBlockState(at, net.minecraft.block.Blocks.AIR.getDefaultState())); }
            if (cloudsBefore != null) { c.options.getCloudRenderMode().setValue(cloudsBefore); cloudsBefore = null; }
            super.cleanup(c);
        }
    }
}
