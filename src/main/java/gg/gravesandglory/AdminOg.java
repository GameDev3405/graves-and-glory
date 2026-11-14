package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Admin tools phase A (2026-11-14 spec, section 4): the server-owned config/gravesandglory/admin.json (the OG window, the players added or removed by hand, and the owners
 * who may run /gg og) and the derived OG status. Pure: no Minecraft classes. Never sent to a client. A missing or malformed file reads as empty (a malformed one is copied to
 * admin.json.bad first and is never rewritten by a load), so the server never crashes on it.
 */
public final class AdminOg {
    private AdminOg() {}
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** The OG window as ISO-8601 instants; both bounds are needed for it to count. */
    public static final class Window { public String start, end; }

    public static final class Config {
        public Window ogWindow;
        public List<String> ogAdded = new ArrayList<>(), ogRemoved = new ArrayList<>(), owners = new ArrayList<>();
        /** The object the file held, so a write keeps keys this build does not know. Not part of the JSON. */
        transient JsonObject raw;
        /** Added by hand: in ogAdded and out of ogRemoved (the last command wins). */
        public void add(UUID id) { ogRemoved.remove(id.toString()); if (!ogAdded.contains(id.toString())) ogAdded.add(id.toString()); }
        /** Removed by hand: in ogRemoved and out of ogAdded. */
        public void remove(UUID id) { ogAdded.remove(id.toString()); if (!ogRemoved.contains(id.toString())) ogRemoved.add(id.toString()); }
    }

    /** An ISO-8601 instant, or a plain date (the start of that UTC day, or with end true the last millisecond of it); null when it is neither. */
    public static Instant parseBound(String text, boolean end) {
        if (text == null) return null;
        String t = text.trim();
        try { return Instant.parse(t); } catch (RuntimeException ignored) {}
        try {
            LocalDate d = LocalDate.parse(t);
            Instant start = d.atStartOfDay().toInstant(ZoneOffset.UTC);
            return end ? start.plusSeconds(86_400).minusMillis(1) : start;
        } catch (RuntimeException ignored) {}
        return null;
    }

    /** The window as {start, end}, or null when there is none (either bound absent or unreadable). */
    public static Instant[] window(Config c) {
        if (c == null || c.ogWindow == null) return null;
        Instant s = parseBound(c.ogWindow.start, false), e = parseBound(c.ogWindow.end, true);
        return s == null || e == null ? null : new Instant[] {s, e};
    }

    /** OG: added by hand, or a first match (stamp above 0) inside the window, bounds inclusive, and not removed by hand. */
    public static boolean isOg(Config c, UUID id, long firstMatchAt) {
        if (c.ogAdded.contains(id.toString())) return true;
        if (firstMatchAt <= 0 || c.ogRemoved.contains(id.toString())) return false;
        Instant[] w = window(c);
        return w != null && firstMatchAt >= w[0].toEpochMilli() && firstMatchAt <= w[1].toEpochMilli();
    }

    /** One line saying why isOg is what it is. */
    public static String reason(Config c, UUID id, long firstMatchAt) {
        if (c.ogAdded.contains(id.toString())) return "OG: added by hand";
        Instant[] w = window(c);
        if (firstMatchAt <= 0) return "not OG: no match finished yet" + (w == null ? " (and no window is set)" : "");
        if (c.ogRemoved.contains(id.toString())) return "not OG: removed by hand";
        if (w == null) return "not OG: no window is set";
        if (isOg(c, id, firstMatchAt)) return "OG: first match " + Instant.ofEpochMilli(firstMatchAt) + " is inside the window";
        return "not OG: first match " + Instant.ofEpochMilli(firstMatchAt) + " is outside the window " + w[0] + " to " + w[1];
    }

    /** The console always may; a player only when listed in owners. An empty owners list means console only. */
    public static boolean mayAdmin(Config c, boolean console, UUID player) {
        return console || (player != null && c.owners.contains(player.toString()));
    }

    /** The command gate: console only when the output is the server itself and no entity runs it (a renamed command block has an entity-less source but another output); otherwise an owner uuid. */
    public static boolean allowed(Config c, boolean outputIsServer, boolean hasEntity, UUID player) {
        return mayAdmin(c, outputIsServer && !hasEntity, player);
    }

    static List<String> uuids(JsonObject j, String key) {
        List<String> out = new ArrayList<>();
        if (!j.has(key) || j.get(key).isJsonNull()) return out;
        if (!j.get(key).isJsonArray()) throw new IllegalArgumentException(key + " must be a list");
        for (var e : j.getAsJsonArray(key)) {
            if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(key + " holds a non-text entry");
            String id = UUID.fromString(e.getAsString()).toString();
            if (!out.contains(id)) out.add(id);
        }
        return out;
    }

    /** Parses and validates the text; throws on anything malformed. */
    static Config parse(String text) {
        var root = JsonParser.parseString(text);
        if (!root.isJsonObject()) throw new IllegalArgumentException("admin.json must be an object");
        JsonObject j = root.getAsJsonObject();
        Config c = new Config();
        c.raw = j;
        c.ogAdded = uuids(j, "ogAdded"); c.ogRemoved = uuids(j, "ogRemoved"); c.owners = uuids(j, "owners");
        if (j.has("ogWindow") && !j.get("ogWindow").isJsonNull()) {
            if (!j.get("ogWindow").isJsonObject()) throw new IllegalArgumentException("ogWindow must be an object");
            JsonObject w = j.getAsJsonObject("ogWindow");
            c.ogWindow = new Window();
            for (String k : new String[] {"start", "end"}) {
                if (!w.has(k) || w.get(k).isJsonNull()) continue;
                String v = w.get(k).getAsString();
                if (parseBound(v, k.equals("end")) == null) throw new IllegalArgumentException("ogWindow." + k + " is not an ISO-8601 instant");
                if (k.equals("start")) c.ogWindow.start = v; else c.ogWindow.end = v;
            }
        }
        return c;
    }

    /** The file read once: the config, and why it is unusable (null when fine). */
    static final class Read { Config config = new Config(); String problem; }

    /** Reads the file text; never throws. Missing or blank: empty. Malformed: empty with problem set, and the bytes copied to name.bad (name.bad-2, ...); the file itself is left as it is. */
    static Read read(Path file) {
        Read r = new Read();
        try {
            if (!Files.isRegularFile(file)) return r;
            String text = Files.readString(file);
            if (text.isBlank()) return r;
            try { r.config = parse(text); }
            catch (RuntimeException bad) {
                r.problem = String.valueOf(bad.getMessage());
                Path kept = keepBad(file);
                SafeFiles.warn.accept("admin.json could not be read (" + r.problem + "); treated as empty and not rewritten. A copy is kept as " + (kept == null ? "(could not be copied)" : kept.getFileName()) + ".");
            }
        } catch (IOException | RuntimeException e) {
            r.problem = "the file could not be opened: " + e;
            SafeFiles.warn.accept("admin.json could not be opened (" + e + "); treated as empty.");
        }
        return r;
    }

    /** One read of the file for callers that do not cache (the checks). */
    public static Config load(Path file) { return read(file).config; }

    /** Copies the damaged file to the first free name.bad, name.bad-2, ...; null when that fails. */
    static Path keepBad(Path file) {
        for (int n = 1; n < 1000; n++) {
            Path to = file.resolveSibling(file.getFileName() + (n == 1 ? ".bad" : ".bad-" + n));
            if (Files.exists(to)) continue;
            try { return Files.copy(file, to); } catch (IOException e) { return null; }
        }
        return null;
    }

    /** The text of the config: the raw object the file held with the known keys overwritten, so unknown keys survive. */
    public static String toJson(Config c) {
        JsonObject j = c.raw == null ? new JsonObject() : c.raw.deepCopy();
        j.add("ogAdded", GSON.toJsonTree(c.ogAdded));
        j.add("ogRemoved", GSON.toJsonTree(c.ogRemoved));
        j.add("owners", GSON.toJsonTree(c.owners));
        if (c.ogWindow == null) j.remove("ogWindow"); else j.add("ogWindow", GSON.toJsonTree(c.ogWindow));
        return GSON.toJson(j);
    }

    /** Atomic write (temp file then move). */
    public static void save(Path file, Config c) throws IOException { SafeFiles.write(file, toJson(c)); }

    /**
     * The server's view of admin.json: read again only when the file's modification time or size changes (a gate check runs on every command suggestion), a damaged file
     * copied to .bad once, and writes refused while the file is damaged, so a command never saves an empty config over it.
     */
    public static final class Store {
        final Path file;
        Config cached = new Config();
        String problem;
        long key = Long.MIN_VALUE;
        int reads;
        public Store(Path file) { this.file = file; }
        long keyOf() {
            try { return Files.isRegularFile(file) ? Files.getLastModifiedTime(file).toMillis() * 31 + Files.size(file) : -1; } catch (IOException e) { return -2; }
        }
        synchronized void refresh() {
            long now = keyOf();
            if (now == key) return;
            key = now; reads++;
            Read r = read(file);
            cached = r.config; problem = r.problem;
        }
        /** The config as the file holds it now (empty when missing or damaged). Treat as read only: change a copy from copy(). */
        public synchronized Config get() { refresh(); return cached; }
        /** Why the file is unusable, or null. */
        public synchronized String problem() { refresh(); return problem; }
        /** A fresh copy of the config for a command to change, then pass to save. */
        public synchronized Config copy() { refresh(); Config c = parseOrEmpty(toJson(cached)); return c; }
        static Config parseOrEmpty(String text) { try { return parse(text); } catch (RuntimeException e) { return new Config(); } }
        /** Writes the config; throws IllegalStateException with the reason when the file is damaged, IOException when the write fails. */
        public synchronized void save(Config c) throws IOException {
            refresh();
            if (problem != null) throw new IllegalStateException("admin.json is damaged (" + problem + "); fix or delete it first. A copy was kept as admin.json.bad.");
            AdminOg.save(file, c);
            key = Long.MIN_VALUE; // read the new file next time
        }
    }
}
