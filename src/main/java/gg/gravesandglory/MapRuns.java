package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Column-run files (gg-mapforge-runs/1), the one format of map adoption. tools/mapforge/runs.py holds the format
 * description and its Python twin; both must agree on the text and the digests. A file lists cells as runs along y,
 * one line per column; kinds are capture, overlay, batch and undo. The world-side helpers here (Cursor, Apply,
 * WorldDigest) step a budget of cells per call so a large file never stalls a tick. */
final class MapRuns {
    static final String FORMAT="gg-mapforge-runs/1",AIR="minecraft:air";
    static final int FLAGS=Block.NOTIFY_LISTENERS|Block.FORCE_STATE|Block.SKIP_DROPS;
    static final Comparator<BlockPos> ORDER=Comparator.comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);
    final TreeMap<String,JsonElement> header=new TreeMap<>();
    final List<String> palette=new ArrayList<>();
    private final HashMap<String,Integer> index=new HashMap<>();
    /** Each column: x, z, then y n p triples (ascending, maximal); columns ascend by (x, z). */
    private final List<int[]> columns=new ArrayList<>();
    final TreeMap<BlockPos,String> be=new TreeMap<>(ORDER);
    private int[] open=new int[32];private int openLen=0;

    MapRuns(String kind){header.put("format",new JsonPrimitive(FORMAT));header.put("kind",new JsonPrimitive(kind));}
    String kind(){return h("kind");}
    String h(String key){var e=header.get(key);return e==null||!e.isJsonPrimitive()?null:e.getAsString();}
    MapRuns put(String key,String value){header.put(key,new JsonPrimitive(value));return this;}

    // ---- building ----
    int index(String state){return index.computeIfAbsent(state,s->{palette.add(s);return palette.size()-1;});}
    /** Appends a run; columns must come in ascending (x, z) order and runs in ascending y, as the file stores them. */
    void add(int x,int z,int y,int n,String state){
        if(n<=0)return;
        int p=index(state);
        if(openLen>0&&open[0]==x&&open[1]==z){
            int ly=open[openLen-3],ln=open[openLen-2],lp=open[openLen-1];
            if(y<ly+ln)throw new IllegalStateException("run at "+x+","+y+","+z+" overlaps the run below it");
            if(y==ly+ln&&lp==p){open[openLen-2]+=n;return;}
        }else{
            if(openLen>0&&(x<open[0]||x==open[0]&&z<open[1]))throw new IllegalStateException("column "+x+","+z+" out of order");
            if(openLen==0&&!columns.isEmpty()){var l=columns.get(columns.size()-1);if(x<l[0]||x==l[0]&&z<=l[1])throw new IllegalStateException("column "+x+","+z+" out of order");}
            seal();open[0]=x;open[1]=z;openLen=2;
        }
        if(openLen+3>open.length)open=Arrays.copyOf(open,open.length*2);
        open[openLen++]=y;open[openLen++]=n;open[openLen++]=p;
    }
    private void seal(){if(openLen>0){columns.add(Arrays.copyOf(open,openLen));openLen=0;}}
    List<int[]> columns(){seal();return columns;}
    /** One box per chunk the columns touch, from the lowest to the highest cell listed in that chunk. */
    List<net.minecraft.util.math.BlockBox> chunkBoxes(){
        Map<Long,int[]> ys=new LinkedHashMap<>();
        for(int[] col:columns()){
            int lo=col[2],hi=col[col.length-3]+col[col.length-2]-1;
            ys.merge(((long)(col[0]>>4)<<32)|((col[1]>>4)&0xffffffffL),new int[]{lo,hi},(a,b)->new int[]{Math.min(a[0],b[0]),Math.max(a[1],b[1])});
        }
        List<net.minecraft.util.math.BlockBox> out=new ArrayList<>(ys.size());
        for(var e:ys.entrySet()){
            int cx=(int)(e.getKey()>>32),cz=(int)(long)e.getKey();
            out.add(new net.minecraft.util.math.BlockBox(cx<<4,e.getValue()[0],cz<<4,(cx<<4)+15,e.getValue()[1],(cz<<4)+15));
        }
        return out;
    }
    long cells(){long c=0;for(int[] col:columns())for(int k=2;k<col.length;k+=3)c+=col[k+1];return c;}

    // ---- digests ----
    static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(Exception e){throw new IllegalStateException(e);}}
    static String hex(byte[] b){return HexFormat.of().formatHex(b);}
    /** Soil that random ticks turn into each other (grass spreads onto dirt and dies under a block, farmland dries,
     * mycelium spreads) counts as dirt in every digest that compares a file with a live world (baseDigest,
     * WorldDigest, the plan and undo checks), so a few ticks between two builds do not refuse an overlay; the overlay
     * still writes the exact state. */
    static final java.util.Set<String> SETTLES=java.util.Set.of("minecraft:grass_block","minecraft:dirt","minecraft:mycelium","minecraft:farmland");
    /** What the game changes on its own, ignored by world-comparing digests: soil random ticks turn into each other
     * counts as dirt, and flowing (non-source) water or lava counts as air (tools/mapforge/runs.py settle). */
    static String settle(String s){int i=s.indexOf('[');String b=i<0?s:s.substring(0,i);
        if(SETTLES.contains(b))return "minecraft:dirt";
        if((b.equals("minecraft:water")||b.equals("minecraft:lava"))&&s.contains("level=")&&!s.contains("level=0"))return "minecraft:air";
        return s;}
    static void run(MessageDigest md,int x,int z,int y,int n,String s){md.update((x+","+z+","+y+","+n+"|"+s+"\n").getBytes(StandardCharsets.UTF_8));}
    String digest(){var md=sha();for(int[] col:columns())for(int k=2;k<col.length;k+=3)run(md,col[0],col[1],col[k],col[k+1],palette.get(col[k+2]));return hex(md.digest());}

    // ---- text ----
    /** The body lines, palette renumbered in order of first use. */
    void lines(Consumer<String> out){
        var order=new int[palette.size()];Arrays.fill(order,-1);var used=new ArrayList<Integer>();
        for(int[] col:columns())for(int k=2;k<col.length;k+=3)if(order[col[k+2]]<0){order[col[k+2]]=used.size();used.add(col[k+2]);}
        for(int i=0;i<used.size();i++)out.accept("P "+i+" "+palette.get(used.get(i)));
        var sb=new StringBuilder();
        for(int[] col:columns()){
            sb.setLength(0);sb.append("C ").append(col[0]).append(' ').append(col[1]);
            for(int k=2;k<col.length;k+=3)sb.append(' ').append(col[k]).append(' ').append(col[k+1]).append(' ').append(order[col[k+2]]);
            out.accept(sb.toString());
        }
        for(var e:be.entrySet())out.accept("E "+e.getKey().getX()+" "+e.getKey().getY()+" "+e.getKey().getZ()+" "+e.getValue());
    }
    /** Writes the file (gzip, through a .part file) and returns its size in bytes. */
    long write(Path path)throws IOException{
        var md=sha();lines(l->md.update((l+"\n").getBytes(StandardCharsets.UTF_8)));
        header.putIfAbsent("dataVersion",new JsonPrimitive(net.minecraft.SharedConstants.getGameVersion().getSaveVersion().getId())); // a file read from disk keeps its own version (its E strings stay as stored)
        header.put("cells",new JsonPrimitive(cells()));header.put("digest",new JsonPrimitive(digest()));header.put("body",new JsonPrimitive(hex(md.digest())));
        var json=new JsonObject();header.forEach(json::add);
        Files.createDirectories(path.toAbsolutePath().getParent());
        var tmp=path.resolveSibling(path.getFileName()+".part");
        try(var w=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(tmp),1<<16),StandardCharsets.UTF_8),1<<16)){
            w.write(new Gson().toJson(json));w.write('\n');
            IOException[] err={null};
            lines(l->{if(err[0]!=null)return;try{w.write(l);w.write('\n');}catch(IOException e){err[0]=e;}});
            if(err[0]!=null)throw err[0];
        }
        Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
        return Files.size(path);
    }
    static MapRuns read(Path path)throws IOException{try(var in=Files.newInputStream(path)){return read(in);}}
    /** Reads and checks a file: format, body hash, sorted maximal runs, palette, cell count and digest. Throws on any fault. */
    static MapRuns read(InputStream raw)throws IOException{
        var r=new BufferedReader(new InputStreamReader(new GZIPInputStream(raw,1<<16),StandardCharsets.UTF_8),1<<16);
        String first=r.readLine();if(first==null)throw new IOException("empty runs file");
        JsonObject head;try{head=JsonParser.parseString(first).getAsJsonObject();}catch(Exception e){throw new IOException("header is not JSON");}
        if(!head.has("format")||!FORMAT.equals(head.get("format").getAsString()))throw new IOException("not a "+FORMAT+" file");
        var out=new MapRuns(head.get("kind").getAsString());head.entrySet().forEach(e->out.header.put(e.getKey(),e.getValue()));
        var md=sha();var filePalette=new HashMap<Integer,Integer>();
        for(String line;(line=r.readLine())!=null;){
            md.update((line+"\n").getBytes(StandardCharsets.UTF_8));
            if(line.isEmpty())continue;
            String[] v;
            switch(line.charAt(0)){
                case 'P'->{v=line.split(" ",3);if(v.length<3)throw new IOException("bad palette line");filePalette.put(Integer.parseInt(v[1]),out.index(v[2]));}
                case 'C'->{
                    v=line.split(" ");int x=Integer.parseInt(v[1]),z=Integer.parseInt(v[2]);
                    if((v.length-3)%3!=0||v.length<6)throw new IOException("column "+x+","+z+": bad run list");
                    int lastEnd=Integer.MIN_VALUE,lastP=-1;
                    for(int k=3;k<v.length;k+=3){
                        int y=Integer.parseInt(v[k]),n=Integer.parseInt(v[k+1]);Integer p=filePalette.get(Integer.parseInt(v[k+2]));
                        if(p==null)throw new IOException("column "+x+","+z+": palette index undefined");
                        if(n<=0||y<-64||y+n-1>319)throw new IOException("column "+x+","+z+": run outside y -64..319");
                        if(y<lastEnd||y==lastEnd&&p==lastP)throw new IOException("column "+x+","+z+": runs overlap or are not maximal at y "+y);
                        try{out.add(x,z,y,n,out.palette.get(p));}catch(IllegalStateException e){throw new IOException(e.getMessage());}
                        lastEnd=y+n;lastP=p;
                    }
                }
                case 'E'->{v=line.split(" ",5);out.be.put(new BlockPos(Integer.parseInt(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3])),v[4]);}
                default->throw new IOException("unknown line "+line.substring(0,Math.min(20,line.length())));
            }
        }
        if(!hex(md.digest()).equals(out.h("body")))throw new IOException("body hash differs from the header: the file was changed or truncated");
        if(!head.has("cells")||out.cells()!=head.get("cells").getAsLong())throw new IOException("cell count differs from the header");
        if(!out.digest().equals(out.h("digest")))throw new IOException("digest differs from the header");
        return out;
    }

    // ---- block states and block entities ----
    private static final Map<BlockState,String> NAMES=new HashMap<>();
    /** Registry id plus every property sorted by name: the canonical state text. */
    static String name(BlockState s){
        return NAMES.computeIfAbsent(s,st->{
            var sb=new StringBuilder(Registries.BLOCK.getId(st.getBlock()).toString());
            var props=new ArrayList<Property<?>>(st.getProperties());props.sort(Comparator.comparing(Property::getName));
            for(int i=0;i<props.size();i++)sb.append(i==0?'[':',').append(props.get(i).getName()).append('=').append(value(st,props.get(i)));
            if(!props.isEmpty())sb.append(']');
            return sb.toString();
        });
    }
    private static <T extends Comparable<T>> String value(BlockState s,Property<T> p){return p.name(s.get(p));}
    /** The palette as block states; throws on a state this game does not know. */
    BlockState[] states(){
        var out=new BlockState[palette.size()];
        for(int i=0;i<out.length;i++){
            try{out[i]=BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(),palette.get(i),false).blockState();}
            catch(Exception e){throw new IllegalArgumentException("unknown block state "+palette.get(i)+": "+e.getMessage());}
        }
        return out;
    }
    static String encode(NbtCompound n){
        var b=new ByteArrayOutputStream();
        try(var o=new DataOutputStream(b)){NbtIo.write(n,o);}catch(IOException e){throw new UncheckedIOException(e);}
        return Base64.getEncoder().encodeToString(b.toByteArray());
    }
    /** The block-entity NBT brought up from this file's stored data version (files from before the port carry none: 1.20.1 = 3465). */
    private NbtCompound upgraded(NbtCompound nbt){
        var v=header.get("dataVersion");int from=v!=null&&v.isJsonPrimitive()?v.getAsInt():3465;
        int now=net.minecraft.SharedConstants.getGameVersion().getSaveVersion().getId();
        if(from>=now)return nbt;
        var fixed=net.minecraft.datafixer.Schemas.getFixer().update(net.minecraft.datafixer.TypeReferences.BLOCK_ENTITY,new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE,(net.minecraft.nbt.NbtElement)nbt),from,now);
        return fixed.getValue() instanceof NbtCompound c?c:nbt;
    }
    static NbtCompound decode(String s){
        try(var i=new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(s)))){return NbtIo.readCompound(i);}catch(IOException e){throw new UncheckedIOException(e);}
    }

    // ---- walking the cells ----
    /** Visits every listed cell in file order; first and last mark the ends of each run. */
    final class Cursor {
        int c,k=2,o,x,y,z,p;boolean first,last;
        boolean next(){
            var cols=columns();
            while(c<cols.size()){
                int[] col=cols.get(c);
                if(k<col.length){
                    int n=col[k+1];x=col[0];z=col[1];y=col[k]+o;p=col[k+2];first=o==0;last=o==n-1;
                    if(++o>=n){o=0;k+=3;}
                    return true;
                }
                c++;k=2;o=0;
            }
            return false;
        }
    }
    /** The digest of a world's cells over this file's ranges, each range split into the world's maximal runs (the
     * baseDigest of an overlay, the afterDigest of an undo). */
    final class WorldDigest {
        final Cursor cur=new Cursor();final MessageDigest md=sha();String s;int rx,rz,ry,rn;String result;
        boolean step(ServerWorld w,int budget){
            if(result!=null)return true;
            var pos=new BlockPos.Mutable();
            while(budget-->0){
                if(!cur.next()){result=hex(md.digest());return true;}
                String now=settle(name(w.getBlockState(pos.set(cur.x,cur.y,cur.z))));
                if(cur.first||!now.equals(s)){flush();rx=cur.x;rz=cur.z;ry=cur.y;rn=1;s=now;}else rn++;
                if(cur.last)flush();
            }
            return false;
        }
        private void flush(){if(s!=null)MapRuns.run(md,rx,rz,ry,rn,s);s=null;}
        String all(ServerWorld w){while(!step(w,1<<20));return result;}
    }
    /** Writes this file's cells into a world, skipping cells that already hold the state; records what it changed
     * into undo when given (state before, and block entity before). Block entities go last. */
    final class Apply {
        final Cursor cur=new Cursor();final BlockState[] states=states();final MapRuns undo;long visited,changed;boolean done;
        Apply(MapRuns undo){this.undo=undo;}
        boolean step(ServerWorld w,int budget){
            if(done)return true;
            var pos=new BlockPos.Mutable();
            while(budget-->0){
                if(!cur.next()){blockEntities(w);done=true;return true;}
                visited++;pos.set(cur.x,cur.y,cur.z);
                BlockState now=w.getBlockState(pos),want=states[cur.p];
                boolean beOnly=now==want&&be.containsKey(pos);
                if(now==want&&!beOnly)continue;
                if(undo!=null){
                    undo.add(cur.x,cur.z,cur.y,1,name(now));
                    var old=w.getBlockEntity(pos);if(old!=null)undo.be.put(pos.toImmutable(),encode(old.createNbt(w.getRegistryManager())));
                }
                if(!beOnly){w.setBlockState(pos,want,FLAGS);changed++;}
            }
            return false;
        }
        private void blockEntities(ServerWorld w){
            for(var e:be.entrySet()){
                var at=w.getBlockEntity(e.getKey());if(at==null)continue;
                var n=decode(e.getValue());if(!n.contains("id"))n.putString("id",net.minecraft.block.entity.BlockEntityType.getId(at.getType()).toString()); // createNbt writes no id; the datafixer's BLOCK_ENTITY type needs one (never stored back, so no hash sees it)
                at.read(upgraded(n),w.getRegistryManager());at.markDirty();
                var st=w.getBlockState(e.getKey());w.updateListeners(e.getKey(),st,st,Block.NOTIFY_ALL);
            }
        }
    }
}
