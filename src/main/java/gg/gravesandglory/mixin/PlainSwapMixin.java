package gg.gravesandglory.mixin;

import gg.gravesandglory.PlainClients;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plain clients P4 (plan decision 2): a plain player's swap-hand key (F). In a battle it turns the hotbar page (orders, tactics); a
 *  plain hotbar item is never swapped into the offhand. The packet first arrives on the network thread, where vanilla requeues it
 *  onto the server thread: only that second, on-thread call is handled. */
@Mixin(ServerPlayNetworkHandler.class)
abstract class PlainSwapMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void gravesandglory$plainSwap(PlayerActionC2SPacket packet, CallbackInfo ci) {
        if (!PlainClients.ON || packet.getAction() != PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND) return;
        if (player.getServer() == null || !player.getServer().isOnThread()) return;
        if (PlainClients.swapHands(player)) ci.cancel();
    }
}
