package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The in-battle barracks screen (the modpack's RecruitmentScreen: its Barracks page and its Armoury tab) as a plain player's 6-row chest
 * menu (plain clients P4). It opens from the tactics hotbar's Armoury key and from Battle.openBarracks. Row 1: Close, Refresh, the purse
 * and training queue, where the commander stands, Cancel training and the page arrows; rows 2 to 6: one banner troop type each, with its
 * card, Train (train:), Disband (disband:) and the four gear buys Weapon II, Weapon III, Armour II and Armour III
 * (gear:(troop):(weapon|armour):(tier), Veterans.buy). A press sends the modpack's own command; the server checks it again. A button the
 * modpack shows greyed is an icon here with the reason it would give, read from the same server rules (Battle.atBarracks, roomFor,
 * Veterans.refusal). The page does not tick: Refresh draws it again with the current gold and queue.
 */
final class PlainArmoury {
    private PlainArmoury() {}

    static final int ROWS = 5;

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Armoury").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 6, new View());
    }

    static final class View implements PlainMenu.Page {
        int page;

        @Override public void draw(PlainMenu m, ServerPlayerEntity p) {
            m.button(0, PlainMenu.named(Items.BARRIER, "Close", Formatting.WHITE, "Back to the battle."), ServerPlayerEntity::closeHandledScreen);
            m.button(1, PlainMenu.named(Items.CLOCK, "Refresh", Formatting.WHITE, "Shows the current gold, queue and buys."), pl -> {});
            var b = GravesAndGlory.battle;
            var s = b == null ? null : b.squads.get(p.getUuid());
            if (s == null) { m.icon(4, PlainMenu.named(Items.PAPER, "No army", Formatting.GRAY, "The Armoury opens for a commander in a battle.")); return; }
            var E = Spec.ECONOMY[0];
            boolean here = b.atBarracks(p, s), buyHere = Veterans.buyHere(b, p, s);
            List<String> queue = new ArrayList<>(List.of("Training " + s.training.size() + " of " + E.maxQueue()));
            int n = 0;
            for (var t : s.training) {
                if (n++ >= 6) { queue.add("..."); break; }
                queue.add(t.spec().name() + ": " + Math.max(0, (t.finished() - b.time + 19) / 20) + " s");
            }
            m.icon(3, PlainMenu.named(Items.GOLD_INGOT, "Gold " + s.gold, Formatting.YELLOW, queue.toArray(String[]::new)));
            m.icon(4, here ? PlainMenu.named(Items.LIME_BANNER, "At your barracks", Formatting.GREEN, "You can train, disband and buy gear.")
                : PlainMenu.named(Items.RED_BANNER, buyHere ? "At a held point" : "Away from your barracks", buyHere ? Formatting.YELLOW : Formatting.RED,
                    "Return to your barracks to train or disband troops.", buyHere ? "Gear can be bought here." : "Buy gear at your barracks or a held point."));
            if (here && !s.training.isEmpty()) m.command(5, PlainMenu.named(Items.CAULDRON, "Cancel training", Formatting.RED, "Clears the queue; its gold comes back."), "cancel-training");
            else m.icon(5, PlainMenu.named(Items.GRAY_DYE, "Cancel training", Formatting.DARK_GRAY, !here ? "Return to your barracks." : "Nothing is training."));

            List<Spec.TroopsRow> types = Veterans.types(s);
            int pages = Math.max(1, (types.size() + ROWS - 1) / ROWS);
            page = Math.max(0, Math.min(page, pages - 1));
            if (page > 0) m.button(7, PlainMenu.named(Items.SPECTRAL_ARROW, "Earlier troops", Formatting.WHITE, "Page " + page + " of " + pages), pl -> page--);
            if (page < pages - 1) m.button(8, PlainMenu.named(Items.SPECTRAL_ARROW, "Later troops", Formatting.WHITE, "Page " + (page + 2) + " of " + pages), pl -> page++);
            if (types.isEmpty()) m.icon(22, PlainMenu.named(Items.PAPER, "Your banner has no troops.", Formatting.GRAY, "Change your banner on the War Table."));
            for (int r = 0; r < ROWS; r++) {
                int at = page * ROWS + r;
                if (at >= types.size()) break;
                row(m, b, s, p, types.get(at), 9 * (r + 1), here, buyHere);
            }
        }

        void row(PlainMenu m, GravesAndGlory.Battle b, GravesAndGlory.Squad s, ServerPlayerEntity p, Spec.TroopsRow t, int base, boolean here, boolean buyHere) {
            var E = Spec.ECONOMY[0];
            int listed = Collections.frequency(s.banner, t.id());
            int fielded = (int) b.fielded(s).filter(u -> u.spec == t).count();
            int training = (int) s.training.stream().filter(x -> x.spec() == t).count();
            m.icon(base, PlainMenu.named(egg(t), Text.literal(t.name()).formatted(PlainRecruit.rarityColour(t.rarity())), List.of(
                Text.literal(PlainClients.label(t.rarity()) + " " + t.group() + (t.ranged() ? ", ranged" : "")).formatted(Formatting.GRAY),
                Text.literal("Fielded " + fielded + ", training " + training + " of " + listed + " in your banner").formatted(Formatting.WHITE),
                Text.literal("HP " + t.health() + "  damage " + t.damage()).formatted(Formatting.DARK_GRAY))));
            int price = t.cost();
            String why = !here ? "Return to your barracks to recruit." : b.roomFor(s, t) != null ? b.roomFor(s, t)
                : s.training.size() >= E.maxQueue() ? "The training queue is full." : s.gold < price ? "You need " + (price - s.gold) + " more gold." : "";
            double secs = Commander.trainingTicks(t) / 20.0;
            String time = "Training time: " + (secs == Math.rint(secs) ? Long.toString(Math.round(secs)) : Double.toString(secs)) + " s";
            if (why.isEmpty()) m.command(base + 1, PlainMenu.named(Items.LIME_DYE, "Train: " + price + " gold", Formatting.GREEN, time, "Click to recruit one " + t.name() + "."), "train:" + t.id());
            else m.icon(base + 1, PlainMenu.named(Items.GRAY_DYE, "Train: " + price + " gold", Formatting.DARK_GRAY, why));
            String dwhy = !here ? "Return to your barracks to disband." : fielded == 0 ? "None of them is fielded." : "";
            if (dwhy.isEmpty()) m.command(base + 2, PlainMenu.named(Items.RED_DYE, "Disband one", Formatting.RED, "Refunds " + E.disbandRefund() + " gold."), "disband:" + t.id());
            else m.icon(base + 2, PlainMenu.named(Items.GRAY_DYE, "Disband one", Formatting.DARK_GRAY, dwhy));
            if (!Veterans.enabled()) { m.icon(base + 4, PlainMenu.named(Items.GRAY_DYE, "Ranks are off", Formatting.DARK_GRAY, "No gear buys this battle.")); return; }
            var row = Veterans.row(b, s, t);
            buy(m, b, s, t, base + 4, LevelMath.WEAPON, 2, row.weapon2(), buyHere);
            buy(m, b, s, t, base + 5, LevelMath.WEAPON, 3, row.weapon3(), buyHere);
            buy(m, b, s, t, base + 7, LevelMath.ARMOUR, 2, row.armour2(), buyHere);
            buy(m, b, s, t, base + 8, LevelMath.ARMOUR, 3, row.armour3(), buyHere);
        }

        void buy(PlainMenu m, GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.TroopsRow t, int slot, String track, int tier, LevelMath.Buy state, boolean buyHere) {
            boolean weapon = LevelMath.WEAPON.equals(track);
            String name = (weapon ? "Weapon " : "Armour ") + MatchStats.roman(tier);
            int price = LevelMath.price(t.rarity(), tier);
            String why = Veterans.refusal(b, s, t, track, tier, buyHere);
            String level = "For " + t.name() + " soldiers of Rank " + tier + " and up.";
            if (state == LevelMath.Buy.BOUGHT) { m.icon(slot, PlainMenu.named(weapon ? Items.IRON_SWORD : Items.IRON_CHESTPLATE, name + ": bought", Formatting.GREEN, level)); return; }
            Item item = state == LevelMath.Buy.LOCKED ? Items.IRON_BARS : weapon ? (tier == 2 ? Items.IRON_SWORD : Items.DIAMOND_SWORD) : (tier == 2 ? Items.IRON_CHESTPLATE : Items.DIAMOND_CHESTPLATE);
            ItemStack st;
            if (why.isEmpty()) {
                st = PlainMenu.named(item, name + ": " + price + " gold", Formatting.GOLD, level, "Click to buy");
                PlainGear.hideAttributes(st);
                m.command(slot, st, "gear:" + t.id() + ":" + track + ":" + tier);
            } else {
                st = PlainMenu.named(item, name + ": " + price + " gold", Formatting.DARK_GRAY, level, why);
                PlainGear.hideAttributes(st);
                m.icon(slot, st);
            }
        }
    }

    static Item egg(Spec.TroopsRow t) {
        Item egg = Registries.ITEM.get(Identifier.of("minecraft", t.entity() + "_spawn_egg"));
        return egg == Items.AIR ? Items.PAPER : egg;
    }
}
