package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.FilmRig;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. The media shoot's free camera: placed once per rendered frame from FilmRig's path, after the title-demo hook. */
@Mixin(value=Camera.class,priority=1200)
abstract class FilmCameraMixin {
    @Shadow protected abstract void setPos(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("TAIL"))
    private void gravesandgloryTest$filmCamera(BlockView area,Entity entity,boolean third,boolean inverse,float delta,CallbackInfo ci){
        double[] v=FilmRig.sample();
        if(v!=null){setRotation((float)v[3],(float)v[4]);setPos(v[0],v[1],v[2]);}
    }
}
