package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.FilmRig;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. The end of every rendered frame: the media shoot reads the finished frame back here. */
@Mixin(MinecraftClient.class)
abstract class FilmFrameMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void gravesandgloryTest$filmFrame(boolean tick,CallbackInfo ci){
        FilmRig.afterFrame((MinecraftClient)(Object)this);
    }
}
