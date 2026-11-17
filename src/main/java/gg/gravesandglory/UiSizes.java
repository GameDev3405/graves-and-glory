package gg.gravesandglory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** The window sizes and GUI scales the UI editor previews and checks (UiPreview, UiWarnings), and vanilla's rule for the GUI scale
 *  and scaled size a framebuffer gets. Pure: no Minecraft classes. */
public final class UiSizes {
    private UiSizes() {}

    /** A window size to preview: its name and framebuffer pixels. */
    public record Res(String name, int w, int h) {}
    /** The resolutions offered, after "actual" (the real window). */
    public static final List<Res> RESOLUTIONS = List.of(new Res("720p", 1280, 720), new Res("1080p", 1920, 1080), new Res("1440p", 2560, 1440), new Res("4K", 3840, 2160));

    /** One previewed layout: framebuffer pixels, the GUI scale factor that results, and the scaled (GUI) size. */
    public record Size(String label, int fbW, int fbH, int scale, int w, int h) {}

    /** The GUI scale factor vanilla picks for a framebuffer and a GUI Scale setting (0 Auto), as Window.calculateScaleFactor. */
    public static int scaleFor(int fbW, int fbH, int setting, boolean unicode) {
        int i = 1;
        while (i != setting && i < fbW && i < fbH && fbW / (i + 1) >= 320 && fbH / (i + 1) >= 240) i++;
        if (unicode && i % 2 != 0) i++;
        return i;
    }

    /** The scaled (GUI) length of a framebuffer length at a scale, rounded up as vanilla does. */
    public static int scaled(int fb, int scale) { int i = fb / scale; return fb % scale != 0 ? i + 1 : i; }

    public static Size sizeFor(int fbW, int fbH, int setting, String resName, boolean unicode) {
        int s = scaleFor(fbW, fbH, setting, unicode);
        String label = resName + (setting == 0 ? " auto" : "") + " x" + s;
        return new Size(label, fbW, fbH, s, scaled(fbW, s), scaled(fbH, s));
    }

    /** Whether a fixed GUI scale is one a framebuffer gives (vanilla lowers a scale the window is too small for). */
    public static boolean fits(int fbW, int fbH, int scale) { return scaleFor(fbW, fbH, scale, false) == scale; }

    /** Every resolution at Auto and at GUI 2, 3 and 4, without repeats and without scales a resolution lowers. */
    public static List<Size> checkSizes(boolean unicode) {
        LinkedHashMap<String, Size> out = new LinkedHashMap<>();
        for (Res r : RESOLUTIONS) {
            for (int g : new int[] {0, 2, 3, 4}) {
                if (g > 0 && !fits(r.w(), r.h(), g)) continue;
                Size s = sizeFor(r.w(), r.h(), g, r.name(), unicode);
                out.putIfAbsent(s.fbW() + "x" + s.fbH() + "@" + s.scale(), s);
            }
        }
        return new ArrayList<>(out.values());
    }
}
