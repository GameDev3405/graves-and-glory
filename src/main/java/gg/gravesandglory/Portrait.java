package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;

/** A live model in a square window, posed as the unit identity spec poses portraits: turned 30 degrees off facing the viewer,
 *  chest up, 2 canvas px per model pixel in a 40 px window (UiMath.portraitSize and portraitFeet), the camera a little below
 *  eye level. Canvas pixels: call between UiCanvas.begin and end. */
final class Portrait {
    private Portrait() {}

    /** The body and head yaw of a portrait: 180 faces the viewer, so the model turns 30 degrees to its left. */
    static final float YAW = 210f;
    /** The camera's tilt below eye level, in degrees, built as vanilla's mouse overload builds its X tilt. */
    static final float TILT = 5f;

    /** What the last draw passed to InventoryScreen.drawEntity: the window's centre x, the feet y and the model size, in canvas
     *  px, with the window's size and the extra model pixels it was asked for. Null before the first draw. */
    record Drawn(int x, int feet, int size, int window, int tall) {}
    private static Drawn last;
    static Drawn last() { return last; }

    /** Draws e in the window (x, y, window x window), scissored to it; tall is the extra model pixels of a tall unit (0 for a
     *  player). The entity's yaws and pitch are put back as they were. Other players' name labels stay off, as the HUD does. */
    static void draw(DrawContext g, LivingEntity e, int x, int y, int window, int tall) {
        var c = MinecraftClient.getInstance();
        float bodyYaw = e.bodyYaw, yaw = e.getYaw(), pitch = e.getPitch(), prevHead = e.prevHeadYaw, head = e.headYaw;
        boolean hudHidden = c.options.hudHidden;
        Quaternionf camera = new Quaternionf().rotateX(TILT * (float) Math.PI / 180f);
        Quaternionf body = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
        UiCanvas.scissor(g, x, y, window, window);
        try {
            e.bodyYaw = YAW; e.setYaw(YAW); e.setPitch(0); e.headYaw = YAW; e.prevHeadYaw = YAW;
            c.options.hudHidden = true; // LivingEntityRenderer.hasLabel asks MinecraftClient.isHudEnabled()
            int cx = x + window / 2, feet = y + UiMath.portraitFeet(window, tall), size = UiMath.portraitSize(window, tall);
            last = new Drawn(cx, feet, size, window, tall);
            InventoryScreen.drawEntity(g, cx, feet, size, body, camera, e);
        } finally {
            c.options.hudHidden = hudHidden;
            e.bodyYaw = bodyYaw; e.setYaw(yaw); e.setPitch(pitch); e.prevHeadYaw = prevHead; e.headYaw = head;
            UiCanvas.endScissor(g);
        }
    }
}
