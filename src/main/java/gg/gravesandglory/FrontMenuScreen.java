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
    private static final Identifier KEY_ART=new Identifier("gravesandglory","textures/gui/title/key_art.png");
    private final String page;
    public FrontMenuScreen(){this("home");}
    FrontMenuScreen(String page){super(Text.literal("Graves and Glory / "+page));this.page=page;}
    private ButtonWidget button(String label,int y,boolean primary,Runnable action){
        // Home: a left column clear of the two heroes at the centre of the art; the other pages stay centred over a dimmed picture.
        int w=page.equals("home")?columnWidth():220,x=page.equals("home")?columnX():width/2-110;
        var b=new GravesAndGloryClient.FantasyButton(x,y,w,20,Text.literal(label),w->action.run());
        b.primary=primary;return addDrawableChild(b);
    }
    private int columnWidth(){return Math.max(96,Math.min(150,width*3/10));}
    private int columnX(){return Math.max(6,width/32);}
    private int maxTop(){return page.equals("home")?Math.max(30,height-26-108):Math.max(76,height/2-38);}
    @Override protected void init(){
        int top=maxTop();
        if(page.equals("home")){
            button("Enter camp / Practice",top,true,()->GravesAndGloryClient.startCamp(client,false));
            button("Play with friends",top+22,false,()->client.setScreen(new FrontMenuScreen("friends")));
            button("Settings",top+44,false,()->client.setScreen(new OptionsScreen(this,client.options)));
            button("Credits",top+66,false,()->client.setScreen(new FrontMenuScreen("credits")));
            button("Quit game",top+88,false,client::scheduleStop);
        }else if(page.equals("friends")){
            button("Host a camp on this network",top,true,()->GravesAndGloryClient.startCamp(client,true));
            button("Join a server",top+22,false,()->client.setScreen(new MultiplayerScreen(this)));
            if(GravesAndGloryClient.launcherContext)button(GravesAndGloryClient.join.isEmpty()?"Host through Melty":"Use launcher invitation",top+44,false,()->{
                if(GravesAndGloryClient.join.isEmpty())GravesAndGloryClient.startCamp(client,true);
                else GravesAndGloryClient.connectOrHost(client);
            });
            button("Back",top+88,false,()->client.setScreen(new FrontMenuScreen()));
        }else button("Back",height-40,false,()->client.setScreen(new FrontMenuScreen()));
    }
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
        g.drawCenteredTextWithShadow(textRenderer,Text.literal("GRAVES AND GLORY").setStyle(net.minecraft.text.Style.EMPTY.withFont(DarkUi.TITLE)),0,0,DarkUi.GOLD);g.getMatrices().pop();
        g.drawCenteredTextWithShadow(textRenderer,"Collect an army. Command the battlefield.",width/2,54,DarkUi.PAPER);
        if(page.equals("friends")){
            g.drawCenteredTextWithShadow(textRenderer,"PLAY WITH FRIENDS",width/2,maxTop()-17,DarkUi.GOLD);
            g.drawCenteredTextWithShadow(textRenderer,"Hosting shares this camp on your local network.",width/2,height/2+54,DarkUi.MUTED);
            g.drawCenteredTextWithShadow(textRenderer,"Remote friends need a reachable server or launcher relay.",width/2,height/2+67,DarkUi.MUTED);
        }
        if(page.equals("credits")){
            int y=height/2-30;
            for(String line:new String[]{"An original Minecraft army battle game","Built for Fabric / Minecraft Java Edition","Original code, UI, faction artwork and audio: Graves and Glory","Community mods, textures and shaders by their credited authors","Full credits, licenses and permissions are included in the pack's docs folder."}){
                g.drawCenteredTextWithShadow(textRenderer,textRenderer.trimToWidth(line,width-24),width/2,y,DarkUi.PAPER);y+=18;
            }
        }
        g.drawText(textRenderer,"v"+VersionGate.VERSION+" / "+VersionGate.BUILD.substring(0,8),8,height-13,DarkUi.MUTED,false);
        super.render(g,mx,my,delta);
    }
    @Override public boolean shouldCloseOnEsc(){return !page.equals("home");}
    @Override public void close(){client.setScreen(new FrontMenuScreen());}
}
