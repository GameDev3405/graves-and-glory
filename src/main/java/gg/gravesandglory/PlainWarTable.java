package gg.gravesandglory;

import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The War Table's home as a plain player's chest menu (plain clients P4, plan decision 5): a hub to every page and the home page's
 * own two rides (practice, the next Long March battle). Each page is its own PlainMenu (Plain* classes); every press sends the
 * modpack page's own command, so the rules and refusals are the modpack's.
 */
final class PlainWarTable {
    private PlainWarTable() {}

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("War Table").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 3, PlainWarTable::draw);
    }

    /** A camp station used by a plain player (PlainClients.station): its War Table page. */
    static void station(ServerPlayerEntity p, String station) {
        switch (station) {
            case "contracts" -> PlainContracts.open(p);
            case "chapter" -> PlainChapter.open(p);
            case "friends" -> PlainFriends.open(p);
            case "forge" -> PlainGear.open(p);
            default -> open(p);
        }
    }

    /** The Long March battle the home page rides to: the one after the last cleared, or the last once all are cleared (WarTableHome.next). */
    static Spec.CampaignBattlesRow next(Profile pr) {
        for (var row : Spec.CAMPAIGNBATTLES) if (row.order() == pr.campaignCleared + 1) return row;
        return Spec.CAMPAIGNBATTLES[Spec.CAMPAIGNBATTLES.length - 1];
    }

    static void draw(PlainMenu m, ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        Profile pr = b.campaign.profile(p.getUuid());
        page(m, 1, Items.WRITABLE_BOOK, "Recruit", "Enlist and star troops.", pl -> PlainRecruit.open(pl, null));
        page(m, 2, Items.WHITE_BANNER, "Banner", "Lay out your army on the war table.", PlainBanner::open);
        page(m, 3, Items.PAPER, "Contracts", "Daily and weekly tasks.", PlainContracts::open);
        page(m, 4, Items.NETHER_STAR, "Chapter", "Your progress track and titles.", PlainChapter::open);
        page(m, 5, Items.PLAYER_HEAD, "Friends", "Party, room and invites.", PlainFriends::open);
        page(m, 6, Items.FILLED_MAP, "Field", "Battlefield and bots.", PlainField::open);
        page(m, 7, Items.IRON_CHESTPLATE, "Gear", "Arsenal, relics and the forge.", PlainGear::open);
        page(m, 15, Items.WRITTEN_BOOK, "Results", "The last battle's report.", PlainResults::open);
        m.command(11, PlainMenu.named(Items.COMPASS, "Practice battle", Formatting.GREEN, "A battle against one bot, in camp."), "camp:practice");
        var row = next(pr);
        m.command(13, PlainMenu.named(Items.GOLDEN_SWORD, "Long March: " + row.name(), Formatting.GOLD, "Ride to the next campaign battle.", "Played solo."), "campaign:" + row.id());
        m.icon(22, PlainMenu.named(Items.GOLD_NUGGET, "Purse", Formatting.YELLOW, "Writs " + pr.troopWrits + "  Pacts " + pr.pacts, "Wins " + pr.wins + " of " + pr.matches));
    }

    static void page(PlainMenu m, int slot, Item item, String name, String what, java.util.function.Consumer<ServerPlayerEntity> open) {
        m.button(slot, PlainMenu.named(item, name, Formatting.GOLD, what), open);
    }

    /** A page's top-left way back to home. */
    static void back(PlainMenu m, int slot) { m.button(slot, PlainMenu.back(), PlainWarTable::open); }

    /** A one-line text icon. */
    static void note(PlainMenu m, int slot, String line, List<String> lore) {
        m.icon(slot, PlainMenu.named(Items.PAPER, line, Formatting.WHITE, lore.toArray(String[]::new)));
    }
}
