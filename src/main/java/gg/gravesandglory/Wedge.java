package gg.gravesandglory;

/**
 * The way out of a dead end for a unit that keeps getting stuck (Phase 3 Task 9 fix 1 and fix 2). A unit walks toward a waypoint on the straight line to its goal, and the pathfinder looks at no more than 640 cells. When the waypoint lies
 * behind a wall of a ruin, no whole path is found and the pathfinder returns the way to the explored cell nearest the waypoint; if that cell is the corner of a pocket of the ruin the unit goes into it, stands there with a path of one
 * node, and the sidestep (three blocks to one side, four on) leads to the same corner again, whichever side it takes. The regression run of the supply houses (Task 9) found one to three of twenty knights, odd and even slots alike,
 * standing for ever in the south-east pocket of the Hollowwood chapel ruin on the march to the house hw-supply-se (the nook goals of MapRouteChecks set a unit in it: it stays).
 * A unit that is stuck again at the place of its last sidestep, soon after it, does not sidestep again: a pathfinder with a larger share of cells (Tactics.detour) finds the whole path to its goal, and the unit walks the first dozen steps of it,
 * which lead it out of the pocket; each further repeat at the same wedge takes eight steps more (at most 40).
 * Fix 2: that search has a price, so it is bounded. It looks for a path to the goal, or to a cell REACH blocks toward it when the goal is farther (the 2560 cells reach a goal about 100 blocks off, not one 200 off: a miss costs the whole
 * search, 8 ms and more); a miss is remembered for RETRY_TICKS (the sidestep runs meanwhile); and at most MAX_SEARCHES start in one tick, over all units. The point of a detour is a node at least AWAY_BLOCKS from the unit, so that a path that
 * doubles back along a thin wall does not end the detour at once.
 * Pure (the count of repeats, the choice, the point, the end), so that test-tools/standalone/WedgeCheck.java can run it without the game.
 */
final class Wedge {
    private Wedge(){}
    /** A sidestep within this many ticks (ten seconds) of the last one counts as the same wedge. */
    static final long AGAIN_TICKS=200;
    /** And within this many blocks of where the last one was made. */
    static final double AGAIN_BLOCKS=6;
    /** The follow range given to the pathfinder of a detour (blocks; a unit's own is 40): 16 cells to the block, so 2560 cells. */
    static final double FOLLOW_RANGE=160;
    /** The farthest a detour looks for a path: a goal beyond this many blocks is replaced by the cell this far along the line to it. (The 2560 cells reached a goal 100 blocks off in 22 of 22 searches of the review, one 140 off in 6 of 22, one 180 off in none.) */
    static final double REACH=80;
    /** A search that found no path to the unit's goal is not made again for this many ticks (15 seconds): the unit sidesteps as it did before. */
    static final long RETRY_TICKS=300;
    /** At most this many detour searches start in one tick, over all units: the worst tick costs one search (8.5 ms at the cap of 2560 cells on a miss, 0.4 to 5 ms when a path is found; measured in the review). */
    static final int MAX_SEARCHES=1;
    /** The point of a detour is at least this many blocks (across the ground) from the unit. */
    static final double AWAY_BLOCKS=5;
    /** A detour ends when the unit is within this many blocks of its point (across the ground) and less than ARRIVE_DY blocks above or below it. */
    static final double ARRIVE_BLOCKS=2,ARRIVE_DY=1.5;
    /** Or when the goal it was made for has moved this many blocks (a new order, an enemy that walked on). */
    static final double MOVED_BLOCKS=4;
    /** How many times before this one the unit has been stuck at this same wedge in a row: 0 for the first. had: it has sidestepped before; lastTime, lastX, lastZ and lastRepeats are that sidestep's tick, place and own count; time, x and z are now. */
    static int repeats(boolean had,long lastTime,double lastX,double lastZ,int lastRepeats,long time,double x,double z){
        if(!had||time<lastTime||time-lastTime>AGAIN_TICKS)return 0;
        double dx=x-lastX,dz=z-lastZ;
        return dx*dx+dz*dz<=AGAIN_BLOCKS*AGAIN_BLOCKS?lastRepeats+1:0;
    }
    /** The steps of the whole path a detour walks: 12 at the first repeat (repeats 1), eight more at each one after, at most 40. */
    static int steps(int repeats){return Math.min(40,12+8*Math.max(0,repeats-1));}
    /** Ticks a detour of that many steps may take before it is dropped: 40 and 10 a step, at most 400. */
    static long ticks(int steps){return Math.min(400L,40L+10L*steps);}
    /** What a stuck unit does: SEARCH for a detour, or sidestep because it is the first stuck here (FIRST), the last search found nothing a short while ago (COOLING), or a search has started in this tick already (BUDGET). */
    static final int SEARCH=0,FIRST=1,COOLING=2,BUDGET=3;
    static final String[] WHY={"search","first stuck","cooling after a miss","search budget of the tick"};
    static int next(int repeats,long time,long retryAt,int searchesThisTick){
        if(repeats<1)return FIRST;
        if(time<retryAt)return COOLING;
        if(searchesThisTick>=MAX_SEARCHES)return BUDGET;
        return SEARCH;
    }
    /** Where the detour looks for a path to: the goal (gx, gz) itself within REACH blocks of the unit (ux, uz), else the point REACH blocks along the line from the unit to it. */
    static double[] target(double ux,double uz,double gx,double gz){
        double dx=gx-ux,dz=gz-uz,d=Math.sqrt(dx*dx+dz*dz);
        if(d<=REACH)return new double[]{gx,gz};
        return new double[]{ux+dx/d*REACH,uz+dz/d*REACH};
    }
    /** The node of a path of `nodes` nodes (x and z across the ground in xs and zs) where the detour ends: `steps` steps on, then on to the first node at least AWAY_BLOCKS from the unit (ux, uz); the last node when none is. */
    static int pick(double[] xs,double[] zs,int nodes,double ux,double uz,int steps){
        int i=Math.max(0,Math.min(nodes-1,steps));
        while(i<nodes-1){
            double dx=xs[i]-ux,dz=zs[i]-uz;
            if(dx*dx+dz*dz>=AWAY_BLOCKS*AWAY_BLOCKS)break;
            i++;
        }
        return i;
    }
    /** Is the detour over: the time is up (until), the unit (ux, uy, uz) has come to its point (px, py, pz), or the goal now (gx, gz) is not where it was when the detour was made (fx, fz)? */
    static boolean over(long time,long until,double ux,double uy,double uz,double px,double py,double pz,double gx,double gz,double fx,double fz){
        if(time>=until)return true;
        double dx=px-ux,dz=pz-uz;
        if(dx*dx+dz*dz<ARRIVE_BLOCKS*ARRIVE_BLOCKS&&Math.abs(py-uy)<ARRIVE_DY)return true;
        double mx=gx-fx,mz=gz-fz;
        return mx*mx+mz*mz>MOVED_BLOCKS*MOVED_BLOCKS;
    }
}
