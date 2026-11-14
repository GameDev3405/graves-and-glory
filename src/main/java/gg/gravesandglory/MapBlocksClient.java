package gg.gravesandglory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.color.world.BiomeColors;

/** Client side of the map blocks: still_water draws translucent and tinted to the biome water colour (the item uses the default water blue). */
@Environment(EnvType.CLIENT)
final class MapBlocksClient {
    static final int WATER_BLUE = 0x3F76E4;
    static void register() {
        BlockRenderLayerMap.INSTANCE.putBlock(MapBlocks.STILL_WATER, RenderLayer.getTranslucent());
        ColorProviderRegistry.BLOCK.register((state, world, pos, tint) -> world != null && pos != null ? BiomeColors.getWaterColor(world, pos) : WATER_BLUE, MapBlocks.STILL_WATER);
        ColorProviderRegistry.ITEM.register((stack, tint) -> WATER_BLUE, MapBlocks.STILL_WATER_ITEM);
    }
}
