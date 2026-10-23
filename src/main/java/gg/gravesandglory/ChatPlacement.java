package gg.gravesandglory;

import gg.gravesandglory.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;

/** Vanilla chat above the battle HUD's left column (Kort's answer on chat placement: above the group cards, small but readable).
 *  While the battle HUD is drawn, the chat's bottom sits GAP GUI px above the column's top: the iron spine above the top card, or
 *  the hint slip while it shows. The chat keeps vanilla's scale, width and line count. Where its lowest line would cover the
 *  score bar, a boss, a seal, the aimed point's name, the lesson or respawn card, the feed or, while the chat is closed, the
 *  crosshair, it moves up past them; where the room above it is short, it shows fewer lines, never smaller text (UiMath.chatSlot).
 *  Open, the player is typing: the chat may cross the crosshair then rather than drop to a line or two. In camp, and whenever the
 *  battle HUD is not drawn, nothing moves. The battle HUD draws after the chat in a frame, so the chat follows the layout of the
 *  frame before. ChatHudMixin applies it.
 *
 *  Accepted, not fixed (controller ruling, UI Task 8a): the crosshair band is vanilla's 15 px icon only. Vanilla's attack
 *  indicator below it (the Crosshair setting's 16x4 cooldown bar, or its 16x16 icon, from the screen's middle + 9) can meet the
 *  closed chat's top line. It shows only for a moment after a swing and sits on the chat's translucent background, and keeping
 *  clear of it would cost lines at the small sizes (1280x720 GUI 3: 4 to 2). */
public final class ChatPlacement {
    private ChatPlacement() {}

    /** The one switch: false puts the chat back at vanilla's place and line count. */
    public static final boolean LIFT_CHAT = true;
    /** GUI px kept clear between the chat and the battle HUD, below the chat and above it. */
    static final int GAP = UiMath.CHAT_GAP;
    /** Vanilla's chat bottom in GUI px above the window's bottom (ChatHud.render's height - 40). */
    static final int VANILLA_BOTTOM = 40;
    /** Vanilla's crosshair icon, a square this many GUI px wide at the screen's centre (InGameHud.renderCrosshair draws it at
     *  ((width - 15) / 2, (height - 15) / 2)). */
    static final int CROSSHAIR = UiMath.CROSSHAIR;

    /** For checks: the chat's line backgrounds as its last frame drew them, a box {x,y,w,h} in GUI px (null when it drew none), and
     *  how many lines that frame drew. */
    public static int[] drawn;
    public static int drawnLines;
    private static int x0, y0, x1, y1, lines;

    /** GUI px per canvas px: the battle HUD's scale (UiCanvas.hudK) over the GUI Scale. */
    static double gui() { return UiCanvas.hudK() / Math.max(1, MinecraftClient.getInstance().getWindow().getScaleFactor()); }

    /** The lifted chat over the battle HUD's last frame (UiMath.chatSlot) for a chat of this scale, line height, width and focus on
     *  a GUI scaledWidth x scaledHeight; null when the frame drew no column or LIFT_CHAT is off. */
    static UiMath.ChatSlot slot(int vanilla, int scaledWidth, int scaledHeight, double scale, int lineHeight, int width, boolean focused) {
        if (!LIFT_CHAT) return null;
        return UiMath.chatSlot(BattleHud.rects(), gui(), scaledWidth, scaledHeight, right(width, scale), lineHeight * scale, vanilla, focused);
    }

    /** How far up the chat moves, in GUI px: from vanilla's bottom to the slot's (GAP above the column's top, or above what its
     *  lowest line would cover); 0 without a column. */
    public static int lift(int scaledWidth, int scaledHeight, double scale, int lineHeight, int width, boolean focused) {
        UiMath.ChatSlot s = slot(1, scaledWidth, scaledHeight, scale, lineHeight, width, focused);
        return s == null ? 0 : Math.max(0, scaledHeight - VANILLA_BOTTOM - s.bottom());
    }

    /** lift for the game's own chat as it is now (its scale, width, line spacing and focus), for checks. */
    public static int lift(int scaledHeight) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ChatHud chat = mc.inGameHud.getChatHud();
        ChatHudAccessor a = (ChatHudAccessor) chat;
        return lift(mc.getWindow().getScaledWidth(), scaledHeight, chat.getChatScale(), a.gravesandglory$getLineHeight(), chat.getWidth(), a.gravesandglory$isChatFocused());
    }

    /** The GUI x where the chat's line backgrounds end. ChatHud.render draws each line ceil(width / scale) wide in its own space,
     *  from 4 px left of that to 8 px right, scaled by scale and moved 4 px right: from GUI x 0 to (ceil(width / scale) + 12) x
     *  scale, which is about width + 12 x scale. */
    public static double right(int width, double scale) { float f = (float) scale; return (Math.ceil(width / f) + 12) * f; }

    /** The lines the lifted chat shows: vanilla's, or as many as fit between its bottom and the lowest of the score bar, the
     *  bosses, the seals, the aimed point's name, the lesson card, the respawn card (while the commander is down) and the feed
     *  over the chat's width, GAP clear; at least one. The crosshair counts only while the chat is closed (focused false), since
     *  one or two lines are too few to type by (UI polish round 2). */
    public static int lines(int vanilla, int scaledWidth, int scaledHeight, double scale, int lineHeight, int width, boolean focused) {
        UiMath.ChatSlot s = slot(vanilla, scaledWidth, scaledHeight, scale, lineHeight, width, focused);
        return s == null ? vanilla : s.lines();
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
