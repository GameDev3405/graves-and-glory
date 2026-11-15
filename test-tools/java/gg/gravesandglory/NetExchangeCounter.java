package gg.gravesandglory;

import gg.gravesandglory.net.Payloads;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/** The counter both sides of the payload exchange check share (no client classes, so the dedicated server can load it). */
class NetExchangeCounter {
    /** Every payload type by direction: S2C first (17 play + 1 configuration), then C2S (7 play + 1 configuration). */
    static final List<Class<?>> S2C = List.of(Payloads.CampPayload.class, Payloads.CommandPayload.class, Payloads.ImpactPayload.class, Payloads.MapGridPayload.class,
        Payloads.MapStatePayload.class, Payloads.StatePayload.class, Payloads.OpenBarracksPayload.class, Payloads.PartyPayload.class, Payloads.EndingBeginPayload.class,
        Payloads.FeedPayload.class, Payloads.GearPayload.class, Payloads.RelicsPayload.class, Payloads.AttackPayload.class, Payloads.HitPayload.class,
        Payloads.StancePayload.class, Payloads.RisePayload.class, Payloads.RankPayload.class, Payloads.ReleaseCheckRequestPayload.class);
    static final List<Class<?>> C2S = List.of(Payloads.OrderPayload.class, Payloads.ProfilePayload.class, Payloads.CameraPayload.class, Payloads.PingPayload.class,
        Payloads.PlanPayload.class, Payloads.TacticalPayload.class, Payloads.TacticalV2Payload.class, Payloads.ReleaseCheckReplyPayload.class);
    /** Types the battle itself has to produce; the client waits for these (or a time limit) before it asks the server to finish the round. */
    static final List<Class<?>> COMBAT = List.of(Payloads.ImpactPayload.class, Payloads.AttackPayload.class, Payloads.HitPayload.class, Payloads.StancePayload.class,
        Payloads.RisePayload.class, Payloads.FeedPayload.class, Payloads.RankPayload.class, Payloads.CommandPayload.class, Payloads.MapStatePayload.class);

    /** "in Payloads$XPayload" / "out Payloads$XPayload" (or "in other:<id>") to a count. */
    final Map<String, AtomicInteger> counts = new ConcurrentSkipListMap<>();
    Path dir;

    static String key(CustomPayload p) {
        Class<?> c = p.getClass();
        return c.getEnclosingClass() == Payloads.class ? c.getSimpleName() : "other:" + p.getId().id();
    }
    void count(String way, Object msg) {
        if (msg instanceof BundlePacket<?> b) { for (Object o : b.getPackets()) count(way, o); return; }
        CustomPayload p = msg instanceof CustomPayloadS2CPacket s ? s.payload() : msg instanceof CustomPayloadC2SPacket c ? c.payload() : null;
        if (p != null) counts.computeIfAbsent(way + " " + key(p), k -> new AtomicInteger()).incrementAndGet();
    }
    int got(String way, Class<?> type) { var a = counts.get(way + " " + type.getSimpleName()); return a == null ? 0 : a.get(); }

    static Object field(Object owner, Class<?> type) throws Exception {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass())
            for (var f : c.getDeclaredFields()) if (type.isAssignableFrom(f.getType())) { f.setAccessible(true); return f.get(owner); }
        return null;
    }
    /** Puts the counter in front of the connection's packet handler, where inbound messages are decoded packets and outbound ones not yet encoded. */
    void attach(Object handlerOrConnection, String tag) {
        try {
            ClientConnection connection = handlerOrConnection instanceof ClientConnection cc ? cc : (ClientConnection) field(handlerOrConnection, ClientConnection.class);
            Channel channel = connection == null ? null : (Channel) field(connection, Channel.class);
            if (channel == null) { log("no channel on " + tag); return; }
            var pipe = channel.pipeline();
            if (pipe.get("gg_net_exchange") != null) return;
            var counter = new ChannelDuplexHandler() {
                @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception { count("in", msg); super.channelRead(ctx, msg); }
                @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception { count("out", msg); super.write(ctx, msg, promise); }
            };
            if (pipe.get("packet_handler") != null) pipe.addBefore("packet_handler", "gg_net_exchange", counter); else pipe.addLast("gg_net_exchange", counter);
            log("counter attached (" + tag + ") pipeline " + pipe.names());
        } catch (Exception e) { log("attach failed (" + tag + "): " + e); e.printStackTrace(); }
    }
    void dump(String name) {
        try {
            StringBuilder s = new StringBuilder();
            counts.forEach((k, v) -> s.append(k).append(' ').append(v.get()).append('\n'));
            Path tmp = dir.resolve(name + ".tmp");
            Files.writeString(tmp, s);
            Files.move(tmp, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { e.printStackTrace(); }
    }
    static void log(String s) { System.out.println("[net-exchange] " + s); }
}
