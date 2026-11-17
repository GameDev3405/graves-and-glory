package gg.gravesandglory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.gravesandglory.UiPreview;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;

/** The UI editor's size preview: a mouse event for a previewed screen is handled in a preview scope, so vanilla maps the pointer with
 *  the preview's scaled size over the real window size: where it points on the stretched screen. */
@Mixin(Mouse.class)
abstract class UiPreviewMouseMixin {
    @WrapMethod(method = "onMouseButton")
    private void gravesandglory$button(long window, int button, int action, int mods, Operation<Void> real) {
        boolean in = UiPreview.previewingShown() && UiPreview.enter();
        try { real.call(window, button, action, mods); } finally { if (in) UiPreview.exit(); }
    }

    @WrapMethod(method = "onCursorPos")
    private void gravesandglory$move(long window, double x, double y, Operation<Void> real) {
        boolean in = UiPreview.previewingShown() && UiPreview.enter();
        try { real.call(window, x, y); } finally { if (in) UiPreview.exit(); }
    }

    @WrapMethod(method = "onMouseScroll")
    private void gravesandglory$scroll(long window, double h, double v, Operation<Void> real) {
        boolean in = UiPreview.previewingShown() && UiPreview.enter();
        try { real.call(window, h, v); } finally { if (in) UiPreview.exit(); }
    }
}
