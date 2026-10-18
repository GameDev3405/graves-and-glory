package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** A button laid out and painted in canvas pixels. Its GUI bounds come from UiCanvas.guiBox, so it clicks where its art
 *  is at any window size and GUI Scale; screens call relayout in init. The label is for narration; the painter draws. */
public class UiButton extends ButtonWidget {
    /** Paints the button in canvas pixels (called between UiCanvas.begin and end); hot means hovered or focused. */
    public interface Painter { void paint(DrawContext g, int x, int y, int w, int h, boolean hot); }

    final int cx, cy, cw, ch;
    final Painter painter;

    public UiButton(int x, int y, int w, int h, Text label, PressAction onPress, UiButton.Painter painter) {
        super(0, 0, 0, 0, label, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.cx = x; this.cy = y; this.cw = w; this.ch = h; this.painter = painter;
        relayout();
    }

    /** {x, y, w, h} in canvas pixels. */
    public int[] canvasBox() { return new int[]{cx, cy, cw, ch}; }

    /** Recomputes the GUI bounds from the canvas box at the current window size and GUI scale. */
    public void relayout() {
        int[] b = UiCanvas.guiBox(cx, cy, cw, ch);
        setX(b[0]); setY(b[1]); width = b[2]; height = b[3];
    }

    @Override protected void renderButton(DrawContext g, int mouseX, int mouseY, float delta) {
        UiCanvas.begin(g);
        try { painter.paint(g, cx, cy, cw, ch, isHovered() || isFocused()); }
        finally { UiCanvas.end(g); }
    }
}
