package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The War Table's Gear page (the modpack's Arsenal, WarTableGear) as a plain player's 6-row chest menu (plain clients P4, plan decision 5).
 * Three tabs on row 1: Arsenal (the six worn slots and the owned pieces; a piece opens its card with Equip, Break down and Repair), Relics
 * (the three relic slots and the owned relics) and Forge (the roll slot, Roll, and the blacksmith's three services). Every press sends the
 * modpack page's own command (gear:equip:, gear:unequip:, gear:relic:, forge:roll:, forge:breakdown:, forge:breakdown-duplicates,
 * forge:repair:, forge:repair-all, forge:expand) through Battle.command, so every rule and refusal is GearCommands' and the notice comes to
 * chat. A plain client never reaches the drop board (DropBoard.available needs the drop payload), so Roll sends forge:roll:, which is the
 * path the server takes for drop:forge: there too. Everything shown is read from the server's Profile through the pure GearView, as the
 * modpack page reads its gear_v1 copy.
 */
final class PlainGear {
    private PlainGear() {}

    static final List<String> FILTERS = List.of("all", "helm", "chest", "legs", "boots", "weapon");
    static final List<String> FORGE_SLOTS = Gear.ROLL_SLOTS;
    /** Grid slots for pieces on the Arsenal tab (rows 3 to 5), and for relics on the Relics tab (rows 4 to 6). */
    static final int GRID = 18, GRID_SIZE = 27;

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Gear").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 6, new View());
    }

    /** The page's state for one window: the tab, the Arsenal's filter, sort and page, the piece card shown, the relic slot picked, the forge slot. */
    static final class View implements PlainMenu.Page {
        String tab = "arsenal", filter = "all", sort = "rarity", forgeSlot = "helm";
        int page, cardUid = -1, relicSlot = -1, armedUid = -1;
        boolean dupesArmed;

        @Override public void draw(PlainMenu m, ServerPlayerEntity p) {
            var b = GravesAndGlory.battle;
            if (b == null) return;
            Profile pr = b.campaign.profile(p.getUuid());
            GearWire.View v = view(pr);
            PlainWarTable.back(m, 0);
            tab(m, 2, "arsenal", Items.IRON_CHESTPLATE, "Arsenal", "Your worn gear and owned pieces.");
            tab(m, 3, "relics", Items.AMETHYST_SHARD, "Relics", "Your three relic slots.");
            tab(m, 4, "forge", Items.ANVIL, "Forge", "Roll gear, break down, repair, expand.");
            m.icon(8, PlainMenu.named(Items.GOLD_NUGGET, "Gear shards " + v.gearShards(), Formatting.YELLOW,
                "Gear power " + v.power(), "Inventory " + GearView.counter(v), "Pity " + v.pity() + " of " + Gear.forge().pity()));
            switch (tab) {
                case "relics" -> relics(m, v);
                case "forge" -> forge(m, v);
                default -> { if (cardUid >= 0 && GearView.piece(v, cardUid) != null) card(m, v); else { cardUid = -1; arsenal(m, v); } }
            }
        }

        void tab(PlainMenu m, int slot, String id, Item item, String name, String what) {
            boolean on = tab.equals(id);
            m.button(slot, PlainMenu.named(item, name + (on ? " (shown)" : ""), on ? Formatting.GREEN : Formatting.GOLD, what),
                on && cardUid < 0 ? null : pl -> { tab = id; cardUid = -1; armedUid = -1; dupesArmed = false; });
        }

        // ---- Arsenal ----
        void arsenal(PlainMenu m, GearWire.View v) {
            double[] t = GearView.totals(v);
            List<String> lore = new ArrayList<>();
            for (int i = 0; i < t.length; i++) if (!GearView.statText(i, t[i]).equals("—")) lore.add(GearView.statWord(i) + " +" + GearView.statText(i, t[i]));
            String set = GearView.setProgress(v);
            if (!set.isEmpty()) lore.add(set);
            if (GearView.nothingWorn(v)) lore.add(GearView.STARTER_NOTICE);
            m.icon(9, PlainMenu.named(Items.ARMOR_STAND, "Commander", Formatting.GOLD, lore.toArray(String[]::new)));
            for (int i = 0; i < Gear.SLOTS.size(); i++) {
                String slot = Gear.SLOTS.get(i);
                Gear.Piece w = GearView.wornIn(v, slot);
                String word = cap(GearView.SLOT_WORDS.get(slot));
                if (w == null) m.icon(10 + i, PlainMenu.named(Items.LIGHT_GRAY_STAINED_GLASS_PANE, word + ": empty", Formatting.DARK_GRAY, "Pick a piece below to wear it."));
                else {
                    List<String> tip = tooltip(v, w);
                    tip.add("Click to unequip");
                    m.command(10 + i, piece(w, word + ": ", tip), "gear:unequip:" + slot);
                }
            }
            List<Gear.Piece> list = GearView.visible(v, filter, sort);
            int pages = Math.max(1, (list.size() + GRID_SIZE - 1) / GRID_SIZE);
            page = Math.max(0, Math.min(page, pages - 1));
            for (int k = 0; k < GRID_SIZE; k++) {
                int at = page * GRID_SIZE + k;
                if (at >= list.size()) break;
                Gear.Piece g = list.get(at);
                List<String> tip = tooltip(v, g);
                tip.add("Click for Equip, Break down and Repair");
                int uid = g.uid;
                m.button(GRID + k, piece(g, "", tip), pl -> { cardUid = uid; armedUid = -1; });
            }
            if (list.isEmpty()) m.icon(GRID + 13, PlainMenu.named(Items.PAPER, "No pieces here", Formatting.GRAY, filter.equals("all") ? "Roll gear at the Forge tab." : "Try another filter."));
            if (page > 0) m.button(45, PlainMenu.named(Items.SPECTRAL_ARROW, "Earlier pieces", Formatting.WHITE, "Page " + page + " of " + pages), pl -> page--);
            if (page < pages - 1) m.button(53, PlainMenu.named(Items.SPECTRAL_ARROW, "Later pieces", Formatting.WHITE, "Page " + (page + 2) + " of " + pages), pl -> page++);
            m.button(48, PlainMenu.named(Items.HOPPER, "Filter: " + cap(filter), Formatting.AQUA, "Click for the next filter.", "All, helm, chest, legs, boots, weapon."),
                pl -> { filter = FILTERS.get((FILTERS.indexOf(filter) + 1) % FILTERS.size()); page = 0; });
            m.button(50, PlainMenu.named(Items.COMPARATOR, "Sort: " + cap(sort), Formatting.AQUA, "Click for the next sort.", "Rarity, newest, slot, set."),
                pl -> { sort = GearView.SORTS.get((GearView.SORTS.indexOf(sort) + 1) % GearView.SORTS.size()); page = 0; });
            m.icon(49, PlainMenu.named(Items.CHEST, "Pieces " + GearView.counter(v), Formatting.WHITE, "Page " + (page + 1) + " of " + pages));
        }

        /** One piece's card: its full tooltip and the three things a piece can have done to it. */
        void card(PlainMenu m, GearWire.View v) {
            Gear.Piece g = GearView.piece(v, cardUid);
            int uid = g.uid;
            boolean worn = GearView.worn(v, uid);
            List<String> tip = tooltip(v, g);
            m.icon(13, piece(g, "", tip));
            m.button(9, PlainMenu.named(Items.ARROW, "Back to the Arsenal", Formatting.WHITE), pl -> { cardUid = -1; armedUid = -1; });
            String slot = Gear.item(g.item).slot();
            if (worn) m.command(29, PlainMenu.named(Items.LEATHER_HELMET, "Unequip", Formatting.YELLOW, "Takes it off the " + GearView.SLOT_WORDS.get(slot) + " slot."), "gear:unequip:" + slot);
            else {
                List<String> l = new ArrayList<>(List.of("Wear it in the " + GearView.SLOT_WORDS.get(slot) + " slot."));
                l.addAll(GearView.compare(v, g));
                m.command(29, PlainMenu.named(Items.LIME_DYE, "Equip", Formatting.GREEN, l.toArray(String[]::new)), "gear:equip:" + uid);
            }
            int value = Forge.breakdownValue(g);
            if (worn) m.icon(31, PlainMenu.named(Items.GRAY_DYE, "Break down", Formatting.DARK_GRAY, "Unequip it first."));
            else if (GearView.confirmBreakdown(g) && armedUid != uid)
                m.button(31, PlainMenu.named(Items.TNT, "Break down for " + value + " gear shards", Formatting.RED, "A " + Gear.rarity(g.rarity).name() + " piece: click again to confirm."), pl -> armedUid = uid);
            else m.command(31, PlainMenu.named(Items.TNT, armedUid == uid ? "Confirm: break it down" : "Break down for " + value + " gear shards", Formatting.RED,
                "Destroys the piece for " + value + " gear shards."), "forge:breakdown:" + uid);
            int cost = Forge.repairCost(g);
            if (g.durability >= Gear.forge().maxDurability()) m.icon(33, PlainMenu.named(Items.GRAY_DYE, "Repair", Formatting.DARK_GRAY, "It needs no repair."));
            else if (v.gearShards() < cost) m.icon(33, PlainMenu.named(Items.GRAY_DYE, "Repair for " + cost + " gear shards", Formatting.DARK_GRAY, "Repair costs " + cost + " gear shards."));
            else m.command(33, PlainMenu.named(Items.SMITHING_TABLE, "Repair for " + cost + " gear shards", Formatting.GREEN, "Back to full durability."), "forge:repair:" + uid);
        }

        // ---- Relics ----
        void relics(PlainMenu m, GearWire.View v) {
            int target = GearView.relicTarget(v, relicSlot);
            for (int i = 0; i < v.relicSlots().size() && i < 3; i++) {
                String id = v.relicSlots().get(i);
                var r = id.isEmpty() ? null : Gear.relic(id);
                boolean on = i == target;
                List<String> lore = new ArrayList<>();
                if (r != null) lore.addAll(relicLore(r));
                lore.add(on ? "Selected: a relic you click goes here." : "Click to send relics here.");
                lore.add("Key " + (6 + i) + " on the battle's tactics page.");
                int slot = i;
                ItemStack st = PlainMenu.named(r == null ? Items.LIGHT_GRAY_STAINED_GLASS_PANE : PlainClients.RELIC_ITEMS[i],
                    "Relic slot " + (i + 1) + ": " + (r == null ? "empty" : r.name()) + (on ? " (selected)" : ""), on ? Formatting.GREEN : r == null ? Formatting.DARK_GRAY : Formatting.LIGHT_PURPLE, lore.toArray(String[]::new));
                m.button(11 + 2 * i, st, pl -> relicSlot = slot);
                if (r != null) m.command(20 + 2 * i, PlainMenu.named(Items.BARRIER, "Empty relic slot " + (i + 1), Formatting.RED, "Takes " + r.name() + " out of the slot."), "gear:relic:" + i + ":-");
            }
            int k = 0;
            for (var r : Spec.RELICS) {
                if (k >= GRID_SIZE) break;
                boolean owned = v.relics().contains(r.id());
                int in = v.relicSlots().indexOf(r.id());
                List<String> lore = new ArrayList<>(relicLore(r));
                if (!owned) {
                    lore.add("Not unlocked: roll a relic at the Forge tab.");
                    m.icon(27 + k, PlainMenu.named(Items.GRAY_DYE, r.name(), Formatting.DARK_GRAY, lore.toArray(String[]::new)));
                } else {
                    if (in >= 0) lore.add("In relic slot " + (in + 1) + ".");
                    lore.add(in == target ? "Already in the selected slot." : "Click to put it in relic slot " + (target + 1) + ".");
                    ItemStack st = PlainMenu.named(relicItem(r), r.name() + (in >= 0 ? " (slot " + (in + 1) + ")" : ""), in >= 0 ? Formatting.GREEN : Formatting.LIGHT_PURPLE, lore.toArray(String[]::new));
                    if (in == target) m.icon(27 + k, st); else m.command(27 + k, st, "gear:relic:" + target + ":" + r.id());
                }
                k++;
            }
        }

        // ---- Forge ----
        void forge(PlainMenu m, GearWire.View v) {
            for (int i = 0; i < FORGE_SLOTS.size(); i++) {
                String s = FORGE_SLOTS.get(i);
                boolean on = s.equals(forgeSlot);
                m.button(10 + i, PlainMenu.named(forgeItem(s), cap(s) + (on ? " (chosen)" : ""), on ? Formatting.GREEN : Formatting.WHITE,
                    "A roll costs " + Forge.rollCost(s) + " gear shards.", on ? "Roll draws from this slot." : "Click to roll this slot."), on ? null : pl -> forgeSlot = s);
            }
            String why = GearView.rollRefusal(v, forgeSlot);
            int cost = Forge.rollCost(forgeSlot);
            List<String> lore = new ArrayList<>(List.of("A random " + (forgeSlot.equals("relic") ? "relic" : forgeSlot + " piece") + " for " + cost + " gear shards."));
            if (!forgeSlot.equals("relic")) {
                int left = Math.max(0, Gear.forge().pity() - v.pity());
                lore.add(left == 0 ? "Pity: this roll is Epic or better." : "Epic or better within " + (left + 1) + " rolls.");
            } else lore.add("A relic you own already refunds some gear shards.");
            if (why.isEmpty()) { lore.add("Click to roll"); m.command(22, PlainMenu.named(Items.ANVIL, "Roll: " + cap(forgeSlot), Formatting.GOLD, lore.toArray(String[]::new)), "forge:roll:" + forgeSlot); }
            else { lore.add(why); m.icon(22, PlainMenu.named(Items.CHIPPED_ANVIL, "Roll: " + cap(forgeSlot), Formatting.RED, lore.toArray(String[]::new))); }
            m.icon(20, PlainMenu.named(Items.BOOK, "Odds", Formatting.WHITE, odds(v).toArray(String[]::new)));
            int dupes = GearView.duplicatesValue(v);
            if (dupes <= 0) m.icon(38, PlainMenu.named(Items.GRAY_DYE, "Break down all duplicates", Formatting.DARK_GRAY, "You have no duplicates to break down."));
            else if (!dupesArmed) m.button(38, PlainMenu.named(Items.TNT, "Break down all duplicates", Formatting.RED, "Keeps the best of each; +" + dupes + " gear shards.", "Click, then confirm."), pl -> dupesArmed = true);
            else m.button(38, PlainMenu.named(Items.TNT, "Confirm: break down duplicates", Formatting.RED, "+" + dupes + " gear shards. Click to confirm."), pl -> {
                dupesArmed = false;
                var b = GravesAndGlory.battle;
                if (b == null) return;
                b.command(pl, "forge:breakdown-duplicates");
                PlainHud.notices(pl);
            });
            int repair = GearView.repairAllCost(v);
            if (repair <= 0) m.icon(40, PlainMenu.named(Items.GRAY_DYE, "Repair all", Formatting.DARK_GRAY, "It needs no repair."));
            else m.command(40, PlainMenu.named(Items.SMITHING_TABLE, "Repair all (" + repair + " gear shards)", v.gearShards() >= repair ? Formatting.GREEN : Formatting.RED,
                v.gearShards() >= repair ? "Every worn-down piece back to full." : "Repairing everything costs " + repair + " gear shards."), "forge:repair-all");
            var f = Gear.forge();
            if (v.inventorySize() >= f.maxSlots()) m.icon(42, PlainMenu.named(Items.GRAY_DYE, "Add a row", Formatting.DARK_GRAY, "Your inventory is as large as it gets."));
            else {
                int c = Forge.expandCost(GearView.asProfile(v));
                m.command(42, PlainMenu.named(Items.CHEST, "Add a row (" + c + " gear shards)", v.gearShards() >= c ? Formatting.GREEN : Formatting.RED,
                    "Inventory " + v.inventorySize() + " to " + Math.min(f.maxSlots(), v.inventorySize() + f.rowSlots()) + " slots.", v.gearShards() >= c ? "Click to buy" : "Another row costs " + c + " gear shards."), "forge:expand");
            }
        }
    }

    /** The rarity odds of the next armour or weapon roll (pity: only Epic and Legendary). */
    static List<String> odds(GearWire.View v) {
        boolean pity = v.pity() >= Gear.forge().pity();
        int total = 0;
        for (var r : Spec.GEARRARITIES) if (!pity || top(r.id())) total += r.rollWeight();
        List<String> out = new ArrayList<>();
        for (var r : Spec.GEARRARITIES) if (!pity || top(r.id())) out.add(r.name() + " " + Math.round(100.0 * r.rollWeight() / Math.max(1, total)) + "%");
        out.add("Relics: drawn evenly.");
        return out;
    }
    static boolean top(String rarity) { return rarity.equals("epic") || rarity.equals("legendary"); }

    /** The server's gear as the modpack page's gear_v1 view (GearWire.View), so the pure GearView rules show the same facts. */
    static GearWire.View view(Profile pr) {
        List<String> slots = new ArrayList<>(pr.relicSlots == null ? List.of() : pr.relicSlots);
        int n = Gear.forge().relicSlots();
        while (slots.size() < n) slots.add("");
        while (slots.size() > n) slots.remove(slots.size() - 1);
        for (int i = 0; i < slots.size(); i++) if (slots.get(i) == null) slots.set(i, "");
        List<Gear.Piece> pieces = new ArrayList<>();
        for (Gear.Piece g : pr.gear) if (g != null) pieces.add(g);
        return new GearWire.View(pr.gearShards, pr.inventorySize, pr.gearPity, pr.gearPower, new LinkedHashMap<>(pr.equipped), new ArrayList<>(pr.relics), slots, pieces);
    }

    /** GearView's hover card without its last line (the modpack's click hint). */
    static List<String> tooltip(GearWire.View v, Gear.Piece g) {
        List<String> t = new ArrayList<>(GearView.tooltip(v, g));
        if (!t.isEmpty()) t.remove(t.size() - 1);
        if (!t.isEmpty()) t.remove(0); // the name is the stack's
        return t;
    }

    static List<String> relicLore(Spec.RelicsRow r) {
        List<String> out = new ArrayList<>();
        out.add(cap(r.role()) + " relic, cooldown " + (r.cooldownTicks() + 19) / 20 + " s");
        out.addAll(wrap(r.text(), 44));
        return out;
    }

    /** A piece as a vanilla item named in plain text, coloured by rarity, its lore wrapped, the vanilla attribute lines hidden. */
    static ItemStack piece(Gear.Piece g, String prefix, List<String> lore) {
        var it = Gear.item(g.item);
        var r = Gear.rarity(g.rarity);
        List<Text> lines = new ArrayList<>();
        for (String l : lore) for (String w : wrap(l, 44)) lines.add(Text.literal(w).formatted(lineColour(l)));
        ItemStack s = PlainMenu.named(pieceItem(it, g.rarity), Text.literal(prefix + (it == null ? g.item : it.name())).formatted(rarityColour(g.rarity)), lines);
        hideAttributes(s);
        if (r != null && Gear.rarityIndex(g.rarity) >= 3) s.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    static Formatting lineColour(String l) {
        if (l.startsWith("▲")) return Formatting.GREEN;
        if (l.startsWith("▼") || l.startsWith("Broken")) return Formatting.RED;
        if (l.startsWith("Click")) return Formatting.YELLOW;
        return Formatting.GRAY;
    }

    static void hideAttributes(ItemStack s) {
        var a = s.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (a != null) s.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, a.withShowInTooltip(false));
        else s.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT.withShowInTooltip(false));
    }

    static Formatting rarityColour(String rarity) {
        return switch (rarity == null ? "" : rarity) {
            case "uncommon" -> Formatting.GREEN; case "rare" -> Formatting.AQUA; case "epic" -> Formatting.LIGHT_PURPLE; case "legendary" -> Formatting.GOLD; default -> Formatting.WHITE;
        };
    }

    /** The vanilla item a piece shows as: armour by slot in a material by rarity, a weapon by its family. */
    static Item pieceItem(Spec.GearItemsRow it, String rarity) {
        if (it == null) return Items.PAPER;
        int tier = Math.max(0, Math.min(4, Gear.rarityIndex(rarity)));
        return switch (it.slot()) {
            case "helm" -> new Item[]{Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET}[tier];
            case "chest" -> new Item[]{Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE}[tier];
            case "legs" -> new Item[]{Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS}[tier];
            case "boots" -> new Item[]{Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS}[tier];
            default -> switch (it.family()) {
                case "greataxe" -> new Item[]{Items.STONE_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE}[tier];
                case "bow" -> Items.BOW;
                case "crossbow" -> Items.CROSSBOW;
                case "staff" -> Items.BLAZE_ROD;
                case "banner" -> Items.WHITE_BANNER;
                case "shield" -> Items.SHIELD;
                case "quiver" -> Items.ARROW;
                default -> new Item[]{Items.STONE_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD}[tier];
            };
        };
    }

    static Item forgeItem(String slot) {
        return switch (slot) {
            case "helm" -> Items.IRON_HELMET; case "chest" -> Items.IRON_CHESTPLATE; case "legs" -> Items.IRON_LEGGINGS; case "boots" -> Items.IRON_BOOTS;
            case "weapon" -> Items.IRON_SWORD; default -> Items.AMETHYST_SHARD;
        };
    }

    /** A relic as a vanilla item, by its role. */
    static Item relicItem(Spec.RelicsRow r) {
        return switch (r.role()) { case "ranged" -> Items.SPYGLASS; case "commander" -> Items.GOAT_HORN; case "healer" -> Items.GLISTERING_MELON_SLICE; default -> Items.BLAZE_POWDER; };
    }

    static String cap(String s) { return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }

    /** Splits a line into lines of at most width characters at spaces (a lore line is not wrapped by the client). */
    static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String w : Arrays.asList(text.split(" "))) {
            if (line.length() > 0 && line.length() + 1 + w.length() > width) { out.add(line.toString()); line.setLength(0); }
            if (line.length() > 0) line.append(' ');
            line.append(w);
        }
        if (line.length() > 0 || out.isEmpty()) out.add(line.toString());
        return out;
    }
}
