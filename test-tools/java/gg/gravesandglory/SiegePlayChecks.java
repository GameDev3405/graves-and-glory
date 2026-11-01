package gg.gravesandglory;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * Suite "siege-play": one Siege round picked in the lobby and played to the end on a real client. The host's room picks
 * Siege (the map moves to Ironhold), the round starts from the room, the commander stands in the Outer Yard until it
 * falls, breaks the Iron Gate with player blows until the open gate returns, then holds the Keep until the attackers win.
 * A stage that bots keep contested past its deadline is forced, and the report says so. Shots: p4-siege-lobby, -yard,
 * -gate, -breach and -keep.
 */
final class SiegePlayChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    volatile UUID player;
    volatile boolean done;
    /** A shot the server asks the client for; the client clears it once taken. */
    volatile String want;
    volatile boolean lobbyScreen;
    int sub, wait;
    long stageAt;
    boolean forcedYard, forcedKeep;
    final List<String> notes = new ArrayList<>();

    @Override public int limit() { return 36000; } // Ironhold builds, then a full round of three stages

    @Override public void cleanup(MinecraftClient c) {
        c.options.hudHidden = true;
        Phase4Checks.server(c, b -> {
            var p = player == null ? null : b.server.getPlayerManager().getPlayer(player);
            if (p != null) { p.getAbilities().invulnerable = false; p.sendAbilitiesUpdate(); }
            if (b.running) b.campaign.returnLobby();
        });
    }

    @Override public void step(MinecraftClient c, int tick) {
        if (tick == 20) { c.setScreen(null); player = c.player.getUuid(); }
        c.options.hudHidden = false; // the stage shots show the ledger
        if (want != null) {
            if (++wait == 1 && lobbyScreen) c.setScreen(new CampScreen("lobby"));
            if (wait == 40) {
                Phase4Checks.shot(c, want);
                if (lobbyScreen) c.setScreen(null);
                lobbyScreen = false; wait = 0; want = null;
            }
        }
        if (done) Phase4Checks.finish(c, "siege-play");
    }

    void ask(String shot) { wait = 0; want = shot; }

    static int top(GravesAndGlory.Battle b, double x, double z) {
        return b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
    }

    void go(GravesAndGlory.Battle b, ServerPlayerEntity p, double x, double z, float yaw) {
        p.teleport(b.world, x, top(b, x, z), z, yaw, 10);
    }

    void fail(String id, String why) {
        Phase4Checks.report(id, false, why);
        done = true;
    }

    @Override public void serverTick(GravesAndGlory.Battle b) {
        if (player == null || done || want != null) return;
        ServerPlayerEntity p = b.server.getPlayerManager().getPlayer(player);
        if (p == null) return;
        var c = b.campaign;
        switch (sub) {
            case 0 -> { // the campaign row: battle 2 is a siege again, its bonus untouched
                Spec.CampaignBattlesRow row = null;
                for (var r : Spec.CAMPAIGNBATTLES) if (r.id().equals("march-2")) row = r;
                Phase4Checks.report("p4-siege-campaign-row", row != null && row.mode().equals(Modes.SIEGE) && row.bonus().equals("b2_lose10"),
                        row == null ? "no march-2" : "mode=" + row.mode() + " bonus=" + row.bonus());
                c.battleMode = Modes.DOMINATION; c.closeParty();
                if (c.map != 1) c.switchMap(1);
                stageAt = b.time; sub = 1;
            }
            case 1 -> { // the host picks Siege in a room: the map follows to Ironhold
                if (b.time - stageAt > 12000) { fail("p4-siege-lobby", "Ironhold was not ready within 12000 ticks"); return; }
                if (!b.ready) return;
                String said = "";
                for (String cmd : List.of("social:room", "social:map:0", "social:mode:siege")) {
                    c.notices.remove(p.getUuid()); c.handle(p, cmd); said += cmd + " -> " + c.notices.getOrDefault(p.getUuid(), "") + "; ";
                }
                boolean listed = SocialHub.MODES.contains(Modes.SIEGE) || Modes.ids().contains(Modes.SIEGE);
                boolean moved = said.contains("played on " + Spec.MAPS[1].name());
                Phase4Checks.report("p4-siege-lobby", listed && moved, said);
                lobbyScreen = true; ask("siege-lobby"); sub = 2;
            }
            case 2 -> { // ready and start from the room
                String said = "";
                for (String cmd : List.of("social:ready", "social:start")) {
                    c.notices.remove(p.getUuid()); c.handle(p, cmd); said += cmd + " -> " + c.notices.getOrDefault(p.getUuid(), "") + "; ";
                }
                stageAt = b.time; sub = 3; notes.add(said);
            }
            case 3 -> { // the round runs as a Siege on Ironhold with the gate shut
                if (!b.running) { if (b.time - stageAt > 12000) fail("p4-siege-start", "no round: " + notes); return; }
                var s = b.squads.get(p.getUuid());
                boolean ok = b.mode instanceof SiegeMode && c.map == 1 && s != null && s.seated && s.team == 0
                        && b.world.getBlockState(new BlockPos(-37, 68, 0)).getBlock() == Blocks.IRON_BARS;
                Phase4Checks.report("p4-siege-start", ok, "mode=" + b.mode.id() + " map=" + c.map + " team=" + (s == null ? "none" : s.team + " seated=" + s.seated));
                if (!ok) { done = true; return; }
                p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate(); // the camera must live through the round
                go(b, p, SiegeMode.YARD_X, SiegeMode.YARD_Z, -90);
                stageAt = b.time; sub = 4;
            }
            case 4 -> { // the Outer Yard: stand in it until it falls
                var m = (SiegeMode) b.mode;
                if (b.time % 100 == 0) go(b, p, SiegeMode.YARD_X, SiegeMode.YARD_Z, -90);
                if (b.time - stageAt == 200) { ask("siege-yard"); return; }
                if (m.stage == 0 && b.time - stageAt > 2400) { forcedYard = true; m.fall(b); }
                if (m.stage < 1) return;
                long secs = (b.time - stageAt) / 20;
                Phase4Checks.report("p4-siege-yard", m.stage == 1 && b.scores[0] == 1 && m.target != null && m.target.isAlive(),
                        (forcedYard ? "forced after 120 s contested" : "fell after " + secs + " s") + ", target hp=" + (m.target == null ? "none" : m.target.hp()));
                var row = SiegeMode.gate();
                go(b, p, row.x() - SiegeMode.PASSAGE_X - 6 + .5, row.z() + .5, -90);
                ask("siege-gate"); sub = 5;
            }
            case 5 -> { // the Iron Gate: player blows until it breaks
                var m = (SiegeMode) b.mode;
                if (m.stage == 1 && m.target != null && b.time % 20 == 0) m.target.damage(p.getDamageSources().playerAttack(p), 200);
                if (m.stage < 2) return;
                boolean open = b.world.getBlockState(new BlockPos(-37, 68, 0)).isAir();
                Phase4Checks.report("p4-siege-breach", m.stage == 2 && b.scores[0] == 2 && open && b.saves.isEmpty(),
                        "passage open=" + open + " saves=" + b.saves.size());
                ask("siege-breach"); sub = 6;
            }
            case 6 -> {
                go(b, p, SiegeMode.KEEP_X, SiegeMode.KEEP_Z, 0);
                stageAt = b.time; sub = 7;
            }
            case 7 -> { // the Keep: hold it until the attackers win
                var m = (SiegeMode) b.mode;
                if (b.time % 100 == 0 && m.stage == 2) go(b, p, SiegeMode.KEEP_X, SiegeMode.KEEP_Z, 0);
                if (b.time - stageAt == 200) { ask("siege-keep"); return; }
                if (m.stage == 2 && b.time - stageAt > 3600) { // contested past 3 min: clear defenders from the ring
                    forcedKeep = true;
                    for (var u : b.units) if (u.squad.team == 1 && u.mob.isAlive() && u.mob.squaredDistanceTo(SiegeMode.KEEP_X, u.mob.getY(), SiegeMode.KEEP_Z) < 400) u.mob.discard();
                }
                if (m.stage < 3) return;
                Phase4Checks.report("p4-siege-keep", b.scores[0] == 3 && c.lastWinner == 0,
                        (forcedKeep ? "forced: defenders cleared from the ring" : "held in " + (b.time - stageAt) / 20 + " s") + ", score=" + b.scores[0] + " winner=" + c.lastWinner);
                stageAt = b.time; sub = 8;
            }
            case 8 -> { // the round ends and its teardown leaves no marker or saved box
                if (b.running && b.time - stageAt < 400) return;
                if (b.running) c.returnLobby();
                boolean clean = !b.running && !Files.exists(Modes.marker(b)) && b.saves.isEmpty();
                Phase4Checks.report("p4-siege-end", clean, "running=" + b.running + " marker=" + Files.exists(Modes.marker(b)) + " saves=" + b.saves.size());
                done = true;
            }
            default -> done = true;
        }
    }
}
