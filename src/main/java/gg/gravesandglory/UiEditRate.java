package gg.gravesandglory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The server's rate limit on UI editor requests (ui_edit_v1), per player: a token bucket that holds up to {@code burst} requests and
 * refills {@code perSecond} a second. A request over the limit is dropped; the player is told at most once every {@link #WARN_MS}.
 * Pure (time is passed in), so the standalone check can drive it.
 */
final class UiEditRate {
    static final long WARN_MS = 2000;
    final double burst, perSecond;
    /** Per player: {tokens, last refill ms, last warning ms}. */
    private final Map<UUID, double[]> buckets = new HashMap<>();

    UiEditRate(double burst, double perSecond) { this.burst = burst; this.perSecond = perSecond; }

    /** Takes one request's token; false when the player is over the limit. */
    synchronized boolean take(UUID id, long nowMs) {
        double[] b = buckets.computeIfAbsent(id, k -> new double[] {burst, nowMs, Double.NEGATIVE_INFINITY});
        b[0] = Math.min(burst, b[0] + Math.max(0, nowMs - b[1]) * perSecond / 1000.0);
        b[1] = nowMs;
        if (b[0] < 1) return false;
        b[0] -= 1;
        return true;
    }

    /** True at most once every WARN_MS per player: whether a dropped request should say why. */
    synchronized boolean warn(UUID id, long nowMs) {
        double[] b = buckets.get(id);
        if (b == null || nowMs - b[2] < WARN_MS) return false;
        b[2] = nowMs;
        return true;
    }

    synchronized void forget(UUID id) { buckets.remove(id); }
}
