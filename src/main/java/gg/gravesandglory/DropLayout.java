package gg.gravesandglory;

/**
 * Where the drop board's altar stands (design 2026-11-14, section 4) and how DropSim's board frame maps into the world. Pure: the spot
 * is the dropBoard sheet row (originDx, originDz from the camp's origin, facing), so it moves with the sheet. The origin is the floor
 * block at the centre of the court; the altar stands at the court's back, its face toward `facing`, and the court runs out in front.
 */
final class DropLayout {
    private DropLayout() {}

    /** The court: this many blocks across the altar's width and from back to front. */
    static final int COURT_WIDTH = 15, COURT_DEPTH = 8;
    /** The coin's plane, in blocks behind the court's centre line (toward the altar's back), and the height of the rail (DropSim y = 0) over the court floor's top. */
    static final double PLANE_BACK = 2.875, RAIL_UP = 12.0;

    /** The centre floor block {x, y, z}. */
    static int[] origin() {
        var c = Spec.CAMP[0];
        var d = Spec.DROPBOARD[0];
        return new int[]{c.originX() + d.originDx(), c.floorY(), c.originZ() + d.originDz()};
    }

    /** The unit vector {x, z} out of the altar's face, toward the viewer. */
    static int[] out() {
        return switch (Spec.DROPBOARD[0].facing()) {
            case "north" -> new int[]{0, -1};
            case "south" -> new int[]{0, 1};
            case "east" -> new int[]{1, 0};
            default -> new int[]{-1, 0};
        };
    }
    /** The unit vector {x, z} of the board's x: the viewer's right as he faces the altar. */
    static int[] right() {
        int[] o = out();
        return new int[]{o[1], -o[0]};
    }

    /** The middle of the court floor's top {x, y, z}. */
    static double[] centre() {
        int[] o = origin();
        return new double[]{o[0] + 0.5, o[1] + 1, o[2] + 0.5};
    }

    /** A point of DropSim's board frame, lifted `forward` blocks out of the coin's plane toward the viewer, in the world {x, y, z}. */
    static double[] world(double bx, double by, double forward) {
        double[] c = centre();
        int[] o = out(), r = right();
        double side = bx - DropSim.WIDTH / 2.0, depth = forward - PLANE_BACK;
        return new double[]{c[0] + r[0] * side + o[0] * depth, c[1] + 1 + RAIL_UP - by, c[2] + r[1] * side + o[1] * depth};
    }
    static double[] world(double bx, double by) { return world(bx, by, 0); }

    /** The yaw (degrees, Minecraft's) of someone looking at the altar's face. */
    static float viewYaw() {
        int[] o = out();
        return (float) Math.toDegrees(Math.atan2(o[0], -o[1]));
    }

    /** Whether a spot is near enough to watch a drop (dropBoard.watchRadius from the court's centre, on the ground plane). */
    static boolean watches(double x, double z) {
        double[] c = centre();
        double r = Spec.DROPBOARD[0].watchRadius();
        return (x - c[0]) * (x - c[0]) + (z - c[2]) * (z - c[2]) <= r * r;
    }
}
