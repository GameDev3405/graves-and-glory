package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class PresentationScreen extends GravesAndGloryClient.ArmyScreen {
    float scale=1;int selected;
    PresentationScreen(){super("Visuals and sound");}
    @Override protected void init(){
        Presentation.load();selected=Presentation.shader();scale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/640f,client.getWindow().getScaledHeight()/360f));width=(int)(client.getWindow().getScaledWidth()/scale);height=(int)(client.getWindow().getScaledHeight()/scale);
        for(int i=0;i<4;i++){final int n=i;button((i==selected?"> ":"")+Presentation.SHADERS[i],30+i%2*190,94+i/2*55,180,b->{Presentation.selectShader(n);clearAndInit();});}
        int texture=Presentation.textures();for(int i=0;i<3;i++){final int n=i;button((texture==i?"> ":"")+Presentation.TEXTURES[i],30+i*195,222,185,b->{Presentation.selectTextures(n);clearAndInit();});}
        button("Impact accents: "+new String[]{"OFF","SUBTLE","STRONG"}[Presentation.impact],30,262,170,b->{Presentation.impact=(Presentation.impact+1)%3;Presentation.save();clearAndInit();});
        // UI Task 8c: Compact, Standard, Large. Fix round 1: the button names the size this window draws the choice at where that is
        // smaller ("Battle HUD: Standard (Compact size here)", 201 px of 11 px Lato, the longest), so it is 220 wide and the accents
        // button 170 (its longest words, STRONG's, 121 px)
        button(HudSize.label(),208,262,220,b->{HudSize.cycle();clearAndInit();});
        button("Minecraft sound levels",width-205,262,175,b->client.setScreen(new net.minecraft.client.gui.screen.option.SoundOptionsScreen(this,client.options)));
        button("Back to war council",30,height-53,220,b->client.setScreen(new GravesAndGloryClient.LobbyScreen()));
    }
    void refresh(){clearAndInit();}
    @Override public boolean mouseClicked(double x,double y,int b){return super.mouseClicked(x/scale,y/scale,b);}
    @Override public boolean mouseReleased(double x,double y,int b){return super.mouseReleased(x/scale,y/scale,b);}
    @Override public void render(DrawContext g,int mx,int my,float delta){
        g.getMatrices().push();g.getMatrices().scale(scale,scale,1);DarkUi.background(g,width,height);
        DarkUi.title(g,"THE LOOK OF WAR",30,25,DarkUi.GOLD);g.drawText(textRenderer,"VISUALS & SOUND / saved on your PC",30,46,DarkUi.MUTED,false);
        DarkUi.frame(g,20,73,395,127);g.drawText(textRenderer,"SHADER",30,80,DarkUi.GOLD,false);
        String[] desc={"Highest battle performance","Soft light / Minecraft character","Dramatic skies / reflections","Lighter cinematic lighting"};
        for(int i=0;i<4;i++)g.drawText(textRenderer,desc[i],30+i%2*190,120+i/2*55,DarkUi.MUTED,false);
        DarkUi.crest(g,width-145,91,48,GravesAndGloryClient.team==1);
        g.drawText(textRenderer,"One shader at a time.",width-192,158,DarkUi.IVORY,false);g.drawText(textRenderer,"Switch in the lobby.",width-192,174,DarkUi.MUTED,false);
        g.drawText(textRenderer,"WORLD TEXTURES / all include Fresh Animations",30,207,DarkUi.GOLD,false);
        g.drawText(textRenderer,"Accents confirm your hits. They never pause the battle.",30,289,DarkUi.MUTED,false);
        g.drawText(textRenderer,textRenderer.trimToWidth(Presentation.notice,width-60),30,height-24,DarkUi.GOLD,false);
        super.render(g,(int)(mx/scale),(int)(my/scale),delta);g.getMatrices().pop();
    }
    @Override public void close(){client.setScreen(new GravesAndGloryClient.LobbyScreen());}
}
