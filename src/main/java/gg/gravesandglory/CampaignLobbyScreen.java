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
    String pendingClass="";int tab=0;String key="";List<ButtonWidget> leadControls=new ArrayList<>();ButtonWidget start,roll,confirmClass;
    public CampaignLobbyScreen(){super("Graves and Glory war council");}
    String stateKey(){return CampaignClient.mode+CampaignClient.map+CampaignClient.fill+CampaignClient.partyKey+team+CampaignClient.profile.commander+CampaignClient.profile.unlocked.size()+CampaignClient.headline;}
    public void showTab(int value){tab=value;clearAndInit();}
    @Override protected void init(){
        uiScale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/640f,client.getWindow().getScaledHeight()/360f));width=(int)(client.getWindow().getScaledWidth()/uiScale);height=(int)(client.getWindow().getScaledHeight()/uiScale);
        start=null;roll=null;slots.clear();leadControls.clear();key=stateKey();int cw=(width-40)/4;
        String[] names={"Battle setup","Army collection","Commander class","Custom lobby"};
        for(int i=0;i<4;i++){final int t=i;button((tab==i?"> ":"")+names[i]+(i==3&&invited()?" *":""),16+i*(cw+4),43,cw,b->{tab=t;clearAndInit();});}
        button("Visuals & sound",width-156,17,140,b->client.setScreen(new PresentationScreen()));
        if(tab==0){
            int lw=(width-52)*2/3,rx=28+lw,rw=width-rx-16;
            String[] modes={"ai","1","2","3","4"};int bw=lw/5;
            for(int i=0;i<5;i++){String id=modes[i];leadControls.add(button((id.equals(CampaignClient.mode)?"> ":"")+(id.equals("ai")?"Practice":id+"v"+id),16+i*bw,81,bw-4,b->send("mode:"+id)));}
            int mw=lw/4;for(int i=0;i<4;i++){final int id=i;leadControls.add(button((CampaignClient.map==i?"> ":"")+Spec.MAPS[i].name(),16+i*mw,height-104,mw-4,b->send("map:"+id)));}
            leadControls.add(button("Bot fill: "+(CampaignClient.fill?"ON":"OFF"),rx,height-104,rw,b->send("fill")));
        }else if(tab==1){
            int row=0,t=Math.max(0,team),left=width/2+8,cardWidth=(width-left-24)/3;
            if(Arrays.stream(Spec.TROOPS).noneMatch(u->u.id().equals(selectedUnit)&&u.faction().equals(Spec.FACTIONS[t].id()))&&!selectedUnit.equals("empty"))selectedUnit=t==0?"knight":"revenant";
            for(var unit:Spec.TROOPS)if(unit.faction().equals(Spec.FACTIONS[t].id())){String id=unit.id();
                var choose=addDrawableChild(new ArmyCards.Recruit(unit,left+(row%3)*(cardWidth+4),91+(row/3)*84,cardWidth,b->{selectedUnit=id;clearAndInit();}));choose.active=true;row++;
            }
            for(int slot=0;slot<20;slot++){final int index=slot;slots.add(addDrawableChild(new ArmyCards.Deployment(index,24+(slot%5)*54,111+(slot/5)*36,b->send("slot:"+index+":"+selectedUnit))));}
            button((selectedUnit.equals("empty")?"> ":"")+"Remove from slot",20,276,160,b->{selectedUnit="empty";clearAndInit();});
            var remove=button("-",left,264,26,b->send("army:"+selectedUnit+":remove"));var add=button("+",width-46,264,26,b->send("army:"+selectedUnit+":add"));remove.active=add.active=!selectedUnit.equals("empty");
            var pick=ArmyCards.spec(selectedUnit);boolean owned=pick==null||CampaignClient.profile.unlocked.contains(pick.id());
            roll=button(owned?"SELECT A LOCKED TROOP TO UNLOCK":"UNLOCK "+pick.name().toUpperCase()+" / "+Spec.CAMPAIGN[0].rollCost()+" GOLD",16,height-52,Math.min(260,width-32),b->{if(!owned)send("unlock:"+pick.id());});
        }else if(tab==2){
            if(pendingClass.isEmpty())pendingClass=CampaignClient.profile.commander;
            int cw2=(width-48)/3;
            for(int i=0;i<Spec.CLASSES.length;i++){var spec=Spec.CLASSES[i];
                var select=button((pendingClass.equals(spec.id())?"> ":"")+"Select "+spec.name(),24+i*(cw2+8),height-74,cw2-16,b->{pendingClass=spec.id();clearAndInit();});
            }
            confirmClass=primary("CONFIRM CLASS",width-195,height-33,177,b->{send("class:"+pendingClass);});
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
        if(roll!=null){var pick=ArmyCards.spec(selectedUnit);roll.active=pick!=null&&!CampaignClient.profile.unlocked.contains(pick.id())&&CampaignClient.profile.gold>=Spec.CAMPAIGN[0].rollCost()&&!running;}
        if(tab==1){var line=CampaignClient.profile.lineup(Math.max(0,team));for(int i=0;i<slots.size();i++)slots.get(i).setUnit(line.get(i));}
        if(running)close();
    }
    /** Custom lobby text: the lead, the members, and any invitation waiting for this player. */
    void partyText(DrawContext g){
        JsonObject party=CampaignClient.party;boolean open=party.get("open").getAsBoolean(),member=party.get("member").getAsBoolean(),lead=CampaignClient.lead;
        int rx=24,rw=width-48;String leader=party.get("leader").getAsString();
        if(invited())text(g,"Invitation from "+party.get("inviter").getAsString()+". Accept or decline below.",rx,137,rw,GOLD);
        else if(!open)text(g,"No custom lobby is open. Open one to invite commanders who are online here.",rx,137,rw,MUTED);
        else if(!member)text(g,"Lead: "+leader+". Ask them for an invitation.",rx,137,rw,MUTED);
        else text(g,"Lead: "+leader+"    Commanders: "+party.getAsJsonArray("members").size()+" of "+party.get("capacity").getAsInt(),rx,137,rw,GOLD);
        int i=0;
        for(var element:party.getAsJsonArray("members")){String name=element.getAsString();boolean isLead=name.equals(leader);
            text(g,name+(isLead?"  (lead)":""),rx+(i/6)*(rw/4),155+(i%6)*12,rw/4-8,isLead?GOLD:WHITE);i++;}
        if(lead)text(g,"INVITE ONLINE COMMANDERS",rx,262,rw,MUTED);
        else if(member)text(g,"Only the lead can invite commanders or start the battle.",rx,262,rw,MUTED);
        text(g,lead?"Invite commanders, then start the battle when you are ready.":member?"Waiting for the lead to start.":"Stand on a pad to quick-match, or wait for an invitation.",18,height-23,width-230,MUTED);
    }
    void text(DrawContext g,String value,int x,int y,int max,int color){g.drawText(textRenderer,net.minecraft.util.Language.getInstance().reorder(textRenderer.trimToWidth(DarkUi.body(value),max)),x,y,color,false);}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        g.getMatrices().push();g.getMatrices().scale(uiScale,uiScale,1);mx=(int)(mx/uiScale);my=(int)(my/uiScale);
        DarkUi.background(g,width,height);
        DarkUi.title(g,new String[]{"BATTLE SETUP","ARMY COLLECTION","CHOOSE YOUR CLASS","CUSTOM LOBBY"}[tab],18,10,WHITE);text(g,"GOLD  "+CampaignClient.profile.gold,width-270,21,110,GOLD);
        boolean hasResult=!CampaignClient.headline.isEmpty();
        text(g,hasResult?CampaignClient.headline:ready?"Choose your army. Raise your banner.":message,18,30,width-185,hasResult?GOLD:MUTED);
        if(tab==0){
            int lw=(width-52)*2/3,rx=28+lw,rw=width-rx-16,previewH=height-225;
            panel(g,16,109,lw,previewH);
            // The tactical preview is built from this battlefield's real terrain.
            var preview=new net.minecraft.util.Identifier("gravesandglory","textures/ui/map-"+CampaignClient.map+".png");
            if(client.getResourceManager().getResource(preview).isPresent())g.drawTexture(preview,17,110,lw-2,previewH-2,0f,130f,1280,460,1280,720);
            else map(g,17,110,lw-2,previewH-2,false);
            g.fill(17,110,15+lw,140,0xdd172126);
            DarkUi.title(g,Spec.MAPS[CampaignClient.map].name(),26,118,WHITE);
            panel(g,rx-8,76,rw+16,height-121);
            text(g,"COMMANDERS",rx,85,rw,WHITE);int y=144;
            for(var element:CampaignClient.players){var p=element.getAsJsonObject();int faction=p.get("team").getAsInt();
                DarkUi.crest(g,rx,y-2,12,faction==1);text(g,p.get("name").getAsString(),rx+20,y,rw-20,faction==0?0xff84c4bf:0xffc4a7d9);y+=13;}
            text(g,"700 x 500 blocks / 3 objectives",24,height-68,lw-16,MUTED);
            int total=CampaignClient.mode.equals("ai")?2:Integer.parseInt(CampaignClient.mode)*2;
            text(g,total+" commander slots",rx,height-72,rw,MUTED);
            String hint=CampaignClient.lead?(hasResult?"Choose the battlefield, then open a custom lobby to play again.":"Choose your battlefield, then start from the Custom lobby tab."):(hasResult?"Stand on a pad or open a custom lobby to play again.":"Only the custom lobby lead chooses the battlefield. Stand on a pad to quick-match.");
            text(g,hint,18,height-23,width-230,MUTED);
            if(CampaignClient.padCountdown>0)text(g,"Match starts in "+CampaignClient.padCountdown+" s / "+CampaignClient.padSize+"v"+CampaignClient.padSize,18,height-44,width-230,GOLD);
            else if(CampaignClient.padDawn+CampaignClient.padDusk>0)text(g,"On pads: Dawn "+CampaignClient.padDawn+", Dusk "+CampaignClient.padDusk+" / plays "+CampaignClient.padSize+"v"+CampaignClient.padSize,18,height-44,width-230,MUTED);
        }else if(tab==1){
            text(g,(team==1?"DUSK":"DAWN")+" / "+CampaignClient.profile.count(Math.max(0,team))+" OF 20 SELECTED",16,74,width-32,GOLD);
            DarkUi.frame(g,15,88,285,182);text(g,"FRONT / toward the enemy",24,94,270,MUTED);text(g,"REAR",24,260,270,MUTED);
            var chosen=ArmyCards.spec(selectedUnit);text(g,chosen==null?"Select a troop portrait":chosen.name(),width/2+45,270,width/2-98,GOLD);
            text(g,"Select a portrait, then place it in your army.",width/2+8,292,width/2-20,MUTED);
            text(g,"100 gold / equal odds / no duplicates / permanent unlocks",16,height-27,width-32,MUTED);
        }else if(tab==2){
            int cw=(width-48)/3;
            for(int i=0;i<3;i++){var c=Spec.CLASSES[i];int x=16+i*(cw+8);boolean selected=pendingClass.equals(c.id());
                panel(g,x,77,cw,height-125);if(selected){g.fill(x,77,x+cw,80,GOLD);g.drawBorder(x,77,cw,height-125,GOLD);}
                DarkUi.title(g,c.name().toUpperCase(),x+12,90,WHITE);
                text(g,CommandMode.label(c.group()).toUpperCase(),x+12,111,cw-24,selected?GOLD:MUTED);
                var spec=Arrays.stream(Spec.TROOPS).filter(t->t.faction().equals(team==1?"dusk":"dawn")&&t.group().equals(c.group())).findFirst().orElse(null);
                int bottom=height-137;if(spec!=null)DarkUi.model(g,spec,x+cw/2,bottom,Math.min(42,(height-220)/3));
                DarkUi.rule(g,x+12,bottom+5,cw-24);int y=bottom+14;
                for(var line:textRenderer.wrapLines(DarkUi.body(c.bonus()),cw-24)){g.drawText(textRenderer,line,x+12,y,WHITE,false);y+=11;}
            }
            var current=Arrays.stream(Spec.CLASSES).filter(c->c.id().equals(CampaignClient.profile.commander)).findFirst().orElse(Spec.CLASSES[0]);
            text(g,"Active commander: "+current.name(),18,height-23,width-230,WHITE);
        }else{
            partyText(g);
        }

        if(!CampaignClient.notice.isEmpty())text(g,CampaignClient.notice,18,tab==1?height-12:69,width-36,GOLD);super.render(g,mx,my,delta);
        if(System.currentTimeMillis()<CampaignClient.revealUntil){int cx=width/2,cy=height/2;panel(g,cx-160,cy-45,320,90);g.drawBorder(cx-160,cy-45,320,90,GOLD);g.drawCenteredTextWithShadow(textRenderer,"A NEW SOLDIER JOINS YOUR BANNER",cx,cy-28,GOLD);long remain=CampaignClient.revealUntil-System.currentTimeMillis();String name=remain>1300?Spec.TROOPS[(int)(System.currentTimeMillis()/75%Spec.TROOPS.length)].name():CampaignClient.revealUnit;g.drawCenteredTextWithShadow(textRenderer,name,cx,cy-2,WHITE);g.drawCenteredTextWithShadow(textRenderer,remain>1300?"Opening recruitment seal...":"PERMANENTLY UNLOCKED",cx,cy+24,GOLD);}
        g.getMatrices().pop();
    }
}

