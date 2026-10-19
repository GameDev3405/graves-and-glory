package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.gravesandglory.mixin.LivingEntityRendererAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

/** A live model in a square window, posed as the unit identity spec poses portraits (rule 8): turned 30 degrees off facing the
 *  viewer, chest up, 2 canvas px per model pixel in a 40 px window (UiMath.portraitSize and portraitFeet), the camera a little
 *  below eye level. Model pixels are the model's own: a renderer that scales its model about the feet (a player by 0.9375, a husk
 *  by 1.0625, a wither skeleton by 1.2) gets the size divided by that scale, so every head sits 4 model px under the window's
 *  top. A silhouette draws the model flat in DarkUi.SILHOUETTE, as the muster roll shows a troop not yet recruited. Canvas
 *  pixels: call between UiCanvas.begin and end. */
final class Portrait {
    private Portrait() {}

    /** The body and head yaw of a portrait: 180 faces the viewer, so the model turns 30 degrees to its left. */
    static final float YAW = 210f;
    /** The camera's tilt below eye level, in degrees, built as vanilla's mouse overload builds its X tilt. */
    static final float TILT = 5f;

    /** What the last draw passed to InventoryScreen.drawEntity: the window's centre x, the feet y and the model size, in canvas
     *  px, with the window's size, the extra model pixels it was asked for, the renderer's own scale the size was divided by and
     *  whether it drew a silhouette. Null before the first draw. */
    record Drawn(int x, int feet, int size, int window, int tall, float scale, boolean silhouette) {}
    private static Drawn last;
    static Drawn last() { return last; }

    /** Draws e in the window (x, y, window x window), scissored to it; tall is the extra model pixels of a tall unit (0 for a
     *  player). The entity's yaws and pitch are put back as they were. Other players' name labels stay off, as the HUD does. */
    static void draw(DrawContext g, LivingEntity e, int x, int y, int window, int tall) { draw(g, e, x, y, window, tall, false); }

    /** As draw(g, e, x, y, window, tall), flat in DarkUi.SILHOUETTE when silhouette is set: the model is drawn with every
     *  fragment fully fogged (fog from -2 to -1, so any distance is past its end) in that colour, keeping the model's own
     *  alpha, and the fog is put back as it was. */
    static void draw(DrawContext g, LivingEntity e, int x, int y, int window, int tall, boolean silhouette) {
        var c = MinecraftClient.getInstance();
        float bodyYaw = e.bodyYaw, yaw = e.getYaw(), pitch = e.getPitch(), prevHead = e.prevHeadYaw, head = e.headYaw;
        boolean hudHidden = c.options.hudHidden;
        float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        float[] fogColor = RenderSystem.getShaderFogColor().clone();
        Quaternionf camera = new Quaternionf().rotateX(TILT * (float) Math.PI / 180f);
        Quaternionf body = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
        UiCanvas.scissor(g, x, y, window, window);
        try {
            e.bodyYaw = YAW; e.setYaw(YAW); e.setPitch(0); e.headYaw = YAW; e.prevHeadYaw = YAW;
            c.options.hudHidden = true; // LivingEntityRenderer.hasLabel asks MinecraftClient.isHudEnabled()
            float scale = modelScale(e);
            int cx = x + window / 2, feet = y + UiMath.portraitFeet(window, tall), size = Math.round(UiMath.portraitSize(window, tall) / scale);
            if (silhouette) {
                int s = DarkUi.SILHOUETTE;
                RenderSystem.setShaderFogStart(-2f);
                RenderSystem.setShaderFogEnd(-1f);
                RenderSystem.setShaderFogColor(((s >> 16) & 255) / 255f, ((s >> 8) & 255) / 255f, (s & 255) / 255f, 1f);
            }
            last = new Drawn(cx, feet, size, window, tall, scale, silhouette);
            InventoryScreen.drawEntity(g, cx, feet, size, body, camera, e);
            // the model stands in front of the page (z 50 and up), and text drawn later tests depth: clear the window's depth
            // (the scissor bounds the clear) so a note laid over the portrait is not cut where the model was
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
        } finally {
            RenderSystem.setShaderFogStart(fogStart);
            RenderSystem.setShaderFogEnd(fogEnd);
            RenderSystem.setShaderFogColor(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);
            c.options.hudHidden = hudHidden;
            e.bodyYaw = bodyYaw; e.setYaw(yaw); e.setPitch(pitch); e.prevHeadYaw = prevHead; e.headYaw = head;
            UiCanvas.endScissor(g);
        }
    }

    /** The scale e's renderer gives its model about the feet (LivingEntityRenderer.scale), read from the renderer itself: 1 for a
     *  renderer that does not scale, or one that is not a LivingEntityRenderer. */
    static float modelScale(LivingEntity e) {
        var renderer = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(e);
        if (!(renderer instanceof LivingEntityRendererAccessor living)) return 1f;
        MatrixStack probe = new MatrixStack();
        living.gravesandglory$scale(e, probe, 1f);
        float s = probe.peek().getPositionMatrix().m11();
        return s > 0 ? s : 1f;
    }
}
