package gg.gravesandglory.mixin;

import gg.gravesandglory.EndingClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The match ending's freeze on this client: troops and projectiles are not ticked, so their age, limbs and positions stand still (EndingClient.holds). */
@Mixin(ClientWorld.class)
abstract class EndingWorldMixin {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    private void graves$endingFreeze(Entity entity, CallbackInfo ci) {
        if (EndingClient.holds(entity)) ci.cancel();
    }
}
