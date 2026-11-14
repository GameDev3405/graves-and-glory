package gg.gravesandglory.mixin;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.AffineTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** A display entity's transformation and interpolation (moving prop parts, docs/props/MOVING-PARTS.md): PropTicker sets a keyframe through these. */
@Mixin(DisplayEntity.class)
public interface DisplayPoseAccessor {
    @Invoker("setTransformation") void gravesandglory$setTransformation(AffineTransformation t);
    @Invoker("setInterpolationDuration") void gravesandglory$setInterpolationDuration(int ticks);
    /** Forced write (vanilla passes force), so the same value 0 still restarts the client's tween. */
    @Invoker("setStartInterpolation") void gravesandglory$setStartInterpolation(int ticks);
    @Invoker("getTransformation") static AffineTransformation gravesandglory$getTransformation(DataTracker tracker){throw new AssertionError();}
}
