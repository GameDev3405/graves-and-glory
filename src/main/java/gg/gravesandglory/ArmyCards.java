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
    static MobEntity portrait(Spec.TroopsRow spec){
        var c=MinecraftClient.getInstance();if(world!=c.world){portraits.clear();world=c.world;}if(c.world==null)return null;
        return portraits.computeIfAbsent(spec.id(),id->{
            MobEntity e=switch(spec.entity()){case "skeleton"->new SkeletonEntity(EntityType.SKELETON,c.world);case "wither_skeleton"->new WitherSkeletonEntity(EntityType.WITHER_SKELETON,c.world);default->new HuskEntity(EntityType.HUSK,c.world);};
            e.setCustomName(Text.literal(spec.name()));e.setCustomNameVisible(false);e.setPersistent();e.setSilent(true);
            e.equipStack(EquipmentSlot.MAINHAND,new ItemStack(Registries.ITEM.get(new Identifier(spec.weapon()))));e.equipStack(EquipmentSlot.HEAD,new ItemStack(Registries.ITEM.get(new Identifier(spec.helmet()))));
            var chest=new ItemStack(Registries.ITEM.get(new Identifier(spec.chest())));if(chest.getItem() instanceof DyeableItem dye)dye.setColor(chest,spec.faction().equals("dawn")?Spec.FACTIONS[0].color():Spec.FACTIONS[1].color());e.equipStack(EquipmentSlot.CHEST,chest);
            if(spec.ability().equals("shield"))e.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));return e;
        });
    }
    static final class Recruit extends ButtonWidget {
        final Spec.TroopsRow spec;boolean selected,unlocked;
        Recruit(Spec.TroopsRow spec,int x,int y,int w,PressAction action){super(x,y,w,78,Text.literal(spec.name()),action,DEFAULT_NARRATION_SUPPLIER);this.spec=spec;setTooltip(Tooltip.of(Text.literal(spec.name()+" / "+CommandMode.label(spec.group())+" / "+spec.health()+" health / "+spec.damage()+" damage / "+spec.ability())));}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            var c=MinecraftClient.getInstance();selected=((CampaignLobbyScreen)c.currentScreen).selectedUnit.equals(spec.id());unlocked=CampaignClient.profile.unlocked.contains(spec.id());
            int x=getX(),y=getY();DarkUi.frame(g,x,y,width,height);
            if(selected||active&&(isHovered()||isFocused()))g.drawBorder(x+1,y+1,width-2,height-2,DarkUi.GOLD);
            int col=spec.faction().equals("dawn")?0x334c9391:0x335e357b;g.fillGradient(x+4,y+4,x+width-4,y+55,col,0x00181820);
            var e=portrait(spec);if(e!=null){e.age=c.player==null?0:c.player.age;InventoryScreen.drawEntity(g,x+width/2,y+52,22,20,0,e);}
            if(!active)g.fill(x+3,y+3,x+width-3,y+height-3,0x99111119);
            String name=spec.name().replace("Dawn ","").replace("Dusk ","");g.drawCenteredTextWithShadow(c.textRenderer,c.textRenderer.trimToWidth(name,width-8),x+width/2,y+57,active?DarkUi.PAPER:DarkUi.MUTED);
            String state=unlocked?CommandMode.label(spec.group()):CampaignClient.mode.equals("ai")?"TRIAL":"LOCKED";g.drawCenteredTextWithShadow(c.textRenderer,state,x+width/2,y+68,unlocked?DarkUi.MUTED:DarkUi.GOLD);
            g.drawTextWithShadow(c.textRenderer,""+CampaignClient.profile.army.getOrDefault(spec.id(),0),x+width-13,y+7,DarkUi.GOLD);
        }
    }
    static final class Deployment extends ButtonWidget {
        final int index;String unit="empty";
        Deployment(int index,int x,int y,PressAction action){super(x,y,51,32,Text.literal("Empty deployment slot "+(index+1)),action,DEFAULT_NARRATION_SUPPLIER);this.index=index;}
        void setUnit(String id){unit=id;var spec=spec(id);setMessage(Text.literal("Slot "+(index+1)+": "+(spec==null?"Empty":spec.name())));setTooltip(Tooltip.of(getMessage()));}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            int x=getX(),y=getY();var c=MinecraftClient.getInstance();var spec=spec(unit);
            g.fillGradient(x,y,x+width,y+height,spec==null?0xff19171d:0xff36302b,0xff18151b);g.drawBorder(x,y,width,height,isHovered()||isFocused()?DarkUi.GOLD:DarkUi.EDGE);
            g.drawText(c.textRenderer,String.format("%02d",index+1),x+3,y+3,0xff7f735f,false);
            if(spec!=null){g.drawItem(new ItemStack(Registries.ITEM.get(new Identifier(spec.weapon()))),x+width/2-8,y+4);String label=CommandMode.label(spec.group());g.drawCenteredTextWithShadow(c.textRenderer,label.substring(0,Math.min(3,label.length())).toUpperCase(),x+width/2,y+23,DarkUi.PAPER);}
            else g.drawCenteredTextWithShadow(c.textRenderer,"+",x+width/2,y+13,DarkUi.MUTED);
        }
    }
}
