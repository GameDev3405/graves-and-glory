package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.*;
import static gg.gravesandglory.GravesAndGloryClient.*;

/** Real troop previews and server-authoritative purchases; no simulated UI inventory. */
class RecruitmentScreen extends ArmyScreen {
    record Controls(int index,ButtonWidget train,ButtonWidget disband){}
    final List<Controls> controls=new ArrayList<>();
    ButtonWidget cancel,recruit,disband; int selected=-1; float uiScale=1;
    RecruitmentScreen(){super("Barracks");}
    @Override protected void init(){
        uiScale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/640f,client.getWindow().getScaledHeight()/360f));
        width=(int)(client.getWindow().getScaledWidth()/uiScale);height=(int)(client.getWindow().getScaledHeight()/uiScale);
        controls.clear();int row=0;int left=18,lw=width*30/100;
        for(int i=0;i<Spec.TROOPS.length;i++){
            var t=Spec.TROOPS[i];if(!t.faction().equals(Spec.FACTIONS[Math.max(0,team)].id()))continue;
            final int index=i;if(selected<0)selected=i;
            var choose=addDrawableChild(new RosterChoice(index,left,82+row*29,lw,b->{selected=index;clearAndInit();}));

            // The same actions are used by the selected-unit controls and the mechanical harness.
            var train=new FantasyButton(0,0,1,1,Text.literal("Recruit "+t.name()),b->send("train:"+t.id()));
            var remove=new FantasyButton(0,0,1,1,Text.literal("Disband "+t.name()),b->send("disband:"+t.id()));
            controls.add(new Controls(i,train,remove));row++;
        }
        int rx=width*65/100,rw=width-rx-18;
        recruit=primary("RECRUIT",rx,height-112,rw,b->{var c=chosen();if(c!=null&&c.train.active)c.train.onPress();});
        disband=button("Disband one / no refund",rx,height-79,rw,b->{var c=chosen();if(c!=null&&c.disband.active)c.disband.onPress();});
        cancel=button("Cancel queue / full refund",18,height-67,lw,b->send("cancel-training"));
        button("Esc  Back",18,height-29,90,b->close());update();
    }
    final class RosterChoice extends ButtonWidget {
        final int index;
        RosterChoice(int index,int x,int y,int w,PressAction action){super(x,y,w,26,Text.literal(Spec.TROOPS[index].name()),action,DEFAULT_NARRATION_SUPPLIER);this.index=index;}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();var t=Spec.TROOPS[index];boolean chosen=selected==index;
            g.fill(x,y,x+width,y+height,chosen?0xff30474a:isHovered()?0xff354146:0xff222d32);
            if(chosen||isFocused())g.drawBorder(x,y,width,height,DarkUi.GOLD);
            g.drawItem(new net.minecraft.item.ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(t.weapon()))),x+5,y+5);
            DarkUi.label(g,t.name(),x+28,y+3,DarkUi.IVORY);
            DarkUi.label(g,t.cost()+" gold",x+28,y+15,DarkUi.MUTED);
            DarkUi.label(g,"Owned "+roster[index],x+width-48,y+15,DarkUi.MUTED);
        }
    }
    Controls chosen(){return controls.stream().filter(c->c.index==selected).findFirst().orElse(null);}
    String unavailable(Spec.TroopsRow t){
        if(!nearBarracks)return "Return to your barracks to recruit.";
        if(!CampaignClient.profile.unlocked.contains(t.id())&&!CampaignClient.mode.equals("ai"))return "Unlock this troop in Army collection.";
        if(count+queued>=20)return "Army and training queue are full.";
        if(gold<t.cost())return "You need "+(t.cost()-gold)+" more gold.";
        return "Training time: "+t.trainingSeconds()+" seconds";
    }
    void update(){
        for(var c:controls){var t=Spec.TROOPS[c.index];c.train.active=nearBarracks&&gold>=t.cost()&&count+queued<20&&(CampaignClient.profile.unlocked.contains(t.id())||CampaignClient.mode.equals("ai"));c.disband.active=nearBarracks&&roster[c.index]>0;}
        var c=chosen();if(c!=null){recruit.active=c.train.active;disband.active=c.disband.active;recruit.setMessage(Text.literal("RECRUIT / "+Spec.TROOPS[selected].cost()+" GOLD"));}
        cancel.active=nearBarracks&&queued>0;
    }
    @Override public void tick(){update();}
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/uiScale,y/uiScale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/uiScale,y/uiScale,b);}
    @Override public boolean mouseScrolled(double x,double y,double amount){return super.mouseScrolled(x/uiScale,y/uiScale,amount);}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        update();g.getMatrices().push();g.getMatrices().scale(uiScale,uiScale,1);
        DarkUi.background(g,width,height);DarkUi.title(g,"BARRACKS",18,15,DarkUi.IVORY);
        DarkUi.label(g,"Recruit and manage your army",18,40,DarkUi.MUTED);
        DarkUi.label(g,"GOLD  "+gold,width-190,19,DarkUi.GOLD);DarkUi.label(g,"ARMY  "+count+" / 20",width-190,39,DarkUi.IVORY);
        int lw=width*30/100,rx=width*65/100,rw=width-rx-18;
        DarkUi.label(g,"AVAILABLE TROOPS",18,70,DarkUi.MUTED);
        int cx=(18+lw+rx)/2;
        g.fill(26+lw,81,rx-12,height-45,0x401b3439);
        // Low plinth and contact shadow ground the live, lit model.
        g.fill(cx-48,height-91,cx+48,height-82,0xff101719);
        g.fill(cx-42,height-92,cx+42,height-89,0xff4a5557);
        if(selected>=0){var t=Spec.TROOPS[selected];
            DarkUi.model(g,t,cx,height-93,Math.min(74,(height-155)/3));
            DarkUi.title(g,t.name(),rx,84,DarkUi.IVORY);DarkUi.label(g,CommandMode.label(t.group()).toUpperCase(),rx,108,DarkUi.GOLD);
            DarkUi.rule(g,rx,128,rw);
            DarkUi.label(g,"Health",rx,143,DarkUi.MUTED);DarkUi.label(g,""+t.health(),rx+rw-26,143,DarkUi.IVORY);
            DarkUi.label(g,"Damage",rx,163,DarkUi.MUTED);DarkUi.label(g,""+t.damage(),rx+rw-26,163,DarkUi.IVORY);
            DarkUi.label(g,"In your army",rx,183,DarkUi.MUTED);DarkUi.label(g,""+roster[selected],rx+rw-26,183,DarkUi.IVORY);
            String ability=switch(t.ability()){case "shield"->"Shield / frontline protection";case "mend"->"Mend / restores allied health";case "blight"->"Blight / offensive magic";default->t.group().equals("ranged")?"Ranged / attacks from distance":"Melee / frontline fighter";};
            DarkUi.label(g,ability,rx,206,DarkUi.MUTED);
            int yy=height-141;for(var line:textRenderer.wrapLines(DarkUi.body(unavailable(t)),rw)){g.drawText(textRenderer,line,rx,yy,recruit.active?DarkUi.MUTED:DarkUi.GOLD,false);yy+=11;}
        }
        DarkUi.label(g,"TRAINING  "+queued+" queued",18,height-103,DarkUi.IVORY);
        DarkUi.label(g,queued>0?"Next deployment in "+trainingSeconds+" seconds":"Your training queue is empty",18,height-88,DarkUi.MUTED);
        DarkUi.label(g,"Replacements follow their group's orders.",130,height-22,DarkUi.MUTED);
        super.render(g,(int)(mx/uiScale),(int)(my/uiScale),delta);g.getMatrices().pop();
    }
}

