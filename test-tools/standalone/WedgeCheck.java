package gg.gravesandglory;

/** The way out of a dead end (Wedge, Phase 3 Task 9 fixes 1 and 2): when a unit is stuck again at the place of its last sidestep, soon after it, how many steps of the whole path a detour walks, how long it may take, and where it ends; what a stuck unit does (search, or sidestep because it is the first stuck here, because the last search found nothing, or because a search has started in this tick); where the search looks (the goal, or the cell 80 blocks toward it); which node ends the detour; and when the detour is over. The behaviour in the game (a unit set in the pocket of the Hollowwood chapel ruin stays there without the detour and leaves with it, for a goal 45 and 200 blocks off) is the nook goals of MapRouteChecks, in the supply suite on map 3. */
public final class WedgeCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 /** A unit's state as Tactics.walk keeps it: the last sidestep (tick, place, repeats) and the tick before which no search is made. */
 static final class Unit{
  boolean had;long time;double x,z;int repeats;long retryAt;
  int stuck(long t,double px,double pz){repeats=Wedge.repeats(had,time,x,z,repeats,t,px,pz);had=true;time=t;x=px;z=pz;return repeats;}
  /** What Tactics.walk does at a stuck: the choice; a search that finds nothing sets the tick to wait for. */
  int act(long t,int searchesThisTick,boolean found){int what=Wedge.next(repeats,t,retryAt,searchesThisTick);if(what==Wedge.SEARCH&&!found)retryAt=t+Wedge.RETRY_TICKS;return what;}
 }
 static double[] line(int n,double x0,double step){double[] a=new double[n];for(int i=0;i<n;i++)a[i]=x0+i*step;return a;}
 static double[] zeros(int n){return new double[n];}
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
  // the guard of a unit that has not sidestepped yet: without it a stuck unit near the origin in the first ten seconds would count as a repeat
  check(Wedge.repeats(false,0,0,0,0,100,0.5,0.5)==0,"A unit that has not sidestepped before is repeat 0 even at the origin, in the first ten seconds");
  // the steps of a detour grow with the repeats, to a limit; the time allowed grows with the steps
  check(Wedge.steps(1)==12&&Wedge.steps(2)==20&&Wedge.steps(3)==28&&Wedge.steps(4)==36&&Wedge.steps(5)==40&&Wedge.steps(50)==40,"A detour walks 12, 20, 28, 36, then 40 steps: "+Wedge.steps(1)+","+Wedge.steps(2)+","+Wedge.steps(3)+","+Wedge.steps(4)+","+Wedge.steps(5));
  check(Wedge.ticks(12)==160&&Wedge.ticks(40)==400&&Wedge.ticks(100)==400,"The time allowed is 40 ticks and 10 a step, at most 400");
  // the bounds the report states: at most one search a tick, a miss remembered 15 seconds, a search looks 80 blocks on and (the pathfinder of the detour) 160 blocks of follow range = 2560 cells, a point 5 blocks off
  check(Wedge.MAX_SEARCHES==1&&Wedge.RETRY_TICKS==300&&Wedge.REACH==80&&Wedge.FOLLOW_RANGE==160&&Wedge.AWAY_BLOCKS==5,"The bounds of the detour search: one a tick, 300 ticks after a miss, 80 blocks, follow range 160, 5 blocks away");
  // what a stuck unit does: the first stuck sidesteps; then it searches; a miss is remembered for 300 ticks; at most one search starts in a tick
  check(Wedge.next(0,1000,0,0)==Wedge.FIRST,"The first stuck here is a sidestep");
  check(Wedge.next(1,1070,0,0)==Wedge.SEARCH,"Stuck again: a search");
  check(Wedge.next(0,1000,5000,0)==Wedge.FIRST,"The first stuck is a sidestep even while a miss is remembered");
  check(Wedge.next(2,1100,1370,0)==Wedge.COOLING&&Wedge.next(2,1369,1370,0)==Wedge.COOLING,"A miss is remembered: no search before its tick");
  check(Wedge.next(2,1370,1370,0)==Wedge.SEARCH,"At the tick the miss was remembered until the search is made again");
  check(Wedge.next(1,1070,0,Wedge.MAX_SEARCHES)==Wedge.BUDGET&&Wedge.next(1,1070,0,Wedge.MAX_SEARCHES-1)==Wedge.SEARCH,"At most MAX_SEARCHES searches start in one tick: the next unit sidesteps");
  check(Wedge.next(1,1070,2000,Wedge.MAX_SEARCHES)==Wedge.COOLING,"A miss is checked before the budget of the tick");
  // a chain: stuck at 1000 (first), 1070 (search, finds nothing), 1140 and 1210 (cooling), on to 1350 (cooling, 5 repeats), 1420 (search again, a path found)
  u=new Unit();
  u.stuck(1000,81.5,102.5);check(u.act(1000,0,false)==Wedge.FIRST,"Chain: the first stuck sidesteps");
  u.stuck(1070,81.6,102.5);check(u.act(1070,0,false)==Wedge.SEARCH&&u.retryAt==1070+Wedge.RETRY_TICKS,"Chain: the second stuck searches and remembers a miss until 1370");
  u.stuck(1140,81.5,102.5);check(u.act(1140,0,false)==Wedge.COOLING&&u.retryAt==1370,"Chain: the third stuck sidesteps while the miss is remembered");
  u.stuck(1210,81.5,102.6);check(u.act(1210,0,false)==Wedge.COOLING,"Chain: so does the fourth");
  u.stuck(1280,81.5,102.6);u.stuck(1350,81.5,102.5);check(u.repeats==5&&u.act(1350,0,true)==Wedge.COOLING,"Chain: the repeats go on counting through the cool-down ("+u.repeats+")");
  u.stuck(1420,81.5,102.5);check(u.act(1420,0,true)==Wedge.SEARCH&&u.retryAt==1370,"Chain: after the cool-down it searches again, and a path found leaves the memory as it was");
  // where the search looks: the goal within 80 blocks (exactly 80 included), else the point 80 blocks along the line to it
  var t=Wedge.target(0,0,60,0);
  check(t[0]==60&&t[1]==0,"A goal 60 blocks off is the target");
  t=Wedge.target(0,0,80,0);
  check(t[0]==80&&t[1]==0,"A goal exactly 80 blocks off is the target");
  t=Wedge.target(0,0,160,0);
  check(Math.abs(t[0]-80)<1e-9&&Math.abs(t[1])<1e-9,"A goal 160 blocks off: the cell 80 along the line");
  t=Wedge.target(10,20,10+120,20+160);
  check(Math.abs(t[0]-(10+48))<1e-9&&Math.abs(t[1]-(20+64))<1e-9,"A goal 200 blocks off on a 3-4-5 line: 80 along it ("+t[0]+","+t[1]+")");
  t=Wedge.target(81.7,102.5,81.7+0.611*200,102.5-0.791*200);
  check(Math.hypot(t[0]-81.7,t[1]-102.5)>79.999&&Math.hypot(t[0]-81.7,t[1]-102.5)<80.001,"The nook-far goal 200 blocks off is cut to 80");
  // the node that ends the detour: that many steps on, or the last; on to the first one 5 blocks from the unit; the last when none is
  check(Wedge.pick(line(100,1,1),zeros(100),100,0,0,12)==12,"Twelve steps on a long straight path is node 12");
  check(Wedge.pick(line(5,10,1),zeros(5),5,0,0,12)==4,"A short path ends at its last node");
  check(Wedge.pick(line(1,10,1),zeros(1),1,0,0,12)==0,"A path of one node is node 0");
  double[] xs=new double[30],zs=new double[30];
  for(int i=0;i<30;i++){xs[i]=i<10?i:i<20?3:3+(i-19);zs[i]=0;} // out 10 blocks, then back to 3 blocks from the unit (a U round a thin wall), then on
  check(Wedge.pick(xs,zs,30,0,0,12)==21,"A node 12 steps on that is within 5 blocks of the unit (the path doubles back) is passed over for the first one 5 blocks away: "+Wedge.pick(xs,zs,30,0,0,12));
  check(Wedge.pick(xs,zs,30,0,0,5)==5,"A node 5 blocks off is far enough");
  check(Wedge.pick(xs,zs,30,0,0,4)==5,"Node 4 is 4 blocks off: the first at or after it that is 5 off is node 5");
  check(Wedge.pick(line(20,1,0.1),zeros(20),20,0,0,12)==19,"A path that never gets 5 blocks away ends at its last node");
  // when the detour is over: time, arrival (2 blocks across, less than 1.5 up or down), or a goal that moved more than 4 blocks
  check(!Wedge.over(100,160,0,65,0,10,65,0,50,50,50,50),"A detour on its way is not over");
  check(!Wedge.over(159,160,0,65,0,10,65,0,50,50,50,50)&&Wedge.over(160,160,0,65,0,10,65,0,50,50,50,50),"The time is up at its tick");
  check(Wedge.over(100,160,9,65,0,10,65,0,50,50,50,50),"One block from its point is arrived");
  check(!Wedge.over(100,160,8,65,0,10,65,0,50,50,50,50),"Exactly 2 blocks from its point is not yet arrived");
  check(!Wedge.over(100,160,9.5,66.5,0,10,65,0,50,50,50,50)&&Wedge.over(100,160,9.5,66.4,0,10,65,0,50,50,50,50),"Next to the point but 1.5 blocks above it (a floor over a wall) is not arrived; 1.4 is");
  check(!Wedge.over(100,160,0,65,0,10,65,0,54,50,50,50)&&Wedge.over(100,160,0,65,0,10,65,0,54.01,50,50,50),"A goal that moved 4 blocks is the same goal; over 4 it is a new one");
  System.out.println("PASS "+count+" wedge scenarios");
 }
}
