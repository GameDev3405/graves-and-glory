package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Admin tools phase A: the join ledger, config/gravesandglory/joins.json, keyed by player uuid: firstSeenAt and firstMatchAt (epoch ms, 0 = not yet) and lastName. Server
 * side only, kept apart from the profile on purpose (profile repair, reset and import never touch it, and no client snapshot can carry it). A stamp is written once and never
 * changes. A missing or malformed file reads as empty (a malformed one is copied to joins.json.bad and is never rewritten by a load). Pure: no Minecraft classes.
 */
public final class JoinLedger {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class Entry { public long firstSeenAt, firstMatchAt; public String lastName = ""; }

    final Map<String, Entry> entries = new TreeMap<>();
    /** True after a change not yet saved. */
    boolean dirty;

    public Entry get(UUID id) { return entries.get(id.toString()); }
    public Map<String, Entry> all() { return Collections.unmodifiableMap(entries); }

    Entry entry(UUID id, String name) {
        Entry e = entries.computeIfAbsent(id.toString(), k -> new Entry());
        if (name != null && !name.isBlank() && !name.equals(e.lastName)) { e.lastName = name; dirty = true; }
        return e;
    }

    /** Notes a login: stamps firstSeenAt once (now above 0) and keeps the latest name. True when the stamp was written. */
    public boolean seen(UUID id, String name, long now) {
        if (now <= 0) return false;
        Entry e = entry(id, name);
        if (e.firstSeenAt > 0) return false;
        e.firstSeenAt = now; dirty = true;
        return true;
    }

    /** Notes a finished match: stamps firstMatchAt once (now above 0). True when the stamp was written. */
    public boolean matched(UUID id, String name, long now) {
        if (now <= 0) return false;
        Entry e = entry(id, name);
        if (e.firstMatchAt > 0) return false;
        e.firstMatchAt = now; dirty = true;
        return true;
    }

    /** The uuid last seen under this name (case-insensitive), or null. */
    public UUID byName(String name) {
        if (name == null) return null;
        for (var e : entries.entrySet()) if (e.getValue().lastName.equalsIgnoreCase(name)) return UUID.fromString(e.getKey());
        return null;
    }

    /** A player counts as having finished a match when he was present at its end (a dropped player who came back is; one who never returned is not), was seated for some time, and the match lasted the reward minimum (Campaign.finish's eligible). */
    public static boolean finished(boolean presentAtEnd, long seatedSeconds, long matchSeconds, int minimumRewardSeconds) {
        return presentAtEnd && seatedSeconds > 0 && matchSeconds >= minimumRewardSeconds;
    }
    /** The crash-recovery form: the seconds he had played (Campaign.payInterrupted) against the reward minimum. */
    public static boolean finished(boolean presentAtEnd, long playedSeconds, int minimumRewardSeconds) {
        return presentAtEnd && playedSeconds > 0 && playedSeconds >= minimumRewardSeconds;
    }

    static JoinLedger parse(String text) {
        JsonElement root = JsonParser.parseString(text);
        if (!root.isJsonObject()) throw new IllegalArgumentException("joins.json must be an object");
        JoinLedger l = new JoinLedger();
        for (var e : root.getAsJsonObject().entrySet()) {
            String id = UUID.fromString(e.getKey()).toString();
            if (!e.getValue().isJsonObject()) throw new IllegalArgumentException("entry " + id + " must be an object");
            JsonObject o = e.getValue().getAsJsonObject();
            Entry en = new Entry();
            en.firstSeenAt = Math.max(0, o.has("firstSeenAt") ? o.get("firstSeenAt").getAsLong() : 0);
            en.firstMatchAt = Math.max(0, o.has("firstMatchAt") ? o.get("firstMatchAt").getAsLong() : 0);
            en.lastName = o.has("lastName") && !o.get("lastName").isJsonNull() ? o.get("lastName").getAsString() : "";
            l.entries.put(id, en);
        }
        return l;
    }

    /** Reads the file; never throws (see the class comment). */
    public static JoinLedger load(Path file) {
        try {
            if (!Files.isRegularFile(file)) return new JoinLedger();
            String text = Files.readString(file);
            if (text.isBlank()) return new JoinLedger();
            try { return parse(text); }
            catch (RuntimeException bad) {
                Path kept = AdminOg.keepBad(file);
                SafeFiles.warn.accept("joins.json could not be read (" + bad.getMessage() + "); treated as empty. A copy is kept as " + (kept == null ? "(could not be copied)" : kept.getFileName()) + ".");
                return new JoinLedger();
            }
        } catch (IOException | RuntimeException e) {
            SafeFiles.warn.accept("joins.json could not be opened (" + e + "); treated as empty.");
            return new JoinLedger();
        }
    }

    /** Atomic write (temp file then move). */
    public void save(Path file) throws IOException { SafeFiles.write(file, GSON.toJson(entries)); dirty = false; }
}
