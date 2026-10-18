package gg.gravesandglory;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

/** The Iron Ledger battle HUD (UI spec 4.5, mock v4-hud-iron-ledger): the score bar between the two bosses, the point seals hanging
 *  from it, the group cards on their iron spine, the commander's ability slot beside the hotbar, and the lesson and respawn cards.
 *  All but the ability slot is drawn on the UiCanvas; the slot sits in vanilla GUI space at the player's GUI Scale, next to the
 *  hotbar. Colour never carries a meaning alone (spec 6.1): seals carry their owner's emblem, a routing card its stamp and grey
 *  card, a fallen one its word. What the last frame drew is kept for checks: cards(), seals(), rects() and texts(). */
public final class BattleHud {
    private BattleHud() {}

    /** A card the last frame drew: its group, its state (PLAIN, FLASH, SELECTED, ROUTING or FALLEN), whether it carried the key
     *  row, and its box in canvas px. */
    public record CardDraw(String group,String state,boolean tall,int x,int y,int w,int h){}
    /** A seal the last frame drew: its sprite and its place in canvas px. */
    public record SealDraw(String sprite,int x,int y){}

    /** Food icons HungerHudMixin skipped. It lives here because a mixin class cannot be referenced at runtime. */
    public static int foodSkipped;
    /** Frames the battle HUD has drawn. */
    public static int frames;

    // What the last frame drew: filled in place every frame, read through the views below.
    private static final List<CardDraw> cardsDrawn=new ArrayList<>();
    private static final List<SealDraw> sealsDrawn=new ArrayList<>();
    private static final Map<String,int[]> rectsDrawn=new LinkedHashMap<>();
    private static final Map<String,String> textsDrawn=new LinkedHashMap<>();
    private static final List<CardDraw> cards=Collections.unmodifiableList(cardsDrawn);
    private static final List<SealDraw> seals=Collections.unmodifiableList(sealsDrawn);
    private static final Map<String,int[]> rects=Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String,String> texts=Collections.unmodifiableMap(textsDrawn);
    /** Set when a frame named a texture the atlas lacks: the HUD stays off, and the client up, until the next resource reload. */
    private static boolean stopped;
    // The lesson copy last wrapped, its style and its lines.
    private static String wrappedCopy;private static Style wrappedStyle;private static List<String> wrappedLines=List.of();private static String wrappedJoined="";

    /** The cards the last frame drew. This and the three below are read-only views that the next frame refills: copy one to keep it. */
    public static List<CardDraw> cards(){return cards;}
    public static List<SealDraw> seals(){return seals;}
    /** Each element's box {x,y,w,h} in canvas px, keyed score, dawn-boss, dusk-boss, seal-(i), card-(group), spine, lesson, respawn
     *  and ability (the slot converted from GUI px, rounded outward). */
    public static Map<String,int[]> rects(){return rects;}
    /** The words and numbers the last frame drew: score-dawn, clock, score-dusk, count-(group), state-(group), lesson, respawn,
     *  ability, ability-seconds and ability-key. */
    public static Map<String,String> texts(){return texts;}
    /** A frame without the ledger (camp, a battle's entrance, a menu over the HUD): nothing reads as drawn. */
    static void clear(){cardsDrawn.clear();sealsDrawn.clear();rectsDrawn.clear();textsDrawn.clear();}

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
                seals(g,font,w,rectsDrawn,sealsDrawn);
                cards(g,font,h,rectsDrawn,cardsDrawn,textsDrawn);
                lesson(g,font,cx,rectsDrawn,textsDrawn);
                respawn(g,font,cx,h,rectsDrawn,textsDrawn);
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

    /** The points hang from the bar on a 28 px pitch: chain, seal (its owner's, or split while contested) and letter; while an order
     *  is aimed into a point's radius, that point's name hangs below its seal. */
    static void seals(DrawContext g,TextRenderer font,int w,Map<String,int[]> r,List<SealDraw> sd){
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
            if(aim==i&&i<pts.length){String name=pts[i].name();g.drawText(font,name,centred(font,name,sc),62,DarkUi.IVORY,true);}
            sd.add(new SealDraw(sprite,sc-13,y));r.put("seal-"+i,new int[]{sc-13,37,27,31});
        }
    }

    /** The group cards, stacked up from 18 px above the canvas bottom on the iron spine, infantry first. A group the round never
     *  fielded has no card. The card that carries the key row (the selected group's, or the bottom one while the whole army is
     *  selected) is 62 px tall, the rest 48. */
    static void cards(DrawContext g,TextRenderer font,int h,Map<String,int[]> r,List<CardDraw> cd,Map<String,String> tx){
        String sel=CommandMode.selected;boolean army=sel.equals("all");
        List<CommandMode.Group> shown=new ArrayList<>();
        for(String id:Morale.GROUPS)for(var grp:CommandMode.groups)if(grp.id().equals(id)&&grp.full()>0){shown.add(grp);break;}
        if(shown.isEmpty())return;
        int n=shown.size();int[] heights=new int[n];
        for(int i=0;i<n;i++)heights[i]=(army?i==n-1:sel.equals(shown.get(i).id()))?62:48;
        int[] tops=UiMath.cardTops(h,heights);
        int first=tops[0],last=tops[n-1]+heights[n-1];
        UiTex.three(g,"hud/spine",12,first-4,last-first+8);r.put("spine",new int[]{12,first-4,9,last-first+8});
        for(int i=0;i<n;i++)card(g,font,shown.get(i),24,tops[i],heights[i]==62,army||sel.equals(shown.get(i).id()),r,cd,tx);
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
        if(tall){ // the order keys, each in an ink chip as wide as its key's name
            int[] at={10,58,105};String[] ids={"attack","follow","formation"},words={"Attack","Follow","Formation"};
            for(int k=0;k<3;k++){
                String key=GravesAndGloryClient.hint(ids[k]);int kw=UiMath.chipWidth(font.getWidth(key)),kx=x+at[k];
                UiTex.three(g,"hud/key-chip",kx,y+44,kw);
                g.drawText(font,key,kx+2,y+46,DarkUi.CHIP_TEXT,false);
                g.drawText(font,words[k],kx+kw+2,y+46,DarkUi.SEPIA,false);
            }
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
            wrappedJoined=String.join(" ",wrappedLines);wrappedCopy=copy;wrappedStyle=style;
        }
        return wrappedLines;
    }

    /** hud/lesson-card at any height. Its corner brackets run down the side edges to rows 19 and 22 of the PNG, so rows 20 and 21
     *  are the only rows with a plain edge, and the card grows between them: the PNG's top 21 rows; then the extra rows, with the
     *  plain edge (rows 20 and 21) in the 14 columns at either side and plain vellum (rows 21 to 30) between them; then the rest of
     *  the PNG (the bottom brackets, the edge and the shadow), lowered by the extra rows. */
    static void lessonCard(DrawContext g,int x,int y,int h){
        int extra=h-40;
        if(extra<=0){UiTex.draw(g,"hud/lesson-card",x,y);return;}
        var e=UiTex.entry("hud/lesson-card");int px=x-e.ox(),py=y-e.oy(),split=21,side=14;
        UiTex.part(g,"hud/lesson-card",px,py,0,0,e.w(),split);
        for(int o=0;o<extra;o+=2){
            UiTex.part(g,"hud/lesson-card",px,py+split+o,0,20,side,Math.min(2,extra-o));
            UiTex.part(g,"hud/lesson-card",px+e.w()-side,py+split+o,e.w()-side,20,side,Math.min(2,extra-o));
        }
        for(int o=0;o<extra;o+=10)UiTex.part(g,"hud/lesson-card",px+side,py+split+o,side,21,e.w()-2*side,Math.min(10,extra-o));
        UiTex.part(g,"hud/lesson-card",px,py+split+extra,0,split,e.w(),e.h()-split);
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
