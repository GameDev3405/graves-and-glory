package gg.gravesandglory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.processor.BlockIgnoreStructureProcessor;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.CRC32;

/** Authored pieces: vanilla structure files in the mod resources (data/gravesandglory/structures/GROUP/NAME.nbt), read here and never from the
 *  generated folder of a world, placed rotated about their centre block. Server side only: the pure functions are also called by the client. */
public final class Structures {
    public static final String NS="gravesandglory";
    /** One placement: the piece, how many blocks its file holds, the time it took, the footprint, whether the file was not there, and the landmarks row that placed it ("-" for any other placement). */
    public record Placed(String id,int blocks,long nanos,BlockBox box,boolean missing,String row){
        public Placed(String id,int blocks,long nanos,BlockBox box,boolean missing){this(id,blocks,nanos,box,missing,"-");}
    }
    /** The last 256 placements, oldest first. */
    public static final List<Placed> LOG=new CopyOnWriteArrayList<>();
    static final int LOG_LIMIT=256;
    record Loaded(StructureTemplate template,int blocks){}
    static final Map<String,Loaded> CACHE=new ConcurrentHashMap<>();
    static{
        // A reload can bring other files, and a new server another set of packs: nothing read earlier may outlive either.
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,manager,success)->CACHE.clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server->CACHE.clear());
    }
    private Structures(){}

    /** Forgets every template read so far; the next template() reads the file again. */
    public static void clearCache(){CACHE.clear();}

    /** The structure file of a piece as NBT: data/gravesandglory/structures/ID.nbt through the resource manager of the server, which is the mod and
     *  its packs. A world generated folder is never read. Empty when there is no such file or it cannot be read. */
    public static Optional<NbtCompound> read(MinecraftServer server,String id){
        try{
            var resource=server.getResourceManager().getResource(new Identifier(NS,"structures/"+id+".nbt"));
            if(resource.isEmpty())return Optional.empty();
            try(InputStream in=resource.get().getInputStream()){return Optional.of(NbtIo.readCompressed(in));}
        }catch(Exception e){System.err.println("[Graves and Glory] structure unreadable: "+id+": "+e);return Optional.empty();}
    }

    static Loaded load(ServerWorld w,String id){
        Loaded known=CACHE.get(id);if(known!=null)return known;
        Optional<NbtCompound> nbt=read(w.getServer(),id);if(nbt.isEmpty())return null;
        try{
            StructureTemplate template=w.getStructureTemplateManager().createTemplate(nbt.get());
            Loaded made=new Loaded(template,nbt.get().getList("blocks",NbtElement.COMPOUND_TYPE).size()); // counted in the file as read: serialising the template again would double the first placement of a large piece
            CACHE.put(id,made);return made;
        }catch(Exception e){System.err.println("[Graves and Glory] structure unreadable: "+id+": "+e);return null;}
    }

    /** The template of a piece, read once and kept until the data packs reload. */
    public static Optional<StructureTemplate> template(ServerWorld w,String id){
        Loaded piece=load(w,id);return piece==null?Optional.empty():Optional.of(piece.template());
    }

    /** 0, 90, 180 or 270 degrees clockwise seen from above; anything else is a mistake of the caller. */
    public static BlockRotation rotation(int degrees){
        return switch(degrees){
            case 0->BlockRotation.NONE;
            case 90->BlockRotation.CLOCKWISE_90;
            case 180->BlockRotation.CLOCKWISE_180;
            case 270->BlockRotation.COUNTERCLOCKWISE_90;
            default->throw new IllegalArgumentException("rotation must be 0, 90, 180 or 270, not "+degrees);
        };
    }

    /** The centre block of a piece in its own cells: the middle column of an odd footprint, on the ground layer. */
    static BlockPos pivot(Vec3i size){return new BlockPos((size.getX()-1)/2,0,(size.getZ()-1)/2);}

    /** Turns the offset (dx,dz) from the pivot clockwise by the given rotation (x east, z south, so east turns to south). */
    static double[] turn(int rotation,double dx,double dz){
        return switch(rotation){
            case 0->new double[]{dx,dz};
            case 90->new double[]{-dz,dx};
            case 180->new double[]{-dx,-dz};
            case 270->new double[]{dz,-dx};
            default->throw new IllegalArgumentException("rotation must be 0, 90, 180 or 270, not "+rotation);
        };
    }

    /** Where cell (lx,ly,lz) of a piece lands when its centre block is placed at (cx,y,cz) and it is turned by rotation degrees: (cx,y,cz) plus the
     *  offset of the cell from the pivot P=((sx-1)/2,0,(sz-1)/2), turned. The centre block stays where it is. */
    public static BlockPos toWorld(Vec3i size,int cx,int y,int cz,int rotation,int lx,int ly,int lz){
        Vec3i p=pivot(size);double[] d=turn(rotation,lx-p.getX(),lz-p.getZ());
        return new BlockPos(cx+(int)d[0],y+ly,cz+(int)d[1]);
    }

    /** The same for a point of the piece (cell corner 0, cell centre 0.5), about the centre of the centre block: (cx+.5,y+ly,cz+.5) plus the offset
     *  of the point from (P.x+.5,P.z+.5), turned. */
    public static Vec3d toWorld(Vec3i size,int cx,int y,int cz,int rotation,double lx,double ly,double lz){
        Vec3i p=pivot(size);double[] d=turn(rotation,lx-(p.getX()+.5),lz-(p.getZ()+.5));
        return new Vec3d(cx+.5+d[0],y+ly,cz+.5+d[1]);
    }

    /** The blocks a piece covers once placed: the turned footprint of its box, from the ground layer up. */
    public static BlockBox footprint(Vec3i size,int cx,int y,int cz,int rotation){
        return BlockBox.create(toWorld(size,cx,y,cz,rotation,0,0,0),toWorld(size,cx,y,cz,rotation,size.getX()-1,size.getY()-1,size.getZ()-1));
    }

    static Placed log(Placed placed){
        LOG.add(placed);while(LOG.size()>LOG_LIMIT)LOG.remove(0);return placed;
    }

    /** Places a piece with its centre block at (cx,y,cz), turned by rotation degrees. Structure void keeps what is there; air clears. A piece with
     *  no file places nothing, logs it and comes back with missing=true; nothing throws but a rotation that is not 0, 90, 180 or 270. */
    public static Placed place(ServerWorld w,String id,int cx,int y,int cz,int rotation){return place(w,id,cx,y,cz,rotation,"-");}
    static Placed place(ServerWorld w,String id,int cx,int y,int cz,int rotation,String row){
        BlockRotation turn=rotation(rotation);
        long started=System.nanoTime();
        Loaded piece=load(w,id);
        if(piece==null){
            System.out.println("[Graves and Glory] structure missing: "+id);
            return log(new Placed(id,0,System.nanoTime()-started,new BlockBox(cx,y,cz,cx,y,cz),true,row));
        }
        StructureTemplate template=piece.template();Vec3i size=template.getSize();
        BlockPos pivot=pivot(size),origin=new BlockPos(cx,y,cz).subtract(pivot);
        StructurePlacementData data=new StructurePlacementData().setRotation(turn).setMirror(BlockMirror.NONE).setIgnoreEntities(true).setUpdateNeighbors(false).addProcessor(BlockIgnoreStructureProcessor.IGNORE_STRUCTURE_BLOCKS).setPosition(pivot);
        boolean any=template.place(w,origin,origin,data,w.getRandom(),Block.NOTIFY_LISTENERS);
        return log(new Placed(id,any?piece.blocks():0,System.nanoTime()-started,footprint(size,cx,y,cz,rotation),false,row));
    }

    // ---- landmarks (Phase 3): one piece per tick after the column build of a map ----
    /** The landmarks rows of the selected map still to place, in sheet order. Server thread only. */
    static final ArrayDeque<Spec.LandmarksRow> QUEUE=new ArrayDeque<>();
    /** Harness only: mixed into hash(), so a check can make the marker of a built map stale. */
    public static String salt="";
    /** Queues the landmarks rows of the selected map (CampaignMaps.selected) in sheet order, after whatever was queued is dropped. */
    public static void queueLandmarks(ServerWorld w){QUEUE.clear();for(var r:Spec.LANDMARKS)if(r.map()==CampaignMaps.selected)QUEUE.add(r);}
    /** Places the next queued landmark (its centre block at (x, elevation, z), turned by its rotation) and comes back true once nothing is left. */
    public static boolean step(ServerWorld w){
        var r=QUEUE.poll();
        if(r!=null)place(w,r.template(),r.x(),r.elevation(),r.z(),r.rotation(),r.id());
        return QUEUE.isEmpty();
    }
    /** Drops every queued landmark: a map switch must not land a piece of the old map on the new one. */
    public static void clearQueue(){QUEUE.clear();}
    /** How many landmarks are still queued. */
    public static int queued(){return QUEUE.size();}
    /** CRC32 (hex) of what the landmarks of a map are made of: its landmarks rows, the bytes of their templates' structure files, the landmarkLines rows of those templates, and salt.
     *  It is in the map cache marker, so a world whose map was built with other landmarks builds that map once more. */
    public static String hash(int map){
        var crc=new CRC32();
        for(var r:Spec.LANDMARKS)if(r.map()==map){
            crc.update(r.toString().getBytes(StandardCharsets.UTF_8));
            crc.update(fileBytes(r.template()));
            for(var l:Spec.LANDMARKLINES)if(l.template().equals(r.template()))crc.update(l.toString().getBytes(StandardCharsets.UTF_8));
        }
        crc.update(salt.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc.getValue());
    }
    /** The bytes of a piece's structure file as the mod ships it (data/gravesandglory/structures/ID.nbt on the class path); empty when there is none. */
    static byte[] fileBytes(String id){
        try(InputStream in=Structures.class.getResourceAsStream("/data/"+NS+"/structures/"+id+".nbt")){return in==null?new byte[0]:in.readAllBytes();}
        catch(Exception e){return new byte[0];}
    }
}
