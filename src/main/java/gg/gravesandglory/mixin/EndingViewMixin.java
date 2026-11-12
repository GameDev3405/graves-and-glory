package gg.gravesandglory.mixin;

import gg.gravesandglory.EndingClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The podium shot's fixed field of view (EndingClient.FOV), whatever the player's FOV option; the plates project with the same value. */
@Mixin(GameRenderer.class)
abstract class EndingViewMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void graves$podiumFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (changingFov && EndingClient.cinematic()) cir.setReturnValue(EndingClient.FOV);
    }
}
