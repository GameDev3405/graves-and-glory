package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
    static{SUITES.put("combat",Combat::new);SUITES.put("morale",MoraleChecks::new);SUITES.put("reinforce",ReinforceChecks::new);SUITES.put("bots",Bots::new);}
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
                        report("p1-tick-cost",now<40,"server tick "+String.format("%.2f",now)+" ms (600-tick mean "+String.format("%.2f",avg)+" ms) with "+troops+" troops at start, "+b.units.size()+" at end");next(b,16);}
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
                case 8->{ // p1-forward-message: the deploy notice names the point, or the barracks
                    ideal(b);String at=anchor==null?"":b.deployNote(own,anchor),home=b.deployNote(own,null);
                    report("p1-forward-message",anchor==point&&at.contains(point.spec.name())&&home.contains("barracks"),"anchored note \""+at+"\", barracks note \""+home+"\"");
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
        /** The old lane rule, written out again, so the check does not lean on the code under test. */
        static int laneRule(GravesAndGlory.Battle b,GravesAndGlory.Squad s,Vec3d centre){
            int best=-1;double value=Double.MAX_VALUE;
            for(int i=0;i<b.points.size();i++){var p=b.points.get(i);double v=Math.hypot(centre.x-p.spec.x(),centre.z-p.spec.z())+(p.owner==s.team?140:0)+(i==s.lane?0:90)-(p.contested?55:0);if(v<value){value=v;best=i;}}
            return best;
        }
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
                    var row=b.campaign.tier(hard);
                    b.campaign.botDecide(hard,row);int none=chosen(b,hard);
                    plant(b,1,9);b.campaign.botDecide(hard,row);int defended=chosen(b,hard);
                    for(var u:foes)u.mob.discard(); // the nine fall, but the memory of them stays
                    b.campaign.botDecide(hard,row);int stale=chosen(b,hard);b.units.removeAll(foes);foes.clear();
                    hard.tier="normal";var normalRow=b.campaign.tier(hard);b.campaign.botDecide(hard,normalRow);int normal=chosen(b,hard),lane=laneRule(b,hard,centre(b,hard));
                    hard.tier="hard";for(var memory:b.sightings.values())memory.clear();
                    var east=b.points.get(4).spec;place(b,hard,east.x()-25,east.z());b.campaign.botDecide(hard,row);int reach=chosen(b,hard);
                    boolean ok=b.points.size()==5&&row.id().equals("hard")&&none==1&&defended>=0&&defended<5&&defended!=1&&stale==1&&normal==1&&normal==lane&&reach==4;
                    report("p1-bots-least-defended",ok,b.points.size()+" points; Hard near "+name(b,1)+" with no sightings -> "+name(b,none)+", with nine Dusk sightings on it -> "+name(b,defended)+", with those nine dead but still remembered -> "+name(b,stale)+"; Normal on the same state -> "+name(b,normal)+" (lane rule: "+name(b,lane)+"); Hard near "+name(b,4)+" -> "+name(b,reach)+" (index above 2 needs the arrays sized from battle.points)");
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
                    if(shotAsked&&wantShot==null){for(var u:foes)u.mob.discard();b.units.removeAll(foes);foes.clear();next(b,13);}
                }
                case 13->{ // p1-bots-charge-timing: eight frozen Dusk revenants 60 blocks away, then 15 blocks away
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
}
