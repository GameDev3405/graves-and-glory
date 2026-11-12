package gg.gravesandglory;

import java.util.*;

/**
 * A commander's saved collection, schema 6 (spec 6.2, plan 2C.3, commander gear spec, command UI custom groups): the banner, the owned troops with stars and mastery, per
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
    static final int SCHEMA = 6;
    /** Schema 5: the most custom numbered groups a commander keeps, and the longest group name. */
    static final int MAX_CUSTOM_GROUPS = 4, GROUP_NAME_MAX = 12;
    /** One custom numbered group (command UI): its number 1..MAX_CUSTOM_GROUPS, a short name and the troop type ids it orders. */
    record CustomGroup(int number, String name, List<String> troopIds) {}
    int schema = SCHEMA;
    /** Set when the file was saved by a newer build (schema above SCHEMA). Never saved: Gson skips transient fields. Campaign.save then writes nothing. */
    transient boolean readOnly;
    int gold, matches, wins;
    boolean tutorialDone;
    String commander = "champion";
    /** rules.squadSize slots: troop ids, ^ for a slot of the Epic or Legendary before it, and empty. */
    List<String> banner = RosterMath.starterBanner();
    /** The side row (factions) this commander asks to fly. */
    String bannerColour = "gold";
    /** Owned troop types by id. A new commander owns one copy of each House Common (spec 6.2). */
    Map<String, Owned> owned = commons();
    Map<String, Integer> shards = new LinkedHashMap<>(), renown = new LinkedHashMap<>(), pity = new LinkedHashMap<>();
    int writs;
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

    /**
     * Repairs a loaded or migrated profile in place. Owned ids the catalog does not know are dropped. Stars clamp to
     * 1..recruitment.maxStars. A banner that fails validBanner is repacked. An unknown class becomes champion and an unknown
     * side colour gold. Shards, renown and pity hold exactly the given Houses, each at 0 or more. A profile still below schema 3 (a
     * schema-2 object from an import or a client copy) takes ProfileMigration.v2toV3 first. battlesWon and bonusClaimed keep only ids a
     * campaignBattles or bonusObjectives row knows, and campaignCleared is derived from battlesWon.
     */
    void sanitize(RosterMath.Catalog c, Set<String> classIds, Set<String> sideIds, List<String> houses) {
        if (schema < 3) ProfileMigration.v2toV3(this, Spec.CAMPAIGNBATTLES);
        if (schema < 4) ProfileMigration.v3toV4(this);
        if (schema < 5) ProfileMigration.v4toV5(this);
        if (schema < 6) ProfileMigration.v5toV6(this);
        if (schema > SCHEMA) readOnly = true; // a save from a newer build: kept as it is, never written by this one
        else schema = SCHEMA;
        if (progression == null) progression = new Progression.State();
        progression.sanitize();
        gold = clamp(gold, 0, 1_000_000);
        matches = Math.max(0, matches);
        wins = clamp(wins, 0, matches);
        writs = Math.max(0, writs);
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
        if (RosterMath.validBanner(banner, c, owned.keySet()) != null) banner = RosterMath.repack(banner, c, owned.keySet());
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
