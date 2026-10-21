package gg.gravesandglory;
import java.util.*;
public final class BotPlannerCheck {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static BotPlanner.Policy neutral=new BotPlanner.Policy(180,0,0);
 public static void main(String[] args){
  check(Arrays.equals(BotPlanner.assign(new double[][]{{2,3},{2,100}},new int[]{-1,-1},new boolean[2],new int[]{-1,-1},0,neutral),new int[]{1,0}),"global assignment beats nearest-first trap");
  check(Arrays.equals(BotPlanner.assign(new double[][]{{1,1},{1,1}},new int[]{-1,-1},new boolean[2],new int[]{-1,-1},0,neutral),new int[]{0,1}),"stable team0 ties");
  check(Arrays.equals(BotPlanner.assign(new double[][]{{1,1},{1,1}},new int[]{-1,-1},new boolean[2],new int[]{-1,-1},1,neutral),new int[]{1,0}),"mirrored team1 ties");
  check(BotPlanner.assign(new double[][]{{1,20}},new int[]{0,0},new boolean[]{true,false},new int[]{-1},0,neutral)[0]==1,"human guard prevents needless duplication");
  var sticky=new BotPlanner.Policy(180,45,24);
  check(BotPlanner.assign(new double[][]{{10,20}},new int[]{-1,0},new boolean[2],new int[]{-1},0,sticky)[0]==1,"keep a secured point");
  check(BotPlanner.assign(new double[][]{{10,20}},new int[]{-1,-1},new boolean[2],new int[]{1},0,sticky)[0]==1,"small distance fluctuation does not churn targets");
  check(BotPlanner.assign(new double[][]{{10,100}},new int[]{-1,-1},new boolean[2],new int[]{1},0,sticky)[0]==0,"large benefit still retargets");
  check(BotPlanner.assign(new double[0][0],new int[0],new boolean[0],new int[0],0,neutral).length==0,"no armies");
  check(Arrays.equals(BotPlanner.assign(new double[][]{{}},new int[0],new boolean[0],new int[]{-1},0,neutral),new int[]{-1}),"no points");
  check(Arrays.equals(BotPlanner.assign(new double[][]{{1},{2},{3}},new int[]{-1},new boolean[1],new int[]{-1,-1,-1},0,neutral),new int[]{0,0,0}),"more armies than points");
  double[][] bridges={{0,50,20}};
  check(Math.abs(BotPlanner.travel(-20,0,20,0,0,0,bridges,2)-(2*Math.hypot(12,50)+16))<1e-6,"river route pays both crossing approaches");
  check(BotPlanner.travel(-20,0,-10,0,0,0,bridges,2)==10,"same-bank route stays direct");
  check(BotPlanner.travel(-20,0,20,0,0,0,bridges,2)==BotPlanner.travel(20,0,-20,0,0,0,bridges,2),"crossing estimate is symmetric");
  check(BotPlanner.yaw(10,0,0)==-90&&BotPlanner.yaw(-10,0,0)==90&&BotPlanner.yaw(0,10,90)==0&&BotPlanner.yaw(0,0,42)==42,"actual threat facing, stable at arrival");
  for(int seed=0;seed<100;seed++){var rng=new Random(seed);double[][] costs=new double[3][5],rotated=new double[3][5];for(int i=0;i<3;i++)for(int j=0;j<5;j++)costs[i][j]=rng.nextDouble()*1000;
   for(int i=0;i<3;i++)for(int j=0;j<5;j++)rotated[i][j]=costs[i][4-j];var a=BotPlanner.assign(costs,new int[]{-1,-1,-1,-1,-1},new boolean[5],new int[]{-1,-1,-1},0,neutral);var b=BotPlanner.assign(rotated,new int[]{-1,-1,-1,-1,-1},new boolean[5],new int[]{-1,-1,-1},1,neutral);check(a[0]==4-b[0]&&a[1]==4-b[1]&&a[2]==4-b[2],"rotated input preserves assignments "+seed);}
  try{BotPlanner.assign(new double[][]{{Double.NaN}},new int[]{-1},new boolean[1],new int[]{-1},0,neutral);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){checks++;}
  System.out.println("BotPlannerCheck: "+checks+" checks passed");
 }
}
