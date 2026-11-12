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
 *  server tick times over the round, entities and the battle's own collections after the round's cleanup, and heap after a forced GC.
 *  AI quality (2026-11-10, read-only on the game): botsim/ai-metrics.csv (one row per bot army and per team, per match; AiMetrics) and botsim/decisions.csv (one row per bot commander decision, from Campaign.decisionListener); tools/bot_quality.py reads them.
 *  -PsimOverlayCheck=true also toggles the dev purpose overlay (AiPurposeOverlay) on the running round and asserts its labels and that troop names are untouched, into botsim/overlay-check.txt; it adds entities, so use it on its own run. */
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
        "heapMbBeforeGc","heapMbAfterGc","heapMaxMb","gcCount","gcMillis","threads","classes","directMb","nonHeapMb","loadedChunks","entities","units","shots","points","specPoints","squads","respawns","pings","botRespawns","botRetreating","vacated","committedMb","buildMs","buildTickCount","buildTickMeanMs","buildTickP95Ms","buildTickMaxMs","startRoundMs","fieldMs","unitsPeak","slowTicks","kinds"};
    static final DateTimeFormatter CLOCK=DateTimeFormatter.ofPattern("HH:mm:ss");
    /** Setup timing (probe only, test tooling): wall time from the map switch (or the first tick that saw an unbuilt arena) to ready, the tick bodies during that build, and startRound() and field() on the match that follows. */
    boolean building;long buildStartNs,buildNs=-1,startRoundNs,fieldNs;long[] buildTicks=new long[1024];int buildTickCount;
    int unitsPeak;
    /** AI quality metrics and the why log (AiMetrics); ownNanos is its time inside a tick, taken off the probe's tick times. */
    final AiMetrics metrics=new AiMetrics();
    boolean probe;Path probeCsv;long tickStart;long[] tickNanos=new long[8192];int tickCount;String probeStartedAt="";
    /** -PsimBanner0 and -PsimBanner1: the botBanners id each team must field, or null for the kit's banner. fielded holds the id each team fields this run, for the CSV and the summary. */
    final String[] bannerChoice={null,null},fielded={"",""};
    /** -PsimMode: the battle mode every round plays (default domination; the map list must host it). A mode other than Domination keeps its own clock unless -PsimRoundSeconds is given.
     *  Under siege, botsim/siege.csv gets one row per bot army and group every SIEGE_TICKS: where the group stands and what it was told, for the stage plan's report. */
    String simMode;boolean roundSecondsGiven;Path siegeCsv,convoyCsv,dominationCsv;
    /** Lobby ticks waited since the arena was ready (or the last round ended); a round starts at LOBBY_TICKS, the shipped ready-pad countdown. */
    int lobbyTicks;static final int LOBBY_TICKS=200;
    static final int SIEGE_TICKS=100;

    @Override public void onInitialize(){
        // Absent means a normal server. Given but blank or non-numeric is an error, not a silent plain server.
        String n=System.getProperty("gravesandglory.botSim");if(n==null)return;
        matches=number("botSim",n);
        dawnTier=tier(System.getProperty("gravesandglory.simDawn","normal"));duskTier=tier(System.getProperty("gravesandglory.simDusk","normal"));
        String mapList=System.getProperty("gravesandglory.simMaps");
        maps=mapList==null?IntStream.range(0,Spec.MAPS.length).toArray():Arrays.stream(mapList.split(",")).map(String::trim).filter(v->!v.isEmpty()).mapToInt(v->number("simMaps",v)).toArray();
        simMode=System.getProperty("gravesandglory.simMode",Modes.DOMINATION).trim();roundSecondsGiven=System.getProperty("gravesandglory.simRoundSeconds")!=null;
        if(!Modes.registered(simMode))throw new IllegalArgumentException("[botsim] simMode must be a registered mode, got '"+simMode+"'");
        for(int m:maps)if(!Modes.hosts(simMode,m))throw new IllegalArgumentException("[botsim] map "+m+" does not host "+simMode);
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
            Files.writeString(results,"match,map,dawnTier,duskTier,winner,dawnScore,duskScore,seconds,banner0,banner1,dawnKills,duskKills,dawnWiped,duskWiped,dawnRetreats,duskRetreats,fights,meanFightSeconds,dawnFlankHits,duskFlankHits,dawnFlankShots,duskFlankShots,score300_0,score300_1,mode,stage,gateBlows,gateDamage,yardSecond,gateSecond,keepSecond,checkpoints,progress,idleMax\n",StandardCharsets.UTF_8);
            if(simMode.equals(Modes.SIEGE)){siegeCsv=dir.resolve("siege.csv");Files.writeString(siegeCsv,"match,second,stage,gateHp,army,team,group,living,x,y,z,order,targetX,targetY,targetZ,enemy\n",StandardCharsets.UTF_8);}
            if(simMode.equals(Modes.DOMINATION)){dominationCsv=dir.resolve("domination.csv");Files.writeString(dominationCsv,"match,map,second,score0,score1,owners,army,team,group,living,x,z,order,targetX,targetZ,retreating,nearestFoe\n",StandardCharsets.UTF_8);Files.writeString(dir.resolve("domination-units.csv"),"match,second,army,group,slot,x,y,z,age,ticking,target,targetDist,navIdle,stuck,slotX,slotZ\n",StandardCharsets.UTF_8);}
            if(simMode.equals(Modes.CONVOY)){convoyCsv=dir.resolve("convoy.csv");Files.writeString(convoyCsv,"match,second,state,distance,pushers,defenders,army,team,group,living,x,z,wagonDist,order,targetX,targetZ,retreating\n",StandardCharsets.UTF_8);}
            Files.writeString(gold,"match,second,team,goldEarned,goldBank,living\n",StandardCharsets.UTF_8);
            Files.writeString(levels,"match,team,banner,firstLevel2,firstLevel3,endShareLevel2,level3Count,gear\n",StandardCharsets.UTF_8);
            Files.writeString(types,"match,map,seat,banner,"+TypeLedger.HEADER+"\n",StandardCharsets.UTF_8);
            Files.writeString(meta,"override="+SheetOverride.describe()+"\nfast="+gg.gravesandglory.testing.SimSpeed.FAST+"\n",StandardCharsets.UTF_8);
        }catch(IOException e){throw new UncheckedIOException(e);}
        metrics.open(dir);
        Campaign.decisionListener=(c,sq,tr,ce,lv,fu,se,en,cc,ch,ho,re,wo,wt)->{try{if(playing)metrics.decision(c,sq,tr,ce,lv,fu,se,en,cc,ch,ho,re,wo,wt);}catch(RuntimeException e){System.out.println("[botsim] decision log failed: "+e);e.printStackTrace(System.out);}};
        overlayCheck=Boolean.getBoolean("gravesandglory.simOverlayCheck");
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->ledger.onDeath(GravesAndGlory.battle,entity,source));
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        probe="1".equals(System.getenv("GRAVES_BOTSIM_PROBE"))||Boolean.getBoolean("gravesandglory.simProbe");
        if(probe){
            probeCsv=dir.resolve("probe.csv");
            try{Files.writeString(probeCsv,String.join(",",PROBE_COLUMNS)+"\n",StandardCharsets.UTF_8);}catch(IOException e){throw new UncheckedIOException(e);}
            // Registered after the harness's own listener, so the tick that ends a round is not timed as part of it.
            ServerTickEvents.START_SERVER_TICK.register(s->tickStart=System.nanoTime());
            ServerTickEvents.END_SERVER_TICK.register(s->{if(building){if(buildTickCount==buildTicks.length)buildTicks=Arrays.copyOf(buildTicks,buildTickCount*2);buildTicks[buildTickCount++]=System.nanoTime()-tickStart;}if(playing){if(tickCount==tickNanos.length)tickNanos=Arrays.copyOf(tickNanos,tickCount*2);tickNanos[tickCount++]=System.nanoTime()-tickStart-metrics.ownNanos;var bt=GravesAndGlory.battle;if(bt!=null)unitsPeak=Math.max(unitsPeak,bt.units.size());}metrics.ownNanos=0;});
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
            if(b.running){scan(b);ledger.tick(b);progress(b);long elapsed=b.time-b.roundStarted;metrics.tick(b);if(elapsed>0&&elapsed%20==0)metrics.second(b);if(overlayCheck)overlayCheck(b,elapsed);if(elapsed>0&&elapsed%SAMPLE_TICKS==0)sample(b,elapsed/20);
                if(siegeCsv!=null&&elapsed>0&&elapsed%SIEGE_TICKS==0)siegeSample(b,elapsed/20);
                if(convoyCsv!=null&&elapsed>0&&elapsed%SIEGE_TICKS==0)convoyTrace(b,elapsed/20);
                if(dominationCsv!=null&&elapsed>0&&elapsed%SIEGE_TICKS==0)dominationTrace(b,elapsed/20);
                if(elapsed>0&&elapsed%20==0)convoySample(b);
                if(elapsed>=SCORE_TICKS&&score300[0]<0){score300[0]=b.scores[0];score300[1]=b.scores[1];}return;}
            playing=false;record(b);
            if(++match>=matches){summary();done=true;server.stop(false);}
            return;
        }
        // A fresh world or a map switch: wait until the arena is built, but not forever.
        if(!b.ready){if(probe&&!building){building=true;buildStartNs=System.nanoTime();buildTickCount=0;}if(++notReady==READY_TICKS){System.out.println("[botsim] arena not ready after "+READY_TICKS+" ticks; stopping");done=true;server.stop(false);}return;}
        notReady=0;if(probe&&building){building=false;buildNs=System.nanoTime()-buildStartNs;}if(b.running)return;
        int map=maps[match%maps.length];
        if(b.campaign.map!=map){lobbyTicks=0;if(probe){building=true;buildStartNs=System.nanoTime();buildTickCount=0;}b.campaign.switchMap(map);return;}
        // A real round starts from the camp some time after the arena is ready (a room's start, a practice press); starting in the ready tick put
        // the build's last step (landmarks, overlay, war table, mapReady) inside the round's tick 0 (docs/reports/perf-targets-2026-11-02.md, tick 0 fix).
        if(++lobbyTicks<LOBBY_TICKS)return;lobbyTicks=0;
        var c=b.campaign;c.size=size;c.fill=true;c.practice=false;c.difficulty=dawnTier;c.lastWinner=ABORTED;c.battleMode=simMode;
        long t0=System.nanoTime();b.startRound();startRoundNs=System.nanoTime()-t0;if(roundSecondsGiven||simMode.equals(Modes.DOMINATION))b.roundSeconds=roundSeconds; // after startRound, which sets the row's clock
        armies.clear();for(var s:b.squads.values())if(s.bot){if(s.team==1)s.tier=duskTier;armies.add(s);}
        t0=System.nanoTime();field(b);fieldNs=System.nanoTime()-t0;seen.clear();resetStats();ledger.reset(b.time);metrics.reset(armies,match,simMode);wallStart=System.currentTimeMillis();
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
            s.banner=Campaign.table(Campaign.bannerRow(id));s.fullHealth.clear();b.reinforce(s);}
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
    void resetStats(){stallRun=stallMax=0;living.clear();retreating.clear();wiped[0]=wiped[1]=0;retreats[0]=retreats[1]=0;fights=0;fightTicks=0;fightOpenedAt=-1;lastBlow=Combat.LAST.peekLast();}
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
            String.valueOf(score300[0]<0?b.scores[0]:score300[0]),String.valueOf(score300[1]<0?b.scores[1]:score300[1]),b.mode.row.id(),siegeColumns(b),convoyColumns(b))+"\n");
        String tickMean="",tickP99="";
        if(probe&&tickCount>0){long[] t=Arrays.copyOf(tickNanos,tickCount);Arrays.sort(t);tickMean=ms(Arrays.stream(t).average().orElse(0)/1e6);tickP99=ms(pct(t,.99));}
        metrics.write(String.valueOf(maps[match%maps.length]),fielded,tickMean,tickP99);
        if(probe)probeRow(b);
        recordLevels();
        ledger.stop();String at=(match+1)+","+maps[match%maps.length]+",";
        append(types,ledger.rows(0,at+"dawn,"+fielded[0])+ledger.rows(1,at+"dusk,"+fielded[1]));
        append(meta,String.format(Locale.ROOT,"match=%d wallSeconds=%.1f gameSeconds=%d fastApplied=%s%n",match+1,(System.currentTimeMillis()-wallStart)/1000.0,(b.time-b.roundStarted)/20,gg.gravesandglory.testing.SimSpeed.applied));
    }
    /** results.csv's Siege columns: the final stage, the gate's blows and damage, and the round second the yard, the gate and the keep fell (blank: never, or not a siege). */
    static String siegeColumns(GravesAndGlory.Battle b){
        if(!(b.mode instanceof SiegeMode m))return ",,,,,";
        return String.join(",",String.valueOf(m.stage),String.valueOf(m.gateBlows),String.format(Locale.ROOT,"%.0f",m.gateDamage),m.yardSecond<0?"":String.valueOf(m.yardSecond),m.gateSecond<0?"":String.valueOf(m.gateSecond),m.keepSecond<0?"":String.valueOf(m.keepSecond));
    }
    /** Convoy: the longest run, in seconds, the wagon sat Idle or Rolling back while the escort still had two or more groups alive (the spec's 60 s ceiling). */
    int stallRun,stallMax;
    void convoySample(GravesAndGlory.Battle b){
        if(!(b.mode instanceof ConvoyMode m))return;
        var groups=new HashSet<String>();for(var u:b.units)if(u.mob.isAlive()&&!u.temporary&&u.squad.team==ConvoyMode.ESCORT)groups.add(u.spec.group());
        boolean stalled=(m.state.equals(ModeMath.IDLE)||m.state.equals(ModeMath.ROLLBACK))&&groups.size()>=2;
        stallRun=stalled?stallRun+1:0;stallMax=Math.max(stallMax,stallRun);
    }
    /** results.csv's Convoy columns: the checkpoints the wagon passed (the destination not counted), the road covered in percent and the longest stall in seconds (blank: not a convoy). */
    String convoyColumns(GravesAndGlory.Battle b){
        if(!(b.mode instanceof ConvoyMode m))return ",,";
        return String.join(",",String.valueOf(m.checkpoints()),String.valueOf(ModeMath.percent(m.distance,m.total())),String.valueOf(stallMax));
    }
    /** AI audit trace (2026-11-09, harness only): under domination, every SIEGE_TICKS one domination.csv row per bot army and group (centre, order, target, retreat, nearest living foe) and every 10 s one domination-units.csv row per living troop, as convoy-units.csv. */
    void dominationTrace(GravesAndGlory.Battle b,long second){
        StringBuilder owners=new StringBuilder();for(var p:b.points)owners.append(p.owner<0?"-":p.owner==0?"D":"K");
        StringBuilder rows=new StringBuilder();
        for(int i=0;i<armies.size();i++){var s=armies.get(i);
            for(var group:Spec.GROUPS){String g=group.id();if(g.equals("all"))continue;
                var mine=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()&&u.spec.group().equals(g)).toList();if(mine.isEmpty())continue;
                var c=Morale.centre(mine);var a=b.storedAssignment(s,g);double foe=Double.MAX_VALUE;
                for(var u:b.units)if(u.squad.team!=s.team&&u.mob.isAlive())foe=Math.min(foe,Math.hypot(u.mob.getX()-c.x,u.mob.getZ()-c.z));
                rows.append(String.format(Locale.ROOT,"%d,%d,%d,%d,%d,%s,%d,%s,%s,%d,%.1f,%.1f,%s,%.1f,%.1f,%s,%.1f%n",match+1,b.campaign.map,second,b.scores[0],b.scores[1],owners,i,SEATS[s.team],g,mine.size(),c.x,c.z,a.order,
                    a.target==null?0:a.target.x,a.target==null?0:a.target.z,b.campaign.botRetreating.contains(s.owner),foe==Double.MAX_VALUE?-1:foe));}}
        append(dominationCsv,rows.toString());
        if(second%10!=0)return;StringBuilder units=new StringBuilder();
        for(var u:b.units)if(u.mob.isAlive()&&armies.indexOf(u.squad)>=0){var f=b.formation(u);var t=u.lockedTarget;
            units.append(String.format(Locale.ROOT,"%d,%d,%d,%s,%d,%.1f,%.1f,%.1f,%d,%s,%s,%.1f,%s,%d,%.1f,%.1f%n",match+1,second,armies.indexOf(u.squad),u.spec.group(),u.slot,u.mob.getX(),u.mob.getY(),u.mob.getZ(),u.mob.age,
                b.world.shouldTickEntity(u.mob.getBlockPos()),t==null?"-":t.getClass().getSimpleName(),t==null?0:Math.sqrt(u.mob.squaredDistanceTo(t)),u.mob.getNavigation().isIdle(),u.stuckTicks,f.x,f.z));}
        append(dominationCsv.resolveSibling("domination-units.csv"),units.toString());
    }
    /** One convoy.csv row per bot army and group with living troops, every SIEGE_TICKS: the wagon's state, road distance and counts, the group's centre, its distance to the wagon, its order and whether the army is retreating. */
    void convoyTrace(GravesAndGlory.Battle b,long second){
        if(!(b.mode instanceof ConvoyMode m)||m.wagon==null)return;
        var w=m.wagon.getPos();StringBuilder rows=new StringBuilder();
        for(int i=0;i<armies.size();i++){var s=armies.get(i);
            for(var group:Spec.GROUPS){String g=group.id();if(g.equals("all"))continue;
                var mine=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()&&u.spec.group().equals(g)).toList();if(mine.isEmpty())continue;
                var c=Morale.centre(mine);var a=b.storedAssignment(s,g);
                rows.append(String.format(Locale.ROOT,"%d,%d,%s,%.1f,%d,%d,%d,%s,%s,%d,%.1f,%.1f,%.1f,%s,%.1f,%.1f,%s%n",match+1,second,m.state,m.distance,m.pushers,m.defenders,i,SEATS[s.team],g,mine.size(),c.x,c.z,Math.hypot(c.x-w.x,c.z-w.z),a.order,
                    a.target==null?0:a.target.x,a.target==null?0:a.target.z,b.campaign.botRetreating.contains(s.owner)));}}
        append(convoyCsv,rows.toString());
        if(second%10!=0)return;StringBuilder units=new StringBuilder();
        for(var u:b.units)if(u.mob.isAlive()&&armies.indexOf(u.squad)>=0){var f=b.formation(u);var t=u.lockedTarget;
            units.append(String.format(Locale.ROOT,"%d,%d,%d,%s,%d,%.1f,%.1f,%.1f,%d,%s,%s,%.1f,%s,%d,%.1f,%.1f%n",match+1,second,armies.indexOf(u.squad),u.spec.group(),u.slot,u.mob.getX(),u.mob.getY(),u.mob.getZ(),u.mob.age,
                b.world.shouldTickEntity(u.mob.getBlockPos()),t==null?"-":t.getClass().getSimpleName(),t==null?0:Math.sqrt(u.mob.squaredDistanceTo(t)),u.mob.getNavigation().isIdle(),u.stuckTicks,f.x,f.z));}
        append(convoyCsv.resolveSibling("convoy-units.csv"),units.toString());
    }
    /** One siege.csv row per bot army and group with living troops: the stage, the gate's health (blank while it is not up), the group's centre and its order. */
    void siegeSample(GravesAndGlory.Battle b,long second){
        if(!(b.mode instanceof SiegeMode m))return;
        StringBuilder rows=new StringBuilder();
        for(int i=0;i<armies.size();i++){var s=armies.get(i);
            for(var group:Spec.GROUPS){String g=group.id();if(g.equals("all"))continue;
                var mine=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()&&u.spec.group().equals(g)).toList();if(mine.isEmpty())continue;
                var c=Morale.centre(mine);var a=b.storedAssignment(s,g);
                rows.append(String.format(Locale.ROOT,"%d,%d,%d,%s,%d,%s,%s,%d,%.1f,%.1f,%.1f,%s,%.1f,%.1f,%.1f,%s%n",match+1,second,m.stage,m.target==null?"":String.format(Locale.ROOT,"%.0f",m.target.hp()),i,SEATS[s.team],g,mine.size(),c.x,c.y,c.z,a.order,
                    a.target==null?0:a.target.x,a.target==null?0:a.target.y,a.target==null?0:a.target.z,a.enemy>=0?"gate":""));}}
        append(siegeCsv,rows.toString());
    }
    /** -PsimOverlayCheck: the dev purpose overlay on a running round (AiPurposeOverlay). At 120 s of round time it is switched on and every living troop must have one label in the documented form and its name and
     *  name visibility must be untouched; at 125 s the labels must have been refreshed about every 10 ticks and stand at their troops; at 130 s it is switched off, and 1 s later no label entity may be left,
     *  no refresh may have run since, and every surviving troop's name (as JSON) and visibility must equal what they were before it was on. Results: botsim/overlay-check.txt. */
    boolean overlayCheck;int overlayStage;final Map<GravesAndGlory.Unit,String> overlayNames=new IdentityHashMap<>();final Map<GravesAndGlory.Unit,Integer> overlayLevels=new IdentityHashMap<>();long refreshesAtOn,refreshesAtOff;final List<String> overlayLines=new ArrayList<>();boolean overlayFailed;
    static String nameOf(GravesAndGlory.Unit u){var n=u.mob.getCustomName();return (n==null?"<none>":net.minecraft.text.Text.Serializer.toJson(n))+"|visible="+u.mob.isCustomNameVisible();}
    void overlayAssert(String what,boolean ok,String detail){overlayLines.add((ok?"PASS ":"FAIL ")+what+(detail.isEmpty()?"":": "+detail));if(!ok)overlayFailed=true;}
    void overlayCheck(GravesAndGlory.Battle b,long elapsed){
        java.util.regex.Pattern form=java.util.regex.Pattern.compile("^\\S+ \\S+ \\| (marching|fighting|holding|idle|stuck|retreating|refill-walking) \\| .+$");
        if(overlayStage==0&&elapsed>=2400){
            overlayStage=1;overlayNames.clear();overlayLevels.clear();List<GravesAndGlory.Unit> living=new ArrayList<>();
            for(var u:b.units)if(u.mob.isAlive()&&!u.temporary){living.add(u);overlayNames.put(u,nameOf(u));overlayLevels.put(u,u.level);}
            overlayAssert("off by default",!AiPurposeOverlay.on&&AiPurposeOverlay.labels.isEmpty(),"on="+AiPurposeOverlay.on+" labels="+AiPurposeOverlay.labels.size());
            AiPurposeOverlay.set(true);refreshesAtOn=AiPurposeOverlay.refreshes;
            int bad=0,missing=0;String example="";
            for(var u:living){var stand=AiPurposeOverlay.labels.get(u);if(stand==null){missing++;continue;}var text=stand.getCustomName()==null?"":stand.getCustomName().getString();
                if(!form.matcher(text).matches()||!stand.isCustomNameVisible()||!stand.getCommandTags().contains(AiPurposeOverlay.TAG)||!stand.isInvisible()){bad++;example=text;}else if(example.isEmpty())example=text;}
            overlayAssert("every living troop has a label in the form '<group> <order> | <state> | <target or spot>'",missing==0&&bad==0&&!living.isEmpty(),living.size()+" troops, "+missing+" without label, "+bad+" malformed, e.g. \""+example+"\"");
            int changed=0;for(var u:living)if(!overlayNames.get(u).equals(nameOf(u)))changed++;
            overlayAssert("troop names and visibility untouched while on",changed==0,changed+" changed");
        }else if(overlayStage==1&&elapsed>=2500){
            overlayStage=2;long runs=AiPurposeOverlay.refreshes-refreshesAtOn;
            overlayAssert("refreshed about every 10 ticks while on",runs>=9&&runs<=11,runs+" refreshes in "+(elapsed-2400)+" ticks");
            int far=0,total=0;double worst=0;for(var e:AiPurposeOverlay.labels.entrySet()){total++;double d=Math.sqrt(e.getValue().squaredDistanceTo(e.getKey().mob));worst=Math.max(worst,d);if(d>6)far++;}
            overlayAssert("labels follow their troops (a label lags its troop by at most one 10-tick refresh)",total>0&&far==0,total+" labels, "+far+" more than 6 blocks from their troop, worst "+String.format(Locale.ROOT,"%.1f",worst));
            int changed=0;for(var e:overlayNames.entrySet())if(e.getKey().mob.isAlive()&&e.getKey().level==overlayLevels.get(e.getKey())&&!e.getValue().equals(nameOf(e.getKey())))changed++;
            overlayAssert("troop names and visibility untouched after refreshes",changed==0,changed+" changed");
            overlayAssert("states seen "+AiPurposeOverlay.statesSeen,!AiPurposeOverlay.statesSeen.isEmpty(),"");
        }else if(overlayStage==2&&elapsed>=2600){
            overlayStage=3;AiPurposeOverlay.set(false);refreshesAtOff=AiPurposeOverlay.refreshes;
            overlayAssert("off: label table empty",AiPurposeOverlay.labels.isEmpty(),AiPurposeOverlay.labels.size()+" left");
        }else if(overlayStage==3&&elapsed>=2620){
            overlayStage=4;int stands=0;for(var e:b.world.iterateEntities())if(e.getCommandTags().contains(AiPurposeOverlay.TAG)&&!e.isRemoved())stands++;
            overlayAssert("off: no label entity left in the world",stands==0,stands+" left");
            overlayAssert("off: no refresh while off",AiPurposeOverlay.refreshes==refreshesAtOff,(AiPurposeOverlay.refreshes-refreshesAtOff)+" ran");
            int changed=0,kept=0;int levelled=0;for(var e:overlayNames.entrySet())if(e.getKey().mob.isAlive()){if(e.getKey().level!=overlayLevels.get(e.getKey())){levelled++;continue;}kept++;if(!e.getValue().equals(nameOf(e.getKey())))changed++;}
            overlayAssert("off: surviving troops' names and visibility exactly as before",changed==0&&kept>0,kept+" troops compared, "+changed+" differ ("+levelled+" skipped: levelled up meanwhile, which the game renames itself)");
            overlayLines.add(overlayFailed?"OVERLAY CHECK FAIL":"OVERLAY CHECK PASS");
            try{Files.writeString(results.resolveSibling("overlay-check.txt"),String.join("\n",overlayLines)+"\n",StandardCharsets.UTF_8);}catch(IOException e){System.out.println("[botsim] could not write overlay-check.txt: "+e);}
            System.out.println("[botsim] "+overlayLines.get(overlayLines.size()-1));
        }
    }
    /** The final line. Mean gold earned is a team's total over its commanders at the end of a round, averaged over the matches.
     *  " aborted=N" goes on the end only when a round stopped without Campaign.finish, " kit=<id>" only under -PsimKit gilded or graveborn, then " banner0=<id> banner1=<id>": the banners the two teams fielded, and " progression=on|off" closes the line. */
    void summary(){
        System.out.println(String.format(Locale.ROOT,"[botsim] matches=%d dawn(%s)=%d dusk(%s)=%d draws=%d dawnWin=%.0f%% meanGoldEarnedAtEnd dawn=%.0f dusk=%.0f",
            matches,dawnTier,dawnWins,duskTier,duskWins,draws,100*MatchStats.winRate(dawnWins,matches),earnedAtEnd[0]/(double)matches,earnedAtEnd[1]/(double)matches)+(aborted>0?" aborted="+aborted:"")+(kit.equals("native")?"":" kit="+kit)+" banner0="+fielded[0]+" banner1="+fielded[1]+" progression="+progression);
    }
    void probeStart(){tickCount=0;unitsPeak=0;probeStartedAt=LocalTime.now().format(CLOCK);System.out.println("[botsim] probe start match="+(match+1)+" map="+maps[match%maps.length]+" at="+probeStartedAt);}
    /** Committed virtual memory of this server process (Windows: its commit charge), or blank when the JVM cannot say. */
    static String committedMb(){try{if(ManagementFactory.getOperatingSystemMXBean() instanceof com.sun.management.OperatingSystemMXBean o)return mb(o.getCommittedVirtualMemorySize());}catch(Throwable e){}return "";}
    /** The tick bodies timed while the arena was building, for the row of the match that follows it: 0 mean, 1 p95, 2 max, in ms; blank when no build was timed. */
    String buildTickStat(int kind){
        if(buildNs<0||buildTickCount==0)return "";
        long[] t=Arrays.copyOf(buildTicks,buildTickCount);Arrays.sort(t);
        return ms(kind==0?Arrays.stream(t).average().orElse(0)/1e6:kind==1?pct(t,.95):t[t.length-1]/1e6);
    }
    static double pct(long[] sorted,double p){return sorted.length==0?0:sorted[Math.max(0,(int)Math.ceil(p*sorted.length)-1)]/1e6;}
    static String ms(double v){return String.format(Locale.ROOT,"%.2f",v);}
    static String mb(long bytes){return String.format(Locale.ROOT,"%.1f",bytes/1048576.0);}
    /** One row of server health at the end of a match, after the round's own cleanup (returnLobby discards the troops and arrows): the round's server tick bodies (START to END of MinecraftServer.tick),
     *  the entities still alive in the world by class, the battle's own collections, and the heap after a forced GC. A failure is printed, never thrown: the probe must not end the soak it measures. */
    void probeRow(GravesAndGlory.Battle b){
        try{
            int n=tickCount;long[] t=Arrays.copyOf(tickNanos,n);StringBuilder slow=new StringBuilder();for(int i=0;i<n;i++)if(t[i]>50_000_000L)slow.append(slow.length()==0?"":";").append(i).append('@').append(Math.round(t[i]/1e6));Arrays.sort(t);tickCount=0;
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
                String.valueOf(c.botRespawns.size()),String.valueOf(c.botRetreating.size()),String.valueOf(c.vacated.size()),committedMb(),
                buildNs<0?"":ms(buildNs/1e6),buildNs<0?"":String.valueOf(buildTickCount),buildTickStat(0),buildTickStat(1),buildTickStat(2),ms(startRoundNs/1e6),ms(fieldNs/1e6),String.valueOf(unitsPeak),slow.toString(),kindText.toString()));
            buildNs=-1;
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
