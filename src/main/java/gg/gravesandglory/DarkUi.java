package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

/** Original, code-drawn ironwork and heraldry; no reference-game art is copied. */
final class DarkUi {
    // UI spec tokens. On dark: ivory primary, muted secondary, brass headings and selection. On vellum: ink and sepia.
    static final int IVORY=0xffeee9df, PAPER=IVORY, MUTED=0xffafb9bc, BRASS=0xffc8a56b, GOLD=BRASS, BRASS_HI=0xffdfbd77, EDGE=0xff414b50;
    static final int INK_TEXT=0xff2a2219, SEPIA=0xff5c4a32, ALARM_TEXT=0xffe47a64, ROUT_TEXT=0xff5e140b, CHIP_TEXT=0xfff1e7cb;
    static final int DAWN=0xff5fb3ad, DUSK=0xffa07cc2, DUSK_TEXT=0xffb99ad6, VELLUM=0xffe6d9b8, CHARCOAL=0xff1a2125, OAK0=0xff160c0d;
    // The mockups' remaining inks: labels on brass plates, "Enter" on brass keys, keys on rail plaques, the ability's key,
    // the shadow under tags, and the cooldown sweep's veil, leading edge and twelve o'clock line.
    static final int PLATE_INK=0xff1e1509, KEY_INK=0xff3a2a12, RAIL_KEY=0xfff0d9a0, ABILITY_KEY=0xff2e1f0c, TAG_SHADOW=0xff120d16;
    static final int SWEEP_VEIL=0xcd15191c, SWEEP_EDGE=0xe6dfbd77, SWEEP_LINE=0xc8b08a4a;
    static final Identifier TITLE=new Identifier("gravesandglory","title"), FOCAL=new Identifier("gravesandglory","focal"), BODY=new Identifier("gravesandglory","body");
    /** The Lato body style: body and label draw in it, and BattleHud wraps the lesson in it. */
    static final Style BODY_STYLE=Style.EMPTY.withFont(BODY);
    /** The one 16 px Marcellus focal line of a screen. */
    static Text focal(String text){return Text.literal(text).setStyle(Style.EMPTY.withFont(FOCAL));}
    /** 11 px Marcellus, for headings and names. */
    static Text heading(String text){return Text.literal(text).setStyle(Style.EMPTY.withFont(TITLE));}
    /** Dawn's teal, Dusk's violet, muted for anyone else. Always paired with a shape (teamMark), never alone. */
    static int teamColor(int team){return team==0?DAWN:team==1?DUSK:MUTED;}
    private static final String[] SUN={"...x...",".x...x.","..xxx..","x.xxx.x","..xxx..",".x...x.","...x..."};
    private static final String[] CRESCENT={"..xxxx.",".xxx...","xxx....","xxx....","xxx....",".xxx...","..xxxx."};
    private static final String[] SQUARE={"xxxxxxx","x.....x","x.....x","x.....x","x.....x","x.....x","xxxxxxx"};
    /** A 7x7 team mark in the team colour: a sun for Dawn (0), a crescent for Dusk (1), a hollow square otherwise (spec 6.1: the diamond is Dusk's, the bare square is neutral). */
    static void teamMark(DrawContext g,int team,int x,int y){
        String[] rows=team==0?SUN:team==1?CRESCENT:SQUARE;int color=teamColor(team);
        for(int j=0;j<7;j++)for(int i=0;i<7;i++)if(rows[j].charAt(i)=='x')g.fill(x+i,y+j,x+i+1,y+j+1,color);
    }
    static void background(DrawContext g,int w,int h){
        // Keep the actual world visible behind a quiet, readable scrim.
        g.fill(0,0,w,h,0xd9101518);
        g.fill(0,0,w,68,0xf2171d20);
        g.fill(0,h-39,w,h,0xf2171d20);
        g.fill(0,67,w,68,0xff394246);
    }
    static void frame(DrawContext g,int x,int y,int w,int h){
        g.fill(x,y,x+w,y+h,0xee1a2125);
        g.drawBorder(x,y,w,h,0xff394246);
    }
    static Text body(String text){return Text.literal(text).setStyle(BODY_STYLE);}
    static void label(DrawContext g,String text,int x,int y,int color){
        g.drawText(MinecraftClient.getInstance().textRenderer,body(text),x,y,color,false);
    }
    static void model(DrawContext g,Spec.TroopsRow troop,int x,int bottom,int size){
        var entity=ArmyCards.portrait(troop);
        if(entity!=null)net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g,x,bottom,size,22,0,entity);
    }
    static void title(DrawContext g,String text,int x,int y,int color){
        g.getMatrices().push();g.getMatrices().translate(x,y,0);g.getMatrices().scale(1.35f,1.35f,1);g.drawText(MinecraftClient.getInstance().textRenderer,Text.literal(text).setStyle(Style.EMPTY.withFont(TITLE)),0,0,color,false);g.getMatrices().pop();
    }
    static void rule(DrawContext g,int x,int y,int w){g.fill(x,y,x+w,y+1,EDGE);}
    static void crest(DrawContext g,int x,int y,int size,boolean dusk){
        int col=dusk?0xff9e77ba:0xff70a8a4;
        g.fill(x,y,x+size,y+size*3/4,EDGE);
        for(int i=0;i<size/4;i++)g.fill(x+i,y+size*3/4+i,x+size-i,y+size*3/4+i+1,EDGE);
        g.fill(x+2,y+2,x+size-2,y+size*3/4,col);
        for(int i=2;i<size/4;i++)g.fill(x+i,y+size*3/4+i-2,x+size-i,y+size*3/4+i-1,col);
        g.fill(x+size/2-1,y+4,x+size/2+1,y+size-3,PAPER);
        g.fill(x+4,y+size/3,x+size-4,y+size/3+2,PAPER);
    }
    static void mapSeal(DrawContext g,int x,int y,int n){
        g.fill(x,y,x+24,y+20,0xff15171b);g.drawBorder(x,y,24,20,EDGE);
        if(n==0){g.fill(x+9,y+2,x+14,y+18,0xff426377);for(int z:new int[]{4,9,14})g.fill(x+3,y+z,x+21,y+z+2,0xffb59969);}
        if(n==1){g.fill(x+5,y+7,x+19,y+17,0xff78716c);for(int z:new int[]{4,10,16})g.fill(x+z,y+3,x+z+4,y+11,0xffa5967f);g.fill(x+10,y+12,x+14,y+18,0xff15171b);}
        if(n==2){for(int i=0;i<12;i++)g.fill(x+12-i/2,y+3+i,x+12+i/2+1,y+4+i,0xffb0babf);g.fill(x+11,y+11,x+13,y+19,0xff685d54);}
        if(n==3){for(int a:new int[]{3,10,17})for(int i=0;i<10;i++)g.fill(x+a+3-i/3,y+3+i,x+a+4+i/3,y+4+i,0xff426153);}
    }
    static final class MapChoice extends net.minecraft.client.gui.widget.ButtonWidget {
        final int index;
        MapChoice(int index,int x,int y,int w,PressAction action){super(x,y,w,39,Text.literal(Spec.MAPS[index].name()),action,DEFAULT_NARRATION_SUPPLIER);this.index=index;}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();frame(g,x,y,width,height);boolean selected=CampaignClient.map==index;
            if(selected||active&&(isHovered()||isFocused()))g.drawBorder(x+1,y+1,width-2,height-2,GOLD);
            mapSeal(g,x+6,y+5,index);var font=MinecraftClient.getInstance().textRenderer;
            g.drawTextWithShadow(font,font.trimToWidth(getMessage().getString(),width-44),x+36,y+9,active?PAPER:MUTED);
            String[] labels={"River town / three crossings","Fortress / supply roads","Snow / sheltered passes","Forest / old causeways","Plains / five points, two flanks"};g.drawText(font,font.trimToWidth(labels[index],width-12),x+6,y+28,MUTED,false);
        }
    }
}
