package gg.gravesandglory;

import java.util.HashSet;
import java.util.Set;

/** The step a stuck unit cannot jump (StepHop, Phase 3 Task 8b), on a grid of blocks: flat ground with a step one block up to the west, and what must not be taken for one (a wall of two, a cell with leaves at its head, a step the wrong way, a fence, a drop). */
public final class StepHopCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 /** A world of full blocks at every cell with y <= 69, and what the case adds: solid cells (a step, a wall, leaves) and cells that are not a full face (a fence). */
 static final class World implements StepHop.Probe{
  final Set<String> solid=new HashSet<>(),partial=new HashSet<>();
  static String k(int x,int y,int z){return x+","+y+","+z;}
  World add(int x,int y,int z){solid.add(k(x,y,z));return this;}
  World part(int x,int y,int z){partial.add(k(x,y,z));return this;}
  boolean block(int x,int y,int z){return y<=69||solid.contains(k(x,y,z));}
  public boolean open(int x,int y,int z){return !block(x,y,z)&&!partial.contains(k(x,y,z));}
  public boolean floor(int x,int y,int z){return block(x,y,z)&&!partial.contains(k(x,y,z));}
  /** the step: every cell at x <= 9 is one block higher (a block at y 70) over z 5..15 */
  static World step(){World w=new World();for(int x=0;x<=9;x++)for(int z=5;z<=15;z++)w.add(x,70,z);return w;}
 }
 /** Columns of ground: each block column solid up to its top (the height a unit on it stands at) (default `ground`), and optionally a ceiling from some height up; a box is empty when it is above every top under it and under every ceiling over it. */
 static final class Heights implements StepHop.Room{
  final double ground;final java.util.Map<String,Double> top=new java.util.HashMap<>(),roof=new java.util.HashMap<>();
  Heights(double ground){this.ground=ground;}
  Heights set(int x,int z,double y){top.put(x+","+z,y);return this;}
  Heights ceiling(int x,int z,double y){roof.put(x+","+z,y);return this;}
  public boolean empty(double x0,double y0,double z0,double x1,double y1,double z1){
   for(int x=(int)Math.floor(x0);x<=(int)Math.floor(x1-1e-9);x++)for(int z=(int)Math.floor(z0);z<=(int)Math.floor(z1-1e-9);z++){
    if(y0<top.getOrDefault(x+","+z,ground)-1e-7)return false;
    Double r=roof.get(x+","+z);if(r!=null&&y1>r+1e-7)return false;
   }
   return true;
  }
 }
 public static void main(String[] args){
  // the units found stuck: at the foot of a step one block up to the west, going west
  var w=World.step();
  int[] c=StepHop.up(10.7,70.0,10.5,0.5,10.5,w);
  check(c!=null&&c[0]==9&&c[1]==71&&c[2]==10,"A unit at the foot of a step to the west, going west, is set on the cell beside it one block up: "+(c==null?"null":c[0]+","+c[1]+","+c[2]));
  c=StepHop.up(10.7,70.05,10.5,0.5,10.5,w);
  check(c!=null&&c[0]==9&&c[1]==71,"A hair off its height (part way up a jump), the same");
  c=StepHop.up(10.7,69.97,10.5,0.5,10.5,w);
  check(c!=null&&c[0]==9&&c[1]==71,"A hair under its height, the same");
  c=StepHop.up(10.7,70.0,10.5,2.5,2.5,w);
  check(c!=null&&c[0]==9&&c[2]==9,"Going north-west it is the north-west cell that is nearest the goal: "+(c==null?"null":c[0]+","+c[1]+","+c[2]));
  // what must not hop
  check(StepHop.up(10.7,70.0,10.5,20.5,10.5,w)==null,"A step behind the unit, the goal to the east, is not taken");
  var wall=World.step();for(int x=0;x<=9;x++)for(int z=5;z<=15;z++)wall.add(x,71,z);
  check(StepHop.up(10.7,70.0,10.5,0.5,10.5,wall)==null,"A wall of two blocks is not a step");
  var leaves=World.step();for(int z=9;z<=11;z++)leaves.add(9,72,z);
  c=StepHop.up(10.7,70.0,10.5,0.5,10.5,leaves);
  check(c==null||c[0]!=9||c[2]==8||c[2]==12,"With leaves at the head of the cells at x 9, z 9..11, none of them is taken: "+(c==null?"null":c[0]+","+c[1]+","+c[2]));
  var fence=World.step();for(int x=0;x<=9;x++)for(int z=5;z<=15;z++)fence.part(x,70,z);
  check(StepHop.up(10.7,70.0,10.5,0.5,10.5,fence)==null,"A fence or a partial block is not a floor to stand on");
  var east=new World();for(int z=5;z<=15;z++)east.add(11,70,z);
  check(StepHop.up(10.7,70.0,10.5,20.5,10.5,east)!=null&&StepHop.up(10.7,70.0,10.5,0.5,10.5,east)==null,"A step to the east is taken going east and not going west");
  check(StepHop.up(10.7,70.0,10.5,10.5,10.5,w)==null,"A unit that has arrived (the goal at its feet) goes nowhere");
  check(StepHop.up(10.7,70.0,10.5,0.5,10.5,new World())==null,"On flat ground there is nothing to step up");
  // the control: over a grid of goals a hop is only ever to a cell of the step, and only to one that is nearer the goal; and both outcomes occur, so that a rule that always hopped, or never, is told apart
  int taken=0,refused=0,wrong=0;
  for(double gx=-10.5;gx<=30.5;gx+=5)for(double gz=-10.5;gz<=30.5;gz+=5){
   var r=StepHop.up(10.7,70.0,10.5,gx,gz,w);
   if(r==null){refused++;continue;}
   taken++;
   if(r[0]!=9||r[1]!=71||Math.hypot(gx-(r[0]+.5),gz-(r[2]+.5))>=Math.hypot(gx-10.7,gz-10.5)-0.3)wrong++;
  }
  check(wrong==0,"Every hop of the grid of goals is to a cell of the step that is nearer the goal: "+wrong+" are not");
  check(taken>5&&refused>5,"The control can fail: of "+(taken+refused)+" goals "+taken+" are taken and "+refused+" refused");
  // where on the step cell a hopping unit lands (Tactics.stepHop): a narrow body at the point nearest its waypoint, a body a block wide or wider in the middle (M3)
  double[] l=StepHop.landing(0.6,9,10,0.5,10.5);
  check(Math.abs(l[0]-9.3)<1e-9&&Math.abs(l[1]-10.5)<1e-9,"A 0.6-wide unit lands at the edge of the cell nearest its waypoint, its body inside the cell: "+l[0]+","+l[1]);
  l=StepHop.landing(1.4,9,10,0.5,10.5);
  check(l[0]==9.5&&l[1]==10.5,"A golem (1.4 wide) lands in the middle of the cell, never off it toward the waypoint: "+l[0]+","+l[1]);
  l=StepHop.landing(1.0,9,10,0.5,30.5);
  check(l[0]==9.5&&l[1]==10.5,"A body a block wide has no room to slide on the block and takes its middle");
  l=StepHop.landing(0.6,9,10,9.45,10.6);
  check(Math.abs(l[0]-9.45)<1e-9&&Math.abs(l[1]-10.6)<1e-9,"A waypoint on the cell is landed on as it is");
  // a wall troop's slot on a rise one block high (Tactics.onRise), read from the troop's own level, not the block it is in (Minor 1)
  var flat=new Heights(70.0);
  check(!StepHop.onRise(10.5,70.0,10.5,0.3,1.95,9.85,10.5,0.6,flat),"On flat ground a slot is not on a rise");
  var sand=new Heights(69.875);
  check(!StepHop.onRise(10.5,69.875,10.5,0.3,1.95,9.85,10.5,0.6,sand),"On flat soul sand or mud (the floor 0.125 under a block top) a slot is not on a rise");
  var rise=new Heights(70.0).set(9,10,71.0);
  check(StepHop.onRise(10.5,70.0,10.5,0.3,1.95,9.85,10.5,0.6,rise),"A slot reaching over the edge of a rise one block high is on it");
  var slab=new Heights(70.0).set(9,10,70.5);
  check(!StepHop.onRise(10.5,70.0,10.5,0.3,1.95,9.85,10.5,0.6,slab),"A slab is a step the walk climbs, not a rise");
  var two=new Heights(70.0).set(9,10,72.0);
  check(!StepHop.onRise(10.5,70.0,10.5,0.3,1.95,9.85,10.5,0.6,two),"A rise of two blocks is not jumped");
  var sandRise=new Heights(69.875).set(9,10,70.875);
  check(StepHop.onRise(10.5,69.875,10.5,0.3,1.95,9.85,10.5,0.6,sandRise),"Soul sand a block higher, from soul sand, is a rise");
  var roof=new Heights(70.0).set(9,10,71.0).ceiling(10,10,72.5);
  check(!StepHop.onRise(10.5,70.0,10.5,0.3,1.95,9.85,10.5,0.6,roof),"With no room to jump above the troop it does not jump");
  // the hop pull (Tactics.hop): half the way to the slot, never more than the cap (0.3, a walk-pace jump) in length, its direction kept
  double[] far=StepHop.pull(2.0,0.0,0.5,0.3);check(Math.abs(far[0]-0.3)<1e-9&&far[1]==0,"A jump begun 2 blocks from its slot is carried at the cap, 0.3 a tick, not 1.0");
  double[] diag=StepHop.pull(1.4,1.4,0.5,0.3);check(Math.abs(Math.hypot(diag[0],diag[1])-0.3)<1e-9&&Math.abs(diag[0]-diag[1])<1e-12,"A diagonal jump from far is capped at 0.3 in length and keeps its direction");
  double[] near=StepHop.pull(0.4,-0.3,0.5,0.3);check(Math.abs(near[0]-0.2)<1e-12&&Math.abs(near[1]+0.15)<1e-12,"A jump from 0.5 away is carried half the way, under the cap");
  double[] pace=StepHop.pull(0.6,0.0,0.5,0.169);check(Math.abs(pace[0]-0.169)<1e-12,"A slower troop is capped at its own walking pace");
  double[] none=StepHop.pull(0,0,0.5,0.3);check(none[0]==0&&none[1]==0,"On the slot there is no pull");
  for(int i=0;i<360;i+=7){double r=Math.toRadians(i);for(double d=0;d<=2.5;d+=0.1){double[] v=StepHop.pull(Math.cos(r)*d,Math.sin(r)*d,0.5,0.3);if(Math.hypot(v[0],v[1])>0.3+1e-9){check(false,"pull over the cap at "+i+" degrees, "+d);}}}
  check(true,"No pull from 0 to 2.5 blocks round the compass exceeds the cap");
  System.out.println("PASS "+count+" step hop scenarios");
 }
}
