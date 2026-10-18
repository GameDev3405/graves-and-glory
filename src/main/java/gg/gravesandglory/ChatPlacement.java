package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.Map;

/** Vanilla chat above the battle HUD's left column (Kort's answer on chat placement: above the group cards, small but readable).
 *  While the battle HUD is drawn, the chat's bottom sits GAP GUI px above the column's top: the iron spine above the top card, or
 *  the hint slip while it shows. The chat keeps vanilla's scale, width and line count; only where the lifted column would reach the
 *  score bar, a boss, a seal, the aimed point's name or the lesson card above it does it show fewer lines, never smaller text. In
 *  camp, and whenever the battle HUD is not drawn, nothing moves. The battle HUD draws after the chat in a frame, so the chat follows
 *  the layout of the frame before. ChatHudMixin applies it. */
public final class ChatPlacement {
    private ChatPlacement() {}

    /** The one switch: false puts the chat back at vanilla's place and line count. */
    public static final boolean LIFT_CHAT = true;
    /** GUI px kept clear between the chat and the battle HUD, below the chat and above it. */
    static final int GAP = 4;
    /** Vanilla's chat bottom in GUI px above the window's bottom (ChatHud.render's height - 40). */
    static final int VANILLA_BOTTOM = 40;

    /** For checks: the chat's line backgrounds as its last frame drew them, a box {x,y,w,h} in GUI px (null when it drew none), and
     *  how many lines that frame drew. */
    public static int[] drawn;
    public static int drawnLines;
    private static int x0, y0, x1, y1, lines;

    /** The top of the battle HUD's left column (the spine above the top card, or the hint slip) in canvas px; -1 when the last
     *  frame drew no column (camp, an entrance, a screen over the HUD). */
    static int columnTop(Map<String, int[]> r) {
        int top = -1;
        for (var e : r.entrySet()) {
            String k = e.getKey();
            if (k.equals("spine") || k.equals("hints") || k.startsWith("card-")) top = top < 0 ? e.getValue()[1] : Math.min(top, e.getValue()[1]);
        }
        return top;
    }

    /** GUI px per canvas px. */
    static double gui() { return UiCanvas.k() / MinecraftClient.getInstance().getWindow().getScaleFactor(); }

    /** How far up the chat moves, in GUI px: from vanilla's bottom to GAP above the column's top; 0 without a column. */
    public static int lift(int scaledHeight) {
        if (!LIFT_CHAT) return 0;
        int top = columnTop(BattleHud.rects());
        if (top < 0) return 0;
        int bottom = (int) Math.floor(top * gui()) - GAP;
        return Math.max(0, scaledHeight - VANILLA_BOTTOM - bottom);
    }

    /** The lines the lifted chat shows: vanilla's, or as many as fit between its bottom and the lowest of the score bar, the
     *  bosses, the seals, the aimed point's name and the lesson card over the chat's width, GAP clear; at least one. */
    public static int lines(int vanilla, int scaledHeight, double scale, int lineHeight, int width) {
        int lift = lift(scaledHeight);
        if (lift <= 0) return vanilla;
        double f = gui(), right = (width + 12) * scale; // the line backgrounds run from GUI x 0 to (width + 12) x scale
        int above = 0;
        for (var e : BattleHud.rects().entrySet()) {
            String k = e.getKey();
            int[] a = e.getValue();
            boolean over = k.equals("score") || k.endsWith("-boss") || k.startsWith("seal-") || k.equals("aim-name") || k.equals("lesson");
            if (over && a[0] * f < right) above = Math.max(above, (int) Math.ceil((a[1] + a[3]) * f));
        }
        int bottom = scaledHeight - VANILLA_BOTTOM - lift, fit = (int) Math.floor((bottom - above - GAP) / (lineHeight * scale));
        return Math.max(1, Math.min(vanilla, fit));
    }

    /** ChatHud.render starts a frame (ChatHudMixin calls this and the two below). */
    public static void begin() { lines = 0; }

    /** A line's background, fill(ax, ay, bx, by) in the chat's own space (scaled by scale, then moved 4 px right). */
    public static void line(int ax, int ay, int bx, int by, double scale) {
        int gx0 = (int) Math.floor((ax + 4) * scale), gy0 = (int) Math.floor(ay * scale), gx1 = (int) Math.ceil((bx + 4) * scale), gy1 = (int) Math.ceil(by * scale);
        if (lines++ == 0) { x0 = gx0; y0 = gy0; x1 = gx1; y1 = gy1; }
        else { x0 = Math.min(x0, gx0); y0 = Math.min(y0, gy0); x1 = Math.max(x1, gx1); y1 = Math.max(y1, gy1); }
    }

    /** ChatHud.render ends a frame. */
    public static void end() { drawnLines = lines; drawn = lines == 0 ? null : new int[]{x0, y0, x1 - x0, y1 - y0}; }
}
