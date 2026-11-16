package gg.gravesandglory;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Test-only reach into the order answer state for cmdui.ReplySuite (army control polish, spec section 5). Never included in releases. */
public final class ReplyProbe {
    private ReplyProbe() {}

    public static void switchMap(GravesAndGlory.Battle b, int n) { if (b.campaign.map != n) b.campaign.switchMap(n); }
    public static boolean onMap(GravesAndGlory.Battle b, int n) { return b != null && b.ready && b.campaign.map == n && CampaignMaps.selected == n; }

    /** Lets the next order past the command cooldown. */
    public static void clearCooldown(GravesAndGlory.Battle b, UUID owner) { b.squads.get(owner).lastOrder = b.time - 1000; }
    public static void intro(GravesAndGlory.Battle b, int ticks) { b.campaign.introUntil = b.time + ticks; }
    public static void down(GravesAndGlory.Battle b, UUID owner, int ticks) { if (ticks > 0) b.respawns.put(owner, b.time + ticks); else b.respawns.remove(owner); }

    /** A tactical_v2 order from this owner as the server receives it: one group at (x, z), with an aimed enemy id (-1 none). */
    public static void order(GravesAndGlory.Battle b, UUID owner, String group, String action, double x, double z, int enemy) {
        var p = b.server.getPlayerManager().getPlayer(owner);
        OrderRules.playerOrder(b, p, List.of(new OrderRules.At(group, x, z)), action, "block", 0f, enemy);
    }

    /** Every stored order of the owner's groups, as "group=order@x,z". */
    public static List<String> stored(GravesAndGlory.Battle b, UUID owner) {
        List<String> out = new ArrayList<>();
        for (var e : b.squads.get(owner).assignments.entrySet()) out.add(e.getKey() + "=" + e.getValue().order + "@" + (e.getValue().target == null ? "-" : String.format("%.2f,%.2f", e.getValue().target.x, e.getValue().target.z)));
        return out;
    }
    public static Vec3d target(GravesAndGlory.Battle b, UUID owner, String group) { var a = b.squads.get(owner).assignments.get(group); return a == null ? null : a.target; }
    public static String orderOf(GravesAndGlory.Battle b, UUID owner, String group) { var a = b.squads.get(owner).assignments.get(group); return a == null ? null : a.order; }
    public static long chargeReadyAt(GravesAndGlory.Battle b, UUID owner) { return b.squads.get(owner).chargeReadyAt; }
    public static long time(GravesAndGlory.Battle b) { return b.time; }
    public static int living(GravesAndGlory.Battle b, UUID owner, String group) { return OrderRules.unitIds(b, b.squads.get(owner), group).length; }

    /** The deep-water column inside the arena nearest (x, z) whose order point moves to dry ground, as {x, z}; null when none within 200. */
    public static int[] deepNear(GravesAndGlory.Battle b, double x, double z) {
        int[] best = null; double bd = Double.MAX_VALUE;
        for (int dx = -200; dx <= 200; dx += 2) for (int dz = -200; dz <= 200; dz += 2) {
            int cx = (int) x + dx, cz = (int) z + dz;
            double d = dx * dx + dz * dz;
            if (d >= bd || !GravesAndGlory.Battle.inArena(cx, cz) || !wide(cx, cz)) continue;
            double[] dry = Arena.orderGround(b.world, cx + .5, cz + .5, x);
            if (dry == null || !GravesAndGlory.Battle.inArena(dry[0], dry[1])) continue;
            best = new int[]{cx, cz}; bd = d;
        }
        return best;
    }
    /** Deep water across the 5 x 5 columns around (x, z): a river or a lake, not one wet cell in a yard. */
    static boolean wide(int x, int z) { for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) if (!Arena.deep(x + dx, z + dz)) return false; return true; }
    public static boolean deep(double x, double z) { return Arena.deep((int) Math.floor(x), (int) Math.floor(z)); }
    /** The client's own answer to a point in water (the same helper the server asks), from the client's world. */
    public static double[] clientGround(net.minecraft.world.BlockView w, double x, double z, double towardX) { return Arena.orderGround(w, x, z, towardX); }

    /** An enemy troop that is alive but out of the owner's sight (MapBlockRules.canSee), as its entity id; -1 when every one is seen. */
    public static int unseenEnemy(GravesAndGlory.Battle b, UUID owner) {
        var p = b.server.getPlayerManager().getPlayer(owner); int team = b.squads.get(owner).team;
        for (var e : b.world.iterateEntities()) { int t = b.teamOf(e); if (e != p && e.isAlive() && t >= 0 && t != team && !MapBlockRules.canSee(p, e)) return e.getId(); }
        return -1;
    }
    /** The deep (5 x 5) cell farthest from (fromX, fromZ) along the line through (x, z), up to 10 blocks on: an aim that clears the bank. */
    public static int[] deepBeyond(double fromX, double fromZ, int x, int z) {
        double dx = x + .5 - fromX, dz = z + .5 - fromZ, l = Math.sqrt(dx * dx + dz * dz); if (l < 1e-6) return new int[]{x, z};
        int[] best = new int[]{x, z};
        for (int k = 1; k <= 10; k++) { int cx = (int) Math.floor(x + .5 + dx / l * k), cz = (int) Math.floor(z + .5 + dz / l * k);
            if (!GravesAndGlory.Battle.inArena(cx, cz)) break; if (wide(cx, cz)) best = new int[]{cx, cz}; }
        return best;
    }
    /** Puts the owner on the ground at (x, z) (Battle.groundAt). */
    public static void stand(GravesAndGlory.Battle b, UUID owner, double x, double z) {
        var p = b.server.getPlayerManager().getPlayer(owner); var g = GravesAndGlory.Battle.groundAt(x, z, x, z);
        p.requestTeleport(g.x, g.y + .1, g.z);
    }
    /** The action bar text the HUD shows now ("" none). */
    public static String overlayText() {
        try { var f = net.minecraft.client.gui.hud.InGameHud.class.getDeclaredField(net.fabricmc.loader.api.FabricLoader.getInstance().getMappingResolver().mapFieldName("named", "net.minecraft.client.gui.hud.InGameHud", "overlayMessage", "Lnet/minecraft/text/Text;"));
            f.setAccessible(true); var t = (net.minecraft.text.Text) f.get(net.minecraft.client.MinecraftClient.getInstance().inGameHud); return t == null ? "" : t.getString(); }
        catch (Exception e) { return "unreadable: " + e; }
    }
    public static void clearAnswer() { CommandMode.lastAnswer = ""; }
    public static int groundY(int x, int z) { return Arena.footing(x, z); }
    public static int respawnSeconds() { return GravesAndGloryClient.respawn; }
    public static int heldSent() { return CommandMode.heldSent; }
    public static String lastAnswer() { return CommandMode.lastAnswer; }
    public static Vec3d aimed() { return CommandMode.destination; }
    public static boolean aiming() { return CommandMode.active(); }
    public static void confirm() { CommandMode.confirm(); }
}
