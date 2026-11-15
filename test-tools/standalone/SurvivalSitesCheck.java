package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;

/** Every Survival site on a bundled engine map: the shrine, the court and each rise site stand on a floor a troop can reach (the route field
 *  keeps only floors reached on foot from the spawns and points, so the dead can walk from where they rise to the shrine), at the height the
 *  row states (a shrine or rise row's y is its ground block, the court's is where the defenders stand), and nine in ten cells of the shrine
 *  ring and of each rise square have such a floor within two blocks of that height, so the ring is one place and the dead never rise in a wall. */
public final class SurvivalSitesCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of("src/main/resources/data/gravesandglory/sectionmaps");
  if(!Files.exists(root))root=Path.of(System.getProperty("user.dir")).resolve(root);
  int maps=0;
  for(int map=0;map<Spec.MAPS.length;map++){var row=Spec.MAPS[map];if(row.source().equals("-"))continue;
   EngineMap m=null;int sites=0;
   for(var s:Spec.MODESITES){if(!s.mode().equals("survival")||s.map()!=map)continue;
    if(m==null)m=EngineMap.read(root.resolve(row.source()));
    sites++;
    int x=(int)Math.floor(s.x()),z=(int)Math.floor(s.z());String at=row.id()+" "+s.id()+" at "+x+","+z;
    int[] f=m.nav.stand(x,z,m::solid,x+.5,z+.5,0);
    check(f!=null,at+": no floor a troop can reach");
    int ground=s.kind().equals("base")?(int)s.y()-1:(int)s.y();
    check(f[2]==ground,at+": stands on ground y "+f[2]+", the row says "+ground);
    if(s.radius()<=0)continue;
    int r=(int)Math.ceil(s.radius()),cells=0,good=0;
    for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){if(s.kind().equals("shrine")&&Math.hypot(dx,dz)>s.radius())continue;
     cells++;int q=m.nav.floorNear(x+dx,ground,z+dz);if(q!=Integer.MIN_VALUE&&Math.abs(q-ground)<=2)good++;}
    check(good*10>=cells*9,at+": only "+good+" of "+cells+" cells within "+s.radius()+" have a floor near y "+ground);
   }
   if(m!=null){maps++;check(sites>=6,row.id()+": a shrine, a court and four rise sites");System.out.println(row.id()+": "+sites+" survival sites stand");}
  }
  check(maps>0,"no engine map hosts Survival");
  System.out.println("SurvivalSitesCheck OK: "+maps+" maps, "+count+" checks");
 }
}
