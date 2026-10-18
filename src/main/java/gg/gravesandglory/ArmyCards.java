package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
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
    /** Whether a troop's House can take the field in this build (houses.fieldable). */
    static boolean fieldable(Spec.TroopsRow t){var h=house(t);return h!=null&&h.fieldable();}
    /** The colour this commander's troops wear: the side row of his bannerColour. */
    static int bannerColour(){for(var f:Spec.FACTIONS)if(f.id().equals(CampaignClient.profile.bannerColour))return f.color();return Spec.FACTIONS[0].color();}
    static MobEntity portrait(Spec.TroopsRow spec){
        var c=MinecraftClient.getInstance();if(world!=c.world){portraits.clear();world=c.world;}if(c.world==null)return null;
        int colour=bannerColour();
        return portraits.computeIfAbsent(spec.id()+"/"+colour,key->{
            // The sheet's entity type: battles spawn husks, skeletons and wither skeletons; the other Houses' beasts and crews show here first.
            MobEntity e=switch(spec.entity()){case "husk"->new HuskEntity(EntityType.HUSK,c.world);case "skeleton"->new SkeletonEntity(EntityType.SKELETON,c.world);case "wither_skeleton"->new WitherSkeletonEntity(EntityType.WITHER_SKELETON,c.world);
                default->{var made=Registries.ENTITY_TYPE.get(new Identifier(spec.entity())).create(c.world);yield made instanceof MobEntity mob?mob:new HuskEntity(EntityType.HUSK,c.world);}};
            e.setCustomName(Text.literal(spec.name()));e.setCustomNameVisible(false);e.setPersistent();e.setSilent(true);
            e.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Registries.ITEM.get(new Identifier(spec.weapon()))));e.equipStack(EquipmentSlot.HEAD,new ItemStack(Registries.ITEM.get(new Identifier(spec.helmet()))));
            var chest=new ItemStack(Registries.ITEM.get(new Identifier(spec.chest())));if(chest.getItem() instanceof DyeableItem dye)dye.setColor(chest,colour);e.equipStack(EquipmentSlot.CHEST,chest);
            if(Tactics.mechanic(spec).equals("shield"))e.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));return e;
        });
    }
    /** A portrait size that fits any model in the same frame: a husk at size, a wolf no larger than 1.25 times, a golem smaller. */
    static int size(MobEntity e,int size){return (int)Math.round(size*Math.min(1.25,1.95/Math.max(.6,Math.max(e.getHeight(),e.getWidth()))));}
    static final class Recruit extends ButtonWidget {
        final Spec.TroopsRow spec;boolean selected,owned;
        Recruit(Spec.TroopsRow spec,int x,int y,int w,PressAction action){super(x,y,w,78,Text.literal(spec.name()),action,DEFAULT_NARRATION_SUPPLIER);this.spec=spec;var h=house(spec);
            setTooltip(Tooltip.of(Text.literal(spec.name()+" / "+(h==null?spec.house():h.name())+" "+spec.rarity()+" / "+CommandMode.label(spec.group())+" / "+spec.health()+" health / "+spec.damage()+" damage"+(fieldable(spec)?"":" / cannot be fielded yet"))));}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            var c=MinecraftClient.getInstance();selected=c.currentScreen instanceof CampaignLobbyScreen lobby&&lobby.selectedUnit.equals(spec.id());owned=CampaignClient.profile.owned.containsKey(spec.id());
            int x=getX(),y=getY();DarkUi.frame(g,x,y,width,height);
            if(selected||active&&(isHovered()||isFocused()))g.drawBorder(x+1,y+1,width-2,height-2,DarkUi.GOLD);
            var h=house(spec);int col=0x33000000|(h==null?0x4c9391:h.accent()&0xffffff);g.fillGradient(x+4,y+4,x+width-4,y+55,col,0x00181820); // the House accent
            var e=portrait(spec);if(e!=null){e.age=c.player==null?0:c.player.age;InventoryScreen.drawEntity(g,x+width/2,y+52,size(e,22),20,0,e);}
            if(!active)g.fill(x+3,y+3,x+width-3,y+height-3,0x99111119);
            g.drawCenteredTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth(spec.name(),width-8),x+width/2,y+57,active?DarkUi.PAPER:DarkUi.MUTED);
            boolean note=!fieldable(spec)||spec.slotWeight()>1;String state=!fieldable(spec)?"NOT FIELDABLE":spec.slotWeight()>1?spec.slotWeight()+" SLOTS":CommandMode.label(spec.group());
            g.drawCenteredTextWithShadow(c.textRenderer,state,x+width/2,y+68,note?DarkUi.GOLD:DarkUi.MUTED);
            g.drawTextWithShadow(c.textRenderer,""+Collections.frequency(CampaignClient.profile.banner,spec.id()),x+width-13,y+7,DarkUi.GOLD);
        }
    }
    static final class Deployment extends ButtonWidget {
        final int index;String unit=RosterMath.EMPTY;
        Deployment(int index,int x,int y,PressAction action){super(x,y,51,32,Text.literal("Empty banner slot "+(index+1)),action,DEFAULT_NARRATION_SUPPLIER);this.index=index;}
        void setUnit(String id){unit=id;var spec=spec(id);setMessage(Text.literal("Slot "+(index+1)+": "+(spec!=null?spec.name():RosterMath.MARKER.equals(id)?"held by the unit before it":"Empty")));setTooltip(Tooltip.of(getMessage()));}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();var c=MinecraftClient.getInstance();var spec=spec(unit);boolean held=RosterMath.MARKER.equals(unit);
            g.fillGradient(x,y,x+width,y+height,spec!=null?0xff36302b:held?0xff2e2924:0xff19171d,0xff18151b);g.drawBorder(x,y,width,height,isHovered()||isFocused()?DarkUi.GOLD:DarkUi.EDGE);
            g.drawText(c.textRenderer,String.format("%02d",index+1),x+3,y+3,0xff7f735f,false);
            if(spec!=null){g.drawItem(new ItemStack(Registries.ITEM.get(new Identifier(spec.weapon()))),x+width/2-8,y+4);boolean wide=spec.slotWeight()>1;String label=wide?spec.rarity():CommandMode.label(spec.group());label=label.substring(0,Math.min(wide?6:3,label.length())).toUpperCase(); // an Epic or Legendary names its rarity in gold (its ^ slots follow); others their group, as before
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
