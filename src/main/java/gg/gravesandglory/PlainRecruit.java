package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The War Table's Recruit page as a 6-row chest menu for a plain player (plain clients, port plan P3; the P2 spike's menu). Row 1: the
 * Houses (a click shows that House), the sealed letter (Enlist) and the purse; rows 3 to 6: the House's muster roll, each card with
 * its action. A click sends the War Table's own command (roll:, buy:, star:, legendary:) through Battle.command, so every rule and
 * refusal is the modpack page's; the result notice comes to chat (PlainHud.notices).
 */
final class PlainRecruit {
    private PlainRecruit() {}

    /** RecruitPages.action without its client classes: what the card's button does for this troop (null: no action). */
    record Action(String label, String command, boolean active, String tip) {}

    static Action action(Spec.TroopsRow t, Profile p) {
        var R = Spec.RECRUITMENT[0];
        var o = p.owned.get(t.id());
        boolean full = o != null && o.stars >= R.maxStars();
        String house = RosterMath.houseName(t.house());
        int shards = p.shards.getOrDefault(t.house(), 0), renown = p.renown.getOrDefault(t.house(), 0);
        switch (t.rarity()) {
            case "common", "rare" -> {
                if (full) return null;
                int cost = Profile.CHOSEN_WRITS; boolean ok = p.troopWrits >= cost;
                return new Action("Buy " + cost, "buy:" + t.id(), ok, !ok ? "Need " + cost + " writs. You have " + p.troopWrits + "." : o == null ? "Joins your collection." : "Adds a copy.");
            }
            case "epic", "legendary" -> {
                if (o != null) {
                    if (full) return null;
                    var r = RosterMath.rarityRow(t.rarity()); int cost = r == null ? 0 : o.stars <= 1 ? r.shardStar2() : r.shardStar3(); boolean ok = shards >= cost;
                    return new Action("Star " + (o.stars + 1), "star:" + t.id(), ok, (ok ? "Star " + (o.stars + 1) + " for " : "Need ") + cost + " " + house + " shards.");
                }
                if (t.rarity().equals("epic")) return new Action("From letters only", null, false, "An Epic comes only from letters.");
                if (p.pacts > 0) return new Action("Pact", "legendary:" + t.house() + ":pact", true, "Spend your Pact.");
                boolean ready = renown >= R.renownForLegendary() && shards >= R.legendaryShards();
                return new Action(R.legendaryShards() + " shards", "legendary:" + t.house() + ":shards", ready, ready ? "Joins your collection." : "Needs " + R.renownForLegendary() + " Renown and " + R.legendaryShards() + " shards, or a Pact.");
            }
            default -> { return null; }
        }
    }

    static Formatting rarityColour(String r) {
        return switch (r) { case "rare" -> Formatting.AQUA; case "epic" -> Formatting.LIGHT_PURPLE; case "legendary" -> Formatting.GOLD; default -> Formatting.WHITE; };
    }

    static ItemStack named(Item item, Text name, List<Text> lore) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, name.copy().styled(st -> st.withItalic(false)));
        if (!lore.isEmpty()) s.set(DataComponentTypes.LORE, new LoreComponent(lore.stream().map(l -> (Text) l.copy().styled(st -> st.withItalic(false))).toList()));
        return s;
    }

    static Text crest(String house) {
        int g = PlainPack.houseGlyph(house);
        return g < 0 ? Text.empty() : Text.empty().append(PlainHud.glyph(g)).append(Text.literal(" ")); // siblings: the space keeps the default font
    }

    /** Opens the page on a House (null: the first fieldable House). */
    static void open(ServerPlayerEntity p, String houseId) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        Profile pr = b.campaign.profile(p.getUuid());
        List<Spec.HousesRow> houses = Arrays.stream(Spec.HOUSES).filter(Spec.HousesRow::fieldable).toList();
        if (houses.isEmpty()) houses = Arrays.asList(Spec.HOUSES);
        String house = houseId != null ? houseId : houses.get(0).id();
        var inv = new SimpleInventory(54);
        Map<Integer, String> actions = new HashMap<>();
        ItemStack pane = named(Items.GRAY_STAINED_GLASS_PANE, Text.literal(" "), List.of());
        for (int i = 0; i < 54; i++) inv.setStack(i, pane.copy());
        int col = 0;
        for (var h : houses) {
            if (col > 4) break;
            boolean on = h.id().equals(house);
            Item banner = switch (h.id()) { case "gilded" -> Items.YELLOW_BANNER; case "graveborn" -> Items.BLACK_BANNER; case "ironclad" -> Items.GRAY_BANNER; case "tidecallers" -> Items.CYAN_BANNER; default -> Items.GREEN_BANNER; };
            inv.setStack(col, named(banner, Text.empty().append(crest(h.id())).append(Text.literal(h.name()).formatted(on ? Formatting.GOLD : Formatting.WHITE)),
                List.of(Text.literal("Shards " + pr.shards.getOrDefault(h.id(), 0) + "  Renown " + pr.renown.getOrDefault(h.id(), 0)).formatted(Formatting.GRAY),
                    Text.literal(on ? "Shown" : "Click to show").formatted(on ? Formatting.GREEN : Formatting.DARK_GRAY))));
            actions.put(col, "house:" + h.id());
            col++;
        }
        boolean canRoll = pr.troopWrits >= Profile.ROLL_WRITS;
        inv.setStack(6, named(Items.PAPER, Text.literal("Enlist: open a sealed letter").formatted(Formatting.GOLD),
            List.of(Text.literal("A random " + RosterMath.houseName(house) + " troop for " + Profile.ROLL_WRITS + " writs").formatted(Formatting.GRAY),
                Text.literal(canRoll ? "Click to enlist" : "Need " + Profile.ROLL_WRITS + " writs").formatted(canRoll ? Formatting.GREEN : Formatting.RED))));
        if (canRoll) actions.put(6, "roll:" + house);
        inv.setStack(8, named(Items.GOLD_NUGGET, Text.literal("Purse").formatted(Formatting.YELLOW),
            List.of(Text.literal("Writs " + pr.troopWrits).formatted(Formatting.WHITE), Text.literal("Pacts " + pr.pacts).formatted(Formatting.WHITE))));
        int slot = 18;
        for (var t : Spec.TROOPS) {
            if (!t.house().equals(house) || slot >= 54) continue;
            var o = pr.owned.get(t.id());
            Item egg = Registries.ITEM.get(Identifier.of("minecraft", t.entity() + "_spawn_egg"));
            if (egg == Items.AIR) egg = Items.PAPER;
            Action a = action(t, pr);
            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal(PlainClients.label(t.rarity()) + " " + t.group() + (t.ranged() ? ", ranged" : "")).formatted(rarityColour(t.rarity())));
            lore.add(Text.literal(o == null ? "Not in your collection" : "Owned: " + "★".repeat(o.stars) + "☆".repeat(Math.max(0, Spec.RECRUITMENT[0].maxStars() - o.stars)) + "  copies " + o.copies).formatted(Formatting.GRAY));
            lore.add(Text.literal("HP " + t.health() + "  damage " + t.damage() + "  cost " + t.cost() + " gold").formatted(Formatting.DARK_GRAY));
            if (a == null) lore.add(Text.literal("Full stars").formatted(Formatting.DARK_GRAY));
            else {
                lore.add(Text.literal(a.label() + (a.active() ? " (click)" : "")).formatted(a.active() ? Formatting.GREEN : Formatting.RED));
                lore.add(Text.literal(a.tip()).formatted(Formatting.GRAY));
                if (a.active() && a.command() != null) actions.put(slot, a.command());
            }
            inv.setStack(slot, named(egg, Text.literal(t.name()).formatted(rarityColour(t.rarity())), lore));
            slot++;
        }
        Text title = Text.empty().append(crest(house)).append(Text.literal("Recruit: " + RosterMath.houseName(house)).styled(st -> st.withFont(PlainPack.TITLE_FONT)));
        final String shown = house;
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInv, pl) -> new Menu(syncId, playerInv, inv, actions, shown), title));
    }

    /** A chest menu whose slots are buttons: a click runs the slot's command and never moves an item. */
    static final class Menu extends GenericContainerScreenHandler {
        final Map<Integer, String> actions;
        final String house;
        Menu(int syncId, PlayerInventory pi, SimpleInventory inv, Map<Integer, String> actions, String house) {
            super(ScreenHandlerType.GENERIC_9X6, syncId, pi, inv, 6);
            this.actions = actions;
            this.house = house;
        }
        @Override public void onSlotClick(int slot, int button, SlotActionType type, PlayerEntity player) {
            if (player instanceof ServerPlayerEntity sp && slot >= 0 && slot < 54) {
                String a = actions.get(slot);
                if (a != null) sp.getServer().execute(() -> click(sp, a, house));
            }
            syncState();
        }
        @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
        @Override public boolean canUse(PlayerEntity player) { return true; }
    }

    static void click(ServerPlayerEntity p, String a, String house) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        if (a.startsWith("house:")) { open(p, a.substring(6)); return; }
        b.command(p, a);
        PlainHud.notices(p);
        open(p, house);
    }
}
