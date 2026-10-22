package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.FilmRig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unshipped. The media shoot's field of view, a decimal per frame instead of the options slider's whole degrees. */
@Mixin(GameRenderer.class)
abstract class FilmFovMixin {
    @Inject(method="getFov",at=@At("RETURN"),cancellable=true)
    private void gravesandgloryTest$filmFov(Camera camera,float tickDelta,boolean changingFov,CallbackInfoReturnable<Double> cir){
        double f=FilmRig.fov();
        if(f>0)cir.setReturnValue(f);
    }
}
