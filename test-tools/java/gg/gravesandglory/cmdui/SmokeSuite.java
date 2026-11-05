package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Proves the command UI harness itself: the runner loads a suite, seats the player, starts a real round and presses a key for exactly one input tick. Never included in releases. */
public final class SmokeSuite implements CommandUiChecks.Suite {
    /** A binding no game code reads, so pressing it changes nothing. */
    static KeyBinding probe;
    static volatile boolean watching;
    static volatile int pressedTicks, wasPressedCount;
    static boolean hooked;

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) {
            if (probe == null) probe = new KeyBinding("key.gravesandglory.cu_smoke_probe", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F20, "key.categories.misc");
            if (!hooked) { // the start of a client tick is where the game's own input handling reads the bindings
                hooked = true;
                ClientTickEvents.START_CLIENT_TICK.register(cl -> {
                    if (!watching) return;
                    if (probe.isPressed()) pressedTicks++;
                    while (probe.wasPressed()) wasPressedCount++;
                });
            }
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) CommandUiChecks.report("cu-smoke-battle", GravesAndGloryClient.battleActive(), "battleActive after seatAndStart");
        if (tick == 50) { pressedTicks = 0; wasPressedCount = 0; watching = true; CommandUiChecks.press(c, probe); }
        if (tick == 56) {
            watching = false;
            CommandUiChecks.report("cu-smoke-press", pressedTicks == 1 && wasPressedCount == 1 && !probe.isPressed(),
                "isPressed seen in " + pressedTicks + " input tick(s), wasPressed fired " + wasPressedCount + " time(s), released=" + !probe.isPressed());
        }
        if (tick == 60) CommandUiChecks.finish(c);
    }
    @Override public void cleanup(MinecraftClient c) { watching = false; }
}
