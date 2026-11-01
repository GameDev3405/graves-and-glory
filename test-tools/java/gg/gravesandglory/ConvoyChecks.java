package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One real Convoy round on Frostmarch (GRAVES_PHASE4_CHECKS=convoy). The player escorts (skirmish seats it on Dawn) and is
 * moved by the suite: beside the wagon it pushes; set just short of Pass Crossing and pushed over, the clock gains 3:00 and the
 * squad 50 gold; taken away with every troop kept clear, the wagon waits 10 s and rolls back but never behind the checkpoint;
 * set short of the refuge and pushed home, the escort wins. Screenshots p4-convoy-push and p4-convoy-checkpoint show the HUD.
 */
final class ConvoyChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    volatile UUID player;volatile boolean done;volatile String shot;
    int stage;long at;double d0;int seconds0,gold0;boolean rolled,floorKept=true;
    final List<String> fails=new ArrayList<>();

    public int limit(){return 16000;}
    public void cleanup(MinecraftClient c){Phase4Checks.server(c,b->{if(b.running)b.campaign.returnLobby();});}

    public void step(MinecraftClient c,int tick){
        if(tick==20){c.setScreen(null);player=c.player.getUuid();}
        String s=shot;
        if(s!=null){c.options.hudHidden=false;if(s.startsWith("!")){Phase4Checks.shot(c,s.substring(1));shot=null;}else shot="!"+s;} // one frame with the HUD drawn, then the picture
        if(done)Phase4Checks.finish(c,"convoy");
    }

    /** The player beside the wagon, the defence's troops kept out of range so nothing contests it. */
    static void beside(GravesAndGlory.Battle b,ServerPlayerEntity p,ConvoyMode m){
        Vec3d w=m.position(m.distance);p.teleport(p.getServerWorld(),w.x,w.y,w.z+2,p.getYaw(),20);
        for(var u:b.units)if(u.squad.team!=ConvoyMode.ESCORT&&u.mob.isAlive()&&u.mob.getPos().distanceTo(w)<20)u.mob.teleport(w.x,w.y+1,w.z+30);
    }
    /** Keeps every troop and the player out of push range, so the wagon is alone. */
    static void clear(GravesAndGlory.Battle b,ServerPlayerEntity p,ConvoyMode m){
        Vec3d w=m.position(m.distance);
        for(var u:b.units)if(u.mob.isAlive()&&u.mob.getPos().distanceTo(w)<20)u.mob.teleport(w.x,w.y+1,w.z+30);
        p.teleport(p.getServerWorld(),w.x,w.y+1,w.z-30,p.getYaw(),20);
    }

    public void serverTick(GravesAndGlory.Battle b){
        if(player==null||done)return;
        ServerPlayerEntity p=b.server.getPlayerManager().getPlayer(player);if(p==null)return;
        if(stage==0){
            if(b.campaign.map!=2){if(at==0){at=b.time;b.campaign.switchMap(2);}else if(b.time-at>12000){end("Frostmarch did not build");}return;}
            if(!b.ready)return;
            b.campaign.battleMode=Modes.CONVOY;
            if(Phase1Checks.skirmish(b,p,2,1,"normal")){stage=1;at=b.time;}
            return;
        }
        if(!b.running&&stage<4){end("round ended early at stage "+stage+", winner "+b.campaign.lastWinner);return;}
        if(!(b.mode instanceof ConvoyMode m)){end("mode is "+b.mode.id());return;}
        switch(stage){
            case 1->{ // push: the player beside the wagon for 5 s
                if(b.time-at<40){d0=m.distance;return;}
                beside(b,p,m);
                if(b.time-at==120)shot="convoy-push";
                if(b.time-at<140)return;
                Phase4Checks.report("p4-convoy-wagon",m.wagon!=null&&!m.wagon.isRemoved()&&!m.wagon.shouldSave()&&m.rule().equals("Convoy: reach The Refuge"),"wagon "+(m.wagon!=null)+", rule "+m.rule());
                Phase4Checks.report("p4-convoy-push",m.distance>d0+3,"moved "+String.format("%.1f",m.distance-d0)+" blocks, state "+m.state);
                m.distance=Math.max(m.distance,m.stops[0]-1.5);seconds0=b.roundSeconds;gold0=b.squads.get(player).gold;stage=2;at=b.time;
            }
            case 2->{ // checkpoint: pushed over Pass Crossing
                beside(b,p,m);
                if(m.checkpoints()<1){if(b.time-at>200)end("never crossed Pass Crossing, at "+m.distance);return;}
                Phase4Checks.report("p4-convoy-checkpoint",b.roundSeconds==seconds0+ConvoyMode.CHECKPOINT_SECONDS&&b.squads.get(player).gold>=gold0+ConvoyMode.CHECKPOINT_GOLD,
                        "clock "+seconds0+" to "+b.roundSeconds+", gold "+gold0+" to "+b.squads.get(player).gold);
                shot="convoy-checkpoint";m.distance=m.stops[0]+8;stage=3;at=b.time;
            }
            case 3->{ // rollback: alone for 10 s, then back, never behind the checkpoint
                clear(b,p,m);
                if(m.distance<m.stops[0]-1e-6)floorKept=false;
                if(m.state.equals(ModeMath.ROLLBACK))rolled=true;
                if(b.time-at<ConvoyMode.IDLE_TICKS+20*20)return;
                Phase4Checks.report("p4-convoy-rollback",rolled&&m.rolledBack()&&floorKept&&Math.abs(m.distance-m.stops[0])<1e-6,
                        "rolled "+rolled+", floor kept "+floorKept+", at "+String.format("%.2f",m.distance)+" of checkpoint "+String.format("%.2f",m.stops[0]));
                m.distance=m.total()-1.5;stage=4;at=b.time;
            }
            case 4->{ // finish: pushed into the refuge
                beside(b,p,m);
                if(!m.finished){if(b.time-at>200)end("not through, at "+m.distance+" of "+m.total());return;}
                Phase4Checks.report("p4-convoy-finish",b.campaign.lastWinner==ConvoyMode.ESCORT,"winner "+b.campaign.lastWinner);
                stage=5;end(null);
            }
            default->{}
        }
    }

    void end(String why){
        if(why!=null){fails.add(why);Phase4Checks.report("p4-convoy-round",false,why);}
        done=true;
    }
}
