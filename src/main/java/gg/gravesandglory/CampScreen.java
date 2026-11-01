package gg.gravesandglory;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.IntPredicate;
import static gg.gravesandglory.GravesAndGloryClient.*;

/** The War Table: home, the recruit page and WarTablePages' banner, contracts, chapter, friends and field, all in canvas pixels;
 *  the battle report (results) keeps the old scaled layout until its own task. */
final class CampScreen extends ArmyScreen {
    String page;float scale=1;int offset;String key="";long requestAt;
    /** The War Table home (page home), kept while this screen lives so its focus and animation survive a rebuild. */
    WarTableHome home;
    /** The War Table recruit page (page recruit), kept while the page is open; leaving the page drops it, so a later visit opens on the first House. */
    WarTableRecruit recruit;
    /** The War Table troop sheet (page troop), kept while the page is open; leaving the page drops it. */
    WarTableTroop troopSheet;
    /** The War Table lobby (page lobby), kept while the page is open; leaving the page drops it. */
    WarTableLobby lobby;
    /** The War Table's other pages (UI plan Task 7), kept while the page is open, so the armed troop, the track's place and a room code being written survive a rebuild; leaving the page drops it. */
    WarTablePages pages;
    /** The keys whose press a War Table page took, not released since, and the watched keys already down when the screen opened (the key that opened it, say). Minecraft hands a screen each repeat of a held key as another keyPressed: those go no further, on whatever page the press left the table at (a held Esc that went home must not then close it, a held Enter must not ride again, the menu key that opened the table must not close it), until keyReleased lets the key through again. */
    final Set<Integer> held=new HashSet<>();
    /** Whether a key is down now, as GLFW has it (the checks stand in for it). */
    static IntPredicate keyDown=k->InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(),k);
    CampScreen(String page){super("Banner camp");this.page=alias(page);for(int k:watched())if(keyDown.test(k))held.add(k);}
    /** The keys the War Table acts on (its pages' keys, Tab, the arrows) and the menu key, which closes it. */
    static List<Integer> watched(){
        List<Integer> keys=new ArrayList<>(List.of(GLFW.GLFW_KEY_ESCAPE,GLFW.GLFW_KEY_ENTER,GLFW.GLFW_KEY_KP_ENTER,GLFW.GLFW_KEY_SPACE,GLFW.GLFW_KEY_TAB,GLFW.GLFW_KEY_B,GLFW.GLFW_KEY_LEFT,GLFW.GLFW_KEY_RIGHT,GLFW.GLFW_KEY_UP,GLFW.GLFW_KEY_DOWN));
        if(menu!=null){var bound=InputUtil.fromTranslationKey(menu.getBoundKeyTranslationKey());if(bound.getCategory()==InputUtil.Type.KEYSYM&&bound.getCode()>=0&&!keys.contains(bound.getCode()))keys.add(bound.getCode());}
        return keys;
    }
    /** The page a name opens: the old names still sent (the war gates' match is the field, the private room is on Friends, the Quartermaster's army is the Banner), else the name. */
    static String alias(String page){return switch(page){case "match"->"field";case "room"->"friends";case "army"->"banner";default->page;};}
    /** The House a station named (recruit:<house>, a tent of the House ring), which the recruit page opens on; "" for none. */
    String stationHouse="";
    /** A station: a page, or page:house (the House ring's tents send recruit:<house>), split on the first colon. */
    static void open(String station){int at=station.indexOf(':');var s=new CampScreen(at<0?station:station.substring(0,at));if(at>=0){s.stationHouse=station.substring(at+1);for(var h:Spec.HOUSES)if(h.id().equals(s.stationHouse))s.house=h.id();}MinecraftClient.getInstance().setScreen(s);}
    /** The House whose six cards the recruit page shows and whose pool a roll draws from; its tab row changes it. It starts on the House the banner leans on. */
    String house=RosterMath.rollHouse(CampaignClient.profile.banner);
    final List<HouseTab> tabs=new ArrayList<>();
    /** Task 12 (RecruitPages): the troop the troop page shows, the House the contracts and chapter pages claim shards for, and tier 20's Rare. */
    String troop="",claimHouse=house,rare=RecruitPages.rares(house).isEmpty()?"":RecruitPages.rares(house).get(0);
    JsonObject social(){return CampaignClient.social;}
    String value(String name){return social().has(name)?social().get(name).getAsString():"";}
    int number(String name,int fallback){return social().has(name)?social().get(name).getAsInt():fallback;}
    JsonArray rows(String name){return social().has(name)?social().getAsJsonArray(name):new JsonArray();}
    /** Sends cmd to the server unless a command went less than 300 ms ago; returns whether it went. */
    boolean action(String cmd){if(System.currentTimeMillis()-requestAt<300)return false;requestAt=System.currentTimeMillis();CampaignClient.notice="";CampaignClient.dismissed="";send(cmd);return true;} // what the player does next speaks anew: a notice he left behind by changing page shows again if the server answers with the same words (review fix round 2)
    /** Turns the table to another page (an old name goes through alias); the keyboard's focus starts afresh there. */
    void page(String next){if(CampaignClient.rollShowing())CampaignClient.revealUntil=0;CampaignClient.dismissed=CampaignClient.notice;page=alias(next);offset=0;if(!page.equals("recruit"))recruit=null;if(!page.equals("troop"))troopSheet=null;if(!page.equals("lobby"))lobby=null;if(pages!=null&&!pages.page.equals(page))pages=null;super.clearAndInit();}
    void rebuild(){clearAndInit();}
    /** The pages drawn at UiMath.pageScale, so they fill a 1440p window (Kort, 2026-10-31): every War Table page (home, recruit,
     *  the troop sheet, the lobby and WarTablePages'), so the rail keeps one size from page to page. The old report pages keep pixelScale. */
    boolean fillsWindow(){return isWarTable(page);}
    static boolean isWarTable(String page){return "home".equals(page)||"recruit".equals(page)||"troop".equals(page)||"lobby".equals(page)||WarTablePages.NAMES.contains(page);}
    UiButton add(UiButton b){return addDrawableChild(b);}
    String state(){return CampaignClient.social.toString()+CampaignClient.progression.toString()+CampaignClient.profile.gold+CampaignClient.profile.owned.keySet()+CampaignClient.profile.pity+CampaignClient.profile.campaignCleared+CampaignClient.difficulty+running+ready+RecruitPages.stateKey(CampaignClient.profile);}
    /** The widgets of the War Table page shown, by name: home's, the recruit page's or WarTablePages'; none on the report. */
    Map<String,UiButton> widgets(){
        if(page.equals("home")&&home!=null)return home.buttons;
        if(page.equals("recruit")&&recruit!=null)return recruit.buttons;
        if(page.equals("troop")&&troopSheet!=null)return troopSheet.buttons;
        if(page.equals("lobby")&&lobby!=null)return lobby.buttons;
        if(pages!=null&&pages.page.equals(page))return pages.buttons;
        return Map.of();
    }
    /** A rebuild (the camp state or a badge changed, the window was resized) makes every widget anew; the keyboard's focus goes back to the widget of the same name, so Tab keeps its place. */
    @Override protected void clearAndInit(){
        String name=null;var focused=getFocused();
        for(var e:widgets().entrySet())if(focused!=null&&e.getValue()==focused)name=e.getKey();
        super.clearAndInit();
        var again=name==null?null:widgets().get(name);
        if(again!=null&&again.visible&&again.active)super.setFocused(again); // as it was: no sheet rises and Ride does not glint again
    }
    @Override protected void init(){
        if(page.equals("home")){scale=1;key=state();if(home==null)home=new WarTableHome(this);home.init();return;} // the War Table draws in canvas pixels
        if(page.equals("recruit")){scale=1;key=state();if(recruit==null)recruit=new WarTableRecruit(this);recruit.init();return;}
        if(page.equals("troop")){scale=1;key=state();if(troopSheet==null)troopSheet=new WarTableTroop(this);troopSheet.init();return;}
        if(page.equals("lobby")){scale=1;key=state();if(lobby==null)lobby=new WarTableLobby(this);lobby.init();return;}
        if(WarTablePages.NAMES.contains(page)){scale=1;key=state();if(pages==null||!pages.page.equals(page))pages=new WarTablePages(this,page);pages.init();return;}
        scale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/760f,client.getWindow().getScaledHeight()/460f));width=(int)(client.getWindow().getScaledWidth()/scale);height=(int)(client.getWindow().getScaledHeight()/scale);key=state();
        String[] names={"Camp guide","Recruitment","Friends","Contracts","Chapter rewards"};String[] ids={"home","recruit","friends","contracts","chapter"};
        for(int i=0;i<ids.length;i++){String id=ids[i];button(names[i],18,94+i*34,150,b->page(id));}
        button("Army & equipment",18,310,150,b->page("banner"));button("Visuals & sound",18,340,150,b->client.setScreen(new PresentationScreen()));button("Return to camp",18,height-42,150,b->close());
        int x=194,w=width-x-22;
        if(CampaignClient.result.has("id"))button("Last battle",18,370,150,b->page("results"));
        if(page.equals("troop")){
            var t=ArmyCards.spec(troop);var a=t==null?null:RecruitPages.action(t,CampaignClient.profile);
            if(a!=null){var go=primary(a.label().toUpperCase(java.util.Locale.ROOT),x,height-114,RecruitPages.LEFT,b->action(a.command()));go.active=a.active()&&(!running||CampaignClient.waiting);go.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(a.tip())));}
            button("Back to recruitment",x,height-82,RecruitPages.LEFT,b->page("recruit"));
        }else if(page.equals("results")){
            button("Claim contracts",x,height-84,w/3-8,b->page("contracts"));button("Chapter rewards",x+w/3,height-84,w/3-8,b->page("chapter"));
            primary(value("room").isEmpty()?"BACK TO CAMP":"RETURN TO ROOM",x+2*w/3,height-88,w/3,b->page(value("room").isEmpty()?"home":"friends"));
        }else if(page.equals("train")){
            // The Drillmaster: battle 1 (the first Long March battle) and the practice skirmish.
            var start=primary("START",x,200,w/2-7,b->action("campaign:"+Spec.CAMPAIGNBATTLES[0].id()));start.active=!running;
            var practice=button("PRACTICE",x+w/2+7,203,w/2-7,b->action("camp:practice"));practice.active=!running; // a button is 20 high and a primary 26: 3 lower, so both labels sit on one line
        }
    }
    @Override public void tick(){if(home!=null&&page.equals("home"))home.tick();if(recruit!=null&&page.equals("recruit"))recruit.tick();if(troopSheet!=null&&page.equals("troop"))troopSheet.tick();if(lobby!=null&&page.equals("lobby"))lobby.tick();if(pages!=null&&pages.page.equals(page))pages.tick();if(!key.equals(state()))clearAndInit();if(running&&!CampaignClient.waiting)close();}
    /** The troops one recruitment roll in this House can give: its Commons, Rares and Epic in sheet order (Legendaries come from Pacts). */
    static List<Spec.TroopsRow> pool(String house){return Arrays.stream(Spec.TROOPS).filter(t->t.house().equals(house)&&!t.rarity().equals("legendary")).toList();}
    static int weight(Spec.TroopsRow t){var r=RosterMath.rarityRow(t.rarity());return r==null?0:r.rollWeight();}
    static String odds(List<Spec.TroopsRow> pool,String rarity,int total){int w=0;for(var t:pool)if(t.rarity().equals(rarity))w+=weight(t);return String.format(java.util.Locale.ROOT,"%.0f",100.0*w/Math.max(1,total))+"%";}
    static String cap(String s){return s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1);}
    /** The camp's header line: the earned title and the gold in hand. */
    String header(){return CampaignClient.profile.progression.title+"  /  "+CampaignClient.profile.gold+" gold";}
    /** The recruit page's roll button: ROLL <HOUSE> / <cost> GOLD for the chosen House. */
    String rollLabel(){return "ROLL "+RosterMath.houseName(house).toUpperCase(java.util.Locale.ROOT)+" / "+Spec.RECRUITMENT[0].rollCost()+" GOLD";}
    /** The pity line under the cards: rolls left until the chosen House's Epic is certain. */
    String pityLine(){return "Epic guaranteed within "+(Spec.RECRUITMENT[0].epicPity()-CampaignClient.profile.pity.getOrDefault(house,0))+" rolls";}
    /** The five House tabs of the recruit page, in House order (empty on other pages). */
    List<HouseTab> houseTabs(){return List.copyOf(tabs);}
    /** Shows another House's cards and rolls from its pool. */
    void pickHouse(String next){house=next;clearAndInit();}
    /** Opens the troop page of a troop (a recruit card's click). */
    void openTroop(String id){troop=id;page("troop");}
    /** The House the contracts and chapter pages' claims pay shards to; tier 20's Rare moves to that House's first. */
    void pickClaimHouse(String next){claimHouse=next;var r=RecruitPages.rares(next);rare=r.isEmpty()?"":r.get(0);clearAndInit();}
    /** Tier 20's Rare: the next Rare of the chosen House. */
    void cycleRare(){var r=RecruitPages.rares(claimHouse);if(!r.isEmpty())rare=r.get((r.indexOf(rare)+1)%r.size());clearAndInit();}
    /** One card of the recruit page: a troop of the House, whether the commander owns it, and its stars and copies. */
    record Card(Spec.TroopsRow troop,boolean owned,int stars,int copies){}
    /** The six troops of a House in sheet order, each with the commander's holding of it. */
    static List<Card> cards(String house,Profile p){
        List<Card> out=new ArrayList<>();
        for(var t:Spec.TROOPS)if(t.house().equals(house)){var o=p.owned.get(t.id());out.add(new Card(t,o!=null,o==null?0:o.stars,o==null?0:o.copies));}
        return out;
    }
    static int rarityColour(String rarity){return switch(rarity){case "rare"->DarkUi.RARE_INK;case "epic"->DarkUi.EPIC_INK;case "legendary"->DarkUi.GOLD;default->DarkUi.MUTED;};}
    /** The lowest pixel (from the card's top) the last card() call drew a word or a pip at, and whether the harness is probing (no portrait is drawn). Task 12 re-review N1: p2-card-layout reads what card() drew, not the row table. */
    int cardLow,cardTop;boolean inCard;static volatile boolean probe;
    /** The troops the last frames' cards drew and whether each was owned (the harness clears it, presses a House tab and reads it: Task 13, p2-full-roster). */
    static final Map<String,Boolean> seenCards=new java.util.concurrent.ConcurrentHashMap<>();
    /** A word of a card at dy under the card's top. line() notes every word drawn while a card is drawn (cardLow), so no row can bypass the layout check. */
    void cardLine(DrawContext g,String text,int x,int y,int dy,int color,int room){line(g,text,x,y+dy,color,room);}
    /** While card() draws: the lowest word or pip so far, as rows under the card's top (the harness reads it; p2-card-layout). */
    void ink(int y){if(inCard)cardLow=Math.max(cardLow,y-cardTop+9);}
    /** One troop card: the portrait on the House accent, the level 1 name, the rarity and the stars as pips; a troop the commander lacks is dimmed and says LOCKED with its odds. */
    void card(DrawContext g,Card card,int x,int y,int w,int h,int poolTotal){
        cardLow=0;cardTop=y;inCard=true;seenCards.put(card.troop().id(),card.owned());
        var troop=card.troop();DarkUi.frame(g,x,y,w,h);
        var row=ArmyCards.house(troop);g.fillGradient(x+3,y+3,x+w-3,y+h-3,DarkUi.CARD_SHADE|(row==null?0x4c9391:row.accent()&0xffffff),DarkUi.CARD_FADE);
        var portrait=ArmyCards.portrait(troop);float wide=portrait==null?0:portrait.getWidth()*1.25f; // a wide model (a spider's legs) is drawn smaller so it stays inside its card
        if(!probe)DarkUi.model(g,troop,x+34,y+h-12,wide>1.5f?(int)(28*1.5f/wide):28);
        g.getMatrices().push();g.getMatrices().translate(0,0,100); // the model is drawn at z 50 and writes depth, so what must show over it is nearer: the dim of a card the commander lacks at z 100, the card's text at z 150
        if(!card.owned())g.fill(x+3,y+3,x+w-3,y+h-3,DarkUi.CARD_DIM);
        g.getMatrices().translate(0,0,50);
        int tx=x+70,room=w-76;
        // A long name (a Legendary's title) wraps onto a second line, and everything under it moves down a line.
        var title=textRenderer.wrapLines(Text.literal(ArmyMobs.levelName(troop,1)),room);int more=title.size()>1?10:0;
        for(int i=0;i<Math.min(2,title.size());i++){g.drawText(textRenderer,title.get(i),tx,y+10+i*10,card.owned()?DarkUi.GOLD:DarkUi.MUTED,false);ink(y+10+i*10);}
        var rows=RecruitPages.cardRows(more>0);cardLine(g,cap(troop.rarity()),tx,y,rows.rarity(),rarityColour(troop.rarity()),room);
        if(card.owned()){
            for(int i=0;i<Spec.RECRUITMENT[0].maxStars();i++){int px=tx+i*11;g.drawBorder(px,y+rows.pips(),8,8,DarkUi.GOLD);if(i<card.stars())g.fill(px+1,y+rows.pips()+1,px+7,y+rows.pips()+7,DarkUi.GOLD);ink(y+rows.pips()-1);}
            cardLine(g,card.copies()+(card.copies()==1?" copy":" copies"),tx,y,rows.copies(),DarkUi.MUTED,room);
        }else{
            cardLine(g,"LOCKED",tx,y,rows.locked(),DarkUi.PAPER,room);
            cardLine(g,troop.rarity().equals("legendary")||weight(troop)<=0?"Comes from a Pact":String.format(java.util.Locale.ROOT,"%.1f",100.0*weight(troop)/Math.max(1,poolTotal))+"% a roll",tx,y,rows.odds(),DarkUi.MUTED,room);
        }
        g.getMatrices().pop();inCard=false;
    }
    /** One House's tab on the recruit page: its glyph and name, the chosen one edged in brass. */
    final class HouseTab extends ButtonWidget {
        final String house;
        HouseTab(String house,int x,int y,int w){super(x,y,w,28,Text.literal(RosterMath.houseName(house)),b->CampScreen.this.pickHouse(house),DEFAULT_NARRATION_SUPPLIER);this.house=house;}
        boolean selected(){return CampScreen.this.house.equals(house);}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();boolean chosen=selected(),focus=active&&(isHovered()||isFocused());
            g.fill(x,y,x+width,y+height,chosen?DarkUi.TAB_CHOSEN:focus?DarkUi.TAB_FOCUS:DarkUi.TAB_IDLE);g.drawBorder(x,y,width,height,chosen||focus?DarkUi.GOLD:DarkUi.TAB_EDGE);
            for(var row:Spec.HOUSES)if(row.id().equals(house))g.drawTexture(new Identifier(row.glyph()),x+6,y+6,0f,0f,16,16,16,16);
            var font=textRenderer;g.drawText(font,font.trimToWidth(getMessage().getString(),width-34),x+28,y+10,chosen?DarkUi.PAPER:DarkUi.MUTED,false);
            if(chosen)g.fill(x,y+height-2,x+width,y+height,DarkUi.GOLD);
        }
    }
    void line(DrawContext g,String text,int x,int y,int color,int max){ink(y);g.drawText(textRenderer, textRenderer.trimToWidth(text,max),x,y,color,false);}
    /** The battle report table from y down: COMMANDER / KILLS / LOST / GOLD / ON POINTS, one row per army (your team first, a rule before the
     *  enemy, each row's team mark in the team column, your row in brass, a crest on the MVP), then one line per point with the seconds each
     *  team held it. Returns the y under it. */
    int table(DrawContext g,JsonObject report,int x,int y,int w){
        if(!report.has("squads")||!report.has("points"))return y;
        JsonArray rows=report.getAsJsonArray("squads");String[] heads={"KILLS","LOST","GOLD","ON POINTS"},keys={"kills","lost","gold","objective"};int[] right={x+w-200,x+w-140,x+w-70,x+w};int name=right[0]-x-74;
        line(g,"COMMANDER",x+25,y,DarkUi.MUTED,name-11);for(int c=0;c<4;c++)g.drawText(textRenderer,heads[c],right[c]-textRenderer.getWidth(heads[c]),y,DarkUi.MUTED,false);
        DarkUi.rule(g,x,y+11,w);y+=14;
        List<MatchStats.SquadStats> tally=new ArrayList<>();for(var e:rows){var s=new MatchStats.SquadStats();s.kills=e.getAsJsonObject().get("kills").getAsInt();s.objectiveSeconds=e.getAsJsonObject().get("objective").getAsInt();tally.add(s);}
        int mvp=MatchStats.mvp(tally);
        for(int i=0;i<rows.size();i++,y+=11){var r=rows.get(i).getAsJsonObject();int color=r.get("you").getAsBoolean()?DarkUi.GOLD:DarkUi.PAPER;
            if(i>0&&r.get("team").getAsInt()!=rows.get(i-1).getAsJsonObject().get("team").getAsInt()){DarkUi.rule(g,x+14,y,w-14);y+=3;}
            if(i==mvp)DarkUi.crest(g,x,y-1,10,r.get("team").getAsInt()==1);
            DarkUi.teamMark(g,r.get("team").getAsInt(),x+14,y);line(g,r.get("name").getAsString()+(i==mvp?"  MVP":""),x+25,y,color,name-11);
            for(int c=0;c<4;c++){String v=r.get(keys[c]).getAsInt()+(c==3?"s":"");g.drawText(textRenderer,v,right[c]-textRenderer.getWidth(v),y,color,false);}
        }
        y+=3;
        for(var e:report.getAsJsonArray("points")){var p=e.getAsJsonObject();line(g,p.get("name").getAsString()+"  Dawn "+p.get("dawn").getAsInt()+"s / Dusk "+p.get("dusk").getAsInt()+"s",x,y,DarkUi.MUTED,w);y+=11;}
        return y;
    }
    /** One House row of the battle report: the House's name in its accent, then the banner's trait tier in it with the seconds the trait held, and the shards and Renown the match paid it. */
    void houseRow(DrawContext g,JsonObject h,int x,int y,int w){
        Spec.HousesRow row=null;for(var r:Spec.HOUSES)if(r.id().equals(h.get("house").getAsString()))row=r;if(row==null)return;
        int tier=h.get("tier").getAsInt();String name=row.name().toUpperCase(java.util.Locale.ROOT);
        line(g,name,x,y,DarkUi.OPAQUE|row.accent(),w);int at=x+textRenderer.getWidth(name)+8;
        line(g,(tier>0?"Trait "+MatchStats.roman(tier)+" held "+h.get("traitSeconds").getAsInt()+"s":"No trait tier")+"   +"+h.get("shards").getAsInt()+" shards   +"+h.get("renown").getAsInt()+" Renown",at,y,DarkUi.PAPER,x+w-at);
    }
    /** One troop type's levels row of the battle report (in-match progression design 7): the troop's name in its House's accent, then what it reached, its level-3 count and the gear its army bought (MatchStats.levelText; in the two-column form levelShort, right-aligned in its cell). */
    void levelRow(DrawContext g,JsonObject r,int x,int y,int cell,boolean compact){
        var t=ArmyCards.spec(r.get("troop").getAsString());var d=levelDrawn(textRenderer,r,cell,compact);if(t==null||d==null)return;
        int accent=DarkUi.PAPER;for(var h:Spec.HOUSES)if(h.id().equals(t.house()))accent=DarkUi.OPAQUE|h.accent();
        int nameWidth=textRenderer.getWidth(d[0]),statsWidth=textRenderer.getWidth(d[1]);
        line(g,d[0],x,y,accent,cell);
        if(compact)line(g,d[1],x+cell-statsWidth,y,DarkUi.PAPER,statsWidth);else line(g,d[1],x+nameWidth+8,y,DarkUi.PAPER,cell-nameWidth-8);
    }
    /** The levels block's form: the long line in one column up to three troop types, the short form in two columns of half a page past three (which keeps its height to half the rows). */
    static boolean levelCompact(int n){return n>3;}
    /** The width of one cell of the levels block when it has n rows on a page of width w, and the lines it takes. */
    static int levelCell(int n,int w){return levelCompact(n)?w/2-8:w;}
    static int levelLines(int n){return levelCompact(n)?(n+1)/2:n;}
    /** One levels row's words, untrimmed: the troop's name and what it reached, long or short (MatchStats.levelText, levelShort); null for a troop this client does not know. */
    static String[] levelWords(JsonObject r,boolean compact){
        var t=ArmyCards.spec(r.get("troop").getAsString());if(t==null)return null;
        var row=new MatchStats.LevelRow(t.id(),r.get("highestLevel").getAsInt(),r.get("level3Count").getAsInt(),r.get("weapon").getAsInt(),r.get("armour").getAsInt());
        String reached=LevelMath.levelName(t,row.highest());
        return new String[]{t.name(),compact?MatchStats.levelShort(row,t.name(),reached):MatchStats.levelText(row,t.name(),reached)};
    }
    /** What levelRow draws in a cell of width cell. The words after the name are never cut: in the short form the name gives way (an ellipsis), in the long form the harness checks that every troop fits the smallest page. */
    static String[] levelDrawn(net.minecraft.client.font.TextRenderer font,JsonObject r,int cell,boolean compact){
        var words=levelWords(r,compact);if(words==null)return null;
        if(!compact)return new String[]{font.trimToWidth(words[0],cell),font.trimToWidth(words[1],cell-font.getWidth(words[0])-8)};
        String stats=font.trimToWidth(words[1],cell);int room=cell-font.getWidth(stats)-8;
        return new String[]{font.getWidth(words[0])<=room?words[0]:font.trimToWidth(words[0],Math.max(0,room-font.getWidth("...")))+"...",stats};
    }
    @Override public void render(DrawContext g,int mx,int my,float delta){
        if(page.equals("home")&&home!=null){home.render(g);super.render(g,mx,my,delta);return;}
        if(page.equals("recruit")&&recruit!=null){recruit.render(g);super.render(g,mx,my,delta);recruit.overlay(g);return;} // the Odds note lies over the widgets
        if(page.equals("troop")&&troopSheet!=null){troopSheet.render(g);super.render(g,mx,my,delta);return;}
        if(page.equals("lobby")&&lobby!=null){lobby.render(g);super.render(g,mx,my,delta);return;}
        if(pages!=null&&pages.page.equals(page)){pages.render(g);super.render(g,mx,my,delta);pages.overlay(g);return;}
        g.getMatrices().push();g.getMatrices().scale(scale,scale,1);DarkUi.background(g,width,height);DarkUi.crest(g,20,16,26,false);DarkUi.title(g,"BANNER CAMP",58,19,DarkUi.GOLD);
        line(g,header(),width-280,24,DarkUi.PAPER,260);
        int x=194,w=width-x-22;DarkUi.frame(g,x-12,79,w+24,height-139);line(g,page.equals("results")?"BATTLE REPORT":page.equals("troop")?"TROOP CARD":page.equals("train")?"TRAINING YARD":"A PLACE TO PREPARE",x,91,DarkUi.GOLD,w);
        var progress=CampaignClient.profile.progression;
        if(page.equals("troop")){
            var t=ArmyCards.spec(troop);if(t!=null)RecruitPages.troopPage(g,textRenderer,t,CampaignClient.profile,x,108,w);
        }else if(page.equals("results")){
            var report=CampaignClient.result;
            if(report.has("id")){
                line(g,report.get("outcome").getAsString()+" / "+report.get("map").getAsString(),x,117,DarkUi.GOLD,w);
                var houses=report.has("houses")?report.getAsJsonArray("houses"):new JsonArray();int houseLines=houses.isEmpty()?0:houses.size()*11+3; // one line per House of the banner, under the gold
                var levels=report.has("levels")?report.getAsJsonArray("levels"):new JsonArray();int levelLines=levelLines(levels.size()); // then one per troop type: its levels and gear (two columns past three)
                int extra=houseLines+(levelLines==0?0:levelLines*11+3);
                int y=Math.max(table(g,report,x,132,w)+5,height-163-extra); // the battle table first; the rewards sit above the buttons, or right under a long table
                boolean saved=report.get("saved").getAsBoolean();
                line(g,saved?"+"+report.get("gold").getAsInt()+" GOLD     +"+report.get("xp").getAsInt()+" CHAPTER XP":"REWARDS NOT SAVED / Progress unchanged",x,y,saved?DarkUi.PAPER:DarkUi.ALARM_TEXT,w);
                line(g,!saved?"Check disk space before playing again.":report.get("eligible").getAsBoolean()?"Rewards saved. Contract and tier rewards are claimed separately.":"No match rewards: play at least 3 minutes to qualify.",x,y+11,DarkUi.MUTED,w);
                for(int i=0;i<houses.size();i++)houseRow(g,houses.get(i).getAsJsonObject(),x,y+24+i*11,w);
                for(int i=0;i<levels.size();i++){boolean two=levelCompact(levels.size());levelRow(g,levels.get(i).getAsJsonObject(),x+(two?i/levelLines*(w/2):0),y+24+houseLines+i%levelLines*11,levelCell(levels.size(),w),two);}
                y+=25+extra;int tier=Progression.tier(progress),xp=Math.min(progress.xp,7500),half=w/2-8,right=x+w-half; // chapter progress on the left, contracts on the right
                line(g,"FOUNDERS / Tier "+tier+" of 30 / "+xp+" XP",x,y,DarkUi.GOLD,half);
                g.fill(x,y+11,x+half,y+15,DarkUi.TRACK);g.fill(x,y+11,x+(int)(half*(tier==30?1:(xp%250)/250f)),y+15,DarkUi.GOLD);
                line(g,tier==30?"Chapter complete. Troops and titles stay yours.":"Next: tier "+(tier+1)+" / "+Progression.reward(tier+1)+" / "+(250-xp%250)+" XP to go",x,y+19,DarkUi.PAPER,half);
                for(int i=0;i<3;i++)line(g,Progression.contract(progress,i)+"  "+Math.min(Progression.progress(progress,i),Progression.target(progress,i))+"/"+Progression.target(progress,i)+(progress.claims.contains("daily:"+i)?" / claimed":Progression.progress(progress,i)>=Progression.target(progress,i)?" / READY TO CLAIM":""),right,y+i*11,DarkUi.MUTED,half);
                line(g,"Weekly battles: "+Math.min(progress.weeklyBattles,Progression.WEEKLY_BATTLES)+"/"+Progression.WEEKLY_BATTLES+(progress.claims.contains("weekly")?" / claimed":Progression.claimable(progress,"weekly")?" / READY TO CLAIM":""),right,y+33,DarkUi.MUTED,half);
            }
        }else if(page.equals("train")){
            var march=Spec.CAMPAIGNBATTLES[0];
            line(g,"Battle "+march.order()+" / "+march.name(),x,117,DarkUi.PAPER,w);
            g.drawTextWrapped(textRenderer,Text.literal(march.briefing()),x,138,w,DarkUi.MUTED);
            line(g,"START begins the battle. PRACTICE is a skirmish against one AI commander.",x,239,DarkUi.MUTED,w);
        }
        boolean card=page.equals("troop")&&CampaignClient.rollShowing(); // the roll card covers the screen: nothing under it takes hover or shows a tooltip
        if(!card)line(g,HintText.expand(CampaignClient.shownNotice(),GravesAndGloryClient::hint),x,height-42,DarkUi.GOLD,w); // the card says what the notice would (review Q1)
        super.render(g,card?-1:(int)(mx/scale),card?-1:(int)(my/scale),delta);
        if(card){RecruitPages.rollOn=page;RecruitPages.noticeShown=false;RecruitPages.rollCard(g,textRenderer,0,0,width,height);} // dims the whole screen, nav and header too: one focal point
        g.getMatrices().pop();
    }
    @Override public boolean mouseClicked(double x,double y,int button){if(page.equals("troop")&&troopSheet==null&&CampaignClient.rollShowing()){CampaignClient.revealUntil=0;return true;}if(page.equals("recruit")&&recruit!=null&&recruit.clickCloses())return true;if(pages!=null&&pages.page.equals(page)){if(button==GLFW.GLFW_MOUSE_BUTTON_RIGHT&&pages.secondaryClick(x,y))return true;pages.clicked(x,y);if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&pages.press(x,y))return true;}return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseScrolled(double x,double y,double amount){if(page.equals("recruit")&&recruit!=null)return recruit.scrolled(amount);return super.mouseScrolled(x/scale,y/scale,amount);}
    @Override public boolean mouseReleased(double x,double y,int button){boolean drop=pages!=null&&pages.page.equals(page)&&pages.released(x,y,button);return super.mouseReleased(x/scale,y/scale,button)||drop;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==GLFW.GLFW_MOUSE_BUTTON_LEFT&&pages!=null&&pages.page.equals(page)&&pages.dragged(x,y))return true;return super.mouseDragged(x/scale,y/scale,button,dx/scale,dy/scale);}
    /** A key's press: the room code being written takes its keys first (so Backspace repeats as it is held); then a held key's repeats stop here (see held); then the page shown, whose keys count as held until released. */
    @Override public boolean keyPressed(int key,int scan,int mods){boolean onPages=pages!=null&&pages.page.equals(page);if(onPages&&pages.typing(key))return true;if(held.contains(key))return true;if(page.equals("home")&&home!=null&&home.keyPressed(key)){held.add(key);return true;}if(page.equals("recruit")&&recruit!=null&&recruit.keyPressed(key)){held.add(key);return true;}if(page.equals("troop")&&troopSheet!=null&&troopSheet.keyPressed(key)){held.add(key);return true;}if(page.equals("lobby")&&lobby!=null&&lobby.keyPressed(key)){held.add(key);return true;}if(onPages&&pages.keyPressed(key)){held.add(key);return true;}if(key==77)return true;return super.keyPressed(key,scan,mods);}
    @Override public boolean charTyped(char ch,int mods){if(pages!=null&&pages.page.equals(page)&&pages.charTyped(ch))return true;return super.charTyped(ch,mods);}
    /** A held key's release lets its presses through again (see held). */
    @Override public boolean keyReleased(int key,int scan,int mods){if(held.remove(key))return true;return super.keyReleased(key,scan,mods);}
    @Override public void mouseMoved(double x,double y){if(page.equals("home")&&home!=null)home.mouseMoved(x,y);super.mouseMoved(x/scale,y/scale);}
    @Override public void setFocused(net.minecraft.client.gui.Element e){super.setFocused(e);if(page.equals("home")&&home!=null)home.focusChanged(e);}
}
