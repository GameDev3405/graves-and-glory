package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import gg.gravesandglory.net.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/**
 * Server-kit smoke join (task 3.5): connect one disposable dev client to a dedicated server and write a result line.
 * -Dgravesandglory.serverSmoke=host:port  [-Dgravesandglory.serverSmokeCase=ok|build|config-build]  [-Dgravesandglory.serverSmokeOut=file]
 * ok: expects the configuration check (release_check_v2) to run, then to enter the world and stay 100 ticks. build: sends a wrong
 * build hash to both gates and expects the version-gate rejection. config-build: wrong build in the configuration reply only (the
 * login fallback answered honestly) and expects the configuration gate's rejection.
 */
public final class ServerKitSmoke implements ClientModInitializer {
    int ticks, inWorld, disconnected;boolean started, reported;volatile boolean configAsked;
    @Override public void onInitializeClient(){
        String address=System.getProperty("gravesandglory.serverSmoke","");
        if(address.isEmpty())return;
        String mode=System.getProperty("gravesandglory.serverSmokeCase","ok");
        Path out=Path.of(System.getProperty("gravesandglory.serverSmokeOut","server-smoke.txt"));
        // Every case swaps the answers in at login INIT (after all initializers, before the configuration phase advertises channels).
        // build: both the login answer (fallback) and the configuration reply (release_check_v2) carry a wrong build, so either gate refuses.
        // config-build: only the configuration reply is wrong, so the configuration gate refuses even with the login fallback on.
        // ok: both answers are honest; the configuration request must have arrived (the 1.21.1 gate ran) before the client is in the world.
        boolean badLogin=mode.equals("build"), badConfig=mode.equals("build")||mode.equals("config-build");
        ClientLoginConnectionEvents.INIT.register((handler,client)->{
            ClientLoginNetworking.unregisterGlobalReceiver(VersionGate.CHANNEL);
            ClientLoginNetworking.registerGlobalReceiver(VersionGate.CHANNEL,(c,h,b,l)->CompletableFuture.completedFuture(badLogin?wrongBuild():VersionGate.response()));
            ClientConfigurationNetworking.unregisterGlobalReceiver(Payloads.ReleaseCheckRequestPayload.ID);
            ClientConfigurationNetworking.registerGlobalReceiver(Payloads.ReleaseCheckRequestPayload.ID,(payload,context)->{
                configAsked=true;
                context.responseSender().sendPacket(Payloads.ReleaseCheckReplyPayload.of(badConfig?wrongBuild():VersionGate.response()));
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            ticks++;
            if(reported)return;
            if(!started){
                if(ticks>100&&c.getOverlay()==null){
                    started=true;
                    ConnectScreen.connect(new FrontMenuScreen(),c,ServerAddress.parse(address),new ServerInfo("Server kit smoke",address,ServerInfo.ServerType.OTHER),false,null);
                }
                return;
            }
            String result=null;
            if(c.currentScreen instanceof DisconnectedScreen screen){
                String text=screen.getNarratedTitle().getString();
                boolean byGate=!mode.equals("config-build")||configAsked;
                if(++disconnected>10)result=(!mode.equals("ok")&&byGate&&text.contains("Install the same modpack release")?"PASS ":"FAIL ")+mode+" rejected (configAsked="+configAsked+"): "+text;
            }else if(c.player!=null&&c.world!=null&&++inWorld>100)
                result=(mode.equals("ok")&&configAsked?"PASS ":"FAIL ")+mode+" in world as "+c.getSession().getUsername()+" (configAsked="+configAsked+")";
            else if(ticks>3600)result="FAIL "+mode+" timeout";
            if(result!=null){
                reported=true;
                try{Files.writeString(out,result+"\n");}catch(Exception e){e.printStackTrace();}
                System.out.println("[server-kit-smoke] "+result);
                c.scheduleStop();
            }
        });
    }

    /** The current protocol and version with a wrong build hash. */
    static net.minecraft.network.PacketByteBuf wrongBuild(){
        var packet=PacketByteBufs.create();
        packet.writeVarInt(CompatibilityPolicy.PROTOCOL);
        packet.writeString(VersionGate.VERSION);
        packet.writeString("0".repeat(64));
        return packet;
    }
}
