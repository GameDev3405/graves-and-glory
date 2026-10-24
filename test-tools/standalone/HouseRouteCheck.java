package gg.gravesandglory;

/** The way into a supply house (HouseRoute, Phase 3 Task 8b): a unit outside the footprint goes first to the point outside the gate that is the shorter way (round a corner of the house when the straight line would cross it) and, within 3 blocks of it, straight on to its goal; a goal outside the footprint, or a unit inside it, is left alone. */
public final class HouseRouteCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 /** A house of rotation 0 at (-125,-72), the nw house of Rivergate: footprint x -140..-110, z -90..-54, front gate at z -54 facing south, side gate in the east wall. */
 static final int CX=-125,CZ=-72;
 static double[] to(int rotation,double hx,double hz,double gx,double gz){return HouseRoute.next(CX,CZ,rotation,hx,hz,gx,gz,0);}
 public static void main(String[] args){
  double gx=CX+.5,gz=CZ+6.5; // the yard centre
  // the gates, in the world
  var front=HouseRoute.world(CX,CZ,0,15.5,37.5);var side=HouseRoute.world(CX,CZ,0,31.5,24.5);
  check(Math.abs(front[0]-(CX+.5))<1e-9&&Math.abs(front[1]-(CZ+19.5))<1e-9,"The front gate point of a house turned 0 is the cell just south of the gate, at its centre column: "+front[0]+","+front[1]);
  check(Math.abs(side[0]-(CX+16.5))<1e-9&&Math.abs(side[1]-(CZ+6.5))<1e-9,"The side gate point of a house turned 0 is the cell just east of the side gate, at the yard's row: "+side[0]+","+side[1]);
  var front180=HouseRoute.world(CX,CZ,180,15.5,37.5);var side180=HouseRoute.world(CX,CZ,180,31.5,24.5);
  check(Math.abs(front180[0]-(CX+.5))<1e-9&&Math.abs(front180[1]-(CZ-18.5))<1e-9&&Math.abs(side180[0]-(CX-15.5))<1e-9&&Math.abs(side180[1]-(CZ-5.5))<1e-9,"Turned 180 the front gate is on the north side and the side gate on the west side");
  // a unit south of the house goes straight to the front gate point
  var a=to(0,CX+.5,CZ+60,gx,gz);
  check(a!=null&&Math.abs(a[0]-front[0])<1e-9&&Math.abs(a[1]-front[1])<1e-9,"A unit south of the house goes to the point outside the front gate");
  // a unit east of the house, level with the yard: the side gate is the shorter way
  var b=to(0,CX+60,CZ+6.5,gx,gz);
  check(b!=null&&Math.abs(b[0]-side[0])<1e-9&&Math.abs(b[1]-side[1])<1e-9,"A unit east of the house, level with the yard, goes to the point outside the side gate");
  // a unit west of the house, level with the yard (the wall is in the way): round the south-west corner, not at the wall
  var c=to(0,CX-40,CZ+6.5,gx,gz);
  check(c!=null&&c[0]<CX-15&&c[1]>CZ+18,"A unit west of the house goes round its south-west corner first: "+(c==null?"null":c[0]+","+c[1]));
  // a unit north of the house (behind the storehouse): a corner on the north side first
  var d=to(0,CX+.5,CZ-60,gx,gz);
  check(d!=null&&d[1]<CZ-18,"A unit north of the house goes to a corner on the north side first: "+(d==null?"null":d[0]+","+d[1]));
  // within 3 blocks of a gate point, in the footprint, or with a goal outside the footprint: straight on
  check(to(0,front[0]+1.0,front[1]+1.0,gx,gz)==null,"A unit within 3 blocks of the front gate point goes straight to its goal");
  check(to(0,side[0]+2.0,side[1],gx,gz)==null,"A unit within 3 blocks of the side gate point goes straight to its goal");
  check(to(0,CX+.5,CZ+10,gx,gz)==null,"A unit in the yard goes straight to its goal");
  check(to(0,CX+.5,CZ+60,CX+.5,CZ+40)==null,"A goal outside the footprint is left alone");
  // every point of a grid round the house, both turns: the first waypoint is reached without crossing the house, and the way on from it is clear or round a corner (the walk ends at a gate)
  int starts=0,crossing=0,naive=0;
  for(int rot:new int[]{0,180})for(double x=CX-60;x<=CX+60;x+=3.5)for(double z=CZ-60;z<=CZ+60;z+=3.5){
   if(HouseRoute.inside(CX,CZ,x,z,0.3))continue;
   var w=HouseRoute.next(CX,CZ,rot,x,z,CX+.5,CZ+(rot==0?6.5:-5.5),0);starts++;
   if(w==null)continue;
   if(!HouseRoute.clear(CX,CZ,x,z,w[0],w[1]))crossing++;
   var g0=HouseRoute.world(CX,CZ,rot,HouseRoute.GATES[0][0],HouseRoute.GATES[0][1]);var g1=HouseRoute.world(CX,CZ,rot,HouseRoute.GATES[1][0],HouseRoute.GATES[1][1]);
   // the control: the straight line to the nearer gate point would cross the house from some starts (that is what the corners are for)
   var nearer=Math.hypot(x-g0[0],z-g0[1])<Math.hypot(x-g1[0],z-g1[1])?g0:g1;
   if(!HouseRoute.clear(CX,CZ,x,z,nearer[0],nearer[1]))naive++;
  }
  check(starts>500,"The grid has starts: "+starts);
  check(crossing==0,"No first waypoint of "+starts+" starts round the house is reached across it: "+crossing+" cross it");
  check(naive>50,"The control can fail: the straight line to the nearer gate crosses the house from "+naive+" of the "+starts+" starts, so the corners are needed");
  // a unit that follows the waypoints reaches a gate point: step 4 blocks at a time from 40 starts
  int arrived=0,walks=0;
  for(int rot:new int[]{0,180})for(double x=CX-60;x<=CX+60;x+=20)for(double z=CZ-60;z<=CZ+60;z+=20){
   if(HouseRoute.inside(CX,CZ,x,z,0.3))continue;walks++;
   double px=x,pz=z;boolean ok=false;double goalZ=CZ+(rot==0?6.5:-5.5);
   for(int step=0;step<400;step++){
    var w=HouseRoute.next(CX,CZ,rot,px,pz,CX+.5,goalZ,0);
    if(w==null){ok=HouseRoute.inside(CX,CZ,px,pz,0.3)||Math.min(Math.hypot(px-HouseRoute.world(CX,CZ,rot,15.5,37.5)[0],pz-HouseRoute.world(CX,CZ,rot,15.5,37.5)[1]),Math.hypot(px-HouseRoute.world(CX,CZ,rot,31.5,24.5)[0],pz-HouseRoute.world(CX,CZ,rot,31.5,24.5)[1]))<HouseRoute.ARRIVE;break;}
    double dx=w[0]-px,dz=w[1]-pz,l=Math.hypot(dx,dz);
    if(l<=0.5){px=w[0];pz=w[1];}else{px+=dx/l*Math.min(l,2.0);pz+=dz/l*Math.min(l,2.0);}
   }
   if(ok)arrived++;
  }
  check(walks>20&&arrived==walks,"A unit that walks to each first waypoint in turn comes to a gate of the house from every one of "+walks+" starts: "+arrived+" do");
  System.out.println("PASS "+count+" house route scenarios");
 }
}
