package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;

/** The deploy guard on every bundled engine map (Kort's playtest 2026-11-13: troops spawned on roofs inside the New Kingsfield base).
 * For both teams, every cell a troop can be placed on at round start or when trained at the barracks (the base's lineup and ring area:
 * 0 to 24 ahead of the spawn, 44 to each side) goes through EngineMap.footing, and the spot it gives must be on ground the spawn
 * reaches on foot (no roof, wall top, tree or closed interior), dry, no higher than one step above the yard, and not on a crate pile (EngineMap.stands). */
public final class DeployGroundCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of("src/main/resources/data/gravesandglory/sectionmaps");
  if(!Files.exists(root))root=Path.of(System.getProperty("user.dir")).resolve(root);
  int maps=0;
  for(var row:Spec.MAPS){if(row.source().equals("-"))continue;maps++;
   EngineMap m=EngineMap.read(root.resolve(row.source()));
   for(EngineMap.Spawn s:m.spawns){int sx=(int)Math.floor(s.x()),sz=(int)Math.floor(s.z()),side=sx<0?1:-1;
    check(m.stands(sx,sz,s.y()),row.id()+" "+s.team()+": the spawn itself is walkable ground");
    int sampled=0,moved=0,off=0;
    for(int u=0;u<=24;u++)for(int dz=-44;dz<=44;dz++){int x=sx+side*u,z=sz+dz;sampled++;
     if(!m.stands(x,z,s.y()))off++;
     int[] c=m.footing(x,z,sx,48,s.y(),null);
     check(c!=null,row.id()+" "+s.team()+": no ground for the deploy cell "+x+","+z);
     if(c[0]!=x||c[1]!=z)moved++;
     check(m.walkable(c[0],c[1]),row.id()+" "+s.team()+": deploy cell "+x+","+z+" stands on "+c[0]+","+c[1]+" (y "+m.solid(c[0],c[1])+"), not ground the spawn walks to");
     check(!m.wet(c[0],c[1]),row.id()+" "+s.team()+": deploy cell "+x+","+z+" stands in water at "+c[0]+","+c[1]);
     check(m.solid(c[0],c[1])<=s.y(),row.id()+" "+s.team()+": deploy cell "+x+","+z+" stands at y "+(m.solid(c[0],c[1])+1)+", above the yard (spawn feet y "+s.y()+")");
     check(Math.abs(c[0]-x)+Math.abs(c[1]-z)<=48,row.id()+": a deploy spot moved far: "+x+","+z+" to "+c[0]+","+c[1]);}
    check(off==moved&&off>0,row.id()+" "+s.team()+": exactly the cells off the ground move ("+off+" off, "+moved+" moved)");
    System.out.println("DeployGroundCheck "+row.id()+" "+s.team()+": "+sampled+" deploy cells, "+off+" off the ground (roof, wall, tree, pile, water) and moved to the nearest ground");}}
  check(maps>=2,"both engine maps are checked (New Kingsfield, New Rivergate): "+maps);
  System.out.println("DeployGroundCheck: PASS ("+count+" checks)");
 }
}
