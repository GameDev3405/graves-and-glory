package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Prop geometry (prop kit plan task S3): facing 0 is the offset from the Structures pivot; a prop at facing f inside a structure placed at
 *  rotation s sits where the same prop at facing f+s would (composition, for cells and points); facing <-> command-block direction round
 *  trips; every facing keeps the footprint's cells distinct; the forge at 90 puts its local (0,0,0) at (1,0,-1) from the pivot (1,0,0). */
public final class PropGeometryCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static final String[] IDS={"forge","anvil","weapon_rack","shield_rack","quench_trough","grindstone_bench"};
 static final int[] FACINGS={0,90,180,270};
 static boolean near(double a,double b){return Math.abs(a-b)<1e-9;}

 /** Turns an offset from the anchor cell by a structure rotation, the way Structures.turn does (90: east to south). */
 static int[] turnCell(int s,int[] c){
  return switch(s){case 0->new int[]{c[0],c[1],c[2]};case 90->new int[]{-c[2],c[1],c[0]};case 180->new int[]{-c[0],c[1],-c[2]};default->new int[]{c[2],c[1],-c[0]};};
 }
 /** Turns a point offset from the anchor cell's min corner about the anchor cell centre by a structure rotation. */
 static double[] turnPoint(int s,double[] p){
  double dx=p[0]-.5,dz=p[2]-.5;
  double[] t=switch(s){case 0->new double[]{dx,dz};case 90->new double[]{-dz,dx};case 180->new double[]{-dx,-dz};default->new double[]{dz,-dx};};
  return new double[]{t[0]+.5,p[1],t[1]+.5};
 }

 public static void main(String[] args)throws Exception{
  for(String id:IDS){
   PropDef d=PropDef.parse(Files.readString(Path.of("src/main/resources/data/gravesandglory/gg_props/"+id+".json")));
   int px=(d.sx()-1)/2,pz=(d.sz()-1)/2;
   for(int lx=0;lx<d.sx();lx++)for(int ly=0;ly<d.sy();ly++)for(int lz=0;lz<d.sz();lz++){
    int[] c=PropGeometry.cell(d,0,lx,ly,lz);
    check(c[0]==lx-px&&c[1]==ly&&c[2]==lz-pz,id+": facing 0 is the offset from the pivot");
    for(int f:FACINGS)for(int s:FACINGS){
     int[] a=turnCell(s,PropGeometry.cell(d,f,lx,ly,lz)),b=PropGeometry.cell(d,PropGeometry.turnDeg(f,s),lx,ly,lz);
     check(a[0]==b[0]&&a[1]==b[1]&&a[2]==b[2],id+": cell composition f="+f+" s="+s);
     double[] pa=turnPoint(s,PropGeometry.point(d,f,lx+.25,ly+.5,lz+.75)),pb=PropGeometry.point(d,PropGeometry.turnDeg(f,s),lx+.25,ly+.5,lz+.75);
     check(near(pa[0],pb[0])&&near(pa[1],pb[1])&&near(pa[2],pb[2]),id+": point composition f="+f+" s="+s);
    }
   }
   for(int f:FACINGS){
    Set<String> seen=new HashSet<>();
    for(int[] cell:d.solid()){int[] w=PropGeometry.cell(d,f,cell[0],cell[1],cell[2]);seen.add(w[0]+","+w[1]+","+w[2]);}
    check(seen.size()==d.solid().size(),id+": footprint cells stay distinct and same count at facing "+f);
    double[] t=PropGeometry.partTransform(d,f);
    check(near(t[0],.5)&&near(t[1],.5)&&near(t[2],.5)&&near(t[3],(180+f)%360),id+": part at the anchor cell centre, yaw 180+facing at "+f);
   }
   double[] centre=PropGeometry.point(d,90,px+.5,0,pz+.5);
   check(near(centre[0],.5)&&near(centre[1],0)&&near(centre[2],.5),id+": the anchor cell centre stays put under rotation");
   double[] corner=PropGeometry.point(d,0,px,0,pz);
   check(near(corner[0],0)&&near(corner[2],0),id+": at facing 0 the anchor cell min corner is the point origin");
  }
  for(int f:FACINGS)check(PropGeometry.facingOf(PropGeometry.direction(f))==f,"facingOf(direction("+f+"))=="+f);
  check(PropGeometry.facingOf("north")==0&&PropGeometry.facingOf("east")==90&&PropGeometry.facingOf("south")==180&&PropGeometry.facingOf("west")==270,"Command-block facing names map clockwise from north");
  check(PropGeometry.facingOf("up")==-1&&PropGeometry.facingOf("north_up")==-1&&PropGeometry.facingOf("")==-1&&PropGeometry.facingOf(null)==-1,"Unknown direction is -1");
  check(PropGeometry.turnDeg(270,180)==90&&PropGeometry.turnDeg(0,0)==0&&PropGeometry.turnDeg(90,-90)==0,"turnDeg wraps to 0..359");
  PropDef forge=PropDef.parse(Files.readString(Path.of("src/main/resources/data/gravesandglory/gg_props/forge.json")));
  int[] o=PropGeometry.cell(forge,90,0,0,0);
  check(1+o[0]==1&&o[1]==0&&o[2]==-1,"Forge at 90 with its anchor on the pivot (1,0,0) puts local (0,0,0) at (1,0,-1): offset "+o[0]+","+o[1]+","+o[2]);
  int[] e=PropGeometry.cell(forge,90,2,0,0);
  check(e[0]==0&&e[2]==1,"Forge at 90: the east end turns south");
  double[] q=PropGeometry.point(forge,90,0,0,0);
  check(near(q[0],1)&&near(q[2],-1),"Forge at 90: the footprint's north-west corner turns to the north-east corner of its turned cell (1,-1)");
  System.out.println("PASS "+count+" prop geometry scenarios");
 }
}
