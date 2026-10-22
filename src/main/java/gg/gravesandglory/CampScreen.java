package gg.gravesandglory;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.IntPredicate;
import static gg.gravesandglory.GravesAndGloryClient.*;

/** The War Table: home, the recruit page and WarTablePages' banner, contracts, chapter, friends and field, all in canvas pixels;
 *  the battle report (results) keeps the old scaled layout until its own task. */
final class CampScreen extends ArmyScreen {
    String page;float scale=1;int offset;String key="";long requestAt;
    /** The War Table home (page home), kept while this screen lives so its focus and animation survive a rebuild. */
    WarTableHome home;
    /** The War Table recruit page (page recruit), kept while the page is open; leaving the page drops it, so a later visit opens on the first House. */
    WarTableRecruit recruit;
    /** The War Table's other pages (UI plan Task 7), kept while the page is open, so the armed troop, the track's place and a room code being written survive a rebuild; leaving the page drops it. */
    WarTablePages pages;
    /** The keys whose press a War Table page took, not released since, and the watched keys already down when the screen opened (the key that opened it, say). Minecraft hands a screen each repeat of a held key as another keyPressed: those go no further, on whatever page the press left the table at (a held Esc that went home must not then close it, a held Enter must not ride again, the menu key that opened the table must not close it), until keyReleased lets the key through again. */
    final Set<Integer> held=new HashSet<>();
    /** Whether a key is down now, as GLFW has it (the checks stand in for it). */
    static IntPredicate keyDown=k->InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(),k);
    CampScreen(String page){super("Banner camp");this.page=alias(page);for(int k:watched())if(keyDown.test(k))held.add(k);}
    /** The keys the War Table acts on (its pages' keys, Tab, the arrows) and the menu key, which closes it. */
    static List<Integer> watched(){
        List<Integer> keys=new ArrayList<>(List.of(GLFW.GLFW_KEY_ESCAPE,GLFW.GLFW_KEY_ENTER,GLFW.GLFW_KEY_KP_ENTER,GLFW.GLFW_KEY_SPACE,GLFW.GLFW_KEY_TAB,GLFW.GLFW_KEY_B,GLFW.GLFW_KEY_LEFT,GLFW.GLFW_KEY_RIGHT,GLFW.GLFW_KEY_UP,GLFW.GLFW_KEY_DOWN));
        if(menu!=null){var bound=InputUtil.fromTranslationKey(menu.getBoundKeyTranslationKey());if(bound.getCategory()==InputUtil.Type.KEYSYM&&bound.getCode()>=0&&!keys.contains(bound.getCode()))keys.add(bound.getCode());}
        return keys;
    }
    /** The page a name opens: the old names still sent (the war gates' match is the field, the private room is on Friends, the Quartermaster's army is the Banner), else the name. */
    static String alias(String page){return switch(page){case "match"->"field";case "room"->"friends";case "army"->"banner";default->page;};}
    static void open(String page){MinecraftClient.getInstance().setScreen(new CampScreen(page));}
    JsonObject social(){return CampaignClient.social;}
    String value(String name){return social().has(name)?social().get(name).getAsString():"";}
    /** Sends cmd to the server unless a command went less than 300 ms ago; returns whether it went. */
    boolean action(String cmd){if(System.currentTimeMillis()-requestAt<300)return false;requestAt=System.currentTimeMillis();send(cmd);return true;}
    /** Turns the table to another page (an old name goes through alias); the keyboard's focus starts afresh there. */
    void page(String next){page=alias(next);offset=0;if(!page.equals("recruit"))recruit=null;if(pages!=null&&!pages.page.equals(page))pages=null;super.clearAndInit();}
    void rebuild(){clearAndInit();}
    UiButton add(UiButton b){return addDrawableChild(b);}
    String state(){return CampaignClient.social.toString()+CampaignClient.progression.toString()+CampaignClient.profile.gold+CampaignClient.profile.unlocked+CampaignClient.profile.campaignCleared+CampaignClient.difficulty+running+ready;}
    /** The widgets of the War Table page shown, by name: home's, the recruit page's or WarTablePages'; none on the report. */
    Map<String,UiButton> widgets(){
        if(page.equals("home")&&home!=null)return home.buttons;
        if(page.equals("recruit")&&recruit!=null)return recruit.buttons;
        if(pages!=null&&pages.page.equals(page))return pages.buttons;
        return Map.of();
    }
    /** A rebuild (the camp state or a badge changed, the window was resized) makes every widget anew; the keyboard's focus goes back to the widget of the same name, so Tab keeps its place. */
    @Override protected void clearAndInit(){
        String name=null;var focused=getFocused();
        for(var e:widgets().entrySet())if(focused!=null&&e.getValue()==focused)name=e.getKey();
        super.clearAndInit();
        var again=name==null?null:widgets().get(name);
        if(again!=null&&again.visible&&again.active)super.setFocused(again); // as it was: no sheet rises and Ride does not glint again
    }
    @Override protected void init(){
        if(page.equals("home")){scale=1;key=state();if(home==null)home=new WarTableHome(this);home.init();return;} // the War Table draws in canvas pixels
        if(page.equals("recruit")){scale=1;key=state();if(recruit==null)recruit=new WarTableRecruit(this);recruit.init();return;}
        if(WarTablePages.NAMES.contains(page)){scale=1;key=state();if(pages==null||!pages.page.equals(page))pages=new WarTablePages(this,page);pages.init();return;}
        scale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/760f,client.getWindow().getScaledHeight()/460f));width=(int)(client.getWindow().getScaledWidth()/scale);height=(int)(client.getWindow().getScaledHeight()/scale);key=state();
        String[] names={"Camp guide","Recruitment","Friends","Contracts","Chapter rewards"};String[] ids={"home","recruit","friends","contracts","chapter"};
        for(int i=0;i<ids.length;i++){String id=ids[i];button(names[i],18,94+i*34,150,b->page(id));}
        button("Army & equipment",18,310,150,b->page("banner"));button("Visuals & sound",18,340,150,b->client.setScreen(new PresentationScreen()));button("Return to camp",18,height-42,150,b->close());
        int x=194,w=width-x-22;
        if(CampaignClient.result.has("id"))button("Last battle",18,370,150,b->page("results"));
        if(page.equals("results")){
            button("Claim contracts",x,height-84,w/3-8,b->page("contracts"));button("Chapter rewards",x+w/3,height-84,w/3-8,b->page("chapter"));
            primary(value("room").isEmpty()?"BACK TO CAMP":"RETURN TO ROOM",x+2*w/3,height-88,w/3,b->page(value("room").isEmpty()?"home":"friends"));
        }
    }
    @Override public void tick(){if(home!=null&&page.equals("home"))home.tick();if(recruit!=null&&page.equals("recruit"))recruit.tick();if(pages!=null&&pages.page.equals(page))pages.tick();if(!key.equals(state()))clearAndInit();if(running&&!CampaignClient.waiting)close();}
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
        if(pages!=null&&pages.page.equals(page)){pages.render(g);super.render(g,mx,my,delta);return;}
        g.getMatrices().push();g.getMatrices().scale(scale,scale,1);DarkUi.background(g,width,height);DarkUi.crest(g,20,16,26,false);DarkUi.title(g,"BANNER CAMP",58,19,DarkUi.GOLD);
        line(g,CampaignClient.profile.progression.title+"  /  "+CampaignClient.profile.gold+" gold",width-280,24,DarkUi.PAPER,260);
        int x=194,w=width-x-22;DarkUi.frame(g,x-12,79,w+24,height-139);line(g,page.equals("results")?"BATTLE REPORT":"A PLACE TO PREPARE",x,91,DarkUi.GOLD,w);
        var progress=CampaignClient.profile.progression;
        if(page.equals("results")){
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
        }
        line(g,HintText.expand(CampaignClient.notice,GravesAndGloryClient::hint),x,height-42,DarkUi.GOLD,w);super.render(g,(int)(mx/scale),(int)(my/scale),delta);
        g.getMatrices().pop();
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(page.equals("recruit")&&recruit!=null&&recruit.clickCloses())return true;if(pages!=null&&pages.page.equals(page)){if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT&&pages.secondaryClick(x,y))return true;pages.clicked(x,y);}return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseScrolled(double x,double y,double amount){if(page.equals("recruit")&&recruit!=null)return recruit.scrolled(amount);return super.mouseScrolled(x/scale,y/scale,amount);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(x/scale,y/scale,button,dx/scale,dy/scale);}
    /** A key's press: the room code being written takes its keys first (so Backspace repeats as it is held); then a held key's repeats stop here (see held); then the page shown, whose keys count as held until released. */
    @Override public boolean keyPressed(int key,int scan,int mods){boolean onPages=pages!=null&&pages.page.equals(page);if(onPages&&pages.typing(key))return true;if(held.contains(key))return true;if(page.equals("home")&&home!=null&&home.keyPressed(key)){held.add(key);return true;}if(page.equals("recruit")&&recruit!=null&&recruit.keyPressed(key)){held.add(key);return true;}if(onPages&&pages.keyPressed(key)){held.add(key);return true;}if(key==77)return true;return super.keyPressed(key,scan,mods);}
    @Override public boolean charTyped(char ch,int mods){if(pages!=null&&pages.page.equals(page)&&pages.charTyped(ch))return true;return super.charTyped(ch,mods);}
    /** A held key's release lets its presses through again (see held). */
    @Override public boolean keyReleased(int key,int scan,int mods){if(held.remove(key))return true;return super.keyReleased(key,scan,mods);}
    @Override public void mouseMoved(double x,double y){if(page.equals("home")&&home!=null)home.mouseMoved(x,y);super.mouseMoved(x/scale,y/scale);}
    @Override public void setFocused(net.minecraft.client.gui.Element e){super.setFocused(e);if(page.equals("home")&&home!=null)home.focusChanged(e);}
}
