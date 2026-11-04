package gg.gravesandglory.mixin;
import gg.gravesandglory.CommanderClips;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** After vanilla poses a player, a commander weapon's clip (CommanderClips) sets the arms that wield it. */
@Mixin(PlayerEntityModel.class)
public class PlayerModelMixin {
    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void gravesandglory$clips(LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
        CommanderClips.pose((PlayerEntityModel<?>) (Object) this, entity, age);
    }
}
