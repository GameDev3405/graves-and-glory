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
        var t=u.lockedTarget;if(!u.spec.ability().equals("shield")||t==null||!t.isAlive())return;
        var a=battle.assignment(u);boolean shield=BattleMath.raiseShield(a.formation,a.order,u.mob.squaredDistanceTo(t),u.spec.range()*u.spec.range(),true);
        if(shield&&!u.mob.isUsingItem())u.mob.setCurrentHand(Hand.OFF_HAND);else if(!shield&&u.mob.isUsingItem())u.mob.stopUsingItem();
    }
    static void tick(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,long time){
        if(time%10!=u.slot%10){brace(battle,u);return;} // the shield is decided every tick; everything else on the unit's 10-tick beat
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
        boolean shield=u.spec.ability().equals("shield")&&BattleMath.raiseShield(a.formation,a.order,nearest,u.spec.range()*u.spec.range(),enemy!=null); // block: up on the approach, down to swing once the enemy is in reach; shieldwall: the full brace
        if(shield&&!mob.isUsingItem())mob.setCurrentHand(Hand.OFF_HAND);else if(!shield&&mob.isUsingItem())mob.stopUsingItem();
        if(u.spec.ability().equals("mend")&&time>=u.abilityAt){
            var ally=battle.units.stream().filter(v->v.squad.team==s.team&&v.mob.isAlive()&&v.mob.getHealth()<v.mob.getMaxHealth()&&v.mob.squaredDistanceTo(mob)<Spec.COMBATDISCIPLINE[0].healRadius()*Spec.COMBATDISCIPLINE[0].healRadius()&&mob.canSee(v.mob)).min(java.util.Comparator.comparingDouble(v->v.mob.getHealth()/v.mob.getMaxHealth())).orElse(null);
            if(ally!=null){Projectiles.fire(battle,u,ally.mob,"mend",(float)(Spec.COMBATDISCIPLINE[0].healAmount()*battle.campaign.damage(u)));u.abilityAt=time+Spec.COMBATDISCIPLINE[0].abilityCooldownTicks();}
        }
        if(u.spellCenter!=null){
            double radius=Spec.COMBATDISCIPLINE[0].spellRadius();for(int i=0;i<12;i++){double angle=i*Math.PI/6;battle.world.spawnParticles(ParticleTypes.WITCH,u.spellCenter.x+Math.cos(angle)*radius,u.spellCenter.y+.2,u.spellCenter.z+Math.sin(angle)*radius,1,0,0,0,0);}
            if(time>=u.windupUntil){for(var v:battle.units)if(v.squad.team!=s.team&&v.mob.isAlive()&&v.mob.squaredDistanceTo(u.spellCenter)<radius*radius){v.mob.damage(battle.world.getDamageSources().indirectMagic(mob,mob),(float)(u.spec.damage()*battle.campaign.damage(u)));v.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.SLOWNESS,40,0));}u.spellCenter=null;} // blight ignores shields: indirect magic is in bypasses_shield (and bypasses_armor), and keeps the Gravecaller as the attacker for kill credit
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
    static int detours,sidesteps,detourSearches,detourMisses;static long detourNanos,detourMaxNanos,lastSearchNanos;static final boolean DETOUR=System.getenv("GRAVES_NO_DETOUR")==null;
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
    static void walk(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d destination,long time,double speed){
        MobEntity mob=u.mob;Vec3d goal=route(u,destination);Vec3d routed=goal;
        // A unit on a detour (see below) walks to its point first, until it is there (Wedge.over), the time is up, or its goal has moved four blocks (a new order, an enemy that walked on).
        boolean onDetour=false;
        if(u.detour!=null){if(Wedge.over(time,u.detourUntil,mob.getX(),mob.getY(),mob.getZ(),u.detour.x,u.detour.y,u.detour.z,routed.x,routed.z,u.detourFor.x,u.detourFor.z))u.detour=null;else{goal=u.detour;onDetour=true;}}
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
        if(!onDetour&&Arena.river(x,z)&&Arena.ground(x,z)<64){x=(int)Arena.riverX(z)+(x>Arena.riverX(z)?11:-11);y=Arena.ground(x,z)+1;}
        // Keep local waypoints out of tree trunks, flagpoles and walls.
        if(!onDetour&&(!battle.world.getBlockState(new BlockPos(x,y,z)).isAir()||!battle.world.getBlockState(new BlockPos(x,y+1,z)).isAir())){
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
