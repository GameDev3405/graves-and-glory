package gg.gravesandglory.mixin;

import gg.gravesandglory.UiEditorClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The UI editor, phase 2: while it is on, each visible button's box is kept as drawn, so a line that runs into a button is warned
 *  about (UiEditorClient.widget). Does nothing while the editor is off. */
@Mixin(ClickableWidget.class)
abstract class UiWidgetLocateMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void gravesandglory$locate(DrawContext g, int mx, int my, float delta, CallbackInfo ci) {
        if (!UiEditorClient.active()) return;
        ClickableWidget w = (ClickableWidget) (Object) this;
        if (w.visible) UiEditorClient.widget(g, w.getX(), w.getY(), w.getWidth(), w.getHeight());
    }
}
