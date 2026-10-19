package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A button laid out and painted in canvas pixels. Hover and clicks test the canvas box itself, so the hit box is the art
 *  at any window size and GUI Scale; its GUI bounds (UiCanvas.guiBox, rounded outward) serve focus and narration, and
 *  screens call relayout in init. The label is for narration; the painter draws. */
public class UiButton extends ButtonWidget {
    /** Paints the button in canvas pixels (called between UiCanvas.begin and end); hot means hovered or focused. */
    public interface Painter { void paint(DrawContext g, int x, int y, int w, int h, boolean hot); }

    int cx, cy, cw, ch;
    final Painter painter;

    public UiButton(int x, int y, int w, int h, Text label, PressAction onPress, UiButton.Painter painter) {
        super(0, 0, 0, 0, label, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.cx = x; this.cy = y; this.cw = w; this.ch = h; this.painter = painter;
        relayout();
    }

    /** {x, y, w, h} in canvas pixels. */
    public int[] canvasBox() { return new int[]{cx, cy, cw, ch}; }

    /** Moves the canvas box (a War Table order sheet grows when it is raised) and recomputes the GUI bounds. */
    public void moveTo(int x, int y, int w, int h) {
        cx = x; cy = y; cw = w; ch = h;
        relayout();
    }

    /** Recomputes the GUI bounds from the canvas box at the current window size and GUI scale. */
    public void relayout() {
        int[] b = UiCanvas.guiBox(cx, cy, cw, ch);
        setX(b[0]); setY(b[1]); width = b[2]; height = b[3];
    }

    /** Whether a GUI point falls inside the canvas box, the art's own bounds. */
    public boolean overArt(double guiX, double guiY) {
        double x = UiCanvas.toCanvas(guiX), y = UiCanvas.toCanvas(guiY);
        return x >= cx && y >= cy && x < cx + cw && y < cy + ch;
    }

    @Override public boolean isMouseOver(double mouseX, double mouseY) { return active && visible && overArt(mouseX, mouseY); }

    @Override protected boolean clicked(double mouseX, double mouseY) { return active && visible && overArt(mouseX, mouseY); }

    @Override protected void renderButton(DrawContext g, int mouseX, int mouseY, float delta) {
        // render rounded the mouse to whole GUI pixels and tested the GUI box; test the art from the exact position
        var c = MinecraftClient.getInstance(); var w = c.getWindow();
        hovered = overArt(c.mouse.getX() * w.getScaledWidth() / w.getWidth(), c.mouse.getY() * w.getScaledHeight() / w.getHeight());
        UiCanvas.begin(g);
        try { painter.paint(g, cx, cy, cw, ch, isHovered() || isFocused()); }
        finally { UiCanvas.end(g); }
    }
}
