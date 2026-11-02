package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/** Phase 1 suite ui-report (plan 4F.2): the end-of-battle report on the War Table page standard, client only. Synthetic reports are
 *  put where the server's would arrive (CampaignClient.result) and the results page is opened: a Domination, a Siege and a Convoy
 *  round, a mode the report does not know (the generic section), a Survival-style section registered with one call, and a crowded
 *  round (8 commanders with 16-letter names, five Houses, every troop type, many capture points). Checks: the page draws at canvas
 *  scale 3 at 2560x1440 and 2 at 1920x1080; every mode shows its own section and none shows another's; every piece of text and
 *  art sits inside the note or the sheet; nothing overlaps its neighbours; the faces switch; Esc and the primary tag leave.
 *  Shots (run/screenshots/p1-report-*.png): each mode on the Battle face, the crowded round on all three faces, at 2560x1440,
 *  1920x1080, 1280x720 and 854x480. */
final class UiReportChecks implements Suite {
    static final String[] MODES = {Modes.DOMINATION, Modes.SIEGE, Modes.CONVOY, "unknown", "survival"};
    int stage,sub,subAt,oldW,oldH,oldGui;boolean finished;JsonObject want;
    final StringBuilder why=new StringBuilder();boolean ok=true;

    static CampScreen screen(MinecraftClient c){return c.currentScreen instanceof CampScreen s&&s.page.equals("results")?s:null;}
    static WarTableReport page(MinecraftClient c){var s=screen(c);return s==null?null:s.report;}

    /** A mode block for the given mode id, as Campaign.reportMode builds it. */
    static JsonObject mode(String id){
        var m=new JsonObject();m.addProperty("id",id);
        switch(id){
            case "siege"->{m.addProperty("name","Siege");m.addProperty("rule","Take every stage of the keep before the time runs out.");m.addProperty("points",false);arr(m,"ledger",2,100);arr(m,"max",3,100);strs(m,"format","fraction","percent");}
            case "convoy"->{m.addProperty("name","Convoy");m.addProperty("rule","Escort the wagon along the road to the last stop.");m.addProperty("points",false);arr(m,"ledger",64,2);arr(m,"max",100,3);strs(m,"format","percent","fraction");}
            case "unknown"->{m.addProperty("name","Unknown Mode");m.addProperty("rule","A mode with no section of its own.");m.addProperty("points",false);arr(m,"ledger",7,9);arr(m,"max",10,10);strs(m,"format","plain","plain");}
            case "survival"->{m.addProperty("name","Survival");m.addProperty("rule","Hold the line against the waves.");m.addProperty("points",false);arr(m,"ledger",5,0);arr(m,"max",0,0);strs(m,"format","plain","plain");}
            default->{m.addProperty("name","Domination");m.addProperty("rule","Hold more points than the enemy to fill your score first.");m.addProperty("points",true);arr(m,"ledger",412,377);arr(m,"max",500,500);strs(m,"format","plain","plain");}
        }
        return m;
    }
    static void arr(JsonObject o,String k,int a,int b){var j=new JsonArray();j.add(a);j.add(b);o.add(k,j);}
    static void strs(JsonObject o,String k,String a,String b){var j=new JsonArray();j.add(a);j.add(b);o.add(k,j);}

    /** A report for the mode: the viewer's team of 'mine' commanders and the other team of 'theirs', the given capture points, Houses and troop levels. */
    static JsonObject synth(String modeId,int mine,int theirs,boolean longNames,int points,int houses,int troops,boolean saved){
        var r=new JsonObject();r.addProperty("id","ui-report-"+modeId+"-"+mine+theirs);r.addProperty("outcome",modeId.equals("siege")?"BATTLE COMPLETE":"VICTORY");
        r.addProperty("map","Kingsfield Crossroads");r.addProperty("seconds",437);r.addProperty("saved",saved);r.addProperty("eligible",true);r.addProperty("gold",saved?46:0);r.addProperty("xp",saved?120:0);r.addProperty("beforeXp",340);
        var squads=new JsonArray();String[] names=longNames?new String[]{"WWWWWWWWWWWWWWWW","MMMMMMMMMMMMMMMM","hard bot","hard bot","QQQQQQQQQQQQQQQQ","normal bot","normal bot","normal bot"}:new String[]{"Kort","Aldric","normal bot","hard bot","easy bot","normal bot","Maffhew","hard bot"};
        for(int i=0;i<mine+theirs;i++){var row=new JsonObject();String n=names[i%names.length];boolean bot=n.endsWith(" bot");row.addProperty("name",n);row.addProperty("team",i<mine?0:1);row.addProperty("bot",bot);row.addProperty("tier",bot?n.substring(0,n.length()-4):"");row.addProperty("you",i==0);
            row.addProperty("kills",longNames?212-i:14+i);row.addProperty("lost",longNames?88+i:6+i);row.addProperty("gold",longNames?1180+37*i:310+40*i);row.addProperty("objective",longNames?640+13*i:120+9*i);squads.add(row);}
        r.add("squads",squads);
        var pts=new JsonArray();String[] pn={"Old Mill","Chapel Hill","Ford","Watch Tower","Quarry","Orchard","Bridge","Crossroads"};
        for(int i=0;i<points;i++){var p=new JsonObject();p.addProperty("name",pn[i%pn.length]);p.addProperty("dawn",longNames?700+i*11:120+i*13);p.addProperty("dusk",longNames?600+i*7:90+i*5);pts.add(p);}
        r.add("points",pts);
        var hs=new JsonArray();var order=Profile.houseOrder();
        for(int i=0;i<houses&&i<order.size();i++){var h=new JsonObject();h.addProperty("house",order.get(i));h.addProperty("tier",1+i%3);h.addProperty("traitSeconds",longNames?312+i*20:95+i*10);h.addProperty("shards",saved?3+i:0);h.addProperty("renown",saved?1+i:0);hs.add(h);}
        r.add("houses",hs);
        var lv=new JsonArray();var ids=new ArrayList<String>();for(var t:Spec.TROOPS)ids.add(t.id());
        for(int i=0;i<troops&&i<ids.size();i++){var l=new JsonObject();l.addProperty("troop",ids.get(i));l.addProperty("highestLevel",1+i%3);l.addProperty("level3Count",i%3==2?2+i:0);l.addProperty("weapon",1+i%3);l.addProperty("armour",1+(i+1)%3);lv.add(l);}
        r.add("levels",lv);r.add("mode",mode(modeId));
        return r;
    }

    /** Whether box a lies inside box b. */
    static boolean in(int[] a,int[] b){return a!=null&&b!=null&&a[0]>=b[0]&&a[1]>=b[1]&&a[0]+a[2]<=b[0]+b[2]&&a[1]+a[3]<=b[1]+b[3];}
    static boolean touches(int[] a,int[] b){return a[0]<b[0]+b[2]&&b[0]<a[0]+a[2]&&a[1]<b[1]+b[3]&&b[1]<a[1]+a[3];}

    /** The page as the last frame drew it: every box of the report's own inside the note or the sheet (the rail, back plaque and floor are the War Table's); the returned text is empty when all is well. */
    String layout(MinecraftClient c){
        var rs=WarTableReport.rects();var note=rs.get("note");var sheet=rs.get("sheet");
        if(note==null||sheet==null)return "no note or sheet drawn (note "+(note!=null)+", sheet "+(sheet!=null)+")";
        StringBuilder bad=new StringBuilder();Set<String> table=WarTable.drawn().keySet();
        for(var e:rs.entrySet()){
            String k=e.getKey();if(k.equals("note")||k.equals("sheet")||table.contains(k)&&!k.startsWith("tab-")&&!k.startsWith("note-"))continue;
            var b=e.getValue();
            if(!(in(b,note)||in(b,sheet)))bad.append(k).append(" ").append(Arrays.toString(b)).append(" outside both papers; ");
        }
        // the text blocks of one paper do not run into each other
        List<String> keys=new ArrayList<>();for(var k:rs.keySet())if(WarTableReport.texts().containsKey(k))keys.add(k);
        for(int i=0;i<keys.size();i++)for(int j=i+1;j<keys.size();j++){
            var a=rs.get(keys.get(i));var b=rs.get(keys.get(j));
            if(touches(shrink(a),shrink(b)))bad.append(keys.get(i)).append(" overlaps ").append(keys.get(j)).append("; ");
        }
        return bad.toString();
    }
    static int[] shrink(int[] a){return new int[]{a[0]+1,a[1]+3,Math.max(1,a[2]-2),Math.max(1,a[3]-6)};}

    void note(String id,boolean pass,String detail){if(!pass){ok=false;}report(id,pass,detail);}

    static final int[][] SIZES={{2560,1440,3},{1920,1080,2},{1280,720,1},{854,480,1}};

    int size,step,rep;String[] shotMode;

    public void step(MinecraftClient c,int tick){
        if(finished)return;
        if(tick>6000){finished=true;report("p1-ui-report-timeout",false,"stuck in stage "+stage+" sub "+sub);finish(c,"ui-report");return;}
        if(stage==0){
            if(tick<20)return;var w=c.getWindow();oldW=w.getWidth();oldH=w.getHeight();oldGui=c.options.getGuiScale().getValue();
            c.setScreen(null);size=0;window(c,SIZES[0][0],SIZES[0][1],0,true);stage=1;subAt=tick;return;
        }
        if(want!=null&&CampaignClient.result!=want)CampaignClient.result=want; // the camp's state packets put the server's report back: the test's own is held every tick
        if(tick-subAt<8)return;
        var sz=SIZES[size];String tag=sz[0]+"x"+sz[1];
        switch(stage){
            case 1->{ // show the mode's report on the Battle face
                String m=MODES[sub];
                if("survival".equals(m)&&!ReportSections.has("survival"))ReportSections.register("survival",r->List.of(new ReportSections.Row("Waves held","5"),new ReportSections.Row("Best wave","7")));
                CampaignClient.result=want=synth(m,3,3,false,m.equals(Modes.DOMINATION)?5:0,3,6,true);
                CampScreen.open("results");stage=2;subAt=tick;
            }
            case 2->{
                var p=page(c);String m=MODES[sub];
                if(p==null){if(tick-subAt>100){note("p1-ui-report-open",false,"the results page did not open for "+m);finished=true;finish(c,"ui-report");}return;}
                if(WarTableReport.section().isEmpty()&&tick-subAt<80)return; // a frame that drew the sections has not run yet (a window change drops frames)
                var rows=WarTableReport.section();String lay=layout(c);
                var labels=new ArrayList<String>();for(var r:rows)labels.add(r.label());
                boolean own=switch(m){
                    case Modes.DOMINATION->labels.contains("Dawn score")&&labels.contains("Dusk score")&&labels.size()==2+5;
                    case Modes.SIEGE->labels.equals(List.of("Stages taken","Stage strength"));
                    case Modes.CONVOY->labels.equals(List.of("Escort progress","Stops held"));
                    case "unknown"->labels.equals(List.of("Dawn","Dusk"));
                    default->labels.equals(List.of("Waves held","Best wave"));};
                boolean pointsCol=WarTableReport.texts().containsKey("header-objective");
                note("p1-ui-report-section-"+m+"-"+tag,own&&lay.isEmpty(),"mode "+m+" at "+tag+" (canvas k "+UiCanvas.k()+", want "+sz[2]+"): section rows "+labels+" "+(own?"are its own":"are WRONG")+(lay.isEmpty()?", layout clean":", layout: "+lay)+(m.equals(Modes.DOMINATION)?"; Points column drawn="+pointsCol:"; Points column drawn="+pointsCol+" (want false)"));
                if(!m.equals(Modes.DOMINATION)&&pointsCol)note("p1-ui-report-points-"+m+"-"+tag,false,"mode "+m+" draws the Points column");
                if(sz[2]!=UiCanvas.k()&&size<2)note("p1-ui-report-scale-"+tag,false,"canvas k "+UiCanvas.k()+" at "+tag+", want "+sz[2]);
                if(size<2||m.equals(Modes.DOMINATION))shot(c,"report-"+m+"-"+tag);
                stage=3;subAt=tick;
            }
            case 3->{ // next mode, then the crowded round on every face, then the next window size
                if(sub<MODES.length-1){sub++;stage=1;return;}
                if(rep==0){CampaignClient.result=want=synth(Modes.DOMINATION,4,4,true,8,5,Spec.TROOPS.length,true);CampScreen.open("results");rep=1;step=0;subAt=tick;stage=4;return;}
                stage=5;
            }
            case 4->{ // crowded: each face
                var p=page(c);if(p==null)return;
                String[] faces=WarTableReport.FACES;
                String lay=layout(c);note("p1-ui-report-crowded-"+faces[step]+"-"+tag,lay.isEmpty()&&faces[step].equals(WarTableReport.texts().get("face")),"crowded round, "+faces[step]+" face at "+tag+" (drawn face "+WarTableReport.texts().get("face")+"): "+(lay.isEmpty()?"all inside the papers, no overlaps":lay));
                shot(c,"report-crowded-"+faces[step]+"-"+tag);
                step++;
                if(step<faces.length){var b=p.button("tab-"+faces[step]);if(b!=null)WarTableRecruit.press(b);subAt=tick;return;}
                stage=5;
            }
            case 5->{
                rep=0;sub=0;
                if(size<SIZES.length-1){size++;window(c,SIZES[size][0],SIZES[size][1],0,true);stage=1;subAt=tick;return;}
                stage=6;subAt=tick;
            }
            case 6->{ // behaviour: a report with nothing saved, Esc, the primary tag
                CampaignClient.result=want=synth(Modes.DOMINATION,2,2,false,3,2,3,false);CampScreen.open("results");stage=7;subAt=tick;
            }
            case 7->{
                var p=page(c);if(p==null)return;
                if(!WarTableReport.texts().containsKey("outcome")&&tick-subAt<80)return;
                boolean notSaved=WarTableReport.texts().values().stream().anyMatch(t->t.contains("not saved"));
                note("p1-ui-report-unsaved",notSaved,"a report with nothing saved says so: "+notSaved);
                var go=p.button("go");boolean live=go!=null&&go.active&&go.visible;
                note("p1-ui-report-go",live,"primary tag "+(live?"live":"missing or dull")+", label "+(go==null?"-":go.getMessage().getString()));
                c.currentScreen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE,0,0);
                stage=8;subAt=tick;
            }
            case 8->{
                boolean home=c.currentScreen instanceof CampScreen s&&s.page.equals("home");
                note("p1-ui-report-esc",home,"Esc from the report lands on "+(c.currentScreen instanceof CampScreen s?s.page:String.valueOf(c.currentScreen)));
                finished=true;finish(c,"ui-report");
            }
            default->{}
        }
    }

    @Override public void cleanup(MinecraftClient c){
        want=null;CampaignClient.result=new JsonObject();
        if(oldW>0){window(c,oldW,oldH,oldGui);}
        c.setScreen(null);
    }
}
