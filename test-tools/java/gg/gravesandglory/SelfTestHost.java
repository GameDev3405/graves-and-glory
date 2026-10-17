package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Unshipped: -PselfTest (or -PautoHost for route checks) needs a hosted world on map 0 with the player joined; Battle.selfTest() seats squads and starts the round itself. */
public final class SelfTestHost implements ClientModInitializer {
    int ticks;boolean hosted,started;
    @Override public void onInitializeClient(){
        boolean self=Boolean.getBoolean("gravesandglory.selfTest");if(!self&&!Boolean.getBoolean("gravesandglory.autoHost"))return;
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server before the arena is built
            if(c.getOverlay()!=null||started)return;
            if(c.world==null){if(!hosted&&++ticks>60){hosted=true;GravesAndGloryClient.connectOrHost(c);}return;}
            var b=GravesAndGlory.battle;if(b==null||c.getServer()==null||c.player==null||!b.ready)return;
            started=true;if(!self)return;
            c.getServer().execute(()->{if(b.campaign.map!=0){b.campaign.switchMap(0);started=false;}});
        });
    }
}
