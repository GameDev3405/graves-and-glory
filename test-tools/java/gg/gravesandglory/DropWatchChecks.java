package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.GameMode;

import java.nio.file.*;
import java.util.*;

/**
 * Two real clients at the drop board. GRAVES_PHASE1_CHECKS=drop-board-watch. The host opens its world to LAN and starts a second
 * client (DropJoin, a clone of its own launch); both stand on the court; the host rolls a unit, aims twice and lets go. The second
 * client must be shown the coin on the rail following the aim, the same path into the same slot, the flare and the reveal, with no
 * cinematic or screen of its own, and its view must end. The host's profile is put back. Never shipped.
 */
final class DropWatchChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    volatile UUID host, visitor; volatile String backup; volatile boolean placed, failed, restored;
    volatile Path dir; volatile Process joiner; int stage; long at;
    int go, aimTicks, overAt = -1, hostSlot = -1; String last = ""; double hostFrom = -1;
    final List<String> phases = new ArrayList<>();
    boolean done;

    static void say(String id, boolean ok, String why) { Phase1Checks.report("p1-drop-watch-" + id, ok, why); }

    @Override public void cleanup(MinecraftClient c) {
        try { if (dir != null) Files.writeString(dir.resolve("done.txt"), "done"); } catch (Exception ignored) {}
        if (joiner != null && joiner.isAlive()) try { joiner.waitFor(45, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        if (joiner != null && joiner.isAlive()) { say("joiner-exit", false, "the second client did not stop within 45 s of done.txt; stopping our own child process"); joiner.destroy(); }
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
                dir = d;
                int port; try (var socket = new java.net.ServerSocket(0)) { port = socket.getLocalPort(); }
                boolean lan = b.server.openToLan(GameMode.ADVENTURE, false, port);
                say("lan", lan, "integrated server open to LAN on " + port);
                if (!lan) { failed = true; return; }
                Files.writeString(d.resolve("address.txt"), "127.0.0.1:" + port);
                joiner = EndingJoin.spawn(d, "-Dgravesandglory.dropJoin=", "DropWatcher");
                say("spawn", joiner != null && joiner.isAlive(), "second client: " + EndingJoin.how);
                if (joiner == null) { failed = true; return; }
                stage = 1; at = b.time;
            } else if (stage == 1) {
                for (var o : b.server.getPlayerManager().getPlayerList()) if (!o.getUuid().equals(host)) visitor = o.getUuid();
                if (visitor == null) { if (b.time - at > 9000) { say("joined", false, "the second client never connected within 9000 ticks (alive=" + joiner.isAlive() + ")"); failed = true; } return; }
                say("joined", true, "second client connected as " + b.server.getPlayerManager().getPlayer(visitor).getGameProfile().getName() + " after " + (b.time - at) + " ticks");
                stage = 2; at = b.time;
            } else if (stage == 2 && b.time - at > 200) { // the visitor's camp has loaded: both onto the court, side by side
                var v = b.server.getPlayerManager().getPlayer(visitor);
                if (v == null) { say("placed", false, "the second client left"); failed = true; return; }
                var hp = DropStage.at(-2, 2, 1); var vp = DropStage.at(2, 3, 1);
                p.teleport(b.world, hp.getX() + 0.5, hp.getY(), hp.getZ() + 0.5, DropLayout.viewYaw(), -20);
                v.teleport(b.world, vp.getX() + 0.5, vp.getY(), vp.getZ() + 0.5, DropLayout.viewYaw(), -20);
                Files.writeString(dir.resolve("placed.txt"), "placed");
                say("placed", v.getWorld() == b.world, "both on the court; the watcher " + String.format(Locale.ROOT, "%.1f", Math.sqrt(v.squaredDistanceTo(p))) + " blocks from the roller");
                placed = true;
            }
        } catch (Exception e) { say("exception", false, e.toString()); e.printStackTrace(); failed = true; }
    }

    List<String> lines() { try { return Files.readAllLines(dir.resolve("watch.txt")); } catch (Exception e) { return List.of(); } }
    static String field(String line, String key) { for (String w : line.split(" ")) if (w.startsWith(key + "=")) return w.substring(key.length() + 1); return ""; }
    static double num(String s) { try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; } }

    public void step(MinecraftClient c, int tick) {
        if (done) return;
        if (tick == 20) { c.setScreen(null); host = c.player.getUuid(); }
        if (failed) { done = true; Phase1Checks.finish(c, "drop-board-watch"); return; }
        if (!placed) return;
        go++;
        if (go == 100) GravesAndGloryClient.send("drop:roll:" + Spec.HOUSES[0].id());
        if (go < 100) return;
        String p = DropClient.phase();
        if (!p.equals(last)) { last = p; if (!p.isEmpty()) phases.add(p); }
        if (p.equals("AIM")) {
            int a = ++aimTicks;
            if (a == 10) DropClient.aim(2.0);
            if (a == 40) DropClient.aim(6.5);
            if (a == 80) DropClient.release();
        }
        if (DropClient.mine != null && DropClient.mine.path != null && hostSlot < 0) { hostSlot = DropClient.mine.slot(); hostFrom = DropClient.mine.path.xs()[0]; }
        if (overAt < 0 && go > 140 && !DropClient.cinematic()) overAt = go;
        if (overAt < 0 && go < 900) return;
        if (overAt < 0) overAt = go;
        var w = lines();
        boolean ended = w.stream().anyMatch(l -> l.startsWith("END"));
        if (!ended && go - overAt < 200) return; // the watcher's view may outlive the roller's return a little
        String name = c.player.getGameProfile().getName();
        String begin = w.stream().filter(l -> l.startsWith("BEGIN")).findFirst().orElse("");
        say("begin", !begin.isEmpty() && field(begin, "name").equals(name) && field(begin, "mine").equals("false") && field(begin, "cinematic").equals("false") && field(begin, "screen").equals("none"),
            "the watcher was shown " + (begin.isEmpty() ? "nothing" : begin) + " (roller " + name + ")");
        List<String> aims = new ArrayList<>(); for (String l : w) if (l.startsWith("AIM")) aims.add(field(l, "target"));
        boolean two = aims.stream().anyMatch(s -> Math.abs(num(s) - 2.0) < 0.3), six = !aims.isEmpty() && Math.abs(num(aims.get(aims.size() - 1)) - 6.5) < 0.3;
        say("aim", two && six, "the roller aimed at 2.0 then 6.5; the watcher's coin was sent to " + aims);
        String path = w.stream().filter(l -> l.startsWith("PATH")).findFirst().orElse("");
        boolean same = !path.isEmpty() && hostSlot >= 0 && field(path, "slot").equals("" + hostSlot) && Math.abs(num(field(path, "from")) - hostFrom) < 0.01;
        say("path", same, "the roller's coin fell from " + String.format(Locale.ROOT, "%.2f", hostFrom) + " into slot " + hostSlot + "; the watcher's: " + (path.isEmpty() ? "no path" : path));
        List<String> seen = new ArrayList<>(); boolean own = false;
        for (String l : w) if (l.startsWith("PHASE")) { seen.add(l.split(" ")[1]); own |= field(l, "cinematic").equals("true"); }
        say("phases", seen.containsAll(List.of("AIM", "DROP", "FLARE", "REVEAL")) && !own, "the watcher's view went through " + seen + ", " + (own ? "with" : "without") + " a cinematic of its own; the roller's " + phases);
        String end = w.stream().filter(l -> l.startsWith("END")).findFirst().orElse("");
        long thrown = w.stream().filter(l -> l.startsWith("exception")).count();
        say("end", ended && field(end, "views").equals("0") && num(field(end, "altarDrawn")) > 0 && thrown == 0, "the watcher's view " + (ended ? "ended: " + end : "never ended") + "; exceptions " + thrown);
        done = true;
        Phase1Checks.finish(c, "drop-board-watch");
    }
}
