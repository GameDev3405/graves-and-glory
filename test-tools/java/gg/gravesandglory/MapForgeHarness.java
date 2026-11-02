package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;

/** UNSHIPPED driver for the Map Forge adoption runs (-PmapForge=SCRIPT, test-tools/MapForge.java). It hosts the usual
 * Graves and Glory world in the isolated run folder and works through a script, one line at a time:
 *   map N           build battlefield N and wait until it is ready (adoption included)
 *   cmd ARGS        /mapforge ARGS must succeed (its job too)
 *   fail ARGS       /mapforge ARGS must be refused (or its job fail)
 *   mc COMMAND      a vanilla command (a builder's hand edit: fill, setblock)
 *   note TEXT       a line in the evidence
 * Evidence: (run folder)/mapforge/harness.txt; the client stops when the script ends, on the first failure, or after
 * 28 minutes. */
public final class MapForgeHarness implements ClientModInitializer {
    static final long LIMIT=28L*60*1000;
    int ticks,buildWait;boolean hosted,building,waitingJob,expectFail,done;volatile Integer result;String current;long began,stepAt;
    final Deque<String> script=new ArrayDeque<>();File evidence;

    void log(String text){
        String line=String.format("%8.1fs %s",(System.currentTimeMillis()-began)/1000.0,text);System.out.println("[Map forge harness] "+line);
        try{Files.writeString(evidence.toPath(),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){e.printStackTrace();}
    }
    void end(MinecraftClient c,boolean ok,String why){log(ok?"COMPLETE":"FAILED: "+why);done=true;c.scheduleStop();}

    @Override public void onInitializeClient(){
        String path=System.getProperty("gravesandglory.mapForgeScript");
        if(path==null||!Boolean.getBoolean("gravesandglory.mapforge"))return;
        try{for(String l:Files.readAllLines(new File(path).toPath())){l=l.strip();if(!l.isEmpty()&&!l.startsWith("#"))script.add(l);}}catch(Exception e){throw new IllegalStateException("map forge script "+path,e);}
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(done||c.getOverlay()!=null)return;
            if(c.world==null){if(!hosted&&++ticks>60){hosted=true;began=System.currentTimeMillis();GravesAndGloryClient.connectOrHost(c);}return;}
            var b=GravesAndGlory.battle;var server=c.getServer();if(b==null||server==null||c.player==null)return;
            c.options.pauseOnLostFocus=false;
            if(evidence==null){evidence=new File(c.runDirectory,"mapforge/harness.txt");evidence.getParentFile().mkdirs();log("script "+path+", "+script.size()+" steps");}
            if(System.currentTimeMillis()-began>LIMIT){end(c,false,"28 minute limit");return;}
            if(building){if(!b.ready){buildWait=0;return;}if(++buildWait<60)return;building=false;log("map "+b.campaign.map+" ready in "+(System.currentTimeMillis()-stepAt)+" ms; adoption: "+MapAdoption.status);return;}
            if(current!=null){
                if(result==null)return;
                if(!waitingJob){
                    int r=result;
                    if(r>0&&MapForge.job!=null){waitingJob=true;return;}
                    finishStep(c,r>0&&MapForge.lastOk);return;
                }
                if(MapForge.job!=null)return;
                finishStep(c,MapForge.lastOk);return;
            }
            if(script.isEmpty()){end(c,true,null);return;}
            String line=script.poll();stepAt=System.currentTimeMillis();
            String[] v=line.split("\\s+",2);String rest=v.length>1?v[1]:"";
            switch(v[0]){
                case "note"->log("note "+rest);
                case "map"->{int target=Integer.parseInt(rest.trim());building=true;buildWait=0;log("building map "+target);
                    server.execute(()->{b.campaign.returnLobby();if(b.campaign.map!=target)b.campaign.switchMap(target);else{b.ready=false;b.buildColumn=0;Structures.clearQueue();b.placing=false;}});}
                case "mc"->{current=line;expectFail=false;waitingJob=false;result=null;MapForge.lastOk=true;MapForge.last="mc "+rest;
                    server.execute(()->result=server.getCommandManager().executeWithPrefix(server.getCommandSource(),rest));}
                case "cmd","fail"->{current=line;expectFail=v[0].equals("fail");waitingJob=false;result=null;
                    server.execute(()->result=server.getCommandManager().executeWithPrefix(server.getCommandSource(),"mapforge "+rest));}
                default->end(c,false,"unknown script line "+line);
            }
        });
    }
    void finishStep(MinecraftClient c,boolean ok){
        String line=current;current=null;result=null;waitingJob=false;
        long ms=System.currentTimeMillis()-stepAt;
        if(ok!=!expectFail){log((expectFail?"UNEXPECTED SUCCESS ":"FAIL ")+line+" -> "+MapForge.last);end(c,false,line);return;}
        log((expectFail?"refused as expected ":"ok ")+line+" ["+ms+" ms] -> "+MapForge.last);
    }
}
