package gg.gravesandglory.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.gravesandglory.HintText;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
/** A keybind text for a binding the mod's hints name (HintText.hinted) names its key as the feed and the chips do: a mouse button
 *  as LMB, RMB or MMB (HintText.chip), any other key, and "Not bound", as vanilla names it, whole: the action bar and the camp
 *  sign have no row to fit. So the server's notices and the camp sign (Campaign.keyText) read the player's key as the feed reads
 *  it. The text keeps its style. */
@Mixin(KeybindTextContent.class)
abstract class KeybindTextMixin {
    @Shadow @Final private String key;

    @ModifyReturnValue(method = "getTranslated", at = @At("RETURN"))
    private Text gravesandglory$chip(Text name) {
        if (!HintText.hinted(key)) return name;
        String s = name.getString(), chip = HintText.chip(s);
        return chip.equals(s) ? name : Text.literal(chip).setStyle(name.getStyle());
    }
}
