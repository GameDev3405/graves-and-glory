package gg.gravesandglory;

/**
 * The way out of a dead end for a unit that keeps getting stuck (Phase 3 Task 9 fix 1). A unit walks toward a waypoint on the straight line to its goal, and the pathfinder looks at no more than 640 cells. When the waypoint lies
 * behind a wall of a ruin, no whole path is found and the pathfinder returns the way to the explored cell nearest the waypoint; if that cell is the corner of a pocket of the ruin the unit goes into it, stands there with a path of one
 * node, and the sidestep (three blocks to one side, four on) leads to the same corner again, whichever side it takes. The regression run of the supply houses (Task 9) found one to three of twenty knights, odd and even slots alike,
 * standing for ever in the south-east pocket of the Hollowwood chapel ruin on the march to the house hw-supply-se (the nook goals of MapRouteChecks set a unit in it: it stays).
 * A unit that is stuck again at the place of its last sidestep, soon after it, does not sidestep again: a pathfinder with a larger share of cells (Tactics.detour) finds the whole path to its goal, and the unit walks the first dozen steps of it,
 * which lead it out of the pocket; each further repeat at the same wedge takes eight steps more (at most 40).
 * Pure (the count of repeats and the steps), so that test-tools/standalone/WedgeCheck.java can run it without the game.
 */
final class Wedge {
    private Wedge(){}
    /** A sidestep within this many ticks (ten seconds) of the last one counts as the same wedge. */
    static final long AGAIN_TICKS=200;
    /** And within this many blocks of where the last one was made. */
    static final double AGAIN_BLOCKS=6;
    /** The follow range given to the pathfinder of a detour (blocks; a unit's own is 40): 16 cells to the block, so 2560 cells. */
    static final double FOLLOW_RANGE=160;
    /** How many times before this one the unit has been stuck at this same wedge in a row: 0 for the first. had: it has sidestepped before; lastTime, lastX, lastZ and lastRepeats are that sidestep's tick, place and own count; time, x and z are now. */
    static int repeats(boolean had,long lastTime,double lastX,double lastZ,int lastRepeats,long time,double x,double z){
        if(!had||time<lastTime||time-lastTime>AGAIN_TICKS)return 0;
        double dx=x-lastX,dz=z-lastZ;
        return dx*dx+dz*dz<=AGAIN_BLOCKS*AGAIN_BLOCKS?lastRepeats+1:0;
    }
    /** The steps of the whole path a detour walks: 12 at the first repeat (repeats 1), eight more at each one after, at most 40. */
    static int steps(int repeats){return Math.min(40,12+8*Math.max(0,repeats-1));}
    /** The node of a path of that many nodes where the detour ends: that many steps on, or the last. */
    static int pick(int nodes,int steps){return Math.max(0,Math.min(nodes-1,steps));}
    /** Ticks a detour of that many steps may take before it is dropped: 40 and 10 a step, at most 400. */
    static long ticks(int steps){return Math.min(400L,40L+10L*steps);}
}
