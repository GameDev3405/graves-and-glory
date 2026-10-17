package gg.gravesandglory.mixin;

import com.google.gson.JsonObject;
import net.minecraft.util.JsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Older model baking can render these angles; its JSON parser rejects them. */
// The nested deserializer is protected; it cannot be used as a Java class literal.
@Mixin(targets = "net.minecraft.client.render.model.json.ModelElement$Deserializer")
public abstract class ModelAngleCompatibilityMixin {
    @Inject(method = "deserializeRotationAngle", at = @At("HEAD"), cancellable = true)
    private void gravesandglory$fractionalAngle(JsonObject rotation, CallbackInfoReturnable<Float> result) {
        if (!rotation.has("angle") || JsonHelper.getBoolean(rotation, "rescale", false)) return;
        float angle = JsonHelper.getFloat(rotation, "angle");
        // Leave vanilla's standard angles, rescaling and invalid-input rejection alone.
        if (Float.isFinite(angle) && angle >= -45 && angle <= 45
                && angle != -45 && angle != -22.5f && angle != 0 && angle != 22.5f && angle != 45)
            result.setReturnValue(angle);
    }
}
