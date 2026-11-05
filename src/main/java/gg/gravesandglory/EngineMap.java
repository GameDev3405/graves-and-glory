package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
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
 public record Point(String name,double x,double z,int y,int radius){}
 public record Spawn(String id,String team,double x,double z,int y){}
 public record Route(String spawn,String point,double straight,Integer walk){}
 public record Tile(String file,int x,int z,int sx,int sy,int sz){}
 public record Road(String id,List<double[]> path,double width){}
 public final Path dir;public final String id,sha;public final int width,depth,ox,oz,baseY,floorY;
 public final int[] solid,water;
 public final List<Point> points=new ArrayList<>();public final List<Spawn> spawns=new ArrayList<>();
 public final List<Route> routes=new ArrayList<>();public final List<Tile> tiles=new ArrayList<>();public final List<Road> roads=new ArrayList<>();
 private final double[] riverX;

 private EngineMap(Path dir,JsonObject m,int[] solid,int[] water){
  this.dir=dir;id=m.get("id").getAsString();sha=m.get("design_sha256").getAsString();
  width=m.get("width").getAsInt();depth=m.get("depth").getAsInt();
  JsonArray o=m.getAsJsonArray("origin");ox=o.get(0).getAsInt();oz=o.get(1).getAsInt();
  baseY=m.get("baseY").getAsInt();floorY=m.get("floorY").getAsInt();this.solid=solid;this.water=water;
  for(JsonElement e:m.getAsJsonArray("points")){JsonObject p=e.getAsJsonObject();points.add(new Point(p.get("name").getAsString(),p.get("x").getAsDouble()+ox,p.get("z").getAsDouble()+oz,p.get("y").getAsInt(),p.get("radius").getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("spawns")){JsonObject p=e.getAsJsonObject();spawns.add(new Spawn(p.get("id").getAsString(),p.get("team").getAsString(),p.get("x").getAsDouble()+ox,p.get("z").getAsDouble()+oz,p.get("y").getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("routes")){JsonObject p=e.getAsJsonObject();JsonElement w=p.get("walk");routes.add(new Route(p.get("spawn").getAsString(),p.get("point").getAsString(),p.get("straight").getAsDouble(),w==null||w.isJsonNull()?null:w.getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("tiles")){JsonObject p=e.getAsJsonObject();JsonArray s=p.getAsJsonArray("size");tiles.add(new Tile(p.get("file").getAsString(),p.get("x").getAsInt()+ox,p.get("z").getAsInt()+oz,s.get(0).getAsInt(),s.get(1).getAsInt(),s.get(2).getAsInt()));}
  for(JsonElement e:m.getAsJsonArray("roads")){JsonObject p=e.getAsJsonObject();List<double[]> path=new ArrayList<>();for(JsonElement q:p.getAsJsonArray("path")){JsonArray a=q.getAsJsonArray();path.add(new double[]{a.get(0).getAsDouble()+ox,a.get(1).getAsDouble()+oz});}roads.add(new Road(p.get("id").getAsString(),path,p.get("width").getAsDouble()));}
  riverX=new double[depth];
  for(int z=0;z<depth;z++){double sum=0;int n=0;for(int x=0;x<width;x++)if(wetCell(water[z*width+x],solid[z*width+x])){sum+=x+ox;n++;}riverX[z]=n==0?-100000:sum/n;}
 }

 /** Reads dir/map.json and its columns.bin; throws IOException with a reason when either is missing or does not fit. */
 public static EngineMap read(Path dir)throws IOException{
  JsonObject m=JsonParser.parseString(Files.readString(dir.resolve("map.json"),StandardCharsets.UTF_8)).getAsJsonObject();
  if(!m.has("width")||!m.has("routes"))throw new IOException(dir+"/map.json is from an older engine: rebuild it with python tools/mapengine build");
  int w=m.get("width").getAsInt(),d=m.get("depth").getAsInt();
  byte[] raw=Files.readAllBytes(dir.resolve(m.has("columns")?m.get("columns").getAsString():"columns.bin"));
  if(raw.length!=4*w*d)throw new IOException("columns.bin holds "+raw.length+" bytes, expected "+4*w*d);
  ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
  int[] solid=new int[w*d],water=new int[w*d];
  for(int i=0;i<w*d;i++)solid[i]=b.getShort();
  for(int i=0;i<w*d;i++)water[i]=b.getShort();
  return new EngineMap(dir,m,solid,water);
 }

 static boolean wetCell(int water,int solid){return water!=NONE&&water>solid;}
 int index(int wx,int wz){int x=Math.max(0,Math.min(width-1,wx-ox)),z=Math.max(0,Math.min(depth-1,wz-oz));return z*width+x;}
 /** world y of the top solid block of the column at world x, z (edge columns repeat past the canvas) */
 public int solid(int wx,int wz){return solid[index(wx,wz)];}
 /** world y of the top water block, or NONE */
 public int water(int wx,int wz){return water[index(wx,wz)];}
 public boolean wet(int wx,int wz){int i=index(wx,wz);return wetCell(water[i],solid[i]);}
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
