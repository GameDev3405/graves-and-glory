package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A battle map built by the map engine (tools/mapengine build): its manifest (map.json) and column grid (columns.bin),
 * in world coordinates. No Minecraft types, so it is checked standalone (EngineMapCheck). The walking rule matches
 * tools/mapengine/walk.py: 4-neighbour steps, at most MAX_STEP up or down, water at most MAX_WADE deep. */
public final class EngineMap {
 public static final int NONE=-32768,MAX_STEP=1,MAX_WADE=1;
 public static final double DETOUR=2.0;
 public record Point(String id,String name,double x,double z,int y,int radius){}
 public record Spawn(String id,String team,double x,double z,int y){}
 public record Route(String spawn,String point,double straight,Integer walk){}
 /** format "stack" (tools/sectionmap.py: size, palette, a big-endian int16 palette index per cell) or a structure template */
 public record Tile(String file,String format,int x,int z,int sx,int sy,int sz){}
 /** Opens a file of the build by its name in map.json: a folder (the opt-in -Dgravesandglory.engineMap) or a folder inside the mod (a bundled map). */
 public interface Opener{InputStream open(String name)throws IOException;}
 public record Road(String id,List<double[]> path,double width){}
 public final Opener files;public final String id,sha;public final int width,depth,ox,oz,baseY,floorY;
 /** the field (playable half sizes) the build names, 0 when it names none; the maps sheet row is what the game plays */
 public final int fieldHalfX,fieldHalfZ;
 /** routes were walked over every floor of a column (tools/sectionmap.py), not over the top block only: check() then compares columns, points and spawns, not walks */
 public final boolean floorRoutes;
 public final int[] solid,water;
 public final List<Point> points=new ArrayList<>();public final List<Spawn> spawns=new ArrayList<>();
 public final List<Route> routes=new ArrayList<>();public final List<Tile> tiles=new ArrayList<>();public final List<Road> roads=new ArrayList<>();
 private final double[] riverX;
 /** The route field baked from the map's own floors (nav.bin.gz, tools/sectionmap.py nav), or null when the build has none or it is stale: troops then walk as they always did. */
 public final NavField nav;

 private EngineMap(Opener files,JsonObject m,int[] solid,int[] water){
  this.files=files;id=m.get("id").getAsString();sha=m.get("design_sha256").getAsString();
  width=m.get("width").getAsInt();depth=m.get("depth").getAsInt();
  JsonArray o=m.getAsJsonArray("origin");ox=o.get(0).getAsInt();oz=o.get(1).getAsInt();
  baseY=m.get("baseY").getAsInt();floorY=m.get("floorY").getAsInt();this.solid=solid;this.water=water;
  JsonObject f=m.has("field")?m.getAsJsonObject("field"):null;fieldHalfX=f==null?0:f.get("halfX").getAsInt();fieldHalfZ=f==null?0:f.get("halfZ").getAsInt();
  floorRoutes=m.has("routesBy")&&m.get("routesBy").getAsString().equals("floors");
  for(JsonElement e:m.getAsJsonArray("points")){JsonObject p=e.getAsJsonObject();points.add(new Point((p.has("id")?p.get("id"):p.get("name")).getAsString(),p.get("name").getAsString(),p.get("x").getAsDouble()+ox,p.get("z").getAsDouble()+oz,p.get("y").getAsInt(),p.get("radius").getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("spawns")){JsonObject p=e.getAsJsonObject();spawns.add(new Spawn(p.get("id").getAsString(),p.get("team").getAsString(),p.get("x").getAsDouble()+ox,p.get("z").getAsDouble()+oz,p.get("y").getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("routes")){JsonObject p=e.getAsJsonObject();JsonElement w=p.get("walk");routes.add(new Route(p.get("spawn").getAsString(),p.get("point").getAsString(),p.get("straight").getAsDouble(),w==null||w.isJsonNull()?null:w.getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("tiles")){JsonObject p=e.getAsJsonObject();JsonArray s=p.getAsJsonArray("size");tiles.add(new Tile(p.get("file").getAsString(),p.has("format")?p.get("format").getAsString():"structure",p.get("x").getAsInt()+ox,p.get("z").getAsInt()+oz,s.get(0).getAsInt(),s.get(1).getAsInt(),s.get(2).getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("roads")){JsonObject p=e.getAsJsonObject();List<double[]> path=new ArrayList<>();for(JsonElement q:p.getAsJsonArray("path")){JsonArray a=q.getAsJsonArray();path.add(new double[]{a.get(0).getAsDouble()+ox,a.get(1).getAsDouble()+oz});}roads.add(new Road(p.get("id").getAsString(),path,p.get("width").getAsDouble()));}
  walkable(ox,oz); // the walkable grid is built when the map loads (a few ms), not on the first troop's tick
  nav=loadNav();
  riverX=new double[depth];
  for(int z=0;z<depth;z++){double sum=0;int n=0;for(int x=0;x<width;x++)if(wetCell(water[z*width+x],solid[z*width+x])){sum+=x+ox;n++;}riverX[z]=n==0?-100000:sum/n;}
 }

 private NavField loadNav(){
  if(System.getenv("GRAVES_NO_NAV")!=null)return null; // test control: the walk as before the route field
  List<int[]> seeds=new ArrayList<>();
  for(Spawn s:spawns)seeds.add(new int[]{(int)Math.floor(s.x()),(int)Math.floor(s.z()),s.y()-1});
  for(Point p:points)seeds.add(new int[]{(int)Math.floor(p.x()),(int)Math.floor(p.z()),p.y()-1});
  try(InputStream in=files.open("nav.bin.gz")){
   NavField n=NavField.read(in,ox,oz,width,depth,floorY,sha,seeds.toArray(new int[0][]));
   System.out.println("[Graves and Glory] route field of "+id+": "+n.summary());return n;
  }catch(IOException e){System.out.println("[Graves and Glory] no route field for "+id+": "+e.getMessage());return null;}
 }

 /** Reads dir/map.json and its columns.bin; throws IOException with a reason when either is missing or does not fit. */
 public static EngineMap read(Path dir)throws IOException{return read(name->Files.newInputStream(dir.resolve(name)),dir.toString());}
 /** A map bundled in the mod under /root (data/gravesandglory/sectionmaps/<id>). */
 public static EngineMap resource(String root)throws IOException{
  return read(name->{InputStream in=EngineMap.class.getResourceAsStream("/"+root+"/"+name);if(in==null)throw new IOException("/"+root+"/"+name+" is not in the mod");return in;},root);
 }
 static EngineMap read(Opener files,String where)throws IOException{
  JsonObject m;try(InputStream in=files.open("map.json")){m=JsonParser.parseString(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();}
  if(!m.has("width")||!m.has("routes"))throw new IOException(where+"/map.json is from an older engine: rebuild it with python tools/mapengine build");
  int w=m.get("width").getAsInt(),d=m.get("depth").getAsInt();
  String columns=m.has("columns")?m.get("columns").getAsString():"columns.bin";byte[] raw;
  try(InputStream in=files.open(columns);InputStream body=columns.endsWith(".gz")?new GZIPInputStream(in):in){raw=body.readAllBytes();}
  if(raw.length!=4*w*d)throw new IOException("columns.bin holds "+raw.length+" bytes, expected "+4*w*d);
  ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
  int[] solid=new int[w*d],water=new int[w*d];
  for(int i=0;i<w*d;i++)solid[i]=b.getShort();
  for(int i=0;i<w*d;i++)water[i]=b.getShort();
  return new EngineMap(files,m,solid,water);
 }

 static boolean wetCell(int water,int solid){return water!=NONE&&water>solid;}
 int index(int wx,int wz){int x=Math.max(0,Math.min(width-1,wx-ox)),z=Math.max(0,Math.min(depth-1,wz-oz));return z*width+x;}
 /** world y of the top solid block of the column at world x, z (edge columns repeat past the canvas) */
 public int solid(int wx,int wz){return solid[index(wx,wz)];}
 /** world y of the top water block, or NONE */
 public int water(int wx,int wz){return water[index(wx,wz)];}
 public boolean wet(int wx,int wz){int i=index(wx,wz);return wetCell(water[i],solid[i]);}
 private boolean[] walk;private int[][] fromSpawn;
 /** On foot (distances: 4-neighbour steps of at most MAX_STEP over the top of each column) from a base spawn or a capture point: the cell is
  *  ground a troop can reach, not a roof, wall top, tree crown or the top of a closed building. The points seed it too, because the column walk
  *  cannot pass under a roof (a gatehouse closes new Rivergate's bases; floorRoutes). Off the canvas: false. */
 public boolean walkable(int wx,int wz){
  if(!inside(wx,wz))return false;
  if(walk==null){boolean[] r=new boolean[width*depth];List<int[]> seeds=new ArrayList<>();
   for(Spawn s:spawns)seeds.add(new int[]{cellX(s.x()),cellZ(s.z())});for(Point p:points)seeds.add(new int[]{cellX(p.x()),cellZ(p.z())});
   for(int[] c:seeds){if(r[c[1]*width+c[0]])continue;int[] d=distances(solid,water,width,depth,c[0],c[1]);for(int i=0;i<d.length;i++)if(d[i]>=0)r[i]=true;}
   walk=r;}
  return walk[(wz-oz)*width+(wx-ox)];
 }
 /** The base spawn nearest world x (a team's own: the two bases sit at opposite ends of the field). Null on a map without spawns. */
 public Spawn home(double x){Spawn best=null;for(Spawn s:spawns)if(best==null||Math.abs(s.x()-x)<Math.abs(best.x()-x))best=s;return best;}
 /** Cell (x, z) is in base s's own yard: its walk from the spawn is at most 1.5 times the straight line plus 12 (a cell across a wall, reached
  *  round by the gate or not at all, is not). */
 public boolean nearHome(Spawn s,int x,int z){
  if(!inside(x,z))return false;
  if(fromSpawn==null){int[][] f=new int[spawns.size()][];for(int i=0;i<f.length;i++)f[i]=distances(solid,water,width,depth,cellX(spawns.get(i).x()),cellZ(spawns.get(i).z()));fromSpawn=f;}
  int d=fromSpawn[spawns.indexOf(s)][(z-oz)*width+(x-ox)];
  return d>=0&&d<=1.5*Math.hypot(x+.5-s.x(),z+.5-s.z())+12;
 }
 /** A troop may be placed on cell (x, z): walkable ground (no roof, wall top, tree crown or closed building top), dry, ground at most
  *  maxGround, and not on a pile: at most one block above the lowest of its eight neighbours (a crate stack or cart top is not a place). */
 public boolean stands(int x,int z,int maxGround){
  if(!walkable(x,z)||wet(x,z))return false;int g=solid(x,z);if(g>maxGround)return false;
  int low=g;for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)low=Math.min(low,solid(x+dx,z+dz));
  return g-low<=1;
 }
 /** Half the width a troop's body takes on its spot, with a hair to spare (a 0.6-wide troop): inside keeps it this far off a bad neighbour. */
 public static final double BODY=0.31;
 /** The spot (x, z), pulled inside its own cell so a body of half width h does not reach into a neighbouring cell (the eight round it) that
  *  does not stand (stands with maxGround, and also when given): a wall post, a fence, a pile or water on the edge of the cell. A troop placed on
  *  the line between a yard cell and a pool wall overlaps the wall's post and is pushed out of it, over the wall into the pool. Moves it 0.5 at
  *  most, inside the cell; a spot clear of every bad neighbour is kept. {x, z}. */
 public double[] inside(double x,double z,double h,int maxGround,java.util.function.BiPredicate<Integer,Integer> also){
  int ix=(int)Math.floor(x),iz=(int)Math.floor(z);double fx=x-ix,fz=z-iz;
  java.util.function.BiPredicate<Integer,Integer> ok=(dx,dz)->stands(ix+dx,iz+dz,maxGround)&&(also==null||also.test(ix+dx,iz+dz));
  if(!ok.test(-1,0))fx=Math.max(fx,h);if(!ok.test(1,0))fx=Math.min(fx,1-h);
  if(!ok.test(0,-1))fz=Math.max(fz,h);if(!ok.test(0,1))fz=Math.min(fz,1-h);
  for(int dx=-1;dx<=1;dx+=2)for(int dz=-1;dz<=1;dz+=2){ // a bad corner cell: off it along whichever axis is the shorter way
   double nx=dx<0?h-fx:fx-(1-h),nz=dz<0?h-fz:fz-(1-h);
   if(nx<=0||nz<=0||ok.test(dx,dz))continue;
   if(nx<=nz)fx-=dx*nx;else fz-=dz*nz;}
  return new double[]{ix+fx,iz+fz};
 }
 /** Where a troop placed at cell (x, z) stands: the cell itself when stands(x, z, maxGround) holds, in home's yard when home is given
  *  (nearHome), and also holds when given; else the nearest such cell within radius by straight distance, ties to the one nearest towardX
  *  (the troop's own base). Null when there is none. also is the caller's extra test (Arena.standing: two free blocks of headroom in the world). */
 public int[] footing(int x,int z,double towardX,int radius,int maxGround,Spawn home,java.util.function.BiPredicate<Integer,Integer> also){
  int[] best=null;long bestD=Long.MAX_VALUE;double bestT=Double.MAX_VALUE;
  for(int r=0;r<=radius;r++){ // square rings outward; a fit in ring r may be beaten by a nearer cell of a later ring, so stop once (r-1)^2 passes it
   if(best!=null&&(long)(r-1)*(r-1)>bestD)break;
   for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
    long d=(long)dx*dx+(long)dz*dz;if(d>(long)radius*radius||d>bestD)continue;int cx=x+dx,cz=z+dz;double t=Math.abs(cx+.5-towardX);
    if(d==bestD&&t>=bestT)continue;
    if(!stands(cx,cz,maxGround)||home!=null&&!nearHome(home,cx,cz)||also!=null&&!also.test(cx,cz))continue;
    best=new int[]{cx,cz};bestD=d;bestT=t;}}
  return best;
 }
 /** The capture point nearest (x, z); null on a map without points. */
 public Point nearestPoint(double x,double z){Point best=null;for(Point p:points)if(best==null||Math.hypot(p.x()-x,p.z()-z)<Math.hypot(best.x()-x,best.z()-z))best=p;return best;}
 /** mean world x of the water in row wz, or -100000 when the row is dry */
 public double riverX(int wz){return riverX[Math.max(0,Math.min(depth-1,wz-oz))];}
 public boolean inside(int wx,int wz){return wx>=ox&&wx<ox+width&&wz>=oz&&wz<oz+depth;}
 int cellX(double wx){return Math.max(0,Math.min(width-1,(int)Math.floor(wx)-ox));}
 int cellZ(double wz){return Math.max(0,Math.min(depth-1,(int)Math.floor(wz)-oz));}

 /** steps from canvas cell sx, sz to every cell over the grids ([z*w+x], world y), or -1 where unreachable */
 public static int[] distances(int[] solid,int[] water,int w,int d,int sx,int sz){
  int n=w*d;int[] dist=new int[n];java.util.Arrays.fill(dist,-1);
  boolean[] ok=new boolean[n];
  for(int i=0;i<n;i++){int wade=water[i]==NONE?0:Math.max(water[i]-solid[i],0);ok[i]=solid[i]!=NONE&&wade<=MAX_WADE;}
  int s=sz*w+sx;if(!ok[s])return dist;
  int[] q=new int[n];int head=0,tail=0;dist[s]=0;q[tail++]=s;
  while(head<tail){int i=q[head++];int x=i%w,h=solid[i],nd=dist[i]+1;
   for(int k=0;k<4;k++){int j;
    if(k==0){if(x+1>=w)continue;j=i+1;}else if(k==1){if(x==0)continue;j=i-1;}else if(k==2){j=i+w;if(j>=n)continue;}else{j=i-w;if(j<0)continue;}
    if(dist[j]<0&&ok[j]&&Math.abs(solid[j]-h)<=MAX_STEP){dist[j]=nd;q[tail++]=j;}}}
  return dist;
 }

 /** Compares grids read back from the world ([z*w+x] over the canvas, world y, NONE where empty) with this map:
  * one line per issue, "code: message"; empty when the world matches the design. */
 public static List<String> check(EngineMap m,int[] solid,int[] water){
  List<String> out=new ArrayList<>();int w=m.width,d=m.depth;
  int bad=0,first=-1;
  for(int i=0;i<w*d;i++){boolean wa=wetCell(water[i],solid[i]),wb=wetCell(m.water[i],m.solid[i]);
   if(solid[i]!=m.solid[i]||wa!=wb||(wa&&water[i]!=m.water[i])){bad++;if(first<0)first=i;}}
  if(bad>0){int x=first%w+m.ox,z=first/w+m.oz;out.add("columns: "+bad+" columns differ from the design, first at "+x+","+z+" (ground "+solid[first]+", design "+m.solid[first]+")");}
  for(Point p:m.points){int i=m.cellZ(p.z())*w+m.cellX(p.x());
   if(solid[i]+1!=p.y())out.add("moved: point "+p.name()+" stands at y "+(solid[i]+1)+", design "+p.y());
   if(wetCell(water[i],solid[i]))out.add("in_water: point "+p.name()+" is in water");}
  for(Spawn s:m.spawns){int i=m.cellZ(s.z())*w+m.cellX(s.x());
   if(solid[i]+1!=s.y())out.add("moved: spawn "+s.id()+" stands at y "+(solid[i]+1)+", design "+s.y());
   if(wetCell(water[i],solid[i]))out.add("in_water: spawn "+s.id()+" is in water");}
  if(m.floorRoutes)return out; // the column walk cannot pass under a roof (a gatehouse): tools/sectionmap.py walked these routes floor by floor
  for(Spawn s:m.spawns){int[] dist=distances(solid,water,w,d,m.cellX(s.x()),m.cellZ(s.z()));
   for(Point p:m.points){int walk=dist[m.cellZ(p.z())*w+m.cellX(p.x())];
    if(walk<0){out.add("unreachable: point "+p.name()+" from spawn "+s.id()+" ("+s.team()+")");continue;}
    Route r=m.routes.stream().filter(q->q.spawn().equals(s.id())&&q.point().equals(p.name())).findFirst().orElse(null);
    if(r==null||r.walk()==null||r.walk()!=walk)out.add("distance: "+s.id()+" to "+p.name()+" walks "+walk+", design "+(r==null?null:r.walk()));
    double straight=Math.hypot(p.x()-s.x(),p.z()-s.z());
    if(walk>DETOUR*Math.max(straight,1))out.add(String.format(java.util.Locale.ROOT,"detour: %s to %s walks %d, %.1f times the straight %.0f",s.id(),p.name(),walk,walk/Math.max(straight,1),straight));}}
  return out;
 }
}
