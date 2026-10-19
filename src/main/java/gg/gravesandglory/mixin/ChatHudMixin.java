package gg.gravesandglory.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gg.gravesandglory.ChatPlacement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The chat above the battle HUD's left column (ChatPlacement): its bottom lifted, in the drawing and in the mouse's hit tests
 *  alike, and its line count cut where the lifted column would reach the HUD above it. Its scale and width stay vanilla's. Each
 *  frame's line backgrounds are recorded for checks. */
@Mixin(ChatHud.class)
abstract class ChatHudMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow public abstract double getChatScale();
    @Shadow public abstract int getWidth();

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;getScaledWindowHeight()I"))
    private int gravesandglory$drawHeight(int height) { return height - ChatPlacement.lift(height); }

    @ModifyExpressionValue(method = {"toChatLineY", "mouseClicked"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getScaledHeight()I"))
    private int gravesandglory$hitHeight(int height) { return height - ChatPlacement.lift(height); }

    @ModifyReturnValue(method = "getVisibleLineCount", at = @At("RETURN"))
    private int gravesandglory$lines(int lines) {
        int lineHeight = ((ChatHudAccessor) (Object) this).gravesandglory$getLineHeight();
        return ChatPlacement.lines(lines, client.getWindow().getScaledHeight(), getChatScale(), lineHeight, getWidth());
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void gravesandglory$begin(DrawContext g, int tick, int mouseX, int mouseY, CallbackInfo ci) { ChatPlacement.begin(); }

    @Inject(method = "render", at = @At("RETURN"))
    private void gravesandglory$end(DrawContext g, int tick, int mouseX, int mouseY, CallbackInfo ci) { ChatPlacement.end(); }

    /** The first fill in render is each line's background: fill(-4, y - lineHeight, width + 8, y, colour). */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void gravesandglory$line(DrawContext g, int x1, int y1, int x2, int y2, int color, Operation<Void> fill) {
        fill.call(g, x1, y1, x2, y2, color);
        ChatPlacement.line(x1, y1, x2, y2, getChatScale());
    }
}
