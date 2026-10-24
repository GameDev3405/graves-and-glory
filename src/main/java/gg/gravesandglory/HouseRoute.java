package gg.gravesandglory;

/**
 * The way into a supply house (Phase 3 Task 8b): where a unit bound for a point inside the walled yard of a house goes first. The yard is walled 2 high on three sides and the units' own pathfinding
 * (a search of 40 blocks from each waypoint, Tactics.walk) does not find the way round a wall of 31 blocks to a gate that is not near: the first run of the route harness had 13 to 16 of its 20 knights
 * at the yard from the starts that met the wall away from a gate, and the rest stood outside it. A unit outside the footprint therefore walks to a point just outside a gate first (round the corners of
 * the house when the straight line to the gate would cross it) and, within 3 blocks of the gate, straight on to its goal, through the opening.
 * The gates are those of the five supply/* pieces (they share one layout, 31 wide and 37 deep, authored facing south): the front gate in the south wall at local x 11..19 and the side gate in the east
 * wall at local z 22..26. Pure functions, so that test-tools/standalone/HouseRouteCheck.java can run them without the game.
 */
final class HouseRoute {
    private HouseRoute(){}
    /** The footprint of the pieces, and what is left round it at the corners (a unit walking round the house keeps this far off the walls). */
    static final double WIDTH=31,DEPTH=37,MARGIN=3.0;
    /** A unit this near a gate point (blocks, horizontally) goes straight on to its goal. */
    static final double ARRIVE=3.0;
    /** The cells just outside the front gate (local (15,37)) and the side gate (local (31,24)), as points: the centres of those cells. */
    static final double[][] GATES={{15.5,37.5},{31.5,24.5}};

    /** The world point of a local point (lx, lz) of the house whose centre block is (cx, cz), turned by rotation (0 or 180). */
    static double[] world(int cx,int cz,int rotation,double lx,double lz){
        double dx=lx-15.5,dz=lz-18.5;
        if(rotation==180){dx=-dx;dz=-dz;}
        return new double[]{cx+.5+dx,cz+.5+dz};
    }
    /** (x, z) is in the footprint of the house, grown by pad blocks. */
    static boolean inside(int cx,int cz,double x,double z,double pad){return Math.abs(x-(cx+.5))<=WIDTH/2+pad&&Math.abs(z-(cz+.5))<=DEPTH/2+pad;}
    /** The segment from a to b does not cross the footprint (a little grown: the walls) of the house. */
    static boolean clear(int cx,int cz,double ax,double az,double bx,double bz){
        double x0=cx+.5-WIDTH/2-0.3,x1=cx+.5+WIDTH/2+0.3,z0=cz+.5-DEPTH/2-0.3,z1=cz+.5+DEPTH/2+0.3;
        double t0=0,t1=1,dx=bx-ax,dz=bz-az;
        double[] p={-dx,dx,-dz,dz},q={ax-x0,x1-ax,az-z0,z1-az};
        for(int i=0;i<4;i++){
            if(Math.abs(p[i])<1e-12){if(q[i]<=0)return true;continue;} // parallel to this side and outside it: never in the box
            double t=q[i]/p[i];
            if(p[i]<0){if(t>t1)return true;if(t>t0)t0=t;}else{if(t<t0)return true;if(t<t1)t1=t;}
        }
        return t1-t0<1e-9; // a touch of the edge is not a crossing
    }
    static double dist(double ax,double az,double bx,double bz){return Math.hypot(ax-bx,az-bz);}

    /** The first point a unit at (hx, hz) walks to on its way to (gx, gz): null when the goal is not in the footprint of the house, when the unit is in it already or within ARRIVE of the gate it would use
     *  (it goes straight to the goal), else the shortest way to a gate: straight to the point outside it, or round one or two corners of the house first. lateral moves the point along the wall (a unit's place in the file,
     *  so that twenty knights do not all aim at one block). */
    static double[] next(int cx,int cz,int rotation,double hx,double hz,double gx,double gz,double lateral){
        if(!inside(cx,cz,gx,gz,0.5)||inside(cx,cz,hx,hz,0.3))return null;
        double m=MARGIN,xs=WIDTH/2+m,zs=DEPTH/2+m;
        double[][] corner={{cx+.5-xs,cz+.5-zs},{cx+.5+xs,cz+.5-zs},{cx+.5+xs,cz+.5+zs},{cx+.5-xs,cz+.5+zs}};
        double best=Double.MAX_VALUE;double[] first=null;
        for(int g=0;g<GATES.length;g++){
            double[] p=world(cx,cz,rotation,GATES[g][0]+(g==0?lateral:0),GATES[g][1]+(g==1?lateral:0));
            if(dist(hx,hz,p[0],p[1])<ARRIVE)return null;
            if(clear(cx,cz,hx,hz,p[0],p[1])){double c=dist(hx,hz,p[0],p[1]);if(c<best){best=c;first=p;}}
            for(int i=0;i<4;i++){
                double[] c1=corner[i];if(!clear(cx,cz,hx,hz,c1[0],c1[1]))continue;
                if(clear(cx,cz,c1[0],c1[1],p[0],p[1])){double c=dist(hx,hz,c1[0],c1[1])+dist(c1[0],c1[1],p[0],p[1]);if(c<best){best=c;first=c1;}}
                for(int j=0;j<4;j++){
                    if(j==i)continue;double[] c2=corner[j];
                    if(!clear(cx,cz,c1[0],c1[1],c2[0],c2[1])||!clear(cx,cz,c2[0],c2[1],p[0],p[1]))continue;
                    double c=dist(hx,hz,c1[0],c1[1])+dist(c1[0],c1[1],c2[0],c2[1])+dist(c2[0],c2[1],p[0],p[1]);
                    if(c<best){best=c;first=c1;}
                }
            }
        }
        return first;
    }
}
