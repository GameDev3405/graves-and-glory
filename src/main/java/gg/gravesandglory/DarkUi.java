package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

/** Original, code-drawn ironwork and heraldry; no reference-game art is copied. */
final class DarkUi {
    static final int GOLD=0xffc8a56b, EDGE=0xff414b50, PAPER=0xffeee9df, MUTED=0xffafb9bc;
    static final Identifier TITLE=new Identifier("gravesandglory","title");
    /** The side row a team flies (CampaignClient.sides, from the camp snapshot); null for no team. */
    static Spec.FactionsRow side(int team){
        if(team!=0&&team!=1)return null;String id=CampaignClient.sides[team];
        for(var f:Spec.FACTIONS)if(f.id().equals(id))return f;return Spec.FACTIONS[team];
    }
    /** A team's side colour, opaque; muted for no team. Always paired with its glyph (teamMark), never alone. */
    static int teamColor(int team){var f=side(team);return f==null?MUTED:0xff000000|f.color();}
    /** A team's side in one word, the first of its name (Gold, Violet, Crimson, Teal); Neutral for no team. */
    static String teamText(int team){var f=side(team);if(f==null)return "Neutral";int space=f.name().indexOf(' ');return space<0?f.name():f.name().substring(0,space);}
    private static final String[] SUN={"...x...",".x...x.","..xxx..","x.xxx.x","..xxx..",".x...x.","...x..."};
    private static final String[] MOON={"..xxxx.",".xxx...","xxx....","xxx....","xxx....",".xxx...","..xxxx."};
    private static final String[] STAR={"...x...","...x...","xxxxxxx",".xxxxx.","..xxx..",".xx.xx.","xx...xx"};
    private static final String[] WAVE={".......",".xx..x.","x..xx.x",".......",".xx..x.","x..xx.x","......."};
    private static final String[] SQUARE={"xxxxxxx","x.....x","x.....x","x.....x","x.....x","x.....x","xxxxxxx"};
    /** A 7x7 mark in the team's side colour: its factions.glyph (sun, moon, star or wave); a hollow square for no team. */
    static void teamMark(DrawContext g,int team,int x,int y){sideMark(g,side(team),x,y);}
    /** The 7x7 glyph of a side row in its colour; a muted hollow square for null. */
    static void sideMark(DrawContext g,Spec.FactionsRow f,int x,int y){
        String[] rows=f==null?SQUARE:switch(f.glyph()){case "sun"->SUN;case "moon"->MOON;case "star"->STAR;case "wave"->WAVE;default->SQUARE;};int color=f==null?MUTED:0xff000000|f.color();
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
    static Text body(String text){return Text.literal(text).setStyle(Style.EMPTY.withFont(new Identifier("gravesandglory","body")));}
    static void label(DrawContext g,String text,int x,int y,int color){
        g.drawText(MinecraftClient.getInstance().textRenderer,body(text),x,y,color,false);
    }
    static void model(DrawContext g,Spec.TroopsRow troop,int x,int bottom,int size){
        var entity=ArmyCards.portrait(troop);
        if(entity!=null)net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g,x,bottom,ArmyCards.size(entity,size),22,0,entity);
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
