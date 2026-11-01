package gg.gravesandglory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static gg.gravesandglory.Phase2Checks.report;
import static gg.gravesandglory.Phase2Checks.server;

/**
 * motion-grade (Task 15, fix round 1: R66-R79): the evidence a grader reads for one motion family, GRAVES_MOTION_FAMILY (a family
 * name or a troop id; default "stub", the knight with the synthetic clips the mo-* checks drive, installed for the run and put back).
 *
 * <p>Everything a sheet shows went through the full TroopMotion pipeline (engine layers, MotionFeet, scene and death handling) as the
 * game draws it (R75). A clip is asked for only at the input level: the stance packet a server would send, an attack or hit event, a
 * scene pose, the shield raised by the game's own Tactics.shield, a real server kill. No sheet shows a raw clip.
 *
 * <p>Parts (GRAVES_GRADE_PARTS, default all): solo (every clip from 8 angles round one troop: 8 frames front and side, the key poses
 * from every angle, weapon-tip and shield-rim trails; R68, R74, R76, R77), loco (walk, run, shield crawl, backpedal and a 90 degree
 * turn, each from idle and back to idle, a side camera tracking at fixed distance, foot traces and planted slide; R71), states (each
 * live state on the 5 by 4 grid: the wide clone-check overview, the formation at real spacing from 30 blocks up high and at eye
 * height, and the close trio at real spacing as a 120-frame capture; R73, R78, R79) and transitions (stance to stance, shield up and
 * down, hit and death while attacking, every frame dense round each event, with the per-tick pose delta; R72).
 *
 * <p>Output: GRAVES_GRADE_OUT/&lt;family&gt;/&lt;yyyyMMdd-HHmmss&gt;/ with manifest.json (every raw file with its marks, the sheet specs, the
 * loco and delta numbers, the grading map). tools/contact_sheet.py renders the sheet specs with Pillow and writes the grading
 * template; without Pillow (or with GRAVES_GRADE_NO_PILLOW) it writes only the template and exits 3, and the same specs are drawn
 * here with ImageIO (no GIFs then).
 */
final class MotionGrade extends Phase2Checks.Stage {
    static final int COLS = 5, ROWS = 4, N = COLS * ROWS, SOLO_UNIT = 2, CAPTURE = 120, EVENT1 = 10, EVENT2 = 70, SETTLE = 40, FRAMES = 8,
            TRANS_FRAMES = 100, DENSE = 10, WIN_W = 1280, WIN_H = 720, GUI = 2, FOV = 70, TIMEOUT = 32000, STRIP = 4;
    static final double PARK = 40, WIDE_X = 2, WIDE_Z = 3, FILL = 0.8, MARGIN = 0.1, LEVEL_EYE = 1.62, LEVEL_Z = -30.5, HIGH_EYE = 16.5,
            HIGH_Z = -26.85, CLOSE_TURN = 30, PLANT_ABOVE = 0.04, SINK = 1 / 32.0, LOCO_WIDEN = 0.15, BACKPEDAL = 0.75, RUN = 1.5625, RUN_MIN = 0.25;
    static final float YAW = 180;
    static final int[] ANGLES = {0, 45, 90, 135, 180, 225, 270, 315};
    static final int L_SOLO = 0, L_WIDE = 1, L_FORMED = 2, L_CLOSE = 3, L_LOCO = 4;
    static final int[] PASSES = {L_WIDE, L_FORMED, L_CLOSE};
    static final String[] LAYOUT = {"solo", "wide", "formed", "close", "loco"};
    static final List<String> DIRS = List.of("front", "right", "back", "left");
    static final float[] BEARINGS = {0, 90, 180, -90};
    static final Map<String, String> SERVER_STANCE = Map.of("idle", "", "carry", "run", "guard", "hold", "brace", "brace", "wall", "wall", "shield", "shield_move", "aim", "ranged_aim", "rest", "ranged_rest");
    static final Set<String> SHIELD_STANCES = Set.of("wall", "brace", "shield_move", "hold");
    static final List<String> CAT_ORDER = List.of("stance", "attack", "hit", "scene", "death");
    static final String OUT_ROOT = System.getProperty("gravesandglory.gradeOut", "work/grade"), REPO = System.getProperty("gravesandglory.repoRoot", ""),
            PYTHON = System.getProperty("gravesandglory.python", "python");
    /** R84: GRAVES_GRADE_NO_PILLOW as contact_sheet.py reads it (1, true or yes). */
    static final boolean NO_PILLOW = Set.of("1", "true", "yes").contains(System.getProperty("gravesandglory.gradeNoPillow", "false").trim().toLowerCase(Locale.ROOT));
    static final Set<String> PARTS = parts(System.getProperty("gravesandglory.gradeParts", "solo,loco,states,transitions"));
    static final String C_GROUND = "#ffffff", C_PLANT = "#40ff40", C_LIFT = "#ff4040", C_SINK = "#ff40ff", C_GTICK = "#c8c8c8", C_HAND = "#ffe040", C_TIP = "#ff9020", C_RIM = "#40e0ff";
    static final String[] RUBRIC = {"weight", "arcs", "anticipation", "silhouette", "variation", "contact", "clipping", "blend"};

    static Set<String> parts(String s) { Set<String> o = new LinkedHashSet<>(); for (String p : s.split(",")) if (!p.isBlank()) o.add(p.trim()); return o; }

    // ---- what the last world render drew ----
    /** One troop as the last world render drew it, with the engine's inputs to that pose. */
    static final class Ent {
        int id, count, deathTime; boolean alive; Vec3d pos; float bodyYaw; float[] pose;
        int novKind = -1, novelties; double now = Double.NaN, tempo = 1, phase, m, run, sh, bp, tw, plant, pw, sceneStart;
        String sceneClip;
        final List<Clip> stanceClips = new ArrayList<>(); final List<Double> stanceW = new ArrayList<>();
        List<MotionEngine.Playing> tracks = List.of();
    }
    /** A world render: its matrices, camera and the watched troops. */
    static final class Snap { long n; Matrix4f proj, view; Vec3d cam; int w, h; final Map<Integer, Ent> ents = new HashMap<>(); }
    static final Set<Integer> WATCH = ConcurrentHashMap.newKeySet();
    static volatile Snap LAST; static long renders; static boolean hooked; static java.lang.reflect.Field bodiesField;

    static void hook() {
        if (hooked) return; hooked = true;
        WorldRenderEvents.END.register(ctx -> {
            renders++;
            if (WATCH.isEmpty()) return;
            try {
                var c = MinecraftClient.getInstance(); if (c.world == null) return;
                Snap s = new Snap(); s.n = renders; s.proj = new Matrix4f(ctx.projectionMatrix()); s.view = new Matrix4f(ctx.matrixStack().peek().getPositionMatrix());
                s.cam = ctx.camera().getPos(); var fb = c.getFramebuffer(); s.w = fb.textureWidth; s.h = fb.textureHeight;
                float td = ctx.tickDelta();
                for (int id : WATCH) { Entity e = c.world.getEntityById(id); if (e != null) s.ents.put(id, ent(e, td)); }
                LAST = s;
            } catch (RuntimeException x) { System.out.println("[mo-grade] recorder: " + x); }
        });
    }
    @SuppressWarnings("unchecked")
    static Map<Integer, TroopMotion.Body> bodies() {
        try {
            if (bodiesField == null) { bodiesField = TroopMotion.class.getDeclaredField("BODIES"); bodiesField.setAccessible(true); }
            return (Map<Integer, TroopMotion.Body>) bodiesField.get(null);
        } catch (ReflectiveOperationException x) { throw new IllegalStateException(x); }
    }
    static Ent ent(Entity e, float td) {
        Ent x = new Ent(); x.id = e.getId(); x.pos = e.getLerpedPos(td); x.bodyYaw = e.getYaw(td);
        if (e instanceof LivingEntity le) { x.bodyYaw = MathHelper.lerpAngleDegrees(td, le.prevBodyYaw, le.bodyYaw); x.deathTime = le.deathTime; x.alive = le.isAlive(); }
        var d = TroopRenderer.drawn(e); if (d != null) { x.count = d.count; x.pose = d.pose == null ? null : d.pose.clone(); }
        var st = TroopMotion.state(e.getId());
        if (st != null && st.frame.seed != null) {
            var f = st.frame; x.now = f.now; x.tempo = f.seed.tempo(); x.phase = f.seed.phase();
            x.m = f.m; x.run = f.run; x.sh = f.sh; x.bp = f.bp; x.tw = f.tw; x.plant = f.plant; x.pw = (1 - f.m) * (1 - f.tw) * f.plant * (1 - Math.min(1, f.bodyAction)); // MotionFeet.plant's pw, the weight the solver uses
            double carry = 1;
            for (int i = st.stances.size() - 1; i >= 0 && carry > 0; i--) {
                var l = st.stances.get(i); double w = MotionMath.ease(l.raw), ew = w * carry; carry *= 1 - w;
                for (int k = 0; k < l.n; k++) { x.stanceClips.add(l.clips[k]); x.stanceW.add(ew * l.coef[k]); }
            }
            x.novKind = st.novKind; x.novelties = st.novelties;
            x.tracks = MotionEngine.playing(st, f.now);
            var b = bodies().get(e.getId()); if (b != null) { x.sceneClip = b.sceneClip; x.sceneStart = b.sceneStart; }
        }
        return x;
    }

    // ---- state ----
    final String arg = System.getProperty("gravesandglory.motionFamily", "stub").trim().isEmpty() ? "stub" : System.getProperty("gravesandglory.motionFamily", "stub").trim();
    volatile String troop; volatile List<GravesAndGlory.Unit> cast = List.of(); volatile int castGen, done;
    volatile boolean shieldTroop; volatile double width = 0.6;
    volatile double[][] locoPath; volatile int locoI = -1;
    final Set<Integer> raised = ConcurrentHashMap.newKeySet();
    String rig, famName, stamp; boolean stub; Map<String, Clip> clipsBefore; File out;
    TroopModel<?> model; TroopMotion.Fam fam; List<double[]> weapon = List.of(); double[] tipPoint; String rimBone;
    CloudRenderMode cloudsBefore; Integer fovWas; Boolean bobWas; int oldW, oldH, oldGui = -1, fbW = WIN_W, fbH = WIN_H; double tanV, tanH;
    double[] cam; int req, held, watchGen = -1, seedGen = -1, ticks; long lastSnapN = -1; NativeImage frameImg;
    // the cloud: every clip's extent, in the solo unit's frame (it faces -z), blocks over its feet
    List<double[]> cloud = new ArrayList<>(); double cloudR, cloudTop, cloudBottom;
    double soloD, soloEye; int[] soloCrop, locoCrop; final Map<Double, double[]> closeCams = new HashMap<>(); final Map<Double, int[][]> closeCrops = new HashMap<>();
    // the plan
    final List<String> clipOrder = new ArrayList<>(); final Map<String, String> clipCat = new LinkedHashMap<>(), clipState = new LinkedHashMap<>();
    final Map<String, LinkedHashMap<String, Double>> keys = new LinkedHashMap<>(); final Map<String, Double> clipKey = new LinkedHashMap<>();
    final List<Job> jobs = new ArrayList<>(); final List<St> states = new ArrayList<>(); final List<Seg> segs = new ArrayList<>(); final List<Tr> trans = new ArrayList<>();
    int ji, li, lph, sti, pass, ps, f, ti, tph, tail;
    // the records
    final JsonArray files = new JsonArray(), unitsJson = new JsonArray(), statesJson = new JsonArray(), clipsJson = new JsonArray(), sheets = new JsonArray(),
            locoJson = new JsonArray(), transJson = new JsonArray(), crowdJson = new JsonArray();
    final Map<String, JsonObject> byFile = new HashMap<>(); final Map<String, JsonObject[]> trails = new HashMap<>();
    final List<String> bad = new ArrayList<>(), capBad = new ArrayList<>(), seedBad = new ArrayList<>(), motionBad = new ArrayList<>(), replayBad = new ArrayList<>(),
            framingBad = new ArrayList<>(), locoBad = new ArrayList<>();
    final List<String> writeErrors = java.util.Collections.synchronizedList(new ArrayList<>());
    final List<String> covered = new ArrayList<>(), noZoom = new ArrayList<>();
    final AtomicInteger writing = new AtomicInteger(); ExecutorService io;
    int seedsChecked, framingChecked; double motionLeast = Double.MAX_VALUE; String motionLeastClip = "";
    Process py; int pyExit = -99; String pyWhy = "";
    // per-pass capture records (unit 2)
    final List<float[]> trPose = new ArrayList<>(); final List<Double> trNow = new ArrayList<>(); final List<Boolean> trFresh = new ArrayList<>(); final List<Long> trWorld = new ArrayList<>();
    final List<String> trBones = new ArrayList<>();
    int levelAt = -1; String levelName;

    String name() { return "motion-grade"; }

    /** A solo job: one clip from one camera angle; its targets (frames and key poses) and what was seen. */
    final class Job {
        final String clip, cat, pre; final int angle, variant; final boolean full, blocked; final Clip c; final String zone, kind; final float bearing; final int hitIdx, hitN;
        final List<String> names = new ArrayList<>(), kinds = new ArrayList<>(), files = new ArrayList<>(); final List<Integer> frameNo = new ArrayList<>();
        double[] times; boolean[] got; int ph, settle, goneFor, maxTicks; boolean seen; double lastT = Double.NaN; final Set<String> played = new TreeSet<>();
        final JsonArray tip = new JsonArray(), rim = new JsonArray(); double stanceW = -1;
        Job(String clip, int angle, int settle) {
            this.clip = clip; this.angle = angle; this.settle = settle; cat = clipCat.get(clip); c = fam.all().get(clip); full = angle == 0 || angle == 90;
            pre = cat.equals("stance") ? SERVER_STANCE.getOrDefault(clip, "hold") : "hold";
            var f2 = fam.family(); int v = -1; String z = null; boolean blk = false; float br = 0; String kd = "slash"; int hi = 0, hn = 1;
            if (cat.equals("attack")) for (int i = 0; i < fam.attackIndex().length; i++) { int k = fam.attackIndex()[i]; if (k >= 0 && k < f2.attacks().size() && f2.attacks().get(k) == c) { v = i; break; } }
            // Task 16 H1: the engine picks a seeded hit variant from the unit's hit count; primeHit sets the count so the job plays this clip
            if (cat.equals("hit")) for (var e : f2.hits().entrySet()) if (e.getValue().contains(c)) { z = e.getKey(); hi = e.getValue().indexOf(c); hn = e.getValue().size(); break; }
            if ("block".equals(z)) { z = "body"; blk = true; }
            // Task 16: a death clip is reached by its own blow: its kind (bolt, which has no clip of its own, for the any-kind deaths) and a bearing
            // that leads with its direction (MotionEngine.death: front 0, a side 91 so the side leads the back, back 180)
            if (cat.equals("death")) { String dk = TroopMotion.deathKey(clip); if (dk != null) { String[] p = dk.split("/"); kd = p[0].equals("any") ? "bolt" : p[0];
                String dir = p.length > 1 ? p[1] : p[0].equals("any") || !DIRS.contains(p[0]) ? "front" : p[0]; br = switch (dir) { case "right" -> 91; case "left" -> -91; case "back" -> 180; default -> 0; }; } }
            variant = v; zone = z == null ? "body" : z; blocked = blk; bearing = br; kind = kd; hitIdx = hi; hitN = hn;
            double L = c.length();
            maxTicks = cat.equals("stance") ? (int) Math.ceil(L * 20 / 0.75) + 30 : cat.equals("attack") || cat.equals("hit") ? 200 : (int) Math.ceil(L * 20) + 80;
            String a = String.format(Locale.ROOT, "a%03d", angle);
            List<Double> ts = new ArrayList<>();
            if (full) for (int i = 0; i < FRAMES; i++) { names.add("f" + i); kinds.add("frame"); files.add("clips/" + clip + "/" + a + "_f" + i + ".png"); frameNo.add(i); ts.add(clampT(frameTime(c, i), c)); }
            for (var k : keys.get(clip).entrySet()) { names.add("k_" + k.getKey()); kinds.add("turn"); files.add("clips/" + clip + "/" + a + "_k_" + k.getKey() + ".png"); frameNo.add(-1); ts.add(clampT(k.getValue(), c)); }
            times = ts.stream().mapToDouble(Double::doubleValue).toArray(); got = new boolean[times.length];
        }
        boolean loop() { return cat.equals("stance"); }
    }
    static double clampT(double t, Clip c) { return c.loop() ? t : Math.max(0, Math.min(t, c.length() - 0.03)); }

    /** A live state on the grid. */
    record St(String name, String cat, String clip, String stance, String pre, int variant, String zone, boolean blocked, float bearing, int key, double len) {
        int[] stills() { return cat.equals("death") ? new int[]{28, 50, 80} : new int[]{Math.min(CAPTURE - 1, EVENT1 + Math.max(2, key))}; }
        boolean twice() { return cat.equals("attack") || cat.equals("hit"); }
    }
    /** A locomotion segment: its stance, shield, speed (blocks a tick), facing, direction along x, and the path the server walks. */
    record Seg(String name, String stance, boolean raised, double speed, float yaw, double dir, boolean turn, int move, double[][] path, String title) {}
    /** A transition: the pre stance and its events (frame, what, arg). */
    record Tr(String name, String pre, List<Object[]> evs, String title) {}
    /** H2 hit sweep and M5 novelty captures ride the transition machinery with their own lengths. */
    static final int HS_EVENT = 20, HS_FRAMES = 48, NOV_FRAMES = 380, MAX_RETRY = 2;
    static int tf(Tr t) { return t.name().startsWith("hs-") ? HS_FRAMES : t.name().equals("novelty-idle") ? NOV_FRAMES : TRANS_FRAMES; }
    static int tf(String n) { return n.startsWith("hs-") ? HS_FRAMES : n.equals("novelty-idle") ? NOV_FRAMES : TRANS_FRAMES; }
    static boolean sweep(String n) { return n.startsWith("hs-"); }
    final List<Integer> trNov = new ArrayList<>(); final List<String> trClip = new ArrayList<>();
    int capTry, capMark;
    /** D5: forget the files a failed capture saved (the retry saves them again under the same names). */
    void dropCapture() { while (files.size() > capMark) { var e = files.remove(files.size() - 1).getAsJsonObject(); byFile.remove(e.get("file").getAsString()); } }

    // ---- the step ----
    public void step(MinecraftClient c, int tick) {
        if (tick == 1) prepare();
        if (tick == 20) {
            c.setScreen(null);
            if (troop == null) { end(c); return; }
            var id = c.player.getUuid(); server(c, b -> { player = id; stage = 1; });
        }
        if (finished || tick < 20) return;
        ticks = tick;
        if (tick > TIMEOUT) { report("p2-motion-grade-timeout", false, "stuck at " + where() + ", server stage " + stage); end(c); return; }
        if (castGen != watchGen) { watchGen = castGen; WATCH.clear(); for (var u : cast) WATCH.add(u.mob.getId()); }
        try {
            switch (cs) {
                case 0 -> {
                    var list = cast;
                    if (litStart(c) && ready && list.size() == N && list.stream().allMatch(u -> client(c, u.mob) != null)) { setup(c); wait = 0; cs = 1; }
                    else if (++wait % 600 == 0) System.out.println("[p2-wait] motion-grade step 0 after " + wait + " ticks: ready=" + ready + ", cast " + list.size() + "; " + litWhy(c));
                }
                case 1 -> { if (++wait < 20) return; if (!plan(c)) { end(c); return; } wait = 0; cs = 2; if (!PARTS.contains("solo")) next(c); }
                case 2 -> solo(c);
                case 3 -> loco(c);
                case 4 -> states(c);
                case 5 -> transitions(c);
                case 6 -> {
                    if (writing.get() > 0 && ++wait < 3600) return;
                    if (writing.get() > 0) writeErrors.add(writing.get() + " images still being written after 3600 ticks");
                    buildSheets(); hitSweepSheets(); writeManifest(); startSheets(); wait = 0; cs = 7;
                }
                case 7 -> {
                    if (py != null && py.isAlive() && ++wait < 9000) return;
                    afterSheets(); serverDone = true; end(c);
                }
                default -> {}
            }
            if (cs >= 2 && cs <= 5) aim(c);
        } finally { if (frameImg != null) { frameImg.close(); frameImg = null; } }
    }
    /** The next part that was asked for (solo, loco, states, transitions), then the files. */
    void next(MinecraftClient c) {
        wait = 0;
        while (true) {
            cs++;
            if (cs == 3 && PARTS.contains("loco") || cs == 4 && PARTS.contains("states") || cs == 5 && (PARTS.contains("transitions") || PARTS.contains("hits") || PARTS.contains("novelty")) || cs >= 6) break;
        }
        if (cs >= 6) { for (var u : cast) { var e = client(c, u.mob); if (e != null) TroopModels.forceFace(e, -1); } raised.clear(); }
    }
    String where() { return "client step " + cs + " (job " + ji + "/" + jobs.size() + " ph " + (ji < jobs.size() ? jobs.get(ji).ph : -1) + ", loco " + li + "/" + segs.size() + " ph " + lph + ", state " + sti + "/" + states.size() + " pass " + pass + " step " + ps + " frame " + f + ", transition " + ti + "/" + trans.size() + " step " + tph + ")"; }

    /** Tick 1: the family's troop, and the synthetic clips installed when it has none of its own (or was asked as stub). */
    void prepare() {
        Phase2Checks.Turn.hook(); hook();
        String t = null;
        if (arg.equals("stub")) { t = "knight"; stub = true; }
        else for (String id : TroopModels.ids()) {
            var m = TroopModels.model(id); var row = m == null ? null : TroopMotion.row(m);
            String pick = System.getenv("GRAVES_GRADE_TROOP"); // Task 16: which troop of the family to grade (the knight for sword_shield); unset, the first
            if (row != null && (id.equals(arg) || row.family().equals(arg)) && (pick == null || pick.isBlank() || id.equals(arg) || id.equals(pick))) { t = id; break; }
        }
        if (t == null) { report("mo-grade", false, "no troop model of the family or troop '" + arg + "'"); return; }
        var m = TroopModels.model(t); var row = TroopMotion.row(m); rig = m.rig; famName = row.family();
        if (!stub && ClipLibrary.folder(rig, famName).isEmpty()) stub = true;
        if (stub) { clipsBefore = ClipLibrary.all(); Map<String, Clip> map = new TreeMap<>(clipsBefore); MotionChecks.synthetic(rig, famName, map); ClipLibrary.install(map); }
        troop = t;
    }

    // ---- server side ----
    void cast(GravesAndGlory.Battle b) {
        force(b, site.add(-42, 0, -42), site.add(42, 0, 42)); floor(b, 40);
        List<GravesAndGlory.Unit> list = new ArrayList<>();
        for (int i = 0; i < N; i++) { double[] t = target(i, L_SOLO, 1.8, 0); var u = post(b, gold, troop, t[0], t[1], YAW); list.add(u); }
        cast = List.copyOf(list); layout(b, L_SOLO, 1.8, 0, YAW);
        shieldTroop = ArmyMobs.tags(list.get(0).spec).contains("shield"); width = list.get(0).mob.getWidth();
        camera(b, 0, 1.6, -6, 0, 0); castGen++;
    }
    public void serverTick(GravesAndGlory.Battle b) {
        calm = false;
        // the stage's skirmish would end on its round timer about 15 minutes in and put the battle report over the world; a full
        // grading run is longer, so the round's clock is held at one minute in (test world only, no rule changes)
        if (b.running && b.time - b.roundStarted > 1200) b.roundStarted = b.time - 1200;
        var path = locoPath; int i = locoI;
        if (path != null && i >= 0 && i < path.length && cast.size() > SOLO_UNIT) { var u = cast.get(SOLO_UNIT); if (u.mob.isAlive()) move(u.mob, path[i][0], path[i][1], (float) path[i][2]); locoI = i + 1; }
        super.serverTick(b);
        for (var p : pins) if (p.mob().isAlive()) { p.mob().setAttacking(false); Tactics.shield(p.mob(), raised.contains(p.mob().getId())); }
    }
    /** Server: each troop of the cast that is dead or gone replaced by a fresh one in its place in the cast. */
    void respawn(GravesAndGlory.Battle b) {
        List<GravesAndGlory.Unit> list = new ArrayList<>(cast); boolean any = false;
        for (int i = 0; i < list.size(); i++) {
            var u = list.get(i); if (u.mob.isAlive() && !u.mob.isRemoved()) continue;
            any = true; GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); b.units.remove(u); posted.remove(u); pins.removeIf(p -> p.mob() == u.mob);
            list.set(i, post(b, gold, troop, 0, 0, YAW));
        }
        if (any) { cast = List.copyOf(list); castGen++; }
    }
    /** Where unit i stands in a layout: {dx, dz, parked (1: PARK blocks straight up)}. */
    static double[] target(int i, int mode, double cell, double locoX) {
        int col = i % COLS, row = i / COLS; double[] park = {(col - 2) * WIDE_X, row * WIDE_Z, 1};
        return switch (mode) {
            case L_SOLO -> i == SOLO_UNIT ? new double[]{0, 0, 0} : park;
            case L_LOCO -> i == SOLO_UNIT ? new double[]{locoX, 0, 0} : park;
            case L_WIDE -> new double[]{(col - 2) * WIDE_X, row * WIDE_Z, 0};
            case L_FORMED -> new double[]{(col - 2) * cell, row * cell, 0};
            case L_CLOSE -> i >= 1 && i <= 3 ? new double[]{(i - 2) * cell, 0, 0} : park;
            default -> park;
        };
    }
    void layout(GravesAndGlory.Battle b, int mode, double cell, double locoX, float locoYaw) {
        var list = cast;
        for (int i = 0; i < list.size(); i++) {
            double[] t = target(i, mode, cell, locoX); Vec3d w = at(t[0], t[1]); if (t[2] > 0) w = w.add(0, PARK, 0);
            MobEntity m = list.get(i).mob; float yaw = mode == L_LOCO && i == SOLO_UNIT ? locoYaw : YAW;
            pins.removeIf(p -> p.mob() == m); pins.add(new Pin(m, w, yaw)); pin(m, w, yaw);
        }
    }
    boolean placed(MinecraftClient c, int mode, double cell, double locoX) {
        var list = cast;
        for (int i = 0; i < list.size(); i++) {
            var e = client(c, list.get(i).mob); if (e == null) return false;
            double[] t = target(i, mode, cell, locoX); Vec3d w = at(t[0], t[1]); double y = w.y + (t[2] > 0 ? PARK : 0);
            if (Math.abs(e.getX() - w.x) > .05 || Math.abs(e.getY() - y) > .05 || Math.abs(e.getZ() - w.z) > .05) return false;
            if (Math.abs(e.prevX - w.x) > .05 || Math.abs(e.prevY - y) > .05 || Math.abs(e.prevZ - w.z) > .05) return false;
        }
        return true;
    }
    void kill(MinecraftClient c, List<GravesAndGlory.Unit> who, float bearing, String kind) {
        final List<GravesAndGlory.Unit> list = new ArrayList<>(who);
        server(c, b -> { for (var u : list) if (u.mob.isAlive()) MotionEvents.resolving(u.mob, () -> {
            MotionEvents.sendHit(u.mob, kind, "body", bearing, 1, false, true);
            u.mob.timeUntilRegen = 0; u.mob.hurtTime = 0; return u.mob.damage(b.world.getDamageSources().generic(), 10000f); }); });
    }

    // ---- the camera: set on this client every tick, so it is exact the frame after ----
    /** A camera relative to the platform centre: x, eye height over the standing layer, z, yaw, pitch. */
    record Cam(double x, double eye, double z, float yaw, float pitch) {}
    void setCam(Cam k) { double x = site.getX() + .5 + k.x(), z = site.getZ() + .5 + k.z(); cam = new double[]{x, site.getY() + k.eye(), z, k.yaw(), k.pitch(), x, z}; }
    void aim(MinecraftClient c) {
        if (awayFor > 0 || cam == null || c.player == null) return;
        var p = c.player; double y = cam[1] - 1.62;
        p.setPosition(cam[0], y, cam[2]); p.prevX = p.lastRenderX = cam[5]; p.prevY = p.lastRenderY = y; p.prevZ = p.lastRenderZ = cam[6];
        p.setYaw((float) cam[3]); p.prevYaw = (float) cam[3]; p.setPitch((float) cam[4]); p.prevPitch = (float) cam[4]; p.setHeadYaw((float) cam[3]); p.setVelocity(Vec3d.ZERO);
    }
    /** The last render was drawn from the camera set now (a static camera). */
    boolean camOk() {
        Snap s = LAST; if (s == null || cam == null) return false;
        return Math.abs(s.cam.x - cam[0]) < .02 && Math.abs(s.cam.y - cam[1]) < .05 && Math.abs(s.cam.z - cam[2]) < .02;
    }
    double[] project(Cam k, double px, double py, double pz) {
        double yaw = Math.toRadians(k.yaw()), pit = Math.toRadians(k.pitch());
        double fx = -Math.sin(yaw) * Math.cos(pit), fy = -Math.sin(pit), fz = Math.cos(yaw) * Math.cos(pit), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
        double dx = px - k.x(), dy = py - k.eye(), dz = pz - k.z(), zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.1) return null;
        double xc = dx * rx + dz * rz, yc = dx * ux + dy * uy + dz * uz;
        return new double[]{fbW / 2.0 * (1 + xc / zc / tanH), fbH / 2.0 * (1 - yc / zc / tanV)};
    }
    boolean fits(Cam[] ks, List<double[]> pts) {
        double mx = fbW * (1 - FILL) / 2, my = fbH * (1 - FILL) / 2;
        for (Cam k : ks) for (double[] p : pts) { double[] s = project(k, p[0], p[1], p[2]); if (s == null || s[0] < mx || s[0] > fbW - mx || s[1] < my || s[1] > fbH - my) return false; }
        return true;
    }
    /** The union of the points' projections from the cameras, grown by MARGIN of its size each way and kept on the frame: x, y, w, h. */
    int[] bbox(Cam[] ks, List<double[]> pts, double margin) {
        double x0 = 1e9, y0 = 1e9, x1 = -1e9, y1 = -1e9;
        for (Cam k : ks) for (double[] p : pts) { double[] s = project(k, p[0], p[1], p[2]); if (s == null) continue; x0 = Math.min(x0, s[0]); y0 = Math.min(y0, s[1]); x1 = Math.max(x1, s[0]); y1 = Math.max(y1, s[1]); }
        return grow(x0, y0, x1, y1, margin, fbW, fbH);
    }
    static int[] grow(double x0, double y0, double x1, double y1, double margin, int W, int H) {
        double mx = (x1 - x0) * margin, my = (y1 - y0) * margin;
        int a = (int) Math.max(0, Math.floor(x0 - mx)), b = (int) Math.max(0, Math.floor(y0 - my)), c = (int) Math.min(W, Math.ceil(x1 + mx)), d = (int) Math.min(H, Math.ceil(y1 + my));
        return new int[]{a, b, Math.max(1, c - a), Math.max(1, d - b)};
    }
    Cam soloCam(int angle, double d) { double a = Math.toRadians(angle); return new Cam(d * Math.sin(a), soloEye, -d * Math.cos(a), angle, 0); }
    Cam closeCam(double cell, double d) {
        double a = Math.toRadians(CLOSE_TURN), eye = cloudTop + 0.4, mid = (cloudTop + cloudBottom) / 2;
        return new Cam(d * Math.sin(a), eye, -d * Math.cos(a), (float) CLOSE_TURN, (float) Math.toDegrees(Math.atan2(eye - mid, d)));
    }
    Cam highCam(double rowGap) { double mid = (ROWS - 1) * rowGap / 2; return new Cam(0, HIGH_EYE, HIGH_Z, 0, (float) Math.toDegrees(Math.atan2(HIGH_EYE - 1, mid - HIGH_Z))); }
    Cam levelCam() { return new Cam(0, LEVEL_EYE, LEVEL_Z, 0, 0); }
    double fitD(java.util.function.DoubleFunction<Cam[]> ks, List<double[]> pts, double lo, double hi) {
        if (!fits(ks.apply(hi), pts)) return hi;
        for (int it = 0; it < 28; it++) { double mid = (lo + hi) / 2; if (fits(ks.apply(mid), pts)) hi = mid; else lo = mid; }
        return hi;
    }
    /** The close camera (distance, then its crops: the trio and the centre unit) for a spacing, fitted to the trio's cloud. */
    double closeD(double cell) {
        return closeCams.computeIfAbsent(cell, k -> {
            List<double[]> trio = new ArrayList<>(), mid = cloud; for (double[] p : cloud) for (int i = -1; i <= 1; i++) trio.add(new double[]{p[0] + i * cell, p[1], p[2]});
            double d = fitD(x -> new Cam[]{closeCam(cell, x)}, trio, 1, 40); Cam ck = closeCam(cell, d);
            closeCrops.put(cell, new int[][]{bbox(new Cam[]{ck}, trio, MARGIN), bbox(new Cam[]{ck}, mid, MARGIN)});
            return new double[]{d};
        })[0];
    }
    double cellFor(String stance) { return shieldTroop && SHIELD_STANCES.contains(stance) ? BattleMath.shieldCell(width, Spec.COMBATDISCIPLINE[0].shieldWallCell()) : Math.max(Spec.FORMATIONS[0].spacing(), BattleMath.spacing(width)); }

    /** The window, field of view, HUD, view bobbing and clouds every shot is taken with. */
    void setup(MinecraftClient c) {
        var win = c.getWindow(); oldW = win.getWidth(); oldH = win.getHeight(); oldGui = c.options.getGuiScale().getValue();
        Phase1Checks.window(c, WIN_W, WIN_H, GUI);
        fovWas = c.options.getFov().getValue(); c.options.getFov().setValue(FOV); fovScale(c, 0);
        bobWas = c.options.getBobView().getValue(); c.options.getBobView().setValue(false);
        c.options.hudHidden = true; cloudsBefore = c.options.getCloudRenderMode().getValue(); c.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        io = Executors.newFixedThreadPool(3, r -> { Thread t = new Thread(r, "motion-grade-writer"); t.setDaemon(true); return t; });
    }

    // ---- the plan ----
    static String category(String n, Set<String> variants) {
        if (TroopMotion.STANCES.contains(n)) return "stance";
        if (variants.contains(n)) return "attack";
        if (n.matches("hit_(head|body|legs)(_\\d+)?") || n.matches("block(_\\d+)?")) return "hit";
        if (TroopMotion.deathKey(n) != null) return "death";
        return "scene";
    }
    static String stateOf(String n, String cat) {
        return switch (cat) {
            case "stance" -> "stance-" + n;
            case "attack" -> "attack-" + n;
            case "hit" -> n.startsWith("block") ? "hit-block" : "hit-" + n.split("_")[1];
            case "death" -> { String d = TroopMotion.deathKey(n); int s = d.indexOf('/'); yield "death-" + (s < 0 ? (DIRS.contains(d) ? d : "front") : d.substring(s + 1)); }
            default -> "scene-" + n;
        };
    }
    static double keyTime(Clip clip) {
        if (clip.impact() >= 0) return clip.impact() / (double) Clip.TICKS;
        Map<String, double[]> m = new HashMap<>(); double best = -1, at = 0; int n = (int) Math.floor(clip.length() * Clip.TICKS + 1e-9);
        for (int i = 0; i <= n; i++) {
            double t = i / (double) Clip.TICKS; if (clip.loop() && t >= clip.length()) break;
            m.clear(); clip.sample(t, m); double s = 0; for (double[] r : m.values()) s += Math.abs(r[0]) + Math.abs(r[1]) + Math.abs(r[2]);
            if (s > best + 1e-9) { best = s; at = t; }
        }
        return at;
    }
    static double frameTime(Clip clip, int i) { return clip.loop() ? clip.length() * i / FRAMES : clip.length() * i / (FRAMES - 1); }
    String keyOf(Clip clip) { for (var e : fam.all().entrySet()) if (e.getValue() == clip) return e.getKey(); return clip == null ? "?" : clip.name(); }
    int keyTicks(String clip) { Double t = clipKey.get(clip); return t == null ? 10 : (int) Math.round(t * Clip.TICKS); }
    static float[] want(TroopModel<?> m, String clip, double t, boolean shield) {
        m.getPart().traverse().forEach(ModelPart::resetTransform);
        TroopMotion.applyForced(m, clip, t, shield);
        float[] o = m.snapshot(null); m.getPart().traverse().forEach(ModelPart::resetTransform); return o;
    }
    /**
     * The key poses of a clip (R76). A loop: its two extremes, the two samples (1/20 s apart) furthest apart. Any other clip: wind-up
     * (before the impact, the sample furthest from the impact pose), impact (the impact marker, else the largest summed rotation),
     * follow-through end (after the impact, the first sample whose step falls under a quarter of the fastest step after it) and recovery
     * (the end).
     */
    LinkedHashMap<String, Double> keyPoses(String n, Clip clip, boolean shield) {
        LinkedHashMap<String, Double> o = new LinkedHashMap<>(); double L = clip.length(); int steps = Math.max(2, (int) Math.round(L * 20));
        int count = clip.loop() ? steps : steps + 1; float[][] p = new float[count][]; double[] t = new double[count];
        for (int k = 0; k < count; k++) { t[k] = L * k / steps; p[k] = want(model, n, Math.min(t[k], clip.loop() ? L : L), shield); }
        if (clip.loop()) {
            double best = -1; int a = 0, b = count / 2;
            for (int i = 0; i < count; i++) for (int j = i + 1; j < count; j++) { double d = Phase2Checks.Anim.maxRot(p[i], p[j]); if (d > best) { best = d; a = i; b = j; } }
            o.put("extreme_a", t[a]); o.put("extreme_b", t[b]); return o;
        }
        double I = Math.min(keyTime(clip), L); int iI = (int) Math.round(I / L * steps);
        int w = 0; double best = -1; for (int k = 0; k < iI; k++) { double d = Phase2Checks.Anim.maxRot(p[k], p[iI]); if (d > best) { best = d; w = k; } }
        int pk = iI; double peak = -1; for (int k = iI; k < count - 1; k++) { double v = Phase2Checks.Anim.maxRot(p[k], p[k + 1]); if (v > peak) { peak = v; pk = k; } }
        int fo = -1; for (int k = pk + 1; k < count - 1; k++) if (Phase2Checks.Anim.maxRot(p[k], p[k + 1]) < peak * 0.25) { fo = k; break; }
        double follow = fo < 0 ? (I + L) / 2 : t[fo];
        o.put("windup", t[w]); o.put("impact", I); o.put("follow", follow); o.put("recovery", L); return o;
    }
    /** Every corner of every box of a pose and its weapon points, in the troop's frame (yaw 180) over its feet, into out (deduped). */
    void points(float[] pose, Set<Long> seen, List<double[]> out) {
        model.applyPose(pose);
        try {
            List<double[]> pts = new ArrayList<>();
            model.getPart().forEachCuboid(base(YAW), (entry, path, i, q) -> corners(entry, q, pts));
            var hp = model.handPath(model.hand()); if (!hp.isEmpty()) for (double[] w : weapon) pts.add(through(hp, YAW, w[0], w[1], w[2]));
            for (double[] p : pts) { long k = Math.round(p[0] * 32) * 1_000_003L * 1_000_003L + Math.round(p[1] * 32) * 1_000_003L + Math.round(p[2] * 32); if (seen.add(k)) out.add(p); }
        } finally { model.getPart().traverse().forEach(ModelPart::resetTransform); }
    }

    @SuppressWarnings("unchecked")
    boolean plan(MinecraftClient c) {
        model = TroopModels.model(troop); fam = model == null ? null : TroopMotion.family(model);
        if (fam == null) { report("mo-grade", false, "the troop " + troop + " has no motion family"); return false; }
        var fb = c.getFramebuffer(); fbW = fb.textureWidth; fbH = fb.textureHeight; tanV = Math.tan(Math.toRadians(FOV / 2.0)); tanH = tanV * fbW / fbH;
        try { var fw = TroopModel.class.getDeclaredField("weaponPoints"); fw.setAccessible(true); weapon = (List<double[]>) fw.get(model); } catch (ReflectiveOperationException x) { weapon = List.of(); }
        double best = -1; for (double[] w : weapon) { double n = w[0] * w[0] + w[1] * w[1] + w[2] * w[2]; if (n > best) { best = n; tipPoint = w; } }
        weaponModel = TroopModels.weapon(model.weaponId(), 1); itemTip = weaponModel == null ? null : meshTip(weaponModel); if (itemTip != null) tipPoint = itemTip;
        for (String b : model.boneOrder()) if (model.shieldBones().contains(b)) { rimBone = b; break; }
        stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new java.util.Date());
        out = new File(new File(OUT_ROOT, arg), stamp); out.mkdirs();
        System.out.println("[mo-grade] family " + famName + " (asked " + arg + ", troop " + troop + ", rig " + rig + (stub ? ", synthetic stub clips" : "") + "), parts " + PARTS + ", evidence in " + out.getAbsolutePath());
        var variants = Tactics.variants(famName); Set<String> ids = new TreeSet<>(); for (var v : variants) ids.add(v.id());
        List<String> names = new ArrayList<>(fam.all().keySet());
        names.sort(java.util.Comparator.comparing((String n) -> CAT_ORDER.indexOf(category(n, ids))).thenComparing(n -> TroopMotion.STANCES.indexOf(n)).thenComparing(n -> n));
        var e = client(c, cast.get(SOLO_UNIT).mob); boolean shield = e instanceof LivingEntity le && !le.getOffHandStack().isEmpty();
        Set<Long> seen = new java.util.HashSet<>();
        for (String n : names) {
            Clip clip = fam.all().get(n); String cat = category(n, ids), st = stateOf(n, cat); double key = keyTime(clip);
            clipOrder.add(n); clipCat.put(n, cat); clipState.put(n, st); clipKey.put(n, key); keys.put(n, keyPoses(n, clip, shield));
            float[][] poses = new float[FRAMES][]; for (int i = 0; i < FRAMES; i++) poses[i] = want(model, n, frameTime(clip, i), shield);
            double most = 0; for (int i = 0; i < FRAMES; i++) for (int j = i + 1; j < FRAMES; j++) most = Math.max(most, Phase2Checks.Anim.maxRot(poses[i], poses[j]));
            if (most <= 1) motionBad.add(String.format(Locale.ROOT, "%s (%.2f degrees)", n, most));
            if (most < motionLeast) { motionLeast = most; motionLeastClip = n; }
            for (double t = 0; t <= clip.length() + 1e-9; t += 0.05) points(want(model, n, Math.min(t, clip.length()), shield), seen, cloud);
            var j = new JsonObject(); j.addProperty("name", n); j.addProperty("category", cat); j.addProperty("state", st);
            j.addProperty("length", clip.length()); j.addProperty("loop", clip.loop()); j.addProperty("impactTicks", clip.impact()); j.addProperty("keyTime", key);
            j.addProperty("keySource", clip.impact() >= 0 ? "impact marker" : "largest summed rotation"); j.addProperty("mostApartDeg", most);
            var times = new JsonArray(); for (int i = 0; i < FRAMES; i++) times.add(frameTime(clip, i)); j.add("frameTimes", times);
            var kp = new JsonObject(); for (var k : keys.get(n).entrySet()) kp.addProperty(k.getKey(), k.getValue()); j.add("keyPoses", kp);
            clipsJson.add(j);
        }
        cloudR = 0; cloudTop = 0; cloudBottom = 0;
        for (double[] p : cloud) { cloudR = Math.max(cloudR, Math.hypot(p[0], p[2])); cloudTop = Math.max(cloudTop, p[1]); cloudBottom = Math.min(cloudBottom, p[1]); }
        cloud.add(new double[]{0, Math.min(0, cloudBottom) - 0.05, 0});
        soloEye = (cloudTop + Math.min(0, cloudBottom)) / 2;
        soloD = fitD(d -> { Cam[] ks = new Cam[ANGLES.length]; for (int i = 0; i < ANGLES.length; i++) ks[i] = soloCam(ANGLES[i], d); return ks; }, cloud, 1, 30);
        Cam[] all = new Cam[ANGLES.length]; for (int i = 0; i < ANGLES.length; i++) all[i] = soloCam(ANGLES[i], soloD);
        soloCrop = bbox(all, cloud, MARGIN);
        int extra = (int) Math.round(soloCrop[2] * LOCO_WIDEN); int lx = Math.max(0, soloCrop[0] - extra), lw = Math.min(fbW - lx, soloCrop[2] + 2 * extra);
        locoCrop = new int[]{lx, soloCrop[1], lw, soloCrop[3]};
        System.out.println(String.format(Locale.ROOT, "[mo-grade] cloud %d points, radius %.2f, top %.2f, bottom %.2f; solo camera %.2f blocks, eye %.2f, crop %s", cloud.size(), cloudR, cloudTop, cloudBottom, soloD, soloEye, Arrays.toString(soloCrop)));

        // solo jobs: clip by clip, every angle
        if (PARTS.contains("solo")) for (String n : clipOrder) {
            String cat = clipCat.get(n);
            for (int a : ANGLES) {
                boolean first = a == ANGLES[0];
                int settle = switch (cat) { case "stance" -> first ? 30 : 3; case "scene" -> 15; case "death" -> first ? 25 : 12; default -> first ? 30 : 8; };
                jobs.add(new Job(n, a, settle));
            }
        }
        // loco segments
        double pace = Tactics.pace(cast.get(SOLO_UNIT).mob), crawl = pace * Spec.COMBATDISCIPLINE[0].crawlShare();
        segs.add(seg("walk", "run", false, pace, -90, 1, 50, "walk: the march (carry stance, as the engine plays a moving troop with no enemy near), at the troop's pace"));
        segs.add(seg("run", "run", false, Math.max(pace * RUN, RUN_MIN), -90, 1, 40, "run: carry stance, pace x " + RUN + " (at least " + RUN_MIN + ")"));
        segs.add(seg("crawl", "shield_move", true, crawl, -90, 1, 50, "shield crawl: stance shield_move, shield raised, pace x crawlShare " + Spec.COMBATDISCIPLINE[0].crawlShare()));
        segs.add(seg("backpedal", "fall_back", false, pace * BACKPEDAL, 90, 1, 50, "backpedal: stance fall_back, facing -x and walking +x at pace x " + BACKPEDAL + " (assumed)"));
        segs.add(seg("guard-shuffle", "hold", false, crawl, -90, 1, 50, "guard shuffle: stance hold (the guard), at the crawl pace, shield not raised"));
        segs.add(seg("turn", "", false, 0, -90, 0, 0, "90 degree turn in place: 10 degrees a tick to the left, held 30 ticks, back, held 20"));
        // states
        var f2 = fam.family();
        for (String s : TroopMotion.STANCES) if (f2.stances().containsKey(s)) { String k = keyOf(f2.stances().get(s)); states.add(new St("stance-" + s, "stance", k, SERVER_STANCE.get(s), s.equals("idle") ? "hold" : "", -1, null, false, 0, 20, fam.all().get(k).length())); }
        for (int i = 0; i < variants.size(); i++) if (i < fam.attackIndex().length && fam.attackIndex()[i] >= 0) { String id = keyOf(f2.attacks().get(fam.attackIndex()[i])); states.add(new St("attack-" + variants.get(i).id(), "attack", id, "hold", "hold", i, null, false, 0, keyTicks(id), fam.all().get(id).length())); }
        for (String z : List.of("head", "body", "legs", "block")) {
            var l = f2.hits().get(z); if (l == null || l.isEmpty()) continue;
            String clip = keyOf(l.get(0)); boolean blk = z.equals("block");
            states.add(new St("hit-" + z, "hit", clip, "hold", "hold", -1, blk ? "body" : z, blk, 0, keyTicks(clip), l.get(0).length()));
        }
        for (String n : clipOrder) if (clipCat.get(n).equals("scene")) states.add(new St("scene-" + n, "scene", n, "hold", "hold", -1, null, false, 0, keyTicks(n), fam.all().get(n).length()));
        if (!f2.deaths().isEmpty()) for (int d = 0; d < DIRS.size(); d++) {
            Clip dc = MotionEngine.deathClip(f2, "slash", DIRS.get(d)); if (dc == null) dc = f2.deaths().values().iterator().next().get(0);
            String clip = keyOf(dc); states.add(new St("death-" + DIRS.get(d), "death", clip, "hold", "hold", -1, null, false, BEARINGS[d], keyTicks(clip), dc.length()));
        }
        if (!PARTS.contains("states")) states.clear();
        for (St s : states) {
            var j = new JsonObject(); j.addProperty("name", s.name()); j.addProperty("category", s.cat()); j.addProperty("clip", s.clip());
            j.addProperty("stance", s.stance()); j.addProperty("preStance", s.pre()); j.addProperty("keyTicks", s.key()); j.addProperty("cell", cellFor(s.stance()));
            var st = new JsonArray(); for (int x : s.stills()) st.add(x); j.add("stillFrames", st);
            var ev = new JsonArray(); ev.add(EVENT1); if (s.twice()) ev.add(EVENT2); j.add("eventsAfterFrames", ev);
            if (s.variant() >= 0) j.addProperty("variantIndex", s.variant());
            if (s.zone() != null) { j.addProperty("zone", s.zone()); j.addProperty("blocked", s.blocked()); j.addProperty("kind", "slash"); j.addProperty("weight", 1); }
            if (s.cat().equals("death")) { j.addProperty("bearing", s.bearing()); j.addProperty("kind", "slash"); j.addProperty("note", "the kill fires after frame 10, so death tick 0 is about frame 11: f28 is death tick about 17 (vanilla death tint, as in game), f50 about 40, f80 about 70"); }
            statesJson.add(j);
        }
        // transitions
        if (PARTS.contains("transitions")) {
            trans.add(new Tr("hold-wall-brace", "hold", List.of(new Object[]{20, "stance", "wall"}, new Object[]{60, "stance", "brace"}), "stance to stance: hold, wall after f20, brace after f60"));
            trans.add(new Tr("guard-carry", "hold", List.of(new Object[]{20, "stance", "run"}, new Object[]{70, "stance", "hold"}), "guard to carry and back: hold, run after f20, hold after f70"));
            trans.add(new Tr("shield-up-down", "hold", List.of(new Object[]{20, "raise", ""}, new Object[]{70, "lower", ""}), "shield up after f20 and down after f70 (the game's Tactics.shield on the server)"));
            trans.add(new Tr("hit-while-attacking", "hold", List.of(new Object[]{20, "attack", ""}, new Object[]{25, "hit", "body"}), "hit while attacking: attack after f20, a body hit after f25"));
            trans.add(new Tr("death-while-attacking", "hold", List.of(new Object[]{20, "attack", ""}, new Object[]{25, "kill", ""}), "death while attacking: attack after f20, a real kill after f25"));
        }
        if (PARTS.contains("hits")) for (String z : List.of("head", "body", "legs")) for (String d : DIRS) for (int w = 0; w < 3; w++)
            trans.add(new Tr("hs-" + z + "-" + d + "-" + w, "hold", java.util.Collections.singletonList(new Object[]{HS_EVENT, "hit", z + ":" + d + ":" + w}), "hit sweep: " + z + " zone, from the " + d + ", weight " + w + " (slash), fired after f" + HS_EVENT));
        if (PARTS.contains("novelty")) trans.add(new Tr("novelty-idle", "", java.util.Collections.singletonList(new Object[]{10, "stance", ""}), "idle for " + NOV_FRAMES / 20 + " s with no enemy, no move: a novelty idle fires (first one 2 to 14 s in)"));
        seeds(c, "the first cast", true);
        System.out.println("[mo-grade] " + clipOrder.size() + " clips (" + jobs.size() + " solo jobs), " + segs.size() + " loco segments, " + states.size() + " states, " + trans.size() + " transitions");
        return true;
    }
    Seg seg(String name, String stance, boolean raised, double speed, float yaw, double dir, int move, String title) {
        List<double[]> p = new ArrayList<>(); boolean turn = name.equals("turn"); double x = turn ? 0 : -10, v = 0; float y = yaw;
        for (int i = 0; i < 15; i++) p.add(new double[]{x, 0, y});
        if (!turn) {
            for (int i = 0; i < move; i++) { v = .546 * v + .454 * speed; x += v * dir; p.add(new double[]{x, 0, y}); }
            for (int i = 0; i < 35; i++) { v = .546 * v; x += v * dir; p.add(new double[]{x, 0, y}); }
        } else {
            for (int i = 0; i < 9; i++) { y += 10; p.add(new double[]{x, 0, y}); }
            for (int i = 0; i < 30; i++) p.add(new double[]{x, 0, y});
            for (int i = 0; i < 9; i++) { y -= 10; p.add(new double[]{x, 0, y}); }
            for (int i = 0; i < 20; i++) p.add(new double[]{x, 0, y});
            move = 9;
        }
        return new Seg(name, stance, raised, speed, yaw, dir, turn, move, p.toArray(new double[0][]), title);
    }
    /** mo-grade-seeds: every unit of the cast its own seed; the first cast is written to the manifest. */
    void seeds(MinecraftClient c, String which, boolean record) {
        var list = cast; Set<String> seen = new TreeSet<>(); var arr = new JsonArray();
        for (int i = 0; i < list.size(); i++) {
            var u = list.get(i); var id = u.mob.getUuid(); var s = MotionMath.seed(id.getMostSignificantBits(), id.getLeastSignificantBits());
            seen.add(String.format(Locale.ROOT, "%.6f/%.6f/%.6f/%d", s.phase(), s.tempo(), s.amp(), s.variantBias()));
            var j = new JsonObject(); j.addProperty("index", i); j.addProperty("entityId", u.mob.getId()); j.addProperty("uuid", id.toString());
            var sj = new JsonObject(); sj.addProperty("phase", s.phase()); sj.addProperty("tempo", s.tempo()); sj.addProperty("amp", s.amp()); sj.addProperty("breath", s.breath());
            sj.addProperty("idleLeg", s.idleLeg()); sj.addProperty("noveltyOffset", s.noveltyOffset()); sj.addProperty("variantBias", s.variantBias()); j.add("seed", sj);
            var roles = new JsonArray(); roles.add("wide"); roles.add("formed"); if (i >= 1 && i <= 3) roles.add("close"); if (i == SOLO_UNIT) { roles.add("solo"); roles.add("loco"); } j.add("roles", roles);
            arr.add(j);
        }
        seedsChecked++; seedGen = castGen;
        if (seen.size() != N || list.size() != N) seedBad.add(which + ": " + seen.size() + " distinct seeds of " + list.size() + " units");
        if (record) for (var x : arr) unitsJson.add(x);
    }
    void inject(MinecraftClient c, String stance) {
        int idx = Math.max(0, MotionEvents.STANCES.indexOf(stance));
        for (var u : cast) { var e = client(c, u.mob); if (e != null && e.isAlive()) MotionEvents.Client.stance(e.getId(), idx, 0f); }
    }
    void scenes(MinecraftClient c, String clip) { for (var u : cast) { var e = client(c, u.mob); if (e != null) TroopMotion.setScenePose(e.getId(), clip); } }

    // ---- geometry of a drawn pose ----
    static final String[] SHINS = {"right_shin", "left_shin"};
    /** Points of a pose in blocks, relative to the troop's feet: the soles (right, left), the hands (right, left), the weapon tip, the shield rim. */
    static final class Geo { final double[][] soles = new double[2][], hands = new double[2][]; final double[][][] feet = new double[2][][]; double[] tip, rim; }
    static MatrixStack base(float yaw) { MatrixStack ms = new MatrixStack(); ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - yaw)); ms.scale(-1, -1, 1); ms.translate(0, -1.501f, 0); return ms; }
    /** D1: the rendered blade tip: the item mesh's vertex farthest from the grip, through the hand path then the weapon's grip, as TroopFeatures.Weapon draws it. */
    double[] bladeTip(List<ModelPart> path, float yaw) {
        if (itemTip == null) return null;
        MatrixStack ms = base(yaw); for (ModelPart p : path) p.rotate(ms); weaponModel.grip(ms);
        Vector4f v = new Vector4f((float) itemTip[0] / 16f, (float) itemTip[1] / 16f, (float) itemTip[2] / 16f, 1); ms.peek().getPositionMatrix().transform(v); return new double[]{v.x, v.y, v.z};
    }
    TroopModels.Weapon weaponModel; double[] itemTip;
    /** The mesh's tip in the item's own frame (model px): the centroid of the vertices at the greatest distance from its origin (the grip). */
    static double[] meshTip(TroopModels.Weapon w) {
        try {
            var fp = TroopMesh.class.getDeclaredField("pos"); fp.setAccessible(true); float[] pos = (float[]) fp.get(w.frames().get(0));
            double best = -1; for (int i = 0; i + 2 < pos.length; i += 3) best = Math.max(best, Math.sqrt(pos[i] * pos[i] + pos[i + 1] * pos[i + 1] + pos[i + 2] * pos[i + 2]));
            double[] sum = new double[3]; int n = 0;
            for (int i = 0; i + 2 < pos.length; i += 3) if (Math.sqrt(pos[i] * pos[i] + pos[i + 1] * pos[i + 1] + pos[i + 2] * pos[i + 2]) >= best - 0.02) { sum[0] += pos[i]; sum[1] += pos[i + 1]; sum[2] += pos[i + 2]; n++; }
            return n == 0 ? null : new double[]{sum[0] / n, sum[1] / n, sum[2] / n};
        } catch (ReflectiveOperationException x) { return null; }
    }
    static double[] through(List<ModelPart> path, float yaw, double x, double y, double z) {
        MatrixStack ms = base(yaw); for (ModelPart p : path) p.rotate(ms);
        Vector4f v = new Vector4f((float) x / 16f, (float) y / 16f, (float) z / 16f, 1); ms.peek().getPositionMatrix().transform(v); return new double[]{v.x, v.y, v.z};
    }
    /** Whether a cuboid that forEachCuboid hands over from under a part (its path "/child/grandchild") is drawn: every part on the way visible, its own part not hidden. */
    static boolean drawn(ModelPart from, String path) {
        ModelPart cur = from; if (!cur.visible) return false;
        for (String seg : path.split("/")) { if (seg.isEmpty()) continue; if (!cur.hasChild(seg)) return false; cur = cur.getChild(seg); if (!cur.visible) return false; }
        return !cur.hidden;
    }
    /** A sole's colour: sinking (below the ground by more than half a texel, 1/32 block), planted, or lifted. */
    static String soleColour(double h) { return h < -SINK ? C_SINK : h <= PLANT_ABOVE ? C_PLANT : C_LIFT; }
    static void corners(MatrixStack.Entry entry, ModelPart.Cuboid q, List<double[]> out) {
        for (int k = 0; k < 8; k++) {
            Vector4f v = new Vector4f(((k & 1) == 0 ? q.minX : q.maxX) / 16f, ((k & 2) == 0 ? q.minY : q.maxY) / 16f, ((k & 4) == 0 ? q.minZ : q.maxZ) / 16f, 1);
            entry.getPositionMatrix().transform(v); out.add(new double[]{v.x, v.y, v.z});
        }
    }
    Geo geo(float[] pose, float yaw) {
        Geo g = new Geo(); if (pose == null) return g;
        model.applyPose(pose);
        try {
            for (int i = 0; i < 2; i++) {
                List<ModelPart> path = model.path(SHINS[i]); if (path.isEmpty()) continue;
                MatrixStack ms = base(yaw); for (int k = 0; k < path.size() - 1; k++) path.get(k).rotate(ms);
                List<double[]> pts = new ArrayList<>(); ModelPart shin = path.get(path.size() - 1); shin.forEachCuboid(ms, (entry, p, n, q) -> { if (drawn(shin, p)) corners(entry, q, pts); });
                double min = 1e9; for (double[] p : pts) min = Math.min(min, p[1]);
                double sx = 0, sy = 0, sz = 0; int n = 0; for (double[] p : pts) if (p[1] < min + 0.02) { sx += p[0]; sy += p[1]; sz += p[2]; n++; }
                if (n > 0) { g.soles[i] = new double[]{sx / n, sy / n, sz / n}; g.feet[i] = pts.toArray(new double[0][]); }
            }
            for (int i = 0; i < 2; i++) { var p = model.handPath(i == 0 ? "right" : "left"); if (!p.isEmpty()) g.hands[i] = through(p, yaw, 0, 0, 0); }
            var hp = model.handPath(model.hand()); if (!hp.isEmpty() && tipPoint != null) g.tip = bladeTip(hp, yaw);
            if (rimBone != null) {
                List<ModelPart> path = model.path(rimBone);
                if (!path.isEmpty()) {
                    MatrixStack ms = base(yaw); for (int k = 0; k < path.size() - 1; k++) path.get(k).rotate(ms);
                    double[] vol = {-1}; double[][] best = {null};
                    ModelPart sb = path.get(path.size() - 1);
                    sb.forEachCuboid(ms, (entry, p, n, q) -> {
                        if (!drawn(sb, p)) return;
                        double v = (q.maxX - q.minX) * (q.maxY - q.minY) * (q.maxZ - q.minZ); if (v <= vol[0]) return; vol[0] = v;
                        Vector4f w = new Vector4f((q.minX + q.maxX) / 32f, q.minY / 16f, (q.minZ + q.maxZ) / 32f, 1); entry.getPositionMatrix().transform(w); best[0] = new double[]{w.x, w.y, w.z};
                    });
                    g.rim = best[0];
                }
            }
        } finally { model.getPart().traverse().forEach(ModelPart::resetTransform); }
        return g;
    }
    /** A point relative to a troop's feet on the frame of a render (pixels), or null behind the camera. */
    static double[] screen(Snap s, Vec3d pos, double[] rel) {
        if (rel == null) return null;
        Vector4f v = new Vector4f((float) (pos.x + rel[0] - s.cam.x), (float) (pos.y + rel[1] - s.cam.y), (float) (pos.z + rel[2] - s.cam.z), 1);
        s.view.transform(v); s.proj.transform(v); if (v.w <= 0) return null;
        return new double[]{(v.x / v.w * .5 + .5) * s.w, (1 - (v.y / v.w * .5 + .5)) * s.h};
    }
    /** Pixels a block at a troop's distance on a render (vertical). */
    static double ppb(Snap s, Vec3d pos) { double[] a = screen(s, pos, new double[]{0, 0, 0}), b = screen(s, pos, new double[]{0, 1, 0}); return a == null || b == null ? 60 : Math.abs(a[1] - b[1]); }

    // ---- marks (in the saved image's pixels) ----
    static JsonArray xy(double[] p, int[] crop) { var a = new JsonArray(); a.add(Math.round((p[0] - crop[0]) * 10) / 10.0); a.add(Math.round((p[1] - crop[1]) * 10) / 10.0); return a; }
    static void line(JsonArray m, double[] a, double[] b, int[] crop, String col, int w) { if (a == null || b == null) return; var j = new JsonObject(); j.addProperty("t", "line"); var p = new JsonArray(); p.add(xy(a, crop)); p.add(xy(b, crop)); j.add("pts", p); j.addProperty("c", col); j.addProperty("w", w); m.add(j); }
    static void dot(JsonArray m, double[] a, int[] crop, String col, int r) { if (a == null) return; var j = new JsonObject(); j.addProperty("t", "dot"); j.add("p", xy(a, crop)); j.addProperty("c", col); j.addProperty("r", r); m.add(j); }
    static void text(JsonArray m, double[] a, int[] crop, String s, String col) { if (a == null) return; var j = new JsonObject(); j.addProperty("t", "text"); j.add("p", xy(a, crop)); j.addProperty("s", s); j.addProperty("c", col); m.add(j); }
    /**
     * The marks of one troop on a frame: the ground line under its feet across the view (white), height ticks every 0.5 blocks beside
     * it on the camera's left (white), the soles (green planted, red lifted), the hands (yellow), the weapon tip (orange), the shield
     * rim (cyan). Returns the planted threshold's state per sole.
     */
    void marks(JsonArray m, Snap s, Ent en, Geo g, int[] crop, double camYaw, boolean ruler) {
        double y = Math.toRadians(camYaw), rx = -Math.cos(y), rz = -Math.sin(y), gy = site.getY() - en.pos.y;
        line(m, screen(s, en.pos, new double[]{-1.6 * rx, gy, -1.6 * rz}), screen(s, en.pos, new double[]{1.6 * rx, gy, 1.6 * rz}), crop, C_GROUND, 1);
        if (ruler) {
            double R = Math.min(cloudR, 1.6) + 0.3;
            for (int k = 0; k * 0.5 <= cloudTop + 0.01; k++) {
                double[] p = screen(s, en.pos, new double[]{-R * rx, gy + k * 0.5, -R * rz}); if (p == null) continue;
                line(m, new double[]{p[0] - (k % 2 == 0 ? 10 : 6), p[1]}, new double[]{p[0] + (k % 2 == 0 ? 10 : 6), p[1]}, crop, C_GROUND, 1);
                text(m, new double[]{p[0] - 34, p[1] - 6}, crop, String.format(Locale.ROOT, "%.1f", k * 0.5), C_GROUND);
            }
        }
        for (int i = 0; i < 2; i++) if (g.soles[i] != null) {
            double[] o = g.soles[i];
            line(m, screen(s, en.pos, new double[]{o[0] - 0.12 * rx, gy, o[2] - 0.12 * rz}), screen(s, en.pos, new double[]{o[0] + 0.12 * rx, gy, o[2] + 0.12 * rz}), crop, C_GTICK, 2);
            dot(m, screen(s, en.pos, o), crop, soleColour(en.pos.y + o[1] - site.getY()), 4);
        }
        for (int i = 0; i < 2; i++) dot(m, screen(s, en.pos, g.hands[i]), crop, C_HAND, 3);
        dot(m, screen(s, en.pos, g.tip), crop, C_TIP, 4);
        dot(m, screen(s, en.pos, g.rim), crop, C_RIM, 4);
    }
    /** The feet crop (round the soles, the ground 70 percent down) and the hands and weapon crop of a troop on a frame, in the saved image's pixels. */
    void crops(JsonObject e, Snap s, Ent en, Geo g, int[] crop) {
        double k = ppb(s, en.pos); double[] a = screen(s, en.pos, g.soles[0]), b = screen(s, en.pos, g.soles[1]);
        double[] gr = screen(s, en.pos, new double[]{0, site.getY() - en.pos.y, 0});
        double cx = a != null && b != null ? (a[0] + b[0]) / 2 : a != null ? a[0] : b != null ? b[0] : gr == null ? crop[0] + crop[2] / 2.0 : gr[0];
        double gy = gr == null ? crop[1] + crop[3] * 0.9 : gr[1];
        var sh = new JsonArray(); for (int i = 0; i < 2; i++) { if (g.soles[i] == null) sh.add(com.google.gson.JsonNull.INSTANCE); else sh.add(r4(en.pos.y + g.soles[i][1] - site.getY())); } e.add("soleHeight", sh);
        double fw = 1.6 * k, fh = 0.8 * k, top = gy - fh * 0.7;
        for (double[] q : new double[][]{a, b}) if (q != null) top = Math.min(top, q[1] - 0.2 * k); // a raised sole (a jump, a body lying) stays in the crop
        e.add("feet", rect(cx - fw / 2 - crop[0], top - crop[1], fw, gy + fh * 0.3 - top, crop));
        double x0 = 1e9, y0 = 1e9, x1 = -1e9, y1 = -1e9;
        for (double[] p : new double[][]{g.hands[0], g.hands[1], g.tip, g.rim}) { double[] q = screen(s, en.pos, p); if (q == null) continue; x0 = Math.min(x0, q[0]); y0 = Math.min(y0, q[1]); x1 = Math.max(x1, q[0]); y1 = Math.max(y1, q[1]); }
        if (x0 > x1) { x0 = crop[0]; y0 = crop[1]; x1 = crop[0] + crop[2]; y1 = crop[1] + crop[3]; }
        double pad = 0.3 * k, w = Math.max(x1 - x0 + 2 * pad, k), h = Math.max(y1 - y0 + 2 * pad, k), mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
        var hr = rect(mx - w / 2 - crop[0], my - h / 2 - crop[1], w, h, crop); e.add("hands", hr); e.addProperty("handsScale", hr.get(2).getAsInt() < 260 ? 2 : 1);
    }
    static JsonArray rect(double x, double y, double w, double h, int[] crop) {
        int W = crop[2], H = crop[3]; int iw = (int) Math.min(W, Math.round(w)), ih = (int) Math.min(H, Math.round(h));
        int ix = (int) Math.max(0, Math.min(W - iw, Math.round(x))), iy = (int) Math.max(0, Math.min(H - ih, Math.round(y)));
        var a = new JsonArray(); a.add(ix); a.add(iy); a.add(Math.max(1, iw)); a.add(Math.max(1, ih)); return a;
    }
    /** R74 check: every marked point of a solo or loco frame lies in its crop. */
    void framing(String file, JsonArray m, int[] crop) {
        framingChecked++;
        for (var x : m) {
            var j = x.getAsJsonObject(); if (!j.get("t").getAsString().equals("dot")) continue;
            var p = j.getAsJsonArray("p"); double px = p.get(0).getAsDouble(), py = p.get(1).getAsDouble();
            if (px < 0 || py < 0 || px > crop[2] || py > crop[3]) { if (framingBad.size() < 12) framingBad.add(file + " (" + j.get("c").getAsString() + " at " + px + "," + py + ")"); else if (framingBad.size() == 12) framingBad.add("..."); return; }
        }
    }

    // ---- the files ----
    JsonObject entry(String kind, String clip, String state, String view, int frame, int angle, double t) {
        var j = new JsonObject(); j.addProperty("kind", kind); if (clip != null) j.addProperty("clip", clip); if (state != null) j.addProperty("state", state);
        if (view != null) j.addProperty("view", view); if (frame >= 0) j.addProperty("frame", frame); if (angle >= 0) j.addProperty("angle", angle);
        if (!Double.isNaN(t)) j.addProperty("t", Math.round(t * 10000) / 10000.0);
        return j;
    }
    NativeImage frameImage(MinecraftClient c) { if (frameImg == null) frameImg = ScreenshotRecorder.takeScreenshot(c.getFramebuffer()); return frameImg; }
    /** The last drawn frame's crop at native resolution (scaled by an integer, nearest neighbour, when scale > 1), written on the writer threads. */
    void save(MinecraftClient c, String rel, int[] r, int scale, JsonObject entry) {
        if (c.currentScreen != null && covered.size() < 20) covered.add(rel + " (" + c.currentScreen.getClass().getSimpleName() + " open)"); // a screen over the world: the battle ended or a menu opened
        NativeImage img = frameImage(c); int W = img.getWidth(), H = img.getHeight();
        int x0 = Math.max(0, Math.min(W - 1, r[0])), y0 = Math.max(0, Math.min(H - 1, r[1])), w = Math.max(1, Math.min(r[2], W - x0)), h = Math.max(1, Math.min(r[3], H - y0));
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) { int p = img.getColor(x0 + x, y0 + y); px[y * w + x] = (p & 0xFF) << 16 | (p & 0xFF00) | (p >> 16 & 0xFF); }
        entry.addProperty("file", rel); entry.addProperty("source", x0 + "," + y0 + "," + w + "x" + h + " of " + W + "x" + H);
        entry.addProperty("width", w * scale); entry.addProperty("height", h * scale); if (scale > 1) entry.addProperty("scale", scale);
        files.add(entry); byFile.put(rel, entry); writing.incrementAndGet();
        final File file = new File(out, rel); final int fw = w, fh = h;
        io.submit(() -> { try { write(px, fw, fh, scale, file); } catch (Throwable x) { writeErrors.add(rel + ": " + x); } finally { writing.decrementAndGet(); } });
    }
    static void write(int[] px, int w, int h, int scale, File file) throws IOException {
        BufferedImage src = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB); src.setRGB(0, 0, w, h, px, 0, w); BufferedImage dst = src;
        if (scale > 1) { dst = new BufferedImage(w * scale, h * scale, BufferedImage.TYPE_INT_RGB); var g = dst.createGraphics(); g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); g.drawImage(src, 0, 0, w * scale, h * scale, null); g.dispose(); }
        file.getParentFile().mkdirs(); ImageIO.write(dst, "png", file);
    }
    /** A fresh render since the last tick, or null. */
    Snap fresh() { Snap s = LAST; if (s == null || s.n == lastSnapN) return null; lastSnapN = s.n; return s; }

    // ---- solo: every clip from 8 angles, the full pipeline ----
    void solo(MinecraftClient c) {
        if (ji >= jobs.size()) { finishSolo(); next(c); return; }
        if (awayFor > 0) { litStart(c); return; }
        Job j = jobs.get(ji); wait++;
        Entity e = client(c, cast.get(SOLO_UNIT).mob);
        switch (j.ph) {
            case 0 -> {
                if (wait == 1) { int tok = ++req; scenes(c, null); raised.clear(); server(c, b -> { respawn(b); layout(b, L_SOLO, 1.8, 0, YAW); done = tok; }); setCam(soloCam(j.angle, soloD)); held = 0; return; }
                if (done != req) { if (wait > 400) { bad.add(j.clip + " a" + j.angle + ": the server never placed the troop"); endJob(c, j, e); } return; }
                if (e != null) TroopModels.forceFace(e, 0);
                inject(c, j.pre);
                boolean ok = e != null && e.isAlive() && placed(c, L_SOLO, 1.8, 0) && camOk(); held = ok ? held + 1 : 0;
                if (held >= 2 || wait > 600) { if (held < 2) bad.add(j.clip + " a" + j.angle + ": started anyway after 600 ticks (troop " + (e != null) + ", camera " + camOk() + ")"); j.ph = 1; wait = 0; }
            }
            case 1 -> {
                inject(c, j.pre);
                if (wait < j.settle || !shotReady(c, j.clip + " a" + j.angle)) return;
                if (j.cat.equals("hit")) primeHit(e, j.hitIdx, j.hitN);
                fireSolo(c, j, e); j.ph = 2; wait = 0; lastSnapN = LAST == null ? -1 : LAST.n;
            }
            case 2 -> soloTick(c, j, e);
            default -> {}
        }
    }
    /** H1 picks a seeded hit variant from the unit's hit count: set the count so the next hit plays variant idx of n (the solo job grades one clip). */
    static void primeHit(Entity e, int idx, int n) {
        var st = e == null ? null : TroopMotion.state(e.getId()); if (st == null) return;
        var id = e.getUuid(); long bias = MotionMath.seed(id.getMostSignificantBits(), id.getLeastSignificantBits()).variantBias();
        java.util.Arrays.fill(st.lastHitVariant, -1);
        int k = st.hitCount + 1; while (MotionEngine.hitVariant(bias, k, -1, n) != idx && k < st.hitCount + 1000) k++;
        st.hitCount = k - 1;
    }
    void fireSolo(MinecraftClient c, Job j, Entity e) {
        if (e == null) return; long tick = c.world.getTime(); int id = e.getId();
        switch (j.cat) {
            case "attack" -> { if (j.variant < 0) replayBad.add(j.clip + ": no attack variant plays it"); else MotionEvents.Client.attack(id, j.variant, tick, Tactics.variants(famName).size()); }
            case "hit" -> MotionEvents.Client.hit(true, id, MotionEvents.KINDS.indexOf("slash"), MotionEvents.ZONES.indexOf(j.zone), 0f, 1, j.blocked ? 1 : 0, tick);
            case "scene" -> TroopMotion.setScenePose(id, j.clip);
            case "death" -> kill(c, List.of(cast.get(SOLO_UNIT)), j.bearing, j.kind);
            default -> {}
        }
    }
    /** The clip time of a job's clip in a drawn frame (NaN when the engine is not playing it). */
    double clipTime(Job j, Ent en) {
        if (Double.isNaN(en.now)) return Double.NaN;
        switch (j.cat) {
            case "stance" -> {
                double w = 0; for (int i = 0; i < en.stanceClips.size(); i++) if (en.stanceClips.get(i) == j.c) w += en.stanceW.get(i);
                j.stanceW = Math.max(j.stanceW, w); if (w < 0.5) return Double.NaN;
                double L = j.c.length(), t = en.now * en.tempo + en.phase / MotionMath.TAU * L; return ((t % L) + L) % L;
            }
            case "scene" -> { return j.clip.equals(en.sceneClip) ? en.now - en.sceneStart : Double.NaN; }
            default -> {
                MotionEngine.Playing best = null;
                for (var p : en.tracks) { j.played.add(keyOf(p.clip())); if (p.clip() == j.c) return p.time(); if (best == null || p.gain() > best.gain()) best = p; }
                return Double.NaN;
            }
        }
    }
    void soloTick(MinecraftClient c, Job j, Entity e) {
        if (!j.cat.equals("death")) inject(c, j.pre);
        Snap s = fresh(); Ent en = s == null || e == null ? null : s.ents.get(e.getId());
        if (en != null && en.pose != null) {
            double t = clipTime(j, en);
            if (Double.isNaN(t)) j.goneFor++; else { j.seen = true; j.goneFor = 0; }
            Geo g = null;
            if (!Double.isNaN(t)) {
                g = geo(en.pose, en.bodyYaw);
                if (j.full) { double[] tp = screen(s, en.pos, g.tip), rp = screen(s, en.pos, g.rim); if (tp != null) j.tip.add(xy(tp, soloCrop)); if (rp != null) j.rim.add(xy(rp, soloCrop)); }
                double L = j.c.length();
                for (int k = 0; k < j.times.length; k++) {
                    if (j.got[k]) continue; boolean take;
                    if (j.loop()) { if (Double.isNaN(j.lastT)) take = false; else { double step = ((t - j.lastT) % L + L) % L, fd = ((t - j.times[k]) % L + L) % L; take = step > 1e-9 && fd < step; } }
                    else take = t >= j.times[k] - 1e-6;
                    if (!take) continue;
                    j.got[k] = true;
                    var x = entry(j.kinds.get(k), j.clip, clipState.get(j.clip), j.angle == 0 ? "front" : j.angle == 90 ? "side" : "turn", j.frameNo.get(k), j.angle, t);
                    x.addProperty("target", j.names.get(k)); x.addProperty("targetTime", j.times[k]); x.addProperty("clipTime", t); x.addProperty("engineNow", en.now);
                    x.addProperty("drawCount", en.count); x.addProperty("deathTime", en.deathTime); x.addProperty("pipeline", "full (TroopMotion, engine layers, MotionFeet)");
                    if (j.loop()) x.addProperty("stanceWeight", j.stanceW);
                    var m = new JsonArray(); marks(m, s, en, g, soloCrop, j.angle, true); x.add("marks", m); crops(x, s, en, g, soloCrop);
                    framing(j.files.get(k), m, soloCrop);
                    save(c, j.files.get(k), soloCrop, 1, x);
                }
                j.lastT = t;
            }
            boolean all = true; for (boolean b : j.got) all &= b;
            boolean endNow = switch (j.cat) {
                case "stance" -> all;
                case "attack", "hit" -> j.seen && j.goneFor >= 8;
                default -> !Double.isNaN(t) && t >= j.c.length() + 0.4 || j.seen && j.goneFor >= 8;
            };
            if (endNow) { endJob(c, j, e); return; }
        }
        if (wait > j.maxTicks) endJob(c, j, e);
    }
    void endJob(MinecraftClient c, Job j, Entity e) {
        List<String> miss = new ArrayList<>(); for (int k = 0; k < j.times.length; k++) if (!j.got[k]) miss.add(j.names.get(k));
        if (!j.seen) replayBad.add(j.clip + " a" + j.angle + ": the engine never played it (played " + j.played + ")");
        else if (!miss.isEmpty()) replayBad.add(j.clip + " a" + j.angle + ": no frame for " + miss);
        if (j.cat.equals("scene") && e != null) TroopMotion.setScenePose(e.getId(), null);
        if (j.full) { var t = new JsonObject(); t.add("tip", j.tip); t.add("rim", j.rim); trails.computeIfAbsent(j.clip, k -> new JsonObject[2])[j.angle == 0 ? 0 : 1] = t; }
        ji++; wait = 0;
    }
    void finishSolo() {
        report("mo-grade-replays", replayBad.isEmpty() && !jobs.isEmpty(), jobs.size() + " solo jobs (" + clipOrder.size() + " clips x 8 angles), each clip asked for at the input level (stance packet, attack or hit event, scene pose, real kill) and caught in the engine's own stance layers, tracks or scene as drawn: 8 frames front and side, the key poses at every angle" + (replayBad.isEmpty() ? "" : "; off: " + replayBad));
        report("mo-grade-motion", motionBad.isEmpty() && !clipOrder.isEmpty(), clipOrder.size() + " clips, each one's 8 frame times: " + (motionBad.isEmpty() ? String.format(Locale.ROOT, "some two differ by more than 1 degree in every clip (least %.2f degrees, %s)", motionLeast, motionLeastClip) : "one pose throughout: " + motionBad));
        report("mo-grade-framing", framingBad.isEmpty() && framingChecked > 0, framingChecked + " solo and loco frames framed from the measured extent of every clip (body, weapon and shield; camera " + String.format(Locale.ROOT, "%.2f", soloD) + " blocks, crop " + soloCrop[2] + "x" + soloCrop[3] + " native, 10 percent margin)" + (framingBad.isEmpty() ? ": every sole, hand, weapon tip and shield rim on its crop" : "; off the crop: " + framingBad));
        if (!bad.isEmpty()) System.out.println("[mo-grade] solo notes: " + bad);
    }

    // ---- loco: walk, run, crawl, backpedal and a turn, tracked from the side ----
    final List<double[]> lrec = new ArrayList<>(); // per frame: serverIdx, x, z, rx, ry, rz, lx, ly, lz, now, engine plant weight
    final List<float[]> lpose = new ArrayList<>(); final List<Double> lnow = new ArrayList<>();
    /** R83: per frame, each foot's drawn shin corners (x, height, z in blocks from the site), or null when not drawn. */
    final List<double[][][]> lcorn = new ArrayList<>();
    void trackCam(Entity e) {
        if (e == null) return; double off = soloD;
        cam = new double[]{e.getX(), site.getY() + soloEye, e.getZ() + off, 180, 0, e.prevX, e.prevZ + off};
    }
    void loco(MinecraftClient c) {
        if (li >= segs.size()) { finishLoco(); next(c); return; }
        if (awayFor > 0) { litStart(c); return; }
        Seg g = segs.get(li); wait++; var u = cast.get(SOLO_UNIT); Entity e = client(c, u.mob);
        double x0 = g.path()[0][0]; float y0 = (float) g.path()[0][2];
        switch (lph) {
            case 0 -> {
                if (wait == 1) { int tok = ++req; final boolean up = g.raised(); server(c, b -> { locoPath = null; locoI = -1; respawn(b); layout(b, L_LOCO, 1.8, x0, y0); raised.clear(); if (up) raised.add(cast.get(SOLO_UNIT).mob.getId()); done = tok; }); held = 0; return; }
                if (done != req) return;
                if (e != null) TroopModels.forceFace(e, 0);
                inject(c, g.stance()); trackCam(e);
                boolean ok = e != null && e.isAlive() && placed(c, L_LOCO, 1.8, x0); held = ok ? held + 1 : 0;
                if (held >= 25 && shotReady(c, "loco " + g.name())) {
                    final double[][] p = g.path(); server(c, b -> { locoPath = p; locoI = 0; });
                    lph = 1; wait = 0; tail = 0; lrec.clear(); lpose.clear(); lnow.clear(); lcorn.clear(); lastSnapN = LAST == null ? -1 : LAST.n;
                } else if (wait > 900) { locoBad.add(g.name() + ": never placed"); li++; wait = 0; }
            }
            case 1 -> {
                inject(c, g.stance()); trackCam(e);
                Snap s = fresh(); Ent en = s == null || e == null ? null : s.ents.get(e.getId());
                int idx = locoI;
                if (en != null && en.pose != null && idx >= 0) {
                    Geo q = geo(en.pose, en.bodyYaw); int k = lrec.size();
                    double[] r = q.soles[0], l = q.soles[1];
                    lrec.add(new double[]{idx, en.pos.x - site.getX() - .5, en.pos.z - site.getZ() - .5,
                            r == null ? Double.NaN : en.pos.x + r[0] - site.getX() - .5, r == null ? Double.NaN : en.pos.y + r[1] - site.getY(), r == null ? Double.NaN : en.pos.z + r[2] - site.getZ() - .5,
                            l == null ? Double.NaN : en.pos.x + l[0] - site.getX() - .5, l == null ? Double.NaN : en.pos.y + l[1] - site.getY(), l == null ? Double.NaN : en.pos.z + l[2] - site.getZ() - .5, en.now, en.pw});
                    lpose.add(en.pose); lnow.add(en.now);
                    double[][][] fc = new double[2][][];
                    for (int i = 0; i < 2; i++) if (q.feet[i] != null) { fc[i] = new double[q.feet[i].length][]; for (int v = 0; v < fc[i].length; v++) { double[] p = q.feet[i][v]; fc[i][v] = new double[]{en.pos.x + p[0] - site.getX() - .5, en.pos.y + p[1] - site.getY(), en.pos.z + p[2] - site.getZ() - .5}; } }
                    lcorn.add(fc);
                    String rel = String.format(Locale.ROOT, "loco/%s/f%03d.png", g.name(), k);
                    var x = entry("loco_frame", null, null, "side-tracking", k, 90, Double.NaN); x.addProperty("segment", g.name()); x.addProperty("serverTick", idx);
                    x.addProperty("phase", idx < 15 ? "idle" : idx < 15 + g.move() ? (g.turn() ? "turn" : "move") : "stop"); x.addProperty("engineNow", en.now);
                    x.addProperty("bodyYaw", en.bodyYaw); x.addProperty("moving", en.m); x.addProperty("running", en.run); x.addProperty("shuffle", en.sh); x.addProperty("backpedal", en.bp); x.addProperty("turning", en.tw); x.addProperty("planted", en.plant); x.addProperty("solverPw", en.pw);
                    var m = new JsonArray(); marks(m, s, en, q, locoCrop, 180, true); x.add("marks", m); crops(x, s, en, q, locoCrop); framing(rel, m, locoCrop);
                    save(c, rel, locoCrop, 1, x);
                }
                if (idx >= g.path().length && ++tail > 12 || wait > g.path().length + 300) {
                    if (wait > g.path().length + 300) locoBad.add(g.name() + ": the path did not finish (server tick " + idx + ")");
                    analyseLoco(g); server(c, b -> { locoPath = null; locoI = -1; });
                    lph = 0; li++; wait = 0;
                }
            }
            default -> {}
        }
    }
    /** R71: the planted slide, foot heights and sink of a segment, and its traces for the plot. */
    void analyseLoco(Seg g) {
        int n = lrec.size(); var j = new JsonObject(); j.addProperty("name", g.name()); j.addProperty("title", g.title()); j.addProperty("stance", g.stance()); j.addProperty("shieldRaised", g.raised());
        j.addProperty("speedBlocksPerTick", g.speed()); j.addProperty("yaw", g.yaw()); j.addProperty("frames", n);
        var ph = new JsonObject(); ph.addProperty("idleTicks", 15); ph.addProperty(g.turn() ? "turnTicks" : "moveTicks", g.move()); ph.addProperty("pathTicks", g.path().length); j.add("serverTicks", ph);
        if (n < 10) { locoBad.add(g.name() + ": " + n + " frames"); locoJson.add(j); return; }
        String[] side = {"right", "left"}; var slide = new JsonArray(); var maxPlant = new JsonArray(); var planted = new JsonArray(); var minH = new JsonArray(); var maxH = new JsonArray(); var rest = new JsonArray(); var thr = new JsonArray();
        var moveSlide = new JsonArray(); var centroidSlide = new JsonArray(); var plants = new JsonArray(); var plantCount = new JsonArray();
        double[][] ser = new double[6][n]; double[][] spans = new double[2][n];
        boolean[] afterIdle = new boolean[n]; for (int k = 0; k < n; k++) afterIdle[k] = lrec.get(k)[0] >= 15;
        for (int i = 0; i < 2; i++) {
            int o = 3 + 3 * i; List<Double> idle = new ArrayList<>(); for (double[] r : lrec) if (r[0] < 15 && !Double.isNaN(r[o + 1])) idle.add(r[o + 1]);
            java.util.Collections.sort(idle); double rh = idle.isEmpty() ? 0 : idle.get(idle.size() / 2), th = Math.max(PLANT_ABOVE, rh + PLANT_ABOVE);
            double cen = 0, lo = 1e9, hi = -1e9; int pl = 0; boolean[] isPl = new boolean[n]; double[][][] fc = new double[n][][];
            for (int k = 0; k < n; k++) {
                double[] r = lrec.get(k); double h = r[o + 1]; ser[2 * i][k] = r[o]; ser[2 * i + 1][k] = h; fc[k] = k < lcorn.size() ? lcorn.get(k)[i] : null; if (Double.isNaN(h)) continue;
                lo = Math.min(lo, h); hi = Math.max(hi, h); boolean p = h <= th; isPl[k] = p; if (p) pl++;
                if (k > 0) { double[] q = lrec.get(k - 1); if (p && !Double.isNaN(q[o + 1]) && q[o + 1] <= th) cen += Math.hypot(r[o] - q[o], r[o + 2] - q[o + 2]); }
            }
            // R83: the slide is measured at the corners the foot stands on, per plant; the totals and the largest come from the plant list
            double tot = 0, mv = 0, mx = 0; int cnt = 0;
            for (var p : SoleSlide.plants(i, isPl, fc, afterIdle)) {
                double eng = 0; for (int k = p.startFrame(); k < p.startFrame() + p.frames(); k++) { eng += lrec.get(k).length > 10 ? lrec.get(k)[10] : Double.NaN; spans[i][k] = 1; }
                var pj = new JsonObject(); pj.addProperty("segment", g.name()); pj.addProperty("foot", i == 0 ? "right" : "left"); pj.addProperty("startFrame", p.startFrame()); pj.addProperty("startServerTick", (int) lrec.get(p.startFrame())[0]);
                pj.addProperty("frames", p.frames()); pj.addProperty("slideBlocks", r4(p.slide())); pj.addProperty("slideAfterIdleBlocks", r4(p.slideAfterIdle())); pj.addProperty("enginePlantWeightMean", r4(eng / p.frames()));
                plants.add(pj); tot += p.slide(); mv += p.slideAfterIdle(); mx = Math.max(mx, p.slide()); cnt++;
            }
            slide.add(r4(tot)); moveSlide.add(r4(mv)); maxPlant.add(r4(mx)); planted.add(pl); minH.add(r4(lo)); maxH.add(r4(hi)); rest.add(r4(rh)); thr.add(r4(th)); centroidSlide.add(r4(cen)); plantCount.add(cnt);
        }
        j.add("plantedSlideBlocks", slide); j.add("plantedSlideAfterIdleBlocks", moveSlide); j.add("largestSinglePlantSlideBlocks", maxPlant); j.add("plantedFrames", planted); j.add("plantCount", plantCount);
        j.add("restSoleHeight", rest); j.add("plantedThreshold", thr); j.add("lowestSoleHeight", minH); j.add("highestSoleHeight", maxH); j.add("soleCentroidSlideBlocks", centroidSlide); j.add("plants", plants);
        j.addProperty("sides", "right, left"); j.addProperty("note", "sole height = the lowest corners of the shin's boxes as drawn; planted = sole height within 0.04 blocks of its idle rest height (at least 0.04); a plant = a run of consecutive planted frames of one foot; slide (R83) = per step, the smallest horizontal move of a corner touching the ground (within " + SoleSlide.CONTACT + " of the lowest) in both frames, summed over a plant, so a box foot rocking heel to toe reads as no slide; plantedSlideBlocks, plantedSlideAfterIdleBlocks and largestSinglePlantSlideBlocks are the sums and the largest of plants[]; soleCentroidSlideBlocks is the old centroid measure, kept for comparison; enginePlantWeightMean is MotionEngine's planted-feet weight over the plant; a negative lowest height is sinking into the ground");
        double moved = Math.hypot(lrec.get(n - 1)[1] - lrec.get(0)[1], lrec.get(n - 1)[2] - lrec.get(0)[2]); j.addProperty("bodyMovedBlocks", r4(moved));
        var tr = new JsonObject(); var fx = new JsonArray(); var bx = new JsonArray(); var tick = new JsonArray();
        for (int k = 0; k < n; k++) { fx.add(k); bx.add(r4(lrec.get(k)[1])); tick.add((int) lrec.get(k)[0]); }
        tr.add("frame", fx); tr.add("serverTick", tick); tr.add("bodyX", bx);
        String[] nm = {"rightX", "rightHeight", "leftX", "leftHeight"}; for (int s = 0; s < 4; s++) { var a = new JsonArray(); for (int k = 0; k < n; k++) a.add(Double.isNaN(ser[s][k]) ? 0 : r4(ser[s][k])); tr.add(nm[s], a); }
        var rz = new JsonArray(); var lz = new JsonArray(); for (int k = 0; k < n; k++) { rz.add(r4(nz(lrec.get(k)[5]))); lz.add(r4(nz(lrec.get(k)[8]))); } tr.add("rightZ", rz); tr.add("leftZ", lz);
        var rp = new JsonArray(); var lp = new JsonArray(); var ep = new JsonArray();
        for (int k = 0; k < n; k++) { rp.add(spans[0][k]); lp.add(spans[1][k]); double[] r = lrec.get(k); ep.add(r.length > 10 ? r4(r[10]) : 0); }
        tr.add("rightPlanted", rp); tr.add("leftPlanted", lp); tr.add("enginePlantWeight", ep);
        j.add("trace", tr);
        int start = -1, stop = -1; for (int k = 0; k < n; k++) { if (start < 0 && lrec.get(k)[0] >= 15) start = k; if (stop < 0 && lrec.get(k)[0] >= 15 + g.move()) stop = k; }
        j.addProperty("startFrame", start); j.addProperty("stopFrame", stop);
        int[] ev = start < 0 ? new int[0] : stop < 0 ? new int[]{start} : new int[]{start, stop};
        String[] en = g.turn() ? new String[]{"turn starts", "turn held"} : new String[]{"idle to move", "move to stop"};
        j.add("delta", delta(lpose, lnow, ev, en, null));
        locoJson.add(j);
        System.out.println("[mo-grade] loco " + g.name() + ": " + n + " frames, planted slide " + slide + " blocks (largest single plant " + maxPlant + "), lowest sole " + minH + ", moved " + r4(moved));
    }
    static double nz(double v) { return Double.isNaN(v) ? 0 : v; }
    static double r4(double v) { return Math.round(v * 10000) / 10000.0; }
    void finishLoco() {
        // R83: the sole tracker's synthetic pin (a rocking foot reads as no slide, a sliding one as its true slide)
        try { report("mo-grade-sole", true, "slide measured at the corners each foot stands on, per plant: " + SoleSlide.selfCheck()); }
        catch (AssertionError x) { report("mo-grade-sole", false, "the sole tracker's synthetic check failed: " + x.getMessage()); }
        boolean ok = locoBad.isEmpty() && locoJson.size() == segs.size();
        StringBuilder sb = new StringBuilder();
        for (var x : locoJson) { var j = x.getAsJsonObject(); if (j.has("plantedSlideBlocks")) sb.append(j.get("name").getAsString()).append(" slide ").append(j.get("plantedSlideBlocks")).append(" over ").append(j.get("plantCount")).append(" plants, largest ").append(j.get("largestSinglePlantSlideBlocks")).append(" lowest ").append(j.get("lowestSoleHeight")).append("; "); }
        report("mo-grade-loco", ok, segs.size() + " segments (walk, run, shield crawl, backpedal, 90 degree turn), each from idle and back to idle, a side camera tracking at " + String.format(Locale.ROOT, "%.2f", soloD) + " blocks, every frame with the ground line, soles and feet crop; planted slide and sole heights in the manifest: " + sb + (locoBad.isEmpty() ? "" : "off: " + locoBad));
    }

    // ---- delta: the per-tick pose change of one troop ----
    /**
     * R72: per drawn frame, each bone's rotation change (degrees, the three angles' absolute changes summed) normalised to one tick of
     * engine time; the sum over bones, the largest bone and the largest pivot move (px); and the second difference (summed absolute
     * change of every angle's per-tick change, degrees per tick squared). Each event's window (E-10 to E+10) gives its largest step
     * and second difference against the p90 of the second difference outside every window; flagged above 3 times that and 10.
     */
    JsonObject delta(List<float[]> poses, List<Double> nows, int[] ev, String[] evName, String refClip) {
        int n = poses.size(); double[] sum = new double[n], mb = new double[n], tr = new double[n], acc = new double[n]; String[] mbName = new String[n];
        List<String> bones = model.boneOrder(); double[] prevD = null;
        for (int k = 1; k < n; k++) {
            float[] a = poses.get(k - 1), b = poses.get(k); if (a == null || b == null || a.length != b.length) { prevD = null; continue; }
            double dn = nows.get(k) - nows.get(k - 1), sc = dn > 1e-4 ? 0.05 / dn : 1; double[] d = new double[a.length / 6 * 3];
            for (int i = 0; i < a.length / 6; i++) {
                double ang = 0;
                for (int ch = 0; ch < 3; ch++) { double v = MathHelper.wrapDegrees(Math.toDegrees(b[i * 6 + 3 + ch] - a[i * 6 + 3 + ch])) * sc; d[i * 3 + ch] = v; ang += Math.abs(v); }
                sum[k] += ang; if (ang > mb[k]) { mb[k] = ang; mbName[k] = i < bones.size() ? bones.get(i) : "#" + i; }
                for (int ch = 0; ch < 3; ch++) tr[k] = Math.max(tr[k], Math.abs(b[i * 6 + ch] - a[i * 6 + ch]) * sc);
            }
            if (prevD != null) { double s = 0; for (int i = 0; i < d.length; i++) s += Math.abs(d[i] - prevD[i]); acc[k] = s; }
            prevD = d;
        }
        boolean[] inWin = new boolean[n]; for (int e : ev) for (int k = Math.max(0, e - DENSE); k <= Math.min(n - 1, e + DENSE); k++) inWin[k] = true;
        List<Double> base = new ArrayList<>(); for (int k = 2; k < n; k++) if (!inWin[k]) base.add(acc[k]);
        if (base.size() < 10) { base.clear(); for (int k = 2; k < n; k++) base.add(acc[k]); }
        java.util.Collections.sort(base); double p90 = base.isEmpty() ? 0 : base.get(Math.min(base.size() - 1, (int) Math.floor(base.size() * 0.9)));
        // D3: the clip's own authored peaks (its poses sampled every tick), so a fast authored action is not flagged as a pop
        double refBone = 0, refAcc = 0, refSum = 0;
        if (refClip != null && fam.all().containsKey(refClip)) {
            Clip rc = fam.all().get(refClip); double[] pd = null; float[] pp = null;
            for (double t = 0; t <= rc.length() + 1e-9; t += 0.05) {
                float[] cur = want(model, refClip, Math.min(t, rc.length()), shieldTroop); double[] d = new double[cur.length / 6 * 3];
                if (pp != null && pp.length == cur.length) {
                    double sm = 0; for (int i = 0; i < cur.length / 6; i++) { double a = 0; for (int ch = 0; ch < 3; ch++) { double v = MathHelper.wrapDegrees(Math.toDegrees(cur[i * 6 + 3 + ch] - pp[i * 6 + 3 + ch])); d[i * 3 + ch] = v; a += Math.abs(v); } sm += a; refBone = Math.max(refBone, a); }
                    refSum = Math.max(refSum, sm);
                    if (pd != null) { double q = 0; for (int i = 0; i < d.length; i++) q += Math.abs(d[i] - pd[i]); refAcc = Math.max(refAcc, q); }
                    pd = d;
                }
                pp = cur;
            }
        }
        var j = new JsonObject(); j.addProperty("authoredPeakSumDegPerTick", r4(refSum)); j.addProperty("authoredPeakBoneDegPerTick", r4(refBone)); j.addProperty("authoredPeakSecondDiff", r4(refAcc));
        j.add("sumDegPerTick", nums(sum)); j.add("maxBoneDegPerTick", nums(mb)); j.add("maxPivotPxPerTick", nums(tr)); j.add("secondDiffDegPerTick2", nums(acc));
        j.addProperty("baselineSecondDiffP90", r4(p90)); j.addProperty("flagRule", "second difference above 3 x the p90 outside the event windows, above 10 degrees per tick squared, and (for a clip) above 1.2 x the clip's own authored peak second difference; largestStepDegPerTick is the SUM over bones, worstBone* is the single worst bone in the window"); j.addProperty("windowRule", "an event owns frames E-10 to E+10, cut where the previous event's frames end and the next event's frames begin (frame k shows events fired after frames below k)");
        var w = new JsonArray(); JsonObject largest = null; double big = -1;
        for (int q = 0; q < ev.length; q++) {
            // frame k can show the events fired after frames below k, so a window ends where the next event's frames begin and starts after the previous event's
            int e = ev[q], lo = Math.max(1, Math.max(e - DENSE, q > 0 ? ev[q - 1] + 1 : 0)), hi = Math.min(n - 1, Math.min(e + DENSE, q + 1 < ev.length ? ev[q + 1] : Integer.MAX_VALUE)), at = lo, aa = lo; double ms = -1, ma = -1;
            for (int k = lo; k <= hi; k++) { if (sum[k] > ms) { ms = sum[k]; at = k; } if (acc[k] > ma) { ma = acc[k]; aa = k; } }
            int wb = lo; for (int k = lo; k <= hi; k++) if (mb[k] > mb[wb]) wb = k;
            var x = new JsonObject(); x.addProperty("worstBone", mbName[wb] == null ? "" : mbName[wb]); x.addProperty("worstBoneDegPerTick", r4(mb[wb])); x.addProperty("worstBoneFrame", wb); x.addProperty("event", evName[q]); x.addProperty("afterFrame", e); x.addProperty("window", lo + "-" + hi);
            x.addProperty("largestStepDegPerTick", r4(ms)); x.addProperty("largestStepFrame", at); x.addProperty("largestBoneThere", mbName[at] == null ? "" : mbName[at]); x.addProperty("largestBoneDegPerTick", r4(mb[at]));
            x.addProperty("largestSecondDiff", r4(ma)); x.addProperty("largestSecondDiffFrame", aa); x.addProperty("ratioToBaseline", r4(ma / Math.max(p90, 1e-6)));
            x.addProperty("flagged", ma > 3 * p90 && ma > 10 && ma > 1.2 * refAcc); x.addProperty("ratioToAuthored", refAcc > 0 ? r4(ma / refAcc) : -1); w.add(x);
            if (ms > big) { big = ms; largest = x; }
        }
        j.add("windows", w); if (largest != null) j.add("largestJump", largest);
        return j;
    }
    static JsonArray nums(double[] v) { var a = new JsonArray(); for (double x : v) a.add(Math.round(x * 100) / 100.0); return a; }

    // ---- states: wide overview, formation (high and level), close trio ----
    void states(MinecraftClient c) {
        if (sti >= states.size()) { finishStates(); next(c); return; }
        if (awayFor > 0) { litStart(c); return; }
        St s = states.get(sti); int mode = PASSES[pass]; wait++;
        switch (ps) {
            case 0 -> {
                if (wait == 1) {
                    double cell = cellFor(s.stance()); int tok = ++req; scenes(c, null); raised.clear();
                    for (var u : cast) { var e = client(c, u.mob); if (e != null) TroopModels.forceFace(e, -1); }
                    server(c, b -> { respawn(b); layout(b, mode, cell, 0, YAW); done = tok; });
                    setCam(mode == L_CLOSE ? closeCam(cell, closeD(cell)) : highCam(mode == L_WIDE ? WIDE_Z : cell)); held = 0; return;
                }
                if (done != req) return;
                inject(c, s.pre()); double cell = cellFor(s.stance());
                boolean ok = placed(c, mode, cell, 0) && camOk(); held = ok ? held + 1 : 0;
                if (held >= 3 || wait > 600) {
                    if (held < 3) bad.add(s.name() + " " + LAYOUT[mode] + ": started anyway after 600 ticks");
                    if (seedGen != castGen) seeds(c, s.name() + " " + LAYOUT[mode] + "'s fresh cast", false);
                    ps = 1; wait = 0;
                }
            }
            case 1 -> {
                inject(c, s.pre());
                if (wait < SETTLE || !shotReady(c, s.name())) return;
                // H1: hit variants are seeded per unit now, so the cast plays a natural spread (no tick to wait for)
                ps = 2; f = 0; levelAt = -1; capMark = files.size(); trPose.clear(); trNow.clear(); trFresh.clear(); trWorld.clear(); lastSnapN = LAST == null ? -1 : LAST.n;
            }
            case 2 -> stateFrame(c, s, mode);
            default -> {}
        }
    }
    List<GravesAndGlory.Unit> subjects(int mode) { var list = cast; return mode == L_CLOSE ? list.subList(1, 4) : list; }
    void stateFrame(MinecraftClient c, St s, int mode) {
        inject(c, f > EVENT1 ? s.stance() : s.pre());
        Snap snap = fresh(); String dir = "states/" + s.name() + "/"; long world = c.world.getTime(); double cell = cellFor(s.stance());
        int[] stills = s.stills(); boolean still = Arrays.stream(stills).anyMatch(x -> x == f);
        if (mode == L_CLOSE) {
            int[][] cr = closeCrops.get(cell); var u2 = client(c, cast.get(SOLO_UNIT).mob); Ent en = snap == null || u2 == null ? null : snap.ents.get(u2.getId());
            var j = entry("capture", s.clip(), s.name(), "close", f, -1, f / 20.0); j.addProperty("worldTime", world); j.addProperty("fresh", snap != null); j.addProperty("cell", cell);
            JsonArray m = new JsonArray(); if (snap != null) closeMarks(m, snap, cr[0], 1); j.add("marks", m);
            if (en != null) { j.addProperty("engineNow", en.now); j.addProperty("deathTime", en.deathTime); }
            save(c, String.format(Locale.ROOT, "%scapture/f%03d.png", dir, f), cr[0], 1, j);
            trPose.add(en == null ? null : en.pose); trNow.add(en == null ? Double.NaN : en.now); trFresh.add(snap != null); trWorld.add(world);
            if (still) { var k = entry("close", s.clip(), s.name(), "close", f, -1, f / 20.0); k.addProperty("worldTime", world); k.add("marks", m.deepCopy()); save(c, String.format(Locale.ROOT, "%sclose_f%03d.png", dir, f), cr[0], 1, k); }
        } else if (still || f == levelAt) {
            boolean level = f == levelAt; String kind = level ? "level" : mode == L_WIDE ? "crowd" : "formed"; int at = level ? f - 1 : f;
            var j = entry(kind, s.clip(), s.name(), kind, f, -1, f / 20.0); j.addProperty("worldTime", world); crowdInfo(j, snap, mode, level, cell, s.name(), at);
            if (s.cat().equals("death") && at == 28) j.addProperty("note", "vanilla death tint, as in game");
            save(c, String.format(Locale.ROOT, "%s%s_f%03d.png", dir, kind, at), new int[]{0, 0, fbW, fbH}, 1, j);
            // A frame the world did not re-render still shows the last render, so that render's snapshot places the zoom box.
            Snap zs = snap != null ? snap : LAST;
            if (zs == null) noZoom.add(dir + kind + "_zoom_f" + at);
            else {
                int[] z = zoomBox(zs, mode); int sc = Math.max(1, (int) Math.floor(Math.min(8, 1600.0 / z[2])));
                var k = entry(kind + "_zoom", s.clip(), s.name(), kind, f, -1, f / 20.0); k.addProperty("worldTime", world); k.addProperty("zoomOf", String.format(Locale.ROOT, "%s%s_f%03d.png", dir, kind, at)); k.addProperty("nearestNeighbour", true);
                if (snap == null) k.addProperty("boxFrom", "the last render (the world did not re-render this tick; the framebuffer still shows that render)");
                save(c, String.format(Locale.ROOT, "%s%s_zoom_f%03d.png", dir, kind, at), z, sc, k);
            }
            if (mode == L_FORMED && !level) { setCam(levelCam()); levelAt = f + 1; }
            if (level) { setCam(highCam(cell)); }
        }
        if (f == EVENT1 || f == EVENT2 && s.twice()) fire(c, s, mode);
        f++;
        int last = Arrays.stream(stills).max().orElse(0);
        boolean doneNow = mode == L_CLOSE ? f >= CAPTURE : mode == L_WIDE ? f > last : f > last + 1;
        if (doneNow) {
            if (mode == L_CLOSE) {
                if (!timingOk(CAPTURE, closeEvents(s)) && capTry < MAX_RETRY) { capTry++; System.out.println("[mo-grade] " + s.name() + ": capture timing failed, retry " + capTry); dropCapture(); scenes(c, null); raised.clear(); ps = 0; wait = 0; return; }
                closeDone(s);
            }
            scenes(c, null); raised.clear();
            if (++pass >= PASSES.length) { pass = 0; sti++; }
            ps = 0; wait = 0;
        }
    }
    /** The close marks: the ground line under the trio and each one's soles. */
    void closeMarks(JsonArray m, Snap s, int[] crop, int unused) {
        Cam k = closeCam(1, 1); double y = Math.toRadians(k.yaw()), rx = -Math.cos(y), rz = -Math.sin(y);
        for (int i = 1; i <= 3; i++) {
            var u = cast.get(i); Ent en = s.ents.get(u.mob.getId()); if (en == null || en.pose == null) continue;
            double gy = site.getY() - en.pos.y; // each troop's own ground line at its own depth: under a camera looking down, one line through one troop sits above or below the others' feet
            line(m, screen(s, en.pos, new double[]{-0.7 * rx, gy, -0.7 * rz}), screen(s, en.pos, new double[]{0.7 * rx, gy, 0.7 * rz}), crop, C_GROUND, 1);
            Geo g = geo(en.pose, en.bodyYaw);
            for (int q = 0; q < 2; q++) if (g.soles[q] != null) {
                double[] o = g.soles[q];
                line(m, screen(s, en.pos, new double[]{o[0] - 0.1 * rx, gy, o[2] - 0.1 * rz}), screen(s, en.pos, new double[]{o[0] + 0.1 * rx, gy, o[2] + 0.1 * rz}), crop, C_GTICK, 2);
                dot(m, screen(s, en.pos, o), crop, soleColour(en.pos.y + o[1] - site.getY()), 3);
            }
        }
    }
    /** The subjects' projected boxes (the cloud's radius and height round each), grown by 10 percent. */
    int[] zoomBox(Snap s, int mode) {
        double x0 = 1e9, y0 = 1e9, x1 = -1e9, y1 = -1e9; double R = Math.min(cloudR, 1.6);
        for (var u : subjects(mode)) {
            Ent en = s.ents.get(u.mob.getId()); if (en == null) continue;
            for (int k = 0; k < 8; k++) {
                double[] p = screen(s, en.pos, new double[]{(k & 1) == 0 ? -R : R, (k & 2) == 0 ? site.getY() - en.pos.y : cloudTop, (k & 4) == 0 ? -R : R});
                if (p == null) continue; x0 = Math.min(x0, p[0]); y0 = Math.min(y0, p[1]); x1 = Math.max(x1, p[0]); y1 = Math.max(y1, p[1]);
            }
        }
        if (x0 > x1) return new int[]{fbW / 4, fbH / 4, fbW / 2, fbH / 2};
        return grow(x0, y0, x1, y1, MARGIN, fbW, fbH);
    }
    /** R78: the camera's distance to the nearest troop (its feet and its middle), in the entry and the crowd record. */
    void crowdInfo(JsonObject j, Snap s, int mode, boolean level, double cell, String state, int frame) {
        if (s == null) return; double near = 1e9, nearMid = 1e9, nearH = 1e9;
        for (var u : subjects(mode)) { Ent en = s.ents.get(u.mob.getId()); if (en == null) continue;
            near = Math.min(near, en.pos.distanceTo(s.cam)); nearMid = Math.min(nearMid, en.pos.add(0, 0.9, 0).distanceTo(s.cam)); nearH = Math.min(nearH, Math.hypot(en.pos.x - s.cam.x, en.pos.z - s.cam.z)); }
        j.addProperty("nearestFeetDist", r4(near)); j.addProperty("nearestMiddleDist", r4(nearMid)); j.addProperty("nearestHorizontalDist", r4(nearH));
        j.addProperty("cameraEyeOverGround", r4(s.cam.y - site.getY())); j.addProperty("spacing", mode == L_WIDE ? "wide overview grid " + WIDE_X + " x " + WIDE_Z + " (clone check only)" : "real spacing " + r4(cell));
        var x = new JsonObject(); x.addProperty("state", state); x.addProperty("shot", level ? "level" : mode == L_WIDE ? "crowd" : "formed"); x.addProperty("frame", frame);
        x.addProperty("nearestFeetDist", r4(near)); x.addProperty("nearestMiddleDist", r4(nearMid)); x.addProperty("cameraEyeOverGround", r4(s.cam.y - site.getY())); crowdJson.add(x);
    }
    void fire(MinecraftClient c, St s, int mode) {
        var subj = subjects(mode); long tick = c.world.getTime();
        switch (s.cat()) {
            case "stance" -> inject(c, s.stance());
            case "attack" -> { int rows = Tactics.variants(famName).size(); for (var u : subj) { var e = client(c, u.mob); if (e != null) MotionEvents.Client.attack(e.getId(), s.variant(), tick, rows); } }
            case "hit" -> { for (var u : subj) { var e = client(c, u.mob); if (e != null) MotionEvents.Client.hit(true, e.getId(), MotionEvents.KINDS.indexOf("slash"), MotionEvents.ZONES.indexOf(s.zone()), 0f, 1, s.blocked() ? 1 : 0, tick); } }
            case "scene" -> { for (var u : subj) { var e = client(c, u.mob); if (e != null) TroopMotion.setScenePose(e.getId(), s.clip()); } }
            case "death" -> { // H2: the crowd dies of mixed kinds and directions, the centre troop keeps the state's slash and bearing
                int i = 0; float[] brs = {0, 91, 180, -91};
                for (var u : subj) { if (u == cast.get(SOLO_UNIT) || subj.size() == 1) kill(c, List.of(u), s.bearing(), "slash"); else { kill(c, List.of(u), brs[i % 4], MotionEvents.KINDS.get(i % MotionEvents.KINDS.size())); i++; } }
            }
            default -> {}
        }
    }
    /** After a close pass: the capture judged (120 frames, each newly drawn, one world tick apart) and its delta. */
    int[] closeEvents(St s) {
        if (s.cat().equals("attack") || s.cat().equals("hit")) { int back = Math.min(CAPTURE - 1, EVENT1 + 1 + (int) Math.round(s.len() * 20)); return new int[]{EVENT1, back, EVENT2}; }
        return s.twice() ? new int[]{EVENT1, EVENT2} : new int[]{EVENT1};
    }
    void closeDone(St s) {
        int[] ev = closeEvents(s); String[] en = ev.length == 3 ? new String[]{"event 1", "clip ends, back to the stance", "event 2"} : s.twice() ? new String[]{"event 1", "event 2"} : new String[]{"event"};
        var tm = judge(s.name(), CAPTURE, ev); tm.addProperty("retries", capTry); capTry = 0;
        var d = delta(new ArrayList<>(trPose), new ArrayList<>(trNow), ev, en, s.clip());
        for (var x : statesJson) { var j = x.getAsJsonObject(); if (j.get("name").getAsString().equals(s.name())) { j.add("delta", d); var a = new JsonArray(); for (int e : ev) a.add(e); j.add("denseWindowsAfter", a); j.add("timing", tm); break; } }
    }
    /** R81: the timing bound of a close capture: every frame newly drawn, at most this many world steps not exactly 1 tick, none in a dense window. */
    static final int MAX_OFF_STEPS = 2;
    static final String TIMING_RULE = "every frame newly drawn; at most " + MAX_OFF_STEPS + " world steps per capture not exactly 1 tick, none touching a dense window (event E-" + DENSE + " to E+" + DENSE + ")";
    /** Per capture: off-step frame -> world ticks of the step that ended there (for the labels and the reports). */
    final Map<String, Map<Integer, Long>> offSteps = new HashMap<>();
    final JsonArray timingJson = new JsonArray();
    /**
     * R81: judge a close capture's timing. Stale frames fail outright; a world step that is not exactly 1 tick (frame i-1 to i) is
     * allowed up to {@link #MAX_OFF_STEPS} per capture, as long as neither of its frames lies in a dense window (E-10 to E+10).
     */
    boolean timingOk(int want, int[] ev) {
        int n = trWorld.size(), off = 0; if (n != want) return false;
        for (int i = 1; i < n; i++) {
            if (!trFresh.get(i)) return false;
            long st = trWorld.get(i) - trWorld.get(i - 1); if (st == 1) continue;
            off++; for (int e : ev) if (i >= e - DENSE && i - 1 <= e + DENSE) return false;
        }
        return off <= MAX_OFF_STEPS;
    }
    JsonObject judge(String name, int want, int[] ev) {
        int n = trWorld.size(), stale = 0; var offs = new JsonArray(); Map<Integer, Long> at = new TreeMap<>(); List<String> inWin = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (!trFresh.get(i)) stale++;
            long st = trWorld.get(i) - trWorld.get(i - 1); if (st == 1) continue;
            at.put(i, st); var o = new JsonObject(); o.addProperty("frame", i); o.addProperty("worldTicks", st);
            boolean w = false; for (int e : ev) if (i >= e - DENSE && i - 1 <= e + DENSE) w = true;
            o.addProperty("inDenseWindow", w); offs.add(o); if (w) inWin.add("f" + i);
        }
        offSteps.put(name, at);
        var t = new JsonObject(); t.addProperty("capture", name); t.addProperty("frames", n); t.addProperty("wantFrames", want); t.addProperty("staleFrames", stale); t.addProperty("offStepCount", at.size()); t.add("offSteps", offs); t.addProperty("rule", TIMING_RULE);
        boolean ok = n == want && stale == 0 && at.size() <= MAX_OFF_STEPS && inWin.isEmpty(); t.addProperty("ok", ok); timingJson.add(t);
        if (!ok) capBad.add(String.format(Locale.ROOT, "%s: %d frames of %d, %d not newly drawn, %d world steps not 1 tick %s%s (allowed: %s)", name, n, want, stale, at.size(), at, inWin.isEmpty() ? "" : ", in a dense window at " + inWin, TIMING_RULE));
        return t;
    }
    /** The off-step counts of a part's captures judged so far, for the reports. */
    String offStepSummary(String part) {
        Set<String> names = new TreeSet<>(); if (part.equals("states")) for (St s : states) names.add(s.name()); else for (Tr t : trans) names.add(t.name());
        int caps = 0, off = 0; List<String> where = new ArrayList<>();
        for (var e : offSteps.entrySet()) if (names.contains(e.getKey())) { caps++; off += e.getValue().size(); if (!e.getValue().isEmpty()) where.add(e.getKey() + " " + e.getValue()); }
        return String.format(Locale.ROOT, "; %d captures, %d world steps not 1 tick in all%s", caps, off, where.isEmpty() ? "" : " (frame=ticks: " + String.join("; ", where) + ")");
    }
    void finishStates() {
        report("mo-grade-capture", capBad.isEmpty() && !states.isEmpty(), states.size() + " states, each a 6 s close capture of " + CAPTURE + " native frames at 20 fps" + (capBad.isEmpty() ? ": " + TIMING_RULE + offStepSummary("states") : "; off: " + capBad));
        report("mo-grade-seeds", seedBad.isEmpty() && seedsChecked > 0, seedsChecked + " casts of " + N + " troops" + (seedBad.isEmpty() ? ", each troop its own variant seed in every cast" : "; off: " + seedBad));
        double near = 1e9; for (var x : crowdJson) near = Math.min(near, x.getAsJsonObject().get("nearestFeetDist").getAsDouble());
        report("mo-grade-states", bad.isEmpty() && near >= 30, states.size() + " live states (wide overview, formation at real spacing from high and at eye height, close trio at real spacing), the nearest crowd troop " + String.format(Locale.ROOT, "%.2f", near) + " blocks from the camera (at least 30)" + (bad.isEmpty() ? "" : "; off: " + bad));
    }

    // ---- transitions ----
    void transitions(MinecraftClient c) {
        if (ti >= trans.size()) { report("mo-grade-transitions", capBad.isEmpty() && transJson.size() == trans.size(), trans.size() + " transitions and sweeps (" + TRANS_FRAMES + " close frames each, hit sweep cells " + HS_FRAMES + ", novelty " + NOV_FRAMES + "), dense sheets and the pose delta round every event" + (capBad.isEmpty() ? "; " + TIMING_RULE + offStepSummary("transitions") : "; capture off: " + capBad)); next(c); return; }
        if (awayFor > 0) { litStart(c); return; }
        Tr t = trans.get(ti); wait++; double cell = cellFor(t.pre());
        switch (tph) {
            case 0 -> {
                if (wait == 1) { int tok = ++req; scenes(c, null); raised.clear(); for (var u : cast) { var e = client(c, u.mob); if (e != null) TroopModels.forceFace(e, -1); }
                    server(c, b -> { respawn(b); layout(b, L_CLOSE, cell, 0, YAW); done = tok; }); setCam(closeCam(cell, closeD(cell))); held = 0; curStance = t.pre(); return; }
                if (done != req) return;
                inject(c, curStance);
                boolean ok = placed(c, L_CLOSE, cell, 0) && camOk(); held = ok ? held + 1 : 0;
                if (held >= 3 || wait > 600) { if (seedGen != castGen) seeds(c, t.name() + "'s fresh cast", false); tph = 1; wait = 0; }
            }
            case 1 -> { inject(c, curStance); if (wait < SETTLE || !shotReady(c, t.name())) return; tph = 2; f = 0; capMark = files.size(); trNov.clear(); trClip.clear(); trPose.clear(); trNow.clear(); trFresh.clear(); trWorld.clear(); lastSnapN = LAST == null ? -1 : LAST.n; }
            case 2 -> {
                inject(c, curStance);
                Snap snap = fresh(); long world = c.world.getTime(); int[][] cr = closeCrops.get(cell);
                var u2 = client(c, cast.get(SOLO_UNIT).mob); Ent en = snap == null || u2 == null ? null : snap.ents.get(u2.getId());
                var j = entry("capture", null, t.name(), "close", f, -1, f / 20.0); j.addProperty("worldTime", world); j.addProperty("fresh", snap != null); j.addProperty("transition", t.name()); j.addProperty("cell", cell);
                JsonArray m = new JsonArray(); if (snap != null) closeMarks(m, snap, cr[0], 1); j.add("marks", m);
                if (en != null) { j.addProperty("engineNow", en.now); j.addProperty("deathTime", en.deathTime); j.addProperty("stance", curStance); }
                save(c, String.format(Locale.ROOT, "transitions/%s/f%03d.png", t.name(), f), cr[0], 1, j);
                trPose.add(en == null ? null : en.pose); trNow.add(en == null ? Double.NaN : en.now); trFresh.add(snap != null); trWorld.add(world);
                trNov.add(en == null ? -1 : en.novKind); trClip.add(en == null || en.tracks.isEmpty() ? "" : keyOf(en.tracks.get(0).clip()));
                for (Object[] ev : t.evs()) if ((int) ev[0] == f) fireTr(c, (String) ev[1], (String) ev[2]);
                f++;
                if (f >= tf(t)) {
                    int[] ev = new int[t.evs().size()]; String[] en2 = new String[ev.length];
                    for (int i = 0; i < ev.length; i++) { ev[i] = (int) t.evs().get(i)[0]; en2[i] = t.evs().get(i)[1] + ((String) t.evs().get(i)[2]).replaceAll("^(.+)$", " $1"); }
                    if (!timingOk(tf(t), ev) && capTry < MAX_RETRY) { capTry++; System.out.println("[mo-grade] " + t.name() + ": capture timing failed, retry " + capTry); dropCapture(); scenes(c, null); raised.clear(); tph = 0; wait = 0; return; }
                    var tm = judge(t.name(), tf(t), ev); tm.addProperty("retries", capTry); capTry = 0;
                    var x = new JsonObject(); x.addProperty("name", t.name()); x.addProperty("title", t.title()); x.addProperty("preStance", t.pre()); x.addProperty("cell", cell);
                    var ea = new JsonArray(); for (Object[] e2 : t.evs()) { var o = new JsonObject(); o.addProperty("afterFrame", (int) e2[0]); o.addProperty("what", (String) e2[1]); o.addProperty("arg", (String) e2[2]); ea.add(o); } x.add("events", ea);
                    x.add("delta", delta(new ArrayList<>(trPose), new ArrayList<>(trNow), ev, en2, null)); x.add("timing", tm); x.addProperty("frames", tf(t));
                    var pc = new JsonArray(); var seen = new java.util.LinkedHashSet<>(trClip); seen.remove(""); for (String k : seen) pc.add(k); x.add("playedClips", pc);
                    var nv = new JsonArray(); for (int k = 0; k < trNov.size(); k++) if (trNov.get(k) >= 0) { var o = new JsonObject(); o.addProperty("frame", k); o.addProperty("kind", trNov.get(k)); nv.add(o); } x.add("noveltyFrames", nv);
                    transJson.add(x);
                    scenes(c, null); raised.clear(); tph = 0; ti++; wait = 0;
                }
            }
            default -> {}
        }
    }
    String curStance = "hold";
    void fireTr(MinecraftClient c, String what, String a) {
        var subj = subjects(L_CLOSE); long tick = c.world.getTime();
        switch (what) {
            case "stance" -> { curStance = a; inject(c, a); }
            case "raise" -> { for (var u : subj) raised.add(u.mob.getId()); }
            case "lower" -> { for (var u : subj) raised.remove(u.mob.getId()); }
            case "attack" -> { int rows = Tactics.variants(famName).size(); int v = 0; while (v < fam.attackIndex().length && fam.attackIndex()[v] < 0) v++; for (var u : subj) { var e = client(c, u.mob); if (e != null) MotionEvents.Client.attack(e.getId(), v, tick, rows); } }
            case "hit" -> {
                String[] q = a.split(":"); String zone = q[0]; int dir = q.length > 1 ? Math.max(0, DIRS.indexOf(q[1])) : 0, wt = q.length > 2 ? Integer.parseInt(q[2]) : 1; float br = new float[]{0, 91, 180, -91}[dir];
                for (var u : subj) { var e = client(c, u.mob); if (e != null) MotionEvents.Client.hit(true, e.getId(), MotionEvents.KINDS.indexOf("slash"), MotionEvents.ZONES.indexOf(zone), br, wt, 0, tick); }
            }
            case "kill" -> kill(c, subj, 0, "slash");
            default -> {}
        }
    }

    // ---- the sheet specs ----
    JsonObject grid(String file, String kind, String clip, String state, String title, String... sub) {
        var g = new JsonObject(); g.addProperty("file", file); g.addProperty("kind", kind); if (clip != null) g.addProperty("clip", clip); if (state != null) g.addProperty("state", state);
        g.addProperty("title", title); var s = new JsonArray(); for (String x : sub) s.add(x);
        String tint = "the red on the troop is vanilla's hurt and death tint, as in game"; boolean said = false; for (String x : sub) said |= x.contains("death tint");
        if (!said && state != null && !kind.equals("contact") && (state.contains("death") || state.contains("hit"))) s.add(tint); g.add("sub", s); g.add("rows", new JsonArray()); sheets.add(g); return g;
    }
    static JsonArray row(JsonObject g, String label) { var r = new JsonObject(); r.addProperty("label", label); var t = new JsonArray(); r.add("tiles", t); g.getAsJsonArray("rows").add(r); return t; }
    JsonObject tile(JsonArray row, String src, JsonArray crop, int scale, boolean nearest, String label, boolean hl, JsonArray extra) {
        var t = new JsonObject(); t.addProperty("src", src); if (crop != null) t.add("crop", crop); t.addProperty("scale", scale); t.addProperty("nearest", nearest); t.addProperty("label", label); if (hl) t.addProperty("hl", true);
        var e = byFile.get(src); var m = new JsonArray(); if (e != null && e.has("marks")) m.addAll(e.getAsJsonArray("marks")); if (extra != null) m.addAll(extra); t.add("marks", m);
        if (e == null) t.addProperty("missing", true);
        row.add(t); return t;
    }
    String lab(String src, String fallback) {
        var e = byFile.get(src); if (e == null) return fallback + " (missing)";
        String s = fallback; if (e.has("clipTime")) s += String.format(Locale.ROOT, " t=%.2fs", e.get("clipTime").getAsDouble());
        if (e.has("deathTime") && e.get("deathTime").getAsInt() > 0) s += " death " + e.get("deathTime").getAsInt();
        if (e.has("soleHeight")) { var h = e.getAsJsonArray("soleHeight"); s += "  soles R " + (h.get(0).isJsonNull() ? "-" : String.format(Locale.ROOT, "%+.2f", h.get(0).getAsDouble())) + " L " + (h.get(1).isJsonNull() ? "-" : String.format(Locale.ROOT, "%+.2f", h.get(1).getAsDouble())); }
        return s;
    }
    static final String LEGEND = "marks: white ground line and height ticks every 0.5 block; soles green planted, red lifted, magenta sinking (more than half a texel, 1/32 block, below the ground), each over a grey ground tick straight below it; hands yellow; weapon tip orange; shield rim cyan";
    JsonObject plot(String file, String kind, String state, String title, int w, int h) {
        var p = new JsonObject(); p.addProperty("file", file); p.addProperty("kind", kind); if (state != null) p.addProperty("state", state);
        var q = new JsonObject(); q.addProperty("w", w); q.addProperty("h", h); q.addProperty("title", title); q.add("panels", new JsonArray()); q.add("vlines", new JsonArray()); p.add("plot", q); sheets.add(p); return q;
    }
    static JsonObject panel(JsonObject plot, String title, String ylabel) { var p = new JsonObject(); p.addProperty("title", title); p.addProperty("ylabel", ylabel); p.add("series", new JsonArray()); p.add("hlines", new JsonArray()); plot.getAsJsonArray("panels").add(p); return p; }
    static void series(JsonObject panel, String name, String col, JsonArray x, JsonArray y) { var s = new JsonObject(); s.addProperty("name", name); s.addProperty("c", col); s.add("x", x); s.add("y", y); panel.getAsJsonArray("series").add(s); }
    static void hline(JsonObject panel, double y, String col, String label) { var s = new JsonObject(); s.addProperty("y", y); s.addProperty("c", col); s.addProperty("label", label); panel.getAsJsonArray("hlines").add(s); }
    static void vline(JsonObject plot, double x, String col, String label) { var s = new JsonObject(); s.addProperty("x", x); s.addProperty("c", col); s.addProperty("label", label); plot.getAsJsonArray("vlines").add(s); }
    static JsonArray range(int n) { var a = new JsonArray(); for (int i = 0; i < n; i++) a.add(i); return a; }
    void deltaPlot(String file, String kind, String state, String title, JsonObject d, List<int[]> events, List<String> names) {
        var p = plot(file, kind, state, title, 1500, 760); int n = d.getAsJsonArray("sumDegPerTick").size(); var x = range(n);
        var a = panel(p, "pose change per tick (" + (state.startsWith("loco-") ? "the one walking troop" : "unit 2, the centre troop") + ")", "deg / tick"); series(a, "summed over bones", "#ffb000", x, d.getAsJsonArray("sumDegPerTick")); series(a, "largest bone", "#40c0ff", x, d.getAsJsonArray("maxBoneDegPerTick"));
        var b = panel(p, "second difference (a pop is a spike)", "deg / tick^2"); series(b, "summed |change of per-tick change|", "#ff6060", x, d.getAsJsonArray("secondDiffDegPerTick2"));
        double base = d.get("baselineSecondDiffP90").getAsDouble(); hline(b, 3 * base, "#ffffff", String.format(Locale.ROOT, "flag line 3 x p90 = %.1f", 3 * base));
        for (int i = 0; i < events.size(); i++) vline(p, events.get(i)[0] + 0.5, "#a0ff80", names.get(i));
    }
    static String who(String state) { return state.startsWith("loco-") ? "the one walking troop" : "the centre troop"; }
    void dense(String file, String state, String clip, String title, String srcFmt, int e, int[] crop, JsonObject d, String note) {
        var g = grid(file, "dense", clip, state, title, "every drawn frame from E-10 to E+10 (E = the frame after which the event fired; E+1 is the first that can show it), " + who(state) + " at native resolution", note, "per-tick pose change of " + who(state) + " under each frame (deg/tick summed over bones; second difference); the largest step in the window is highlighted");
        double[] sum = null, acc = null; if (d != null) { sum = toArr(d.getAsJsonArray("sumDegPerTick")); acc = toArr(d.getAsJsonArray("secondDiffDegPerTick2")); }
        int at = -1; double best = -1; if (sum != null) for (int k = Math.max(1, e - DENSE); k <= Math.min(sum.length - 1, e + DENSE); k++) if (sum[k] > best) { best = sum[k]; at = k; }
        JsonArray row = null; int col = 0;
        for (int k = e - DENSE; k <= e + DENSE; k++) {
            if (k < 0) continue; String src = String.format(Locale.ROOT, srcFmt, k); if (!byFile.containsKey(src)) continue;
            if (row == null || col == 7) { row = row(g, ""); col = 0; }
            String l = String.format(Locale.ROOT, "f%03d E%+d", k, k - e) + (sum != null && k < sum.length ? String.format(Locale.ROOT, "  %.1f d/t  dd %.1f", sum[k], acc[k]) : "") + stepNote(state.startsWith("transition-") ? state.substring(11) : state, k, k);
            tile(row, src, cropArr(crop), 1, false, l, k == at, null); col++;
        }
    }
    /** R81: the world steps not exactly 1 tick that end on frames from..to of a capture, for a tile's label ("" when none). */
    String stepNote(String capture, int from, int to) {
        var m = offSteps.get(capture); if (m == null) return ""; StringBuilder b = new StringBuilder();
        for (var e : m.entrySet()) if (e.getKey() >= from && e.getKey() <= to) b.append(String.format(Locale.ROOT, "  STEP %d TICKS at f%03d", e.getValue(), e.getKey()));
        return b.toString();
    }
    static double[] toArr(JsonArray a) { double[] v = new double[a.size()]; for (int i = 0; i < v.length; i++) v[i] = a.get(i).getAsDouble(); return v; }
    static JsonArray cropArr(int[] r) { if (r == null) return null; var a = new JsonArray(); for (int x : r) a.add(x); return a; }
    /** A crop of the centre unit inside a close capture (the capture is the trio's crop of the frame). */
    static int[] inner(int[] outer, int[] innerAbs) { return new int[]{Math.max(0, innerAbs[0] - outer[0]), Math.max(0, innerAbs[1] - outer[1]), Math.min(innerAbs[2], outer[2]), Math.min(innerAbs[3], outer[3])}; }

    void buildSheets() {
        String cams = String.format(Locale.ROOT, "solo camera %.2f blocks from the troop, eye %.2f over the ground, level; crop %dx%d of the %dx%d frame, native; fitted to every clip's extent with a 10 percent margin", soloD, soloEye, soloCrop[2], soloCrop[3], fbW, fbH);
        if (PARTS.contains("solo")) for (String n : clipOrder) {
            String st = clipState.get(n); var kp = keys.get(n); String fr = "full pipeline: the clip asked for at the input level, engine layers and MotionFeet on";
            var g = grid("clips/" + n + "/contact.png", "contact", n, st, "clip " + n + " (" + st + "): 8 frames front and side, feet and hands crops, key poses", fr, cams, LEGEND,
                    st.startsWith("death") || st.startsWith("hit") ? "the red on the troop is vanilla's hurt and death tint, as in game" : "labels: clip time; soles' height over the ground in blocks (R right, L left)");
            for (int a : new int[]{0, 90}) {
                String v = a == 0 ? "front (angle 0)" : "side (angle 90, the troop's right)", pre = String.format(Locale.ROOT, "clips/%s/a%03d_", n, a);
                var r = row(g, v + ": 8 frames"); for (int i = 0; i < FRAMES; i++) tile(r, pre + "f" + i + ".png", null, 1, false, lab(pre + "f" + i + ".png", "f" + i), false, null);
                r = row(g, v + ": feet, x2 nearest"); for (int i = 0; i < FRAMES; i++) { var e = byFile.get(pre + "f" + i + ".png"); tile(r, pre + "f" + i + ".png", e == null ? null : e.getAsJsonArray("feet"), 2, true, lab(pre + "f" + i + ".png", "f" + i + " feet"), false, null); }
                int hx0 = Integer.MAX_VALUE, hy0 = Integer.MAX_VALUE, hx1 = -1, hy1 = -1;
                for (int i = 0; i < FRAMES; i++) { var e = byFile.get(pre + "f" + i + ".png"); if (e == null || !e.has("hands")) continue; var h = e.getAsJsonArray("hands"); hx0 = Math.min(hx0, h.get(0).getAsInt()); hy0 = Math.min(hy0, h.get(1).getAsInt()); hx1 = Math.max(hx1, h.get(0).getAsInt() + h.get(2).getAsInt()); hy1 = Math.max(hy1, h.get(1).getAsInt() + h.get(3).getAsInt()); }
                JsonArray hc = hx1 < 0 ? null : cropArr(new int[]{hx0, hy0, hx1 - hx0, hy1 - hy0}); int hs = hc != null && hx1 - hx0 <= 330 && hy1 - hy0 <= 330 ? 2 : 1;
                r = row(g, v + ": hands and weapon, one crop for all 8 frames" + (hs == 2 ? ", x2 nearest" : ", native")); for (int i = 0; i < FRAMES; i++) tile(r, pre + "f" + i + ".png", hc, hs, true, "f" + i + " hands", false, null);
            }
            for (int a : new int[]{0, 90}) { var r = row(g, (a == 0 ? "front" : "side") + ": key poses"); for (String k : kp.keySet()) { String src = String.format(Locale.ROOT, "clips/%s/a%03d_k_%s.png", n, a, k); tile(r, src, null, 1, false, lab(src, k), false, null); } }
            var t = grid("clips/" + n + "/turnaround.png", "turnaround", n, st, "clip " + n + " (" + st + "): each key pose from 8 angles (0 front, 90 its right, 180 back, 270 its left)", fr, cams, LEGEND);
            for (String k : kp.keySet()) { var r = row(t, k); for (int a : ANGLES) { String src = String.format(Locale.ROOT, "clips/%s/a%03d_k_%s.png", n, a, k); tile(r, src, null, 1, false, lab(src, a + " deg"), false, null); } }
            var tr = grid("clips/" + n + "/trail.png", "trail", n, st, "clip " + n + " (" + st + "): weapon-tip (orange) and shield-rim (cyan) trails over the whole clip", fr, "each dot is one drawn frame (1/20 s); the frame under the trail is the " + (kp.containsKey("impact") ? "impact" : "first extreme") + " key pose");
            var r = row(tr, "front and side");
            String key = kp.containsKey("impact") ? "impact" : "extreme_a"; JsonObject[] tl = trails.getOrDefault(n, new JsonObject[2]);
            for (int a = 0; a < 2; a++) {
                String src = String.format(Locale.ROOT, "clips/%s/a%03d_k_%s.png", n, a == 0 ? 0 : 90, key); var extra = new JsonArray();
                if (tl[a] != null) for (String w : new String[]{"tip", "rim"}) { var pts = tl[a].getAsJsonArray(w); if (pts.size() < 2) continue; var o = new JsonObject(); o.addProperty("t", "poly"); o.add("pts", pts); o.addProperty("c", w.equals("tip") ? C_TIP : C_RIM); o.addProperty("w", 2); o.addProperty("dots", true); extra.add(o); }
                tile(r, src, null, 1, false, (a == 0 ? "front" : "side") + " " + lab(src, key), false, extra);
            }
        }
        for (var x : locoJson) {
            var j = x.getAsJsonObject(); String n = j.get("name").getAsString(); int frames = j.get("frames").getAsInt(); if (frames == 0) continue;
            int start = j.get("startFrame").getAsInt(), stop = j.has("stopFrame") ? j.get("stopFrame").getAsInt() : -1;
            var g = grid("loco/" + n + "/loco.png", "loco", null, "loco-" + n, "locomotion: " + j.get("title").getAsString(), "full pipeline as the game moves a troop: the server walks it a tick at a time, this client interpolates, the engine poses it; the side camera tracks it at a fixed distance",
                    String.format(Locale.ROOT, "planted slide (right, left) %s blocks; largest single plant %s; lowest sole %s (negative = sinking); every 4th frame, the feet x2 below each", j.get("plantedSlideBlocks"), j.get("largestSinglePlantSlideBlocks"), j.get("lowestSoleHeight")), LEGEND);
            JsonArray fr = null, ft = null; int col = 0;
            for (int k = 0; k < frames; k += STRIP) {
                String src = String.format(Locale.ROOT, "loco/%s/f%03d.png", n, k); var e = byFile.get(src); if (e == null) continue;
                if (fr == null || col == 9) { fr = row(g, ""); ft = row(g, ""); col = 0; }
                String l = String.format(Locale.ROOT, "f%03d %s", k, e.get("phase").getAsString()); boolean hl = k == start || k == stop;
                tile(fr, src, null, 1, false, l, hl, null); tile(ft, src, e.getAsJsonArray("feet"), 2, true, l + " feet", hl, null); col++;
            }
            var tr = j.getAsJsonObject("trace"); var p = plot("loco/" + n + "/loco_trace.png", "loco_trace", "loco-" + n, "locomotion " + n + ": foot traces (blocks; frame = one drawn frame, 1/20 s)", 1500, 1150);
            var a = panel(p, "feet and body along x (a planted foot is flat)", "x"); series(a, "right sole", "#ff8040", tr.getAsJsonArray("frame"), tr.getAsJsonArray("rightX")); series(a, "left sole", "#40c0ff", tr.getAsJsonArray("frame"), tr.getAsJsonArray("leftX")); series(a, "body", "#c0c0c0", tr.getAsJsonArray("frame"), tr.getAsJsonArray("bodyX"));
            if (n.equals("turn")) { var z = panel(p, "feet along z (the turn swings them)", "z"); series(z, "right sole", "#ff8040", tr.getAsJsonArray("frame"), tr.getAsJsonArray("rightZ")); series(z, "left sole", "#40c0ff", tr.getAsJsonArray("frame"), tr.getAsJsonArray("leftZ")); }
            var h = panel(p, "sole height over the ground (below 0 = sinking)", "y"); series(h, "right sole", "#ff8040", tr.getAsJsonArray("frame"), tr.getAsJsonArray("rightHeight")); series(h, "left sole", "#40c0ff", tr.getAsJsonArray("frame"), tr.getAsJsonArray("leftHeight"));
            hline(h, 0, "#ffffff", "ground"); var th = j.getAsJsonArray("plantedThreshold"); hline(h, th.get(0).getAsDouble(), "#80ff80", "planted below " + th.get(0).getAsDouble());
            if (tr.has("rightPlanted")) {
                // R83: the measured plants as spans (each foot its own band) against MotionEngine's planted-feet weight
                var pa = panel(p, "measured plants: right band 1.6-2.0, left band 1.1-1.5 (high = planted); the engine's planted-feet weight 0-1", "plant");
                JsonArray rb = new JsonArray(), lb = new JsonArray(); for (int k = 0; k < frames; k++) { rb.add(1.6 + 0.4 * tr.getAsJsonArray("rightPlanted").get(k).getAsDouble()); lb.add(1.1 + 0.4 * tr.getAsJsonArray("leftPlanted").get(k).getAsDouble()); }
                series(pa, "right plants", "#ff8040", tr.getAsJsonArray("frame"), rb); series(pa, "left plants", "#40c0ff", tr.getAsJsonArray("frame"), lb); series(pa, "solver pw (MotionFeet: (1-m)(1-tw) plant (1-bodyAction))", "#e0e060", tr.getAsJsonArray("frame"), tr.getAsJsonArray("enginePlantWeight"));
            }
            if (start >= 0) vline(p, start, "#a0ff80", n.equals("turn") ? "turn starts" : "move ordered"); if (stop >= 0) vline(p, stop, "#ffa080", n.equals("turn") ? "turn held" : "stop ordered");
            if (j.has("delta")) { // R80: every segment its own delta plot and both dense windows
                var d = j.getAsJsonObject("delta"); List<int[]> evs = new ArrayList<>(); List<String> nm = new ArrayList<>();
                if (start >= 0) { evs.add(new int[]{start}); nm.add(n.equals("turn") ? "turn starts" : "idle to move"); } if (stop >= 0) { evs.add(new int[]{stop}); nm.add(n.equals("turn") ? "turn held" : "move to stop"); }
                deltaPlot("loco/" + n + "/delta.png", "delta", "loco-" + n, "locomotion " + n + ": per-tick pose delta", d, evs, nm);
                for (int i = 0; i < evs.size(); i++) {
                    String nm2 = nm.get(i).replace(' ', '-');
                    dense("loco/" + n + "/dense_" + nm2 + ".png", "loco-" + n, null, "locomotion " + n + ": " + nm.get(i) + " (dense)", "loco/" + n + "/f%03d.png", evs.get(i)[0], null, d, "tracking side camera, the whole loco crop");
                }
            }
        }
        for (var x : statesJson) {
            var j = x.getAsJsonObject(); String n = j.get("name").getAsString(), clip = j.get("clip").getAsString(); double cell = j.get("cell").getAsDouble(); int[][] cr = closeCrops.get(cell);
            if (cr == null) continue;
            var g = grid("states/" + n + "/strip.png", "strip", clip, n, "state " + n + ": every 4th close frame (the trio at real spacing " + r4(cell) + ", native)", "event after f010" + (j.getAsJsonArray("eventsAfterFrames").size() > 1 ? " and f070" : "") + " (highlighted: the first frame that can show it)", "every troop gets the same event on the same tick: attacks and hits are synchronised volleys");
            JsonArray r = null; int col = 0;
            for (int k = 0; k < CAPTURE; k += STRIP) {
                String src = String.format(Locale.ROOT, "states/%s/capture/f%03d.png", n, k); if (r == null || col == 6) { r = row(g, ""); col = 0; }
                tile(r, src, null, 1, false, String.format(Locale.ROOT, "f%03d t=%.2fs", k, k / 20.0) + stepNote(n, k - STRIP + 1, k), k == EVENT1 + 2 || k == EVENT2 + 2 && j.getAsJsonArray("eventsAfterFrames").size() > 1, null); col++;
            }
            if (!j.has("delta")) continue;
            var d = j.getAsJsonObject("delta"); List<int[]> evs = new ArrayList<>(); List<String> nm = new ArrayList<>();
            for (var w : d.getAsJsonArray("windows")) { var o = w.getAsJsonObject(); evs.add(new int[]{o.get("afterFrame").getAsInt()}); nm.add(o.get("event").getAsString()); }
            deltaPlot("states/" + n + "/delta.png", "delta", n, "state " + n + ": per-tick pose delta of the centre troop (close capture)", d, evs, nm);
            int[] in = inner(cr[0], cr[1]);
            for (int i = 0; i < evs.size(); i++) dense(String.format(Locale.ROOT, "states/%s/dense_f%03d.png", n, evs.get(i)[0]), n, clip, "state " + n + ": " + nm.get(i) + " (dense)", "states/" + n + "/capture/f%03d.png", evs.get(i)[0], in, d, "the centre troop's crop of the close capture");
        }
        for (var x : transJson) {
            var j = x.getAsJsonObject(); String n = j.get("name").getAsString(); double cell = j.get("cell").getAsDouble(); int[][] cr = closeCrops.get(cell); var d = j.getAsJsonObject("delta");
            List<int[]> evs = new ArrayList<>(); List<String> nm = new ArrayList<>();
            for (var w : d.getAsJsonArray("windows")) { var o = w.getAsJsonObject(); evs.add(new int[]{o.get("afterFrame").getAsInt()}); nm.add(o.get("event").getAsString()); }
            if (sweep(n)) continue; // H2: a sweep cell has no sheets of its own, only a cell in its zone's grid
            deltaPlot("transitions/" + n + "/delta.png", "delta", "transition-" + n, "transition " + n + ": " + j.get("title").getAsString(), d, evs, nm);
            int[] in = inner(cr[0], cr[1]);
            for (int i = 0; i < evs.size(); i++) dense(String.format(Locale.ROOT, "transitions/%s/dense_f%03d.png", n, evs.get(i)[0]), "transition-" + n, null, "transition " + n + ": " + nm.get(i) + " (dense)", "transitions/" + n + "/f%03d.png", evs.get(i)[0], in, d, j.get("title").getAsString());
            var g = grid("transitions/" + n + "/strip.png", "strip", null, "transition-" + n, "transition " + n + ": every 4th close frame (the trio at real spacing " + r4(cell) + ")", j.get("title").getAsString());
            JsonArray r = null; int col = 0;
            for (int k = 0; k < tf(n); k += STRIP) { String src = String.format(Locale.ROOT, "transitions/%s/f%03d.png", n, k); if (r == null || col == 5) { r = row(g, ""); col = 0; } tile(r, src, null, 1, false, String.format(Locale.ROOT, "f%03d", k) + stepNote(n, k - STRIP + 1, k), false, null); col++; }
        }
    }

    /** H2: one grid per zone, a row per direction and weight, tiles at the frames the hit shows in. */
    final JsonArray hitSweepJson = new JsonArray();
    void hitSweepSheets() {
        for (String z : List.of("head", "body", "legs")) {
            int[][] cr = null; JsonObject any = null; for (var x : transJson) { var j = x.getAsJsonObject(); if (j.get("name").getAsString().startsWith("hs-" + z + "-")) { any = j; break; } }
            if (any == null) continue;
            var g = grid("transitions/hitsweep_" + z + ".png", "strip", null, "hitsweep-" + z, "hit sweep, " + z + " zone: rows = direction (front, right, back, left) x weight 0-2; tiles = f21 f24 f28 f34 of the close capture", "the hit fires after f" + HS_EVENT + "; the delta numbers per cell are in manifest.hitSweep");
            for (String d : DIRS) for (int w = 0; w < 3; w++) {
                String n = "hs-" + z + "-" + d + "-" + w; JsonObject j = null; for (var x : transJson) if (x.getAsJsonObject().get("name").getAsString().equals(n)) j = x.getAsJsonObject();
                if (j == null) continue;
                var r = row(g, d + " w" + w);
                for (int k : new int[]{21, 24, 28, 34}) tile(r, String.format(Locale.ROOT, "transitions/%s/f%03d.png", n, k), null, 1, false, "f" + k, false, null);
                var dl = j.getAsJsonObject("delta"); var o = new JsonObject(); o.addProperty("cell", n); o.addProperty("zone", z); o.addProperty("dir", d); o.addProperty("weight", w);
                double sum = 0, bone = 0; String bn = ""; int bf = -1;
                for (var wq : dl.getAsJsonArray("windows")) { var q = wq.getAsJsonObject(); sum = Math.max(sum, q.get("largestStepDegPerTick").getAsDouble()); if (q.get("worstBoneDegPerTick").getAsDouble() > bone) { bone = q.get("worstBoneDegPerTick").getAsDouble(); bn = q.get("worstBone").getAsString(); bf = q.get("worstBoneFrame").getAsInt(); } }
                o.addProperty("sumDegPerTick", r4(sum)); o.addProperty("worstBone", bn); o.addProperty("worstBoneDegPerTick", r4(bone)); o.addProperty("worstBoneFrame", bf); o.add("playedClips", j.get("playedClips")); o.add("timing", j.get("timing"));
                hitSweepJson.add(o);
            }
        }
    }

    // ---- the grading map (R65, M2, R78) ----
    JsonObject grading() {
        var all = new JsonObject();
        for (var x : statesJson) {
            var j = x.getAsJsonObject(); String n = j.get("name").getAsString(), clip = j.get("clip").getAsString(), stance = j.get("stance").getAsString(); var ev = new JsonObject();
            String cdir = "clips/" + clip + "/", sdir = "states/" + n + "/"; int still = j.getAsJsonArray("stillFrames").get(0).getAsInt(); String sf = String.format(Locale.ROOT, "_f%03d.png", still);
            List<String> stills = new ArrayList<>(); for (var s : j.getAsJsonArray("stillFrames")) stills.add(String.format(Locale.ROOT, "_f%03d.png", s.getAsInt()));
            List<String> dense = new ArrayList<>(); if (j.has("denseWindowsAfter")) for (var w : j.getAsJsonArray("denseWindowsAfter")) dense.add(String.format(Locale.ROOT, "%sdense_f%03d.png", sdir, w.getAsInt()));
            put(ev, "weight", sdir + "strip.png", "timing of the live state at 20 fps; event after f010", cdir + "contact.png", "8 frames front and side", sdir + "delta.png", "speed of the pose per tick");
            put(ev, "arcs", cdir + "trail.png", "weapon-tip and shield-rim paths, front and side", cdir + "turnaround.png", "key poses from 8 angles");
            put(ev, "anticipation", cdir + "contact.png", "key-pose rows: " + String.join(", ", keys.getOrDefault(clip, new LinkedHashMap<>()).keySet()), sdir + "strip.png", "the live state");
            List<String> sil = new ArrayList<>(); for (String s : stills) { sil.add(sdir + "level" + s); sil.add("level shot, eye height 1.62, nearest troop at least 30 blocks"); sil.add(sdir + "level_zoom" + s); sil.add("the same, the troops zoomed nearest neighbour"); }
            put(ev, "silhouette", sil.toArray(new String[0]));
            List<String> var = new ArrayList<>(); for (String s : stills) { var.add(sdir + "formed_zoom" + s); var.add("the formation at real spacing, from 30 blocks up high, zoomed"); }
            var.add(sdir + "crowd_zoom" + sf); var.add("the wide overview grid, a clone check only (spacing exaggerated)"); var.add(sdir + "formed" + sf); var.add("the whole formation frame");
            put(ev, "variation", var.toArray(new String[0]));
            List<String> con = new ArrayList<>(List.of("loco/walk/loco.png", "walk: frames and feet crops", "loco/walk/loco_trace.png", "walk: foot traces; manifest loco[].plantedSlideBlocks", "loco/turn/loco_trace.png", "turn in place"));
            if (stance.equals("run")) { con.add("loco/run/loco.png"); con.add("run"); con.add("loco/run/loco_trace.png"); con.add("run traces"); }
            if (Set.of("shield_move", "wall", "brace").contains(stance)) { con.add("loco/crawl/loco.png"); con.add("shield crawl"); con.add("loco/crawl/loco_trace.png"); con.add("crawl traces"); }
            if (stance.equals("hold")) { con.add("loco/backpedal/loco.png"); con.add("backpedal"); con.add("loco/backpedal/loco_trace.png"); con.add("backpedal traces"); }
            con.add(cdir + "contact.png"); con.add("the feet rows of the clip");
            put(ev, "contact", con.toArray(new String[0]));
            List<String> clp = new ArrayList<>(List.of(cdir + "turnaround.png", "body, weapon and shield from 8 angles at each key pose"));
            for (String s : stills) { clp.add(sdir + "formed" + s); clp.add("neighbours at real spacing"); clp.add(sdir + "close" + s); clp.add("the trio at real spacing, close"); }
            for (String d : dense) { clp.add(d); clp.add("every frame round the event"); }
            put(ev, "clipping", clp.toArray(new String[0]));
            List<String> bl = new ArrayList<>(); for (String d : dense) { bl.add(d); bl.add("every frame round the event"); } bl.add(sdir + "delta.png"); bl.add("per-tick pose delta; manifest states[].delta.windows (flag rule there)");
            for (String t : relatedTransitions(n, stance)) { bl.add(t); bl.add("a related transition"); }
            put(ev, "blend", bl.toArray(new String[0]));
            all.add(n, ev);
        }
        return all;
    }
    List<String> relatedTransitions(String state, String stance) {
        List<String> o = new ArrayList<>();
        if (state.equals("stance-wall") || state.equals("stance-brace")) o.add("transitions/hold-wall-brace/dense_f020.png");
        if (state.equals("stance-carry") || state.equals("stance-guard")) o.add("transitions/guard-carry/dense_f020.png");
        if (state.equals("stance-shield") || state.equals("hit-block")) o.add("transitions/shield-up-down/dense_f020.png");
        if (state.startsWith("attack-")) { o.add("transitions/hit-while-attacking/dense_f025.png"); o.add("transitions/death-while-attacking/dense_f025.png"); }
        if (state.startsWith("hit-")) o.add("transitions/hit-while-attacking/dense_f025.png");
        if (state.startsWith("death-")) o.add("transitions/death-while-attacking/dense_f025.png");
        // R80: each stance's own gait (idle walks, carry runs, guard and brace shuffle in the crawl and backpedal, the raised shield crawls)
        java.util.function.Consumer<String> gait = gn -> { o.add("loco/" + gn + "/dense_idle-to-move.png"); o.add("loco/" + gn + "/dense_move-to-stop.png"); };
        if (state.equals("stance-idle")) gait.accept("walk");
        if (state.equals("stance-carry")) gait.accept("run");
        if (state.equals("stance-guard") || state.equals("stance-brace")) { gait.accept("crawl"); gait.accept("backpedal"); gait.accept("guard-shuffle"); }
        if (state.equals("stance-shield")) gait.accept("crawl");
        o.removeIf(p -> sheetFiles().stream().noneMatch(p::equals));
        return o;
    }
    Set<String> sheetFileSet;
    Set<String> sheetFiles() { if (sheetFileSet == null) { sheetFileSet = new java.util.HashSet<>(); for (var x : sheets) sheetFileSet.add(x.getAsJsonObject().get("file").getAsString()); } return sheetFileSet; }
    void put(JsonObject ev, String key, String... fileNote) {
        var a = new JsonArray(); Set<String> have = sheetFiles();
        for (int i = 0; i + 1 < fileNote.length; i += 2) { String f = fileNote[i]; if (!have.contains(f) && !byFile.containsKey(f)) continue; var o = new JsonObject(); o.addProperty("file", f); o.addProperty("note", fileNote[i + 1]); a.add(o); }
        ev.add(key, a);
    }

    // ---- the manifest and the sheets ----
    JsonObject cameraJson() {
        var j = new JsonObject();
        j.addProperty("fov", FOV); j.addProperty("window", WIN_W + "x" + WIN_H); j.addProperty("framebuffer", fbW + "x" + fbH); j.addProperty("troopYaw", YAW);
        j.addProperty("cloud", String.format(Locale.ROOT, "%d points: every box corner and weapon point of every clip sampled each 1/20 s; radius %.2f, top %.2f, bottom %.2f blocks", cloud.size(), cloudR, cloudTop, cloudBottom));
        j.addProperty("solo", String.format(Locale.ROOT, "%.2f blocks from the troop at 8 angles (0 front, 90 its right), eye %.2f, level; fitted so every clip's extent from every angle fills at most %.0f percent of the frame; crop %s of the frame (the union of the extent's projections plus 10 percent), native, no downscale", soloD, soloEye, FILL * 100, Arrays.toString(soloCrop)));
        j.addProperty("loco", String.format(Locale.ROOT, "the solo side camera (%.2f blocks, eye %.2f) tracking the troop each tick along x; crop %s (the solo crop 15 percent wider each side)", soloD, soloEye, Arrays.toString(locoCrop)));
        var cl = new JsonObject(); for (var e : closeCams.entrySet()) { Cam k = closeCam(e.getKey(), e.getValue()[0]); cl.addProperty(String.valueOf(r4(e.getKey())), String.format(Locale.ROOT, "spacing %.2f: %.2f blocks from the centre troop, %.0f degrees toward its right, eye %.2f, pitched %.1f down; trio crop %s, centre crop %s", e.getKey(), e.getValue()[0], CLOSE_TURN, k.eye(), k.pitch(), Arrays.toString(closeCrops.get(e.getKey())[0]), Arrays.toString(closeCrops.get(e.getKey())[1]))); }
        j.add("close", cl);
        j.addProperty("high", String.format(Locale.ROOT, "the formation and the wide overview from (0, %.2f over the ground, %.2f) in front, pitched at the grid's middle: the nearest row about 31 blocks away", HIGH_EYE, HIGH_Z));
        j.addProperty("level", String.format(Locale.ROOT, "the formation from (0, eye %.2f, %.2f), level: the nearest row %.1f blocks away", LEVEL_EYE, LEVEL_Z, -LEVEL_Z));
        j.addProperty("spacing", String.format(Locale.ROOT, "real spacing (R73): a shield troop in wall, brace, shield_move or hold stands shieldCell = max(%.2f, width + 0.1) = %.2f apart; any other max(formation %.2f, max(1.3, width + 0.3)) = %.2f. The wide grid (%.0f x %.0f) is only the clone-check overview",
                Spec.COMBATDISCIPLINE[0].shieldWallCell(), cellFor("wall"), Spec.FORMATIONS[0].spacing(), cellFor(""), WIDE_X, WIDE_Z));
        return j;
    }
    void writeManifest() {
        var m = new JsonObject();
        m.addProperty("family", arg); m.addProperty("engineFamily", famName); m.addProperty("troop", troop); m.addProperty("rig", rig); m.addProperty("stub", stub);
        m.addProperty("created", stamp); m.addProperty("out", out.getAbsolutePath()); var pa = new JsonArray(); for (String p : PARTS) pa.add(p); m.add("parts", pa);
        m.addProperty("pipeline", "every sheet is the full TroopMotion pipeline as the game draws it (engine layers, MotionFeet, scene and death handling); clips are asked for only at the input level (stance packets, attack and hit events, scene poses, the server's Tactics.shield, real kills). No sheet is a raw clip with the engine layers off.");
        m.addProperty("shieldTroop", shieldTroop); m.addProperty("width", width);
        m.add("camera", cameraJson());
        var tl = new JsonObject(); tl.addProperty("captureFrames", CAPTURE); tl.addProperty("fps", Clip.TICKS); tl.addProperty("settleTicks", SETTLE); tl.addProperty("event1AfterFrame", EVENT1); tl.addProperty("event2AfterFrame", EVENT2); tl.addProperty("stripEvery", STRIP); tl.addProperty("denseWindow", DENSE);
        tl.addProperty("note", "frame f is the frame drawn last when client tick f ran; events fire right after frame f is captured, so f+1 is the first that can show them; events from the server (a kill, the shield) reach this client a tick or more later");
        m.add("timeline", tl);
        m.add("units", unitsJson); m.add("clips", clipsJson); m.add("states", statesJson); m.add("loco", locoJson); m.add("transitions", transJson); m.add("hitSweep", hitSweepJson); m.add("crowdDistances", crowdJson);
        double near = 1e9; for (var x : crowdJson) near = Math.min(near, x.getAsJsonObject().get("nearestFeetDist").getAsDouble()); if (near < 1e9) m.addProperty("crowdNearestDist", r4(near));
        var lj = new JsonObject(); for (var x : transJson) { var j = x.getAsJsonObject(); if (!sweep(j.get("name").getAsString()) && j.getAsJsonObject("delta").has("largestJump")) lj.add(j.get("name").getAsString(), j.getAsJsonObject("delta").get("largestJump")); }
        for (var x : locoJson) { var j = x.getAsJsonObject(); if (j.has("delta") && j.getAsJsonObject("delta").has("largestJump")) lj.add("loco-" + j.get("name").getAsString(), j.getAsJsonObject("delta").get("largestJump")); }
        m.add("largestJumpPerTransition", lj);
        m.add("sheets", sheets); m.add("grading", grading()); m.add("files", files);
        var notes = new JsonArray();
        notes.add("Volleys are synchronised: every troop in a crowd, formation or close shot gets its attack or hit on the same tick. Judge variation by the stances' phase and amplitude and by the faces and seeds, not by event timing.");
        notes.add("Silhouette at distance: cite the level shot (eye height 1.62, the nearest troop at least 30 blocks); variation: the formation shot from high up (real spacing) first, the wide grid only as the clone-check overview.");
        notes.add("Deaths: f028 shows vanilla's red death tint, as in game; f050 and f080 are death ticks about 40 and 70.");
        notes.add("Blend continuity: cite the dense sheets and the delta plots, not the GIF.");
        m.add("notes", notes);
        var ck = new JsonObject(); ck.addProperty("shotsOff", bad.size()); ck.addProperty("captureOff", capBad.size()); ck.addProperty("seedsOff", seedBad.size()); ck.addProperty("motionOff", motionBad.size()); ck.addProperty("replaysOff", replayBad.size()); ck.addProperty("framingOff", framingBad.size()); ck.addProperty("locoOff", locoBad.size()); ck.addProperty("captureTimingRule", TIMING_RULE); ck.add("captureTiming", timingJson);
        var we = new JsonArray(); for (String x : new ArrayList<>(writeErrors)) we.add(x); ck.add("writeErrors", we); var cv = new JsonArray(); for (String x : covered) cv.add(x); ck.add("framesUnderAScreen", cv); var nz = new JsonArray(); for (String x : noZoom) nz.add(x); ck.add("zoomsMissing", nz); m.add("checks", ck);
        try { java.nio.file.Files.writeString(new File(out, "manifest.json").toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(m)); }
        catch (IOException x) { writeErrors.add("manifest.json: " + x); }
    }
    void startSheets() {
        File script = new File(REPO, "tools/contact_sheet.py");
        if (REPO.isBlank() || !script.isFile()) { pyWhy = "no tools/contact_sheet.py under '" + REPO + "'"; return; }
        try {
            var pb = new ProcessBuilder(PYTHON, script.getAbsolutePath(), out.getAbsolutePath(), "--repo", REPO).redirectErrorStream(true).redirectOutput(new File(out, "sheets.log"));
            if (NO_PILLOW) pb.environment().put("GRAVES_GRADE_NO_PILLOW", "1");
            py = pb.start();
        } catch (IOException x) { pyWhy = "python did not start: " + x; }
    }
    void afterSheets() {
        if (py != null) { if (py.isAlive()) { py.destroy(); pyWhy = "contact_sheet.py still running after 9000 ticks"; } else pyExit = py.exitValue(); }
        boolean pillow = pyExit == 0;
        if (pyExit == 3) javaSheets();
        else if (pyExit != 0 && pyWhy.isEmpty()) pyWhy = "contact_sheet.py exited " + pyExit + " (see sheets.log)";
        List<String> missing = new ArrayList<>(); Map<String, Integer> kinds = new TreeMap<>(); int n = 0;
        try {
            var m = JsonParser.parseString(java.nio.file.Files.readString(new File(out, "manifest.json").toPath())).getAsJsonObject();
            for (var x : m.getAsJsonArray("files")) {
                var j = x.getAsJsonObject(); String file = j.get("file").getAsString(); n++; kinds.merge(j.get("kind").getAsString(), 1, Integer::sum);
                if (!new File(out, file).isFile()) { if (missing.size() < 10) missing.add(file); else if (missing.size() == 10) missing.add("..."); }
            }
            for (var x : m.getAsJsonArray("sheets")) { String file = x.getAsJsonObject().get("file").getAsString(); if (!new File(out, file).isFile()) { if (missing.size() < 10) missing.add(file); else if (missing.size() == 10) missing.add("..."); } }
            if (m.has("pillow")) pillow = m.get("pillow").getAsBoolean();
        } catch (Exception x) { missing.add("manifest.json unreadable: " + x); }
        List<String> want = new ArrayList<>(List.of("template"));
        if (PARTS.contains("solo")) want.addAll(List.of("frame", "turn", "contact", "turnaround", "trail"));
        if (PARTS.contains("loco")) want.addAll(List.of("loco_frame", "loco", "loco_trace", "dense", "delta"));
        if (PARTS.contains("states")) want.addAll(List.of("crowd", "crowd_zoom", "formed", "formed_zoom", "level", "level_zoom", "close", "capture", "strip", "dense", "delta"));
        if (PARTS.contains("transitions")) want.addAll(List.of("capture", "strip", "dense", "delta"));
        if (PARTS.contains("hits") && !PARTS.contains("transitions")) want.addAll(List.of("capture", "strip"));
        if (pillow && PARTS.contains("states")) want.add("gif");
        List<String> absent = new ArrayList<>(); for (String w : new LinkedHashSet<>(want)) if (!kinds.containsKey(w)) absent.add(w);
        // R80: every loco segment its own sheets, trace, delta plot and both dense windows; a missing one fails the files check
        List<String> locoMissing = new ArrayList<>();
        if (PARTS.contains("loco")) for (Seg g : segs) {
            String d = "loco/" + g.name() + "/"; List<String> need = new ArrayList<>(List.of(d + "loco.png", d + "loco_trace.png", d + "delta.png"));
            for (String e : g.turn() ? new String[]{"turn-starts", "turn-held"} : new String[]{"idle-to-move", "move-to-stop"}) need.add(d + "dense_" + e + ".png");
            for (String f : need) if (!new File(out, f).isFile()) locoMissing.add(f);
        }
        boolean ok = missing.isEmpty() && absent.isEmpty() && locoMissing.isEmpty() && writeErrors.isEmpty() && pyWhy.isEmpty() && covered.isEmpty() && noZoom.isEmpty();
        report("mo-grade-files", ok, n + " files in " + out.getAbsolutePath() + " by kind " + kinds + (pillow ? " (sheets by contact_sheet.py with Pillow)" : pyExit == 3 ? " (sheets by ImageIO: no Pillow" + (NO_PILLOW ? ", GRAVES_GRADE_NO_PILLOW" : "") + ")" : "")
            + (PARTS.contains("loco") ? "; every loco segment (" + segs.size() + ") has loco, loco_trace, delta and both dense sheets" + (locoMissing.isEmpty() ? "" : " except: " + locoMissing) : "")
            + (missing.isEmpty() ? "" : "; missing: " + missing) + (absent.isEmpty() ? "" : "; no " + absent) + (writeErrors.isEmpty() ? "" : "; write errors: " + writeErrors) + (covered.isEmpty() ? "" : "; frames taken with a screen over the world: " + covered) + (noZoom.isEmpty() ? "" : "; zoom shots not taken: " + noZoom) + (pyWhy.isEmpty() ? "" : "; " + pyWhy));
    }
    /** Without Pillow: the manifest's sheet specs drawn with ImageIO (the same specs contact_sheet.py renders) and added to the manifest. */
    void javaSheets() {
        try {
            File mf = new File(out, "manifest.json");
            var m = JsonParser.parseString(java.nio.file.Files.readString(mf.toPath())).getAsJsonObject(); var fl = m.getAsJsonArray("files");
            Map<String, BufferedImage> cache = new HashMap<>();
            for (var x : m.getAsJsonArray("sheets")) {
                var sp = x.getAsJsonObject(); String rel = sp.get("file").getAsString();
                BufferedImage img = sp.has("plot") ? SheetArt.plot(sp.getAsJsonObject("plot")) : SheetArt.grid(sp, out, cache);
                File f = new File(out, rel); f.getParentFile().mkdirs(); ImageIO.write(img, "png", f);
                var j = new JsonObject(); j.addProperty("kind", sp.get("kind").getAsString()); j.addProperty("file", rel); if (sp.has("clip")) j.addProperty("clip", sp.get("clip").getAsString()); if (sp.has("state")) j.addProperty("state", sp.get("state").getAsString()); j.addProperty("renderer", "ImageIO"); fl.add(j);
                if (cache.size() > 64) cache.clear();
            }
            m.addProperty("pillow", false); java.nio.file.Files.writeString(mf.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(m));
        } catch (Exception x) { pyWhy = "the ImageIO sheets failed: " + x; x.printStackTrace(); }
    }

    public void cleanup(MinecraftClient c) {
        WATCH.clear(); LAST = null; raised.clear(); locoPath = null; locoI = -1;
        for (var u : cast) { var e = client(c, u.mob); if (e != null) { TroopMotion.setScenePose(e.getId(), null); TroopModels.forceFace(e, -1); } }
        Phase2Checks.Turn.noTarget = false; TroopMotion.clearForced();
        if (clipsBefore != null) { ClipLibrary.install(clipsBefore); clipsBefore = null; }
        if (cloudsBefore != null) { c.options.getCloudRenderMode().setValue(cloudsBefore); cloudsBefore = null; }
        if (fovWas != null) { c.options.getFov().setValue(fovWas); fovWas = null; }
        if (bobWas != null) { c.options.getBobView().setValue(bobWas); bobWas = null; }
        if (oldGui >= 0 && oldW > 0) { Phase1Checks.window(c, oldW, oldH, oldGui); oldGui = -1; }
        if (io != null) io.shutdown();
        if (py != null && py.isAlive()) py.destroy();
        if (frameImg != null) { frameImg.close(); frameImg = null; }
        super.cleanup(c);
    }

    /** The sheet renderer without Pillow: the grid and plot specs of the manifest, as tools/contact_sheet.py draws them. */
    static final class SheetArt {
        static final java.awt.Color BG = new java.awt.Color(24, 24, 24), FG = java.awt.Color.WHITE, DIM = new java.awt.Color(170, 170, 170), HL = new java.awt.Color(255, 220, 0);
        static java.awt.Color col(String hex) { try { return java.awt.Color.decode(hex); } catch (RuntimeException x) { return FG; } }
        static BufferedImage grid(JsonObject sp, File out, Map<String, BufferedImage> cache) throws IOException {
            int pad = 8, head = 30 + 18 * sp.getAsJsonArray("sub").size(), rowLab = 20, tileLab = 16;
            List<int[]> sizes = new ArrayList<>(); int W = 900, H = head;
            for (var r : sp.getAsJsonArray("rows")) {
                var row = r.getAsJsonObject(); int w = pad, h = 0;
                for (var t : row.getAsJsonArray("tiles")) { int[] d = dims(t.getAsJsonObject(), out, cache); w += d[0] + pad; h = Math.max(h, d[1]); }
                sizes.add(new int[]{w, h}); W = Math.max(W, w); H += (row.get("label").getAsString().isEmpty() ? 0 : rowLab) + tileLab + h + pad;
            }
            BufferedImage img = new BufferedImage(W, H + pad, BufferedImage.TYPE_INT_RGB); var g = img.createGraphics();
            g.setColor(BG); g.fillRect(0, 0, img.getWidth(), img.getHeight());
            g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(FG); g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 18)); g.drawString(sp.get("title").getAsString(), pad, 22);
            g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 13)); g.setColor(DIM); int y = 40;
            for (var s : sp.getAsJsonArray("sub")) { g.drawString(s.getAsString(), pad, y); y += 18; }
            y = head; int ri = 0;
            for (var r : sp.getAsJsonArray("rows")) {
                var row = r.getAsJsonObject(); int h = sizes.get(ri++)[1];
                if (!row.get("label").getAsString().isEmpty()) { g.setColor(FG); g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 14)); g.drawString(row.get("label").getAsString(), pad, y + 15); y += rowLab; }
                int x = pad;
                for (var tt : row.getAsJsonArray("tiles")) {
                    var t = tt.getAsJsonObject(); int[] d = dims(t, out, cache);
                    g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12)); g.setColor(t.has("hl") ? HL : FG); g.drawString(t.get("label").getAsString(), x + 2, y + 12);
                    BufferedImage src = load(t.get("src").getAsString(), out, cache); int ty = y + tileLab;
                    if (src != null) {
                        int[] c = crop(t, src); int sc = t.get("scale").getAsInt();
                        var sub = src.getSubimage(c[0], c[1], c[2], c[3]);
                        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, t.get("nearest").getAsBoolean() ? java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g.drawImage(sub, x, ty, c[2] * sc, c[3] * sc, null);
                        var clip = g.getClip(); g.setClip(x, ty, c[2] * sc, c[3] * sc); marks(g, t.getAsJsonArray("marks"), c[0], c[1], sc, x, ty); g.setClip(clip);
                    } else { g.setColor(DIM); g.drawString("missing", x + 4, ty + 20); }
                    if (t.has("hl")) { g.setColor(HL); g.setStroke(new java.awt.BasicStroke(3)); g.drawRect(x - 2, ty - 2, d[0] + 3, d[1] + 3); g.setStroke(new java.awt.BasicStroke(1)); }
                    x += d[0] + pad;
                }
                y += tileLab + h + pad;
            }
            g.dispose(); return img;
        }
        static BufferedImage load(String rel, File out, Map<String, BufferedImage> cache) throws IOException {
            if (cache.containsKey(rel)) return cache.get(rel); File f = new File(out, rel); BufferedImage i = f.isFile() ? ImageIO.read(f) : null; cache.put(rel, i); return i;
        }
        static int[] crop(JsonObject t, BufferedImage src) {
            if (!t.has("crop")) return new int[]{0, 0, src.getWidth(), src.getHeight()};
            var a = t.getAsJsonArray("crop"); int x = Math.max(0, Math.min(src.getWidth() - 1, a.get(0).getAsInt())), y = Math.max(0, Math.min(src.getHeight() - 1, a.get(1).getAsInt()));
            return new int[]{x, y, Math.max(1, Math.min(src.getWidth() - x, a.get(2).getAsInt())), Math.max(1, Math.min(src.getHeight() - y, a.get(3).getAsInt()))};
        }
        static int[] dims(JsonObject t, File out, Map<String, BufferedImage> cache) throws IOException {
            BufferedImage src = load(t.get("src").getAsString(), out, cache); if (src == null) return new int[]{200, 100};
            int[] c = crop(t, src); int sc = t.get("scale").getAsInt(); return new int[]{c[2] * sc, c[3] * sc};
        }
        static void marks(java.awt.Graphics2D g, JsonArray m, int cx, int cy, int sc, int ox, int oy) {
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            for (var x : m) {
                var j = x.getAsJsonObject(); String t = j.get("t").getAsString(); g.setColor(col(j.get("c").getAsString()));
                switch (t) {
                    case "line", "poly" -> {
                        var p = j.getAsJsonArray("pts"); int n = p.size(); int[] xs = new int[n], ys = new int[n];
                        for (int i = 0; i < n; i++) { var q = p.get(i).getAsJsonArray(); xs[i] = (int) Math.round((q.get(0).getAsDouble() - cx) * sc + ox); ys[i] = (int) Math.round((q.get(1).getAsDouble() - cy) * sc + oy); }
                        g.setStroke(new java.awt.BasicStroke(j.has("w") ? j.get("w").getAsInt() : 1)); g.drawPolyline(xs, ys, n);
                        if (j.has("dots")) for (int i = 0; i < n; i++) g.fillOval(xs[i] - 2, ys[i] - 2, 5, 5);
                        g.setStroke(new java.awt.BasicStroke(1));
                    }
                    case "dot" -> { var q = j.getAsJsonArray("p"); int r = j.get("r").getAsInt(); int px = (int) Math.round((q.get(0).getAsDouble() - cx) * sc + ox), py = (int) Math.round((q.get(1).getAsDouble() - cy) * sc + oy); g.fillOval(px - r, py - r, 2 * r + 1, 2 * r + 1); g.setColor(java.awt.Color.BLACK); g.drawOval(px - r, py - r, 2 * r + 1, 2 * r + 1); }
                    case "text" -> { var q = j.getAsJsonArray("p"); g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 11)); g.drawString(j.get("s").getAsString(), (int) Math.round((q.get(0).getAsDouble() - cx) * sc + ox), (int) Math.round((q.get(1).getAsDouble() - cy) * sc + oy) + 10); }
                    default -> {}
                }
            }
        }
        static double niceStep(double raw) { if (!(raw > 0)) return 1; double e = Math.pow(10, Math.floor(Math.log10(raw))); for (double m : new double[]{1, 2, 5, 10}) if (m * e >= raw) return m * e; return 10 * e; }
        static BufferedImage plot(JsonObject p) {
            int W = p.get("w").getAsInt(), H = p.get("h").getAsInt(), left = 80, right = 20, top = 40, gap = 46; var panels = p.getAsJsonArray("panels"); int np = Math.max(1, panels.size());
            int ph = (H - top - 30 - gap * (np - 1)) / np;
            BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB); var g = img.createGraphics(); g.setColor(BG); g.fillRect(0, 0, W, H);
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON); g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(FG); g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 16)); g.drawString(p.get("title").getAsString(), left, 24);
            double x0 = 1e9, x1 = -1e9;
            for (var pa : panels) for (var s : pa.getAsJsonObject().getAsJsonArray("series")) for (var v : s.getAsJsonObject().getAsJsonArray("x")) { x0 = Math.min(x0, v.getAsDouble()); x1 = Math.max(x1, v.getAsDouble()); }
            if (x0 >= x1) { x0 = 0; x1 = 1; }
            int pw = W - left - right;
            for (int i = 0; i < panels.size(); i++) {
                var pa = panels.get(i).getAsJsonObject(); int py = top + i * (ph + gap);
                double y0 = 0, y1 = 0; for (var s : pa.getAsJsonArray("series")) for (var v : s.getAsJsonObject().getAsJsonArray("y")) { y0 = Math.min(y0, v.getAsDouble()); y1 = Math.max(y1, v.getAsDouble()); }
                for (var h : pa.getAsJsonArray("hlines")) { double v = h.getAsJsonObject().get("y").getAsDouble(); y0 = Math.min(y0, v); y1 = Math.max(y1, v); }
                if (y1 - y0 < 1e-9) y1 = y0 + 1; double padY = (y1 - y0) * 0.06; y0 -= padY; y1 += padY;
                g.setColor(new java.awt.Color(40, 40, 40)); g.fillRect(left, py, pw, ph); g.setColor(DIM); g.drawRect(left, py, pw, ph);
                g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12)); g.drawString(pa.get("title").getAsString() + "  [" + pa.get("ylabel").getAsString() + "]", left + 4, py - 6);
                double st = niceStep((y1 - y0) / 4); int dp = Math.max(0, (int) -Math.floor(Math.log10(st) + 1e-9)); // even steps (1, 2 or 5 x 10^n), as contact_sheet.py draws them
                for (double v = Math.ceil(y0 / st - 1e-9) * st; v <= y1 + 1e-9; v += st) { int yy = (int) (py + ph - (v - y0) / (y1 - y0) * ph); g.setColor(new java.awt.Color(70, 70, 70)); g.drawLine(left, yy, left + pw, yy); g.setColor(DIM); g.drawString(String.format(Locale.ROOT, "%." + dp + "f", Math.abs(v) < st / 1e6 ? 0 : v), 6, yy + 4); }
                for (var h : pa.getAsJsonArray("hlines")) { var o = h.getAsJsonObject(); int yy = (int) (py + ph - (o.get("y").getAsDouble() - y0) / (y1 - y0) * ph); g.setColor(col(o.get("c").getAsString())); g.setStroke(new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_MITER, 1, new float[]{6, 4}, 0)); g.drawLine(left, yy, left + pw, yy); g.setStroke(new java.awt.BasicStroke(1)); g.drawString(o.get("label").getAsString(), left + pw - 260, yy - 3); }
                int ly = py + 16;
                for (var s : pa.getAsJsonArray("series")) {
                    var o = s.getAsJsonObject(); var xs = o.getAsJsonArray("x"); var ys = o.getAsJsonArray("y"); int n = Math.min(xs.size(), ys.size()); int[] ax = new int[n], ay = new int[n];
                    for (int k = 0; k < n; k++) { ax[k] = (int) (left + (xs.get(k).getAsDouble() - x0) / (x1 - x0) * pw); ay[k] = (int) (py + ph - (ys.get(k).getAsDouble() - y0) / (y1 - y0) * ph); }
                    g.setColor(col(o.get("c").getAsString())); g.setStroke(new java.awt.BasicStroke(2)); g.drawPolyline(ax, ay, n); g.setStroke(new java.awt.BasicStroke(1));
                    g.drawString(o.get("name").getAsString(), left + 8, ly); ly += 14;
                }
                for (var v : p.getAsJsonArray("vlines")) { var o = v.getAsJsonObject(); int xx = (int) (left + (o.get("x").getAsDouble() - x0) / (x1 - x0) * pw); g.setColor(col(o.get("c").getAsString())); g.drawLine(xx, py, xx, py + ph); if (i == 0) g.drawString(o.get("label").getAsString(), xx + 3, py + ph - 6); }
                if (i == panels.size() - 1) { g.setColor(DIM); for (double xv = Math.ceil(x0 / 10) * 10; xv <= x1; xv += 10) { int xx = (int) (left + (xv - x0) / (x1 - x0) * pw); g.drawString(String.valueOf((int) xv), xx - 6, py + ph + 16); } g.drawString("frame", left + pw - 40, py + ph + 28); }
            }
            g.dispose(); return img;
        }
    }
}
