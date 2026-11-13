package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;
import java.util.UUID;

/** p4-map-art (battle map Task 5; a Phase4Checks suite, run as GRAVES_PHASE4_CHECKS=map-art): on map 0 (Rivergate, a procedural old map)
 *  and then New Kingsfield (an engine map), the battle map texture (the real look only, toned down; Revision 2) exists within 200 client
 *  ticks of the round start, is painted from that map's grid at one texel per block, and is dumped as a PNG to <run>/evidence/map-art. */
public final class MapArtChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    static final int[] MAPS = {0, 5};
    static final int WITHIN = 200;
    volatile UUID player;
    /** index into MAPS; the server sets started once that map's round runs, the client counts from there */
    volatile int at;
    volatile boolean started, finished;
    int startTick = -1;

    public int limit() { return 16000; } // New Kingsfield is built from scratch after the switch

    public void step(MinecraftClient c, int tick) {
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); }
        if (finished) return;
        if (at >= MAPS.length) { finished = true; Phase4Checks.finish(c, "map-art"); return; }
        if (!started) { startTick = -1; return; }
        if (startTick < 0) startTick = tick;
        int map = MAPS[at];
        String id = Spec.MAPS[map].id();
        var g = BattleMapClient.grid;
        boolean ready = g != null && g.map.equals(id) && BattleMapArt.real() != null && id.equals(BattleMapArt.builtMap());
        int waited = tick - startTick;
        if (!ready && waited <= WITHIN) return;
        String name = "p4-map-art-" + id;
        if (!ready) Phase4Checks.report(name, false, "no texture " + waited + " ticks after the round start (grid " + (g == null ? "none" : g.map) + ", built " + BattleMapArt.builtMap() + ")");
        else {
            int[] rs = BattleMapArt.size();
            boolean sizes = rs[0] == g.width && rs[1] == g.depth;
            String dumped;
            try {
                Path dir = Path.of(System.getProperty("gravesandglory.localChecks", "work/local-checks.txt")).toAbsolutePath().getParent().resolve("map-art");
                dumped = BattleMapArt.dump(dir) ? "dumped to " + dir : "dump found no textures";
            } catch (Exception e) { dumped = "dump failed: " + e; }
            Phase4Checks.report(name, sizes, "the texture " + waited + " ticks after the round start; grid " + g.width + "x" + g.depth + ", real " + rs[0] + "x" + rs[1]
                + "; " + dumped);
        }
        at++; // before started drops, so the server never restarts the map just checked
        started = false;
    }

    public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || started || finished || at >= MAPS.length) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        if (Phase1Checks.skirmish(b, p, MAPS[at], 3, "normal")) {
            System.out.println("[p4] map-art: round started on map " + MAPS[at] + " (" + Spec.MAPS[MAPS[at]].id() + ")");
            started = true;
        }
    }
}
