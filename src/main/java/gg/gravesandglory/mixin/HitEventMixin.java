package gg.gravesandglory.mixin;

import gg.gravesandglory.MotionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A troop struck by anything but a troop's blow (a player, a fall, fire, magic) sends hit_v1 too (MotionEvents.damaged); Combat.resolve sends its own. */
@Mixin(LivingEntity.class)
abstract class HitEventMixin {
    @Inject(method="damage",at=@At("RETURN"))
    void gravesandgloryHit(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        if(ci.getReturnValue())MotionEvents.damaged((LivingEntity)(Object)this,source,amount);
    }
}
