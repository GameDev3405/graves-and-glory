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
    String pendingClass="";int tab=0;String key="";List<ButtonWidget> leadControls=new ArrayList<>();ButtonWidget start,confirmClass;
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
    String stateKey(){return CampaignClient.mode+CampaignClient.map+CampaignClient.fill+CampaignClient.difficulty+CampaignClient.partyKey+team+CampaignClient.profile.commander+CampaignClient.profile.owned.size()+CampaignClient.headline;}
    public void showTab(int value){tab=value;clearAndInit();}
    @Override protected void init(){
        uiScale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/640f,client.getWindow().getScaledHeight()/360f));width=(int)(client.getWindow().getScaledWidth()/uiScale);height=(int)(client.getWindow().getScaledHeight()/uiScale);
        start=null;slots.clear();leadControls.clear();key=stateKey();int cw=(width-40)/4;
        String[] names={"Battle setup","Your banner","Commander class","Custom lobby"};
        for(int i=0;i<4;i++){final int t=i;button((tab==i?"> ":"")+names[i]+(i==3&&invited()?" *":""),16+i*(cw+4),43,cw,b->{if(t==0)client.setScreen(new CampScreen("home"));else if(t==3)client.setScreen(new CampScreen("friends"));else{tab=t;clearAndInit();}});}
        button("Visuals & sound",width-156,17,140,b->client.setScreen(new PresentationScreen()));
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
        }else if(tab==2){
            // One row of cards per track (HERO, GENERAL), each class a card: click to pick it, CONFIRM CLASS to lead as it.
            if(pendingClass.isEmpty())pendingClass=CampaignClient.profile.commander;
            var tracks=classTracks();int perRow=tracks.stream().mapToInt(List::size).max().orElse(1),cardW=(width-32-8*(perRow-1))/perRow,ph=classPanelHeight();
            for(int r=0;r<tracks.size();r++){var row=tracks.get(r);for(int i=0;i<row.size();i++)addDrawableChild(new ClassCard(row.get(i),16+i*(cardW+8),classRowTop(r)+12,cardW,ph));}
            confirmClass=primary("CONFIRM CLASS",width-195,height-33,177,b->{send("class:"+pendingClass);});
        }else{
            partyTab();
        }

    }
    /** The classes by track (hero, then general, in sheet order), one row of cards each. */
    static List<List<Spec.ClassesRow>> classTracks(){Map<String,List<Spec.ClassesRow>> out=new LinkedHashMap<>();for(var c:Spec.CLASSES)out.computeIfAbsent(c.track(),k->new ArrayList<>()).add(c);return new ArrayList<>(out.values());}
    /** The y of a track's heading; its cards stand 12 below it. */
    int classRowTop(int row){return 81+row*(classPanelHeight()+17);}
    /** The card height: the room between the tab bar and the footer, shared by the track rows and their headings. */
    int classPanelHeight(){int rows=Math.max(1,classTracks().size());return (height-41-81-5*(rows-1))/rows-12;}
    /** A track heading's note. */
    static String trackNote(String track){return switch(track){case "hero"->"Fights in the front rank";case "general"->"Commands the whole army";default->"";};}
    /** The track headings drawn in the last frame, top to bottom, for the checks. */
    final List<String> classHeadings=new ArrayList<>();
    /**
     * One class card: its name and the troops it favours, its active ability (the ability key) with its cooldown and effect, and its
     * passive. A click picks it; CONFIRM CLASS sends the pick. Gold marks the pick.
     */
    final class ClassCard extends ButtonWidget {
        static final int ACTIVE_LINES=3,PASSIVE_LINES=2;
        final Spec.ClassesRow c;
        /** Text lines the card had no room for in the last frame; the checks want none. */
        int clipped;
        ClassCard(Spec.ClassesRow c,int x,int y,int w,int h){super(x,y,w,h,Text.literal(c.name()),b->{pendingClass=c.id();clearAndInit();},DEFAULT_NARRATION_SUPPLIER);this.c=c;}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY(),w=width,h=height,room=w-20;boolean selected=pendingClass.equals(c.id());
            panel(g,x,y,w,h);
            if(selected){g.fill(x,y,x+w,y+3,DarkUi.GOLD);g.drawBorder(x,y,w,h,DarkUi.GOLD);}else if(isHovered()||isFocused())g.drawBorder(x,y,w,h,DarkUi.MUTED);
            DarkUi.title(g,c.name().toUpperCase(Locale.ROOT),x+10,y+6,DarkUi.IVORY);
            String group=Arrays.stream(Spec.GROUPS).filter(r->r.id().equals(c.group())).map(r->r.label()).findFirst().orElse(c.group()).toUpperCase(Locale.ROOT);
            int gw=textRenderer.getWidth(DarkUi.body(group));DarkUi.label(g,group,x+w-10-gw,y+9,selected?DarkUi.GOLD:DarkUi.MUTED);
            String cooldown=c.cooldownTicks()/20+" s cooldown";int cdw=textRenderer.getWidth(DarkUi.body(cooldown));
            DarkUi.label(g,cooldown,x+w-10-cdw,y+23,DarkUi.MUTED);text(g,Commander.activeName(c.active()).toUpperCase(Locale.ROOT),x+10,y+23,room-cdw-6,DarkUi.GOLD);
            clipped=0;int ty=y+34;var lines=textRenderer.wrapLines(DarkUi.body(c.activeText()),room);
            for(int i=0;i<lines.size();i++){if(i>=ACTIVE_LINES){clipped++;continue;}g.drawText(textRenderer,lines.get(i),x+10,ty,DarkUi.IVORY,false);ty+=10;}
            int py=y+34+10*ACTIVE_LINES+2;DarkUi.label(g,"PASSIVE",x+10,py,DarkUi.MUTED);int pw=textRenderer.getWidth(DarkUi.body("PASSIVE"))+6;DarkUi.rule(g,x+10+pw,py+5,room-pw);
            ty=py+11;lines=textRenderer.wrapLines(DarkUi.body(c.passiveText()),room);
            for(int i=0;i<lines.size();i++){if(i>=PASSIVE_LINES||ty+9>y+h){clipped++;continue;}g.drawText(textRenderer,lines.get(i),x+10,ty,DarkUi.IVORY,false);ty+=10;}
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
        text(g,lead?"Invite commanders, then start the battle when you are ready.":member?"Waiting for the lead to start.":"Stand on a pad to quick-match, or wait for an invitation.",18,height-23,width-230,DarkUi.MUTED);
    }
    void text(DrawContext g,String value,int x,int y,int max,int color){g.drawText(textRenderer,net.minecraft.util.Language.getInstance().reorder(textRenderer.trimToWidth(DarkUi.body(value),max)),x,y,color,false);}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        g.getMatrices().push();g.getMatrices().scale(uiScale,uiScale,1);mx=(int)(mx/uiScale);my=(int)(my/uiScale);
        DarkUi.background(g,width,height);
        DarkUi.title(g,new String[]{"BATTLE SETUP","YOUR BANNER","CHOOSE YOUR CLASS","CUSTOM LOBBY"}[tab],18,10,DarkUi.IVORY);text(g,"GOLD  "+CampaignClient.profile.gold,width-270,21,110,DarkUi.GOLD);
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
            text(g,"700 x 500 blocks / 3 objectives",24,height-68,lw-16,DarkUi.MUTED);
            int total=CampaignClient.mode.equals("ai")?2:Integer.parseInt(CampaignClient.mode)*2;
            text(g,total+" commander slots",rx,height-72,rw,DarkUi.MUTED);
            String hint=CampaignClient.lead?(hasResult?"Choose the battlefield, then open a custom lobby to play again.":"Choose your battlefield, then start from the Custom lobby tab."):(hasResult?"Stand on a pad or open a custom lobby to play again.":"Only the custom lobby lead chooses the battlefield. Stand on a pad to quick-match.");
            text(g,hint,18,height-23,width-230,DarkUi.MUTED);
            if(CampaignClient.padCountdown>0)text(g,"Match starts in "+CampaignClient.padCountdown+" s / "+CampaignClient.padSize+"v"+CampaignClient.padSize,18,height-44,width-230,DarkUi.GOLD);
            else if(CampaignClient.padDawn+CampaignClient.padDusk>0)text(g,"On pads: Dawn "+CampaignClient.padDawn+", Dusk "+CampaignClient.padDusk+" / plays "+CampaignClient.padSize+"v"+CampaignClient.padSize,18,height-44,width-230,DarkUi.MUTED);
        }else if(tab==1){
            var catalog=RosterMath.Catalog.fromSpec();var line=CampaignClient.profile.banner;int used=(int)line.stream().filter(id->!id.equals(RosterMath.EMPTY)).count();
            text(g,"BANNER / "+RosterMath.units(line,catalog)+" UNITS IN "+used+" OF "+line.size()+" SLOTS",16,74,width/2-28,DarkUi.GOLD);
            DarkUi.frame(g,15,88,285,182);text(g,"FRONT / toward the enemy",24,94,270,DarkUi.MUTED);text(g,"REAR",24,260,270,DarkUi.MUTED);
            int left=width/2+8;var row=Arrays.stream(Spec.HOUSES).filter(h->h.id().equals(house)).findFirst().orElse(null);
            if(row!=null&&Arrays.stream(Spec.TROOPS).noneMatch(t->t.house().equals(house)&&CampaignClient.profile.owned.containsKey(t.id())))text(g,"No "+row.name()+" troops in your collection yet.",left,104,width-left-16,DarkUi.MUTED);
            text(g,pickLine(),left,266,width-left-16,DarkUi.GOLD);
            text(g,row!=null&&!row.fieldable()?row.name()+" has not joined the war: its troops cannot be fielded yet.":"Pick a troop, then a slot. Epics fill 2 slots, Legendaries 4.",left,280,width-left-16,DarkUi.MUTED);
            text(g,"New troops join by recruitment rolls at the camp shrine, "+Spec.RECRUITMENT[0].rollCost()+" gold each. The four marks set your banner colour.",16,height-27,width-32,DarkUi.MUTED);
        }else if(tab==2){
            // The track headings over each row of class cards (the cards draw themselves).
            classHeadings.clear();var tracks=classTracks();
            for(int r=0;r<tracks.size();r++){String track=tracks.get(r).get(0).track(),head=track.toUpperCase(Locale.ROOT),note=trackNote(track);int y=classRowTop(r),hx=18;
                text(g,head,hx,y,width-36,DarkUi.GOLD);hx+=textRenderer.getWidth(DarkUi.body(head))+8;
                if(!note.isEmpty()){text(g,note,hx,y,width-hx-18,DarkUi.MUTED);hx+=textRenderer.getWidth(DarkUi.body(note))+8;}
                DarkUi.rule(g,hx,y+5,width-16-hx);classHeadings.add(head);
            }
            var current=Arrays.stream(Spec.CLASSES).filter(c->c.id().equals(CampaignClient.profile.commander)).findFirst().orElse(Spec.CLASSES[0]);
            text(g,"Active commander: "+current.name(),18,height-23,width-230,DarkUi.IVORY);
        }else{
            partyText(g);
        }

        if(!CampaignClient.notice.isEmpty())text(g,HintText.expand(CampaignClient.notice,GravesAndGloryClient::hint),18,tab==1?height-12:69,width-36,DarkUi.GOLD);super.render(g,mx,my,delta);
        if(System.currentTimeMillis()<CampaignClient.revealUntil){int cx=width/2,cy=height/2;panel(g,cx-160,cy-45,320,90);g.drawBorder(cx-160,cy-45,320,90,DarkUi.GOLD);g.drawCenteredTextWithShadow(textRenderer,"A NEW SOLDIER JOINS YOUR BANNER",cx,cy-28,DarkUi.GOLD);long remain=CampaignClient.revealUntil-System.currentTimeMillis();String name=remain>1300?Spec.TROOPS[(int)(System.currentTimeMillis()/75%Spec.TROOPS.length)].name():CampaignClient.revealUnit;g.drawCenteredTextWithShadow(textRenderer,name,cx,cy-2,DarkUi.IVORY);g.drawCenteredTextWithShadow(textRenderer,remain>1300?"Opening recruitment seal...":"PERMANENTLY UNLOCKED",cx,cy+24,DarkUi.GOLD);}
        g.getMatrices().pop();
    }
}

