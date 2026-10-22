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
    /** Part of the hash: a change to the way the terrain is written makes every world rebuild it. */
    static final String TERRAIN="terrain-1";
    /** Where the plateau begins and where its bank starts at the foot (dz, from the origin of the camp): the bank rises over the 13 blocks between them. */
    static final int RIDGE_TOP_DZ=-51,RIDGE_FOOT_DZ=-38;
    static String generation="";
    /** Every line this class logged, oldest first (the harness reads them as well as the console). */
    static final List<String> LINES=new CopyOnWriteArrayList<>();
    static void log(String line){System.out.println(line);LINES.add(line);}

    record Spot(double x,double y,double z,float yaw,float pitch){}

    // ---- the pure functions ----

    /** Where a commander stands after toLobby: the camp spawn. Task 5 adds the first arrival, which is why the profile comes with it. */
    static Spot spawn(Campaign.Profile p){var c=Spec.CAMP[0];return new Spot(c.originX()+c.spawnDx(),c.spawnY(),c.originZ()+c.spawnDz(),(float)c.spawnYaw(),(float)c.spawnPitch());}

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
        crc.update(TERRAIN.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc.getValue());
    }
    static Path marker(ServerWorld w){return w.getServer().getSavePath(WorldSavePath.ROOT).resolve(MARKER_FILE);}
    static boolean built(ServerWorld w,String hash){
        try{return com.google.gson.JsonParser.parseString(Files.readString(marker(w))).getAsJsonObject().get("terrain").getAsString().equals(hash);}catch(Exception e){return false;}
    }
    static void mark(ServerWorld w,String hash){
        try{Files.writeString(marker(w),"{\"terrain\":\""+hash+"\"}");}catch(Exception e){System.err.println("[Graves and Glory] camp terrain marker could not be written: "+e);}
    }

    /** The height of the island at (dx,dz) from the origin of the camp, before the pieces: the floor, the plateau and the bank to it, the slope behind the plateau, the flanks, the southern skirt, and outside the bowl a gentle swell. */
    static int height(Spec.CampRow c,int dx,int dz){
        double rise=c.plateauY()-c.floorY(),h=c.floorY();
        if(dz<=RIDGE_TOP_DZ)h+=rise;else if(dz<RIDGE_FOOT_DZ)h+=rise*(RIDGE_FOOT_DZ-dz)/(double)(RIDGE_FOOT_DZ-RIDGE_TOP_DZ);
        if(dz<c.minDz())h+=Math.min(18,0.7*(c.minDz()-dz));
        if(Math.abs(dx)>c.maxDx())h+=Math.min(20,0.8*(Math.abs(dx)-c.maxDx()));
        if(dz>c.maxDz())h-=Math.min(2,(dz-c.maxDz())*0.5);
        if(!inBowl(c,dx,dz))h+=2*Math.sin(dx*.21)*Math.cos(dz*.17);
        return (int)Math.round(h);
    }
    static boolean inBowl(Spec.CampRow c,int dx,int dz){return dx>=c.minDx()&&dx<=c.maxDx()&&dz>=c.minDz()&&dz<=c.maxDz();}

    /** The terrain: the height of every column of the island box, the rows of campPieces laid over it (a flatten row's footprint takes its floor y, a flatten=false footprint keeps the ground, and a cell within two of a flatten row's footprint takes the
     *  highest such floor), stone under dirt under grass (stone from y 80), the air above it up to the top of what stood there, then the spruces outside the bowl.
     *  Written with Arena's build flags (2 and 16): no shape update runs, so taking away the blocks of an older camp cannot break and spill what stood against them, and the terrain holds nothing a shape update would change
     *  (stone, dirt, grass, air, spruce logs and persistent leaves). The pieces come after, through Structures.place, which does run them. */
    static void terrain(ServerWorld w){
        var c=Spec.CAMP[0];int width=c.islandMaxDx()-c.islandMinDx()+1,depth=c.islandMaxDz()-c.islandMinDz()+1;
        int[] ground=new int[width*depth];
        var rows=Spec.CAMPPIECES;var boxes=new BlockBox[rows.length];for(int i=0;i<rows.length;i++)boxes[i]=box(rows[i]);
        for(int i=0;i<width;i++)for(int k=0;k<depth;k++){
            int dx=c.islandMinDx()+i,dz=c.islandMinDz()+k,x=c.originX()+dx,z=c.originZ()+dz,h=height(c,dx,dz);
            Integer flat=null,apron=null;boolean inside=false;
            for(int r=0;r<rows.length;r++){
                var b=boxes[r];
                if(x>=b.getMinX()&&x<=b.getMaxX()&&z>=b.getMinZ()&&z<=b.getMaxZ()){inside=true;if(rows[r].flatten())flat=flat==null?rows[r].y():Math.max(flat,rows[r].y());}
                else if(rows[r].flatten()&&x>=b.getMinX()-2&&x<=b.getMaxX()+2&&z>=b.getMinZ()-2&&z<=b.getMaxZ()+2)apron=apron==null?rows[r].y():Math.max(apron,rows[r].y());
            }
            ground[i*depth+k]=flat!=null?flat:inside?h:apron!=null?apron:h;
        }
        long t0=System.nanoTime();
        for(int cx=(c.originX()+c.islandMinDx()-2)>>4;cx<=(c.originX()+c.islandMaxDx()+2)>>4;cx++)for(int cz=(c.originZ()+c.islandMinDz()-2)>>4;cz<=(c.originZ()+c.islandMaxDz()+2)>>4;cz++)w.getChunk(cx,cz);
        long t1=System.nanoTime();
        boolean was=Arena.building;Arena.building=true;
        try{
            int flags=Arena.flags();var pos=new BlockPos.Mutable();
            for(int i=0;i<width;i++)for(int k=0;k<depth;k++){
                int x=c.originX()+c.islandMinDx()+i,z=c.originZ()+c.islandMinDz()+k,h=ground[i*depth+k];
                w.getChunk(x>>4,z>>4); // load it first: getTopY answers the world bottom for a chunk that is not loaded
                int top=w.getTopY(Heightmap.Type.WORLD_SURFACE,x,z);
                for(int y=top;y>h;y--)if(!w.getBlockState(pos.set(x,y,z)).isAir())w.setBlockState(pos,Blocks.AIR.getDefaultState(),flags); // from the top down: nothing is left standing on a block that has gone
                for(int y=c.islandBottomY();y<h-1;y++)w.setBlockState(pos.set(x,y,z),Blocks.STONE.getDefaultState(),flags);
                w.setBlockState(pos.set(x,h-1,z),Blocks.DIRT.getDefaultState(),flags);
                w.setBlockState(pos.set(x,h,z),(h>=80?Blocks.STONE:Blocks.GRASS_BLOCK).getDefaultState(),flags);
            }
            long t2=System.nanoTime();
            for(int i=0;i<width;i++)for(int k=0;k<depth;k++){
                int dx=c.islandMinDx()+i,dz=c.islandMinDz()+k,h=ground[i*depth+k];
                if(!inBowl(c,dx,dz)&&h>64&&h<88&&Math.floorMod(dx*734287+dz*912931,23)==0)Arena.tree(w,c.originX()+dx,h+1,c.originZ()+dz,5,8+Math.floorMod(dx+dz,4),true);
            }
            log("[Graves and Glory] camp terrain detail: chunks "+millis(t0,t1)+" ms, columns "+millis(t1,t2)+" ms, trees "+millis(t2,System.nanoTime())+" ms");
        }finally{Arena.building=was;}
    }

    // ---- the people ----

    /** Discards what the older camps and this one left (every tag in the discard list), then spawns the villager of each station row with an npc and the label of every row. */
    static void people(ServerWorld w){
        var old=new ArrayList<Entity>();
        for(var e:w.iterateEntities()){var tags=e.getCommandTags();if(tags.contains(TAG)||tags.contains("gravesandglory-camp-v3")||tags.contains("gravesandglory-lobby")||tags.contains("bannerbound-lobby"))old.add(e);}
        old.forEach(Entity::discard);
        for(var s:Spec.CAMPSTATIONS){if(!s.npc().equals("-"))npc(w,s);label(w,s);}
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
        var c=Spec.CAMP[0];var label=EntityType.TEXT_DISPLAY.create(w);if(label==null)return;
        var nbt=label.writeNbt(new net.minecraft.nbt.NbtCompound());nbt.putString("text",Text.Serializer.toJson(text(s.label())));
        nbt.putString("billboard","center");nbt.putInt("background",0xb0192227);nbt.putBoolean("shadow",true);nbt.putInt("line_width",220);nbt.putFloat("view_range",2f);
        var transform=new net.minecraft.nbt.NbtCompound();var scale=new net.minecraft.nbt.NbtList();for(int i=0;i<3;i++)scale.add(net.minecraft.nbt.NbtFloat.of(1.7f));transform.put("scale",scale);var translation=new net.minecraft.nbt.NbtList();for(int i=0;i<3;i++)translation.add(net.minecraft.nbt.NbtFloat.of(0));transform.put("translation",translation);
        for(String key:new String[]{"left_rotation","right_rotation"}){var rotation=new net.minecraft.nbt.NbtList();for(int i=0;i<4;i++)rotation.add(net.minecraft.nbt.NbtFloat.of(i==3?1:0));transform.put(key,rotation);}nbt.put("transformation",transform);
        label.readNbt(nbt);label.refreshPositionAndAngles(c.originX()+s.x(),s.y()+3.7,c.originZ()+s.z(),0,0);
        label.addCommandTag(TAG);label.addCommandTag("camp-generation:"+generation);label.addCommandTag("camp:"+s.page());w.spawnEntity(label);
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
