package gg.gravesandglory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.IntStream;

/** Unshipped: runServer -PbotSim=<matches> plays bot-vs-bot rounds on a dedicated server with nobody online and stops the server when done.
 *  One match at a time (one Battle per server). Dawn's bots play -PsimDawn, Dusk's -PsimDusk (Spec.BOTCOMMAND ids); -PsimMaps is cycled.
 *  -PsimKit=native (default) gives each team its own faction's troops; dawn or dusk gives both teams that faction's troop rows (colours and bases stay), so kit and seat can be told apart.
 *  Writes botsim/results.csv (one row per match) and botsim/gold.csv (each team every 30 s of round time) under the server root.
 *  Soak aid, off by default: GRAVES_BOTSIM_PROBE=1 in the environment (or -Dgravesandglory.simProbe=true) also writes botsim/probe.csv and prints one "[botsim] probe" line per match:
 *  server tick times over the round, entities and the battle's own collections after the round's cleanup, and heap after a forced GC. */
public final class BotSimulation implements ModInitializer {
    static final int SAMPLE_TICKS=600;
    /** How long an arena build may take (5 minutes) before the simulation gives up. */
    static final int READY_TICKS=6000;
    /** Campaign.lastWinner while a simulated round runs. Only Campaign.finish overwrites it, so a round that stops any other way is recorded as aborted. */
    static final int ABORTED=-2;
    int matches,size,roundSeconds,waited,notReady;int[] maps;String dawnTier,duskTier,kit;Path results,gold;
    /** The seats, for the CSV team columns: -PsimKit may give both teams one faction id. */
    static final String[] SEATS={"dawn","dusk"};
    /** The match being prepared or played (0-based); playing while its round runs. */
    int match;boolean playing,done;
    /** This round's bot armies. They leave Battle.squads when the round ends, so the end-of-round tally is read from here. */
    final List<GravesAndGlory.Squad> armies=new ArrayList<>();
    int dawnWins,duskWins,draws,aborted;final long[] earnedAtEnd={0,0};
    /** Soak probe: the columns of botsim/probe.csv, and the state of the round being timed. */
    static final String[] PROBE_COLUMNS={"match","map","startedAt","endedAt","roundTicks","tickMeanMs","tickP50Ms","tickP95Ms","tickP99Ms","tickMaxMs","ticksOver50ms","ticksOver100ms","serverAvgTickMs",
        "heapMbBeforeGc","heapMbAfterGc","heapMaxMb","gcCount","gcMillis","threads","classes","directMb","nonHeapMb","loadedChunks","entities","units","shots","points","specPoints","squads","respawns","pings","botRespawns","botRetreating","vacated","kinds"};
    static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm:ss");
    boolean probe;Path probeCsv;long tickStart;long[] tickNanos=new long[8192];int tickCount;String startedAt="";

    @Override public void onInitialize(){
        // Absent means a normal server. Given but blank or non-numeric is an error, not a silent plain server.
        String n=System.getProperty("gravesandglory.botSim");if(n==null)return;
        matches=number("botSim",n);
        dawnTier=tier(System.getProperty("gravesandglory.simDawn","normal"));duskTier=tier(System.getProperty("gravesandglory.simDusk","normal"));
        String mapList=System.getProperty("gravesandglory.simMaps");
        maps=mapList==null?IntStream.range(0,Spec.MAPS.length).toArray():Arrays.stream(mapList.split(",")).map(String::trim).filter(v->!v.isEmpty()).mapToInt(v->number("simMaps",v)).toArray();
        size=number("simSize",System.getProperty("gravesandglory.simSize","3"));roundSeconds=number("simRoundSeconds",System.getProperty("gravesandglory.simRoundSeconds","300"));
        kit=System.getProperty("gravesandglory.simKit","native").trim();
        if(!kit.equals("native")&&Arrays.stream(Spec.FACTIONS).noneMatch(f->f.id().equals(kit)))throw new IllegalArgumentException("[botsim] simKit must be native, dawn or dusk, got '"+kit+"'");
        if(matches<1||roundSeconds<1||size<1||size>Spec.RULES[0].playersPerTeam()||maps.length==0||Arrays.stream(maps).anyMatch(m->m<0||m>=Spec.MAPS.length))
            throw new IllegalArgumentException("[botsim] bad settings: matches="+matches+" size="+size+" roundSeconds="+roundSeconds+" maps="+Arrays.toString(maps));
        Path dir=FabricLoader.getInstance().getGameDir().resolve("botsim");results=dir.resolve("results.csv");gold=dir.resolve("gold.csv");
        try{Files.createDirectories(dir);
            Files.writeString(results,"match,map,dawnTier,duskTier,winner,dawnScore,duskScore,seconds\n",StandardCharsets.UTF_8);
            Files.writeString(gold,"match,second,team,goldEarned,goldBank,living\n",StandardCharsets.UTF_8);
        }catch(IOException e){throw new UncheckedIOException(e);}
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        probe="1".equals(System.getenv("GRAVES_BOTSIM_PROBE"))||Boolean.getBoolean("gravesandglory.simProbe");
        if(probe){
            probeCsv=dir.resolve("probe.csv");
            try{Files.writeString(probeCsv,String.join(",",PROBE_COLUMNS)+"\n",StandardCharsets.UTF_8);}catch(IOException e){throw new UncheckedIOException(e);}
            // Registered after the harness's own listener, so the tick that ends a round is not timed as part of it.
            ServerTickEvents.START_SERVER_TICK.register(s->tickStart=System.nanoTime());
            ServerTickEvents.END_SERVER_TICK.register(s->{if(playing){if(tickCount==tickNanos.length)tickNanos=Arrays.copyOf(tickNanos,tickCount*2);tickNanos[tickCount++]=System.nanoTime()-tickStart;}});
        }
    }
    static String tier(String id){
        String t=id.trim();if(Arrays.stream(Spec.BOTCOMMAND).noneMatch(r->r.id().equals(t)))throw new IllegalArgumentException("[botsim] unknown bot tier: "+id);return t;
    }
    static int number(String name,String value){
        try{return Integer.parseInt(value.trim());}catch(NumberFormatException e){throw new IllegalArgumentException("[botsim] "+name+" must be a whole number, got '"+value+"'");}
    }

    void tick(MinecraftServer server){
        var b=GravesAndGlory.battle;if(done)return;
        if(b==null){if(++waited==200){System.out.println("[botsim] no battle on this world (level-name must be Graves and Glory); stopping");done=true;server.stop(false);}return;}
        if(playing){
            if(b.running){long elapsed=b.time-b.roundStarted;if(elapsed>0&&elapsed%SAMPLE_TICKS==0)sample(b,elapsed/20);return;}
            playing=false;record(b);
            if(++match>=matches){summary();done=true;server.stop(false);}
            return;
        }
        // A fresh world or a map switch: wait until the arena is built, but not forever.
        if(!b.ready){if(++notReady==READY_TICKS){System.out.println("[botsim] arena not ready after "+READY_TICKS+" ticks; stopping");done=true;server.stop(false);}return;}
        notReady=0;if(b.running)return;
        int map=maps[match%maps.length];
        if(b.campaign.map!=map){b.campaign.switchMap(map);return;}
        var c=b.campaign;c.size=size;c.fill=true;c.practice=false;c.difficulty=dawnTier;b.roundSeconds=roundSeconds;c.lastWinner=ABORTED;
        useKit();
        b.startRound();
        armies.clear();for(var s:b.squads.values())if(s.bot){if(s.team==1)s.tier=duskTier;armies.add(s);}
        playing=true;if(probe)probeStart();
    }
    /** -PsimKit dawn or dusk: both teams' faction rows take that faction's id and troops, so the bots' lineup, purchases and full strength
     *  (all read Spec.FACTIONS[team].id()) use one kit. Name, colour, base and barracks stay the seat's. Map switches restore the rows,
     *  so this runs before every round. */
    void useKit(){
        if(kit.equals("native"))return;
        var donor=Arrays.stream(Spec.FACTIONS).filter(f->f.id().equals(kit)).findFirst().orElseThrow();
        for(int t=0;t<2;t++){var f=Spec.FACTIONS[t];Spec.FACTIONS[t]=new Spec.FactionsRow(kit,f.name(),f.color(),f.spawnX(),f.spawnZ(),donor.troops(),f.barracksX(),f.barracksZ());}
    }
    /** One gold.csv row per team: gold earned so far, gold in hand and living troops. */
    void sample(GravesAndGlory.Battle b,long second){
        StringBuilder rows=new StringBuilder();
        for(int team=0;team<2;team++){final int t=team;int earned=0,bank=0;for(var s:armies)if(s.team==t){earned+=s.stats.goldEarned;bank+=s.gold;}
            long living=b.units.stream().filter(u->u.squad.team==t&&u.mob.isAlive()).count();
            rows.append(match+1).append(',').append(second).append(',').append(SEATS[t]).append(',').append(earned).append(',').append(bank).append(',').append(living).append('\n');}
        append(gold,rows.toString());
    }
    void record(GravesAndGlory.Battle b){
        int winner=b.campaign.lastWinner;
        if(winner==0)dawnWins++;else if(winner==1)duskWins++;else if(winner==ABORTED)aborted++;else draws++;
        for(var s:armies)earnedAtEnd[s.team]+=s.stats.goldEarned;
        append(results,String.join(",",String.valueOf(match+1),String.valueOf(maps[match%maps.length]),dawnTier,duskTier,winner==ABORTED?"aborted":winner<0?"draw":SEATS[winner],
            String.valueOf(b.scores[0]),String.valueOf(b.scores[1]),String.valueOf((b.time-b.roundStarted)/20))+"\n");
        if(probe)probeRow(b);
    }
    /** The final line. Mean gold earned is a team's total over its commanders at the end of a round, averaged over the matches.
     *  " aborted=N" goes on the end only when a round stopped without Campaign.finish, and " kit=<id>" only under -PsimKit dawn or dusk, so a normal run keeps the documented format. */
    void summary(){
        System.out.println(String.format(Locale.ROOT,"[botsim] matches=%d dawn(%s)=%d dusk(%s)=%d draws=%d dawnWin=%.0f%% meanGoldEarnedAtEnd dawn=%.0f dusk=%.0f",
            matches,dawnTier,dawnWins,duskTier,duskWins,draws,100*MatchStats.winRate(dawnWins,matches),earnedAtEnd[0]/(double)matches,earnedAtEnd[1]/(double)matches)+(aborted>0?" aborted="+aborted:"")+(kit.equals("native")?"":" kit="+kit));
    }
    void probeStart(){tickCount=0;startedAt=LocalTime.now().format(CLOCK);System.out.println("[botsim] probe start match="+(match+1)+" map="+maps[match%maps.length]+" at="+startedAt);}
    static double pct(long[] sorted,double p){return sorted.length==0?0:sorted[Math.max(0,(int)Math.ceil(p*sorted.length)-1)]/1e6;}
    static String ms(double v){return String.format(Locale.ROOT,"%.2f",v);}
    static String mb(long bytes){return String.format(Locale.ROOT,"%.1f",bytes/1048576.0);}
    /** One row of server health at the end of a match, after the round's own cleanup (returnLobby discards the troops and arrows): the round's server tick bodies (START to END of MinecraftServer.tick),
     *  the entities still alive in the world by class, the battle's own collections, and the heap after a forced GC. A failure is printed, never thrown: the probe must not end the soak it measures. */
    void probeRow(GravesAndGlory.Battle b){
        try{
            int n=tickCount;long[] t=Arrays.copyOf(tickNanos,n);Arrays.sort(t);tickCount=0;
            Map<String,Integer> kinds=new TreeMap<>();int entities=0;
            for(var e:b.world.iterateEntities())if(!e.isRemoved()){entities++;kinds.merge(e.getClass().getSimpleName(),1,Integer::sum);}
            StringBuilder kindText=new StringBuilder();for(var k:kinds.entrySet())kindText.append(kindText.length()==0?"":";").append(k.getKey()).append('=').append(k.getValue());
            Runtime rt=Runtime.getRuntime();long before=rt.totalMemory()-rt.freeMemory();
            System.gc();
            long after=rt.totalMemory()-rt.freeMemory(),gcCount=0,gcMillis=0,direct=0;
            for(GarbageCollectorMXBean g:ManagementFactory.getGarbageCollectorMXBeans()){gcCount+=Math.max(0,g.getCollectionCount());gcMillis+=Math.max(0,g.getCollectionTime());}
            for(BufferPoolMXBean p:ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class))if(p.getName().equals("direct"))direct+=p.getMemoryUsed();
            var c=b.campaign;
            List<String> v=new ArrayList<>(List.of(String.valueOf(match+1),String.valueOf(maps[match%maps.length]),startedAt,LocalTime.now().format(CLOCK),String.valueOf(n),
                ms(n==0?0:Arrays.stream(t).average().orElse(0)/1e6),ms(pct(t,.50)),ms(pct(t,.95)),ms(pct(t,.99)),ms(n==0?0:t[n-1]/1e6),
                String.valueOf(Arrays.stream(t).filter(x->x>50_000_000L).count()),String.valueOf(Arrays.stream(t).filter(x->x>100_000_000L).count()),ms(b.server.getTickTime()),
                mb(before),mb(after),mb(rt.maxMemory()),String.valueOf(gcCount),String.valueOf(gcMillis),String.valueOf(ManagementFactory.getThreadMXBean().getThreadCount()),
                String.valueOf(ManagementFactory.getClassLoadingMXBean().getLoadedClassCount()),mb(direct),mb(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()),
                String.valueOf(b.world.getChunkManager().getLoadedChunkCount()),String.valueOf(entities),String.valueOf(b.units.size()),String.valueOf(b.shots.size()),
                String.valueOf(b.points.size()),String.valueOf(Spec.POINTS.length),String.valueOf(b.squads.size()),String.valueOf(b.respawns.size()),String.valueOf(b.pings.size()),
                String.valueOf(c.botRespawns.size()),String.valueOf(c.botRetreating.size()),String.valueOf(c.vacated.size()),kindText.toString()));
            append(probeCsv,String.join(",",v)+"\n");
            StringBuilder line=new StringBuilder("[botsim] probe");for(int i=0;i<PROBE_COLUMNS.length;i++)line.append(' ').append(PROBE_COLUMNS[i]).append('=').append(v.get(i));
            System.out.println(line);
        }catch(Throwable e){System.out.println("[botsim] probe failed: "+e);}
    }
    /** Recreates botsim/ if it vanished. A failed write is logged, not thrown, so the run still ends with its summary line. */
    static void append(Path file,String text){
        try{Files.createDirectories(file.getParent());Files.writeString(file,text,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(IOException e){System.out.println("[botsim] could not write "+file+": "+e);}
    }
}
