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
 *   wait N          do nothing for N ticks
 *   shot NAME x y z lookX lookY lookZ   the same, aimed at a point
 *   shot NAME x y z yaw pitch   move the player there as a spectator, wait for the chunks, save
 *                   (run folder)/mapforge/screenshots/NAME.png with the HUD hidden
 *   practice        start a practice round (camp:practice, as MapTour does): bots field, the point flags and the battle HUD show
 *   hudshot NAME x y z lookX lookY lookZ   teleport the player there in its own game mode and save NAME.png with the HUD shown
 *   points          a line in the evidence with each point (id, name, x, y, z, owner) and the scores
 * Queue runs (tools/checkq.py) pass the script as --env GRAVES_MAPFORGE_SCRIPT=<path> in place of -PmapForge.
 * Evidence: (run folder)/mapforge/harness.txt; the client stops when the script ends, on the first failure, or after
 * 28 minutes. */
public final class MapForgeHarness implements ClientModInitializer {
    static final long LIMIT=28L*60*1000;
    int ticks,buildWait,pause,settle;String shotName,shotTp;boolean hosted,building,waitingJob,expectFail,done;volatile Integer result;String current;long began,stepAt;
    final Deque<String> script=new ArrayDeque<>();File evidence;

    void log(String text){
        String line=String.format("%8.1fs %s",(System.currentTimeMillis()-began)/1000.0,text);System.out.println("[Map forge harness] "+line);
        try{Files.writeString(evidence.toPath(),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){e.printStackTrace();}
    }
    void end(MinecraftClient c,boolean ok,String why){log(ok?"COMPLETE":"FAILED: "+why);done=true;c.scheduleStop();}

    @Override public void onInitializeClient(){
        String env=System.getenv("GRAVES_MAPFORGE_SCRIPT");
        String path=env!=null&&!env.isBlank()?env:System.getProperty("gravesandglory.mapForgeScript");
        if(path==null||!(env!=null&&!env.isBlank()||Boolean.getBoolean("gravesandglory.mapforge")))return;
        try{for(String l:Files.readAllLines(new File(path).toPath())){l=l.strip();if(!l.isEmpty()&&!l.startsWith("#"))script.add(l);}}catch(Exception e){throw new IllegalStateException("map forge script "+path,e);}
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(done||c.getOverlay()!=null)return;
            if(c.world==null){if(!hosted&&++ticks>60){hosted=true;began=System.currentTimeMillis();GravesAndGloryClient.connectOrHost(c);}return;}
            var b=GravesAndGlory.battle;var server=c.getServer();if(b==null||server==null||c.player==null)return;
            c.options.pauseOnLostFocus=false;c.options.getCloudRenderMode().setValue(net.minecraft.client.option.CloudRenderMode.OFF);
            if(began==0)began=System.currentTimeMillis();if(evidence==null){evidence=new File(c.runDirectory,"mapforge/harness.txt");evidence.getParentFile().mkdirs();log("script "+path+", "+script.size()+" steps");}
            if(System.currentTimeMillis()-began>LIMIT){end(c,false,"28 minute limit");return;}
            if(building){if(!b.ready){buildWait=0;return;}if(++buildWait<60)return;building=false;log("map "+b.campaign.map+" ready in "+(System.currentTimeMillis()-stepAt)+" ms; adoption: "+MapAdoption.status);return;}
            if(c.player.isDead()){c.player.requestRespawn();c.setScreen(null);settle=40;return;}if(shotName!=null&&shotTp!=null){String tp=shotTp;server.execute(()->{for(var p:server.getPlayerManager().getPlayerList())if(p.interactionManager.getGameMode()!=net.minecraft.world.GameMode.SPECTATOR){p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),tp);}});}if(!building&&!b.ready){settle=40;return;}if(settle>0){settle--;return;}if(pause>0){pause--;return;}
            if(shotName!=null){
                String name=shotName;shotName=null;
                net.minecraft.client.util.ScreenshotRecorder.saveScreenshot(new File(c.runDirectory,"mapforge"),name+".png",c.getFramebuffer(),t->{});
                log("shot "+name);return;
            }
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
                case "practice"->{var id=c.player.getUuid();log("practice round on map "+b.campaign.map);pause=200;
                    server.execute(()->{var p=server.getPlayerManager().getPlayer(id);if(p!=null){b.campaign.handle(p,"camp:practice");b.campaign.introUntil=b.time;}});}
                case "hudshot"->{String[] a=rest.trim().split("\\s+");
                    if(a.length!=7){end(c,false,"hudshot NAME x y z lookX lookY lookZ: "+line);return;}
                    shotName=a[0];pause=120;c.options.hudHidden=false;b.campaign.leash=false;String tp="tp @a "+a[1]+" "+a[2]+" "+a[3]+" facing "+a[4]+" "+a[5]+" "+a[6];
                    server.execute(()->server.getCommandManager().executeWithPrefix(server.getCommandSource().withSilent(),tp));}
                case "points"->{StringBuilder s=new StringBuilder("points");for(var p:b.points)s.append(" ").append(p.spec.id()).append("(").append(p.spec.name()).append(" ").append(p.spec.x()).append(",").append(p.spec.elevation()).append(",").append(p.spec.z()).append(" owner ").append(p.owner).append(")");
                    log(s+" scores "+b.scores[0]+"-"+b.scores[1]+" field "+Field.halfX()+"x"+Field.halfZ()+" spawns "+Spec.FACTIONS[0].spawnX()+","+Spec.FACTIONS[0].spawnZ()+" / "+Spec.FACTIONS[1].spawnX()+","+Spec.FACTIONS[1].spawnZ());}
                case "wait"->pause=Integer.parseInt(rest.trim());
                case "props"->{String[] a=rest.trim().split("\\s+"); // props x0 y0 z0 x1 y1 z1: wake the prop anchors in templates placed by command (the game does it on chunk load)
                    if(a.length!=6){end(c,false,"props x0 y0 z0 x1 y1 z1: "+line);return;}
                    int[] n=new int[6];for(int i=0;i<6;i++)n[i]=Integer.parseInt(a[i]);var id=c.player.getUuid();pause=20;
                    server.execute(()->{var p=server.getPlayerManager().getPlayer(id);
                        if(p!=null)log("props "+Props.scanBox(p.getServerWorld(),new net.minecraft.util.math.BlockBox(n[0],n[1],n[2],n[3],n[4],n[5]))+" found in "+rest);});}
                case "shot"->{String[] a=rest.trim().split("\\s+");
                    if(a.length!=6&&a.length!=7){end(c,false,"shot NAME x y z yaw pitch | shot NAME x y z lookX lookY lookZ: "+line);return;}
                    shotName=a[0];pause=120;c.options.hudHidden=true;b.campaign.leash=false;
                    String tp="execute as @a at @s run tp @s "+a[1]+" "+a[2]+" "+a[3]+(a.length==7?" facing "+a[4]+" "+a[5]+" "+a[6]:" "+a[4]+" "+a[5]);
                    shotTp=tp;server.execute(()->{server.getCommandManager().executeWithPrefix(server.getCommandSource(),"gamemode spectator @a");
                        server.getCommandManager().executeWithPrefix(server.getCommandSource(),tp);});}
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
