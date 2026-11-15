package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

import java.nio.file.*;
import java.util.*;

/**
 * The second client of the ending-sync suite (EndingChecks.SyncSuite). The host's check clones its own launch command line into a second JVM (spawn) with its own
 * game directory and runtime, a different username and -Dgravesandglory.endingJoin=dir. This class is the second JVM's whole job: connect to the host's LAN world
 * (dir/address.txt), join the room whose code is in dir/code.txt, ready up, log each ending step change with the world tick and a wall clock (dir/join-steps.txt),
 * skip on from STATS (two skips: to REWARDS, then DONE), report the screen it lands on (dir/join-lobby.txt), and stop when dir/done.txt appears.
 */
public final class EndingJoin implements ClientModInitializer {
    /** How the last spawn was made, or why it failed (for the host's report line). */
    static String how = "";
    int clock, stage, stageTick, stepTicks, landTicks; MatchEnding.Step last; boolean sawRewards;

    public void onInitializeClient() {
        String d = System.getProperty("gravesandglory.endingJoin", "");
        if (d.isEmpty()) return;
        Path dir = Path.of(d);
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try { tick(c, dir); } catch (Exception e) { log(dir, "join-console-extra.txt", "exception " + e); e.printStackTrace(); }
        });
    }
    static void log(Path dir, String file, String line) {
        try { Files.writeString(dir.resolve(file), line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) {}
    }
    void tick(MinecraftClient c, Path dir) throws Exception {
        clock++;
        if (Files.exists(dir.resolve("done.txt"))) { c.scheduleStop(); return; }
        if (c.getOverlay() != null) return;
        switch (stage) {
            case 0 -> {
                if (!(c.currentScreen instanceof FrontMenuScreen) || clock < 60 || !Files.exists(dir.resolve("address.txt"))) return;
                String address = Files.readString(dir.resolve("address.txt")).trim();
                ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Ending sync", address, ServerInfo.ServerType.OTHER), false, null);
                stage = 1;
            }
            case 1 -> { if (c.player != null && GravesAndGloryClient.ready) stage = 2; }
            case 2 -> {
                if (!Files.exists(dir.resolve("code.txt"))) return;
                GravesAndGloryClient.send("social:join:" + Files.readString(dir.resolve("code.txt")).trim());
                stage = 3; stageTick = 0;
            }
            case 3 -> { if (++stageTick >= 60) { GravesAndGloryClient.send("social:ready"); stage = 4; } }
            default -> watch(c, dir);
        }
    }
    void watch(MinecraftClient c, Path dir) {
        var st = EndingClient.step();
        if (st != last && c.world != null) {
            log(dir, "join-steps.txt", (st == null ? "END" : st.name()) + " " + c.world.getTime() + " " + System.currentTimeMillis());
            last = st; stepTicks = 0;
            if (st == MatchEnding.Step.REWARDS) sawRewards = true;
        }
        stepTicks++;
        if (st == MatchEnding.Step.STATS && stepTicks == 15) EndingClient.skip(c);       // STATS -> REWARDS
        if (st == MatchEnding.Step.REWARDS && stepTicks == 8) EndingClient.skip(c);      // REWARDS -> DONE
        if (sawRewards && (st == null || st == MatchEnding.Step.DONE) && !Files.exists(dir.resolve("join-lobby.txt"))) {
            if (c.currentScreen instanceof CampScreen cs) log(dir, "join-lobby.txt", "CampScreen " + cs.page + " after " + landTicks + " ticks");
            else if (++landTicks > 200) log(dir, "join-lobby.txt", "no CampScreen after 200 ticks; screen " + (c.currentScreen == null ? "none" : c.currentScreen.getClass().getSimpleName()));
        }
    }

    /** Starts the second client as a clone of this JVM's command line. Returns null (how says why) when it cannot. */
    static Process spawn(Path dir) {
        try {
            String cl = ProcessHandle.current().info().commandLine().orElse(null);
            List<String> a;
            if (cl != null) a = parseWin(cl);
            else { // Windows JVMs often hide the command line from ProcessHandle: rebuild it from the runtime beans
                a = new ArrayList<>();
                a.add(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString());
                a.addAll(java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
                String[] cmd = System.getProperty("sun.java.command", "").split(" ");
                if (cmd.length == 0 || cmd[0].isEmpty()) { how = "neither ProcessHandle.commandLine() nor sun.java.command is available"; return null; }
                if (cmd[0].endsWith(".jar")) { a.add("-jar"); a.add(cmd[0]); }
                else { a.add("-cp"); a.add(System.getProperty("java.class.path")); a.add(cmd[0]); }
                a.addAll(Arrays.asList(cmd).subList(1, cmd.length));
                cl = String.join(" ", a) + " [rebuilt from RuntimeMXBean and sun.java.command]";
            }
            if (a.size() < 3) { how = "command line did not parse: " + cl; return null; }
            Map<String, String> props = new LinkedHashMap<>();
            props.put("-Dgravesandglory.runtime=", dir.resolve("runtime").toString());
            props.put("-Dgravesandglory.localChecks=", dir.resolve("local-checks.txt").toString());
            props.put("-Dgravesandglory.phase4Checks=", "");
            props.put("-Dgravesandglory.endingJoin=", dir.toString());
            props.put("-Dgravesandglory.phase1Checks=", "");
            Set<String> seen = new HashSet<>();
            boolean gameDir = false, user = false;
            for (int i = 1; i < a.size(); i++) {
                String s = a.get(i);
                for (var e : props.entrySet()) if (s.startsWith(e.getKey())) { a.set(i, e.getKey() + e.getValue()); seen.add(e.getKey()); }
                if (s.equals("--gameDir") && i + 1 < a.size()) { a.set(i + 1, dir.resolve("run").toString()); gameDir = true; }
                if (s.equals("--username") && i + 1 < a.size()) { a.set(i + 1, "EndJoiner"); user = true; }
                if (s.equals("--uuid") && i + 1 < a.size()) a.set(i + 1, "00000000-0000-0000-0000-00000000e0d1");
            }
            // Loom's launch has no --gameDir: the game directory is the working directory, which is set to dir/run below.
            for (var e : props.entrySet()) if (!seen.contains(e.getKey())) a.add(1, e.getKey() + e.getValue());
            if (!user) { a.add("--username"); a.add("EndJoiner"); }
            Path opts = dir.resolve("run").resolve("options.txt");
            if (!Files.exists(opts)) Files.writeString(opts, "pauseOnLostFocus:false\nonboardAccessibility:false\nrenderDistance:6\nsimulationDistance:5\nguiScale:2\nresourcePacks:[\"fabric\",\"gravesandglory:armory\"]\n");
            ProcessBuilder pb = new ProcessBuilder(a).directory(dir.resolve("run").toFile()).redirectErrorStream(true).redirectOutput(dir.resolve("join-console.log").toFile());
            Process p = pb.start();
            how = "cloned the host launch (" + a.size() + " arguments, " + (gameDir ? "--gameDir swapped" : "game dir = working dir") + "), game dir " + dir.resolve("run") + ", log " + dir.resolve("join-console.log");
            return p;
        } catch (Exception e) { how = "spawn failed: " + e; return null; }
    }

    /** The Windows command-line rules (CommandLineToArgvW): backslashes only count before a quote. */
    static List<String> parseWin(String s) {
        List<String> out = new ArrayList<>(); StringBuilder cur = new StringBuilder(); boolean in = false, any = false; int i = 0, n = s.length();
        while (i < n) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                int j = i; while (j < n && s.charAt(j) == '\\') j++;
                int bs = j - i;
                if (j < n && s.charAt(j) == '"') { cur.append("\\".repeat(bs / 2)); if (bs % 2 == 1) { cur.append('"'); i = j + 1; } else i = j; }
                else { cur.append("\\".repeat(bs)); i = j; }
                any = true; continue;
            }
            if (ch == '"') { in = !in; any = true; i++; continue; }
            if (Character.isWhitespace(ch) && !in) { if (any) { out.add(cur.toString()); cur.setLength(0); any = false; } i++; continue; }
            cur.append(ch); any = true; i++;
        }
        if (any) out.add(cur.toString());
        return out;
    }
}
