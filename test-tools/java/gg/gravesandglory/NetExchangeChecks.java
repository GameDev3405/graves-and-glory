package gg.gravesandglory;

import gg.gravesandglory.net.Payloads;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.BundlePacket;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.util.math.Vec3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unshipped dedicated-server payload exchange check (port plan P1.3). Proves that every one of the 26 gravesandglory payload types crosses a real
 * network channel between a 1.21.1 dev client and a dedicated server, in each direction it is used.
 * <p>Both sides add one Netty handler in front of the connection's packet handler and count the custom payloads by their decoded class
 * (Payloads.XPayload), so a type that did not register and decoded as an unknown payload counts as missing. Server: counts what it sends
 * (S2C) and receives (C2S); client: what it receives (S2C) and sends (C2S).
 * <p>Server: -Dgravesandglory.netExchange=&lt;shared dir&gt; (build.gradle -PnetExchange=&lt;dir&gt;, which also loads SliceProbe on the same dir for
 * "finish" and "stop"). Writes &lt;dir&gt;/address.txt once started and &lt;dir&gt;/server-payloads.txt once a second. Injects one party_v1 per join
 * (party_v1 has no sender in src/main since the War Table replaced the party lectern; its client receiver is still registered).
 * <p>Client: -Dgravesandglory.netExchange=&lt;dir&gt; [-Dgravesandglory.netExchangeServer=host:port, else &lt;dir&gt;/address.txt]. Joins, starts a
 * practice battle (camp:practice on order_v1), calls the real client senders for every C2S type (and sends tactical_v1, which nothing in src/main
 * sends any more, by hand with the 1.20.1 layout), waits for the combat S2C types, has the server finish the round (ending_begin), then compares
 * both sides' counts, prints one "[net-exchange] PASS|FAIL" line per type and direction, writes &lt;dir&gt;/verdict.txt and stops the server.
 */
public final class NetExchangeChecks extends NetExchangeCounter implements ClientModInitializer {
    int clock, step, waitFrom, disconnects;
    boolean started, done;
    String address;

    @Override public void onInitializeClient() {
        String d = System.getProperty("gravesandglory.netExchange", "");
        if (d.isEmpty()) return;
        dir = Path.of(d);
        address = System.getProperty("gravesandglory.netExchangeServer", "");
        ClientLoginConnectionEvents.INIT.register((handler, client) -> attach(handler, "client login"));
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try { tick(c); } catch (Exception e) { log("client exception " + e); e.printStackTrace(); finish(c, "client exception " + e); }
        });
    }

    void tick(MinecraftClient c) throws Exception {
        clock++;
        if (done) return;
        if (c.getOverlay() != null) return;
        c.options.pauseOnLostFocus = false;
        if (!started) {
            if (address.isEmpty() && Files.exists(dir.resolve("address.txt"))) address = Files.readString(dir.resolve("address.txt")).trim();
            if (clock > 100 && !address.isEmpty()) {
                started = true; waitFrom = clock;
                log("connecting " + address);
                ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Net exchange", address, ServerInfo.ServerType.OTHER), false, null);
            } else if (clock > 20 * 900) finish(c, "no server address after 15 minutes");
            return;
        }
        if (c.currentScreen instanceof DisconnectedScreen s) { if (++disconnects > 10) finish(c, "disconnected: " + s.getNarratedTitle().getString()); return; }
        if (clock % 20 == 0) dump("client-payloads.txt");
        if (c.player == null || c.world == null || c.getNetworkHandler() == null) { if (clock - waitFrom > 20 * 600) finish(c, "not in the world after 10 minutes"); return; }
        int since = clock - waitFrom;
        switch (step) {
            case 0 -> { if (GravesAndGloryClient.ready) { log("in world, ready"); step = 1; waitFrom = clock; } else if (since > 20 * 300) finish(c, "never ready"); }
            case 1 -> { // the injected party_v1 opens the camp screen; give it two seconds, then close every screen and ask for a practice battle
                if (since < 60) return;
                if (c.currentScreen != null) c.setScreen(null);
                if (GravesAndGloryClient.battleActive()) { log("battle active"); step = 2; waitFrom = clock; return; }
                if (since % 200 == 60) { log("send camp:practice"); GravesAndGloryClient.send("camp:practice"); }
                if (since > 20 * 600) finish(c, "no practice battle after 10 minutes");
            }
            case 2 -> { // the client senders, once each, a few seconds into the battle
                if (since < 100) return;
                if (c.currentScreen != null) c.setScreen(null);
                Vec3d at = c.player.getPos();
                CampaignClient.sendProfile(c);
                CommandMode.ping(Spec.PINGS.length > 0 ? Spec.PINGS[0].id() : "attack", at.x, at.z);
                BattleMapClient.sendPlan(0, 0, "", -1, at.x, at.z);
                var eye = ChargeCamera.class.getDeclaredMethod("sendEye"); eye.setAccessible(true); eye.invoke(null);
                var v1 = PacketByteBufs.create(); // tactical_v1 in its 1.20.1 layout: group, action, formation, x, z, yaw, target
                v1.writeString("all"); v1.writeString("attack"); v1.writeString("block"); v1.writeDouble(at.x); v1.writeDouble(at.z); v1.writeFloat(c.player.getYaw()); v1.writeInt(-1);
                if (ClientPlayNetworking.canSend(GravesAndGlory.TACTICAL)) ClientPlayNetworking.send(Payloads.TacticalPayload.of(v1));
                GravesAndGloryClient.send("barracks"); // order_v1 "barracks": the server answers with barracks_v1 (Battle.openBarracks)
                Files.writeString(dir.resolve("cmd-probe.txt"), "follow\n");
                log("sent profile, ping, plan, camera, tactical_v1, barracks; asked the probe to follow the fighting");
                step = 3; waitFrom = clock;
            }
            case 3 -> { // combat traffic: up to four minutes, then the round ends either way
                if (c.currentScreen != null && since % 20 == 0) c.setScreen(null); // the barracks screen
                if (since % 20 == 10 && c.interactionManager != null) { // strike the nearest troop in reach (impact_v1 when it is an enemy)
                    net.minecraft.entity.LivingEntity near = null; double best = 3.5 * 3.5;
                    for (var e : c.world.getEntities()) if (e instanceof net.minecraft.entity.LivingEntity l && !(e instanceof net.minecraft.entity.player.PlayerEntity) && l.isAlive() && c.player.squaredDistanceTo(e) < best) { best = c.player.squaredDistanceTo(e); near = l; }
                    if (near != null) { c.player.lookAt(net.minecraft.command.argument.EntityAnchorArgumentType.EntityAnchor.EYES, near.getEyePos()); c.interactionManager.attackEntity(c.player, near); c.player.swingHand(net.minecraft.util.Hand.MAIN_HAND); }
                }
                // tactical_v2 attack once the 6 s intro and the command cooldown are over (OrderRules.playerOrder drops orders before introUntil and an unknown formation);
                // the army moves to fight (combat traffic) and the server acknowledges it (command_v1). Repeated every 20 s until command_v1 arrives.
                if (since % 400 == 0 && since > 0 && got("in", Payloads.CommandPayload.class) == 0) { CommandMode.issue("all", "attack", "block", c.player.getPos(), c.player.getYaw(), -1); log("sent tactical_v2 attack (block)"); }
                if (since == 20 * 150 && got("in", Payloads.RisePayload.class) == 0) Files.writeString(dir.resolve("cmd-probe.txt"), "rise\n");
                boolean all = COMBAT.stream().allMatch(t -> got("in", t) > 0);
                if (!all && since < 20 * 240 && GravesAndGloryClient.battleActive()) { if (since % 400 == 0) log("waiting for combat payloads: missing " + COMBAT.stream().filter(t -> got("in", t) == 0).map(Class::getSimpleName).toList()); return; }
                log("combat wait over after " + since / 20 + " s (all=" + all + "); asking the server to finish the round");
                Files.writeString(dir.resolve("cmd-server.txt"), "finish 0\n");
                Files.writeString(dir.resolve("cmd-probe.txt"), "unfollow\n");
                step = 4; waitFrom = clock;
            }
            case 4 -> { // the ending: wait for ending_begin and the camp's gear/relics updates, then a few seconds more
                if (since == 20 * 40) Files.writeString(dir.resolve("cmd-probe.txt"), "camp\n"); // back in camp: a station's use (camp_station_v1)
                if (c.currentScreen != null && since % 20 == 0 && since > 20 * 45) c.setScreen(null);
                if (got("in", Payloads.EndingBeginPayload.class) > 0 && got("in", Payloads.CampPayload.class) > 0 && since > 20 * 50 || since > 20 * 150) { step = 5; waitFrom = clock; }
            }
            case 5 -> { // the server dumps its counts once a second
                if (since < 60) return;
                finish(c, null);
            }
            default -> {}
        }
    }

    void finish(MinecraftClient c, String problem) {
        done = true;
        dump("client-payloads.txt");
        Map<String, Integer> server = new HashMap<>();
        try { for (String line : Files.readAllLines(dir.resolve("server-payloads.txt"))) { String[] f = line.trim().split(" "); if (f.length == 3) server.put(f[0] + " " + f[1], Integer.parseInt(f[2])); } }
        catch (Exception e) { log("no server counts: " + e); }
        List<String> lines = new ArrayList<>();
        int fails = 0;
        if (problem != null) { lines.add("FAIL flow " + problem); fails++; }
        for (var t : S2C) {
            int sent = server.getOrDefault("out " + t.getSimpleName(), 0), received = got("in", t);
            boolean ok = sent > 0 && received > 0;
            if (!ok) fails++;
            lines.add((ok ? "PASS" : "FAIL") + " s2c " + t.getSimpleName() + " server-sent=" + sent + " client-received=" + received);
        }
        for (var t : C2S) {
            int sent = got("out", t), received = server.getOrDefault("in " + t.getSimpleName(), 0);
            boolean ok = sent > 0 && received > 0;
            if (!ok) fails++;
            lines.add((ok ? "PASS" : "FAIL") + " c2s " + t.getSimpleName() + " client-sent=" + sent + " server-received=" + received);
        }
        for (var e : counts.entrySet()) if (e.getKey().contains("other:gravesandglory")) { lines.add("FAIL unknown " + e.getKey() + " " + e.getValue()); fails++; }
        for (var e : server.entrySet()) if (e.getKey().contains("other:gravesandglory")) { lines.add("FAIL unknown server " + e.getKey() + " " + e.getValue()); fails++; }
        lines.add((fails == 0 ? "PASS" : "FAIL") + " net-exchange total " + (S2C.size() + C2S.size()) + " types, " + fails + " failures");
        for (String l : lines) log(l);
        try {
            Files.write(dir.resolve("verdict.txt"), lines);
            Files.writeString(dir.resolve("cmd-server.txt"), "stop\n");
        } catch (Exception e) { e.printStackTrace(); }
        c.scheduleStop();
    }
}
