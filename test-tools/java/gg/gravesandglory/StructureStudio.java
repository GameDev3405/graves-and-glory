package gg.gravesandglory;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Unshipped piece studio: builds a piece from its command script (data/gravesandglory_studio/functions/GROUP/NAME.mcfunction) in the world at ORIGIN,
 *  saves it with the vanilla structure saver, writes it back as a script, and registers /studio. Pieces are authored as scripts, exported here, and
 *  shipped as plain structure files. Never included in releases. */
public final class StructureStudio implements ModInitializer {
    public static final String NS="gravesandglory_studio";
    /** The south-east-up stage: the minimum corner of every piece while it is built. */
    public static final BlockPos ORIGIN=new BlockPos(1000,64,1000);
    /** Air is set this many blocks beyond the piece, so a neighbour cannot change how a block connects. */
    static final int MARGIN=2;
    static final Pattern HEADER=Pattern.compile("# size ([0-9]+) ([0-9]+) ([0-9]+)");
    static final Pattern OFFSET=Pattern.compile("~(-?[0-9]+)?");

    /** A script that cannot be built, with the line it failed on. */
    public static final class StudioException extends RuntimeException { public StudioException(String message){super(message);} }
    record Line(int number,String text){}
    record Script(Vec3i size,List<Line> commands){}

    /** Reads the script text: line 1 is the size header, every other line is blank, a comment, or a fill or setblock with ~ offsets from the
     *  minimum corner that stay inside the size. Odd width and depth up to 48, height up to 32. */
    static Script parse(String label,String text){
        if(!text.isEmpty()&&text.charAt(0)==0xFEFF)text=text.substring(1);
        String[] raw=text.split("\r?\n",-1);
        Matcher header=HEADER.matcher(raw[0].trim());
        if(!header.matches())throw new StudioException(label+" line 1: expected '# size <sx> <sy> <sz>'");
        int sx=Integer.parseInt(header.group(1)),sy=Integer.parseInt(header.group(2)),sz=Integer.parseInt(header.group(3));
        if(sx%2==0||sz%2==0||sx>48||sz>48||sy<1||sy>32)throw new StudioException(label+" line 1: size "+sx+" "+sy+" "+sz+" must have odd width and depth up to 48 and a height from 1 to 32");
        Vec3i size=new Vec3i(sx,sy,sz);List<Line> commands=new ArrayList<>();
        for(int i=1;i<raw.length;i++){
            String line=raw[i].trim();if(line.isEmpty()||line.startsWith("#"))continue;
            String[] part=line.split("[ \t]+",8);boolean fill=part[0].equals("fill");
            if(!fill&&!part[0].equals("setblock"))throw new StudioException(label+" line "+(i+1)+": only fill and setblock are allowed, not '"+part[0]+"'");
            int coordinates=fill?6:3;
            if(part.length<coordinates+2)throw new StudioException(label+" line "+(i+1)+": "+part[0]+" needs "+coordinates+" ~ offsets and a block");
            for(int k=0;k<coordinates;k++){
                Matcher offset=OFFSET.matcher(part[k+1]);
                if(!offset.matches())throw new StudioException(label+" line "+(i+1)+": '"+part[k+1]+"' is not a ~ offset from the minimum corner");
                int v=offset.group(1)==null?0:Integer.parseInt(offset.group(1)),limit=k%3==0?sx:k%3==1?sy:sz;
                if(v<0||v>=limit)throw new StudioException(label+" line "+(i+1)+": offset ~"+v+" is outside the size "+sx+" "+sy+" "+sz);
            }
            commands.add(new Line(i+1,line));
        }
        return new Script(size,commands);
    }

    /** Every script, as GROUP/NAME, in order. */
    public static List<String> scripts(MinecraftServer s){
        var ids=new TreeSet<String>();
        for(Identifier id:s.getResourceManager().findResources("functions",i->i.getNamespace().equals(NS)&&i.getPath().endsWith(".mcfunction")).keySet()){
            String path=id.getPath();ids.add(path.substring("functions/".length(),path.length()-".mcfunction".length()));
        }
        return new ArrayList<>(ids);
    }
    static String text(MinecraftServer s,String id){
        var resource=s.getResourceManager().getResource(new Identifier(NS,"functions/"+id+".mcfunction"));
        if(resource.isEmpty())throw new StudioException("no script "+NS+":"+id);
        try(InputStream in=resource.get().getInputStream()){return new String(in.readAllBytes(),StandardCharsets.UTF_8);}
        catch(Exception e){throw new StudioException("script "+id+" unreadable: "+e);}
    }
    /** The size in the header of a script. */
    public static Vec3i size(MinecraftServer s,String id){return parse(id,text(s,id)).size();}

    // ---- the stage ----
    static final Set<Long> forced=new HashSet<>();
    static BlockBox dirty;
    /** Loads and holds every chunk the block rectangle touches, so pieces can be built and read far from the arena. */
    public static void force(ServerWorld w,int minX,int minZ,int maxX,int maxZ){
        for(int cx=minX>>4;cx<=maxX>>4;cx++)for(int cz=minZ>>4;cz<=maxZ>>4;cz++){
            if(forced.add(ChunkPos.toLong(cx,cz)))w.setChunkForced(cx,cz,true);
            w.getChunk(cx,cz);
        }
    }
    /** Lets go of every chunk force() held. */
    public static void release(ServerWorld w){
        for(long key:forced)w.setChunkForced(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key),false);
        forced.clear();dirty=null;
    }
    static void set(ServerWorld w,BlockBox box,BlockState state){
        var pos=new BlockPos.Mutable();
        for(int x=box.getMinX();x<=box.getMaxX();x++)for(int y=box.getMinY();y<=box.getMaxY();y++)for(int z=box.getMinZ();z<=box.getMaxZ();z++){
            pos.set(x,y,z);w.setBlockState(pos,state,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
        }
    }
    /** Sets what the studio built before to air a few ticks ahead of the next piece, so the light engine has caught up when it is built: a crop
     *  breaks when a neighbour is set beside it in the stale shadow of the last piece, and the rebuild then differs from the shipped file. */
    public static void clear(ServerWorld w){if(dirty!=null)set(w,dirty,Blocks.AIR.getDefaultState());}
    /** Clears everything the studio built before, sets the new piece box and a margin of air around it, then the box itself to structure void. */
    static void stage(ServerWorld w,Vec3i size){
        BlockBox box=new BlockBox(ORIGIN.getX(),ORIGIN.getY(),ORIGIN.getZ(),ORIGIN.getX()+size.getX()-1,ORIGIN.getY()+size.getY()-1,ORIGIN.getZ()+size.getZ()-1);
        BlockBox grown=box.expand(MARGIN),clear=dirty==null?grown:grown.encompass(dirty);
        force(w,clear.getMinX(),clear.getMinZ(),clear.getMaxX(),clear.getMaxZ());
        set(w,clear,Blocks.AIR.getDefaultState());
        set(w,box,Blocks.STRUCTURE_VOID.getDefaultState());
        dirty=grown;
    }

    // ---- build, save, export ----
    /** Builds a script text at ORIGIN: its box becomes structure void inside a margin of air, then each line runs as a level 4 command with ~ at ORIGIN.
     *  A line that fails fails the piece, and the message names the line. Returns the size from the header. */
    public static Vec3i run(MinecraftServer s,String label,String text){
        Script script=parse(label,text);ServerWorld w=s.getOverworld();
        stage(w,script.size());
        var source=s.getCommandSource().withPosition(Vec3d.of(ORIGIN)).withLevel(4).withSilent();
        var dispatcher=s.getCommandManager().getDispatcher();
        for(Line line:script.commands()){
            try{dispatcher.execute(line.text(),source);}
            catch(Exception e){throw new StudioException(label+" line "+line.number()+": "+e.getMessage()+" in '"+line.text()+"'");}
        }
        return script.size();
    }
    /** Builds the script of a piece at ORIGIN and returns its size. */
    public static Vec3i build(MinecraftServer s,String id){return run(s,id,text(s,id));}
    /** What stands at ORIGIN, saved with the vanilla structure saver: structure void is left out, so the cells no line set keep the ground. */
    public static NbtCompound save(MinecraftServer s,String id,Vec3i size){
        var template=new StructureTemplate();
        template.saveFromWorld(s.getOverworld(),ORIGIN,size,false,Blocks.STRUCTURE_VOID);
        return template.writeNbt(new NbtCompound());
    }
    public static Path out(){return Path.of(System.getProperty("gravesandglory.studioOut","studio-out"));}
    static Path write(NbtCompound nbt,String id){
        Path file=out().resolve(id+".nbt");
        try{Files.createDirectories(file.getParent());NbtIo.writeCompressed(nbt,file.toFile());}
        catch(Exception e){throw new StudioException("could not write "+file+": "+e);}
        return file;
    }
    /** Builds a piece from its script, saves it and writes STUDIO_OUT/GROUP/NAME.nbt (gzip). */
    public static Path export(MinecraftServer s,String id){return write(save(s,id,build(s,id)),id);}
    /** Empty when the script of a piece rebuilds to exactly the shipped structure file (NBT equals), else what differs. */
    public static String stale(MinecraftServer s,String id){
        var shipped=Structures.read(s,id);
        if(shipped.isEmpty())return "no structure file data/gravesandglory/structures/"+id+".nbt";
        NbtCompound fresh=save(s,id,build(s,id));
        return fresh.equals(shipped.get())?"":"the script no longer builds the shipped file: "+difference(fresh,shipped.get());
    }
    /** True when the script of a piece rebuilds to exactly the shipped structure file. */
    public static boolean fresh(MinecraftServer s,String id){return stale(s,id).isEmpty();}
    static String difference(NbtCompound fresh,NbtCompound shipped){
        var keys=new TreeSet<String>(fresh.getKeys());keys.addAll(shipped.getKeys());var why=new StringBuilder();
        for(String key:keys){
            NbtElement a=fresh.get(key),b=shipped.get(key);if(Objects.equals(a,b))continue;
            if(a instanceof NbtList la&&b instanceof NbtList lb){
                int i=0;while(i<la.size()&&i<lb.size()&&la.get(i).equals(lb.get(i)))i++;
                why.append(key).append(" has ").append(la.size()).append(" entries against ").append(lb.size()).append(", first difference at ").append(i).append(": ").append(i<la.size()?la.get(i):"none").append(" against ").append(i<lb.size()?lb.get(i):"none").append("; ");
            }else why.append(key).append(" is ").append(a).append(" against ").append(b).append("; ");
        }
        return why.length()>400?why.substring(0,400)+"...":why.toString();
    }

    // ---- dump: a template back to a script ----
    record Cell(int x,int y,int z,String state,String nbt){}
    /** NAME[prop=value,...] for a palette entry, properties in alphabetical order. */
    static String stateText(NbtCompound entry){
        String name=entry.getString("Name");
        if(!entry.contains("Properties",NbtElement.COMPOUND_TYPE))return name;
        NbtCompound properties=entry.getCompound("Properties");
        var parts=new ArrayList<String>();
        for(String key:new TreeSet<>(properties.getKeys()))parts.add(key+"="+properties.getString(key));
        return parts.isEmpty()?name:name+"["+String.join(",",parts)+"]";
    }
    /** The script that rebuilds a template: the size header, then one setblock per block (block entity data kept), with a run of equal blocks along z
     *  that follow each other in the template and carry no block entity data merged into one fill. Structure void is left out; air is written, because
     *  a piece may need to clear. Lines keep the order the template stores its blocks in (full cubes, then the other blocks, then the block entities,
     *  each by y, x, z), so what a block hangs on is written before it: a vine or a banner is not removed by the update of a neighbour written first. */
    public static String dump(StructureTemplate template){
        NbtCompound nbt=template.writeNbt(new NbtCompound());Vec3i size=template.getSize();
        NbtList palette=nbt.getList("palette",NbtElement.COMPOUND_TYPE);
        String[] states=new String[palette.size()];boolean[] skip=new boolean[palette.size()];
        for(int i=0;i<states.length;i++){NbtCompound entry=palette.getCompound(i);states[i]=stateText(entry);skip[i]=entry.getString("Name").equals("minecraft:structure_void");}
        var cells=new ArrayList<Cell>();
        NbtList blocks=nbt.getList("blocks",NbtElement.COMPOUND_TYPE);
        for(int i=0;i<blocks.size();i++){
            NbtCompound block=blocks.getCompound(i);NbtList at=block.getList("pos",NbtElement.INT_TYPE);int state=block.getInt("state");
            if(skip[state])continue;
            String data=null;
            if(block.contains("nbt",NbtElement.COMPOUND_TYPE)){
                NbtCompound copy=block.getCompound("nbt").copy();
                for(String key:new String[]{"id","x","y","z"})copy.remove(key);
                if(!copy.isEmpty())data=copy.toString();
            }
            cells.add(new Cell(at.getInt(0),at.getInt(1),at.getInt(2),states[state],data));
        }
        var out=new StringBuilder("# size "+size.getX()+" "+size.getY()+" "+size.getZ()+"\n");
        for(int i=0;i<cells.size();){
            Cell first=cells.get(i);int last=i;
            while(first.nbt()==null&&last+1<cells.size()){
                Cell now=cells.get(last),next=cells.get(last+1);
                if(next.x()!=now.x()||next.y()!=now.y()||next.z()!=now.z()+1||next.nbt()!=null||!next.state().equals(first.state()))break;
                last++;
            }
            if(last>i)out.append("fill ~").append(first.x()).append(" ~").append(first.y()).append(" ~").append(first.z()).append(" ~").append(first.x()).append(" ~").append(first.y()).append(" ~").append(cells.get(last).z()).append(' ').append(first.state()).append('\n');
            else out.append("setblock ~").append(first.x()).append(" ~").append(first.y()).append(" ~").append(first.z()).append(' ').append(first.state()).append(first.nbt()==null?"":first.nbt()).append('\n');
            i=last+1;
        }
        return out.toString();
    }

    // ---- /studio ----
    static final SuggestionProvider<ServerCommandSource> PIECES=(context,builder)->CommandSource.suggestMatching(scripts(context.getSource().getServer()),builder);
    static String id(CommandContext<ServerCommandSource> context){return IdentifierArgumentType.getIdentifier(context,"id").getPath();}
    interface Action { String run(ServerCommandSource source,String id) throws Exception; }
    /** Runs a /studio action and lets go of the chunks it forced: forcing is saved with the world, so a command must not leave one behind that no suite holds. */
    static int act(CommandContext<ServerCommandSource> context,Action action){
        ServerWorld world=context.getSource().getServer().getOverworld();Set<Long> held=new HashSet<>(forced);
        try{String said=action.run(context.getSource(),id(context));context.getSource().sendFeedback(()->Text.literal(said),false);return 1;}
        catch(Exception e){context.getSource().sendError(Text.literal(String.valueOf(e.getMessage()==null?e:e.getMessage())));return 0;}
        finally{letGo(world,held);}
    }
    /** Lets go of every chunk forced since held was taken, and of no other. */
    static void letGo(ServerWorld w,Set<Long> held){
        for(var it=forced.iterator();it.hasNext();){
            long key=it.next();if(held.contains(key))continue;
            w.setChunkForced(ChunkPos.getPackedX(key),ChunkPos.getPackedZ(key),false);it.remove();
        }
    }

    @Override public void onInitialize(){
        CommandRegistrationCallback.EVENT.register((dispatcher,registryAccess,environment)->dispatcher.register(
            CommandManager.literal("studio").requires(source->source.hasPermissionLevel(2))
                .then(CommandManager.literal("build").then(CommandManager.argument("id",IdentifierArgumentType.identifier()).suggests(PIECES).executes(context->act(context,(source,id)->{
                    Vec3i size=build(source.getServer(),id);return "built "+id+" "+size.toShortString()+" at "+ORIGIN.toShortString();}))))
                .then(CommandManager.literal("save").then(CommandManager.argument("id",IdentifierArgumentType.identifier()).suggests(PIECES).executes(context->act(context,(source,id)->{
                    Path file=write(save(source.getServer(),id,size(source.getServer(),id)),id);return "saved what stands at "+ORIGIN.toShortString()+" to "+file;}))))
                .then(CommandManager.literal("dump").then(CommandManager.argument("id",IdentifierArgumentType.identifier()).suggests(PIECES).executes(context->act(context,(source,id)->{
                    // The vanilla manager reads <world>/generated first, which is where a structure block saves, and the mod's own copy after:
                    // an edit of Kort's is what gets dumped. (Structures.template reads the mod only, by design, so it would dump the stale piece.)
                    var server=source.getServer();Path generated=server.getSavePath(WorldSavePath.GENERATED).resolve(Structures.NS+"/structures/"+id+".nbt");
                    var template=server.getStructureTemplateManager().getTemplate(new Identifier(Structures.NS,id));
                    if(template.isEmpty())throw new StudioException("no structure file for "+id+": not in "+generated+" and not in the mod resources");
                    String script=dump(template.get());Path file=out().resolve(id+".mcfunction");
                    Files.createDirectories(file.getParent());Files.writeString(file,script,StandardCharsets.UTF_8);
                    String from=Files.isRegularFile(generated)?"the generated file was used: "+generated:"no generated file, so the shipped piece was used";
                    return "dumped "+id+" ("+from+") as a script of "+script.split("\n").length+" lines to "+file;}))))
                .then(CommandManager.literal("place").then(CommandManager.argument("id",IdentifierArgumentType.identifier()).suggests(PIECES).then(CommandManager.argument("rotation",IntegerArgumentType.integer(0,270)).executes(context->act(context,(source,id)->{
                    int rotation=IntegerArgumentType.getInteger(context,"rotation");BlockPos at=BlockPos.ofFloored(source.getPlayerOrThrow().getPos());
                    var placed=Structures.place(source.getWorld(),id,at.getX(),at.getY()-1,at.getZ(),rotation);
                    return placed.missing()?"no structure file for "+id:"placed "+id+" turned "+rotation+" with its centre under your feet: "+placed.blocks()+" blocks in "+String.format("%.1f",placed.nanos()/1e6)+" ms, box "+placed.box();})))))
        ));
    }
}
