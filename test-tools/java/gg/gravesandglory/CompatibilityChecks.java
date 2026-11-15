package gg.gravesandglory;

import gg.gravesandglory.net.Payloads;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.network.PacketByteBuf;

/**
 * Deliberately old/missing handshake responses, confined to disposable development clients.
 *
 * <p>-Dgravesandglory.compatibilityCase=[gate-]case, case one of old, protocol, build, malformed, missing:
 * <ul>
 * <li>no prefix: both the configuration reply (release_check_v2, the 1.21.1 gate) and the login answer (release_check_v1, the
 *     fallback) are wrong, so the client is refused whichever gate the server runs (-Dgravesandglory.loginGate=false or not);</li>
 * <li>{@code config-}: only the configuration reply is wrong and the login answer is honest, so the configuration gate is the one
 *     that refuses, even with the fallback on. The server's request must have reached this client (missing: it must not, since the
 *     channel was never advertised);</li>
 * <li>{@code login-}: only the login answer is wrong (the fallback; the server must keep -Dgravesandglory.loginGate on). The login
 *     query must have reached this client (missing: the channel is not understood).</li>
 * </ul>
 * Each case passes when the client is refused before world admission with the policy text; evidence id compatibility-[gate-]case.
 */
public final class CompatibilityChecks implements ClientModInitializer {
    int ticks, disconnectedTicks;boolean reported;
    volatile boolean loginAsked, configAsked;
    @Override public void onInitializeClient(){
        String mode=System.getProperty("gravesandglory.compatibilityCase","");
        if(mode.isEmpty())return;
        String gate=mode.startsWith("config-")?"config":mode.startsWith("login-")?"login":"both";
        String kind=gate.equals("both")?mode:mode.substring(gate.length()+1);
        boolean badLogin=!gate.equals("config"), badConfig=!gate.equals("login");
        // Login INIT runs after every initializer and before the configuration phase advertises the client's channels, so a removed
        // configuration receiver is really unadvertised (missing), and a replaced one answers the server's request.
        ClientLoginConnectionEvents.INIT.register((handler,client)->{
            ClientLoginNetworking.unregisterGlobalReceiver(VersionGate.CHANNEL);
            if(!(badLogin&&kind.equals("missing")))ClientLoginNetworking.registerGlobalReceiver(VersionGate.CHANNEL,(c,h,b,l)->{
                loginAsked=true;
                return CompletableFuture.completedFuture(badLogin?answer(kind):VersionGate.response());
            });
            ClientConfigurationNetworking.unregisterGlobalReceiver(Payloads.ReleaseCheckRequestPayload.ID);
            if(!(badConfig&&kind.equals("missing")))ClientConfigurationNetworking.registerGlobalReceiver(Payloads.ReleaseCheckRequestPayload.ID,(payload,context)->{
                configAsked=true;
                context.responseSender().sendPacket(Payloads.ReleaseCheckReplyPayload.of(badConfig?answer(kind):VersionGate.response()));
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            ticks++;
            if(client.currentScreen instanceof DisconnectedScreen screen){
                if(!reported){
                    String text=screen.getNarratedTitle().getString();
                    boolean refused=text.contains("requires Graves and Glory "+VersionGate.VERSION)
                            &&text.contains("Install the same modpack release")&&client.player==null;
                    // Which gate refused: the request of the gate under test reached this client (or, for missing, never could).
                    boolean byGate=switch(gate){
                        case "config"->kind.equals("missing")?!configAsked:configAsked;
                        case "login"->kind.equals("missing")||loginAsked;
                        default->true;
                    };
                    boolean pass=refused&&byGate;
                    String detail=text+" [gate="+gate+" loginAsked="+loginAsked+" configAsked="+configAsked+"]";
                    MeltyTest.report("compatibility-"+mode,pass,detail);
                    System.out.println("[Graves and Glory compatibility fixture] "+mode+"="+pass+" "+detail);
                    reported=true;
                }
                if(++disconnectedTicks==10)ScreenshotRecorder.saveScreenshot(client.runDirectory,"compatibility-"+mode+".png",client.getFramebuffer(),t->{});
                if(disconnectedTicks==20)client.scheduleStop();
            }else if(ticks>2400){MeltyTest.report("compatibility-"+mode,false,"No expected rejection within timeout [gate="+gate+" loginAsked="+loginAsked+" configAsked="+configAsked+"]");client.scheduleStop();}
        });
    }

    /** A wrong answer body for the case (the same bytes for the login answer and the configuration reply). */
    static PacketByteBuf answer(String kind){
        var packet=PacketByteBufs.create();
        if(!kind.equals("malformed")){
            packet.writeVarInt(kind.equals("protocol")?CompatibilityPolicy.PROTOCOL-1:CompatibilityPolicy.PROTOCOL);
            packet.writeString(kind.equals("old")?"0.0.0-old":VersionGate.VERSION);
            packet.writeString(kind.equals("build")?"0".repeat(64):VersionGate.BUILD);
        }
        return packet;
    }
}
