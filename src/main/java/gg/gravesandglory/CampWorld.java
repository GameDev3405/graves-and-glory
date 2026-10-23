package gg.gravesandglory;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/** The player's camp: a terrain island (a bowl, a ridge, a plateau) written by Java, the authored pieces of campPieces placed on it from their structure files, the villagers and labels of campStations,
 *  and the war table. Every coordinate comes from the camp, campPieces and campStations sheets; the pure functions (spawn, inCamp, where, station, text, tableCell) read nothing but the sheets and the map
 *  that stands, so the client can call them too. */
final class CampWorld {
    /** Camp entities carry this tag, camp-generation:<g> and camp:<page>; the entities of the older camps (gravesandglory-camp-v3 and the two lobby tags) are discarded on every load. */
    static final String TAG="gravesandglory-camp-v4";
    /** In the world folder: the hash of what the terrain was built from. */
    static final String MARKER_FILE="gravesandglory-camp.json";
    /** Part of the hash: a change to the way the terrain is shaped or written makes every world rebuild it (CampTerrain.REVISION, which the arena margin's revision names as well). */
    static final String TERRAIN=CampTerrain.REVISION;
    /** Where the plateau begins and where its bank starts at the foot beside the stairs (CampTerrain). */
    static final int RIDGE_TOP_DZ=CampTerrain.RIDGE_TOP_DZ,RIDGE_FOOT_DZ=CampTerrain.RIDGE_FOOT_DZ;
    /** The southern rim behind the gates (CampTerrain): the camp's half of the southern seam. Behind the island the arena's north hills (CampaignMaps, north_mountains, 20 high and 45 on Frostmarch) are cut off at z -266, the last column the build writes;
     *  taper() is the arena's half: in the 16 columns of the build beyond the playable box (z -251 to -266) the hills meet the crest of the rim, and no face is left. */
    static final int RIM_FOOT_DZ=CampTerrain.RIM_FOOT_DZ,RIM_RISE=CampTerrain.RIM_RISE;
    static final double RIM_SLOPE=CampTerrain.RIM_SLOPE;
    /** The blocks beyond the width of the island over which taper() fades out. */
    static final int SKIRT=28;
    static String generation="";
    /** What this class logged, oldest first, at most LOG_KEEP lines (the harness reads them as well as the console). */
    static final List<String> LINES=new CopyOnWriteArrayList<>();
    static final int LOG_KEEP=300;
    private static long logged;
    static synchronized void log(String line){System.out.println(line);LINES.add(line);logged++;while(LINES.size()>LOG_KEEP)LINES.remove(0);}
    /** How many lines were ever logged: a mark that stays right after the oldest lines were dropped (an index into LINES does not). */
    static synchronized long logged(){return logged;}
    /** The lines logged after the mark (a value of logged()), oldest first; fewer than that if more than LOG_KEEP came since. */
    static synchronized List<String> since(long mark){var all=new ArrayList<>(LINES);int n=(int)Math.min(Math.max(0,logged-mark),all.size());return all.subList(all.size()-n,all.size());}

    record Spot(double x,double y,double z,float yaw,float pitch){}

    // ---- the pure functions ----

    /** Where a commander stands after toLobby: the first arrival (the yard, firstDx/firstDz/firstYaw, looking east to the Drillmaster) while the profile has cleared no Long March battle, else the camp spawn. A null profile is the camp spawn. */
    static Spot spawn(Profile p){
        var c=Spec.CAMP[0];
        if(p!=null&&p.campaignCleared==0)return new Spot(c.originX()+c.firstDx(),c.spawnY(),c.originZ()+c.firstDz(),(float)c.firstYaw(),(float)c.spawnPitch());
        return new Spot(c.originX()+c.spawnDx(),c.spawnY(),c.originZ()+c.spawnDz(),(float)c.spawnYaw(),(float)c.spawnPitch());
    }

    /** Inside the island box (x and z, the far edge included as a block) and not below minY: where the leash lets a commander stay. */
    static boolean inCamp(double x,double y,double z){
        var c=Spec.CAMP[0];
        return x>=c.originX()+c.islandMinDx()&&x<c.originX()+c.islandMaxDx()+1&&z>=c.originZ()+c.islandMinDz()&&z<c.originZ()+c.islandMaxDz()+1&&y>=c.minY();
    }

    /** The first campStations row with that page, else the centre of the first campPieces row whose station starts with it (at the height of a standing commander); null when there is none. */
    static Vec3d where(String page){
        var c=Spec.CAMP[0];
        for(var s:Spec.CAMPSTATIONS)if(s.page().equals(page))return new Vec3d(c.originX()+s.x(),s.y(),c.originZ()+s.z());
        for(var row:Spec.CAMPPIECES)if(!row.station().equals("-")&&row.station().startsWith(page)){var box=box(row);return new Vec3d((box.getMinX()+box.getMaxX()+1)/2.0,row.y()+1,(box.getMinZ()+box.getMaxZ()+1)/2.0);}
        return null;
    }

    /** The page of the station a clicked block belongs to: a campStations box (|x-sx|<=halfX, |z-sz|<=halfZ, y-2 to y+4, the block taken by its centre), else a campPieces footprint whose station is not - (from one below the floor to 12 above it); null for none. */
    static String station(BlockPos p){
        var c=Spec.CAMP[0];double x=p.getX()+.5,z=p.getZ()+.5;
        for(var s:Spec.CAMPSTATIONS)if(Math.abs(x-(c.originX()+s.x()))<=s.halfX()&&Math.abs(z-(c.originZ()+s.z()))<=s.halfZ()&&p.getY()>=s.y()-2&&p.getY()<=s.y()+4)return s.page();
        for(var row:Spec.CAMPPIECES){
            if(row.station().equals("-"))continue;var box=box(row);
            if(p.getX()>=box.getMinX()&&p.getX()<=box.getMaxX()&&p.getZ()>=box.getMinZ()&&p.getZ()<=box.getMaxZ()&&p.getY()>=row.y()-1&&p.getY()<=row.y()+12)return row.station();
        }
        return null;
    }

    /** A label as Text: gold, {key:use} becomes the keybind key.use and {key:<id>} the keybind key.gravesandglory.<id>. */
    static Text text(String label){
        MutableText out=Text.empty();var m=Pattern.compile("\\{key:([a-z_]+)\\}").matcher(label);int from=0;
        while(m.find()){
            if(m.start()>from)out.append(Text.literal(label.substring(from,m.start())));
            out.append(Text.keybind(m.group(1).equals("use")?"key.use":"key.gravesandglory."+m.group(1)));from=m.end();
        }
        if(from<label.length())out.append(Text.literal(label.substring(from)));
        return out.formatted(Formatting.GOLD);
    }

    /** The footprint of a campPieces row: its piece (the pieces sheet gives the size) turned about its centre block. */
    static BlockBox box(Spec.CampPiecesRow row){
        var c=Spec.CAMP[0];var p=piece(row.template());
        return Structures.footprint(new Vec3i(p.width(),p.height(),p.depth()),c.originX()+row.dx(),row.y(),c.originZ()+row.dz(),row.rotation());
    }
    static Spec.PiecesRow piece(String id){for(var p:Spec.PIECES)if(p.id().equals(id))return p;throw new IllegalStateException("no pieces row "+id);}

    /** Where display k (0 to 5) of a House row stands, its feet: the row's slot (slotX+2k, slotY, slotZ, local to its piece) turned with the piece, at the centre of its block. Pure, so the client can call it. */
    static Vec3d slot(Spec.CampPiecesRow row,int k){
        var c=Spec.CAMP[0];var p=piece(row.template());
        var at=Structures.toWorld(new Vec3i(p.width(),p.height(),p.depth()),c.originX()+row.dx(),row.y(),c.originZ()+row.dz(),row.rotation(),row.slotX()+2*k,row.slotY(),row.slotZ());
        return new Vec3d(at.getX()+.5,at.getY(),at.getZ()+.5);
    }
    /** The yaw a display of a piece turned by rotation faces: the front of a piece authored facing south (0 south, 90 west, 180 north, 270 east). */
    static float yaw(int rotation){return switch(rotation){case 0->0f;case 90->90f;case 180->180f;case 270->-90f;default->throw new IllegalArgumentException("rotation "+rotation);};}

    // ---- the build ----

    /** The camp at world start (and in the harness): terrain when the world lacks the current hash, the pieces at every start, the people, the war table; the hash is written last. */
    static void build(ServerWorld w){
        generation=UUID.randomUUID().toString();
        String hash=hash();
        long started=System.nanoTime();
        if(!built(w,hash)){terrain(w);log("[Graves and Glory] camp terrain "+millis(started)+" ms");}
        else log("[Graves and Glory] camp terrain skipped");
        started=System.nanoTime();
        var c=Spec.CAMP[0];
        for(var row:Spec.CAMPPIECES)Structures.place(w,row.template(),c.originX()+row.dx(),row.y(),c.originZ()+row.dz(),row.rotation());
        log("[Graves and Glory] camp pieces "+millis(started)+" ms");
        people(w);
        warTable(w);
        mark(w,hash);
    }
    static long millis(long started){return (System.nanoTime()-started)/1000000;}
    static long millis(long from,long to){return (to-from)/1000000;}

    /** CRC32 of the camp and campPieces rows, the camp/ rows of pieces and the terrain version. */
    static String hash(){
        var crc=new CRC32();
        crc.update(Arrays.toString(Spec.CAMP).getBytes(StandardCharsets.UTF_8));
        crc.update(Arrays.toString(Spec.CAMPPIECES).getBytes(StandardCharsets.UTF_8));
        for(var p:Spec.PIECES)if(p.id().startsWith("camp/"))crc.update(p.toString().getBytes(StandardCharsets.UTF_8));
        crc.update((TERRAIN+"/"+rimEnd(Spec.CAMP[0])).getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc.getValue());
    }
    static Path marker(ServerWorld w){return w.getServer().getSavePath(WorldSavePath.ROOT).resolve(MARKER_FILE);}
    static boolean built(ServerWorld w,String hash){
        try{return com.google.gson.JsonParser.parseString(Files.readString(marker(w))).getAsJsonObject().get("terrain").getAsString().equals(hash);}catch(Exception e){return false;}
    }
    static void mark(ServerWorld w,String hash){
        try{Files.writeString(marker(w),"{\"terrain\":\""+hash+"\"}");}catch(Exception e){System.err.println("[Graves and Glory] camp terrain marker could not be written: "+e);}
    }

    /** The last row (dz) the terrain writes: the south edge of the island, or the row before the first column of the arena when that is further south (the arena owns z from -worldHalfZ on, and the rim runs up to it
     *  so that nothing but the rim is seen from the camp; the leash box of inCamp stays the island). */
    static int rimEnd(Spec.CampRow c){return CampTerrain.rimEnd(c);}
    /** The height of the island at (dx,dz) from the origin of the camp, before the pieces (CampTerrain.heightRaw). */
    static double heightRaw(Spec.CampRow c,int dx,int dz){return CampTerrain.heightRaw(c,dx,dz);}
    static int height(Spec.CampRow c,int dx,int dz){return CampTerrain.height(c,dx,dz);}
    /** The crest of the rim at dx: the height of its last row, the one the arena's margin has to meet. */
    static int rimTop(Spec.CampRow c,int dx){return CampTerrain.rimTop(c,dx);}
    /** The arena's height h at (x,z) in the margin north of the playable box (z less than -arenaHalfZ, down to -worldHalfZ, the last column the build writes), pulled towards the crest of the rim: with the share (-arenaHalfZ-z)/(worldHalfZ-arenaHalfZ)
     *  that grows by a sixteenth a row from nothing at the box (the box itself is not touched) to all of it at the edge, so the last column of the arena stands at the height of the first row of the rim. Over the width of the island the pull is full;
     *  beyond it, it fades out over SKIRT blocks. The hills of the north (up to 109 on Frostmarch at the edge) come down to the rim and the valley of the river comes up to it, nowhere by more than 3 a block. Arena.height calls this for z less than -arenaHalfZ only. */
    static double taper(int x,int z,double h){
        var c=Spec.CAMP[0];int edge=-GravesAndGlory.R.arenaHalfZ(),end=-GravesAndGlory.R.worldHalfZ();
        if(z>=edge)return h;
        int dx=x-c.originX(),out=Math.max(0,Math.max(dx-c.islandMaxDx(),c.islandMinDx()-dx));
        if(out>=SKIRT)return h;
        double share=Math.min(1,(edge-z)/(double)(edge-end)),t=out/(double)SKIRT,across=1-t*t*(3-2*t);
        return h+share*across*(rimTop(c,dx)-h);
    }
    static boolean inBowl(Spec.CampRow c,int dx,int dz){return CampTerrain.inBowl(c,dx,dz);}

    /** The terrain: the ground of every column of the island box (CampTerrain.ground: the height with the rows of campPieces laid over it), stone (with the andesite lenses of CampTerrain.rockKind in its top six blocks) under the under block under the top block (CampTerrain.tops and underAt: rock and soil in patches, rockier
     *  with height from y 84), the air above it up to the top of what stood there, then the plants of the slopes and the spruces (CampTerrain.plant and tree).
     *  Written with Arena's build flags (2 and 16): no shape update runs, so taking away the blocks of an older camp cannot break and spill what stood against them, and the terrain holds nothing a shape update would change
     *  (stone, andesite, dirt, coarse dirt, grass, air, grass and ferns on grass, spruce logs and persistent leaves). The pieces come after, through Structures.place, which does run them.
     *  The chunks it covers are loaded first, and that time is counted in the 6 s budget: without the preload the first build generated them one at a time between the block writes and took 7573 ms. If p3-camp-budget fails, look at the preload first.
     *  The rows go from the island's north edge to rimEnd (the south rim); the trees keep their crowns out of the arena's columns and put leaves only into air, so a crown on the rim or a flank does not eat into the slope. */
    static void terrain(ServerWorld w){
        var c=Spec.CAMP[0];int width=c.islandMaxDx()-c.islandMinDx()+1,last=rimEnd(c),depth=last-c.islandMinDz()+1;
        var rows=Spec.CAMPPIECES;int[][] boxes=new int[rows.length][];boolean[] flatten=new boolean[rows.length];int[] floors=new int[rows.length];
        for(int i=0;i<rows.length;i++){var b=box(rows[i]);boxes[i]=new int[]{b.getMinX(),b.getMaxX(),b.getMinZ(),b.getMaxZ()};flatten[i]=rows[i].flatten();floors[i]=rows[i].y();}
        int[] ground=CampTerrain.ground(c,boxes,flatten,floors),kinds=CampTerrain.tops(ground,width,depth);
        long t0=System.nanoTime();
        for(int cx=(c.originX()+c.islandMinDx()-2)>>4;cx<=(c.originX()+c.islandMaxDx()+2)>>4;cx++)for(int cz=(c.originZ()+c.islandMinDz()-2)>>4;cz<=(c.originZ()+last+2)>>4;cz++)w.getChunk(cx,cz);
        long t1=System.nanoTime();
        boolean was=Arena.building;Arena.building=true;
        try{
            int flags=Arena.flags();var pos=new BlockPos.Mutable();
            for(int i=0;i<width;i++)for(int k=0;k<depth;k++){
                int dx=c.islandMinDx()+i,dz=c.islandMinDz()+k,x=c.originX()+dx,z=c.originZ()+dz,h=ground[i*depth+k];
                w.getChunk(x>>4,z>>4); // load it first: getTopY answers the world bottom for a chunk that is not loaded
                int top=w.getTopY(Heightmap.Type.WORLD_SURFACE,x,z);
                for(int y=top;y>h;y--)if(!w.getBlockState(pos.set(x,y,z)).isAir())w.setBlockState(pos,Blocks.AIR.getDefaultState(),flags); // from the top down: nothing is left standing on a block that has gone
                for(int y=c.islandBottomY();y<h-1;y++)w.setBlockState(pos.set(x,y,z),(y>=h-6?block(CampTerrain.rockKind(x,y,z),Blocks.STONE):Blocks.STONE).getDefaultState(),flags); // the faces of a crag show the andesite lenses of its top
                double rock=CampTerrain.steepness(ground,width,depth,i,k);
                int kind=kinds[i*depth+k];
                w.setBlockState(pos.set(x,h-1,z),block(CampTerrain.underAt(kind,h,rock,x,z),Blocks.DIRT).getDefaultState(),flags);
                w.setBlockState(pos.set(x,h,z),block(kind,Blocks.GRASS_BLOCK).getDefaultState(),flags);
                int plant=CampTerrain.plant(c,dx,dz,h,kind);
                if(plant!=0)w.setBlockState(pos.set(x,h+1,z),(plant==1?Blocks.GRASS:Blocks.FERN).getDefaultState(),flags);
            }
            long t2=System.nanoTime();int trees=0;
            for(int i=0;i<width;i++)for(int k=0;k<depth;k++){
                int dx=c.islandMinDx()+i,dz=c.islandMinDz()+k,h=ground[i*depth+k];
                var t=CampTerrain.tree(c,dx,dz,h,CampTerrain.steepness(ground,width,depth,i,k));
                if(t!=null){Arena.tree(w,c.originX()+dx,h+1,c.originZ()+dz,t[1],t[0],true,true);trees++;}
            }
            log("[Graves and Glory] camp terrain detail: chunks "+millis(t0,t1)+" ms, columns "+millis(t1,t2)+" ms, trees "+trees+" in "+millis(t2,System.nanoTime())+" ms");
        }finally{Arena.building=was;}
    }

    /** The 0 to 1 steepness of a slope that climbs perStride blocks over four blocks for the arena's margin (CampTerrain.rockOf; the camp itself uses CampTerrain.slopeRock). */
    static double rockOf(double perStride){return CampTerrain.rockOf(perStride);}
    static double hash01(int x,int z,int seed){return CampTerrain.hash01(x,z,seed);}
    /** The block of a CampTerrain kind, flat being the flat block of the ground (grass, snow, podzol). */
    static Block block(int kind,Block flat){return kind==CampTerrain.STONE?Blocks.STONE:kind==CampTerrain.ANDESITE?Blocks.ANDESITE:kind==CampTerrain.COARSE?Blocks.COARSE_DIRT:kind==CampTerrain.DIRT?Blocks.DIRT:kind==CampTerrain.ROOTED?Blocks.ROOTED_DIRT:flat;}
    /** The block on top of a column: the flat block (grass, snow, podzol) on level ground; where the ground is steep, rock (0 to 1) is the share of the columns, taken in patches and by hash, that show stone or coarse dirt instead (CampTerrain.mix).
     *  The camp and the margin of the arena both lay it. Coarse dirt and not plain dirt: grass spreads over a plain dirt block that has air above it, so the patches would turn green by themselves over the hours of a game. */
    static Block mix(Block flat,double rock,int x,int z){return block(CampTerrain.mix(rock,x,z),flat);}
    /** The block under the top block: dirt on the flat, and on steep ground (it is the face seen between two rows) dirt, coarse dirt or stone by hash (CampTerrain.under). */
    static BlockState under(double rock,int x,int z){return underBlock(rock,x,z).getDefaultState();}
    static Block underBlock(double rock,int x,int z){return block(CampTerrain.under(rock,x,z),Blocks.DIRT);}

    // ---- the people ----

    /** Discards what the older camps and this one left (every tag in the discard list), then spawns the villager of each station row with an npc and the label of every row. */
    static void people(ServerWorld w){
        var old=new ArrayList<Entity>();
        for(var e:w.iterateEntities()){var tags=e.getCommandTags();if(tags.contains(TAG)||tags.contains("gravesandglory-camp-v3")||tags.contains("gravesandglory-lobby")||tags.contains("bannerbound-lobby"))old.add(e);}
        old.forEach(Entity::discard);
        for(var s:Spec.CAMPSTATIONS){if(!s.npc().equals("-"))npc(w,s);label(w,s);}
        lessons(w);dummies(w);
    }
    static void npc(ServerWorld w,Spec.CampStationsRow s){
        var c=Spec.CAMP[0];VillagerEntity v=new VillagerEntity(EntityType.VILLAGER,w);
        v.refreshPositionAndAngles(c.originX()+s.x(),s.y(),c.originZ()+s.z(),(float)s.yaw(),0);v.setHeadYaw((float)s.yaw());v.setBodyYaw((float)s.yaw());
        v.setAiDisabled(true);v.setInvulnerable(true);v.setSilent(true);v.setPersistent();
        var profession=Registries.VILLAGER_PROFESSION.get(new Identifier("minecraft",s.npc()));
        v.setVillagerData(v.getVillagerData().withProfession(profession).withLevel(5));
        v.setCustomName(Text.literal(s.label().split(" / ")[0]));v.setCustomNameVisible(false);
        v.addCommandTag(TAG);v.addCommandTag("camp-generation:"+generation);v.addCommandTag("camp:"+s.page());
        w.spawnEntity(v);
    }
    /** The floating text of a row, 3.7 blocks above its y: the same text display style as before, the text from text(). */
    static void label(ServerWorld w,Spec.CampStationsRow s){
        var c=Spec.CAMP[0];display(w,s.label(),c.originX()+s.x(),s.y()+3.7,c.originZ()+s.z(),"camp:"+s.page());
    }
    /** One text display in the label style (centre billboard, dark panel, shadow, 1.7 scale) at a world position, text through text(), tagged TAG, the generation and the extra tag. */
    static void display(ServerWorld w,String text,double x,double y,double z,String extraTag){display(w,text(text),x,y,z,extraTag,"center",1.7f,220,0f);}
    /** One text display with the label's dark panel and shadow: the billboard mode (center turns to the viewer, fixed stands at the yaw), the scale, the line width in pixels and the yaw. */
    static void display(ServerWorld w,Text text,double x,double y,double z,String extraTag,String billboard,float size,int lineWidth,float yaw){
        var label=EntityType.TEXT_DISPLAY.create(w);if(label==null)return;
        var nbt=label.writeNbt(new net.minecraft.nbt.NbtCompound());nbt.putString("text",Text.Serializer.toJson(text));
        nbt.putString("billboard",billboard);nbt.putInt("background",0xb0192227);nbt.putBoolean("shadow",true);nbt.putInt("line_width",lineWidth);nbt.putFloat("view_range",2f);
        var transform=new net.minecraft.nbt.NbtCompound();var scale=new net.minecraft.nbt.NbtList();for(int i=0;i<3;i++)scale.add(net.minecraft.nbt.NbtFloat.of(size));transform.put("scale",scale);var translation=new net.minecraft.nbt.NbtList();for(int i=0;i<3;i++)translation.add(net.minecraft.nbt.NbtFloat.of(0));transform.put("translation",translation);
        for(String key:new String[]{"left_rotation","right_rotation"}){var rotation=new net.minecraft.nbt.NbtList();for(int i=0;i<4;i++)rotation.add(net.minecraft.nbt.NbtFloat.of(i==3?1:0));transform.put(key,rotation);}nbt.put("transformation",transform);
        label.readNbt(nbt);label.refreshPositionAndAngles(x,y,z,yaw,0);
        label.addCommandTag(TAG);label.addCommandTag("camp-generation:"+generation);label.addCommandTag(extraTag);w.spawnEntity(label);
    }

    // ---- the training yard ----

    /** The lesson boards: a fixed panel facing east (yaw -90) at 1.0 scale wrapped at 112 pixels, so it is 2.85 blocks wide and lies on its three-block backboard of the yard piece (camp/training_yard, local x 0)
     *  instead of turning to the viewer: four turning 1.7 panels 9 blocks wide on rows 6 blocks apart overlapped into one garbled line from the bowl (Task 3 grade). */
    static final float LESSON_SCALE=1.0f,LESSON_YAW=-90f;
    static final int LESSON_LINE=112;
    /** The lesson boards of the yard: one text display per campLessons row at (originX+dx, y, originZ+dz), tagged TAG and camp-lesson:<id>; {key:<id>} in a text becomes the keybind, so the board shows the key the player has set. */
    static void lessons(ServerWorld w){
        var c=Spec.CAMP[0];
        for(var l:Spec.CAMPLESSONS)display(w,lessonText(l.text()),c.originX()+l.dx(),l.y(),c.originZ()+l.dz(),"camp-lesson:"+l.id(),"fixed",LESSON_SCALE,LESSON_LINE,LESSON_YAW);
    }
    /** A lesson as Text: the part before the first " / " is a bold heading on its own line, the rest goes through text() (gold, keybinds). */
    static Text lessonText(String lesson){
        int cut=lesson.indexOf(" / ");if(cut<0)return text(lesson);
        return Text.empty().append(Text.literal(lesson.substring(0,cut)).formatted(Formatting.BOLD)).append("\n").append(text(lesson.substring(cut+3))).formatted(Formatting.GOLD);
    }
    /** How many dummies stand in the yard, and where the first one stands (dx, dz from the origin of the camp, y): the others follow every two blocks along x. */
    static final int DUMMIES=6;
    static final double DUMMY_DX=-52.5,DUMMY_DZ=0.5,DUMMY_Y=65;
    /** The six practice dummies: armor stands facing west (yaw -90) in an iron helmet and a leather chestplate, invulnerable, no gravity, tagged TAG and camp-dummy. */
    static void dummies(ServerWorld w){
        var c=Spec.CAMP[0];
        for(int k=0;k<DUMMIES;k++){
            var stand=new net.minecraft.entity.decoration.ArmorStandEntity(w,c.originX()+DUMMY_DX+2*k,DUMMY_Y,c.originZ()+DUMMY_DZ);
            stand.refreshPositionAndAngles(c.originX()+DUMMY_DX+2*k,DUMMY_Y,c.originZ()+DUMMY_DZ,-90f,0f);
            stand.setInvulnerable(true);stand.setNoGravity(true);
            stand.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_HELMET));
            stand.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new net.minecraft.item.ItemStack(net.minecraft.item.Items.LEATHER_CHESTPLATE));
            stand.addCommandTag(TAG);stand.addCommandTag("camp-generation:"+generation);stand.addCommandTag("camp-dummy");
            w.spawnEntity(stand);
        }
    }

    // ---- the war table ----

    /** Lays the carpets of the war table: cell (i,j) at (originX+tableDx+i, tableY, originZ+tableDz+j) shows the map that stands. Runs at the camp build and after every map build. */
    static void warTable(ServerWorld w){
        var c=Spec.CAMP[0];
        for(int i=0;i<15;i++)for(int j=0;j<9;j++)w.setBlockState(new BlockPos(c.originX()+c.tableDx()+i,c.tableY(),c.originZ()+c.tableDz()+j),tableCell(i,j),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
    }
    /** What cell (i,j), i 0..14 and j 0..8, of the table shows: the arena x from -350+i*700/15 to -350+(i+1)*700/15 and z from -250+j*500/9 to -250+(j+1)*500/9, sampled 7 by 7. First match wins: a capture point in it, the lit candle;
     *  any bridge sample, light gray; any river sample, cyan; 7 or more road samples, brown; 25 or more samples above y 79, gray; otherwise white on Frostmarch and green elsewhere. */
    static BlockState tableCell(int i,int j){
        double x0=-350+i*700.0/15,x1=-350+(i+1)*700.0/15,z0=-250+j*500.0/9,z1=-250+(j+1)*500.0/9;
        for(var p:Spec.POINTS)if(p.x()>=x0&&p.x()<x1&&p.z()>=z0&&p.z()<z1)return Blocks.CANDLE.getDefaultState().with(CandleBlock.LIT,true);
        boolean bridge=false,river=false;int road=0,high=0;
        for(int a=0;a<7;a++)for(int b=0;b<7;b++){
            double x=x0+(x1-x0)*(a+.5)/7,z=z0+(z1-z0)*(b+.5)/7;int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
            for(var s:Spec.STRUCTURES)if(s.kind().equals("bridge")&&Math.abs(x-s.x())<=s.width()/2.0&&Math.abs(z-s.z())<=s.depth()/2.0)bridge=true;
            if(CampaignMaps.river(bx,bz))river=true;
            if(CampaignMaps.road(bx,bz))road++;
            if(Arena.height(bx,bz)>79)high++;
        }
        if(bridge)return Blocks.LIGHT_GRAY_CARPET.getDefaultState();
        if(river)return Blocks.CYAN_CARPET.getDefaultState();
        if(road>=7)return Blocks.BROWN_CARPET.getDefaultState();
        if(high>=25)return Blocks.GRAY_CARPET.getDefaultState();
        return (CampaignMaps.selected==2?Blocks.WHITE_CARPET:Blocks.GREEN_CARPET).getDefaultState();
    }
}
