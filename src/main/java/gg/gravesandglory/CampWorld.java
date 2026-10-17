package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import java.util.*;

/** The player's camp: a courtyard with identifiable, interactive destinations. */
final class CampWorld {
    static final String TAG="gravesandglory-camp-v3";
    static String generation="";
    static void build(ServerWorld w){
        generation=UUID.randomUUID().toString();
        var old=new ArrayList<net.minecraft.entity.Entity>();for(var e:w.iterateEntities())if(e.getCommandTags().contains(TAG)||e.getCommandTags().contains("gravesandglory-lobby")||e.getCommandTags().contains("bannerbound-lobby"))old.add(e);old.forEach(net.minecraft.entity.Entity::discard);
        int z=CampaignMaps.LOBBY_Z;
        // Replace the old hall as well as its floor; do not touch the battlefield.
        Arena.box(w,-42,65,z-32,85,19,65,Blocks.AIR);
        Arena.box(w,-42,64,z-32,85,1,65,Blocks.MOSS_BLOCK);
        for(int x=-41;x<=41;x++)for(int dz=-31;dz<=31;dz++){
            if(Math.abs(x)<5||Math.abs(dz)<4||Math.abs(x)<17&&dz< -9||Math.abs(dz+9)<2||Math.abs(dz-15)<2)Arena.put(w,x,64,z+dz,((x+dz)&3)==0?Blocks.MOSSY_STONE_BRICKS:Blocks.STONE_BRICKS);
            if(Math.abs(x)==41||Math.abs(dz)==31)Arena.box(w,x,65,z+dz,1,6,1,Blocks.STONE_BRICKS);
            if((Math.abs(x)==41||Math.abs(dz)==31)&&Math.floorMod(x+dz,4)<2)Arena.put(w,x,71,z+dz,Blocks.CHISELED_STONE_BRICKS);
        }
        for(int x:new int[]{-39,39})for(int dz:new int[]{-29,29}){Arena.box(w,x-2,65,z+dz-2,5,10,5,Blocks.STONE_BRICKS);Arena.box(w,x-2,75,z+dz-2,5,1,5,Blocks.DEEPSLATE_TILES);Arena.put(w,x,76,z+dz,Blocks.LANTERN);}
        for(int x:new int[]{-18,18}){Arena.box(w,x,65,z-25,1,10,1,Blocks.DARK_OAK_FENCE);Arena.box(w,x,72,z-25,4,4,1,x<0?Blocks.CYAN_WOOL:Blocks.PURPLE_WOOL);}
        // Sunken central heraldic seal, clear of movement routes.
        for(int x=-4;x<=4;x++)for(int dz=-4;dz<=4;dz++)if(Math.abs(x)+Math.abs(dz)<=5)Arena.put(w,x,64,z+dz,Math.abs(x)+Math.abs(dz)==5?Blocks.GOLD_BLOCK:Blocks.DEEPSLATE_TILES);
        for(int x:new int[]{-37,37})for(int dz:new int[]{-26,-6,15,26}){Arena.box(w,x,65,z+dz,1,4,1,Blocks.DARK_OAK_FENCE);Arena.put(w,x,69,z+dz,Blocks.LANTERN);}
        pavilion(w,-27,z-13,Blocks.CYAN_WOOL);pavilion(w,27,z-13,Blocks.PURPLE_WOOL);
        pavilion(w,-27,z+15,Blocks.GREEN_WOOL);pavilion(w,27,z+15,Blocks.BLUE_WOOL);
        Arena.put(w,-27,65,z-15,Blocks.SMITHING_TABLE);npc(w,-27,z-12,"Quartermaster / Army & shop","army");
        Arena.put(w,27,65,z-15,Blocks.ENCHANTING_TABLE);npc(w,27,z-12,"Recruiter / New troops","recruit");
        Arena.put(w,-27,65,z+12,Blocks.LECTERN);npc(w,-27,z+16,"Archivist / Daily contracts","contracts");
        Arena.put(w,27,65,z+12,Blocks.CARTOGRAPHY_TABLE);npc(w,27,z+16,"Steward / Parties & rooms","friends");
        Arena.box(w,-3,65,z+27,7,2,3,Blocks.POLISHED_BLACKSTONE_BRICKS);Arena.put(w,0,67,z+28,Blocks.GOLD_BLOCK);
        label(w,0,69,z+27,"FOUNDERS CHAPTER / 30 tiers","chapter");
        // Gate pads have raised frames, glowing inlays and overhead banners.
        for(int team=0;team<2;team++){
            int left=team==0?-11:8;
            Arena.box(w,left-1,64,CampaignMaps.PAD_Z-1,6,1,6,Blocks.POLISHED_DEEPSLATE);
            Arena.box(w,left,64,CampaignMaps.PAD_Z,4,1,4,team==0?Blocks.LIGHT_BLUE_CONCRETE:Blocks.PURPLE_CONCRETE);
            for(int x:new int[]{left-2,left+5}){Arena.box(w,x,65,CampaignMaps.PAD_Z,1,5,1,Blocks.STONE_BRICKS);Arena.put(w,x,70,CampaignMaps.PAD_Z,Blocks.LANTERN);}
            Arena.box(w,left-2,71,CampaignMaps.PAD_Z,8,1,1,Blocks.DARK_OAK_PLANKS);
            label(w,left+1.5,69,CampaignMaps.PAD_Z-1,(team==0?"DAWN":"DUSK")+" / Stand here to ready","match");
        }
        label(w,0,72,z-22,"WAR GATES / Local matches / Bots fill empty seats","match");
        label(w,0,68,z+23,"WELCOME TO BANNER CAMP / R opens your camp guide","home");
        for(int x:new int[]{-19,19})for(int dz:new int[]{8,22}){Arena.put(w,x,65,z+dz,Blocks.FLOWERING_AZALEA);Arena.put(w,x+1,65,z+dz,Blocks.AZALEA);}
        dress(w,z);
    }
    static void pavilion(ServerWorld w,int x,int z,net.minecraft.block.Block cloth){
        Arena.box(w,x-8,64,z-7,17,1,15,Blocks.SPRUCE_PLANKS);
        for(int dx:new int[]{-8,8})for(int dz:new int[]{-7,7})Arena.box(w,x+dx,65,z+dz,1,5,1,Blocks.STRIPPED_SPRUCE_LOG);
        for(int step=0;step<5;step++){
            int edge=9-step*2;Arena.box(w,x-edge,70+step,z-8,edge*2+1,1,17,cloth);
            for(int dz:new int[]{-8,8})Arena.box(w,x-edge,70+step,z+dz,edge*2+1,1,1,Blocks.DARK_OAK_PLANKS);
        }
        Arena.box(w,x-9,69,z-8,19,1,1,Blocks.STRIPPED_SPRUCE_LOG);Arena.box(w,x-9,69,z+8,19,1,1,Blocks.STRIPPED_SPRUCE_LOG);
        for(int dx:new int[]{-8,8}){Arena.box(w,x+dx,65,z-5,1,2,11,Blocks.SPRUCE_FENCE);Arena.put(w,x+dx,68,z,Blocks.CHAIN);}
        for(int dx:new int[]{-7,7})Arena.put(w,x+dx,69,z,Blocks.LANTERN);
    }
    static void npc(ServerWorld w,double x,double z,String name,String station){
        VillagerEntity v=new VillagerEntity(EntityType.VILLAGER,w);v.refreshPositionAndAngles(x,65,z,x<0?-90:90,0);v.setAiDisabled(true);v.setInvulnerable(true);v.setSilent(true);v.setPersistent();
        var profession=switch(station){case "army"->net.minecraft.village.VillagerProfession.ARMORER;case "recruit"->net.minecraft.village.VillagerProfession.CLERIC;case "contracts"->net.minecraft.village.VillagerProfession.LIBRARIAN;default->net.minecraft.village.VillagerProfession.CARTOGRAPHER;};
        v.setVillagerData(v.getVillagerData().withProfession(profession).withLevel(5));
        v.setCustomName(Text.literal(name));v.setCustomNameVisible(false);v.addCommandTag(TAG);v.addCommandTag("camp-generation:"+generation);v.addCommandTag("camp:"+station);w.spawnEntity(v);label(w,x,68.7,z,name,station);
    }
    static void dress(ServerWorld w,int z){
        // Carved wall buttresses and warm lamps make the courtyard readable after dusk.
        for(int x=-32;x<=32;x+=8)for(int side:new int[]{-1,1}){
            Arena.box(w,x,65,z+side*30,2,7,1,Blocks.POLISHED_ANDESITE);
            Arena.put(w,x,68,z+side*29,Blocks.LANTERN);
        }
        for(int x:new int[]{-12,12}){
            Arena.box(w,x-3,64,z+8,7,1,5,Blocks.POLISHED_ANDESITE);
            Arena.box(w,x-2,65,z+9,5,1,3,Blocks.MOSS_BLOCK);
            for(int dx:new int[]{-2,2})Arena.put(w,x+dx,66,z+10,Blocks.FLOWERING_AZALEA);
            Arena.box(w,x,65,z+10,1,4,1,Blocks.OAK_LOG);
            Arena.box(w,x-2,69,z+8,5,2,5,Blocks.AZALEA_LEAVES);
            Arena.box(w,x-1,71,z+9,3,1,3,Blocks.FLOWERING_AZALEA_LEAVES);
            Arena.box(w,x-2,65,z+16,5,1,1,Blocks.SPRUCE_SLAB);
            for(int dx:new int[]{-3,3})Arena.put(w,x+dx,65,z+16,Blocks.SPRUCE_FENCE);
        }
        // Armory stock, a forge and displayed equipment; all outside the walking lane.
        for(int x=-32;x<=-23;x+=3){Arena.put(w,x,65,z-19,Blocks.BARREL);Arena.put(w,x,66,z-19,Blocks.CHEST);}
        Arena.box(w,-34,65,z-13,2,2,3,Blocks.POLISHED_BLACKSTONE_BRICKS);Arena.put(w,-34,67,z-12,Blocks.ANVIL);
        for(int dx:new int[]{-3,3}){
            var stand=EntityType.ARMOR_STAND.create(w);if(stand!=null){stand.refreshPositionAndAngles(-27+dx,65,z-17,0,0);stand.setInvulnerable(true);stand.setNoGravity(true);
                stand.equipStack(net.minecraft.entity.EquipmentSlot.CHEST,new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_CHESTPLATE));
                stand.equipStack(net.minecraft.entity.EquipmentSlot.HEAD,new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_HELMET));
                stand.addCommandTag(TAG);stand.addCommandTag("camp-generation:"+generation);w.spawnEntity(stand);}
        }
        // Recruitment shrine has a crystal focus and a glowing floor ring.
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)if(Math.max(Math.abs(dx),Math.abs(dz))==3)Arena.put(w,27+dx,64,z-15+dz,Blocks.AMETHYST_BLOCK);
        Arena.box(w,26,65,z-19,3,2,1,Blocks.CRYING_OBSIDIAN);Arena.put(w,27,67,z-19,Blocks.AMETHYST_CLUSTER);
        for(int x:new int[]{23,31}){Arena.box(w,x,65,z-18,1,3,1,Blocks.POLISHED_BLACKSTONE_BRICKS);Arena.put(w,x,68,z-18,Blocks.SOUL_LANTERN);}
        // Archive and strategy room furniture carry their roles into the world.
        Arena.box(w,-33,65,z+20,13,3,1,Blocks.BOOKSHELF);
        for(int x:new int[]{-31,-23}){Arena.put(w,x,65,z+17,Blocks.LECTERN);Arena.put(w,x,66,z+20,Blocks.LANTERN);}
        Arena.box(w,23,65,z+20,9,1,2,Blocks.DARK_OAK_SLAB);
        for(int x:new int[]{24,30}){Arena.put(w,x,66,z+20,Blocks.LANTERN);Arena.put(w,x,65,z+18,Blocks.SPRUCE_STAIRS);}
        Arena.put(w,27,66,z+20,Blocks.CARTOGRAPHY_TABLE);
        // Monument silhouette and enclosed light keep the spawn approach open.
        for(int x:new int[]{-4,4}){Arena.box(w,x,65,z+28,1,5,1,Blocks.CHISELED_POLISHED_BLACKSTONE);Arena.put(w,x,70,z+28,Blocks.LANTERN);}
        Arena.box(w,-2,68,z+29,5,3,1,Blocks.DEEPSLATE_TILES);Arena.put(w,0,71,z+29,Blocks.GOLD_BLOCK);
        for(int team=0;team<2;team++){
            int left=team==0?-11:8;var cloth=team==0?Blocks.CYAN_WOOL:Blocks.PURPLE_WOOL;
            for(int x:new int[]{left-2,left+5}){Arena.box(w,x-1,64,CampaignMaps.PAD_Z-1,3,1,3,Blocks.CHISELED_STONE_BRICKS);Arena.put(w,x,68,CampaignMaps.PAD_Z-1,team==0?Blocks.LIGHT_BLUE_BANNER:Blocks.PURPLE_BANNER);}
            Arena.box(w,left-1,72,CampaignMaps.PAD_Z,6,1,1,Blocks.POLISHED_ANDESITE);
            Arena.box(w,left,73,CampaignMaps.PAD_Z,4,1,1,cloth);Arena.put(w,left+1,74,CampaignMaps.PAD_Z,Blocks.GOLD_BLOCK);
            for(int dx:new int[]{0,3})for(int dz:new int[]{0,3})Arena.put(w,left+dx,64,CampaignMaps.PAD_Z+dz,Blocks.SEA_LANTERN);
        }
    }
    static void label(ServerWorld w,double x,double y,double z,String name,String station){
        var label=EntityType.TEXT_DISPLAY.create(w);if(label==null)return;
        var nbt=label.writeNbt(new net.minecraft.nbt.NbtCompound());nbt.putString("text",Text.Serializer.toJson(Text.literal(name).formatted(net.minecraft.util.Formatting.GOLD)));
        nbt.putString("billboard","center");nbt.putInt("background",0xb0192227);nbt.putBoolean("shadow",true);nbt.putInt("line_width",220);nbt.putFloat("view_range",2f);
        var transform=new net.minecraft.nbt.NbtCompound();var scale=new net.minecraft.nbt.NbtList();for(int i=0;i<3;i++)scale.add(net.minecraft.nbt.NbtFloat.of(1.7f));transform.put("scale",scale);var translation=new net.minecraft.nbt.NbtList();for(int i=0;i<3;i++)translation.add(net.minecraft.nbt.NbtFloat.of(0));transform.put("translation",translation);
        for(String key:new String[]{"left_rotation","right_rotation"}){var rotation=new net.minecraft.nbt.NbtList();for(int i=0;i<4;i++)rotation.add(net.minecraft.nbt.NbtFloat.of(i==3?1:0));transform.put(key,rotation);}nbt.put("transformation",transform);
        label.readNbt(nbt);label.refreshPositionAndAngles(x,y,z,0,0);label.addCommandTag(TAG);label.addCommandTag("camp-generation:"+generation);label.addCommandTag("camp:"+station);w.spawnEntity(label);
    }
    static String station(BlockPos p){int dz=p.getZ()-CampaignMaps.LOBBY_Z;
        if(Math.abs(p.getX()+27)<=3&&Math.abs(dz+15)<=3)return "army";
        if(Math.abs(p.getX()-27)<=3&&Math.abs(dz+15)<=3)return "recruit";
        if(Math.abs(p.getX()+27)<=3&&Math.abs(dz-12)<=3)return "contracts";
        if(Math.abs(p.getX()-27)<=3&&Math.abs(dz-12)<=3)return "friends";
        if(Math.abs(p.getX())<=3&&dz>=26&&dz<=29)return "chapter";return "";}
}
