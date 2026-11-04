package gg.gravesandglory;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.util.ScreenshotRecorder;

/** Deliberately old/missing handshake responses, confined to disposable development clients. */
public final class CompatibilityChecks implements ClientModInitializer {
    int ticks, disconnectedTicks;boolean reported;
    @Override public void onInitializeClient(){
        String mode=System.getProperty("gravesandglory.compatibilityCase","");
        if(mode.isEmpty())return;
        ClientLoginConnectionEvents.INIT.register((handler,client)->{
            ClientLoginNetworking.unregisterGlobalReceiver(VersionGate.CHANNEL);
            if(!mode.equals("missing"))ClientLoginNetworking.registerGlobalReceiver(VersionGate.CHANNEL,(c,h,b,l)->{
                var packet=PacketByteBufs.create();
                if(!mode.equals("malformed")){
                    packet.writeVarInt(mode.equals("protocol")?CompatibilityPolicy.PROTOCOL-1:CompatibilityPolicy.PROTOCOL);
                    packet.writeString(mode.equals("old")?"0.0.0-old":VersionGate.VERSION);
                    packet.writeString(mode.equals("build")?"0".repeat(64):VersionGate.BUILD);
                }
                return CompletableFuture.completedFuture(packet);
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            ticks++;
            if(client.currentScreen instanceof DisconnectedScreen screen){
                if(!reported){
                    String text=screen.getNarratedTitle().getString();
                    boolean pass=text.contains("requires Graves and Glory "+VersionGate.VERSION)
                            &&text.contains("Install the same modpack release")&&client.player==null;
                    MeltyTest.report("compatibility-"+mode,pass,text);
                    System.out.println("[Graves and Glory compatibility fixture] "+mode+"="+pass+" "+text);
                    reported=true;
                }
                if(++disconnectedTicks==10)ScreenshotRecorder.saveScreenshot(client.runDirectory,"compatibility-"+mode+".png",client.getFramebuffer(),t->{});
                if(disconnectedTicks==20)client.scheduleStop();
            }else if(ticks>2400){MeltyTest.report("compatibility-"+mode,false,"No expected rejection within timeout");client.scheduleStop();}
        });
    }
}
