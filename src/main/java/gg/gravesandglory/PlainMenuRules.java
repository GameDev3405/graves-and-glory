package gg.gravesandglory;

/**
 * What a click in a plain chest menu does (plain clients P4, plan decision 1), without Minecraft classes so a standalone check pins
 * it. A plain menu is a page of buttons: no item ever moves. The one click that does anything is a plain left or right PICKUP on a
 * button slot inside the menu, at most once per COOLDOWN ticks. Every other click is refused: shift-click (QUICK_MOVE), a number-key
 * or offhand swap (SWAP), the middle-click copy (CLONE), Q and ctrl-Q (THROW), a drag (QUICK_CRAFT), a double-click collect
 * (PICKUP_ALL), a click outside the window (slot -999) and any slot of the player's inventory. The menu then resends every slot and
 * an empty cursor, so the client's guess is undone, and nothing is ever held to drop on close.
 */
public final class PlainMenuRules {
    private PlainMenuRules() {}

    /** SlotActionType's ordinals (1.21.1). */
    public static final int PICKUP = 0, QUICK_MOVE = 1, SWAP = 2, CLONE = 3, THROW = 4, QUICK_CRAFT = 5, PICKUP_ALL = 6;
    /** Ticks between two buttons run from one menu: a double-click or a burst of packets runs a button once. */
    public static final int COOLDOWN = 8;

    /** Whether the click runs the button in slot: a PICKUP with the left or right button on a menu slot (0 to menuSize - 1). */
    public static boolean runs(int slot, int button, int mode, int menuSize) {
        return mode == PICKUP && (button == 0 || button == 1) && slot >= 0 && slot < menuSize;
    }

    /** runs, and the menu's last button ran at least COOLDOWN ticks ago (lastRun below 0: none yet). */
    public static boolean runs(int slot, int button, int mode, int menuSize, long now, long lastRun) {
        return runs(slot, button, mode, menuSize) && (lastRun < 0 || now - lastRun >= COOLDOWN);
    }
}
