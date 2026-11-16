package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The altar and its coin as the client draws them (design 2026-11-14, section 4). The art is made with the prop workflow
 * (art/props/drop_altar, drop_coin) and copied into the mod's own assets by tools/dropboard_assets.py: the prop resource pack only
 * reaches the builder server's clients, and a camp has to show its altar to everyone. The parts are extra models (no item, no
 * registry entry), drawn in the prop frame: model px (0,0,0) is the minimum corner of the anchor cell, front north, turned by facing.
 * The server only keeps the altar's space solid (DropStage).
 */
final class DropAltar {
    private DropAltar() {}
    record Part(Identifier model, boolean glow, double x, double y, double z) {}
    static final Identifier COIN = Identifier.of("gravesandglory", "dropboard/drop_coin");
    /** How far off (blocks) the altar is still drawn. */
    static final double RANGE = 160;
    private static List<Part> parts;
    /** Altars drawn, for the suites. */
    static volatile int drawn;
    private static final ItemStack STACK = new ItemStack(Items.PAPER);

    static void register() {
        ModelLoadingPlugin.register(ctx -> {
            ctx.addModels(COIN);
            // the altar's regions: 4 across, 5 up, plain (0-19) and glowing (20-39); the ones with no art are simply missing
            for (int n = 0; n < 40; n++) if (DropAltar.class.getResource("/assets/gravesandglory/models/dropboard/drop_altar_" + n + ".json") != null) ctx.addModels(Identifier.of("gravesandglory", "dropboard/drop_altar_" + n));
        });
    }

    private static List<Part> parts() {
        if (parts != null) return parts;
        var list = new ArrayList<Part>();
        try (var in = DropAltar.class.getResourceAsStream("/assets/gravesandglory/dropboard/altar.json")) {
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : o.getAsJsonArray("parts")) {
                var p = e.getAsJsonObject(); var off = p.getAsJsonArray("offset");
                list.add(new Part(Identifier.of("gravesandglory", "dropboard/" + p.get("model").getAsString()), p.get("glow").getAsBoolean(), off.get(0).getAsDouble() / 16, off.get(1).getAsDouble() / 16, off.get(2).getAsDouble() / 16));
            }
        } catch (Exception e) { System.out.println("[Graves and Glory] drop board: the altar's parts could not be read: " + e); }
        return parts = list;
    }

    /** The altar's anchor cell: the middle of its front row, on the plinth. */
    static BlockPos anchor() { return DropStage.at(0, DropStage.ALTAR_FRONT, 3); }
    static int facing() { return Math.max(0, PropGeometry.facingOf(Spec.DROPBOARD[0].facing())); }

    static BakedModel model(MinecraftClient c, Identifier id) { return ((net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager) c.getBakedModelManager()).getModel(id); }

    /** Draws a model whose centre is its cell's centre, at the matrix's origin. */
    static void draw(MinecraftClient c, MatrixStack m, VertexConsumerProvider consumers, Identifier id, int light) {
        BakedModel model = model(c, id);
        if (model == null) return;
        c.getItemRenderer().renderItem(STACK, ModelTransformationMode.NONE, false, m, consumers, light, OverlayTexture.DEFAULT_UV, model);
    }

    static void world(WorldRenderContext ctx) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null || !GravesAndGloryClient.inCamp()) return;
        BlockPos a = anchor();
        var cam = ctx.camera().getPos();
        if (cam.squaredDistanceTo(a.getX(), a.getY(), a.getZ()) > RANGE * RANGE) return;
        if (!c.world.getBlockState(DropStage.markerPos().up(3)).isOf(DropStage.FLOOR)) return; // no court here (another world's camp, an old save)
        MatrixStack m = ctx.matrixStack();
        int facing = facing();
        // the court's light at the altar's foot, in front of its face: the stone is lit as the court is
        int[] out = DropLayout.out();
        int light = WorldRenderer.getLightmapCoordinates(c.world, a.add(out[0], 1, out[1]));
        m.push();
        m.translate(a.getX() + 0.5 - cam.x, a.getY() + 0.5 - cam.y, a.getZ() + 0.5 - cam.z);
        m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing));
        for (Part p : parts()) {
            m.push();
            m.translate(p.x, p.y, p.z);
            draw(c, m, ctx.consumers(), p.model, p.glow ? LightmapTextureManager.MAX_LIGHT_COORDINATE : light);
            m.pop();
        }
        m.pop();
        drawn++;
    }
}
