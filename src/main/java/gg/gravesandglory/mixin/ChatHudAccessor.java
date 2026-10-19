package gg.gravesandglory.mixin;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Vanilla's private ChatHud.getLineHeight, which ChatHudMixin's line cut and the checks call, so neither keeps a copy of it. */
@Mixin(ChatHud.class)
public interface ChatHudAccessor {
    /** ChatHud.getLineHeight: one chat line's height in the chat's scaled space, from the Line Spacing option. */
    @Invoker("getLineHeight") int gravesandglory$getLineHeight();
}
