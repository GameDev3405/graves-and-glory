package gg.gravesandglory;
import net.minecraft.client.MinecraftClient;
import java.util.*;
final class BeforeBannerCapture implements Phase1Checks.Suite {
 int stage,at;
 public void step(MinecraftClient c,int tick){
  if(tick>1200){Phase1Checks.report("before-capture-timeout",false,"stage "+stage);Phase1Checks.finish(c,"ui-before");return;}
  if(stage==0){var id=c.player.getUuid();Phase1Checks.server(c,b->{var p=b.campaign.profile(id);p.gold=1234;p.commander="champion";p.bannerColour="gold";p.owned=Profile.commons();p.owned.put("mage",new Profile.Owned());p.owned.put("gl_marshal",new Profile.Owned());p.banner=new ArrayList<>(Collections.nCopies(20,RosterMath.EMPTY));for(int i=0;i<4;i++)p.banner.set(i,"knight");b.campaign.save(id);});stage=1;return;}
  if(stage==1){if(CampaignClient.profile.gold!=1234||System.currentTimeMillis()<CampaignClient.revealUntil)return;Phase1Checks.window(c,854,480,2);CampScreen.open("army");stage=2;at=tick;return;}
  if(stage==2&&tick-at>=20){Phase1Checks.UiRecruitChecks.park(c);Phase1Checks.shot(c,"banner-before-854");((CampaignLobbyScreen)c.currentScreen).showTab(2);stage=3;at=tick;return;}
  if(stage==3&&tick-at>=20){Phase1Checks.UiRecruitChecks.park(c);Phase1Checks.shot(c,"classes-before-854");Phase1Checks.report("before-capture",true,"original editor and class screen, matching after fixture and window");Phase1Checks.finish(c,"ui-before");stage=4;}
 }
}
