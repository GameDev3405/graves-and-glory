package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;

/** Keeps the server's "moved too quickly" movement check quiet after a deliberate teleport (slice dry run 2026-11-02, minor 2). */
final class TeleportSync {
    private TeleportSync() {}

    /** Call right after ServerPlayerEntity.teleport: moves the network handler's last-tick reference to where the player now stands. */
    static void afterTeleport(ServerPlayerEntity p) {
        if (p.networkHandler != null) p.networkHandler.syncWithPlayerPosition();
    }
}
