package gg.gravesandglory;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
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
 * (build.gradle -PplainProbe). /plainprobe state|writs|practice|wagon|blocks|recruit|keyorder|slot|view|aim|overview and, for P4,
 * open|inv|relic|squad, op 2, for the
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
                .executes(c -> { var p = EntityArgumentType.getPlayer(c, "player"); var w = new WagonEntity(p.getServerWorld()); w.refreshPositionAndAngles(p.getX() + 6, p.getY(), p.getZ() - 4, 0, 0);
                    p.getServerWorld().spawnEntity(w);
                    for (var v : p.getServer().getPlayerManager().getPlayerList()) log("wagon " + w.getId() + " for " + v.getName().getString() + ": sent as " + Registries.ENTITY_TYPE.getId(PlainLooks.wagon(v)));
                    return 1; })))
            .then(CommandManager.literal("blocks").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { var p = EntityArgumentType.getPlayer(c, "player"); var at = p.getBlockPos().east(3); // at the feet, 3 east: in view of a player facing east
                    p.getServerWorld().setBlockState(at.north(2), MapBlocks.TROOP_FENCE.getDefaultState());
                    for (var w : List.of(at, at.south(), at.east(), at.east().south())) p.getServerWorld().setBlockState(w, MapBlocks.STILL_WATER.getDefaultState());
                    log("test blocks at " + at.north(2).toShortString() + " (troop_fence) and a 2x2 of still_water from " + at.toShortString());
                    for (var v : p.getServer().getPlayerManager().getPlayerList()) log("blocks for " + v.getName().getString() + ": " + (PlainLooks.realBlocks(v) ? "real" : "barrier"));
                    return 1; })))
            .then(CommandManager.literal("mage").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> mage(EntityArgumentType.getPlayer(c, "player")))))
            .then(CommandManager.literal("recruit").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { PlainRecruit.open(EntityArgumentType.getPlayer(c, "player"), null); return 1; })))
            .then(CommandManager.literal("keyorder").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("action", StringArgumentType.word())
                .executes(c -> { PlainClients.order(EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "action")); return 1; }))))
            .then(CommandManager.literal("slot").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("n", IntegerArgumentType.integer(0, 8))
                .executes(c -> { var p = EntityArgumentType.getPlayer(c, "player"); int n = IntegerArgumentType.getInteger(c, "n"); p.getInventory().selectedSlot = n; p.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(n)); return 1; }))))
            .then(CommandManager.literal("view").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("entity", StringArgumentType.word())
                .executes(c -> view(EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "entity"), 6))
                .then(CommandManager.argument("dist", FloatArgumentType.floatArg(2, 40))
                    .executes(c -> view(EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "entity"), FloatArgumentType.getFloat(c, "dist")))))))
            .then(CommandManager.literal("aim").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("dist", FloatArgumentType.floatArg(1, 600))
                .executes(c -> aim(EntityArgumentType.getPlayer(c, "player"), FloatArgumentType.getFloat(c, "dist"))))))
            .then(CommandManager.literal("overview").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> overview(EntityArgumentType.getPlayer(c, "player")))))
            .then(CommandManager.literal("open").then(CommandManager.argument("player", EntityArgumentType.player()).then(CommandManager.argument("page", StringArgumentType.word())
                .executes(c -> open(EntityArgumentType.getPlayer(c, "player"), StringArgumentType.getString(c, "page"))))))
            .then(CommandManager.literal("inv").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { String st = inv(EntityArgumentType.getPlayer(c, "player")); c.getSource().sendFeedback(() -> Text.literal(st), false); return 1; })))
            .then(CommandManager.literal("relic").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> relic(EntityArgumentType.getPlayer(c, "player")))))
            .then(CommandManager.literal("squad").then(CommandManager.argument("player", EntityArgumentType.player())
                .executes(c -> { String st = squad(EntityArgumentType.getPlayer(c, "player")); c.getSource().sendFeedback(() -> Text.literal(st), false); return 1; })))));
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

    /** P4: opens a plain War Table page by name. */
    static int open(ServerPlayerEntity p, String page) {
        switch (page) {
            case "wartable" -> PlainWarTable.open(p);
            case "recruit" -> PlainRecruit.open(p, null);
            case "banner" -> PlainBanner.open(p);
            case "contracts" -> PlainContracts.open(p);
            case "chapter" -> PlainChapter.open(p);
            case "friends" -> PlainFriends.open(p);
            case "field" -> PlainField.open(p);
            case "results" -> PlainResults.open(p);
            case "gear" -> PlainGear.open(p);
            case "armoury" -> PlainArmoury.open(p);
            default -> { log("open: no page " + page); return 0; }
        }
        log("open " + page + " for " + p.getName().getString() + ": " + p.currentScreenHandler.getClass().getSimpleName() + " sync " + p.currentScreenHandler.syncId);
        return 1;
    }

    /** P4 dupe checks: the player's inventory (every non-empty slot as index:item*count[:plain kind]), the cursor, the open menu and
     *  whether every stack in it is a vanilla item, and item entities within 16 blocks. The harness compares it before and after each
     *  exploit packet. */
    static String inv(ServerPlayerEntity p) {
        StringBuilder sb = new StringBuilder("inv");
        var in = p.getInventory();
        for (int i = 0; i < in.size(); i++) {
            var st = in.getStack(i);
            if (st.isEmpty()) continue;
            String kind = PlainClients.kindOf(st);
            sb.append(' ').append(i).append(':').append(Registries.ITEM.getId(st.getItem())).append('*').append(st.getCount()).append(kind == null ? "" : ":" + kind);
        }
        var h = p.currentScreenHandler;
        sb.append(" cursor=").append(h.getCursorStack().isEmpty() ? "empty" : Registries.ITEM.getId(h.getCursorStack().getItem()) + "*" + h.getCursorStack().getCount());
        sb.append(" screen=").append(h.getClass().getSimpleName()).append('#').append(h.syncId);
        if (h instanceof PlainMenu m) {
            int modded = 0, filled = 0;
            for (int i = 0; i < m.size; i++) {
                var st = m.inv.getStack(i);
                if (st.isEmpty()) continue;
                filled++;
                if (!Registries.ITEM.getId(st.getItem()).getNamespace().equals("minecraft")) modded++;
            }
            int pane = -1, button = -1;
            for (int i = 0; i < m.size; i++) { boolean isPane = m.inv.getStack(i).isOf(net.minecraft.item.Items.GRAY_STAINED_GLASS_PANE); if (isPane && pane < 0) pane = i; if (!isPane && !m.inv.getStack(i).isEmpty() && button < 0) button = i; }
            sb.append(" menu=").append(m.size).append(" filled=").append(filled).append(" moditems=").append(modded).append(" pane=").append(pane).append(" button=").append(button);
        }
        int drops = p.getServerWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class, p.getBoundingBox().expand(16), e -> true).size();
        sb.append(" drops=").append(drops);
        log(sb.toString());
        return sb.toString();
    }

    /** P4: puts the first relic this build has behaviour for in the player's relic slot 1 (test only), so a practice battle seats it. */
    static int relic(ServerPlayerEntity p) {
        var pr = GravesAndGlory.battle.campaign.profile(p.getUuid());
        for (var r : Spec.RELICS) {
            var t = Commander.tuning(r.id());
            if (t == null || RelicEffects.ACTIVES.get(t.effect()) == null) continue;
            pr.relics.add(r.id());
            String why = Gear.setRelic(pr, 0, r.id());
            log("test relic for " + p.getName().getString() + ": " + r.id() + " in slot 1" + (why == null ? "" : " refused: " + why) + " (test only)");
            return 1;
        }
        log("relic: none with behaviour");
        return 0;
    }

    /** P4: a plain player's battle squad: the kept shape, each group's assignment (order/formation) and each relic slot's clock. */
    static String squad(ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        var s = b == null ? null : b.squads.get(p.getUuid());
        if (s == null) { log("squad: none"); return "squad none"; }
        StringBuilder sb = new StringBuilder("squad shape=" + PlainClients.SHAPE.get(p.getUuid()) + " formation=" + s.formation + " order=" + s.order + " tactics=" + PlainClients.ON_TACTICS.contains(p.getUuid()) + " assign=");
        for (var e : s.assignments.entrySet()) sb.append(e.getKey()).append('/').append(e.getValue().order).append('/').append(e.getValue().formation).append(',');
        sb.append(" relics=");
        for (int i = 0; i < s.relicState.size(); i++) sb.append(i).append(':').append(Commander.relicIn(s, i)).append(":ready").append(Commander.readyIn(b, s, i)).append(":active").append(Commander.activeFor(b, s, i)).append(',');
        log(sb.toString());
        return sb.toString();
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
        double y = Math.max(m.y, b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z))) + 1.5 + Math.max(0, dist - 6) / 2; // farther views look down over the front rank
        face(p, x, y, z, m.add(0, 1.2, 0));
        log("view " + what + ": entity " + best.mob.getId() + " troop=" + best.spec.id() + " server=" + Registries.ENTITY_TYPE.getId(best.mob.getType()) + " (raw " + Registries.ENTITY_TYPE.getRawId(best.mob.getType()) + ", class " + best.mob.getClass().getSimpleName() + ") plain look=" + Registries.ENTITY_TYPE.getId(PlainLooks.byRole(best.spec, best.mob.getType()))
            + " (raw " + Registries.ENTITY_TYPE.getRawId(PlainLooks.byRole(best.spec, best.mob.getType())) + ") look(viewer)=" + Registries.ENTITY_TYPE.getId(PlainLooks.look(best.mob, p)) + " named=" + (best.mob.hasCustomName() ? ArmyMobs.named(best.mob.getCustomName().getString()) != null : false));
        return 1;
    }

    /** A lone mage troop (ArmyHusk named as the mage row, AI off, so no combat, shield or spell) 3.5 blocks ahead, the player facing it. */
    static int mage(ServerPlayerEntity p) {
        Spec.TroopsRow row = null;
        for (var t : Spec.TROOPS) if (t.id().equals("mage")) row = t;
        if (row == null) { log("mage: no mage row"); return 0; }
        var w = p.getServerWorld();
        double r = Math.toRadians(p.getYaw()), x = p.getX() - Math.sin(r) * 3.5, z = p.getZ() + Math.cos(r) * 3.5;
        var m = new GravesAndGlory.ArmyHusk(w);
        m.refreshPositionAndAngles(x, p.getY(), z, p.getYaw() + 180, 0);
        m.setHeadYaw(p.getYaw() + 180);
        m.setCustomName(Text.literal(row.name()));
        m.setAiDisabled(true);
        m.setSilent(true);
        w.spawnEntity(m);
        face(p, p.getX(), p.getY(), p.getZ(), m.getPos().add(0, 1.2, 0));
        for (var v : p.getServer().getPlayerManager().getPlayerList())
            log("mage " + m.getId() + " named " + row.name() + " for " + v.getName().getString() + ": sent as " + Registries.ENTITY_TYPE.getId(PlainLooks.look(m, v)) + " (raw " + Registries.ENTITY_TYPE.getRawId(PlainLooks.look(m, v)) + ")");
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
