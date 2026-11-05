package gg.gravesandglory;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;

/**
 * Test-only reach into the package-private order state for cmdui.OrderIntakeSuite (command UI plan Task 3): it sends real tactical_v1
 * packets through CommandMode.issue and reads back what the server stored. Never included in releases.
 */
public final class OrderIntakeProbe {
    private OrderIntakeProbe() {}

    /** A group's stored order as the server holds it (null fields mirror a null chargeDir). */
    public record Snap(String order, String formation, double tx, double tz, float yaw, boolean hunt, Vec3d chargeDir, boolean thenFollow, long issued) {}

    /** Sends one single-group order from this client through CommandMode.issue (the command keys' sender), no aimed enemy. */
    public static void send(String group, String action, String formation, double x, double z, float yaw) {
        CommandMode.issue(group, action, formation, new Vec3d(x, 65, z), yaw, -1);
    }

    /** One group of a multi-group order and its own point (a Charge's start, a Formation's anchor). */
    public record At(String group, double x, double z) {}

    /** The multi-group order packet (tactical_v2), written by hand as an old or a harness client would: action, formation, yaw, target, then
     *  n x (group, x, z). Returns whether the channel was open (false while the server has no tactical_v2). */
    public static boolean sendMany(String action, String formation, float yaw, java.util.List<At> groups) {
        var id = new Identifier("gravesandglory", "tactical_v2");
        if (!ClientPlayNetworking.canSend(id)) return false;
        var b = PacketByteBufs.create(); b.writeString(action); b.writeString(formation); b.writeFloat(yaw); b.writeInt(-1);
        b.writeInt(groups.size()); for (var g : groups) { b.writeString(g.group()); b.writeDouble(g.x()); b.writeDouble(g.z()); }
        ClientPlayNetworking.send(id, b);
        return true;
    }

    /** An old client's single-group tactical_v1 packet, written by hand (the command keys no longer send it). */
    public static boolean sendV1(String group, String action, String formation, double x, double z, float yaw) {
        var id = new Identifier("gravesandglory", "tactical_v1");
        if (!ClientPlayNetworking.canSend(id)) return false;
        var b = PacketByteBufs.create(); b.writeString(group); b.writeString(action); b.writeString(formation); b.writeDouble(x); b.writeDouble(z); b.writeFloat(yaw); b.writeInt(-1);
        ClientPlayNetworking.send(id, b);
        return true;
    }

    /** Order packets this client has sent through CommandMode (any version). */
    public static int ordersSent() { return CommandMode.ordersSent; }

    /** The group's stored Assignment (null when it has none of its own). */
    public static Snap snap(GravesAndGlory.Battle b, UUID owner, String group) {
        var s = b.squads.get(owner);
        var a = s == null ? null : s.assignments.get(group);
        return a == null ? null : new Snap(a.order, a.formation, a.target.x, a.target.z, a.yaw, a.hunt, a.chargeDir, a.thenFollow, a.issued);
    }

    public static boolean inArena(double x, double z) { return GravesAndGlory.Battle.inArena(x, z); }
    public static boolean wet(double x, double z) { return GravesAndGlory.Battle.wet(x, z); }
    public static long time(GravesAndGlory.Battle b) { return b.time; }
    public static long chargeReadyAt(GravesAndGlory.Battle b, UUID owner) { return b.squads.get(owner).chargeReadyAt; }
    public static void resetCharge(GravesAndGlory.Battle b, UUID owner) { b.squads.get(owner).chargeReadyAt = 0; }
    public static int team(GravesAndGlory.Battle b, UUID owner) { return b.squads.get(owner).team; }
    /** The named group's living troops' centre (the commander's position if none). */
    public static Vec3d centre(GravesAndGlory.Battle b, UUID owner, String group, Vec3d fallback) { return OrderRules.centre(b, b.squads.get(owner), group, fallback); }
    public static int living(GravesAndGlory.Battle b, UUID owner, String group) { return OrderRules.unitIds(b, b.squads.get(owner), group).length; }

    /** Puts the owner in (or takes him out of) the respawn wait, without the tick's respawn handling. */
    public static void respawning(GravesAndGlory.Battle b, UUID owner, boolean on) {
        if (on) b.respawns.put(owner, b.time + 100_000); else b.respawns.remove(owner);
    }

    /** A synthetic, troopless, non-bot squad on the team (the testOwners pattern): an OUTLINE recipient with no connection. */
    public static UUID addSynthetic(GravesAndGlory.Battle b, int team) {
        var s = new GravesAndGlory.Squad(UUID.randomUUID(), team, b.fieldBase(team));
        s.order = "hold";
        b.testOwners.add(s.owner);
        b.squads.put(s.owner, s);
        return s.owner;
    }
    public static void removeSynthetic(GravesAndGlory.Battle b, UUID owner) { b.squads.remove(owner); b.testOwners.remove(owner); }
    /** The order ids OrderRules maps (compat), for an in-game check that it delegates to BattleMath. */
    public static String loadOrder(String id) { return OrderRules.loadOrder(id); }
    public static String loadFormation(String id) { return OrderRules.loadFormation(id); }
}
