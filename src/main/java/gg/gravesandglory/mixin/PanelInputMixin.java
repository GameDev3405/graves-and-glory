package gg.gravesandglory.mixin;

import gg.gravesandglory.CommandPanel;
import gg.gravesandglory.CommandSelection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The command panel's hold on the mouse (command UI plan Task 7), only while CommandPanel.capturing (panel open, no order being aimed,
 * no Screen): the cursor stays free (the game may not relock it), a left click goes to the panel or picks the troop under the cursor and
 * never swings, a right drag turns the camera and never uses an item. Keyboard input is untouched, so WASD still walks.
 */
@Mixin(Mouse.class)
abstract class PanelInputMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow private double x;
    @Shadow private double y;
    @Unique private boolean gravesRightHeld;

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void graves$panelClick(long window, int button, int action, int mods, CallbackInfo ci) {
        if (window != client.getWindow().getHandle()) return;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_RELEASE && gravesRightHeld) { gravesRightHeld = false; ci.cancel(); return; }
        if (!CommandPanel.capturing()) return;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (action == GLFW.GLFW_PRESS) {
                var w = client.getWindow();
                double gx = x * w.getScaledWidth() / (double) w.getWidth(), gy = y * w.getScaledHeight() / (double) w.getHeight();
                boolean shift = (mods & GLFW.GLFW_MOD_SHIFT) != 0 || CommandSelection.shiftHeld(client);
                CommandPanel.mouseClicked(gx, gy, 0, shift);
            }
            ci.cancel();
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            gravesRightHeld = action == GLFW.GLFW_PRESS;
            ci.cancel();
        } else ci.cancel(); // middle and side buttons: nothing picks blocks or fires bindings while the panel holds the mouse
    }

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void graves$panelDrag(long window, double nx, double ny, CallbackInfo ci) {
        if (window != client.getWindow().getHandle() || !gravesRightHeld || client.player == null || client.mouse.isCursorLocked()) return;
        double sens = client.options.getMouseSensitivity().getValue() * 0.6 + 0.2, k = sens * sens * sens * 8.0;
        int invert = client.options.getInvertYMouse().getValue() ? -1 : 1;
        client.player.changeLookDirection((nx - x) * k, (ny - y) * k * invert);
    }

    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    private void graves$panelKeepsCursor(CallbackInfo ci) {
        if (CommandPanel.capturing()) ci.cancel();
    }
}
