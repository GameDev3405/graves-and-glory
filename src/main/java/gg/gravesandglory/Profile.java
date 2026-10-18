package gg.gravesandglory;

import java.util.*;

/**
 * A commander's saved collection, schema 2 (spec 6.2): the banner, the owned troops with stars, stages and mastery, and per
 * House shards, Renown and Epic pity. Server-owned JSON, one file per player. Pure, so the standalone checks compile it;
 * it replaces Campaign.Profile in Task 2.
 */
public final class Profile {
    /** One owned troop type: copies received, stars (1 to 3), evolution stage (1 to 3), and its mastery ledger. */
    static final class Owned { int copies=1, stars=1, stage=1, kills, survived, mastery; String variant="base"; }

    int schema = 2;
    int gold, matches, wins;
    boolean tutorialDone;
    String commander = "champion";
    /** rules.squadSize slots: troop ids, ^ for a slot of the Epic or Legendary before it, and empty. */
    List<String> banner = RosterMath.starterBanner();
    /** The side row (factions) this commander asks to fly. */
    String bannerColour = "gold";
    Map<String, Owned> owned = new LinkedHashMap<>();
    Map<String, Integer> shards = new LinkedHashMap<>(), renown = new LinkedHashMap<>(), pity = new LinkedHashMap<>();
    int writs;
    Progression.State progression = new Progression.State();
    /** The Long March: the highest campaignBattles order won (Phase 1). */
    int campaignCleared;
    /** Campaign chapters whose Legendary Writ was granted, so each chapter grants one. */
    Set<Integer> writChapters = new LinkedHashSet<>();
    /** Unlocked banner cosmetics (renown rewards such as cosmetic:pennant). */
    Set<String> cosmetics = new LinkedHashSet<>();

    /**
     * Repairs a loaded or migrated profile in place. Owned ids the catalog does not know are dropped. Stars clamp to
     * 1..recruitment.maxStars and stages to 1..the troop's last evolutions stage, which is 1 for Epics and Legendaries (they
     * have no evolve line). A banner that fails validBanner is repacked. An unknown class becomes champion and an unknown
     * side colour gold. Shards, renown and pity hold exactly the given Houses, each at 0 or more.
     */
    void sanitize(RosterMath.Catalog c, Set<String> classIds, Set<String> sideIds, List<String> houses) {
        schema = 2;
        if (progression == null) progression = new Progression.State();
        progression.sanitize();
        gold = clamp(gold, 0, 1_000_000);
        matches = Math.max(0, matches);
        wins = clamp(wins, 0, matches);
        writs = Math.max(0, writs);
        campaignCleared = Math.max(0, campaignCleared);
        if (commander == null || !classIds.contains(commander)) commander = "champion";
        if (bannerColour == null || !sideIds.contains(bannerColour)) bannerColour = "gold";
        if (owned == null) owned = new LinkedHashMap<>();
        owned.entrySet().removeIf(e -> e.getValue() == null || !c.known(e.getKey()));
        int maxMastery = Arrays.stream(Spec.MASTERY).mapToInt(Spec.MasteryRow::level).max().orElse(0);
        for (var e : owned.entrySet()) {
            Owned o = e.getValue();
            o.copies = Math.max(1, o.copies);
            o.stars = clamp(o.stars, 1, Spec.RECRUITMENT[0].maxStars());
            o.stage = clamp(o.stage, 1, lastStage(e.getKey()));
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
    }
    static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    /** The last evolution stage a troop can reach: its highest evolutions.stage, or 1 without an evolve line. */
    static int lastStage(String troop) {
        int stage = 1;
        for (var e : Spec.EVOLUTIONS) if (e.troop().equals(troop)) stage = Math.max(stage, e.stage());
        return stage;
    }
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
