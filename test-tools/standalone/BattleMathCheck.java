package gg.gravesandglory;

/** Pure Phase 1 battle maths: projectile travel, HUD loss detection and rout maths. Never shipped. */
public final class BattleMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        check(BattleMath.travelTicks(22, 2.2, 2, 14) == 10, "arrow at max range lands in 10 ticks");
        check(BattleMath.travelTicks(1, 2.2, 2, 14) == 2, "point-blank shots still travel the minimum");
        check(BattleMath.travelTicks(400, 1.2, 2, 14) == 14, "travel is capped");
        check(BattleMath.tookLoss(.80f, 12, .78f, 12) && BattleMath.tookLoss(.8f, 12, .9f, 11), "health drop or a death flashes");
        check(!BattleMath.tookLoss(.80f, 12, .805f, 12) && !BattleMath.tookLoss(.5f, 6, .7f, 6), "noise and healing do not flash");
        check(!BattleMath.tookLoss(.80f, 12, .795f, 12), "a sub-threshold drop does not flash");
        check(BattleMath.tookLoss(1f, 12, 1f - 4f / 36f / 12f, 12), "one arrow on a full 12-knight group flashes");
        check(Math.abs(BattleMath.strength(60, 360) - 1/6.0) < 1e-9 && BattleMath.strength(400, 360) == 1 && BattleMath.strength(5, 0) == 1, "strength is health-weighted and clamped");
        check(BattleMath.relativeBearing(0, 0, 1) < 1 && Math.abs(BattleMath.relativeBearing(0, 0, -1) - 180) < 1 && Math.abs(BattleMath.relativeBearing(0, -1, 0) - 90) < 1, "yaw 0 faces +z");
        double[] rx={0,.5,-.5}, rz={-5,-5,-5}, fx={0,.5,-.5}, fz={5,5,5};
        check(BattleMath.flanked(0, rx, rz, 110, 3) && !BattleMath.flanked(0, fx, fz, 110, 3) && !BattleMath.flanked(0, new double[]{0,0}, new double[]{-5,-5}, 110, 3), "three rear attackers flank; front or two do not");
        check(BattleMath.shouldRout(.2, false, .25, .6) && BattleMath.shouldRout(.5, true, .25, .6), "weak routs; flanked half-strength routs");
        check(!BattleMath.shouldRout(.5, false, .25, .6) && !BattleMath.shouldRout(.9, true, .25, .6), "half-strength unflanked and flanked fresh hold");
        double[] ax={0,-40}, az={0,-140};
        check(BattleMath.nearestAnchor(40, 0, -108, 0, ax, az) == 0, "centre point is nearer the front than base");
        check(BattleMath.nearestAnchor(-100, 0, -108, 0, ax, az) == -1, "base wins when the fight is at home");
        check(BattleMath.nearestAnchor(40, 0, -108, 0, new double[0], new double[0]) == -1, "no anchors means base");
        System.out.println("BattleMathCheck: " + checks + " checks passed");
    }
}
