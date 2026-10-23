package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
    static final Map<String,Supplier<Suite>> SUITES=new LinkedHashMap<>();
    static{SUITES.put("combat",Combat::new);SUITES.put("morale",MoraleChecks::new);SUITES.put("reinforce",ReinforceChecks::new);SUITES.put("bots",Bots::new);SUITES.put("tutorial",TutorialChecks::new);SUITES.put("report",ReportChecks::new);}
    static final ArrayDeque<String> queue=new ArrayDeque<>();
    static volatile Suite current;static volatile String currentName="";static volatile boolean failed;
    static int tick,front;static boolean started;

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

    @Override public void onInitializeClient(){
        String list=System.getProperty("gravesandglory.phase1Checks","");if(list.isBlank()||!System.getProperty("gravesandglory.phase2Checks","").isEmpty())return;
        for(String name:list.split(","))if(!name.isBlank())queue.add(name.trim());
        ServerTickEvents.END_SERVER_TICK.register(s->{if(current instanceof ServerWatch w&&GravesAndGlory.battle!=null)try{w.serverTick(GravesAndGlory.battle);}catch(Exception e){report("p1-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            if(c.getOverlay()!=null)return;
            if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            listenSounds(c);
            if(current==null){
                if(queue.isEmpty())return;
                currentName=queue.poll();failed=false;tick=0;var make=SUITES.get(currentName);
                if(make==null){report("p1-"+currentName+"-suite",false,"unknown suite");if(queue.isEmpty())c.scheduleStop();return;}
                current=make.get();
            }
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
                case 8->{ // p1-forward-message: the deploy notice names the point, or the barracks
                    ideal(b);String at=anchor==null?"":b.deployNote(own,anchor),home=b.deployNote(own,null);
                    report("p1-forward-message",GravesAndGlory.Anchor.of(point).equals(anchor)&&at.contains(point.spec.name())&&home.contains("barracks"),"anchored note \""+at+"\", barracks note \""+home+"\"");
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
            if(stage==4){ // the camp home page after difficulty:hard, then a real press of the Bots button
                if(homeAt==-1){homeAt=tick;CampScreen.open("home");}
                else if(tick==homeAt+25){
                    var bots=button(c.currentScreen,"Bots: ");
                    homeOk=c.currentScreen instanceof CampScreen&&bots!=null&&bots.getMessage().getString().equals("Bots: Hard")&&bots.active;
                    homeWhy=bots==null?"no Bots button on the camp home page":"\""+bots.getMessage().getString()+"\", active="+bots.active;
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
                    report("p1-bots-tiers",lobbyOk&&commandOk&&homeOk&&buttonOk&&tiersOk&&refusedOk,"lobby: "+lobbyWhy+"; difficulty:hard set it="+commandOk+"; camp home: "+homeWhy+"; button press reached the server="+buttonOk+"; solo pad 3v3: "+tiersWhy+"; difficulty:easy refused while running (still "+c.difficulty+")="+refusedOk);
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
        int steady=-9,steadyTicks,clientGoldBefore=-1,clientGoldAfter=-1,cinematicTicks,beforeAt=-1,afterAt=-1,waited;String copyWhy="",beforeWhy="no camp home read";boolean beforePrimary,briefingShot,startReported,padReported;

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
        /** The battle 1 button on the camp home page, or null. */
        static ButtonWidget march(Screen screen){
            if(screen instanceof CampScreen)for(var e:screen.children())if(e instanceof ButtonWidget w&&w.getMessage().getString().toUpperCase(Locale.ROOT).startsWith("THE LONG MARCH"))return w;
            return null;
        }
        static boolean primary(ButtonWidget w){return w instanceof GravesAndGloryClient.FantasyButton f&&f.primary;}
        static String describe(ButtonWidget w){return w==null?"no battle 1 button":"\""+w.getMessage().getString()+"\" primary="+primary(w);}
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
                boolean whole=joined.replace(" ","").equals(full.replace(" ","")),fits=drawn.stream().allMatch(l->c.textRenderer.getWidth(l)<=GravesAndGloryClient.LESSON_TEXT_WIDTH);
                lessonWhole[now]=whole&&fits;copyWhy+=(copyWhy.isEmpty()?"":"; ")+"lesson "+(now+1)+": "+drawn.size()+" line(s), whole="+whole+", inside "+GravesAndGloryClient.LESSON_TEXT_WIDTH+" px="+fits+(whole?"":", drawn ends \""+joined.substring(Math.max(0,joined.length()-16))+"\"");
            }
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(stage==2){ // the camp home before the battle: battle 1 is the primary button
                if(beforeAt<0){beforeAt=tick;CampScreen.open("home");}
                else if(tick==beforeAt+25){var w=march(c.currentScreen);beforePrimary=primary(w);beforeWhy=describe(w);shot(c,"tutorial-camp-before");}
                else if(tick==beforeAt+30){c.setScreen(null);server(c,b->next(b,3));}
            }
            if(stage>=4&&stage<=6){
                if(CampaignClient.cinematic()&&!briefingShot&&++cinematicTicks==40){shot(c,"tutorial-briefing");briefingShot=true;reportStart();}
                watch(c,first,true);
            }
            if(stage==6&&c.currentScreen!=null)c.setScreen(null); // the barracks lesson opens the barracks screen
            if(stage==6&&first.over&&++first.wait==40){ // 40 ticks for the lesson end and the new gold to reach the client
                reportStart();clientGoldAfter=CampaignClient.profile.gold;int shots=0,whole=0;for(int i=0;i<4;i++){if(lessonShot[i])shots++;if(lessonWhole[i])whole++;}
                String use=c.options.useKey.getBoundKeyLocalizedText().getString();boolean named=GravesAndGloryClient.lessonText(3).contains(use); // lesson 4 names the use key as bound (drawn whole above)
                report("p1-tutorial-lessons",first.taught()&&shots==4&&whole==4&&named,first.why()+"; "+shots+" of 4 lesson screenshots; "+whole+" of 4 lessons drawn whole inside the panel ("+copyWhy+"); lesson 4 names the use key \""+use+"\": "+named);
                server(c,b->next(b,7));
            }
            if(stage==8){ // the camp home after the victory: battle 1 is a normal button; pressing it is the replay
                if(afterAt<0){if(c.currentScreen instanceof CampScreen s&&s.page.equals("results")||++waited>=60){afterAt=tick;CampScreen.open("home");}}
                else if(tick==afterAt+25){var w=march(c.currentScreen);
                    report("p1-tutorial-clear",clearOk&&CampaignClient.profile.campaignCleared==1&&beforePrimary&&w!=null&&!primary(w),clearWhy+"; client campaignCleared "+CampaignClient.profile.campaignCleared+"; camp home before the battle: "+beforeWhy+"; after the victory: "+describe(w));
                    shot(c,"tutorial-camp-after");
                }
                else if(tick==afterAt+30){var w=march(c.currentScreen);
                    if(w!=null){w.onPress();server(c,b->next(b,9));}
                    else{replay.commands="no battle 1 button to replay from";replay.over=true;c.setScreen(null);server(c,b->next(b,12));}
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
}
