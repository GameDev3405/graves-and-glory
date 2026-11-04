package gg.gravesandglory.mixin;

import gg.gravesandglory.GearCombat;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A shield block in battle puts the shield on the worn off-hand family's cooldown (GearCombat.onShieldBlock); vanilla's own wear goes on. */
@Mixin(PlayerEntity.class)
abstract class ShieldCooldownMixin {
    @Inject(method="damageShield",at=@At("HEAD"))
    private void gravesandgloryShieldCooldown(float amount,CallbackInfo ci){
        GearCombat.onShieldBlock((PlayerEntity)(Object)this);
    }
}
