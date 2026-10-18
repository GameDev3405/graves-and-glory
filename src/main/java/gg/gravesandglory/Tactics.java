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
    static void tick(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,long time){
        if(time%10!=u.slot%10)return;
        var mob=u.mob;var s=u.squad;var a=battle.assignment(u);
        var owner=battle.server.getPlayerManager().getPlayer(s.owner);
        if(a.order.equals("follow")&&owner!=null&&!battle.respawns.containsKey(s.owner)){a.target=owner.getPos();a.yaw=owner.getYaw();}
        var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(a.order)).findFirst().orElse(Spec.ORDERS[0]);
        LivingEntity enemy=null;double nearest=order.engageRadius()*order.engageRadius();
        if(order.engage()&&u.lockedTarget!=null&&u.lockedTarget.isAlive()&&time<u.lockUntil&&mob.squaredDistanceTo(u.lockedTarget)<nearest*1.4&&(!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(u.lockedTarget.getPos(),a.target)<nearest))enemy=u.lockedTarget;
        if(order.engage()&&a.enemy>=0&&battle.world.getEntityById(a.enemy) instanceof LivingEntity explicit&&explicit.isAlive()&&mob.canSee(explicit)){enemy=explicit;a.target=explicit.getPos();}
        if(enemy!=null)nearest=mob.squaredDistanceTo(enemy);
        boolean locked=enemy!=null;
        for(var other:battle.units)if(order.engage()&&!locked&&other.squad.team!=s.team&&other.mob.isAlive()){
            double d=mob.squaredDistanceTo(other.mob);
            boolean inLeash=!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(other.mob.getPos(),a.target)<order.engageRadius()*order.engageRadius();
            if(d<nearest&&inLeash){enemy=other.mob;nearest=d;}
        }
        for(ServerPlayerEntity p:battle.server.getPlayerManager().getPlayerList()){
            var other=battle.squads.get(p.getUuid());if(order.engage()&&!locked&&other!=null&&other.team!=s.team&&!p.isSpectator()&&p.isAlive()&&(!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(p.getPos(),a.target)<order.engageRadius()*order.engageRadius())){
                double d=mob.squaredDistanceTo(p);if(d<nearest){enemy=p;nearest=d;}
            }
        }
        if(enemy!=u.lockedTarget){u.lockedTarget=enemy;u.lockUntil=time+Spec.COMBATDISCIPLINE[0].targetMemoryTicks();}
        mob.setTarget(enemy);mob.setAttacking(enemy!=null);
        boolean shield=u.spec.ability().equals("shield")&&(a.formation.equals("block")||a.formation.equals("shieldwall"))&&!a.order.equals("charge")&&!a.order.equals("retreat")&&enemy!=null&&(nearest<36||a.formation.equals("shieldwall"));
        if(shield&&!mob.isUsingItem())mob.setCurrentHand(Hand.OFF_HAND);else if(!shield&&mob.isUsingItem())mob.stopUsingItem();
        if(u.spec.ability().equals("mend")&&time>=u.abilityAt){
            var ally=battle.units.stream().filter(v->v.squad.team==s.team&&v.mob.isAlive()&&v.mob.getHealth()<v.mob.getMaxHealth()&&v.mob.squaredDistanceTo(mob)<Spec.COMBATDISCIPLINE[0].healRadius()*Spec.COMBATDISCIPLINE[0].healRadius()&&mob.canSee(v.mob)).min(java.util.Comparator.comparingDouble(v->v.mob.getHealth()/v.mob.getMaxHealth())).orElse(null);
            if(ally!=null){Projectiles.fire(battle,u,ally.mob,"mend",(float)(Spec.COMBATDISCIPLINE[0].healAmount()*battle.campaign.damage(u)));u.abilityAt=time+Spec.COMBATDISCIPLINE[0].abilityCooldownTicks();}
        }
        if(u.spellCenter!=null){
            double radius=Spec.COMBATDISCIPLINE[0].spellRadius();for(int i=0;i<12;i++){double angle=i*Math.PI/6;battle.world.spawnParticles(ParticleTypes.WITCH,u.spellCenter.x+Math.cos(angle)*radius,u.spellCenter.y+.2,u.spellCenter.z+Math.sin(angle)*radius,1,0,0,0,0);}
            if(time>=u.windupUntil){for(var v:battle.units)if(v.squad.team!=s.team&&v.mob.isAlive()&&v.mob.squaredDistanceTo(u.spellCenter)<radius*radius){v.mob.damage(battle.world.getDamageSources().mobAttack(mob),(float)(u.spec.damage()*battle.campaign.damage(u)));v.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,40,0));}u.spellCenter=null;}
        }
        if(enemy!=null){
            mob.getLookControl().lookAt(enemy,40,40);
            boolean visible=mob.canSee(enemy);
            if(nearest<=u.spec.range()*u.spec.range()&&visible){
                if(u.spec.range()>3&&nearest<N.rangedRetreatDistance()*N.rangedRetreatDistance()&&!a.order.equals("hold")){
                    Vec3d away=mob.getPos().subtract(enemy.getPos()).multiply(1,0,1).normalize();
                    walk(battle,u,mob.getPos().add(away.multiply(5)),time,1.05);
                }else mob.getNavigation().stop();
                if(time>=u.nextAttack){
                    int cooldown=s.commander.equals("marksman")&&u.spec.group().equals("ranged")?Math.round(u.spec.cooldown()/1.1f):u.spec.cooldown();u.nextAttack=u.nextAttack<time-10?time+cooldown:u.nextAttack+cooldown;mob.swingHand(Hand.MAIN_HAND);
                    BattleEffects.strike(battle,u,enemy);
                    if(u.spec.ability().equals("blight")){if(time>=u.abilityAt){u.spellCenter=enemy.getPos();u.windupUntil=time+Spec.COMBATDISCIPLINE[0].spellWindupTicks();u.abilityAt=time+Spec.COMBATDISCIPLINE[0].abilityCooldownTicks();Projectiles.bolt(battle,u,u.spellCenter);}}
                    else if(!u.spec.ability().equals("mend")){
                        float amount=(float)(u.spec.damage()*battle.campaign.damage(u));
                        // The hit is decided now; a ranged hit lands when its visible arrow arrives.
                        if(u.spec.range()>3)Projectiles.fire(battle,u,enemy,"arrow",amount);else enemy.damage(battle.world.getDamageSources().mobAttack(mob),amount);
                    }
                    if(u.spec.range()>3){
                        battle.world.playSound(null,mob.getBlockPos(),u.spec.magic()?SoundEvents.ENTITY_EVOKER_CAST_SPELL:SoundEvents.ENTITY_ARROW_SHOOT,SoundCategory.HOSTILE,.25f,1.1f);
                    }
                }
            }else if(!a.order.equals("hold")||GravesAndGlory.Battle.horizontalDistance(enemy.getPos(),a.target)<order.engageRadius()*order.engageRadius()){
                Vec3d goal=enemy.getPos();
                if(u.spec.range()>3&&!visible){Vec3d toward=enemy.getPos().subtract(mob.getPos()).multiply(1,0,1).normalize();double sign=u.slot%2==0?1:-1;goal=mob.getPos().add(-toward.z*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep(),0,toward.x*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep());}
                if(u.spec.range()<=3){double angle=u.slot*2.399963;goal=goal.add(Math.cos(angle)*1.6,0,Math.sin(angle)*1.6);}
                if(u.spec.range()>3&&visible&&nearest>u.spec.range()*u.spec.range())goal=goal.add(mob.getPos().subtract(goal).multiply(1,0,1).normalize().multiply(u.spec.range()*N.rangedStandoff()));
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
                double offset=(u.slot%5-2)*1.3;
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
