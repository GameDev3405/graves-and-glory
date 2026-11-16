package gg.gravesandglory;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

/**
 * UNSHIPPED plain-client test commands (port plan P3; the P2 spike's /plainspike), only with -Dgravesandglory.plainProbe=true
 * (build.gradle -PplainProbe). /plainprobe state|writs|practice|wagon|blocks|recruit|keyorder|slot|view|aim|overview, op 2, for the
 * scripted RCON session (tools/plain_clients). Every line goes to the server log with a "[plain-probe]" prefix.
 */
public final class PlainProbe implements ModInitializer {
    static void log(String s) { System.out.println("[plain-probe] " + s); }

    @Override public void onInitialize() {
        if (!Boolean.getBoolean("gravesandglory.plainProbe")) return;
        CommandRegistrationCallback.EVENT.register((d, access, env) -> d.register(CommandManager.literal("plainprobe").requires(s -> s.hasPermissionLevel(2))
            .then(CommandManager.literal("state").executes(c -> { String st = state(); c.getSource().sendFeedback(() -> Text.literal(st), false); return 1; }))
            .then(CommandManager.literal("writs").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("n", IntegerArgumentType.integer(0, 1000))
                .executes(c -> { var p = EntityArgumentType.getPlayer(c, "player"); int n = IntegerArgumentType.getInteger(c, "n");
                    GravesAndGlory.battle.campaign.profile(p.getUuid()).troopWrits = n; log("test writs for " + p.getName().getString() + " set to " + n + " (test only)"); return 1; }))))
            .then(CommandManager.literal("practice").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { PlainClients.use(EntityArgumentType.getPlayer(c, "player"), "practice"); return 1; })))
            .then(CommandManager.literal("wagon").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { var p = EntityArgumentType.getPlayer(c, "player"); var w = new WagonEntity(p.getServerWorld()); w.refreshPositionAndAngles(p.getX() + 4, p.getY(), p.getZ(), 0, 0);
                    p.getServerWorld().spawnEntity(w);
                    for (var v : p.getServer().getPlayerManager().getPlayerList()) log("wagon " + w.getId() + " for " + v.getName().getString() + ": sent as " + Registries.ENTITY_TYPE.getId(PlainLooks.wagon(v)));
                    return 1; })))
            .then(CommandManager.literal("blocks").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { var p = EntityArgumentType.getPlayer(c, "player"); var at = p.getBlockPos().up(3);
                    p.getServerWorld().setBlockState(at.east(2), MapBlocks.TROOP_FENCE.getDefaultState()); p.getServerWorld().setBlockState(at.west(2), MapBlocks.STILL_WATER.getDefaultState());
                    log("test blocks at " + at.east(2).toShortString() + " (troop_fence) and " + at.west(2).toShortString() + " (still_water)");
                    for (var v : p.getServer().getPlayerManager().getPlayerList()) log("blocks for " + v.getName().getString() + ": " + (PlainLooks.realBlocks(v) ? "real" : "barrier"));
                    return 1; })))
            .then(CommandManager.literal("recruit").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { PlainRecruit.open(EntityArgumentType.getPlayer(c, "player"), null); return 1; })))
            .then(CommandManager.literal("keyorder").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("action", StringArgumentType.word())
                .executes(c -> { PlainClients.order(EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "action")); return 1; }))))
            .then(CommandManager.literal("slot").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("n", IntegerArgumentType.integer(0, 8))
                .executes(c -> { var p = EntityArgumentType.getPlayer(c, "player"); int n = IntegerArgumentType.getInteger(c, "n"); p.getInventory().selectedSlot = n; p.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(n)); return 1; }))))
            .then(CommandManager.literal("view").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("entity", StringArgumentType.word())
                .executes(c -> view(EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "entity"), 6)))))
            .then(CommandManager.literal("aim").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("dist", FloatArgumentType.floatArg(1, 600))
                .executes(c -> aim(EntityArgumentType.getPlayer(c, "player"), FloatArgumentType.getFloat(c, "dist"))))))
            .then(CommandManager.literal("overview").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> overview(EntityArgumentType.getPlayer(c, "player")))))));
    }

    static String state() {
        var b = GravesAndGlory.battle;
        if (b == null) { log("state: no battle"); return "no battle"; }
        Map<String, Integer> types = new TreeMap<>();
        for (var u : b.units) if (u.mob.isAlive()) types.merge(Registries.ENTITY_TYPE.getId(u.mob.getType()).getPath() + "/" + u.spec.id() + "/team" + u.squad.team, 1, Integer::sum);
        StringBuilder sq = new StringBuilder();
        for (var s : b.squads.values()) sq.append(s.owner.toString(), 0, 8).append(s.bot ? "(bot)" : "").append(" team").append(s.team).append(s.seated ? " seated" : "").append(s.bot ? "" : " order=" + s.order + " lastOrder=" + s.lastOrder + " chargeReadyAt=" + s.chargeReadyAt).append("; ");
        StringBuilder flags = new StringBuilder();
        for (var p : b.server.getPlayerManager().getPlayerList()) flags.append(p.getName().getString()).append(VersionGate.plain(p) ? "=plain" : "=modded").append(VersionGate.rawIds(p) ? "/rawIds" : "").append(' ');
        String st = "state ready=" + b.ready + " running=" + b.running + " time=" + b.time + " intro=" + b.campaign.introUntil + " players=" + flags + "squads=" + sq + " units=" + types;
        log(st);
        return st;
    }

    static void face(ServerPlayerEntity p, double x, double y, double z, Vec3d look) {
        double dx = look.x - x, dy = look.y - (y + p.getStandingEyeHeight()), dz = look.z - z;
        float yaw = (float) (MathHelper.atan2(dz, dx) * 180 / Math.PI) - 90;
        float pitch = (float) -(MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
        p.teleport(p.getServerWorld(), x, y, z, yaw, pitch);
        TeleportSync.afterTeleport(p);
    }

    /** Puts the player near the nearest living troop whose server mob type or troop id is the given one, facing it. */
    static int view(ServerPlayerEntity p, String what, double dist) {
        var b = GravesAndGlory.battle;
        if (b == null) return 0;
        GravesAndGlory.Unit best = null;
        for (var u : b.units) {
            if (!u.mob.isAlive()) continue;
            boolean match = Registries.ENTITY_TYPE.getId(u.mob.getType()).getPath().equals(what) || u.spec.id().equals(what);
            if (match && (best == null || u.mob.squaredDistanceTo(p) < best.mob.squaredDistanceTo(p))) best = u;
        }
        if (best == null) { log("view " + what + ": none alive"); return 0; }
        Vec3d m = best.mob.getPos();
        double ang = Math.atan2(p.getZ() - m.z, p.getX() - m.x);
        double x = m.x + Math.cos(ang) * dist, z = m.z + Math.sin(ang) * dist;
        double y = Math.max(m.y, b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z))) + 1.5;
        face(p, x, y, z, m.add(0, 1.2, 0));
        log("view " + what + ": entity " + best.mob.getId() + " troop=" + best.spec.id() + " server=" + Registries.ENTITY_TYPE.getId(best.mob.getType()) + " plain look=" + Registries.ENTITY_TYPE.getId(PlainLooks.byRole(best.spec, best.mob.getType())));
        return 1;
    }

    /** Faces the player at the ground dist blocks ahead along its yaw. */
    static int aim(ServerPlayerEntity p, float dist) {
        double r = Math.toRadians(p.getYaw()), x = p.getX() - Math.sin(r) * dist, z = p.getZ() + Math.cos(r) * dist;
        int top = p.getServerWorld().getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        face(p, p.getX(), p.getY(), p.getZ(), new Vec3d(x, top - 0.5, z));
        log(String.format(Locale.ROOT, "aim %s at ground %.0f,%d,%.0f (%.0f blocks ahead)", p.getName().getString(), x, top, z, dist));
        return 1;
    }

    /** A view over the player's army towards the enemy, for the HUD screenshot. */
    static int overview(ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        if (b == null) return 0;
        var s = b.squads.get(p.getUuid());
        if (s == null) return 0;
        var list = b.fielded(s).toList();
        if (list.isEmpty()) return 0;
        double x = 0, z = 0;
        for (var u : list) { x += u.mob.getX(); z += u.mob.getZ(); }
        x /= list.size(); z /= list.size();
        var base = GravesAndGlory.Battle.base(s.team);
        double ang = Math.atan2(z - base.z, x - base.x);
        double cx = x - Math.cos(ang) * 10, cz = z - Math.sin(ang) * 10;
        double cy = b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(cx), (int) Math.floor(cz)) + 1;
        face(p, cx, cy, cz, new Vec3d(x + Math.cos(ang) * 30, cy - 2, z + Math.sin(ang) * 30));
        return 1;
    }
}
