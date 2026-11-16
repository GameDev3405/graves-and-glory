package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.FilmRig;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Unshipped. Feeds the client's tick counter a virtual clock that runs at 1/FilmRig.slow of real time, so client ticks and
 *  their tickDelta interpolation slow with the server (FilmSlowServerMixin). The clock is continuous, so switching the
 *  factor never jumps time. Vanilla at 1 apart from a fixed offset. 1.21.1: the counter is RenderTickCounter.Dynamic and
 *  its public beginRenderTick(long timeMillis, boolean tick) takes the clock. */
@Mixin(RenderTickCounter.Dynamic.class)
abstract class FilmSlowClockMixin {
    @Unique private static double gravesandgloryTest$virtual;
    @Unique private static long gravesandgloryTest$last = Long.MIN_VALUE;
    @ModifyVariable(method = "beginRenderTick(JZ)I", at = @At("HEAD"), argsOnly = true)
    private long gravesandgloryTest$clock(long real) {
        if (gravesandgloryTest$last == Long.MIN_VALUE) { gravesandgloryTest$last = real; gravesandgloryTest$virtual = real; }
        gravesandgloryTest$virtual += (real - gravesandgloryTest$last) / (double) Math.max(1, FilmRig.slow);
        gravesandgloryTest$last = real;
        return (long) gravesandgloryTest$virtual;
    }
}
