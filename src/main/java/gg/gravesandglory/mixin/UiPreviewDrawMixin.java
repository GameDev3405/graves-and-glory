package gg.gravesandglory.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.gravesandglory.UiEditorClient;
import gg.gravesandglory.UiPreview;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The UI editor, phase 2: a scissor box set while a screen is previewed at another size is moved from the preview's framebuffer to
 *  the real one (UiPreview.scissor), and while the editor is on each scissor box is followed, so a line clipped by its box is
 *  reported (UiEditorClient.clip). Does nothing while the editor is off and no size is previewed. */
@Mixin(DrawContext.class)
abstract class UiPreviewDrawMixin {
    @WrapOperation(method = "setScissor", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableScissor(IIII)V"))
    private void gravesandglory$scissor(int x, int y, int w, int h, Operation<Void> real) {
        if (UiPreview.scoped()) UiPreview.scissor(x, y, w, h); else real.call(x, y, w, h);
    }

    @Inject(method = "enableScissor(IIII)V", at = @At("HEAD"))
    private void gravesandglory$clipOn(int x0, int y0, int x1, int y1, CallbackInfo ci) {
        if (UiEditorClient.active()) UiEditorClient.clipPush(x0, y0, x1, y1);
    }

    @Inject(method = "disableScissor", at = @At("HEAD"))
    private void gravesandglory$clipOff(CallbackInfo ci) {
        if (UiEditorClient.active()) UiEditorClient.clipPop();
    }
}
