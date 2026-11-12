package gg.gravesandglory.mixin;

import gg.gravesandglory.EndingClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Escape during the match ending (no screen open) skips this player's step instead of opening the pause menu, as the menu key does. */
@Mixin(Keyboard.class)
abstract class EndingKeyMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void graves$endingEscape(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key != GLFW.GLFW_KEY_ESCAPE || window != client.getWindow().getHandle() || client.currentScreen != null || EndingClient.step() == null) return;
        if (action == GLFW.GLFW_PRESS) EndingClient.escape(client);
        ci.cancel();
    }
}
