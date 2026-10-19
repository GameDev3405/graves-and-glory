package gg.gravesandglory;

import com.google.gson.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;
import static gg.gravesandglory.GravesAndGloryClient.*;

/** Quiet camp navigation; social, recruitment and progression have separate destinations. */
final class CampScreen extends ArmyScreen {
    String page;float scale=1;int offset;String key="",roomInput="";long requestAt;TextFieldWidget code;
    /** The War Table home (page home), kept while this screen lives so its focus and animation survive a rebuild. */
    WarTableHome home;
    /** The War Table recruit page (page recruit), kept while the page is open; leaving the page drops it, so a later visit opens on the first House. */
    WarTableRecruit recruit;
    /** The keys whose press the recruit page took, not released since. Minecraft hands a screen each repeat of a held key as another keyPressed: those go no further, on whatever page the press left the table at (a held Esc that went home must not then close it), until keyReleased lets the key through again. */
    final Set<Integer> held=new HashSet<>();
    CampScreen(String page){super("Banner camp");this.page=page;}
    static void open(String page){var c=net.minecraft.client.MinecraftClient.getInstance();if(page.equals("army"))c.setScreen(new LobbyScreen(1));else c.setScreen(new CampScreen(page));}
    JsonObject social(){return CampaignClient.social;}
    boolean flag(String name){return social().has(name)&&social().get(name).getAsBoolean();}
    String value(String name){return social().has(name)?social().get(name).getAsString():"";}
    int number(String name,int fallback){return social().has(name)?social().get(name).getAsInt():fallback;}
    JsonArray rows(String name){return social().has(name)?social().getAsJsonArray(name):new JsonArray();}
    /** Sends cmd to the server unless a command went less than 300 ms ago; returns whether it went. */
    boolean action(String cmd){if(System.currentTimeMillis()-requestAt<300)return false;requestAt=System.currentTimeMillis();send(cmd);return true;}
    /** Until UI Task 7 builds the field picker, page field (the home's Change) is the map voting page, with the bots' tier. */
    void page(String next){page=next.equals("field")?"match":next;offset=0;if(!page.equals("recruit"))recruit=null;clearAndInit();}
    void rebuild(){clearAndInit();}
    UiButton add(UiButton b){return addDrawableChild(b);}
    String state(){return CampaignClient.social.toString()+CampaignClient.progression.toString()+CampaignClient.profile.gold+CampaignClient.profile.unlocked+CampaignClient.profile.campaignCleared+CampaignClient.difficulty+running+ready;}
    @Override protected void init(){
        if(page.equals("home")){scale=1;key=state();code=null;if(home==null)home=new WarTableHome(this);home.init();return;} // the War Table draws in canvas pixels
        if(page.equals("recruit")){scale=1;key=state();code=null;if(recruit==null)recruit=new WarTableRecruit(this);recruit.init();return;}
        scale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/760f,client.getWindow().getScaledHeight()/460f));width=(int)(client.getWindow().getScaledWidth()/scale);height=(int)(client.getWindow().getScaledHeight()/scale);key=state();code=null;
        String[] names={"Camp guide","Recruitment","Friends","Private room","Contracts","Chapter rewards"};String[] ids={"home","recruit","friends","room","contracts","chapter"};
        for(int i=0;i<ids.length;i++){String id=ids[i];button((page.equals(id)?"> ":"")+names[i],18,94+i*34,150,b->page(id));}
        button("Army & equipment",18,310,150,b->open("army"));button("Visuals & sound",18,340,150,b->client.setScreen(new PresentationScreen()));button("Return to camp",18,height-42,150,b->close());
        int x=194,w=width-x-22;
        if(CampaignClient.result.has("id"))button("Last battle",18,370,150,b->page("results"));
        if(page.equals("friends")){
            if(!flag("party"))primary("CREATE PARTY",x,117,180,b->action("social:create"));else{
                button("Leave party",x,117,120,b->action("social:leave"));button(flag("partyLead")?"Disband party":"Claim offline lead",x+130,117,160,b->action(flag("partyLead")?"social:disband":"social:claim"));
                primary("CREATE PRIVATE ROOM",x+w-215,113,215,b->action("social:room"));
            }
            if(!value("inviter").isEmpty()){button("Accept invitation",x,166,155,b->action("social:accept"));button("Decline",x+166,166,100,b->action("social:decline"));}
            int i=0;for(var e:rows("members")){var row=e.getAsJsonObject();String name=row.get("name").getAsString();if(flag("partyLead")&&!row.get("me").getAsBoolean()){button("Make leader",x+w-198,217+i*27,105,b->action("social:transfer:"+name));button("Remove",x+w-83,217+i*27,83,b->action("social:kick:"+name));}i++;}
            i=0;var available=rows("available");for(int n=Math.min(offset,available.size());n<available.size()&&i<4;n++){var e=available.get(n);String name=e.getAsString();var invite=button("Invite "+name,x+(i%2)*(w/2),345+(i/2)*26,w/2-8,b->action("social:invite:"+name));invite.active=flag("partyLead");i++;}
            if(available.size()>4)button("More friends",x+w-120,396,120,b->{offset=offset+4<available.size()?offset+4:0;clearAndInit();});
        }else if(page.equals("room")){
            if(value("room").isEmpty()){
                primary("CREATE PRIVATE ROOM",x,144,w,b->action("social:room"));
                code=new TextFieldWidget(textRenderer,x,222,w-146,24,Text.literal("Room code"));code.setMaxLength(8);code.setText(roomInput);code.setChangedListener(v->roomInput=v);addDrawableChild(code);
                button("Join with party",x+w-136,224,136,b->action("social:join:"+roomInput.trim()));
            }else{
                button("Copy room code",x,115,145,b->client.keyboard.setClipboard(value("room")));button("Leave room",x+155,115,110,b->action("social:exit"));
                for(int t=0;t<2;t++){final int team=t;button(t==0?"Move party to Dawn":"Move party to Dusk",x+t*(w/2),155,w/2-8,b->action("social:team:"+team));}
                for(int i=1;i<=4;i++){final int size=i;var button=button(number("size",2)==i?"> "+i+"v"+i:i+"v"+i,x+(i-1)*60,310,54,b->action("social:size:"+size));button.active=flag("host")&&!flag("playing");}
                var fill=button("Bots: "+(flag("fill")?"ON":"OFF"),x+250,310,100,b->action("social:fill"));fill.active=flag("host")&&!flag("playing");
                var map=button(Spec.MAPS[Math.min(number("map",0),Spec.MAPS.length-1)].name()+" / next",x,341,w,b->action("social:map:"+((number("map",0)+1)%Spec.MAPS.length)));map.active=flag("host")&&!flag("playing");
                button("Toggle ready",x,height-78,w/2-8,b->action("social:ready"));var start=primary(flag("playing")?"MATCH IN PROGRESS":flag("preparing")?"PREPARING MAP":"START TOGETHER",x+w/2,height-82,w/2,b->action("social:start"));start.active=flag("host")&&!flag("playing")&&ready&&!running;
            }
        }else if(page.equals("contracts")){
            for(int i=0;i<3;i++){final int slot=i;button("Claim",x+w-90,124+i*65,90,b->action("progress:daily:"+slot));}
            button("Free third-contract reroll",x,317,230,b->action("progress:reroll"));button("Claim weekly",x+w-130,354,130,b->action("progress:weekly"));
        }else if(page.equals("chapter")){
            for(int i=0;i<10;i++){int tier=offset+i+1;if(tier>30)break;button("Claim tier "+tier,x+(i%2)*(w/2),121+(i/2)*45,w/2-8,b->action("progress:founders:"+tier));}
            button("Previous 10",x,359,120,b->{offset=Math.max(0,offset-10);clearAndInit();});button("Next 10",x+130,359,120,b->{offset=Math.min(20,offset+10);clearAndInit();});
            button("Cycle earned title",x+w-180,359,180,b->{var titles=CampaignClient.profile.progression.titles.toArray(String[]::new);int index=Arrays.asList(titles).indexOf(CampaignClient.profile.progression.title);if(titles.length>0)action("progress:title:"+titles[(index+1)%titles.length]);});
        }else if(page.equals("results")){
            button("Claim contracts",x,height-84,w/3-8,b->page("contracts"));button("Chapter rewards",x+w/3,height-84,w/3-8,b->page("chapter"));
            primary(value("room").isEmpty()?"BACK TO CAMP":"RETURN TO ROOM",x+2*w/3,height-88,w/3,b->page(value("room").isEmpty()?"home":"room"));
        }else if(page.equals("match")){
            for(int i=0;i<Spec.MAPS.length;i++){final int map=i;button("Vote: "+Spec.MAPS[i].name(),x,149+i*35,w,b->action("vote:"+map));}
            var bots=button(CampaignClient.botsLabel(),x,149+Spec.MAPS.length*35,w/2-7,b->action("difficulty:"+CampaignClient.nextDifficulty()));bots.active=!running; // from the old home until the field picker (UI Task 7)
            button("Play with friends in a room",x,350,w,b->page("room"));
        }
    }
    @Override public void tick(){if(home!=null&&page.equals("home"))home.tick();if(recruit!=null&&page.equals("recruit"))recruit.tick();if(!key.equals(state())&&!(code!=null&&code.isFocused()))clearAndInit();if(running&&!CampaignClient.waiting)close();}
    void line(DrawContext g,String text,int x,int y,int color,int max){g.drawText(textRenderer, textRenderer.trimToWidth(text,max),x,y,color,false);}
    /** The battle report table from y down: COMMANDER / KILLS / LOST / GOLD / ON POINTS, one row per army (your team first, a rule before the
     *  enemy, each row's team mark in the team column, your row in brass, a crest on the MVP), then one line per point with the seconds each
     *  team held it. Returns the y under it. */
    int table(DrawContext g,JsonObject report,int x,int y,int w){
        if(!report.has("squads")||!report.has("points"))return y;
        JsonArray rows=report.getAsJsonArray("squads");String[] heads={"KILLS","LOST","GOLD","ON POINTS"},keys={"kills","lost","gold","objective"};int[] right={x+w-200,x+w-140,x+w-70,x+w};int name=right[0]-x-74;
        line(g,"COMMANDER",x+25,y,DarkUi.MUTED,name-11);for(int c=0;c<4;c++)g.drawText(textRenderer,heads[c],right[c]-textRenderer.getWidth(heads[c]),y,DarkUi.MUTED,false);
        DarkUi.rule(g,x,y+11,w);y+=14;
        List<MatchStats.SquadStats> tally=new ArrayList<>();for(var e:rows){var s=new MatchStats.SquadStats();s.kills=e.getAsJsonObject().get("kills").getAsInt();s.objectiveSeconds=e.getAsJsonObject().get("objective").getAsInt();tally.add(s);}
        int mvp=MatchStats.mvp(tally);
        for(int i=0;i<rows.size();i++,y+=11){var r=rows.get(i).getAsJsonObject();int color=r.get("you").getAsBoolean()?DarkUi.GOLD:DarkUi.PAPER;
            if(i>0&&r.get("team").getAsInt()!=rows.get(i-1).getAsJsonObject().get("team").getAsInt()){DarkUi.rule(g,x+14,y,w-14);y+=3;}
            if(i==mvp)DarkUi.crest(g,x,y-1,10,r.get("team").getAsInt()==1);
            DarkUi.teamMark(g,r.get("team").getAsInt(),x+14,y);line(g,r.get("name").getAsString()+(i==mvp?"  MVP":""),x+25,y,color,name-11);
            for(int c=0;c<4;c++){String v=r.get(keys[c]).getAsInt()+(c==3?"s":"");g.drawText(textRenderer,v,right[c]-textRenderer.getWidth(v),y,color,false);}
        }
        y+=3;
        for(var e:report.getAsJsonArray("points")){var p=e.getAsJsonObject();line(g,p.get("name").getAsString()+"  Dawn "+p.get("dawn").getAsInt()+"s / Dusk "+p.get("dusk").getAsInt()+"s",x,y,DarkUi.MUTED,w);y+=11;}
        return y;
    }
    @Override public void render(DrawContext g,int mx,int my,float delta){
        if(page.equals("home")&&home!=null){home.render(g);super.render(g,mx,my,delta);return;}
        if(page.equals("recruit")&&recruit!=null){recruit.render(g);super.render(g,mx,my,delta);recruit.overlay(g);return;} // the Odds note lies over the widgets
        g.getMatrices().push();g.getMatrices().scale(scale,scale,1);DarkUi.background(g,width,height);DarkUi.crest(g,20,16,26,false);DarkUi.title(g,"BANNER CAMP",58,19,DarkUi.GOLD);
        line(g,CampaignClient.profile.progression.title+"  /  "+CampaignClient.profile.gold+" gold",width-280,24,DarkUi.PAPER,260);
        int x=194,w=width-x-22;DarkUi.frame(g,x-12,79,w+24,height-139);String title=switch(page){case "friends"->"YOUR FRIEND GROUP";case "room"->"PRIVATE WAR ROOM";case "contracts"->"CONTRACT BOARD";case "chapter"->"FOUNDERS CHAPTER";case "match"->"WAR GATES";case "results"->"BATTLE REPORT";default->"A PLACE TO PREPARE";};
        line(g,title,x,91,DarkUi.GOLD,w);
        var p=CampaignClient.profile;var progress=p.progression;
        if(page.equals("friends")){
            line(g,flag("party")?"Leader: "+value("leader")+" / party survives matches and server restarts":"Parties hold up to four friends and always enter one team.",x,104,DarkUi.MUTED,w);
            if(!value("inviter").isEmpty())line(g,"Invitation from "+value("inviter")+" ("+number("inviteSeconds",0)+"s)",x,149,DarkUi.GOLD,w);
            int i=0;for(var e:rows("members")){var r=e.getAsJsonObject();line(g,r.get("name").getAsString()+" / "+(r.get("online").getAsBoolean()?"online":"offline"),x,223+i++*27,DarkUi.PAPER,w-215);}
            line(g,"INVITE SOMEONE IN THIS CAMP",x,325,DarkUi.GOLD,w);
            if(rows("available").isEmpty())line(g,"Friends appear here after joining this game server.",x,353,DarkUi.MUTED,w);
        }else if(page.equals("room")){
            if(value("room").isEmpty()){
                line(g,"Create a room, or enter the private code a friend shared.",x,119,DarkUi.PAPER,w);line(g,"ROOM CODE",x,202,DarkUi.GOLD,w);
                line(g,"Your whole party joins one team. No silent splitting.",x,281,DarkUi.MUTED,w);line(g,"Each commander readies. The host chooses map, mode and bots.",x,306,DarkUi.MUTED,w);
            }else{line(g,"CODE "+value("room")+"  /  "+(flag("host")?"You host":"Member"),x+w-225,119,DarkUi.GOLD,225);
                int[] count={0,0};for(var e:rows("commanders")){var r=e.getAsJsonObject();int team=r.get("team").getAsInt();line(g,r.get("name").getAsString()+" / "+(!r.get("online").getAsBoolean()?"reconnecting":r.get("ready").getAsBoolean()?"READY":"not ready"),x+team*(w/2),193+count[team]++*24,DarkUi.PAPER,w/2-10);}}
        }else if(page.equals("contracts")){
            for(int i=0;i<3;i++){int y=128+i*65;line(g,Progression.contract(progress,i),x,y,DarkUi.PAPER,w-110);line(g,Math.min(Progression.progress(progress,i),Progression.target(progress,i))+" / "+Progression.target(progress,i)+"  |  "+(i==1?40:30)+" gold + 75 XP"+(progress.claims.contains("daily:"+i)?" / CLAIMED":""),x,y+21,DarkUi.MUTED,w-110);}
            line(g,"Weekly: "+progress.weeklyBattles+" / "+Progression.WEEKLY_BATTLES+" battles / 150 gold + 300 XP",x,358,DarkUi.PAPER,w-140);line(g,"Daily reset 00:00 UTC / weekly reset Monday 00:00 UTC",x,386,DarkUi.MUTED,w);
        }else if(page.equals("chapter")){
            line(g,"Tier "+Progression.tier(progress)+" / 30  |  "+Math.min(progress.xp,7500)+" / 7500 XP  |  Free; no expiry",x,108,DarkUi.PAPER,w);
            for(int i=0;i<10;i++){int tier=offset+i+1;String reward=tier%5==0?Progression.TITLES[tier/5-1]+" title":"25 gold";line(g,reward+(progress.claims.contains("founders:"+tier)?" / claimed":Progression.tier(progress)<tier?" / locked":" / earned"),x+(i%2)*(w/2)+4,146+(i/2)*45,DarkUi.MUTED,w/2-16);}
            line(g,"Finish the chapter; keep every unit and title when new chapters arrive.",x,397,DarkUi.MUTED,w);
        }else if(page.equals("results")){
            var report=CampaignClient.result;
            if(report.has("id")){
                line(g,report.get("outcome").getAsString()+" / "+report.get("map").getAsString(),x,117,DarkUi.GOLD,w);
                int y=Math.max(table(g,report,x,132,w)+5,height-163); // the battle table first; the rewards sit above the buttons, or right under a long table
                boolean saved=report.get("saved").getAsBoolean();
                line(g,saved?"+"+report.get("gold").getAsInt()+" GOLD     +"+report.get("xp").getAsInt()+" CHAPTER XP":"REWARDS NOT SAVED / Progress unchanged",x,y,saved?DarkUi.PAPER:DarkUi.ALARM_TEXT,w);
                line(g,!saved?"Check disk space before playing again.":report.get("eligible").getAsBoolean()?"Rewards saved. Contract and tier rewards are claimed separately.":"No match rewards: play at least 3 minutes to qualify.",x,y+11,DarkUi.MUTED,w);
                y+=25;int tier=Progression.tier(progress),xp=Math.min(progress.xp,7500),half=w/2-8,right=x+w-half; // chapter progress on the left, contracts on the right
                line(g,"FOUNDERS / Tier "+tier+" of 30 / "+xp+" XP",x,y,DarkUi.GOLD,half);
                g.fill(x,y+11,x+half,y+15,DarkUi.TRACK);g.fill(x,y+11,x+(int)(half*(tier==30?1:(xp%250)/250f)),y+15,DarkUi.GOLD);
                line(g,tier==30?"Chapter complete. Troops and titles stay yours.":"Next: tier "+(tier+1)+" / "+((tier+1)%5==0?Progression.TITLES[(tier+1)/5-1]+" title":"25 gold")+" / "+(250-xp%250)+" XP to go",x,y+19,DarkUi.PAPER,half);
                for(int i=0;i<3;i++)line(g,Progression.contract(progress,i)+"  "+Math.min(Progression.progress(progress,i),Progression.target(progress,i))+"/"+Progression.target(progress,i)+(progress.claims.contains("daily:"+i)?" / claimed":Progression.progress(progress,i)>=Progression.target(progress,i)?" / READY TO CLAIM":""),right,y+i*11,DarkUi.MUTED,half);
                line(g,"Weekly battles: "+Math.min(progress.weeklyBattles,Progression.WEEKLY_BATTLES)+"/"+Progression.WEEKLY_BATTLES+(progress.claims.contains("weekly")?" / claimed":Progression.claimable(progress,"weekly")?" / READY TO CLAIM":""),right,y+33,DarkUi.MUTED,half);
            }
        }else if(page.equals("match")){
            line(g,"Dawn "+CampaignClient.padDawn+" / 4   |   Dusk "+CampaignClient.padDusk+" / 4   |   "+CampaignClient.padCountdown+"s",x,116,DarkUi.GOLD,w);
            line(g,"Vote while on a ready pad. Most votes wins; ties use map order.",x,133,DarkUi.MUTED,w);
        }
        line(g,HintText.expand(CampaignClient.notice,GravesAndGloryClient::hint),x,height-42,DarkUi.GOLD,w);super.render(g,(int)(mx/scale),(int)(my/scale),delta);
        g.getMatrices().pop();
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(page.equals("recruit")&&recruit!=null&&recruit.clickCloses())return true;return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseScrolled(double x,double y,double amount){if(page.equals("recruit")&&recruit!=null)return recruit.scrolled(amount);return super.mouseScrolled(x/scale,y/scale,amount);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(x/scale,y/scale,button,dx/scale,dy/scale);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(held.contains(key))return true;if(page.equals("home")&&home!=null&&home.keyPressed(key))return true;if(page.equals("recruit")&&recruit!=null&&recruit.keyPressed(key)){held.add(key);return true;}if(code!=null&&code.isFocused()&&key!=256)return code.keyPressed(key,scan,mods);if(key==77)return true;return super.keyPressed(key,scan,mods);}
    /** A held key's release lets its presses through again (see held). */
    @Override public boolean keyReleased(int key,int scan,int mods){if(held.remove(key))return true;return super.keyReleased(key,scan,mods);}
    @Override public void mouseMoved(double x,double y){if(page.equals("home")&&home!=null)home.mouseMoved(x,y);super.mouseMoved(x/scale,y/scale);}
    @Override public void setFocused(net.minecraft.client.gui.Element e){super.setFocused(e);if(page.equals("home")&&home!=null)home.focusChanged(e);}
}

