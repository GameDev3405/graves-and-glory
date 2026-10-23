package gg.gravesandglory;

import java.util.*;

/** Pure in-match progression rules (design sections 3, 4 and 9.1): level thresholds, the bounty, the stat factors, worn tiers, Armoury buy states, prices, refusal copy, bot buy order, and the kill-credit log. Never shipped. */
public final class LevelMathCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static boolean eq(double a, double b) { return Math.abs(a - b) < 1e-9; }

    static Spec.TroopsRow troop(String id) {
        for (var t : Spec.TROOPS) if (t.id().equals(id)) return t;
        throw new AssertionError("no troop row " + id);
    }
    /** The sheet's bot row for a difficulty, with fixed gear columns so the fixtures survive Task 11 tuning. */
    static Spec.BotCommandRow bot(String id, int maxTier, int reserveTroops, String priority) {
        for (var b : Spec.BOTCOMMAND) if (b.id().equals(id))
            return new Spec.BotCommandRow(b.id(), b.retreatShare(), b.rallyShare(), b.chargeRatio(), b.decisionTicks(), b.flankLeastDefended(), b.defenderWeight(), b.shieldwallVsArchers(), b.archerThreatCount(), b.archerThreatRange(), b.chargeDistance(), maxTier, reserveTroops, priority);
        throw new AssertionError("no botCommand row " + id);
    }
    static LevelMath.TypeState type(String troop, int seen, int weapon, int armour, int living2) {
        var t = troop(troop);
        return new LevelMath.TypeState(t.id(), t.group(), t.rarity(), seen, weapon, armour, living2);
    }
    static String picks(List<LevelMath.Option> list) {
        StringBuilder b = new StringBuilder();
        for (var o : list) b.append(b.length() == 0 ? "" : ",").append(o.troop()).append(':').append(o.track()).append(':').append(o.tier());
        return b.toString();
    }
    static int spent(List<LevelMath.Option> list) { int s = 0; for (var o : list) s += o.price(); return s; }
    static void reserveKept(Spec.BotCommandRow row, List<LevelMath.Option> list, int gold, int cheapest, String fixture) {
        check(gold - spent(list) >= row.gearReserveTroops() * cheapest || list.isEmpty(), fixture + ": botPicks spent into the reserve (" + spent(list) + " of " + gold + ")");
        for (var o : list) check(o.tier() <= row.gearMaxTier() && o.tier() >= 2, fixture + ": a pick above gearMaxTier or below tier II: " + picks(list));
    }
    /** A byte buffer that writes and reads as PacketByteBuf does: a boolean is one byte 0 or 1, a byte is one byte, a string is its UTF-8 length as a VarInt, then the bytes. */
    static final class Wire implements LevelMath.Out {
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        Wire() {}
        Wire(byte[] start) { out.writeBytes(start); }
        void raw(byte v) { out.write(v); }
        byte[] bytes() { return out.toByteArray(); }
        public void bool(boolean v) { out.write(v ? 1 : 0); }
        public void b(int v) { out.write(v); }
        public void str(String v) {
            byte[] s = v.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int n = s.length;
            while ((n & ~0x7F) != 0) { out.write((n & 0x7F) | 0x80); n >>>= 7; }
            out.write(n);
            out.writeBytes(s);
        }
        LevelMath.In reader() {
            byte[] data = bytes();
            return new LevelMath.In() {
                int at;
                public boolean bool() { return data[at++] != 0; }
                public int b() { return data[at++]; }
                public String str() {
                    int n = 0, shift = 0, v;
                    do { v = data[at++]; n |= (v & 0x7F) << shift; shift += 7; } while ((v & 0x80) != 0);
                    String s = new String(data, at, n, java.nio.charset.StandardCharsets.UTF_8);
                    at += n;
                    return s;
                }
            };
        }
    }
    static boolean clean(String copy) {
        String c = copy.toLowerCase(Locale.ROOT);
        return !c.contains("stage") && !c.contains("evolve") && !c.contains("writ");
    }

    public static void main(String[] args) {
        // lm-thresholds: level 2 at 3 credits, level 3 at 8 (design 3.1)
        check(LevelMath.levelFor(0) == 1 && LevelMath.levelFor(2) == 1 && LevelMath.levelFor(3) == 2 && LevelMath.levelFor(7) == 2 && LevelMath.levelFor(8) == 3 && LevelMath.levelFor(99) == 3, "lm-thresholds: 0->1, 2->1, 3->2, 7->2, 8->3, 99->3");
        check(LevelMath.levelFor(-1) == 1, "lm-thresholds: no credits below zero lower the level under 1");

        // lm-carry: 9 credits at once take a fresh soldier to level 3, two level ups
        check(LevelMath.levelFor(0 + 9) == 3 && LevelMath.levelFor(0 + 9) - LevelMath.levelFor(0) == 2, "lm-carry: 0 plus 9 credits gives level 3 and two level ups");
        check(LevelMath.levelFor(2 + 1) - LevelMath.levelFor(2) == 1 && LevelMath.levelFor(8 + 3) - LevelMath.levelFor(8) == 0, "lm-carry: one level up at the threshold, none past level 3");

        // lm-bounty: a victim counts its own level; a commander counts commanderBounty; a temporary troop counts 1 (design 3.2)
        check(LevelMath.bounty(1, false, false) == 1 && LevelMath.bounty(2, false, false) == 2 && LevelMath.bounty(3, false, false) == 3, "lm-bounty: level 1 gives 1, level 2 gives 2, level 3 gives 3");
        check(LevelMath.bounty(1, true, false) == 3 && LevelMath.bounty(1, true, false) == Spec.PROGRESSION[0].commanderBounty(), "lm-bounty: a commander gives progression.commanderBounty (3)");
        check(LevelMath.bounty(3, false, true) == 1 && LevelMath.bounty(1, false, true) == 1, "lm-bounty: a temporary troop gives 1, whatever level it shows");
        check(LevelMath.bounty(0, false, false) == 1, "lm-bounty: a victim never counts less than 1");

        // lm-factor: one multiplier per stat, level plus tier (design 4.1)
        check(eq(LevelMath.healthFactor(3, 3), 1.30) && eq(LevelMath.damageFactor(3, 3), 1.30), "lm-factor: level 3 with tier III is 1.30 per stat");
        check(eq(LevelMath.healthFactor(3, 3) * LevelMath.damageFactor(3, 3), 1.69), "lm-factor: level 3 with tier III is 1.69 in product");
        check(eq(LevelMath.healthFactor(1, 1), 1.0) && eq(LevelMath.damageFactor(1, 1), 1.0), "lm-factor: a fresh soldier is 1.0");
        check(eq(LevelMath.healthFactor(2, 2), 1.15) && eq(LevelMath.damageFactor(2, 1), 1.05) && eq(LevelMath.damageFactor(1, 3), 1.20) && eq(LevelMath.healthFactor(3, 1), 1.10), "lm-factor: the level and tier parts add (2+II 1.15, 2+I 1.05, 1+III 1.20, 3+I 1.10)");

        // lm-worn: min(level, bought), never under I (design 4.2)
        check(LevelMath.worn(3, 2) == 2, "lm-worn: a level-3 soldier with tier II bought wears II");
        check(LevelMath.worn(1, 3) == 1, "lm-worn: a level-1 soldier with III bought wears I");
        check(LevelMath.worn(2, 1) == 1 && LevelMath.worn(3, 3) == 3 && LevelMath.worn(2, 3) == 2 && LevelMath.worn(0, 0) == 1, "lm-worn: level 2 with nothing bought wears I; 3 and III wear III; 2 and III wear II; never under I");

        // lm-buy-states: the four states (design 4.2, 5.1)
        var L = LevelMath.Buy.LOCKED; var O = LevelMath.Buy.OPEN; var B = LevelMath.Buy.BOUGHT; var U = LevelMath.Buy.UNAFFORDABLE;
        check(LevelMath.buyState(2, 1, 1, 1000, 40) == L && LevelMath.buyState(2, 1, 1, 0, 40) == L, "lm-buy-states: tier II is LOCKED before a level-2 soldier is seen, rich or poor");
        check(LevelMath.buyState(2, 2, 1, 1000, 40) == O && LevelMath.buyState(2, 2, 1, 40, 40) == O, "lm-buy-states: tier II is OPEN once a level-2 soldier is seen and the gold covers it");
        check(LevelMath.buyState(2, 2, 1, 39, 40) == U, "lm-buy-states: tier II is UNAFFORDABLE one gold short");
        check(LevelMath.buyState(2, 2, 2, 1000, 40) == B && LevelMath.buyState(2, 3, 3, 0, 40) == B && LevelMath.buyState(3, 3, 3, 0, 80) == B, "lm-buy-states: a bought tier is BOUGHT, gold or not");
        check(LevelMath.buyState(3, 3, 1, 1000, 80) == L, "lm-buy-states: tier III is LOCKED without tier II, even at level 3");
        check(LevelMath.buyState(3, 2, 2, 1000, 80) == L && LevelMath.buyState(3, 3, 2, 1000, 80) == O && LevelMath.buyState(3, 3, 2, 79, 80) == U, "lm-buy-states: tier III needs a seen level 3 and tier II; then OPEN, or UNAFFORDABLE short");
        int seen = 2; // the level-2 soldier who opened the buy died; the highest level seen stays 2 for the round
        check(LevelMath.buyState(2, seen, 1, 1000, 40) == O, "lm-buy-states: an opened buy stays open when the highest level seen stays at 2");

        // lm-prices: design 4.3
        String[] rar = {"common", "rare", "epic", "legendary"};
        int[][] table = {{40, 80}, {60, 120}, {80, 160}, {100, 200}};
        for (int i = 0; i < rar.length; i++) {
            check(LevelMath.price(rar[i], 2) == table[i][0] && LevelMath.price(rar[i], 3) == table[i][1], "lm-prices: " + rar[i] + " is " + table[i][0] + "/" + table[i][1]);
            check(2 * (LevelMath.price(rar[i], 2) + LevelMath.price(rar[i], 3)) == 6 * table[i][0], "lm-prices: " + rar[i] + " both tracks, both tiers is " + 6 * table[i][0]);
        }
        var gn = Arrays.stream(Spec.BOTBANNERS).filter(b -> b.id().equals("gilded_normal")).findFirst().orElseThrow();
        Set<String> kinds = new LinkedHashSet<>(Arrays.asList(gn.slots().split(","))); kinds.remove("^");
        int full = 0; for (String k : kinds) full += 2 * (LevelMath.price(troop(k).rarity(), 2) + LevelMath.price(troop(k).rarity(), 3));
        check(kinds.size() == 5 && full == 1680, "lm-prices: gilded_normal's full gear cost is 1,680 over five types, not " + full + " over " + kinds);
        boolean threw = false; try { LevelMath.price("mythic", 2); } catch (IllegalArgumentException e) { threw = true; }
        check(threw, "lm-prices: an unknown rarity is refused, not priced 0");

        // lm-tier-cap (Task 10, Task 5 re-review note 4): the rarities sheet prices tiers II and III only, so a gearTiers sheet with a fourth row must not make the buy states, the refusals or the bots reach price() above III
        var sheetTiers = Spec.GEARTIERS;
        try {
            Spec.GEARTIERS = new Spec.GearTiersRow[] {sheetTiers[0], sheetTiers[1], sheetTiers[2], new Spec.GearTiersRow("t4", 4, 0.3, 0.3)};
            check(LevelMath.maxTier() == 3, "lm-tier-cap: with a fourth gearTiers row the highest tier is still the highest priced one, III, not " + LevelMath.maxTier());
            check(LevelMath.buyState(4, 4, 3, 1000, 0) == LevelMath.Buy.LOCKED, "lm-tier-cap: tier IV is LOCKED however rich the army and high the soldier");
            String why = LevelMath.refusal(4, 4, 3, 1000, 0, true, "Knight");
            check(why.startsWith("Needs a level-4 "), "lm-tier-cap: tier IV is refused, not bought: " + why);
            List<LevelMath.Option> capped = null; String thrown = "";
            try { capped = LevelMath.botPicks(bot("hard", 4, 0, "open"), List.of(type("knight", 3, 3, 3, 4), type("archer", 3, 1, 1, 1)), 100000, 22); } catch (RuntimeException e) { thrown = e.toString(); }
            check(thrown.isEmpty() && picks(capped).equals("archer:weapon:2,archer:weapon:3,archer:armour:2,archer:armour:3"), "lm-tier-cap: a bot whose row allows tier IV buys up to III and never throws (the knight is done, the archer buys II and III on both tracks): " + (thrown.isEmpty() ? picks(capped) : "THREW " + thrown));
        } finally { Spec.GEARTIERS = sheetTiers; }
        check(LevelMath.maxTier() == 3 && Spec.GEARTIERS.length == 3, "lm-tier-cap: the sheet's own three tiers are back");

        // lm-refusal: the Armoury's player copy (design 5.1)
        check(LevelMath.refusal(2, 2, 1, 100, 40, true).isEmpty() && LevelMath.refusal(3, 3, 2, 80, 80, true, "armour", "Knight-Captain").isEmpty(), "lm-refusal: an allowed buy gives no copy");
        check(LevelMath.refusal(2, 1, 1, 100, 40, true, "Knight").equals("Needs a level-2 Knight"), "lm-refusal: a locked tier II names the level-2 name: " + LevelMath.refusal(2, 1, 1, 100, 40, true, "Knight"));
        check(LevelMath.refusal(3, 2, 2, 100, 80, true, "Knight-Captain").equals("Needs a level-3 Knight-Captain"), "lm-refusal: a locked tier III names the level-3 name");
        check(LevelMath.refusal(3, 3, 1, 100, 80, true).equals("Buy Weapon II first") && LevelMath.refusal(3, 3, 1, 100, 80, true, "armour", "Knight-Captain").equals("Buy Armour II first"), "lm-refusal: tier III before tier II names the track's tier II");
        check(LevelMath.refusal(2, 2, 2, 100, 40, true).equals("Already bought") && LevelMath.refusal(3, 3, 3, 0, 80, false).equals("Already bought"), "lm-refusal: a bought tier says Already bought");
        check(LevelMath.refusal(2, 2, 1, 39, 40, true).equals("Not enough gold"), "lm-refusal: one gold short says Not enough gold");
        check(LevelMath.refusal(2, 2, 1, 100, 40, false).equals("Buy at your barracks or a held point") && LevelMath.refusal(2, 2, 1, 0, 40, false).equals("Buy at your barracks or a held point"), "lm-refusal: an open buy away from the barracks names the place, rich or poor");
        check(LevelMath.refusal(2, 1, 1, 100, 40, false, "Knight").equals("Needs a level-2 Knight"), "lm-refusal: a locked buy names the level first, even away from the barracks");
        check(LevelMath.refusal(2, 1, 1, 100, 40, true).equals("Needs a level-2 soldier"), "lm-refusal: with no level name the copy says soldier");
        String[] copies = {LevelMath.refusal(2, 1, 1, 100, 40, true, "Knight"), LevelMath.refusal(2, 1, 1, 100, 40, true), LevelMath.refusal(3, 3, 1, 100, 80, true), LevelMath.refusal(3, 3, 1, 100, 80, true, "armour", "x"), LevelMath.refusal(2, 2, 2, 100, 40, true), LevelMath.refusal(2, 2, 1, 39, 40, true), LevelMath.refusal(2, 2, 1, 100, 40, false)};
        for (String c : copies) check(clean(c), "lm-refusal: player copy says stage, evolve or Writ: " + c);

        // level names (design 3.3): Commons and Rares take their line, Epics and Legendaries keep their name
        check(LevelMath.levelName(troop("knight"), 1).equals("Order Squire") && LevelMath.levelName(troop("knight"), 2).equals("Knight") && LevelMath.levelName(troop("knight"), 3).equals("Knight-Captain"), "lm-level-names: Order Squire, Knight, Knight-Captain");
        check(LevelMath.levelName(troop("mage"), 3).equals("Sunweaver") && LevelMath.levelName(troop("gl_marshal"), 2).equals(troop("gl_marshal").name()), "lm-level-names: an Epic and a Legendary keep their own name");

        // lm-bot-easy: banner order, weapon then armour, tier II only, 8 troops kept
        var easy = bot("easy", 2, 8, "banner"); int cheap = 22, er = 8 * cheap;
        List<LevelMath.TypeState> et = List.of(type("knight", 3, 1, 1, 2), type("archer", 2, 1, 1, 0), type("mage", 1, 1, 1, 0));
        var e1 = LevelMath.botPicks(easy, et, er + 160, cheap);
        check(picks(e1).equals("knight:weapon:2,knight:armour:2,archer:weapon:2,archer:armour:2"), "lm-bot-easy: banner order, weapon first, no tier III at level 3: " + picks(e1));
        reserveKept(easy, e1, er + 160, cheap, "lm-bot-easy");
        var e2 = LevelMath.botPicks(easy, et, er + 159, cheap);
        check(picks(e2).equals("knight:weapon:2,knight:armour:2,archer:weapon:2"), "lm-bot-easy: one gold short of the last buy skips it: " + picks(e2));
        reserveKept(easy, e2, er + 159, cheap, "lm-bot-easy");
        check(LevelMath.botPicks(easy, et, er + 39, cheap).isEmpty() && LevelMath.botPicks(easy, et, er - 1, cheap).isEmpty(), "lm-bot-easy: nothing is bought into the reserve");

        // lm-bot-normal: banner order; ranged, siege and support weapon first, infantry and cavalry armour first; up to III; 5 troops kept
        var normal = bot("normal", 3, 5, "role"); int nr = 5 * cheap;
        List<LevelMath.TypeState> nt = List.of(type("knight", 3, 1, 1, 1), type("archer", 2, 1, 1, 0), type("wm_hound", 2, 1, 1, 0), type("mage", 2, 1, 1, 0), type("ic_ballista", 2, 1, 1, 0), type("sentinel", 1, 1, 1, 0));
        var n1 = LevelMath.botPicks(normal, nt, 100000, cheap);
        check(picks(n1).equals("knight:armour:2,knight:armour:3,knight:weapon:2,knight:weapon:3,archer:weapon:2,archer:armour:2,wm_hound:armour:2,wm_hound:weapon:2,mage:weapon:2,mage:armour:2,ic_ballista:weapon:2,ic_ballista:armour:2"), "lm-bot-normal: role order per type, both tracks to III where open: " + picks(n1));
        reserveKept(normal, n1, 100000, cheap, "lm-bot-normal");
        var n2 = LevelMath.botPicks(normal, nt, nr + 40 + 80 + 40, cheap);
        check(picks(n2).equals("knight:armour:2,knight:armour:3,knight:weapon:2"), "lm-bot-normal: the gold runs out at the reserve: " + picks(n2));
        reserveKept(normal, n2, nr + 160, cheap, "lm-bot-normal");
        var n3 = LevelMath.botPicks(normal, List.of(type("tc_harpooner", 3, 3, 1, 2), type("tc_raider", 3, 2, 3, 1)), 100000, cheap);
        check(picks(n3).equals("tc_harpooner:armour:2,tc_harpooner:armour:3,tc_raider:weapon:3"), "lm-bot-normal: bought tiers are skipped, tier III follows the bought II: " + picks(n3));

        // lm-bot-hard: every open buy, most level-2+ soldiers first (ties in banner order), up to III, 2 troops kept
        var hard = bot("hard", 3, 2, "open"); int hr = 2 * cheap;
        List<LevelMath.TypeState> ht = List.of(type("knight", 2, 1, 1, 1), type("archer", 3, 2, 1, 4), type("sentinel", 1, 1, 1, 0), type("longbow", 2, 1, 1, 4));
        int hardAll = 80 + 40 + 80 + 60 + 60 + 40 + 40;
        var h1 = LevelMath.botPicks(hard, ht, hr + hardAll, cheap);
        check(picks(h1).equals("archer:weapon:3,archer:armour:2,archer:armour:3,longbow:weapon:2,longbow:armour:2,knight:weapon:2,knight:armour:2"), "lm-bot-hard: most veterans first, ties in banner order: " + picks(h1));
        reserveKept(hard, h1, hr + hardAll, cheap, "lm-bot-hard");
        var h2 = LevelMath.botPicks(hard, ht, hr + hardAll - 1, cheap);
        check(picks(h2).equals("archer:weapon:3,archer:armour:2,archer:armour:3,longbow:weapon:2,longbow:armour:2,knight:weapon:2"), "lm-bot-hard: one gold short of the last buy keeps the reserve: " + picks(h2));
        reserveKept(hard, h2, hr + hardAll - 1, cheap, "lm-bot-hard");
        var h3 = LevelMath.botPicks(bot("hard", 2, 2, "open"), ht, 100000, cheap);
        check(picks(h3).equals("archer:armour:2,longbow:weapon:2,longbow:armour:2,knight:weapon:2,knight:armour:2"), "lm-bot-hard: gearMaxTier caps the tier: " + picks(h3));
        for (var o : h1) check(o.price() == LevelMath.price(troop(o.troop()).rarity(), o.tier()), "lm-bot-hard: a pick carries its price");

        // lm-credit-*: the 10-second assist and support logs (design 3.2); ids are entity ids, times battle ticks
        long W = Spec.PROGRESSION[0].assistSeconds() * 20L, T = 1000; int V = 100;
        var log = new CreditLog();
        check(log.credited(V, 1, 0, T, W).equals(Set.of(1)), "lm-credit-killer: with an empty log only the killer is credited");
        check(log.credited(V, -1, 0, T, W).isEmpty(), "lm-credit-killer: a killing blow that was not a troop's credits no killer");
        log.hit(V, 2, 0, T - 150); log.hit(V, 3, 0, T - W - 1); log.hit(V, 4, 0, T - W);
        Set<Integer> c = log.credited(V, 1, 0, T, W);
        check(c.contains(2) && c.contains(4) && !c.contains(3), "lm-credit-window: a helper inside 10 s (and on its last tick) is credited, one outside is not: " + c);
        log.hit(V, 2, 0, T - 400); // an older blow by the same helper does not push his entry out of the window
        check(log.credited(V, 1, 0, T, W).contains(2), "lm-credit-window: a helper's latest blow counts");
        log.support(2, 5, 0, T - 100); log.support(1, 6, 0, T - 50); log.support(77, 7, 0, T - 50); log.support(2, 8, 0, T - W - 1);
        c = log.credited(V, 1, 0, T, W);
        check(c.contains(5), "lm-credit-support: a supporter of a helper is credited");
        check(c.contains(6), "lm-credit-support: a supporter of the killer is credited");
        check(!c.contains(7), "lm-credit-support: a supporter of nobody in the fight is not credited");
        check(!c.contains(8), "lm-credit-support: a support older than 10 s is not credited");
        log.hit(V, 9, 0, T - 10);
        check(log.credited(V, 1, 0, T, W).contains(9), "lm-credit-allied: an allied army's helper (same team) is credited");
        log.hit(V, 10, 1, T - 10); log.support(2, 11, 1, T - 10); log.support(10, 12, 1, T - 10);
        c = log.credited(V, 1, 0, T, W);
        check(!c.contains(10) && !c.contains(11) && !c.contains(12), "lm-credit-enemy: an enemy-team helper, an enemy supporter of a helper and a supporter of the enemy helper are not credited: " + c);
        check(!c.contains(V), "lm-credit-enemy: the victim is never credited");
        log.hit(200, 13, 0, T - 10);
        check(!log.credited(V, 1, 0, T, W).contains(13), "lm-credit-victim: a blow on another victim does not credit this kill");
        check(log.credited(V, 1, 0, T, W).equals(Set.of(1, 2, 4, 5, 6, 9)), "lm-credit-set: the whole credited set is killer, helpers and their supporters: " + log.credited(V, 1, 0, T, W));
        log.forget(2);
        c = log.credited(V, 1, 0, T, W);
        check(!c.contains(2) && c.contains(4) && c.contains(9), "lm-credit-forget: a dead helper is not credited, the living helpers still are: " + c);
        check(c.contains(5), "lm-credit-forget: whoever healed the dead helper inside the window took part and is still credited");
        log.forget(1);
        c = log.credited(V, 1, 0, T, W);
        check(!c.contains(1) && c.contains(6) && c.contains(4), "lm-credit-forget: a shot landing after its archer died credits his helpers and his healer, not him: " + c);
        log.hit(V, 14, 0, T - 500);
        check(log.credited(V, 20, 0, T, 100000).contains(14), "lm-credit-trim: before the trim an old entry is still held");
        log.trim(T, W);
        c = log.credited(V, 20, 0, T, 100000);
        check(!c.contains(14) && !c.contains(3) && c.contains(4) && c.contains(9) && c.contains(20), "lm-credit-trim: trim drops entries older than the window and keeps the rest: " + c);
        log.hit(300, 15, 0, T); log.forget(300);
        check(log.credited(300, 21, 0, T, W).equals(Set.of(21)), "lm-credit-forget: a dead victim's own log is dropped");
        log.clear();
        check(log.credited(V, 22, 0, T, W).equals(Set.of(22)), "lm-credit-clear: clear empties every log (a round boundary)");
        log.forget(23); log.trim(T + 10 * W, W);
        check(log.credited(V, 23, 0, T + 10 * W, W).equals(Set.of(23)), "lm-credit-forget: a forgotten id with no entries left is dropped by trim, so a reused id is credited again");

        // lm-state-roundtrip: the state_v9 Armoury block (armouryHere, byte n, then per type the troop id and four Buy ordinal bytes) through the pure encoder, on PacketByteBuf's own byte rules
        var armoury = new LevelMath.Armoury(true, List.of(
            new LevelMath.ArmouryRow("knight", LevelMath.Buy.OPEN, LevelMath.Buy.LOCKED, LevelMath.Buy.BOUGHT, LevelMath.Buy.UNAFFORDABLE),
            new LevelMath.ArmouryRow("archer", LevelMath.Buy.LOCKED, LevelMath.Buy.LOCKED, LevelMath.Buy.LOCKED, LevelMath.Buy.LOCKED)));
        var wire = new Wire();
        LevelMath.encodeArmoury(armoury, wire);
        byte[] want = {1, 2, 6, 'k', 'n', 'i', 'g', 'h', 't', 1, 0, 2, 3, 6, 'a', 'r', 'c', 'h', 'e', 'r', 0, 0, 0, 0};
        check(Arrays.equals(wire.bytes(), want), "lm-state-roundtrip: the layout is armouryHere, byte n, then per type the troop id (VarInt length, UTF-8) and weapon2, weapon3, armour2, armour3 as Buy ordinals: " + Arrays.toString(wire.bytes()));
        check(LevelMath.Buy.LOCKED.ordinal() == 0 && LevelMath.Buy.OPEN.ordinal() == 1 && LevelMath.Buy.BOUGHT.ordinal() == 2 && LevelMath.Buy.UNAFFORDABLE.ordinal() == 3, "lm-state-roundtrip: the Buy ordinals on the wire are LOCKED 0, OPEN 1, BOUGHT 2, UNAFFORDABLE 3");
        check(armoury.equals(LevelMath.decodeArmoury(wire.reader())), "lm-state-roundtrip: decode gives back what encode wrote");
        var empty = new Wire();
        LevelMath.encodeArmoury(LevelMath.Armoury.EMPTY, empty);
        check(Arrays.equals(empty.bytes(), new byte[] {0, 0}) && LevelMath.Armoury.EMPTY.equals(LevelMath.decodeArmoury(empty.reader())), "lm-state-roundtrip: the empty Armoury (before Task 5) is two zero bytes and reads back empty");
        check(LevelMath.decodeArmoury(new Wire(new byte[] {0, (byte) (Spec.TROOPS.length + 1)}).reader()) == null, "lm-state-roundtrip: a count above the troop sheet is refused");
        check(LevelMath.decodeArmoury(new Wire(new byte[] {0, (byte) -1}).reader()) == null, "lm-state-roundtrip: a negative count is refused");
        check(LevelMath.decodeArmoury(new Wire(new byte[] {1, 1, 1, 'x', 0, 4, 0, 0}).reader()) == null, "lm-state-roundtrip: a buy byte outside the Buy ordinals is refused");
        var three = new Wire();
        LevelMath.encodeArmoury(armoury, three);
        three.raw((byte) 9);
        var reader = three.reader();
        check(armoury.equals(LevelMath.decodeArmoury(reader)) && reader.b() == 9, "lm-state-roundtrip: decode reads exactly the block, leaving the next field in place");

        System.out.println("LevelMathCheck: " + checks + " checks passed");
    }
}
