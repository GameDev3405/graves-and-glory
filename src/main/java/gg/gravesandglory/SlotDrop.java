package gg.gravesandglory;

import java.util.*;

/** The rules of dragging on the War Table's Banner page, apart from any screen. A drop is nothing new for the server: each plan is a
 *  short run of the same "slot:(seat):(troop or empty)" commands a click sends, and every step is worked out here with the server's own
 *  rule (Campaign's slot command: the seat is set, then the banner is repacked and cut to the Houses that can take the field), so a plan
 *  says what the banner will be and in how many steps. The banner is always packed: troops sit front to back and the empty seats are
 *  the last ones, so a troop dropped on any empty seat lands on the first free seat, and a seated troop moved to an empty seat goes to
 *  the end of the line. A wide troop (an Epic's two seats, a Legendary's four) is one unit that moves with its later halves. */
final class SlotDrop {
    private SlotDrop() {}

    static final String EMPTY = RosterMath.EMPTY, MARKER = RosterMath.MARKER;

    /** What the server checks a slot command against: the troops' sheet, the troops the commander owns and the Houses that can take the field. */
    record Rules(RosterMath.Catalog catalog, Set<String> owned, Set<String> fieldable) {}

    /** A drop worked out. ok: whether it changes the banner as asked; why: the reason when not; commands: the slot commands to send,
     *  in order; states: the banner after each command (the last is result), so the next command is sent when the banner shows the one before;
     *  changed: the seats whose troop differs in result. */
    record Plan(boolean ok, String why, List<String> commands, List<List<String>> states, List<String> result, List<Integer> changed) {
        static Plan no(String why) { return new Plan(false, why, List.of(), List.of(), List.of(), List.of()); }
        boolean touches(int seat) { return changed.contains(seat); }
    }

    static String command(int seat, String troop) { return "slot:" + seat + ":" + troop; }

    /** The banner after the server takes "slot:(slot):(id)": the seat set, then repack and the Houses that cannot field cut. A command
     *  the server ignores (a seat out of range, an unknown or unowned troop, a House that cannot field) returns the banner as it is. */
    static List<String> apply(List<String> banner, int slot, String id, Rules r) {
        if (slot < 0 || slot >= banner.size()) return banner;
        if (!EMPTY.equals(id) && (!r.catalog().known(id) || !r.owned().contains(id) || !r.fieldable().contains(r.catalog().houseOf(id)))) return banner;
        List<String> entries = new ArrayList<>(banner);
        entries.set(slot, id);
        return RosterMath.fieldable(RosterMath.repack(entries, r.catalog(), r.owned()), r.catalog(), r.fieldable());
    }

    /** The first seat of the unit seat i belongs to: i itself, or the seat a wide troop starts in when i is one of its later halves. */
    static int head(List<String> banner, int i) {
        int j = Math.max(0, Math.min(i, banner.size() - 1));
        while (j > 0 && MARKER.equals(banner.get(j))) j--;
        return j;
    }

    /** The seats a unit that starts in seat head takes: it and the markers after it. */
    static List<Integer> seatsOf(List<String> banner, int head) {
        List<Integer> out = new ArrayList<>();
        if (head < 0 || head >= banner.size() || EMPTY.equals(banner.get(head)) || MARKER.equals(banner.get(head))) return out;
        out.add(head);
        for (int i = head + 1; i < banner.size() && MARKER.equals(banner.get(i)); i++) out.add(i);
        return out;
    }

    /** The first seat of each unit, front to back. */
    static List<Integer> heads(List<String> banner) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < banner.size(); i++) if (!EMPTY.equals(banner.get(i)) && !MARKER.equals(banner.get(i))) out.add(i);
        return out;
    }

    static int firstEmpty(List<String> banner) { return banner.indexOf(EMPTY); }

    static int count(List<String> banner, String id) { return Collections.frequency(banner, id); }

    static List<Integer> changed(List<String> before, List<String> after) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < Math.min(before.size(), after.size()); i++) if (!before.get(i).equals(after.get(i))) out.add(i);
        return out;
    }

    private static Plan plan(List<String> before, List<String> cmds, List<List<String>> states) {
        var result = states.get(states.size() - 1);
        return new Plan(!result.equals(before), "", cmds, states, result, changed(before, result));
    }

    /** A troop of the roll dropped on seat: an empty seat takes it at the first free seat; an occupied one gives its unit's place to it (as a
     *  click on the seat does). Illegal when no room is left for it: too few free seats together, or a rarity at its limit. */
    static Plan fromRoll(List<String> banner, String troop, int seat, Rules r) {
        if (seat < 0 || seat >= banner.size()) return Plan.no("No such seat.");
        boolean empty = EMPTY.equals(banner.get(seat));
        int slot = empty ? seat : head(banner, seat);
        var next = apply(banner, slot, troop, r);
        if (next.equals(banner)) return Plan.no(!empty && banner.get(slot).equals(troop) ? "Already seated there." : noRoom(banner, troop, r));
        if (count(next, troop) <= count(banner, troop)) return Plan.no(noRoom(banner, troop, r));
        return plan(banner, List.of(command(slot, troop)), List.of(next));
    }

    /** Why a troop does not fit the banner: its rarity is at the limit, or there are too few free seats together. */
    static String noRoom(List<String> banner, String troop, Rules r) {
        var c = r.catalog();
        String rarity = c.rarityOf(troop);
        int w = Math.max(1, c.weight(troop));
        if (rarity != null) {
            int units = 0;
            for (var id : banner) if (rarity.equals(c.rarityOf(id))) units++;
            int max = RosterMath.bannerMax(rarity);
            if (units >= max) return "A banner holds " + max + " " + rarity + " troop" + (max == 1 ? "" : "s") + ".";
        }
        int free = Collections.frequency(banner, EMPTY);
        return w > 1 && free > 0 && free < w ? "No " + w + " free seats together." : "Every seat is taken.";
    }

    /** The seated troop whose unit holds seat `from`, dropped on seat `to`. An empty seat: the troop leaves its place and is seated again
     *  at the end of the line. An occupied seat: the two units trade places in the line, however wide each is. Illegal when nothing would
     *  change (the same unit, the same troop, the troop already last), or when the trade cannot be made within the rarity limits. */
    static Plan move(List<String> banner, int from, int to, Rules r) {
        if (from < 0 || from >= banner.size() || to < 0 || to >= banner.size()) return Plan.no("No such seat.");
        if (EMPTY.equals(banner.get(from))) return Plan.no("Nothing seated there.");
        int a = head(banner, from);
        String u = banner.get(a);
        if (EMPTY.equals(banner.get(to))) {
            var cleared = apply(banner, a, EMPTY, r);
            int k = firstEmpty(cleared);
            var placed = apply(cleared, k, u, r);
            var p = plan(banner, List.of(command(a, EMPTY), command(k, u)), List.of(cleared, placed));
            return p.ok() ? p : Plan.no("Already at the end of the line.");
        }
        int b = head(banner, to);
        if (a == b) return Plan.no("That troop's own seat.");
        String v = banner.get(b);
        if (u.equals(v)) return Plan.no("The same troop.");
        // the units' order with the two traded, laid out as the server would pack it
        List<String> order = new ArrayList<>();
        for (int h : heads(banner)) order.add(h == a ? v : h == b ? u : banner.get(h));
        var target = RosterMath.repack(order, r.catalog(), r.owned());
        if (count(target, u) != count(banner, u) || count(target, v) != count(banner, v)) return Plan.no("No room to trade those two.");
        // two steps either way: put one troop in the other's place, then the other in the place left; the order that keeps the limits wins
        var viaV = tradeSteps(banner, a, b, u, v, true, r);
        if (viaV != null && viaV.get(viaV.size() - 1).equals(target)) return plan(banner, tradeCommands(banner, a, b, u, v, true, r), viaV);
        var viaU = tradeSteps(banner, a, b, u, v, false, r);
        if (viaU != null && viaU.get(viaU.size() - 1).equals(target)) return plan(banner, tradeCommands(banner, a, b, u, v, false, r), viaU);
        return Plan.no("No room to trade those two.");
    }

    /** The banners after each of the two trade commands, or null when the first leaves the units in a different count. firstA: the unit at
     *  a gets the troop of b first (else the unit at b gets the troop of a first). */
    private static List<List<String>> tradeSteps(List<String> banner, int a, int b, String u, String v, boolean firstA, Rules r) {
        var heads = heads(banner);
        int ia = heads.indexOf(a), ib = heads.indexOf(b);
        var s1 = firstA ? apply(banner, a, v, r) : apply(banner, b, u, r);
        var h1 = heads(s1);
        if (h1.size() != heads.size()) return null;
        // the units keep their order, so the unit left to change is the one at the same index in the banner it was
        int other = firstA ? h1.get(ib) : h1.get(ia);
        var s2 = apply(s1, other, firstA ? u : v, r);
        return List.of(s1, s2);
    }

    private static List<String> tradeCommands(List<String> banner, int a, int b, String u, String v, boolean firstA, Rules r) {
        var heads = heads(banner);
        int ia = heads.indexOf(a), ib = heads.indexOf(b);
        var s1 = firstA ? apply(banner, a, v, r) : apply(banner, b, u, r);
        var h1 = heads(s1);
        return firstA ? List.of(command(a, v), command(h1.get(ib), u)) : List.of(command(b, u), command(h1.get(ia), v));
    }

    /** The seated troop whose unit holds seat `from`, dropped back on the roll: its seats empty. */
    static Plan unseat(List<String> banner, int from, Rules r) {
        if (from < 0 || from >= banner.size() || EMPTY.equals(banner.get(from))) return Plan.no("Nothing seated there.");
        int a = head(banner, from);
        var next = apply(banner, a, EMPTY, r);
        return next.equals(banner) ? Plan.no("Nothing to take off.") : plan(banner, List.of(command(a, EMPTY)), List.of(next));
    }
}
