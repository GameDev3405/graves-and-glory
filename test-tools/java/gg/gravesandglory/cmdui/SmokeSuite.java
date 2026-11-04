package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import net.minecraft.client.MinecraftClient;

/** Proves the command UI harness itself: the runner loads a suite, seats the player and starts a real round. Never included in releases. */
public final class SmokeSuite implements CommandUiChecks.Suite {
    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) CommandUiChecks.seatAndStart(c);
        if (tick == 40) CommandUiChecks.report("cu-smoke-battle", GravesAndGloryClient.battleActive(), "battleActive after seatAndStart");
        if (tick == 60) CommandUiChecks.finish(c);
    }
    @Override public void cleanup(MinecraftClient c) {}
}
