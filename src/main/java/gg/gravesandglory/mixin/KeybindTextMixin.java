package gg.gravesandglory.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import gg.gravesandglory.GravesAndGloryClient;
import net.minecraft.text.KeybindTextContent;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
/** On a Graves and Glory server, a keybind text for a binding the mod's hints name (HintText.hinted) names its key as the feed
 *  and the chips do: the first three mouse buttons as LMB, RMB or MMB, by the key bound (GravesAndGloryClient.keybindChip), in
 *  every language; any other key, and an unbound one, as the game names it, whole: the action bar and the camp sign have no row
 *  to fit. So the server's notices and the camp sign (Campaign.keyText) read the player's key as the feed reads it. Anywhere
 *  else (another server, another world) every keybind text stays vanilla's. The text keeps its style. */
@Mixin(KeybindTextContent.class)
abstract class KeybindTextMixin {
    @Shadow @Final private String key;

    @ModifyReturnValue(method = "getTranslated", at = @At("RETURN"))
    private Text gravesandglory$chip(Text name) {
        String chip = GravesAndGloryClient.keybindChip(key);
        return chip == null ? name : Text.literal(chip).setStyle(name.getStyle());
    }
}
