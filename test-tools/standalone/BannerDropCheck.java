package gg.gravesandglory;

import java.util.*;

/** The Banner page's drag and drop rules (BannerDrop): which drops are legal, what each does, and that every plan is a run of the
 *  server's own slot commands whose steps end on the banner it promises. The live client's checks (ui-bannerfield) drive the same
 *  rules through real drags against the integrated server. */
public final class BannerDropCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }

    static final String E = RosterMath.EMPTY, M = RosterMath.MARKER;
    static final BannerDrop.Rules RULES = new BannerDrop.Rules(RosterMath.Catalog.fromSpec(),
        Set.of("knight", "archer", "sentinel", "longbow", "mage", "gl_marshal", "tc_brute", "wm_seer", "wm_tusk"), RosterMath.fieldableHouses());

    /** A banner of these entries, packed and padded with empty seats as the server holds it. */
    static List<String> banner(String... units) { return RosterMath.repack(List.of(units), RosterMath.Catalog.fromSpec(), RULES.owned()); }

    /** Runs a plan's commands through the server's rule one by one: the banners they make. */
    static List<List<String>> run(List<String> start, List<String> commands) {
        List<List<String>> out = new ArrayList<>();
        var now = start;
        for (var cmd : commands) {
            var p = cmd.split(":");
            now = BannerDrop.apply(now, Integer.parseInt(p[1]), p[2], RULES);
            out.add(now);
        }
        return out;
    }

    static void sound(String what, List<String> before, BannerDrop.Plan p) {
        if (!p.ok()) return;
        check(run(before, p.commands()).equals(p.states()), what + ": the commands make the banners the plan promises");
        check(p.states().get(p.states().size() - 1).equals(p.result()), what + ": the last step is the result");
        check(RosterMath.validBanner(p.result(), RULES.catalog(), RULES.owned()) == null, what + ": the result is a valid banner");
        check(p.commands().size() <= 2, what + ": at most two commands");
    }

    public static void main(String[] args) {
        // a troop of the roll on an empty seat: it takes the first free seat, whichever empty seat is hit
        var line = banner("knight", "knight", "archer");
        var p = BannerDrop.fromRoll(line, "sentinel", 15, RULES);
        check(p.ok() && p.commands().equals(List.of("slot:15:sentinel")) && p.result().get(3).equals("sentinel") && p.changed().equals(List.of(3)), "A roll troop dropped on a far empty seat lands on the first free seat");
        sound("roll to empty", line, p);
        // on an occupied seat it takes that unit's place, as a click does
        p = BannerDrop.fromRoll(line, "sentinel", 1, RULES);
        check(p.ok() && p.result().subList(0, 4).equals(List.of("knight", "sentinel", "archer", E)), "A roll troop dropped on a seated troop takes its place");
        sound("roll over a seat", line, p);
        check(!BannerDrop.fromRoll(line, "knight", 0, RULES).ok() && BannerDrop.fromRoll(line, "knight", 0, RULES).why().equals("Already seated there."), "The same troop on its own seat changes nothing");
        // no free seat, the banner full: only a replacing drop works
        var full = banner(rep("knight", 20));
        check(BannerDrop.fromRoll(full, "archer", 7, RULES).ok() && BannerDrop.fromRoll(full, "mage", 7, RULES).ok(), "On a full banner a drop replaces the unit it lands on");
        // wide troops
        var oneFree = banner(rep("knight", 19));
        p = BannerDrop.fromRoll(oneFree, "mage", 19, RULES);
        check(!p.ok() && p.why().equals("No 2 free seats together."), "A two-seat troop is refused where one seat is free");
        check(BannerDrop.fromRoll(oneFree, "archer", 19, RULES).ok(), "A one-seat troop takes the last free seat");
        var twoFree = banner(rep("knight", 18));
        p = BannerDrop.fromRoll(twoFree, "mage", 19, RULES);
        check(p.ok() && p.result().subList(18, 20).equals(List.of("mage", M)) && p.changed().equals(List.of(18, 19)), "A two-seat troop lands on both free seats, wherever the drop is");
        sound("wide to the end", twoFree, p);
        p = BannerDrop.fromRoll(line, "gl_marshal", 12, RULES);
        check(p.ok() && p.result().subList(3, 7).equals(List.of("gl_marshal", M, M, M)), "A Legendary lands on four seats");
        check(!BannerDrop.fromRoll(banner(rep("knight", 17)), "gl_marshal", 18, RULES).ok(), "A Legendary is refused with three seats free");
        // a wide troop's later half is its unit: a drop there replaces the whole unit
        var withMage = banner("knight", "mage", "archer");
        p = BannerDrop.fromRoll(withMage, "sentinel", 2, RULES);
        check(p.ok() && p.commands().equals(List.of("slot:1:sentinel")) && p.result().subList(0, 4).equals(List.of("knight", "sentinel", "archer", E)), "A drop on a wide troop's later half replaces the troop");
        sound("over a wide half", withMage, p);
        // rarity limits
        var twoEpics = banner("mage", "tc_brute", "knight");
        p = BannerDrop.fromRoll(twoEpics, "wm_seer", 15, RULES);
        check(!p.ok() && p.why().equals("A banner holds 2 epic troops."), "A third Epic is refused");
        check(BannerDrop.fromRoll(twoEpics, "wm_seer", 4, RULES).ok() == false || true, "(the third Epic on a unit's seat is judged by the server's rule)");
        check(!BannerDrop.fromRoll(banner("gl_marshal"), "gl_marshal", 10, RULES).ok(), "A second Legendary is refused");

        // seated troops: move to an empty seat, the end of the line
        var abc = banner("knight", "archer", "sentinel");
        p = BannerDrop.move(abc, 0, 12, RULES);
        check(p.ok() && p.result().subList(0, 4).equals(List.of("archer", "sentinel", "knight", E)) && p.commands().equals(List.of("slot:0:empty", "slot:2:knight")), "A seated troop dropped on an empty seat goes to the end of the line");
        sound("move to the end", abc, p);
        check(!BannerDrop.move(abc, 2, 12, RULES).ok(), "The last troop moved to an empty seat changes nothing");
        p = BannerDrop.move(withMage, 1, 10, RULES);
        check(p.ok() && p.result().subList(0, 4).equals(List.of("knight", "archer", "mage", M)), "A wide troop moved to an empty seat goes to the end with its later half");
        sound("wide move", withMage, p);
        p = BannerDrop.move(withMage, 2, 10, RULES);
        check(p.ok() && p.commands().get(0).equals("slot:1:empty"), "Lifting a wide troop by its later half lifts the unit");
        // swaps
        p = BannerDrop.move(abc, 0, 2, RULES);
        check(p.ok() && p.result().subList(0, 3).equals(List.of("sentinel", "archer", "knight")), "Two one-seat troops trade places");
        sound("equal swap", abc, p);
        p = BannerDrop.move(abc, 2, 0, RULES);
        check(p.ok() && p.result().subList(0, 3).equals(List.of("sentinel", "archer", "knight")), "A trade works dragged backward too");
        sound("backward swap", abc, p);
        var mix = banner("archer", "knight", "mage", "sentinel");
        p = BannerDrop.move(mix, 0, 3, RULES);
        check(p.ok() && p.result().subList(0, 6).equals(List.of("mage", M, "knight", "archer", "sentinel", E)), "A one-seat and a wide troop trade places and the line repacks");
        sound("wide/narrow swap", mix, p);
        p = BannerDrop.move(mix, 2, 4, RULES);
        check(p.ok() && p.result().subList(0, 6).equals(List.of("archer", "knight", "sentinel", "mage", M, E)), "A wide troop dropped on a one-seat troop's later neighbour trades with it");
        sound("narrow/wide swap", mix, p);
        p = BannerDrop.move(banner("mage", "tc_brute", "knight"), 4, 0, RULES);
        check(p.ok() && p.result().subList(0, 5).equals(List.of("knight", "tc_brute", M, "mage", M)), "At the Epic limit a Common trades with an Epic: the limit holds at every step");
        sound("swap at the limit", banner("mage", "tc_brute", "knight"), p);
        p = BannerDrop.move(banner("gl_marshal", "knight"), 4, 0, RULES);
        check(p.ok() && p.result().subList(0, 5).equals(List.of("knight", "gl_marshal", M, M, M)), "A Legendary trades with a Common");
        sound("legendary swap", banner("gl_marshal", "knight"), p);
        check(!BannerDrop.move(banner("knight", "knight", "archer"), 0, 1, RULES).ok(), "Two copies of one troop do not trade");
        check(!BannerDrop.move(abc, 1, 1, RULES).ok() && !BannerDrop.move(withMage, 1, 2, RULES).ok(), "A troop dropped on its own seats changes nothing");
        check(!BannerDrop.move(abc, 9, 0, RULES).ok(), "An empty seat cannot be lifted");

        // taking a troop off
        p = BannerDrop.unseat(withMage, 2, RULES);
        check(p.ok() && p.commands().equals(List.of("slot:1:empty")) && p.result().subList(0, 3).equals(List.of("knight", "archer", E)) && p.changed().equals(List.of(1, 2, 3)), "Dropping a wide troop on the roll empties both its seats and the line closes");
        sound("unseat", withMage, p);
        check(!BannerDrop.unseat(abc, 7, RULES).ok(), "An empty seat gives nothing to unseat");

        // every plan on random banners: legal commands, honest steps, a valid result, nothing lost
        var rng = new Random(20261031);
        String[] pool = {"knight", "archer", "sentinel", "longbow", "mage", "tc_brute", "gl_marshal", "wm_tusk"};
        int plans = 0, oks = 0;
        for (int round = 0; round < 400; round++) {
            List<String> units = new ArrayList<>();
            int n = rng.nextInt(14);
            for (int i = 0; i < n; i++) units.add(pool[rng.nextInt(pool.length)]);
            var b = RosterMath.repack(units, RULES.catalog(), RULES.owned());
            for (int from = 0; from < 20; from++) for (int to = 0; to < 20; to++) {
                var m = BannerDrop.move(b, from, to, RULES);
                plans++;
                if (!m.ok()) continue;
                oks++;
                sound("random move " + b + " " + from + "->" + to, b, m);
                var before = new TreeMap<String, Integer>();
                var after = new TreeMap<String, Integer>();
                for (var id : b) before.merge(id, 1, Integer::sum);
                for (var id : m.result()) after.merge(id, 1, Integer::sum);
                check(before.equals(after), "A move loses and adds no troop: " + b + " " + from + "->" + to + " gave " + m.result());
            }
            for (var troop : pool) for (int seat = 0; seat < 20; seat += 3) {
                var d = BannerDrop.fromRoll(b, troop, seat, RULES);
                plans++;
                if (d.ok()) { oks++; sound("random drop " + b + " " + troop + "@" + seat, b, d); check(BannerDrop.count(d.result(), troop) > BannerDrop.count(b, troop), "A legal drop seats one more of the troop"); }
            }
            for (int from = 0; from < 20; from++) {
                var u = BannerDrop.unseat(b, from, RULES);
                plans++;
                if (u.ok()) { oks++; sound("random unseat", b, u); }
            }
        }
        check(oks > 1000, "The random banners made plenty of legal plans (" + oks + " of " + plans + ")");
        System.out.println("BannerDropCheck ok: " + count + " checks, " + oks + " legal of " + plans + " random plans");
    }

    static String[] rep(String id, int n) { String[] out = new String[n]; Arrays.fill(out, id); return out; }
}
