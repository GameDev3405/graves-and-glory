package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.*;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.util.*;

/** Portraits are rendered from the same Minecraft models and loadouts as actual troops. */
final class ArmyCards {
    static final Map<String,MobEntity> portraits=new HashMap<>();static Object world;
    static Spec.TroopsRow spec(String id){return Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)).findFirst().orElse(null);}
    static Spec.HousesRow house(Spec.TroopsRow t){for(var h:Spec.HOUSES)if(h.id().equals(t.house()))return h;return null;}
    /** A troop's rarity as a word: Common, Rare, Epic, Legendary. */
    static String rarityName(Spec.TroopsRow t){return Morale.label(t.rarity());}
    /** Whether a troop's House can take the field in this build (houses.fieldable). */
    static boolean fieldable(Spec.TroopsRow t){var h=house(t);return h!=null&&h.fieldable();}
    /**
     * The colour this commander's troops wear. In battle it is the side his team flies (DarkUi.side, from the snapshot), which is not
     * his request when the other team or a teammate claimed that colour first; in the camp and the lobby it is the side row of his
     * bannerColour, the colour he asks for.
     */
    static int troopColour(){
        if(GravesAndGloryClient.battleActive()){var side=DarkUi.side(GravesAndGloryClient.team);if(side!=null)return side.color();}
        for(var f:Spec.FACTIONS)if(f.id().equals(CampaignClient.profile.bannerColour))return f.color();return Spec.FACTIONS[0].color();
    }
    /** The stage 1 portrait. */
    static MobEntity portrait(Spec.TroopsRow spec){return portrait(spec,1);}
    /**
     * A portrait mob of a troop at an evolution stage, named as Battle.spawn names it (ArmyMobs.stageName), so a troop with a
     * model draws it through TroopRenderer: no team, so no match side and no pennant, in the company's cloth (unit identity
     * rule 8 as Kort ruled it), with its painted shield on a grey field (Battle.PORTRAIT_FIELD). Cached per id, stage and colour: the chest item
     * is still dyed the commander's colour (the barracks and editor checks read it), though troop armour never draws.
     */
    static MobEntity portrait(Spec.TroopsRow spec,int stage){
        var c=MinecraftClient.getInstance();if(world!=c.world){portraits.clear();world=c.world;}if(c.world==null)return null;
        int colour=troopColour();
        return portraits.computeIfAbsent(spec.id()+"/"+stage+"/"+colour,key->{
            // The sheet's entity type (ArmyMobs.portrait): a vanilla mob, so a card draws a House's beasts and crews before the House can take the field.
            MobEntity e=ArmyMobs.portrait(spec.entity(),c.world);
            e.setCustomName(Text.literal(ArmyMobs.stageName(spec,stage)));e.setCustomNameVisible(false);e.setPersistent();e.setSilent(true);
            e.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Registries.ITEM.get(new Identifier(spec.weapon()))));e.equipStack(EquipmentSlot.HEAD,new ItemStack(Registries.ITEM.get(new Identifier(spec.helmet()))));
            var chest=new ItemStack(Registries.ITEM.get(new Identifier(spec.chest())));if(chest.getItem() instanceof DyeableItem dye)dye.setColor(chest,colour);e.equipStack(EquipmentSlot.CHEST,chest);
            if(ArmyMobs.tags(spec).contains("shield"))e.equipStack(EquipmentSlot.OFFHAND,GravesAndGlory.Battle.shield(spec,GravesAndGlory.Battle.PORTRAIT_FIELD));
            TroopModels.dress(e,"company");return e;
        });
    }
    // ---- the troop card's counter lines (Task 12) ----
    /** The counter rows a troop's card shows (CounterMath.cardRows on its counter tags, ArmyMobs.tags, and its attack kind), in sheet order. */
    static List<CounterMath.CardRow> counterRows(Spec.TroopsRow t){return CounterMath.cardRows(ArmyMobs.tags(t),CounterMath.attackKind(t),Spec.COUNTERS);}
    /** Whether a counter line favours the troop: a blow it takes softened, or a blow it deals sharpened. */
    static boolean favours(CounterMath.CardRow r){return r.incoming()?r.multiplier()<1:r.multiplier()>1;}
    /** Ember red: a counter line against the troop (its swords are broken, so the state is not colour alone). */
    static final int AGAINST=0xffd8826c;
    /** A 9x9 crossed-swords glyph, blades up: pommels, crossguards and grips at the bottom. */
    private static final String[] SWORDS={"x.......x",".x.....x.","..x...x..","...x.x...","....x....",".x.x.x.x.","..x...x..",".x.....x.","x.......x"};
    /** A line against the troop: the same swords with both blades broken off below their tips (rows 2 and 3 empty), a shape and not only a colour (review minor 2). */
    private static final String[] BROKEN={"x.......x",".x.....x.",".........",".........","....x....",".x.x.x.x.","..x...x..",".x.....x.","x.......x"};
    /** The crossed-swords glyph of a counter line at (x, y), drawn with fills; broken (down is true) for a line against the troop. */
    static void swords(DrawContext g,int x,int y,int colour,boolean down){
        var mask=glyph(!down);for(int j=0;j<9;j++){String row=mask[j];for(int i=0;i<9;i++)if(row.charAt(i)=='x')g.fill(x+i,y+j,x+i+1,y+j+1,colour);}
    }
    /** The 9x9 mask of a counter line's glyph: for a line in the troop's favour, or against it. */
    static String[] glyph(boolean good){return good?SWORDS:BROKEN;}
    /** One counter line as the card words it: "Takes x0.35 / Raised shields stop most arrows." or "Deals x1.60 / ...". */
    static String counterText(CounterMath.CardRow r){
        String m=String.format(Locale.ROOT,r.multiplier()==Math.rint(r.multiplier()*10)/10?"%.1f":"%.2f",r.multiplier());
        return (r.incoming()?"Takes x":"Deals x")+m+"  "+r.text();
    }
    /**
     * Draws a troop's counter lines from y, one a line: the crossed swords (brass and whole for a line in its favour; ember and
     * broken against it) and the line's words, trimmed to w. Returns the ids of the rows drawn, in order.
     */
    static List<String> counterLines(DrawContext g,TextRenderer f,Spec.TroopsRow t,int x,int y,int w){
        List<String> drawn=new ArrayList<>();
        for(var r:counterRows(t)){boolean good=favours(r);swords(g,x,y,good?DarkUi.GOLD:AGAINST,!good);
            g.drawText(f,f.trimToWidth(counterText(r),w-14),x+14,y+1,DarkUi.PAPER,false);y+=12;drawn.add(r.id());}
        return drawn;
    }
    /** The troopNotes row of a troop (its beats and beaten-by cells and its level 2 and 3 card lines), or null. */
    static Spec.TroopNotesRow notes(Spec.TroopsRow t){for(var n:Spec.TROOPNOTES)if(n.id().equals(t.id()))return n;return null;}

    /** A portrait size that fits any model in the same frame: a husk at size, a wolf no larger than 1.25 times, a golem smaller. */
    static int size(MobEntity e,int size){return (int)Math.round(size*Math.min(1.25,1.95/Math.max(.6,Math.max(e.getHeight(),e.getWidth()))));}
    static final class Recruit extends ButtonWidget {
        final Spec.TroopsRow spec;boolean selected;
        Recruit(Spec.TroopsRow spec,int x,int y,int w,PressAction action){super(x,y,w,78,Text.literal(spec.name()),action,DEFAULT_NARRATION_SUPPLIER);this.spec=spec;setTooltip(Tooltip.of(Text.literal(describe(spec))));}
        /** The card's tooltip: name, House and rarity, group, health and damage, and whether the House cannot be fielded yet. */
        static String describe(Spec.TroopsRow spec){var h=house(spec);return spec.name()+" / "+(h==null?spec.house():h.name())+" "+rarityName(spec)+" / "+Morale.label(spec.group())+" / "+spec.health()+" health / "+spec.damage()+" damage"+(fieldable(spec)?"":" / cannot be fielded yet");}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            var c=MinecraftClient.getInstance();selected=c.currentScreen instanceof CampaignLobbyScreen lobby&&lobby.selectedUnit.equals(spec.id());
            int x=getX(),y=getY();DarkUi.frame(g,x,y,width,height);
            if(selected||active&&(isHovered()||isFocused()))g.drawBorder(x+1,y+1,width-2,height-2,DarkUi.GOLD);
            var h=house(spec);int col=0x33000000|(h==null?0x4c9391:h.accent()&0xffffff);g.fillGradient(x+4,y+4,x+width-4,y+55,col,0x00181820); // the House accent
            var e=portrait(spec);if(e!=null){e.age=c.player==null?0:c.player.age;InventoryScreen.drawEntity(g,x+width/2,y+52,size(e,22),20,0,e);}
            g.getMatrices().push();g.getMatrices().translate(0,0,100); // as CampScreen.card: the model is drawn at z 50 and writes depth, so an inactive card's dim sits at z 100 and the card's text at z 150
            if(!active)g.fill(x+3,y+3,x+width-3,y+height-3,0x99111119);
            g.getMatrices().translate(0,0,50);
            g.drawCenteredTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth(spec.name(),width-8),x+width/2,y+57,active?DarkUi.PAPER:DarkUi.MUTED);
            boolean note=!fieldable(spec)||spec.slotWeight()>1;String state=!fieldable(spec)?"NOT FIELDABLE":spec.slotWeight()>1?spec.slotWeight()+" SLOTS":Morale.label(spec.group());
            g.drawCenteredTextWithShadow(c.textRenderer,state,x+width/2,y+68,note?DarkUi.GOLD:DarkUi.MUTED);
            g.drawTextWithShadow(c.textRenderer,""+Collections.frequency(CampaignClient.profile.banner,spec.id()),x+width-13,y+7,DarkUi.GOLD);
            g.getMatrices().pop();
        }
    }
    static final class Deployment extends ButtonWidget {
        final int index;String unit=RosterMath.EMPTY;
        /**
         * The word under a filled slot's weapon. An Epic or Legendary names its rarity (drawn in gold; its ^ slots follow): the whole
         * word when it fits the room, else its first six letters and a full stop, so LEGENDARY reads LEGEND. in the default font's 51 px
         * slot. Other troops show the first three letters of their group (INF, RAN, SUP).
         */
        static String slotLabel(Spec.TroopsRow spec,TextRenderer font,int room){
            if(spec.slotWeight()<=1){String group=spec.group().toUpperCase(Locale.ROOT);return group.substring(0,Math.min(3,group.length()));}
            String rarity=spec.rarity().toUpperCase(Locale.ROOT);
            return rarity.length()<=6||font.getWidth(rarity)<=room?rarity:rarity.substring(0,6)+".";
        }
        Deployment(int index,int x,int y,PressAction action){super(x,y,51,32,Text.literal("Empty banner slot "+(index+1)),action,DEFAULT_NARRATION_SUPPLIER);this.index=index;}
        void setUnit(String id){unit=id;var spec=spec(id);setMessage(Text.literal("Slot "+(index+1)+": "+(spec!=null?spec.name():RosterMath.MARKER.equals(id)?"held by the unit before it":"Empty")));setTooltip(Tooltip.of(getMessage()));}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();var c=MinecraftClient.getInstance();var spec=spec(unit);boolean held=RosterMath.MARKER.equals(unit);
            g.fillGradient(x,y,x+width,y+height,spec!=null?0xff36302b:held?0xff2e2924:0xff19171d,0xff18151b);g.drawBorder(x,y,width,height,isHovered()||isFocused()?DarkUi.GOLD:DarkUi.EDGE);
            g.drawText(c.textRenderer,String.format("%02d",index+1),x+3,y+3,0xff7f735f,false);
            if(spec!=null){g.drawItem(new ItemStack(Registries.ITEM.get(new Identifier(spec.weapon()))),x+width/2-8,y+4);boolean wide=spec.slotWeight()>1;String label=slotLabel(spec,c.textRenderer,width-4);
                g.drawCenteredTextWithShadow(c.textRenderer,label,x+width/2,y+23,wide?DarkUi.GOLD:DarkUi.PAPER);}
            else if(held){if(index%5!=0)g.fill(x-3,y+height/2-1,x,y+height/2+1,DarkUi.GOLD);g.drawCenteredTextWithShadow(c.textRenderer,"^",x+width/2,y+13,DarkUi.GOLD);} // a slot of the Epic or Legendary before it
            else g.drawCenteredTextWithShadow(c.textRenderer,"+",x+width/2,y+13,DarkUi.MUTED);
        }
    }
    /** A banner colour choice: the side's glyph and colour; the colour this commander asks for has a gold edge. */
    static final class Colour extends ButtonWidget {
        final Spec.FactionsRow side;
        Colour(Spec.FactionsRow side,int x,int y,PressAction action){super(x,y,34,20,Text.literal("Banner colour: "+side.name()),action,DEFAULT_NARRATION_SUPPLIER);this.side=side;setTooltip(Tooltip.of(getMessage()));}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();boolean mine=side.id().equals(CampaignClient.profile.bannerColour);
            g.fill(x,y,x+width,y+height,mine?0xff30474a:0xff252f34);g.drawBorder(x,y,width,height,mine||isHovered()||isFocused()?DarkUi.GOLD:0xff455156);
            DarkUi.sideMark(g,side,x+6,y+7);g.fill(x+17,y+7,x+28,y+14,0xff000000|side.color());
        }
    }
}
