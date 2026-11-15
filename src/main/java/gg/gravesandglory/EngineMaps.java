package gg.gravesandglory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A map built by the map engine (map engine Plan 2), played in one map slot, Domination only. Two sources:
 *  - opt-in: with -Dgravesandglory.engineMap=<dir with map.json> the slot -Dgravesandglory.engineMapSlot (default 1, Ironhold) takes its ground,
 *    points, spawns and roads from that build, for this JVM only; the slot's landmark and supply-house rows are dropped. Without the property nothing changes.
 *  - bundled: a maps row whose source is not "-" is played from the build in the mod under data/gravesandglory/sectionmaps/<source>
 *    (tools/sectionmap.py: the new Kingsfield from its 54 sections), read when the slot is first asked for. */
public final class EngineMaps {
 /** the opt-in build and its slot */
 static final EngineMap optIn;public static final int slot;
 /** bundled builds by map slot, read once (UNREAD until then, NOT_READ when it failed); Arena.ground asks on every call, so no lock once read */
 private static final Object UNREAD=new Object(),NOT_READ=new Object();
 private static final java.util.concurrent.atomic.AtomicReferenceArray<Object> bundled=new java.util.concurrent.atomic.AtomicReferenceArray<>(filled(Spec.MAPS.length));
 private static Object[] filled(int n){Object[] a=new Object[n];Arrays.fill(a,UNREAD);return a;}
 /** the build being placed (step), fixed from its first tick */
 static EngineMap map;
 static final int FLAGS=Block.NOTIFY_LISTENERS|Block.FORCE_STATE;
 static{
  EngineMap m=null;String dir=System.getProperty("gravesandglory.engineMap");int s=Integer.getInteger("gravesandglory.engineMapSlot",1);
  if(dir!=null&&!dir.isBlank()){
   try{m=EngineMap.read(Path.of(dir));
    var R=GravesAndGlory.R;
    if(m.width!=2*R.arenaHalfX()||m.depth!=2*R.arenaHalfZ()){System.out.println("[Graves and Glory] engine map "+dir+" is "+m.width+"x"+m.depth+", the arena needs "+2*R.arenaHalfX()+"x"+2*R.arenaHalfZ()+": not used");m=null;}
   }catch(Exception e){System.out.println("[Graves and Glory] engine map "+dir+" not read: "+e.getMessage());m=null;}
  }
  optIn=m;slot=s;
  if(m!=null){
   Spec.LANDMARKS=Arrays.stream(Spec.LANDMARKS).filter(r->r.map()!=s).toArray(Spec.LandmarksRow[]::new);
   Spec.SUPPLYHOUSES=Arrays.stream(Spec.SUPPLYHOUSES).filter(r->r.map()!=s).toArray(Spec.SupplyHousesRow[]::new);
   Tactics.housesAt=-1;
   System.out.println("[Graves and Glory] engine map "+m.id+" ("+m.sha.substring(0,12)+") plays in map slot "+s);
  }
 }
 /** The engine build a map slot plays, or null for a map CampaignMaps makes. */
 public static EngineMap map(int index){
  if(optIn!=null&&index==slot)return optIn;
  if(index<0||index>=bundled.length())return null;
  Object m=bundled.get(index);if(m==UNREAD)m=read(index);
  return m instanceof EngineMap e?e:null;
 }
 private static synchronized Object read(int index){
  Object m=bundled.get(index);if(m!=UNREAD)return m;
  if(Spec.MAPS[index].source().equals("-"))m=NOT_READ;
  else{String root="data/gravesandglory/sectionmaps/"+Spec.MAPS[index].source();
   try{var e=EngineMap.resource(root);m=e;System.out.println("[Graves and Glory] bundled map "+e.id+" ("+e.sha.substring(0,12)+") plays in map slot "+index);
    var row=Spec.MAPS[index];if(e.fieldHalfX!=row.fieldHalfX()||e.fieldHalfZ!=row.fieldHalfZ())System.out.println("[Graves and Glory] WARNING bundled map "+e.id+" was packed with field "+e.fieldHalfX+"x"+e.fieldHalfZ+" but the maps sheet says "+row.fieldHalfX()+"x"+row.fieldHalfZ()+": the barrier follows the sheet");}
   catch(Exception e){m=NOT_READ;System.out.println("[Graves and Glory] bundled map "+root+" not read: "+e.getMessage());}}
  bundled.set(index,m);return m;
 }
 public static boolean on(int index){return map(index)!=null;}
 /** the engine build of the selected map, or null */
 public static EngineMap current(){return map(CampaignMaps.selected);}
 /** the engine map is the selected map */
 public static boolean active(){return on(CampaignMaps.selected);}
 static EngineMap.Spawn spawn(EngineMap m,int team){String t=team==0?"dawn":"dusk";for(var s:m.spawns)if(s.team().equals(t))return s;return m.spawns.get(team%m.spawns.size());}
 public static int spawnX(int map,int team){return (int)Math.floor(spawn(map(map),team).x());}
 public static int spawnZ(int map,int team){return (int)Math.floor(spawn(map(map),team).z());}

 /** The end of CampaignMaps.select on the engine slot: the slot's tables from the engine build. */
 static void apply(){
  int index=CampaignMaps.selected;var map=map(index);
  var points=new ArrayList<Spec.PointsRow>();
  for(var p:map.points){int x=(int)Math.floor(p.x()),z=(int)Math.floor(p.z());points.add(new Spec.PointsRow(p.id(),p.name(),x,z,p.radius(),8,p.y()-1));} // the point's own floor (EngineMap.check holds it to the column top unless it is roofed): Hollowwood's shrine lies under the Yoke
  var roads=new ArrayList<CampaignMaps.Road>();
  for(var r:map.roads)for(int i=0;i+1<r.path().size();i++){double[] a=r.path().get(i),b=r.path().get(i+1);roads.add(new CampaignMaps.Road((int)Math.round(a[0]),(int)Math.round(a[1]),(int)Math.round(b[0]),(int)Math.round(b[1]),(int)Math.ceil(r.width()/2)));}
  Spec.STRUCTURES=new Spec.StructuresRow[0];Spec.TERRAIN=new Spec.TerrainRow[0];Spec.SCENERY=new Spec.SceneryRow[0];
  CampaignMaps.roads=roads;Spec.POINTS=points.toArray(Spec.PointsRow[]::new);
  for(int t=0;t<2;t++){var f=Spec.FACTIONS[t];int x=spawnX(index,t),z=spawnZ(index,t);Spec.FACTIONS[t]=new Spec.FactionsRow(f.id(),f.name(),f.color(),f.glyph(),f.dye(),x,z,x+(x<0?-3:3),z);}
 }

 // placement: load every tile once, then columns under a time budget per tick, then the point dressing, then the read-back check
 static final long BUDGET_NS=40_000_000L;
 static int phase=0,column=0;static long startNs,placeNs;static int[] tileOf;static short[][] stacks;static int[] heights;static BlockState[] palette;
 /** a prop anchor of the build (a tile's "anchors", tools/sectionmap.py): the command block's world cell and its Command text */
 record Anchor(BlockPos pos,String cmd){}
 static final List<Anchor> anchors=new ArrayList<>();
 public static int lastProps=-1;
 static int[] readSolid,readWater;public static List<String> lastIssues=List.of();public static long lastBuildMs=-1;

 static int worldW(){return 2*GravesAndGlory.R.worldHalfX()+1;}
 static int worldD(){return 2*GravesAndGlory.R.worldHalfZ()+1;}
 public static int percent(){if(map==null)return 0;int total=worldW()*worldD();return phase<=1?0:phase==2?column*90/total:phase==3?90:90+column*10/Math.max(1,map.width*map.depth);}

 static void load()throws Exception{
  var lookup=Registries.BLOCK.getReadOnlyWrapper();var pal=new ArrayList<BlockState>();var index=new java.util.HashMap<BlockState,Short>();
  tileOf=new int[map.width*map.depth];stacks=new short[map.tiles.size()][];heights=new int[map.tiles.size()];
  for(int t=0;t<map.tiles.size();t++){var tile=map.tiles.get(t);NbtCompound tag;
   try(InputStream in=map.files.open(tile.file())){tag=NbtIo.readCompressed(in);}
   NbtList p=tag.getList("palette",NbtElement.COMPOUND_TYPE);short[] local=new short[p.size()];
   for(int i=0;i<p.size();i++){BlockState s=NbtHelper.toBlockState(lookup,p.getCompound(i));Short k=index.get(s);if(k==null){k=(short)pal.size();pal.add(s);index.put(s,k);}local[i]=k;}
   int sx=tile.sx(),sy=tile.sy(),sz=tile.sz();short[] st=new short[sx*sz*sy];Arrays.fill(st,(short)-1);
   if(tile.format().equals("stack")){ // tools/sectionmap.py: cell (z*sx+x)*sy+y, a big-endian int16 index into the tile's palette, -1 none
    byte[] raw=tag.getByteArray("stack");if(raw.length!=2*st.length)throw new IllegalStateException(tile.file()+": "+raw.length+" stack bytes, expected "+2*st.length);
    for(int i=0;i<st.length;i++){int v=(short)((raw[2*i]<<8)|(raw[2*i+1]&255));st[i]=v<0?-1:local[v];}
   }else{
    NbtList blocks=tag.getList("blocks",NbtElement.COMPOUND_TYPE);
    for(int i=0;i<blocks.size();i++){NbtCompound b=blocks.getCompound(i);NbtList pos=b.getList("pos",NbtElement.INT_TYPE);st[(pos.getInt(2)*sx+pos.getInt(0))*sy+pos.getInt(1)]=local[b.getInt("state")];}
   }
   stacks[t]=st;heights[t]=sy;
   for(int z=0;z<sz;z++)for(int x=0;x<sx;x++)tileOf[(tile.z()-map.oz+z)*map.width+tile.x()-map.ox+x]=t;
  }
  palette=pal.toArray(BlockState[]::new);
 }
 /** The prop anchors a packed map's tiles hold, in world cells (read from the tiles on their own so a standalone check needs no Minecraft world). */
 static void loadAnchors()throws Exception{
  anchors.clear();
  for(var tile:map.tiles){NbtCompound tag;try(InputStream in=map.files.open(tile.file())){tag=NbtIo.readCompressed(in);}
   NbtList a=tag.getList("anchors",NbtElement.COMPOUND_TYPE);
   for(int i=0;i<a.size();i++){NbtCompound c=a.getCompound(i);anchors.add(new Anchor(new BlockPos(tile.x()+c.getInt("x"),map.floorY+c.getInt("y"),tile.z()+c.getInt("z")),c.getString("cmd")));}}
 }
 /** Writes the anchors' Command text into the command blocks the tiles placed (a block state alone carries no text), once every column is placed. */
 static int armAnchors(ServerWorld w){
  int n=0;
  for(var a:anchors){if(!w.getBlockState(a.pos()).isOf(Blocks.COMMAND_BLOCK)||!(w.getBlockEntity(a.pos()) instanceof CommandBlockBlockEntity cb)){System.out.println("[Graves and Glory] prop anchor at "+a.pos().toShortString()+" is not a command block in the world");continue;}
   cb.getCommandExecutor().setCommand(a.cmd());cb.setAuto(false);cb.markDirty();n++;}
  return n;
 }
 /** the block of canvas column (cx, cz) at y, or air */
 static BlockState at(int cx,int cz,int y){int t=tileOf[cz*map.width+cx];var tile=map.tiles.get(t);int ly=y-map.floorY;if(ly<0||ly>=heights[t])return Blocks.AIR.getDefaultState();
  short s=stacks[t][((cz-(tile.z()-map.oz))*tile.sx()+cx-(tile.x()-map.ox))*heights[t]+ly];return s<0?Blocks.AIR.getDefaultState():palette[s];}
 /** the y above the highest block of a column, loading its chunk (World.getTopY answers the bottom of the world for an unloaded chunk) */
 static int top(ServerWorld w,int x,int z){return w.getChunk(x>>4,z>>4).sampleHeightmap(Heightmap.Type.WORLD_SURFACE,x&15,z&15)+1;}
 /** true while set() places a block: CarvedPumpkinGuardMixin then skips the golem pattern search, which reads neighbours in chunks that are not ticking yet
  *  (CachedBlockPosition answers null there and vanilla throws) and would otherwise build golems out of map dressing */
 public static boolean placing;
 static void set(ServerWorld w,BlockPos.Mutable pos,BlockState s){if(w.getBlockState(pos)!=s){placing=true;try{w.setBlockState(pos,s,FLAGS);}finally{placing=false;}}}

 /** One tick of building the engine map; true once it is placed and checked (the next call starts a new build). */
 /** Drops a build in progress: the next step starts over with the map then selected (Campaign.switchMap). */
 public static void reset(){stacks=null;tileOf=null;readSolid=null;readWater=null;phase=0;column=0;map=null;}
 public static boolean step(ServerWorld w){
  long t0=System.nanoTime();
  if(phase==0){startNs=t0;column=0;map=current();
   try{load();loadAnchors();}catch(Exception e){throw new IllegalStateException("engine map tiles not read: "+e.getMessage(),e);}
   System.out.println("[Graves and Glory] engine map "+map.id+": "+map.tiles.size()+" tiles read in "+(System.nanoTime()-t0)/1_000_000+" ms");phase=2;return false;}
  var R=GravesAndGlory.R;var pos=new BlockPos.Mutable();
  if(phase==2){int total=worldW()*worldD();
   while(column<total&&System.nanoTime()-t0<BUDGET_NS){
    int x=column%worldW()-R.worldHalfX(),z=column/worldW()-R.worldHalfZ();column++;
    int cx=Math.max(0,Math.min(map.width-1,x-map.ox)),cz=Math.max(0,Math.min(map.depth-1,z-map.oz));
    // The margin north of the playable box copies the edge column, shifted by CampWorld.taper so it meets the rim of the camp with no step (as Arena does for hand-made maps).
    // A shifted column is laid solid: its water would stand above or below the edge's and spill, so water there takes the edge's ground block.
    int shift=0,ground=map.solid(cx+map.ox,cz+map.oz);
    // A bundled map's canvas is smaller than the world: past it the edge column is laid up to its ground only (no stretched walls, roofs or trees), at most baseY.
    boolean skirt=map!=optIn&&!map.inside(x,z);if(skirt&&ground!=EngineMap.NONE)ground=Math.min(ground,map.baseY);
    if(z<-R.arenaHalfZ()&&ground!=EngineMap.NONE)shift=(int)Math.round(CampWorld.taper(x,z,ground+1))-1-ground;
    int top=map.floorY+heights[tileOf[cz*map.width+cx]]+Math.max(0,shift);
    for(int y=map.floorY;y<top;y++){pos.set(x,y,z);
     if(skirt&&y-shift>ground){set(w,pos,Blocks.AIR.getDefaultState());continue;}
     if(shift==0){set(w,pos,at(cx,cz,y));continue;}
     int sy=y-shift;BlockState s=sy>ground?Blocks.AIR.getDefaultState():at(cx,cz,Math.max(map.floorY,sy));
     if(s.isOf(Blocks.WATER)||s.isOf(MapBlocks.STILL_WATER))s=at(cx,cz,ground);
     set(w,pos,s);}
    int surface=top(w,x,z);
    for(int y=top;y<surface;y++){pos.set(x,y,z);set(w,pos,Blocks.AIR.getDefaultState());}
    int hx=Field.halfX(),hz=Field.halfZ();
    if(Math.abs(x)==hx&&Math.abs(z)<=hz||Math.abs(z)==hz&&Math.abs(x)<=hx){int g=Math.max(map.solid(x,z),map.water(x,z))+1;for(int y=g;y<g+6;y++){pos.set(x,y,z);set(w,pos,Blocks.BARRIER.getDefaultState());}}
   }
   if(column>=total){placeNs=System.nanoTime()-startNs;phase=3;}
   return false;}
  if(phase==3){w.setTimeOfDay(3000);int armed=armAnchors(w);if(!anchors.isEmpty())System.out.println("[Graves and Glory] engine map "+map.id+": "+armed+" of "+anchors.size()+" prop anchors armed");
   for(var p:Spec.POINTS){
    if(map!=optIn){Arena.flag(w,p,-1);continue;} // a bundled map draws its own point rings and pads
    for(int dx=-p.radius();dx<=p.radius();dx++)for(int dz=-p.radius();dz<=p.radius();dz++){double d=Math.hypot(dx,dz);int x=p.x()+dx,z=p.z()+dz;
     boolean pad=Math.abs(dx)<=1&&Math.abs(dz)<=1,ring=d>=p.radius()-1&&d<=p.radius();
     if((pad||ring)&&map.inside(x,z)&&!map.wet(x,z)){pos.set(x,map.solid(x,z),z);w.setBlockState(pos,(pad?Blocks.CHISELED_STONE_BRICKS:Blocks.SMOOTH_STONE).getDefaultState(),FLAGS);}}
    Arena.flag(w,p,-1);}
   readSolid=new int[map.width*map.depth];readWater=new int[map.width*map.depth];column=0;phase=4;return false;}
  if(phase==4){int total=map.width*map.depth;
   while(column<total&&System.nanoTime()-t0<BUDGET_NS){int cx=column%map.width,cz=column/map.width,x=cx+map.ox,z=cz+map.oz;
    int solid=EngineMap.NONE,water=EngineMap.NONE;
    for(int y=top(w,x,z);y>=map.floorY;y--){pos.set(x,y,z);BlockState s=w.getBlockState(pos);
     if(s.isOf(Blocks.WATER)||s.isOf(MapBlocks.STILL_WATER)){if(water==EngineMap.NONE)water=y;continue;} // still_water reads as water (tools/mapengine/build.py water_state): no-entry water, never ground
     if(s.isAir()||s.isOf(Blocks.BARRIER)||s.isOf(Blocks.SEA_LANTERN)||s.isOf(MapBlocks.TROOP_FENCE)||s.getBlock() instanceof FenceBlock||s.isIn(BlockTags.WOOL))continue; // troop_fence is a wall, never ground (ground_state)
     solid=y;break;}
    readSolid[column]=solid;readWater[column]=water;column++;}
   if(column<total)return false;
   lastIssues=EngineMap.check(map,readSolid,readWater);lastBuildMs=(System.nanoTime()-startNs)/1_000_000;
   String head="engine map "+map.id+" sha "+map.sha+": placed in "+placeNs/1_000_000+" ms, built and checked in "+lastBuildMs+" ms, "+(lastIssues.isEmpty()?"PASS":lastIssues.size()+" issues");
   System.out.println("[Graves and Glory] "+head);for(String s:lastIssues)System.out.println("[Graves and Glory]   "+s);
   try{Files.writeString(Path.of("engine-map-check.txt"),head+"\n"+String.join("\n",lastIssues)+(lastIssues.isEmpty()?"":"\n"),StandardCharsets.UTF_8);}catch(Exception e){System.out.println("[Graves and Glory] engine-map-check.txt not written: "+e.getMessage());}
   Diagnostics.report("engine-map-check",lastIssues.isEmpty(),lastIssues.isEmpty()?null:String.join("; ",lastIssues.subList(0,Math.min(5,lastIssues.size()))));
   { // the props (always: a map without anchors still clears the parts a previous build left; scanBox walks block entities per chunk): every anchor gets its parts once the ground is placed and read back (the light blocks it restores must not read as ground)
    lastProps=Props.scanBox(w,new BlockBox(map.ox,w.getBottomY(),map.oz,map.ox+map.width-1,w.getTopY()-1,map.oz+map.depth-1));
    System.out.println("[Graves and Glory] engine map "+map.id+": props "+lastProps+" found by Props.scanBox over "+anchors.size()+" anchors");}
   anchors.clear();
   stacks=null;tileOf=null;readSolid=null;readWater=null;phase=0;map=null;return true;}
  phase=0;return false;
 }
}
