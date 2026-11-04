package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/**
 * Server-kit smoke join (task 3.5): connect one disposable dev client to a dedicated server and write a result line.
 * -Dgravesandglory.serverSmoke=host:port  [-Dgravesandglory.serverSmokeCase=ok|build]  [-Dgravesandglory.serverSmokeOut=file]
 * ok: expects to enter the world and stay 100 ticks. build: sends a wrong build hash and expects the version-gate rejection.
 */
public final class ServerKitSmoke implements ClientModInitializer {
    int ticks, inWorld, disconnected;boolean started, reported;
    @Override public void onInitializeClient(){
        String address=System.getProperty("gravesandglory.serverSmoke","");
        if(address.isEmpty())return;
        String mode=System.getProperty("gravesandglory.serverSmokeCase","ok");
        Path out=Path.of(System.getProperty("gravesandglory.serverSmokeOut","server-smoke.txt"));
        if(mode.equals("build"))ClientLoginConnectionEvents.INIT.register((handler,client)->{
            ClientLoginNetworking.unregisterGlobalReceiver(VersionGate.CHANNEL);
            ClientLoginNetworking.registerGlobalReceiver(VersionGate.CHANNEL,(c,h,b,l)->{
                var packet=PacketByteBufs.create();
                packet.writeVarInt(CompatibilityPolicy.PROTOCOL);
                packet.writeString(VersionGate.VERSION);
                packet.writeString("0".repeat(64));
                return CompletableFuture.completedFuture(packet);
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            ticks++;
            if(reported)return;
            if(!started){
                if(ticks>100&&c.getOverlay()==null){
                    started=true;
                    ConnectScreen.connect(new FrontMenuScreen(),c,ServerAddress.parse(address),new ServerInfo("Server kit smoke",address,false),false);
                }
                return;
            }
            String result=null;
            if(c.currentScreen instanceof DisconnectedScreen screen){
                String text=screen.getNarratedTitle().getString();
                if(++disconnected>10)result=(mode.equals("build")&&text.contains("Install the same modpack release")?"PASS ":"FAIL ")+mode+" rejected: "+text;
            }else if(c.player!=null&&c.world!=null&&++inWorld>100)
                result=(mode.equals("ok")?"PASS ":"FAIL ")+mode+" in world as "+c.getSession().getUsername();
            else if(ticks>3600)result="FAIL "+mode+" timeout";
            if(result!=null){
                reported=true;
                try{Files.writeString(out,result+"\n");}catch(Exception e){e.printStackTrace();}
                System.out.println("[server-kit-smoke] "+result);
                c.scheduleStop();
            }
        });
    }
}
