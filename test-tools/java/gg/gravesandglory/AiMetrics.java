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
    /** Per-troop memory for the per-tick reads. ap*: the approach being timed (troop metrics); trail: its place every TRAIL_SECONDS, newest last. */
    static final class UnitMem {LivingEntity target;long abilityAt,secondAt;Vec3d spell;boolean spellHit;
        LivingEntity apSeen,apTarget;long apStart;double apD0;int apTicks,apCrawl;final Vec3d[] trail=new Vec3d[3];final boolean[] trailIdle=new boolean[3];}
    // ---- troop metrics (fixes of 2026-11-11: Attack creep, archer homing, pacing at spawn), botsim/troop-metrics.csv, one row per team per match ----
    //  - arrows: archer shots (Projectiles.ARROWS_*) fired, struck their target, lost (target gone before landing), by range bin at the loose; hit rate = hit / (fired - lost).
    //  - approach: a melee troop of a bot army on Attack or Charge picks an enemy more than its reach + 2 away; timed until it stands within reach + 0.5 of it
    //    (contact). Dropped if the target or the order changes first. approachSpeed = closed distance / time; approachCrawl = share of its ticks under the crawl.
    //  - spawnReversals: a troop within SPAWN_RADIUS of its side's field base, with no target, whose 2 s legs (each >= 1.5 blocks) turn back on themselves (cos < -0.5).
    //  - baseYawFlips: a bot decision that turns an army's facing by more than 90 degrees while its centre stands within SPAWN_RADIUS of its field base.
    static final String[] TROOP_COLUMNS={"match","map","mode","team","fired0","hit0","lost0","fired8","hit8","lost8","fired16","hit16","lost16","fired24","hit24","lost24",
        "approaches","approachMeanS","approachMedianS","approachSpeed","approachCrawl","spawnReversals","spawnReversalsOpening","baseYawFlips"};
    static final double SPAWN_RADIUS=40;static final int TRAIL_SECONDS=2;static final long OPENING_TICKS=400; // spawnReversalsOpening: those in the round's first 20 s
    static final class TeamAcc {final long[][] arrows0=new long[3][];final List<Double> apSeconds=new ArrayList<>();double apDist;long apTicks,apCrawl;int reversals,reversalsOpening,yawFlips;}
    TeamAcc[] teams={new TeamAcc(),new TeamAcc()};final StringBuilder pacingRows=new StringBuilder(),blindRows=new StringBuilder();Path pacingCsv,blindCsv;final Map<GravesAndGlory.Squad,Float> lastYaw=new IdentityHashMap<>();Path troopCsv;
    static long[] row(long[][] counter,int team){return counter[team].clone();}

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
        troopCsv=dir.resolve("troop-metrics.csv");BotSimulation.append(troopCsv,String.join(",",TROOP_COLUMNS)+"\n");
        blindCsv=dir.resolve("blind.csv");BotSimulation.append(blindCsv,"match,second,team,group,ranged,order,kind,distance,cacheAge,speed\n"); // one row per blind pick: what was picked, and how old its sight cache (Tactics.sees) was
        pacingCsv=dir.resolve("pacing.csv");BotSimulation.append(pacingCsv,"match,second,team,army,group,slot,x0,z0,x1,z1,x2,z2,order,formation,targetX,targetZ,yaw,muster,retreating,entity\n"); // one row per spawnReversals event
    }
    void reset(List<GravesAndGlory.Squad> armies,int match,String mode){
        this.armies=List.copyOf(armies);this.match=match;this.mode=mode;index.clear();acc=new Acc[armies.size()];
        for(int i=0;i<armies.size();i++){index.put(armies.get(i),i);acc[i]=new Acc();}
        mem.clear();state.reset();decisionRows.setLength(0);lastYaw.clear();
        for(int t=0;t<2;t++){teams[t]=new TeamAcc();teams[t].arrows0[0]=row(Projectiles.ARROWS_FIRED,t);teams[t].arrows0[1]=row(Projectiles.ARROWS_HIT,t);teams[t].arrows0[2]=row(Projectiles.ARROWS_LOST,t);}
    }
    /** The approach of a melee troop on Attack or Charge (TROOP_COLUMNS), every tick. */
    void approach(GravesAndGlory.Battle b,GravesAndGlory.Unit u,UnitMem m){
        if(u.spec.ranged()||u.squad.team<0||u.squad.team>1)return;
        String o=b.assignment(u).order;boolean attack=o.equals("attack")||o.equals("charge");
        var t=u.lockedTarget;double reach=Abilities.range(b,u);
        if(t!=m.apSeen){
            m.apSeen=t;m.apTarget=null;
            if(attack&&t!=null&&t.isAlive()){double d=Math.sqrt(u.mob.squaredDistanceTo(t));if(d>reach+2){m.apTarget=t;m.apStart=b.time;m.apD0=d;m.apTicks=0;m.apCrawl=0;}}
            return;
        }
        if(m.apTarget==null)return;
        if(!attack||!t.isAlive()){m.apTarget=null;return;}
        m.apTicks++;if(OrderIntakeProbeBehaviour.crawling(u))m.apCrawl++;
        double d=Math.sqrt(u.mob.squaredDistanceTo(t));
        if(d<=reach+.5){var ta=teams[u.squad.team];double s=(b.time-m.apStart)/20.0;ta.apSeconds.add(s);ta.apDist+=m.apD0-d;ta.apTicks+=m.apTicks;ta.apCrawl+=m.apCrawl;m.apTarget=null;}
    }
    /** Pacing at spawn (TROOP_COLUMNS), every TRAIL_SECONDS. */
    void pacing(GravesAndGlory.Battle b,GravesAndGlory.Unit u,UnitMem m){
        if(u.squad.team<0||u.squad.team>1)return;
        System.arraycopy(m.trail,1,m.trail,0,2);System.arraycopy(m.trailIdle,1,m.trailIdle,0,2);
        m.trail[2]=u.mob.getPos();m.trailIdle[2]=u.lockedTarget==null;
        if(m.trail[0]==null||!m.trailIdle[0]||!m.trailIdle[1]||!m.trailIdle[2])return;
        Vec3d base=b.fieldBase(u.squad.team);if(Math.hypot(m.trail[1].x-base.x,m.trail[1].z-base.z)>SPAWN_RADIUS)return;
        double ax=m.trail[1].x-m.trail[0].x,az=m.trail[1].z-m.trail[0].z,bx=m.trail[2].x-m.trail[1].x,bz=m.trail[2].z-m.trail[1].z,la=Math.hypot(ax,az),lb=Math.hypot(bx,bz);
        if(la>=1.5&&lb>=1.5&&(ax*bx+az*bz)/(la*lb)<-.5){teams[u.squad.team].reversals++;if(b.time-b.roundStarted<OPENING_TICKS)teams[u.squad.team].reversalsOpening++;
            var a=b.assignment(u);Integer army=index.get(u.squad);
            pacingRows.append(String.format(Locale.ROOT,"%d,%d,%s,%d,%s,%d,%.1f,%.1f,%.1f,%.1f,%.1f,%.1f,%s,%s,%.1f,%.1f,%.0f,%b,%b,%d%n",match+1,(b.time-b.roundStarted)/20,SEATS[u.squad.team],army==null?-1:army,u.spec.group(),u.slot,
                m.trail[0].x,m.trail[0].z,m.trail[1].x,m.trail[1].z,m.trail[2].x,m.trail[2].z,a.order,a.formation,a.target==null?0:a.target.x,a.target==null?0:a.target.z,a.yaw,u.musterSince>=0,b.campaign.botRetreating.contains(u.squad.owner),u.mob.getId()));}
    }

    // ---- every tick -----------------------------------------------------------------------------------------------------------------------

    /** A blind pick's row (blind.csv): the target's kind (troop, player, structure), its distance, how many ticks ago the picker's sight cache last saw
     *  it (Tactics.sees; -1 never), and the picker's speed (blocks a second, its last tick). */
    void blind(GravesAndGlory.Battle b,GravesAndGlory.Unit u,net.minecraft.entity.LivingEntity t){
        String kind=t instanceof net.minecraft.server.network.ServerPlayerEntity?"player":b.units.stream().anyMatch(o->o.mob==t)?"troop":"structure";
        long[] r=u.sight.get(t.getUuid());long age=r==null||r[1]<0?-1:b.time-r[1];
        double v=Math.hypot(u.mob.getX()-u.mob.prevX,u.mob.getZ()-u.mob.prevZ)*20;
        blindRows.append(String.format(Locale.ROOT,"%d,%d,%s,%s,%b,%s,%s,%.1f,%d,%.2f%n",match+1,(b.time-b.roundStarted)/20,SEATS[u.squad.team],u.spec.group(),u.spec.ranged(),b.assignment(u).order,kind,Math.sqrt(u.mob.squaredDistanceTo(t)),age,v));
    }

    void tick(GravesAndGlory.Battle b){
        long t0=System.nanoTime();
        for(var u:b.units){
            if(!u.mob.isAlive()||u.temporary)continue;Integer i=index.get(u.squad);if(i==null)continue;var a=acc[i];var m=mem.computeIfAbsent(u,k->new UnitMem());
            var t=u.lockedTarget;
            if(t!=m.target){
                if(t!=null){a.picks++;if(!u.mob.canSee(t)){a.blind++;blind(b,u,t);}if(m.target!=null&&m.target.isAlive())a.switches++;}
                m.target=t;
            }
            casts(b,u,m,a);
            approach(b,u,m);
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
            if((b.time-b.roundStarted)/20%TRAIL_SECONDS==0)pacing(b,u,mem.computeIfAbsent(u,k->new UnitMem()));
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
        Float was=lastYaw.put(s,s.yaw);Vec3d base=b.fieldBase(s.team);
        if(was!=null&&s.team>=0&&s.team<2&&BattleMath.yawGap(was,s.yaw)>90&&Math.hypot(center.x-base.x,center.z-base.z)<=SPAWN_RADIUS)teams[s.team].yawFlips++;
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
        StringBuilder troops=new StringBuilder();
        for(int t=0;t<2;t++){var ta=teams[t];troops.append(String.join(",",String.valueOf(match+1),map,mode,SEATS[t]));
            long[] f=row(Projectiles.ARROWS_FIRED,t),h=row(Projectiles.ARROWS_HIT,t),l=row(Projectiles.ARROWS_LOST,t);
            for(int bin=0;bin<Projectiles.RANGE_BINS;bin++)troops.append(',').append(f[bin]-ta.arrows0[0][bin]).append(',').append(h[bin]-ta.arrows0[1][bin]).append(',').append(l[bin]-ta.arrows0[2][bin]);
            var sorted=new ArrayList<>(ta.apSeconds);Collections.sort(sorted);double sum=0;for(double v:sorted)sum+=v;
            troops.append(',').append(sorted.size()).append(',').append(sorted.isEmpty()?"":String.format(Locale.ROOT,"%.2f",sum/sorted.size()))
                .append(',').append(sorted.isEmpty()?"":String.format(Locale.ROOT,"%.2f",sorted.get(sorted.size()/2)))
                .append(',').append(sum<=0?"":String.format(Locale.ROOT,"%.2f",ta.apDist/sum)).append(',').append(ta.apTicks==0?"":String.format(Locale.ROOT,"%.3f",ta.apCrawl/(double)ta.apTicks))
                .append(',').append(ta.reversals).append(',').append(ta.reversalsOpening).append(',').append(ta.yawFlips).append('\n');}
        BotSimulation.append(troopCsv,troops.toString());
        if(blindRows.length()>0){BotSimulation.append(blindCsv,blindRows.toString());blindRows.setLength(0);}
        if(pacingRows.length()>0){BotSimulation.append(pacingCsv,pacingRows.toString());pacingRows.setLength(0);}
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
