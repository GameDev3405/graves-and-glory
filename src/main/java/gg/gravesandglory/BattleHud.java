package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import java.util.*;

/** The Iron Ledger battle HUD (UI spec 4.5, mock v4-hud-iron-ledger): the score bar between the two bosses, the point seals hanging
 *  from it, the group cards on their iron spine, the commander's ability slot beside the hotbar, and the lesson and respawn cards.
 *  All but the ability slot is drawn on the UiCanvas; the slot sits in vanilla GUI space at the player's GUI Scale, next to the
 *  hotbar. Colour never carries a meaning alone (spec 6.1): seals carry their owner's emblem, a routing card its stamp and grey
 *  card, a fallen one its word. The event feed hangs at the right edge; the hint layer (altLayer) puts every key row on the cards
 *  and the hint slip above them. What the last frame drew is kept for checks: cards(), seals(), rects(), texts(), hintRows(),
 *  hintEnds() and keyRows(). */
public final class BattleHud {
    private BattleHud() {}

    /** A card the last frame drew: its group, its state (PLAIN, FLASH, SELECTED, ROUTING or FALLEN), whether it carried the key
     *  row, and its box in canvas px. */
    public record CardDraw(String group,String state,boolean tall,int x,int y,int w,int h){}
    /** A seal the last frame drew: its sprite and its place in canvas px. */
    public record SealDraw(String sprite,int x,int y){}
    /** A tall card's key row the last frame drew: its group, its chips and words in order, and the canvas x where each starts and
     *  where it ends. */
    public record KeyRowDraw(String group,List<String> parts,List<Integer> from,List<Integer> to){}

    /** Food icons HungerHudMixin skipped. It lives here because a mixin class cannot be referenced at runtime. */
    public static int foodSkipped;
    /** Frames the battle HUD has drawn. */
    public static int frames;
    /** Times the lesson copy was wrapped: once per new copy or style, and once more after a resource reload. */
    public static int wraps;
    /** How long a round's start shows every key hint (UI spec 4.5: its first 60 s), a presentation timing. */
    public static final long HINT_LAYER_MS=60000;
    /** The client clock (ms) when running last turned true; the hint layer shows until HINT_LAYER_MS after it. The STATE receiver
     *  sets it, and a check may move it back. */
    public static long roundShownMs;

    // What the last frame drew: filled in place every frame, read through the views below.
    private static final List<CardDraw> cardsDrawn=new ArrayList<>();
    private static final List<SealDraw> sealsDrawn=new ArrayList<>();
    private static final Map<String,int[]> rectsDrawn=new LinkedHashMap<>();
    private static final Map<String,String> textsDrawn=new LinkedHashMap<>();
    private static final List<List<String>> hintRowsDrawn=new ArrayList<>();
    private static final List<Integer> hintEndsDrawn=new ArrayList<>();
    private static final List<KeyRowDraw> keyRowsDrawn=new ArrayList<>();
    private static final List<CardDraw> cards=Collections.unmodifiableList(cardsDrawn);
    private static final List<SealDraw> seals=Collections.unmodifiableList(sealsDrawn);
    private static final Map<String,int[]> rects=Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String,String> texts=Collections.unmodifiableMap(textsDrawn);
    private static final List<List<String>> hintRows=Collections.unmodifiableList(hintRowsDrawn);
    private static final List<Integer> hintEnds=Collections.unmodifiableList(hintEndsDrawn);
    private static final List<KeyRowDraw> keyRows=Collections.unmodifiableList(keyRowsDrawn);
    /** Set when a frame named a texture the atlas lacks: the HUD stays off, and the client up, until the next resource reload. */
    private static boolean stopped;
    // The lesson copy last wrapped, its style and its lines.
    private static String wrappedCopy;private static Style wrappedStyle;private static List<String> wrappedLines=List.of();private static String wrappedJoined="";

    /** The cards the last frame drew. This and the three below are read-only views that the next frame refills: copy one to keep it. */
    public static List<CardDraw> cards(){return cards;}
    public static List<SealDraw> seals(){return seals;}
    /** Each element's box {x,y,w,h} in canvas px, keyed score, dawn-boss, dusk-boss, seal-(i), card-(group), spine, hints (the hint
     *  slip), lesson, respawn, feed (the feed slip, while a line lives), aim-name (the aimed point's name, while an order is aimed into
     *  a point) and ability (the slot converted from GUI px, rounded outward). */
    public static Map<String,int[]> rects(){return rects;}
    /** The words and numbers the last frame drew: score-dawn, clock, score-dusk, count-(group), state-(group), lesson, respawn,
     *  feed-(row) (each feed line as trimmed, rows 0 to 4 from the top), aim-name, ability, ability-seconds and ability-key. */
    public static Map<String,String> texts(){return texts;}
    /** The chip and word texts of each row the hint slip last drew, in order; empty while the hint layer is down. */
    public static List<List<String>> hintRows(){return hintRows;}
    /** The canvas x where each row of the hint slip ends (its last chip or word), as the last frame drew it; empty while the hint
     *  layer is down. */
    public static List<Integer> hintEnds(){return hintEnds;}
    /** The key rows the tall cards last drew, in drawing order; empty while no card is tall. */
    public static List<KeyRowDraw> keyRows(){return keyRows;}
    /** The hint layer is up: the hints key held, the round's first HINT_LAYER_MS, or an order being aimed. Every living card then
     *  carries its key row, and the hint slip above the cards names the other keys. */
    public static boolean altLayer(){
        var k=GravesAndGloryClient.hints;
        return k!=null&&k.isPressed()||Util.getMeasuringTimeMs()-roundShownMs<HINT_LAYER_MS||CommandMode.active();
    }
    /** A frame without the ledger (camp, a battle's entrance, a menu over the HUD): nothing reads as drawn. */
    static void clear(){cardsDrawn.clear();sealsDrawn.clear();rectsDrawn.clear();textsDrawn.clear();hintRowsDrawn.clear();hintEndsDrawn.clear();keyRowsDrawn.clear();}
    /** A disconnect (GravesAndGloryClient's handler, on the client thread): the last battle's records go with it, since no HUD frame
     *  runs without a world to clear them, and ChatPlacement.lift reads rects() before the next world's first frame. That is what
     *  the frame drew, the lesson wrap and the lines drawn from it, and the hint layer's round clock. */
    static void reset(){clear();wrappedCopy=null;wrappedStyle=null;wrappedLines=List.of();wrappedJoined="";roundShownMs=0;GravesAndGloryClient.lessonDrawn=List.of();}
    /** The lesson copy the cached wrap was made for; null after a reset or a resource reload. */
    static String wrappedFor(){return wrappedCopy;}

    /** At every resource reload (fonts and the atlas can change there): wrap the lesson again, and try the HUD again after a texture
     *  stop. GravesAndGloryClient registers it. */
    public static final SimpleSynchronousResourceReloadListener RELOADER=new SimpleSynchronousResourceReloadListener(){
        @Override public Identifier getFabricId(){return new Identifier("gravesandglory","battle_hud");}
        @Override public void reload(ResourceManager manager){wrappedCopy=null;stopped=false;}
    };

    public static void render(DrawContext g,float delta){
        clear();
        if(stopped)return;
        var c=MinecraftClient.getInstance();var font=c.textRenderer;
        try{
            UiCanvas.begin(g);
            try{
                int w=UiCanvas.width(),h=UiCanvas.height(),cx=w/2;
                ledger(g,font,cx,rectsDrawn,textsDrawn);
                seals(g,font,w,rectsDrawn,sealsDrawn,textsDrawn);
                cards(g,font,h,rectsDrawn,cardsDrawn,textsDrawn);
                lesson(g,font,cx,rectsDrawn,textsDrawn);
                respawn(g,font,cx,h,rectsDrawn,textsDrawn);
                feed(g,font,w,h,rectsDrawn,textsDrawn);
            }finally{UiCanvas.end(g);}
            ability(g,c,font,rectsDrawn,textsDrawn);
        }catch(IllegalArgumentException e){ // UiTex.entry names a texture the atlas lacks: a resource pack's atlas, or none read at all
            stopped=true;clear();
            System.err.println("[Graves and Glory] The battle HUD stopped: "+e.getMessage()+". It stays off until the next resource reload.");
            return;
        }
        frames++;
    }

    /** The x that centres an 8 px text on cx as the mock does: the text's box is its width plus one, the shadow's column. */
    static int centred(TextRenderer font,String s,int cx){return (int)Math.round(cx-(font.getWidth(s)+1)/2.0);}

    /** The score bar: Dawn's score, its track, the round clock, Dusk's track and score, centred between the Dawn and Dusk bosses. */
    static void ledger(DrawContext g,TextRenderer font,int cx,Map<String,int[]> r,Map<String,String> tx){
        String dawn=String.valueOf(GravesAndGloryClient.dawn),clock=UiMath.clock(CommandMode.seconds),dusk=String.valueOf(GravesAndGloryClient.dusk);
        int win=GravesAndGlory.R.winScore(),wa=font.getWidth(dawn),wc=font.getWidth(clock),wd=font.getWidth(dusk);
        UiTex.draw(g,"hud/score-card",cx-128,13);
        // The row as the mock lays it out: each 8 px text takes its width plus one, with 6 px gaps round the two 56 px tracks.
        int x=(int)Math.round(cx-(wa+1+wc+1+wd+1+2*56+4*6)/2.0);
        g.drawText(font,dawn,x,21,DarkUi.INK_TEXT,false);x+=wa+1+6;
        UiTex.draw(g,"hud/track-empty",x,21);UiTex.bar(g,"hud/track-dawn-fill",x+1,23,UiMath.trackFill(GravesAndGloryClient.dawn,win),false);x+=56+6;
        g.drawText(font,clock,x,21,DarkUi.INK_TEXT,false);x+=wc+1+6;
        UiTex.draw(g,"hud/track-empty",x,21);UiTex.bar(g,"hud/track-dusk-fill",x+1,23,UiMath.trackFill(GravesAndGloryClient.dusk,win),true);x+=56+6;
        g.drawText(font,dusk,x,21,DarkUi.INK_TEXT,false);
        UiTex.draw(g,"hud/boss-dawn",cx-140,12);UiTex.draw(g,"hud/boss-dusk",cx+115,12);
        r.put("score",new int[]{cx-128,13,256,24});r.put("dawn-boss",new int[]{cx-140,12,27,28});r.put("dusk-boss",new int[]{cx+115,12,27,28});
        tx.put("score-dawn",dawn);tx.put("clock",clock);tx.put("score-dusk",dusk);
    }

    /** The top of the aimed point's name: the seal rects' foot (each seal's box runs from its chain's top at 37 down 31 px), so the
     *  name clears every seal. */
    static final int AIM_NAME_Y=68;

    /** The points hang from the bar on a 28 px pitch: chain, seal (its owner's, or split while contested) and letter; while an order
     *  is aimed into a point's radius, that point's name hangs below its seal. */
    static void seals(DrawContext g,TextRenderer font,int w,Map<String,int[]> r,List<SealDraw> sd,Map<String,String> tx){
        int n=GravesAndGloryClient.owners.length,aim=CommandMode.aimPoint();var pts=Spec.POINTS;
        for(int i=0;i<n;i++){
            int sc=UiMath.sealCentre(i,n,w),owner=GravesAndGloryClient.ownerAt(i);boolean contested=GravesAndGloryClient.contestedAt(i);
            int y=contested?40:41;String sprite;
            UiTex.draw(g,"hud/chain",sc-2,37);
            if(contested){sprite="hud/seal-contested";UiTex.draw(g,"hud/seal-contested",sc-13,y);}
            else if(owner==0){sprite="hud/seal-dawn";UiTex.draw(g,"hud/seal-dawn",sc-13,y);}
            else if(owner==1){sprite="hud/seal-dusk";UiTex.draw(g,"hud/seal-dusk",sc-13,y);}
            else{sprite="hud/seal-neutral";UiTex.draw(g,"hud/seal-neutral",sc-13,y);}
            String id=i<pts.length?pts[i].id():String.valueOf((char)('A'+i));
            g.drawText(font,id,centred(font,id,sc),contested?49:50,DarkUi.IVORY,true);
            if(aim==i&&i<pts.length){String name=pts[i].name();int nx=centred(font,name,sc);g.drawText(font,name,nx,AIM_NAME_Y,DarkUi.IVORY,true);tx.put("aim-name",name);r.put("aim-name",new int[]{nx,AIM_NAME_Y,font.getWidth(name)+1,9});}
            sd.add(new SealDraw(sprite,sc-13,y));r.put("seal-"+i,new int[]{sc-13,37,27,31});
        }
    }

    /** The group cards, stacked up from 18 px above the canvas bottom on the iron spine, infantry first. A group the round never
     *  fielded has no card. A card that carries the key row (the selected group's, or the bottom one while the whole army is
     *  selected; in the hint layer every card with troops left) is 62 px tall, the rest 48. In the hint layer the hint slip hangs
     *  above the top card (above the stack's foot when there is no card). */
    static void cards(DrawContext g,TextRenderer font,int h,Map<String,int[]> r,List<CardDraw> cd,Map<String,String> tx){
        String sel=CommandMode.selected;boolean army=sel.equals("all"),alt=altLayer();
        List<CommandMode.Group> shown=new ArrayList<>();
        for(String id:Morale.GROUPS)for(var grp:CommandMode.groups)if(grp.id().equals(id)&&grp.full()>0){shown.add(grp);break;}
        if(shown.isEmpty()){if(alt)hintSlip(g,font,h-18,r);return;}
        int n=shown.size();int[] heights=new int[n];
        for(int i=0;i<n;i++)heights[i]=(army?i==n-1:sel.equals(shown.get(i).id()))||alt&&shown.get(i).count()>0?62:48;
        int[] tops=UiMath.cardTops(h,heights);
        int first=tops[0],last=tops[n-1]+heights[n-1];
        UiTex.three(g,"hud/spine",12,first-4,last-first+8);r.put("spine",new int[]{12,first-4,9,last-first+8});
        for(int i=0;i<n;i++)card(g,font,shown.get(i),24,tops[i],heights[i]==62,army||sel.equals(shown.get(i).id()),r,cd,tx);
        if(alt)hintSlip(g,font,first,r);
    }

    /** How far a hint-slip row may run: from 10 px into the slip to 8 px inside the right edge of its paper, which the slip's PNG
     *  paints from x 2 to 172. */
    static final int HINT_ROW_W=154;
    /** The words on a tall card's key row, and the short ones its row takes where long key names crowd it (UiMath.cardRow). */
    static final List<String> CARD_WORDS=List.of("Attack","Follow","Formation"),CARD_SHORT_WORDS=List.of("Attack","Follow","Form");

    /** The hint slip, its box 48 px above firstTop: row one names Map and Commands (Confirm and Cancel while an order is aimed), row
     *  two the facing keys and the hints key. Each key is an ink chip naming the player's binding, a mouse button as LMB, RMB or MMB.
     *  Where long names would run a row past the paper, they give way alike (UiMath.fitKeys): nothing runs past it. */
    static void hintSlip(DrawContext g,TextRenderer font,int firstTop,Map<String,int[]> r){
        int x=24,y=firstTop-48;boolean aiming=CommandMode.active();
        UiTex.draw(g,"hud/notice-slip",x,y);
        String[][] ids={aiming?new String[]{"confirm","cancel"}:new String[]{"map","menu"},{"rotate_left","rotate_right","hints"}};
        String[][] words={aiming?new String[]{"Confirm","Cancel"}:new String[]{"Map","Commands"},{null,"Turn","Hints"}};
        for(int row=0;row<2;row++){
            List<String> keys=new ArrayList<>();for(String id:ids[row])keys.add(GravesAndGloryClient.hint(id));
            keys=UiMath.fitKeys(keys,Arrays.asList(words[row]),HINT_ROW_W,font::getWidth);
            List<String> drawn=new ArrayList<>();int kx=x+10,ky=y+(row==0?7:22),end=kx;
            for(int k=0;k<keys.size();k++){
                String key=keys.get(k);int kw=UiMath.chipWidth(font.getWidth(key));
                UiTex.three(g,"hud/key-chip",kx,ky,kw);g.drawText(font,key,kx+2,ky+2,DarkUi.CHIP_TEXT,false);drawn.add(key);end=kx+kw;kx+=kw+2;
                String word=words[row][k];
                if(word!=null){g.drawText(font,word,kx,ky+2,DarkUi.SEPIA,false);drawn.add(word);end=kx+font.getWidth(word);kx+=font.getWidth(word)+8;}
            }
            hintRowsDrawn.add(List.copyOf(drawn));hintEndsDrawn.add(end);
        }
        r.put("hints",new int[]{x,y,176,40});
    }

    /** One card. State, first match wins: FALLEN (none left of a fielded group), ROUTING, FLASH (a loss in the last 450 ms),
     *  SELECTED, PLAIN. The chosen group's card (every card while the army is selected) names its order and formation beside the glyphs. */
    static void card(DrawContext g,TextRenderer font,CommandMode.Group grp,int x,int y,boolean tall,boolean chosen,Map<String,int[]> r,List<CardDraw> cd,Map<String,String> tx){
        String id=grp.id();
        String state=grp.count()==0?"FALLEN":grp.routing()?"ROUTING":CommandMode.flashing(id)?"FLASH":chosen?"SELECTED":"PLAIN";
        boolean down=state.equals("ROUTING")||state.equals("FALLEN");
        switch(state){
            case "PLAIN"->{if(tall)UiTex.draw(g,"hud/card-plain-62",x,y);else UiTex.draw(g,"hud/card-plain",x,y);}
            case "FLASH"->{if(tall)UiTex.draw(g,"hud/card-flash-62",x,y);else UiTex.draw(g,"hud/card-flash",x,y);}
            case "SELECTED"->{if(tall)UiTex.draw(g,"hud/card-sel",x,y);else UiTex.draw(g,"hud/card-sel-48",x,y);}
            default->{if(tall)UiTex.draw(g,"hud/card-rout-62",x,y);else UiTex.draw(g,"hud/card-rout",x,y);}
        }
        if(chosen)UiTex.draw(g,"hud/hook-brass",x-7,y+3);else UiTex.draw(g,"hud/hook",x-7,y+3);
        g.drawText(font,DarkUi.heading(groupLabel(id)),x+10,y+7,DarkUi.INK_TEXT,false);
        // The state, right-aligned to x 166: the rout stamp, or a word (the fight glyph before Fighting).
        String word;
        if(state.equals("ROUTING")){word="ROUTING";UiTex.draw(g,"hud/stamp-rout",x+108,y+4);g.drawText(font,word,centred(font,word,x+135),y+8,DarkUi.ROUT_TEXT,false);}
        else{
            word=state.equals("FALLEN")?"Fallen":grp.fighting()?"Fighting":"Steady";Text t=DarkUi.body(word);int wx=x+166-font.getWidth(t);
            g.drawText(font,t,wx,y+7,word.equals("Fighting")?DarkUi.INK_TEXT:DarkUi.SEPIA,false);
            if(word.equals("Fighting"))UiTex.draw(g,"hud/g-fight",wx-3-9,y+9);
        }
        // Strength in its channel; during a flash the stretch just lost shows white, from the strength now to the strength before.
        if(down)UiTex.draw(g,"hud/hp-channel-grey",x+10,y+22);else UiTex.draw(g,"hud/hp-channel",x+10,y+22);
        int bar=UiMath.barWidth(grp.strength());
        if(state.equals("ROUTING"))UiTex.bar(g,"hud/hp-rout",x+11,y+23,bar,false);
        else if(!state.equals("FALLEN")){if(GravesAndGloryClient.team==1)UiTex.bar(g,"hud/hp-full-dusk",x+11,y+23,bar,false);else UiTex.bar(g,"hud/hp-full-dawn",x+11,y+23,bar,false);}
        if(state.equals("FLASH"))UiTex.bar(g,"hud/hp-lost",x+11+bar,y+23,UiMath.barWidth(CommandMode.lostFrom(id))-bar,false);
        String count=UiMath.countText(grp.count(),grp.full());
        g.drawText(font,count,x+166-font.getWidth(count)-1,y+21,down?DarkUi.ROUT_TEXT:DarkUi.INK_TEXT,false);
        orderGlyph(g,grp.order(),down,x+10,y+30);
        if(chosen){
            g.drawText(font,orderLabel(grp.order()),x+22,y+31,DarkUi.INK_TEXT,false);
            formationGlyph(g,grp.formation(),down,x+72,y+30);
            g.drawText(font,formationLabel(grp.formation()),x+84,y+31,DarkUi.INK_TEXT,false);
        }else formationGlyph(g,grp.formation(),down,x+25,y+30);
        if(tall){ // the order keys, each in an ink chip as wide as its key's name: at the mock's places, or where long names crowd the row, flowed and fitted as on the hint slip (UiMath.cardRow)
            var row=UiMath.cardRow(List.of(GravesAndGloryClient.hint("attack"),GravesAndGloryClient.hint("follow"),GravesAndGloryClient.hint("formation")),CARD_WORDS,CARD_SHORT_WORDS,font::getWidth);
            List<String> parts=new ArrayList<>();List<Integer> from=new ArrayList<>(),to=new ArrayList<>();
            for(int k=0;k<3;k++){
                String key=row.keys().get(k),label=row.words().get(k);int kw=UiMath.chipWidth(font.getWidth(key)),kx=x+row.at().get(k);
                UiTex.three(g,"hud/key-chip",kx,y+44,kw);
                g.drawText(font,key,kx+2,y+46,DarkUi.CHIP_TEXT,false);
                g.drawText(font,label,kx+kw+2,y+46,DarkUi.SEPIA,false);
                parts.add(key);from.add(kx);to.add(kx+kw);parts.add(label);from.add(kx+kw+2);to.add(kx+kw+2+font.getWidth(label));
            }
            keyRowsDrawn.add(new KeyRowDraw(id,List.copyOf(parts),List.copyOf(from),List.copyOf(to)));
        }
        int ch=tall?62:48;cd.add(new CardDraw(id,state,tall,x,y,176,ch));r.put("card-"+id,new int[]{x,y,176,ch});
        tx.put("count-"+id,count);tx.put("state-"+id,word);
    }

    static void orderGlyph(DrawContext g,String order,boolean faded,int x,int y){
        if(faded)switch(order){
            case "follow"->UiTex.draw(g,"hud/g-follow-faded",x,y);
            case "move"->UiTex.draw(g,"hud/g-move-faded",x,y);
            case "attack"->UiTex.draw(g,"hud/g-attack-faded",x,y);
            case "hold"->UiTex.draw(g,"hud/g-hold-faded",x,y);
            case "charge"->UiTex.draw(g,"hud/g-charge-faded",x,y);
            case "retreat"->UiTex.draw(g,"hud/g-retreat-faded",x,y);
            default->{}
        }else switch(order){
            case "follow"->UiTex.draw(g,"hud/g-follow",x,y);
            case "move"->UiTex.draw(g,"hud/g-move",x,y);
            case "attack"->UiTex.draw(g,"hud/g-attack",x,y);
            case "hold"->UiTex.draw(g,"hud/g-hold",x,y);
            case "charge"->UiTex.draw(g,"hud/g-charge",x,y);
            case "retreat"->UiTex.draw(g,"hud/g-retreat",x,y);
            default->{}
        }
    }

    /** The formation glyph; block draws as Compact, which is what the sheets call it. */
    static void formationGlyph(DrawContext g,String formation,boolean faded,int x,int y){
        if(faded)switch(formation){
            case "block"->UiTex.draw(g,"hud/g-compact-faded",x,y);
            case "line"->UiTex.draw(g,"hud/g-line-faded",x,y);
            case "wedge"->UiTex.draw(g,"hud/g-wedge-faded",x,y);
            case "loose"->UiTex.draw(g,"hud/g-loose-faded",x,y);
            case "shieldwall"->UiTex.draw(g,"hud/g-shieldwall-faded",x,y);
            default->{}
        }else switch(formation){
            case "block"->UiTex.draw(g,"hud/g-compact",x,y);
            case "line"->UiTex.draw(g,"hud/g-line",x,y);
            case "wedge"->UiTex.draw(g,"hud/g-wedge",x,y);
            case "loose"->UiTex.draw(g,"hud/g-loose",x,y);
            case "shieldwall"->UiTex.draw(g,"hud/g-shieldwall",x,y);
            default->{}
        }
    }

    static String groupLabel(String id){for(var row:Spec.GROUPS)if(row.id().equals(id))return row.label();return CommandMode.label(id);}
    static String orderLabel(String id){for(var row:Spec.ORDERS)if(row.id().equals(id))return row.label();return CommandMode.label(id);}
    static String formationLabel(String id){for(var row:Spec.FORMATIONS)if(row.id().equals(id))return row.label();return CommandMode.label(id);}

    /** The Long March lesson on a vellum card below the seals; its copy wraps to LESSON_TEXT_WIDTH and the card grows a row per
     *  line, so nothing is cut. Hidden while an order is being aimed. */
    static void lesson(DrawContext g,TextRenderer font,int cx,Map<String,int[]> r,Map<String,String> tx){
        int step=CampaignClient.tutorial;if(step<0||step>3||CommandMode.active())return;
        List<String> lines=wrapped(font,GravesAndGloryClient.lessonText(step),DarkUi.BODY_STYLE);
        int x=cx-200,y=88,h=40+11*Math.max(0,lines.size()-1);
        lessonCard(g,x,y,h);
        g.drawText(font,"THE LONG MARCH / BATTLE "+CampaignClient.battleOrder()+" / LESSON "+(step+1)+" OF 4",x+10,y+7,DarkUi.SEPIA,false);
        for(int i=0;i<lines.size();i++)g.drawText(font,DarkUi.body(lines.get(i)),x+10,y+20+11*i,DarkUi.INK_TEXT,false);
        GravesAndGloryClient.lessonDrawn=lines;r.put("lesson",new int[]{x,y,400,h});tx.put("lesson",wrappedJoined);
    }

    /** The lesson copy wrapped to LESSON_TEXT_WIDTH. It is wrapped again only when the copy changes (the next step, or a rebound key
     *  the copy names) or its style does; a resource reload clears it, since the font may have changed. */
    static List<String> wrapped(TextRenderer font,String copy,Style style){
        if(!copy.equals(wrappedCopy)||!style.equals(wrappedStyle)){
            wrappedLines=font.getTextHandler().wrapLines(copy,GravesAndGloryClient.LESSON_TEXT_WIDTH,style).stream().map(StringVisitable::getString).toList();
            wrappedJoined=String.join(" ",wrappedLines);wrappedCopy=copy;wrappedStyle=style;wraps++;
        }
        return wrappedLines;
    }

    /** The PNG rows outside a lesson card's box: one above it (the atlas oy) and five of shadow below. */
    static final int LESSON_FRAME=6;
    /** hud/lesson-card at any height: a three-v (tools/ui_art kit_extra.lesson_card) whose middle band is the side edges' plain rows
     *  around bare vellum, so the card grows between its corner brackets; at 40 px it is the painting itself. */
    static void lessonCard(DrawContext g,int x,int y,int h){UiTex.three(g,"hud/lesson-card",x,y,h+LESSON_FRAME);}

    /** The event feed (v4.1): the vellum leaf at the right edge, its box at (W-188, H/2-38), the newest line on the bottom row (4) and
     *  older ones above. A line is its glyph and its text, trimmed to 140 px after a whole word (UiMath.trimWords), in its style's
     *  ink, fading over its last 2 s (a line whose alpha is under 4 is skipped: Minecraft draws such text opaque). The slip draws
     *  while a line lives, at the newest line's alpha. */
    static void feed(DrawContext g,TextRenderer font,int w,int h,Map<String,int[]> r,Map<String,String> tx){
        long now=Util.getMeasuringTimeMs();var lines=EventFeed.lines(now);if(lines.isEmpty())return;
        int x=w-188,y=h/2-38,n=lines.size(),viewer=GravesAndGloryClient.team;
        try{RenderSystem.setShaderColor(1,1,1,UiMath.feedAlpha(now-lines.get(n-1).bornMs()));UiTex.draw(g,"hud/feed-slip",x,y);}finally{RenderSystem.setShaderColor(1,1,1,1);}
        for(int i=0;i<n;i++){
            var line=lines.get(i);int row=EventFeed.MAX_LINES-n+i,ry=y+9+12*row;float a=UiMath.feedAlpha(now-line.bornMs());int alpha=Math.round(a*255);
            if(alpha<4)continue;
            var style=FeedLines.style(line.kind(),line.team(),viewer);
            try{RenderSystem.setShaderColor(1,1,1,a);feedGlyph(g,style.glyph(),x+8,ry);}finally{RenderSystem.setShaderColor(1,1,1,1);}
            String text=UiMath.trimWords(line.text(),140,font::getWidth);
            g.drawText(font,text,x+20,ry,alpha<<24|style.argb()&0xffffff,false);tx.put("feed-"+row,text);
        }
        r.put("feed",new int[]{x,y,172,74});
    }

    /** A feed glyph by its atlas name, each name written out so the art check sees it. */
    static void feedGlyph(DrawContext g,String glyph,int x,int y){
        switch(glyph){
            case "hud/fgv-seal-dawn"->UiTex.draw(g,"hud/fgv-seal-dawn",x,y);
            case "hud/fgv-seal-dusk"->UiTex.draw(g,"hud/fgv-seal-dusk",x,y);
            case "hud/fgv-seal-x-dawn"->UiTex.draw(g,"hud/fgv-seal-x-dawn",x,y);
            case "hud/fgv-seal-x-dusk"->UiTex.draw(g,"hud/fgv-seal-x-dusk",x,y);
            case "hud/fgv-skull"->UiTex.draw(g,"hud/fgv-skull",x,y);
            case "hud/fgv-skull-grey"->UiTex.draw(g,"hud/fgv-skull-grey",x,y);
            case "hud/fgv-banner-down"->UiTex.draw(g,"hud/fgv-banner-down",x,y);
            case "hud/fgv-banner-up"->UiTex.draw(g,"hud/fgv-banner-up",x,y);
            case "hud/fgv-rout"->UiTex.draw(g,"hud/fgv-rout",x,y);
            case "hud/fgv-moon"->UiTex.draw(g,"hud/fgv-moon",x,y);
            default->UiTex.draw(g,"hud/fgv-sun",x,y);
        }
    }

    /** While the commander is down: a card at the middle of the canvas counting the seconds. */
    static void respawn(DrawContext g,TextRenderer font,int cx,int h,Map<String,int[]> r,Map<String,String> tx){
        int seconds=GravesAndGloryClient.respawn;if(seconds<=0)return;
        int x=cx-100,y=h/2-40;String line="Respawn in "+seconds+" s";Text t=DarkUi.heading(line);
        UiTex.draw(g,"hud/respawn-card",x,y);
        g.drawText(font,t,(int)Math.round(cx-font.getWidth(t)/2.0),y+8,DarkUi.INK_TEXT,false);
        r.put("respawn",new int[]{x,y,200,28});tx.put("respawn",line);
    }

    /** The commander ability beside the hotbar, in GUI px at the player's GUI Scale: the iron slot, the horn, the cooldown sweep (a
     *  veil over the part of the turn still to come and its bright leading edge), the key on its brass plate and the seconds left.
     *  A ready ability (nothing left of its cooldown) shows no sweep and no number. */
    static void ability(DrawContext g,MinecraftClient c,TextRenderer font,Map<String,int[]> r,Map<String,String> tx){
        String id=GravesAndGloryClient.abilityId;if(id.equals("none"))return;
        var w=c.getWindow();int x=w.getScaledWidth()/2+104,y=w.getScaledHeight()-47,left=GravesAndGloryClient.abilityRemaining,total=GravesAndGloryClient.abilityTotal;
        UiTex.draw(g,"hud/ability-slot",x,y);
        UiTex.draw(g,"hud/horn",x+5,y+6); // every ability shows the horn until Phase 2 gives each its picture
        if(left>0&&total>0)sweep(g,x+4,y+4,Math.min(1,left/(double)total));
        UiTex.draw(g,"hud/key-brass-plate",x+1,y+1);
        String key=GravesAndGloryClient.hint("ability"),secs=left>0?String.valueOf((left+19)/20):"";
        g.drawText(font,key,x+4,y+4,DarkUi.ABILITY_KEY,false);
        if(!secs.isEmpty())g.drawText(font,secs,centred(font,secs,x+39),y+12,DarkUi.IVORY,true);
        double f=w.getScaleFactor()/UiCanvas.k();
        int x0=(int)Math.floor(x*f+1e-9),y0=(int)Math.floor(y*f+1e-9),x1=(int)Math.ceil((x+50)*f-1e-9),y1=(int)Math.ceil((y+32)*f-1e-9);
        r.put("ability",new int[]{x0,y0,x1-x0,y1-y0});tx.put("ability",id);tx.put("ability-seconds",secs);tx.put("ability-key",key);
    }

    /** The 24 px cooldown sweep at (x, y) by UiMath.sweep, filled one run of a row at a time. */
    static void sweep(DrawContext g,int x,int y,double remain){
        for(int row=0;row<24;row++){
            int from=0,kind=UiMath.sweep(0,row,24,remain);
            for(int col=1;col<=24;col++){
                int next=col<24?UiMath.sweep(col,row,24,remain):-1;
                if(next==kind)continue;
                if(kind>0)g.fill(x+from,y+row,x+col,y+row+1,kind==1?DarkUi.SWEEP_VEIL:kind==2?DarkUi.SWEEP_EDGE:DarkUi.SWEEP_LINE);
                from=col;kind=next;
            }
        }
    }
}
