package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Real widget/server tests for the isolated v2 War Table Banner; disposable profiles only. */
final class UiBanner05Checks implements Phase1Checks.Suite {
    static final int[][] SIZES={{854,480,2},{960,480,1},{960,540,1},{1280,720,3},{1920,974,2}};
    int stage,at,size;List<String> before;int sent;
    static WarTableBanner page(MinecraftClient c){return c.currentScreen instanceof CampScreen s?s.banner:null;}
    static void click(MinecraftClient c,String name){var p=page(c);var b=p==null?null:p.button(name);if(b!=null)Phase1Checks.UiRecruitChecks.click(c,b.canvasBox());}
    static boolean inside(int[] r){return r!=null&&r[0]>=0&&r[1]>=0&&r[0]+r[2]<=UiCanvas.width()&&r[1]+r[3]<=UiCanvas.height();}
    static int count(String id){return Collections.frequency(CampaignClient.profile.banner,id);}
    void report(String id,boolean pass,String why){Phase1Checks.report("banner05-"+id,pass,why);}
    void next(int value,int tick){stage=value;at=tick;}
    public void step(MinecraftClient c,int tick){
        if(tick>2500){report("timeout",false,"stage "+stage+", size "+size+", banner "+CampaignClient.profile.banner);Phase1Checks.finish(c,"ui05-banner");return;}
        if(stage==0){var id=c.player.getUuid();Phase1Checks.server(c,b->{var p=b.campaign.profile(id);p.gold=1234;p.owned=Profile.commons();p.owned.put("mage",new Profile.Owned());p.owned.put("gl_marshal",new Profile.Owned());p.banner=new ArrayList<>(Collections.nCopies(20,RosterMath.EMPTY));for(int i=0;i<4;i++)p.banner.set(i,"knight");b.campaign.save(id);});next(1,tick);return;}
        if(stage==1){if(CampaignClient.profile.gold!=1234||count("knight")!=4||count(RosterMath.EMPTY)!=16||System.currentTimeMillis()<CampaignClient.revealUntil)return;var w=SIZES[size];Phase1Checks.window(c,w[0],w[1],w[2]);CampScreen.open("banner");next(2,tick);return;}
        if(stage==2&&tick-at>=15){
            var p=page(c);List<String> bad=new ArrayList<>();for(var e:p.buttons.entrySet())if(e.getValue().visible&&!inside(e.getValue().canvasBox()))bad.add(e.getKey());
            for(var e:p.rects.entrySet())if(!inside(e.getValue()))bad.add(e.getKey());
            for(int i=0;i<20;i++)if(!Objects.equals(p.texts.get("seat-"+i),CampaignClient.profile.banner.get(i)))bad.add("snapshot-"+i);
            for(int i=0;i<20;i++)for(int j=0;j<i;j++)if(Phase1Checks.UiHomeChecks.overlap(p.button("seat-"+i).canvasBox(),p.button("seat-"+j).canvasBox()))bad.add("seat overlap");
            report("layout-"+size,bad.isEmpty(),"canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", problems "+bad);
            Phase1Checks.shot(c,"banner05-layout-"+size);click(c,"commander");next(3,tick);return;
        }
        if(stage==3&&tick-at>=15){
            var l=c.currentScreen instanceof CampaignLobbyScreen s?s:null;int cards=0,clipped=0;if(l!=null)for(var e:l.children())if(e instanceof CampaignLobbyScreen.ClassCard card){cards++;clipped+=card.clipped;if(!card.headerFits)clipped++;}
            report("class-cards-"+size,l!=null&&cards==6&&clipped==0,"selector headers and body: cards "+cards+", clipped lines "+clipped);
            Phase1Checks.shot(c,"banner05-classes-"+size);Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ESCAPE);report("class-return-"+size,page(c)!=null,"Esc returns to Banner");
            if(++size<SIZES.length){var w=SIZES[size];Phase1Checks.window(c,w[0],w[1],w[2]);CampScreen.open("banner");next(2,tick);return;}
            Phase1Checks.window(c,960,540,1);CampScreen.open("banner");next(4,tick);return;
        }
        if(stage==4&&tick-at>=15){var p=page(c);sent=p.sent.size();click(c,"row-2");report("unowned-disabled",!p.button("row-2").active&&p.sent.size()==sent&&!p.armed.equals("sentinel"),"unowned rare cannot be armed");click(c,"row-4");click(c,"seat-4");next(5,tick);return;}
        if(stage==5&&count("mage")==1&&tick-at>=12){report("epic-weight",CampaignClient.profile.banner.get(5).equals("^"),CampaignClient.profile.banner.toString());click(c,"seat-6");next(6,tick);return;}
        if(stage==6&&count("mage")==2&&tick-at>=12){before=List.copyOf(CampaignClient.profile.banner);click(c,"seat-8");next(7,tick);return;}
        if(stage==7&&tick-at>=20){report("epic-limit",before.equals(CampaignClient.profile.banner)&&CampaignClient.notice.contains("No room"),CampaignClient.notice);click(c,"row-5");click(c,"seat-8");next(8,tick);return;}
        if(stage==8&&count("gl_marshal")==1&&tick-at>=12){report("legendary-weight",CampaignClient.profile.banner.subList(9,12).equals(List.of("^","^","^")),CampaignClient.profile.banner.toString());before=List.copyOf(CampaignClient.profile.banner);click(c,"seat-12");next(9,tick);return;}
        if(stage==9&&tick-at>=20){report("legendary-limit",before.equals(CampaignClient.profile.banner)&&count("gl_marshal")==1,CampaignClient.profile.banner.toString());click(c,"row-0");sent=page(c).sent.size();click(c,"seat-5");report("continuation-replace-blocked",!page(c).button("seat-5").active&&page(c).sent.size()==sent,"no edit command from ^ with a troop armed");click(c,"clear");click(c,"seat-10");next(10,tick);return;}
        if(stage==10&&count("gl_marshal")==0&&tick-at>=12){report("markers-visible",page(c).rects.containsKey("marker-5")&&"Unit 5".equals(page(c).texts.get("held-5"))&&page(c).rects.containsKey("marker-7"),"continuation arrows label owner slots 5 and 7");report("clear-continuation",page(c).sent.get(page(c).sent.size()-1).equals("slot:8:empty")&&count("mage")==2,CampaignClient.profile.banner.toString());Phase1Checks.shot(c,"banner05-weighted-after-clear");click(c,"colour-teal");next(11,tick);return;}
        if(stage==11&&CampaignClient.profile.bannerColour.equals("teal")&&tick-at>=12){report("colour-server",true,"server snapshot requests teal");click(c,"house-wildmoot");next(12,tick);return;}
        if(stage==12&&tick-at>=10){var p=page(c);report("unfieldable-disabled",p.house.equals("wildmoot")&&p.armed.isEmpty()&&!p.button("row-0").active&&!p.button("seat-0").active,"owned Wildmoot remains collection-only");Phase1Checks.shot(c,"banner05-collection-only");click(c,"house-graveborn");next(13,tick);return;}
        if(stage==13&&tick-at>=10){click(c,"row-0");click(c,"seat-0");next(14,tick);return;}
        if(stage==14&&count("revenant")==1&&tick-at>=12){report("cross-house-server",count("knight")==3&&RosterMath.validBanner(CampaignClient.profile.banner,RosterMath.Catalog.fromSpec(),CampaignClient.profile.owned.keySet())==null,CampaignClient.profile.banner.toString());
            var p=page(c);p.screen.setFocused(p.button("clear"));Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ENTER);report("keyboard-clear",p.armed.equals(RosterMath.EMPTY),"Enter activates focused Clear");
            click(c,"commander");next(15,tick);return;}
        if(stage==15&&tick-at>=10){var l=(CampaignLobbyScreen)c.currentScreen;for(var e:l.children())if(e instanceof CampaignLobbyScreen.ClassCard card&&card.c.id().equals("marshal")){clickClass(c,l,card);break;}clickClass(c,l,l.confirmClass);next(16,tick);return;}
        if(stage==16&&CampaignClient.profile.commander.equals("marshal")&&tick-at>=12){report("class-confirm-server",true,"existing selector confirms Marshal through server");Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ESCAPE);next(17,tick);return;}
        if(stage==17&&tick-at>=10){report("class-summary",page(c)!=null&&"Marshal".equals(page(c).texts.get("commander-name")),"Banner reads selected class snapshot");click(c,"equipment");report("old-editor-preserved",c.currentScreen instanceof GravesAndGloryClient.LobbyScreen,"Army & equipment opens existing editor");CampScreen.open("recruit");next(18,tick);return;}
        if(stage==18&&tick-at>=10){Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_B);report("recruit-route",page(c)!=null,"Recruit B routes to Banner");next(19,tick);return;}
        if(stage==19&&tick-at>=10){Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ESCAPE);report("back-home",c.currentScreen instanceof CampScreen s&&s.page.equals("home"),"Esc goes home");Phase1Checks.finish(c,"ui05-banner");stage=20;}
    }
    static void clickClass(MinecraftClient c,CampaignLobbyScreen l,ButtonWidget b){double x=(b.getX()+b.getWidth()/2.0)*l.uiScale,y=(b.getY()+b.getHeight()/2.0)*l.uiScale;c.currentScreen.mouseClicked(x,y,0);c.currentScreen.mouseReleased(x,y,0);}
}
