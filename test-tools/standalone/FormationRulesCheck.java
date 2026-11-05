package gg.gravesandglory;

/**
 * Command UI plan Task 6: the Shield wall's geometry. A surrounded wall's square (BattleMath.squareLayout) puts its men evenly on four edges, each
 * facing straight out, and the front-arc test (BattleMath.frontArc at FRONT_ARC) takes an arrow from 59 degrees off the facing, not one from 61.
 * FormationRulesSuite checks the rules themselves in the game.
 */
public final class FormationRulesCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }
    static double off(double deg, double target) { double d = ((deg - target) % 360 + 540) % 360 - 180; return Math.abs(d); }
    public static void main(String[] args) {
        double[][] sq = BattleMath.squareLayout(12, 0.7);
        check(sq.length == 12, "squareLayout(12, 0.7) has 12 places, got " + sq.length);
        int[] perSide = new int[4];
        for (double[] p : sq) {
            check(p.length == 3, "each place is {side, back, facingYaw}");
            int side = -1;
            for (int q = 0; q < 4; q++) if (off(p[2], q * 90) <= 1) side = q;
            check(side >= 0, "facing " + p[2] + " is within 1 degree of 0, 90, 180 or 270");
            perSide[side]++;
        }
        for (int q = 0; q < 4; q++) check(perSide[q] == 3, "3 places face " + q * 90 + ", got " + perSide[q]);
        // every man stands on his own edge, out from the square's centre the way he faces
        double cs = 0, cb = 0; for (double[] p : sq) { cs += p[0]; cb += p[1]; } cs /= sq.length; cb /= sq.length;
        for (double[] p : sq) {
            double yaw = Math.toRadians(p[2]); // in the side/back frame a facing f points to side -sin(f)... the frame's own: facing 0 is -back, 90 is -side
            double fs = -Math.sin(yaw), fb = -Math.cos(yaw);
            check((p[0] - cs) * fs + (p[1] - cb) * fb > 0.5, "place " + p[0] + ", " + p[1] + " faces out (facing " + p[2] + ")");
        }
        for (int i = 0; i < sq.length; i++) for (int j = i + 1; j < sq.length; j++)
            check(Math.hypot(sq[i][0] - sq[j][0], sq[i][1] - sq[j][1]) >= 0.7 - 1e-9, "places " + i + " and " + j + " stand a cell apart");
        check(BattleMath.squareLayout(0, 0.7).length == 0, "no men, no places");
        for (double facing : new double[]{0, 90, -135, 200}) {
            double y59 = Math.toRadians(facing + 59), y61 = Math.toRadians(facing - 61);
            check(BattleMath.frontArc(facing, -Math.sin(y59), Math.cos(y59), BattleMath.FRONT_ARC), "59 degrees off facing " + facing + " is in the front arc");
            check(!BattleMath.frontArc(facing, -Math.sin(y61), Math.cos(y61), BattleMath.FRONT_ARC), "61 degrees off facing " + facing + " is outside the front arc");
        }
        check(BattleMath.FRONT_ARC == 60, "the front arc is +-60 degrees");
        System.out.println("FormationRulesCheck: " + count + " checks passed");
    }
}
