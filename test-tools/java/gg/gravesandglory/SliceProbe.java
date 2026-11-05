package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

/**
 * Unshipped slice dry-run probe for a dedicated server (plan task 3.7). Read-only on the game except two explicit commands.
 * -Dgravesandglory.sliceDir=<dir>  writes <dir>/state-server.json once a second (pid, phase, mode, map, round time, scores, seats, held slots,
 * every loaded profile's gold, matches, wins) and polls <dir>/cmd-server.txt: "finish <team>" ends the running round (-1 draw), "stop" stops the server cleanly.
 */
public final class SliceProbe implements ModInitializer {
    Path dir;
    int tick;

    @Override public void onInitialize() {
        String d = System.getProperty("gravesandglory.sliceDir", "");
        if (d.isEmpty() || FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;
        dir = Path.of(d);
        try { Files.createDirectories(dir); } catch (Exception e) { throw new RuntimeException(e); }
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (++tick % 20 != 0) return;
            try { state(s); commands(s); } catch (Exception e) { e.printStackTrace(); }
        });
    }

    void state(net.minecraft.server.MinecraftServer s) throws Exception {
        JsonObject j = new JsonObject();
        j.addProperty("pid", ProcessHandle.current().pid());
        j.addProperty("wall", System.currentTimeMillis());
        var b = GravesAndGlory.battle;
        j.addProperty("phase", b == null ? "none" : !b.ready ? "build" : b.running ? "battle" : "camp");
        j.addProperty("players", s.getCurrentPlayerCount());
        JsonArray names = new JsonArray();
        for (var p : s.getPlayerManager().getPlayerList()) names.add(p.getGameProfile().getName());
        j.add("online", names);
        if (b != null) {
            j.addProperty("mode", b.mode.id());
            j.addProperty("map", b.campaign.map);
            j.addProperty("time", b.time);
            j.addProperty("roundStarted", b.roundStarted);
            j.addProperty("roundSeconds", b.roundSeconds);
            j.addProperty("scores", Arrays.toString(b.scores));
            j.addProperty("matchId", b.campaign.matchId);
            JsonArray seats = new JsonArray();
            for (var sq : b.squads.values()) {
                JsonObject o = new JsonObject();
                var pl = s.getPlayerManager().getPlayer(sq.owner);
                o.addProperty("owner", pl != null ? pl.getGameProfile().getName() : sq.bot ? "bot" : sq.owner.toString().substring(0, 8));
                o.addProperty("team", sq.team); o.addProperty("bot", sq.bot); o.addProperty("seated", sq.seated); o.addProperty("takeoverAt", sq.takeoverAt);
                seats.add(o);
            }
            j.add("seats", seats);
            JsonArray held = new JsonArray();
            for (var e : b.campaign.vacatedUntil.entrySet()) held.add(e.getKey() + "@" + e.getValue());
            j.add("held", held);
            JsonObject profs = new JsonObject();
            for (var e : b.campaign.profiles.entrySet()) {
                JsonObject p = new JsonObject();
                p.addProperty("gold", e.getValue().gold); p.addProperty("matches", e.getValue().matches); p.addProperty("wins", e.getValue().wins);
                p.addProperty("xp", e.getValue().progression.xp);
                p.addProperty("shards", String.valueOf(e.getValue().shards)); p.addProperty("renown", String.valueOf(e.getValue().renown));
                profs.add(e.getKey().toString(), p);
            }
            j.add("profiles", profs);
        }
        Path tmp = dir.resolve("state-server.tmp");
        Files.writeString(tmp, j.toString());
        Files.move(tmp, dir.resolve("state-server.json"), StandardCopyOption.REPLACE_EXISTING);
    }

    void commands(net.minecraft.server.MinecraftServer s) throws Exception {
        Path f = dir.resolve("cmd-server.txt");
        if (!Files.exists(f)) return;
        List<String> lines = Files.readAllLines(f); Files.delete(f);
        for (String line : lines) {
            line = line.trim();
            System.out.println("[slice-probe] command " + line);
            var b = GravesAndGlory.battle;
            if (line.startsWith("finish") && b != null && b.running) b.campaign.finish(Integer.parseInt(line.substring(6).trim().isEmpty() ? "-1" : line.substring(6).trim()));
            else if (line.equals("stop")) s.stop(false);
        }
    }
}
