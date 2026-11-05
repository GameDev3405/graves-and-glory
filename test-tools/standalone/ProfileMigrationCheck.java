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

    // File stubs: parse knows the v1 fixture text and any text holding "schema":N; parseV2 and write carry the fields the upgrade
    // touches (gold, banner, campaignCleared, writs, writChapters, battlesWon, bonusClaimed); anything else is corrupt.
    static final String ORIGINAL = "{\"gold\":420,\"commander\":\"vanguard\",\r\n \"unlocked\":[\"knight\",\"archer\"],\"note\":\"caf" + (char) 0xE9 + "\"}\r\n";
    static ProfileMigration.V1 parse(String text) {
        if (text.equals(ORIGINAL)) return v1();
        var m = java.util.regex.Pattern.compile("\"schema\":(\\d+)").matcher(text);
        if (m.find()) {
            var v = new ProfileMigration.V1();
            v.schema = Integer.parseInt(m.group(1));
            return v;
        }
        throw new IllegalArgumentException("not a profile");
    }
    static String field(String text, String key) {
        var m = java.util.regex.Pattern.compile("\"" + key + "\":\"([^\"]*)\"").matcher(text);
        return m.find() ? m.group(1) : "";
    }
    static int number(String text, String key) {
        var m = java.util.regex.Pattern.compile("\"" + key + "\":(-?\\d+)").matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
    static <T extends Collection<String>> T items(String text, String key, T into) {
        for (String s : field(text, key).split(",")) if (!s.isEmpty()) into.add(s);
        return into;
    }
    static Profile parseV2(String text) {
        var p = new Profile();
        p.schema = number(text, "schema"); p.gold = number(text, "gold"); p.campaignCleared = number(text, "campaignCleared"); p.writs = number(text, "writs");
        if (text.contains("\"banner\"")) p.banner = items(text, "banner", new ArrayList<>());
        p.writChapters = new LinkedHashSet<>();
        for (String c : items(text, "writChapters", new ArrayList<>())) p.writChapters.add(Integer.parseInt(c));
        p.battlesWon = text.contains("\"battlesWon\"") ? items(text, "battlesWon", new LinkedHashSet<>()) : null; // a schema-2 file has no such key: Gson leaves it null
        p.bonusClaimed = text.contains("\"bonusClaimed\"") ? items(text, "bonusClaimed", new LinkedHashSet<>()) : null;
        p.customGroups = text.contains("\"customGroups\"") ? groups(field(text, "customGroups")) : null; // a schema-4 file has no such key: Gson leaves it null
        return p;
    }
    static String write(Profile p) {
        String text = "{\"schema\":" + p.schema + ",\"gold\":" + p.gold + ",\"banner\":\"" + String.join(",", p.banner) + "\",\"campaignCleared\":" + p.campaignCleared + ",\"writs\":" + p.writs
            + ",\"writChapters\":\"" + String.join(",", p.writChapters.stream().map(String::valueOf).toList()) + "\"";
        if (p.schema >= 3) text += ",\"battlesWon\":\"" + String.join(",", p.battlesWon) + "\",\"bonusClaimed\":\"" + String.join(",", p.bonusClaimed) + "\"";
        if (p.schema >= 5) text += ",\"customGroups\":\"" + String.join(";", p.customGroups.stream().map(g -> g.number() + "|" + g.name() + "|" + String.join("+", g.troopIds())).toList()) + "\"";
        return text + "}";
    }
    /** The stub form of customGroups: number|name|id+id entries joined by ;. */
    static List<Profile.CustomGroup> groups(String text) {
        List<Profile.CustomGroup> out = new ArrayList<>();
        for (String g : text.split(";")) {
            if (g.isEmpty()) continue;
            String[] f = g.split("[|]", -1);
            out.add(new Profile.CustomGroup(Integer.parseInt(f[0]), f[1], f[2].isEmpty() ? List.of() : List.of(f[2].split("[+]"))));
        }
        return out;
    }
    static Profile upgrade(Path file) throws IOException { return ProfileMigration.upgradeFile(file, ProfileMigrationCheck::parse, ProfileMigrationCheck::parseV2, ProfileMigrationCheck::write, cat); }
    static final String V2_SAVE = "{\"schema\":2,\"gold\":77,\"banner\":\"knight,archer\",\"campaignCleared\":1,\"writs\":1,\"writChapters\":\"1\"}";
    static Set<String> march(int n) {
        Set<String> ids = new LinkedHashSet<>();
        for (int i = 1; i <= n; i++) ids.add("march-" + i);
        return ids;
    }

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
        check(blank.schema == 5 && blank.banner.equals(RosterMath.starterBanner()) && blank.commander.equals("champion") && blank.bannerColour.equals("gold"), "profile-default: schema 5, the starter banner, champion, gold");
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
        check(p.gold == 0 && p.wins == 3 && p.progression != null && p.schema == 5, "sanitize: gold and wins clamp; a missing progression is rebuilt");
        var valid = new Profile();
        for (String id : COMMONS) valid.owned.put(id, owned(1, 1));
        valid.banner = new ArrayList<>(line("revenant*12,bonebow*8"));
        valid.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(valid.banner.equals(line("revenant*12,bonebow*8")), "sanitize: a valid banner is left alone");
        // schema 3 (plan 2C.3): a fresh save, v2toV3 in memory, the sanitizer and a round trip
        var newSave = new Profile();
        newSave.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(newSave.schema == 5 && newSave.customGroups.isEmpty() && newSave.battlesWon.isEmpty() && newSave.bonusClaimed.isEmpty() && newSave.campaignCleared == 0, "fresh-save: schema 5, no custom group, no battle won, no bonus claimed, campaignCleared 0");
        var old = parseV2(V2_SAVE.replace("\"campaignCleared\":1", "\"campaignCleared\":6"));
        check(ProfileMigration.v2toV3(old, Spec.CAMPAIGNBATTLES) == old && old.schema == 3 && old.battlesWon.equals(march(6)) && old.bonusClaimed.isEmpty() && old.writs == 1, "v2toV3: in place; campaignCleared 6 gives all six battles, writs kept");
        var unmigrated = parseV2(V2_SAVE);
        unmigrated.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(unmigrated.schema == 5 && unmigrated.battlesWon.equals(march(1)) && unmigrated.campaignCleared == 1, "sanitize: a schema-2 profile that skipped the file upgrade runs v2toV3 first");
        var broken = new Profile();
        broken.battlesWon = new LinkedHashSet<>(List.of("march-1", "march-3", "march-9")); broken.bonusClaimed = new LinkedHashSet<>(List.of("b1_hold_all", "b_bogus")); broken.campaignCleared = 5;
        broken.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(broken.battlesWon.equals(Set.of("march-1", "march-3")) && broken.bonusClaimed.equals(Set.of("b1_hold_all")) && broken.campaignCleared == 1, "sanitize: unknown battle and bonus ids are dropped; campaignCleared is the unbroken run from march-1 (1, not 5)");
        var nulls = new Profile();
        nulls.battlesWon = null; nulls.bonusClaimed = null;
        nulls.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(nulls.battlesWon != null && nulls.bonusClaimed != null, "sanitize: missing battlesWon and bonusClaimed are rebuilt empty");
        var saved = new Profile();
        saved.battlesWon = new LinkedHashSet<>(march(2)); saved.bonusClaimed = new LinkedHashSet<>(List.of("b1_hold_all")); saved.writs = 0; saved.gold = 55;
        saved.sanitize(cat, CLASSES, SIDES, HOUSES);
        var loaded = parseV2(write(saved));
        loaded.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(write(loaded).equals(write(saved)) && loaded.battlesWon.equals(march(2)) && loaded.bonusClaimed.equals(Set.of("b1_hold_all")) && loaded.campaignCleared == 2 && loaded.schema == 5, "round-trip: a schema 5 save loads and saves back unchanged (battlesWon, bonusClaimed, campaignCleared 2)");
        // schema 5 (command UI custom groups): a schema-4 file loads with no custom group and saves as schema 5; a schema-5 file round-trips
        String v4Save = "{\"schema\":4,\"gold\":66,\"banner\":\"knight,archer\",\"campaignCleared\":2,\"writs\":0,\"writChapters\":\"\",\"battlesWon\":\"march-1,march-2\",\"bonusClaimed\":\"\"}";
        var four = parseV2(v4Save);
        check(four.customGroups == null && four.schema == 4, "v4-fixture: a schema-4 file carries no customGroups key");
        four.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(four.schema == 5 && four.customGroups != null && four.customGroups.isEmpty() && four.gold == 66 && four.battlesWon.equals(march(2)) && !four.readOnly, "v4toV5: a schema-4 profile loads with customGroups empty, every other field kept, schema 5, writable");
        check(write(four).contains("\"schema\":5") && write(four).contains("\"customGroups\":\"\""), "v4toV5: the loaded schema-4 profile saves as schema 5 with an empty customGroups");
        check(ProfileMigration.v4toV5(parseV2(v4Save)).schema == 5, "v4toV5: sets schema 5");
        var grouped = new Profile();
        grouped.customGroups = new ArrayList<>(List.of(new Profile.CustomGroup(2, "Flank", List.of("archer", "bonebow")), new Profile.CustomGroup(1, "Line", List.of("knight"))));
        grouped.sanitize(cat, CLASSES, SIDES, HOUSES);
        var reread = parseV2(write(grouped));
        reread.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(reread.schema == 5 && reread.customGroups.equals(grouped.customGroups) && write(reread).equals(write(grouped))
            && reread.customGroups.equals(List.of(new Profile.CustomGroup(1, "Line", List.of("knight")), new Profile.CustomGroup(2, "Flank", List.of("archer", "bonebow")))), "v5-round-trip: a schema-5 file with two custom groups loads and saves back unchanged, sorted by number");
        var messy = new Profile();
        messy.customGroups = new ArrayList<>(Arrays.asList(null, new Profile.CustomGroup(1, "  A very long group name  ", Arrays.asList("knight", "knight", "bogus", null)), new Profile.CustomGroup(1, "Dup", List.of()),
            new Profile.CustomGroup(0, "Zero", List.of()), new Profile.CustomGroup(5, "Five", List.of()), new Profile.CustomGroup(3, " ", null), new Profile.CustomGroup(2, "B", List.of()), new Profile.CustomGroup(4, "D", List.of())));
        messy.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(messy.customGroups.size() == 4 && messy.customGroups.get(0).equals(new Profile.CustomGroup(1, "A very long", List.of("knight"))) && messy.customGroups.get(2).equals(new Profile.CustomGroup(3, "Group 3", List.of()))
            && messy.customGroups.stream().mapToInt(Profile.CustomGroup::number).boxed().toList().equals(List.of(1, 2, 3, 4)), "custom-sanitize: nulls, repeats and numbers outside 1..4 go; names trim to 12 and default to Group n; unknown or repeated troop ids go");
        var newer = parseV2(v4Save.replace("\"schema\":4", "\"schema\":6"));
        newer.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(newer.readOnly && newer.schema == 6, "read-only: a schema-6 profile from a newer build is still kept read-only");
        // the file upgrade
        Path dir = Files.createTempDirectory("gravesandglory-profile-migration-check-");
        try {
            byte[] original = ORIGINAL.getBytes(StandardCharsets.UTF_8);
            Path file = dir.resolve("a1.json"), backup = dir.resolve("a1.v1.json"), tmp = dir.resolve("a1.tmp");
            Files.write(file, original);
            Profile migrated = upgrade(file);
            check(migrated != null && migrated.schema == 4 && Arrays.equals(Files.readAllBytes(backup), original), "mig-file: <id>.v1.json is a byte copy of the original");
            check(Files.readString(file).contains("\"schema\":4") && Files.readString(file).equals(write(migrated)) && !Files.exists(tmp), "mig-file: <id>.json is the new schema 4 file, written through a temp file and a move");
            byte[] backupBytes = Files.readAllBytes(backup), fileBytes = Files.readAllBytes(file);
            check(upgrade(file) == null && Arrays.equals(Files.readAllBytes(backup), backupBytes) && Arrays.equals(Files.readAllBytes(file), fileBytes), "mig-idempotent: a second call returns null and both files are byte-unchanged");
            Path again = dir.resolve("b2.json"), oldBackup = dir.resolve("b2.v1.json");
            Files.write(again, original);
            Files.writeString(oldBackup, "an earlier v1 copy");
            check(upgrade(again) != null && Files.readString(oldBackup).equals("an earlier v1 copy") && Files.readString(again).contains("\"schema\":4"), "mig-backup-once: an existing <id>.v1.json is never replaced");
            byte[] corrupt = "{\"gold\":42".getBytes(StandardCharsets.UTF_8);
            Path bad = dir.resolve("c3.json");
            Files.write(bad, corrupt);
            boolean threw = false;
            try { upgrade(bad); } catch (IllegalStateException expected) { threw = true; }
            check(threw && Arrays.equals(Files.readAllBytes(bad), corrupt) && !Files.exists(dir.resolve("c3.v1.json")) && !Files.exists(dir.resolve("c3.tmp")), "mig-corrupt: a parse failure throws IllegalStateException; the original is unchanged, with no .v1.json and no .tmp");
            // mig-v2: a schema-2 save (plan 2C.3) goes to schema 3 and keeps every field
            Path v2 = dir.resolve("e5.json"), v2Backup = dir.resolve("e5.v2.json");
            Files.writeString(v2, V2_SAVE);
            Profile up = upgrade(v2);
            check(up != null && up.schema == 4 && up.battlesWon.equals(march(1)) && up.bonusClaimed.isEmpty(), "mig-v2: schema 2 with campaignCleared 1 becomes schema 4 with battlesWon [march-1] and no bonus claimed");
            check(up.gold == 77 && up.banner.equals(List.of("knight", "archer")) && up.campaignCleared == 1 && up.writs == 1 && up.writChapters.equals(Set.of(1)), "mig-v2: gold, banner, campaignCleared, writs and writChapters are kept unchanged");
            check(Files.readString(v2Backup).equals(V2_SAVE) && !Files.exists(dir.resolve("e5.v1.json")) && Files.readString(v2).equals(write(up)), "mig-v2: <id>.v2.json is a byte copy of the schema-2 file, no .v1.json, and <id>.json is the schema 4 file");
            byte[] v3Bytes = Files.readAllBytes(v2);
            check(upgrade(v2) == null && Arrays.equals(Files.readAllBytes(v2), v3Bytes) && Files.readString(v2Backup).equals(V2_SAVE), "mig-v2-idempotent: a schema 4 file returns null; both files are unchanged");
            Path deep = dir.resolve("f6.json");
            Files.writeString(deep, V2_SAVE.replace("\"campaignCleared\":1", "\"campaignCleared\":3"));
            check(upgrade(deep).battlesWon.equals(march(3)), "mig-v2: campaignCleared 3 gives march-1, march-2 and march-3");
            check(Files.readString(dir.resolve("a1.v2.json")).contains("\"schema\":2") && !Files.readString(dir.resolve("a1.v2.json")).contains("battlesWon"), "mig-file: a schema-1 file also keeps its schema-2 image once as <id>.v2.json");
            check(migrated.battlesWon.equals(march(1)) && migrated.bonusClaimed.isEmpty() && migrated.campaignCleared == 1, "mig-file: schema 1 runs on to schema 3; campaignCleared 1 gives battlesWon [march-1]");
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
