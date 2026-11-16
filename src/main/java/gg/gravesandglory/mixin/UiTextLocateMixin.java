package gg.gravesandglory.mixin;

import gg.gravesandglory.UiEditorClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The UI editor (phase 1: words): while it is on, every string a screen draws is offered to UiEditorClient.drawn, which keeps the
 *  box of each one that UiText gave out, so a click on the line finds its piece. Does nothing while the editor is off. Only finds
 *  where a piece is drawn; what is drawn is decided by UiText alone (no wording is ever matched to change it). */
@Mixin(DrawContext.class)
abstract class UiTextLocateMixin {
    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", at = @At("HEAD"))
    private void gravesandglory$locateString(TextRenderer tr, String text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (UiEditorClient.active()) UiEditorClient.drawn((DrawContext) (Object) this, tr, text, x, y);
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"))
    private void gravesandglory$locateOrdered(TextRenderer tr, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (!UiEditorClient.active() || text == null) return;
        StringBuilder b = new StringBuilder();
        text.accept((index, style, cp) -> { b.appendCodePoint(cp); return true; });
        UiEditorClient.drawn((DrawContext) (Object) this, tr, b.toString(), x, y);
    }
}
