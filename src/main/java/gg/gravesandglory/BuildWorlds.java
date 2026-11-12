package gg.gravesandglory;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The builder server's seven canvases (Kort 2026-11-03; the camp canvas dropped 2026-11-08, its stations live in the Old Hold lobby): lobby and one per map, each a flat dimension gravesandglory:build_(id) of the gg_build data pack
 * (resourcepacks/gg_build, registered only in build mode). A plan is what BuildMode paints once on a canvas, and its /warp names. Camp and lobby carry the
 * LobbyCanvas camp plan. A map canvas carries the map's key points from CampaignMaps.select (the same tables a battle builds from): the edge of the ground a
 * battle lays, the playable border, both teams' gates, barracks and spawns, the capture points, the lane crossings, the supply houses and every road as a line.
 */
final class BuildWorlds {
    record Plan(String id,RegistryKey<World> key,List<LobbyCanvas.Area> areas,List<LobbyCanvas.Line> lines,Map<String,double[]> warps){}

    static List<String> ids(){var all=new ArrayList<String>();all.add("lobby");for(var m:Spec.MAPS)all.add(m.id());return all;}
    static RegistryKey<World> key(String id){return RegistryKey.of(RegistryKeys.WORLD,new Identifier("gravesandglory","build_"+id));}
    static String safe(String id){return id.toLowerCase().replaceAll("[^a-z0-9_-]","_");}

    /** Every plan, the lobby first, then the maps in Spec.MAPS order. Selects each map in turn (build mode has no battle to disturb). */
    static List<Plan> plans(){
        var all=new ArrayList<Plan>();
        for(String id:List.of("lobby")){var warps=new LinkedHashMap<String,double[]>();LobbyCanvas.warps(warps);all.add(new Plan(id,key(id),LobbyCanvas.plan(),List.of(),warps));}
        for(int m=0;m<Spec.MAPS.length;m++)all.add(map(m));
        return all;
    }

    static Plan map(int m){
        CampaignMaps.select(m);
        var R=GravesAndGlory.R;String id=Spec.MAPS[m].id();
        var areas=new ArrayList<LobbyCanvas.Area>();var lines=new ArrayList<LobbyCanvas.Line>();
        areas.add(new LobbyCanvas.Area("","CANVAS EDGE","A battle lays ground out to this line. Build inside it.",Blocks.LIGHT_GRAY_CONCRETE,-R.worldHalfX(),-R.worldHalfZ(),R.worldHalfX(),R.worldHalfZ(),false));
        areas.add(new LobbyCanvas.Area("","PLAYABLE BORDER","Battles stay inside this line: the barrier wall stands on it.",Blocks.WHITE_CONCRETE,-Field.halfX(m),-Field.halfZ(m),Field.halfX(m),Field.halfZ(m),false));
        String[] team={"Dawn","Dusk"},palette={"dawn","dusk"};Block[] colour={Blocks.LIGHT_BLUE_CONCRETE,Blocks.BLUE_CONCRETE};
        for(int t=0;t<2;t++){
            String low=palette[t],name=team[t];
            box(areas,id,low+"_gate","gate-"+low,name.toUpperCase()+" GATE",name+"'s base gate: the team's home edge.",Blocks.BROWN_CONCRETE);
            box(areas,id,low+"_barracks","barracks-"+low,name.toUpperCase()+" BARRACKS",name+"'s barracks hall.",Blocks.BLACK_CONCRETE);
            int sx=CampaignMaps.spawnX(m,t);
            areas.add(new LobbyCanvas.Area("spawn-"+low,name.toUpperCase()+" SPAWN",name+" commanders and troops arrive here.",colour[t],sx-3,-3,sx+3,3,false));
        }
        for(var p:Spec.POINTS)areas.add(new LobbyCanvas.Area("point-"+safe(p.id()),"POINT "+p.id(),p.name()+": capture point, radius "+p.radius()+".",Blocks.RED_CONCRETE,p.x()-p.radius(),p.z()-p.radius(),p.x()+p.radius(),p.z()+p.radius(),false,true));
        String[] lane={"north","middle","south"};
        for(int i=0;i<3;i++)box(areas,id,"crossing_"+i,"lane-"+lane[i],lane[i].toUpperCase()+" LANE","The "+lane[i]+" lane crosses the river here.",Blocks.CYAN_CONCRETE);
        for(var h:Spec.SUPPLYHOUSES){
            if(h.map()!=m)continue;
            String warp="house-"+safe(h.id()),text=h.name()+": a supply house. Its yard is the centre.";
            if(!box(areas,id,h.landmark(),warp,"SUPPLY HOUSE",text,Blocks.YELLOW_CONCRETE))areas.add(new LobbyCanvas.Area(warp,"SUPPLY HOUSE",text,Blocks.YELLOW_CONCRETE,h.yardX()-5,h.yardZ()-5,h.yardX()+5,h.yardZ()+5,false));
        }
        for(var r:CampaignMaps.roads)lines.add(new LobbyCanvas.Line(r.ax(),r.az(),r.bx(),r.bz(),Blocks.LIME_CONCRETE));
        var warps=new LinkedHashMap<String,double[]>();
        warps.put("center",new double[]{0,LobbyCanvas.GROUND+1,0,180});
        for(var a:areas)if(!a.warp().isEmpty())warps.put(a.warp(),new double[]{a.cx(),LobbyCanvas.GROUND+1,a.z1()+2,180});
        return new Plan(id,key(id),areas,lines,warps);
    }

    /** The footprint of the structures row with this id (the selected map's), as an outlined area; false if the map has no such row. */
    static boolean box(List<LobbyCanvas.Area> areas,String map,String row,String warp,String title,String text,Block colour){
        for(var s:Spec.STRUCTURES)if(s.id().equals(row)){
            int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2;
            areas.add(new LobbyCanvas.Area(warp,title,text,colour,x0,z0,x0+s.width()-1,z0+s.depth()-1,false));
            return true;
        }
        BuildMode.log(map+": no structures row "+row+"; "+warp+" not outlined");
        return false;
    }
}
