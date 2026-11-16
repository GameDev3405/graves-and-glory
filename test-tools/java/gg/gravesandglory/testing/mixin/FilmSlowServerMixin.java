package gg.gravesandglory.testing.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.gravesandglory.FilmRig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Unshipped. Stretches the server's tick slot by FilmRig.slow while a title slow-motion take runs. Vanilla at 1.
 *  1.21.1: runServer reads the slot from ServerTickManager.getNanosPerTick() (50 ms at 20 TPS) instead of a 50L constant. */
@Mixin(MinecraftServer.class)
abstract class FilmSlowServerMixin {
    @ModifyExpressionValue(method = "runServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickManager;getNanosPerTick()J"), require = 1)
    private long gravesandgloryTest$slowTicks(long v) { return v * Math.max(1, FilmRig.slow); }
}
