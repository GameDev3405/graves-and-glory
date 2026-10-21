package gg.gravesandglory;

import java.util.Arrays;

/** Pure bounded team assignment: no enemy omniscience, unit statistics or orders are changed here. */
public final class BotPlanner {
 private BotPlanner(){}
 public record Policy(double duplicatePenalty,double holdBias,double stickiness){}
 /** Globally minimize friendly travel/coverage costs for at most four bot armies and five objectives. */
 public static int[] assign(double[][] travel,int[] owners,boolean[] covered,int[] previous,int team,Policy policy){
  int n=travel.length,m=owners.length;int[] empty=new int[n];Arrays.fill(empty,-1);if(n==0||m==0)return empty;
  if(n>4||m>5||covered.length!=m||previous.length!=n)throw new IllegalArgumentException("unsupported planner shape");
  double[][] cost=new double[n][m];
  for(int i=0;i<n;i++){if(travel[i].length!=m)throw new IllegalArgumentException("ragged travel matrix");for(int j=0;j<m;j++){
   if(!Double.isFinite(travel[i][j])||travel[i][j]<0)throw new IllegalArgumentException("invalid travel cost");
   cost[i][j]=travel[i][j]-(owners[j]==team?policy.holdBias:0)-(previous[i]==j?policy.stickiness:0);
  }}
  int[] loads=new int[m];for(int j=0;j<m;j++)loads[j]=covered[j]?1:0;
  Search search=new Search(cost,policy.duplicatePenalty,team);search.visit(0,0,loads,new int[n]);return search.best;
 }
 private static final class Search{
  final double[][] cost;final double duplicate;final int team;double score=Double.POSITIVE_INFINITY;int[] best;
  Search(double[][] cost,double duplicate,int team){this.cost=cost;this.duplicate=duplicate;this.team=team;}
  void visit(int row,double value,int[] loads,int[] result){
   if(row==cost.length){if(value<score-1e-7){score=value;best=result.clone();}return;}
   for(int step=0;step<loads.length;step++){int j=team==0?step:loads.length-1-step;result[row]=j;
    double next=value+cost[row][j]+duplicate*loads[j];loads[j]++;visit(row+1,next,loads,result);loads[j]--;
   }
  }
 }
 /** Same crossing geometry as Tactics.route; includes both bridge mouths rather than distance through water. */
 public static double travel(double x,double z,double tx,double tz,double riverHere,double riverThere,double[][] bridges,double inset){
  double direct=Math.hypot(tx-x,tz-z),side=x-riverHere,destination=tx-riverThere;
  if(Math.signum(side)==Math.signum(destination)||Math.abs(destination)<=7||bridges.length==0)return direct;
  double best=Double.POSITIVE_INFINITY;
  for(var bridge:bridges){double mouth=bridge[2]/2-inset,entry=bridge[0]+Math.signum(side)*mouth,exit=bridge[0]+Math.signum(destination)*mouth;
   best=Math.min(best,Math.hypot(x-entry,z-bridge[1])+Math.abs(exit-entry)+Math.hypot(tx-exit,tz-bridge[1]));}
  return best;
 }
 public static float yaw(double dx,double dz,float fallback){return Math.hypot(dx,dz)<1?fallback:(float)Math.toDegrees(Math.atan2(-dx,dz));}
}
