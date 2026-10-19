package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

/** The design canvas our screens and the HUD are laid out on (UI spec section 3, Canvas): the framebuffer divided by
 *  k, where k = UiMath.pixelScale is the whole number of screen pixels per art pixel (2 at 1080p, 1 at 1366x768),
 *  never more than the player's GUI Scale. Between begin and end, coordinates are canvas pixels. */
public final class UiCanvas {
    private UiCanvas() {}

    static Window window() { return MinecraftClient.getInstance().getWindow(); }

    /** Screen pixels per canvas pixel, from the effective GUI scale, so GUI Scale Auto (stored as 0) works. */
    public static int k() {
        Window w = window();
        return UiMath.pixelScale(w.getFramebufferWidth(), w.getFramebufferHeight(), (int) w.getScaleFactor());
    }

    public static int width() { return window().getFramebufferWidth() / k(); }

    public static int height() { return window().getFramebufferHeight() / k(); }

    /** Draw in canvas pixels until end: pushes a matrix scale of k / GUI scale. */
    public static void begin(DrawContext g) {
        float s = (float) (k() / window().getScaleFactor());
        g.getMatrices().push();
        g.getMatrices().scale(s, s, 1);
    }

    public static void end(DrawContext g) { g.getMatrices().pop(); }

    /** A GUI coordinate (a mouse position, say) in canvas pixels. */
    public static double toCanvas(double guiCoord) { return guiCoord * window().getScaleFactor() / k(); }

    /** The GUI box {x, y, w, h} covering a canvas box, rounded outward, for widget bounds and hit tests. */
    public static int[] guiBox(int x, int y, int w, int h) {
        double f = k() / window().getScaleFactor();
        int x0 = (int) Math.floor(x * f + 1e-9), y0 = (int) Math.floor(y * f + 1e-9);
        int x1 = (int) Math.ceil((x + w) * f - 1e-9), y1 = (int) Math.ceil((y + h) * f - 1e-9);
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /** Clips drawing to a canvas box. DrawContext.enableScissor ignores the matrix stack, so this flushes what is
     *  queued and sets the GL scissor in framebuffer pixels itself. Pair with endScissor. */
    public static void scissor(DrawContext g, int x, int y, int w, int h) {
        g.draw();
        int k = k();
        RenderSystem.enableScissor(x * k, window().getFramebufferHeight() - (y + h) * k, Math.max(0, w) * k, Math.max(0, h) * k);
    }

    public static void endScissor(DrawContext g) {
        g.draw();
        RenderSystem.disableScissor();
    }
}
