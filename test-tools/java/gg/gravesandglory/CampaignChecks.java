package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;

/** Explicit test runs only. Real client packets, game entities and saved profiles. */
public final class CampaignChecks implements ClientModInitializer {
    int tick,contactsBefore;volatile boolean failed;Campaign.Profile backup;GravesAndGlory.Unit victim;float health;Vec3d botStart;UUID bot;UUID repairBot;
    void server(MinecraftClient c,java.util.function.Consumer<GravesAndGlory.Battle> f){c.getServer().execute(()->{if(GravesAndGlory.battle!=null)try{f.accept(GravesAndGlory.battle);}catch(Exception e){failed=true;MeltyTest.report("suite-exception",false,e.toString());e.printStackTrace();}});}
    void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"campaign-"+name+".png",c.getFramebuffer(),text->{});}
    void report(String id,boolean result,String why){if(!result)failed=true;MeltyTest.report(id,result,why);System.out.println("[Graves and Glory campaign] "+id+"="+result+" "+why);}
    /** Present after run 1 saves the collection; its existence marks the relaunch. */
    static Path restartFile(MinecraftClient c){return c.runDirectory.toPath().resolve("../evidence/restart-state.json");}
    /** Restart and fixture steps exist only in the development harness, never in a player or Melty test install. */
    static boolean devRun(){return net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();}
    /** Temporary diagnostic (dev harness only): why the lobby never receives STATE. */
    static int probeServer,probeClient;
    static void probeLine(String line){try{Path p=net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("../evidence/state-probe.txt");Files.writeString(p,line+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}
    static void stateProbe(){
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{
            if(++probeServer%100!=0)return;
            var b=GravesAndGlory.battle;StringBuilder sb=new StringBuilder("server tick="+probeServer+" battle="+(b!=null));
            if(b!=null)sb.append(" ready="+b.ready+" time="+b.time+" points="+b.points.size());
            for(var p:s.getPlayerManager().getPlayerList())sb.append(" player="+p.getName().getString()+" canSend="+net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,GravesAndGlory.STATE));
            probeLine(sb.toString());
        });
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null||++probeClient%100!=0)return;
            probeLine("client tick="+probeClient+" ready="+GravesAndGloryClient.ready+" running="+GravesAndGloryClient.running+" message="+GravesAndGloryClient.message+" screen="+(c.currentScreen==null?"none":c.currentScreen.getClass().getSimpleName()));
        });
    }
    @Override public void onInitializeClient(){
        if(!System.getProperty("gravesandglory.phase3Checks","").isEmpty())return; // the Phase 3 suites drive their own world
        // The battle self-test (-PselfTest) drives its own battle; these suites would fight it for the same world.
        if(!MeltyTest.active()||Boolean.getBoolean("gravesandglory.selfTest")||Boolean.getBoolean("gravesandglory.campChecks")||Boolean.getBoolean("gravesandglory.networkHost")||Boolean.getBoolean("gravesandglory.networkJoin")||!System.getProperty("gravesandglory.phase1Checks","").isEmpty())return;
        stateProbe();
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            tick++;
            if(Boolean.getBoolean("gravesandglory.startupOnly")){
                if(tick==25)report("startup-texture-choice",Presentation.textures()==Integer.getInteger("gravesandglory.expectedTexture",2),"Selected texture index "+Presentation.textures());
                if(tick==35){report("startup-suite",!failed,"Startup-only fixture complete");c.scheduleStop();}
                return;
            }
            if(tick==20){c.setScreen(new GravesAndGloryClient.LobbyScreen());server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());backup=Campaign.JSON.fromJson(Campaign.JSON.toJson(b.campaign.profile(p.getUuid())),Campaign.Profile.class);report("lobby-ready",p.getZ()<-275&&b.world.getBlockState(new BlockPos(0,64,CampaignMaps.LOBBY_Z)).isSolidBlock(b.world,new BlockPos(0,64,CampaignMaps.LOBBY_Z)),"Player stands in built war council");});}
            if(tick==25&&devRun()&&Files.exists(restartFile(c))){shot(c,"restart-lobby");server(c,b->{try{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var pr=b.campaign.profile(p.getUuid());var saved=Campaign.JSON.fromJson(Files.readString(restartFile(c)),Campaign.Profile.class);
                report("restart-collection",Campaign.JSON.toJsonTree(pr).equals(Campaign.JSON.toJsonTree(saved)),"Entire collection, loadout and commander profile after reopen matches the saved profile");
                if(Boolean.getBoolean("gravesandglory.migrationFixture")){
                    Path run=c.runDirectory.toPath(),save=b.server.getSavePath(net.minecraft.util.WorldSavePath.ROOT);
                    report("legacy-world-upgrade",Files.isRegularFile(run.resolve("saves/Bannerbound/level.dat"))&&Files.isRegularFile(run.resolve("saves/Graves and Glory/level.dat"))&&b.server.getSaveProperties().getLevelName().equals("Bannerbound"),"Legacy-named Minecraft world loaded from a copy; original remains");
                    report("legacy-profile-upgrade",Files.isDirectory(save.resolve("bannerbound-test-profiles"))&&Files.isRegularFile(b.campaign.directory.resolve(p.getUuid()+".json"))&&pr.gold==saved.gold,"Legacy host profile directory retained and current profile available");
                    report("legacy-layout-upgrade",Files.exists(save.resolve("bannerbound-layout.json"))&&Files.exists(save.resolve("gravesandglory-layout.json")),"Legacy map cache marker copied and retained");
                    report("legacy-settings-upgrade",Files.mismatch(run.resolve("bannerbound/presentation.json"),run.resolve("gravesandglory/presentation.json"))==-1,"Legacy visual preferences copied without altering original");
                }
                long stale=0;for(var e:b.world.iterateEntities())if(e.getCommandTags().contains("gravesandglory")&&e.getClass()==net.minecraft.entity.mob.HuskEntity.class)stale++;
                report("restart-clean-lobby",stale==0,"Hostile husks left from the earlier session: "+stale);}catch(Exception e){report("restart-collection",false,e.toString());}});}
            if(tick==40)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var pad=CampaignMaps.padCenter(0);b.relocate(p,pad.x,pad.y,pad.z,90,0);});
            if(tick==52)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var on=b.campaign.onPad.get(p.getUuid());String note=b.campaign.notices.getOrDefault(p.getUuid(),"");
                report("pad-ready",on!=null&&on==0&&b.campaign.onPad.size()==1&&b.campaign.padCountdown>=0&&b.campaign.padCountdown<=200,"Solo commander is ready and the countdown is active: pad="+on+" countdown="+b.campaign.padCountdown+" notice="+note);});
            if(tick==54)server(c,b->b.campaign.toLobby(b.server.getPlayerManager().getPlayer(c.player.getUuid())));
            if(tick==66)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());report("pad-step-off",!b.campaign.onPad.containsKey(p.getUuid())&&b.campaign.padCountdown<0,"Stepping off the pad clears the ready state; pads="+b.campaign.onPad.size());});
            if(tick==70)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());UUID me=p.getUuid();var camp=b.campaign;
                camp.closeParty();camp.handle(p,"party:open");
                report("party-open",me.equals(camp.partyLeader)&&camp.party.contains(me),"Commander opens a custom lobby and leads it; leader="+camp.partyLeader);
                UUID other=UUID.randomUUID();camp.partyLeader=other;camp.party.add(other);
                camp.handle(p,"party:invite:"+p.getGameProfile().getName());
                report("invite-leader-only",!camp.invites.containsKey(me)&&camp.notices.getOrDefault(me,"").contains("Only the custom lobby leader"),"A member cannot send invitations; notice="+camp.notices.getOrDefault(me,""));
                camp.closeParty();camp.partyLeader=other;camp.party.add(other);camp.invites.put(me,other);camp.inviteUntil.put(me,(long)b.time-1);
                camp.handle(p,"party:accept");
                report("invite-expired",!camp.party.contains(me)&&!camp.invites.containsKey(me)&&camp.notices.getOrDefault(me,"").contains("expired"),"Accepting after the sixty-second window is refused; notice="+camp.notices.getOrDefault(me,""));
                camp.closeParty();camp.partyLeader=other;camp.party.add(other);camp.invites.put(me,other);camp.inviteUntil.put(me,(long)b.time+1200);
                camp.handle(p,"party:decline");
                report("invite-decline",!camp.party.contains(me)&&!camp.invites.containsKey(me)&&!camp.inviteUntil.containsKey(me),"Declining removes the invitation without joining");
                camp.closeParty();camp.partyLeader=other;camp.party.add(other);camp.invites.put(me,other);camp.inviteUntil.put(me,(long)b.time+1200);
                camp.handle(p,"party:accept");
                report("invite-accept",camp.party.contains(me)&&!camp.invites.containsKey(me),"Accepting a live invitation joins the custom lobby");
                camp.closeParty();});
            if(tick==60)shot(c,"war-council");
            if(tick==100&&devRun()&&Files.exists(restartFile(c))){shot(c,"restart-excalibur");int expected=Integer.getInteger("gravesandglory.expectedTexture",2);report("restart-texture-choice",Presentation.textures()==expected,"Saved texture choice preserved; expected "+expected+", actual "+Presentation.textures());}
            if(tick==80)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var pr=b.campaign.profile(p.getUuid());int before=pr.unlocked.size();pr.gold=100;b.campaign.handle(p,"roll");report("unit-unlocks",pr.unlocked.size()==before+1&&pr.gold==0,"One paid random roll unlocks one previously locked type");var want=java.util.Arrays.stream(Spec.TROOPS).filter(t->!pr.unlocked.contains(t.id())).findFirst().orElse(null);if(want!=null){pr.gold=100;b.campaign.handle(p,"unlock:"+want.id());report("chosen-unlock",pr.unlocked.contains(want.id())&&pr.gold==0,"A paid unlock adds exactly the chosen troop");}
                try{var loaded=Campaign.JSON.fromJson(Files.readString(b.campaign.directory.resolve(p.getUuid()+".json")),Campaign.Profile.class);report("profile-persistence",loaded.unlocked.equals(pr.unlocked)&&loaded.gold==pr.gold,"Read collection back from disk");}catch(Exception e){report("profile-persistence",false,e.toString());}
                for(var t:Spec.TROOPS)if(t.faction().equals("dawn"))pr.army.put(t.id(),0);
                for(int i=0;i<20;i++)b.campaign.handle(p,"army:knight:add");b.campaign.handle(p,"army:knight:add");b.campaign.handle(p,"slot:0:archer");boolean grid=pr.lineup(0).get(0).equals("archer")&&pr.army.get("archer")==1&&pr.army.get("knight")==19;report("army-composition",pr.count(0)==20&&grid,"Twenty selected; twenty-first rejected; grid replaces one slot and updates counts");b.campaign.handle(p,"slot:0:knight");
                b.campaign.handle(p,"party:open");b.campaign.handle(p,"mode:ai");b.campaign.handle(p,"start");var s=b.squads.get(p.getUuid());report("bot-practice",b.squads.size()==2&&b.squads.values().stream().filter(x->x.bot).count()==1&&b.units.size()==40,"One real commander and one AI army");
                var knight=b.units.stream().filter(u->u.squad==s).findFirst().orElseThrow();report("commander-bonuses",Math.abs(knight.mob.getMaxHealth()-knight.spec.health()*1.2)<.01,"Vanguard infantry bonus applied at spawn");var enemy=b.units.stream().filter(u->u.squad.bot).findFirst().orElseThrow();bot=enemy.mob.getUuid();botStart=enemy.mob.getPos();});
            if(tick==150){report("intro-camera",CampaignClient.cinematic()&&c.options.getPerspective()!=net.minecraft.client.option.Perspective.FIRST_PERSON,"Camera shows commander without moving their entity");shot(c,"commander-entrance");}
            if(tick==240)server(c,b->{var u=b.units.stream().filter(x->x.mob.getUuid().equals(bot)).findFirst().orElse(null);report("bot-objectives",u!=null&&u.mob.squaredDistanceTo(botStart)>4&&u.squad.order.equals("attack"),"AI army advances toward an objective");var s=b.squads.get(c.player.getUuid());
                for(var troop:b.units)if(troop.squad==s){int x=-200+troop.slot%5*2,z=troop.slot/5*2;troop.mob.refreshPositionAndAngles(x,Arena.ground(x,z)+1,z,0,0);}s.order="hold";s.target=new Vec3d(-200,65,0);
                var enemy=b.squads.values().stream().filter(x->x.bot).findFirst().orElseThrow();enemy.order="hold";enemy.target=new Vec3d(-196,65,0);var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals("revenant")).findFirst().orElseThrow();victim=b.spawn(enemy,spec,19,new Vec3d(-196,Arena.ground(-196,0)+1,0));health=victim.mob.getHealth();});
            if(tick==300){shot(c,"battle");server(c,b->{report("combat-effects",!victim.mob.isAlive()||victim.mob.getHealth()<health,"Actual melee damage after swing and spatial effect");});}
            if(tick==320){CommandMode.issue("infantry","move","line",new Vec3d(-170,Arena.ground(-170,0)+1,0),-90,-1);}
            if(tick==350)server(c,b->{var s=b.squads.get(c.player.getUuid());report("tactical-orders",s.assignments.containsKey("infantry")&&s.assignments.get("infantry").order.equals("move"),"Real client packet changed group assignment");b.roundStarted=b.time-181*20;b.campaign.joinedAt.put(s.owner,b.roundStarted);b.campaign.finish(s.team);report("match-rewards",b.campaign.profile(s.owner).gold==40,"Completed practice awards forty collection gold; elapsed-time fixture");});
            if(tick==380){c.setScreen(new GravesAndGloryClient.LobbyScreen());((CampaignLobbyScreen)c.currentScreen).showTab(1);}
            if(tick==420)shot(c,"collection");
            if(tick==450){c.setScreen(new GravesAndGloryClient.LobbyScreen());((CampaignLobbyScreen)c.currentScreen).showTab(2);}
            if(tick==460&&c.currentScreen instanceof CampaignLobbyScreen screen){screen.pendingClass="marksman";screen.showTab(2);screen.confirmClass.onPress();}
            if(tick==480){report("class-confirmation",CampaignClient.profile.commander.equals("marksman"),"Confirmed class selection reached server and returned in profile state");shot(c,"classes");}
            if(tick==500){c.setScreen(null);server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());boolean pass=true;for(int n=1;n<=4;n++){b.campaign.handle(p,"party:open");b.campaign.handle(p,"mode:"+n);b.campaign.handle(p,"start");pass&=b.squads.size()==n*2&&b.units.size()==n*40;for(int team=0;team<2;team++){final int t=team;pass&=b.squads.values().stream().filter(s->s.team==t).count()==n;}if(n<4)b.campaign.finish(-1);}report("team-modes",pass,"1v1, 2v2, 3v3, 4v4 filled to exact team capacities");});}
            if(tick==650)server(c,b->{report("large-battle",b.units.size()==160&&b.units.stream().allMatch(u->u.mob.isAlive()),"Eight twenty-troop armies simulated; tick ms="+b.server.getTickTime());var missing=b.units.stream().filter(u->u.squad.bot).findFirst().orElseThrow();repairBot=missing.squad.owner;missing.mob.discard();var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,-235,85,28,-70,25);});
            if(tick==655)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());b.relocate(p,-286,Arena.ground(-286,40)+1,40,0,0);p.changeGameMode(GameMode.ADVENTURE);p.setHealth(p.getMaxHealth());});
            if(tick==669)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var s=b.squads.get(p.getUuid());report("barracks-ready",b.atBarracks(p,s)&&!b.respawns.containsKey(p.getUuid())&&!p.isSpectator(),"Fixture stands outside barracks: "+p.getPos()+" respawn="+b.respawns.containsKey(p.getUuid()));});
            if(tick==670)GravesAndGloryClient.send("barracks");
            if(tick==685){report("barracks-open",c.currentScreen instanceof GravesAndGloryClient.BarracksScreen,"Barracks request opened actual client UI");if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.disband.onPress();}
            if(tick==700&&c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen){screen.mouseClicked(30*screen.uiScale,120*screen.uiScale,0);screen.mouseReleased(30*screen.uiScale,120*screen.uiScale,0);screen.recruit.onPress();}
            if(tick==720){report("barracks-queue",GravesAndGloryClient.queued==1,"Real UI purchased an unlocked archer");shot(c,"barracks-training");if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.cancel.onPress();}
            if(tick==745){report("barracks-cancel",GravesAndGloryClient.queued==0,"UI cancelled and refunded queue");if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen){screen.mouseClicked(30*screen.uiScale,120*screen.uiScale,0);screen.mouseReleased(30*screen.uiScale,120*screen.uiScale,0);screen.recruit.onPress();}}
            if(tick==835){server(c,b->{var ai=b.squads.get(repairBot);report("bot-replacements",ai!=null&&b.units.stream().filter(u->u.squad==ai&&u.mob.isAlive()).count()==20,"AI trained a casualty replacement through its paid queue");});report("barracks-ui",GravesAndGloryClient.count==20&&GravesAndGloryClient.queued==0,"Replacement completed after sequential training");c.setScreen(null);}
            if(tick==840){contactsBefore=Presentation.receivedImpacts;server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var own=b.squads.get(p.getUuid());var enemy=b.units.stream().filter(u->u.squad.team!=own.team&&u.mob.isAlive()).findFirst().orElseThrow();enemy.mob.damage(b.world.getDamageSources().playerAttack(p),6);});}
            if(tick==847){report("impact-contact",Presentation.receivedImpacts>contactsBefore,"A confirmed commander hit delivered a contact packet to the actual client");contactsBefore=Presentation.receivedImpacts;}
            if(tick==850)server(c,b->{
                boolean routes=true;for(var bridge:Spec.STRUCTURES)if(bridge.kind().equals("bridge")){for(int x=bridge.x()-bridge.width()/2;x<=bridge.x()+bridge.width()/2;x++)routes&=Math.abs(Arena.ground(x,bridge.z())-64)<=1;}
                report("map-routes",routes&&GravesAndGlory.R.arenaHalfX()*2==700&&GravesAndGlory.R.arenaHalfZ()*2==500,"Authored dry centreline across all three river crossings");
                var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var s=b.squads.get(p.getUuid());var friendly=b.units.stream().filter(u->u.squad==s).findFirst().orElseThrow();float hp=friendly.mob.getHealth();friendly.mob.damage(b.world.getDamageSources().playerAttack(p),5);report("friendly-fire",friendly.mob.getHealth()==hp,"Allied damage rejected");
                b.campaign.finish(-1);if(backup!=null){b.campaign.profiles.put(p.getUuid(),Campaign.JSON.fromJson(Campaign.JSON.toJson(backup),Campaign.Profile.class));b.campaign.save(p.getUuid());}b.campaign.handle(p,"mode:4");});
            if(tick==865)report("impact-friendly-filter",Presentation.receivedImpacts==contactsBefore,"Rejected friendly damage produced no contact accent");
            if(tick==1010&&devRun())server(c,b->{try{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());Files.writeString(restartFile(c),Campaign.JSON.toJson(b.campaign.profile(p.getUuid())));
                var husk=net.minecraft.entity.EntityType.HUSK.create(b.world);husk.addCommandTag("gravesandglory");husk.refreshPositionAndAngles(-20,65,CampaignMaps.LOBBY_Z,0,0);b.world.spawnEntity(husk);
                report("restart-fixture",true,"Collection saved and one stale hostile husk planted for the reopen test");}catch(Exception e){report("restart-fixture",false,e.toString());}});
            if(tick==900){c.setScreen(new GravesAndGloryClient.LobbyScreen());report("campaign-suite",!failed,"Mechanical checks finished; local multiplayer joining is a separate run");shot(c,"final-lobby");}
            if(tick==950)server(c,b->{
                var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var camp=b.campaign;
                var pad=CampaignMaps.padCenter(0);b.relocate(p,pad.x,pad.y,pad.z,90,0);camp.padMapRolled=true;
                camp.padTick(); // since 0.3.5 a new ready group first resets the countdown, so this tick only seats him on the pad
                camp.padCountdown=10;camp.padTick();
                report("solo-pad-3v3",b.running&&camp.size==3&&b.squads.size()==6&&b.units.size()==120&&b.squads.values().stream().filter(s->s.bot).count()==5,"One ready commander starts a 3v3 with five AI commanders");
                camp.finish(-1);if(backup!=null){camp.profiles.put(p.getUuid(),Campaign.JSON.fromJson(Campaign.JSON.toJson(backup),Campaign.Profile.class));camp.save(p.getUuid());}
            });
            if(tick==980)report("roadmap-suite",!failed,"Full regression including solo ready-pad battle completed");
        });
    }
}

