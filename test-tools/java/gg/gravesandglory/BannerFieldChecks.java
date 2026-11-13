package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/** Phase 1 suite ui-bannerfield (plan 1.3): the War Table's Banner page and Field page in a real client against its integrated
 *  server, each in a normal, an empty and an error state, shot at 1920x1080 (canvas scale 2) and 2560x1440 (scale 3), GUI Auto,
 *  borderless. Scenes: banner-normal (a mixed banner, a wide troop), banner-empty (one troop, no seat filled), banner-empty-house
 *  (a House with no troop recruited), banner-error (a third Epic refused by the server), banner-battle (a round is on),
 *  field-normal (a commander with a mixed banner), field-empty (a commander back in the camp), field-error (a round is on). Every shot is
 *  judged: the page is open at the wanted scale, no widget overlaps another, and every widget and drawn box sits 12 px inside the
 *  canvas and clear of the rail (UiHomeChecks.fits). Shots: run/screenshots/p1-bf-(scene)-(1080|1440).png. */
final class BannerFieldChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    static final String[] SCENES = {"banner-normal", "banner-empty", "banner-empty-house", "banner-error", "field-normal", "field-empty", "field-error", "banner-battle",
        "drag-valid", "drag-invalid", "drag-ghost", "drag-unseat",
        "drop-roll-empty", "drop-roll-wide", "drop-roll-refused", "drop-move-empty", "drop-swap", "drop-swap-wide", "drop-unseat", "drop-snap", "drop-cancel", "drop-same", "drop-click"};
    volatile UUID player;volatile boolean serverRunning;
    int scene,phase,pAt,sub,oldW,oldH,oldGui,serial;boolean started,finished;String saved;List<String> want=List.of();String note="";

    public void serverTick(GravesAndGlory.Battle b){serverRunning=b.running;}

    void go(int next,int tick){phase=next;pAt=tick+1;sub=0;} // k reads 0 on the next tick
    static String page(String scene){return scene.startsWith("field")?"field":"banner";}

    /** A commander owning `owned` (plus every Common), with `stars` for the ids named, and the banner these units make. */
    static Profile profile(List<String> owned,Map<String,Integer> stars,List<String> units){
        var p=new Profile();p.troopWrits=3;
        for(String id:owned)p.owned.computeIfAbsent(id,x->new Profile.Owned());
        for(var e:stars.entrySet()){var o=p.owned.get(e.getKey());if(o!=null)o.stars=e.getValue();}
        p.sanitize();p.setTable(p.activeBanner,WarGrid.sparse(Campaign.table(RosterMath.repack(units,RosterMath.Catalog.fromSpec(),p.owned.keySet()))));p.sanitize();
        return p;
    }
    static List<String> times(int n,String id){List<String> l=new ArrayList<>();for(int i=0;i<n;i++)l.add(id);return l;}
    static List<String> join(List<String>... parts){List<String> l=new ArrayList<>();for(var p:parts)l.addAll(p);return l;}

    /** The profile each banner scene runs on. */
    static Profile banner(String scene){
        if(scene.startsWith("drop-")||scene.startsWith("drag-"))return dragProfile(scene);
        return switch(scene){
            case "banner-normal" -> profile(List.of("sentinel","longbow","mage","wm_tusk","tc_tideguard","ic_breaker"),Map.of("knight",3,"sentinel",2,"longbow",2),
                join(List.of("mage"),times(3,"sentinel"),times(4,"longbow"),times(4,"knight"),times(3,"archer")));
            case "banner-error" -> profile(List.of("mage","tc_brute","wm_seer","wm_tusk"),Map.of(),join(List.of("mage","tc_brute"),times(8,"knight"),times(4,"archer")));
            case "banner-battle" -> banner("banner-normal");
            default -> { var p=profile(List.of(),Map.of(),List.of());p.owned.keySet().removeIf(id->!id.equals("knight"));p.setTable(p.activeBanner,WarGrid.sparse(Campaign.table(RosterMath.repack(List.of(),RosterMath.Catalog.fromSpec(),p.owned.keySet()))));yield p; }
        };
    }

    /** Server and client state for the scene. */
    void setup(MinecraftClient c,String name){
        var id=player;serial=CampaignClient.noticeSerial;
        if(isBanner(name)){
            var p=banner(name);want=p.line();
            server(c,b->{if(b.running)b.campaign.returnLobby();var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,p);b.campaign.save(id);});
        } else if(name.equals("banner-battle")){
            want=banner(name).line();
        } else if(name.equals("field-normal")){
            server(c,b->{if(b.running)b.campaign.returnLobby();var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,banner("banner-normal"));});
        } else if(name.equals("field-empty")){
            server(c,b->{var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);});
        } else if(name.equals("field-error")){
            server(c,b->{var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,banner("banner-normal"));b.campaign.save(id);});
        }
    }
    boolean startedRound,standDown;
    /** Whether the scene's state has reached the client. */
    boolean ready(MinecraftClient c,String name,int tick){
        switch(name){
            case "field-normal": return CampaignClient.profile.line().equals(banner("banner-normal").line())&&!GravesAndGloryClient.running;
            case "field-empty": return !GravesAndGloryClient.running;
            case "field-error": case "banner-battle": return GravesAndGloryClient.running&&CampaignClient.waiting&&serverRunning; // seated players cannot open the War Table in a round: the commander is stood down
            default: return CampaignClient.profile.line().equals(want)&&!GravesAndGloryClient.running;
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
            case "field-normal": { // a click on the second map row shows its card; nothing is voted any more
                if(k==0){UiPagesChecks.click(c,UiPagesChecks.box(c,"map-1"));return false;}
                if(k>=8){note+="; map-1 clicked, no Vote beside it";return true;}
                return false;
            }
            case "drag-valid": case "drag-invalid": case "drag-ghost": case "drag-unseat": {
                if(fromRoll(name)){if(k==0){gildedTab(c);return false;}if(k<8)return false;k-=8;}
                if(k==0){if(!resolve(c,name)){note+="; source or target not on the page";return true;}press(c);return false;}
                if(k==2){if(src!=null)moveTo(c,nudge());return false;}
                if(k==4){if(src!=null)moveTo(c,dst);return false;}
                return k>=8; // the card is held while the shot is taken
            }
            default: return true;
        }
    }

    /** The page as the last frame drew it at canvas scale wantK. */
    String judge(MinecraftClient c,String name,int wantK,boolean[] ok){
        var p=UiPagesChecks.pages(c);var b=UiPagesChecks.boxes(c);List<String> over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
        for(int i=0;i<keys.size();i++)for(int j=i+1;j<keys.size();j++)if(UiHomeChecks.overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));
        var bad=p==null?List.of("no page"):UiHomeChecks.fits(WarTablePages.rects(),b);
        boolean pass=p!=null&&p.page.equals(page(name))&&UiCanvas.k()==wantK&&over.isEmpty()&&bad.isEmpty();
        var t=WarTablePages.texts();String drag="";
        if(isDrag(name)){var gw=t.get("ghost-word");boolean g=p!=null&&p.dragging&&WarTablePages.rects().containsKey("ghost")&&dragWord(name).equals(gw);pass&=g;drag=", dragging "+(p!=null&&p.dragging)+", ghost word \""+gw+"\" (wanted \""+dragWord(name)+"\")";}
        ok[0]=pass;
        String said=page(name).equals("banner")?" seated \""+t.get("seated")+"\", roll-empty \""+t.get("roll-empty")+"\", hint \""+t.get("roll-hint")+"\"":" status \""+t.get("status")+"\", bots "+t.get("bots-current");
        return "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (k "+UiCanvas.k()+"), "+b.size()+" widgets, overlapping "+over+", not fitting "+bad+", notice \""+t.get("notice")+"\","+said+drag+(pass?"":" WRONG");
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
                if(k>(name.equals("field-error")?2400:400)){report("p1-bf-"+name,false,"the state never reached the client: banner "+CampaignClient.profile.line()+" wanted "+want+" (valid: "+RosterMath.validTable(WarGrid.sparse(want),RosterMath.Catalog.fromSpec(),banner(name).owned.keySet(),RosterMath.fieldableHouses())+"), running "+GravesAndGloryClient.running+", server running "+serverRunning);scene++;go(0,tick);}
            }
            case 1->{ // the page from the table's door
                if(k<60&&k%6==0&&UiPagesChecks.pages(c)==null){CampScreen.open(page(name));return;} // a round's opening may close it again
                if(k>=20&&UiPagesChecks.pages(c)!=null){UiRecruitChecks.park(c);go(2,tick);}
                else if(k>=80)go(2,tick);
            }
            case 2->{ // the scene's action, then the pointer parked off the page
                if(UiPagesChecks.pages(c)==null){report("p1-bf-"+name,false,"the page is not open; screen "+c.currentScreen+", running "+GravesAndGloryClient.running+", waiting "+CampaignClient.waiting+", player "+(c.player==null?"none":c.player.isAlive()+" "+c.interactionManager.getCurrentGameMode()));scene++;go(0,tick);return;}
                if(name.startsWith("drop-")){dropScene(c,name,k,tick);return;}
                if(sub==0){if(act(c,name,k)){sub=1;pAt=tick+1;}return;}
                if(k==0&&!isDrag(name))UiRecruitChecks.park(c);
                if(k>=4)go(3,tick);
            }
            case 3->{ // shot at 1080p
                boolean[] ok={false};String why=judge(c,name,2,ok);shot(c,"bf-"+name+"-1080");
                report("p1-bf-"+name+"-1080",ok[0],why+note);window(c,2560,1440,0,true);go(4,tick);
            }
            case 4->{ // shot at 1440p
                if(isDrag(name)){ // the resize built the screen again: the card is lifted again, at the new scale
                    if(k==10&&fromRoll(name))gildedTab(c);
                    if(k==18){var pg=UiPagesChecks.pages(c);if(pg!=null)pg.cancelDrag();if(!resolve(c,name))note+="; source or target not on the page at 1440";else press(c);}
                    if(k==20&&src!=null)moveTo(c,nudge());
                    if(k==22&&src!=null)moveTo(c,dst);
                } else if(k==22)UiRecruitChecks.park(c);
                if(k==(isDrag(name)?30:26)){boolean[] ok={false};String why=judge(c,name,3,ok);shot(c,"bf-"+name+"-1440");report("p1-bf-"+name+"-1440",ok[0],why+note);
                    window(c,1920,1080,0,true);c.setScreen(null);scene++;go(0,tick);}
            }
            default->{}
        }
    }

    // ============================================================================================================ drag and drop
    static boolean isBanner(String n){return n.startsWith("banner")&&!n.equals("banner-battle")||n.startsWith("drop-")||n.startsWith("drag-");}
    static boolean isDrag(String n){return n.startsWith("drag-");}

    /** The profile each drag or drop scene runs on: the mixed banner (4 seats free), a banner with 2 seats free (and a wide troop on the roll), or one with 1 seat free. */
    static Profile dragProfile(String n){
        return switch(n){
            case "drop-roll-wide" -> profile(List.of("mage"),Map.of(),join(times(9,"knight"),times(9,"archer")));
            case "drop-roll-refused","drop-same","drag-invalid" -> profile(List.of("mage"),Map.of(),join(times(10,"knight"),times(9,"archer")));
            default -> banner("banner-normal");
        };
    }
    /** {source kind (roll: a roll row of the troop; unit: the first seat the troop is in), its troop, target kind (seat: a seat; unit: the first seat of a troop; paper: the roll; off: nothing), its argument, "esc" to press Esc before the drop}. */
    static String[] spec(String n){
        return switch(n){
            case "drop-roll-empty","drag-valid" -> new String[]{"roll","knight","seat","18"};
            case "drop-roll-wide" -> new String[]{"roll","mage","seat","19"};
            case "drop-roll-refused" -> new String[]{"roll","mage","seat","19"};
            case "drop-move-empty" -> new String[]{"unit","mage","seat","19"};
            case "drop-swap" -> new String[]{"unit","sentinel","unit","longbow"};
            case "drop-swap-wide","drag-ghost" -> new String[]{"unit","mage","unit","knight"};
            case "drop-unseat","drag-unseat" -> new String[]{"unit","archer","paper",""};
            case "drop-snap" -> new String[]{"unit","mage","off",""};
            case "drop-cancel" -> new String[]{"unit","mage","seat","19","esc"};
            case "drop-same","drag-invalid" -> new String[]{"unit","knight","seat","3"};
            default -> null;
        };
    }
    /** Whether the scene lifts a troop from the roll (the Gilded tab is opened first: the roll opens on the Wildmoot tab). */
    static boolean fromRoll(String n){var s=spec(n);return n.equals("drop-click")||s!=null&&s[0].equals("roll");}
    static void gildedTab(MinecraftClient c){UiPagesChecks.click(c,UiPagesChecks.box(c,"house-"+RosterMath.Catalog.fromSpec().houseOf("knight")));}
    static String dragWord(String n){return switch(n){case "drag-valid"->"Seat";case "drag-invalid"->"The same troop.";case "drag-ghost"->"Trade places";default->"Take off";};}
    static int[] mid(int[] b){return b==null?null:new int[]{b[0]+b[2]/2,b[1]+b[3]/2};}

    List<String> base=List.of();SlotDrop.Plan plan;int[] src,dst;int sent0,doneAt,relK;String flight="",dragSeen="";

    /** Works the scene's source, target and expected plan out from the banner and the page as drawn now (canvas px). */
    boolean resolve(MinecraftClient c,String name){
        var sp=spec(name);base=new ArrayList<>(CampaignClient.profile.line());plan=null;src=null;dst=null;
        var r=WarTablePages.dropRules();String troop=sp[1];int head=base.indexOf(troop),seat=-1;
        if(sp[0].equals("roll")){var roll=WarTablePages.owned();for(int i=0;i<roll.size();i++)if(roll.get(i).troopId().equals(troop))src=mid(UiPagesChecks.box(c,"row-"+i));}
        else if(head>=0)src=mid(UiPagesChecks.box(c,"seat-"+head));
        switch(sp[2]){
            case "seat"->{seat=Integer.parseInt(sp[3]);dst=mid(UiPagesChecks.box(c,"seat-"+seat));}
            case "unit"->{seat=base.indexOf(sp[3]);if(seat>=0)dst=mid(UiPagesChecks.box(c,"seat-"+seat));}
            case "paper"->dst=mid(WarTablePages.rects().get("roll"));
            default->dst=new int[]{UiCanvas.width()-30,30};
        }
        if(src==null||dst==null)return false;
        if(sp[2].equals("seat")||sp[2].equals("unit"))plan=sp[0].equals("roll")?SlotDrop.fromRoll(base,troop,seat,r):SlotDrop.move(base,head,seat,r);
        else if(sp[2].equals("paper"))plan=SlotDrop.unseat(base,head,r);
        return true;
    }
    int[] nudge(){double dx=dst[0]-src[0],dy=dst[1]-src[1],n=Math.max(1,Math.hypot(dx,dy));return new int[]{(int)Math.round(src[0]+dx/n*14),(int)Math.round(src[1]+dy/n*14)};}
    void press(MinecraftClient c){var s=c.currentScreen;double x=UiHomeChecks.gui(c,src[0]),y=UiHomeChecks.gui(c,src[1]);if(s!=null){s.mouseMoved(x,y);s.mouseClicked(x,y,0);}}
    void moveTo(MinecraftClient c,int[] pt){var s=c.currentScreen;if(s!=null)s.mouseDragged(UiHomeChecks.gui(c,pt[0]),UiHomeChecks.gui(c,pt[1]),0,0,0);}
    void release(MinecraftClient c,int[] pt){var s=c.currentScreen;if(s!=null)s.mouseReleased(UiHomeChecks.gui(c,pt[0]),UiHomeChecks.gui(c,pt[1]),0);}
    static String flightOf(WarTablePages p){return p==null||p.flyAt==0?"none":p.flyBack?"back":"land";}

    /** A drop scene: the real mouse events of a drag (press, two moves, release) or of a click, then what the banner and the commands sent came to. */
    void dropScene(MinecraftClient c,String name,int k,int tick){
        var p=UiPagesChecks.pages(c);boolean click=name.equals("drop-click"),esc=!click&&spec(name).length>4;
        if(fromRoll(name)){if(k==0){gildedTab(c);return;}if(k<8)return;k-=8;}
        if(k==0){
            sent0=WarTablePages.sent().size();doneAt=-1;flight="";dragSeen="";relK=click?8:6;
            if(click){
                base=new ArrayList<>(CampaignClient.profile.line());plan=SlotDrop.fromRoll(base,"knight",18,WarTablePages.dropRules());
                var roll=WarTablePages.owned();for(int i=0;i<roll.size();i++)if(roll.get(i).troopId().equals("knight"))UiPagesChecks.click(c,UiPagesChecks.box(c,"row-"+i));
                return;
            }
            if(!resolve(c,name)){report("p1-bf-"+name,false,"source or target not on the page: src "+Arrays.toString(src)+" dst "+Arrays.toString(dst));scene++;go(0,tick);return;}
            press(c);return;
        }
        if(click){if(k==8)UiPagesChecks.click(c,UiPagesChecks.box(c,"seat-18"));}
        else{
            if(k==2)moveTo(c,nudge());
            if(k==4){moveTo(c,dst);dragSeen=""+(p!=null&&p.dragging);}
            if(k==6){
                if(esc){UiHomeChecks.key(c,org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);flight=flightOf(UiPagesChecks.pages(c));release(c,dst);}
                else{release(c,dst);flight=flightOf(UiPagesChecks.pages(c));}
            }
        }
        if(k<=relK)return;
        boolean change=plan!=null&&plan.ok()&&!esc;
        if(change){
            if(doneAt<0&&CampaignClient.profile.line().equals(plan.result())&&(p==null||p.pend==null))doneAt=k;
            if(doneAt>=0&&k>=doneAt+6||k>relK+130)dropJudge(c,name,tick,change,esc);
        }else if(k>=relK+24)dropJudge(c,name,tick,change,esc);
    }

    /** The scene's literal expectations (apart from the plan's), on the banner it began with. */
    static boolean literal(String name,List<String> live,List<String> base){
        int f=Collections.frequency(live,"knight");
        return switch(name){
            case "drop-roll-empty","drop-click" -> f==5&&live.get(16).equals("knight");
            case "drop-roll-wide" -> live.get(18).equals("mage")&&live.get(19).equals(RosterMath.MARKER);
            case "drop-move-empty" -> live.get(14).equals("mage")&&live.get(15).equals(RosterMath.MARKER)&&live.get(0).equals("sentinel");
            case "drop-swap" -> {int a=base.indexOf("sentinel"),b=base.indexOf("longbow");yield live.get(a).equals("longbow")&&live.get(b).equals("sentinel")&&Collections.frequency(live,"sentinel")==3;}
            case "drop-swap-wide" -> Collections.frequency(live,"mage")==1&&f==4&&!live.equals(base);
            case "drop-unseat" -> Collections.frequency(live,"archer")==2&&live.indexOf(RosterMath.EMPTY)==15;
            case "drop-roll-refused","drop-snap","drop-cancel","drop-same" -> live.equals(base);
            default -> true;
        };
    }

    void dropJudge(MinecraftClient c,String name,int tick,boolean change,boolean esc){
        var p=UiPagesChecks.pages(c);var live=new ArrayList<>(CampaignClient.profile.line());var all=WarTablePages.sent();
        var cmds=new ArrayList<>(all.subList(Math.min(sent0,all.size()),all.size()));
        boolean click=name.equals("drop-click");
        boolean ok=change?live.equals(plan.result())&&cmds.equals(plan.commands()):live.equals(base)&&cmds.isEmpty();
        var valid=RosterMath.validTable(WarGrid.sparse(live),RosterMath.Catalog.fromSpec(),CampaignClient.profile.owned.keySet(),RosterMath.fieldableHouses());
        ok&=valid==null&&literal(name,live,base)&&p!=null&&p.page.equals("banner");
        String wantFlight=click?"":change?(spec(name)[2].equals("paper")?"none":"land"):"back"; // a card taken off the banner has no seat to land on
        if(!click)ok&=dragSeen.equals("true")&&flight.equals(wantFlight);
        String said=plan==null?"no plan":plan.ok()?"legal":"refused \""+plan.why()+"\"";
        if(plan!=null&&!plan.ok()&&!plan.why().isEmpty()&&!esc)ok&=p!=null&&p.noticeText.contains(plan.why());
        report("p1-bf-"+name,ok,said+", sent "+cmds+" (planned "+(plan==null?"[]":plan.commands())+"), banner "+(live.equals(base)?"unchanged":"changed")+(change?", matches plan "+live.equals(plan.result()):"")+", dragging seen "+dragSeen+", card flight "+flight+" (wanted "+wantFlight+"), valid "+valid+", notice \""+(p==null?"":p.noticeText)+"\""+(ok?"":" WRONG"));
        scene++;go(0,tick);
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
