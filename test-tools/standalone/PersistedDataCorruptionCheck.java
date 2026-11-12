package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Corrupt, empty, truncated (torn write), garbage and wrong-type input for the JSON the server persists. Never shipped.
 * Campaign needs Minecraft, so the check reproduces its two loaders line for line on the pure classes they call (Campaign.readParties
 * and Campaign.readProfile) and runs them through SafeFiles.load exactly as Campaign does. Kort approved the behaviour on 2026-11-01
 * (Q21): a damaged parties file or profile falls back to its .bak, then to defaults, with a logged warning, and never stops the server
 * start or a player's join; the damaged bytes are kept as name.corrupt. Every persisted write is atomic (SafeFiles.write).
 */
public final class PersistedDataCorruptionCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create(); // same settings as Campaign.JSON
    static final List<String> warnings = new ArrayList<>();

    /** Unusable text, as a file could hold it. */
    static Map<String, String> badTexts(String valid) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("empty", "");
        m.put("whitespace", " \n\t ");
        m.put("null literal", "null");
        m.put("truncated at half", valid.substring(0, valid.length() / 2));
        m.put("truncated at one byte", valid.substring(0, 1));
        m.put("truncated before closing brace", valid.substring(0, valid.lastIndexOf('}')));
        m.put("garbage text", "this is not json at all");
        m.put("binary garbage", new String(new byte[]{0, 1, 2, (byte) 0xFE, (byte) 0xFF, 0x7F, 0, 0}, StandardCharsets.ISO_8859_1));
        m.put("NUL padding (torn write on some filesystems)", "\0\0\0\0\0\0\0\0");
        m.put("array not object", "[]");
        m.put("string not object", "\"profile\"");
        m.put("number not object", "12345");
        m.put("wrong-type field", "{\"gold\":\"lots\",\"schema\":\"three\"}");
        m.put("wrong-type nested", "{\"owned\":[1,2,3],\"banner\":{\"a\":1}}");
        return m;
    }
    static List<String> names(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) { return s.map(p -> p.getFileName().toString()).sorted().toList(); }
    }
    /** The original bytes survive somewhere in dir: in place, as a migration backup, or moved aside as .corrupt. */
    static boolean kept(Path dir, Path file, byte[] bytes) throws IOException {
        for (String n : names(dir)) {
            Path p = dir.resolve(n);
            if (!Files.isRegularFile(p)) continue;
            boolean candidate = p.equals(file) || n.contains(".corrupt") || n.endsWith(".v1.json") || n.endsWith(".v2.json");
            if (candidate && (Arrays.equals(Files.readAllBytes(p), bytes) || n.endsWith(".v1.json") || n.endsWith(".v2.json"))) return true;
        }
        return false;
    }

    // ---- The parties file: Campaign.readParties through SafeFiles.load, as the Campaign constructor calls it ----
    static SocialHub readParties(Path file) throws Exception {
        SocialHub s = JSON.fromJson(Files.readString(file), SocialHub.class);
        if (s == null) throw new IllegalStateException("Empty party data");
        s.restore();
        return s;
    }
    static SocialHub loadParties(Path file) { return SafeFiles.load(file, PersistedDataCorruptionCheck::readParties, SocialHub::new, "Party data"); }
    static void usable(SocialHub hub, String what) {
        check(hub != null && hub.parties != null && hub.names != null && hub.rooms != null && hub.invites != null, "parties usable after " + what);
        hub.createParty(UUID.randomUUID());
    }
    static void parties(Path root) throws IOException {
        SocialHub seeded = new SocialHub();
        UUID a = UUID.randomUUID();
        seeded.createParty(a);
        String valid = JSON.toJson(seeded);
        Path okDir = Files.createDirectories(root.resolve("parties-ok"));
        Path ok = okDir.resolve("gravesandglory-parties.json");
        SafeFiles.writeWithBackup(ok, valid);
        check(loadParties(ok).party(a) != null, "a valid parties file round-trips");
        check(loadParties(okDir.resolve("missing.json")).parties.isEmpty(), "a missing parties file is no parties, quietly");
        int index = 0;
        for (var e : badTexts(valid).entrySet()) {
            byte[] bytes = e.getValue().getBytes(StandardCharsets.ISO_8859_1);
            // With a good .bak: the backup is restored.
            Path d = Files.createDirectories(root.resolve("parties-bak-" + index));
            Path f = d.resolve("gravesandglory-parties.json");
            Files.writeString(SafeFiles.backup(f), valid);
            Files.write(f, bytes);
            warnings.clear();
            SocialHub hub;
            try { hub = loadParties(f); } catch (Throwable t) { check(false, "parties: " + e.getKey() + " escaped as " + t); return; }
            usable(hub, e.getKey());
            boolean parsed = warnings.isEmpty(); // a parseable but hollow object reads as it is
            if (!parsed) {
                check(hub.party(a) != null, "parties fall back to the .bak for " + e.getKey());
                check(warnings.size() == 1 && warnings.get(0).contains("restored from"), "the .bak fallback is logged for " + e.getKey() + ": " + warnings);
            }
            check(kept(d, f, bytes), "the damaged parties bytes are kept for " + e.getKey());
            // Without a .bak: defaults, logged.
            Path d2 = Files.createDirectories(root.resolve("parties-none-" + index));
            Path f2 = d2.resolve("gravesandglory-parties.json");
            Files.write(f2, bytes);
            warnings.clear();
            try { hub = loadParties(f2); } catch (Throwable t) { check(false, "parties (no .bak): " + e.getKey() + " escaped as " + t); return; }
            if (!parsed) check(hub.parties.isEmpty() && warnings.size() == 1 && warnings.get(0).contains("using defaults"), "no .bak: defaults with a warning for " + e.getKey() + ": " + warnings);
            usable(hub, e.getKey() + " without a .bak");
            check(kept(d2, f2, bytes), "the damaged parties bytes are kept without a .bak for " + e.getKey());
            index++;
        }
        // Both the file and its .bak damaged: defaults, both copies kept.
        Path d = Files.createDirectories(root.resolve("parties-both"));
        Path f = d.resolve("gravesandglory-parties.json");
        Files.writeString(f, "{\"parties\":");
        Files.writeString(SafeFiles.backup(f), "garbage");
        warnings.clear();
        check(loadParties(f).parties.isEmpty(), "file and .bak both damaged: no parties");
        check(warnings.size() == 1 && warnings.get(0).contains("its backup could not be read"), "the double failure is logged: " + warnings);
        check(names(d).containsAll(List.of("gravesandglory-parties.json.bak", "gravesandglory-parties.json.corrupt", "gravesandglory-parties.json.corrupt-2")), "both damaged copies are kept: " + names(d));
        check(!Files.exists(f), "the damaged copy is moved aside, so the next save starts clean");
        // Parseable but hollow objects are accepted and normalised.
        Files.writeString(f, "{\"parties\":null,\"names\":null}");
        check(loadParties(f).parties.isEmpty(), "null maps are replaced by restore()");
    }

    // ---- The commander profile: Campaign.readProfile through SafeFiles.load, as Campaign.profile calls it ----
    static Profile readProfile(Path file) throws Exception {
        Profile p = ProfileMigration.upgradeFile(file, text -> JSON.fromJson(text, ProfileMigration.V1.class), text -> JSON.fromJson(text, Profile.class), JSON::toJson, RosterMath.Catalog.fromSpec());
        if (p == null) p = JSON.fromJson(Files.readString(file), Profile.class);
        if (p == null) throw new IllegalStateException("Empty profile");
        p.sanitize();
        return p;
    }
    static Profile freshProfile() { Profile fresh = new Profile(); fresh.sanitize(); return fresh; }
    static Profile loadProfile(Path file) { return SafeFiles.load(file, PersistedDataCorruptionCheck::readProfile, PersistedDataCorruptionCheck::freshProfile, "Commander profile"); }
    static void sane(Profile p, String what) {
        check(p != null && p.owned != null && p.banners != null && !p.banners.isEmpty() && p.progression != null && p.commander != null, "sanitized profile has no null parts after " + what);
    }
    static void profiles(Path root) throws IOException {
        Profile fresh = freshProfile();
        fresh.gearShards = 123;
        String valid = JSON.toJson(fresh);
        Path dir = Files.createDirectories(root.resolve("profiles"));
        Path good = dir.resolve(UUID.randomUUID() + ".json");
        Files.writeString(good, valid);
        check(loadProfile(good).gearShards == 123, "a valid current profile loads untouched");
        check(names(dir).size() == 1, "reading a current profile writes nothing");

        int restored = 0, defaults = 0, read = 0, index = 0;
        for (var e : badTexts(valid).entrySet()) {
            byte[] bytes = e.getValue().getBytes(StandardCharsets.ISO_8859_1);
            for (boolean withBak : new boolean[]{true, false}) {
                Path d = Files.createDirectories(root.resolve("bad-" + (index++)));
                Path f = d.resolve(UUID.randomUUID() + ".json");
                if (withBak) Files.writeString(SafeFiles.backup(f), valid);
                Files.write(f, bytes);
                warnings.clear();
                Profile p;
                try { p = loadProfile(f); } catch (Throwable t) { check(false, "profile: " + e.getKey() + " escaped as " + t); return; }
                sane(p, e.getKey());
                if (warnings.isEmpty()) read++; // parsed (a schema-less object is taken for v1 and migrated; its .v1.json keeps the original)
                else if (withBak) { restored++; check(p.gearShards == 123, "profile restored from its .bak for " + e.getKey()); }
                else { defaults++; check(p.gearShards == fresh().gearShards && warnings.get(0).contains("using defaults"), "no .bak: a default profile for " + e.getKey()); }
                check(warnings.size() <= 1, "one warning per fallback: " + warnings);
                check(kept(d, f, bytes), "no data loss for " + e.getKey() + (withBak ? " (with .bak)" : ""));
                check(names(d).stream().noneMatch(n -> n.endsWith(".tmp")), "no stray .tmp left for " + e.getKey());
            }
        }
        check(restored > 0 && defaults == restored, "every refused profile falls back: " + restored + " from .bak, " + defaults + " to defaults, " + read + " parsed");
        System.out.println("profile loader: " + restored + " restored from .bak, " + defaults + " to defaults, " + read + " parsed");

        // Unwritable migration temp path: the upgrade fails with an IOException. The join still goes ahead (on defaults, logged) and the
        // original bytes are kept aside for an admin.
        Path d = Files.createDirectories(root.resolve("unwritable"));
        UUID id = UUID.randomUUID();
        Path f = d.resolve(id + ".json");
        Files.writeString(f, "{\"schema\":1,\"gold\":55,\"commander\":\"vanguard\"}");
        byte[] before = Files.readAllBytes(f);
        Files.createDirectory(d.resolve(id + ".tmp"));
        warnings.clear();
        Profile p = loadProfile(f);
        sane(p, "an unwritable temp path");
        check(warnings.size() == 1 && warnings.get(0).contains("could not be read") && warnings.get(0).contains("Exception"), "the unwritable upgrade is logged with its cause: " + warnings);
        check(kept(d, f, before), "unwritable temp path keeps the original profile bytes");
    }
    static Profile fresh() { return freshProfile(); }

    // ---- Atomic writes: SafeFiles.write and writeWithBackup (profiles, parties, layout and terrain markers, Presentation, HudSize, BuildMode) ----
    static void atomicWrites(Path root) throws IOException {
        Path d = Files.createDirectories(root.resolve("atomic"));
        Path f = d.resolve("prefs.json");
        SafeFiles.write(f, "{\"a\":1}");
        check(Files.readString(f).equals("{\"a\":1}") && names(d).equals(List.of("prefs.json")), "a write lands whole and leaves no temp file");
        SafeFiles.writeWithBackup(f, "{\"a\":2}");
        check(Files.readString(f).equals("{\"a\":2}") && Files.readString(SafeFiles.backup(f)).equals("{\"a\":1}"), "a backed-up write keeps the previous file as .bak");
        // The temp path is blocked (a directory): the write fails and the target keeps its old bytes.
        Files.createDirectory(d.resolve("prefs.json.tmp"));
        boolean failed = false;
        try { SafeFiles.write(f, "{\"a\":3}"); } catch (IOException expected) { failed = true; }
        check(failed && Files.readString(f).equals("{\"a\":2}"), "a failed write leaves the old file intact");
        warnings.clear();
        check(!SafeFiles.tryWrite(f, "x", "Marker") && warnings.size() == 1 && warnings.get(0).startsWith("Marker could not be written"), "a best-effort write warns instead of throwing");
        Files.writeString(d.resolve("blocker"), "x");
        failed = false;
        try { SafeFiles.write(d.resolve("blocker/inner.json"), "x"); } catch (IOException expected) { failed = true; }
        check(failed, "an unwritable parent surfaces as IOException");
        SafeFiles.write(d.resolve("new/deep/file.json"), "y");
        check(Files.readString(d.resolve("new/deep/file.json")).equals("y"), "write creates missing parent folders");
        // Every persisted writer goes through SafeFiles: no direct Files.writeString left in them.
        Path src = source();
        for (String name : List.of("Campaign.java", "CampWorld.java", "BuildMode.java", "HudSize.java", "Presentation.java")) {
            String text = Files.readString(src.resolve(name));
            check(!text.contains("Files.writeString("), name + " writes persisted files through SafeFiles only");
            check(text.contains("SafeFiles."), name + " uses SafeFiles");
        }
    }
    static Path source() {
        for (Path at = Paths.get("").toAbsolutePath(); at != null; at = at.getParent()) {
            Path s = at.resolve("src/main/java/gg/gravesandglory");
            if (Files.isDirectory(s)) return s;
        }
        throw new AssertionError("run from the repository: src/main/java/gg/gravesandglory not found");
    }

    // ---- Layout marker (cache only): LayoutMarker.current must never throw ----
    static void layoutMarker() {
        String valid = LayoutMarker.text(2, "abc");
        check(LayoutMarker.current(valid, 2, "abc"), "a valid layout marker is current");
        for (var e : badTexts(valid).entrySet()) {
            boolean current;
            try { current = LayoutMarker.current(e.getValue(), 2, "abc"); }
            catch (Throwable t) { check(false, "layout marker: " + e.getKey() + " escaped as " + t); return; }
            check(!current, "a damaged layout marker is never current: " + e.getKey());
        }
    }

    // ---- Legacy copy: copyMissing keeps bytes verbatim and lets the current file win ----
    static void legacy(Path root) throws IOException {
        Path old = Files.createDirectories(root.resolve("legacy"));
        Path oldFile = old.resolve("a.json");
        Files.writeString(oldFile, "{\"gold\":"); // truncated legacy file
        Path current = root.resolve("current/a.json");
        LegacyDataMigration.copyMissing(oldFile, current);
        // The copy does not validate, so a torn legacy file is carried over as it is; the profile loader then falls back (see profiles).
        check(Files.readString(current).equals("{\"gold\":"), "a damaged legacy file is copied verbatim, not repaired or dropped");
        check(Files.readString(oldFile).equals("{\"gold\":"), "the legacy original is untouched");
        Files.writeString(oldFile, "{\"gold\":9}");
        LegacyDataMigration.copyMissing(oldFile, current);
        check(Files.readString(current).equals("{\"gold\":"), "an existing (even corrupt) current file always wins and is never overwritten");
        LegacyDataMigration.copyMissing(root.resolve("missing.json"), root.resolve("current/b.json"));
        check(!Files.exists(root.resolve("current/b.json")), "a missing legacy file is a no-op");
        Files.writeString(root.resolve("blocker"), "x"); // the destination's parent is a regular file: unwritable
        boolean failed = false;
        try { LegacyDataMigration.copyMissing(oldFile, root.resolve("blocker/c.json")); } catch (IOException expected) { failed = true; }
        check(failed, "an unwritable destination surfaces as IOException (Campaign wraps it and stops the server)");
        check(names(old).equals(List.of("a.json")), "no migration temp files left behind after the failure");
        check(names(root.resolve("current")).equals(List.of("a.json")), "no migration temp files left in the destination");
    }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("gg-corruption-check");
        SafeFiles.warn = warnings::add;
        try {
            parties(root);
            profiles(root);
            atomicWrites(root);
            layoutMarker();
            legacy(root);
        } finally {
            try (Stream<Path> s = Files.walk(root)) { for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
        }
        System.out.println("PersistedDataCorruptionCheck: " + checks + " checks passed");
    }
}
