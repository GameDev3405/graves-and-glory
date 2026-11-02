package gg.gravesandglory;

import java.util.*;

/**
 * A commander's saved collection, schema 3 (spec 6.2, plan 2C.3): the banner, the owned troops with stars and mastery, per
 * House shards, Renown and Epic pity, and the Long March battles won and bonuses paid. Server-owned JSON, one file per player
 * (Campaign.profile loads it, migrating a schema-1 or schema-2 file first). Pure, so the standalone checks compile it.
 */
public final class Profile {
    /** The JSON key of progression. Campaign.forClient finds the nested state by it to strip the match ids; a rename of the field that leaves this behind sends them again, which p1-report-snapshot catches. */
    static final String PROGRESSION = "progression";
    /** One owned troop type: copies received, stars (1 to 3) and its mastery ledger. A stored "stage" field of an older file is ignored: Gson drops a key the class lacks. */
    static final class Owned { int copies=1, stars=1, kills, survived, mastery; String variant="base"; }

    /** The current profile schema; ProfileMigration upgrades older files to it. */
    static final int SCHEMA = 3;
    int schema = SCHEMA;
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

    /**
     * Repairs a loaded or migrated profile in place. Owned ids the catalog does not know are dropped. Stars clamp to
     * 1..recruitment.maxStars. A banner that fails validBanner is repacked. An unknown class becomes champion and an unknown
     * side colour gold. Shards, renown and pity hold exactly the given Houses, each at 0 or more. A profile still below schema 3 (a
     * schema-2 object from an import or a client copy) takes ProfileMigration.v2toV3 first. battlesWon and bonusClaimed keep only ids a
     * campaignBattles or bonusObjectives row knows, and campaignCleared is derived from battlesWon.
     */
    void sanitize(RosterMath.Catalog c, Set<String> classIds, Set<String> sideIds, List<String> houses) {
        if (schema < SCHEMA) ProfileMigration.v2toV3(this, Spec.CAMPAIGNBATTLES);
        schema = SCHEMA;
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
    }
    /** One copy of each Common in the troops sheet, in sheet order. */
    static Map<String, Owned> commons() {
        Map<String, Owned> out = new LinkedHashMap<>();
        for (var t : Spec.TROOPS) if (t.rarity().equals("common")) out.put(t.id(), new Owned());
        return out;
    }
    /** sanitize against the sheets: the troops catalog, the classes, the side rows and the House order. */
    void sanitize() {
        Set<String> classes = new LinkedHashSet<>(), sides = new LinkedHashSet<>();
        for (var c : Spec.CLASSES) classes.add(c.id());
        for (var f : Spec.FACTIONS) sides.add(f.id());
        sanitize(RosterMath.Catalog.fromSpec(), classes, sides, houseOrder());
    }
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
