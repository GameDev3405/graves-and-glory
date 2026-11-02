package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** EngineMap (map engine Plan 2 Task 2) on the committed fixture test-tools/resources/fixtures/engine-map: the reader,
 * the world placement, the walking distances against the engine's own, and the post-build check. */
public final class EngineMapCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static boolean has(List<String> issues,String code){return issues.stream().anyMatch(s->s.startsWith(code+":"));}
 public static void main(String[] args)throws Exception{
  Path dir=Path.of("test-tools/resources/fixtures/engine-map");
  if(!Files.exists(dir.resolve("map.json")))dir=Path.of(System.getProperty("user.dir")).resolve(dir);
  EngineMap m=EngineMap.read(dir);
  check(m.width==128&&m.depth==72&&m.ox==-64&&m.oz==-36,"canvas and origin: "+m.width+"x"+m.depth+" at "+m.ox+","+m.oz);
  check(m.spawns.size()==2&&m.points.size()==2&&m.routes.size()==4&&m.roads.size()==1,"objects read");
  EngineMap.Spawn dawn=m.spawns.get(0);
  check(dawn.team().equals("dawn")&&dawn.x()==10-64&&dawn.z()==0,"dawn spawn in world coordinates: "+dawn);
  check(m.solid((int)dawn.x(),(int)dawn.z())+1==dawn.y(),"the spawn stands on its column");
  check(m.solid(-1000,-1000)==m.solid(-64,-36),"columns past the canvas repeat the edge");
  check(m.riverX(0)>-8&&m.riverX(0)<8,"the river runs near x 0 in world: "+m.riverX(0));
  boolean dry=true;for(int x=-64;x<64;x++)if(m.wet(x,0)&&Math.abs(x-m.riverX(0))>4)dry=false;
  check(dry,"water only near the river line");
  // the Java walk equals the engine's
  for(EngineMap.Spawn s:m.spawns){int[] d=EngineMap.distances(m.solid,m.water,m.width,m.depth,m.cellX(s.x()),m.cellZ(s.z()));
   for(EngineMap.Point p:m.points){int walk=d[m.cellZ(p.z())*m.width+m.cellX(p.x())];
    EngineMap.Route r=m.routes.stream().filter(q->q.spawn().equals(s.id())&&q.point().equals(p.name())).findFirst().get();
    check(r.walk()!=null&&r.walk()==walk,s.id()+" to "+p.name()+": Java walks "+walk+", engine "+r.walk());}}
  // the clean grid gives no issues
  List<String> clean=EngineMap.check(m,m.solid.clone(),m.water.clone());
  check(clean.isEmpty(),"the design's own columns pass: "+clean);
  // a raised column under the dawn spawn: columns + moved
  int[] solid=m.solid.clone();int i=m.cellZ(dawn.z())*m.width+m.cellX(dawn.x());solid[i]+=3;
  List<String> raised=EngineMap.check(m,solid,m.water.clone());
  check(has(raised,"columns")&&has(raised,"moved"),"a raised spawn column is reported: "+raised);
  // a flooded point
  EngineMap.Point a=m.points.get(0);int[] water=m.water.clone();int j=m.cellZ(a.z())*m.width+m.cellX(a.x());water[j]=m.solid[j]+3;
  check(has(EngineMap.check(m,m.solid.clone(),water),"in_water"),"a flooded point is reported");
  // a cliff across the canvas: unreachable
  solid=m.solid.clone();int wall=m.cellX(a.x())+8;for(int z=0;z<m.depth;z++)solid[z*m.width+wall]+=10;
  List<String> cut=EngineMap.check(m,solid,m.water.clone());
  check(has(cut,"unreachable"),"a cliff cutting the map is reported: "+cut);
  // a wall with a gap at the north edge: a longer walk than the design
  solid=m.solid.clone();for(int z=6;z<m.depth;z++)solid[z*m.width+wall]+=10;
  check(has(EngineMap.check(m,solid,m.water.clone()),"distance"),"a longer walk is reported");
  System.out.println("EngineMapCheck: "+count+" checks passed");
 }
}
