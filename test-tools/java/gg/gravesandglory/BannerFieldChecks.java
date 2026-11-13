package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/** Phase 1 suite ui-bannerfield (plan 1.3, army Task 7): the War Table's Banner page (the 9 x 6 grid) and Field page in a real client
 *  against its integrated server. Shot scenes: banner-normal (a mixed banner of 630 gold with a Legendary), banner-empty (one troop owned,
 *  nothing placed), banner-empty-house (a House with no troop recruited), banner-hover (the brush over an empty cell: its words beside it),
 *  banner-error (a second Legendary refused, the why in the notice and on the palette), banner-battle (a round is on), field-normal,
 *  field-empty and field-error; each banner scene at 1920x1080 (canvas scale 2), 2560x1440 (scale 3) and 900x520 (the compact page), the
 *  field scenes at the first two. Every shot is judged: the page is open at the wanted scale, no widget overlaps another, and every widget and
 *  drawn box sits 12 px inside the canvas and clear of the rail (UiHomeChecks.fits). Shots: run/screenshots/p1-bf-(scene)-(1080|1440|small).png.
 *  banner-paint is the paint proof, by real clicks: a cell painted with the brush is sent and the server's snapshot carries it
 *  (p1-bf-paint-place), a second Legendary is refused with the why in words and nothing sent (p1-bf-paint-refused), and another banner's tab
 *  shows that banner, whose Use button makes it the one that goes into battle (p1-bf-paint-switch). */
final class BannerFieldChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    static final String[] SCENES = {"banner-normal", "banner-empty", "banner-empty-house", "banner-hover", "banner-error", "banner-paint",
        "field-normal", "field-empty", "field-error", "banner-battle"};
    /** The window sizes shot: 1080p, 1440p and a small window that gets the compact page (scale -1: the page must be compact). */
    static final int[][] SIZES = {{1920, 1080, 2}, {2560, 1440, 3}, {900, 520, -1}};
    static final String[] SIZE_NAMES = {"1080", "1440", "small"};
    static final String LEGEND = "gl_marshal";
    volatile UUID player;volatile boolean serverRunning;
    int scene,phase,pAt,sub,oldW,oldH,oldGui,serial,shotIdx;boolean started,finished;String saved;List<String> want=List.of();String note="";

    public void serverTick(GravesAndGlory.Battle b){serverRunning=b.running;}

    void go(int next,int tick){phase=next;pAt=tick+1;sub=0;} // k reads 0 on the next tick
    static String page(String scene){return scene.startsWith("field")?"field":"banner";}

    /** A table from cell, troop pairs. */
    static SortedMap<Integer,String> tbl(Object... pairs){SortedMap<Integer,String> t=new TreeMap<>();for(int i=0;i+1<pairs.length;i+=2)t.put((Integer)pairs[i],(String)pairs[i+1]);return t;}
    /** Cells first..first+n-1 holding the troop, put into t. */
    static void fill(SortedMap<Integer,String> t,int first,int n,String id){for(int i=0;i<n;i++)t.put(first+i,id);}
    /** A commander owning `owned` (plus every Common) with the banners given (name, table, name, table...), the first the active one. */
    @SuppressWarnings("unchecked")
    static Profile profile(List<String> owned,Object... banners){
        var p=new Profile();p.troopWrits=3;
        for(String id:owned)p.owned.computeIfAbsent(id,x->new Profile.Owned());
        p.sanitize();p.banners.clear();
        for(int i=0;i+1<banners.length;i+=2)p.banners.add(new Profile.Banner((String)banners[i],WarGrid.format((SortedMap<Integer,String>)banners[i+1])));
        p.activeBanner=0;p.sanitize();
        return p;
    }
    /** Every banner's name and cells, as the snapshot carries them. */
    static List<String> cells(Profile p){List<String> l=new ArrayList<>();if(p.banners!=null)for(var b:p.banners)l.add(b.name+"="+b.cells);return l;}

    /** The profile each banner scene runs on. */
    static Profile banner(String scene){
        return switch(scene){
            case "banner-normal","banner-battle" -> { var t=tbl(4,LEGEND,13,"mage");fill(t,9,3,"sentinel");fill(t,18,4,"longbow");fill(t,27,4,"knight");fill(t,36,3,"archer");
                yield profile(List.of("sentinel","longbow","mage",LEGEND,"wm_tusk"),"Banner I",t,"Banner II",tbl(0,"knight",1,"knight")); }
            case "banner-hover","banner-error","banner-paint" -> profile(List.of(LEGEND,"sentinel"),"Banner I",tbl(4,LEGEND,30,"sentinel"),"Banner II",tbl(0,"knight",1,"knight"));
            default -> { var p=profile(List.of(),"Banner I",tbl());p.owned.keySet().removeIf(id->!id.equals("knight"));p.sanitize();yield p; }
        };
    }

    static boolean isBanner(String n){return n.startsWith("banner")&&!n.equals("banner-battle");}

    /** Server and client state for the scene. */
    void setup(MinecraftClient c,String name){
        var id=player;serial=CampaignClient.noticeSerial;
        if(isBanner(name)){
            var p=banner(name);want=cells(p);
            server(c,b->{if(b.running)b.campaign.returnLobby();var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,p);b.campaign.save(id);});
        } else if(name.equals("banner-battle")){
            want=cells(banner(name));
        } else if(name.equals("field-normal")){
            server(c,b->{if(b.running)b.campaign.returnLobby();var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,banner("banner-normal"));});
        } else if(name.equals("field-empty")){
            server(c,b->{var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);});
        } else if(name.equals("field-error")){
            server(c,b->{var pos=b.server.getPlayerManager().getPlayer(id);if(pos!=null)b.campaign.toLobby(pos);b.campaign.profiles.put(id,banner("banner-normal"));b.campaign.save(id);});
        }
    }
    boolean standDown;
    /** Whether the scene's state has reached the client. */
    boolean ready(MinecraftClient c,String name,int tick){
        switch(name){
            case "field-normal": return cells(CampaignClient.profile).equals(cells(banner("banner-normal")))&&!GravesAndGloryClient.running;
            case "field-empty": return !GravesAndGloryClient.running;
            case "field-error": case "banner-battle": return GravesAndGloryClient.running&&CampaignClient.waiting&&serverRunning; // seated players cannot open the War Table in a round: the commander is stood down
            default: return cells(CampaignClient.profile).equals(want)&&!GravesAndGloryClient.running;
        }
    }

    /** The palette row box of a troop on the House shown, or null. */
    static int[] row(MinecraftClient c,String troop){int i=rowIndex(troop);return i<0?null:UiPagesChecks.box(c,"row-"+i);}
    /** The palette row index of a troop on the House shown, or -1. */
    static int rowIndex(String troop){var roll=WarTablePages.owned();for(int i=0;i<roll.size();i++)if(roll.get(i).troopId().equals(troop))return i;return -1;}
    /** Shows the Gilded House's troops on the palette. */
    static void gilded(MinecraftClient c){UiPagesChecks.click(c,UiPagesChecks.box(c,"house-gilded"));}

    /** The scene's own action once the page is open (run again after each resize); true when done. */
    boolean act(MinecraftClient c,String name,int k){
        switch(name){
            case "banner-empty-house": if(k==0)UiPagesChecks.click(c,UiPagesChecks.box(c,"house-wildmoot"));return k>=8;
            case "banner-hover": {
                if(k==0){gilded(c);return false;}
                if(k==6){UiPagesChecks.click(c,row(c,"knight"));return false;}
                if(k==10){var b=UiPagesChecks.box(c,"cell-13");if(b!=null)UiRecruitChecks.pointer(c,b[0]+b[2]/2.0,b[1]+b[3]/2.0);return false;}
                return k>=14;
            }
            case "banner-error": {
                if(k==0){gilded(c);return false;}
                if(k==6){UiPagesChecks.click(c,row(c,LEGEND));return false;}
                if(k==10){UiPagesChecks.click(c,UiPagesChecks.box(c,"cell-20"));return false;}
                return k>=13;
            }
            case "field-normal": { // a click on the second map row shows its card; nothing is voted any more
                if(k==0){UiPagesChecks.click(c,UiPagesChecks.box(c,"map-1"));return false;}
                return k>=8;
            }
            default: return true;
        }
    }
    /** Whether the scene keeps the pointer where its action left it (the hover shot). */
    static boolean held(String name){return name.equals("banner-hover");}

    /** The page as the last frame drew it at canvas scale wantK (-1: the compact page). */
    String judge(MinecraftClient c,String name,int wantK,boolean[] ok){
        var p=UiPagesChecks.pages(c);var b=UiPagesChecks.boxes(c);List<String> over=new ArrayList<>();var keys=new ArrayList<>(b.keySet());
        for(int i=0;i<keys.size();i++)for(int j=i+1;j<keys.size();j++)if(UiHomeChecks.overlap(b.get(keys.get(i)),b.get(keys.get(j))))over.add(keys.get(i)+"/"+keys.get(j));
        var bad=p==null?List.of("no page"):UiHomeChecks.fits(WarTablePages.rects(),b);
        boolean pass=p!=null&&p.page.equals(page(name))&&(wantK<0?WarTable.compact():UiCanvas.k()==wantK)&&over.isEmpty()&&bad.isEmpty();
        var t=WarTablePages.texts();String extra="";
        if(name.equals("banner-hover")){String w=t.get("cell-word"),want="Place "+WarTablePages.troopName("knight")+", "+WarTablePages.cost("knight")+" gold";pass&=want.equals(w);extra=", hover words \""+w+"\" (want \""+want+"\")";}
        if(name.equals("banner-error")){String why=BannerDrop.why("legendary_limit"),on=t.get("why-"+rowIndex(LEGEND));pass&=p!=null&&why.equals(p.noticeText)&&why.equals(on);extra=", why on the palette \""+on+"\" (want \""+why+"\")";}
        ok[0]=pass;
        String said=page(name).equals("banner")?" troops \""+t.get("troops")+"\", gold \""+t.get("gold")+"\", shown tab \""+t.get("tab-shown")+"\", roll-empty \""+t.get("roll-empty")+"\"":" status \""+t.get("status")+"\", bots "+t.get("bots-current");
        return "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+" (k "+UiCanvas.k()+", compact "+WarTable.compact()+"), "+b.size()+" widgets, overlapping "+over+", not fitting "+bad+", notice \""+t.get("notice")+"\","+said+extra+(pass?"":" WRONG");
    }

    public void step(MinecraftClient c,int tick){
        if(finished)return;
        if(tick>20000){report("p1-bf-timeout",false,"stuck in scene "+(scene<SCENES.length?SCENES[scene]:"end")+" phase "+phase);end(c);return;}
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
                if(k>(name.equals("field-error")?2400:400)){report("p1-bf-"+name,false,"the state never reached the client: banners "+cells(CampaignClient.profile)+" wanted "+want+", running "+GravesAndGloryClient.running+", server running "+serverRunning);scene++;go(0,tick);}
            }
            case 1->{ // the page from the table's door
                if(k<60&&k%6==0&&UiPagesChecks.pages(c)==null){CampScreen.open(page(name));return;} // a round's opening may close it again
                if(k>=20&&UiPagesChecks.pages(c)!=null){UiRecruitChecks.park(c);go(2,tick);}
                else if(k>=80)go(2,tick);
            }
            case 2->{ // the scene's action, then the shots
                if(UiPagesChecks.pages(c)==null){report("p1-bf-"+name,false,"the page is not open; screen "+c.currentScreen+", running "+GravesAndGloryClient.running+", waiting "+CampaignClient.waiting);scene++;go(0,tick);return;}
                if(name.equals("banner-paint")){paint(c,k,tick);return;}
                if(act(c,name,k)){shotIdx=0;go(3,tick);sub=1;}
            }
            case 3->{ // a shot at SIZES[shotIdx]; after a resize the page is built again, so the action runs again first
                if(sub==0){
                    if(k==0)window(c,SIZES[shotIdx][0],SIZES[shotIdx][1],0,true);
                    if(k<12)return;
                    if(act(c,name,k-12)){sub=1;pAt=tick+1;}
                    return;
                }
                if(k==0&&!held(name))UiRecruitChecks.park(c);
                if(k<4)return;
                boolean[] ok={false};String why=judge(c,name,SIZES[shotIdx][2],ok);shot(c,"bf-"+name+"-"+SIZE_NAMES[shotIdx]);
                report("p1-bf-"+name+"-"+SIZE_NAMES[shotIdx],ok[0],why+note);
                shotIdx++;
                if(shotIdx>=(page(name).equals("banner")?SIZES.length:2)){window(c,1920,1080,0,true);c.setScreen(null);scene++;go(0,tick);}
                else go(3,tick);
            }
            default->{}
        }
    }

    // ============================================================================================================ the paint proof
    int st,stAt,sent0;String expect="",why="";

    /** banner-paint, by real clicks on the page: a place the server takes, a refusal in words, then another banner shown and made the one
     *  that goes into battle. */
    void paint(MinecraftClient c,int k,int tick){
        var p=UiPagesChecks.pages(c);
        if(k==0){st=0;stAt=0;}
        int d=k-stAt;
        switch(st){
            case 0->{if(d==0)gilded(c);if(d>=6){st=1;stAt=k+1;}}
            case 1->{if(d==0)UiPagesChecks.click(c,row(c,"knight"));if(d>=4){st=2;stAt=k+1;}}
            case 2->{
                sent0=WarTablePages.sent().size();
                var plan=BannerDrop.paint(WarTablePages.table(),13,"knight",WarTablePages.rules());
                expect=plan.ok()?plan.withBanner(WarTablePages.shownBanner()).command():"(refused: "+plan.why()+")";
                UiPagesChecks.click(c,UiPagesChecks.box(c,"cell-13"));st=3;stAt=k+1;
            }
            case 3->{
                var sent=WarTablePages.sent();var mine=new ArrayList<>(sent.subList(Math.min(sent0,sent.size()),sent.size()));
                boolean went=mine.contains(expect),held="knight".equals(WarTablePages.savedTable().get(13));
                if(went&&held||d>120){report("p1-bf-paint-place",went&&held,"a click on cell 13 with the Order Squire brush sent "+mine+" (want "+expect+"), the snapshot holds it "+held+", troops \""+WarTablePages.texts().get("troops")+"\", gold \""+WarTablePages.texts().get("gold")+"\"");st=4;stAt=k+1;}
            }
            case 4->{if(d==0)UiPagesChecks.click(c,row(c,LEGEND));if(d>=4){st=5;stAt=k+1;}}
            case 5->{
                sent0=WarTablePages.sent().size();why=BannerDrop.paint(WarTablePages.table(),20,LEGEND,WarTablePages.rules()).why();
                UiPagesChecks.click(c,UiPagesChecks.box(c,"cell-20"));st=6;stAt=k+1;
            }
            case 6->{if(d>=6){
                int more=WarTablePages.sent().size()-sent0;boolean ok=!why.isEmpty()&&more==0&&p!=null&&why.equals(p.noticeText)&&!WarTablePages.table().containsKey(20);
                report("p1-bf-paint-refused",ok,"a second Legendary on cell 20: why \""+why+"\", commands sent "+more+" (want 0), notice \""+(p==null?"":p.noticeText)+"\", cell 20 "+WarTablePages.table().get(20));st=7;stAt=k+1;}}
            case 7->{if(d==0)UiPagesChecks.click(c,UiPagesChecks.box(c,"tab-1"));if(d>=6){st=8;stAt=k+1;}}
            case 8->{
                var bs=CampaignClient.profile.banners;
                boolean shows=WarTablePages.shownBanner()==1&&bs.size()>1&&WarTablePages.table().equals(WarGrid.parse(bs.get(1).cells))&&"Banner II".equals(WarTablePages.texts().get("tab-shown"));
                note="tab-1 shows banner "+WarTablePages.shownBanner()+" (want 1), its table "+WarTablePages.table()+", shown tab \""+WarTablePages.texts().get("tab-shown")+"\""+(shows?"":" WRONG");
                UiPagesChecks.click(c,UiPagesChecks.box(c,"use"));st=9;stAt=k+1;
            }
            case 9->{
                boolean used=CampaignClient.profile.activeBanner==1;
                if(used||d>120){report("p1-bf-paint-switch",used&&!note.endsWith("WRONG"),note+"; Use made the active banner "+CampaignClient.profile.activeBanner+" (want 1)");note="";st=10;stAt=k+1;}
            }
            case 10->{if(d==0)UiPagesChecks.click(c,row(c,"knight"));if(d>=4){st=11;stAt=k+1;}}
            case 11->{ // a double click on one cell: the place and the take-off are two different commands, so the second is not dropped as a repeat of the first
                if(d==0){sent0=WarTablePages.sent().size();var b=UiPagesChecks.box(c,"cell-5");UiPagesChecks.click(c,b);UiPagesChecks.click(c,b);}
                if(d>=80){
                    var mine=new ArrayList<>(WarTablePages.sent().subList(Math.min(sent0,WarTablePages.sent().size()),WarTablePages.sent().size()));
                    String place=BannerDrop.command(1,5,"knight"),off=BannerDrop.command(1,5,BannerDrop.ERASE);
                    boolean empty=!WarGrid.parse(CampaignClient.profile.banners.get(1).cells).containsKey(5);
                    report("p1-bf-paint-double",mine.equals(List.of(place,off))&&empty,"two quick clicks on cell 5 sent "+mine+" (want ["+place+", "+off+"]), and the server's banner 1 holds cell 5 "+!empty+" (want empty)");
                    st=12;stAt=k+1;}
            }
            case 12->{ // a fast drag over 12 cells in one tick: every one reaches the server
                if(d==0){
                    sent0=WarTablePages.sent().size();var s=UiPagesChecks.screen(c);var b=UiPagesChecks.box(c,"cell-9");
                    double x=UiHomeChecks.gui(c,b[0]+b[2]/2.0),y=UiHomeChecks.gui(c,b[1]+b[3]/2.0);s.mouseMoved(x,y);s.mouseClicked(x,y,0);
                    for(int i=10;i<=20;i++){var q=UiPagesChecks.box(c,"cell-"+i);double qx=UiHomeChecks.gui(c,q[0]+q[2]/2.0),qy=UiHomeChecks.gui(c,q[1]+q[3]/2.0);s.mouseDragged(qx,qy,0,qx-x,qy-y);x=qx;y=qy;}
                    s.mouseReleased(x,y,0);
                }
                if(d>=120){
                    var held=WarGrid.parse(CampaignClient.profile.banners.get(1).cells);List<Integer> missing=new ArrayList<>();for(int i=9;i<=20;i++)if(!"knight".equals(held.get(i)))missing.add(i);
                    report("p1-bf-paint-drag",missing.isEmpty(),"a fast drag over cells 9 to 20 sent "+(WarTablePages.sent().size()-sent0)+" commands; the server's banner 1 lacks cells "+missing+" (want none); notice \""+WarTablePages.texts().get("notice")+"\"");
                    st=13;stAt=k+1;}
            }
            case 13->{ // Delete armed on one banner does not delete another: switching tabs disarms
                var pg=UiPagesChecks.pages(c);
                if(d==0){sent0=WarTablePages.sent().size();UiPagesChecks.click(c,UiPagesChecks.box(c,"delete"));}
                if(d==2){boolean armed=pg!=null&&pg.deleteArmedBanner==1;note="Delete armed on banner "+(pg==null?-2:pg.deleteArmedBanner)+" (want 1)"+(armed?"":" WRONG");UiPagesChecks.click(c,UiPagesChecks.box(c,"tab-0"));}
                if(d==4){note+="; after tab-0 it is armed on "+(pg==null?-2:pg.deleteArmedBanner)+" (want -1)"+(pg!=null&&pg.deleteArmedBanner==-1?"":" WRONG");UiPagesChecks.click(c,UiPagesChecks.box(c,"delete"));}
                if(d==8){
                    var mine=WarTablePages.sent().subList(Math.min(sent0,WarTablePages.sent().size()),WarTablePages.sent().size());
                    boolean deleted=mine.stream().anyMatch(m->m.startsWith("banner:delete:"));
                    report("p1-bf-paint-delete-guard",!note.contains("WRONG")&&!deleted&&CampaignClient.profile.banners.size()==2&&pg!=null&&pg.deleteArmedBanner==0,note+"; the first Delete on banner 0 only armed it: sent "+mine+", banners "+CampaignClient.profile.banners.size()+" (want 2)");
                    note="";UiPagesChecks.click(c,UiPagesChecks.box(c,"tab-1"));st=14;stAt=k+1;}
            }
            case 14->{ // renaming: R (the menu key) and Space are letters of the name, Backspace repeats, the screen stays open
                var s=UiPagesChecks.screen(c);
                if(d==2)UiPagesChecks.click(c,UiPagesChecks.box(c,"rename"));
                if(d==4){
                    var pg=UiPagesChecks.pages(c);String start=pg==null?"":String.valueOf(pg.renaming);
                    UiRecruitChecks.press(c,org.lwjgl.glfw.GLFW.GLFW_KEY_R);if(c.currentScreen!=null)c.currentScreen.charTyped('R',0);UiRecruitChecks.release(c,org.lwjgl.glfw.GLFW.GLFW_KEY_R);
                    UiRecruitChecks.press(c,org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE);if(c.currentScreen!=null)c.currentScreen.charTyped(' ',0);UiRecruitChecks.release(c,org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE);
                    boolean open=UiPagesChecks.screen(c)==s&&s!=null&&UiPagesChecks.pages(c)!=null;String typed=open?String.valueOf(UiPagesChecks.pages(c).renaming):"(closed)";
                    UiRecruitChecks.press(c,org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE);UiRecruitChecks.press(c,org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE);UiRecruitChecks.press(c,org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE);UiRecruitChecks.release(c,org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE);
                    boolean still=UiPagesChecks.screen(c)==s&&UiPagesChecks.pages(c)!=null;String after=still?String.valueOf(UiPagesChecks.pages(c).renaming):"(closed)";
                    String wantTyped=start+"R ",wantAfter=wantTyped.substring(0,Math.max(0,wantTyped.length()-3));
                    report("p1-bf-rename-keys",open&&still&&typed.equals(wantTyped)&&after.equals(wantAfter),"renaming \""+start+"\": R then Space typed \""+typed+"\" with the screen "+(open?"open":"closed")+" (want \""+wantTyped+"\"), three held Backspaces left \""+after+"\" (want \""+wantAfter+"\")");
                    UiRecruitChecks.tap(c,org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE);
                    st=15;stAt=k+1;}
            }
            case 15->{if(d>=4){var pg=UiPagesChecks.pages(c);report("p1-bf-rename-esc",pg!=null&&pg.renaming==null&&UiPagesChecks.screen(c)!=null,"Esc drops the name being written and keeps the table open: renaming "+(pg==null?"(no page)":String.valueOf(pg.renaming))+" (want null)");scene++;go(0,tick);}}
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
