package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/**
 * Harness client that joins a dedicated server on start: -Dgravesandglory.connect=host:port (gradle env GRAVES_ROADMAP_CONNECT).
 * Uses the same ConnectScreen call as the multiplayer list, once the title screen has settled, so any check class that
 * runs "once in the world" (or a CompatibilityChecks refusal case) works against a server. Inert when the property is empty.
 */
public final class ConnectOnStart implements ClientModInitializer {
    int ticks;boolean started;
    @Override public void onInitializeClient(){
        String address=System.getProperty("gravesandglory.connect","");
        if(address.isEmpty())return;
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(started||++ticks<100||c.getOverlay()!=null)return;
            started=true;
            System.out.println("[connect-on-start] connecting to "+address);
            ConnectScreen.connect(new FrontMenuScreen(),c,ServerAddress.parse(address),new ServerInfo("Harness join",address,false),false);
        });
    }
}
