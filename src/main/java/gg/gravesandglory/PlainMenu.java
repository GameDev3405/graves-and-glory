package gg.gravesandglory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A plain player's chest menu page (plain clients P4, plan decision 1): every War Table page a vanilla client sees is one of these.
 * The slots are buttons, never items: onSlotClick never runs the vanilla click logic, so nothing is taken, swapped, thrown, dragged,
 * collected or put in. PlainMenuRules decides which click runs a button (a plain PICKUP on a button slot, once per cooldown); after
 * any click the whole window and an empty cursor are resent, which undoes the client's guess. Every stack is a vanilla item named in
 * plain text (the no-mod-ids rule).
 *
 * <p>A page is a Page: it draws into the menu with button and icon, and a button's action runs on the server thread after the click
 * (usually Battle.command with the modpack page's own command string, then a redraw).
 */
final class PlainMenu extends GenericContainerScreenHandler {
    /** A page's drawing: fills the menu (its buttons and icons) for the player. Called on open and after every button. */
    interface Page { void draw(PlainMenu m, ServerPlayerEntity p); }

    final SimpleInventory inv;
    final int size;
    private final Map<Integer, Consumer<ServerPlayerEntity>> buttons = new HashMap<>();
    private Page page;
    private long lastRun = -1;

    private PlainMenu(int syncId, PlayerInventory pi, SimpleInventory inv, int rows, Page page) {
        super(type(rows), syncId, pi, inv, rows);
        this.inv = inv;
        this.size = rows * 9;
        this.page = page;
    }

    static ScreenHandlerType<GenericContainerScreenHandler> type(int rows) {
        return switch (rows) { case 1 -> ScreenHandlerType.GENERIC_9X1; case 2 -> ScreenHandlerType.GENERIC_9X2; case 3 -> ScreenHandlerType.GENERIC_9X3; case 4 -> ScreenHandlerType.GENERIC_9X4; case 5 -> ScreenHandlerType.GENERIC_9X5; default -> ScreenHandlerType.GENERIC_9X6; };
    }

    /** Opens a page in a new window (a new title needs one). */
    static void open(ServerPlayerEntity p, Text title, int rows, Page page) {
        int r = Math.max(1, Math.min(6, rows));
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, pi, pl) -> {
            var m = new PlainMenu(syncId, pi, new SimpleInventory(r * 9), r, page);
            if (pl instanceof ServerPlayerEntity sp) m.redraw(sp);
            return m;
        }, title));
    }

    /** Clears the menu to the background pane and draws the page again (same window). */
    void redraw(ServerPlayerEntity p) {
        buttons.clear();
        ItemStack pane = named(Items.GRAY_STAINED_GLASS_PANE, Text.literal(" "), List.of());
        for (int i = 0; i < size; i++) inv.setStack(i, pane.copy());
        page.draw(this, p);
        sendContentUpdates();
    }

    /** Swaps the page shown in this window (its title stays). */
    void show(ServerPlayerEntity p, Page next) { page = next; redraw(p); }

    /** A slot that does something when clicked. */
    void button(int slot, ItemStack stack, Consumer<ServerPlayerEntity> action) {
        if (slot < 0 || slot >= size) return;
        inv.setStack(slot, stack);
        if (action != null) buttons.put(slot, action); else buttons.remove(slot);
    }

    /** A slot that only shows something. */
    void icon(int slot, ItemStack stack) { button(slot, stack, null); }

    /** A button that sends a War Table command (the modpack page's own string) through Battle.command, shows the notice in chat and redraws. */
    void command(int slot, ItemStack stack, String cmd) {
        button(slot, stack, p -> {
            var b = GravesAndGlory.battle;
            if (b == null) return;
            b.command(p, cmd);
            PlainHud.notices(p);
        });
    }

    @Override public void onSlotClick(int slot, int button, SlotActionType type, PlayerEntity player) {
        // never super: the vanilla logic moves items. A refused click only resyncs.
        if (player instanceof ServerPlayerEntity sp) {
            long now = sp.getServer().getTicks();
            if (PlainMenuRules.runs(slot, button, type.ordinal(), size, now, lastRun)) {
                var action = buttons.get(slot);
                if (action != null) {
                    lastRun = now;
                    sp.getServer().execute(() -> {
                        if (sp.currentScreenHandler != this) return; // closed or replaced meanwhile
                        action.accept(sp);
                        if (sp.currentScreenHandler == this) redraw(sp);
                    });
                }
            }
        }
        setCursorStack(ItemStack.EMPTY);
        syncState();
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean canInsertIntoSlot(ItemStack stack, Slot slot) { return false; }
    @Override public boolean canInsertIntoSlot(Slot slot) { return false; }
    @Override public boolean canUse(PlayerEntity player) { return player.isAlive(); }
    @Override public void onClosed(PlayerEntity player) {
        setCursorStack(ItemStack.EMPTY); // nothing is ever held; vanilla's close would drop the cursor
        super.onClosed(player);
    }

    // ---- shared stack helpers (vanilla items, plain text) ----
    static ItemStack named(Item item, Text name, List<Text> lore) { return PlainRecruit.named(item, name, lore); }
    static ItemStack named(Item item, String name, Formatting colour, String... lore) {
        return named(item, Text.literal(name).formatted(colour), java.util.Arrays.stream(lore).map(l -> (Text) Text.literal(l).formatted(Formatting.GRAY)).toList());
    }
    static ItemStack back() { return named(Items.ARROW, "Back to the War Table", Formatting.WHITE); }
}
