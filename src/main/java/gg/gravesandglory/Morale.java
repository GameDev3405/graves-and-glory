package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-group standing. No group ever breaks (the no-routing design): a group keeps its order, its formation and its fight until every member is
 * dead or its commander gives a new one. What this class keeps is the three groups the cards show, their lineup health (the ceiling of the
 * strength bar) and the LAST STAND: a group whose strength is at or below combatDiscipline.lastStandShare while an enemy stands within
 * lastStandRadius of its centre. The troops do nothing different for it; it is information for the commander (the card and the feed).
 */
public final class Morale {
    static final Spec.CombatDisciplineRow D=Spec.COMBATDISCIPLINE[0];
    static final String[] GROUPS={"infantry","ranged","support"};
    private Morale(){}

    /** A feed line the last stand would send: the army's owner, the group and the battle time. The HUD task joins the feed call here; the harness reads the list. */
    public record Line(UUID owner,String group,long time){}
    /** Every last-stand line recorded since the round started, in order, a player's army's only (Battle.startRound clears it; the closing HUD task sends them to the feed). */
    public static final CopyOnWriteArrayList<Line> lastStandLines=new CopyOnWriteArrayList<>();
    /** A new round: the lines of the last one go. */
    static void startRound(){lastStandLines.clear();}
    /** The display name of a sheet id (a group, an order, a rarity): its first letter in upper case. The server (the last-stand notice) and the client (CommandMode, ArmyCards) read it here. */
    public static String label(String id){return Character.toUpperCase(id.charAt(0))+id.substring(1);}

    /** Whether the group stands its last stand now (tick keeps Squad.lastStand). */
    public static boolean lastStand(GravesAndGlory.Squad s,String group){return s.lastStand.contains(group);}

    /** Full health and full head count per group from the squad's lineup; only ever raised during a round. */
    static void lineup(GravesAndGlory.Battle b,GravesAndGlory.Squad s,List<String> line){
        Map<String,Double> full=new HashMap<>();Map<String,Integer> heads=new HashMap<>();int position=0;
        for(String id:line){if(position++>=WarGrid.cells()||id.equals(RosterMath.EMPTY)||id.equals(RosterMath.MARKER))continue;
            var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)).findFirst().orElse(null);if(spec==null)continue;
            full.merge(spec.group(),spec.health()*b.campaign.health(s,spec),Double::sum);heads.merge(spec.group(),1,Integer::sum);}
        for(var e:full.entrySet())s.fullHealth.merge(e.getKey(),e.getValue(),Math::max);
        for(var e:heads.entrySet())s.fullCount.merge(e.getKey(),e.getValue(),Math::max);
    }

    /** A troop joined mid-round: the group's ceiling rises to the health actually fielded (so a trained-only group still has a strength), and never falls. */
    static void fielded(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group){
        double health=0;int heads=0;for(var u:b.units)if(u.squad==s&&!u.temporary&&u.spec.group().equals(group)&&u.mob.isAlive()){health+=u.spec.health()*b.campaign.health(s,u.spec);heads++;}
        s.fullHealth.merge(group,health,Math::max);s.fullCount.merge(group,heads,Math::max);
    }

    /**
     * The last-stand pass, every 20 ticks while the round is live. A group is at its last stand while its strength (BattleMath.strength of its
     * members' living health over its ceiling) is at or below lastStandShare and an enemy troop or commander stands within lastStandRadius of
     * its centre; it leaves the state when no enemy is near or its strength rises, and a group with no living member is in no state. When a
     * group turns to its last stand its owner is told once ("Infantry make a last stand"), and again no sooner than lastStandFeedSeconds later.
     */
    public static void tick(GravesAndGlory.Battle b){
        for(var s:new ArrayList<>(b.squads.values())){if(!s.seated)continue;
            for(String group:GROUPS){
                List<GravesAndGlory.Unit> members=members(b,s,group);
                if(members.isEmpty()){s.lastStand.remove(group);continue;}
                Vec3d centre=centre(members);
                double living=0;for(var u:members)living+=u.mob.getHealth();
                double strength=BattleMath.strength(living,s.fullHealth.getOrDefault(group,0.0));
                boolean last=strength<=D.lastStandShare()&&enemyWithin(b,s.team,centre,D.lastStandRadius());
                if(!last){s.lastStand.remove(group);continue;}
                if(!s.lastStand.add(group))continue;
                Long told=s.lastStandAt.get(group);
                if(told!=null&&b.time-told<D.lastStandFeedSeconds()*20L)continue;
                s.lastStandAt.put(group,b.time);if(s.bot)continue; // a bot's army is told nothing, and its lines are nobody's to read
                lastStandLines.add(new Line(s.owner,group,b.time));b.campaign.notice(s.owner,label(group)+" make a last stand");
            }
        }
    }
    /** The army's living troops of a group that count toward its strength: a temporary troop (Grave Bloom's) is not one of them. The rule for temporary troops: they share their group's orders but not its numbers (its strength and its centre). */
    static List<GravesAndGlory.Unit> members(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group){
        List<GravesAndGlory.Unit> out=new ArrayList<>();for(var u:b.units)if(u.squad==s&&!u.temporary&&u.spec.group().equals(group)&&u.mob.isAlive())out.add(u);return out;
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
}
