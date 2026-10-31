package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;

/** The design canvas our screens and the HUD are laid out on (UI spec section 3, Canvas): the framebuffer divided by
 *  k, where k = UiMath.pixelScale is the whole number of screen pixels per art pixel (2 at 1080p, 1 at 1366x768),
 *  never more than the player's GUI Scale. Between begin and end, coordinates are canvas pixels. The battle HUD has its own
 *  scale, hudK (UI Task 8a; the HUD size setting since UI Task 8c), with its own canvas (hudWidth, hudHeight, beginHud). */
public final class UiCanvas {
    private UiCanvas() {}

    static Window window() { return MinecraftClient.getInstance().getWindow(); }

    /** Screen pixels per canvas pixel, from the effective GUI scale, so GUI Scale Auto (stored as 0) works. On a War Table page that
     *  fills the window (CampScreen.fillsWindow: the lobby and the troop sheet) it is UiMath.pageScale, else UiMath.pixelScale. */
    public static int k() {
        Window w = window();
        int fw = w.getFramebufferWidth(), fh = w.getFramebufferHeight(), gui = (int) w.getScaleFactor();
        return fillsWindow() ? UiMath.pageScale(fw, fh, gui) : UiMath.pixelScale(fw, fh, gui);
    }

    /** True while the screen shown is a War Table page drawn at UiMath.pageScale. */
    public static boolean fillsWindow() {
        return MinecraftClient.getInstance().currentScreen instanceof CampScreen s && s.fillsWindow();
    }

    public static int width() { return window().getFramebufferWidth() / k(); }

    public static int height() { return window().getFramebufferHeight() / k(); }

    /** Draw in canvas pixels until end: pushes a matrix scale of k / GUI scale. */
    public static void begin(DrawContext g) { begin(g, k()); }

    private static void begin(DrawContext g, int k) {
        float s = (float) (k / window().getScaleFactor());
        g.getMatrices().push();
        g.getMatrices().scale(s, s, 1);
    }

    private static int hudW = -1, hudH = -1, hudGui = -1, hudSize = -1, hudScale = 1;

    /** The battle HUD's screen pixels per canvas pixel at the player's HUD size (HudSize; UiMath.hudScale: k at Compact, a step or
     *  two more at Standard and Large where the HUD fits), worked out again only when the window, the GUI Scale or the size
     *  changes. The screens keep k. */
    public static int hudK() {
        Window w = window();
        int fw = w.getFramebufferWidth(), fh = w.getFramebufferHeight(), gui = (int) w.getScaleFactor(), size = HudSize.get();
        if (fw != hudW || fh != hudH || gui != hudGui || size != hudSize) { hudScale = UiMath.hudScale(fw, fh, gui, size); hudW = fw; hudH = fh; hudGui = gui; hudSize = size; }
        return hudScale;
    }

    /** The battle HUD's canvas, the framebuffer over hudK. */
    public static int hudWidth() { return window().getFramebufferWidth() / hudK(); }

    public static int hudHeight() { return window().getFramebufferHeight() / hudK(); }

    /** Draw the battle HUD in its canvas pixels until end: a matrix scale of hudK / GUI scale. */
    public static void beginHud(DrawContext g) { begin(g, hudK()); }

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
