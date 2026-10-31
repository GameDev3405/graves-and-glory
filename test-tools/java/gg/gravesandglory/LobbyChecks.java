package gg.gravesandglory;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/** Phase 1 suite ui-lobby (plan 3.4, 4C.3): the War Table lobby in a real client against its integrated server. The one human
 *  commander opens the council from camp; a stand-in second commander (a FakePlayer the server lists as online, named Aldric)
 *  joins the room so the page can be seen as a guest. Checks: the seats with bot fill on and off, the host's map pick, the
 *  ten-second countdown started and stopped, the guest's locked orders, and a round started by the countdown with bots in the
 *  empty seats. Shots (run/screenshots/p1-lobby-*.png): 1920x1080 and 2560x1440, borderless, GUI Auto, host and guest, with bots
 *  and with the countdown running. */
final class LobbyChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    volatile int stage;volatile UUID player,guest;volatile boolean serverRunning;volatile int botsDawn=-1,botsDusk=-1,seatedHumans=-1;
    int mark=-1,waitFrom,oldW,oldH,oldGui,map0=-1;boolean finished;ServerPlayerEntity stand;

    public void serverTick(GravesAndGlory.Battle b){
        serverRunning=b.running;
        if(b.running&&botsDawn<0){int d=0,k=0,h=0;for(var s:b.squads.values()){if(s.bot){if(s.team==0)d++;else k++;}else if(s.seated)h++;}botsDawn=d;botsDusk=k;seatedHumans=h;}
    }
    int since(int tick){if(mark!=stage){mark=stage;waitFrom=tick;}return tick-waitFrom;}
    static WarTableLobby lobby(MinecraftClient c){return c.currentScreen instanceof CampScreen s&&s.page.equals("lobby")?s.lobby:null;}
    static JsonObject social(){return CampaignClient.social;}
    static boolean flag(String k){return social().has(k)&&social().get(k).getAsBoolean();}
    static int num(String k){return social().has(k)?social().get(k).getAsInt():-1;}
    static String str(String k){return social().has(k)?social().get(k).getAsString():"";}
    static boolean press(MinecraftClient c,String name){var l=lobby(c);var b=l==null?null:l.button(name);if(b==null||!b.active)return false;WarTableRecruit.press(b);return true;}
    static boolean active(MinecraftClient c,String name){var l=lobby(c);var b=l==null?null:l.button(name);return b!=null&&b.active;}
    static String kinds(int team){StringBuilder s=new StringBuilder();for(var seat:WarTableLobby.seats(team))s.append(s.length()==0?"":",").append(seat.kind());return s.toString();}
    void next(){stage++;}
    void fail(MinecraftClient c,String id,String why){report(id,false,why);end(c);}

    public void step(MinecraftClient c,int tick){
        if(finished)return;
        if(tick>9000){fail(c,"p1-lobby-timeout","stuck in stage "+stage);return;}
        int k=since(tick);
        switch(stage){
            case 0->{ // at camp, no round, no room; the window 1920x1080 borderless at GUI Auto
                if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
                c.setScreen(null);window(c,1920,1080,0,true);stage=-1;
                server(c,b->{if(b.running)b.campaign.returnLobby();var s=b.campaign.social;if(s.room(player)!=null)s.leaveRoom(player);s.revision++;stage=1;});
            }
            case 1->{if(GravesAndGloryClient.running||!str("room").isEmpty()){if(k>300)fail(c,"p1-lobby-camp","still in a round or a room 300 ticks after the return to camp");return;}CampScreen.open("lobby");next();}
            case 2->{ // the page asks for a room; the server opens one three a side with bots on
                if(str("room").isEmpty()||num("size")!=SocialHub.LOBBY_SIZE||lobby(c)==null){if(k>200)fail(c,"p1-lobby-open","no 3v3 room 200 ticks after the lobby page opened (room="+str("room")+", size="+num("size")+")");return;}
                if(k<30)return;
                String dawn=kinds(0),dusk=kinds(1);var t=WarTableLobby.texts();
                boolean ok=dawn.equals("human,bot,bot,closed")&&dusk.equals("bot,bot,bot,closed")&&flag("host")&&flag("fill")&&"BOT".equals(t.get("seat-1-0"))&&active(c,"fill")&&active(c,"map-next")&&active(c,"mode-domination");
                report("p1-lobby-seats",ok,"Dawn "+dawn+", Dusk "+dusk+", seat-1-0 reads "+t.get("seat-1-0")+", host="+flag("host")+", fill="+flag("fill")+" (want human,bot,bot,closed / bot,bot,bot,closed, BOT, the host's orders live)");
                shot(c,"lobby-1080-host-bots");press(c,"fill");next();
            }
            case 3->{ // bot fill off: the empty seats are open and one ready commander does not count down
                if(flag("fill")){if(k>100)fail(c,"p1-lobby-fill-off","fill still on 100 ticks after the host's toggle");return;}
                if(k<10)return;
                report("p1-lobby-fill-off",kinds(0).equals("human,open,open,closed")&&kinds(1).equals("open,open,open,closed"),"Dawn "+kinds(0)+", Dusk "+kinds(1)+" with fill off");
                press(c,"ready");next();
            }
            case 4->{
                if(!WarTableLobby.myReady()){if(k>100)fail(c,"p1-lobby-fill-off-holds","not ready 100 ticks after pressing Ready");return;}
                if(k<40)return;
                String status=WarTableLobby.texts().getOrDefault("status","");
                report("p1-lobby-fill-off-holds",num("countdown")==0&&str("waiting").equals("Fill both teams or enable bots.")&&status.contains("Fill both teams"),"ready alone with fill off for 40 ticks: countdown="+num("countdown")+", waiting=\""+str("waiting")+"\", status=\""+status+"\"");
                press(c,"ready");next();
            }
            case 5->{if(WarTableLobby.myReady()||k<10){if(k>100)fail(c,"p1-lobby-stand-down","still ready 100 ticks after Stand down");return;}press(c,"fill");next();}
            case 6->{if(!flag("fill")||k<10){if(k>100)fail(c,"p1-lobby-fill-on","fill still off");return;}map0=num("map");press(c,"map-next");next();}
            case 7->{ // the host's map pick reaches the snapshot
                if(num("map")==map0){if(k>100)fail(c,"p1-lobby-host-map","map still "+map0+" 100 ticks after Next map");return;}
                if(k<10)return;
                report("p1-lobby-host-map",num("map")==(map0+1)%Spec.MAPS.length&&WarTableLobby.texts().getOrDefault("map","").equals(Spec.MAPS[num("map")].id()),"map "+map0+" -> "+num("map")+", the note reads "+WarTableLobby.texts().get("map"));
                press(c,"map-prev");next();
            }
            case 8->{if(num("map")!=map0||k<10){if(k>100)fail(c,"p1-lobby-host-map-back","map not back");return;}press(c,"ready");next();}
            case 9->{ // everyone ready: the countdown runs and the sheet says so
                if(num("countdown")<=0){if(k>100)fail(c,"p1-lobby-countdown-start","no countdown 100 ticks after Ready (waiting=\""+str("waiting")+"\")");return;}
                if(k<4)return;
                String cd=WarTableLobby.texts().getOrDefault("status","");
                report("p1-lobby-countdown-start",num("countdown")>=8&&num("countdown")<=10&&cd.startsWith("Battle begins in")&&WarTableLobby.texts().containsKey("countdown"),"countdown "+num("countdown")+" s, the sheet reads \""+cd+"\"");
                shot(c,"lobby-1080-host-countdown");press(c,"ready");next();
            }
            case 10->{ // standing down stops it
                if(num("countdown")!=0||WarTableLobby.myReady()||!CampaignClient.notice.startsWith("Countdown stopped")){if(k>100)fail(c,"p1-lobby-countdown-cancel","countdown "+num("countdown")+", ready="+WarTableLobby.myReady()+", notice \""+CampaignClient.notice+"\" 100 ticks after Stand down");return;}
                report("p1-lobby-countdown-cancel",!WarTableLobby.texts().containsKey("countdown"),"countdown 0, notice \""+CampaignClient.notice+"\", no countdown line drawn");
                stage=-1;
                server(c,b->{ // the stand-in guest joins Dusk, picks Graveborn, and takes the host's seat
                    var p=new Phase1Checks.Listener(b.world);stand=p;b.server.getPlayerManager().getPlayerList().add(p);guest=p.getUuid();var s=b.campaign.social;s.names.put(guest,"Aldric");
                    s.presence(new HashSet<>(b.server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList()),System.currentTimeMillis());
                    b.campaign.profile(guest).commander=Spec.CLASSES[Spec.CLASSES.length-1].id();
                    s.joinRoom(guest,s.room(player).code);s.house(guest,"graveborn",RosterMath.fieldableHouses());s.handHost(player,guest);s.ready(guest);stage=11;});
            }
            case 11->{ // the guest's view: every order locked, the seats and Ready still live
                if(flag("host")||WarTableLobby.commanders().size()<2){if(k>100)fail(c,"p1-lobby-guest","still host or alone 100 ticks after the hand-over");return;}
                if(k<20)return;
                boolean locked=!active(c,"mode-domination")&&!active(c,"map-next")&&!active(c,"map-prev")&&!active(c,"fill")&&!active(c,"size-up")&&!active(c,"size-down");
                String orders=WarTableLobby.texts().getOrDefault("orders","");
                report("p1-lobby-guest",locked&&active(c,"ready")&&kinds(1).startsWith("human,bot,bot")&&orders.contains("Aldric"),"orders locked="+locked+", Ready live="+active(c,"ready")+", Dusk "+kinds(1)+", the note says \""+orders+"\"");
                shot(c,"lobby-1080-guest");window(c,2560,1440,0,true);next();
            }
            case 12->{
                if(k<20)return;var w=c.getWindow();
                report("p1-lobby-1440",w.getFramebufferWidth()==2560&&w.getFramebufferHeight()==1440,"framebuffer "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+", canvas k "+UiCanvas.k());
                shot(c,"lobby-1440-guest");press(c,"ready");next();
            }
            case 13->{
                if(num("countdown")<=0){if(k>100)fail(c,"p1-lobby-guest-countdown","no countdown with both ready");return;}
                if(k<4)return;shot(c,"lobby-1440-guest-countdown");press(c,"ready");stage=-1;
                server(c,b->{b.campaign.social.handHost(guest,player);stage=14;});
            }
            case 14->{
                if(!flag("host")||num("countdown")!=0){if(k>100)fail(c,"p1-lobby-host-back","host not back");return;}
                if(k<20)return;shot(c,"lobby-1440-host-bots");press(c,"ready");stage=-1;server(c,b->{b.campaign.social.ready(guest);stage=15;});
            }
            case 15->{
                if(num("countdown")<=0){if(k>100)fail(c,"p1-lobby-host-countdown","no countdown at 1440");return;}
                if(k<4)return;shot(c,"lobby-1440-host-countdown");stage=-1;
                server(c,b->{dropGuest(b);stage=16;}); // the guest leaves; the room stands down
            }
            case 16->{if(WarTableLobby.commanders().size()!=1||WarTableLobby.myReady()||k<10){if(k>100)fail(c,"p1-lobby-guest-leaves","guest still seated");return;}press(c,"ready");next();}
            case 17->{ // the countdown runs out and the round starts with bots in the five empty seats
                if(!serverRunning||botsDawn<0){if(k>900)fail(c,"p1-lobby-bot-fill-on","no round 900 ticks after the last Ready (countdown "+num("countdown")+", notice \""+CampaignClient.notice+"\")");return;}
                report("p1-lobby-bot-fill-on",botsDawn==2&&botsDusk==3&&seatedHumans==1,"the round the countdown started fields "+seatedHumans+" commander and bots Dawn "+botsDawn+", Dusk "+botsDusk+" (want 1, 2, 3), "+k+" ticks after Ready");
                end(c);
            }
            default->{}
        }
    }
    void dropGuest(GravesAndGlory.Battle b){
        if(guest==null)return;var s=b.campaign.social;if(s.room(guest)!=null)s.remove(s.room(guest),Set.of(guest));s.names.remove(guest);s.revision++;
        if(stand!=null)b.server.getPlayerManager().getPlayerList().remove(stand);stand=null;
    }
    void end(MinecraftClient c){
        if(finished)return;finished=true;
        server(c,b->{dropGuest(b);if(b.running)b.campaign.returnLobby();var s=b.campaign.social;if(player!=null&&s.room(player)!=null){var r=s.room(player);r.playing=false;s.leaveRoom(player);}s.revision++;});
        c.setScreen(null);if(oldW>0)window(c,oldW,oldH,oldGui);
        finish(c,"ui-lobby");
    }
    @Override public void cleanup(MinecraftClient c){if(!finished)end(c);}
}
