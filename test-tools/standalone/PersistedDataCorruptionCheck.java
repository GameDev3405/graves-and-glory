package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Corrupt, empty, truncated (torn write), garbage and wrong-type input for the JSON the server persists. Never shipped.
 * Campaign needs Minecraft, so the check reproduces its two read paths line for line on the pure classes they call:
 * the parties file (Campaign constructor: Gson to SocialHub, then restore) and the commander profile (Campaign.profile:
 * ProfileMigration.upgradeFile with Gson, then sanitize). It also covers LayoutMarker.current and LegacyDataMigration.copyMissing.
 * It documents the CURRENT behaviour; the TODO lines mark findings for Kort (save behaviour is an approval gate, nothing is fixed here).
 */
public final class PersistedDataCorruptionCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create(); // same settings as Campaign.JSON

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
    /** True when t is the kind of failure the loader contract allows: a Gson parse failure or the loader's own IllegalStateException. */
    static boolean contained(Throwable t) {
        return t instanceof JsonParseException || t instanceof IllegalStateException;
    }

    // ---- The parties file: Campaign constructor, lines 80 to 82 ----
    static SocialHub loadParties(String text) {
        try {
            SocialHub social = JSON.fromJson(text, SocialHub.class);
            if (social == null) throw new IllegalStateException("Empty party data");
            social.restore();
            return social;
        } catch (Exception e) {
            throw new IllegalStateException("Party data could not be read; original and .bak are preserved", e);
        }
    }
    static void parties() {
        SocialHub seeded = new SocialHub();
        UUID a = UUID.randomUUID();
        seeded.createParty(a);
        String valid = JSON.toJson(seeded);
        SocialHub back = loadParties(valid);
        check(back.party(a) != null, "a valid parties file round-trips");
        int threw = 0;
        for (var e : badTexts(valid).entrySet()) {
            try {
                SocialHub hub = loadParties(e.getValue());
                // Loaded without throwing (for example a parseable but hollow object): must be fully usable after restore().
                check(hub.parties != null && hub.names != null && hub.rooms != null && hub.invites != null, "parties usable after " + e.getKey());
                hub.createParty(UUID.randomUUID());
            } catch (IllegalStateException refused) {
                threw++;
                check(refused.getMessage().contains("original and .bak are preserved"), "parties refusal names the preserved copies: " + e.getKey());
            } catch (Throwable t) {
                check(false, "parties: " + e.getKey() + " escaped as " + t);
            }
        }
        // TODO(Kort): a corrupt parties file stops the server at start (the Campaign constructor throws). It is never repaired, and the .bak
        // that save() keeps is never read back. Current behaviour is kept on purpose: refuse rather than silently drop every party.
        check(threw >= 8, "most corrupt parties files are refused loudly, not read as empty (" + threw + ")");
        // Parseable but hollow objects are accepted and normalised.
        check(loadParties("{}").parties.isEmpty(), "empty object reads as no parties");
        check(loadParties("{\"parties\":null,\"names\":null}").parties.isEmpty(), "null maps are replaced by restore()");
    }

    // ---- The commander profile: Campaign.profile, lines 91 to 93 ----
    static Profile loadProfile(Path file) {
        try {
            Profile p = ProfileMigration.upgradeFile(file, text -> JSON.fromJson(text, ProfileMigration.V1.class), text -> JSON.fromJson(text, Profile.class), JSON::toJson, RosterMath.Catalog.fromSpec());
            if (p == null) p = JSON.fromJson(Files.readString(file), Profile.class);
            if (p == null) throw new IllegalStateException("Empty profile");
            p.sanitize();
            return p;
        } catch (Exception e) {
            throw new IllegalStateException("Commander profile could not be read. Original and .bak preserved", e);
        }
    }
    static List<String> names(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) { return s.map(p -> p.getFileName().toString()).sorted().toList(); }
    }
    static Path writeProfile(Path root, String text) throws IOException {
        Path f = Files.createDirectories(root.resolve("probe-" + UUID.randomUUID())).resolve(UUID.randomUUID() + ".json");
        Files.writeString(f, text);
        return f;
    }
    static void profiles(Path root) throws IOException {
        Profile fresh = new Profile();
        fresh.sanitize();
        fresh.gold = 123;
        String valid = JSON.toJson(fresh);
        Path dir = Files.createDirectories(root.resolve("profiles"));
        Path good = dir.resolve(UUID.randomUUID() + ".json");
        Files.writeString(good, valid);
        check(loadProfile(good).gold == 123, "a valid current profile loads untouched");
        check(names(dir).size() == 1, "reading a current profile writes nothing");

        int threw = 0, read = 0, index = 0;
        for (var e : badTexts(valid).entrySet()) {
            Path d = Files.createDirectories(root.resolve("bad-" + (index++)));
            Path f = d.resolve(UUID.randomUUID() + ".json");
            byte[] bytes = e.getValue().getBytes(StandardCharsets.ISO_8859_1);
            Files.write(f, bytes);
            try {
                Profile p = loadProfile(f);
                read++;
                System.out.println("profile read without refusal: " + e.getKey());
                // A profile that parsed must be sane after sanitize(): defaults filled in, never null parts.
                check(p.owned != null && p.banner != null && p.progression != null && p.commander != null, "sanitized profile has no null parts after " + e.getKey());
            } catch (IllegalStateException refused) {
                threw++;
                check(contained(refused.getCause()), "profile failure is contained for " + e.getKey() + ": " + refused.getCause());
            } catch (Throwable t) {
                check(false, "profile: " + e.getKey() + " escaped as " + t);
            }
            // Failure must never cost the original: either the file is intact, or it was upgraded and its original bytes were kept as a backup.
            boolean intact = Arrays.equals(Files.readAllBytes(f), bytes);
            boolean backedUp = names(d).stream().anyMatch(n -> n.endsWith(".v1.json") || n.endsWith(".v2.json"));
            check(intact || backedUp, "no data loss for " + e.getKey());
            check(names(d).stream().noneMatch(n -> n.endsWith(".tmp")), "no stray .tmp left for " + e.getKey());
        }
        check(threw > 0, "some corrupt profiles are refused (" + threw + " refused, " + read + " read)");
        // TODO(Kort): a corrupt commander file makes profile(id) throw, so that player cannot join until the file is fixed by hand.
        // There is no automatic fallback to the .bak that save() keeps. Refusing is deliberate (it protects the original), but it is a
        // hard stop for one player. A schema-less JSON object with unknown or mismatched keys ("wrong-type nested" above) is not refused: it is
        // taken for a v1 profile and migrated to a default one, and only the .v1.json backup keeps the original (no loss, but silent).
        System.out.println("profile loader: " + threw + " refused, " + read + " read as defaults");

        // Unwritable target: the temp path is a directory, so the final write fails. The original must survive.
        Path d = Files.createDirectories(root.resolve("unwritable"));
        UUID id = UUID.randomUUID();
        Path f = d.resolve(id + ".json");
        Files.writeString(f, "{\"schema\":1,\"gold\":55,\"commander\":\"vanguard\"}");
        byte[] before = Files.readAllBytes(f);
        Files.createDirectory(d.resolve(id + ".tmp"));
        try {
            loadProfile(f);
            check(false, "an unwritable temp path must not look like success");
        } catch (IllegalStateException refused) {
            check(refused.getCause() instanceof IOException, "unwritable profile fails with an IOException: " + refused.getCause());
        }
        check(Arrays.equals(Files.readAllBytes(f), before), "unwritable temp path leaves the original profile untouched");
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
        // TODO(Kort): the copy does not validate, so a torn legacy file is carried over as it is; the profile loader then refuses it.
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
        try {
            parties();
            profiles(root);
            layoutMarker();
            legacy(root);
        } finally {
            try (Stream<Path> s = Files.walk(root)) { for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
        }
        System.out.println("PersistedDataCorruptionCheck: " + checks + " checks passed");
    }
}
