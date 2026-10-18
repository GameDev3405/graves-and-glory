package gg.gravesandglory;

import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

/**
 * The walk scan, shared by MapTour and Phase3Checks (harness only, never in the player jar). It visits every road cell, every bridge deck cell
 * (the planks) and every capture-ring cell of the arena. A cell is blocked when a block with a collision shape stands at Arena.ground+1 or +2,
 * where a knight's feet and head go. Each blocked cell is counted once, in this order:
 * road (on a deck; or on a road, outside every footprint and ring), base (inside the footprint of a base, a barracks or a "..._stores" structure),
 * building (inside any other footprint, bridges excepted) and through (a building cell that a road of width 4 or more covers while neither end
 * of that road lies inside that footprint: a house lane ends at its house, a through road does not), point (inside a capture ring).
 * A through cell counts in building and in through; detail names it "through". detail has one "kind x z block" line per blocked cell.
 * A deck is the planks only: columns x..x+width-1 and rows z..z+depth-1 from x=s.x()-width/2, z=s.z()-depth/2. Arena.ground also counts the
 * row z+depth and the column x+width as bridge, but nothing is built on them as a deck.
 */
final class Walkability {
    private Walkability(){}
    record Scan(int cells,int road,int through,int building,int base,int point,String detail){}

    /** The whole arena. */
    static Scan scan(GravesAndGlory.Battle b){return scan(b,null);}
    /** Only the cells inside the x and z range of area (one footprint); null scans the whole arena. */
    static Scan scan(GravesAndGlory.Battle b,BlockBox area){
        int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ(),x0=-hx,x1=hx,z0=-hz,z1=hz;
        if(area!=null){x0=Math.max(x0,area.getMinX());x1=Math.min(x1,area.getMaxX());z0=Math.max(z0,area.getMinZ());z1=Math.min(z1,area.getMaxZ());}
        int cells=0,road=0,through=0,building=0,base=0,point=0;var all=new StringBuilder();
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){
            boolean deck=onDeck(x,z),ring=inRing(x,z);
            if(!deck&&!ring&&!Arena.road(x,z))continue;cells++;int g=Arena.ground(x,z);String hit=null;
            for(int y=g+1;y<=g+2&&hit==null;y++){var pos=new BlockPos(x,y,z);var st=b.world.getBlockState(pos);if(!st.getCollisionShape(b.world,pos).isEmpty())hit=Registries.BLOCK.getId(st.getBlock()).getPath();}
            if(hit==null)continue;
            String kind="road";
            if(!deck){
                boolean inBase=false,inBuilding=false,inThrough=false;
                for(var s:Spec.STRUCTURES){if(s.kind().equals("bridge")||!inside(s,x,z))continue;if(baseLike(s))inBase=true;else{inBuilding=true;inThrough|=through(x,z,s);}}
                kind=inBase?"base":inThrough?"through":inBuilding?"building":ring?"point":"road";
            }
            switch(kind){case "road"->road++;case "through"->{building++;through++;}case "building"->building++;case "base"->base++;default->point++;}
            all.append(kind).append(' ').append(x).append(' ').append(z).append(' ').append(hit).append('\n');
        }
        return new Scan(cells,road,through,building,base,point,all.toString());
    }

    /** The planks of a bridge, not Arena.ground's wider test. */
    static boolean onDeck(int x,int z){
        for(var s:Spec.STRUCTURES)if(s.kind().equals("bridge")){int bx=s.x()-s.width()/2,bz=s.z()-s.depth()/2;if(x>=bx&&x<bx+s.width()&&z>=bz&&z<bz+s.depth())return true;}
        return false;
    }
    static boolean inRing(int x,int z){for(var p:Spec.POINTS)if((x-p.x())*(x-p.x())+(z-p.z())*(z-p.z())<=p.radius()*p.radius())return true;return false;}
    /** The footprint test MapDressing.onStructure uses with no margin. */
    static boolean inside(Spec.StructuresRow s,int x,int z){return Math.abs(x-s.x())<=s.width()/2&&Math.abs(z-s.z())<=s.depth()/2;}
    static boolean baseLike(Spec.StructuresRow s){return s.kind().equals("base")||s.kind().equals("barracks")||s.id().endsWith("_stores");}
    /** A road of width 4 or more covers the cell and neither end of it is inside the footprint of s. */
    static boolean through(int x,int z,Spec.StructuresRow s){
        for(var r:CampaignMaps.roads)if(r.width()>=4&&CampaignMaps.distance(x,z,r)<r.width()&&!inside(s,r.ax(),r.az())&&!inside(s,r.bx(),r.bz()))return true;
        return false;
    }
}
