package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** The War Table's Gear page as a plain player's chest menu (plain clients P4). STUB: to be built. */
final class PlainGear {
    private PlainGear() {}

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Gear"), 3, (m, pl) -> PlainWarTable.back(m, 0));
    }
}
