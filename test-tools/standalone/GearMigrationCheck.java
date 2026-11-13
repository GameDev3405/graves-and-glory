package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Gear foundation Task 4: the schema 3 to 4 migration through the real Gson loaders (the same steps as Campaign.readProfile), the
 * backups it keeps, a second load that changes nothing, the v1 chain, the gear half of Profile.sanitize and the fresh starter. Never shipped.
 */
public final class GearMigrationCheck {
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
    static Gear.Piece piece(int uid, String item, int quality, int durability) {
        return new Gear.Piece(uid, item, "common", quality, durability);
    }
    static Set<String> items(Profile p) {
        Set<String> out = new TreeSet<>();
        for (var g : p.gear) out.add(g.item);
        return out;
    }

    public static void main(String[] args) throws Exception {
        Path fixture = Paths.get("test-tools/resources/fixtures/profile-v3-veteran.json");
        byte[] v3Bytes = Files.readAllBytes(fixture);
        Path root = Files.createTempDirectory("gg-gear-migration");
        try {
            // mig4-fixture
            Profile before = JSON.fromJson(new String(v3Bytes, StandardCharsets.UTF_8), Profile.class);
            Path file = root.resolve("vet.json"), backup = root.resolve("vet.v3.json");
            Files.write(file, v3Bytes);
            Profile p = load(file);
            check(p.schema == Profile.SCHEMA, "schema is current, got " + p.schema);
            check(p.gold == null && p.troopWrits == 9 && p.gearShards == 50 && p.commander.equals("marshal"), "950 gold is 9 troop writs and 50 gear shards; the commander is unchanged");
            check(before.battlesWon.equals(Set.of("march-1")) && p.battlesWon.isEmpty(), "battlesWon: the gear step keeps it; the chapter 1 reset (schema 6 step) then empties the march-era win: " + p.battlesWon);
            check(p.owned.keySet().equals(before.owned.keySet()) && new TreeMap<>(p.table().values().stream().collect(java.util.stream.Collectors.groupingBy(t -> t, java.util.stream.Collectors.counting()))).equals(new TreeMap<>(before.banner.stream().filter(t -> !t.equals("empty") && !t.equals("^")).collect(java.util.stream.Collectors.groupingBy(t -> t, java.util.stream.Collectors.counting())))), "owned troops are unchanged and the banner troops moved onto the war table: " + p.table());
            check(p.gear.size() == 5, "a marshal gets 5 starter pieces, got " + p.gear.size());
            check(items(p).equals(new TreeSet<>(List.of("commander_helm", "commander_chest", "commander_legs", "commander_boots", "war_banner"))), "the commander set and the banner: " + items(p));
            check(p.equipped.size() == 5 && p.equipped.containsKey("main") && !p.equipped.containsKey("off"), "all 5 pieces are worn");
            check(p.relics.equals(Set.of("rally_horn")) && p.relicSlots.equals(List.of("rally_horn", "", "")), "rally_horn in slot 0");
            check(p.gearShards == 50 && p.gearPity == 0 && p.inventorySize == Spec.FORGE[0].startSlots(), "the old gold remainder is the only shards, no pity, the starting inventory");
            check(p.gearPower == 5, "gear power is 5, got " + p.gearPower);
            check(Files.isRegularFile(backup) && Arrays.equals(Files.readAllBytes(backup), v3Bytes), ".v3.json is a byte copy of the fixture");
            check(Files.readString(file).contains("\"schema\": 4"), "the file on disk is schema 4");

            // mig4-idempotent
            byte[] saved = Files.readAllBytes(file);
            Profile again = load(file);
            check(Arrays.equals(Files.readAllBytes(file), saved) && again.gear.size() == 5, "a second load changes nothing");
            check(Arrays.equals(Files.readAllBytes(backup), v3Bytes), "the backup is untouched by a second load");
            Files.write(file, v3Bytes); // the schema 3 file comes back with a backup already in place
            Files.write(backup, "KEEP".getBytes(StandardCharsets.UTF_8));
            Profile redo = load(file);
            check(new String(Files.readAllBytes(backup), StandardCharsets.UTF_8).equals("KEEP"), "an existing backup is never replaced");
            check(redo.gear.size() == 5 && redo.schema == Profile.SCHEMA, "the migrated again profile has 5 pieces, not 10: " + redo.gear.size());
            try (Stream<Path> s = Files.list(root)) { check(s.noneMatch(x -> x.getFileName().toString().endsWith(".tmp")), "no .tmp left behind"); }

            // mig4-chain
            byte[] v1Bytes = Files.readAllBytes(Paths.get("test-tools/resources/fixtures/profile-v1-full.json"));
            Path chain = root.resolve("chain.json");
            Files.write(chain, v1Bytes);
            Profile c = load(chain);
            check(c.schema == Profile.SCHEMA && Files.readString(chain).contains("\"schema\": 4"), "a v1 file ends at the current schema (the file at 4, sanitize brings the rest)");
            check(Arrays.equals(Files.readAllBytes(root.resolve("chain.v1.json")), v1Bytes), ".v1.json keeps the original");
            check(Files.readString(root.resolve("chain.v2.json")).contains("\"schema\": 2"), ".v2.json keeps the schema 2 image");
            check(Files.readString(root.resolve("chain.v3.json")).contains("\"schema\": 3"), ".v3.json keeps the schema 3 image");
            check(c.gear.size() >= 5 && c.equipped.size() == c.gear.size() && c.relics.size() == 1, "the chained profile carries a starter set: " + c.gear.size());
            // a schema 2 file keeps its own bytes as .v2.json and the intermediate as .v3.json
            byte[] v2Bytes = Files.readAllBytes(root.resolve("chain.v2.json"));
            Path two = root.resolve("two.json");
            Files.write(two, v2Bytes);
            Profile t = load(two);
            check(t.schema == Profile.SCHEMA && Arrays.equals(Files.readAllBytes(root.resolve("two.v2.json")), v2Bytes), "a schema 2 file keeps .v2.json");
            check(Files.readString(root.resolve("two.v3.json")).contains("\"schema\": 3"), "and gains a .v3.json image");

            // sanitize-gear
            Profile s = new Profile();
            s.gear.add(piece(1, "bruiser_helm", 140, -3));
            s.gear.add(piece(2, "nope", 50, 50));
            s.gear.add(piece(1, "bruiser_boots", 50, 50)); // repeated uid
            s.gear.add(null);
            s.gear.add(new Gear.Piece(3, "bruiser_chest", "mythic", 50, 50)); // unknown rarity
            s.gear.add(piece(4, "bruiser_chest", 50, 500));
            s.equipped.put("helm", 4); // stale: points at a chest piece
            s.equipped.put("chest", 4);
            s.equipped.put("legs", 99);
            s.relics.addAll(List.of("dash", "nope"));
            s.relicSlots = new ArrayList<>(List.of("dash", "dash"));
            s.inventorySize = 77;
            s.gearShards = -5;
            s.gearPity = 99;
            s.sanitize();
            check(s.gear.size() == 2, "the unknown item, repeated uid, null and unknown rarity are dropped: " + s.gear.size());
            check(s.gear.get(0).uid == 1 && s.gear.get(0).quality == 100 && s.gear.get(0).durability == 0, "quality 140 becomes 100 and durability -3 becomes 0");
            check(s.gear.get(1).uid == 4 && s.gear.get(1).durability == Spec.FORGE[0].maxDurability(), "durability clamps to the maximum");
            check(!s.equipped.containsKey("helm") && !s.equipped.containsKey("legs") && s.equipped.get("chest") == 4 && s.equipped.size() == 1, "stale equipped entries are dropped: " + s.equipped);
            check(s.relics.equals(Set.of("dash")), "unknown relics are dropped");
            check(s.relicSlots.equals(List.of("dash", "", "")), "relicSlots is exactly 3 with no repeats: " + s.relicSlots);
            check(s.inventorySize == 70, "77 rounds down to 70, got " + s.inventorySize);
            check(s.gearShards == 0 && s.gearPity == Spec.FORGE[0].pity(), "marks and pity clamp");
            check(s.nextGearUid >= 5, "next uid is past the highest, got " + s.nextGearUid);
            check(s.gearPower == Gear.power(s) && s.gearPower == 1, "gear power is recomputed: " + s.gearPower);
            s.inventorySize = 500;
            s.sanitize();
            check(s.inventorySize == 120, "500 becomes the 120 maximum, got " + s.inventorySize);
            s.inventorySize = 3;
            s.sanitize();
            check(s.inventorySize == Spec.FORGE[0].startSlots(), "a small inventory rises to the start size");
            Profile two2 = new Profile();
            two2.gear.add(piece(1, "greatsword", 50, 100));
            two2.gear.add(piece(2, "kite_shield", 50, 100));
            two2.equipped.put("main", 1);
            two2.equipped.put("off", 2);
            two2.sanitize();
            check(two2.equipped.containsKey("main") && !two2.equipped.containsKey("off"), "a two-handed main drops the off-hand");
            Profile big = new Profile();
            for (int i = 1; i <= 130; i++) big.gear.add(piece(i, "bruiser_helm", 50, 100));
            big.sanitize();
            check(big.gear.size() == 130, "a profile over its inventory keeps every piece: " + big.gear.size());
            Profile nulls = new Profile();
            nulls.gear = null; nulls.equipped = null; nulls.relics = null; nulls.relicSlots = null;
            nulls.sanitize();
            check(nulls.gear.isEmpty() && nulls.equipped.isEmpty() && nulls.relicSlots.equals(List.of("", "", "")), "null gear fields become empty");

            // fresh
            Profile f = new Profile();
            Gear.grantStarter(f);
            f.sanitize();
            check(f.gear.size() == 6 && f.relics.equals(Set.of("stand_fast")) && f.relicSlots.get(0).equals("stand_fast"), "a fresh champion has the 6 bruiser pieces and stand_fast");
            check(f.schema == Profile.SCHEMA && f.gearPower == 6, "fresh is the current schema with gear power 6");

            // newer-save: a schema-8 file (newer than Profile.SCHEMA) survives a load and a save attempt byte for byte, and the profile is read-only for the session
            String future = "{\"schema\": 8, \"gold\": 123, \"futureField\": {\"a\": [1, 2, 3]}}";
            Path newer = root.resolve("future.json");
            Files.writeString(newer, future);
            byte[] futureBytes = Files.readAllBytes(newer);
            Profile fut = load(newer);
            check(fut.readOnly && fut.schema == 8, "a schema-8 profile loads read-only and keeps schema 8, got " + fut.schema);
            check(!JSON.toJson(fut).contains("readOnly"), "the read-only flag is never saved");
            // C01-F2: a JSON snapshot restored the way Campaign rolls back (claim, roll, payout) stays read-only, so a later save cannot overwrite the newer file
            Profile copy = Profile.restored(JSON.fromJson(JSON.toJson(fut), Profile.class));
            check(copy.readOnly && copy.schema == 8, "a restored schema-8 snapshot stays read-only and keeps schema 8, got readOnly=" + copy.readOnly + " schema=" + copy.schema);
            fut.gold = 999;
            ProfileMigration.saveProfile(fut, newer, JSON.toJson(fut));
            check(Arrays.equals(Files.readAllBytes(newer), futureBytes), "the save attempt leaves the schema-8 file byte for byte");
            check(!Files.exists(root.resolve("future.json.bak")) && !Files.exists(root.resolve("future.json.tmp")) && !Files.exists(root.resolve("future.v3.json")), "no backup or temp file appears beside it");
            Path normal = root.resolve("normal.json");
            Profile ok = new Profile();
            ok.sanitize();
            check(!ok.readOnly && ok.schema == Profile.SCHEMA, "a current profile is not read-only");
            ok.gearShards = 77;
            ProfileMigration.saveProfile(ok, normal, JSON.toJson(ok));
            check(JSON.fromJson(Files.readString(normal), Profile.class).gearShards == 77, "a current profile saves");

            // import: the gear fields reset, the inventory back to the starting size
            Profile imp = new Profile();
            imp.sanitize();
            imp.inventorySize = 120;
            imp.gearShards = 500;
            imp.gearPity = 9;
            for (int i = 1; i <= 30; i++) imp.gear.add(piece(i, "bruiser_helm", 50, 100));
            imp.nextGearUid = 31;
            Gear.resetForImport(imp);
            check(imp.inventorySize == Spec.FORGE[0].startSlots() && imp.gearShards == 0 && imp.gearPity == 0, "an import resets the inventory size, marks and pity");
            check(imp.gear.size() == 6 && imp.equipped.size() == 6 && imp.nextGearUid == 7 && imp.relics.size() == 1, "an import gets a fresh starter set only");
        } finally {
            try (Stream<Path> w = Files.walk(root)) { for (Path x : w.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(x); }
        }
        System.out.println("GearMigrationCheck: " + checks + " checks passed");
    }
}
