package gg.gravesandglory;

import static gg.gravesandglory.CounterMath.*;
import static gg.gravesandglory.RosterMath.*;

import java.util.*;
import java.util.random.RandomGenerator;

/** Pure Phase 2 House rules: the counter table, House traits, the progression cap, stars, rolls, banners and rewards. Never shipped. */
public final class HousesCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static boolean eq(double a, double b) { return Math.abs(a - b) < 1e-9; }

    // The fixture catalog: house, rarity and slot weight of all 30 troops (spec section 2), each House in houses.troops order.
    static final List<String> HOUSES = List.of("gilded", "graveborn", "wildmoot", "ironclad", "tidecallers");
    static final String[][] ROSTER = {
        {"knight", "archer", "sentinel", "longbow", "mage", "gl_marshal"},
        {"revenant", "bonebow", "reaper", "stalker", "hexer", "gb_hollowking"},
        {"wm_hound", "wm_hunter", "wm_stalker", "wm_tusk", "wm_seer", "wm_oldhorn"},
        {"ic_hearthguard", "ic_arbalist", "ic_breaker", "ic_ballista", "ic_mortar", "ic_colossus"},
        {"tc_raider", "tc_harpooner", "tc_tideguard", "tc_mist", "tc_brute", "tc_admiral"}};
    static final String[] RARITY = {"common", "common", "rare", "rare", "epic", "legendary"};
    static final int[] WEIGHT = {1, 1, 1, 1, 2, 4};
    static final Catalog cat = fixture();
    static final Set<String> ALL = cat.house().keySet();
    static Catalog fixture() {
        Map<String, String> house = new LinkedHashMap<>(), rarity = new LinkedHashMap<>();
        Map<String, Integer> weight = new LinkedHashMap<>();
        for (int h = 0; h < ROSTER.length; h++) for (int i = 0; i < RARITY.length; i++) {
            house.put(ROSTER[h][i], HOUSES.get(h)); rarity.put(ROSTER[h][i], RARITY[i]); weight.put(ROSTER[h][i], WEIGHT[i]);
        }
        return new Catalog(house, rarity, weight);
    }

    static Spec.RaritiesRow rarity(String id) {
        for (var r : Spec.RARITIES) if (r.id().equals(id)) return r;
        throw new AssertionError("no rarity row " + id);
    }
    static Spec.HousesRow houseRow(String id) {
        for (var h : Spec.HOUSES) if (h.id().equals(id)) return h;
        throw new AssertionError("no house row " + id);
    }
    /** A House's trait thresholds (slots per tier, tier I first), from the houseTraits sheet. */
    static int[] thresholds(String house) {
        return Arrays.stream(Spec.HOUSETRAITS).filter(t -> t.house().equals(house)).sorted(Comparator.comparingInt(Spec.HouseTraitsRow::tier)).mapToInt(Spec.HouseTraitsRow::slots).toArray();
    }
    static final int[] T = thresholds("gilded");
    static final int[] R = Arrays.stream(Spec.RENOWN).sorted(Comparator.comparingInt(Spec.RenownRow::level)).mapToInt(Spec.RenownRow::points).toArray();
    static final Spec.MasteryRow[] M = Spec.MASTERY;
    static final Spec.RaritiesRow C = rarity("common");

    /** "knight*12,archer*6,mage,^" as a list: each part is an id, optionally times a count. */
    static List<String> banner(String spec) {
        List<String> out = new ArrayList<>();
        for (String part : spec.split(",")) {
            String[] p = part.split("\\*");
            for (int i = 0, n = p.length > 1 ? Integer.parseInt(p[1]) : 1; i < n; i++) out.add(p[0]);
        }
        return out;
    }
    static final List<String> SPEC_EXAMPLE_BANNER = List.of("knight","knight","knight","knight","sentinel","sentinel","archer","archer","longbow","mage","^","gl_marshal","^","^","^","revenant","revenant","bonebow","hexer","^");
    static final List<String> V1_DAWN_DEFAULT = banner("knight*12,archer*6,mage*2");
    static final List<String> EXPECTED_DAWN_REPACK = banner("knight*12,archer*6,mage,^");
    static final List<String> TWO_LEGENDARIES = banner("gl_marshal,^,^,^,gb_hollowking,^,^,^,knight*12");
    static final List<String> THREE_EPICS = banner("mage,^,hexer,^,wm_seer,^,knight*14");
    static long legendaries(List<String> banner) { return banner.stream().filter(id -> "legendary".equals(cat.rarityOf(id))).count(); }
    static long epics(List<String> banner) { return banner.stream().filter(id -> "epic".equals(cat.rarityOf(id))).count(); }

    // Hits: tags are comma lists; the bearing is 0 in front of the target and 180 behind it.
    static Set<String> tags(String csv) { return Set.of(csv.split(",")); }
    static Hit hit(String attacker, String target, double bearing, boolean raised) { return new Hit(tags(attacker), tags(target), bearing, raised, false, false, false, false, false); }
    static Hit impact(String attacker, String target) { return new Hit(tags(attacker), tags(target), 10, false, true, true, false, false, false); }
    static Hit charging(String attacker, String target) { return new Hit(tags(attacker), tags(target), 10, false, false, true, false, false, false); }
    static Hit holdHit(String attacker, String target) { return new Hit(tags(attacker), tags(target), 10, false, false, false, true, false, false); }
    static Hit noSky(String attacker) { return new Hit(tags(attacker), tags("any"), 10, false, false, false, false, true, false); }
    static Hit mist(String attacker) { return new Hit(tags(attacker), tags("any"), 10, false, false, false, false, false, true); }
    static Hit covered(String attacker, String target, double bearing) { return new Hit(tags(attacker), tags(target), bearing, false, false, false, false, false, false, true, false); }
    static Hit area(String attacker, String target, double bearing) { return new Hit(tags(attacker), tags(target), bearing, false, false, false, false, false, false, false, true); }
    static Spec.AbilitiesRow ability(String id) { for (var r : Spec.ABILITIES) if (r.id().equals(id)) return r; throw new AssertionError("no abilities row " + id); }
    static Spec.ClassesRow classRow(String id) { for (var r : Spec.CLASSES) if (r.id().equals(id)) return r; throw new AssertionError("no classes row " + id); }
    static Spec.TroopsRow troop(String id) { for (var r : Spec.TROOPS) if (r.id().equals(id)) return r; throw new AssertionError("no troops row " + id); }
    static double counters(Hit h) { return CounterMath.counters(h, Spec.COUNTERS); }
    static Spec.HouseTraitsRow trait(String house, int tier) { return CounterMath.trait(Spec.HOUSETRAITS, house, tier); }

    // Rolls: a House pool comes from houses.troops and the catalog rarity; weights come from rarities.rollWeight.
    static List<String> pool(String house, String rarity) {
        List<String> out = new ArrayList<>();
        for (String id : houseRow(house).troops().split(",")) if (rarity.equals(cat.rarityOf(id))) out.add(id);
        return out;
    }
    static Roll rollOnce(String house, int pity, int epicPity, RandomGenerator rng) {
        return roll(pool(house, "common"), pool(house, "rare"), pool(house, "epic").get(0), rarity("common").rollWeight(), rarity("rare").rollWeight(), rarity("epic").rollWeight(), pity, epicPity, rng);
    }
    record Shares(double common, double rare, double epic) {}
    static Shares shares(int n, RandomGenerator rng, int epicPity) {
        int[] count = new int[3];
        int pity = 0;
        for (int i = 0; i < n; i++) {
            Roll r = rollOnce("gilded", pity, epicPity, rng);
            pity = r.pityAfter();
            switch (cat.rarityOf(r.troop())) {
                case "common" -> count[0]++;
                case "rare" -> count[1]++;
                case "epic" -> count[2]++;
                default -> throw new AssertionError("rolled " + r.troop());
            }
        }
        return new Shares(count[0] / (double) n, count[1] / (double) n, count[2] / (double) n);
    }
    /** Always draws 0, so every roll that is not forced takes the first Common. */
    static final RandomGenerator ZERO = () -> 0L;
    static int firstEpicWithZeroRng() {
        int pity = 0;
        for (int n = 1; n <= 1000; n++) {
            Roll r = rollOnce("gilded", pity, Spec.RECRUITMENT[0].epicPity(), ZERO);
            if ("epic".equals(cat.rarityOf(r.troop()))) return n;
            pity = r.pityAfter();
        }
        return -1;
    }
    static int pityAfterEpic() {
        int epicPity = Spec.RECRUITMENT[0].epicPity();
        Roll r = rollOnce("gilded", epicPity - 1, epicPity, ZERO);
        return r.troop().equals("mage") ? r.pityAfter() : -1;
    }
    static boolean noLegendaryIn(int n) {
        var rng = new SplittableRandom(11);
        int[] pity = new int[HOUSES.size()];
        for (int i = 0; i < n; i++) {
            int h = i % HOUSES.size();
            Roll r = rollOnce(HOUSES.get(h), pity[h], Spec.RECRUITMENT[0].epicPity(), rng);
            pity[h] = r.pityAfter();
            if ("legendary".equals(cat.rarityOf(r.troop())) || !HOUSES.get(h).equals(cat.houseOf(r.troop()))) return false;
        }
        return rarity("legendary").rollWeight() == 0;
    }
    static int addCopyAt(int copies, String rarityId) {
        var r = rarity(rarityId);
        var o = new Profile.Owned();
        o.copies = copies;
        o.stars = starFromCopies(copies, r);
        return addCopy(o, r);
    }

    /** Always draws the top of the range, so a roll that is not forced takes the Epic, the last share of the pool. */
    static final RandomGenerator TOP = new RandomGenerator() {
        @Override public long nextLong() { return -1L; }
        @Override public int nextInt(int bound) { return bound - 1; }
    };
    /** Pity after a rolled (not forced) Epic: a Gilded roll at pity 5, well below recruitment.epicPity. */
    static int rolledEpicPity() {
        Roll r = rollOnce("gilded", 5, Spec.RECRUITMENT[0].epicPity(), TOP);
        return r.troop().equals("mage") ? r.pityAfter() : -1;
    }
    /** Stars after adding one copy to an owned troop with these copies and stars; -1 if the copy was dismantled or not counted. */
    static int starsAfterCopy(int copies, int stars, String rarityId) {
        var o = new Profile.Owned();
        o.copies = copies;
        o.stars = stars;
        return addCopy(o, rarity(rarityId)) == 0 && o.copies == copies + 1 ? o.stars : -1;
    }

    /** A Founders state at tier 30, every tier open. */
    static Progression.State tier30() {
        var s = new Progression.State();
        s.xp = Progression.TIERS * Progression.XP_PER_TIER;
        return s;
    }
    /** Task 11 review minor 3: a Pact pays for a chapter only when every battle of it is cleared, pinned on a two-battle chapter (the sheet's chapter 1 has one). */
    static void pacts() {
        var rows = List.of(march("march-1", 1, 1), march("march-2", 1, 2), march("march-3", 2, 3));
        check(pactChapters(rows, 0, Set.of()).isEmpty(), "pacts: nothing cleared pays nothing");
        check(pactChapters(rows, 1, Set.of()).isEmpty(), "pacts: battle 1 of a two-battle chapter cleared pays no Pact (every battle of the chapter, not any)");
        check(pactChapters(rows, 2, Set.of()).equals(List.of(1)), "pacts: both battles of chapter 1 cleared pay its Pact: " + pactChapters(rows, 2, Set.of()));
        check(pactChapters(rows, 3, Set.of(1)).equals(List.of(2)) && pactChapters(rows, 3, Set.of(1, 2)).isEmpty(), "pacts: a chapter already paid pays again never");
        check(pactChapters(rows, 3, Set.of()).equals(List.of(1, 2)), "pacts: two chapters cleared at once pay two, in chapter order");
        check(pactChapters(Arrays.asList(Spec.CAMPAIGNBATTLES), Spec.CAMPAIGNBATTLES.length, Set.of()).size() == Arrays.stream(Spec.CAMPAIGNBATTLES).map(Spec.CampaignBattlesRow::chapter).distinct().count(), "pacts: the sheet's Long March cleared pays one Pact a chapter");
    }
    static Spec.CampaignBattlesRow march(String id, int chapter, int order) { return new Spec.CampaignBattlesRow(id, chapter, order, id, 0, 1, "easy", false, "", ""); }
    /** The re-authored Founders chapter (spec 4.8) and the House-aware contract claims (spec 4.5), through Progression.claim alone. */
    static void founders() {
        var s = tier30();
        var gold1 = Progression.claim(s, "founders:1");
        check(gold1.gold() == 30 && gold1.shards() == 0 && gold1.troop() == null && s.claims.contains("founders:1"), "founders:1 gives 30 gold: " + gold1);
        var bare = Progression.claim(s, "founders:2");
        check(bare.gold() == 0 && bare.shards() == 0 && bare.message().contains("Choose a House") && !s.claims.contains("founders:2"), "founders:2 without a House gives nothing, asks for one and stays unclaimed: " + bare);
        var shards = Progression.claim(s, "founders:2:gilded");
        check(shards.shards() == 10 && "gilded".equals(shards.house()) && shards.gold() == 0 && s.claims.contains("founders:2") && s.claims.stream().noneMatch(k -> k.startsWith("founders:2:")), "founders:2:gilded gives 10 shards to gilded and is stored as founders:2: " + shards + " " + s.claims);
        var nowhere = Progression.claim(s, "founders:4:nowhere");
        check(nowhere.shards() == 0 && !s.claims.contains("founders:4"), "a shard tier naming no House gives nothing: " + nowhere);
        var epic = Progression.claim(s, "founders:20:mage");
        check(epic.troop() == null && epic.gold() == 0 && !s.claims.contains("founders:20"), "founders:20:mage (an Epic) is refused: " + epic);
        var noRare = Progression.claim(s, "founders:20");
        check(noRare.troop() == null && !s.claims.contains("founders:20"), "founders:20 without a troop is refused: " + noRare);
        var rare = Progression.claim(s, "founders:20:longbow");
        check("longbow".equals(rare.troop()) && rare.gold() == 0 && s.claims.contains("founders:20"), "founders:20:longbow grants the Order Longbow: " + rare);
        var pennant = Progression.claim(s, "founders:21");
        check("pennant_pattern".equals(pennant.cosmetic()) && s.claims.contains("founders:21"), "founders:21 gives the cosmetic pennant_pattern: " + pennant);
        var frame = Progression.claim(s, "founders:29");
        check("banner_frame".equals(frame.cosmetic()), "founders:29 gives the cosmetic banner_frame: " + frame);
        var top = Progression.claim(s, "founders:30");
        check(top.gold() == 0 && s.titles.contains("Bannerlord"), "founders:30 gives the title Bannerlord: " + top);
        // every tier claimed once, each with what it asks for: 300 gold, 12 x 10 shards, the five titles (no Vanguard), one Rare, two cosmetics
        var all = tier30();
        int gold = 0, shardTotal = 0;List<String> troops = new ArrayList<>(), cosmetics = new ArrayList<>();
        for (int t = 1; t <= Progression.TIERS; t++) {
            var g = Progression.claim(all, "founders:" + t + (t == 20 ? ":longbow" : ":graveborn"));
            gold += g.gold();shardTotal += g.shards();if (g.troop() != null) troops.add(g.troop());if (g.cosmetic() != null) cosmetics.add(g.cosmetic());
        }
        check(gold == 300 && shardTotal == 120 && troops.equals(List.of("longbow")) && cosmetics.equals(List.of("pennant_pattern", "banner_frame")) && all.claims.size() == 30, "all 30 tiers: gold " + gold + " (want 300), shards " + shardTotal + " (want 120), troops " + troops + ", cosmetics " + cosmetics + ", claims " + all.claims.size());
        check(all.titles.equals(new LinkedHashSet<>(List.of("Recruit", "Scout", "Pathfinder", "Captain", "Veteran", "Bannerlord"))), "the chapter's titles are Scout, Pathfinder, Captain, Veteran and Bannerlord: " + all.titles);
        var old = tier30();old.claims.add("founders:2");
        var again = Progression.claim(old, "founders:2:gilded");
        check(again.shards() == 0 && again.gold() == 0 && again.message().equals("Already claimed."), "a state holding the old claim founders:2 returns Already claimed: " + again);
        // the daily that completes all three gives +3 shards to its House; the first two give none
        var d = new Progression.State();Progression.reset(d, java.time.LocalDate.of(2026, 10, 23));
        for (int i = 0; i < 3; i++) Progression.match(d, "m" + i, true, true);
        d.dailyCaptures = 1;
        var d0 = Progression.claim(d, "daily:0:ironclad");var d1 = Progression.claim(d, "daily:1:ironclad");
        check(d0.gold() == 30 && d0.shards() == 0 && d1.gold() == 40 && d1.shards() == 0, "the first two dailies pay gold and no shards: " + d0 + " " + d1);
        var d2bare = Progression.claim(d, "daily:2");
        check(d2bare.shards() == 0 && d2bare.gold() == 0 && d2bare.message().contains("Choose a House") && !d.claims.contains("daily:2"), "the third daily without a House waits for one: " + d2bare);
        var d2 = Progression.claim(d, "daily:2:ironclad");
        check(d2.gold() == 30 && d2.shards() == 3 && "ironclad".equals(d2.house()) && d.claims.equals(Set.of("daily:0", "daily:1", "daily:2")), "the third daily with :ironclad gives 3 Ironclad shards and every key is stored bare: " + d2 + " " + d.claims);
        check(Progression.claim(d, "daily:0:ironclad").message().equals("Already claimed.") && Progression.claim(d, "daily:00:ironclad").gold() == 0, "a daily pays once, whatever suffix or spelling");
        // the weekly: 150 gold, 300 XP and +10 shards to a chosen House
        var w = new Progression.State();Progression.reset(w, java.time.LocalDate.of(2026, 10, 23));
        for (int i = 0; i < 6; i++) Progression.match(w, "w" + i, true, true);
        var wBare = Progression.claim(w, "weekly");var wk = Progression.claim(w, "weekly:tidecallers");
        check(wBare.gold() == 0 && wBare.message().contains("Choose a House") && wk.gold() == 150 && wk.xp() == 300 && wk.shards() == 10 && "tidecallers".equals(wk.house()) && w.claims.contains("weekly"), "the weekly asks for a House, then gives 150 gold, 300 XP and 10 shards to it: " + wBare + " " + wk);
    }

    public static void main(String[] args) {
        // the sheets agree with the spec section 2 fixture
        check(Arrays.stream(Spec.HOUSES).map(Spec.HousesRow::id).toList().equals(HOUSES), "houses sheet lists the five Houses in order");
        for (int h = 0; h < ROSTER.length; h++) check(houseRow(HOUSES.get(h)).troops().equals(String.join(",", ROSTER[h])), "houses." + HOUSES.get(h) + ".troops is the spec roster in rarity order");
        for (int i = 0; i < RARITY.length; i++) check(rarity(RARITY[i]).slotWeight() == WEIGHT[i], "rarities." + RARITY[i] + ".slotWeight is " + WEIGHT[i]);
        for (String house : HOUSES) check(T.length == 3 && Arrays.equals(thresholds(house), T), "houseTraits " + house + " has tiers I to III at the same slot counts");
        // counters (Spec.COUNTERS)
        check(eq(counters(hit("arrow","shield",10,true)),0.35) && eq(counters(hit("arrow","shield",10,false)),0.75) && eq(counters(hit("arrow","shield",150,true)),1.0), "C1/C2 front only");
        check(eq(counters(hit("melee","shield",10,true)),0.6) && eq(counters(hit("melee","shield",130,true)),1.3), "C3 front, C16 flank");
        check(eq(counters(hit("melee","shield",100,true)),1.0) && eq(counters(hit("melee","shield",111,true)),1.3) && Spec.COMBATDISCIPLINE[0].flankArcDegrees()==110, "C3 and C16: a hit past the shield's 90 degrees but inside the 110 flank arc gets neither");
        check(eq(counters(hit("melee,sunder","shield",10,true)),1.4) && eq(counters(hit("arrow,armour_bolt","shield",10,true)),0.7) && eq(counters(hit("magic","shield",10,true)),1.0), "C14 C15 C4");
        check(eq(counters(impact("melee","ranged")),1.5) && eq(counters(charging("melee","ranged")),1.2) && eq(counters(impact("melee,beast,cavalry","ranged")),1.5) && eq(counters(hit("melee,beast,cavalry","ranged",10,false)),1.3), "C5-C7 highest applies");
        check(eq(counters(hit("melee,spear","beast",10,false)),1.6) && eq(counters(hit("melee,beast","spear,holding",10,false)),0.7) && eq(counters(hit("melee,beast","spear,holding",120,false)),1.3), "C8 C9");
        check(eq(counters(hit("siege","structure",0,false)),4.0) && eq(counters(hit("nonsiege","structure",0,false)),0.25) && eq(counters(hit("siege","large",0,false)),1.3) && eq(counters(hit("siege","small",0,false)),0.6), "C10-C13");
        check(eq(counters(mist("arrow,shot")),0.5) && eq(counters(holdHit("arrow,shot,steady","any")),1.2) && eq(counters(noSky("arrow,shot,ambush")),1.25) && eq(counters(holdHit("melee,brace","charging")),1.3), "C17 C18");
        check(eq(counters(hit("melee,sunder","shield",130,true)),1.4*1.3) && CounterMath.applied(hit("melee,sunder","shield",130,true),Spec.COUNTERS).equals(List.of("C14","C16")), "separate groups multiply");
        // the no-routing design (Task 8b): flank shots (C16s), covered flanks and area blows; the combatDiscipline columns the morale sheet held; fearless and dread as a stance and an aura
        check(eq(counters(hit("arrow,shot","any",130,false)),1.15) && CounterMath.applied(hit("arrow,shot","any",130,false),Spec.COUNTERS).equals(List.of("C16s")) && eq(counters(hit("arrow,shot","any",10,false)),1.0) && eq(counters(hit("bolt,shot","shield",130,true)),1.15), "C16s: a shot into a flank or rear lands x1.15, from the front x1");
        check(eq(counters(covered("melee","any",130)),1.0) && CounterMath.applied(covered("melee","any",130),Spec.COUNTERS).isEmpty() && eq(counters(covered("shot","any",130)),1.0), "a covered flank (Bulwark) takes neither C16 nor C16s");
        check(eq(counters(area("melee","any",130)),1.0) && CounterMath.applied(area("melee","any",130),Spec.COUNTERS).isEmpty() && eq(counters(area("melee,sunder","shield",130)),1.4) && CounterMath.applied(area("melee,sunder","shield",130),Spec.COUNTERS).equals(List.of("C14")), "area damage (a cleave's second target, a slam, blight) takes no flank row; the other rows still apply (C14)");
        var D=Spec.COMBATDISCIPLINE[0];
        check(D.lastStandShare()==0.25 && D.lastStandRadius()==16 && D.hornCooldownSeconds()==8 && D.hornVolume()==1.6 && D.woundedShare()==0.3 && D.lastStandFeedSeconds()==30 && D.arriveRadius()==4.0, "combatDiscipline carries the last stand, the feedback cue, the wounded share and the arrival radius");
        check(Arrays.stream(Spec.class.getDeclaredFields()).noneMatch(f->f.getName().equals("MORALE")), "Spec has no MORALE table: the morale sheet is gone");
        check(ability("fearless").amount()==0.2 && ability("fearless").amount2()==0.3 && ability("dread").amount2()==0 && ability("oath_banner").anchors() && Arrays.stream(Spec.ABILITIES).filter(Spec.AbilitiesRow::anchors).count()==1, "fearless is +20% at 30% health, dread routs nothing, the Oath Banner alone anchors");
        check(classRow("marshal").value1()==0.2 && classRow("marshal").value2()==0 && classRow("marshal").active().equals("rally_horn"), "the Marshal's Muster (the sheet's rally_horn): +20% speed, nothing steadies");
        check(Set.of(troop("ic_colossus").tags().split(",")).contains("large") && !Set.of(troop("ic_colossus").tags().split(",")).contains("fearless") && Set.of(troop("bonebow").tags().split(",")).contains("fearless"), "Brassjaw is large and not fearless; the Bone Archer is fearless");
        check(CounterMath.applied(impact("melee,beast,cavalry","ranged"),Spec.COUNTERS).equals(List.of("C5")) && CounterMath.applied(hit("arrow,armour_bolt","shield",10,true),Spec.COUNTERS).equals(List.of("C15")) && CounterMath.applied(hit("arrow","any",10,false),Spec.COUNTERS).isEmpty(), "applied names the row that wins each group");
        check(total(0.1,1,1,1,.25,2.5)==0.25 && total(2,1.5,1,1,.25,2.5)==2.5 && eq(total(1.4,0.85,1,0.75,.25,2.5),0.8925), "clamp");
        check(Spec.COMBATDISCIPLINE[0].multiplierMin()==0.25 && Spec.COMBATDISCIPLINE[0].multiplierMax()==2.5 && Spec.COMBATDISCIPLINE[0].chargeImpactTicks()==60, "combatDiscipline holds the clamp and the charge-impact window");
        // traits
        check(traitTier(5,T)==0 && traitTier(6,T)==1 && traitTier(10,T)==2 && traitTier(14,T)==3 && traitTier(20,T)==3, "tiers 6/10/14");
        check(houseSlots(SPEC_EXAMPLE_BANNER,cat).equals(Map.of("gilded",15,"graveborn",5)), "spec 6.2 banner: Gilded 15, Graveborn 5");
        check(houseSlots(starterBanner(),cat).equals(Map.of("gilded",20)) && houseSlots(banner("knight,^,empty*18"),cat).equals(Map.of("gilded",1)), "a ^ counts only for the unit it belongs to");
        check(trait("gilded",0)==null && trait("gilded",2).id().equals("gilded_2") && bulwark(null,true,"arrow")==1, "tier 0 has no trait row");
        check(eq(bulwark(trait("gilded",3),true,"arrow"),0.55) && eq(bulwark(trait("gilded",3),true,"blight"),0.55) && bulwark(trait("gilded",3),false,"arrow")==1 && bulwark(trait("gilded",3),true,"melee")==1, "bulwark");
        check(eq(bulwark(trait("gilded",3),true,"bolt"),0.55), "bulwark: a bolt is a ranged hit too");
        check(eq(packHunters(trait("wildmoot",2),3,0),1.2) && eq(packHunters(trait("wildmoot",2),2,120),1.2) && eq(packHunters(trait("wildmoot",2),3,120),1.2) && packHunters(trait("wildmoot",2),2,90)==1, "pack hunters once");
        check(eq(entrench(trait("ironclad",1),true,false),0.9) && eq(entrench(trait("ironclad",1),true,true),0.8) && entrench(trait("ironclad",1),false,true)==1, "entrench doubled on points");
        check(eq(riptideSpeed(trait("tidecallers",2),true),1.25) && riptideSpeed(trait("tidecallers",2),false)==1 && riptideSpeed(trait("gilded",2),true)==1, "riptide speed in water or rain only");
        // cap, stars: the +12% cap comes from stars alone (in-match progression design, sections 8 and 10): every rarity's star 3 is at most capPct, Common and Rare +6% / +12%
        for (var r : Spec.RARITIES) check(r.starPct2() <= r.starPct3() && r.starPct3() <= Spec.RECRUITMENT[0].capPct()+1e-9, "cap from stars alone: rarities." + r.id() + ".starPct3 " + r.starPct3() + " is at most recruitment.capPct");
        for (String r : List.of("common", "rare")) check(eq(rarity(r).starPct2(),0.06) && eq(rarity(r).starPct3(),0.12) && eq(rarity(r).starPct3(),Spec.RECRUITMENT[0].capPct()), "cap from stars alone: a " + r + " is +6% at star 2 and +12% at star 3, the whole cap");
        check(progressionMultiplier(.06,.06,.12)==1.12 && progressionMultiplier(.12,.06,.12)==1.12 && eq(progressionMultiplier(.03,0,.12),1.03), "cap clamps");
        check(starFromCopies(1,C)==1 && starFromCopies(2,C)==2 && starFromCopies(3,C)==2 && starFromCopies(4,C)==3, "4 copies for star 3");
        check(starFromCopies(9,rarity("legendary"))==1 && starPct(1,C)==0 && eq(starPct(2,C),0.06) && eq(starPct(3,C),0.12) && eq(starPct(3,rarity("epic")),0.12), "star bonuses; Legendary stars come from shards only");
        check(Arrays.stream(Spec.MASTERY).filter(m -> m.level() == 1).map(Spec.MasteryRow::reward).toList().equals(List.of("title:Veteran")), "Mastery I gives the title only: it unlocks nothing");
        founders();
        pacts();
        // the battle report's House rows (Task 11): every House the banner has slots in, House order, with its banner tier, trait seconds, shards and Renown (0 when absent)
        var houseRows = MatchStats.houseRows(HOUSES, houseSlots(SPEC_EXAMPLE_BANNER,cat), Map.of("gilded",3), Map.of("gilded",40,"graveborn",12,"wildmoot",9), Map.of("gilded",2), Map.of("gilded",2,"graveborn",1));
        check(houseRows.equals(List.of(new MatchStats.HouseRow("gilded",3,40,2,2), new MatchStats.HouseRow("graveborn",0,12,0,1))), "report House rows: one per House with banner slots, a House at tier 0 included, none for a House with no slots: " + houseRows);
        check(addCopyAt(4,"common")==2 && addCopyAt(4,"rare")==4 && addCopyAt(4,"epic")==8 && addCopyAt(1,"common")==0, "dismantle past star 3");
        check(starsAfterCopy(1,1,"common")==2 && starsAfterCopy(3,2,"common")==3, "addCopy raises stars: the 2nd copy gives star 2 and the 4th star 3");
        // rolls (shares() runs 200,000 rolls with new SplittableRandom(7) and epicPity = Integer.MAX_VALUE)
        var sh=shares(200_000, new SplittableRandom(7), Integer.MAX_VALUE);
        check(Math.abs(sh.common()-.86)<=.005 && Math.abs(sh.rare()-.12)<=.004 && Math.abs(sh.epic()-.02)<=.002, "weights 43/6/2");
        check(firstEpicWithZeroRng()==40 && pityAfterEpic()==0, "pity: 40th roll gives the Epic, then resets");
        check(rolledEpicPity()==0, "pity: a rolled Epic resets the counter too, not only the forced 40th roll");
        check(noLegendaryIn(10_000), "Legendaries never roll");
        // banners
        check(validBanner(SPEC_EXAMPLE_BANNER,cat,ALL)==null && "missing_marker".equals(validBanner(V1_DAWN_DEFAULT,cat,ALL)), "validation");
        check("size".equals(validBanner(banner("knight*19"),cat,ALL)) && "unknown".equals(validBanner(banner("oracle,knight*19"),cat,ALL)) && "not_owned".equals(validBanner(starterBanner(),cat,Set.of("knight")))
            && "orphan_marker".equals(validBanner(banner("knight,^,knight*18"),cat,ALL)) && "legendary_limit".equals(validBanner(TWO_LEGENDARIES,cat,ALL)) && "epic_limit".equals(validBanner(THREE_EPICS,cat,ALL)), "each validation failure is named");
        check("missing_marker".equals(validBanner(banner("knight*19,mage"),cat,ALL)), "an Epic in the last slot has no room for its ^: missing_marker");
        check(repack(V1_DAWN_DEFAULT,cat,ALL).equals(EXPECTED_DAWN_REPACK), "v1 Dawn line repacks to knight x12, archer x6, mage, ^");
        check(legendaries(repack(TWO_LEGENDARIES,cat,ALL))==1 && epics(repack(THREE_EPICS,cat,ALL))==2 && repack(THREE_EPICS,cat,ALL).size()==20, "limits and size");
        check(repack(banner("knight*12,mage,^,oracle,archer*8"),cat,Set.of("knight","archer")).equals(banner("knight*12,archer*8")) && starterBanner().equals(banner("knight*12,archer*8")), "repack drops unowned and unknown ids; the starter banner");
        check(repack(banner("knight*17,gl_marshal,archer"),cat,ALL).equals(banner("knight*17,archer,empty*2")), "repack skips a Legendary that does not fit and still places the Common after it");
        for (var b : Spec.BOTBANNERS) check(validBanner(Arrays.asList(b.slots().split(",")),cat,ALL)==null, "bot banner " + b.id() + " is a valid banner");
        // renown, rewards, mastery
        check(renownLevel(2,R)==0 && renownLevel(3,R)==1 && renownLevel(29,R)==4 && renownLevel(30,R)==5, "renown 3/8/15/22/30");
        var w=matchRewards(Map.of("gilded",14,"graveborn",6),T,HOUSES,true); check(w.shards().equals(Map.of("gilded",2,"graveborn",1)) && w.renown().equals(Map.of("gilded",2,"graveborn",2)), "win rewards");
        var l=matchRewards(Map.of("gilded",14,"graveborn",5),T,HOUSES,false); check(l.shards().equals(Map.of("gilded",1)) && l.renown().equals(Map.of("gilded",1)), "loss rewards, tier 0 earns nothing");
        var tie=matchRewards(Map.of("gilded",10,"graveborn",10),T,HOUSES,true); check(tie.shards().equals(Map.of("gilded",2,"graveborn",1)), "a tied largest House is the first in House order");
        var low=matchRewards(Map.of("gilded",5,"graveborn",5,"wildmoot",5,"ironclad",5),T,HOUSES,true); check(low.shards().equals(Map.of("gilded",1)) && low.renown().isEmpty(), "a win gives the largest House its shard even below tier I (a tie goes to the first in House order); Renown still needs tier I");
        check(masteryLevel(39,8,M)==0 && masteryLevel(40,8,M)==1 && masteryLevel(150,24,M)==1 && masteryLevel(400,60,M)==3, "mastery needs both");
        // the troops sheet (Task 2): the catalog, units and what a banner can field
        check(Catalog.fromSpec().equals(cat), "Catalog.fromSpec() equals the fixture catalog: House, rarity and slot weight of all 30 troops");
        check(units(SPEC_EXAMPLE_BANNER,cat)==15 && units(starterBanner(),cat)==20 && units(banner("mage,^,empty*18"),cat)==1 && units(null,cat)==0, "units: an Epic or a Legendary counts once, markers and empty slots not at all");
        check(fieldableHouses().equals(Set.of("gilded","graveborn","wildmoot","ironclad","tidecallers")), "Gilded, Graveborn, Wildmoot, Ironclad and Tidecallers are the fieldable Houses (every House since Task 10)");
        var wild=banner("wm_seer,^,knight*10,wm_hound*4,wm_oldhorn,^,^,^");var without=Set.of("gilded","graveborn","ironclad","tidecallers"); // a fieldable set without Wildmoot, as the sheet stood before Task 10
        check(fieldable(wild,cat,without).equals(banner("empty*2,knight*10,empty*8")) && validBanner(fieldable(wild,cat,without),cat,ALL)==null && wild.get(0).equals("wm_seer") && fieldable(wild,cat,fieldableHouses()).equals(wild), "fieldable: an unfieldable unit and its markers become empty, the rest keep their slots, and the input is unchanged; with its House fieldable the banner is unchanged");
        check(fieldable(SPEC_EXAMPLE_BANNER,cat,fieldableHouses()).equals(SPEC_EXAMPLE_BANNER), "fieldable: a banner of fieldable Houses is unchanged");
        // the roll House, House names and the launch roster (moved from Campaign to RosterMath in fix round 1, so the camp screens read pure helpers)
        check(houseName("gilded").equals("Gilded Order") && houseName("graveborn").equals("Graveborn") && houseName("nowhere").equals("nowhere"), "houseName: the House's sheet name, or the id when no House has it");
        check(rollHouse(starterBanner()).equals("gilded") && rollHouse(banner("revenant*3,knight*2,empty*15")).equals("graveborn") && rollHouse(banner("knight*2,revenant*2,empty*16")).equals("gilded")
                && rollHouse(banner("wm_seer,^,wm_hound*8,empty*10")).equals("wildmoot") && rollHouse(null).equals("gilded"), "rollHouse: the fieldable House with most slots, a tie to House order, else the first fieldable House");
        var roster = launchRoster();
        check(roster.equals(List.of("knight", "archer", "sentinel", "longbow", "mage", "revenant", "bonebow", "reaper", "stalker", "hexer", "wm_hound", "wm_hunter", "wm_stalker", "wm_tusk", "wm_seer", "ic_hearthguard", "ic_arbalist", "ic_breaker", "ic_ballista", "ic_mortar", "tc_raider", "tc_harpooner", "tc_tideguard", "tc_mist", "tc_brute")), "launchRoster: the Commons, Rares and Epic of each fieldable House, no Legendary: " + roster);
        System.out.println("HousesCheck: " + checks + " checks passed");
    }
}
