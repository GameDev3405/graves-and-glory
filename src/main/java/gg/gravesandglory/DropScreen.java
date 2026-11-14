package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * The drop board's input layer while the cinematic runs (design 2026-11-14, section 5): the mouse slides the coin along the rail, a
 * click or Space lets it go, and after that a click, Escape or the menu key skips. It draws only the letterbox, the hints and the
 * result's plate (DropRender.screen); the world shows through. The cursor is hidden while it is open.
 */
final class DropScreen extends Screen {
    DropScreen() { super(Text.literal("The altar")); }

    @Override protected void init() { GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN); }
    @Override public void removed() { if (client != null) GLFW.glfwSetInputMode(client.getWindow().getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL); }
    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void renderBackground(DrawContext g) {}

    @Override public void render(DrawContext g, int mouseX, int mouseY, float delta) { DropRender.screen(g, width, height); }

    /** The coin stands under the cursor: the screen's width at the altar's distance, through the cinematic's field of view. */
    @Override public void mouseMoved(double x, double y) {
        double span = 2 * DropClient.WIDE * Math.tan(Math.toRadians(DropClient.FOV / 2)) * width / Math.max(1, height);
        DropClient.aim(DropSim.WIDTH / 2.0 + (x / Math.max(1, width) - 0.5) * span);
    }

    @Override public boolean mouseClicked(double x, double y, int button) { press(); return true; }

    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE || GravesAndGloryClient.menu != null && GravesAndGloryClient.menu.matchesKey(key, scan)) { press(); return true; }
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_A) { var v = DropClient.mine; if (v != null) DropClient.aim(v.targetX - 0.5); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_D) { var v = DropClient.mine; if (v != null) DropClient.aim(v.targetX + 0.5); return true; }
        return true;
    }

    private void press() {
        var v = DropClient.mine;
        if (v == null) return;
        if (!v.released) DropClient.release(); else DropClient.skip(client);
    }
}
