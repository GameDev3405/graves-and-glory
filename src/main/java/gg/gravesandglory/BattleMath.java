package gg.gravesandglory;

/** Pure battle arithmetic shared by the server, the HUD and standalone checks. No Minecraft types. */
public final class BattleMath {
    private BattleMath() {}
    /** Ticks a shot spends in the air: distance over speed, rounded, clamped to [minTicks,maxTicks]. */
    public static int travelTicks(double distance, double blocksPerTick, int minTicks, int maxTicks) {
        long ticks = Math.round(distance / blocksPerTick);
        return (int) Math.max(minTicks, Math.min(maxTicks, ticks));
    }
    /** A group took a loss when a member died or its average health fell by at least one percent. */
    public static boolean tookLoss(float prevHealth, int prevCount, float health, int count) {
        return count < prevCount || prevHealth - health >= 0.01f - 1e-6f;
    }
}
