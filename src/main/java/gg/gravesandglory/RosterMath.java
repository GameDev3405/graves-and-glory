package gg.gravesandglory;

import java.util.*;
import java.util.random.RandomGenerator;

/**
 * Pure House roster maths (spec 1.3, 4 and 6.2): banner slots and validation, trait tiers, recruitment rolls with Epic pity,
 * stars, the progression cap and match rewards. No Minecraft types, so the standalone HousesCheck compiles it. Every number
 * comes from the sheets, through Spec or the caller.
 */
public final class RosterMath {
    private RosterMath() {}
    /** A banner slot filled by the Epic or Legendary unit before it. */
    public static final String MARKER = "^";
    /** A banner slot with no unit. */
    public static final String EMPTY = "empty";

    /** The House, rarity, slot weight and muster cost of each troop id. */
    public record Catalog(Map<String, String> house, Map<String, String> rarity, Map<String, Integer> slotWeight, Map<String, Integer> cost) {
        /** A catalog with no costs (every troop costs 0), for callers that never price a banner. */
        public Catalog(Map<String, String> house, Map<String, String> rarity, Map<String, Integer> slotWeight) { this(house, rarity, slotWeight, Map.of()); }
        /** The troop's muster cost (troops.cost); 0 for a marker, an empty slot or an unknown id. */
        public int cost(String id) {
            Integer v = id == null ? null : cost.get(id);
            return v == null ? 0 : v;
        }
        /** The troop's rarity (a rarities id), or null for a marker, an empty slot or an unknown id. */
        public String rarityOf(String id) { return id == null ? null : rarity.get(id); }
        /** The troop's House id, or null for a marker, an empty slot or an unknown id. */
        public String houseOf(String id) { return id == null ? null : house.get(id); }
        /** Banner slots the unit fills; a marker, an empty slot or an unknown id weighs 0. */
        public int weight(String id) {
            Integer w = id == null || id.equals(MARKER) || id.equals(EMPTY) ? null : slotWeight.get(id);
            return w == null ? 0 : w;
        }
        /** True for a troop id in the catalog. */
        public boolean known(String id) { return houseOf(id) != null; }

        private static Spec.TroopsRow[] cachedRows;
        private static Catalog cached;
        /** The catalog of the troops sheet: each troop's house, rarity, slotWeight and cost columns. */
        public static synchronized Catalog fromSpec() {
            if (cachedRows != Spec.TROOPS) {
                Map<String, String> house = new LinkedHashMap<>(), rarity = new LinkedHashMap<>();
                Map<String, Integer> weight = new LinkedHashMap<>(), cost = new LinkedHashMap<>();
                for (var t : Spec.TROOPS) {
                    house.put(t.id(), t.house());
                    rarity.put(t.id(), t.rarity());
                    weight.put(t.id(), t.slotWeight());
                    cost.put(t.id(), t.cost());
                }
                cached = new Catalog(Collections.unmodifiableMap(house), Collections.unmodifiableMap(rarity), Collections.unmodifiableMap(weight), Collections.unmodifiableMap(cost));
                cachedRows = Spec.TROOPS;
            }
            return cached;
        }
    }

    /** The gold a banner may spend at muster (rules.musterAllowance). */
    public static int allowance() { return Spec.RULES[0].musterAllowance(); }
    /** The troops one banner may hold (rules.maxTroops). */
    public static int maxTroops() { return Spec.RULES[0].maxTroops(); }
    /** The banners one profile may keep (rules.maxBanners). */
    public static int maxBanners() { return Spec.RULES[0].maxBanners(); }

    /**
     * Null when a war table banner is valid, else the first rule it breaks, checked in this order: range (a cell outside the
     * table), unknown (not a troop), not_owned, house (a House outside houses), count (more than maxTroops), cost (more than the
     * muster allowance), then the rarities.bannerMax limits, rarest first (legendary_limit, epic_limit).
     */
    public static String validTable(Map<Integer, String> table, Catalog c, Set<String> owned, Set<String> houses) {
        for (int i : table.keySet()) if (!WarGrid.inRange(i)) return "range";
        for (String id : table.values()) if (!c.known(id)) return "unknown";
        for (String id : table.values()) if (!owned.contains(id)) return "not_owned";
        for (String id : table.values()) if (!houses.contains(c.houseOf(id))) return "house";
        if (table.size() > maxTroops()) return "count";
        if (WarGrid.cost(table, c) > allowance()) return "cost";
        Map<String, Integer> units = new HashMap<>();
        for (String id : table.values()) units.merge(c.rarityOf(id), 1, Integer::sum);
        for (int i = Spec.RARITIES.length - 1; i >= 0; i--) {
            var r = Spec.RARITIES[i];
            if (units.getOrDefault(r.id(), 0) > r.bannerMax()) return r.id() + "_limit";
        }
        return null;
    }

    /** Slots in a banner (rules.squadSize). */
    static int bannerSize() { return Spec.RULES[0].squadSize(); }
    static int maxStars() { return Spec.RECRUITMENT[0].maxStars(); }
    static Spec.RaritiesRow rarityRow(String id) {
        for (var r : Spec.RARITIES) if (r.id().equals(id)) return r;
        return null;
    }
    /** How many units of a rarity one banner may hold (rarities.bannerMax); no limit for a rarity the sheet lacks. */
    static int bannerMax(String rarity) {
        var r = rarityRow(rarity);
        return r == null ? Integer.MAX_VALUE : r.bannerMax();
    }

    /**
     * Banner slots per House (army rework R5): each troop of the banner adds its slotWeight to its House, so a war table troop that takes one
     * cell still counts its weight toward House tiers. Markers, empty cells and unknown ids count for nothing. Houses with no slots are absent.
     */
    public static Map<String, Integer> houseSlots(List<String> banner, Catalog c) {
        Map<String, Integer> slots = new LinkedHashMap<>();
        if (banner != null) for (String id : banner) {
            String house = c.houseOf(id);
            if (house != null) slots.merge(house, Math.max(1, c.weight(id)), Integer::sum);
        }
        return slots;
    }

    /** A House's trait tier for its banner slots: one tier per threshold reached (houseTraits.slots of tiers I to III), so 0 to 3. */
    public static int traitTier(int slots, int[] thresholds) { return reached(slots, thresholds); }
    static int reached(int value, int[] thresholds) {
        int n = 0;
        for (int t : thresholds) if (value >= t) n++;
        return n;
    }

    /**
     * Null when a banner is valid, else the first rule it breaks, checked in this order: size (not rules.squadSize
     * entries), unknown (an entry that is not a troop, ^ or empty), not_owned, orphan_marker (a ^ no unit needs),
     * missing_marker (an Epic or Legendary followed by fewer than weight-1 markers), then the rarities.bannerMax limits,
     * rarest first (legendary_limit, epic_limit).
     */
    public static String validBanner(List<String> banner, Catalog c, Set<String> owned) {
        if (banner == null || banner.size() != bannerSize()) return "size";
        for (String id : banner) if (!MARKER.equals(id) && !EMPTY.equals(id) && !c.known(id)) return "unknown";
        for (String id : banner) if (c.known(id) && !owned.contains(id)) return "not_owned";
        int pending = 0;
        for (String id : banner) {
            if (MARKER.equals(id)) {
                if (pending == 0) return "orphan_marker";
                pending--;
                continue;
            }
            if (pending > 0) return "missing_marker";
            pending = Math.max(0, c.weight(id) - 1);
        }
        if (pending > 0) return "missing_marker";
        Map<String, Integer> units = new HashMap<>();
        for (String id : banner) if (c.known(id)) units.merge(c.rarityOf(id), 1, Integer::sum);
        for (int i = Spec.RARITIES.length - 1; i >= 0; i--) {
            var r = Spec.RARITIES[i];
            if (units.getOrDefault(r.id(), 0) > r.bannerMax()) return r.id() + "_limit";
        }
        return null;
    }

    /**
     * Rebuilds a banner from entries, in order. It skips empty slots, markers, unknown and unowned ids, and a unit past its
     * rarity's bannerMax (a Legendary after the first, an Epic after the second). Each other unit is appended with its
     * weight-1 markers when it still fits; then the banner is padded with empty to rules.squadSize.
     */
    public static List<String> repack(List<String> entries, Catalog c, Set<String> owned) {
        int size = bannerSize();
        List<String> out = new ArrayList<>(size);
        Map<String, Integer> units = new HashMap<>();
        if (entries != null) for (String id : entries) {
            if (!c.known(id) || !owned.contains(id)) continue;
            String rarity = c.rarityOf(id);
            int w = Math.max(1, c.weight(id));
            if (units.getOrDefault(rarity, 0) >= bannerMax(rarity) || out.size() + w > size) continue;
            out.add(id);
            for (int i = 1; i < w; i++) out.add(MARKER);
            units.merge(rarity, 1, Integer::sum);
        }
        while (out.size() < size) out.add(EMPTY);
        return out;
    }

    /** The banner of a new profile, and of a migrated one with no usable v1 line: 12 Order Squires, then Order Archers (8 at rules.squadSize 20). */
    public static List<String> starterBanner() {
        int size = bannerSize();
        List<String> out = new ArrayList<>(size);
        while (out.size() < Math.min(12, size)) out.add("knight");
        while (out.size() < size) out.add("archer");
        return out;
    }

    /** Units in a banner: its troop entries, so an Epic or a Legendary counts once and markers and empty slots not at all. */
    public static int units(List<String> banner, Catalog c) {
        int n = 0;
        if (banner != null) for (String id : banner) if (c.known(id)) n++;
        return n;
    }

    /** The Houses a banner may field in this build (houses.fieldable). */
    public static Set<String> fieldableHouses() {
        Set<String> out = new LinkedHashSet<>();
        for (var h : Spec.HOUSES) if (h.fieldable()) out.add(h.id());
        return out;
    }

    /** A House's name (houses.name), or the id itself when no House has it. */
    public static String houseName(String id) {
        for (var h : Spec.HOUSES) if (h.id().equals(id)) return h.name();
        return id;
    }

    /** The launch roster on the chapter line: every troop of a fieldable House a recruitment roll can give (Legendaries come from Pacts). */
    public static List<String> launchRoster() {
        Set<String> fieldable = fieldableHouses();
        List<String> out = new ArrayList<>();
        for (var t : Spec.TROOPS) if (fieldable.contains(t.house()) && !t.rarity().equals("legendary")) out.add(t.id());
        return out;
    }

    /**
     * The House a plain roll draws from: the fieldable House with the most slots in the banner (a tie goes to House order),
     * else the first fieldable House, else the first House of the sheet.
     */
    public static String rollHouse(List<String> banner) {
        Map<String, Integer> slots = houseSlots(banner, Catalog.fromSpec());
        Set<String> fieldable = fieldableHouses();
        String best = null;
        int most = 0;
        for (var h : Spec.HOUSES) if (fieldable.contains(h.id()) && slots.getOrDefault(h.id(), 0) > most) { most = slots.get(h.id()); best = h.id(); }
        return best != null ? best : fieldable.isEmpty() ? Spec.HOUSES[0].id() : fieldable.iterator().next();
    }

    /**
     * The banner as it can take the field: a unit of a House outside fieldable, and each of its markers, becomes empty, so
     * the other units keep their slots and a valid banner stays valid. The input is not changed.
     */
    public static List<String> fieldable(List<String> banner, Catalog c, Set<String> fieldable) {
        List<String> out = new ArrayList<>(banner == null ? List.of() : banner);
        boolean dropping = false;
        for (int i = 0; i < out.size(); i++) {
            String id = out.get(i);
            if (MARKER.equals(id)) { if (dropping) out.set(i, EMPTY); continue; }
            dropping = c.known(id) && !fieldable.contains(c.houseOf(id));
            if (dropping) out.set(i, EMPTY);
        }
        return out;
    }

    /**
     * The banner a War Table lobby commander takes into the match (Kort, 2026-10-31: the House pick fields only that House): the
     * banner with every unit of another House left empty, as fieldable does, and nothing at all when the House picked cannot take
     * the field in this build. A banner with no unit of the House comes back all empty; the lobby refuses Ready then.
     */
    public static List<String> houseBanner(List<String> banner, Catalog c, String house, Set<String> fieldable) {
        return fieldable(banner, c, house != null && fieldable.contains(house) ? Set.of(house) : Set.of());
    }

    /** One recruitment roll: the troop drawn, and the House's Epic pity counter after it. */
    public record Roll(String troop, int pityAfter) {}

    /**
     * Draws one troop from a House pool (spec 4.2). Each Common weighs wCommon, each Rare wRare and the Epic wEpic
     * (rarities.rollWeight); Legendaries are never in the pool. The Epic is forced when pity+1 >= epicPity, so at
     * recruitment.epicPity 40 the 40th roll in a row without an Epic gives it. pityAfter is 0 after an Epic, else pity+1.
     */
    public static Roll roll(List<String> commons, List<String> rares, String epic, int wCommon, int wRare, int wEpic, int pity, int epicPity, RandomGenerator rng) {
        if ((long) pity + 1 >= epicPity) return new Roll(epic, 0);
        int commonShare = commons.size() * wCommon, rareShare = rares.size() * wRare;
        int r = rng.nextInt(commonShare + rareShare + wEpic);
        if (r < commonShare) return new Roll(commons.get(r / wCommon), pity + 1);
        r -= commonShare;
        if (r < rareShare) return new Roll(rares.get(r / wRare), pity + 1);
        return new Roll(epic, 0);
    }

    /**
     * Stars from copies alone: star 1 for the first copy, star 2 after dupsStar2 more and star 3 after dupsStar3 more,
     * capped at recruitment.maxStars. A rarity with 0 duplicates there (Legendary) earns its stars with shards only.
     */
    public static int starFromCopies(int copies, Spec.RaritiesRow r) {
        if (copies < 1) return 0;
        int stars = 1;
        if (r.dupsStar2() > 0 && copies >= 1 + r.dupsStar2()) {
            stars = 2;
            if (r.dupsStar3() > 0 && copies >= 1 + r.dupsStar2() + r.dupsStar3()) stars = 3;
        }
        return Math.min(stars, maxStars());
    }

    /**
     * Adds one rolled or bought copy of an owned troop. Below recruitment.maxStars it counts toward the next star and 0 is
     * returned. At maxStars it is dismantled instead: copies stay as they are and rarities.dismantleShards are returned.
     */
    public static int addCopy(Profile.Owned o, Spec.RaritiesRow r) {
        if (o.stars >= maxStars()) return r.dismantleShards();
        o.copies++;
        o.stars = Math.max(o.stars, starFromCopies(o.copies, r));
        return 0;
    }

    /** The star bonus to health and damage: 0 at star 1, starPct2 at star 2, starPct3 at star 3. */
    public static double starPct(int stars, Spec.RaritiesRow r) { return stars >= 3 ? r.starPct3() : stars == 2 ? r.starPct2() : 0; }

    /** The progression multiplier for health or damage: 1 plus the star bonus, capped at 1 + capPct (recruitment.capPct). Stars alone since the in-match progression design. */
    public static double progressionMultiplier(double starPct, double capPct) { return Math.min(1 + capPct, 1 + starPct); }

    /** A House's Renown level for its points: one level per renown.points threshold reached, so 0 to 5. */
    public static int renownLevel(int points, int[] thresholds) { return reached(points, thresholds); }

    /** What one eligible match earns, per House. Houses that earn nothing are absent. */
    public record Rewards(Map<String, Integer> shards, Map<String, Integer> renown) {}

    /**
     * Shards and Renown for one eligible match (spec 4.3 and 4.5), in the campaign sheet's amounts. Every House whose banner
     * slots reach trait tier I earns completionShards shards and completionRenown Renown, plus victoryRenown more Renown on
     * a win. On a win the largest House in the banner also earns victoryShards shards, whatever its tier (Task 1 review
     * ruling, 2026-10-18); a tie goes to the House earlier in houseOrder. At the sheet's 1 / 1 / 1 / 1 a win gives the
     * largest House 2 shards when it is at tier I or higher and 1 when it is below.
     */
    public static Rewards matchRewards(Map<String, Integer> bannerSlots, int[] thresholds, List<String> houseOrder, boolean won) {
        var amounts = Spec.CAMPAIGN[0];
        Map<String, Integer> shards = new LinkedHashMap<>(), renown = new LinkedHashMap<>();
        String largest = null;
        int most = 0;
        for (String house : houseOrder) {
            int slots = bannerSlots.getOrDefault(house, 0);
            if (slots > most) { most = slots; largest = house; }
            if (traitTier(slots, thresholds) == 0) continue;
            earn(shards, house, amounts.completionShards());
            earn(renown, house, amounts.completionRenown() + (won ? amounts.victoryRenown() : 0));
        }
        if (won && largest != null) earn(shards, largest, amounts.victoryShards());
        return new Rewards(shards, renown);
    }
    /**
     * Pays an amount of the old gold in troop writs: (writGold + goldEquivalent) / 100 whole writs go to troopWrits (capped at 1,000,000)
     * and the remainder stays in writGold (0 to 99) for the next payout, so 80 then 40 makes 1 writ and leaves 20. Nothing or less pays nothing.
     * Gear shards are not touched here (Profile.payGold pays them beside this).
     */
    public static void payWrits(Profile p, int goldEquivalent) {
        if (goldEquivalent <= 0) return;
        long total = Math.max(0, p.writGold) + (long) goldEquivalent;
        p.troopWrits = (int) Math.min(1_000_000L, (long) p.troopWrits + total / 100);
        p.writGold = (int) (total % 100);
    }
    /** A match's rewards for a player seated only part of it: every shard and Renown count pro-rated (MatchRules.prorate, rounded down), a count that falls to 0 dropped. */
    public static Rewards prorate(Rewards full, long seated, long total) {
        return new Rewards(prorate(full.shards(), seated, total), prorate(full.renown(), seated, total));
    }
    private static Map<String, Integer> prorate(Map<String, Integer> counts, long seated, long total) {
        Map<String, Integer> out = new LinkedHashMap<>();
        counts.forEach((house, n) -> { int part = MatchRules.prorate(n, seated, total); if (part > 0) out.put(house, part); });
        return out;
    }
    static void earn(Map<String, Integer> counts, String house, int amount) { if (amount > 0) counts.merge(house, amount, Integer::sum); }

    /** A troop's mastery level: the mastery rows reached in level order, each needing both its kills and its matches survived. */
    public static int masteryLevel(int kills, int matches, Spec.MasteryRow[] rows) {
        int level = 0;
        for (var r : Arrays.stream(rows).sorted(Comparator.comparingInt(Spec.MasteryRow::level)).toList()) {
            if (kills < r.kills() || matches < r.matches()) break;
            level = r.level();
        }
        return level;
    }

    /**
     * The Long March chapters that pay a Pact now (spec 4.3): each chapter whose every battle (the rows of that chapter) has an order
     * at or below cleared, the highest order won, and that is not in paid. Ascending chapter order.
     */
    public static List<Integer> pactChapters(List<Spec.CampaignBattlesRow> rows, int cleared, Set<Integer> paid) {
        Set<Integer> chapters = new TreeSet<>();
        for (var r : rows) chapters.add(r.chapter());
        List<Integer> out = new ArrayList<>();
        for (int chapter : chapters)
            if (!paid.contains(chapter) && rows.stream().filter(r -> r.chapter() == chapter).allMatch(r -> r.order() <= cleared)) out.add(chapter);
        return out;
    }
}
