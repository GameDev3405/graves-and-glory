package gg.gravesandglory.mixin;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** MatrixStack.Entry's renormalise flag (1.21: transformNormal normalises each normal unless it is set), which Portrait sets once it
 *  has put back the 1.20.1 normal matrix, so a vanilla model's normals are used as given, as on 1.20.1. */
@Mixin(MatrixStack.Entry.class)
public interface MatrixEntryAccessor {
    @Accessor("canSkipNormalization") void gravesandglory$setCanSkipNormalization(boolean skip);
}
