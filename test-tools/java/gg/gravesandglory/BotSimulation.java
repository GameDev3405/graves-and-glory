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
 *  -PsimKit=native (default) fields Dawn's seat with the gilded_<tier> bot banner and Dusk's with graveborn_<tier>; gilded or graveborn gives both seats that House's row (sides and bases stay), so House and seat can be told apart.
 *  -PsimBanner0 and -PsimBanner1 (botBanners ids, for example gilded_normal) field every bot of team 0 or team 1 with that exact banner, whatever the kit says; results.csv and the summary name the banners fielded.
 *  -PsimProgression=on|off overrides progression.enabled for the run (Veterans.override; absent: the sheet's value), the in-match progression gate's baseline switch.
 *  Writes botsim/results.csv (one row per match, with the no-routing design's fight stats and each team's score at 300 s), botsim/gold.csv (each team every 30 s of round
 *  time) and botsim/levels.csv (per match and team: when the first soldier reached level 2 and 3, the share of living soldiers at level 2 or higher at the end, the level-3
 *  count and every gear buy with its second) under the server root.
 *  Soak aid, off by default: GRAVES_BOTSIM_PROBE=1 in the environment (or -Dgravesandglory.simProbe=true) also writes botsim/probe.csv and prints one "[botsim] probe" line per match:
 *  server tick times over the round, entities and the battle's own collections after the round's cleanup, and heap after a forced GC. */
public final class BotSimulation implements ModInitializer {
    static final int SAMPLE_TICKS=600;
    /** results.csv's score300_0 and score300_1: each team's score at 300 s of round time (6000 battle ticks), or the final score when the round ended sooner. */
    static final int SCORE_TICKS=6000;
    /** How long an arena build may take (5 minutes) before the simulation gives up. */
    static final int READY_TICKS=6000;
    /** Campaign.lastWinner while a simulated round runs. Only Campaign.finish overwrites it, so a round that stops any other way is recorded as aborted. */
    static final int ABORTED=-2;
    int matches,size,roundSeconds,waited,notReady;int[] maps;String dawnTier,duskTier,kit,progression;Path results,gold,levels,types,meta;
    /** The balance method's per-type ledger (types.csv) and the wall clock of the match that runs (meta.txt), read-only on the game. */
    final TypeLedger ledger=new TypeLedger();long wallStart;
    /** This round's score at SCORE_TICKS per team, -1 until then. */
    final int[] score300={-1,-1};
    /** This round's progression tallies (Veterans.TALLY), the round's start and each team's share of living soldiers at level 2 or higher, as the last running tick saw them: the round's end clears the Armoury and puts fresh tallies in place before record runs. */
    final Veterans.Tally[] tallies={null,null};long startedAt;final double[] shareLevel2={-1,-1};
    /** The seats, for the CSV team columns: -PsimKit may give both teams one House. */
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
    boolean probe;Path probeCsv;long tickStart;long[] tickNanos=new long[8192];int tickCount;String probeStartedAt="";
    /** -PsimBanner0 and -PsimBanner1: the botBanners id each team must field, or null for the kit's banner. fielded holds the id each team fields this run, for the CSV and the summary. */
    final String[] bannerChoice={null,null},fielded={"",""};

    @Override public void onInitialize(){
        // Absent means a normal server. Given but blank or non-numeric is an error, not a silent plain server.
        String n=System.getProperty("gravesandglory.botSim");if(n==null)return;
        matches=number("botSim",n);
        dawnTier=tier(System.getProperty("gravesandglory.simDawn","normal"));duskTier=tier(System.getProperty("gravesandglory.simDusk","normal"));
        String mapList=System.getProperty("gravesandglory.simMaps");
        maps=mapList==null?IntStream.range(0,Spec.MAPS.length).toArray():Arrays.stream(mapList.split(",")).map(String::trim).filter(v->!v.isEmpty()).mapToInt(v->number("simMaps",v)).toArray();
        size=number("simSize",System.getProperty("gravesandglory.simSize","3"));roundSeconds=number("simRoundSeconds",System.getProperty("gravesandglory.simRoundSeconds","300"));
        kit=System.getProperty("gravesandglory.simKit","native").trim();
        if(!kit.equals("native")&&Arrays.stream(Spec.HOUSES).noneMatch(h->h.id().equals(kit)&&h.fieldable()))throw new IllegalArgumentException("[botsim] simKit must be native or a fieldable House ("+Arrays.stream(Spec.HOUSES).filter(h->h.fieldable()).map(h->h.id()).collect(java.util.stream.Collectors.joining(", "))+"), got '"+kit+"'");
        for(int team=0;team<2;team++){String id=System.getProperty("gravesandglory.simBanner"+team);if(id==null)continue;String row=id.trim();
            if(Arrays.stream(Spec.BOTBANNERS).noneMatch(r->r.id().equals(row)))throw new IllegalArgumentException("[botsim] simBanner"+team+" must be a botBanners id, got '"+id+"'");bannerChoice[team]=row;}
        String prog=System.getProperty("gravesandglory.simProgression");
        if(prog!=null){String v=prog.trim();if(v.equals("on"))Veterans.override=Boolean.TRUE;else if(v.equals("off"))Veterans.override=Boolean.FALSE;else throw new IllegalArgumentException("[botsim] simProgression must be on or off, got '"+prog+"'");}
        progression=Veterans.enabled()?"on":"off";
        if(matches<1||roundSeconds<1||size<1||size>Spec.RULES[0].playersPerTeam()||maps.length==0||Arrays.stream(maps).anyMatch(m->m<0||m>=Spec.MAPS.length))
            throw new IllegalArgumentException("[botsim] bad settings: matches="+matches+" size="+size+" roundSeconds="+roundSeconds+" maps="+Arrays.toString(maps));
        Path dir=FabricLoader.getInstance().getGameDir().resolve("botsim");results=dir.resolve("results.csv");gold=dir.resolve("gold.csv");levels=dir.resolve("levels.csv");types=dir.resolve("types.csv");meta=dir.resolve("meta.txt");
        SheetOverride.applyFromProperty(); // -PsimOverride: in-memory sheet changes for an ablation (balance method); none by default
        try{Files.createDirectories(dir);
            Files.writeString(results,"match,map,dawnTier,duskTier,winner,dawnScore,duskScore,seconds,banner0,banner1,dawnKills,duskKills,dawnWiped,duskWiped,dawnRetreats,duskRetreats,fights,meanFightSeconds,dawnFlankHits,duskFlankHits,dawnFlankShots,duskFlankShots,score300_0,score300_1\n",StandardCharsets.UTF_8);
            Files.writeString(gold,"match,second,team,goldEarned,goldBank,living\n",StandardCharsets.UTF_8);
            Files.writeString(levels,"match,team,banner,firstLevel2,firstLevel3,endShareLevel2,level3Count,gear\n",StandardCharsets.UTF_8);
            Files.writeString(types,"match,map,seat,banner,"+TypeLedger.HEADER+"\n",StandardCharsets.UTF_8);
            Files.writeString(meta,"override="+SheetOverride.describe()+"\nfast="+gg.gravesandglory.testing.SimSpeed.FAST+"\n",StandardCharsets.UTF_8);
        }catch(IOException e){throw new UncheckedIOException(e);}
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->ledger.onDeath(GravesAndGlory.battle,entity,source));
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
            if(b.running){scan(b);ledger.tick(b);progress(b);long elapsed=b.time-b.roundStarted;if(elapsed>0&&elapsed%SAMPLE_TICKS==0)sample(b,elapsed/20);
                if(elapsed>=SCORE_TICKS&&score300[0]<0){score300[0]=b.scores[0];score300[1]=b.scores[1];}return;}
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
        b.startRound();
        armies.clear();for(var s:b.squads.values())if(s.bot){if(s.team==1)s.tier=duskTier;armies.add(s);}
        field(b);seen.clear();resetStats();ledger.reset(b.time);wallStart=System.currentTimeMillis();
        score300[0]=score300[1]=-1;startedAt=b.roundStarted;tallies[0]=tallies[1]=null;shareLevel2[0]=shareLevel2[1]=-1;
        playing=true;if(probe)probeStart();
    }
    /** Phase 2: once each seat's tier is set, every bot army fields one bot banner row for its tier (Spec.BOTBANNERS), replacing the
     *  lane-picked banner startRound gave it: native gives Dawn's seat gilded_<tier> and Dusk's graveborn_<tier>; -PsimKit gilded or
     *  graveborn gives both seats that House's row; -PsimBanner0 or -PsimBanner1 overrides a whole team with one exact row.
     *  Sides, bases and barracks stay the seat's. */
    void field(GravesAndGlory.Battle b){
        for(var s:armies){String house=kit.equals("native")?(s.team==0?"gilded":"graveborn"):kit;
            b.units.removeIf(u->{if(u.squad!=s)return false;u.mob.discard();return true;});
            String id=bannerChoice[s.team]!=null?bannerChoice[s.team]:house+"_"+s.tier;fielded[s.team]=id;
            s.banner=Campaign.bannerRow(id);s.fullHealth.clear();b.reinforce(s);}
    }
    /** Where each troop stood at the last sample, for the stuck count (cleared at each round start). */
    final Map<UUID,net.minecraft.util.math.Vec3d> seen=new HashMap<>();
    /** The sample's health line on the console: living troops, how many are on fire, how many stood still for the whole sample (30 s) with no target and an order other than Hold (a troop that cannot walk, or one that burns, shows here), how many are posed as vanilla would have them (a wolf sitting, a wolf, boar-kin or bear holding anger: Task 10's mobs must never be) and how many bears rear (Old Horn's trample pulse alone, 20 ticks each). */
    void health(GravesAndGlory.Battle b,long second){
        int living=0,burning=0,stuck=0,posed=0,rearing=0;Map<UUID,net.minecraft.util.math.Vec3d> now=new HashMap<>();
        for(var u:b.units){if(!u.mob.isAlive())continue;living++;if(u.mob.isOnFire()||u.mob.getFireTicks()>0)burning++;
            var was=seen.get(u.mob.getUuid());if(was!=null&&u.mob.getTarget()==null&&u.mob.squaredDistanceTo(was)<1.0&&!b.assignment(u).order.equals("hold"))stuck++;
            if(u.mob instanceof net.minecraft.entity.passive.WolfEntity w&&(w.isSitting()||w.isInSittingPose())||u.mob instanceof net.minecraft.entity.mob.Angerable a&&(a.hasAngerTime()||a.getAngryAt()!=null))posed++;
            if(u.mob instanceof net.minecraft.entity.passive.PolarBearEntity pb&&pb.isWarning())rearing++;
            now.put(u.mob.getUuid(),u.mob.getPos());}
        seen.clear();seen.putAll(now);
        System.out.println("[botsim] health match="+(match+1)+" second="+second+" living="+living+" burning="+burning+" stuck="+stuck+" posed="+posed+" rearing="+rearing);
    }
    /** One gold.csv row per team: gold earned so far, gold in hand and living troops. */
    void sample(GravesAndGlory.Battle b,long second){
        health(b,second);
        StringBuilder rows=new StringBuilder();
        for(int team=0;team<2;team++){final int t=team;int earned=0,bank=0;for(var s:armies)if(s.team==t){earned+=s.stats.goldEarned;bank+=s.gold;}
            long living=b.units.stream().filter(u->u.squad.team==t&&u.mob.isAlive()).count();
            rows.append(match+1).append(',').append(second).append(',').append(SEATS[t]).append(',').append(earned).append(',').append(bank).append(',').append(living).append('\n');}
        append(gold,rows.toString());
    }
    /** The no-routing design's fight stats (its section 8), scanned every tick of a round. Kills per side are the other side's losses (Squad.stats.lost, a death by any cause);
     *  a group is wiped out when a bot army's group goes from living regular troops to none (temporaries do not count, and a group refilled from the barracks can be wiped again);
     *  a retreat is a bot army entering Campaign.botRetreating (the bot's own Retreat order); a fight opens at the first blow logged (Combat.LAST) while none is open and closes
     *  at the next wipe or retreat, or the round's end, so meanFightSeconds is the mean length from first contact to one side's group wiped or retreating;
     *  dawnFlankHits, duskFlankHits, dawnFlankShots and duskFlankShots are the C16 and C16s rows each side's blows and shots carried (MatchStats.SquadStats). */
    final Map<String,Integer> living=new HashMap<>();final Set<UUID> retreating=new HashSet<>();
    final int[] wiped={0,0},retreats={0,0};int fights;long fightTicks,fightOpenedAt=-1;Combat.HitLog lastBlow;
    void resetStats(){living.clear();retreating.clear();wiped[0]=wiped[1]=0;retreats[0]=retreats[1]=0;fights=0;fightTicks=0;fightOpenedAt=-1;lastBlow=Combat.LAST.peekLast();}
    void scan(GravesAndGlory.Battle b){
        boolean closes=false;Map<String,Integer> now=new HashMap<>();
        for(var u:b.units)if(u.mob.isAlive()&&!u.temporary)now.merge(u.squad.owner+"/"+u.spec.group(),1,Integer::sum);
        for(var s:armies)for(String g:Morale.GROUPS){String k=s.owner+"/"+g;if(living.getOrDefault(k,0)>0&&now.getOrDefault(k,0)==0){wiped[s.team]++;closes=true;}}
        living.clear();living.putAll(now);
        for(var s:armies){boolean r=b.campaign.botRetreating.contains(s.owner);if(r&&retreating.add(s.owner)){retreats[s.team]++;closes=true;}if(!r)retreating.remove(s.owner);}
        var blow=Combat.LAST.peekLast();if(fightOpenedAt<0&&blow!=lastBlow)fightOpenedAt=b.time;lastBlow=blow;
        if(closes&&fightOpenedAt>=0)closeFight(b);
    }
    void closeFight(GravesAndGlory.Battle b){fights++;fightTicks+=b.time-fightOpenedAt;fightOpenedAt=-1;}
    /** Each running tick: holds this round's tallies and each team's share of living regular soldiers (no temporaries) at level 2 or higher, for levels.csv. */
    void progress(GravesAndGlory.Battle b){
        int[] alive={0,0},veterans={0,0};
        for(var u:b.units)if(u.mob.isAlive()&&!u.temporary&&(u.squad.team==0||u.squad.team==1)){alive[u.squad.team]++;if(u.level>=2)veterans[u.squad.team]++;}
        for(int t=0;t<2;t++){tallies[t]=Veterans.TALLY[t];shareLevel2[t]=alive[t]==0?-1:veterans[t]/(double)alive[t];}
    }
    /** One levels.csv row per team: the round second the first soldier reached level 2 and level 3 (blank: never), the share at level 2 or higher at the end (blank: none living), the level-3 count, and the gear bought as troop:track:tier@second joined by ';'. */
    void recordLevels(){
        StringBuilder rows=new StringBuilder();
        for(int t=0;t<2;t++){var tally=tallies[t];
            String first2=tally==null||tally.firstAt[2]<0?"":String.valueOf((tally.firstAt[2]-startedAt)/20),first3=tally==null||tally.firstAt[3]<0?"":String.valueOf((tally.firstAt[3]-startedAt)/20);
            StringBuilder gear=new StringBuilder();if(tally!=null)for(var g:tally.bought){if(gear.length()>0)gear.append(';');gear.append(g.troop()).append(':').append(g.track()).append(':').append(g.tier()).append('@').append((g.time()-startedAt)/20);}
            rows.append(match+1).append(',').append(SEATS[t]).append(',').append(fielded[t]).append(',').append(first2).append(',').append(first3).append(',')
                .append(shareLevel2[t]<0?"":String.format(Locale.ROOT,"%.3f",shareLevel2[t])).append(',').append(tally==null?0:tally.level3s).append(',').append(gear).append('\n');}
        append(levels,rows.toString());
    }
    void record(GravesAndGlory.Battle b){
        int winner=b.campaign.lastWinner;
        if(winner==0)dawnWins++;else if(winner==1)duskWins++;else if(winner==ABORTED)aborted++;else draws++;
        for(var s:armies)earnedAtEnd[s.team]+=s.stats.goldEarned;
        if(fightOpenedAt>=0)closeFight(b);
        int[] kills={0,0},flankHits={0,0},flankShots={0,0};for(var s:armies){kills[1-s.team]+=s.stats.lost;flankHits[s.team]+=s.stats.flankHits;flankShots[s.team]+=s.stats.flankShots;} // the flank rows each side landed (MatchStats.SquadStats, counted in Combat.resolve)
        append(results,String.join(",",String.valueOf(match+1),String.valueOf(maps[match%maps.length]),dawnTier,duskTier,winner==ABORTED?"aborted":winner<0?"draw":SEATS[winner],
            String.valueOf(b.scores[0]),String.valueOf(b.scores[1]),String.valueOf((b.time-b.roundStarted)/20),fielded[0],fielded[1],
            String.valueOf(kills[0]),String.valueOf(kills[1]),String.valueOf(wiped[0]),String.valueOf(wiped[1]),String.valueOf(retreats[0]),String.valueOf(retreats[1]),String.valueOf(fights),fights==0?"":String.format(Locale.ROOT,"%.1f",fightTicks/20.0/fights),String.valueOf(flankHits[0]),String.valueOf(flankHits[1]),String.valueOf(flankShots[0]),String.valueOf(flankShots[1]),
            String.valueOf(score300[0]<0?b.scores[0]:score300[0]),String.valueOf(score300[1]<0?b.scores[1]:score300[1]))+"\n");
        if(probe)probeRow(b);
        recordLevels();
        ledger.stop();String at=(match+1)+","+maps[match%maps.length]+",";
        append(types,ledger.rows(0,at+"dawn,"+fielded[0])+ledger.rows(1,at+"dusk,"+fielded[1]));
        append(meta,String.format(Locale.ROOT,"match=%d wallSeconds=%.1f gameSeconds=%d fastApplied=%s%n",match+1,(System.currentTimeMillis()-wallStart)/1000.0,(b.time-b.roundStarted)/20,gg.gravesandglory.testing.SimSpeed.applied));
    }
    /** The final line. Mean gold earned is a team's total over its commanders at the end of a round, averaged over the matches.
     *  " aborted=N" goes on the end only when a round stopped without Campaign.finish, " kit=<id>" only under -PsimKit gilded or graveborn, then " banner0=<id> banner1=<id>": the banners the two teams fielded, and " progression=on|off" closes the line. */
    void summary(){
        System.out.println(String.format(Locale.ROOT,"[botsim] matches=%d dawn(%s)=%d dusk(%s)=%d draws=%d dawnWin=%.0f%% meanGoldEarnedAtEnd dawn=%.0f dusk=%.0f",
            matches,dawnTier,dawnWins,duskTier,duskWins,draws,100*MatchStats.winRate(dawnWins,matches),earnedAtEnd[0]/(double)matches,earnedAtEnd[1]/(double)matches)+(aborted>0?" aborted="+aborted:"")+(kit.equals("native")?"":" kit="+kit)+" banner0="+fielded[0]+" banner1="+fielded[1]+" progression="+progression);
    }
    void probeStart(){tickCount=0;probeStartedAt=LocalTime.now().format(CLOCK);System.out.println("[botsim] probe start match="+(match+1)+" map="+maps[match%maps.length]+" at="+probeStartedAt);}
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
            List<String> v=new ArrayList<>(List.of(String.valueOf(match+1),String.valueOf(maps[match%maps.length]),probeStartedAt,LocalTime.now().format(CLOCK),String.valueOf(n),
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
