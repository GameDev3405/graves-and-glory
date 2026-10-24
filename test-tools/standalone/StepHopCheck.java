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
  System.out.println("PASS "+count+" step hop scenarios");
 }
}
