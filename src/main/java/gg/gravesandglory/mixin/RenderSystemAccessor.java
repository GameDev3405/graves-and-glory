package gg.gravesandglory.mixin;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** RenderSystem's two shader light directions (in view axes for the world pass), which BodyCache turns back into world axes for the bodies it draws. */
@Mixin(value=RenderSystem.class,remap=false)
public interface RenderSystemAccessor {
    @Accessor("shaderLightDirections") static Vector3f[] gravesandglory$lights(){throw new AssertionError();}
}
