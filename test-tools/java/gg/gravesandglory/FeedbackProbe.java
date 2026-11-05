package gg.gravesandglory;

import net.minecraft.util.math.Vec3d;

/**
 * Test-only reach into package-private command-UI state for cmdui.FeedbackSuite and FeedbackMate (command UI plan Task 9): the aim point,
 * the stance a troop would be posed in, and the server's per-kind horn throttle. Never included in releases.
 */
public final class FeedbackProbe {
    private FeedbackProbe() {}

    /** The ground point the aim ray hits (CommandMode.tick), null when none. */
    public static Vec3d destination() { return CommandMode.destination; }
    /** The facing (degrees) the aimed order will carry. */
    public static float facing() { return CommandMode.facing; }
    /** The formation id an aimed Formation order will carry. */
    public static String formation() { return CommandMode.formation; }
    /** The stance name TroopMotion poses the living troop in ("" when none): "rally" while OrderFeedback holds its weapon up. */
    public static String stance(int entityId) { return TroopMotion.stance(entityId, false).name(); }
    /** Server thread: forgets the last horn/shout per commander, so the next order of any kind plays its cue (the 8 s throttle has not run out). */
    public static void clearThrottle() { BattleEffects.lastFeedback.clear(); }
    /** Server thread: seats the host's squad and every other human squad on the host's team (a lobby joiner is unseated, and on the emptier side), returns the first other human's id or null. */
    public static java.util.UUID seatMate(GravesAndGlory.Battle b, java.util.UUID host) {
        var hs = b.squads.get(host); hs.seated = true;
        java.util.UUID mate = null;
        for (var s : b.squads.values())
            if (!s.owner.equals(host) && !s.bot && b.server.getPlayerManager().getPlayer(s.owner) != null) { s.team = hs.team; s.seated = true; if (mate == null) mate = s.owner; }
        return mate;
    }

    /** Starts the round for every seated squad and skips the intro hold. */
    public static void start(GravesAndGlory.Battle b) { b.startRound(); b.campaign.introUntil = b.time; }

    /** Stands the named group's living troops in a loose block at x,z (for the rally-pose picture: they are close enough to read). */
    public static void bring(GravesAndGlory.Battle b, java.util.UUID owner, String group, double x, double z, double y) {
        int n = 0;
        for (int id : OrderRules.unitIds(b, b.squads.get(owner), group)) {
            var e = b.server.getPlayerManager().getPlayer(owner).getServerWorld().getEntityById(id);
            if (e == null) continue;
            e.refreshPositionAndAngles(x + (n % 4) * 1.4, y, z + (n / 4) * 1.4, 0, 0); n++;
        }
    }

    /** Moves the teammate beside the host so the two avatars do not share one spot (the teammate's picture was taken inside the host's head). */
    public static void placeMate(GravesAndGlory.Battle b, java.util.UUID mate, java.util.UUID host, double dx, double dz) {
        var m = b.server.getPlayerManager().getPlayer(mate); var h = b.server.getPlayerManager().getPlayer(host);
        if (m != null && h != null) m.requestTeleport(h.getX() + dx, h.getY(), h.getZ() + dz);
    }
}
