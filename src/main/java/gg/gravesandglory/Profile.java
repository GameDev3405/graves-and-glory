package gg.gravesandglory;

import java.util.*;

/**
 * A commander's saved collection, schema 7 (spec 6.2, plan 2C.3, commander gear spec, command UI custom groups, war table army rework): up to five named war table banners, the owned troops with stars and mastery, per
 * House shards, Renown and Epic pity, the Long March battles won and bonuses paid, and the commander's gear, relics and Forge marks. Server-owned JSON, one file per player
 * (Campaign.profile loads it, migrating an older file first). Pure, so the standalone checks compile it.
 */
public final class Profile {
    /** The JSON key of progression. Campaign.forClient finds the nested state by it to strip the match ids; a rename of the field that leaves this behind sends them again, which p1-report-snapshot catches. */
    static final String PROGRESSION = "progression";
    /** The JSON key of the gear list. Campaign.forClient strips it from the snapshot (the client gets the compact gear_v1 string instead). */
    static final String GEAR = "gear";
    /** One owned troop type: copies received, stars (1 to 3) and its mastery ledger. A stored "stage" field of an older file is ignored: Gson drops a key the class lacks. */
    static final class Owned { int copies=1, stars=1, kills, survived, mastery; String variant="base"; }

    /** The current profile schema; ProfileMigration upgrades older files to it. */
    static final int SCHEMA = 7;
    /** Schema 7: the longest banner name. */
    static final int MAX_BANNER_NAME = 16;
    /** Troop writs a recruitment roll costs, and a chosen recruit (the old 100 and 300 gold, at 100 gold a writ). */
    static final int ROLL_WRITS = 1, CHOSEN_WRITS = 3;
    /** Schema 7: one saved war table banner: its name and its cells as the compact string WarGrid.format writes. A Gson class, not a record, so an old file missing a field still loads. */
    static final class Banner {
        String name, cells;
        Banner() {}
        Banner(String name, String cells) { this.name = name; this.cells = cells; }
    }
    /** Schema 5: the most custom numbered groups a commander keeps, and the longest group name. */
    static final int MAX_CUSTOM_GROUPS = 4, GROUP_NAME_MAX = 12;
    /** One custom numbered group (command UI): its number 1..MAX_CUSTOM_GROUPS, a short name and the troop type ids it orders. */
    record CustomGroup(int number, String name, List<String> troopIds) {}
    int schema = SCHEMA;
    /** Set when the file was saved by a newer build (schema above SCHEMA). Never saved: Gson skips transient fields. Campaign.save then writes nothing. */
    transient boolean readOnly;
    int matches, wins;
    boolean tutorialDone;
    String commander = "champion";
    /** Schema 7: one to rules.maxBanners saved war table banners, and the index of the one a match takes. */
    List<Banner> banners = starterBanners();
    int activeBanner;
    /** Schema 7: the Legendary Pacts held (named writs before schema 7). */
    int pacts;
    /** Schema 7: troop writs (they pay for recruitment rolls and chosen troops), the part of a writ earned so far as 0 to 99 hundredths, and gear shards (they pay for gear and Forge work). */
    int troopWrits, writGold, gearShards;
    /** Schema 7: a one-time text for the player (a banner that was trimmed on load). The server clears it once it has been shown; null when there is none. */
    String notice;
    /**
     * Legacy fields of schema 6 and older, read only: ProfileMigration.v6toV7 moves them (the 20-entry banner, the saved gold and the Pact count) and sanitize nulls
     * them, so Gson (which does not write nulls) never saves them again. Nothing but the migrations reads them.
     */
    List<String> banner;
    Integer gold, writs;
    /** The side row (factions) this commander asks to fly. */
    String bannerColour = "gold";
    /** Owned troop types by id. A new commander owns one copy of each House Common (spec 6.2). */
    Map<String, Owned> owned = commons();
    Map<String, Integer> shards = new LinkedHashMap<>(), renown = new LinkedHashMap<>(), pity = new LinkedHashMap<>();
    Progression.State progression = new Progression.State();
    /** The Long March: the highest campaignBattles order with it and every lower order won. Since schema 3 it is derived from battlesWon (MarchMath.cleared) on sanitize; the War Table and CampWorld.spawn still read it. */
    int campaignCleared;
    /** Schema 3: campaignBattles ids won at least once. */
    Set<String> battlesWon = new LinkedHashSet<>();
    /** Schema 3: bonusObjectives ids already paid; a bonus pays once. */
    Set<String> bonusClaimed = new LinkedHashSet<>();
    /** Campaign chapters whose Legendary Pact was granted, so each chapter grants one. */
    Set<Integer> writChapters = new LinkedHashSet<>();
    /** Unlocked banner cosmetics (renown rewards such as cosmetic:pennant). */
    Set<String> cosmetics = new LinkedHashSet<>();
    /** Schema 4: the commander's gear. Pieces (any number, kept even above inventorySize), worn uids by slot, unlocked relics and the three relic slots. */
    List<Gear.Piece> gear = new ArrayList<>();
    Map<String, Integer> equipped = new LinkedHashMap<>();
    Set<String> relics = new LinkedHashSet<>();
    List<String> relicSlots = new ArrayList<>(List.of("", "", ""));
    int nextGearUid = 1;
    /** Cached Gear.power of the worn pieces, for matchmaking and bot scaling. */
    int gearPower = 0;
    /** Schema 4: Forge marks (the blacksmith's currency), the rolls since the last Epic or better, and the inventory capacity in pieces. */
    int forgeMarks, gearPity;
    int inventorySize = Spec.FORGE[0].startSlots();
    /** Schema 5: custom numbered groups, stored with the Armoury loadout; orders sent to custom:<number> reach these troop types. */
    List<CustomGroup> customGroups = new ArrayList<>();
    /** Ending polish pass 3: personal bests by MatchEnding.BEST_KEYS (kills, armyKills, score, keptPct, captures, commanderDowns). A file without the key loads with
     *  none (no schema change); a key with no record yet is set quietly by the next eligible match. Kept off the camp snapshot (Campaign.forClient). */
    Map<String, Integer> bests = new LinkedHashMap<>();

    /**
     * Repairs a loaded or migrated profile in place. Owned ids the catalog does not know are dropped. Stars clamp to
     * 1..recruitment.maxStars. Banners are repaired by sanitizeBanners, and a profile below schema 7 takes ProfileMigration.v6toV7 first. An unknown class becomes champion and an unknown
     * side colour gold. Shards, renown and pity hold exactly the given Houses, each at 0 or more. A profile still below schema 3 (a
     * schema-2 object from an import or a client copy) takes ProfileMigration.v2toV3 first. battlesWon and bonusClaimed keep only ids a
     * campaignBattles or bonusObjectives row knows, and campaignCleared is derived from battlesWon.
     */
    void sanitize(RosterMath.Catalog c, Set<String> classIds, Set<String> sideIds, List<String> houses) {
        if (schema < 3) ProfileMigration.v2toV3(this, Spec.CAMPAIGNBATTLES);
        if (schema < 4) ProfileMigration.v3toV4(this);
        if (schema < 5) ProfileMigration.v4toV5(this);
        if (schema < 6) ProfileMigration.v5toV6(this);
        boolean toSeven = schema < 7; // v6toV7 runs below, once the owned troops are clean
        if (schema > SCHEMA) readOnly = true; // a save from a newer build: kept as it is, never written by this one
        if (progression == null) progression = new Progression.State();
        progression.sanitize();
        matches = Math.max(0, matches);
        wins = clamp(wins, 0, matches);
        if (commander == null || !classIds.contains(commander)) commander = "champion";
        if (bannerColour == null || !sideIds.contains(bannerColour)) bannerColour = "gold";
        if (owned == null) owned = new LinkedHashMap<>();
        owned.entrySet().removeIf(e -> e.getValue() == null || !c.known(e.getKey()));
        int maxMastery = Arrays.stream(Spec.MASTERY).mapToInt(Spec.MasteryRow::level).max().orElse(0);
        for (var e : owned.entrySet()) {
            Owned o = e.getValue();
            o.copies = Math.max(1, o.copies);
            o.stars = clamp(o.stars, 1, Spec.RECRUITMENT[0].maxStars());
            o.kills = Math.max(0, o.kills);
            o.survived = Math.max(0, o.survived);
            o.mastery = clamp(o.mastery, 0, maxMastery);
            if (o.variant == null || o.variant.isBlank()) o.variant = "base";
        }
        if (toSeven) ProfileMigration.v6toV7(this, c);
        gold = null; // the legacy fields are read by the migration only; null, Gson never writes them
        writs = null;
        banner = null;
        pacts = Math.max(0, pacts);
        troopWrits = clamp(troopWrits, 0, 1_000_000);
        gearShards = clamp(gearShards, 0, 1_000_000);
        writGold = clamp(writGold, 0, 99);
        sanitizeBanners(c);
        shards = perHouse(shards, houses);
        renown = perHouse(renown, houses);
        pity = perHouse(pity, houses);
        if (writChapters == null) writChapters = new LinkedHashSet<>();
        writChapters.removeIf(chapter -> chapter == null || chapter < 1);
        if (cosmetics == null) cosmetics = new LinkedHashSet<>();
        cosmetics.removeIf(id -> id == null || id.isBlank());
        if (battlesWon == null) battlesWon = new LinkedHashSet<>();
        battlesWon.removeIf(id -> MarchMath.row(id, Spec.CAMPAIGNBATTLES) == null);
        if (bonusClaimed == null) bonusClaimed = new LinkedHashSet<>();
        bonusClaimed.removeIf(id -> id == null || Arrays.stream(Spec.BONUSOBJECTIVES).noneMatch(b -> b.id().equals(id)));
        campaignCleared = MarchMath.cleared(battlesWon, Spec.CAMPAIGNBATTLES);
        sanitizeGear();
        customGroups = customGroups(customGroups, c);
        bests = bests(bests);
    }
    /** Personal bests repaired: only MatchEnding.BEST_KEYS, each at 0 or more, in that order; null becomes empty. */
    static Map<String, Integer> bests(Map<String, Integer> in) {
        Map<String, Integer> out = new LinkedHashMap<>();
        if (in != null) for (String k : MatchEnding.BEST_KEYS) { Integer v = in.get(k); if (v != null) out.put(k, Math.max(0, v)); }
        return out;
    }
    /**
     * The banner half of sanitize. One to maxBanners banners remain (none becomes the starter banner, extra ones at the back go); a name loses control characters, is
     * trimmed, cut to MAX_BANNER_NAME and, when blank, becomes "Banner N". Each table drops cells that are out of range, unknown or of an unowned troop, then
     * loses its highest cells (the back rows) until RosterMath.validTable passes for the fieldable Houses: a banner that no longer fits (a troop sold off, a House
     * not fieldable, a sheet cost raised) is trimmed, never wiped, and the player gets a one-time notice. activeBanner clamps to the banners that remain.
     */
    private void sanitizeBanners(RosterMath.Catalog c) {
        if (banners == null) banners = new ArrayList<>();
        banners.removeIf(Objects::isNull);
        if (banners.isEmpty()) banners = starterBanners();
        while (banners.size() > RosterMath.maxBanners()) banners.remove(banners.size() - 1);
        Set<String> houses = new LinkedHashSet<>(houseOrder()); // every House: a House switched off keeps its troops saved and is left out at field time (RosterMath.fieldable), as in schema 6
        int trimmed = 0;
        for (int i = 0; i < banners.size(); i++) {
            Banner b = banners.get(i);
            b.name = bannerName(b.name, i + 1);
            SortedMap<Integer, String> table = WarGrid.parse(b.cells);
            int before = table.size();
            table.entrySet().removeIf(e -> !WarGrid.inRange(e.getKey()) || !c.known(e.getValue()) || !owned.containsKey(e.getValue()));
            dropOverLimit(table, c);
            SortedMap<Integer, String> kept = WarGrid.trim(table, t -> RosterMath.validTable(t, c, owned.keySet(), houses) == null);
            trimmed += before - kept.size();
            b.cells = WarGrid.format(kept);
        }
        if (trimmed > 0 && notice == null) notice = "Your banner no longer fits the war table; " + trimmedText(trimmed) + " taken off the back rows.";
        activeBanner = clamp(activeBanner, 0, banners.size() - 1);
    }
    /** Removes, front to back, each troop past its rarity's rarities.bannerMax (a second Legendary), so trim never has to clear the rows behind it. */
    static void dropOverLimit(SortedMap<Integer, String> table, RosterMath.Catalog c) {
        Map<String, Integer> seen = new HashMap<>();
        table.entrySet().removeIf(e -> { String r = c.rarityOf(e.getValue()); return seen.merge(r, 1, Integer::sum) > RosterMath.bannerMax(r); });
    }
    /** "1 troop was" or "N troops were": the troop count of a banner trim notice. */
    static String trimmedText(int n) { return n == 1 ? "1 troop was" : n + " troops were"; }
    /** A banner name without control characters, trimmed and cut to MAX_BANNER_NAME; "Banner N" when nothing is left. */
    static String bannerName(String name, int number) {
        String n = name == null ? "" : name.replaceAll("\\p{Cntrl}", "").trim();
        if (n.codePointCount(0, n.length()) > MAX_BANNER_NAME) n = n.substring(0, n.offsetByCodePoints(0, MAX_BANNER_NAME)).trim(); // whole code points, so an emoji is never split
        return n.isEmpty() ? "Banner " + number : n;
    }
    /** The banners of a new profile: "Banner I" holding the starter table. */
    static List<Banner> starterBanners() {
        List<Banner> out = new ArrayList<>();
        out.add(new Banner("Banner I", WarGrid.format(starterTable())));
        return out;
    }
    /** The starter table: 12 Order Squires then 8 Order Archers, placed by role (the knights in the front rows, the archers behind them). */
    static SortedMap<Integer, String> starterTable() {
        List<String> troops = new ArrayList<>();
        for (int i = 0; i < 12; i++) troops.add("knight");
        for (int i = 0; i < 8; i++) troops.add("archer");
        return WarGrid.place(troops, id -> id.equals("archer") ? 2 : 0);
    }
    /** The active banner, clamped to the banners held; null only when there are none (sanitize always leaves one). */
    private Banner active() {
        if (banners == null || banners.isEmpty()) return null;
        return banners.get(clamp(activeBanner, 0, banners.size() - 1));
    }
    /** Pays a gold amount of the old economy in the new one: it fills writGold to whole troop writs (100 each) and counts the same number of gear shards. */
    void payGold(int gold) {
        if (gold <= 0) return;
        long w = writGold + (long) gold;
        troopWrits = (int) Math.min(1_000_000L, troopWrits + w / 100);
        writGold = (int) (w % 100);
        gearShards = (int) Math.min(1_000_000L, (long) gearShards + gold);
    }

    /** The active banner's table: cell index to troop id, sorted by cell. Empty when there is no banner. */
    SortedMap<Integer, String> table() {
        Banner b = active();
        return b == null ? new TreeMap<>() : WarGrid.parse(b.cells);
    }
    /** The active banner as the dense list a battle reads: WarGrid.cells() entries, RosterMath.EMPTY where nothing stands. */
    List<String> line() { return WarGrid.dense(table()); }
    /** Saves a table as the cells of banner number banner (0-based). It throws IllegalArgumentException for a banner the profile does not hold. */
    void setTable(int banner, Map<Integer, String> t) {
        if (banners == null || banner < 0 || banner >= banners.size()) throw new IllegalArgumentException("no banner " + banner);
        banners.get(banner).cells = WarGrid.format(t);
    }
    /**
     * Repairs custom groups: nulls and repeated or out-of-range numbers go (the first stays), a name is trimmed, clamped to GROUP_NAME_MAX
     * and defaults to "Group n", troop ids keep catalog ids once each, at most MAX_CUSTOM_GROUPS remain, sorted by number.
     */
    static List<CustomGroup> customGroups(List<CustomGroup> groups, RosterMath.Catalog c) {
        List<CustomGroup> out = new ArrayList<>();
        if (groups == null) return out;
        Set<Integer> numbers = new HashSet<>();
        for (CustomGroup g : groups) {
            if (g == null || g.number() < 1 || g.number() > MAX_CUSTOM_GROUPS || !numbers.add(g.number())) continue;
            List<String> ids = new ArrayList<>();
            if (g.troopIds() != null) for (String id : g.troopIds()) if (id != null && c.known(id) && !ids.contains(id)) ids.add(id);
            out.add(new CustomGroup(g.number(), groupName(g.name(), g.number()), ids));
        }
        out.sort(Comparator.comparingInt(CustomGroup::number));
        return out;
    }
    /** A group name trimmed to GROUP_NAME_MAX characters, "Group n" when blank. */
    static String groupName(String name, int number) {
        String n = name == null ? "" : name.replaceAll("\\p{Cntrl}", "").trim();
        if (n.length() > GROUP_NAME_MAX) n = n.substring(0, GROUP_NAME_MAX).trim();
        return n.isEmpty() ? "Group " + number : n;
    }
    /** The custom group with this number, or null. */
    CustomGroup customGroup(int number) {
        if (customGroups != null) for (CustomGroup g : customGroups) if (g != null && g.number() == number) return g;
        return null;
    }
    /**
     * The gear half of sanitize. Null pieces, unknown items or rarities and repeated uids (the first stays) are dropped; quality clamps to
     * 0..100 and durability to 0..maxDurability; marks and pity clamp; the inventory size is a whole row between the start and the maximum;
     * nextGearUid passes every uid; equipped keeps only owned pieces in their own slot (a two-handed main frees the off-hand); relics keep
     * known ids and relicSlots is exactly the sheet's count of empty or owned, non-repeating relics. A piece is never dropped for being over the inventory size.
     */
    private void sanitizeGear() {
        var f = Gear.forge();
        if (gear == null) gear = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        gear.removeIf(g -> g == null || g.item == null || Gear.item(g.item) == null || g.rarity == null || Gear.rarity(g.rarity) == null || !seen.add(g.uid));
        int top = 0;
        for (Gear.Piece g : gear) {
            g.quality = clamp(g.quality, 0, 100);
            g.durability = clamp(g.durability, 0, f.maxDurability());
            top = Math.max(top, g.uid);
        }
        nextGearUid = Math.max(Math.max(1, nextGearUid), top + 1);
        forgeMarks = clamp(forgeMarks, 0, 1_000_000);
        gearPity = clamp(gearPity, 0, f.pity());
        int size = clamp(inventorySize, f.startSlots(), f.maxSlots());
        inventorySize = Math.max(f.startSlots(), f.startSlots() + (size - f.startSlots()) / f.rowSlots() * f.rowSlots());
        if (equipped == null) equipped = new LinkedHashMap<>();
        equipped.entrySet().removeIf(e -> {
            if (e.getKey() == null || e.getValue() == null) return true;
            Gear.Piece g = Gear.piece(this, e.getValue());
            var it = g == null ? null : Gear.item(g.item);
            return it == null || !it.slot().equals(e.getKey());
        });
        Gear.Piece main = Gear.piece(this, equipped.getOrDefault("main", 0));
        var mainItem = main == null ? null : Gear.item(main.item);
        if (mainItem != null && mainItem.hands() == 2) equipped.remove("off");
        if (relics == null) relics = new LinkedHashSet<>();
        relics.removeIf(id -> id == null || Gear.relic(id) == null);
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < f.relicSlots(); i++) {
            String id = relicSlots != null && i < relicSlots.size() ? relicSlots.get(i) : "";
            slots.add(id != null && relics.contains(id) && !slots.contains(id) ? id : "");
        }
        relicSlots = slots;
        gearPower = Gear.power(this);
    }
    /** One copy of each Common in the troops sheet, in sheet order. */
    static Map<String, Owned> commons() {
        Map<String, Owned> out = new LinkedHashMap<>();
        for (var t : Spec.TROOPS) if (t.rarity().equals("common")) out.put(t.id(), new Owned());
        return out;
    }
    /** sanitize against the sheets: the troops catalog, the legacy class ids (gearSets.classes), the side rows and the House order. */
    void sanitize() {
        Set<String> classes = new LinkedHashSet<>(), sides = new LinkedHashSet<>();
        for (var set : Spec.GEARSETS) for (String c : set.classes().split(",")) if (!c.isEmpty() && !c.equals("-")) classes.add(c); // the legacy class ids: the starter role of an old profile
        for (var f : Spec.FACTIONS) sides.add(f.id());
        sanitize(RosterMath.Catalog.fromSpec(), classes, sides, houseOrder());
    }
    /** A profile rebuilt from a JSON snapshot (Campaign's rollbacks). Gson leaves the transient readOnly false, so sanitize re-derives it from the kept schema; idempotent on a sanitized snapshot. */
    static Profile restored(Profile copy) { copy.sanitize(); return copy; }
    /** House ids in sheet order. */
    static List<String> houseOrder() {
        List<String> out = new ArrayList<>();
        for (var h : Spec.HOUSES) out.add(h.id());
        return out;
    }
    static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    /** A count per House, in House order: every given House at 0 or more, and no other key. */
    static Map<String, Integer> perHouse(Map<String, Integer> counts, List<String> houses) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String h : houses) {
            Integer n = counts == null ? null : counts.get(h);
            out.put(h, n == null ? 0 : Math.max(0, n));
        }
        return out;
    }
}
