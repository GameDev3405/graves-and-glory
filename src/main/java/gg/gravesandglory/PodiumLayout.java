package gg.gravesandglory;

/** Where the podium stage stands and where its figures and the camera go, with no Minecraft in it (PodiumStageCheck). One shared stage: 160 blocks north of the camp origin (z -310) and 52 beyond the camp island's far edge (z -418),
 *  and 204 beyond the arena world's north edge (z -266), so no map and no camp piece owns the ground. The stage faces south (toward the camp): the camera looks north (yaw 180) at the plinths, the backdrop behind them. */
final class PodiumLayout {
    private PodiumLayout() {}
    /** Stage centre column. */
    static final int X = 0, Z = -470;
    /** Half the dais across (x) and deep (z, either side of the centre column); the stage fits inside this box, the check keeps 2 spare. */
    static final int HALF_X = 10, HALF_Z = 8;
    /** Plinth heights above the dais top: rank 1 centre, rank 2 left, rank 3 right as seen from the view spot. */
    static final int[] HEIGHTS = {3, 2, 1};
    /** Rank 2 and rank 3 stand this many blocks left and right of the centre (the plinths are 3 wide, so one block of gap). */
    static final int SPACING = 4;
    /** The view spot is this far south of the centre plinth. */
    static final int VIEW_DISTANCE = 12;

    /** Stage centre block x,y,z: the top block of the dais (its walking surface is at y+1). */
    static int[] origin() { return new int[]{X, Spec.RULES[0].floorY(), Z}; }

    /** The standing points: rank 1 centre, rank 2 left, rank 3 right as seen from the view spot (the viewer looks north, so left is west). On the plinths' stripped-oak tops. */
    static double[][] plinths() {
        int[] o = origin();
        double[][] p = new double[3][];
        int[] dx = {0, -SPACING, SPACING};
        for (int i = 0; i < 3; i++) p[i] = new double[]{o[0] + 0.5 + dx[i], o[1] + 1 + HEIGHTS[i], o[2] + 0.5};
        return p;
    }

    /** x,y,z,yaw,pitch of the camera: 12 blocks in front (south) of the centre plinth, level with the top of plinth 1, looking straight at it. */
    static double[] viewSpot() {
        double[] c = plinths()[0];
        return new double[]{c[0], c[1], c[2] + VIEW_DISTANCE, 180, 0};
    }

    /** The middle of the stage at the dais surface: x, y, z. */
    static double[] centre() {
        int[] o = origin();
        return new double[]{o[0] + 0.5, o[1] + 1, o[2] + 0.5};
    }
}
