package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.gravesandglory.net.Payloads;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * The server half of the UI editor (UI editor design 5.4 to 5.7, phase 1: words). Holds the published sheet
 * (config/gravesandglory/ui-edits.json, with its history) and each owner's draft (config/gravesandglory/ui-drafts/(uuid).json, so a
 * draft survives a restart), sends the published sheet to every player on join and on each Publish, and answers the editor.
 *
 * <p>Who may edit: the owners in admin.json (AdminOg.mayAdmin), checked on every request; the editor being open on a client is never
 * the protection. Every edit is validated against the piece list the build generated (ui/pieces.json) before it is saved. A
 * malformed sheet is ignored (players get the built-in edits only), copied to ui-edits.json.bad and never overwritten by Publish;
 * /gg ui reset writes a fresh one (the damaged bytes stay in the .bad copy).
 *
 * <p>Wire (ui_sheet_v1, server to client): a byte kind, then UTF-8 JSON as a byte array. Kind 0: the published sheet (version and
 * edits). Kind 1, owners only: the editor's state (open, draft, published version, history list, a message). ui_edit_v1 (client to
 * server): UTF-8 JSON {op: set|clear|publish|discard|rollback|state|close, id, text, version}.
 */
public final class UiEdits {
    private UiEdits() {}

    public static final byte KIND_SHEET = 0, KIND_EDITOR = 1;
    /** Largest editor request accepted, in bytes. */
    public static final int REQUEST_CAP = 16 * 1024;
    /** Largest sheet or editor state sent, in bytes. */
    public static final int SEND_CAP = 1 << 20;

    static Path dir() { return AdminCommands.dir(); }
    static Path sheetFile() { return dir().resolve("ui-edits.json"); }
    static Path draftFile(UUID id) { return dir().resolve("ui-drafts").resolve(id + ".json"); }

    private static Map<String, UiSheet.Piece> pieces;
    private static Map<String, String> renames;
    private static UiSheet sheet;
    private static String problem;
    private static final Map<UUID, Map<String, String>> DRAFTS = new HashMap<>();

    // ---- resources ----

    static String resource(String name) {
        try (InputStream in = UiEdits.class.getResourceAsStream("/assets/gravesandglory/ui/" + name)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) { return null; }
    }

    /** The pieces this build declares (ui/pieces.json); empty, with a warning, when the file is missing or damaged. */
    public static synchronized Map<String, UiSheet.Piece> pieces() {
        if (pieces == null) {
            String text = resource("pieces.json");
            try { pieces = text == null ? Map.of() : UiSheet.parsePieces(text); }
            catch (RuntimeException e) { pieces = Map.of(); SafeFiles.warn.accept("ui/pieces.json could not be read (" + e.getMessage() + "); the UI editor accepts no edits."); }
        }
        return pieces;
    }

    public static synchronized Map<String, String> renames() {
        if (renames == null) {
            String text = resource("renames.json");
            try { renames = text == null ? Map.of() : UiSheet.parseRenames(text); } catch (RuntimeException e) { renames = Map.of(); }
        }
        return renames;
    }

    /** The mod's built-in edits (ui/builtin-edits.json), renames applied. */
    public static Map<String, String> builtin() {
        String text = resource("builtin-edits.json");
        try { return text == null ? Map.of() : UiSheet.renamed(UiSheet.parse(text).edits, renames()); }
        catch (RuntimeException e) { return Map.of(); }
    }

    // ---- the sheet ----

    static synchronized UiSheet sheet() {
        if (sheet == null) {
            sheet = new UiSheet(); problem = null;
            Path f = sheetFile();
            try {
                if (Files.isRegularFile(f)) {
                    String text = Files.readString(f);
                    if (!text.isBlank()) {
                        try { sheet = UiSheet.parse(text); sheet.edits = UiSheet.renamed(sheet.edits, renames()); }
                        catch (RuntimeException bad) {
                            problem = String.valueOf(bad.getMessage());
                            Path kept = AdminOg.keepBad(f);
                            SafeFiles.warn.accept("ui-edits.json could not be read (" + problem + "); players get the built-in UI edits and the file is not rewritten. A copy is kept as " + (kept == null ? "(could not be copied)" : kept.getFileName()) + ". /gg ui reset starts a fresh sheet.");
                            sheet = new UiSheet();
                        }
                    }
                }
            } catch (IOException e) { problem = "the file could not be opened: " + e; }
        }
        return sheet;
    }

    static void saveSheet() throws IOException { SafeFiles.write(sheetFile(), sheet.toJson(true)); }

    /** An owner's draft: his saved one, else a copy of the published edits. */
    static synchronized Map<String, String> draft(UUID owner) {
        Map<String, String> d = DRAFTS.get(owner);
        if (d != null) return d;
        d = new TreeMap<>(sheet().edits);
        Path f = draftFile(owner);
        try {
            if (Files.isRegularFile(f)) d = new TreeMap<>(UiSheet.renamed(UiSheet.parse(Files.readString(f)).edits, renames()));
        } catch (IOException | RuntimeException bad) {
            AdminOg.keepBad(f);
            SafeFiles.warn.accept("UI draft " + f.getFileName() + " could not be read (" + bad.getMessage() + "); the draft starts again from the published sheet.");
        }
        DRAFTS.put(owner, d);
        return d;
    }

    static void saveDraft(UUID owner, Map<String, String> d) throws IOException {
        UiSheet s = new UiSheet();
        s.version = sheet().version; s.edits = new TreeMap<>(d);
        SafeFiles.write(draftFile(owner), s.toJson(false));
    }

    // ---- wiring ----

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> { synchronized (UiEdits.class) { sheet = null; problem = null; DRAFTS.clear(); } });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sendSheet(handler.player));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.UiEditPayload.ID, (payload, context) -> {
            ServerPlayerEntity p = context.player();
            byte[] body = payload.body();
            if (body.length > REQUEST_CAP) return;
            String json = new String(body, StandardCharsets.UTF_8);
            context.server().execute(() -> request(context.server(), p, json));
        });
    }

    static boolean owner(ServerPlayerEntity p) {
        return p != null && AdminOg.mayAdmin(AdminCommands.admin().get(), false, p.getUuid());
    }

    static void send(ServerPlayerEntity p, byte kind, String json) {
        if (!ServerPlayNetworking.canSend(p, Payloads.UiSheetPayload.ID)) return;
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > SEND_CAP) { SafeFiles.warn.accept("UI sheet too large to send (" + bytes.length + " bytes)"); return; }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeByte(kind);
        buf.writeByteArray(bytes);
        ServerPlayNetworking.send(p, Payloads.UiSheetPayload.of(buf));
    }

    /** The published sheet (players never get the history). */
    static void sendSheet(ServerPlayerEntity p) { send(p, KIND_SHEET, sheet().toJson(false)); }

    static void broadcast(MinecraftServer server) { for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) sendSheet(p); }

    /** Sends an owner the editor's state; open asks his client to show the editor. */
    static void sendState(ServerPlayerEntity p, boolean open, String message) {
        UiSheet s = sheet();
        JsonObject o = new JsonObject();
        o.addProperty("open", open);
        o.addProperty("version", s.version);
        o.addProperty("published", s.summary.isEmpty() ? "" : "version " + s.version + ", " + s.time + ", " + s.who + ": " + s.summary);
        o.add("draft", UiSheet.editsJson(draft(p.getUuid())));
        JsonArray h = new JsonArray();
        for (UiSheet.Entry en : s.history) {
            JsonObject j = new JsonObject();
            j.addProperty("version", en.version); j.addProperty("time", en.time); j.addProperty("who", en.who); j.addProperty("summary", en.summary);
            h.add(j);
        }
        o.add("history", h);
        JsonArray orphans = new JsonArray();
        for (String id : UiSheet.orphans(draft(p.getUuid()), pieces())) orphans.add(id);
        o.add("orphans", orphans);
        o.addProperty("damaged", problem == null ? "" : problem);
        o.addProperty("message", message == null ? "" : message);
        send(p, KIND_EDITOR, UiSheet.COMPACT.toJson(o));
    }

    // ---- requests ----

    static void request(MinecraftServer server, ServerPlayerEntity p, String json) {
        if (!owner(p)) return; // the gate, on every request
        String op = "", id = "", text = null; int version = -1;
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            op = UiSheet.str(o, "op"); id = UiSheet.str(o, "id");
            if (o.has("text") && o.get("text").isJsonPrimitive()) text = o.get("text").getAsString();
            if (o.has("version")) version = o.get("version").getAsInt();
        } catch (RuntimeException bad) { sendState(p, false, "Request not understood."); return; }
        String who = p.getName().getString();
        switch (op) {
            case "state" -> sendState(p, false, null);
            case "set" -> sendState(p, false, set(p.getUuid(), id, text));
            case "clear" -> sendState(p, false, set(p.getUuid(), id, null));
            case "publish" -> sendState(p, false, publish(server, p.getUuid(), who));
            case "discard" -> sendState(p, false, discard(p.getUuid()));
            case "rollback" -> sendState(p, false, rollback(server, version, who));
            default -> sendState(p, false, "Unknown request.");
        }
    }

    /** Sets (text not null) or clears one piece in the owner's draft. Returns the message for the editor. */
    static synchronized String set(UUID owner, String id, String text) {
        UiSheet.Piece piece = pieces().get(id);
        if (piece == null) return "No piece " + id + " in this build.";
        Map<String, String> d = draft(owner);
        if (text != null) {
            String why = UiSheet.problem(piece, text);
            if (why != null) return "Not saved: " + why + ".";
            if (text.equals(piece.def())) d.remove(id); else d.put(id, text);
        } else d.remove(id);
        try { saveDraft(owner, d); } catch (IOException e) { return "Draft kept in memory only; the file could not be written: " + e.getMessage(); }
        Set<String> dropped = text == null ? Set.of() : UiTemplate.dropped(piece.def(), text);
        return dropped.isEmpty() ? "Draft saved." : "Draft saved. No longer shown: " + UiTemplate.braced(dropped) + ".";
    }

    static synchronized String publish(MinecraftServer server, UUID owner, String who) {
        if (problem != null) return "Not published: ui-edits.json is damaged (" + problem + "). /gg ui reset starts a fresh sheet; the damaged one is kept as ui-edits.json.bad.";
        Map<String, String> d = draft(owner);
        UiSheet s = sheet();
        UiSheet before = s.copy();
        String sum;
        try { sum = s.publish(d, pieces(), who, Instant.now()); }
        catch (IllegalArgumentException bad) { return "Not published: " + bad.getMessage(); }
        try { saveSheet(); }
        catch (IOException e) { sheet = before; return "Not published: ui-edits.json could not be written: " + e.getMessage(); }
        dropDraft(owner);
        broadcast(server);
        return "Published version " + s.version + " (" + sum + ").";
    }

    static synchronized String discard(UUID owner) {
        dropDraft(owner);
        return "Draft discarded; back to the published version " + sheet().version + ".";
    }

    static void dropDraft(UUID owner) {
        DRAFTS.remove(owner);
        try { Files.deleteIfExists(draftFile(owner)); } catch (IOException ignored) {}
    }

    static synchronized String rollback(MinecraftServer server, int version, String who) {
        if (problem != null) return "Not rolled back: ui-edits.json is damaged (" + problem + "). /gg ui reset starts a fresh sheet.";
        UiSheet s = sheet();
        UiSheet before = s.copy();
        String sum;
        try { sum = s.rollback(version, who, Instant.now()); } catch (IllegalArgumentException bad) { return bad.getMessage(); }
        try { saveSheet(); } catch (IOException e) { sheet = before; return "Not rolled back: " + e.getMessage(); }
        broadcast(server);
        return "Published version " + s.version + " (" + sum + ").";
    }

    /** /gg ui reset: back to the built-in edits for everyone. Works on a damaged sheet too (its bytes are in the .bad copy). */
    static synchronized String reset(MinecraftServer server, String who) {
        UiSheet s = sheet();
        UiSheet before = s.copy();
        String sum = s.reset(who, Instant.now());
        try { saveSheet(); } catch (IOException e) { sheet = before; return "Not reset: " + e.getMessage(); }
        problem = null;
        broadcast(server);
        return "Published version " + s.version + " (" + sum + "). Players see the mod's built-in UI wording.";
    }

    /** Lines for /gg ui history. */
    static synchronized List<String> history() {
        UiSheet s = sheet();
        List<String> out = new ArrayList<>();
        out.add("Current: version " + s.version + (s.summary.isEmpty() ? "" : " (" + s.time + ", " + s.who + ": " + s.summary + ")") + (problem == null ? "" : " [sheet damaged: " + problem + "]"));
        for (UiSheet.Entry en : s.history) out.add("v" + en.version + "  " + en.time + "  " + en.who + "  " + en.summary + "  (" + en.edits.size() + " edits)");
        return out;
    }
}
