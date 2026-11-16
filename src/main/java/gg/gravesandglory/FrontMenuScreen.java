package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Ordinary launcher entry point. Starting a world or connection always takes a click. */
public final class FrontMenuScreen extends Screen {
    /** The approved key art (board 4F.3), 1920x1080, drawn cover-scaled behind every page of this menu. */
    private static final Identifier KEY_ART=Identifier.of("gravesandglory","textures/gui/title/key_art.png");
    private final String page;
    public FrontMenuScreen(){this("home");}
    FrontMenuScreen(String page){super(Text.literal("Graves and Glory / "+page));this.page=page;}
    private ButtonWidget button(String label,int y,boolean primary,Runnable action){
        // Home: a left column clear of the two heroes at the centre of the art; the other pages stay centred over a dimmed picture.
        int bw=page.equals("home")?columnWidth():220,x=page.equals("home")?columnX():width/2-110;
        var b=new GravesAndGloryClient.FantasyButton(x,y,bw,20,Text.literal(label),w->action.run());
        b.primary=primary;return addDrawableChild(b);
    }
    private int columnWidth(){return Math.max(96,Math.min(150,width*3/10));}
    private int columnX(){return Math.max(6,width/32);}
    /** Home: the row pitch (22, squeezed to 20, the buttons' own height, on a very short screen) so all five buttons end 4 px or more above the bottom. */
    private int homePitch(){return Math.max(20,Math.min(22,(height-4-Math.max(30,height-134)-20)/4));}
    private int maxTop(){
        if(page.equals("home"))return Math.min(Math.max(30,height-26-108),height-4-(4*homePitch()+20));
        int top=Math.max(76,height/2-38);
        // Friends: the buttons, then the two hint lines, then Back, all kept on screen (the title area gives way on a very short one)
        return page.equals("friends")?Math.min(top,height-4-(friendsBack()+20)):top;
    }
    /** Friends: Back's offset from the top of the buttons, below the last action button and the two hint lines (26 px for both). */
    private int friendsBack(){return 22*(GravesAndGloryClient.launcherContext?3:2)+34;}
    @Override protected void init(){
        int top=maxTop();
        if(page.equals("home")){
            int p=homePitch();
            button(UiText.t("menu.home.enterCamp","Enter camp / Practice"),top,true,()->GravesAndGloryClient.startCamp(client,false));
            button(UiText.t("menu.home.friends","Play with friends"),top+p,false,()->client.setScreen(new FrontMenuScreen("friends")));
            button(UiText.t("menu.home.settings","Settings"),top+2*p,false,()->client.setScreen(new OptionsScreen(this,client.options)));
            button(UiText.t("menu.home.credits","Credits"),top+3*p,false,()->client.setScreen(new FrontMenuScreen("credits")));
            button(UiText.t("menu.home.quit","Quit game"),top+4*p,false,client::scheduleStop);
        }else if(page.equals("friends")){
            button(UiText.t("menu.friends.host","Host a camp on this network"),top,true,()->GravesAndGloryClient.startCamp(client,true));
            button(UiText.t("menu.friends.join","Join a server"),top+22,false,()->client.setScreen(new MultiplayerScreen(this)));
            if(GravesAndGloryClient.launcherContext)button(GravesAndGloryClient.join.isEmpty()?UiText.t("menu.friends.hostMelty","Host through Melty"):UiText.t("menu.friends.invitation","Use launcher invitation"),top+44,false,()->{
                if(GravesAndGloryClient.join.isEmpty())GravesAndGloryClient.startCamp(client,true);
                else GravesAndGloryClient.connectOrHost(client);
            });
            button(UiText.t("menu.friends.back","Back"),top+friendsBack(),false,()->client.setScreen(new FrontMenuScreen()));
        }else button(UiText.t("menu.credits.back","Back"),height-40,false,()->client.setScreen(new FrontMenuScreen()));
    }
    /** 1.21: Screen.render draws this (blur, dimming, the title panorama) before the widgets; 1.20.1 drew none here, and this screen draws its own. */
    @Override public void renderBackground(DrawContext g,int mx,int my,float delta){}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        // Cover-scale: fill the window, keep the aspect, crop the overflow equally on both sides.
        float cover=Math.max(width/1920f,height/1080f);
        g.enableScissor(0,0,width,height);
        g.getMatrices().push();g.getMatrices().translate((width-1920*cover)/2f,(height-1080*cover)/2f,0);g.getMatrices().scale(cover,cover,1);
        g.drawTexture(KEY_ART,0,0,0,0,1920,1080,1920,1080);g.getMatrices().pop();g.disableScissor();
        if(!page.equals("home"))g.fill(0,0,width,height,0xaa000000);
        else{g.fillGradient(0,0,width,72,0x99000000,0x00000000);g.fillGradient(0,height-150,columnX()+columnWidth()+24,height,0x00000000,0x88000000);}
        DarkUi.crest(g,width/2-153,27,28,false);DarkUi.crest(g,width/2+125,27,28,true);
        g.getMatrices().push();g.getMatrices().translate(width/2f,29,0);g.getMatrices().scale(1.5f,1.5f,1);
        g.drawCenteredTextWithShadow(textRenderer,Text.literal(UiText.t("menu.title","GRAVES AND GLORY")).setStyle(net.minecraft.text.Style.EMPTY.withFont(DarkUi.TITLE)),0,0,DarkUi.GOLD);g.getMatrices().pop();
        g.drawCenteredTextWithShadow(textRenderer,UiText.t("menu.tagline","Collect an army. Command the battlefield."),width/2,54,DarkUi.PAPER);
        if(page.equals("friends")){
            g.drawCenteredTextWithShadow(textRenderer,UiText.t("menu.friends.heading","PLAY WITH FRIENDS"),width/2,maxTop()-17,DarkUi.GOLD);
            {
                // the two hint lines sit between the last action button and Back, 26 px for both
                int hy=maxTop()+22*(GravesAndGloryClient.launcherContext?3:2)+2;
                g.drawCenteredTextWithShadow(textRenderer,UiText.t("menu.friends.hintLan","Hosting shares this camp on your local network."),width/2,hy,DarkUi.MUTED);
                g.drawCenteredTextWithShadow(textRenderer,UiText.t("menu.friends.hintRemote","Remote friends need a reachable server or launcher relay."),width/2,hy+13,DarkUi.MUTED);
            }
        }
        if(page.equals("credits")){
            int y=height/2-30;
            for(String line:new String[]{UiText.t("menu.credits.line1","An original Minecraft army battle game"),UiText.t("menu.credits.line2","Built for Fabric / Minecraft Java Edition"),UiText.t("menu.credits.line3","Original code, UI, faction artwork and audio: Graves and Glory"),UiText.t("menu.credits.line4","Community mods, textures and shaders by their credited authors"),UiText.t("menu.credits.line5","Full credits, licenses and permissions are included in the pack's docs folder.")}){
                g.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(line,width-24),width/2,y,DarkUi.PAPER);y+=18;
            }
        }
        g.drawText(textRenderer,UiText.t("menu.version","v{version} / {build}","version",VersionGate.VERSION,"build",VersionGate.BUILD.substring(0,8)),8,height-13,DarkUi.MUTED,false);
        super.render(g,mx,my,delta);
    }
    @Override public boolean shouldCloseOnEsc(){return !page.equals("home");}
    @Override public void close(){client.setScreen(new FrontMenuScreen());}
}
