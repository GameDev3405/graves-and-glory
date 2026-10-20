package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import java.util.*;

/** Tests only the unfinished Task 7 pages, using a disposable client world. */
final class UiContinuationChecks implements Phase1Checks.Suite {
    static final String FEEDBACK = "Your request could not be completed. Return to the war table and try again after the current battle has ended.";
    static final String[] PAGES = {"banner", "contracts", "chapter", "friends", "field"};
    static final int[][] WINDOWS = {{854,480,2},{1280,720,3},{1920,974,1}};
    int stage, at, page, size;
    UUID player;
    public void step(MinecraftClient c, int tick) {
        if (tick > 3000) { Phase1Checks.report("p1-ui-continuation-timeout",false,"stage "+stage); Phase1Checks.finish(c,"ui-continuation"); return; }
        if (stage == 0) {
            player = c.player.getUuid();
            c.setScreen(null);
            Phase1Checks.server(c,b->{if(b.running)b.campaign.returnLobby();b.campaign.social.leaveRoom(player);b.campaign.social.leaveParty(player);});
            stage=1; at=tick; return;
        }
        if (stage == 1 && tick-at >= 10) {
            int[] w=WINDOWS[size]; Phase1Checks.window(c,w[0],w[1],w[2]);
            CampScreen.open(PAGES[page]); stage=2; at=tick; return;
        }
        if (stage == 2 && tick-at >= 15) {
            Phase1Checks.UiRecruitChecks.park(c);
            Phase1Checks.server(c,b->b.campaign.notice(player,FEEDBACK));
            stage=3; at=tick; return;
        }
        if (stage == 3 && tick-at >= 20) {
            var screen=c.currentScreen instanceof CampScreen s?s:null;
            int[] notice=WarTablePages.rects().get("notice");
            boolean shown=FEEDBACK.equals(WarTablePages.texts().get("notice"));
            boolean exact=UiCanvas.width()==WINDOWS[size][0]&&UiCanvas.height()==WINDOWS[size][1];
            boolean within=notice!=null&&notice[0]>=0&&notice[1]>=(UiCanvas.height()<540?UiTex.entry("table/rail").h()+2:0)&&notice[0]+notice[2]<=UiCanvas.width()&&notice[1]+notice[3]<=UiCanvas.height();
            List<String> clipped=new ArrayList<>(),overlap=new ArrayList<>();
            if(screen!=null&&screen.pages!=null){
                var buttons=new ArrayList<>(screen.pages.buttons.entrySet());
                for(int i=0;i<buttons.size();i++){
                    var b=buttons.get(i).getValue(); if(!b.visible)continue; int[] r=b.canvasBox();
                    if(r[0]<0||r[1]<0||r[0]+r[2]>UiCanvas.width()||r[1]+r[3]>UiCanvas.height())clipped.add(buttons.get(i).getKey());
                    for(int j=i+1;j<buttons.size();j++)if(buttons.get(j).getValue().visible&&Phase1Checks.UiHomeChecks.overlap(r,buttons.get(j).getValue().canvasBox()))overlap.add(buttons.get(i).getKey()+"/"+buttons.get(j).getKey());
                }
            } else clipped.add("page missing");
            Phase1Checks.report("p1-ui-continuation-"+PAGES[page]+"-"+WINDOWS[size][0],exact&&shown&&within&&clipped.isEmpty()&&overlap.isEmpty(),
                "canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", notice "+Arrays.toString(notice)+", shown "+shown+", clipped controls "+clipped+", overlaps "+overlap);
            Phase1Checks.shot(c,"continuation-"+PAGES[page]+"-"+WINDOWS[size][0]);
            if(size==0&&page==0){stage=4;at=tick;return;}
            advance(c,tick);
        }
        if(stage==4&&tick-at>WarTableHome.NOTICE_TICKS+WarTableHome.NOTICE_FADE_TICKS+5){
            int[] heading=WarTablePages.rects().get("title");
            boolean restored=heading!=null&&heading[1]>=UiTex.entry("table/rail").h()+2&&"Your Banner".equals(WarTablePages.texts().get("title"))&&!WarTablePages.rects().containsKey("notice");
            Phase1Checks.report("p1-ui-continuation-heading-restored",restored,"heading "+WarTablePages.texts().get("title")+", notice "+Arrays.toString(WarTablePages.rects().get("notice")));
            Phase1Checks.shot(c,"continuation-banner-restored-854");advance(c,tick);
        }
    }
    void advance(MinecraftClient c,int tick){
        if(++page==PAGES.length){page=0;size++;}
        if(size==WINDOWS.length){c.setScreen(null);Phase1Checks.finish(c,"ui-continuation");return;}
        stage=1;at=tick;
    }
}
