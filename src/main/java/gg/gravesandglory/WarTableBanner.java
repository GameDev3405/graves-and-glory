package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** War Table presentation of the v2 banner. Only existing server slot/colour commands mutate the roster. */
final class WarTableBanner {
    final CampScreen screen;
    final Map<String,UiButton> buttons=new LinkedHashMap<>();
    final Map<String,int[]> rects=new LinkedHashMap<>();
    final Map<String,String> texts=new LinkedHashMap<>();
    final List<String> sent=new ArrayList<>();
    String house=RosterMath.rollHouse(CampaignClient.profile.banner),armed="";
    int noticeSeen=CampaignClient.noticeSerial,noticeTicks;
    String notice="";
    WarTableBanner(CampScreen screen){this.screen=screen;armed=RecruitView.roll(house).stream().filter(r->r.owned()&&RecruitView.fieldable(house)).map(RecruitView.Row::troopId).findFirst().orElse("");}
    static boolean compact(){return UiCanvas.width()<960;}
    static int rollX(){return compact()?80:56;}
    static int sheetX(){return compact()?340:328;}
    static int sideX(){return compact()?655:664;}
    UiButton button(String key){return buttons.get(key);}
    UiButton add(String key,UiButton b){buttons.put(key,b);return screen.add(b);}
    void init(){
        buttons.clear();int ox=WarTable.origin()[0],oy=WarTable.origin()[1];
        add("back",WarTable.backPlaque(b->screen.page("home")));
        add("recruit",WarTable.plaque(800,104,"icon-recruit","Recruit",null,0,0,b->screen.page("recruit")));
        int i=0;for(var h:Spec.HOUSES){String id=h.id();int x=ox+204+112*i++;
            add("house-"+id,new UiButton(x,13,104,id.equals(house)?60:50,Text.literal(h.name()),b->showHouse(id),(g,bx,by,w,bh,hot)->paintHouse(g,id,bx,by,hot)));
        }
        for(i=0;i<6;i++){final int row=i;add("row-"+i,new UiButton(ox+rollX(),oy+140+56*i,240,56,Text.empty(),b->pick(row),(g,x,y,w,h,hot)->{if(hot)g.drawBorder(x+4,y+4,w-8,h-8,DarkUi.SEPIA);}));}
        for(i=0;i<Spec.RULES[0].squadSize();i++){final int slot=i;
            add("seat-"+i,new UiButton(ox+sheetX()+28+(i%5)*50,oy+188+(i/5)*52,48,48,Text.empty(),b->place(slot),(g,x,y,w,h,hot)->{if(hot)g.drawBorder(x,y,w,h,DarkUi.BRASS_HI);}));
        }
        tag("clear",ox+sheetX()+108,oy+452,"Clear",()->{armed=RosterMath.EMPTY;refresh();});
        plate("commander",ox+sideX(),oy+166,"Commander class",this::commander);
        plate("equipment",ox+sideX(),oy+214,"Army & equipment",()->CampScreen.open("army"));
        i=0;for(var side:Spec.FACTIONS){String id=side.id();add("colour-"+id,new UiButton(ox+sideX()+12+100*(i%2),oy+390+30*(i++/2),64,24,Text.literal(side.name()),b->send("colour:"+id),(g,x,y,w,h,hot)->{
            UiTex.draw(g,"table/btn-vellum-64x24",x,y);DarkUi.sideMark(g,side,x+9,y+8);word(g,"colour-label-"+id,side.name().split(" ")[0],x+21,y+8,40,DarkUi.INK_TEXT);
            if(id.equals(CampaignClient.profile.bannerColour)||hot)g.drawBorder(x,y,w,h,DarkUi.BRASS_HI);
        }));}
        refresh();
    }
    void tag(String key,int x,int y,String label,Runnable action){add(key,new UiButton(x,y,88,24,Text.literal(label),b->action.run(),(g,bx,by,w,h,hot)->{
        UiTex.draw(g,"table/btn-vellum-88x24",bx,by);WarTableHome.centred(g,DarkUi.heading(label),bx+44,by+6,hot?DarkUi.SEPIA:DarkUi.INK_TEXT);
    }));}
    void plate(String key,int x,int y,String label,Runnable action){add(key,new UiButton(x,y,240,40,Text.literal(label),b->action.run(),(g,bx,by,w,h,hot)->{
        UiTex.draw(g,"table/btn-brass-240x40",bx,by);WarTableHome.centred(g,DarkUi.heading(label),bx+120,by+14,DarkUi.PLATE_INK);
        if(hot)g.drawBorder(bx+3,by+3,w-6,h-6,DarkUi.BRASS_HI);
    }));}
    void showHouse(String id){if(id.equals(house))return;house=id;armed="";screen.rebuild();}
    void pick(int index){var rows=RecruitView.roll(house);if(index>=0&&index<rows.size()){var r=rows.get(index);if(r.owned()&&RecruitView.fieldable(house)&&WarTableRecruit.battleFree())armed=r.troopId();}refresh();}
    void refresh(){
        var rows=RecruitView.roll(house);boolean free=WarTableRecruit.battleFree();
        if(!armed.isEmpty()&&!armed.equals(RosterMath.EMPTY)&&(!CampaignClient.profile.owned.containsKey(armed)||!house.equals(RosterMath.Catalog.fromSpec().houseOf(armed))||!RosterMath.fieldableHouses().contains(RosterMath.Catalog.fromSpec().houseOf(armed))))armed="";
        for(int i=0;i<6;i++){var b=button("row-"+i);if(b==null)continue;b.visible=i<rows.size();b.active=b.visible&&rows.get(i).owned()&&RecruitView.fieldable(house)&&free;if(b.visible)b.setMessage(Text.literal(rows.get(i).name()+(!b.active?" / unavailable":" / pick troop")));}
        var line=CampaignClient.profile.banner;
        for(int i=0;i<line.size();i++){var b=button("seat-"+i);if(b==null)continue;String id=line.get(i);b.active=free&&!armed.isEmpty()&&(!RosterMath.MARKER.equals(id)||armed.equals(RosterMath.EMPTY));b.setMessage(Text.literal("Slot "+(i+1)+": "+(RosterMath.MARKER.equals(id)?"continuation of slot "+(ownerSlot(line,i)+1):RosterMath.EMPTY.equals(id)?"Empty":RecruitView.name(id))));}
        for(var e:buttons.entrySet())if(e.getKey().startsWith("colour-")||Set.of("clear","commander","equipment").contains(e.getKey()))e.getValue().active=free;
    }
    /** A continuation clears its weighted unit's origin; never issue a fake ^ troop command. */
    static int ownerSlot(List<String> line,int slot){while(slot>0&&RosterMath.MARKER.equals(line.get(slot)))slot--;return slot;}
    void place(int slot){refresh();var b=button("seat-"+slot);if(b==null||!b.active)return;var line=CampaignClient.profile.banner;int target=armed.equals(RosterMath.EMPTY)?ownerSlot(line,slot):slot;send("slot:"+target+":"+armed);}
    void send(String cmd){if(WarTableRecruit.battleFree()&&screen.action(cmd))sent.add(cmd);}
    void commander(){MinecraftClient.getInstance().setScreen(new CampaignLobbyScreen(){ {tab=2;} @Override public void close(){if(GravesAndGloryClient.running&&!CampaignClient.waiting){super.close();return;}var back=new CampScreen("banner");back.held.add(GLFW.GLFW_KEY_ESCAPE);MinecraftClient.getInstance().setScreen(back);} });}
    void tick(){if(noticeSeen!=CampaignClient.noticeSerial){noticeSeen=CampaignClient.noticeSerial;notice=CampaignClient.notice;noticeTicks=200;}if(noticeTicks>0)noticeTicks--;refresh();}
    boolean keyPressed(int key){if(key==GLFW.GLFW_KEY_ESCAPE){screen.page("home");return true;}if(key==GLFW.GLFW_KEY_R){screen.page("recruit");return true;}return false;}
    void word(DrawContext g,String key,String value,int x,int y,int room,int ink){var f=MinecraftClient.getInstance().textRenderer;String shown=UiMath.trim(value,room,s->f.getWidth(DarkUi.body(s)));g.drawText(f,DarkUi.body(shown),x,y,ink,false);texts.put(key,shown);rects.put(key,new int[]{x,y,f.getWidth(DarkUi.body(shown)),11});}
    void paintHouse(DrawContext g,String id,int x,int y,boolean hot){var h=Arrays.stream(Spec.HOUSES).filter(v->v.id().equals(id)).findFirst().orElseThrow();UiTex.draw(g,"recruit/banner-"+id+(id.equals(house)?"-on":""),x,y);String label=UiMath.trim(h.name(),96,s->MinecraftClient.getInstance().textRenderer.getWidth(DarkUi.heading(s)));WarTableHome.centred(g,DarkUi.heading(label),x+52,y+27,id.equals("gilded")?DarkUi.INK_TEXT:DarkUi.IVORY);if(hot&&!id.equals(house)){int width=MinecraftClient.getInstance().textRenderer.getWidth(DarkUi.heading(label));g.fill(x+52-width/2,y+40,x+52+(width+1)/2,y+41,id.equals("gilded")?DarkUi.INK_TEXT:DarkUi.IVORY);}}
    void portrait(DrawContext g,String id,int x,int y,boolean silhouette){UiTex.draw(g,"table/frame-back-48",x+4,y+4);var model=WarTableRecruit.model(id);if(model!=null)Portrait.draw(g,model,x+4,y+4,40,0,silhouette);UiTex.draw(g,"table/frame-rim-48",x,y);}
    void render(DrawContext g){UiCanvas.begin(g);try{
        rects.clear();texts.clear();int ox=WarTable.origin()[0],oy=WarTable.origin()[1],rx=ox+rollX(),sx=ox+sheetX(),right=ox+sideX();
        WarTable.drawChrome(g,"banner");UiTex.three(g,"recruit/banner-rod",ox+196,12,568);
        rects.put("roll",UiTex.draw(g,"recruit/muster-roll",rx,oy+124));
        var rows=RecruitView.roll(house);for(int i=0;i<rows.size();i++){var row=rows.get(i);int y=oy+140+56*i;
            if(row.troopId().equals(armed))UiTex.draw(g,"recruit/row-mark",rx,y);else if(i>0)UiTex.draw(g,"recruit/rule-208",rx+16,y);
            portrait(g,row.troopId(),rx+4,y+4,!row.owned());word(g,"row-"+i,row.name(),rx+60,y+12,172,row.owned()?DarkUi.INK_TEXT:DarkUi.SEPIA);
            String state=!row.owned()?"Not recruited":!RecruitView.fieldable(house)?"Not fieldable yet":row.rarity()+" / "+ArmyCards.spec(row.troopId()).slotWeight()+(ArmyCards.spec(row.troopId()).slotWeight()==1?" slot":" slots");
            word(g,"row-state-"+i,state,rx+60,y+30,172,DarkUi.SEPIA);
        }
        rects.put("sheet",UiTex.draw(g,"recruit/letter-hi",sx,oy+124));
        WarTableHome.centred(g,DarkUi.heading("Your Banner"),sx+152,oy+140,DarkUi.INK_TEXT);
        var line=CampaignClient.profile.banner;int used=(int)line.stream().filter(id->!id.equals(RosterMath.EMPTY)).count();
        String count=RosterMath.units(line,RosterMath.Catalog.fromSpec())+" units / "+used+" of "+line.size()+" slots";word(g,"count",count,sx+28,oy+157,260,DarkUi.SEPIA);
        word(g,"front","FRONT",sx+28,oy+174,260,DarkUi.SEPIA);
        for(int i=0;i<line.size();i++){int x=sx+28+(i%5)*50,y=oy+188+(i/5)*52;String id=line.get(i);boolean marker=RosterMath.MARKER.equals(id),empty=RosterMath.EMPTY.equals(id);
            if(!marker&&!empty)portrait(g,id,x,y,false);else{UiTex.draw(g,"table/frame-back-48",x+4,y+4);UiTex.draw(g,"table/frame-rim-48",x,y);g.getMatrices().push();g.getMatrices().translate(0,0,100);
                if(marker){g.fill(x+12,y+23,x+36,y+25,DarkUi.BRASS_HI);for(int d=0;d<5;d++){g.fill(x+12+d,y+23-d,x+14+d,y+25-d,DarkUi.BRASS_HI);g.fill(x+12+d,y+23+d,x+14+d,y+25+d,DarkUi.BRASS_HI);}word(g,"held-"+i,"Unit "+(ownerSlot(line,i)+1),x+6,y+32,36,DarkUi.IVORY);rects.put("marker-"+i,new int[]{x+12,y+19,24,10});}
                else{g.fill(x+19,y+23,x+29,y+25,DarkUi.MUTED);g.fill(x+23,y+19,x+25,y+29,DarkUi.MUTED);}
                g.getMatrices().pop();}
            word(g,"slot-number-"+i,""+(i+1),x+3,y+3,40,DarkUi.IVORY);texts.put("seat-"+i,id);rects.put("seat-"+i,new int[]{x,y,48,48});
        }
        word(g,"rear","REAR",sx+28,oy+398,260,DarkUi.SEPIA);
        word(g,"armed",armed.isEmpty()?"Pick a troop from this House":armed.equals(RosterMath.EMPTY)?"Clear tool: choose a slot":RecruitView.name(armed),sx+28,oy+416,260,DarkUi.INK_TEXT);
        word(g,"hint",armed.equals(RosterMath.EMPTY)?"Clear a held slot to remove its unit.":"Pick a troop, then a numbered slot.",sx+28,oy+434,260,DarkUi.SEPIA);
        word(g,"commander-heading","COMMANDER",right+8,oy+124,224,DarkUi.BRASS);
        String name=Arrays.stream(Spec.CLASSES).filter(c->c.id().equals(CampaignClient.profile.commander)).map(Spec.ClassesRow::name).findFirst().orElse(CampaignClient.profile.commander);
        word(g,"commander-name",name,right+8,oy+144,224,DarkUi.IVORY);
        String[] rules={"BANNER RULES","Epic: "+RosterMath.rarityRow("epic").slotWeight()+" slots / at most "+RosterMath.rarityRow("epic").bannerMax(),"Legendary: "+RosterMath.rarityRow("legendary").slotWeight()+" slots / at most "+RosterMath.rarityRow("legendary").bannerMax(),"Edits pack troops to the front.","Houses awaiting war cannot fight."};
        for(int i=0;i<rules.length;i++)word(g,"rule-"+i,rules[i],right+8,oy+274+18*i,224,i==0?DarkUi.BRASS:DarkUi.MUTED);
        word(g,"colour-heading","BANNER COLOUR",right+8,oy+372,224,DarkUi.BRASS);
        String colour=Arrays.stream(Spec.FACTIONS).filter(c->c.id().equals(CampaignClient.profile.bannerColour)).map(Spec.FactionsRow::name).findFirst().orElse("");word(g,"colour",colour,right+8,oy+458,224,DarkUi.IVORY);
        if(noticeTicks>0&&!notice.isBlank())word(g,"notice",HintText.expand(notice,GravesAndGloryClient::hint),12,80,UiCanvas.width()-24,DarkUi.IVORY);
    }finally{UiCanvas.end(g);}}
}
