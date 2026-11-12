package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * QA matrix D16: every checked-in v1 profile fixture (fresh, full, invalid, partial, cache) goes through the real Gson loaders that
 * Campaign.readProfile uses, on a copy in a temp folder, and must come out as a valid current-schema profile. The v1 bytes are kept once as
 * name.v1.json, a second load changes nothing, and the refunded troops (mage, hexer, oracle, wraith) are never owned. Never shipped.
 */
public final class ProfileFixtureCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create(); // same settings as Campaign.JSON

    static Profile load(Path file) throws Exception {
        Profile p = ProfileMigration.upgradeFile(file, text -> JSON.fromJson(text, ProfileMigration.V1.class), text -> JSON.fromJson(text, Profile.class), JSON::toJson, RosterMath.Catalog.fromSpec());
        if (p == null) p = JSON.fromJson(Files.readString(file), Profile.class);
        if (p == null) throw new IllegalStateException("Empty profile");
        p.sanitize();
        return p;
    }

    public static void main(String[] args) throws Exception {
        Path fixtures = Paths.get("test-tools/resources/fixtures");
        Set<String> commons = new LinkedHashSet<>();
        for (var t : Spec.TROOPS) if (t.rarity().equals("common")) commons.add(t.id());
        check(commons.size() == 10, "the catalog has 10 Commons: " + commons.size());
        List<String> names = List.of("fresh", "full", "invalid", "partial", "cache");
        Path root = Files.createTempDirectory("gg-fixture-check");
        try {
            for (String name : names) {
                Path source = fixtures.resolve("profile-v1-" + name + ".json");
                check(Files.isRegularFile(source), "fixture exists: " + source);
                byte[] original = Files.readAllBytes(source);
                Path file = root.resolve(name + ".json"), backup = root.resolve(name + ".v1.json");
                Files.write(file, original);
                V1Probe v1 = new V1Probe(JSON.fromJson(new String(original, java.nio.charset.StandardCharsets.UTF_8), ProfileMigration.V1.class));
                Profile p = load(file);
                check(p.schema == Profile.SCHEMA, name + ": migrates to the current schema, got " + p.schema);
                check(p.owned.keySet().containsAll(commons), name + ": owns all 10 Commons");
                check(Collections.disjoint(p.owned.keySet(), ProfileMigration.REFUNDED), name + ": mage, hexer, oracle and wraith are refunded, never owned");
                check(p.owned.values().stream().allMatch(o -> o.copies >= 1 && o.stars >= 1), name + ": every owned troop has a copy and a star");
                check(p.banner == null && p.banners.size() == 1 && p.line().size() == WarGrid.cells() && !p.table().isEmpty(), name + ": one banner on the 54-cell war table: " + p.table());
                check(p.commander != null && Set.of("champion", "warden", "warlock").contains(p.commander), name + ": commander is a v2 class: " + p.commander);
                check(p.gold == null && p.troopWrits * 100 + p.gearShards >= v1.gold, name + ": gold (now troop writs and gear shards) is at least the v1 gold plus the refunds: " + p.troopWrits + "/" + p.gearShards + " vs " + v1.gold);
                check(p.battlesWon != null && p.bonusClaimed != null, name + ": battlesWon and bonusClaimed exist");
                check(Files.isRegularFile(backup) && Arrays.equals(Files.readAllBytes(backup), original), name + ": .v1.json is a byte copy of the fixture");
                check(Files.readString(file).contains("\"schema\": 4"), name + ": the file on disk is schema 4");
                byte[] saved = Files.readAllBytes(file);
                Profile again = load(file);
                check(again.troopWrits == p.troopWrits && again.gearShards == p.gearShards && again.pacts == p.pacts && Arrays.equals(Files.readAllBytes(file), saved) && Arrays.equals(Files.readAllBytes(backup), original), name + ": a second load changes nothing");
                try (Stream<Path> s = Files.list(root)) {
                    check(s.noneMatch(x -> x.getFileName().toString().endsWith(".tmp")), name + ": no .tmp left behind");
                }
            }
            // the later-schema fixtures: the four v6 files, the v3 veteran and the v2 QA seed load at the current schema with a valid table
            for (String name : List.of("profile-v6-starter", "profile-v6-wide", "profile-v6-over", "profile-v6-gold", "profile-v3-veteran")) loadsClean(fixtures.resolve(name + ".json"), root, name);
            loadsClean(Paths.get("test-tools/qa/profile-v2-seed.json"), root, "profile-v2-seed");
        } finally {
            try (Stream<Path> s = Files.walk(root)) { for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p); }
        }
        System.out.println("ProfileFixtureCheck: " + checks + " checks passed");
    }

    static void loadsClean(Path source, Path root, String name) throws Exception {
        check(Files.isRegularFile(source), "fixture exists: " + source);
        Path file = root.resolve(name + ".later.json");
        Files.copy(source, file);
        Profile p = load(file);
        check(p.schema == Profile.SCHEMA && !p.readOnly && p.gold == null && p.writs == null && p.banner == null, name + ": loads at the current schema with the legacy fields gone");
        check(!p.banners.isEmpty() && RosterMath.validTable(p.table(), RosterMath.Catalog.fromSpec(), p.owned.keySet(), RosterMath.fieldableHouses()) == null, name + ": the active table passes the validator: " + RosterMath.validTable(p.table(), RosterMath.Catalog.fromSpec(), p.owned.keySet(), RosterMath.fieldableHouses()));
        check(p.troopWrits >= 0 && p.gearShards >= 0 && p.pacts >= 0, name + ": purses are not negative");
    }

    record V1Probe(int gold) {
        V1Probe(ProfileMigration.V1 v) { this(v.gold); }
    }
}
