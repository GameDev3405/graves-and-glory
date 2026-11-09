package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class PresentationScreen extends GravesAndGloryClient.ArmyScreen {
    float scale=1;int selected;
    /** The screen that opened this one (the lobby or the camp's pause page), which Back and Esc return to; null opens the war council. */
    private final net.minecraft.client.gui.screen.Screen parent;
    /** Opened from whatever screen is showing now. */
    PresentationScreen(){this(net.minecraft.client.MinecraftClient.getInstance().currentScreen);}
    PresentationScreen(net.minecraft.client.gui.screen.Screen parent){super("Visuals and sound");this.parent=parent instanceof PresentationScreen?null:parent;}
    private void leave(){client.setScreen(parent!=null?parent:new GravesAndGloryClient.LobbyScreen());}
    @Override protected void init(){
        Presentation.load();selected=Presentation.shader();scale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/640f,client.getWindow().getScaledHeight()/360f));width=(int)(client.getWindow().getScaledWidth()/scale);height=(int)(client.getWindow().getScaledHeight()/scale);
        for(int i=0;i<4;i++){final int n=i;button((i==selected?"> ":"")+Presentation.SHADERS[i],30+i%2*190,94+i/2*55,180,b->{Presentation.selectShader(n);clearAndInit();});}
        button("Impact accents: "+new String[]{"OFF","SUBTLE","STRONG"}[Presentation.impact],30,222,170,b->{Presentation.impact=(Presentation.impact+1)%3;Presentation.save();clearAndInit();});
        // UI Task 8c: Compact, Standard, Large. Fix round 1: the button names the size this window draws the choice at where that is
        // smaller (UI Task 8b: "Battle HUD: Standard (Compact scale here)", 206 px of 11 px Lato, the longest), so it is 222 wide
        // (2 px to spare inside FantasyButton's 14 px of padding; 5 px clear of the sound button on the narrowest, 640 px, screen)
        // and the accents button 170 (its longest words, STRONG's, 121 px)
        button(HudSize.label(),208,222,222,b->{HudSize.cycle();clearAndInit();});
        button("Minecraft sound levels",width-205,222,175,b->client.setScreen(new net.minecraft.client.gui.screen.option.SoundOptionsScreen(this,client.options)));
        button("Troop depth: "+(Presentation.troopDepth?"ON":"OFF"),30,252,145,b->{Presentation.troopDepth=!Presentation.troopDepth;Presentation.save();clearAndInit();});
        button(parent==null||parent instanceof CampaignLobbyScreen?"Back to war council":"Back",30,height-53,220,b->leave());
    }
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
        g.drawText(textRenderer,"Accents confirm your hits. They never pause the battle.",30,279,DarkUi.MUTED,false);
        g.drawText(textRenderer,"Troop depth raises the armour detail of troops near you.",30,291,DarkUi.MUTED,false);
        g.drawText(textRenderer,textRenderer.trimToWidth(Presentation.notice,width-60),30,height-24,DarkUi.GOLD,false);
        super.render(g,(int)(mx/scale),(int)(my/scale),delta);g.getMatrices().pop();
    }
    @Override public void close(){leave();}
}
