package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Moving prop parts (docs/props/MOVING-PARTS.md): defs without motion stay at rest; the great banner's cloth sways about its crossbar, never
 *  behind its rest plane (the pole), with its pivot fixed; the entity-frame pose matches the same turn done in the prop frame; anchors desync;
 *  each prop sends one keyframe per KEY window; bad motion entries are refused. */
public final class PropMotionCheck {
 static final java.util.Set<String> MOVING=java.util.Set.of("great_banner","spear_standard","tent_pavilion","tent_pavilion_large","tent_pavilion_large_dawn",
  "tent_pavilion_large_dusk","tent_pavilion_large_weathered","trebuchet","cook_spit");
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

 static PropDef.Part part(PropDef d,String model){for(PropDef.Part p:d.parts())if(p.model().equals(model))return p;throw new AssertionError(d.id()+" has no part "+model);}
 /** Where model point P (px) of part p lands under pose, in model px of the prop at facing 0 (the part entity sits at the anchor cell
  *  centre plus its offset; the entity frame mirrors x and z, see PropMotion). */
 static double[] world(float[] pose,PropDef.Part p,double x,double y,double z){
  double[] l=PropMotion.apply(pose,p,x,y,z);
  return new double[]{8+p.ox()-16*l[0],8+p.oy()+16*l[1],8+p.oz()-16*l[2]};
 }
 static double dist(double[] a,double[] b){return Math.sqrt((a[0]-b[0])*(a[0]-b[0])+(a[1]-b[1])*(a[1]-b[1])+(a[2]-b[2])*(a[2]-b[2]));}

 /** Pennants (spear_standard, tent_pavilion, the large pavilions): the hoist stays on the pole, the fly flutters a few px, neighbours are
  *  out of step, and world() is exact at rest. */
 static void pennants()throws Exception{
  // {id, part, pole x, pole z, hoist y bottom, hoist y top, tip x}
  Object[][] all={{"spear_standard","spear_standard_pennant",4,8,24,36,18},{"tent_pavilion","tent_pavilion_27",8,8,102,112,31},
   {"tent_pavilion_large","tent_pavilion_large_88",8,8,160,176,44},{"tent_pavilion_large_dawn","tent_pavilion_large_dawn_88",8,8,160,176,44},
   {"tent_pavilion_large_dusk","tent_pavilion_large_dusk_88",8,8,160,176,44},{"tent_pavilion_large_weathered","tent_pavilion_large_weathered_88",8,8,160,176,44}};
  for(Object[] r:all){
   PropDef d=def((String)r[0]);PropDef.Part cloth=part(d,(String)r[1]);
   int px=(int)r[2],pz=(int)r[3],y0=(int)r[4],y1=(int)r[5],tip=(int)r[6];
   check(d.motion().size()==3&&d.motion().stream().allMatch(m->m.part().equals(cloth.model())),d.id()+": three entries, all on the pennant");
   for(PropDef.Part p:d.parts())if(p!=cloth)check(!PropMotion.moves(d,p),d.id()+" part "+p.model()+" does not move");
   check(dist(world(PropMotion.REST,cloth,tip,y1,pz),new double[]{tip,y1,pz})<1e-6,d.id()+": world() is exact at rest");
   long seed=PropEffects.seed(3,70,-9);double hoist=0,most=0;
   double mid=(y0+y1)/2.0;
   for(int t=0;t<3000;t+=3){
    float[] pose=PropMotion.pose(d,cloth,t,seed);
    for(int y=y0;y<=y1;y++){double[] a=world(pose,cloth,px,y,pz);hoist=Math.max(hoist,Math.hypot(a[0]-px,a[2]-pz));}
    most=Math.max(most,dist(world(pose,cloth,tip,mid,pz),new double[]{tip,mid,pz}));
   }
   check(hoist<1.3,d.id()+": the hoist stays on the pole axis (most "+hoist+" px off)");
   double len=tip-px;
   check(most>len*0.25&&most<len*0.55,d.id()+": the fly sweeps a gentle "+String.format("%.1f",most)+" px for a "+len+" px pennant");
   check(!java.util.Arrays.equals(PropMotion.pose(d,cloth,500,seed),PropMotion.pose(d,cloth,500,PropEffects.seed(4,70,-9))),d.id()+": neighbours are out of step");
  }
 }

 /** cook_spit: a rotate on its entity origin. No translation ever (the client slerp turns it in place), a quarter turn per quarter period,
  *  the spit axis holds, the roast never dips into the flames (tips at y 14) and the rod turns clear of the forks. */
 static void spit()throws Exception{
  PropDef d=def("cook_spit");PropDef.Part roast=part(d,"cook_spit_roast");
  check(d.motion().size()==1&&d.motion().get(0).kind().equals("rotate")&&PropMotion.moves(d,roast)&&!PropMotion.moves(d,part(d,"cook_spit"))&&!PropMotion.moves(d,part(d,"cook_spit_fire")),"cook_spit: only the roast turns");
  PropDef.Motion m=d.motion().get(0);
  check(m.px()==8+roast.ox()&&m.py()==8+roast.oy()&&m.pz()==8+roast.oz(),"cook_spit: the pivot is the roast's entity origin");
  long seed=PropEffects.seed(-20,64,5);double low=1e9,axis=0,maxT=0,zr=0,yr=1e9;
  for(int t=0;t<m.period();t++){
   float[] pose=PropMotion.pose(d,roast,t,seed);
   maxT=Math.max(maxT,Math.abs(pose[0])+Math.abs(pose[1])+Math.abs(pose[2]));
   for(double[] P:new double[][]{{1,22,8},{31,22,8},{16,22,8}})axis=Math.max(axis,dist(world(pose,roast,P[0],P[1],P[2]),P));
   for(double[] P:new double[][]{{13,17,3},{13,17,13},{13,27,3},{13,27,13},{11,18,4},{11,26,12}})low=Math.min(low,world(pose,roast,P[0],P[1],P[2])[1]);
   for(double[] P:new double[][]{{4,21,7},{4,23,9},{4,21,9},{4,23,7}}){double[] w=world(pose,roast,P[0],P[1],P[2]);zr=Math.max(zr,Math.abs(w[2]-8));yr=Math.min(yr,w[1]);}
  }
  check(maxT<1e-5,"cook_spit: no translation, only rotation ("+maxT+")");
  check(axis<1e-4,"cook_spit: the spit axis holds ("+axis+" px)");
  check(low>14.2,"cook_spit: the roast's lowest corner stays above the flame tips: "+low+" px");
  double a0=PropMotion.angle(m,0,0,seed),a1=PropMotion.angle(m,0,m.period()/4.0,seed);
  check(Math.abs(((a1-a0)%360+360)%360-90)<1e-6,"cook_spit: a quarter turn per quarter period");
  check(zr<2&&yr>20,"cook_spit: the rod turns clear of the fork (z within "+zr+" px of the axle, lowest y "+yr+")");
 }

 /** trebuchet: a multi-part moving piece. The arm is six parts and the hanging box three; each turns as one (a model point lands in the
  *  same place under every part's pose), the box's pin stays on the arm's pin, the creak is a few px at the tip, the frame stays still. */
 static void trebuchet()throws Exception{
  PropDef d=def("trebuchet");
  java.util.List<PropDef.Part> arm=new java.util.ArrayList<>(),box=new java.util.ArrayList<>();
  for(int n=90;n<96;n++)arm.add(part(d,"trebuchet_"+n));
  for(int n=96;n<99;n++)box.add(part(d,"trebuchet_"+n));
  int moving=0;for(PropDef.Part p:d.parts())if(PropMotion.moves(d,p))moving++;
  check(moving==9,"trebuchet: nine moving parts (arm 6, box 3): "+moving);
  long seed=PropEffects.seed(100,64,100);double spread=0,pin=0,tipMost=0,boxMost=0;
  double[][] pts={{8,140,-12},{8,60,40},{8,190,-50},{-10,195,-45},{8,30,90},{20,100,-60}};
  double[] q={8,178.9,-50.9};
  double[] tip0=world(PropMotion.REST,arm.get(0),8,42,86),boxLow0=world(PropMotion.REST,box.get(0),8,92,-51);
  for(int t=0;t<1500;t+=5){
   for(double[] P:pts){
    double[] ref=world(PropMotion.pose(d,arm.get(0),t,seed),arm.get(0),P[0],P[1],P[2]);
    for(PropDef.Part p:arm)spread=Math.max(spread,dist(world(PropMotion.pose(d,p,t,seed),p,P[0],P[1],P[2]),ref));
    double[] bref=world(PropMotion.pose(d,box.get(0),t,seed),box.get(0),P[0],P[1],P[2]);
    for(PropDef.Part p:box)spread=Math.max(spread,dist(world(PropMotion.pose(d,p,t,seed),p,P[0],P[1],P[2]),bref));
   }
   pin=Math.max(pin,dist(world(PropMotion.pose(d,arm.get(2),t,seed),arm.get(2),q[0],q[1],q[2]),world(PropMotion.pose(d,box.get(1),t,seed),box.get(1),q[0],q[1],q[2])));
   tipMost=Math.max(tipMost,dist(world(PropMotion.pose(d,arm.get(0),t,seed),arm.get(0),8,42,86),tip0));
   boxMost=Math.max(boxMost,dist(world(PropMotion.pose(d,box.get(0),t,seed),box.get(0),8,92,-51),boxLow0));
  }
  check(spread<1e-3,"trebuchet: the arm's parts (and the box's) turn as one piece: spread "+spread+" px");
  check(pin<1e-3,"trebuchet: the box hangs on the arm's pin: "+pin+" px");
  check(tipMost>1.5&&tipMost<4,"trebuchet: the arm tip creaks a few px ("+String.format("%.2f",tipMost)+")");
  check(boxMost>1&&boxMost<6,"trebuchet: the box swings a little ("+String.format("%.2f",boxMost)+" px at its floor)");
  for(PropDef.Part p:d.parts())if(!arm.contains(p)&&!box.contains(p))check(!PropMotion.moves(d,p),"trebuchet: "+p.model()+" (frame) stays still");
  check(PropMotion.jitter(d.motion(),d.motion().size()-2)==0,"trebuchet: the box's arm entry shares the arm's desync");
 }

 public static void main(String[] args)throws Exception{
  long seed=PropEffects.seed(10,64,-3);
  try(Stream<Path> files=Files.list(Path.of("src/main/resources/data/gravesandglory/gg_props"))){
   for(Path f:(Iterable<Path>)files.filter(p->p.toString().endsWith(".json"))::iterator){
    PropDef d=PropDef.parse(Files.readString(f));
    if(MOVING.contains(d.id())){check(!d.motion().isEmpty(),d.id()+" moves");continue;}
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
  String ok="\"part\":\"t\",\"kind\":\"sway\",\"axis\":\"x\",\"pivot\":[8,8,8],\"amplitude\":5,\"period\":60";
  java.util.function.Function<String,String> entry=extra->head+"{"+extra+"}]}";
  check(!refused(entry.apply(ok)),"a good entry parses");
  check(!refused(entry.apply(ok.replace("\"x\"","\"-y\"")+",\"phase\":0.99,\"bias\":-90")),"axis -y, phase 0.99 and bias -90 parse");
  check(refused(entry.apply(ok.replace("\"part\":\"t\"","\"part\":\"u\""))),"an unknown part is refused");
  check(refused(entry.apply(ok.replace("sway","wobble"))),"an unknown kind is refused");
  check(refused(entry.apply(ok.replace("\"x\"","\"w\""))),"a bad axis is refused");
  check(refused(entry.apply(ok.replace("[8,8,8]","[8,8]"))),"a short pivot is refused");
  check(refused(entry.apply(ok.replace("[8,8,8]","[8,\"up\",8]"))),"a pivot that is not a number is refused");
  check(refused(entry.apply(ok.replace("[8,8,8]","[8,8,200]"))),"a pivot far outside the prop is refused");
  check(!refused(entry.apply(ok.replace("[8,8,8]","[-32,48,48]"))),"a pivot 32 px outside the prop parses");
  check(refused(entry.apply(ok+",\"phase\":1")),"phase 1 is refused");
  check(refused(entry.apply(ok+",\"bias\":91")),"bias over 90 is refused");
  check(refused(entry.apply(ok+",\"bias\":-91")),"bias under -90 is refused");
  check(refused(entry.apply(ok.replace("\"amplitude\":5","\"amplitude\":91").replace("60","2400"))),"amplitude over 90 is refused");
  check(refused(entry.apply(ok.replace("\"amplitude\":5","\"amplitude\":0"))),"a sway without amplitude is refused");
  check(refused(entry.apply(ok.replace("60","2401"))),"a period over 2400 is refused");
  check(refused(entry.apply(ok.replace("60","59"))),"a sway period under 6 keyframes is refused");
  check(refused(entry.apply(ok.replace("60","60.5"))),"a fractional period is refused");
  check(refused(entry.apply(ok.replace(",\"period\":60",""))),"a missing period is refused");
  check(refused(entry.apply(ok.replace("\"amplitude\":5","\"amplitude\":45"))),"45 degrees in 60 ticks (51 degrees a keyframe) is refused");
  check(!refused(entry.apply(ok.replace("\"amplitude\":5","\"amplitude\":39"))),"39 degrees in 60 ticks (44 degrees a keyframe) parses");
  String rj="\"part\":\"t\",\"kind\":\"rotate\",\"axis\":\"y\",\"pivot\":[8,8,8],\"period\":40";
  check(!refused(entry.apply(rj)),"a rotate about the entity origin with period 40 parses");
  check(refused(entry.apply(rj.replace("40","39"))),"a rotate of a quarter turn or more per keyframe is refused");
  check(refused(entry.apply(rj.replace("[8,8,8]","[8,8,4]"))),"a rotate off the entity origin with period 40 is refused");
  check(refused(entry.apply(rj.replace("[8,8,8]","[8,8,4]").replace("40","119"))),"a rotate off the entity origin under 12 keyframes is refused");
  check(!refused(entry.apply(rj.replace("[8,8,8]","[8,8,4]").replace("40","120"))),"a rotate off the entity origin with period 120 parses");
  String offs=head.replace("\"cmd\":7319990}","\"cmd\":7319990,\"offset\":[0,16,0]}");
  check(!refused(offs+"{"+rj.replace("[8,8,8]","[8,24,8]")+"}]}"),"a rotate about an offset part's entity origin parses");
  check(refused(offs+"{"+rj+"}]}"),"an offset part's [8,8,8] is not its entity origin");
  pennants();spit();trebuchet();
  System.out.println("PropMotionCheck: "+count+" checks pass; hem forward "+String.format("%.2f..%.2f",minFwd,maxFwd)+" px, sideways up to "+String.format("%.2f",maxSide)+" px");
 }
}
