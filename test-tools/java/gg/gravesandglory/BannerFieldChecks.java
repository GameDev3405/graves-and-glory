package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/** Phase 1 suite ui-bannerfield (plan 1.3): the War Table's Banner page and Field page in a real client against its integrated
 *  server, each in a normal, an empty and an error state, shot at 1920x1080 (canvas scale 2) and 2560x1440 (scale 3), GUI Auto,
 *  borderless. Scenes: banner-normal (a mixed banner, a wide troop), banner-empty (one troop, no seat filled), banner-empty-house
 *  (a House with no troop recruited), banner-error (a third Epic refused by the server), banner-battle (a round is on),
 *  field-normal (on the war gates with a saved vote), field-empty (off the gates), field-error (a round is on). Every shot is
 *  judged: the page is open at the wanted scale, no widget overlaps another, and every widget and drawn box sits 12 px inside the
 *  canvas and clear of the rail (UiHomeChecks.fits). Shots: run/screenshots/p1-bf-(scene)-(1080|1440).png. */
final class BannerFieldChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    static final String[] SCENES = {"banner-normal", "banner-empty", "banner-empty-house", "banner-error", "field-normal", "field-empty", "field-error", "banner-battle"};
    volatile UUID player;volatile boolean serverOnPad,serverRunning;
    int scene,phase,pAt,sub,oldW,oldH,oldGui,serial;boolean started,finished;String saved;List<String> want=List.of();String note="";

    public void serverTick(GravesAndGlory.Battle b){var id=player;serverOnPad=id!=null&&b.campaign.onPad.containsKey(id);serverRunning=b.running;}

    void go(int next,int tick){phase=next;pAt=tick+1;sub=0;} // k reads 0 on the next tick
    static String page(String scene){return scene.startsWith("banner")?"banner":"field";}

    /** A commander owning `owned` (plus every Common), with `stars` for the ids named, and the banner these units make. */
    static Profile profile(List<String> owned,Map<String,Integer> stars,List<String> units){
        var p=new Profile();p.gold=300;
        for(String id:owned)p.owned.computeIfAbsent(id,x->new Profile.Owned());
        for(var e:stars.entrySet()){var o=p.owned.get(e.getKey());if(o!=null)o.stars=e.getValue();}
        p.sanitize();p.banner=RosterMath.repack(units,RosterMath.Catalog.fromSpec(),p.owned.keySet());p.sanitize();
        return p;
    }
    static List<String> times(int n,String id){List<String> l=new ArrayList<>();for(int i=0;i<n;i++)l.add(id);return l;}
    static List<String> join(List<String>... parts){List<String> l=new ArrayList<>();for(var p:parts)l.addAll(p);return l;}

    /** The profile each banner scene runs on. */
    static Profile banner(String scene){
        return switch(scene){
            case "banner-normal" -> profile(List.of("sentinel","longbow","mage","wm_tusk","tc_tideguard","ic_breaker"),Map.of("knight",3,"sentinel",2,"longbow",2),
                join(List.of("mage"),times(3,"sentinel"),times(4,"longbow"),times(4,"knight"),times(3,"archer")));
            case "banner-error" -> profile(List.of("mage","tc_brute","wm_seer","wm_tusk"),Map.of(),join(List.of("mage","tc_brute"),times(8,"knight"),times(4,"archer")));
            case "banner-battle" -> banner("banner-normal");
            default -> { var p=profile(List.of(),Map.of(),List.of());p.owned.keySet().removeIf(id->!id.equals("knight"));p.banner=RosterMath.repack(List.of(),RosterMath.Catalog.fromSpec(),p.owned.keySet());yield p; }
        };
    }

    /** Server and client state for the scene. */
    void setup(MinecraftClient c,String name){
        var id=player;serial=CampaignClient.noticeSerial;
        if(name.startsWith("banner") && !name.equals("banner-battle")){
            var p=banner(name);want=p.banner;
            server(c,b->{if(b.running)b.campaign.returnLobby();var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,p);b.campaign.save(id);});
        } else if(name.equals("banner-battle")){
            want=banner(name).banner;
        } else if(name.equals("field-normal")){
            server(c,b->{if(b.running)b.campaign.returnLobby();var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,banner("banner-normal"));b.campaign.mapVotes.remove(id);});
            GravesAndGloryClient.send("camp:gates");
        } else if(name.equals("field-empty")){
            server(c,b->{var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.mapVotes.remove(id);});
        } else if(name.equals("field-error")){
            server(c,b->{var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,banner("banner-normal"));b.campaign.save(id);});
        }
    }
    boolean startedRound,standDown;
    /** Whether the scene's state has reached the client. */
    boolean ready(MinecraftClient c,String name,int tick){
        switch(name){
            case "field-normal": return CampaignClient.padMine>=0&&serverOnPad&&CampaignClient.profile.banner.equals(banner("banner-normal").banner);
            case "field-empty": return CampaignClient.padMine<0&&!serverOnPad&&!GravesAndGloryClient.running;
            case "field-error": case "banner-battle": return GravesAndGloryClient.running&&CampaignClient.waiting&&serverRunning; // seated players cannot open the War Table in a round: the commander is stood down
            default: return CampaignClient.profile.banner.equals(want)&&!GravesAndGloryClient.running;
        }
    }

    /** The scene's own action once the page is open; true when done. */
    boolean act(MinecraftClient c,String name,int k){
        switch(name){
            case "banner-empty-house": if(k==0)UiPagesChecks.click(c,UiPagesChecks.box(c,"house-wildmoot"));return k>=8;
            case "banner-error": {
                if(k==0){UiPagesChecks.click(c,UiPagesChecks.box(c,"house-wildmoot"));return false;}
                if(k==8){var roll=WarTablePages.owned();int i=-1;for(int j=0;j<roll.size();j++)if(roll.get(j).troopId().equals("wm_seer"))i=j;
                    if(i<0){note+="; wm_seer not on the Wildmoot roll";return true;}UiPagesChecks.click(c,UiPagesChecks.box(c,"row-"+i));return false;}
                if(k==16){serial=CampaignClient.noticeSerial;UiPagesChecks.click(c,UiPagesChecks.box(c,"seat-17"));return false;}
                if(k>16&&CampaignClient.noticeSerial!=serial&&CampaignClient.notice.startsWith("No room")){note+="; notice \""+CampaignClient.notice+"\"";return k>=22;}
                if(k>120){note+="; no refusal notice 120 ticks on";return true;}
                return false;
            }
            case "field-normal": {
                if(k==0){serial=CampaignClient.noticeSerial;UiPagesChecks.click(c,UiPagesChecks.box(c,"map-1"));return false;}
                if(k==6)UiPagesChecks.click(c,UiPagesChecks.box(c,"vote-1"));
                if(k>6&&CampaignClient.noticeSerial!=serial&&CampaignClient.notice.startsWith("Vote saved")){note+="; notice \""+CampaignClient.notice+"\"";return k>=14;}
                if(k>120){note+="; no vote notice 120 ticks on";return true;}
                return false;
            }
            default: return true;
        }
    }

    /** The page as the last frame drew it at canvas scale wantK. */
    String judge(MinecraftClient c,String name,int wantK,boolean[] ok){
        var p=UiPagesChecks.pages(c);var b=UiPagesChecks.boxes(c);List<String> over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
        for(int i=0;i<keys.size();i++)for(int j=i+1;j<keys.size();j++)if(UiHomeChecks.overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));
        var bad=p==null?List.of("no page"):UiHomeChecks.fits(WarTablePages.rects(),b);
        boolean pass=p!=null&&p.page.equals(page(name))&&UiCanvas.k()==wantK&&over.isEmpty()&&bad.isEmpty();ok[0]=pass;
        var t=WarTablePages.texts();
        String said=name.startsWith("banner")?" seated \""+t.get("seated")+"\", roll-empty \""+t.get("roll-empty")+"\", hint \""+t.get("roll-hint")+"\"":" status \""+t.get("status")+"\", bots "+t.get("bots-current");
        return "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (k "+UiCanvas.k()+"), "+b.size()+" widgets, overlapping "+over+", not fitting "+bad+", notice \""+t.get("notice")+"\","+said+(pass?"":" WRONG");
    }

    public void step(MinecraftClient c,int tick){
        if(finished)return;
        if(tick>16000){report("p1-bf-timeout",false,"stuck in scene "+(scene<SCENES.length?SCENES[scene]:"end")+" phase "+phase);end(c);return;}
        if(!started){
            if(tick<20)return;
            started=true;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();player=c.player.getUuid();
            saved=Campaign.JSON.toJson(CampaignClient.profile);c.setScreen(null);pAt=tick+1;return;
        }
        if(scene>=SCENES.length){end(c);return;}
        String name=SCENES[scene];int k=tick-pAt;
        switch(phase){
            case 0->{ // the scene's state, built on the server; waits for the snapshot to carry it
                if(k==0){note="";standDown=false;setup(c,name);c.setScreen(null);window(c,1920,1080,0,true);return;}
                if((name.equals("field-error")||name.equals("banner-battle"))&&!(name.equals("banner-battle")&&serverRunning)){ // a round: started on the server until it runs
                    if(k>=2&&!serverRunning){server(c,b->{var p=b.server.getPlayerManager().getPlayer(player);if(p!=null)skirmish(b,p,0,1,"normal");});}
                    else if(serverRunning&&!standDown){standDown=true;server(c,b->{var sq=b.squads.get(player);if(sq!=null)sq.seated=false;});}
                }
                if(ready(c,name,tick)&&k>=(name.equals("field-error")||name.equals("banner-battle")?150:30)&&CampaignClient.camera==null){go(1,tick);return;}
                if(k>(name.equals("field-error")?2400:400)){report("p1-bf-"+name,false,"the state never reached the client: banner "+CampaignClient.profile.banner+" wanted "+want+" (valid: "+RosterMath.validBanner(want,RosterMath.Catalog.fromSpec(),banner(name).owned.keySet())+"), padMine "+CampaignClient.padMine+", running "+GravesAndGloryClient.running+", server running "+serverRunning+", onPad "+serverOnPad);scene++;go(0,tick);}
            }
            case 1->{ // the page from the table's door
                if(k<60&&k%6==0&&UiPagesChecks.pages(c)==null){CampScreen.open(page(name));return;} // a round's opening may close it again
                if(k>=20&&UiPagesChecks.pages(c)!=null){UiRecruitChecks.park(c);go(2,tick);}
                else if(k>=80)go(2,tick);
            }
            case 2->{ // the scene's action, then the pointer parked off the page
                if(UiPagesChecks.pages(c)==null){report("p1-bf-"+name,false,"the page is not open; screen "+c.currentScreen+", running "+GravesAndGloryClient.running+", waiting "+CampaignClient.waiting+", player "+(c.player==null?"none":c.player.isAlive()+" "+c.interactionManager.getCurrentGameMode()));scene++;go(0,tick);return;}
                if(sub==0){if(act(c,name,k)){sub=1;pAt=tick+1;}return;}
                if(k==0)UiRecruitChecks.park(c);
                if(k>=4)go(3,tick);
            }
            case 3->{ // shot at 1080p
                boolean[] ok={false};String why=judge(c,name,2,ok);shot(c,"bf-"+name+"-1080");
                report("p1-bf-"+name+"-1080",ok[0],why+note);window(c,2560,1440,0,true);go(4,tick);
            }
            case 4->{ // shot at 1440p
                if(k==22)UiRecruitChecks.park(c);
                if(k==26){boolean[] ok={false};String why=judge(c,name,3,ok);shot(c,"bf-"+name+"-1440");report("p1-bf-"+name+"-1440",ok[0],why);
                    window(c,1920,1080,0,true);c.setScreen(null);scene++;go(0,tick);}
            }
            default->{}
        }
    }

    void end(MinecraftClient c){
        if(finished)return;finished=true;c.setScreen(null);
        var id=player;var back=saved==null?null:Campaign.JSON.fromJson(saved,Profile.class);
        if(id!=null)server(c,b->{if(b.running)b.campaign.returnLobby();var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);if(back!=null){b.campaign.profiles.put(id,back);b.campaign.save(id);}});
        if(oldW>0)window(c,oldW,oldH,oldGui);
        finish(c,"ui-bannerfield");
    }
    @Override public void cleanup(MinecraftClient c){if(!finished)end(c);}
}
