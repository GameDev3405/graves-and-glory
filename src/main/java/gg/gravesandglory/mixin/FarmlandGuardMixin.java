package gg.gravesandglory.mixin;

import gg.gravesandglory.GravesAndGlory;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Farmland is never trampled while the game's battle world runs: the new Kingsfield's fields (farmland with crops, Kort's choice) keep their crops
 *  under marching troops and players. Landing still costs what a fall on any other block costs (Block.onLandedUpon). */
@Mixin(FarmlandBlock.class)
abstract class FarmlandGuardMixin {
    @Inject(method="onLandedUpon",at=@At("HEAD"),cancellable=true)
    void gravesandgloryKeepFields(World world,BlockState state,BlockPos pos,Entity entity,float fallDistance,CallbackInfo ci){
        if(GravesAndGlory.battle==null)return;
        entity.handleFallDamage(fallDistance,1.0F,entity.getDamageSources().fall());
        ci.cancel();
    }
}
