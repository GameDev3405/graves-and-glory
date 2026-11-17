package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.Instant;
import java.util.*;

/**
 * The UI editor's data (UI editor design 5.2 to 5.9, phase 1: words): the piece list the build generates (ui/pieces.json), a sheet of
 * edits (the mod's built-in edits, the server's published sheet with its history, an owner's draft), the validation every edit passes
 * on the server and again on each client, Publish, Roll back and Reset. Pure: Gson only, so the standalone check runs it.
 *
 * <p>Sheet file (config/gravesandglory/ui-edits.json, and assets/gravesandglory/ui/builtin-edits.json without history):
 * <pre>{"format":1,"version":3,"edits":{"camp.title":{"text":"..."}},"history":[{"version":2,"time":"...","who":"...","summary":"...","edits":{...}}]}</pre>
 * Phase 1 edits hold only "text"; a later phase adds fields beside it. An edit whose piece is unknown is an orphan: kept in the
 * sheet, ignored when drawing, listed by the editor and the build check.
 */
public final class UiSheet {
    public static final int FORMAT = 1;
    /** Longest edited line, in characters: a safety cap only (5.3). */
    public static final int TEXT_CAP = 300;
    /** Published versions the history keeps (open question 3 for Kort; the design assumes 50). */
    public static final int HISTORY_CAP = 50;
    /** Most edits a sheet may hold. */
    public static final int EDIT_CAP = 4000;
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    static final Gson COMPACT = new GsonBuilder().disableHtmlEscaping().create();

    /** A piece as the build declared it: its id, default template, the slots its code supplies, whether it may show key hints. */
    public record Piece(String id, String def, Set<String> slots, boolean keys, String file) {}

    /** One past published version. */
    public static final class Entry {
        public int version;
        public String time = "", who = "", summary = "";
        public Map<String, String> edits = new TreeMap<>();
    }

    public int version;
    /** When, by whom and why the current version was published (empty for version 0). */
    public String time = "", who = "", summary = "";
    /** Piece id to edited text. Sorted, so the file diffs cleanly. */
    public Map<String, String> edits = new TreeMap<>();
    public List<Entry> history = new ArrayList<>();

    public UiSheet copy() { return parse(toJson(true)); }

    // ---- pieces ----

    /** The piece list (ui/pieces.json): {"pieces":[{"id","default","slots":[...],"keys":bool,"file"}],"renames":{"old":"new"}}. Throws on malformed. */
    public static Map<String, Piece> parsePieces(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Piece> out = new LinkedHashMap<>();
        for (JsonElement e : root.getAsJsonArray("pieces")) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("locked") && o.get("locked").getAsBoolean()) continue; // drawn as a piece, never edited here
            Set<String> slots = new LinkedHashSet<>();
            if (o.has("slots")) for (JsonElement s : o.getAsJsonArray("slots")) slots.add(s.getAsString());
            String id = o.get("id").getAsString();
            if (out.containsKey(id)) throw new IllegalArgumentException("piece " + id + " is declared twice");
            out.put(id, new Piece(id, o.get("default").getAsString(), slots, o.has("keys") && o.get("keys").getAsBoolean(), o.has("file") ? o.get("file").getAsString() : ""));
        }
        return out;
    }

    /** The rename table of the piece list (old id to new id), empty when it has none. */
    public static Map<String, String> parseRenames(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, String> out = new LinkedHashMap<>();
        if (root.has("renames")) for (var e : root.getAsJsonObject("renames").entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        return out;
    }

    // ---- validation ----

    /** Why this text may not be the edit for this piece, or null when it may (5.5: known piece, known slots, length cap, plain text). */
    public static String problem(Piece piece, String text) {
        if (piece == null) return "no such piece";
        if (text == null) return "no text";
        if (text.length() > TEXT_CAP) return "longer than " + TEXT_CAP + " characters";
        for (int i = 0; i < text.length(); ) {
            int c = text.codePointAt(i);
            i += Character.charCount(c);
            if (c == '§') return "the section sign (a formatting code) is not allowed; text is shown as typed";
            if (Character.isISOControl(c)) return "control characters (line breaks, tabs) are not allowed";
            if (invisible(c)) return String.format(java.util.Locale.ROOT, "invisible or direction characters (U+%04X) are not allowed", c);
        }
        return UiTemplate.problem(text, piece.slots(), piece.keys());
    }

    /** A character that draws nothing or reorders the text around it: every format character (zero-width spaces and joiners, the
     *  bidi marks, embeddings, overrides and isolates, the byte-order mark, the soft hyphen, the word joiner), the Hangul and
     *  halfwidth fillers, and a lone surrogate. An edit with one could look like other wording than it holds. */
    static boolean invisible(int c) {
        return Character.getType(c) == Character.FORMAT || Character.getType(c) == Character.SURROGATE
            || c == 0x115F || c == 0x1160 || c == 0x3164 || c == 0xFFA0 || c == 0x034F;
    }

    /** The text a client may draw for an edit, or null to draw the default: the same rules, applied to a sheet it does not trust. */
    public static String usable(Piece piece, String text) {
        return piece != null && problem(piece, text) == null ? text : null;
    }

    // ---- reading and writing ----

    static Map<String, String> editsOf(JsonObject o, String where) {
        Map<String, String> out = new TreeMap<>();
        if (!o.has("edits") || o.get("edits").isJsonNull()) return out;
        if (!o.get("edits").isJsonObject()) throw new IllegalArgumentException(where + "edits must be an object");
        for (var e : o.getAsJsonObject("edits").entrySet()) {
            if (!e.getValue().isJsonObject()) throw new IllegalArgumentException(where + "edit " + e.getKey() + " must be an object");
            JsonObject edit = e.getValue().getAsJsonObject();
            if (edit.has("text") && edit.get("text").isJsonPrimitive()) out.put(e.getKey(), edit.get("text").getAsString());
            // phase 1 reads text only; other fields of a later phase are dropped by this build rather than refused
        }
        if (out.size() > EDIT_CAP) throw new IllegalArgumentException(where + "more than " + EDIT_CAP + " edits");
        return out;
    }

    /** Parses a sheet; throws on anything malformed (the caller keeps the bytes and runs on the built-in edits). */
    public static UiSheet parse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) throw new IllegalArgumentException("the sheet must be an object");
        JsonObject o = root.getAsJsonObject();
        if (o.has("format") && o.get("format").getAsInt() > FORMAT) throw new IllegalArgumentException("format " + o.get("format").getAsInt() + " is newer than this build reads");
        UiSheet s = new UiSheet();
        s.version = o.has("version") ? o.get("version").getAsInt() : 0;
        s.time = str(o, "time"); s.who = str(o, "who"); s.summary = str(o, "summary");
        s.edits = editsOf(o, "");
        if (o.has("history") && o.get("history").isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray("history")) {
                JsonObject h = e.getAsJsonObject();
                Entry en = new Entry();
                en.version = h.get("version").getAsInt();
                en.time = str(h, "time"); en.who = str(h, "who"); en.summary = str(h, "summary");
                en.edits = editsOf(h, "history " + en.version + ": ");
                s.history.add(en);
            }
        }
        return s;
    }

    static String str(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : ""; }

    static JsonObject editsJson(Map<String, String> edits) {
        JsonObject out = new JsonObject();
        for (var e : edits.entrySet()) { JsonObject t = new JsonObject(); t.addProperty("text", e.getValue()); out.add(e.getKey(), t); }
        return out;
    }

    /** The sheet as JSON; withHistory false gives what players are sent (version and edits only). */
    public String toJson(boolean withHistory) {
        JsonObject o = new JsonObject();
        o.addProperty("format", FORMAT);
        o.addProperty("version", version);
        if (withHistory) { o.addProperty("time", time); o.addProperty("who", who); o.addProperty("summary", summary); }
        o.add("edits", editsJson(edits));
        if (withHistory) {
            JsonArray h = new JsonArray();
            for (Entry en : history) {
                JsonObject j = new JsonObject();
                j.addProperty("version", en.version); j.addProperty("time", en.time); j.addProperty("who", en.who); j.addProperty("summary", en.summary);
                j.add("edits", editsJson(en.edits));
                h.add(j);
            }
            o.add("history", h);
            return GSON.toJson(o);
        }
        return COMPACT.toJson(o);
    }

    // ---- renames and orphans (5.9) ----

    /** The edits with every renamed id moved to its new id (an edit already under the new id wins). */
    public static Map<String, String> renamed(Map<String, String> edits, Map<String, String> renames) {
        Map<String, String> out = new TreeMap<>();
        for (var e : edits.entrySet()) {
            String id = e.getKey();
            for (int hops = 0; renames.containsKey(id) && hops < 16; hops++) id = renames.get(id);
            if (!out.containsKey(id) || id.equals(e.getKey())) out.put(id, e.getValue());
        }
        return out;
    }

    /** Edit ids no piece has (orphans: kept, ignored when drawing). */
    public static List<String> orphans(Map<String, String> edits, Map<String, Piece> pieces) {
        List<String> out = new ArrayList<>();
        for (String id : edits.keySet()) if (!pieces.containsKey(id)) out.add(id);
        return out;
    }

    /** Edits that would draw: known piece, valid text. */
    public static Map<String, String> drawable(Map<String, String> edits, Map<String, Piece> pieces) {
        Map<String, String> out = new HashMap<>();
        for (var e : edits.entrySet()) {
            String t = usable(pieces.get(e.getKey()), e.getValue());
            if (t != null) out.put(e.getKey(), t);
        }
        return out;
    }

    // ---- publishing (5.6, 5.7) ----

    /** One line saying what changes from one set of edits to the next. */
    public static String summary(Map<String, String> from, Map<String, String> to) {
        int changed = 0, added = 0, removed = 0;
        for (var e : to.entrySet()) { String old = from.get(e.getKey()); if (old == null) added++; else if (!old.equals(e.getValue())) changed++; }
        for (String id : from.keySet()) if (!to.containsKey(id)) removed++;
        if (changed + added + removed == 0) return "no change";
        List<String> parts = new ArrayList<>();
        if (added > 0) parts.add(added + " line" + (added == 1 ? "" : "s") + " retyped");
        if (changed > 0) parts.add(changed + " retyped again");
        if (removed > 0) parts.add(removed + " back to default");
        return String.join(", ", parts);
    }

    /** Records the current version in the history, then makes the given edits current under a new version. Returns the summary. */
    String advance(Map<String, String> next, String who, Instant now, String note) {
        String sum = note != null ? note : summary(edits, next);
        Entry en = new Entry();
        en.version = version; en.edits = new TreeMap<>(edits);
        en.time = time; en.who = this.who; en.summary = summary;
        if (version > 0 || !edits.isEmpty()) history.add(0, en);
        while (history.size() > HISTORY_CAP) history.remove(history.size() - 1);
        version++;
        edits = new TreeMap<>(next);
        time = now.toString(); this.who = who; summary = sum;
        return sum;
    }

    /** Publish: the draft becomes the published sheet. Every edit is validated again; the first problem is thrown. */
    public String publish(Map<String, String> draft, Map<String, Piece> pieces, String who, Instant now) {
        for (var e : draft.entrySet()) {
            if (!pieces.containsKey(e.getKey())) continue; // an orphan carried over from the published sheet: kept
            String p = problem(pieces.get(e.getKey()), e.getValue());
            if (p != null) throw new IllegalArgumentException(e.getKey() + ": " + p);
        }
        // nothing changed: no new version, so none is pushed out of the history
        if (draft.equals(edits)) throw new IllegalArgumentException("nothing to publish: the draft is the same as version " + version);
        return advance(draft, who, now, null);
    }

    /** Republishes an earlier version as a new one. */
    public String rollback(int toVersion, String who, Instant now) {
        for (Entry en : history) if (en.version == toVersion) return advance(en.edits, who, now, "rolled back to version " + toVersion);
        throw new IllegalArgumentException("no version " + toVersion + " in the history (it keeps the last " + HISTORY_CAP + ")");
    }

    /** Back to the mod's built-in edits: an empty sheet, the previous one kept in the history. */
    public String reset(String who, Instant now) { return advance(new TreeMap<>(), who, now, "reset to the built-in defaults"); }

    // ---- baking (5.8) ----

    /** Built-in edits after a bake: the old built-in edits with the published ones on top (a published edit equal to the piece's code default is dropped). */
    public static Map<String, String> baked(Map<String, String> builtin, Map<String, String> published, Map<String, Piece> pieces) {
        Map<String, String> out = new TreeMap<>(builtin);
        for (var e : published.entrySet()) {
            Piece p = pieces.get(e.getKey());
            if (p != null && p.def().equals(e.getValue())) out.remove(e.getKey()); else out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    /** The server sheet's edits after a bake: only what differs from the new built-in edits. */
    public static Map<String, String> differing(Map<String, String> published, Map<String, String> builtin) {
        Map<String, String> out = new TreeMap<>();
        for (var e : published.entrySet()) if (!e.getValue().equals(builtin.get(e.getKey()))) out.put(e.getKey(), e.getValue());
        return out;
    }
}
