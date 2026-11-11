package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Unshipped (test tools, never in a player jar): the AI quality metrics and the "why" log of the bot simulation (BotSimulation, report docs/reports/ai-audit-and-design-2026-11-09.md sections 4.5 and 7.8).
 *  Writes botsim/ai-metrics.csv (one row per bot army and one per team, per match) and botsim/decisions.csv (one row per bot commander decision, from Campaign.decisionListener).
 *  It only reads the game. Metrics are per troop, not per group centre: refills walking in from base drag a group's centre away from where its troops are.
 *  Definitions (a sample is one second of one living, non-temporary troop of a bot army, from its second read on):
 *   - idle (idleShare): no fight (AiState.Read.fighting) and no progress toward its order spot, except holdIdle: idle on Hold (or arrived) standing at a capture point its own side holds or at a mode objective, which is the order working.
 *     idleShare and holdIdleShare are disjoint; together they are all idle.
 *   - idleMaxGroupS: the longest run, in seconds, of a group with at least 90 percent of its sampled troops in (non-hold) idle.
 *   - objective: near a capture point of a mode that plays them or a mode objective (Convoy wagon and the road ahead, Siege stage target, Survival shrine), or closing on the nearest one; not while retreating. The group's own target does not count.
 *   - blind pick: a troop's locked target changed to a new one that its mob cannot see (LivingEntity.canSee) at the end of the tick of the change.
 *   - trickle: a reinforcement (first seen over 3 s into the round) at the moment it first fights, with no living friendly troop within 8 blocks.
 *   - wasted cast: a cooldown spent (Unit.abilityAt or the level-3 store rising) with nothing to use it on: an enemy-target row with no living enemy within the caster's reach plus the row's radius, an
 *     ally-target row with no ally in its radius; a blight or mist is judged at its burst, by whether an enemy stood in the radius at the last tick before the burst.
 *   - stuck over 30 s: a troop that should walk (not fighting, not at its spot, no arrival) makes no progress for 30 s running; counted once per run.
 *  Nothing here changes a tick of the game except through its own time, which BotSimulation subtracts from the probe's tick times. */
final class AiMetrics {
    static final String[] COLUMNS={"match","map","mode","scope","id","team","banner","troopSeconds","idleShare","holdIdleShare","fightShare","objectiveShare","idleMaxGroupS","blindPicks","picks","trickleAlone","trickleTotal","trickleIndex",
        "wastedCasts","casts","stuck30","retreats","commanderSwitches","decisions","troopSwitches","unspentGold","tickMeanMs","tickP99Ms"};
    static final String[] DECISION_COLUMNS={"match","second","army","team","tier","order","formation","targetName","targetX","targetZ","living","full","retreatTest","retreating","scores","charge","lane"};
    static final String[] SEATS={"dawn","dusk"};

    static final class Acc {
        long troopSec,idleSec,holdIdleSec,fightSec,objSec;int picks,blind,switches,trickleAlone,trickleTotal,casts,wasted,stuck30,retreats,cmdSwitches,decisions,gold,idleMax;
        final Map<String,Integer> groupRun=new HashMap<>();
        String lastTarget="";boolean retreating;
    }
    /** Per-troop memory for the per-tick reads. */
    static final class UnitMem {LivingEntity target;long abilityAt,secondAt;Vec3d spell;boolean spellHit;}

    final AiState state=new AiState();
    Path metricsCsv,decisionsCsv;
    List<GravesAndGlory.Squad> armies=List.of();final Map<GravesAndGlory.Squad,Integer> index=new IdentityHashMap<>();
    Acc[] acc=new Acc[0];final Map<GravesAndGlory.Unit,UnitMem> mem=new IdentityHashMap<>();
    final StringBuilder decisionRows=new StringBuilder();
    int match;String mode="";
    /** Nanoseconds this tool spent in the tick being timed (BotSimulation subtracts them from the probe's tick time). */
    long ownNanos;

    void open(Path dir){
        metricsCsv=dir.resolve("ai-metrics.csv");decisionsCsv=dir.resolve("decisions.csv");
        BotSimulation.append(metricsCsv,String.join(",",COLUMNS)+"\n");
        BotSimulation.append(decisionsCsv,String.join(",",DECISION_COLUMNS)+"\n");
    }
    void reset(List<GravesAndGlory.Squad> armies,int match,String mode){
        this.armies=List.copyOf(armies);this.match=match;this.mode=mode;index.clear();acc=new Acc[armies.size()];
        for(int i=0;i<armies.size();i++){index.put(armies.get(i),i);acc[i]=new Acc();}
        mem.clear();state.reset();decisionRows.setLength(0);
    }

    // ---- every tick -----------------------------------------------------------------------------------------------------------------------

    void tick(GravesAndGlory.Battle b){
        long t0=System.nanoTime();
        for(var u:b.units){
            if(!u.mob.isAlive()||u.temporary)continue;Integer i=index.get(u.squad);if(i==null)continue;var a=acc[i];var m=mem.computeIfAbsent(u,k->new UnitMem());
            var t=u.lockedTarget;
            if(t!=m.target){
                if(t!=null){a.picks++;if(!u.mob.canSee(t))a.blind++;if(m.target!=null&&m.target.isAlive())a.switches++;}
                m.target=t;
            }
            casts(b,u,m,a);
        }
        for(int i=0;i<armies.size();i++){var s=armies.get(i);var a=acc[i];a.gold=s.gold;
            boolean r=b.campaign.botRetreating.contains(s.owner);if(r&&!a.retreating)a.retreats++;a.retreating=r;}
        ownNanos+=System.nanoTime()-t0;
    }
    void casts(GravesAndGlory.Battle b,GravesAndGlory.Unit u,UnitMem m,Acc a){
        long now=b.time;
        if(u.abilityAt>m.abilityAt&&u.abilityAt>now)judge(b,u,Abilities.row(u),a);
        m.abilityAt=u.abilityAt;
        if(u.level>=3){Long s=Abilities.SECOND_AT.get(u);if(s!=null){if(s>m.secondAt&&s>now)judge(b,u,Abilities.second(u),a);m.secondAt=s;}}
        if(u.spellCenter!=null){m.spell=u.spellCenter;var row=Abilities.row(u);double radius=row.radius()>0?row.radius():Spec.COMBATDISCIPLINE[0].spellRadius();m.spellHit=enemyWithin(b,u.squad.team,u.spellCenter,radius);}
        else if(m.spell!=null){m.spell=null;if(now>=u.stunnedUntil){a.casts++;if(!m.spellHit)a.wasted++;}}
    }
    /** One cast at the tick the cooldown was spent. A blight or mist is counted at its burst instead (casts). */
    void judge(GravesAndGlory.Battle b,GravesAndGlory.Unit u,Spec.AbilitiesRow row,Acc a){
        if(row.mechanic().equals("blight")||row.mechanic().equals("mist"))return;
        a.casts++;
        boolean used;
        if(row.target().equals("allies")){
            double r=row.radius()>0?row.radius():Spec.COMBATDISCIPLINE[0].healRadius();used=false;
            for(var v:b.units)if(v!=u&&v.squad.team==u.squad.team&&v.mob.isAlive()&&u.mob.squaredDistanceTo(v.mob)<=r*r){used=true;break;}
        }else{
            double r=Abilities.range(b,u)+Math.max(0,row.radius());used=false;
            for(var v:b.units)if(v.squad.team!=u.squad.team&&v.mob.isAlive()&&u.mob.squaredDistanceTo(v.mob)<=r*r){used=true;break;}
        }
        if(!used)a.wasted++;
    }
    static boolean enemyWithin(GravesAndGlory.Battle b,int team,Vec3d at,double radius){
        for(var v:b.units)if(v.squad.team!=team&&v.mob.isAlive()&&v.mob.squaredDistanceTo(at)<=radius*radius)return true;
        return false;
    }

    // ---- once a second --------------------------------------------------------------------------------------------------------------------

    void second(GravesAndGlory.Battle b){
        long t0=System.nanoTime();
        var alive=new ArrayList<GravesAndGlory.Unit>();Map<String,int[]> groups=new HashMap<>();
        for(var u:b.units){
            if(!u.mob.isAlive()||u.temporary)continue;Integer i=index.get(u.squad);if(i==null)continue;alive.add(u);var a=acc[i];
            var r=state.read(b,u,b.time);var m=state.memory(u);
            if(r.fighting&&m.refill&&!m.fought){m.fought=true;a.trickleTotal++;if(!AiState.friendNear(b,u))a.trickleAlone++;}
            if(!r.valid)continue;
            a.troopSec++;
            if(r.fighting)a.fightSec++;
            boolean idle=AiState.idle(r),holdIdle=idle&&r.holdAtOwn(),otherIdle=idle&&!holdIdle;
            if(holdIdle)a.holdIdleSec++;else if(otherIdle)a.idleSec++;
            if(r.objective())a.objSec++;
            if(m.noMoveTicks>=600){if(m.stuckCounted==0){a.stuck30++;m.stuckCounted=1;}}else if(m.noMoveTicks==0)m.stuckCounted=0;
            var g=groups.computeIfAbsent(i+"/"+r.group,k->new int[2]);g[0]++;if(otherIdle)g[1]++;
        }
        state.retain(alive);mem.keySet().retainAll(Set.copyOf(alive));
        for(int i=0;i<acc.length;i++){var a=acc[i];
            for(var group:Spec.GROUPS){String g=group.id();var c=groups.get(i+"/"+g);
                if(c==null||c[0]==0){a.groupRun.remove(g);continue;}
                if(c[1]*10>=9*c[0]){int run=a.groupRun.merge(g,1,Integer::sum);a.idleMax=Math.max(a.idleMax,run);}else a.groupRun.remove(g);}}
        if(decisionRows.length()>0){BotSimulation.append(decisionsCsv,decisionRows.toString());decisionRows.setLength(0);}
        ownNanos+=System.nanoTime()-t0;
    }

    // ---- the why log ----------------------------------------------------------------------------------------------------------------------

    /** Campaign.decisionListener: one decisions.csv row per bot commander decision, after the decision is made. Scores are semicolon-separated, never commas. */
    void decision(Campaign c,GravesAndGlory.Squad s,Spec.BotCommandRow tier,Vec3d center,int living,int full,Collection<GravesAndGlory.Battle.Sight> seen,int enemies,boolean couldCharge,
                  GravesAndGlory.Capture choice,int hold,boolean retreating,String wasOrder,Vec3d wasTarget){
        Integer i=index.get(s);if(i==null)return;
        long t0=System.nanoTime();
        var b=c.b;var a=acc[i];
        String targetName=s.target==null?"":AiState.spotName(b,s.target);
        a.decisions++;
        if(!a.lastTarget.isEmpty()&&!a.lastTarget.equals(targetName))a.cmdSwitches++;
        a.lastTarget=targetName;
        StringBuilder scores=new StringBuilder(),lane=new StringBuilder();int count=b.points.size();
        if(!tier.flankLeastDefended()){
            int laneIndex=BattleMath.laneTarget(s.lane,s.team,count);lane.append("lane=p").append(laneIndex);
            lane.append(";guard=").append(hold);
            scores.append(c.botWhy); // Campaign.botDecide's point costs (BattleMath.pointValue), written while a listener is set
        }else{
            lane.append("flank=true;hold=").append(hold);
            for(int p=0;p<count;p++){var pt=b.points.get(p);int defenders=0;
                for(var sight:seen)if(Math.hypot(sight.x()-pt.spec.x(),sight.z()-pt.spec.z())<=Campaign.NEAR_RADIUS_FACTOR*pt.spec.radius())defenders++;
                if(p>0)scores.append(';');
                scores.append(pt.spec.name().replace(',',' ')).append(':').append(String.format(Locale.ROOT,"owner%d+defenders%d",pt.owner,defenders));}
        }
        String charge="";
        if(!retreating&&!s.order.equals("hold")){
            double nearest=Double.MAX_VALUE;long near=0;for(var sight:seen){double d=Math.hypot(sight.x()-center.x,sight.z()-center.z);nearest=Math.min(nearest,d);if(d<=Campaign.NEAR_RADIUS_FACTOR*tier.chargeDistance())near++;}
            boolean timed=tier.chargeDistance()>0?near>0&&BattleMath.timedCharge(nearest,living,near,tier.chargeRatio(),tier.chargeDistance()):enemies>0&&BattleMath.timedCharge(0,living,enemies,tier.chargeRatio(),0);
            charge=String.format(Locale.ROOT,"nearestSeen=%s;near=%d;enemies=%d;ratio=%.2f;distance=%.0f;timed=%b;couldCharge=%b",nearest==Double.MAX_VALUE?"none":String.format(Locale.ROOT,"%.0f",nearest),near,enemies,tier.chargeRatio(),tier.chargeDistance(),timed,couldCharge);
        }
        String retreatTest=String.format(Locale.ROOT,"living%d<=full%d*%.2f:%b;rally>=%.2f:%b",living,full,tier.retreatShare(),living<=full*tier.retreatShare(),tier.rallyShare(),living>=full*tier.rallyShare());
        decisionRows.append(String.join(",",String.valueOf(match+1),String.valueOf((b.time-b.roundStarted)/20),String.valueOf(i),SEATS[s.team],tier.id(),s.order,s.formation,targetName.replace(',',':'),
            s.target==null?"":String.format(Locale.ROOT,"%.1f",s.target.x),s.target==null?"":String.format(Locale.ROOT,"%.1f",s.target.z),String.valueOf(living),String.valueOf(full),retreatTest,String.valueOf(retreating),scores.toString(),charge,lane.toString())).append('\n');
        ownNanos+=System.nanoTime()-t0;
    }

    // ---- the rows -------------------------------------------------------------------------------------------------------------------------

    /** Appends the match's rows: one per bot army, then one per team. tickMeanMs and tickP99Ms are the server tick bodies of the round (blank without GRAVES_BOTSIM_PROBE). */
    void write(String map,String[] banners,String tickMean,String tickP99){
        StringBuilder out=new StringBuilder();
        for(int i=0;i<acc.length;i++)out.append(row(map,"army",String.valueOf(i),armies.get(i).team,banners[armies.get(i).team],List.of(acc[i]),tickMean,tickP99)).append('\n');
        for(int t=0;t<2;t++){List<Acc> mine=new ArrayList<>();for(int i=0;i<acc.length;i++)if(armies.get(i).team==t)mine.add(acc[i]);
            if(!mine.isEmpty())out.append(row(map,"team",SEATS[t],t,banners[t],mine,tickMean,tickP99)).append('\n');}
        BotSimulation.append(metricsCsv,out.toString());
    }
    String row(String map,String scope,String id,int team,String banner,List<Acc> list,String tickMean,String tickP99){
        long troop=0,idle=0,hold=0,fight=0,obj=0;int blind=0,picks=0,alone=0,total=0,wasted=0,casts=0,stuck=0,retreats=0,cmd=0,decisions=0,switches=0,gold=0,idleMax=0;
        for(var a:list){troop+=a.troopSec;idle+=a.idleSec;hold+=a.holdIdleSec;fight+=a.fightSec;obj+=a.objSec;blind+=a.blind;picks+=a.picks;alone+=a.trickleAlone;total+=a.trickleTotal;wasted+=a.wasted;casts+=a.casts;
            stuck+=a.stuck30;retreats+=a.retreats;cmd+=a.cmdSwitches;decisions+=a.decisions;switches+=a.switches;gold+=a.gold;idleMax=Math.max(idleMax,a.idleMax);}
        return String.join(",",String.valueOf(match+1),map,mode,scope,id,SEATS[team],banner,String.valueOf(troop),share(idle,troop),share(hold,troop),share(fight,troop),share(obj,troop),String.valueOf(idleMax),String.valueOf(blind),String.valueOf(picks),
            String.valueOf(alone),String.valueOf(total),share(alone,total),String.valueOf(wasted),String.valueOf(casts),String.valueOf(stuck),String.valueOf(retreats),String.valueOf(cmd),String.valueOf(decisions),String.valueOf(switches),String.valueOf(gold),tickMean,tickP99);
    }
    static String share(long part,long whole){return whole==0?"":String.format(Locale.ROOT,"%.4f",part/(double)whole);}
}
