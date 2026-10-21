package gg.gravesandglory;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;
/** Unshipped observations only; cadence variant changes test-owned Spec rows, never production files. */
public final class BalanceTelemetry implements ModInitializer {
 final Map<UUID,String> previous=new HashMap<>();final Map<UUID,Integer> switches=new HashMap<>();Path file;int round;
 public void onInitialize(){
  if(System.getProperty("gravesandglory.botSim")==null)return;
  file=FabricLoader.getInstance().getGameDir().resolve("botsim/decisions.csv");
  BotSimulation.append(file,"round,tick,team,lane,tier,living,order,formation,x,z,targetX,targetZ,targetDistance,nearPoint,nearDistance,owners,switches,yaw,label\n");
  String cadence=System.getProperty("gravesandglory.balanceCadence","");
  if(!cadence.isEmpty())for(int i=0;i<Spec.BOTCOMMAND.length;i++){var r=Spec.BOTCOMMAND[i];if(r.id().equals("easy"))Spec.BOTCOMMAND[i]=new Spec.BotCommandRow(r.id(),r.retreatShare(),r.rallyShare(),r.chargeRatio(),Integer.parseInt(cadence),r.flankLeastDefended(),r.defenderWeight(),r.shieldwallVsArchers(),r.archerThreatCount(),r.archerThreatRange(),r.chargeDistance());}
  ServerTickEvents.END_SERVER_TICK.register(server->{var b=GravesAndGlory.battle;if(b==null||!b.running)return;if(b.time-b.roundStarted==1)round++;if(b.time%20!=0)return;
   String owners=String.join(";",b.points.stream().map(p->""+p.owner).toList());
   for(var s:b.squads.values())if(s.bot&&s.seated){
    var mine=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).toList();if(mine.isEmpty())continue;var c=Morale.centre(mine);
    String target=Math.round(s.target.x)+":"+Math.round(s.target.z),old=previous.put(s.owner,target);if(old!=null&&!old.equals(target))switches.merge(s.owner,1,Integer::sum);
    if(b.time%200!=0)continue;int nearest=-1;double dist=Double.MAX_VALUE;for(int i=0;i<b.points.size();i++){var q=b.points.get(i);double d=Math.hypot(c.x-q.spec.x(),c.z-q.spec.z());if(d<dist){dist=d;nearest=i;}}
    BotSimulation.append(file,String.format(Locale.ROOT,"%d,%d,%d,%d,%s,%d,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%s,%d,%.2f,%s\n",round,b.time-b.roundStarted,s.team,s.lane,s.tier,mine.size(),s.order,s.formation,c.x,c.z,s.target.x,s.target.z,Math.hypot(c.x-s.target.x,c.z-s.target.z),nearest,dist,owners,switches.getOrDefault(s.owner,0),s.yaw,System.getProperty("gravesandglory.balanceLabel","baseline")));
   }
  });
 }
}
