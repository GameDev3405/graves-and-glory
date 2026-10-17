package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import java.util.*;

/** Four authored 700 by 500 battlefields. Routes are also terrain constraints. */
public final class CampaignMaps {
    public static int selected=0;
    static final int LOBBY_Z=-310;
    /** Ready pads: a four-block square on the lobby floor. Dawn is on the west, Dusk on the east. */
    static final int PAD_Z=LOBBY_Z-14;
    static boolean onPad(int team,double x,double y,double z){double min=team==0?-11:8;return x>=min&&x<min+4&&z>=PAD_Z&&z<PAD_Z+4&&y>64&&y<68;}
    record Road(int ax,int az,int bx,int bz,int width){}
    static List<Road> roads=new ArrayList<>();
    public static void select(int index){
        selected=Math.max(0,Math.min(3,index));
        var structures=new ArrayList<Spec.StructuresRow>();var scenery=new ArrayList<Spec.SceneryRow>();
        var terrain=new ArrayList<Spec.TerrainRow>();roads=new ArrayList<>();
        String[] names=switch(selected){case 1->new String[]{"Supply Yard","Iron Gate","Watch Hill"};case 2->new String[]{"Caravan Refuge","Pass Crossing","Signal Ridge"};case 3->new String[]{"Logging Camp","Old Causeway","Moon Shrine"};default->new String[]{"Mill Hamlet","Trade Crossing","River Shrine"};};
        int[] zs=selected==1?new int[]{-130,0,130}:selected==2?new int[]{-155,0,155}:new int[]{-145,0,145};
        Spec.POINTS=new Spec.PointsRow[3];
        for(int i=0;i<3;i++){
            int z=zs[i],rx=(int)riverX(z),px=rx+(i==0?-29:i==2?29:0);
            Spec.POINTS[i]=new Spec.PointsRow(""+(char)('A'+i),names[i],px,z,17,8,64);
            structures.add(s("crossing_"+i,"bridge",rx,z,44,i==1?24:16,2,i==0?"wood":i==2?"ford":"stone",64));
            for(int side:new int[]{-1,1}){road(side*290,0,side*205,z,5);road(side*205,z,rx+side*28,z,5);}
            road(rx-29,z,rx+29,z,6);
            if(i==0){
                structures.add(s("mill","mill",rx-18,z-28,12,12,8,"wood",64));
                structures.add(s("farm","farm",rx-90,z-25,26,18,1,"wood",64));
                structures.add(s("market","market",rx-65,z+25,18,8,5,"dawn",64));
                for(int k=0;k<4;k++){int hx=rx-75-(k%2)*26,hz=z-4+(k/2)*31;structures.add(s("house_"+k,"house",hx,hz,14,12,9,"wood",64));road(hx,hz,rx-29,z,3);}
                road(rx-90,z-25,rx-29,z,3);road(rx-18,z-28,rx-29,z,3);
            } else if(i==1){
                if(selected==1){structures.add(s("ironhold","ruin",0,27,58,42,20,"stone",64));structures.add(s("breach","breach",-37,0,6,50,10,"stone",64));}
                else {structures.add(s("customs","house",rx-58,30,18,14,10,"wood",64));structures.add(s("warehouse","barracks",rx+60,-30,24,18,10,"stone",64));}
            } else structures.add(s("shrine","shrine",rx+59,z-18,22,18,13,"dusk",64));
        }
        for(int side:new int[]{-1,1}){
            String palette=side<0?"dawn":"dusk";
            structures.add(s(palette+"_gate","base",side*314,0,20,72,16,palette,64));
            structures.add(s(palette+"_barracks","barracks",side*286,-side*32,28,24,12,palette,64));
            structures.add(s(palette+"_stores","house",side*287,side*40,18,16,10,"wood",64));
            road(side*286,-32,side*286,40,5);road(side*205,zs[0],side*205,zs[2],4);
            for(int z:new int[]{-85,85}){structures.add(s(palette+"_tower_"+z,"watchtower",side*180,z,11,11,15,palette,67));road(side*180,z,side*205,z,3);}
            terrain.add(new Spec.TerrainRow(palette+"_ridge",side*138,-80,62,85,selected==2?28.0:13.0));
            terrain.add(new Spec.TerrainRow(palette+"_hill",side*130,100,65,70,selected==1?18.0:12.0));
            terrain.add(new Spec.TerrainRow(palette+"_shelter",side*265,0,48,64,-2.0));
        }
        terrain.add(new Spec.TerrainRow("north_mountains",0,-247,180,45,selected==2?45.0:20.0));
        terrain.add(new Spec.TerrainRow("south_mountains",0,247,180,45,selected==2?45.0:20.0));
        if(selected==1){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill")||st.kind().equals("shrine"));
            structures.add(s("supply_barn","house",-105,-160,24,18,11,"wood",64));
            structures.add(s("garrison","barracks",65,135,30,22,13,"stone",64));
            for(int side:new int[]{-1,1})for(int z:new int[]{-62,62})structures.add(s("curtain_"+side+"_"+z,"breach",side*72,z,5,68,13,"stone",64));
        }else if(selected==2){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill")||st.kind().equals("market"));
            structures.add(s("caravan_lodge","house",-80,-178,26,18,12,"wood",64));
            structures.add(s("pass_refuge","house",62,34,20,16,12,"wood",64));
            structures.add(s("signal_tower","watchtower",57,166,14,14,19,"stone",64));
            road(-80,-178,-20,-155,4);road(62,34,25,0,4);
        }else if(selected==3){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill"));
            structures.add(s("sawmill","house",-85,-176,24,18,10,"wood",64));
            structures.add(s("broken_chapel","ruin",70,100,28,22,12,"dusk",64));
            road(-85,-176,-35,-145,4);road(70,100,40,145,3);
        }
        Spec.STRUCTURES=structures.toArray(Spec.StructuresRow[]::new);Spec.TERRAIN=terrain.toArray(Spec.TerrainRow[]::new);
        // Place woodland only outside military roads, homes, fields and objectives.
        for(int x=-325;x<=325;x+=13)for(int z=-225;z<=225;z+=13){int hash=Math.floorMod(x*7343+z*9129,97);boolean forest=selected==3?hash<55:selected==2?hash<13:hash<22;
            if(!forest||roadDistance(x,z)<10||Math.abs(x-riverX(z))<17)continue;
            boolean clear=true;for(var st:Spec.STRUCTURES)if(Math.abs(x-st.x())<st.width()/2+9&&Math.abs(z-st.z())<st.depth()/2+9)clear=false;
            for(var p:Spec.POINTS)if(Math.hypot(x-p.x(),z-p.z())<30)clear=false;
            if(clear)scenery.add(new Spec.SceneryRow("tree_"+x+"_"+z,"tree",x,z,selected==3?10+hash%5:7+hash%3,selected==3?7:5,selected>=2?"spruce":"oak"));
        }
        Spec.SCENERY=scenery.toArray(Spec.SceneryRow[]::new);
        Spec.FACTIONS[0]=new Spec.FactionsRow("dawn","Dawn Covenant",5636095,-283,0,"knight,archer,mage",-286,32);
        Spec.FACTIONS[1]=new Spec.FactionsRow("dusk","Dusk Legion",11141290,283,0,"revenant,bonebow,hexer",286,-32);
    }
    static Spec.StructuresRow s(String id,String kind,int x,int z,int w,int d,int h,String palette,int y){return new Spec.StructuresRow(id,kind,x,z,w,d,h,palette,y);}
    static void road(int a,int b,int c,int d,int width){roads.add(new Road(a,b,c,d,width));}
    static double distance(double x,double z,Road r){double dx=r.bx-r.ax,dz=r.bz-r.az,t=Math.max(0,Math.min(1,((x-r.ax)*dx+(z-r.az)*dz)/(dx*dx+dz*dz)));return Math.hypot(x-r.ax-t*dx,z-r.az-t*dz);}
    static double roadDistance(int x,int z){double d=999;for(var r:roads)d=Math.min(d,distance(x,z,r)-r.width);return d;}
    public static boolean road(int x,int z){return roadDistance(x,z)<0;}
    public static double riverX(double z){return switch(selected){case 1->0;case 2->9*Math.sin(z*.013);case 3->18*Math.sin(z*.011);default->12*Math.sin(z*.014);};}
    static boolean river(int x,int z){return Math.abs(x-riverX(z))<(selected==3?9:6);}
    static void lobby(ServerWorld w){
        Arena.box(w,-42,64,LOBBY_Z-32,85,1,65,Blocks.STONE_BRICKS);
        for(int x=-42;x<=42;x++)for(int z=LOBBY_Z-32;z<=LOBBY_Z+32;z++){
            if(Math.abs(x)==42||Math.abs(z-LOBBY_Z)==32)Arena.box(w,x,65,z,1,2,1,Blocks.STONE_BRICK_WALL);
            else if(Math.abs(x)<5||Math.abs(z-LOBBY_Z)<4)Arena.put(w,x,64,z,Blocks.POLISHED_ANDESITE);
        }
        for(int x:new int[]{-32,32})for(int dz:new int[]{-23,23}){Arena.box(w,x,65,LOBBY_Z+dz,2,9,2,Blocks.STONE_BRICKS);Arena.box(w,x,74,LOBBY_Z+dz,2,2,2,Blocks.SEA_LANTERN);}
        var old=new ArrayList<net.minecraft.entity.Entity>();for(var e:w.iterateEntities())if(e.getCommandTags().contains("gravesandglory-lobby")||e.getCommandTags().contains("bannerbound-lobby"))old.add(e);for(var e:old)e.discard();
        int display=0;for(var unit:Spec.TROOPS)if(unit.count()>0){
            int x=-15+display*6;ArmorStandEntity stand=new ArmorStandEntity(w,x,65,LOBBY_Z+6);stand.setNoGravity(true);stand.setInvulnerable(true);stand.setCustomName(Text.literal(unit.name()));stand.setCustomNameVisible(true);stand.addCommandTag("gravesandglory-lobby");
            stand.equipStack(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(unit.weapon()))));
            stand.equipStack(EquipmentSlot.HEAD,new ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(unit.helmet()))));
            stand.equipStack(EquipmentSlot.CHEST,new ItemStack(net.minecraft.registry.Registries.ITEM.get(new net.minecraft.util.Identifier(unit.chest()))));w.spawnEntity(stand);display++;
        }
        // Open-sided recruitment hall, armoury and war-table pavilion.
        for(int cx:new int[]{-25,25}){Arena.box(w,cx-10,64,LOBBY_Z-20,21,1,22,Blocks.DARK_OAK_PLANKS);for(int dx:new int[]{-10,10})for(int dz:new int[]{-20,1})Arena.box(w,cx+dx,65,LOBBY_Z+dz,1,7,1,Blocks.STRIPPED_OAK_LOG);Arena.box(w,cx-11,72,LOBBY_Z-21,23,1,24,Blocks.DARK_OAK_SLAB);Arena.put(w,cx,65,LOBBY_Z-10,cx<0?Blocks.ENCHANTING_TABLE:Blocks.SMITHING_TABLE);Arena.put(w,cx+3,65,LOBBY_Z-10,Blocks.LECTERN);}
        Arena.box(w,-9,65,LOBBY_Z+14,19,1,9,Blocks.CHISELED_STONE_BRICKS);Arena.put(w,0,66,LOBBY_Z+18,Blocks.CARTOGRAPHY_TABLE);Arena.put(w,3,66,LOBBY_Z+18,Blocks.LECTERN);
        for(int team=0;team<2;team++)for(int x=team==0?-11:8;x<(team==0?-7:12);x++)for(int z=PAD_Z;z<PAD_Z+4;z++)Arena.put(w,x,64,z,team==0?Blocks.LIGHT_BLUE_WOOL:Blocks.MAGENTA_WOOL);
        for(int x=-18;x<=18;x+=6){Arena.put(w,x,65,LOBBY_Z-26,Blocks.LANTERN);}
        for(int x:new int[]{-38,38})for(int dz=-24;dz<=24;dz+=12)Arena.put(w,x,65,LOBBY_Z+dz,Blocks.LANTERN);
    }
}
