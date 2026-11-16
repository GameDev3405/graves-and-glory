package gg.gravesandglory.mixin;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 port: the "Saving worlds" loop in MinecraftServer.shutdown ticks each world's chunk manager with a supplier that is always true, so
 * ServerChunkLoadingManager.unloadChunks drains its unload queue until it is empty. A chunk queued for unload whose holder a ChunkLoader has
 * acquired since (refCount above 0, the saving future done) fails ChunkHolder.isSavable, and its unload task re-queues itself at once
 * (tryUnloadChunk, new in 1.21): the drain never ends. The game spins at "Saving worlds" for good (port test lane E3: archer, bonebow, e3-camp4).
 * Each tick of that loop gets a 50 ms budget instead; the loop itself still runs until every world reports nothing left to save.
 * The loader that holds the reference finishes only through its world's chunk main-thread executor. In that loop the server runs those tasks
 * only inside runTasksTillTickEnd and only before tickStartTimeNanos (the loop sets it 1 ms ahead), which a 50 ms drain always overruns, so
 * before runTasksTillTickEnd each world's queued chunk tasks also run, within another 50 ms (verify lane E3, e3v-quit-01 hung without this).
 */
@Mixin(MinecraftServer.class)
abstract class ShutdownUnloadMixin {
    @ModifyArg(method="shutdown",at=@At(value="INVOKE",target="Lnet/minecraft/server/world/ServerChunkManager;tick(Ljava/util/function/BooleanSupplier;Z)V"),index=0)
    private BooleanSupplier gravesandglory$boundedShutdownTick(BooleanSupplier keepTicking){
        long until=Util.getMeasuringTimeNano()+50_000_000L;
        return ()->Util.getMeasuringTimeNano()<until;
    }

    @Inject(method="shutdown",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;runTasksTillTickEnd()V"))
    private void gravesandglory$runChunkTasks(CallbackInfo ci){
        long until=Util.getMeasuringTimeNano()+50_000_000L;
        for(ServerWorld world:((MinecraftServer)(Object)this).getWorlds())
            while(Util.getMeasuringTimeNano()<until&&world.getChunkManager().executeQueuedTasks()){}
    }
}
