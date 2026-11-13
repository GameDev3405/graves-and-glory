package gg.gravesandglory;

import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.text.Text;
import java.util.*;

/** Authored 700 by 500 battlefields. Routes are also terrain constraints. */
public final class CampaignMaps {
    public static int selected=0;
    /** The route field last selected, whose cached goal fields are dropped when another map is. */
    private static NavField navHeld;
    /** The z of the camp origin. */
    static final int LOBBY_Z=Spec.CAMP[0].originZ();
    record Road(int ax,int az,int bx,int bz,int width){}
    static List<Road> roads=new ArrayList<>(),building=roads;
    /** How many objectives (capture points) a map has: the three crossings, plus the two flank outposts of Kingsfield (map 4). Read by the lobby without building the map. */
    public static int objectiveCount(int map){var e=EngineMaps.map(map);return e!=null?e.points.size():map==4?5:3;}
    public static synchronized void select(int index){
        selected=Math.max(0,Math.min(Spec.MAPS.length-1,index));
        if(navHeld!=null)navHeld.reset();var engine=EngineMaps.map(selected);navHeld=engine==null?null:engine.nav;if(navHeld!=null)navHeld.reset(); // a map was selected: no route field keeps the goals of the last one
        var structures=new ArrayList<Spec.StructuresRow>();var scenery=new ArrayList<Spec.SceneryRow>();
        var terrain=new ArrayList<Spec.TerrainRow>();building=new ArrayList<>();
        String[] names=switch(selected){case 1->new String[]{"Supply Yard","Iron Gate","Watch Hill"};case 2->new String[]{"Caravan Refuge","Pass Crossing","Signal Ridge"};case 3->new String[]{"Logging Camp","Old Causeway","Moon Shrine"};case 4->new String[]{"North Ford","Kings Bridge","South Ford"};default->new String[]{"Mill Hamlet","Trade Crossing","River Shrine"};};
        int[] zs=selected==1?new int[]{-130,0,130}:selected==4?new int[]{-160,0,160}:selected==2?new int[]{-155,0,155}:new int[]{-145,0,145};
        // Kingsfield adds a raised outpost on each flank, placed with half-turn symmetry so neither side is favoured.
        boolean flanks=selected==4;var points=new Spec.PointsRow[flanks?5:3]; // an engine slot (EngineMaps.apply) replaces these with its own points
        for(int i=0;i<3;i++){
            int z=zs[i],rx=(int)riverX(z),px=rx+(flanks?0:i==0?-29:i==2?29:0);
            points[flanks?i*2:i]=new Spec.PointsRow(""+(char)('A'+(flanks?i*2:i)),names[i],px,z,17,8,64);
            structures.add(s("crossing_"+i,"bridge",rx,z,44,i==1?24:16,2,i==0?"wood":i==2?"ford":"stone",64));
            for(int side:new int[]{-1,1}){
                var bend=fanBend(side,z);
                if(bend==null)road(side*290,0,side*205,z,5);else{road(side*290,0,bend[0],bend[1],5);road(bend[0],bend[1],side*205,z,5);}
                road(side*205,z,rx+side*28,z,5);
            }
            road(rx-29,z,rx+29,z,6);
            if(i==0){
                structures.add(s("mill","mill",rx-18,z-28,12,12,8,"wood",64));
                structures.add(s("farm","farm",rx-90,z-25,26,18,1,"wood",64));
                structures.add(s("market","market",rx-50,z+33,18,8,5,"dawn",64));
                for(int k=0;k<4;k++){int hx=rx-75-(k%2)*26,hz=z+12+(k/2)*15;structures.add(s("house_"+k,"house",hx,hz,14,12,9,"wood",64));road(hx,hz,rx-29,z,3);}
                road(rx-90,z-25,rx-29,z,3);road(rx-18,z-28,rx-29,z,3);
            } else if(i==1){
                if(selected==1){structures.add(s("ironhold","ruin",0,27,58,42,20,"stone",64));structures.add(s("breach","breach",-37,0,6,50,10,"stone",64));}
                else {boolean kings=selected==4;structures.add(s("customs","house",kings?-45:rx-58,kings?47:30,18,14,10,"wood",64));structures.add(s("warehouse","barracks",kings?45:rx+60,kings?-47:-30,24,18,10,"stone",64));}
            } else structures.add(s("shrine","shrine",rx+59,z-18,22,18,13,"dusk",64));
        }
        for(int side:new int[]{-1,1}){
            String palette=side<0?"dawn":"dusk";
            structures.add(s(palette+"_gate","base",side*314,0,20,72,16,palette,64));
            int team=side<0?0:1;structures.add(s(palette+"_barracks","barracks",barracksX(selected,team),barracksZ(team),28,24,12,palette,64));
            structures.add(s(palette+"_stores","house",side*287,side*40,18,16,10,"wood",64));
            road(side*286,-32,side*286,40,5);road(side*205,zs[0],side*205,zs[2],4);
            for(int z:new int[]{-85,85}){structures.add(s(palette+"_tower_"+z,"watchtower",side*180,z,11,11,15,palette,67));road(side*180,z,side*205,z,3);}
            terrain.add(new Spec.TerrainRow(palette+"_ridge",side*138,flanks?side*80:-80,62,85,selected==2?28.0:13.0));
            terrain.add(new Spec.TerrainRow(palette+"_hill",side*130,flanks?-side*100:100,65,70,selected==1?18.0:12.0));
            terrain.add(new Spec.TerrainRow(palette+"_shelter",side*265,0,48,64,-2.0));
        }
        terrain.add(new Spec.TerrainRow("north_mountains",0,-247,180,45,selected==2?45.0:20.0));
        terrain.add(new Spec.TerrainRow("south_mountains",0,247,180,45,selected==2?45.0:20.0));
        if(selected==1){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill")||st.kind().equals("shrine"));
            structures.add(s("supply_barn","house",-105,-160,24,18,11,"wood",64));
            structures.add(s("garrison","barracks",65,147,30,22,13,"stone",64));
            for(int side:new int[]{-1,1})for(int z:new int[]{-62,62})structures.add(s("curtain_"+side+"_"+z,"breach",side*72,z,5,68,13,"stone",64));
        }else if(selected==2){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill")||st.kind().equals("market"));
            structures.add(s("caravan_lodge","house",-80,-178,26,18,12,"wood",64));
            structures.add(s("pass_refuge","house",62,34,20,16,12,"wood",64));
            structures.add(s("signal_tower","watchtower",62,170,14,14,19,"stone",64));
            road(-80,-178,-20,-155,4);road(62,34,25,0,4);
        }else if(flanks){
            points[1]=new Spec.PointsRow("B","West Outpost",-130,100,17,8,74);
            points[3]=new Spec.PointsRow("D","East Outpost",130,-100,17,8,74);
            for(int side:new int[]{-1,1}){road(side*205,-side*100,side*130,-side*100,4);road(side*130,-side*100,side*28,0,4);}
        }else if(selected==3){
            structures.removeIf(st->st.kind().equals("farm")||st.kind().equals("mill"));
            structures.add(s("sawmill","house",-85,-176,24,18,10,"wood",64));
            structures.add(s("broken_chapel","ruin",70,100,28,22,12,"dusk",64));
            road(-85,-176,-35,-145,4);road(70,100,40,145,3);
        }
        landmarks(structures);
        // The supply houses (Phase 3 Task 8b): a spur from the yard centre of each house of this map out of its front gate to the middle lane, laid with the other roads so the woodland keeps off it and Walkability files the part inside the house as building
        for(var h:Spec.SUPPLYHOUSES)if(h.map()==selected)road(h.yardX(),h.yardZ(),h.spurX(),h.spurZ(),4);
        // The integrated client re-selects the same map while the server builds it,
        // so every shared table is swapped in whole, never left half filled.
        Spec.POINTS=points;roads=building;Spec.STRUCTURES=structures.toArray(Spec.StructuresRow[]::new);Spec.TERRAIN=terrain.toArray(Spec.TerrainRow[]::new);
        // Place woodland only outside military roads, homes, fields and objectives.
        for(int x=-325;x<=325;x+=13)for(int z=-225;z<=225;z+=13){int hash=Math.floorMod(x*7343+z*9129,97);boolean forest=selected==3?hash<55:selected==2?hash<13:hash<22;
            if(!forest||roadDistance(x,z)<10||Math.abs(x-riverX(z))<17)continue;
            boolean clear=true;for(var st:Spec.STRUCTURES)if(Math.abs(x-st.x())<st.width()/2+9&&Math.abs(z-st.z())<st.depth()/2+9)clear=false;
            for(var p:points)if(Math.hypot(x-p.x(),z-p.z())<30)clear=false;
            if(clear)scenery.add(new Spec.SceneryRow("tree_"+x+"_"+z,"tree",x,z,selected==3?10+hash%5:7+hash%3,selected==3?7:5,selected>=2?"spruce":"oak"));
        }
        Spec.POINTS=points;Spec.SCENERY=scenery.toArray(Spec.SceneryRow[]::new);
        // Rows 0 and 1 hold the team positions; each keeps its side identity (id, name, colour, glyph).
        for(int t=0;t<2;t++){var f=Spec.FACTIONS[t];Spec.FACTIONS[t]=new Spec.FactionsRow(f.id(),f.name(),f.color(),f.glyph(),f.dye(),spawnX(selected,t),0,barracksX(selected,t),barracksZ(t));}
        if(EngineMaps.on(selected))EngineMaps.apply(); // map engine Plan 2: an opt-in engine build replaces this slot
    }
    /** The spawn x of a team (0 Dawn, 1 Dusk) on a map; every spawn is at z 0. The one source for the team rows, the barracks and the client's map check. Ironhold uses Kort's signed
     *  D1a values (docs/maps/ironhold/layout-draft-2026-10-30.md, make_manifest.py): Dawn on the siege camp shelf at -314, Dusk in the Donjon Court at 128. Other maps: the camp muster at ±283. */
    public static int spawnX(int map,int team){if(EngineMaps.on(map))return EngineMaps.spawnX(map,team);return map==1?(team==0?-314:128):(team==0?-283:283);}
    /** The barracks of a team: 3 blocks behind its spawn in the camp (±286), except Ironhold's Dawn barracks, which keep their camp place (-286) because the spawn moved onto the gate shelf.
     *  Ironhold's Dusk barracks hall stands in the Donjon Court, clear of the north supply house. */
    static int barracksX(int map,int team){return team==0?-286:map==1?spawnX(map,1)+3:286;}
    static int barracksZ(int team){return team==0?32:-32;}
    /** The fan road from the muster point of a camp (side*290, 0) to a lane head (side*205, z) bends once on the side where the barracks of the camp stand (the Dawn barracks are south of the muster point, the Dusk barracks north of it), at (side*266, -side*12),
     *  so that the road passes the end of the hall 5 blocks off. It used to run straight and into the wall of the barracks. Null: the road is straight. */
    static int[] fanBend(int side,int z){return Integer.signum(z)==-side?new int[]{side*266,-side*12}:null;}
    static Spec.StructuresRow s(String id,String kind,int x,int z,int w,int d,int h,String palette,int y){return new Spec.StructuresRow(id,kind,x,z,w,d,h,palette,y);}
    /** The landmarks rows of the selected map, in sheet order: each removes the row it replaces and stands in its place as a "landmark" row with the size and palette of its piece (width and depth swapped at 90 and 270), so the woodland, the dressing and
     *  Arena.height keep off its footprint. A replaces that names no row, and that no earlier row of this map named, is logged. */
    static void landmarks(List<Spec.StructuresRow> structures){
        var named=new HashSet<String>();
        for(var r:Spec.LANDMARKS){
            if(r.map()!=selected)continue;
            if(!r.replaces().equals("-")&&!structures.removeIf(st->st.id().equals(r.replaces()))&&!named.contains(r.replaces()))System.out.println("[Graves and Glory] landmark "+r.id()+": no row "+r.replaces());
            named.add(r.replaces());
            Spec.PiecesRow piece=null;for(var p:Spec.PIECES)if(p.id().equals(r.template()))piece=p;
            if(piece==null){System.out.println("[Graves and Glory] landmark "+r.id()+": no piece "+r.template());continue;}
            boolean turned=r.rotation()==90||r.rotation()==270;
            structures.add(s(r.id(),"landmark",r.x(),r.z(),turned?piece.depth():piece.width(),turned?piece.width():piece.depth(),piece.height(),piece.palette(),r.elevation()));
        }
    }
    static void road(int a,int b,int c,int d,int width){building.add(new Road(a,b,c,d,width));}
    static double distance(double x,double z,Road r){double dx=r.bx-r.ax,dz=r.bz-r.az,t=Math.max(0,Math.min(1,((x-r.ax)*dx+(z-r.az)*dz)/(dx*dx+dz*dz)));return Math.hypot(x-r.ax-t*dx,z-r.az-t*dz);}
    static double roadDistance(int x,int z){double d=999;for(var r:roads)d=Math.min(d,distance(x,z,r)-r.width);return d;}
    public static boolean road(int x,int z){return roadDistance(x,z)<0;}
    public static double riverX(double z){return switch(selected){case 1->0;case 2->9*Math.sin(z*.013);case 3->18*Math.sin(z*.011);default->12*Math.sin(z*.014);};}
    static boolean river(int x,int z){return Math.abs(x-riverX(z))<(selected==3?9:6);}
    static void lobby(ServerWorld w){CampWorld.build(w);}
}
