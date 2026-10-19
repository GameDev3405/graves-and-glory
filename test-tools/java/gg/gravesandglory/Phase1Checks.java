package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Isolated real-client checks for Phase 1 (solo core). GRAVES_PHASE1_CHECKS=<suite>[,<suite>...]. Never included in releases. */
public final class Phase1Checks implements ClientModInitializer {
    public interface Suite { void step(MinecraftClient c,int tick); }
    /** Optional for a suite: called on the server thread after every Battle.tick, for checks that need exact tick timing. */
    public interface ServerWatch { void serverTick(GravesAndGlory.Battle b); }
    /** A suite that sees every HUD frame (Fabric's HUD callback runs after InGameHud has drawn the status bars), including frames
     *  while the client's ready is false, when step() does not run. clientTick runs every client tick, ready or not. */
    public interface HudWatch { void hudFrame(MinecraftClient c); default void clientTick(MinecraftClient c){} }
    static final Map<String,Supplier<Suite>> SUITES=new LinkedHashMap<>();
    static{SUITES.put("combat",Combat::new);SUITES.put("morale",MoraleChecks::new);SUITES.put("reinforce",ReinforceChecks::new);SUITES.put("bots",Bots::new);SUITES.put("tutorial",TutorialChecks::new);SUITES.put("report",ReportChecks::new);SUITES.put("ui-art",UiArtChecks::new);SUITES.put("ui-hud",UiHudChecks::new);SUITES.put("ui-feed",UiFeedChecks::new);SUITES.put("ui-home",UiHomeChecks::new);SUITES.put("ui-leave",UiLeaveChecks::new);}
    static final ArrayDeque<String> queue=new ArrayDeque<>();
    static volatile Suite current;static volatile String currentName="";static volatile boolean failed;
    static int tick,front,hold;static boolean started; // hold: client ticks the current suite waits (Phase1Checks.window)

    /** One evidence line (pass/fail id) plus a "[p1] id=result why" log line. */
    public static void report(String id,boolean pass,String why){if(!pass)failed=true;MeltyTest.report(id,pass,why);System.out.println("[p1] "+id+"="+pass+" "+why);}
    public static void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"p1-"+name+".png",c.getFramebuffer(),t->{});}
    public static void server(MinecraftClient c,Consumer<GravesAndGlory.Battle> f){c.getServer().execute(()->{if(GravesAndGlory.battle!=null)try{f.accept(GravesAndGlory.battle);}catch(Exception e){report("p1-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});}
    /** Practice-style start on the given map. Returns false while the map is still being switched or built; call again next tick. */
    public static boolean skirmish(GravesAndGlory.Battle b,ServerPlayerEntity p,int map,int size,String difficulty){
        var c=b.campaign;
        if(b.running)c.returnLobby();
        if(c.map!=map){c.switchMap(map);return false;}
        if(!b.ready||!b.squads.containsKey(p.getUuid()))return false;
        c.difficulty=difficulty; // the bots of this round take this tier (a Spec.BOTCOMMAND id)
        c.closeParty();c.partyLeader=p.getUuid();c.party.add(p.getUuid());c.practice=size==1;c.size=size;c.fill=true;
        for(var s:b.squads.values())if(!s.bot){s.seated=s.owner.equals(p.getUuid());if(s.seated)s.team=0;}
        b.startRound();c.introUntil=b.time;return true;
    }
    /** Reports p1-<suite>-suite, then stops the client once the last listed suite is done. */
    public static void finish(MinecraftClient c,String suite){
        report("p1-"+suite+"-suite",!failed,failed?"an earlier p1 check failed":"all checks passed");
        current=null;if(queue.isEmpty())c.scheduleStop();
    }
    /** Sets the window to w x h and the GUI Scale option to gui (0 is Auto), applies both, then holds the suite for 5 client ticks.
     *  The window has its title bar. */
    public static void window(MinecraftClient c,int w,int h,int gui){window(c,w,h,gui,false);}
    /** As window(c,w,h,gui), but borderless at the top left of the screen when bare is set. With its title bar a 1920x1080 window
     *  does not fit a 1920x1080 desktop: Windows clamps it to 1920x1061 (fix round 1, run ui-fix1-155207), where the canvas is k 1.
     *  A window set without bare gets its title bar back. A minimized window keeps its old framebuffer whatever size it is set to
     *  (run ui-polish1-200436: someone at the PC minimized it), so it is shown again first, without taking the focus. */
    public static void window(MinecraftClient c,int w,int h,int gui,boolean bare){
        var win=c.getWindow();unminimize(c);GLFW.glfwSetWindowAttrib(win.getHandle(),GLFW.GLFW_DECORATED,bare?GLFW.GLFW_FALSE:GLFW.GLFW_TRUE);
        win.setWindowedSize(w,h);if(bare)GLFW.glfwSetWindowPos(win.getHandle(),0,0);
        c.options.getGuiScale().setValue(gui);c.onResolutionChanged();hold=5;
    }
    static boolean minimized(MinecraftClient c){return GLFW.glfwGetWindowAttrib(c.getWindow().getHandle(),GLFW.GLFW_ICONIFIED)==GLFW.GLFW_TRUE;}
    /** Shows a minimized window at its last size and place; on Windows without activating it, so no one's keys or mouse go to it. */
    static void unminimize(MinecraftClient c){
        if(!minimized(c))return;long handle=c.getWindow().getHandle();
        if(org.lwjgl.system.Platform.get()==org.lwjgl.system.Platform.WINDOWS)org.lwjgl.system.windows.User32.ShowWindow(org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(handle),org.lwjgl.system.windows.User32.SW_SHOWNOACTIVATE);
        else GLFW.glfwRestoreWindow(handle);
        System.out.println("[p1] window: shown again after it was minimized (no focus taken), for "+currentName);
    }

    @Override public void onInitializeClient(){
        String list=System.getProperty("gravesandglory.phase1Checks","");if(list.isBlank())return;
        for(String name:list.split(","))if(!name.isBlank())queue.add(name.trim());
        ServerTickEvents.END_SERVER_TICK.register(s->{if(current instanceof ServerWatch w&&GravesAndGlory.battle!=null)try{w.serverTick(GravesAndGlory.battle);}catch(Exception e){report("p1-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});
        HudRenderCallback.EVENT.register((g,delta)->{if(current instanceof HudWatch w)try{w.hudFrame(MinecraftClient.getInstance());}catch(Exception e){report("p1-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            // a stray Escape from someone at the PC opens the pause menu, which pauses the integrated server and can stall a suite
            // that is waiting on it; once the window has lost the focus, the menu closes (no suite opens it)
            if(c.currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen&&!c.isWindowFocused()){c.setScreen(null);System.out.println("[p1] closed a pause menu left open in the unfocused window, during "+currentName);}
            if(c.getOverlay()!=null)return;
            if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
            if(current instanceof HudWatch w)try{w.clientTick(c);}catch(Exception e){report("p1-"+currentName+"-exception",false,e.toString());e.printStackTrace();}
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            if(current==null){
                if(queue.isEmpty())return;
                currentName=queue.poll();failed=false;tick=0;var make=SUITES.get(currentName);
                if(make==null){report("p1-"+currentName+"-suite",false,"unknown suite");if(queue.isEmpty())c.scheduleStop();return;}
                current=make.get();
            }
            if(hold>0){hold--;return;}
            try{current.step(c,++tick);}catch(Exception e){report("p1-"+currentName+"-exception",false,e.toString());e.printStackTrace();finish(c,currentName);}
        });
    }

    static int tagged(GravesAndGlory.Battle b){int n=0;for(var e:b.world.iterateEntities())if(e.getCommandTags().contains(Projectiles.TAG))n++;return n;}
    static double segmentDistance(Vec3d p,Vec3d a,Vec3d z){Vec3d ab=z.subtract(a);double t=Math.max(0,Math.min(1,p.subtract(a).dotProduct(ab)/Math.max(1e-9,ab.lengthSquared())));return p.distanceTo(a.add(ab.multiply(t)));}
    static Spec.TroopsRow troop(String id){return Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)).findFirst().orElseThrow();}
    static double top(GravesAndGlory.Battle b,double x,double z){return b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,(int)Math.floor(x),(int)Math.floor(z));}
    static void freeze(GravesAndGlory.Unit u){u.nextAttack=Long.MAX_VALUE;u.abilityAt=Long.MAX_VALUE;u.spellCenter=null;}

    /** Task 1: visible delayed projectiles, team impact flashes, HUD health flash and tick cost. Stages run on the server tick. */
    static final class Combat implements Suite,ServerWatch {
        volatile int stage;volatile String wantShot;volatile boolean shotTaken;volatile boolean finished;
        volatile UUID player;long stageAt;int shotDelay,hudTick=-1,hudSeen=-1;
        final Map<LivingEntity,Float> lastHealth=new HashMap<>();
        Projectiles.Shot first;float firstHealth;boolean firstDelayOk;
        GravesAndGlory.Unit chosen;Projectiles.Shot duskShot;int flashesBefore;boolean duskOk;String duskWhy="";
        GravesAndGlory.Unit orphanShooter,orphanSecond;Exception orphanError;
        double tickSum;int tickSamples,troops;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        boolean late(GravesAndGlory.Battle b,int ticks){return b.time-stageAt>ticks;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        /** A spectator camera floats to one side of the field: troops never target it and it never respawns. */
        void camera(GravesAndGlory.Battle b,Vec3d look,double side,double up){
            var p=sp(b);p.changeGameMode(GameMode.SPECTATOR);double drop=up+p.getStandingEyeHeight()-1;
            b.relocate(p,look.x,look.y+up,look.z+side,180,(float)Math.toDegrees(Math.atan2(drop,side)));
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=2){c.setScreen(null);shot(c,wantShot);shotTaken=true;}
            if(stage==10){
                if(hudTick<0&&tick%5==0&&!CommandMode.flashing("infantry")){hudTick=tick;server(c,b->{
                    var own=b.squads.get(player);var u=b.units.stream().filter(v->v.squad==own&&v.spec.group().equals("infantry")&&v.mob.isAlive()&&v.mob.getHealth()>5).findFirst().orElse(null);
                    if(u==null){report("p1-hud-flash",false,"no living infantry of the player's army to damage");return;}
                    u.mob.damage(b.world.getDamageSources().magic(),4);System.out.println("[p1] hud-flash damaged "+u.spec.id()+" to "+u.mob.getHealth());});}
                if(hudTick>0&&hudSeen<0&&CommandMode.flashing("infantry"))hudSeen=tick;
                if(hudSeen>0&&tick==hudSeen+3){boolean still=CommandMode.flashing("infantry");shot(c,"combat-hud-flash");
                    report("p1-hud-flash",hudSeen-hudTick<=30&&still,"flash after "+(hudSeen-hudTick)+" client ticks, still lit at screenshot="+still);stage=11;}
                else if(hudTick>0&&hudSeen<0&&tick>hudTick+30){shot(c,"combat-hud-flash");report("p1-hud-flash",false,"no infantry flash within 30 client ticks");stage=11;}
            }
            if(stage>=14&&c.currentScreen!=null)c.setScreen(null);
            if(stage==16&&!finished){finished=true;finish(c,"combat");}
            if(tick>12000&&!finished){finished=true;report("p1-combat-timeout",false,"stuck in stage "+stage);finish(c,"combat");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // fixture: practice on map 0, the two facing visual squads, a camera to one side
                    var p=sp(b);if(p==null||!skirmish(b,p,0,1,"normal"))return;
                    b.testSquad=b.squads.get(player);b.prepareVisualSquads(true);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    camera(b,new Vec3d(0,Math.max(63,Arena.ground(0,3))+1,3),12,6);
                    System.out.println("[p1] combat fixture: "+b.units.size()+" troops, map "+b.campaign.map);next(b,2);
                }
                case 2->{ // the first arrow: no damage on the firing tick, damage after it lands, visible in between
                    if(first==null){
                        for(var s:b.shots)if(s.kind().equals("arrow")){first=s;break;}
                        if(first==null){if(late(b,1200)){report("p1-arrow-delay",false,"no arrow fired in 60 s");report("p1-arrow-visible",false,"no arrow fired");next(b,3);}break;}
                        firstHealth=lastHealth.getOrDefault(first.target(),first.target().getHealth());
                        firstDelayOk=first.fired()==b.time&&first.target().getHealth()==firstHealth;
                        System.out.println("[p1] first arrow fired="+first.fired()+" lands="+first.lands()+" health="+firstHealth+" now="+first.target().getHealth());
                    }
                    if(b.time==(first.fired()+first.lands())/2){
                        Vec3d from=first.from(),to=Projectiles.aim(first.target());double best=1e9;
                        for(var e:b.world.iterateEntities())if(e instanceof ArrowEntity&&e.getCommandTags().contains(Projectiles.TAG))best=Math.min(best,segmentDistance(e.getPos(),from,to));
                        report("p1-arrow-visible",best<=3,"nearest tagged arrow "+String.format("%.2f",best)+" blocks from the shot line at tick "+(b.time-first.fired())+" of "+(first.lands()-first.fired()));
                    }
                    if(b.time==first.lands()+1){
                        var t=first.target();boolean hurt=!t.isAlive()||t.getHealth()<firstHealth;
                        report("p1-arrow-delay",firstDelayOk&&hurt,"health at fire "+firstHealth+", unchanged on firing tick="+firstDelayOk+", after landing "+t.getHealth()+" alive="+t.isAlive());
                        next(b,3);
                    }
                }
                case 3->{ // screenshot while several arrows are mid-air, then every troop holds its fire
                    long arrows=b.shots.stream().filter(s->s.kind().equals("arrow")&&b.time>s.fired()).count();
                    if(wantShot==null&&(arrows>=3||late(b,200))){System.out.println("[p1] volley screenshot with "+arrows+" arrows in flight");wantShot="combat-volley";}
                    if(shotTaken){for(var u:b.units)freeze(u);System.out.println("[p1] volley stopped with "+Projectiles.inFlight(b)+" shots in flight");next(b,5);}
                }
                case 5->{if(b.time-stageAt>=40){int left=tagged(b);report("p1-arrow-cleanup",left==0&&b.shots.stream().noneMatch(s->s.kind().equals("arrow")),left+" tagged arrows 40 ticks after the volley stopped");next(b,6);}}
                case 6->{ // only one Dusk archer may shoot
                    chosen=b.units.stream().filter(u->u.squad.team==1&&u.spec.range()>3&&!u.spec.magic()&&u.mob.isAlive()).findFirst().orElse(null);
                    if(chosen==null){duskWhy="no Dusk archer alive";next(b,8);break;}
                    chosen.nextAttack=0;next(b,7);
                }
                case 7->{
                    if(duskShot==null){for(var s:b.shots)if(s.shooter()==chosen&&s.kind().equals("arrow")){duskShot=s;flashesBefore=BattleEffects.flashes;freeze(chosen);}
                        if(duskShot==null&&late(b,400)){duskWhy="the Dusk archer never fired";freeze(chosen);next(b,8);}break;}
                    if(b.time>=duskShot.lands()){
                        duskOk=BattleEffects.flashes>flashesBefore&&BattleEffects.lastFlashColor==Spec.FACTIONS[1].color();
                        duskWhy="Dusk arrow flashes +"+(BattleEffects.flashes-flashesBefore)+" colour "+Integer.toHexString(BattleEffects.lastFlashColor);next(b,8);
                    }
                }
                case 8->{ // only one Dawn soldier may strike, placed beside a Dusk troop
                    chosen=b.units.stream().filter(u->u.squad.team==0&&u.spec.range()<3&&u.mob.isAlive()).findFirst().orElse(null);
                    var foe=b.units.stream().filter(u->u.squad.team==1&&u.mob.isAlive()).min(Comparator.comparingDouble(u->u.spec.range())).orElse(null);
                    if(chosen==null||foe==null){report("p1-impact-team-colour",false,duskWhy+"; no Dawn soldier and Dusk foe alive");next(b,10);break;}
                    chosen.mob.refreshPositionAndAngles(foe.mob.getX()-1.5,foe.mob.getY(),foe.mob.getZ(),-90,0);chosen.nextAttack=0;flashesBefore=BattleEffects.flashes;next(b,9);
                }
                case 9->{
                    if(BattleEffects.flashes>flashesBefore){freeze(chosen);boolean dawnOk=BattleEffects.lastFlashColor==Spec.FACTIONS[0].color();
                        report("p1-impact-team-colour",duskOk&&dawnOk,duskWhy+"; Dawn melee colour "+Integer.toHexString(BattleEffects.lastFlashColor)+" (Dusk "+Integer.toHexString(Spec.FACTIONS[1].color())+", Dawn "+Integer.toHexString(Spec.FACTIONS[0].color())+")");next(b,10);}
                    else if(late(b,400)){freeze(chosen);report("p1-impact-team-colour",false,duskWhy+"; the Dawn soldier never struck");next(b,10);}
                }
                case 11->{ // a shot whose target vanishes, then a round that ends with a shot in the air
                    var dusk=b.squads.values().stream().filter(s->s.team==1).findFirst().orElseThrow();var dawn=b.squads.get(player);
                    double x=-40,z=3;
                    orphanShooter=b.spawn(dusk,troop("bonebow"),30,new Vec3d(x+8,top(b,x+8,z),z));
                    var gone=b.spawn(dawn,troop("knight"),30,new Vec3d(x-8,top(b,x-8,z-2),z-2));orphanSecond=b.spawn(dawn,troop("knight"),31,new Vec3d(x-8,top(b,x-8,z+2),z+2));
                    for(var u:List.of(orphanShooter,gone,orphanSecond))freeze(u);
                    Projectiles.fire(b,orphanShooter,gone.mob,"arrow",4);gone.mob.discard();next(b,12);
                }
                case 12->{if(b.time-stageAt>=2){
                    try{Projectiles.fire(b,orphanShooter,orphanSecond.mob,"arrow",4);System.out.println("[p1] orphan: "+Projectiles.inFlight(b)+" in flight at finish");b.campaign.finish(-1);}catch(Exception e){orphanError=e;e.printStackTrace();}
                    next(b,13);}}
                case 13->{int left=tagged(b);report("p1-shot-orphan",orphanError==null&&Projectiles.inFlight(b)==0&&left==0&&!b.running,"error="+orphanError+" inFlight="+Projectiles.inFlight(b)+" taggedArrows="+left+" running="+b.running);next(b,14);}
                case 14->{ // tick cost: eight bot armies fighting around the centre point
                    var p=sp(b);if(p==null||!skirmish(b,p,0,4,"normal"))return;
                    var own=b.squads.get(player);own.bot=true;own.order="attack";b.reinforce(own);
                    var centre=b.points.stream().min(Comparator.comparingDouble(c->Math.hypot(c.spec.x(),c.spec.z()))).orElseThrow().spec;
                    int[] n={0,0};
                    for(var u:b.units){int t=u.squad.team,i=n[t]++;double x=centre.x()+(t==0?-12:12)+(t==0?-1:1)*(i/10)*1.5,z=centre.z()+(i%10-4.5)*1.5;
                        u.mob.refreshPositionAndAngles(x,top(b,x,z),z,t==0?-90:90,0);}
                    for(var s:b.squads.values())if(s.seated){s.order="attack";s.target=new Vec3d(centre.x(),65,centre.z());}
                    troops=b.units.size();camera(b,new Vec3d(centre.x(),Math.max(63,Arena.ground(centre.x(),centre.z()))+1,centre.z()),24,14);
                    System.out.println("[p1] tick-cost fixture: "+b.squads.values().stream().filter(s->s.seated&&s.bot).count()+" bot armies, "+troops+" troops around "+centre.id());next(b,15);
                }
                case 15->{
                    tickSum+=b.server.getTickTime();tickSamples++;
                    if(b.time-stageAt>=600){double avg=tickSum/tickSamples;float now=b.server.getTickTime();
                        System.out.println("[p1] tick-cost avg="+String.format("%.2f",avg));
                        report("p1-tick-cost",now<40,"server tick "+String.format("%.2f",now)+" ms (600-tick mean "+String.format("%.2f",avg)+" ms) with "+troops+" troops at start, "+b.units.size()+" at end");
                        var mine=b.squads.get(player);if(mine!=null)mine.bot=false; // the commander's army fought as a bot here; the next suite's returnLobby drops bot armies, and with it his seat
                        next(b,16);}
                }
                default->{}
            }
            lastHealth.clear();for(var u:b.units)lastHealth.put(u.mob,u.mob.getHealth());
        }
    }

    /** Task 2: rout when weak or flanked, regroup, stored orders, RoutGuard, horns and the ROUTING HUD. Stages run on the server tick. */
    static final class MoraleChecks implements Suite,ServerWatch {
        static final Spec.MoraleRow M=Spec.MORALE[0];
        static final double FX=48,FZ=0; // open valley on the Dusk side of Rivergate (60 blocks from the sheet's Dusk base; CampaignMaps moves the live base to x=283, inside a village)
        volatile int stage;volatile String wantShot;volatile boolean finished,hudDone;
        volatile UUID player;long stageAt;int shotDelay;
        GravesAndGlory.Squad own,enemy,flankers;
        long routStart=-1,routEnd=-1;double startDistance;int routHorns,rallyHorns,guardCalls;
        boolean weakOk,orderStored;String weakWhy="";
        final List<GravesAndGlory.Unit> shadows=new ArrayList<>();
        final Morale.RoutGuard guard=(b,s,g)->{guardCalls++;return true;};

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        static int horns(String kind){return BattleEffects.horns.getOrDefault(kind,0);}
        List<GravesAndGlory.Unit> knights(GravesAndGlory.Battle b){return b.units.stream().filter(u->u.squad==own&&u.spec.group().equals("infantry")&&u.mob.isAlive()).toList();}
        double homeDistance(GravesAndGlory.Battle b){Vec3d home=GravesAndGlory.Battle.base(0);return knights(b).stream().mapToDouble(u->Math.hypot(u.mob.getX()-home.x,u.mob.getZ()-home.z)).average().orElse(0);}
        Vec3d centre(GravesAndGlory.Battle b){return Morale.centre(knights(b));}
        static void place(GravesAndGlory.Battle b,GravesAndGlory.Unit u,double x,double z,float yaw){u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(x,top(b,x,z),z,yaw,0);u.deployed=u.mob.getPos();u.lastPosition=u.mob.getPos();u.nextPath=0;u.stuckTicks=0;}
        void move(GravesAndGlory.Battle b,String group,double x,double z){own.assignments.put(group,new GravesAndGlory.Assignment("move","block",new Vec3d(x,top(b,x,z),z),-90));}
        /** A fresh, frozen, full infantry group standing in block at the fixture point, facing Dusk. */
        void freshInfantry(GravesAndGlory.Battle b){
            for(var u:knights(b))u.mob.discard();b.units.removeIf(u->u.squad==own&&u.spec.group().equals("infantry"));
            own.routedAt.remove("infantry");own.steadyUntil.remove("infantry");b.reinforce(own);move(b,"infantry",FX,FZ);
            for(var u:knights(b)){Vec3d at=b.formation(u);place(b,u,at.x,at.z,-90);freeze(u);}
        }
        /** Kills the rearmost knights (highest slots), leaving the front rank. */
        void kill(GravesAndGlory.Battle b,int n){var list=new ArrayList<>(knights(b));list.sort(Comparator.comparingInt(u->-u.slot));for(int i=0;i<n&&i<list.size();i++)list.get(i).mob.kill();b.units.removeIf(u->!u.mob.isAlive());}
        void enemyFront(GravesAndGlory.Battle b){enemy.target=new Vec3d(FX+6,top(b,FX+6,FZ),FZ);int i=0;for(var u:b.units)if(u.squad==enemy){double x=FX+6+(i/5)*2,z=FZ+(i%5-2)*2;place(b,u,x,z,90);i++;}}
        void camera(GravesAndGlory.Battle b,double x,double z,double up,float yaw,float pitch){var p=sp(b);p.changeGameMode(GameMode.SPECTATOR);b.relocate(p,x,top(b,x,z)+up,z,yaw,pitch);}
        /** Keeps three frozen Dusk troops beside the routing knights. */
        void shadow(GravesAndGlory.Battle b){if(knights(b).isEmpty())return;Vec3d at=centre(b);int i=0;for(var u:shadows)if(u.mob.isAlive())place(b,u,at.x+4,at.z+(i++-1)*2,90);}

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(wantShot!=null&&++shotDelay>=2){c.setScreen(null);shot(c,wantShot);wantShot=null;shotDelay=0;}
            if(!hudDone&&stage>=3&&stage<=5){
                var g=CommandMode.groups.stream().filter(v->v.id().equals("infantry")).findFirst().orElse(null);
                if(g!=null&&g.routing()&&wantShot==null){hudDone=true;wantShot="morale-hud";report("p1-hud-routing",true,"client infantry group routing=true, count "+g.count()+", stored order "+g.order());}
            }
            if(!hudDone&&stage>5){hudDone=true;report("p1-hud-routing",false,"the client never saw infantry routing");}
            if(stage==99&&!finished&&wantShot==null){finished=true;finish(c,"morale");}
            if(tick>12000&&!finished){finished=true;Morale.GUARDS.remove(guard);report("p1-morale-timeout",false,"stuck in stage "+stage);finish(c,"morale");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // fixture: practice on map 0, the player's army 60 blocks from the Dusk base, one frozen Dusk squad holding 6 blocks in front
                    var p=sp(b);if(p==null||!skirmish(b,p,0,1,"normal"))return;
                    own=b.squads.get(player);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    for(var u:b.units)if(u.squad!=own)u.mob.discard();b.units.removeIf(u->u.squad!=own);
                    own.order="move";own.formation="block";own.target=new Vec3d(FX,top(b,FX,FZ),FZ);own.yaw=-90;
                    move(b,"ranged",FX-8,FZ+24);move(b,"support",FX-8,FZ+30);
                    for(var u:b.units)if(u.squad==own&&!u.spec.group().equals("infantry")){Vec3d at=b.formation(u);place(b,u,at.x,at.z,-90);freeze(u);}
                    freshInfantry(b);
                    enemy=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(FX+6,65,FZ));enemy.order="hold";enemy.yaw=90;int slot=0;
                    for(var spec:Spec.TROOPS)if(spec.faction().equals(Spec.FACTIONS[1].id()))for(int n=0;n<spec.count();n++,slot++)freeze(b.spawn(enemy,spec,slot,new Vec3d(FX+6,65,FZ)));
                    enemyFront(b);
                    camera(b,FX-20,FZ+9,5,-105,18);
                    System.out.println("[p1] morale fixture at "+(int)FX+", "+(int)FZ+": "+knights(b).size()+" knights, fullHealth="+own.fullHealth+", "+b.units.stream().filter(u->u.squad==enemy).count()+" Dusk troops, map "+b.campaign.map);
                    next(b,2);
                }
                case 2->{if(since(b)>=40){routHorns=horns("rout");kill(b,10);System.out.println("[p1] rout-weak: killed 10, "+knights(b).size()+" knights left");next(b,3);}}
                case 3->{ // p1-rout-weak: routing within 40 ticks, rout horn, then running home
                    if(Morale.routing(own,"infantry")){
                        routStart=own.routedAt.get("infantry");startDistance=homeDistance(b);
                        // Watch from 14 blocks down the road home, looking back past the runners at the enemy.
                        Vec3d at=centre(b);double cx=at.x-14,cz=at.z+5;camera(b,cx,cz,4,(float)Math.toDegrees(Math.atan2(-(at.x-cx),at.z-cz)),15);
                        weakWhy="routed "+since(b)+" ticks after the losses, rout horns +"+(horns("rout")-routHorns);weakOk=since(b)<=40&&horns("rout")>routHorns;
                        next(b,4);
                    }else if(since(b)>40){report("p1-rout-weak",false,"no rout 40 ticks after killing 10 of 12 knights");report("p1-rout-order-kept",false,"no rout");report("p1-rout-ends",false,"no rout");next(b,7);}
                }
                case 4->{
                    if(since(b)==20){ // p1-rout-order-kept, part one: an order given mid-rout is stored while the rout still drives the group
                        var p=sp(b);own.lastOrder=-1000;b.tactical(p,"infantry","hold","block",FX-8,FZ,-90,-1);
                        var stored=own.assignments.get("infantry");var unit=knights(b).get(0);
                        orderStored=stored!=null&&stored.order.equals("hold")&&b.assignment(unit).order.equals("retreat");
                        System.out.println("[p1] mid-rout order: stored="+(stored==null?"none":stored.order)+" effective="+b.assignment(unit).order);
                    }
                    if(since(b)==40)wantShot="morale-rout";
                    if(since(b)==60){double d=homeDistance(b);
                        report("p1-rout-weak",weakOk&&startDistance-d>=4,weakWhy+"; mean distance to base "+String.format("%.1f",startDistance)+" -> "+String.format("%.1f",d)+" after 60 ticks");
                        rallyHorns=horns("rally");
                    }
                    if(since(b)==70){ // after the screenshot, which shows the enemy the knights are running from
                        // Camp the rest of the Dusk squad on the Dawn base and keep three frozen Dusk troops beside the runners,
                        // so neither reaching home nor shaking off pursuit can end the rout: only routMaxSeconds can.
                        Vec3d home=GravesAndGlory.Battle.base(0);enemy.target=home;int i=0;
                        for(var u:b.units)if(u.squad==enemy){if(shadows.size()<3&&u.spec.group().equals("infantry"))shadows.add(u);else{place(b,u,home.x+6+(i/5)*2,home.z+(i%5-2)*2,90);i++;}}
                        next(b,5);
                    }
                }
                case 5->{ // p1-rout-ends: the rout ends by routMaxSeconds; then no new rout during the regroup immunity
                    if(b.time%10==0)shadow(b);
                    if(routEnd<0&&!Morale.routing(own,"infantry")){routEnd=b.time;
                        var a=b.assignment(knights(b).get(0));
                        report("p1-rout-order-kept",orderStored&&a.order.equals("hold")&&Math.abs(a.target.x-(FX-8))<.01,"stored during rout="+orderStored+"; after regroup the infantry order is "+a.order+" at "+(int)a.target.x+", "+(int)a.target.z);
                        System.out.println("[p1] regroup after "+(routEnd-routStart)+" ticks, enemy within routSafeRadius="+Morale.enemyWithin(b,0,centre(b),M.routSafeRadius())+", rally horns +"+(horns("rally")-rallyHorns));
                    }
                    long limit=routStart+M.routMaxSeconds()*20L+40;
                    if(routEnd<0&&b.time>limit){report("p1-rout-ends",false,"still routing "+(b.time-routStart)+" ticks after it began");report("p1-rout-order-kept",false,"the rout never ended");next(b,7);}
                    else if(routEnd>=0&&Morale.routing(own,"infantry")){report("p1-rout-ends",false,"routed again "+(b.time-routEnd)+" ticks after regrouping");next(b,7);}
                    else if(routEnd>=0&&b.time>=routEnd+M.regroupImmunitySeconds()*20L-1){
                        report("p1-rout-ends",routEnd<=limit&&horns("rally")>rallyHorns&&Morale.enemyWithin(b,0,centre(b),M.engageCheckRadius()),"rout lasted "+(routEnd-routStart)+" ticks (max "+M.routMaxSeconds()*20+"), rally horns +"+(horns("rally")-rallyHorns)+", held "+(b.time-routEnd)+" ticks of immunity with "+knights(b).size()+" knights and Dusk troops in range");
                        next(b,6);
                    }
                }
                case 6->{shadow(b);if(since(b)>=40){System.out.println("[p1] after immunity: routing again="+Morale.routing(own,"infantry"));next(b,7);}}
                case 7->{ // p1-flank-rout: half strength alone holds; four attackers from behind break it
                    shadows.clear();enemyFront(b);freshInfantry(b);next(b,8);
                }
                case 8->{if(since(b)>=20){kill(b,6);next(b,9);}}
                case 9->{if(since(b)>=40){
                    if(Morale.routing(own,"infantry")){report("p1-flank-rout",false,"half-strength group routed before it was flanked");next(b,11);break;}
                    Vec3d at=centre(b);flankers=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(at.x-5,at.y,at.z));flankers.order="hold";flankers.yaw=-90;
                    var revenant=troop("revenant");var ks=knights(b);
                    for(int i=0;i<4;i++){double z=at.z-1.5+i;var u=b.spawn(flankers,revenant,40+i,new Vec3d(at.x-5,top(b,at.x-5,z),z));freeze(u);
                        var nearest=ks.stream().min(Comparator.comparingDouble(k->k.mob.squaredDistanceTo(u.mob))).orElseThrow();u.mob.setTarget(nearest.mob);}
                    System.out.println("[p1] flank: not routing at half strength; 4 revenants placed 5 blocks behind "+ks.size()+" knights");next(b,10);
                }}
                case 10->{
                    if(Morale.routing(own,"infantry")){report("p1-flank-rout",true,"held at half strength for 40 ticks, routed "+since(b)+" ticks after four attackers came from behind");next(b,11);}
                    else if(since(b)>40){report("p1-flank-rout",false,"no rout within 40 ticks of the flank");next(b,11);}
                }
                case 11->{ // p1-rout-guard: a guard that holds keeps a broken group from routing
                    for(var u:b.units)if(u.squad==flankers)u.mob.discard();b.units.removeIf(u->u.squad==flankers);
                    enemyFront(b);freshInfantry(b);guardCalls=0;routHorns=horns("rout");Morale.GUARDS.add(guard);kill(b,10);next(b,12);
                }
                case 12->{if(since(b)>=60){boolean routed=Morale.routing(own,"infantry");Morale.GUARDS.remove(guard);
                    report("p1-rout-guard",!routed&&guardCalls>0&&horns("rout")==routHorns,"routing="+routed+" with "+knights(b).size()+" knights; guard consulted "+guardCalls+" times; rout horns +"+(horns("rout")-routHorns));next(b,13);}}
                case 13->{freshInfantry(b);enemyFront(b);camera(b,FX-12,FZ,4,-90,12);next(b,14);}
                case 14->{if(since(b)>=20){ // p1-charge-horn: a charge sounds once; another charge inside the cooldown is silent
                    var p=sp(b);var foe=b.units.stream().filter(u->u.squad==enemy&&u.mob.isAlive()&&p.canSee(u.mob)).findFirst().orElse(null);
                    if(foe==null){report("p1-charge-horn",false,"no Dusk troop visible from the camera");next(b,99);break;}
                    int before=horns("charge");own.lastOrder=-1000;b.tactical(p,"infantry","charge","block",foe.mob.getX(),foe.mob.getZ(),-90,foe.mob.getId());int first=horns("charge");
                    own.lastOrder=-1000;b.tactical(p,"ranged","charge","block",foe.mob.getX(),foe.mob.getZ(),-90,foe.mob.getId());int second=horns("charge");
                    var ranged=own.assignments.get("ranged");
                    report("p1-charge-horn",first==before+1&&second==first&&ranged!=null&&ranged.order.equals("charge"),"charge horns "+before+" -> "+first+" after the infantry charge, "+second+" after the ranged charge inside the "+M.hornCooldownSeconds()+" s cooldown (ranged order "+(ranged==null?"none":ranged.order)+")");
                    next(b,99);
                }}
                default->{}
            }
        }
    }

    /** Task 3: forward reinforcement from a held point, the walk measured before and after, and the fallbacks to the barracks. Stages run on the server tick. */
    static final class ReinforceChecks implements Suite,ServerWatch {
        static final Spec.EconomyRow E=Spec.ECONOMY[0];
        static final int TIMEOUT=2400; // ticks a measured walk may take
        static final String[] PROBES={"p1-forward-contested","p1-forward-enemy-near","p1-forward-flipped","p1-forward-fresh"};
        volatile int stage;volatile String wantShot;volatile boolean finished;
        volatile UUID player;long stageAt;int shotDelay;
        final Spec.TroopsRow knight=troop("knight");
        GravesAndGlory.Squad own;GravesAndGlory.Capture point,anchor;
        Set<GravesAndGlory.Unit> known=new HashSet<>();GravesAndGlory.Unit trained,foe;long appeared;Vec3d spawnAt;
        double before,after;boolean shotAsked,control,heldOk;int probe;
        GravesAndGlory.Unit garrison;long captured=-1;
        Spec.TroopsRow support;double fullOne,fullTwo,fullThree;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        static double flat(Vec3d a,double x,double z){return Math.hypot(a.x-x,a.z-z);}
        /** A spectator camera at (x,y,z) looking at the given spot. */
        void camera(GravesAndGlory.Battle b,double x,double y,double z,Vec3d look){
            var p=sp(b);p.changeGameMode(GameMode.SPECTATOR);double dx=look.x-x,dz=look.z-z;
            b.relocate(p,x,y,z,(float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(look.y-y,Math.hypot(dx,dz))));
        }
        void fail(GravesAndGlory.Battle b,String id,String why){report(id,false,why);next(b,99);}
        /** Disbands one knight and queues a replacement that is due now, so the next training tick spawns it and makes the deploy decision. */
        boolean buy(GravesAndGlory.Battle b){
            var gone=b.units.stream().filter(u->u.squad==own&&u.spec==knight&&u.mob.isAlive()).findFirst().orElse(null);if(gone==null)return false;
            gone.mob.discard();b.units.remove(gone);known=new HashSet<>(b.units);trained=null;
            own.training.addLast(new GravesAndGlory.Training(knight,b.time));return true;
        }
        /** True once the bought troop exists; records when and where it appeared. */
        boolean arrived(GravesAndGlory.Battle b){
            if(trained==null)for(var u:b.units)if(u.squad==own&&u.mob.isAlive()&&!known.contains(u)){trained=u;appeared=b.time;spawnAt=u.mob.getPos();break;}
            return trained!=null;
        }
        /** Ticks from appearing until the troop stands within arrivalTolerance+3 of its formation point, or -1 while it has not. */
        long walked(GravesAndGlory.Battle b){Vec3d goal=b.formation(trained);return flat(trained.mob.getPos(),goal.x,goal.z)<=Spec.NAVIGATION[0].arrivalTolerance()+3?b.time-appeared:-1;}
        /** Puts the middle point in the state that makes it an anchor; returns whether it now is one. */
        boolean ideal(GravesAndGlory.Battle b){
            b.forwardReinforcement=true;point.owner=0;point.ownedSince=b.time-E.forwardHoldSeconds()*20L-1;point.contested=false;
            if(foe!=null){foe.mob.discard();b.units.remove(foe);foe=null;}
            return b.reinforcementAnchors(0).contains(point);
        }
        void drop(GravesAndGlory.Battle b){if(trained!=null){trained.mob.discard();b.units.remove(trained);trained=null;}}
        void disband(GravesAndGlory.Battle b,GravesAndGlory.Unit u){if(u!=null){u.mob.discard();b.units.remove(u);}}
        long supporters(GravesAndGlory.Battle b){return b.units.stream().filter(u->u.squad==own&&u.spec==support&&u.mob.isAlive()).count();}
        double fullHealth(){return own.fullHealth.getOrDefault("support",0.0);}

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(wantShot!=null&&++shotDelay>=2){c.setScreen(null);shot(c,wantShot);wantShot=null;shotDelay=0;}
            if(stage==99&&!finished&&wantShot==null){finished=true;finish(c,"reinforce");}
            if(tick>9000&&!finished){finished=true;report("p1-reinforce-timeout",false,"stuck in stage "+stage);finish(c,"reinforce");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // fixture: practice on map 0; the Dusk army is removed and inert; the player's army marches on a spot 40 blocks past the middle point
                    var p=sp(b);if(p==null||!skirmish(b,p,0,1,"normal"))return;
                    own=b.squads.get(player);point=b.points.get(1);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    for(var u:b.units)if(u.squad!=own)u.mob.discard();b.units.removeIf(u->u.squad!=own);
                    double gx=point.spec.x()+40,gz=point.spec.z();
                    own.order="move";own.formation="block";own.target=new Vec3d(gx,Arena.ground((int)gx,(int)gz)+1,gz);own.yaw=-90;
                    System.out.println("[p1] reinforce fixture: map "+b.campaign.map+", "+b.points.size()+" points, middle point "+point.spec.name()+" at "+point.spec.x()+", "+point.spec.z()+", Dawn base "+GravesAndGlory.Battle.base(0)+", army ordered to "+(int)gx+", "+(int)gz+", "+b.units.size()+" troops");
                    next(b,2);
                }
                case 2->{ // before: forward reinforcement off, so the new knight walks from the barracks
                    b.forwardReinforcement=false;
                    if(!buy(b)){fail(b,"p1-reinforce-walk","no living knight to disband");break;}
                    next(b,3);
                }
                case 3->{
                    if(!arrived(b)){if(since(b)>100)fail(b,"p1-reinforce-walk","the bought knight never appeared (before)");break;}
                    long t=walked(b);
                    if(t>=0||b.time-appeared>=TIMEOUT){before=(t>=0?t:TIMEOUT)/20.0;System.out.println("[p1] reinforce before: appeared at "+spawnAt+", "+(t>=0?"arrived":"TIMED OUT")+" after "+(b.time-appeared)+" ticks");drop(b);next(b,4);}
                }
                case 4->{ // the point is now held: put the camera above it and give the client time to load the chunks
                    Arena.flag(b.world,point.spec,0);
                    if(!ideal(b)){fail(b,"p1-reinforce-walk","the middle point did not become an anchor");break;}
                    double g=Arena.ground(point.spec.x(),point.spec.z());
                    camera(b,point.spec.x()-10,g+4,point.spec.z()+12,new Vec3d(point.spec.x()-9,g+2.5,point.spec.z()-3));
                    next(b,5);
                }
                case 5->{if(since(b)>=80){
                    if(!ideal(b)||!buy(b)){fail(b,"p1-reinforce-walk","the point stopped being an anchor or no knight to disband");break;}
                    next(b,6);
                }}
                case 6->{ // after: forward reinforcement on, the point held 10 s: the new knight appears at the point
                    if(!arrived(b)){if(since(b)>100)fail(b,"p1-reinforce-walk","the bought knight never appeared (after)");break;}
                    if(!shotAsked){shotAsked=true;anchor=b.deploy(own,trained.slot).anchor();wantShot="reinforce-forward";
                        System.out.println("[p1] reinforce after: appeared at "+spawnAt+", "+String.format("%.1f",flat(spawnAt,point.spec.x(),point.spec.z()))+" blocks from the point centre, anchor "+(anchor==null?"none":anchor.spec.name()));}
                    long t=walked(b);
                    if(t>=0||b.time-appeared>=TIMEOUT){after=(t>=0?t:TIMEOUT)/20.0;
                        System.out.println("[p1] reinforce-walk before="+String.format("%.1f",before)+"s after="+String.format("%.1f",after)+"s");
                        report("p1-reinforce-walk",t>=0&&anchor==point&&after<before&&after<=20,"walk to the formation point "+String.format("%.1f",before)+" s from the barracks, "+String.format("%.1f",after)+" s from "+(anchor==null?"no anchor":anchor.spec.name())+" (needs after < before and <= 20 s)");
                        drop(b);next(b,7);}
                }
                case 7->{ // one disturbance per probe on an otherwise ideal point; the troop must deploy at the barracks
                    if(probe>=PROBES.length){next(b,8);break;}
                    control=ideal(b);
                    switch(probe){
                        case 0->point.contested=true;
                        case 1->{double z=point.spec.z()+10;var dusk=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(point.spec.x(),65,z));dusk.order="hold";dusk.yaw=90;
                            foe=b.spawn(dusk,troop("revenant"),30,new Vec3d(point.spec.x(),top(b,point.spec.x(),z),z));freeze(foe);foe.deployed=foe.mob.getPos();}
                        case 2->point.owner=1;
                        default->point.ownedSince=b.time;
                    }
                    if(!buy(b)){fail(b,PROBES[probe],"no living knight to disband");break;}
                    next(b,71);
                }
                case 71->{
                    if(!arrived(b)){if(since(b)>100){report(PROBES[probe],false,"the bought knight never appeared");probe++;next(b,7);}break;}
                    Vec3d home=GravesAndGlory.Battle.base(0);double fromBase=flat(spawnAt,home.x,home.z),fromPoint=flat(spawnAt,point.spec.x(),point.spec.z());
                    report(PROBES[probe],control&&fromBase<=30,"point an anchor before the change="+control+"; knight appeared "+String.format("%.1f",fromBase)+" blocks from the barracks spot and "+String.format("%.1f",fromPoint)+" from the point (owner "+point.owner+", contested "+point.contested+")");
                    drop(b);probe++;next(b,7);
                }
                case 8->{ // p1-forward-message: the deploy decision names the point, or the barracks. Since UI Task 3 the player's deploy
                    // notice is the feed's "Dawn Knight deploying" line (p1-ui-feed-deploy), so the old notice text, deployNote, is retired (UI polish M15)
                    boolean anchorNow=ideal(b);var fwd=b.deploy(own,0);b.forwardReinforcement=false;var home=b.deploy(own,0);b.forwardReinforcement=true;Vec3d base=GravesAndGlory.Battle.base(0);
                    report("p1-forward-message",anchorNow&&anchor==point&&fwd.anchor()==point&&home.anchor()==null&&flat(home.at(),base.x,base.z)<=10,"the anchored spawn's decision named "+(anchor==null?"no point":anchor.spec.name())+" (the middle point is "+point.spec.name()+"); with the point held again a slot-0 decision deploys at "+(fwd.anchor()==null?"the barracks":fwd.anchor().spec.name())+", and with forward reinforcement off at "+(home.anchor()==null?"the barracks":home.anchor().spec.name())+", "+String.format("%.1f",flat(home.at(),base.x,base.z))+" blocks from the barracks spot; the player reads the feed's \""+FeedLines.deploying(1,knight.name())+"\" line");
                    // p1-forward-held, nothing faked: a lone Dawn knight on a neutral point captures it through captureTick, which stamps ownedSince
                    point.owner=-1;point.challenger=-1;point.progress=0;point.contested=false;point.ownedSince=0;Arena.flag(b.world,point.spec,-1);
                    var holder=new GravesAndGlory.Squad(UUID.randomUUID(),0,new Vec3d(point.spec.x(),65,point.spec.z()));holder.order="hold";holder.yaw=-90;
                    garrison=b.spawn(holder,knight,31,new Vec3d(point.spec.x(),top(b,point.spec.x(),point.spec.z()),point.spec.z()));freeze(garrison);garrison.deployed=garrison.mob.getPos();next(b,81);
                }
                case 81->{
                    if(captured<0){
                        if(point.owner==0){captured=b.time;heldOk=point.ownedSince==b.time&&!b.reinforcementAnchors(0).contains(point);}
                        else if(since(b)>400){report("p1-forward-held",false,"the garrison never captured the point in 400 ticks");disband(b,garrison);next(b,82);}
                        break;
                    }
                    long held=b.time-captured,need=E.forwardHoldSeconds()*20L;
                    if(held==need-1)heldOk&=!b.reinforcementAnchors(0).contains(point);
                    if(held==need){boolean anchorNow=b.reinforcementAnchors(0).contains(point);
                        report("p1-forward-held",heldOk&&anchorNow,"captured at tick "+captured+" (ownedSince "+point.ownedSince+"); not an anchor for the first "+need+" ticks="+heldOk+", an anchor at "+held+" ticks held="+anchorNow);
                        disband(b,garrison);next(b,82);}
                }
                case 82->{
                    // the Task 2 review fix: a group made only of trained troops still gets a health ceiling
                    support=Arrays.stream(Spec.TROOPS).filter(t->t.faction().equals(Spec.FACTIONS[0].id())&&t.group().equals("support")).findFirst().orElseThrow();
                    for(var u:b.units)if(u.squad==own&&u.spec.group().equals("support"))u.mob.discard();b.units.removeIf(u->!u.mob.isAlive()||u.mob.isRemoved());
                    own.fullHealth.remove("support");own.training.addLast(new GravesAndGlory.Training(support,b.time));next(b,9);
                }
                case 9->{if(supporters(b)==1){fullOne=fullHealth();own.training.addLast(new GravesAndGlory.Training(support,b.time));next(b,10);}else if(since(b)>100){fail(b,"p1-trained-fullhealth","the first support troop never appeared");}}
                case 10->{if(supporters(b)==2){fullTwo=fullHealth();
                    for(var u:b.units)if(u.squad==own&&u.spec==support&&u.mob.isAlive()){u.mob.kill();break;}b.units.removeIf(u->!u.mob.isAlive()||u.mob.isRemoved());
                    own.training.addLast(new GravesAndGlory.Training(support,b.time));next(b,11);}else if(since(b)>100){fail(b,"p1-trained-fullhealth","the second support troop never appeared");}}
                case 11->{if(supporters(b)==2){fullThree=fullHealth();double one=support.health()*b.campaign.health(own,support);
                    report("p1-trained-fullhealth",Math.abs(fullOne-one)<1e-6&&Math.abs(fullTwo-2*one)<1e-6&&Math.abs(fullThree-2*one)<1e-6,"support ceiling after 1, 2 and a replaced troop: "+fullOne+", "+fullTwo+", "+fullThree+" (one troop is "+one+")");
                    next(b,99);}else if(since(b)>100){fail(b,"p1-trained-fullhealth","the replacement support troop never appeared");}}
                default->{}
            }
        }
    }

    /** Task 4: bot difficulty tiers. The command, the lobby and camp buttons and the Hard cadence first (map 0), then Hard's decisions on Kingsfield Crossroads (map 4, five points). Stages run on the server tick; the UI stages run on the client tick. */
    static final class Bots implements Suite,ServerWatch {
        volatile int stage;volatile String wantShot;volatile boolean finished,shotAsked;
        volatile UUID player;long stageAt;int shotDelay,lobbyAt=-1,homeAt=-1;
        boolean lobbyOk,commandOk,homeOk,buttonOk,tiersOk,refusedOk;String lobbyWhy="",homeWhy="",tiersWhy="";
        final Map<UUID,Vec3d> firstTarget=new HashMap<>();final Map<UUID,Long> decidedAt=new HashMap<>();
        GravesAndGlory.Squad hard;List<GravesAndGlory.Unit> foes=new ArrayList<>();
        int hornsBefore;String farOrder="";boolean shieldFormation;double gap;
        GravesAndGlory.Unit hexer,aim;final Map<GravesAndGlory.Unit,Float> hpBefore=new HashMap<>();final Set<GravesAndGlory.Unit> bracedBefore=new HashSet<>();boolean frontalBlocked;String frontalWhy="";

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        static ButtonWidget button(Screen screen,String prefix){
            if(screen!=null)for(var e:screen.children())if(e instanceof ButtonWidget w&&w.getMessage().getString().startsWith(prefix))return w;
            return null;
        }
        /** Stops the real armies fighting or refilling, and removes every troop and remembered sighting. */
        void clear(GravesAndGlory.Battle b){
            for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
            for(var u:b.units)u.mob.discard();b.units.clear();
            for(var memory:b.sightings.values())memory.clear();
            for(var p:b.points){p.owner=-1;p.contested=false;p.progress=0;p.challenger=-1;}
        }
        /** A synthetic bot army (not in b.squads, so only the fixture decides for it): the lineup of the team's starting troops, frozen, in rows behind (cx,cz). */
        GravesAndGlory.Squad army(GravesAndGlory.Battle b,int team,String tier,double cx,double cz){
            var s=new GravesAndGlory.Squad(UUID.randomUUID(),team,new Vec3d(cx,top(b,cx,cz),cz));
            s.bot=true;s.tier=tier;s.lane=1;s.order="hold";s.yaw=team==0?-90:90;int slot=0;
            for(var spec:Spec.TROOPS)if(spec.faction().equals(Spec.FACTIONS[team].id())&&spec.count()>0)
                for(int n=0;n<spec.count();n++,slot++)freeze(b.spawn(s,spec,slot,new Vec3d(cx,top(b,cx,cz),cz)));
            place(b,s,cx,cz);return s;
        }
        /** Stands the army's rows behind the front rank at (cx,cz), facing the enemy. */
        void place(GravesAndGlory.Battle b,GravesAndGlory.Squad s,double cx,double cz){
            int back=s.team==0?-1:1;
            for(var u:b.units)if(u.squad==s){double x=cx+back*(u.slot/5)*1.8,z=cz+(u.slot%5-2)*1.8;
                u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(x,top(b,x,z),z,s.team==0?-90:90,0);u.deployed=u.mob.getPos();u.lastPosition=u.mob.getPos();u.nextPath=0;u.stuckTicks=0;}
        }
        static Vec3d centre(GravesAndGlory.Battle b,GravesAndGlory.Squad s){return Morale.centre(b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).toList());}
        /** Frozen foes of the given kind in rows of four, the front rank at x, centred on z. */
        void spawnFoes(GravesAndGlory.Battle b,String id,int count,double x,double z){
            for(var u:foes)u.mob.discard();b.units.removeAll(foes);foes.clear();
            var squad=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(x,top(b,x,z),z));squad.order="hold";squad.yaw=90;
            for(int i=0;i<count;i++){double fx=x+(i/4)*2,fz=z+(i%4-1.5)*2;foes.add(freezeIt(b.spawn(squad,troop(id),i,new Vec3d(fx,top(b,fx,fz),fz))));}
        }
        static GravesAndGlory.Unit freezeIt(GravesAndGlory.Unit u){freeze(u);return u;}
        /** Index of the point the bot's target sits on, or -1. */
        static int chosen(GravesAndGlory.Battle b,GravesAndGlory.Squad s){
            for(int i=0;i<b.points.size();i++)if(Math.abs(b.points.get(i).spec.x()-s.target.x)<.01&&Math.abs(b.points.get(i).spec.z()-s.target.z)<.01)return i;
            return -1;
        }
        static String name(GravesAndGlory.Battle b,int i){return i<0?"none":b.points.get(i).spec.name()+" ("+i+")";}
        /** Puts n frozen Dusk revenants on a point and plants them in the Dawn team's memory. The bots only count sightings of units that are still alive, so the sightings carry the units' own ids. */
        void plant(GravesAndGlory.Battle b,int point,int n){
            var at=b.points.get(point).spec;spawnFoes(b,"revenant",n,at.x(),at.z());
            var memory=b.sightings.computeIfAbsent(0,k->new HashMap<>());
            for(var u:foes)memory.put(u.mob.getUuid(),new GravesAndGlory.Battle.Sight((float)u.mob.getX(),(float)u.mob.getZ(),b.time));
        }
        /** What Dawn sees of the Dusk units now: its memory is wiped, the sight pass runs, and the remembered count comes back. */
        int see(GravesAndGlory.Battle b){b.sightings.computeIfAbsent(0,k->new HashMap<>()).clear();b.updateSight();return b.sightings.get(0).size();}
        /** A spectator camera at (x,z) looking along yaw/pitch, up blocks above the ground. */
        void camera(GravesAndGlory.Battle b,double x,double z,double up,float yaw,float pitch){var p=sp(b);p.changeGameMode(GameMode.SPECTATOR);b.relocate(p,x,top(b,x,z)+up,z,yaw,pitch);}

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(wantShot!=null&&++shotDelay>=2){shot(c,wantShot);wantShot=null;shotDelay=0;}
            if(stage==2){ // the war council, as the lobby lead: Bot fill and Bots share the row
                if(lobbyAt==-1&&CampaignClient.lead){lobbyAt=tick;c.setScreen(new GravesAndGloryClient.LobbyScreen());}
                else if(lobbyAt>=0&&tick==lobbyAt+25){
                    var bots=button(c.currentScreen,"Bots: ");var fill=button(c.currentScreen,"Bot fill");
                    lobbyOk=bots!=null&&fill!=null&&bots.getMessage().getString().equals("Bots: Normal")&&bots.active&&fill.active&&fill.getX()+fill.getWidth()<=bots.getX();
                    lobbyWhy=bots==null||fill==null?"a button is missing":"\""+fill.getMessage().getString()+"\" and \""+bots.getMessage().getString()+"\", active="+bots.active+", "+fill.getWidth()+" + "+bots.getWidth()+" px side by side";
                    wantShot="bots-lobby";
                }
                else if(lobbyAt>=0&&tick==lobbyAt+30){c.setScreen(null);lobbyAt=-2;server(c,b->next(b,3));}
            }
            if(stage==4){ // the camp's match page after difficulty:hard (the Bots button left the home with UI Task 5), then a real press of the Bots button
                if(homeAt==-1){homeAt=tick;CampScreen.open("match");}
                else if(tick==homeAt+25){
                    var bots=button(c.currentScreen,"Bots: ");
                    homeOk=c.currentScreen instanceof CampScreen&&bots!=null&&bots.getMessage().getString().equals("Bots: Hard")&&bots.active;
                    homeWhy=bots==null?"no Bots button on the camp's match page":"\""+bots.getMessage().getString()+"\", active="+bots.active;
                    wantShot="bots-difficulty";
                }
                else if(tick==homeAt+30){var bots=button(c.currentScreen,"Bots: ");if(bots!=null)bots.onPress();homeAt=-2;server(c,b->next(b,5));}
            }
            if(stage>=6&&c.currentScreen!=null)c.setScreen(null);
            if(stage==99&&!finished&&wantShot==null){finished=true;finish(c,"bots");}
            if(tick>12000&&!finished){finished=true;report("p1-bots-timeout",false,"stuck in stage "+stage);finish(c,"bots");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // map 0 camp: open a custom lobby so the player leads it
                    var p=sp(b);if(p==null||!b.ready)return;
                    if(b.running){b.campaign.returnLobby();stageAt=b.time;return;} // an earlier suite's skirmish still runs: back to camp first
                    if(since(b)<40)return; // the client hears that the round and its lobby are over before a new lobby opens
                    b.command(p,"party:open");System.out.println("[p1] bots start: map "+b.campaign.map+", difficulty "+b.campaign.difficulty+", lead="+b.campaign.leader(p));next(b,2);
                }
                case 3->{ // the command, once no lobby needs a leader
                    var p=sp(b);b.command(p,"party:leave");b.command(p,"difficulty:hard");
                    commandOk=b.campaign.difficulty.equals("hard");System.out.println("[p1] difficulty:hard -> "+b.campaign.difficulty);next(b,4);
                }
                case 5->{ // the Bots button was pressed on the client: hard -> easy; put it back on hard by command
                    if(b.campaign.difficulty.equals("easy")){buttonOk=true;b.command(sp(b),"difficulty:hard");System.out.println("[p1] Bots button moved the tier hard -> easy; command set it back to "+b.campaign.difficulty);next(b,6);}
                    else if(since(b)>100){System.out.println("[p1] Bots button press did not reach the server; difficulty is "+b.campaign.difficulty);b.command(sp(b),"difficulty:hard");next(b,6);}
                }
                case 6->{ // a solo pad 3v3 match: every bot takes the tier; the command is refused while it runs
                    var c=b.campaign;c.closeParty();c.onPad.clear();c.onPad.put(player,0);c.mapVotes.clear();c.startQuickMatch();c.introUntil=b.time;
                    var bots=b.squads.values().stream().filter(s->s.bot).toList();
                    tiersOk=b.running&&bots.size()==5&&bots.stream().allMatch(s->s.tier.equals("hard"));
                    tiersWhy=bots.size()+" bots with tiers "+bots.stream().map(s->s.tier).toList()+" (running="+b.running+", "+c.size+"v"+c.size+")";
                    b.command(sp(b),"difficulty:easy");refusedOk=c.difficulty.equals("hard");
                    for(var s:bots)firstTarget.put(s.owner,s.target);
                    report("p1-bots-tiers",lobbyOk&&commandOk&&homeOk&&buttonOk&&tiersOk&&refusedOk,"lobby: "+lobbyWhy+"; difficulty:hard set it="+commandOk+"; camp match page: "+homeWhy+"; button press reached the server="+buttonOk+"; solo pad 3v3: "+tiersWhy+"; difficulty:easy refused while running (still "+c.difficulty+")="+refusedOk);
                    next(b,7);
                }
                case 7->{ // Hard decides on its 60-tick beat: each bot's first order change lands on a multiple of 60
                    for(var s:b.squads.values())if(s.bot&&!decidedAt.containsKey(s.owner)&&!s.target.equals(firstTarget.get(s.owner)))decidedAt.put(s.owner,b.time);
                    if(decidedAt.size()==firstTarget.size()||since(b)>=130){
                        boolean ok=decidedAt.size()==firstTarget.size()&&decidedAt.values().stream().allMatch(t->t%60==0);
                        report("p1-bots-cadence",ok,decidedAt.size()+" of "+firstTarget.size()+" Hard bots chose a point; first decision ticks "+decidedAt.values()+" (every one must be a multiple of 60)");
                        b.campaign.returnLobby();next(b,8);
                    }
                }
                case 8->{if(since(b)>=40)next(b,9);} // the client learns the round is over before the map changes under it
                case 9->{ // Kingsfield Crossroads, a camera over the shieldwall ground, everything else inert
                    var p=sp(b);if(p==null||!skirmish(b,p,4,1,"normal"))return;
                    clear(b);camera(b,-92,11,2,180,6);
                    System.out.println("[p1] bots fixture: map "+b.campaign.map+" "+Spec.MAPS[b.campaign.map].name()+", "+b.points.size()+" points: "+b.points.stream().map(c->c.spec.id()+"@"+c.spec.x()+","+c.spec.z()).toList());
                    next(b,10);
                }
                case 10->{ // p1-bots-least-defended
                    clear(b);hard=army(b,0,"hard",-105,100); // 25 blocks east of West Outpost (index 1)
                    var shipped=b.campaign.tier(hard);var row=new Spec.BotCommandRow(shipped.id(),shipped.retreatShare(),shipped.rallyShare(),shipped.chargeRatio(),shipped.decisionTicks(),true,shipped.defenderWeight(),shipped.shieldwallVsArchers(),shipped.archerThreatCount(),shipped.archerThreatRange(),shipped.chargeDistance()); // the flanking rule stays tested; the shipped Hard row has it off (Kort, 2026-10-18)
                    b.campaign.botDecide(hard,row);int none=chosen(b,hard);
                    plant(b,1,9);b.campaign.botDecide(hard,row);int defended=chosen(b,hard);
                    for(var u:foes)u.mob.discard(); // the nine fall, but the memory of them stays
                    b.campaign.botDecide(hard,row);int stale=chosen(b,hard);b.units.removeAll(foes);foes.clear();
                    hard.tier="normal";var normalRow=b.campaign.tier(hard);b.campaign.botDecide(hard,normalRow);int normal=chosen(b,hard);
                    hard.tier="hard";for(var memory:b.sightings.values())memory.clear();
                    var east=b.points.get(4).spec;place(b,hard,east.x()-25,east.z());b.campaign.botDecide(hard,row);int reach=chosen(b,hard);
                    // A Dusk Normal army on the centre point, lane 1, holding points 0, 2 and 4: West Outpost (1) and its mirror (3) are about as far as each other, so only the mirrored Dusk lane picks 3.
                    var dusk=army(b,1,"normal",0,0);for(int i:new int[]{0,2,4})b.points.get(i).owner=1;
                    b.campaign.botDecide(dusk,b.campaign.tier(dusk));int duskNormal=chosen(b,dusk);
                    for(var u:b.units)if(u.squad==dusk)u.mob.discard();b.units.removeIf(u->u.squad==dusk);for(var p:b.points)p.owner=-1;
                    boolean ok=b.points.size()==5&&row.id().equals("hard")&&none==1&&defended>=0&&defended<5&&defended!=1&&stale==1&&normal==1&&reach==4&&duskNormal==3;
                    report("p1-bots-least-defended",ok,b.points.size()+" points; Hard near "+name(b,1)+" with no sightings -> "+name(b,none)+", with nine Dusk sightings on it -> "+name(b,defended)+", with those nine dead but still remembered -> "+name(b,stale)+"; Normal on the same state -> "+name(b,normal)+" (expected "+name(b,1)+"); Hard near "+name(b,4)+" -> "+name(b,reach)+" (index above 2 needs the arrays sized from battle.points); Dusk Normal on "+name(b,2)+", lane 1, holding points 0, 2 and 4 -> "+name(b,duskNormal)+" (expected "+name(b,3)+", lane 1 mirrored)");
                    next(b,11);
                }
                case 11->{if(since(b)>=40){ // p1-bots-shieldwall: six frozen Dusk archers 20 blocks ahead of a Dawn Hard army
                    clear(b);hard=army(b,0,"hard",-100,0);Vec3d at=centre(b,hard);
                    for(var u:foes)u.mob.discard();foes.clear();
                    var duskSquad=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(-80,top(b,-80,0),0));duskSquad.order="hold";duskSquad.yaw=90;
                    for(int i=0;i<6;i++){double z=(i-2.5)*2;foes.add(freezeIt(b.spawn(duskSquad,troop("bonebow"),i,new Vec3d(-80,top(b,-80,z),z))));}
                    b.updateSight();int sighted=b.sightings.get(0).size();gap=-80-b.units.stream().filter(u->u.squad==hard).mapToDouble(u->u.mob.getX()).max().orElse(0);
                    hard.tier="normal";b.campaign.botDecide(hard,b.campaign.tier(hard));boolean normalPlain=!hard.assignments.containsKey("infantry");
                    hard.tier="hard";b.campaign.botDecide(hard,b.campaign.tier(hard));var wall=hard.assignments.get("infantry");
                    boolean formation=wall!=null&&wall.formation.equals("shieldwall");
                    System.out.println("[p1] shieldwall fixture: army centre "+at+", "+sighted+" Dusk sightings, Normal on the same state leaves infantry in "+hard.formation+" (no override="+normalPlain+"), Hard infantry assignment "+(wall==null?"none":wall.order+"/"+wall.formation));
                    shieldFormation=formation&&normalPlain;next(b,12);
                }}
                case 12->{
                    if(since(b)>=40&&!shotAsked){shotAsked=true;
                        long up=b.units.stream().filter(u->u.squad==hard&&u.spec.id().equals("knight")&&u.mob.isUsingItem()).count(),knights=b.units.stream().filter(u->u.squad==hard&&u.spec.id().equals("knight")&&u.mob.isAlive()).count();
                        Vec3d at=centre(b,hard);
                        report("p1-bots-shieldwall",shieldFormation&&up>=6,"Hard infantry formation shieldwall (Normal left it alone)="+shieldFormation+"; "+up+" of "+knights+" knights using their shield "+since(b)+" ticks later; six Dusk archers "+String.format("%.0f",gap)+" blocks in front of the front rank when ordered ("+String.format("%.0f",Math.abs(foes.get(0).mob.getX()-at.x))+" from the army centre now), no Dusk melee");
                        wantShot="bots-shieldwall";
                    }
                    if(shotAsked&&wantShot==null)next(b,13); // the archers stay, so the wall keeps bracing for p1-bots-blight
                }
                case 13->{ // p1-bots-blight: a frontal blow on a knight braced in shieldwall is blocked; a Gravecaller burst on the same wall is not
                    var knights=b.units.stream().filter(u->u.squad==hard&&u.spec.id().equals("knight")&&u.mob.isAlive()).toList();
                    if(hexer==null){
                        var braced=knights.stream().filter(u->u.mob.isBlocking()).toList();
                        if(braced.size()<2){report("p1-bots-blight",false,"only "+braced.size()+" knights braced");for(var u:foes)u.mob.discard();b.units.removeAll(foes);foes.clear();next(b,14);return;}
                        aim=braced.get(0);var guard=braced.get(1);double hx=aim.mob.getX()+12,hz=aim.mob.getZ(); // 12 blocks ahead of the wall (Dawn faces +x), inside every shield arc
                        var squad=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(hx,top(b,hx,hz),hz));squad.order="hold";squad.yaw=90;
                        hexer=freezeIt(b.spawn(squad,troop("hexer"),0,new Vec3d(hx,top(b,hx,hz),hz)));foes.add(hexer);
                        float hp=guard.mob.getHealth();int regen=guard.mob.timeUntilRegen;boolean hit=guard.mob.damage(b.world.getDamageSources().mobAttack(hexer.mob),5);
                        frontalBlocked=!hit&&regen<=10&&guard.mob.getHealth()==hp;frontalWhy="a frontal blow of 5 from the Gravecaller on a braced knight: health "+hp+" -> "+guard.mob.getHealth()+" (hurt cooldown before it "+regen+")";
                    }
                    if(hexer.spellCenter!=null||hpBefore.isEmpty()){
                        if(since(b)>60){report("p1-bots-blight",false,"the burst never landed; "+frontalWhy);for(var u:foes)u.mob.discard();b.units.removeAll(foes);foes.clear();next(b,14);return;}
                        // Until the burst lands (on the next Gravecaller decision tick): keep it on the aimed knight, and note every knight health and brace.
                        hexer.spellCenter=aim.mob.getPos();hexer.windupUntil=b.time;hpBefore.clear();bracedBefore.clear();
                        for(var u:knights){hpBefore.put(u,u.mob.getHealth());if(u.mob.isBlocking())bracedBefore.add(u);}
                        return;
                    }
                    float before=hpBefore.getOrDefault(aim,0f),after=aim.mob.getHealth();boolean aimBraced=bracedBefore.contains(aim);
                    long hitBraced=bracedBefore.stream().filter(u->u.mob.getHealth()<hpBefore.get(u)).count();
                    report("p1-bots-blight",frontalBlocked&&aimBraced&&after<before,frontalWhy+", blocked="+frontalBlocked+"; then a Gravecaller burst on a braced knight (braced="+aimBraced+"): health "+before+" -> "+after+"; "+hitBraced+" of "+bracedBefore.size()+" braced knights lost health");
                    for(var u:foes)u.mob.discard();b.units.removeAll(foes);foes.clear();hexer=null;next(b,14);
                }
                case 14->{ // p1-bots-charge-timing: eight frozen Dusk revenants 60 blocks away, then 15 blocks away
                    clear(b);hard=army(b,0,"hard",-100,0);foes.clear();Vec3d at=centre(b,hard);
                    spawnFoes(b,"revenant",8,at.x+60,at.z);int seenFar=see(b);
                    b.campaign.botDecide(hard,b.campaign.tier(hard));farOrder=hard.order;hornsBefore=BattleEffects.horns.getOrDefault("charge",0);int hornsFar=hornsBefore;
                    spawnFoes(b,"revenant",8,at.x+28,at.z);int seenMid=see(b); // seen, and the army outnumbers them, but they are outside the 18-block gate
                    b.campaign.botDecide(hard,b.campaign.tier(hard));String midOrder=hard.order;int hornsMid=BattleEffects.horns.getOrDefault("charge",0);
                    spawnFoes(b,"revenant",8,at.x+15,at.z);int seenNear=see(b);
                    b.campaign.botDecide(hard,b.campaign.tier(hard));int hornsNear=BattleEffects.horns.getOrDefault("charge",0);
                    report("p1-bots-charge-timing",farOrder.equals("attack")&&midOrder.equals("attack")&&seenMid==8&&hornsMid==hornsFar&&hard.order.equals("charge")&&seenNear==8&&hornsNear>hornsMid,"Hard army with eight Dusk revenants 60 blocks off ("+seenFar+" remembered): order "+farOrder+"; 28 blocks off ("+seenMid+" remembered, outside the 18-block gate): order "+midOrder+"; 15 blocks off ("+seenNear+" remembered): order "+hard.order+"; charge horns "+hornsFar+" -> "+hornsMid+" -> "+hornsNear);
                    for(var u:foes)u.mob.discard();b.units.removeAll(foes);foes.clear();
                    for(var u:b.units)if(u.squad==hard&&u.slot>=3)u.mob.discard(); // three left of twenty: the army retreats
                    hard.yaw=0;b.campaign.botDecide(hard,b.campaign.tier(hard));
                    report("p1-bots-retreat-facing",hard.order.equals("retreat")&&hard.yaw==-90,"Hard army cut down to three: order "+hard.order+", facing "+hard.yaw+" (Dawn faces -90 after every decision, retreat included)");
                    next(b,99);
                }
                default->{}
            }
        }
    }

    /** Task 6: The Long March battle 1 (sheet row march-1). campaign:march-1 switches to its map, then starts it against one Easy bot with a
     *  briefing card in the entrance; the four lessons advance from the commands their keys send and pay rollCost gold once, though they repeat
     *  on a replay; a victory marks the battle cleared and turns its primary camp button into a normal one; a pad match never teaches.
     *  Commands run on the server tick; the client records the lessons it shows, reads the camp buttons and takes the screenshots. */
    static final class TutorialChecks implements Suite,ServerWatch {
        static final int ROLL=Spec.CAMPAIGN[0].rollCost(),START_TICKS=900;
        /** One run of the four lessons: the server lesson after each command and the profile gold around the barracks lesson (server thread), the lessons the client showed (client thread). */
        static final class Run {
            final List<Integer> steps=Collections.synchronizedList(new ArrayList<>());volatile int goldBefore=-1,goldAfter=-1;volatile String commands="";volatile boolean over;
            final List<Integer> seen=new ArrayList<>();int wait;
            boolean taught(){return seen.equals(List.of(0,1,2,3,-1))&&List.copyOf(steps).equals(List.of(1,2,3,-1));}
            String why(){return "client showed lessons "+seen+" (expected [0, 1, 2, 3, -1]), server lesson after each command "+steps+" (expected [1, 2, 3, -1]); "+commands;}
        }
        final Run first=new Run(),replay=new Run();
        volatile int stage;volatile boolean finished,startOk,clearOk,padOk;volatile UUID player;long stageAt,introUntil;
        volatile int goldRepeat=-1;volatile String startWhy="no battle started",clearWhy="",padWhy="";
        // client thread only
        final boolean[] lessonShot=new boolean[4],lessonWhole=new boolean[4];final Set<Integer> padSeen=new TreeSet<>();
        int steady=-9,steadyTicks,clientGoldBefore=-1,clientGoldAfter=-1,cinematicTicks,beforeAt=-1,afterAt=-1,waited;String copyWhy="",beforeWhy="no camp home read",beforeRide;boolean briefingShot,startReported,padReported;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        int gold(GravesAndGlory.Battle b){return b.campaign.profile(player).gold;}
        void fresh(GravesAndGlory.Battle b){b.campaign.profiles.put(player,new Campaign.Profile());b.campaign.save(player);}
        /** The command of lesson n, as the keys send it: R then Move (a tactical move), B (a tactical formation), G (a tactical attack on open ground ahead), the barracks lectern. Dawn faces +x. */
        void lesson(GravesAndGlory.Battle b,Run r,int n){
            var p=sp(b);Vec3d at=p.getPos();
            switch(n){
                case 0->b.tactical(p,"all","move","block",at.x+12,at.z,p.getYaw(),-1);
                case 1->b.tactical(p,"all","formation","wedge",at.x,at.z,p.getYaw(),-1);
                case 2->b.tactical(p,"all","attack","block",at.x+20,at.z,p.getYaw(),-1);
                default->{r.goldBefore=gold(b);b.command(p,"barracks");r.goldAfter=gold(b);}
            }
            r.steps.add(b.campaign.tutorialStep(player));r.commands+=(n==0?"":", ")+new String[]{"move","formation","attack","barracks"}[n]+" at intro+"+(b.time-introUntil);
        }
        /** The War Table home's Ride plate as the last frame drew it while the Campaign sheet is raised (UI Task 5), or null. */
        static String march(MinecraftClient c){return c.currentScreen instanceof CampScreen s&&s.page.equals("home")&&s.home!=null&&s.home.focused==2?WarTableHome.texts().get("ride"):null;}
        /** Right twice, as the keys send it: the War Table raises the Campaign sheet, whichever sheet was up. */
        static void raiseCampaign(MinecraftClient c){if(c.currentScreen!=null)for(int i=0;i<2;i++)c.currentScreen.keyPressed(GLFW.GLFW_KEY_RIGHT,0,0);}
        static String describe(String ride){return ride==null?"no War Table home with the Campaign sheet raised":"the Campaign sheet raised, the Ride plate reads \""+ride+"\"";}
        /** Reported once: at the briefing screenshot, or when the lessons are reported if no entrance was seen. */
        void reportStart(){
            if(startReported)return;startReported=true;String name=CampaignClient.battle.has("name")?CampaignClient.battle.get("name").getAsString():"";
            report("p1-tutorial-start",startOk&&name.equals("The Training Field")&&briefingShot,startWhy+"; client battle name \""+name+"\"; briefing screenshot "+(briefingShot?"taken "+cinematicTicks+" ticks into the entrance":"not taken (no entrance seen)"));
        }
        /** Records each new lesson value the client shows for run r; in the first run, photographs each lesson after three steady ticks on screen and compares what the panel drew with the full copy. */
        void watch(MinecraftClient c,Run r,boolean photograph){
            int now=CampaignClient.tutorial;
            if((now>=0||!r.seen.isEmpty())&&(r.seen.isEmpty()||r.seen.get(r.seen.size()-1)!=now))r.seen.add(now);
            if(!photograph)return;
            if(now==3&&clientGoldBefore<0)clientGoldBefore=CampaignClient.profile.gold;
            boolean shown=now>=0&&now<=3&&!lessonShot[now]&&GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()&&c.currentScreen==null;
            steadyTicks=shown?(now==steady?steadyTicks+1:1):0;steady=now;
            if(steadyTicks>=3){shot(c,"tutorial-lesson-"+(now+1));lessonShot[now]=true;
                // what the lesson panel drew this frame against the full lesson copy: nothing cut, every line inside the panel
                String full=GravesAndGloryClient.lessonText(now);var drawn=GravesAndGloryClient.lessonDrawn;String joined=String.join(" ",drawn);
                boolean whole=joined.replace(" ","").equals(full.replace(" ","")),fits=drawn.stream().allMatch(l->c.textRenderer.getWidth(DarkUi.body(l))<=GravesAndGloryClient.LESSON_TEXT_WIDTH); // measured in the body style it wraps in
                lessonWhole[now]=whole&&fits;copyWhy+=(copyWhy.isEmpty()?"":"; ")+"lesson "+(now+1)+": "+drawn.size()+" line(s), whole="+whole+", inside "+GravesAndGloryClient.LESSON_TEXT_WIDTH+" px="+fits+(whole?"":", drawn ends \""+joined.substring(Math.max(0,joined.length()-16))+"\"");
            }
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(stage==2){ // the camp home before the battle: the Campaign sheet rides to battle 1
                if(beforeAt<0){beforeAt=tick;CampScreen.open("home");}
                else if(tick==beforeAt+20)raiseCampaign(c);
                else if(tick==beforeAt+25){beforeRide=march(c);beforeWhy=describe(beforeRide);shot(c,"tutorial-camp-before");}
                else if(tick==beforeAt+30){c.setScreen(null);CampaignClient.homeMode="skirmish";server(c,b->next(b,3));}
            }
            if(stage>=4&&stage<=6){
                if(CampaignClient.cinematic()&&!briefingShot&&++cinematicTicks==40){shot(c,"tutorial-briefing");briefingShot=true;reportStart();}
                watch(c,first,true);
            }
            if(stage==6&&c.currentScreen!=null)c.setScreen(null); // the barracks lesson opens the barracks screen
            if(stage==6&&first.over&&++first.wait==40){ // 40 ticks for the lesson end and the new gold to reach the client
                reportStart();clientGoldAfter=CampaignClient.profile.gold;int shots=0,whole=0;for(int i=0;i<4;i++){if(lessonShot[i])shots++;if(lessonWhole[i])whole++;}
                String use=HintText.chip(c.options.useKey.getBoundKeyLocalizedText().getString());boolean named=GravesAndGloryClient.lessonText(3).contains(use); // lesson 4 names the use key as bound, as its chip (RMB for the right button, UI polish M6; drawn whole above)
                report("p1-tutorial-lessons",first.taught()&&shots==4&&whole==4&&named,first.why()+"; "+shots+" of 4 lesson screenshots; "+whole+" of 4 lessons drawn whole inside the panel ("+copyWhy+"); lesson 4 names the use key \""+use+"\": "+named);
                server(c,b->next(b,7));
            }
            if(stage==8){ // the camp home after the victory: all cleared, the Campaign sheet rides to the last battle, battle 1; pressing it is the replay
                if(afterAt<0){if(c.currentScreen instanceof CampScreen s&&s.page.equals("results")||++waited>=60){afterAt=tick;CampScreen.open("home");}}
                else if(tick==afterAt+20)raiseCampaign(c);
                else if(tick==afterAt+25){String ride=march(c);
                    report("p1-tutorial-clear",clearOk&&CampaignClient.profile.campaignCleared==1&&"Ride to The Training Field".equals(beforeRide)&&"Ride to The Training Field".equals(ride),clearWhy+"; client campaignCleared "+CampaignClient.profile.campaignCleared+"; camp home before the battle: "+beforeWhy+"; after the victory: "+describe(ride));
                    shot(c,"tutorial-camp-after");
                }
                else if(tick==afterAt+30){var ride=c.currentScreen instanceof CampScreen s&&s.home!=null?s.home.button("ride"):null;
                    if(ride!=null&&march(c)!=null){ride.onPress();server(c,b->next(b,9));}
                    else{replay.commands="no Ride plate to replay from";replay.over=true;c.setScreen(null);server(c,b->next(b,12));}
                    CampaignClient.homeMode="skirmish";
                }
            }
            if(stage>=9&&stage<=11)watch(c,replay,false);
            if(stage>=11&&stage<=12&&c.currentScreen!=null)c.setScreen(null);
            if(stage==12&&replay.over&&++replay.wait==40){
                boolean paidOnce=first.goldBefore>=0&&first.goldAfter-first.goldBefore==ROLL&&goldRepeat==first.goldAfter&&clientGoldBefore>=0&&clientGoldAfter-clientGoldBefore==ROLL;
                boolean replayFree=replay.goldBefore>=0&&replay.goldAfter==replay.goldBefore;
                report("p1-tutorial-reward-once",paidOnce&&replay.taught()&&replayFree,"first run: profile gold "+first.goldBefore+" -> "+first.goldAfter+" on the barracks lesson (expected +"+ROLL+"), "+goldRepeat+" after a second barracks command, client gold "+clientGoldBefore+" -> "+clientGoldAfter+"; replay: "+replay.why()+"; profile gold "+replay.goldBefore+" -> "+replay.goldAfter+" on its barracks lesson (expected no change)");
                server(c,b->next(b,13));
            }
            if(stage==14&&GravesAndGloryClient.running)padSeen.add(CampaignClient.tutorial);
            if(stage==15&&!padReported){padReported=true;
                report("p1-tutorial-not-on-pads",padOk&&padSeen.equals(Set.of(-1)),padWhy+"; client lesson values while the match ran "+padSeen+" (expected only -1)");
                server(c,b->next(b,99));
            }
            if(stage==99&&!finished){finished=true;finish(c,"tutorial");}
            if(tick>12000&&!finished){finished=true;report("p1-tutorial-timeout",false,"stuck in stage "+stage);finish(c,"tutorial");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // in camp, off map 0 (so the command has to switch maps), a fresh commander, the server bot setting Normal
                    var p=sp(b);if(p==null||!b.ready)return;
                    if(b.running){b.campaign.returnLobby();stageAt=b.time;return;}
                    if(since(b)<40)return;
                    if(b.campaign.map==0){b.campaign.switchMap(1);return;}
                    fresh(b);b.campaign.difficulty="normal";
                    System.out.println("[p1] tutorial start: map "+b.campaign.map+", fresh profile, gold "+gold(b)+", bots "+b.campaign.difficulty);next(b,2);
                }
                case 3->{b.command(sp(b),"campaign:march-1");next(b,4);}
                case 4->{ // the battle starts once its map is built
                    if(!b.running){
                        if(since(b)>START_TICKS){startWhy="no battle "+START_TICKS+" ticks after campaign:march-1 (map "+b.campaign.map+", ready "+b.ready+")";first.commands="no battle";first.over=true;next(b,6);}
                        return;
                    }
                    var bots=b.squads.values().stream().filter(s->s.bot&&s.seated).toList();var own=b.squads.get(player);
                    startOk=b.campaign.size==1&&b.campaign.map==0&&bots.size()==1&&bots.get(0).tier.equals("easy")&&bots.get(0).team==1&&own!=null&&own.seated&&own.team==0&&b.campaign.difficulty.equals("normal");
                    startWhy="running "+since(b)+" ticks after campaign:march-1, "+b.campaign.size+"v"+b.campaign.size+" on map "+b.campaign.map+", "+bots.size()+" bot(s): "+bots.stream().map(s->s.tier+" on team "+s.team).toList()+", the commander on team "+(own==null?"none":own.team)+", server bot setting still "+b.campaign.difficulty;
                    System.out.println("[p1] "+startWhy);introUntil=b.campaign.introUntil;next(b,5);
                }
                case 5,10->{ // one lesson command every 60 ticks after the intro
                    long t=b.time-introUntil;
                    if(t>0&&t%60==0){int n=(int)(t/60)-1;lesson(b,stage==5?first:replay,n);if(n==3)next(b,stage+1);}
                }
                case 6->{if(since(b)==20&&!first.over){b.command(sp(b),"barracks");goldRepeat=gold(b);first.over=true;}} // the client reports the lessons, then 7
                case 7->{ // a victory clears the battle
                    b.campaign.finish(0);int cleared=b.campaign.profile(player).campaignCleared;
                    clearOk=cleared==1&&!b.running;clearWhy="after finish(0): campaignCleared "+cleared+", running "+b.running;System.out.println("[p1] "+clearWhy);next(b,8);
                }
                case 9->{ // the replay, started by the camp button, once it runs
                    if(!b.running){if(since(b)>START_TICKS){replay.commands="no replay "+START_TICKS+" ticks after the camp button was pressed";replay.over=true;next(b,12);}return;}
                    introUntil=b.campaign.introUntil;next(b,10);
                }
                case 11->{if(since(b)==20){replay.over=true;next(b,12);}} // the client reports reward-once, then 13
                case 13->{ // a solo pad 3v3 for a fresh commander
                    if(b.running){b.campaign.returnLobby();stageAt=b.time;return;}
                    if(since(b)<40)return;
                    fresh(b);var c=b.campaign;c.closeParty();c.onPad.clear();c.onPad.put(player,0);c.mapVotes.clear();c.startQuickMatch();padWhy="";next(b,14);
                }
                case 14->{
                    int step=b.campaign.tutorialStep(player);if(!padWhy.contains("["+step+"]"))padWhy+="["+step+"]";
                    if(since(b)>=60){padOk=b.running&&b.campaign.size==3&&padWhy.equals("[-1]");padWhy="solo pad match "+b.campaign.size+"v"+b.campaign.size+" running="+b.running+" for a fresh commander, server lessons over 60 ticks "+padWhy+" (expected only [-1])";b.campaign.returnLobby();next(b,15);}
                }
                default->{}
            }
        }
    }

    /** Task 7: the end-of-match report on Kingsfield Crossroads (map 4, five points). The commander strikes down two Dusk troops, loses one of
     *  his own, holds point 0 for 60 ticks and one income interval, and a Dawn archer's arrow lands after the archer fell; then the round ends.
     *  Every troop is frozen while the fixture runs, so only the fixture kills. Stages run on the server tick; the client checks the results page,
     *  then photographs the same page holding the fullest report it can get (4v4 on five points) as layout evidence. */
    static final class ReportChecks implements Suite,ServerWatch {
        static final Spec.EconomyRow E=Spec.ECONOMY[0];
        volatile int stage;volatile boolean finished,over;volatile UUID player;long stageAt;
        GravesAndGlory.Squad own;GravesAndGlory.Capture point;GravesAndGlory.Unit archer,mark;Projectiles.Shot arrow;int killsBefore;
        int overAt=-1,shownAt=-1,staleTicks;JsonObject real,crowd; // client thread only

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        /** Keeps the commander on foot in the middle of point 0, where captureTick counts him. */
        void stand(GravesAndGlory.Battle b){
            var p=sp(b);var at=point.spec;if(p.isSpectator())p.changeGameMode(GameMode.ADVENTURE);
            if(Math.hypot(p.getX()-at.x(),p.getZ()-at.z())>2)b.relocate(p,at.x()+.5,top(b,at.x(),at.z()),at.z()+.5,-90,10);
        }
        static JsonArray rows(JsonObject report,String key){return report!=null&&report.has(key)&&report.get(key).isJsonArray()?report.getAsJsonArray(key):new JsonArray();}
        static int number(JsonElement row,String key){var o=row.getAsJsonObject();return o.has(key)?o.get(key).getAsInt():-1;}
        static String text(JsonElement row,String key){var o=row.getAsJsonObject();return o.has(key)?o.get(key).getAsString():"";}
        /** The given report with the fullest table it can hold: 4v4 with the widest 16-letter names, three-digit tallies, four-digit gold and long holds. */
        static JsonObject crowded(JsonObject real){
            var r=real.deepCopy();var squads=new JsonArray();String[] names={"WWWWWWWWWWWWWWWW","MMMMMMMMMMMMMMMM","hard bot","hard bot","QQQQQQQQQQQQQQQQ","normal bot","normal bot","normal bot"};
            for(int i=0;i<8;i++){var row=new JsonObject();boolean bot=names[i].endsWith(" bot");row.addProperty("name",names[i]);row.addProperty("team",i/4);row.addProperty("bot",bot);row.addProperty("tier",bot?names[i].substring(0,names[i].length()-4):"");row.addProperty("you",i==0);
                row.addProperty("kills",i==4?212:97+i);row.addProperty("lost",88+i);row.addProperty("gold",1180+37*i);row.addProperty("objective",640+13*i);squads.add(row);}
            r.add("squads",squads);for(var e:r.getAsJsonArray("points")){var p=e.getAsJsonObject();p.addProperty("dawn",700+p.get("dawn").getAsInt());p.addProperty("dusk",600+p.get("dusk").getAsInt());}
            return r;
        }
        static JsonObject parse(String json){return com.google.gson.JsonParser.parseString(json).getAsJsonObject();}
        /** The camp snapshot at its realistic worst (256 match ids held, every claim and title, the 8-row report) fits the client's read limit as sent today, carries no match ids, and the saved profile still refuses a repeat id; then each fallback of Campaign.fitted stays inside the limit. Everything is put back before this server tick ends, so no state packet carries the fixture. */
        void snapshotChecks(GravesAndGlory.Battle b){
            var c=b.campaign;var p=sp(b);var st=c.profile(player).progression;var grants=new LinkedHashSet<>(st.matchGrants);var claims=new HashSet<>(st.claims);var titles=new LinkedHashSet<>(st.titles);var realReport=c.results.get(player);String notice=c.notices.get(player);
            try{
                while(st.matchGrants.size()<256)st.matchGrants.add(UUID.randomUUID().toString());
                for(int i=0;i<3;i++)st.claims.add("daily:"+i);st.claims.add("weekly");for(int t=1;t<=Progression.TIERS;t++)st.claims.add(Progression.SEASON+":"+t);st.titles.addAll(Arrays.asList(Progression.TITLES));
                var four=crowded(realReport);c.results.put(player,four);String worst=c.snapshot(p);
                var saved=Campaign.JSON.fromJson(Campaign.JSON.toJson(c.profile(player)),Campaign.Profile.class).progression;var again=Progression.match(saved,st.matchGrants.iterator().next(),true,true);
                boolean pretty=worst.contains("\n"),ids=worst.contains("matchGrants"),kept=parse(worst).getAsJsonObject("result").has("id");
                report("p1-report-snapshot",worst.length()<=Campaign.SNAPSHOT_LIMIT&&pretty&&kept&&!ids&&saved.matchGrants.size()==256&&again.xp()==0,
                    "worst camp snapshot ("+st.matchGrants.size()+" match ids held by the server, "+st.claims.size()+" claims, "+st.titles.size()+" titles, an 8-row report on "+b.points.size()+" points) is "+worst.length()+" of "+Campaign.SNAPSHOT_LIMIT+" characters, sent pretty="+pretty+" with the report="+kept+", carries match ids="+ids+"; the saved profile keeps "+saved.matchGrants.size()+" ids and pays a repeat id "+again.xp()+" XP");
                // the guard: grow the report until the pretty form no longer fits, then until the compact form does not either, then a notice longer than the limit
                var rows=four.getAsJsonArray("squads");var row=rows.get(0);String compact="",dropped="",empty;int compactRows=0,droppedRows=0;
                for(int n=0;n<200&&compact.isEmpty();n++){for(int k=0;k<5;k++)rows.add(row.deepCopy());String s=c.snapshot(p);if(!s.contains("\n")){compact=s;compactRows=rows.size();}}
                for(int n=0;n<400&&dropped.isEmpty();n++){for(int k=0;k<5;k++)rows.add(row.deepCopy());String s=c.snapshot(p);var r=s.length()>2?parse(s).getAsJsonObject("result"):null;if(r!=null&&!r.has("id")){dropped=s;droppedRows=rows.size();}}
                c.notices.put(player,"x".repeat(Campaign.SNAPSHOT_LIMIT));empty=c.snapshot(p);
                boolean compactOk=!compact.isEmpty()&&compact.length()<=Campaign.SNAPSHOT_LIMIT&&parse(compact).getAsJsonObject("result").has("id");
                boolean droppedOk=!dropped.isEmpty()&&dropped.length()<=Campaign.SNAPSHOT_LIMIT&&parse(dropped).has("profile");
                report("p1-report-snapshot-guard",compactOk&&droppedOk&&empty.equals("{}"),
                    "with "+compactRows+" report rows the snapshot went compact at "+compact.length()+" characters (report kept="+compactOk+"); with "+droppedRows+" rows it left the report out at "+dropped.length()+" (profile kept="+droppedOk+"); with a "+Campaign.SNAPSHOT_LIMIT+"-character notice it sent "+empty+" (limit "+Campaign.SNAPSHOT_LIMIT+")");
            }finally{st.matchGrants.clear();st.matchGrants.addAll(grants);st.claims.clear();st.claims.addAll(claims);st.titles.clear();st.titles.addAll(titles);c.results.put(player,realReport);if(notice==null)c.notices.remove(player);else c.notices.put(player,notice);}
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if((stage==3||stage==4)&&staleTicks<30&&GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()){ // the Kingsfield crash (fixed in 6ecbfb0): after a switch to five points the client's point arrays hold three until the next state packet, and the battle HUD's point strip and minimap must still draw
                GravesAndGloryClient.contested=new boolean[3];GravesAndGloryClient.owners=new int[]{-1,-1,-1};GravesAndGloryClient.progress=new int[3];GravesAndGloryClient.challengers=new int[]{-1,-1,-1};
                if(++staleTicks==30){shot(c,"kingsfield-stale");report("p1-kingsfield-stale-points",Spec.POINTS.length==5,"battle HUD point strip and minimap drawn for 30 client ticks with "+Spec.POINTS.length+" points against three-entry point arrays (the state between a switch to Kingsfield and the next state packet); the client kept running");}
            }
            if(over&&overAt<0)overAt=tick;
            if(overAt>=0&&shownAt<0&&!finished){
                if(c.currentScreen instanceof CampScreen s&&s.page.equals("results"))shownAt=tick;
                else if(tick-overAt>60){shot(c,"report");report("p1-report-screen",false,"no results page 60 client ticks after the round ended (screen: "+(c.currentScreen==null?"none":c.currentScreen.getClass().getSimpleName())+")");finished=true;finish(c,"report");}
            }
            if(shownAt>=0&&tick==shownAt+5){ // a few frames after the page opened, so the screenshot holds it
                var r=CampaignClient.result;int rows=rows(r,"squads").size(),points=rows(r,"points").size();shot(c,"report");
                report("p1-report-screen",shownAt-overAt<=60&&rows>0&&points==5,"results page open "+(shownAt-overAt)+" client ticks after the round ended; the client holds "+rows+" squad rows and "+points+" point rows");
                if(points==5){real=r;crowd=crowded(r);CampaignClient.result=crowd;}else{finished=true;finish(c,"report");}
            }
            if(crowd!=null&&tick>shownAt+8){ // layout evidence, not a check: shot once no state packet has put the real report back since the last tick
                if(CampaignClient.result==crowd){shot(c,"report-4v4");CampaignClient.result=real;crowd=null;finished=true;finish(c,"report");}
                else CampaignClient.result=crowd;
            }
            if(tick>12000&&!finished){finished=true;report("p1-report-timeout",false,"stuck in stage "+stage);finish(c,"report");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // fixture: practice on Kingsfield Crossroads against one Normal bot; nobody refills
                    var p=sp(b);if(p==null||!skirmish(b,p,4,1,"normal"))return;
                    own=b.squads.get(player);point=b.points.get(0);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    System.out.println("[p1] report fixture: map "+b.campaign.map+" "+Spec.MAPS[b.campaign.map].name()+", "+b.points.size()+" points, "+b.units.size()+" troops, point 0 is "+point.spec.name()+" at "+point.spec.x()+", "+point.spec.z());
                    next(b,2);
                }
                case 2->{ // two Dusk troops fall to the commander's blows and one of his own dies; then his team owns point 0 and he stands on it
                    var p=sp(b);var foes=b.units.stream().filter(u->u.squad.team!=own.team&&u.mob.isAlive()).limit(2).toList();
                    for(var u:foes)u.mob.damage(b.world.getDamageSources().playerAttack(p),99);
                    var mine=b.units.stream().filter(u->u.squad==own&&u.mob.isAlive()).findFirst().orElseThrow();mine.mob.kill();
                    System.out.println("[p1] report blows: "+foes.stream().filter(u->!u.mob.isAlive()).count()+" of "+foes.size()+" Dusk troops struck down by the commander, his own "+mine.spec.id()+" dead="+!mine.mob.isAlive()+"; tally kills "+own.stats.kills+", lost "+own.stats.lost);
                    point.owner=own.team;point.ownedSince=b.time;point.challenger=-1;point.progress=0;point.contested=false;Arena.flag(b.world,point.spec,own.team);
                    stand(b);next(b,3);
                }
                case 3->{stand(b);if(since(b)>=60){System.out.println("[p1] report point: 60 ticks on "+point.spec.name()+", objective "+own.stats.objectiveSeconds+" s");next(b,4);}}
                case 4->{stand(b);if(since(b)>=E.incomeSeconds()*20L){System.out.println("[p1] report income: one interval waited, gold earned "+own.stats.goldEarned);next(b,5);}}
                case 5->{ // the late kill: a Dawn archer looses at a Dusk troop on its last health point and falls before the arrow lands
                    mark=b.units.stream().filter(u->u.squad.team!=own.team&&u.mob.isAlive()&&u.spec.group().equals("infantry")).findFirst().orElse(null);
                    if(mark==null){report("p1-report-late-kill",false,"no living Dusk infantry to shoot");next(b,7);break;}
                    mark.mob.setHealth(1);double x=mark.mob.getX()-12,z=mark.mob.getZ();
                    archer=b.spawn(own,troop("archer"),30,new Vec3d(x,top(b,x,z),z));freeze(archer);
                    Projectiles.fire(b,archer,mark.mob,"arrow",(float)(archer.spec.damage()*b.campaign.damage(archer)));arrow=b.shots.get(b.shots.size()-1);
                    archer.mob.kill();killsBefore=own.stats.kills;
                    System.out.println("[p1] report late shot: fired at tick "+arrow.fired()+", lands at "+arrow.lands()+", archer dead="+!archer.mob.isAlive()+", army kills "+killsBefore);
                    next(b,6);
                }
                case 6->{if(b.time>arrow.lands()){
                    boolean gone=!archer.mob.isAlive()&&!b.units.contains(archer);
                    report("p1-report-late-kill",!mark.mob.isAlive()&&gone&&own.stats.kills==killsBefore+1,"arrow loosed at tick "+arrow.fired()+" by an archer killed the same tick (out of the battle at landing="+gone+"), landed at "+arrow.lands()+": target alive="+mark.mob.isAlive()+", last hit by the commander's army="+(mark.lastHitBy==own)+"; the army's kills "+killsBefore+" -> "+own.stats.kills);
                    next(b,7);
                }}
                case 7->{ // the round ends: the commander's report as the server keeps it for his client
                    int seated=(int)b.squads.values().stream().filter(s->s.seated).count();String side=own.team==0?"dawn":"dusk";
                    b.campaign.finish(own.team);var r=b.campaign.results.get(player);var squads=rows(r,"squads");var points=rows(r,"points");
                    JsonElement me=null;boolean names=true;for(var e:squads){if(text(e,"you").equals("true"))me=e;if(text(e,"bot").equals("true"))names&=text(e,"name").equals(text(e,"tier")+" bot");}
                    boolean ownFirst=squads.size()>0&&number(squads.get(0),"team")==own.team;
                    report("p1-report-fields",squads.size()==seated&&me!=null&&number(me,"kills")>=2&&number(me,"lost")>=1&&number(me,"gold")>=E.passiveGold()&&number(me,"objective")>=2&&ownFirst&&names,
                        squads.size()+" squad rows for "+seated+" seated armies, own team first="+ownFirst+", bot rows named \"<tier> bot\"="+names+"; rows "+squads+" (the commander's row needs kills>=2, lost>=1, gold>="+E.passiveGold()+", objective>=2); the snapshot is "+b.campaign.snapshot(sp(b)).length()+" of the client's 16000 characters");
                    report("p1-report-five-points",points.size()==5&&number(points.get(0),side)>=2,points.size()+" point rows "+points+" (point 0 needs 2 s or more for "+side+")");
                    snapshotChecks(b);
                    over=true;next(b,8);
                }
                default->{}
            }
            if(stage>=2&&stage<=6)for(var u:b.units)freeze(u); // only the fixture strikes
        }
    }

    /** UI plan Task 1, the UI kit: the exported atlas, the canvas scale at three window sizes, then the kit sheet at 960x540 GUI 1, where a canvas pixel is a framebuffer pixel, shot page by page with every opaque pixel of every native-size draw and bar cut compared with its PNG. Client thread only. */
    static final class UiArtChecks implements Suite {
        int stage,oldW,oldH,oldGui,shootAt,reopened,compared,wrong,magenta,drawn,shots;boolean canvasOk=true;
        final StringBuilder canvas=new StringBuilder(),first=new StringBuilder();UiKitSheet sheet;Map<String,NativeImage> art;

        public void step(MinecraftClient c,int tick){
            if(tick>3000&&stage<6){report("p1-ui-art-timeout",false,"stuck in stage "+stage+" at page "+(sheet==null?-1:sheet.page));end(c);return;}
            switch(stage){
                case 0->{if(tick<20)return;c.setScreen(null);var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();atlas(c);reload(c);window(c,960,540,1);stage=1;}
                case 1->{canvas(c,"960x540 GUI 1",UiCanvas.k()==1&&UiCanvas.width()==960&&UiCanvas.height()==540);window(c,1280,720,3);stage=2;}
                case 2->{canvas(c,"1280x720 GUI 3",UiCanvas.k()==1&&UiCanvas.width()==1280&&UiCanvas.toCanvas(100)==300);window(c,1280,720,0);stage=3;}
                case 3->{canvas(c,"1280x720 GUI Auto",UiCanvas.k()==1);report("p1-ui-canvas",canvasOk,canvas.toString());window(c,960,540,1);stage=4;}
                case 4->{ // toasts draw over screens, so none may show while the sheet is shot
                    c.getToastManager().clear();sheet=new UiKitSheet();c.setScreen(sheet);
                    try{art=UiKitSheet.loadArt(sheet.names);}catch(Exception e){report("p1-ui-kit-sheet",false,"the kit PNGs could not be read: "+e);end(c);return;}
                    shootAt=tick+10;stage=5;
                }
                case 5->shoot(c,tick);
                case 6->{stage=7;finish(c,"ui-art");}
                default->{}
            }
        }

        /** p1-ui-atlas: every atlas.json entry's PNG opens through the resource manager at the entry's size and UiTex read the entry alike; hud/card-sel is 181x68 at 1, 1; no name holds a mockup-only pt- texture and no PNG under textures/gui is missing from the atlas. */
        void atlas(MinecraftClient c){
            var rm=c.getResourceManager();var res=rm.getResource(UiTex.ATLAS);List<String> bad=new ArrayList<>();int opened=0;JsonObject textures=new JsonObject();
            if(res.isEmpty())bad.add("no "+UiTex.ATLAS);
            else try(var r=res.get().getReader()){textures=com.google.gson.JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("textures");}catch(Exception e){bad.add("atlas.json unreadable: "+e);}
            for(var t:textures.entrySet()){
                String n=t.getKey();var o=t.getValue().getAsJsonObject();int w=o.get("w").getAsInt(),h=o.get("h").getAsInt();
                if(n.contains("pt-"))bad.add(n+" is a mockup-only pt- texture");
                if(!UiTex.has(n)){bad.add(n+" is not in UiTex");continue;}
                var e=UiTex.entry(n);
                if(e.w()!=w||e.h()!=h||e.ox()!=o.get("ox").getAsInt()||e.oy()!=o.get("oy").getAsInt()||!e.kind().equals(o.get("kind").getAsString())||e.l()!=o.get("l").getAsInt()||e.t()!=o.get("t").getAsInt()||e.r()!=o.get("r").getAsInt()||e.b()!=o.get("b").getAsInt()||e.frames()!=o.get("frames").getAsInt())bad.add(n+" read by UiTex as "+e);
                var png=rm.getResource(e.id());if(png.isEmpty()){bad.add(n+": no "+e.id());continue;}
                try(var in=png.get().getInputStream();var img=NativeImage.read(in)){if(img.getWidth()==w&&img.getHeight()==h)opened++;else bad.add(n+" is "+img.getWidth()+"x"+img.getHeight()+", the atlas says "+w+"x"+h);}
                catch(Exception x){bad.add(n+": "+x);}
            }
            for(var id:rm.findResources("textures/gui",f->f.getNamespace().equals("gravesandglory")&&f.getPath().endsWith(".png")).keySet()){
                String n=id.getPath().substring("textures/gui/".length(),id.getPath().length()-4);
                if(!textures.has(n))bad.add(id+" is not in the atlas");
            }
            var card=UiTex.has("hud/card-sel")?UiTex.entry("hud/card-sel"):null;boolean cardOk=card!=null&&card.w()==181&&card.h()==68&&card.ox()==1&&card.oy()==1;
            report("p1-ui-atlas",bad.isEmpty()&&cardOk&&opened>0&&opened==textures.size(),opened+" of "+textures.size()+" atlas.json entries open through the resource manager at their listed size and read alike by UiTex; hud/card-sel "+(card==null?"missing":card.w()+"x"+card.h()+" at "+card.ox()+", "+card.oy())+" (181x68 at 1, 1 wanted); "+(bad.isEmpty()?"no pt- names and no PNG outside the atlas":bad.size()+" wrong: "+String.join("; ",bad.subList(0,Math.min(6,bad.size())))));
        }

        /** p1-ui-atlas-reload (fix round 1): a resource reload that finds no atlas, or an atlas it cannot read, keeps the textures read before (UiTex logs each); the real atlas then reads again. */
        void reload(MinecraftClient c){
            var before=UiTex.has("hud/card-sel")?UiTex.entry("hud/card-sel"):null;int n=UiTex.count();
            UiTex.load(id->Optional.empty());boolean missing=before!=null&&UiTex.has("hud/card-sel")&&UiTex.entry("hud/card-sel")==before&&UiTex.count()==n;
            UiTex.load(id->id.equals(UiTex.ATLAS)?Optional.of(new net.minecraft.resource.Resource(null,()->new java.io.ByteArrayInputStream("{\"textures\":{\"hud/x\":".getBytes(java.nio.charset.StandardCharsets.UTF_8)))):Optional.empty());
            boolean unreadable=UiTex.has("hud/card-sel")&&UiTex.entry("hud/card-sel")==before&&UiTex.count()==n;
            UiTex.load(c.getResourceManager());boolean reread=UiTex.has("hud/card-sel")&&UiTex.entry("hud/card-sel").equals(before)&&UiTex.count()==n;
            report("p1-ui-atlas-reload",missing&&unreadable&&reread,"a reload with no atlas kept the "+n+" textures="+missing+", one with a cut-off atlas.json kept them="+unreadable+", the real atlas read again="+reread+" ("+UiTex.count()+" textures)");
        }

        /** One reading for p1-ui-canvas, with the window it was taken at. */
        void canvas(MinecraftClient c,String label,boolean pass){
            var w=c.getWindow();canvasOk&=pass;
            canvas.append(canvas.isEmpty()?"":"; ").append(label).append(pass?": ":" WRONG: ").append("framebuffer ").append(w.getFramebufferWidth()).append('x').append(w.getFramebufferHeight())
                .append(", GUI Scale option ").append(c.options.getGuiScale().getValue()).append(" (in effect ").append((int)w.getScaleFactor()).append("), k ").append(UiCanvas.k())
                .append(", canvas ").append(UiCanvas.width()).append('x').append(UiCanvas.height()).append(", 100 GUI px = ").append(UiCanvas.toCanvas(100)).append(" canvas px");
        }

        /** One page of the kit sheet, once the last frame drew it: the missing texture's magenta counted, the art compared, the screenshot taken. */
        void shoot(MinecraftClient c,int tick){
            c.getToastManager().clear();
            if(c.currentScreen!=sheet){ // something replaced the sheet: put it back and let it draw again
                if(++reopened>3){report("p1-ui-kit-sheet",false,"the sheet was replaced four times, last by "+c.currentScreen);end(c);return;}
                c.setScreen(sheet);shootAt=tick+5;return;
            }
            if(tick<shootAt||sheet.shownPage!=sheet.page)return;
            var w=c.getWindow();
            if(w.getFramebufferWidth()!=960||w.getFramebufferHeight()!=540||w.getScaleFactor()!=1||UiCanvas.k()!=1){report("p1-ui-kit-sheet",false,"the sheet is not at 960x540 GUI 1: framebuffer "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+", GUI scale "+w.getScaleFactor()+", k "+UiCanvas.k());end(c);return;}
            try(NativeImage img=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){
                for(int y=0;y<img.getHeight();y++)for(int x=0;x<img.getWidth();x++)if((img.getColor(x,y)&0xffffff)==0xf800f8)magenta++;
                int[] cw=sheet.compare(img,art,first);compared+=cw[0];wrong+=cw[1];
            }
            drawn+=sheet.drawnOnPage;shots++;
            shot(c,sheet.page==0?"ui-kit-sheet":"ui-kit-sheet-"+(sheet.page+1));
            if(sheet.page+1<sheet.pages){sheet.page++;shootAt=tick+4;return;}
            int fixed=0,nine=0,three=0;Set<String> all=new HashSet<>(sheet.names),onSheet=new HashSet<>();
            for(String n:sheet.names)switch(UiTex.entry(n).kind()){case "nine"->nine++;case "three-h","three-v"->three++;default->fixed++;}
            for(var p:sheet.placed)if(!p.item().how().equals("bar"))onSheet.add(p.item().name());
            int bars=sheet.count("bar",false);boolean everyOne=!all.isEmpty()&&onSheet.equals(all);
            boolean layout=everyOne&&sheet.count("draw",false)==fixed&&sheet.count("nine",false)==nine&&sheet.count("nine",true)==nine&&sheet.count("three",false)==three&&sheet.count("three",true)==three&&bars==UiKitSheet.BAR_LENGTHS.length;
            report("p1-ui-kit-sheet",layout&&sheet.error==null&&drawn==sheet.placed.size()&&shots==sheet.pages&&compared>0&&wrong==0&&magenta==0,
                all.size()+" atlas textures on "+sheet.pages+" pages of 960x540 at GUI 1 ("+fixed+" drawn once, "+nine+" nine and "+three+" three slices twice, at native size and 1.5x, "+bars+" bar cuts of lengths "+Arrays.toString(UiKitSheet.BAR_LENGTHS)+"), every texture placed="+everyOne+": "+drawn+" of "+sheet.placed.size()+" draws made"+(sheet.error==null?"":", first error "+sheet.error)
                +"; "+compared+" opaque pixels of the native-size draws and bar cuts compared with their PNGs, "+wrong+" wrong"+(first.isEmpty()?"":" (first: "+first+")")+"; "+magenta+" missing-texture magenta pixels; "+shots+" screenshots, p1-ui-kit-sheet"+(shots>1?" to p1-ui-kit-sheet-"+shots:""));
            end(c);
        }

        /** Closes the sheet and puts the window back as the suite found it; p1-ui-art-suite follows once it has settled. */
        void end(MinecraftClient c){if(art!=null)UiKitSheet.closeAll(art);art=null;c.setScreen(null);if(oldW>0)window(c,oldW,oldH,oldGui);stage=6;}
    }

    /** UI Task 2: the Iron Ledger battle HUD and no hunger (UI spec 4.5), at 960x540 GUI 1 unless a check says otherwise. Practice on
     *  map 0 with the enemy army gone: the player's army stands on the Dawn road 63 blocks out from the base with the player just
     *  behind it, every troop frozen; one frozen Dusk revenant stands in front of the infantry (which then reads Fighting), and a
     *  frozen Dawn knight and Dusk revenant share point B (contested); A is Dawn's and C is Dusk's. Three knights and a mage fall
     *  first, so the cards read 9/12, 6/6 and 1/2 as in v4-hud-iron-ledger. Fixtures run on the server tick; every check reads what
     *  BattleHud reports the last frame drew. Kingsfield (map 4, five points) comes last. */
    static final class UiHudChecks implements Suite,ServerWatch,HudWatch {
        static final double AX=-220,AZ=0; // open ground on the Dawn road, 63 blocks out from the live Dawn base at x=-283
        static final List<BattleHud.CardDraw> CARDS=List.of(new BattleHud.CardDraw("infantry","FLASH",false,24,348,176,48),new BattleHud.CardDraw("ranged","SELECTED",true,24,404,176,62),new BattleHud.CardDraw("support","ROUTING",false,24,474,176,48));
        static final List<BattleHud.SealDraw> SEALS=List.of(new BattleHud.SealDraw("hud/seal-dawn",439,41),new BattleHud.SealDraw("hud/seal-contested",467,40),new BattleHud.SealDraw("hud/seal-dusk",495,41));
        /** Width, height, GUI scale, 1 for a borderless window (the 1080 windows: see window(c,w,h,gui,bare)), then the chat lines
         *  p1-ui-chat-lift wants there with the lesson and respawn cards up, closed and open (worked out by hand). Closed (UI polish
         *  M1): vanilla's 10 where the chat stops short of the screen's centre, else what fits between the chat's bottom and 4 px
         *  below vanilla's 15 px crosshair at the centre (1 at 854x480 GUI 2, 4 at 1280x720 GUI 3, 2 at 1920x1080 GUI 4). Open (UI
         *  polish round 2) the crosshair no longer counts: vanilla's 20, or what fits 4 px below the respawn card where the chat
         *  reaches it (2, 5 and 3 at those three sizes, where the card ends at GUI y 114, 116 and 129). At 1366x768 GUI 2 (UI polish
         *  M2) the chat stops just short of the crosshair (GUI x 332 of 334) but reaches the respawn card, which ends at GUI y 186:
         *  10 lines fit below it, open or closed. */
        static final int[][] WINDOWS={{960,540,1,0,10,20},{854,480,2,0,1,2},{1280,720,3,0,4,5},{1920,1080,2,1,10,20},{1920,1080,4,1,2,3},{1366,768,2,0,10,10}};
        // Written by both threads. A negative stage is a hand-off: the client waits for the server task it sent to set the next stage.
        volatile int stage;volatile UUID player;
        volatile boolean hungerOn,hungerDone,routSupport,keepRespawn,fiveReady;volatile int hungerSets,hungerOk;volatile String hungerWrong="",fixture="";
        volatile int lessonStep=3; // the lesson keepLesson pins: 3, the longest, two lines, so the card grows; the cache check also pins 2
        // p1-ui-hud-cache and p1-ui-hud-guard (fix round 1), client thread
        int cacheW,cacheF,sameWraps=-1,stepWraps=-1,reloadWraps=-1,guardF;boolean viewsSame,guardStopped;Object[] views;String lessonBefore="",guardWhy="";
        java.util.concurrent.CompletableFuture<Void> reloading;
        // server thread
        long hungerStart=-1;final List<GravesAndGlory.Unit> pair=new ArrayList<>();
        // client thread
        int oldW,oldH,oldGui,waitFrom,seen,flashAt,askAt,abilitySeen=-1,framesAt,win,closedLines;
        boolean finished,keepLesson,sealsOk,smallOk=true,abilityOk,edgeOk;String sealsWhy="",smallWhy="",abilityWhy="",edgeWhy="";List<BattleHud.CardDraw> two=List.of();
        // p1-ui-chat-lift (controller addition A, client thread): the chat above the left column in battle, in place in camp; the
        // chat text size p1-ui-chat-edge changed, to put back (-1: unchanged)
        int campLift=-1;boolean chatOk=true;String chatWhy="";double chatScale=-1;
        // client thread, every HUD frame: the food draws the mixin skipped in that frame, watched during the hunger run and while the
        // five-seal battlefield builds
        int lastSkipped,hungerFrames,hungerFull,hungerLow,buildFrames,buildFull,buildLow,buildCloses;boolean watchHunger,watchBuild;String hungerOdd="",buildOdd="";

        ServerPlayerEntity sp(GravesAndGlory.Battle b){return player==null?null:b.server.getPlayerManager().getPlayer(player);}
        static CommandMode.Group group(String id){for(var g:CommandMode.groups)if(g.id().equals(id))return g;return null;}
        static boolean meets(int[] a,int[] b){return a!=null&&b!=null&&a[0]<b[0]+b[2]&&b[0]<a[0]+a[2]&&a[1]<b[1]+b[3]&&b[1]<a[1]+a[3];}
        static boolean inside(int[] a,int w,int h){return a[0]>=0&&a[1]>=0&&a[0]+a[2]<=w&&a[1]+a[3]<=h;}
        static String box(int[] a){return a==null?"none":Arrays.toString(a);}
        /** A card may hang on the spine and a boss may sit on the score card; nothing else may touch. */
        static boolean allowed(String a,String b){
            if(a.startsWith("card-")&&b.equals("spine")||b.startsWith("card-")&&a.equals("spine"))return true;
            return a.endsWith("-boss")&&b.equals("score")||b.endsWith("-boss")&&a.equals("score");
        }
        /** Every pair of a frame's rects that touch, but those allowed() lets touch. */
        static List<String> touching(Map<String,int[]> r){
            List<String> bad=new ArrayList<>();var keys=new ArrayList<>(r.keySet());
            for(int i=0;i<keys.size();i++)for(int j=i+1;j<keys.size();j++)if(!allowed(keys.get(i),keys.get(j))&&meets(r.get(keys.get(i)),r.get(keys.get(j))))bad.add(keys.get(i)+" "+box(r.get(keys.get(i)))+" meets "+keys.get(j)+" "+box(r.get(keys.get(j))));
            return bad;
        }
        /** A GUI box in canvas px, rounded outward. */
        static int[] canvas(MinecraftClient c,int x,int y,int w,int h){
            double f=c.getWindow().getScaleFactor()/UiCanvas.k();
            int x0=(int)Math.floor(x*f+1e-9),y0=(int)Math.floor(y*f+1e-9),x1=(int)Math.ceil((x+w)*f-1e-9),y1=(int)Math.ceil((y+h)*f-1e-9);
            return new int[]{x0,y0,x1-x0,y1-y0};
        }
        static void kill(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group,int n){int k=0;for(var u:b.units)if(k<n&&u.squad==s&&u.spec.group().equals(group)&&u.mob.isAlive()){u.mob.kill();k++;}b.units.removeIf(u->!u.mob.isAlive());}
        /** Every enemy troop gone, and neither army refills. */
        static void quiet(GravesAndGlory.Battle b,GravesAndGlory.Squad own){
            for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
            for(var u:b.units)if(u.squad!=own)u.mob.discard();b.units.removeIf(u->u.squad!=own);
        }

        void fixture(GravesAndGlory.Battle b){
            var p=sp(b);if(p==null||!skirmish(b,p,0,1,"normal"))return;
            var own=b.squads.get(player);quiet(b,own);float yaw=-90;
            own.order="hold";own.formation="block";own.target=new Vec3d(AX,top(b,AX,AZ),AZ);own.yaw=yaw;
            own.assignments.put("infantry",new GravesAndGlory.Assignment("attack","shieldwall",new Vec3d(AX,top(b,AX,AZ),AZ),yaw));
            own.assignments.put("ranged",new GravesAndGlory.Assignment("hold","line",new Vec3d(AX-9,top(b,AX-9,AZ),AZ),yaw));
            own.assignments.put("support",new GravesAndGlory.Assignment("follow","block",new Vec3d(AX-16,top(b,AX-16,AZ+9),AZ+9),yaw));
            for(var u:b.units)if(u.squad==own){Vec3d at=b.formation(u);MoraleChecks.place(b,u,at.x,at.z,yaw);freeze(u);}
            kill(b,own,"infantry",3);kill(b,own,"support",1);
            var foes=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(AX+7,top(b,AX+7,AZ),AZ));foes.order="hold";foes.yaw=90;
            freeze(b.spawn(foes,troop("revenant"),0,new Vec3d(AX+7,top(b,AX+7,AZ),AZ)));
            var spot=b.points.get(1).spec;double px=spot.x(),pz=spot.z();
            for(int team=0;team<2;team++){
                var s=new GravesAndGlory.Squad(UUID.randomUUID(),team,new Vec3d(px,top(b,px,pz),pz));s.order="hold";s.yaw=team==0?-90:90;
                double x=px+(team==0?-2:2);var u=b.spawn(s,troop(team==0?"knight":"revenant"),0,new Vec3d(x,top(b,x,pz),pz));freeze(u);pair.add(u);
            }
            b.points.get(0).owner=0;b.points.get(2).owner=1;
            b.relocate(p,AX-16,top(b,AX-16,AZ+9),AZ+9,-120,12);
            fixture=b.units.stream().filter(u->u.squad==own).count()+" own troops at "+(int)AX+", "+(int)AZ+", a frozen revenant in front of them, a frozen pair on "+spot.id()+" ("+spot.name()+" at "+spot.x()+", "+spot.z()+"), map "+b.campaign.map;
            System.out.println("[p1] ui-hud fixture: "+fixture);stage=2;
        }
        /** Every 20 ticks for 200 ticks: food 6 and 40 exhaustion; one tick later the pin must have put food back to 20 with no saturation. */
        void hunger(GravesAndGlory.Battle b){
            var p=sp(b);if(p==null)return;var hm=p.getHungerManager();
            if(hungerStart<0)hungerStart=b.time;long k=b.time-hungerStart;
            if(k%20==1&&k<=181){if(hm.getFoodLevel()==20&&hm.getSaturationLevel()==0)hungerOk++;else hungerWrong+=(hungerWrong.isEmpty()?"":", ")+"food "+hm.getFoodLevel()+" saturation "+hm.getSaturationLevel()+" at +"+k;}
            if(k%20==0&&k<200){hm.setFoodLevel(6);hm.addExhaustion(40);hungerSets++;}
            if(k>=181)hungerDone=true;
        }
        void five(GravesAndGlory.Battle b){
            var p=sp(b);if(p==null||!skirmish(b,p,4,1,"normal"))return;
            quiet(b,b.squads.get(player));int[] owners={0,0,-1,1,1};for(int i=0;i<b.points.size()&&i<owners.length;i++)b.points.get(i).owner=owners[i];
            System.out.println("[p1] ui-hud five seals: map "+b.campaign.map+", "+b.points.size()+" points");fiveReady=true;
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(stage==1)fixture(b);
            var own=player==null?null:b.squads.get(player);
            if(own!=null&&routSupport&&b.units.stream().anyMatch(u->u.squad==own&&u.spec.group().equals("support")&&u.mob.isAlive()))own.routedAt.put("support",b.time); // never long enough to regroup
            if(keepRespawn&&player!=null)b.respawns.put(player,b.time+12*20L);
            if(hungerOn&&!hungerDone)hunger(b);
            if(stage==71&&!fiveReady)five(b);
        }

        /** Each HUD frame: how many food draws the mixin skipped in it. During the hunger run, and while the five-seal battlefield
         *  builds (the client's ready is false), every frame at food 20 must skip the whole row: 20 draws, 10 backgrounds and 10 icons.
         *  Frames at lower food (the harness's food set can reach the client before the next tick's pin) are counted apart. */
        public void hudFrame(MinecraftClient c){
            int skipped=BattleHud.foodSkipped,delta=skipped-lastSkipped;lastSkipped=skipped;
            boolean build=watchBuild&&!GravesAndGloryClient.ready;
            if(!(watchHunger||build)||c.player==null||c.interactionManager==null||!c.interactionManager.hasStatusBars())return;
            boolean full=c.player.getHungerManager().getFoodLevel()==20;
            if(watchHunger){if(!full)hungerLow++;else{hungerFrames++;if(delta==20)hungerFull++;else if(hungerOdd.length()<60)hungerOdd+=(hungerOdd.isEmpty()?"":", ")+delta;}}
            if(build){if(!full)buildLow++;else{buildFrames++;if(delta==20)buildFull++;else if(buildOdd.length()<60)buildOdd+=(buildOdd.isEmpty()?"":", ")+delta;}}
        }
        /** While the five-seal battlefield builds, the camp screen the round's end opened hides the whole HUD (CinematicHudMixin
         *  cancels InGameHud under any ArmyScreen), so no food draw would run to be skipped. A player who presses Escape sees the HUD,
         *  hotbar and hearts included: the check closes the screen the same way (ArmyScreen's Escape is setScreen(null)). */
        @Override public void clientTick(MinecraftClient c){
            if(watchBuild&&!GravesAndGloryClient.ready&&GravesAndGloryClient.menuVisible()){c.setScreen(null);buildCloses++;}
        }
        void reportBuild(){
            watchBuild=false;
            report("p1-ui-no-hunger-build",buildFrames>=20&&buildFull==buildFrames,"while the five-seal battlefield built (client ready false) "+buildFrames+" HUD frames had food 20 and "+buildFull+" of them skipped all 20 food draws"+(buildOdd.isEmpty()?"":" (the others skipped "+buildOdd+")")+(buildLow>0?"; "+buildLow+" frames at lower food left out":"")+"; camp screens closed during the build (as Escape would): "+buildCloses);
        }

        public void step(MinecraftClient c,int tick){
            if(tick>9000&&!finished){report("p1-ui-hud-timeout",false,"stuck in stage "+stage);end(c);return;}
            if(keepLesson)CampaignClient.tutorial=lessonStep;
            if(Math.abs(stage)>=2&&stage<99&&stage!=46&&stage!=47&&stage!=49&&c.currentScreen!=null)c.setScreen(null); // stages 46, 47 and 49 hold the chat open
            switch(stage){
                case 0->{
                    if(tick<20)return;c.setScreen(null);var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();
                    campLift=ChatPlacement.lift(w.getScaledHeight());CommandMode.selected="all";UUID id=c.player.getUuid();window(c,960,540,1);waitFrom=tick;stage=-1;server(c,b->{player=id;stage=1;});
                }
                case -1,1->{if(tick-waitFrom>2400){report("p1-ui-hud-fixture",false,"no fixture 2400 ticks after the request");end(c);}}
                case 2->{ // the fixture on screen; then p1-ui-seals, part one
                    var inf=group("infantry");var ran=group("ranged");var sup=group("support");
                    boolean shown=GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()&&inf!=null&&inf.count()==9&&ran!=null&&ran.count()==6&&sup!=null&&sup.count()==1
                        &&GravesAndGloryClient.ownerAt(0)==0&&GravesAndGloryClient.ownerAt(2)==1&&GravesAndGloryClient.contestedAt(1)&&!GravesAndGloryClient.contestedAt(0)&&!GravesAndGloryClient.contestedAt(2);
                    if(shown&&seen==0){seen=tick;BattleHud.roundShownMs=net.minecraft.util.Util.getMeasuringTimeMs()-61000;} // past the hint layer of the round's first 60 s (suite ui-feed checks it)
                    if(seen>0&&tick>=seen+2){sealsOk=BattleHud.seals().equals(SEALS);sealsWhy="owners [0, contested, 1]: "+BattleHud.seals()+(sealsOk?"":" (wanted "+SEALS+")");seen=0;stage=3;}
                    else if(seen==0&&tick-waitFrom>2400){report("p1-ui-hud-fixture",false,"the client never showed the fixture ("+fixture+"): groups "+CommandMode.groups+", owners "+Arrays.toString(GravesAndGloryClient.owners)+", contested "+Arrays.toString(GravesAndGloryClient.contested));end(c);}
                }
                case 3->{hungerOn=true;watchHunger=true;waitFrom=tick;stage=4;}
                case 4->{ // p1-ui-no-hunger
                    if(tick==waitFrom+30)shot(c,"ui-hud-no-hunger");
                    if(hungerDone&&tick>waitFrom+30){
                        watchHunger=false;
                        report("p1-ui-no-hunger",hungerSets==10&&hungerOk==10&&hungerFrames>=20&&hungerFull==hungerFrames,hungerSets+" times the server set the food to 6 and added 40 exhaustion; one tick later the food was 20 and the saturation 0 "+hungerOk+" times"+(hungerWrong.isEmpty()?"":" (wrong: "+hungerWrong+")")+"; "+hungerFull+" of "+hungerFrames+" HUD frames at food 20 skipped all 20 food draws"+(hungerOdd.isEmpty()?"":" (the others skipped "+hungerOdd+")")+(hungerLow>0?"; "+hungerLow+" frames at lower food left out":""));
                        hungerOn=false;stage=10;
                    }else if(tick-waitFrom>600){watchHunger=false;report("p1-ui-no-hunger",false,"the hunger run did not finish: "+hungerSets+" sets, "+hungerOk+" good, "+hungerFrames+" frames at food 20 measured");hungerOn=false;stage=10;}
                }
                case 10->{ // p1-ui-cards: ranged selected, support routing, then infantry hurt
                    CommandMode.selected="ranged";routSupport=true;waitFrom=tick;stage=-10;
                    server(c,b->{b.scores[0]=148;b.scores[1]=121;b.roundStarted=b.time-(b.roundSeconds-462)*20L;stage=11;}); // the mock's 148, 7:42 and 121
                }
                case 11->{
                    var cs=BattleHud.cards();
                    if(cs.size()==3&&cs.get(2).state().equals("ROUTING")&&cs.get(1).tall()&&!CommandMode.flashing("infantry")){
                        waitFrom=tick;stage=-12;
                        server(c,b->{var own=b.squads.get(player);int n=0;
                            for(var u:b.units)if(n<4&&u.squad==own&&u.spec.group().equals("infantry")&&u.mob.isAlive()&&u.mob.getHealth()>10){u.mob.damage(b.world.getDamageSources().magic(),10);n++;}
                            System.out.println("[p1] ui-hud: "+n+" knights take 10 magic damage");stage=12;});
                    }else if(tick-waitFrom>200){report("p1-ui-cards",false,"support never drew ROUTING with ranged selected: "+cs);stage=20;}
                }
                case 12->{
                    if(CommandMode.flashing("infantry")){flashAt=tick;stage=13;}
                    else if(tick-waitFrom>40){shot(c,"ui-hud-cards");report("p1-ui-cards",false,"no infantry flash within 40 client ticks of the damage");stage=20;}
                }
                case 13->{
                    var cs=BattleHud.cards();var t=BattleHud.texts();boolean cards=cs.equals(CARDS);
                    // The fixture's counts and words; the ledger as the server set it (148, 7:42, 121), allowing one score tick and the seconds since.
                    boolean words="9/12".equals(t.get("count-infantry"))&&"6/6".equals(t.get("count-ranged"))&&"1/2".equals(t.get("count-support"))
                        &&"Fighting".equals(t.get("state-infantry"))&&"Steady".equals(t.get("state-ranged"))&&"ROUTING".equals(t.get("state-support"));
                    int dawn=GravesAndGloryClient.dawn,dusk=GravesAndGloryClient.dusk,secs=CommandMode.seconds;
                    boolean ledger=String.valueOf(dawn).equals(t.get("score-dawn"))&&String.valueOf(dusk).equals(t.get("score-dusk"))&&UiMath.clock(secs).equals(t.get("clock"))
                        &&dawn>=148&&dawn<=149&&dusk>=121&&dusk<=122&&secs>=442&&secs<=462;
                    boolean match=cards&&words&&ledger;
                    if(match||tick>=flashAt+3){shot(c,"ui-hud-cards");
                        report("p1-ui-cards",match,(tick-flashAt)+" client tick(s) after the flash the cards drew "+cs+(cards?"":" (wanted "+CARDS+")")+"; counts "+t.get("count-infantry")+", "+t.get("count-ranged")+", "+t.get("count-support")+" (wanted 9/12, 6/6, 1/2); words "+t.get("state-infantry")+", "+t.get("state-ranged")+", "+t.get("state-support")+" (wanted Fighting, Steady, ROUTING); ledger "+t.get("score-dawn")+" "+t.get("clock")+" "+t.get("score-dusk")+" (client "+dawn+", "+UiMath.clock(secs)+", "+dusk+"; wanted 148 or 149, 7:22 to 7:42, 121 or 122)");stage=20;}
                }
                case 20->{CommandMode.selected="all";routSupport=false;waitFrom=tick;stage=-20;server(c,b->{b.squads.get(player).routedAt.remove("support");stage=21;});}
                case 21->{ // p1-ui-army-selected
                    var sup=group("support");boolean settled=sup!=null&&!sup.routing()&&!CommandMode.flashing("infantry")&&!CommandMode.flashing("ranged")&&!CommandMode.flashing("support");
                    if(!settled)seen=0;else if(seen==0)seen=tick;
                    if(seen>0&&tick>=seen+2){var cs=BattleHud.cards();
                        boolean ok=cs.size()==3&&cs.stream().allMatch(d->d.state().equals("SELECTED"))&&cs.stream().map(BattleHud.CardDraw::tall).toList().equals(List.of(false,false,true))&&cs.stream().map(BattleHud.CardDraw::y).toList().equals(List.of(348,404,460));
                        report("p1-ui-army-selected",ok,"with the whole army selected the cards drew "+cs);seen=0;stage=30;}
                    else if(seen==0&&tick-waitFrom>200){report("p1-ui-army-selected",false,"support never stopped routing, or a flash never ended: "+BattleHud.cards());stage=30;}
                }
                case 30->{askAt=tick;stage=-30;server(c,b->{b.testAbility=new GravesAndGlory.Battle.AbilityState("rally_horn",960,1800);stage=31;});}
                case 31->{ // p1-ui-ability
                    if(abilitySeen<0&&GravesAndGloryClient.abilityId.equals("rally_horn"))abilitySeen=tick;
                    if(abilitySeen>=0&&tick>=abilitySeen+1){
                        int[] r=BattleHud.rects().get("ability");String secs=BattleHud.texts().get("ability-seconds");
                        abilityOk=abilitySeen-askAt<=12&&Arrays.equals(r,new int[]{584,493,50,32})&&"48".equals(secs);
                        abilityWhy="the client read abilityId rally_horn "+(abilitySeen-askAt)+" client ticks after the request (10 server ticks, the hand-off and a frame allowed: 12); rect ability "+box(r)+", seconds \""+secs+"\", key \""+BattleHud.texts().get("ability-key")+"\"";
                        shot(c,"ui-hud-ability");askAt=tick;stage=-32;server(c,b->{b.testAbility=null;stage=32;});
                    }else if(abilitySeen<0&&tick-askAt>60){report("p1-ui-ability",false,"the client never read rally_horn (abilityId "+GravesAndGloryClient.abilityId+")");stage=40;}
                }
                case 32->{
                    if(!BattleHud.rects().containsKey("ability")&&GravesAndGloryClient.abilityId.equals("none")){report("p1-ui-ability",abilityOk&&tick-askAt<=12,abilityWhy+"; with testAbility null the slot was gone "+(tick-askAt)+" client ticks later");stage=40;}
                    else if(tick-askAt>60){report("p1-ui-ability",false,abilityWhy+"; the slot was still drawn 60 ticks after testAbility was cleared");stage=40;}
                }
                case 40->{keepLesson=true;keepRespawn=true;win=0;stage=-40;server(c,b->{b.testAbility=new GravesAndGlory.Battle.AbilityState("rally_horn",960,1800);stage=41;});}
                case 41->{var s=WINDOWS[win];window(c,s[0],s[1],s[2],s[3]==1);waitFrom=tick;stage=42;
                    var chat=c.inGameHud.getChatHud();chat.clear(false);chat.addMessage(net.minecraft.text.Text.literal("Chat check: hold the ford"));chat.addMessage(net.minecraft.text.Text.literal("Chat check: archers to the ridge"));}
                case 42->{ // p1-ui-small-window, one window size
                    var r=BattleHud.rects();var s=WINDOWS[win];var score=r.get("score");var fb=c.getWindow();
                    if(minimized(c)){window(c,s[0],s[1],s[2],s[3]==1);return;} // minimized since the size was set: show it again, then measure
                    boolean sized=fb.getFramebufferWidth()==s[0]&&fb.getFramebufferHeight()==s[1]&&(int)fb.getScaleFactor()==s[2];
                    boolean full=sized&&r.containsKey("lesson")&&r.containsKey("respawn")&&r.containsKey("ability")&&r.containsKey("spine")&&BattleHud.cards().size()==3&&score!=null&&score[0]==UiCanvas.width()/2-128;
                    if(full||tick-waitFrom>100){
                        if(full)measure(c,s,r);else{smallOk=false;smallWhy+=(smallWhy.isEmpty()?"":"; ")+s[0]+"x"+s[1]+" GUI "+s[2]+": no frame with every element in 100 ticks (drew "+r.keySet()+")";}
                        if(win==0)shot(c,"ui-hud-chat");if(win==1)shot(c,"ui-hud-small");if(win==2)shot(c,"ui-hud-1280");if(win==3)shot(c,"ui-hud-1080-gui2");if(win==4)shot(c,"ui-hud-1080-gui4");if(win==5)shot(c,"ui-hud-1366");
                        // then the chat open at this size (UI polish round 2), 25 lines in it: stage 47 measures it
                        var chat=c.inGameHud.getChatHud();chat.clear(false);for(int i=1;i<=25;i++)chat.addMessage(net.minecraft.text.Text.literal("Chat check line "+i));
                        c.setScreen(new net.minecraft.client.gui.screen.ChatScreen(""));waitFrom=tick;stage=47;
                    }
                }
                case 47->{if(tick-waitFrom>=3){measureOpen(c,WINDOWS[win],BattleHud.rects());c.setScreen(null);win++;stage=win<WINDOWS.length?41:45;}}
                case 45->{ // p1-ui-chat-respawn (UI polish M2): at 1366x768 GUI 2, the last window, the open chat stops below the respawn card
                    var chat=c.inGameHud.getChatHud();chat.clear(false);for(int i=1;i<=25;i++)chat.addMessage(net.minecraft.text.Text.literal("Chat check line "+i));
                    c.setScreen(new net.minecraft.client.gui.screen.ChatScreen(""));waitFrom=tick;stage=46;}
                case 46->{
                    if(tick-waitFrom>=3){var w=c.getWindow();var hud=c.inGameHud.getChatHud();int[] dr=ChatPlacement.drawn,card=BattleHud.rects().get("respawn"),chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);
                        boolean open=c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;int shown=hud.getVisibleLineCount();shot(c,"ui-hud-chat-open");
                        report("p1-ui-chat-respawn",open&&card!=null&&chat!=null&&shown==10&&ChatPlacement.drawnLines==10&&!meets(chat,card),"with the chat open (true: "+open+") at "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+" GUI "+(int)w.getScaleFactor()+" and 25 lines in it, it shows "+shown+" lines (wanted 10 of vanilla's 20) and drew "+ChatPlacement.drawnLines+" at canvas "+box(chat)+" (GUI "+box(dr)+"); the respawn card at "+box(card));
                        c.setScreen(null);keepRespawn=false;window(c,1280,720,3);waitFrom=tick;stage=-46;server(c,b->{b.respawns.remove(player);stage=48;});}
                }
                case 48->{ // p1-ui-chat-open (UI polish round 2): typing with the commander up at 1280x720 GUI 3, the crosshair over the chat
                    var r=BattleHud.rects();var fb=c.getWindow();
                    if(minimized(c)){window(c,1280,720,3);return;}
                    boolean sized=fb.getFramebufferWidth()==1280&&fb.getFramebufferHeight()==720&&(int)fb.getScaleFactor()==3;
                    if(sized&&r.containsKey("lesson")&&r.containsKey("spine")&&!r.containsKey("respawn")||tick-waitFrom>200){
                        var chat=c.inGameHud.getChatHud();closedLines=chat.getVisibleLineCount();chat.clear(false);for(int i=1;i<=25;i++)chat.addMessage(net.minecraft.text.Text.literal("Chat check line "+i));
                        c.setScreen(new net.minecraft.client.gui.screen.ChatScreen(""));waitFrom=tick;stage=49;}
                }
                case 49->{
                    if(tick-waitFrom>=3){var w=c.getWindow();var hud=c.inGameHud.getChatHud();var r=BattleHud.rects();int sw=w.getScaledWidth(),sh=w.getScaledHeight(),shown=hud.getVisibleLineCount();
                        int[] dr=ChatPlacement.drawn,cross={(sw-15)/2,(sh-15)/2,15,15},chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);boolean open=c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen,over=meets(dr,cross);
                        List<String> hit=new ArrayList<>();if(chat!=null)for(var e:r.entrySet())if(meets(chat,e.getValue()))hit.add(e.getKey());shot(c,"ui-hud-chat-typing");
                        report("p1-ui-chat-open",open&&chat!=null&&r.containsKey("lesson")&&!r.containsKey("respawn")&&closedLines==4&&shown==13&&ChatPlacement.drawnLines==13&&hit.isEmpty()&&over,"at "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+" GUI "+(int)w.getScaleFactor()+" with the commander up (a respawn card: "+r.containsKey("respawn")+") and the lesson card at "+box(r.get("lesson"))+", the closed chat showed "+closedLines+" lines (wanted 4, the crosshair above it); open (true: "+open+") with 25 lines in it, it shows "+shown+" and drew "+ChatPlacement.drawnLines+" at GUI "+box(dr)+" (wanted 13, the lesson card above it)"+(hit.isEmpty()?", apart from every element of the HUD":", meeting "+hit)+", "+(over?"over":"clear of")+" the crosshair at GUI "+box(cross));
                        c.setScreen(null);stage=43;}
                }
                case 43->{keepLesson=false;keepRespawn=false;CampaignClient.tutorial=-1;report("p1-ui-small-window",smallOk,smallWhy);report("p1-ui-chat-lift",chatOk&&campLift==0,"in camp the chat moved "+campLift+" GUI px; "+chatWhy);window(c,960,540,1);
                    chatScale=c.options.getChatScale().getValue();c.options.getChatScale().setValue(0.6);
                    var chat=c.inGameHud.getChatHud();chat.clear(false);chat.addMessage(net.minecraft.text.Text.literal("Chat check: hold the ford"));chat.addMessage(net.minecraft.text.Text.literal("Chat check: archers to the ridge"));waitFrom=tick;stage=44;}
                case 44->{ // p1-ui-chat-edge (UI polish M5): at 60% chat text size the cut's right edge is where vanilla ends the lines it draws
                    if(tick-waitFrom>=5){var hud=c.inGameHud.getChatHud();int[] dr=ChatPlacement.drawn;double scale=hud.getChatScale(),right=ChatPlacement.right(hud.getWidth(),scale);
                        report("p1-ui-chat-edge",dr!=null&&Math.abs(scale-0.6)<1e-6&&Math.abs(dr[0]+dr[2]-right)<1,"at chat text size "+scale+" and width "+hud.getWidth()+" the line backgrounds were drawn to GUI x "+(dr==null?"none":String.valueOf(dr[0]+dr[2]))+"; ChatPlacement.right gives "+String.format(Locale.ROOT,"%.2f",right)+" (the old (width + 12) x scale was "+String.format(Locale.ROOT,"%.2f",(hud.getWidth()+12)*scale)+")");
                        c.options.getChatScale().setValue(chatScale);chatScale=-1;stage=-44;server(c,b->{b.respawns.remove(player);b.testAbility=null;stage=50;});}
                }
                case 50->{waitFrom=tick;stage=-50;server(c,b->{kill(b,b.squads.get(player),"support",9);stage=51;});}
                case 51->{ // p1-ui-cards-edge
                    var sup=BattleHud.cards().stream().filter(d->d.group().equals("support")).findFirst().orElse(null);String count=BattleHud.texts().get("count-support");
                    boolean fallen=sup!=null&&sup.state().equals("FALLEN");
                    if(fallen||tick-waitFrom>100){
                        edgeOk=fallen&&"0/2".equals(count);edgeWhy="with every support troop killed the support card drew "+sup+" with count \""+count+"\"";
                        CommandMode.selected="infantry";waitFrom=tick;stage=-52;
                        server(c,b->{var own=b.squads.get(player);own.fullCount.remove("support");own.fullHealth.remove("support");stage=52;});
                    }
                }
                case 52->{
                    var cs=BattleHud.cards();
                    if(cs.size()==2){two=List.copyOf(cs);framesAt=BattleHud.frames;stage=53;} // cards() is a view the next frame refills
                    else if(tick-waitFrom>100){report("p1-ui-cards-edge",false,edgeWhy+"; with support's full count gone the cards still drew "+cs);stage=60;}
                }
                case 53->{
                    if(BattleHud.frames>=framesAt+100){
                        boolean ok=edgeOk&&two.stream().map(BattleHud.CardDraw::y).toList().equals(List.of(404,474))&&two.get(0).group().equals("infantry")&&two.get(0).tall()&&BattleHud.cards().size()==2;
                        report("p1-ui-cards-edge",ok,edgeWhy+"; with support's full count and health gone and infantry selected the cards drew "+two+"; "+(BattleHud.frames-framesAt)+" frames later the HUD still draws "+BattleHud.cards().size()+" cards");stage=60;
                    }else if(tick-waitFrom>600){report("p1-ui-cards-edge",false,edgeWhy+"; only "+(BattleHud.frames-framesAt)+" HUD frames in 600 ticks after the two-card layout");stage=60;}
                }
                case 60->{CommandMode.selected="all";waitFrom=tick;stage=-60;server(c,b->{for(var p:b.points){p.owner=-1;p.progress=0;p.challenger=-1;}for(var u:pair)u.mob.discard();b.units.removeAll(pair);pair.clear();stage=61;});}
                case 61->{ // p1-ui-seals, part two
                    boolean neutral=GravesAndGloryClient.owners.length==3;for(int i=0;i<GravesAndGloryClient.owners.length;i++)if(GravesAndGloryClient.ownerAt(i)>=0||GravesAndGloryClient.contestedAt(i))neutral=false;
                    if(neutral&&seen==0)seen=tick;
                    if(seen>0&&tick>=seen+2){var ss=BattleHud.seals();boolean all=ss.size()==3&&ss.stream().allMatch(v->v.sprite().equals("hud/seal-neutral"));
                        report("p1-ui-seals",sealsOk&&all,sealsWhy+"; every owner -1: "+ss);seen=0;stage=62;}
                    else if(seen==0&&tick-waitFrom>200){report("p1-ui-seals",false,sealsWhy+"; the client never showed every point neutral and uncontested");stage=62;}
                }
                // p1-ui-aim-name (controller addition C): a move aimed into point B hangs B's name below its seal, at canvas y 68 where the
                // seal rects end (UI polish M3: it was 62, over the seals' feet), and every rect of that frame is apart, the name's too
                case 62->{waitFrom=tick;stage=-62;server(c,b->{var p=sp(b);var spot=b.points.get(1).spec;double x=spot.x()-6;if(p!=null)b.relocate(p,x,top(b,x,spot.z()),spot.z(),-90,35);stage=63;});}
                case 63->{if(tick-waitFrom>=10){CommandMode.begin("move");seen=0;waitFrom=tick;stage=64;}}
                case 64->{
                    if(seen==0&&CommandMode.aimPoint()==1)seen=tick;
                    if(seen>0&&tick>=seen+3){
                        var font=c.textRenderer;String want=Spec.POINTS[1].name(),got=BattleHud.texts().get("aim-name");int[] r=BattleHud.rects().get("aim-name");
                        int[] wantR={BattleHud.centred(font,want,UiMath.sealCentre(1,GravesAndGloryClient.owners.length,UiCanvas.width())),68,font.getWidth(want)+1,9};
                        var touch=touching(BattleHud.rects());shot(c,"ui-hud-aim");
                        report("p1-ui-aim-name",want.equals(got)&&Arrays.equals(r,wantR)&&CommandMode.aimPoint()==1&&touch.isEmpty(),"a move aimed at "+CommandMode.destination+" (point "+Spec.POINTS[1].id()+") drew the name '"+got+"' at "+box(r)+" (wanted '"+want+"' at "+box(wantR)+"); seals "+box(BattleHud.rects().get("seal-0"))+", "+box(BattleHud.rects().get("seal-1"))+", "+box(BattleHud.rects().get("seal-2"))+"; of the frame's "+BattleHud.rects().size()+" rects ("+String.join(", ",BattleHud.rects().keySet())+") "+(touch.isEmpty()?"none touch":"these touch: "+String.join("; ",touch)));
                        CommandMode.cancel();seen=0;stage=70;
                    }else if(seen==0&&tick-waitFrom>100){report("p1-ui-aim-name",false,"the aim never came into point "+Spec.POINTS[1].id()+" in 100 ticks (aimPoint "+CommandMode.aimPoint()+", aimed at "+CommandMode.destination+")");CommandMode.cancel();stage=70;}
                }
                case 70->{waitFrom=tick;fiveReady=false;watchBuild=true;stage=71;} // the server switches to Kingsfield; the build is watched frame by frame
                case 71->{ // p1-ui-seals-five
                    if(seen==0&&fiveReady&&GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()&&GravesAndGloryClient.owners.length==5){seen=tick;BattleHud.roundShownMs=net.minecraft.util.Util.getMeasuringTimeMs()-61000;}
                    if(seen>0&&tick>=seen+3){
                        var ss=BattleHud.seals();var r=BattleHud.rects();var xs=ss.stream().map(BattleHud.SealDraw::x).toList();
                        boolean clear=r.containsKey("seal-0")&&r.containsKey("seal-4")&&!meets(r.get("seal-0"),r.get("dawn-boss"))&&!meets(r.get("seal-4"),r.get("dusk-boss"));
                        shot(c,"ui-hud-five-seals");
                        report("p1-ui-seals-five",xs.equals(List.of(411,439,467,495,523))&&clear,"map "+CampaignClient.map+": seals "+ss+"; seal-0 "+box(r.get("seal-0"))+" and dawn-boss "+box(r.get("dawn-boss"))+", seal-4 "+box(r.get("seal-4"))+" and dusk-boss "+box(r.get("dusk-boss")));
                        reportBuild();seen=0;stage=80;
                    }else if(seen==0&&tick-waitFrom>2400){reportBuild();report("p1-ui-seals-five",false,"no five-point battle 2400 ticks after the request (owners "+GravesAndGloryClient.owners.length+", server ready "+fiveReady+")");end(c);}
                }
                case 80->{keepLesson=true;lessonStep=3;waitFrom=tick;stage=81;} // p1-ui-hud-cache: lesson 4 pinned
                case 81->{ // after 10 ticks lesson 4's wrap is cached: the next 40 frames wrap nothing and keep the same read-back views
                    if(tick-waitFrom>=10&&BattleHud.rects().containsKey("lesson")){shot(c,"ui-hud-lesson");System.out.println("[p1] ui-hud-lesson: the three-v lesson card at "+box(BattleHud.rects().get("lesson"))+" with "+GravesAndGloryClient.lessonDrawn.size()+" lines"); // controller addition B
                        cacheW=BattleHud.wraps;cacheF=BattleHud.frames;lessonBefore=BattleHud.texts().get("lesson");views=new Object[]{BattleHud.rects(),BattleHud.texts(),BattleHud.cards(),BattleHud.seals()};waitFrom=tick;stage=82;}
                    else if(tick-waitFrom>200)lateFail(c,"the lesson card was not drawn 200 ticks after lesson 4 was pinned");
                }
                case 82->{
                    if(BattleHud.frames-cacheF>=40){sameWraps=BattleHud.wraps-cacheW;viewsSame=views[0]==BattleHud.rects()&&views[1]==BattleHud.texts()&&views[2]==BattleHud.cards()&&views[3]==BattleHud.seals();
                        cacheW=BattleHud.wraps;cacheF=BattleHud.frames;lessonStep=2;waitFrom=tick;stage=83;}
                    else if(tick-waitFrom>200)lateFail(c,"40 HUD frames did not pass in 200 ticks ("+(BattleHud.frames-cacheF)+" did)");
                }
                case 83->{ // lesson 3 pinned: one wrap for the new copy over the next 40 frames
                    if(tick-waitFrom>=10&&BattleHud.frames-cacheF>=40&&BattleHud.texts().containsKey("lesson")&&!BattleHud.texts().get("lesson").equals(lessonBefore)){stepWraps=BattleHud.wraps-cacheW;stage=84;}
                    else if(tick-waitFrom>200)lateFail(c,"lesson 3 was not drawn 200 ticks after it was pinned");
                }
                case 84->{ // p1-ui-hud-guard: an atlas without hud/spine, as a resource pack's could be, must stop the HUD and not the client
                    try{byte[] json=atlasWithout(c,"hud/spine").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        UiTex.load(id->id.equals(UiTex.ATLAS)?Optional.of(new net.minecraft.resource.Resource(null,()->new java.io.ByteArrayInputStream(json))):Optional.empty());
                    }catch(Exception e){lateFail(c,"could not write an atlas without hud/spine: "+e);return;}
                    guardF=BattleHud.frames;waitFrom=tick;stage=85;
                }
                case 85->{ // 10 ticks on that atlas, then a full resource reload (F3+T's), which reads the real atlas again
                    if(tick-waitFrom>=10){
                        guardStopped=!UiTex.has("hud/spine")&&BattleHud.frames==guardF&&BattleHud.rects().isEmpty()&&BattleHud.cards().isEmpty();
                        guardWhy="with hud/spine gone from the atlas ("+UiTex.count()+" textures) the client ran on and the HUD drew "+(BattleHud.frames-guardF)+" whole frames in 10 ticks, leaving "+BattleHud.rects().size()+" rects";
                        cacheW=BattleHud.wraps;reloading=c.reloadResources();waitFrom=tick;stage=86;
                    }
                }
                case 86->{ // the step stops while the reload's overlay is up
                    if(reloading.isDone()){guardF=BattleHud.frames;waitFrom=tick;stage=87;}
                    else if(tick-waitFrom>1200)lateFail(c,"the resource reload had not finished 1200 ticks after it began");
                }
                case 87->{
                    if(tick-waitFrom>=10){
                        boolean back=!reloading.isCompletedExceptionally()&&UiTex.has("hud/spine")&&BattleHud.frames>guardF&&BattleHud.rects().containsKey("spine")&&BattleHud.rects().containsKey("lesson");
                        reloadWraps=BattleHud.wraps-cacheW;
                        report("p1-ui-hud-guard",guardStopped&&back,guardWhy+"; after the resource reload the atlas has "+UiTex.count()+" textures and the HUD drew "+(BattleHud.frames-guardF)+" frames in 10 ticks with the spine "+box(BattleHud.rects().get("spine"))+" and the lesson "+box(BattleHud.rects().get("lesson")));
                        report("p1-ui-hud-cache",sameWraps==0&&viewsSame&&stepWraps==1&&reloadWraps==1,"lesson 4 held for 40 frames: "+sameWraps+" wraps (wanted 0), the same read-back views="+viewsSame+"; lesson 3 for 40 frames: "+stepWraps+" wrap (wanted 1); after the resource reload: "+reloadWraps+" (wanted 1)");
                        keepLesson=false;lessonStep=3;CampaignClient.tutorial=-1;end(c);
                    }
                }
                case 99->{stage=100;finish(c,"ui-hud");}
                default->{}
            }
        }

        /** One window of p1-ui-small-window: every rect inside the canvas, none touching another (but a card and the spine, a boss and
         *  the score card), and none touching the vanilla hotbar or hearts converted to canvas px. */
        void measure(MinecraftClient c,int[] s,Map<String,int[]> r){
            var w=c.getWindow();int cw=UiCanvas.width(),ch=UiCanvas.height(),sw=w.getScaledWidth(),sh=w.getScaledHeight();List<String> bad=new ArrayList<>();
            if(w.getFramebufferWidth()!=s[0]||w.getFramebufferHeight()!=s[1]||(int)w.getScaleFactor()!=s[2])bad.add("the window is "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+" at GUI "+(int)w.getScaleFactor());
            for(var e:r.entrySet())if(!inside(e.getValue(),cw,ch))bad.add(e.getKey()+" "+box(e.getValue())+" leaves the canvas");
            bad.addAll(touching(r));
            int[] hotbar=canvas(c,sw/2-91,sh-22,182,22),hearts=canvas(c,sw/2-91,sh-39,81,9);
            for(var e:r.entrySet()){if(meets(e.getValue(),hotbar))bad.add(e.getKey()+" meets the hotbar");if(meets(e.getValue(),hearts))bad.add(e.getKey()+" meets the hearts");}
            smallOk&=bad.isEmpty();
            // p1-ui-chat-lift: the chat's lines above the left column (ChatPlacement), its top below vanilla's crosshair wherever it
            // reaches the screen's centre (UI polish M1), and the lines it shows closed, the window's wanted count (measureOpen: open)
            var hud=c.inGameHud.getChatHud();int top=ChatPlacement.columnTop(r);int[] dr=ChatPlacement.drawn,cross={(sw-15)/2,(sh-15)/2,15,15};
            int[] chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);String first=r.keySet().stream().filter(k->k.startsWith("card-")).min(Comparator.comparingInt(k->r.get(k)[1])).orElse(null);
            int shown=hud.getVisibleLineCount();
            boolean clear=dr!=null&&(dr[0]+dr[2]<=cross[0]||dr[1]>=cross[1]+cross[3]);
            boolean apart=chat!=null&&top>=0&&chat[1]+chat[3]<=top&&ChatPlacement.drawnLines==Math.min(2,s[4])&&shown==s[4]&&clear&&first!=null&&!meets(chat,r.get(first))&&!meets(chat,r.get("respawn"));chatOk&=apart;
            chatWhy+=(chatWhy.isEmpty()?"":"; ")+s[0]+"x"+s[1]+" GUI "+s[2]+": chat scale "+hud.getChatScale()+", lifted "+ChatPlacement.lift(sh)+" GUI px, "+ChatPlacement.drawnLines+" lines drawn at canvas "+box(chat)+" (GUI "+box(dr)+"), the column's top at "+top+" and "+first+" "+box(first==null?null:r.get(first))+", the crosshair at GUI "+box(cross)+(clear?" clear":" COVERED")+", the respawn card at "+box(r.get("respawn"))+"; closed it shows "+shown+" lines (wanted "+s[4]+")"+(apart?"":", WRONG");
            smallWhy+=(smallWhy.isEmpty()?"":"; ")+s[0]+"x"+s[1]+" GUI "+s[2]+": canvas "+cw+"x"+ch+" (k "+UiCanvas.k()+"), "+r.size()+" rects, cards "+BattleHud.cards().stream().map(d->d.group()+" "+d.y()).toList()+", ability "+box(r.get("ability"))+", lesson "+box(r.get("lesson"))+", respawn "+box(r.get("respawn"))+", spine "+box(r.get("spine"))+", hotbar "+box(hotbar)+", hearts "+box(hearts)+(bad.isEmpty()?", every rect inside and apart":", WRONG: "+String.join("; ",bad.subList(0,Math.min(6,bad.size()))));
        }

        /** p1-ui-chat-lift at one window with the chat open (UI polish round 2): it shows and draws the window's open count, apart
         *  from every element of the HUD; over the crosshair is allowed, and the log says which. */
        void measureOpen(MinecraftClient c,int[] s,Map<String,int[]> r){
            var w=c.getWindow();var hud=c.inGameHud.getChatHud();int sw=w.getScaledWidth(),sh=w.getScaledHeight(),lh=((gg.gravesandglory.mixin.ChatHudAccessor)hud).gravesandglory$getLineHeight(),shown=hud.getVisibleLineCount();
            int vanilla=net.minecraft.client.gui.hud.ChatHud.getHeight(c.options.getChatHeightFocused().getValue())/lh;int[] dr=ChatPlacement.drawn,cross={(sw-15)/2,(sh-15)/2,15,15},chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);
            boolean open=c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;List<String> hit=new ArrayList<>();if(chat!=null)for(var e:r.entrySet())if(meets(chat,e.getValue()))hit.add(e.getKey());
            boolean ok=open&&chat!=null&&shown==s[5]&&ChatPlacement.drawnLines==s[5]&&hit.isEmpty();chatOk&=ok;
            chatWhy+=(chatWhy.isEmpty()?"":"; ")+s[0]+"x"+s[1]+" GUI "+s[2]+" open"+(open?"":" (NOT open)")+": "+shown+" of vanilla's "+vanilla+" lines shown and "+ChatPlacement.drawnLines+" drawn at GUI "+box(dr)+" (wanted "+s[5]+")"+(hit.isEmpty()?", apart from every element of the HUD":", meeting "+hit)+", "+(meets(dr,cross)?"over":"clear of")+" the crosshair"+(ok?"":", WRONG");
        }

        /** The real atlas JSON with one texture left out. */
        static String atlasWithout(MinecraftClient c,String name)throws java.io.IOException{
            try(var reader=c.getResourceManager().getResource(UiTex.ATLAS).orElseThrow().getReader()){
                JsonObject atlas=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();atlas.getAsJsonObject("textures").remove(name);return atlas.toString();
            }
        }
        /** p1-ui-hud-cache or p1-ui-hud-guard could not finish: both fail, the real atlas and the HUD come back, and the suite ends. */
        void lateFail(MinecraftClient c,String why){
            report("p1-ui-hud-cache",false,why);report("p1-ui-hud-guard",false,why);
            UiTex.load(c.getResourceManager());BattleHud.RELOADER.reload(c.getResourceManager());keepLesson=false;lessonStep=3;CampaignClient.tutorial=-1;end(c);
        }
        /** Puts the window, the selection and every forced value back; p1-ui-hud-suite follows once the window has settled. */
        void end(MinecraftClient c){
            finished=true;keepLesson=false;keepRespawn=false;routSupport=false;hungerOn=false;watchHunger=false;watchBuild=false;CommandMode.selected="all";
            if(chatScale>=0){c.options.getChatScale().setValue(chatScale);chatScale=-1;}
            if(c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)c.setScreen(null);
            UUID id=player;server(c,b->{b.testAbility=null;if(id!=null)b.respawns.remove(id);});
            if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;
        }
    }

    /** UI Task 3: the battle event feed (feed_v1), key hints from the player's own bindings, the hint layer and the map's glyphs, over
     *  four practice rounds: A on map 0 (the bindings, the hint layer, the map), B on map 0 (a point taken, losses and kills), C on
     *  map 0 (a rout, a deploy, a fallen solo commander) and D on Kingsfield (the flood). Each round goes back to the camp before the
     *  next starts, so the client sees running turn false and then true: the feed clears and the hint layer's first 60 s begin again.
     *  The window is 960x540 at GUI 1 (k 1, the mock's size), but GUI 2 for the map's screenshot. */
    static final class UiFeedChecks implements Suite,ServerWatch {
        static final double AX=-220,AZ=0; // open ground on map 0's Dawn road (UiHudChecks.AX)
        static final String NOTICE="{key:menu} opens your war council.";
        static final String LONG="The long notice of the flood check runs on well past the width of the feed slip, so the slip trims it";
        static final List<List<String>> HINT_ROWS=List.of(List.of("Not bound","Map","K","Commands"),List.of("[","]","Turn","Left Alt","Hints"));
        static final List<String> AIM_ROW=List.of("LMB","Confirm","RMB","Cancel");
        static final List<String> COLOURS=List.of("red","blue","green","gold","yellow","purple","white","grey","gray","orange","cyan","aqua","black","pink","violet","brown");
        static final UUID STALE=UUID.randomUUID(); // the remembered sighting the map stage keeps 100 ticks old
        // Written by both threads. A negative stage is a hand-off: the client waits for the server task it sent to set the next stage.
        volatile int stage,want,live;volatile UUID player;volatile boolean fixed,pointOwned,mapMarks;volatile long floodAt=-1,pointTicks=-1;
        volatile String fixture="",pointName="",killed="",deployWhy="",flooded="";
        // server thread
        int floodSet;long fixedAt;
        // client thread
        int oldW,oldH,oldGui,round,after,waitFrom,seen,seen2,mark=-1,routAt,respawnAt,sentBefore;boolean finished,campOk,battleOk,rowsOk,inFirst,respawnSeen,banner,floodOk,prepOk;
        String campWhy="",battleWhy="",rowsWhy="",prepWhy="",floodWhy="";List<Boolean> tallFirst=List.of(),tallLate=List.of();

        ServerPlayerEntity sp(GravesAndGlory.Battle b){return player==null?null:b.server.getPlayerManager().getPlayer(player);}
        static List<EventFeed.Line> lines(){return EventFeed.lines(net.minecraft.util.Util.getMeasuringTimeMs());}
        /** The camp sign as this client draws it: the text display whose text starts WELCOME TO BANNER CAMP (CampWorld's home sign);
         *  null while the client has none. */
        static net.minecraft.text.Text campSign(MinecraftClient c){
            if(c.world!=null)for(var e:c.world.getEntities())if(e instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity d&&d.getData()!=null&&d.getData().text().getString().startsWith("WELCOME TO BANNER CAMP"))return d.getData().text();
            return null;
        }
        static long count(String kind,int team,String text){return lines().stream().filter(l->l.kind().equals(kind)&&l.team()==team&&l.text().equals(text)).count();}
        static String feed(){return lines().stream().map(l->l.kind()+"/"+l.team()+" '"+l.text()+"'").toList().toString();}
        static boolean bannerLine(){return lines().stream().anyMatch(l->l.kind().equals("down")||l.kind().equals("up"));}
        static List<Boolean> talls(){return BattleHud.cards().stream().map(BattleHud.CardDraw::tall).toList();}
        static net.minecraft.client.util.InputUtil.Key hintsKey(){return net.minecraft.client.util.InputUtil.fromTranslationKey(GravesAndGloryClient.hints.getBoundKeyTranslationKey());}
        /** The troop a loss line names ("?" for none). */
        static String troopOf(String text){for(var t:Spec.TROOPS)if(text.equals(FeedLines.lost(1,t.name()))||text.endsWith(" "+FeedLines.plural(t.name())+" lost"))return t.id();return "?";}
        static int killTroop(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String id,int n){int k=0;for(var u:b.units)if(k<n&&u.squad==s&&u.spec.id().equals(id)&&u.mob.isAlive()){u.mob.kill();k++;}b.units.removeIf(u->!u.mob.isAlive());return k;}
        /** Frozen, and with its AI off so it keeps the place the map shows. */
        static void still(GravesAndGlory.Unit u){freeze(u);u.mob.setAiDisabled(true);}
        /** Client ticks since this stage began (the first step that called this in it). */
        int since(int tick){if(mark!=stage){mark=stage;waitFrom=tick;}return tick-waitFrom;}
        /** The menu, map and turn keys back on their defaults (R, M, [ and ]). */
        static void defaults(){for(var k:new net.minecraft.client.option.KeyBinding[]{GravesAndGloryClient.menu,GravesAndGloryClient.mapKey,GravesAndGloryClient.rotateLeft,GravesAndGloryClient.rotateRight,GravesAndGloryClient.attack})k.setBoundKey(k.getDefaultKey());net.minecraft.client.option.KeyBinding.updateKeysByCode();}
        /** A chip shows the name, or a trim of it (UiMath.trimKey): a start of the name, the dots, then an end of it. */
        static boolean trimOf(String chip,String name){int d=chip.indexOf("...");return chip.equals(name)||d>=0&&name.startsWith(chip.substring(0,d))&&name.endsWith(chip.substring(d+3));}
        /** Back to the camp (the client must see running false), then round r on its map; 20 ticks after the client shows it, stage next. */
        void restart(MinecraftClient c,int r,int next){round=r;after=next;stage=-900;server(c,b->{if(b.running)b.campaign.returnLobby();stage=900;});}

        void fixture(GravesAndGlory.Battle b,ServerPlayerEntity p,int r){
            var own=b.squads.get(player);UiHudChecks.quiet(b,own);float yaw=-90;
            if(r==1){ // own troops on a 24-block grid (a map dot apart at GUI 2), three allied knights to the south and three revenants to the east, all held in place
                int i=0;for(var u:b.units)if(u.squad==own){double x=AX+(i%5)*24,z=AZ-36+(i/5)*24;MoraleChecks.place(b,u,x,z,yaw);still(u);i++;}
                var ally=new GravesAndGlory.Squad(UUID.randomUUID(),0,new Vec3d(AX,top(b,AX,AZ+70),AZ+70));ally.order="hold";
                var foes=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(AX+120,top(b,AX+120,AZ),AZ));foes.order="hold";foes.yaw=90;
                for(int k=0;k<3;k++){double ax=AX+k*24,az=AZ+70,ex=AX+120,ez=AZ-24+k*24;
                    still(b.spawn(ally,troop("knight"),k,new Vec3d(ax,top(b,ax,az),az)));still(b.spawn(foes,troop("revenant"),k,new Vec3d(ex,top(b,ex,ez),ez)));}
                b.relocate(p,AX-10,top(b,AX-10,AZ),AZ,-90,12);
                fixture=i+" own troops on a 24-block grid from "+(int)AX+", "+(int)(AZ-36)+", three allied knights at z "+(int)(AZ+70)+" and three revenants at x "+(int)(AX+120)+", AI off";
            }else if(r==2){ // the infantry holds point 0
                var spot=b.points.get(0).spec;double px=spot.x(),pz=spot.z();pointName=spot.name();
                own.assignments.put("infantry",new GravesAndGlory.Assignment("hold","block",new Vec3d(px,top(b,px,pz),pz),yaw));
                for(var u:b.units)if(u.squad==own){if(u.spec.group().equals("infantry")){Vec3d at=b.formation(u);MoraleChecks.place(b,u,at.x,at.z,yaw);}freeze(u);}
                b.relocate(p,px-8,top(b,px-8,pz),pz,yaw,12);
                fixture="the infantry holds point 0 ("+spot.id()+", "+pointName+" at "+spot.x()+", "+spot.z()+")";
            }else if(r==3){ // the army at AX with a frozen revenant 7 blocks in front of the infantry, close enough for the morale rules
                own.assignments.put("infantry",new GravesAndGlory.Assignment("hold","block",new Vec3d(AX,top(b,AX,AZ),AZ),yaw));
                own.assignments.put("ranged",new GravesAndGlory.Assignment("hold","line",new Vec3d(AX-9,top(b,AX-9,AZ),AZ),yaw));
                own.assignments.put("support",new GravesAndGlory.Assignment("hold","block",new Vec3d(AX-16,top(b,AX-16,AZ+9),AZ+9),yaw));
                for(var u:b.units)if(u.squad==own){Vec3d at=b.formation(u);MoraleChecks.place(b,u,at.x,at.z,yaw);freeze(u);}
                var foes=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(AX+7,top(b,AX+7,AZ),AZ));foes.order="hold";foes.yaw=90;
                freeze(b.spawn(foes,troop("revenant"),0,new Vec3d(AX+7,top(b,AX+7,AZ),AZ)));
                b.relocate(p,AX-16,top(b,AX-16,AZ+9),AZ+9,-120,12);
                fixture="the army at "+(int)AX+", "+(int)AZ+" with a frozen revenant 7 blocks in front of the infantry";
            }else{ // Kingsfield: a fourth troop, five Dawn Sentinels beside the army
                var at=b.units.stream().filter(u->u.squad==own).map(u->u.mob.getPos()).findFirst().orElse(GravesAndGlory.Battle.base(0));
                for(int k=0;k<5;k++){double x=at.x+2+k*1.5,z=at.z+3;b.spawn(own,troop("sentinel"),20+k,new Vec3d(x,top(b,x,z),z));}
                for(var u:b.units)if(u.squad==own)freeze(u);
                fixture="map "+b.campaign.map+" ("+b.points.size()+" points) with five Dawn Sentinels beside the army: "+b.units.stream().filter(u->u.squad==own).count()+" own troops";
            }
            System.out.println("[p1] ui-feed round "+r+": "+fixture);
        }
        /** The map stage: each revenant a fresh sighting and one remembered sighting 100 ticks old, every tick. */
        void marks(GravesAndGlory.Battle b){
            var memory=b.sightings.computeIfAbsent(0,k->new HashMap<>());
            for(var u:b.units)if(u.squad.team==1&&u.mob.isAlive())memory.put(u.mob.getUuid(),new GravesAndGlory.Battle.Sight((float)u.mob.getX(),(float)u.mob.getZ(),b.time));
            memory.put(STALE,new GravesAndGlory.Battle.Sight((float)(AX+150),(float)AZ,b.time-100));
        }
        /** p1-ui-feed-loss-kill, one server tick: three own knights die, and the player's attack kills two Dusk Revenants. */
        void lossKill(GravesAndGlory.Battle b){
            var own=b.squads.get(player);var p=sp(b);int k=killTroop(b,own,"knight",3);
            var foes=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(AX,top(b,AX,AZ+30),AZ+30));foes.order="hold";
            List<GravesAndGlory.Unit> two=new ArrayList<>();for(int i=0;i<2;i++){double x=AX+i*3,z=AZ+30;two.add(b.spawn(foes,troop("revenant"),i,new Vec3d(x,top(b,x,z),z)));}
            int dead=0;for(var u:two){u.mob.damage(p.getDamageSources().playerAttack(p),1000f);if(!u.mob.isAlive())dead++;}
            b.units.removeIf(u->!u.mob.isAlive());
            killed="in one server tick "+k+" own Dawn Knights were killed and the player's attack killed "+dead+" of two Dusk Revenants";
        }
        /** p1-ui-feed-deploy: one knight dies, and the player trains one at the barracks. */
        void deploy(GravesAndGlory.Battle b){
            var own=b.squads.get(player);var p=sp(b);int k=killTroop(b,own,"knight",1);var f=Spec.FACTIONS[0];
            b.relocate(p,f.barracksX(),top(b,f.barracksX(),f.barracksZ())+1,f.barracksZ(),-90,8);int gold=own.gold;
            b.manage(p,own,"train:knight");
            deployWhy=k+" knight killed; at the barracks (in reach: "+b.atBarracks(p,own)+") train:knight queued "+own.training.size()+" (gold "+gold+" to "+own.gold+")";
        }
        /** p1-ui-feed-flood, one server tick: 20 own troops of four kinds die, every point changes hands, and the first of ten messages. */
        void flood(GravesAndGlory.Battle b){
            var own=b.squads.get(player);Map<String,Integer> plan=Map.of("knight",8,"archer",5,"mage",2,"sentinel",5);Map<String,Integer> dead=new TreeMap<>();
            for(var e:plan.entrySet())dead.put(e.getKey(),killTroop(b,own,e.getKey(),e.getValue()));
            for(int i=0;i<b.points.size();i++)FeedEvents.point(b,b.points.get(i),i%2);
            b.message="Flood notice 1";b.sendState();floodSet=1;floodAt=b.time;
            flooded="in one server tick own troops died "+dead+", FeedEvents.point ran for "+b.points.size()+" points (Dawn and Dusk in turn), and the message changed ten times over ten ticks (the last "+LONG.length()+" characters)";
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(want>0){var p=sp(b);if(p!=null&&skirmish(b,p,want==4?4:0,1,"normal")){live=want;want=0;fixture(b,p,live);fixedAt=b.time;fixed=true;}}
            if(live==2&&!pointOwned&&b.running&&!b.points.isEmpty()&&b.points.get(0).owner==0){pointTicks=b.time-fixedAt;pointOwned=true;}
            if(mapMarks&&b.running)marks(b);
            if(floodAt>=0&&floodSet<10&&b.running&&b.time-floodAt>=floodSet){b.message=floodSet==9?LONG:"Flood notice "+(floodSet+1);b.sendState();floodSet++;}
        }

        public void step(MinecraftClient c,int tick){
            if(tick>24000&&!finished){report("p1-ui-feed-timeout",false,"stuck in stage "+stage+" (round "+round+")");end(c);return;}
            if(stage!=0&&stage!=19&&!finished&&c.currentScreen!=null)c.setScreen(null);
            switch(stage){
                case 0->{ // p1-ui-hints-rebound: the menu key on K, the map key unbound; first back to the camp, for its sign (stage 3)
                    if(tick<20)return;c.setScreen(null);var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();
                    CommandMode.selected="all";window(c,960,540,1);stage=-3;server(c,b->{if(b.running)b.campaign.returnLobby();stage=3;});}
                case 3->{
                    var sign=campSign(c);if(sign==null&&since(tick)<=400)return; // the client gets the sign with the camp's chunks
                    // p1-ui-hints-mouse (UI polish M6): a mouse button reads LMB, RMB or MMB outside the hint layer too: the camp sign, a
                    // notice and the lessons, with Commands on the middle button and Use on the right one (its default)
                    GravesAndGloryClient.menu.setBoundKey(net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));net.minecraft.client.option.KeyBinding.updateKeysByCode();
                    String mouseCamp=GravesAndGloryClient.campHint(),mouseNotice=HintText.expand(NOTICE,GravesAndGloryClient::hint),lessonOne=GravesAndGloryClient.lessonText(0),lessonFour=GravesAndGloryClient.lessonText(3);
                    report("p1-ui-hints-mouse",mouseCamp.startsWith("MMB / Camp guide")&&mouseCamp.contains(" RMB on a station")&&mouseNotice.equals("MMB opens your war council.")&&lessonOne.startsWith("Press MMB, ")&&lessonFour.startsWith("Press RMB at "),"campHint() '"+mouseCamp+"'; the notice '"+NOTICE+"' reads '"+mouseNotice+"'; lesson 1 '"+lessonOne+"'; lesson 4 '"+lessonFour+"'");
                    // p1-ui-keybind-chips (UI polish round 2): the keybind texts the server sends (Campaign.keyText) name a key as the feed
                    // does, a mouse button as LMB, RMB or MMB (KeybindTextMixin): the camp sign as this client draws it, a notice as the
                    // action bar shows it, and the use key's own keybind text against the feed's name for it
                    String signText=sign==null?"none":sign.getString(),signWant="WELCOME TO BANNER CAMP / "+GravesAndGloryClient.hint("menu")+" opens your camp guide",feedUse=GravesAndGloryClient.hint("use");
                    boolean keyed=sign!=null&&sign.getSiblings().stream().anyMatch(t->t.getContent() instanceof net.minecraft.text.KeybindTextContent kb&&kb.getKey().equals("key.gravesandglory.menu"));
                    String bar=Campaign.keyText(NOTICE).getString(),use=net.minecraft.text.Text.keybind(HintText.bindingKey("use")).getString();
                    report("p1-ui-keybind-chips",keyed&&signText.equals(signWant)&&signWant.contains(" MMB ")&&bar.equals(mouseNotice)&&use.equals(feedUse)&&use.equals("RMB"),"with Commands on the middle button the camp sign"+(keyed?", a keybind text,":" (no keybind text in it)")+" reads '"+signText+"' (the feed's names make '"+signWant+"'); the notice '"+NOTICE+"' reads '"+bar+"' on the action bar and '"+mouseNotice+"' in the feed; the use key's keybind text reads '"+use+"', the feed's name for it '"+feedUse+"'");
                    GravesAndGloryClient.menu.setBoundKey(net.minecraft.client.util.InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_K));
                    GravesAndGloryClient.mapKey.setBoundKey(net.minecraft.client.util.InputUtil.UNKNOWN_KEY);net.minecraft.client.option.KeyBinding.updateKeysByCode();
                    String camp=GravesAndGloryClient.campHint();campOk=camp.contains("K");campWhy="campHint() '"+camp+"'";
                    CampaignClient.notice="";UUID id=c.player.getUuid();stage=-1;server(c,b->{player=id;b.campaign.notice(id,Campaign.WELCOME_WAIT);stage=1;});
                }
                case 1,2->{ // the camp path, for each welcome string: the template the client keeps, what CampScreen draws, the action bar's text
                    String sent=stage==1?Campaign.WELCOME_WAIT:Campaign.WELCOME;boolean got=sent.equals(CampaignClient.notice);
                    if(got){String drawn=HintText.expand(CampaignClient.notice,GravesAndGloryClient::hint),bar=Campaign.keyText(CampaignClient.notice).getString();
                        campOk&=drawn.contains("K opens")&&bar.contains("K opens");campWhy+="; notice '"+CampaignClient.notice+"': CampScreen draws '"+drawn+"', the action bar reads '"+bar+"'";}
                    else if(since(tick)>200){campOk=false;campWhy+="; the client's notice was '"+CampaignClient.notice+"' 200 ticks after the server sent '"+sent+"'";}
                    if(got||since(tick)>200){
                        if(stage==1){CampaignClient.notice="";stage=-2;server(c,b->{b.campaign.notice(player,Campaign.WELCOME);stage=2;});}
                        else restart(c,1,10);
                    }
                }
                case 900->{
                    if(!GravesAndGloryClient.running){fixed=false;seen=0;want=round;stage=901;}
                    else if(since(tick)>200){report("p1-ui-feed-fixture",false,"the client still showed a round 200 ticks after the return to the camp");end(c);}
                }
                case 901->{
                    if(seen==0&&fixed&&GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic())seen=tick;
                    if(seen>0&&tick>=seen+20){seen=0;stage=after;}
                    else if(seen==0&&since(tick)>3000){report("p1-ui-feed-fixture",false,"round "+round+" was not on screen 3000 ticks after it was asked for");end(c);}
                }
                // Round A: map 0
                case 10->{stage=-10;server(c,b->{b.message=NOTICE;stage=11;});}
                case 11->{ // the battle path: the STATE message as the newest feed line, with the player's key
                    var ls=lines();String newest=ls.isEmpty()?"":ls.get(ls.size()-1).text();int k=since(tick);
                    if(newest.equals("K opens your war council.")||k>20){battleOk=newest.equals("K opens your war council.");battleWhy="the server set the message '"+NOTICE+"'; "+k+" client ticks later the newest feed line was '"+newest+"'";seen=tick;stage=12;}
                }
                case 12->{ // the hint layer in the round's first 60 s, then the screenshot, then an order aimed
                    if(tick>=seen+2){var rows=List.copyOf(BattleHud.hintRows());long age=net.minecraft.util.Util.getMeasuringTimeMs()-BattleHud.roundShownMs;
                        rowsOk=rows.equals(HINT_ROWS)&&age<BattleHud.HINT_LAYER_MS;rowsWhy="hint rows "+rows+" "+age/1000+" s into the round";
                        // p1-ui-feed-round-notice (UI polish M7): the round's opening notice, under the newest line, is drawn whole
                        List<String> feedRows=new ArrayList<>();for(int r=0;r<EventFeed.MAX_LINES;r++){String t=BattleHud.texts().get("feed-"+r);if(t!=null)feedRows.add(t);}
                        String opening=GravesAndGlory.Battle.ROUND_START;
                        report("p1-ui-feed-round-notice",feedRows.contains(opening)&&c.textRenderer.getWidth(opening)<=140,"the feed drew "+feedRows+"; the opening notice '"+opening+"' is "+c.textRenderer.getWidth(opening)+" px of the 140 a line holds");
                        shot(c,"ui-hud-hints");CommandMode.begin("move");seen=tick;stage=13;}
                }
                case 13->{
                    if(tick>=seen+2){var rows=BattleHud.hintRows();List<String> first=rows.isEmpty()?List.of():List.copyOf(rows.get(0));boolean aimed=CommandMode.active();
                        boolean aimOk=aimed&&first.equals(AIM_ROW);CommandMode.cancel();defaults();
                        report("p1-ui-hints-rebound",campOk&&battleOk&&rowsOk&&aimOk,campWhy+"; "+battleWhy+"; "+rowsWhy+"; aiming (active "+aimed+") row 1 "+first+"; then the menu and map keys went back to "+GravesAndGloryClient.hint("menu")+" and "+GravesAndGloryClient.hint("map"));
                        seen=tick;stage=23;}
                }
                case 23->{ // p1-ui-hints-long (UI polish M4): two long names on each row of the hint slip, which must still end on its paper.
                    // The turn keys go on Page Up and Page Down, two long names with one start, as the review's Keypad 4 and 6 would be; in
                    // game those read 4 and 6 (GLFW names a printable key by its character) and fit (run ui-polish1-201624). The row cannot
                    // keep every last word whole (UiMathCheck: P...Up, Pag..., L...Alt), so the two turn chips need only differ. Attack goes on
                    // Caps Lock (UI polish round 2), too long for the mock's places on the tall cards: each card's key row must still keep
                    // its Attack pair clear of the Follow chip and end by the card's x + 166
                    var keysym=net.minecraft.client.util.InputUtil.Type.KEYSYM;
                    GravesAndGloryClient.menu.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_RIGHT_SHIFT));GravesAndGloryClient.mapKey.setBoundKey(net.minecraft.client.util.InputUtil.UNKNOWN_KEY);
                    GravesAndGloryClient.rotateLeft.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_PAGE_UP));GravesAndGloryClient.rotateRight.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_PAGE_DOWN));
                    GravesAndGloryClient.attack.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_CAPS_LOCK));
                    net.minecraft.client.option.KeyBinding.updateKeysByCode();seen=tick;stage=24;}
                case 24->{
                    if(tick>=seen+2){var rows=List.copyOf(BattleHud.hintRows());var ends=List.copyOf(BattleHud.hintEnds());int[] slip=BattleHud.rects().get("hints");
                        boolean shape=rows.size()==2&&ends.size()==2&&slip!=null&&rows.get(0).size()==4&&rows.get(1).size()==5;
                        boolean on=shape&&ends.get(0)<=slip[0]+164&&ends.get(1)<=slip[0]+164; // the slip PNG's paper ends at x 172: 8 px kept clear, as on the left
                        String shift=GravesAndGloryClient.menu.getBoundKeyLocalizedText().getString(),up=GravesAndGloryClient.rotateLeft.getBoundKeyLocalizedText().getString(),down=GravesAndGloryClient.rotateRight.getBoundKeyLocalizedText().getString();
                        boolean named=shape&&shift.equals("Right Shift")&&up.equals("Page Up")&&down.equals("Page Down")&&trimOf(rows.get(0).get(0),"Not bound")&&rows.get(0).get(1).equals("Map")&&trimOf(rows.get(0).get(2),shift)&&rows.get(0).get(3).equals("Commands")
                            &&trimOf(rows.get(1).get(0),up)&&trimOf(rows.get(1).get(1),down)&&rows.get(1).get(2).equals("Turn")&&trimOf(rows.get(1).get(3),"Left Alt")&&rows.get(1).get(4).equals("Hints");
                        boolean apart=shape&&!rows.get(1).get(0).equals(rows.get(1).get(1))&&rows.get(0).get(2).endsWith("Shift")&&String.join("",rows.get(0)).contains("...")&&String.join("",rows.get(1)).contains("...");
                        String caps=GravesAndGloryClient.attack.getBoundKeyLocalizedText().getString();var keyRows=List.copyOf(BattleHud.keyRows());List<String> cardsWrong=new ArrayList<>();
                        for(var kr:keyRows){int[] card=BattleHud.rects().get("card-"+kr.group());var p=kr.parts();
                            boolean ok=card!=null&&p.size()==6&&trimOf(p.get(0),caps)&&p.get(1).equals("Attack")&&p.get(2).equals(GravesAndGloryClient.hint("follow"))&&p.get(3).equals("Follow")&&p.get(4).equals(GravesAndGloryClient.hint("formation"))&&p.get(5).equals("Form")&&kr.from().get(0)>=card[0]+10&&kr.to().get(5)<=card[0]+166;
                            for(int i=0;ok&&i<5;i++)ok=kr.to().get(i)<kr.from().get(i+1);if(!ok)cardsWrong.add(kr.group());}
                        boolean cardsOk=caps.equals("Caps Lock")&&keyRows.size()==3&&cardsWrong.isEmpty();
                        shot(c,"ui-hud-hints-long");defaults();
                        report("p1-ui-hints-long",on&&named&&apart&&cardsOk,"with Map unbound, Commands on "+shift+" and Turn on "+up+" and "+down+", the slip at "+UiHudChecks.box(slip)+" drew the rows "+rows+", ending at canvas x "+ends+" (the paper's text edge at "+(slip==null?"?":String.valueOf(slip[0]+164))+"); with Attack on "+caps+" the tall cards drew the key rows "+keyRows.stream().map(kr->kr.group()+" "+kr.parts()+" from x "+kr.from()+" to "+kr.to()+" (the card at "+UiHudChecks.box(BattleHud.rects().get("card-"+kr.group()))+")").toList()+(cardsWrong.isEmpty()?"":", WRONG on "+cardsWrong)+"; then the keys went back to "+GravesAndGloryClient.hint("menu")+", "+GravesAndGloryClient.hint("map")+", "+GravesAndGloryClient.hint("rotate_left")+", "+GravesAndGloryClient.hint("rotate_right")+" and "+GravesAndGloryClient.hint("attack"));
                        seen=tick;stage=14;}
                }
                case 14->{ // p1-ui-alt-layer: in the first 60 s every card is tall
                    if(tick>=seen+2&&!CommandMode.active()){tallFirst=talls();inFirst=net.minecraft.util.Util.getMeasuringTimeMs()-BattleHud.roundShownMs<BattleHud.HINT_LAYER_MS;
                        CommandMode.selected="ranged";BattleHud.roundShownMs-=61000;seen=tick;stage=15;}
                }
                case 15->{if(tick>=seen+2){tallLate=talls();net.minecraft.client.option.KeyBinding.setKeyPressed(hintsKey(),true);seen=tick;stage=16;}}
                case 16->{
                    net.minecraft.client.option.KeyBinding.setKeyPressed(hintsKey(),true);
                    if(tick>=seen+2){var held=talls();boolean pressed=GravesAndGloryClient.hints.isPressed();net.minecraft.client.option.KeyBinding.setKeyPressed(hintsKey(),false);CommandMode.selected="all";
                        boolean ok=inFirst&&tallFirst.size()==3&&!tallFirst.contains(false)&&tallLate.equals(List.of(false,true,false))&&held.size()==3&&!held.contains(false);
                        report("p1-ui-alt-layer",ok,"in the round's first 60 s (true: "+inFirst+") with the army selected the cards drew tall "+tallFirst+"; with roundShownMs moved 61 s back and ranged selected "+tallLate+"; with the hints key ("+GravesAndGloryClient.hint("hints")+") held (pressed "+pressed+") "+held);
                        stage=17;}
                }
                case 17->{window(c,960,540,2);mapMarks=true;seen=tick;stage=18;}
                case 18->{if(tick>=seen+20){c.setScreen(new GravesAndGloryClient.MapScreen(null));seen=tick;stage=19;}}
                case 19->{ // p1-ui-map-glyphs
                    if(tick>=seen+5){
                        String legend=GravesAndGloryClient.MapScreen.legend(),lower=" "+legend.toLowerCase(Locale.ROOT)+" ";
                        List<String> colours=COLOURS.stream().filter(word->java.util.regex.Pattern.compile("[^a-z]"+word+"[^a-z]").matcher(lower).find()).toList();
                        var ms=GravesAndGloryClient.markers;long own=ms.stream().filter(GravesAndGloryClient.Marker::own).count(),ally=ms.size()-own;
                        var ss=GravesAndGloryClient.sightings;long fresh=ss.stream().filter(v->v.age()<20).count(),old=ss.size()-fresh;
                        boolean open=c.currentScreen instanceof GravesAndGloryClient.MapScreen;
                        shot(c,"ui-map");
                        report("p1-ui-map-glyphs",open&&legend.equals("LMB a group to select / RMB to move")&&colours.isEmpty()&&own>0&&ally>0&&fresh>0&&old>0,"legend '"+legend+"' (colour words in it: "+colours+"); the map (open: "+open+", GUI 2) marks "+own+" own troops (dots), "+ally+" allied (rings), "+fresh+" fresh sightings (x) and "+old+" remembered (small x); "+fixture);
                        c.setScreen(null);mapMarks=false;window(c,960,540,1);restart(c,2,30);
                    }
                }
                // Round B: map 0
                case 30->{ // p1-ui-feed-point: the infantry holds point 0
                    if(pointOwned){seen=tick;stage=31;}
                    else if(since(tick)>1300){report("p1-ui-feed-point",false,"point 0 ("+pointName+") had no Dawn owner 1300 client ticks after the infantry was set to hold it; feed "+feed());stage=32;}
                }
                case 31->{
                    String line="Dawn took "+pointName;long n=count("point",0,line);
                    if(n==1||tick>seen+20){report("p1-ui-feed-point",n==1&&pointTicks<=1200,fixture+"; Dawn owned it "+pointTicks+" server ticks later; "+(tick-seen)+" client ticks after that the feed held "+n+" line(s) ('point', 0, '"+line+"'): "+feed());stage=32;}
                }
                case 32->{EventFeed.clear();stage=-32;server(c,b->{lossKill(b);stage=33;});}
                case 33->{ // p1-ui-feed-loss-kill
                    if(since(tick)>=60){long lost=count("loss",0,"3 Dawn Knights lost"),slain=count("kill",0,"2 Dusk Revenants lost");
                        report("p1-ui-feed-loss-kill",lost==1&&slain==1,killed+"; 60 client ticks later the feed held "+lost+" '3 Dawn Knights lost' (loss, 0) and "+slain+" '2 Dusk Revenants lost' (kill, 0): "+feed());restart(c,3,50);}
                }
                // Round C: map 0
                case 50->{EventFeed.clear();stage=-50;server(c,b->{killTroop(b,b.squads.get(player),"knight",10);stage=51;});}
                case 51->{ // p1-ui-feed-rout: the morale rules rout the infantry
                    int k=since(tick);
                    if(count("rout",0,"Infantry routing")==1){routAt=k;stage=-52;server(c,b->{Morale.rally(b,b.squads.get(player),"infantry");stage=52;});}
                    else if(k>100){report("p1-ui-feed-rout",false,"no 'Infantry routing' line 100 client ticks after 10 of the 12 knights died: "+feed());stage=54;}
                }
                case 52->{
                    int k=since(tick);
                    if(count("regroup",0,"Infantry regrouped")==1){stage=-53;server(c,b->{Morale.rally(b,b.squads.get(player),"ranged");stage=53;});}
                    else if(k>40){report("p1-ui-feed-rout",false,"'Infantry routing' "+routAt+" client ticks after the deaths, but no 'Infantry regrouped' 40 ticks after Morale.rally: "+feed());stage=54;}
                }
                case 53->{
                    if(since(tick)>=30){long regroups=lines().stream().filter(l->l.kind().equals("regroup")).count();
                        report("p1-ui-feed-rout",regroups==1&&count("regroup",0,"Ranged regrouped")==0,"'Infantry routing' "+routAt+" client ticks after 10 of the 12 knights died, then 'Infantry regrouped' after Morale.rally; 30 ticks after Morale.rally on ranged (not routing) the feed holds "+regroups+" regroup line(s): "+feed());stage=54;}
                }
                case 54->{EventFeed.clear();stage=-54;server(c,b->{deploy(b);stage=55;});}
                case 55->{ // p1-ui-feed-deploy
                    int k=since(tick);long n=count("deploy",0,"Dawn Knight deploying");
                    if(n==1||k>400){report("p1-ui-feed-deploy",n==1,deployWhy+"; "+k+" client ticks later the feed held "+n+" 'Dawn Knight deploying' (deploy, 0): "+feed());stage=56;}
                }
                case 56->{banner=false;respawnSeen=false;seen2=0;sentBefore=FeedEvents.sent;stage=-56;server(c,b->{var p=sp(b);p.damage(p.getDamageSources().magic(),1000f);stage=57;});}
                case 57->{ // p1-ui-feed-commander-solo
                    int k=since(tick);banner|=bannerLine();
                    if(BattleHud.rects().containsKey("respawn")){respawnSeen=true;respawnAt=k;stage=58;}
                    else if(k>60){report("p1-ui-feed-commander-solo",false,"no respawn card 60 client ticks after the player was killed: "+feed());restart(c,4,70);}
                }
                case 58->{
                    int k=since(tick);banner|=bannerLine();
                    if(seen2==0&&!BattleHud.rects().containsKey("respawn")&&GravesAndGloryClient.respawn==0)seen2=tick;
                    if(seen2>0&&tick>=seen2+40){int sent=FeedEvents.sent-sentBefore;
                        report("p1-ui-feed-commander-solo",respawnSeen&&!banner,"the respawn card showed "+respawnAt+" client ticks after the player was killed and left "+(seen2-waitFrom)+" ticks after that; "+(banner?"a down or up line arrived":"no down or up line arrived")+" up to 40 ticks after the respawn ("+sent+" feed lines sent by the server meanwhile): "+feed());
                        restart(c,4,70);}
                    else if(seen2==0&&k>600){report("p1-ui-feed-commander-solo",false,"the respawn had not ended 600 client ticks after the card showed: "+feed());restart(c,4,70);}
                }
                // Round D: Kingsfield
                case 70->{ // p1-ui-feed-flood
                    var prep=lines().stream().map(EventFeed.Line::text).filter(t->t.startsWith("Preparing")).toList();prepOk=prep.isEmpty();prepWhy=prep.isEmpty()?"no Preparing line of the build reached the feed":"build lines in the feed: "+prep;
                    stage=-70;server(c,b->{flood(b);stage=71;});
                }
                case 71->{
                    if(since(tick)>=60){
                        var ls=lines();var font=c.textRenderer;List<String> wide=new ArrayList<>(),rows=new ArrayList<>(),troops=new ArrayList<>();
                        for(int r=0;r<EventFeed.MAX_LINES;r++){String t=BattleHud.texts().get("feed-"+r);if(t==null)continue;rows.add(t+" ("+font.getWidth(t)+" px)");if(font.getWidth(t)>140)wide.add(t);}
                        for(var l:ls)if(l.kind().equals("loss"))troops.add(troopOf(l.text()));
                        boolean unique=new HashSet<>(troops).size()==troops.size()&&!troops.contains("?");
                        // the long notice cut after a whole word (UI polish M7), not inside one
                        String cut=null;for(int r=0;r<EventFeed.MAX_LINES;r++){String t=BattleHud.texts().get("feed-"+r);if(t!=null&&t.startsWith("The long"))cut=t;}
                        String kept=cut!=null&&cut.endsWith("...")?cut.substring(0,cut.length()-3):null;boolean atWord=kept!=null&&kept.length()<LONG.length()&&LONG.startsWith(kept)&&!Character.isLetterOrDigit(LONG.charAt(kept.length()));
                        floodOk=ls.size()<=5&&rows.size()==ls.size()&&wide.isEmpty()&&unique&&prepOk&&atWord;
                        floodWhy=fixture+"; "+flooded+"; 60 client ticks later "+ls.size()+" lines live: "+feed()+"; drawn "+rows+(wide.isEmpty()?", none over 140 px":", over 140 px: "+wide)+"; the long notice drawn as '"+cut+"' (cut after a whole word: "+atWord+"); loss lines by troop "+troops+"; "+prepWhy;
                        shot(c,"ui-feed-flood");stage=-72;server(c,b->{b.campaign.returnLobby();stage=72;});
                    }
                }
                case 72->{
                    int k=since(tick);boolean empty=lines().isEmpty();
                    if(empty||k>20){report("p1-ui-feed-flood",floodOk&&empty,floodWhy+"; after returnLobby the feed was "+(empty?"empty "+k+" client ticks later":"still "+feed()+" 20 ticks later"));end(c);}
                }
                case 99->{stage=100;finish(c,"ui-feed");}
                default->{}
            }
        }
        /** Puts the keys, the selection, the hints key and the window back; p1-ui-feed-suite follows once the window has settled. */
        void end(MinecraftClient c){
            finished=true;mapMarks=false;want=0;CommandMode.cancel();CommandMode.selected="all";defaults();
            if(GravesAndGloryClient.hints!=null)net.minecraft.client.option.KeyBinding.setKeyPressed(hintsKey(),false);
            if(c.currentScreen instanceof GravesAndGloryClient.MapScreen)c.setScreen(null);
            if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;
        }
    }

    /** UI Task 5: the War Table home (UI spec 4.2), CampScreen.open("home") at 960x540 GUI 1 (the canvas is the 960x540 page, origin
     *  0) unless a check says otherwise. The checks read the widgets' canvas boxes and what WarTableHome reports the last frame
     *  drew; the pointer and the keys go to the screen as the game sends them (mouseMoved, mouseClicked, keyPressed). */
    static final class UiHomeChecks implements Suite,ServerWatch {
        static final String[] LAYOUT_NAMES={"camp","recruit","banner","contracts","chapter","friends","practice","skirmish","campaign","change","ride"};
        static final int[][] LAYOUT={{56,12,96,28},{184,12,112,28},{304,12,112,28},{424,12,112,28},{544,12,112,28},{664,12,112,28},{228,200,152,136},{392,188,176,160},{580,200,152,136},{796,340,80,24},{360,388,240,40}};
        static final int[] PRACTICE_LO={228,200,152,136},PRACTICE_HI={216,188,176,160},SKIRMISH_LO={404,200,152,136};
        // the server thread writes these, the client thread reads them
        volatile int stage;volatile UUID player;volatile boolean serverRunning,serverOnPad;volatile int padCountdown=-1;
        // client thread only
        int mark=-1,waitFrom,oldW,oldH,oldGui,runAt=-1,padAt=-1;boolean finished,focusOk=true,modesOk=true,scalesOk=true;
        String focusWhy="",modesWhy="",gatesWhy="",scalesWhy="";

        public void serverTick(GravesAndGlory.Battle b){serverRunning=b.running;var id=player;serverOnPad=id!=null&&b.campaign.onPad.containsKey(id);padCountdown=b.campaign.padCountdown;}
        /** Steps since the suite entered its current stage. */
        int since(int tick){if(mark!=stage){mark=stage;waitFrom=tick;}return tick-waitFrom;}
        static WarTableHome home(MinecraftClient c){return c.currentScreen instanceof CampScreen s&&s.page.equals("home")?s.home:null;}
        static int[] box(MinecraftClient c,String name){var h=home(c);var b=h==null?null:h.button(name);return b==null?null:b.canvasBox();}
        static double gui(MinecraftClient c,double canvas){return canvas*UiCanvas.k()/c.getWindow().getScaleFactor();}
        /** The pointer moves to the middle of a canvas box, as GLFW's cursor callback reports it. */
        static void move(MinecraftClient c,int[] b){if(b!=null&&c.currentScreen!=null)c.currentScreen.mouseMoved(gui(c,b[0]+b[2]/2.0),gui(c,b[1]+b[3]/2.0));}
        /** A left click in the middle of a canvas box, the pointer moving there first. */
        static void click(MinecraftClient c,int[] b){if(b==null||c.currentScreen==null)return;double x=gui(c,b[0]+b[2]/2.0),y=gui(c,b[1]+b[3]/2.0);var s=c.currentScreen;s.mouseMoved(x,y);s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
        static void key(MinecraftClient c,int key){if(c.currentScreen!=null)c.currentScreen.keyPressed(key,0,0);}
        static String str(int[] b){return b==null?"none":Arrays.toString(b);}
        static boolean overlap(int[] a,int[] b){return a[0]<b[0]+b[2]&&b[0]<a[0]+a[2]&&a[1]<b[1]+b[3]&&b[1]<a[1]+a[3];}
        static boolean inside(int[] a,int w,int h){return a[0]>=0&&a[1]>=0&&a[0]+a[2]<=w&&a[1]+a[3]<=h;}
        /** The widgets' canvas boxes by name, in Tab order. */
        static Map<String,int[]> boxes(MinecraftClient c){Map<String,int[]> m=new LinkedHashMap<>();var h=home(c);if(h!=null)for(var e:h.buttons.entrySet())m.put(e.getKey(),e.getValue().canvasBox());return m;}
        /** The Ride plate's label as the last frame drew it. */
        static String label(){return WarTableHome.texts().getOrDefault("ride","none");}
        /** The solid pixels (alpha 200 or more: the props themselves, not their light or shadows) of frame f of a sheet, as {x,y}
         *  inside the frame, read from the PNG the game loaded; null when it cannot be read. */
        static List<int[]> solid(MinecraftClient c,String name,int f){
            var e=UiTex.entry(name);int cw=e.w()/Math.max(1,e.frames());List<int[]> out=new ArrayList<>();
            try(var in=c.getResourceManager().getResource(e.id()).orElseThrow().getInputStream();var img=NativeImage.read(in)){
                for(int y=0;y<img.getHeight();y++)for(int x=0;x<cw;x++)if(((img.getColor(f*cw+x,y)>>>24)&0xff)>=200)out.add(new int[]{x,y});
            }catch(Exception ex){return null;}
            return out;
        }

        public void step(MinecraftClient c,int tick){
            if(tick>8000&&!finished){report("p1-ui-home-timeout",false,"stuck in stage "+stage);end(c);return;}
            switch(stage){
                case 0->{ // at camp with no round running, the window at 960x540 GUI 1, the session's sheet back on Skirmish
                    if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
                    c.setScreen(null);CampaignClient.homeMode="skirmish";window(c,960,540,1);stage=-1;server(c,b->{if(b.running)b.campaign.returnLobby();stage=1;});
                }
                case 1->{
                    if(GravesAndGloryClient.running){if(since(tick)>300){report("p1-ui-home-layout",false,"the client still showed a round 300 ticks after the return to camp");end(c);}return;}
                    CampScreen.open("home");stage=2;
                }
                case 2->{ // p1-ui-home-layout, once the opening glint has passed
                    if(since(tick)<20)return;
                    var boxes=boxes(c);StringBuilder bad=new StringBuilder();
                    for(int i=0;i<LAYOUT.length;i++){int[] b=boxes.get(LAYOUT_NAMES[i]);if(b==null||!Arrays.equals(b,LAYOUT[i]))bad.append(bad.length()==0?"":", ").append(LAYOUT_NAMES[i]).append(" ").append(str(b)).append(" not ").append(str(LAYOUT[i]));}
                    int[] o=WarTable.origin();String ride=label();var t=WarTableHome.texts();
                    report("p1-ui-home-layout",home(c)!=null&&bad.length()==0&&ride.equals("Ride to the war gates")&&o[0]==0&&o[1]==0&&CampaignClient.homeMode.equals("skirmish"),
                        "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", origin "+o[0]+","+o[1]+"; "+(bad.length()==0?"the 11 widget boxes as pinned":"wrong: "+bad)+"; the plate reads \""+ride+"\"; homeMode "+CampaignClient.homeMode+"; field card \""+t.get("field-name")+"\" / \""+t.get("field-rule")+"\" / \""+t.get("field-teams")+"\"");
                    shot(c,"ui-home");stage=3;
                }
                case 3->{ // p1-ui-home-party: solo, the local player's seat with the live model, then the Invite seat
                    var r=WarTableHome.rects();var pt=WarTableHome.portraits();String name=c.player.getGameProfile().getName();int[] invite=box(c,"invite");
                    boolean seat=Arrays.equals(r.get("seat-0"),new int[]{56,206,48,48})&&r.containsKey("ready-0")&&!r.containsKey("seat-1")&&name.equals(WarTableHome.texts().get("seat-0"));
                    boolean model=pt.size()==1&&pt.get(0).model()&&pt.get(0).name().equals(name)&&pt.get(0).size()==32&&pt.get(0).feet()==282&&pt.get(0).x()==80;
                    report("p1-ui-home-party",seat&&model&&invite!=null&&invite[1]==262&&Arrays.equals(r.get("invite"),invite),"seat-0 "+str(r.get("seat-0"))+" named \""+WarTableHome.texts().get("seat-0")+"\" (player "+name+"), ready check "+str(r.get("ready-0"))+", seat-1 "+str(r.get("seat-1"))+"; Invite seat "+str(invite)+" (drawn "+str(r.get("invite"))+"); portraits "+pt);
                    stage=4;
                }
                case 4->{ // p1-ui-home-focus: the pointer over Practice raises it while Skirmish settles; Right and Left move the raised sheet
                    int k=since(tick);var h=home(c);if(h==null){report("p1-ui-home-focus",false,focusWhy+"; the home closed");stage=5;return;}
                    if(k==0){move(c,PRACTICE_LO);focusOk&=h.focused==0&&CampaignClient.homeMode.equals("practice");focusWhy="the pointer over Practice: focused "+h.focused+" ("+CampaignClient.homeMode+")";}
                    else if(k==1){var r=WarTableHome.rects();shot(c,"ui-home-focus-mid");focusWhy+="; a tick later (focus-mid): practice drawn "+str(r.get("practice"))+", skirmish "+str(r.get("skirmish"))+", seal "+str(r.get("seal"));}
                    else if(k==11){var r=WarTableHome.rects();shot(c,"ui-home-focus");
                        boolean raised=Arrays.equals(r.get("practice"),PRACTICE_HI)&&Arrays.equals(r.get("skirmish"),SKIRMISH_LO)&&Arrays.equals(r.get("seal"),new int[]{216+71,188-17,37,38})&&r.containsKey("ribbon");
                        focusOk&=raised&&Arrays.equals(box(c,"practice"),PRACTICE_HI)&&Arrays.equals(box(c,"skirmish"),SKIRMISH_LO);
                        focusWhy+="; 10 ticks later (focus): practice "+str(r.get("practice"))+" (widget "+str(box(c,"practice"))+"), skirmish "+str(r.get("skirmish"))+" (widget "+str(box(c,"skirmish"))+"), seal "+str(r.get("seal"))+", ribbon "+str(r.get("ribbon"));}
                    else if(k==12){key(c,GLFW.GLFW_KEY_RIGHT);int a=h.focused;key(c,GLFW.GLFW_KEY_RIGHT);int b=h.focused;key(c,GLFW.GLFW_KEY_RIGHT);focusOk&=a==1&&b==2&&h.focused==2;focusWhy+="; Right, Right, Right: focused "+a+", "+b+", "+h.focused;}
                    else if(k==14){key(c,GLFW.GLFW_KEY_LEFT);key(c,GLFW.GLFW_KEY_LEFT);key(c,GLFW.GLFW_KEY_LEFT);focusOk&=h.focused==0&&CampaignClient.homeMode.equals("practice");focusWhy+="; Left three times: "+h.focused+" ("+CampaignClient.homeMode+")";}
                    else if(k==40){var dir=c.runDirectory.toPath().resolve("screenshots");boolean mid=java.nio.file.Files.exists(dir.resolve("p1-ui-home-focus-mid.png")),done=java.nio.file.Files.exists(dir.resolve("p1-ui-home-focus.png"));
                        report("p1-ui-home-focus",focusOk&&mid&&done,focusWhy+"; p1-ui-home-focus-mid.png "+(mid?"saved":"missing")+", p1-ui-home-focus.png "+(done?"saved":"missing"));stage=5;}
                }
                case 5->{ // p1-ui-home-modes: from Campaign, a click on Practice; then Enter starts the practice round
                    int k=since(tick);if(home(c)==null){report("p1-ui-home-modes",false,"the home was not open");stage=9;return;}
                    if(k==0){key(c,GLFW.GLFW_KEY_RIGHT);key(c,GLFW.GLFW_KEY_RIGHT);modesWhy="Right twice: focused "+home(c).focused;click(c,box(c,"practice"));}
                    else if(k==3){String l=label();modesOk&=Arrays.equals(box(c,"practice"),PRACTICE_HI)&&Arrays.equals(box(c,"skirmish"),SKIRMISH_LO)&&l.equals("Begin practice")&&CampaignClient.homeMode.equals("practice");
                        modesWhy+="; a click on Practice: practice "+str(box(c,"practice"))+", skirmish "+str(box(c,"skirmish"))+", the plate \""+l+"\", homeMode "+CampaignClient.homeMode;
                        key(c,GLFW.GLFW_KEY_ENTER);runAt=tick;stage=6;}
                }
                case 6->{
                    if(serverRunning){modesWhy+="; Enter: the round ran "+(tick-runAt)+" ticks later";stage=-7;server(c,b->{b.campaign.returnLobby();stage=7;});}
                    else if(tick-runAt>100){modesOk=false;modesWhy+="; Enter: no round 100 ticks later (notice \""+CampaignClient.notice+"\")";stage=-7;server(c,b->{if(b.running)b.campaign.returnLobby();stage=7;});}
                }
                case 7->{ // back at camp after returnLobby, the home open again for 20 ticks
                    int k=since(tick);
                    if(GravesAndGloryClient.running||serverRunning){if(k>400){report("p1-ui-home-modes",false,modesWhy+"; still in a round 400 ticks after returnLobby");stage=9;}return;}
                    if(home(c)!=null){if(k>=20)stage=8;return;}
                    if(k>=60){CampScreen.open("home");mark=-1;}
                }
                case 8->{ // the Campaign sheet rides to the first Long March battle
                    int k=since(tick);if(home(c)==null){report("p1-ui-home-modes",false,modesWhy+"; the home closed before the Campaign click");stage=9;return;}
                    if(k==0)click(c,box(c,"campaign"));
                    else if(k==6){String l=label();int[] r=WarTableHome.rects().get("ride-label"),ride=box(c,"ride");int start=r==null||ride==null?-1:r[0]-ride[0],stop=r==null||ride==null?-1:r[0]+r[2]-ride[0];
                        int[] sheet=WarTableHome.rects().get("campaign"),card=WarTableHome.rects().get("field");int gap=sheet==null||card==null?-99:card[0]-(sheet[0]+sheet[2]); // the raised sheet stays off the field card
                        modesOk&=CampaignClient.profile.campaignCleared==0&&home(c).focused==2&&l.equals("Ride to The Training Field")&&start>=57&&stop<=232&&gap>=8;
                        modesWhy+="; campaignCleared "+CampaignClient.profile.campaignCleared+", a click on Campaign: focused "+home(c).focused+", the plate \""+l+"\" from x "+start+" to "+stop+" of the plate; the raised Campaign sheet "+str(sheet)+" ends "+gap+" px left of the field card "+str(card);
                        report("p1-ui-home-modes",modesOk,modesWhy);stage=9;}
                }
                case 9->{ // p1-ui-home-gates: Skirmish, then Ride, sets the commander on the ready pad
                    int k=since(tick);
                    if(k==0){if(home(c)==null)CampScreen.open("home");return;}
                    if(k==5){click(c,box(c,"skirmish"));return;}
                    if(k==7){gatesWhy="a click on Skirmish, the plate \""+label()+"\"";click(c,box(c,"ride"));padAt=tick;return;}
                    if(k<7)return;
                    if(serverOnPad){gatesWhy+="; Ride: on the pad "+(tick-padAt)+" ticks later (countdown "+padCountdown+"), the table "+(c.currentScreen==null?"closed":"still open: "+c.currentScreen.getClass().getSimpleName());
                        stage=-10;server(c,b->{var p=b.server.getPlayerManager().getPlayer(player);if(p!=null)b.campaign.toLobby(p);stage=10;});}
                    else if(tick-padAt>40){report("p1-ui-home-gates",false,gatesWhy+"; Ride: not on the pad 40 ticks later (notice \""+CampaignClient.notice+"\")");stage=11;}
                }
                case 10->{ // stepping off the pad cancels it
                    int k=since(tick);
                    if(!serverOnPad&&padCountdown<0){report("p1-ui-home-gates",true,gatesWhy+"; moved off: off the pad with the countdown cancelled "+k+" ticks later");stage=11;}
                    else if(k>60){report("p1-ui-home-gates",false,gatesWhy+"; 60 ticks after the move off: on the pad "+serverOnPad+", countdown "+padCountdown);stage=11;}
                }
                case 11->{ // p1-ui-home-props: the props' solid pixels stay 12 px inside the page and clear of everything a page can show
                    int k=since(tick);
                    if(k==0){CampScreen.open("home");return;}
                    if(k<10)return;
                    int w=UiCanvas.width(),h=UiCanvas.height();int[] o=WarTable.origin();List<int[]> content=new ArrayList<>();List<String> names=new ArrayList<>();
                    for(int i=0;i<4;i++){content.add(new int[]{o[0]+56,o[1]+206+56*i,50,50});names.add("seat "+i);content.add(new int[]{o[0]+116,o[1]+206+56*i+12,WarTableHome.NAME_W,26});names.add("seat "+i+" name");}
                    content.add(new int[]{o[0]+56,o[1]+186,60,14});names.add("Party");
                    for(int i=0;i<3;i++){int cx=WarTableHome.CENTRES[i];content.add(new int[]{o[0]+cx-88,o[1]+188-17,182,168+17});names.add(WarTableHome.MODES[i]+" raised");content.add(new int[]{o[0]+cx-76,o[1]+200,156,141});names.add(WarTableHome.MODES[i]+" flat");}
                    content.add(new int[]{o[0]+WarTableHome.FIELD_X,o[1]+WarTableHome.FIELD_Y,172,193});names.add("field card");
                    content.add(new int[]{o[0]+360,o[1]+388,242,43});names.add("Ride");
                    content.add(new int[]{o[0]+WarTableHome.FIELD_X+40,o[1]+392,89,26});names.add("Last battle");
                    content.add(new int[]{o[0]+260,o[1]+440,440,12});names.add("notice");
                    content.add(new int[]{0,0,w,66});names.add("rail");
                    for(var e:boxes(c).entrySet()){content.add(e.getValue());names.add(e.getKey());}
                    StringBuilder bad=new StringBuilder();int pixels=0;
                    String[] sheet={"table/prop-candle","table/prop-inkwell"};int[] frames={4,3},px0={o[0]+WarTableHome.CANDLE_X,o[0]+WarTableHome.INKWELL_X},py0={o[1]+WarTableHome.CANDLE_Y,o[1]+WarTableHome.INKWELL_Y};
                    for(int s=0;s<2;s++)for(int f=0;f<frames[s];f++){var px=solid(c,sheet[s],f);if(px==null){bad.append("; ").append(sheet[s]).append(" unreadable");continue;}
                        int edge=0,under=0;String first="";
                        for(int[] q:px){int x=px0[s]+q[0],y=py0[s]+q[1];pixels++;if(x<12||y<12||x>=w-12||y>=h-12)edge++;
                            for(int i=0;i<content.size();i++){int[] b=content.get(i);if(x>=b[0]&&y>=b[1]&&x<b[0]+b[2]&&y<b[1]+b[3]){under++;if(first.isEmpty())first=names.get(i)+" at "+x+","+y;}}}
                        if(edge>0||under>0)bad.append("; ").append(sheet[s]).append(" frame ").append(f).append(": ").append(edge).append(" px within 12 px of the edge, ").append(under).append(" under content").append(first.isEmpty()?"":" (first "+first+")");}
                    boolean wood=WarTable.tableTexture("home").equals("table/oak-table")&&WarTable.tableTexture("recruit").equals("table/oak-table-recruit")&&WarTable.tableTexture("chapter").equals("table/oak-table");
                    var r=WarTableHome.rects();boolean drawn=r.containsKey("glow")&&r.containsKey("candle")&&r.containsKey("inkwell");
                    report("p1-ui-home-props",bad.length()==0&&pixels>0&&wood&&drawn,pixels+" solid prop pixels (the candle's 4 frames, the inkwell's 3) against the 12 px edge and "+content.size()+" content boxes (a full party of four, every sheet flat and raised, the field card, Ride, Last battle, the notice line, the rail, every widget)"+(bad.length()==0?": all clear":bad)
                        +"; the wood: home "+WarTable.tableTexture("home")+", recruit "+WarTable.tableTexture("recruit")+", chapter (not painted yet) "+WarTable.tableTexture("chapter")+"; drawn: glow "+str(r.get("glow"))+", candle "+str(r.get("candle"))+", inkwell "+str(r.get("inkwell")));
                    stage=12;
                }
                case 12->{ // p1-ui-home-scales: 1280x720 at GUI 3 (canvas 1280x720), 854x480 at GUI 2 (smaller than the page), 1920x1080 at GUI 2
                    int k=since(tick);
                    if(k==0){window(c,1280,720,3);return;}
                    if(k==20){int[] o=WarTable.origin();var r=WarTableHome.rects();int[] t=r.get("table"),rail=r.get("rail"),ride=box(c,"ride");int w=UiCanvas.width(),h=UiCanvas.height();
                        boolean covers=t!=null&&t[0]<=0&&t[1]<=0&&t[0]+t[2]>=w&&t[1]+t[3]>=h;
                        boolean props=r.containsKey("candle")&&r.containsKey("inkwell")&&r.containsKey("glow");
                        scalesOk&=home(c)!=null&&w==1280&&h==720&&o[0]==160&&o[1]==90&&Arrays.equals(ride,new int[]{520,478,240,40})&&rail!=null&&rail[2]==1280&&covers&&props;
                        scalesWhy="1280x720 GUI 3: canvas "+w+"x"+h+" (k "+UiCanvas.k()+"), origin "+o[0]+","+o[1]+", ride "+str(ride)+", rail "+str(rail)+", table "+str(t)+(covers?" covers the canvas":" leaves a gap")+", candle "+str(r.get("candle"))+", inkwell "+str(r.get("inkwell"));shot(c,"ui-home-720");return;}
                    if(k==22){window(c,854,480,2);return;}
                    if(k==42){int w=UiCanvas.width(),h=UiCanvas.height();var b=boxes(c);List<String> out=new ArrayList<>(),over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
                        for(int i=0;i<keys.size();i++){if(!inside(b.get(keys.get(i)),w,h))out.add(keys.get(i)+" "+str(b.get(keys.get(i))));for(int j=i+1;j<keys.size();j++)if(overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));}
                        var r=WarTableHome.rects();boolean cut=r.containsKey("candle")||r.containsKey("inkwell")||r.containsKey("glow"); // the props would cross the edge: left off
                        scalesOk&=home(c)!=null&&w==854&&h==480&&b.size()>=12&&out.isEmpty()&&over.isEmpty()&&!cut;
                        scalesWhy+="; 854x480 GUI 2: canvas "+w+"x"+h+", origin "+Arrays.toString(WarTable.origin())+", "+b.size()+" widgets, outside the canvas "+out+", overlapping "+over+", props "+(cut?"drawn across the edge: candle "+str(r.get("candle"))+", inkwell "+str(r.get("inkwell")):"left off (they would cross the 12 px edge)");shot(c,"ui-home-small");return;}
                    if(k==44){window(c,1920,1080,2,true);return;}
                    if(k==64){int[] o=WarTable.origin();boolean same=Arrays.equals(box(c,"ride"),LAYOUT[10])&&Arrays.equals(box(c,"camp"),LAYOUT[0]);
                        scalesOk&=home(c)!=null&&UiCanvas.k()==2&&UiCanvas.width()==960&&UiCanvas.height()==540&&o[0]==0&&o[1]==0&&same;
                        scalesWhy+="; 1920x1080 GUI 2 (borderless): k "+UiCanvas.k()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", camp "+str(box(c,"camp"))+", ride "+str(box(c,"ride"));shot(c,"ui-home-1080");
                        report("p1-ui-home-scales",scalesOk,scalesWhy);end(c);}
                }
                case 99->{stage=100;finish(c,"ui-home");}
                default->{}
            }
        }
        /** Closes the table, puts the session's sheet back on Skirmish and the window as the suite found it; p1-ui-home-suite follows. */
        void end(MinecraftClient c){finished=true;c.setScreen(null);CampaignClient.homeMode="skirmish";if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;}
    }

    /** UI polish M11, the last suite listed (it leaves the world): a disconnect empties what the battle HUD keeps between frames. A
     *  practice round on map 0 draws the pinned lesson 4, then a feed line, the hint slip and the cards with an attack order aimed (the
     *  card hides while an order is aimed; its wrap and lines stay); then the client leaves as the pause menu's Save and Quit does. No HUD frame runs without a world, so 20 ticks after it is gone the records
     *  must already be empty: what the last frame drew, the feed, the aimed order, the cards' groups and flashes, the lesson wrap and
     *  its lines, and the hint layer's clock, with the chat lift back at 0. */
    static final class UiLeaveChecks implements Suite,ServerWatch,HudWatch {
        volatile boolean want,fixed;volatile UUID player;volatile String thread="";
        // client thread
        int stage,waitFrom,waited,left;String before="";
        UiLeaveChecks(){ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{if(current==this)thread=Thread.currentThread().getName();});} // which thread the event fires on
        public void serverTick(GravesAndGlory.Battle b){if(want){var p=player==null?null:b.server.getPlayerManager().getPlayer(player);if(p!=null&&skirmish(b,p,0,1,"normal")){want=false;fixed=true;}}}
        public void hudFrame(MinecraftClient c){}
        /** What the battle HUD keeps between frames. */
        static String records(MinecraftClient c){
            return "rects "+BattleHud.rects().keySet()+", "+BattleHud.texts().size()+" texts, "+BattleHud.cards().size()+" cards, "+BattleHud.seals().size()+" seals, hint rows "+BattleHud.hintRows()+", "+BattleHud.keyRows().size()+" card key rows, "+EventFeed.lines(net.minecraft.util.Util.getMeasuringTimeMs()).size()+" feed lines, order aimed "+CommandMode.active()+" (at "+CommandMode.destination+"), "+CommandMode.groups.size()+" groups, "+CommandMode.flashUntil.size()+" flashes, lesson wrap "+(BattleHud.wrappedFor()!=null)+", "+GravesAndGloryClient.lessonDrawn.size()+" lesson lines, round clock "+BattleHud.roundShownMs+", chat lift "+ChatPlacement.lift(c.getWindow().getScaledHeight());
        }
        static boolean empty(MinecraftClient c){
            return BattleHud.rects().isEmpty()&&BattleHud.texts().isEmpty()&&BattleHud.cards().isEmpty()&&BattleHud.seals().isEmpty()&&BattleHud.hintRows().isEmpty()&&BattleHud.hintEnds().isEmpty()&&BattleHud.keyRows().isEmpty()&&EventFeed.lines(net.minecraft.util.Util.getMeasuringTimeMs()).isEmpty()
                &&!CommandMode.active()&&CommandMode.destination==null&&CommandMode.groups.isEmpty()&&CommandMode.flashUntil.isEmpty()&&BattleHud.wrappedFor()==null&&GravesAndGloryClient.lessonDrawn.isEmpty()&&BattleHud.roundShownMs==0&&ChatPlacement.lift(c.getWindow().getScaledHeight())==0;
        }
        public void step(MinecraftClient c,int tick){
            if(stage<3)CampaignClient.tutorial=3; // lesson 4 pinned, so the lesson card draws
            if(stage>0&&stage<3&&tick-waitFrom>3000){report("p1-ui-leave-clear",false,"no round with its lesson and an aimed order 3000 ticks in (stage "+stage+"): "+records(c));stage=9;finish(c,"ui-leave");return;}
            switch(stage){
                case 0->{
                    if(tick<20)return;
                    if(!queue.isEmpty()){report("p1-ui-leave-clear",false,"ui-leave leaves the world, so it must be the last suite listed; still queued: "+queue);stage=9;finish(c,"ui-leave");return;}
                    c.setScreen(null);player=c.player.getUuid();want=true;waitFrom=tick;stage=1;
                }
                case 1->{ // the round on screen with its lesson card: a feed line, then an attack order aimed
                    if(c.currentScreen!=null)c.setScreen(null);
                    if(fixed&&GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()&&BattleHud.rects().containsKey("lesson")){EventFeed.push("notice",GravesAndGloryClient.team,"Leaving the field",net.minecraft.util.Util.getMeasuringTimeMs());CommandMode.begin("attack");stage=2;}
                }
                case 2->{ // every record held at once (the lesson card hides while an order is aimed, its wrap and lines stay), then the pause
                    // menu's Save and Quit (GameMenuScreen.disconnect), outside the tick
                    var r=BattleHud.rects();
                    if(CommandMode.active()&&r.containsKey("feed")&&r.containsKey("hints")&&!BattleHud.cards().isEmpty()&&BattleHud.wrappedFor()!=null&&!GravesAndGloryClient.lessonDrawn.isEmpty()){
                        before=records(c);stage=3;
                        c.send(()->{if(c.world!=null)c.world.disconnect();c.disconnect(new net.minecraft.client.gui.screen.MessageScreen(net.minecraft.text.Text.translatable("menu.savingLevel")));c.setScreen(new net.minecraft.client.gui.screen.TitleScreen());});
                    }
                }
                default->{}
            }
        }
        /** Runs with no world too, where step() does not: 20 ticks after the world and the server are gone, the records must be empty. */
        @Override public void clientTick(MinecraftClient c){
            if(stage!=3)return;
            if(c.world==null&&c.getServer()==null)left++;
            if(left<20&&++waited<=1200)return;
            report("p1-ui-leave-clear",left>=20&&!thread.isEmpty()&&empty(c),"before the Save and Quit: "+before+"; the disconnect event fired on the thread '"+thread+"'; "+(left>=20?"20 client ticks with no world later: ":"the world was not gone 1200 ticks after the Save and Quit: ")+records(c));
            stage=9;finish(c,"ui-leave");
        }
    }
}
