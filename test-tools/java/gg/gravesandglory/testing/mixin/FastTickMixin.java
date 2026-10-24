package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.testing.SimSpeed;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unshipped. With SimSpeed.FAST the wait between ticks ends when the work is done instead of when the 50 ms slot is up.
 *  Vanilla's shouldKeepTicking is "a task is running, or the clock has time left". Here it is "a task is running, or the last runTask
 *  still found work": each wait (runTasksTillTickEnd, which prepareStartRegion also uses) first drains the server queue and the worlds'
 *  chunk-manager queues, which runOneTask only pumps while shouldKeepTicking holds, then the next tick starts at once.
 *  A first version returned hasRunningTasks() alone; that never pumped the chunk queues and hung in prepareStartRegion. Vanilla otherwise. */
@Mixin(MinecraftServer.class)
abstract class FastTickMixin extends ReentrantThreadExecutor<ServerTask> {
    @Unique private boolean gravesandgloryTest$idle;
    FastTickMixin(String name) { super(name); }
    @Inject(method = "runTasksTillTickEnd", at = @At("HEAD"))
    private void gravesandgloryTest$wake(CallbackInfo ci) { gravesandgloryTest$idle = false; }
    @Inject(method = "runTask", at = @At("RETURN"))
    private void gravesandgloryTest$noteIdle(CallbackInfoReturnable<Boolean> cir) { if (!cir.getReturnValueZ()) gravesandgloryTest$idle = true; }
    @Inject(method = "shouldKeepTicking", at = @At("HEAD"), cancellable = true)
    private void gravesandgloryTest$fastTicks(CallbackInfoReturnable<Boolean> cir) {
        if (!SimSpeed.FAST) return;
        SimSpeed.applied = true;
        cir.setReturnValue(this.hasRunningTasks() || !gravesandgloryTest$idle);
    }
}
