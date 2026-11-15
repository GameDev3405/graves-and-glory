package gg.gravesandglory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;

/**
 * Unshipped performance baseline probe for a dedicated server with real clients (plan task 3.6).
 * -Dgravesandglory.perfProbe=<dir>  writes <dir>/server-windows.csv (one row per 100 ticks), <dir>/server-battles.csv (one row per phase)
 * and <dir>/server-builds.csv (map build wall time and ticks, from "arena not ready" to "ready").
 * -Dgravesandglory.perfBattleSeconds=<n>  ends each battle after n seconds of server time (draw), so the lobby can pick the next map.
 * Read-only on the game apart from that optional finish call.
 */
public final class PerfProbe implements ModInitializer {
    static final int WINDOW = 100;
    long tickStart, worldStart, worldNs, endStart;
    /** Runs before every mod's END_SERVER_TICK listener (and this probe's own runs after them), so a first-ticks row splits the tick into world ticks, mod end-of-tick listeners and the rest (network, players, autosave). */
    static final Identifier EARLY = Identifier.of("gravesandglory_local_test_tools", "perf_early"), LATE = Identifier.of("gravesandglory_local_test_tools", "perf_late");
    final List<Double> window = new ArrayList<>();
    final List<Double> phaseTicks = new ArrayList<>();
    String phase = "none";
    long phaseWall, runTicks;
    long buildWall, buildTicks;
    int buildMap = -1, lastMap = -1;
    final long wall0 = System.currentTimeMillis();
    Path dir;

    @Override public void onInitialize() {
        String d = System.getProperty("gravesandglory.perfProbe", "");
        // -Dgravesandglory.perfProbeIntegrated=true also runs it on an integrated (client-hosted) server.
        if (d.isEmpty() || (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER && !Boolean.getBoolean("gravesandglory.perfProbeIntegrated"))) return;
        dir = Path.of(d);
        int battleSeconds = Integer.getInteger("gravesandglory.perfBattleSeconds", 0);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("server-windows.csv"), "wallS,phase,map,players,meanMs,p95Ms,maxMs,heapMb,heapCommittedMb,chunks,entities\n");
            Files.writeString(dir.resolve("server-battles.csv"), "wallS,phase,map,ticks,wallSecs,meanMs,p50Ms,p95Ms,p99Ms,maxMs,over50\n");
            Files.writeString(dir.resolve("server-builds.csv"), "wallS,map,buildWallS,buildTicks\n");
            Files.writeString(dir.resolve("server-firstticks.csv"), "wallS,map,tick,ms,worldMs,endListenersMs,restMs\n");
        } catch (IOException e) { throw new RuntimeException(e); }
        ServerTickEvents.START_SERVER_TICK.register(s -> { tickStart = System.nanoTime(); worldNs = 0; });
        ServerTickEvents.START_WORLD_TICK.register(w -> worldStart = System.nanoTime());
        ServerTickEvents.END_WORLD_TICK.register(w -> worldNs += System.nanoTime() - worldStart);
        ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(EARLY, Event.DEFAULT_PHASE);
        ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(Event.DEFAULT_PHASE, LATE);
        ServerTickEvents.END_SERVER_TICK.register(EARLY, s -> endStart = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(LATE, s -> {
            long endNs = System.nanoTime() - endStart;
            double ms = (System.nanoTime() - tickStart) / 1e6;
            var b = GravesAndGlory.battle;
            String now = b == null ? "none" : !b.ready ? "build" : b.running ? "battle" : "camp";
            int map = b == null || b.campaign == null ? -1 : b.campaign.map;
            if (!now.equals(phase)) {
                flushPhase();
                if (now.equals("build")) { buildWall = System.currentTimeMillis(); buildTicks = 0; buildMap = map; }
                if (phase.equals("build")) {
                    append("server-builds.csv", String.format(Locale.ROOT, "%.0f,%d,%.1f,%d%n", (System.currentTimeMillis() - wall0) / 1000.0, buildMap, (System.currentTimeMillis() - buildWall) / 1000.0, buildTicks));
                }
                phase = now; phaseWall = System.currentTimeMillis(); runTicks = 0;
                phaseTicks.clear(); lastMap = map;
            }
            if (now.equals("build")) buildTicks++;
            // Battle tick 0 is the tick in which startRound ran (the phase flips at its end).
            if (now.equals("battle") && runTicks < 20) append("server-firstticks.csv", String.format(Locale.ROOT, "%.0f,%d,%d,%.2f,%.2f,%.2f,%.2f%n", (System.currentTimeMillis() - wall0) / 1000.0, map, runTicks, ms, worldNs / 1e6, endNs / 1e6, ms - worldNs / 1e6 - endNs / 1e6));
            window.add(ms); phaseTicks.add(ms); runTicks++;
            if (window.size() >= WINDOW) {
                List<Double> sorted = new ArrayList<>(window); Collections.sort(sorted);
                double mean = window.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                var rt = Runtime.getRuntime();
                int chunks = 0, entities = 0;
                for (var w : s.getWorlds()) { chunks += w.getChunkManager().getLoadedChunkCount(); for (var e : w.iterateEntities()) entities++; }
                append("server-windows.csv", String.format(Locale.ROOT, "%.0f,%s,%d,%d,%.2f,%.2f,%.2f,%d,%d,%d,%d%n", (System.currentTimeMillis() - wall0) / 1000.0, phase, map, s.getCurrentPlayerCount(),
                    mean, sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1), sorted.get(sorted.size() - 1), (rt.totalMemory() - rt.freeMemory()) >> 20,
                    ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted() >> 20, chunks, entities));
                window.clear();
            }
            if (battleSeconds > 0 && now.equals("battle") && runTicks >= battleSeconds * 20L && b.campaign != null) b.campaign.finish(-1);
        });
        Runtime.getRuntime().addShutdownHook(new Thread(this::flushPhase));
    }

    void flushPhase() {
        if (phaseTicks.size() < 20 || dir == null) return;
        List<Double> t = new ArrayList<>(phaseTicks); Collections.sort(t);
        double mean = t.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        int n = t.size(); long over = t.stream().filter(x -> x > 50).count();
        append("server-battles.csv", String.format(Locale.ROOT, "%.0f,%s,%d,%d,%.1f,%.2f,%.2f,%.2f,%.2f,%.2f,%d%n", (System.currentTimeMillis() - wall0) / 1000.0, phase, lastMap, n, (System.currentTimeMillis() - phaseWall) / 1000.0,
            mean, t.get((int) Math.ceil(n * 0.5) - 1), t.get((int) Math.ceil(n * 0.95) - 1), t.get((int) Math.ceil(n * 0.99) - 1), t.get(n - 1), over));
    }

    void append(String file, String line) {
        try { Files.writeString(dir.resolve(file), line, StandardOpenOption.APPEND, StandardOpenOption.CREATE); } catch (IOException e) { e.printStackTrace(); }
    }
}
