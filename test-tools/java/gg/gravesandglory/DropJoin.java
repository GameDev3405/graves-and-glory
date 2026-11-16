package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import java.nio.file.*;
import java.util.Locale;

/**
 * The second client of the drop-board-watch suite (DropWatchChecks): a clone of the host's launch (EndingJoin.spawn) with
 * -Dgravesandglory.dropJoin=dir. It joins the host's LAN world (dir/address.txt), stands where the host puts it and writes what it is
 * shown of the host's drop to dir/watch.txt, one line a change: BEGIN, AIM, PATH, PHASE, END. It takes a shot on the rail, in the pegs,
 * at the flare and at the reveal, and stops when dir/done.txt appears. Never shipped.
 */
public final class DropJoin implements ClientModInitializer {
    int clock, stage, phaseTicks; boolean had; String lastPhase = ""; double lastAim = -1; DropClient.View seen;

    public void onInitializeClient() {
        String d = System.getProperty("gravesandglory.dropJoin", "");
        if (d.isEmpty()) return;
        Path dir = Path.of(d);
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try { tick(c, dir); } catch (Exception e) { log(dir, "exception " + e); e.printStackTrace(); }
        });
    }
    static void log(Path dir, String line) { EndingJoin.log(dir, "watch.txt", line); }
    static String f(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    void tick(MinecraftClient c, Path dir) throws Exception {
        clock++;
        c.options.pauseOnLostFocus = false;
        if (Files.exists(dir.resolve("done.txt"))) { c.scheduleStop(); return; }
        if (c.getOverlay() != null) return;
        switch (stage) {
            case 0 -> {
                if (!(c.currentScreen instanceof FrontMenuScreen) || clock < 60 || !Files.exists(dir.resolve("address.txt"))) return;
                String address = Files.readString(dir.resolve("address.txt")).trim();
                ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Drop watch", address, ServerInfo.ServerType.OTHER), false, null);
                stage = 1;
            }
            case 1 -> { if (c.player != null && GravesAndGloryClient.ready) { stage = 2; log(dir, "READY inCamp=" + GravesAndGloryClient.inCamp()); } }
            default -> watch(c, dir);
        }
    }

    void watch(MinecraftClient c, Path dir) {
        if (c.player == null) return;
        boolean placed = Files.exists(dir.resolve("placed.txt"));
        if (placed && c.currentScreen != null && seen == null && !had) c.setScreen(null); // whatever the camp opened: the court is to be seen
        DropClient.View v = null;
        for (var o : DropClient.views.values()) if (!o.mine) v = o;
        if (v == null) {
            if (had) { log(dir, "END altarDrawn=" + DropAltar.drawn + " views=" + DropClient.views.size()); had = false; seen = null; }
            return;
        }
        if (v != seen) {
            seen = v; had = true; lastPhase = ""; lastAim = -1;
            log(dir, "BEGIN name=" + v.name + " what=" + v.what + " mine=" + v.mine + " cinematic=" + DropClient.cinematic()
                + " screen=" + (c.currentScreen == null ? "none" : c.currentScreen.getClass().getSimpleName()) + " hud=" + !c.options.hudHidden);
        }
        if (!v.released && Math.abs(v.targetX - lastAim) > 0.05) { lastAim = v.targetX; log(dir, "AIM target=" + f(v.targetX) + " drawn=" + f(v.x)); }
        String p = v.phase(DropClient.now()).name();
        if (!p.equals(lastPhase)) {
            lastPhase = p; phaseTicks = 0;
            if (p.equals("DROP")) log(dir, "PATH slot=" + v.slot() + " from=" + f(v.path.xs()[0]) + " seconds=" + f(v.length()));
            log(dir, "PHASE " + p + " cinematic=" + DropClient.cinematic() + " riser=" + (v.riser != null));
            if (p.equals("REVEAL")) Phase1Checks.shot(c, "drop-watch-reveal");
        }
        phaseTicks++;
        if (p.equals("AIM") && phaseTicks == 35) Phase1Checks.shot(c, "drop-watch-aim");
        if (p.equals("DROP") && phaseTicks == 30) { Phase1Checks.shot(c, "drop-watch-pegs"); var at = v.coin(DropClient.now()); log(dir, "COIN x=" + f(at[0]) + " y=" + f(at[1])); }
        if (p.equals("FLARE") && phaseTicks == 6) Phase1Checks.shot(c, "drop-watch-flare");
    }
}
