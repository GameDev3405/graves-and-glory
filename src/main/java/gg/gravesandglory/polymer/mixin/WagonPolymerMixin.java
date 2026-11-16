package gg.gravesandglory.polymer.mixin;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import gg.gravesandglory.PlainLooks;
import gg.gravesandglory.WagonEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Plain clients (P3), Polymer loaded only: the supply wagon is a chest minecart to a viewer that cannot decode its type
 *  (PlainLooks.wagon). PolymerBridge.init registers the type as server-only. */
@Mixin(WagonEntity.class)
public abstract class WagonPolymerMixin implements PolymerEntity {
    @Override public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
        return PlainLooks.wagon(player);
    }
}
