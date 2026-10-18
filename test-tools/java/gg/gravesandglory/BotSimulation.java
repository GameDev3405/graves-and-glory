package gg.gravesandglory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.IntStream;

/** Unshipped: runServer -PbotSim=<matches> plays bot-vs-bot rounds on a dedicated server with nobody online and stops the server when done.
 *  One match at a time (one Battle per server). Dawn's bots play -PsimDawn, Dusk's -PsimDusk (Spec.BOTCOMMAND ids); -PsimMaps is cycled.
 *  Writes botsim/results.csv (one row per match) and botsim/gold.csv (each team every 30 s of round time) under the server root. */
public final class BotSimulation implements ModInitializer {
    static final int SAMPLE_TICKS=600;
    /** How long an arena build may take (5 minutes) before the simulation gives up. */
    static final int READY_TICKS=6000;
    /** Campaign.lastWinner while a simulated round runs. Only Campaign.finish overwrites it, so a round that stops any other way is recorded as aborted. */
    static final int ABORTED=-2;
    int matches,size,roundSeconds,waited,notReady;int[] maps;String dawnTier,duskTier;Path results,gold;
    /** The match being prepared or played (0-based); playing while its round runs. */
    int match;boolean playing,done;
    /** This round's bot armies. They leave Battle.squads when the round ends, so the end-of-round tally is read from here. */
    final List<GravesAndGlory.Squad> armies=new ArrayList<>();
    int dawnWins,duskWins,draws,aborted;final long[] earnedAtEnd={0,0};

    @Override public void onInitialize(){
        // Absent means a normal server. Given but blank or non-numeric is an error, not a silent plain server.
        String n=System.getProperty("gravesandglory.botSim");if(n==null)return;
        matches=number("botSim",n);
        dawnTier=tier(System.getProperty("gravesandglory.simDawn","normal"));duskTier=tier(System.getProperty("gravesandglory.simDusk","normal"));
        String mapList=System.getProperty("gravesandglory.simMaps");
        maps=mapList==null?IntStream.range(0,Spec.MAPS.length).toArray():Arrays.stream(mapList.split(",")).map(String::trim).filter(v->!v.isEmpty()).mapToInt(v->number("simMaps",v)).toArray();
        size=number("simSize",System.getProperty("gravesandglory.simSize","3"));roundSeconds=number("simRoundSeconds",System.getProperty("gravesandglory.simRoundSeconds","300"));
        if(matches<1||roundSeconds<1||size<1||size>Spec.RULES[0].playersPerTeam()||maps.length==0||Arrays.stream(maps).anyMatch(m->m<0||m>=Spec.MAPS.length))
            throw new IllegalArgumentException("[botsim] bad settings: matches="+matches+" size="+size+" roundSeconds="+roundSeconds+" maps="+Arrays.toString(maps));
        Path dir=FabricLoader.getInstance().getGameDir().resolve("botsim");results=dir.resolve("results.csv");gold=dir.resolve("gold.csv");
        try{Files.createDirectories(dir);
            Files.writeString(results,"match,map,dawnTier,duskTier,winner,dawnScore,duskScore,seconds\n",StandardCharsets.UTF_8);
            Files.writeString(gold,"match,second,team,goldEarned,goldBank,living\n",StandardCharsets.UTF_8);
        }catch(IOException e){throw new UncheckedIOException(e);}
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
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
        b.startRound();
        armies.clear();for(var s:b.squads.values())if(s.bot){if(s.team==1)s.tier=duskTier;armies.add(s);}
        playing=true;
    }
    /** One gold.csv row per team: gold earned so far, gold in hand and living troops. */
    void sample(GravesAndGlory.Battle b,long second){
        StringBuilder rows=new StringBuilder();
        for(int team=0;team<2;team++){final int t=team;int earned=0,bank=0;for(var s:armies)if(s.team==t){earned+=s.stats.goldEarned;bank+=s.gold;}
            long living=b.units.stream().filter(u->u.squad.team==t&&u.mob.isAlive()).count();
            rows.append(match+1).append(',').append(second).append(',').append(Spec.FACTIONS[t].id()).append(',').append(earned).append(',').append(bank).append(',').append(living).append('\n');}
        append(gold,rows.toString());
    }
    void record(GravesAndGlory.Battle b){
        int winner=b.campaign.lastWinner;
        if(winner==0)dawnWins++;else if(winner==1)duskWins++;else if(winner==ABORTED)aborted++;else draws++;
        for(var s:armies)earnedAtEnd[s.team]+=s.stats.goldEarned;
        append(results,String.join(",",String.valueOf(match+1),String.valueOf(maps[match%maps.length]),dawnTier,duskTier,winner==ABORTED?"aborted":winner<0?"draw":Spec.FACTIONS[winner].id(),
            String.valueOf(b.scores[0]),String.valueOf(b.scores[1]),String.valueOf((b.time-b.roundStarted)/20))+"\n");
    }
    /** The final line. Mean gold earned is a team's total over its commanders at the end of a round, averaged over the matches.
     *  " aborted=N" goes on the end only when a round stopped without Campaign.finish, so a normal run keeps the documented format. */
    void summary(){
        System.out.println(String.format(Locale.ROOT,"[botsim] matches=%d dawn(%s)=%d dusk(%s)=%d draws=%d dawnWin=%.0f%% meanGoldEarnedAtEnd dawn=%.0f dusk=%.0f",
            matches,dawnTier,dawnWins,duskTier,duskWins,draws,100*MatchStats.winRate(dawnWins,matches),earnedAtEnd[0]/(double)matches,earnedAtEnd[1]/(double)matches)+(aborted>0?" aborted="+aborted:""));
    }
    /** Recreates botsim/ if it vanished. A failed write is logged, not thrown, so the run still ends with its summary line. */
    static void append(Path file,String text){
        try{Files.createDirectories(file.getParent());Files.writeString(file,text,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(IOException e){System.out.println("[botsim] could not write "+file+": "+e);}
    }
}
