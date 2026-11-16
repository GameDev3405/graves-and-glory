package gg.gravesandglory;

import java.util.List;

/**
 * Every order's one answer (army control polish, spec 2026-11-10 section 5): the refusal codes, the gate that picks one, and the words.
 * Pure: no Minecraft types, so the rule checks run it without a game (OrderReplyCheck). The words live in the sheet's orderLines table.
 * <ul>
 *   <li>Refused: the server sends the code on command_v1 (CommandNet.refuse) and the client writes the line (CommandMode.answer).</li>
 *   <li>Accepted: the server writes one action bar line in words, "Archers: attack" (OrderRules.playerOrder).</li>
 * </ul>
 * The 4-tick cooldown between orders is no refusal: it is silent, and the client holds a press that lands in it (CommandMode.issue).
 */
public final class OrderReply {
    private OrderReply() {}
    public static final String DOWN = "down", INTRO = "intro", NO_TROOPS = "no_troops", CHARGE_COOLDOWN = "charge_cooldown",
            TARGET_GONE = "target_gone", LOST_SIGHT = "lost_sight", OUT_OF_RANGE = "out_of_range";
    /** The codes the server may send, in the order the gate tests them (OUT_OF_RANGE comes last, once every group's point is known). */
    public static final List<String> CODES = List.of(DOWN, INTRO, TARGET_GONE, LOST_SIGHT, NO_TROOPS, CHARGE_COOLDOWN, OUT_OF_RANGE);

    /**
     * The refusal for an order that has passed the silent gates (a battle running, the commander seated, not inside the command cooldown),
     * before any point is worked out; null when nothing refuses it yet. down: the commander is respawning; intro: the round intro is
     * playing; targetGone: an aimed enemy that is dead, missing or not an enemy; targetUnseen: an aimed enemy alive but out of the
     * commander's sight (Kort, 2026-11-16: "Lost sight of them", apart from a dead one); living: the named groups' living troops; charge and
     * chargeReady: a Charge, and whether the commander's Charge is off cooldown. A shape-only order (formation) skips the troop count:
     * it moves nobody. An empty Charge is refused as no_troops before the cooldown is read, so it never starts the cooldown.
     */
    public static String gate(boolean down, boolean intro, boolean targetGone, boolean targetUnseen, String order, int living, boolean chargeReady) {
        if (down) return DOWN;
        if (intro) return INTRO;
        if (targetGone) return TARGET_GONE;
        if (targetUnseen) return LOST_SIGHT;
        if (!"formation".equals(order) && living <= 0) return NO_TROOPS;
        if ("charge".equals(order) && !chargeReady) return CHARGE_COOLDOWN;
        return null;
    }

    /** The refusal's line: n is the seconds of down and charge_cooldown, subject the group word of no_troops ("riders", "troops"). */
    public static String refusal(String code, int n, String subject) {
        return text(code).replace("{n}", String.valueOf(Math.max(0, n))).replace("{groups}", subject == null || subject.isEmpty() ? "troops" : subject);
    }

    /** The refusal's line as this client draws it: through the UI editor's gate, so an owner can retype it. Each default is the sheet's
     *  orderLines text for the code (UiEditorCheck pins them equal, so with no edit it reads exactly as refusal()); an unknown code reads as
     *  refusal() does. */
    public static String refusalLine(String code, int n, String subject) {
        String num = String.valueOf(Math.max(0, n)), groups = subject == null || subject.isEmpty() ? "troops" : subject;
        return switch (code) {
            case DOWN -> UiText.t("order.refusal.down", "You are down. Orders wait until you return ({n} s).", "n", num);
            case INTRO -> UiText.t("order.refusal.intro", "Wait for the horn.");
            case NO_TROOPS -> UiText.t("order.refusal.noTroops", "No {groups} left.", "groups", groups);
            case CHARGE_COOLDOWN -> UiText.t("order.refusal.chargeCooldown", "Charge ready in {n} s.", "n", num);
            case TARGET_GONE -> UiText.t("order.refusal.targetGone", "That enemy is gone.");
            case LOST_SIGHT -> UiText.t("order.refusal.lostSight", "Lost sight of them.");
            case OUT_OF_RANGE -> UiText.t("order.refusal.outOfRange", "Too far to order.");
            default -> refusal(code, n, subject);
        };
    }

    /** The respawn card's second line as this client draws it, through the UI editor's gate (default: the sheet's downCard text). */
    public static String downCardLine() { return UiText.t("order.downCard", "Your troops keep their last orders"); }

    /** The accepted order's line: the groups by name ("Infantry and Riders", "All"), then the order in words; shape is a Formation's label. */
    public static String accepted(String order, List<String> names, String shape) {
        return text(order).replace("{groups}", join(names)).replace("{shape}", shape == null ? "" : shape);
    }

    /** "A", "A and B", "A, B and C"; nothing named reads "All". */
    public static String join(List<String> names) {
        if (names == null || names.isEmpty()) return "All";
        if (names.size() == 1) return names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1)) + " and " + names.get(names.size() - 1);
    }

    /** A group's name on a line: All for the whole army, the sheet's label for the five groups, else the id as given (a custom group's own name). */
    public static String name(String group) {
        if ("all".equals(group)) return "All";
        for (var g : Spec.GROUPS) if (g.id().equals(group)) return g.label();
        return group;
    }

    /** The word no_troops uses for the named groups: one group's name in lower case ("riders"), else "troops". */
    public static String subject(List<String> names) {
        if (names == null || names.size() != 1) return "troops";
        String g = names.get(0);
        if ("All".equals(g) || "All troops".equals(g)) return "troops";
        return g.toLowerCase(java.util.Locale.ROOT);
    }

    /** The respawn card's second line while the commander is down. */
    public static String downCard() { return text("downCard"); }

    /** The sheet's line for an id (the code, the order or downCard); the id itself if the sheet lacks it. */
    static String text(String id) {
        for (var row : Spec.ORDERLINES) if (row.id().equals(id)) return row.text();
        return id;
    }
}
