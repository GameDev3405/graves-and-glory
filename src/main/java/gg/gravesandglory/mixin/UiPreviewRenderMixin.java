package gg.gravesandglory.mixin;

import gg.gravesandglory.UiPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** The UI editor's size preview: a previewed screen is drawn with the pointer in its own (preview) GUI pixels. */
@Mixin(GameRenderer.class)
abstract class UiPreviewRenderMixin {
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"), index = 1)
    private int gravesandglory$mouseX(int real) {
        if (!UiPreview.previewingShown()) return real;
        MinecraftClient c = MinecraftClient.getInstance();
        return (int) (c.mouse.getX() * UiPreview.w() / Math.max(1, c.getWindow().getWidth()));
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"), index = 2)
    private int gravesandglory$mouseY(int real) {
        if (!UiPreview.previewingShown()) return real;
        MinecraftClient c = MinecraftClient.getInstance();
        return (int) (c.mouse.getY() * UiPreview.h() / Math.max(1, c.getWindow().getHeight()));
    }
}
