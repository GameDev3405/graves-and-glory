package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import java.util.IdentityHashMap;
import java.util.Map;

/** Unshipped (test tools, never in a player jar): reads what one troop is doing, for the AI quality metrics (AiMetrics) and the dev purpose overlay (AiPurposeOverlay).
 *  It only reads: no call here moves a troop, changes an order or touches the formation cache. The "order spot" is the group's order target (Assignment.target), not the troop's own
 *  slot: Battle.formation keeps held places (heldPlace), so asking it from outside could change what the game does.
 *  One tracker per consumer; read() must be called at a steady cadence (dtTicks), because progress is the drop in distance to the spot since the troop's previous read. */
final class AiState {
    /** A troop within this many blocks of its group's order target counts as at its spot (rows of up to 10 columns and a role depth of up to 9 stand around the target). */
    static final double SPOT_RADIUS=14;
    /** On a mode objective: within this of the group's target (Convoy's wagon, Siege's stage spot), or within a capture point's radius plus POINT_SLACK. */
    static final double OBJECTIVE_RADIUS=16,POINT_SLACK=4;
    /** Progress toward the spot: at least this many blocks per second closer (a walking troop closes 3 to 6). */
    static final double PROGRESS_PER_SECOND=0.8;
    /** Reinforcements are troops spawned later than this many ticks after the round began. */
    static final long REFILL_AFTER_TICKS=60;
    static final double FRIEND_RADIUS=8;
    /** The labels the overlay shows. */
    static final String MARCHING="marching",FIGHTING="fighting",HOLDING="holding",IDLE="idle",STUCK="stuck",RETREATING="retreating",REFILL_WALKING="refill-walking";

    /** Per-troop memory between reads. */
    static final class Mem {
        Vec3d pos;long at=-1;boolean refill,fought;long noMoveTicks;int stuckCounted;
        LivingEntity lastTarget;
    }
    final Map<GravesAndGlory.Unit,Mem> mem=new IdentityHashMap<>();

    /** One troop's reading. valid is false on the first read of a troop (no previous position to compare). */
    static final class Read {
        boolean valid,fighting,progress,atSpot,arrived,hold,retreating,onObjective,refill,stuck;
        String group,order,state,spotName;LivingEntity foe;Vec3d spot;double spotDistance;
        /** Objective: on a mode objective, or moving toward one. */
        boolean objective(){return onObjective||(progress&&!retreating&&!onObjective&&!arrived);}
    }

    void reset(){mem.clear();}
    void forget(GravesAndGlory.Unit u){mem.remove(u);}
    void retain(java.util.Collection<GravesAndGlory.Unit> alive){mem.keySet().retainAll(java.util.Set.copyOf(alive));}

    Read read(GravesAndGlory.Battle b,GravesAndGlory.Unit u,long time){
        var r=new Read();var m=mem.computeIfAbsent(u,k->new Mem());boolean first=m.at<0;
        var mob=u.mob;Vec3d pos=mob.getPos();
        if(first)m.refill=(b.time-mob.age)-b.roundStarted>REFILL_AFTER_TICKS; // spawned (now minus its age in ticks) well after the round began
        var a=b.assignment(u);r.group=u.spec.group();r.order=a.order;
        r.arrived=u.squad.arrived.contains(r.group);r.hold=a.order.equals("hold")||r.arrived;r.retreating=a.order.equals("retreat")&&!r.arrived;
        r.refill=m.refill;
        Vec3d spot=a.target==null?pos:a.target;r.spot=spot;
        r.spotDistance=Math.hypot(pos.x-spot.x,pos.z-spot.z);r.atSpot=r.spotDistance<=SPOT_RADIUS;
        // Fight: the order engages and a living enemy is locked or stands within the order's engage radius (a group that has arrived fights as on Hold).
        String orderId=r.arrived?"hold":a.order;double engage=0;boolean engages=false;
        for(var o:Spec.ORDERS)if(o.id().equals(orderId)){engages=o.engage();engage=o.engageRadius();break;}
        LivingEntity foe=null;
        if(engages){
            var locked=u.lockedTarget;if(locked!=null&&locked.isAlive())foe=locked;
            else{double best=engage*engage;
                for(var other:b.units)if(other.squad.team!=u.squad.team&&other.mob.isAlive()){double d=mob.squaredDistanceTo(other.mob);if(d<best){best=d;foe=other.mob;}}}
        }
        r.foe=foe;r.fighting=foe!=null;
        // Objective: on the group's target (a mode's goal), or on a capture point of a mode that plays them.
        boolean onTarget=!a.order.equals("retreat")&&r.spotDistance<=OBJECTIVE_RADIUS,onPoint=false;
        if(!a.order.equals("retreat")&&b.mode.capturesPoints(b))for(var p:b.points){double reach=p.spec.radius()+POINT_SLACK;if(Math.hypot(pos.x-p.spec.x(),pos.z-p.spec.z())<=reach){onPoint=true;break;}}
        r.onObjective=onTarget||onPoint;
        r.spotName=spotName(b,spot);
        if(!first){
            double dtSeconds=Math.max(1,time-m.at)/20.0;
            double before=Math.hypot(m.pos.x-spot.x,m.pos.z-spot.z);
            r.progress=before-r.spotDistance>=PROGRESS_PER_SECOND*dtSeconds;
            r.valid=true;
            // A troop that should walk (not fighting, not at its spot, not in a held arrival) and makes no progress is waiting or stuck.
            if(!r.fighting&&!r.atSpot&&!r.arrived&&!r.progress)m.noMoveTicks+=Math.max(1,time-m.at);else m.noMoveTicks=0;
        }
        r.stuck=m.noMoveTicks>=Spec.NAVIGATION[0].stuckSeconds()*20L;
        if(r.fighting)m.lastTarget=foe;
        // The label.
        if(r.retreating)r.state=RETREATING;
        else if(r.stuck)r.state=STUCK;
        else if(r.fighting)r.state=FIGHTING;
        else if(r.refill&&!r.atSpot&&(r.progress||first))r.state=REFILL_WALKING;
        else if(r.progress||(first&&!r.atSpot))r.state=MARCHING;
        else if(r.hold&&r.atSpot)r.state=HOLDING;
        else r.state=IDLE;
        m.pos=pos;m.at=time;
        return r;
    }

    /** Seconds' worth of "no progress and no fight" for the metric: a troop that is not fighting and makes no progress toward its spot (valid reads only). */
    static boolean idle(Read r){return r.valid&&!r.fighting&&!r.progress;}
    /** The memory of a troop, created if it was not read yet. */
    Mem memory(GravesAndGlory.Unit u){return mem.computeIfAbsent(u,k->new Mem());}

    /** A capture point's name when the spot lies on one, else "x,z". */
    static String spotName(GravesAndGlory.Battle b,Vec3d spot){
        for(var p:b.points)if(Math.hypot(spot.x-p.spec.x(),spot.z-p.spec.z())<=2)return p.spec.name();
        return Math.round(spot.x)+","+Math.round(spot.z);
    }
    /** Whether a living friendly troop (not the troop itself, not a temporary) stands within FRIEND_RADIUS blocks of it. */
    static boolean friendNear(GravesAndGlory.Battle b,GravesAndGlory.Unit u){
        for(var other:b.units)if(other!=u&&other.squad.team==u.squad.team&&other.mob.isAlive()&&!other.temporary&&u.mob.squaredDistanceTo(other.mob)<=FRIEND_RADIUS*FRIEND_RADIUS)return true;
        return false;
    }
}
