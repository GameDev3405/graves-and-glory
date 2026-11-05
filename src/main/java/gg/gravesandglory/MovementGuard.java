package gg.gravesandglory;

/** The vanilla "moved too quickly" test and the reference position it measures from, so a headless check can state the failure TeleportSync prevents. */
final class MovementGuard {
    private MovementGuard() {}

    /** Vanilla: the squared jump from the handler's last-tick reference, less the squared velocity, over 100 (one packet). */
    static boolean movedTooQuickly(double refX, double refY, double refZ, double newX, double newY, double newZ, double velocitySq) {
        double dx = newX - refX, dy = newY - refY, dz = newZ - refZ;
        return dx * dx + dy * dy + dz * dz - velocitySq > 100.0;
    }

    /**
     * The reference the handler holds after the server teleports a player at the end of a tick, before the handler ticks again: the old spot
     * unless the teleport synced it (ServerPlayNetworkHandler.syncWithPlayerPosition).
     */
    static double[] referenceAfterTeleport(double[] oldReference, double[] newPosition, boolean synced) {
        return synced ? newPosition.clone() : oldReference.clone();
    }
}
