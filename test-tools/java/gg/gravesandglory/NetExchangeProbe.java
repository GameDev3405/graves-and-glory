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
            if (tick[0] % 20 == 0) { dump("server-payloads.txt"); try { commands(server); } catch (Exception ex) { log("probe command failed: " + ex); ex.printStackTrace(); } }
            if (follow && tick[0] % 60 == 0) follow(server);
        });
    }

    /** "follow" from the client: keep each player beside a troop that is fighting, so the motion events (attack_v1, hit_v1) of troops he tracks reach him. */
    boolean follow;

    /**
     * &lt;dir&gt;/cmd-probe.txt, one command per line (the client writes it):
     * follow: start following the fighting (see above);
     * rise: if no rise_v1 went out naturally (only a House with rise_again or an undying ability rolls one), replay MotionEvents.sendRise for one living
     *   troop each player tracks, with a 40-tick window: the real sender, the troop's presentation only;
     * camp: GravesAndGlory.openCamp(player, first station), the path a camp station's use takes, for each player (camp_station_v1 outside a battle).
     */
    void commands(net.minecraft.server.MinecraftServer server) throws Exception {
        Path f = dir.resolve("cmd-probe.txt");
        if (!Files.exists(f)) return;
        var lines = Files.readAllLines(f); Files.delete(f);
        var b = GravesAndGlory.battle;
        for (String line : lines) {
            line = line.trim(); log("probe command " + line);
            if (line.equals("follow")) follow = true;
            else if (line.equals("unfollow")) follow = false;
            else if (line.equals("rise") && b != null) {
                if (got("out", Payloads.RisePayload.class) > 0) { log("rise_v1 already went out naturally"); continue; }
                for (var p : server.getPlayerManager().getPlayerList())
                    for (var u : b.units) if (u.mob.isAlive() && net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(u.mob).contains(p)) {
                        u.riseTick = u.mob.getWorld().getTime(); u.riseWindow = 40;
                        log("replayed rise for troop " + u.mob.getId() + " to " + MotionEvents.sendRise(u) + " players (injected)");
                        break;
                    }
            } else if (line.equals("camp")) {
                String station = Spec.CAMPSTATIONS[0].page();
                for (var p : server.getPlayerManager().getPlayerList()) { GravesAndGlory.openCamp(p, station); log("openCamp " + station + " for " + p.getGameProfile().getName()); }
            }
        }
    }

    void follow(net.minecraft.server.MinecraftServer server) {
        var b = GravesAndGlory.battle;
        if (b == null || !b.running) return;
        for (var p : server.getPlayerManager().getPlayerList()) {
            GravesAndGlory.Unit pick = null; double best = Double.MAX_VALUE;
            for (var u : b.units) {
                if (!u.mob.isAlive() || u.mob.getTarget() == null || !u.mob.getTarget().isAlive()) continue;
                double d = u.mob.squaredDistanceTo(p);
                if (d < best) { best = d; pick = u; }
            }
            if (pick == null) { // nobody fights yet: stand by the enemy troop nearest our own army's first troop
                for (var u : b.units) if (u.mob.isAlive() && b.squads.get(p.getUuid()) != null && u.squad.team != b.squads.get(p.getUuid()).team) { double d = u.mob.squaredDistanceTo(p); if (d < best) { best = d; pick = u; } }
            }
            if (pick == null || best < 12 * 12) continue;
            var at = pick.mob.getPos();
            p.requestTeleport(at.x + 3, at.y + 1, at.z + 3);
            log("follow: moved " + p.getGameProfile().getName() + " beside troop " + pick.mob.getId() + " (fighting=" + (pick.mob.getTarget() != null) + ")");
        }
    }
}
