package gg.gravesandglory;

import java.util.*;

/** Rolls through the drop board (design 2026-11-14): with a slot drawn for every roll the troops and rarities come out as often as with no board, pity included; a multiplier under 1 is the old roll draw for draw. Never shipped. */
public final class DropRollCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static void near(String what, double got, double want, int n) {
        double sigma = Math.sqrt(Math.max(want * (1 - want), 1e-9) / n);
        check(Math.abs(got - want) < 4.5 * sigma + 1e-9, what + ": " + got + ", expected " + want);
    }

    public static void main(String[] args) {
        // units: the old roll and the board's roll, a slot drawn each time, over a long run with the pity counter carried
        List<String> commons = List.of("c1", "c2"), rares = List.of("r1", "r2");
        int wc = RosterMath.rarityRow("common").rollWeight(), wr = RosterMath.rarityRow("rare").rollWeight(), we = RosterMath.rarityRow("epic").rollWeight(), cap = Spec.RECRUITMENT[0].epicPity();
        int n = 600000;
        Map<String, Integer> old = new HashMap<>(), board = new HashMap<>();
        var rng = new SplittableRandom(11);
        int pity = 0, forcedOld = 0, forcedBoard = 0;
        for (int i = 0; i < n; i++) { if (pity + 1 >= cap) forcedOld++; var r = RosterMath.roll(commons, rares, "e", wc, wr, we, pity, cap, rng); pity = r.pityAfter(); old.merge(r.troop(), 1, Integer::sum); }
        pity = 0;
        for (int i = 0; i < n; i++) {
            int m = DropMath.multiplier(DropMath.drawSlot(rng));
            if (pity + 1 >= cap) forcedBoard++;
            var r = RosterMath.roll(commons, rares, "e", wc, wr, we, pity, cap, m, rng); pity = r.pityAfter(); board.merge(r.troop(), 1, Integer::sum);
        }
        double total = 2.0 * wc + 2.0 * wr + we;
        for (String t : List.of("c1", "c2")) near("board " + t, board.get(t) / (double) n, old.get(t) / (double) n, n / 2);
        for (String t : List.of("r1", "r2")) { near("board " + t, board.get(t) / (double) n, old.get(t) / (double) n, n / 2); near("rare share", board.get(t) / (double) n, wr / total, n); }
        near("board epic", board.get("e") / (double) n, old.get("e") / (double) n, n / 2);
        near("forced epics", forcedBoard / (double) n, forcedOld / (double) n, n / 2);

        // a multiplier under 1 is the old roll, draw for draw
        var a = new SplittableRandom(5); var b = new SplittableRandom(5);
        for (int i = 0; i < 2000; i++) check(RosterMath.roll(commons, rares, "e", wc, wr, we, i % cap, cap, a).equals(RosterMath.roll(commons, rares, "e", wc, wr, we, i % cap, cap, 0, b)), "multiplier 0 is the old unit roll");
        // pity is still forced on the board
        check(RosterMath.roll(commons, rares, "e", wc, wr, we, cap - 1, cap, 1, rng).troop().equals("e"), "the Epic is forced at pity on any slot");
        // x10 never rolls below what the table allows, x1 still can roll an Epic
        boolean epicOnX1 = false;
        for (int i = 0; i < 200000 && !epicOnX1; i++) epicOnX1 = RosterMath.roll(commons, rares, "e", wc, wr, we, 0, cap, 1, rng).troop().equals("e");
        check(epicOnX1, "x1 can still roll the Epic");

        // gear: rarities over a long run with the pity counter carried, old against board
        Map<String, Integer> gOld = new HashMap<>(), gBoard = new HashMap<>();
        int g = 300000;
        Profile p = rich();
        for (int i = 0; i < g; i++) { var r = Forge.roll(p, "helm", rng); gOld.merge(r.piece().rarity, 1, Integer::sum); p.gear.clear(); p.gearShards = 1 << 30; }
        p = rich();
        for (int i = 0; i < g; i++) { var r = Forge.roll(p, "helm", rng, DropMath.multiplier(DropMath.drawSlot(rng))); gBoard.merge(r.piece().rarity, 1, Integer::sum); p.gear.clear(); p.gearShards = 1 << 30; }
        for (var r : Spec.GEARRARITIES) near("gear " + r.id(), gBoard.getOrDefault(r.id(), 0) / (double) g, gOld.getOrDefault(r.id(), 0) / (double) g, g / 2);
        // multiplier 0 is the old gear roll, draw for draw
        Profile pa = rich(), pb = rich();
        a = new SplittableRandom(9); b = new SplittableRandom(9);
        for (int i = 0; i < 500; i++) {
            var ra = Forge.roll(pa, "weapon", a); var rb = Forge.roll(pb, "weapon", b, 0);
            check(ra.piece().item.equals(rb.piece().item) && ra.piece().rarity.equals(rb.piece().rarity) && ra.piece().quality == rb.piece().quality, "multiplier 0 is the old gear roll");
            pa.gear.clear(); pb.gear.clear();
        }
        // a pity roll on the board is still epic or legendary
        p = rich(); p.gearPity = Gear.forge().pity();
        for (int i = 0; i < 200; i++) { p.gearPity = Gear.forge().pity(); String r = Forge.roll(p, "helm", rng, 1 + i % 10).piece().rarity; check(r.equals("epic") || r.equals("legendary"), "a pity roll gives epic or legendary on any slot, got " + r); p.gear.clear(); }

        // relics: the duplicate refund averages what it was
        p = rich(); for (var r : Spec.RELICS) p.relics.add(r.id());
        p.relics.remove(Spec.RELICS[0].id()); // one missing so the roll is allowed; the rest are duplicates
        int plain = (int) Math.floor(Forge.rollCost("relic") * Gear.forge().relicDuplicatePct() + 1e-9);
        double sum = 0; int dupes = 0;
        for (int i = 0; i < 300000; i++) {
            var r = Forge.roll(p, "relic", rng, DropMath.multiplier(DropMath.drawSlot(rng)));
            if (r.duplicate()) { sum += r.refund(); dupes++; } else p.relics.remove(r.relic());
            p.gearShards = 1 << 30;
        }
        check(Math.abs(sum / dupes - plain) < plain * 0.02 + 0.3, "a relic duplicate refunds " + plain + " on average, got " + sum / dupes);

        System.out.println("DropRollCheck: " + checks + " checks passed");
    }

    static Profile rich() { Profile p = new Profile(); p.gearShards = 1 << 30; p.inventorySize = 1000; return p; }
}
