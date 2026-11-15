package gg.gravesandglory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.net.ServerSocket;
import java.util.*;

/** Two real development clients; loopback connection, private disposable saves. */
public final class CampNetworkChecks implements ClientModInitializer {
 int clock,step,stageTicks;boolean started,rejoined,failed;UUID friend;String address;
 final Path control=Path.of(System.getProperty("gravesandglory.networkRoot","work/network"));
 void check(String id,boolean pass){MeltyTest.report("friends-"+id,pass,id);System.out.println("[friends-check] "+id+"="+pass);if(!pass)failed=true;}
 void connect(MinecraftClient c){ConnectScreen.connect(new FrontMenuScreen(),c,ServerAddress.parse(address),new ServerInfo("Isolated friends test",address, ServerInfo.ServerType.OTHER), false, null);}
 void server(MinecraftClient c,java.util.function.Consumer<GravesAndGlory.Battle> fn){c.getServer().execute(()->{try{fn.accept(GravesAndGlory.battle);}catch(Exception e){check("server-exception",false);e.printStackTrace();}});}
 public void onInitializeClient(){boolean host=Boolean.getBoolean("gravesandglory.networkHost"),join=Boolean.getBoolean("gravesandglory.networkJoin");if(!host&&!join)return;
  ClientTickEvents.END_CLIENT_TICK.register(c->{try{
   clock++;if(join&&Files.exists(control.resolve("done.txt"))){check("extended-friend-suite",!failed);c.scheduleStop();return;}if(c.getOverlay()!=null)return;
   if(host&&step==15&&c.player==null&&++stageTicks>80){GravesAndGloryClient.startCamp(c,false);step=16;return;}
   if(!started){if(!(c.currentScreen instanceof FrontMenuScreen)||clock<60)return;started=true;
    if(host)GravesAndGloryClient.startCamp(c,false);else {address=Files.readString(control.resolve("address.txt")).trim();connect(c);}return;}
   if(join){
    if(step==8&&c.player==null){check("host-stop-disconnects-cleanly",!GravesAndGloryClient.running);step=9;return;}
    if(step==9&&Files.exists(control.resolve("restart-ready.txt"))){address=Files.readString(control.resolve("address.txt")).trim();connect(c);step=10;return;}
    if(step==10&&c.player!=null&&GravesAndGloryClient.ready){check("restart-party-retained",CampaignClient.social.getAsJsonArray("members").size()==2);check("restart-room-cleared",CampaignClient.social.get("room").getAsString().isEmpty());Files.writeString(control.resolve("restart-joined.txt"),"ready");step=11;return;}
    if(step==11&&Files.exists(control.resolve("visitor-disconnect.txt"))){c.world.disconnect();c.disconnect(new FrontMenuScreen());step=12;return;}
    if(step==12&&Files.exists(control.resolve("visitor-ready.txt"))){connect(c);step=13;stageTicks=0;return;}
    if(step==13&&c.player!=null&&GravesAndGloryClient.ready&&++stageTicks>30){check("full-match-waits-in-camp",CampaignClient.waiting&&GravesAndGloryClient.inCamp());CommandMode.begin("attack");check("waiting-orders-disabled",!CommandMode.active());c.setScreen(new CampScreen("recruit"));check("waiting-camp-menu",c.currentScreen instanceof CampScreen);GravesAndGloryClient.send("social:room");step=14;stageTicks=0;return;}
    if(step==14&&++stageTicks>40){check("waiting-room-created",!CampaignClient.social.get("room").getAsString().isEmpty());Files.writeString(control.resolve("visitor-checked.txt"),"checked");step=15;return;}
    if(step==15&&c.player!=null&&GravesAndGloryClient.ready&&!GravesAndGloryClient.running){check("visitor-no-match-report",!CampaignClient.result.has("id"));Files.writeString(control.resolve("visitor-returned.txt"),"returned");step=16;return;}
    if(step==4&&c.player==null&&++stageTicks>80){connect(c);step=5;return;}
    if(c.player==null||!GravesAndGloryClient.ready)return;
    if(step==0&&CampaignClient.social.has("inviter")&&!CampaignClient.social.get("inviter").getAsString().isEmpty()){GravesAndGloryClient.send("social:accept");step=1;return;}
    if(step==1&&CampaignClient.social.has("room")&&!CampaignClient.social.get("room").getAsString().isEmpty()){check("join-room-same-team",CampaignClient.social.getAsJsonArray("commanders").size()==2);GravesAndGloryClient.send("social:ready");step=2;return;}
    if(step==2&&GravesAndGloryClient.battleActive()){check("friend-enters-battle",true);step=3;stageTicks=0;return;}
    if(step==3&&++stageTicks>100){c.world.disconnect();c.disconnect(new FrontMenuScreen());step=4;stageTicks=0;return;}
    if(step==5&&GravesAndGloryClient.battleActive()){check("friend-reconnects-to-battle",true);step=6;return;}
    if(step==6&&!GravesAndGloryClient.running){check("friend-returns-with-party",CampaignClient.social.get("party").getAsBoolean()&&!CampaignClient.social.get("room").getAsString().isEmpty());GravesAndGloryClient.send("social:ready");step=7;return;}
    if(step==7&&GravesAndGloryClient.battleActive()){check("friend-rematch",true);step=8;}
    return;
   }
   if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
   if(step==0){step=1;server(c,b->{try{int port;try(var socket=new ServerSocket(0)){port=socket.getLocalPort();}check("test-lan-open",b.server.openToLan(GameMode.ADVENTURE,false,port));Files.createDirectories(control);Files.writeString(control.resolve("address.txt"),"127.0.0.1:"+port);b.campaign.social=new SocialHub();b.campaign.saveSocial();}catch(Exception e){throw new RuntimeException(e);}});return;}
   if(step==1&&c.getServer().getPlayerManager().getPlayerList().size()==2){step=2;GravesAndGloryClient.send("social:create");stageTicks=0;return;}
   if(step==2&&++stageTicks==20){GravesAndGloryClient.send("social:invite:CampFriend");step=3;return;}
   if(step==3&&CampaignClient.social.getAsJsonArray("members").size()==2){step=4;check("invite-accept-roundtrip",true);GravesAndGloryClient.send("social:room");return;}
   if(step==4&&CampaignClient.social.getAsJsonArray("commanders").size()==2){step=5;server(c,b->{var r=b.campaign.social.room(c.player.getUuid());r.map=b.campaign.map;friend=r.members.stream().filter(id->!id.equals(c.player.getUuid())).findFirst().orElseThrow();check("party-teams",r.teams.get(friend).equals(r.teams.get(c.player.getUuid())));});GravesAndGloryClient.send("social:ready");return;}
   if(step==5&&CampaignClient.social.getAsJsonArray("commanders").asList().stream().allMatch(e->e.getAsJsonObject().get("ready").getAsBoolean())){step=6;GravesAndGloryClient.send("social:start");return;}
   if(step==6&&GravesAndGloryClient.battleActive()){check("host-enters-battle",true);step=7;return;}
   if(step==7&&c.getServer().getPlayerManager().getPlayerList().size()==1){step=8;server(c,b->{check("disconnect-holds-army",b.campaign.vacated.containsKey(friend));check("disconnect-keeps-party",b.campaign.social.party(friend)!=null);});return;}
   if(step==8&&c.getServer().getPlayerManager().getPlayerList().size()==2){step=9;stageTicks=0;return;}
   if(step==9&&++stageTicks==50){step=10;server(c,b->{check("reconnect-reclaims-army",b.squads.containsKey(friend)&&!b.squads.get(friend).bot&&b.squads.get(friend).seated&&!b.campaign.vacated.containsKey(friend));check("whole-party-active",b.squads.get(friend).team==b.squads.get(c.player.getUuid()).team);b.roundStarted=b.time-181*20;for(UUID id:b.campaign.participants)b.campaign.joinedAt.put(id,b.roundStarted);b.campaign.finish(b.squads.get(friend).team);});return;}
   if(step==10&&!GravesAndGloryClient.running){step=11;check("host-party-results",CampaignClient.social.getAsJsonArray("members").size()==2);GravesAndGloryClient.send("social:ready");return;}
   if(step==11&&CampaignClient.social.getAsJsonArray("commanders").asList().stream().allMatch(e->e.getAsJsonObject().get("ready").getAsBoolean())){step=12;GravesAndGloryClient.send("social:start");return;}
   if(step==12&&GravesAndGloryClient.battleActive()){check("host-rematch",true);step=13;stageTicks=0;return;}
   if(step==13&&++stageTicks==80){step=14;stageTicks=0;server(c,b->{b.campaign.finish(-1);check("party-after-two-matches",b.campaign.social.party(friend).members.size()==2);try{var saved=Campaign.JSON.fromJson(Files.readString(b.server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("gravesandglory-parties.json")),SocialHub.class);saved.restore();check("two-member-disk-persistence",saved.party(friend).members.size()==2);}catch(Exception e){check("disk-read",false);}check("host-suite",!failed);});}
   if(step==14&&++stageTicks>40){c.world.disconnect();c.disconnect(new FrontMenuScreen());step=15;stageTicks=0;return;}
   if(step==16){step=17;server(c,b->{try{check("server-restart-party",b.campaign.social.party(friend)!=null&&b.campaign.social.party(friend).members.size()==2);check("server-restart-no-rooms",b.campaign.social.rooms.isEmpty());int port;try(var socket=new ServerSocket(0)){port=socket.getLocalPort();}check("reopen-lan",b.server.openToLan(GameMode.ADVENTURE,false,port));Files.writeString(control.resolve("address.txt"),"127.0.0.1:"+port);Files.writeString(control.resolve("restart-ready.txt"),"ready");}catch(Exception e){throw new RuntimeException(e);}});return;}
   if(step==17&&Files.exists(control.resolve("restart-joined.txt"))){step=18;server(c,b->{b.campaign.social.disband(c.player.getUuid());b.campaign.saveSocial();try{Files.writeString(control.resolve("visitor-disconnect.txt"),"disconnect");}catch(Exception e){throw new RuntimeException(e);}});return;}
   if(step==18&&c.getServer().getPlayerManager().getPlayerList().size()==1){GravesAndGloryClient.send("camp:practice");step=19;return;}
   if(step==19&&GravesAndGloryClient.battleActive()){Files.writeString(control.resolve("visitor-ready.txt"),"ready");step=20;return;}
   if(step==20&&Files.exists(control.resolve("visitor-checked.txt"))){step=21;server(c,b->{check("full-match-no-extra-seat",b.squads.values().stream().filter(s->s.seated).count()==2&&!b.squads.get(friend).seated);check("visitor-not-participant",!b.campaign.participants.contains(friend));var room=b.campaign.social.room(friend);check("visitor-room-independent",room!=null&&!room.playing);int gold=b.campaign.profile(friend).gearShards,xp=b.campaign.profile(friend).progression.xp;b.roundStarted=b.time-181*20;b.campaign.joinedAt.put(c.player.getUuid(),b.roundStarted);b.campaign.finish(0);check("visitor-no-reward",b.campaign.profile(friend).gearShards==gold&&b.campaign.profile(friend).progression.xp==xp);b.campaign.social.createRoom(c.player.getUuid());check("two-live-waiting-rooms",b.campaign.social.rooms.size()==2);b.campaign.social.configure(c.player.getUuid(),3,b.campaign.map,false,Spec.MAPS.length);check("other-room-unchanged",room.size==2&&room.fill);});return;}
   if(step==21&&Files.exists(control.resolve("visitor-returned.txt"))){check("extended-host-suite",!failed);FilesWrite.done(control);step=22;}
  }catch(Exception e){check("client-exception",false);e.printStackTrace();}});
 }
 static class FilesWrite{static void done(Path p){try{Files.writeString(p.resolve("done.txt"),"finished");}catch(Exception e){throw new RuntimeException(e);}}}
}

