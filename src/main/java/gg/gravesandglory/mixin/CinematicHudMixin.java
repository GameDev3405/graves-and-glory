package gg.gravesandglory.mixin;
import gg.gravesandglory.CampaignClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(InGameHud.class)
abstract class CinematicHudMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    void gravesandgloryCinematic(DrawContext g,float delta,CallbackInfo ci){
        if(gg.gravesandglory.GravesAndGloryClient.menuVisible()){ci.cancel();return;}
        if(gg.gravesandglory.EndingClient.hudActive()){var c=MinecraftClient.getInstance();gg.gravesandglory.EndingClient.hud(g,c.getWindow().getScaledWidth(),c.getWindow().getScaledHeight());ci.cancel();return;}
        if(CampaignClient.cinematic()){var c=MinecraftClient.getInstance();CampaignClient.hud(g,c.getWindow().getScaledWidth(),c.getWindow().getScaledHeight());ci.cancel();}
    }
    @Inject(method={"renderHotbar","renderStatusBars","renderExperienceBar","renderMountHealth"},at=@At("HEAD"),cancellable=true)
    void gravesandgloryCamp(CallbackInfo ci){if(gg.gravesandglory.GravesAndGloryClient.inCamp())ci.cancel();}
}
