package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the supply wagon from the model tools/build_wagon.py writes (models/entity/supply_wagon.json: parts, pivots, box-UV
 * cubes) on its 1-texel-per-pixel texture. The wheel_* parts turn with WagonEntity's eased wheel angle.
 */
public final class WagonRenderer extends EntityRenderer<WagonEntity> {
    static final Identifier TEXTURE = Identifier.of("gravesandglory", "textures/entity/supply_wagon.png");
    static final String MODEL = "/assets/gravesandglory/models/entity/supply_wagon.json";

    final ModelPart root;
    final List<ModelPart> wheels = new ArrayList<>();

    WagonRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        shadowRadius = 1.4f;
        root = build();
        for (String w : new String[]{"wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr"}) wheels.add(root.getChild(w));
    }

    static void register() {
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(WagonEntity.TYPE, WagonRenderer::new);
    }

    static ModelPart build() {
        JsonObject json;
        try (var in = WagonRenderer.class.getResourceAsStream(MODEL)) {
            if (in == null) throw new IllegalStateException("missing " + MODEL);
            json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {throw new IllegalStateException(e);}
        ModelData data = new ModelData();
        Map<String, ModelPartData> made = new HashMap<>();
        made.put("root", data.getRoot());
        for (var el : json.getAsJsonArray("parts")) {
            JsonObject p = el.getAsJsonObject();
            ModelPartBuilder b = ModelPartBuilder.create();
            for (var ce : p.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                JsonArray uv = c.getAsJsonArray("uv"), f = c.getAsJsonArray("from"), s = c.getAsJsonArray("size");
                b.uv(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                    .cuboid(f.get(0).getAsFloat(), f.get(1).getAsFloat(), f.get(2).getAsFloat(), s.get(0).getAsFloat(), s.get(1).getAsFloat(), s.get(2).getAsFloat());
            }
            JsonArray pv = p.getAsJsonArray("pivot"), r = p.getAsJsonArray("rotation");
            ModelTransform t = ModelTransform.of(pv.get(0).getAsFloat(), pv.get(1).getAsFloat(), pv.get(2).getAsFloat(),
                r.get(0).getAsFloat() * MathHelper.RADIANS_PER_DEGREE, r.get(1).getAsFloat() * MathHelper.RADIANS_PER_DEGREE, r.get(2).getAsFloat() * MathHelper.RADIANS_PER_DEGREE);
            made.put(p.get("name").getAsString(), made.get(p.get("parent").getAsString()).addChild(p.get("name").getAsString(), b, t));
        }
        JsonArray size = json.getAsJsonArray("texture");
        return TexturedModelData.of(data, size.get(0).getAsInt(), size.get(1).getAsInt()).createModel();
    }

    @Override public void render(WagonEntity wagon, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider buffers, int light) {
        float turn = MathHelper.lerp(tickDelta, wagon.prevWheel, wagon.wheel);
        // Rolling toward -z (forward) turns a wheel's top forward: negative pitch about x in model space (y down).
        for (ModelPart w : wheels) w.pitch = -turn;
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - MathHelper.lerpAngleDegrees(tickDelta, wagon.prevYaw, wagon.getYaw())));
        matrices.scale(-1, -1, 1);
        matrices.translate(0, -1.501, 0);
        root.render(matrices, buffers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
        super.render(wagon, yaw, tickDelta, matrices, buffers, light);
    }

    @Override public Identifier getTexture(WagonEntity wagon) {return TEXTURE;}
}
