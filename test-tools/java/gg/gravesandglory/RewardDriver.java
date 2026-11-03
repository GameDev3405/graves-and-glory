package gg.gravesandglory;

import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.nio.file.*;
import java.util.*;

/**
 * Unshipped QA driver for matrix row K7 (rewards). Runs only on a development dedicated server started with
 * -Dgravesandglory.rewardDriver=&lt;control dir&gt; (gradle -PrewardDriver=&lt;dir&gt;). It never changes a rule: it only
 * calls the real Campaign.finish, moves the round's clock back, and writes what the game holds.
 * Protocol: write one line "ID op [arg]" to &lt;dir&gt;/cmd.txt; the server thread deletes it and writes the JSON answer to &lt;dir&gt;/out-ID.json.
 *  state            the round, each participant (team, seated, seated seconds, away seconds) and every loaded profile
 *  shift N          pretends the round (and every participant's seating and drop) began N seconds earlier, so a 3 minute round needs no 3 minutes of waiting
 *  finish W         Campaign.finish(W) (0 Dawn, 1 Dusk, -1 draw); answers whether a round was running
 */
public final class RewardDriver implements ModInitializer {
    int ticks;
    @Override public void onInitialize(){
        String dir=System.getProperty("gravesandglory.rewardDriver");if(dir==null||dir.isBlank())return;
        Path control=Path.of(dir);
        ServerTickEvents.END_SERVER_TICK.register(server->{
            if(++ticks%5!=0)return;
            Path cmd=control.resolve("cmd.txt");if(!Files.exists(cmd))return;
            String id="x";JsonObject out=new JsonObject();
            try{
                String line=Files.readString(cmd).trim();Files.delete(cmd);
                String[] parts=line.split(" ");id=parts[0];String op=parts[1];int arg=parts.length>2?Integer.parseInt(parts[2]):0;
                var b=GravesAndGlory.battle;var c=b.campaign;
                out.addProperty("op",op);
                switch(op){
                    case "shift"->{long t=arg*20L;b.roundStarted-=t;
                        /* a participant who is away keeps his own seating span (leftAt - joinedAt), so only the round clock and present players move */
                        for(UUID u:c.participants)if(!c.leftAt.containsKey(u))c.joinedAt.computeIfPresent(u,(k,v)->v-t);}
                    case "finish"->{out.addProperty("wasRunning",b.running);c.finish(arg);}
                    case "state"->{}
                    default->throw new IllegalArgumentException("unknown op "+op);
                }
                out.addProperty("running",b.running);out.addProperty("time",b.time);out.addProperty("roundSeconds",b.roundSeconds);out.addProperty("elapsedSeconds",(b.time-b.roundStarted)/20);
                out.addProperty("profileDir",c.directory.toAbsolutePath().toString());out.addProperty("practice",c.practice);
                JsonArray parts2=new JsonArray();
                for(UUID u:c.participants){JsonObject p=new JsonObject();var s=b.squads.get(u);p.addProperty("id",u.toString());p.addProperty("name",c.name(u));p.addProperty("team",s==null?-1:s.team);p.addProperty("seated",s!=null&&s.seated&&!s.bot);
                    p.addProperty("seatedSeconds",c.seatedTicks(u)/20.0);p.addProperty("awaySeconds",c.awayTicks.getOrDefault(u,0L)/20.0);p.addProperty("away",c.away(u));parts2.add(p);}
                out.add("participants",parts2);
                JsonArray profiles=new JsonArray();
                for(var e:c.profiles.entrySet()){var pr=e.getValue();JsonObject p=new JsonObject();p.addProperty("id",e.getKey().toString());p.addProperty("name",c.name(e.getKey()));
                    p.addProperty("gold",pr.gold);p.addProperty("xp",pr.progression.xp);p.addProperty("matches",pr.matches);p.addProperty("wins",pr.wins);p.addProperty("shards",pr.shards.values().stream().mapToInt(Integer::intValue).sum());profiles.add(p);}
                out.add("profiles",profiles);
                out.add("results",new JsonArray());
                for(var r:c.results.entrySet()){JsonObject p=r.getValue();JsonObject q=new JsonObject();q.addProperty("id",r.getKey().toString());q.addProperty("outcome",p.get("outcome").getAsString());q.addProperty("gold",p.get("gold").getAsInt());q.addProperty("eligible",p.get("eligible").getAsBoolean());q.addProperty("saved",p.get("saved").getAsBoolean());q.addProperty("seconds",p.get("seconds").getAsLong());out.getAsJsonArray("results").add(q);}
                out.addProperty("ok",true);
            }catch(Exception e){out.addProperty("ok",false);out.addProperty("error",e.toString());}
            try{Files.writeString(control.resolve("out-"+id+".json"),out.toString());}catch(Exception e){e.printStackTrace();}
        });
    }
}
