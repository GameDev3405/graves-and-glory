package gg.gravesandglory.mixin;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gg.gravesandglory.BattleHud;
import gg.gravesandglory.GravesAndGloryClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
/** No hunger on a Graves and Glory server (it pins food at 20): the food row is not drawn there, in camp, in battle and while a
 *  battlefield builds. The gate is GravesAndGloryClient.modServer, set by the first STATE packet and cleared on disconnect, so
 *  saves the mod does not run keep vanilla hunger and their food row. Only the food icon blits are skipped, so hearts, armour and
 *  air bubbles draw as before. 1.21: the food row is its own method, renderFood (called between the profiler's "food" and "air"
 *  swaps, as the 1.20.1 blits were), and draws GUI sprites (drawGuiTexture), two or three a shank as 1.20.1's drawTexture did. */
@Mixin(InGameHud.class)
abstract class HungerHudMixin {
    @WrapWithCondition(method = "renderFood",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    private boolean gravesandglory$food(DrawContext g, Identifier id, int x, int y, int w, int h) {
        if (!GravesAndGloryClient.modServer) return true;
        BattleHud.foodSkipped++; return false;
    }
}
