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
    /** The tier I portrait: weapon tier 1 and armour tier 1. */
    static MobEntity portrait(Spec.TroopsRow spec){return portrait(spec,1,1);}
    /**
     * A portrait mob of a troop at a weapon tier and an armour tier (1 to 3), named as Battle.spawn names a fresh one (ArmyMobs.levelName at level 1),
     * so a troop with a model draws it through TroopRenderer: its tiers are set on the mob with RankSync.setLocal (level 1, so no chevrons), and a
     * tier the troop has no art for draws the highest one below it, as in battle. No team, so no match side and no pennant, in the company's cloth
     * (unit identity rule 8 as Kort ruled it), with its painted shield on a grey field (Battle.PORTRAIT_FIELD). Cached per id, tiers and colour: the
     * chest item is still dyed the commander's colour (the barracks and editor checks read it), though troop armour never draws.
     */
    static MobEntity portrait(Spec.TroopsRow spec,int weaponTier,int armourTier){
        var c=MinecraftClient.getInstance();if(world!=c.world){portraits.clear();world=c.world;}if(c.world==null)return null;
        int colour=troopColour();
        return portraits.computeIfAbsent(spec.id()+"/"+weaponTier+"/"+armourTier+"/"+colour,key->make(spec,weaponTier,armourTier,colour,key));
    }
    /** What made each portrait mob: its key (troop/weapon tier/armour tier/colour) and the arguments, so Portrait can make a frame its own copy. Weak keys by identity. */
    private static final Map<MobEntity,Object[]> MADE=new com.google.common.collect.MapMaker().weakKeys().makeMap();
    /** The cache key of a portrait mob this class made, or null for any other entity. */
    static String keyOf(MobEntity e){Object[] a=MADE.get(e);return a==null?null:(String)a[4];}
    /** A fresh portrait mob dressed as e (a frame's own copy, Portrait.twin), or null when e is no portrait mob. Not cached here. */
    static MobEntity copy(MobEntity e){
        Object[] a=MADE.get(e);var c=MinecraftClient.getInstance();if(a==null||c.world==null)return null;
        return make((Spec.TroopsRow)a[0],(int)a[1],(int)a[2],(int)a[3],(String)a[4]);
    }
    private static MobEntity make(Spec.TroopsRow spec,int weaponTier,int armourTier,int colour,String key){
        var c=MinecraftClient.getInstance();
        {
            // The sheet's entity type (ArmyMobs.portrait): a vanilla mob, so a card draws a House's beasts and crews before the House can take the field.
            MobEntity e=ArmyMobs.portrait(spec.entity(),c.world);
            e.setCustomName(Text.literal(ArmyMobs.levelName(spec,1)));e.setCustomNameVisible(false);e.setPersistent();e.setSilent(true);
            RankSync.setLocal(e,new RankSync.Rank(1,weaponTier,armourTier));
            e.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Registries.ITEM.get(Identifier.of(spec.weapon()))));e.equipStack(EquipmentSlot.HEAD,new ItemStack(Registries.ITEM.get(Identifier.of(spec.helmet()))));
            var chest=new ItemStack(Registries.ITEM.get(Identifier.of(spec.chest())));GravesAndGlory.Battle.dye(chest,colour);e.equipStack(EquipmentSlot.CHEST,chest);
            if(ArmyMobs.tags(spec).contains("shield"))e.equipStack(EquipmentSlot.OFFHAND,GravesAndGlory.Battle.shield(spec,GravesAndGlory.Battle.PORTRAIT_FIELD));
            TroopModels.dress(e,"company");MADE.put(e,new Object[]{spec,weaponTier,armourTier,colour,key});return e;
        }
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
        return (r.incoming()?UiText.t("cards.counterTakes","Takes x{multiplier}","multiplier",m):UiText.t("cards.counterDeals","Deals x{multiplier}","multiplier",m))+"  "+r.text();
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
    /** The troopNotes row of a troop (its beats and beaten-by cells; the level 2 and 3 card lines come from its levelAbilities rows), or null. */
    static Spec.TroopNotesRow notes(Spec.TroopsRow t){for(var n:Spec.TROOPNOTES)if(n.id().equals(t.id()))return n;return null;}

    /** Draws a portrait mob as the cards do (InventoryScreen.drawEntity), telling TroopModels the scale first: a troop of density above 1 draws its one-texel-a-px twins on a card under 16 x density GUI px a block (the size as given, not times the GUI scale; see TroopModels.FAR_CARD_PER_DENSITY). */
    static void draw(DrawContext g,int x,int bottom,int size,float mouseX,float mouseY,MobEntity e){
        TroopModels.sized(e,size);drawEntity(g,x,bottom,size,mouseX,mouseY,e);
    }
    /**
     * 1.20.1's InventoryScreen.drawEntity(context, x, y, size, mouseX, mouseY, entity), which 1.20.2 replaced with a scissored box
     * and other mouse maths: the feet at (x, y), mouseX and mouseY the cursor's offset from the point the mob looks from, the
     * same turns, and 1.21's low-level draw with no offset (its own translate, scale and dispatcher turn for the 1.21 camera).
     */
    static void drawEntity(DrawContext g,int x,int y,int size,float mouseX,float mouseY,net.minecraft.entity.LivingEntity e){
        float f=(float)Math.atan(mouseX/40f),h=(float)Math.atan(mouseY/40f);
        org.joml.Quaternionf q=new org.joml.Quaternionf().rotateZ((float)Math.PI),pitch=new org.joml.Quaternionf().rotateX(h*20f*0.017453292f);q.mul(pitch);
        float bodyYaw=e.bodyYaw,yaw=e.getYaw(),pitchWas=e.getPitch(),prevHead=e.prevHeadYaw,head=e.headYaw;
        e.bodyYaw=180f+f*20f;e.setYaw(180f+f*40f);e.setPitch(-h*20f);e.headYaw=e.getYaw();e.prevHeadYaw=e.getYaw();
        try{InventoryScreen.drawEntity(g,(float)x,(float)y,(float)size,new org.joml.Vector3f(),q,pitch,e);}
        finally{e.bodyYaw=bodyYaw;e.setYaw(yaw);e.setPitch(pitchWas);e.prevHeadYaw=prevHead;e.headYaw=head;}
    }
    /** A portrait size that fits any model in the same frame: a husk at size, a wolf no larger than 1.25 times, a golem smaller. */
    static int size(MobEntity e,int size){return (int)Math.round(size*Math.min(1.25,1.95/Math.max(.6,Math.max(e.getHeight(),e.getWidth()))));}
}
