package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BannerBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.RotationPropertyHelper;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The modded half of the cosmetics (cosmetics render plan 6): what a modded client draws beyond the vanilla patterns.
 * <ul>
 * <li>On the commander's held banner mesh (CommanderGearFeature.weapon, banner family): the stack's vanilla pattern layers on both faces of
 * the mesh's flag, with the vanilla banner pattern sprites (a modded client draws the mesh, not the vanilla item, so the patterns would
 * not show otherwise), then the extras of his equipped set (cosmetic_v1).</li>
 * <li>On a planted Forward Supply banner (its stand is an armour stand with a cosmetic_v1 entry): the extras, on the vanilla flag.</li>
 * <li>A title line under a player's name tag (never on yourself in first person), small and grey-gold.</li>
 * </ul>
 * The extras are small textured boxes in the flag's own frame (u across the flag from the pole side, v down it, n through it), all from one
 * 16x16 sheet of swatches (tools/build_cosmetic_extras.py). Their shapes and colours are placeholders until Kort's art review.
 */
public final class CosmeticExtras {
    private CosmeticExtras() {}

    static final Identifier TEX = Identifier.of("gravesandglory", "textures/entity/cosmetic/extras.png");
    /** Swatch cells of extras.png (tools/build_cosmetic_extras.py SWATCHES). */
    static final int CRIMSON = 0, GOLD = 1, IRON = 2, GILT = 3, BRASS = 4, DARK_GOLD = 6;
    static final int TITLE_INK = 0xFFC8B070;

    /** Harness probes: what the last frames drew ("held:<id>", "planted:<id>", "pattern:<n>" for a flag's layer count, "title:<text>"), cleared by begin(). */
    static final Set<String> drawn = ConcurrentHashMap.newKeySet();
    static final Set<String> drawing = ConcurrentHashMap.newKeySet();
    /** Harness: the extra drawn per frame is written to drawing, then swapped into drawn at the next frame's start. */
    static void begin() { drawn.clear(); drawn.addAll(drawing); drawing.clear(); }

    /**
     * A flag's frame in the current matrix's units (blocks): o the top corner on the pole side of the flag's mid-plane, u across the flag,
     * v down it, n its unit normal (front), half its half-thickness; the pole's top at (poleU, poleTopV) in flag units.
     */
    record Frame(Vector3f o, Vector3f u, Vector3f v, Vector3f n, float half, float poleU, float poleTopV) {
        Vector3f at(float fu, float fv, float fn) { return new Vector3f(o).add(new Vector3f(u).mul(fu)).add(new Vector3f(v).mul(fv)).add(new Vector3f(n).mul(fn)); }
    }

    // ---- the extras ----

    /** Draws the extras of a slot map on a flag (record: "held" or "planted", for the probe). */
    static void extras(MatrixStack m, VertexConsumerProvider vcp, int light, Frame f, Map<String, String> slots, String record) {
        if (slots == null || slots.isEmpty()) return;
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
        Matrix4f pm = m.peek().getPositionMatrix();
        float cloth = Math.max(.006f, f.half() * .6f), rail = Math.max(.02f, f.half() * 2.2f);
        for (String slot : Cosmetics.SLOTS) {
            String id = slots.get(slot);
            if (id == null) continue;
            boolean any = true;
            switch (id) {
                case "pennant" -> { // a swallow-tail streamer flying from the pole above the flag, crimson
                    box(vc, pm, f, 0.02f, 0.80f, -0.17f, -0.07f, -cloth, cloth, CRIMSON, light);
                    box(vc, pm, f, 0.80f, 1.00f, -0.17f, -0.135f, -cloth, cloth, CRIMSON, light);
                    box(vc, pm, f, 0.80f, 1.00f, -0.105f, -0.07f, -cloth, cloth, CRIMSON, light);
                }
                case "pennant_pattern" -> { // a longer two-tail streamer in gold with a crimson stripe
                    box(vc, pm, f, 0.02f, 0.78f, -0.19f, -0.06f, -cloth, cloth, GOLD, light);
                    box(vc, pm, f, 0.02f, 0.78f, -0.14f, -0.11f, -cloth * 1.4f, cloth * 1.4f, CRIMSON, light);
                    box(vc, pm, f, 0.78f, 1.08f, -0.19f, -0.145f, -cloth, cloth, GOLD, light);
                    box(vc, pm, f, 0.78f, 1.08f, -0.105f, -0.06f, -cloth, cloth, GOLD, light);
                }
                case "trim" -> { // a gold fringe along the flag's foot: a band and eight tassels
                    box(vc, pm, f, 0f, 1f, 0.98f, 1.03f, -cloth * 1.5f, cloth * 1.5f, GOLD, light);
                    for (int k = 0; k < 8; k++) box(vc, pm, f, k / 8f + .02f, k / 8f + .085f, 1.03f, 1.11f, -cloth, cloth, GOLD, light);
                }
                case "frame", "banner_frame", "frame_brass" -> { // rails on the flag's two sides
                    int cell = id.equals("frame") ? IRON : id.equals("banner_frame") ? GILT : BRASS;
                    box(vc, pm, f, -0.06f, 0f, -0.02f, 1.02f, -rail, rail, cell, light);
                    box(vc, pm, f, 1f, 1.06f, -0.02f, 1.02f, -rail, rail, cell, light);
                    if (id.equals("banner_frame")) for (float fv : new float[]{-0.04f, 1.0f}) for (float fu : new float[]{-0.08f, 0.98f})
                        box(vc, pm, f, fu, fu + .1f, fv, fv + .06f, -rail * 1.4f, rail * 1.4f, GILT, light); // gilded knobs at the corners
                }
                case "standard" -> { // a gilded crossbar over the flag's head and a finial on the pole's top
                    box(vc, pm, f, -0.08f, 1.08f, -0.05f, 0.0f, -rail, rail, DARK_GOLD, light);
                    box(vc, pm, f, f.poleU() - .07f, f.poleU() + .07f, f.poleTopV() - .13f, f.poleTopV(), -rail * 1.5f, rail * 1.5f, GILT, light);
                    box(vc, pm, f, f.poleU() - .03f, f.poleU() + .03f, f.poleTopV() - .20f, f.poleTopV() - .13f, -rail * .8f, rail * .8f, GILT, light);
                }
                default -> any = false; // skin_proven draws on the troops (GeoTroopRenderer), not the banner
            }
            if (any) drawing.add(record + ":" + id);
        }
    }

    /** A box between flag coordinates (u0..u1, v0..v1, n0..n1 in blocks along the normal), every face in one swatch, drawn without culling. */
    static void box(VertexConsumer vc, Matrix4f pm, Frame f, float u0, float u1, float v0, float v1, float n0, float n1, int cell, int light) {
        Vector3f[] c = new Vector3f[8];
        for (int i = 0; i < 8; i++) c[i] = f.at((i & 1) == 0 ? u0 : u1, (i & 2) == 0 ? v0 : v1, (i & 4) == 0 ? n0 : n1);
        float s0 = (cell % 4) * 4 / 16f + .01f, s1 = s0 + 4 / 16f - .02f, t0 = (cell / 4) * 4 / 16f + .01f, t1 = t0 + 4 / 16f - .02f;
        int[][] faces = {{4, 5, 7, 6}, {1, 0, 2, 3}, {0, 1, 5, 4}, {6, 7, 3, 2}, {0, 4, 6, 2}, {5, 1, 3, 7}}; // front, back, top, bottom, pole side, far side
        Vector3f[] normals = {new Vector3f(f.n()), new Vector3f(f.n()).negate(), new Vector3f(f.v()).normalize().negate(), new Vector3f(f.v()).normalize(), new Vector3f(f.u()).normalize().negate(), new Vector3f(f.u()).normalize()};
        float[][] uv = {{s0, t0}, {s1, t0}, {s1, t1}, {s0, t1}};
        for (int k = 0; k < 6; k++) {
            Vector3f nn = normals[k];
            for (int j = 0; j < 4; j++) {
                Vector3f p = pm.transformPosition(new Vector3f(c[faces[k][j]]));
                vc.vertex(p.x, p.y, p.z, 0xFFFFFFFF, uv[j][0], uv[j][1], OverlayTexture.DEFAULT_UV, light, nn.x, nn.y, nn.z);
            }
        }
    }

    // ---- the held banner (CommanderGearFeature) ----

    /** One weapon file's flag: its two faces (corners in pixels, with their sheet texels) and the pole's top; null when the file has none. */
    record Flag(float[][] front, float[][] frontUv, float[][] back, float[][] backUv, float[] poleTop) {}
    private static final Map<String, Optional<Flag>> FLAGS = new ConcurrentHashMap<>();
    /** Forgets the cached flags (a resource reload). */
    static void reload() { FLAGS.clear(); }

    /** The flag of a banner weapon at a rarity, read once from models/gear/weapon/<item>.json: the two largest quads whose sheet region is no more than four times as tall as wide. */
    static Flag flag(String item, int rarity) {
        return FLAGS.computeIfAbsent(item + "#" + rarity, k -> {
            try {
                var res = MinecraftClient.getInstance().getResourceManager().getResource(Identifier.of(TroopModels.NAMESPACE, "models/gear/weapon/" + item + ".json"));
                if (res.isEmpty()) return Optional.empty();
                JsonObject j;
                try (Reader r = res.get().getReader()) { j = JsonParser.parseReader(r).getAsJsonObject(); }
                var rs = j.getAsJsonObject("rarities");
                JsonObject row = rs.has(Integer.toString(rarity)) ? rs.getAsJsonObject(Integer.toString(rarity)) : rs.getAsJsonObject("1");
                return Optional.ofNullable(flagOf(row.getAsJsonArray("quads")));
            } catch (Exception e) {
                System.err.println("[Graves and Glory] cosmetic flag of " + item + " unreadable: " + e);
                return Optional.empty();
            }
        }).orElse(null);
    }

    static Flag flagOf(JsonArray quads) {
        List<float[]> flags = new ArrayList<>(), poles = new ArrayList<>();
        for (var el : quads) {
            JsonArray q = el.getAsJsonArray();
            if (q.size() != 20) continue;
            float[] v = new float[20];
            for (int i = 0; i < 20; i++) v[i] = q.get(i).getAsFloat();
            float uMin = Math.min(Math.min(v[12], v[14]), Math.min(v[16], v[18])), uMax = Math.max(Math.max(v[12], v[14]), Math.max(v[16], v[18]));
            float tMin = Math.min(Math.min(v[13], v[15]), Math.min(v[17], v[19])), tMax = Math.max(Math.max(v[13], v[15]), Math.max(v[17], v[19]));
            float w = uMax - uMin, h = tMax - tMin;
            if (w <= 0 || h <= 0) continue;
            (h / w <= 4 ? flags : poles).add(v);
        }
        flags.sort(Comparator.comparingDouble(v -> -area(v)));
        if (flags.size() < 2) return null;
        float[] a = flags.get(0), b = flags.get(1);
        float[] top = null;
        // the pole's top: the corner of the long quads furthest up the flag's v (up is -v)
        Vector3f[] fa = corners(a); float[][] fuv = uvs(a);
        Vector3f tl = null, bl = null;
        for (int k = 0; k < 4; k++) { if (fuv[k][0] == min(fuv, 0) && fuv[k][1] == min(fuv, 1)) tl = fa[k]; if (fuv[k][0] == min(fuv, 0) && fuv[k][1] == max(fuv, 1)) bl = fa[k]; }
        if (tl != null && bl != null) {
            Vector3f down = new Vector3f(bl).sub(tl);
            float best = Float.MAX_VALUE;
            for (float[] p : poles) for (Vector3f c : corners(p)) { float d = new Vector3f(c).sub(tl).dot(down); if (d < best) { best = d; top = new float[]{c.x, c.y, c.z}; } }
        }
        return new Flag(pts(a), uvs(a), pts(b), uvs(b), top);
    }
    static double area(float[] v) {
        Vector3f p0 = new Vector3f(v[0], v[1], v[2]), a = new Vector3f(v[3], v[4], v[5]).sub(p0), b = new Vector3f(v[9], v[10], v[11]).sub(p0);
        return a.cross(b).length();
    }
    static Vector3f[] corners(float[] v) { Vector3f[] c = new Vector3f[4]; for (int k = 0; k < 4; k++) c[k] = new Vector3f(v[3 * k], v[3 * k + 1], v[3 * k + 2]); return c; }
    static float[][] pts(float[] v) { float[][] c = new float[4][]; for (int k = 0; k < 4; k++) c[k] = new float[]{v[3 * k], v[3 * k + 1], v[3 * k + 2]}; return c; }
    static float[][] uvs(float[] v) { float[][] c = new float[4][]; for (int k = 0; k < 4; k++) c[k] = new float[]{v[12 + 2 * k], v[13 + 2 * k]}; return c; }
    static float min(float[][] uv, int a) { float m = Float.MAX_VALUE; for (float[] x : uv) m = Math.min(m, x[a]); return m; }
    static float max(float[][] uv, int a) { float m = -Float.MAX_VALUE; for (float[] x : uv) m = Math.max(m, x[a]); return m; }

    /** The frame of a flag, in blocks (the mesh's pixels / 16), on its mid-plane, the front face's top corner on the pole side as origin. */
    static Frame frame(Flag fl) {
        Vector3f tl = null, tr = null, bl = null;
        float[][] uv = fl.frontUv();
        for (int k = 0; k < 4; k++) {
            Vector3f p = new Vector3f(fl.front()[k][0], fl.front()[k][1], fl.front()[k][2]).div(16f);
            boolean left = uv[k][0] == min(uv, 0), up = uv[k][1] == min(uv, 1);
            if (left && up) tl = p; else if (!left && up) tr = p; else if (left) bl = p;
        }
        if (tl == null || tr == null || bl == null) return null;
        Vector3f cf = centre(fl.front()), cb = centre(fl.back());
        Vector3f through = new Vector3f(cf).sub(cb);
        float half = through.length() / 2f;
        Vector3f n = half > 1e-5f ? new Vector3f(through).normalize() : new Vector3f(tr).sub(tl).cross(new Vector3f(bl).sub(tl)).normalize();
        Vector3f o = new Vector3f(tl).sub(new Vector3f(n).mul(half));
        Vector3f u = new Vector3f(tr).sub(tl), v = new Vector3f(bl).sub(tl);
        float poleU = 0, poleTopV = -0.1f;
        if (fl.poleTop() != null) {
            Vector3f d = new Vector3f(fl.poleTop()[0], fl.poleTop()[1], fl.poleTop()[2]).div(16f).sub(o);
            poleU = d.dot(u) / u.lengthSquared(); poleTopV = d.dot(v) / v.lengthSquared();
        }
        // a pole on the far side of the flag: flip u so the extras' "pole side" is the pole's
        if (poleU > .5f) { o = new Vector3f(o).add(u); u = new Vector3f(u).negate(); poleU = 1 - poleU; }
        return new Frame(o, u, v, n, half, poleU, poleTopV);
    }
    static Vector3f centre(float[][] c) { Vector3f s = new Vector3f(); for (float[] p : c) s.add(p[0], p[1], p[2]); return s.div(4f * 16f); }

    /**
     * Inside CommanderGearFeature.weapon, in the mesh's own frame, right after a banner-family mesh drew: the stack's pattern layers on both
     * flag faces, then the wearer's extras.
     */
    static void held(MatrixStack m, VertexConsumerProvider vcp, int light, int overlay, Entity e, String item) {
        ItemStack s = CommanderGearFeature.stack(e, EquipmentSlot.MAINHAND);
        var art = GearLooks.art(s);
        if (art == null || !art.item().equals(item)) return;
        Flag fl = flag(item, art.rarity());
        if (fl == null) return;
        var layers = s.get(DataComponentTypes.BANNER_PATTERNS);
        if (layers != null && !layers.layers().isEmpty()) {
            int drawnLayers = 0;
            for (var layer : layers.layers()) {
                VertexConsumer vc = TexturedRenderLayers.getBannerPatternTextureId(layer.pattern()).getVertexConsumer(vcp, RenderLayer::getEntityNoOutline);
                int argb = 0xFF000000 | (layer.color().getEntityColor() & 0xFFFFFF);
                canvas(m, vc, fl.front(), fl.frontUv(), centre(fl.back()), argb, light, overlay);
                canvas(m, vc, fl.back(), fl.backUv(), centre(fl.front()), argb, light, overlay);
                drawnLayers++;
            }
            drawing.add("pattern:" + drawnLayers);
        }
        Frame f = frame(fl);
        if (f != null) extras(m, vcp, light, f, CosmeticSync.client(e).slots(), "held");
    }

    /** One face of the mesh's flag as a vanilla banner canvas (the pattern sprite's front, 20 by 40 of its 64 by 64), lifted off the cloth away from the other face. */
    static void canvas(MatrixStack m, VertexConsumer vc, float[][] pts, float[][] uv, Vector3f otherCentre, int argb, int light, int overlay) {
        float u0 = min(uv, 0), u1 = max(uv, 0), t0 = min(uv, 1), t1 = max(uv, 1);
        Vector3f c = centre(pts), out = new Vector3f(c).sub(otherCentre);
        if (out.lengthSquared() < 1e-10f) out.set(0, 0, 1);
        out.normalize();
        Matrix4f pm = m.peek().getPositionMatrix();
        Vector3f nn = m.peek().getNormalMatrix().transform(new Vector3f(out));
        for (int k = 0; k < 4; k++) {
            Vector3f p = new Vector3f(pts[k][0], pts[k][1], pts[k][2]).div(16f).add(new Vector3f(out).mul(.004f));
            p = pm.transformPosition(p);
            float su = (uv[k][0] - u0) / (u1 - u0), sv = (uv[k][1] - t0) / (t1 - t0);
            vc.vertex(p.x, p.y, p.z, argb, (1 + 20 * su) / 64f, (1 + 40 * sv) / 64f, overlay, light, nn.x, nn.y, nn.z);
        }
    }

    // ---- the planted banner and the title (WorldRenderEvents.AFTER_ENTITIES) ----

    /** The vanilla standing banner's flag (BannerBlockEntityRenderer: pivot at block centre, 2/3 scale, flag 20 by 40 pixels hung at y 32 above, 1 thick at z -2..-1), in the banner's turned block frame. */
    static final Frame STANDING = new Frame(new Vector3f(-10 / 24f, .5f + 32 / 24f, 1.5f / 24f), new Vector3f(20 / 24f, 0, 0), new Vector3f(0, -40 / 24f, 0), new Vector3f(0, 0, 1), .5f / 24f, .5f, -0.01f);

    static void world(WorldRenderContext ctx) {
        var mc = MinecraftClient.getInstance();
        if (mc.world == null || ctx.consumers() == null || ctx.matrixStack() == null) return;
        var cam = ctx.camera().getPos();
        MatrixStack m = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        float delta = ctx.tickCounter().getTickDelta(true);
        for (Entity e : mc.world.getEntities()) {
            if (e instanceof ArmorStandEntity) planted(mc, m, vcp, cam, e);
            else if (e instanceof AbstractClientPlayerEntity p) title(mc, m, vcp, cam, p, delta);
        }
    }

    /** A planted Forward Supply banner: the stand's set (cosmetic_v1) as extras on the standing banner block it sits on. */
    static void planted(MinecraftClient mc, MatrixStack m, VertexConsumerProvider vcp, net.minecraft.util.math.Vec3d cam, Entity e) {
        var entry = CosmeticSync.client(e);
        if (entry.slots().isEmpty()) return;
        var pos = e.getBlockPos();
        var state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof BannerBlock)) return;
        float yaw = -RotationPropertyHelper.toDegrees(state.get(BannerBlock.ROTATION));
        m.push();
        m.translate(pos.getX() + .5 - cam.x, pos.getY() - cam.y, pos.getZ() + .5 - cam.z);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        extras(m, vcp, WorldRenderer.getLightmapCoordinates(mc.world, pos), STANDING, entry.slots(), "planted");
        m.pop();
    }

    /** The title line under a player's name tag: not with the HUD hidden (F1, as name tags), never on yourself in first person, and only within name tag range. */
    static void title(MinecraftClient mc, MatrixStack m, VertexConsumerProvider vcp, net.minecraft.util.math.Vec3d cam, AbstractClientPlayerEntity p, float delta) {
        String title = CosmeticSync.client(p).title();
        if (title.isEmpty() || !MinecraftClient.isHudEnabled() || p.isInvisible()) return;
        if (p == mc.player && mc.options.getPerspective().isFirstPerson()) return;
        var at = p.getLerpedPos(delta);
        if (at.squaredDistanceTo(cam) > 64 * 64) return;
        TextRenderer font = mc.textRenderer;
        m.push();
        m.translate(at.x - cam.x, at.y + p.getHeight() + .26 - cam.y, at.z - cam.z);
        m.multiply(mc.gameRenderer.getCamera().getRotation());
        m.scale(.02f, -.02f, .02f);
        Matrix4f pm = m.peek().getPositionMatrix();
        float x = -font.getWidth(title) / 2f;
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        font.draw(title, x, 0, 0x40FFFFFF & TITLE_INK | 0x40000000, false, pm, vcp, TextRenderer.TextLayerType.SEE_THROUGH, 0x30000000, light);
        font.draw(title, x, 0, TITLE_INK, false, pm, vcp, TextRenderer.TextLayerType.NORMAL, 0, light);
        m.pop();
        drawing.add("title:" + title);
    }
}
