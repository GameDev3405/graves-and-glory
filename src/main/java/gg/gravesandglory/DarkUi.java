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
    /** The floor a canvas larger than the War Table shows round the planks: the kit's deepest iron (tools/ui_art/v4pal.IRON[0]). */
    static final int FLOOR=0xff0b0d0f;
    /** Accessibility cues (UI task 4F.5): the keyboard-focus ring's two dashes (brass and iron, so one shows on dark and on vellum) and the disabled veil's scanline. */
    static final int FOCUS_DASH=0xffdfbd77, FOCUS_GAP=0xff1a1410, DISABLED_LINE=0x55231a12, DISABLED_EDGE=0xff2c3438;
    /** A troop not yet recruited, as the muster roll's portraits show it: the flat silhouette the mock's portraits are painted in
     *  (tools/ui_art/v4a_portrait.py, sil). */
    static final int SILHOUETTE=0xff46555b;
    // The mockups' remaining inks: labels on brass plates, "Enter" on brass keys, keys on rail plaques, the ability's key,
    // the shadow under tags, and the cooldown sweep's veil, leading edge and twelve o'clock line.
    /** The command panel's gold and cream (CommandPanel.GOLD, CREAM) as the battle HUD draws them: the Formation aim's strip (its fill, the lit
     *  shape, the shape's name, the key words) and the group column's mouse washes (a card's formation box, a card or chip). */
    static final int AIM_STRIP_FILL=0xb31b120c, AIM_SHAPE_ON=0x4dffd27a, AIM_GOLD=0xffffd27a, AIM_CREAM=0xfffff6dc, COLUMN_HOVER=0x59ffd27a, COLUMN_WASH=0x38ffd27a;
    static final int PLATE_INK=0xff1e1509, KEY_INK=0xff3a2a12, RAIL_KEY=0xfff0d9a0, ABILITY_KEY=0xff2e1f0c, TAG_SHADOW=0xff120d16;
    static final int SWEEP_VEIL=0xcd15191c, SWEEP_EDGE=0xe6dfbd77, SWEEP_LINE=0xc8b08a4a;
    // The Chapter track's inks (work/ui-concepts/track, approved 2026-10-18): the Oath row's names on its sepia plaques and their
    // second lines, and "Enter" on the dull Claim plate's key.
    static final int SEPIA_L=0xffc4b28a, SEPIA_LINE=0xffb9a77f, DULL_KEY=0xffb3ad9f;
    // The battlefield map's inks: the dark pin under a point's mark, a group's pin, the contested ring, anything fighting, the
    // friendly troop marks, fresh and remembered enemy sightings, pings and the player.
    static final int MAP_PIN=0xcc101722, MAP_GROUP=0xff111923, CONTEST=0xffffaa55, WARN=0xffff9955, MAP_TROOP=0xfff5e2a8, MAP_ENEMY=0xffff6860, MAP_STALE=0xffa2a5aa, PING=0xffffcc55, MAP_YOU=0xffffffff;
    // Screen inks: the map screen's scrim, the entrance letterbox, the band over the battle preview and the chapter bar's track.
    static final int SCRIM=0xee0c1420, LETTERBOX=0xff080c13, PREVIEW_BAND=0xdd172126, TRACK=0xff29383c;
    /** Phase 2's troop page, cards and House tabs in CampScreen, under the UI standard (integration-0.5; the values Phase 2 drew): the rarity inks, the card's accent shade and fade, the dim of a troop not owned, the House tab fills and edge, and the opaque alpha a House accent takes. */
    static final int RARE_INK=0xff8fb8d8, EPIC_INK=0xffc79be0, CARD_SHADE=0x33000000, DROP_VEIL=0x77160c0d, DROP_DIM=0xaa160c0d, DROP_LIFT=0x66000000, CARD_FADE=0x00181820, CARD_DIM=0x99111119, TAB_CHOSEN=0xff30474a, TAB_FOCUS=0xff39464b, TAB_IDLE=0xff252f34, TAB_EDGE=0xff455156, OPAQUE=0xff000000, POPOVER_DIM=0x99000000;
    static final Identifier TITLE=new Identifier("gravesandglory","title"), FOCAL=new Identifier("gravesandglory","focal"), BODY=new Identifier("gravesandglory","body"), SMALL=new Identifier("gravesandglory","small");
    /** The Lato body style: body and label draw in it. */
    static final Style BODY_STYLE=Style.EMPTY.withFont(BODY);
    /** Lato at 11 px, the battle HUD's lesson note (UI Task 8c): BattleHud wraps the lesson in it. */
    static final Style SMALL_STYLE=Style.EMPTY.withFont(SMALL);
    static Text small(String text){return Text.literal(text).setStyle(SMALL_STYLE);}
    /** The one 16 px Marcellus focal line of a screen. */
    static Text focal(String text){return Text.literal(text).setStyle(Style.EMPTY.withFont(FOCAL));}
    /** 11 px Marcellus, for headings and names. */
    static Text heading(String text){return Text.literal(text).setStyle(Style.EMPTY.withFont(TITLE));}
    /** The factions row with this id; null for an id the sheet lacks. */
    private static Spec.FactionsRow faction(String id){for(var f:Spec.FACTIONS)if(f.id().equals(id))return f;return null;}
    /**
     * The side row a team flies; null for no team. While a battle runs the live state names it (GravesAndGloryClient.sideIds, state_v9);
     * in the camp, and for an id the sheet lacks, the camp snapshot does (CampaignClient.sides, which in the lobby previews the next round's sides).
     */
    static Spec.FactionsRow side(int team){
        if(team!=0&&team!=1)return null;
        Spec.FactionsRow row=GravesAndGloryClient.battleActive()?faction(GravesAndGloryClient.sideIds[team]):null;
        if(row==null)row=faction(CampaignClient.sides[team]);
        return row!=null?row:Spec.FACTIONS[team];
    }
    /** A team's side colour, opaque; muted for no team. Always paired with its glyph (teamMark), never alone. */
    /** Whether the keyboard, not the pointer, holds this widget's focus: the only time the focus ring draws (a hover and a click keep their own looks). */
    static boolean keyboardFocus(net.minecraft.client.gui.widget.ClickableWidget w){return w.isFocused()&&w.active&&w.visible&&MinecraftClient.getInstance().getNavigationType().isKeyboard();}
    /** The keyboard-focus ring (4F.5): a 1 px dashed ring 2 px outside the box, two px of brass and two of iron in turn, so it reads on dark plates and on vellum alike and apart from the hover look. */
    static void focusRing(DrawContext g,int x,int y,int w,int h){
        int x0=x-2,y0=y-2,x1=x+w+2,y1=y+h+2;
        for(int i=x0;i<x1;i++){int c=((i-x0)/2&1)==0?FOCUS_DASH:FOCUS_GAP;g.fill(i,y0,i+1,y0+1,c);g.fill(i,y1-1,i+1,y1,c);}
        for(int j=y0+1;j<y1-1;j++){int c=((j-y0)/2&1)==0?FOCUS_DASH:FOCUS_GAP;g.fill(x0,j,x0+1,j+1,c);g.fill(x1-1,j,x1,j+1,c);}
    }
    /** focusRing round a widget's own GUI box, when the keyboard holds its focus. */
    static void focus(DrawContext g,net.minecraft.client.gui.widget.ClickableWidget w){if(keyboardFocus(w))focusRing(g,w.getX(),w.getY(),w.getWidth(),w.getHeight());}
    /** The disabled veil (4F.5): a scanline over every other row of a box, so a plate that cannot be pressed reads as dimmed by its texture, not by a shade of ink alone. */
    static void disabledVeil(DrawContext g,int x,int y,int w,int h){for(int j=y;j<y+h;j+=2)g.fill(x,j,x+w,j+1,DISABLED_LINE);}
    /** A House accent (rgb) as an opaque colour. */
    static int opaque(int rgb){return 0xff000000|rgb;}
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
    /** The width houseMark draws for a tier: the 16 px glyph, a gap and the numeral. */
    static int houseMarkWidth(int tier){return 19+MinecraftClient.getInstance().textRenderer.getWidth(MatchStats.roman(tier));}
    /** A House's 16x16 glyph (houses.glyph) with its trait tier in Roman numerals beside it, in the accent's text ink (UiMath.accentText). Nothing is drawn for a House the sheet lacks. */
    static void houseMark(DrawContext g,String house,int tier,int x,int y){
        Spec.HousesRow row=null;for(var h:Spec.HOUSES)if(h.id().equals(house))row=h;if(row==null)return;
        g.drawTexture(new Identifier(row.glyph()),x,y,0f,0f,16,16,16,16);
        g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,MatchStats.roman(tier),x+19,y+4,UiMath.accentText(row.accent()));
    }
    private static final String[] DOT={".xxx.","xxxxx","xxxxx","xxxxx",".xxx."};
    private static final String[] RING={".xxx.","x...x","x...x","x...x",".xxx."};
    private static final String[] CROSS={"x...x",".x.x.","..x..",".x.x.","x...x"};
    private static final String[] SMALL_CROSS={".....",".x.x.","..x..",".x.x.","....."};
    /** A 5x5 map mark centred on (x, y), its shape the meaning (spec 6.1): "own" a filled dot (your troops), "ally" a ring (allied
     *  troops), "enemy" an x (an enemy seen now), "enemy-old" a small x (an enemy remembered). The colour shows only a state. */
    static void mapMark(DrawContext g,String kind,int x,int y,int color){
        String[] rows=switch(kind){case "own"->DOT;case "ally"->RING;case "enemy-old"->SMALL_CROSS;default->CROSS;};
        for(int j=0;j<5;j++)for(int i=0;i<5;i++)if(rows[j].charAt(i)=='x')g.fill(x-2+i,y-2+j,x-1+i,y-1+j,color);
    }
    static void background(DrawContext g,int w,int h){
        // Keep the actual world visible behind a quiet, readable scrim.
        g.fill(0,0,w,h,0xd9101518);
        g.fill(0,0,w,68,0xf2171d20);
        g.fill(0,h-39,w,h,0xf2171d20);
        g.fill(0,67,w,68,0xff394246);
    }
    static void frame(DrawContext g,int x,int y,int w,int h){
        g.fill(x,y,x+w,y+h,UiMath.PANEL_FILL);
        g.drawBorder(x,y,w,h,0xff394246);
    }
    static Text body(String text){return Text.literal(text).setStyle(BODY_STYLE);}
    static void label(DrawContext g,String text,int x,int y,int color){
        g.drawText(MinecraftClient.getInstance().textRenderer,body(text),x,y,color,false);
    }
    static void model(DrawContext g,Spec.TroopsRow troop,int x,int bottom,int size){
        var entity=ArmyCards.portrait(troop);
        if(entity!=null)Portrait.full(g,entity,x,bottom,size); // Portrait.full sizes by the head and the width, once
    }
    static void title(DrawContext g,String text,int x,int y,int color){
        g.getMatrices().push();g.getMatrices().translate(x,y,0);g.getMatrices().scale(1.35f,1.35f,1);g.drawText(MinecraftClient.getInstance().textRenderer,Text.literal(text).setStyle(Style.EMPTY.withFont(TITLE)),0,0,color,false);g.getMatrices().pop();
    }
    /** A title at 1.35 scale, or smaller when its words would pass maxW. */
    static void titleFit(DrawContext g,String text,int x,int y,int color,int maxW){
        var tr=MinecraftClient.getInstance().textRenderer;float w=tr.getWidth(Text.literal(text).setStyle(Style.EMPTY.withFont(TITLE))),sc=Math.min(1.35f,w<=0?1.35f:maxW/w);
        g.getMatrices().push();g.getMatrices().translate(x,y,0);g.getMatrices().scale(sc,sc,1);g.drawText(tr,Text.literal(text).setStyle(Style.EMPTY.withFont(TITLE)),0,0,color,false);g.getMatrices().pop();
    }
    static void rule(DrawContext g,int x,int y,int w){g.fill(x,y,x+w,y+1,EDGE);}
    static void crest(DrawContext g,int x,int y,int size,boolean dusk){
        int col=dusk?0xff9e77ba:0xff70a8a4;
        g.fill(x,y,x+size,y+size*3/4,EDGE);
        for(int i=0;i<size/4;i++)g.fill(x+i,y+size*3/4+i,x+size-i,y+size*3/4+i+1,EDGE);
        g.fill(x+2,y+2,x+size-2,y+size*3/4,col);
        for(int i=2;i<size/4;i++)g.fill(x+i,y+size*3/4+i-2,x+size-i,y+size*3/4+i-1,col);
        if(dusk){ // a saltire for the dusk side, a cross for dawn: the shape tells the sides apart as the colour does (4F.5)
            int span=size-9;for(int i=0;i<=span;i++){int px=x+4+i,py=y+4+i*(size*3/4-6)/Math.max(1,span);g.fill(px,py,px+2,py+2,PAPER);int qx=x+size-6-i;g.fill(qx,py,qx+2,py+2,PAPER);}
        }else{
            g.fill(x+size/2-1,y+4,x+size/2+1,y+size-3,PAPER);
            g.fill(x+4,y+size/3,x+size-4,y+size/3+2,PAPER);
        }
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
            focus(g,this);
            mapSeal(g,x+6,y+5,index);var font=MinecraftClient.getInstance().textRenderer;
            g.drawTextWithShadow(font,font.trimToWidth(getMessage().getString(),width-44),x+36,y+9,active?PAPER:MUTED);
            String[] labels={"River town / three crossings","Fortress / supply roads","Snow / sheltered passes","Forest / old causeways","Plains / five points, two flanks"};g.drawText(font,font.trimToWidth(labels[index],width-12),x+6,y+28,MUTED,false);
        }
    }
}
