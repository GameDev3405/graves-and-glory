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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.nbt.NbtCompound;
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
    public interface Suite { void step(MinecraftClient c,int tick);
        /** Called once on the client thread when the suite ends by any path (done, timed out or thrown), before the next suite starts: puts back what the suite changed, or fails a check that never ran. */
        default void cleanup(MinecraftClient c){} }
    /** Optional for a suite: called on the server thread after every Battle.tick, for checks that need exact tick timing. */
    public interface ServerWatch { void serverTick(GravesAndGlory.Battle b); }
    /** A suite that sees every HUD frame (Fabric's HUD callback runs after InGameHud has drawn the status bars), including frames
     *  while the client's ready is false, when step() does not run. clientTick runs every client tick, ready or not. */
    public interface HudWatch { void hudFrame(MinecraftClient c); default void clientTick(MinecraftClient c){} }
    static final Map<String,Supplier<Suite>> SUITES=new LinkedHashMap<>();
    static{SUITES.put("combat",Combat::new);SUITES.put("ui-lobby",LobbyChecks::new);SUITES.put("morale",MoraleChecks::new);SUITES.put("reinforce",ReinforceChecks::new);SUITES.put("bots",Bots::new);SUITES.put("tutorial",TutorialChecks::new);SUITES.put("report",ReportChecks::new);SUITES.put("ui-art",UiArtChecks::new);SUITES.put("ui-hud",UiHudChecks::new);SUITES.put("ui-feed",UiFeedChecks::new);SUITES.put("ui-home",UiHomeChecks::new);SUITES.put("ui-recruit",UiRecruitChecks::new);SUITES.put("ui-pages",UiPagesChecks::new);SUITES.put("ui-icons",UiIconsChecks::new);SUITES.put("ui-leave",UiLeaveChecks::new);}
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
    /** Runs the suite's cleanup, reports p1-<suite>-suite, then stops the client once the last listed suite is done. */
    public static void finish(MinecraftClient c,String suite){
        var done=current;current=null;
        if(done!=null)try{done.cleanup(c);}catch(Exception e){report("p1-"+suite+"-exception",false,"cleanup: "+e);e.printStackTrace();}
        report("p1-"+suite+"-suite",!failed,failed?"an earlier p1 check failed":"all checks passed");
        if(queue.isEmpty())c.scheduleStop();
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
        String list=System.getProperty("gravesandglory.phase1Checks","");if(list.isBlank()||!System.getProperty("gravesandglory.phase2Checks","").isEmpty())return;
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
            listenSounds(c);
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
    /** The ids of the sounds this client's own sound system played, in order (a SoundInstanceListener, registered once per client by listenSounds): what the player really hears. Every suite reads it. */
    public static final java.util.concurrent.CopyOnWriteArrayList<String> clientSounds=new java.util.concurrent.CopyOnWriteArrayList<>();
    static boolean soundsListened;
    public static void listenSounds(MinecraftClient c){if(soundsListened)return;soundsListened=true;c.getSoundManager().registerListener((sound,set)->clientSounds.add(sound.getId().toString()));}
    /** How many times this client has played the sound. */
    public static long heardOnClient(String id){return clientSounds.stream().filter(id::equals).count();}
    /**
     * A second player for the sound checks: a FakePlayer of the battle's world that stands where it is put and is listed among the server's players
     * while it is in, so the world-sound path (PlayerManager.sendToAround, where the three-argument PlayerEntity.playSound goes) reaches it like any
     * player in range; its network handler keeps every packet sent to it instead of a connection. sounds() lists the sounds it was sent.
     */
    public static final class Listener extends net.fabricmc.fabric.api.entity.FakePlayer {
        public final List<net.minecraft.network.packet.Packet<?>> packets=new java.util.concurrent.CopyOnWriteArrayList<>();
        Listener(net.minecraft.server.world.ServerWorld w){
            super(w,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Listener"));
            new net.minecraft.server.network.ServerPlayNetworkHandler(w.getServer(),new net.minecraft.network.ClientConnection(net.minecraft.network.NetworkSide.SERVERBOUND),this){
                @Override public void sendPacket(net.minecraft.network.packet.Packet<?> packet,net.minecraft.network.PacketCallbacks callbacks){packets.add(packet);}
            };
        }
        /** Stands at (x, y, z) in the battle's world and joins the server's player list. */
        public static Listener join(GravesAndGlory.Battle b,double x,double y,double z){var l=new Listener(b.world);l.refreshPositionAndAngles(x,y,z,0,0);b.server.getPlayerManager().getPlayerList().add(l);return l;}
        public void leave(GravesAndGlory.Battle b){b.server.getPlayerManager().getPlayerList().remove(this);}
        /** The ids of the sounds sent to it (PlaySoundS2CPacket), in order. */
        public List<String> sounds(){List<String> out=new ArrayList<>();for(var p:packets)if(p instanceof net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket s)out.add(s.getSound().value().getId().toString());return out;}
        /** Those of them that are this mod's (gravesandglory:...) or the order chime. */
        public List<String> cues(){return sounds().stream().filter(s->s.startsWith("gravesandglory:")||s.endsWith("note_block.chime")).toList();}
    }

    /** Task 1: visible delayed projectiles, team impact flashes, HUD health flash and tick cost. Stages run on the server tick. */
    static final class Combat implements Suite,ServerWatch {
        volatile int stage;volatile String wantShot;volatile boolean shotTaken;volatile boolean finished;
        volatile UUID player;long stageAt;int shotDelay,hudTick=-1,hudSeen=-1;
        final Map<LivingEntity,Float> lastHealth=new HashMap<>();
        Projectiles.Shot first;float firstHealth;boolean firstDelayOk;
        GravesAndGlory.Unit chosen;Projectiles.Shot duskShot;int flashesBefore;boolean duskOk;String duskWhy="";
        GravesAndGlory.Unit orphanShooter,orphanSecond;Exception orphanError;
        NbtCompound savedArrow;boolean arrowAlive,arrowSaved;
        double tickSum;int tickSamples,troops;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        boolean late(GravesAndGlory.Battle b,int ticks){return b.time-stageAt>ticks;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        /** A spectator camera floats to one side of the field: troops never target it and it never respawns. */
        void camera(GravesAndGlory.Battle b,Vec3d look,double side,double up){
            var p=sp(b);p.changeGameMode(GameMode.SPECTATOR);double drop=up+p.getStandingEyeHeight()-1;
            b.relocate(p,look.x,look.y+up,look.z+side,180,(float)Math.toDegrees(Math.atan2(drop,side)));
        }

        /** The commander's army fights as a bot in the tick-cost stage; the next suite's returnLobby drops bot armies, and with it his seat, so he gets it back however this suite ends. */
        void handBack(GravesAndGlory.Battle b){var mine=player==null?null:b.squads.get(player);if(mine!=null)mine.bot=false;}
        public void cleanup(MinecraftClient c){server(c,this::handBack);}

        /** p1-arrow-reload, part 1 (final review, minor 1): the arrow of the shot just fired survives its own spawn, and is written to NBT in flight the way a chunk save writes it. */
        void saveArrow(GravesAndGlory.Battle b){
            var arrow=b.shots.get(b.shots.size()-1).visual();
            arrowAlive=arrow!=null&&!arrow.isRemoved();
            savedArrow=new NbtCompound();arrowSaved=arrow!=null&&arrow.shouldSave()&&arrow.saveNbt(savedArrow);
        }
        /** p1-arrow-reload, part 2: the round has ended, so nothing is queued, as after a restart. The arrow rebuilt from its NBT and added to the world fires the same load event a chunk load does, and must be discarded, not fly on as a real arrow. A real quit and reload is not scripted here. */
        void reloadArrow(GravesAndGlory.Battle b){
            Entity back=arrowSaved?EntityType.getEntityFromNbt(savedArrow,b.world).orElse(null):null;
            boolean arrowAgain=back instanceof ArrowEntity&&back.getCommandTags().contains(Projectiles.TAG);
            if(back!=null){back.setUuid(UUID.randomUUID());b.world.spawnEntity(back);}
            boolean gone=back!=null&&back.isRemoved();int left=tagged(b);
            report("p1-arrow-reload",arrowAlive&&arrowSaved&&arrowAgain&&gone&&left==0&&Projectiles.inFlight(b)==0,"a fresh shot's arrow alive after its own spawn="+arrowAlive+"; written to NBT in flight="+arrowSaved+", read back as a tagged arrow="+arrowAgain+"; with "+Projectiles.inFlight(b)+" shots queued the rebuilt arrow was discarded on entering the world="+gone+", tagged arrows left="+left);
            if(back!=null&&!back.isRemoved())back.discard(); // a stray must not fail the checks after this one
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
                        duskOk=BattleEffects.flashes>flashesBefore&&BattleEffects.lastFlashColor==b.side(1).color();
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
                    if(BattleEffects.flashes>flashesBefore){freeze(chosen);boolean dawnOk=BattleEffects.lastFlashColor==b.side(0).color();
                        report("p1-impact-team-colour",duskOk&&dawnOk,duskWhy+"; Dawn melee colour "+Integer.toHexString(BattleEffects.lastFlashColor)+" (Dusk "+Integer.toHexString(b.side(1).color())+", Dawn "+Integer.toHexString(b.side(0).color())+")");next(b,10);}
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
                    try{Projectiles.fire(b,orphanShooter,orphanSecond.mob,"arrow",4);System.out.println("[p1] orphan: "+Projectiles.inFlight(b)+" in flight at finish");saveArrow(b);b.campaign.finish(-1);}catch(Exception e){orphanError=e;e.printStackTrace();}
                    next(b,13);}}
                case 13->{int left=tagged(b);report("p1-shot-orphan",orphanError==null&&Projectiles.inFlight(b)==0&&left==0&&!b.running,"error="+orphanError+" inFlight="+Projectiles.inFlight(b)+" taggedArrows="+left+" running="+b.running);reloadArrow(b);next(b,14);}
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
                        handBack(b);next(b,16);}
                }
                default->{}
            }
            lastHealth.clear();for(var u:b.units)lastHealth.put(u.mob,u.mob.getHealth());
        }
    }

    /** Task 2, rewritten by the no-routing design: no group breaks (p1-no-rout); flank blows, C16s and covered flanks (p1-flank-damage); the LAST STAND state, its feed line and its HUD stamp (p1-last-stand, p1-hud-last-stand); command feedback to the commanding player alone and no horn in the world (p1-command-sound, p1-charge-horn); Retreat and Move arrive and fight as Hold (p1-retreat-arrive). Stages run on the server tick. */
    static final class MoraleChecks implements Suite,ServerWatch {
        static final Spec.CombatDisciplineRow D=Spec.COMBATDISCIPLINE[0];
        static final double FX=48,FZ=0; // open valley on the Dusk side of Rivergate (60 blocks from the sheet's Dusk base; CampaignMaps moves the live base to x=283, inside a village)
        volatile int stage;volatile String wantShot;volatile boolean finished,hudDone;
        volatile UUID player;long stageAt;int shotDelay;
        GravesAndGlory.Squad own,enemy,flankers;
        final List<GravesAndGlory.Unit> flank=new ArrayList<>();
        // p1-no-rout
        boolean orderKept=true;int samples,targeted;double startHome;String noRoutWhy="";boolean noRoutOk;
        // p1-last-stand
        int linesBefore;long onAt=-1,offAt=-1,againAt=-1,risenAt=-1;boolean lastOk=true;String lastWhy="";
        // the sounds
        boolean soundOk=true;String soundWhy="";
        // the arrival
        GravesAndGlory.Unit waylayer,greeter;int strikes;boolean targetOnWay,runSeen,fought;long arrivedAt=-1,foughtAt=-1;String arriveWhy="";boolean arriveOk=true;Vec3d dest;
        // the sounds' transport (p1-feedback-reach): what the commanding client played and what a second player 5 blocks from him was sent
        List<String> listenerCues=List.of();long routBefore,chargeBefore,chimeBefore;
        // the arrival's pull and its ranged and support legs
        long pullAt=-1,settleAt=-1;double pullMax;int pullSamples,pullArrived,pullHold,pullTargets;boolean pullOk,rangedStart,supportStart;Leg rangedLeg,supportLeg;Vec3d rangedDest,supportDest;
        // the last-stand line due after lastStandFeedSeconds
        long off2At=-1,on2At=-1;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        List<GravesAndGlory.Unit> knights(GravesAndGlory.Battle b){return b.units.stream().filter(u->u.squad==own&&u.spec.group().equals("infantry")&&u.mob.isAlive()).toList();}
        double homeDistance(GravesAndGlory.Battle b){Vec3d home=GravesAndGlory.Battle.base(0);return knights(b).stream().mapToDouble(u->Math.hypot(u.mob.getX()-home.x,u.mob.getZ()-home.z)).average().orElse(0);}
        Vec3d centre(GravesAndGlory.Battle b){return Morale.centre(knights(b));}
        static void place(GravesAndGlory.Battle b,GravesAndGlory.Unit u,double x,double z,float yaw){u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(x,top(b,x,z),z,yaw,0);u.mob.setHeadYaw(yaw);u.mob.setBodyYaw(yaw);u.deployed=u.mob.getPos();u.lastPosition=u.mob.getPos();u.nextPath=0;u.stuckTicks=0;}
        /** A stored group order (block formation, facing Dusk) to a spot, as Battle.issue would store it. */
        void order(GravesAndGlory.Battle b,String group,String order,double x,double z){own.arrived.remove(group);own.assignments.put(group,new GravesAndGlory.Assignment(order,"block",new Vec3d(x,top(b,x,z),z),-90));}
        /** A fresh, full infantry group standing in block at the fixture point on Hold, facing Dusk; frozen (never attacking) or free to fight. */
        void freshInfantry(GravesAndGlory.Battle b,boolean frozen){
            for(var u:knights(b))u.mob.discard();b.units.removeIf(u->u.squad==own&&u.spec.group().equals("infantry"));
            own.lastStand.remove("infantry");own.lastStandAt.remove("infantry");b.reinforce(own);order(b,"infantry","hold",FX,FZ);
            for(var u:knights(b)){Vec3d at=b.formation(u);place(b,u,at.x,at.z,-90);if(frozen)freeze(u);}
            Houses.recompute(own);
        }
        /** Kills the rearmost knights (highest slots), leaving the front rank. */
        void kill(GravesAndGlory.Battle b,int n){var list=new ArrayList<>(knights(b));list.sort(Comparator.comparingInt(u->-u.slot));for(int i=0;i<n&&i<list.size();i++)list.get(i).mob.kill();b.units.removeIf(u->!u.mob.isAlive());}
        /** The Dusk squad 6 blocks in front of the knights. */
        void enemyFront(GravesAndGlory.Battle b){enemy.target=new Vec3d(FX+6,top(b,FX+6,FZ),FZ);int i=0;for(var u:b.units)if(u.squad==enemy){double x=FX+6+(i/5)*2,z=FZ+(i%5-2)*2;place(b,u,x,z,90);i++;}}
        /** The Dusk squad camped on the Dawn base, far beyond lastStandRadius of the knights. */
        void enemyHome(GravesAndGlory.Battle b){Vec3d home=GravesAndGlory.Battle.base(0);enemy.target=home;int i=0;for(var u:b.units)if(u.squad==enemy){place(b,u,home.x+6+(i/5)*2,home.z+(i%5-2)*2,90);i++;}}
        void camera(GravesAndGlory.Battle b,double x,double z,double up,float yaw,float pitch){var p=sp(b);p.changeGameMode(GameMode.SPECTATOR);b.relocate(p,x,top(b,x,z)+up,z,yaw,pitch);}
        /** Frozen Dusk Risen of the flankers squad standing behind the knights (west of them: the knights face east), each with the nearest knight as its target. */
        void placeFlankers(GravesAndGlory.Battle b,int n){
            Vec3d at=centre(b);flankers=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(at.x-5,at.y,at.z));flankers.order="hold";flankers.yaw=-90;
            var revenant=troop("revenant");var ks=knights(b);flank.clear();
            for(int i=0;i<n;i++){double z=at.z-1.5+i;var u=b.spawn(flankers,revenant,40+i,new Vec3d(at.x-5,top(b,at.x-5,z),z));freeze(u);flank.add(u);
                var nearest=ks.stream().min(Comparator.comparingDouble(k->k.mob.squaredDistanceTo(u.mob))).orElseThrow();u.mob.setTarget(nearest.mob);}
        }
        void dropFlankers(GravesAndGlory.Battle b){if(flankers==null)return;for(var u:b.units)if(u.squad==flankers)u.mob.discard();b.units.removeIf(u->u.squad==flankers);flank.clear();}
        /** One blow of base 1 from the attacker's troop on the victim, past the victim's hurt cooldown, as a primary hit (not a cleave's second target), and the hit it logged. */
        static gg.gravesandglory.Combat.HitLog blow(GravesAndGlory.Battle b,GravesAndGlory.Unit attacker,GravesAndGlory.Unit victim,String kind){
            victim.mob.timeUntilRegen=0;victim.mob.hurtTime=0;var mark=gg.gravesandglory.Combat.LAST.peekLast();gg.gravesandglory.Combat.resolve(b,attacker,victim.mob,1f,kind,false);var h=gg.gravesandglory.Combat.LAST.peekLast();return h==mark?null:h;
        }
        static boolean is(gg.gravesandglory.Combat.HitLog h,double counters,List<String> rows){return h!=null&&Math.abs(h.counters()-counters)<1e-9&&h.rows().equals(rows);}
        static String show(gg.gravesandglory.Combat.HitLog h){return h==null?"no hit":"counters "+h.counters()+" rows "+h.rows()+" stance "+h.stance()+" traits "+h.traits()+" total "+h.total();}
        /** How many feedback sounds this player heard of a kind. */
        static int heard(UUID p,String kind){int n=0;for(var h:BattleEffects.heard)if(h.player().equals(p)&&h.kind().equals(kind))n++;return n;}
        static int lines(UUID owner,String group){int n=0;for(var l:Morale.lastStandLines)if(l.owner().equals(owner)&&l.group().equals(group))n++;return n;}
        boolean lastStand(){return Morale.lastStand(own,"infantry");}
        /** A frozen Dusk Risen of the flankers squad at a spot (the waylayer on the way, the greeter near a destination). */
        GravesAndGlory.Unit risen(GravesAndGlory.Battle b,double x,double z,float yaw){var u=b.spawn(flankers,troop("revenant"),40+flank.size(),new Vec3d(x,top(b,x,z),z));freeze(u);place(b,u,x,z,yaw);flank.add(u);return u;}
        boolean anyTarget(GravesAndGlory.Battle b){for(var k:knights(b))if(k.mob.getTarget()!=null)return true;return false;}
        boolean arrived(GravesAndGlory.Battle b){var ks=knights(b);return !ks.isEmpty()&&Tactics.arrived(b,ks.get(0),b.assignment(ks.get(0)));}
        /** One group's arrival reading: when it was taken (stage ticks), whether it is right, and the reading. */
        record Leg(long at,boolean ok,String why){}
        /**
         * Reads a group's arrival on a Retreat to spot: null while Tactics.arrived is false for its first member; else the reading at the moment it turned true:
         * its centre's distance to the centre of its slots (want arriveRadius or less: the arrival is measured there, Battle.formation) and the latch, with its
         * distance to the spot for the record.
         */
        Leg leg(GravesAndGlory.Battle b,String group,Vec3d spot){
            var ms=Morale.members(b,own,group);if(ms.isEmpty())return new Leg(since(b),false,group+": no member left");
            if(!Tactics.arrived(b,ms.get(0),b.assignment(ms.get(0))))return null;
            Vec3d c=Morale.centre(ms),slots=Vec3d.ZERO;for(var m:ms)slots=slots.add(b.formation(m));slots=slots.multiply(1.0/ms.size());
            double toSpot=Math.sqrt(GravesAndGlory.Battle.horizontalDistance(c,spot)),toSlots=Math.sqrt(GravesAndGlory.Battle.horizontalDistance(c,slots));
            boolean ok=toSlots<=D.arriveRadius()&&own.arrived.contains(group);
            return new Leg(since(b),ok,group+" ("+ms.size()+" troops) arrived at +"+since(b)+" with its centre "+String.format("%.1f",toSlots)+" from the centre of its slots (want "+D.arriveRadius()+" or less) and "+String.format("%.1f",toSpot)+" from the spot, latched="+own.arrived.contains(group)+"="+ok);
        }
        /** The same group 100 ticks on, settled on its slots: its centre stands beyond arriveRadius of the spot (its slots are behind it, so by the spot it would count as not arrived) and it is still arrived and latched. */
        Leg settled(GravesAndGlory.Battle b,String group,Vec3d spot){
            var ms=Morale.members(b,own,group);if(ms.isEmpty())return new Leg(since(b),false,group+": no member left");
            double toSpot=Math.sqrt(GravesAndGlory.Battle.horizontalDistance(Morale.centre(ms),spot));boolean arrived=Tactics.arrived(b,ms.get(0),b.assignment(ms.get(0))),latched=own.arrived.contains(group);
            boolean ok=toSpot>D.arriveRadius()&&arrived&&latched;
            return new Leg(since(b),ok,group+" settled with its centre "+String.format("%.1f",toSpot)+" from the spot (want more than "+D.arriveRadius()+": by the spot it would not count as arrived) and still arrived="+arrived+", latched="+latched+"="+ok);
        }
        /** The troop faces Dusk (east) again, whatever it was looking at, so a bearing measured from it is the fixture's. */
        static void face(GravesAndGlory.Unit u){u.mob.setYaw(-90);u.mob.setHeadYaw(-90);u.mob.setBodyYaw(-90);}

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(wantShot!=null&&++shotDelay>=2){c.setScreen(null);shot(c,wantShot);wantShot=null;shotDelay=0;}
            if(!hudDone&&stage>=6&&stage<=7){
                var g=CommandMode.groups.stream().filter(v->v.id().equals("infantry")).findFirst().orElse(null);
                if(g!=null&&g.lastStand()&&wantShot==null){hudDone=true;wantShot="hud-last-stand";report("p1-hud-last-stand",true,"client infantry group lastStand=true, count "+g.count()+", stored order "+g.order()+" (the card stamps LAST STAND; picture hud-last-stand)");}
            }
            if(!hudDone&&stage>7){hudDone=true;report("p1-hud-last-stand",false,"the client never saw the infantry at its last stand");}
            if(stage==99&&!finished&&wantShot==null){finished=true;finish(c,"morale");}
            if(tick>12000&&!finished){finished=true;report("p1-morale-timeout",false,"stuck in stage "+stage);finish(c,"morale");} // the id keeps the suite's name (morale) for continuity with every plan's regression list, though no morale system remains
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // fixture: practice on map 0, the player's army 60 blocks from the Dusk base on Hold, one frozen Dusk squad holding 6 blocks in front
                    var p=sp(b);if(p==null||!skirmish(b,p,0,1,"normal"))return;
                    own=b.squads.get(player);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    for(var u:b.units)if(u.squad!=own)u.mob.discard();b.units.removeIf(u->u.squad!=own);
                    own.order="hold";own.formation="block";own.target=new Vec3d(FX,top(b,FX,FZ),FZ);own.yaw=-90;
                    order(b,"ranged","hold",FX-8,FZ+24);order(b,"support","hold",FX-8,FZ+30);
                    for(var u:b.units)if(u.squad==own&&!u.spec.group().equals("infantry")){Vec3d at=b.formation(u);place(b,u,at.x,at.z,-90);freeze(u);}
                    freshInfantry(b,true);
                    enemy=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(FX+6,65,FZ));enemy.order="hold";enemy.yaw=90;int slot=0;
                    // Phase 1's Dusk default line (12 Risen, 6 Bone Archers, 2 Gravecallers), spawned directly so the fixture stays the one Phase 1 measured.
                    String[] duskLine={"revenant","bonebow","hexer"};int[] duskCounts={12,6,2};
                    for(int k=0;k<duskLine.length;k++)for(int n=0;n<duskCounts[k];n++,slot++)freeze(b.spawn(enemy,troop(duskLine[k]),slot,new Vec3d(FX+6,65,FZ)));
                    enemyFront(b);
                    camera(b,FX-20,FZ+9,5,-105,18);
                    System.out.println("[p1] morale fixture at "+(int)FX+", "+(int)FZ+": "+knights(b).size()+" knights, fullHealth="+own.fullHealth+", traits "+own.traits+", "+b.units.stream().filter(u->u.squad==enemy).count()+" Dusk troops, map "+b.campaign.map);
                    next(b,2);
                }
                case 2->{if(since(b)>=40){startHome=homeDistance(b);kill(b,10);samples=0;targeted=0;orderKept=true;System.out.println("[p1] no-rout: killed 10, "+knights(b).size()+" knights left at "+String.format("%.0f%%",100*BattleMath.strength(knights(b).stream().mapToDouble(u->u.mob.getHealth()).sum(),own.fullHealth.get("infantry"))));next(b,3);}}
                case 3->{ // p1-no-rout, part one: two knights of twelve, Dusk 6 blocks in front: for 300 ticks they keep Hold at the fixture point, keep targets and do not run home
                    var ks=knights(b);if(ks.isEmpty()){report("p1-no-rout",false,"no knight left");next(b,5);break;}
                    var a=b.assignment(ks.get(0));orderKept&=a.order.equals("hold")&&Math.abs(a.target.x-FX)<.01;
                    if(since(b)%20==0){samples++;if(anyTarget(b))targeted++;}
                    if(since(b)<300)return;
                    double home=homeDistance(b);boolean stayed=startHome-home<4;
                    noRoutOk=orderKept&&targeted*2>=samples&&stayed&&lastStand();
                    noRoutWhy="two knights of twelve at "+String.format("%.0f%%",100*BattleMath.strength(ks.stream().mapToDouble(u->u.mob.getHealth()).sum(),own.fullHealth.get("infantry")))+" with 20 Dusk troops 6 blocks in front for 300 ticks: their order stayed Hold at the fixture point="+orderKept+"; a knight had a target in "+targeted+" of "+samples+" samples; mean distance to base "+String.format("%.1f",startHome)+" -> "+String.format("%.1f",home)+" (did not run home="+stayed+"); at their last stand="+lastStand();
                    // part two: four Risen strike them from behind; the group keeps holding
                    for(var k:ks)face(k);placeFlankers(b,4);StringBuilder hits=new StringBuilder();boolean c16=true;
                    for(var f:flank){var h=blow(b,f,ks.get(0),"melee");c16&=is(h,1.3,List.of("C16"));hits.append(show(h)).append("; ");}
                    noRoutOk&=c16;noRoutWhy+="; four Risen struck a knight from behind: "+hits+"(want counters 1.3 rows [C16] each="+c16+")";
                    next(b,4);
                }
                case 4->{ // 60 ticks after the flank blows the order is still Hold; then p1-flank-damage on the same knight
                    var ks=knights(b);if(ks.isEmpty()){report("p1-no-rout",false,noRoutWhy+"; no knight left after the flank");next(b,5);break;}
                    var a=b.assignment(ks.get(0));orderKept&=a.order.equals("hold");
                    if(since(b)<60)return;
                    report("p1-no-rout",noRoutOk&&orderKept,noRoutWhy+"; 60 ticks after the flank blows the order is "+a.order+" (want hold, kept="+orderKept+")");
                    var k=ks.get(0);face(k);enemyFront(b);Houses.recompute(own);var f=flank.get(0);
                    var front=b.units.stream().filter(u->u.squad==enemy&&u.spec.id().equals("revenant")&&u.mob.isAlive()).min(Comparator.comparingDouble(u->u.mob.squaredDistanceTo(k.mob))).orElseThrow();
                    var fromFront=blow(b,front,k,"melee");var melee=blow(b,f,k,"melee");var arrow=blow(b,f,k,"arrow");var bolt=blow(b,f,k,"bolt");var blight=blow(b,f,k,"blight");
                    double bearingBehind=BattleMath.relativeBearing(k.mob.getYaw(),f.mob.getX()-k.mob.getX(),f.mob.getZ()-k.mob.getZ()),bearingFront=BattleMath.relativeBearing(k.mob.getYaw(),front.mob.getX()-k.mob.getX(),front.mob.getZ()-k.mob.getZ());
                    boolean open=is(fromFront,1.0,List.of())&&is(melee,1.3,List.of("C16"))&&is(arrow,1.15,List.of("C16s"))&&is(bolt,1.15,List.of("C4","C16s"))&&is(blight,1.0,List.of("C4"))&&!Houses.covered(b,k);
                    String why="a Dusk Risen's blow from the front (bearing "+String.format("%.0f",bearingFront)+") "+show(fromFront)+"; from behind (bearing "+String.format("%.0f",bearingBehind)+", arc "+D.flankArcDegrees()+"): melee "+show(melee)+" (want 1.3 C16), an arrow "+show(arrow)+" (want 1.15 C16s) and a bolt "+show(bolt)+" (want 1.15: C4 at x1, magic ignores the shield, and C16s), blight "+show(blight)+" (area: want 1.0, C4 alone); covered="+Houses.covered(b,k)+" (want false: his Gilded archers stand 24+ off, a knight of his own group 1.5 off does not cover)="+open;
                    // covered: an Order Archer of his own army (another group) steps to within the tier's radius of the knight; the bonus returns just outside it
                    var archer=b.units.stream().filter(u->u.squad==own&&u.spec.id().equals("archer")&&u.mob.isAlive()).findFirst().orElseThrow();Vec3d was=archer.mob.getPos();
                    int tier=own.traits.getOrDefault("gilded",0);var t=CounterMath.trait(Spec.HOUSETRAITS,"gilded",tier);double radius=t==null?0:t.radius();
                    place(b,archer,k.mob.getX()-3,k.mob.getZ()+3,-90);boolean covered=Houses.covered(b,k);var cMelee=blow(b,f,k,"melee");var cArrow=blow(b,f,k,"arrow");
                    place(b,archer,k.mob.getX()-(radius+1),k.mob.getZ(),-90);boolean uncovered=!Houses.covered(b,k);var uMelee=blow(b,f,k,"melee");
                    place(b,archer,was.x,was.z,-90);
                    boolean cover=tier>=1&&covered&&is(cMelee,1.0,List.of())&&is(cArrow,1.0,List.of())&&uncovered&&is(uMelee,1.3,List.of("C16"));
                    why+="; with an Order Archer of his army 4.2 off (Gilded tier "+tier+", radius "+radius+") the knight is covered="+covered+": melee from behind "+show(cMelee)+", an arrow "+show(cArrow)+" (want 1.0, no row); with the archer "+(radius+1)+" off covered="+!uncovered+" and melee from behind "+show(uMelee)+" (want 1.3 C16)="+cover;
                    report("p1-flank-damage",open&&cover,why);
                    dropFlankers(b);next(b,5);
                }
                case 5->{ // p1-last-stand: a fresh full group with Dusk in front leaves the state; then ten die
                    freshInfantry(b,true);enemyFront(b);next(b,6);
                }
                case 6->{
                    if(since(b)<40)return;
                    if(since(b)==40){boolean off=!lastStand();lastOk=off;lastWhy="a fresh full group with Dusk 6 blocks off is not at its last stand="+off;linesBefore=lines(player,"infantry");kill(b,10);onAt=-1;}
                    if(onAt<0&&lastStand())onAt=since(b)-40;
                    if(onAt<0){if(since(b)>100){lastOk=false;lastWhy+="; after ten of twelve died the state never came on in 60 ticks";report("p1-last-stand",false,lastWhy);next(b,8);}return;}
                    int added=lines(player,"infantry")-linesBefore;boolean line=added==1;lastOk&=onAt<=30&&line;
                    lastWhy+="; two knights of twelve ("+String.format("%.0f%%",100*BattleMath.strength(knights(b).stream().mapToDouble(u->u.mob.getHealth()).sum(),own.fullHealth.get("infantry")))+", at or below "+D.lastStandShare()+") with Dusk within "+D.lastStandRadius()+" turned to their last stand "+onAt+" ticks after the deaths (want 30 or fewer: the pass runs every 20 ticks, plus a beat) and the hook recorded "+added+" line(s) for this owner's infantry (want 1)="+line;
                    next(b,7);
                }
                case 7->{ // the HUD stamp is read on the client (step); then Dusk camps far away and the state goes
                    if(!hudDone&&since(b)<120)return;
                    enemyHome(b);offAt=-1;next(b,8);
                }
                case 8->{
                    if(offAt<0&&!lastStand())offAt=since(b);
                    if(offAt<0){if(since(b)>60){lastOk=false;lastWhy+="; with Dusk gone the state stayed on for 60 ticks";report("p1-last-stand",false,lastWhy);next(b,11);}return;}
                    lastWhy+="; with Dusk camped "+String.format("%.0f",Math.sqrt(GravesAndGlory.Battle.horizontalDistance(centre(b),GravesAndGlory.Battle.base(0))))+" blocks off the state went "+offAt+" ticks later (want 30 or fewer: the pass runs every 20 ticks, plus a beat)";lastOk&=offAt<=30;
                    enemyFront(b);againAt=-1;next(b,9);
                }
                case 9->{ // Dusk returns inside 30 s: the state comes back and the hook adds no second line
                    if(againAt<0&&lastStand())againAt=since(b);
                    if(againAt<0){if(since(b)>60){lastOk=false;lastWhy+="; with Dusk back the state did not return in 60 ticks";report("p1-last-stand",false,lastWhy);next(b,11);}return;}
                    int added=lines(player,"infantry")-linesBefore;boolean once=added==1;lastOk&=againAt<=30&&once;
                    lastWhy+="; with Dusk back the state returned "+againAt+" ticks later and the hook still has "+added+" line(s) (one per "+D.lastStandFeedSeconds()+" s)="+once;
                    // the line due after lastStandFeedSeconds: the last telling is set back that far, Dusk leaves and returns, and the hook adds exactly one more line
                    own.lastStandAt.put("infantry",b.time-D.lastStandFeedSeconds()*20L);enemyHome(b);off2At=-1;next(b,21);
                }
                case 21->{
                    if(off2At<0&&!lastStand())off2At=since(b);
                    if(off2At<0){if(since(b)>60){lastOk=false;lastWhy+="; (the 30 s leg) with Dusk gone again the state stayed on for 60 ticks";report("p1-last-stand",false,lastWhy);next(b,11);}return;}
                    lastOk&=off2At<=30;enemyFront(b);on2At=-1;next(b,22);
                }
                case 22->{
                    if(on2At<0&&lastStand())on2At=since(b);
                    if(on2At<0){if(since(b)>60){lastOk=false;lastWhy+="; (the 30 s leg) with Dusk back the state did not return in 60 ticks";report("p1-last-stand",false,lastWhy);next(b,11);}return;}
                    int added=lines(player,"infantry")-linesBefore;boolean more=added==2;lastOk&=on2At<=30&&more;
                    lastWhy+="; with the last telling set back "+D.lastStandFeedSeconds()+" s, Dusk gone (the state off "+off2At+" ticks later, want 30 or fewer) and back (on "+on2At+" ticks later, want 30 or fewer) the hook has "+added+" line(s) (want 2: exactly one more)="+more;
                    // the strength rises: ten fresh knights join at the fixture point with Dusk still in front
                    b.reinforce(own);for(var u:knights(b))if(u.deployed==null||u.mob.squaredDistanceTo(centre(b))>400){Vec3d at=b.formation(u);place(b,u,at.x,at.z,-90);freeze(u);}
                    Houses.recompute(own);risenAt=-1;next(b,10);
                }
                case 10->{
                    if(risenAt<0&&!lastStand())risenAt=since(b);
                    if(risenAt<0){if(since(b)>60){lastOk=false;lastWhy+="; with ten fresh knights the state stayed on for 60 ticks";report("p1-last-stand",false,lastWhy);next(b,11);}return;}
                    lastOk&=risenAt<=30&&knights(b).size()==12;
                    report("p1-last-stand",lastOk,lastWhy+"; with ten fresh knights ("+knights(b).size()+" living, Dusk still 6 off) the state went "+risenAt+" ticks later (want 30 or fewer: the pass runs every 20 ticks, plus a beat)");
                    next(b,11);
                }
                case 11->{freshInfantry(b,true);enemyFront(b);camera(b,FX-12,FZ,4,-90,12);next(b,12);}
                case 12->{if(since(b)>=20){ // p1-charge-horn and the command sounds: Retreat and Charge play to the commanding player alone, once per kind per hornCooldownSeconds; another order the chime; a bot's charge nothing
                    var p=sp(b);var foe=b.units.stream().filter(u->u.squad==enemy&&u.mob.isAlive()&&p.canSee(u.mob)).findFirst().orElse(null);
                    if(foe==null){report("p1-charge-horn",false,"no Dusk troop visible from the camera");soundOk=false;soundWhy="no Dusk troop visible from the camera";next(b,13);break;}
                    var listener=Listener.join(b,p.getX()+5,p.getY(),p.getZ());routBefore=heardOnClient("gravesandglory:horn_rout");chargeBefore=heardOnClient("gravesandglory:horn_charge");chimeBefore=heardOnClient("minecraft:block.note_block.chime");
                    int total=BattleEffects.heard.size(),r0=heard(player,"retreat"),c0=heard(player,"charge");
                    own.lastOrder=-1000;b.tactical(p,"infantry","retreat","block",FX-20,FZ,-90,-1);int r1=heard(player,"retreat");
                    own.lastOrder=-1000;b.tactical(p,"ranged","retreat","block",FX-20,FZ+24,-90,-1);int r2=heard(player,"retreat");
                    own.lastOrder=-1000;b.tactical(p,"infantry","charge","block",foe.mob.getX(),foe.mob.getZ(),-90,foe.mob.getId());int c1=heard(player,"charge");
                    own.lastOrder=-1000;b.tactical(p,"ranged","charge","block",foe.mob.getX(),foe.mob.getZ(),-90,foe.mob.getId());int c2=heard(player,"charge");
                    var ranged=own.assignments.get("ranged");
                    own.lastOrder=-1000;b.tactical(p,"support","hold","block",FX-8,FZ+30,-90,-1);int afterHold=BattleEffects.heard.size();
                    b.markCharge(enemy,"all");int afterBot=BattleEffects.heard.size();
                    listenerCues=listener.cues();listener.leave(b);boolean nobodyElse=listenerCues.isEmpty();
                    boolean mine=BattleEffects.heard.stream().allMatch(h->h.player().equals(player));
                    boolean charge=c1==c0+1&&c2==c1&&ranged!=null&&ranged.order.equals("charge");
                    report("p1-charge-horn",charge&&nobodyElse,"the commanding player's charge feedback "+c0+" -> "+c1+" after the infantry charge, "+c2+" after the ranged charge inside the "+D.hornCooldownSeconds()+" s cooldown (ranged order "+(ranged==null?"none":ranged.order)+"); a second player 5 blocks from him was sent "+listenerCues+" (want nothing: the cues go to him alone)="+nobodyElse);
                    soundOk=r1==r0+1&&r2==r1&&charge&&afterHold==total+2&&afterBot==afterHold&&mine;
                    soundWhy="Retreat feedback to the player "+r0+" -> "+r1+" (want +1), a second Retreat at once "+r2+" (want no more); Charge "+c0+" -> "+c1+" -> "+c2+"; a Hold order added nothing ("+total+" -> "+afterHold+", want +2 over the four orders); a bot's charge added nothing ("+afterBot+"); every sound heard was this player's="+mine;
                    next(b,16);
                }}
                case 16->{ // p1-feedback-reach: what was really sent. The commanding client played the Retreat cue, the Charge cue and the chime once each (its own sound system, SoundInstanceListener), and the second player 5 blocks from him was sent none of them
                    if(since(b)<30)return;
                    long rout=heardOnClient("gravesandglory:horn_rout")-routBefore,charges=heardOnClient("gravesandglory:horn_charge")-chargeBefore,chime=heardOnClient("minecraft:block.note_block.chime")-chimeBefore;
                    boolean reach=rout==1&&charges==1&&chime==1&&listenerCues.isEmpty();
                    report("p1-feedback-reach",reach,"after Retreat, Retreat, Charge, Charge and Hold the commanding client played horn_rout "+rout+" time(s), horn_charge "+charges+", the chime "+chime+" (want 1, 1, 1: the throttled repeats play nothing) and a second player 5 blocks from him was sent "+listenerCues+" (want nothing)="+reach);
                    soundOk&=reach;soundWhy+="; the transport: the client played rout "+rout+", charge "+charges+", chime "+chime+" (want 1 each) and the second player was sent "+listenerCues+" (want nothing)="+reach;
                    report("p1-command-sound",soundOk,soundWhy); // reported here, not at stage 99: the client ends the suite the tick the stage turns 99, before a server tick could report
                    next(b,13);
                }
                case 13->{ // p1-retreat-arrive: a free group on Retreat to 20 blocks west; a frozen Risen on the way strikes them; another waits 5 blocks beyond the destination
                    freshInfantry(b,false);enemyHome(b);
                    flankers=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(FX-9,65,FZ));flankers.order="hold";flankers.yaw=-90;flank.clear();
                    waylayer=risen(b,FX-9,FZ+1.2,-90);greeter=risen(b,FX-25,FZ,-90);
                    dest=new Vec3d(FX-20,top(b,FX-20,FZ),FZ);order(b,"infantry","retreat",FX-20,FZ);
                    strikes=0;targetOnWay=false;runSeen=false;fought=false;arrivedAt=-1;foughtAt=-1;
                    camera(b,FX-14,FZ+14,6,-150,30);
                    next(b,14);
                }
                case 14->{
                    var ks=knights(b);if(ks.isEmpty()){report("p1-retreat-arrive",false,"no knight left");next(b,99);break;}
                    if(arrivedAt<0){
                        if(arrived(b)){arrivedAt=since(b);return;} // read first: on the tick it arrives a knight's beat may already take a target
                        if(since(b)%10==0)for(var k:ks)if(waylayer.mob.isAlive()&&k.mob.squaredDistanceTo(waylayer.mob)<2.5*2.5){blow(b,waylayer,k,"melee");strikes++;}
                        if(anyTarget(b))targetOnWay=true;
                        if(Tactics.stance(b,ks.get(0)).equals("run"))runSeen=true;
                        if(since(b)>1200){arriveOk=false;arriveWhy="the retreating group never arrived within 4 of its destination in 1200 ticks (centre "+String.format("%.1f",Math.sqrt(GravesAndGlory.Battle.horizontalDistance(centre(b),dest)))+" off)";report("p1-retreat-arrive",false,arriveWhy);next(b,99);}
                        return;
                    }
                    if(foughtAt<0&&anyTarget(b))foughtAt=since(b)-arrivedAt;
                    if(foughtAt<0&&since(b)-arrivedAt<=100)return;
                    boolean onWay=strikes>0&&!targetOnWay&&runSeen,after=foughtAt>=0&&foughtAt<=100&&Tactics.stance(b,ks.get(0)).equals("hold")&&b.assignment(ks.get(0)).order.equals("retreat");
                    arriveOk=onWay&&after;
                    arriveWhy="on the way a Risen struck them "+strikes+" times and no knight ever took a target="+!targetOnWay+" (stance run="+runSeen+")="+onWay+"; they arrived at +"+arrivedAt+" and "+(foughtAt<0?"never":foughtAt+" ticks later")+" a knight took a target (the Risen 5 blocks beyond, within Hold's 10), their stance now "+Tactics.stance(b,ks.get(0))+" (want hold) with the stored order still "+b.assignment(ks.get(0)).order+"="+after;
                    // the same for a Move order: to 10 blocks south of the fixture point, with a Risen waiting 8 blocks beyond it (inside Hold's 10, past arriveRadius)
                    for(var u:new ArrayList<>(flank))u.mob.discard();b.units.removeAll(flank);flank.clear();
                    greeter=risen(b,FX,FZ+18,-90);dest=new Vec3d(FX,top(b,FX,FZ+10),FZ+10);order(b,"infantry","move",FX,FZ+10);
                    for(var k:ks){k.mob.setTarget(null);k.lockedTarget=null;}
                    targetOnWay=false;arrivedAt=-1;foughtAt=-1;next(b,15);
                }
                case 15->{
                    var ks=knights(b);if(ks.isEmpty()){report("p1-retreat-arrive",false,arriveWhy+"; no knight left for the Move");next(b,99);break;}
                    if(arrivedAt<0){
                        if(arrived(b)){arrivedAt=since(b);return;}
                        if(since(b)>10&&anyTarget(b))targetOnWay=true;
                        if(since(b)>1200){report("p1-retreat-arrive",false,arriveWhy+"; the Move group never arrived within 4 of its destination in 1200 ticks");next(b,99);}
                        return;
                    }
                    if(foughtAt<0&&anyTarget(b))foughtAt=since(b)-arrivedAt;
                    if(foughtAt<0&&since(b)-arrivedAt<=100)return;
                    boolean move=!targetOnWay&&foughtAt>=0&&foughtAt<=100;arriveOk&=move;
                    arriveWhy+="; on a Move 10 blocks south no knight took a target on the way="+!targetOnWay+", they arrived at +"+arrivedAt+" and "+(foughtAt<0?"never":foughtAt+" ticks later")+" a knight took a target (the Risen 8 beyond, within Hold's 10)="+move;
                    pullAt=-1;pullMax=0;pullSamples=pullArrived=pullHold=pullTargets=0;next(b,17);
                }
                case 17->{ // the Hold fight pulls the group out: the Risen 8 beyond the spot, pinned and kept alive, draws the knights past arriveRadius of their spot; arrival stays latched (Squad.arrived) and they keep Hold and their targets instead of turning back into a passive Move that walks home
                    var ks=knights(b);if(ks.isEmpty()){report("p1-retreat-arrive",false,arriveWhy+"; no knight left for the pull");next(b,99);break;}
                    if(!greeter.mob.isAlive()){report("p1-retreat-arrive",false,arriveWhy+"; the pinned Risen died");next(b,99);break;}
                    place(b,greeter,FX,FZ+18,-90);greeter.mob.setHealth(greeter.mob.getMaxHealth());
                    double off=Math.sqrt(GravesAndGlory.Battle.horizontalDistance(centre(b),dest));
                    if(pullAt<0){
                        if(off>D.arriveRadius()+1)pullAt=since(b);
                        else if(since(b)>600){report("p1-retreat-arrive",false,arriveWhy+"; the Hold fight never pulled the centre past "+(D.arriveRadius()+1)+" of the spot in 600 ticks ("+String.format("%.1f",off)+" off now)");next(b,99);}
                        return;
                    }
                    pullMax=Math.max(pullMax,off);pullSamples++;if(arrived(b))pullArrived++;if(Tactics.stance(b,ks.get(0)).equals("hold"))pullHold++;if(anyTarget(b))pullTargets++;
                    if(since(b)-pullAt<60)return;
                    pullOk=pullArrived==pullSamples&&pullHold==pullSamples&&pullTargets*4>=pullSamples*3&&own.arrived.contains("infantry");
                    arriveWhy+="; the Hold fight pulled the centre "+String.format("%.1f",pullMax)+" from the spot: over "+pullSamples+" ticks past "+(D.arriveRadius()+1)+" arrived="+pullArrived+", stance hold="+pullHold+", a knight had a target in "+pullTargets+" (want all, all, three in four) and the latch holds="+own.arrived.contains("infantry")+"="+pullOk;
                    arriveOk&=pullOk;
                    // ranged and support on Retreat, 10 blocks west of where they hold: their slots stand 6.8 and 10.8 or more behind each spot (Battle.formation), so the centre never comes within arriveRadius of the spot itself; arrival is measured at the slots
                    dropFlankers(b);
                    if(Morale.members(b,own,"support").isEmpty()){ // the fixture's banner fields no support troop: two Sunweavers join the army, frozen, on the support group's Hold slots
                        for(int i=0;i<2;i++){var m=b.spawn(own,troop("mage"),20+i,new Vec3d(FX-8,top(b,FX-8,FZ+30),FZ+30));freeze(m);Vec3d at=b.formation(m);place(b,m,at.x,at.z,-90);}
                        Morale.fielded(b,own,"support");Houses.recompute(own);
                    }
                    rangedDest=new Vec3d(FX-18,top(b,FX-18,FZ+24),FZ+24);supportDest=new Vec3d(FX-18,top(b,FX-18,FZ+30),FZ+30);
                    order(b,"ranged","retreat",FX-18,FZ+24);order(b,"support","retreat",FX-18,FZ+30);
                    for(var u:b.units)if(u.squad==own&&!u.spec.group().equals("infantry")){u.mob.setTarget(null);u.lockedTarget=null;u.nextPath=0;}
                    var rs=Morale.members(b,own,"ranged");var ss=Morale.members(b,own,"support");
                    rangedStart=!rs.isEmpty()&&Tactics.arrived(b,rs.get(0),b.assignment(rs.get(0)));supportStart=!ss.isEmpty()&&Tactics.arrived(b,ss.get(0),b.assignment(ss.get(0)));
                    rangedLeg=null;supportLeg=null;settleAt=-1;next(b,18);
                }
                case 18->{
                    if(rangedLeg==null)rangedLeg=leg(b,"ranged",rangedDest);
                    if(supportLeg==null)supportLeg=leg(b,"support",supportDest);
                    if(rangedLeg==null||supportLeg==null){
                        if(since(b)>1200){report("p1-retreat-arrive",false,arriveWhy+"; ranged and support on Retreat 10 blocks west: "+(rangedLeg==null?"ranged never arrived in 1200 ticks":rangedLeg.why())+"; "+(supportLeg==null?"support never arrived in 1200 ticks":supportLeg.why()));next(b,99);}
                        return;
                    }
                    if(settleAt<0)settleAt=since(b);
                    if(since(b)-settleAt<100)return;
                    var rangedSettled=settled(b,"ranged",rangedDest);var supportSettled=settled(b,"support",supportDest);
                    boolean legs=rangedLeg.ok()&&supportLeg.ok()&&rangedSettled.ok()&&supportSettled.ok()&&!rangedStart&&!supportStart;
                    arriveOk&=legs;
                    report("p1-retreat-arrive",arriveOk,arriveWhy+"; ranged and support on Retreat 10 blocks west (neither counted as arrived when ordered="+(!rangedStart&&!supportStart)+"): "+rangedLeg.why()+"; "+supportLeg.why()+"; 100 ticks on: "+rangedSettled.why()+"; "+supportSettled.why()+"="+legs);
                    next(b,99);
                }
                case 99->{}
                default->{}
            }
        }
    }

    /** Task 3: forward reinforcement from a held point, the walk measured before and after, and the fallbacks to the barracks. Stages run on the server tick. */
    static final class ReinforceChecks implements Suite,ServerWatch {
        static final Spec.EconomyRow E=Spec.ECONOMY[0];
        static final int TIMEOUT=2400; // ticks a measured walk may take
        static final String[] PROBES={"p1-forward-contested","p1-forward-enemy-near","p1-forward-flipped","p1-forward-fresh","p1-forward-enemy-spawn"};
        volatile int stage;volatile String wantShot;volatile boolean finished;
        volatile UUID player;long stageAt;int shotDelay;
        final Spec.TroopsRow knight=troop("knight");
        GravesAndGlory.Squad own;GravesAndGlory.Capture point;GravesAndGlory.Anchor anchor;
        Set<GravesAndGlory.Unit> known=new HashSet<>();GravesAndGlory.Unit trained,foe;long appeared;Vec3d spawnAt;
        double before,after;boolean shotAsked,control,heldOk;int probe;
        GravesAndGlory.Unit garrison;long captured=-1;
        Spec.TroopsRow support;double fullOne,fullTwo,fullThree,fullFour;

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
            return b.reinforcementAnchors(0).contains(GravesAndGlory.Anchor.of(point));
        }
        void drop(GravesAndGlory.Battle b){if(trained!=null){trained.mob.discard();b.units.remove(trained);trained=null;}}
        /** One frozen Dusk revenant at (x,z) in an ad hoc hold squad, the enemy of the enemy probes; ideal() removes it. */
        void foeAt(GravesAndGlory.Battle b,double x,double z){var dusk=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(x,65,z));dusk.order="hold";dusk.yaw=90;
            foe=b.spawn(dusk,troop("revenant"),30,new Vec3d(x,top(b,x,z),z));freeze(foe);foe.deployed=foe.mob.getPos();}
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
                        System.out.println("[p1] reinforce after: appeared at "+spawnAt+", "+String.format("%.1f",flat(spawnAt,point.spec.x(),point.spec.z()))+" blocks from the point centre, anchor "+(anchor==null?"none":anchor.name()));}
                    long t=walked(b);
                    if(t>=0||b.time-appeared>=TIMEOUT){after=(t>=0?t:TIMEOUT)/20.0;
                        System.out.println("[p1] reinforce-walk before="+String.format("%.1f",before)+"s after="+String.format("%.1f",after)+"s");
                        report("p1-reinforce-walk",t>=0&&GravesAndGlory.Anchor.of(point).equals(anchor)&&after<before&&after<=20,"walk to the formation point "+String.format("%.1f",before)+" s from the barracks, "+String.format("%.1f",after)+" s from "+(anchor==null?"no anchor":anchor.name())+" (needs after < before and <= 20 s)");
                        drop(b);next(b,7);}
                }
                case 7->{ // one disturbance per probe on an otherwise ideal point; the troop must deploy at the barracks
                    if(probe>=PROBES.length){next(b,8);break;}
                    control=ideal(b);
                    switch(probe){
                        case 0->point.contested=true;
                        case 1->foeAt(b,point.spec.x(),point.spec.z()+10);
                        case 2->point.owner=1;
                        case 3->point.ownedSince=b.time;
                        default->{ // the enemy stands beyond the safe radius of the point centre but inside it of where the troops appear (forwardSpawnOffset toward the base)
                            Vec3d at=b.deployCentre(0,point);double dx=at.x-point.spec.x(),dz=at.z-point.spec.z(),len=Math.hypot(dx,dz),t=E.forwardSafeRadius()-E.forwardSpawnOffset()/2;
                            foeAt(b,at.x+dx/len*t,at.z+dz/len*t);}
                    }
                    if(!buy(b)){fail(b,PROBES[probe],"no living knight to disband");break;}
                    next(b,71);
                }
                case 71->{
                    if(!arrived(b)){if(since(b)>100){report(PROBES[probe],false,"the bought knight never appeared");probe++;next(b,7);}break;}
                    Vec3d home=GravesAndGlory.Battle.base(0);double fromBase=flat(spawnAt,home.x,home.z),fromPoint=flat(spawnAt,point.spec.x(),point.spec.z());
                    Vec3d at=b.deployCentre(0,point);String enemy=foe==null?"":", the enemy "+String.format("%.1f",flat(foe.mob.getPos(),point.spec.x(),point.spec.z()))+" blocks from the point centre and "+String.format("%.1f",flat(foe.mob.getPos(),at.x,at.z))+" from where troops appear, safe radius "+E.forwardSafeRadius();
                    report(PROBES[probe],control&&fromBase<=30,"point an anchor before the change="+control+"; knight appeared "+String.format("%.1f",fromBase)+" blocks from the barracks spot and "+String.format("%.1f",fromPoint)+" from the point (owner "+point.owner+", contested "+point.contested+enemy+")");
                    drop(b);probe++;next(b,7);
                }
                case 8->{ // p1-forward-message: the deploy decision names the point, or the barracks. Since UI Task 3 the player's deploy
                    // notice is the feed's "Dawn Knight deploying" line (p1-ui-feed-deploy), so the old notice text, deployNote, is retired (UI polish M15)
                    boolean anchorNow=ideal(b);var fwd=b.deploy(own,0);b.forwardReinforcement=false;var home=b.deploy(own,0);b.forwardReinforcement=true;Vec3d base=GravesAndGlory.Battle.base(0);
                    report("p1-forward-message",anchorNow&&GravesAndGlory.Anchor.of(point).equals(anchor)&&GravesAndGlory.Anchor.of(point).equals(fwd.anchor())&&home.anchor()==null&&flat(home.at(),base.x,base.z)<=10,"the anchored spawn's decision named "+(anchor==null?"no point":anchor.name())+" (the middle point is "+point.spec.name()+"); with the point held again a slot-0 decision deploys at "+(fwd.anchor()==null?"the barracks":fwd.anchor().name())+", and with forward reinforcement off at "+(home.anchor()==null?"the barracks":home.anchor().name())+", "+String.format("%.1f",flat(home.at(),base.x,base.z))+" blocks from the barracks spot; the player reads the feed's \""+FeedLines.deploying(1,knight.name())+"\" line");
                    // p1-forward-held, nothing faked: a lone Dawn knight on a neutral point captures it through captureTick, which stamps ownedSince
                    point.owner=-1;point.challenger=-1;point.progress=0;point.contested=false;point.ownedSince=0;Arena.flag(b.world,point.spec,-1);
                    var holder=new GravesAndGlory.Squad(UUID.randomUUID(),0,new Vec3d(point.spec.x(),65,point.spec.z()));holder.order="hold";holder.yaw=-90;
                    garrison=b.spawn(holder,knight,31,new Vec3d(point.spec.x(),top(b,point.spec.x(),point.spec.z()),point.spec.z()));freeze(garrison);garrison.deployed=garrison.mob.getPos();next(b,81);
                }
                case 81->{
                    if(captured<0){
                        if(point.owner==0){captured=b.time;heldOk=point.ownedSince==b.time&&!b.reinforcementAnchors(0).contains(GravesAndGlory.Anchor.of(point));}
                        else if(since(b)>400){report("p1-forward-held",false,"the garrison never captured the point in 400 ticks");disband(b,garrison);next(b,82);}
                        break;
                    }
                    long held=b.time-captured,need=E.forwardHoldSeconds()*20L;
                    if(held==need-1)heldOk&=!b.reinforcementAnchors(0).contains(GravesAndGlory.Anchor.of(point));
                    if(held==need){boolean anchorNow=b.reinforcementAnchors(0).contains(GravesAndGlory.Anchor.of(point));
                        report("p1-forward-held",heldOk&&anchorNow,"captured at tick "+captured+" (ownedSince "+point.ownedSince+"); not an anchor for the first "+need+" ticks="+heldOk+", an anchor at "+held+" ticks held="+anchorNow);
                        disband(b,garrison);next(b,82);}
                }
                case 82->{
                    // the Task 2 review fix: a group made only of trained troops still gets a health ceiling
                    support=troop("mage"); // the Gilded Order's support troop (Phase 2: troops belong to Houses, not sides)
                    // Phase 1's Dawn line ended with two support troops in slots 18 and 19; the starter banner has none there, so those two slots are freed for the trained ones.
                    for(var u:b.units)if(u.squad==own&&(u.spec.group().equals("support")||u.slot>=18))u.mob.discard();b.units.removeIf(u->!u.mob.isAlive()||u.mob.isRemoved());
                    own.fullHealth.remove("support");own.training.addLast(new GravesAndGlory.Training(support,b.time));next(b,9);
                }
                case 9->{if(supporters(b)==1){fullOne=fullHealth();own.training.addLast(new GravesAndGlory.Training(support,b.time));next(b,10);}else if(since(b)>100){fail(b,"p1-trained-fullhealth","the first support troop never appeared");}}
                case 10->{if(supporters(b)==2){fullTwo=fullHealth();
                    for(var u:b.units)if(u.squad==own&&u.spec==support&&u.mob.isAlive()){u.mob.kill();break;}b.units.removeIf(u->!u.mob.isAlive()||u.mob.isRemoved());
                    own.training.addLast(new GravesAndGlory.Training(support,b.time));next(b,11);}else if(since(b)>100){fail(b,"p1-trained-fullhealth","the second support troop never appeared");}}
                case 11->{if(supporters(b)==2){fullThree=fullHealth(); // then both fall and one is trained: the ceiling keeps two troops (adding would give four, the living sum one)
                    for(var u:b.units)if(u.squad==own&&u.spec==support&&u.mob.isAlive())u.mob.kill();b.units.removeIf(u->!u.mob.isAlive()||u.mob.isRemoved());
                    own.training.addLast(new GravesAndGlory.Training(support,b.time));next(b,12);}else if(since(b)>100){fail(b,"p1-trained-fullhealth","the replacement support troop never appeared");}}
                case 12->{if(supporters(b)==1){fullFour=fullHealth();double one=support.health()*b.campaign.health(own,support);
                    report("p1-trained-fullhealth",Math.abs(fullOne-one)<1e-6&&Math.abs(fullTwo-2*one)<1e-6&&Math.abs(fullThree-2*one)<1e-6&&Math.abs(fullFour-2*one)<1e-6,"support ceiling after 1, 2, a replaced troop, and 2 lost then 1 trained: "+fullOne+", "+fullTwo+", "+fullThree+", "+fullFour+" (one troop is "+one+")");
                    next(b,99);}else if(since(b)>100){fail(b,"p1-trained-fullhealth","the support troop trained after both fell never appeared");}}
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
        String farOrder="";boolean shieldFormation;double gap;
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
        /** A synthetic bot army (not in b.squads, so only the fixture decides for it): the team's Normal bot banner (Phase 2: gilded_normal for team 0, graveborn_normal for team 1), frozen, each unit at its banner position, in rows behind (cx,cz). */
        GravesAndGlory.Squad army(GravesAndGlory.Battle b,int team,String tier,double cx,double cz){
            var s=new GravesAndGlory.Squad(UUID.randomUUID(),team,new Vec3d(cx,top(b,cx,cz),cz));
            s.bot=true;s.tier=tier;s.lane=1;s.order="hold";s.yaw=team==0?-90:90;s.banner=Campaign.bannerRow(team==0?"gilded_normal":"graveborn_normal");
            for(int slot=0;slot<s.banner.size();slot++){var spec=Campaign.troop(s.banner.get(slot));if(spec!=null)freeze(b.spawn(s,spec,slot,new Vec3d(cx,top(b,cx,cz),cz)));}
            place(b,s,cx,cz);return s;
        }
        /** Shield troops (abilities mechanic shield: the Order Squire and the Templar of gilded_normal) of a squad, alive. */
        static List<GravesAndGlory.Unit> shields(GravesAndGlory.Battle b,GravesAndGlory.Squad s){return b.units.stream().filter(u->u.squad==s&&u.tags.contains("shield")&&u.mob.isAlive()).toList();}
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
            if(stage==4){ // the War Table's Field page after difficulty:hard (UI Task 7: the tiers are three tags, the current one lit), then a real press of Easy
                if(homeAt==-1){homeAt=tick;CampScreen.open("field");}
                else if(tick==homeAt+25){
                    var s=c.currentScreen instanceof CampScreen cs&&cs.page.equals("field")?cs.pages:null;var easy=s==null?null:s.button("bots-easy");
                    homeOk=s!=null&&"Hard".equals(WarTablePages.texts().get("bots-current"))&&easy!=null&&easy.active;
                    homeWhy=s==null?"no Field page":"the bots read \""+WarTablePages.texts().get("bots-current")+"\", Easy "+(easy!=null&&easy.active?"taking a press":"dull");
                    wantShot="bots-difficulty";
                }
                else if(tick==homeAt+30){var s=c.currentScreen instanceof CampScreen cs&&cs.page.equals("field")?cs.pages:null;var easy=s==null?null:s.button("bots-easy");if(easy!=null)easy.onPress();homeAt=-2;server(c,b->next(b,5));}
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
                    report("p1-bots-tiers",lobbyOk&&commandOk&&homeOk&&buttonOk&&tiersOk&&refusedOk,"lobby: "+lobbyWhy+"; difficulty:hard set it="+commandOk+"; the Field page: "+homeWhy+"; button press reached the server="+buttonOk+"; solo pad 3v3: "+tiersWhy+"; difficulty:easy refused while running (still "+c.difficulty+")="+refusedOk);
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
                    var shipped=b.campaign.tier(hard);var row=new Spec.BotCommandRow(shipped.id(),shipped.retreatShare(),shipped.rallyShare(),shipped.chargeRatio(),shipped.decisionTicks(),true,shipped.defenderWeight(),shipped.shieldwallVsArchers(),shipped.archerThreatCount(),shipped.archerThreatRange(),shipped.chargeDistance(),shipped.gearMaxTier(),shipped.gearReserveTroops(),shipped.gearPriority()); // the flanking rule stays tested; the shipped Hard row has it off (Kort, 2026-10-18)
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
                        long up=shields(b,hard).stream().filter(u->u.mob.isUsingItem()).count(),knights=shields(b,hard).size();
                        Vec3d at=centre(b,hard);
                        report("p1-bots-shieldwall",shieldFormation&&up>=6,"Hard infantry formation shieldwall (Normal left it alone)="+shieldFormation+"; "+up+" of "+knights+" shield troops using their shield "+since(b)+" ticks later; six Dusk archers "+String.format("%.0f",gap)+" blocks in front of the front rank when ordered ("+String.format("%.0f",Math.abs(foes.get(0).mob.getX()-at.x))+" from the army centre now), no Dusk melee");
                        wantShot="bots-shieldwall";
                    }
                    if(shotAsked&&wantShot==null)next(b,13); // the archers stay, so the wall keeps bracing for p1-bots-blight
                }
                case 13->{ // p1-bots-blight: a frontal blow on a knight braced in shieldwall is blocked; a Gravecaller burst on the same wall is not
                    var knights=shields(b,hard);
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
                    hard.chargedAt.clear();int heardBefore=BattleEffects.heard.size();
                    b.campaign.botDecide(hard,b.campaign.tier(hard));farOrder=hard.order;Long chargeFar=hard.chargedAt.get("infantry");
                    spawnFoes(b,"revenant",8,at.x+28,at.z);int seenMid=see(b); // seen, and the army outnumbers them, but they are outside the 18-block gate
                    b.campaign.botDecide(hard,b.campaign.tier(hard));String midOrder=hard.order;Long chargeMid=hard.chargedAt.get("infantry");
                    spawnFoes(b,"revenant",8,at.x+15,at.z);int seenNear=see(b);
                    b.campaign.botDecide(hard,b.campaign.tier(hard));Long chargeNear=hard.chargedAt.get("infantry");
                    boolean silent=BattleEffects.heard.size()==heardBefore; // true by construction: Battle.markCharge never calls BattleEffects.feedback, and Campaign.botDecide calls only markCharge; the grep of botDecide is the proof that a bot's charge plays nothing
                    report("p1-bots-charge-timing",farOrder.equals("attack")&&midOrder.equals("attack")&&seenMid==8&&chargeFar==null&&chargeMid==null&&hard.order.equals("charge")&&seenNear==8&&chargeNear!=null&&chargeNear==b.time&&silent,"Hard army with eight Dusk revenants 60 blocks off ("+seenFar+" remembered): order "+farOrder+"; 28 blocks off ("+seenMid+" remembered, outside the 18-block gate): order "+midOrder+"; 15 blocks off ("+seenNear+" remembered): order "+hard.order+"; the charge stamped (Squad.chargedAt) "+chargeFar+" -> "+chargeMid+" -> "+(chargeNear==null?"null":"now")+"; feedback sounds "+heardBefore+" -> "+BattleEffects.heard.size()+" (want none: true by construction, see the comment)="+silent);
                    for(var u:foes)u.mob.discard();b.units.removeAll(foes);foes.clear();
                    for(var u:b.units)if(u.squad==hard&&u.slot>=3)u.mob.discard(); // its first three banner slots left (two troops of nineteen): the army retreats
                    hard.yaw=0;b.campaign.botDecide(hard,b.campaign.tier(hard));long left=b.units.stream().filter(u->u.squad==hard&&u.mob.isAlive()).count();
                    report("p1-bots-retreat-facing",hard.order.equals("retreat")&&hard.yaw==-90,"Hard army cut down to "+left+" of "+RosterMath.units(hard.banner,RosterMath.Catalog.fromSpec())+": order "+hard.order+", facing "+hard.yaw+" (Dawn faces -90 after every decision, retreat included)");
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
        static final int ROLL=Spec.RECRUITMENT[0].rollCost(),START_TICKS=900; // the barracks lesson pays one recruitment roll
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
        void fresh(GravesAndGlory.Battle b){b.campaign.profiles.put(player,new Profile());b.campaign.save(player);}
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
        /** Right tapped twice, as the keys send it (pressed and let go: the table hears a held key once): the War Table raises the
         *  Campaign sheet, whichever sheet was up. */
        static void raiseCampaign(MinecraftClient c){for(int i=0;i<2;i++){if(c.currentScreen!=null)c.currentScreen.keyPressed(GLFW.GLFW_KEY_RIGHT,0,0);if(c.currentScreen!=null)c.currentScreen.keyReleased(GLFW.GLFW_KEY_RIGHT,0,0);}}
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
                // what the lesson note drew this frame against the full lesson copy: nothing cut, every line inside the note, measured in
                // the small style it wraps in, the first after its LESSON k/4 tag; and at most two lines (UI Task 8c, the lighter note)
                String full=GravesAndGloryClient.lessonText(now);var drawn=GravesAndGloryClient.lessonDrawn;String joined=String.join(" ",drawn);
                int firstW=GravesAndGloryClient.LESSON_TEXT_WIDTH-c.textRenderer.getWidth(DarkUi.small(BattleHud.lessonTag(now)))-UiMath.LESSON_TAG_GAP;
                boolean whole=joined.replace(" ","").equals(full.replace(" ","")),fits=true,brief=drawn.size()<=2;
                for(int i=0;i<drawn.size();i++)fits&=c.textRenderer.getWidth(DarkUi.small(drawn.get(i)))<=(i==0?firstW:GravesAndGloryClient.LESSON_TEXT_WIDTH);
                lessonWhole[now]=whole&&fits&&brief;copyWhy+=(copyWhy.isEmpty()?"":"; ")+"lesson "+(now+1)+": "+drawn.size()+" line(s) (at most two: "+brief+"), whole="+whole+", inside "+firstW+" px after the tag and "+GravesAndGloryClient.LESSON_TEXT_WIDTH+" px below it="+fits+(whole?"":", drawn ends \""+joined.substring(Math.max(0,joined.length()-16))+"\"");
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
                // lesson 4 names the use key as bound (drawn whole above): the first three mouse buttons by the key bound, not its name,
                // as LMB, RMB or MMB (RMB by default, UI polish M6 and Task 8a), any other key as the game names it
                var useKey=net.minecraft.client.util.InputUtil.fromTranslationKey(c.options.useKey.getBoundKeyTranslationKey());
                String use=useKey.getCategory()==net.minecraft.client.util.InputUtil.Type.MOUSE&&useKey.getCode()>=0&&useKey.getCode()<=2?List.of("LMB","RMB","MMB").get(useKey.getCode()):c.options.useKey.getBoundKeyLocalizedText().getString();boolean named=GravesAndGloryClient.lessonText(3).contains(use);
                report("p1-tutorial-lessons",first.taught()&&shots==4&&whole==4&&named,first.why()+"; "+shots+" of 4 lesson screenshots; "+whole+" of 4 lessons drawn whole inside the lesson note in at most two lines ("+copyWhy+"); lesson 4 names the use key \""+use+"\": "+named);
                server(c,b->next(b,7));
            }
            if(stage==8){ // the camp home after the victory: all cleared, the Campaign sheet rides to the last battle, battle 1; pressing it is the replay
                if(afterAt<0){if(c.currentScreen instanceof CampScreen s&&s.page.equals("results")||++waited>=60){afterAt=tick;CampScreen.open("home");}}
                else if(tick==afterAt+20)raiseCampaign(c);
                else if(tick==afterAt+25){String ride=march(c);
                    report("p1-tutorial-clear",clearOk&&CampaignClient.profile.campaignCleared==1&&"Ride to The Training Field".equals(beforeRide)&&("Ride to "+Spec.CAMPAIGNBATTLES[1].name()).equals(ride),clearWhy+"; client campaignCleared "+CampaignClient.profile.campaignCleared+"; camp home before the battle: "+beforeWhy+"; after the victory: "+describe(ride));
                    shot(c,"tutorial-camp-after");
                }
                else if(tick==afterAt+30){var ride=c.currentScreen instanceof CampScreen s&&s.home!=null?s.home.button("ride"):null;
                    // plan 2C.3: once battle 1 is won the Ride plate leads on to battle 2, so the replay asks for battle 1 by id, as the march page will (2C.5)
                    if(ride!=null&&march(c)!=null&&c.currentScreen instanceof CampScreen s&&s.action("campaign:"+Spec.CAMPAIGNBATTLES[0].id()))server(c,b->next(b,9));
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
        /** p1-kingsfield-stale-points cannot pass by never running: without its 30 qualifying client ticks before the suite ends, it fails. */
        public void cleanup(MinecraftClient c){if(staleTicks<30)report("p1-kingsfield-stale-points",false,"only "+staleTicks+" of 30 client ticks drawn against three-entry point arrays before the suite ended (stage "+stage+")");}
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
        /** p1-report-stale-kill (final review, minor 2): a death with no attacker (here /kill, standing for a fall or lava) credits the army whose blow last hurt the victim only inside Projectiles.CREDIT_WINDOW_TICKS. A blow one tick past the window earns nothing; one on its last tick earns the kill and the kill gold. */
        void staleKill(GravesAndGlory.Battle b){
            var foes=b.units.stream().filter(u->u.squad.team!=own.team&&u.mob.isAlive()).limit(2).toList();
            if(foes.size()<2){report("p1-report-stale-kill",false,"fewer than two living enemy troops to test with ("+foes.size()+")");return;}
            int window=Projectiles.CREDIT_WINDOW_TICKS,kills0=own.stats.kills,gold0=own.gold;
            var stale=foes.get(0);stale.lastHitBy=own;stale.lastHitAt=b.time-window-1;stale.mob.kill();
            int kills1=own.stats.kills,gold1=own.gold;
            var fresh=foes.get(1);fresh.lastHitBy=own;fresh.lastHitAt=b.time-window;fresh.mob.kill();
            report("p1-report-stale-kill",!stale.mob.isAlive()&&!fresh.mob.isAlive()&&kills1==kills0&&gold1==gold0&&own.stats.kills==kills1+1&&own.gold==gold1+E.killGold(),
                "window "+window+" ticks; an enemy killed with no attacker "+(window+1)+" ticks after the commander's army last hit him: kills "+kills0+" -> "+kills1+", gold "+gold0+" -> "+gold1+"; one killed "+window+" ticks after: kills "+kills1+" -> "+own.stats.kills+", gold "+gold1+" -> "+own.gold+" (kill gold "+E.killGold()+")");
        }
        /** The camp snapshot at its realistic worst (256 match ids held, every claim and title, the 8-row report) fits the client's read limit as sent today, carries no match ids, and the saved profile still refuses a repeat id; then each fallback of Campaign.fitted stays inside the limit. Everything is put back before this server tick ends, so no state packet carries the fixture. */
        void snapshotChecks(GravesAndGlory.Battle b){
            var c=b.campaign;var p=sp(b);var st=c.profile(player).progression;var grants=new LinkedHashSet<>(st.matchGrants);var claims=new HashSet<>(st.claims);var titles=new LinkedHashSet<>(st.titles);var realReport=c.results.get(player);String notice=c.notices.get(player);
            try{
                while(st.matchGrants.size()<256)st.matchGrants.add(UUID.randomUUID().toString());
                for(int i=0;i<3;i++)st.claims.add("daily:"+i);st.claims.add("weekly");for(int t=1;t<=Progression.TIERS;t++)st.claims.add(Progression.SEASON+":"+t);st.titles.addAll(Arrays.asList(Progression.TITLES));
                var four=crowded(realReport);c.results.put(player,four);String worst=c.snapshot(p);
                var saved=Campaign.JSON.fromJson(Campaign.JSON.toJson(c.profile(player)),Profile.class).progression;var again=Progression.match(saved,st.matchGrants.iterator().next(),true,true);
                String key=Progression.State.MATCH_GRANTS;boolean named=Campaign.JSON.toJsonTree(st).getAsJsonObject().has(key);
                boolean pretty=worst.contains("\n"),ids=worst.contains(key)||st.matchGrants.stream().anyMatch(worst::contains),kept=parse(worst).getAsJsonObject("result").has("id");
                report("p1-report-snapshot",worst.length()<=Campaign.SNAPSHOT_LIMIT&&pretty&&kept&&named&&!ids&&saved.matchGrants.size()==256&&again.xp()==0,
                    "worst camp snapshot ("+st.matchGrants.size()+" match ids held by the server, "+st.claims.size()+" claims, "+st.titles.size()+" titles, an 8-row report on "+b.points.size()+" points) is "+worst.length()+" of "+Campaign.SNAPSHOT_LIMIT+" characters, sent pretty="+pretty+" with the report="+kept+", carries match ids="+ids+" (key "+key+" names the saved field="+named+"); the saved profile keeps "+saved.matchGrants.size()+" ids and pays a repeat id "+again.xp()+" XP");
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
                    staleKill(b);next(b,7);
                }}
                case 7->{ // the round ends: the commander's report as the server keeps it for his client
                    int seated=(int)b.squads.values().stream().filter(s->s.seated).count();String side=own.team==0?"dawn":"dusk";
                    b.campaign.finish(own.team);var r=b.campaign.results.get(player);var squads=rows(r,"squads");var points=rows(r,"points");
                    JsonElement me=null;boolean names=true;for(var e:squads){if(text(e,"you").equals("true"))me=e;if(text(e,"bot").equals("true"))names&=text(e,"name").equals(text(e,"tier")+" bot");}
                    boolean ownFirst=squads.size()>0&&number(squads.get(0),"team")==own.team;
                    report("p1-report-fields",squads.size()==seated&&me!=null&&number(me,"kills")>=2&&number(me,"lost")>=1&&number(me,"gold")>=E.passiveGold()&&number(me,"objective")>=2&&ownFirst&&names,
                        squads.size()+" squad rows for "+seated+" seated armies, own team first="+ownFirst+", bot rows named \"<tier> bot\"="+names+"; rows "+squads+" (the commander's row needs kills>=2, lost>=1, gold>="+E.passiveGold()+", objective>=2); the snapshot is "+b.campaign.snapshot(sp(b)).length()+" of the client's "+Campaign.SNAPSHOT_LIMIT+" characters");
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

        /** p1-ui-atlas: every atlas.json entry's PNG opens through the resource manager at the entry's size and UiTex read the entry alike; hud/card-sel is 181x68 at 1, 1; no name holds a mockup-only pt- texture and no PNG under textures/gui is missing from the atlas (Phase 2's House glyphs under house/ are not the atlas's: tools/build_ui_art.py FOREIGN). */
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
                if(!textures.has(n)&&!n.startsWith("house/"))bad.add(id+" is not in the atlas");
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
     *  frozen Dawn knight and Dusk revenant share point B (contested); A is Dawn's and C is Dusk's. The fixture states its own
     *  banner (HUD_BANNER: ten knights, six archers and two Sunweavers by troop id), so the counts do not move with the starter army;
     *  three knights and a Sunweaver fall first, so the cards read 7/10, 6/6 and 1/2. Fixtures run on the server tick; every check reads what
     *  BattleHud reports the last frame drew. Kingsfield (map 4, five points) comes last. Since UI Task 8c the selected group keeps
     *  its ledger card and every other group hangs as a chip; the window sweep runs at each HUD size (HudSize). */
    static final class UiHudChecks implements Suite,ServerWatch,HudWatch {
        static final double AX=-220,AZ=0; // open ground on the Dawn road, 63 blocks out from the live Dawn base at x=-283
        /** The column with ranged selected at 960x540 GUI 1 (UI Task 8c): the infantry's chip in its loss flash, ranged's ledger card,
         *  support's chip at its last stand, stacked 4 px apart up from the column's foot at y 522 (UiMathCheck pins the same boxes). */
        static final List<BattleHud.CardDraw> CARDS=List.of(new BattleHud.CardDraw("infantry","FLASH",false,24,416,176,18),new BattleHud.CardDraw("ranged","SELECTED",true,24,438,176,62),new BattleHud.CardDraw("support","LASTSTAND",false,24,504,176,18));
        /** The banner the fixture fields, whatever the starter army is: a banner id or "troop*count" parts, as Phase2Checks.banner reads them
         *  (^ is the second seat of the Sunweaver before it): 2 + 2 seats of Sunweaver, ten Order Squires and six Order Archers fill the
         *  20 seats of rules.squadSize. Infantry 10, ranged 6, support 2 before the fixture's deaths. */
        static final String HUD_BANNER="mage,^,mage,^,knight*10,archer*6";
        static final int INFANTRY=10,RANGED=6,SUPPORT=2,KILLED_INFANTRY=3,KILLED_SUPPORT=1;
        static final List<BattleHud.SealDraw> SEALS=List.of(new BattleHud.SealDraw("hud/seal-dawn",439,41),new BattleHud.SealDraw("hud/seal-contested",467,40),new BattleHud.SealDraw("hud/seal-dusk",495,41));
        /** Width, height, GUI scale, 1 for a borderless window (the 1080 windows: see window(c,w,h,gui,bare)); the battle HUD's scale
         *  there at each size, Compact, Standard and Large (UI Task 8c, UiMath.hudScale: Compact at the kit scale on every window,
         *  Standard and Large a step up where the whole HUD fits beside vanilla's bars, Large no further here; UiMathCheck pins the same
         *  numbers); then the chat lines p1-ui-chat-lift wants over the Compact HUD, closed and open, in four passes, worked out from the
         *  layout (UiMathCheck pins the same numbers): A, the lesson note and the respawn card with the hint layer down; B, the lesson
         *  with the hint layer up (a round's first seconds: the hint slip) and the commander up; C, a battle with neither, the layer
         *  down (the default battle state, the footprint's); D, a battle's first seconds, the layer up (the 1080p GUI 4 frame of the
         *  Task 8a review's Minor 4). Every frame has a feed line (its Important 2). Closed, the chat's lowest line keeps 4 px clear of
         *  vanilla's 15 px crosshair: below it where there is room (one line at 854x480 GUI 2 in B and D, the hint slip above the chips
         *  leaving one line between it and the crosshair), else above it. Open, the crosshair does not count (UI polish round 2). At
         *  Standard and Large the chat is held to the same rules, not to numbers. Last, the HUD's scale under Task 8a (the footprint's
         *  baseline) and 1 for the brief's five footprint windows. */
        static final int[][] WINDOWS={{960,540,1,0,1,1,1,10,20,10,20,10,20,10,20,1,0},{854,480,2,0,1,1,1,4,6,1,9,4,14,1,12,1,1},{1280,720,3,0,1,1,1,6,8,6,16,6,18,6,17,1,1},{1920,1080,2,1,2,3,3,10,20,10,20,10,20,10,20,2,1},{1920,1080,4,1,2,3,3,7,9,5,15,7,19,5,17,2,1},{1280,720,2,0,1,2,2,10,14,10,20,10,20,10,20,2,0},{1366,768,2,0,1,2,2,10,15,10,20,10,20,10,20,2,1}};
        /** Each window's screenshot; pass B adds -hints, C -battle and D -battle-hints, and a size above Compact -standard or -large. */
        static final String[] SHOTS={"ui-hud-chat","ui-hud-small","ui-hud-1280","ui-hud-1080-gui2","ui-hud-1080-gui4","ui-hud-720-gui2","ui-hud-1366"},PASS_SHOT={"","-hints","-battle","-battle-hints"},SIZE_SHOT={"","-standard","-large"};
        /** The footprint's baseline (UI Task 8c, run ui-8c-base-211317): in the default battle state the HUD before this task drew two
         *  48 px cards, the selected army's 62 px card and the 172 x 74 feed slip, 40536 canvas px at Task 8a's scale. */
        static final int BASELINE_AREA=40536;
        /** A battle's chat, twelve lines so the closed chat fills every line it shows. */
        static final List<String> CHAT=List.of("<Ansel> hold the ford until the archers are up","<Brannoc> on my way","<Ansel> Dusk knights at the mill","<Corwen> my archers are on the ridge","<Brannoc> taking the crossing","<Ansel> watch the left, revenants coming","<Corwen> need help at the shrine","<Brannoc> holding","<Ansel> good, push when the horn sounds","<Corwen> banner down, back in 12","<Brannoc> gg so far","<Ansel> last push, all to the crossing");
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
        // the window sweep (UI Tasks 8a and 8c, client thread): the HUD size (HudSize), the pass (0 A, 1 B, 2 C, 3 D), ticks the
        // pass's state has held; the frames p1-ui-small-window and p1-ui-hud-layout measured, p1-ui-hud-scale's windows, the
        // footprint's five, and the chat's lines frame by frame
        int size,pass,settled,scaleSeen,smallSeen,layoutSeen,footSeen;boolean scaleOk=true,layoutOk=true,footOk=true;String scaleWhy="",layoutWhy="",footWhy="";StringBuilder chatLines=new StringBuilder();
        // client thread, every HUD frame: the food draws the mixin skipped in that frame, watched during the hunger run and while the
        // five-seal battlefield builds
        int lastSkipped,hungerFrames,hungerFull,hungerLow,buildFrames,buildFull,buildLow,buildCloses;boolean watchHunger,watchBuild;String hungerOdd="",buildOdd="";

        /** keepLesson pins a lesson the server does not know: every STATE packet (each 10 ticks) puts the client's step back to the
         *  server's -1, and a frame drawn before the next client tick showed no lesson card, so the chat lifted over it the frame
         *  after (run ui-8a-green-192612: the open chat at 1920x1080 GUI 4 measured 10 lines past a card that frame lacked). The pin
         *  is also set as each frame starts, before the HUD draws. In play the server sends the step itself, so nothing flickers. */
        UiHudChecks(){net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.START.register(ctx->{if(current==this&&keepLesson)CampaignClient.tutorial=lessonStep;});}
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
        /** A GUI box in the battle HUD's canvas px (UiCanvas.hudK), rounded outward. */
        static int[] canvas(MinecraftClient c,int x,int y,int w,int h){
            double f=c.getWindow().getScaleFactor()/UiCanvas.hudK();
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
            var p=sp(b);if(p==null||!Phase2Checks.skirmishBanners(b,p,0,HUD_BANNER,null))return;
            var own=b.squads.get(player);quiet(b,own);float yaw=-90;
            own.order="hold";own.formation="block";own.target=new Vec3d(AX,top(b,AX,AZ),AZ);own.yaw=yaw;
            own.assignments.put("infantry",new GravesAndGlory.Assignment("attack","shieldwall",new Vec3d(AX,top(b,AX,AZ),AZ),yaw));
            own.assignments.put("ranged",new GravesAndGlory.Assignment("hold","line",new Vec3d(AX-9,top(b,AX-9,AZ),AZ),yaw));
            own.assignments.put("support",new GravesAndGlory.Assignment("follow","block",new Vec3d(AX-16,top(b,AX-16,AZ+9),AZ+9),yaw));
            for(var u:b.units)if(u.squad==own){Vec3d at=b.formation(u);MoraleChecks.place(b,u,at.x,at.z,yaw);freeze(u);}
            kill(b,own,"infantry",KILLED_INFANTRY);kill(b,own,"support",KILLED_SUPPORT);
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
            if(own!=null&&routSupport&&b.units.stream().anyMatch(u->u.squad==own&&u.spec.group().equals("support")&&u.mob.isAlive())){own.lastStand.add("support");for(var u:b.units)if(u.squad==own&&u.spec.group().equals("support")&&u.mob.isAlive())u.mob.setHealth(Math.min(u.mob.getHealth(),u.mob.getMaxHealth()*0.2f));var sup=b.units.stream().filter(u->u.squad==own&&u.spec.group().equals("support")&&u.mob.isAlive()).findFirst().orElse(null);var foe=b.units.stream().filter(u->u.squad!=own&&u.mob.isAlive()).findFirst().orElse(null);if(sup!=null&&foe!=null)MoraleChecks.place(b,sup,foe.mob.getX()-5,foe.mob.getZ()+3,-90);} // Morale itself finds the support group at its last stand: weak, an enemy within the radius
            if(keepRespawn&&player!=null)b.respawns.put(player,b.time+12*20L);
            if(keepRespawn&&b.running){b.roundStarted=b.time-(b.roundSeconds-462)*20L;b.scores[0]=148;b.scores[1]=121;} // the sweep (stages 40 to 49) holds the mock's 148, 7:42 and 121 that stage 10 set: a red run's 200-tick waits (a window that never lays out as set) outlast the 462 s and the 152 scoring steps the round had left, ending the round mid-sweep (UI Task 8 leftovers)
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
            if(tick>24000&&!finished){report("p1-ui-hud-timeout",false,"stuck in stage "+stage+" ("+HudSize.NAMES[size]+", pass "+"ABCD".charAt(pass)+", window "+win+")");end(c);return;}
            if(keepLesson)CampaignClient.tutorial=lessonStep;
            if(Math.abs(stage)>=2&&stage<99&&stage!=46&&stage!=47&&stage!=49&&stage!=92&&c.currentScreen!=null)c.setScreen(null); // stages 46, 47 and 49 hold the chat open, 92 the Visuals and sound screen
            switch(stage){
                case 0->{
                    if(tick<20)return;c.setScreen(null);var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();
                    campLift=ChatPlacement.lift(w.getScaledHeight());CommandMode.selected="all";UUID id=c.player.getUuid();window(c,960,540,1);waitFrom=tick;stage=-1;server(c,b->{player=id;stage=1;});
                }
                case -1,1->{if(tick-waitFrom>2400){report("p1-ui-hud-fixture",false,"no fixture 2400 ticks after the request");end(c);}}
                case 2->{ // the fixture on screen; then p1-ui-seals, part one
                    var inf=group("infantry");var ran=group("ranged");var sup=group("support");
                    boolean shown=GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()&&inf!=null&&inf.count()==INFANTRY-KILLED_INFANTRY&&ran!=null&&ran.count()==RANGED&&sup!=null&&sup.count()==SUPPORT-KILLED_SUPPORT
                        &&GravesAndGloryClient.ownerAt(0)==0&&GravesAndGloryClient.ownerAt(2)==1&&GravesAndGloryClient.contestedAt(1)&&!GravesAndGloryClient.contestedAt(0)&&!GravesAndGloryClient.contestedAt(2);
                    if(shown&&seen==0){seen=tick;BattleHud.roundShownMs=net.minecraft.util.Util.getMeasuringTimeMs()-61000;} // past the hint layer of the round's first seconds, 15 at Compact and 60 at Standard and Large (suite ui-feed checks it)
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
                case 10->{ // p1-ui-cards: ranged selected, support at its last stand, then infantry hurt
                    CommandMode.selected="ranged";routSupport=true;waitFrom=tick;stage=-10;
                    server(c,b->{b.scores[0]=148;b.scores[1]=121;b.roundStarted=b.time-(b.roundSeconds-462)*20L;stage=11;}); // the mock's 148, 7:42 and 121
                }
                case 11->{
                    var cs=BattleHud.cards();
                    if(cs.size()==3&&cs.get(2).state().equals("LASTSTAND")&&cs.get(1).tall()&&!CommandMode.flashing("infantry")){
                        waitFrom=tick;stage=-12;
                        server(c,b->{var own=b.squads.get(player);int n=0;
                            for(var u:b.units)if(n<4&&u.squad==own&&u.spec.group().equals("infantry")&&u.mob.isAlive()&&u.mob.getHealth()>10){u.mob.damage(b.world.getDamageSources().magic(),10);n++;}
                            System.out.println("[p1] ui-hud: "+n+" knights take 10 magic damage");stage=12;});
                    }else if(tick-waitFrom>200){report("p1-ui-cards",false,"support never drew LASTSTAND with ranged selected: "+cs);stage=20;}
                }
                case 12->{
                    if(CommandMode.flashing("infantry")){flashAt=tick;stage=13;}
                    else if(tick-waitFrom>40){shot(c,"ui-hud-cards");report("p1-ui-cards",false,"no infantry flash within 40 client ticks of the damage");stage=20;}
                }
                case 13->{
                    var cs=BattleHud.cards();var t=BattleHud.texts();boolean cards=cs.equals(CARDS);
                    // The fixture's counts and words; the ledger as the server set it (148, 7:42, 121), allowing one score tick and the seconds since.
                    String wantInf=(INFANTRY-KILLED_INFANTRY)+"/"+INFANTRY,wantRan=RANGED+"/"+RANGED,wantSup=(SUPPORT-KILLED_SUPPORT)+"/"+SUPPORT;
                    boolean words=wantInf.equals(t.get("count-infantry"))&&wantRan.equals(t.get("count-ranged"))&&wantSup.equals(t.get("count-support"))
                        &&"Fighting".equals(t.get("state-infantry"))&&"Steady".equals(t.get("state-ranged"))&&"LAST STAND".equals(t.get("state-support"))
                        &&"hud/stamp-last-chip".equals(t.get("stamp-support"))&&"hud/chip-plain".equals(t.get("plate-support"))&&t.get("stamp-infantry")==null&&t.get("stamp-ranged")==null; // a chip at its last stand is a live chip, not the grey one, with the stamp and its words
                    int dawn=GravesAndGloryClient.dawn,dusk=GravesAndGloryClient.dusk,secs=CommandMode.seconds;
                    boolean ledger=String.valueOf(dawn).equals(t.get("score-dawn"))&&String.valueOf(dusk).equals(t.get("score-dusk"))&&UiMath.clock(secs).equals(t.get("clock"))
                        &&dawn>=148&&dawn<=149&&dusk>=121&&dusk<=122&&secs>=442&&secs<=462;
                    boolean match=cards&&words&&ledger;
                    if(match||tick>=flashAt+3){shot(c,"ui-hud-cards");
                        report("p1-ui-cards",match,(tick-flashAt)+" client tick(s) after the flash the cards drew "+cs+(cards?"":" (wanted "+CARDS+")")+"; counts "+t.get("count-infantry")+", "+t.get("count-ranged")+", "+t.get("count-support")+" (wanted "+wantInf+", "+wantRan+", "+wantSup+"); words "+t.get("state-infantry")+", "+t.get("state-ranged")+", "+t.get("state-support")+" (wanted Fighting, Steady, LAST STAND), support's stamp "+t.get("stamp-support")+" on plate "+t.get("plate-support")+" (wanted hud/stamp-last-chip on hud/chip-plain; none on the others); ledger "+t.get("score-dawn")+" "+t.get("clock")+" "+t.get("score-dusk")+" (client "+dawn+", "+UiMath.clock(secs)+", "+dusk+"; wanted 148 or 149, 7:22 to 7:42, 121 or 122)");CommandMode.selected="support";seen=0;waitFrom=tick;stage=14;}
                }
                case 14->{ // p1-ui-cards-last-stand: the same group selected, so its full card stands its last stand
                    var cs=BattleHud.cards();var t=BattleHud.texts();var sup=cs.stream().filter(d->d.group().equals("support")).findFirst().orElse(null);
                    boolean tall=sup!=null&&sup.tall()&&sup.state().equals("LASTSTAND");
                    if(tall&&seen==0)seen=tick;else if(!tall)seen=0;
                    if(seen>0&&tick>=seen+2){
                        boolean ok=sup.h()==UiMath.CARD_H&&"LAST STAND".equals(t.get("state-support"))&&"1/2".equals(t.get("count-support"))&&"hud/stamp-last-card".equals(t.get("stamp-support"))&&"hud/card-sel".equals(t.get("plate-support"));
                        shot(c,"ui-hud-last-stand-card");
                        report("p1-ui-cards-last-stand",ok,"with support selected at its last stand the column drew "+cs+"; support's words "+t.get("state-support")+", count "+t.get("count-support")+", stamp "+t.get("stamp-support")+" on plate "+t.get("plate-support")+" (wanted its tall card, LAST STAND, 1/2, hud/stamp-last-card on the live hud/card-sel, not the grey card)");seen=0;stage=20;}
                    else if(seen==0&&tick-waitFrom>200){report("p1-ui-cards-last-stand",false,"support selected at its last stand never drew its tall LASTSTAND card: "+cs);stage=20;}
                }
                case 20->{CommandMode.selected="all";routSupport=false;waitFrom=tick;stage=-20;server(c,b->{var own=b.squads.get(player);own.lastStand.remove("support");for(var u:b.units)if(u.squad==own&&u.spec.group().equals("support")&&u.mob.isAlive()){u.mob.setHealth(u.mob.getMaxHealth());MoraleChecks.place(b,u,AX-16,AZ+9,-90);}stage=21;});}
                case 21->{ // p1-ui-army-selected
                    var sup=group("support");boolean settled=sup!=null&&!sup.lastStand()&&!CommandMode.flashing("infantry")&&!CommandMode.flashing("ranged")&&!CommandMode.flashing("support");
                    if(!settled)seen=0;else if(seen==0)seen=tick;
                    if(seen>0&&tick>=seen+2){var cs=BattleHud.cards();int[] strip=BattleHud.rects().get("keys");var rows=BattleHud.keyRows().stream().map(k->k.group()+" "+k.parts()).toList();
                        // UI Task 8c: a brass chip for each group over the army's one key strip, the keys the same for every group
                        boolean ok=cs.size()==3&&cs.stream().allMatch(d->d.state().equals("SELECTED"))&&cs.stream().map(BattleHud.CardDraw::tall).toList().equals(List.of(false,false,false))&&cs.stream().map(BattleHud.CardDraw::y).toList().equals(List.of(440,462,484))
                            &&Arrays.equals(strip,new int[]{24,506,176,16})&&BattleHud.keyRows().size()==1&&BattleHud.keyRows().get(0).group().equals("all");
                        shot(c,"ui-hud-army");
                        report("p1-ui-army-selected",ok,"with the whole army selected the column drew "+cs+", the key strip at "+box(strip)+" and the key rows "+rows+" (wanted three brass chips at y 440, 462 and 484 over the strip at [24, 506, 176, 16] with one row, all's)");seen=0;stage=30;}
                    else if(seen==0&&tick-waitFrom>200){report("p1-ui-army-selected",false,"support never stopped routing, or a flash never ended: "+BattleHud.cards());stage=30;}
                }
                case 30->{askAt=tick;stage=-30;server(c,b->{b.testAbility=new GravesAndGlory.Battle.AbilityState("rally_horn",960,1800);stage=31;});}
                case 31->{ // p1-ui-ability
                    if(abilitySeen<0&&GravesAndGloryClient.abilityId.equals("rally_horn"))abilitySeen=tick;
                    if(abilitySeen>=0&&tick>=abilitySeen+1){
                        int[] r=BattleHud.rects().get("ability");String secs=BattleHud.texts().get("ability-seconds");
                        String icon=BattleHud.texts().get("ability-icon");
                        abilityOk=abilitySeen-askAt<=12&&Arrays.equals(r,new int[]{584,493,50,32})&&"48".equals(secs)&&"hud/ability-rally_horn".equals(icon);
                        abilityWhy="the client read abilityId rally_horn "+(abilitySeen-askAt)+" client ticks after the request (10 server ticks, the hand-off and a frame allowed: 12); rect ability "+box(r)+", seconds \""+secs+"\", key \""+BattleHud.texts().get("ability-key")+"\", picture "+icon+" (wanted hud/ability-rally_horn, never the title's hud/horn)";
                        shot(c,"ui-hud-ability");askAt=tick;stage=-32;server(c,b->{b.testAbility=null;stage=32;});
                    }else if(abilitySeen<0&&tick-askAt>60){report("p1-ui-ability",false,"the client never read rally_horn (abilityId "+GravesAndGloryClient.abilityId+")");stage=40;}
                }
                case 32->{ // testAbility cleared: the 960-tick test triple is gone, and the slot shows what play shows (GravesAndGloryClient.classAbility: the commander class's own active, or nothing for none and the unshown Forward Supply)
                    if(seen==0&&GravesAndGloryClient.abilityRemaining!=960)seen=tick;
                    if(seen>0&&tick>=seen+2){
                        String id=GravesAndGloryClient.abilityId,icon=BattleHud.texts().get("ability-icon");boolean shown=BattleHud.rects().containsKey("ability");
                        boolean right=id.equals("none")||id.equals(BattleHud.UNSHOWN_ABILITY)?!shown:shown&&("hud/ability-"+id).equals(icon);
                        report("p1-ui-ability",abilityOk&&right&&seen-askAt<=12,abilityWhy+"; with testAbility null the client read abilityId \""+id+"\" "+(seen-askAt)+" client ticks later and the slot "+(shown?"drew picture "+icon:"was gone")+" (wanted its own picture for a class active, no slot for none)");
                        askAt=tick;seen=0;stage=-33;server(c,b->{b.testAbility=new GravesAndGlory.Battle.AbilityState("forward_supply",960,1800);stage=33;});}
                    else if(seen==0&&tick-askAt>60){report("p1-ui-ability",false,abilityWhy+"; the test ability (960 ticks) was still on the slot 60 ticks after testAbility was cleared");stage=40;}
                }
                case 33->{ // p1-ui-ability-hidden: the Quartermaster's Forward Supply draws no slot at all (the key stays bound)
                    boolean named=GravesAndGloryClient.abilityId.equals("forward_supply");
                    if(named&&seen==0)seen=tick;
                    if(seen>0&&tick>=seen+3){
                        boolean none=!BattleHud.rects().containsKey("ability")&&!BattleHud.texts().containsKey("ability")&&!BattleHud.texts().containsKey("ability-icon");
                        shot(c,"ui-hud-ability-hidden");
                        report("p1-ui-ability-hidden",none&&seen-askAt<=12,"the client read abilityId forward_supply "+(seen-askAt)+" client ticks after the request; the frame drew rects "+BattleHud.rects().keySet()+" (wanted no ability rect), picture \""+BattleHud.texts().get("ability-icon")+"\" (wanted none): no slot, no frame, no number; the key "+GravesAndGloryClient.hint("ability")+" stays bound");
                        askAt=tick;seen=0;stage=-34;server(c,b->{b.testAbility=null;stage=34;});}
                    else if(seen==0&&tick-askAt>60){report("p1-ui-ability-hidden",false,"the client never read forward_supply (abilityId "+GravesAndGloryClient.abilityId+")");stage=40;}
                }
                case 34->{if(GravesAndGloryClient.abilityRemaining!=960||tick-askAt>60)stage=40;}
                case 40->{keepLesson=true;keepRespawn=true;win=0;pass=0;size=UiMath.HUD_COMPACT;HudSize.set(size);stage=-40;server(c,b->{b.testAbility=new GravesAndGlory.Battle.AbilityState("rally_horn",960,1800);stage=41;});}
                case 41->{var s=WINDOWS[win];window(c,s[0],s[1],s[2],s[3]==1);waitFrom=tick;settled=0;stage=42;
                    BattleHud.roundShownMs=net.minecraft.util.Util.getMeasuringTimeMs()-(pass==1||pass==3?0:61000); // B and D: a round's first seconds, the hint layer up
                    EventFeed.clear();feedLine(); // a feed line in every measured frame (the Task 8a review's Important 2)
                    var chat=c.inGameHud.getChatHud();chat.clear(false);for(String m:CHAT)chat.addMessage(net.minecraft.text.Text.literal(m));}
                case 42->{ // p1-ui-small-window, p1-ui-hud-layout and p1-ui-chat-lift: one window at one size in one pass
                    var r=BattleHud.rects();var s=WINDOWS[win];var fb=c.getWindow();
                    if(minimized(c)){window(c,s[0],s[1],s[2],s[3]==1);return;} // minimized since the size was set: show it again, then measure
                    if(EventFeed.lines(net.minecraft.util.Util.getMeasuringTimeMs()).isEmpty())feedLine(); // the line outlived its 6 s: another
                    boolean sized=fb.getFramebufferWidth()==s[0]&&fb.getFramebufferHeight()==s[1]&&(int)fb.getScaleFactor()==s[2];
                    boolean laid=sized&&UiCanvas.hudK()==s[4+size]&&layoutDiff(UiMath.hudLayout(s[0],s[1],s[2],UiCanvas.hudK(),size,expected(c)),plates(r)).isEmpty();
                    boolean full=laid&&r.containsKey("ability")&&r.containsKey("feed")&&BattleHud.cards().size()==3&&ChatPlacement.drawnLines>0;
                    settled=full?settled+1:0; // the chat follows the HUD's frame before: the pass's state held for three ticks
                    if(settled>=3||tick-waitFrom>200){
                        if(settled<3){smallOk=false;smallWhy+=(smallWhy.isEmpty()?"":"; ")+s[0]+"x"+s[1]+" GUI "+s[2]+" "+HudSize.NAMES[size]+" pass "+"ABCD".charAt(pass)+": no frame laid out as the pass sets it in 200 ticks (drew "+r.keySet()+", the HUD at "+UiCanvas.hudK()+"x), WRONG";}
                        measure(c,s,r);
                        if(shoot(s))shot(c,SHOTS[win]+PASS_SHOT[pass]+SIZE_SHOT[size]);
                        // then the chat open at this size (UI polish round 2), 25 lines in it: stage 47 measures it
                        var chat=c.inGameHud.getChatHud();chat.clear(false);for(int i=1;i<=25;i++)chat.addMessage(net.minecraft.text.Text.literal("Chat check line "+i));
                        c.setScreen(new net.minecraft.client.gui.screen.ChatScreen(""));waitFrom=tick;stage=47;
                    }
                }
                case 47->{if(tick-waitFrom>=3){measureOpen(c,WINDOWS[win],BattleHud.rects());c.setScreen(null);win++;stage=win<WINDOWS.length?41:size==UiMath.HUD_COMPACT&&pass==0?45:54;}}
                case 45->{ // p1-ui-chat-respawn (UI polish M2): at 1366x768 GUI 2, Compact's pass A's last window, the open chat stops below the respawn card
                    var chat=c.inGameHud.getChatHud();chat.clear(false);for(int i=1;i<=25;i++)chat.addMessage(net.minecraft.text.Text.literal("Chat check line "+i));
                    c.setScreen(new net.minecraft.client.gui.screen.ChatScreen(""));waitFrom=tick;stage=46;}
                case 46->{ // at Compact the column ends far below the respawn card: the open chat shows 15 lines under it
                    if(tick-waitFrom>=3){var w=c.getWindow();var hud=c.inGameHud.getChatHud();int want=WINDOWS[WINDOWS.length-1][8];int[] dr=ChatPlacement.drawn,card=BattleHud.rects().get("respawn"),chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);
                        boolean open=c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;int shown=hud.getVisibleLineCount();shot(c,"ui-hud-chat-open");
                        report("p1-ui-chat-respawn",open&&card!=null&&chat!=null&&shown==want&&ChatPlacement.drawnLines==want&&!meets(chat,card)&&chat[1]>=card[1]+card[3],"with the chat open (true: "+open+") at "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+" GUI "+(int)w.getScaleFactor()+" (the HUD at "+UiCanvas.hudK()+"x, "+HudSize.NAMES[HudSize.get()]+") and 25 lines in it, it shows "+shown+" lines (wanted "+want+" of vanilla's 20) and drew "+ChatPlacement.drawnLines+" at canvas "+box(chat)+" (GUI "+box(dr)+"), wanted below the respawn card at "+box(card)+"; the lesson note at "+box(BattleHud.rects().get("lesson")));
                        c.setScreen(null);stage=54;}
                }
                case 54->{ // a pass done at every window: the next pass, or pass A at the next size; after Large, the size setting
                    win=0;
                    if(pass==0){pass=1;keepRespawn=false;stage=-54;server(c,b->{b.respawns.remove(player);stage=41;});} // B: the hint layer up, the commander up
                    else if(pass==1){pass=2;keepLesson=false;CampaignClient.tutorial=-1;stage=41;} // C: a battle without the tutorial's cards
                    else if(pass==2){pass=3;stage=41;} // D: a battle's first seconds, the hint layer up
                    else if(size<UiMath.HUD_LARGE){size++;HudSize.set(size);pass=0;keepLesson=true;keepRespawn=true;stage=41;}
                    else{size=UiMath.HUD_COMPACT;HudSize.set(size);stage=90;}
                }
                case 90->{window(c,1920,1080,4,true);waitFrom=tick;stage=91;} // p1-ui-hud-size-setting at 1080p GUI 4, where Standard is a step above Compact
                case 91->{
                    if(minimized(c)){window(c,1920,1080,4,true);return;}
                    var fb=c.getWindow();boolean sized=fb.getFramebufferWidth()==1920&&fb.getFramebufferHeight()==1080&&(int)fb.getScaleFactor()==4;
                    if(sized&&tick-waitFrom>=5||tick-waitFrom>200){c.setScreen(new PresentationScreen());waitFrom=tick;stage=92;}
                }
                case 92->{if(tick-waitFrom>=5){shot(c,"ui-hud-size-setting");sizeSetting(c);c.setScreen(null);stage=57;}}
                case 57->{keepLesson=true;BattleHud.roundShownMs=net.minecraft.util.Util.getMeasuringTimeMs()-61000;EventFeed.clear();feedLine();window(c,1280,720,3);waitFrom=tick;stage=48;}
                case 48->{ // p1-ui-chat-open (UI polish round 2): typing with the commander up at 1280x720 GUI 3, the crosshair over the chat
                    var r=BattleHud.rects();var fb=c.getWindow();
                    if(minimized(c)){window(c,1280,720,3);return;}
                    if(EventFeed.lines(net.minecraft.util.Util.getMeasuringTimeMs()).isEmpty())feedLine();
                    boolean sized=fb.getFramebufferWidth()==1280&&fb.getFramebufferHeight()==720&&(int)fb.getScaleFactor()==3;
                    if(sized&&r.containsKey("lesson")&&r.containsKey("spine")&&r.containsKey("feed")&&!r.containsKey("respawn")||tick-waitFrom>200){
                        var chat=c.inGameHud.getChatHud();closedLines=chat.getVisibleLineCount();chat.clear(false);for(int i=1;i<=25;i++)chat.addMessage(net.minecraft.text.Text.literal("Chat check line "+i));
                        c.setScreen(new net.minecraft.client.gui.screen.ChatScreen(""));waitFrom=tick;stage=49;}
                }
                case 49->{
                    if(tick-waitFrom>=3){var w=c.getWindow();var hud=c.inGameHud.getChatHud();var r=BattleHud.rects();int sw=w.getScaledWidth(),sh=w.getScaledHeight(),shown=hud.getVisibleLineCount();
                        int[] dr=ChatPlacement.drawn,cross={(sw-15)/2,(sh-15)/2,15,15},chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);boolean open=c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen,over=meets(dr,cross);
                        List<String> hit=new ArrayList<>();if(chat!=null)for(var e:r.entrySet())if(meets(chat,e.getValue()))hit.add(e.getKey());shot(c,"ui-hud-chat-typing");
                        // Compact (UI Task 8c): closed, the chat keeps off vanilla's bars (UiMath.chatSlot, a49ceb0): its bottom at GUI 187, 4 px
                        // clear of the armour row at sh-49 (the hearts at sh-39), so 6 lines fit below the crosshair; open, it keeps 4 px over
                        // the ability slot beside the hotbar (UI Task 8b-2: bottom 189, not vanilla's 200) and 16 fit below the lesson note's shadow
                        report("p1-ui-chat-open",open&&chat!=null&&r.containsKey("lesson")&&r.containsKey("feed")&&!r.containsKey("respawn")&&closedLines==6&&shown==16&&ChatPlacement.drawnLines==16&&hit.isEmpty()&&over,"at "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+" GUI "+(int)w.getScaleFactor()+" ("+HudSize.NAMES[HudSize.get()]+", the HUD at "+UiCanvas.hudK()+"x) with the commander up (a respawn card: "+r.containsKey("respawn")+"), the feed leaf at "+box(r.get("feed"))+" and the lesson note at "+box(r.get("lesson"))+", the closed chat showed "+closedLines+" lines (wanted 6: its bottom 4 px over the armour row, the crosshair above it); open (true: "+open+") with 25 lines in it, it shows "+shown+" and drew "+ChatPlacement.drawnLines+" at GUI "+box(dr)+" (wanted 16, the lesson note above it, the ability slot below)"+(hit.isEmpty()?", apart from every element of the HUD":", meeting "+hit)+", "+(over?"over":"clear of")+" the crosshair at GUI "+box(cross));
                        c.setScreen(null);stage=43;}
                }
                case 43->{keepLesson=false;keepRespawn=false;CampaignClient.tutorial=-1;int frames=3*4*WINDOWS.length;
                    report("p1-ui-small-window",smallOk&&smallSeen==frames,smallSeen+" frames measured (wanted "+frames+": "+WINDOWS.length+" windows, four passes, three sizes), each against vanilla's bars as it had them, the offhand slot at every size"+(smallWhy.isEmpty()?"; in every one each plate is 12 px inside the canvas and apart, the feed leaf, lesson note and respawn card CARD_GAP rows clear of each other's and the column's inks":"; "+smallWhy));
                    report("p1-ui-hud-layout",layoutOk&&layoutSeen==frames,"in "+layoutSeen+" frames (wanted "+frames+") the plates as BattleHud drew them against UiMath.hudLayout for the state the pass set, one layout for the draw and the fit (the Task 8a review's Minor 3): "+(layoutWhy.isEmpty()?"every one equal":layoutWhy));
                    report("p1-ui-chat-lift",chatOk&&campLift==0,"in camp the chat moved "+campLift+" GUI px; closed/open lines frame by frame: "+chatLines+(chatWhy.isEmpty()?"":"; "+chatWhy));
                    report("p1-ui-hud-scale",scaleOk&&scaleSeen==3*WINDOWS.length,"the battle HUD at each size (UI Task 8c: Compact at the kit scale on every window, Standard and Large a step up where the whole HUD fits beside vanilla's bars): "+scaleWhy+(scaleSeen==3*WINDOWS.length?"":"; only "+scaleSeen+" of "+3*WINDOWS.length+" windows measured")+(Arrays.stream(WINDOWS).allMatch(v->v[6]==v[5])?"; Large draws as Standard at every one of these windows (no step further fits them)":""));
                    report("p1-ui-hud-footprint",footOk&&footSeen==5,"the HUD's plates (cards and chips, the key strip, the feed leaf, the hint slip, the lesson note, the respawn card) as a share of the screen in the default battle state (three groups, the army selected, the hint layer down, a feed line, no lesson, the commander up: pass C at Compact), against the HUD before this task ("+BASELINE_AREA+" canvas px at Task 8a's scale, run ui-8c-base-211317), at most half of it and the centre band clear at the brief's five windows: "+footWhy+(footSeen==5?"":"; only "+footSeen+" of the five measured"));
                    window(c,960,540,1);
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
                        edgeOk=fallen&&"0/2".equals(count);edgeWhy="with every support troop killed the support chip drew "+sup+" with count \""+count+"\"";if(fallen)shot(c,"ui-hud-fallen");
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
                        boolean ok=edgeOk&&two.stream().map(BattleHud.CardDraw::y).toList().equals(List.of(438,504))&&two.get(0).group().equals("infantry")&&two.get(0).tall()&&BattleHud.cards().size()==2;
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
                        // pinned by number, not by the centring under test (Task 3 review Minor 8): map 0's point B, 78 px of text and its
                        // shadow, centred under the middle seal at x 480 of the 960 px canvas, the half pixel rounded right (run ui-8a-base-181517)
                        String want="Trade Crossing",got=BattleHud.texts().get("aim-name");int[] r=BattleHud.rects().get("aim-name");
                        int[] wantR={441,68,79,9};
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
                    if(tick-waitFrom>=10&&BattleHud.rects().containsKey("lesson")){shot(c,"ui-hud-lesson");System.out.println("[p1] ui-hud-lesson: the three-v lesson note at "+box(BattleHud.rects().get("lesson"))+" with "+GravesAndGloryClient.lessonDrawn.size()+" lines"); // controller addition B
                        cacheW=BattleHud.wraps;cacheF=BattleHud.frames;lessonBefore=BattleHud.texts().get("lesson");views=new Object[]{BattleHud.rects(),BattleHud.texts(),BattleHud.cards(),BattleHud.seals()};waitFrom=tick;stage=82;}
                    else if(tick-waitFrom>200)lateFail(c,"the lesson note was not drawn 200 ticks after lesson 4 was pinned");
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

        /** One window at one size in one pass (UI Task 8c): the HUD at the window's scale for the size (p1-ui-hud-scale, pass A); the
         *  plates the frame drew, the ability slot and the aimed name apart, equal to UiMath.hudLayout for the state the pass set
         *  (p1-ui-hud-layout, the Task 8a review's Minor 3); and UiMath.frameFaults on them against vanilla's bars as this frame has
         *  them, the selected slot's frame where it is and the offhand slot at every size (p1-ui-small-window): every plate 12 px
         *  inside the canvas, none on another (but a card or chip on the spine, a boss on the score card), none on a bar, and the feed
         *  leaf, lesson note and respawn card CARD_GAP rows clear of each other's inks and the column's (the review's Important 2: the
         *  feed leaf in every frame). Then the lifted chat (p1-ui-chat-lift) and, at Compact in pass C, the footprint
         *  (p1-ui-hud-footprint). Each frame is logged in full; the reports carry what went wrong. */
        void measure(MinecraftClient c,int[] s,Map<String,int[]> r){
            var w=c.getWindow();int hk=UiCanvas.hudK(),cw=UiCanvas.hudWidth(),ch=UiCanvas.hudHeight(),sw=w.getScaledWidth(),sh=w.getScaledHeight();List<String> bad=new ArrayList<>();
            String at=s[0]+"x"+s[1]+" GUI "+s[2]+" "+HudSize.NAMES[size]+" pass "+"ABCD".charAt(pass);
            if(w.getFramebufferWidth()!=s[0]||w.getFramebufferHeight()!=s[1]||(int)w.getScaleFactor()!=s[2])bad.add("the window is "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+" at GUI "+(int)w.getScaleFactor());
            if(hk!=s[4+size])bad.add("the HUD at "+hk+"x (wanted "+s[4+size]+"x)");
            if(pass==0){boolean ok=hk==s[4+size]&&cw==s[0]/hk&&ch==s[1]/hk&&UiCanvas.k()==UiMath.pixelScale(s[0],s[1],s[2]);scaleOk&=ok;scaleSeen++;
                scaleWhy+=(scaleWhy.isEmpty()?"":"; ")+s[0]+"x"+s[1]+" GUI "+s[2]+" "+HudSize.NAMES[size]+": the HUD at "+hk+"x (wanted "+s[4+size]+"x) on a "+cw+"x"+ch+" canvas, the screens at "+UiCanvas.k()+"x"+(ok?"":", WRONG");}
            var off=layoutDiff(UiMath.hudLayout(s[0],s[1],s[2],hk,size,expected(c)),plates(r));layoutOk&=off.isEmpty();layoutSeen++;
            if(!off.isEmpty())layoutWhy+=(layoutWhy.isEmpty()?"":"; ")+at+": "+String.join(", ",off.subList(0,Math.min(4,off.size())));
            Map<String,int[]> bars=new LinkedHashMap<>();int slot=c.player.getInventory().selectedSlot;
            bars.put("the hotbar",canvas(c,sw/2-91,sh-22,182,22));bars.put("the selected slot",canvas(c,sw/2-92+slot*20,sh-23,24,22));bars.put("the offhand slot",canvas(c,sw/2-120,sh-23,29,24));bars.put("the hearts",canvas(c,sw/2-91,sh-39,81,9));
            bars.put("the armour row",canvas(c,sw/2-91,sh-49,81,9));bars.put("the experience bar",canvas(c,sw/2-91,sh-29,182,5));bars.put("the crosshair",canvas(c,(sw-15)/2,(sh-15)/2,15,15));
            bad.addAll(UiMath.frameFaults(r,cw,ch,bars));
            if(!r.containsKey("feed"))bad.add("no feed leaf in the frame");
            smallOk&=bad.isEmpty();smallSeen++;
            if(!bad.isEmpty())smallWhy+=(smallWhy.isEmpty()?"":"; ")+at+": WRONG: "+String.join("; ",bad.subList(0,Math.min(6,bad.size())));
            System.out.println("[p1] ui-hud frame "+at+": canvas "+cw+"x"+ch+" (HUD "+hk+"x, screens "+UiCanvas.k()+"x), "+r.size()+" rects, column "+BattleHud.cards().stream().map(d->d.group()+" "+d.y()+(d.tall()?" card":" chip")).toList()+", keys "+box(r.get("keys"))+", hints "+box(r.get("hints"))+", lesson "+box(r.get("lesson"))+", respawn "+box(r.get("respawn"))+", feed "+box(r.get("feed"))+", spine "+box(r.get("spine"))+", ability "+box(r.get("ability"))+", hotbar "+box(bars.get("the hotbar"))+", offhand "+box(bars.get("the offhand slot"))+", crosshair "+box(bars.get("the crosshair"))+(bad.isEmpty()?", every rect inside and apart":", WRONG: "+bad)+(off.isEmpty()?", as laid out":", NOT as laid out: "+off));
            // p1-ui-chat-lift: the chat's lines above the left column (ChatPlacement), on the screen, apart from every element of the
            // HUD and, closed, from vanilla's crosshair (UI polish M1, Task 8a) and vanilla's bars (UI Task 8c: the compact column can
            // leave the chat at vanilla's own place, on the armour row at 1280x720 GUI 3), and at Compact the lines it shows closed, the window's
            // wanted count in this pass (measureOpen: open); twelve lines are in it, so it draws every line it shows
            var hud=c.inGameHud.getChatHud();int top=UiMath.columnTop(r);int[] dr=ChatPlacement.drawn,cross={(sw-15)/2,(sh-15)/2,15,15};
            int[] chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);int shown=hud.getVisibleLineCount(),want=size==UiMath.HUD_COMPACT?s[7+2*pass]:-1;
            List<String> hit=new ArrayList<>();if(chat!=null)for(var e:r.entrySet())if(meets(chat,e.getValue()))hit.add(e.getKey());
            List<String> onBars=new ArrayList<>();if(chat!=null)for(var e:bars.entrySet())if(!e.getKey().equals("the crosshair")&&meets(chat,e.getValue()))onBars.add(e.getKey()); // UI Task 8c
            boolean clear=dr!=null&&!meets(dr,cross);
            boolean apart=chat!=null&&dr[1]>=0&&top!=UiMath.NO_COLUMN&&chat[1]+chat[3]<=top&&ChatPlacement.drawnLines==shown&&(want<0?shown>=1:shown==want)&&clear&&hit.isEmpty()&&onBars.isEmpty();chatOk&=apart;
            chatLines.append(chatLines.length()==0?"":", ").append(s[0]).append("x").append(s[1]).append(" GUI ").append(s[2]).append(" ").append(HudSize.NAMES[size]).append(" ").append("ABCD".charAt(pass)).append(" ").append(shown).append(want<0||shown==want?"":" (want "+want+")");
            if(!apart)chatWhy+=(chatWhy.isEmpty()?"":"; ")+at+" closed: chat scale "+hud.getChatScale()+", lifted "+ChatPlacement.lift(sh)+" GUI px, "+ChatPlacement.drawnLines+" lines drawn at canvas "+box(chat)+" (GUI "+box(dr)+"), the column's top at "+top+", the crosshair at GUI "+box(cross)+(clear?" clear":" COVERED")+(hit.isEmpty()?", apart from every element of the HUD":", meeting "+hit)+(onBars.isEmpty()?", off vanilla's bars":", on "+onBars)+"; it shows "+shown+" lines"+(want<0?"":" (wanted "+want+")")+", WRONG";
            // p1-ui-hud-footprint (UI Task 8c, item 5): the default battle state is pass C at Compact
            if(size==UiMath.HUD_COMPACT&&pass==2){
                double share=UiMath.footprint(r,hk,s[0],s[1]),before=BASELINE_AREA*(double)s[15]*s[15]/((double)s[0]*s[1]);var band=UiMath.inBand(r,cw,ch);
                boolean ok=share<=before/2&&band.isEmpty();if(s[16]==1){footOk&=ok;footSeen++;}
                footWhy+=(footWhy.isEmpty()?"":"; ")+String.format(Locale.ROOT,"%dx%d GUI %d: %.3f%% before, %.3f%% now (%.0f%% of before)",s[0],s[1],s[2],100*before,100*share,100*share/before)+(band.isEmpty()?", the centre band clear":", in the centre band "+band)+(s[16]==1?(ok?"":", WRONG"):" (logged; not one of the five)");
            }
        }

        /** p1-ui-chat-lift at one window with the chat open (UI polish round 2): at Compact it shows and draws the window's open count
         *  in this pass, at Standard and Large at least a line; on the screen and apart from every element of the HUD; over the
         *  crosshair is allowed, and the log says which. */
        void measureOpen(MinecraftClient c,int[] s,Map<String,int[]> r){
            var w=c.getWindow();var hud=c.inGameHud.getChatHud();int sw=w.getScaledWidth(),sh=w.getScaledHeight(),lh=((gg.gravesandglory.mixin.ChatHudAccessor)hud).gravesandglory$getLineHeight(),shown=hud.getVisibleLineCount(),want=size==UiMath.HUD_COMPACT?s[8+2*pass]:-1;
            int vanilla=net.minecraft.client.gui.hud.ChatHud.getHeight(c.options.getChatHeightFocused().getValue())/lh;int[] dr=ChatPlacement.drawn,cross={(sw-15)/2,(sh-15)/2,15,15},chat=dr==null?null:canvas(c,dr[0],dr[1],dr[2],dr[3]);
            boolean open=c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen;List<String> hit=new ArrayList<>();if(chat!=null)for(var e:r.entrySet())if(meets(chat,e.getValue()))hit.add(e.getKey());
            boolean ok=open&&chat!=null&&dr[1]>=0&&(want<0?shown>=1:shown==want)&&ChatPlacement.drawnLines==shown&&hit.isEmpty();chatOk&=ok;
            chatLines.append("/").append(shown).append(want<0||shown==want?"":" (want "+want+")").append(dr!=null&&meets(dr,cross)?" over the crosshair":"");
            if(!ok)chatWhy+=(chatWhy.isEmpty()?"":"; ")+s[0]+"x"+s[1]+" GUI "+s[2]+" "+HudSize.NAMES[size]+" pass "+"ABCD".charAt(pass)+" open"+(open?"":" (NOT open)")+": "+shown+" of vanilla's "+vanilla+" lines shown and "+ChatPlacement.drawnLines+" drawn at GUI "+box(dr)+(want<0?"":" (wanted "+want+")")+(hit.isEmpty()?", apart from every element of the HUD":", meeting "+hit)+", WRONG";
        }
        /** Whether this frame is photographed: every pass at Compact; B and C at Standard, and at Large where its scale is not
         *  Standard's (at none of these windows: the scale report says so). */
        boolean shoot(int[] s){return size==UiMath.HUD_COMPACT||(pass==1||pass==2)&&(size==UiMath.HUD_STANDARD||s[6]!=s[5]);}
        /** A feed line for the frame to draw (the Task 8a review's Important 2: the feed in every measured frame). */
        static void feedLine(){EventFeed.push("point",0,"Dawn took Mill Hamlet",net.minecraft.util.Util.getMeasuringTimeMs());}
        /** A frame's plates as BattleHud drew them: its rects without the ability slot (vanilla's GUI px) and the aimed point's name. */
        static Map<String,int[]> plates(Map<String,int[]> r){var p=new LinkedHashMap<>(r);p.remove("ability");p.remove("aim-name");return p;}
        /** Where drawn boxes differ from the layout: a box the layout has that the frame lacks or drew elsewhere, and one it drew that
         *  the layout lacks. Empty when they are the same. */
        static List<String> layoutDiff(Map<String,int[]> want,Map<String,int[]> got){
            List<String> off=new ArrayList<>();
            for(var e:want.entrySet())if(!Arrays.equals(e.getValue(),got.get(e.getKey())))off.add(e.getKey()+" drawn "+box(got.get(e.getKey()))+", laid out "+box(e.getValue()));
            for(var e:got.entrySet())if(!want.containsKey(e.getKey()))off.add(e.getKey()+" drawn "+box(e.getValue())+", not in the layout");
            return off;
        }
        /** The state this pass sets, as UiMath.hudLayout reads it, from the game's own records: the points, the groups fielded in their
         *  order, the selection, the hint layer in B and D, lesson 4's lines in A and B (UiMath.lessonLines in the small style after its
         *  tag, as the note wraps it), the commander down in A, and the feed line every frame has. */
        UiMath.HudState expected(MinecraftClient c){
            List<String> fielded=new ArrayList<>();for(String id:Morale.GROUPS){var g=group(id);if(g!=null&&g.full()>0)fielded.add(id);}
            int lines=0;var font=c.textRenderer;
            if(pass<2){int indent=font.getWidth(DarkUi.small(BattleHud.lessonTag(lessonStep)))+UiMath.LESSON_TAG_GAP;lines=UiMath.lessonLines(GravesAndGloryClient.lessonText(lessonStep),indent,UiMath.LESSON_TEXT_W,v->font.getWidth(DarkUi.small(v))).size();}
            return new UiMath.HudState(GravesAndGloryClient.owners.length,fielded,CommandMode.selected,pass==1||pass==3,lines,pass==0,true);
        }
        /** p1-ui-hud-size-setting (UI Task 8c, item 2): the Visuals and sound screen's Battle HUD button names the size and cycles
         *  Compact, Standard, Large and back to Compact; each press saves gravesandglory/hud.json at once, a launch reads the same back
         *  (HudSize.reload, as the first HUD frame of a launch does), and the HUD's scale follows: at 1080p GUI 4, 2x at Compact and 3x
         *  at Standard and Large. Fix round 1 (the Task 8c review's Important 2): Large draws at Standard's scale there, so its button
         *  and its notice say so (UiMath.hudShownAs), in the words written out here; Compact and Standard say only their names. Every
         *  word the button can show must fit it uncut in the game's body font. */
        void sizeSetting(MinecraftClient c){
            boolean ok=c.currentScreen instanceof PresentationScreen;List<String> steps=new ArrayList<>();int[] wantK={2,3,3};
            String[] wantLabel={"Battle HUD: Compact","Battle HUD: Standard","Battle HUD: Large (Standard size here)"};
            String[] wantNotice={"Battle HUD: Compact. Your choice is saved.","Battle HUD: Standard. Your choice is saved.","Battle HUD: Large. Your choice is saved. In this window it draws at Standard size."};
            for(int press=0;press<4;press++){
                if(press>0){var button=hudButton(c);if(button==null){ok=false;steps.add("no Battle HUD button to press");break;}button.onPress();}
                int want=press%3;var b=hudButton(c);String label=b==null?"none":b.getMessage().getString(),file=hudFile(),notice=Presentation.notice;int got=HudSize.get(),k=UiCanvas.hudK();
                HudSize.reload();int read=HudSize.get();
                boolean step=label.equals(wantLabel[want])&&(press==0||notice.equals(wantNotice[want]))&&got==want&&read==want&&k==wantK[want]&&file.equals("{\"size\":\""+HudSize.NAMES[want]+"\"}");ok&=step;
                steps.add((press==0?"before any press":"press "+press)+": the button reads '"+label+"'"+(press==0?"":" (wanted '"+wantLabel[want]+"'), the notice '"+notice+"' (wanted '"+wantNotice[want]+"')")+", HudSize "+HudSize.NAMES[got]+", hud.json "+file+", read back as a launch reads it: "+HudSize.NAMES[read]+", the HUD at "+k+"x (wanted "+HudSize.NAMES[want]+" at "+wantK[want]+"x)"+(step?"":", WRONG"));
            }
            // fix round 1: every word the button can show fits it uncut, in the game's own body font (FantasyButton trims to its
            // width less 14), the longest, "Battle HUD: Standard (Compact scale here)" (UI Task 8b), included though this window never shows it
            var hb=hudButton(c);int room=hb==null?0:hb.getWidth()-14,widest=0;String widestLabel="none";
            for(int z=UiMath.HUD_COMPACT;z<=UiMath.HUD_LARGE;z++)for(int as=UiMath.HUD_COMPACT;as<=z;as++){String l=UiMath.hudSizeLabel(z,as);int w=c.textRenderer.getWidth(DarkUi.body(l));if(w>widest){widest=w;widestLabel=l;}}
            ok&=hb!=null&&widest<=room;steps.add("the widest words the button can show, '"+widestLabel+"', take "+widest+" px of its "+room+" px"+(hb!=null&&widest<=room?"":", WRONG: they would be cut"));
            report("p1-ui-hud-size-setting",ok,"the Visuals and sound screen ("+(c.currentScreen instanceof PresentationScreen?"open":"NOT open")+") at 1920x1080 GUI 4: "+String.join("; ",steps)+"; the notice reads '"+Presentation.notice+"'");
        }
        /** The Visuals and sound screen's Battle HUD button, or null. */
        static net.minecraft.client.gui.widget.ButtonWidget hudButton(MinecraftClient c){
            if(c.currentScreen!=null)for(var e:c.currentScreen.children())if(e instanceof net.minecraft.client.gui.widget.ButtonWidget b&&b.getMessage().getString().startsWith("Battle HUD: "))return b;
            return null;
        }
        static String hudFile(){try{return java.nio.file.Files.readString(HudSize.file());}catch(Exception e){return "unreadable ("+e.getClass().getSimpleName()+")";}}

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
            finished=true;keepLesson=false;keepRespawn=false;routSupport=false;hungerOn=false;watchHunger=false;watchBuild=false;CommandMode.selected="all";if(HudSize.get()!=UiMath.HUD_COMPACT)HudSize.set(UiMath.HUD_COMPACT);
            if(chatScale>=0){c.options.getChatScale().setValue(chatScale);chatScale=-1;}
            if(c.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)c.setScreen(null);
            UUID id=player;server(c,b->{b.testAbility=null;if(id!=null)b.respawns.remove(id);});
            if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;
        }
    }

    /** UI Task 3: the battle event feed (feed_v1), key hints from the player's own bindings, the hint layer and the map's glyphs, over
     *  four practice rounds: A on map 0 (the bindings, the hint layer, the map), B on map 0 (a point taken, losses and kills, then
     *  a line of each kind on the slip), C on
     *  map 0 (a rout, a deploy, a fallen solo commander) and D on Kingsfield (the flood). Each round goes back to the camp before the
     *  next starts, so the client sees running turn false and then true: the feed clears and the hint layer's first seconds begin
     *  again. The window is 960x540 at GUI 1 (k 1, the mock's size), but GUI 2 for the map's screenshot. The HUD is at its default
     *  size, Compact (UI Task 8c): the hint layer's first 15 s, and the feed's leaf of three lines that live 6 s. */
    static final class UiFeedChecks implements Suite,ServerWatch {
        static final double AX=-220,AZ=0; // open ground on map 0's Dawn road (UiHudChecks.AX)
        static final String NOTICE="{key:menu} opens your war council.";
        static final String LONG="The long notice of the flood check runs on well past the width of the feed slip, so the slip trims it";
        static final List<List<String>> HINT_ROWS=List.of(List.of("Not Bound","Map","K","Commands"),List.of("[","]","Turn","Left Alt","Hints")); // an unbound key in the game's own words (UI Task 8a)
        /** p1-ui-feed-kinds (Task 3 review Minor 9): one feed line of each kind, in three shots of Compact's leaf of three lines (UI Task 8c),
         *  as a Dawn player sees them: each line's kind, team and text, then the row, glyph and ink the leaf must draw it with (the inks
         *  written out, FeedLines' table: Dawn, Dusk, grey, loss red, brass, sepia). The words come from FeedLines and the sheets (the
         *  troop names and the faction names the game prints), not from literals that a renamed troop would leave behind. */
        record FeedKind(String kind,int team,String text,int row,String glyph,int argb){}
        static String side(int team){return FeedLines.teamShort(Spec.FACTIONS[team].name());}
        static final List<List<FeedKind>> FEED_KINDS=List.of(
            List.of(new FeedKind("point",0,FeedLines.pointTaken(side(0),"Mill Hamlet"),0,"hud/fgv-seal-dawn",0xff1b4a47),new FeedKind("point",1,FeedLines.pointTaken(side(1),"Trade Crossing"),1,"hud/fgv-seal-x-dusk",0xff52376d),
                new FeedKind("kill",0,FeedLines.lost(2,troop("revenant").name()),2,"hud/fgv-skull-grey",0xff47433d)),
            List.of(new FeedKind("loss",0,FeedLines.lost(3,troop("knight").name()),0,"hud/fgv-skull",0xff7a2416),new FeedKind("deploy",0,FeedLines.deploying(2,troop("knight").name()),1,"hud/fgv-sun",0xff3d3124),
                new FeedKind("down",0,FeedLines.bannerDown("Ansel"),2,"hud/fgv-banner-down",0xff5c3e00)),
            List.of(new FeedKind("up",0,FeedLines.bannerUp("Ansel"),0,"hud/fgv-banner-up",0xff5c3e00),new FeedKind("notice",0,"Capture the objectives.",1,"hud/fgv-sun",0xff3d3124),
                new FeedKind("laststand",0,FeedLines.lastStand("Infantry"),2,"hud/fgv-laststand",0xff47433d)));
        /** The banner every round of this suite fields (Phase2Checks.skirmishBanners), whatever the starter army is: a Sunweaver (two seats),
         *  twelve Order Squires and six Order Archers fill the 20 seats, so the HUD has its three groups, the morale check its twelve knights
         *  and the alt-layer check its three chips. */
        static final String FEED_BANNER="mage,^,knight*12,archer*6";
        static final List<String> AIM_ROW=List.of("LMB","Confirm","RMB","Cancel");
        static final List<String> COLOURS=List.of("red","blue","green","gold","yellow","purple","white","grey","gray","orange","cyan","aqua","black","pink","violet","brown");
        static final UUID STALE=UUID.randomUUID(); // the remembered sighting the map stage keeps 100 ticks old
        // Written by both threads. A negative stage is a hand-off: the client waits for the server task it sent to set the next stage.
        volatile int stage,want,live;volatile UUID player;volatile boolean fixed,pointOwned,mapMarks;volatile long floodAt=-1,pointTicks=-1;
        volatile String fixture="",pointName="",killed="",deployWhy="",flooded="";
        // server thread
        int floodSet;long fixedAt;
        // client thread
        int oldW,oldH,oldGui,round,after,waitFrom,seen,seen2,mark=-1,routAt,respawnAt,sentBefore,floodMost;boolean finished,campOk,battleOk,rowsOk,inFirst,respawnSeen,banner,floodOk,prepOk,slipFirst,slipLast,slipLate;
        String campWhy="",battleWhy="",rowsWhy="",prepWhy="",floodWhy="",floodCut;List<Boolean> tallFirst=List.of(),tallLate=List.of();
        boolean kindsOk=true;String kindsWhy=""; // p1-ui-feed-kinds

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
        static String feedRow(BattleHud.FeedDraw d){return "row "+d.row()+" "+d.glyph()+" #"+String.format(Locale.ROOT,"%06x",d.argb()&0xffffff)+" '"+d.text()+"'";}
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
            if(want>0){var p=sp(b);if(p!=null&&Phase2Checks.skirmishBanners(b,p,want==4?4:0,FEED_BANNER,null)){live=want;want=0;fixture(b,p,live);fixedAt=b.time;fixed=true;}}
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
                    CommandMode.selected="all";if(HudSize.get()!=UiMath.HUD_COMPACT)HudSize.set(UiMath.HUD_COMPACT); // the checks below are written for the default size
                    window(c,960,540,1);stage=-3;server(c,b->{if(b.running)b.campaign.returnLobby();stage=3;});}
                case 3->{
                    var sign=campSign(c);if(sign==null&&since(tick)<=400)return; // the client gets the sign with the camp's chunks
                    // p1-ui-hints-mouse (UI polish M6): a mouse button reads LMB, RMB or MMB outside the hint layer too: the camp sign, a
                    // notice and the lessons, with Commands on the middle button and Use on the right one (its default)
                    GravesAndGloryClient.menu.setBoundKey(net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));net.minecraft.client.option.KeyBinding.updateKeysByCode();
                    String mouseCamp=GravesAndGloryClient.campHint(),mouseNotice=HintText.expand(NOTICE,GravesAndGloryClient::hint),lessonOne=GravesAndGloryClient.lessonText(0),lessonFour=GravesAndGloryClient.lessonText(3);
                    report("p1-ui-hints-mouse",mouseCamp.startsWith("MMB / War table")&&mouseCamp.contains(" RMB on a station")&&mouseNotice.equals("MMB opens your war council.")&&lessonOne.startsWith("Press MMB, ")&&lessonFour.startsWith("Press RMB at "),"campHint() '"+mouseCamp+"'; the notice '"+NOTICE+"' reads '"+mouseNotice+"'; lesson 1 '"+lessonOne+"'; lesson 4 '"+lessonFour+"'");
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
                case 12->{ // the hint layer in the round's first seconds (15 at Compact), then the screenshot, then an order aimed
                    if(tick>=seen+2){var rows=List.copyOf(BattleHud.hintRows());long age=net.minecraft.util.Util.getMeasuringTimeMs()-BattleHud.roundShownMs;
                        rowsOk=rows.equals(HINT_ROWS)&&age<BattleHud.hintLayerMs();rowsWhy="hint rows "+rows+" "+age/1000+" s into the round (the layer's first "+BattleHud.hintLayerMs()/1000+" s)";
                        // p1-ui-feed-round-notice (UI polish M7): the round's opening notice, under the newest line, is drawn whole
                        int most=UiMath.feedLines(HudSize.get()),textW=UiMath.feedTextW(HudSize.get());
                        List<String> feedRows=new ArrayList<>();for(int r=0;r<most;r++){String t=BattleHud.texts().get("feed-"+r);if(t!=null)feedRows.add(t);}
                        String opening=GravesAndGlory.Battle.ROUND_START;
                        report("p1-ui-feed-round-notice",feedRows.contains(opening)&&c.textRenderer.getWidth(opening)<=textW,"the feed drew "+feedRows+"; the opening notice '"+opening+"' is "+c.textRenderer.getWidth(opening)+" px of the "+textW+" a line holds on "+HudSize.NAMES[HudSize.get()]+"'s leaf");
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
                    // game those read 4 and 6 (GLFW names a printable key by its character) and fit (run ui-polish1-201624). Too long for
                    // the row whole, the keys take their short names (UI Task 8a, the re-review's 'Pag...'): None for the unbound Map key,
                    // RShift; PgUp, PgDn, LAlt. Attack goes on Caps Lock (UI polish round 2), too long for the mock's places: with the army
                    // selected the key strip's one row (UI Task 8c) reads Caps, keeps its Attack pair clear of the Follow chip and ends by
                    // the strip's x + 166, Formation as Form (Kort, 2026-10-18)
                    var keysym=net.minecraft.client.util.InputUtil.Type.KEYSYM;
                    GravesAndGloryClient.menu.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_RIGHT_SHIFT));GravesAndGloryClient.mapKey.setBoundKey(net.minecraft.client.util.InputUtil.UNKNOWN_KEY);
                    GravesAndGloryClient.rotateLeft.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_PAGE_UP));GravesAndGloryClient.rotateRight.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_PAGE_DOWN));
                    GravesAndGloryClient.attack.setBoundKey(keysym.createFromCode(GLFW.GLFW_KEY_CAPS_LOCK));
                    net.minecraft.client.option.KeyBinding.updateKeysByCode();seen=tick;stage=24;}
                case 24->{
                    if(tick>=seen+2){var rows=List.copyOf(BattleHud.hintRows());var ends=List.copyOf(BattleHud.hintEnds());int[] slip=BattleHud.rects().get("hints");
                        boolean shape=rows.size()==2&&ends.size()==2&&slip!=null&&rows.get(0).size()==4&&rows.get(1).size()==5;
                        boolean on=shape&&ends.get(0)<=slip[0]+slip[2]-12&&ends.get(1)<=slip[0]+slip[2]-12; // the 164 px slip's paper ends at x 160: 8 px kept clear, as on the left
                        String shift=GravesAndGloryClient.menu.getBoundKeyLocalizedText().getString(),up=GravesAndGloryClient.rotateLeft.getBoundKeyLocalizedText().getString(),down=GravesAndGloryClient.rotateRight.getBoundKeyLocalizedText().getString();
                        boolean named=shape&&shift.equals("Right Shift")&&up.equals("Page Up")&&down.equals("Page Down")&&rows.equals(List.of(List.of("None","Map","RShift","Commands"),List.of("PgUp","PgDn","Turn","LAlt","Hints")));
                        String caps=GravesAndGloryClient.attack.getBoundKeyLocalizedText().getString();var keyRows=List.copyOf(BattleHud.keyRows());List<String> cardsWrong=new ArrayList<>();
                        for(var kr:keyRows){int[] card=BattleHud.rects().get(kr.group().equals("all")?"keys":"card-"+kr.group());var p=kr.parts();
                            boolean ok=card!=null&&p.equals(List.of("Caps","Attack","V","Follow","B","Form"))&&kr.from().get(0)>=card[0]+10&&kr.to().get(5)<=card[0]+166;
                            for(int i=0;ok&&i<5;i++)ok=kr.to().get(i)<kr.from().get(i+1);if(!ok)cardsWrong.add(kr.group());}
                        boolean cardsOk=caps.equals("Caps Lock")&&keyRows.size()==1&&keyRows.get(0).group().equals("all")&&cardsWrong.isEmpty(); // the army selected: one row, the key strip's
                        shot(c,"ui-hud-hints-long");defaults();
                        report("p1-ui-hints-long",on&&named&&cardsOk,"with Map unbound, Commands on "+shift+" and Turn on "+up+" and "+down+", the slip at "+UiHudChecks.box(slip)+" drew the rows "+rows+", ending at canvas x "+ends+" (the paper's text edge at "+(slip==null?"?":String.valueOf(slip[0]+slip[2]-12))+"); with Attack on "+caps+" and the army selected the column drew the key rows "+keyRows.stream().map(kr->kr.group()+" "+kr.parts()+" from x "+kr.from()+" to "+kr.to()+" (the plate at "+UiHudChecks.box(BattleHud.rects().get(kr.group().equals("all")?"keys":"card-"+kr.group()))+")").toList()+" (wanted one, the key strip's)"+(cardsWrong.isEmpty()?"":", WRONG on "+cardsWrong)+"; then the keys went back to "+GravesAndGloryClient.hint("menu")+", "+GravesAndGloryClient.hint("map")+", "+GravesAndGloryClient.hint("rotate_left")+", "+GravesAndGloryClient.hint("rotate_right")+" and "+GravesAndGloryClient.hint("attack"));
                        seen=tick;stage=14;}
                }
                // p1-ui-alt-layer (UI Task 8c): at Compact the hint slip hangs in the round's first 15 s (the layer's last 2 s checked
                // too), is gone a second after them and comes back while the hints key is held; the hint layer makes no card tall, the
                // selection does (the army's three chips, then ranged's card between two chips)
                case 14->{
                    if(tick>=seen+2&&!CommandMode.active()){long now=net.minecraft.util.Util.getMeasuringTimeMs();tallFirst=talls();slipFirst=BattleHud.rects().containsKey("hints");inFirst=now-BattleHud.roundShownMs<BattleHud.hintLayerMs();
                        BattleHud.roundShownMs=now-(BattleHud.hintLayerMs()-2000);seen=tick;stage=25;}
                }
                case 25->{if(tick>=seen+2){slipLast=BattleHud.rects().containsKey("hints");CommandMode.selected="ranged";BattleHud.roundShownMs=net.minecraft.util.Util.getMeasuringTimeMs()-(BattleHud.hintLayerMs()+1000);seen=tick;stage=15;}}
                case 15->{if(tick>=seen+2){tallLate=talls();slipLate=BattleHud.rects().containsKey("hints");net.minecraft.client.option.KeyBinding.setKeyPressed(hintsKey(),true);seen=tick;stage=16;}}
                case 16->{
                    net.minecraft.client.option.KeyBinding.setKeyPressed(hintsKey(),true);
                    if(tick>=seen+2){var held=talls();boolean slipHeld=BattleHud.rects().containsKey("hints"),pressed=GravesAndGloryClient.hints.isPressed();net.minecraft.client.option.KeyBinding.setKeyPressed(hintsKey(),false);CommandMode.selected="all";
                        boolean ok=BattleHud.hintLayerMs()==UiMath.HINT_LAYER_COMPACT_MS&&inFirst&&slipFirst&&tallFirst.equals(List.of(false,false,false))&&slipLast&&!slipLate&&tallLate.equals(List.of(false,true,false))&&slipHeld&&held.equals(List.of(false,true,false));
                        report("p1-ui-alt-layer",ok,"at "+HudSize.NAMES[HudSize.get()]+" the hint layer lasts "+BattleHud.hintLayerMs()/1000+" s (wanted 15); in the round's first seconds (true: "+inFirst+") the hint slip drew="+slipFirst+" and, the army selected, the plates drew tall "+tallFirst+" (wanted three chips); with 2 s of the layer left the slip drew="+slipLast+"; a second past it, ranged selected, the slip drew="+slipLate+" (wanted false) and the plates tall "+tallLate+"; with the hints key ("+GravesAndGloryClient.hint("hints")+") held (pressed "+pressed+") the slip drew="+slipHeld+" and the plates tall "+held);
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
                    String line=FeedLines.pointTaken(side(0),pointName);long n=count("point",0,line);
                    if(n==1||tick>seen+20){report("p1-ui-feed-point",n==1&&pointTicks<=1200,fixture+"; Dawn owned it "+pointTicks+" server ticks later; "+(tick-seen)+" client ticks after that the feed held "+n+" line(s) ('point', 0, '"+line+"'): "+feed());stage=32;}
                }
                case 32->{EventFeed.clear();stage=-32;server(c,b->{lossKill(b);stage=33;});}
                case 33->{ // p1-ui-feed-loss-kill
                    if(since(tick)>=60){String lostLine=FeedLines.lost(3,troop("knight").name()),slainLine=FeedLines.lost(2,troop("revenant").name());long lost=count("loss",0,lostLine),slain=count("kill",0,slainLine);
                        report("p1-ui-feed-loss-kill",lost==1&&slain==1,killed+"; 60 client ticks later the feed held "+lost+" '"+lostLine+"' (loss, 0) and "+slain+" '"+slainLine+"' (kill, 0): "+feed());stage=34;}
                }
                case 34,35,36->{ // p1-ui-feed-kinds: each set of lines on Compact's leaf alone, then its screenshot three ticks later
                    int set=stage-34;
                    if(since(tick)==0){EventFeed.clear();long now=net.minecraft.util.Util.getMeasuringTimeMs();for(var k:FEED_KINDS.get(set))EventFeed.push(k.kind(),k.team(),k.text(),now);}
                    else if(since(tick)>=3){
                        var got=List.copyOf(BattleHud.feedRows());var want=FEED_KINDS.get(set).stream().map(k->new BattleHud.FeedDraw(k.row(),k.glyph(),k.argb(),k.text())).toList();
                        boolean ok=GravesAndGloryClient.team==0&&got.equals(want);kindsOk&=ok;
                        kindsWhy+=(kindsWhy.isEmpty()?"":"; ")+"shot "+(set+1)+" (viewer team "+GravesAndGloryClient.team+"): drew "+got.stream().map(UiFeedChecks::feedRow).toList()+(ok?"":", WRONG (wanted "+want.stream().map(UiFeedChecks::feedRow).toList()+")");
                        shot(c,set==0?"ui-feed-kinds":"ui-feed-kinds-"+(set+1));
                        if(set<FEED_KINDS.size()-1)stage=35+set;
                        else{report("p1-ui-feed-kinds",kindsOk,"one line of each kind, pushed onto "+HudSize.NAMES[HudSize.get()]+"'s leaf as a Dawn player sees them: "+kindsWhy);EventFeed.clear();restart(c,3,50);}
                    }
                }
                // Round C: map 0
                case 50->{EventFeed.clear();stage=-50;server(c,b->{killTroop(b,b.squads.get(player),"knight",10);stage=51;});}
                case 51->{ // p1-ui-feed-last-stand: ten of the twelve knights die with a revenant 7 blocks off, so Morale's pass tells the owner once
                    int k=since(tick);String line=FeedLines.lastStand(Morale.label("infantry"));
                    if(count("laststand",0,line)==1){routAt=k;stage=-52;server(c,b->{ // the group leaves and takes its last stand again inside lastStandFeedSeconds: no second line; the support Sunweaver at 20 percent with no enemy near: none
                        var own=b.squads.get(player);own.lastStand.remove("infantry");
                        for(var u:b.units)if(u.squad==own&&u.spec.group().equals("support")&&u.mob.isAlive())u.mob.setHealth(u.mob.getMaxHealth()*0.2f);
                        stage=52;});}
                    else if(k>100){report("p1-ui-feed-last-stand",false,"no '"+line+"' line 100 client ticks after 10 of the 12 knights died: "+feed());stage=54;}
                }
                case 52->{
                    int k=since(tick);
                    if(k>=60){String line=FeedLines.lastStand(Morale.label("infantry")),other=FeedLines.lastStand(Morale.label("support"));var inf=UiHudChecks.group("infantry");var sup=UiHudChecks.group("support");
                        long tolds=lines().stream().filter(l->l.kind().equals("laststand")).count();int gate=Spec.COMBATDISCIPLINE[0].lastStandFeedSeconds();
                        boolean ok=count("laststand",0,line)==1&&count("laststand",0,other)==0&&tolds==1&&inf!=null&&inf.lastStand()&&sup!=null&&!sup.lastStand();
                        report("p1-ui-feed-last-stand",ok,"'"+line+"' "+routAt+" client ticks after 10 of the 12 knights died (an enemy within "+Spec.COMBATDISCIPLINE[0].lastStandRadius()+"); then the infantry left the state and took it again inside "+gate+" s and the Sunweaver fell to 20 percent with no enemy near: "+(k-0)+" client ticks later the feed holds "+tolds+" last-stand line(s) ("+count("laststand",0,line)+" for the infantry, "+count("laststand",0,other)+" for support), the infantry reads lastStand "+(inf==null?"?":inf.lastStand())+" and support "+(sup==null?"?":sup.lastStand())+": "+feed());stage=54;}
                }
                case 54->{EventFeed.clear();stage=-54;server(c,b->{deploy(b);stage=55;});}
                case 55->{ // p1-ui-feed-deploy
                    int k=since(tick);String line=FeedLines.deploying(1,troop("knight").name());long n=count("deploy",0,line);
                    if(n==1||k>400){report("p1-ui-feed-deploy",n==1,deployWhy+"; "+k+" client ticks later the feed held "+n+" '"+line+"' (deploy, 0): "+feed());stage=56;}
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
                    floodMost=0;floodCut=null;stage=-70;server(c,b->{flood(b);stage=71;});
                }
                case 71->{
                    // Compact's leaf holds three lines (UI Task 8c), so the loss lines, which FeedEvents sends 40 ticks after the deaths,
                    // push the long notice out before the 60th tick: every frame from the flood on is watched for the most rows drawn and
                    // for the long notice as drawn
                    var drawn=List.copyOf(BattleHud.feedRows());floodMost=Math.max(floodMost,drawn.size());for(var d:drawn)if(d.text().startsWith("The long"))floodCut=d.text();
                    if(since(tick)>=60){
                        int most=UiMath.feedLines(HudSize.get()),textW=UiMath.feedTextW(HudSize.get());
                        var ls=lines();var font=c.textRenderer;List<String> wide=new ArrayList<>(),rows=new ArrayList<>(),troops=new ArrayList<>();
                        for(int r=0;r<most;r++){String t=BattleHud.texts().get("feed-"+r);if(t==null)continue;rows.add(t+" ("+font.getWidth(t)+" px)");if(font.getWidth(t)>textW)wide.add(t);}
                        for(var l:ls)if(l.kind().equals("loss"))troops.add(troopOf(l.text()));
                        boolean unique=new HashSet<>(troops).size()==troops.size()&&!troops.contains("?");
                        // the long notice cut after a whole word (UI polish M7), not inside one, and inside the line
                        String cut=floodCut;
                        String kept=cut!=null&&cut.endsWith("...")?cut.substring(0,cut.length()-3):null;boolean atWord=kept!=null&&kept.length()<LONG.length()&&LONG.startsWith(kept)&&!Character.isLetterOrDigit(LONG.charAt(kept.length()))&&font.getWidth(cut)<=textW;
                        floodOk=ls.size()<=most&&floodMost<=most&&rows.size()==ls.size()&&wide.isEmpty()&&unique&&prepOk&&atWord;
                        floodWhy=fixture+"; "+flooded+"; "+HudSize.NAMES[HudSize.get()]+"'s leaf holds "+most+" lines and drew at most "+floodMost+" in a frame; 60 client ticks later "+ls.size()+" lines live: "+feed()+"; drawn "+rows+(wide.isEmpty()?", none over "+textW+" px":", over "+textW+" px: "+wide)+"; the long notice drawn as '"+cut+"'"+(cut==null?"":" ("+font.getWidth(cut)+" px)")+" (cut after a whole word: "+atWord+"); loss lines by troop "+troops+"; "+prepWhy;
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
        volatile int stage;volatile UUID player;volatile boolean serverRunning,serverOnPad,serverRoom;volatile int padCountdown=-1;
        // client thread only
        int mark=-1,waitFrom,oldW,oldH,oldGui,runAt=-1,padAt=-1;boolean finished,focusOk=true,modesOk=true,scalesOk=true,noticeOk=true,badgesOk;
        String focusWhy="",modesWhy="",gatesWhy="",scalesWhy="",noticeWhy="",badgesWhy="";
        long noticeFirst,noticeSecond;Progression.State oldProgression;JsonObject oldResult;
        /** The result the suite pretends the last battle left, held in CampaignClient.result every tick (each camp snapshot puts the server's back), so Last battle is on the home for the stages that follow. */
        JsonObject fakeResult;
        static final String PRACTICE_SOLO="Practice is solo. Use a private room to play with your party.";

        public void serverTick(GravesAndGlory.Battle b){serverRunning=b.running;var id=player;serverOnPad=id!=null&&b.campaign.onPad.containsKey(id);serverRoom=id!=null&&b.campaign.social.room(id)!=null;padCountdown=b.campaign.padCountdown;}
        /** Steps since the suite entered its current stage. */
        int since(int tick){if(mark!=stage){mark=stage;waitFrom=tick;}return tick-waitFrom;}
        static WarTableHome home(MinecraftClient c){return c.currentScreen instanceof CampScreen s&&s.page.equals("home")?s.home:null;}
        static int[] box(MinecraftClient c,String name){var h=home(c);var b=h==null?null:h.button(name);return b==null?null:b.canvasBox();}
        static double gui(MinecraftClient c,double canvas){return canvas*UiCanvas.k()/c.getWindow().getScaleFactor();}
        /** The pointer moves to the middle of a canvas box, as GLFW's cursor callback reports it. */
        static void move(MinecraftClient c,int[] b){if(b!=null&&c.currentScreen!=null)c.currentScreen.mouseMoved(gui(c,b[0]+b[2]/2.0),gui(c,b[1]+b[3]/2.0));}
        /** A left click in the middle of a canvas box, the pointer moving there first. */
        static void click(MinecraftClient c,int[] b){if(b==null||c.currentScreen==null)return;double x=gui(c,b[0]+b[2]/2.0),y=gui(c,b[1]+b[3]/2.0);var s=c.currentScreen;s.mouseMoved(x,y);s.mouseClicked(x,y,0);s.mouseReleased(x,y,0);}
        /** A key pressed and let go, as a tap reaches the screen (the table hears a held key once). */
        static void key(MinecraftClient c,int key){if(c.currentScreen!=null)c.currentScreen.keyPressed(key,0,0);if(c.currentScreen!=null)c.currentScreen.keyReleased(key,0,0);}
        static String str(int[] b){return b==null?"none":Arrays.toString(b);}
        static boolean overlap(int[] a,int[] b){return a[0]<b[0]+b[2]&&b[0]<a[0]+a[2]&&a[1]<b[1]+b[3]&&b[1]<a[1]+a[3];}
        static boolean inside(int[] a,int w,int h){return a[0]>=0&&a[1]>=0&&a[0]+a[2]<=w&&a[1]+a[3]<=h;}
        /** The widgets' canvas boxes by name, in Tab order. */
        static Map<String,int[]> boxes(MinecraftClient c){Map<String,int[]> m=new LinkedHashMap<>();var h=home(c);if(h!=null)for(var e:h.buttons.entrySet())m.put(e.getKey(),e.getValue().canvasBox());return m;}
        /** Skirmish eases flat as Practice rises; a tick on the pointer moves to Campaign and Skirmish is the third sheet: it must go on
         *  along the same curve, not start it again from where it stood (a jump of 0.2 of the rise at that tick). */
        static boolean thirdSheetKeepsItsCurve(WarTableHome src){
            String mode=CampaignClient.homeMode; // focus() sets the session's sheet; the scratch table must leave it as it was
            try{
                var t=new WarTableHome(src.screen);t.focused=1;for(int i=0;i<3;i++){t.from[i]=t.to[i]=i==1?1:0;}
                t.focus(0);t.changedAt[1]-=1; // a tick on from its start
                float before=t.rise(1,t.clock());t.focus(2);float after=t.rise(1,t.clock());
                return before>0.1f&&before<0.9f&&Math.abs(after-before)<0.05f;
            }finally{CampaignClient.homeMode=mode;}
        }
        /** The framebuffer's colour at canvas (x, y) as 0xRRGGBB (the last frame drawn), or -1. */
        static int pixel(MinecraftClient c,int x,int y){
            try(NativeImage img=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){
                int px=x*UiCanvas.k(),py=y*UiCanvas.k();if(px<0||py<0||px>=img.getWidth()||py>=img.getHeight())return -1;
                int abgr=img.getColor(px,py);return ((abgr&0xff)<<16)|(abgr&0xff00)|((abgr>>16)&0xff);
            }catch(Exception ex){return -1;}
        }
        static boolean darker(int a,int b){return ((a>>16)&0xff)<((b>>16)&0xff)&&((a>>8)&0xff)<((b>>8)&0xff)&&(a&0xff)<(b&0xff);}
        /** UI Task 8b-2 (Kort, 2026-10-22: everything fits, every page, no noted crop): what a page draws at a size it must fit, every
         *  box (the drawn art's and the widgets', the table, the rail and the candle's light apart) 12 px inside the canvas, and none
         *  under the rail (its height, 66) but what hangs on it: a box whose top is the plaques' row (y 12) or the banners' (13) is the
         *  rail's own, as is a box inside one (a banner's label); any other with its top above 66 lies over the rail (fix round 1, Minor 2: a box lifted to y 20 was missed).
         *  Empty when the page fits. */
        static List<String> fits(Map<String,int[]> drawn,Map<String,int[]> widgets){
            int w=UiCanvas.width(),h=UiCanvas.height(),railH=UiTex.entry("table/rail").h();List<String> bad=new ArrayList<>();
            Map<String,int[]> all=new LinkedHashMap<>();
            for(var e:drawn.entrySet())if(!Set.of("table","rail","glow").contains(e.getKey()))all.put(e.getKey(),e.getValue());
            for(var e:widgets.entrySet())all.put("widget "+e.getKey(),e.getValue());
            List<int[]> hung=new ArrayList<>();for(int[] a:all.values())if(a!=null&&a[1]<=13)hung.add(a); // the plaques and the banners, and what they carry
            for(var e:all.entrySet()){int[] a=e.getValue();if(a==null)continue;
                if(a[0]<12||a[1]<12||a[0]+a[2]>w-12||a[1]+a[3]>h-12)bad.add(e.getKey()+" "+str(a)+" not 12 px inside the "+w+"x"+h+" canvas");
                if(a[1]>13&&a[1]<railH&&hung.stream().noneMatch(q->a[0]>=q[0]&&a[1]>=q[1]&&a[0]+a[2]<=q[0]+q[2]&&a[1]+a[3]<=q[1]+q[3]))bad.add(e.getKey()+" "+str(a)+" under the rail");}
            return bad;
        }
        /** The big-canvas oak (UI Task 8b-2): on a canvas bigger than the 960 page the planks run to its edges. Oak's red runs over
         *  its green and its blue in every tone; the old floor (DarkUi.FLOOR, 0b0d0f), the camp's grass and stone seen past the
         *  planks do not (fix round 1: a red-over-blue test let the grass through): reads the framebuffer at the four edges' middles and where the floor
         *  used to show (half way from the 1280x720 middle to the edge). Empty when every one is oak. */
        static List<String> oakToEdges(MinecraftClient c){
            int cw=UiCanvas.width(),ch=UiCanvas.height(),ox=(cw-1280)/4,oy=(ch-720)/4;List<String> bad=new ArrayList<>();
            int[][] at={{0,ch/2},{cw-1,ch/2},{cw/2,ch-1},{ox,ch/2},{cw-1-ox,ch/2},{cw/2,ch-1-Math.max(0,oy)},{cw/4,UiTex.entry("table/rail").h()+2}};
            for(int[] p:at){int warm=0,px=pixel(c,p[0],p[1]); // five pixels along the row (a nail's iron is grey): three must be oak
                for(int dx=-2;dx<=2;dx++){int x=Math.max(0,Math.min(cw-1,p[0]+dx)),q=pixel(c,x,p[1]),r=(q>>16)&0xff;if(q>=0&&r>((q>>8)&0xff)&&r>(q&0xff))warm++;}
                if(warm<3)bad.add(p[0]+","+p[1]+" "+hex(px));}
            return bad;
        }
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
            if(fakeResult!=null)CampaignClient.result=fakeResult;
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
                    int[] drawnTable=WarTable.drawn().get("table"),drawnRail=WarTable.drawn().get("rail");var badges=WarTable.badges();
                    boolean chrome=WarTable.drawnTable().equals("table/oak-table")&&Arrays.equals(drawnTable,new int[]{-480,-270,1920,1080})&&drawnRail!=null&&drawnRail[2]==UiCanvas.width()&&drawnRail[3]==UiTex.entry("table/rail").h()&&!WarTable.drawn().containsKey("floor")&&Arrays.equals(WarTable.drawn().get("maproll"),new int[]{148,48,156,80});
                    boolean badged=Integer.valueOf(WarTableHome.contractsBadge()).equals(badges.get("Contracts"))&&Integer.valueOf(WarTableHome.friendsBadge()).equals(badges.get("Friends"))&&badges.get("Camp")!=null&&badges.get("Camp")==0;
                    report("p1-ui-home-layout",home(c)!=null&&bad.length()==0&&ride.equals("Open the war council")&&o[0]==0&&o[1]==0&&CampaignClient.homeMode.equals("skirmish")&&chrome&&badged,
                        "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", origin "+o[0]+","+o[1]+"; "+(bad.length()==0?"the 11 widget boxes as pinned":"wrong: "+bad)+"; the plate reads \""+ride+"\"; homeMode "+CampaignClient.homeMode+"; drawChrome drew "+WarTable.drawnTable()+" at "+str(drawnTable)+", the rail "+str(drawnRail)+", badges "+badges+"; field card \""+t.get("field-name")+"\" / \""+t.get("field-rule")+"\" / \""+t.get("field-teams")+"\"");
                    shot(c,"ui-home");stage=3;
                }
                case 3->{ // p1-ui-home-party: solo, the local player's seat with the live model, then the Invite seat
                    var r=WarTableHome.rects();var pt=WarTableHome.portraits();String name=c.player.getGameProfile().getName();int[] invite=box(c,"invite");
                    boolean seat=Arrays.equals(r.get("seat-0"),new int[]{56,206,48,48})&&r.containsKey("ready-0")&&!r.containsKey("seat-1")&&name.equals(WarTableHome.texts().get("seat-0"));
                    // 2 px per model pixel is size 32 for a model drawn at its own size; the player renderer draws its model at 0.9375, so 32 / 0.9375 = 34.1 reaches drawEntity
                    boolean model=pt.size()==1&&pt.get(0).model()&&pt.get(0).name().equals(name)&&pt.get(0).size()==34&&pt.get(0).feet()==282&&pt.get(0).x()==80&&Portrait.last()!=null&&Portrait.last().window()==40&&Portrait.last().tall()==0&&Portrait.last().size()==34&&Portrait.last().scale()==0.9375f&&!Portrait.last().silhouette()&&Portrait.last().feet()==282&&Portrait.last().x()==80;
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
                    else if(k==30){boolean third=thirdSheetKeepsItsCurve(h);focusOk&=third;focusWhy+="; a third sheet still easing keeps its curve when the focus moves again: "+third;}
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
                case 9->{ // p1-ui-home-gates: Skirmish, then Ride, opens the War Table lobby with a room three a side
                    int k=since(tick);
                    if(k==0){if(home(c)==null)CampScreen.open("home");return;}
                    if(k==5){click(c,box(c,"skirmish"));return;}
                    if(k==7){gatesWhy="a click on Skirmish, the plate \""+label()+"\"";click(c,box(c,"ride"));padAt=tick;return;}
                    if(k<7)return;
                    // Kort, 2026-10-30: the War Table lobby is the skirmish's main path; Ride opens it and asks the server for a room (social:lobby). The pads keep working in camp.
                    String room=CampaignClient.social.has("room")?CampaignClient.social.get("room").getAsString():"";
                    if(c.currentScreen instanceof CampScreen s&&s.page.equals("lobby")&&!room.isEmpty()){
                        gatesWhy+="; Ride: the War Table lobby opened "+(tick-padAt)+" ticks later with room "+room+" ("+CampaignClient.social.get("size").getAsInt()+" a side)";
                        report("p1-ui-home-gates",CampaignClient.social.get("size").getAsInt()==SocialHub.LOBBY_SIZE,gatesWhy);GravesAndGloryClient.send("social:exit");s.page("home");stage=11;}
                    else if(tick-padAt>60){report("p1-ui-home-gates",false,gatesWhy+"; Ride: no lobby with a room 60 ticks later (screen "+(c.currentScreen instanceof CampScreen s2?s2.page:String.valueOf(c.currentScreen))+", notice \""+CampaignClient.notice+"\")");stage=11;}
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
                    boolean wood=WarTable.tableTexture("home").equals("table/oak-table")&&WarTable.tableTexture("recruit").equals("table/oak-table-recruit")&&WarTable.tableTexture("results").equals("table/oak-table")&&WarTable.tableTexture("chapter").equals("table/oak-table-chapter")&&WarTable.tableTexture("table/oak-table-recruit").equals("table/oak-table-recruit")&&WarTable.tableTexture("table/oak-table").equals("table/oak-table")&&WarTable.tableTexture("nonesuch").equals("table/oak-table");
                    var r=WarTableHome.rects();boolean drawn=r.containsKey("glow")&&r.containsKey("candle")&&r.containsKey("inkwell");
                    report("p1-ui-home-props",bad.length()==0&&pixels>0&&wood&&drawn,pixels+" solid prop pixels (the candle's 4 frames, the inkwell's 3) against the 12 px edge and "+content.size()+" content boxes (a full party of four, every sheet flat and raised, the field card, Ride, Last battle, the notice line, the rail, every widget)"+(bad.length()==0?": all clear":bad)
                        +"; the wood: home "+WarTable.tableTexture("home")+", recruit "+WarTable.tableTexture("recruit")+" (or by name "+WarTable.tableTexture("table/oak-table-recruit")+"), results (not painted yet) "+WarTable.tableTexture("results")+", chapter "+WarTable.tableTexture("chapter")+", an unknown page "+WarTable.tableTexture("nonesuch")+"; drawn: glow "+str(r.get("glow"))+", candle "+str(r.get("candle"))+", inkwell "+str(r.get("inkwell")));
                    stage=14;
                }
                case 14->{ // p1-ui-home-notice: a refusal sent again shows again; a notice already waiting when the table opens stays hidden
                    int k=since(tick);var h=home(c);if(h==null){if(k>0)GravesAndGloryClient.send("social:exit");report("p1-ui-home-notice",false,noticeWhy+"; the home closed at step "+k+(k>0?" (the room left)":""));stage=15;return;} // leaves the room the step asked for, so later stages run outside it
                    if(k==0){click(c,box(c,"practice"));h.screen.action("social:room");noticeWhy="Practice chosen ("+CampaignClient.homeMode+"), a private room asked for";}
                    else if(k==12){
                        if(!serverRoom||!CampaignClient.homeMode.equals("practice")){noticeOk=false;noticeWhy+="; no private room ("+serverRoom+") or Practice not chosen ("+CampaignClient.homeMode+"): Enter not pressed, the room left";GravesAndGloryClient.send("social:exit");stage=15;report("p1-ui-home-notice",false,noticeWhy);return;}
                        noticeFirst=h.noticeAt;key(c,GLFW.GLFW_KEY_ENTER);}
                    else if(k==26){String t1=WarTableHome.texts().get("notice");noticeSecond=h.noticeAt;
                        noticeOk&=PRACTICE_SOLO.equals(t1)&&noticeSecond>noticeFirst;noticeWhy+="; the first Ride: the line reads \""+t1+"\", shown at tick "+noticeSecond+" (was "+noticeFirst+")";key(c,GLFW.GLFW_KEY_ENTER);}
                    else if(k==40){String t2=WarTableHome.texts().get("notice");boolean again=h.noticeAt>noticeSecond;
                        noticeOk&=PRACTICE_SOLO.equals(t2)&&again;noticeWhy+="; the same refusal again: the line reads \""+t2+"\", shown again at tick "+h.noticeAt+" (was "+noticeSecond+"), "+(again?"it showed anew":"it did not show anew");
                        click(c,box(c,"skirmish"));h.screen.action("social:exit");}
                    else if(k==60){CampScreen.open("home");}
                    else if(k==72){String stale=WarTableHome.texts().get("notice");boolean hidden=stale==null&&home(c)!=null&&!serverRoom;
                        noticeOk&=hidden;noticeWhy+="; the room left and the table opened again: "+(stale==null?"no notice shown (the one waiting is stale)":"\""+stale+"\" shown")+", room open "+serverRoom;
                        report("p1-ui-home-notice",noticeOk,noticeWhy);stage=15;}
                }
                case 15->{ // p1-ui-home-badges: the Contracts badge counts the Claim buttons the Contracts page would show, the Friends badge a waiting invitation
                    int k=since(tick);var h=home(c);if(h==null){report("p1-ui-home-badges",false,"the home closed");stage=16;return;}
                    if(k==0)oldProgression=CampaignClient.profile.progression;
                    if(k<=6){ // put in again each tick: the camp snapshot, every 10 ticks, puts the server's profile and party back
                        var st=new Progression.State();st.sanitize();st.dailyBattles=3;st.weeklyBattles=Progression.WEEKLY_BATTLES;st.claims.add("daily:0");
                        CampaignClient.profile.progression=st;CampaignClient.party.addProperty("inviteSeconds",30);h.screen.rebuild();
                    }
                    if(k>=1&&k<=7){var bd=WarTable.badges();
                        if(Integer.valueOf(2).equals(bd.get("Contracts"))&&Integer.valueOf(1).equals(bd.get("Friends"))&&WarTableHome.contractsBadge()==2&&Integer.valueOf(0).equals(bd.get("Recruit"))){badgesOk=true;badgesWhy="drawn "+bd+" with daily 1 and the weekly claimable, daily 0 claimed, daily 2 unfinished, an invitation waiting";}}
                    if(k==8){CampaignClient.profile.progression=oldProgression;CampaignClient.party.addProperty("inviteSeconds",0);h.screen.rebuild();
                        report("p1-ui-home-badges",badgesOk,badgesOk?badgesWhy:"the badges never read Contracts 2 and Friends 1: "+WarTable.badges());stage=16;}
                }
                case 16->{ // p1-ui-home-last: a result id puts Last battle under the field card, clear of Change and Ride; it stays for the scales
                    int k=since(tick);var h=home(c);if(h==null){report("p1-ui-home-last",false,"the home closed");stage=12;return;}
                    if(k==0){oldResult=CampaignClient.result;fakeResult=new JsonObject();fakeResult.addProperty("id","ui-home-fix1");CampaignClient.result=fakeResult;h.screen.rebuild();}
                    else if(k==10){
                        int[] last=box(c,"last"),change=box(c,"change"),ride=box(c,"ride"),card=WarTableHome.rects().get("field");var r=WarTableHome.rects();
                        boolean placed=Arrays.equals(last,new int[]{792,392,88,24});
                        boolean clear=last!=null&&change!=null&&ride!=null&&card!=null&&!overlap(last,change)&&!overlap(last,ride)&&!overlap(last,card)&&inside(last,UiCanvas.width(),UiCanvas.height());
                        boolean drawn=Arrays.equals(r.get("last"),last)&&"Last battle".equals(WarTableHome.texts().get("last"));
                        shot(c,"ui-home-last");
                        var s=(CampScreen)c.currentScreen;click(c,last);boolean opens=s.page.equals("results");s.page("home"); // put back before a frame draws a result that has only an id
                        report("p1-ui-home-last",placed&&clear&&drawn&&opens,"Last battle "+str(last)+" (pinned 792,392,88,24), drawn "+str(r.get("last"))+" reading \""+WarTableHome.texts().get("last")+"\"; Change "+str(change)+", Ride "+str(ride)+", field card "+str(card)+": "+(clear?"clear of all three and inside the canvas":"overlapping or outside")+"; a click opens "+(opens?"Results":"nothing"));
                        stage=12;}
                }
                case 12->{ // p1-ui-home-scales: 1280x720 at GUI 3 (canvas 1280x720), 854x480 at GUI 2 (smaller than the page), 1920x1080 at GUI 2, then canvases larger than the table: 1920x974 and 1920x1080 at GUI 1
                    int k=since(tick);
                    if(k==0){window(c,1280,720,3);return;}
                    if(k==20){int[] o=WarTable.origin();var r=WarTableHome.rects();int[] t=r.get("table"),rail=r.get("rail"),ride=box(c,"ride");int w=UiCanvas.width(),h=UiCanvas.height();
                        boolean covers=t!=null&&t[0]<=0&&t[1]<=0&&t[0]+t[2]>=w&&t[1]+t[3]>=h;
                        boolean props=r.containsKey("candle")&&r.containsKey("inkwell")&&r.containsKey("glow");
                        scalesOk&=home(c)!=null&&w==1280&&h==720&&o[0]==160&&o[1]==90&&Arrays.equals(ride,new int[]{520,478,240,40})&&rail!=null&&rail[2]==1280&&covers&&props&&box(c,"last")!=null;
                        scalesWhy="1280x720 GUI 3: canvas "+w+"x"+h+" (k "+UiCanvas.k()+"), origin "+o[0]+","+o[1]+", ride "+str(ride)+", rail "+str(rail)+", table "+str(t)+(covers?" covers the canvas":" leaves a gap")+", candle "+str(r.get("candle"))+", inkwell "+str(r.get("inkwell"));shot(c,"ui-home-720");return;}
                    if(k==22){window(c,854,480,2);return;}
                    if(k==42){int w=UiCanvas.width(),h=UiCanvas.height();var b=boxes(c);List<String> out=new ArrayList<>(),over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
                        for(int i=0;i<keys.size();i++){if(!inside(b.get(keys.get(i)),w,h))out.add(keys.get(i)+" "+str(b.get(keys.get(i))));for(int j=i+1;j<keys.size();j++)if(overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));}
                        // UI Task 8b-2: the compact page. The party in a row under the rail, the sheets' block centred under it, the candle
                        // at the foot left of Ride, the inkwell and the rolled map left off; every box 12 px inside, none under the rail
                        var r=WarTableHome.rects();var bad=fits(r,b);
                        boolean props=r.containsKey("candle")&&r.containsKey("glow")&&!r.containsKey("inkwell")&&!r.containsKey("maproll");
                        int[] field=r.get("field");boolean placed=WarTable.compact()&&Arrays.equals(field,new int[]{611,160,168,188})&&Arrays.equals(r.get("seat-0"),new int[]{75,82,48,48})&&Arrays.equals(box(c,"ride"),new int[]{219,360,240,40})&&Arrays.equals(box(c,"camp"),new int[]{12,12,96,28});
                        scalesOk&=home(c)!=null&&w==854&&h==480&&b.size()>=12&&out.isEmpty()&&over.isEmpty()&&bad.isEmpty()&&props&&placed;
                        scalesWhy+="; 854x480 GUI 2 (compact "+WarTable.compact()+"): canvas "+w+"x"+h+", "+b.size()+" widgets, outside the canvas "+out+", overlapping "+over+", not fitting "+bad+", candle "+str(r.get("candle"))+(props?", inkwell and rolled map left off":" WRONG props "+r.keySet())
                            +", seat 0 "+str(r.get("seat-0"))+", field card "+str(field)+", ride "+str(box(c,"ride"))+", camp "+str(box(c,"camp"))+(placed?"":" WRONG (pinned 75,82 / 611,160 / 219,360 / 12,12)");shot(c,"ui-home-small");return;}
                    if(k==44){window(c,1920,1080,2,true);return;}
                    if(k==64){int[] o=WarTable.origin();boolean same=Arrays.equals(box(c,"ride"),LAYOUT[10])&&Arrays.equals(box(c,"camp"),LAYOUT[0]);
                        scalesOk&=home(c)!=null&&UiCanvas.k()==2&&UiCanvas.width()==960&&UiCanvas.height()==540&&o[0]==0&&o[1]==0&&same;
                        scalesWhy+="; 1920x1080 GUI 2 (borderless): k "+UiCanvas.k()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", camp "+str(box(c,"camp"))+", ride "+str(box(c,"ride"));shot(c,"ui-home-1080");return;}
                    if(k==66){window(c,1920,974,1,true);return;}
                    if(k==86){scalesWhy+="; "+bigCanvas(c,1920,974,"ui-home-1920x974");return;}
                    if(k==88){window(c,1920,1080,1,true);return;}
                    if(k==108){scalesWhy+="; "+bigCanvas(c,1920,1080,"ui-home-1920x1080-gui1");
                        report("p1-ui-home-scales",scalesOk,scalesWhy);end(c);}
                }
                case 99->{stage=100;finish(c,"ui-home");}
                default->{}
            }
        }
        /** A big canvas (a maximized window on a 1080p screen, 1920x1080 at GUI 1): the page stays centred, the 1920x1080 oak
         *  (UI Task 8b-2) runs to every edge with no floor, the rail runs the width, every widget is inside and the props and the
         *  rolled map are drawn. Reads the framebuffer's pixels at the edges. Returns the line for the report and clears scalesOk when
         *  wrong. */
        String bigCanvas(MinecraftClient c,int w,int h,String shotName){
            int cw=UiCanvas.width(),ch=UiCanvas.height(),k=UiCanvas.k();int[] o=WarTable.origin();var r=WarTableHome.rects();var b=boxes(c);
            int tx=(cw-WarTable.TABLE_W)/2,ty=(ch-WarTable.TABLE_H)/2;
            List<String> out=new ArrayList<>(),over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
            for(int i=0;i<keys.size();i++){if(!inside(b.get(keys.get(i)),cw,ch))out.add(keys.get(i));for(int j=i+1;j<keys.size();j++)if(overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));}
            int[] t=r.get("table"),rail=r.get("rail");
            boolean placed=cw==w&&ch==h&&k==1&&o[0]==(w-960)/2&&o[1]==(h-540)/2&&Arrays.equals(t,new int[]{tx,ty,WarTable.TABLE_W,WarTable.TABLE_H})&&!r.containsKey("floor")&&rail!=null&&rail[2]==cw&&Arrays.equals(box(c,"ride"),new int[]{o[0]+360,o[1]+388,240,40});
            boolean props=r.containsKey("candle")&&r.containsKey("inkwell")&&r.containsKey("glow")&&Arrays.equals(r.get("maproll"),new int[]{o[0]+148,WarTable.MAPROLL_Y,156,80}); // the rolled map lies on the planks under the rail
            var floor=oakToEdges(c);
            boolean ok=placed&&out.isEmpty()&&over.isEmpty()&&props&&floor.isEmpty()&&b.size()>=13;
            scalesOk&=ok;shot(c,shotName);
            return w+"x"+h+" GUI 1: canvas "+cw+"x"+ch+" (k "+k+"), origin "+o[0]+","+o[1]+", table "+str(t)+", rail "+str(rail)+", ride "+str(box(c,"ride"))+", "+b.size()+" widgets, outside "+out+", overlapping "+over+", props "
                +(props?"drawn, the rolled map on the planks":"missing, or no rolled map: "+r.keySet())+"; the edges "+(floor.isEmpty()?"oak all round, no floor":"not oak at "+floor)+(ok?"":" WRONG");
        }
        static String hex(int rgb){return rgb<0?"none":String.format("%06x",rgb);}
        /** Closes the table, puts the session's sheet back on Skirmish and the window as the suite found it; p1-ui-home-suite follows. */
        void end(MinecraftClient c){finished=true;fakeResult=null;if(oldResult!=null)CampaignClient.result=oldResult;c.setScreen(null);CampaignClient.homeMode="skirmish";if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;}
    }

    /** UI plan Task 6, restated for the War Table port (Task 4): the recruit page on a fresh commander with 1000 gold, at 960x540 GUI 1
     *  (canvas 960x540, origin 0,0) unless a step says otherwise. The five Houses hang on the rod in Spec.HOUSES order and the steps
     *  name them by index (the first is the House the page opens on, the second the one they switch to); the troops, prices, words
     *  and odds come from Spec.TROOPS, RecruitView and RecruitPages.action, never from a literal, and the draw of a roll is fixed
     *  (Campaign.rollRng) on the House's first Rare so the troop that arrives is known (RosterMath.roll with the same draw).
     *  Layout: the banners and their labels, the roll with its mark and rules, the letter with its action tag and the words under
     *  it, the ladder, the three columns on one top and one foot (the foot read from the framebuffer), what reached each portrait's
     *  drawEntity, and the Odds note, which a click elsewhere closes; then the same page at 1920x1080 GUI 2 for grading. Enter held
     *  down enlists one troop of the first House, and the letter opens on it under the broken seal; the tag buys the troop shown
     *  (buy:, once, however fast it is pressed), an Epic not found shows "From letters only", a Legendary its inactive tag and why;
     *  the second banner lists the second House's roll and a click on Enlist draws from it. Broke: with 50 gold the plate turns iron,
     *  the tag goes sepia, says why and takes no press, and Enter sends nothing; a purse the server has emptied before the client saw
     *  it meets the server's refusal on the notice line; a House whose every troop is owned lists them all as recruited, a troop at
     *  full stars shows "Full stars" for its tag, and a roll:<house> sent anyway is refused with the gold untouched. Keys: with Tab's
     *  focus on the tag, Enter presses it; the underline follows the pointer or the keyboard's focus, never a click's; the wheel; a
     *  held Esc closes the note, then goes home and no further; the back and Banner plaques; B. Scales: the layout's page again (a fresh
     *  commander) at 854x480 GUI 2 (the compact page, UI Task 8b-2) and 1920x974 GUI 1 (the big-canvas oak to every edge), every
     *  line of the letter inside its paper at each size. The page never sends unlock: or army:. Keys reach the screen as GLFW sends
     *  them (a press, a held key's repeats as more presses, then the release; Tab sets the keyboard's navigation first), clicks as Mouse
     *  sends them (the navigation turns to the mouse's), and the pointer the page reads when it draws is set where a check needs it
     *  (parked off every widget otherwise). */
    static final class UiRecruitChecks implements Suite {
        static final int GOLD=1000;
        static final String H0=Spec.HOUSES[0].id(),H1=Spec.HOUSES[1].id();
        /** The action tag's box on the 960 page: the letter's corner (328, 124) plus the tag's place in it. */
        static final int[] CHOOSE={420,436,120,24};
        static final java.lang.reflect.Field MOUSE_X=mouseField("x"),MOUSE_Y=mouseField("y");
        // the server thread writes these, the client thread reads them
        volatile int stage;volatile UUID player;volatile java.util.random.RandomGenerator rngSaved;
        // client thread only
        int mark=-1,waitFrom,oldW,oldH,oldGui,sentBefore,goldBefore,serialBefore,seen=-1;long revealBefore;boolean finished,layoutOk=true,keysOk=true,scalesOk=true;
        String picked="",why="",layoutWhy="",keysWhy="",scalesWhy="",compactWant="";Set<String> before=Set.of();CampScreen table;

        int since(int tick){if(mark!=stage){mark=stage;waitFrom=tick;}return tick-waitFrom;}
        static CampScreen screen(MinecraftClient c){return c.currentScreen instanceof CampScreen s?s:null;}
        static WarTableRecruit page(MinecraftClient c){var s=screen(c);return s!=null&&s.page.equals("recruit")?s.recruit:null;}
        static int[] box(MinecraftClient c,String name){var p=page(c);var b=p==null?null:p.button(name);return b==null||!b.visible?null:b.canvasBox();}
        /** The page's visible widgets' canvas boxes by name, in Tab order. */
        static Map<String,int[]> boxes(MinecraftClient c){Map<String,int[]> m=new LinkedHashMap<>();var p=page(c);if(p!=null)for(var e:p.buttons.entrySet())if(e.getValue().visible)m.put(e.getKey(),e.getValue().canvasBox());return m;}
        static String str(int[] b){return UiHomeChecks.str(b);}
        static int[] rect(String name){return WarTableRecruit.rects().get(name);}
        static String text(String name){return WarTableRecruit.texts().get(name);}
        static Integer color(String name){return WarTableRecruit.colors().get(name);}
        static String hex(Integer c){return c==null?"none":String.format("%08x",c);}
        static String name(String troop){return ArmyCards.spec(troop).name();}
        static Set<String> owned(){return new LinkedHashSet<>(CampaignClient.profile.owned.keySet());}
        /** What a new commander owns: one copy of each Common. */
        static Set<String> start(){return new Profile().owned.keySet();}
        static String lastSent(){var s=WarTableRecruit.sent();return s.isEmpty()?"none":s.get(s.size()-1);}
        /** The rows the last frame drew, as troop ids in order. */
        static List<String> rowTroops(){List<String> out=new ArrayList<>();for(var r:WarTableRecruit.rows())out.add(r.troop());return out;}
        /** A House's troops in Spec.TROOPS order. */
        static List<String> troopsOf(String house){List<String> out=new ArrayList<>();for(var t:Spec.TROOPS)if(t.house().equals(house))out.add(t.id());return out;}
        /** The portrait the last frame drew at a place (row-(i) or letter), or null. */
        static WarTableRecruit.PortraitDraw portrait(String where){for(var p:WarTableRecruit.portraits())if(p.where().equals(where))return p;return null;}
        /** The size a portrait window asks drawEntity for: UiMath.portraitSize over the scale the troop's renderer gives its model. */
        static int size(String troop,int window){return Math.round(UiMath.portraitSize(window,0)/Portrait.modelScale(WarTableRecruit.model(troop)));}
        /** The kind line under a troop's name on the letter: its rarity and its group ("rare infantry"). */
        static String kind(String troop){var t=ArmyCards.spec(troop);return t.rarity()+" "+RecruitView.group(t.group()).toLowerCase(Locale.ROOT);}
        /** The first troop of a House's roll the commander does not own that a Buy can take (a Common or a Rare), or null. */
        static String firstBuyable(String house){for(var r:RecruitView.roll(house))if(!r.owned()&&!r.rarity().equals("epic")&&!r.rarity().equals("legendary"))return r.troopId();return null;}
        /** A draw whose every nextInt(bound) is value mod bound (Campaign.rollRng). */
        static java.util.random.RandomGenerator fixed(int value){return new java.util.random.RandomGenerator(){public long nextLong(){return value;}public int nextInt(int bound){return value%bound;}};}
        /** The draw that lands a roll on the first Rare of a House: past all its Commons' weights. */
        static int rareDraw(String house){int commons=0;for(var t:Spec.TROOPS)if(t.house().equals(house)&&t.rarity().equals("common"))commons++;return commons*RosterMath.rarityRow("common").rollWeight();}
        /** The troop a roll of the House lands on with that draw and no letters before it, as Campaign.roll draws it (RosterMath.roll). */
        static String drawn(String house,int draw,int pity){
            List<String> commons=new ArrayList<>(),rares=new ArrayList<>();String epic=null;
            for(var t:Spec.TROOPS)if(t.house().equals(house)){if(t.rarity().equals("common"))commons.add(t.id());else if(t.rarity().equals("rare"))rares.add(t.id());else if(t.rarity().equals("epic"))epic=t.id();}
            return RosterMath.roll(commons,rares,epic,RosterMath.rarityRow("common").rollWeight(),RosterMath.rarityRow("rare").rollWeight(),RosterMath.rarityRow("epic").rollWeight(),pity,Spec.RECRUITMENT[0].epicPity(),fixed(draw)).troop();
        }
        /** Whether the page has sent an unlock: or an army: since the first n commands (both are refused in Phase 2). */
        static boolean oldCommands(int from){var s=WarTableRecruit.sent();for(int i=from;i<s.size();i++)if(s.get(i).startsWith("unlock:")||s.get(i).startsWith("army:"))return true;return false;}
        /** The banner of House i on the 960 page: the rod's left end plus 8 plus 112 a banner. */
        static int bx(int i){return WarTableRecruit.rodX(Spec.HOUSES.length)+8+WarTableRecruit.BANNER_STEP*i;}

        /** A key's press as GLFW hands it over (Keyboard.onKey turns the navigation to the keyboard's before the screen hears Tab or
         *  an arrow); a held key's repeats are more presses with no release between. */
        static void press(MinecraftClient c,int key){
            if(key==GLFW.GLFW_KEY_TAB)c.setNavigationType(net.minecraft.client.gui.navigation.GuiNavigationType.KEYBOARD_TAB);
            else if(key>=GLFW.GLFW_KEY_RIGHT&&key<=GLFW.GLFW_KEY_UP)c.setNavigationType(net.minecraft.client.gui.navigation.GuiNavigationType.KEYBOARD_ARROW);
            if(c.currentScreen!=null)c.currentScreen.keyPressed(key,0,0);
        }
        static void release(MinecraftClient c,int key){if(c.currentScreen!=null)c.currentScreen.keyReleased(key,0,0);}
        /** A key pressed and let go. */
        static void tap(MinecraftClient c,int key){press(c,key);release(c,key);}
        /** A left click in the middle of a canvas box as Mouse.onMouseButton sends it: the navigation turns to the mouse's first. */
        static void click(MinecraftClient c,int[] b){c.setNavigationType(net.minecraft.client.gui.navigation.GuiNavigationType.MOUSE);UiHomeChecks.click(c,b);}
        /** The wheel turned by amount over the roll, as Mouse.onMouseScroll hands it to the screen. */
        static void wheel(MinecraftClient c,double amount){if(c.currentScreen!=null)c.currentScreen.mouseScrolled(UiHomeChecks.gui(c,176),UiHomeChecks.gui(c,300),amount);}
        static java.lang.reflect.Field mouseField(String name){try{var f=net.minecraft.client.Mouse.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException e){throw new IllegalStateException(e);}}
        /** The game's pointer at canvas (x, y), where GLFW's cursor callback would leave it (the OS cursor stays where it is): hover
         *  on the page is read from the pointer when a frame draws, not from the events the suite sends. */
        static void pointer(MinecraftClient c,double x,double y){try{MOUSE_X.setDouble(c.mouse,x*UiCanvas.k());MOUSE_Y.setDouble(c.mouse,y*UiCanvas.k());}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
        /** The pointer at the canvas's bottom-left corner, off every widget at every size. */
        static void park(MinecraftClient c){pointer(c,1,UiCanvas.height()-2);}
        static boolean hotNone(){return WarTableRecruit.underlined().isEmpty();}
        /** Where a texture's art ends as the last frame drew it: the row under the last fully opaque row of column col of the PNG the
         *  game loaded, the art's top-left at canvas (left, top); null unless the framebuffer shows that row's pixel there. */
        static Integer foot(MinecraftClient c,String name,int left,int top,int col){
            try(var in=c.getResourceManager().getResource(UiTex.entry(name).id()).orElseThrow().getInputStream();var img=NativeImage.read(in)){
                for(int y=img.getHeight()-1;y>=0;y--){int abgr=img.getColor(col,y);if((abgr>>>24)!=255)continue;
                    int rgb=((abgr&0xff)<<16)|(abgr&0xff00)|((abgr>>16)&0xff);return UiHomeChecks.pixel(c,left+col,top+y)==rgb?top+y+1:null;}
            }catch(Exception ex){return null;}
            return null;
        }
        /** The notice line where the page puts it: its first line's top at 504, centred on the ladder's axis (784), inside the page
         *  12 px from its right edge and foot. */
        static boolean noticePlaced(int[] n){int[] o=WarTable.origin();return n!=null&&n[1]==o[1]+504&&Math.abs(n[0]+n[2]/2-(o[0]+784))<=1&&n[0]+n[2]<=o[0]+948&&n[1]+n[3]<=o[1]+528;}
        /** Every line the letter writes under its name (the reason under the tag, the outcome of a roll, the words that stand for a missing
         *  tag) lies inside the paper, 8 px from its edges and above its foot, at this size; empty when it does. */
        static String letterFits(){
            var lt=rect("letter");List<String> bad=new ArrayList<>();if(lt==null)return "no letter drawn";
            for(String k:new String[]{"reason","outcome","noaction"}){var r=rect(k);if(r!=null&&(r[0]<lt[0]+8||r[0]+r[2]>lt[0]+lt[2]-8||r[1]+r[3]>lt[1]+lt[3]-8))bad.add(k+" "+UiHomeChecks.str(r)+" not 8 px inside the paper "+UiHomeChecks.str(lt));}
            return String.join("; ",bad);
        }

        public void cleanup(MinecraftClient c){
            if(c.getServer()!=null)server(c,b->{if(rngSaved!=null){b.campaign.rollRng=rngSaved;rngSaved=null;}});
        }

        public void step(MinecraftClient c,int tick){
            if(tick>7000&&!finished){report("p1-ui-recruit-timeout",false,"stuck in stage "+stage+": "+why);end(c);return;}
            switch(stage){
                case 0->{ // a fresh commander with GOLD gold at camp, the roll's draw fixed on the first House's first Rare, the window at 960x540 GUI 1
                    if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
                    c.setScreen(null);window(c,960,540,1);stage=-1;
                    server(c,b->{if(b.running)b.campaign.returnLobby();var p=new Profile();p.gold=GOLD;b.campaign.profiles.put(player,p);if(rngSaved==null)rngSaved=b.campaign.rollRng;b.campaign.rollRng=fixed(rareDraw(H0));b.campaign.save(player);stage=1;});
                }
                case 1->{ // the snapshot brings the fresh profile, then the table opens on the recruit page
                    var p=CampaignClient.profile;boolean fresh=p.gold==GOLD&&p.owned.keySet().equals(start());
                    if(GravesAndGloryClient.running||!fresh){if(since(tick)>300){report("p1-ui-recruit-layout",false,"no fresh profile with "+GOLD+" gold 300 ticks on: gold "+p.gold+", unlocked "+p.owned.keySet()+", running "+GravesAndGloryClient.running);end(c);}return;}
                    CampScreen.open("recruit");stage=2;
                }
                case 2->{ // p1-ui-recruit-layout, then the Odds note and a click elsewhere
                    int k=since(tick);
                    if(k==0){park(c);return;}
                    if(k==10){layoutWhy=layout(c);shot(c,"ui-recruit");click(c,box(c,"odds"));return;}
                    if(k==14){ // the note: the House's rules as RecruitView words them, each line inside the note
                        var font=c.textRenderer;var rules=RecruitView.note(H0);List<String> got=new ArrayList<>();for(int i=0;text("note-"+i)!=null;i++)got.add(text("note-"+i));
                        boolean wide=true;for(String g:got)wide&=font.getWidth(DarkUi.body(g))<=WarTableRecruit.NOTE_W;
                        String joined=String.join(" ",got);
                        boolean note=page(c)!=null&&page(c).odds&&Arrays.equals(rect("note"),new int[]{360,200,244,165})&&joined.equals(String.join(" ",rules))&&wide&&got.size()<=WarTableRecruit.NOTE_LINES&&!joined.contains("No duplicates")&&joined.contains(RecruitView.oddsLine(H0))
                            &&200+34+12*(got.size()-1)+9<=200+165-12;
                        layoutOk&=note;layoutWhy+="; Odds opens the note "+str(rect("note"))+": \""+joined+"\" in "+got.size()+" lines (want RecruitView.note, each inside "+WarTableRecruit.NOTE_W+" px, the odds line \""+RecruitView.oddsLine(H0)+"\", no \"No duplicates\")"+(note?"":" WRONG");
                        click(c,box(c,"row-0"));return;}
                    if(k==18){String sel=layoutSel;boolean closed=page(c)!=null&&!page(c).odds&&!WarTableRecruit.rects().containsKey("note")&&sel.equals(text("letter-troop"));
                        layoutOk&=closed;layoutWhy+="; a click on row 0 "+(closed?"closed the note and went no further (the letter still shows "+sel+")":"WRONG: note "+str(rect("note"))+", the letter "+text("letter-troop"));
                        report("p1-ui-recruit-layout",layoutOk,layoutWhy);stage=3;}
                }
                case 3->{ // the same page at 1920x1080 GUI 2 (k 2, the 960x540 page at twice the size), for grading; then back, and Enter goes down
                    int k=since(tick);
                    if(k==0){window(c,1920,1080,2,true);return;}
                    if(k==18){park(c);return;}
                    if(k==20){var o=WarTable.origin();boolean same=page(c)!=null&&UiCanvas.k()==2&&UiCanvas.width()==960&&UiCanvas.height()==540&&Arrays.equals(box(c,"enlist"),new int[]{664,354,240,40})&&Arrays.equals(rect("banner-"+H0),new int[]{bx(0),13,104,60})&&hotNone()&&letterFits().isEmpty();
                        why="1920x1080 GUI 2: k "+UiCanvas.k()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", enlist "+str(box(c,"enlist"))+", underlined "+WarTableRecruit.underlined()+", letter "+(letterFits().isEmpty()?"inside its paper":letterFits())+(same?"":" WRONG");System.out.println("[p1] ui-recruit "+why);
                        if(!same)report("p1-ui-recruit-layout-1080",false,why);shot(c,"ui-recruit-1080");window(c,960,540,1);return;}
                    if(k==30){park(c);return;}
                    if(k==32){table=screen(c);goldBefore=CampaignClient.profile.gold;before=owned();sentBefore=WarTableRecruit.sent().size();revealBefore=CampaignClient.revealUntil;press(c,GLFW.GLFW_KEY_ENTER);stage=4;}
                }
                case 4->{ // p1-ui-recruit-enlist: Enter held down (the press, two repeats 400 ms apart, past the 300 ms throttle, then the release) sends one roll:<house>;
                          // within 70 ticks the troop the fixed draw lands on is, for the price of a letter, on the letter under the broken seal
                    int k=since(tick);
                    if(k==8||k==16){press(c,GLFW.GLFW_KEY_ENTER);return;} // the held key's repeats, each another keyPressed
                    if(k==20){release(c,GLFW.GLFW_KEY_ENTER);return;}
                    if(k<46)return; // room for a second roll, had a repeat sent one, to come back
                    String want=drawn(H0,rareDraw(H0),0);var now=owned();now.removeAll(before);
                    boolean arrived=now.equals(Set.of(want))&&CampaignClient.profile.gold==goldBefore-RecruitView.cost()&&CampaignClient.revealUntil!=revealBefore;
                    if(!arrived&&k<=70)return;
                    String id=now.isEmpty()?"none":now.iterator().next();
                    boolean ok=arrived&&id.equals(text("letter-troop"))&&Arrays.equals(rect("broken"),new int[]{463,108,35,36})&&UiMath.pityLine(RecruitView.pityLeft(H0)).equals(text("rung"))&&kind(id).equals(text("letter-line1"))
                        &&!WarTableRecruit.texts().containsKey("letter-outcome")&&WarTableRecruit.sent().size()==sentBefore+1&&("roll:"+H0).equals(lastSent())&&!oldCommands(sentBefore)&&screen(c)==table&&table.held.isEmpty()&&!WarTableRecruit.texts().containsKey("notice");
                    report("p1-ui-recruit-enlist",ok,"Enter held (a press, two repeats, the release) sent "+(WarTableRecruit.sent().size()-sentBefore)+" command(s), the last "+lastSent()+"; the fixed draw lands on "+want+", new "+now+", gold "+goldBefore+" -> "+CampaignClient.profile.gold+", reveal "+CampaignClient.revealUnit+"; the letter shows "+text("letter-troop")+" (\""+text("letter-line1")+"\"), broken seal "+str(rect("broken"))+", rung \""+text("rung")+"\", held keys after the release "+(table==null?"none":table.held)+", notice line "+(text("notice")==null?"none (the recruit's own notice came with the reveal)":"\""+text("notice")+"\"")+(arrived?"":", nothing arrived within 70 ticks"));
                    shot(c,"ui-recruit-enlist");stage=5;
                }
                case 5->{ // p1-ui-recruit-choose: a click on a troop of the first House a Buy can take and not owned, then the tag pressed twice in a row (one buy), then an Epic not found and a Legendary
                    int k=since(tick);
                    if(k==0){picked=firstBuyable(H0);int at=-1;var rows=WarTableRecruit.rows();for(var r:rows)if(r.troop().equals(picked))at=r.index();
                        if(picked==null||at<0){report("p1-ui-recruit-choose",false,"no troop of the first House left for a Buy: rows "+rowTroops());stage=6;return;}
                        click(c,box(c,"row-"+at));why="row "+at+" ("+picked+") clicked";return;}
                    if(k==3){var a=RecruitView.action(picked);
                        boolean shown=a!=null&&picked.equals(text("letter-troop"))&&!WarTableRecruit.rects().containsKey("broken")&&Arrays.equals(rect("choose"),CHOOSE)&&Arrays.equals(box(c,"choose"),CHOOSE)&&a.command().equals("buy:"+picked)&&a.active()
                            &&a.label().equals(text("choose"))&&("buy:"+picked).equals(text("choose-command"))&&Integer.valueOf(DarkUi.INK_TEXT).equals(color("choose"))&&a.tip().equals(text("reason"))&&Integer.valueOf(DarkUi.SEPIA).equals(color("reason"))&&letterFits().isEmpty();
                        why+="; the letter shows "+text("letter-troop")+(shown?" with the tag \""+text("choose")+"\" ("+text("choose-command")+") and under it \""+text("reason")+"\"":" WRONG: tag \""+text("choose")+"\" "+text("choose-command")+" "+str(rect("choose"))+" ink "+hex(color("choose"))+" with "+CampaignClient.profile.gold+" gold, reason \""+text("reason")+"\", broken "+str(rect("broken"))+", "+letterFits());
                        if(!shown){report("p1-ui-recruit-choose",false,why);stage=6;return;}
                        goldBefore=CampaignClient.profile.gold;sentBefore=WarTableRecruit.sent().size();click(c,box(c,"choose"));return;}
                    if(k==4){click(c,box(c,"choose"));return;} // the second press 50 ms on: the throttle and the page's wait for the camp's answer must turn it away
                    if(k>4&&k<48)return; // the reveal reaches the page, and a second press that slipped through would have reached the purse
                    if(k==48){boolean done=CampaignClient.profile.owned.containsKey(picked)&&CampaignClient.profile.gold==goldBefore-RecruitView.chooseCost();
                        buyOk=done&&("buy:"+picked).equals(lastSent())&&WarTableRecruit.sent().size()==sentBefore+1&&!oldCommands(sentBefore)&&picked.equals(text("letter-troop"))&&WarTableRecruit.rects().containsKey("broken")&&!WarTableRecruit.texts().containsKey("notice");
                        why+="; the tag pressed twice 50 ms apart sent "+WarTableRecruit.sent().subList(sentBefore,WarTableRecruit.sent().size())+", "+picked+(CampaignClient.profile.owned.containsKey(picked)?" recruited":" not recruited")+", gold "+goldBefore+" -> "+CampaignClient.profile.gold+" (one buy costs "+RecruitView.chooseCost()+"), the letter "+text("letter-troop")+" under the seal "+str(rect("broken"))+", notice line "+text("notice")+(buyOk?"":" WRONG");
                        for(var t:Spec.TROOPS)if(t.house().equals(H0)){if(t.rarity().equals("epic"))epicId=t.id();if(t.rarity().equals("legendary"))legId=t.id();}
                        int ei=-1;for(var r:WarTableRecruit.rows())if(r.troop().equals(epicId))ei=r.index();if(ei>=0)click(c,box(c,"row-"+ei));return;}
                    if(k==52){ // the Epic not found: its words in place of a tag
                        epicOk=epicId!=null&&epicId.equals(text("letter-troop"))&&!owned().contains(epicId)&&RecruitView.action(epicId)==null&&!RecruitView.noAction(epicId).isEmpty()&&RecruitView.noAction(epicId).equals(text("letter-none"))
                            &&!WarTableRecruit.rects().containsKey("choose")&&box(c,"choose")==null&&letterFits().isEmpty();
                        why+="; the Epic "+epicId+" not found: the letter says \""+text("letter-none")+"\" in place of a tag (tag "+str(rect("choose"))+")"+(epicOk?"":" WRONG");
                        int li=-1;for(var r:WarTableRecruit.rows())if(r.troop().equals(legId))li=r.index();if(li>=0)click(c,box(c,"row-"+li));return;}
                    if(k==56){ // the Legendary not found: its tag, inactive, and why in words
                        var a=legId==null?null:RecruitView.action(legId);var p=page(c);
                        legOk=a!=null&&legId.equals(text("letter-troop"))&&!a.active()&&a.label().equals(text("choose"))&&a.command().equals(text("choose-command"))&&a.command().startsWith("legendary:"+H0+":")&&p!=null&&p.choose!=null&&p.choose.visible&&!p.choose.active
                            &&Integer.valueOf(DarkUi.SEPIA).equals(color("choose"))&&!a.tip().isEmpty()&&a.tip().equals(text("reason"))&&Integer.valueOf(DarkUi.INK_TEXT).equals(color("reason"))&&letterFits().isEmpty();
                        why+="; the Legendary "+legId+" not found: the tag \""+text("choose")+"\" ("+text("choose-command")+") is inactive and under it \""+text("reason")+"\""+(legOk?"":" WRONG");
                        shot(c,"ui-recruit-legendary");report("p1-ui-recruit-choose",buyOk&&epicOk&&legOk,why);stage=6;}
                }
                case 6->{ // p1-ui-recruit-dusk: the second banner lists the second House's roll, its label not underlined (chosen, and a click's focus); a click on Enlist sends roll:<house> and the troop of the fixed draw arrives
                    int k=since(tick);
                    if(k==0){park(c);click(c,box(c,"house-"+H1));return;}
                    if(k==4){var tx=WarTableRecruit.textures();List<String> want=troopsOf(H1);var have=owned();
                        boolean inks=WarTableRecruit.rows().size()==want.size();StringBuilder rowWhy=new StringBuilder();for(var row:WarTableRecruit.rows()){boolean mine=have.contains(row.troop());inks&=row.color()==(mine?DarkUi.INK_TEXT:DarkUi.SEPIA)&&row.silhouette()==!mine&&row.name().equals(name(row.troop()));rowWhy.append(row.troop()).append(row.color()==DarkUi.INK_TEXT?" ink":row.color()==DarkUi.SEPIA?" sepia":" "+hex(row.color())).append(row.silhouette()?" (silhouette)":"").append(", ");}
                        var lp=portrait("letter");var ld=lp==null?null:lp.drawn();String first=null;for(var r:RecruitView.roll(H1))if(first==null&&!r.owned())first=r.troopId();
                        boolean listed=rowTroops().equals(want)&&inks&&("recruit/banner-"+H1+"-on").equals(tx.get("banner-"+H1))&&("recruit/banner-"+H0).equals(tx.get("banner-"+H0))
                            &&Arrays.equals(rect("banner-"+H1),new int[]{bx(1),13,104,60})&&Arrays.equals(rect("banner-"+H0),new int[]{bx(0),13,104,50})&&Integer.valueOf(DarkUi.IVORY).equals(color("banner-"+H1))
                            &&first!=null&&first.equals(text("letter-troop"))&&ld!=null&&ld.size()==size(first,160)&&UiMath.pityLine(RecruitView.pityLeft(H1)).equals(text("rung"))&&hotNone()&&!start().contains(drawn(H1,rareDraw(H0),0));
                        why="the second banner: rows "+rowWhy+"banners "+H0+" "+tx.get("banner-"+H0)+" "+H1+" "+tx.get("banner-"+H1)+" "+str(rect("banner-"+H1))+" label "+hex(color("banner-"+H1))+", letter "+text("letter-troop")+" portrait "+ld+", rung \""+text("rung")+"\", underlined "+WarTableRecruit.underlined()+(listed?"":" WRONG");
                        shot(c,"ui-recruit-dusk");
                        if(!listed){report("p1-ui-recruit-dusk",false,why);stage=7;return;}
                        goldBefore=CampaignClient.profile.gold;before=owned();sentBefore=WarTableRecruit.sent().size();click(c,box(c,"enlist"));return;}
                    if(k>4){String want=drawn(H1,rareDraw(H0),0);var now=owned();now.removeAll(before);boolean arrived=now.equals(Set.of(want))&&CampaignClient.profile.gold==goldBefore-RecruitView.cost();
                        if(!arrived&&k<=45)return;
                        if(arrived&&k<48)return;
                        String id=now.isEmpty()?"none":now.iterator().next();
                        boolean ok=arrived&&("roll:"+H1).equals(lastSent())&&WarTableRecruit.sent().size()==sentBefore+1&&id.equals(text("letter-troop"))&&WarTableRecruit.rects().containsKey("broken")&&UiMath.pityLine(RecruitView.pityLeft(H1)).equals(text("rung"));
                        report("p1-ui-recruit-dusk",ok,why+"; a click on Enlist sent "+lastSent()+", the fixed draw lands on "+want+", new "+now+", gold "+goldBefore+" -> "+CampaignClient.profile.gold+", the letter "+text("letter-troop")+" under the seal "+str(rect("broken"))+", rung \""+text("rung")+"\"");
                        shot(c,"ui-recruit-dusk-enlist");stage=7;}
                }
                case 7->{ // p1-ui-recruit-broke: the server leaves 50 gold
                    stage=-1;server(c,b->{b.campaign.profile(player).gold=50;b.campaign.save(player);stage=71;});
                }
                case 71->{ // the snapshot brings the 50 gold, then the first banner
                    if(CampaignClient.profile.gold!=50){if(since(tick)>300){report("p1-ui-recruit-broke",false,"gold never reached 50: "+CampaignClient.profile.gold);stage=8;}return;}
                    click(c,box(c,"house-"+H0));stage=72;
                }
                case 72->{ // the iron plate: "Need <cost>"; the tag on an owned Common goes sepia, says why in words and takes no press; Enter sends nothing; then a purse the client still shows full
                    int k=since(tick);
                    if(k==4){var tx=WarTableRecruit.textures();
                        boolean iron="table/btn-iron-240x40".equals(tx.get("enlist"))&&"Enlist".equals(text("enlist"))&&Integer.valueOf(DarkUi.IVORY).equals(color("enlist"))&&("Need "+RecruitView.cost()).equals(text("enlist-cost"))&&Integer.valueOf(DarkUi.ALARM_TEXT).equals(color("enlist-cost"))
                            &&UiMath.pityLine(RecruitView.pityLeft(H0)).equals(text("rung"))&&"50".equals(text("purse"))&&Arrays.equals(rect("banner-"+H0),new int[]{bx(0),13,104,60});
                        why="50 gold on the first House: plate "+tx.get("enlist")+" \""+text("enlist")+"\" "+hex(color("enlist"))+", \""+text("enlist-cost")+"\" "+hex(color("enlist-cost"))+", purse "+text("purse")+", rung \""+text("rung")+"\""+(iron?"":" WRONG");
                        picked=troopsOf(H0).get(0); // an owned Common: a Buy would add a copy, and 50 gold is short of its price
                        if(!iron||!CampaignClient.profile.owned.containsKey(picked)){report("p1-ui-recruit-broke",false,why+(CampaignClient.profile.owned.containsKey(picked)?"":"; "+picked+" is not owned"));stage=8;return;}
                        click(c,box(c,"row-0"));return;}
                    if(k==8){var p=page(c);int[] ch=box(c,"choose");var a=RecruitView.action(picked);
                        boolean sepia=a!=null&&!a.active()&&picked.equals(text("letter-troop"))&&p!=null&&p.choose!=null&&p.choose.visible&&!p.choose.active&&Integer.valueOf(DarkUi.SEPIA).equals(color("choose"))&&Arrays.equals(ch,CHOOSE)
                            &&a.tip().equals(text("reason"))&&a.tip().contains(String.valueOf(RecruitView.chooseCost()))&&a.tip().contains("50")&&Integer.valueOf(DarkUi.INK_TEXT).equals(color("reason"))&&letterFits().isEmpty();
                        why+="; "+picked+" selected: the tag "+str(ch)+(p!=null&&p.choose!=null&&p.choose.active?" active":" inactive")+" \""+text("choose")+"\", ink "+hex(color("choose"))+", under it \""+text("reason")+"\" in "+hex(color("reason"))+(sepia?"":" WRONG");
                        if(!sepia){report("p1-ui-recruit-broke",false,why);stage=8;return;}
                        pointer(c,ch[0]+ch[2]/2.0,ch[1]+ch[3]/2.0);sentBefore=WarTableRecruit.sent().size();click(c,ch);return;}
                    if(k==12){boolean dead=!WarTableRecruit.underlined().contains("choose")&&WarTableRecruit.sent().size()==sentBefore;
                        why+="; the pointer over it and a click: "+(dead?"no underline, nothing sent":"WRONG: underlined "+WarTableRecruit.underlined()+", sent "+(WarTableRecruit.sent().size()-sentBefore));
                        if(!dead){report("p1-ui-recruit-broke",false,why);stage=8;return;}
                        park(c);before=owned();sentBefore=WarTableRecruit.sent().size();revealBefore=CampaignClient.revealUntil;tap(c,GLFW.GLFW_KEY_ENTER);return;}
                    if(k==38){boolean still=CampaignClient.profile.gold==50&&owned().equals(before)&&CampaignClient.revealUntil==revealBefore&&WarTableRecruit.sent().size()==sentBefore;
                        why+="; Enter: "+(still?"nothing sent, gold 50, no reveal":"WRONG: sent "+(WarTableRecruit.sent().size()-sentBefore)+", gold "+CampaignClient.profile.gold+", reveal "+(CampaignClient.revealUntil!=revealBefore));
                        if(!still){report("p1-ui-recruit-broke",false,why);stage=8;return;}
                        // the server holds 50 but the client still shows 300 (a purse spent elsewhere since the last snapshot): Enter sends roll:<house> and the server refuses it
                        serialBefore=CampaignClient.noticeSerial;seen=-1;CampaignClient.profile.gold=300;page(c).refresh();sentBefore=WarTableRecruit.sent().size();tap(c,GLFW.GLFW_KEY_ENTER);return;}
                    if(k>38){
                        if(CampaignClient.noticeSerial==serialBefore){if(k<=90)return;}
                        else if(seen<0){seen=k;return;}
                        else if(k<seen+3)return; // the page takes the notice and a frame draws it
                        int[] n=rect("notice");String told=CampaignClient.notice;
                        boolean refused=("roll:"+H0).equals(lastSent())&&WarTableRecruit.sent().size()==sentBefore+1&&!told.isEmpty()&&told.contains(String.valueOf(RecruitView.cost()))&&told.equals(text("notice"))&&noticePlaced(n)
                            &&Integer.valueOf(DarkUi.IVORY).equals(color("notice"))&&CampaignClient.profile.gold==50&&owned().equals(before);
                        why+="; the client shown 300 while the server holds 50: Enter sent "+lastSent()+", the server says \""+told+"\", the notice line reads \""+text("notice")+"\" at "+str(n)+" "+hex(color("notice"))+", gold "+CampaignClient.profile.gold+(refused?"":" WRONG");
                        if(!refused){report("p1-ui-recruit-broke",false,why);stage=8;return;}
                        seen=-1;stage=-1;
                        server(c,b->{var p=b.campaign.profile(player);for(var t:Spec.TROOPS)if(t.house().equals(H0))p.owned.putIfAbsent(t.id(),new Profile.Owned());var full=p.owned.get(troopsOf(H0).get(0));full.stars=Spec.RECRUITMENT[0].maxStars();b.campaign.save(player);stage=73;});}
                }
                case 73->{ // every troop of the first House owned on the server, the first of them at full stars; once the client has them, Odds
                    boolean all=true;for(String id:troopsOf(H0))all&=CampaignClient.profile.owned.containsKey(id);
                    var first=CampaignClient.profile.owned.get(troopsOf(H0).get(0));all&=first!=null&&first.stars>=Spec.RECRUITMENT[0].maxStars();
                    if(!all){if(since(tick)>300){report("p1-ui-recruit-broke",false,why+"; the first House's troops never all arrived: "+CampaignClient.profile.owned.keySet());stage=8;}return;}
                    if(since(tick)<3)return;
                    click(c,box(c,"row-0"));stage=731; // the client's own diff of newly owned troops reveals the last of them (the Lord Marshal) and the page selects it: the first troop is picked again
                }
                case 731->{if(since(tick)<3)return;if(!picked.equals(text("letter-troop"))){if(since(tick)>60){report("p1-ui-recruit-broke",false,why+"; the first troop never came back on the letter: "+text("letter-troop"));stage=8;}return;}click(c,box(c,"odds"));stage=74;}
                case 74->{ // every troop owned: the roll lists them all as recruited and the first, at full stars, has no tag but says so; Esc closes the note and the page stays; a roll:<house> sent anyway is refused
                    int k=since(tick);
                    if(k==3){var tx=WarTableRecruit.textures();
                        boolean inks=WarTableRecruit.rows().size()==troopsOf(H0).size();for(var row:WarTableRecruit.rows())inks&=row.color()==DarkUi.INK_TEXT&&!row.silhouette();
                        boolean full=picked.equals(text("letter-troop"))&&"Full stars".equals(text("letter-none"))&&RecruitView.noAction(picked).equals(text("letter-none"))&&!WarTableRecruit.rects().containsKey("choose")&&box(c,"choose")==null&&RecruitView.action(picked)==null&&!WarTableRecruit.rects().containsKey("reason")&&letterFits().isEmpty();
                        boolean found="table/btn-iron-240x40".equals(tx.get("enlist"))&&("Need "+RecruitView.cost()).equals(text("enlist-cost"))&&page(c)!=null&&page(c).odds&&String.join(" ",noteLines()).equals(String.join(" ",RecruitView.note(H0)));
                        why+="; every troop of the first House owned: rows "+(inks?"all ink, no silhouette":"WRONG")+", "+picked+" at "+Spec.RECRUITMENT[0].maxStars()+" stars: the letter says \""+text("letter-none")+"\" in place of a tag (tag "+str(rect("choose"))+", reason "+str(rect("reason"))+")"+(full?"":" WRONG")+", plate "+tx.get("enlist")+" \""+text("enlist-cost")+"\", Odds note "+str(rect("note"))+(found?"":" WRONG");
                        shot(c,"ui-recruit-broke");
                        if(!inks||!full||!found){report("p1-ui-recruit-broke",false,why);stage=8;return;}
                        tap(c,GLFW.GLFW_KEY_ESCAPE);return;}
                    if(k==6){boolean closed=page(c)!=null&&!page(c).odds&&!WarTableRecruit.rects().containsKey("note");
                        why+="; Esc "+(closed?"closed the note and the page stayed":"WRONG: note "+str(rect("note"))+", page "+(page(c)!=null));
                        if(!closed){report("p1-ui-recruit-broke",false,why);stage=8;return;}
                        // the page sends nothing now; a roll:<house> sent anyway must be refused by the server, gold untouched
                        serialBefore=CampaignClient.noticeSerial;seen=-1;before=owned();GravesAndGloryClient.send("roll:"+H0);return;}
                    if(k>6){
                        if(CampaignClient.noticeSerial==serialBefore){if(k<=60)return;}
                        else if(seen<0){seen=k;return;}
                        else if(k<seen+3)return;
                        String told=CampaignClient.notice;
                        boolean refused=!told.isEmpty()&&told.contains(String.valueOf(RecruitView.cost()))&&told.equals(text("notice"))&&noticePlaced(rect("notice"))&&CampaignClient.profile.gold==50&&owned().equals(before);
                        why+="; roll:"+H0+" sent anyway: notice \""+told+"\" (serial "+serialBefore+" to "+CampaignClient.noticeSerial+"), the line reads \""+text("notice")+"\" at "+str(rect("notice"))+", gold "+CampaignClient.profile.gold+(refused?"":" WRONG");
                        report("p1-ui-recruit-broke",refused,why);seen=-1;stage=8;}
                }
                case 8->{stage=9;}
                case 9->{ // p1-ui-recruit-keys: the page open with GOLD gold again, so the tag can buy
                    if(page(c)==null)CampScreen.open("recruit");
                    stage=-1;server(c,b->{b.campaign.profile(player).gold=GOLD;b.campaign.save(player);stage=91;});
                }
                case 91->{ // the snapshot brings GOLD; the second banner
                    if(CampaignClient.profile.gold!=GOLD||page(c)==null){if(since(tick)>300){report("p1-ui-recruit-keys",false,"no recruit page with "+GOLD+" gold: gold "+CampaignClient.profile.gold+", page "+(page(c)!=null));stage=10;}return;}
                    page(c).odds=false; // a note left open by an earlier check would take the click
                    park(c);click(c,box(c,"house-"+H1));stage=92;
                }
                case 92->{ // a troop of the second House a Buy can take, by the mouse; Tab to the tag; Enter presses the tag, not Enlist
                    int k=since(tick);
                    if(k==3){picked=firstBuyable(H1);int at=-1;for(var r:WarTableRecruit.rows())if(r.troop().equals(picked))at=r.index();
                        if(picked==null||at<0){report("p1-ui-recruit-keys",false,"no troop of the second House left for a Buy: rows "+rowTroops());stage=10;return;}
                        click(c,box(c,"row-"+at));return;}
                    if(k==6){var p=page(c);if(p==null){report("p1-ui-recruit-keys",false,"the page closed before Tab");stage=10;return;}int n=0;while(n<40&&!(p.choose!=null&&p.choose.isFocused())){tap(c,GLFW.GLFW_KEY_TAB);n++;}
                        keysWhy=picked+" selected by a click; "+n+" Tab presses put the focus on "+(p.choose!=null&&p.choose.isFocused()?"the tag":"something else");return;}
                    if(k==9){var p=page(c);boolean focus=p!=null&&p.choose!=null&&p.choose.isFocused()&&p.choose.active&&c.getNavigationType().isKeyboard()&&WarTableRecruit.underlined().equals(Set.of("choose"));
                        keysOk&=focus;keysWhy+=", underlined "+WarTableRecruit.underlined()+(focus?"":" WRONG");
                        goldBefore=CampaignClient.profile.gold;sentBefore=WarTableRecruit.sent().size();tap(c,GLFW.GLFW_KEY_ENTER);return;}
                    if(k>9){boolean done=CampaignClient.profile.owned.containsKey(picked)&&CampaignClient.profile.gold==goldBefore-RecruitView.chooseCost();
                        if(!done&&k<=50)return;
                        if(done&&k<53)return;
                        boolean ok=done&&WarTableRecruit.sent().size()==sentBefore+1&&("buy:"+picked).equals(lastSent())&&!oldCommands(sentBefore)&&picked.equals(text("letter-troop"));
                        keysOk&=ok;keysWhy+="; Enter sent "+WarTableRecruit.sent().subList(sentBefore,WarTableRecruit.sent().size())+", "+picked+(CampaignClient.profile.owned.containsKey(picked)?" recruited":" not recruited")+", gold "+goldBefore+" -> "+CampaignClient.profile.gold+(ok?"":" WRONG");
                        stage=93;}
                }
                case 93->{ // the underline: not for a click's focus; under the pointer on Odds and on the House not chosen, not on the chosen one
                    int k=since(tick);
                    if(k==0){click(c,box(c,"row-0"));park(c);return;}
                    if(k==3){boolean none=hotNone();keysOk&=none;keysWhy+="; row 0 clicked, the pointer away: underlined "+WarTableRecruit.underlined()+(none?"":" WRONG");
                        int[] b=box(c,"odds");pointer(c,b[0]+b[2]/2.0,b[1]+b[3]/2.0);return;}
                    if(k==5){boolean odds=WarTableRecruit.underlined().equals(Set.of("odds"));keysOk&=odds;keysWhy+=", over Odds "+WarTableRecruit.underlined()+(odds?"":" WRONG");
                        int[] b=box(c,"house-"+H1);pointer(c,b[0]+b[2]/2.0,b[1]+b[3]/2.0);return;}
                    if(k==7){boolean chosen=hotNone();keysOk&=chosen;keysWhy+=", over the chosen second banner "+WarTableRecruit.underlined()+(chosen?"":" WRONG");
                        int[] b=box(c,"house-"+H0);pointer(c,b[0]+b[2]/2.0,b[1]+b[3]/2.0);return;}
                    if(k==9){boolean other=WarTableRecruit.underlined().equals(Set.of("house-"+H0));keysOk&=other;keysWhy+=", over the first banner "+WarTableRecruit.underlined()+(other?"":" WRONG");
                        park(c);stage=94;}
                }
                case 94->{ // the wheel: ten notches toward the player, ten away, a sideways turn (0) and two small turns; the roll stays between its ends
                    int k=since(tick);var p=page(c);
                    if(p==null){keysOk=false;keysWhy+="; the page closed before the wheel";stage=95;return;}
                    var roll=RecruitView.roll(p.house);int last=Math.max(0,roll.size()-WarTableRecruit.ROWS);
                    if(k==0){StringBuilder w=new StringBuilder();boolean ok=true;
                        for(int i=0;i<10;i++)wheel(c,-1);ok&=p.scroll==last&&p.wheel==0;w.append("ten notches down: first row ").append(p.scroll);
                        for(int i=0;i<10;i++)wheel(c,1);ok&=p.scroll==0&&p.wheel==0;w.append(", ten up: ").append(p.scroll);
                        wheel(c,0);ok&=p.scroll==0&&p.wheel==0;w.append(", a sideways turn: ").append(p.scroll);
                        wheel(c,-0.4);wheel(c,-0.4);ok&=p.scroll==0&&(last==0?p.wheel==0:Math.abs(p.wheel-0.8)<1e-9);w.append(", two turns of 0.4 down: ").append(p.scroll).append(" carrying ").append(p.wheel);
                        keysOk&=ok;keysWhy+="; the wheel over a roll of "+roll.size()+" (last first row "+last+"): "+w+(ok?"":" WRONG");return;}
                    if(k==2){List<String> want=new ArrayList<>();for(int i=p.scroll;i<Math.min(roll.size(),p.scroll+WarTableRecruit.ROWS);i++)want.add(roll.get(i).troopId());
                        boolean rows=rowTroops().equals(want);keysOk&=rows;keysWhy+=", rows drawn "+rowTroops()+(rows?"":" WRONG");
                        click(c,box(c,"odds"));stage=95;}
                }
                case 95->{ // a held Esc: the press closes the note, its repeat does nothing; let go, a press goes home and its repeat leaves the table open; then the plaques and B
                    int k=since(tick);
                    if(k==2){boolean open=page(c)!=null&&page(c).odds;keysOk&=open;keysWhy+="; Odds "+(open?"open":"WRONG: not open");table=screen(c);press(c,GLFW.GLFW_KEY_ESCAPE);return;}
                    if(k==4){boolean shut=page(c)!=null&&!page(c).odds;keysOk&=shut;keysWhy+="; Esc held: the press "+(shut?"closed the note":"WRONG: left the note open");press(c,GLFW.GLFW_KEY_ESCAPE);return;}
                    if(k==6){boolean stays=page(c)!=null;keysOk&=stays;keysWhy+=", a repeat "+(stays?"left the page where it was":"WRONG: went home");release(c,GLFW.GLFW_KEY_ESCAPE);press(c,GLFW.GLFW_KEY_ESCAPE);return;}
                    if(k==8){var s=screen(c);boolean home=s!=null&&s==table&&s.page.equals("home");keysOk&=home;keysWhy+="; let go and pressed again: "+(home?"home":"WRONG: "+(s==null?"the table closed":s.page));press(c,GLFW.GLFW_KEY_ESCAPE);return;}
                    if(k==10){var s=screen(c);boolean open=s!=null&&s==table&&s.page.equals("home");keysOk&=open;keysWhy+=", its repeat "+(open?"left the table open on home":"WRONG: "+(s==null?"closed the table":s.page));release(c,GLFW.GLFW_KEY_ESCAPE);
                        boolean clear=table!=null&&table.held.isEmpty();keysOk&=clear;keysWhy+=", held after the release "+(table==null?"?":table.held)+(clear?"":" WRONG");
                        click(c,UiHomeChecks.box(c,"recruit"));return;}
                    if(k==13){var p=page(c);boolean back=p!=null&&p.house.equals(H0);keysOk&=back;keysWhy+="; home's Recruit plaque "+(back?"reopened the page on the first House":"WRONG: "+(p==null?"no page":p.house));click(c,box(c,"back"));return;}
                    if(k==16){var s=screen(c);boolean home=s!=null&&s.page.equals("home");keysOk&=home;keysWhy+="; the back plaque "+(home?"went home":"WRONG: "+(s==null?"the table closed":s.page));click(c,UiHomeChecks.box(c,"recruit"));return;}
                    if(k==19){boolean again=page(c)!=null;keysOk&=again;if(!again)keysWhy+="; WRONG: home's Recruit plaque did not reopen the page";click(c,box(c,"banner"));return;}
                    if(k==22){var s=screen(c);boolean banner=s!=null&&s.page.equals("banner")&&s.pages!=null;keysOk&=banner;keysWhy+="; reopened, the Banner plaque "+(banner?"opened the Banner page":"WRONG: "+(s==null?c.currentScreen:s.page));CampScreen.open("recruit");return;}
                    if(k==25){boolean again=page(c)!=null;keysOk&=again;if(!again)keysWhy+="; WRONG: the page did not open again";press(c,GLFW.GLFW_KEY_B);return;}
                    if(k==27){var s=screen(c);boolean banner=s!=null&&s.page.equals("banner")&&s.pages!=null;keysOk&=banner;keysWhy+="; reopened, B "+(banner?"opened the Banner page":"WRONG: "+(s==null?c.currentScreen:s.page));release(c,GLFW.GLFW_KEY_B);
                        report("p1-ui-recruit-keys",keysOk,keysWhy);CampScreen.open("recruit");stage=10;}
                }
                case 10->{ // p1-ui-recruit-scales: the page as the layout step saw it (a fresh commander with GOLD gold)
                    stage=-1;server(c,b->{var p=new Profile();p.gold=GOLD;b.campaign.profiles.put(player,p);b.campaign.save(player);stage=101;});
                }
                case 101->{ // the snapshot brings the fresh profile; the page opens anew, at 854x480 GUI 2 (the compact page, UI Task 8b-2)
                    var p=CampaignClient.profile;boolean fresh=p.gold==GOLD&&p.owned.keySet().equals(start());
                    if(!fresh){if(since(tick)>300){report("p1-ui-recruit-scales",false,"no fresh profile with "+GOLD+" gold 300 ticks on: gold "+p.gold+", unlocked "+p.owned.keySet());end(c);}return;}
                    CampScreen.open("recruit");window(c,854,480,2);stage=102;
                }
                case 102->{ // then 1920x974 GUI 1 (the big-canvas oak to the edges, UI Task 8b-2)
                    int k=since(tick);
                    if(k==18){park(c);return;}
                    if(k==20){scalesWhy=small(c);shot(c,"ui-recruit-854");stage=1021;}
                }
                case 1021->{ // the letter's reveal at 854x480: Enlist lands the House's first Rare, a new recruit under the broken seal
                    int k=since(tick);
                    if(k==2){compactWant=drawn(H0,rareDraw(H0),0);sentBefore=WarTableRecruit.sent().size();goldBefore=CampaignClient.profile.gold;click(c,box(c,"enlist"));return;}
                    var r=CampaignClient.lastRoll;
                    if(k>2&&r!=null&&r.troop().equals(compactWant)&&CampaignClient.rollShowing()){CampaignClient.revealHeldAt=600;stage=1022;return;}
                    if(k>300){scalesOk=false;scalesWhy+="; WRONG: the roll's letter never showed at 854x480: lastRoll "+r;CampaignClient.revealHeldAt=-1;stage=1026;}
                }
                case 1022->{
                    int k=since(tick);
                    if(k==4){var bad=UiHomeChecks.fits(WarTableRecruit.rects(),boxes(c));String lf=letterFits();
                        boolean ok=WarTable.compact()&&compactWant.equals(text("letter-troop"))&&WarTableRecruit.rects().containsKey("broken")&&text("letter-outcome")==null&&bad.isEmpty()&&lf.isEmpty()&&("roll:"+H0).equals(lastSent())&&CampaignClient.profile.gold==goldBefore-RecruitView.cost();
                        scalesOk&=ok;scalesWhy+="; at 854x480 a new recruit's letter: "+text("letter-troop")+" under the seal "+str(rect("broken"))+", not fitting "+bad+", letter lines "+(lf.isEmpty()?"inside the paper":lf)+", tag \""+text("choose")+"\" under it \""+text("reason")+"\""+(ok?"":" WRONG");
                        shot(c,"ui-recruit-854-roll");CampaignClient.revealHeldAt=-1;stage=1023;}
                }
                case 1023->{ // the reveal ends of itself; then the same draw lands on the troop it just raised: a copy, no seal, the star lit
                    int k=since(tick);
                    if(CampaignClient.rollShowing()){if(k>200){scalesOk=false;scalesWhy+="; WRONG: the first reveal never ended";stage=1026;}return;}
                    if(k<4)return;
                    goldBefore=CampaignClient.profile.gold;click(c,box(c,"enlist"));stage=1024;
                }
                case 1024->{
                    int k=since(tick);var r=CampaignClient.lastRoll;
                    if(k>2&&r!=null&&r.troop().equals(compactWant)&&r.duplicate()&&CampaignClient.rollShowing()){CampaignClient.revealHeldAt=600;stage=1025;return;}
                    if(k>300){scalesOk=false;scalesWhy+="; WRONG: the copy's letter never showed at 854x480: lastRoll "+r;CampaignClient.revealHeldAt=-1;stage=1026;}
                }
                case 1025->{
                    int k=since(tick);
                    if(k==4){var r=CampaignClient.lastRoll;var bad=UiHomeChecks.fits(WarTableRecruit.rects(),boxes(c));String lf=letterFits();String want=r==null?"":RecruitPages.outcome(r,CampaignClient.profile);
                        boolean ok=compactWant.equals(text("letter-troop"))&&!WarTableRecruit.rects().containsKey("broken")&&!want.isEmpty()&&want.equals(text("letter-outcome"))&&bad.isEmpty()&&lf.isEmpty()&&CampaignClient.profile.gold==goldBefore-RecruitView.cost();
                        scalesOk&=ok;scalesWhy+="; the copy's letter: \""+text("letter-outcome")+"\" with stars "+text("letter-stars")+", no seal "+(WarTableRecruit.rects().containsKey("broken")?"WRONG":"drawn")+", not fitting "+bad+", letter lines "+(lf.isEmpty()?"inside the paper":lf)+(ok?"":" WRONG");
                        shot(c,"ui-recruit-854-duplicate");CampaignClient.revealHeldAt=-1;stage=1026;}
                }
                case 1026->{ // the reveal over, the Odds note open: every line inside the page, nothing cut
                    int k=since(tick);
                    if(CampaignClient.rollShowing()){if(k>200){scalesOk=false;scalesWhy+="; WRONG: the reveal never ended";stage=1028;}return;}
                    if(k<4)return;
                    click(c,box(c,"odds"));stage=1027;
                }
                case 1027->{
                    int k=since(tick);
                    if(k==4){var bad=UiHomeChecks.fits(WarTableRecruit.rects(),boxes(c));var lines=noteLines();
                        boolean ok=page(c)!=null&&page(c).odds&&!lines.isEmpty()&&String.join(" ",lines).equals(String.join(" ",RecruitView.note(H0)))&&bad.isEmpty()&&rect("note")!=null;
                        scalesOk&=ok;scalesWhy+="; the Odds note at 854x480 "+str(rect("note"))+" holds "+lines.size()+" lines, not fitting "+bad+(ok?"":" WRONG");
                        shot(c,"ui-recruit-854-odds");stage=1028;}
                }
                case 1028->{
                    if(page(c)!=null)page(c).odds=false;stage=1029;
                }
                case 1029->{ // then 1920x974 GUI 1 (the big-canvas oak to the edges, UI Task 8b-2)
                    int k=since(tick);
                    if(k==2){window(c,1920,974,1,true);return;}
                    if(k==20){park(c);return;}
                    if(k==22){scalesWhy+="; "+big(c);shot(c,"ui-recruit-1920x974");return;}
                    if(k==24){report("p1-ui-recruit-scales",scalesOk,scalesWhy);end(c);}
                }
                case 99->{stage=100;finish(c,"ui-recruit");}
                default->{}
            }
        }
        /** The Odds note's lines as the last frame drew them (note-0, note-1 ...). */
        static List<String> noteLines(){List<String> got=new ArrayList<>();for(int i=0;text("note-"+i)!=null;i++)got.add(text("note-"+i));return got;}
        /** The troop the layout step found selected (the first of the roll not owned), read when it checked the letter. */
        String layoutSel="";
        /** The layout of the page on a fresh commander (p1-ui-recruit-layout): returns what it read, and sets layoutOk. The first House's roll, its first unowned troop on the letter. */
        String layout(MinecraftClient c){
            var r=page(c);var rows=WarTableRecruit.rows();var tx=WarTableRecruit.textures();var hs=Spec.HOUSES;var have=start();
            int[] o=WarTable.origin();boolean canvas=UiCanvas.width()==960&&UiCanvas.height()==540&&UiCanvas.k()==1&&o[0]==0&&o[1]==0;
            // the rod and the five banners: the chosen one longer and in its bright cloth, every label centred 52 px into its cloth, 27 down, inside it
            boolean banners=Arrays.equals(rect("rod"),new int[]{196,12,112*hs.length+8,10});StringBuilder bw=new StringBuilder();
            for(int i=0;i<hs.length;i++){String id=hs[i].id();boolean on=i==0;int[] want={bx(i),13,104,on?60:50},lb=rect("label-"+id);
                boolean ok=Arrays.equals(rect("banner-"+id),want)&&Arrays.equals(box(c,"house-"+id),want)&&("recruit/banner-"+id+(on?"-on":"")).equals(tx.get("banner-"+id))&&hs[i].name().equals(text("banner-"+id))
                    &&Integer.valueOf(i==0?DarkUi.INK_TEXT:DarkUi.IVORY).equals(color("banner-"+id))&&lb!=null&&Math.abs(lb[0]+lb[2]/2-(bx(i)+52))<=1&&lb[1]==40&&lb[0]>=bx(i)+4&&lb[0]+lb[2]<=bx(i)+100;
                banners&=ok;bw.append(id).append(' ').append(str(rect("banner-"+id))).append(' ').append(tx.get("banner-"+id)).append(" \"").append(text("banner-"+id)).append("\" ").append(hex(color("banner-"+id))).append(" label ").append(str(lb)).append(ok?"":" WRONG").append("; ");}
            // the roll: the first House's six in Spec.TROOPS order, ink when owned and sepia (a silhouette) when not; the mark on the first unowned, the rules between rows
            List<String> want=troopsOf(H0);int sel=-1;for(int i=0;i<want.size();i++)if(sel<0&&!have.contains(want.get(i)))sel=i;
            layoutSel=sel<0?"":want.get(sel);
            boolean inks=rows.size()==want.size();StringBuilder rowWhy=new StringBuilder();
            for(var row:rows){boolean mine=have.contains(row.troop());inks&=row.color()==(mine?DarkUi.INK_TEXT:DarkUi.SEPIA)&&row.silhouette()==!mine&&row.name().equals(name(row.troop()));rowWhy.append(row.troop()).append(row.color()==DarkUi.INK_TEXT?" ink":row.color()==DarkUi.SEPIA?" sepia":" "+hex(row.color())).append(row.silhouette()?" (silhouette)":"").append(", ");}
            boolean marks=sel>=0&&Arrays.equals(rect("mark"),new int[]{52,140+56*sel,234,58})&&!WarTableRecruit.rects().containsKey("rule-"+sel);
            for(int i=1;i<want.size();i++)if(i!=sel)marks&=Arrays.equals(rect("rule-"+i),new int[]{72,140+56*i,208,1});
            boolean roll=rowTroops().equals(want)&&inks&&marks;
            // live portraits: 40 px on the roll at 2 px per model pixel, 160 px in the letter at 8, each over its renderer's scale
            boolean portraits=true;StringBuilder ptWhy=new StringBuilder();
            for(int i=0;i<want.size();i++){var p=portrait("row-"+i);var d=p==null?null:p.drawn();portraits&=d!=null&&p.troop().equals(want.get(i))&&d.window()==40&&d.x()==84&&d.top()==148+56*i&&d.feet()==148+56*i+UiMath.portraitFeet(40,0)&&d.size()==size(want.get(i),40)&&d.silhouette()==!have.contains(want.get(i))&&d.left()==64;
                ptWhy.append(d==null?"none":d.size()+"@"+d.x()+","+d.feet()).append(" ");}
            // the letter: the first unowned troop, its kind and price, and its action tag with the words under it
            String sid=layoutSel;var sp=ArmyCards.spec(sid);var act=RecruitView.action(sid);var lp=portrait("letter");var ld=lp==null?null:lp.drawn();
            boolean letter=sp!=null&&act!=null&&sid.equals(text("letter-troop"))&&name(sid).equals(text("letter-name"))&&Integer.valueOf(DarkUi.INK_TEXT).equals(color("letter-name"))
                &&ld!=null&&ld.window()==160&&ld.x()==480&&ld.top()==164&&ld.feet()==164+UiMath.portraitFeet(160,0)&&ld.size()==size(sid,160)&&!ld.silhouette()
                &&Arrays.equals(rect("window"),new int[]{400,164,160,160})&&kind(sid).equals(text("letter-line1"))&&Integer.valueOf(DarkUi.SEPIA).equals(color("letter-line1"))
                &&("Deploys in battle for "+sp.cost()+" gold").equals(text("letter-line2"))&&Integer.valueOf(DarkUi.INK_TEXT).equals(color("letter-line2"))&&!WarTableRecruit.rects().containsKey("broken")
                &&Arrays.equals(box(c,"choose"),CHOOSE)&&Arrays.equals(rect("choose"),CHOOSE)&&act.label().equals(text("choose"))&&("buy:"+sid).equals(text("choose-command"))&&act.command().equals("buy:"+sid)&&act.active()
                &&Integer.valueOf(DarkUi.INK_TEXT).equals(color("choose"))&&act.tip().equals(text("reason"))&&Integer.valueOf(DarkUi.SEPIA).equals(color("reason"))&&letterFits().isEmpty();
            boolean ladder="Enlist".equals(text("enlist"))&&Integer.valueOf(DarkUi.PLATE_INK).equals(color("enlist"))&&String.valueOf(RecruitView.cost()).equals(text("enlist-cost"))&&Integer.valueOf(DarkUi.PLATE_INK).equals(color("enlist-cost"))
                &&"table/btn-brass-240x40".equals(tx.get("enlist"))&&Arrays.equals(rect("enlist"),new int[]{664,354,240,40})
                &&String.valueOf(GOLD).equals(text("purse"))&&rect("purse")!=null&&rect("purse")[1]==406&&Math.abs(rect("purse")[0]+rect("purse")[2]/2-784)<=1&&UiMath.pityLine(RecruitView.pityLeft(H0)).equals(text("rung"))&&Integer.valueOf(DarkUi.MUTED).equals(color("rung"))
                &&Arrays.equals(rect("bundle"),new int[]{664,124,240,218})&&Arrays.equals(rect("odds"),new int[]{752,468,64,24})&&!WarTableRecruit.rects().containsKey("notice");
            // one top and one foot for the three columns: the roll's, the letter's and the bundle's art drawn from 124; the paper's
            // foot (the row under the last opaque row at the art's middle column, its pixel read back from the framebuffer) of
            // the roll, the letter and the Odds tag within 3 px above 492
            int[] rl=rect("roll"),lt=rect("letter"),bd=rect("bundle"),od=rect("odds");
            Integer rf=rl==null?null:foot(c,"recruit/muster-roll",rl[0],rl[1],rl[2]/2),lf=lt==null?null:foot(c,"recruit/letter-hi",lt[0],lt[1],lt[2]/2),of=od==null?null:foot(c,"table/btn-vellum-64x24",od[0],od[1],od[2]/2);
            boolean baseline=Arrays.equals(rl,new int[]{41,124,270,372})&&Arrays.equals(lt,new int[]{328,124,310,376})&&bd!=null&&bd[1]==124
                &&rf!=null&&lf!=null&&of!=null&&rf>=489&&rf<=492&&lf>=489&&lf<=492&&of>=489&&of<=492;
            boolean chrome="table/oak-table-recruit".equals(tx.get("table"))&&Arrays.equals(box(c,"back"),new int[]{56,12,120,28})&&Arrays.equals(box(c,"banner"),new int[]{800,12,104,28})
                &&!WarTableRecruit.rects().containsKey("maproll")&&hotNone();
            int[] sb=box(c,"sheet"),nm=rect("name"); // the widget that opens the troop sheet covers the letter's name and little more
            boolean sheet=sb!=null&&nm!=null&&sb[0]<=nm[0]&&sb[0]+sb[2]>=nm[0]+nm[2]&&sb[1]<=nm[1]&&sb[1]+sb[3]>=nm[1]+11&&sb[2]<=nm[2]+10&&sb[1]+sb[3]<=rect("letter")[1]+240&&!WarTableRecruit.underlined().contains("sheet");
            layoutOk=r!=null&&canvas&&banners&&roll&&portraits&&letter&&ladder&&baseline&&chrome&&sheet&&boxes(c).size()==2+hs.length+WarTableRecruit.ROWS+4;
            return "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" k "+UiCanvas.k()+(canvas?"":" WRONG")+"; rod "+str(rect("rod"))+", banners "+bw+(banners?"":"WRONG")
                +"; roll "+rowTroops()+": "+rowWhy+"mark "+str(rect("mark"))+", rules "+str(rect("rule-1"))+" "+str(rect("rule-2"))+" "+str(rect("rule-3"))+" "+str(rect("rule-4"))+" "+str(rect("rule-5"))+(roll?"":" WRONG")+"; portraits (size@x,feet) "+ptWhy+(portraits?"":"WRONG")
                +"; letter "+text("letter-troop")+" \""+text("letter-name")+"\" "+hex(color("letter-name"))+" / \""+text("letter-line1")+"\" "+hex(color("letter-line1"))+" / \""+text("letter-line2")+"\" "+hex(color("letter-line2"))+", portrait "+ld+", window "+str(rect("window"))+", tag \""+text("choose")+"\" ("+text("choose-command")+") "+str(box(c,"choose"))+" "+hex(color("choose"))+", under it \""+text("reason")+"\" "+hex(color("reason"))+(letter?"":" WRONG")
                +"; enlist \""+text("enlist")+"\" "+hex(color("enlist"))+" for "+text("enlist-cost")+" on "+tx.get("enlist")+", purse "+text("purse")+" "+str(rect("purse"))+", rung \""+text("rung")+"\" "+hex(color("rung"))+", bundle "+str(bd)+(ladder?"":" WRONG")
                +"; columns: roll "+str(rl)+", letter "+str(lt)+", bundle "+str(bd)+" from 124; the paper's foot (framebuffer) roll "+rf+", letter "+lf+", Odds tag "+of+(baseline?" (one foot at 492)":" WRONG")
                +"; table "+tx.get("table")+", back "+str(box(c,"back"))+", banner "+str(box(c,"banner"))+", "+boxes(c).size()+" widgets, no rolled map, underlined "+WarTableRecruit.underlined()+(chrome?"":" WRONG")
                +"; the sheet widget "+str(sb)+" over the name "+str(nm)+(sheet?"":" WRONG");
        }
        boolean buyOk,epicOk,legOk;String epicId,legId;
        /** 854x480 GUI 2, the first-launch window: the compact page (UI Task 8b-2; Kort, 2026-10-22: everything fits, no crop). The
         *  three columns close up and rise (roll box 27, letter 286, ladder 600 on its axis at 720), the rung's line and Odds share a
         *  row under the purse, the Banner plaque stands 12 px from the right edge, and the five banners hang from the rod inside the
         *  canvas clear of both plaques. Pins what lands where: every widget inside the canvas, none overlapping, every box the page
         *  drew and every widget 12 px inside, none under the rail (UiHomeChecks.fits), and the letter's lines inside its paper. */
        String small(MinecraftClient c){
            int w=UiCanvas.width(),h=UiCanvas.height();var b=boxes(c);var r=WarTableRecruit.rects();
            List<String> out=new ArrayList<>(),over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
            for(int i=0;i<keys.size();i++){if(!UiHomeChecks.inside(b.get(keys.get(i)),w,h))out.add(keys.get(i)+" "+str(b.get(keys.get(i))));for(int j=i+1;j<keys.size();j++)if(UiHomeChecks.overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));}
            var bad=UiHomeChecks.fits(r,b);String letterBad=letterFits();
            int[] roll=r.get("roll"),enlist=box(c,"enlist"),rung=r.get("rung"),odds=box(c,"odds");
            boolean row=rung!=null&&odds!=null&&rung[0]+rung[2]<=odds[0]&&rung[1]>=odds[1]&&rung[1]+rung[3]<=odds[1]+odds[3]; // the rung's line and Odds on one row, either side of the axis
            boolean ok=page(c)!=null&&WarTable.compact()&&w==854&&h==480&&UiCanvas.k()==1&&b.size()==2+Spec.HOUSES.length+WarTableRecruit.ROWS+4&&out.isEmpty()&&over.isEmpty()&&bad.isEmpty()&&letterBad.isEmpty()&&row
                &&Arrays.equals(roll,new int[]{12,92,270,372})&&Arrays.equals(enlist,new int[]{600,322,240,40})&&Arrays.equals(r.get("bundle"),new int[]{600,92,240,218})&&Arrays.equals(r.get("letter"),new int[]{286,92,310,376})&&Arrays.equals(box(c,"choose"),new int[]{378,404,120,24})
                &&Arrays.equals(box(c,"back"),new int[]{12,12,120,28})&&Arrays.equals(box(c,"banner"),new int[]{738,12,104,28})&&Arrays.equals(odds,new int[]{724,408,64,24})&&hotNone();
            scalesOk&=ok;
            return "854x480 GUI 2 (compact "+WarTable.compact()+"): canvas "+w+"x"+h+" (k "+UiCanvas.k()+"), "+b.size()+" widgets, outside the canvas "+out+", overlapping "+over+", not fitting "+bad+", letter lines "+(letterBad.isEmpty()?"inside the paper":letterBad)
                +"; roll "+str(roll)+", letter "+str(r.get("letter"))+", choose "+str(box(c,"choose"))+", reason "+str(r.get("reason"))+", bundle "+str(r.get("bundle"))+", enlist "+str(enlist)+", rung "+str(rung)+" and odds "+str(odds)+(row?" on one row":" WRONG: not on one row")+", back "+str(box(c,"back"))+", banner "+str(box(c,"banner"))+", the first banner "+str(box(c,"house-"+H0))+(ok?"":" WRONG");
        }
        /** 1920x974 GUI 1, a maximized window on a 1080p screen: the page stays centred on the big-canvas oak (UI Task 8b-2), which
         *  runs to every edge with no floor (read from the framebuffer as home's check reads it), the rail runs the width with the
         *  banners hanging from it, every widget is inside and the letter's lines are inside its paper. */
        String big(MinecraftClient c){
            int cw=UiCanvas.width(),ch=UiCanvas.height(),k=UiCanvas.k();int[] o=WarTable.origin();var r=WarTableRecruit.rects();var b=boxes(c);
            int tx=(cw-WarTable.TABLE_W)/2,ty=(ch-WarTable.TABLE_H)/2;
            List<String> out=new ArrayList<>(),over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
            for(int i=0;i<keys.size();i++){if(!UiHomeChecks.inside(b.get(keys.get(i)),cw,ch))out.add(keys.get(i));for(int j=i+1;j<keys.size();j++)if(UiHomeChecks.overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));}
            int[] t=r.get("table"),rail=r.get("rail"),enlist=box(c,"enlist"),roll=r.get("roll"),first=box(c,"house-"+H0);String letterBad=letterFits();
            boolean placed=cw==1920&&ch==974&&k==1&&o[0]==480&&o[1]==217&&Arrays.equals(t,new int[]{tx,ty,WarTable.TABLE_W,WarTable.TABLE_H})&&!r.containsKey("floor")&&rail!=null&&rail[2]==cw
                &&Arrays.equals(enlist,new int[]{o[0]+664,o[1]+354,240,40})&&Arrays.equals(roll,new int[]{o[0]+41,o[1]+124,270,372})&&Arrays.equals(first,new int[]{o[0]+bx(0),13,104,60})&&!r.containsKey("maproll")
                &&Arrays.equals(r.get("bundle"),new int[]{o[0]+664,o[1]+124,240,218})&&Arrays.equals(box(c,"choose"),new int[]{o[0]+CHOOSE[0],o[1]+CHOOSE[1],CHOOSE[2],CHOOSE[3]});
            var floor=UiHomeChecks.oakToEdges(c);
            boolean ok=page(c)!=null&&placed&&out.isEmpty()&&over.isEmpty()&&floor.isEmpty()&&letterBad.isEmpty()&&b.size()==2+Spec.HOUSES.length+WarTableRecruit.ROWS+4&&hotNone();
            scalesOk&=ok;
            return "1920x974 GUI 1: canvas "+cw+"x"+ch+" (k "+k+"), origin "+o[0]+","+o[1]+", table "+str(t)+", rail "+str(rail)+", the first banner "+str(first)+", roll "+str(roll)+", bundle "+str(r.get("bundle"))+", enlist "+str(enlist)+", choose "+str(box(c,"choose"))+", "+b.size()+" widgets, outside "+out+", overlapping "+over+", letter lines "+(letterBad.isEmpty()?"inside the paper":letterBad)
                +"; the edges "+(floor.isEmpty()?"oak all round, no floor":"not oak at "+floor)+(ok?"":" WRONG");
        }
        /** Closes the table and puts the window back as the suite found it; p1-ui-recruit-suite follows. */
        void end(MinecraftClient c){finished=true;c.setScreen(null);if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;}
    }

    /** UI plan Task 7: the War Table's other pages (WarTablePages) on a fresh commander with 300 gold, at 960x540 GUI 1 (canvas
     *  960x540, origin 0,0) unless a step says otherwise. Each of Banner, Contracts, Chapter, Friends and the Field opens from home's
     *  plaque (Change for the field); then its layout (layout: every visible widget and every piece of art 12 px inside the page, the
     *  widgets clear of each other and all the War Table's own, the back plaque and the rail as home has it with the open page's
     *  plaque lit, the page's own oak, its focal title or, on the Chapter, its chapter block), a screenshot (ui-page-<name>), one
     *  real action through the server, the page again at 1920x1080 GUI 2 for grading (ui-page-<name>-1080, the same layout) and the
     *  back plaque home: one p1-ui-page-<name> each (the Chapter's claimable and claimed states shot at k 2 as well, beside the
     *  approved mockups, and so are the Contracts' claim and the Field's war gates). Then every page at 1280x720 GUI 3, 854x480
     *  GUI 2 and 1920x974 GUI 1 (p1-ui-page-scales), and home's field
     *  card on each of the five maps at k 2 (p1-ui-page-cards). Then Task 6's carried items: Tab's focus survives a rebuild, a direct one and
     *  one on a change of the camp state (p1-ui-page-focus); the menu key already down when the table opens does not close it until
     *  it is let go (p1-ui-page-held-open); a held Enter on home rides once (p1-ui-page-held-home). Keys, clicks and the pointer
     *  reach the screen as UiRecruitChecks sends them. */
    static final class UiPagesChecks implements Suite,ServerWatch {
        static final String[] PAGES={"banner","contracts","chapter","friends","field"};
        static final String REROLLED="Third contract changed to tactical orders. One free reroll per day.";
        static final Set<String> REROLL_REFUSALS=Set.of("Your free daily reroll is already used.","That contract is already claimed.");
        /** One of the server's reroll refusals. Null-safe (Set.of throws on null): a notice line no frame has drawn yet is none. */
        static boolean refused(String notice){return notice!=null&&REROLL_REFUSALS.contains(notice);}
        // the server thread writes these, the client thread reads them
        volatile int stage,serverCountdown=-1;volatile UUID player;volatile boolean serverRoom,serverOnPad;volatile String serverDifficulty="";
        // client thread only
        int mark=-1,waitFrom,oldW,oldH,oldGui,index,sub,subAt,serial,serial1,serial2,menuCode=-1,gold=-1,landedAt=-1,origMap=-1,cardAsked=-1,fieldRow,goldBefore,xpBefore,pick;boolean finished,ok=true,slid,scalesOk=true,cardsOk=true;
        String why="",before="",focusName="",scalesWhy="",cardsWhy="";List<String> roll=List.of();String at0="",at19="";CampScreen table;
        /** Claims with a House (War Table port Task 1): the House the banner leans on, the shards it held before a claim, and what the claims about to be pressed pay (worked out by the game's own rule, grant). */
        String house="";int shardsBefore;Progression.Grant gA,gW,gT;
        final Set<Integer> rollSeen=new TreeSet<>(),oathSeen=new TreeSet<>();

        public void serverTick(GravesAndGlory.Battle b){var id=player;serverRoom=id!=null&&b.campaign.social.room(id)!=null;serverDifficulty=b.campaign.difficulty;
            serverOnPad=id!=null&&b.campaign.onPad.containsKey(id);serverCountdown=b.campaign.padCountdown;}
        /** The commander off the war gates, back where the camp puts its commanders. */
        void stepOff(MinecraftClient c){server(c,b->{var p=b.server.getPlayerManager().getPlayer(player);if(p!=null)b.campaign.toLobby(p);});}
        /** Box a lies inside box b. */
        static boolean in(int[] a,int[] b){return a!=null&&b!=null&&a[0]>=b[0]&&a[1]>=b[1]&&a[0]+a[2]<=b[0]+b[2]&&a[1]+a[3]<=b[1]+b[3];}
        int since(int tick){if(mark!=stage){mark=stage;waitFrom=tick;}return tick-waitFrom;}
        /** Moves a page's action on a step; at(tick) counts the ticks since. */
        void next(int tick){sub++;subAt=tick;}
        int at(int tick){return tick-subAt;}
        static CampScreen screen(MinecraftClient c){return c.currentScreen instanceof CampScreen s?s:null;}
        static WarTablePages pages(MinecraftClient c){var s=screen(c);return s!=null&&s.pages!=null&&s.pages.page.equals(s.page)?s.pages:null;}
        static UiButton button(MinecraftClient c,String name){var p=pages(c);return p==null?null:p.button(name);}
        static int[] box(MinecraftClient c,String name){var b=button(c,name);return b==null||!b.visible?null:b.canvasBox();}
        /** The page's visible widgets' canvas boxes by name, in Tab order. */
        static Map<String,int[]> boxes(MinecraftClient c){Map<String,int[]> m=new LinkedHashMap<>();var p=pages(c);if(p!=null)for(var e:p.buttons.entrySet())if(e.getValue().visible)m.put(e.getKey(),e.getValue().canvasBox());return m;}
        static String str(int[] b){return UiHomeChecks.str(b);}
        static String text(String k){return WarTablePages.texts().get(k);}
        static int[] rect(String k){return WarTablePages.rects().get(k);}
        static String tex(String k){return WarTablePages.textures().get(k);}
        static String lastSent(){var s=WarTablePages.sent();return s.isEmpty()?"none":s.get(s.size()-1);}
        static void click(MinecraftClient c,int[] b){UiRecruitChecks.click(c,b);}
        /** A click with the other mouse button in the middle of a canvas box, as Mouse sends it. */
        static void rightClick(MinecraftClient c,int[] b){
            if(b==null||c.currentScreen==null)return;c.setNavigationType(net.minecraft.client.gui.navigation.GuiNavigationType.MOUSE);
            double x=UiHomeChecks.gui(c,b[0]+b[2]/2.0),y=UiHomeChecks.gui(c,b[1]+b[3]/2.0);var s=c.currentScreen;s.mouseMoved(x,y);s.mouseClicked(x,y,1);s.mouseReleased(x,y,1);
        }
        /** Characters as Keyboard.onChar hands them to the screen. */
        static void type(MinecraftClient c,String s){for(char ch:s.toCharArray())if(c.currentScreen!=null)c.currentScreen.charTyped(ch,0);}
        /** Inside the canvas and 12 px from each of its edges. */
        static boolean within(int[] a){return a!=null&&a[0]>=12&&a[1]>=12&&a[0]+a[2]<=UiCanvas.width()-12&&a[1]+a[3]<=UiCanvas.height()-12;}
        /** The name of the page's widget the screen's focus is on, or "none". */
        static String focused(MinecraftClient c){var s=screen(c);var p=pages(c);if(s==null||p==null)return "none";for(var e:p.buttons.entrySet())if(e.getValue()==s.getFocused())return e.getKey();return "none";}
        /** Adds the tiers the last frame drew on each of the track's rows. */
        void tiersDrawn(){for(var k:WarTablePages.rects().keySet()){if(k.startsWith("roll-"))rollSeen.add(Integer.parseInt(k.substring(5)));if(k.startsWith("oath-"))oathSeen.add(Integer.parseInt(k.substring(5)));}}

        /** The page as the last frame drew it: every visible widget and every piece of art (not the floor, the table or the rail)
         *  inside the page 12 px from its edges, the widgets clear of each other and all of them the War Table's own (no vanilla
         *  button); the back plaque where every page has it, the rail's camp plaques where home has them, the open page's drawn lit
         *  in its place (none lit on the field, which has no plaque); the page's own oak; its focal title in IVORY centred on 480 at
         *  84, or on the Chapter none (its chapter block names it). Returns the line for the report; clears ok when wrong. */
        String layout(MinecraftClient c,String name){
            var s=screen(c);int[] o=WarTable.origin();var b=boxes(c);
            List<String> out=new ArrayList<>(),over=new ArrayList<>(),art=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
            for(int i=0;i<keys.size();i++){int[] a=b.get(keys.get(i));if(!within(a))out.add(keys.get(i)+" "+str(a));
                for(int j=i+1;j<keys.size();j++)if(UiHomeChecks.overlap(a,b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));}
            for(var e:WarTablePages.rects().entrySet())if(!Set.of("table","rail").contains(e.getKey())&&!within(e.getValue()))art.add(e.getKey()+" "+str(e.getValue()));
            boolean ours=s!=null&&s.children().stream().allMatch(e->e instanceof UiButton);
            boolean back=Arrays.equals(b.get("back"),new int[]{o[0]+56,12,120,28});
            boolean rail=true;StringBuilder railWhy=new StringBuilder();
            // UI Task 8b (the Task 7 review's Minor 8): each camp plaque where the home suite pins home's (UiHomeChecks.LAYOUT, the
            // 960x540 page), moved by the origin's x; every one of home's camp plaques is on the rail (WarTableHome.RAIL, the one source)
            List<String> homeNames=Arrays.asList(UiHomeChecks.LAYOUT_NAMES);
            for(String r:homeNames.subList(1,6))if(!Arrays.asList(WarTableHome.RAIL).contains(r)){rail=false;railWhy.append(' ').append(r).append(" off the rail");}
            for(int i=0;i<WarTableHome.RAIL.length;i++){String r=WarTableHome.RAIL[i];int j=homeNames.indexOf(r);int[] home=j<0?null:UiHomeChecks.LAYOUT[j];
                int[] want=home==null?null:new int[]{o[0]+home[0],home[1],home[2],home[3]};
                boolean at=want!=null&&(r.equals(name)?!b.containsKey(r)&&Arrays.equals(rect("here"),want)&&"table/plaque-112-here".equals(tex("here")):Arrays.equals(b.get(r),want));
                rail&=at;if(!at)railWhy.append(' ').append(r);}
            if(!Arrays.asList(WarTableHome.RAIL).contains(name)&&rect("here")!=null){rail=false;railWhy.append(" a plaque lit");}
            String table=WarTable.drawnTable();boolean wood=table.equals("table/oak-table-"+name);
            String title=text("title");int[] tr=rect("title");
            boolean titled=name.equals("chapter")?title==null&&"Chapter 1 / The Founders".equals(text("chapter"))
                :WarTablePages.TITLES.get(name).equals(title)&&tr!=null&&Math.abs(tr[0]+tr[2]/2-(o[0]+480))<=1&&tr[1]==o[1]+84&&Integer.valueOf(DarkUi.IVORY).equals(WarTablePages.colors().get("title"));
            boolean pass=pages(c)!=null&&b.size()>=5&&out.isEmpty()&&over.isEmpty()&&art.isEmpty()&&ours&&back&&rail&&wood&&titled;
            ok&=pass;
            return "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (k "+UiCanvas.k()+"): "+b.size()+" widgets, within 12 px of the edge "+out+", overlapping "+over+", art within 12 px of the edge "+art
                +(ours?", every widget the War Table's own":", a vanilla widget among them")+", back "+str(b.get("back"))+", the rail "+(rail?"as home's, this page's plaque lit":"WRONG:"+railWhy)
                +", the oak "+table+", "+(name.equals("chapter")?"the chapter block \""+text("chapter")+"\"":"the title \""+title+"\" at "+str(tr))+(pass?"":" WRONG");
        }
        /** layout(c,name) for the scales: its line, clearing scalesOk (not ok) when wrong. */
        String scaled(MinecraftClient c,String name){boolean was=ok;ok=true;String line=layout(c,name);scalesOk&=ok;ok=was;return line;}
        /** 1280x720 GUI 3: the page centred on a canvas of 1280x720 (origin 160,90), the table's planks covering it, and layout's rules. */
        String mid(MinecraftClient c,String name){
            int[] o=WarTable.origin(),t=rect("table");int w=UiCanvas.width(),h=UiCanvas.height();
            boolean placed=pages(c)!=null&&w==1280&&h==720&&UiCanvas.k()==1&&o[0]==160&&o[1]==90&&t!=null&&t[0]<=0&&t[1]<=0&&t[0]+t[2]>=w&&t[1]+t[3]>=h;scalesOk&=placed;
            return "origin "+o[0]+","+o[1]+", table "+str(t)+(placed?"":" WRONG")+", "+scaled(c,name);
        }
        /** 854x480 GUI 2, the first-launch window: the compact page (UI Task 8b-2; Kort, 2026-10-22: everything must fit, every page,
         *  no noted crop). Every widget inside the canvas, none overlapping, and every box the page drew and every widget 12 px inside,
         *  none under the rail (UiHomeChecks.fits); the back plaque 12 px from the left edge; the page's own oak. */
        String small(MinecraftClient c,String name){
            int w=UiCanvas.width(),h=UiCanvas.height();var b=boxes(c);var r=WarTablePages.rects();
            List<String> out=new ArrayList<>(),over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
            for(int i=0;i<keys.size();i++){if(!UiHomeChecks.inside(b.get(keys.get(i)),w,h))out.add(keys.get(i)+" "+str(b.get(keys.get(i))));for(int j=i+1;j<keys.size();j++)if(UiHomeChecks.overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));}
            var bad=UiHomeChecks.fits(r,b);
            int[] title=rect("title"),back=b.get("back");String table=WarTable.drawnTable();
            boolean pass=pages(c)!=null&&WarTable.compact()&&w==854&&h==480&&UiCanvas.k()==1&&b.size()>=5&&out.isEmpty()&&over.isEmpty()&&bad.isEmpty()
                &&Arrays.equals(back,new int[]{12,12,120,28})&&table.equals("table/oak-table-"+name);scalesOk&=pass;
            return "canvas "+w+"x"+h+" (k "+UiCanvas.k()+", compact "+WarTable.compact()+"), "+b.size()+" widgets, outside the canvas "+out+", overlapping "+over+", not fitting "+bad+", back "+str(back)+", the title "+(title==null?"none (the chapter block)":str(title))+", the oak "+table+(pass?"":" WRONG");
        }
        /** 1920x974 GUI 1, a maximized window on a 1080p screen: the page stays centred on the big-canvas oak (UI Task 8b-2), which
         *  runs to every edge with no floor (read from the framebuffer as home's check reads it), the rail runs the width, and
         *  layout's rules hold. */
        String big(MinecraftClient c,String name){
            int cw=UiCanvas.width(),ch=UiCanvas.height(),k=UiCanvas.k();int[] o=WarTable.origin(),t=rect("table"),rail=rect("rail");
            int tx=(cw-WarTable.TABLE_W)/2,ty=(ch-WarTable.TABLE_H)/2;
            boolean placed=pages(c)!=null&&cw==1920&&ch==974&&k==1&&o[0]==480&&o[1]==217&&Arrays.equals(t,new int[]{tx,ty,WarTable.TABLE_W,WarTable.TABLE_H})&&rect("floor")==null&&rail!=null&&rail[2]==cw;
            var floor=UiHomeChecks.oakToEdges(c);
            scalesOk&=placed&&floor.isEmpty();
            return "origin "+o[0]+","+o[1]+", table "+str(t)+", rail "+str(rail)+(placed?"":" WRONG")+"; the edges "+(floor.isEmpty()?"oak all round, no floor":"not oak at "+floor+" WRONG")+"; "+scaled(c,name);
        }

        public void step(MinecraftClient c,int tick){
            if(tick>12000&&!finished){report("p1-ui-pages-timeout",false,"stuck in stage "+stage+" (page "+(index<PAGES.length?PAGES[index]:"none")+", step "+sub+"): "+why);end(c);return;}
            switch(stage){
                case 0->{ // a fresh commander with 300 gold at camp, in no party or room, the bots on Normal; the window at 960x540 GUI 1
                    if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
                    c.setScreen(null);CampaignClient.homeMode="skirmish";window(c,960,540,1);stage=-1;
                    server(c,b->{if(b.running)b.campaign.returnLobby();var id=player;b.campaign.social.leaveRoom(id);b.campaign.social.leaveParty(id);b.campaign.closeParty();b.campaign.difficulty="normal";
                        var p=new Profile();p.gold=300;b.campaign.profiles.put(id,p);b.campaign.save(id);stage=1;});
                }
                case 1->{ // the snapshot brings the fresh profile, no party and no room
                    var p=CampaignClient.profile;boolean fresh=p.gold==300&&p.owned.keySet().equals(new Profile().owned.keySet())&&p.progression.xp==0&&!WarTablePages.flag("party")&&WarTablePages.value("room").isEmpty()&&"normal".equals(CampaignClient.difficulty);
                    if(GravesAndGloryClient.running||!fresh){if(since(tick)>300){report("p1-ui-pages-setup",false,"no fresh profile with 300 gold, out of party and room, bots on Normal 300 ticks on: gold "+p.gold+", unlocked "+p.owned.keySet()+", party "+WarTablePages.flag("party")+", room \""+WarTablePages.value("room")+"\", bots "+CampaignClient.difficulty);end(c);}return;}
                    index=0;stage=2;
                }
                case 2->{ // the page from home's plaque (Change for the field), its layout and ui-page-<name>
                    int k=since(tick);String name=PAGES[index];
                    if(k==0){ok=true;why="";sub=0;CampScreen.open("home");return;}
                    if(k==10){click(c,UiHomeChecks.box(c,name.equals("field")?"change":name));return;}
                    if(k==20){UiRecruitChecks.park(c);return;}
                    if(k==22){var s=screen(c);boolean opened=s!=null&&s.page.equals(name)&&pages(c)!=null;ok&=opened;
                        why="home's "+(name.equals("field")?"Change":"plaque")+" opened "+(s==null?"nothing":s.page)+(opened?"":" WRONG");
                        if(!opened){stage=4;return;}
                        why+="; 960x540 GUI 1, "+layout(c,name);shot(c,"ui-page-"+name);sub=0;subAt=tick;stage=3;}
                }
                case 3->{ // the page's action
                    if(pages(c)==null){ok=false;why+="; the page closed during its action (step "+sub+") WRONG";stage=4;return;}
                    switch(PAGES[index]){case "banner"->banner(c,tick);case "contracts"->contracts(c,tick);case "chapter"->chapter(c,tick);case "friends"->friends(c,tick);default->field(c,tick);}
                }
                case 4->{ // the page at 1920x1080 GUI 2 (k 2) for grading, then 960x540 again and the back plaque home
                    int k=since(tick);String name=PAGES[index];
                    if(k==0){window(c,1920,1080,2,true);return;}
                    if(k==20){UiRecruitChecks.park(c);return;}
                    if(k==22){if(pages(c)!=null){why+="; 1920x1080 GUI 2, "+layout(c,name);shot(c,"ui-page-"+name+"-1080");}return;}
                    if(k==24){window(c,2560,1440,0,true);return;} // and at 2560x1440, the automatic GUI Scale: the War Table's 3x (Kort, 2026-10-31)
                    if(k==42){UiRecruitChecks.park(c);return;}
                    if(k==44){if(pages(c)!=null){ // the compact page at k 3 (853x480): every widget inside and fitting, the War Table's 3x
                        var bx=boxes(c);int w=UiCanvas.width(),h=UiCanvas.height();List<String> out=new ArrayList<>();for(var e:bx.entrySet())if(!UiHomeChecks.inside(e.getValue(),w,h))out.add(e.getKey());
                        var unfit=UiHomeChecks.fits(WarTablePages.rects(),bx);boolean pass=UiCanvas.k()==3&&WarTable.compact()&&out.isEmpty()&&unfit.isEmpty();ok&=pass;
                        why+="; 2560x1440 auto: canvas "+w+"x"+h+" (k "+UiCanvas.k()+", compact "+WarTable.compact()+"), outside "+out+", not fitting "+unfit+(pass?"":" WRONG");shot(c,"ui-page-"+name+"-1440");}return;}
                    if(k==46){window(c,960,540,1);return;}
                    if(k==62){UiRecruitChecks.park(c);click(c,box(c,"back"));return;}
                    if(k==66){var s=screen(c);boolean home=s!=null&&s.page.equals("home")&&s.home!=null;ok&=home;why+="; the back plaque went "+(home?"home":"WRONG: "+(s==null?"the table closed":s.page));
                        report("p1-ui-page-"+name,ok,why);index++;stage=index<PAGES.length?2:5;}
                }
                case 5->{ // p1-ui-page-scales: each page at 1280x720 GUI 3 (canvas 1280x720), 854x480 GUI 2 (the compact page, UI Task 8b-2)
                    // and 1920x974 GUI 1 (the big-canvas oak to every edge)
                    int k=since(tick),p=k/80,j=k%80;
                    if(p>=PAGES.length){if(j==0)window(c,960,540,1);if(j==20){report("p1-ui-page-scales",scalesOk,scalesWhy);stage=6;}return;}
                    String name=PAGES[p];
                    if(j==0){CampScreen.open(name);window(c,1280,720,3);return;}
                    if(j==20||j==44||j==68){UiRecruitChecks.park(c);return;}
                    if(j==22){scalesWhy+=(p==0?"":"; ")+name+" at 1280x720 GUI 3: "+mid(c,name);shot(c,"ui-page-"+name+"-720");return;}
                    if(j==24){window(c,854,480,2);return;}
                    if(j==46){scalesWhy+="; at 854x480 GUI 2: "+small(c,name);shot(c,"ui-page-"+name+"-854");return;}
                    if(j==48){window(c,1920,974,1,true);return;}
                    if(j==70){scalesWhy+="; at 1920x974 GUI 1: "+big(c,name);shot(c,"ui-page-"+name+"-1920x974");}
                }
                case 6->{ // p1-ui-page-cards: home's field card at 1920x1080 GUI 2 on each map in turn (ui-home-card-<id>), the server's map set as
                    // the snapshot carries it and no map built (the card follows the snapshot's map); then the camp's map put back
                    int k=since(tick);
                    if(k==0){origMap=CampaignClient.map;cardAsked=-1;sub=0;CampScreen.open("home");window(c,1920,1080,2,true);return;}
                    if(k<20)return;
                    if(sub<Spec.MAPS.length){int i=sub;
                        if(cardAsked!=i){cardAsked=i;subAt=tick;landedAt=-1;server(c,b->b.campaign.map=i);return;}
                        if(landedAt<0&&CampaignClient.map==i)landedAt=tick;
                        if(landedAt>0&&tick==landedAt+4)UiRecruitChecks.park(c);
                        if(landedAt>0&&tick==landedAt+6){String want="table/map-"+Spec.MAPS[i].id(),got=WarTableHome.texts().get("field-card");boolean pass=screen(c)!=null&&screen(c).home!=null&&want.equals(got)&&Spec.MAPS[i].name().equals(WarTableHome.texts().get("field-name"));
                            cardsOk&=pass;cardsWhy+=(i==0?"":", ")+Spec.MAPS[i].name()+": "+got+(pass?"":" WRONG (want "+want+")");shot(c,"ui-home-card-"+Spec.MAPS[i].id());landedAt=-1;sub++;}
                        else if(landedAt<0&&at(tick)>100){cardsOk=false;cardsWhy+=(i==0?"":", ")+Spec.MAPS[i].name()+" never reached the client WRONG";sub++;}
                        return;}
                    if(cardAsked!=-2){cardAsked=-2;subAt=tick;int m=Math.max(0,origMap);server(c,b->b.campaign.map=m);return;}
                    if(CampaignClient.map==Math.max(0,origMap)&&at(tick)>=3){window(c,960,540,1);report("p1-ui-page-cards",cardsOk,"home's field card at 1920x1080 GUI 2: "+cardsWhy+"; the camp's map put back ("+Spec.MAPS[Math.max(0,origMap)].name()+")");stage=10;}
                    else if(at(tick)>100){window(c,960,540,1);report("p1-ui-page-cards",false,cardsWhy+"; the camp's map not put back WRONG");stage=10;}
                }
                case 10->{ // p1-ui-page-focus: Tab to Remove on the Banner's row 0; a rebuild, then a change of the camp state, leave the focus there
                    int k=since(tick);var s=screen(c);
                    if(k==0){CampScreen.open("banner");return;}
                    if(k==5){UiRecruitChecks.park(c);for(int i=0;i<16&&!focused(c).equals("remove-0");i++)UiRecruitChecks.tap(c,GLFW.GLFW_KEY_TAB);focusName=focused(c);
                        why="Tab to "+focusName;if(s!=null)s.rebuild();String again=focused(c);ok=focusName.equals("remove-0")&&again.equals(focusName);why+=", a rebuild: "+again+(ok?"":" WRONG");return;}
                    if(k==7){boolean hot=WarTablePages.underlined().contains(focusName);ok&=hot;why+=", drawn underlined "+WarTablePages.underlined()+(hot?"":" WRONG");gold=CampaignClient.profile.gold;
                        server(c,b->{b.campaign.profile(player).gold+=1;b.campaign.save(player);});return;}
                    if(k>7&&gold>=0&&CampaignClient.profile.gold!=gold){gold=-1;landedAt=tick;return;}
                    if(landedAt>0&&tick==landedAt+3){String again=focused(c);var b=s==null||s.pages==null?null:s.pages.button(focusName);boolean kept=again.equals(focusName)&&b!=null&&b.isFocused();ok&=kept;
                        why+="; the gold changed on the server (the camp state: a rebuild): the focus on "+again+(kept?"":" WRONG");landedAt=-1;
                        report("p1-ui-page-focus",ok,why);stage=11;return;}
                    if(k>200){report("p1-ui-page-focus",false,why+"; no change of the camp state reached the client WRONG");landedAt=-1;stage=11;}
                }
                case 11->{ // p1-ui-page-held-open: the table opened with the menu key down (as the key that opened it is); its repeats leave it open, a press after the release closes it
                    int k=since(tick);
                    if(k==0){c.setScreen(null);return;}
                    if(k==2){var bound=net.minecraft.client.util.InputUtil.fromTranslationKey(GravesAndGloryClient.menu.getBoundKeyTranslationKey());int code=bound.getCode();menuCode=code;
                        if(bound.getCategory()!=net.minecraft.client.util.InputUtil.Type.KEYSYM||code<0){report("p1-ui-page-held-open",false,"the menu key is not a key on the keyboard: "+bound.getTranslationKey());stage=12;return;}
                        var saved=CampScreen.keyDown;try{CampScreen.keyDown=key->key==code;c.setScreen(new CampScreen("home"));}finally{CampScreen.keyDown=saved;}
                        table=screen(c);boolean seeded=table!=null&&table.held.contains(code);ok=seeded;why="the menu key ("+bound.getLocalizedText().getString()+") down as the table opened: held "+(table==null?"?":table.held)+(seeded?"":" WRONG");
                        UiRecruitChecks.press(c,code);return;}
                    if(k==4){boolean open=c.currentScreen==table;ok&=open;why+="; its repeat "+(open?"left the table open":"WRONG: closed it");UiRecruitChecks.press(c,menuCode);return;}
                    if(k==6){boolean open=c.currentScreen==table;ok&=open;why+=", a second repeat "+(open?"too":"WRONG: closed it");UiRecruitChecks.release(c,menuCode);UiRecruitChecks.press(c,menuCode);return;}
                    if(k==8){boolean shut=c.currentScreen!=table;ok&=shut;why+="; let go and pressed again: "+(shut?"the table closed":"WRONG: still open");UiRecruitChecks.release(c,menuCode);
                        report("p1-ui-page-held-open",ok,why);stage=12;}
                }
                case 12->{ // p1-ui-page-held-home: in a private room with Practice raised, a held Enter rides once (one refusal), a press after the release rides again
                    int k=since(tick);var s=screen(c);
                    if(k==0){c.setScreen(null);CampScreen.open("home");sub=0;return;}
                    if(k==5){click(c,UiHomeChecks.box(c,"practice"));if(screen(c)!=null)screen(c).action("social:room");why="Practice raised ("+CampaignClient.homeMode+"), a private room asked for";return;}
                    if(k<5)return;
                    if(sub==0){if(serverRoom&&!WarTablePages.value("room").isEmpty()&&k>=20){sub=1;subAt=tick;} // on to the first Enter, this tick
                        else{if(k>200){report("p1-ui-page-held-home",false,why+"; no room 200 ticks on WRONG");stage=13;}return;}}
                    int j=at(tick);
                    if(sub==1&&j==0){boolean practice=s!=null&&s.home!=null&&s.home.focused==0&&"practice".equals(CampaignClient.homeMode);ok=practice;why+="; in room "+WarTablePages.value("room")+", Practice "+(practice?"raised":"WRONG: not raised");
                        serial=CampaignClient.noticeSerial;UiRecruitChecks.press(c,GLFW.GLFW_KEY_ENTER);return;}
                    if(sub==1&&j==20){serial1=CampaignClient.noticeSerial;boolean once=serial1!=serial&&UiHomeChecks.PRACTICE_SOLO.equals(WarTableHome.texts().get("notice"));ok&=once;
                        why+="; Enter: the notice serial "+serial+" to "+serial1+" (\""+WarTableHome.texts().get("notice")+"\")"+(once?"":" WRONG");UiRecruitChecks.press(c,GLFW.GLFW_KEY_ENTER);return;}
                    if(sub==1&&j==40){serial2=CampaignClient.noticeSerial;boolean none=serial2==serial1;ok&=none;why+="; its repeat a second later: the serial "+serial2+(none?" (nothing sent)":" WRONG: it rode again");
                        UiRecruitChecks.release(c,GLFW.GLFW_KEY_ENTER);UiRecruitChecks.press(c,GLFW.GLFW_KEY_ENTER);return;}
                    if(sub==1&&j==60){int last=CampaignClient.noticeSerial;boolean again=last!=serial2;ok&=again;why+="; let go and pressed again: the serial "+last+(again?" (it rode)":" WRONG: nothing sent");
                        UiRecruitChecks.release(c,GLFW.GLFW_KEY_ENTER);click(c,UiHomeChecks.box(c,"skirmish"));GravesAndGloryClient.send("social:exit");sub=2;subAt=tick;return;}
                    if(sub==2){if(!serverRoom&&WarTablePages.value("room").isEmpty()){why+="; the room left";report("p1-ui-page-held-home",ok,why);stage=13;}
                        else if(j>200){report("p1-ui-page-held-home",false,why+"; the room was not left WRONG");stage=13;}}
                }
                case 13->end(c);
                case 99->{stage=100;finish(c,"ui-pages");}
                default->{}
            }
        }

        /** Banner: a click on row 1 arms its troop and a click on seat 0 sends slot:0:<it>, which the snapshot brings back and the seat
         *  draws; the other button on seat 19 sends slot:19:empty; then both seats are put back as they were. */
        void banner(MinecraftClient c,int tick){
            int k=at(tick),t=WarTablePages.team();int[] o=WarTable.origin();var line=CampaignClient.profile.banner;var p=pages(c);
            switch(sub){
                case 0->{roll=new ArrayList<>();for(var r:WarTablePages.owned())roll.add(r.troopId());at0=line.get(0);at19=line.get(19);
                    var add=button(c,"add-0");var remove=button(c,"remove-0");
                    boolean start=roll.size()>=2&&at0.equals(roll.get(0))&&!at19.equals(WarTablePages.EMPTY)&&add!=null&&!add.active&&remove!=null&&remove.active&&(WarTablePages.used()+"/20 seats").equals(text("seated"))&&"12 seats".equals(text("count-0"));
                    ok&=start;why+="; the roll "+roll+", seat 0 "+at0+", seat 19 "+at19+", \""+text("seated")+"\", row 0 \""+text("count-0")+"\" with Add "+(add!=null&&add.active?"taking a press":"dull")+" and Remove "+(remove!=null&&remove.active?"taking a press":"dull")+(start?"":" WRONG");
                    // with places left on the roll, the hint sits in its last place, at the foot (finding 3 of the Task 7 review)
                    int[] hr=rect("roll-hint");int last=o[1]+WarTablePages.ROW_TOP+WarTablePages.ROW_H*(WarTablePages.ROWS-1);
                    boolean foot=roll.size()>=WarTablePages.ROWS||WarTablePages.ROLL_HINT.equals(text("roll-hint"))&&in(hr,rect("roll"))&&hr[1]>=last&&hr[1]+hr[3]<=last+WarTablePages.ROW_H;
                    ok&=foot;why+="; the hint \""+text("roll-hint")+"\" at "+str(hr)+(foot?" in the roll's last place":" WRONG (the last place "+last+" to "+(last+WarTablePages.ROW_H)+", the roll "+str(rect("roll"))+")");
                    click(c,box(c,"row-1"));next(tick);}
                // a step that reads the page as a frame drew it (its words, marks and art) waits for the frame: on a busy machine the client
                // can run more than one tick before it draws, so the snapshot can land with the last frame still showing the old state.
                // Each waits for the drawn state to catch up, or judges (and fails) once the wait is over
                case 1->{int[] mark=rect("mark");if(k<2||!WarTablePages.troopName(roll.get(1)).equals(text("armed"))&&k<40)return;
                    boolean armed=roll.get(1).equals(p.armed)&&WarTablePages.troopName(roll.get(1)).equals(text("armed"))&&Arrays.equals(mark,new int[]{o[0]+52,o[1]+196,234,58});
                    ok&=armed;why+="; row 1 armed "+p.armed+" (the band reads \""+text("armed")+"\", the mark at "+str(mark)+")"+(armed?"":" WRONG");
                    landedAt=-1;click(c,box(c,"seat-0"));next(tick);}
                case 2->{boolean landed=roll.get(1).equals(line.get(0));if(landed&&landedAt<0)landedAt=tick;
                    if(landed&&k>=3&&(roll.get(1).equals(text("seat-0"))||tick>=landedAt+20)){boolean pass=("slot:0:"+roll.get(1)).equals(lastSent())&&roll.get(1).equals(text("seat-0"));ok&=pass;
                        why+="; seat 0 clicked: sent "+lastSent()+", the snapshot's seat 0 is "+line.get(0)+", drawn "+text("seat-0")+(pass?"":" WRONG");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; seat 0 clicked: sent "+lastSent()+", the snapshot's seat 0 still "+line.get(0)+" 100 ticks on WRONG";landedAt=-1;next(tick);}}
                case 3->{if(k<8)return;landedAt=-1;rightClick(c,box(c,"seat-19"));next(tick);}
                case 4->{boolean landed=WarTablePages.EMPTY.equals(line.get(19));if(landed&&landedAt<0)landedAt=tick;
                    if(landed&&k>=3&&(WarTablePages.EMPTY.equals(text("seat-19"))||tick>=landedAt+20)){boolean pass="slot:19:empty".equals(lastSent())&&WarTablePages.EMPTY.equals(text("seat-19"));ok&=pass;
                        why+="; the other button on seat 19: sent "+lastSent()+", the snapshot's seat 19 is "+line.get(19)+", drawn "+text("seat-19")+(pass?"":" WRONG");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; the other button on seat 19: sent "+lastSent()+", seat 19 still "+line.get(19)+" WRONG";landedAt=-1;next(tick);}}
                case 5->{if(k<8)return;click(c,box(c,"row-"+Math.max(0,roll.indexOf(at0))));click(c,box(c,"seat-0"));next(tick);}
                case 6->{if(at0.equals(line.get(0)))next(tick);else if(k>100){ok=false;why+="; seat 0 not put back ("+line.get(0)+") WRONG";next(tick);}}
                case 7->{if(k<8)return;click(c,box(c,"row-"+Math.max(0,roll.indexOf(at19))));click(c,box(c,"seat-19"));next(tick);}
                case 8->{if(at19.equals(line.get(19))){why+="; seats 0 and 19 put back ("+line.get(0)+", "+line.get(19)+")";stage=4;}
                    else if(k>100){ok=false;why+="; seat 19 not put back ("+line.get(19)+") WRONG";stage=4;}}
                default->stage=4;
            }
        }

        /** What claiming key would pay now, by the game's own rule: Progression.claim on a copy of the state (a JSON round trip) with every contract finished,
         *  every tier reached and stored's own claim taken off. The expected words and amounts of the claim checks come from here and from the sheets, never from a number written out. */
        static Progression.Grant grant(Progression.State src,String stored,String key){
            var s=Campaign.JSON.fromJson(Campaign.JSON.toJson(src),Progression.State.class);s.sanitize();
            s.dailyBattles=Math.max(s.dailyBattles,3);s.dailyCaptures=Math.max(s.dailyCaptures,1);s.dailyOrders=Math.max(s.dailyOrders,10);
            s.weeklyBattles=Math.max(s.weeklyBattles,Progression.WEEKLY_BATTLES);s.xp=Math.max(s.xp,Progression.XP_PER_TIER*Progression.TIERS);s.claims.remove(stored);
            return Progression.claim(s,key);
        }
        /** The hero letter's tier when the track starts at first (the page's own rule, UiMath.heroTier then the next title tier). */
        static int heroAt(int first){return WarTablePages.titleFrom(UiMath.heroTier(first,WarTablePages.shown(),Progression.TIERS,WarTablePages.EVERY));}
        /** A Roll plaque's two lines hold the words of the tier's reward (Progression.reward) and nothing else: "30 gold" reads "Gold / 30"; the article and the word House are left out. */
        static boolean plaqueIs(int tier,String plaque){
            if(plaque==null)return false;Set<String> want=new TreeSet<>(),got=new TreeSet<>();
            for(String w:Progression.reward(tier).toLowerCase(Locale.ROOT).split(" "))if(!Set.of("a","an","house").contains(w))want.add(w);
            for(String w:plaque.toLowerCase(Locale.ROOT).split("[ /]+"))if(!w.isEmpty())got.add(w);
            return want.equals(got);
        }
        /** No House chip and no Rare chip on the page. */
        static boolean noChips(MinecraftClient c){for(var h:Spec.HOUSES)if(box(c,"house-"+h.id())!=null)return false;return box(c,"rare")==null;}
        String houseName0(){return RosterMath.houseName(house);}

        /** Contracts: the three dailies and the weekly where the page puts them; Reroll changes the third contract (or meets the
         *  server's refusal on the notice line) and then takes no press. Then, at 1920x1080 GUI 2, the first daily and the weekly
         *  completed on the server: Claim shows on those two notes only, each inside its note and clear of its count
         *  (ui-page-contracts-claim); each press lands in the snapshot, pays what the server's notice says and stamps its note in
         *  place of Claim (ui-page-contracts-claimed). The contracts, the XP and the gold are put back as they were, so the Chapter
         *  still meets a fresh commander. */
        void contracts(MinecraftClient c,int tick){
            int k=at(tick);int[] o=WarTable.origin();var reroll=button(c,"reroll");var pr=CampaignClient.profile.progression;
            switch(sub){
                case 0->{boolean placed=Arrays.equals(rect("weekly"),new int[]{o[0]+328,o[1]+300,308,101})&&Arrays.equals(box(c,"reroll"),new int[]{o[0]+436,o[1]+252,88,24});
                    for(int i=0;i<3;i++)placed&=Arrays.equals(rect("note-"+i),new int[]{o[0]+140+240*i,o[1]+140,204,101});
                    boolean fresh=reroll!=null&&reroll.active&&"Swap the third contract, once a day".equals(text("reroll-hint"));
                    // the House row: five chips in the Houses' order under the reset line, 24 tall or more, centred on the page, the banner's House chosen (its word, the pressed art), the reward words from the game's own rule
                    house=RosterMath.rollHouse(CampaignClient.profile.banner);String houseName=RosterMath.houseName(house);
                    boolean chips=true;int prevRight=-1;StringBuilder row=new StringBuilder();
                    for(var h:Spec.HOUSES){int[] b=box(c,"house-"+h.id());
                        boolean here=b!=null&&b[2]>=24&&b[3]>=24&&b[1]==o[1]+436&&b[0]>prevRight&&h.name().equals(text("house-"+h.id()))&&(h.id().equals(house)?"table/chip-house-on":"table/chip-house").equals(tex("house-"+h.id()));
                        chips&=here;row.append(h.id()).append(' ').append(str(b)).append(here?"":" WRONG").append(", ");if(b!=null)prevRight=b[0]+b[2];}
                    int[] first=box(c,"house-"+Spec.HOUSES[0].id()),last=box(c,"house-"+Spec.HOUSES[Spec.HOUSES.length-1].id());
                    boolean centred=first!=null&&last!=null&&Math.abs((first[0]+last[0]+last[2])/2-(o[0]+480))<=2;
                    boolean chosen=house.equals(text("house-chosen"));
                    var g0=grant(pr,"daily:0","daily:0");var g1=grant(pr,"daily:1","daily:1");var g2=grant(pr,"daily:2","daily:2");var gw=grant(pr,"weekly","weekly:"+house);
                    boolean words=(g0.gold()+" gold and "+g0.xp()+" XP").equals(text("reward-0"))&&(g1.gold()+" gold and "+g1.xp()+" XP").equals(text("reward-1"))&&(g2.gold()+" gold and "+g2.xp()+" XP").equals(text("reward-2"))
                        &&(gw.gold()+" gold, "+gw.xp()+" XP and "+gw.shards()+" "+houseName+" shards").equals(text("reward-weekly"))&&text("shards-0")==null&&text("shards-1")==null&&text("shards-2")==null;
                    ok&=placed&&fresh&&chips&&centred&&chosen&&words;why+="; notes "+str(rect("note-0"))+", "+str(rect("note-1"))+", "+str(rect("note-2"))+", weekly "+str(rect("weekly"))+", Reroll "+str(box(c,"reroll"))+(placed?"":" WRONG")
                        +"; the third contract \""+text("contract-2")+"\", Reroll "+(fresh?"taking a press":"WRONG: dull (\""+text("reroll-hint")+"\")")
                        +"; the House row "+row+"centred on the page="+centred+(chips&&centred?"":" WRONG")+", the banner's House "+house+" chosen (the drawn word \""+text("house-chosen")+"\")"+(chosen?"":" WRONG")
                        +"; the rewards \""+text("reward-0")+"\", \""+text("reward-1")+"\", \""+text("reward-2")+"\" and the weekly's \""+text("reward-weekly")+"\" are Progression.claim's own words, no shards line yet"+(words?"":" WRONG");
                    serial=CampaignClient.noticeSerial;before=text("contract-2");landedAt=-1;click(c,box(c,"reroll"));next(tick);}
                case 1->{boolean changed=!Objects.equals(before,text("contract-2")),noticed=CampaignClient.noticeSerial!=serial,landed=noticed&&(changed||refused(CampaignClient.notice));
                    if(landed&&landedAt<0)landedAt=tick;String n=text("notice");
                    if(landed&&k>=3&&(REROLLED.equals(n)||refused(n)||tick>=landedAt+20)){ // judged once a frame has drawn the notice line
                        boolean pass="progress:reroll".equals(lastSent())&&(changed?"Issue ten tactical orders".equals(text("contract-2"))&&REROLLED.equals(n)&&reroll!=null&&!reroll.active&&"Rerolled today".equals(text("reroll-hint")):refused(n));
                        ok&=pass;why+="; Reroll sent "+lastSent()+": the third contract reads \""+text("contract-2")+"\", the notice line \""+n+"\", Reroll "+(reroll!=null&&reroll.active?"still taking a press":"dull")+" (\""+text("reroll-hint")+"\")"+(pass?"":" WRONG");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; Reroll sent "+lastSent()+": no change and no refusal 100 ticks on (the third contract \""+text("contract-2")+"\", the notice \""+CampaignClient.notice+"\") WRONG";landedAt=-1;next(tick);}}
                case 2->{window(c,1920,1080,2,true);goldBefore=CampaignClient.profile.gold;xpBefore=pr.xp;shardsBefore=CampaignClient.profile.shards.getOrDefault(house,0);landedAt=-1; // one battle today, six this week
                    gA=grant(pr,"daily:0","daily:0");gW=grant(pr,"weekly","weekly:"+house);
                    server(c,b->{var s=b.campaign.profile(player).progression;Progression.reset(s,Progression.today());s.dailyBattles=1;s.weeklyBattles=Progression.WEEKLY_BATTLES;b.campaign.save(player);});next(tick);}
                case 3->{if(k==18)UiRecruitChecks.park(c);boolean landed=pr.dailyBattles==1&&pr.weeklyBattles==Progression.WEEKLY_BATTLES;if(landed&&landedAt<0)landedAt=tick;
                    boolean drawn="Claim".equals(text("claim-0"))&&"Claim".equals(text("claim-weekly"));
                    if(landed&&k>=20&&(drawn||tick>=landedAt+20)){ // judged once a frame has drawn both Claims
                        int[] b0=box(c,"claim-0"),bw=box(c,"claim-weekly");var c0=button(c,"claim-0");var cw=button(c,"claim-weekly");
                        boolean shown=drawn&&c0!=null&&c0.active&&cw!=null&&cw.active&&box(c,"claim-1")==null&&box(c,"claim-2")==null&&Integer.valueOf(DarkUi.INK_TEXT).equals(WarTablePages.colors().get("claim-0"));
                        boolean placed=in(b0,rect("note-0"))&&in(bw,rect("weekly"))&&rect("progress-0")!=null&&rect("progress-weekly")!=null&&!UiHomeChecks.overlap(b0,rect("progress-0"))&&!UiHomeChecks.overlap(bw,rect("progress-weekly"));
                        boolean counts="1 / 1".equals(text("progress-0"))&&"1 / 3".equals(text("progress-1"))&&"6 / 6".equals(text("progress-weekly"));
                        boolean pass=shown&&placed&&counts;ok&=pass;
                        why+="; one battle today and six this week set on the server: Claim on note 0 at "+str(b0)+" and on the weekly at "+str(bw)+", none on notes 1 and 2"+(shown?"":" WRONG")
                            +"; inside their notes "+str(rect("note-0"))+" and "+str(rect("weekly"))+" and clear of their counts \""+text("progress-0")+"\" at "+str(rect("progress-0"))+" and \""+text("progress-weekly")+"\" at "+str(rect("progress-weekly"))+", note 1 \""+text("progress-1")+"\""+(placed&&counts?"":" WRONG")
                            +"; 1920x1080 GUI 2, "+layout(c,"contracts");
                        shot(c,"ui-page-contracts-claim");landedAt=-1;click(c,b0);next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; the finished contracts never reached the client (daily battles "+pr.dailyBattles+", weekly "+pr.weeklyBattles+") WRONG";sub=6;subAt=tick;}}
                // each claim lands in the snapshot; the page draws the stamp and the server's notice a frame later: judged once drawn, or
                // 20 ticks after the landing. Eight ticks at least between the presses, past CampScreen.action's 300 ms
                case 4->{boolean landed=pr.claims.contains("daily:0");if(landed&&landedAt<0)landedAt=tick;String want=gA.message();
                    boolean drawn="table/stamp-hollow".equals(tex("stamp-0"))&&"Claimed".equals(text("progress-0"))&&want.equals(text("notice"));
                    if(landed&&k>=8&&(drawn||tick>=landedAt+20)){
                        boolean pass="progress:daily:0".equals(lastSent())&&drawn&&box(c,"claim-0")==null&&in(rect("stamp-0"),rect("note-0"))&&want.equals(CampaignClient.notice)&&CampaignClient.profile.gold==goldBefore+gA.gold()&&pr.xp==xpBefore+gA.xp();
                        ok&=pass;why+="; Claim on note 0 sent "+lastSent()+": claims "+pr.claims+", the note reads \""+text("progress-0")+"\", stamped "+tex("stamp-0")+" at "+str(rect("stamp-0"))+", Claim "+(box(c,"claim-0")==null?"gone":"still shown")
                            +", the notice line \""+text("notice")+"\", gold "+goldBefore+" to "+CampaignClient.profile.gold+", XP "+xpBefore+" to "+pr.xp+(pass?"":" WRONG");
                        landedAt=-1;click(c,box(c,"claim-weekly"));next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; Claim on note 0 sent "+lastSent()+": the claim never landed (claims "+pr.claims+", the notice \""+CampaignClient.notice+"\") WRONG";sub=6;subAt=tick;}}
                case 5->{boolean landed=pr.claims.contains("weekly");if(landed&&landedAt<0)landedAt=tick;String want=gW.message();
                    boolean drawn="table/stamp-hollow".equals(tex("stamp-weekly"))&&"Claimed".equals(text("progress-weekly"))&&want.equals(text("notice"));
                    if(landed&&k>=3&&(drawn||tick>=landedAt+20)){
                        boolean pass=("progress:weekly:"+house).equals(lastSent())&&drawn&&box(c,"claim-weekly")==null&&in(rect("stamp-weekly"),rect("weekly"))&&want.equals(CampaignClient.notice)&&CampaignClient.profile.gold==goldBefore+gA.gold()+gW.gold()&&pr.xp==xpBefore+gA.xp()+gW.xp()
                            &&CampaignClient.profile.shards.getOrDefault(house,0)==shardsBefore+gW.shards()&&box(c,"house-"+house)!=null;
                        ok&=pass;why+="; Claim on the weekly sent "+lastSent()+": the note reads \""+text("progress-weekly")+"\", stamped "+tex("stamp-weekly")+" at "+str(rect("stamp-weekly"))+", Claim "+(box(c,"claim-weekly")==null?"gone":"still shown")
                            +", the notice line \""+text("notice")+"\", gold "+CampaignClient.profile.gold+", XP "+pr.xp+", "+houseName0()+" shards "+shardsBefore+" to "+CampaignClient.profile.shards.getOrDefault(house,0)+" (want +"+gW.shards()+")"+(pass?"":" WRONG");
                        shot(c,"ui-page-contracts-claimed");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; Claim on the weekly sent "+lastSent()+": the claim never landed (claims "+pr.claims+", the notice \""+CampaignClient.notice+"\") WRONG";next(tick);}}
                case 6->{int g0=goldBefore,x0=xpBefore,s0=shardsBefore;String h0=house; // put back: no claims, no battles, the XP, the gold and the shards as they were
                    server(c,b->{var pp=b.campaign.profile(player);var s=pp.progression;s.claims.remove("daily:0");s.claims.remove("weekly");s.dailyBattles=0;s.weeklyBattles=0;s.xp=x0;pp.gold=g0;if(s0<=0)pp.shards.remove(h0);else pp.shards.put(h0,s0);b.campaign.save(player);});next(tick);}
                case 7->{boolean back=!pr.claims.contains("daily:0")&&!pr.claims.contains("weekly")&&pr.dailyBattles==0&&pr.weeklyBattles==0&&pr.xp==xpBefore&&CampaignClient.profile.gold==goldBefore&&CampaignClient.profile.shards.getOrDefault(house,0)==shardsBefore;
                    if(back&&(text("notice")==null||k>=200)){why+="; put back: no claims, XP "+pr.xp+", gold "+CampaignClient.profile.gold;stage=4;} // the claims' notice faded, for the grading shot
                    else if(!back&&k>100){ok=false;why+="; not put back: claims "+pr.claims+", XP "+pr.xp+", gold "+CampaignClient.profile.gold+" WRONG";stage=4;}}
                default->stage=4;
            }
        }

        /** Chapter: on the fresh commander the Claim plate is dull and the track starts at tier 1; with tier 11 reached and tiers 1 to
         *  10 claimed the plate is bright, the track starts at tier 10 and the plaques stand as the approved track has them; the
         *  arrows show every tier of the chapter on both rows (from 1, 11 and 21); Enter claims tier 11: the plate dulls, the claim's
         *  stamp is the wet one and the track slides one plaque on, tier 11 (still the current tier) lifted at its left end as the
         *  approved track's claim state has it. Then a claimed title is worn from its plaque and taken off. */
        void chapter(MinecraftClient c,int tick){
            int k=at(tick);int[] o=WarTable.origin();var claim=button(c,"claim");var pr=CampaignClient.profile.progression;
            switch(sub){
                case 0->{boolean dull=claim!=null&&!claim.active&&"table/btn-dull-240x40".equals(tex("claim"))&&"1".equals(text("first"))&&"0".equals(text("tier"))&&String.valueOf(heroAt(1)).equals(text("hero"))&&noChips(c);
                    ok&=dull;why+="; fresh: tier "+text("tier")+", the track from "+text("first")+", the hero tier "+text("hero")+" ("+text("hero-name")+"), Claim "+tex("claim")+(claim!=null&&claim.active?" taking a press":"")+(dull?"":" WRONG");
                    rollSeen.clear();oathSeen.clear();slid=false;landedAt=-1;
                    server(c,b->{var s=b.campaign.profile(player).progression;s.xp=2870;for(int t=1;t<=10;t++)s.claims.add(Progression.SEASON+":"+t);s.titles.add("Scout");s.titles.add("Pathfinder");b.campaign.save(player);});
                    next(tick);}
                case 1->{if(pr.xp==2870&&pr.claims.contains(Progression.SEASON+":10")){window(c,1920,1080,2,true);next(tick);}else if(k>100){ok=false;why+="; tier 11 never reached the client WRONG";stage=4;}} // the rest at k 2, as the approved mockups
                case 2->{if(k==18)UiRecruitChecks.park(c);if(k<20)return;tiersDrawn();
                    int ht=heroAt(10);String heroName=Progression.TITLES[WarTablePages.titleAt(ht)];
                    boolean bright=claim!=null&&claim.active&&"table/btn-brass-240x40".equals(tex("claim"))&&"Claim".equals(text("claim"))&&"10".equals(text("first"))&&"11".equals(text("tier"))&&String.valueOf(ht).equals(text("hero"))&&heroName.equals(text("hero-name"))
                        &&("Unlocks at "+UiMath.thousands(ht*Progression.XP_PER_TIER)+" XP").equals(text("hero-state"))&&(UiMath.thousands(2870)+" / "+UiMath.thousands(Progression.XP_PER_TIER*Progression.TIERS)+" XP").equals(text("xp"))
                        &&(UiMath.thousands(12*Progression.XP_PER_TIER-2870)+" XP to tier 12").equals(text("to-next"))
                        &&"Tier 11 gives".equals(text("gives-head"))&&Progression.reward(11).equals(text("gives"))&&noChips(c); // a gold tier waits: what it gives in the reward's own words, and no House row
                    gT=grant(pr,Progression.SEASON+":11",Progression.SEASON+":11");
                    boolean plaques=Arrays.equals(rect("roll-10"),new int[]{o[0]+136,o[1]+320,68,80})&&Arrays.equals(rect("roll-11"),new int[]{o[0]+212,o[1]+312,68,88})&&Arrays.equals(rect("oath-12"),new int[]{o[0]+288,o[1]+408,68,80})
                        &&"chapter/plaque-tier-b".equals(tex("roll-10"))&&"chapter/plaque-tier-raised".equals(tex("roll-11"))&&"chapter/plaque-tier-dull-b".equals(tex("roll-12"))&&"table/stamp-hollow-b".equals(tex("stamp-10"))
                        &&"Pathfinder / title".equals(text("roll-10"))&&plaqueIs(11,text("roll-11"))&&plaqueIs(12,text("roll-12"))&&plaqueIs(13,text("roll-13"))&&"Tabard / livery".equals(text("oath-11"))&&"Portrait / frame".equals(text("oath-10"))&&button(c,"title-10")!=null;
                    ok&=bright&&plaques;why+="; tier 11 reached, 1 to 10 claimed: Claim "+tex("claim")+(claim!=null&&claim.active?" taking a press":" dull")+", the track from "+text("first")+", the hero tier "+text("hero")+" "+text("hero-name")+" (\""+text("hero-state")+"\"), \""+text("xp")+"\", \""+text("to-next")+"\""+(bright?"":" WRONG")
                        +"; roll-10 "+str(rect("roll-10"))+" "+tex("roll-10")+" \""+text("roll-10")+"\" stamped "+tex("stamp-10")+", roll-11 "+str(rect("roll-11"))+" "+tex("roll-11")+" \""+text("roll-11")+"\", roll-12 "+tex("roll-12")+", oath-11 \""+text("oath-11")+"\", oath-12 "+str(rect("oath-12"))+(plaques?"":" WRONG");
                    shot(c,"ui-page-chapter-claimable");click(c,box(c,"earlier"));next(tick);}
                // the arrows, the title plaques: each step reads the track as a frame drew it, so it waits for the frame (up to 40 ticks)
                case 3->{if(k<3||!"1".equals(text("first"))&&k<40)return;tiersDrawn();var earlier=button(c,"earlier");boolean at="1".equals(text("first"))&&earlier!=null&&!earlier.active&&"chapter/arrow-left-dim".equals(tex("earlier"));
                    ok&=at;why+="; the left arrow: from "+text("first")+" (the arrow "+tex("earlier")+")"+(at?"":" WRONG");click(c,box(c,"later"));next(tick);}
                case 4->{if(k<3||!"11".equals(text("first"))&&k<40)return;tiersDrawn();boolean at="11".equals(text("first"));ok&=at;why+=", the right arrow: from "+text("first")+(at?"":" WRONG");click(c,box(c,"later"));next(tick);}
                case 5->{if(k<3||!"21".equals(text("first"))&&k<40)return;tiersDrawn();var later=button(c,"later");boolean at="21".equals(text("first"))&&later!=null&&!later.active&&"chapter/arrow-right-dim".equals(tex("later"));
                    Set<Integer> all=new TreeSet<>();for(int t=1;t<=Progression.TIERS;t++)all.add(t);boolean every=rollSeen.equals(all)&&oathSeen.equals(all);
                    ok&=at&&every;why+=", again: from "+text("first")+" (the arrow "+tex("later")+")"+(at?"":" WRONG")+"; tiers drawn on the Roll "+rollSeen.size()+" and on the Oath "+oathSeen.size()+" of "+Progression.TIERS+(every?"":" WRONG: "+rollSeen+" / "+oathSeen);
                    shot(c,"ui-page-chapter-end");UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ENTER);next(tick);}
                case 6->{int[] r11=rect("roll-11");if("11".equals(text("first"))&&r11!=null&&r11[0]>o[0]+136)slid=true;
                    if(landedAt<0&&pr.claims.contains(Progression.SEASON+":11"))landedAt=tick;
                    if(landedAt>0&&tick>=landedAt+12){
                        boolean pass="progress:founders:11".equals(lastSent())&&slid&&"11".equals(text("first"))&&Arrays.equals(rect("roll-11"),new int[]{o[0]+136,o[1]+312,68,88})&&claim!=null&&!claim.active&&"table/btn-dull-240x40".equals(tex("claim"))
                            &&"table/stamp-hollow-fresh".equals(tex("stamp-11"))&&gT.message().equals(text("notice"))&&"Locked".equals(text("claim"))&&Progression.refusal(pr,Progression.SEASON+":12").equals(text("claim-why"));
                        ok&=pass;why+="; Enter sent "+lastSent()+": the track "+(slid?"slid one plaque":"did not slide")+" to start at "+text("first")+" (roll-11 at "+str(rect("roll-11"))+"), Claim "+tex("claim")+", tier 11 stamped "+tex("stamp-11")+", the notice \""+text("notice")+"\""+(pass?"":" WRONG");
                        shot(c,"ui-page-chapter-claimed");landedAt=-1;next(tick);}
                    else if(k>120){ok=false;why+="; Enter sent "+lastSent()+": the claim never landed WRONG";stage=4;}}
                case 7->{if(k<8)return;click(c,box(c,"earlier"));next(tick);}
                case 8->{if(k<3||!"1".equals(text("first"))&&k<40)return;boolean shown="1".equals(text("first"))&&button(c,"title-5")!=null&&button(c,"title-10")!=null;ok&=shown;why+="; back to tier 1: the claimed titles' plaques "+(shown?"take a press":"WRONG: missing");
                    landedAt=-1;click(c,box(c,"title-10"));next(tick);}
                case 9->{boolean landed="Pathfinder".equals(pr.title);if(landed&&landedAt<0)landedAt=tick;
                    if(landed&&k>=3&&("Pathfinder / worn".equals(text("roll-10"))||tick>=landedAt+20)){boolean worn="Pathfinder / worn".equals(text("roll-10"))&&"progress:title:Pathfinder".equals(lastSent());ok&=worn;why+="; Pathfinder's plaque sent "+lastSent()+", now \""+text("roll-10")+"\""+(worn?"":" WRONG");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; Pathfinder's plaque sent "+lastSent()+": the title is still "+pr.title+" WRONG";landedAt=-1;stage=4;}}
                case 10->{if(k<8)return;landedAt=-1;click(c,box(c,"title-10"));next(tick);}
                case 11->{boolean landed="Recruit".equals(pr.title);if(landed&&landedAt<0)landedAt=tick;
                    if(landed&&k>=3&&("Pathfinder / title".equals(text("roll-10"))||tick>=landedAt+20)){boolean off="Pathfinder / title".equals(text("roll-10"))&&"progress:title:Recruit".equals(lastSent());ok&=off;why+=", pressed again it sent "+lastSent()+" and reads \""+text("roll-10")+"\""+(off?"":" WRONG");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; pressed again: the title is still "+pr.title+" WRONG";landedAt=-1;stage=4;}}
                // tier 12 reached, a shard tier: the House row shows, the banner's House chosen, the reward line names it; another chip moves the pick and the words; then the XP is put back
                case 12->{if(k<6)return;landedAt=-1;int xp=12*Progression.XP_PER_TIER+10;server(c,b->{b.campaign.profile(player).progression.xp=xp;b.campaign.save(player);});next(tick);}
                case 13->{if(pr.xp==12*Progression.XP_PER_TIER+10){if(landedAt<0)landedAt=tick;
                        if(tick>=landedAt+8){String hn=RosterMath.houseName(house);boolean row=true;StringBuilder rw=new StringBuilder();
                            for(var h:Spec.HOUSES){int[] b=box(c,"house-"+h.id());boolean here=b!=null&&b[2]>=24&&b[3]>=24&&h.name().equals(text("house-"+h.id()));row&=here;rw.append(h.id()).append(' ').append(str(b)).append(here?"":" WRONG").append(", ");}
                            boolean gives="Tier 12 gives".equals(text("gives-head"))&&Progression.reward(12).replace("House shards",hn+" shards").equals(text("gives"))&&box(c,"rare")==null&&house.equals(text("house-chosen"));
                            ok&=row&&gives;why+="; tier 12 reached (a shard tier): the House row "+rw+"the banner's House "+house+" chosen (\""+text("house-chosen")+"\"), the reward line \""+text("gives-head")+" "+text("gives")+"\""+(row&&gives?"":" WRONG");
                            landedAt=-1;click(c,box(c,"house-tidecallers"));next(tick);}}
                    else if(k>100){ok=false;why+="; tier 12 never reached the client WRONG";stage=4;}}
                case 14->{if(k<4||!"tidecallers".equals(text("house-chosen"))&&k<40)return;
                    String hn=RosterMath.houseName("tidecallers");boolean moved="tidecallers".equals(text("house-chosen"))&&"table/chip-house-on".equals(tex("house-tidecallers"))&&"table/chip-house".equals(tex("house-"+house))&&Progression.reward(12).replace("House shards",hn+" shards").equals(text("gives"));
                    ok&=moved;why+="; the Tidecallers chip pressed: chosen \""+text("house-chosen")+"\", the line \""+text("gives")+"\""+(moved?"":" WRONG");shot(c,"ui-page-chapter-pick");
                    click(c,box(c,"house-"+house));next(tick);}
                case 15->{if(k<4||!house.equals(text("house-chosen"))&&k<40)return;boolean back=house.equals(text("house-chosen"));ok&=back;why+="; the banner's House pressed again: chosen \""+text("house-chosen")+"\""+(back?"":" WRONG");
                    landedAt=-1;server(c,b->{b.campaign.profile(player).progression.xp=2870;b.campaign.save(player);});next(tick);}
                case 16->{if(pr.xp==2870&&k>=3)stage=4;else if(k>100){ok=false;why+="; the XP was not put back WRONG";stage=4;}}
                default->stage=4;
            }
        }

        /** Friends: the room code's line takes letters and digits (upper case, eight at most; Join with party only for a whole code,
         *  and until then the line says how much is written, "5 of 8"), Backspace, and Esc ends the writing without leaving the
         *  page; the party's rule stands beside the seats, and with nobody else in the camp the camp column says how friends reach
         *  a camp; Create party makes a party and Leave party leaves it, as the snapshot shows; a private room lays its tags out
         *  clear of each other (ui-page-friends-room); Leave room leaves it, and the server's notice speaks of no party. */
        void friends(MinecraftClient c,int tick){
            int k=at(tick);var p=pages(c);var join=button(c,"room-join");
            switch(sub){
                case 0->{click(c,box(c,"code"));type(c,"ab-c1 2");next(tick);}
                case 1->{if(k<2||!"ABC12".equals(text("room-code"))&&k<40)return; // read as a frame drew it: waits for the frame
                    boolean partial=p.typing&&"ABC12".equals(p.code)&&join!=null&&!join.active&&"ABC12".equals(text("room-code"))&&"5 of 8".equals(text("code-count"));
                    ok&=partial;why+="; the code's line clicked and \"ab-c1 2\" typed: \""+p.code+"\" (writing "+p.typing+"), the line's count \""+text("code-count")+"\", Join with party "+(join!=null&&join.active?"taking a press":"dull")+(partial?"":" WRONG");type(c,"3456");next(tick);}
                case 2->{if(k<2||!"ABC12345".equals(text("room-code"))&&k<40)return;String whole=p.code;boolean full="ABC12345".equals(whole)&&join!=null&&join.active&&text("code-count")==null;UiRecruitChecks.tap(c,GLFW.GLFW_KEY_BACKSPACE);boolean back="ABC1234".equals(p.code);
                    UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ESCAPE);var s=screen(c);boolean stays=!p.typing&&s!=null&&s.page.equals("friends");boolean pass=full&&back&&stays;ok&=pass;
                    why+="; \"3456\" more: \""+whole+"\" with Join "+(join!=null&&join.active?"taking a press":"dull")+" and "+(text("code-count")==null?"no count":"the count \""+text("code-count")+"\"")+"; Backspace: \""+p.code+"\"; Esc: "+(stays?"the writing ends on the page":"WRONG: "+(s==null?"the table closed":s.page+", writing "+p.typing))+(pass?"":" WRONG");next(tick);}
                case 3->{if(k<2||!"ABC1234".equals(text("room-code"))&&k<40)return;var create=button(c,"create");boolean shown=create!=null&&create.visible&&create.active&&"Create party".equals(text("create"));
                    boolean open="Open seat".equals(text("open-1"))&&"Open seat".equals(text("open-3"))&&text("open-0")==null&&WarTablePages.me().equals(text("member-0"))&&"table/note-room-short".equals(tex("room"));
                    boolean count="7 of 8".equals(text("code-count"))&&join!=null&&!join.active;
                    boolean words=WarTablePages.PARTY_RULE.equals(text("party-rule"))&&text("party-line")==null&&text("room-why")==null&&in(rect("party-rule"),rect("party"))&&(c.getServer()!=null&&c.getServer().isRemote()?WarTablePages.CAMP_OPEN:WarTablePages.CAMP_HOW).equals(text("camp-how"));
                    ok&=shown&&open&&count&&words;
                    why+="; Create party "+(shown?"shown":"WRONG: missing")+", seat 0 "+text("member-0")+" and seats 1 to 3 \""+text("open-1")+"\""+", the room's note "+tex("room")+(open?"":" WRONG")
                        +"; the code's line after Esc \""+text("room-code")+"\" counted \""+text("code-count")+"\" with Join "+(join!=null&&join.active?"taking a press":"dull")+(count?"":" WRONG")
                        +"; beside the seats \""+text("party-rule")+"\" at "+str(rect("party-rule"))+", the camp column \""+text("camp-how")+"\""+(words?"":" WRONG");click(c,box(c,"create"));next(tick);}
                case 4->{ // the party lands in the snapshot; the page rebuilds on it (Leave party a widget) and a frame draws it
                    var leave=button(c,"leave");
                    if(WarTablePages.flag("party")&&leave!=null&&"Leave party".equals(text("leave"))){
                        boolean sent="social:create".equals(lastSent()),tags=leave.visible&&button(c,"disband")!=null&&button(c,"create")==null,seat=WarTablePages.me().equals(text("member-0"));
                        // UI Task 8b: the leader is said once, on seat 0 ("You lead the party"): the line under the seats does not name them again;
                        // the leader makes and joins rooms, so the room's note carries no word on why its tags are dull
                        String drawn=String.valueOf(text("party-line"));boolean line=WarTablePages.PARTY_LASTS.equals(drawn)&&!drawn.contains(WarTablePages.me())&&text("room-why")==null;
                        // UI Task 8b-2: the leader said once across every word the page drew, and in the in-party shot
                        // the leader said once (fix round 1, Minor 1): every drawn text naming a leader ("lead", "Led by") and every one
                        // carrying the player's name, the lead- tags apart: only seat 0's status and seat 0's name
                        List<String> leads=new ArrayList<>(),names=new ArrayList<>();
                        for(var e:WarTablePages.texts().entrySet()){String v=e.getValue();if(v==null||e.getKey().startsWith("lead-"))continue;String low=v.toLowerCase(java.util.Locale.ROOT);
                            if(low.contains("lead")||low.contains("led by"))leads.add(e.getKey()+" \""+v+"\"");if(v.contains(WarTablePages.me()))names.add(e.getKey()+" \""+v+"\"");}
                        boolean once="You lead the party".equals(text("status-0"))&&leads.size()==1&&names.size()==1&&names.get(0).startsWith("member-0 ");shot(c,"ui-page-friends-party");
                        boolean pass=sent&&tags&&seat&&line&&once;ok&=pass;why+="; the leader said "+leads.size()+" time(s): "+leads+", the name "+names.size()+" time(s): "+names+(once?"":" WRONG");
                        why+="; Create party sent "+lastSent()+(sent?"":" WRONG")+": party "+WarTablePages.flag("party")+", Leave party and Disband party "+(tags?"shown, Create party gone":"WRONG: disband "+button(c,"disband")+", create "+button(c,"create"))
                            +", seat 0 "+text("member-0")+(seat?"":" WRONG")+", \""+text("party-line")+"\""+(line?"":" WRONG");next(tick);}
                    else if(k>100){ok=false;why+="; Create party sent "+lastSent()+": no party drawn 100 ticks on (party "+WarTablePages.flag("party")+", Leave party "+(leave==null?"missing":"\""+text("leave")+"\"")+") WRONG";stage=4;}}
                case 5->{if(k<8)return;click(c,box(c,"leave"));next(tick);}
                case 6->{if(!WarTablePages.flag("party")&&k>=3){boolean pass="social:leave".equals(lastSent())&&button(c,"create")!=null;ok&=pass;why+="; Leave party sent "+lastSent()+": party "+WarTablePages.flag("party")+", Create party "+(button(c,"create")!=null?"back":"missing")+(pass?"":" WRONG");next(tick);}
                    else if(k>100){ok=false;why+="; Leave party sent "+lastSent()+": still in a party WRONG";stage=4;}}
                case 7->{if(k<8)return;click(c,box(c,"room-create"));next(tick);}
                case 8->{if(!WarTablePages.value("room").isEmpty()&&k>=3)next(tick);else if(k>100){ok=false;why+="; Create private room sent "+lastSent()+": no room WRONG";stage=4;}}
                case 9->{if(k<3)return;UiRecruitChecks.park(c);next(tick);}
                case 10->{if(k<2||!(WarTablePages.value("room").equals(text("code"))&&"0 of 1 ready".equals(text("room-ready")))&&k<40)return; // read as a frame drew it
                    boolean code=WarTablePages.value("room").equals(text("code"))&&"table/note-room".equals(tex("room"))&&"0 of 1 ready".equals(text("room-ready"))&&text("room-map")!=null;ok&=code;
                    why+="; Create private room sent "+lastSent()+": in room "+text("code")+" on "+tex("room")+", \""+text("room-ready")+"\", the map "+text("room-map")+(code?"":" WRONG")+", "+layout(c,"friends");shot(c,"ui-page-friends-room");next(tick);}
                case 11->{if(k<8)return;landedAt=-1;click(c,box(c,"room-leave"));next(tick);}
                case 12->{boolean landed=WarTablePages.value("room").isEmpty()&&!serverRoom;if(landed&&landedAt<0)landedAt=tick;String want="You left the room."; // alone: no word of a party
                    if(landed&&(want.equals(text("notice"))||tick>=landedAt+20)){boolean said=want.equals(text("notice"))&&"social:exit".equals(lastSent());ok&=said;
                        why+="; Leave room sent "+lastSent()+": out of the room, the notice line \""+text("notice")+"\""+(said?"":" WRONG");landedAt=-1;stage=4;}
                    else if(!landed&&k>100){ok=false;why+="; Leave room sent "+lastSent()+": still in the room WRONG";landedAt=-1;stage=4;}}
                default->stage=4;
            }
        }

        /** The field: off the war gates every row is sepia and every Vote dull under the line that says where to stand; each row shows
         *  its map's painted card; Hard sets the server's bots to hard, Normal puts them back. Then, at 1920x1080 GUI 2, the
         *  commander on the war gates (camp:gates, as home's Ride sends it): every row in ink and every Vote live under the gates'
         *  count (ui-page-field-gates); Vote on the camp's map brings the server's "Vote saved: <map>" on the notice line and the
         *  label Voted on that row alone (ui-page-field-voted); stepped off, the rows go back to sepia and dull. A lone commander on
         *  the gates starts the countdown to a match: he steps off long before it runs out. */
        void field(MinecraftClient c,int tick){
            int k=at(tick);int[] o=WarTable.origin();
            if((sub==8||sub==9)&&serverCountdown>0&&serverCountdown<=60){ok=false;why+="; the war gates' countdown down to "+serverCountdown+" ticks (step "+sub+"): stepped off before a match could start WRONG";
                stepOff(c);landedAt=-1;sub=10;subAt=tick;return;}
            switch(sub){
                case 0->{boolean off=CampaignClient.padMine<0&&"Stand on the war gates to vote".equals(text("status"))&&Integer.valueOf(DarkUi.SEPIA).equals(WarTablePages.colors().get("status"));
                    for(int i=0;i<Spec.MAPS.length;i++){var v=button(c,"vote-"+i);off&=v!=null&&!v.active&&Spec.MAPS[i].name().equals(text("map-"+i))&&Integer.valueOf(DarkUi.SEPIA).equals(WarTablePages.colors().get("map-"+i));}
                    ok&=off;why+="; off the war gates: \""+text("status")+"\""+(off?", every row sepia and every Vote dull":" WRONG");fieldRow=0;click(c,box(c,"map-0"));next(tick);}
                case 1->{int i=fieldRow;String want="table/map-"+Spec.MAPS[i].id();if(k<2||!want.equals(tex("card"))&&k<40)return; // the card as a frame drew it: waits for the frame
                    boolean card=want.equals(tex("card"))&&Spec.MAPS[i].name().equals(text("card-name"))&&Arrays.equals(rect("card"),new int[]{o[0]+WarTablePages.CARD_X,o[1]+WarTablePages.CARD_Y,139,83});
                    ok&=card;why+=(i==0?"; each row's card: ":", ")+tex("card")+(card?"":" WRONG (want "+want+" at "+str(rect("card"))+")");
                    if(i+1<Spec.MAPS.length){fieldRow=i+1;click(c,box(c,"map-"+(i+1)));subAt=tick;}else next(tick);}
                case 2->{boolean normal="Normal".equals(text("bots-current"))&&"normal".equals(serverDifficulty);ok&=normal;why+="; bots "+text("bots-current")+" (the server's "+serverDifficulty+")"+(normal?"":" WRONG");
                    landedAt=-1;click(c,box(c,"bots-hard"));next(tick);}
                // the tier lands in the client's tick and the page records its words only when a frame draws it: judged once the drawn
                // words have caught up, or 20 ticks after the landing (a page that never redraws the tier fails there)
                case 3->{boolean landed="hard".equals(serverDifficulty)&&"hard".equals(CampaignClient.difficulty);if(landed&&landedAt<0)landedAt=tick;
                    if(landed&&k>=3&&("Hard".equals(text("bots-current"))||tick>=landedAt+20)){boolean pass="difficulty:hard".equals(lastSent())&&"Hard".equals(text("bots-current"));ok&=pass;
                        why+="; Hard sent "+lastSent()+": the server's bots "+serverDifficulty+", the page reads "+text("bots-current")+(pass?"":" WRONG");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; Hard sent "+lastSent()+": the server's bots still "+serverDifficulty+" WRONG";landedAt=-1;next(tick);}}
                case 4->{if(k<8)return;landedAt=-1;click(c,box(c,"bots-normal"));next(tick);}
                case 5->{boolean landed="normal".equals(serverDifficulty)&&"normal".equals(CampaignClient.difficulty);if(landed&&landedAt<0)landedAt=tick;
                    if(landed&&k>=3&&("Normal".equals(text("bots-current"))||tick>=landedAt+20)){boolean pass="difficulty:normal".equals(lastSent())&&"Normal".equals(text("bots-current"));ok&=pass;
                        why+="; Normal sent "+lastSent()+": back to "+serverDifficulty+", the page reads "+text("bots-current")+(pass?"":" WRONG");landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; Normal sent "+lastSent()+": the server's bots still "+serverDifficulty+" WRONG";landedAt=-1;next(tick);}}
                case 6->{window(c,1920,1080,2,true);next(tick);}
                case 7->{if(k==18)UiRecruitChecks.park(c);if(k<20)return;pick=Math.max(0,Math.min(Spec.MAPS.length-1,CampaignClient.map));GravesAndGloryClient.send("camp:gates");landedAt=-1;next(tick);}
                // on the gates the server counts the commander (the snapshot's pads) and the page redraws a frame later: judged once a
                // frame has drawn the gates' line and every row and Vote in ink (the snapshot can land in a frame drawn before the tick
                // that makes the Votes live), or 20 ticks after the landing
                case 8->{boolean landed=CampaignClient.padMine>=0&&serverOnPad;if(landed&&landedAt<0)landedAt=tick;String status=text("status");
                    boolean inked=true;for(int i=0;i<Spec.MAPS.length;i++)inked&=Integer.valueOf(DarkUi.INK_TEXT).equals(WarTablePages.colors().get("vote-"+i))&&Integer.valueOf(DarkUi.INK_TEXT).equals(WarTablePages.colors().get("map-"+i));
                    boolean drawn=status!=null&&status.startsWith("War gates: ")&&inked;
                    if(landed&&k>=3&&(drawn||tick>=landedAt+20)){
                        boolean live=drawn&&Integer.valueOf(DarkUi.INK_TEXT).equals(WarTablePages.colors().get("status"))&&status.contains((CampaignClient.padMine==0?"Dawn":"Dusk")+" 1 / ");
                        List<String> votes=new ArrayList<>();
                        for(int i=0;i<Spec.MAPS.length;i++){var v=button(c,"vote-"+i);boolean on=v!=null&&v.active&&"Vote".equals(text("vote-"+i));live&=on;votes.add(text("vote-"+i)+(on?" live":" dull"));}
                        ok&=live;why+="; camp:gates sent: on the war gates (the snapshot's pad "+CampaignClient.padMine+", the server's "+serverOnPad+"), \""+status+"\""+(live?", every row in ink and every Vote live":" WRONG (rows and Votes drawn in ink "+inked+", the line's ink "+UiHomeChecks.hex(WarTablePages.colors().getOrDefault("status",0))+", "+votes+")")+"; 1920x1080 GUI 2, "+layout(c,"field");
                        shot(c,"ui-page-field-gates");serial=CampaignClient.noticeSerial;landedAt=-1;click(c,box(c,"map-"+pick));click(c,box(c,"vote-"+pick));next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; camp:gates sent: the commander never stood on the war gates (the snapshot's pad "+CampaignClient.padMine+", the server's "+serverOnPad+") WRONG";stepOff(c);landedAt=-1;sub=10;subAt=tick;}}
                case 9->{if(k==2)UiRecruitChecks.park(c);String name=Spec.MAPS[pick].name(),want=WarTablePages.VOTE_SAVED+name;
                    boolean landed=CampaignClient.noticeSerial!=serial&&want.equals(CampaignClient.notice);if(landed&&landedAt<0)landedAt=tick;
                    boolean drawn="Voted".equals(text("vote-"+pick))&&want.equals(text("notice"));
                    if(landed&&k>=4&&(drawn||tick>=landedAt+20)){boolean others=true;for(int i=0;i<Spec.MAPS.length;i++)if(i!=pick)others&="Vote".equals(text("vote-"+i));
                        boolean pass=("vote:"+pick).equals(lastSent())&&drawn&&others&&("table/map-"+Spec.MAPS[pick].id()).equals(tex("card"));ok&=pass;
                        why+="; Vote on "+name+" sent "+lastSent()+": the notice line \""+text("notice")+"\", its row's label \""+text("vote-"+pick)+"\""+(others?", the others Vote":", WRONG: another row not Vote")+", the card "+tex("card")+(pass?"":" WRONG");
                        shot(c,"ui-page-field-voted");stepOff(c);landedAt=-1;next(tick);}
                    else if(!landed&&k>100){ok=false;why+="; Vote on "+name+" sent "+lastSent()+": no \""+want+"\" 100 ticks on (the notice \""+CampaignClient.notice+"\") WRONG";stepOff(c);landedAt=-1;next(tick);}}
                case 10->{boolean off=CampaignClient.padMine<0&&!serverOnPad;if(off&&landedAt<0)landedAt=tick;
                    boolean sepia=true;for(int i=0;i<Spec.MAPS.length;i++)sepia&=Integer.valueOf(DarkUi.SEPIA).equals(WarTablePages.colors().get("vote-"+i))&&Integer.valueOf(DarkUi.SEPIA).equals(WarTablePages.colors().get("map-"+i));
                    boolean drawn="Stand on the war gates to vote".equals(text("status"))&&sepia;
                    if(off&&k>=3&&(drawn||tick>=landedAt+20)){boolean dull=drawn&&Integer.valueOf(DarkUi.SEPIA).equals(WarTablePages.colors().get("status"));
                        for(int i=0;i<Spec.MAPS.length;i++){var v=button(c,"vote-"+i);dull&=v!=null&&!v.active&&"Vote".equals(text("vote-"+i));}
                        ok&=dull;why+="; stepped off (the server's pad "+serverOnPad+"): \""+text("status")+"\""+(dull?", every Vote dull and none Voted":" WRONG");landedAt=-1;next(tick);}
                    else if(!off&&k>100){ok=false;why+="; stepped off: still on the war gates (the snapshot's pad "+CampaignClient.padMine+", the server's "+serverOnPad+") WRONG";landedAt=-1;stage=4;}}
                case 11->{if(text("notice")==null||k>=200)stage=4;} // the vote's notice faded, for the grading shot
                default->stage=4;
            }
        }

        /** Closes the table, puts the session's sheet back on Skirmish and the window as the suite found it; p1-ui-pages-suite follows. */
        void end(MinecraftClient c){finished=true;c.setScreen(null);CampaignClient.homeMode="skirmish";if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;}
    }

    /** UI follow-up to plan Task 9 (suite ui-icons): the six Phase 2 classes' icons on the War Table's Banner page and the six class actives'
     *  own pictures in the HUD's ability slot. p1-ui-icons-atlas: every class of the sheet has table/icon-class-(id) (14 x 14) and its
     *  active has hud/ability-(active) (22 x 20) in the atlas. p1-ui-icons-classes: the Banner page at 960x540 GUI 1 and at 1920x1080 GUI 2
     *  draws a plaque for every class, the commander's own lit (screenshots ui-icons-classes, ui-icons-classes-gui2). p1-ui-icons-hud: in a
     *  practice battle with testAbility set to each active, the slot draws that active's own picture (BattleHud.texts "ability-icon"),
     *  cooling and ready at 1920x1080 GUI 2 and cooling at 960x540 GUI 1 (screenshots ui-icons-hud-(active)-cool-gui2, -ready-gui2,
     *  -cool-gui1). p1-ui-icons-real: with testAbility cleared and the squad's class set to each class, the packet's class active fills the
     *  slot with that class's picture (screenshots ui-icons-hud-(class)-real-gui2); an active this build has no behaviour for (ready below 0)
     *  leaves the slot bare. */
    static final class UiIconsChecks implements Suite,ServerWatch {
        static final String[] ACTIVES={"stand_fast","bloodrush","mark_prey","rally_horn","grave_bloom","forward_supply"};
        /** The active whose slot the HUD hides (War Table port Task 6, ruling 5): the Quartermaster's Forward Supply; p1-ui-ability-hidden checks that it draws nothing. */
        static final String HIDDEN="forward_supply";
        // the server thread writes these, the client thread reads them
        volatile int stage;volatile UUID player;volatile String serverClass;
        // client thread only
        int mark=-1,waitFrom,oldW,oldH,oldGui,index,sub,subAt;boolean finished,hudOk=true,realOk=true;String hudWhy="",realWhy="";
        int since(int tick){if(mark!=stage){mark=stage;waitFrom=tick;}return tick-waitFrom;}
        void go(int s,int tick){sub=s;subAt=tick;}
        int at(int tick){return tick-subAt;}
        public void serverTick(GravesAndGlory.Battle b){
            var own=player==null?null:b.squads.get(player);
            if(stage==40&&player!=null){var p=b.server.getPlayerManager().getPlayer(player);if(p!=null&&skirmish(b,p,0,1,"normal")){UiHudChecks.quiet(b,b.squads.get(player));stage=41;}}
            String want=serverClass;if(own!=null&&want!=null&&!want.equals(own.commander)){own.commander=want;Commander.reset(b,own);}
        }
        static String box(int[] a){return a==null?"none":a[0]+","+a[1]+" "+a[2]+"x"+a[3];}
        /** The slot as the last frame drew it: the picture's texture, the key, the seconds, the rect. */
        static String slot(){var t=BattleHud.texts();return "ability \""+t.get("ability")+"\", picture \""+t.get("ability-icon")+"\", key \""+t.get("ability-key")+"\", seconds \""+t.get("ability-seconds")+"\", rect "+box(BattleHud.rects().get("ability"));}
        void fail(MinecraftClient c,String id,String why){report(id,false,why);end(c);}
        void end(MinecraftClient c){
            finished=true;c.setScreen(null);serverClass=null;
            server(c,b->{b.testAbility=null;var own=player==null?null:b.squads.get(player);if(own!=null){own.commander="champion";Commander.reset(b,own);}if(b.running)b.campaign.returnLobby();});
            if(oldW>0)window(c,oldW,oldH,oldGui);stage=99;
        }

        public void step(MinecraftClient c,int tick){
            if(tick>14000&&!finished){report("p1-ui-icons-timeout",false,"stuck in stage "+stage+" (active "+(index<ACTIVES.length?ACTIVES[index]:"none")+", step "+sub+")");end(c);return;}
            switch(stage){
                case 0->{ // the atlas, then a fresh commander (the Marshal) with 300 gold at camp, in no party or room, on a 960x540 GUI 1 window
                    if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
                    List<String> bad=new ArrayList<>();
                    for(var cl:Spec.CLASSES){
                        String iconName="table/icon-class-"+cl.id(),pic=BattleHud.abilityIcon(cl.active()); // named in a variable: the generator's literal scan (build_ui_art --verify) reads only whole names
                        var ic=UiTex.has(iconName)?UiTex.entry(iconName):null;
                        if(ic==null||ic.w()!=14||ic.h()!=14)bad.add(iconName+(ic==null?" missing":" "+ic.w()+"x"+ic.h()));
                        var ab=UiTex.has(pic)?UiTex.entry(pic):null;
                        if(ab==null||ab.w()!=22||ab.h()!=20)bad.add(pic+(ab==null?" missing":" "+ab.w()+"x"+ab.h()));
                    }
                    boolean same=Spec.CLASSES.length==ACTIVES.length;for(int i=0;same&&i<ACTIVES.length;i++)same=Arrays.asList(ACTIVES).contains(Spec.CLASSES[i].active());
                    report("p1-ui-icons-atlas",bad.isEmpty()&&same,Spec.CLASSES.length+" classes, each with its 14x14 icon and its active's 22x20 picture in the atlas"+(bad.isEmpty()?"":"; wrong: "+bad)+(same?"":"; the sheet's actives are not the six this suite knows"));
                    c.setScreen(null);CampaignClient.homeMode="skirmish";window(c,960,540,1);stage=-1;
                    server(c,b->{if(b.running)b.campaign.returnLobby();var id=player;b.campaign.social.leaveRoom(id);b.campaign.social.leaveParty(id);b.campaign.closeParty();b.campaign.difficulty="normal";
                        var p=new Profile();p.gold=300;p.commander="marshal";b.campaign.profiles.put(id,p);b.campaign.save(id);stage=1;});
                }
                case 1->{ // the snapshot brings the fresh profile
                    var p=CampaignClient.profile;boolean fresh=p.gold==300&&"marshal".equals(p.commander)&&!GravesAndGloryClient.running;
                    if(!fresh){if(since(tick)>300)fail(c,"p1-ui-icons-setup","no fresh profile with 300 gold and the Marshal 300 ticks on: gold "+p.gold+", class "+p.commander);return;}
                    stage=2;
                }
                case 2->{ // the Banner page from home's plaque at 960x540 GUI 1
                    int k=since(tick);
                    if(k==0){CampScreen.open("home");return;}
                    if(k==10){UiHomeChecks.click(c,UiHomeChecks.box(c,"banner"));return;}
                    if(k==20){UiRecruitChecks.park(c);return;}
                    if(k==22){var s=UiPagesChecks.screen(c);
                        if(s==null||!s.page.equals("banner")||UiPagesChecks.pages(c)==null){fail(c,"p1-ui-icons-classes","home's Banner plaque opened "+(s==null?"nothing":s.page));return;}
                        shot(c,"ui-icons-classes");window(c,1920,1080,2,true);stage=3;}
                }
                case 3->{ // the same page at 1920x1080 GUI 2 (k 2)
                    int k=since(tick);
                    if(k==6)UiRecruitChecks.park(c);
                    if(k==12){var s=UiPagesChecks.screen(c);
                        List<String> boxes=new ArrayList<>(),missing=new ArrayList<>();
                        for(var cl:Spec.CLASSES){int[] r=WarTablePages.rects().get("class-"+cl.id());boxes.add(cl.id()+" "+box(r));if(r==null)missing.add(cl.id());}
                        shot(c,"ui-icons-classes-gui2");
                        boolean ok=s!=null&&UiPagesChecks.pages(c)!=null&&missing.isEmpty()&&"table/plaque-120-here".equals(WarTablePages.textures().get("class-marshal"))&&"table/plaque-120".equals(WarTablePages.textures().get("class-champion"));
                        report("p1-ui-icons-classes",ok,"the Banner page at 1920x1080 GUI 2 (canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", k "+UiCanvas.k()+") drew a plaque for each class, the Marshal's lit: "+boxes+(missing.isEmpty()?"":"; no plaque for "+missing)+(ok?"":" WRONG"));
                        c.setScreen(null);window(c,960,540,1);stage=40; // the practice battle (the server starts it)
                    }
                }
                case 41->{ // the Iron Ledger is up
                    int k=since(tick);
                    if(GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()&&BattleHud.rects().containsKey("score")&&k>=20){
                        BattleHud.roundShownMs=net.minecraft.util.Util.getMeasuringTimeMs()-61000; // past the hint layer of the round's first seconds
                        window(c,1920,1080,2,true);index=0;go(0,tick);stage=5;return;}
                    if(k>1200)fail(c,"p1-ui-icons-hud","the battle HUD never came up (running "+GravesAndGloryClient.running+", ready "+GravesAndGloryClient.ready+", waiting "+CampaignClient.waiting+")");
                }
                case 5->hudSweep(c,tick,true); // each active cooling and ready at 1920x1080 GUI 2
                case 6->hudSweep(c,tick,false); // each active cooling at 960x540 GUI 1
                case 7->real(c,tick); // the class path at 1920x1080 GUI 2
                case 8->{report("p1-ui-icons-hud",hudOk,hudWhy);report("p1-ui-icons-real",realOk,realWhy);end(c);}
                case 99->{stage=100;finish(c,"ui-icons");}
                default->{}
            }
        }

        /** The six actives one after another through testAbility: the slot must draw hud/ability-(id), cooling (48 s) and, at GUI 2, ready. */
        void hudSweep(MinecraftClient c,int tick,boolean gui2){
            if(index>=ACTIVES.length){
                if(gui2){window(c,960,540,1);index=0;go(0,tick);stage=6;}
                else{window(c,1920,1080,2,true);server(c,b->b.testAbility=null);index=0;go(0,tick);stage=7;}
                return;
            }
            String id=ACTIVES[index],gui=gui2?"gui2":"gui1";int k=at(tick);var t=BattleHud.texts();
            if(id.equals(HIDDEN)){index++;go(0,tick);return;} // no slot to draw a picture in
            switch(sub){
                case 0->{server(c,b->b.testAbility=new GravesAndGlory.Battle.AbilityState(id,960,1800));go(1,tick);}
                case 1->{
                    boolean up=GravesAndGloryClient.abilityId.equals(id)&&id.equals(t.get("ability"))&&"48".equals(t.get("ability-seconds"));
                    if(up&&k>=4){UiRecruitChecks.park(c);go(2,tick);}
                    else if(k>200){hudOk=false;hudWhy+=(hudWhy.isEmpty()?"":"; ")+id+" never showed in the slot at "+gui+" ("+slot()+") WRONG";index++;go(0,tick);}
                }
                case 2->{
                    if(k==3){shot(c,"ui-icons-hud-"+id+"-cool-"+gui);boolean pic=BattleHud.abilityIcon(id).equals(t.get("ability-icon"));hudOk&=pic;
                        hudWhy+=(hudWhy.isEmpty()?"":"; ")+id+" "+gui+" cooling: "+slot()+(pic?"":" WRONG");
                        if(gui2){server(c,b->b.testAbility=new GravesAndGlory.Battle.AbilityState(id,0,1800));go(3,tick);}else{index++;go(0,tick);}}
                }
                case 3->{
                    boolean up=GravesAndGloryClient.abilityId.equals(id)&&id.equals(t.get("ability"))&&GravesAndGloryClient.abilityRemaining==0&&"".equals(t.get("ability-seconds"));
                    if(up&&k>=4){shot(c,"ui-icons-hud-"+id+"-ready-"+gui);boolean pic=BattleHud.abilityIcon(id).equals(t.get("ability-icon"));hudOk&=pic;
                        hudWhy+=" | ready: "+slot()+(pic?"":" WRONG");index++;go(0,tick);}
                    else if(k>200){hudOk=false;hudWhy+=" | "+id+" ready never drew ("+slot()+") WRONG";index++;go(0,tick);}
                }
                default->{index++;go(0,tick);}
            }
        }

        /** The class path: testAbility cleared, the squad's class set on the server, the packet's active fills the slot (the Quartermaster's
         *  too once the build has Forward Supply; an active with no behaviour here, ready below 0, leaves it bare). */
        void real(MinecraftClient c,int tick){
            if(index>=Spec.CLASSES.length){stage=8;return;}
            var cl=Spec.CLASSES[index];int k=at(tick);var t=BattleHud.texts();
            switch(sub){
                case 0->{if(k>=10){serverClass=cl.id();go(1,tick);}}
                case 1->{
                    boolean named=GravesAndGloryClient.commanderActive.equals(cl.active());
                    boolean drawn=BattleHud.rects().containsKey("ability")&&cl.active().equals(t.get("ability"));
                    if(named&&k>=8&&(drawn||cl.active().equals(HIDDEN)||GravesAndGloryClient.commanderReady<0)){
                        UiRecruitChecks.park(c);String pic=t.getOrDefault("ability-icon","");
                        boolean right=drawn?!cl.active().equals(HIDDEN)&&BattleHud.abilityIcon(cl.active()).equals(pic):!BattleHud.rects().containsKey("ability");
                        realOk&=right;realWhy+=(realWhy.isEmpty()?"":"; ")+cl.id()+": the packet names "+GravesAndGloryClient.commanderActive+" (ready in "+GravesAndGloryClient.commanderReady+", running "+GravesAndGloryClient.commanderFor+"), the slot "+(drawn?"drew "+pic+" ("+slot()+")":cl.active().equals(HIDDEN)?"was hidden, as the Quartermaster's is":"stayed bare")+(right?"":" WRONG");
                        go(2,tick);}
                    else if(k>300){realOk=false;realWhy+=(realWhy.isEmpty()?"":"; ")+cl.id()+": the slot never followed the class (packet "+GravesAndGloryClient.commanderActive+", ready "+GravesAndGloryClient.commanderReady+", "+slot()+") WRONG";index++;go(0,tick);}
                }
                case 2->{if(k==3){shot(c,"ui-icons-hud-"+cl.id()+"-real-gui2");index++;go(0,tick);}}
                default->{index++;go(0,tick);}
            }
        }
    }

    /** UI polish M11, the last suite listed (it leaves the world): a disconnect empties what the battle HUD keeps between frames and
     *  puts the command state back as a new client starts it. A practice round on map 0 draws the pinned lesson 4; then a feed line,
     *  an attack order aimed down the field (the hint slip up, the selected group's card with its key row, the lesson note hidden, its wrap and lines
     *  kept) with ranged, the wedge and a set facing chosen, a ping on the map, the clock at 7:42 and a knight killed (a card flash).
     *  The guard is honest (Task 3 re-review Minor 6): every record must hold something at once before the client leaves, or the
     *  check fails naming the empty ones, since "empty after" proves nothing of a record that was empty before. Then the client
     *  leaves as the pause menu's Save and Quit does, and this suite's own disconnect listener, which runs after the mod's, empties
     *  the client's task queue as MinecraftClient.disconnect's cancelTasks can (the re-review's race): a clear the mod queued would
     *  be lost. No HUD frame runs without a world, so 20 ticks after it is gone nothing may be held: what the last frame drew, the
     *  feed (its raw size, expired lines too), the aimed order, the groups and flashes, the pings, the clock, the selection, the
     *  formation and the facing, the lesson wrap and lines, the hint layer's clock and the chat lift. p1-ui-keybind-scope: on the
     *  mod's server the use key's keybind text reads RMB, off it the game's own words. */
    static final class UiLeaveChecks implements Suite,ServerWatch,HudWatch {
        volatile boolean want,fixed;volatile UUID player;volatile String thread="",wiped="";
        // client thread
        int stage,waitFrom,waited,left;String before="",useIn="";
        // registered when the suite starts, after the mod's own handler, so it runs after it: which thread the event fires on, then the wipe
        UiLeaveChecks(){ClientPlayConnectionEvents.DISCONNECT.register((h,c)->{if(current!=this)return;thread=Thread.currentThread().getName();wiped=wipeTasks(c);});}
        /** MinecraftClient.disconnect's own first step, ThreadExecutor.cancelTasks (protected): whatever the client had queued is dropped. */
        static String wipeTasks(MinecraftClient c){
            try{var m=net.minecraft.util.thread.ThreadExecutor.class.getDeclaredMethod("cancelTasks");m.setAccessible(true);int n=c.getTaskCount();m.invoke(c);return "wiped "+n+" queued task(s)";}
            catch(ReflectiveOperationException e){return "could not empty the task queue: "+e;}
        }
        public void serverTick(GravesAndGlory.Battle b){if(want){var p=player==null?null:b.server.getPlayerManager().getPlayer(player);if(p!=null&&skirmish(b,p,0,1,"normal")){want=false;fixed=true;}}}
        public void hudFrame(MinecraftClient c){}
        /** What the battle HUD and the command state keep between frames. */
        static String records(MinecraftClient c){
            return "rects "+BattleHud.rects().keySet()+", "+BattleHud.texts().size()+" texts, "+BattleHud.cards().size()+" cards, "+BattleHud.seals().size()+" seals, hint rows "+BattleHud.hintRows()+" ending at "+BattleHud.hintEnds()+", "+BattleHud.keyRows().size()+" card key rows, "+BattleHud.feedRows().size()+" feed rows drawn, "+EventFeed.size()+" feed lines held, order aimed "+CommandMode.active()+" (at "+CommandMode.destination+", "+CommandMode.preview.size()+" preview marks), "+CommandMode.groups.size()+" groups, "+CommandMode.flashUntil.size()+" flashes ("+CommandMode.flashFrom.size()+" strengths), "+CommandMode.pings.size()+" pings, clock "+CommandMode.seconds+" s, selected "+CommandMode.selected+", formation "+CommandMode.formation+", facing "+CommandMode.facing+(CommandMode.facingSet?" (set)":"")+", lesson wrap "+(BattleHud.wrappedFor()!=null)+", "+GravesAndGloryClient.lessonDrawn.size()+" lesson lines, round clock "+BattleHud.roundShownMs+", chat lift "+ChatPlacement.lift(c.getWindow().getScaledHeight());
        }
        /** Each record by name, and whether it holds anything: the command state's when it differs from a new client's. */
        static Map<String,Boolean> held(MinecraftClient c){
            Map<String,Boolean> m=new LinkedHashMap<>();
            m.put("rects",!BattleHud.rects().isEmpty());m.put("texts",!BattleHud.texts().isEmpty());m.put("cards",!BattleHud.cards().isEmpty());m.put("seals",!BattleHud.seals().isEmpty());
            m.put("hint rows",!BattleHud.hintRows().isEmpty());m.put("hint row ends",!BattleHud.hintEnds().isEmpty());m.put("card key rows",!BattleHud.keyRows().isEmpty());m.put("feed rows",!BattleHud.feedRows().isEmpty());m.put("feed lines",EventFeed.size()>0);
            m.put("aimed order",CommandMode.active());m.put("destination",CommandMode.destination!=null);m.put("preview",!CommandMode.preview.isEmpty());m.put("groups",!CommandMode.groups.isEmpty());
            m.put("flashes",!CommandMode.flashUntil.isEmpty());m.put("flash strengths",!CommandMode.flashFrom.isEmpty());m.put("pings",!CommandMode.pings.isEmpty());m.put("clock",CommandMode.seconds!=600);
            m.put("selection",!CommandMode.selected.equals("all"));m.put("formation",!CommandMode.formation.equals("block"));m.put("facing",CommandMode.facingSet||CommandMode.facing!=0);
            m.put("lesson wrap",BattleHud.wrappedFor()!=null);m.put("lesson lines",!GravesAndGloryClient.lessonDrawn.isEmpty());m.put("round clock",BattleHud.roundShownMs!=0);m.put("chat lift",ChatPlacement.lift(c.getWindow().getScaledHeight())!=0);
            return m;
        }
        static List<String> named(Map<String,Boolean> m,boolean value){return m.entrySet().stream().filter(e->e.getValue()==value).map(Map.Entry::getKey).toList();}
        public void step(MinecraftClient c,int tick){
            if(stage<3)CampaignClient.tutorial=3; // lesson 4 pinned, so the lesson card draws
            if(stage==1&&tick-waitFrom>3000){report("p1-ui-leave-clear",false,"no round with its lesson card 3000 ticks in: "+records(c));stage=9;finish(c,"ui-leave");return;}
            if(stage==2&&tick-waitFrom>600){report("p1-ui-leave-clear",false,"600 ticks after the round showed, these records still held nothing: "+named(held(c),false)+" ("+records(c)+")");CommandMode.cancel();stage=9;finish(c,"ui-leave");return;}
            switch(stage){
                case 0->{
                    if(tick<20)return;
                    if(!queue.isEmpty()){report("p1-ui-leave-clear",false,"ui-leave leaves the world, so it must be the last suite listed; still queued: "+queue);stage=9;finish(c,"ui-leave");return;}
                    c.setScreen(null);player=c.player.getUuid();want=true;waitFrom=tick;stage=1;
                }
                case 1->{ // the round on screen with its lesson card: then every record is given something to hold
                    if(c.currentScreen!=null)c.setScreen(null);
                    if(fixed&&GravesAndGloryClient.battleActive()&&!CampaignClient.cinematic()&&BattleHud.rects().containsKey("lesson")){
                        useIn=net.minecraft.text.Text.keybind("key.use").getString(); // on the mod's server
                        EventFeed.push("notice",GravesAndGloryClient.team,"Leaving the field",net.minecraft.util.Util.getMeasuringTimeMs());
                        CommandMode.begin("attack");CommandMode.selected="ranged";CommandMode.formation="wedge";CommandMode.facing=45;CommandMode.facingSet=true;
                        c.player.setPitch(40); // looking down at the field before the player, so the aimed order has a destination and a preview
                        UUID id=player;server(c,b->{var p=b.server.getPlayerManager().getPlayer(id);var own=b.squads.get(id);if(p==null||own==null)return;
                            b.ping(p,Spec.PINGS[0].id(),UiHudChecks.AX,UiHudChecks.AZ); // on the Dawn road, inside the arena
                            b.roundStarted=b.time-(b.roundSeconds-462)*20L; // the clock at 7:42
                            for(var u:b.units)if(u.squad==own&&u.spec.group().equals("infantry")&&u.mob.isAlive()){u.mob.kill();break;} // a knight falls: the infantry card flashes
                            b.units.removeIf(u->!u.mob.isAlive());});
                        waitFrom=tick;stage=2;
                    }
                }
                case 2->{ // every record held at once, then the pause menu's Save and Quit (GameMenuScreen.disconnect), outside the tick
                    if(c.currentScreen!=null)c.setScreen(null);
                    if(EventFeed.size()==0)EventFeed.push("notice",GravesAndGloryClient.team,"Leaving the field",net.minecraft.util.Util.getMeasuringTimeMs()); // a feed line lives 6 s at Compact (UI Task 8c)
                    if(named(held(c),false).isEmpty()){
                        before=records(c);stage=3;
                        c.send(()->{if(c.world!=null)c.world.disconnect();c.disconnect(new net.minecraft.client.gui.screen.MessageScreen(net.minecraft.text.Text.translatable("menu.savingLevel")));c.setScreen(new net.minecraft.client.gui.screen.TitleScreen());});
                    }
                }
                default->{}
            }
        }
        /** Runs with no world too, where step() does not: 20 ticks after the world and the server are gone, nothing may be held. */
        @Override public void clientTick(MinecraftClient c){
            if(stage!=3)return;
            if(c.world==null&&c.getServer()==null)left++;
            if(left<20&&++waited<=1200)return;
            var kept=named(held(c),true);
            report("p1-ui-leave-clear",left>=20&&!thread.isEmpty()&&wiped.startsWith("wiped")&&kept.isEmpty(),"before the Save and Quit every record held something: "+before+"; the disconnect event fired on the thread '"+thread+"', and this suite's listener after the mod's "+wiped+"; "+(left>=20?"20 client ticks with no world later: ":"the world was not gone 1200 ticks after the Save and Quit: ")+records(c)+(kept.isEmpty()?"; nothing held":"; STILL HELD: "+kept));
            String useOut=net.minecraft.text.Text.keybind("key.use").getString(),right=net.minecraft.client.util.InputUtil.Type.MOUSE.createFromCode(GLFW.GLFW_MOUSE_BUTTON_RIGHT).getLocalizedText().getString();
            report("p1-ui-keybind-scope",left>=20&&"RMB".equals(useIn)&&useOut.equals(right)&&!useOut.equals("RMB"),"the use key's keybind text (Text.keybind(\"key.use\"), the camp sign's and the notices' kind) read '"+useIn+"' on the mod's server (wanted RMB) and '"+useOut+"' off it (wanted the game's own '"+right+"')");
            stage=9;finish(c,"ui-leave");
        }
    }
}
