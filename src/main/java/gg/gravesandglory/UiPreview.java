package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;

import java.util.List;
import java.util.Locale;

/**
 * The UI editor's window-size preview (UI editor design 5.6, phase 2): a mod screen in edit mode laid out as it would be in another
 * window size and GUI scale, without touching the real window.
 *
 * <p>How: while the previewed screen is laid out, drawn, ticked or handed a mouse event (a "scope": {@link #enter}/{@link #exit}),
 * the window answers with the preview's framebuffer size, GUI scale and scaled size (UiPreviewWindowMixin), so the screen's own code
 * (vanilla's width and height, UiCanvas's framebuffer-based canvas, CampScreen's own scale) lays out exactly as in that window. Its
 * drawing is then stretched onto the real window by one matrix scale ({@link #sx}, {@link #sy}), scissor rectangles are rescaled to the
 * real framebuffer (UiPreviewScissorMixin), and the mouse lands where it points because vanilla maps it with the (preview) scaled
 * size over the real window size. Outside a scope (the world, the vanilla HUD, the final blit, shaders) every value is the real one.
 * Only screens the editor outlines are previewed, and only while the editor is on; the editor's own screens stay at the real size.
 */
public final class UiPreview {
    private UiPreview() {}

    public static final List<UiSizes.Res> RESOLUTIONS = UiSizes.RESOLUTIONS;
    /** The GUI scales offered: -1 the player's own setting, 0 Auto, then fixed scales. */
    public static final int[] SCALES = {-1, 0, 1, 2, 3, 4, 5, 6};

    /** The chosen resolution (-1: the real window) and GUI scale (an entry of SCALES). */
    static int res = -1, gui = -1;

    /** The size in force while previewing (null: no preview). */
    static UiSizes.Size size;

    /** Scope depth, and the real window's values captured when the outermost scope opened. */
    static int depth;
    static int realFbW, realFbH;
    static double realScale;

    // ---- the size ----

    public static boolean on() { return size != null; }
    public static UiSizes.Size size() { return size; }

    static boolean unicode() {
        MinecraftClient c = MinecraftClient.getInstance();
        return c != null && c.options != null && c.options.getForceUnicodeFont().getValue();
    }

    /** The size the chosen resolution and scale give, or null when both are the real ones. */
    static UiSizes.Size chosen() {
        if (res < 0 && gui < 0) return null;
        MinecraftClient c = MinecraftClient.getInstance();
        Window win = c.getWindow();
        int fbW = res < 0 ? win.getFramebufferWidth() : RESOLUTIONS.get(res).w(), fbH = res < 0 ? win.getFramebufferHeight() : RESOLUTIONS.get(res).h();
        int setting = gui < 0 ? c.options.getGuiScale().getValue() : gui;
        return UiSizes.sizeFor(fbW, fbH, setting, res < 0 ? "this window" : RESOLUTIONS.get(res).name(), unicode());
    }

    public static String resLabel() { return res < 0 ? "Size: actual" : "Size: " + RESOLUTIONS.get(res).name(); }
    public static String guiLabel() { return gui < 0 ? "GUI: yours" : gui == 0 ? "GUI: auto" : "GUI: " + gui; }

    /** The strip's Size button: the next resolution (after 4K, the real window again). */
    public static void nextRes() { res = res + 1 >= RESOLUTIONS.size() ? -1 : res + 1; apply(); }
    /** The strip's GUI button: the next scale, skipping fixed scales the chosen resolution cannot fit. */
    public static void nextGui() {
        int at = 0;
        for (int i = 0; i < SCALES.length; i++) if (SCALES[i] == gui) at = i;
        for (int step = 1; step <= SCALES.length; step++) {
            int next = SCALES[(at + step) % SCALES.length];
            if (next <= 0 || fits(next)) { gui = next; break; }
        }
        apply();
    }

    /** Whether a fixed GUI scale is a scale the chosen resolution gives (vanilla clamps larger ones). */
    static boolean fits(int scale) {
        Window win = MinecraftClient.getInstance().getWindow();
        int fbW = res < 0 ? win.getFramebufferWidth() : RESOLUTIONS.get(res).w(), fbH = res < 0 ? win.getFramebufferHeight() : RESOLUTIONS.get(res).h();
        return UiSizes.fits(fbW, fbH, scale);
    }

    /** Back to the real window (the editor closed). */
    public static void reset() { res = -1; gui = -1; force(null); }

    /** Takes up the chosen size and lays the shown screen out again in it. */
    public static void apply() { force(chosen()); }

    /** Previews size (null: none) and lays the shown screen out again. Used by the size check too. */
    static void force(UiSizes.Size s) {
        size = s;
        relayout(MinecraftClient.getInstance().currentScreen);
    }

    /** Lays a screen out again at the size in force: the preview's when it is previewed, else the real one. */
    static void relayout(Screen screen) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (screen == null || c.currentScreen != screen) return;
        if (previewing(screen)) {
            boolean in = enter();
            try { screen.resize(c, size.w(), size.h()); } finally { if (in) exit(); }
        } else if (UiEditorClient.editable(screen)) {
            Window win = c.getWindow();
            if (screen.width != win.getScaledWidth() || screen.height != win.getScaledHeight()) screen.resize(c, win.getScaledWidth(), win.getScaledHeight());
        }
    }

    /** Whether this screen is drawn in the preview: the editor is on, a size is chosen, and it is a screen the editor outlines. */
    public static boolean previewing(Screen s) { return size != null && UiEditorClient.active() && UiEditorClient.editable(s); }

    /** Whether the shown screen is previewed. */
    public static boolean previewingShown() { return size != null && previewing(MinecraftClient.getInstance().currentScreen); }

    // ---- the scope ----

    /** Opens a scope when a size is previewed (the caller must call exit when this returns true). The real values are read first. */
    public static boolean enter() {
        if (size == null) return false;
        if (depth == 0) {
            Window win = MinecraftClient.getInstance().getWindow();
            realFbW = win.getFramebufferWidth(); realFbH = win.getFramebufferHeight(); realScale = win.getScaleFactor();
        }
        depth++;
        return true;
    }

    public static void exit() { if (depth > 0) depth--; }

    /** Closes every open scope for a while (a screen opened from inside one is laid out at the real size); returns the depth to resume. */
    public static int suspend() { int d = depth; depth = 0; return d; }
    public static void resume(int d) { depth = d; }

    /** A frame starts outside any scope (a scope an exception left open is closed). */
    static void frameStart() { depth = 0; }

    /** Whether the window should answer with the preview's values now. */
    public static boolean scoped() { return depth > 0 && size != null; }

    public static int fbW() { return size.fbW(); }
    public static int fbH() { return size.fbH(); }
    public static double scale() { return size.scale(); }
    public static int w() { return size.w(); }
    public static int h() { return size.h(); }

    /** The stretch from the preview's GUI space onto the real one: real GUI pixels per preview GUI pixel. Valid in a scope. */
    public static float sx() { return (float) ((realFbW / realScale) / ((double) size.fbW() / size.scale())); }
    public static float sy() { return (float) ((realFbH / realScale) / ((double) size.fbH() / size.scale())); }

    /** Whether the preview's shape differs from the window's (it is then stretched to fit). */
    public static boolean stretched() {
        if (size == null) return false;
        Window win = MinecraftClient.getInstance().getWindow();
        double a = (double) size.fbW() / size.fbH(), b = (double) win.getFramebufferWidth() / Math.max(1, win.getFramebufferHeight());
        return Math.abs(a / b - 1) > 0.03;
    }

    /** A scissor box in the preview's framebuffer pixels, moved to the real framebuffer's (DrawContext.setScissor, in a scope). */
    public static void scissor(int x, int y, int w, int h) {
        if (!scoped()) { RenderSystem.enableScissor(x, y, w, h); return; }
        double kx = (double) realFbW / size.fbW(), ky = (double) realFbH / size.fbH();
        int x0 = (int) Math.floor(x * kx), y0 = (int) Math.floor(y * ky), x1 = (int) Math.ceil((x + w) * kx), y1 = (int) Math.ceil((y + h) * ky);
        RenderSystem.enableScissor(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    // ---- the sizes a check walks through ----

    /** The sizes a size check walks through. */
    public static List<UiSizes.Size> checkSizes() { return UiSizes.checkSizes(unicode()); }

    static String describe(UiSizes.Size s) { return s == null ? "actual" : String.format(Locale.ROOT, "%s (%dx%d GUI)", s.label(), s.w(), s.h()); }
}
