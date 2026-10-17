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
    static{SUITES.put("combat",Combat::new);SUITES.put("morale",MoraleChecks::new);}
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
}
