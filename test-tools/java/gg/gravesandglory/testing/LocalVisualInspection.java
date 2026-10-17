package gg.gravesandglory.testing;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.world.GameMode;

/** Unshipped camera inspection of the already-tested world. No player input. */
public final class LocalVisualInspection implements ClientModInitializer {
    int ticks;
    @Override public void onInitializeClient(){
        if(!Boolean.getBoolean("gravesandglory.visualInspection"))return;
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null||c.getServer()==null)return;
            ticks++;c.options.pauseOnLostFocus=false;
            if(ticks==200){c.options.getViewDistance().setValue(24);var id=c.player.getUuid();c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayer(id);if(p!=null){p.changeGameMode(GameMode.SPECTATOR);p.teleport(c.getServer().getOverworld(),0,230,0,0,90);}});}
            if(ticks==210)c.worldRenderer.reload();
            if(ticks==400||ticks==600||ticks==900)ScreenshotRecorder.saveScreenshot(c.runDirectory,"overview-inspection-"+ticks+".png",c.getFramebuffer(),t->{});
        });
    }
}
