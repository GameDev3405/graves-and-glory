package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.GameMode;

import java.io.File;
import java.nio.file.*;
import java.util.*;

/**
 * Films the drop board from a watcher's side (Kort, 2026-11-14; one shoot only). GRAVES_PHASE1_CHECKS=drop-film-watch. The host opens to
 * LAN and starts a second client (DropFilmJoin, a clone of its own launch); both stand on the court, the watcher a few blocks from the
 * roller facing the altar, HUD hidden. The host rolls a unit (aims at 2.0, then 6.5, lets go); the second client writes the video of
 * what it is shown. The roll is repeated (harness only) until the coin lands in x5 or x10 or the result is Rare or better, up to
 * GRAVES_FILM_TRIES times; the best take is kept. The watcher's spot is GRAVES_FILM_WSIDE/WFRONT/WUP (blocks), WPITCH, WFOV.
 * Never shipped.
 */
final class DropFilmWatchChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    volatile UUID host, visitor; volatile String backup; volatile boolean placed, failed, restored;
    volatile Path dir; volatile Process joiner; int stage; long at;
    int go, attempt, aimTicks, tail, best = -1, bestAttempt, state, stateAt; String last = "", bestNote = "";
    DropClient.View lastView;
    boolean done;
    final File out = new File(System.getenv().getOrDefault("GRAVES_FILM_OUT", System.getProperty("gravesandglory.repoRoot", ".") + "/work/media/dropboard"));
    static final int TRIES = DropFilm.envInt("GRAVES_FILM_TRIES", 10), WANT = DropFilm.envInt("GRAVES_FILM_WANT", 5);

    static void say(String id, boolean ok, String why) { Phase1Checks.report("p1-drop-film-watch-" + id, ok, why); }
    static double env(String k, double d) { try { return Double.parseDouble(System.getenv().getOrDefault(k, "" + d)); } catch (NumberFormatException e) { return d; } }

    @Override public void cleanup(MinecraftClient c) {
        try { if (dir != null) Files.writeString(dir.resolve("done.txt"), "done"); } catch (Exception ignored) {}
        if (joiner != null && joiner.isAlive()) try { joiner.waitFor(60, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (joiner != null && joiner.isAlive()) { say("joiner-exit", false, "the second client did not stop within 60 s of done.txt; stopping our own child process"); joiner.destroy(); }
        if (!restored && host != null && backup != null && c.getServer() != null) try { c.getServer().submit(() -> {
            var b = GravesAndGlory.battle; if (b == null) return;
            b.campaign.profiles.put(host, Campaign.JSON.fromJson(backup, Profile.class)); b.campaign.save(host); restored = true;
        }).join(); } catch (RuntimeException e) { say("restore", false, e.toString()); }
    }

    public void serverTick(GravesAndGlory.Battle b) {
        if (host == null || failed || placed) return;
        try {
            var p = b.server.getPlayerManager().getPlayer(host); if (p == null) return;
            if (stage == 0) {
                if (!b.ready) return;
                backup = Campaign.JSON.toJson(b.campaign.profile(host));
                var pr = b.campaign.profile(host); pr.troopWrits = 10; b.campaign.save(host);
                Path d = Path.of(System.getProperty("gravesandglory.runtime")).toAbsolutePath().getParent().resolve("drop-join");
                if (Files.exists(d)) try (var s = Files.list(d)) { for (Path f : (Iterable<Path>) s::iterator) if (Files.isRegularFile(f)) Files.delete(f); }
                Files.createDirectories(d.resolve("run")); Files.createDirectories(d.resolve("runtime"));
                dir = d; out.mkdirs();
                Files.writeString(d.resolve("out.txt"), out.getAbsolutePath());
                int port; try (var socket = new java.net.ServerSocket(0)) { port = socket.getLocalPort(); }
                boolean lan = b.server.openToLan(GameMode.ADVENTURE, false, port);
                say("lan", lan, "integrated server open to LAN on " + port);
                if (!lan) { failed = true; return; }
                Files.writeString(d.resolve("address.txt"), "127.0.0.1:" + port);
                joiner = EndingJoin.spawn(d, "-Dgravesandglory.dropFilmJoin=", "DropWatcher");
                say("spawn", joiner != null && joiner.isAlive(), "second client: " + EndingJoin.how);
                if (joiner == null) { failed = true; return; }
                stage = 1; at = b.time;
            } else if (stage == 1) {
                for (var o : b.server.getPlayerManager().getPlayerList()) if (!o.getUuid().equals(host)) visitor = o.getUuid();
                if (visitor == null) { if (b.time - at > 9000) { say("joined", false, "the second client never connected within 9000 ticks (alive=" + joiner.isAlive() + ")"); failed = true; } return; }
                say("joined", true, "second client connected after " + (b.time - at) + " ticks");
                stage = 2; at = b.time;
            } else if (stage == 2 && b.time - at > 300) {
                var v = b.server.getPlayerManager().getPlayer(visitor);
                if (v == null) { say("placed", false, "the second client left"); failed = true; return; }
                var hp = DropStage.at((int) env("GRAVES_FILM_RSIDE", -2), 2, 1);
                var vp = DropStage.at((int) env("GRAVES_FILM_WSIDE", 2), (int) env("GRAVES_FILM_WFRONT", 6), (int) env("GRAVES_FILM_WUP", 1));
                p.teleport(b.world, hp.getX() + 0.5, hp.getY(), hp.getZ() + 0.5, DropLayout.viewYaw(), -20);
                v.teleport(b.world, vp.getX() + 0.5, vp.getY(), vp.getZ() + 0.5, DropLayout.viewYaw(), (float) env("GRAVES_FILM_WPITCH", -25));
                Files.writeString(dir.resolve("placed.txt"), "placed");
                say("placed", v.getWorld() == b.world, "both on the court; the watcher " + String.format(Locale.ROOT, "%.1f", Math.sqrt(v.squaredDistanceTo(p))) + " blocks from the roller");
                placed = true;
            }
        } catch (Exception e) { say("exception", false, e.toString()); e.printStackTrace(); failed = true; }
    }

    String read(String name) { try { return Files.readString(dir.resolve(name)).trim(); } catch (Exception e) { return ""; } }
    boolean has(String name) { return Files.exists(dir.resolve(name)); }
    static String field(String s, String key) { for (String w : s.split(" ")) if (w.startsWith(key + "=")) return w.substring(key.length() + 1); return ""; }
    void to(int tick, int s) { state = s; stateAt = go; last = ""; aimTicks = 0; tail = 0; }

    public void step(MinecraftClient c, int tick) {
        if (tick == 2) { c.options.getEnableVsync().setValue(false); c.options.getMaxFps().setValue(20); c.options.pauseOnLostFocus = false; c.options.getViewDistance().setValue(4);
            long h = c.getWindow().getHandle(); org.lwjgl.glfw.GLFW.glfwSetWindowSize(h, 480, 270); org.lwjgl.glfw.GLFW.glfwSetWindowPos(h, 2000, 1100); } // the host's own window is not filmed: small and slow, the GPU is the watcher's
        if (done) return;
        if (tick == 20) { c.setScreen(null); host = c.player.getUuid(); }
        if (failed) { done = true; Phase1Checks.finish(c, "drop-film-watch"); return; }
        if (!placed) return;
        go++;
        int k = go - stateAt;
        switch (state) {
            case 0 -> { if (go > 60) to(go, 1); } // the camp settles
            case 1 -> { // top up, ask the watcher to start a file
                attempt++;
                Phase1Checks.server(c, b -> { var pr = b.campaign.profile(host); pr.troopWrits = 10; b.campaign.save(host); });
                try { Files.writeString(dir.resolve("attempt.txt"), "" + attempt); } catch (Exception e) { say("attempt", false, e.toString()); failed = true; }
                to(go, 2);
            }
            case 2 -> { // waiting for the watcher's file to go live
                if (has("ready-" + attempt + ".txt")) to(go, 6);
                else if (k > 1200) { say("ready", false, "the watcher never started its file"); failed = true; }
            }
            case 6 -> { // a moment of the empty court, then the roll
                if (k > 40) {
                    GravesAndGloryClient.send("drop:roll:" + Spec.HOUSES[0].id());
                    to(go, 3);
                }
            }
            case 3 -> { // the roller's side of the drop; the watcher films it
                String p = DropClient.phase();
                if (!p.equals(last)) last = p;
                if (p.equals("AIM")) {
                    int a = ++aimTicks;
                    if (a == 12) DropClient.aim(2.0);
                    if (a == 45) DropClient.aim(6.5);
                    if (a == 80) DropClient.release();
                }
                if (DropClient.mine != null && DropClient.mine.path != null) lastView = DropClient.mine;
                boolean over = k > 20 && !DropClient.cinematic();
                if (over && ++tail > 40) to(go, 4);
                if (k > 1000) { say("roll", false, "the roll never ended"); failed = true; }
            }
            case 4 -> { // the watcher closes its file; judge the take
                if (!has("watch-" + attempt + ".done")) { if (k > 1200) { say("watch-done", false, "the watcher never finished attempt " + attempt); failed = true; } return; }
                String res = read("watch-" + attempt + ".done");
                int sc = lastView == null ? 0 : DropFilmChecks.score(lastView);
                File f = new File(out, "watch-try" + attempt + ".mp4");
                String note = "attempt " + attempt + " slot " + (lastView == null ? "?" : DropFilmChecks.MULT[lastView.slot()]) + " rarity " + (lastView == null ? "?" : DropRender.rarity(lastView)) + " score " + sc + "; " + res;
                System.out.println("[p1] drop-film-watch: " + note);
                boolean ok = field(res, "ok").equals("true") && f.exists();
                java.util.regex.Matcher sm = java.util.regex.Pattern.compile("\"slow\":\"([^\"]*)\"").matcher(res);
                if (sm.find()) for (String e : sm.group(1).split("_")) { String[] kv = e.split("="); if (kv.length == 2 && Integer.parseInt(kv[0]) > 45 && Integer.parseInt(kv[1]) > 100) ok = false; } // a stall after the start is a stutter in the take
                if (ok && sc > best) { if (bestAttempt > 0) { new File(out, "watch-try" + bestAttempt + ".mp4").delete(); new File(out, "watch-try" + bestAttempt + ".mp4.json").delete(); } best = sc; bestAttempt = attempt; bestNote = note; }
                else { f.delete(); new File(f.getPath() + ".json").delete(); }
                lastView = null;
                if (best >= WANT || attempt >= TRIES) to(go, 5); else to(go, 1);
            }
            case 5 -> {
                File f = new File(out, "watch-try" + bestAttempt + ".mp4"), fin = new File(out, "watcher-unit.mp4");
                boolean ok = bestAttempt > 0 && f.exists();
                if (ok) { fin.delete(); new File(fin.getPath() + ".json").delete(); ok = f.renameTo(fin); new File(f.getPath() + ".json").renameTo(new File(fin.getPath() + ".json")); }
                say("take", ok, "kept " + (ok ? fin.getName() : "nothing") + " after " + attempt + " attempt(s): " + bestNote + (best >= WANT ? " (picked for a high slot or a Rare+ result)" : " (best of the tries, not a high one)"));
                try { Files.writeString(new File(out, "watcher-picks.txt").toPath(), bestNote); } catch (Exception ignored) {}
                done = true;
                Phase1Checks.finish(c, "drop-film-watch");
            }
            default -> {}
        }
    }
}
