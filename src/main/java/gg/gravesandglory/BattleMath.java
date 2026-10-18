package gg.gravesandglory;

/** Pure battle arithmetic shared by the server, the HUD and standalone checks. No Minecraft types. */
public final class BattleMath {
    private BattleMath() {}
    /** Ticks a shot spends in the air: distance over speed, rounded, clamped to [minTicks,maxTicks]. */
    public static int travelTicks(double distance, double blocksPerTick, int minTicks, int maxTicks) {
        long ticks = Math.round(distance / blocksPerTick);
        return (int) Math.max(minTicks, Math.min(maxTicks, ticks));
    }
    /** Whether a blow struck at lastHitAt can still credit a death at now: no more than window ticks ago, and not in the future. */
    public static boolean creditWindowOpen(long now, long lastHitAt, int window) {
        long age = now - lastHitAt;
        return age >= 0 && age <= window;
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
    /** Per army index, the point index that army must hold, or -1 (free to flank). Positions are {x,z}; near means within nearRadius.
     *  Guards first: a point owned by team keeps the nearest army standing near it as its guard (ties: the lower army index), and that army
     *  holds it, so a point is not left the moment it is taken. Then every owned point still unguarded takes the nearest army that is neither
     *  guarding nor holding. In both passes the nearest army-point pair is settled first, so an army takes at most one point and goes to the
     *  nearer of two. assignable (one per army) is false for an army the bots cannot order, a player's: it still guards the point it stands
     *  on, so no bot is pulled back to it, but it is never sent to one and always gets -1. */
    public static int[] garrison(int[] owners, int team, double[][] armyPos, boolean[] assignable, double[][] pointPos, double nearRadius) {
        int[] hold = new int[armyPos.length];
        for (int i = 0; i < hold.length; i++) hold[i] = -1;
        int n = Math.min(owners.length, pointPos.length);
        boolean[] open = new boolean[n], taken = new boolean[armyPos.length];
        for (int j = 0; j < n; j++) open[j] = owners[j] == team;
        int[] pair;
        while ((pair = nearestPair(open, taken, null, armyPos, pointPos, nearRadius)) != null) {
            taken[pair[0]] = true;
            open[pair[1]] = false;
            if (assignable[pair[0]]) hold[pair[0]] = pair[1];
        }
        while ((pair = nearestPair(open, taken, assignable, armyPos, pointPos, Double.POSITIVE_INFINITY)) != null) {
            taken[pair[0]] = true;
            open[pair[1]] = false;
            hold[pair[0]] = pair[1];
        }
        return hold;
    }
    /** The nearest {army, point} pair, at most limit apart, of an open point and an army not yet taken (and assignable, unless that is null),
     *  or null. Points, then armies, are scanned in index order and only a strictly nearer pair replaces the best, so ties go to the lower
     *  point index, then the lower army index. */
    private static int[] nearestPair(boolean[] open, boolean[] taken, boolean[] assignable, double[][] armyPos, double[][] pointPos, double limit) {
        int[] best = null;
        double bestDistance = limit;
        for (int j = 0; j < open.length; j++) {
            if (!open[j]) continue;
            for (int i = 0; i < armyPos.length; i++) {
                if (taken[i] || assignable != null && !assignable[i]) continue;
                double distance = Math.hypot(armyPos[i][0] - pointPos[j][0], armyPos[i][1] - pointPos[j][1]);
                if (best == null ? distance <= limit : distance < bestDistance) { best = new int[]{i, j}; bestDistance = distance; }
            }
        }
        return best;
    }
    /** The point index a bot army's lane sends it to. Dawn's lane i is point i; Dusk's is the mirror, points-1-i, so both teams
     *  play the same plan from their own side (Kingsfield: Dawn A, B, C against Dusk E, D, C; three points: Dusk C, B, A). */
    public static int laneTarget(int lane, int team, int points) {
        int i = Math.floorMod(lane, points);
        return team == 0 ? i : points - 1 - i;
    }
    /** A charge goes in when the army outnumbers the enemies near it by chargeRatio and, when chargeDistance is above zero, the nearest enemy is within it. */
    public static boolean timedCharge(double nearestEnemyDistance, long living, long enemiesNear, double chargeRatio, double chargeDistance) {
        return living >= enemiesNear * chargeRatio && (chargeDistance <= 0 || nearestEnemyDistance <= chargeDistance);
    }
    /** Brace behind shields when enough archers threaten, no melee has closed in, and there are shields to raise. */
    public static boolean shieldwallVsArchers(int rangedThreats, int meleeThreats, int threshold, boolean hasShields) {
        return hasShields && rangedThreats >= threshold && meleeThreats == 0;
    }
    /** Whether a shield troop holds its shield up. Shieldwall braces against any enemy. Block raises it only on the approach, with the
     *  enemy inside 6 blocks but outside sword reach (reachSq: the troop's range, squared), and lowers it to swing once the enemy is in
     *  reach, as a player must. Never on a charge or a retreat. Distances are squared, in blocks. */
    public static boolean raiseShield(String formation, String order, double nearestSq, double reachSq, boolean hasEnemy) {
        if (!hasEnemy || order.equals("charge") || order.equals("retreat")) return false;
        if (formation.equals("shieldwall")) return true;
        return formation.equals("block") && nearestSq > reachSq && nearestSq < 6 * 6;
    }
}
