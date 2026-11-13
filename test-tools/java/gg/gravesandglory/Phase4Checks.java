package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Isolated real-client checks for Phase 4 (modes and campaign). GRAVES_PHASE4_CHECKS=<suite>[,<suite>...]. Never included in releases. */
public final class Phase4Checks implements ClientModInitializer {
    public interface Suite { void step(MinecraftClient c,int tick);
        /** Client ticks the suite may take before it is failed as stuck (one tick is 50 ms). */
        default int limit(){return 2400;}
        /** Called once on the client thread when the suite ends by any path (done, timed out or thrown), before the next suite starts: puts back what the suite changed. */
        default void cleanup(MinecraftClient c){} }
    /** Optional for a suite: called on the server thread after every server tick, for checks that need exact tick timing. */
    public interface ServerWatch { void serverTick(GravesAndGlory.Battle b); }
    static final Map<String,Supplier<Suite>> SUITES=new LinkedHashMap<>();
    static{SUITES.put("modes",ModesChecks::new);SUITES.put("convoy",ConvoyChecks::new);}
    static{SUITES.put("ending",EndingChecks::new);SUITES.put("ending-modes",EndingChecks.ModesSuite::new);SUITES.put("ending-sync",EndingChecks.SyncSuite::new);}
    static{SUITES.put("siege-play",SiegePlayChecks::new);SUITES.put("march-modes",MarchModesChecks::new);SUITES.put("march-bonus",MarchBonusChecks::new);SUITES.put("survival",SurvivalChecks::new);SUITES.put("plan-server",PlanServerChecks::new);SUITES.put("map-art",MapArtChecks::new);SUITES.put("map-screen",MapScreenChecks::new);}
    static final ArrayDeque<String> queue=new ArrayDeque<>();
    static volatile Suite current;static volatile String currentName="";static volatile boolean failed,broken;
    static int tick,front;static boolean started;

    /** One evidence line (pass/fail id) plus a "[p4] id=result why" log line. */
    public static void report(String id,boolean pass,String why){
        if(!pass)failed=true;
        String line=why==null?"":why.replace('\r',' ').replace('\n',' ');
        MeltyTest.report(id,pass,line);System.out.println("[p4] "+id+"="+pass+" "+line);
    }
    /** Saves the framebuffer as screenshots/p4-<name>.png in the run folder. */
    public static void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"p4-"+name+".png",c.getFramebuffer(),t->{});}
    /** Runs f on the server thread with the battle. A throw is reported as p4-<suite>-exception and ends the suite. */
    public static void server(MinecraftClient c,Consumer<GravesAndGlory.Battle> f){
        if(c.getServer()==null)return;
        c.getServer().execute(()->{if(GravesAndGlory.battle!=null)try{f.accept(GravesAndGlory.battle);}catch(Throwable e){report("p4-"+currentName+"-exception",false,e.toString());e.printStackTrace();broken=true;}});
    }
    /** Runs the suite cleanup, reports p4-<suite>-suite, then stops the client once the last listed suite is done. */
    public static void finish(MinecraftClient c,String suite){
        var done=current;current=null;
        if(done!=null)try{done.cleanup(c);}catch(Exception e){report("p4-"+suite+"-exception",false,"cleanup: "+e);e.printStackTrace();}
        report("p4-"+suite+"-suite",!failed,failed?"an earlier p4 check failed":"all checks passed");
        if(queue.isEmpty())c.scheduleStop();
    }

    @Override public void onInitializeClient(){
        String list=System.getProperty("gravesandglory.phase4Checks","");if(list.isBlank())return;
        for(String name:list.split(","))if(!name.isBlank())queue.add(name.trim());
        ServerTickEvents.END_SERVER_TICK.register(s->{if(current instanceof ServerWatch w&&GravesAndGlory.battle!=null)try{w.serverTick(GravesAndGlory.battle);}catch(Throwable e){report("p4-"+currentName+"-exception",false,e.toString());e.printStackTrace();broken=true;}});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            c.options.hudHidden=true; // the pictures are of the world
            if(c.getOverlay()!=null)return;
            if(c.currentScreen instanceof net.minecraft.client.gui.screen.AccessibilityOnboardingScreen s){s.close();return;} // a fresh test root has no options.txt: press Continue as a player would
            if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            if(current==null){
                if(queue.isEmpty())return;
                currentName=queue.poll();failed=false;broken=false;tick=0;var make=SUITES.get(currentName);
                if(make==null){report("p4-"+currentName+"-suite",false,"unknown suite");if(queue.isEmpty())c.scheduleStop();return;}
                current=make.get();
            }
            if(broken){broken=false;finish(c,currentName);return;}
            try{
                if(++tick>current.limit()){report("p4-"+currentName+"-timeout",false,"no result after "+current.limit()+" ticks");finish(c,currentName);return;}
                current.step(c,tick);
            }catch(Exception e){report("p4-"+currentName+"-exception",false,e.toString());e.printStackTrace();finish(c,currentName);}
        });
    }

    /**
     * Suite "modes": the mode frame on a real integrated server. In the lobby, the battlemode command refuses a mode the
     * map does not host and the snapshot carries the mode block. In a round, Domination's scoring interval finishes at
     * the target, a 30 s round times out on the higher score and draws on a tie, a squad with no point owned earns the
     * passive income, and the crash marker exists while the round runs, is gone after it ends, and puts the world time
     * and the point flags back when recovered. Ownership and scores are re-asserted every server tick while a scoring
     * scenario runs, so no bot can change the state under the check.
     */
    static final class ModesChecks implements Suite,ServerWatch {
        volatile UUID player;volatile boolean done;
        int scenario,sub;                 // server thread only
        long armedAt,stageAt,finishedAt;
        int gold0,earned0;
        boolean siegeDuring,siegeTeardown,siegeRecovered,deadPoints,keptMarker,roomMode,swapSafe,roomClears;
        Path siegeFile;
        final List<String> notes=new ArrayList<>(),fails=new ArrayList<>();

        public int limit(){return 24000;} // the Siege scenario builds Ironhold and plays two rounds

        public void cleanup(MinecraftClient c){server(c,b->{if(b.running)b.campaign.returnLobby();});}

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);player=c.player.getUuid();}
            if(done)finish(c,"modes");
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(player==null||done)return;
            ServerPlayerEntity p=b.server.getPlayerManager().getPlayer(player);
            if(p==null)return;
            if(scenario==0){scenario=1;sub=0;return;}
            switch(scenario){
                case 1->refuse(b,p);
                case 2->json(b,p);
                case 7->marker(b,p);
                case 8->siege(b,p);
                default->scoring(b,p);
            }
        }

        /** The battlemode command: Siege is registered but not played on map 0, an unknown id is refused, and domination is accepted. */
        void refuse(GravesAndGlory.Battle b,ServerPlayerEntity p){
            if(b.campaign.map!=0){ // the save keeps the last map: this suite opens on Rivergate, whatever the last run left
                if(stageAt==0){stageAt=b.time;b.campaign.switchMap(0);return;}
                if(b.time-stageAt>12000){report("p4-modes-refuse",false,"Rivergate was not ready within 12000 ticks of the switch");next(2);return;}
                if(!b.ready)return;
            }
            if(!b.ready)return;
            b.campaign.closeParty();b.campaign.partyLeader=p.getUuid();b.campaign.party.add(p.getUuid()); // the command is the lobby leader's, as map:, mode: and fill are
            String start=b.campaign.battleMode;
            b.campaign.notices.remove(p.getUuid());
            b.campaign.handle(p,"battlemode:siege");
            String siege=b.campaign.notices.getOrDefault(p.getUuid(),"");
            boolean siegeRefused=start.equals(Modes.DOMINATION)&&b.campaign.battleMode.equals(Modes.DOMINATION)&&siege.contains("not played on");
            boolean hosted=Modes.registered(Modes.SIEGE)&&Modes.hosts(Modes.SIEGE,1)&&!Modes.hosts(Modes.SIEGE,0);
            b.campaign.notices.remove(p.getUuid());
            b.campaign.handle(p,"battlemode:nosuch");
            String nosuch=b.campaign.notices.getOrDefault(p.getUuid(),"");
            boolean nosuchRefused=b.campaign.battleMode.equals(Modes.DOMINATION)&&!nosuch.isEmpty();
            b.campaign.notices.remove(p.getUuid());
            boolean accepted=b.campaign.handle(p,"battlemode:domination");
            String dom=b.campaign.notices.getOrDefault(p.getUuid(),"");
            boolean ok=siegeRefused&&hosted&&nosuchRefused&&accepted&&b.campaign.battleMode.equals(Modes.DOMINATION)&&dom.contains("Battle mode: Domination");
            report("p4-modes-refuse",ok,"siege on map 0 -> \""+siege+"\" (hosts map 1="+Modes.hosts(Modes.SIEGE,1)+", map 0="+Modes.hosts(Modes.SIEGE,0)+") / nosuch -> \""+nosuch+"\" / domination -> \""+dom+"\"");
            next(2);
        }

        /** The lobby snapshot's battleMode block: id, rule, the sheet's three how-to lines, max and format. */
        void json(GravesAndGlory.Battle b,ServerPlayerEntity p){
            if(b.running){report("p4-modes-json",false,"a round is running, so the snapshot is not the lobby's");next(3);return;}
            var row=Modes.row(Modes.DOMINATION);
            JsonObject snap=JsonParser.parseString(b.campaign.snapshot(p)).getAsJsonObject();
            JsonObject m=snap.has("battleMode")?snap.getAsJsonObject("battleMode"):null;
            List<String> want=ModeMath.howTo(row.howTo()),wantDefend=ModeMath.howTo(row.howToDefend()),how=m==null?List.of():strings(m,"howTo"),defend=m==null?List.of():strings(m,"howToDefend");
            List<Integer> max=m==null?List.of():ints(m,"max");List<String> format=m==null?List.of():strings(m,"format");
            String rule=m==null?"":str(m,"rule"),id=m==null?"":str(m,"id");
            boolean ok=id.equals("domination")&&rule.equals("Domination to 300")&&want.size()==2&&how.equals(want)&&defend.equals(wantDefend)&&max.equals(List.of(300,300))&&format.equals(List.of("plain","plain"));
            report("p4-modes-json",ok,"id="+id+" rule=\""+rule+"\" howTo="+how.size()+" lines, match="+how.equals(want)+", defend="+defend.equals(wantDefend)+" (the two cells are identical for Domination), max="+max+" format="+format);
            next(3);
        }

        /** Asserts the marker during a round, that returnLobby removes it, that the cached-ready hook restores the time and the flags, and that a rebuilt-map marker deletes only files inside its own folder. */
        void marker(GravesAndGlory.Battle b,ServerPlayerEntity p){
            if(sub==0){
                if(!Phase1Checks.skirmish(b,p,0,1,"normal"))return;
                sub=1;armedAt=b.time;return;
            }
            Path path=Modes.marker(b);
            boolean exists=Files.exists(path);
            String mode="",map="";
            if(exists)try{JsonObject m=JsonParser.parseString(Files.readString(path)).getAsJsonObject();mode=str(m,"mode");map=String.valueOf(m.has("map")?m.get("map").getAsInt():-1);}catch(Exception e){mode="unreadable: "+e;}
            boolean during=exists&&mode.equals("domination")&&map.equals("0");
            b.campaign.returnLobby();
            boolean gone=!Files.exists(path);
            boolean cleared=false,flagBack=false,removed=false,timeOk=false,folderGone=false,cwdKept=false;
            String bad="";
            try{
                Files.createDirectories(path.getParent());
                JsonObject hand=new JsonObject();hand.addProperty("mode",Modes.DOMINATION);hand.addProperty("map",0);hand.addProperty("time",18000);hand.addProperty("flagsDown",true);hand.add("saved",new JsonArray());
                Files.writeString(path,Campaign.JSON.toJson(hand));
                var world=b.server.getOverworld();var point=b.points.get(0);
                BlockPos base=new BlockPos(point.spec.x(),point.spec.elevation()+1,point.spec.z());
                world.setBlockState(base,Blocks.AIR.getDefaultState(),net.minecraft.block.Block.NOTIFY_LISTENERS|net.minecraft.block.Block.FORCE_STATE);
                cleared=world.getBlockState(base).isAir();
                world.setTimeOfDay(3000); // the init time: the recovered 18000 must be the one that stands
                b.mapReady();           // the cached-ready hook, not Modes.recover directly
                long time=world.getTimeOfDay();
                timeOk=time==18000;
                flagBack=world.getBlockState(base).getBlock()==Blocks.OAK_FENCE;
                removed=!Files.exists(path);
                if(!timeOk)bad="world time "+time+", want 18000; ";
                if(!cleared)bad+="point A's flag was not cleared before recovery; ";
                if(!flagBack)bad+="point A's flag base is "+world.getBlockState(base).getBlock()+", want oak fence; ";
                // A marker for another map drops itself and the files it saved, only in its own folder: a same-named file in the working directory must survive.
                String name="p4-marker-saved.txt";
                Path folderFile=path.getParent().resolve(name),cwdFile=Path.of(name);
                Files.createDirectories(path.getParent());
                Files.writeString(folderFile,"saved by the round");
                Files.writeString(cwdFile,"not the marker's");
                JsonObject other=new JsonObject();other.addProperty("mode",Modes.DOMINATION);other.addProperty("map",(b.campaign.map+1)%Spec.MAPS.length);other.addProperty("time",18000);other.addProperty("flagsDown",false);
                JsonArray saved=new JsonArray();saved.add(name);other.add("saved",saved);
                Files.writeString(path,Campaign.JSON.toJson(other));
                Modes.recover(b);
                folderGone=!Files.exists(folderFile);
                cwdKept=Files.exists(cwdFile);
                Files.deleteIfExists(cwdFile);
                if(!folderGone)bad+="the rebuilt-map marker left its saved file in the marker folder; ";
                if(!cwdKept)bad+="the rebuilt-map marker deleted a file in the working directory; ";
            }catch(Exception e){bad="marker: "+e;}
            boolean ok=during&&gone&&cleared&&timeOk&&flagBack&&removed&&folderGone&&cwdKept&&bad.isEmpty();
            report("p4-mode-marker",ok,(during?"marker written during the round":"no marker during the round")+", gone after the lobby="+gone+", cached-ready hook: time 18000="+timeOk+", point A flag back="+flagBack+", file removed="+removed+", rebuilt-map saved file deleted="+folderGone+", working-directory file kept="+cwdKept+(bad.isEmpty()?"":(" / "+bad)));
            next(8);
        }

        /**
         * Siege on Ironhold: the round shuts the gate with a real portcullis, takes all three flags down, and writes a marker
         * with one saved box. returnLobby restores the gate, the flags and the files; a second round recovered through the
         * cached-ready hook does the same after a crash.
         */
        void siege(GravesAndGlory.Battle b,ServerPlayerEntity p){
            Path marker=Modes.marker(b);
            BlockPos passage=new BlockPos(-39,68,0); // the intact gate outer grille; the open gate is air there
            BlockPos flagBase=new BlockPos(b.points.get(0).spec.x(),b.points.get(0).spec.elevation()+1,b.points.get(0).spec.z());
            if(sub==0){
                if(b.campaign.map!=1){b.campaign.switchMap(1);stageAt=b.time;sub=1;}
                return;
            }
            if(sub==1){
                if(b.time-stageAt>12000){fails.add("Ironhold was not ready within 12000 ticks of the switch");report("p4-mode-siege",false,"Ironhold did not build");done=true;return;}
                if(!b.ready)return;
                b.campaign.battleMode=Modes.SIEGE;
                if(Phase1Checks.skirmish(b,p,1,1,"normal")){armedAt=b.time;sub=2;}
                return;
            }
            if(sub==2){
                if(!b.running)return;
                boolean openAtStart=b.world.getBlockState(passage).isAir();{var gr=SiegeMode.gate();Structures.place(b.world,SiegeMode.GATE_INTACT,gr.x(),gr.elevation(),gr.z(),gr.rotation());} // the gate is open until the yard falls; drop it as the drop does
                if(!openAtStart)fails.add("Siege round: the gate was shut before the yard fell");
                boolean markerOk=false,fileOk=false,shut=false,down=true;String bad="";siegeFile=null;
                try{
                    if(Files.exists(marker)){
                        JsonObject m=JsonParser.parseString(Files.readString(marker)).getAsJsonObject();
                        var saved=m.getAsJsonArray("saved");
                        markerOk=str(m,"mode").equals(Modes.SIEGE)&&m.get("map").getAsInt()==1&&m.get("flagsDown").getAsBoolean()&&saved.size()==1+SiegeMode.wards().size(); // the gate and the wards
                        if(markerOk){
                            var s=JsonParser.parseString(saved.get(0).getAsString()).getAsJsonObject();
                            siegeFile=Path.of(s.get("file").getAsString());fileOk=Files.exists(siegeFile);
                        }
                    }
                    shut=b.world.getBlockState(passage).getBlock()==Blocks.IRON_BARS;
                    for(var point:b.points){var base=new BlockPos(point.spec.x(),point.spec.elevation()+1,point.spec.z());if(!b.world.getBlockState(base).isAir())down=false;}
                    if(!markerOk)bad+="marker mode/map/flags/saved; ";
                    if(!fileOk)bad+="saved file missing; ";
                    if(!shut)bad+="passage is "+b.world.getBlockState(passage).getBlock()+", not iron bars; ";
                    if(!down)bad+="a flag is still up; ";
                }catch(Exception e){bad="siege: "+e;}
                siegeDuring=markerOk&&fileOk&&shut&&down&&b.saves.size()==1+SiegeMode.wards().size()&&bad.isEmpty();
                if(!siegeDuring)fails.add("Siege round: "+bad);
                // Flags down: the commander stands on point A past its capture time, yet the point stays neutral; a held point is no forward anchor.
                var a=b.points.get(0);
                p.teleport(b.world,a.spec.x()+0.5,Arena.ground(a.spec.x(),a.spec.z())+1,a.spec.z()+0.5,0,0);
                for(int i=0;i<a.spec.captureSeconds()+3;i++)b.captureTick();
                boolean neutral=a.owner==-1&&a.progress==0&&b.world.getBlockState(flagBase).isAir();
                a.owner=0;a.ownedSince=b.time-1_000_000L;
                boolean noAnchor=b.reinforcementAnchors(0).stream().noneMatch(x->x.name().equals(a.spec.name()));
                a.owner=-1;
                deadPoints=neutral&&noAnchor&&!b.mode.capturesPoints(b);
                if(!deadPoints)fails.add("Siege points: neutral after standing on A="+neutral+" (owner "+a.owner+", progress "+a.progress+"), no point anchor="+noAnchor);
                // A teardown that cannot restore the gate keeps the marker and the saved box; once the file is back, a second return succeeds.
                Path held=siegeFile==null?null:siegeFile.resolveSibling(siegeFile.getFileName()+".held");
                try{
                    if(held!=null)Files.move(siegeFile,held);
                    b.campaign.returnLobby();
                    keptMarker=held!=null&&Files.exists(marker)&&b.saves.size()==1&&b.world.getBlockState(passage).getBlock()==Blocks.IRON_BARS;
                    if(held!=null)Files.move(held,siegeFile);
                }catch(Exception e){fails.add("teardown failure: "+e);}
                if(!keptMarker)fails.add("a failed teardown: marker kept="+Files.exists(marker)+", saves="+b.saves.size());
                b.campaign.returnLobby();
                sub=3;
                return;
            }
            boolean gateOpen=b.world.getBlockState(passage).isAir(),flagBack=b.world.getBlockState(flagBase).getBlock()==Blocks.OAK_FENCE;
            boolean filesGone=!Files.exists(marker)&&(siegeFile==null||!Files.exists(siegeFile));
            if(sub==3){
                siegeTeardown=gateOpen&&flagBack&&filesGone;
                if(!siegeTeardown)fails.add("Siege teardown: gate open="+gateOpen+", flags back="+flagBack+", files gone="+filesGone);
                // The second Siege round starts from a lobby room whose host picked Siege: the room's mode must reach the round.
                var c=b.campaign;c.battleMode=Modes.DOMINATION;c.closeParty();
                for(var s:b.squads.values())if(!s.bot)s.seated=false;
                String said="";
                for(String cmd:List.of("social:room","social:mode:siege","social:map:1","social:ready","social:start")){c.notices.remove(p.getUuid());c.handle(p,cmd);said+=cmd+" -> "+c.notices.getOrDefault(p.getUuid(),"")+"; ";}
                roomMode=b.running&&b.mode.id().equals(Modes.SIEGE)&&c.battleMode.equals(Modes.SIEGE);
                if(!roomMode){fails.add("room start: running="+b.running+", mode="+b.mode.id()+" / "+said);c.battleMode=Modes.SIEGE;if(!Phase1Checks.skirmish(b,p,1,1,"normal"))return;}
                armedAt=b.time;sub=4;
                return;
            }
            // sub 4: a running round is abandoned to the cached-ready hook, as a crash would leave it
            if(!b.running)return;
            // First, startRound re-entered mid-Siege: to Domination puts the gate, flags and file back; Domination to Siege
            // and Siege to Siege then shut it again from a fresh snapshot of the open gate, which the recovery below proves.
            {
                Path before=b.saves.isEmpty()?null:b.saves.get(0).file();
                b.campaign.battleMode=Modes.DOMINATION;b.startRound();
                boolean open=b.world.getBlockState(passage).isAir(),up=b.world.getBlockState(flagBase).getBlock()==Blocks.OAK_FENCE;
                boolean fileGone=before!=null&&!Files.exists(before)&&b.saves.isEmpty();
                boolean domMarker=false;
                try{var m=JsonParser.parseString(Files.readString(marker)).getAsJsonObject();domMarker=str(m,"mode").equals(Modes.DOMINATION)&&m.getAsJsonArray("saved").isEmpty()&&!m.get("flagsDown").getAsBoolean();}catch(Exception ignored){}
                b.campaign.battleMode=Modes.SIEGE;b.startRound();b.startRound();{var gr=SiegeMode.gate();Structures.place(b.world,SiegeMode.GATE_INTACT,gr.x(),gr.elevation(),gr.z(),gr.rotation());} // the yard falls again
                boolean reshut=b.world.getBlockState(passage).getBlock()==Blocks.IRON_BARS&&b.saves.size()==1+SiegeMode.wards().size()&&b.mode.id().equals(Modes.SIEGE);
                swapSafe=open&&up&&fileGone&&domMarker&&reshut;
                if(!swapSafe)fails.add("startRound swap: gate open="+open+", flags up="+up+", file gone="+fileGone+", domination marker="+domMarker+", shut again="+reshut);
            }
            boolean shut=b.world.getBlockState(passage).getBlock()==Blocks.IRON_BARS;
            boolean marked=Files.exists(marker);Path file=null;
            try{
                if(marked){var m=JsonParser.parseString(Files.readString(marker)).getAsJsonObject();var saved=m.getAsJsonArray("saved");
                    if(saved.size()==1+SiegeMode.wards().size())file=Path.of(JsonParser.parseString(saved.get(0).getAsString()).getAsJsonObject().get("file").getAsString());}
            }catch(Exception ignored){}
            b.mapReady(); // Modes.recover, the hook a cached start calls
            boolean open=b.world.getBlockState(passage).isAir(),flagsBack=b.world.getBlockState(flagBase).getBlock()==Blocks.OAK_FENCE;
            boolean gone=!Files.exists(marker)&&(file==null||!Files.exists(file));
            siegeRecovered=shut&&marked&&open&&flagsBack&&gone;
            if(!siegeRecovered)fails.add("Siege recovery: shut="+shut+", marked="+marked+", gate open="+open+", flags back="+flagsBack+", files gone="+gone);
            b.campaign.returnLobby();
            roomClears=b.campaign.battleMode.equals(Modes.DOMINATION)&&b.campaign.battleModeBeforeRoom==null; // the lobby's own mode (Domination before the room) is back
            b.campaign.handle(p,"social:exit");
            report("p4-mode-siege",siegeDuring&&siegeTeardown&&siegeRecovered,
                "round: gate shut="+siegeDuring+"; teardown: gate open, flags up, file gone="+siegeTeardown+"; crash recovery through mapReady="+siegeRecovered);
            report("p4-mode-dead-points",deadPoints,"Siege: point A stays neutral under a commander and is no forward anchor="+deadPoints);
            report("p4-mode-teardown-kept",keptMarker,"an unrestorable gate keeps the marker and its saved box="+keptMarker);
            report("p4-mode-room",roomMode,"a room whose host picked Siege starts a Siege round="+roomMode);
            report("p4-mode-room-clears",roomClears,"after the room's Siege round the lobby's battleMode is "+b.campaign.battleMode+" (want domination)");
            report("p4-mode-swap",swapSafe,"startRound mid-Siege restores the gate before the next mode="+swapSafe);
            if(!fails.isEmpty())System.out.println("[p4] siege fails: "+String.join(" / ",fails));
            done=true;
        }

        void scoring(GravesAndGlory.Battle b,ServerPlayerEntity p){
            if(sub==0){
                if(stageAt==0)stageAt=b.time;
                if(b.points.isEmpty()){fails.add("map 0 has no capture points");next(7);return;}
                if(b.time-stageAt>2400){fails.add("the practice round on map 0 did not start within 2400 ticks");reportDomination();next(7);return;}
                if(!Phase1Checks.skirmish(b,p,0,1,"normal"))return;
                b.campaign.lastWinner=-1;
                arm(b);
                armedAt=b.time;sub=1;
                return;
            }
            switch(scenario){
                case 3->dominance(b);
                case 4->timeout(b,0,"5 to 3");
                case 5->timeout(b,-1,"4 to 4");
                case 6->passive(b);
            }
        }

        /** The state each scoring scenario starts from. A is armed at 299 with point A held; B and C shorten the round to 30 s and arm a score; D leaves every point neutral. */
        void arm(GravesAndGlory.Battle b){
            if(scenario==3){
                neutral(b);b.points.get(0).owner=0;
                b.scores[0]=299;b.scores[1]=0;
            }else if(scenario==4||scenario==5){
                b.roundSeconds=30;b.roundStarted=b.time-590;
                b.scores[0]=scenario==4?5:4;b.scores[1]=scenario==4?3:4;
            }else{
                b.roundSeconds=900;neutral(b);
                var s=b.squads.get(player);gold0=s.gold;earned0=s.stats.goldEarned;
                int passive=Spec.ECONOMY[0].passiveGold(),pure=b.mode.income(b,s);
                if(pure!=passive)fails.add("mode.income is "+pure+" with no point owned, want passive gold "+passive);
            }
        }

        static void neutral(GravesAndGlory.Battle b){
            for(var c:b.points){c.owner=-1;c.progress=0;c.challenger=-1;c.contested=false;}
        }

        /** A: 299 to 0 with point A held finishes for team 0 at the next scoring interval (the mode has done it inside Battle.tick by the time this runs). */
        void dominance(GravesAndGlory.Battle b){
            if(b.running){
                neutral(b);b.points.get(0).owner=0;b.scores[0]=299;
                if(b.time-armedAt>200){fails.add("no finish 200 ticks after arming 299 to 0 with point A held (scores "+b.scores[0]+":"+b.scores[1]+")");next(4);}
                return;
            }
            finishedAt=b.time;
            var row=Modes.row(Modes.DOMINATION);
            if(b.campaign.lastWinner!=0)fails.add("the interval at 299 finished for team "+b.campaign.lastWinner+", want 0");
            else if(b.scores[0]<row.winTarget())fails.add("finished for team 0 at "+b.scores[0]+" to "+b.scores[1]+", want the target "+row.winTarget());
            else if(finishedAt%row.scoreInterval()!=0)fails.add("finished at tick "+finishedAt+", not a scoring interval");
            else if(finishedAt-armedAt>row.scoreInterval())fails.add("finished "+(finishedAt-armedAt)+" ticks after arming, want the next "+row.scoreInterval()+"-tick interval");
            else notes.add("299 to 0 with A held finished for team 0 at the next interval ("+(finishedAt-armedAt)+" ticks), score "+b.scores[0]+" to "+b.scores[1]);
            next(4);
        }

        /** B and C: the 30 s round times out on the higher score, and a tie is a draw, both after exactly 600 ticks. */
        void timeout(GravesAndGlory.Battle b,int want,String label){
            if(b.running){
                if(b.time-armedAt>30){fails.add("the 30 s round at "+label+" did not time out 30 ticks after arming at 590 elapsed");next(want==0?5:6);}
                return;
            }
            long round=b.time-b.roundStarted;
            int winner=b.campaign.lastWinner;
            if(round!=600)fails.add("the 30 s round at "+label+" ended after "+round+" ticks, want 600");
            else if(winner!=want)fails.add("the 30 s round at "+label+" ended for "+(winner<0?"a draw":"team "+winner)+", want "+(want<0?"a draw":"team "+want));
            else notes.add("30 s round at "+label+" ended in "+(want<0?"a draw":"a team 0 win")+" after "+round+" ticks");
            next(want==0?5:6);
        }

        /** D: with no point owned the squad earns the passive income every economy.incomeSeconds (5 gold per 100 ticks), observed on the real earn tick. */
        void passive(GravesAndGlory.Battle b){
            if(!b.running){fails.add("the round ended before the income tick");reportDomination();next(7);return;}
            neutral(b);
            var s=b.squads.get(player);
            int every=Spec.ECONOMY[0].incomeSeconds()*20,passive=Spec.ECONOMY[0].passiveGold();
            if(b.time%every==0){
                int gold=s.gold-gold0,earned=s.stats.goldEarned-earned0;
                if(gold!=passive)fails.add("no point owned but the squad earned "+gold+" gold in an income interval, want "+passive);
                else if(earned!=passive)fails.add("no point owned but stats.goldEarned rose by "+earned+", want "+passive);
                else notes.add("no point owned: one squad earned "+gold+" gold on the "+every+"-tick income tick");
                reportDomination();
                next(7);
                return;
            }
            if(b.time-armedAt>400){fails.add("no income tick within 400 ticks of arming (round time "+b.time+")");reportDomination();next(7);}
        }

        void reportDomination(){
            report("p4-modes-domination",fails.isEmpty(),fails.isEmpty()?"4 checks: "+String.join(" / ",notes):String.join(" / ",fails));
        }

        void next(int to){scenario=to;sub=0;stageAt=0;}

        static String str(JsonObject o,String key){return o!=null&&o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
        static List<String> strings(JsonObject o,String key){List<String> out=new ArrayList<>();if(o!=null&&o.has(key)&&o.get(key).isJsonArray())for(var e:o.getAsJsonArray(key))out.add(e.getAsString());return out;}
        static List<Integer> ints(JsonObject o,String key){List<Integer> out=new ArrayList<>();if(o!=null&&o.has(key)&&o.get(key).isJsonArray())for(var e:o.getAsJsonArray(key))out.add(e.getAsInt());return out;}
    }
}
