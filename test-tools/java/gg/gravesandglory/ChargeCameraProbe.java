package gg.gravesandglory;

import java.util.UUID;

/** Test-only server reads for ChargeCameraSuite (command UI plan Task 10). Never included in releases. */
public final class ChargeCameraProbe {
    private ChargeCameraProbe() {}

    /** The mean feet height of an army's living troops of a group ("all": every group), or NaN with none. */
    public static double troopsY(GravesAndGlory.Battle b, UUID owner, String group) {
        var s = b.squads.get(owner);
        double y = 0; int n = 0;
        for (var u : b.units) if (u.squad == s && u.mob.isAlive() && (group.equals("all") || u.spec.group().equals(group))) { y += u.mob.getY(); n++; }
        return n == 0 ? Double.NaN : y / n;
    }

    /** The server's own record of the player's look: {yaw, pitch}. */
    public static float[] serverLook(GravesAndGlory.Battle b, UUID owner) {
        var p = b.server.getPlayerManager().getPlayer(owner);
        return p == null ? null : new float[]{p.getYaw(), p.getPitch()};
    }
}
