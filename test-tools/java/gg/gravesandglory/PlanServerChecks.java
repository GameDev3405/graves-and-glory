package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** p4-plan-server (battle map Task 4; a Phase4Checks suite, run as GRAVES_PHASE4_CHECKS=plan-server): on map 0 with bot fill, a real battleplan_v1 packet from this client puts ATTACK on point 1; every allied
 *  bot that is not retreating must have its target on point 1 by its second decision after the plan lands, say "on it, taking" once in the team
 *  feed, and a direct HOLD at a place must put that bot's target on the place at its next decision. Also: the grid and state packets arrived. */
public final class PlanServerChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    record Seen(long time, Vec3d target, boolean retreating, String order) {}
    volatile int stage;volatile UUID player;long stageAt;
    /** The client's next packet: 1 Attack on point 1, 2 direct Hold at holdX,holdZ on the first bot. */
    volatile int send;volatile double holdX,holdZ;
    final Map<UUID,List<Seen>> decisions=new HashMap<>();
    List<GravesAndGlory.Squad> bots=new ArrayList<>();
    long planAt=-1,landedAt=-1,holdAt=-1,holdLanded=-1;boolean finished,gridOk,stateOk;String stateWhy="";
    final List<String> heard=new ArrayList<>();
    volatile boolean listening;

    public int limit(){return 4000;}
    void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
    long since(GravesAndGlory.Battle b){return b.time-stageAt;}
    ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
    static int chosen(GravesAndGlory.Battle b,GravesAndGlory.Squad s){
        for(int i=0;i<b.points.size();i++)if(Math.abs(b.points.get(i).spec.x()-s.target.x)<.01&&Math.abs(b.points.get(i).spec.z()-s.target.z)<.01)return i;
        return -1;
    }

    public void step(MinecraftClient c,int tick){
        if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();Phase4Checks.server(c,b->{player=id;next(b,1);});}
        int cmd=send;
        if(cmd==1){send=0;BattleMapClient.sendPlan(0,1,"",BattlePlan.Job.ATTACK.ordinal(),0,0);}
        else if(cmd==2){send=0;var id=bots.get(0).owner.toString();BattleMapClient.sendPlan(1,-1,id,BattlePlan.Job.HOLD.ordinal(),holdX,holdZ);}
        if(listening)for(var l:EventFeed.lines(net.minecraft.util.Util.getMeasuringTimeMs()))if(!heard.contains(l.bornMs()+"|"+l.text()))heard.add(l.bornMs()+"|"+l.text());
        if(stage==3&&BattleMapClient.state!=null&&!stateOk){
            for(var p:BattleMapClient.state.points())if(p.point()==1&&p.job()==BattlePlan.Job.ATTACK.ordinal())stateOk=true;
        }
        if(stage==99&&!finished){finished=true;Campaign.decisionListener=null;listening=false;Phase4Checks.finish(c,"plan-server");}
        if(tick>3800&&!finished){finished=true;Campaign.decisionListener=null;Phase4Checks.report("p4-plan-server-timeout",false,"stuck in stage "+stage);Phase4Checks.finish(c,"plan-server");}
    }

    public void serverTick(GravesAndGlory.Battle b){
        switch(stage){
            case 1->{
                var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,3,"normal"))return;
                bots=new ArrayList<>();
                for(var s:b.squads.values())if(s.bot&&s.seated&&s.team==0)bots.add(s);
                Campaign.decisionListener=(c,sq,tr,ce,lv,fu,se,en,cc,ch,ho,re,wo,wt)->{
                    if(landedAt<0)return; // only decisions made once the server holds the plan count
                    decisions.computeIfAbsent(sq.owner,k->new ArrayList<>()).add(new Seen(b.time,sq.target,re,sq.order));
                };
                System.out.println("[p1] plan-server fixture: map "+b.campaign.map+", "+b.points.size()+" points, "+bots.size()+" allied bots");
                if(bots.isEmpty()){Phase4Checks.report("p4-plan-server-bots",false,"no allied bot seated");next(b,99);break;}
                next(b,2);
            }
            case 2->{
                if(since(b)<100)break; // let the first decisions settle
                gridOk=b.mapGrid!=null&&BattleMapClient.grid!=null&&BattleMapClient.grid.width>0;
                planAt=b.time;listening=true;send=1;next(b,3);
            }
            case 3->{ // wait for the plan to land and both bots to decide twice
                boolean landed=b.plan.points(0).stream().anyMatch(q->q.point()==1&&q.job()==BattlePlan.Job.ATTACK);
                if(!landed){if(since(b)>100){Phase4Checks.report("p4-plan-server-attack",false,"the plan packet never landed on the server");next(b,99);}break;}
                if(landedAt<0){landedAt=b.time;break;}
                boolean ready=true;
                for(var s:bots)if(!b.campaign.botRetreating.contains(s.owner)&&decisions.getOrDefault(s.owner,List.of()).size()<2)ready=false;
                if(!ready){if(since(b)>900){Phase4Checks.report("p4-plan-server-attack",false,"bots did not decide twice: "+summary(b));next(b,99);}break;}
                boolean ok=true;int checked=0;StringBuilder why=new StringBuilder();
                for(var s:bots){
                    var ds=decisions.getOrDefault(s.owner,List.of());
                    boolean everRetreated=ds.stream().anyMatch(Seen::retreating)||b.campaign.botRetreating.contains(s.owner);
                    if(everRetreated){why.append(s.tier).append(" retreating, skipped; ");continue;}
                    checked++;
                    boolean on=false;for(int i=0;i<2&&i<ds.size();i++){var t=ds.get(i).target();if(Math.abs(t.x-b.points.get(1).spec.x())<.01&&Math.abs(t.z-b.points.get(1).spec.z())<.01)on=true;}
                    why.append(s.owner.toString(),0,4).append(on?" on point 1":" NOT on point 1 (targets "+ds.stream().limit(2).map(d->d.target().toString()).toList()+")").append("; ");
                    if(!on)ok=false;
                }
                if(checked==0)ok=false;
                Phase4Checks.report("p4-plan-server-attack",ok,checked+" non-retreating bots: "+why);
                Phase4Checks.report("p4-plan-server-grid",gridOk,"the server scanned a grid and the client holds one: "+gridOk+(BattleMapClient.grid==null?"":" ("+BattleMapClient.grid.width+" x "+BattleMapClient.grid.depth+")"));
                next(b,4);
            }
            case 4->{ // the ack lines come spread over 20 to 60 ticks per bot, once each; and the state packet carried the plan
                if(since(b)<140)break;
                long taking=heard.stream().filter(h->h.contains("on it, taking ")).count();
                long expected=bots.stream().filter(s->!b.campaign.botRetreating.contains(s.owner)).count();
                Phase4Checks.report("p4-plan-server-ack",taking==expected&&expected>0,taking+" 'on it, taking' lines for "+expected+" bots: "+heard);
                Phase4Checks.report("p4-plan-server-state",stateOk,"battlemap_v1 listed the ATTACK plan on point 1");
                var s=bots.get(0);
                holdX=b.points.get(1).spec.x()-30;holdZ=b.points.get(1).spec.z()+20;
                if(b.campaign.botRetreating.contains(s.owner)){Phase4Checks.report("p4-plan-server-hold",false,"bot 0 is retreating, no place to hold");next(b,99);break;}
                holdAt=b.time;send=2;next(b,5);
            }
            case 5->{
                var s=bots.get(0);
                boolean landed=b.plan.orders(0).stream().anyMatch(o->o.army().equals(s.owner.toString())&&o.job()==BattlePlan.Job.HOLD&&o.point()<0);
                if(!landed){if(since(b)>100){Phase4Checks.report("p4-plan-server-hold",false,"the direct order never landed");next(b,99);}break;}
                if(holdLanded<0){holdLanded=b.time;decisions.remove(s.owner);break;}
                var ds=decisions.getOrDefault(s.owner,List.of());
                if(ds.isEmpty()){if(since(b)>600){Phase4Checks.report("p4-plan-server-hold",false,"the bot never decided again");next(b,99);}break;}
                // the decision list was cleared the tick the order landed: its first entry is the one the hold must show in
                var first=ds.get(0);
                double d=Math.hypot(first.target().x-holdX,first.target().z-holdZ);
                Phase4Checks.report("p4-plan-server-hold",first.retreating()||d<=1.5&&first.order().equals("hold"),"first decision after the direct HOLD: order "+first.order()+", target "+d+" blocks from the place");
                next(b,99);
            }
            default->{}
        }
    }

    String summary(GravesAndGlory.Battle b){
        StringBuilder sb=new StringBuilder();
        for(var s:bots)sb.append(s.owner.toString(),0,4).append('=').append(decisions.getOrDefault(s.owner,List.of()).size()).append(b.campaign.botRetreating.contains(s.owner)?"(retreating) ":" ");
        return sb.toString();
    }

    public void cleanup(MinecraftClient c){
        Campaign.decisionListener=null;
        if(!finished)Phase4Checks.report("p4-plan-server-incomplete",false,"the suite ended in stage "+stage);
    }
}
