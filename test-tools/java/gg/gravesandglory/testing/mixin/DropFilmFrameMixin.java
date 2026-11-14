package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.DropFilm;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. The end of every rendered frame: the drop-board film reads the finished frame back here. */
@Mixin(MinecraftClient.class)
abstract class DropFilmFrameMixin {
    @Inject(method="render",at=@At("TAIL"))
    private void gravesandgloryTest$dropFilmFrame(boolean tick,CallbackInfo ci){
        DropFilm.afterFrame((MinecraftClient)(Object)this);
    }
}
