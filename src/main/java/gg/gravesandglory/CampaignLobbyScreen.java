package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.*;
import static gg.gravesandglory.GravesAndGloryClient.*;

/** A responsive war council over the physical lobby, using the actual roster. */
public class CampaignLobbyScreen extends GravesAndGloryClient.ArmyScreen {
    float uiScale=1;
    String selectedUnit="knight";final List<ArmyCards.Deployment> slots=new ArrayList<>();
    int tab=0;String key="";List<ButtonWidget> leadControls=new ArrayList<>();ButtonWidget start;
    /** The House whose owned troops the banner editor shows; "" until the tab opens, then the largest House in the banner. */
    String house="";
    public CampaignLobbyScreen(){super("Graves and Glory war council");}
    /** Shows a House's troops in the banner editor. A picked troop of another House is let go, so a slot click never places a troop the tab does not show; the clear tool stays. */
    void showHouse(String id){house=id;var picked=ArmyCards.spec(selectedUnit);if(picked!=null&&!picked.house().equals(id))selectedUnit="";}
    /** The gold line under the editor's troop cards: the picked troop, the clear tool, or no pick. */
    String pickLine(){
        if(selectedUnit.isEmpty())return "No troop picked.";
        var chosen=ArmyCards.spec(selectedUnit);
        return chosen==null?"Clear: pick a slot to empty it.":chosen.name()+" / "+ArmyCards.rarityName(chosen)+(chosen.slotWeight()>1?" / fills "+chosen.slotWeight()+" slots":"");
    }
    String stateKey(){return CampaignClient.mode+CampaignClient.map+CampaignClient.fill+CampaignClient.difficulty+CampaignClient.partyKey+team+CampaignClient.profile.owned.size()+CampaignClient.headline;}
    public void showTab(int value){tab=value;clearAndInit();}
    @Override protected void init(){
        uiScale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/640f,client.getWindow().getScaledHeight()/360f));width=(int)(client.getWindow().getScaledWidth()/uiScale);height=(int)(client.getWindow().getScaledHeight()/uiScale);
        start=null;slots.clear();leadControls.clear();key=stateKey();int cw=(width-36)/3;
        String[] names={"Battle setup","Your banner","Custom lobby"};
        for(int i=0;i<3;i++){final int t=i;button((tab==i?"> ":"")+names[i]+(i==2&&invited()?" *":""),16+i*(cw+4),43,cw,b->{if(t==0)client.setScreen(new CampScreen("home"));else if(t==2)client.setScreen(new CampScreen("friends"));else{tab=t;clearAndInit();}});}
        button("Visuals & sound",width-156,17,140,b->client.setScreen(new PresentationScreen(this)));
        if(tab==0){
            int lw=(width-52)*2/3,rx=28+lw,rw=width-rx-16;
            String[] modes={"ai","1","2","3","4"};int bw=lw/5;
            for(int i=0;i<5;i++){String id=modes[i];leadControls.add(button((id.equals(CampaignClient.mode)?"> ":"")+(id.equals("ai")?"Practice":id+"v"+id),16+i*bw,81,bw-4,b->send("mode:"+id)));}
            int mw=lw/Spec.MAPS.length;for(int i=0;i<Spec.MAPS.length;i++){final int id=i;leadControls.add(button((CampaignClient.map==i?"> ":"")+Spec.MAPS[i].name(),16+i*mw,height-104,mw-4,b->send("map:"+id)));}
            leadControls.add(button("Bot fill: "+(CampaignClient.fill?"ON":"OFF"),rx,height-104,rw/2-2,b->send("fill")));
            leadControls.add(button(CampaignClient.botsLabel(),rx+rw/2+2,height-104,rw-rw/2-2,b->send("difficulty:"+CampaignClient.nextDifficulty())));
        }else if(tab==1){
            // The banner editor (spec 6.2): pick an owned troop, then a slot. Troops of a House that cannot be fielded yet stay inactive.
            int left=width/2+8,rw=width-left-16,cardWidth=(rw-8)/3,row=0;
            if(house.isEmpty())showHouse(RosterMath.rollHouse(CampaignClient.profile.banner));
            // House filter: each button as wide as its name needs (the label plus the button's 14 px padding), shared out over the row so none is cut.
            int need=0;for(var h:Spec.HOUSES)need+=textRenderer.getWidth(DarkUi.body(h.name().split(" ")[0]))+17;
            int hx=left;for(var h:Spec.HOUSES){String id=h.id(),label=h.name().split(" ")[0];int hw=row==Spec.HOUSES.length-1?left+rw-hx:(int)Math.round((textRenderer.getWidth(DarkUi.body(label))+17)*(double)rw/Math.max(1,need));
                button((house.equals(id)?"> ":"")+label,hx,74,hw-3,b->{showHouse(id);clearAndInit();});hx+=hw;row++;}
            row=0;
            for(var unit:Spec.TROOPS)if(unit.house().equals(house)&&CampaignClient.profile.owned.containsKey(unit.id())){String id=unit.id();
                var choose=addDrawableChild(new ArmyCards.Recruit(unit,left+(row%3)*(cardWidth+4),97+(row/3)*84,cardWidth,b->{selectedUnit=id;clearAndInit();}));choose.active=ArmyCards.fieldable(unit);row++;
            }
            for(int slot=0;slot<20;slot++){final int index=slot;slots.add(addDrawableChild(new ArmyCards.Deployment(index,24+(slot%5)*54,111+(slot/5)*36,b->{if(!selectedUnit.isEmpty())send("slot:"+index+":"+selectedUnit);})));}
            button((selectedUnit.equals(RosterMath.EMPTY)?"> ":"")+"Clear a slot",20,276,120,b->{selectedUnit=RosterMath.EMPTY;clearAndInit();});
            for(int i=0;i<Spec.FACTIONS.length;i++){var side=Spec.FACTIONS[i];addDrawableChild(new ArmyCards.Colour(side,146+i*38,276,b->send("colour:"+side.id())));}
        }else{
            partyTab();
        }

    }
    /** An invitation waits for this player: they are not in the lobby yet and the lead's invite has not expired. */
    boolean invited(){JsonObject party=CampaignClient.party;return !party.get("member").getAsBoolean()&&party.get("inviteSeconds").getAsInt()>0;}
    /** Custom lobby: open one, invite commanders who are online here, leave, or answer an invitation. The lead starts the match. */
    void partyTab(){
        JsonObject party=CampaignClient.party;boolean open=party.get("open").getAsBoolean(),member=party.get("member").getAsBoolean();
        int rx=24,rw=width-48,half=(rw-8)/2,col=(rw-12)/3;
        if(!open)button("Open custom lobby",rx,109,220,b->send("party:open"));
        else if(member)button("Leave lobby",rx,109,220,b->send("party:leave"));
        else if(invited()){button("Accept",rx,109,half,b->send("party:accept"));button("Decline",rx+half+8,109,half,b->send("party:decline"));}
        if(CampaignClient.lead){
            int shown=0;
            for(var element:party.getAsJsonArray("online")){
                if(shown==6)break;
                final String name=element.getAsString();
                button("Invite "+name,rx+(shown%3)*(col+6),278+(shown/3)*24,col,b->send("party:invite:"+name));shown++;
            }
        }
        start=primary(CampaignClient.headline.isEmpty()?"ENTER BATTLE":"PLAY AGAIN",width-195,height-33,177,b->send("start"));
    }
    @Override public boolean mouseClicked(double x,double y,int button){return super.mouseClicked(x/uiScale,y/uiScale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/uiScale,y/uiScale,button);}
    @Override public void tick(){
        if(!stateKey().equals(key))clearAndInit();
        for(var b:leadControls)b.active=CampaignClient.lead&&ready&&!running;
        if(start!=null)start.active=CampaignClient.lead&&ready&&!running;
        if(tab==1){var line=CampaignClient.profile.banner;for(int i=0;i<slots.size();i++)slots.get(i).setUnit(i<line.size()?line.get(i):RosterMath.EMPTY);}
        if(running&&!CampaignClient.waiting)close();
    }
    /** Custom lobby text: the lead, the members, and any invitation waiting for this player. */
    void partyText(DrawContext g){
        JsonObject party=CampaignClient.party;boolean open=party.get("open").getAsBoolean(),member=party.get("member").getAsBoolean(),lead=CampaignClient.lead;
        int rx=24,rw=width-48;String leader=party.get("leader").getAsString();
        if(invited())text(g,"Invitation from "+party.get("inviter").getAsString()+". Accept or decline below.",rx,137,rw,DarkUi.GOLD);
        else if(!open)text(g,"No custom lobby is open. Open one to invite commanders who are online here.",rx,137,rw,DarkUi.MUTED);
        else if(!member)text(g,"Lead: "+leader+". Ask them for an invitation.",rx,137,rw,DarkUi.MUTED);
        else text(g,"Lead: "+leader+"    Commanders: "+party.getAsJsonArray("members").size()+" of "+party.get("capacity").getAsInt(),rx,137,rw,DarkUi.GOLD);
        int i=0;
        for(var element:party.getAsJsonArray("members")){String name=element.getAsString();boolean isLead=name.equals(leader);
            text(g,name+(isLead?"  (lead)":""),rx+(i/6)*(rw/4),155+(i%6)*12,rw/4-8,isLead?DarkUi.GOLD:DarkUi.IVORY);i++;}
        if(lead)text(g,"INVITE ONLINE COMMANDERS",rx,262,rw,DarkUi.MUTED);
        else if(member)text(g,"Only the lead can invite commanders or start the battle.",rx,262,rw,DarkUi.MUTED);
        text(g,lead?"Invite commanders, then start the battle when you are ready.":member?"Waiting for the lead to start.":"Open the war council at the war table, or wait for an invitation.",18,height-23,width-230,DarkUi.MUTED);
    }
    void text(DrawContext g,String value,int x,int y,int max,int color){g.drawText(textRenderer,net.minecraft.util.Language.getInstance().reorder(textRenderer.trimToWidth(DarkUi.body(value),max)),x,y,color,false);}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        g.getMatrices().push();g.getMatrices().scale(uiScale,uiScale,1);mx=(int)(mx/uiScale);my=(int)(my/uiScale);
        DarkUi.background(g,width,height);
        DarkUi.title(g,new String[]{"BATTLE SETUP","YOUR BANNER","CUSTOM LOBBY"}[tab],18,10,DarkUi.IVORY);text(g,"GOLD  "+CampaignClient.profile.gold,width-270,21,110,DarkUi.GOLD);
        boolean hasResult=!CampaignClient.headline.isEmpty();
        text(g,hasResult?CampaignClient.headline:ready?"Choose your army. Raise your banner.":HintText.expand(message,GravesAndGloryClient::hint),18,30,width-185,hasResult?DarkUi.GOLD:DarkUi.MUTED);
        if(tab==0){
            int lw=(width-52)*2/3,rx=28+lw,rw=width-rx-16,previewH=height-225;
            panel(g,16,109,lw,previewH);
            // The tactical preview is built from this battlefield's real terrain.
            var preview=new net.minecraft.util.Identifier("gravesandglory","textures/ui/map-"+CampaignClient.map+".png");
            if(client.getResourceManager().getResource(preview).isPresent())g.drawTexture(preview,17,110,lw-2,previewH-2,0f,130f,1280,460,1280,720);
            else map(g,17,110,lw-2,previewH-2,false);
            g.fill(17,110,15+lw,140,DarkUi.PREVIEW_BAND);
            DarkUi.title(g,Spec.MAPS[CampaignClient.map].name(),26,118,DarkUi.IVORY);
            panel(g,rx-8,76,rw+16,height-121);
            text(g,"COMMANDERS",rx,85,rw,DarkUi.IVORY);int y=144;
            for(var element:CampaignClient.players){var p=element.getAsJsonObject();int faction=p.get("team").getAsInt();
                DarkUi.crest(g,rx,y-2,12,faction==1);DarkUi.teamMark(g,faction,rx+16,y);text(g,p.get("name").getAsString(),rx+27,y,rw-27,DarkUi.teamColor(faction));y+=13;}
            text(g,"700 x 500 blocks / "+CampaignMaps.objectiveCount(CampaignClient.map)+" objectives",24,height-68,lw-16,DarkUi.MUTED);
            int total=CampaignClient.mode.equals("ai")?2:Integer.parseInt(CampaignClient.mode)*2;
            text(g,total+" commander slots",rx,height-72,rw,DarkUi.MUTED);
            String hint=CampaignClient.lead?(hasResult?"Choose the battlefield, then open a custom lobby to play again.":"Choose your battlefield, then start from the Custom lobby tab."):(hasResult?"Open the war council at the war table to play again.":"Only the custom lobby lead chooses the battlefield. Open the war council at the war table to play in a room.");
            text(g,hint,18,height-23,width-230,DarkUi.MUTED);
        }else if(tab==1){
            var catalog=RosterMath.Catalog.fromSpec();var line=CampaignClient.profile.banner;int used=(int)line.stream().filter(id->!id.equals(RosterMath.EMPTY)).count();
            text(g,"BANNER / "+RosterMath.units(line,catalog)+" UNITS IN "+used+" OF "+line.size()+" SLOTS",16,74,width/2-28,DarkUi.GOLD);
            DarkUi.frame(g,15,88,285,182);text(g,"FRONT / toward the enemy",24,94,270,DarkUi.MUTED);text(g,"REAR",24,260,270,DarkUi.MUTED);
            int left=width/2+8;var row=Arrays.stream(Spec.HOUSES).filter(h->h.id().equals(house)).findFirst().orElse(null);
            if(row!=null&&Arrays.stream(Spec.TROOPS).noneMatch(t->t.house().equals(house)&&CampaignClient.profile.owned.containsKey(t.id())))text(g,"No "+row.name()+" troops in your collection yet.",left,104,width-left-16,DarkUi.MUTED);
            text(g,pickLine(),left,266,width-left-16,DarkUi.GOLD);
            text(g,row!=null&&!row.fieldable()?row.name()+" has not joined the war: its troops cannot be fielded yet.":"Pick a troop, then a slot. Epics fill 2 slots, Legendaries 4.",left,280,width-left-16,DarkUi.MUTED);
            text(g,"New troops join by recruitment rolls at the camp shrine, "+Spec.RECRUITMENT[0].rollCost()+" gold each. The four marks set your banner colour.",16,height-27,width-32,DarkUi.MUTED);
        }else{
            partyText(g);
        }

        if(!CampaignClient.notice.isEmpty())text(g,HintText.expand(CampaignClient.notice,GravesAndGloryClient::hint),18,tab==1?height-12:69,width-36,DarkUi.GOLD);super.render(g,mx,my,delta);
        if(System.currentTimeMillis()<CampaignClient.revealUntil){int cx=width/2,cy=height/2;panel(g,cx-160,cy-45,320,90);g.drawBorder(cx-160,cy-45,320,90,DarkUi.GOLD);g.drawCenteredTextWithShadow(textRenderer,"A NEW SOLDIER JOINS YOUR BANNER",cx,cy-28,DarkUi.GOLD);long remain=CampaignClient.revealUntil-System.currentTimeMillis();String name=remain>1300?Spec.TROOPS[(int)(System.currentTimeMillis()/75%Spec.TROOPS.length)].name():CampaignClient.revealUnit;g.drawCenteredTextWithShadow(textRenderer,name,cx,cy-2,DarkUi.IVORY);g.drawCenteredTextWithShadow(textRenderer,remain>1300?"Opening recruitment seal...":"PERMANENTLY UNLOCKED",cx,cy+24,DarkUi.GOLD);}
        g.getMatrices().pop();
    }
}

