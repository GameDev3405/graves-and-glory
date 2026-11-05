package gg.gravesandglory.mixin;
import gg.gravesandglory.CommandMode;
import gg.gravesandglory.CommandPanel;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MinecraftClient.class)
abstract class CommandInputMixin {
    @Inject(method="doAttack",at=@At("HEAD"),cancellable=true)
    private void gravesandgloryAttack(CallbackInfoReturnable<Boolean> ci){if(CommandMode.confirm())ci.setReturnValue(false);else if(CommandPanel.capturing()){CommandPanel.swallowedKeys++;ci.setReturnValue(false);}} // a keyboard-bound attack never swings while the panel holds the mouse (PanelInputMixin swallows the mouse buttons)
    @Inject(method="doItemUse",at=@At("HEAD"),cancellable=true)
    private void gravesandgloryUse(CallbackInfo ci){if(CommandMode.cancel())ci.cancel();else if(CommandPanel.capturing()){CommandPanel.swallowedKeys++;ci.cancel();}}
}

