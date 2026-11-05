package gg.gravesandglory.mixin;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
/** The dispatcher's shadow switch and its shadow draw, which BodyCache uses for the bodies it draws past the dispatcher. */
@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderDispatcherAccessor {
    @Accessor("renderShadows") boolean gravesandglory$renderShadows();
    @Invoker("renderShadow") static void gravesandglory$renderShadow(MatrixStack matrices,VertexConsumerProvider consumers,Entity entity,float opacity,float tickDelta,WorldView world,float radius){throw new AssertionError();}
}
