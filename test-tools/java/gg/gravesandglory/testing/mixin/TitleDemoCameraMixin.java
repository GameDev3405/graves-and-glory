package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.TitleDemos;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. A free camera for the title-screen demo photographs while perspective stays third person behind, so the
 *  local player is still drawn. Runs after the game's own camera hooks; does nothing while TitleDemos.camera is null. */
@Mixin(value=Camera.class,priority=1100)
abstract class TitleDemoCameraMixin {
    @Shadow protected abstract void setPos(double x,double y,double z);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="update",at=@At("TAIL"))
    private void gravesandgloryTest$titleDemoCamera(BlockView area,Entity entity,boolean third,boolean inverse,float delta,CallbackInfo ci){
        double[] v=TitleDemos.camera;
        if(v!=null){setRotation((float)v[3],(float)v[4]);setPos(v[0],v[1],v[2]);}
    }
}
