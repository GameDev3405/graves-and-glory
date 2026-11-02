package gg.gravesandglory;

import com.google.gson.JsonObject;
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
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unshipped performance baseline client (plan task 3.6). One real dev client joins a dedicated server, forms a War Table room (host) or joins it (guest),
 * readies for each map in turn, plays the battle (the server ends it, see PerfProbe) and samples frame times, heap and network bytes once a second.
 * -Dgravesandglory.perfClient=host:port  -Dgravesandglory.perfRole=host|guest  -Dgravesandglory.perfDir=<shared dir>
 * -Dgravesandglory.perfMaps=1,0  [-Dgravesandglory.perfSize=3]
 */
public final class PerfClient implements ClientModInitializer {
    final AtomicLong in = new AtomicLong(), out = new AtomicLong();
    final List<Double> frames = new ArrayList<>();
    long lastFrame, clock, joinedAt, lastSend, wall0 = System.currentTimeMillis();
    boolean started, attached, wasBattle, hostWroteRoom;
    int idx, battles;
    Path dir, csv;
    String name = "?";

    void note(String s) { System.out.println("[perf-client] " + s); try { Files.writeString(dir.resolve("client-" + name + ".log"), String.format(Locale.ROOT, "%.0f %s%n", (System.currentTimeMillis() - wall0) / 1000.0, s), StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) {} }

    @Override public void onInitializeClient() {
        String address = System.getProperty("gravesandglory.perfClient", "");
        if (address.isEmpty()) return;
        boolean host = System.getProperty("gravesandglory.perfRole", "guest").equals("host");
        dir = Path.of(System.getProperty("gravesandglory.perfDir", "perf"));
        int[] maps = Arrays.stream(System.getProperty("gravesandglory.perfMaps", "1,0").split(",")).mapToInt(Integer::parseInt).toArray();
        int size = Integer.getInteger("gravesandglory.perfSize", 3);
        try { Files.createDirectories(dir); } catch (Exception e) { throw new RuntimeException(e); }
        WorldRenderEvents.START.register(ctx -> {
            long now = System.nanoTime();
            if (lastFrame != 0) synchronized (frames) { frames.add((now - lastFrame) / 1e6); }
            lastFrame = now;
        });
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try { tick(c, address, host, maps, size); } catch (Exception e) { e.printStackTrace(); note("exception " + e); }
        });
    }

    void tick(MinecraftClient c, String address, boolean host, int[] maps, int size) throws Exception {
        clock++;
        name = c.getSession().getUsername();
        if (csv == null) {
            csv = dir.resolve("client-" + name + ".csv");
            Files.writeString(csv, "wallS,phase,frames,fpsMean,frameP95Ms,frameMaxMs,heapMb,heapMaxMb,bytesIn,bytesOut,entities,pingMs,room,mapIdx\n");
        }
        if (Files.exists(dir.resolve("done.txt"))) { note("done, stopping"); c.scheduleStop(); return; }
        if (c.getOverlay() != null) return;
        if (!started) {
            if (clock > 100) { started = true; note("connecting " + address); ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Perf baseline", address, false), false); }
            return;
        }
        if (c.player == null || c.world == null || c.getNetworkHandler() == null) { if (clock % 200 == 0) note("not in world yet screen=" + (c.currentScreen == null ? "none" : c.currentScreen.getClass().getSimpleName())); return; }
        if (!attached) attach(c);
        if (joinedAt == 0) { joinedAt = clock; note("in world"); }
        boolean battle = GravesAndGloryClient.battleActive();
        String phase = !GravesAndGloryClient.ready ? "load" : battle ? "battle" : GravesAndGloryClient.running ? "waiting" : "camp";
        if (clock % 20 == 0) sample(c, phase);
        if (battle) wasBattle = true;
        else if (wasBattle && GravesAndGloryClient.ready && !GravesAndGloryClient.running) { wasBattle = false; battles++; idx++; note("battle " + battles + " finished"); }
        if (!GravesAndGloryClient.ready) return;
        if (idx >= maps.length) { if (host) { Files.writeString(dir.resolve("done.txt"), "done"); } return; }
        JsonObject s = CampaignClient.social;
        if (s == null || GravesAndGloryClient.running || clock - lastSend < 30) return;
        String room = s.get("room").getAsString();
        if (host) {
            Files.writeString(dir.resolve("target.txt"), Integer.toString(maps[idx]));
            if (room.isEmpty()) { send("social:lobby"); return; }
            if (!hostWroteRoom || clock % 400 == 0) { Files.writeString(dir.resolve("room.txt"), room); hostWroteRoom = true; }
            if (s.get("preparing").getAsBoolean()) return;
            if (s.getAsJsonArray("commanders").size() < 2) return;
            if (s.get("size").getAsInt() != size) { send("social:size:" + size); return; }
            if (s.get("map").getAsInt() != maps[idx]) { send("social:map:" + maps[idx]); return; }
        } else {
            if (room.isEmpty()) { if (Files.exists(dir.resolve("room.txt"))) send("social:join:" + Files.readString(dir.resolve("room.txt")).trim()); return; }
            if (s.get("preparing").getAsBoolean()) return;
            if (!Files.exists(dir.resolve("target.txt")) || s.get("map").getAsInt() != Integer.parseInt(Files.readString(dir.resolve("target.txt")).trim())) return;
            if (s.get("size").getAsInt() != size) return;
        }
        boolean meReady = false;
        for (var e : s.getAsJsonArray("commanders")) if (e.getAsJsonObject().get("me").getAsBoolean()) meReady = e.getAsJsonObject().get("ready").getAsBoolean();
        if (!meReady) send("social:ready");
    }

    void send(String cmd) { lastSend = clock; note("send " + cmd); GravesAndGloryClient.send(cmd); }

    void sample(MinecraftClient c, String phase) throws Exception {
        List<Double> f;
        synchronized (frames) { f = new ArrayList<>(frames); frames.clear(); }
        Collections.sort(f);
        double sum = f.stream().mapToDouble(Double::doubleValue).sum();
        double fps = f.isEmpty() ? 0 : f.size() / (sum / 1000.0);
        double p95 = f.isEmpty() ? 0 : f.get((int) Math.ceil(f.size() * 0.95) - 1), max = f.isEmpty() ? 0 : f.get(f.size() - 1);
        var rt = Runtime.getRuntime();
        var entry = c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
        JsonObject s = CampaignClient.social;
        Files.writeString(csv, String.format(Locale.ROOT, "%.0f,%s,%d,%.1f,%.2f,%.2f,%d,%d,%d,%d,%d,%d,%s,%d%n", (System.currentTimeMillis() - wall0) / 1000.0, phase, f.size(), fps, p95, max,
            (rt.totalMemory() - rt.freeMemory()) >> 20, rt.maxMemory() >> 20, in.get(), out.get(), c.world.getRegularEntityCount(), entry == null ? -1 : entry.getLatency(),
            s == null ? "" : s.get("room").getAsString(), idx), StandardOpenOption.APPEND);
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
