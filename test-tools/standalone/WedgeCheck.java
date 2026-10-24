package gg.gravesandglory;

/** The way out of a dead end (Wedge, Phase 3 Task 9 fix 1): when a unit is stuck again at the place of its last sidestep, soon after it, and how many steps of the whole path a detour walks, how long it may take, and where it ends. The behaviour in the game (a unit set in the pocket of the Hollowwood chapel ruin stays there without the detour and leaves with it) is the nook goals of MapRouteChecks, in the supply suite on map 3. */
public final class WedgeCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 /** A unit's state as Tactics.walk keeps it: the last sidestep (tick, place, repeats). */
 static final class Unit{
  boolean had;long time;double x,z;int repeats;
  int stuck(long t,double px,double pz){repeats=Wedge.repeats(had,time,x,z,repeats,t,px,pz);had=true;time=t;x=px;z=pz;return repeats;}
 }
 public static void main(String[] args){
  // repeats: the first stuck is 0, then 1, 2, 3 while it is the same wedge
  var u=new Unit();
  check(u.stuck(1000,81.5,102.5)==0,"The first sidestep of a unit is repeat 0");
  check(u.stuck(1060,81.6,102.5)==1&&u.stuck(1120,81.5,102.6)==2&&u.stuck(1180,81.7,102.5)==3,"Stuck again every three seconds at the same place counts 1, 2, 3");
  // the edges: ten seconds exactly and six blocks exactly are the same wedge, a tick or a hair more are not; a clock that ran back is a new wedge
  u=new Unit();u.stuck(1000,0,0);
  check(u.stuck(1000+Wedge.AGAIN_TICKS,0,0)==1,"Exactly ten seconds after is the same wedge");
  u=new Unit();u.stuck(1000,0,0);
  check(u.stuck(1000+Wedge.AGAIN_TICKS+1,0,0)==0,"A tick over ten seconds is a new wedge");
  u=new Unit();u.stuck(1000,0,0);
  check(u.stuck(1060,Wedge.AGAIN_BLOCKS,0)==1,"Exactly six blocks away is the same wedge");
  u=new Unit();u.stuck(1000,0,0);
  check(u.stuck(1060,Wedge.AGAIN_BLOCKS+0.01,0)==0,"Over six blocks away is a new wedge");
  u=new Unit();u.stuck(5000,0,0);
  check(u.stuck(100,0,0)==0,"A clock that has run back (a new battle) is a new wedge");
  // the steps of a detour grow with the repeats, to a limit; the pick is the node that many steps on or the last; the time allowed grows with the steps
  check(Wedge.steps(1)==12&&Wedge.steps(2)==20&&Wedge.steps(3)==28&&Wedge.steps(4)==36&&Wedge.steps(5)==40&&Wedge.steps(50)==40,"A detour walks 12, 20, 28, 36, then 40 steps: "+Wedge.steps(1)+","+Wedge.steps(2)+","+Wedge.steps(3)+","+Wedge.steps(4)+","+Wedge.steps(5));
  check(Wedge.pick(100,12)==12&&Wedge.pick(5,12)==4&&Wedge.pick(1,12)==0,"The end of a detour is the node that many steps on, or the last node of a short path");
  check(Wedge.ticks(12)==160&&Wedge.ticks(40)==400&&Wedge.ticks(100)==400,"The time allowed is 40 ticks and 10 a step, at most 400");
  System.out.println("PASS "+count+" wedge scenarios");
 }
}
