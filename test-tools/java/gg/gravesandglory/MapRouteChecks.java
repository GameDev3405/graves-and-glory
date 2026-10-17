package gg.gravesandglory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.*;

/** Opt-in disposable-lab tour: twenty actual pathfinding mobs over twelve crossings. */
final class MapRouteChecks {
    final MinecraftClient client;int map=0,bridge=0,phase=0;long at;boolean all=true,done;Campaign.Profile backup;GravesAndGlory.Squad squad;
    MapRouteChecks(MinecraftClient c){client=c;}
    void tick(){
        var b=GravesAndGlory.battle;if(b==null||done||!b.ready)return;ServerPlayerEntity p=b.server.getPlayerManager().getPlayer(client.player.getUuid());
        if(phase==0){
            if(backup==null){backup=Campaign.JSON.fromJson(Campaign.JSON.toJson(b.campaign.profile(p.getUuid())),Campaign.Profile.class);b.campaign.returnLobby();b.campaign.handle(p,"map:0");if(!b.ready)return;}
            var profile=b.campaign.profile(p.getUuid());for(var t:Spec.TROOPS)if(t.faction().equals("dawn"))profile.army.put(t.id(),t.id().equals("knight")?20:0);
            b.campaign.handle(p,"mode:ai");b.startRound();b.campaign.introUntil=b.time;
            b.squads.values().removeIf(s->s.bot);b.units.removeIf(u->{if(u.squad.bot){u.mob.discard();return true;}return false;});squad=b.squads.get(p.getUuid());
            bridge=0;prepare(b,p);phase=1;return;
        }
        var crossings=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();var route=crossings.get(bridge);
        long passed=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()&&u.mob.getX()>route.x()+route.width()/2.0+4&&u.mob.getY()>63).count();
        if(passed==20||b.time-at>1200){
            boolean pass=passed==20;all&=pass;String name="map-"+map+"-bridge-"+bridge;MeltyTest.report(name,pass,"crossed "+passed+"/20");System.out.println("[Graves and Glory routes] "+name+" "+passed+"/20; elapsed="+(b.time-at)/20.0+"s");
            int currentMap=map,currentBridge=bridge;client.execute(()->ScreenshotRecorder.saveScreenshot(client.runDirectory,"route-map-"+currentMap+"-bridge-"+currentBridge+".png",client.getFramebuffer(),text->{}));
            bridge++;if(bridge<crossings.size()){prepare(b,p);return;}
            b.campaign.returnLobby();map++;if(map<4){b.campaign.handle(p,"map:"+map);phase=0;}
            else{b.campaign.profiles.put(p.getUuid(),backup);b.campaign.save(p.getUuid());b.campaign.handle(p,"mode:4");MeltyTest.report("all-map-navigation",all,"Twelve actual twenty-troop crossings");System.out.println("[Graves and Glory routes] ALL MAPS COMPLETE success="+all);done=true;}
        }
    }
    void prepare(GravesAndGlory.Battle b,ServerPlayerEntity p){
        var bridgeSpec=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList().get(bridge);double start=bridgeSpec.x()-bridgeSpec.width()/2.0-14;
        squad.assignments.clear();squad.order="move";squad.formation="line";squad.target=new Vec3d(bridgeSpec.x()+bridgeSpec.width()/2.0+18,65,bridgeSpec.z());squad.yaw=-90;
        for(var u:b.units)if(u.squad==squad){double x=start-(u.slot/5)*2,z=bridgeSpec.z()+(u.slot%5-2)*1.8;u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(x,Arena.ground((int)x,(int)z)+1,z,-90,0);u.nextPath=0;u.stuckTicks=0;}
        p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,start-14,78,bridgeSpec.z()+24,-60,22);at=b.time;
        client.execute(()->client.setScreen(null));
    }
}
