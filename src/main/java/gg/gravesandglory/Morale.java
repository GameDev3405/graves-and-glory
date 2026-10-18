package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/** Per-group morale: a badly hurt or flanked group breaks, runs for its base, then regroups and takes its stored order. */
public final class Morale {
    static final Spec.MoraleRow M=Spec.MORALE[0];
    static final String[] GROUPS={"infantry","ranged","support"};
    /** A trait that keeps a group from breaking (Phase 2: Gilded Bulwark, Champion Stand Fast). */
    public interface RoutGuard { boolean holds(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group); }
    public static final CopyOnWriteArrayList<RoutGuard> GUARDS=new CopyOnWriteArrayList<>();
    private Morale(){}

    public static boolean routing(GravesAndGlory.Squad s,String group){return s.routedAt.containsKey(group);}

    /** Full health and full head count per group from the squad's lineup; only ever raised during a round. */
    static void lineup(GravesAndGlory.Battle b,GravesAndGlory.Squad s,List<String> line){
        Map<String,Double> full=new HashMap<>();Map<String,Integer> heads=new HashMap<>();int position=0;
        for(String id:line){if(position++>=GravesAndGlory.R.squadSize()||id.equals("empty")||!b.campaign.unlocked(s,id))continue;
            var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)).findFirst().orElse(null);if(spec==null)continue;
            full.merge(spec.group(),spec.health()*b.campaign.health(s,spec),Double::sum);heads.merge(spec.group(),1,Integer::sum);}
        for(var e:full.entrySet())s.fullHealth.merge(e.getKey(),e.getValue(),Math::max);
        for(var e:heads.entrySet())s.fullCount.merge(e.getKey(),e.getValue(),Math::max);
    }

    /** A troop joined mid-round: the group's ceiling rises to the health actually fielded (so a trained-only group can still rout), and never falls. */
    static void fielded(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group){
        double health=0;int heads=0;for(var u:b.units)if(u.squad==s&&u.spec.group().equals(group)&&u.mob.isAlive()){health+=u.spec.health()*b.campaign.health(s,u.spec);heads++;}
        s.fullHealth.merge(group,health,Math::max);s.fullCount.merge(group,heads,Math::max);
    }

    /** Runs every 20 ticks while the round is live. */
    public static void tick(GravesAndGlory.Battle b){
        for(var s:new ArrayList<>(b.squads.values())){if(!s.seated)continue;
            for(String group:GROUPS){
                List<GravesAndGlory.Unit> members=new ArrayList<>();for(var u:b.units)if(u.squad==s&&u.spec.group().equals(group)&&u.mob.isAlive())members.add(u);
                if(members.isEmpty()){s.routedAt.remove(group);continue;}
                Vec3d centre=centre(members);
                if(routing(s,group)){
                    long elapsed=b.time-s.routedAt.get(group);
                    boolean safe=!enemyWithin(b,s.team,centre,M.routSafeRadius());
                    boolean home=GravesAndGlory.Battle.horizontalDistance(centre,GravesAndGlory.Battle.base(s.team))<=10*10;
                    if(elapsed>=M.routMinSeconds()*20L&&safe||home||elapsed>=M.routMaxSeconds()*20L)rally(b,s,group);
                    continue;
                }
                if(b.time<s.steadyUntil.getOrDefault(group,0L)||!enemyWithin(b,s.team,centre,M.engageCheckRadius()))continue;
                double living=0;for(var u:members)living+=u.mob.getHealth();
                double strength=BattleMath.strength(living,s.fullHealth.getOrDefault(group,0.0));
                if(!BattleMath.shouldRout(strength,flanked(b,s,group,members,centre),M.routShare(),M.flankedRoutShare()))continue;
                boolean held=false;for(var guard:GUARDS)if(guard.holds(b,s,group)){held=true;break;}
                if(held)continue;
                s.routedAt.put(group,b.time);
                // Break contact at once: drop targets and paths so the group turns for home this tick.
                for(var u:members){u.lockedTarget=null;u.spellCenter=null;u.mob.setTarget(null);u.mob.getNavigation().stop();u.nextPath=0;u.stuckTicks=0;}
                BattleEffects.horn(b,s,centre,"rout");
            }
        }
    }
    /** Ends a rout now (or steadies a group that is not routing): immunity starts and the rally horn sounds. */
    public static void rally(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group){
        s.routedAt.remove(group);s.steadyUntil.put(group,b.time+M.regroupImmunitySeconds()*20L);
        for(var u:b.units)if(u.squad==s&&u.spec.group().equals(group)){u.nextPath=0;u.stuckTicks=0;}
        List<GravesAndGlory.Unit> members=new ArrayList<>();for(var u:b.units)if(u.squad==s&&u.spec.group().equals(group)&&u.mob.isAlive())members.add(u);
        BattleEffects.horn(b,s,members.isEmpty()?s.target:centre(members),"rally");
    }
    static Vec3d centre(List<GravesAndGlory.Unit> members){Vec3d sum=Vec3d.ZERO;for(var u:members)sum=sum.add(u.mob.getPos());return sum.multiply(1.0/members.size());}
    /** Any living enemy troop or seated, non-spectating enemy commander within radius (horizontal). */
    static boolean enemyWithin(GravesAndGlory.Battle b,int team,Vec3d centre,double radius){
        double r2=radius*radius;
        for(var u:b.units)if(u.squad.team!=team&&u.mob.isAlive()&&GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(),centre)<=r2)return true;
        for(ServerPlayerEntity p:b.server.getPlayerManager().getPlayerList()){var other=b.squads.get(p.getUuid());
            if(other!=null&&other.seated&&other.team!=team&&p.isAlive()&&!p.isSpectator()&&p.getWorld()==b.world&&GravesAndGlory.Battle.horizontalDistance(p.getPos(),centre)<=r2)return true;}
        return false;
    }
    /** Enemy troops close to the centre that are fighting a member, from outside the group's facing arc. */
    static boolean flanked(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group,List<GravesAndGlory.Unit> members,Vec3d centre){
        Set<Object> mobs=new HashSet<>();for(var u:members)mobs.add(u.mob);
        List<double[]> offsets=new ArrayList<>();double r2=M.flankRadius()*M.flankRadius();
        for(var e:b.units)if(e.squad.team!=s.team&&e.mob.isAlive()&&mobs.contains(e.mob.getTarget())&&GravesAndGlory.Battle.horizontalDistance(e.mob.getPos(),centre)<=r2)offsets.add(new double[]{e.mob.getX()-centre.x,e.mob.getZ()-centre.z});
        if(offsets.size()<M.flankMinAttackers())return false;
        double[] dx=new double[offsets.size()],dz=new double[offsets.size()];for(int i=0;i<dx.length;i++){dx[i]=offsets.get(i)[0];dz[i]=offsets.get(i)[1];}
        return BattleMath.flanked(b.storedAssignment(s,group).yaw,dx,dz,M.flankArcDegrees(),M.flankMinAttackers());
    }
}
