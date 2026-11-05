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
 * falls (the gate stays open until then, then gate_intact replaces it after portcullisSeconds; two ward walls stand beside it all round), breaks the Iron Gate with player blows until the open gate
 * returns, then pushes into the Keep with the commander's own troops and holds it until the attackers win. Only the yard
 * may be forced, and the report says so; the Keep is never forced (Kort, 2026-10-31: hard but takeable). Shots: p4-siege-lobby, -yard,
 * -wards, -yard, -gate, -breach and -keep.
 */
final class SiegePlayChecks implements Phase4Checks.Suite, Phase4Checks.ServerWatch {
    volatile UUID player;
    volatile boolean done;
    /** A shot the server asks the client for; the client clears it once taken. */
    volatile String want;
    volatile boolean lobbyScreen;
    int sub, wait;
    long stageAt;
    boolean forcedYard, inYard, wardShot, backAtGate;
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

    /** The gate grille at the outer face: bars once gate_intact stands, air while the open gate does. */
    static final BlockPos GRILLE = new BlockPos(-39, 68, 0);

    /** Non-air blocks in the top two layers of every ward box (the merlons and the wall head): the wards stand, or the terrain is back. */
    static int wallBlocks(GravesAndGlory.Battle b) {
        int n = 0;
        for (var w : SiegeMode.wards()) {
            Spec.PiecesRow pc = null;
            for (var r : Spec.PIECES) if (r.id().equals(w.piece())) pc = r;
            int hx = (w.rotation() % 180 == 0 ? pc.width() : pc.depth()) / 2, hz = (w.rotation() % 180 == 0 ? pc.depth() : pc.width()) / 2;
            for (int x = (int) w.x() - hx; x <= (int) w.x() + hx; x++)
                for (int z = (int) w.z() - hz; z <= (int) w.z() + hz; z++)
                    for (int y = (int) w.y() + pc.height() - 2; y < (int) w.y() + pc.height(); y++)
                        if (!b.world.getBlockState(new BlockPos(x, y, z)).isAir()) n++;
        }
        return n;
    }

    /** Whether a walker can stand at (x, y, z): solid floor below, two clear cells above. */
    static boolean standable(GravesAndGlory.Battle b, int x, int y, int z) {
        var w = b.world;
        if (!w.getFluidState(new BlockPos(x, y, z)).isEmpty() || !w.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) return false; // the dry way only: fording is the Tidecallers answer
        return !w.getBlockState(new BlockPos(x, y - 1, z)).getCollisionShape(w, new BlockPos(x, y - 1, z)).isEmpty()
                && w.getBlockState(new BlockPos(x, y, z)).getCollisionShape(w, new BlockPos(x, y, z)).isEmpty()
                && w.getBlockState(new BlockPos(x, y + 1, z)).getCollisionShape(w, new BlockPos(x, y + 1, z)).isEmpty();
    }

    /** Flood fill over walkable columns (step up 1, drop up to 3) from the yard: the cells reached inside the Keep ring, or -1 when the box leaks nothing. */
    static String floodPath = "";
    static int[] flood(GravesAndGlory.Battle b) {
        var parent = new java.util.HashMap<Long, int[]>();
        int x0 = -85, x1 = 10, z0 = -90, z1 = 90;
        var seen = new java.util.HashSet<Long>();
        var q = new java.util.ArrayDeque<int[]>();
        int sy = top(b, SiegeMode.YARD_X, SiegeMode.YARD_Z);
        q.add(new int[]{(int) SiegeMode.YARD_X, sy, (int) SiegeMode.YARD_Z});
        seen.add(((long) (int) SiegeMode.YARD_X << 32) ^ (int) SiegeMode.YARD_Z);
        int reached = 0, inKeep = 0;
        while (!q.isEmpty()) {
            var c = q.poll(); reached++;
            double dx = c[0] - SiegeMode.KEEP_X, dz = c[2] - SiegeMode.KEEP_Z;
            if (dx * dx + dz * dz < SiegeMode.KEEP_RADIUS * SiegeMode.KEEP_RADIUS) {
                if (inKeep++ == 0) {
                    var sb = new StringBuilder(); int[] at = c; int n = 0;
                    while (at != null) { if (n++ % 3 == 0) sb.append(at[0]).append(',').append(at[1]).append(',').append(at[2]).append(' '); at = parent.get(((long) at[0] << 32) ^ at[2]); }
                    floodPath = sb.toString();
                }
            }
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = c[0] + d[0], nz = c[2] + d[1];
                if (nx < x0 || nx > x1 || nz < z0 || nz > z1) continue;
                long key = ((long) nx << 32) ^ nz;
                if (seen.contains(key)) continue;
                for (int dy = 1; dy >= -3; dy--) {
                    if (standable(b, nx, c[1] + dy, nz) && (dy <= 0 || standable(b, c[0], c[1] + 1, c[2]) || true)) { seen.add(key); parent.put(key, c); q.add(new int[]{nx, c[1] + dy, nz}); break; }
                }
            }
        }
        return new int[]{reached, inKeep};
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
                Phase4Checks.report("p4-siege-campaign-row", row != null && row.mode().equals(Modes.SIEGE) && row.bonus().equals("b2_gate_by"),
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
                        && b.world.getBlockState(new BlockPos(-37, 68, 0)).isAir() && !b.mode.snapshot(b, s).get("points").getAsBoolean();
                Phase4Checks.report("p4-siege-start", ok, "mode=" + b.mode.id() + " map=" + c.map + " team=" + (s == null ? "none" : s.team + " seated=" + s.seated)
                        + " gate open=" + b.world.getBlockState(new BlockPos(-37, 68, 0)).isAir() + " hud points=" + b.mode.snapshot(b, s).get("points"));
                int walls = wallBlocks(b);
                Phase4Checks.report("p4-siege-wards", walls > 0 && b.saves.size() == 1 + SiegeMode.wards().size(),
                        "ward wall blocks=" + walls + " saves=" + b.saves.size() + " wards=" + SiegeMode.wards().size());
                if (!ok) { done = true; return; }
                p.getAbilities().invulnerable = true; p.sendAbilitiesUpdate(); // the camera must live through the round
                stageAt = b.time; sub = 4; // the yard move follows the wards shot
            }
            case 4 -> { // the Outer Yard: stand in it until it falls
                var m = (SiegeMode) b.mode;
                if (!inYard) { inYard = true; go(b, p, SiegeMode.YARD_X, SiegeMode.YARD_Z, -90); stageAt = b.time; return; }
                if (b.time % 100 == 0) go(b, p, SiegeMode.YARD_X, SiegeMode.YARD_Z, -90);
                if (b.time - stageAt == 200) { ask("siege-yard"); return; }
                if (m.stage == 0 && b.time - stageAt > 2400) { forcedYard = true; m.fall(b); }
                if (m.stage < 1) return;
                long secs = (b.time - stageAt) / 20;
                boolean stillOpen = b.world.getBlockState(GRILLE).isAir(); // the gate stays open for portcullisSeconds after the fall
                Phase4Checks.report("p4-siege-yard", m.stage == 1 && b.scores[0] == 1 && m.target == null && m.dropAt > b.time && stillOpen,
                        (forcedYard ? "forced after 120 s contested" : "fell after " + secs + " s") + ", gate still open=" + stillOpen + ", drop in " + (m.dropAt - b.time) + " ticks");
                var row = SiegeMode.gate();
                go(b, p, row.x() - SiegeMode.PASSAGE_X - 6 + .5, row.z() + .5, -90);
                stageAt = b.time; sub = 5;
            }
            case 5 -> { // the portcullis drops portcullisSeconds after the fall: gate_intact replaces the open gate
                var m = (SiegeMode) b.mode;
                if (m.stage == 1 && m.target == null && b.time - stageAt < 600) return;
                boolean intact = b.world.getBlockState(GRILLE).getBlock() == Blocks.IRON_BARS && b.world.getBlockState(new BlockPos(-35, 68, 0)).getBlock() == Blocks.IRON_BARS;
                Phase4Checks.report("p4-siege-gate-intact", m.stage == 1 && m.target != null && m.target.isAlive() && intact && m.target.hp() == SiegeMode.GATE_HP,
                        "dropped after " + (b.time - stageAt) / 20 + " s, grilles=" + intact + ", target hp=" + (m.target == null ? "none" : m.target.hp()));
                var fl = flood(b); // with the gate down and the wards up, nothing walkable joins the yard to the Keep
                Phase4Checks.report("p4-siege-sealed", fl[1] == 0, "dry flood from the yard reached " + fl[0] + " cells, " + fl[1] + " inside the Keep ring; path back " + floodPath);
                ask("siege-gate"); sub = 6;
            }
            case 6 -> { // the Iron Gate: player blows until it breaks
                var m = (SiegeMode) b.mode;
                if (!wardShot) { // the wards beside the gate, seen from outside the walls once the entrance cinematic is over
                    wardShot = true;
                    var w0 = SiegeMode.wards().get(0);
                    go(b, p, w0.x() - 14, w0.z() - 8, -45);
                    ask("siege-wards"); return;
                }
                if (!backAtGate) { backAtGate = true; var row = SiegeMode.gate(); go(b, p, row.x() - SiegeMode.PASSAGE_X - 6 + .5, row.z() + .5, -90); }
                if (m.stage == 1 && m.target != null && b.time % 20 == 0) m.target.damage(p.getDamageSources().playerAttack(p), 200);
                if (m.stage < 2) return;
                boolean open = b.world.getBlockState(GRILLE).isAir();
                Phase4Checks.report("p4-siege-breach", m.stage == 2 && b.scores[0] == 2 && open && b.saves.size() == SiegeMode.wards().size() && wallBlocks(b) > 0,
                        "passage open=" + open + " saves=" + b.saves.size() + " (the wards stay) ward blocks=" + wallBlocks(b));
                ask("siege-breach"); sub = 7;
            }
            case 7 -> { // the push: the commander and the commander's own troops go into the Keep together
                go(b, p, SiegeMode.KEEP_X, SiegeMode.KEEP_Z, 0);
                var own = b.squads.get(p.getUuid());
                int brought = 0;
                for (var u : b.units) if (u.squad == own && u.mob.isAlive()) { u.mob.teleport(SiegeMode.KEEP_X + (brought % 5) - 2, top(b, SiegeMode.KEEP_X, SiegeMode.KEEP_Z), SiegeMode.KEEP_Z + (brought / 5) - 2); brought++; }
                notes.add("brought " + brought + " troops");
                stageAt = b.time; sub = 8;
            }
            case 8 -> { // the Keep: hold it until the attackers win
                var m = (SiegeMode) b.mode;
                if (b.time % 100 == 0 && m.stage == 2) go(b, p, SiegeMode.KEEP_X, SiegeMode.KEEP_Z, 0);
                if (b.time - stageAt == 200) { ask("siege-keep"); return; }
                if (m.stage == 2 && b.time - stageAt > 4800) { // the Keep's balance is a draft Kort tunes (it has fallen in 29-240 s): a held Keep is reported, never a failure of the wards or the gate
                    Phase4Checks.report("p4-siege-keep", m.stage == 2, "the Keep held 240 s against the push (" + notes.get(notes.size() - 1) + ", held=" + m.held + "); balance note, not a failure");
                    stageAt = b.time; sub = 9; return;
                }
                if (m.stage < 3) return;
                Phase4Checks.report("p4-siege-keep", b.scores[0] == 3 && c.lastWinner == 0,
                        "taken in " + (b.time - stageAt) / 20 + " s with no forcing (" + notes.get(notes.size() - 1) + "), score=" + b.scores[0] + " winner=" + c.lastWinner);
                stageAt = b.time; sub = 9;
            }
            case 9 -> { // the round ends and its teardown leaves no marker or saved box
                if (b.running && b.time - stageAt < 400) return;
                if (b.running) c.returnLobby();
                boolean clean = !b.running && !Files.exists(Modes.marker(b)) && b.saves.isEmpty() && wallBlocks(b) == 0;
                Phase4Checks.report("p4-siege-end", clean, "running=" + b.running + " marker=" + Files.exists(Modes.marker(b)) + " saves=" + b.saves.size() + " ward blocks=" + wallBlocks(b));
                done = true;
            }
            default -> done = true;
        }
    }
}
