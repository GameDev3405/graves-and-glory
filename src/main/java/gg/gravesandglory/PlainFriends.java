package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** The War Table's Friends page as a plain player's chest menu (plain clients P4). STUB: to be built. */
final class PlainFriends {
    private PlainFriends() {}

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Friends"), 3, (m, pl) -> PlainWarTable.back(m, 0));
    }
}
