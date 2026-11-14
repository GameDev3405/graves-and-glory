package gg.gravesandglory.mixin;

import gg.gravesandglory.ArmyMob;
import gg.gravesandglory.MapBlockRules;
import net.minecraft.entity.ai.pathing.AmphibiousPathNodeMaker;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TroopPathMixin for the amphibious node maker (a troop drowned), which overrides getDefaultNodeType without calling the land one. */
@Mixin(AmphibiousPathNodeMaker.class)
abstract class TroopAmphibiousPathMixin extends LandPathNodeMaker {
    @Inject(method = "getDefaultNodeType", at = @At("RETURN"), cancellable = true)
    void gravesandgloryTroopBlocks(BlockView world, int x, int y, int z, CallbackInfoReturnable<PathNodeType> cir) {
        if (entity instanceof ArmyMob) cir.setReturnValue(MapBlockRules.troopNode(world, x, y, z, cir.getReturnValue()));
    }
}
