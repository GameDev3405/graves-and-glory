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

/** The dedicated-server side of {@link NetExchangeChecks}. */
public final class NetExchangeProbe extends NetExchangeCounter implements ModInitializer {
    @Override public void onInitialize() {
        String d = System.getProperty("gravesandglory.netExchange", "");
        if (d.isEmpty() || FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;
        dir = Path.of(d);
        try { Files.createDirectories(dir); } catch (Exception e) { throw new RuntimeException(e); }
        ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register((handler, server) -> attach(handler, "server configuration " + handler.getDebugProfile().getName()));
        Map<UUID, Integer> partyAt = new ConcurrentHashMap<>();
        int[] tick = {0};
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> { attach(handler, "server play " + handler.player.getGameProfile().getName()); partyAt.put(handler.player.getUuid(), tick[0] + 40); });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try { Files.writeString(dir.resolve("address.txt"), "127.0.0.1:" + server.getServerPort()); } catch (Exception e) { e.printStackTrace(); }
            log("server started on port " + server.getServerPort() + ", loginGate=" + System.getProperty("gravesandglory.loginGate", "(default on)"));
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tick[0]++;
            for (var e : partyAt.entrySet()) {
                if (e.getValue() > tick[0]) continue;
                partyAt.remove(e.getKey());
                var p = server.getPlayerManager().getPlayer(e.getKey());
                if (p != null && ServerPlayNetworking.canSend(p, GravesAndGlory.PARTY)) { ServerPlayNetworking.send(p, new Payloads.PartyPayload(new byte[0])); log("injected party_v1 to " + p.getGameProfile().getName()); }
            }
            if (tick[0] % 20 == 0) dump("server-payloads.txt");
        });
    }
}
