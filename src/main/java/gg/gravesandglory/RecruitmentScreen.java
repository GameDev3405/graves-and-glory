package gg.gravesandglory;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import static gg.gravesandglory.GravesAndGloryClient.*;

/** Real troop previews and server-authoritative purchases; no simulated UI inventory. Two War Table pages as tabs: the barracks (0, BarracksPage) and the Armoury (1, ArmouryTab). */
class RecruitmentScreen extends ArmyScreen {
    record Controls(int index,ButtonWidget train,ButtonWidget disband){}
    final List<Controls> controls=new ArrayList<>();
    ButtonWidget cancel,recruit,disband,barracksTab,armouryTab; int selected=-1; float uiScale=1;
    /** The page shown: 0 the barracks, 1 the Armoury. The Armoury key opens the screen on page 1 (GravesAndGloryClient), the lectern on page 0. */
    int tab; final ArmouryTab page=new ArmouryTab(this); final BarracksPage barracks=new BarracksPage(this);
    RecruitmentScreen(){this(0);}
    /** Keys whose press this screen took and which are not released since: the Armoury key (also when it was down as the screen opened, the key that opened it), an Enter that recruited the picked troop, and an Enter or Space that pressed the Recruit or Disband plate. Minecraft hands a screen each repeat of a held key as another keyPressed; those stop here until keyReleased, so a held Armoury key does not close the page it opened and a held Enter neither recruits nor disbands a troop per repeat: one per press (Kort, C12-P3-F2b; as CampScreen.held). */
    final Set<Integer> held=new HashSet<>();
    RecruitmentScreen(int tab){super("Barracks");this.tab=tab;if(armoury!=null){var bound=net.minecraft.client.util.InputUtil.fromTranslationKey(armoury.getBoundKeyTranslationKey());if(bound.getCategory()==net.minecraft.client.util.InputUtil.Type.KEYSYM&&bound.getCode()>=0&&net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(),bound.getCode()))held.add(bound.getCode());}}
    /** The page's widgets are added through the screen (addDrawableChild is protected), and the screen is built again when its rows change or scroll. */
    <T extends Element&Drawable&Selectable> T add(T widget){return addDrawableChild(widget);}
    void rebuild(){clearAndInit();}
    void show(int next){if(tab!=next){tab=next;clearAndInit();}}
    /** The rail, as every War Table page has it: Back (Esc) to the battle, then the two pages' plaques, the one shown lit as the page you are on ("> Barracks", "> Armoury"). */
    void railTabs(){
        add(WarTable.plaque(56,96,"icon-camp","Back","Esc",61,0,b->close()));
        barracksTab=add(tab==0?here(184,"Barracks","icon-recruit"):WarTable.plaque(184,112,"icon-recruit","Barracks",null,0,0,b->show(0)));
        armouryTab=add(tab==1?here(304,"Armoury","icon-banner"):WarTable.plaque(304,112,"icon-banner","Armoury",null,0,0,b->show(1)));
    }
    private UiButton here(int x,String label,String icon){
        String mark="table/"+icon;
        return new UiButton(WarTable.railX(x),12,112,28,Text.literal("> "+label),b->{},(g,bx,by,w,h,hot)->{
            UiTex.draw(g,"table/plaque-112-here",bx,by);
            UiTex.draw(g,mark,bx+8,by+7);
            g.drawText(textRenderer,DarkUi.heading(label),bx+28,by+8,DarkUi.RAIL_KEY,false);
        });
    }
    @Override protected void init(){
        uiScale=1;controls.clear(); // both pages are War Table pages: canvas pixels at UiMath.pageScale, the window's own GUI size
        if(tab==1){page.init();railTabs();return;}
        // The same actions are used by the page's plates and the mechanical harness: one pair per troop row.
        for(int i:bannerTroops()){
            var t=Spec.TROOPS[i];
            var train=new FantasyButton(0,0,1,1,Text.literal("Recruit "+t.name()),b->send("train:"+t.id()));
            var remove=new FantasyButton(0,0,1,1,Text.literal("Disband "+t.name()),b->send("disband:"+t.id()));
            controls.add(new Controls(i,train,remove));
        }
        barracks.init();railTabs();update();
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
    /** A troop's round price: the sheet cost, as the server charges it (no class discount: the classes are gone). */
    static int price(Spec.TroopsRow t){return t.cost();}
    static boolean trainable(Spec.TroopsRow t){int i=index(t);return nearBarracks&&gold>=price(t)&&roster[i]+pending[i]<listed(t)&&weight(true)+t.slotWeight()<=GravesAndGlory.R.squadSize();}
    String unavailable(Spec.TroopsRow t){
        if(!nearBarracks)return "Return to your barracks to recruit.";
        int i=index(t),listed=listed(t);
        if(listed==0)return "Not in your banner. Change your banner in the war council.";
        if(roster[i]+pending[i]>=listed)return "All "+listed+" of your banner are fielded or in training.";
        if(weight(true)+t.slotWeight()>GravesAndGlory.R.squadSize())return "Army and training queue are full.";
        if(gold<price(t))return "You need "+(price(t)-gold)+" more gold.";
        return trainingTime(t);
    }
    /** The training time line: whole seconds without a decimal (4 seconds), others with theirs (3.5 seconds). */
    static String trainingTime(Spec.TroopsRow t){double s=Commander.trainingTicks(t)/20.0;String n=s==Math.rint(s)?Long.toString(Math.round(s)):Double.toString(s);return "Training time: "+n+(n.equals("1")?" second":" seconds");}
    void update(){
        for(var c:controls){var t=Spec.TROOPS[c.index];c.train.active=trainable(t);c.disband.active=nearBarracks&&roster[c.index]>0;}
        var c=chosen();if(c!=null){recruit.active=c.train.active;disband.active=c.disband.active;recruit.setMessage(Text.literal("RECRUIT / "+price(Spec.TROOPS[selected])+" GOLD"));}
        cancel.active=nearBarracks&&queued>0;
        if(c==null){recruit.active=false;disband.active=false;}
    }
    @Override public void tick(){if(tab==1)page.tick();else{barracks.tick();update();}}
    @Override public boolean mouseClicked(double x,double y,int b){boolean r=super.mouseClicked(x/uiScale,y/uiScale,b);if(tab==0)setFocused(null);return r;}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/uiScale,y/uiScale,b);}
    @Override public boolean mouseScrolled(double x,double y,double amount){if(amount==0)return true;if(tab==1&&page.scroll(amount>0?-1:1))return true;if(tab==0&&barracks.scroll(amount>0?-1:1))return true;return super.mouseScrolled(x/uiScale,y/uiScale,amount);}
    /** The Armoury key switches to the Armoury page (from the barracks) or closes the screen (from the Armoury); Page Up and Down scroll the Armoury's troop rows. */
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(held.contains(key))return true;
        if(armoury!=null&&armoury.matchesKey(key,scan)&&!(tab==1&&page.editing>0)){held.add(key);if(tab==1)close();else show(1);return true;}
        if(tab==1&&(key==GLFW.GLFW_KEY_PAGE_UP||key==GLFW.GLFW_KEY_PAGE_DOWN)&&page.scroll(key==GLFW.GLFW_KEY_PAGE_UP?-1:1))return true;
        if(tab==0){ // Enter recruits the picked troop (the key its plate shows); Up and Down pick the row above or below; Page Up and Down scroll the roll
            if((key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)&&recruit!=null&&recruit.active&&!(getFocused() instanceof ButtonWidget f&&f!=recruit&&f.isFocused()&&client.getNavigationType().isKeyboard())){held.add(key);recruit.onPress();return true;}
            if(key==GLFW.GLFW_KEY_UP||key==GLFW.GLFW_KEY_DOWN){barracks.step(key==GLFW.GLFW_KEY_UP?-1:1);setFocused(null);return true;}
            if((key==GLFW.GLFW_KEY_PAGE_UP||key==GLFW.GLFW_KEY_PAGE_DOWN)&&barracks.scroll(key==GLFW.GLFW_KEY_PAGE_UP?-1:1))return true;
            if((key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE)&&getFocused()!=null&&(getFocused()==disband||getFocused()==recruit)){boolean r=super.keyPressed(key,scan,mods);if(r)held.add(key);return r;}
        }
        return super.keyPressed(key,scan,mods);
    }
    /** A held key's release lets its presses through again (see held). */
    @Override public boolean keyReleased(int key,int scan,int mods){if(held.remove(key))return true;return super.keyReleased(key,scan,mods);}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        if(tab==0)update();
        UiCanvas.begin(g);try{if(tab==1)page.render(g);else barracks.render(g);}finally{UiCanvas.end(g);}
        super.render(g,mx,my,delta);
    }
}

