package gg.gravesandglory;

/**
 * Prop geometry under rotation (prop kit plan task S3). Pure: JDK only.
 *
 * <p>Facing is 0, 90, 180 or 270, clockwise seen from above, and turns exactly as {@code Structures.turn}: 90 turns east to south,
 * (dx,dz) -> (-dz,dx). The anchor cell is the Structures pivot {@code ((sx-1)/2, 0, (sz-1)/2)}; the anchor is a
 * {@code minecraft:command_block} one block under it whose {@code facing} state ({@code north/east/south/west}) is the prop facing
 * (S1 spike: WorldEdit turns {@code facing}, not jigsaw {@code orientation}).
 */
public final class PropGeometry {
    private PropGeometry(){}
    static final String[] DIRECTIONS={"north","east","south","west"};

    /** Turns (dx,dz) clockwise by facing, as Structures.turn. */
    static double[] turn(int facing,double dx,double dz){
        return switch(facing){
            case 0->new double[]{dx,dz};
            case 90->new double[]{-dz,dx};
            case 180->new double[]{-dx,-dz};
            case 270->new double[]{dz,-dx};
            default->throw new IllegalArgumentException("facing must be 0, 90, 180 or 270, not "+facing);
        };
    }

    static int valid(int facing){
        if(facing!=0&&facing!=90&&facing!=180&&facing!=270)throw new IllegalArgumentException("facing must be 0, 90, 180 or 270, not "+facing);
        return facing;
    }

    /** {a+b} wrapped to 0..359. */
    public static int turnDeg(int a,int b){return ((a+b)%360+360)%360;}

    /** World offset {dx,dy,dz} of cell (lx,ly,lz) of the prop from its anchor cell, turned by facing. */
    public static int[] cell(PropDef d,int facing,int lx,int ly,int lz){
        int px=(d.sx()-1)/2,pz=(d.sz()-1)/2;
        double[] t=turn(facing,lx-px,lz-pz);
        return new int[]{(int)t[0],ly,(int)t[1]};
    }

    /** Offset in blocks from the anchor cell's min corner of the point (x,y,z) given in the cell frame (blocks), turned by facing about the
     *  anchor cell centre. */
    public static double[] point(PropDef d,int facing,double x,double y,double z){
        int px=(d.sx()-1)/2,pz=(d.sz()-1)/2;
        double[] t=turn(facing,x-(px+.5),z-(pz+.5));
        return new double[]{t[0]+.5,y,t[1]+.5};
    }

    /** Every part's item_display (S1 calibration): position {ox,oy,oz} relative to the anchor cell's min corner = the anchor cell centre,
     *  yaw 180+facing degrees, transformation translation 0, scale 1, item_display "fixed". */
    public static double[] partTransform(PropDef d,int facing){
        return new double[]{.5,.5,.5,(180+valid(facing))%360};
    }

    /** One part's item_display: {@link #partTransform(PropDef,int)} moved by the part's offset (model px, prop frame) turned by facing. */
    public static double[] partTransform(PropDef d,int facing,PropDef.Part p){
        double[] t=partTransform(d,facing),o=turn(facing,p.ox()/16.0,p.oz()/16.0);
        return new double[]{t[0]+o[0],t[1]+p.oy()/16.0,t[2]+o[1],t[3]};
    }

    /** How far (blocks) any part entity of a prop sits from its anchor cell centre. */
    public static double reach(PropDef d){
        double r=0;for(PropDef.Part p:d.parts())r=Math.max(r,Math.max(Math.abs(p.ox()),Math.max(Math.abs(p.oy()),Math.abs(p.oz())))/16.0);
        return r;
    }

    /** The prop facing of a command-block {@code facing} value: north 0, east 90, south 180, west 270; anything else -1. */
    public static int facingOf(String dir){
        if(dir!=null)for(int i=0;i<4;i++)if(DIRECTIONS[i].equals(dir))return 90*i;
        return -1;
    }

    /** The command-block {@code facing} value of a prop facing (inverse of facingOf). */
    public static String direction(int facing){
        return DIRECTIONS[valid(facing)/90];
    }
}
