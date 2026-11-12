package gg.gravesandglory;

/** The podium stage's layout: where it stands relative to the arena and the camp, and what the camera sees from the view spot. */
public final class PodiumStageCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) {
        var rules = Spec.RULES[0];
        var camp = Spec.CAMP[0];
        int[] o = PodiumLayout.origin();
        check(o.length == 3, "origin is x,y,z");
        // Outside the arena world by 32 or more on at least one axis.
        check(Math.abs(o[0]) > rules.worldHalfX() + 32 || Math.abs(o[2]) > rules.worldHalfZ() + 32, "origin is outside the arena world by 32+");
        double d = Math.hypot(o[0] - camp.originX(), o[2] - camp.originZ());
        check(d >= 120 && d <= 300, "origin 120 to 300 blocks from the camp origin: " + d);
        // Clear of the camp island box: the stage footprint (half 12 across, 10 deep) never touches it.
        int half = PodiumLayout.HALF_X + 2, deep = PodiumLayout.HALF_Z + 2;
        boolean clearX = o[0] + half < camp.originX() + camp.islandMinDx() || o[0] - half > camp.originX() + camp.islandMaxDx();
        boolean clearZ = o[2] + deep < camp.originZ() + camp.islandMinDz() || o[2] - deep > camp.originZ() + camp.islandMaxDz();
        check(clearX || clearZ, "stage footprint clear of the camp island box");
        // Clear of the arena world box too.
        boolean awayX = o[0] - half > rules.worldHalfX() || o[0] + half < -rules.worldHalfX();
        boolean awayZ = o[2] - deep > rules.worldHalfZ() || o[2] + deep < -rules.worldHalfZ();
        check(awayX || awayZ, "stage footprint clear of the arena world box");

        double[][] p = PodiumLayout.plinths();
        check(p.length == 3 && p[0].length == 3, "three standing points");
        check(p[0][1] > p[1][1] && p[1][1] > p[2][1], "plinth 1 higher than 2 higher than 3");
        double[] c = PodiumLayout.centre();
        check(c.length == 3 && Math.abs(c[0] - p[0][0]) < 1e-9 && Math.abs(c[2] - p[0][2]) < 1e-9, "centre is the middle plinth column");

        double[] v = PodiumLayout.viewSpot();
        check(v.length == 5, "viewSpot is x,y,z,yaw,pitch");
        double dx = p[0][0] - v[0], dz = p[0][2] - v[2];
        check(Math.abs(Math.hypot(dx, dz) - 12) < 0.01, "view spot 12 blocks from plinth 1: " + Math.hypot(dx, dz));
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = Math.abs(((yaw - v[3]) % 360 + 540) % 360 - 180);
        check(diff <= 2, "yaw faces plinth 1: " + yaw + " vs " + v[3]);
        check(Math.abs(v[1] - p[0][1]) < 0.01, "view spot level with plinth 1's top");
        for (int i = 1; i < 3; i++) {
            double ax = p[i][0] - v[0], az = p[i][2] - v[2];
            double off = Math.abs(((Math.toDegrees(Math.atan2(-ax, az)) - v[3]) % 360 + 540) % 360 - 180);
            check(off < 25, "plinth " + (i + 1) + " inside the 50 degree field: " + off);
            // and still inside it after the 3 block push-in
            double fx = -Math.sin(Math.toRadians(v[3])) * 3, fz = Math.cos(Math.toRadians(v[3])) * 3;
            double bx = p[i][0] - (v[0] + fx), bz = p[i][2] - (v[2] + fz);
            double near = Math.abs(((Math.toDegrees(Math.atan2(-bx, bz)) - v[3]) % 360 + 540) % 360 - 180);
            check(near < 25.5, "plinth " + (i + 1) + " inside the field after the push-in: " + near);
        }
        System.out.println("PodiumStageCheck passed (" + checks + " checks)");
    }
}
