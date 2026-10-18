package gg.gravesandglory.mixin;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.gravesandglory.BattleHud;
import gg.gravesandglory.GravesAndGloryClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;
/** No hunger in Graves and Glory (the server pins food at 20): the food row is not drawn. Only the icon blits between the
 *  profiler's "food" and "air" sections are skipped, so hearts, armour and air bubbles draw as before. */
@Mixin(InGameHud.class)
abstract class HungerHudMixin {
    @WrapWithCondition(method = "renderStatusBars",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIII)V"),
        slice = @Slice(from = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=food"),
                       to = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=air")))
    private boolean gravesandglory$food(DrawContext g, Identifier id, int x, int y, int u, int v, int w, int h) {
        if (!GravesAndGloryClient.ready) return true;
        BattleHud.foodSkipped++; return false;
    }
}
