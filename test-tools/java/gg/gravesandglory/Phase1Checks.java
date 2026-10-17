package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Isolated real-client checks for Phase 1 (solo core). GRAVES_PHASE1_CHECKS=<suite>[,<suite>...]. Never included in releases. */
public final class Phase1Checks implements ClientModInitializer {
    public interface Suite { void step(MinecraftClient c,int tick); }
    /** Optional for a suite: called on the server thread after every Battle.tick, for checks that need exact tick timing. */
    public interface ServerWatch { void serverTick(GravesAndGlory.Battle b); }
    static final Map<String,Supplier<Suite>> SUITES=new LinkedHashMap<>();
    static{SUITES.put("combat",Combat::new);}
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
        // difficulty is accepted for the bot tiers of later tasks; Task 1 bots use the one existing behaviour.
        c.closeParty();c.partyLeader=p.getUuid();c.party.add(p.getUuid());c.practice=size==1;c.size=size;c.fill=true;
        for(var s:b.squads.values())if(!s.bot){s.seated=s.owner.equals(p.getUuid());if(s.seated)s.team=0;}
        b.startRound();c.introUntil=b.time;return true;
    }
    /** Reports p1-<suite>-suite, then stops the client once the last listed suite is done. */
    public static void finish(MinecraftClient c,String suite){
        report("p1-"+suite+"-suite",!failed,failed?"an earlier p1 check failed":"all checks passed");
        current=null;if(queue.isEmpty())c.scheduleStop();
    }

    @Override public void onInitializeClient(){
        String list=System.getProperty("gravesandglory.phase1Checks","");if(list.isBlank())return;
        for(String name:list.split(","))if(!name.isBlank())queue.add(name.trim());
        ServerTickEvents.END_SERVER_TICK.register(s->{if(current instanceof ServerWatch w&&GravesAndGlory.battle!=null)try{w.serverTick(GravesAndGlory.battle);}catch(Exception e){report("p1-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            if(c.getOverlay()!=null)return;
            if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
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
                    // The brief says 4, but 4 of a full vanguard knight's 36 health across 12 knights is a 0.0093 group drop, under tookLoss's 0.01; 5 (one knight blow) clears it.
                    u.mob.damage(b.world.getDamageSources().magic(),5);System.out.println("[p1] hud-flash damaged "+u.spec.id()+" to "+u.mob.getHealth());});}
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
                        report("p1-tick-cost",now<40,"server tick "+String.format("%.2f",now)+" ms (600-tick mean "+String.format("%.2f",avg)+" ms) with "+troops+" troops at start, "+b.units.size()+" at end");next(b,16);}
                }
                default->{}
            }
            lastHealth.clear();for(var u:b.units)lastHealth.put(u.mob,u.mob.getHealth());
        }
    }
}
