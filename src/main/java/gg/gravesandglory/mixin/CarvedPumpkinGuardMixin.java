package gg.gravesandglory.mixin;

import gg.gravesandglory.EngineMaps;
import net.minecraft.block.BlockState;
import net.minecraft.block.CarvedPumpkinBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A carved pumpkin laid by the map engine (the new Kingsfield's scarecrows) is dressing: no golem search. Vanilla's search reads neighbours through
 *  CachedBlockPosition, which answers null in chunks that are loaded but not ticking yet, and the server crashed on it while building the map. */
@Mixin(CarvedPumpkinBlock.class)
abstract class CarvedPumpkinGuardMixin {
    @Inject(method="onBlockAdded",at=@At("HEAD"),cancellable=true)
    void gravesandgloryNoGolemFromMaps(BlockState state,World world,BlockPos pos,BlockState oldState,boolean notify,CallbackInfo ci){
        if(EngineMaps.placing)ci.cancel();
    }
}
