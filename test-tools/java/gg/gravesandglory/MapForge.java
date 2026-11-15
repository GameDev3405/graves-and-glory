package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** UNSHIPPED Map Forge command surface for the builder (plan task 2A.2): /mapforge plan, apply, undo, export, diff,
 * verify, backup, adopt, snapshot, walk and status. It registers on the builder server (BuildMode, -Dgg.build, with
 * the mapforge-builder jar) or with -Dgravesandglory.mapforge=true (the MapForgeHarness client). Every file it reads
 * or writes lives in (run folder)/mapforge/; the format is tools/mapforge/runs.py's gg-mapforge-runs/1.
 *
 * Build authority (Kort, 2026-10-28): an agent's batch is dry-run first (plan) and applies only inside its allow
 * regions, never on a deny lock, a capture ring, a faction spawn or a camp station, never deleting a region; apply
 * writes a backup zip and an undo file before and as it changes the world. Long jobs step a budget of cells a tick. */
public final class MapForge implements ModInitializer {
    static final int BUDGET=1<<18;
    static final DateTimeFormatter STAMP=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    interface Job{boolean step(ServerWorld w);}
    static MinecraftServer server;static Job job;static String jobName,last="idle";static boolean lastOk=true;static long jobStart;
    /** The batch the last plan passed: file name, its sha256, and the world's digest over its cells then. */
    static String plannedName,plannedSha,plannedBefore;

    static boolean enabled(){return BuildMode.active()||Boolean.getBoolean("gravesandglory.mapforge");}
    @Override public void onInitialize(){
        if(!enabled())return;
        ServerLifecycleEvents.SERVER_STARTED.register(s->server=s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{server=null;job=null;});
        ServerTickEvents.END_SERVER_TICK.register(s->{
            if(job==null)return;
            try{if(job.step(s.getOverworld()))job=null;}catch(Exception e){e.printStackTrace();finish(false,jobName+" failed: "+e);job=null;}
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->dispatcher.register(CommandManager.literal("mapforge").requires(s->s.hasPermissionLevel(2))
            .executes(c->say(c.getSource(),"mapforge plan|apply|undo|export|diff|verify|backup|adopt|snapshot|walk|status; files in mapforge/"))
            .then(CommandManager.argument("args",StringArgumentType.greedyString()).executes(c->run(c.getSource(),StringArgumentType.getString(c,"args"))))));
    }
    static int say(ServerCommandSource s,String text){last=text;s.sendFeedback(()->Text.literal("[mapforge] "+text),false);log(text);return 1;}
    static int no(ServerCommandSource s,String text){s.sendError(Text.literal("[mapforge] "+text));log("REFUSED "+text);lastOk=false;last=text;return 0;}
    static void log(String text){System.out.println("[Graves and Glory mapforge] "+text);}
    static void finish(boolean ok,String text){lastOk=ok;last=text+" ("+(System.nanoTime()-jobStart)/1_000_000+" ms)";log((ok?"":"FAILED ")+last);}
    static void start(String name,Job j){jobName=name;jobStart=System.nanoTime();lastOk=true;last="running "+name;job=j;}

    static Path dir(){return server.getRunDirectory().resolve("mapforge");}
    static Path file(String name){
        if(name.contains("..")||name.contains(":")||name.startsWith("/")||name.startsWith("\\"))throw new IllegalArgumentException("file names stay inside mapforge/: "+name);
        return dir().resolve(name);
    }
    static int map(){return CampaignMaps.selected;}
    static String scopeId(boolean camp){return camp?"camp":Spec.MAPS[map()].id();}
    static String stamp(boolean camp){return camp?"camp:"+CampWorld.hash():MapAdoption.stamp(map());}
    static String sha(Path p)throws IOException{var md=MapRuns.sha();md.update(Files.readAllBytes(p));return MapRuns.hex(md.digest());}

    static int run(ServerCommandSource s,String line){
        String[] a=line.trim().split("\\s+");
        if(server==null)server=s.getServer();
        if(job!=null&&!a[0].equals("status"))return no(s,"busy with "+jobName+"; /mapforge status");
        if(!a[0].equals("status")){lastOk=true;last=a[0];}
        try{
            return switch(a[0]){
                case "status"->say(s,(job!=null?"running "+jobName:"idle")+"; last: "+(lastOk?"":"FAILED ")+last+"; adoption: "+MapAdoption.status);
                case "backup"->{String z=backup("command");yield say(s,"backup "+z);}
                case "export"->export(s,a);
                case "diff"->diff(s,a);
                case "verify"->verify(s,a);
                case "plan"->plan(s,a);
                case "apply"->apply(s,a);
                case "undo"->undo(s,a);
                case "adopt"->adopt(s,a);
                case "snapshot"->snapshot(s,a);
                case "protects"->protects(s,a);
                case "walk"->{var r=MapForgeRoutes.check(s.getServer().getOverworld(),a.length>1?a[1]:"now");lastOk=r.ok();last="walk "+r.summary();yield r.ok()?say(s,last):no(s,last);}
                default->no(s,"unknown verb "+a[0]);
            };
        }catch(Exception e){e.printStackTrace();return no(s,a[0]+": "+e.getMessage());}
    }

    /** Saves the world and zips its save folder to mapforge/backups/(time)-(why).zip. */
    static String backup(String why)throws IOException{
        long t=System.nanoTime();
        server.saveAll(true,true,true);
        Path d=dir().resolve("backups");Files.createDirectories(d);
        Path target=d.resolve(LocalDateTime.now().format(STAMP)+"-"+why+".zip"),temp=d.resolve(target.getFileName()+".part");
        BuildMode.zip(server.getSavePath(WorldSavePath.ROOT),temp);Files.move(temp,target);
        String r=target.getFileName()+" "+Files.size(target)/1024+" KB in "+(System.nanoTime()-t)/1_000_000+" ms";
        log("backup "+r);return r;
    }

    /** export NAME x0 y0 z0 x1 y1 z1 [camp]: a capture of every cell of the box, air included. */
    static int export(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<8)return no(s,"export NAME x0 y0 z0 x1 y1 z1 [camp]");
        Path out=file(a[1]);int[] v=new int[6];for(int i=0;i<6;i++)v[i]=Integer.parseInt(a[2+i]);
        int x0=Math.min(v[0],v[3]),x1=Math.max(v[0],v[3]),y0=Math.min(v[1],v[4]),y1=Math.max(v[1],v[4]),z0=Math.min(v[2],v[5]),z1=Math.max(v[2],v[5]);
        boolean camp=a.length>8&&a[8].equals("camp");
        var r=new MapRuns("capture").put("map",scopeId(camp)).put("stamp",stamp(camp)).put("source","MapForge export");
        var box=new JsonObject();box.add("min",ints(x0,y0,z0));box.add("max",ints(x1,y1,z1));r.header.put("box",box);
        start("export "+a[1],new Job(){
            int x=x0,z=z0;final BlockPos.Mutable p=new BlockPos.Mutable();
            public boolean step(ServerWorld w){
                for(int cells=0;cells<BUDGET;){
                    if(x>x1){
                        try{long size=r.write(out);finish(true,"export "+a[1]+": "+r.cells()+" cells, "+r.palette.size()+" states, "+r.be.size()+" block entities, "+size+" bytes, digest "+r.h("digest").substring(0,12)+", sha "+sha(out).substring(0,12));}
                        catch(IOException e){finish(false,"export "+a[1]+": "+e);}
                        return true;
                    }
                    for(int y=y0;y<=y1;y++){
                        BlockState st=w.getBlockState(p.set(x,y,z));r.add(x,z,y,1,MapRuns.name(st));
                        if(st.hasBlockEntity()){var e=w.getBlockEntity(p);if(e!=null)r.be.put(p.toImmutable(),MapRuns.encode(e.createNbt(w.getRegistryManager())));}
                    }
                    cells+=y1-y0+1;
                    if(++z>z1){z=z0;x++;}
                }
                return false;
            }
        });
        return say(s,"exporting "+(long)(x1-x0+1)*(y1-y0+1)*(z1-z0+1)+" cells to mapforge/"+a[1]);
    }
    static JsonArray ints(int... v){var j=new JsonArray();for(int i:v)j.add(i);return j;}

    /** diff BASE FINAL OUT: overlay of FINAL over BASE (two captures of one box and one generator stamp). */
    static int diff(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<4)return no(s,"diff BASE FINAL OUT");
        long t=System.nanoTime();
        var base=MapRuns.read(file(a[1]));var fin=MapRuns.read(file(a[2]));
        if(!"capture".equals(base.kind())||!"capture".equals(fin.kind()))return no(s,"diff needs two captures");
        if(!Objects.equals(base.h("stamp"),fin.h("stamp")))return no(s,"the generator stamp changed between the base and the final capture; re-export the base");
        if(!Objects.equals(base.h("map"),fin.h("map")))return no(s,"the captures are of different maps");
        var out=diff(base,fin);
        out.put("map",fin.h("map")).put("stamp",fin.h("stamp")).put("source","MapForge diff "+a[1]+" "+a[2]).put("finalDigest",fin.digest());
        long size=out.write(file(a[3]));
        return say(s,"overlay "+a[3]+": "+out.cells()+" cells, "+out.be.size()+" block entities, "+size+" bytes in "+(System.nanoTime()-t)/1_000_000+" ms");
    }
    static int[] expand(int[] col,int lo,int hi){
        var v=new int[hi-lo];Arrays.fill(v,-1);
        for(int k=2;k<col.length;k+=3)for(int y=Math.max(col[k],lo);y<Math.min(col[k]+col[k+1],hi);y++)v[y-lo]=col[k+2];
        return v;
    }
    static MapRuns diff(MapRuns base,MapRuns fin){
        var out=new MapRuns("overlay");var baseCols=new HashMap<Long,int[]>();
        for(int[] c:base.columns())baseCols.put(key(c[0],c[1]),c);
        var md=MapRuns.sha();
        for(int[] c:fin.columns()){
            int[] b=baseCols.get(key(c[0],c[1]));if(b==null)continue;
            int lo=Math.max(b[2],c[2]),hi=Math.min(b[b.length-3]+b[b.length-2],c[c.length-3]+c[c.length-2]);if(lo>=hi)continue;
            int[] bv=expand(b,lo,hi),fv=expand(c,lo,hi);
            // Changed cells, as overlay runs; each range's base cells feed the base digest as maximal runs.
            int y=0;
            while(y<hi-lo){
                if(same(base,bv[y],fin,fv[y])&&Objects.equals(base.be.get(new BlockPos(c[0],lo+y,c[1])),fin.be.get(new BlockPos(c[0],lo+y,c[1])))){y++;continue;}
                int start=y;String st=fin.palette.get(fv[y]);
                while(y<hi-lo&&fin.palette.get(fv[y]).equals(st)&&!(same(base,bv[y],fin,fv[y])&&Objects.equals(base.be.get(new BlockPos(c[0],lo+y,c[1])),fin.be.get(new BlockPos(c[0],lo+y,c[1])))))y++;
                out.add(c[0],c[1],lo+start,y-start,st);
                for(int yy=start;yy<y;yy++){var p=new BlockPos(c[0],lo+yy,c[1]);var e=fin.be.get(p);if(e!=null)out.be.put(p,e);}
            }
        }
        // baseDigest: the base's maximal runs inside each overlay run, settled (MapRuns.settle).
        var baseD=MapRuns.sha();
        for(int[] c:out.columns()){
            int[] b=baseCols.get(key(c[0],c[1]));
            for(int k=2;k<c.length;k+=3){
                int lo=c[k],hi=c[k]+c[k+1];int[] bv=expand(b,lo,hi);
                int y=0;while(y<bv.length){int s0=y;String st=MapRuns.settle(bv[y]<0?MapRuns.AIR:base.palette.get(bv[y]));while(y<bv.length&&MapRuns.settle(bv[y]<0?MapRuns.AIR:base.palette.get(bv[y])).equals(st))y++;MapRuns.run(baseD,c[0],c[1],lo+s0,y-s0,st);}
            }
        }
        out.put("baseDigest",MapRuns.hex(baseD.digest()));
        return out;
    }
    static boolean same(MapRuns a,int pa,MapRuns b,int pb){return (pa<0?MapRuns.AIR:a.palette.get(pa)).equals(pb<0?MapRuns.AIR:b.palette.get(pb));}
    static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}

    /** verify FILE: every cell the file lists holds its state in the world. */
    static int verify(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<2)return no(s,"verify FILE");
        var r=MapRuns.read(file(a[1]));var states=r.states();var cur=r.new Cursor();
        start("verify "+a[1],new Job(){
            long n,bad,soil;final List<String> first=new ArrayList<>();final BlockPos.Mutable p=new BlockPos.Mutable();
            public boolean step(ServerWorld w){
                for(int i=0;i<BUDGET;i++){
                    if(!cur.next()){finish(bad==0,"verify "+a[1]+": "+n+" cells, "+bad+" differ, "+soil+" settled by the game (soil ticks, flowing fluid; MapRuns.settle)"+(first.isEmpty()?"":" e.g. "+String.join("; ",first)));return true;}
                    n++;var st=w.getBlockState(p.set(cur.x,cur.y,cur.z));
                    if(st!=states[cur.p]&&MapRuns.settle(MapRuns.name(st)).equals(MapRuns.settle(r.palette.get(cur.p)))){soil++;continue;}
                    if(st!=states[cur.p]){bad++;if(first.size()<8)first.add(cur.x+","+cur.y+","+cur.z+" is "+MapRuns.name(st)+" not "+r.palette.get(cur.p));}
                }
                return false;
            }
        });
        return say(s,"verifying "+r.cells()+" cells of "+a[1]);
    }

    // ---- build authority ----
    record Lock(String id,int[] min,int[] max,int cx,int cz,int r,int y0,int y1,boolean cylinder){
        boolean has(int x,int y,int z){return cylinder?y>=y0&&y<=y1&&(x-cx)*(x-cx)+(z-cz)*(z-cz)<=r*r:x>=min[0]&&x<=max[0]&&y>=min[1]&&y<=max[1]&&z>=min[2]&&z<=max[2];}
    }
    static Lock lock(JsonObject o){
        String id=o.has("id")?o.get("id").getAsString():"lock";
        if(o.has("center")){var c=o.getAsJsonArray("center");var y=o.getAsJsonArray("y");return new Lock(id,null,null,c.get(0).getAsInt(),c.get(1).getAsInt(),o.get("radius").getAsInt(),y.get(0).getAsInt(),y.get(1).getAsInt(),true);}
        return new Lock(id,arr(o.getAsJsonArray("min")),arr(o.getAsJsonArray("max")),0,0,0,0,0,false);
    }
    static int[] arr(JsonArray a){return new int[]{a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt()};}
    static List<Lock> locks(MapRuns r,String kind){
        var out=new ArrayList<Lock>();var e=r.header.get("locks");
        if(e!=null&&e.isJsonObject()&&e.getAsJsonObject().has(kind))for(JsonElement l:e.getAsJsonObject().getAsJsonArray(kind))out.add(lock(l.getAsJsonObject()));
        return out;
    }
    /** What the game itself protects on the selected map: capture rings (elevation to elevation+3) and the faction
     * spawn columns; in the camp, every station. */
    static String protectedBy(boolean camp,int x,int y,int z){
        if(camp){var st=CampWorld.station(new BlockPos(x,y,z));return st==null?null:"camp station "+st;}
        for(var p:Spec.POINTS)if(y>=p.elevation()&&y<=p.elevation()+3&&(x-p.x())*(x-p.x())+(z-p.z())*(z-p.z())<=p.radius()*p.radius())return "capture point "+p.id();
        // The flag the game redraws on every capture (Arena.flag): pole, cloth and lantern.
        for(var p:Spec.POINTS)if(z==p.z()&&x>=p.x()&&x<=p.x()+3&&y>=p.elevation()&&y<=p.elevation()+Arena.POINT_POLE+2)return "flag of point "+p.id();
        for(var f:Spec.FACTIONS)if(Math.abs(x-f.spawnX())<=2&&Math.abs(z-f.spawnZ())<=2)return "spawn "+f.id();
        return null;
    }

    /** protects OUT: what protectedBy guards on the selected map, as deny locks for tools/mapforge adopt batch --protects. */
    static int protects(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<2)return no(s,"protects OUT");
        var locks=new com.google.gson.JsonArray();
        for(var p:Spec.POINTS){var o=new com.google.gson.JsonObject();o.addProperty("id","point-"+p.id());var c=new com.google.gson.JsonArray();c.add(p.x());c.add(p.z());o.add("center",c);o.addProperty("radius",p.radius());var y=new com.google.gson.JsonArray();y.add(p.elevation());y.add(p.elevation()+3);o.add("y",y);locks.add(o);
            var fl=new com.google.gson.JsonObject();fl.addProperty("id","flag-"+p.id());var lo=new com.google.gson.JsonArray();lo.add(p.x());lo.add(p.elevation());lo.add(p.z());var hi=new com.google.gson.JsonArray();hi.add(p.x()+3);hi.add(p.elevation()+Arena.POINT_POLE+2);hi.add(p.z());fl.add("min",lo);fl.add("max",hi);locks.add(fl);}
        for(var f:Spec.FACTIONS){var o=new com.google.gson.JsonObject();o.addProperty("id","spawn-"+f.id());var lo=new com.google.gson.JsonArray();lo.add(f.spawnX()-2);lo.add(-64);lo.add(f.spawnZ()-2);var hi=new com.google.gson.JsonArray();hi.add(f.spawnX()+2);hi.add(319);hi.add(f.spawnZ()+2);o.add("min",lo);o.add("max",hi);locks.add(o);}
        var root=new com.google.gson.JsonObject();root.addProperty("map",Spec.MAPS[map()].id());root.addProperty("stamp",stamp(false));root.add("protect",locks);
        Files.writeString(file(a[1]),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root));
        return say(s,"protects "+a[1]+": "+locks.size()+" locks");
    }

    /** plan BATCH: the dry run. Counts what the batch would change and refuses it on any lock, protect or region
     * deletion; a passing plan is the only one apply accepts. */
    static int plan(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<2)return no(s,"plan BATCH");
        Path f=file(a[1]);var r=MapRuns.read(f);
        if(!"batch".equals(r.kind()))return no(s,a[1]+" is a "+r.kind()+", not a batch");
        boolean camp="camp".equals(r.h("map"));
        if(!camp&&!Spec.MAPS[map()].id().equals(r.h("map")))return no(s,"batch is for "+r.h("map")+" but the world holds "+Spec.MAPS[map()].id());
        if("delete".equals(r.h("purpose")))return no(s,"agents may not delete a region");
        var allow=locks(r,"allow");var deny=locks(r,"deny");
        if(allow.isEmpty())return no(s,"a batch needs at least one allow region");
        var states=r.states();var cur=r.new Cursor();var before=r.new WorldDigest();String fileSha=sha(f);plannedName=null;
        start("plan "+a[1],new Job(){
            long n,changes,removed,solid;final TreeMap<String,Long> hits=new TreeMap<>();final List<String> first=new ArrayList<>();final BlockPos.Mutable p=new BlockPos.Mutable();boolean scanned;
            public boolean step(ServerWorld w){
                if(!scanned){
                    for(int i=0;i<BUDGET;i++){
                        if(!cur.next()){scanned=true;return false;}
                        n++;var st=w.getBlockState(p.set(cur.x,cur.y,cur.z));var want=states[cur.p];
                        if(!st.isAir())solid++;
                        if(st==want)continue;
                        changes++;if(!st.isAir()&&want.isAir())removed++;
                        String why=null;
                        if(allow.stream().noneMatch(l->l.has(cur.x,cur.y,cur.z)))why="outside every allow region";
                        else for(var l:deny)if(l.has(cur.x,cur.y,cur.z)){why="deny lock "+l.id();break;}
                        if(why==null)why=protectedBy(camp,cur.x,cur.y,cur.z);
                        if(why!=null){hits.merge(why,1L,Long::sum);if(first.size()<8)first.add(cur.x+","+cur.y+","+cur.z+" ("+why+")");}
                    }
                    return false;
                }
                if(!before.step(w,BUDGET))return false;
                var problems=new ArrayList<String>();
                hits.forEach((k,v)->problems.add(v+" changed cells on "+k));
                if(removed>0&&removed*2>solid)problems.add("it would clear "+removed+" of the "+solid+" solid cells it covers: agents may not delete a region");
                String summary="plan "+a[1]+": "+n+" cells, "+changes+" would change ("+removed+" cleared)";
                writeReport(a[1]+".plan.txt",summary,problems,first);
                if(problems.isEmpty()){plannedName=a[1];plannedSha=fileSha;plannedBefore=before.result;finish(true,summary+"; passes, /mapforge apply "+a[1]);}
                else finish(false,summary+"; REFUSED: "+String.join("; ",problems)+(first.isEmpty()?"":" e.g. "+String.join("; ",first)));
                return true;
            }
        });
        return say(s,"planning "+r.cells()+" cells of "+a[1]);
    }
    static void writeReport(String name,String summary,List<String> problems,List<String> first){
        try{Files.writeString(file(name),summary+"\n"+(problems.isEmpty()?"passes\n":"REFUSED\n"+String.join("\n",problems)+"\n")+(first.isEmpty()?"":"examples:\n"+String.join("\n",first)+"\n"));}catch(IOException e){log("report "+name+": "+e);}
    }

    /** apply BATCH: only the batch the last plan passed, on an unchanged world; backup zip first, then the cells,
     * recording mapforge/(batch).undo.runs.gz with what they replaced. */
    static int apply(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<2)return no(s,"apply BATCH");
        Path f=file(a[1]);
        if(!a[1].equals(plannedName)||!sha(f).equals(plannedSha))return no(s,"run /mapforge plan "+a[1]+" first (a passing dry run of this exact file)");
        Path undoFile=file(a[1].replace(".runs.gz","")+".undo.runs.gz");
        if(Files.exists(undoFile))return no(s,undoFile.getFileName()+" already exists; undo or move it before applying again");
        var r=MapRuns.read(f);
        var undo=new MapRuns("undo").put("map",r.h("map")).put("stamp",String.valueOf(r.h("stamp"))).put("batch",plannedSha).put("source","MapForge apply "+a[1]);
        var check=r.new WorldDigest();var write=r.new Apply(undo);plannedName=null;
        start("apply "+a[1],new Job(){
            int phase;String zip;MapRuns.WorldDigest after;
            public boolean step(ServerWorld w){
                switch(phase){
                    case 0->{if(!check.step(w,BUDGET))return false;
                        if(!check.result.equals(plannedBefore)){finish(false,"the world changed since the plan; plan again");return true;}
                        try{zip=backup("apply");}catch(IOException e){finish(false,"backup failed, nothing applied: "+e);return true;}
                        phase=1;return false;}
                    case 1->{if(!write.step(w,BUDGET))return false;after=undo.new WorldDigest();phase=2;return false;}
                    default->{if(!after.step(w,BUDGET))return false;
                        undo.put("afterDigest",after.result);
                        try{long size=undo.write(undoFile);finish(true,"apply "+a[1]+": "+write.changed+" cells changed of "+write.visited+"; undo "+undoFile.getFileName()+" ("+size+" bytes); backup "+zip);}
                        catch(IOException e){finish(false,"applied, but the undo file failed: "+e+"; restore backup "+zip);}
                        return true;}
                }
            }
        });
        return say(s,"applying "+a[1]);
    }

    /** undo FILE: restores what an apply replaced, when the world still holds what that apply left. */
    static int undo(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<2)return no(s,"undo FILE");
        var r=MapRuns.read(file(a[1]));
        if(!"undo".equals(r.kind())||r.h("afterDigest")==null)return no(s,a[1]+" is not an undo file");
        var check=r.new WorldDigest();var write=r.new Apply(null);
        start("undo "+a[1],new Job(){
            int phase;String zip;
            public boolean step(ServerWorld w){
                if(phase==0){if(!check.step(w,BUDGET))return false;
                    if(!check.result.equals(r.h("afterDigest"))){finish(false,"the world changed since that apply; undo refused (restore a backup instead)");return true;}
                    try{zip=backup("undo");}catch(IOException e){finish(false,"backup failed, nothing undone: "+e);return true;}
                    phase=1;return false;}
                if(!write.step(w,BUDGET))return false;
                try{Files.move(file(a[1]),file(a[1]+".done"));}catch(IOException e){log("could not retire "+a[1]+": "+e);}
                finish(true,"undo "+a[1]+": "+write.changed+" cells restored; backup "+zip);return true;
            }
        });
        return say(s,"undoing "+a[1]);
    }

    /** adopt OVERLAY: the shipped MapAdoption path (header checks, base digest, budgeted apply) on a file. */
    static int adopt(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<2)return no(s,"adopt OVERLAY");
        MapRuns r;try{r=MapRuns.read(file(a[1]));}catch(IOException e){MapAdoption.status="refused: unreadable: "+e.getMessage();return no(s,"adoption refused: unreadable: "+e.getMessage());}
        MapAdoption.reset(map());MapAdoption.startedAt=System.nanoTime();
        if("camp".equals(r.h("map"))){
            // The spike's camp overlay: the same check and apply as a map, against the camp's stamp.
            if(!"overlay".equals(r.kind())||!stamp(true).equals(r.h("stamp"))){MapAdoption.status="refused: camp overlay kind or stamp";return no(s,"adoption refused: camp overlay kind or stamp");}
            MapAdoption.runs=r;MapAdoption.check=r.new WorldDigest();MapAdoption.phase=MapAdoption.Phase.CHECK;MapAdoption.status="checking";
        }else MapAdoption.begin(r,s.getServer().getOverworld());
        if(MapAdoption.phase==MapAdoption.Phase.DONE)return no(s,"adoption "+MapAdoption.status);
        start("adopt "+a[1],w->{if(!MapAdoption.step(w))return false;finish(MapAdoption.status.startsWith("adopted"),"adopt "+a[1]+": "+MapAdoption.status);return true;});
        return say(s,"adopting "+a[1]);
    }
    /** snapshot CAPTURE: writes a whole capture back (the full-snapshot form of the spike), timed. */
    static int snapshot(ServerCommandSource s,String[] a)throws IOException{
        if(a.length<2)return no(s,"snapshot CAPTURE");
        long t=System.nanoTime();var r=MapRuns.read(file(a[1]));long read=(System.nanoTime()-t)/1_000_000;
        if(!"capture".equals(r.kind()))return no(s,a[1]+" is not a capture");
        var write=r.new Apply(null);
        start("snapshot "+a[1],w->{if(!write.step(w,BUDGET))return false;finish(true,"snapshot "+a[1]+": read "+read+" ms, "+write.visited+" cells, "+write.changed+" changed");return true;});
        return say(s,"writing snapshot "+a[1]);
    }
}
