package gg.gravesandglory.mixin;
import gg.gravesandglory.CommanderGearFeature;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The third-person held item draws nothing for a commander weapon CommanderGearFeature draws with its own model (first person is HeldItemRenderer, untouched). */
@Mixin(HeldItemFeatureRenderer.class)
public class HeldItemFeatureMixin {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void gravesandglory$gearArt(LivingEntity entity, ItemStack stack, ModelTransformationMode mode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertices, int light, CallbackInfo ci) {
        if (CommanderGearFeature.drawsOwn(stack)) ci.cancel();
    }
}
