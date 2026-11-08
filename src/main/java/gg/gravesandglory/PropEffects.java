package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/**
 * Which alive effects a prop plays on a tick (prop kit plan task S5). Pure: JDK only.
 *
 * <p>The ticker runs every {@link #STEP} ticks, so an effect's period is its {@code every} rounded up to a multiple of STEP and its phase is
 * a multiple of STEP chosen from the anchor seed: effects fire only on ticks that are multiples of STEP, props desync, and the same anchor
 * always plays the same sequence. {@code chance} is rolled from (seed, tick, effect index), never from a shared random.
 */
public final class PropEffects {
    private PropEffects(){}
    /** Ticks between ticker runs. */
    public static final int STEP=5;

    /** The effect seed of the prop anchored at cell (x,y,z). */
    public static long seed(int x,int y,int z){
        return mix(((long)x&0x3FFFFFFL)<<38|((long)z&0x3FFFFFFL)<<12|((long)y&0xFFFL));
    }

    /** {@code every} rounded up to a multiple of STEP (at least STEP). */
    public static int period(int every){return Math.max(STEP,(every+STEP-1)/STEP*STEP);}

    /** Ticks 0..period-1 (a multiple of STEP) by which effect {@code index} of a prop with this seed is shifted. */
    public static int phase(long seed,int index,int every){
        int slots=period(every)/STEP;
        return (int)Long.remainderUnsigned(mix(seed+0x9E3779B97F4A7C15L*(index+1)),slots)*STEP;
    }

    /** Whether effect {@code index} (e) of a prop with this seed fires on {@code tick}. */
    public static boolean fires(PropDef.Effect e,int index,long tick,long seed){
        if(Math.floorMod(tick,STEP)!=0)return false;
        int p=period(e.every());
        if(Math.floorMod(tick-phase(seed,index,e.every()),p)!=0)return false;
        if(e.chance()>=1)return true;
        return roll(seed,tick,index)<e.chance();
    }

    /** The effects of {@code d} that fire on {@code tick} for the prop with this seed, in def order; empty (shared) when none. */
    public static List<PropDef.Effect> due(PropDef d,long tick,long seed){
        List<PropDef.Effect> out=null;
        List<PropDef.Effect> all=d.effects();
        for(int i=0;i<all.size();i++){
            PropDef.Effect e=all.get(i);
            if(!fires(e,i,tick,seed))continue;
            if(out==null)out=new ArrayList<>(2);
            out.add(e);
        }
        return out==null?List.of():out;
    }

    /** Where effect e plays, as an offset in blocks from the anchor cell's min corner, turned with the prop's facing. */
    public static double[] at(PropDef d,int facing,PropDef.Effect e){return PropGeometry.point(d,facing,e.x(),e.y(),e.z());}

    /** A uniform 0..1 roll from (seed, tick, index). */
    static double roll(long seed,long tick,int index){
        return (mix(seed^mix(tick*0xC2B2AE3D27D4EB4FL+index))>>>11)*0x1.0p-53;
    }

    /** SplitMix64 finaliser. */
    static long mix(long z){
        z+=0x9E3779B97F4A7C15L;
        z=(z^(z>>>30))*0xBF58476D1CE4E5B9L;
        z=(z^(z>>>27))*0x94D049BB133111EBL;
        return z^(z>>>31);
    }
}
