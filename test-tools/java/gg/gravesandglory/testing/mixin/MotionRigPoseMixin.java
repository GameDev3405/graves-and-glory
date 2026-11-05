package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.MotionChecks;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. The motion suite's joint bends (MotionChecks.BENDS): pitch added to a troop's forearms or shins right after the
 *  model's own setAngles, before the model and its layers draw. Does nothing for an entity without bends. */
@Mixin(value=LivingEntityRenderer.class,priority=1100)
abstract class MotionRigPoseMixin<T extends LivingEntity,M extends EntityModel<T>> {
    @Shadow protected M model;
    @Inject(method="render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/client/render/entity/model/EntityModel;setAngles(Lnet/minecraft/entity/Entity;FFFFF)V",shift=At.Shift.AFTER))
    private void gravesandgloryTest$motionBend(T entity,float yaw,float delta,MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci){
        MotionChecks.bend(entity,model);
    }
}
