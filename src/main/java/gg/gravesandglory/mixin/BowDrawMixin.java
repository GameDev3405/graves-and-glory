package gg.gravesandglory.mixin;

import gg.gravesandglory.GearCombat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A commander's bow reads its draw as use ticks x GearEffects.drawFactor (GearCombat.drawTicks): a longbow draws slower, draw_speed faster. */
@Mixin(BowItem.class)
abstract class BowDrawMixin {
    private static final ThreadLocal<LivingEntity> gravesandglory$user=new ThreadLocal<>();

    @Inject(method="onStoppedUsing",at=@At("HEAD"))
    private void gravesandgloryDrawUser(ItemStack stack,World world,LivingEntity user,int remainingUseTicks,CallbackInfo ci){
        gravesandglory$user.set(user);
    }

    @ModifyArg(method="onStoppedUsing",at=@At(value="INVOKE",target="Lnet/minecraft/item/BowItem;getPullProgress(I)F"),index=0)
    private int gravesandgloryDrawTicks(int ticks){
        var user=gravesandglory$user.get();
        gravesandglory$user.remove();
        return user==null?ticks:GearCombat.drawTicks(user,ticks);
    }
}
