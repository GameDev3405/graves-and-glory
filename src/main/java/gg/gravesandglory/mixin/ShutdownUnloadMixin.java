package gg.gravesandglory.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 1.21.1 port: the "Saving worlds" loop in MinecraftServer.shutdown ticks each world's chunk manager with a supplier that is always true, so
 * ServerChunkLoadingManager.unloadChunks drains its unload queue until it is empty. A chunk queued for unload whose holder a ChunkLoader has
 * acquired since (refCount above 0, the saving future done) fails ChunkHolder.isSavable, and its unload task re-queues itself at once
 * (tryUnloadChunk, new in 1.21): the drain never ends, while the loader that holds the reference only advances and releases it in
 * updateChunks on this same server thread. The game spins at "Saving worlds" for good (port test lane E3: archer, bonebow, e3-camp4).
 * Each tick of that loop gets a 50 ms budget instead; the loop itself still runs until every world reports nothing left to save.
 */
@Mixin(MinecraftServer.class)
abstract class ShutdownUnloadMixin {
    @ModifyArg(method="shutdown",at=@At(value="INVOKE",target="Lnet/minecraft/server/world/ServerChunkManager;tick(Ljava/util/function/BooleanSupplier;Z)V"),index=0)
    private BooleanSupplier gravesandglory$boundedShutdownTick(BooleanSupplier keepTicking){
        long until=Util.getMeasuringTimeNano()+50_000_000L;
        return ()->Util.getMeasuringTimeNano()<until;
    }
}
