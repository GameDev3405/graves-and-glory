package gg.gravesandglory.mixin;

import gg.gravesandglory.PropGameplay;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A battle player's main-hand swing at a destructible prop's barrier is a heavy hit on it (PropGameplay.swing). The adventure-mode client sends no
 *  block attack for a barrier, but it still swings; this runs on the server thread, after vanilla's forceMainThread. */
@Mixin(ServerPlayNetworkHandler.class)
abstract class PropSwingMixin {
    @Shadow public ServerPlayerEntity player;
    @Inject(method="onHandSwing",at=@At("TAIL"))
    void gravesandglorySwing(HandSwingC2SPacket packet,CallbackInfo ci){
        if(packet.getHand()==Hand.MAIN_HAND)PropGameplay.swing(player);
    }
}
