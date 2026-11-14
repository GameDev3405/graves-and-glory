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
    int mark=-1,waitFrom,oldW,oldH,oldGui,map0=-1,mi,mapStart=-1;boolean finished;ServerPlayerEntity stand;String guestCode="";

    public void serverTick(GravesAndGlory.Battle b){
        serverRunning=b.running;
        if(b.running&&botsDawn<0){int d=0,k=0,h=0;for(var s:b.squads.values()){if(s.bot){if(s.team==0)d++;else k++;}else if(s.seated)h++;}botsDawn=d;botsDusk=k;seatedHumans=h;for(var s:b.squads.values())if(!s.bot&&s.owner!=null&&s.owner.equals(player))fielded=new ArrayList<>(s.banner);}
    }
    boolean sentMap,modeSeen;int mapWas;String modeWas="";static final int FIT_WAIT=WarTableLobby.FIT_TICKS+30;
    /** The stand-in guest's id when there is one, else a random id the room does not know (a House pick by nobody in the room is refused and changes nothing). */
    UUID guestOrNull(){return guest!=null?guest:UUID.randomUUID();}
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
    /** Joins the join-by-code chain's next stage (stages 81 to 84 follow one another). */
    void next81(){stage++;}
    /** Every House chip draws its House's whole first word, and the row stays inside the sheet's text width; the canvas scale is want. */
    String chips(MinecraftClient c,int want,boolean[] ok){
        var l=lobby(c);var t=WarTableLobby.texts();StringBuilder sb=new StringBuilder();boolean pass=l!=null&&UiCanvas.k()==want;int left=Integer.MAX_VALUE,right=0;var sheet=WarTableLobby.rects().get("sheet");
        for(String id:RosterMath.fieldableHouses()){String name=t.getOrDefault("house-"+id+"-name","");var b=l==null?null:l.button("house-"+id);
            var r=WarTableLobby.rects().get("house-"+id+"-name");pass&=name.equals(WarTableLobby.chipName(id))&&!name.contains("...")&&b!=null&&r!=null;if(r!=null){left=Math.min(left,r[0]);right=Math.max(right,r[0]+r[2]);}
            sb.append(name).append(t.getOrDefault("house-"+id+"-seal","").equals("true")?"(seal) ":" ");}
        pass&=right-left<=WarTableLobby.TEXT_W&&sheet!=null&&left>=sheet[0]&&right<=sheet[0]+sheet[2];ok[0]=pass;
        return "chips "+sb.toString().trim()+", names span "+(right-left)+" canvas px of "+WarTableLobby.TEXT_W+", inside the sheet "+(sheet!=null&&left>=sheet[0]&&right<=sheet[0]+sheet[2])+", canvas k "+UiCanvas.k()+" (want "+want+")";
    }
    /** The map-wait view's faults (empty when right): every part drawn, the percent and a tip from the list, each box inside the canvas and its own paper, and no two boxes overlapping. */
    String waitFaults(MinecraftClient c,String at){
        var r=WarTableLobby.rects();var t=WarTableLobby.texts();List<String> bad=new ArrayList<>();int[] sheet=r.get("sheet"),note=r.get("note");
        List<String> onSheet=new ArrayList<>(List.of("wait-head","wait-percent","wait-bar","wait-side-0","wait-side-1","wait-seat-0-0","wait-seat-1-0","wait-tip","ready")),onNote=List.of("wait-card","wait-map-name","wait-mode-head","wait-mode-summary");
        for(String k:onSheet)if(r.get(k)==null)bad.add(k+" not drawn");
        for(String k:onNote)if(k.equals("wait-mode-head")?!t.containsKey(k):r.get(k)==null)bad.add(k+" not drawn");
        if(r.get("status")!=null||t.containsKey("status"))bad.add("the wait view still draws a status line: \""+t.get("status")+"\" (the heading and the plate say it)");
        if(!bad.isEmpty())return at+": "+bad;
        int map=Math.max(0,Math.min(Spec.MAPS.length-1,num("map")));var m=Spec.MAPS[map];
        if(!"37%".equals(t.get("wait-percent")))bad.add("percent reads "+t.get("wait-percent"));
        if(!("Preparing "+m.name()).equals(t.get("wait-head")))bad.add("heading reads "+t.get("wait-head"));
        if(!("table/map-"+m.id()).equals(WarTableLobby.textures().get("wait-card")))bad.add("card is "+WarTableLobby.textures().get("wait-card"));
        if(!m.name().equals(t.get("wait-map-name")))bad.add("map name reads "+t.get("wait-map-name"));
        String want=WarTableLobby.summary(str("mode"));if(want.isEmpty()||!want.equals(t.get("wait-mode-summary")))bad.add("mode summary reads \""+t.get("wait-mode-summary")+"\" (want \""+want+"\")");
        if(!WarTableLobby.tipTexts().contains(t.get("wait-tip")))bad.add("tip \""+t.get("wait-tip")+"\" is not in the list of "+Spec.TIPS.length);
        if(Spec.TIPS.length!=20)bad.add(Spec.TIPS.length+" tips, want 20");
        if(!t.getOrDefault("wait-tip-drawn","").equals(t.get("wait-tip")))bad.add("the tip is cut short: drew \""+t.get("wait-tip-drawn")+"\"");
        if(t.getOrDefault("wait-seat-0-0","").isBlank()||t.getOrDefault("wait-seat-0-0","").equals("Bot"))bad.add("Dawn's first line-up row reads \""+t.get("wait-seat-0-0")+"\"");
        if(!"Bot".equals(t.get("wait-seat-1-0")))bad.add("Dusk's first row reads \""+t.get("wait-seat-1-0")+"\" (want Bot)");
        if(t.getOrDefault("wait-seat-0-0-house","").isEmpty())bad.add("the commander's House is not drawn");
        List<String> all=new ArrayList<>(onSheet);all.addAll(onNote);all.addAll(List.of("wait-seat-0-1","wait-seat-0-2","wait-seat-1-1","wait-seat-1-2"));
        List<String> have=new ArrayList<>();for(String k:all)if(r.get(k)!=null)have.add(k);
        for(String k:have){int[] b=r.get(k);int[] paper=onNote.contains(k)?note:sheet;
            if(b[0]<0||b[1]<0||b[0]+b[2]>UiCanvas.width()||b[1]+b[3]>UiCanvas.height())bad.add(k+" off the canvas "+Arrays.toString(b));
            if(paper==null||b[0]<paper[0]||b[1]<paper[1]||b[0]+b[2]>paper[0]+paper[2]||b[1]+b[3]>paper[1]+paper[3])bad.add(k+" not inside its paper "+Arrays.toString(b));}
        for(int i=0;i<have.size();i++)for(int j=i+1;j<have.size();j++)if(overlaps(r.get(have.get(i)),r.get(have.get(j))))bad.add(have.get(i)+" overlaps "+have.get(j));
        return bad.isEmpty()?"":at+": "+bad;
    }
    void fail(MinecraftClient c,String id,String why){report(id,false,why);end(c);}

    String growthWhy="";
        /** The Growth face of the troop sheet: its four rows by name in order, each with a line and a lasting, every drawn box inside the sheet, and no word "Veteran" or "level" on it. Empty when right. */
        String growthFaults(MinecraftClient c,String at){
            var t=WarTableTroop.texts();var r=WarTableTroop.rects();int[] sheet=r.get("sheet");List<String> bad=new ArrayList<>();
            if(!"growth".equals(t.get("tab")))bad.add("tab is "+t.get("tab"));
            for(String n:new String[]{"Stars","Rank","Armoury","Mastery"}){
                if(!n.equals(t.get("head-growth-"+n)))bad.add(n+" row missing");
                String line=t.get("growth-line-"+n),lasts=t.get("growth-"+n);
                if(line==null||line.isBlank())bad.add(n+" has no line");
                if(lasts==null||!lasts.startsWith("Lasts "))bad.add(n+" has no duration (\""+lasts+"\")");
                for(String k:new String[]{"head-growth-"+n,"growth-line-"+n,"growth-"+n}){int[] b=r.get(k);if(b==null||sheet==null||b[0]<sheet[0]||b[1]<sheet[1]||b[0]+b[2]>sheet[0]+sheet[2]||b[1]+b[3]>sheet[1]+sheet[3])bad.add(k+" not inside the sheet "+java.util.Arrays.toString(b));}
            }
            int[] last=r.get("growth-line-Mastery");if(last!=null&&last[1]+last[3]>sheet[1]+WarTableTroop.CONTENT_END)bad.add("Mastery's line runs past the sheet's content end");
            for(var e:t.entrySet())if(e.getKey().startsWith("growth")&&e.getValue().toLowerCase().matches(".*\b(levels?|veterans?)\b.*"))bad.add(e.getKey()+" says \""+e.getValue()+"\"");
            List<String> names=new ArrayList<>();for(String k:r.keySet())if(k.contains("growth"))names.add(k);
            for(int i=0;i<names.size();i++)for(int j=i+1;j<names.size();j++)if(overlaps(r.get(names.get(i)),r.get(names.get(j))))bad.add(names.get(i)+" overlaps "+names.get(j));
            return bad.isEmpty()?"":at+": "+bad;
        }
        static boolean overlaps(int[] a,int[] b){return a!=null&&b!=null&&a[0]<b[0]+b[2]&&b[0]<a[0]+a[2]&&a[1]<b[1]+b[3]&&b[1]<a[1]+a[3];}
        public void step(MinecraftClient c,int tick){
        if(finished)return;
        if(tick>9000){fail(c,"p1-lobby-timeout","stuck in stage "+stage);return;}
        int k=since(tick);
        switch(stage){
            case 0->{ // at camp, no round, no room; the window 1920x1080 borderless at GUI Auto
                if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
                c.setScreen(null);window(c,1920,1080,0,true);stage=-1;
                server(c,b->{if(b.running)b.campaign.returnLobby();var s=b.campaign.social;if(s.room(player)!=null)s.leaveRoom(player);
                    var pr=b.campaign.profile(player);savedBanner=new ArrayList<>(pr.line());pr.owned.computeIfAbsent("revenant",x->new Profile.Owned());
                    var bn=new java.util.TreeMap<>(pr.table());bn.put(bn.lastKey(),"revenant");pr.setTable(pr.activeBanner,bn);s.revision++;stage=1;});
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
                var l=lobby(c);var rects=WarTableLobby.rects();
                boolean extras=l.button("copy")!=null&&l.button("invite")!=null&&l.button("start")==null&&l.button("code")==null&&str("room").equals(t.get("code"))&&l.button("tier-easy")!=null&&l.button("tier-normal")!=null&&l.button("tier-hard")!=null;
                report("p1-lobby-room-tools",extras,"room code "+t.get("code")+" (snapshot "+str("room")+"), Copy "+(l.button("copy")!=null)+", Invite "+(l.button("invite")!=null)+", Start now "+(l.button("start")!=null?"shown WRONG: nobody is ready":"hidden")+", Join field "+(l.button("code")!=null?"shown WRONG":"hidden")+", the three bot tiers "+(l.button("tier-hard")!=null));
                boolean tierDefault=str("difficulty").equals(CampaignClient.difficulty)&&CampaignClient.difficulty.equals(t.get("tier"))&&active(c,"tier-easy")&&active(c,"tier-hard");
                report("p1-lobby-tier-default",tierDefault,"the room's bot difficulty "+str("difficulty")+" (the server's setting "+CampaignClient.difficulty+"), the page marks "+t.get("tier")+", the tiers live for the host");
                shot(c,"lobby-1080-host-bots");mi=0;modeSeen=false;mark=-99;mapStart=num("map");stage=50;
            }
            case 50->{ // each mode tab: the sheet draws that mode's one line of rules and usual length; a mode that does not host the map takes the map and says so
                var modes=WarTableLobby.modes();
                if(mi>=modes.size()){stage=53;return;}
                String id=modes.get(mi);
                if(!str("mode").equals(id)){if(k==0||k%40==0)press(c,"mode-"+id);if(k>200)fail(c,"p1-lobby-summary-"+id,"the mode never became "+id+" ("+str("mode")+")");return;}
                if(!modeSeen){modeSeen=true;mark=-99;return;}if(k<8)return;
                var t=WarTableLobby.texts();String want=WarTableLobby.summary(id),drawn=t.get("summary");var r=WarTableLobby.rects().get("summary");var sheet=WarTableLobby.rects().get("sheet");
                boolean ok=!want.isEmpty()&&want.equals(drawn)&&r!=null&&sheet!=null&&r[0]>=sheet[0]&&r[0]+r[2]<=sheet[0]+sheet[2]&&r[0]+r[2]<=sheet[0]+WarTableLobby.PAD+WarTableLobby.TEXT_W;
                report("p1-lobby-summary-"+id,ok,"mode "+id+": the sheet reads \""+drawn+"\" (want \""+want+"\"), width "+(r==null?-1:r[2])+" of "+WarTableLobby.TEXT_W);
                if(mi>0){boolean fits=Modes.hosts(id,num("map"))&&("set to fit "+WarTableLobby.modeName(id)).equals(t.get("fit-map"))&&!t.containsKey("fit-mode");
                    report("p1-lobby-fit-map-"+id,fits,"mode "+id+" took map "+num("map")+" ("+Spec.MAPS[num("map")].name()+"); beside Map: \""+t.get("fit-map")+"\"");}
                if(mi==modes.size()-1)shot(c,"lobby-1080-host-fit");
                mi++;modeSeen=false;mark=-99;
            }
            case 53->{ // a map the mode does not host takes the mode back to Domination, and says so beside the Mode tabs
                if(k==0){mapWas=num("map");modeWas=str("mode");sentMap=false;}
                if(!sentMap){sentMap=true;press(c,"map-next");return;}
                if(num("map")==mapWas){if(k>200)fail(c,"p1-lobby-fit-mode","the map never moved");return;}
                if(k<8||(k<60&&!WarTableLobby.texts().containsKey("fit-mode")))return;
                var t=WarTableLobby.texts();
                String want="set to fit the map";
                boolean ok=Modes.hosts(str("mode"),num("map"))&&!str("mode").equals(modeWas)&&want.equals(t.get("fit-mode"));
                report("p1-lobby-fit-mode",ok,"map "+mapWas+" -> "+num("map")+" ("+Spec.MAPS[num("map")].name()+"), the mode "+modeWas+" -> "+str("mode")+"; beside Mode: \""+t.get("fit-mode")+"\" (want \""+want+"\"); snapshot fitted \""+str("fitted")+"\" serial "+num("fitSerial")+", keys "+t.keySet());
                sentMap=false;stage=54;
            }
            case 54->{ // the note lasts three seconds, then goes
                if(k<FIT_WAIT){if(k==10)shot(c,"lobby-1080-host-fit-mode");return;}
                boolean gone=!WarTableLobby.texts().containsKey("fit-mode")&&!WarTableLobby.texts().containsKey("fit-map");
                report("p1-lobby-fit-clears",gone,"after "+k+" ticks the changed-to-fit notes "+(gone?"are gone":"still show: "+WarTableLobby.texts().get("fit-mode")+" / "+WarTableLobby.texts().get("fit-map")));
                stage=-1;server(c,b->{var so=b.campaign.social;var r=so.room(player);if(r!=null){r.map=mapStart;r.mode="domination";r.ready.clear();so.revision++;}stage=55;});
            }
            case 55->{if(num("map")!=mapStart||!str("mode").equals("domination")||k<10){if(k>100)fail(c,"p1-lobby-fit-restore","map "+num("map")+", mode "+str("mode"));return;}press(c,"fill");stage=3;}
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
                boolean startHidden=lobby(c).button("start")==null;report("p1-lobby-start-now-needs-all",startHidden,"ready alone with fill off (the room cannot start): Start now "+(startHidden?"hidden":"shown WRONG"));
                press(c,"tier-hard");stage=60;
            }
            case 60->{ // Ready, then bot difficulty: it moves the room's setting, not the server's, and nobody is un-readied
                if(!"hard".equals(str("difficulty"))){if(k>100)fail(c,"p1-lobby-tier","the room's bot difficulty is "+str("difficulty")+" 100 ticks after Hard");else if(k>=30&&k%30==0)press(c,"tier-hard");return;}
                if(k<10)return;
                boolean kept=WarTableLobby.myReady(),marked="hard".equals(WarTableLobby.texts().get("tier")),serverKept=!"hard".equals(CampaignClient.difficulty);
                report("p1-lobby-tier-keeps-ready",kept&&marked&&serverKept,"after Hard: ready="+kept+", the page marks "+WarTableLobby.texts().get("tier")+", the server-wide setting is still "+CampaignClient.difficulty+" (it is the default for new rooms)");
                stage=-1;
                server(c,b->{var so=b.campaign.social;var r=so.room(player);boolean fillBefore=r.fill;
                    so.configure(player,r.size,r.map,!r.fill,Spec.MAPS.length);boolean keptFill=r.ready.contains(player)&&r.fill!=fillBefore;
                    so.configure(player,r.size,r.map,!r.fill,Spec.MAPS.length);
                    so.house(player,RosterMath.rollHouse(b.campaign.profile(player).line()),RosterMath.fieldableHouses());boolean housePickOwn=!r.ready.contains(player); // a pick takes back the picker's own Ready only
                    r.ready.add(player);so.house(guestOrNull(),"gilded",RosterMath.fieldableHouses());boolean otherKept=r.ready.contains(player);
                    so.difficulty(player,"normal",Set.of("easy","normal","hard"));boolean tierKept=r.ready.contains(player)&&"normal".equals(r.difficulty);
                    so.configure(player,r.size,(r.map+1)%Spec.MAPS.length,r.fill,Spec.MAPS.length);boolean mapClears=!r.ready.contains(player);r.map=(r.map+Spec.MAPS.length-1)%Spec.MAPS.length;
                    r.ready.add(player);so.mode(player,"domination");boolean modeClears=!r.ready.contains(player);
                    r.ready.add(player);so.configure(player,r.size,r.map,r.fill,Spec.MAPS.length);boolean sameKept=r.ready.contains(player);
                    report("p1-lobby-ready-flags",keptFill&&housePickOwn&&tierKept&&mapClears&&modeClears&&sameKept,"bots toggle keeps Ready "+keptFill+", a House pick takes back only the picker's "+housePickOwn+", difficulty keeps Ready "+tierKept+", a map change un-readies "+mapClears+", a mode change un-readies "+modeClears+", an unchanged save keeps Ready "+sameKept);
                    r.ready.clear();so.revision++;stage=5;});
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
            case 21->{if(k<2)return;
                // the banner line and Change tag, the dimmed Houses and their tooltip (the host is on Tidecallers, whose troops the banner lacks)
                var t=WarTableLobby.texts();var l=lobby(c);var p=CampaignClient.profile;var cat=RosterMath.Catalog.fromSpec();var bn=p.banners.get(p.activeBanner);var tb=WarGrid.parse(bn.cells);
                String want=bn.name+" · "+WarGrid.troops(tb,cat)+" troops · "+WarGrid.cost(tb,cat)+" gold";String line=t.getOrDefault("banner","");
                report("p1-lobby-banner-line",line.equals(want)&&!t.containsKey("gear")&&WarTableLobby.rects().get("banner")!=null&&active(c,"change"),"the sheet reads \""+line+"\" (want \""+want+"\"), old gear line "+(t.containsKey("gear")?"STILL DRAWN":"gone")+", Change "+(active(c,"change")?"live":"dull"));
                StringBuilder dims=new StringBuilder();boolean dimOk=true;
                for(String id:RosterMath.fieldableHouses()){boolean empty=WarTableLobby.houseTroops(id)==0;boolean drawn="true".equals(t.get("house-"+id+"-dim"));var b=l==null?null:l.button("house-"+id);
                    boolean tip=b!=null&&(b.getTooltip()!=null)==empty;dimOk&=drawn==empty&&tip;dims.append(id).append(drawn?" dim":" lit").append(tip?"":"(TIP WRONG)").append(' ');}
                report("p1-lobby-house-dim",dimOk&&"true".equals(t.get("house-tidecallers-dim"))&&"false".equals(t.get("house-gilded-dim")),"Houses: "+dims.toString().trim()+" (dim exactly where the banner has no troops, tooltip only on those)");
                report("p1-lobby-house-dim-words",WarTableLobby.noTroops("tidecallers").equals("Your banner has no Tidecallers troops.")&&WarTableLobby.noTroops("wildmoot").equals("Your banner has no Wildmoot troops."),"tooltip words \""+WarTableLobby.noTroops("wildmoot")+"\"");
                var wm=l==null?null:l.button("house-wildmoot");if(wm!=null){int[] box=UiCanvas.guiBox(wm.canvasBox()[0],wm.canvasBox()[1],wm.canvasBox()[2],wm.canvasBox()[3]);BarracksUiChecks.hover(c,box[0]+box[2]/2.0,box[1]+box[3]/2.0);}
                stage=24;
            }
            case 24->{if(k<30)return;shot(c,"lobby-1080-host-no-house");BarracksUiChecks.hover(c,1,1);
                if(!press(c,"change")){fail(c,"p1-lobby-change","Change is not live on the host's sheet");return;}
                stage=25;
            }
            case 25->{ // the Change round trip: the Banner page on the battle banner, then back to the same room
                if(k<10)return;var s=c.currentScreen instanceof CampScreen cs?cs:null;
                boolean onBanner=s!=null&&"banner".equals(s.page)&&WarTablePages.shownBanner==CampaignClient.profile.activeBanner;
                String there=s==null?"none":s.page;
                if(onBanner&&s.pages!=null&&s.pages.button("back")!=null){WarTableRecruit.press(s.pages.button("back"));stage=26;return;}
                if(k>100)fail(c,"p1-lobby-change","Change landed on page "+there+", battle banner shown "+(WarTablePages.shownBanner==CampaignClient.profile.activeBanner));
            }
            case 26->{if(k<10)return;var s=c.currentScreen instanceof CampScreen cs?cs:null;boolean back=s!=null&&"lobby".equals(s.page)&&lobby(c)!=null&&WarTableLobby.commanders().size()>=1;
                if(!back){if(k>100)fail(c,"p1-lobby-change","Back from the Banner page did not return to the room (page "+(s==null?"none":s.page)+")");return;}
                report("p1-lobby-change",true,"Change opens the Banner page on the battle banner and Back returns to the room");
                press(c,"house-gilded");stage=22;}
            case 22->{if(!WarTableLobby.myHouse().equals("gilded")||WarTableLobby.myTroops()<=0||k<10){if(k>100)fail(c,"p1-lobby-house-gilded","house "+WarTableLobby.myHouse()+", troops "+WarTableLobby.myTroops());return;}
                report("p1-lobby-house-gilded",active(c,"ready")&&WarTableLobby.myTroops()==RosterMath.LEGACY_BANNER_SIZE-1,"Gilded fields "+WarTableLobby.myTroops()+" troops (want "+(RosterMath.LEGACY_BANNER_SIZE-1)+"), Ready "+(active(c,"ready")?"live":"dull"));
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
                report("p1-lobby-start-now-guest",lobby(c).button("start")==null,"a guest's sheet has "+(lobby(c).button("start")==null?"no":"a")+" Start now");
                report("p1-lobby-pass-host-guest",lobby(c).button("pass-host")==null,"a guest's note has "+(lobby(c).button("pass-host")==null?"no":"a")+" Pass host");
                report("p1-lobby-guest",locked&&active(c,"ready")&&kinds(1).startsWith("human,bot,bot")&&orders.contains("Aldric"),"orders locked="+locked+", Ready live="+active(c,"ready")+", Dusk "+kinds(1)+", the note says \""+orders+"\"");
                String rc=WarTableLobby.readyCount();report("p1-lobby-ready-words",rc.endsWith(" ready. Starts 10 s after everyone is ready.")&&!rc.contains("after the last"),"the ready line reads \""+rc+"\"");
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
                stage=80;}
            case 80->{ // out of a room: the Join by code field. A stand-in commander opens a room of his own; the host leaves his and joins it by its code
                if(k<4)return;stage=-1;
                server(c,b->{var p=new Phase1Checks.Listener(b.world);stand=p;b.server.getPlayerManager().getPlayerList().add(p);guest=p.getUuid();var s=b.campaign.social;s.names.put(guest,"Aldric");
                    s.presence(new HashSet<>(b.server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList()),System.currentTimeMillis());
                    b.campaign.profile(guest).commander="quartermaster";s.createRoom(guest);guestCode=s.room(guest).code;s.leaveRoom(player);stage=81;});
            }
            case 81->{
                var l=lobby(c);if(!str("room").isEmpty()||l==null||l.button("code")==null||k<10){if(k>200)fail(c,"p1-lobby-join-field","no Join by code field out of a room (room \""+str("room")+"\")");return;}
                boolean ok=l.button("join")!=null&&!l.button("join").active&&l.button("open")!=null&&WarTableLobby.texts().containsKey("join-hint");
                report("p1-lobby-join-field",ok,"out of a room: the code line shown, Join "+(l.button("join").active?"live WRONG":"dull")+", Open the council "+(l.button("open")!=null?"shown":"missing"));
                press(c,"code");if(c.currentScreen!=null)for(char ch:"ab-c1 2".toCharArray())c.currentScreen.charTyped(ch,0);next81();
            }
            case 82->{
                var l=lobby(c);if(l==null)return;if(k<2||!"ABC12".equals(l.code)&&k<40)return;
                boolean partial=l.typing&&"ABC12".equals(l.code)&&!active(c,"join");
                if(!"5 of 8".equals(WarTableLobby.texts().get("code-count"))&&k<40)return;
                report("p1-lobby-code-partial",partial&&"5 of 8".equals(WarTableLobby.texts().get("code-count")),"typed \"ab-c1 2\": \""+l.code+"\" (writing "+l.typing+"), the line counts \""+WarTableLobby.texts().get("code-count")+"\", Join "+(active(c,"join")?"live WRONG":"dull"));
                shot(c,"lobby-join-code");
                if(c.currentScreen!=null){for(int i=0;i<5;i++)c.currentScreen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE,0,0);for(char ch:guestCode.toCharArray())c.currentScreen.charTyped(ch,0);}
                next81();
            }
            case 83->{
                var l=lobby(c);if(l==null||!guestCode.equals(l.code)||!active(c,"join")){if(k>60)fail(c,"p1-lobby-code-whole","the code line reads \""+(l==null?"":l.code)+"\", Join "+(active(c,"join")?"live":"dull")+" (want "+guestCode+" and live)");return;}
                report("p1-lobby-code-whole",true,"the whole code "+guestCode+" makes Join live");press(c,"join");next81();
            }
            case 84->{
                if(!guestCode.equals(str("room"))||lobby(c)==null||k<10){if(k>200)fail(c,"p1-lobby-join-code","room is \""+str("room")+"\", want "+guestCode+" 200 ticks after Join (notice \""+CampaignClient.notice+"\")");return;}
                report("p1-lobby-join-code",!flag("host")&&WarTableLobby.hostName().equals(stand.getName().getString()),"joined room "+str("room")+" by its code: host="+flag("host")+", hosted by "+WarTableLobby.hostName());
                shot(c,"lobby-join-code-joined");stage=-1;
                server(c,b->{var s=b.campaign.social;if(s.room(player)!=null)s.leaveRoom(player);dropGuest(b);stage=85;});
            }
            case 85->{if(!str("room").isEmpty()||lobby(c)==null||k<10){if(k>200)fail(c,"p1-lobby-reopen","still in a room");return;}if(press(c,"open"))stage=86;}
            case 86->{if(str("room").isEmpty()||!flag("host")||lobby(c)==null||k<10){if(k>200)fail(c,"p1-lobby-reopen","no room after Open the council");return;}press(c,"house-gilded");stage=87;}
            case 87->{if(!WarTableLobby.myHouse().equals("gilded")||k<10){if(k>100)fail(c,"p1-lobby-reopen","house "+WarTableLobby.myHouse());return;}
                if(c.currentScreen instanceof CampScreen s)s.openTroop("knight");stage=40;}
            case 40->{ // the troop sheet fills the window at 1440p too
                if(!(c.currentScreen instanceof CampScreen s)||!s.page.equals("troop")||k<20){if(k>100)fail(c,"p1-lobby-troop-1440","no troop sheet");return;}
                report("p1-lobby-troop-1440",UiCanvas.k()==3&&UiCanvas.width()*3<=2560&&UiCanvas.height()*3<=1440,"troop sheet at 2560x1440: canvas k "+UiCanvas.k()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (want 3)");
                shot(c,"troop-1440");s.troopSheet.face("growth");stage=43;}
            case 43->{ // the Growth face: the four names (Stars, Rank, Armoury, Mastery), each with a line and how long it lasts, none of them the old system word; at 1440p
                if(k<4)return;growthWhy=growthFaults(c,"2560x1440");report("p1-lobby-troop-growth-1440",growthWhy.isEmpty(),growthWhy.isEmpty()?"the Growth face names Stars, Rank, Armoury and Mastery with a line and a duration each, inside the sheet, and no Veteran or level word":growthWhy);
                shot(c,"troop-growth-1440");if(c.currentScreen instanceof CampScreen s)s.troopSheet.face("troop");window(c,1920,1080,0,true);stage=42;}
            case 42->{ // and at 1080p, the War Table's 2x (Kort, 2026-10-31); then back to 1440p and the lobby
                if(k<20)return;
                report("p1-lobby-troop-1080",UiCanvas.k()==2&&c.currentScreen instanceof CampScreen s&&s.page.equals("troop"),"troop sheet at 1920x1080: canvas k "+UiCanvas.k()+", canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (want 2)");
                shot(c,"troop-1080");if(c.currentScreen instanceof CampScreen s)s.troopSheet.face("growth");stage=44;}
            case 44->{ // the Growth face again at 1080p, and the Troop face's words checked for the old system word
                if(k<4)return;growthWhy=growthFaults(c,"1920x1080");report("p1-lobby-troop-growth-1080",growthWhy.isEmpty(),growthWhy.isEmpty()?"the Growth face is whole at 1920x1080":growthWhy);
                shot(c,"troop-growth-1080");if(c.currentScreen instanceof CampScreen s)s.troopSheet.face("troop");stage=45;}
            case 45->{ // the Troop face reads Rank and Mastery, never Levels or Veteran as a system
                if(k<4)return;var t=WarTableTroop.texts();String bad="";for(var e:t.entrySet())if(e.getValue().toLowerCase().matches(".*\b(levels?|veterans?)\b.*")&&!e.getKey().startsWith("level-")&&!e.getKey().equals("mastery-title")&&!e.getKey().equals("mastery-next")&&!e.getKey().equals("letter-name"))bad+=e.getKey()+"=\""+e.getValue()+"\" ";
                report("p1-lobby-troop-names",bad.isEmpty()&&"Mastery".equals(WarTableTroop.texts().get("head-mastery-level"))&&"Rank in battle".equals(WarTableTroop.texts().get("head-levels")),"the Troop face's headings read "+WarTableTroop.texts().get("head-mastery-level")+" and "+WarTableTroop.texts().get("head-levels")+(bad.isEmpty()?"; no line says level or veteran":"; WRONG: "+bad));
                window(c,2560,1440,0,true);CampScreen.open("lobby");stage=41;}
            case 41->{if(lobby(c)==null||k<20){if(k>100)fail(c,"p1-lobby-back","lobby not back after the troop sheet");return;}stage=90;}
            case 90->{ // the map wait at 2560x1440: the room waits for its map (the view forced, 37 percent), and the screen shows the card, both line-ups, the progress and one tip, inside the papers and clear of each other
                if(k==0){WarTableLobby.previewWait=37;return;}
                if(k<12)return;
                String bad=waitFaults(c,"2560x1440");report("p1-lobby-map-wait-1440",bad.isEmpty(),bad.isEmpty()?"map wait at 2560x1440: card, line-ups, 37% bar and a listed tip, all inside the papers and apart":bad);
                shot(c,"lobby-map-wait-1440");window(c,854,480,0,true);stage=91;}
            case 91->{ // and at the smallest size the checks use
                if(k<20)return;
                String bad=waitFaults(c,"854x480");report("p1-lobby-map-wait-854",bad.isEmpty(),bad.isEmpty()?"map wait at 854x480: card, line-ups, 37% bar and a listed tip, all inside the papers and apart":bad);
                shot(c,"lobby-map-wait-854");WarTableLobby.previewWait=-1;window(c,2560,1440,0,true);stage=92;}
            case 92->{if(k<20)return;var l=lobby(c);if(l==null||l.button("invite")==null){if(k>100)fail(c,"p1-lobby-map-wait-end","the council page did not come back after the map wait");return;}press(c,"invite");stage=70;}
            case 70->{ // Invite opens the friends list as a pop-over over the page; Close puts it away
                var l=lobby(c);if(l==null||!l.inviting||l.button("invite-close")==null||!WarTableLobby.texts().containsKey("popover")||k<6){if(k>100)fail(c,"p1-lobby-invite-popover","the pop-over did not open");else if(k>=30&&k%30==0&&l!=null&&!l.inviting)press(c,"invite");return;}
                var pr=WarTableLobby.rects().get("popover");var sh=WarTableLobby.rects().get("sheet");
                boolean ok=pr!=null&&sh!=null&&pr[0]>=sh[0]&&pr[0]+pr[2]<=sh[0]+sh[2]&&pr[1]>=sh[1]&&pr[1]+pr[3]<=sh[1]+sh[3]&&l.button("ready")==null&&l.button("seat-0-0")==null;
                report("p1-lobby-invite-popover",ok,"Invite opened the pop-over \""+WarTableLobby.texts().get("popover")+"\" inside the sheet, the page's own tags put away: "+ok+(WarTableLobby.texts().containsKey("popover-empty")?"; the empty-camp line \""+WarTableLobby.texts().get("popover-empty")+"\"":""));
                shot(c,"lobby-1440-invite-popover");press(c,"invite-close");stage=71;
            }
            case 71->{var l=lobby(c);if(l==null||l.inviting||l.button("ready")==null||k<6){if(k>100)fail(c,"p1-lobby-invite-close","the pop-over did not close");return;}
                report("p1-lobby-invite-close",l.button("start")==null&&l.button("invite")!=null,"Close put the pop-over away; Start now "+(l.button("start")==null?"hidden":"shown WRONG")+" before anyone is ready");
                press(c,"ready");stage=72;}
            case 72->{ // every human ready: the host's Start now beside Ready (the countdown runs; Start now would skip it)
                if(num("countdown")<=0||!str("waiting").isEmpty()){if(k>100)fail(c,"p1-lobby-start-now-host","no countdown after Ready (waiting \""+str("waiting")+"\")");return;}
                if(k<6||(k<40&&WarTableLobby.rects().get("start")==null))return;var l=lobby(c);boolean shown=l!=null&&l.button("start")!=null&&l.button("start").active&&l.button("ready")!=null&&flag("host");
                if(l!=null&&l.button("start")!=null){var s1=WarTableLobby.rects().get("start");var r1=WarTableLobby.rects().get("ready");shown&=s1!=null&&r1!=null&&s1[0]>=r1[0]+r1[2];}
                report("p1-lobby-start-now-host",shown,"host, every human ready: Start now "+(shown?"shown beside Ready":"WRONG: missing or overlapping (start button "+(l==null||l.button("start")==null?"absent":"active "+l.button("start").active)+", start rect "+java.util.Arrays.toString(WarTableLobby.rects().get("start"))+", ready rect "+java.util.Arrays.toString(WarTableLobby.rects().get("ready"))+", host "+flag("host")+", waiting \""+str("waiting")+"\")"));
                shot(c,"lobby-1440-start-now");stage=17;}
            case 17->{ // the countdown runs out and the round starts with bots in the five empty seats
                if(!serverRunning||botsDawn<0){if(k>900)fail(c,"p1-lobby-bot-fill-on","no round 900 ticks after the last Ready (countdown "+num("countdown")+", notice \""+CampaignClient.notice+"\")");return;}
                var f=fielded;int units=f==null?-1:RosterMath.units(f,RosterMath.Catalog.fromSpec());
                report("p1-lobby-house-fielded",f!=null&&!f.contains("revenant")&&f.contains("knight")&&units==RosterMath.LEGACY_BANNER_SIZE-1,"the Gilded pick fields "+units+" units, revenant "+(f!=null&&f.contains("revenant")?"fielded":"left home")+" (want "+(RosterMath.LEGACY_BANNER_SIZE-1)+" Gilded, no revenant)");
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
        server(c,b->{dropGuest(b);if(b.running)b.campaign.returnLobby();if(savedBanner!=null&&player!=null){var sp=b.campaign.profile(player);sp.setTable(sp.activeBanner,WarGrid.sparse(savedBanner));}var s=b.campaign.social;if(player!=null&&s.room(player)!=null){var r=s.room(player);r.playing=false;s.leaveRoom(player);}s.revision++;});
        c.setScreen(null);if(oldW>0)window(c,oldW,oldH,oldGui);
        finish(c,"ui-lobby");
    }
    @Override public void cleanup(MinecraftClient c){if(!finished)end(c);}
}
