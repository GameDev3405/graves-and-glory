package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.FilmRig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Unshipped. The media shoot draws no HUD but keeps options.hudHidden off, because the first-person hand is skipped while it is on. */
@Mixin(InGameHud.class)
abstract class FilmHudMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void gravesandgloryTest$noHud(DrawContext context,float tickDelta,CallbackInfo ci){
        if(FilmRig.noHud)ci.cancel();
    }
}
