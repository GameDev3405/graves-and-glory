package gg.gravesandglory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The builder server: one saved world per map and one for the camp, started with -Dgg.build=camp or -Dgg.build=(a map id). Each world generates its map exactly once (the marker file gg-build.json says it did)
 * and from then on the builders' work is never regenerated, rebuilt or discarded: there is no Battle, no unit, no NPC, no bot and no match. Players are creative, /warp moves them around the world,
 * /map hands the server to another saved world (the start script reads next-world.txt and relaunches), and the world is zipped every 30 minutes while anyone has been on.
 */
final class BuildMode {
    static final String PROPERTY="gg.build";
    static final String MARKER="gg-build.json";
    static final String NEXT="next-world.txt";
    static final int BACKUP_TICKS=30*60*20;
    static final DateTimeFormatter STAMP=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** The world this server runs: -1 for the camp, else the index into Spec.MAPS. */
    static int map=-1;
    static MinecraftServer server;
    static ServerWorld world;
    static boolean ready;
    static int column,ticks,sinceBackup;
    static boolean placing,changed;
    static final Map<String,double[]> WARPS=new LinkedHashMap<>();

    static boolean active(){return System.getProperty(PROPERTY)!=null;}
    static String id(int index){return index<0?"camp":Spec.MAPS[index].id();}
    static List<String> worlds(){var all=new ArrayList<String>();all.add("camp");for(var m:Spec.MAPS)all.add(m.id());return all;}
    static int indexOf(String id){if(id.equals("camp"))return -1;for(int i=0;i<Spec.MAPS.length;i++)if(Spec.MAPS[i].id().equals(id))return i;return -2;}
    static String safe(String id){return id.toLowerCase().replaceAll("[^a-z0-9_-]","_");}

    static void init(){
        String wanted=System.getProperty(PROPERTY).trim().toLowerCase();
        map=indexOf(wanted);
        if(map==-2)throw new IllegalStateException("gg.build must be camp or one of "+worlds()+", not "+wanted);
        ServerLifecycleEvents.SERVER_STARTED.register(BuildMode::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(ready&&changed)backup(s,"stop");});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{server=null;world=null;});
        ServerTickEvents.END_SERVER_TICK.register(BuildMode::tick);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,s)->{
            if(!ready){handler.disconnect(Text.literal("The "+id(map)+" world is being generated for the first time ("+progress()+"%). Try again in a few minutes."));return;}
            changed=true;handler.player.changeGameMode(GameMode.CREATIVE);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,s)->changed=true);
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
            dispatcher.register(CommandManager.literal("warp").requires(s->s.hasPermissionLevel(MatchRules.OPS)) // ops only (Kort 2026-11-01)
                .executes(c->listWarps(c.getSource()))
                .then(CommandManager.argument("name",StringArgumentType.word())
                    .suggests((c,b)->CommandSource.suggestMatching(WARPS.keySet(),b))
                    .executes(c->warp(c.getSource(),StringArgumentType.getString(c,"name")))));
            dispatcher.register(CommandManager.literal("backup").requires(s->s.hasPermissionLevel(2)).executes(c->{backup(server,"command");c.getSource().sendFeedback(()->Text.literal("Backup written to backups/"+id(map)),false);return 1;}));
            dispatcher.register(CommandManager.literal("map").requires(s->s.hasPermissionLevel(2))
                .executes(c->listWorlds(c.getSource()))
                .then(CommandManager.argument("name",StringArgumentType.word())
                    .suggests((c,b)->CommandSource.suggestMatching(worlds(),b))
                    .executes(c->switchTo(c,StringArgumentType.getString(c,"name")))));
        });
    }

    static void start(MinecraftServer s){
        server=s;world=s.getOverworld();
        if(map>=0)CampaignMaps.select(map);
        var rules=world.getGameRules();
        rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false,s);
        rules.get(GameRules.DO_WEATHER_CYCLE).set(false,s);
        rules.get(GameRules.DO_MOB_SPAWNING).set(false,s);
        rules.get(GameRules.DO_FIRE_TICK).set(false,s);
        rules.get(GameRules.DO_VINES_SPREAD).set(false,s);
        rules.get(GameRules.KEEP_INVENTORY).set(true,s);
        rules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false,s);
        world.setTimeOfDay(6000);world.setWeather(0,0,false,false);
        s.setDifficulty(Difficulty.PEACEFUL,true);
        warps();
        if(Files.exists(marker())){
            String saved="";try{saved=Files.readString(marker());}catch(IOException e){log("marker unreadable: "+e);}
            if(!saved.contains("\""+id(map)+"\"")){log("this save belongs to a different world than "+id(map)+"; stopping so nothing is overwritten");server.stop(false);return;}
            ready=true;placeSpawn();log("existing "+id(map)+" world kept as it is");
        }
        else log("generating "+id(map)+" for the first time");
    }

    static Path root(){return server.getSavePath(WorldSavePath.ROOT);}
    static Path marker(){return root().resolve(MARKER);}
    static void log(String text){System.out.println("[Graves and Glory build] "+text);}
    static int total(){return (2*GravesAndGlory.R.worldHalfX()+1)*(2*GravesAndGlory.R.worldHalfZ()+1);}
    static int progress(){return map<0?0:Math.min(99,column*100/Math.max(1,total()));}

    static void tick(MinecraftServer s){
        if(!ready){generate();return;}
        if(!s.getPlayerManager().getPlayerList().isEmpty())changed=true;
        if(++sinceBackup>=BACKUP_TICKS){sinceBackup=0;if(changed)backup(s,"timer");}
    }

    /** The one generation: the camp in a single step, a map as the Battle builds it (columns, the finishing pass, the landmarks), then the marker. Nothing here ever runs again for this world. */
    static void generate(){
        if(server==null)return;
        if(map<0){CampWorld.buildBare(world);done();return;}
        var rules=GravesAndGlory.R;int total=total(),width=2*rules.worldHalfX()+1;
        for(int n=0;n<rules.buildColumnsPerTick()&&column<total;n++,column++)Arena.column(world,column%width-rules.worldHalfX(),column/width-rules.worldHalfZ());
        if(++ticks%200==0)log("generating "+id(map)+" "+progress()+"%");
        if(column<total)return;
        if(!placing){Arena.finish(world);Structures.queueLandmarks(world);placing=true;}
        else if(Structures.step(world))done();
    }

    static void done(){
        try{SafeFiles.write(marker(),"{\"world\":\""+id(map)+"\",\"generated\":\""+LocalDateTime.now()+"\"}");}catch(IOException e){log("marker could not be written: "+e);}
        server.saveAll(true,true,true);
        ready=true;placeSpawn();changed=true;log(id(map)+" generated; builders may join");
    }

    // ---- warps ----
    static void warps(){
        WARPS.clear();
        if(map<0){
            var c=Spec.CAMP[0];
            WARPS.put("spawn",new double[]{c.originX()+c.spawnDx(),c.spawnY(),c.originZ()+c.spawnDz(),c.spawnYaw()});
            for(var s:Spec.CAMPSTATIONS)WARPS.put("station-"+safe(s.id()),new double[]{c.originX()+s.x(),s.y(),c.originZ()+s.z(),s.yaw()});
            return;
        }
        WARPS.put("center",new double[]{0,Double.NaN,0,0});
        for(var f:Spec.FACTIONS){
            WARPS.put("spawn-"+safe(f.id()),new double[]{f.spawnX(),Double.NaN,f.spawnZ(),0});
            WARPS.put("barracks-"+safe(f.id()),new double[]{f.barracksX(),Double.NaN,f.barracksZ(),0});
        }
        for(var p:Spec.POINTS)WARPS.put("point-"+safe(p.id()),new double[]{p.x(),Double.NaN,p.z(),0});
        for(var l:Spec.LANDMARKS)if(l.map()==map)WARPS.put("lm-"+safe(l.id()),new double[]{l.x(),Double.NaN,l.z(),0});
        for(var h:Spec.SUPPLYHOUSES)if(h.map()==map)WARPS.put("house-"+safe(h.id()),new double[]{h.yardX(),Double.NaN,h.yardZ(),0});
    }
    static double[] at(double[] w){
        double y=Double.isNaN(w[1])?world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(w[0]),(int)Math.floor(w[2])):w[1];
        return new double[]{w[0]+0.5,y,w[2]+0.5,w[3]};
    }
    static void placeSpawn(){var p=at(WARPS.values().iterator().next());world.setSpawnPos(BlockPos.ofFloored(p[0],p[1],p[2]),(float)p[3]);}
    static int listWarps(ServerCommandSource s){s.sendFeedback(()->Text.literal("Warps in "+id(map)+": "+String.join(", ",WARPS.keySet())),false);return WARPS.size();}
    static int warp(ServerCommandSource s,String name){
        var w=WARPS.get(name);
        if(w==null){s.sendError(Text.literal("No warp "+name+". /warp lists them."));return 0;}
        var player=s.getPlayer();
        if(player==null){s.sendError(Text.literal("Only a player can warp."));return 0;}
        var p=at(w);player.teleport(world,p[0],p[1],p[2],(float)p[3],0);
        s.sendFeedback(()->Text.literal("Warped to "+name),false);return 1;
    }
    static int listWorlds(ServerCommandSource s){s.sendFeedback(()->Text.literal("This is "+id(map)+". Worlds: "+String.join(", ",worlds())+". /map <name> switches (about 30 seconds)."),false);return 1;}
    static int switchTo(CommandContext<ServerCommandSource> c,String name){
        var s=c.getSource();
        if(indexOf(name)==-2){s.sendError(Text.literal("No world "+name+". Worlds: "+String.join(", ",worlds())));return 0;}
        if(name.equals(id(map))){s.sendError(Text.literal("Already in "+name));return 0;}
        try{SafeFiles.write(server.getRunDirectory().toPath().resolve(NEXT),name);}catch(IOException e){s.sendError(Text.literal("Could not queue the switch: "+e));return 0;}
        server.getPlayerManager().broadcast(Text.literal("Saving, then switching to "+name+". Reconnect in about 30 seconds."),false);
        server.stop(false);return 1;
    }

    // ---- backups ----
    static Path backups(){return server.getRunDirectory().toPath().resolve("backups").resolve(id(map));}

    /** Saves, then zips the whole save folder on the server thread: a copy taken while a chunk is being written could be torn, and the world is small. */
    static void backup(MinecraftServer s,String why){
        long started=System.nanoTime();
        try{
            s.saveAll(true,true,true);
            Path dir=backups();Files.createDirectories(dir);
            Path target=dir.resolve(LocalDateTime.now().format(STAMP)+".zip"),temp=dir.resolve(target.getFileName()+".part");
            zip(root(),temp);Files.move(temp,target);
            prune(dir,LocalDateTime.now());changed=false;
            log("backup ("+why+") "+target.getFileName()+" "+Files.size(target)/1024+" KB in "+(System.nanoTime()-started)/1000000+" ms");
        }catch(Exception e){log("backup failed: "+e);}
    }
    static void zip(Path root,Path target)throws IOException{
        try(OutputStream out=Files.newOutputStream(target);ZipOutputStream zip=new ZipOutputStream(out);Stream<Path> files=Files.walk(root)){
            for(Path file:(Iterable<Path>)files::iterator){
                if(!Files.isRegularFile(file)||file.getFileName().toString().equals("session.lock"))continue;
                zip.putNextEntry(new ZipEntry(root.relativize(file).toString().replace('\\','/')));Files.copy(file,zip);zip.closeEntry();
            }
        }
    }
    static LocalDateTime stamp(Path file){try{return LocalDateTime.parse(file.getFileName().toString().replace(".zip",""),STAMP);}catch(Exception e){return null;}}
    /** Everything from the last 48 hours stays; of the older ones the newest zip of each day stays for 14 days; the rest go. */
    static void prune(Path dir,LocalDateTime now)throws IOException{
        var keepDay=new HashSet<LocalDate>();var drop=new ArrayList<Path>();
        List<Path> zips;try(Stream<Path> list=Files.list(dir)){zips=list.filter(f->stamp(f)!=null).sorted((a,b)->stamp(b).compareTo(stamp(a))).toList();}
        for(Path f:zips){
            var t=stamp(f);var age=Duration.between(t,now);
            if(age.toHours()<48)continue;
            if(age.toDays()>=14||!keepDay.add(t.toLocalDate()))drop.add(f);
        }
        for(Path f:drop)Files.deleteIfExists(f);
    }
}
