package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;

/** The spawn guard on every bundled engine map (Kort's playtest 2026-11-13: troops spawned on roofs inside the New Kingsfield base).
 * Base deploys: for both teams, every cell a troop can be placed on at round start or when trained at the barracks (the lineup and ring area:
 * 0 to 24 ahead of the spawn, 56 to each side, room for five armies a team) goes through EngineMap.footing as Arena.standing calls it, and the
 * spot must be ground (reached on foot from a spawn or point over the column tops: no roof, wall top, tree or closed building), in the team's
 * own yard (EngineMap.nearHome: not across its wall), dry, no higher than one block above the yard, and not on a crate pile.
 * Forward deploys: every cell within 14 of each point, as Arena.forward calls it, stands on ground no higher than the point's plus one.
 * The world's headroom test (Arena.guarded) needs a world: the bot sim's spawn-ground.csv covers it. */
public final class DeployGroundCheck {
 static final double[] EDGES={0.0,0.15,0.5,0.85,0.999};
 /** Whether a body of half width h on (x, z) stays inside cells that stand (the cell it is on and any neighbour it reaches into). */
 static boolean clear(EngineMap m,double x,double z,double h,int max){
  for(int cx=(int)Math.floor(x-h);cx<=(int)Math.floor(x+h);cx++)for(int cz=(int)Math.floor(z-h);cz<=(int)Math.floor(z+h);cz++)if(!m.stands(cx,cz,max))return false;
  return true;}
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of("src/main/resources/data/gravesandglory/sectionmaps");
  if(!Files.exists(root))root=Path.of(System.getProperty("user.dir")).resolve(root);
  int maps=0;
  for(var row:Spec.MAPS){if(row.source().equals("-"))continue;maps++;
   EngineMap m=EngineMap.read(root.resolve(row.source()));
   for(EngineMap.Spawn s:m.spawns){int sx=(int)Math.floor(s.x()),sz=(int)Math.floor(s.z()),side=s.team().equals("dawn")?1:-1;
    check(m.home(s.x())==s,row.id()+" "+s.team()+": the spawn is its own side's home");
    check(m.stands(sx,sz,s.y())&&m.nearHome(s,sx,sz),row.id()+" "+s.team()+": the spawn itself is yard ground");
    for(int u=0;u<=24;u++)for(int dz=-56;dz<=56;dz+=8)check(m.inYard(s,sx+side*u,sz+dz),row.id()+" "+s.team()+": base deploy cell "+(sx+side*u)+","+(sz+dz)+" counts as its yard");
    for(EngineMap.Point p:m.points)check(!m.inYard(s,p.x(),p.z()),row.id()+" "+s.team()+": point "+p.name()+" is a forward spot, not its yard");
    int sampled=0,moved=0,off=0;
    for(int u=0;u<=24;u++)for(int dz=-56;dz<=56;dz++){int x=sx+side*u,z=sz+dz;sampled++;
     if(!m.stands(x,z,s.y())||!m.nearHome(s,x,z))off++;
     int[] c=m.footing(x,z,s.x(),48,s.y(),s,null);
     check(c!=null,row.id()+" "+s.team()+": no ground for the deploy cell "+x+","+z);
     if(c[0]!=x||c[1]!=z)moved++;
     String at=row.id()+" "+s.team()+": deploy cell "+x+","+z+" stands on "+c[0]+","+c[1]+" (ground y "+m.solid(c[0],c[1])+")";
     check(m.walkable(c[0],c[1]),at+": not ground a troop walks to (roof, wall top, tree)");
     check(m.nearHome(s,c[0],c[1]),at+": outside its own yard");
     check(!m.wet(c[0],c[1]),at+": in water");
     check(m.solid(c[0],c[1])<=s.y(),at+": above the yard (spawn feet y "+s.y()+")");
     check(Math.hypot(c[0]-x,c[1]-z)<=48,at+": moved more than 48");
     if(c[0]==x&&c[1]==z)for(double fx:EDGES)for(double fz:EDGES){double[] q=m.inside(x+fx,z+fz,EngineMap.BODY,s.y(),null);
      check(Math.floor(q[0])==x&&Math.floor(q[1])==z,at+": the spot "+(x+fx)+","+(z+fz)+" left its cell");
      check(clear(m,q[0],q[1],0.3,s.y()),at+": a troop on "+(x+fx)+","+(z+fz)+" (kept at "+q[0]+","+q[1]+") overlaps a neighbour that is not yard ground (wall post, pile, water)");}}
    check(off==moved&&off>0,row.id()+" "+s.team()+": exactly the cells off the yard ground move ("+off+" off, "+moved+" moved)");
    System.out.println("DeployGroundCheck "+row.id()+" "+s.team()+": "+sampled+" base deploy cells, "+off+" off the yard ground (roof, wall, tree, pile, water, across the wall) and moved");}
   for(EngineMap.Point p:m.points){int px=(int)Math.floor(p.x()),pz=(int)Math.floor(p.z()),max=m.solid(px,pz)+1,moved=0,n=0;
    check(m.walkable(px,pz),row.id()+" point "+p.name()+" is walkable ground");
    for(int dx=-14;dx<=14;dx++)for(int dz=-14;dz<=14;dz++){if(dx*dx+dz*dz>14*14)continue;int x=px+dx,z=pz+dz;n++;
     int[] c=m.footing(x,z,m.spawns.get(0).x(),48,max,null,null);
     check(c!=null,row.id()+" point "+p.name()+": no ground for the forward deploy cell "+x+","+z);
     if(c[0]!=x||c[1]!=z)moved++;
     check(m.stands(c[0],c[1],max),row.id()+" point "+p.name()+": forward cell "+x+","+z+" stands on "+c[0]+","+c[1]+" (ground y "+m.solid(c[0],c[1])+", point ground "+(max-1)+")");}
    System.out.println("DeployGroundCheck "+row.id()+" point "+p.name()+": "+n+" forward deploy cells, "+moved+" moved off a roof, pile or water");}}
  check(maps>=2,"both engine maps are checked (New Kingsfield, New Rivergate): "+maps);
  System.out.println("DeployGroundCheck: PASS ("+count+" checks)");
 }
}
