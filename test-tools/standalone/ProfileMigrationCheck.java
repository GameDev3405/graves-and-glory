package gg.gravesandglory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Pure Phase 2 profile v2 and the one-time v1 migration (spec 6.2 and 6.3), with parse and write stubs in place of Gson. Never shipped. */
public final class ProfileMigrationCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    static final RosterMath.Catalog cat = HousesCheck.cat; // the spec section 2 fixture catalog
    static final List<String> HOUSES = HousesCheck.HOUSES;
    static final Set<String> COMMONS = Set.of("knight", "archer", "revenant", "bonebow", "wm_hound", "wm_hunter", "ic_hearthguard", "ic_arbalist", "tc_raider", "tc_harpooner");
    static final Set<String> CLASSES = Set.of("champion", "reaver", "warden", "marshal", "warlock", "quartermaster");
    static final Set<String> SIDES = Set.of("gold", "violet", "crimson", "teal");
    static final int REFUND = Spec.ECONOMY[0].refundGold();

    static List<String> line(String spec) { return HousesCheck.banner(spec); }
    static boolean zeros(Map<String, Integer> counts) {
        Map<String, Integer> zero = new HashMap<>();
        for (String h : HOUSES) zero.put(h, 0);
        return zero.equals(counts);
    }
    /** A v1 profile as Campaign.Profile saved it: the six starters, the default Dawn and Dusk lines, gold 420, Vanguard; plus any extra unlocks. */
    static ProfileMigration.V1 v1(String... extra) {
        var v = new ProfileMigration.V1();
        v.gold = 420; v.matches = 31; v.wins = 17; v.commander = "vanguard"; v.tutorialDone = true; v.campaignCleared = 1;
        v.unlocked = new LinkedHashSet<>(List.of("knight", "archer", "mage", "revenant", "bonebow", "hexer"));
        v.unlocked.addAll(List.of(extra));
        v.army = new LinkedHashMap<>(Map.of("knight", 12, "archer", 6, "mage", 2, "revenant", 12, "bonebow", 6, "hexer", 2));
        v.deployment = new HashMap<>(Map.of("dawn", line("knight*12,archer*6,mage*2"), "dusk", line("revenant*12,bonebow*6,hexer*2")));
        v.progression = new Progression.State();
        return v;
    }
    static Profile toV2(ProfileMigration.V1 v) { return ProfileMigration.toV2(v, cat); }
    static String classOf(String v1Class) {
        var v = v1();
        v.commander = v1Class;
        return toV2(v).commander;
    }
    static Profile.Owned owned(int copies, int stars) {
        var o = new Profile.Owned();
        o.copies = copies; o.stars = stars;
        return o;
    }

    // File stubs: parse knows the v1 fixture text and any text holding "schema":2; anything else is corrupt.
    static final String ORIGINAL = "{\"gold\":420,\"commander\":\"vanguard\",\r\n \"unlocked\":[\"knight\",\"archer\"],\"note\":\"caf" + (char) 0xE9 + "\"}\r\n";
    static ProfileMigration.V1 parse(String text) {
        if (text.equals(ORIGINAL)) return v1();
        if (text.contains("\"schema\":2")) {
            var v = new ProfileMigration.V1();
            v.schema = 2;
            return v;
        }
        throw new IllegalArgumentException("not a profile");
    }
    static String write(Profile p) { return "{\"schema\":" + p.schema + ",\"gold\":" + p.gold + ",\"banner\":\"" + String.join(",", p.banner) + "\"}"; }
    static Profile upgrade(Path file) throws IOException { return ProfileMigration.upgradeFile(file, ProfileMigrationCheck::parse, ProfileMigrationCheck::write, cat); }

    public static void main(String[] args) throws IOException {
        // mig-fresh: the v1 defaults
        var fresh = toV2(v1());
        check(fresh.owned.keySet().equals(COMMONS) && fresh.owned.values().stream().allMatch(o -> o.copies == 1 && o.stars == 1), "mig-fresh: owned is exactly the 10 Commons at star 1; mage and hexer are refunded, not owned");
        check(fresh.banner.equals(line("knight*12,archer*6,empty*2")) && fresh.commander.equals("champion"), "mig-fresh: banner knight x12, archer x6, empty x2; commander champion");
        check(REFUND == 300 && fresh.gold == 420 + 2 * REFUND, "mig-fresh: gold is v1 gold + 600 (economy.refundGold 300 each for mage and hexer)");
        check(fresh.matches == 31 && fresh.wins == 17 && fresh.campaignCleared == 1 && fresh.tutorialDone, "mig-fresh: matches, wins, campaignCleared and tutorialDone are copied");
        check(zeros(fresh.shards) && zeros(fresh.renown) && zeros(fresh.pity) && fresh.schema == 2 && fresh.writs == 0, "mig-fresh: five House keys at 0 in shards, renown and pity; schema 2");
        var starters = v1();
        starters.unlocked = new LinkedHashSet<>(List.of("knight"));
        check(toV2(starters).gold == 420 + 2 * REFUND && toV2(starters).owned.keySet().equals(COMMONS), "mig-fresh: mage and hexer are refunded even when the v1 file does not list them (v1 starters)");
        // mig-full: all 12 v1 ids unlocked
        var full = toV2(v1("sentinel", "longbow", "oracle", "reaper", "stalker", "wraith"));
        Set<String> fullOwned = new HashSet<>(COMMONS);
        fullOwned.addAll(List.of("sentinel", "longbow", "reaper", "stalker"));
        check(full.owned.keySet().equals(fullOwned) && Collections.disjoint(full.owned.keySet(), Set.of("mage", "hexer", "oracle", "wraith")), "mig-full: adds sentinel, longbow, reaper and stalker; no mage, hexer, oracle or wraith key");
        check(full.gold == 420 + 4 * REFUND, "mig-full: gold is v1 gold + 1200 (300 each for mage, hexer, oracle and wraith)");
        // mig-invalid-dawn, mig-both-invalid
        var shortDawn = v1();
        shortDawn.deployment.put("dawn", line("knight*7"));
        check(toV2(shortDawn).banner.equals(line("revenant*12,bonebow*6,empty*2")), "mig-invalid-dawn: a Dawn line of 7 falls back to the Dusk line, repacked to revenant x12, bonebow x6, empty x2");
        var neither = v1();
        neither.deployment.put("dawn", line("knight*7"));
        neither.deployment.put("dusk", line("empty*20"));
        var wrongSide = v1();
        wrongSide.deployment.put("dawn", line("knight*12,revenant*8"));
        wrongSide.deployment.remove("dusk");
        var none = v1();
        none.deployment = null;
        check(toV2(neither).banner.equals(RosterMath.starterBanner()) && toV2(wrongSide).banner.equals(RosterMath.starterBanner()) && toV2(none).banner.equals(RosterMath.starterBanner()), "mig-both-invalid: a short, empty, wrong-side or missing line gives starterBanner()");
        // mig-class
        check(classOf("marksman").equals("warden") && classOf("arcanist").equals("warlock") && classOf("vanguard").equals("champion") && classOf("paladin").equals("champion") && classOf(null).equals("champion"), "mig-class: marksman to warden, arcanist to warlock, vanguard and unknown to champion");
        // mig-table, mig-map: the spec 6.3 id table and the section 5 class map, and Dawn ids mapped through the table
        check(ProfileMigration.ID_MAP.equals(Map.ofEntries(Map.entry("knight", "knight"), Map.entry("archer", "archer"), Map.entry("mage", "mage"), Map.entry("sentinel", "sentinel"), Map.entry("longbow", "longbow"), Map.entry("oracle", "mage"),
                Map.entry("revenant", "revenant"), Map.entry("bonebow", "bonebow"), Map.entry("hexer", "hexer"), Map.entry("reaper", "reaper"), Map.entry("stalker", "stalker"), Map.entry("wraith", "hexer")))
            && ProfileMigration.CLASS_MAP.equals(Map.of("vanguard", "champion", "marksman", "warden", "arcanist", "warlock")), "mig-table: ID_MAP is the spec 6.3 table (oracle to mage, wraith to hexer) and CLASS_MAP the section 5 one");
        var mapped = v1("sentinel", "longbow", "oracle");
        mapped.deployment.put("dawn", line("sentinel*4,longbow*4,oracle*2,knight*10"));
        check(toV2(mapped).banner.equals(line("sentinel*4,longbow*4,knight*10,empty*2")), "mig-map: Dawn ids map through ID_MAP; oracle becomes mage, which is not owned, so the repack drops it");
        // mig-progression
        var veteran = v1();
        var state = veteran.progression;
        state.xp = 4321; state.claims.addAll(List.of("founders:20", "daily:0")); state.titles.addAll(List.of("Scout", "Vanguard")); state.title = "Vanguard";
        var moved = toV2(veteran).progression;
        check(moved.xp == 4321 && moved.claims.equals(Set.of("founders:20", "daily:0")) && moved.titles.equals(Set.of("Recruit", "Scout", "Vanguard")) && moved.title.equals("Vanguard"), "mig-progression: xp, claims and titles (Vanguard too) are copied unchanged");
        // profile defaults and the sanitizer
        var blank = new Profile();
        check(blank.schema == 2 && blank.banner.equals(RosterMath.starterBanner()) && blank.commander.equals("champion") && blank.bannerColour.equals("gold"), "profile-default: schema 2, the starter banner, champion, gold");
        check(blank.owned.keySet().equals(COMMONS) && new ArrayList<>(blank.owned.keySet()).equals(List.of("knight", "archer", "revenant", "bonebow", "wm_hound", "wm_hunter", "ic_hearthguard", "ic_arbalist", "tc_raider", "tc_harpooner"))
            && blank.owned.values().stream().allMatch(o -> o.copies == 1 && o.stars == 1) && RosterMath.validBanner(blank.banner, cat, blank.owned.keySet()) == null, "profile-default: one copy of each of the 10 Commons, in sheet order, so the starter banner is valid");
        var p = new Profile();
        p.owned.clear();
        p.owned.put("knight", owned(0, 0)); p.owned.put("mage", owned(2, 7)); p.owned.put("oracle", owned(1, 1)); p.owned.put("archer", null);
        p.owned.get("knight").mastery = 9; p.owned.get("mage").mastery = -1;
        p.banner = new ArrayList<>(line("knight*12,oracle,mage*2"));
        p.commander = "vanguard"; p.bannerColour = "dawn";
        p.shards = new HashMap<>(Map.of("gilded", -5, "bogus", 3)); p.renown = null; p.pity = new HashMap<>();
        p.gold = -4; p.matches = 3; p.wins = 9; p.progression = null;
        p.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(p.owned.keySet().equals(Set.of("knight", "mage")), "sanitize: unknown ids and empty entries are dropped from owned");
        var knight = p.owned.get("knight");
        var mage = p.owned.get("mage");
        check(knight.copies == 1 && knight.stars == 1 && mage.stars == 3, "sanitize: stars clamp to 1..3");
        check(Arrays.stream(Profile.Owned.class.getDeclaredFields()).noneMatch(f -> f.getName().equals("stage")) && Arrays.stream(Profile.class.getDeclaredMethods()).noneMatch(m -> m.getName().equals("lastStage")), "profile-no-stage: Owned has no stage field and Profile no lastStage, so a stored stage is never read (Gson ignores the key) and never written (the in-game migration suite round-trips a stage:3 file)");
        check(knight.mastery == 3 && mage.mastery == 0, "sanitize: mastery clamps to 0..3, the last mastery level (9 becomes 3, -1 becomes 0)");
        check(RosterMath.validBanner(p.banner, cat, p.owned.keySet()) == null && p.banner.equals(line("knight*12,mage,^,mage,^,empty*4")), "sanitize: a banner that fails validBanner is repacked");
        check(p.commander.equals("champion") && p.bannerColour.equals("gold"), "sanitize: an unknown class maps to champion and an unknown colour to gold");
        check(zeros(p.shards) && zeros(p.renown) && zeros(p.pity), "sanitize: shards, renown and pity hold the five House keys at 0 or more, and nothing else");
        check(p.gold == 0 && p.wins == 3 && p.progression != null && p.schema == 2, "sanitize: gold and wins clamp; a missing progression is rebuilt");
        var valid = new Profile();
        for (String id : COMMONS) valid.owned.put(id, owned(1, 1));
        valid.banner = new ArrayList<>(line("revenant*12,bonebow*8"));
        valid.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(valid.banner.equals(line("revenant*12,bonebow*8")), "sanitize: a valid banner is left alone");
        // the file upgrade
        Path dir = Files.createTempDirectory("gravesandglory-profile-migration-check-");
        try {
            byte[] original = ORIGINAL.getBytes(StandardCharsets.UTF_8);
            Path file = dir.resolve("a1.json"), backup = dir.resolve("a1.v1.json"), tmp = dir.resolve("a1.tmp");
            Files.write(file, original);
            Profile migrated = upgrade(file);
            check(migrated != null && migrated.schema == 2 && Arrays.equals(Files.readAllBytes(backup), original), "mig-file: <id>.v1.json is a byte copy of the original");
            check(Files.readString(file).contains("\"schema\":2") && Files.readString(file).equals(write(migrated)) && !Files.exists(tmp), "mig-file: <id>.json is the new schema 2 file, written through a temp file and a move");
            byte[] backupBytes = Files.readAllBytes(backup), fileBytes = Files.readAllBytes(file);
            check(upgrade(file) == null && Arrays.equals(Files.readAllBytes(backup), backupBytes) && Arrays.equals(Files.readAllBytes(file), fileBytes), "mig-idempotent: a second call returns null and both files are byte-unchanged");
            Path again = dir.resolve("b2.json"), oldBackup = dir.resolve("b2.v1.json");
            Files.write(again, original);
            Files.writeString(oldBackup, "an earlier v1 copy");
            check(upgrade(again) != null && Files.readString(oldBackup).equals("an earlier v1 copy") && Files.readString(again).contains("\"schema\":2"), "mig-backup-once: an existing <id>.v1.json is never replaced");
            byte[] corrupt = "{\"gold\":42".getBytes(StandardCharsets.UTF_8);
            Path bad = dir.resolve("c3.json");
            Files.write(bad, corrupt);
            boolean threw = false;
            try { upgrade(bad); } catch (IllegalStateException expected) { threw = true; }
            check(threw && Arrays.equals(Files.readAllBytes(bad), corrupt) && !Files.exists(dir.resolve("c3.v1.json")) && !Files.exists(dir.resolve("c3.tmp")), "mig-corrupt: a parse failure throws IllegalStateException; the original is unchanged, with no .v1.json and no .tmp");
            Path stuck = dir.resolve("d4.json");
            Files.write(stuck, original);
            Files.createDirectory(dir.resolve("d4.tmp")); // <id>.tmp cannot be written as a file
            boolean failed = false;
            try { upgrade(stuck); } catch (IOException expected) { failed = true; }
            check(failed && Arrays.equals(Files.readAllBytes(stuck), original), "mig-tmp-fail: when <id>.tmp cannot be written the original <id>.json is left unchanged");
        } finally {
            try (Stream<Path> paths = Files.walk(dir)) { paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
        }
        System.out.println("ProfileMigrationCheck: " + checks + " checks passed");
    }
}
