package gg.gravesandglory.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.gravesandglory.UiPreview;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The UI editor's size preview (UiPreview): inside a preview scope the window answers with the previewed framebuffer size, GUI scale
 *  and scaled size; outside one (and whenever no size is previewed) with its own. One static check per call, no allocation. */
@Mixin(Window.class)
abstract class UiPreviewWindowMixin {
    @ModifyReturnValue(method = "getFramebufferWidth", at = @At("RETURN"))
    private int gravesandglory$fbW(int real) { return UiPreview.scoped() ? UiPreview.fbW() : real; }

    @ModifyReturnValue(method = "getFramebufferHeight", at = @At("RETURN"))
    private int gravesandglory$fbH(int real) { return UiPreview.scoped() ? UiPreview.fbH() : real; }

    @ModifyReturnValue(method = "getScaleFactor", at = @At("RETURN"))
    private double gravesandglory$scale(double real) { return UiPreview.scoped() ? UiPreview.scale() : real; }

    @ModifyReturnValue(method = "getScaledWidth", at = @At("RETURN"))
    private int gravesandglory$w(int real) { return UiPreview.scoped() ? UiPreview.w() : real; }

    @ModifyReturnValue(method = "getScaledHeight", at = @At("RETURN"))
    private int gravesandglory$h(int real) { return UiPreview.scoped() ? UiPreview.h() : real; }
}
