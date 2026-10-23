package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The camp's Task 12 parts, kept apart from CampScreen (which the UI plan restyles) so CampScreen only calls in: the roll result
 * card, the recruit cards' actions (a chosen buy, an Epic's or Legendary's star, the Legendary unlock) and their click to the troop
 * page, the troop page itself (stars, Mastery bars, the level names, the three ability lines, beats and beaten by, the counter
 * lines), and the House picker the contracts and chapter pages claim shards with. Every number comes from the sheets.
 */
final class RecruitPages {
    private RecruitPages() {}

    // ---- the recruit cards: their action and their click to the troop page ----
    /** One card action: its label, the command it sends, whether it can be sent now, and its tooltip. */
    record Action(String label,String command,boolean active,String tip){}
    /**
     * The action a recruit card offers for a troop, or null for none: a Common or Rare is bought for recruitment.chosenCost (until full
     * stars); an owned Epic or Legendary takes its next star for its House's shards; a Legendary not owned is unlocked by a Pact, or
     * by recruitment.legendaryShards shards at recruitment.renownForLegendary Renown. An Epic not owned comes only from rolls.
     */
    static Action action(Spec.TroopsRow t,Profile p){
        var R=Spec.RECRUITMENT[0];var o=p.owned.get(t.id());boolean full=o!=null&&o.stars>=R.maxStars();String house=RosterMath.houseName(t.house());
        int shards=p.shards.getOrDefault(t.house(),0),renown=p.renown.getOrDefault(t.house(),0);
        switch(t.rarity()){
            case "common","rare"->{if(full)return null;int cost=R.chosenCost();
                return new Action("Buy "+cost,"buy:"+t.id(),p.gold>=cost,"Recruit "+t.name()+" for "+cost+" gold: "+(o==null?"it joins your collection.":"a copy toward its next star."));}
            case "epic","legendary"->{
                if(o!=null){if(full)return null;var r=RosterMath.rarityRow(t.rarity());int cost=r==null?0:o.stars<=1?r.shardStar2():r.shardStar3();
                    return new Action("Star "+(o.stars+1),"star:"+t.id(),shards>=cost,"Star "+(o.stars+1)+" of "+t.name()+" for "+cost+" "+house+" shards. You have "+shards+".");}
                if(t.rarity().equals("epic"))return null;
                if(p.writs>0)return new Action("Pact","legendary:"+t.house()+":pact",true,"Spend your Pact: "+t.name()+" joins your collection.");
                boolean ready=renown>=R.renownForLegendary()&&shards>=R.legendaryShards();
                return new Action(R.legendaryShards()+" shards","legendary:"+t.house()+":shards",ready,t.name()+" answers a Pact, or "+R.legendaryShards()+" "+house+" shards at "+R.renownForLegendary()+" Renown. You have "+shards+" shards and "+renown+" Renown.");
            }
            default->{return null;}
        }
    }
    /** The action button's place on a card at (x, y, w, h). */
    static int[] actionBox(int x,int y,int w,int h){return new int[]{x+w-74,y+h-24,68,18};}
    /**
     * A card's action button, in the camp's button look (GravesAndGloryClient.FantasyButton's), drawn at z 200: the card under it
     * draws its dim and words at z 100 to 150 (over its portrait), and the button must sit over those.
     */
    static final class CardAction extends ButtonWidget {
        final Action action;
        CardAction(int[] box,Action a,Consumer<String> send){super(box[0],box[1],box[2],box[3],Text.literal(a.label()),b->send.accept(a.command()),DEFAULT_NARRATION_SUPPLIER);action=a;active=a.active();setTooltip(Tooltip.of(Text.literal(a.tip())));}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            boolean focus=active&&(isHovered()||isFocused());int x=getX(),y=getY();
            g.getMatrices().push();g.getMatrices().translate(0,0,200);
            g.fill(x,y,x+width,y+height,!active?0xff20272b:focus?0xff39464b:0xff252f34);g.drawBorder(x,y,width,height,focus?DarkUi.GOLD:0xff455156);
            var font=MinecraftClient.getInstance().textRenderer;var label=font.trimToWidth(DarkUi.body(getMessage().getString()),Math.max(1,width-8));
            g.drawText(font,net.minecraft.util.Language.getInstance().reorder(label),x+(width-font.getWidth(label))/2,y+(height-9)/2,!active?0xff879297:DarkUi.PAPER,false);
            g.getMatrices().pop();
        }
    }
    /** A whole card as a button that opens the troop page: no face of its own, a brass edge when hovered or focused. */
    static final class CardHit extends ButtonWidget {
        final String troop;
        CardHit(String troop,String name,int x,int y,int w,int h,Consumer<String> open){super(x,y,w,h,Text.literal(name),b->open.accept(troop),DEFAULT_NARRATION_SUPPLIER);this.troop=troop;}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){if(isHovered()||isFocused())g.drawBorder(getX(),getY(),width,height,DarkUi.GOLD);}
    }
    /**
     * The recruit page's card widgets for a House: each card's action button first (so a click on it is the action's, not the
     * card's), then each card as a CardHit. send sends a command; open opens the troop page.
     */
    static List<ClickableWidget> recruitWidgets(String house,Profile p,int x,int w,Consumer<String> send,Consumer<String> open){
        List<ClickableWidget> actions=new ArrayList<>(),cards=new ArrayList<>();var list=CampScreen.cards(house,p);int cw=w/3-10,ch=CARD_H;
        for(int i=0;i<list.size();i++){var t=list.get(i).troop();int cx=x+(i%3)*(w/3),cy=CARD_Y+(i/3)*(ch+8);
            var a=action(t,p);if(a!=null){int[] box=actionBox(cx,cy,cw,ch);var b=new CardAction(box,a,send);actions.add(b);}
            cards.add(new CardHit(t.id(),t.name(),cx,cy,cw,ch,open));}
        actions.addAll(cards);return actions;
    }
    static final int CARD_Y=152,CARD_H=96;
    /**
     * A recruit card's text rows (y from the card's top) under its name: the rarity, the stars' pips, the copies, LOCKED and the odds
     * line. A two-line name (a Legendary's title) closes them up rather than pushing them down, so every row ends above the action
     * button at y + CARD_H - 24 (review minor 1: at virtual width 817 three Legendary titles wrap).
     */
    record CardRows(int rarity,int pips,int copies,int locked,int odds){}
    static CardRows cardRows(boolean twoLine){return twoLine?new CardRows(32,44,56,44,56):new CardRows(24,40,54,44,58);}
    // ---- what was drawn, for the harness (suite screens reads it, never re-derives it) ----
    /** The last roll card drawn: its troop, its label, the stars it fills from and to, the pips filled, the pip growing (-1 none) and how far, and its outcome line. */
    record SeenRoll(String troop,String label,int from,int to,int filled,int growing,float part,String outcome){}
    /** The last troop page drawn: its troop, the stars drawn (-1 for a troop not owned), the level names, the three ability lines, the Mastery bars drawn, the beats and beaten-by lines and the counter rows drawn with their glyphs. */
    record SeenTroop(String troop,int stars,List<String> levels,List<String> abilities,int masteryBars,String beats,String beatenBy,List<String> glyphs,List<String> counts){}
    static volatile SeenRoll seenRoll;static volatile SeenTroop seenTroop;
    /** The roll card's last dim (x, y, w, h in CampScreen's space), the page it was drawn over, and whether the last camp frame drew the notice line. */
    static volatile int[] rollDim;static volatile String rollOn;static volatile boolean noticeShown;

    // ---- the roll result card ----
    /** Stars as pips: max boxes of size s, the first filled ones filled, the box at index growing filled by part (0 to 1) from its foot. */
    static void pips(DrawContext g,int x,int y,int s,int gap,int filled,int growing,float part){
        for(int i=0;i<Spec.RECRUITMENT[0].maxStars();i++){int px=x+i*(s+gap);g.drawBorder(px,y,s,s,DarkUi.GOLD);
            if(i<filled)g.fill(px+2,y+2,px+s-2,y+s-2,DarkUi.GOLD);
            else if(i==growing&&part>0){int top=y+s-2-Math.round((s-4)*part);g.fill(px+2,top,px+s-2,y+s-2,0xffe6c98d);}}
    }
    /** The copies a troop holding needs for its next star (RosterMath.starFromCopies), or 0 at full stars. */
    static int nextStarCopies(Profile.Owned o,Spec.RaritiesRow r){
        if(o==null||r==null||o.stars>=RosterMath.maxStars())return 0;
        for(int c=o.copies+1;c<64;c++)if(RosterMath.starFromCopies(c,r)>o.stars)return c;return 0;
    }
    /**
     * The roll result card over the recruit or troop page (the reveal's focal point): the whole screen behind dimmed (CampScreen passes it); the portrait on the House
     * accent; NEW RECRUIT or DUPLICATE; the name and rarity; the stars as large pips filling from starsBefore to starsAfter over
     * CampaignClient.ROLL_FILL_MS; and what the copy did. Skip reveal ends it.
     */
    static void rollCard(DrawContext g,TextRenderer f,int areaX,int areaY,int areaW,int areaH){
        var r=CampaignClient.lastRoll;var t=r==null?null:ArmyCards.spec(r.troop());if(t==null)return;
        g.getMatrices().push();g.getMatrices().translate(0,0,300);
        g.fill(areaX,areaY,areaX+areaW,areaY+areaH,0xe60d1215);rollDim=new int[]{areaX,areaY,areaW,areaH}; // 90%: the page behind reads as shapes, not words
        int cw=Math.min(420,areaW-40),ch=156,cx=areaX+(areaW-cw)/2,cy=areaY+(areaH-ch)/2;
        DarkUi.frame(g,cx,cy,cw,ch);g.drawBorder(cx,cy,cw,ch,DarkUi.GOLD);g.drawBorder(cx+2,cy+2,cw-4,ch-4,0xff5a4a30);
        var row=ArmyCards.house(t);int accent=row==null?0x4c9391:row.accent()&0xffffff;
        int px=cx+14,py=cy+14,pw=104,ph=ch-28;g.fill(px,py,px+pw,py+ph,0xff141a1d);g.fillGradient(px+1,py+1,px+pw-1,py+ph-1,0x55000000|accent,0x10181820);g.drawBorder(px,py,pw,ph,0xff455156);
        DarkUi.model(g,t,px+pw/2,py+ph-10,40); // at z 350: over the card's face, under its words
        g.getMatrices().translate(0,0,100);
        int tx=px+pw+16,room=cx+cw-14-tx;
        String label=r.duplicate()?"DUPLICATE":"NEW RECRUIT";DarkUi.title(g,label,tx,cy+18,DarkUi.GOLD);
        g.drawText(f,f.trimToWidth(t.name(),room),tx,cy+42,DarkUi.PAPER,false);
        g.drawText(f,f.trimToWidth(CampScreen.cap(t.rarity())+" / "+RosterMath.houseName(t.house()),room),tx,cy+54,CampScreen.rarityColour(t.rarity()),false);
        long at=CampaignClient.rollElapsed();int from=r.starsBefore(),to=Math.max(from,r.starsAfter());
        int filled=from,growing=-1;float part=0;
        if(to>from){float span=(float)CampaignClient.ROLL_FILL_MS/(to-from);int k=(int)Math.min(to-from,at/span);filled=from+k;if(filled<to){growing=filled;part=Math.min(1,(at-k*span)/span);}}
        pips(g,tx,cy+72,16,6,filled,growing,part);
        var o=CampaignClient.profile.owned.get(t.id());String what;int colour=DarkUi.GOLD;
        if(r.shards()>0)what="+"+r.shards()+" "+RosterMath.houseName(t.house())+" shards: already at full stars";
        else if(!r.duplicate())what="Joined your collection";
        else if(to>from)what="Star "+from+" to star "+to;
        else{int need=nextStarCopies(o,RosterMath.rarityRow(t.rarity()));what=o==null?"Another copy":need>0?"Copy "+o.copies+" of "+need+" toward star "+(o.stars+1):"Another copy";colour=DarkUi.PAPER;}
        g.drawText(f,f.trimToWidth(what,room),tx,cy+100,colour,false);seenRoll=new SeenRoll(t.id(),label,from,to,filled,growing,part,what);
        g.drawText(f,f.trimToWidth(r.duplicate()?"Each star adds to its health and damage.":"Place it in your banner in the war council.",room),tx,cy+116,DarkUi.MUTED,false);
        g.drawText(f,f.trimToWidth("Click anywhere to continue",room),tx,cy+ch-22,DarkUi.MUTED,false);
        g.getMatrices().pop();
    }

    // ---- the troop page ----
    /** The three level names of a troop (in-match levels I to III): its evolutions rows' names, or its own name at each for a troop with no line. */
    static List<String> levelNames(Spec.TroopsRow t){return List.of(ArmyMobs.stageName(t,1),ArmyMobs.stageName(t,2),ArmyMobs.stageName(t,3));}
    /** The three ability lines: the abilities row's text (level I), then the troopNotes level 2 and level 3 lines. */
    static List<String> abilityLines(Spec.TroopsRow t){
        String base=t.ability();for(var a:Spec.ABILITIES)if(a.id().equals(t.ability()))base=a.text();var n=ArmyCards.notes(t);
        return List.of(base,n==null?"":n.level2(),n==null?"":n.level3());
    }
    /** What a Mastery level gives, worded for the card from its mastery.reward cell. */
    static String masteryReward(Spec.MasteryRow m){
        List<String> out=new ArrayList<>();
        for(String part:m.reward().split(";")){String[] kv=part.split(":",2);if(kv.length<2)continue;
            out.add(switch(kv[0]){case "title"->kv[1]+" title";case "skin"->kv[1].equals("veteran")?"Veteran trim":CampScreen.cap(kv[1])+" look";case "frame"->CampScreen.cap(kv[1])+" frame";default->CampScreen.cap(kv[1]);});}
        return String.join(", ",out);
    }
    /** A thin progress bar: its track, its fill to have of want, brass when full. */
    static void bar(DrawContext g,int x,int y,int w,int have,int want){
        g.fill(x,y,x+w,y+4,0xff29383c);int fill=want<=0?w:(int)Math.min(w,(long)w*have/want);g.fill(x,y,x+fill,y+4,have>=want?DarkUi.GOLD:0xff8a9a9c);
    }
    /** Lines of text wrapped to w, at most max (the last trimmed). */
    static List<net.minecraft.text.OrderedText> wrap(TextRenderer f,String s,int w,int max){var lines=f.wrapLines(Text.literal(s),w);return lines.size()>max?lines.subList(0,max):lines;}
    /** A small section head: the word in muted caps and a rule under the column. */
    static void head(DrawContext g,TextRenderer f,String s,int x,int y,int w){g.drawText(f,s,x,y,DarkUi.MUTED,false);DarkUi.rule(g,x+f.getWidth(s)+6,y+4,w-f.getWidth(s)-6);}
    /** The troop page's left column width; the right column starts LEFT + 16 in. */
    static final int LEFT=150;
    /**
     * The troop page: on the left the portrait on its House accent, the name, rarity and group, the stars and what the next star
     * takes; on the right the three level names, the three ability lines, the three Mastery levels with their kill and battle
     * bars, and the matchups (beats, beaten by, and the counter lines with their crossed swords). y is the content top.
     */
    static void troopPage(DrawContext g,TextRenderer f,Spec.TroopsRow t,Profile p,int x,int y,int w){
        var o=p.owned.get(t.id());var row=ArmyCards.house(t);int accent=row==null?0x4c9391:row.accent()&0xffffff;
        g.fill(x,y,x+LEFT,y+128,0xff141a1d);g.fillGradient(x+1,y+1,x+LEFT-1,y+127,0x44000000|accent,0x10181820);g.drawBorder(x,y,LEFT,128,0xff455156);
        DarkUi.model(g,t,x+LEFT/2,y+118,48);
        g.getMatrices().push();g.getMatrices().translate(0,0,150);
        if(o==null)g.fill(x+1,y+1,x+LEFT-1,y+127,0x88111119);
        var name=wrap(f,t.name(),LEFT,2);int ny=y+138;for(var l:name){g.drawText(f,l,x,ny,DarkUi.GOLD,false);ny+=10;}
        g.drawText(f,f.trimToWidth(CampScreen.cap(t.rarity())+" / "+Morale.label(t.group()),LEFT),x,ny+2,CampScreen.rarityColour(t.rarity()),false);
        int sy=ny+18;
        if(o!=null){pips(g,x,sy,10,4,o.stars,-1,0);
            var r=RosterMath.rarityRow(t.rarity());int need=nextStarCopies(o,r);boolean copies=t.rarity().equals("common")||t.rarity().equals("rare");
            String next=o.stars>=RosterMath.maxStars()?"Full stars":copies?(need>0?o.copies+" of "+need+" copies to star "+(o.stars+1):o.copies+" copies"):"Star "+(o.stars+1)+": "+(r==null?0:o.stars<=1?r.shardStar2():r.shardStar3())+" shards";
            g.drawText(f,f.trimToWidth(next,LEFT),x,sy+16,DarkUi.MUTED,false);}
        else g.drawText(f,f.trimToWidth("Not in your collection",LEFT),x,sy,DarkUi.MUTED,false);
        // the right column
        int rx=x+LEFT+16,rw=w-LEFT-16,ry=y;
        head(g,f,"LEVELS",rx,ry,rw);ry+=13;var names=levelNames(t);boolean line=!names.get(0).equals(names.get(2));
        if(line)for(int i=0;i<3;i++){int cx=rx+i*(rw/3);String n=DarkUi.roman(i+1);g.drawText(f,n,cx,ry,DarkUi.GOLD,false);g.drawText(f,f.trimToWidth(names.get(i),rw/3-f.getWidth(n)-10),cx+f.getWidth(n)+4,ry,DarkUi.PAPER,false);}
        else{g.drawText(f,"I  II  III",rx,ry,DarkUi.GOLD,false);g.drawText(f,f.trimToWidth(names.get(0)+" at every level",rw-60),rx+f.getWidth("I  II  III")+6,ry,DarkUi.PAPER,false);}
        ry+=18;head(g,f,"ABILITY",rx,ry,rw);ry+=13;var lines=abilityLines(t);
        for(int i=0;i<3;i++){String n=DarkUi.roman(i+1);g.drawText(f,n,rx,ry,DarkUi.GOLD,false);
            var wrapped=wrap(f,lines.get(i).isEmpty()?"No change at this level":lines.get(i),rw-20,2);int ly=ry;for(var l:wrapped){g.drawText(f,l,rx+20,ly,i==0?DarkUi.PAPER:0xffd9d3c7,false);ly+=10;}ry=Math.max(ry+12,ly+2);}
        ry+=4;head(g,f,"MASTERY",rx,ry,rw);ry+=13;int level=o==null?0:o.mastery,bars=0;
        int col=0;for(var m:Spec.MASTERY)col=Math.max(col,f.getWidth(masteryReward(m)));col+=12;
        int kw=f.getWidth("400/400 kills"),bw=f.getWidth("60/60 survived"),room=Math.max(48,rw-20-col-kw-bw-6-6-10),killBar=room*56/96,fightBar=room-killBar;List<String> counts=new ArrayList<>();
        for(var m:Spec.MASTERY){boolean reached=level>=m.level();String n=DarkUi.roman(m.level());
            g.drawText(f,n,rx,ry,reached?DarkUi.GOLD:DarkUi.MUTED,false);g.drawText(f,masteryReward(m),rx+20,ry,reached?DarkUi.PAPER:DarkUi.MUTED,false);
            int kills=o==null?0:o.kills,fought=o==null?0:o.survived;int bx=rx+20+col;
            bar(g,bx,ry+3,killBar,kills,m.kills());bars++;g.drawText(f,Math.min(kills,m.kills())+"/"+m.kills()+" kills",bx+killBar+6,ry,DarkUi.MUTED,false);
            int bx2=bx+killBar+6+kw+10;bar(g,bx2,ry+3,fightBar,fought,m.matches());bars++;String count=Math.min(fought,m.matches())+"/"+m.matches()+" survived";g.drawText(f,count,bx2+fightBar+6,ry,DarkUi.MUTED,false);counts.add(count);
            ry+=13;}
        ry+=5;head(g,f,"MATCHUPS",rx,ry,rw);ry+=13;var notes=ArmyCards.notes(t);
        if(notes!=null){for(String[] kv:new String[][]{{"Beats",notes.beats()},{"Beaten by",notes.beatenBy()}}){int lw=f.getWidth("Beaten by")+8;g.drawText(f,kv[0],rx,ry,DarkUi.GOLD,false);
            var wrapped=wrap(f,kv[1],rw-lw,2);for(var l:wrapped){g.drawText(f,l,rx+lw,ry,DarkUi.PAPER,false);ry+=10;}ry+=2;}}
        ry+=3;var glyphs=ArmyCards.counterLines(g,f,t,rx,ry,rw);
        g.getMatrices().pop();
        seenTroop=new SeenTroop(t.id(),o==null?-1:o.stars,names,lines,bars,notes==null?"":notes.beats(),notes==null?"":notes.beatenBy(),glyphs,counts);
    }

    // ---- the House picker of the contracts and chapter pages ----
    /** A House chip: the House's glyph on a small tile, the chosen one edged and underlined in brass; the tooltip names the House. */
    static final class HouseChip extends ButtonWidget {
        final String house;final Supplier<String> chosen;
        HouseChip(String house,int x,int y,Supplier<String> chosen,Consumer<String> pick){super(x,y,CHIP,CHIP,Text.literal(RosterMath.houseName(house)),b->pick.accept(house),DEFAULT_NARRATION_SUPPLIER);this.house=house;this.chosen=chosen;setTooltip(Tooltip.of(Text.literal("Shards from claims go to the "+RosterMath.houseName(house))));}
        boolean selected(){return house.equals(chosen.get());}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();boolean on=selected(),focus=isHovered()||isFocused();
            g.fill(x,y,x+width,y+height,on?0xff30474a:focus?0xff39464b:0xff252f34);g.drawBorder(x,y,width,height,on||focus?DarkUi.GOLD:0xff455156);
            for(var row:Spec.HOUSES)if(row.id().equals(house))g.drawTexture(new Identifier(row.glyph()),x+(width-16)/2,y+(height-16)/2,0f,0f,16,16,16,16);
            if(on)g.fill(x+2,y+height-3,x+width-2,y+height-1,DarkUi.GOLD);
        }
    }
    static final int CHIP=24,CHIP_GAP=4;
    /** The five House chips, right-aligned to right at y. */
    static List<HouseChip> houseChips(int right,int y,Supplier<String> chosen,Consumer<String> pick){
        List<HouseChip> out=new ArrayList<>();int n=Spec.HOUSES.length,x=right-n*CHIP-(n-1)*CHIP_GAP;
        for(int i=0;i<n;i++)out.add(new HouseChip(Spec.HOUSES[i].id(),x+i*(CHIP+CHIP_GAP),y,chosen,pick));return out;
    }
    /** The picker's caption, drawn left of the chips: "Shards to <House>". Returns the text. */
    static String chipCaption(String house){return "Shards to "+RosterMath.houseName(house);}
    /** Draws the caption right-aligned before the chips. */
    static void caption(DrawContext g,TextRenderer f,String house,int right,int y){
        int n=Spec.HOUSES.length,x=right-n*CHIP-(n-1)*CHIP_GAP-10;String s=chipCaption(house);g.drawText(f,s,x-f.getWidth(s),y+(CHIP-8)/2,DarkUi.PAPER,false);
    }
    /** The shards a contract line adds to its rewards: the daily that completes all three and the weekly pay shards to the chosen House ("" for the others). */
    static String contractShards(Progression.State s,int slot){
        if(slot<0)return " + "+Progression.WEEKLY_SHARDS+" shards";
        boolean last=true;for(int i=0;i<3;i++)if(i!=slot&&!s.claims.contains("daily:"+i))last=false;
        return last?" + "+Progression.DAILY_SHARDS+" shards":"";
    }
    /** A daily or weekly claim with the chosen House (Progression.claim reads it only where shards are paid). */
    static String contractClaim(String key,String house){return "progress:"+key+":"+house;}
    /** A Founders claim: a shard tier with the chosen House, tier 20 with the chosen Rare, any other tier bare. */
    static String foundersClaim(int tier,String house,String rare){
        if(Progression.SHARD_TIERS.contains(tier))return "progress:founders:"+tier+":"+house;
        if(tier==Progression.RARE_TIER)return "progress:founders:"+tier+":"+rare;
        return "progress:founders:"+tier;
    }
    /** The Rares of a House in sheet order: tier 20's choice. */
    static List<String> rares(String house){List<String> out=new ArrayList<>();for(var t:Spec.TROOPS)if(t.house().equals(house)&&t.rarity().equals("rare"))out.add(t.id());return out;}
    static TextRenderer font(){return MinecraftClient.getInstance().textRenderer;}
    /** What CampScreen.state adds so its pages rebuild when a buy, a star or an unlock lands: shards, Pacts, Renown and each holding's copies, stars and mastery. */
    static String stateKey(Profile p){
        StringBuilder sb=new StringBuilder().append(p.shards).append(p.writs).append(p.renown);
        for(var e:new TreeMap<>(p.owned).entrySet())sb.append(e.getKey()).append(e.getValue().copies).append('/').append(e.getValue().stars).append('/').append(e.getValue().mastery).append(';');
        return sb.toString();
    }
}
