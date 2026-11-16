package gg.gravesandglory.polymer.mixin;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import gg.gravesandglory.PlainLooks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Plain clients (P3), Polymer loaded only: troop_fence and still_water are barriers to a viewer that cannot decode them, and
 *  themselves to a modpack viewer whose raw ids match the server's (PlainLooks.realBlocks). Being PolymerObjects, Polymer marks
 *  them server-only when they are registered. */
@Mixin(targets = {"gg.gravesandglory.MapBlocks$Fence", "gg.gravesandglory.MapBlocks$StillWater"})
public abstract class MapBlockPolymerMixin implements PolymerBlock {
    /** Without a viewer (a packet built for nobody in particular): the stand-in, which every client can decode. */
    @Override public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.BARRIER.getDefaultState();
    }
    @Override public BlockState getPolymerBlockState(BlockState state, ServerPlayerEntity player) {
        return PlainLooks.realBlocks(player) ? state : Blocks.BARRIER.getDefaultState();
    }
}
