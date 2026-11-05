package gg.gravesandglory.mixin;
import gg.gravesandglory.CommanderGearFeature;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The vanilla armour layer draws nothing for a commander gear stand-in that CommanderGearFeature draws with its own model. */
@Mixin(ArmorFeatureRenderer.class)
public class ArmorFeatureMixin<T extends LivingEntity, A extends BipedEntityModel<T>> {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private void gravesandglory$gearArt(MatrixStack matrices, VertexConsumerProvider vertices, T entity, EquipmentSlot slot, int light, A model, CallbackInfo ci) {
        if (CommanderGearFeature.drawsOwn(entity.getEquippedStack(slot))) ci.cancel();
    }
}
