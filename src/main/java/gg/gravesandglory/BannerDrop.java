package gg.gravesandglory;

import java.util.*;

/** The paint rules of the War Table's banner page, apart from any screen. The client and the server both run them: the client to preview a stroke and
 *  build the command it sends, the server to decide the same command (BannerCommands). A stroke puts one troop on one cell, takes the troop of that
 *  cell off (the same brush on a cell that holds it, or the brush "-"), and is allowed only when the whole table still passes RosterMath.validTable. */
final class BannerDrop {
    private BannerDrop() {}

    /** The brush that takes a troop off a cell. */
    static final String ERASE = "-";

    /** What a stroke is checked against: the troops' sheet, the troops the commander owns and the Houses that can take the field. */
    record Rules(RosterMath.Catalog c, Set<String> owned, Set<String> houses) {}

    /** A stroke worked out. ok: whether it is allowed; why: the reason in plain words when not (empty when ok); command: the "cell:(banner):(col):(row):(id or -)"
     *  string a client sends, banner 0 here (withBanner sets another); result: the table after the stroke (the table as it was when refused). */
    record Plan(boolean ok, String why, String command, SortedMap<Integer, String> result) {
        /** The same plan with its command addressed to another banner number. */
        Plan withBanner(int banner) { return new Plan(ok, why, command.replaceFirst("^cell:[0-9]+:", "cell:" + banner + ":"), result); }
    }

    /** The command a stroke on cell sends: "cell:(banner):(col):(row):(id or -)". */
    static String command(int banner, int cell, String brush) {
        return "cell:" + banner + ":" + WarGrid.col(cell) + ":" + WarGrid.row(cell) + ":" + brush;
    }

    /** Paints brush on cell of table: a troop id places it there (replacing what stands there), the same troop already there takes it off, ERASE takes off whatever
     *  stands there. The result must pass RosterMath.validTable for the rules, else the plan is refused with the reason. */
    static Plan paint(SortedMap<Integer, String> table, int cell, String brush, Rules r) {
        SortedMap<Integer, String> same = new TreeMap<>(table);
        String command = command(0, Math.max(0, cell), brush == null ? ERASE : brush);
        if (!WarGrid.inRange(cell)) return new Plan(false, why("range"), command, same);
        String b = brush == null ? ERASE : brush;
        SortedMap<Integer, String> next = new TreeMap<>(table);
        if (b.equals(ERASE) || b.equals(table.get(cell))) { next.remove(cell); command = command(0, cell, ERASE); } // taking off always sends the ERASE id, so the same troop clicked twice is two different commands
        else {
            if (!r.c().known(b)) return new Plan(false, why("unknown"), command, same);
            next.put(cell, b);
        }
        String refusal = RosterMath.validTable(next, r.c(), r.owned(), r.houses());
        if (refusal != null) {
            String reason = why(refusal);
            if (refusal.equals("cost") && !b.equals(ERASE)) { // say the price against what is left, "Need 22 gold, 10 left"
                SortedMap<Integer, String> without = new TreeMap<>(table);
                without.remove(cell);
                reason = costWhy(r.c().cost(b), RosterMath.allowance() - WarGrid.cost(without, r.c()));
            }
            return new Plan(false, reason, command, same);
        }
        return new Plan(true, "", command, next);
    }

    /** The cost refusal: the troop's price against the gold still free ("Need 22 gold, 10 left"). */
    static String costWhy(int price, int left) { return "Need " + price + " gold, " + Math.max(0, left) + " left"; }

    /** The short reason for a RosterMath.validTable failure name: a few words, no full stop, as the cell strip and a row's tooltip show it. */
    static String why(String failure) {
        if (failure == null) return "";
        switch (failure) {
            case "range": return "That cell is not on the table";
            case "unknown": return "That is not a troop";
            case "not_owned": return "You do not own that troop yet";
            case "house": return "This House is not playable yet.";
            case "count": return RosterMath.maxTroops() + " troops max";
            case "cost": return "Over the " + RosterMath.allowance() + " gold limit";
            default:
                if (failure.endsWith("_limit")) {
                    String rarity = failure.substring(0, failure.length() - 6);
                    int max = RosterMath.bannerMax(rarity);
                    String name = rarity.isEmpty() ? rarity : Character.toUpperCase(rarity.charAt(0)) + rarity.substring(1);
                    return max + " " + name + " per banner";
                }
                return "That does not fit your banner";
        }
    }
}
