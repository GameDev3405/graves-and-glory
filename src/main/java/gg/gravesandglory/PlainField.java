package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The War Table's Field page as a plain player's chest menu (plain clients P4, plan decision 5). As on the modpack page, the maps are
 * shown, not picked (the war council host picks the map); the one press is the bots' tier (difficulty:), which the server allows only
 * while no battle runs and only to the host. The Statuses glossary sits under the bots.
 */
final class PlainField {
    private PlainField() {}

    /** The bot tiers the modpack Field page offers (WarTablePages.TIER_IDS). */
    static final String[] TIERS = {"easy", "normal", "hard"};

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Field").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 5, PlainField::draw);
    }

    static String tierName(String id) { return id.isEmpty() ? id : Character.toUpperCase(id.charAt(0)) + id.substring(1); }

    static void draw(PlainMenu m, ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        var c = b.campaign;
        PlainWarTable.back(m, 0);
        String status = b.running ? "A battle is on: maps wait until it ends" : "The war council host picks the map";
        PlainWarTable.note(m, 4, status, List.of("Open Friends, then the war council, to pick a map."));
        // the maps, row 2 (and on into row 3 past nine)
        for (int i = 0; i < Spec.MAPS.length && i < 18; i++) {
            var map = Spec.MAPS[i];
            boolean built = i == c.map;
            List<String> lore = new ArrayList<>(PlainBanner.wrap(map.description(), 40));
            if (built) lore.add("The battlefield standing now");
            m.icon(9 + i, PlainBanner.glint(PlainBanner.item(built ? Items.FILLED_MAP : Items.MAP, map.name(), built ? Formatting.GOLD : Formatting.WHITE, lore), built));
        }
        // the bots
        m.icon(27, PlainBanner.item(Items.ZOMBIE_HEAD, "Bots: " + tierName(c.difficulty), Formatting.GOLD, List.of("How hard the bots fight.")));
        Item[] icons = {Items.WOODEN_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD};
        for (int i = 0; i < TIERS.length; i++) {
            String id = TIERS[i];
            boolean on = id.equals(c.difficulty);
            var stack = PlainBanner.glint(PlainBanner.item(icons[i], tierName(id), on ? Formatting.GOLD : Formatting.WHITE,
                b.running ? List.of(on ? "Chosen" : "", "A battle is on: the bots wait until it ends") : List.of(on ? "Chosen" : "Click to choose")), on);
            if (b.running) m.icon(29 + i, stack); else m.command(29 + i, stack, "difficulty:" + id);
        }
        // the Statuses glossary
        m.icon(36, PlainBanner.item(Items.BOOK, "Statuses", Formatting.GOLD, List.of("What the marks over a troop mean.")));
        Item[] marks = {Items.SLIME_BALL, Items.GOLD_NUGGET, Items.GRAY_DYE, Items.REDSTONE, Items.TARGET, Items.PISTON};
        int k = 0;
        for (var e : Statuses.Glossary.entries()) {
            if (k >= 8) break;
            Item it = k < marks.length ? marks[k] : Items.PAPER;
            m.icon(37 + k, PlainBanner.item(it, e.name(), Formatting.WHITE, PlainBanner.wrap(e.line(), 40)));
            k++;
        }
    }
}
