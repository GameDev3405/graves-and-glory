package gg.gravesandglory;

import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import java.util.*;

/** Server-authoritative targeting, separation, bridge routing and local path recovery. */
final class Tactics {
    static final Spec.NavigationRow N=Spec.NAVIGATION[0];
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
        var owner=battle.server.getPlayerManager().getPlayer(s.owner);
        if(a.order.equals("follow")&&owner!=null&&!battle.respawns.containsKey(s.owner)){a.target=owner.getPos();a.yaw=owner.getYaw();}
        var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(a.order)).findFirst().orElse(Spec.ORDERS[0]);
        double engage=engageRadius(battle,u,order.engageRadius()); // the order's radius, widened to the weapon's reach for a siege troop
        LivingEntity enemy=null;double nearest=engage*engage;
        if(order.engage()&&u.lockedTarget!=null&&u.lockedTarget.isAlive()&&time<u.lockUntil&&mob.squaredDistanceTo(u.lockedTarget)<nearest*1.4&&(!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(u.lockedTarget.getPos(),a.target)<nearest))enemy=u.lockedTarget;
        if(order.engage()&&a.enemy>=0&&battle.world.getEntityById(a.enemy) instanceof LivingEntity explicit&&explicit.isAlive()&&mob.canSee(explicit)){enemy=explicit;a.target=explicit.getPos();}
        if(enemy!=null)nearest=mob.squaredDistanceTo(enemy);
        boolean locked=enemy!=null;
        for(var other:battle.units)if(order.engage()&&!locked&&other.squad.team!=s.team&&other.mob.isAlive()){
            double d=mob.squaredDistanceTo(other.mob);
            boolean inLeash=!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(other.mob.getPos(),a.target)<engage*engage;
            if(d<nearest&&inLeash){enemy=other.mob;nearest=d;}
        }
        if(order.engage()&&!locked&&enemy==null)for(var st:battle.structures){ // a supply banner is a target when no troop or commander is: the army first, then its banner
            if(st.team()==s.team||!st.isAlive())continue;
            double d=mob.squaredDistanceTo(st);
            boolean inLeash=!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(st.getPos(),a.target)<engage*engage;
            if(d<nearest&&inLeash){enemy=st;nearest=d;}
        }
        for(ServerPlayerEntity p:battle.server.getPlayerManager().getPlayerList()){
            var other=battle.squads.get(p.getUuid());if(order.engage()&&!locked&&other!=null&&other.team!=s.team&&!p.isSpectator()&&p.isAlive()&&(!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(p.getPos(),a.target)<engage*engage)){
                double d=mob.squaredDistanceTo(p);if(d<nearest){enemy=p;nearest=d;}
            }
        }
        if(enemy!=u.lockedTarget){u.lockedTarget=enemy;u.lockUntil=time+Spec.COMBATDISCIPLINE[0].targetMemoryTicks();}
        mob.setTarget(enemy);mob.setAttacking(enemy!=null);
        double reach=Abilities.range(battle,u);boolean ranged=u.spec.ranged();
        boolean shield=u.tags.contains("shield")&&BattleMath.raiseShield(a.formation,a.order,nearest,reach*reach,enemy!=null); // block: up on the approach, down to swing once the enemy is in reach; shieldwall: the full brace
        if(shield&&!mob.isUsingItem())mob.setCurrentHand(Hand.OFF_HAND);else if(!shield&&mob.isUsingItem())mob.stopUsingItem();
        Abilities.tick(battle,u,enemy,time); // mend heals and a caster's spell bursts on the beat
        if(enemy!=null){
            mob.getLookControl().lookAt(enemy,40,40);
            boolean visible=mob.canSee(enemy);
            double flee=Math.max(N.rangedRetreatDistance(),Abilities.minRange(u)); // a ranged troop backs off from an enemy this near; one with a minimum range (the Mortar) from anything inside it
            if(nearest<=reach*reach&&visible){
                if(ranged&&nearest<flee*flee&&!a.order.equals("hold")){
                    Vec3d away=mob.getPos().subtract(enemy.getPos()).multiply(1,0,1).normalize();
                    walk(battle,u,mob.getPos().add(away.multiply(5)),time,1.05);
                }else mob.getNavigation().stop();
                if(time>=u.nextAttack&&time>=u.stunnedUntil&&!Abilities.tooClose(u,nearest)){
                    int cooldown=u.spec.cooldown();u.nextAttack=u.nextAttack<time-10?time+cooldown:u.nextAttack+cooldown;mob.swingHand(Hand.MAIN_HAND);
                    BattleEffects.strike(battle,u,enemy);
                    Abilities.attack(battle,u,enemy,time);
                    if(ranged){
                        battle.world.playSound(null,mob.getBlockPos(),u.spec.magic()?SoundEvents.ENTITY_EVOKER_CAST_SPELL:SoundEvents.ENTITY_ARROW_SHOOT,SoundCategory.HOSTILE,.25f,1.1f);
                    }
                }
            }else if(!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(enemy.getPos(),a.target)<engage*engage){
                Vec3d goal=enemy.getPos();
                if(ranged&&!visible){Vec3d toward=enemy.getPos().subtract(mob.getPos()).multiply(1,0,1).normalize();double sign=u.slot%2==0?1:-1;goal=mob.getPos().add(-toward.z*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep(),0,toward.x*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep());}
                if(!ranged){double angle=u.slot*2.399963;goal=goal.add(Math.cos(angle)*1.6,0,Math.sin(angle)*1.6);}
                if(ranged&&visible&&nearest>reach*reach)goal=goal.add(mob.getPos().subtract(goal).multiply(1,0,1).normalize().multiply(reach*N.rangedStandoff()));
                walk(battle,u,goal,time,order.moveMultiplier());
            }else mob.getNavigation().stop();
        }else{
            Vec3d goal=battle.formation(u);
            if(GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal)>N.arrivalTolerance()*N.arrivalTolerance())walk(battle,u,goal,time,order.moveMultiplier());
            else{mob.getNavigation().stop();mob.setYaw(a.yaw);mob.setHeadYaw(a.yaw);mob.setBodyYaw(a.yaw);}
        }
    }
    static Vec3d route(GravesAndGlory.Unit u,Vec3d goal){
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
        if(Arena.river(x,z)&&Arena.ground(x,z)<64){x=(int)Arena.riverX(z)+(x>Arena.riverX(z)?11:-11);y=Arena.ground(x,z)+1;}
        // Keep local waypoints out of tree trunks, flagpoles and walls.
        if(!battle.world.getBlockState(new BlockPos(x,y,z)).isAir()||!battle.world.getBlockState(new BlockPos(x,y+1,z)).isAir()){
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
