package gg.gravesandglory.mixin;

import gg.gravesandglory.PlainClients;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Plain clients P4 (review H1): a plain player's hotbar items stay locked in every window (own inventory, barrels, anvils); a click that would move one
 *  (pick up, swap, shift-click, drag, offhand) is refused. PlainMenu overrides onSlotClick without super, so its own lock is separate. */
@Mixin(ScreenHandler.class)
abstract class PlainInventoryMixin {
    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void gravesandglory$plainLock(int slot, int button, SlotActionType type, PlayerEntity player, CallbackInfo ci) {
        if (PlainClients.lockedClick((ScreenHandler) (Object) this, slot, button, type, player)) ci.cancel();
    }
}
