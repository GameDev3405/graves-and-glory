package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Prop alive effects (prop kit plan task S5): the forge smokes in every 20-tick window; the anvil rings 5-15 times in 10 000 ticks; the
 *  same seed gives the same sequence and different seeds desync; effects fire only on ticker steps; effect points turn with facing. */
public final class PropEffectsCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static boolean near(double a,double b){return Math.abs(a-b)<1e-9;}
 static PropDef def(String id)throws Exception{return PropDef.parse(Files.readString(Path.of("src/main/resources/data/gravesandglory/gg_props/"+id+".json")));}
 static int fires(PropDef d,String effectId,long seed,long from,long to){
  int n=0;for(long t=from;t<to;t++)for(PropDef.Effect e:PropEffects.due(d,t,seed))if(e.id().equals(effectId))n++;
  return n;
 }

 public static void main(String[] args)throws Exception{
  PropDef forge=def("forge"),anvil=def("anvil");
  long[] seeds={PropEffects.seed(0,64,0),PropEffects.seed(12,70,-40),PropEffects.seed(-300,5,812),PropEffects.seed(1,1,1),PropEffects.seed(1000,100,1000)};
  check(forge.effects().stream().anyMatch(e->e.id().equals("minecraft:large_smoke")),"forge has chimney smoke");
  check(anvil.effects().stream().anyMatch(e->e.id().equals("minecraft:block.anvil.use")),"anvil has the anvil ring");
  for(long s:seeds)for(long start=0;start<2000;start++)
   check(fires(forge,"minecraft:large_smoke",s,start,start+20)>=1,"forge smokes in every 20-tick window (seed "+s+", from "+start+")");
  int total=0;
  for(long s:seeds){
   int n=fires(anvil,"minecraft:block.anvil.use",s,0,10000);
   check(n>=5&&n<=15,"anvil rings 5-15 times over 10 000 ticks, seed "+s+": "+n);
  }
  for(int i=0;i<200;i++)total+=fires(anvil,"minecraft:block.anvil.use",PropEffects.seed(i*7,64,i*-13),0,10000);
  check(total>=200*8&&total<=200*12,"anvil mean over 200 anvils near 10 per 10 000 ticks: "+total/200.0);
  for(long s:seeds){
   List<String> a=new ArrayList<>(),b=new ArrayList<>();
   for(long t=0;t<4000;t++){for(PropDef.Effect e:PropEffects.due(forge,t,s))a.add(t+e.id());for(PropDef.Effect e:PropEffects.due(forge,t,s))b.add(t+e.id());}
   check(a.equals(b)&&!a.isEmpty(),"same seed gives the same sequence");
  }
  Set<String> sparkRuns=new HashSet<>();
  for(int i=0;i<10;i++){
   StringBuilder sb=new StringBuilder();long s=PropEffects.seed(i*3,64,0);
   for(long t=0;t<2000;t++)for(PropDef.Effect e:PropEffects.due(forge,t,s))if(e.id().equals("minecraft:lava"))sb.append(t).append(',');
   sparkRuns.add(sb.toString());
  }
  check(sparkRuns.size()>=8,"different seeds desync the forge sparks: "+sparkRuns.size()+" distinct of 10");
  for(long t=0;t<1000;t++)if(t%PropEffects.STEP!=0)check(PropEffects.due(forge,t,seeds[0]).isEmpty()&&PropEffects.due(anvil,t,seeds[0]).isEmpty(),"effects fire only on ticker steps, tick "+t);
  check(PropEffects.due(def("weapon_rack"),0,seeds[0]).isEmpty(),"a prop without effects is silent");
  for(PropDef.Effect e:forge.effects())for(int f:new int[]{0,90,180,270}){
   double[] p=PropEffects.at(forge,f,e),q=PropGeometry.point(forge,f,e.x(),e.y(),e.z());
   check(near(p[0],q[0])&&near(p[1],q[1])&&near(p[2],q[2]),"effect point turns with facing "+f+" ("+e.id()+")");
  }
  PropDef.Effect smoke=forge.effects().stream().filter(e->e.id().equals("minecraft:large_smoke")).findFirst().get();
  double[] s0=PropEffects.at(forge,0,smoke),s90=PropEffects.at(forge,90,smoke);
  check(!(near(s0[0],s90[0])&&near(s0[2],s90[2])),"the chimney smoke moves when the forge turns 90");
  check(near(s0[0],1.5-1+0)&&near(s0[2],1.5),"forge facing 0: chimney top sits over cell (1,3,1), offset (0.5,_,1.5) from the anchor cell (1,0,0) corner");
  System.out.println("PASS "+count+" prop effect scenarios");
 }
}
