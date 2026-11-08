package gg.gravesandglory;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CommandBlock;
import net.minecraft.block.LightBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Props in the world (prop kit plan task S4). No registry entries: a prop is a vanilla command block (the anchor, one block under the anchor
 * cell, {@code facing} = prop facing, {@code Command} = {@code gravesandglory:prop/<id> floor=<state>}, the floor block it replaced, never run), {@code barrier} blocks on its solid
 * cells, {@code light} blocks on its light cells and one {@code item_display} per part, tagged {@code gg_prop}, {@code gg_prop@x,y,z}
 * (anchor cell) and {@code gg_prop#<cmd>} (which part). The anchor is the truth: the scanner (chunk load, {@link #scanBox}) spawns missing
 * parts, discards duplicates and discards parts whose anchor is gone.
 */
public final class Props {
    private Props(){}
    static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("gravesandglory");
    public static final String TAG="gg_prop",ANCHOR_PREFIX="gravesandglory:prop/",FLOOR_KEY="floor=";
    /** Item display view range multiplier, so props show at build distances. */
    static final float VIEW_RANGE=4f;
    /** Ticks a loaded chunk waits for its entities before the scan gives up on it. */
    static final int CHUNK_WAIT=200;
    /** Alive effects play only while a player in the same world is within this many blocks of the anchor cell (S5). */
    public static final double RANGE=24.0;

    /** Defs by id, reloaded from {@code data/<ns>/gg_props/*.json} on every data reload. */
    public static volatile Map<String,PropDef> DEFS=Map.of();
    public record Live(BlockPos anchor,PropDef def,int facing){}

    private static final Map<World,Map<BlockPos,Live>> LIVE=new HashMap<>();
    private static final Map<World,Map<Long,Integer>> PENDING_CHUNKS=new HashMap<>();
    private static final Map<World,List<Entity>> PENDING_ENTITIES=new HashMap<>();

    public static void init(){
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            @Override public Identifier getFabricId(){return new Identifier("gravesandglory","props");}
            @Override public void reload(ResourceManager manager){DEFS=load(manager);}
        });
        ServerChunkEvents.CHUNK_LOAD.register((w,chunk)->PENDING_CHUNKS.computeIfAbsent(w,k->new HashMap<>()).put(chunk.getPos().toLong(),0));
        ServerChunkEvents.CHUNK_UNLOAD.register(Props::chunkUnloaded);
        ServerEntityEvents.ENTITY_LOAD.register((e,w)->{
            if(e instanceof DisplayEntity.ItemDisplayEntity&&e.getCommandTags().contains(TAG))PENDING_ENTITIES.computeIfAbsent(w,k->new ArrayList<>()).add(e);
        });
        ServerTickEvents.END_WORLD_TICK.register(Props::tick);
        ServerWorldEvents.UNLOAD.register((server,w)->{LIVE.remove(w);PENDING_CHUNKS.remove(w);PENDING_ENTITIES.remove(w);});
        CommandRegistrationCallback.EVENT.register((d,access,env)->d.register(CommandManager.literal("prop").requires(s->s.hasPermissionLevel(2))
            .then(CommandManager.literal("place").then(CommandManager.argument("id",StringArgumentType.word()).suggests((c,b)->CommandSource.suggestMatching(DEFS.keySet(),b))
                .then(CommandManager.argument("pos",BlockPosArgumentType.blockPos()).then(CommandManager.argument("facing",IntegerArgumentType.integer(0,270)).executes(c->{
                    BlockPos pos=BlockPosArgumentType.getBlockPos(c,"pos");
                    String why=place(c.getSource().getWorld(),StringArgumentType.getString(c,"id"),pos,IntegerArgumentType.getInteger(c,"facing"));
                    if(!why.isEmpty()){c.getSource().sendError(Text.literal(why));return 0;}
                    c.getSource().sendFeedback(()->Text.literal("Placed "+StringArgumentType.getString(c,"id")+" at "+pos.toShortString()),true);return 1;})))))
            .then(CommandManager.literal("remove").then(CommandManager.argument("pos",BlockPosArgumentType.blockPos()).executes(c->{
                BlockPos pos=BlockPosArgumentType.getBlockPos(c,"pos");
                if(!remove(c.getSource().getWorld(),pos)){c.getSource().sendError(Text.literal("No prop at "+pos.toShortString()));return 0;}
                c.getSource().sendFeedback(()->Text.literal("Removed the prop at "+pos.toShortString()),true);return 1;})))));
    }

    static Map<String,PropDef> load(ResourceManager manager){
        Map<String,PropDef> out=new TreeMap<>();
        for(Map.Entry<Identifier,Resource> e:manager.findResources("gg_props",id->id.getPath().endsWith(".json")).entrySet()){
            try(Reader r=e.getValue().getReader()){
                StringBuilder sb=new StringBuilder();char[] buf=new char[4096];int n;
                while((n=r.read(buf))>0)sb.append(buf,0,n);
                PropDef d=PropDef.parse(sb.toString());
                if(out.put(d.id(),d)!=null)LOG.warn("[props] duplicate prop id {} in {}",d.id(),e.getKey());
            }catch(Exception x){LOG.error("[props] skipped {}: {}",e.getKey(),x.getMessage());}
        }
        LOG.info("[props] loaded {} prop defs",out.size());
        return java.util.Collections.unmodifiableMap(out);
    }

    // ---- geometry in the world --------------------------------------------------------------------------------------------------------

    /** The world cells of a prop: solid cells to barrier, light cells to light, in that order. */
    static List<BlockPos> solidCells(PropDef d,BlockPos anchor,int facing){
        List<BlockPos> out=new ArrayList<>();
        for(int[] c:d.solid()){int[] o=PropGeometry.cell(d,facing,c[0],c[1],c[2]);out.add(anchor.add(o[0],o[1],o[2]));}
        return out;
    }
    static BlockPos lightCell(PropDef d,BlockPos anchor,int facing,PropDef.Light l){
        int[] o=PropGeometry.cell(d,facing,l.x(),l.y(),l.z());return anchor.add(o[0],o[1],o[2]);
    }
    static String anchorTag(BlockPos anchor){return TAG+"@"+anchor.getX()+","+anchor.getY()+","+anchor.getZ();}
    static String partTag(int cmd){return TAG+"#"+cmd;}

    /** A cell a prop may take: air or replaceable, and not another prop's barrier or light. */
    static boolean free(BlockState s){return (s.isAir()||s.isReplaceable())&&!s.isOf(Blocks.LIGHT)&&!s.isOf(Blocks.BARRIER);}
    /** The anchor replaces the floor block under the anchor cell: anything but another anchor, a block entity, a prop cell or an
     *  unbreakable block. */
    static boolean anchorFree(ServerWorld w,BlockPos p){
        BlockState s=w.getBlockState(p);
        return w.getBlockEntity(p)==null&&!s.isOf(Blocks.BARRIER)&&!s.isOf(Blocks.LIGHT)&&s.getHardness(w,p)>=0;
    }

    // ---- place / remove ---------------------------------------------------------------------------------------------------------------

    /** Places prop {@code id} with its anchor cell at {@code anchorCell}; "" on success, else the refusal text and nothing changed. */
    public static String place(ServerWorld w,String id,BlockPos anchorCell,int facing){
        String why=check(w,id,anchorCell,facing);
        if(!why.isEmpty())return why;
        PropDef d=DEFS.get(id);
        BlockPos base=anchorCell.down();
        List<BlockPos> solid=solidCells(d,anchorCell,facing);
        BlockState floor=w.getBlockState(base);
        w.setBlockState(base,Blocks.COMMAND_BLOCK.getDefaultState().with(CommandBlock.FACING,Direction.byName(PropGeometry.direction(facing))).with(CommandBlock.CONDITIONAL,false),3);
        if(w.getBlockEntity(base) instanceof CommandBlockBlockEntity cb){
            cb.getCommandExecutor().setCommand(anchorCommand(id,floor));cb.setAuto(false);cb.markDirty();
        }
        for(BlockPos p:solid)w.setBlockState(p,Blocks.BARRIER.getDefaultState(),3);
        for(PropDef.Light l:d.lights())w.setBlockState(lightCell(d,anchorCell,facing,l),Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15,l.level()),3);
        ensureParts(w,anchorCell.toImmutable(),d,facing);
        return "";
    }

    /** Whether prop {@code id} could be placed with its anchor cell at {@code anchorCell}: "" if so, else the refusal text. Changes nothing
     *  (the S6 ghost turns red on a refusal). */
    public static String check(ServerWorld w,String id,BlockPos anchorCell,int facing){
        PropDef d=DEFS.get(id);
        if(d==null)return "Unknown prop "+id;
        if(facing!=0&&facing!=90&&facing!=180&&facing!=270)return "Facing must be 0, 90, 180 or 270, not "+facing;
        BlockPos base=anchorCell.down();
        List<BlockPos> solid=solidCells(d,anchorCell,facing);
        List<BlockPos> all=new ArrayList<>(solid);
        for(PropDef.Light l:d.lights())all.add(lightCell(d,anchorCell,facing,l));
        all.add(base);
        for(BlockPos p:all){
            if(w.isOutOfHeightLimit(p)||!w.getWorldBorder().contains(p))return "Out of world at "+p.getX()+" "+p.getY()+" "+p.getZ();
            if(!w.getChunkManager().isChunkLoaded(p.getX()>>4,p.getZ()>>4))return "Out of world at "+p.getX()+" "+p.getY()+" "+p.getZ()+" (not loaded)";
        }
        for(BlockPos p:all){
            boolean ok=p.equals(base)?anchorFree(w,p):free(w.getBlockState(p));
            if(!ok)return "Overlap at "+p.getX()+" "+p.getY()+" "+p.getZ()+" ("+w.getBlockState(p).getBlock().getName().getString()+")";
        }
        return "";
    }

    /** The anchor's {@code Command} text: {@code gravesandglory:prop/<id> floor=<block state>}, the floor block the anchor replaced (vanilla
     *  keeps no custom NBT key on a command block, so the floor rides in the text; remove puts it back). */
    static String anchorCommand(String id,BlockState floor){
        return ANCHOR_PREFIX+id+" "+FLOOR_KEY+BlockArgumentParser.stringifyBlockState(floor);
    }
    /** The prop id of an anchor {@code Command} text, or null if it is not one. */
    static String anchorId(String cmd){
        if(cmd==null||!cmd.startsWith(ANCHOR_PREFIX))return null;
        int sp=cmd.indexOf(' ',ANCHOR_PREFIX.length());
        return cmd.substring(ANCHOR_PREFIX.length(),sp<0?cmd.length():sp);
    }
    /** The floor block saved in an anchor {@code Command} text; air when there is none or it no longer parses. */
    static BlockState anchorFloor(String cmd){
        int i=cmd==null?-1:cmd.indexOf(" "+FLOOR_KEY);
        if(i<0)return Blocks.AIR.getDefaultState();
        String s=cmd.substring(i+1+FLOOR_KEY.length()).trim();
        try{return BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(),s,false).blockState();}
        catch(Exception x){LOG.warn("[props] anchor floor {} does not parse: {}",s,x.getMessage());return Blocks.AIR.getDefaultState();}
    }

    /** Removes the prop covering {@code anyFootprintCell} (a solid or light cell, or its anchor): exactly its cells, anchor and parts. */
    public static boolean remove(ServerWorld w,BlockPos anyFootprintCell){
        BlockPos anchor=owner(w,anyFootprintCell);
        if(anchor==null)return false;
        Live l=readAnchor(w,anchor);
        for(Entity e:parts(w,anchor))e.discard();
        for(BlockPos p:solidCells(l.def(),anchor,l.facing()))if(w.getBlockState(p).isOf(Blocks.BARRIER))w.setBlockState(p,Blocks.AIR.getDefaultState(),3);
        for(PropDef.Light li:l.def().lights()){BlockPos p=lightCell(l.def(),anchor,l.facing(),li);if(w.getBlockState(p).isOf(Blocks.LIGHT))w.setBlockState(p,Blocks.AIR.getDefaultState(),3);}
        BlockState floor=w.getBlockEntity(anchor.down()) instanceof CommandBlockBlockEntity cb?anchorFloor(cb.getCommandExecutor().getCommand()):Blocks.AIR.getDefaultState();
        w.setBlockState(anchor.down(),floor,3);
        Map<BlockPos,Live> m=LIVE.get(w);if(m!=null)m.remove(anchor);
        return true;
    }

    /** The anchor cell of the prop whose solid cell, light cell or anchor block is {@code cell}, or null. */
    public static BlockPos owner(ServerWorld w,BlockPos cell){
        int r=1;
        for(PropDef d:DEFS.values())r=Math.max(r,Math.max(d.sx(),Math.max(d.sy(),d.sz())));
        BlockPos.Mutable p=new BlockPos.Mutable();
        for(int dy=-r;dy<=1;dy++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            p.set(cell.getX()+dx,cell.getY()+dy,cell.getZ()+dz);
            if(!w.getBlockState(p).isOf(Blocks.COMMAND_BLOCK))continue;
            BlockPos anchor=p.up();
            Live l=readAnchor(w,anchor);
            if(l==null)continue;
            if(cell.equals(p))return anchor;
            for(BlockPos c:solidCells(l.def(),anchor,l.facing()))if(c.equals(cell))return anchor;
            for(PropDef.Light li:l.def().lights())if(lightCell(l.def(),anchor,l.facing(),li).equals(cell))return anchor;
        }
        return null;
    }

    /** The prop whose anchor cell is {@code anchor}, read from the command block under it; null if there is none or its id is unknown. */
    static Live readAnchor(ServerWorld w,BlockPos anchor){
        BlockPos base=anchor.down();
        BlockState s=w.getBlockState(base);
        if(!s.isOf(Blocks.COMMAND_BLOCK))return null;
        BlockEntity be=w.getBlockEntity(base);
        if(!(be instanceof CommandBlockBlockEntity cb))return null;
        String cmd=cb.getCommandExecutor().getCommand();
        String id=anchorId(cmd);
        if(id==null)return null;
        PropDef d=DEFS.get(id);
        int facing=PropGeometry.facingOf(s.get(CommandBlock.FACING).asString());
        if(d==null||facing<0)return null;
        return new Live(anchor.toImmutable(),d,facing);
    }

    // ---- parts -------------------------------------------------------------------------------------------------------------------------

    static List<Entity> parts(ServerWorld w,BlockPos anchor){
        String tag=anchorTag(anchor);
        return new ArrayList<>(w.getEntitiesByType(EntityType.ITEM_DISPLAY,new Box(anchor).expand(1),e->e.getCommandTags().contains(tag)));
    }

    /** Makes the anchor's parts exactly one item_display per def part at the calibrated transform; records the prop as live. */
    static void ensureParts(ServerWorld w,BlockPos anchor,PropDef d,int facing){
        double[] t=PropGeometry.partTransform(d,facing);
        float yaw=(float)t[3];
        double x=anchor.getX()+t[0],y=anchor.getY()+t[1],z=anchor.getZ()+t[2];
        Set<Integer> have=new HashSet<>();
        Set<Integer> want=new HashSet<>();for(PropDef.Part p:d.parts())want.add(p.cmd());
        for(Entity e:parts(w,anchor)){
            int cmd=partCmd(e);
            boolean placed=Math.abs(MathHelper.wrapDegrees(e.getYaw()-yaw))<1&&e.squaredDistanceTo(x,y,z)<1e-4;
            if(cmd<0||!want.contains(cmd)||!placed||!have.add(cmd))e.discard();
        }
        for(PropDef.Part p:d.parts())if(!have.contains(p.cmd()))spawnPart(w,anchor,p,x,y,z,yaw);
        LIVE.computeIfAbsent(w,k->new LinkedHashMap<>()).put(anchor.toImmutable(),new Live(anchor.toImmutable(),d,facing));
    }

    static int partCmd(Entity e){
        for(String s:e.getCommandTags())if(s.startsWith(TAG+"#"))try{return Integer.parseInt(s.substring(TAG.length()+1));}catch(NumberFormatException x){return -1;}
        return -1;
    }
    static BlockPos tagAnchor(Entity e){
        for(String s:e.getCommandTags())if(s.startsWith(TAG+"@")){
            String[] a=s.substring(TAG.length()+1).split(",");
            if(a.length==3)try{return new BlockPos(Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]));}catch(NumberFormatException x){return null;}
        }
        return null;
    }

    static void spawnPart(ServerWorld w,BlockPos anchor,PropDef.Part p,double x,double y,double z,float yaw){
        NbtCompound n=new NbtCompound();
        n.putString("id","minecraft:item_display");
        NbtCompound item=new NbtCompound();
        item.putString("id","minecraft:paper");item.putByte("Count",(byte)1);
        NbtCompound tag=new NbtCompound();tag.putInt("CustomModelData",p.cmd());item.put("tag",tag);
        n.put("item",item);
        n.putString("item_display","fixed");
        n.putFloat("view_range",VIEW_RANGE);
        if(p.glow()){NbtCompound b=new NbtCompound();b.putInt("block",15);b.putInt("sky",15);n.put("brightness",b);}
        NbtList tags=new NbtList();
        tags.add(NbtString.of(TAG));tags.add(NbtString.of(anchorTag(anchor)));tags.add(NbtString.of(partTag(p.cmd())));
        n.put("Tags",tags);
        EntityType.getEntityFromNbt(n,w).ifPresent(e->{e.refreshPositionAndAngles(x,y,z,yaw,0);w.spawnEntity(e);});
    }

    // ---- scanner ----------------------------------------------------------------------------------------------------------------------

    /** Scans the loaded chunks of {@code box}: every prop anchor gets exactly its parts; prop parts whose anchor is gone, or that no
     *  longer belong to it, are discarded. Returns the number of props found. */
    public static int scanBox(ServerWorld w,BlockBox box){
        int found=0;
        for(int cx=box.getMinX()>>4;cx<=box.getMaxX()>>4;cx++)for(int cz=box.getMinZ()>>4;cz<=box.getMaxZ()>>4;cz++){
            WorldChunk chunk=w.getChunkManager().getWorldChunk(cx,cz);
            if(chunk==null)continue;
            for(BlockPos base:new ArrayList<>(chunk.getBlockEntities().keySet())){
                if(!box.contains(base.up())&&!box.contains(base))continue;
                Live l=readAnchor(w,base.up());
                if(l==null)continue;
                ensureParts(w,l.anchor(),l.def(),l.facing());found++;
            }
        }
        Box area=new Box(box.getMinX(),box.getMinY(),box.getMinZ(),box.getMaxX()+1,box.getMaxY()+1,box.getMaxZ()+1);
        for(Entity e:new ArrayList<>(w.getEntitiesByType(EntityType.ITEM_DISPLAY,area,e->e.getCommandTags().contains(TAG))))checkPart(w,e);
        return found;
    }

    /** Discards a prop part whose anchor is gone or that its anchor's def does not have. */
    static void checkPart(ServerWorld w,Entity e){
        if(e.isRemoved())return;
        BlockPos anchor=tagAnchor(e);
        Live l=anchor==null?null:readAnchor(w,anchor);
        if(l==null){e.discard();return;}
        ensureParts(w,l.anchor(),l.def(),l.facing());
    }

    static void tick(ServerWorld w){
        List<Entity> ents=PENDING_ENTITIES.remove(w);
        if(ents!=null)for(Entity e:ents)if(!e.isRemoved()&&e.getWorld()==w)checkPart(w,e);
        Map<Long,Integer> chunks=PENDING_CHUNKS.get(w);
        if(chunks==null||chunks.isEmpty())return;
        for(Iterator<Map.Entry<Long,Integer>> it=chunks.entrySet().iterator();it.hasNext();){
            Map.Entry<Long,Integer> c=it.next();
            long key=c.getKey();
            if(!w.isChunkLoaded(key)){ // entities not loaded yet
                if(c.getValue()>=CHUNK_WAIT)it.remove();else c.setValue(c.getValue()+1);
                continue;
            }
            it.remove();
            ChunkPos cp=new ChunkPos(key);
            if(w.getChunkManager().getWorldChunk(cp.x,cp.z)==null)continue;
            scanBox(w,new BlockBox(cp.getStartX(),w.getBottomY(),cp.getStartZ(),cp.getEndX(),w.getTopY()-1,cp.getEndZ()));
        }
    }

    static void chunkUnloaded(ServerWorld w,WorldChunk chunk){
        Map<Long,Integer> pending=PENDING_CHUNKS.get(w);if(pending!=null)pending.remove(chunk.getPos().toLong());
        Map<BlockPos,Live> m=LIVE.get(w);
        if(m!=null){ChunkPos cp=chunk.getPos();m.keySet().removeIf(a->(a.getX()>>4)==cp.x&&(a.getZ()>>4)==cp.z);}
    }

    /** The live map's values as they are, without the anchor check or a copy (the S5 ticker's cheap pass); do not modify. */
    static java.util.Collection<Live> liveView(ServerWorld w){
        Map<BlockPos,Live> m=LIVE.get(w);
        return m==null?List.of():m.values();
    }

    /** The props known to be in loaded chunks of {@code w}; entries whose anchor has gone are dropped. */
    public static List<Live> live(ServerWorld w){
        Map<BlockPos,Live> m=LIVE.get(w);
        if(m==null)return List.of();
        List<Live> out=new ArrayList<>(m.size());
        for(Iterator<Map.Entry<BlockPos,Live>> it=m.entrySet().iterator();it.hasNext();){
            Map.Entry<BlockPos,Live> e=it.next();
            Live now=readAnchor(w,e.getKey());
            if(now==null){it.remove();continue;}
            if(now.def()!=e.getValue().def()||now.facing()!=e.getValue().facing())e.setValue(now); // a data reload or a turned anchor
            out.add(now);
        }
        return out;
    }
}
