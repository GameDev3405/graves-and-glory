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
 *  empty seats; the House pick (Kort, 2026-10-31): the House chips' names whole, Ready refused for a House the banner lacks, and the
 *  round fielding only the picked House (the banner carries one Graveborn revenant that a Gilded pick leaves home); the host-only
 *  Pass host handing the seat to the guest; the lobby and the troop sheet at canvas scale 3 at 2560x1440 and 2 at 1080p. Shots (run/screenshots/p1-lobby-*.png): 1920x1080 and 2560x1440, borderless, GUI Auto, host and guest, with bots
 *  and with the countdown running. */
final class LobbyChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    volatile int stage;volatile UUID player,guest;volatile boolean serverRunning;volatile int botsDawn=-1,botsDusk=-1,seatedHumans=-1;volatile List<String> fielded;List<String> savedBanner;
    int mark=-1,waitFrom,oldW,oldH,oldGui,map0=-1;boolean finished;ServerPlayerEntity stand;

    public void serverTick(GravesAndGlory.Battle b){
        serverRunning=b.running;
        if(b.running&&botsDawn<0){int d=0,k=0,h=0;for(var s:b.squads.values()){if(s.bot){if(s.team==0)d++;else k++;}else if(s.seated)h++;}botsDawn=d;botsDusk=k;seatedHumans=h;for(var s:b.squads.values())if(!s.bot&&s.owner!=null&&s.owner.equals(player))fielded=new ArrayList<>(s.banner);}
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
    /** Every House chip draws its House's whole first word, and the row stays inside the sheet's text width; the canvas scale is want. */
    String chips(MinecraftClient c,int want,boolean[] ok){
        var l=lobby(c);var t=WarTableLobby.texts();StringBuilder sb=new StringBuilder();boolean pass=l!=null&&UiCanvas.k()==want;int left=Integer.MAX_VALUE,right=0;var sheet=WarTableLobby.rects().get("sheet");
        for(String id:RosterMath.fieldableHouses()){String name=t.getOrDefault("house-"+id+"-name","");var b=l==null?null:l.button("house-"+id);
            var r=WarTableLobby.rects().get("house-"+id+"-name");pass&=name.equals(WarTableLobby.chipName(id))&&!name.contains("...")&&b!=null&&r!=null;if(r!=null){left=Math.min(left,r[0]);right=Math.max(right,r[0]+r[2]);}
            sb.append(name).append(t.getOrDefault("house-"+id+"-seal","").equals("true")?"(seal) ":" ");}
        pass&=right-left<=WarTableLobby.TEXT_W&&sheet!=null&&left>=sheet[0]&&right<=sheet[0]+sheet[2];ok[0]=pass;
        return "chips "+sb.toString().trim()+", names span "+(right-left)+" canvas px of "+WarTableLobby.TEXT_W+", inside the sheet "+(sheet!=null&&left>=sheet[0]&&right<=sheet[0]+sheet[2])+", canvas k "+UiCanvas.k()+" (want "+want+")";
    }
    void fail(MinecraftClient c,String id,String why){report(id,false,why);end(c);}

    public void step(MinecraftClient c,int tick){
        if(finished)return;
        if(tick>9000){fail(c,"p1-lobby-timeout","stuck in stage "+stage);return;}
        int k=since(tick);
        switch(stage){
            case 0->{ // at camp, no round, no room; the window 1920x1080 borderless at GUI Auto
                if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
                c.setScreen(null);window(c,1920,1080,0,true);stage=-1;
                server(c,b->{if(b.running)b.campaign.returnLobby();var s=b.campaign.social;if(s.room(player)!=null)s.leaveRoom(player);
                    var pr=b.campaign.profile(player);savedBanner=new ArrayList<>(pr.banner);pr.owned.computeIfAbsent("revenant",x->new Profile.Owned());
                    var bn=new ArrayList<>(RosterMath.starterBanner());bn.set(bn.size()-1,"revenant");pr.banner=bn;s.revision++;stage=1;});
            }
            case 1->{if(GravesAndGloryClient.running||!str("room").isEmpty()){if(k>300)fail(c,"p1-lobby-camp","still in a round or a room 300 ticks after the return to camp");return;}CampScreen.open("lobby");next();}
            case 2->{ // the page asks for a room; the server opens one three a side with bots on
                if(str("room").isEmpty()||num("size")!=SocialHub.LOBBY_SIZE||lobby(c)==null){if(k>200)fail(c,"p1-lobby-open","no 3v3 room 200 ticks after the lobby page opened (room="+str("room")+", size="+num("size")+")");return;}
                if(k<30)return;
                String dawn=kinds(0),dusk=kinds(1);var t=WarTableLobby.texts();
                boolean ok=dawn.equals("human,bot,bot,closed")&&dusk.equals("bot,bot,bot,closed")&&flag("host")&&flag("fill")&&"BOT".equals(t.get("seat-1-0"))&&active(c,"fill")&&active(c,"map-next")&&active(c,"mode-domination");
                report("p1-lobby-seats",ok,"Dawn "+dawn+", Dusk "+dusk+", seat-1-0 reads "+t.get("seat-1-0")+", host="+flag("host")+", fill="+flag("fill")+" (want human,bot,bot,closed / bot,bot,bot,closed, BOT, the host's orders live)");
                boolean[] chipOk={false};String ch=chips(c,2,chipOk);
                report("p1-lobby-chips-1080",chipOk[0],ch);
                report("p1-lobby-pass-host-alone",lobby(c).button("pass-host")!=null&&!active(c,"pass-host"),"alone the host's Pass host is "+(lobby(c).button("pass-host")==null?"missing":active(c,"pass-host")?"live":"dull")+" (want dull: no one to pass it to)");
                shot(c,"lobby-1080-host-bots");press(c,"fill");next();
            }
            case 3->{ // bot fill off: the empty seats are open and one ready commander does not count down
                if(flag("fill")){if(k>100)fail(c,"p1-lobby-fill-off","fill still on 100 ticks after the host's toggle");return;}
                if(k<10)return;
                report("p1-lobby-fill-off",kinds(0).equals("human,open,open,closed")&&kinds(1).equals("open,open,open,closed"),"Dawn "+kinds(0)+", Dusk "+kinds(1)+" with fill off");
                press(c,"ready");next();
            }
            case 4->{
                if(!WarTableLobby.myReady()){if(k>100)fail(c,"p1-lobby-fill-off-holds","not ready 100 ticks after pressing Ready");else if(k>=30&&k%30==0)press(c,"ready");return;} // a press right after the fill toggle can be dropped (CampScreen's 300 ms guard) or land on a dull button: press again while still not ready
                if(k<40)return;
                String status=WarTableLobby.texts().getOrDefault("status","");
                boolean holdOk=num("countdown")==0&&str("waiting").equals("Fill both teams or enable bots.")&&status.contains("Fill both teams");
                if(!holdOk&&k<=140)return; // the drawn status follows the snapshot on the next rendered frame
                report("p1-lobby-fill-off-holds",holdOk,"ready alone with fill off for 40 ticks: countdown="+num("countdown")+", waiting=\""+str("waiting")+"\", status=\""+status+"\"");
                press(c,"ready");next();
            }
            case 5->{if(WarTableLobby.myReady()||k<10){if(k>100)fail(c,"p1-lobby-stand-down","still ready 100 ticks after Stand down");return;}press(c,"fill");next();}
            case 6->{if(!flag("fill")||k<10){if(k>100)fail(c,"p1-lobby-fill-on","fill still off");return;}press(c,"house-tidecallers");stage=20;}
            case 20->{ // a House the banner lacks: Ready is dull, the sheet says why, and the server refuses it too
                if(!WarTableLobby.myHouse().equals("tidecallers")||WarTableLobby.myTroops()!=0){if(k>100)fail(c,"p1-lobby-house-refused","house "+WarTableLobby.myHouse()+", troops "+WarTableLobby.myTroops()+" 100 ticks after picking Tidecallers");return;}
                if(k<10)return;
                String status=WarTableLobby.texts().getOrDefault("status","");boolean dull=!active(c,"ready");stage=-1;
                server(c,b->{var so=b.campaign.social;String said=so.ready(player,b.campaign.houseRefusal(so.room(player),player));boolean r=so.room(player).ready.contains(player);
                    report("p1-lobby-house-refused",dull&&status.contains("no Tidecallers troops")&&CampaignClient.notice.contains("no Tidecallers troops")&&!r&&said.contains("no Tidecallers troops"),"Ready "+(dull?"dull":"live")+", notice \""+CampaignClient.notice+"\", the sheet reads \""+status+"\", the server answers \""+said+"\", ready="+r);
                    stage=21;});
            }
            case 21->{if(k<2)return;shot(c,"lobby-1080-host-no-house");press(c,"house-gilded");next();}
            case 22->{if(!WarTableLobby.myHouse().equals("gilded")||WarTableLobby.myTroops()<=0||k<10){if(k>100)fail(c,"p1-lobby-house-gilded","house "+WarTableLobby.myHouse()+", troops "+WarTableLobby.myTroops());return;}
                report("p1-lobby-house-gilded",active(c,"ready")&&WarTableLobby.myTroops()==RosterMath.bannerSize()-1,"Gilded fields "+WarTableLobby.myTroops()+" troops (want "+(RosterMath.bannerSize()-1)+"), Ready "+(active(c,"ready")?"live":"dull"));
                map0=num("map");press(c,"map-next");stage=7;}
            case 7->{ // the host's map pick reaches the snapshot
                if(num("map")==map0){if(k>100)fail(c,"p1-lobby-host-map","map still "+map0+" 100 ticks after Next map");return;}
                if(k<10)return;
                boolean mapOk=num("map")==(map0+1)%Spec.MAPS.length&&WarTableLobby.texts().getOrDefault("map","").equals(Spec.MAPS[num("map")].id());
                if(!mapOk&&k<=100)return; // the drawn note follows the snapshot on the next rendered frame: poll, time out at 100 ticks
                report("p1-lobby-host-map",mapOk,"map "+map0+" -> "+num("map")+", the note reads "+WarTableLobby.texts().get("map"));
                press(c,"map-prev");next();
            }
            case 8->{if(num("map")!=map0||k<10){if(k>100)fail(c,"p1-lobby-host-map-back","map not back");return;}press(c,"ready");next();}
            case 9->{ // everyone ready: the countdown runs and the sheet says so
                if(num("countdown")<=0){if(k>100)fail(c,"p1-lobby-countdown-start","no countdown 100 ticks after Ready (waiting=\""+str("waiting")+"\")");return;}
                if(k<8)return; // CampScreen.action drops a second press within 300 ms
                String cd=WarTableLobby.texts().getOrDefault("status","");
                boolean cdOk=num("countdown")>=8&&num("countdown")<=10&&cd.startsWith("Battle begins in")&&WarTableLobby.texts().containsKey("countdown");
                if(!cdOk&&k<=40&&num("countdown")>=8)return; // the sheet redraws on the next rendered frame: poll while the countdown is still in range
                report("p1-lobby-countdown-start",cdOk,"countdown "+num("countdown")+" s, the sheet reads \""+cd+"\"");
                shot(c,"lobby-1080-host-countdown");press(c,"ready");next();
            }
            case 10->{ // standing down stops it
                if(num("countdown")!=0||WarTableLobby.myReady()||!CampaignClient.notice.startsWith("Countdown stopped")){if(k>100)fail(c,"p1-lobby-countdown-cancel","countdown "+num("countdown")+", ready="+WarTableLobby.myReady()+", notice \""+CampaignClient.notice+"\" 100 ticks after Stand down");return;}
                if(WarTableLobby.texts().containsKey("countdown")&&k<=100)return; // the countdown line leaves the sheet on the next rendered frame
                report("p1-lobby-countdown-cancel",!WarTableLobby.texts().containsKey("countdown"),"countdown 0, notice \""+CampaignClient.notice+"\", no countdown line drawn");
                stage=-1;
                server(c,b->{ // the stand-in guest joins Dusk, picks Graveborn, and takes the host's seat
                    var p=new Phase1Checks.Listener(b.world);stand=p;b.server.getPlayerManager().getPlayerList().add(p);guest=p.getUuid();var s=b.campaign.social;s.names.put(guest,"Aldric");
                    s.presence(new HashSet<>(b.server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList()),System.currentTimeMillis());
                    b.campaign.profile(guest).commander="quartermaster";
                    s.joinRoom(guest,s.room(player).code);s.house(guest,"graveborn",RosterMath.fieldableHouses());s.handHost(player,guest);s.ready(guest);stage=11;});
            }
            case 11->{ // the guest's view: every order locked, the seats and Ready still live
                if(flag("host")||WarTableLobby.commanders().size()<2){if(k>100)fail(c,"p1-lobby-guest","still host or alone 100 ticks after the hand-over");return;}
                if(k<20)return;
                boolean locked=!active(c,"mode-domination")&&!active(c,"map-next")&&!active(c,"map-prev")&&!active(c,"fill")&&!active(c,"size-up")&&!active(c,"size-down");
                String orders=WarTableLobby.texts().getOrDefault("orders","");
                report("p1-lobby-pass-host-guest",lobby(c).button("pass-host")==null,"a guest's note has "+(lobby(c).button("pass-host")==null?"no":"a")+" Pass host");
                report("p1-lobby-guest",locked&&active(c,"ready")&&kinds(1).startsWith("human,bot,bot")&&orders.contains("Aldric"),"orders locked="+locked+", Ready live="+active(c,"ready")+", Dusk "+kinds(1)+", the note says \""+orders+"\"");
                shot(c,"lobby-1080-guest");window(c,2560,1440,0,true);next();
            }
            case 12->{
                if(k<20)return;var w=c.getWindow();
                boolean[] ok={false};String ch=chips(c,3,ok);var nr=WarTableLobby.rects().get("note");boolean tabsIn=nr!=null;int tabsSeen=0;for(var e:WarTableLobby.rects().entrySet())if(e.getKey().startsWith("mode-")){tabsSeen++;var r=e.getValue();tabsIn&=nr!=null&&r[0]>=nr[0]+WarTableLobby.NPAD&&r[0]+r[2]<=nr[0]+WarTableLobby.NOTE_W-WarTableLobby.NPAD&&r[1]+r[3]<=nr[1]+nr[3];}ch+=", "+tabsSeen+" mode tabs inside the note: "+tabsIn;ok[0]&=tabsIn&&tabsSeen>=4;
                report("p1-lobby-1440",w.getFramebufferWidth()==2560&&w.getFramebufferHeight()==1440&&ok[0]&&UiCanvas.width()*3<=2560&&UiCanvas.height()*3<=1440,"framebuffer "+w.getFramebufferWidth()+"x"+w.getFramebufferHeight()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", "+ch);
                shot(c,"lobby-1440-guest");press(c,"ready");next();
            }
            case 13->{
                if(num("countdown")<=0){if(k>100)fail(c,"p1-lobby-guest-countdown","no countdown with both ready");return;}
                if(k<8)return;shot(c,"lobby-1440-guest-countdown");press(c,"ready");stage=-1;
                server(c,b->{b.campaign.social.handHost(guest,player);stage=14;});
            }
            case 14->{
                if(!flag("host")||num("countdown")!=0){if(k>100)fail(c,"p1-lobby-host-back","host not back");return;}
                if(k<20)return;
                if(!press(c,"pass-host")){fail(c,"p1-lobby-pass-host","the host's Pass host is not live with Aldric in the room");return;}
                stage=30;
            }
            case 30->{ // choosing: Aldric's seat offers the host's seat
                if(k<10)return;var l=lobby(c);var t=WarTableLobby.texts();
                boolean ok=l!=null&&l.choosingHost&&active(c,"seat-1-0")&&"Make host".equals(t.get("seat-1-0-hint"))&&!active(c,"ready");
                report("p1-lobby-pass-host-choose",ok,"choosing="+(l!=null&&l.choosingHost)+", Aldric's seat "+(active(c,"seat-1-0")?"live":"dull")+" reads \""+t.get("seat-1-0-hint")+"\", Ready "+(active(c,"ready")?"live":"dull"));
                shot(c,"lobby-1440-host-pass");
                if(!press(c,"seat-1-0")){fail(c,"p1-lobby-pass-host","Aldric's seat is not live while passing the host");return;}
                stage=31;
            }
            case 31->{ // social:host:Aldric moved the seat
                if(flag("host")){if(k>100)fail(c,"p1-lobby-pass-host","still host 100 ticks after choosing Aldric's seat (notice \""+CampaignClient.notice+"\")");return;}
                if(k<10)return;
                report("p1-lobby-pass-host",!flag("host")&&lobby(c).button("pass-host")==null&&!lobby(c).choosingHost&&!WarTableLobby.texts().getOrDefault("orders","").isEmpty(),"after Pass host and Aldric's seat: host="+flag("host")+", the note reads \""+WarTableLobby.texts().getOrDefault("orders","")+"\", Pass host "+(lobby(c).button("pass-host")==null?"gone":"still shown"));
                stage=-1;server(c,b->{b.campaign.social.handHost(guest,player);stage=32;});
            }
            case 32->{
                if(!flag("host")){if(k>100)fail(c,"p1-lobby-host-back","host not back after the pass test");return;}
                if(k<20)return;shot(c,"lobby-1440-host-bots");press(c,"ready");stage=-1;server(c,b->{b.campaign.social.ready(guest);stage=15;});
            }
            case 15->{
                if(num("countdown")<=0){if(k>100)fail(c,"p1-lobby-host-countdown","no countdown at 1440");return;}
                if(k<4)return;shot(c,"lobby-1440-host-countdown");stage=-1;
                server(c,b->{dropGuest(b);stage=16;}); // the guest leaves; the room stands down
            }
            case 16->{if(WarTableLobby.commanders().size()!=1||WarTableLobby.myReady()||k<10){if(k>100)fail(c,"p1-lobby-guest-leaves","guest still seated");return;}
                if(c.currentScreen instanceof CampScreen s)s.openTroop("knight");stage=40;}
            case 40->{ // the troop sheet fills the window at 1440p too
                if(!(c.currentScreen instanceof CampScreen s)||!s.page.equals("troop")||k<20){if(k>100)fail(c,"p1-lobby-troop-1440","no troop sheet");return;}
                report("p1-lobby-troop-1440",UiCanvas.k()==3&&UiCanvas.width()*3<=2560&&UiCanvas.height()*3<=1440,"troop sheet at 2560x1440: canvas k "+UiCanvas.k()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (want 3)");
                shot(c,"troop-1440");window(c,1920,1080,0,true);stage=42;}
            case 42->{ // and at 1080p, the War Table's 2x (Kort, 2026-10-31); then back to 1440p and the lobby
                if(k<20)return;
                report("p1-lobby-troop-1080",UiCanvas.k()==2&&c.currentScreen instanceof CampScreen s&&s.page.equals("troop"),"troop sheet at 1920x1080: canvas k "+UiCanvas.k()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (want 2)");
                shot(c,"troop-1080");window(c,2560,1440,0,true);CampScreen.open("lobby");stage=41;}
            case 41->{if(lobby(c)==null||k<20){if(k>100)fail(c,"p1-lobby-back","lobby not back after the troop sheet");return;}press(c,"ready");stage=17;}
            case 17->{ // the countdown runs out and the round starts with bots in the five empty seats
                if(!serverRunning||botsDawn<0){if(k>900)fail(c,"p1-lobby-bot-fill-on","no round 900 ticks after the last Ready (countdown "+num("countdown")+", notice \""+CampaignClient.notice+"\")");return;}
                var f=fielded;int units=f==null?-1:RosterMath.units(f,RosterMath.Catalog.fromSpec());
                report("p1-lobby-house-fielded",f!=null&&!f.contains("revenant")&&f.contains("knight")&&units==RosterMath.bannerSize()-1,"the Gilded pick fields "+units+" units, revenant "+(f!=null&&f.contains("revenant")?"fielded":"left home")+" (want "+(RosterMath.bannerSize()-1)+" Gilded, no revenant)");
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
        server(c,b->{dropGuest(b);if(b.running)b.campaign.returnLobby();if(savedBanner!=null&&player!=null)b.campaign.profile(player).banner=savedBanner;var s=b.campaign.social;if(player!=null&&s.room(player)!=null){var r=s.room(player);r.playing=false;s.leaveRoom(player);}s.revision++;});
        c.setScreen(null);if(oldW>0)window(c,oldW,oldH,oldGui);
        finish(c,"ui-lobby");
    }
    @Override public void cleanup(MinecraftClient c){if(!finished)end(c);}
}
