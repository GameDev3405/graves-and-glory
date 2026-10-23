package gg.gravesandglory;

import com.google.gson.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import java.util.*;
import static gg.gravesandglory.GravesAndGloryClient.*;

/** Quiet camp navigation; social, recruitment and progression have separate destinations. */
final class CampScreen extends ArmyScreen {
    String page;float scale=1;int offset;String key="",roomInput="";long requestAt;TextFieldWidget code;
    /** The House whose six cards the recruit page shows and whose pool a roll draws from; its tab row changes it. It starts on the House the banner leans on. */
    String house=RosterMath.rollHouse(CampaignClient.profile.banner);
    final List<HouseTab> tabs=new ArrayList<>();
    /** Task 12 (RecruitPages): the troop the troop page shows, the House the contracts and chapter pages claim shards for, and tier 20's Rare. */
    String troop="",claimHouse=house,rare=RecruitPages.rares(house).isEmpty()?"":RecruitPages.rares(house).get(0);
    CampScreen(String page){super("Banner camp");this.page=page;}
    static void open(String page){var c=net.minecraft.client.MinecraftClient.getInstance();if(page.equals("army"))c.setScreen(new LobbyScreen(1));else c.setScreen(new CampScreen(page));}
    JsonObject social(){return CampaignClient.social;}
    boolean flag(String name){return social().has(name)&&social().get(name).getAsBoolean();}
    String value(String name){return social().has(name)?social().get(name).getAsString():"";}
    int number(String name,int fallback){return social().has(name)?social().get(name).getAsInt():fallback;}
    JsonArray rows(String name){return social().has(name)?social().getAsJsonArray(name):new JsonArray();}
    void action(String cmd){if(System.currentTimeMillis()-requestAt<300)return;requestAt=System.currentTimeMillis();send(cmd);}
    void page(String next){if(CampaignClient.rollShowing())CampaignClient.revealUntil=0;CampaignClient.notice="";page=next;offset=0;clearAndInit();} // leaving a page ends its roll card (review minor 4) and its notice (Task 12 re-review N2: "rises to star 2!" must not follow the player to another card)
    String state(){return CampaignClient.social.toString()+CampaignClient.progression.toString()+CampaignClient.profile.gold+CampaignClient.profile.owned.keySet()+CampaignClient.profile.pity+CampaignClient.profile.campaignCleared+CampaignClient.difficulty+running+ready+RecruitPages.stateKey(CampaignClient.profile);}
    @Override protected void init(){
        scale=Math.min(1,Math.min(client.getWindow().getScaledWidth()/760f,client.getWindow().getScaledHeight()/460f));width=(int)(client.getWindow().getScaledWidth()/scale);height=(int)(client.getWindow().getScaledHeight()/scale);key=state();code=null;
        String[] names={"Camp guide","Recruitment","Friends","Private room","Contracts","Chapter rewards"};String[] ids={"home","recruit","friends","room","contracts","chapter"};
        for(int i=0;i<ids.length;i++){String id=ids[i];button((page.equals(id)?"> ":"")+names[i],18,94+i*34,150,b->page(id));}
        button("Army & equipment",18,310,150,b->open("army"));button("Visuals & sound",18,340,150,b->client.setScreen(new PresentationScreen()));button("Return to camp",18,height-42,150,b->close());
        int x=194,w=width-x-22;
        if(CampaignClient.result.has("id"))button("Last battle",18,370,150,b->page("results"));
        if(page.equals("home")){
            // The Long March: battle 1 is the primary button until it is won, which points new commanders to it; then a normal button for replays.
            var march=Spec.CAMPAIGNBATTLES[0];String go="campaign:"+march.id();
            var marchButton=CampaignClient.profile.campaignCleared<march.order()?primary("THE LONG MARCH / BATTLE "+march.order(),x,147,w,b->action(go)):button("The Long March / battle "+march.order(),x,147,w,b->action(go));marchButton.active=!running;
            primary("OPEN RECRUITMENT",x,181,w/2-7,b->page("recruit"));primary("PARTY WITH FRIENDS",x+w/2+7,181,w/2-7,b->page("friends"));
            button("Practice / skirmish",x,262,w/2-7,b->action("camp:practice"));button("Army / deployment",x+w/2+7,262,w/2-7,b->open("army"));
            var bots=button(CampaignClient.botsLabel(),x,286,w/2-7,b->action("difficulty:"+CampaignClient.nextDifficulty()));bots.active=!running;
            button("Ready gates & map voting",x,350,w/2-7,b->page("match"));button("Daily contracts",x+w/2+7,350,w/2-7,b->page("contracts"));
        }else if(page.equals("recruit")){
            // A tab per House, then its six cards; the roll draws from the chosen House (spec 4.2, 4.6) and a duplicate raises stars.
            tabs.clear();int tabWidth=w/Spec.HOUSES.length;
            for(int i=0;i<Spec.HOUSES.length;i++)tabs.add(addDrawableChild(new HouseTab(Spec.HOUSES[i].id(),x+i*tabWidth,112,tabWidth-6)));
            int cost=Spec.RECRUITMENT[0].rollCost();
            var roll=primary(rollLabel(),x,height-78,w,b->action("roll:"+house));roll.active=CampaignClient.profile.gold>=cost&&(!running||CampaignClient.waiting);
            button("Skip reveal",width-126,49,104,b->CampaignClient.revealUntil=0);
            for(var widget:RecruitPages.recruitWidgets(house,CampaignClient.profile,x,w,this::action,this::openTroop))addDrawableChild(widget);
        }else if(page.equals("troop")){
            var t=ArmyCards.spec(troop);var a=t==null?null:RecruitPages.action(t,CampaignClient.profile);
            if(a!=null){var go=primary(a.label().toUpperCase(java.util.Locale.ROOT),x,height-114,RecruitPages.LEFT,b->action(a.command()));go.active=a.active()&&(!running||CampaignClient.waiting);go.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(a.tip())));}
            button("Back to recruitment",x,height-82,RecruitPages.LEFT,b->page("recruit"));
        }else if(page.equals("friends")){
            if(!flag("party"))primary("CREATE PARTY",x,117,180,b->action("social:create"));else{
                button("Leave party",x,117,120,b->action("social:leave"));button(flag("partyLead")?"Disband party":"Claim offline lead",x+130,117,160,b->action(flag("partyLead")?"social:disband":"social:claim"));
                primary("CREATE PRIVATE ROOM",x+w-215,113,215,b->action("social:room"));
            }
            if(!value("inviter").isEmpty()){button("Accept invitation",x,166,155,b->action("social:accept"));button("Decline",x+166,166,100,b->action("social:decline"));}
            int i=0;for(var e:rows("members")){var row=e.getAsJsonObject();String name=row.get("name").getAsString();if(flag("partyLead")&&!row.get("me").getAsBoolean()){button("Make leader",x+w-198,217+i*27,105,b->action("social:transfer:"+name));button("Remove",x+w-83,217+i*27,83,b->action("social:kick:"+name));}i++;}
            i=0;var available=rows("available");for(int n=Math.min(offset,available.size());n<available.size()&&i<4;n++){var e=available.get(n);String name=e.getAsString();var invite=button("Invite "+name,x+(i%2)*(w/2),345+(i/2)*26,w/2-8,b->action("social:invite:"+name));invite.active=flag("partyLead");i++;}
            if(available.size()>4)button("More friends",x+w-120,396,120,b->{offset=offset+4<available.size()?offset+4:0;clearAndInit();});
        }else if(page.equals("room")){
            if(value("room").isEmpty()){
                primary("CREATE PRIVATE ROOM",x,144,w,b->action("social:room"));
                code=new TextFieldWidget(textRenderer,x,222,w-146,24,Text.literal("Room code"));code.setMaxLength(8);code.setText(roomInput);code.setChangedListener(v->roomInput=v);addDrawableChild(code);
                button("Join with party",x+w-136,224,136,b->action("social:join:"+roomInput.trim()));
            }else{
                button("Copy room code",x,115,145,b->client.keyboard.setClipboard(value("room")));button("Leave room",x+155,115,110,b->action("social:exit"));
                for(int t=0;t<2;t++){final int team=t;button(t==0?"Move party to Dawn":"Move party to Dusk",x+t*(w/2),155,w/2-8,b->action("social:team:"+team));}
                for(int i=1;i<=4;i++){final int size=i;var button=button(number("size",2)==i?"> "+i+"v"+i:i+"v"+i,x+(i-1)*60,310,54,b->action("social:size:"+size));button.active=flag("host")&&!flag("playing");}
                var fill=button("Bots: "+(flag("fill")?"ON":"OFF"),x+250,310,100,b->action("social:fill"));fill.active=flag("host")&&!flag("playing");
                var map=button(Spec.MAPS[Math.min(number("map",0),Spec.MAPS.length-1)].name()+" / next",x,341,w,b->action("social:map:"+((number("map",0)+1)%Spec.MAPS.length)));map.active=flag("host")&&!flag("playing");
                button("Toggle ready",x,height-78,w/2-8,b->action("social:ready"));var start=primary(flag("playing")?"MATCH IN PROGRESS":flag("preparing")?"PREPARING MAP":"START TOGETHER",x+w/2,height-82,w/2,b->action("social:start"));start.active=flag("host")&&!flag("playing")&&ready&&!running;
            }
        }else if(page.equals("contracts")){
            for(int i=0;i<3;i++){final int slot=i;button("Claim",x+w-90,124+i*65,90,b->action(RecruitPages.contractClaim("daily:"+slot,claimHouse)));}
            button("Free third-contract reroll",x,317,230,b->action("progress:reroll"));button("Claim weekly",x+w-130,354,130,b->action(RecruitPages.contractClaim("weekly",claimHouse)));
            for(var chip:RecruitPages.houseChips(x+w,86,()->claimHouse,this::pickClaimHouse))addDrawableChild(chip);
        }else if(page.equals("chapter")){
            for(int i=0;i<10;i++){int tier=offset+i+1;if(tier>30)break;boolean pick=tier==Progression.RARE_TIER;int bw=w/2-8-(pick?112:0);
                button("Claim tier "+tier,x+(i%2)*(w/2),121+(i/2)*45,bw,b->action(RecruitPages.foundersClaim(tier,claimHouse,rare)));
                if(pick){var t=ArmyCards.spec(rare);button(t==null?"Choose a Rare":"Rare: "+t.name(),x+(i%2)*(w/2)+bw+4,121+(i/2)*45,108,b->cycleRare()).setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal("Tier "+tier+"'s Rare, from the chosen House")));}}
            for(var chip:RecruitPages.houseChips(x+w,86,()->claimHouse,this::pickClaimHouse))addDrawableChild(chip);
            button("Previous 10",x,359,120,b->{offset=Math.max(0,offset-10);clearAndInit();});button("Next 10",x+130,359,120,b->{offset=Math.min(20,offset+10);clearAndInit();});
            button("Cycle earned title",x+w-180,359,180,b->{var titles=CampaignClient.profile.progression.titles.toArray(String[]::new);int index=Arrays.asList(titles).indexOf(CampaignClient.profile.progression.title);if(titles.length>0)action("progress:title:"+titles[(index+1)%titles.length]);});
        }else if(page.equals("results")){
            button("Claim contracts",x,height-84,w/3-8,b->page("contracts"));button("Chapter rewards",x+w/3,height-84,w/3-8,b->page("chapter"));
            primary(value("room").isEmpty()?"BACK TO CAMP":"RETURN TO ROOM",x+2*w/3,height-88,w/3,b->page(value("room").isEmpty()?"home":"room"));
        }else if(page.equals("match")){
            for(int i=0;i<Spec.MAPS.length;i++){final int map=i;button("Vote: "+Spec.MAPS[i].name(),x,149+i*35,w,b->action("vote:"+map));}
            button("Play with friends in a room",x,350,w,b->page("room"));
        }
    }
    @Override public void tick(){if(!key.equals(state())&&!(code!=null&&code.isFocused()))clearAndInit();if(running&&!CampaignClient.waiting)close();}
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
    /** One card of the recruit page: a troop of the House, whether the commander owns it, and its stars, evolution stage and copies. */
    record Card(Spec.TroopsRow troop,boolean owned,int stars,int stage,int copies){}
    /** The six troops of a House in sheet order, each with the commander's holding of it. */
    static List<Card> cards(String house,Profile p){
        List<Card> out=new ArrayList<>();
        for(var t:Spec.TROOPS)if(t.house().equals(house)){var o=p.owned.get(t.id());out.add(new Card(t,o!=null,o==null?0:o.stars,o==null?0:o.stage,o==null?0:o.copies));}
        return out;
    }
    static int rarityColour(String rarity){return switch(rarity){case "rare"->0xff8fb8d8;case "epic"->0xffc79be0;case "legendary"->DarkUi.GOLD;default->DarkUi.MUTED;};}
    /** The lowest pixel (from the card's top) the last card() call drew a word or a pip at, and whether the harness is probing (no portrait is drawn). Task 12 re-review N1: p2-card-layout reads what card() drew, not the row table. */
    int cardLow;static volatile boolean probe;
    /** The troops the last frames' cards drew and whether each was owned (the harness clears it, presses a House tab and reads it: Task 13, p2-full-roster). */
    static final Map<String,Boolean> seenCards=new java.util.concurrent.ConcurrentHashMap<>();
    /** A word of a card at dy under the card's top: drawn, and noted for cardLow. */
    void cardLine(DrawContext g,String text,int x,int y,int dy,int color,int room){line(g,text,x,y+dy,color,room);cardLow=Math.max(cardLow,dy+9);}
    /** One troop card: the portrait on the House accent, the stage name, the rarity, the stars as pips and the stage; a troop the commander lacks is dimmed and says LOCKED with its odds. */
    void card(DrawContext g,Card card,int x,int y,int w,int h,int poolTotal){
        cardLow=0;seenCards.put(card.troop().id(),card.owned());
        var troop=card.troop();DarkUi.frame(g,x,y,w,h);
        var row=ArmyCards.house(troop);g.fillGradient(x+3,y+3,x+w-3,y+h-3,0x33000000|(row==null?0x4c9391:row.accent()&0xffffff),0x00181820);
        var portrait=ArmyCards.portrait(troop);float wide=portrait==null?0:portrait.getWidth()*1.25f; // a wide model (a spider's legs) is drawn smaller so it stays inside its card
        if(!probe)DarkUi.model(g,troop,x+34,y+h-12,wide>1.5f?(int)(28*1.5f/wide):28);
        g.getMatrices().push();g.getMatrices().translate(0,0,100); // the model is drawn at z 50 and writes depth, so what must show over it is nearer: the dim of a card the commander lacks at z 100, the card's text at z 150
        if(!card.owned())g.fill(x+3,y+3,x+w-3,y+h-3,0x99111119);
        g.getMatrices().translate(0,0,50);
        int tx=x+70,room=w-76;
        // A long name (a Legendary's title) wraps onto a second line, and everything under it moves down a line.
        var title=textRenderer.wrapLines(Text.literal(ArmyMobs.stageName(troop,card.owned()?card.stage():1)),room);int more=title.size()>1?10:0;
        for(int i=0;i<Math.min(2,title.size());i++){g.drawText(textRenderer,title.get(i),tx,y+10+i*10,card.owned()?DarkUi.GOLD:DarkUi.MUTED,false);cardLow=Math.max(cardLow,10+i*10+9);}
        var rows=RecruitPages.cardRows(more>0);cardLine(g,cap(troop.rarity()),tx,y,rows.rarity(),rarityColour(troop.rarity()),room);
        if(card.owned()){
            for(int i=0;i<Spec.RECRUITMENT[0].maxStars();i++){int px=tx+i*11;g.drawBorder(px,y+rows.pips(),8,8,DarkUi.GOLD);if(i<card.stars())g.fill(px+1,y+rows.pips()+1,px+7,y+rows.pips()+7,DarkUi.GOLD);cardLow=Math.max(cardLow,rows.pips()+8);}
            cardLine(g,card.copies()+(card.copies()==1?" copy":" copies"),tx,y,rows.copies(),DarkUi.MUTED,room);
        }else{
            cardLine(g,"LOCKED",tx,y,rows.locked(),DarkUi.PAPER,room);
            cardLine(g,troop.rarity().equals("legendary")||weight(troop)<=0?"Comes from a Pact":String.format(java.util.Locale.ROOT,"%.1f",100.0*weight(troop)/Math.max(1,poolTotal))+"% a roll",tx,y,rows.odds(),DarkUi.MUTED,room);
        }
        g.getMatrices().pop();
    }
    /** One House's tab on the recruit page: its glyph and name, the chosen one edged in brass. */
    final class HouseTab extends ButtonWidget {
        final String house;
        HouseTab(String house,int x,int y,int w){super(x,y,w,28,Text.literal(RosterMath.houseName(house)),b->CampScreen.this.pickHouse(house),DEFAULT_NARRATION_SUPPLIER);this.house=house;}
        boolean selected(){return CampScreen.this.house.equals(house);}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();boolean chosen=selected(),focus=active&&(isHovered()||isFocused());
            g.fill(x,y,x+width,y+height,chosen?0xff30474a:focus?0xff39464b:0xff252f34);g.drawBorder(x,y,width,height,chosen||focus?DarkUi.GOLD:0xff455156);
            for(var row:Spec.HOUSES)if(row.id().equals(house))g.drawTexture(new Identifier(row.glyph()),x+6,y+6,0f,0f,16,16,16,16);
            var font=textRenderer;g.drawText(font,font.trimToWidth(getMessage().getString(),width-34),x+28,y+10,chosen?DarkUi.PAPER:DarkUi.MUTED,false);
            if(chosen)g.fill(x,y+height-2,x+width,y+height,DarkUi.GOLD);
        }
    }
    void line(DrawContext g,String text,int x,int y,int color,int max){g.drawText(textRenderer, textRenderer.trimToWidth(text,max),x,y,color,false);}
    /** The battle report table from y down: COMMANDER / KILLS / LOST / GOLD / ON POINTS, one row per army (your team first, a rule before the
     *  enemy, your row in brass, a crest on the MVP), then one line per point with the seconds each team held it. Returns the y under it. */
    int table(DrawContext g,JsonObject report,int x,int y,int w){
        if(!report.has("squads")||!report.has("points"))return y;
        JsonArray rows=report.getAsJsonArray("squads");String[] heads={"KILLS","LOST","GOLD","ON POINTS"},keys={"kills","lost","gold","objective"};int[] right={x+w-200,x+w-140,x+w-70,x+w};int name=right[0]-x-74;
        line(g,"COMMANDER",x+14,y,DarkUi.MUTED,name);for(int c=0;c<4;c++)g.drawText(textRenderer,heads[c],right[c]-textRenderer.getWidth(heads[c]),y,DarkUi.MUTED,false);
        DarkUi.rule(g,x,y+11,w);y+=14;
        List<MatchStats.SquadStats> tally=new ArrayList<>();for(var e:rows){var s=new MatchStats.SquadStats();s.kills=e.getAsJsonObject().get("kills").getAsInt();s.objectiveSeconds=e.getAsJsonObject().get("objective").getAsInt();tally.add(s);}
        int mvp=MatchStats.mvp(tally);
        for(int i=0;i<rows.size();i++,y+=11){var r=rows.get(i).getAsJsonObject();int color=r.get("you").getAsBoolean()?DarkUi.GOLD:DarkUi.PAPER;
            if(i>0&&r.get("team").getAsInt()!=rows.get(i-1).getAsJsonObject().get("team").getAsInt()){DarkUi.rule(g,x+14,y,w-14);y+=3;}
            if(i==mvp)DarkUi.crest(g,x,y-1,10,r.get("team").getAsInt()==1);
            line(g,r.get("name").getAsString()+(i==mvp?"  MVP":""),x+14,y,color,name);
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
        line(g,name,x,y,0xff000000|row.accent(),w);int at=x+textRenderer.getWidth(name)+8;
        line(g,(tier>0?"Trait "+DarkUi.roman(tier)+" held "+h.get("traitSeconds").getAsInt()+"s":"No trait tier")+"   +"+h.get("shards").getAsInt()+" shards   +"+h.get("renown").getAsInt()+" Renown",at,y,DarkUi.PAPER,x+w-at);
    }
    @Override public void render(DrawContext g,int mx,int my,float delta){
        g.getMatrices().push();g.getMatrices().scale(scale,scale,1);DarkUi.background(g,width,height);DarkUi.crest(g,20,16,26,false);DarkUi.title(g,"BANNER CAMP",58,19,DarkUi.GOLD);
        line(g,header(),width-280,24,DarkUi.PAPER,260);
        int x=194,w=width-x-22;DarkUi.frame(g,x-12,79,w+24,height-139);String title=switch(page){case "recruit"->"RECRUITMENT SHRINE";case "friends"->"YOUR FRIEND GROUP";case "room"->"PRIVATE WAR ROOM";case "contracts"->"CONTRACT BOARD";case "chapter"->"FOUNDERS CHAPTER";case "troop"->"TROOP CARD";case "match"->"WAR GATES";case "results"->"BATTLE REPORT";default->"A PLACE TO PREPARE";};
        line(g,title,x,91,DarkUi.GOLD,w);
        var p=CampaignClient.profile;var progress=p.progression;
        if(page.equals("home")){
            line(g,"Recruit an army. Gather your friends. Fight under one banner.",x,113,DarkUi.PAPER,w);
            line(g,"Quartermaster west / Recruiter east",x,131,DarkUi.MUTED,w);
            var roster=RosterMath.launchRoster();line(g,Progression.chapter(progress,(int)roster.stream().filter(p.owned::containsKey).count(),roster.size()),x,218,DarkUi.GOLD,w);
            line(g,"Practice: a skirmish against one AI commander at the chosen difficulty.",x,239,DarkUi.MUTED,w);
            line(g,"Easy, Normal or Hard AI commanders.",x+w/2+7,292,DarkUi.MUTED,w/2-7);
            line(g,"War gates: stand together to ready; step off to cancel.",x,314,DarkUi.PAPER,w);
            line(g,"Local matches only. Internet matchmaking is on hold.",x,333,DarkUi.MUTED,w);
        }else if(page.equals("recruit")){
            var pool=pool(house);int total=0;for(var t:pool)total+=weight(t);
            line(g,"Draws from the "+RosterMath.houseName(house)+" pool: "+odds(pool,"common",total)+" Commons / "+odds(pool,"rare",total)+" Rares / "+odds(pool,"epic",total)+" Epic / a duplicate raises stars",x,102,DarkUi.MUTED,w);
            var cards=cards(house,p);int cardWidth=w/3-10,cardHeight=96;
            for(int i=0;i<cards.size();i++)card(g,cards.get(i),x+(i%3)*(w/3),152+(i/3)*(cardHeight+8),cardWidth,cardHeight,total);
            line(g,pityLine()+(ArmyCards.house(pool.get(0))!=null&&!ArmyCards.house(pool.get(0)).fieldable()?"  /  "+RosterMath.houseName(house)+" cannot take the field yet":""),x,height-96,DarkUi.GOLD,w);
        }else if(page.equals("troop")){
            var t=ArmyCards.spec(troop);if(t!=null)RecruitPages.troopPage(g,textRenderer,t,p,x,108,w);
        }else if(page.equals("friends")){
            line(g,flag("party")?"Leader: "+value("leader")+" / party survives matches and server restarts":"Parties hold up to four friends and always enter one team.",x,104,DarkUi.MUTED,w);
            if(!value("inviter").isEmpty())line(g,"Invitation from "+value("inviter")+" ("+number("inviteSeconds",0)+"s)",x,149,DarkUi.GOLD,w);
            int i=0;for(var e:rows("members")){var r=e.getAsJsonObject();line(g,r.get("name").getAsString()+" / "+(r.get("online").getAsBoolean()?"online":"offline"),x,223+i++*27,DarkUi.PAPER,w-215);}
            line(g,"INVITE SOMEONE IN THIS CAMP",x,325,DarkUi.GOLD,w);
            if(rows("available").isEmpty())line(g,"Friends appear here after joining this game server.",x,353,DarkUi.MUTED,w);
        }else if(page.equals("room")){
            if(value("room").isEmpty()){
                line(g,"Create a room, or enter the private code a friend shared.",x,119,DarkUi.PAPER,w);line(g,"ROOM CODE",x,202,DarkUi.GOLD,w);
                line(g,"Your whole party joins one team. No silent splitting.",x,281,DarkUi.MUTED,w);line(g,"Each commander readies. The host chooses map, mode and bots.",x,306,DarkUi.MUTED,w);
            }else{line(g,"CODE "+value("room")+"  /  "+(flag("host")?"You host":"Member"),x+w-225,119,DarkUi.GOLD,225);
                int[] count={0,0};for(var e:rows("commanders")){var r=e.getAsJsonObject();int team=r.get("team").getAsInt();line(g,r.get("name").getAsString()+" / "+(!r.get("online").getAsBoolean()?"reconnecting":r.get("ready").getAsBoolean()?"READY":"not ready"),x+team*(w/2),193+count[team]++*24,DarkUi.PAPER,w/2-10);}}
        }else if(page.equals("contracts")){
            RecruitPages.caption(g,textRenderer,claimHouse,x+w,86);
            for(int i=0;i<3;i++){int y=128+i*65;line(g,Progression.contract(progress,i),x,y,DarkUi.PAPER,w-110);line(g,Math.min(Progression.progress(progress,i),Progression.target(progress,i))+" / "+Progression.target(progress,i)+"  |  "+(i==1?40:30)+" gold + 75 XP"+RecruitPages.contractShards(progress,i)+(progress.claims.contains("daily:"+i)?" / CLAIMED":""),x,y+21,DarkUi.MUTED,w-110);}
            line(g,"Weekly: "+progress.weeklyBattles+" / 6 battles / 150 gold + 300 XP"+RecruitPages.contractShards(progress,-1),x,358,DarkUi.PAPER,w-140);line(g,"Daily reset 00:00 UTC / weekly reset Monday 00:00 UTC",x,386,DarkUi.MUTED,w);
        }else if(page.equals("chapter")){
            RecruitPages.caption(g,textRenderer,claimHouse,x+w,86);
            line(g,"Tier "+Progression.tier(progress)+" / 30  |  "+Math.min(progress.xp,7500)+" / 7500 XP  |  Free; no expiry",x,108,DarkUi.PAPER,w);
            for(int i=0;i<10;i++){int tier=offset+i+1;String reward=Progression.reward(tier);line(g,reward+(progress.claims.contains("founders:"+tier)?" / claimed":Progression.tier(progress)<tier?" / locked":" / earned"),x+(i%2)*(w/2)+4,146+(i/2)*45,DarkUi.MUTED,w/2-16);}
            line(g,"Finish the chapter; keep every unit and title when new chapters arrive.",x,397,DarkUi.MUTED,w);
        }else if(page.equals("results")){
            var report=CampaignClient.result;
            if(report.has("id")){
                line(g,report.get("outcome").getAsString()+" / "+report.get("map").getAsString(),x,117,DarkUi.GOLD,w);
                var houses=report.has("houses")?report.getAsJsonArray("houses"):new JsonArray();int extra=houses.isEmpty()?0:houses.size()*11+3; // one line per House of the banner, under the gold
                int y=Math.max(table(g,report,x,132,w)+5,height-163-extra); // the battle table first; the rewards sit above the buttons, or right under a long table
                boolean saved=report.get("saved").getAsBoolean();
                line(g,saved?"+"+report.get("gold").getAsInt()+" GOLD     +"+report.get("xp").getAsInt()+" CHAPTER XP":"REWARDS NOT SAVED / Progress unchanged",x,y,saved?DarkUi.PAPER:0xffff7777,w);
                line(g,!saved?"Check disk space before playing again.":report.get("eligible").getAsBoolean()?"Rewards saved. Contract and tier rewards are claimed separately.":"No match rewards: play at least 3 minutes to qualify.",x,y+11,DarkUi.MUTED,w);
                for(int i=0;i<houses.size();i++)houseRow(g,houses.get(i).getAsJsonObject(),x,y+24+i*11,w);
                y+=25+extra;int tier=Progression.tier(progress),xp=Math.min(progress.xp,7500),half=w/2-8,right=x+w-half; // chapter progress on the left, contracts on the right
                line(g,"FOUNDERS / Tier "+tier+" of 30 / "+xp+" XP",x,y,DarkUi.GOLD,half);
                g.fill(x,y+11,x+half,y+15,0xff29383c);g.fill(x,y+11,x+(int)(half*(tier==30?1:(xp%250)/250f)),y+15,DarkUi.GOLD);
                line(g,tier==30?"Chapter complete. Troops and titles stay yours.":"Next: tier "+(tier+1)+" / "+Progression.reward(tier+1)+" / "+(250-xp%250)+" XP to go",x,y+19,DarkUi.PAPER,half);
                for(int i=0;i<3;i++)line(g,Progression.contract(progress,i)+"  "+Math.min(Progression.progress(progress,i),Progression.target(progress,i))+"/"+Progression.target(progress,i)+(progress.claims.contains("daily:"+i)?" / claimed":Progression.progress(progress,i)>=Progression.target(progress,i)?" / READY TO CLAIM":""),right,y+i*11,DarkUi.MUTED,half);
                line(g,"Weekly battles: "+Math.min(progress.weeklyBattles,6)+"/6"+(progress.claims.contains("weekly")?" / claimed":progress.weeklyBattles>=6?" / READY TO CLAIM":""),right,y+33,DarkUi.MUTED,half);
            }
        }else if(page.equals("match")){
            line(g,"Dawn "+CampaignClient.padDawn+" / 4   |   Dusk "+CampaignClient.padDusk+" / 4   |   "+CampaignClient.padCountdown+"s",x,116,DarkUi.GOLD,w);
            line(g,"Vote while on a ready pad. Most votes wins; ties use map order.",x,133,DarkUi.MUTED,w);
        }
        boolean reveals=page.equals("recruit")||page.equals("troop"),card=reveals&&CampaignClient.rollShowing(); // the roll card covers the screen: nothing under it takes hover or shows a tooltip
        if(!card)line(g,CampaignClient.notice,x,height-42,DarkUi.GOLD,w); // the card says what the notice would (review Q1)
        super.render(g,card?-1:(int)(mx/scale),card?-1:(int)(my/scale),delta);
        if(card){RecruitPages.rollOn=page;RecruitPages.noticeShown=false;RecruitPages.rollCard(g,textRenderer,0,0,width,height);} // dims the whole screen, nav and header too: one focal point
        else if(reveals&&CampaignClient.revealUntil>System.currentTimeMillis()){g.getMatrices().push();g.getMatrices().translate(0,0,300);DarkUi.frame(g,x+30,180,w-60,100);line(g,"RECRUITMENT CONFIRMED",x+50,201,DarkUi.GOLD,w-100);line(g,CampaignClient.revealUnit+" has joined your collection.",x+50,226,DarkUi.PAPER,w-100);line(g,"Place it in your banner in the war council / Skip reveal above to continue",x+50,251,DarkUi.MUTED,w-100);g.getMatrices().pop();}
        g.getMatrices().pop();
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if((page.equals("recruit")||page.equals("troop"))&&CampaignClient.rollShowing()){CampaignClient.revealUntil=0;return true;} // any click ends the roll card, as Skip reveal does
        return super.mouseClicked(x/scale,y/scale,button);}
    @Override public boolean mouseReleased(double x,double y,int button){return super.mouseReleased(x/scale,y/scale,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return super.mouseDragged(x/scale,y/scale,button,dx/scale,dy/scale);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(code!=null&&code.isFocused()&&key!=256)return code.keyPressed(key,scan,mods);if(key==77)return true;return super.keyPressed(key,scan,mods);}
}

