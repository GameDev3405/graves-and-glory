package gg.gravesandglory.mixin;
import gg.gravesandglory.ArmyMobs;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds spell gestures, bow aim and a restrained breathing pose to named troops. */
@Mixin(BipedEntityModel.class)
abstract class BipedAnimationMixin<T extends LivingEntity> {
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Shadow public ModelPart head;
    @Inject(method="setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    void gravesandgloryPose(T e,float limb,float distance,float age,float yaw,float pitch,CallbackInfo ci){
        if(e.getCustomName()==null)return;
        var t=ArmyMobs.named(e.getCustomName().getString()); // a troop's name or its evolved stage name
        if(t!=null){
            if(e instanceof net.minecraft.entity.mob.MobEntity mob&&mob.isAttacking()&&t.ranged()){
                rightArm.pitch=-1.5f+head.pitch;rightArm.yaw=head.yaw-.1f;
                leftArm.pitch=t.magic()?-1.05f+(float)Math.sin(age*.2)*.12f:-1.5f+head.pitch;
                leftArm.yaw=head.yaw+(t.magic()?.6f:.35f);
            }else if(distance<.05){rightArm.roll+=(float)Math.sin(age*.065)*.025f;leftArm.roll-=(float)Math.sin(age*.065)*.025f;}
        }
    }
}
