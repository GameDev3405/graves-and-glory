package gg.gravesandglory.mixin;

import gg.gravesandglory.GravesAndGlory;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only confirmed enemy damage from the viewing commander produces a contact accent. */
@Mixin(LivingEntity.class)
abstract class ContactFeedbackMixin {
    @Unique private long gravesandgloryLastContact=-20;
    @Inject(method="damage",at=@At("RETURN"))
    void gravesandgloryContact(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        if(!ci.getReturnValue()||amount<=0||!(source.getAttacker() instanceof ServerPlayerEntity player))return;
        var battle=GravesAndGlory.battle;if(battle==null||!battle.running||player.getWorld()!=battle.world)return;
        LivingEntity self=(LivingEntity)(Object)this;
        long time=battle.world.getTime();if(time-gravesandgloryLastContact<6)return;
        gravesandgloryLastContact=time;gg.gravesandglory.BattleEffects.contact(self,source,amount);
    }
}
