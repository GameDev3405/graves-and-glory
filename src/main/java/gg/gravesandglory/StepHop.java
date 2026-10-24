package gg.gravesandglory;

/**
 * The step a stuck unit cannot jump (Phase 3 Task 8b): a jump needs three blocks of clear air above the lower cell, and under the low skirt of a spruce (its first ring of leaves is two above the foot of the trunk, so on a slope
 * one block lower it hangs at the head of the unit) the unit bumps its head and stands at the foot of a step one block high, jumping, for ever. The route harness of the supply houses found it on the wooded slopes of Frostmarch and
 * Hollowwood: one or two of twenty knights stood still for the whole round, at the same few cells of the map, from every start that passed there (the pathfinder sees room on the step and leads up it; sometimes it finds no path at all).
 * A unit that has not moved for the stuck time is set on the cell beside it, one block up, that is nearest to where it is going, when there is one (a floor to stand on, room for the unit above it). Nothing else is moved: a wall of two
 * blocks, a fence, a drop or a cell that is no nearer the goal is not a hop. Pure, so that test-tools/standalone/StepHopCheck.java can run it on a grid of blocks without the game.
 */
final class StepHop {
    private StepHop(){}
    /** What the world says about a cell: nothing in it that a unit would bump into (no collision shape, no fluid), and a full block face to stand on. */
    interface Probe{boolean open(int x,int y,int z);boolean floor(int x,int y,int z);}
    /** The cell (x, y, z), feet, that a unit with its feet at (x, y, z) steps up onto on its way to the point (gx, gz): one of the eight cells round its own, one block higher, with a floor under it, open at feet and at head, and at
     *  least 0.3 nearer the point than the unit is; the nearest of them. Null: none (the unit is not at a step, or the step leads away from where it is going). A unit is often a hair off an integer height, or part way up a jump: 0.1 is allowed. */
    static int[] up(double x,double y,double z,double gx,double gz,Probe w){
        int cx=(int)Math.floor(x),cz=(int)Math.floor(z),floorY=(int)Math.floor(y+0.1);
        double best=Math.hypot(gx-x,gz-z)-0.3;int[] cell=null;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            if(dx==0&&dz==0)continue;
            int nx=cx+dx,nz=cz+dz;
            if(!w.floor(nx,floorY,nz)||!w.open(nx,floorY+1,nz)||!w.open(nx,floorY+2,nz))continue;
            double d=Math.hypot(gx-(nx+.5),gz-(nz+.5));
            if(d<best){best=d;cell=new int[]{nx,floorY+1,nz};}
        }
        return cell;
    }
}
