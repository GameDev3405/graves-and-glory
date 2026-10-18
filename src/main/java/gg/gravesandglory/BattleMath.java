package gg.gravesandglory;

/** Pure battle arithmetic shared by the server, the HUD and standalone checks. No Minecraft types. */
public final class BattleMath {
    private BattleMath() {}
    /** Ticks a shot spends in the air: distance over speed, rounded, clamped to [minTicks,maxTicks]. */
    public static int travelTicks(double distance, double blocksPerTick, int minTicks, int maxTicks) {
        long ticks = Math.round(distance / blocksPerTick);
        return (int) Math.max(minTicks, Math.min(maxTicks, ticks));
    }
    /** A group took a loss when a member died or its average health fell by at least 0.008 (one 4-damage hit on a full 12-knight group). */
    public static boolean tookLoss(float prevHealth, int prevCount, float health, int count) {
        return count < prevCount || prevHealth - health >= 0.008f - 1e-6f;
    }
    /** A group's fighting strength: living health over the health it started the round with, clamped to [0,1]. */
    public static double strength(double livingHealth, double fullHealth) {
        if (fullHealth <= 0) return 1;
        return Math.max(0, Math.min(1, livingHealth / fullHealth));
    }
    /** Degrees (0..180) between Minecraft's forward vector for a yaw, (-sin yaw, cos yaw), and the offset (dx,dz). */
    public static double relativeBearing(double facingYawDeg, double dx, double dz) {
        double length = Math.hypot(dx, dz);
        if (length < 1e-9) return 0;
        double yaw = Math.toRadians(facingYawDeg);
        double cos = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / length;
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }
    /** Flanked when at least minAttackers of the given attacker offsets sit more than arcDeg off the group's facing. */
    public static boolean flanked(double facingYawDeg, double[] dx, double[] dz, double arcDeg, int minAttackers) {
        int behind = 0;
        for (int i = 0; i < Math.min(dx.length, dz.length); i++) if (relativeBearing(facingYawDeg, dx[i], dz[i]) > arcDeg) behind++;
        return behind >= minAttackers;
    }
    /** A group breaks when its strength falls to routShare, or to flankedRoutShare while flanked. */
    public static boolean shouldRout(double strength, boolean flanked, double routShare, double flankedRoutShare) {
        return strength <= routShare || flanked && strength <= flankedRoutShare;
    }
    /** Index of the anchor nearest the target (tx,tz), or -1 when no anchor is strictly nearer to the target than the base (bx,bz). */
    public static int nearestAnchor(double tx, double tz, double bx, double bz, double[] ax, double[] az) {
        int best = -1;
        double bestDistance = Math.hypot(tx - bx, tz - bz);
        for (int i = 0; i < Math.min(ax.length, az.length); i++) {
            double distance = Math.hypot(tx - ax[i], tz - az[i]);
            if (distance < bestDistance) { bestDistance = distance; best = i; }
        }
        return best;
    }
    /** Index of the point to attack: the one minimising distance + defenders*defenderWeight among points not owned by team (all points when the team owns every one). */
    public static int leastDefended(double cx, double cz, double[] px, double[] pz, int[] owner, int[] defenders, int team, double defenderWeight) {
        int n = Math.min(Math.min(px.length, pz.length), Math.min(owner.length, defenders.length));
        boolean anyFree = false;
        for (int i = 0; i < n; i++) if (owner[i] != team) { anyFree = true; break; }
        int best = -1;
        double bestValue = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (anyFree && owner[i] == team) continue;
            double value = Math.hypot(px[i] - cx, pz[i] - cz) + defenders[i] * defenderWeight;
            if (value < bestValue) { bestValue = value; best = i; }
        }
        return best;
    }
    /** A charge goes in when the army outnumbers the enemies near it by chargeRatio and, when chargeDistance is above zero, the nearest enemy is within it. */
    public static boolean timedCharge(double nearestEnemyDistance, long living, long enemiesNear, double chargeRatio, double chargeDistance) {
        return living >= enemiesNear * chargeRatio && (chargeDistance <= 0 || nearestEnemyDistance <= chargeDistance);
    }
    /** Brace behind shields when enough archers threaten, no melee has closed in, and there are shields to raise. */
    public static boolean shieldwallVsArchers(int rangedThreats, int meleeThreats, int threshold, boolean hasShields) {
        return hasShields && rangedThreats >= threshold && meleeThreats == 0;
    }
}
