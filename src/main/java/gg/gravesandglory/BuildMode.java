package gg.gravesandglory;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionType;

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
 * The builder server (Kort 2026-11-03): one save (gg-build) holding seven canvases at once, the flat dimensions gravesandglory:build_(camp, lobby, rivergate, ironhold,
 * frostmarch, hollowwood, kingsfield) of the built-in gg_build data pack, which only -Dgg.build registers. Each canvas is painted once with its plan (BuildWorlds) and
 * marked by gg-build.json in its dimension folder; a marked canvas is never painted or regenerated again. There is no Battle, no unit, no NPC, no bot and no match.
 * Everyone is creative and op (once, on first join); /map lists the canvases and takes the caller to one, /warp moves them within the canvas they stand in, vanilla
 * saves every 5 minutes, and the whole save (every dimension) is zipped every 30 minutes while anyone has been on, on stop and on /backup.
 */
final class BuildMode {
    static final String PROPERTY="gg.build";
    static final String MARKER="gg-build.json";
    static final String SAVE="gg-build";
    static final String OPPED="gg-build-opped.txt";
    static final int BACKUP_TICKS=30*60*20;
    static final DateTimeFormatter STAMP=DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    static MinecraftServer server;
    static boolean ready;
    static int ticks,sinceBackup;
    static boolean changed;
    static final Map<String,BuildWorlds.Plan> PLANS=new LinkedHashMap<>();
    /**
     * Players who joined this tick, settled at the end of it. Fabric's JOIN fires inside PlayerManager.onPlayerConnect before vanilla adds the player to the
     * world it loaded them into (the overworld for a first join), so a cross-world teleport there left the player registered in both worlds; the first leave
     * after moving a chunk then crashed the server in ChunkTicketManager.handleChunkLeave (2026-11-03). By END_SERVER_TICK onPlayerConnect has returned.
     */
    static final List<ServerPlayerEntity> JOINED=new ArrayList<>();

    static boolean active(){return System.getProperty(PROPERTY)!=null;}
    static List<String> worlds(){return new ArrayList<>(PLANS.isEmpty()?BuildWorlds.ids():PLANS.keySet());}

    static void init(){
        // The seven flat canvases. Registered only here, so outside build mode no dimension is added.
        ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("gravesandglory","gg_build"),FabricLoader.getInstance().getModContainer("gravesandglory").orElseThrow(),
            Text.literal("Graves and Glory builder worlds"),ResourcePackActivationType.ALWAYS_ENABLED);
        ServerLifecycleEvents.SERVER_STARTED.register(BuildMode::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{if(ready&&changed)backup(s,"stop");});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{server=null;ready=false;JOINED.clear();});
        ServerTickEvents.END_SERVER_TICK.register(BuildMode::tick);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,s)->{
            if(!ready){handler.disconnect(Text.literal("The builder worlds are being painted for the first time. Try again in a minute."));return;}
            changed=true;var player=handler.player;
            opOnce(s,player);
            JOINED.add(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((old,player,alive)->{if(ready&&plan(player)==null)arrive(player,PLANS.get("lobby"));});
        ServerPlayConnectionEvents.DISCONNECT.register((handler,s)->changed=true);
        CommandRegistrationCallback.EVENT.register((dispatcher,access,environment)->{
            dispatcher.register(CommandManager.literal("warp").requires(s->s.hasPermissionLevel(MatchRules.OPS)) // ops only (Kort 2026-11-01)
                .executes(c->listWarps(c.getSource()))
                .then(CommandManager.argument("name",StringArgumentType.word())
                    .suggests((c,b)->{var p=c.getSource().getPlayer();var plan=p==null?null:plan(p);return CommandSource.suggestMatching(plan==null?List.of():plan.warps().keySet(),b);})
                    .executes(c->warp(c.getSource(),StringArgumentType.getString(c,"name")))));
            dispatcher.register(CommandManager.literal("backup").requires(s->s.hasPermissionLevel(2)).executes(c->{backup(server,"command");c.getSource().sendFeedback(()->Text.literal("Backup written to backups/"+SAVE),false);return 1;}));
            dispatcher.register(CommandManager.literal("map").requires(s->s.hasPermissionLevel(2))
                .executes(c->listWorlds(c.getSource()))
                .then(CommandManager.argument("name",StringArgumentType.word())
                    .suggests((c,b)->CommandSource.suggestMatching(worlds(),b))
                    .executes(c->go(c.getSource(),StringArgumentType.getString(c,"name")))));
        });
    }

    static void start(MinecraftServer s){
        server=s;var overworld=s.getOverworld();
        var rules=overworld.getGameRules();
        rules.get(GameRules.DO_DAYLIGHT_CYCLE).set(false,s);
        rules.get(GameRules.DO_WEATHER_CYCLE).set(false,s);
        rules.get(GameRules.DO_MOB_SPAWNING).set(false,s);
        rules.get(GameRules.DO_FIRE_TICK).set(false,s);
        rules.get(GameRules.DO_VINES_SPREAD).set(false,s);
        rules.get(GameRules.KEEP_INVENTORY).set(true,s);
        rules.get(GameRules.ANNOUNCE_ADVANCEMENTS).set(false,s);
        // The canvases share the overworld's clock and weather, so this makes every one of them always day and clear.
        overworld.setTimeOfDay(6000);overworld.setWeather(0,0,false,false);
        s.setDifficulty(Difficulty.PEACEFUL,true);
        PLANS.clear();
        for(var p:BuildWorlds.plans())PLANS.put(p.id(),p);
        for(var p:PLANS.values())if(s.getWorld(p.key())==null){log("dimension "+p.key().getValue()+" is missing (the gg_build data pack is not enabled); stopping so nothing is built in the wrong place");s.stop(false);return;}
        if(Files.exists(root().resolve(MARKER)))log("this save holds an old single-world builder save (gg-build.json at its root); its overworld is left untouched, builders start in the lobby canvas");
        int left=(int)PLANS.values().stream().filter(p->!Files.exists(marker(p))).count();
        if(left==0){ready=true;log("all "+PLANS.size()+" builder worlds kept as they are");}
        else log("painting "+left+" of "+PLANS.size()+" builder worlds for the first time");
    }

    static Path root(){return server.getSavePath(WorldSavePath.ROOT);}
    /** The canvas's own marker, inside its dimension folder so a backup or restore keeps the two together. */
    static Path marker(BuildWorlds.Plan p){return DimensionType.getSaveDirectory(p.key(),root()).resolve(MARKER);}
    static void log(String text){System.out.println("[Graves and Glory build] "+text);}
    static ServerWorld world(BuildWorlds.Plan p){return server.getWorld(p.key());}
    /** The plan of the canvas the player stands in, or null outside the canvases (the unused overworld). */
    static BuildWorlds.Plan plan(ServerPlayerEntity player){for(var p:PLANS.values())if(player.getWorld().getRegistryKey()==p.key())return p;return null;}

    static void tick(MinecraftServer s){
        if(server==null)return;
        if(!ready){paintNext();return;}
        settleJoined(s);
        if(!s.getPlayerManager().getPlayerList().isEmpty())changed=true;
        if(++sinceBackup>=BACKUP_TICKS){sinceBackup=0;if(changed)backup(s,"timer");}
    }

    /** One unpainted canvas a tick: its lines, outlines and signs, then its marker. When none is left the server opens. Nothing here ever runs again for a marked canvas. */
    static void paintNext(){
        for(var p:PLANS.values()){
            if(Files.exists(marker(p)))continue;
            long started=System.nanoTime();
            LobbyCanvas.paint(world(p),p.areas(),p.lines());
            try{SafeFiles.write(marker(p),"{\"world\":\""+p.id()+"\",\"painted\":\""+LocalDateTime.now()+"\"}");}catch(IOException e){log("marker for "+p.id()+" could not be written: "+e+"; stopping so it is not painted twice");server.stop(false);return;}
            log(p.id()+" painted ("+p.areas().size()+" outlines, "+p.lines().size()+" lines) in "+(System.nanoTime()-started)/1000000+" ms");
            return;
        }
        server.saveAll(true,true,true);
        ready=true;changed=true;log("every builder world painted; builders may join");
    }

    // ---- auto-op: every player is op once, on first join (Kort 2026-11-03); a later de-op sticks ----
    static void opOnce(MinecraftServer s,ServerPlayerEntity player){
        Path file=s.getRunDirectory().toPath().resolve(OPPED);String uuid=player.getUuidAsString();
        try{
            String seen=Files.exists(file)?Files.readString(file):"";
            if(seen.lines().anyMatch(uuid::equals))return;
            if(!s.getPlayerManager().isOperator(player.getGameProfile()))s.getPlayerManager().addToOperators(player.getGameProfile());
            SafeFiles.write(file,seen+uuid+"\n");
            log("made "+player.getGameProfile().getName()+" op on first join");
        }catch(IOException e){log("auto-op of "+player.getGameProfile().getName()+" not recorded: "+e);}
    }

    /** Creative mode, and the lobby for anyone outside the canvases, once each new player is fully in their world. */
    static void settleJoined(MinecraftServer s){
        if(JOINED.isEmpty())return;
        var list=new ArrayList<>(JOINED);JOINED.clear();
        for(var player:list){
            if(player.isDisconnected()||s.getPlayerManager().getPlayer(player.getUuid())!=player)continue;
            player.changeGameMode(GameMode.CREATIVE);
            if(plan(player)==null)arrive(player,PLANS.get("lobby"));
        }
    }

    // ---- moving between and within canvases ----
    static void arrive(ServerPlayerEntity player,BuildWorlds.Plan p){teleport(player,p,p.warps().values().iterator().next());}
    static void teleport(ServerPlayerEntity player,BuildWorlds.Plan p,double[] w){
        player.teleport(world(p),w[0]+0.5,w[1],w[2]+0.5,(float)w[3],0);TeleportSync.afterTeleport(player);
    }
    static int listWarps(ServerCommandSource s){
        var player=s.getPlayer();var p=player==null?null:plan(player);
        if(p==null){s.sendError(Text.literal("Not in a builder world. /map lists them."));return 0;}
        s.sendFeedback(()->Text.literal("Warps in "+p.id()+": "+String.join(", ",p.warps().keySet())),false);return p.warps().size();
    }
    static int warp(ServerCommandSource s,String name){
        var player=s.getPlayer();
        if(player==null){s.sendError(Text.literal("Only a player can warp."));return 0;}
        var p=plan(player);
        if(p==null){s.sendError(Text.literal("Not in a builder world. /map lists them."));return 0;}
        var w=p.warps().get(name);
        if(w==null){s.sendError(Text.literal("No warp "+name+" in "+p.id()+". /warp lists them."));return 0;}
        teleport(player,p,w);
        s.sendFeedback(()->Text.literal("Warped to "+name),false);return 1;
    }
    static int listWorlds(ServerCommandSource s){
        var player=s.getPlayer();var p=player==null?null:plan(player);
        s.sendFeedback(()->Text.literal((p==null?"":"You are in "+p.id()+". ")+"Worlds: "+String.join(", ",worlds())+". /map <name> takes you there."),false);return PLANS.size();
    }
    static int go(ServerCommandSource s,String name){
        var p=PLANS.get(name);
        if(p==null){s.sendError(Text.literal("No world "+name+". Worlds: "+String.join(", ",worlds())));return 0;}
        var player=s.getPlayer();
        if(player==null){s.sendError(Text.literal("Only a player can go to a world."));return 0;}
        arrive(player,p);
        s.sendFeedback(()->Text.literal("Now in "+name+". /warp lists its places."),false);return 1;
    }

    // ---- backups ----
    static Path backups(){return server.getRunDirectory().toPath().resolve("backups").resolve(SAVE);}

    /** Saves, then zips the whole save folder (the overworld and every canvas dimension) on the server thread: a copy taken while a chunk is being written could be torn. */
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
