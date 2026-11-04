package gg.gravesandglory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unshipped performance probe for one integrated client plus bot-vs-bot (plan task 5.5). Pair it with -PautoHost (SelfTestHost hosts the world) and the BotSimulation
 * system properties. -Dgravesandglory.perfInt=<dir> writes <dir>/client.csv once a second (frames, fps, frame p95 and max, heap, bytes in and out of the connection, entities, phase)
 * and <dir>/events.log (wall seconds to the host call, to the world, to the arena being ready). While a battle runs it puts the player in spectator mode above the middle
 * of the map so the client renders the fight (a camp view would not). Read-only on the game apart from that teleport.
 */
public final class PerfIntegrated implements ClientModInitializer {
    final AtomicLong in = new AtomicLong(), out = new AtomicLong();
    final List<Double> frames = new ArrayList<>();
    long lastFrame, clock, wall0 = System.currentTimeMillis(), waitStart, worldAt, readyAt;
    boolean attached;
    Path dir, csv;

    void note(String s) { try { Files.writeString(dir.resolve("events.log"), String.format(Locale.ROOT, "%.1f %s%n", (System.currentTimeMillis() - wall0) / 1000.0, s), StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) {} }

    @Override public void onInitializeClient() {
        String d = System.getProperty("gravesandglory.perfInt", "");
        if (d.isEmpty()) return;
        dir = Path.of(d);
        try {
            Files.createDirectories(dir);
            csv = dir.resolve("client.csv");
            Files.writeString(csv, "wallS,phase,frames,fpsMean,frameP95Ms,frameP99Ms,frameMaxMs,heapMb,bytesIn,bytesOut,entities\n");
        } catch (Exception e) { throw new RuntimeException(e); }
        WorldRenderEvents.START.register(ctx -> {
            long now = System.nanoTime();
            if (lastFrame != 0) synchronized (frames) { frames.add((now - lastFrame) / 1e6); }
            lastFrame = now;
        });
        ClientTickEvents.END_CLIENT_TICK.register(c -> { try { tick(c); } catch (Exception e) { note("exception " + e); } });
    }

    void tick(MinecraftClient c) throws Exception {
        clock++;
        if (Files.exists(dir.resolve("stop.txt"))) { note("stop"); c.scheduleStop(); return; }
        if (c.world == null || c.player == null) { if (waitStart == 0 && clock > 60 && c.getOverlay() == null) { waitStart = System.currentTimeMillis(); note("host call about now"); } return; }
        if (worldAt == 0) { worldAt = System.currentTimeMillis(); note(String.format(Locale.ROOT, "in world %.1f s after the host call", (worldAt - waitStart) / 1000.0)); }
        if (!attached) attach(c);
        var b = GravesAndGlory.battle;
        if (readyAt == 0 && b != null && b.ready) { readyAt = System.currentTimeMillis(); note(String.format(Locale.ROOT, "arena ready %.1f s after the host call", (readyAt - waitStart) / 1000.0)); }
        boolean battle = GravesAndGloryClient.battleActive();
        if (battle && c.getServer() != null && clock % 100 == 0) {
            c.getServer().execute(() -> {
                var sp = c.getServer().getPlayerManager().getPlayerList();
                if (sp.isEmpty()) return;
                var p = sp.get(0);
                p.changeGameMode(GameMode.SPECTATOR);
                if (p.getPos().distanceTo(new Vec3d(0, 90, 0)) > 6) p.teleport(p.getServerWorld(), 0, 90, 0, 0f, 35f);
            });
        }
        if (clock % 20 == 0) sample(c, !GravesAndGloryClient.ready ? "load" : battle ? "battle" : "camp");
    }

    void sample(MinecraftClient c, String phase) throws Exception {
        List<Double> f;
        synchronized (frames) { f = new ArrayList<>(frames); frames.clear(); }
        Collections.sort(f);
        double sum = f.stream().mapToDouble(Double::doubleValue).sum();
        double fps = f.isEmpty() ? 0 : f.size() / (sum / 1000.0);
        double p95 = f.isEmpty() ? 0 : f.get((int) Math.ceil(f.size() * 0.95) - 1), p99 = f.isEmpty() ? 0 : f.get((int) Math.ceil(f.size() * 0.99) - 1), max = f.isEmpty() ? 0 : f.get(f.size() - 1);
        var rt = Runtime.getRuntime();
        Files.writeString(csv, String.format(Locale.ROOT, "%.0f,%s,%d,%.1f,%.2f,%.2f,%.2f,%d,%d,%d,%d%n", (System.currentTimeMillis() - wall0) / 1000.0, phase, f.size(), fps, p95, p99, max,
            (rt.totalMemory() - rt.freeMemory()) >> 20, in.get(), out.get(), c.world.getRegularEntityCount()), StandardOpenOption.APPEND);
    }

    void attach(MinecraftClient c) throws Exception {
        attached = true;
        var connection = c.getNetworkHandler().getConnection();
        Channel channel = null;
        for (var field : connection.getClass().getDeclaredFields()) if (Channel.class.isAssignableFrom(field.getType())) { field.setAccessible(true); channel = (Channel) field.get(connection); }
        if (channel == null) { note("no channel field"); return; }
        channel.pipeline().addFirst("perf_in", new ChannelInboundHandlerAdapter() {
            @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof ByteBuf b) in.addAndGet(b.readableBytes());
                super.channelRead(ctx, msg);
            }
        });
        channel.pipeline().addFirst("perf_out", new ChannelOutboundHandlerAdapter() {
            @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof ByteBuf b) out.addAndGet(b.readableBytes());
                super.write(ctx, msg, promise);
            }
        });
        note("byte counters attached");
    }
}
