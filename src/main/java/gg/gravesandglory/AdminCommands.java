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
    private static AdminOg.Store store;
    /** admin.json as the server sees it (cached by modification time). */
    static synchronized AdminOg.Store admin() {
        if (store == null) store = new AdminOg.Store(adminFile());
        return store;
    }
    /** The ledger, read once per server run; a damaged file is read again once it changes, and the stamps taken meanwhile are kept. */
    static synchronized JoinLedger ledger() {
        if (ledger == null) ledger = JoinLedger.load(joinsFile());
        else if (ledger.broken) ledger = JoinLedger.reload(ledger, joinsFile()); // a repaired file takes the stamps kept meanwhile and saves them at once
        return ledger;
    }
    private static synchronized void saveLedger() {
        try { ledger().save(joinsFile()); }
        catch (IOException | RuntimeException e) {
            if (JoinLedger.limited("joins-save", System.currentTimeMillis())) System.err.println("[Graves and Glory] ERROR: joins.json could not be saved (stamps are kept in memory only): " + e.getMessage());
        }
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
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { synchronized (AdminCommands.class) { ledger = null; store = null; } });
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

    /** The gate (AdminOg.allowed): the console is a source with no entity whose output is the server itself; anyone else must be an owner in admin.json. */
    static boolean allowed(ServerCommandSource s) {
        boolean outputIsServer = ((gg.gravesandglory.mixin.ServerCommandSourceAccessor) s).gravesandgloryOutput() instanceof net.minecraft.server.MinecraftServer;
        ServerPlayerEntity p = s.getPlayer();
        return AdminOg.allowed(admin().get(), outputIsServer, s.getEntity() != null, p == null ? null : p.getUuid());
    }

    static int fail(ServerCommandSource s, String text) { s.sendError(Text.literal(text)); return 0; }
    static int say(ServerCommandSource s, String text) { s.sendFeedback(() -> Text.literal(text), false); return 1; }

    static int setWindow(ServerCommandSource s, String range) {
        String[] parts = range.trim().split("\\s+");
        Instant a = parts.length == 2 ? AdminOg.parseBound(parts[0], false) : null, b = parts.length == 2 ? AdminOg.parseBound(parts[1], true) : null;
        if (a == null || b == null) return fail(s, "Usage: /gg og window <start> <end>, each an ISO-8601 instant (2026-12-01T00:00:00Z) or a date (2026-12-01: a start date is 00:00:00 UTC, an end date is the end of that UTC day).");
        if (b.isBefore(a)) return fail(s, "The end is before the start.");
        var c = admin().copy();
        if (c.ogWindow == null) c.ogWindow = new AdminOg.Window(); // keep the window object: its unknown keys survive
        c.ogWindow.start = a.toString(); c.ogWindow.end = b.toString();
        return store(s, c, "OG window set: " + a + " to " + b + " (UTC, both inclusive).");
    }

    static int clearWindow(ServerCommandSource s) {
        var c = admin().copy();
        c.ogWindow = null;
        return store(s, c, "OG window cleared: only players added by hand are OG.");
    }

    static int store(ServerCommandSource s, AdminOg.Config c, String done) {
        try { admin().save(c); return say(s, done); }
        catch (IOException | RuntimeException e) { return fail(s, "admin.json was not changed: " + e.getMessage()); }
    }

    /** An online player, or a name or uuid the join ledger has seen; null when none (the user cache is not asked: it can block on Mojang and accepts accounts that never joined). */
    static UUID resolve(ServerCommandSource s, String name) {
        var online = s.getServer().getPlayerManager().getPlayer(name);
        if (online != null) return online.getUuid();
        UUID id = ledger().byName(name);
        if (id != null) return id;
        try { UUID u = UUID.fromString(name); if (ledger().get(u) != null) return u; } catch (IllegalArgumentException ignored) {}
        return null;
    }

    static String nameOf(UUID id) { var e = ledger().get(id); return e == null || e.lastName.isEmpty() ? id.toString() : e.lastName + " (" + id + ")"; }

    static int edit(CommandContext<ServerCommandSource> c, boolean add) {
        var s = c.getSource(); String name = StringArgumentType.getString(c, "player");
        UUID id = resolve(s, name);
        if (id == null) return fail(s, "No player named " + name + ": the server has not seen them.");
        var cfg = admin().copy();
        if (add) cfg.add(id); else cfg.remove(id);
        return store(s, cfg, nameOf(id) + (add ? " is now OG by hand." : " is now removed from OG by hand."));
    }

    static int list(ServerCommandSource s) {
        var cfg = admin().get();
        warnDamaged(s);
        var w = AdminOg.window(cfg);
        say(s, "OG window: " + (w == null ? "none" : w[0] + " to " + w[1]));
        say(s, "Added by hand (" + cfg.ogAdded.size() + "): " + names(cfg.ogAdded));
        say(s, "Removed by hand (" + cfg.ogRemoved.size() + "): " + names(cfg.ogRemoved));
        long byDate = ledger().all().entrySet().stream().filter(e -> AdminOg.isOg(cfg, UUID.fromString(e.getKey()), e.getValue().firstMatchAt)).count();
        return say(s, "OG in total: " + byDate + " of " + ledger().all().size() + " players in the join ledger.");
    }

    static void warnDamaged(ServerCommandSource s) {
        String p = admin().problem();
        if (p != null) s.sendError(Text.literal("admin.json is damaged (" + p + "): it reads as empty and commands will not change it. A copy is kept as admin.json.bad."));
        if (ledger().broken) s.sendError(Text.literal("joins.json is damaged (" + ledger().brokenReason + "): stamps are not being saved."));
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
        var cfg = admin().get(); warnDamaged(s); var e = ledger().get(id);
        long seen = e == null ? 0 : e.firstSeenAt, match = e == null ? 0 : e.firstMatchAt;
        say(s, nameOf(id) + ": first seen " + stamp(seen) + ", first match " + stamp(match));
        return say(s, AdminOg.reason(cfg, id, match));
    }
}
