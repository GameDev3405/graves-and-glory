package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.gravesandglory.mixin.LivingEntityRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.io.Reader;
import java.util.*;

/**
 * The one portrait camera: every live unit the UI shows in a frame goes through here (the 40 px roll and seat frames, the 160 px
 * letters, the commander's frames, and the roll card's model through {@link #full}). Unit identity rule 8, revised for the portrait
 * audit (Kort, 2026-11-11: units stared off, lost their heads to the frame's top and moved in lockstep):
 * <ul>
 * <li>Framing by the model's own head ({@link Bounds}): the window runs from {@link #PAD} model px above the highest point the head
 * carries (helmet, crest, crown, hood) down to {@link #BELOW} face heights under the chin, so every face shows whole and the
 * shoulders and chest read under it. A big head (the Hollow King's crown) widens the view; a small one is held to {@link #MIN_SPAN}.
 * A face that stands forward of the feet (a hound, a beast) is put back over the window's centre. {@link #TUNE} holds the few
 * per-troop corrections the bounds alone do not get right.</li>
 * <li>Gaze: the body turns {@link #BODY_YAW} - 180 degrees off facing the viewer (a three-quarter view), the head turns back to
 * {@link #HEAD_YAW}, so the face looks almost at the viewer, a little down toward the camera ({@link #HEAD_PITCH}), which sits
 * {@link #TILT} degrees below eye level.</li>
 * <li>Phase: each frame on screen draws its own copy of the portrait mob ({@link #twin}), whose clock runs {@link #phase} ticks
 * apart from its neighbours', so a row of frames never breathes in lockstep. TroopMotion and GeckoLib both read that clock.</li>
 * </ul>
 * Canvas pixels: call draw between UiCanvas.begin and end.
 */
final class Portrait {
    private Portrait() {}

    /** The body's yaw: 180 faces the viewer, so the body turns 30 degrees to its left (rule 8's three-quarter view). */
    static final float BODY_YAW = 210f;
    /** The head's yaw: turned back 18 degrees from the body, so the face is 12 degrees off the viewer and reads as looking at them. */
    static final float HEAD_YAW = 192f;
    /** The head's pitch, degrees down: the camera sits under eye level, so the eyes come down a little to meet it. */
    static final float HEAD_PITCH = 4f;
    /** The camera's tilt below eye level, in degrees, built as vanilla's mouse overload builds its X tilt. */
    static final float TILT = 5f;
    /** Model px shown above the head's highest point. */
    static final float PAD = 2.5f;
    /** Face heights shown under the chin: the shoulders and the top of the chest. */
    static final float BELOW = 1.5f;
    /** The least and most model px a window spans top to bottom. */
    static final float MIN_SPAN = 18f, MAX_SPAN = 46f;
    /** Ticks of idle clock the frames on screen are spread over (longer than any idle loop, so neighbours land anywhere in it). */
    static final int PHASE_TICKS = 120;

    /** Per-troop corrections where the head bounds alone frame badly: {span multiplier, model px to move the view down, model px to
     *  move it toward the face's side}. Empty rows are not kept; see the portrait audit report for why each row exists. */
    static final Map<String, float[]> TUNE = Map.of(
        // the Tidecaller brute's hunched back stands well above its low-slung head: frame up and out to keep both
        "tc_brute", new float[]{1.2f, -6f, 0f});
    /** The most model px a window spans for a head whose gear stands tall over its face (see fit). */
    static final float HEAD_SPAN = 30f;

    /** What the last draw passed to InventoryScreen.drawEntity: the window's centre x (where the feet stand), the feet y and the
     *  model size, in canvas px, with the window's size, the model px it spans, the renderer's own scale the size was divided by,
     *  whether it drew a silhouette, the top-left corner of the window it scissored the model to, and the frame's clock offset in
     *  ticks. Null before the first draw. */
    record Drawn(int x, int feet, int size, int window, float span, float scale, boolean silhouette, int left, int top, int phase) {}
    private static Drawn last;
    static Drawn last() { return last; }

    /** Where a portrait stands in its window: the feet's x and y offsets from the window's top-left and drawEntity's size. */
    record Fit(int dx, int feet, int size, float span) {}

    /** The head of a model at rest, in model px above the feet (y up): the highest point it carries over every tier, the top and
     *  bottom of the face (the head bone's own boxes), and how far the face stands in front of the feet. */
    record Bounds(float top, float faceTop, float faceBottom, float forward) {
        /** A player's head: the 32 px biped. */
        static final Bounds BIPED = new Bounds(32, 32, 24, 0);
    }

    /** Draws e in the window (x, y, window x window), scissored to it. The entity's yaws, pitch and age are put back as they were.
     *  Other players' name labels stay off, as the HUD does. */
    static void draw(DrawContext g, LivingEntity e, int x, int y, int window) { draw(g, e, x, y, window, false); }

    /** As draw(g, e, x, y, window), flat in DarkUi.SILHOUETTE when silhouette is set: the model is drawn with every fragment fully
     *  fogged (fog from -2 to -1, so any distance is past its end) in that colour, keeping the model's own alpha, and the fog is put
     *  back as it was. */
    static void draw(DrawContext g, LivingEntity source, int x, int y, int window, boolean silhouette) {
        var c = MinecraftClient.getInstance();
        LivingEntity e = twin(source, x, y, window);
        int phase = e == source ? 0 : phase(x, y, window);
        float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        float[] fogColor = RenderSystem.getShaderFogColor().clone();
        Quaternionf camera = new Quaternionf().rotateX(TILT * (float) Math.PI / 180f);
        Quaternionf body = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
        UiCanvas.scissor(g, x, y, window, window);
        Pose pose = Pose.of(e, c);
        try {
            pose.apply(e, c, clock(e, source, phase));
            Fit f = fit(e, window);
            float scale = modelScale(e);
            int cx = x + f.dx(), feet = y + f.feet();
            if (silhouette) {
                int s = DarkUi.SILHOUETTE;
                RenderSystem.setShaderFogStart(-2f);
                RenderSystem.setShaderFogEnd(-1f);
                RenderSystem.setShaderFogColor(((s >> 16) & 255) / 255f, ((s >> 8) & 255) / 255f, (s & 255) / 255f, 1f);
            }
            last = new Drawn(cx, feet, f.size(), window, f.span(), scale, silhouette, x, y, phase);
            InventoryScreen.drawEntity(g, cx, feet, f.size(), body, camera, e);
            // the model stands in front of the page (z 50 and up), and text drawn later tests depth: clear the window's depth
            // (the scissor bounds the clear) so a note laid over the portrait is not cut where the model was
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        } finally {
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            RenderSystem.setShaderFogColor(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            pose.restore(e, c);
            UiCanvas.endScissor(g);
        }
    }

    /**
     * A whole-body portrait in GUI px (the roll card and the old troop card): the feet at (x, bottom), sized as ArmyCards.size asks,
     * shrunk further when the model stands taller than a 32 px biped so its head stays on the card; posed and phased as a framed one.
     */
    static void full(DrawContext g, MobEntity source, int x, int bottom, int size) {
        var c = MinecraftClient.getInstance();
        LivingEntity e = twin(source, x, bottom, size);
        int phase = e == source ? 0 : phase(x, bottom, size);
        Bounds b = bounds(e);
        int sized = Math.max(1, Math.round(size * Math.min(1f, 33f / (b.top() + 1f))));
        Quaternionf camera = new Quaternionf().rotateX(TILT * (float) Math.PI / 180f);
        Quaternionf body = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
        Pose pose = Pose.of(e, c);
        try {
            pose.apply(e, c, clock(e, source, phase));
            TroopModels.sized(e, sized);
            InventoryScreen.drawEntity(g, x, bottom, sized, body, camera, e);
        } finally {
            pose.restore(e, c);
        }
    }

    /** The fit of e's head in a window `window` canvas px square (Fit's offsets are from the window's top-left). */
    static Fit fit(LivingEntity e, int window) {
        Bounds b = bounds(e);
        float[] tune = TUNE.get(troopId(e));
        float faceH = Math.max(4f, Math.min(8f, b.faceTop() - b.faceBottom())); // a hood or a great helm is one box with the face: frame by a head's 8
        float upper = b.top() + PAD, lower = b.faceBottom() - BELOW * faceH;
        // tall headgear (a hood, a crown, antlers) takes its height from the chest, never from the face: at most HEAD_SPAN model px
        // a window, but always the face and a little under it
        lower = Math.min(b.faceBottom() - 4f, Math.max(lower, upper - HEAD_SPAN));
        float span = Math.max(MIN_SPAN, Math.min(MAX_SPAN, upper - lower));
        if (tune != null) { span *= tune[0]; upper -= tune[1]; }
        float px = window / span; // canvas px a model px
        float side = (float) Math.sin(Math.toRadians(BODY_YAW - 180f)) * b.forward() + (tune == null ? 0f : tune[2]);
        int size = Math.round(16f * px / modelScale(e));
        return new Fit(Math.round(window / 2f + side * px), Math.round(upper * px), size, span);
    }

    // ---- bounds ----

    private static final Map<String, Bounds> GEO = new HashMap<>();
    /** GEO's mark for a rig whose face it could not read (the legacy bounds stand in). */
    private static final Bounds NONE = new Bounds(0, 0, 0, 0);
    private static Object geoWorld;

    /** The head bounds of what draws e: a GeckoLib troop's rig (geo/troop/<id>.geo.json), else its TroopModel, else a biped's. */
    static Bounds bounds(LivingEntity e) {
        var look = TroopModels.resolve(e);
        if (look != null) {
            String id = look.troop().id();
            if (GeoTroopRenderer.handles(look)) {
                if (geoWorld != MinecraftClient.getInstance().world) { GEO.clear(); geoWorld = MinecraftClient.getInstance().world; } // a resource reload drops the world
                Bounds g = GEO.computeIfAbsent(id, k -> { Bounds r = geo(k); return r == null ? NONE : r; }); // NONE: read once, not every frame
                if (g != NONE) return g;
            }
            TroopModel<?> m = TroopModels.model(id);
            double[] h = m == null ? null : m.headAtRest();
            if (h != null) return new Bounds((float) (24 - h[0]), (float) (24 - h[1]), (float) (24 - h[2]), (float) h[3]);
        }
        return Bounds.BIPED;
    }

    /** The head subtree of a GeckoLib rig (Bedrock geometry: px, y up from the ground, the face toward -z); null when unreadable. */
    private static Bounds geo(String troop) {
        var id = new Identifier("gravesandglory", "geo/troop/" + troop + ".geo.json");
        var res = MinecraftClient.getInstance().getResourceManager().getResource(id);
        if (res.isEmpty()) return null;
        try (Reader r = res.get().getReader()) {
            JsonObject geo = JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            Map<String, String> parent = new HashMap<>();
            for (var el : geo.getAsJsonArray("bones")) { var b = el.getAsJsonObject(); parent.put(b.get("name").getAsString(), b.has("parent") ? b.get("parent").getAsString() : null); }
            float top = -Float.MAX_VALUE, faceTop = -Float.MAX_VALUE, faceBottom = Float.MAX_VALUE, forward = 0; int n = 0;
            for (var el : geo.getAsJsonArray("bones")) {
                var b = el.getAsJsonObject(); String name = b.get("name").getAsString();
                if (!b.has("cubes") || !under(name, parent)) continue;
                for (var ce : b.getAsJsonArray("cubes")) {
                    var cube = ce.getAsJsonObject(); var o = cube.getAsJsonArray("origin"); var s = cube.getAsJsonArray("size");
                    float inflate = cube.has("inflate") ? cube.get("inflate").getAsFloat() : 0f;
                    float y0 = o.get(1).getAsFloat() - inflate, y1 = o.get(1).getAsFloat() + s.get(1).getAsFloat() + inflate;
                    top = Math.max(top, y1);
                    if (face(name, parent)) { faceTop = Math.max(faceTop, y1); faceBottom = Math.min(faceBottom, y0); forward += -(o.get(2).getAsFloat() + s.get(2).getAsFloat() / 2f); n++; }
                }
            }
            if (n == 0) return null;
            return new Bounds(top, faceTop, faceBottom, forward / n);
        } catch (Exception ex) { return null; }
    }

    /** The face's boxes in a rig: the head bone's own, else the first face variant's (face_v0, the card face), which the rebuilt
     *  rigs hang under an empty head bone. */
    private static boolean face(String bone, Map<String, String> parent) {
        return bone.equals("head") || bone.equals("face_v0") || bone.equals("face") && "head".equals(parent.get(bone));
    }

    private static boolean under(String bone, Map<String, String> parent) {
        for (String b = bone; b != null; b = parent.get(b)) if (b.equals("head")) return true;
        return false;
    }

    static String troopId(LivingEntity e) { var look = TroopModels.resolve(e); return look == null ? "" : look.troop().id(); }

    // ---- one copy a frame, each on its own clock ----

    private static Object world;
    /** The copies by frame (the portrait mob's ArmyCards key and the frame's place), newest use last; at most TWIN_CAP kept. */
    private static final int TWIN_CAP = 160;
    private static final LinkedHashMap<String, MobEntity> TWINS = new LinkedHashMap<>(64, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, MobEntity> eldest) {
            if (size() <= TWIN_CAP) return false;
            forget(eldest.getValue()); return true;
        }
    };
    /** Each copy's source age at its first draw. A copy runs on the plain clock for WARM ticks, so GeckoLib starts its clips and
     *  finishes the opening blend into idle there (a controller re-anchors its clock where a blend ends, which would swallow an
     *  offset given earlier); the offset that follows then moves the loop's phase. Weak keys by identity. */
    private static final Map<Entity, Integer> STARTED = new MapMaker().weakKeys().makeMap();
    /** Ticks a copy runs unshifted before its offset applies: past GeoTroop.BLEND, the longest opening blend. */
    static final int WARM = 8;
    /** The copies whose clock is their own age (TroopMotion.time reads it instead of the world's). Weak keys by identity. */
    private static final Set<Entity> POSED = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    /** The motion state kept by entity id for a copy the cache let go (copies never unload, so ENTITY_UNLOAD never clears it). */
    private static void forget(MobEntity t) { TroopMotion.forget(t.getId()); MotionEvents.Client.forget(t.getId()); }

    /** The copy each portrait mob was last drawn through. Weak keys and values by identity. */
    private static final Map<Entity, Entity> LATEST = new MapMaker().weakKeys().weakValues().makeMap();
    /** The copy a portrait mob was last drawn through, or null: TroopRenderer.drawn(source) reads the copy's record, so a check that
     *  watches the shared portrait mob sees the frames that draw it. */
    static Entity latest(Entity source) { return LATEST.get(source); }

    /** Whether e is a portrait copy on its own clock (TroopMotion.time then takes e.age for the world's time). */
    static boolean posed(Entity e) { return !POSED.isEmpty() && POSED.contains(e); }

    /** The copy of a portrait mob that the frame at (x, y, window) draws, made on first use; e itself when it is no portrait mob
     *  (a player). */
    static LivingEntity twin(LivingEntity e, int x, int y, int window) {
        var c = MinecraftClient.getInstance();
        if (world != c.world) { TWINS.values().forEach(Portrait::forget); TWINS.clear(); world = c.world; }
        if (!(e instanceof MobEntity mob)) return e;
        String key = ArmyCards.keyOf(mob);
        if (key == null) return e;
        MobEntity t = TWINS.computeIfAbsent(key + "@" + x + "," + y + "," + window, k -> ArmyCards.copy(mob));
        if (t == null) return e;
        POSED.add(t);
        LATEST.put(mob, t);
        return t;
    }

    /** A frame's clock offset, ticks: a hash of its place, spread over PHASE_TICKS, so frames side by side land far apart. */
    static int phase(int x, int y, int window) {
        int h = x * 73856093 ^ y * 19349663 ^ window * 83492791;
        h ^= h >>> 13; h *= 1540483477; h ^= h >>> 15; // 1540483477: MurmurHash2's mixing constant
        return Math.floorMod(h, PHASE_TICKS);
    }

    /** The age a frame draws e at: the local player's age, plus the frame's offset once the copy is warm. */
    private static int clock(LivingEntity e, LivingEntity source, int phase) {
        if (e == source) return source.age;
        // the copy's clock is the local player's age (what the pages set on their mobs anyway): a caller that leaves the source's
        // age alone, as the roll card does, would otherwise freeze the copy on its first frame, mid-way into its opening blend
        var c = MinecraftClient.getInstance();
        int now = c.player != null ? c.player.age : c.world != null ? (int) c.world.getTime() : source.age;
        int first = STARTED.computeIfAbsent(e, k -> now);
        int since = now - first;
        return now + (since >= 0 && since < WARM ? 0 : phase);
    }

    /** An entity's yaws, pitch, age and the HUD flag, kept to put back after a portrait draw. */
    private record Pose(float bodyYaw, float prevBodyYaw, float yaw, float pitch, float prevPitch, float prevHead, float head, int age, boolean hudHidden) {
        static Pose of(LivingEntity e, MinecraftClient c) { return new Pose(e.bodyYaw, e.prevBodyYaw, e.getYaw(), e.getPitch(), e.prevPitch, e.prevHeadYaw, e.headYaw, e.age, c.options.hudHidden); }
        void apply(LivingEntity e, MinecraftClient c, int age) {
            e.bodyYaw = BODY_YAW; e.prevBodyYaw = BODY_YAW; e.setYaw(HEAD_YAW); e.setPitch(HEAD_PITCH); e.prevPitch = HEAD_PITCH;
            e.headYaw = HEAD_YAW; e.prevHeadYaw = HEAD_YAW; e.age = age;
            c.options.hudHidden = true; // LivingEntityRenderer.hasLabel asks MinecraftClient.isHudEnabled()
        }
        void restore(LivingEntity e, MinecraftClient c) {
            c.options.hudHidden = hudHidden;
            e.bodyYaw = bodyYaw; e.prevBodyYaw = prevBodyYaw; e.setYaw(yaw); e.setPitch(pitch); e.prevPitch = prevPitch;
            e.prevHeadYaw = prevHead; e.headYaw = head; e.age = age;
        }
    }

    /** The scale e's renderer gives its model about the feet (LivingEntityRenderer.scale), read from the renderer itself: 1 for a
     *  renderer that does not scale, or one that is not a LivingEntityRenderer. */
    static float modelScale(LivingEntity e) {
        Object renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(e);
        // A troop with no model of its own is drawn by the vanilla renderer TroopRenderer wraps, which applies its own scale:
        // probe that one (TroopRenderer.scale itself applies none). A troop with a model draws unscaled, so reads 1.
        if (renderer instanceof TroopRenderer<?> troop) {
            var look = TroopModels.resolve(e);
            if (look != null && TroopModels.modelOf(look.troop().id()) != null) return 1f;
            renderer = troop.vanilla();
        }
        if (!(renderer instanceof LivingEntityRendererAccessor living)) return 1f;
        MatrixStack probe = new MatrixStack();
        living.gravesandglory$scale(e, probe, 1f);
        float s = probe.peek().getPositionMatrix().m11();
        return s > 0 ? s : 1f;
    }
}
