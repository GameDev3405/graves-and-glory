package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import java.io.File;
import java.nio.file.*;

/**
 * The second client of the drop-film-watch suite (DropFilmWatchChecks): a clone of the host's launch with -Dgravesandglory.dropFilmJoin=dir.
 * It joins the host's LAN world, hides its HUD (the first-person hand goes with it), and for each attempt the host announces
 * (dir/attempt.txt) it writes a video of what it is shown (out/watch-tryN.mp4): ready-N.txt when frames are being written, watch-N.done
 * when the view has ended and a second or two have passed. Stops when dir/done.txt appears. Never shipped.
 */
public final class DropFilmJoin implements ClientModInitializer {
    int clock, stage, seenAttempt, current, tail, waited; boolean had, fovSet; File outDir;
    DropClient.View seen; int slot = -1; String rarity = "";

    public void onInitializeClient() {
        String d = System.getProperty("gravesandglory.dropFilmJoin", "");
        if (d.isEmpty()) return;
        Path dir = Path.of(d);
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try { tick(c, dir); } catch (Exception e) { EndingJoin.log(dir, "watch.txt", "exception " + e); e.printStackTrace(); }
        });
    }

    void tick(MinecraftClient c, Path dir) throws Exception {
        clock++;
        c.options.pauseOnLostFocus = false;
        if (Files.exists(dir.resolve("done.txt"))) { DropFilm.stop(); c.scheduleStop(); return; }
        if (clock == 5 || clock == 100) DropFilm.prepare(c, DropFilm.envInt("GRAVES_FILM_WW", 1920), DropFilm.envInt("GRAVES_FILM_WH", 1080), 60);
        if (c.getOverlay() != null) return;
        switch (stage) {
            case 0 -> {
                if (!(c.currentScreen instanceof FrontMenuScreen) || clock < 60 || !Files.exists(dir.resolve("address.txt"))) return;
                String address = Files.readString(dir.resolve("address.txt")).trim();
                ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Drop film", address, false), false);
                stage = 1;
            }
            case 1 -> { if (c.player != null && GravesAndGloryClient.ready) stage = 2; }
            default -> watch(c, dir);
        }
    }

    void finishTake(Path dir, boolean ok, String why) {
        String stats = DropFilm.stop();
        try { Files.writeString(dir.resolve("watch-" + current + ".done"), "ok=" + ok + " slot=" + slot + " rarity=" + rarity + " why=" + why.replace(' ', '_') + " " + stats.replace(' ', '_')); } catch (Exception ignored) {}
        current = 0; had = false; seen = null; slot = -1; rarity = ""; tail = 0; waited = 0;
    }

    void watch(MinecraftClient c, Path dir) throws Exception {
        if (c.player == null) return;
        if (Files.exists(dir.resolve("placed.txt"))) {
            c.options.hudHidden = true; // the HUD goes, and with it the first-person hand
            if (c.currentScreen != null && seen == null && !had) c.setScreen(null);
            if (!fovSet) { c.options.getFov().setValue((int) DropFilmWatchChecks.env("GRAVES_FILM_WFOV", 80)); fovSet = true; }
        }
        if (outDir == null && Files.exists(dir.resolve("out.txt"))) outDir = new File(Files.readString(dir.resolve("out.txt")).trim());
        // a new attempt: open the file, tell the host once frames are being written
        if (current == 0 && clock % 5 == 0 && outDir != null && Files.exists(dir.resolve("attempt.txt")) && Files.exists(dir.resolve("placed.txt"))) {
            int n; try { n = Integer.parseInt(Files.readString(dir.resolve("attempt.txt")).trim()); } catch (NumberFormatException e) { n = 0; }
            if (n > seenAttempt) { seenAttempt = n; current = n; DropFilm.start(c, new File(outDir, "watch-try" + n + ".mp4"), 60, 90); }
        }
        if (current == 0) return;
        if (DropFilm.live && !Files.exists(dir.resolve("ready-" + current + ".txt"))) Files.writeString(dir.resolve("ready-" + current + ".txt"), "live");
        DropClient.View v = null;
        for (var o : DropClient.views.values()) if (!o.mine) v = o;
        if (v != null) { seen = v; had = true; if (v.slot() >= 0) { slot = v.slot(); rarity = DropRender.rarity(v); } }
        else if (had) { // the view is over: two more seconds of the court, then close
            if (++tail > 40) finishTake(dir, true, "view ended");
        } else if (Files.exists(dir.resolve("ready-" + current + ".txt")) && ++waited > 1500) finishTake(dir, false, "no view within 75 s");
    }
}
