package gg.gravesandglory;

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
    /** The stances a troop shows its clients (stance_v2 sends the index): BattleMath.STANCES. */
    static final List<String> STANCES=BattleMath.STANCES;
    /** stance_v2 packets sent: on a change (Tactics.show), to a player who starts tracking a troop, and to a player whose client registers the channel late (the harness reads them; MotionEvents keeps its own). */
    static volatile int stanceSends,trackingSends,registerSends;
    /** The stance a unit's order gives it (BattleMath.stance with the plain-hold rule, unformed, not aiming, shield down): its stance at spawn, and the base Tactics.show refines. An arrived Retreat or Move group (arrived) stands as on Hold. */
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
        if(order.engage()||a.thenFollow)return false; // a player's Retreat never holds where it stops: it turns to Follow me at its commander (tick)
        String group=CustomGroups.stateKey(u);if(u.squad.arrived.contains(group))return true; // a custom group's own latch when it splits a spec group (Task 12 fix I1)
        var members=sharers(b,u);if(members.isEmpty())return false;
        double r=Spec.COMBATDISCIPLINE[0].arriveRadius();
        if(GravesAndGlory.Battle.horizontalDistance(Morale.centre(members),slotCentre(b,members))>r*r)return false;
        u.squad.arrived.add(group);return true;
    }
    /** The centre of the members' formation slots (Battle.formation): where their walk ends. */
    static Vec3d slotCentre(GravesAndGlory.Battle b,List<GravesAndGlory.Unit> members){Vec3d sum=Vec3d.ZERO;for(var m:members)sum=sum.add(b.formation(m));return sum.multiply(1.0/members.size());}
    /** Whether the unit's group centre stands within r of the centre of its formation slots (as arrived reads it, without the latch). */
    static boolean groupWithin(GravesAndGlory.Battle b,GravesAndGlory.Unit u,double r){
        var members=sharers(b,u);if(members.isEmpty())return false;
        return GravesAndGlory.Battle.horizontalDistance(Morale.centre(members),slotCentre(b,members))<=r*r;
    }
    /** The living troops that count toward the unit's group centre (not temporary, as Morale.members) by its position key (CustomGroups.stateKey):
     *  its spec group's troops less any under a custom group's order, or the troops that share that custom order (Task 12). */
    static List<GravesAndGlory.Unit> sharers(GravesAndGlory.Battle b,GravesAndGlory.Unit u){
        String key=CustomGroups.stateKey(u);List<GravesAndGlory.Unit> out=new java.util.ArrayList<>();
        for(var m:b.units)if(m.squad==u.squad&&!m.temporary&&m.mob.isAlive()&&CustomGroups.stateKey(m).equals(key))out.add(m);
        return out;
    }
    /** The unit's group turns to another order in place (command UI Task 5): a player's Charge to Attack (hunt) on its first landed blow or at its
     *  end point, a player's Retreat to Follow me at its commander. Same point, formation and facing; chargedAt is kept for the charge impact.
     *  Every key that holds the old assignment takes the new one, so a custom group's order (troop:&lt;id&gt; keys, Task 12) turns for its troops alone. */
    static GravesAndGlory.Assignment become(GravesAndGlory.Unit u,GravesAndGlory.Assignment a,String order,long time){
        var n=new GravesAndGlory.Assignment(order,a.formation,a.target,a.yaw);n.issued=time;n.perch=a.perch;n.hunt=order.equals("attack");
        boolean shared=false;for(var e:u.squad.assignments.entrySet())if(e.getValue()==a){e.setValue(n);shared=true;}
        if(!shared)u.squad.assignments.put(u.spec.group(),n);u.squad.arrived.remove(CustomGroups.stateKey(u));return n;
    }
    /** Whether a point is on a player's Charge path from this troop: ahead along Assignment.chargeDir (up to chargeRunBlocks) and within
     *  chargeLane blocks of the troop's line. Every other order has no path, so anything passes. */
    static final double CHARGE_LANE=3;
    static boolean ahead(GravesAndGlory.Unit u,GravesAndGlory.Assignment a,Vec3d p){
        if(a.chargeDir==null)return true;
        double dx=p.x-u.mob.getX(),dz=p.z-u.mob.getZ(),along=dx*a.chargeDir.x+dz*a.chargeDir.z,across=Math.abs(dx*a.chargeDir.z-dz*a.chargeDir.x);
        return along>=-1&&along<=Spec.COMBATDISCIPLINE[0].chargeRunBlocks()&&across<=CHARGE_LANE;
    }
    static String stance(GravesAndGlory.Assignment a,GravesAndGlory.Unit u){
        var tags=ArmyMobs.tags(u.spec);return BattleMath.stance(a.order,tags.contains("shield"),tags.contains("spear"),false,false,false,false);
    }
    /**
     * The stance a troop shows on its beat (stance_v2, BattleMath.stance): by the order it fights under (a, held when it has arrived), formed when it is a wall
     * troop (Battle.walls) within the arrival tolerance of its slot, aiming when it is ranged with an enemy in sight and in reach (its aimPitch then
     * Projectiles.aimPitch at that enemy, positive down, else 0), and a shield troop's shield up. Sent to every player tracking it when the stance changes, or
     * its aim moves more than AIM_STEP degrees.
     */
    static void show(GravesAndGlory.Battle b,GravesAndGlory.Unit u,GravesAndGlory.Assignment a,boolean aiming,float pitch){
        boolean shield=u.tags.contains("shield"),spear=u.tags.contains("spear");
        boolean formed=GravesAndGlory.Battle.walls(u,a.order)&&GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(),b.formation(u))<=N.arrivalTolerance()*N.arrivalTolerance();
        String stance=BattleMath.stance(a.order,shield,spear,u.spec.ranged(),formed,aiming,shield&&u.mob.isUsingItem());
        u.orderStance=stance;u.orderPitch=stance.equals("ranged_aim")?pitch:0;
        String shown=BattleMath.shownStance(u.orderStance,b.time,u.stunnedUntil);
        set(u,shown,shown.equals("stunned")?0:u.orderPitch);
    }
    /**
     * A stunned troop (Unit.stunnedUntil: a Shield bash, Rise Again, a bear's roar) shows "stunned" over its order's stance, sent the tick the stun begins (this runs
     * every tick, not only on the beat) and replaced by the order's stance (the last one a beat chose, orderStance) the tick it ends. A commander is never a Unit,
     * so Statuses never stuns him and this never runs for him.
     */
    static void stunStance(GravesAndGlory.Unit u,long time){
        String shown=BattleMath.shownStance(u.orderStance,time,u.stunnedUntil);
        if(shown.equals("stunned"))set(u,shown,0);
        else if(u.stance.equals("stunned"))set(u,shown,u.orderPitch);
    }
    /** How far (degrees) an aiming troop's aim moves before its clients hear of it again. */
    static final float AIM_STEP=.25f;
    static void set(GravesAndGlory.Unit u,String stance,float aimPitch){
        if(stance.equals(u.stance)&&Math.abs(aimPitch-u.aimPitch)<=AIM_STEP)return;
        u.stance=stance;u.aimPitch=aimPitch;stanceSends+=MotionEvents.sendStance(u,stance,aimPitch);
    }
    /** Between beats a shield troop on an order with no stance of its own shows its shield as brace raises or lowers it: shield_move while it is up. */
    static void shieldStance(GravesAndGlory.Unit u){
        if(!u.tags.contains("shield")||u.spec.ranged()||!(u.stance.isEmpty()||u.stance.equals("shield_move")))return;
        set(u,u.mob.isUsingItem()?"shield_move":"",0);
    }
    /** The crawl's movement-speed modifier (MULTIPLY_TOTAL, kept through Abilities.keepSpeed like the Howl's): it multiplies with the Howl, Muster and the order's moveMultiplier. */
    static final UUID CRAWL_ID=UUID.fromString("9c3e7a51-4f2b-4d8e-b6a0-2e5d1c7f3a94");
    static final Spec.CombatDisciplineRow C=Spec.COMBATDISCIPLINE[0];
    /** Archer fire (the rework's section 5): a bow, crossbow or thrower troop. A caster's spell or the Mortar's shell is no arrow to raise a shield against. */
    static boolean archer(GravesAndGlory.Unit u){var f=u.spec.family();return f.equals("bow")||f.equals("crossbow")||f.equals("thrower");}
    /** When an archer shoots at an enemy troop (Projectiles.fire) or takes aim at one from within its reach (tick), marks that troop's squad group as
     *  shot at now (Squad.shotAt). Any other shooter, or a target that is no enemy troop (a commander, a banner, an ally being mended), marks nothing. */
    static void stamp(GravesAndGlory.Battle b,GravesAndGlory.Unit shooter,Entity target){
        if(!archer(shooter))return;var victim=Combat.unitOf(b,target);
        if(victim!=null&&victim.squad.team!=shooter.squad.team)victim.squad.shotAt.put(victim.spec.group(),b.time);
    }
    /** Whether the unit's squad group was shot at within fireMemoryTicks (BattleMath.underFire). */
    static boolean underFire(GravesAndGlory.Battle b,GravesAndGlory.Unit u){return BattleMath.underFire(b.time,u.squad.shotAt.getOrDefault(u.spec.group(),0L),C.fireMemoryTicks());}
    /** Whether a player's Formation (Assignment.holdSlots) group is in place: its centre came within arriveRadius of its slots once; latched on
     *  Squad.arrived like an arrival, cleared by the next order. */
    static boolean placed(GravesAndGlory.Battle b,GravesAndGlory.Unit u){
        String group=CustomGroups.stateKey(u);if(u.squad.arrived.contains(group))return true;
        if(!groupWithin(b,u,Spec.COMBATDISCIPLINE[0].arriveRadius()))return false;
        u.squad.arrived.add(group);return true;
    }
    /** The leash a target must be inside. In place on a player's Formation (slotted): a melee troop takes only what comes within its reach of
     *  its own slot, a ranged troop anything (its weapon's reach bounds it). Otherwise Hold's: within the engage radius of the anchor. */
    static boolean leash(GravesAndGlory.Unit u,GravesAndGlory.Assignment a,boolean hold,boolean holdSiege,boolean slotted,Vec3d slot,double keep,double engage,Vec3d p){
        if(slotted)return u.spec.ranged()||GravesAndGlory.Battle.horizontalDistance(p,slot)<=keep*keep;
        return !hold||holdSiege||GravesAndGlory.Battle.horizontalDistance(p,a.target)<engage*engage;
    }
    /** The order a shield troop raises its shield by (BattleMath.shieldOrder): an arrived Move group stands as on Hold (the latch Squad.arrived, which arrived sets on the beat); a player's Retreat (thenFollow) raises as Follow me does, under fire and without Hold's wall (Ruling 2), and still strikes nothing. */
    static String shieldOrder(GravesAndGlory.Unit u,GravesAndGlory.Assignment a){
        if(a.thenFollow&&a.order.equals("retreat"))return "follow"; // a player's Retreat raises shields under fire as on the march (Ruling 2), and still strikes nothing
        return BattleMath.shieldOrder(a.order,u.squad.arrived.contains(CustomGroups.stateKey(u)));
    }
    /** Squared distance to the nearest living enemy troop or commander inside scouting range that the troop can see (one line-of-sight test, on the
     *  nearest), or -1 for none: on Hold a shield wall raises its shields once an enemy is in sight, not only once one walks into its leash. */
    static double sighted(GravesAndGlory.Battle b,GravesAndGlory.Unit u){
        double range=Spec.SCOUTING[0].range(),best=range*range;LivingEntity seen=null;
        for(var other:b.units)if(other.squad.team!=u.squad.team&&other.mob.isAlive()){double d=u.mob.squaredDistanceTo(other.mob);if(d<best){best=d;seen=other.mob;}}
        for(ServerPlayerEntity p:b.server.getPlayerManager().getPlayerList()){var other=b.squads.get(p.getUuid());if(other!=null&&other.team!=u.squad.team&&!p.isSpectator()&&p.isAlive()){double d=u.mob.squaredDistanceTo(p);if(d<best){best=d;seen=p;}}}
        return seen!=null&&u.mob.canSee(seen)?best:-1;
    }
    /** Raises or lowers a shield troop's shield (the off hand: Combat counts it raised while the item is in use). */
    static void shield(MobEntity mob,boolean up){if(up&&!mob.isUsingItem())mob.setCurrentHand(Hand.OFF_HAND);else if(!up&&mob.isUsingItem())mob.stopUsingItem();}
    /** The crawl (section 5): a shield troop with its shield up under archer fire on any order but Hold moves at crawlShare of its pace. Kept every tick
     *  (brace), so it comes off on the tick the shield drops, the fire memory runs out or the order turns to Hold; never stacked (Abilities.keepSpeed).
     *  Never on Attack or Charge (crawls: Kort 2026-11-11, an attack closes at its pace; the crawl is a march or a Formation pushing forward). */
    static void crawl(GravesAndGlory.Battle b,GravesAndGlory.Unit u,String order){
        boolean on=u.tags.contains("shield")&&u.mob.isUsingItem()&&crawls(order)&&!b.assignment(u).thenFollow&&underFire(b,u); // a player's Retreat runs at its pace with shields up (fix round 1 ruling)
        Abilities.keepSpeed(u.mob,CRAWL_ID,"Graves and Glory crawl",on?BattleMath.speedModifier(C.crawlShare(),u.mob.isTouchingWater()):0);
    }
    /** Whether an order (shieldOrder's) slows a shield troop under fire: not Hold (it stands), and not Attack or Charge (they close at their pace). */
    static boolean crawls(String order){return !order.equals("hold")&&!order.equals("attack")&&!order.equals("charge");}
    /** Between beats a shield troop re-reads the distance to the enemy it chose on its beat, so it lowers its shield on the tick that enemy steps
     *  into sword reach (BattleMath.raiseShield), and drops it on the tick its order turns to Charge or Retreat. With no enemy chosen it re-reads
     *  the fire: off Hold the shield follows it tick by tick (it drops with the crawl when the fire memory runs out); on Hold fire raises it at once,
     *  and an enemy only in sight keeps the beat's decision until the next beat. The crawl is kept every tick. */
    static void brace(GravesAndGlory.Battle battle,GravesAndGlory.Unit u){
        if(!u.tags.contains("shield"))return;
        var a=battle.assignment(u);String order=shieldOrder(u,a);var t=u.lockedTarget;boolean fire=underFire(battle,u);
        if(t!=null&&t.isAlive()){double reach=Abilities.range(battle,u);shield(u.mob,BattleMath.raiseShield(order,fire,u.mob.squaredDistanceTo(t),reach*reach,true));}
        else if(!order.equals("hold"))shield(u.mob,BattleMath.raiseShield(order,fire,Double.MAX_VALUE,0,false));
        else if(fire)shield(u.mob,true);
        crawl(battle,u,order);
    }
    /**
     * The last stretch (ruling R21): a wall troop (Battle.walls) with no enemy chosen faces its Hold yaw (yaw, body and head) every tick within
     * STRETCH of its slot, and steps in without turning: sideways or backwards as well as forwards. It is a layer over walk, not a way round it:
     * it holds the troop only where walk would step it straight onto its slot (walk's precise branch), that is, when its way is not routed
     * (route gives the slot itself: no bridge, no house gate), it is on no detour, and every cell of the straight line to the slot is open and
     * level (clear). Anywhere else walk routes it, turning as it walks. The step is a level velocity toward the slot of walking pace (pace), or
     * 0.454 of the way once that is less (let go at any tick, it coasts onto the slot), down to SETTLED: one smooth motion onto the slot, with no
     * stop at the arrival tolerance for settle to slide it on later. The velocity is used, not MoveControl.strafeTo, because strafe input is
     * capped at a quarter of the movement speed (0.245 s a tick for speed s), short of walking pace (0.98 s x s) for any troop faster than 0.25
     * (a Squire at 0.28 would step in at 89% of his walk). A troop that gains less than PROGRESS in STUCK ticks (a body or a corner in its way)
     * is let go for STUCK_OFF ticks, so walk's stuck handling, StepHop and detours take it. Pressed against the face of a rise its slot is on
     * (onRise), it jumps as settle does, and faces the same way through the jump (hop carries it). It yields when it has an enemy (it turns only
     * to fight one). In the air it only keeps facing. Returns whether it holds the troop.
     */
    static boolean stretch(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,GravesAndGlory.Assignment a,long time){
        var t=u.lockedTarget;
        if(!GravesAndGlory.Battle.walls(u,a.order)||t!=null&&t.isAlive()){u.stretchAt=-1;return false;}
        var mob=u.mob;if(u.place==null)u.place=battle.formation(u);Vec3d goal=u.place;
        if(u.hopTo!=null){face(mob,a.yaw);return true;}
        double d2=GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal);
        if(d2>STRETCH*STRETCH||time<u.stretchOff||!clear(battle,u,goal)){u.stretchAt=-1;return false;}
        double d=Math.sqrt(d2);
        if(d>N.arrivalTolerance()){ // the stuck fallback: no gain of PROGRESS for STUCK ticks lets walk have it
            if(u.stretchAt<0||d<u.stretchBest-PROGRESS){u.stretchBest=d;u.stretchAt=time;}
            else if(time-u.stretchAt>=STUCK){u.stretchOff=time+STUCK_OFF;u.stretchAt=-1;stretchStuck++;return false;}
        }else u.stretchAt=-1;
        mob.getNavigation().stop();face(mob,a.yaw);
        if(d<=SETTLED||!mob.isOnGround())return true;
        if(mob.horizontalCollision&&onRise(mob,goal)){mob.getJumpControl().setActive();u.hopTo=goal;u.hopUntil=time+HOP_TICKS;u.hopAirborne=false;hops++;return true;}
        double step=Math.min(d*.454,pace(mob));Vec3d delta=goal.subtract(mob.getPos());
        mob.setVelocity(delta.x/d*step,mob.getVelocity().y,delta.z/d*step);
        return true;
    }
    /** The stuck fallback of the stretch: a gain this small (blocks) in STUCK ticks is no progress, and the troop is left to walk for STUCK_OFF ticks; how many times that happened (HoldLineChecks reads it). */
    static final double PROGRESS=.05;static final int STUCK=20,STUCK_OFF=100;static int stretchStuck;
    /** The troop would walk straight onto its goal (walk's precise branch): no detour, no routed way (route gives the goal itself), and every cell the
     *  straight line from it to the goal crosses (sampled each 0.25 block) is open and level (open). */
    static boolean clear(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d goal){
        if(u.detour!=null||GravesAndGlory.Battle.horizontalDistance(route(u,goal),goal)>1e-6)return false;
        var mob=u.mob;double dx=goal.x-mob.getX(),dz=goal.z-mob.getZ();int n=Math.max(1,(int)Math.ceil(Math.sqrt(dx*dx+dz*dz)/.25));
        int lx=Integer.MIN_VALUE,lz=Integer.MIN_VALUE;
        for(int i=0;i<=n;i++){
            int x=(int)Math.floor(mob.getX()+dx*i/n),z=(int)Math.floor(mob.getZ()+dz*i/n);
            if(x==lx&&z==lz)continue;lx=x;lz=z;
            if(!open(battle,mob,x,z))return false;
        }
        return true;
    }
    /** How near its slot a wall troop is held to its Hold yaw: walk's precise range, inside which it would otherwise step (MoveControl) facing its step. */
    static final double STRETCH=N.preciseRange();
    /** Sets the mob's yaw, body yaw and head yaw. */
    static void face(MobEntity mob,float yaw){mob.setYaw(yaw);mob.setBodyYaw(yaw);mob.setHeadYaw(yaw);}
    /** A troop's walking pace in blocks a tick on level ground: MoveControl gives it forward input s (its movement speed), vanilla adds 0.98 s x s
     *  of velocity a tick, and ground friction keeps 0.546 of it, so it settles at 0.98 s x s / 0.454. Its speed modifiers (crawl, Howl) count. */
    static double pace(MobEntity mob){double s=mob.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);return .98*s*s/.454;}
    /** Whether a cell is a level step or a one-block rise away (walk's precise branch tests its goal's cell so): the cell and the one above are air,
     *  its ground is within 1.1 of the troop's feet, and it is no river water below the crossings. In the air (mid-hop) the feet are read at the
     *  block level they are over (floor of y), so a troop jumping onto a one-block rise does not read its own line blocked at the top of the
     *  jump (Task 16, R89). */
    static boolean open(GravesAndGlory.Battle battle,MobEntity mob,int gx,int gz){
        int gy=Arena.ground(gx,gz)+1;double feet=mob.isOnGround()?mob.getY():Math.floor(mob.getY());
        return Math.abs(feet-gy)<1.1&&!(Arena.river(gx,gz)&&Arena.ground(gx,gz)<64)&&battle.world.getBlockState(new BlockPos(gx,gy,gz)).isAir()&&battle.world.getBlockState(new BlockPos(gx,gy+1,gz)).isAir();
    }
    /** How far a troop picks its targets: the order's engage radius, or for a siege troop (tag siege) at least its weapon's reach (Entrench's bonus included), so artillery shoots what it can reach and not only what the order's leash allows. The sheet's order radii are untouched. */
    static double engageRadius(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,double order){return u.tags.contains("siege")?Math.max(order,Abilities.range(battle,u)):order;}
    /** Line of sight for targeting (AI fix 2026-11-11): whether the troop sees the entity now. One raycast (LivingEntity.canSee) per troop and entity
     *  a beat at most: a result read within SIGHT_REFRESH ticks is reused (Unit.sight, keyed by UUID). Old entries are dropped once the cache grows past SIGHT_KEEP. */
    static boolean sees(GravesAndGlory.Unit u,Entity e,long time){
        long[] r=u.sight.get(e.getUuid());
        if(r==null||time-r[0]>=SIGHT_REFRESH||time<r[0]){
            if(r==null){if(u.sight.size()>=SIGHT_KEEP)u.sight.values().removeIf(v->time-v[0]>SIGHT_REFRESH*4||time<v[0]);r=new long[]{0,Long.MIN_VALUE/2};u.sight.put(e.getUuid(),r);}
            r[0]=time;sightRays++;if(u.mob.canSee(e))r[1]=time;
        }
        return r[1]==r[0];
    }
    /** Whether the troop saw the entity within sightMemoryTicks: a target it already holds is kept that long out of sight, so it does not flicker. */
    static boolean remembers(GravesAndGlory.Unit u,Entity e,long time){
        if(sees(u,e,time))return true;
        long[] r=u.sight.get(e.getUuid());return r!=null&&time-r[1]<=C.sightMemoryTicks();
    }
    static final int SIGHT_REFRESH=10,SIGHT_KEEP=32;
    /** Raycasts sees has made since the server started (the sim may read it). */
    static long sightRays;
    /** The scan's PICK_KEEP nearest candidates, nearest first (offer). The pick takes the first the troop can see among the PICK_TESTS nearest, then
     *  among PICK_MORE of the rest, a different few each beat (Unit.pickRoll), so a visible enemy behind a screen of hidden ones is found within a few
     *  beats: at most PICK_TESTS + PICK_MORE raycasts a beat. */
    static final int PICK_TESTS=6,PICK_MORE=3,PICK_KEEP=15;
    static final LivingEntity[] PICK=new LivingEntity[PICK_KEEP];static final double[] PICK_D=new double[PICK_KEEP];
    /** Puts a candidate into PICK by distance; returns the new count. */
    static int offer(int n,LivingEntity e,double d){
        int i=Math.min(n,PICK_KEEP-1);
        if(n==PICK_KEEP&&d>=PICK_D[PICK_KEEP-1])return n;
        while(i>0&&PICK_D[i-1]>d){PICK[i]=PICK[i-1];PICK_D[i]=PICK_D[i-1];i--;}
        PICK[i]=e;PICK_D[i]=d;return Math.min(n+1,PICK_KEEP);
    }
    /** The first candidate in PICK the troop sees (or, by the wagon, one standing in its push range: it stops the wagon seen or not), or null. */
    static LivingEntity pick(GravesAndGlory.Unit u,int found,Vec3d guard,long time){
        for(int k=0;k<Math.min(found,PICK_TESTS);k++)if(seenOrPushing(u,PICK[k],guard,time))return PICK[k];
        int rest=found-PICK_TESTS;
        if(rest>0){int start=u.pickRoll++;for(int j=0;j<Math.min(rest,PICK_MORE);j++){var e=PICK[PICK_TESTS+Math.floorMod(start*PICK_MORE+j,rest)];if(seenOrPushing(u,e,guard,time))return e;}}
        return null;
    }
    static boolean seenOrPushing(GravesAndGlory.Unit u,LivingEntity e,Vec3d guard,long time){return horde(u)||guard!=null&&inPush(guard,e)||sees(u,e,time);}
    /** Survival's waves (the director's army, SurvivalMode.DIRECTOR) pick as before the sight rule: the nearest defender in reach, seen or not. They still shoot only what they see. */
    static boolean horde(GravesAndGlory.Unit u){return SurvivalMode.DIRECTOR.equals(u.squad.owner);}
    /** Escorts guard the wagon (AI fix 2026-11-11, audit C1 and C2): a Convoy escort troop within guardNear of the wagon gets the wagon's position, so
     *  its pick puts enemies within guardRadius of the wagon (onWagon: a single defender within push range stops it) before any other. Target choice
     *  only, for player and bot escorts alike: it moves no troop. null in any other mode, for the defence, and for an escort troop farther off. */
    static Vec3d guardPoint(GravesAndGlory.Battle battle,GravesAndGlory.Unit u){
        if(!(battle.mode instanceof ConvoyMode c)||u.squad.team!=ConvoyMode.ESCORT||c.wagon==null||c.finished||ConvoyMode.R.guardRadius()<=0)return null;
        Vec3d w=c.wagon.getPos();double near=ConvoyMode.R.guardNear();
        return GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(),w)<=near*near?w:null;
    }
    static boolean onWagon(Vec3d guard,Entity e){double r=ConvoyMode.R.guardRadius();return GravesAndGlory.Battle.horizontalDistance(e.getPos(),guard)<=r*r&&Math.abs(e.getY()-guard.y)<=ConvoyMode.PUSH_HEIGHT;}
    /** Within the wagon's push range (ConvoyMode.inRange): an enemy there stops the wagon, so an escort by it counts it as seen. */
    static boolean inPush(Vec3d guard,Entity e){return ConvoyMode.inRange(guard,e.getX(),e.getY(),e.getZ());}
    /** A scan candidate for the pick (offer): by the wagon, enemies on it first (their distance less GUARD_FIRST); with a target held for later, only those. */
    static int guarded(int n,Vec3d guard,LivingEntity held,LivingEntity e,double d){
        if(guard==null)return offer(n,e,d);
        boolean on=onWagon(guard,e);
        if(held!=null&&!on)return n;
        return offer(n,e,on?d-GUARD_FIRST:d);
    }
    static final double GUARD_FIRST=1e7;
    static void tick(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,long time){
        hop(u,time); // a wall troop jumping onto its slot on a rise is carried over the edge every tick of the jump
        if(u.lockedTarget!=null&&!u.lockedTarget.isAlive())u.lockedTarget=null; // a fallen enemy lies a while (Task 7): drop it at once, not on the next beat
        if(u.mob.getTarget()!=null&&!u.mob.getTarget().isAlive())u.mob.setTarget(null);
        if(u.looseAt>=0&&time>=u.looseAt)looseLate(battle,u,time); // Kort Q70: a bow troop's shot leaves its loose ticks after the beat that took it
        stunStance(u,time);
        if(time<u.downUntil){u.mob.getNavigation().stop();u.mob.setTarget(null);u.mob.setAttacking(false);return;} // the Undying King down and rising (Houses.UNDYING_DOWN): no move, no turn, no act
        if(time%10!=u.slot%10){brace(battle,u);shieldStance(u);stretch(battle,u,battle.assignment(u),time);return;} // the shield and a wall troop's last stretch every tick; everything else on the unit's 10-tick beat
        u.place=null; // the beat reads the slot again
        var mob=u.mob;var s=u.squad;var a=battle.assignment(u);
        Houses.sample(battle,u); // Entrench: has it stood still since its last beat
        Houses.swim(battle,u); // Riptide: faster while it touches water or it rains
        boolean arrived=arrived(battle,u,a); // a Retreat or Move group at its destination fights as on Hold from its slot (arrived)
        var owner=battle.server.getPlayerManager().getPlayer(s.owner);
        if(a.thenFollow&&a.order.equals("retreat")){ // a player's Retreat (Task 5): back to the commander where he stands now, frozen at his last spot while he waits to respawn; Follow me once there
            if(owner!=null&&!battle.respawns.containsKey(s.owner)&&!owner.isSpectator()&&owner.isAlive())a.target=owner.getPos();
            if(groupWithin(battle,u,C.retreatArriveRadius()))a=become(u,a,"follow",time);
        }else if(a.order.equals("charge")&&a.chargeDir!=null&&groupWithin(battle,u,C.arriveRadius()))a=become(u,a,"attack",time); // a player's Charge at its end point hunts on
        if(a.order.equals("follow")&&owner!=null&&!battle.respawns.containsKey(s.owner)){a.target=owner.getPos();a.yaw=owner.getYaw();}
        String orderId=a.order;
        var order=arrived?HOLD:Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(orderId)).findFirst().orElse(Spec.ORDERS[0]);
        boolean hold=a.order.equals("hold")||arrived;
        boolean holdSiege=hold&&u.tags.contains("siege"); // artillery on Hold shoots what is within its reach from where it stands and never walks toward a target, so it never walks out of Entrench
        boolean hunt=a.order.equals("attack")&&a.hunt; // a player's Attack: anything it can see within scouting range, else the nearest enemy anywhere (no leash)
        boolean dash=a.order.equals("charge")&&a.chargeDir!=null; // a player's Charge down its direction
        boolean volley=dash&&u.spec.ranged(),lane=dash&&!volley; // ranged on a Charge advance and shoot what is in reach; melee strike only what stands on their path
        double speed=order.moveMultiplier()*(a.order.equals("hold")?FormationRules.moveShare(u,a):1); // a Formation (Hold) walks at its formation's share
        double engage=holdSiege?Abilities.range(battle,u):engageRadius(battle,u,hunt?Spec.SCOUTING[0].range():order.engageRadius()); // the order's radius, widened to the weapon's reach for a siege troop; on Hold the weapon's reach as it is now
        if(volley)engage=Math.min(engage,Abilities.range(battle,u));
        boolean slotted=a.order.equals("hold")&&a.holdSlots&&placed(battle,u); // a player's Formation in place: melee keep their slots, ranged shoot anything in reach
        Vec3d slot=slotted?battle.formation(u):null;double keep=Abilities.range(battle,u);
        if(slotted&&u.spec.ranged())engage=keep;
        boolean holdRanged=hold&&!holdSiege&&u.spec.ranged(); // a ranged troop on Hold (or arrived) shoots any enemy it sees within its weapon's reach, from its slot: it never walks out after one
        if(holdRanged)engage=Math.max(engage,keep);
        boolean told=false;LivingEntity enemy=null,close=null;double nearest=engage*engage,closest=nearest; // enemy: the one it picks (never one inside its minimum range); close: the nearest of any, which a ranged troop backs off from
        boolean hidden=!horde(u)&&u.lockedTarget!=null&&u.lockedTarget.isAlive()&&!sees(u,u.lockedTarget,time); // a held target out of sight is kept for sightMemoryTicks from its last sighting, even past the lock
        if(order.engage()&&u.lockedTarget!=null&&u.lockedTarget.isAlive()&&(time<u.lockUntil||hidden)&&mob.squaredDistanceTo(u.lockedTarget)<nearest*(holdSiege?1:1.4)&&!Abilities.tooClose(u,mob.squaredDistanceTo(u.lockedTarget))&&leash(u,a,hold,holdSiege,slotted,slot,keep,engage,u.lockedTarget.getPos())&&(!lane||ahead(u,a,u.lockedTarget.getPos()))&&(horde(u)||remembers(u,u.lockedTarget,time)))enemy=u.lockedTarget; // kept while seen within sightMemoryTicks, so a step behind a post does not drop it
        if(order.engage()&&a.enemy>=0&&battle.world.getEntityById(a.enemy) instanceof LivingEntity explicit&&explicit.isAlive()&&sees(u,explicit,time)&&!Abilities.tooClose(u,mob.squaredDistanceTo(explicit))){enemy=explicit;a.target=explicit.getPos();told=true;}
        Vec3d guard=order.engage()?guardPoint(battle,u):null; // Convoy: an escort troop near the wagon puts the defenders on it first (one there stops it)
        LivingEntity held=null;
        if(enemy!=null&&!told&&guard!=null&&!onWagon(guard,enemy)){held=enemy;enemy=null;} // it keeps its target only if no defender on the wagon is in sight
        if(enemy!=null){nearest=mob.squaredDistanceTo(enemy);close=enemy;closest=nearest;}
        boolean locked=enemy!=null;
        // The scan: every enemy troop and commander inside the engage radius and the leash. close is the nearest of any (a ranged troop backs off
        // from it, seen or not); the pick is the nearest the troop can see, tested nearest first on at most PICK_TESTS of them (sees, cached a beat).
        int found=0;
        if(order.engage()&&!locked){
            for(var other:battle.units)if(other.squad.team!=s.team&&other.mob.isAlive()){
                double d=mob.squaredDistanceTo(other.mob);
                if(!(d<closest||d<nearest))continue;
                if(!leash(u,a,hold,holdSiege,slotted,slot,keep,engage,other.mob.getPos())||lane&&!ahead(u,a,other.mob.getPos()))continue;
                if(d<closest){close=other.mob;closest=d;}
                if(d<nearest&&!Abilities.tooClose(u,d))found=guarded(found,guard,held,other.mob,d);
            }
            for(ServerPlayerEntity p:battle.server.getPlayerManager().getPlayerList()){
                var other=battle.squads.get(p.getUuid());if(other!=null&&other.team!=s.team&&!p.isSpectator()&&p.isAlive()&&leash(u,a,hold,holdSiege,slotted,slot,keep,engage,p.getPos())&&(!lane||ahead(u,a,p.getPos()))){
                    double d=mob.squaredDistanceTo(p);
                    if(d<closest){close=p;closest=d;}
                    if(d<nearest&&!Abilities.tooClose(u,d))found=guarded(found,guard,held,p,d);
                }
            }
            enemy=pick(u,found,guard,time);if(enemy!=null)nearest=mob.squaredDistanceTo(enemy);
            java.util.Arrays.fill(PICK,0,found,null);
            if(enemy==null&&held!=null){enemy=held;nearest=mob.squaredDistanceTo(held);locked=true;} // no defender on the wagon in sight: back to the target it held
        }
        if(order.engage()&&!locked){var drawn=GearCombat.taunter(battle,u);if(drawn!=null&&(horde(u)||sees(u,drawn,time))&&!Abilities.tooClose(u,mob.squaredDistanceTo(drawn))&&(!lane||ahead(u,a,drawn.getPos()))&&(!volley||mob.squaredDistanceTo(drawn)<=engage*engage)){enemy=drawn;nearest=mob.squaredDistanceTo(drawn);close=drawn;closest=nearest;}} // taunt_aura: a share of the troops near a commander wearing it pick him
        if(order.engage()&&!locked&&enemy==null)for(var st:battle.structures){ // a supply banner is a target when no troop or commander is: the army first, then its commander, then its banner
            if(st.team()==s.team||!st.isAlive())continue;
            double d=mob.squaredDistanceTo(st);
            boolean inLeash=leash(u,a,hold,holdSiege,slotted,slot,keep,engage,st.getPos())&&(!lane||ahead(u,a,st.getPos()));
            if(inLeash&&d<closest){close=st;closest=d;}
            if(inLeash&&d<nearest&&!Abilities.tooClose(u,d)&&(!u.spec.ranged()||horde(u)||sees(u,st,time))){enemy=st;nearest=d;} // a ranged troop shoots only a banner it sees; melee walk to one
        }
        if(hunt&&!locked&&enemy==null){ // nothing in sight within scouting range: the nearest living enemy troop or commander anywhere in the battle
            double far=Double.MAX_VALUE;
            for(var other:battle.units)if(other.squad.team!=s.team&&other.mob.isAlive()){double d=mob.squaredDistanceTo(other.mob);if(d<far&&!Abilities.tooClose(u,d)){far=d;enemy=other.mob;}}
            for(ServerPlayerEntity p:battle.server.getPlayerManager().getPlayerList()){var other=battle.squads.get(p.getUuid());if(other!=null&&other.team!=s.team&&!p.isSpectator()&&p.isAlive()&&!battle.respawns.containsKey(p.getUuid())){double d=mob.squaredDistanceTo(p);if(d<far&&!Abilities.tooClose(u,d)){far=d;enemy=p;}}}
            if(enemy!=null)nearest=far;
        }
        if(enemy!=u.lockedTarget){u.lockedTarget=enemy;u.lockUntil=time+Spec.COMBATDISCIPLINE[0].targetMemoryTicks();u.lastSeen=null;}
        if(enemy!=null&&sees(u,enemy,time))u.lastSeen=enemy.getPos(); // where a melee troop chases it once it is round a corner
        mob.setTarget(enemy);mob.setAttacking(enemy!=null);
        double reach=Abilities.range(battle,u);boolean ranged=u.spec.ranged();boolean post=a.perch||slotted&&ranged||holdRanged; // a ranged troop on a raised post, in place on a Formation or on Hold shoots from its slot
        if(enemy!=null&&nearest<=reach*reach&&(horde(u)||sees(u,enemy,time)))stamp(battle,u,enemy); // an archer taking aim from within its reach puts its target's group under fire
        if(u.tags.contains("shield")){ // Hold: up with any enemy picked or in sight; other orders: up under archer fire; never on Charge or Retreat, down to swing once the enemy is in reach
            String shieldOrder=shieldOrder(u,a);double near=nearest;boolean any=enemy!=null;
            if(!any&&shieldOrder.equals("hold")&&sighted(battle,u)>=0){any=true;near=Double.MAX_VALUE;} // an enemy only in sight is not picked (outside the leash), so it is never struck at and never lowers the shield, however near
            shield(mob,BattleMath.raiseShield(shieldOrder,underFire(battle,u),near,reach*reach,any));
            crawl(battle,u,shieldOrder);
        }
        boolean aiming=ranged&&enemy!=null&&nearest<=reach*reach&&sees(u,enemy,time); // stance_v2: a ranged troop with its target in reach and sight aims at it
        show(battle,u,arrived?held(a):a,aiming,aiming?Projectiles.aimPitch(battle,u,enemy):0);
        Abilities.tick(battle,u,enemy,time); // mend heals and a caster's spell bursts on the beat
        double flee=Math.max(N.rangedRetreatDistance(),Abilities.minRange(u)); // a ranged troop backs off from an enemy this near; one with a minimum range (the Mortar) from anything inside it
        if(enemy!=null){
            mob.getLookControl().lookAt(enemy,40,40);
            boolean visible=sees(u,enemy,time);
            if(nearest<=reach*reach&&visible){
                if(ranged&&close!=null&&closest<flee*flee&&!hold&&!a.perch)backOff(battle,u,close,time);
                else if(post&&ranged)toSlot(battle,u,a,time,speed); // it shoots on its way up and climbs on
                else mob.getNavigation().stop();
                if(time>=u.nextAttack&&time>=u.stunnedUntil&&!pending(battle,u)&&!Abilities.tooClose(u,nearest)){
                    int cooldown=u.spec.cooldown();u.nextAttack=u.nextAttack<time-10?time+cooldown:u.nextAttack+cooldown;mob.swingHand(Hand.MAIN_HAND);
                    int late=Projectiles.looseTicks(u);
                    if(late>0){u.looseAt=time+late;u.looseTarget=enemy;} // Kort Q70: a bow troop's shot leaves late ticks on (looseLate)
                    else shoot(battle,u,enemy,time,ranged);
                }else if(announcesAhead(u)&&time+10>=u.nextAttack&&time+10>=u.stunnedUntil&&!pending(battle,u)&&!Abilities.tooClose(u,nearest))Projectiles.drawAhead(battle,u,enemy,10); // the beat before a bow's or crossbow's shot: its clip is announced now, the shot still leaves on the next beat (cosmetic, R110 Plan B)
                else if(Abilities.castsAhead(u)&&time+10>=u.nextAttack&&time+10>=u.abilityAt&&time+10>=u.stunnedUntil&&!pending(battle,u)&&!Abilities.tooClose(u,nearest))Projectiles.drawAhead(battle,u,enemy,10); // the beat before the hexer's blight or the mist caller's mist leaves (its spell cooldown ready too): its cast clip is announced now, at the start tick Abilities.attack's release stamps (cosmetic, Kort 2026-11-06)
            }else if(post&&ranged){toSlot(battle,u,a,time,speed); // a ranged troop on a post shoots what it can reach from there and never walks off after the rest
            }else if(volley){toSlot(battle,u,a,time,speed); // a ranged troop on a Charge never closes on its target: it runs on down the path and stops once it can shoot
            }else if(!holdSiege&&leash(u,a,hold,holdSiege,slotted,slot,keep,engage,enemy.getPos())){
                Vec3d goal=enemy.getPos();
                boolean far=hunt&&nearest>reach*reach; // a hunt's far quarry, unseen: walk at it, not around the obstruction in front
                if(ranged&&!visible&&!far){Vec3d toward=enemy.getPos().subtract(mob.getPos()).multiply(1,0,1).normalize();double sign=u.slot%2==0?1:-1;goal=mob.getPos().add(-toward.z*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep(),0,toward.x*sign*Spec.COMBATDISCIPLINE[0].rangedObstructionStep());}
                if(!ranged&&!visible&&!far&&u.lastSeen!=null)goal=u.lastSeen; // round a corner: to where it was last seen, until the memory runs out
                else if(!ranged){double angle=u.slot*2.399963;goal=goal.add(Math.cos(angle)*1.6,0,Math.sin(angle)*1.6);}
                if(ranged&&(visible||far)&&nearest>reach*reach)goal=goal.add(mob.getPos().subtract(goal).multiply(1,0,1).normalize().multiply(reach*N.rangedStandoff()));
                walk(battle,u,goal,time,speed);
            }else mob.getNavigation().stop();
        }else if(close!=null&&ranged){ // everything in its leash stands inside its minimum range: nothing to shoot; it backs off (unless it holds) and does not walk off to its formation
            if(post)toSlot(battle,u,a,time,speed);else if(!hold&&closest<flee*flee)backOff(battle,u,close,time);else mob.getNavigation().stop();
        }else{
            toSlot(battle,u,a,time,speed);
        }
        if(a.order.equals("hold")&&!mob.getNavigation().isIdle())FormationRules.shove(battle,u,a); // a Formation's troops walking into place make room (Task 6's rule)
    }
    /** To its formation slot, and still there facing the order's way once it stands on it: within arrivalTolerance across and, on a post (Assignment.perch), at its height, so that a troop under a tower top is not taken for one on it. */
    static void toSlot(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,GravesAndGlory.Assignment a,long time,double speed){
        MobEntity mob=u.mob;Vec3d goal=battle.formation(u);u.place=goal;
        boolean there=GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal)<=N.arrivalTolerance()*N.arrivalTolerance()&&(!a.perch||Math.abs(mob.getY()-goal.y)<1.1);
        if(!there){if(!stretch(battle,u,a,time))walk(battle,u,goal,time,speed,a.perch);} // a wall troop's last stretch faces its Hold yaw (stretch)
        else{mob.getNavigation().stop();mob.setYaw(a.yaw);mob.setHeadYaw(a.yaw);mob.setBodyYaw(a.yaw);if(GravesAndGlory.Battle.walls(u,a.order))settle(u,goal,time);}
    }
    /** A raised goal (Assignment.perch) is climbed to by a whole vanilla path to its own height once it is this near across (blocks); walk's ground waypoints, which only know the terrain (Arena.ground), lead there until then. */
    static final double CLIMB_RANGE=24;
    /**
     * A wall troop inside the arrival tolerance of its slot is set going the rest of the way: the tolerance (0.35) is half its 0.7 cell, so one
     * left there would lean on its neighbour, and the walk stops short or overshoots by about that much. On the ground a velocity v carries a mob
     * v / (1 - 0.546) = 2.2 v before friction stops it, so 0.454 of the way is the whole way, and it stops on its slot. A mob in the air is left.
     * A slot on a rise one block high (onRise) is jumped onto instead: the level push would only press the troop against the face of the rise.
     * The jump is straight up (its jump control), and hop carries it over the edge onto its slot while it is in the air; it keeps facing the
     * order's way the whole time (the beat set its yaw), so it neither stares sideways nor drifts forward on its own air control.
     */
    static void settle(GravesAndGlory.Unit u,Vec3d goal,long time){
        var mob=u.mob;Vec3d delta=goal.subtract(mob.getPos()).multiply(1,0,1);
        if(!mob.isOnGround()||delta.lengthSquared()<=SETTLED*SETTLED)return;
        if(onRise(mob,goal)){mob.getJumpControl().setActive();u.hopTo=goal;u.hopUntil=time+HOP_TICKS;u.hopAirborne=false;hops++;return;}
        mob.setVelocity(delta.x*.454,mob.getVelocity().y,delta.z*.454);
    }
    /**
     * Every tick of a wall troop's jump onto its slot (settle): its level velocity is HOP_PULL of the way to the slot, no faster than its walking pace and never over HOP_CAP
     * (StepHop.pull: a jump begun from the far side of the stretch is no lunge), so once its feet clear the
     * edge it comes down on the slot (the next beat's settle finishes the last of it). The jump ends when it lands after being in the air, after
     * HOP_TICKS, or when it walks off (to fight, or on a new order).
     */
    static void hop(GravesAndGlory.Unit u,long time){
        if(u.hopTo==null)return;var mob=u.mob;
        if(!mob.isAlive()||time>u.hopUntil||!mob.getNavigation().isIdle()||u.hopAirborne&&mob.isOnGround()){u.hopTo=null;return;}
        if(!mob.isOnGround())u.hopAirborne=true;
        Vec3d delta=u.hopTo.subtract(mob.getPos()).multiply(1,0,1);double[] v=StepHop.pull(delta.x,delta.z,HOP_PULL,Math.min(HOP_CAP,pace(mob)));
        mob.setVelocity(v[0],mob.getVelocity().y,v[1]);
    }
    /** The share of the way to its slot a jumping wall troop is carried each tick, how long a jump may last, and how many jumps settle started (HoldLineChecks reads it). */
    static final double HOP_PULL=.5,HOP_CAP=.3;static final int HOP_TICKS=20;static int hops;
    /**
     * The troop's slot reaches over the edge of a rise one block high (StepHop.onRise): its body on the slot, at the troop's own level, would
     * stand in the rise (higher than a step climbs), and one block up it has room, as it has above itself for the jump. The level is the troop's
     * own Y, not the block it is in, so soul sand or mud underfoot is not a rise. A slot 0.7 wide on ground with a step in it is stood on from the
     * top of the rise; a troop that comes to it along the lower ground stops against the face, as much short of its slot as its body reaches over
     * the edge (0.15 for a slot 0.15 from it), inside the arrival tolerance, so walk, which jumps, is not called again. Its neighbour in the rank,
     * pushed off it, stays short of his own slot too (HoldLineChecks: a Squire 0.15 short, the Marshal beside him 0.165).
     */
    static boolean onRise(MobEntity mob,Vec3d goal){
        var w=mob.getWorld();
        return StepHop.onRise(mob.getX(),mob.getY(),mob.getZ(),mob.getWidth()/2,mob.getHeight(),goal.x,goal.z,mob.getStepHeight(),(x0,y0,z0,x1,y1,z1)->w.isSpaceEmpty(mob,new Box(x0,y0,z0,x1,y1,z1)));
    }
    /** How near its slot a wall troop must stand to be left alone by settle. */
    static final double SETTLED=.03;
    /** Five blocks straight away from an enemy, at a hurried walk: how a ranged troop backs off from one too near. */
    static void backOff(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,LivingEntity from,long time){
        Vec3d away=u.mob.getPos().subtract(from.getPos()).multiply(1,0,1).normalize();
        walk(battle,u,u.mob.getPos().add(away.multiply(5)),time,1.05);
    }
    /** The way to a goal: across the river by the nearest bridge, unless the troop fords it (Riptide tier II and III), which goes straight at the goal. */
    static Vec3d route(GravesAndGlory.Unit u,Vec3d goal){
        fieldPoint=null;
        if(Houses.fords(u))return goal;
        Vec3d way=fieldWay(u,goal);if(way!=null){fieldPoint=way;return way;}
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
                var battle=GravesAndGlory.battle;double offset=(u.hasCell()?WarGrid.col(u.slot)-(WarGrid.cols()-1)/2:u.slot%5-2)*(battle==null?GravesAndGlory.Battle.spacing(u):battle.cell(u)); // its lane: a wall troop's cell on Hold, else its body's room
                double entry=best.x()+Math.signum(side)*(best.width()/2.0-N.bridgeEntryInset());
                boolean onBridge=Math.abs(here.z-best.z())<best.depth()/2.0-1&&Math.abs(here.x-best.x())<best.width()/2.0+2;
                double x=onBridge?best.x()+Math.signum(destination)*(best.width()/2.0+2):entry;
                return new Vec3d(x,best.elevation()+1,best.z()+offset);
            }
        }
        return houseGate(u,goal);
    }
    /** The route field of the engine map in play (NavField, nav.bin.gz), or null: an old map, or an engine build without one. */
    /** The waypoint the last route() call took from the route field (an identity: walk asks whether the goal it was handed is that very point), null when it took none. */
    static Vec3d fieldPoint;
    static NavField field(){var e=EngineMaps.current();return e==null?null:e.nav;}
    /** Where a troop on foot goes next toward a goal it cannot see across the floors: the farthest cell of the field's way it can see, a real floor with its height (Arena.floor and the world's top block play no part), a portal cell at a door narrower than NavField.PORTAL.
     *  Null: no field (old maps, and the goal itself is wanted), the goal is in plain sight over the floors, or no route is known. A table lookup and a few line tests, no search of the map; a field is built at most NavField.BUILDS_PER_TICK times a tick. */
    static Vec3d fieldWay(GravesAndGlory.Unit u,Vec3d goal){
        var nav=field();if(nav==null)return null;
        var mob=u.mob;var b=GravesAndGlory.battle;
        var wp=nav.route(mob.getX(),mob.getY(),mob.getZ(),goal.x,goal.y,goal.z,b==null?0:b.time);
        if(wp==null)return null;
        double lane=(u.hasCell()?WarGrid.col(u.slot)-(WarGrid.cols()-1)/2:u.slot%5-2)*(b==null?GravesAndGlory.Battle.spacing(u):b.cell(u)); // its lane through a door, as at a bridge: a wall troop's cell on Hold, else its body's room
        wp=nav.lane(wp,mob.getX(),mob.getZ(),lane);
        return new Vec3d(wp.x(),wp.y(),wp.z());
    }
    /** A goal inside the walled yard of a supply house (Phase 3 Task 8b): the point just outside a gate (HouseRoute) until the unit stands at it, then the goal itself, so that the units do not stand at the wall. Any other goal comes back as it is. */
    static Vec3d houseGate(GravesAndGlory.Unit u,Vec3d goal){
        for(var r:housesOfMap()){
            double[] p=HouseRoute.next(r.x(),r.z(),r.rotation(),u.mob.getX(),u.mob.getZ(),goal.x,goal.z,(u.hasCell()?WarGrid.col(u.slot)-(WarGrid.cols()-1)/2:u.slot%5-2)*0.8);
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
    /** The unit stood still for the stuck time: when the cell beside it that is nearest its waypoint is one block up (a step under a low skirt of leaves, which a jump cannot make), it is set on that cell, at the point of it nearest the waypoint. */
    static boolean stepHop(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,Vec3d goal){
        if(!HOP)return false;
        var w=battle.world;var mob=u.mob;
        int[] c=StepHop.up(mob.getX(),mob.getY(),mob.getZ(),goal.x,goal.z,new StepHop.Probe(){
            public boolean open(int x,int y,int z){var p=new BlockPos(x,y,z);return w.getBlockState(p).getCollisionShape(w,p).isEmpty()&&w.getFluidState(p).isEmpty();}
            public boolean floor(int x,int y,int z){var p=new BlockPos(x,y,z);return w.getBlockState(p).isSideSolidFullSquare(w,p,Direction.UP);}
        });
        if(c==null)return false;
        if(TRACE_HOPS)System.out.println("[Graves and Glory] step hop of slot "+u.slot+" from ("+String.format("%.1f,%.1f,%.1f",mob.getX(),mob.getY(),mob.getZ())+") to ("+c[0]+","+c[1]+","+c[2]+") on its way to ("+String.format("%.1f,%.1f",goal.x,goal.z)+")");
        // on the point of the step nearest the waypoint, not its middle (StepHop.landing), so two wall neighbours that hop onto one block land up to
        // 1 - width apart (0.4 for a 0.6-wide troop, their boxes still overlapping by 0.2 until settle walks them back to their places); a body a
        // block wide or wider has no room to slide on the block and takes its middle
        double[] land=StepHop.landing(mob.getWidth(),c[0],c[2],goal.x,goal.z);
        mob.refreshPositionAndAngles(land[0],c[1],land[1],mob.getYaw(),mob.getPitch());mob.setVelocity(Vec3d.ZERO);stepHops++;
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
            int gx=(int)Math.floor(t[0]),gz=(int)Math.floor(t[1]),gy;{int[] f=Arena.floor(battle.world,gx,gz,mob.getX(),mob.getZ());gx=f[0];gz=f[1];gy=f[2];} // one cell: its x, z and floor together (Arena.floor)
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
        boolean onField=fieldPoint!=null&&goal==fieldPoint; // a waypoint of the route field: a floor cell with its own height, taken as it is (like a detour point)
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
        int gx=(int)Math.floor(goal.x),gz=(int)Math.floor(goal.z),gy;{int[] f=onField&&!onDetour?new int[]{gx,gz,(int)Math.floor(goal.y)}:Arena.floor(battle.world,gx,gz,mob.getX(),mob.getZ());gx=f[0];gz=f[1];gy=f[2];} // the goal's cell: x, z and floor together, the same cell for the arrival test and moveTo (Arena.floor)
        if(!onDetour&&!onField&&GravesAndGlory.Battle.horizontalDistance(mob.getPos(),goal)<N.preciseRange()*N.preciseRange()&&Math.abs(mob.getY()-gy)<1.1&&!(Arena.river(gx,gz)&&Arena.ground(gx,gz)<64)&&battle.world.getBlockState(new BlockPos(gx,gy,gz)).isAir()&&battle.world.getBlockState(new BlockPos(gx,gy+1,gz)).isAir()){
            mob.getNavigation().stop();boolean same=gx==(int)Math.floor(goal.x)&&gz==(int)Math.floor(goal.z);mob.getMoveControl().moveTo(same?goal.x:gx+.5,gy,same?goal.z:gz+.5,speed);return;
        }
        Vec3d delta=goal.subtract(mob.getPos()).multiply(1,0,1);
        if(!onDetour&&!onField&&delta.length()>N.waypointDistance())goal=mob.getPos().add(delta.normalize().multiply(N.waypointDistance())); // a detour point is on a whole path: no straight waypoint toward it
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
        int x=(int)Math.floor(goal.x),z=(int)Math.floor(goal.z),y=onDetour||onField?(int)Math.floor(goal.y):Arena.ground(x,z)+1;
        if(!onDetour&&!onField&&!wade&&Arena.river(x,z)&&Arena.ground(x,z)<64){int[] dry=EngineMaps.active()?Arena.dryNear(battle.world,x,z,mob.getX(),Integer.MAX_VALUE):null; // an engine map's river need not run north-south (new Rivergate): the nearest dry column
            if(dry!=null){x=dry[0];z=dry[1];}else x=(int)Arena.riverX(z)+(x>Arena.riverX(z)?11:-11);y=Arena.ground(x,z)+1;}
        // Keep local waypoints out of tree trunks, flagpoles and walls.
        if(!onDetour&&!onField&&!(wade&&Arena.river(x,z))&&(!battle.world.getBlockState(new BlockPos(x,y,z)).isAir()||!battle.world.getBlockState(new BlockPos(x,y+1,z)).isAir())){
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

    /** Each family's swings (attackVariants rows) in the sheet's order, the order the client's ClipLibrary plays them by. */
    private static Map<String,List<Spec.AttackVariantsRow>> variantsByFamily;
    /** The swings of this troop family, in the sheet's order; none for a family the sheet lacks. */
    static List<Spec.AttackVariantsRow> variants(String family){
        if(variantsByFamily==null){var m=new HashMap<String,List<Spec.AttackVariantsRow>>();for(var r:Spec.ATTACKVARIANTS)m.computeIfAbsent(r.family(),k->new ArrayList<>()).add(r);variantsByFamily=m;}
        return variantsByFamily.getOrDefault(family,List.of());
    }
    /** Melee swings begun, blows landed on their impact frame and blows that missed there (strikeLands false), since the server started: the live checks read them. */
    static int swings,strikesLanded,strikesMissed;
    /** Swings refused because the troop's last blow is still pending (Tactics.swing; the attack gate already waits for it, so any is a caller's slip). */
    static int swingsRefused;
    /** The attack a troop's beat takes: a melee blow lands on its swing's impact frame (swing, land); a shot or a spell goes now, with its sound. */
    static void shoot(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,LivingEntity enemy,long time,boolean ranged){
        if(swing(battle,u,enemy,time))return;
        BattleEffects.strike(battle,u,enemy);
        boolean started=Abilities.attack(battle,u,enemy,time);
        if(ranged&&started)Audio.shot(battle,u); // a shot refused (a shell too close, a healer) makes no sound; a caster's spell or a bowstring (AudioCues.MAGIC_CAST, BOW_SHOOT)
    }
    /**
     * Kort Q70 (2026-11-02): a bow troop's late shot, its loose ticks (Projectiles.looseTicks) after the beat that took its cooldown. It leaves
     * at the target the beat chose, as that beat would have shot it, unless the troop is stunned now or the target has fallen or left the world:
     * then the shot is lost (as a melee blow is, BattleMath.strikeLands) and the cooldown stands.
     */
    static void looseLate(GravesAndGlory.Battle battle,GravesAndGlory.Unit u,long time){
        LivingEntity t=u.looseTarget;u.looseAt=-1;u.looseTarget=null;
        if(t==null||!u.mob.isAlive()||time<u.stunnedUntil||!t.isAlive()||t.isRemoved()||t.getWorld()!=battle.world||t instanceof net.minecraft.entity.player.PlayerEntity p&&p.isSpectator()||!u.mob.canSee(t)){looseLost++;u.nextAttack=Math.min(u.nextAttack,time);return;} // the lost shot's cooldown is given back // nor through a wall the target stepped behind since the beat
        shoot(battle,u,t,time,true);
    }
    /** Late shots lost (looseLate) since the server started. */
    static int looseLost;
    /** Whether a troop's shot is announced a beat early (Projectiles.drawAhead), so its clip's wind-up plays before the loose instead of fading in late: a bow's, a crossbow's, a thrower's or a bolt-shooting caster's (the Seer's; Kort 2026-11-06). The mage's mend is announced in Abilities.mend on its own cooldown; the blight and mist spells make no attack event (cosmetic). */
    static boolean announcesAhead(GravesAndGlory.Unit u){if(u.spec.magic())return u.spec.ranged()&&"caster".equals(u.spec.family())&&Abilities.shoots(u);return u.spec.ranged()&&("bow".equals(u.spec.family())||"crossbow".equals(u.spec.family())||"thrower".equals(u.spec.family()));}
    /** Whether a troop's shot draws ahead with Kort Q70's loose and hold seeds (Projectiles.looseTicks, hold): a bow's, whose clip draws for its 10 impact ticks before the loose (R110 Plan B). */
    static boolean drawsAhead(GravesAndGlory.Unit u){return u.spec.ranged()&&!u.spec.magic()&&"bow".equals(u.spec.family());}
    /** Whether this troop has a blow pending (Battle.strikes): one at most, so its next swing waits for it. */
    static boolean pending(GravesAndGlory.Battle b,GravesAndGlory.Unit u){if(u.looseAt>=0)return true;for(var s:b.strikes)if(s.attacker()==u)return true;return false;}
    /**
     * A melee troop's swing (its cooldown already taken and its arm swung): the swing it plays (never the one before, BattleMath.pickVariant), its golem or ravager
     * swing shown now, and its blow queued for the impact frame, that swing's impactTicks on (clamped to land before the next swing can start,
     * BattleMath.impactDelay). Its swing is drawn from the battle's own rolls (Battle.rolls), and its Charge is judged now (Combat.charge, Ruling R29)
     * and carried by the blow. A troop with a blow still pending swings no second one (the call does nothing: swingsRefused). False for a troop whose
     * attack is not a melee blow (Abilities.melee) or whose family has no swings: its attack goes now.
     */
    static boolean swing(GravesAndGlory.Battle b,GravesAndGlory.Unit u,LivingEntity enemy,long time){
        if(!Abilities.melee(u))return false;
        var rows=variants(u.spec.family());if(rows.isEmpty())return false;
        if(pending(b,u)){swingsRefused++;return true;}
        int variant=BattleMath.pickVariant(rows.size(),u.lastVariant,b.rolls.nextLong());
        int delay=BattleMath.impactDelay(rows.get(variant).impactTicks(),u.nextAttack-time);
        u.lastVariant=variant;u.catchVariant=variant;u.swungAt=time;u.swingTick=u.mob.getWorld().getTime();swings++;
        MotionEvents.sendAttack(u,variant,u.swingTick); // attack_v1: the swing Battle picked, from the world tick it begins on (time is swungAt)
        Abilities.animate(b,u);
        var strike=new GravesAndGlory.Battle.PendingStrike(u,enemy,variant,time,time+delay,Combat.charge(b,u,true));
        if(delay>0)b.strikes.add(strike);else land(b,strike);
        return true;
    }
    /** Lands every pending blow whose impact frame has come (Battle.tick, after the shots and before the troops act). */
    static void land(GravesAndGlory.Battle b){
        if(b.strikes.isEmpty())return;
        List<GravesAndGlory.Battle.PendingStrike> due=new ArrayList<>();
        b.strikes.removeIf(s->{if(s.landsAt()>b.time)return false;due.add(s);return true;});
        for(var s:due)land(b,s);
    }
    /**
     * One blow on its impact frame: its flash and cue and its hit (Combat.resolve, with the on-hit abilities, under its swing's Charge) if
     * BattleMath.strikeLands: the attacker alive and not stunned since this blow's swing (PendingStrike.swungAt), the target alive (a commander struck down, a spectator awaiting his respawn, is not) and within the attacker's reach
     * plus meleeReachGrace. Otherwise the blow misses; the swing's cooldown stands either way.
     */
    static void land(GravesAndGlory.Battle b,GravesAndGlory.Battle.PendingStrike s){
        var u=s.attacker();LivingEntity target=s.target();
        boolean attackerAlive=u.mob.isAlive()&&!u.mob.isRemoved();
        boolean targetAlive=target.isAlive()&&!target.isRemoved()&&!(target instanceof ServerPlayerEntity p&&(p.isSpectator()||b.respawns.containsKey(p.getUuid())));
        if(!BattleMath.strikeLands(attackerAlive,u.stunnedUntil>s.swungAt(),targetAlive,u.mob.squaredDistanceTo(target),Abilities.range(b,u),Spec.COMBATDISCIPLINE[0].meleeReachGrace())){strikesMissed++;return;}
        strikesLanded++;
        BattleEffects.strike(b,u,target);
        u.blow=s.charge();u.blowVariant=s.variant();
        try{Abilities.land(b,u,target);}finally{u.blow=null;u.blowVariant=-1;}
        var a=b.assignment(u); // a player's Charge: the group's first landed blow turns it to Attack (hunt); its chargedAt stays for the impact
        if(a!=null&&a.order.equals("charge")&&a.chargeDir!=null&&Combat.landedNow(b,u))become(u,a,"attack",b.time); // landed: damage() took it (Task 6 review I1), so a blow that did not land keeps the Charge and its crash
    }
}
