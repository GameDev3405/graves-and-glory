package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Moving prop parts (docs/props/MOVING-PARTS.md): defs without motion stay at rest; the great banner's cloth sways about its crossbar, never
 *  behind its rest plane (the pole), with its pivot fixed; the entity-frame pose matches the same turn done in the prop frame; anchors desync;
 *  each prop sends one keyframe per KEY window; bad motion entries are refused. */
public final class PropMotionCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static PropDef def(String id)throws Exception{return PropDef.parse(Files.readString(Path.of("src/main/resources/data/gravesandglory/gg_props/"+id+".json")));}
 static boolean refused(String json){try{PropDef.parse(json);return false;}catch(IllegalArgumentException e){return true;}}

 /** Model point P turned by angle a (deg) about prop axis (0..2, sign) through Q, in the prop frame (px). */
 static double[] turnInProp(double[] P,int axis,int sign,double[] Q,double a){
  double r=Math.toRadians(a)*sign,c=Math.cos(r),s=Math.sin(r);
  double x=P[0]-Q[0],y=P[1]-Q[1],z=P[2]-Q[2];
  double[] o=switch(axis){case 0->new double[]{x,c*y-s*z,s*y+c*z};case 1->new double[]{c*x+s*z,y,-s*x+c*z};default->new double[]{c*x-s*y,s*x+c*y,z};};
  return new double[]{o[0]+Q[0],o[1]+Q[1],o[2]+Q[2]};
 }

 public static void main(String[] args)throws Exception{
  long seed=PropEffects.seed(10,64,-3);
  try(Stream<Path> files=Files.list(Path.of("src/main/resources/data/gravesandglory/gg_props"))){
   for(Path f:(Iterable<Path>)files.filter(p->p.toString().endsWith(".json"))::iterator){
    PropDef d=PropDef.parse(Files.readString(f));
    if(d.id().equals("great_banner"))continue;
    check(d.motion().isEmpty()&&!PropMotion.moves(d),d.id()+" has no motion");
    for(PropDef.Part p:d.parts())check(java.util.Arrays.equals(PropMotion.pose(d,p,1234,seed),PropMotion.REST),d.id()+" part "+p.model()+" rests");
   }
  }
  PropDef b=def("great_banner");
  PropDef.Part pole=b.parts().get(0),cloth=b.parts().get(2);
  check(cloth.model().equals("great_banner_cloth")&&b.motion().size()==2,"the banner's cloth has two motion entries");
  check(!PropMotion.moves(b,pole)&&PropMotion.moves(b,cloth),"only the cloth moves");
  check(java.util.Arrays.equals(PropMotion.pose(b,pole,500,seed),PropMotion.REST),"the pole rests");

  // one entry alone: the entity-frame pose moves points exactly as the turn in the prop frame, mapped by R(P)
  PropDef.Motion m=b.motion().get(0);
  PropDef one=new PropDef(b.id(),b.category(),b.name(),b.sx(),b.sy(),b.sz(),b.parts(),b.solid(),b.lights(),b.effects(),null,java.util.List.of(m));
  double[][] pts={{8,32,5},{-6,40,4},{22,79,6},{8,80,6}};
  for(int t=0;t<200;t+=7){
   float[] pose=PropMotion.pose(one,cloth,t,seed);double a=PropMotion.angle(m,0,t,seed);
   for(double[] P:pts){
    double[] want=turnInProp(P,m.axis(),m.sign(),new double[]{m.px(),m.py(),m.pz()},a);
    double[] got=PropMotion.apply(pose,cloth,P[0],P[1],P[2]),wantE=PropMotion.entityPoint(cloth,want[0],want[1],want[2]);
    for(int i=0;i<3;i++)check(Math.abs(got[i]-wantE[i])<1e-4,"pose matches the prop-frame turn at t="+t+" point "+java.util.Arrays.toString(P)+" axis "+i+": "+got[i]+" vs "+wantE[i]);
   }
  }

  // the composed sway: the pivot holds, the hem only ever moves forward (north, away from the pole), by a gentle amount, and it moves
  double[] hem=PropMotion.entityPoint(cloth,8,32,5),top=PropMotion.entityPoint(cloth,8,80,6);
  double minFwd=1e9,maxFwd=-1e9,maxSide=0;float[] first=PropMotion.pose(b,cloth,0,seed);boolean changed=false;
  for(int t=0;t<2000;t++){
   float[] pose=PropMotion.pose(b,cloth,t,seed);
   double[] h=PropMotion.apply(pose,cloth,8,32,5),q=PropMotion.apply(pose,cloth,8,80,6);
   check(Math.hypot(q[0]-top[0],Math.hypot(q[1]-top[1],q[2]-top[2]))<0.05,"the cloth's top centre stays at the crossbar, t="+t);
   double fwd=(h[2]-hem[2])*16; // entity z grows towards the front (north) of the prop
   minFwd=Math.min(minFwd,fwd);maxFwd=Math.max(maxFwd,fwd);maxSide=Math.max(maxSide,Math.abs(h[0]-hem[0])*16);
   if(!java.util.Arrays.equals(pose,first))changed=true;
  }
  check(changed,"the cloth pose changes over time");
  check(minFwd>0.2,"the hem never swings back into the pole: least forward "+minFwd+" px");
  check(maxFwd<7&&maxFwd>3,"the hem swings out a gentle 3..7 px: most "+maxFwd+" px");
  check(maxSide>0.8&&maxSide<3,"the hem drifts sideways 0.8..3 px: most "+maxSide+" px");

  // desync and keyframe slots
  long other=PropEffects.seed(11,64,-3);
  check(!java.util.Arrays.equals(PropMotion.pose(b,cloth,300,seed),PropMotion.pose(b,cloth,300,other)),"neighbouring banners are not in step");
  for(long s:new long[]{seed,other,PropEffects.seed(-500,3,77)}){
   for(long w0=0;w0<400;w0+=PropMotion.KEY){
    int n=0;for(long t=w0;t<w0+PropMotion.KEY;t+=PropEffects.STEP)if(PropMotion.keyframe(t,s))n++;
    check(n==1,"one keyframe per "+PropMotion.KEY+"-tick window, seed "+s+" from "+w0+": "+n);
   }
  }

  // kinds
  PropDef.Motion rot=new PropDef.Motion("x","rotate",1,1,8,8,8,0,0,80,0),swing=new PropDef.Motion("x","swing",0,1,8,8,8,30,-10,100,0);
  check(Math.abs(PropMotion.angle(rot,0,20,seed)-PropMotion.angle(rot,0,0,seed)-90)<1e-6||Math.abs(PropMotion.angle(rot,0,20,seed)-PropMotion.angle(rot,0,0,seed)+270)<1e-6,"rotate turns a quarter per quarter period");
  double lo=1e9,hi=-1e9;for(int t=0;t<400;t++){double a=PropMotion.angle(swing,0,t,seed);lo=Math.min(lo,a);hi=Math.max(hi,a);}
  check(lo>=-40.001&&hi<=20.001&&hi-lo>59,"swing stays within bias +- amplitude: "+lo+".."+hi);

  // refusals
  String head="{\"id\":\"t\",\"category\":\"c\",\"name\":\"T\",\"size\":[1,1,1],\"parts\":[{\"model\":\"t\",\"cmd\":7319990}],\"solid\":[],\"motion\":[";
  check(!refused(head+"{\"part\":\"t\",\"kind\":\"sway\",\"axis\":\"-y\",\"pivot\":[8,8,8],\"amplitude\":5,\"period\":40}]}"),"a good entry parses");
  check(refused(head+"{\"part\":\"u\",\"kind\":\"sway\",\"axis\":\"x\",\"pivot\":[8,8,8],\"amplitude\":5,\"period\":40}]}"),"an unknown part is refused");
  check(refused(head+"{\"part\":\"t\",\"kind\":\"wobble\",\"axis\":\"x\",\"pivot\":[8,8,8],\"amplitude\":5,\"period\":40}]}"),"an unknown kind is refused");
  check(refused(head+"{\"part\":\"t\",\"kind\":\"sway\",\"axis\":\"w\",\"pivot\":[8,8,8],\"amplitude\":5,\"period\":40}]}"),"a bad axis is refused");
  check(refused(head+"{\"part\":\"t\",\"kind\":\"sway\",\"axis\":\"x\",\"pivot\":[8,8],\"amplitude\":5,\"period\":40}]}"),"a short pivot is refused");
  check(refused(head+"{\"part\":\"t\",\"kind\":\"sway\",\"axis\":\"x\",\"pivot\":[8,8,8],\"amplitude\":5,\"period\":10}]}"),"a period under two keyframes is refused");
  check(refused(head+"{\"part\":\"t\",\"kind\":\"rotate\",\"axis\":\"x\",\"pivot\":[8,8,8],\"period\":30}]}"),"a rotate of a quarter turn or more per keyframe is refused");
  check(refused(head+"{\"part\":\"t\",\"kind\":\"sway\",\"axis\":\"x\",\"pivot\":[8,8,8],\"amplitude\":0,\"period\":40}]}"),"a sway without amplitude is refused");
  System.out.println("PropMotionCheck: "+count+" checks pass; hem forward "+String.format("%.2f..%.2f",minFwd,maxFwd)+" px, sideways up to "+String.format("%.2f",maxSide)+" px");
 }
}
