package gg.gravesandglory.mixin;

import gg.gravesandglory.EndingClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The podium is shown at night (spec 6): this client's time of day while the ending is at the stage. The server's world time is untouched. */
@Mixin(ClientWorld.Properties.class)
abstract class EndingTimeMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void graves$podiumNight(CallbackInfoReturnable<Long> cir) {
        if (EndingClient.night()) cir.setReturnValue(EndingClient.NIGHT);
    }
}
