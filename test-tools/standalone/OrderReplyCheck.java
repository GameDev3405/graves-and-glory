package gg.gravesandglory;

import java.util.*;

/** The army control spec's rule checks for order answers (section 8): each refusal code, an empty Charge never reaching the cooldown, and the words. */
public final class OrderReplyCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        // the gate, code by code, in its order
        check(OrderReply.DOWN.equals(OrderReply.gate(true, true, true, "charge", 0, false)), "down comes first");
        check(OrderReply.INTRO.equals(OrderReply.gate(false, true, true, "attack", 5, true)), "the intro next");
        check(OrderReply.TARGET_GONE.equals(OrderReply.gate(false, false, true, "attack", 5, true)), "a dead or unseen aimed enemy");
        check(OrderReply.NO_TROOPS.equals(OrderReply.gate(false, false, false, "hold", 0, true)), "no living troops in the named groups");
        check(OrderReply.NO_TROOPS.equals(OrderReply.gate(false, false, false, "charge", 0, false)), "an empty Charge is no_troops, never charge_cooldown, so it never reaches the cooldown");
        check(OrderReply.NO_TROOPS.equals(OrderReply.gate(false, false, false, "charge", 0, true)), "an empty Charge with the Charge ready is refused too");
        check(OrderReply.CHARGE_COOLDOWN.equals(OrderReply.gate(false, false, false, "charge", 3, false)), "a Charge before it is ready");
        check(OrderReply.gate(false, false, false, "charge", 3, true) == null, "a ready Charge with troops passes");
        check(OrderReply.gate(false, false, false, "attack", 3, false) == null, "the Charge cooldown does not hold other orders");
        check(OrderReply.gate(false, false, false, "formation", 0, true) == null, "a shape change moves nobody: no troop count");
        for (String o : List.of("attack", "hold", "follow", "retreat")) check(OrderReply.NO_TROOPS.equals(OrderReply.gate(false, false, false, o, 0, true)), o + " with no troops");

        // every code has a line in the sheet, with its number and group filled
        for (String code : OrderReply.CODES) {
            String line = OrderReply.refusal(code, 7, "riders");
            check(!line.equals(code) && !line.contains("{") && line.endsWith("."), code + " reads as a sentence: " + line);
        }
        check(OrderReply.refusal(OrderReply.DOWN, 7, "").equals("You are down. Orders wait until you return (7 s)."), "down: " + OrderReply.refusal(OrderReply.DOWN, 7, ""));
        check(OrderReply.refusal(OrderReply.INTRO, 0, "").equals("Wait for the horn."), "intro");
        check(OrderReply.refusal(OrderReply.NO_TROOPS, 0, OrderReply.subject(List.of("Riders"))).equals("No riders left."), "no_troops names one group");
        check(OrderReply.refusal(OrderReply.NO_TROOPS, 0, OrderReply.subject(List.of("Infantry", "Riders"))).equals("No troops left."), "no_troops for several groups");
        check(OrderReply.refusal(OrderReply.NO_TROOPS, 0, OrderReply.subject(List.of("All"))).equals("No troops left."), "no_troops for the whole army");
        check(OrderReply.refusal(OrderReply.CHARGE_COOLDOWN, 12, "").equals("Charge ready in 12 s."), "charge_cooldown");
        check(OrderReply.refusal(OrderReply.TARGET_GONE, 0, "").equals("That enemy is gone."), "target_gone");
        check(OrderReply.refusal(OrderReply.OUT_OF_RANGE, 0, "").equals("Too far to order."), "out_of_range");
        check(OrderReply.refusal(OrderReply.DOWN, -3, "").contains("(0 s)"), "no negative seconds");

        // accepted lines in words, never the old "archers / hold / 120, -44"
        check(OrderReply.accepted("follow", List.of(OrderReply.name("all")), "").equals("All: follow me"), "All: follow me");
        check(OrderReply.accepted("attack", List.of(OrderReply.name("ranged")), "").equals("Archers: attack"), "Archers: attack");
        check(OrderReply.accepted("hold", List.of(OrderReply.name("infantry"), OrderReply.name("cavalry")), "").startsWith("Infantry and Riders: "), "two groups joined with and");
        check(OrderReply.join(List.of("A", "B", "C")).equals("A, B and C"), "three groups");
        check(OrderReply.accepted("formation", List.of("Infantry"), "Wedge").equals("Infantry: form Wedge"), "a shape change names the shape");
        check(OrderReply.name("custom:7").equals("custom:7"), "an unknown id reads as itself");
        for (var o : Spec.ORDERS) {
            String id = BattleMath.loadOrder(o.id());
            String line = OrderReply.accepted(id, List.of("Infantry"), "Wedge");
            check(line.startsWith("Infantry: ") && !line.contains("{") && !line.contains(" / "), o.id() + " reads in words: " + line);
        }
        check(!OrderReply.downCard().equals("downCard") && !OrderReply.downCard().isBlank(), "the respawn card's second line is in the sheet");
        System.out.println("OrderReplyCheck: " + checks + " checks pass");
    }
}
