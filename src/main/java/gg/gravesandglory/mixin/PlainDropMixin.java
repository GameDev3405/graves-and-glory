package gg.gravesandglory.mixin;

import gg.gravesandglory.PlainClients;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plain clients (port plan P3): a plain hotbar item (gg_plain) never lands in the world, whether thrown (Q, a drag out of the
 *  inventory) or dropped on death; the locked hotbar puts it back at once. */
@Mixin(ServerPlayerEntity.class)
abstract class PlainDropMixin {
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void gravesandglory$keepPlainItems(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        if (!PlainClients.ON || PlainClients.kindOf(stack) == null) return;
        PlainClients.hotbar((ServerPlayerEntity) (Object) this);
        cir.setReturnValue(null);
    }
}
