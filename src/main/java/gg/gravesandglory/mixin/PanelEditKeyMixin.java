package gg.gravesandglory.mixin;

import gg.gravesandglory.CommandPanel;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Esc in the command panel's edit mode leaves it (saving) instead of opening the pause menu. */
@Mixin(Keyboard.class)
abstract class PanelEditKeyMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void graves$panelEditEscape(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key != GLFW.GLFW_KEY_ESCAPE || window != client.getWindow().getHandle() || client.currentScreen != null || !CommandPanel.editing()) return;
        if (action == GLFW.GLFW_PRESS) CommandPanel.leaveEdit(client);
        ci.cancel();
    }
}
