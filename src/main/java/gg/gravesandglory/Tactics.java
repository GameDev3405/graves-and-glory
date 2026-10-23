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
                if(ranged&&close!=null&&closest<flee*flee&&!hold)backOff(battle,u,close,time);
                else mob.getNavigation().stop();
                if(time>=u.nextAttack&&time>=u.stunnedUntil&&!Abilities.tooClose(u,nearest)){
                    int cooldown=u.spec.cooldown();u.nextAttack=u.nextAttack<time-10?time+cooldown:u.nextAttack+cooldown;mob.swingHand(Hand.MAIN_HAND);
                    BattleEffects.strike(battle,u,enemy);
                    Abilities.attack(battle,u,enemy,time);
                    if(ranged){
                        battle.world.playSound(null,mob.getBlockPos(),u.spec.magic()?SoundEvents.ENTITY_EVOKER_CAST_SPELL:SoundEvents.ENTITY_ARROW_SHOOT,SoundCategory.HOSTILE,.25f,1.1f);
                    }
                }
            }else if(!holdSiege&&(!hold||GravesAndGlory.Battle.horizontalDistance(enemy.getPos(),a.target)<engage*engage)){
                Vec3d goal=enemy.getPos();
                if(ranged&&!visible){Vec3d toward=enemy.getPos().subtract(mob.getPos()).multiply(1,0,1).normalize();double sign=u.slot%2==0?1:-1;goal=mob.getPos().add(-toward.z*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep(),0,toward.x*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep());}
                if(!ranged){double angle=u.slot*2.399963;goal=goal.add(Math.cos(angle)*1.6,0,Math.sin(angle)*1.6);}
                if(ranged&&visible&&nearest>reach*reach)goal=goal.add(mob.getPos().subtract(goal).multiply(1,0,1).normalize().multiply(reach*N.rangedStandoff()));
                walk(battle,u,goal,time,order.moveMultiplier());
            }else mob.getNavigation().stop();
        }else if(close!=null&&ranged){ // everything in its leash stands inside its minimum range: nothing to shoot; it backs off (unless it holds) and does not walk off to its formation
            if(!hold&&closest<flee*flee)backOff(battle,u,close,time);else mob.getNavigation().stop();
        }else{
            Vec3d goal=battle.formation(u);
            if(GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal)>N.arrivalTolerance()*N.arrivalTolerance())walk(battle,u,goal,time,order.moveMultiplier());
            else{mob.getNavigation().stop();mob.setYaw(a.yaw);mob.setHeadYaw(a.yaw);mob.setBodyYaw(a.yaw);}
        }
    }
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
        return goal;
    }
    static void walk(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d destination,long time,double speed){
        MobEntity mob=u.mob;Vec3d goal=route(u,destination);
        boolean wade=Houses.fords(u); // a fording troop's water path penalty is 0, and neither the river snap nor the waypoint search below keeps its waypoints out of the river
        mob.setPathfindingPenalty(PathNodeType.WATER,wade?0f:(float)N.waterPenalty());
        int gx=(int)Math.floor(goal.x),gz=(int)Math.floor(goal.z),gy=Arena.ground(gx,gz)+1;
        if(GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal)<N.preciseRange()*N.preciseRange()&&Math.abs(mob.getY()-gy)<1.1&&!(Arena.river(gx,gz)&&Arena.ground(gx,gz)<64)&&battle.world.getBlockState(new BlockPos(gx,gy,gz)).isAir()&&battle.world.getBlockState(new BlockPos(gx,gy+1,gz)).isAir()){
            mob.getNavigation().stop();mob.getMoveControl().moveTo(goal.x,gy,goal.z,speed);return;
        }
        Vec3d delta=goal.subtract(mob.getPos()).multiply(1,0,1);
        if(delta.length()>N.waypointDistance())goal=mob.getPos().add(delta.normalize().multiply(N.waypointDistance()));
        if(mob.squaredDistanceTo(u.lastPosition)<.03)u.stuckTicks+=10;else u.stuckTicks=0;
        u.lastPosition=mob.getPos();
        if(u.stuckTicks>N.stuckSeconds()*20){
            double sign=(u.slot%2==0?1:-1);Vec3d sideways=new Vec3d(-delta.z,0,delta.x).normalize().multiply(3*sign);
            goal=mob.getPos().add(delta.normalize().multiply(4)).add(sideways);u.stuckTicks=0;u.nextPath=0;
            if(mob.isOnGround())mob.getJumpControl().setActive();
        }
        int x=(int)Math.floor(goal.x),z=(int)Math.floor(goal.z),y=Arena.ground(x,z)+1;
        if(!wade&&Arena.river(x,z)&&Arena.ground(x,z)<64){x=(int)Arena.riverX(z)+(x>Arena.riverX(z)?11:-11);y=Arena.ground(x,z)+1;}
        // Keep local waypoints out of tree trunks, flagpoles and walls.
        if(!(wade&&Arena.river(x,z))&&(!battle.world.getBlockState(new BlockPos(x,y,z)).isAir()||!battle.world.getBlockState(new BlockPos(x,y+1,z)).isAir())){
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
