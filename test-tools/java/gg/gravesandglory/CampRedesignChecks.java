package gg.gravesandglory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.gui.widget.ButtonWidget;
import java.util.*;

/** Isolated real-client checks for the camp redesign. Never included in releases. */
public final class CampRedesignChecks implements ClientModInitializer {
 int tick,front;boolean started;volatile boolean failed;java.util.random.RandomGenerator liveRoll;volatile int dupCopies,dupGold,shardsBefore;
 void check(String id,boolean pass){MeltyTest.report("camp-"+id,pass,id);System.out.println("[camp-check] "+id+"="+pass);if(!pass)failed=true;}
 void server(MinecraftClient c,java.util.function.Consumer<GravesAndGlory.Battle> fn){c.getServer().execute(()->{try{fn.accept(GravesAndGlory.battle);}catch(Exception e){check("exception",false);e.printStackTrace();}});}
 void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"camp-"+name+".png",c.getFramebuffer(),t->{});}
 void press(MinecraftClient c,String prefix){var b=c.currentScreen.children().stream().filter(v->v instanceof ButtonWidget w&&w.getMessage().getString().startsWith(prefix)).map(v->(ButtonWidget)v).findFirst().orElseThrow();b.onPress();}
 /** The Friends page's ready tag is one toggle widget whose word flips ("Ready", "Cancel ready"); the old screen's "Toggle ready" had one fixed label. Press whichever is shown. */
 void toggleReady(MinecraftClient c){var b=c.currentScreen.children().stream().filter(v->v instanceof ButtonWidget w&&(w.getMessage().getString().equals("Ready")||w.getMessage().getString().equals("Cancel ready"))).map(v->(ButtonWidget)v).findFirst().orElseThrow();b.onPress();}
 public void onInitializeClient(){if(!Boolean.getBoolean("gravesandglory.campChecks"))return;
  ClientTickEvents.END_CLIENT_TICK.register(c->{try{
   if(c.getOverlay()!=null)return;if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
   if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;tick++;
   if(tick==20)server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());b.campaign.returnLobby();b.campaign.profiles.put(p.getUuid(),new Profile());b.campaign.profile(p.getUuid()).gold=300;b.campaign.collectionsReady.add(p.getUuid());b.campaign.importAllowed.remove(p.getUuid());b.campaign.save(p.getUuid());
    long npcs=0;for(var e:b.world.iterateEntities())if(e.getCommandTags().contains(CampWorld.TAG)&&e instanceof net.minecraft.entity.passive.VillagerEntity)npcs++;
    check("four-interactive-stewards",npcs==4);check("no-camp-weapons",p.getInventory().isEmpty());});
   if(tick==45){c.setScreen(null);check("battle-hud-off",GravesAndGloryClient.inCamp()&&!GravesAndGloryClient.battleActive());CommandMode.begin("attack");check("orders-disabled-in-camp",!CommandMode.active());}
   if(tick==70){shot(c,"courtyard"); // the camp's slips (UI Task 8b-2): drawn where UiMath.campHud puts them, the keys on ink chips
    var w=c.getWindow();var want=UiMath.campHud(w.getFramebufferWidth(),w.getFramebufferHeight(),(int)w.getScaleFactor(),UiCanvas.hudK());var rows=GravesAndGloryClient.campRowsDrawn;
    boolean placed=GravesAndGloryClient.campRectsDrawn.size()==2&&want.keySet().stream().allMatch(k->Arrays.equals(want.get(k),GravesAndGloryClient.campRectsDrawn.get(k)));
    boolean words=rows.size()==4&&rows.get(2).equals(List.of(GravesAndGloryClient.hint("menu"),"War table"))&&rows.get(3).equals(List.of(GravesAndGloryClient.hint("use"),"on a station to interact"))&&rows.get(1).get(0).startsWith(CampaignClient.profile.gold+" gold");
    System.out.println("[camp-check] slips "+GravesAndGloryClient.campRectsDrawn.keySet()+" rows "+rows);check("hint-slip",placed&&words);}
   if(tick==80)c.setScreen(new CampScreen("home"));if(tick==95)shot(c,"guide");
   if(tick==100)CampScreen.open("recruit");if(tick==115)shot(c,"recruitment");if(tick==118)server(c,b->{liveRoll=b.campaign.rollRng;b.campaign.rollRng=Phase2Checks.Engine.fixed(86);}); /* a fixed draw: the Gilded pool at 86 is its first Rare, a troop the fresh collection lacks, so the roll always opens the reveal panel (a duplicate does not) */ if(tick==120)press(c,"Enlist");
   if(tick==150){var pr=CampaignClient.profile;check("server-confirmed-recruit",pr.gold==200&&pr.owned.values().stream().mapToInt(o->o.copies).sum()+pr.shards.values().stream().mapToInt(Integer::intValue).sum()==Profile.commons().size()+1); /* one roll: a new troop */shot(c,"reward-reveal");server(c,b->{if(liveRoll!=null)b.campaign.rollRng=liveRoll;});}
   /* An owned troop drawn again through the recruit page: fixed(0) draws the Gilded pool's first Common, the Order Squire every fresh collection owns. The duplicate raises it to star 2; at full stars the next copy turns into Gilded shards. */
   if(tick==152)server(c,b->{var pr=b.campaign.profile(c.player.getUuid());var o=pr.owned.get("knight");dupCopies=o==null?-1:o.copies;dupGold=pr.gold;b.campaign.rollRng=Phase2Checks.Engine.fixed(0);});
   if(tick==155)press(c,"Enlist");
   if(tick==165)server(c,b->{var id=c.player.getUuid();var pr=b.campaign.profile(id);var o=pr.owned.get("knight");
    check("duplicate-roll",dupCopies==1&&o!=null&&o.copies==2&&o.stars==2&&pr.gold==dupGold-Spec.RECRUITMENT[0].rollCost()&&b.campaign.notices.getOrDefault(id,"").contains("rises to star 2"));
    if(o!=null){o.stars=RosterMath.maxStars();o.copies=4;}shardsBefore=pr.shards.getOrDefault("gilded",0);dupGold=pr.gold;b.campaign.save(id);b.campaign.rollRng=Phase2Checks.Engine.fixed(0);});
   if(tick==168)press(c,"Enlist");
   if(tick==172)server(c,b->{var id=c.player.getUuid();var pr=b.campaign.profile(id);var o=pr.owned.get("knight");
    check("shard-roll",o!=null&&o.copies==4&&o.stars==RosterMath.maxStars()&&pr.shards.getOrDefault("gilded",0)==shardsBefore+RosterMath.rarityRow("common").dismantleShards()&&pr.gold==dupGold-Spec.RECRUITMENT[0].rollCost()&&b.campaign.notices.getOrDefault(id,"").contains("full stars"));
    if(liveRoll!=null)b.campaign.rollRng=liveRoll;});
   if(tick==175){CampScreen.open("friends");press(c,"Create party");}
   if(tick==200){check("party-packet",CampaignClient.social.get("party").getAsBoolean());shot(c,"friends");press(c,"Create private room");}
   if(tick==225){server(c,b->{var r=b.campaign.social.room(c.player.getUuid());if(r!=null)r.map=b.campaign.map;});CampScreen.open("room");check("room-packet",!CampaignClient.social.get("room").getAsString().isEmpty());}
   if(tick==245)shot(c,"room");
   if(tick==260){toggleReady(c);}
   if(tick==280){press(c,"Start together");}
   if(tick==310){check("room-started",GravesAndGloryClient.battleActive());server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());check("room-seat",b.squads.get(p.getUuid()).seated&&!b.campaign.activeRoom.isEmpty());check("room-bot-count",b.squads.values().stream().filter(s->s.seated).count()==4);});}
   if(tick==440)shot(c,"battle-hud");
   if(tick==460)server(c,b->{var id=c.player.getUuid();b.roundStarted=b.time-181*20;b.campaign.joinedAt.put(id,b.roundStarted);b.campaign.finish(b.squads.get(id).team);check("party-survives-results",b.campaign.social.party(id)!=null);check("room-survives-results",b.campaign.social.room(id)!=null&&!b.campaign.social.room(id).playing);check("progress-earned",b.campaign.profile(id).progression.xp==150);});
   if(tick==480){check("results-screen",c.currentScreen instanceof CampScreen s&&s.page.equals("results"));check("results-confirmed-rewards",CampaignClient.result.get("gold").getAsInt()==120&&CampaignClient.result.get("xp").getAsInt()==150&&CampaignClient.result.get("saved").getAsBoolean());shot(c,"results");}
   if(tick==490){check("hud-clears-after-match",GravesAndGloryClient.inCamp());CampScreen.open("contracts");}
   if(tick==510){shot(c,"contracts");press(c,"Claim");}
   if(tick==535){check("daily-claim",CampaignClient.profile.progression.claims.contains("daily:0"));int gold=CampaignClient.profile.gold;press(c,"Claim");server(c,b->check("claim-ledger",b.campaign.profile(c.player.getUuid()).gold==gold));}
   if(tick==560)CampScreen.open("chapter");if(tick==580)shot(c,"chapter");
   if(tick==600){CampScreen.open("room");toggleReady(c);}
   if(tick==625){press(c,"Start together");}
   if(tick==655){check("rematch-with-same-room",GravesAndGloryClient.battleActive());server(c,b->b.campaign.finish(-1));}
   if(tick==680){org.lwjgl.glfw.GLFW.glfwSetWindowSize(c.getWindow().getHandle(),854,480);CampScreen.open("home");}
   if(tick==710){shot(c,"small-guide");CampScreen.open("recruit");}
   if(tick==730){shot(c,"small-recruitment");CampScreen.open("results");check("short-match-no-rewards-report",CampaignClient.result.get("gold").getAsInt()==0&&CampaignClient.result.get("xp").getAsInt()==0);}
   if(tick==740)shot(c,"small-results");
   if(tick==750){check("suite",!failed);c.setScreen(new CampScreen("home"));server(c,b->{var path=b.server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("gravesandglory-parties.json");try{var saved=Campaign.JSON.fromJson(java.nio.file.Files.readString(path),SocialHub.class);saved.restore();check("party-readback",saved.party(c.player.getUuid())!=null);var profile=Campaign.JSON.fromJson(java.nio.file.Files.readString(b.campaign.directory.resolve(c.player.getUuid()+".json")),Profile.class);check("profile-readback",profile.progression.claims.contains("daily:0")&&profile.progression.xp>=225);}catch(Exception e){check("readback",false);}});}
   if(tick==780){c.setScreen(null);server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());b.relocate(p,22,66,CampaignMaps.LOBBY_Z-7,-145,5);GravesAndGlory.openCamp(p,"recruit");});}
   if(tick==800){check("station-network-route",c.currentScreen instanceof CampScreen screen&&screen.page.equals("recruit"));c.setScreen(null);}
   if(tick==820)shot(c,"recruiter-station");
   if(tick==850){server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());b.campaign.toLobby(p);
        var profile=b.campaign.profile(p.getUuid());profile.progression.xp=250;int gold=profile.gold;var tmp=b.campaign.directory.resolve(p.getUuid()+".tmp");
        try{java.nio.file.Files.createDirectory(tmp);b.campaign.progressCommand(p,"progress:founders:1");var after=b.campaign.profile(p.getUuid());check("failed-save-rollback",after.gold==gold&&!after.progression.claims.contains("founders:1"));}
        catch(Exception e){check("failed-save-fixture",false);}finally{try{java.nio.file.Files.deleteIfExists(tmp);}catch(Exception ignored){}b.campaign.profile(p.getUuid()).progression.xp=225;b.campaign.save(p.getUuid());}
       });c.setScreen(new CampScreen("home"));}
   if(tick==880)GravesAndGloryClient.send("social:map:"+((CampaignClient.social.get("map").getAsInt()+1)%Spec.MAPS.length));
   if(tick==905)GravesAndGloryClient.send("social:ready");
   if(tick==930)GravesAndGloryClient.send("social:start");
   if(tick==990){check("map-preparation-auto-start",GravesAndGloryClient.battleActive());server(c,b->{b.campaign.finish(-1);b.campaign.social.leaveRoom(c.player.getUuid());});}
   if(tick==1020)server(c,b->b.relocate(b.server.getPlayerManager().getPlayer(c.player.getUuid()),-9.5,65,CampaignMaps.PAD_Z,0,0));
   if(tick==1050)server(c,b->check("gate-pad-countdown",b.campaign.onPad.containsKey(c.player.getUuid())&&b.campaign.padCountdown>=0));
   if(tick==1070)server(c,b->b.campaign.toLobby(b.server.getPlayerManager().getPlayer(c.player.getUuid())));
   if(tick==1100)server(c,b->check("gate-step-off-cancels",!b.campaign.onPad.containsKey(c.player.getUuid())&&b.campaign.padCountdown<0));
   if(tick==1120){check("final-suite",!failed);c.setScreen(new CampScreen("home"));}
  }catch(Exception e){check("client-exception",false);e.printStackTrace();tick++;}});
 }
}
