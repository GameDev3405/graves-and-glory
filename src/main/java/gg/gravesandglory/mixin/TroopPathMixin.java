package gg.gravesandglory.mixin;

import gg.gravesandglory.ArmyMob;
import gg.gravesandglory.MapBlockRules;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.ai.pathing.PathContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Troops path round the map blocks (docs/props/MAP-BLOCKS.md): troop_fence and still_water answer vanilla's context-free shape query with an empty shape, so the stock node
 * maker calls both open. A node maker whose own mob is a troop (ArmyMob) classifies their cells, and the cell above each, BLOCKED (MapBlockRules.troopNode); every other mob's
 * node maker is left alone, because the test is on the maker's entity. The amphibious maker (drowned) has its own copy of this hook, TroopAmphibiousPathMixin.
 */
@Mixin(LandPathNodeMaker.class)
abstract class TroopPathMixin extends PathNodeMaker {
    @Inject(method = "getDefaultNodeType", at = @At("RETURN"), cancellable = true)
    void gravesandgloryTroopBlocks(PathContext context, int x, int y, int z, CallbackInfoReturnable<PathNodeType> cir) {
        if (entity instanceof ArmyMob) cir.setReturnValue(MapBlockRules.troopNode(context.getWorld(), x, y, z, cir.getReturnValue()));
    }
}
