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
        if (b.equals(ERASE) || b.equals(table.get(cell))) { next.remove(cell); command = command(0, cell, b.equals(ERASE) ? ERASE : b); }
        else {
            if (!r.c().known(b)) return new Plan(false, why("unknown"), command, same);
            next.put(cell, b);
        }
        String refusal = RosterMath.validTable(next, r.c(), r.owned(), r.houses());
        if (refusal != null) return new Plan(false, why(refusal), command, same);
        return new Plan(true, "", command, next);
    }

    /** The plain-words sentence for a RosterMath.validTable failure name. */
    static String why(String failure) {
        if (failure == null) return "";
        switch (failure) {
            case "range": return "That cell is not on the war table.";
            case "unknown": return "That is not a troop.";
            case "not_owned": return "You do not own that troop yet.";
            case "house": return "That troop's House has not joined the war.";
            case "count": return "A banner holds at most " + RosterMath.maxTroops() + " troops.";
            case "cost": return "That is more than the banner's " + RosterMath.allowance() + " gold allows.";
            default:
                if (failure.endsWith("_limit")) {
                    String rarity = failure.substring(0, failure.length() - 6);
                    int max = RosterMath.bannerMax(rarity);
                    String name = rarity.isEmpty() ? rarity : Character.toUpperCase(rarity.charAt(0)) + rarity.substring(1);
                    return max == 1 ? "Only one " + name + " troop can stand in a banner." : "Only " + max + " " + name + " troops can stand in a banner.";
                }
                return "That does not fit the war table.";
        }
    }
}
