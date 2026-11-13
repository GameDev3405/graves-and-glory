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
        p.schema = number(text, "schema"); p.campaignCleared = number(text, "campaignCleared");
        if (p.schema >= 7) { // the schema 7 fields; the legacy ones are null, as Gson leaves them in a file that never wrote them
            p.gold = null; p.writs = null; p.banner = null;
            p.troopWrits = number(text, "troopWrits"); p.writGold = number(text, "writGold"); p.gearShards = number(text, "gearShards"); p.pacts = number(text, "pacts"); p.activeBanner = number(text, "activeBanner");
            p.banners = new ArrayList<>();
            for (String b : field(text, "banners").split(";")) if (!b.isEmpty()) { String[] f = b.split("[|]", -1); p.banners.add(new Profile.Banner(f[0], f[1])); }
        } else {
            p.gold = number(text, "gold"); p.writs = number(text, "writs");
            if (text.contains("\"banner\"")) p.banner = items(text, "banner", new ArrayList<>());
        }
        p.writChapters = new LinkedHashSet<>();
        for (String c : items(text, "writChapters", new ArrayList<>())) p.writChapters.add(Integer.parseInt(c));
        p.battlesWon = text.contains("\"battlesWon\"") ? items(text, "battlesWon", new LinkedHashSet<>()) : null; // a schema-2 file has no such key: Gson leaves it null
        p.bonusClaimed = text.contains("\"bonusClaimed\"") ? items(text, "bonusClaimed", new LinkedHashSet<>()) : null;
        p.customGroups = text.contains("\"customGroups\"") ? groups(field(text, "customGroups")) : null; // a schema-4 file has no such key: Gson leaves it null
        return p;
    }
    static String write(Profile p) {
        String text = "{\"schema\":" + p.schema;
        if (p.schema >= 7) text += ",\"troopWrits\":" + p.troopWrits + ",\"writGold\":" + p.writGold + ",\"gearShards\":" + p.gearShards + ",\"pacts\":" + p.pacts + ",\"activeBanner\":" + p.activeBanner
            + ",\"banners\":\"" + String.join(";", p.banners.stream().map(b -> b.name + "|" + b.cells).toList()) + "\"";
        else text += ",\"gold\":" + p.gold + ",\"banner\":\"" + String.join(",", p.banner) + "\",\"writs\":" + p.writs;
        text += ",\"campaignCleared\":" + p.campaignCleared + ",\"writChapters\":\"" + String.join(",", p.writChapters.stream().map(String::valueOf).toList()) + "\"";
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
        for (int i = 1; i <= n; i++) ids.add("oath-" + i); // v2toV3 rebuilds battlesWon from the current sheet's rows
        return ids;
    }

    // Schema 7 (army rework Task 2): the v6 fixtures go through the real Gson loaders and the real sheet catalog.
    static final com.google.gson.Gson GSON = new com.google.gson.Gson();
    static Profile fixture(String name) throws IOException {
        return GSON.fromJson(Files.readString(Paths.get("test-tools/resources/fixtures/profile-v6-" + name + ".json")), Profile.class);
    }
    static long count(Map<Integer, String> table, String id) { return table.values().stream().filter(id::equals).count(); }
    static String problem(Profile p) { return RosterMath.validTable(p.table(), RosterMath.Catalog.fromSpec(), p.owned.keySet(), RosterMath.fieldableHouses()); }

    /** The starter banner's 12 Squires and 8 Archers move onto the war table block (entry i to column 2 + i % 5, row i / 5), whole, with no notice. */
    static void v6StarterMoves() throws IOException {
        var p = fixture("starter");
        check(p.schema == 6 && p.banner.size() == 20 && p.gold == 300, "v6StarterMoves: the fixture is a schema 6 file with a 20-entry banner");
        p.sanitize();
        check(p.schema == 7 && p.banner == null && p.gold == null && p.writs == null, "v6StarterMoves: schema 7, the legacy fields nulled");
        check(p.banners.size() == 1 && p.activeBanner == 0 && p.banners.get(0).name.equals("Banner I") && p.notice == null, "v6StarterMoves: one banner, named Banner I, no notice");
        var t = p.table();
        check(t.size() == 20 && count(t, "knight") == 12 && count(t, "archer") == 8, "v6StarterMoves: all 20 troops are on the table: " + t);
        boolean placed = true;
        for (int i = 0; i < 20; i++) placed &= (i < 12 ? "knight" : "archer").equals(t.get(WarGrid.index(2 + i % 5, i / 5)));
        check(placed && problem(p) == null, "v6StarterMoves: entry i sits on (2 + i % 5, i / 5) and the table passes the validator: " + problem(p));
        check(p.line().size() == WarGrid.cells() && WarGrid.sparse(p.line()).equals(t), "v6StarterMoves: line() is the dense 54-entry form of the table");
    }

    /** An Epic's ^ pair and a Legendary's ^^^ run leave the table: each troop lands once, on its first entry, and no marker survives. */
    static void v6WideDropsMarkers() throws IOException {
        var p = fixture("wide");
        check(p.banner.stream().filter("^"::equals).count() == 5, "v6WideDropsMarkers: the fixture holds two Epic markers and a Legendary run of three");
        p.sanitize();
        var t = p.table();
        check(t.size() == 15 && count(t, "knight") == 8 && count(t, "mage") == 2 && count(t, "gl_marshal") == 1 && count(t, "archer") == 4, "v6WideDropsMarkers: 15 troops, each wide one once: " + t);
        check(!t.containsValue("^") && !p.line().contains("^") && "mage".equals(t.get(WarGrid.index(5, 1))) && "mage".equals(t.get(WarGrid.index(2, 2))) && "gl_marshal".equals(t.get(WarGrid.index(4, 2))), "v6WideDropsMarkers: no marker anywhere; the wide troops sit on their first entry's cell");
        check(p.notice == null && problem(p) == null, "v6WideDropsMarkers: it fits the allowance, so no notice: " + problem(p));
    }

    /** A banner over the allowance loses troops from the back until it fits, never all of them, and the player is told once. */
    static void v6OverTrims() throws IOException {
        var p = fixture("over");
        var cat = RosterMath.Catalog.fromSpec();
        int line = 0;
        for (String id : p.banner) if (!id.equals("^")) line += cat.cost(id);
        check(line > RosterMath.allowance(), "v6OverTrims: the fixture's banner costs " + line + ", over the allowance " + RosterMath.allowance());
        p.sanitize();
        var t = p.table();
        check(t.size() == 12 && count(t, "longbow") == 1 && count(t, "sentinel") == 8 && count(t, "mage") == 2 && count(t, "gl_marshal") == 1, "v6OverTrims: the 3 highest cells (longbows at the back) went: " + t);
        check(problem(p) == null && WarGrid.cost(t, cat) <= RosterMath.allowance(), "v6OverTrims: what is left passes the validator: " + problem(p));
        check(p.notice != null && p.notice.contains("3 troops were") && p.notice.contains("war table"), "v6OverTrims: the one-time notice names the trim: " + p.notice);
        String note = p.notice;
        p.sanitize();
        check(p.notice.equals(note) && p.table().equals(t), "v6OverTrims: sanitizing again changes nothing");
    }

    /** gold 1234 and 2 writs: 12 troop writs and 34 gear shards (the rest of the gold), the 2 old writs become Pacts. */
    static void v6GoldSplit() throws IOException {
        var p = fixture("gold");
        p.sanitize();
        check(p.troopWrits == 12 && p.gearShards == 34 && p.writGold == 0 && p.pacts == 2, "v6GoldSplit: 1234 gold is 12 troop writs and 34 gear shards, 2 Pacts: " + p.troopWrits + "/" + p.gearShards + "/" + p.pacts);
        check(p.gold == null && p.writs == null, "v6GoldSplit: gold and writs are null afterwards");
        var small = fixture("gold");
        small.gold = 99; small.writs = null;
        small.sanitize();
        check(small.troopWrits == 0 && small.gearShards == 99 && small.pacts == 0, "v6GoldSplit: 99 gold is no writ and 99 shards; no writs is no Pact");
        var neg = fixture("gold");
        neg.gold = -50;
        neg.sanitize();
        check(neg.troopWrits == 0 && neg.gearShards == 0, "v6GoldSplit: negative gold counts as 0");
        var pay = new Profile();
        pay.writGold = 60;
        pay.payGold(250);
        check(pay.troopWrits == 3 && pay.writGold == 10 && pay.gearShards == 250, "payGold: 60 + 250 gold is 3 writs with 10 over, and 250 shards");
    }

    /** A schema 1 file chains through every step to schema 7: refunds 600 gold, which is 10 writs and 20 shards, and the 18-troop line sits on the table. */
    static void v1ChainsToV7() {
        var p = toV2(v1());
        p.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(p.schema == 7 && p.gold == null && p.banner == null && p.writs == null, "v1ChainsToV7: schema 7, legacy fields nulled");
        check(p.troopWrits == 10 && p.gearShards == 20 && p.pacts == 0, "v1ChainsToV7: v1 gold 420 plus the 600 refund is 10 writs and 20 shards: " + p.troopWrits + "/" + p.gearShards);
        check(p.banners.size() == 1 && p.table().size() == 18 && p.notice == null && p.owned.keySet().equals(COMMONS), "v1ChainsToV7: the 18 troops of the v1 line are on the war table, whole");
    }

    /** A banner that no longer fits is trimmed from the back, never wiped: 30 Squires cost 660 against 640. */
    static void sanitizeTrimsNotWipes() {
        var p = new Profile();
        var cells = new TreeMap<Integer, String>();
        for (int i = 0; i < 30; i++) cells.put(i, "knight");
        cells.put(40, "mage"); // not owned: dropped, the rest kept
        p.banners = new ArrayList<>(List.of(new Profile.Banner("Wide", WarGrid.format(cells)), new Profile.Banner("Empty", "")));
        p.sanitize();
        var t = p.banners.get(0).cells;
        var kept = WarGrid.parse(t);
        check(kept.size() == 29 && kept.firstKey() == 0 && kept.lastKey() == 28 && count(kept, "knight") == 29, "sanitizeTrimsNotWipes: 29 of 30 Squires stay (the unowned cell went first): " + t);
        check(p.banners.get(1).cells.isEmpty() && p.banners.size() == 2 && p.banners.get(1).name.equals("Empty"), "sanitizeTrimsNotWipes: an empty banner stays empty and named");
        check(p.notice != null && p.notice.contains("2 troops were") && problem(p) == null, "sanitizeTrimsNotWipes: the notice counts what was taken off (the unowned cell and the 30th): " + p.notice);
        var old = new Profile();
        old.banners = new ArrayList<>(List.of(new Profile.Banner("Kept", "0:knight")));
        old.notice = "earlier";
        old.sanitize();
        check(old.notice.equals("earlier") && old.table().size() == 1, "sanitizeTrimsNotWipes: an unread notice is not overwritten");
    }

    /** Review 1 fix-ups: a second Legendary goes alone (the troops behind it stay), names cut on whole code points, saved gold clamps before the split. */
    static void reviewFixups() {
        var p = new Profile();
        p.owned.put("gl_marshal", new Profile.Owned());
        p.owned.put("gb_hollowking", new Profile.Owned());
        var cells = new TreeMap<Integer, String>();
        cells.put(0, "gl_marshal"); cells.put(1, "gb_hollowking"); cells.put(2, "knight"); cells.put(30, "archer");
        p.banners = new ArrayList<>(List.of(new Profile.Banner("Two", WarGrid.format(cells))));
        p.sanitize();
        var kept = p.table();
        check(kept.size() == 3 && "gl_marshal".equals(kept.get(0)) && !kept.containsKey(1) && "archer".equals(kept.get(30)), "reviewFixups: only the second Legendary goes: " + kept);
        String emoji = new String(Character.toChars(0x1F600));
        String cut = Profile.bannerName("abcdefghijklmno" + emoji + "z", 1);
        check(cut.equals("abcdefghijklmno" + emoji), "reviewFixups: a 17-code-point name keeps the whole emoji as its 16th: " + cut);
        var rich = new Profile();
        rich.schema = 6; rich.gold = 900_000_000; rich.banner = null;
        ProfileMigration.v6toV7(rich, RosterMath.Catalog.fromSpec());
        check(rich.troopWrits == 10_000 && rich.gearShards == 0, "reviewFixups: gold clamps at 1,000,000 before the split: " + rich.troopWrits + "/" + rich.gearShards);
    }

    /** Names clean and cut to 16, a blank one is Banner N; banners past the limit go and activeBanner clamps. */
    static void bannerNamesClamp() {
        var p = new Profile();
        p.banners = new ArrayList<>();
        p.banners.add(new Profile.Banner("  A very long banner name indeed  ", "0:knight"));
        p.banners.add(new Profile.Banner("Bad\u0007\nName", "0:archer"));
        p.banners.add(new Profile.Banner(null, "0:knight"));
        p.banners.add(null);
        p.banners.add(new Profile.Banner("   ", ""));
        for (int i = 0; i < 6; i++) p.banners.add(new Profile.Banner("Extra " + i, "0:knight"));
        p.activeBanner = 99;
        p.sanitize();
        check(p.banners.size() == RosterMath.maxBanners() && p.banners.size() == 5, "bannerNamesClamp: five banners at most: " + p.banners.size());
        check(p.banners.get(0).name.equals("A very long bann") && p.banners.get(0).name.length() == Profile.MAX_BANNER_NAME, "bannerNamesClamp: a long name is cut to 16 characters: " + p.banners.get(0).name);
        check(p.banners.get(1).name.equals("BadName") && p.banners.get(2).name.equals("Banner 3") && p.banners.get(3).name.equals("Banner 4"), "bannerNamesClamp: control characters go; a null or blank name is Banner N");
        check(p.activeBanner == 4 && p.table().isEmpty() == p.banners.get(4).cells.isEmpty(), "bannerNamesClamp: activeBanner 99 clamps to the last banner");
        var none = new Profile();
        none.banners = new ArrayList<>();
        none.activeBanner = -3;
        none.sanitize();
        check(none.banners.size() == 1 && none.activeBanner == 0 && none.table().equals(Profile.starterTable()), "bannerNamesClamp: no banner at all becomes the starter banner");
        var set = new Profile();
        try { set.setTable(5, new TreeMap<>()); check(false, "setTable: a banner index past the list throws"); } catch (IllegalArgumentException expected) { checks++; }
        set.setTable(0, new TreeMap<>(Map.of(4, "archer")));
        check(WarGrid.format(set.table()).equals("4:archer") && set.banners.get(0).name.equals("Banner I"), "setTable: replaces the cells of a banner and keeps its name");
    }

    /** The legacy gold, writs and banner are never written again: Gson (no serializeNulls) omits the nulls, and the file reads back the same. */
    static void legacyFieldsNotWritten() throws IOException {
        var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create(); // Campaign.JSON's settings
        var p = fixture("gold");
        p.sanitize();
        var tree = gson.toJsonTree(p).getAsJsonObject();
        check(!tree.has("gold") && !tree.has("writs") && !tree.has("banner") && !tree.has("notice"), "legacyFieldsNotWritten: no gold, writs, banner or empty notice key: " + tree.keySet());
        check(tree.get("schema").getAsInt() == 7 && tree.get("troopWrits").getAsInt() == 12 && tree.get("gearShards").getAsInt() == 34 && tree.get("pacts").getAsInt() == 2 && tree.has("writGold") && tree.has("activeBanner") && tree.getAsJsonArray("banners").size() == 1, "legacyFieldsNotWritten: schema 7 and the new keys are written");
        String text = gson.toJson(p);
        var back = gson.fromJson(text, Profile.class);
        back.sanitize();
        check(gson.toJson(back).equals(text) && back.gold == null && back.writs == null && back.banner == null, "legacyFieldsNotWritten: the file reads back and writes the same bytes, with the purses unchanged");
    }

    /** Saved Forge marks add 1:1 to gear shards (gold 1234 and marks 50 end at 12 writs and 84 shards), the marks key is never written, and a schema 7 file that still has marks folds them once. */
    static void marksFoldIntoShards() throws IOException {
        var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        var p = fixture("gold");
        p.forgeMarks = 50;
        p.sanitize();
        check(p.troopWrits == 12 && p.gearShards == 84 && p.forgeMarks == null, "marksFoldIntoShards: 1234 gold and 50 marks are 12 writs and 84 gear shards: " + p.troopWrits + "/" + p.gearShards);
        check(!gson.toJsonTree(p).getAsJsonObject().has("forgeMarks"), "marksFoldIntoShards: no forgeMarks key in the Gson output");
        p.sanitize();
        check(p.gearShards == 84, "marksFoldIntoShards: sanitizing again adds nothing");
        var seven = new Profile();
        seven.gearShards = 10; seven.forgeMarks = 25; // a schema 7 file from an earlier build of this branch
        seven.sanitize();
        check(seven.gearShards == 35 && seven.forgeMarks == null, "marksFoldIntoShards: a schema 7 profile with marks folds them once: " + seven.gearShards);
        seven.sanitize();
        check(seven.gearShards == 35, "marksFoldIntoShards: and only once");
        var big = new Profile();
        big.gearShards = 900_000; big.forgeMarks = 900_000;
        big.sanitize();
        check(big.gearShards == 1_000_000, "marksFoldIntoShards: the folded total stays within 1,000,000");
    }

    /** Writs keep the part of a writ earned so far: 80 then 40 gold-equivalents make 1 writ and leave 20 in writGold. */
    static void writsCarryRemainder() {
        var p = new Profile();
        RosterMath.payWrits(p, 80);
        check(p.troopWrits == 0 && p.writGold == 80, "writsCarryRemainder: 80 is no writ yet");
        RosterMath.payWrits(p, 40);
        check(p.troopWrits == 1 && p.writGold == 20 && p.gearShards == 0, "writsCarryRemainder: then 40 makes 1 writ with 20 over, and payWrits pays no shards: " + p.troopWrits + "/" + p.writGold);
        RosterMath.payWrits(p, 0); RosterMath.payWrits(p, -30);
        check(p.troopWrits == 1 && p.writGold == 20, "writsCarryRemainder: nothing or less pays nothing");
        RosterMath.payWrits(p, 1280);
        check(p.troopWrits == 14 && p.writGold == 0, "writsCarryRemainder: 1280 more with the 20 carried is 13 writs, 14 in all");
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
        check(blank.schema == 7 && blank.banners.size() == 1 && blank.activeBanner == 0 && blank.table().equals(Profile.starterTable()) && blank.line().size() == WarGrid.cells() && blank.commander.equals("champion") && blank.bannerColour.equals("gold"), "profile-default: schema 7, one starter banner, champion, gold");
        check(blank.gold == null && blank.writs == null && blank.banner == null && blank.pacts == 0 && blank.troopWrits == 0 && blank.writGold == 0 && blank.gearShards == 0 && blank.notice == null, "profile-default: the legacy fields are null and the schema 7 purses are empty");
        check(blank.owned.keySet().equals(COMMONS) && new ArrayList<>(blank.owned.keySet()).equals(List.of("knight", "archer", "revenant", "bonebow", "wm_hound", "wm_hunter", "ic_hearthguard", "ic_arbalist", "tc_raider", "tc_harpooner"))
            && blank.owned.values().stream().allMatch(o -> o.copies == 1 && o.stars == 1) && RosterMath.validTable(blank.table(), cat, blank.owned.keySet(), new HashSet<>(HOUSES)) == null, "profile-default: one copy of each of the 10 Commons, in sheet order, so the starter banner is valid");
        var p = new Profile();
        p.owned.clear();
        p.owned.put("knight", owned(0, 0)); p.owned.put("mage", owned(2, 7)); p.owned.put("oracle", owned(1, 1)); p.owned.put("archer", null);
        p.owned.get("knight").mastery = 9; p.owned.get("mage").mastery = -1;
        p.banners = new ArrayList<>(List.of(new Profile.Banner("  ", "0:knight,1:oracle,2:mage,3:mage,99:knight,x")));
        p.commander = "vanguard"; p.bannerColour = "dawn";
        p.shards = new HashMap<>(Map.of("gilded", -5, "bogus", 3)); p.renown = null; p.pity = new HashMap<>();
        p.troopWrits = -4; p.gearShards = 2_000_000; p.writGold = 150; p.pacts = -2; p.matches = 3; p.wins = 9; p.progression = null;
        p.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(p.owned.keySet().equals(Set.of("knight", "mage")), "sanitize: unknown ids and empty entries are dropped from owned");
        var knight = p.owned.get("knight");
        var mage = p.owned.get("mage");
        check(knight.copies == 1 && knight.stars == 1 && mage.stars == 3, "sanitize: stars clamp to 1..3");
        check(Arrays.stream(Profile.Owned.class.getDeclaredFields()).noneMatch(f -> f.getName().equals("stage")) && Arrays.stream(Profile.class.getDeclaredMethods()).noneMatch(m -> m.getName().equals("lastStage")), "profile-no-stage: Owned has no stage field and Profile no lastStage, so a stored stage is never read (Gson ignores the key) and never written (the in-game migration suite round-trips a stage:3 file)");
        check(knight.mastery == 3 && mage.mastery == 0, "sanitize: mastery clamps to 0..3, the last mastery level (9 becomes 3, -1 becomes 0)");
        check(WarGrid.format(p.table()).equals("0:knight,2:mage,3:mage") && p.banners.get(0).name.equals("Banner 1"), "sanitize: a table drops its unknown, unowned and out of range cells and keeps the rest; a blank name becomes Banner 1");
        check(p.commander.equals("champion") && p.bannerColour.equals("gold"), "sanitize: an unknown class maps to champion and an unknown colour to gold");
        check(zeros(p.shards) && zeros(p.renown) && zeros(p.pity), "sanitize: shards, renown and pity hold the five House keys at 0 or more, and nothing else");
        check(p.troopWrits == 0 && p.gearShards == 1_000_000 && p.writGold == 99 && p.pacts == 0 && p.wins == 3 && p.progression != null && p.schema == 7, "sanitize: the purses clamp (writs 0, shards 1000000, writGold 0..99, Pacts 0) and wins clamp; a missing progression is rebuilt");
        var valid = new Profile();
        for (String id : COMMONS) valid.owned.put(id, owned(1, 1));
        valid.banners = new ArrayList<>(List.of(new Profile.Banner("Line", "0:revenant,1:bonebow,10:revenant")));
        valid.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(valid.banners.size() == 1 && valid.banners.get(0).cells.equals("0:revenant,1:bonebow,10:revenant") && valid.banners.get(0).name.equals("Line") && valid.notice == null, "sanitize: a valid banner is left alone, with no notice");
        // schema 3 (plan 2C.3): a fresh save, v2toV3 in memory, the sanitizer and a round trip
        var newSave = new Profile();
        newSave.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(newSave.schema == 7 && newSave.customGroups.isEmpty() && newSave.battlesWon.isEmpty() && newSave.bonusClaimed.isEmpty() && newSave.campaignCleared == 0, "fresh-save: schema 7, no custom group, no battle won, no bonus claimed, campaignCleared 0");
        var old = parseV2(V2_SAVE.replace("\"campaignCleared\":1", "\"campaignCleared\":6"));
        check(ProfileMigration.v2toV3(old, Spec.CAMPAIGNBATTLES) == old && old.schema == 3 && old.battlesWon.equals(march(6)) && old.bonusClaimed.isEmpty() && old.writs == 1, "v2toV3: in place; campaignCleared 6 gives all six battles, writs kept");
        var unmigrated = parseV2(V2_SAVE);
        unmigrated.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(unmigrated.schema == 7 && unmigrated.battlesWon.isEmpty() && unmigrated.campaignCleared == 0 && unmigrated.pacts == 1 && unmigrated.writs == null && unmigrated.writChapters.isEmpty(), "sanitize: a schema-2 profile that skipped the file upgrade runs v2toV3 first, then the chapter 1 reset (writs kept)");
        var broken = new Profile();
        broken.battlesWon = new LinkedHashSet<>(List.of("oath-1", "oath-3", "oath-9")); broken.bonusClaimed = new LinkedHashSet<>(List.of("o1_hold_all", "b_bogus")); broken.campaignCleared = 5;
        broken.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(broken.battlesWon.equals(Set.of("oath-1", "oath-3")) && broken.bonusClaimed.equals(Set.of("o1_hold_all")) && broken.campaignCleared == 1, "sanitize: unknown battle and bonus ids are dropped; campaignCleared is the unbroken run from oath-1 (1, not 5)");
        var nulls = new Profile();
        nulls.battlesWon = null; nulls.bonusClaimed = null;
        nulls.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(nulls.battlesWon != null && nulls.bonusClaimed != null, "sanitize: missing battlesWon and bonusClaimed are rebuilt empty");
        var saved = new Profile();
        saved.battlesWon = new LinkedHashSet<>(march(2)); saved.bonusClaimed = new LinkedHashSet<>(List.of("o1_hold_all")); saved.pacts = 0; saved.gearShards = 55;
        saved.sanitize(cat, CLASSES, SIDES, HOUSES);
        var loaded = parseV2(write(saved));
        loaded.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(write(loaded).equals(write(saved)) && loaded.battlesWon.equals(march(2)) && loaded.bonusClaimed.equals(Set.of("o1_hold_all")) && loaded.campaignCleared == 2 && loaded.schema == 7, "round-trip: a schema 7 save loads and saves back unchanged (battlesWon, bonusClaimed, campaignCleared 2)");
        // schema 5 (command UI custom groups): a schema-4 file loads with no custom group and saves as schema 5; a schema-5 file round-trips
        String v4Save = "{\"schema\":4,\"gold\":66,\"banner\":\"knight,archer\",\"campaignCleared\":2,\"writs\":0,\"writChapters\":\"\",\"battlesWon\":\"march-1,march-2\",\"bonusClaimed\":\"\"}";
        var four = parseV2(v4Save);
        check(four.customGroups == null && four.schema == 4, "v4-fixture: a schema-4 file carries no customGroups key");
        four.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(four.schema == 7 && four.customGroups != null && four.customGroups.isEmpty() && four.troopWrits == 0 && four.gearShards == 66 && four.battlesWon.isEmpty() && !four.readOnly, "v4toV5: a schema-4 profile loads with customGroups empty, other fields kept, its march wins reset (schema 7), writable");
        check(write(four).contains("\"schema\":7") && write(four).contains("\"customGroups\":\"\""), "v4toV5: the loaded schema-4 profile saves as schema 7 with an empty customGroups");
        check(ProfileMigration.v4toV5(parseV2(v4Save)).schema == 5, "v4toV5: sets schema 5");
        // ending polish pass 3: personal bests. An old file has no bests key: it loads with none, the first match sets records quietly, a later one beats them
        var noBests = parseV2(v4Save.replace("\"schema\":4", "\"schema\":6"));
        noBests.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(noBests.bests != null && noBests.bests.isEmpty() && !noBests.readOnly && noBests.schema == 7 && noBests.gearShards == 66, "bests-old-file: a schema-6 profile saved before bests loads with none, writable, other fields kept (schema 7, gold now gear shards)");
        check(MatchEnding.bests(noBests.bests, Map.of("kills", 11, "armyKills", 40)).isEmpty() && noBests.bests.equals(Map.of("kills", 11, "armyKills", 40)), "bests-old-file: its first match sets records quietly");
        check(MatchEnding.bests(noBests.bests, Map.of("kills", 14, "armyKills", 30)).equals(List.of(new MatchEnding.Best("kills", "Kills", 14, 11))) && noBests.bests.get("kills") == 14, "bests-old-file: a later better match reports and keeps the beaten value");
        var nullBests = new Profile();
        nullBests.bests = null;
        nullBests.sanitize(cat, CLASSES, SIDES, HOUSES);
        var oddBests = new Profile();
        oddBests.bests = new LinkedHashMap<>(Map.of("score", -4, "bogus", 9, "captures", 3));
        oddBests.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(nullBests.bests != null && nullBests.bests.isEmpty() && oddBests.bests.equals(Map.of("score", 0, "captures", 3)) && new ArrayList<>(oddBests.bests.keySet()).equals(List.of("score", "captures")), "bests-sanitize: null becomes empty; unknown keys go, values clamp to 0, in key order");
        var grouped = new Profile();
        grouped.customGroups = new ArrayList<>(List.of(new Profile.CustomGroup(2, "Flank", List.of("archer", "bonebow")), new Profile.CustomGroup(1, "Line", List.of("knight"))));
        grouped.sanitize(cat, CLASSES, SIDES, HOUSES);
        var reread = parseV2(write(grouped));
        reread.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(reread.schema == 7 && reread.customGroups.equals(grouped.customGroups) && write(reread).equals(write(grouped))
            && reread.customGroups.equals(List.of(new Profile.CustomGroup(1, "Line", List.of("knight")), new Profile.CustomGroup(2, "Flank", List.of("archer", "bonebow")))), "v5-round-trip: a schema-5 file with two custom groups loads and saves back unchanged, sorted by number");
        var messy = new Profile();
        messy.customGroups = new ArrayList<>(Arrays.asList(null, new Profile.CustomGroup(1, "  A very long group name  ", Arrays.asList("knight", "knight", "bogus", null)), new Profile.CustomGroup(1, "Dup", List.of()),
            new Profile.CustomGroup(0, "Zero", List.of()), new Profile.CustomGroup(5, "Five", List.of()), new Profile.CustomGroup(3, " ", null), new Profile.CustomGroup(2, "B", List.of()), new Profile.CustomGroup(4, "D", List.of())));
        messy.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(messy.customGroups.size() == 4 && messy.customGroups.get(0).equals(new Profile.CustomGroup(1, "A very long", List.of("knight"))) && messy.customGroups.get(2).equals(new Profile.CustomGroup(3, "Group 3", List.of()))
            && messy.customGroups.stream().mapToInt(Profile.CustomGroup::number).boxed().toList().equals(List.of(1, 2, 3, 4)), "custom-sanitize: nulls, repeats and numbers outside 1..4 go; names trim to 12 and default to Group n; unknown or repeated troop ids go");
        var newer = parseV2(v4Save.replace("\"schema\":4", "\"schema\":8"));
        newer.sanitize(cat, CLASSES, SIDES, HOUSES);
        check(newer.readOnly && newer.schema == 8, "read-only: a schema-8 profile from a newer build is kept read-only");
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
            check(up != null && up.schema == 4 && up.battlesWon.equals(march(1)) && up.bonusClaimed.isEmpty(), "mig-v2: schema 2 with campaignCleared 1 becomes schema 4 with battlesWon [oath-1] (the reset empties it on sanitize) and no bonus claimed");
            check(up.gold == 77 && up.banner.equals(List.of("knight", "archer")) && up.campaignCleared == 1 && up.writs == 1 && up.writChapters.equals(Set.of(1)), "mig-v2: gold, banner, campaignCleared, writs and writChapters are kept unchanged");
            check(Files.readString(v2Backup).equals(V2_SAVE) && !Files.exists(dir.resolve("e5.v1.json")) && Files.readString(v2).equals(write(up)), "mig-v2: <id>.v2.json is a byte copy of the schema-2 file, no .v1.json, and <id>.json is the schema 4 file");
            byte[] v3Bytes = Files.readAllBytes(v2);
            check(upgrade(v2) == null && Arrays.equals(Files.readAllBytes(v2), v3Bytes) && Files.readString(v2Backup).equals(V2_SAVE), "mig-v2-idempotent: a schema 4 file returns null; both files are unchanged");
            Path deep = dir.resolve("f6.json");
            Files.writeString(deep, V2_SAVE.replace("\"campaignCleared\":1", "\"campaignCleared\":3"));
            check(upgrade(deep).battlesWon.equals(march(3)), "mig-v2: campaignCleared 3 gives the first three rows");
            check(Files.readString(dir.resolve("a1.v2.json")).contains("\"schema\":2") && !Files.readString(dir.resolve("a1.v2.json")).contains("battlesWon"), "mig-file: a schema-1 file also keeps its schema-2 image once as <id>.v2.json");
            check(migrated.battlesWon.equals(march(1)) && migrated.bonusClaimed.isEmpty() && migrated.campaignCleared == 1, "mig-file: schema 1 runs on to schema 3; campaignCleared 1 gives battlesWon [oath-1] before sanitize");
            Path stuck = dir.resolve("d4.json");
            Files.write(stuck, original);
            Files.createDirectory(dir.resolve("d4.tmp")); // <id>.tmp cannot be written as a file
            boolean failed = false;
            try { upgrade(stuck); } catch (IOException expected) { failed = true; }
            check(failed && Arrays.equals(Files.readAllBytes(stuck), original), "mig-tmp-fail: when <id>.tmp cannot be written the original <id>.json is left unchanged");
        } finally {
            try (Stream<Path> paths = Files.walk(dir)) { paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete()); }
        }
        v6StarterMoves(); v6WideDropsMarkers(); v6OverTrims(); v6GoldSplit(); v1ChainsToV7(); sanitizeTrimsNotWipes(); bannerNamesClamp(); reviewFixups(); legacyFieldsNotWritten(); marksFoldIntoShards(); writsCarryRemainder();
        System.out.println("ProfileMigrationCheck: " + checks + " checks passed");
    }
}
