package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** The War Table's Armoury page as a plain player's chest menu (plain clients P4). STUB: to be built. */
final class PlainArmoury {
    private PlainArmoury() {}

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Armoury"), 3, (m, pl) -> PlainWarTable.back(m, 0));
    }
}
