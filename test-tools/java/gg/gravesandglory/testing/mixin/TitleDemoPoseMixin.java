package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.TitleDemos;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. Holds the local player's right arm (and with it the held sword) in a fixed raised pose for the title-screen
 *  demo photographs. PlayerEntityModel copies the arm into the sleeve after this, so the sleeve follows. Does nothing
 *  while TitleDemos.pose is null or for any other entity. */
@Mixin(value=BipedEntityModel.class,priority=1100)
abstract class TitleDemoPoseMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart rightArm;
    @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void gravesandgloryTest$titleDemoPose(T entity,float limb,float distance,float age,float headYaw,float headPitch,CallbackInfo ci){
        float[] p=TitleDemos.pose;
        if(p!=null&&entity==MinecraftClient.getInstance().player){rightArm.pitch=p[0];rightArm.yaw=p[1];rightArm.roll=p[2];}
    }
}
