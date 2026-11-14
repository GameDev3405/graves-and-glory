package gg.gravesandglory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Admin tools phase A: the /gg root and /gg og (the OG window, hand adds and removes, list, check), and the join ledger the server keeps (JoinLedger). Everything is server
 * side and nothing is sent to a client. Who may run /gg og is AdminOg.mayAdmin (the console, or a uuid in admin.json's owners), never the vanilla permission level: BuildMode
 * ops every player on the builder server.
 */
public final class AdminCommands {
    private AdminCommands() {}

    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("gravesandglory"); }
    static Path adminFile() { return dir().resolve("admin.json"); }
    static Path joinsFile() { return dir().resolve("joins.json"); }

    private static JoinLedger ledger;
    /** The ledger, read once per server run. */
    static synchronized JoinLedger ledger() {
        if (ledger == null) ledger = JoinLedger.load(joinsFile());
        return ledger;
    }
    private static synchronized void saveLedger() {
        try { ledger().save(joinsFile()); }
        catch (IOException | RuntimeException e) { System.err.println("[Graves and Glory] joins.json could not be saved: " + e); }
    }

    /** A login: stamps firstSeenAt once and keeps the name current. Never throws. */
    static void seen(ServerPlayerEntity p) {
        try { var l = ledger(); boolean stamped = l.seen(p.getUuid(), p.getName().getString(), System.currentTimeMillis()); if (stamped || l.dirty) saveLedger(); }
        catch (RuntimeException e) { System.err.println("[Graves and Glory] join stamp failed: " + e); }
    }
    /** A finished match (JoinLedger.finished decided): stamps firstMatchAt once. Never throws. */
    static void matched(UUID id, String name) {
        try { var l = ledger(); boolean stamped = l.matched(id, name, System.currentTimeMillis()); if (stamped || l.dirty) saveLedger(); }
        catch (RuntimeException e) { System.err.println("[Graves and Glory] match stamp failed: " + e); }
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { synchronized (AdminCommands.class) { ledger = null; } });
        CommandRegistrationCallback.EVENT.register((d, access, env) -> d.register(CommandManager.literal("gg").requires(AdminCommands::allowed)
            .then(CommandManager.literal("og")
                .then(CommandManager.literal("window")
                    .then(CommandManager.literal("clear").executes(c -> clearWindow(c.getSource())))
                    .then(CommandManager.argument("range", StringArgumentType.greedyString()).executes(c -> setWindow(c.getSource(), StringArgumentType.getString(c, "range")))))
                .then(CommandManager.literal("add").then(CommandManager.argument("player", StringArgumentType.word()).executes(c -> edit(c, true))))
                .then(CommandManager.literal("remove").then(CommandManager.argument("player", StringArgumentType.word()).executes(c -> edit(c, false))))
                .then(CommandManager.literal("list").executes(c -> list(c.getSource())))
                .then(CommandManager.literal("check").then(CommandManager.argument("player", StringArgumentType.word()).executes(c -> check(c)))))));
    }

    /** True for the server console: no entity and the console's name, "Server" (rcon is "Rcon", a command block "@" unless an op renames it). */
    static boolean console(ServerCommandSource s) { return s.getEntity() == null && "Server".equals(s.getName()); }

    static boolean allowed(ServerCommandSource s) {
        ServerPlayerEntity p = s.getPlayer();
        return AdminOg.mayAdmin(AdminOg.load(adminFile()), console(s), p == null ? null : p.getUuid());
    }

    static int fail(ServerCommandSource s, String text) { s.sendError(Text.literal(text)); return 0; }
    static int say(ServerCommandSource s, String text) { s.sendFeedback(() -> Text.literal(text), false); return 1; }

    static int setWindow(ServerCommandSource s, String range) {
        String[] parts = range.trim().split("\\s+");
        Instant a = parts.length == 2 ? AdminOg.parseBound(parts[0], false) : null, b = parts.length == 2 ? AdminOg.parseBound(parts[1], true) : null;
        if (a == null || b == null) return fail(s, "Usage: /gg og window <start> <end>, each an ISO-8601 instant (2026-12-01T00:00:00Z) or a date (2026-12-01).");
        if (b.isBefore(a)) return fail(s, "The end is before the start.");
        var c = AdminOg.load(adminFile());
        c.ogWindow = new AdminOg.Window(); c.ogWindow.start = a.toString(); c.ogWindow.end = b.toString();
        return store(s, c, "OG window set: " + a + " to " + b + " (inclusive).");
    }

    static int clearWindow(ServerCommandSource s) {
        var c = AdminOg.load(adminFile());
        c.ogWindow = null;
        return store(s, c, "OG window cleared: only players added by hand are OG.");
    }

    static int store(ServerCommandSource s, AdminOg.Config c, String done) {
        try { AdminOg.save(adminFile(), c); return say(s, done); }
        catch (IOException | RuntimeException e) { return fail(s, "admin.json could not be saved: " + e.getMessage()); }
    }

    /** An online player, a name the ledger has seen, or a name the server's user cache knows; null when none. A bare uuid is accepted when the ledger knows it. */
    static UUID resolve(ServerCommandSource s, String name) {
        var online = s.getServer().getPlayerManager().getPlayer(name);
        if (online != null) return online.getUuid();
        UUID id = ledger().byName(name);
        if (id != null) return id;
        try { var u = s.getServer().getUserCache().findByName(name); if (u.isPresent()) return u.get().getId(); } catch (RuntimeException ignored) {}
        try { UUID u = UUID.fromString(name); if (ledger().get(u) != null) return u; } catch (IllegalArgumentException ignored) {}
        return null;
    }

    static String nameOf(UUID id) { var e = ledger().get(id); return e == null || e.lastName.isEmpty() ? id.toString() : e.lastName + " (" + id + ")"; }

    static int edit(CommandContext<ServerCommandSource> c, boolean add) {
        var s = c.getSource(); String name = StringArgumentType.getString(c, "player");
        UUID id = resolve(s, name);
        if (id == null) return fail(s, "No player named " + name + ": the server has not seen them.");
        var cfg = AdminOg.load(adminFile());
        if (add) cfg.add(id); else cfg.remove(id);
        return store(s, cfg, nameOf(id) + (add ? " is now OG by hand." : " is now removed from OG by hand."));
    }

    static int list(ServerCommandSource s) {
        var cfg = AdminOg.load(adminFile());
        var w = AdminOg.window(cfg);
        say(s, "OG window: " + (w == null ? "none" : w[0] + " to " + w[1]));
        say(s, "Added by hand (" + cfg.ogAdded.size() + "): " + names(cfg.ogAdded));
        say(s, "Removed by hand (" + cfg.ogRemoved.size() + "): " + names(cfg.ogRemoved));
        long byDate = ledger().all().entrySet().stream().filter(e -> AdminOg.isOg(cfg, UUID.fromString(e.getKey()), e.getValue().firstMatchAt)).count();
        return say(s, "OG in total: " + byDate + " of " + ledger().all().size() + " players in the join ledger.");
    }

    static String names(List<String> ids) {
        if (ids.isEmpty()) return "none";
        StringJoiner j = new StringJoiner(", ");
        for (String id : ids) j.add(nameOf(UUID.fromString(id)));
        return j.toString();
    }

    static String stamp(long at) { return at <= 0 ? "not yet" : Instant.ofEpochMilli(at).toString(); }

    static int check(CommandContext<ServerCommandSource> c) {
        var s = c.getSource(); String name = StringArgumentType.getString(c, "player");
        UUID id = resolve(s, name);
        if (id == null) return fail(s, "No player named " + name + ": the server has not seen them.");
        var cfg = AdminOg.load(adminFile()); var e = ledger().get(id);
        long seen = e == null ? 0 : e.firstSeenAt, match = e == null ? 0 : e.firstMatchAt;
        say(s, nameOf(id) + ": first seen " + stamp(seen) + ", first match " + stamp(match));
        return say(s, AdminOg.reason(cfg, id, match));
    }
}
