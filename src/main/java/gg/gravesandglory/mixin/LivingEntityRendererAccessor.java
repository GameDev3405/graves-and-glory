package gg.gravesandglory.mixin;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Vanilla's protected LivingEntityRenderer.scale, the size a renderer gives its model about the feet (0.9375 for a player, 1.0625
 *  for a husk, 1.2 for a wither skeleton), which Portrait reads so rule 8 frames every model in its own pixels. */
@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
    /** LivingEntityRenderer.scale: applies the renderer's own scale for the entity to the matrices. */
    @Invoker("scale") void gravesandglory$scale(LivingEntity entity, MatrixStack matrices, float tickDelta);
}
