package gg.gravesandglory;
import net.minecraft.block.*;
import net.minecraft.block.enums.SlabType;
import net.minecraft.block.enums.WallShape;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.world.Heightmap;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Authored terrain shared by world construction, navigation and the map. */
public final class Arena {
    static final int Y=GravesAndGlory.R.floorY();
    /** True while the arena build (column, finish) runs: its blocks are written with flags 2|16 (force state), so no neighbour shape update runs and clearing a block cannot break
     *  the plant, lantern or crop that stood on or against it and spill it as an item (column clears each column bottom to top). The lobby (CampWorld) shares put and box and keeps plain 2. */
    static boolean building;
    static int flags(){return building?Block.NOTIFY_LISTENERS|Block.FORCE_STATE:Block.NOTIFY_LISTENERS;}
    /** How many loose items the last finish found in the built area and discarded. */
    static volatile int discarded;
    /** The fences, walls and panes the build placed, in order (server thread only). A placement with flag 16 runs no shape update, so settleShapes connects them once the build is done. */
    static final List<BlockPos> shaped=new ArrayList<>();
    static boolean connects(Block b){return b instanceof HorizontalConnectingBlock||b instanceof WallBlock;}
    static void note(BlockPos pos,Block b){if(building&&connects(b))shaped.add(pos);}
    static void put(ServerWorld w,int x,int y,int z,Block b){var pos=new BlockPos(x,y,z);w.setBlockState(pos,b.getDefaultState(),flags());note(pos,b);}
    static void box(ServerWorld w,int x,int y,int z,int sx,int sy,int sz,Block b){for(int a=0;a<sx;a++)for(int h=0;h<sy;h++)for(int c=0;c<sz;c++)put(w,x+a,y+h,z+c,b);}
    public static double riverX(double z){return CampaignMaps.riverX(z);}
    public static boolean river(int x,int z){return CampaignMaps.river(x,z);}
    /** A bridge rail stands only where water is within a column of it and no building is paved over the river there (the courtyard of Ironhold covers it, and a rail there would stand on a floor). */
    static boolean wet(int px,int row){return (river(px-1,row)||river(px,row)||river(px+1,row))&&!paved(px,row);}
    /** Inside the footprint of a structure that is not a bridge (the test MapDressing.onStructure makes with no margin). */
    static boolean paved(int x,int z){for(var s:Spec.STRUCTURES)if(!s.kind().equals("bridge")&&Math.abs(x-s.x())<=s.width()/2&&Math.abs(z-s.z())<=s.depth()/2)return true;return false;}
    /** Water in the column or in either column beside it. */
    static boolean wetBank(int x,int z){return river(x-1,z)||river(x,z)||river(x+1,z);}
    /** A cell of a bridge: its planks, and the two rail rows beside them. */
    static boolean deckCell(int x,int z){
        for(var s:Spec.STRUCTURES)if(s.kind().equals("bridge")){int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2;if(x>=x0&&x<x0+s.width()&&z>=z0-1&&z<=z0+s.depth())return true;}
        return false;
    }
    /** Whether the ring of a capture point is laid at all: its band (radius-1 to radius out) has to run on the ground, off the river and its banks and off the decks, for at least 40 cells in one piece round the circle. At a point on the river
     *  the water and the deck cut the band into pieces of a few cells, and those would lie on the banks as loose pale blocks; there the point is the bridge, and neither the ring nor the edge stone of MapDressing is laid. */
    public static boolean ringLaid(Spec.PointsRow p){
        int r=p.radius();var cells=new ArrayList<int[]>();
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){double d=Math.hypot(dx,dz);if(d>=r-1&&d<=r)cells.add(new int[]{dx,dz});}
        cells.sort(java.util.Comparator.comparingDouble(c->Math.atan2(c[1],c[0])));
        int n=cells.size(),run=0,best=0;
        for(int i=0;i<2*n;i++){var c=cells.get(i%n);int x=p.x()+c[0],z=p.z()+c[1];if(!wetBank(x,z)&&!deckCell(x,z)){run++;best=Math.max(best,Math.min(run,n));}else run=0;}
        return best>=40;
    }
    public static boolean road(int x,int z){return CampaignMaps.road(x,z);}
    static double blend(double h,double flat,double dist,double radius,double shoulder){double t=MathHelper.clamp((dist-radius)/shoulder,0,1);t=t*t*(3-2*t);return flat+(h-flat)*t;}
    public static int height(int x,int z){
        double h=Y;
        for(var hill:Spec.TERRAIN){double wx=x+Spec.TERRAINSTYLE[0].warpAmplitude()*Math.sin(z*Spec.TERRAINSTYLE[0].warpFrequency()),wz=z+5*Math.sin(x*.043);double a=(wx-hill.x())/(double)hill.radiusX(),b=(wz-hill.z())/(double)hill.radiusZ();h+=hill.height()*Math.exp(-2*(a*a+b*b));}
        h+=.5*Spec.TERRAINSTYLE[0].roughness()*(Math.sin(x*.067+z*.031)+.5*Math.sin(z*.13-x*.043));
        double d=Math.abs(x-riverX(z));if(d<14)h=blend(h,Y-3,d,5,9);
        if(d>=5&&d<14)h=Math.max(Y-1,h);
        for(var s:Spec.STRUCTURES)if(!s.kind().equals("bridge")){double dx=Math.max(0,Math.abs(x-s.x())-s.width()/2.0-2),dz=Math.max(0,Math.abs(z-s.z())-s.depth()/2.0-2);h=blend(h,s.elevation(),Math.hypot(dx,dz),0,10);}
        for(var p:Spec.POINTS)h=blend(h,p.elevation(),Math.hypot(x-p.x(),z-p.z()),p.radius()+2,9);
        // Bridge decks and their approaches must be walkable even where a hill
        // meets the river. Clear/flatten the entire deck and blend broad ramps.
        for(var s:Spec.STRUCTURES)if(s.kind().equals("bridge")){double dx=Math.max(0,Math.abs(x-s.x())-s.width()/2.0-1),dz=Math.max(0,Math.abs(z-s.z())-s.depth()/2.0-1);h=blend(h,s.elevation(),Math.hypot(dx,dz),0,12);}
        double rd=CampaignMaps.roadDistance(x,z);
        if(rd<8)h=blend(h,roadLevel(x,z),rd,0,8);
        double border=Math.max(Math.abs(x)-GravesAndGlory.R.arenaHalfX()+14,Math.abs(z)-GravesAndGlory.R.arenaHalfZ()+12);
        if(border>0)h+=Math.min(Spec.TERRAINSTYLE[0].boundaryRise(),border*.4)*(.65+.35*Math.sin(x*.055+z*.043));
        return (int)Math.round(h);
    }
    /** How far a road climbs to a raised capture point (Kingsfield's outposts): from the floor of the arena at the end of this distance beyond the plateau of the point up to the elevation of the point at the edge of the plateau. */
    static final double CLIMB=48;
    /** The height a road is laid at: the floor of the arena, and for a road that runs to a raised point, the elevation of the point on its plateau (radius+2 round the centre) and the climb up to it. A road that ends at such a point therefore meets
     *  the ground of the point, where it used to cut a trench ten blocks deep through the plateau and leave the ring, the pad and the flag hanging in the air over it. */
    static double roadLevel(int x,int z){
        double level=Y;
        for(var p:Spec.POINTS)if(p.elevation()>Y){
            double t=MathHelper.clamp(1-(Math.hypot(x-p.x(),z-p.z())-(p.radius()+2))/CLIMB,0,1);t=t*t*(3-2*t);
            level=Math.max(level,Y+(p.elevation()-Y)*t);
        }
        return level;
    }
    public static int ground(int x,int z){for(var s:Spec.STRUCTURES)if(s.kind().equals("bridge")&&Math.abs(x-s.x())<=s.width()/2&&Math.abs(z-s.z())<=s.depth()/2)return s.elevation();return river(x,z)?Y-3:height(x,z);}
    public static int mapColor(int x,int z){
        int h=height(x,z),color=river(x,z)?0x416d88:road(x,z)?0xa79b75:h>79?0x687270:h>72?0x68815a:0x507b49;
        for(var s:Spec.STRUCTURES)if(Math.abs(x-s.x())<=s.width()/2&&Math.abs(z-s.z())<=s.depth()/2)color=s.kind().equals("bridge")?0xaab0ab:s.palette().equals("wood")?0x9c7958:0x919692;
        if(!river(x,z)&&!road(x,z)){if(CampaignMaps.selected==2)color=h>89?0x9bafb5:0xd4dedc;else if(CampaignMaps.selected==3)color=0x354d41;else if(CampaignMaps.selected==1)color=h>72?0x757965:0x7c8658;}
        double k=.9+Math.min(.18,(height(x-1,z)-height(x+1,z))*.025);
        return 0xff000000|((int)((color>>16&255)*k)<<16)|((int)((color>>8&255)*k)<<8)|(int)((color&255)*k);
    }
    public static void column(ServerWorld w,int x,int z){building=true;try{buildColumn(w,x,z);}finally{building=false;}}
    static void buildColumn(ServerWorld w,int x,int z){
        w.getChunk(x>>4,z>>4); // load it first: getTopY answers the world bottom for an unloaded chunk, and without shape updates nothing else has read this chunk yet
        int h=height(x,z),top=w.getTopY(Heightmap.Type.WORLD_SURFACE,x,z);
        int from=river(x,z)?Math.min(h+1,Y):h+1; // a river column writes water up to Y-1 and nothing at y=h, so from Y up the block of the map built before would survive at h
        for(int y=from;y<=top;y++)if(!w.getBlockState(new BlockPos(x,y,z)).isAir())put(w,x,y,z,Blocks.AIR);
        for(int y=Y-9;y<h-2;y++)put(w,x,y,z,Blocks.STONE);
        put(w,x,h-2,z,Blocks.STONE);put(w,x,h-1,z,Blocks.DIRT);
        if(river(x,z)){for(int y=Y-5;y<=Y-3;y++)put(w,x,y,z,Blocks.GRAVEL);for(int y=Y-2;y<=Y-1;y++)put(w,x,y,z,Blocks.WATER);}
        else{
            put(w,x,h,z,road(x,z)?((x+z)%5==0?Blocks.COARSE_DIRT:Blocks.DIRT_PATH):h>94?Blocks.STONE:CampaignMaps.selected==2?Blocks.SNOW_BLOCK:CampaignMaps.selected==3?Blocks.PODZOL:Blocks.GRASS_BLOCK);
            var deco=Spec.DECORATIONS[0];int hash=Math.floorMod(x*734287+z*912931+x*z*17,100003);
            boolean clear=!road(x,z)&&h<84&&Math.abs(x-riverX(z))>15;
            for(var p:Spec.POINTS)if(Math.hypot(x-p.x(),z-p.z())<deco.avoidPointRadius())clear=false;
            for(var s:Spec.STRUCTURES)if(Math.abs(x-s.x())<s.width()/2+deco.structureMargin()&&Math.abs(z-s.z())<s.depth()/2+deco.structureMargin())clear=false;
            if(clear&&hash%deco.plantEvery()==0)put(w,x,h+1,z,CampaignMaps.selected==2?Blocks.SNOW:CampaignMaps.selected==3?Blocks.FERN:hash%deco.flowerEvery()==0?Blocks.POPPY:Blocks.GRASS);
        }
        if(Math.abs(x)==GravesAndGlory.R.arenaHalfX()||Math.abs(z)==GravesAndGlory.R.arenaHalfZ())box(w,x,h+1,z,1,6,1,Blocks.BARRIER);
    }
    public static void finish(ServerWorld w){building=true;try{finishBuild(w);settleShapes(w);discardLoose(w);}finally{building=false;shaped.clear();}}
    static void finishBuild(ServerWorld w){
        w.setTimeOfDay(CampaignMaps.selected==3?11500:CampaignMaps.selected==2?6000:3000);
        for(var s:Spec.STRUCTURES)structure(w,s);
        clearDecks(w);
        for(var p:Spec.POINTS){
            boolean laid=ringLaid(p);
            for(int dx=-p.radius();dx<=p.radius();dx++)for(int dz=-p.radius();dz<=p.radius();dz++){
                double d=Math.hypot(dx,dz);int x=p.x()+dx,z=p.z()+dz;
                if(laid&&d>=p.radius()-1&&d<=p.radius()&&!wetBank(x,z)&&!deckCell(x,z))put(w,x,p.elevation(),z,Blocks.SMOOTH_STONE); // the ring is laid on the ground: not across the river, not across a deck
            }
            box(w,p.x()-1,p.elevation(),p.z()-1,3,1,3,Blocks.CHISELED_STONE_BRICKS);flag(w,p,-1);
        }
        for(var s:Spec.SCENERY){int y=height(s.x(),s.z())+1;if(s.kind().equals("tree")){
            // Spruce grows as a cone, oak as a rounded crown; leaves are persistent so the woods never thin out mid-battle.
            boolean spruce=s.palette().equals("spruce");Block log=spruce?Blocks.SPRUCE_LOG:Blocks.OAK_LOG;
            BlockState leaf=(spruce?Blocks.SPRUCE_LEAVES:Blocks.OAK_LEAVES).getDefaultState().with(LeavesBlock.PERSISTENT,true);int r=s.width()/2,top=y+s.height();
            if(spruce)for(int dy=0;dy<s.height()-1;dy++){int ly=y+2+dy;int rr=Math.max(0,(int)Math.round(r*(1-dy/(double)(s.height()-1))+(dy%2==0?0:-.6)));
                for(int dx=-rr;dx<=rr;dx++)for(int dz=-rr;dz<=rr;dz++)if(dx*dx+dz*dz<=rr*rr+1&&!(dx==0&&dz==0))w.setBlockState(new BlockPos(s.x()+dx,ly,s.z()+dz),leaf,flags());}
            else for(int dx=-r;dx<=r;dx++)for(int dy=-2;dy<=2;dy++)for(int dz=-r;dz<=r;dz++){double e=dx*dx/(double)(r*r)+dy*dy/4.5+dz*dz/(double)(r*r);if(e<=1.05&&Math.floorMod(dx*31+dy*17+dz*7+s.x(),11)!=0)w.setBlockState(new BlockPos(s.x()+dx,top-2+dy,s.z()+dz),leaf,flags());}
            box(w,s.x(),y,s.z(),1,spruce?s.height():s.height()-1,1,log);if(spruce)w.setBlockState(new BlockPos(s.x(),top,s.z()),leaf,flags());
        }else for(int dx=0;dx<s.width();dx++)for(int dz=0;dz<s.width();dz++)for(int dy=0;dy<s.height();dy++)if(dx+dz+dy<s.width()+s.height()-1)put(w,s.x()+dx,y+dy,s.z()+dz,(dx+dz)%3==0?Blocks.MOSSY_COBBLESTONE:Blocks.ANDESITE);}
        if(CampaignMaps.selected==1)for(int row=0;row<4;row++)box(w,-109+row*4,65,-152,3,2,3,Blocks.HAY_BLOCK);
        if(!Boolean.getBoolean("gravesandglory.noDressing"))MapDressing.dress(w); // off only in local A/B harness runs
    }
    /** Gives every fence, wall and pane the build placed the connections its neighbours imply, the state a placement with shape updates would have had, written without notifying anything around it. */
    static void settleShapes(ServerWorld w){
        for(var pos:shaped){var s=w.getBlockState(pos);if(!connects(s.getBlock()))continue;var n=Block.postProcessState(s,w,pos);if(n!=s&&!n.isAir())w.setBlockState(pos,n,flags());}
    }
    /** The safety net after a build: the item entities loaded in the built area are discarded and counted in discarded. Items in chunks that are no longer loaded are out of reach, which is why the
     *  build itself writes without shape updates. */
    static void discardLoose(ServerWorld w){
        int hx=GravesAndGlory.R.worldHalfX(),hz=GravesAndGlory.R.worldHalfZ();
        var found=w.getEntitiesByType(EntityType.ITEM,new Box(-hx,w.getBottomY(),-hz,hx+1,w.getTopY(),hz+1),e->true);
        var kinds=new TreeMap<String,Integer>();
        for(var e:found){kinds.merge(Registries.ITEM.getId(e.getStack().getItem()).getPath(),e.getStack().getCount(),Integer::sum);e.discard();}
        discarded=found.size();
        if(!found.isEmpty())System.out.println("[Graves and Glory] The arena build left "+found.size()+" loose items, discarded: "+kinds);
    }
    /** A deck is its planks only (x..x+width-1 by z..z+depth-1 from the footprint corner): above each deck cell, up to twelve blocks, are cleared, whatever else was built across it. */
    public static void clearDecks(ServerWorld w){for(var s:Spec.STRUCTURES)if(s.kind().equals("bridge"))box(w,s.x()-s.width()/2,s.elevation()+1,s.z()-s.depth()/2,s.width(),12,s.depth(),Blocks.AIR);}
    static void structure(ServerWorld w,Spec.StructuresRow s){
        int x=s.x()-s.width()/2,z=s.z()-s.depth()/2,ground=s.elevation(),y=ground+1;
        Block stone=s.palette().equals("dusk")?Blocks.DEEPSLATE_BRICKS:Blocks.STONE_BRICKS,cloth=s.palette().equals("dusk")?Blocks.PURPLE_WOOL:Blocks.CYAN_WOOL;
        switch(s.kind()){
            case "mill" -> {
                box(w,x,ground,z,s.width(),1,s.depth(),Blocks.COBBLESTONE);
                for(int dy=0;dy<6;dy++)for(int dx=0;dx<s.width();dx++)for(int dz=0;dz<s.depth();dz++)if(dx==0||dz==0||dx==s.width()-1||dz==s.depth()-1)put(w,x+dx,y+dy,z+dz,dy<2?Blocks.STONE_BRICKS:Blocks.STRIPPED_OAK_LOG);
                box(w,x+3,y,z,3,3,1,Blocks.AIR);for(int a=0;a<5;a++)box(w,x-1+a,y+6+a,z-1,s.width()+2-a*2,1,s.depth()+2,Blocks.DARK_OAK_PLANKS);
                int wheelX=x+s.width(),wheelZ=z+s.depth()/2;for(int dy=-3;dy<=3;dy++)for(int dz=-3;dz<=3;dz++)if(Math.abs(dy*dy+dz*dz-9)<4||dy==0||dz==0)put(w,wheelX,y+2+dy,wheelZ+dz,Blocks.DARK_OAK_SLAB);
                put(w,x+2,y,z-1,Blocks.BARREL);put(w,x+6,y,z-1,Blocks.HAY_BLOCK);
            }
            case "farm" -> {for(int dx=0;dx<s.width();dx++)for(int dz=0;dz<s.depth();dz++){put(w,x+dx,ground,z+dz,dx%5==2?Blocks.WATER:Blocks.FARMLAND);if(dx%5!=2)w.setBlockState(new BlockPos(x+dx,y,z+dz),Blocks.WHEAT.getDefaultState().with(CropBlock.AGE,7),flags());}for(int dx=0;dx<s.width();dx+=3){put(w,x+dx,y,z-1,Blocks.OAK_FENCE);put(w,x+dx,y,z+s.depth(),Blocks.OAK_FENCE);}}
            case "breach" -> {for(int dz=0;dz<s.depth();dz++)if(Math.abs(dz-s.depth()/2)>(s.id().equals("breach")?5:3))box(w,x,y,z+dz,s.width(),Math.max(2,s.height()-Math.abs(dz-s.depth()/2)/2),1,Blocks.CRACKED_STONE_BRICKS);box(w,x+1,y,z,1,11,1,Blocks.DARK_OAK_FENCE);box(w,x+2,y+7,z,2,4,1,Blocks.CYAN_WOOL);}
            case "bridge" -> {if(s.palette().equals("ford")){for(int dx=0;dx<s.width();dx++)for(int dz=0;dz<s.depth();dz++){int px=x+dx,pz=z+dz;boolean center=Math.abs(pz-s.z())<4;boolean wet=!center&&Math.abs(px-riverX(pz))<6;put(w,px,ground-1,pz,Blocks.STONE);put(w,px,ground,pz,wet?Blocks.WATER:Math.abs(px-riverX(pz))>8?Blocks.COARSE_DIRT:(dx+dz)%4==0?Blocks.MOSSY_COBBLESTONE:Blocks.ANDESITE);}break;}if(s.palette().equals("wood")){box(w,x,ground,z,s.width(),1,s.depth(),Blocks.SPRUCE_PLANKS);for(int row:new int[]{z-1,z+s.depth()})for(int dx=0;dx<s.width();dx++){int px=x+dx;if(dx%4==0)box(w,px,ground-3,row,1,4,1,Blocks.SPRUCE_LOG);else w.setBlockState(new BlockPos(px,ground,row),Blocks.SPRUCE_SLAB.getDefaultState().with(SlabBlock.TYPE,SlabType.TOP),flags());if(wet(px,row))w.setBlockState(new BlockPos(px,y,row),Blocks.SPRUCE_FENCE.getDefaultState().with(FenceBlock.EAST,wet(px+1,row)).with(FenceBlock.WEST,wet(px-1,row)),flags());}break;}box(w,x,ground,z,s.width(),1,s.depth(),Blocks.STONE_BRICKS);for(int row:new int[]{z-1,z+s.depth()})for(int a=0;a<s.width();a++){int px=x+a;if(!wet(px,row))continue;put(w,px,ground,row,Blocks.STONE_BRICKS);w.setBlockState(new BlockPos(px,y,row),Blocks.STONE_BRICK_WALL.getDefaultState().with(WallBlock.EAST_SHAPE,wet(px+1,row)?WallShape.LOW:WallShape.NONE).with(WallBlock.WEST_SHAPE,wet(px-1,row)?WallShape.LOW:WallShape.NONE).with(WallBlock.UP,a%8==0||!wet(px-1,row)||!wet(px+1,row)),flags());if(a%8==0)put(w,px,y+1,row,Blocks.LANTERN);}for(int end:new int[]{x,x+s.width()-1})box(w,end,ground-4,z+1,1,4,s.depth()-2,stone);}
            case "base" -> {for(int dz:new int[]{0,s.depth()-5}){box(w,x,y,z+dz,5,s.height(),5,stone);box(w,x+5,y+2,z+dz+1,1,s.height()-4,3,cloth);for(int a=0;a<5;a+=2){put(w,x+a,y+s.height(),z+dz,stone);put(w,x+a,y+s.height(),z+dz+4,stone);}put(w,x+2,y+s.height(),z+dz+2,Blocks.SEA_LANTERN);}box(w,x,y+8,z+5,4,3,s.depth()-10,stone);box(w,x-3,ground,z-3,s.width()+6,1,s.depth()+6,Blocks.POLISHED_ANDESITE);}
            case "house", "barracks" -> {
                boolean barracks=s.kind().equals("barracks");Block wall=barracks?stone:Blocks.OAK_PLANKS;
                box(w,x,ground,z,s.width(),1,s.depth(),barracks?Blocks.POLISHED_ANDESITE:Blocks.COBBLESTONE);
                for(int h=0;h<s.height()-2;h++){box(w,x,y+h,z,s.width(),1,1,wall);box(w,x,y+h,z+s.depth()-1,s.width(),1,1,wall);box(w,x,y+h,z,1,1,s.depth(),wall);box(w,x+s.width()-1,y+h,z,1,1,s.depth(),wall);}
                for(int dz:new int[]{z,z+s.depth()-1})box(w,x+s.width()/2-2,y,dz,5,4,1,Blocks.AIR);
                if(barracks){for(int dx:new int[]{x,x+s.width()-1})box(w,dx,y,z+s.depth()/2-2,1,4,5,Blocks.AIR);box(w,x,y+s.height()-2,z,s.width(),1,s.depth(),Blocks.DARK_OAK_PLANKS);for(int a=0;a<s.width();a+=3){put(w,x+a,y+s.height()-1,z,stone);put(w,x+a,y+s.height()-1,z+s.depth()-1,stone);}put(w,s.x(),y,s.z(),Blocks.LECTERN);for(int a=y+5;a<=y+s.height()-3;a++)put(w,s.x(),a,s.z(),Blocks.CHAIN);w.setBlockState(new BlockPos(s.x(),y+4,s.z()),Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING,true),flags());for(int a:new int[]{-1,1}){box(w,s.x()+a*6,y,s.z()-4,1,4,1,Blocks.OAK_LOG);box(w,s.x()+a*6+1,y+2,s.z()-4,2,2,1,cloth);put(w,s.x()+a*6,y,s.z()+4,Blocks.SMITHING_TABLE);}}
                else{for(int a=0;a<5;a++)box(w,x-1+a,y+s.height()-2+a,z-1,s.width()+2-2*a,1,s.depth()+2,Blocks.DARK_OAK_PLANKS);for(int dx:new int[]{x+2,x+s.width()-4})box(w,dx,y+2,z,2,2,1,Blocks.GLASS_PANE);for(int dx:new int[]{x,x+s.width()-1})for(int dz:new int[]{z,z+s.depth()-1})box(w,dx,y,dz,1,s.height()-2,1,Blocks.STRIPPED_OAK_LOG);box(w,x+2,y,z+s.depth()-3,2,s.height()+2,2,Blocks.BRICKS);put(w,x+2,y+s.height()+2,z+s.depth()-3,Blocks.CAMPFIRE);for(int dz:new int[]{z,z+s.depth()-1}){box(w,x+2,y+2,dz,2,2,1,Blocks.GLASS_PANE);box(w,x+s.width()-4,y+2,dz,2,2,1,Blocks.GLASS_PANE);}}
            }
            case "ruin", "watchtower" -> {
                box(w,x,ground,z,s.width(),1,s.depth(),Blocks.MOSSY_STONE_BRICKS);for(int a=0;a<s.width();a++){int h=3+Math.floorMod(a*17,6);if(Math.abs(a-s.width()/2)>3){box(w,x+a,y,z,1,h,2,stone);box(w,x+a,y,z+s.depth()-2,1,h-1,2,stone);}}
                for(int a:new int[]{0,s.width()-4})for(int b:new int[]{0,s.depth()-4}){box(w,x+a,y,z+b,4,s.height(),4,stone);box(w,x+a+1,y+2,z+b+1,2,s.height()-2,2,Blocks.AIR);put(w,x+a+1,y+s.height(),z+b,Blocks.SOUL_LANTERN);for(int c=0;c<4;c+=2)put(w,x+a+c,y+s.height(),z+b,stone);}
                for(int a:new int[]{0,s.width()-2})for(int b=0;b<s.depth();b++)if(Math.abs(b-s.depth()/2)>4)box(w,x+a,y,z+b,2,4,1,stone);
                if(s.kind().equals("watchtower")){box(w,x,y+s.height()-1,z,s.width(),1,s.depth(),Blocks.OAK_PLANKS);box(w,x-2,y,z+s.depth()/2-2,4,4,5,Blocks.AIR);}
            }
            case "shrine" -> {box(w,x-2,ground,z-2,s.width()+4,1,s.depth()+4,Blocks.SMOOTH_QUARTZ);for(int a:new int[]{0,s.width()-2})for(int b:new int[]{0,s.depth()-2}){int high=b==0?s.height():a==0?3:5;box(w,x+a,y,z+b,2,high,2,Blocks.QUARTZ_PILLAR);if(b==0)put(w,x+a,y+high,z+b,Blocks.SEA_LANTERN);}box(w,x,y+s.height()-1,z,s.width(),1,2,Blocks.CHISELED_QUARTZ_BLOCK);box(w,x+2,y,z+s.depth()-3,5,1,2,Blocks.QUARTZ_PILLAR);box(w,s.x()-1,y,s.z(),3,2,2,Blocks.CHISELED_QUARTZ_BLOCK);put(w,s.x(),y+2,s.z(),Blocks.AMETHYST_BLOCK);put(w,x+s.width()+1,y,z+s.depth()/2,Blocks.MOSSY_STONE_BRICK_SLAB);}
            case "market" -> {for(int a=0;a<3;a++){int dx=x+a*6;box(w,dx,y,z,1,s.height()-1,1,Blocks.OAK_FENCE);box(w,dx+4,y,z,1,s.height()-1,1,Blocks.OAK_FENCE);box(w,dx,y+s.height()-1,z-1,5,1,5,a%2==0?cloth:Blocks.WHITE_WOOL);box(w,dx,y,z+2,5,1,1,Blocks.OAK_SLAB);put(w,dx+1,y,z+3,Blocks.BARREL);}}
        }
    }
    public static void flag(ServerWorld w,Spec.PointsRow p,int team){Block color=team<0?Blocks.WHITE_WOOL:team==0?Blocks.CYAN_WOOL:Blocks.PURPLE_WOOL;box(w,p.x(),p.elevation()+1,p.z(),1,6,1,Blocks.OAK_FENCE);box(w,p.x()+1,p.elevation()+4,p.z(),3,2,1,color);put(w,p.x(),p.elevation()+7,p.z(),Blocks.SEA_LANTERN);}
}
