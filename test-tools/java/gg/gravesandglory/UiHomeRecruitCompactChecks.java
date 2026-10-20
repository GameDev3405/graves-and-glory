package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Layout and real widget input checks for the home/recruit compact continuation. Disposable worlds only. */
final class UiHomeRecruitCompactChecks implements Phase1Checks.Suite {
    static final int[][] SIZES={{854,480,2},{920,500,2},{960,480,1},{960,540,1},{1280,720,3}};
    static final String NOTICE="A unit roll costs 100 gold. Earn gold by completing battles.";
    int stage,at,size,mode;
    boolean rapidOk;
    static boolean inside(int[] r){return r!=null&&r[0]>=0&&r[1]>=0&&r[0]+r[2]<=UiCanvas.width()&&r[1]+r[3]<=UiCanvas.height();}
    static CampScreen screen(MinecraftClient c){return c.currentScreen instanceof CampScreen s?s:null;}
    static void click(MinecraftClient c,UiButton b){if(b!=null)Phase1Checks.UiRecruitChecks.click(c,b.canvasBox());}
    public void step(MinecraftClient c,int tick){
        if(tick>2500){Phase1Checks.report("p1-ui-compact-timeout",false,"stage "+stage);Phase1Checks.finish(c,"ui-compact");return;}
        if(stage==0){
            var w=SIZES[size];Phase1Checks.window(c,w[0],w[1],w[2]);CampaignClient.homeMode="practice";
            CampScreen.open("home");mode=0;at=tick;stage=1;return;
        }
        if(stage==1&&tick-at>=15){
            Phase1Checks.UiRecruitChecks.park(c);
            var s=screen(c);var r=WarTableHome.rects();List<String> bad=new ArrayList<>();
            boolean exact=UiCanvas.width()==SIZES[size][0]&&UiCanvas.height()==SIZES[size][1];
            int[] field=r.get("field");
            if(!inside(field)||field[0]+172>UiCanvas.width())bad.add("field card/shadow");
            for(int i=0;i<3;i++){
                int[] box=r.get(WarTableHome.MODES[i]);
                if(!inside(box)||Phase1Checks.UiHomeChecks.overlap(box,field))bad.add(WarTableHome.MODES[i]);
                for(int j=0;j<i;j++)if(Phase1Checks.UiHomeChecks.overlap(box,r.get(WarTableHome.MODES[j])))bad.add("sheet overlap");
            }
            if(s==null||s.home==null||s.home.focused!=mode)bad.add("focus");
            Phase1Checks.report("p1-ui-compact-home-"+size+"-"+mode,exact&&bad.isEmpty(),"canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", field "+Arrays.toString(field)+", problems "+bad);
            Phase1Checks.shot(c,"compact-home-"+SIZES[size][0]+"x"+SIZES[size][1]+"-"+WarTableHome.MODES[mode]);
            if(++mode<3){click(c,s.home.button(WarTableHome.MODES[mode]));at=tick;return;}
            if(WarTableHome.compact()){rapidOk=true;at=tick;stage=5;return;}
            CampScreen.open("recruit");at=tick;stage=2;return;
        }
        if(stage==5){
            Phase1Checks.UiRecruitChecks.park(c);
            var r=WarTableHome.rects();int[] field=r.get("field");
            for(String name:WarTableHome.MODES){int[] b=r.get(name);rapidOk&=inside(b)&&b[0]+b[2]+6<=field[0];}
            if(tick-at>=30){
                Phase1Checks.report("p1-ui-compact-rapid-focus-"+size,rapidOk,"30 consecutive focus changes, paper and shadows clear of the field card: "+rapidOk);
                CampScreen.open("recruit");at=tick;stage=2;return;
            }
            click(c,screen(c).home.button(WarTableHome.MODES[(tick-at)%3]));return;
        }
        if(stage==2&&tick-at>=15){
            var s=screen(c);click(c,s.recruit.button("row-1"));at=tick;stage=3;return;
        }
        if(stage==3&&tick-at>=5){
            var s=screen(c);boolean picked=s!=null&&s.recruit!=null&&"archer".equals(s.recruit.selected);
            click(c,s.recruit.button("odds"));boolean opened=s.recruit.odds;
            Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ESCAPE);
            boolean closed=!s.recruit.odds&&screen(c)==s;
            Phase1Checks.report("p1-ui-compact-input-"+size,picked&&opened&&closed,"row selected "+s.recruit.selected+", Odds opened "+opened+", Esc closed only Odds "+closed);
            var id=c.player.getUuid();Phase1Checks.server(c,b->b.campaign.notice(id,NOTICE));at=tick;stage=4;return;
        }
        if(stage==4&&tick-at>=20){
            Phase1Checks.UiRecruitChecks.park(c);var r=WarTableRecruit.rects();List<String> bad=new ArrayList<>();
            for(String name:List.of("roll","mark","letter","bundle","notice"))if(!inside(r.get(name)))bad.add(name+" "+Arrays.toString(r.get(name)));
            var s=screen(c);for(var e:s.recruit.buttons.entrySet())if(e.getValue().visible&&!inside(e.getValue().canvasBox()))bad.add("button "+e.getKey());
            boolean shown=NOTICE.equals(WarTableRecruit.texts().get("notice"));
            if(WarTableRecruit.compact()||UiCanvas.height()<540){
                int[] notice=r.get("notice");if(notice==null||notice[1]<UiTex.entry("table/rail").h()+2)bad.add("notice under rail");
                for(var e:s.recruit.buttons.entrySet())if(e.getValue().visible&&notice!=null&&Phase1Checks.UiHomeChecks.overlap(notice,e.getValue().canvasBox()))bad.add("notice overlaps "+e.getKey());
            }
            Phase1Checks.report("p1-ui-compact-recruit-"+size,shown&&bad.isEmpty(),"canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", feedback \""+WarTableRecruit.texts().get("notice")+"\", problems "+bad);
            Phase1Checks.shot(c,"compact-recruit-"+SIZES[size][0]+"x"+SIZES[size][1]);
            if(++size==SIZES.length){c.setScreen(null);Phase1Checks.finish(c,"ui-compact");return;}
            stage=0;
        }
    }
}
