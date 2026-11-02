package gg.gravesandglory;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.nio.file.Files;
import java.util.ArrayList;

/** UNSHIPPED route check on the final blocks (plan task 2A.5): from every faction spawn, a breadth-first walk over
 * standable cells (two clear cells over a solid or water floor; step up one, drop up to three) must reach every
 * capture point; then Walkability.scan counts blocked road, bridge and point cells. Report: mapforge/walk-(tag).txt. */
final class MapForgeRoutes {
    record Result(boolean ok,String summary){}
    static final int YMIN=0,YMAX=255;
    static ServerWorld w;static final BlockPos.Mutable p=new BlockPos.Mutable();

    static boolean clear(int x,int y,int z){BlockState s=w.getBlockState(p.set(x,y,z));return s.getCollisionShape(w,p).isEmpty()&&!s.getFluidState().isIn(FluidTags.LAVA);}
    static boolean floor(int x,int y,int z){BlockState s=w.getBlockState(p.set(x,y,z));return !s.getCollisionShape(w,p).isEmpty()||s.getFluidState().getFluid()==Fluids.WATER;}
    static boolean stand(int x,int y,int z){return floor(x,y-1,z)&&clear(x,y,z)&&clear(x,y+1,z);}
    static long key(int x,int y,int z){return ((long)(x+30000)<<36)|((long)(z+30000)<<12)|(y-YMIN);}
    static int kx(long k){return (int)(k>>>36)-30000;}static int kz(long k){return (int)((k>>>12)&0xffffff)-30000;}static int ky(long k){return (int)(k&0xfff)+YMIN;}

    static Result check(ServerWorld world,String tag){
        w=world;long t=System.nanoTime();
        int hx=GravesAndGlory.R.worldHalfX(),hz=GravesAndGlory.R.worldHalfZ();
        var lines=new ArrayList<String>();boolean ok=true;int routes=0,reached=0;
        for(var f:Spec.FACTIONS){
            int sx=f.spawnX(),sz=f.spawnZ(),sy=-1;
            for(int y=YMAX-2;y>YMIN;y--)if(stand(sx,y,sz)){sy=y;break;}
            if(sy<0){lines.add("spawn "+f.id()+" ("+sx+","+sz+"): no standable cell");ok=false;continue;}
            var dist=new Long2IntOpenHashMap();dist.defaultReturnValue(-1);var q=new LongArrayFIFOQueue();
            long start=key(sx,sy,sz);dist.put(start,0);q.enqueue(start);
            int[][] dirs={{1,0},{-1,0},{0,1},{0,-1}};
            while(!q.isEmpty()){
                long k=q.dequeueLong();int x=kx(k),y=ky(k),z=kz(k),d=dist.get(k);
                for(int[] dv:dirs){
                    int nx=x+dv[0],nz=z+dv[1];if(Math.abs(nx)>hx||Math.abs(nz)>hz)continue;
                    int to=Integer.MIN_VALUE;
                    if(y+1<YMAX-1&&clear(x,y+2,z)&&stand(nx,y+1,nz))to=y+1; // step up one
                    else for(int ny=y;ny>=y-3&&ny>YMIN;ny--){
                        if(!clear(nx,ny+1,nz))break; // a wall at body height: nothing lower is reachable through it
                        if(stand(nx,ny,nz)){to=ny;break;}
                        if(!clear(nx,ny,nz))break;
                    }
                    if(to!=Integer.MIN_VALUE){long nk=key(nx,to,nz);if(dist.get(nk)<0){dist.put(nk,d+1);q.enqueue(nk);}}
                }
            }
            lines.add("spawn "+f.id()+" ("+sx+","+sy+","+sz+"): "+dist.size()+" reachable cells");
            for(var pt:Spec.POINTS){
                routes++;int best=-1;
                for(var e:dist.long2IntEntrySet()){long k=e.getLongKey();int x=kx(k),z=kz(k),y=ky(k);
                    if((x-pt.x())*(x-pt.x())+(z-pt.z())*(z-pt.z())<=pt.radius()*pt.radius()&&y>=pt.elevation()-2&&y<=pt.elevation()+6&&(best<0||e.getIntValue()<best))best=e.getIntValue();}
                if(best>=0)reached++;else ok=false;
                lines.add("  to point "+pt.id()+" ("+pt.x()+","+pt.z()+"): "+(best<0?"NOT REACHED":best+" steps"));
            }
        }
        String scan="";
        if(GravesAndGlory.battle!=null){var s=Walkability.scan(GravesAndGlory.battle);scan=" walkable cells="+s.cells()+" blocked road/bridge="+s.road()+" in-building="+s.building()+" in-point="+s.point()+" through="+s.through()+" base="+s.base();lines.add("Walkability:"+scan);lines.add(s.detail());}
        String summary=reached+"/"+routes+" spawn-to-point routes reached;"+scan+" ("+(System.nanoTime()-t)/1_000_000+" ms)";
        try{var out=MapForge.file("walk-"+tag+".txt");Files.createDirectories(out.getParent());Files.writeString(out,summary+"\n"+String.join("\n",lines)+"\n");}catch(Exception e){MapForge.log("walk report: "+e);}
        return new Result(ok,summary);
    }
}
