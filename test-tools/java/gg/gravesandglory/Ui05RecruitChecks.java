package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Preview-contract checks in a disposable integrated server. Not part of the player JAR. */
final class Ui05RecruitChecks implements Phase1Checks.Suite {
    static final int[][] SIZES={{854,480,2},{960,540,1},{1280,720,3}};
    int stage,at,size,house; int gold,shards; String epic;
    static CampScreen screen(MinecraftClient c){return c.currentScreen instanceof CampScreen s?s:null;}
    static boolean inside(int[] r){return r!=null&&r[0]>=0&&r[1]>=0&&r[0]+r[2]<=UiCanvas.width()&&r[1]+r[3]<=UiCanvas.height();}
    static void click(MinecraftClient c,UiButton b){if(b!=null)Phase1Checks.UiRecruitChecks.click(c,b.canvasBox());}
    void report(String name,boolean pass,String why){Phase1Checks.report("ui05-"+name,pass,why);}
    public void step(MinecraftClient c,int tick){
        if(tick>2400){report("timeout",false,"stage "+stage+", size "+size+", House "+house);Phase1Checks.finish(c,"ui05-recruit");return;}
        if(stage==0){
            var id=c.player.getUuid();Phase1Checks.server(c,b->{var p=b.campaign.profile(id);p.gold=2000;p.owned=Profile.commons();p.pity.clear();p.shards.clear();p.pity.put("gilded",39);b.campaign.save(id);});
            at=tick;stage=1;return;
        }
        if(stage==1){
            if(CampaignClient.profile.gold!=2000||CampaignClient.profile.pity.getOrDefault("gilded",0)!=39)return;
            var w=SIZES[size];Phase1Checks.window(c,w[0],w[1],w[2]);CampScreen.open("recruit");at=tick;stage=2;return;
        }
        if(stage==2&&tick-at>=15){
            var s=screen(c);s.recruit.chooseHouse(Spec.HOUSES[house].id());at=tick;stage=3;return;
        }
        if(stage==3&&tick-at>=10){
            Phase1Checks.UiRecruitChecks.park(c);var s=screen(c);var r=s.recruit;String h=Spec.HOUSES[house].id();
            List<String> bad=new ArrayList<>();
            if(RecruitView.houses().size()!=5||RecruitView.roll(h).size()!=6||RecruitView.newLeft(h)!=5)bad.add("House pool");
            for(String key:List.of("roll","letter","bundle","enlist","odds"))if(!inside(WarTableRecruit.rects().get(key)))bad.add(key);
            for(var e:r.buttons.entrySet())if(e.getValue().visible&&!inside(e.getValue().canvasBox()))bad.add(e.getKey());
            if(r.button("choose")!=null||WarTableRecruit.sent().stream().anyMatch(cmd->cmd.startsWith("unlock:")))bad.add("unsupported purchase");
            String availability=WarTableRecruit.texts().get("collection-1");
            if(availability==null||availability.contains("not fieldable")!=!Spec.HOUSES[house].fieldable())bad.add("availability");
            report("layout-"+size+"-"+h,bad.isEmpty(),"canvas "+UiCanvas.width()+"x"+UiCanvas.height()+", "+availability+", problems "+bad);
            click(c,r.button("odds"));at=tick;stage=4;return;
        }
        if(stage==4&&tick-at>=5){
            var expected=RecruitView.odds(Spec.HOUSES[house].id());boolean ok=expected.size()==7&&inside(WarTableRecruit.rects().get("note"));
            for(int i=0;i<expected.size();i++)ok&=expected.get(i).equals(WarTableRecruit.texts().get("note-"+i));
            ok&=expected.contains("Common (each): 43.0%")&&expected.contains("Rare (each): 6.0%")&&expected.contains("Epic (each): 2.0%");
            report("odds-"+size+"-"+house,ok,expected.toString());
            Phase1Checks.shot(c,"ui05-odds-"+size+"-"+Spec.HOUSES[house].id());
            Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_ESCAPE);
            if(++house<Spec.HOUSES.length){stage=2;at=tick;return;}
            house=0;
            if(++size<SIZES.length){var w=SIZES[size];Phase1Checks.window(c,w[0],w[1],w[2]);CampScreen.open("recruit");at=tick;stage=2;return;}
            var r=screen(c).recruit;r.chooseHouse("gilded");gold=CampaignClient.profile.gold;click(c,r.button("enlist"));at=tick;stage=5;return;
        }
        if(stage==5){
            epic=Arrays.stream(Spec.TROOPS).filter(t->t.house().equals("gilded")&&t.rarity().equals("epic")).findFirst().orElseThrow().id();
            if(CampaignClient.profile.gold!=gold-RecruitView.cost()||!CampaignClient.profile.owned.containsKey(epic))return;
            if(tick-at<15)return;
            report("real-new-roll",CampaignClient.profile.pity.getOrDefault("gilded",-1)==0,"gold "+gold+" -> "+CampaignClient.profile.gold+", Epic "+epic+", pity reset");
            var id=c.player.getUuid();Phase1Checks.server(c,b->{var p=b.campaign.profile(id);var o=p.owned.get(epic);o.stars=3;o.copies=4;p.pity.put("gilded",39);p.shards.put("gilded",7);var knight=p.owned.get("knight");knight.stage=2;knight.mastery=1;b.campaign.save(id);});at=tick;stage=6;return;
        }
        if(stage==6){
            var p=CampaignClient.profile;if(p.pity.getOrDefault("gilded",0)!=39||p.shards.getOrDefault("gilded",0)!=7||p.owned.get("knight").stage!=2)return;
            var r=screen(c).recruit;r.selected="knight";r.refresh();at=tick;stage=7;return;
        }
        if(stage==7&&tick-at>=10){
            report("stage-profile",RecruitView.name("knight").equals(WarTableRecruit.texts().get("letter-name"))&&"Stage 2 / Mastery 1 / Copies 1".equals(WarTableRecruit.texts().get("collection-0")),WarTableRecruit.texts().toString());
            report("stage-portrait",WarTableRecruit.model("knight").getName().getString().equals(RecruitView.name("knight")),"portrait "+WarTableRecruit.model("knight").getName().getString());
            Phase1Checks.shot(c,"ui05-collection-stage");gold=CampaignClient.profile.gold;shards=7;click(c,screen(c).recruit.button("enlist"));at=tick;stage=8;return;
        }
        if(stage==8){
            var p=CampaignClient.profile;if(p.gold!=gold-RecruitView.cost()||p.shards.getOrDefault("gilded",0)<=shards)return;
            if(tick-at<15)return;
            report("real-duplicate-roll",p.shards.get("gilded")==shards+RosterMath.rarityRow("epic").dismantleShards()&&p.owned.get(epic).copies==4,"gold "+p.gold+", shards "+shards+" -> "+p.shards.get("gilded"));
            var id=c.player.getUuid();Phase1Checks.server(c,b->{var v=b.campaign.profile(id);v.gold=0;b.campaign.save(id);});at=tick;stage=9;return;
        }
        if(stage==9){
            if(CampaignClient.profile.gold!=0||tick-at<15)return;
            var r=screen(c).recruit;int sent=WarTableRecruit.sent().size();click(c,r.button("enlist"));report("insufficient-gold",!r.canEnlist()&&WarTableRecruit.sent().size()==sent,"disabled roll emits no command");
            Phase1Checks.UiRecruitChecks.tap(c,GLFW.GLFW_KEY_B);report("banner-route",c.currentScreen instanceof GravesAndGloryClient.LobbyScreen,"B opens existing v2 army editor");
            Phase1Checks.window(c,854,480,2);CampScreen.open("recruit");
            var id=c.player.getUuid();Phase1Checks.server(c,b->b.campaign.handle(b.server.getPlayerManager().getPlayer(id),"roll:gilded"));at=tick;stage=10;
        }
        if(stage==10&&tick-at>=20){
            var r=screen(c).recruit;var notice=WarTableRecruit.rects().get("notice");boolean clear=inside(notice);
            if(notice!=null)for(var b:r.buttons.values())if(b.visible)clear&=!Phase1Checks.UiHomeChecks.overlap(notice,b.canvasBox());
            report("real-refusal-feedback",clear&&WarTableRecruit.texts().getOrDefault("notice","").contains("costs"),"notice "+Arrays.toString(notice)+" / "+WarTableRecruit.texts().get("notice"));
            Phase1Checks.shot(c,"ui05-compact-refusal");
            CampaignClient.homeMode="practice";CampScreen.open("home");at=tick;stage=11;
        }
        if(stage==11&&tick-at>=15){
            var r=WarTableHome.rects();int[] field=r.get("field");boolean ok=inside(field);
            for(String name:WarTableHome.MODES)ok&=inside(r.get(name))&&!Phase1Checks.UiHomeChecks.overlap(r.get(name),field);
            report("compact-home",ok,"home sheets and field card inside canvas without overlap");
            Phase1Checks.shot(c,"ui05-compact-home");Phase1Checks.finish(c,"ui05-recruit");stage=12;
        }
    }
}
