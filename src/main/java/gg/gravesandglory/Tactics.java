package gg.gravesandglory;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import java.util.*;

/** Server-authoritative targeting, separation, bridge crossing and local path recovery. */
final class Tactics {
    static final Spec.NavigationRow N=Spec.NAVIGATION[0];
    /** The stances a troop shows its clients (stance_v1 sends the index): none, hold, brace, fall_back and run. */
    static final List<String> STANCES=List.of("","hold","brace","fall_back","run");
    /** stance_v1 packets sent: on a change at a unit's beat, to a player who starts tracking a troop, and to a player whose client registers the channel late (the harness reads them). */
    static volatile int stanceSends,trackingSends,registerSends;
    /** The stance a unit's order gives it (Phase 2 Task 5b; the unit AI phase's stances sheet maps its own rows here later). An arrived Retreat or Move group (arrived) stands as on Hold. */
    static String stance(GravesAndGlory.Battle b,GravesAndGlory.Unit u){var a=b.assignment(u);return stance(arrived(b,u,a)?held(a):a,u);}
    /** Hold's order row, which an arrived Retreat or Move group fights by. */
    static final Spec.OrdersRow HOLD=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals("hold")).findFirst().orElse(Spec.ORDERS[0]);
    /** The assignment as on Hold at the same spot, formation and facing: what an arrived group stands and fights by. */
    static GravesAndGlory.Assignment held(GravesAndGlory.Assignment a){var h=new GravesAndGlory.Assignment("hold",a.formation,a.target,a.yaw);h.issued=a.issued;return h;}
    /**
     * Arrival (the no-routing design's section 6): a group under an order that engages nothing (Retreat, Move) has arrived once its centre (Morale.members:
     * its living troops that are not temporary) stands within combatDiscipline.arriveRadius of the centre of its own slots (slotCentre: the spots its
     * troops walk to, which Battle.formation puts behind the order's spot, 5, 7 and 9 blocks and the rows for ranged, siege and support), and from then
     * until its next order it fights as on Hold, from its slot and with Hold's engage radius. The arrival is latched on the squad (Squad.arrived): a Hold
     * fight that pulls the group out of the radius does not undo it; Battle.issue, the command path, Campaign.botDecide and Battle.startRound clear it.
     * On the way it fights nothing, even when struck. A bot's Retreat to its base and Muster's Move orders arrive the same way. The unit AI plan's arrive
     * column replaces this later.
     */
    static boolean arrived(GravesAndGlory.Battle b,GravesAndGlory.Unit u,GravesAndGlory.Assignment a){
        var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(a.order)).findFirst().orElse(Spec.ORDERS[0]);
        if(order.engage())return false;
        String group=u.spec.group();if(u.squad.arrived.contains(group))return true;
        var members=Morale.members(b,u.squad,group);if(members.isEmpty())return false;
        double r=Spec.COMBATDISCIPLINE[0].arriveRadius();
        if(GravesAndGlory.Battle.horizontalDistance(Morale.centre(members),slotCentre(b,members))>r*r)return false;
        u.squad.arrived.add(group);return true;
    }
    /** The centre of the members' formation slots (Battle.formation): where their walk ends. */
    static Vec3d slotCentre(GravesAndGlory.Battle b,List<GravesAndGlory.Unit> members){Vec3d sum=Vec3d.ZERO;for(var m:members)sum=sum.add(b.formation(m));return sum.multiply(1.0/members.size());}
    static String stance(GravesAndGlory.Assignment a,GravesAndGlory.Unit u){
        return switch(a.order){
            case "hold"->"shieldwall".equals(a.formation)||ArmyMobs.tags(u.spec).contains("spear")?"brace":"hold";
            case "fall_back"->"fall_back";
            case "charge","retreat"->"run";
            default->"";
        };
    }
    /** Sends a unit's stance to each player that can take stance_v1: its entity id and the stance's index in STANCES. Returns how many it sent. */
    static int sendStance(GravesAndGlory.Unit u,Collection<ServerPlayerEntity> players){
        int index=Math.max(0,STANCES.indexOf(u.stance)),sent=0;
        for(ServerPlayerEntity p:players){
            if(!ServerPlayNetworking.canSend(p,GravesAndGlory.STANCE))continue;
            var buf=PacketByteBufs.create();buf.writeVarInt(u.mob.getId());buf.writeByte(index);ServerPlayNetworking.send(p,GravesAndGlory.STANCE,buf);sent++;
        }
        return sent;
    }
    /** Sends a player the stance of every troop it tracks that has one: for a player whose client registered stance_v1 after its trackers started. Returns how many it sent. */
    static int sendTracked(GravesAndGlory.Battle b,ServerPlayerEntity p){
        int sent=0;
        for(GravesAndGlory.Unit u:b.units)if(!u.stance.isEmpty()&&PlayerLookup.tracking(u.mob).contains(p))sent+=sendStance(u,List.of(p));
        return sent;
    }
    /** Between beats a shield troop re-reads the distance to the enemy it chose on its beat, so a block knight lowers its shield on the tick
     *  that enemy steps into sword reach (BattleMath.raiseShield). With no live target it keeps the beat's decision until the next beat. */
    static void brace(GravesAndGlory.Battle battle,GravesAndGlory.Unit u){
        var t=u.lockedTarget;if(!u.tags.contains("shield")||t==null||!t.isAlive())return;
        var a=battle.assignment(u);double reach=Abilities.range(battle,u);boolean shield=BattleMath.raiseShield(a.formation,a.order,u.mob.squaredDistanceTo(t),reach*reach,true);
        if(shield&&!u.mob.isUsingItem())u.mob.setCurrentHand(Hand.OFF_HAND);else if(!shield&&u.mob.isUsingItem())u.mob.stopUsingItem();
    }
    /** How far a troop picks its targets: the order's engage radius, or for a siege troop (tag siege) at least its weapon's reach (Entrench's bonus included), so artillery shoots what it can reach and not only what the order's leash allows. The sheet's order radii are untouched. */
    static double engageRadius(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,double order){return u.tags.contains("siege")?Math.max(order,Abilities.range(battle,u)):order;}
    static void tick(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,long time){
        if(time%10!=u.slot%10){brace(battle,u);return;} // the shield is decided every tick; everything else on the unit's 10-tick beat
        var mob=u.mob;var s=u.squad;var a=battle.assignment(u);
        Houses.sample(battle,u); // Entrench: has it stood still since its last beat
        Houses.swim(battle,u); // Riptide: faster while it touches water or it rains
        boolean arrived=arrived(battle,u,a); // a Retreat or Move group at its destination fights as on Hold from its slot (arrived)
        String stance=stance(arrived?held(a):a,u);if(!stance.equals(u.stance)){u.stance=stance;stanceSends+=sendStance(u,PlayerLookup.tracking(mob));}
        var owner=battle.server.getPlayerManager().getPlayer(s.owner);
        if(a.order.equals("follow")&&owner!=null&&!battle.respawns.containsKey(s.owner)){a.target=owner.getPos();a.yaw=owner.getYaw();}
        var order=arrived?HOLD:Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(a.order)).findFirst().orElse(Spec.ORDERS[0]);
        boolean hold=a.order.equals("hold")||arrived;
        boolean holdSiege=hold&&u.tags.contains("siege"); // artillery on Hold shoots what is within its reach from where it stands and never walks toward a target, so it never walks out of Entrench
        double engage=holdSiege?Abilities.range(battle,u):engageRadius(battle,u,order.engageRadius()); // the order's radius, widened to the weapon's reach for a siege troop; on Hold the weapon's reach as it is now
        LivingEntity enemy=null,close=null;double nearest=engage*engage,closest=nearest; // enemy: the one it picks (never one inside its minimum range); close: the nearest of any, which a ranged troop backs off from
        if(order.engage()&&u.lockedTarget!=null&&u.lockedTarget.isAlive()&&time<u.lockUntil&&mob.squaredDistanceTo(u.lockedTarget)<nearest*(holdSiege?1:1.4)&&!Abilities.tooClose(u,mob.squaredDistanceTo(u.lockedTarget))&&(!hold||holdSiege||GravesAndGlory.Battle.horizontalDistance(u.lockedTarget.getPos(),a.target)<nearest))enemy=u.lockedTarget;
        if(order.engage()&&a.enemy>=0&&battle.world.getEntityById(a.enemy) instanceof LivingEntity explicit&&explicit.isAlive()&&mob.canSee(explicit)&&!Abilities.tooClose(u,mob.squaredDistanceTo(explicit))){enemy=explicit;a.target=explicit.getPos();}
        if(enemy!=null){nearest=mob.squaredDistanceTo(enemy);close=enemy;closest=nearest;}
        boolean locked=enemy!=null;
        for(var other:battle.units)if(order.engage()&&!locked&&other.squad.team!=s.team&&other.mob.isAlive()){
            double d=mob.squaredDistanceTo(other.mob);
            boolean inLeash=!hold||holdSiege||GravesAndGlory.Battle.horizontalDistance(other.mob.getPos(),a.target)<engage*engage;
            if(inLeash&&d<closest){close=other.mob;closest=d;}
            if(inLeash&&d<nearest&&!Abilities.tooClose(u,d)){enemy=other.mob;nearest=d;}
        }
        for(ServerPlayerEntity p:battle.server.getPlayerManager().getPlayerList()){
            var other=battle.squads.get(p.getUuid());if(order.engage()&&!locked&&other!=null&&other.team!=s.team&&!p.isSpectator()&&p.isAlive()&&(!hold||holdSiege||GravesAndGlory.Battle.horizontalDistance(p.getPos(),a.target)<engage*engage)){
                double d=mob.squaredDistanceTo(p);
                if(d<closest){close=p;closest=d;}
                if(d<nearest&&!Abilities.tooClose(u,d)){enemy=p;nearest=d;}
            }
        }
        if(order.engage()&&!locked&&enemy==null)for(var st:battle.structures){ // a supply banner is a target when no troop or commander is: the army first, then its commander, then its banner
            if(st.team()==s.team||!st.isAlive())continue;
            double d=mob.squaredDistanceTo(st);
            boolean inLeash=!hold||holdSiege||GravesAndGlory.Battle.horizontalDistance(st.getPos(),a.target)<engage*engage;
            if(inLeash&&d<closest){close=st;closest=d;}
            if(inLeash&&d<nearest&&!Abilities.tooClose(u,d)){enemy=st;nearest=d;}
        }
        if(enemy!=u.lockedTarget){u.lockedTarget=enemy;u.lockUntil=time+Spec.COMBATDISCIPLINE[0].targetMemoryTicks();}
        mob.setTarget(enemy);mob.setAttacking(enemy!=null);
        double reach=Abilities.range(battle,u);boolean ranged=u.spec.ranged();
        boolean shield=u.tags.contains("shield")&&BattleMath.raiseShield(a.formation,a.order,nearest,reach*reach,enemy!=null); // block: up on the approach, down to swing once the enemy is in reach; shieldwall: the full brace
        if(shield&&!mob.isUsingItem())mob.setCurrentHand(Hand.OFF_HAND);else if(!shield&&mob.isUsingItem())mob.stopUsingItem();
        Abilities.tick(battle,u,enemy,time); // mend heals and a caster's spell bursts on the beat
        double flee=Math.max(N.rangedRetreatDistance(),Abilities.minRange(u)); // a ranged troop backs off from an enemy this near; one with a minimum range (the Mortar) from anything inside it
        if(enemy!=null){
            mob.getLookControl().lookAt(enemy,40,40);
            boolean visible=mob.canSee(enemy);
            if(nearest<=reach*reach&&visible){
                if(ranged&&close!=null&&closest<flee*flee&&!hold&&!a.perch)backOff(battle,u,close,time);
                else if(a.perch&&ranged)toSlot(battle,u,a,time,order.moveMultiplier()); // it shoots on its way up and climbs on
                else mob.getNavigation().stop();
                if(time>=u.nextAttack&&time>=u.stunnedUntil&&!Abilities.tooClose(u,nearest)){
                    int cooldown=u.spec.cooldown();u.nextAttack=u.nextAttack<time-10?time+cooldown:u.nextAttack+cooldown;mob.swingHand(Hand.MAIN_HAND);
                    BattleEffects.strike(battle,u,enemy);
                    Abilities.attack(battle,u,enemy,time);
                    if(ranged){
                        Audio.shot(battle,u); // a caster's spell or a bowstring (AudioCues.MAGIC_CAST, BOW_SHOOT)
                    }
                }
            }else if(a.perch&&ranged){toSlot(battle,u,a,time,order.moveMultiplier()); // a ranged troop on a post shoots what it can reach from there and never walks off after the rest
            }else if(!holdSiege&&(!hold||GravesAndGlory.Battle.horizontalDistance(enemy.getPos(),a.target)<engage*engage)){
                Vec3d goal=enemy.getPos();
                if(ranged&&!visible){Vec3d toward=enemy.getPos().subtract(mob.getPos()).multiply(1,0,1).normalize();double sign=u.slot%2==0?1:-1;goal=mob.getPos().add(-toward.z*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep(),0,toward.x*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep());}
                if(!ranged){double angle=u.slot*2.399963;goal=goal.add(Math.cos(angle)*1.6,0,Math.sin(angle)*1.6);}
                if(ranged&&visible&&nearest>reach*reach)goal=goal.add(mob.getPos().subtract(goal).multiply(1,0,1).normalize().multiply(reach*N.rangedStandoff()));
                walk(battle,u,goal,time,order.moveMultiplier());
            }else mob.getNavigation().stop();
        }else if(close!=null&&ranged){ // everything in its leash stands inside its minimum range: nothing to shoot; it backs off (unless it holds) and does not walk off to its formation
            if(a.perch)toSlot(battle,u,a,time,order.moveMultiplier());else if(!hold&&closest<flee*flee)backOff(battle,u,close,time);else mob.getNavigation().stop();
        }else{
            toSlot(battle,u,a,time,order.moveMultiplier());
        }
    }
    /** To its formation slot, and still there facing the order's way once it stands on it: within arrivalTolerance across and, on a post (Assignment.perch), at its height, so that a troop under a tower top is not taken for one on it. */
    static void toSlot(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,GravesAndGlory.Assignment a,long time,double speed){
        MobEntity mob=u.mob;Vec3d goal=battle.formation(u);
        boolean there=GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal)<=N.arrivalTolerance()*N.arrivalTolerance()&&(!a.perch||Math.abs(mob.getY()-goal.y)<1.1);
        if(!there)walk(battle,u,goal,time,speed,a.perch);
        else{mob.getNavigation().stop();mob.setYaw(a.yaw);mob.setHeadYaw(a.yaw);mob.setBodyYaw(a.yaw);}
    }
    /** A raised goal (Assignment.perch) is climbed to by a whole vanilla path to its own height once it is this near across (blocks); walk's ground waypoints, which only know the terrain (Arena.ground), lead there until then. */
    static final double CLIMB_RANGE=24;
    /** Five blocks straight away from an enemy, at a hurried walk: how a ranged troop backs off from one too near. */
    static void backOff(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,LivingEntity from,long time){
        Vec3d away=u.mob.getPos().subtract(from.getPos()).multiply(1,0,1).normalize();
        walk(battle,u,u.mob.getPos().add(away.multiply(5)),time,1.05);
    }
    /** The way to a goal: across the river by the nearest bridge, unless the troop fords it (Riptide tier II and III), which goes straight at the goal. */
    static Vec3d route(GravesAndGlory.Unit u,Vec3d goal){
        if(Houses.fords(u))return goal;
        Vec3d here=u.mob.getPos();
        double side=here.x-Arena.riverX(here.z),destination=goal.x-Arena.riverX(goal.z);
        if(Math.signum(side)!=Math.signum(destination)&&Math.abs(destination)>7){
            Spec.StructuresRow best=null;double score=Double.MAX_VALUE;
            for(var bridge:Spec.STRUCTURES)if(bridge.kind().equals("bridge")){
                double entry=bridge.x()+Math.signum(side)*(bridge.width()/2.0-N.bridgeEntryInset());
                double d=Math.hypot(here.x-entry,here.z-bridge.z())+Math.hypot(goal.x-bridge.x(),goal.z-bridge.z());
                if(d<score){score=d;best=bridge;}
            }
            if(best!=null){
                double offset=(u.slot%5-2)*GravesAndGlory.Battle.spacing(u);
                double entry=best.x()+Math.signum(side)*(best.width()/2.0-N.bridgeEntryInset());
                boolean onBridge=Math.abs(here.z-best.z())<best.depth()/2.0-1&&Math.abs(here.x-best.x())<best.width()/2.0+2;
                double x=onBridge?best.x()+Math.signum(destination)*(best.width()/2.0+2):entry;
                return new Vec3d(x,best.elevation()+1,best.z()+offset);
            }
        }
        return houseGate(u,goal);
    }
    /** A goal inside the walled yard of a supply house (Phase 3 Task 8b): the point just outside a gate (HouseRoute) until the unit stands at it, then the goal itself, so that the units do not stand at the wall. Any other goal comes back as it is. */
    static Vec3d houseGate(GravesAndGlory.Unit u,Vec3d goal){
        for(var r:housesOfMap()){
            double[] p=HouseRoute.next(r.x(),r.z(),r.rotation(),u.mob.getX(),u.mob.getZ(),goal.x,goal.z,(u.slot%5-2)*0.8);
            if(p!=null)return new Vec3d(p[0],goal.y,p[1]);
        }
        return goal;
    }
    /** The landmarks rows of the supply houses of the selected map, looked up once per map (walk asks for them on every call of every unit). */
    static int housesAt=-1;static Spec.LandmarksRow[] houseRows=new Spec.LandmarksRow[0];
    static Spec.LandmarksRow[] housesOfMap(){
        int map=CampaignMaps.selected;
        if(housesAt!=map){
            var rows=new java.util.ArrayList<Spec.LandmarksRow>();
            for(var h:Spec.SUPPLYHOUSES){
                if(h.map()!=map)continue;
                for(var r:Spec.LANDMARKS)if(r.id().equals(h.landmark())&&r.map()==map)rows.add(r);
            }
            houseRows=rows.toArray(new Spec.LandmarksRow[0]);housesAt=map;
        }
        return houseRows;
    }
    /** How many times a stuck unit was set on the step beside it (StepHop); the route harness prints it. GRAVES_NO_HOP=1 leaves the hop out (the control of the house marches). */
    static int stepHops;static final boolean HOP=System.getenv("GRAVES_NO_HOP")==null;
    /** The unit stood still for the stuck time: when the cell beside it that is nearest its waypoint is one block up (a step under a low skirt of leaves, which a jump cannot make), it is set on that cell. */
    static boolean stepHop(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d goal){
        if(!HOP)return false;
        var w=battle.world;var mob=u.mob;
        int[] c=StepHop.up(mob.getX(),mob.getY(),mob.getZ(),goal.x,goal.z,new StepHop.Probe(){
            public boolean open(int x,int y,int z){var p=new BlockPos(x,y,z);return w.getBlockState(p).getCollisionShape(w,p).isEmpty()&&w.getFluidState(p).isEmpty();}
            public boolean floor(int x,int y,int z){var p=new BlockPos(x,y,z);return w.getBlockState(p).isSideSolidFullSquare(w,p,Direction.UP);}
        });
        if(c==null)return false;
        if(TRACE_HOPS)System.out.println("[Graves and Glory] step hop of slot "+u.slot+" from ("+String.format("%.1f,%.1f,%.1f",mob.getX(),mob.getY(),mob.getZ())+") to ("+c[0]+","+c[1]+","+c[2]+") on its way to ("+String.format("%.1f,%.1f",goal.x,goal.z)+")");
        mob.refreshPositionAndAngles(c[0]+.5,c[1],c[2]+.5,mob.getYaw(),mob.getPitch());mob.setVelocity(Vec3d.ZERO);stepHops++;
        PINS.remove(u);
        return true;
    }
    static final boolean TRACE_HOPS=System.getenv("GRAVES_ROUTE_TRACE")!=null;
    /** Counters of Tactics.walk (the route harness and the bot sim print them): units sent round by a detour, units that sidestepped, detour searches started, searches that found no path to look at, and the time the searches took (total and longest, in nanoseconds).
     *  GRAVES_NO_DETOUR=1 leaves the detour out (the control of the chapel nook goals). */
    static int detours,sidesteps,detourSearches,detourMisses,climbs,climbMisses;static long detourNanos,detourMaxNanos,lastSearchNanos;static final boolean DETOUR=System.getenv("GRAVES_NO_DETOUR")==null;
    /** The searches started in the tick searchTick, over all units: at most Wedge.MAX_SEARCHES a tick. */
    static long searchTick=Long.MIN_VALUE;static int searchesThisTick;
    static int searchesAt(long time){return time==searchTick?searchesThisTick:0;}
    /** The nearest cell to (x, z) within eight blocks that a unit can stand in: something to stand on, nothing with a collision shape or a fluid at feet and head, no river. Null when there is none. */
    static int[] standable(GravesAndGlory.Battle battle,int x,int z){
        var w=battle.world;
        for(int r=0;r<=8;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            int tx=x+dx,tz=z+dz,ty=Arena.ground(tx,tz)+1;
            if(Arena.river(tx,tz))continue;
            var below=new BlockPos(tx,ty-1,tz);var feet=new BlockPos(tx,ty,tz);var head=new BlockPos(tx,ty+1,tz);
            if(!w.getBlockState(below).getCollisionShape(w,below).isEmpty()&&w.getFluidState(below).isEmpty()
                &&w.getBlockState(feet).getCollisionShape(w,feet).isEmpty()&&w.getFluidState(feet).isEmpty()
                &&w.getBlockState(head).getCollisionShape(w,head).isEmpty()&&w.getFluidState(head).isEmpty())return new int[]{tx,ty,tz};
        }
        return null;
    }
    /** The unit is stuck again where it sidestepped before: the point of a path to its goal, as many steps on as Wedge says, found by a pathfinder with a larger share of cells (a unit's own looks at 640 and stops short of the way out of a pocket).
     *  The path is to the goal within Wedge.REACH blocks, else to the cell that far along the line to it (the large search reaches a goal 100 blocks off, not one 200 off: it would run to its cap and find nothing). Null: no whole path to look at, so the caller sidesteps and remembers the miss.
     *  The follow range of the unit is raised for the search and put back; it is not a tracked attribute in 1.20.1 (EntityAttributes registers it without setTracked), so no packet goes to a client. */
    static Vec3d detour(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d goal,int repeats,long time){
        if(time!=searchTick){searchTick=time;searchesThisTick=0;}
        searchesThisTick++;detourSearches++;
        long t0=System.nanoTime();
        try{
            var mob=u.mob;
            double[] t=Wedge.target(mob.getX(),mob.getZ(),goal.x,goal.z);
            int gx=(int)Math.floor(t[0]),gz=(int)Math.floor(t[1]),gy=Arena.ground(gx,gz)+1;
            if(t[0]!=goal.x||t[1]!=goal.z){int[] c=standable(battle,gx,gz);if(c==null)return null;gx=c[0];gy=c[1];gz=c[2];}
            var follow=mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_FOLLOW_RANGE);double was=follow.getBaseValue();
            net.minecraft.entity.ai.pathing.Path path;
            try{follow.setBaseValue(Wedge.FOLLOW_RANGE);path=new net.minecraft.entity.ai.pathing.MobNavigation(mob,battle.world).findPathTo(gx+.5,gy,gz+.5,1);}
            finally{follow.setBaseValue(was);}
            if(path==null||!path.reachesTarget()||path.getLength()<4)return null;
            int nodes=path.getLength();double[] xs=new double[nodes],zs=new double[nodes];
            for(int i=0;i<nodes;i++){var p=path.getNode(i);xs[i]=p.x+.5;zs[i]=p.z+.5;}
            var n=path.getNode(Wedge.pick(xs,zs,nodes,mob.getX(),mob.getZ(),Wedge.steps(repeats)));
            return new Vec3d(n.x+.5,n.y,n.z+.5);
        }finally{long d=System.nanoTime()-t0;lastSearchNanos=d;detourNanos+=d;detourMaxNanos=Math.max(detourMaxNanos,d);}
    }
    /** Where a walking unit stood when it last moved more than a block, and since when: a unit that jumps and sidesteps in place (the sidestep moves it a few tenths each beat, which zeroes the still count of walk) is still pinned. */
    static final class Pin{Vec3d anchor;long since,seen;}
    static final Map<GravesAndGlory.Unit,Pin> PINS=new WeakHashMap<>();
    /** The unit has been walking for five seconds (100 ticks) and has not left a block-wide patch. A unit that stopped walking (arrived, fighting) starts again when it walks on. */
    static boolean pinned(GravesAndGlory.Unit u,long time){
        var p=PINS.computeIfAbsent(u,k->new Pin());var at=u.mob.getPos();
        if(p.anchor==null||time-p.seen>30||at.squaredDistanceTo(p.anchor)>1.0){p.anchor=at;p.since=time;}
        p.seen=time;
        if(time-p.since<100)return false;
        p.since=time;return true;
    }
    static void walk(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d destination,long time,double speed){walk(battle,u,destination,time,speed,false);}
    static void walk(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d destination,long time,double speed,boolean climb){
        MobEntity mob=u.mob;Vec3d goal=route(u,destination);Vec3d routed=goal;
        // A raised goal (a tower top): every ground waypoint below would put it at the tower's foot (Arena.ground is the terrain under the tower), so near enough it paths to the goal's own height, up the stair.
        if(climb&&goal==destination&&destination.y>Arena.ground((int)Math.floor(destination.x),(int)Math.floor(destination.z))+2.5&&GravesAndGlory.Battle.horizontalDistance(mob.getPos(),destination)<CLIMB_RANGE*CLIMB_RANGE){
            if(GravesAndGlory.Battle.horizontalDistance(mob.getPos(),destination)<N.preciseRange()*N.preciseRange()&&Math.abs(mob.getY()-destination.y)<1.1){mob.getNavigation().stop();mob.getMoveControl().moveTo(destination.x,destination.y,destination.z,speed);return;}
            if(time<u.nextPath&&u.lastGoal!=null&&u.lastGoal.squaredDistanceTo(destination)<1&&!mob.getNavigation().isIdle())return;
            if(mob.getNavigation().startMovingTo(destination.x,destination.y,destination.z,speed)){u.lastGoal=destination;u.nextPath=time+N.repathTicks();u.stuckTicks=0;climbs++;return;}
            climbMisses++; // no path up from here: the ground walk below, toward the tower's foot, until a later search finds one
        }
        // A unit on a detour (see below) walks to its point first, until it is there (Wedge.over), the time is up, or its goal has moved four blocks (a new order, an enemy that walked on).
        boolean onDetour=false;
        if(u.detour!=null){if(Wedge.over(time,u.detourUntil,mob.getX(),mob.getY(),mob.getZ(),u.detour.x,u.detour.y,u.detour.z,routed.x,routed.z,u.detourFor.x,u.detourFor.z))u.detour=null;else{goal=u.detour;onDetour=true;}}
        boolean wade=Houses.fords(u); // a fording troop's water path penalty is 0, and neither the river snap nor the waypoint search below keeps its waypoints out of the river
        mob.setPathfindingPenalty(PathNodeType.WATER,wade?0f:(float)N.waterPenalty());
        int gx=(int)Math.floor(goal.x),gz=(int)Math.floor(goal.z),gy=Arena.ground(gx,gz)+1;
        if(!onDetour&&GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal)<N.preciseRange()*N.preciseRange()&&Math.abs(mob.getY()-gy)<1.1&&!(Arena.river(gx,gz)&&Arena.ground(gx,gz)<64)&&battle.world.getBlockState(new BlockPos(gx,gy,gz)).isAir()&&battle.world.getBlockState(new BlockPos(gx,gy+1,gz)).isAir()){
            mob.getNavigation().stop();mob.getMoveControl().moveTo(goal.x,gy,goal.z,speed);return;
        }
        Vec3d delta=goal.subtract(mob.getPos()).multiply(1,0,1);
        if(!onDetour&&delta.length()>N.waypointDistance())goal=mob.getPos().add(delta.normalize().multiply(N.waypointDistance())); // a detour point is on a whole path: no straight waypoint toward it
        if(mob.squaredDistanceTo(u.lastPosition)<.03)u.stuckTicks+=10;else u.stuckTicks=0;
        u.lastPosition=mob.getPos();
        boolean pinned=pinned(u,time);
        if(u.stuckTicks>N.stuckSeconds()*20||pinned){
            if(stepHop(battle,u,goal)){u.stuckTicks=0;u.nextPath=0;}
            else if(u.stuckTicks>N.stuckSeconds()*20){
                // Stuck again where it sidestepped before (within ten seconds, six blocks): the sidestep leads to the same corner of a pocket, so go round by a point the pathfinder can reach (Wedge).
                u.wedgeRepeats=Wedge.repeats(u.wedgeAt!=null,u.wedgeTime,u.wedgeAt==null?0:u.wedgeAt.x,u.wedgeAt==null?0:u.wedgeAt.z,u.wedgeRepeats,time,mob.getX(),mob.getZ());
                u.wedgeAt=mob.getPos();u.wedgeTime=time;
                int what=DETOUR?Wedge.next(u.wedgeRepeats,time,u.detourRetryAt,searchesAt(time)):Wedge.FIRST;
                Vec3d out=null;
                if(what==Wedge.SEARCH){out=detour(battle,u,routed,u.wedgeRepeats,time);if(out==null){u.detourRetryAt=time+Wedge.RETRY_TICKS;detourMisses++;}
                    if(TRACE_HOPS)System.out.println("[Graves and Glory] detour search of slot "+u.slot+" toward ("+String.format("%.1f,%.1f",routed.x,routed.z)+") "+(out==null?"found no path":"found a path")+" in "+String.format("%.2f",lastSearchNanos/1e6)+" ms");}
                if(out!=null){
                    if(TRACE_HOPS)System.out.println("[Graves and Glory] detour of slot "+u.slot+" from ("+String.format("%.1f,%.1f,%.1f",mob.getX(),mob.getY(),mob.getZ())+") to ("+String.format("%.1f,%.1f,%.1f",out.x,out.y,out.z)+") on its way to ("+String.format("%.1f,%.1f",routed.x,routed.z)+"), stuck again "+u.wedgeRepeats+" times at this wedge");
                    u.detour=out;u.detourFor=routed;u.detourUntil=time+Wedge.ticks(Wedge.steps(u.wedgeRepeats));goal=out;onDetour=true;detours++;u.stuckTicks=0;u.nextPath=0;
                }else{
                    sidesteps++;if(TRACE_HOPS)System.out.println("[Graves and Glory] sidestep of slot "+u.slot+" at ("+String.format("%.1f,%.1f,%.1f",mob.getX(),mob.getY(),mob.getZ())+") on its way to ("+String.format("%.1f,%.1f",routed.x,routed.z)+"), stuck again "+u.wedgeRepeats+" times at this wedge ("+(!DETOUR?"detours off":what==Wedge.SEARCH?"no path to look at, not searched again for "+Wedge.RETRY_TICKS+" ticks":Wedge.WHY[what])+")");
                    double sign=(u.slot%2==0?1:-1);Vec3d sideways=new Vec3d(-delta.z,0,delta.x).normalize().multiply(3*sign);
                    goal=mob.getPos().add(delta.normalize().multiply(4)).add(sideways);u.stuckTicks=0;u.nextPath=0;
                    if(mob.isOnGround())mob.getJumpControl().setActive();
                }
            }
        }
        // A detour point is a node of a path: it is used as it is (its own height; no move off the river bank, no search for another column, which could land behind the wall it leads round).
        int x=(int)Math.floor(goal.x),z=(int)Math.floor(goal.z),y=onDetour?(int)Math.floor(goal.y):Arena.ground(x,z)+1;
        if(!onDetour&&!wade&&Arena.river(x,z)&&Arena.ground(x,z)<64){x=(int)Arena.riverX(z)+(x>Arena.riverX(z)?11:-11);y=Arena.ground(x,z)+1;}
        // Keep local waypoints out of tree trunks, flagpoles and walls.
        if(!onDetour&&!(wade&&Arena.river(x,z))&&(!battle.world.getBlockState(new BlockPos(x,y,z)).isAir()||!battle.world.getBlockState(new BlockPos(x,y+1,z)).isAir())){
            boolean found=false;
            for(int r=1;r<=5&&!found;r++)for(int dx=-r;dx<=r&&!found;dx++)for(int dz=-r;dz<=r;dz++){
                int tx=x+dx,tz=z+dz,ty=Arena.ground(tx,tz)+1;
                if(!Arena.river(tx,tz)&&battle.world.getBlockState(new BlockPos(tx,ty,tz)).isAir()&&battle.world.getBlockState(new BlockPos(tx,ty+1,tz)).isAir()){x=tx;y=ty;z=tz;found=true;break;}
            }
        }
        Vec3d next=new Vec3d(x+.5,y,z+.5);
        if(time>=u.nextPath||u.lastGoal==null||u.lastGoal.squaredDistanceTo(next)>9||mob.getNavigation().isIdle()){
            mob.getNavigation().startMovingTo(next.x,next.y,next.z,speed);u.lastGoal=next;u.nextPath=time+N.repathTicks();
        }
    }
}
