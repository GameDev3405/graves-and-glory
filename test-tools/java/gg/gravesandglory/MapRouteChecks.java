package gg.gravesandglory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.*;

/**
 * Opt-in disposable-lab tour: twenty actual pathfinding mobs over every crossing of the chosen maps.
 * It sets up its own conditions, so a result describes the route and nothing else: a round with no AI commander,
 * one army of twenty knights, and troops placed only after the round intro is over. Whatever came before
 * (the suite's solo battle, the previous map) is sent back to camp first, so no later camp return undoes a placement.
 * Maps come from control.json {"action":"routes","maps":[0,1]}; with no list all five maps 0-4 are toured.
 * Kingsfield (map 4) used to be left out: switching to it from a running client threw in GravesAndGloryClient.map (its HUD indexed the
 * three-entry objective arrays with five points); that was fixed in 6ecbfb0. crossed counts the 20/20 crossings per map (three when a map passes).
 */
final class MapRouteChecks {
    final MinecraftClient client;final int[] maps;final Map<Integer,Integer> crossed=new HashMap<>();
    int index,bridge,phase,retries,crossings;long at;boolean all=true,done;
    Campaign.Profile backup;GravesAndGlory.Squad squad;boolean practice,fill;int size;
    MapRouteChecks(MinecraftClient c,int[] requested){
        client=c;
        maps=requested!=null&&requested.length>0?requested:new int[]{0,1,2,3,4};
    }
    static List<Spec.StructuresRow> bridges(){return Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();}
    void tick(){
        var b=GravesAndGlory.battle;if(b==null||done||client.player==null)return;
        ServerPlayerEntity p=b.server.getPlayerManager().getPlayer(client.player.getUuid());if(p==null)return;
        int map=maps[index];
        if(phase==0){
            // Camp return first: it clears the running round, every troop and any AI commander, and it moves the player.
            if(backup==null){backup=Campaign.JSON.fromJson(Campaign.JSON.toJson(b.campaign.profile(p.getUuid())),Campaign.Profile.class);practice=b.campaign.practice;fill=b.campaign.fill;size=b.campaign.size;}
            b.campaign.returnLobby();
            // Not practice, no bot fill: prepareRound then adds no AI commander to either team.
            b.campaign.practice=false;b.campaign.fill=false;b.campaign.size=1;
            if(b.campaign.map!=map)b.campaign.switchMap(map);
            phase=1;return;
        }
        if(phase==1){
            if(!b.ready||b.campaign.map!=map||CampaignMaps.selected!=map)return; // the arena is still being built
            var profile=b.campaign.profile(p.getUuid());
            for(var t:Spec.TROOPS)if(t.faction().equals("dawn"))profile.army.put(t.id(),t.id().equals("knight")?20:0);
            squad=b.squads.get(p.getUuid());if(squad==null)return;
            squad.team=0;squad.seated=true;
            b.startRound();
            b.squads.values().removeIf(s->s!=squad);
            b.units.removeIf(u->{if(u.squad!=squad){u.mob.discard();return true;}return false;});
            phase=2;return;
        }
        if(!b.running){ // something sent everyone back to camp: set the map up again, up to three times
            if(++retries>3){
                String name="map-"+map+"-setup";MeltyTest.report(name,false,"the round kept ending before the crossings were measured");System.out.println("[Graves and Glory routes] "+name+" FAILED");
                all=false;nextMap(b,p);return;
            }
            phase=0;return;
        }
        if(phase==2){
            if(b.time<b.campaign.introUntil)return; // units do not move before the intro ends, and the player is held at base
            long mine=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()).count(),knights=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()&&u.spec.id().equals("knight")).count(),foreign=b.units.stream().filter(u->u.squad!=squad&&u.mob.isAlive()).count();
            boolean army=mine==20&&knights==20&&foreign==0;all&=army;
            String name="map-"+map+"-setup";MeltyTest.report(name,army,"army="+mine+" knights="+knights+" foreign="+foreign);System.out.println("[Graves and Glory routes] "+name+" army="+mine+" knights="+knights+" foreign="+foreign);
            prepare(b,p);phase=3;return;
        }
        var crossingList=bridges();var route=crossingList.get(bridge);
        long passed=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()&&u.mob.getX()>route.x()+route.width()/2.0+4&&u.mob.getY()>63).count();
        if(passed==20||b.time-at>1200){
            boolean pass=passed==20;all&=pass;crossings++;if(pass)crossed.merge(map,1,Integer::sum);String name="map-"+map+"-bridge-"+bridge;MeltyTest.report(name,pass,"crossed "+passed+"/20");System.out.println("[Graves and Glory routes] "+name+" "+passed+"/20; elapsed="+(b.time-at)/20.0+"s");
            int currentMap=map,currentBridge=bridge;client.execute(()->ScreenshotRecorder.saveScreenshot(client.runDirectory,"route-map-"+currentMap+"-bridge-"+currentBridge+".png",client.getFramebuffer(),text->{}));
            bridge++;if(bridge<crossingList.size()){prepare(b,p);return;}
            nextMap(b,p);
        }
    }
    void nextMap(GravesAndGlory.Battle b,ServerPlayerEntity p){
        bridge=0;retries=0;index++;phase=0;
        if(index<maps.length)return;
        b.campaign.returnLobby();
        b.campaign.profiles.put(p.getUuid(),backup);b.campaign.save(p.getUuid());
        b.campaign.practice=practice;b.campaign.fill=fill;b.campaign.size=size;
        MeltyTest.report("all-map-navigation",all,crossings+" actual twenty-troop crossings over "+maps.length+" maps");
        System.out.println("[Graves and Glory routes] ALL MAPS COMPLETE success="+all+" crossings="+crossings);done=true;
    }
    /** Places the army west of the next crossing, only now that the round is running, and sends it east. */
    void prepare(GravesAndGlory.Battle b,ServerPlayerEntity p){
        var span=bridges().get(bridge);double start=span.x()-span.width()/2.0-14,westEdge=span.x()-span.width()/2.0-3;
        squad.assignments.clear();squad.order="move";squad.formation="line";squad.target=new Vec3d(span.x()+span.width()/2.0+18,65,span.z());squad.yaw=-90;
        for(var u:b.units)if(u.squad==squad){
            Vec3d cell=standingCell(b,start-(u.slot/5)*2,span.z()+(u.slot%5-2)*1.8,westEdge);
            u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(cell.x,cell.y,cell.z,-90,0);u.nextPath=0;u.stuckTicks=0;u.lastPosition=u.mob.getPos();
        }
        p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,start-14,78,span.z()+24,-60,22);at=b.time;
        client.execute(()->client.setScreen(null));
    }
    /** The nearest place to (x,z) a knight can stand: something to stand on, nothing solid at feet or head, no water, and not past maxX. Keeps walls, trees and the river out of the placement. */
    static Vec3d standingCell(GravesAndGlory.Battle b,double x,double z,double maxX){
        int cx=(int)Math.floor(x),cz=(int)Math.floor(z);
        for(int r=0;r<=8;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            int tx=cx+dx,tz=cz+dz,ty=Arena.ground(tx,tz)+1;
            if(tx+.5>maxX||Arena.river(tx,tz))continue;
            var below=b.world.getBlockState(new BlockPos(tx,ty-1,tz));var feet=b.world.getBlockState(new BlockPos(tx,ty,tz));var head=b.world.getBlockState(new BlockPos(tx,ty+1,tz));
            boolean floor=!below.getCollisionShape(b.world,new BlockPos(tx,ty-1,tz)).isEmpty()&&below.getFluidState().isEmpty();
            boolean open=feet.getCollisionShape(b.world,new BlockPos(tx,ty,tz)).isEmpty()&&feet.getFluidState().isEmpty()&&head.getCollisionShape(b.world,new BlockPos(tx,ty+1,tz)).isEmpty()&&head.getFluidState().isEmpty();
            if(floor&&open)return new Vec3d(tx+.5,ty,tz+.5);
        }
        return new Vec3d(cx+.5,Arena.ground(cx,cz)+1,cz+.5);
    }
}
