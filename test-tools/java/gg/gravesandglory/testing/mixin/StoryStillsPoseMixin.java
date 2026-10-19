package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.StoryStills;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. Holds a staged actor's head, arms and legs at the angles StoryStills.POSES gives for its entity id, right
 *  after the model's own setAngles (so after the zombie arms a husk is drawn with), before armour and held items copy
 *  them. Does nothing for an entity without a pose. */
@Mixin(value=LivingEntityRenderer.class,priority=1100)
abstract class StoryStillsPoseMixin<T extends LivingEntity,M extends EntityModel<T>> {
    @Shadow protected M model;
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void gravesandgloryTest$storyPose(T entity,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci){
        var pose=StoryStills.POSES.get(entity.getId());
        if(pose==null||!(model instanceof BipedEntityModel<?> biped))return;
        for(var e:pose.entrySet()){
            ModelPart part=switch(e.getKey()){case "head"->biped.head;case "body"->biped.body;case "rarm"->biped.rightArm;case "larm"->biped.leftArm;case "rleg"->biped.rightLeg;case "lleg"->biped.leftLeg;default->null;};
            if(part==null)continue;float[] v=e.getValue();part.pitch=v[0];part.yaw=v[1];part.roll=v[2];
        }
        biped.hat.copyTransform(biped.head);
    }
}
