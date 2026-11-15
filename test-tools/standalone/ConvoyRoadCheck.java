package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Every Convoy road on a bundled engine map, walked the way the wagon slides it (ConvoyRoad.floors): each sample stands on a floor a troop
 *  can reach, the road never steps more than one block between samples half a block apart, the road is at least five wide on that level at
 *  every sample (the wagon and a pusher each side), and a node with a height of its own stands on a floor at that height. */
public final class ConvoyRoadCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of("src/main/resources/data/gravesandglory/sectionmaps");
  if(!Files.exists(root))root=Path.of(System.getProperty("user.dir")).resolve(root);
  int roads=0;
  for(var row:Spec.MAPS){if(row.source().equals("-"))continue;
   int map=-1;for(int i=0;i<Spec.MAPS.length;i++)if(Spec.MAPS[i]==row)map=i;
   List<ConvoyRoad.Node> road=ConvoyRoad.of(map);
   if(road.size()<2)continue;
   roads++;
   EngineMap m=EngineMap.read(root.resolve(row.source()));
   double[][] nodes=ConvoyRoad.nodes(road);
   double[] st=ModeMath.stations(nodes);
   int[] f=ConvoyRoad.floors(m,road);
   for(var n:road)if(n.y()!=0)check(m.nav.floorAt((int)Math.floor(n.x()),(int)Math.floor(n.y())-1,(int)Math.floor(n.z())),row.id()+" road node "+n.x()+","+n.z()+": no reachable floor to stand on at y "+n.y());
   int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
   for(int k=0;k<f.length;k++){double d=Math.min(st[st.length-1],k*ConvoyRoad.STEP);double[] p=ModeMath.along(nodes,st,d);
    int x=(int)Math.floor(p[0]),z=(int)Math.floor(p[1]);String at=row.id()+" road at "+x+","+z+" ("+(int)d+" along)";
    check(m.nav.floorAt(x,f[k],z),at+": no reachable floor at y "+f[k]);
    if(k>0)check(Math.abs(f[k]-f[k-1])<=1,at+": steps "+f[k-1]+" to "+f[k]);
    int seg=ModeMath.segment(st,d);double dx=nodes[seg+1][0]-nodes[seg][0],dz=nodes[seg+1][1]-nodes[seg][1],len=Math.hypot(dx,dz);
    for(int side=-2;side<=2;side++){int qx=(int)Math.floor(p[0]-dz/len*side),qz=(int)Math.floor(p[1]+dx/len*side);int q=m.nav.floorNear(qx,f[k],qz);
     check(q!=Integer.MIN_VALUE&&Math.abs(q-f[k])<=1,at+": the road is not five wide here ("+qx+","+qz+" has "+(q==Integer.MIN_VALUE?"no floor":"floor "+q)+" beside "+f[k]+")");}
    low=Math.min(low,f[k]);high=Math.max(high,f[k]);}
   System.out.println(row.id()+": road "+(int)st[st.length-1]+" long, floors "+low+".."+high+", "+f.length+" samples");
  }
  check(roads>0,"no engine map has a Convoy road");
  System.out.println("ConvoyRoadCheck OK: "+roads+" roads, "+count+" checks");
 }
}
