package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import java.util.*;

/** The Iron Ledger battle HUD (UI spec 4.5, mock v4-hud-iron-ledger, made lighter by UI Task 8c): the score bar between the two
 *  bosses, the point seals hanging from it, the group column on its iron spine, the commander's ability slot beside the hotbar, the
 *  lesson note and the respawn card. In the column the selected group keeps its ledger card, with its order, formation and key
 *  row; every other group hangs as a chip one line high, and while the whole army is selected each group's chip is brass and one
 *  key strip hangs under the bottom chip. All but the ability slot is drawn on the HUD's canvas (UiCanvas.hudK, at the player's
 *  HudSize), every plate where UiMath.hudLayout puts it for the frame's state (state()); the slot sits in vanilla GUI space at the
 *  player's GUI Scale, next to the hotbar. Colour never carries a meaning alone (spec 6.1): seals carry their owner's emblem, a
 *  routing group its stamp and grey plate, a fallen one its word, a fighting one the crossed swords. The event feed hangs at the
 *  right edge; the hint layer (altLayer) hangs the hint slip above the column. What the last frame drew is kept for checks:
 *  cards(), seals(), rects(), texts(), hintRows(), hintEnds(), keyRows() and feedRows(). */
public final class BattleHud {
    private BattleHud() {}

    /** A plate of the group column the last frame drew: its group, its state (PLAIN, FLASH, SELECTED, ROUTING or FALLEN), whether
     *  it is the selected group's ledger card (tall, with the key row) rather than a chip, and its box in canvas px. */
    public record CardDraw(String group,String state,boolean tall,int x,int y,int w,int h){}
    /** A seal the last frame drew: its sprite and its place in canvas px. */
    public record SealDraw(String sprite,int x,int y){}
    /** A key row the last frame drew, on the selected group's card or ("all") on the army's key strip: its chips and words in
     *  order, and the canvas x where each starts and where it ends. */
    public record KeyRowDraw(String group,List<String> parts,List<Integer> from,List<Integer> to){}
    /** A feed line the last frame drew: its row (from 0 at the leaf's top), its glyph's atlas name, its ink (opaque) and its text as
     *  trimmed. */
    public record FeedDraw(int row,String glyph,int argb,String text){}

    /** Food icons HungerHudMixin skipped. It lives here because a mixin class cannot be referenced at runtime. */
    public static int foodSkipped;
    /** Frames the battle HUD has drawn. */
    public static int frames;
    /** Times the lesson copy was wrapped: once per new copy or tag, and once more after a resource reload. */
    public static int wraps;
    /** The client clock (ms) when running last turned true; the hint layer shows until hintLayerMs() after it. The STATE receiver
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
    private static final List<FeedDraw> feedRowsDrawn=new ArrayList<>();
    private static final List<CardDraw> cards=Collections.unmodifiableList(cardsDrawn);
    private static final List<SealDraw> seals=Collections.unmodifiableList(sealsDrawn);
    private static final Map<String,int[]> rects=Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String,String> texts=Collections.unmodifiableMap(textsDrawn);
    private static final List<List<String>> hintRows=Collections.unmodifiableList(hintRowsDrawn);
    private static final List<Integer> hintEnds=Collections.unmodifiableList(hintEndsDrawn);
    private static final List<KeyRowDraw> keyRows=Collections.unmodifiableList(keyRowsDrawn);
    private static final List<FeedDraw> feedRows=Collections.unmodifiableList(feedRowsDrawn);
    /** Set when a frame named a texture the atlas lacks: the HUD stays off, and the client up, until the next resource reload. */
    private static boolean stopped;
    // The lesson copy last wrapped, the tag before it, its lines and the lines joined.
    private static String wrappedCopy,wrappedTag;private static List<String> wrappedLines=List.of();private static String wrappedJoined="";

    /** The column's plates the last frame drew. This and the three below are read-only views that the next frame refills: copy one
     *  to keep it. */
    public static List<CardDraw> cards(){return cards;}
    public static List<SealDraw> seals(){return seals;}
    /** Each element's box {x,y,w,h} in canvas px, keyed as UiMath.hudLayout keys them (score, dawn-boss, dusk-boss, seal-(i), spine,
     *  card-(group) for a card or a chip, keys for the army's key strip, hints for the hint slip, lesson, respawn, and feed while a
     *  line lives), then aim-name (the aimed point's name, while an order is aimed into a point) and ability (the slot converted
     *  from GUI px, rounded outward). */
    public static Map<String,int[]> rects(){return rects;}
    /** The words and numbers the last frame drew: score-dawn, clock, score-dusk, count-(group), state-(group) (the word a card
     *  shows; on a chip ROUTING and Fallen as drawn, Fighting for its crossed swords and Steady for none), lesson (its lines
     *  joined), respawn, feed-(row) (each feed line as trimmed), aim-name, ability, ability-seconds and ability-key. */
    public static Map<String,String> texts(){return texts;}
    /** The chip and word texts of each row the hint slip last drew, in order; empty while the hint layer is down. */
    public static List<List<String>> hintRows(){return hintRows;}
    /** The canvas x where each row of the hint slip ends (its last chip or word), as the last frame drew it; empty while the hint
     *  layer is down. */
    public static List<Integer> hintEnds(){return hintEnds;}
    /** The key rows the last frame drew: the selected group's card's, or the army's key strip's. */
    public static List<KeyRowDraw> keyRows(){return keyRows;}
    /** The feed lines the last frame drew, top row first; empty while the feed is empty. */
    public static List<FeedDraw> feedRows(){return feedRows;}
    /** How long a round's start shows the hint layer at the player's HUD size (UiMath.hintLayerMs: 15 s at Compact, the spec's 60 s
     *  at Standard and Large). */
    static long hintLayerMs(){return UiMath.hintLayerMs(HudSize.get());}
    /** The hint layer is up: the hints key held, the round's first hintLayerMs(), or an order being aimed. The hint slip then hangs
     *  above the column, naming the keys the column does not (the order keys are always on the selected card or the key strip). */
    public static boolean altLayer(){
        var k=GravesAndGloryClient.hints;
        return k!=null&&k.isPressed()||Util.getMeasuringTimeMs()-roundShownMs<hintLayerMs()||CommandMode.active();
    }
    /** A frame without the ledger (camp, a battle's entrance, a menu over the HUD): nothing reads as drawn. */
    static void clear(){cardsDrawn.clear();sealsDrawn.clear();rectsDrawn.clear();textsDrawn.clear();hintRowsDrawn.clear();hintEndsDrawn.clear();keyRowsDrawn.clear();feedRowsDrawn.clear();}
    /** A disconnect (GravesAndGloryClient.resetHud, on the client thread: the first client tick without a world after it, and the
     *  next join): the last battle's records go with it, since no HUD frame runs without a world to clear them, and ChatPlacement
     *  reads rects() before the next world's first frame. That is what the frame drew, the lesson wrap and the lines drawn from it,
     *  and the hint layer's round clock. */
    static void reset(){clear();wrappedCopy=null;wrappedTag=null;wrappedLines=List.of();wrappedJoined="";roundShownMs=0;GravesAndGloryClient.lessonDrawn=List.of();}
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
            UiCanvas.beginHud(g);
            try{
                var win=c.getWindow();long now=Util.getMeasuringTimeMs();int size=HudSize.get();var s=state(font,now);
                var at=UiMath.hudLayout(win.getFramebufferWidth(),win.getFramebufferHeight(),(int)win.getScaleFactor(),UiCanvas.hudK(),size,s);
                ledger(g,font,at,rectsDrawn,textsDrawn);
                seals(g,font,at,rectsDrawn,sealsDrawn,textsDrawn);
                column(g,font,s,at,rectsDrawn,cardsDrawn,textsDrawn);
                lesson(g,font,at,rectsDrawn,textsDrawn);
                respawn(g,font,at,rectsDrawn,textsDrawn);
                feed(g,font,size,now,at,rectsDrawn,textsDrawn);
            }finally{UiCanvas.end(g);}
            ability(g,c,font,rectsDrawn,textsDrawn);
        }catch(IllegalArgumentException e){ // UiTex.entry names a texture the atlas lacks: a resource pack's atlas, or none read at all
            stopped=true;clear();
            System.err.println("[Graves and Glory] The battle HUD stopped: "+e.getMessage()+". It stays off until the next resource reload.");
            return;
        }
        frames++;
    }

    /** This frame's state as UiMath.hudLayout reads it: the points, the groups the round fielded (a full count above 0) in their
     *  order, the selection, the hint layer, the lesson's lines (none outside the tutorial's four lessons or while an order is
     *  aimed), the commander down and a feed line alive at now. */
    static UiMath.HudState state(TextRenderer font,long now){
        List<String> fielded=new ArrayList<>();
        for(String id:Morale.GROUPS){var grp=group(id);if(grp!=null&&grp.full()>0)fielded.add(id);}
        int step=CampaignClient.tutorial,lines=step<0||step>3||CommandMode.active()?0:lesson(font,step).size();
        return new UiMath.HudState(GravesAndGloryClient.owners.length,List.copyOf(fielded),CommandMode.selected,altLayer(),lines,GravesAndGloryClient.respawn>0,!EventFeed.lines(now).isEmpty());
    }

    /** The group's last record from the server, or null. */
    static CommandMode.Group group(String id){for(var grp:CommandMode.groups)if(grp.id().equals(id))return grp;return null;}

    /** The x that centres an 8 px text on cx as the mock does: the text's box is its width plus one, the shadow's column. */
    static int centred(TextRenderer font,String s,int cx){return (int)Math.round(cx-(font.getWidth(s)+1)/2.0);}

    /** The score bar: Dawn's score, its track, the round clock, Dusk's track and score, centred between the Dawn and Dusk bosses. */
    static void ledger(DrawContext g,TextRenderer font,Map<String,int[]> at,Map<String,int[]> r,Map<String,String> tx){
        int[] card=at.get("score"),dawnBoss=at.get("dawn-boss"),duskBoss=at.get("dusk-boss");int cx=card[0]+card[2]/2,y=card[1]+8;
        String dawn=String.valueOf(GravesAndGloryClient.dawn),clock=UiMath.clock(CommandMode.seconds),dusk=String.valueOf(GravesAndGloryClient.dusk);
        int win=GravesAndGlory.R.winScore(),wa=font.getWidth(dawn),wc=font.getWidth(clock),wd=font.getWidth(dusk);
        UiTex.draw(g,"hud/score-card",card[0],card[1]);
        // The row as the mock lays it out: each 8 px text takes its width plus one, with 6 px gaps round the two 56 px tracks.
        int x=(int)Math.round(cx-(wa+1+wc+1+wd+1+2*56+4*6)/2.0);
        g.drawText(font,dawn,x,y,DarkUi.INK_TEXT,false);x+=wa+1+6;
        UiTex.draw(g,"hud/track-empty",x,y);UiTex.bar(g,"hud/track-dawn-fill",x+1,y+2,UiMath.trackFill(GravesAndGloryClient.dawn,win),false);x+=56+6;
        g.drawText(font,clock,x,y,DarkUi.INK_TEXT,false);x+=wc+1+6;
        UiTex.draw(g,"hud/track-empty",x,y);UiTex.bar(g,"hud/track-dusk-fill",x+1,y+2,UiMath.trackFill(GravesAndGloryClient.dusk,win),true);x+=56+6;
        g.drawText(font,dusk,x,y,DarkUi.INK_TEXT,false);
        UiTex.draw(g,"hud/boss-dawn",dawnBoss[0],dawnBoss[1]);UiTex.draw(g,"hud/boss-dusk",duskBoss[0],duskBoss[1]);
        r.put("score",card);r.put("dawn-boss",dawnBoss);r.put("dusk-boss",duskBoss);
        tx.put("score-dawn",dawn);tx.put("clock",clock);tx.put("score-dusk",dusk);
    }

    /** The top of the aimed point's name: the seal rects' foot (each seal's box runs from its chain's top at 37 down 31 px), so the
     *  name clears every seal. */
    static final int AIM_NAME_Y=68;

    /** The points hang from the bar on a 28 px pitch: chain, seal (its owner's, or split while contested) and letter; while an order
     *  is aimed into a point's radius, that point's name hangs below its seal. */
    static void seals(DrawContext g,TextRenderer font,Map<String,int[]> at,Map<String,int[]> r,List<SealDraw> sd,Map<String,String> tx){
        int n=GravesAndGloryClient.owners.length,aim=CommandMode.aimPoint();var pts=Spec.POINTS;
        for(int i=0;i<n;i++){
            int[] box=at.get("seal-"+i);if(box==null)continue;
            int sc=box[0]+13,top=box[1],owner=GravesAndGloryClient.ownerAt(i);boolean contested=GravesAndGloryClient.contestedAt(i);
            int y=top+(contested?3:4);String sprite;
            UiTex.draw(g,"hud/chain",sc-2,top);
            if(contested){sprite="hud/seal-contested";UiTex.draw(g,"hud/seal-contested",sc-13,y);}
            else if(owner==0){sprite="hud/seal-dawn";UiTex.draw(g,"hud/seal-dawn",sc-13,y);}
            else if(owner==1){sprite="hud/seal-dusk";UiTex.draw(g,"hud/seal-dusk",sc-13,y);}
            else{sprite="hud/seal-neutral";UiTex.draw(g,"hud/seal-neutral",sc-13,y);}
            String id=i<pts.length?pts[i].id():String.valueOf((char)('A'+i));
            g.drawText(font,id,centred(font,id,sc),y+9,DarkUi.IVORY,true);
            sd.add(new SealDraw(sprite,sc-13,y));r.put("seal-"+i,box);
            if(aim==i&&i<pts.length){String name=pts[i].name();int nx=centred(font,name,sc);g.drawText(font,name,nx,AIM_NAME_Y,DarkUi.IVORY,true);tx.put("aim-name",name);r.put("aim-name",new int[]{nx,AIM_NAME_Y,font.getWidth(name)+1,9});}
        }
    }

    /** The group column (UI Task 8c), each plate where the layout hangs it, infantry at the top: the spine; the selected group's
     *  ledger card (card) and a chip for every other group the round fielded (chip), or while the whole army is selected a brass
     *  chip for each group over the army's key strip (keyStrip); and in the hint layer the hint slip above the top plate (above the
     *  column's foot when there is none). A group the round never fielded has no plate. */
    static void column(DrawContext g,TextRenderer font,UiMath.HudState s,Map<String,int[]> at,Map<String,int[]> r,List<CardDraw> cd,Map<String,String> tx){
        boolean army="all".equals(s.selected());
        int[] spine=at.get("spine");if(spine!=null){UiTex.three(g,"hud/spine",spine[0],spine[1],spine[3]);r.put("spine",spine);}
        for(String id:s.groups()){
            var grp=group(id);int[] box=at.get("card-"+id);if(grp==null||box==null)continue;
            if(box[3]==UiMath.CARD_H)card(g,font,grp,box,r,cd,tx);else chip(g,font,grp,box,army,r,cd,tx);
        }
        int[] keys=at.get("keys");if(keys!=null)keyStrip(g,font,keys,r);
        int[] hints=at.get("hints");if(hints!=null)hintSlip(g,font,hints,r);
    }

    /** How far a hint-slip row may run: from 10 px into the slip to 12 px inside its box's right edge (the 164 px slip's paper ends
     *  at x 160, so 8 px of paper stay clear, as on the left). */
    static final int HINT_ROW_W=142;
    /** The words on a key row, and the short ones it takes where long key names crowd it (UiMath.cardRow). */
    static final List<String> CARD_WORDS=List.of("Attack","Follow","Formation"),CARD_SHORT_WORDS=List.of("Attack","Follow","Form");

    /** The hint slip in its box: row one names Map and Commands (Confirm and Cancel while an order is aimed), row two the facing keys
     *  and the hints key. Each key is an ink chip naming the player's binding, a mouse button as LMB, RMB or MMB. Where long names
     *  would run a row past the paper, the row takes the keys' short names (PgDn, RShift, None for an unbound key), and those give
     *  way alike where they still would (UiMath.fitKeys): nothing runs past it. */
    static void hintSlip(DrawContext g,TextRenderer font,int[] box,Map<String,int[]> r){
        int x=box[0],y=box[1];boolean aiming=CommandMode.active();
        UiTex.draw(g,"hud/notice-slip",x,y);
        String[][] ids={aiming?new String[]{"confirm","cancel"}:new String[]{"map","menu"},{"rotate_left","rotate_right","hints"}};
        String[][] words={aiming?new String[]{"Confirm","Cancel"}:new String[]{"Map","Commands"},{null,"Turn","Hints"}};
        for(int row=0;row<2;row++){
            List<String> keys=new ArrayList<>(),shorts=new ArrayList<>();for(String id:ids[row]){keys.add(GravesAndGloryClient.hint(id));shorts.add(GravesAndGloryClient.hintShort(id));}
            keys=UiMath.fitKeys(keys,shorts,Arrays.asList(words[row]),HINT_ROW_W,8,font::getWidth);
            List<String> drawn=new ArrayList<>();int kx=x+10,ky=y+(row==0?7:22),end=kx;
            for(int k=0;k<keys.size();k++){
                String key=keys.get(k);int kw=UiMath.chipWidth(font.getWidth(key));
                UiTex.three(g,"hud/key-chip",kx,ky,kw);g.drawText(font,key,kx+2,ky+2,DarkUi.CHIP_TEXT,false);drawn.add(key);end=kx+kw;kx+=kw+2;
                String word=words[row][k];
                if(word!=null){g.drawText(font,word,kx,ky+2,DarkUi.SEPIA,false);drawn.add(word);end=kx+font.getWidth(word);kx+=font.getWidth(word)+8;}
            }
            hintRowsDrawn.add(List.copyOf(drawn));hintEndsDrawn.add(end);
        }
        r.put("hints",box);
    }

    /** A plate's state, first match wins: FALLEN (none left of a fielded group), ROUTING, FLASH (a loss in the last
     *  CommandMode.FLASH_MS), SELECTED (the selected group, or every group while the army is), PLAIN. */
    static String plateState(CommandMode.Group grp,boolean chosen){
        return grp.count()==0?"FALLEN":grp.routing()?"ROUTING":CommandMode.flashing(grp.id())?"FLASH":chosen?"SELECTED":"PLAIN";
    }

    /** The selected group's ledger card, 62 px tall, as the mock draws it: its name, its state word (the rout stamp; the fight
     *  glyph before Fighting), the strength bar and count, the order and formation glyphs with their names, and the key row. */
    static void card(DrawContext g,TextRenderer font,CommandMode.Group grp,int[] box,Map<String,int[]> r,List<CardDraw> cd,Map<String,String> tx){
        String id=grp.id(),state=plateState(grp,true);int x=box[0],y=box[1];
        boolean down=state.equals("ROUTING")||state.equals("FALLEN");
        switch(state){
            case "FLASH"->UiTex.draw(g,"hud/card-flash-62",x,y);
            case "SELECTED"->UiTex.draw(g,"hud/card-sel",x,y);
            default->UiTex.draw(g,"hud/card-rout-62",x,y);
        }
        UiTex.draw(g,"hud/hook-brass",x-7,y+3);
        g.drawText(font,DarkUi.heading(groupLabel(id)),x+10,y+7,DarkUi.INK_TEXT,false);
        // The state, right-aligned to x 166: the rout stamp, or a word (the fight glyph before Fighting).
        String word;
        if(state.equals("ROUTING")){word="ROUTING";UiTex.draw(g,"hud/stamp-rout",x+108,y+4);g.drawText(font,word,centred(font,word,x+135),y+8,DarkUi.ROUT_TEXT,false);}
        else{
            word=state.equals("FALLEN")?"Fallen":grp.fighting()?"Fighting":"Steady";Text t=DarkUi.body(word);int wx=x+166-font.getWidth(t);
            g.drawText(font,t,wx,y+7,word.equals("Steady")?DarkUi.SEPIA:DarkUi.INK_TEXT,false); // Fallen in ink: sepia on the grey plate is 3.5:1
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
        g.drawText(font,orderLabel(grp.order()),x+22,y+31,DarkUi.INK_TEXT,false);
        formationGlyph(g,grp.formation(),down,x+72,y+30);
        g.drawText(font,formationLabel(grp.formation()),x+84,y+31,DarkUi.INK_TEXT,false);
        keyRow(g,font,id,x,y+44,DarkUi.SEPIA);
        cd.add(new CardDraw(id,state,true,x,y,box[2],box[3]));r.put("card-"+id,box);
        tx.put("count-"+id,count);tx.put("state-"+id,word);
    }

    /** Where a chip's state slot starts (66 px wide: the strength channel, the ROUTING stamp or Fallen), and how wide its name may
     *  run from x 10, 4 px short of the slot. */
    static final int CHIP_SLOT=56,CHIP_NAME_W=42;

    /** A group's chip, 176 x 18 (UI Task 8c): its hook on the spine, its name, its state slot, its count right-aligned to x 156 and,
     *  while it fights, the crossed swords at x 159. The slot holds the strength bar in its channel, the stretch just lost white
     *  during a loss flash (the red rule round the chip); ROUTING's stamp and word in place of the bar; or Fallen, on the grey
     *  plate. Steady is the chip without the swords: the chip calls the eye only for what has changed. Task 9's LAST STAND takes
     *  this slot as ROUTING does: its stamp (hud/stamp-chip) and its words, never a colour alone (spec 6.1). */
    static void chip(DrawContext g,TextRenderer font,CommandMode.Group grp,int[] box,boolean chosen,Map<String,int[]> r,List<CardDraw> cd,Map<String,String> tx){
        String id=grp.id(),state=plateState(grp,chosen);int x=box[0],y=box[1],sx=x+CHIP_SLOT;
        boolean down=state.equals("ROUTING")||state.equals("FALLEN");
        switch(state){
            case "PLAIN"->UiTex.draw(g,"hud/chip-plain",x,y);
            case "FLASH"->UiTex.draw(g,"hud/chip-flash",x,y);
            case "SELECTED"->UiTex.draw(g,"hud/chip-sel",x,y);
            default->UiTex.draw(g,"hud/chip-rout",x,y);
        }
        if(chosen)UiTex.draw(g,"hud/hook-brass",x-7,y+3);else UiTex.draw(g,"hud/hook",x-7,y+3);
        String name=UiMath.trim(groupLabel(id),CHIP_NAME_W,v->font.getWidth(DarkUi.heading(v)));
        g.drawText(font,DarkUi.heading(name),x+10,y+4,DarkUi.INK_TEXT,false);
        String word;
        if(state.equals("ROUTING")){word="ROUTING";UiTex.draw(g,"hud/stamp-chip",sx,y+2);g.drawText(font,word,centred(font,word,sx+33),y+5,DarkUi.ROUT_TEXT,false);}
        else if(state.equals("FALLEN")){word="Fallen";Text t=DarkUi.body(word);g.drawText(font,t,(int)Math.round(sx+33-font.getWidth(t)/2.0),y+4,DarkUi.INK_TEXT,false);} // ink: 6.4:1 on the grey plate, sepia 3.5:1
        else{
            word=grp.fighting()?"Fighting":"Steady";int bar=UiMath.chipBar(grp.strength());
            UiTex.draw(g,"hud/chip-channel",sx,y+6);
            if(GravesAndGloryClient.team==1)UiTex.bar(g,"hud/hp-full-dusk",sx+1,y+7,bar,false);else UiTex.bar(g,"hud/hp-full-dawn",sx+1,y+7,bar,false);
            if(state.equals("FLASH"))UiTex.bar(g,"hud/hp-lost",sx+1+bar,y+7,UiMath.chipBar(CommandMode.lostFrom(id))-bar,false);
            if(grp.fighting())UiTex.draw(g,"hud/g-fight",x+159,y+4);
        }
        String count=UiMath.countText(grp.count(),grp.full()); // up to 30/30, the full roster's 30 troops: 30 px, clear of the slot
        g.drawText(font,count,x+156-font.getWidth(count),y+5,down?DarkUi.ROUT_TEXT:DarkUi.INK_TEXT,false);
        cd.add(new CardDraw(id,state,false,x,y,box[2],box[3]));r.put("card-"+id,box);
        tx.put("count-"+id,count);tx.put("state-"+id,word);
    }

    /** The army's key strip under the bottom chip while the whole army is selected (UI Task 8c): the order keys of a card's key row
     *  at a card's places, on a 176 x 16 plate capped in brass and hung on the spine by a brass hook. The keys are the same for every
     *  group, so one row serves the army: the cheapest form that keeps them on screen. Its labels are in KEY_INK, a step darker than
     *  the card's sepia: the strip is the darker vellum, where sepia holds 4.6:1 and 3.7:1 at its edges (#bda97c), KEY_INK 6.0:1
     *  there (UI Task 8c fix round 1; test_ui_standard checks it). */
    static void keyStrip(DrawContext g,TextRenderer font,int[] box,Map<String,int[]> r){
        int x=box[0],y=box[1];
        UiTex.draw(g,"hud/keys-strip",x,y);UiTex.draw(g,"hud/hook-brass",x-7,y+3);
        keyRow(g,font,"all",x,y+3,DarkUi.KEY_INK);r.put("keys",box);
    }

    /** A key row at canvas y ky on a plate at x: the order keys, each in an ink chip as wide as its key's name, at the mock's places,
     *  or where long names crowd the row flowed and fitted as on the hint slip, short names first (UiMath.cardRow); each key's label
     *  after it in labelInk, the plate's own (sepia on a card, KEY_INK on the key strip). */
    static void keyRow(DrawContext g,TextRenderer font,String group,int x,int ky,int labelInk){
        var row=UiMath.cardRow(List.of(GravesAndGloryClient.hint("attack"),GravesAndGloryClient.hint("follow"),GravesAndGloryClient.hint("formation")),List.of(GravesAndGloryClient.hintShort("attack"),GravesAndGloryClient.hintShort("follow"),GravesAndGloryClient.hintShort("formation")),CARD_WORDS,CARD_SHORT_WORDS,font::getWidth);
        List<String> parts=new ArrayList<>();List<Integer> from=new ArrayList<>(),to=new ArrayList<>();
        for(int k=0;k<3;k++){
            String key=row.keys().get(k),label=row.words().get(k);int kw=UiMath.chipWidth(font.getWidth(key)),kx=x+row.at().get(k);
            UiTex.three(g,"hud/key-chip",kx,ky,kw);
            g.drawText(font,key,kx+2,ky+2,DarkUi.CHIP_TEXT,false);
            g.drawText(font,label,kx+kw+2,ky+2,labelInk,false);
            parts.add(key);from.add(kx);to.add(kx+kw);parts.add(label);from.add(kx+kw+2);to.add(kx+kw+2+font.getWidth(label));
        }
        keyRowsDrawn.add(new KeyRowDraw(group,List.copyOf(parts),List.copyOf(from),List.copyOf(to)));
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

    /** The lesson note's tag, before the copy's first line: LESSON k/4. */
    static String lessonTag(int step){return "LESSON "+(step+1)+"/4";}

    /** The Long March lesson as one short note at the top left (UI Task 8c): under the seals and off the centre band, in the box the
     *  layout gives it, the LESSON k/4 tag in KEY_INK and the copy after it in ink, both in 11 px Lato (the tag a step darker than
     *  sepia, which holds 3.7:1 on the note's darkest vellum, #bda97c; KEY_INK 6.0:1: UI Task 8c fix round 1). Every lesson is two lines with the
     *  default keys; a long key name may make a third, and the note grows a line for it (a three-v whose middle band is bare
     *  vellum), so nothing is cut. The layout leaves it out outside the tutorial, while an order is aimed, and where it would come
     *  within CARD_GAP rows of the respawn card's ink (UiMath.lessonYields: at Compact never, on the windows ui-hud covers). */
    static void lesson(DrawContext g,TextRenderer font,Map<String,int[]> at,Map<String,int[]> r,Map<String,String> tx){
        int[] box=at.get("lesson");if(box==null)return;
        int step=CampaignClient.tutorial,x=box[0],y=box[1];List<String> lines=lesson(font,step);Text tag=DarkUi.small(lessonTag(step));
        UiTex.three(g,"hud/lesson-note",x,y,box[3]+LESSON_FRAME);
        g.drawText(font,tag,x+8,y+5,DarkUi.KEY_INK,false);
        int indent=font.getWidth(tag)+UiMath.LESSON_TAG_GAP;
        for(int i=0;i<lines.size();i++)g.drawText(font,DarkUi.small(lines.get(i)),x+8+(i==0?indent:0),y+5+11*i,DarkUi.INK_TEXT,false);
        GravesAndGloryClient.lessonDrawn=lines;r.put("lesson",box);tx.put("lesson",wrappedJoined);
    }

    /** Lesson step's copy in lines (UiMath.lessonLines: LESSON_TEXT_W a line in the small style, the first after the tag). It is
     *  wrapped again only when the copy changes (the next step, or a rebound key the copy names) or its tag does; a resource reload
     *  clears it, since the font may have changed. */
    static List<String> lesson(TextRenderer font,int step){
        String copy=GravesAndGloryClient.lessonText(step),tag=lessonTag(step);
        if(!copy.equals(wrappedCopy)||!tag.equals(wrappedTag)){
            int indent=font.getWidth(DarkUi.small(tag))+UiMath.LESSON_TAG_GAP;
            wrappedLines=UiMath.lessonLines(copy,indent,UiMath.LESSON_TEXT_W,v->font.getWidth(DarkUi.small(v)));
            wrappedJoined=String.join(" ",wrappedLines);wrappedCopy=copy;wrappedTag=tag;wraps++;
        }
        return wrappedLines;
    }

    /** The PNG rows outside a lesson note's box: one above it (the atlas oy) and five of shadow below. */
    static final int LESSON_FRAME=6;

    /** The event feed (v4.1) on its vellum leaf at the right edge (UiMath.feedBox): Compact's leaf of three entries, 166 x 44, or
     *  the five-entry leaf, 172 x 74, at Standard and Large; the newest line on the bottom row and older ones above. A line is its
     *  glyph and its text, trimmed to UiMath.feedTextW after a whole word (UiMath.trimWords), in its style's ink, fading over its last
     *  2 s (a line whose alpha is under 4 is skipped: Minecraft draws such text opaque). The leaf draws while a line lives, at the
     *  newest line's alpha. */
    static void feed(DrawContext g,TextRenderer font,int size,long now,Map<String,int[]> at,Map<String,int[]> r,Map<String,String> tx){
        int[] box=at.get("feed");if(box==null)return;var lines=EventFeed.lines(now);if(lines.isEmpty())return;
        int x=box[0],y=box[1],n=lines.size(),most=UiMath.feedLines(size),viewer=GravesAndGloryClient.team;long life=UiMath.feedLife(size);
        try{RenderSystem.setShaderColor(1,1,1,UiMath.feedAlpha(now-lines.get(n-1).bornMs(),life));if(size==UiMath.HUD_COMPACT)UiTex.draw(g,"hud/feed-slip-3",x,y);else UiTex.draw(g,"hud/feed-slip",x,y);}finally{RenderSystem.setShaderColor(1,1,1,1);}
        for(int i=0;i<n;i++){
            var line=lines.get(i);int row=most-n+i,ry=y+UiMath.feedRowY(size,row);float a=UiMath.feedAlpha(now-line.bornMs(),life);int alpha=Math.round(a*255);
            if(alpha<4)continue;
            var style=FeedLines.style(line.kind(),line.team(),viewer);
            try{RenderSystem.setShaderColor(1,1,1,a);feedGlyph(g,style.glyph(),x+8,ry);}finally{RenderSystem.setShaderColor(1,1,1,1);}
            String text=UiMath.trimWords(line.text(),UiMath.feedTextW(size),font::getWidth);
            g.drawText(font,text,x+20,ry,alpha<<24|style.argb()&0xffffff,false);tx.put("feed-"+row,text);feedRowsDrawn.add(new FeedDraw(row,style.glyph(),style.argb(),text));
        }
        r.put("feed",box);
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

    /** While the commander is down: the card counting the seconds in the box the layout gives it, 40 px above the canvas's middle, or
     *  higher where vanilla's crosshair (at the GUI Scale, above the HUD's scale) would come within 4 px of it (UiMath.respawnY). */
    static void respawn(DrawContext g,TextRenderer font,Map<String,int[]> at,Map<String,int[]> r,Map<String,String> tx){
        int[] box=at.get("respawn");if(box==null)return;
        int x=box[0],y=box[1],cx=x+box[2]/2;String line="Respawn in "+GravesAndGloryClient.respawn+" s";Text t=DarkUi.heading(line);
        UiTex.draw(g,"hud/respawn-card",x,y);
        g.drawText(font,t,(int)Math.round(cx-font.getWidth(t)/2.0),y+8,DarkUi.INK_TEXT,false);
        r.put("respawn",box);tx.put("respawn",line);
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
        double f=w.getScaleFactor()/UiCanvas.hudK();
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
