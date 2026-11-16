package gg.gravesandglory.polymer.mixin;

import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Plain clients (P3), Polymer loaded only: the map blocks' builder items show as barriers to every client. Only map builders hold
 *  them, and builder servers run without Polymer, where the items are unchanged. */
@Mixin(targets = "gg.gravesandglory.MapBlocks$MapBlockItem")
public abstract class MapBlockItemPolymerMixin implements PolymerItem {
    @Override public Item getPolymerItem(ItemStack itemStack, ServerPlayerEntity player) {
        return Items.BARRIER;
    }
}
