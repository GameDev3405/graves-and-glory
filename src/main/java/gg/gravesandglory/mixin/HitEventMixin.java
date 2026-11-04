package gg.gravesandglory.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import gg.gravesandglory.BattleMath;
import gg.gravesandglory.MotionEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A troop struck by anything but a troop's blow (a player, a fall, fire, magic) sends hit_v1 too (MotionEvents.damaged); Combat.resolve sends its own.
 * It is sent when damage returns true, and also when vanilla's shield stopped the blow whole (damage then returns false): that blow is a hit, blocked.
 * Its weight reads the health and absorption the troop really lost in this call, so a blow inside the hurt cooldown or on a shield reads light.
 */
@Mixin(LivingEntity.class)
abstract class HitEventMixin {
    @Inject(method="damage",at=@At("HEAD"))
    void gravesandgloryBefore(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci,@Share("before") LocalFloatRef before){
        LivingEntity self=(LivingEntity)(Object)this;before.set(self.getHealth()+self.getAbsorptionAmount());
    }
    /** Vanilla calls damageShield only when the raised shield took the blow (LivingEntity.damage, after blockedByShield). */
    @Inject(method="damage",at=@At(value="INVOKE",target="Lnet/minecraft/entity/LivingEntity;damageShield(F)V"))
    void gravesandgloryBlocked(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci,@Share("blocked") LocalBooleanRef blocked){blocked.set(true);}
    @Inject(method="damage",at=@At("RETURN"))
    void gravesandgloryHit(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci,@Share("before") LocalFloatRef before,@Share("blocked") LocalBooleanRef blocked){
        if(!ci.getReturnValue()&&!blocked.get())return;
        LivingEntity self=(LivingEntity)(Object)this;
        MotionEvents.damaged(self,source,BattleMath.lostHealth(before.get(),self.getHealth()+self.getAbsorptionAmount()),blocked.get());
    }
}
