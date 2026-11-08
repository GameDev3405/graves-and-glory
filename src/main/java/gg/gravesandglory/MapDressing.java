package gg.gravesandglory;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import java.util.*;

/** Battlefield dressing. Every lamp marks a road, every wall is cover at a fight,
 * every banner tells a squad where it musters. Nothing is placed on a road, a
 * muster lane, a bridge deck or a structure, so troop pathing is unchanged. */
final class MapDressing {
    final ServerWorld w;final int map=CampaignMaps.selected;
    final Block lamp,post,cover,coverTop,pave,trim,plank,log;
    final List<int[]> lamps=new ArrayList<>();
    /** Columns within one block of a prop's footprint (its solid and light cells and its anchor) in this world, read once from the live props (plan task S9). */
    final Set<Long> nearProps;
    MapDressing(ServerWorld w){
        this.w=w;
        nearProps=propColumns(w);
        lamp=map==3?Blocks.SOUL_LANTERN:Blocks.LANTERN;
        post=switch(map){case 1->Blocks.STONE_BRICK_WALL;case 2,3->Blocks.SPRUCE_FENCE;case 4->Blocks.OAK_FENCE;default->Blocks.DARK_OAK_FENCE;};
        cover=switch(map){case 1->Blocks.CRACKED_STONE_BRICKS;case 2->Blocks.PACKED_ICE;case 3->Blocks.MOSSY_COBBLESTONE;case 4->Blocks.STONE_BRICKS;default->Blocks.COBBLESTONE;};
        coverTop=switch(map){case 1->Blocks.STONE_BRICK_WALL;case 2->Blocks.SPRUCE_FENCE;case 3->Blocks.MOSSY_COBBLESTONE_WALL;case 4->Blocks.STONE_BRICK_WALL;default->Blocks.COBBLESTONE_WALL;};
        pave=switch(map){case 1->Blocks.ANDESITE;case 2->Blocks.PACKED_ICE;case 3->Blocks.MUD_BRICKS;default->Blocks.POLISHED_ANDESITE;};
        trim=switch(map){case 2->Blocks.POLISHED_DIORITE;case 3->Blocks.MOSSY_STONE_BRICKS;default->Blocks.STONE_BRICKS;};
        plank=map>=2&&map<=3?Blocks.SPRUCE_PLANKS:Blocks.OAK_PLANKS;log=map>=2&&map<=3?Blocks.SPRUCE_LOG:Blocks.OAK_LOG;
    }
    static void dress(ServerWorld w){var d=new MapDressing(w);d.camps();d.points();d.bridgeheads();d.redoubts();d.setPieces();d.roadLamps();d.worn();}

    // ---- placement rules -------------------------------------------------
    static int hash(int x,int z){return Math.floorMod(x*734287+z*912931+x*z*17,100003);}
    int h(int x,int z){return Arena.height(x,z);}
    void put(int x,int y,int z,Block b){Arena.put(w,x,y,z,b);}
    void set(int x,int y,int z,BlockState s){var pos=new BlockPos(x,y,z);w.setBlockState(pos,s,Arena.flags());Arena.note(pos,s.getBlock());}
    /** A lantern that hangs from the block above it (a slab, a plank, a chain): every lantern of the arena hangs from or stands on a block. */
    void hang(int x,int y,int z,Block lantern){set(x,y,z,lantern.getDefaultState().with(LanternBlock.HANGING,true));}
    /** A muster lane: the strip in front of each spawn where squads appear (GravesAndGlory spawn layout). */
    static boolean muster(int x,int z){for(int t=0;t<2;t++){var f=Spec.FACTIONS[t];int u=(x-f.spawnX())*(t==0?1:-1);if(u>=-2&&u<=24&&Math.abs(z-f.spawnZ())<=40)return true;}return false;}
    static boolean onStructure(int x,int z,int margin){for(var s:Spec.STRUCTURES)if(Math.abs(x-s.x())<=s.width()/2+margin&&Math.abs(z-s.z())<=s.depth()/2+margin)return true;return false;}
    /** On what a structure builds: the x0..x0+width-1 by z0..z0+depth-1 of its floor course or deck planks (onStructure with no margin also counts the row and the column beyond them, which are ground). */
    static boolean built(int x,int z){return nearBuilt(x,z,0);}
    /** Within margin blocks of what a structure builds (the floor course of a building, the planks of a deck). A ford builds only where it stands in the river (8 columns of its line): its approaches are the ground and the road. */
    static boolean nearBuilt(int x,int z,int margin){
        for(var s:Spec.STRUCTURES){int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2;
            if(x<x0-margin||x>=x0+s.width()+margin||z<z0-margin||z>=z0+s.depth()+margin)continue;
            if(s.palette().equals("ford")&&Math.abs(x-Arena.riverX(z))>8+margin)continue;
            return true;}
        return false;
    }
    /** Within two columns of the water of the river (its half-width is 6, and 9 on Hollowwood, the wide river): nothing is set down there. The test is against the real river, so the outermost column of Hollowwood's does not pass. */
    static boolean nearRiver(int map,int x,int z){return Math.abs(x-Arena.riverX(z))<(map==3?11:8);}
    boolean nearRiver(int x,int z){return nearRiver(map,x,z);}
    static boolean nearTree(int x,int z){for(var s:Spec.SCENERY)if(Math.abs(x-s.x())<=s.width()/2+1&&Math.abs(z-s.z())<=s.width()/2+1)return true;return false;}
    static Set<Long> propColumns(ServerWorld w){
        Set<Long> out=new HashSet<>();
        for(var l:Props.live(w)){
            List<BlockPos> cells=new ArrayList<>(Props.solidCells(l.def(),l.anchor(),l.facing()));cells.add(l.anchor());
            for(var li:l.def().lights())cells.add(Props.lightCell(l.def(),l.anchor(),l.facing(),li));
            for(var c:cells)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)out.add(ChunkPos.toLong(c.getX()+dx,c.getZ()+dz));
        }
        return out;
    }
    /** Open ground a prop may stand on: off props (one block round their footprint), off roads (with a shoulder), river, bridges, structures, muster lanes and trees, and level. */
    boolean free(int x,int z,double shoulder){
        if(nearProps.contains(ChunkPos.toLong(x,z)))return false; // on or next to a prop
        if(Math.abs(x)>GravesAndGlory.R.arenaHalfX()-4||Math.abs(z)>GravesAndGlory.R.arenaHalfZ()-4)return false;
        if(CampaignMaps.roadDistance(x,z)<shoulder||Math.abs(x-Arena.riverX(z))<(map==3?12:9)||onStructure(x,z,1)||muster(x,z)||nearTree(x,z))return false;
        int y=h(x,z);return Math.abs(h(x+1,z)-y)<=1&&Math.abs(h(x-1,z)-y)<=1&&Math.abs(h(x,z+1)-y)<=1&&Math.abs(h(x,z-1)-y)<=1;
    }
    boolean free(int x,int z){return free(x,z,1.5);}
    boolean inPoint(int x,int z,int margin){for(var p:Spec.POINTS)if(Math.hypot(x-p.x(),z-p.z())<=p.radius()+margin)return true;return false;}
    boolean area(int x0,int z0,int x1,int z1){for(int x=Math.min(x0,x1);x<=Math.max(x0,x1);x++)for(int z=Math.min(z0,z1);z<=Math.max(z0,z1);z++)if(!free(x,z)||inPoint(x,z,2))return false;return true;}
    boolean lampNear(int x,int z,int d){for(var l:lamps)if(Math.abs(l[0]-x)<d&&Math.abs(l[1]-z)<d)return true;return false;}
    /** Lamp post: three posts and a lantern, about head height above a marching column. */
    void lampPost(int x,int z){int y=h(x,z);for(int a=1;a<=3;a++)put(x,y+a,z,post);put(x,y+4,z,lamp);lamps.add(new int[]{x,z});}
    /** A war banner: a pole with a lantern finial and a flag three wide and four deep, flying toward dir along x
     * and a matching drape on the pole's z faces, so it reads from the front and the side. */
    void banner(int x,int z,int height,Block cloth,int dir){int y=h(x,z);for(int a=1;a<=height;a++)put(x,y+a,z,Blocks.DARK_OAK_FENCE);put(x,y+height+1,z,lamp);
        for(int a=1;a<=3;a++)for(int b=0;b<4-(a==3?1:0);b++)put(x+dir*a,y+height-1-b,z,cloth);
        for(int b=1;b<=3;b++){put(x,y+height-b,z-1,cloth);put(x,y+height-b,z+1,cloth);}}
    static final int NO_ROOM=Integer.MIN_VALUE;
    /** The first z from z0 stepping by dir where the pole, its drapes and its flag (three either way along x) are all off road and off buildings;
     * NO_ROOM when nothing within 40 is clear, so the caller leaves that banner out rather than plant it on a road. */
    static int clearZ(int x,int z0,int dir){for(int z=z0,k=0;k<40;z+=dir,k++){boolean ok=true;for(int dx=-3;dx<=3;dx++)for(int d=-1;d<=1;d++)ok&=CampaignMaps.roadDistance(x+dx,z+d)>=1.5&&!onStructure(x+dx,z+d,0);if(ok)return z;}return NO_ROOM;}
    static Block cloth(int team){return team==0?Blocks.CYAN_WOOL:team==1?Blocks.PURPLE_WOOL:Blocks.WHITE_WOOL;}
    static Block carpet(int team){return team==0?Blocks.CYAN_CARPET:Blocks.PURPLE_CARPET;}

    // ---- war camps: where every army starts and returns -------------------
    void camps(){
        for(int t=0;t<2;t++){
            var f=Spec.FACTIONS[t];int fwd=t==0?1:-1,bx=f.spawnX(),bz=f.spawnZ();Block team=cloth(t),dark=t==0?Blocks.POLISHED_ANDESITE:Blocks.POLISHED_BLACKSTONE;
            // Command dais: flush stone where the commander arrives, a brazier ring behind it.
            // Parade ground: inside the palisade every road is laid in stone, edged in trim, so the camp reads as one place.
            for(int u=-19;u<=25;u++)for(int v=-63;v<=63;v++){int x=bx+u*fwd,z=bz+v;double rd=CampaignMaps.roadDistance(x,z);if(rd>=0.5||built(x,z))continue;
                put(x,h(x,z),z,rd>-1?trim:hash(x,z)%7==0?Blocks.CRACKED_STONE_BRICKS:pave);}
            Block inlay=t==0?Blocks.CYAN_GLAZED_TERRACOTTA:Blocks.PURPLE_GLAZED_TERRACOTTA;
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){int x=bx+dx,z=bz+dz,m=Math.max(Math.abs(dx),Math.abs(dz));
                put(x,h(x,z),z,m==4?trim:dx==0&&dz==0?Blocks.CHISELED_STONE_BRICKS:m==1||dx==0||dz==0?inlay:dark);}
            // Command standards: the two tallest banners on the field stand behind the dais.
            // Both stand off the parade road, stepped outward until clear, so the march out stays open.
            for(int dz:new int[]{-6,6}){int z=clearZ(bx-fwd*6,bz+dz,Integer.signum(dz));if(z==NO_ROOM)continue;banner(bx-fwd*6,z,9,team,fwd);
                int fz=clearZ(bx-fwd*6,z+Integer.signum(dz)*2,Integer.signum(dz)),x=bx-fwd*6;if(fz!=NO_ROOM)for(int a=1;a<=2;a++)put(x,h(x,fz)+a,fz,a==1?trim:map==3?Blocks.SOUL_CAMPFIRE:Blocks.CAMPFIRE);}
            // The muster ground is the grass between the paved roads and nothing is scattered on it (a squad finds its lane by the standards below); trampled earth is laid by worn(), as one pattern, last.
            // Lane standards behind each squad lane, so a commander can read the line from the dais.
            for(int i=0;i<4;i++){int z=bz+(int)((i-1.5)*22);z=clearZ(bx-fwd*3,z,z>bz?1:-1);if(z!=NO_ROOM)banner(bx-fwd*3,z,6+i%2,team,fwd);}
            // Palisade: side walls and a back wall to the gatehouse; the front stays open to march out.
            for(int u=-20;u<=26;u++)for(int side:new int[]{-1,1})palisade(bx+u*fwd,bz+side*64);
            for(int v=36;v<=64;v++)for(int side:new int[]{-1,1})palisade(bx-20*fwd,bz+side*v);
            for(int v=46;v<=64;v++)for(int side:new int[]{-1,1})palisade(bx+26*fwd,bz+side*v);
            for(int side:new int[]{-1,1})for(int u:new int[]{-20,26}){int x=bx+u*fwd,z=bz+side*64;if(free(x,z,1)){int y=h(x,z);for(int a=1;a<=5;a++)put(x,y+a,z,log);put(x,y+6,z,lamp);}}
            // Tents along the walls; the forge beside the barracks; supplies beside the stores.
            int barracksSide=Integer.signum(f.barracksZ()-bz);
            for(int u=-14;u<=18;u+=8)for(int side:new int[]{-1,1}){if(side==barracksSide&&u==-14){forge(bx+u*fwd,bz+side*54,fwd);continue;}tent(bx+u*fwd,bz+side*55,t,(u/8+side)%2==0);}
            for(int k=0;k<5;k++){int x=bx+(-12+k*3)*fwd,z=bz-barracksSide*28;if(free(x,z)&&!inPoint(x,z,2)){int y=h(x,z);put(x,y+1,z,k%2==0?Blocks.BARREL:Blocks.HAY_BLOCK);if(k%2==0)put(x,y+2,z,Blocks.HAY_BLOCK);}}
        }
    }
    void palisade(int x,int z){if(!free(x,z,2))return;int y=h(x,z);for(int a=1;a<=3;a++)put(x,y+a,z,log);if(hash(x,z)%2==0)put(x,y+4,z,Blocks.SPRUCE_FENCE);}
    /** An A-frame tent with a bedroll and a supply barrel. */
    void tent(int cx,int cz,int team,boolean white){
        if(!area(cx-2,cz-2,cx+2,cz+2))return;int y=h(cx,cz);Block c=white?Blocks.WHITE_WOOL:cloth(team);
        for(int dx=-2;dx<=2;dx++){put(cx+dx,y+1,cz-2,c);put(cx+dx,y+1,cz+2,c);put(cx+dx,y+2,cz-1,c);put(cx+dx,y+2,cz+1,c);put(cx+dx,y+3,cz,c);}
        for(int dz=-1;dz<=1;dz++)put(cx-2,y+1,cz+dz,c);put(cx+2,y+1,cz,Blocks.AIR);
        put(cx,y+1,cz-1,carpet(team));put(cx-1,y+1,cz+1,Blocks.BARREL);put(cx+1,y+4,cz,Blocks.DARK_OAK_FENCE);
    }
    /** Field forge: anvil, blast furnace, quenching cauldron and grindstone under a plank roof. */
    void forge(int cx,int cz,int fwd){
        if(!area(cx-3,cz-3,cx+3,cz+3))return;int y=h(cx,cz);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)put(cx+dx,y,cz+dz,Blocks.COBBLESTONE);
        for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3})for(int a=1;a<=3;a++)put(cx+dx,y+a,cz+dz,Blocks.DARK_OAK_FENCE);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)put(cx+dx,y+4,cz+dz,Blocks.DARK_OAK_SLAB);
        put(cx,y+1,cz,Blocks.ANVIL);put(cx+2*fwd,y+1,cz-2,Blocks.BLAST_FURNACE);put(cx+2*fwd,y+2,cz-2,Blocks.CAMPFIRE);put(cx-2*fwd,y+1,cz-2,Blocks.LAVA_CAULDRON);
        put(cx-2*fwd,y+1,cz+2,Blocks.GRINDSTONE);put(cx+2*fwd,y+1,cz+2,Blocks.SMITHING_TABLE);hang(cx,y+3,cz,lamp);
    }

    // ---- objectives: the places armies hold ------------------------------
    void points(){
        for(var p:Spec.POINTS){
            int r=p.radius();
            // Plaza floor, one pattern on every plaza: within 9.2 blocks of the pole trampled earth, a compass of trim three wide along the axes, a ring of paving from 8 blocks out (the 3x3 pad under the pole is Arena's).
            // Nothing is scattered, and no road surface is left among the paving: a dirt path block is lower than its neighbours and reads as a pit.
            for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++){int x=p.x()+dx,z=p.z()+dz;double d=Math.hypot(dx,dz);
                if(d>=9.2||Math.abs(dx)<=1&&Math.abs(dz)<=1||nearRiver(x,z)||built(x,z))continue;
                put(x,h(x,z),z,d>=8?pave:(Math.abs(dx)<=1||Math.abs(dz)<=1)?trim:Blocks.COARSE_DIRT);}
            // Capture edge: a flush band of dark stone exactly on the radius, so the line you must cross is on the ground.
            Block edge=map==2?Blocks.BLUE_ICE:map==3?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.POLISHED_BLACKSTONE;
            if(Arena.ringLaid(p))for(int dx=-r-1;dx<=r+1;dx++)for(int dz=-r-1;dz<=r+1;dz++){double d=Math.hypot(dx,dz);int x=p.x()+dx,z=p.z()+dz;
                if(d<r-.5||d>=r+.5||nearRiver(x,z)||onStructure(x,z,0))continue;put(x,h(x,z),z,edge);}
            // Boundary posts: lanterns just outside the ring (past the worn strip that lies one block outside its edge stone) show the capture edge at night.
            for(int k=0;k<20;k++){double a=k*Math.PI/10;int x=p.x()+(int)Math.round(Math.cos(a)*(r+3.0)),z=p.z()+(int)Math.round(Math.sin(a)*(r+3.0));
                if(CampaignMaps.roadDistance(x,z)<1||nearRiver(x,z)||onStructure(x,z,0)||muster(x,z))continue;int y=h(x,z);put(x,y+1,z,coverTop);put(x,y+2,z,lamp);}
            // Cover: six breast-high walls between the roads, so defenders have something to hold.
            for(int k=0;k<6;k++){double a=Math.PI/6+k*Math.PI/3,tx=-Math.sin(a),tz=Math.cos(a);int cx=p.x()+(int)Math.round(Math.cos(a)*11),cz=p.z()+(int)Math.round(Math.sin(a)*11);
                List<int[]> cells=new ArrayList<>();boolean ok=true;
                for(int s=-2;s<=2;s++){int x=cx+(int)Math.round(tx*s),z=cz+(int)Math.round(tz*s);if(CampaignMaps.roadDistance(x,z)<1.5||nearRiver(x,z)||onStructure(x,z,1)||Math.abs(h(x,z)-p.elevation())>1)ok=false;cells.add(new int[]{x,z});}
                if(!ok)continue;for(int[] c:cells){int y=h(c[0],c[1]);put(c[0],y+1,c[1],cover);put(c[0],y+2,c[1],coverTop);}
                int bx=cx-(int)Math.round(Math.cos(a)*2),bz=cz-(int)Math.round(Math.sin(a)*2);
                if(CampaignMaps.roadDistance(bx,bz)>=1.5&&!nearRiver(bx,bz)&&!onStructure(bx,bz,1))put(bx,h(bx,bz)+1,bz,k%2==0?Blocks.BARREL:Blocks.HAY_BLOCK);}
            // Braziers on the four diagonals, raised so no one walks into the fire.
            for(int k=0;k<4;k++){double a=Math.PI/4+k*Math.PI/2;int x=p.x()+(int)Math.round(Math.cos(a)*15),z=p.z()+(int)Math.round(Math.sin(a)*15);
                if(CampaignMaps.roadDistance(x,z)<1.5||nearRiver(x,z)||onStructure(x,z,1))continue;int y=h(x,z);put(x,y+1,z,trim);put(x,y+2,z,trim);put(x,y+3,z,map==3?Blocks.SOUL_CAMPFIRE:Blocks.CAMPFIRE);}
            // Approach cover: past the ring, clumps of crates and a short wall where attackers stage before the push.
            for(int k=0;k<8;k++){double a=Math.PI/8+k*Math.PI/4;int cx=p.x()+(int)Math.round(Math.cos(a)*(r+9)),cz=p.z()+(int)Math.round(Math.sin(a)*(r+9));
                if(!area(cx-1,cz-1,cx+1,cz+1))continue;int y=h(cx,cz);double tx=-Math.sin(a),tz=Math.cos(a);
                for(int s2=-1;s2<=1;s2++){int x=cx+(int)Math.round(tx*s2),z=cz+(int)Math.round(tz*s2);put(x,h(x,z)+1,z,cover);if(s2!=0)put(x,h(x,z)+2,z,coverTop);}
                int ox=cx-(int)Math.round(Math.cos(a)*2),oz=cz-(int)Math.round(Math.sin(a)*2);if(free(ox,oz,2)&&!inPoint(ox,oz,2)){put(ox,h(ox,oz)+1,oz,k%2==0?Blocks.BARREL:Blocks.HAY_BLOCK);if(k%3==0)put(ox,h(ox,oz)+2,oz,Blocks.BARREL);}}
            // Landmark obelisks on the two road-free diagonals farthest out: seen from the camps, they say "the point is here".
            int built=0;for(int k=0;k<8&&built<2;k++){double a=Math.PI/8+k*Math.PI/4;int x=p.x()+(int)Math.round(Math.cos(a)*(r+5)),z=p.z()+(int)Math.round(Math.sin(a)*(r+5));
                if(!area(x-1,z-1,x+1,z+1))continue;int y=h(x,z);built++;
                for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)put(x+dx,y+1,z+dz,trim);
                for(int a2=2;a2<=8;a2++)put(x,y+a2,z,a2==5?Blocks.CHISELED_STONE_BRICKS:trim);put(x,y+9,z,map==3?Blocks.SOUL_CAMPFIRE:Blocks.CAMPFIRE);
                for(int b=6;b<=8;b++)for(int dz:new int[]{-1,1})put(x,y+b,z+dz,Blocks.WHITE_WOOL);}
            if(p.elevation()>64)outpost(p);
        }
    }
    /** Kingsfield's raised outposts: a log stockade with four wide gates and corner lookouts. */
    void outpost(Spec.PointsRow p){
        int r=p.radius()+4;
        for(int k=0;k<360;k++){double a=Math.toRadians(k);int x=p.x()+(int)Math.round(Math.cos(a)*r),z=p.z()+(int)Math.round(Math.sin(a)*r);
            if(k%90<14||k%90>76||CampaignMaps.roadDistance(x,z)<3)continue;int y=h(x,z);for(int b=1;b<=3;b++)put(x,y+b,z,Blocks.SPRUCE_LOG);put(x,y+4,z,Blocks.SPRUCE_FENCE);}
        for(int k=0;k<4;k++){double a=Math.PI/4+k*Math.PI/2;int x=p.x()+(int)Math.round(Math.cos(a)*(r+2)),z=p.z()+(int)Math.round(Math.sin(a)*(r+2));if(!free(x,z,2))continue;int y=h(x,z);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){if(dx!=0&&dz!=0)for(int b=1;b<=5;b++)put(x+dx,y+b,z+dz,Blocks.SPRUCE_LOG);put(x+dx,y+6,z+dz,Blocks.SPRUCE_PLANKS);}
            put(x,y+7,z,Blocks.LANTERN);for(int dx=-1;dx<=1;dx++)for(int b=3;b<=5;b++){put(x+dx,y+b,z-2,Blocks.WHITE_WOOL);put(x+dx,y+b,z+2,Blocks.WHITE_WOOL);}}
    }

    // ---- bridgeheads and rally redoubts ----------------------------------
    void bridgeheads(){
        for(var s:Spec.STRUCTURES){if(!s.kind().equals("bridge"))continue;boolean ford=s.palette().equals("ford");
            for(int end:new int[]{-1,1})for(int side:new int[]{-1,1}){int x=s.x()+end*(s.width()/2+1),z=s.z()+side*(s.depth()/2+1);for(int k=0;k<4&&CampaignMaps.roadDistance(x,z)<0.5;k++)z+=side;if(Math.abs(x-Arena.riverX(z))<7||CampaignMaps.roadDistance(x,z)<0.5)continue;int y=h(x,z);
                if(ford){for(int a=1;a<=2;a++)put(x,y+a,z,Blocks.MOSSY_COBBLESTONE_WALL);put(x,y+3,z,lamp);continue;}
                for(int a=1;a<=4;a++)put(x,y+a,z,trim);put(x,y+5,z,Blocks.CHISELED_STONE_BRICKS);put(x,y+6,z,lamp);put(x,y+4,z-side,trim);hang(x,y+3,z-side,Blocks.LANTERN);}}
    }
    /** Where each army's flank road meets its lateral road: a crescent of earthworks facing the enemy, with a rally banner. */
    void redoubts(){
        List<int[]> junctions=new ArrayList<>();int[] zs=Arrays.stream(Spec.POINTS).filter(p->p.elevation()==64).mapToInt(Spec.PointsRow::z).toArray();
        for(int side:new int[]{-1,1}){for(int z:zs)junctions.add(new int[]{side*205,z,side});if(map==4)junctions.add(new int[]{side*205,-side*100,side});}
        for(var j:junctions){int jx=j[0],jz=j[1],enemy=-j[2],team=j[2]<0?0:1;
            for(int a=-70;a<=70;a+=4){double r=Math.toRadians(a);for(int rad:new int[]{11,12}){int x=jx+(int)Math.round(Math.cos(r)*rad*enemy),z=jz+(int)Math.round(Math.sin(r)*rad);
                if(!free(x,z,2)||inPoint(x,z,3))continue;int y=h(x,z);put(x,y+1,z,rad==12?Blocks.COARSE_DIRT:cover);if(rad==11)put(x,y+2,z,coverTop);}}
            int bx=jx-8*enemy,bz=jz+(jz>0?-9:9);if(free(bx,bz,2))banner(bx,bz,5,cloth(team),enemy);
            for(int k=0;k<3;k++){int x=jx-10*enemy+k,z=jz+(jz>0?9:-9);if(free(x,z,2)){int y=h(x,z);put(x,y+1,z,k==1?Blocks.HAY_BLOCK:Blocks.BARREL);}}
            int fx=jx-11*enemy,fz=jz+(jz>0?-4:4);if(free(fx,fz,2)){int y=h(fx,fz);put(fx,y+1,fz,Blocks.COBBLESTONE);put(fx,y+2,fz,Blocks.COBBLESTONE);put(fx,y+3,fz,map==3?Blocks.SOUL_CAMPFIRE:Blocks.CAMPFIRE);}
        }
    }

    // ---- roads: lamps every sixteen blocks, alternating sides -------------
    void roadLamps(){
        for(var r:CampaignMaps.roads){double dx=r.bx()-r.ax(),dz=r.bz()-r.az(),len=Math.hypot(dx,dz);if(len<12)continue;double nx=-dz/len,nz=dx/len;
            for(double s=8,k=0;s<len-4;s+=16,k++){double side=k%2==0?1:-1,off=r.width()+2;int x=(int)Math.round(r.ax()+dx*s/len+nx*off*side),z=(int)Math.round(r.az()+dz*s/len+nz*off*side);
                if(lampNear(x,z,9)||!free(x,z,1)||inPoint(x,z,2))continue;lampPost(x,z);}}
    }

    // ---- trampled earth: one pattern, laid last ---------------------------
    /** Dirt path cells the guard of worn() turned back to ground in the last build: a cell with no dirt path on any of its four sides draws as a pit between full blocks, the passes are meant to leave none (the arena checks read this). */
    static volatile int despeckled;
    /** The cells worn() laid in the last build: {x, z}. */
    final List<int[]> laid=new ArrayList<>();
    /** The parade ground of a camp, which the camp paves: from 19 blocks behind the dais to 25 in front of it, 63 either side. */
    static boolean parade(int x,int z){for(int t=0;t<2;t++){var f=Spec.FACTIONS[t];int u=(x-f.spawnX())*(t==0?1:-1);if(u>=-19&&u<=25&&Math.abs(z-f.spawnZ())<=63)return true;}return false;}
    /** Ground a worn strip may be laid on: the plain ground of the map with air or a plant above it (nothing solid stands on it), off the water, off the floors of buildings and the planks of decks and one block of margin round them (nothing is trampled against a wall), off the parade grounds. */
    boolean wearable(int x,int z){
        if(nearRiver(x,z)||nearBuilt(x,z,1)||parade(x,z))return false;
        int y=h(x,z);
        if(!w.getBlockState(new BlockPos(x,y,z)).isOf(Arena.natural(y)))return false;
        var up=w.getBlockState(new BlockPos(x,y+1,z));
        return up.isAir()||up.getBlock() instanceof PlantBlock||up.isOf(Blocks.SNOW);
    }
    void lay(int x,int z){int y=h(x,z);if(!w.getBlockState(new BlockPos(x,y+1,z)).isAir())put(x,y+1,z,Blocks.AIR);put(x,y,z,Blocks.DIRT_PATH);laid.add(new int[]{x,z});}
    boolean path(int x,int z){return w.getBlockState(new BlockPos(x,h(x,z),z)).isOf(Blocks.DIRT_PATH);}
    /** A cell the worn band follows: not a road cell itself, and touching on a side a road cell that is still dirt path (where the road is paved, planked or built over, the band has nothing to follow). */
    boolean follows(int x,int z){
        if(CampaignMaps.road(x,z))return false;
        return CampaignMaps.road(x-1,z)&&path(x-1,z)||CampaignMaps.road(x+1,z)&&path(x+1,z)||CampaignMaps.road(x,z-1)&&path(x,z-1)||CampaignMaps.road(x,z+1)&&path(x,z+1);
    }
    /** A cell of the worn ring of a point whose edge stone ends at radius in: outside it and touching it, eight ways (a ring that touched it four ways only would be cut into single cells on a diagonal). */
    static boolean ringCell(int dx,int dz,double in){
        if(Math.hypot(dx,dz)<in)return false;
        for(int ex=-1;ex<=1;ex++)for(int ez=-1;ez<=1;ez++)if(Math.hypot(dx+ex,dz+ez)<in)return true;
        return false;
    }
    /** The cell of the stone ring (radius out from the point) that lies on the same radius as the cell (dx, dz) of its worn ring. */
    static int[] bandOf(Spec.PointsRow p,int dx,int dz){double d=Math.hypot(dx,dz);return new int[]{p.x()+(int)Math.round(dx*p.radius()/d),p.z()+(int)Math.round(dz*p.radius()/d)};}
    /** Trampled earth is one pattern and nothing else: a worn band one block wide along every road edge (the cells that touch a road cell on a side) and a ring one block wide just outside the edge stone of every point whose stone ring is laid, both of dirt path
     *  on plain ground. Both are continuous: a cell of the band touches the road it follows, the ring touches the edge stone eight ways and so every cell of it touches a dirt path cell on a side, and a last guard puts back as ground any dirt path cell that is
     *  still alone. Nothing is scattered and nothing lies against a wall, on the water, on a deck or on a parade ground; a cell that holds anything but plain ground (paving, the stone ring, a prop's foot) is left as it is. */
    void worn(){
        laid.clear();despeckled=0;
        int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ(),wz=2*hz+1;
        var road=new BitSet((2*hx+1)*wz);
        for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++)if(CampaignMaps.road(x,z))road.set((x+hx)*wz+(z+hz));
        for(int x=-hx+1;x<hx;x++)for(int z=-hz+1;z<hz;z++){
            if(road.get((x+hx)*wz+(z+hz)))continue;
            if(!(road.get((x-1+hx)*wz+(z+hz))||road.get((x+1+hx)*wz+(z+hz))||road.get((x+hx)*wz+(z-1+hz))||road.get((x+hx)*wz+(z+1+hz))))continue;
            if(follows(x,z)&&wearable(x,z))lay(x,z);
        }
        for(var p:Spec.POINTS){
            if(!Arena.ringLaid(p))continue;
            int r=p.radius();double in=r+.5;
            for(int dx=-r-4;dx<=r+4;dx++)for(int dz=-r-4;dz<=r+4;dz++){
                if(!ringCell(dx,dz,in))continue;
                var b=bandOf(p,dx,dz);
                if(Arena.wetBank(b[0],b[1])||Arena.deckCell(b[0],b[1]))continue; // the stone ring is cut here
                if(wearable(p.x()+dx,p.z()+dz))lay(p.x()+dx,p.z()+dz);
            }
        }
        var cand=new ArrayList<int[]>(laid);
        for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++)if(road.get((x+hx)*wz+(z+hz)))cand.add(new int[]{x,z});
        var put=new StringBuilder();
        for(var c:cand)if(path(c[0],c[1])&&!path(c[0]-1,c[1])&&!path(c[0]+1,c[1])&&!path(c[0],c[1]-1)&&!path(c[0],c[1]+1)){int y=h(c[0],c[1]);put(c[0],y,c[1],Arena.natural(y));if(++despeckled<=12)put.append(" (").append(c[0]).append(", ").append(c[1]).append(')');}
        System.out.println("[Graves and Glory] Worn earth: "+laid.size()+" cells laid (band and rings), "+despeckled+" lone cells put back as ground"+(despeckled==0?"":":"+put));
    }

    // ---- set pieces: what each battlefield is about -----------------------
    void setPieces(){
        switch(map){
            case 0->{ // Rivergate: a working river valley. Fishing piers, a well for the hamlet, haystacks by the farm.
                for(int z:new int[]{-215,-72,72,215})for(int bank:new int[]{-1,1})pier((int)Math.round(Arena.riverX(z))+bank*7,z,-bank);
                var mill=Spec.POINTS[0];well(mill.x()-30,mill.z()+2);
                for(int k=0;k<4;k++){int x=mill.x()-62-k*4,z=mill.z()-40;if(free(x,z)){int y=h(x,z);put(x,y+1,z,Blocks.HAY_BLOCK);put(x,y+2,z,Blocks.HAY_BLOCK);}}
            }
            case 1->{ // Ironhold: the siege. Rubble below the walls, stakes facing the gate, a trebuchet per side.
                for(int k=0;k<70;k++){int x=-60+hash(k,7)%120,z=-70+hash(7,k)%140;if(!free(x,z,2)||inPoint(x,z,2))continue;int y=h(x,z);put(x,y+1,z,k%3==0?Blocks.COBBLESTONE:Blocks.CRACKED_STONE_BRICKS);if(k%4==0&&free(x+1,z,2))put(x+1,y+1,z,Blocks.STONE_BRICK_SLAB);}
                for(int side:new int[]{-1,1}){trebuchet(side*112,side*40,-side);for(int z=-48;z<=48;z+=3)if(Math.abs(z)>10){int x=side*90;if(free(x,z,2)){int y=h(x,z);put(x,y+1,z,Blocks.DARK_OAK_FENCE);put(x,y+2,z,Blocks.DARK_OAK_FENCE);}}}
            }
            case 2->{ // Frostmarch: the signal fire, stranded caravans, windbreaks on the ridges.
                for(var s:Spec.STRUCTURES)if(s.id().equals("signal_tower")){int y=s.elevation()+s.height();put(s.x(),y,s.z(),Blocks.NETHERRACK);put(s.x(),y+1,s.z(),Blocks.FIRE);for(int a=-1;a<=1;a+=2){put(s.x()+a,y,s.z(),Blocks.COBBLESTONE_WALL);put(s.x(),y,s.z()+a,Blocks.COBBLESTONE_WALL);}}
                wagon(-150,-120);wagon(150,120);wagon(-60,60);wagon(60,-60);
                for(int side:new int[]{-1,1})for(int k=0;k<14;k++){int x=side*(120+k*2),z=-side*(70-k);if(free(x,z,2)&&!inPoint(x,z,2)){int y=h(x,z);put(x,y+1,z,Blocks.SPRUCE_LOG);put(x,y+2,z,Blocks.SPRUCE_FENCE);put(x,y+3,z,Blocks.SNOW);}}
            }
            case 3->{ // Hollowwood: a logging forest. Log stacks by the camp, boardwalks through the bog, mushrooms under the pines.
                for(int k=0;k<6;k++)logStack(-120+k*48,k%2==0?-60:60);
                for(var r:CampaignMaps.roads){double dx=r.bx()-r.ax(),dz=r.bz()-r.az(),len=Math.hypot(dx,dz);for(double s=0;s<=len;s++){int x=(int)Math.round(r.ax()+dx*s/len),z=(int)Math.round(r.az()+dz*s/len);
                    if(Math.abs(x-Arena.riverX(z))<40&&Math.abs(x-Arena.riverX(z))>=10&&!onStructure(x,z,0))for(int o=-1;o<=1;o++){int px=x+(int)Math.round(-dz/len*o),pz=z+(int)Math.round(dx/len*o);if(!inPoint(px,pz,1))put(px,h(px,pz),pz,Blocks.SPRUCE_PLANKS);}}}
                for(var s:Spec.SCENERY)if(hash(s.x(),s.z())%3==0){int x=s.x()+3,z=s.z()+1;if(free(x,z,2))put(x,h(x,z)+1,z,hash(x,z)%2==0?Blocks.RED_MUSHROOM:Blocks.BROWN_MUSHROOM);}
                for(var p:Spec.POINTS)if(p.id().equals("C"))for(int k=0;k<8;k++){double a=k*Math.PI/4;int x=p.x()+(int)Math.round(Math.cos(a)*6),z=p.z()+(int)Math.round(Math.sin(a)*6);if(CampaignMaps.roadDistance(x,z)>=1&&!nearRiver(x,z))put(x,h(x,z)+1,z,Blocks.AMETHYST_CLUSTER);}
            }
            case 4->{ // Kingsfield: farmland crossroads. Hedgerows along the fields, a windmill above the north ford.
                for(int side:new int[]{-1,1})for(int x=60;x<=190;x++){int wx=side*x;for(int z:new int[]{-side*40,side*170}){if(!free(wx,z,2)||inPoint(wx,z,3)||x%22<4)continue;int y=h(wx,z);set(wx,y+1,z,Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT,true));set(wx,y+2,z,Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT,true));}}
                windmill(-62,-200);windmill(62,200);
            }
            default->{}
        }
    }
    void pier(int x,int z,int dir){int y=Arena.Y-1;for(int a=0;a<6;a++)for(int b=-1;b<=1;b++){int px=x+a*dir,pz=z+b;if(onStructure(px,pz,2))return;put(px,y,pz,Blocks.SPRUCE_PLANKS);}
        for(int a:new int[]{1,5})for(int b:new int[]{-1,1}){put(x+a*dir,y-1,z+b,Blocks.SPRUCE_LOG);put(x+a*dir,y+1,z+b,Blocks.SPRUCE_FENCE);}put(x+5*dir,y+2,z+1,Blocks.LANTERN);put(x,y+1,z-1,Blocks.BARREL);}
    void well(int cx,int cz){if(!area(cx-2,cz-2,cx+2,cz+2))return;int y=h(cx,cz);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){put(cx+dx,y-1,cz+dz,Blocks.COBBLESTONE);put(cx+dx,y,cz+dz,dx==0&&dz==0?Blocks.WATER:Blocks.COBBLESTONE);if(dx!=0||dz!=0)put(cx+dx,y+1,cz+dz,Blocks.COBBLESTONE_WALL);}
        for(int dx:new int[]{-1,1})for(int a=2;a<=3;a++)put(cx+dx,y+a,cz,Blocks.OAK_FENCE);for(int dx=-2;dx<=2;dx++)put(cx+dx,y+4,cz,Blocks.OAK_SLAB);hang(cx,y+3,cz,Blocks.LANTERN);}
    void trebuchet(int cx,int cz,int face){if(!area(cx-3,cz-3,cx+3,cz+3))return;int y=h(cx,cz);
        for(int dz:new int[]{-2,2})for(int a=1;a<=5;a++)put(cx,y+a,cz+dz,Blocks.DARK_OAK_LOG);for(int dz=-2;dz<=2;dz++)put(cx,y+6,cz+dz,Blocks.DARK_OAK_PLANKS);
        for(int a=-4;a<=4;a++)put(cx-a*face,y+6+(a>0?a/2:0),cz,Blocks.STRIPPED_DARK_OAK_LOG);put(cx+4*face,y+5,cz,Blocks.COBBLESTONE);put(cx+4*face,y+4,cz,Blocks.CHAIN);
        for(int dx=-2;dx<=2;dx++)for(int dz:new int[]{-3,3})put(cx+dx,y+1,cz+dz,Blocks.DARK_OAK_SLAB);put(cx-2*face,y+1,cz,Blocks.STONE);put(cx-3*face,y+1,cz+1,Blocks.STONE);}
    void wagon(int cx,int cz){if(!area(cx-3,cz-2,cx+3,cz+2))return;int y=h(cx,cz);
        for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=1;dz++)put(cx+dx,y+1,cz+dz,Blocks.SPRUCE_SLAB);for(int dx:new int[]{-2,2})for(int dz:new int[]{-2,2})put(cx+dx,y+1,cz+dz,Blocks.DARK_OAK_LOG);
        put(cx-1,y+2,cz,Blocks.BARREL);put(cx,y+2,cz,Blocks.CHEST);put(cx+1,y+2,cz-1,Blocks.WHITE_WOOL);put(cx+1,y+2,cz+1,Blocks.WHITE_WOOL);put(cx+1,y+3,cz,Blocks.WHITE_WOOL);put(cx+3,y+1,cz,Blocks.SPRUCE_FENCE);put(cx-2,y+2,cz-1,Blocks.SNOW);}
    void logStack(int cx,int cz){if(!area(cx-3,cz-2,cx+3,cz+2))return;int y=h(cx,cz);BlockState l=Blocks.SPRUCE_LOG.getDefaultState().with(PillarBlock.AXIS,Direction.Axis.X);
        for(int dx=-3;dx<=3;dx++)for(int dz=-2;dz<=2;dz++)for(int a=1;a<=3-Math.abs(dz);a++)set(cx+dx,y+a,cz+dz,l);
        put(cx+5,y+1,cz,Blocks.STRIPPED_SPRUCE_LOG);put(cx+5,y+2,cz,Blocks.SPRUCE_TRAPDOOR);}
    void windmill(int cx,int cz){if(!area(cx-3,cz-3,cx+3,cz+3))return;int y=h(cx,cz);
        for(int a=1;a<=12;a++)for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)==2||Math.abs(dz)==2)put(cx+dx,y+a,cz+dz,a<=4?Blocks.COBBLESTONE:Blocks.WHITE_TERRACOTTA);
        for(int a=0;a<3;a++)for(int dx=-2+a;dx<=2-a;dx++)for(int dz=-2+a;dz<=2-a;dz++)put(cx+dx,y+13+a,cz+dz,Blocks.DARK_OAK_PLANKS);
        put(cx,y+1,cz-2,Blocks.AIR);put(cx,y+2,cz-2,Blocks.AIR);int hz=cz+3,hy=y+10;put(cx,hy,hz,Blocks.DARK_OAK_LOG);
        for(int a=1;a<=7;a++){put(cx+a,hy,hz,Blocks.WHITE_WOOL);put(cx-a,hy,hz,Blocks.WHITE_WOOL);if(hy+a<=y+16)put(cx,hy+a,hz,Blocks.WHITE_WOOL);if(hy-a>y+2)put(cx,hy-a,hz,Blocks.WHITE_WOOL);}}
}
