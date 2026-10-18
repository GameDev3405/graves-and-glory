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
        controls.clear();int row=0;int left=18,lw=width*30/100;var listed=bannerTroops();
        if(!listed.contains(selected))selected=listed.isEmpty()?-1:listed.get(0);
        int rowH=Math.max(14,Math.min(29,(height-67-6-82)/Math.max(1,listed.size()))); // a banner can list up to 11 troop types; the rows shrink to fit
        for(int i:listed){
            var t=Spec.TROOPS[i];
            final int index=i;
            var choose=addDrawableChild(new RosterChoice(index,left,82+row*rowH,lw,rowH-3,b->{selected=index;clearAndInit();}));

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
        RosterChoice(int index,int x,int y,int w,int h,PressAction action){super(x,y,w,h,Text.literal(Spec.TROOPS[index].name()),action,DEFAULT_NARRATION_SUPPLIER);this.index=index;}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();var t=Spec.TROOPS[index];boolean chosen=selected==index;
            g.fill(x,y,x+width,y+height,chosen?0xff30474a:isHovered()?0xff354146:0xff222d32);
            if(chosen||isFocused())g.drawBorder(x,y,width,height,GOLD);
            String fielded=roster[index]+" / "+listed(t);
            if(height>=24){g.drawItem(new net.minecraft.item.ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(t.weapon()))),x+5,y+5);
                DarkUi.label(g,t.name(),x+28,y+3,WHITE);DarkUi.label(g,t.cost()+" gold",x+28,y+15,MUTED);DarkUi.label(g,fielded,x+width-40,y+15,MUTED);}
            else{DarkUi.label(g,t.name()+"  "+t.cost()+" gold",x+5,y+(height-8)/2,WHITE);DarkUi.label(g,fielded,x+width-40,y+(height-8)/2,MUTED);}
        }
    }
    Controls chosen(){return controls.stream().filter(c->c.index==selected).findFirst().orElse(null);}
    /** The troop rows the barracks lists: each troop type of this commander's banner once, in banner order (spec 6.2). */
    static List<Integer> bannerTroops(){List<Integer> out=new ArrayList<>();for(String id:CampaignClient.banner)for(int i=0;i<Spec.TROOPS.length;i++)if(Spec.TROOPS[i].id().equals(id)&&!out.contains(i))out.add(i);return out;}
    /** How many of a troop the banner lists: the most that may be fielded and in training at once. */
    static int listed(Spec.TroopsRow t){return Collections.frequency(CampaignClient.banner,t.id());}
    /** Banner slots the living and queued troops fill. */
    static int weight(boolean queued){int w=0;for(int i=0;i<Spec.TROOPS.length;i++)w+=(roster[i]+(queued?pending[i]:0))*Spec.TROOPS[i].slotWeight();return w;}
    static int index(Spec.TroopsRow t){for(int i=0;i<Spec.TROOPS.length;i++)if(Spec.TROOPS[i]==t)return i;return 0;}
    /** Whether the barracks would train one more of this troop now: banner count, slot weights and gold. */
    static boolean trainable(Spec.TroopsRow t){int i=index(t);return nearBarracks&&gold>=t.cost()&&roster[i]+pending[i]<listed(t)&&weight(true)+t.slotWeight()<=GravesAndGlory.R.squadSize();}
    String unavailable(Spec.TroopsRow t){
        if(!nearBarracks)return "Return to your barracks to recruit.";
        int i=index(t),listed=listed(t);
        if(listed==0)return "Not in your banner. Change your banner in the war council.";
        if(roster[i]+pending[i]>=listed)return "All "+listed+" of your banner are fielded or in training.";
        if(weight(true)+t.slotWeight()>GravesAndGlory.R.squadSize())return "Army and training queue are full.";
        if(gold<t.cost())return "You need "+(t.cost()-gold)+" more gold.";
        return "Training time: "+t.trainingSeconds()+" seconds";
    }
    void update(){
        for(var c:controls){var t=Spec.TROOPS[c.index];c.train.active=trainable(t);c.disband.active=nearBarracks&&roster[c.index]>0;}
        var c=chosen();if(c!=null){recruit.active=c.train.active;disband.active=c.disband.active;recruit.setMessage(Text.literal("RECRUIT / "+Spec.TROOPS[selected].cost()+" GOLD"));}
        cancel.active=nearBarracks&&queued>0;
        if(c==null){recruit.active=false;disband.active=false;}
    }
    @Override public void tick(){update();}
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/uiScale,y/uiScale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/uiScale,y/uiScale,b);}
    @Override public boolean mouseScrolled(double x,double y,double amount){return super.mouseScrolled(x/uiScale,y/uiScale,amount);}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        update();g.getMatrices().push();g.getMatrices().scale(uiScale,uiScale,1);
        DarkUi.background(g,width,height);DarkUi.title(g,"BARRACKS",18,15,WHITE);
        DarkUi.label(g,"Recruit and manage your army",18,40,MUTED);
        DarkUi.label(g,"GOLD  "+gold,width-190,19,GOLD);DarkUi.label(g,"ARMY  "+weight(false)+" / "+GravesAndGlory.R.squadSize()+" SLOTS",width-190,39,WHITE);
        int lw=width*30/100,rx=width*65/100,rw=width-rx-18;
        DarkUi.label(g,"YOUR BANNER'S TROOPS",18,70,MUTED);
        int cx=(18+lw+rx)/2;
        g.fill(26+lw,81,rx-12,height-45,0x401b3439);
        // Low plinth and contact shadow ground the live, lit model.
        g.fill(cx-48,height-91,cx+48,height-82,0xff101719);
        g.fill(cx-42,height-92,cx+42,height-89,0xff4a5557);
        if(selected>=0){var t=Spec.TROOPS[selected];
            DarkUi.model(g,t,cx,height-93,Math.min(74,(height-155)/3));
            DarkUi.title(g,t.name(),rx,84,WHITE);DarkUi.label(g,CommandMode.label(t.group()).toUpperCase(),rx,108,GOLD);
            DarkUi.rule(g,rx,128,rw);
            DarkUi.label(g,"Health",rx,143,MUTED);DarkUi.label(g,""+t.health(),rx+rw-26,143,WHITE);
            DarkUi.label(g,"Damage",rx,163,MUTED);DarkUi.label(g,""+t.damage(),rx+rw-26,163,WHITE);
            DarkUi.label(g,"Fielded / in banner",rx,183,MUTED);DarkUi.label(g,roster[selected]+" / "+listed(t),rx+rw-34,183,WHITE);
            String ability=switch(Tactics.mechanic(t)){case "shield"->"Shield / frontline protection";case "mend"->"Mend / restores allied health";case "blight"->"Blight / offensive magic";default->t.group().equals("ranged")?"Ranged / attacks from distance":"Melee / frontline fighter";};
            DarkUi.label(g,ability,rx,206,MUTED);
            int yy=height-141;for(var line:textRenderer.wrapLines(DarkUi.body(unavailable(t)),rw)){g.drawText(textRenderer,line,rx,yy,recruit.active?MUTED:GOLD,false);yy+=11;}
        }
        DarkUi.label(g,"TRAINING  "+queued+" queued",18,height-103,WHITE);
        DarkUi.label(g,queued>0?"Next deployment in "+trainingSeconds+" seconds":"Your training queue is empty",18,height-88,MUTED);
        DarkUi.label(g,"Replacements follow their group's orders.",130,height-22,MUTED);
        super.render(g,(int)(mx/uiScale),(int)(my/uiScale),delta);g.getMatrices().pop();
    }
}

