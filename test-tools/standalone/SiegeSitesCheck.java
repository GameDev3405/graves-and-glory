package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Every Siege fort on a bundled engine map: the yard, the keep, the musters and the posts stand on floors a troop can reach at the height their rows
 *  state; with every plug shut no walk (four ways, steps of one block) leads from the yard to the keep or to the defenders' muster, so the gate is the
 *  only way in; with the gate's own plug open and the others shut the walk gets there through the gate; and the spot the gate's target stands on, one
 *  block off the plug's face on the attackers' side, is a floor at the gate row's height. The plug boxes are SiegeMode.plugBox's. */
public final class SiegeSitesCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static long key(int x,int y,int z){return ((long)(x+4096)<<40)|((long)(z+4096)<<20)|(y+512);}
 /** A plug's columns: x0, z0, x1, z1 (thickness seconds along its passage, radius each side of the centre across it; rotation 90 or 270: the passage runs along z). */
 static int[] box(Spec.ModeSitesRow p){
  int thick=p.seconds(),half=(int)p.radius();boolean turned=Math.floorMod(p.rotation(),180)==90;
  return turned?new int[]{(int)p.x()-half,(int)p.z()-thick/2,(int)p.x()+half,(int)p.z()-thick/2+thick-1}:new int[]{(int)p.x()-thick/2,(int)p.z()-half,(int)p.x()-thick/2+thick-1,(int)p.z()+half};
 }
 /** Whether a walk from one floor reaches the column of another with the boxes shut. */
 static boolean walks(EngineMap m,int[] from,int[] to,List<int[]> shut){
  var seen=new HashSet<Long>();var q=new ArrayDeque<int[]>();q.add(new int[]{from[0],from[2],from[1]});seen.add(key(from[0],from[2],from[1]));
  int[][] d={{1,0},{-1,0},{0,1},{0,-1}};
  while(!q.isEmpty()){int[] c=q.poll();if(c[0]==to[0]&&c[2]==to[1]&&Math.abs(c[1]-to[2])<=1)return true;
   for(int[] e:d){int nx=c[0]+e[0],nz=c[2]+e[1],ny=m.nav.floorNear(nx,c[1],nz);if(ny==Integer.MIN_VALUE||Math.abs(ny-c[1])>1)continue;
    boolean in=false;for(int[] b:shut)if(nx>=b[0]&&nx<=b[2]&&nz>=b[1]&&nz<=b[3])in=true;
    if(!in&&seen.add(key(nx,ny,nz)))q.add(new int[]{nx,ny,nz});}}
  return false;
 }
 static int[] stand(EngineMap m,Spec.ModeSitesRow s,String at){
  int x=(int)Math.floor(s.x()),z=(int)Math.floor(s.z());int[] f=m.nav.stand(x,z,m::solid,x+.5,z+.5,0);
  check(f!=null,at+": no floor a troop can reach");return f;
 }
 public static void main(String[] args)throws Exception{
  Path root=Path.of("src/main/resources/data/gravesandglory/sectionmaps");
  if(!Files.exists(root))root=Path.of(System.getProperty("user.dir")).resolve(root);
  int maps=0;
  for(int map=0;map<Spec.MAPS.length;map++){var row=Spec.MAPS[map];if(row.source().equals("-"))continue;
   Spec.ModeSitesRow yard=null,gate=null,keep=null;var plugs=new ArrayList<Spec.ModeSitesRow>();var bases=new ArrayList<Spec.ModeSitesRow>();var posts=new ArrayList<Spec.ModeSitesRow>();
   for(var s:Spec.MODESITES){if(!s.mode().equals("siege")||s.map()!=map)continue;
    switch(s.kind()){case "stage"->{if(s.stage()==0)yard=s;else keep=s;}case "gate"->gate=s;case "plug"->plugs.add(s);case "base"->bases.add(s);case "post"->posts.add(s);default->{}}}
   if(gate==null)continue;
   String id=row.id();
   EngineMap m=EngineMap.read(root.resolve(row.source()));
   check(yard!=null&&keep!=null&&!plugs.isEmpty()&&!bases.isEmpty(),id+": a yard, a keep, a plug and a defenders' muster");
   Spec.ModeSitesRow muster=null;
   for(var b:bases)if(muster==null||Math.hypot(b.x()-keep.x(),b.z()-keep.z())<Math.hypot(muster.x()-keep.x(),muster.z()-keep.z()))muster=b;
   int[] y0=stand(m,yard,id+" "+yard.id()),k0=stand(m,keep,id+" "+keep.id()),b0=stand(m,muster,id+" "+muster.id());
   check(b0[2]==(int)muster.y(),id+" "+muster.id()+": stands on ground y "+b0[2]+", the row says "+(int)muster.y());
   for(var b:bases)if(b!=muster){check(b.stage()!=muster.stage(),id+" "+b.id()+": the second base row is the attackers' camp, of the other team");int[] c=stand(m,b,id+" "+b.id());
    check(walks(m,c,y0,List.of()),id+" "+b.id()+": the attackers' camp walks to the yard");}
   for(var p:posts){int[] f=stand(m,p,id+" "+p.id());check(f[2]==(int)p.y(),id+" "+p.id()+": stands on ground y "+f[2]+", the row says "+(int)p.y());}
   for(var s:new Spec.ModeSitesRow[]{yard,keep}){int r=(int)Math.ceil(s.radius()),cells=0,good=0;int x=(int)Math.floor(s.x()),z=(int)Math.floor(s.z());int[] c=s==yard?y0:k0;
    for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){if(Math.hypot(dx,dz)>s.radius())continue;cells++;int[] f=m.nav.stand(x+dx,z+dz,m::solid,x+dx+.5,z+dz+.5,0);if(f!=null&&Math.abs(f[2]-c[2])<=3)good++;}
    check(good*4>=cells*3,id+" "+s.id()+": only "+good+" of "+cells+" cells of the ring have a floor near y "+c[2]);}
   var shut=new ArrayList<int[]>();Spec.ModeSitesRow own=null;
   for(var p:plugs){shut.add(box(p));if(p.x()==gate.x()&&p.z()==gate.z())own=p;}
   check(own!=null,id+": a plug row at the gate");
   check(walks(m,y0,k0,List.of()),id+": the yard walks to the keep with the gate open");
   check(!walks(m,y0,k0,shut),id+": with every plug shut a walk still leads from the yard to the keep");
   check(!walks(m,y0,b0,shut),id+": with every plug shut a walk still leads from the yard to the defenders' muster");
   check(walks(m,b0,k0,shut),id+": the defenders' muster walks to the keep behind the shut gate");
   for(var p:posts)check(walks(m,b0,stand(m,p,id),shut),id+" "+p.id()+": the defenders' muster walks to the post behind the shut gate");
   var others=new ArrayList<int[]>();for(var p:plugs)if(p!=own)others.add(box(p));
   check(walks(m,y0,k0,others),id+": the gate alone does not open the way to the keep");
   int turn=Math.floorMod(gate.rotation(),360),ux=turn==0?1:turn==180?-1:0,uz=turn==90?1:turn==270?-1:0,pass=own.seconds()/2+1;
   int tx=(int)gate.x()-ux*(pass+1),tz=(int)gate.z()-uz*(pass+1),ty=m.nav.floorNear(tx,(int)gate.y(),tz);
   check(ty==(int)gate.y(),id+": the gate's target stands at "+tx+","+tz+" on floor "+ty+", the gate row says "+(int)gate.y());
   check(walks(m,y0,new int[]{tx,tz,ty},shut),id+": the yard walks to the gate's face");
   maps++;System.out.println(id+": the fort is shut by "+plugs.size()+" plugs, "+(muster.stage()==0?"Dawn":"Dusk")+" defends");
  }
  check(maps>=5,"Siege on five engine maps, found "+maps);
  System.out.println("SiegeSitesCheck OK: "+maps+" maps, "+count+" checks");
 }
}
