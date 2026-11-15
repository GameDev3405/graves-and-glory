package gg.gravesandglory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;

/** Unshipped interaction tests; reports mechanics, never asserts fun or balance. */
public final class CommandPlaytest implements ClientModInitializer {
    int tick;boolean started;Vec3d initial;float hp;GravesAndGlory.Unit victim,healer,hexer;boolean fourSeen;
    static void result(String name,boolean pass,String detail){try{Path file=Path.of("../evidence/revision-02/playtest.txt");Files.createDirectories(file.getParent());Files.writeString(file,(pass?"PASS ":"FAIL ")+name+" "+detail+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    void server(MinecraftClient c,java.util.function.Consumer<GravesAndGlory.Battle> action){c.getServer().execute(()->action.accept(GravesAndGlory.battle));}
    void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"revision-"+name+".png",c.getFramebuffer(),t->{});}
    @Override public void onInitializeClient(){
        if(!Boolean.getBoolean("gravesandglory.commandPlaytest"))return;
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            c.options.pauseOnLostFocus=false;tick++;
            if(tick==1){c.setScreen(null);server(c,b->{var s=b.squads.get(c.player.getUuid());s.seated=true;b.startRound();b.campaign.introUntil=b.time; /* a lobby join is unseated (AdmissionPolicy) and startRound fields seated squads only, so seat the player as the self-test seats its squads; and end the 120-tick intro, during which tactical refuses orders, because these checks issue theirs from tick 60 */for(var u:b.units)if(u.squad==s){double x=-78+u.slot%5*2,z=10+u.slot/5*2;u.mob.refreshPositionAndAngles(x,Arena.ground((int)x,(int)z)+1,z,-90,0);}var p=b.server.getPlayerManager().getPlayer(s.owner);p.teleport(b.world,-93,72,22,-70,20);});}
            if(tick==60){CommandMode.selected="infantry";CommandMode.issue("infantry","hold","line",new Vec3d(-68,65,10),-90,-1);}
            if(tick==80)CommandMode.issue("ranged","move","line",new Vec3d(-70,65,35),-90,-1);
            if(tick==100)CommandMode.issue("support","hold","loose",new Vec3d(-92,65,12),-90,-1);
            if(tick==140)server(c,b->{var s=b.squads.get(c.player.getUuid());result("independent-packet-orders",s.assignments.size()==3&&s.assignments.get("infantry").order.equals("hold")&&s.assignments.get("ranged").order.equals("hold")&&s.assignments.get("support").target.x==-92,"Three real client packets / distinct assignments (a ranged move packet is stored as hold)");initial=b.units.stream().filter(u->u.squad==s&&u.spec.group().equals("ranged")).findFirst().orElseThrow().mob.getPos();});
            if(tick==160)CommandSelection.setPanelOpen(true);
            if(tick==180)shot(c,"command-interface");
            if(tick==200){CommandSelection.setPanelOpen(false);c.setScreen(null);server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,-85,84,5,-90,45);});CommandMode.begin("move");}
            if(tick==220)shot(c,"world-preview");
            if(tick==240){CommandMode.cancel();c.setScreen(new BattleMapScreen(null));}
            if(tick==260)shot(c,"tactical-map");
            if(tick==280)c.setScreen(null);
            if(tick==340)server(c,b->{var s=b.squads.get(c.player.getUuid());var unit=b.units.stream().filter(u->u.squad==s&&u.spec.group().equals("ranged")).findFirst().orElseThrow();result("ranged-group-moves",unit.mob.squaredDistanceTo(initial)>16,"distance="+Math.sqrt(unit.mob.squaredDistanceTo(initial)));var infantry=b.units.stream().filter(u->u.squad==s&&u.spec.group().equals("infantry")).findFirst().orElseThrow();var enemy=new GravesAndGlory.Squad(UUID.randomUUID(),1-s.team,infantry.mob.getPos().add(2,0,0));enemy.order="hold";var type=Campaign.troop(enemy.team==0?"knight":"revenant"); /* the first melee troop of the team's default House (Phase 2: troops belong to Houses, not sides) */victim=b.spawn(enemy,type,0,infantry.mob.getPos().add(2,0,0));});
            if(tick==400){CommandMode.issue("infantry","retreat","compact".equals(CommandMode.formation)?"block":CommandMode.formation,new Vec3d(-110,65,10),90,-1);}
            if(tick==420)server(c,b->{var s=b.squads.get(c.player.getUuid());result("retreat-interrupt",b.units.stream().filter(u->u.squad==s&&u.spec.group().equals("infantry")).allMatch(u->u.mob.getTarget()==null),"Attack targets cleared within one second");hp=victim.mob.getHealth();initial=b.units.stream().filter(u->u.squad==s&&u.spec.group().equals("infantry")).findFirst().orElseThrow().mob.getPos();});
            if(tick==500)server(c,b->{var s=b.squads.get(c.player.getUuid());var u=b.units.stream().filter(v->v.squad==s&&v.spec.group().equals("infantry")).findFirst().orElseThrow();result("retreat-disengages",u.mob.getX()<initial.x-3&&victim.mob.getHealth()==hp,"retreat distance="+(initial.x-u.mob.getX())+", enemy hp="+victim.mob.getHealth());victim.mob.discard();});
            if(tick==520)CommandMode.issue("all","follow","block",c.player.getPos(),c.player.getYaw(),-1);
            if(tick==560)server(c,b->{var s=b.squads.get(c.player.getUuid());result("whole-army-recall",s.assignments.values().stream().allMatch(a->a.order.equals("follow")),"All groups recalled");/* the old charge-requires-enemy check is gone: a charge needs no enemy now; cmdui/OrderIntakeSuite cu-intake-charge-* cover the rule */});
            if(tick==600){shot(c,"group-hud");result("suite-complete",true,"Local interaction slice; four-player experience and map rework remain unverified");server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var f=Spec.FACTIONS[b.squads.get(p.getUuid()).team];p.teleport(b.world,f.barracksX(),66,f.barracksZ(),0,0);});}
            if(tick==620)GravesAndGloryClient.send("barracks");
            if(tick==640)shot(c,"barracks");
            if(tick==660){if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.controls.get(0).disband().onPress();}
            if(tick==680){if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.controls.get(0).train().onPress();} /* the barracks lists one row per troop type of the banner; row 0 is the troop disbanded at tick 660 */
            if(tick==700){shot(c,"barracks-queue");result("barracks-queue",GravesAndGloryClient.queued==1,"Purchased replacement through real UI");}
            if(tick==710){if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.cancel.onPress();}
            if(tick==740){result("barracks-cancel",GravesAndGloryClient.queued==0,"Cancelled through real UI");c.setScreen(null);}
            if(tick==760){CommandMode.ping("help",-44,-43);c.setScreen(new BattleMapScreen(null));}
            if(tick==780){shot(c,"map-ping");result("teammate-ping",!CommandMode.pings.isEmpty(),"Server relayed map ping");}
            if(tick==800){c.setScreen(null);server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,-43,83,-25,145,28);});}
            if(tick==860)shot(c,"hamlet");
            if(tick==880)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());p.teleport(b.world,-34,86,6,-45,27);});
            if(tick==940)shot(c,"gatehouse");
            if(tick==960)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());p.teleport(b.world,20,83,-20,-130,25);});
            if(tick==1020)shot(c,"shrine");
            if(tick==1040)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());p.teleport(b.world,57,83,81,135,35);});
            if(tick==1100)shot(c,"ford");
            if(tick==1120)server(c,b->{
                for(var u:b.units)u.mob.discard();b.units.clear();var s=b.squads.get(c.player.getUuid());s.assignments.clear();s.order="hold";s.target=new Vec3d(-74,65,0);s.yaw=-90;
                var melee=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals("knight")).findFirst().orElseThrow();var support=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals("mage")).findFirst().orElseThrow();
                victim=b.spawn(s,melee,0,new Vec3d(-74,Arena.ground(-74,0)+1,0));victim.mob.setHealth(10);healer=b.spawn(s,support,1,new Vec3d(-78,Arena.ground(-78,0)+1,0));hp=victim.mob.getHealth();
            });
            if(tick==1200)server(c,b->{result("dawn-support-heals",victim.mob.getHealth()>hp,"health "+hp+" -> "+victim.mob.getHealth());healer.mob.discard();var enemy=new GravesAndGlory.Squad(UUID.randomUUID(),1,new Vec3d(-65,65,0));enemy.order="hold";var type=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals("hexer")).findFirst().orElseThrow();hexer=b.spawn(enemy,type,0,new Vec3d(-65,Arena.ground(-65,0)+1,0));victim.mob.equipStack(net.minecraft.entity.EquipmentSlot.OFFHAND,net.minecraft.item.ItemStack.EMPTY);victim.mob.setAiDisabled(true);victim.squad.order="move";victim.squad.target=victim.mob.getPos();hexer.squad.order="attack";victim.mob.setHealth(30);hp=30;});
            if(tick==1240)shot(c,"blight-telegraph");
            if(tick==1320)server(c,b->{result("dusk-blight-resolves",victim.mob.getHealth()<hp,"health "+hp+" -> "+victim.mob.getHealth()+", casterTarget="+hexer.mob.getTarget()+", windup="+hexer.windupUntil+", distance="+hexer.mob.distanceTo(victim.mob));hexer.mob.discard();victim.mob.discard();for(var s:b.squads.values()){s.assignments.clear();s.order="hold";s.target=b.base(s.team);b.reinforce(s);}});
            if(tick>1400&&tick%100==0)server(c,b->{if(!fourSeen&&b.server.getPlayerManager().getCurrentPlayerCount()==4){fourSeen=true;long alive=b.units.stream().filter(u->u.mob.isAlive()).count();result("four-clients-connected",b.squads.size()==4&&alive==80,"players="+b.squads.size()+", troops="+alive+", tickMs="+b.server.getAverageTickTime());}});
        });
    }
}
