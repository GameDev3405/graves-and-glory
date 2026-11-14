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

    /**
     * A player finished a match when he was present at its end (a dropped player who came back is; one who never returned is not) and his own seated seconds reach the reward
     * minimum: a late joiner who sat less than that is not stamped, however long the match ran.
     */
    public static boolean finished(boolean presentAtEnd, long playedSeconds, int minimumRewardSeconds) {
        return presentAtEnd && playedSeconds > 0 && playedSeconds >= minimumRewardSeconds;
    }

    /**
     * Who a crash-recovered match stamps: players who had played the minimum and were not away when the checkpoint was written. A checkpoint without the away list (written by
     * an older build) stamps nobody: absence cannot be told.
     */
    public static Set<UUID> crashStamps(Map<UUID, Long> played, Set<UUID> away, int minimumRewardSeconds) {
        Set<UUID> out = new LinkedHashSet<>();
        if (away == null) return out;
        for (var e : played.entrySet()) if (finished(!away.contains(e.getKey()), e.getValue(), minimumRewardSeconds)) out.add(e.getKey());
        return out;
    }

    /** True when an existing file could not be read (damaged or blank): the ledger then refuses every save, so the stamps in the file are never overwritten. */
    boolean broken;
    String brokenReason;
    long brokenKey;
    /** Why a save was refused, to log at most once a minute (limited). */
    private static final Map<String, Long> lastLogged = new HashMap<>();
    /** True when this message may be logged now: at most once per 60 s per key. */
    static synchronized boolean limited(String key, long nowMillis) {
        Long last = lastLogged.get(key);
        if (last != null && nowMillis - last < 60_000 && nowMillis >= last) return false;
        lastLogged.put(key, nowMillis);
        return true;
    }

    static long keyOf(Path file) {
        try { return Files.getLastModifiedTime(file).toMillis() * 31 + Files.size(file); } catch (IOException e) { return -2; }
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

    /** Reads the file; never throws. A missing file starts empty. An existing file that is blank or damaged also reads as empty but the ledger is marked broken and refuses saves; a copy is kept as joins.json.bad. */
    public static JoinLedger load(Path file) {
        JoinLedger l = new JoinLedger();
        try {
            if (!Files.isRegularFile(file)) return l;
            String text = Files.readString(file);
            try {
                if (text.isBlank()) throw new IllegalArgumentException("the file is blank");
                return parse(text);
            } catch (RuntimeException bad) {
                Path kept = AdminOg.keepBad(file);
                l.markBroken(file, String.valueOf(bad.getMessage()));
                SafeFiles.warn.accept("joins.json could not be read (" + l.brokenReason + "); no stamp will be saved until it is fixed, and the file is left as it is. A copy is kept as " + (kept == null ? "(could not be copied)" : kept.getFileName()) + ".");
            }
        } catch (IOException | RuntimeException e) {
            l.markBroken(file, "could not be opened: " + e);
            SafeFiles.warn.accept("joins.json could not be opened (" + e + "); no stamp will be saved until it is fixed.");
        }
        return l;
    }

    /**
     * The ledger to use now: when the broken one's file has changed and reads clean, the fresh ledger with the stamps taken meanwhile merged in and saved at once (a crash
     * before the next stamp must not lose them); otherwise the broken ledger, its key updated so the file is not read again until it changes.
     */
    static JoinLedger reload(JoinLedger old, Path file) {
        if (!old.broken || keyOf(file) == old.brokenKey) return old;
        JoinLedger fresh = load(file);
        if (fresh.broken) { old.brokenKey = fresh.brokenKey; return old; }
        fresh.absorb(old);
        try { fresh.save(file); } catch (IOException | RuntimeException e) { SafeFiles.warn.accept("joins.json: the merged stamps could not be saved yet (" + e.getMessage() + ")."); }
        return fresh;
    }

    void markBroken(Path file, String reason) { broken = true; brokenReason = reason; brokenKey = keyOf(file); }

    /** Stamps this ledger lacks, taken from another (the one read after a fixed file). */
    void absorb(JoinLedger other) {
        for (var e : other.entries.entrySet()) {
            Entry mine = entries.computeIfAbsent(e.getKey(), k -> new Entry());
            if (mine.firstSeenAt <= 0 && e.getValue().firstSeenAt > 0) { mine.firstSeenAt = e.getValue().firstSeenAt; }
            if (mine.firstMatchAt <= 0 && e.getValue().firstMatchAt > 0) { mine.firstMatchAt = e.getValue().firstMatchAt; }
            if (mine.lastName.isEmpty()) mine.lastName = e.getValue().lastName;
        }
        dirty = true;
    }

    /** Atomic write (temp file then move). */
    public void save(Path file) throws IOException {
        if (broken) throw new IllegalStateException("joins.json is damaged (" + brokenReason + "); not overwriting it");
        SafeFiles.writeWithBackup(file, GSON.toJson(entries));
        dirty = false;
    }
}
