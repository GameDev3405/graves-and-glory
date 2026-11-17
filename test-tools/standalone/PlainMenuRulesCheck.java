package gg.gravesandglory;

/** Plain chest menus are dupe-safe (plain clients P4, plan decision 1): every click type on every slot region, and the burst limit.
 *  The live counterpart is tools/plain_clients/plainbot.py --menus, which sends each packet and reads the server's inventory. Never shipped. */
public final class PlainMenuRulesCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) {
        int size = 54;
        int[] menuSlots = {0, 17, 53}, playerSlots = {54, 80, 89}, outside = {-999, -1, 90, 1000};
        check(PlainMenuRules.runs(0, 0, PlainMenuRules.PICKUP, size) && PlainMenuRules.runs(53, 1, PlainMenuRules.PICKUP, size), "a left or right click on a menu slot runs its button");
        for (int s : menuSlots) {
            check(!PlainMenuRules.runs(s, 0, PlainMenuRules.QUICK_MOVE, size) && !PlainMenuRules.runs(s, 1, PlainMenuRules.QUICK_MOVE, size), "shift-click on menu slot " + s + " is refused");
            for (int key = 0; key <= 8; key++) check(!PlainMenuRules.runs(s, key, PlainMenuRules.SWAP, size), "number key " + (key + 1) + " swap on menu slot " + s + " is refused");
            check(!PlainMenuRules.runs(s, 40, PlainMenuRules.SWAP, size), "offhand swap (F) on menu slot " + s + " is refused");
            check(!PlainMenuRules.runs(s, 2, PlainMenuRules.CLONE, size), "middle-click copy on menu slot " + s + " is refused");
            check(!PlainMenuRules.runs(s, 0, PlainMenuRules.THROW, size) && !PlainMenuRules.runs(s, 1, PlainMenuRules.THROW, size), "Q and ctrl-Q on menu slot " + s + " are refused");
            for (int b : new int[]{0, 1, 2, 4, 5, 6, 8, 9, 10}) check(!PlainMenuRules.runs(s, b, PlainMenuRules.QUICK_CRAFT, size), "drag stage " + b + " over menu slot " + s + " is refused");
            check(!PlainMenuRules.runs(s, 0, PlainMenuRules.PICKUP_ALL, size), "double-click collect on menu slot " + s + " is refused");
            check(!PlainMenuRules.runs(s, 2, PlainMenuRules.PICKUP, size) && !PlainMenuRules.runs(s, -1, PlainMenuRules.PICKUP, size), "a pickup with an odd button on menu slot " + s + " is refused");
        }
        for (int s : playerSlots) for (int mode = 0; mode <= 6; mode++) for (int b = 0; b <= 40; b++)
            check(!PlainMenuRules.runs(s, b, mode, size), "player inventory slot " + s + " mode " + mode + " button " + b + " does nothing");
        for (int s : outside) for (int mode = 0; mode <= 6; mode++) check(!PlainMenuRules.runs(s, 0, mode, size), "slot " + s + " outside the menu does nothing (mode " + mode + ")");
        check(!PlainMenuRules.runs(27, 0, PlainMenuRules.PICKUP, 27) && PlainMenuRules.runs(26, 0, PlainMenuRules.PICKUP, 27), "a 3-row menu's first player slot is 27");
        for (int mode = 7; mode < 12; mode++) check(!PlainMenuRules.runs(0, 0, mode, size), "an unknown mode " + mode + " is refused");
        // burst limit: a double-click or a packet burst runs a button once
        check(PlainMenuRules.runs(4, 0, PlainMenuRules.PICKUP, size, 100, -1), "the first click runs");
        check(!PlainMenuRules.runs(4, 0, PlainMenuRules.PICKUP, size, 100, 100) && !PlainMenuRules.runs(4, 0, PlainMenuRules.PICKUP, size, 103, 100), "a second click within the cooldown does not");
        check(PlainMenuRules.runs(4, 0, PlainMenuRules.PICKUP, size, 100 + PlainMenuRules.COOLDOWN, 100), "a click after the cooldown runs");
        check(!PlainMenuRules.runs(4, 0, PlainMenuRules.QUICK_MOVE, size, 1000, -1), "the cooldown never lets a refused click through");
        System.out.println("PlainMenuRulesCheck: " + checks + " checks passed");
    }
}
