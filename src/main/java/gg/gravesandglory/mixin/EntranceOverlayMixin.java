package gg.gravesandglory.mixin;
import gg.gravesandglory.CampaignClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
abstract class EntranceOverlayMixin {
    @Inject(method="render",at=@At("TAIL"))
    void gravesandgloryBars(float delta,long start,boolean tick,CallbackInfo ci){
        if(!CampaignClient.cinematic())return;var c=MinecraftClient.getInstance();
        var g=new DrawContext(c,c.getBufferBuilders().getEntityVertexConsumers());
        CampaignClient.hud(g,c.getWindow().getScaledWidth(),c.getWindow().getScaledHeight());g.draw();
    }
}
