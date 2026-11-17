package gg.gravesandglory.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gg.gravesandglory.UiPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;

/** The UI editor's size preview: a screen opened from inside a preview scope (a previewed screen's tick or click closing itself) is
 *  opened at the real size; the UI editor lays it out again at the preview's when it is previewed too. */
@Mixin(MinecraftClient.class)
abstract class UiPreviewClientMixin {
    @WrapMethod(method = "setScreen")
    private void gravesandglory$setScreen(Screen screen, Operation<Void> real) {
        int depth = UiPreview.suspend();
        try { real.call(screen); } finally { UiPreview.resume(depth); }
    }
}
