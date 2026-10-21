package gg.gravesandglory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;

/** Actual20TPS battle observation, same fixture for baseline and candidate; no gameplay acceleration property. */
final class BotBalanceVisualChecks implements Phase1Checks.Suite,Phase1Checks.ServerWatch {
 static final int[] MAPS={0,4};
 volatile int stage,mapIndex,clientTicks,requestedAt;volatile String requested="";volatile boolean captured,mapView;
 UUID player;String clientRequest="";int shot;long start;boolean configured;
 final Map<UUID,Vec3d> last=new HashMap<>();
 public void step(MinecraftClient c,int tick){
  clientTicks=tick;if(!configured){configured=true;player=c.player.getUuid();Phase1Checks.window(c,1280,720,2);c.options.getMaxFps().setValue(40);c.options.pauseOnLostFocus=false;c.setScreen(null);stage=1;}
  if(tick>7000){Phase1Checks.report("bot-visual-timeout",false,"stage "+stage+", map "+mapIndex);Phase1Checks.finish(c,"bots-visual");return;}
  if(!requested.isEmpty()&&!captured){
   if(!requested.equals(clientRequest)){clientRequest=requested;requestedAt=tick;c.setScreen(mapView?new GravesAndGloryClient.MapScreen(null):null);}
   if(tick-requestedAt>=12){Phase1Checks.shot(c,"bots-"+requested);captured=true;}
  }
  if(stage==4){Phase1Checks.finish(c,"bots-visual");stage=5;}
 }
 public void serverTick(GravesAndGlory.Battle b){
  if(stage==1){var p=b.server.getPlayerManager().getPlayer(player);if(p==null||!Phase1Checks.skirmish(b,p,MAPS[mapIndex],3,"normal"))return;
   for(var u:new ArrayList<>(b.units))u.mob.discard();b.units.clear();
   for(var s:b.squads.values())if(s.seated){s.bot=true;s.tier=s.team==0?"hard":"normal";s.banner=Campaign.bannerRow("gilded_normal");s.commander=Spec.CLASSES[s.lane%Spec.CLASSES.length].id();s.fullHealth.clear();b.reinforce(s);}
   p.changeGameMode(GameMode.SPECTATOR);b.campaign.introUntil=b.time;b.campaign.leash=false;start=b.time;shot=0;requested="";captured=false;stage=2;return;
  }
  if(stage!=2)return;long elapsed=b.time-start;
  if(elapsed%100==0){
   Path evidence=Path.of(System.getProperty("gravesandglory.localChecks")).getParent().resolve("visual-observations.csv");
   if(!Files.exists(evidence))BotSimulation.append(evidence,"map,second,team,living,unitsInCapture,stationaryAwayFromFight,score\n");
   for(int team=0;team<2;team++){final int side=team;var units=b.units.stream().filter(u->u.squad.team==side&&u.mob.isAlive()).toList();int on=0,idle=0;
    for(var u:units){Vec3d pos=u.mob.getPos(),previous=last.put(u.mob.getUuid(),u.mob.getPos());boolean near=false;for(var point:b.points)if(Math.hypot(pos.x-point.spec.x(),pos.z-point.spec.z())<=point.spec.radius())near=true;if(near)on++;
     if(previous!=null&&previous.squaredDistanceTo(pos)<1&&!near&&(u.lockedTarget==null||!u.lockedTarget.isAlive())&&!u.squad.order.equals("retreat"))idle++;}
    BotSimulation.append(evidence,String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%d,%d\n",MAPS[mapIndex],elapsed/20,team,units.size(),on,idle,b.scores[team]));
   }
  }
  if(!requested.isEmpty()){if(!captured)return;requested="";shot++;}
  if(shot<3&&elapsed>=(shot+1)*600){
   mapView=shot==1;var p=b.server.getPlayerManager().getPlayer(player);
   if(!mapView){var army=b.squads.values().stream().filter(s->s.bot&&s.team==0&&s.lane==0).findFirst().orElseThrow();
    var front=b.units.stream().filter(u->u.squad==army&&u.mob.isAlive()).sorted(Comparator.comparingDouble(u->u.mob.squaredDistanceTo(army.target))).limit(8).toList();
    Vec3d look=front.isEmpty()?army.target:Morale.centre(front);double x=look.x+22,z=look.z+30,y=Math.max(look.y,Arena.ground((int)x,(int)z)+1)+16;
    float yaw=BotPlannerYaw(look.x-x,look.z-z),pitch=(float)Math.toDegrees(Math.atan2(y+p.getStandingEyeHeight()-look.y,Math.hypot(x-look.x,z-look.z)));
    b.relocate(p,x,y,z,yaw,pitch);
   }
   captured=false;requested="map"+MAPS[mapIndex]+"-"+((shot+1)*30)+"s-"+(mapView?"map":"scene");return;
  }
  if(shot>=3&&elapsed>=1880){Phase1Checks.report("bot-visual-map"+MAPS[mapIndex],b.scores[0]+b.scores[1]>0&&b.units.size()>0,"normal20TPS battle seen at30,60,90s, scores "+Arrays.toString(b.scores)+", living "+b.units.size());
   var own=b.squads.get(player);if(own!=null)own.bot=false;b.campaign.returnLobby();b.campaign.leash=true;last.clear();
   if(++mapIndex<MAPS.length)stage=1;else stage=4;
  }
 }
 static float BotPlannerYaw(double dx,double dz){return (float)Math.toDegrees(Math.atan2(-dx,dz));}
 public void cleanup(MinecraftClient c){Phase1Checks.server(c,b->{var own=b.squads.get(player);if(own!=null)own.bot=false;b.campaign.leash=true;});}
}
