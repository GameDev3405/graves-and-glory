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
    int tick,contactsBefore;volatile boolean failed;Profile backup;GravesAndGlory.Unit victim;float health;Vec3d botStart;UUID bot;UUID repairBot;Map<UUID,Vec3d> botStarts=new HashMap<>();String repairWhy="";Vec3d botTarget;long botDecided=-1;
    void server(MinecraftClient c,java.util.function.Consumer<GravesAndGlory.Battle> f){c.getServer().execute(()->{if(GravesAndGlory.battle!=null)try{f.accept(GravesAndGlory.battle);}catch(Exception e){failed=true;MeltyTest.report("suite-exception",false,e.toString());e.printStackTrace();}});}
    void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"campaign-"+name+".png",c.getFramebuffer(),text->{});}
    void report(String id,boolean result,String why){if(!result)failed=true;MeltyTest.report(id,result,why);System.out.println("[Graves and Glory campaign] "+id+"="+result+" "+why);}
    /** Phase 2: every army fields exactly the units its banner lists (a bot banner may list fewer than twenty). */
    static int fielded(GravesAndGlory.Battle b){var cat=RosterMath.Catalog.fromSpec();int n=0;for(var s:b.squads.values())n+=RosterMath.units(s.banner,cat);return n;}
    static boolean eachFieldsBanner(GravesAndGlory.Battle b){var cat=RosterMath.Catalog.fromSpec();for(var s:b.squads.values())if(b.units.stream().filter(u->u.squad==s).count()!=RosterMath.units(s.banner,cat))return false;return true;}
    static int copies(Profile pr){int n=0;for(var o:pr.owned.values())n+=o.copies;for(int v:pr.shards.values())n+=v;return n;}
    /** Present after run 1 saves the collection; its existence marks the relaunch. */
    static Path restartFile(MinecraftClient c){return c.runDirectory.toPath().resolve("../evidence/restart-state.json");}
    /** The built-in armory pack is the only resource pack the modpack carries; Fabric enables it on a first launch. */
    static boolean armoryEnabled(MinecraftClient c){return c.getResourcePackManager().getEnabledNames().contains("gravesandglory:armory");}
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
        if(!MeltyTest.active()||Boolean.getBoolean("gravesandglory.selfTest")||Boolean.getBoolean("gravesandglory.campChecks")||Boolean.getBoolean("gravesandglory.frontChecks")||Boolean.getBoolean("gravesandglory.networkHost")||Boolean.getBoolean("gravesandglory.networkJoin")||!System.getProperty("gravesandglory.rankNet","").isEmpty()||!System.getProperty("gravesandglory.phase1Checks","").isEmpty()||!System.getProperty("gravesandglory.phase2Checks","").isEmpty()||!System.getProperty("gravesandglory.phase4Checks","").isEmpty()||!System.getProperty("gravesandglory.commandUiChecks","").isEmpty())return;
        stateProbe();
        // Nothing else opens the world: without this the client idles on the first-run accessibility screen, then on the front menu (QA run 3 stall).
        int[] front={0};boolean[] opened={false};
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            if(c.getOverlay()!=null)return;
            if(c.currentScreen instanceof net.minecraft.client.gui.screen.AccessibilityOnboardingScreen s){s.close();return;} // a fresh root has no options.txt: press Continue as a player would
            if(!opened[0]&&c.currentScreen instanceof FrontMenuScreen&&++front[0]==40){opened[0]=true;GravesAndGloryClient.startCamp(c,false);}
        });
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            tick++;
            if(Boolean.getBoolean("gravesandglory.startupOnly")){
                if(tick==25)report("startup-armory-enabled",armoryEnabled(c),"The built-in armory pack is enabled on a first launch");
                if(tick==35){report("startup-suite",!failed,"Startup-only fixture complete");c.scheduleStop();}
                return;
            }
            if(tick==20){c.setScreen(new GravesAndGloryClient.LobbyScreen());server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());backup=Campaign.JSON.fromJson(Campaign.JSON.toJson(b.campaign.profile(p.getUuid())),Profile.class);report("lobby-ready",CampWorld.inCamp(p.getX(),p.getY(),p.getZ())&&b.world.getBlockState(new BlockPos(0,64,CampaignMaps.LOBBY_Z)).isSolidBlock(b.world,new BlockPos(0,64,CampaignMaps.LOBBY_Z)),"Player stands in built war council");});}
            if(tick==25&&devRun()&&Files.exists(restartFile(c))){shot(c,"restart-lobby");server(c,b->{try{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var pr=b.campaign.profile(p.getUuid());var saved=Campaign.JSON.fromJson(Files.readString(restartFile(c)),Profile.class);
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
            if(tick==100&&devRun()&&Files.exists(restartFile(c))){shot(c,"restart-look");report("restart-armory-enabled",armoryEnabled(c),"The armory pack stays enabled across a restart");}
            if(tick==80)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var pr=b.campaign.profile(p.getUuid());var cat=RosterMath.Catalog.fromSpec();UUID me=p.getUuid();
                // Phase 2 (spec 4.2): one roll draws from a House pool; a new troop joins the collection, a duplicate adds a copy (or shards at full stars).
                int before=pr.owned.size(),copies=copies(pr);pr.gold=100;b.campaign.handle(p,"roll");report("unit-unlocks",pr.gold==0&&(pr.owned.size()==before+1||copies(pr)>copies),"One paid House roll adds one troop or one copy: owned "+before+"->"+pr.owned.size()+", copies and shards "+copies+"->"+copies(pr)+"; "+b.campaign.notices.getOrDefault(me,""));
                var want=java.util.Arrays.stream(Spec.TROOPS).filter(t->!pr.owned.containsKey(t.id())).findFirst().orElse(null);
                if(want!=null){pr.gold=100;int owned=pr.owned.size();b.campaign.handle(p,"unlock:"+want.id());String note=b.campaign.notices.getOrDefault(me,"");report("chosen-unlock",!pr.owned.containsKey(want.id())&&pr.owned.size()==owned&&pr.gold==100&&note.contains("recruitment rolls"),"Chosen unlocks are retired (spec 6.3): unlock:"+want.id()+" is refused and charges nothing; notice="+note);pr.gold=0;}
                try{var loaded=Campaign.JSON.fromJson(Files.readString(b.campaign.directory.resolve(p.getUuid()+".json")),Profile.class);report("profile-persistence",Campaign.JSON.toJsonTree(loaded).equals(Campaign.JSON.toJsonTree(pr))&&loaded.schema==Profile.SCHEMA,"Read the current-schema (v3) collection, banner and gold back from disk");}catch(Exception e){report("profile-persistence",false,e.toString());}
                // Banner editing (banner rules, spec 4.6): slot:<i>:<id> sets one slot; an index past the banner and an unowned troop are refused.
                pr.banner=new ArrayList<>(java.util.Collections.nCopies(20,"knight"));b.campaign.handle(p,"slot:0:archer");b.campaign.handle(p,"slot:20:knight");String unowned=pr.owned.containsKey("gl_marshal")?null:"gl_marshal";if(unowned!=null)b.campaign.handle(p,"slot:1:"+unowned);
                boolean grid=pr.banner.size()==20&&pr.banner.get(0).equals("archer")&&java.util.Collections.frequency(pr.banner,"knight")==19&&RosterMath.validBanner(pr.banner,cat,pr.owned.keySet())==null;
                report("army-composition",grid&&RosterMath.units(pr.banner,cat)==20,"Twenty-slot banner: slot 0 replaced in place, slot 20 and an unowned Legendary refused; banner="+pr.banner);b.campaign.handle(p,"slot:0:knight");
                pr.owned.get("knight").stars=3; // a star 3 knight shows the progression bonus at spawn
                b.campaign.handle(p,"party:open");b.campaign.handle(p,"mode:ai");b.campaign.handle(p,"start");var s=b.squads.get(p.getUuid());var bots=b.squads.values().stream().filter(x->x.bot).toList();
                report("bot-practice",b.squads.size()==2&&bots.size()==1&&b.units.stream().filter(u->u.squad==s).count()==20&&b.units.size()==fielded(b)&&eachFieldsBanner(b),"One real commander (twenty knights) and one AI army fielding its banner: "+(bots.isEmpty()?"none":bots.get(0).banner)+"; units="+b.units.size());
                var knight=b.units.stream().filter(u->u.squad==s).findFirst().orElseThrow();double want3=knight.spec.health()*RosterMath.progressionMultiplier(RosterMath.starPct(3,RosterMath.rarityRow(knight.spec.rarity())),Spec.RECRUITMENT[0].capPct());var enemy=b.units.stream().filter(u->u.squad.bot&&u.spec.group().equals("infantry")).findFirst().orElseThrow(); /* the bot's front line, as in Phase 1: a Normal bot banner opens with its Epic support in slot 0 */
                botStarts.clear();for(var u:b.units)if(u.squad.bot)botStarts.put(u.mob.getUuid(),u.mob.getPos());
                report("commander-bonuses",want3>knight.spec.health()&&Math.abs(knight.mob.getMaxHealth()-want3)<.01&&Math.abs(enemy.mob.getMaxHealth()-enemy.spec.health())<.01,"Star 3 knight spawns with "+knight.mob.getMaxHealth()+" health (sheet "+knight.spec.health()+"); the AI fights at the sheet value; class bonuses return in Phase 2 Task 4");bot=enemy.mob.getUuid();botStart=enemy.mob.getPos();botTarget=enemy.squad.target;botDecided=-1;});
            if(tick==150){report("intro-camera",CampaignClient.cinematic()&&c.options.getPerspective()!=net.minecraft.client.option.Perspective.FIRST_PERSON,"Camera shows commander without moving their entity");shot(c,"commander-entrance");}
            if(tick>80&&tick<340&&botDecided<0)server(c,b->{var u=b.units.stream().filter(x->x.mob.getUuid().equals(bot)).findFirst().orElse(null);if(u!=null&&botTarget!=null&&!u.squad.target.equals(botTarget))botDecided=b.time-b.campaign.introUntil;}); /* a Normal bot decides when b.time is a multiple of decisionTicks (100), so its first decision comes within 100 ticks of the intro end; the check below waits that cadence and 40 ticks more, and every later step moved 100 ticks with it */
            if(tick==340)server(c,b->{var u=b.units.stream().filter(x->x.mob.getUuid().equals(bot)).findFirst().orElse(null);var moved=new StringBuilder();if(u!=null)for(String g:new String[]{"infantry","ranged","support"}){double sum=0;int n=0;for(var x:b.units)if(x.squad==u.squad&&x.spec.group().equals(g)&&botStarts.containsKey(x.mob.getUuid())){sum+=Math.sqrt(x.mob.squaredDistanceTo(botStarts.get(x.mob.getUuid())));n++;}moved.append(" "+g+" "+(n==0?"-":String.format(Locale.ROOT,"%.1f",sum/n)));}
                report("bot-objectives",u!=null&&u.mob.squaredDistanceTo(botStart)>4&&u.squad.order.equals("attack"),"AI army advances toward an objective: "+(u==null?"the tracked unit is gone":u.spec.id()+" moved "+String.format(Locale.ROOT,"%.1f",Math.sqrt(u.mob.squaredDistanceTo(botStart)))+" blocks under order "+u.squad.order)+"; mean blocks moved by group:"+moved+"; "+(botDecided<0?"no decision of its own seen":"its own first decision came "+botDecided+" ticks after the intro")+", the check "+(b.time-b.campaign.introUntil)+" ticks after it");var s=b.squads.get(c.player.getUuid());
                for(var troop:b.units)if(troop.squad==s){int x=-200+troop.slot%5*2,z=troop.slot/5*2;troop.mob.refreshPositionAndAngles(x,Arena.ground(x,z)+1,z,0,0);}s.order="hold";s.target=new Vec3d(-200,65,0);
                var enemy=b.squads.values().stream().filter(x->x.bot).findFirst().orElseThrow();enemy.order="hold";enemy.target=new Vec3d(-196,65,0);var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals("revenant")).findFirst().orElseThrow();victim=b.spawn(enemy,spec,19,new Vec3d(-196,Arena.ground(-196,0)+1,0));health=victim.mob.getHealth();});
            if(tick==400){shot(c,"battle");server(c,b->{report("combat-effects",!victim.mob.isAlive()||victim.mob.getHealth()<health,"Actual melee damage after swing and spatial effect");});}
            if(tick==420){CommandMode.issue("infantry","move","line",new Vec3d(-170,Arena.ground(-170,0)+1,0),-90,-1);}
            if(tick==450)server(c,b->{var s=b.squads.get(c.player.getUuid());report("tactical-orders",s.assignments.containsKey("infantry")&&s.assignments.get("infantry").order.equals("move"),"Real client packet changed group assignment");b.roundStarted=b.time-181*20;b.campaign.joinedAt.put(s.owner,b.roundStarted);b.campaign.finish(s.team);report("match-rewards",b.campaign.profile(s.owner).gold==40,"Completed practice awards forty collection gold; elapsed-time fixture");});
            if(tick==480){c.setScreen(new GravesAndGloryClient.LobbyScreen());((CampaignLobbyScreen)c.currentScreen).showTab(1);}
            if(tick==520)shot(c,"collection");
            if(tick==550){c.setScreen(new GravesAndGloryClient.LobbyScreen());((CampaignLobbyScreen)c.currentScreen).showTab(2);}
            if(tick==580){report("lobby-party-tab",c.currentScreen instanceof CampaignLobbyScreen,"The lobby's third tab is the custom lobby now that gear replaced the class tab");shot(c,"party");}
            if(tick==600){c.setScreen(null);server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());boolean pass=true;for(int n=1;n<=4;n++){b.campaign.handle(p,"party:open");b.campaign.handle(p,"mode:"+n);b.campaign.handle(p,"start");pass&=b.squads.size()==n*2&&b.units.size()==fielded(b)&&eachFieldsBanner(b);for(int team=0;team<2;team++){final int t=team;pass&=b.squads.values().stream().filter(s->s.team==t).count()==n;}if(n<4)b.campaign.finish(-1);}report("team-modes",pass,"1v1, 2v2, 3v3, 4v4 filled to exact team capacities; every army fields its banner");});}
            if(tick==750)server(c,b->{report("large-battle",b.squads.size()==8&&b.units.size()==fielded(b)&&b.units.size()>=8*16&&b.units.stream().allMatch(u->u.mob.isAlive()),"Eight banner armies simulated ("+b.units.size()+" troops); tick ms="+b.server.getTickTime());var missing=b.units.stream().filter(u->u.squad.bot&&u.spec.rarity().equals("common")).findFirst().orElseThrow();repairWhy=missing.spec.id()+" ("+missing.spec.trainingSeconds()+" s to train) lost with "+missing.squad.gold+" gold"; /* a Common, as in Phase 1: slot 0 of a Normal bot banner is a 9-second Epic */repairBot=missing.squad.owner;missing.mob.discard();var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,-235,85,28,-70,25);});
            if(tick==755)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());b.relocate(p,-286,Arena.ground(-286,40)+1,40,0,0);p.changeGameMode(GameMode.ADVENTURE);p.setHealth(p.getMaxHealth());});
            if(tick==769)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var s=b.squads.get(p.getUuid());report("barracks-ready",b.atBarracks(p,s)&&!b.respawns.containsKey(p.getUuid())&&!p.isSpectator(),"Fixture stands outside barracks: "+p.getPos()+" respawn="+b.respawns.containsKey(p.getUuid()));});
            if(tick==770)GravesAndGloryClient.send("barracks");
            if(tick==785){report("barracks-open",c.currentScreen instanceof GravesAndGloryClient.BarracksScreen,"Barracks request opened actual client UI");if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.disband.onPress();}
            if(tick==800&&c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen){screen.mouseClicked(30*screen.uiScale,95*screen.uiScale,0);screen.mouseReleased(30*screen.uiScale,95*screen.uiScale,0);screen.recruit.onPress();}
            if(tick==820){report("barracks-queue",GravesAndGloryClient.queued==1,"Real UI trained a knight of the commander's banner");shot(c,"barracks-training");if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.cancel.onPress();}
            if(tick==845){report("barracks-cancel",GravesAndGloryClient.queued==0,"UI cancelled and refunded queue");if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen){screen.mouseClicked(30*screen.uiScale,95*screen.uiScale,0);screen.mouseReleased(30*screen.uiScale,95*screen.uiScale,0);screen.recruit.onPress();}}
            if(tick==935){server(c,b->{var ai=b.squads.get(repairBot);report("bot-replacements",ai!=null&&b.units.stream().filter(u->u.squad==ai&&u.mob.isAlive()).count()==RosterMath.units(ai.banner,RosterMath.Catalog.fromSpec()),"AI trained a casualty replacement through its paid queue: "+repairWhy+"; alive "+(ai==null?-1:b.units.stream().filter(u->u.squad==ai&&u.mob.isAlive()).count())+" of "+(ai==null?-1:RosterMath.units(ai.banner,RosterMath.Catalog.fromSpec()))+", queued "+(ai==null?-1:ai.training.size()));});report("barracks-ui",GravesAndGloryClient.count==20&&GravesAndGloryClient.queued==0,"Replacement completed after sequential training");c.setScreen(null);}
            if(tick==940){contactsBefore=Presentation.receivedImpacts;server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var own=b.squads.get(p.getUuid());var enemy=b.units.stream().filter(u->u.squad.team!=own.team&&u.mob.isAlive()).findFirst().orElseThrow();enemy.mob.damage(b.world.getDamageSources().playerAttack(p),6);});}
            if(tick==947){report("impact-contact",Presentation.receivedImpacts>contactsBefore,"A confirmed commander hit delivered a contact packet to the actual client");contactsBefore=Presentation.receivedImpacts;}
            if(tick==950)server(c,b->{
                boolean routes=true;for(var bridge:Spec.STRUCTURES)if(bridge.kind().equals("bridge")){for(int x=bridge.x()-bridge.width()/2;x<=bridge.x()+bridge.width()/2;x++)routes&=Math.abs(Arena.ground(x,bridge.z())-64)<=1;}
                report("map-routes",routes&&GravesAndGlory.R.arenaHalfX()*2==700&&GravesAndGlory.R.arenaHalfZ()*2==500,"Authored dry centreline across all three river crossings");
                var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var s=b.squads.get(p.getUuid());var friendly=b.units.stream().filter(u->u.squad==s).findFirst().orElseThrow();float hp=friendly.mob.getHealth();friendly.mob.damage(b.world.getDamageSources().playerAttack(p),5);report("friendly-fire",friendly.mob.getHealth()==hp,"Allied damage rejected");
                b.campaign.finish(-1);if(backup!=null){b.campaign.profiles.put(p.getUuid(),Campaign.JSON.fromJson(Campaign.JSON.toJson(backup),Profile.class));b.campaign.save(p.getUuid());}b.campaign.handle(p,"mode:4");});
            if(tick==965)report("impact-friendly-filter",Presentation.receivedImpacts==contactsBefore,"Rejected friendly damage produced no contact accent");
            if(tick==1110&&devRun())server(c,b->{try{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var mark=b.campaign.profile(p.getUuid());if(!Files.exists(restartFile(c).resolveSibling("keep-profile"))){mark.gold=777;b.campaign.save(p.getUuid());} /* a non-default value, so the reopen proves the saved file was read */ Files.writeString(restartFile(c),Campaign.JSON.toJson(mark));
                var husk=net.minecraft.entity.EntityType.HUSK.create(b.world);husk.addCommandTag("gravesandglory");husk.refreshPositionAndAngles(-20,65,CampaignMaps.LOBBY_Z,0,0);b.world.spawnEntity(husk);
                report("restart-fixture",true,"Collection saved and one stale hostile husk planted for the reopen test");}catch(Exception e){report("restart-fixture",false,e.toString());}});
            if(tick==1000){c.setScreen(new GravesAndGloryClient.LobbyScreen());report("campaign-suite",!failed,"Mechanical checks finished; local multiplayer joining is a separate run");shot(c,"final-lobby");}
            if(tick==1050)server(c,b->{
                var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var camp=b.campaign;
                var pad=CampaignMaps.padCenter(0);b.relocate(p,pad.x,pad.y,pad.z,90,0);camp.padMapRolled=true;
                camp.padTick(); // since 0.3.5 a new ready group first resets the countdown, so this tick only seats him on the pad
                camp.padCountdown=10;camp.padTick();
                report("solo-pad-3v3",b.running&&camp.size==3&&b.squads.size()==6&&b.units.size()==fielded(b)&&eachFieldsBanner(b)&&b.squads.values().stream().filter(s->s.bot).count()==5,"One ready commander starts a 3v3 with five AI commanders");
                camp.finish(-1);if(backup!=null){camp.profiles.put(p.getUuid(),Campaign.JSON.fromJson(Campaign.JSON.toJson(backup),Profile.class));camp.save(p.getUuid());}
            });
            if(tick==1130)c.scheduleStop(); // a clean quit, so the next launch reopens the saved world and profile
            if(tick==1080)report("roadmap-suite",!failed,"Full regression including solo ready-pad battle completed");
        });
    }
}

