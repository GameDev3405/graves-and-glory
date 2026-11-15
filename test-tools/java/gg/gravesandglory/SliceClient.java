package gg.gravesandglory;

import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import java.nio.file.*;
import java.util.*;

/**
 * Unshipped slice dry-run client (plan task 3.7), a remote-controlled harness client. Nothing is scripted: the driver writes commands to
 * <dir>/cmd-<user>.txt (one per line: connect, disconnect, send <social/order command>, quit) and reads <dir>/state-<user>.json (twice a second:
 * pid, connection, camp or battle phase, the lobby room, the profile summary, the last result report) and client-<user>.log.
 * -Dgravesandglory.sliceClient=host:port  -Dgravesandglory.sliceDir=<dir>
 */
public final class SliceClient implements ClientModInitializer {
    Path dir;
    String address, name = "?";
    long clock, wall0 = System.currentTimeMillis();
    String lastScreen = "";

    void note(String s) {
        System.out.println("[slice-client] " + s);
        try { Files.writeString(dir.resolve("client-" + name + ".log"), String.format(Locale.ROOT, "%.0f %s%n", (System.currentTimeMillis() - wall0) / 1000.0, s), StandardOpenOption.CREATE, StandardOpenOption.APPEND); } catch (Exception ignored) {}
    }

    @Override public void onInitializeClient() {
        address = System.getProperty("gravesandglory.sliceClient", "");
        if (address.isEmpty()) return;
        dir = Path.of(System.getProperty("gravesandglory.sliceDir", "slice"));
        try { Files.createDirectories(dir); } catch (Exception e) { throw new RuntimeException(e); }
        ClientTickEvents.END_CLIENT_TICK.register(c -> { try { tick(c); } catch (Exception e) { e.printStackTrace(); note("exception " + e); } });
    }

    void tick(MinecraftClient c) throws Exception {
        clock++;
        name = c.getSession().getUsername();
        String screen = c.currentScreen == null ? "none" : c.currentScreen.getClass().getSimpleName();
        if (!screen.equals(lastScreen)) { lastScreen = screen; note("screen " + screen); }
        if (clock % 10 == 0) commands(c);
        if (clock % 10 == 5) state(c);
    }

    void commands(MinecraftClient c) throws Exception {
        if (c.getOverlay() != null) return;
        Path f = dir.resolve("cmd-" + name + ".txt");
        if (!Files.exists(f)) return;
        List<String> lines = Files.readAllLines(f); Files.delete(f);
        for (String line : lines) {
            line = line.trim();
            note("command " + line);
            if (line.equals("connect")) ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Slice dry run", address, ServerInfo.ServerType.OTHER), false, null);
            else if (line.equals("disconnect")) { if (c.getNetworkHandler() != null) c.getNetworkHandler().getConnection().disconnect(Text.literal("slice dry run: clean disconnect")); }
            else if (line.startsWith("send ")) GravesAndGloryClient.send(line.substring(5));
            else if (line.equals("quit")) c.scheduleStop();
        }
    }

    void state(MinecraftClient c) throws Exception {
        JsonObject j = new JsonObject();
        j.addProperty("pid", ProcessHandle.current().pid());
        j.addProperty("wall", System.currentTimeMillis());
        j.addProperty("screen", lastScreen);
        boolean inWorld = c.player != null && c.world != null && c.getNetworkHandler() != null;
        j.addProperty("inWorld", inWorld);
        j.addProperty("ready", GravesAndGloryClient.ready);
        j.addProperty("running", GravesAndGloryClient.running);
        j.addProperty("battle", GravesAndGloryClient.battleActive());
        j.addProperty("camp", GravesAndGloryClient.inCamp());
        j.addProperty("headline", CampaignClient.headline);
        j.addProperty("notice", CampaignClient.notice);
        j.addProperty("waiting", CampaignClient.waiting);
        j.add("social", CampaignClient.social);
        j.add("result", CampaignClient.result);
        j.add("battleInfo", CampaignClient.battle);
        var p = CampaignClient.profile;
        JsonObject pr = new JsonObject();
        pr.addProperty("gearShards", p.gearShards); pr.addProperty("troopWrits", p.troopWrits); pr.addProperty("matches", p.matches); pr.addProperty("wins", p.wins);
        pr.addProperty("xp", p.progression.xp); pr.addProperty("shards", String.valueOf(p.shards)); pr.addProperty("renown", String.valueOf(p.renown));
        j.add("profile", pr);
        if (inWorld) { j.addProperty("x", c.player.getX()); j.addProperty("y", c.player.getY()); j.addProperty("z", c.player.getZ()); j.addProperty("entities", c.world.getRegularEntityCount()); }
        Path tmp = dir.resolve("state-" + name + ".tmp");
        Files.writeString(tmp, j.toString());
        Files.move(tmp, dir.resolve("state-" + name + ".json"), StandardCopyOption.REPLACE_EXISTING);
    }
}
